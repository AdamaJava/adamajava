#!/usr/bin/perl -w

##############################################################################
#
#  Program:  qcoverage.pl
#  Author:   John V Pearson
#  Created:  2011-02-18
#
#  Attempt at a quick-n-dirty coverage tool.
#
#  $Id: qcoverage.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Getopt::Long;
use IO::File;
use Data::Dumper;
use Time::HiRes;
use Pod::Usage;

use QCMG::IO::SamReader;

use vars qw( $CVSID $REVISION $VERBOSE $VERSION %PARAMS );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $CVSID ) = '$Id: qcoverage.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    $PARAMS{'infiles'}      = [];
    $PARAMS{'outfile'}      = 'qcoverage_summary.txt';
    $PARAMS{'coverage'}     = 'qcoverage_details.txt';
    $PARAMS{'regions'}      = 'qcoverage_regions.txt';
    $VERBOSE                = 0;
    $VERSION                = 0;

    my $help                = 0;
    my $man                 = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'i|infiles=s@'         =>  $PARAMS{'infiles'},      # -i
           'o|outfile=s'          => \$PARAMS{'outfile'},      # -o
           'c|coverage=s'         => \$PARAMS{'coverage'},     # -c
           'r|regions=s'          => \$PARAMS{'regions'},      # -r
           'g|gfffile=s'          => \$PARAMS{'gfffile'},      # -g
           'v|verbose+'           => \$VERBOSE,                # -v
           'version!'             => \$VERSION,                #
           'h|help|?'             => \$help,                   # -h
           'm|man'                => \$man                     # -m
           );

    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;
    if ($VERSION) { print "$CVSID\n"; exit }

    # Allow for ,-separated lists of input alignment report files
    my @infiles = map { split /\,/,$_ } @{ $PARAMS{'infiles'} };
    $PARAMS{'infiles'} = \@infiles;
    
    print "\nqcoverage.pl  v$REVISION  [" . localtime() ."]\n",
          '    infile(s)      ', join(",\n".' 'x22,
                                      @{ $PARAMS{'infiles'} }),"\n",
          "    outfile        $PARAMS{'outfile'}\n",
          "    coverage       $PARAMS{'coverage'}\n",
          "    regions        $PARAMS{'regions'}\n",
          "    gfffile        $PARAMS{'gfffile'}\n",
          "    verbose        $VERBOSE\n\n" if $VERBOSE;

    my $gff = MyGffReader->new( filename => $PARAMS{gfffile},
                                outfile  => $PARAMS{regions},
                                verbose  => $VERBOSE );

    # Do it!
    my_coverage_calculator( $gff );

    print "\n[" . localtime() ."]  Finished.\n";
}


# As part of the implementation of this script, I (JP) did some basic
# execution profiling:
#
# 1. Using an OO perl file parser (QCMG:IO::SamReader) to create objects
#    (QCMG::IO::SamRecord) for each read takes about 1 minute per
#    million records. 
# 2. Using non-OO perl code to read each SAM record as a line of text
#    takes about 4 seconds perl million records.
# 
# In both cases, the underlying file reading mechanism was
# IO::File->new( "samtools view -h $infile |" ) so that is not a
# differentiator.
#
# Looking at memory usage, if we use the 'big array' approach where the
# coverage for each sequence is first tallied in an array the length of
# the sequence, i.e. one int for every base in the sequence, then we see
# that 50M positions pushed us to about 2GB of RAM so the longest
# chromosome (chr)1 should get us up to approx 10GB which is big but
# quite acceptable given the specs on our cluster nodes.


