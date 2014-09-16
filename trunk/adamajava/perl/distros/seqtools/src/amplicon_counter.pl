#!/usr/bin/perl -w

##############################################################################
#
#  Program:  amplicon_counter.pl
#  Author:   John V Pearson
#  Created:  2010-09-29
#
#  Script for determining amplicon coverage by looking at read start
#  rather than standard per-base coverage stats.
#
#  $Id: amplicon_counter.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
use Carp qw( carp croak );

use QCMG::IO::GffReader;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: amplicon_counter.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my @infiles        = ();
    my $gfffile        = '';
    my $outfile        = '';
    my $logfile        = '';
    my $upstream       = 35;
    #my $samtoolsbin    = '/panfs/share/software/samtools-0.1.8/samtools';
    my $samtoolsbin    = '/sw/samtools/1.8/bin/samtools';
       $VERBOSE        = 0;
       $VERSION        = 0;
    my $help           = 0;
    my $man            = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $cmdline = join(' ',@ARGV);
    my $results = GetOptions (
           'i|infile=s'           => \@infiles,       # -i
           'g|gfffile=s'          => \$gfffile,       # -g
           'o|outfile=s'          => \$outfile,       # -o
           'l|logfile=s'          => \$logfile,       # -l
           'u|upstream=i'         => \$upstream,      # -u
           's|samtoolsbin=s'      => \$samtoolsbin,   # -s
           'v|verbose+'           => \$VERBOSE,       # -v
           'version!'             => \$VERSION,       # --version
           'h|help|?'             => \$help,          # -?
           'man|m'                => \$man            # -m
           );

    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;

    if ($VERSION) {
        print "$SVNID\n";
        exit;
    }

    qlogfile($logfile) if $logfile;
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $cmdline\n");

    # Allow for ,-separated lists of files
    @infiles = map { split /\,/,$_ } @infiles;

    die 'You must supply a BAM file name' unless scalar(@infiles);
    die 'You must supply a GFF file name' unless $gfffile;

    # Start by reading the regions we are looking for.
    my $rh_regions = read_gff_file( $gfffile );

    my $rh_tally = process_bams( \@infiles, $rh_regions, $samtoolsbin );
    write_report( $rh_tally, $rh_regions, $upstream, $outfile );

}


sub process_bams {
    my $ra_infiles  = shift;
    my $rh_regions  = shift;
    my $samtoolsbin = shift;

    #print Dumper $ra_infiles, $rh_regions;
    
    my %tally = ();

    foreach my $file (@{ $ra_infiles }) {

        # Open BAM file for reading - N.B. we do *NOT* want the header output
        qlogprint "Opening BAM file $file\n";
        open INSAM,  "$samtoolsbin view $file |"
            or die "Can't open samtools view on BAM [$file]: $!\n";

        my $ctr = 0;

        # Read through BAM
        while (my $line = <INSAM>) {
            chomp $line;
            $ctr++;
            if (($ctr % 100000 == 0) and $VERBOSE) {
                qlogprint {l=>'INFO'}, "$ctr Records read from file\n";
            }

            # Skip any reads that match a sequence not in our GFF
            my @fields = split /\t/, $line;
            next unless exists $rh_regions->{$fields[2]};
            $tally{$fields[2]}->{$fields[3]}++;

            # Useful for debugging limited run
            #last if $ctr > 20000;
        }
        qlogprint "$ctr records processed from $file\n";
    }

    return \%tally;
}


sub read_gff_file {
    my $gfffile = shift;

    my %regions = ();
    my $gff = QCMG::IO::GffReader->new( filename => $gfffile );
    while (my $region = $gff->next_record()) {
        # Store region details in hash keyed on sequence
        push @{ $regions{ $region->seqname } },
             [ $region->start, $region->end ];
    }

    # Sort regions by start position within each sequence
    foreach my $seq (keys %regions) {
        my @sorted_regions = map  { $_->[1] }
                             sort { $a->[0] <=> $b->[0] }
                             map  { [$_->[0], $_ ] }
                             @{ $regions{$seq} };
        $regions{ $seq } = \@sorted_regions;
    }

    return \%regions;
}


sub write_report {
    my $rh_tally   = shift;
    my $rh_regions = shift;
    my $upstream   = shift;
    my $outfile    = shift;

    # Get the detailed output file ready
    my $outfh = IO::File->new( $outfile, 'w' );
    croak "Can't open output file $outfile for writing: $!\n"
        unless defined $outfh;
    
    print $outfh join("\t", qw( chr start end max total flag )), "\n";
    
    foreach my $seq (sort keys %{$rh_regions}) {
        my $last = 0;
        foreach my $region (@{ $rh_regions->{$seq} }) {
            my $max   = 0;
            my $total = 0;
            my $start = $region->[0];
            my $stop  = $start + $upstream;
            my $flag = '';
            foreach my $loc ($start..$stop) {
                if (exists $rh_tally->{$seq}->{$loc}) {
                     my $count = $rh_tally->{$seq}->{$loc};
                     $max = $count > $max ? $count : $max;
                     $total += $count;
                }
            }
            $flag .= 'too_close' if $start < $last+$upstream;
            $last = $start;
            print $outfh join("\t",$seq,$start,$stop,$max,$total,$flag), "\n";
        }
    }

    print $outfh "\n###\n\n", join("\t", qw( chr start_pos tally )), "\n";

    foreach my $seq (sort keys %{$rh_tally}) {
        foreach my $loc (sort {$a<=>$b} keys %{$rh_tally->{$seq}}) {
            print $outfh join("\t",$seq,$loc,$rh_tally->{$seq}->{$loc}),
                         "\n";
        }
    }

}


__END__

=head1 NAME

amplicon_counter.pl - Perl script for coverage for amplicon sequencing


=head1 SYNOPSIS

 amplicon_counter.pl [options]


=head1 ABSTRACT

This is a very quick-n-dirty script for creating BED files of read
start points from BAM files for WTA analysis.


=head1 OPTIONS

 -i | --infile        name of BAM file(s)
 -g | --gfffile       name of GFF file of amplicon regions
 -o | --outfile       name of output file
 -l | --logfile       name of log file
 -u | --upstream      distance (bp) upstream to search for max read count
 -s | --samtoolsbin   location of samtools binary
 -v | --verbose       print diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

B<bam2bed.pl> relies on samtools for the conversions between SAM and 
BAM so you'll need to have samtools installed to use this script.  This
script is very basic and makes the following assumptions:

 1. The BAM is sorted by sequence and position within each sequence.
 2. All reads flagged as second-in-pair are ignored.  This means we only
    count each LMP or PE pair once BUT it also means we tally LMP and PE
    read-pairs based on the leftmost base of their F3 read which might
    be ideal.

=head2 Commandline Options

=over

=item B<-i | --infile>

Full pathname to BAM to be processed.

=item B<-t | --tmpdir>

Directory where the 2 output BED files (one for positive and one for
negative) will be placed.
This script was designed to be run once by John
Pearson so the default value for this parameter is JP's ~/tmp/
directory (/panfs/home/jpearson/tmp/).

=item B<-s | --samtoolsbin>

Full pathname to the samtools executable.

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


=head1 SEE ALSO

=over 2

=item perl

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: amplicon_counter.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012

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