sub my_coverage_calculator {
    my $gff      = shift;

    # Make sure we can read all of the files before starting any processing
    foreach my $file (@{ $PARAMS{infiles} }) {
        die "Unable to open [$file] for reading: $!" unless ( -r $file );
    }

    # We will make the following assumptions:
    # 1. the reads are sorted by sequence and location
    # 2. only paired reads will have ISIZE values

    my %seqcov       = ();
    my %physcov      = ();
    my @coverage     = ();
    my $coverage_ptr = 1;

    # Get the detailed output file ready
    my $outfh = IO::File->new( $PARAMS{coverage}, 'w' )
                or die "Can't open coverage file [$PARAMS{coverage}]: $!";

    foreach my $infile (@{ $PARAMS{infiles} }) {
        my $infh;
        my $ctr = 0;

        # Cope with SAM or BAM formats based on file extension
        if ($infile =~ /\.bam/) {
            $infh = IO::File->new( "samtools view -h $infile |" )
               or die "Can't open samtools view on BAM [$infile]: $!";
        }
        elsif ($infile =~ /\.sam/) {
            $infh = IO::File->new( "<$infile" )
               or die "Can't open SAM file [$infile]: $!";
        }
        else {
            die "File type for [$infile] cannot be determined ",
                'because extension is not .bam or .sam';
        }

        print( '[' . localtime() . "] opening file $infile\n" ) if $VERBOSE;

        # Local variables
        my $seq      = '';
        my @thisseq  = ();
        my @thisphys = ();

        # Tallys
        my $lines     = 0;
        my $records   = 0;
        my $processed = 0;

        # Read each alignment record
        while (my $line = $infh->getline) {
             chomp $line;
             $lines++;
             next unless $line;        # skip blanks
             next if ($line =~ /^@/);  # skip headers
             $records++;

             # Optional progress message
             if ($records % 1000000 == 0) {
                 print( '[' . localtime() .
                        "]   $records records processed from $infile\n" )
                     if $VERBOSE;
             }

             # Split record into fields
             my @fields = split /\t/, $line;

             # Skip reads that FLAG says are duplicates or didn't map
             next if ($fields[1] & 1024 or $fields[1] & 4);

             $processed++;

             # If reference changes then we need to finalise any
             # existing coverage for this sequence.
             if ($seq ne $fields[2]) {
                 # if there was a sequence finalise the tallys
                 if ($seq) {
                     print( '[' . localtime() . "]  completed seq $seq - ",
                            "lines: $lines;  records:$records;  ",
                            "processed:$processed\n" )
                            if $VERBOSE;
                     finalise( $gff, $seq, $outfh,
                               \%seqcov, \%physcov, \@thisseq, \@thisphys ); 
                 }
                 # Initialise local variables ready for next sequence
                 $seq      = $fields[2];
                 @thisseq  = ();
                 @thisphys = ();
                 $lines    = 0;
                 $records  = 0;
                 print( '[' . localtime() . "]  starting seq $seq\n" )
                     if $VERBOSE;
             }

             # Process the CIGAR tag because we must ignore clipped and
             # inserted bases, i.e. S, H or I.  This piece of code adds
             # approx 10 secs per 1M reads.

             my $cigar = $fields[5];
             $cigar =~ s/\d+[SHI]//g;  # delete S, H and I runs
             my @ops = split /[A-Z]/, $cigar;  # split into base runs
             my @lengths = grep( /\d+/, @ops);  # drop letters
             my $base_len = 0;
             $base_len += $_ foreach @lengths;  # tally base runs

             #print "$fields[5] $cigar $base_len\n";

             # Do sequence coverage
             my $start = $fields[3];
             my $end   = $fields[3]+$base_len;
             foreach my $i ($start..$end) {
                 $thisseq[$i]++;
             }

             # For fragments, physical coverage is the same as sequence
             # but for paired reads, tally the template length if this
             # read is the first of the pair (TLEN is positive)
             if (! ($fields[1] & 2)) {
                 my $start = $fields[3];
                 my $end   = $fields[3]+$base_len;
                 foreach my $i ($start..$end) {
                     $thisphys[$i]++;
                 }
             }
             elsif ($fields[1] & 2 and $fields[8] > 0) {
                 my $start = $fields[3];
                 my $end   = $fields[3]+$fields[8];
                 foreach my $i ($start..$end) {
                     $thisphys[$i]++;
                 }
             }

        }
        $ctr += $lines;

        # Finalise the final sequence
        print( '[' . localtime() . "]  completed seq $seq",
               " - lines: $lines;  records:$records; processed:$processed\n" )
               if $VERBOSE;
        finalise( $gff, $seq, $outfh, \%seqcov, \%physcov,
                                      \@thisseq, \@thisphys ); 
    }
    $outfh->close;

    # Print out the coverage summaries;
    my $sumfh = IO::File->new( $PARAMS{outfile}, 'w' )
                or die "Can't open outfile [$PARAMS{outfile}]: $!";

    print $sumfh join("\t", qw( Sequence CoverageType CoverageLevel 
                                BaseCount ) ),"\n";
    foreach my $seq (sort keys %seqcov) {
        # Sequence Coverage
        my @scovs = sort { $a <=> $b } (keys %{ $seqcov{$seq} });
        foreach my $i (0..$scovs[$#scovs]) {
            my $scov = defined $seqcov{$seq}->{$i} ? $seqcov{$seq}->{$i} : 0;
            print $sumfh join("\t", $seq, 'sequence', $i, $scov),"\n";
        }
        #Physical Coverage
        my @pcovs = sort { $a <=> $b } (keys %{ $physcov{$seq} });
        foreach my $i (0..$pcovs[$#pcovs]) {
            my $pcov = defined $physcov{$seq}->{$i} ? $physcov{$seq}->{$i} : 0;
            print $sumfh join("\t", $seq, 'physical', $i, $pcov),"\n";
        }
    }
    $sumfh->close;

}


sub finalise {
    my $gff         = shift;
    my $seq         = shift;
    my $outfh       = shift;
    my $rh_seqcov   = shift;
    my $rh_physcov  = shift;
    my $ra_thisseq  = shift;
    my $ra_thisphys = shift;

    # Do sequence coverage first
    $rh_seqcov->{ $seq } = {};

    my $last_seq_cov  = scalar(@{$ra_thisseq}) - 1;
    my $last_phys_cov = scalar(@{$ra_thisphys}) - 1;

    my $last_base = ($last_seq_cov > $last_phys_cov) ? $last_seq_cov :
                                                       $last_phys_cov;

    foreach my $i (0..$last_seq_cov) {
        my $scov = defined $ra_thisseq->[$i] ? $ra_thisseq->[$i] : 0;
        my $pcov = defined $ra_thisphys->[$i] ? $ra_thisphys->[$i] : 0;
        print $outfh join("\t", $seq, $i, $scov, $pcov),"\n";
        # Tally the coverage
        $rh_seqcov->{$seq}->{$scov}++;
        $rh_physcov->{$seq}->{$pcov}++;
    }

    $gff->process_sequence( $seq, $ra_thisseq );
}



###########################################################################
#
#  Module:   MyGffReader
#  Creator:  John V Pearson
#  Created:  2009-01-26
#
###########################################################################


package MyGffReader;

use Data::Dumper;
use IO::Zlib;
use Carp qw( confess );

sub new {
    my $class = shift;
    my %params = @_;

    confess "MyGffReader:new() requires a filename or zipname parameter"
        unless ( (exists $params{filename} and defined $params{filename}) or
                 (exists $params{zipname} and defined $params{zipname}) );

    my $self = { filename        => $params{filename},
                 outfile         => $params{outfile},
                 record_ctr      => 0,
                 records         => {},
                 regions_overlap => 0,
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };
    bless $self, $class;

    my $fh;
    if ( $params{zipname} ) {
        $fh = IO::Zlib->new( $params{zipname}, 'r' );
        confess 'Unable to open ', $params{zipname}, "for reading: $!"
            unless defined $fh;
        $self->{filename} = $params{zipname};
    }
    elsif ( $params{filename} ) {
        $fh = IO::File->new( $params{filename}, 'r' );
        confess 'Unable to open ', $params{filename}, "for reading: $!"
            unless defined $fh;
        $self->{filename} = $params{filename};
    }

    my $outfh = IO::File->new( $self->{outfile}, 'w' )
                or die "Can't open outfile [",$self->{outfile},"]: $!";
    $self->{outfh} = $outfh;

    # Initialise
    while (my $line = $fh->getline()) {
        chomp $line;
        next unless $line;        # skip blanks
        next if ($line =~ /^#/);  # skip comments

        $self->{record_ctr}++;
        my @fields = split /\t/, $line;
        push @{ $self->{records}->{$fields[0]} }, \@fields;

        print( '['. localtime(). '] ', $self->{record_ctr},
               " GFF records processed\n" )
               if $self->{record_ctr} % 50000 == 0;
    }

    # Sort each sequence region collection on start point and work out
    # if the regions are overlapping or distinct.  The criteria for
    # distinct are (a) each region end point is >= the region start
    # point, (b) each region start point is > the end point of the
    # preceding region.

    foreach my $seq (sort keys %{ $self->{records} }) {
        # Sort regions based on start location
        my @regions = map { $_->[1] }
                      sort { $a->[0] <=> $b->[0] }
                      map { [ $_->[5], $_ ] }
                      @{ $self->{records}->{$seq} };

        # Save sorted regions for all subsequent operations
        $self->{records}->{$seq} = \@regions;

        #print Dumper \@regions;
        # Minor known logic bug - not considering first record
        foreach my $i (1..$#regions) {
            if ($regions[$i]->[5] > $regions[$i]->[6] or
                $regions[$i]->[5] <= $regions[$i-1]->[6])  {
                #print "coords: ", join(',', @{$regions[$i-1]}), "\n",
                #      '        ', join(',', @{$regions[$i]}), "\n";
                $self->{regions_overlap}++;
            }
        }
        #print join("\t", 'seq: ', $seq, scalar(@regions),
        #                 $self->{regions_overlap} ), "\n";
    }

    return $self;
}


sub process_sequence {
    my $self        = shift;
    my $seq         = shift;
    my $ra_coverage = shift;
    my $outfh       = $self->{outfh};

    foreach my $ra_region (@{ $self->{records}->{$seq} }) {
        my %coverage = ();

        # Tally the coverage
        foreach my $i ($ra_region->[5]..$ra_region->[6]) {
            my $cov = defined $ra_coverage->[$i] ? $ra_coverage->[$i] : 0;
            $coverage{ $cov }++;
        }
        
        # Create tallys by coverage
        my @covs = map { $_ . ':' . $coverage{ $_ } }
                   sort { $a <=> $b } keys %coverage;

        # Print it all out
        print $outfh 
              $ra_region->[0] .':'. $ra_region->[5] .'-'. $ra_region->[6],
              "\t", join(',',@covs), "\n";
    }
}


1;


__END__

=head1 NAME

qcoverage.pl - quick-n-dirty coverage tool


=head1 SYNOPSIS

 qcoverage.pl [options]


=head1 OPTIONS

 -i | --infile        SAM/BAM alignment file(s) to be processed
 -o | --outfile       summary sequence and physical coverage output
 -c | --coverage      per-base sequence and physical coverage output
 -r | --regions       per-region sequence coverage output
 -g | --gfffile       GFF format file of regions for coverage reporting
 -v | --verbose       print progress and diagnostic messages
      --version       print version number
 -h | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

B<qcoverage.pl> is very basic.


=head2 Command-line options

=over 

=item B<-i | --infile>

SAM or BAM alignment file.  The B<-i> option may be specified multiple 
times, and each B<-i> may be followed by a comma-separated list (no 
spaces!) of filenames.  You may also mix the 2 modes. 

=item B<-o | --outfile>

Name of the text-format file output.

=item B<--version>

Print the script version number and exit immediately.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.
The default level is 0 so no diagnostic messages are output.  At level
1, a small number of progress-related messages are written.  These
messages will be printed to STDOUT unless the B<-l> option is used to
specify a logfile.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a 
perldoc on the script.

=back

=head2 Example invocation

 qcoverage.pl -v \
     -i library1_exome_1.bam \
     -o library1_coverage


=head1 AUTHOR

=over

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: qcoverage.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011

Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
