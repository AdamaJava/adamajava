#!/usr/bin/perl -w

##############################################################################
#
#  Program:  qsignature.pl
#  Author:   John V Pearson
#  Created:  2011-11-01
#
#  Take a collection of VCF files and do all pairwise comparisons, where
#  a comparison means taking the alternate/reference allele ration form
#  both files for each SNP position, taking the difference and saving
#  that for later analysis.
#
#  $Id: qsignature.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak verbose );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;
use POSIX qw( floor );
use Storable qw(dclone);
use Statistics::Descriptive;

use QCMG::FileDir::Finder;
use QCMG::IO::CnvReader;
use QCMG::IO::MafReader;
use QCMG::IO::VcfReader;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: qsignature.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

# Setup global data structures (if any)

BEGIN {
}


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my @vcffiles   = ();
    my @vcfdirs    = ();
    my $outfile    = 'qsig.csv';
    my $logfile    = '';
    my $mincov     = 10;
       $VERBOSE    = 0;
       $VERSION    = 0;
    my $help       = 0;
    my $man        = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $cmdline = join(' ',@ARGV);
    my $results = GetOptions (
           'c|vcffile=s'          => \@vcffiles,      # -c
           'd|vcfdir=s'           => \@vcfdirs,       # -d
           'o|outfile=s'          => \$outfile,       # -o
           'l|logfile=s'          => \$logfile,       # -l
           'n|mincov=s'           => \$mincov,        # -n
           'v|verbose+'           => \$VERBOSE,       # -v
           'version!'             => \$VERSION,       # --version
           'h|help|?'             => \$help,          # -?
           'man|m'                => \$man            # -m
           );
    
    if ($VERSION) {
        print "$SVNID\n";
        exit;
    }

    qlogfile($logfile) if $logfile;
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $cmdline\n");
    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;

    # Allow for ,-separated lists of files and directories
    @vcffiles = map { split /\,/,$_ } @vcffiles;
    @vcfdirs  = map { split /\,/,$_ } @vcfdirs;

    # Now we need to decide which of the 3 modes has been invoked

    if (@vcffiles and @vcfdirs) {
        # Compare VCFs against VCFs in directories
        my $finder = QCMG::FileDir::Finder->new( verbose => $VERBOSE );
        my @newvcffiles = ();
        foreach my $dir (@vcfdirs) {
            my @tmpfiles = $finder->find_file( $dir, 'qsig.vcf' );
            # Remove any seq_lib VCFs
            my @files = grep(/seq_mapped/,@tmpfiles);
            my $count = scalar(@files);
            qlogprint {l=>'TOOL'}, "Found $count VCF files in $dir\n";
            push @newvcffiles, @files;
        }
        my $ra_vcffiles1 = read_vcffiles( @vcffiles );
        my $ra_vcffiles2 = read_vcffiles( @newvcffiles );
        my $rh_mapsets   = compare_vcffiles( $ra_vcffiles1, $ra_vcffiles2 ); 
        write_report( $rh_mapsets, $outfile );
    }
    elsif (@vcfdirs) {
        # Compare VCFs within directories
        my $finder = QCMG::FileDir::Finder->new( verbose => $VERBOSE );
        my %all_mapsets = (); 
        foreach my $dir (@vcfdirs) {
            my @files = $finder->find_file( $dir, 'qsig.vcf' );
            my $count = scalar(@files);
            qlogprint {l=>'TOOL'}, "Found $count VCF files in $dir\n";
            my $ra_vcffiles = read_vcffiles( @files );
            my $rh_mapsets  = compare_vcffiles( $ra_vcffiles, $ra_vcffiles ); 
            # Clone results into summary data structure
            foreach my $key (keys %{ $rh_mapsets }) {
                $all_mapsets{ $key } = $rh_mapsets->{ $key };
            }
        }
        write_report( \%all_mapsets, $outfile );
    }
    elsif (@vcffiles) {
        # Cross-compare VCFs against themselves
        my $ra_vcffiles = read_vcffiles( @vcffiles );
        my $rh_mapsets  = compare_vcffiles( $ra_vcffiles, $ra_vcffiles ); 
        write_report( $rh_mapsets, $outfile );
    }
    else {
        die "You did not specify any vcffile or vcfdir parameters";
    }

    qlogend;
}


sub read_vcffiles {
    my @files = @_;

    my @vcffiles = ();
    foreach my $file (@files) {
        my @vcfs = ();
        my $vr = QCMG::IO::VcfReader->new( filename => $file,
                                           verbose  => $VERBOSE );
        while (my $rec = $vr->next_record) {
            push @vcfs, $rec;
        }
        push @vcffiles, { file     => $file,
                          records  => \@vcfs,
                          headers  => $vr->headers,
                          reccount => $vr->record_count };
        qlogprint {l=>'TOOL'},
                  'Read '. $vr->record_count . " VCF records from $file\n";
    }

    return \@vcffiles;
}


sub compare_vcffiles {
    my $ra_vcffiles1 = shift;
    my $ra_vcffiles2 = shift;

    my @vcffiles1 = @{ $ra_vcffiles1 };
    my @vcffiles2 = @{ $ra_vcffiles2 };

    my $stats = Statistics::Descriptive::Sparse->new();

    # Initialise mapsets
    my %mapsets = ();
    foreach my $vcffile (@vcffiles1, @vcffiles2) {
        #print Dumper $vcffile;
        my $mapset = $vcffile->{headers}->{slide} . '.nopd.' .
                     $vcffile->{headers}->{barcode};
        if (! exists $mapsets{$mapset}) {
            $mapsets{$mapset} =
                { name        => $mapset,
                  patient     => $vcffile->{headers}->{patient_id},
                  input_type  => $vcffile->{headers}->{input_type},
                  library     => $vcffile->{headers}->{library},
                  comparisons => {} };
        }
    }

    # Cartesian product of comparisons on two vcffiles lists
    my $ctr = 0;
    foreach my $vcffile1 (@vcffiles1) {

        my $mapset1 = $vcffile1->{headers}->{slide} . '.nopd.' .
                      $vcffile1->{headers}->{barcode};

        foreach my $vcffile2 (@vcffiles2) {
            $ctr++;

            my $mapset2 = $vcffile2->{headers}->{slide} . '.nopd.' .
                          $vcffile2->{headers}->{barcode};

            my $ra_scores = _process_pair( $vcffile1, $vcffile2 );
            my @scores = @{ $ra_scores };

            my $class = '';
            if ( $vcffile1->{headers}->{patient_id} eq
                 $vcffile2->{headers}->{patient_id} ) {
                 $class .= 'A';
            }
            else {
                 $class .= 'B';
            }
            if ( $vcffile1->{headers}->{input_type} eq
                 $vcffile2->{headers}->{input_type} ) {
                 $class .= 'A';
            }
            else {
                 $class .= 'B';
            }
            if ( $vcffile1->{headers}->{library} eq
                 $vcffile2->{headers}->{library} ) {
                 $class .= 'A';
            }
            else {
                 $class .= 'B';
            }

            $stats->add_data( @scores );

            # Add the match to both mapsets
            $mapsets{$mapset1}->{comparisons}->{$mapset2} =
                  { snps_used  => scalar(@scores),
                    AAA_rating => $class,
                    score      => $stats->mean,
                    sd         => $stats->standard_deviation };
            $mapsets{$mapset2}->{comparisons}->{$mapset1} =
                  { snps_used  => scalar(@scores),
                    AAA_rating => $class,
                    score      => $stats->mean,
                    sd         => $stats->standard_deviation };

            $stats->clear;
        }
    }
    qlogprint {l=>'TOOL'}, "Completed $ctr pairwise VCF comparisons\n";

    return \%mapsets;
}


sub _process_pair {
    my $rh_file1 = shift;
    my $rh_file2 = shift;

    my $pattern =
        qr/FULLCOV=A:(\d+),C:(\d+),G:(\d+),T:(\d+),N:(\d+),TOTAL:(\d+)/;

    qlogprint( 'Comparing ', $rh_file1->{headers}->{slide}, ' [',
                             $rh_file1->{headers}->{barcode}, ',',
                             $rh_file1->{headers}->{patient_id}, '] and ',
                             $rh_file2->{headers}->{slide}, ' [',
                             $rh_file2->{headers}->{barcode}, ',',
                             $rh_file2->{headers}->{patient_id}, "]\n" );

    die "Record count mismatch\n"
        unless ($rh_file1->{reccount} == $rh_file2->{reccount});

    my @scores = ();
    foreach my $i (0..($rh_file1->{reccount}-1)) {
        my $rec1 = $rh_file1->{records}->[$i];
        my $rec2 = $rh_file2->{records}->[$i];
        die "Record details mismatch\n"
            unless ($rec1->chrom eq $rec2->chrom and
                    $rec1->position eq $rec2->position and
                    $rec1->ref_allele eq $rec2->ref_allele);

        my $info1 = {};
        if ($rec1->info =~ /$pattern/) {
           $info1 = { A => $1, C => $2, G => $3, T => $4,
                      N => $5, TOTAL => $6 };
        }
        my $info2 = {};
        if ($rec2->info =~ /$pattern/) {
           $info2 = { A => $1, C => $2, G => $3, T => $4,
                      N => $5, TOTAL => $6 };
        }
        next unless ($info1->{TOTAL} >= 10 and
                     $info2->{TOTAL} >= 10);

        # For starters, we'll do the laziest score possible - take the
        # reference counts as a % of the totals and subtract them

        push @scores, abs( $info1->{ $rec1->ref_allele } / $info1->{TOTAL} -
                           $info2->{ $rec1->ref_allele } / $info2->{TOTAL});
    }

    return \@scores;
}


sub write_report {
    my $rh_mapsets = shift;
    my $file       = shift;

    my %mapsets = %{ $rh_mapsets };

    # Get the detailed output file ready
    my $outfh = IO::File->new( $file, 'w' );
    croak "Can't open output file $file for writing: $!"
        unless defined $outfh;

    my $score_threshold = 0.03;
    my $snps_threshold  = 300;
    my @problems  = ();

    print $outfh join("\t", qw( Mapset1 Library1 Patient1
                                Mapset2 Library2 Patient2
                                SNPs_used AAA_rating Score StdDev)),"\n";

    # Group analyses by subject IDs
    foreach my $mapset1 (sort keys %mapsets) {
        foreach my $mapset2 (sort keys %{$mapsets{$mapset1}->{comparisons}} ) {
           my $this_comp = $mapsets{$mapset1}->{comparisons}->{$mapset2};
           print $outfh join("\t", $mapset1,
                                   $mapsets{$mapset1}->{library},
                                   $mapsets{$mapset1}->{patient},
                                   $mapset2,
                                   $mapsets{$mapset2}->{library},
                                   $mapsets{$mapset2}->{patient},
                                   $this_comp->{snps_used},
                                   $this_comp->{AAA_rating},
                                   $this_comp->{score},
                                   $this_comp->{sd} ), "\n";

           # Spot any bad matches to BAMs with same patient ID
           if (($mapsets{$mapset1}->{patient} eq
                $mapsets{$mapset2}->{patient}) and
                $this_comp->{snps_used} > $snps_threshold and
                $this_comp->{score} >= $score_threshold) {
                push @problems, [ 'Bad match to BAM from same donor',
                                  $mapset1,
                                  $mapsets{$mapset1}->{patient},
                                  $mapset2,
                                  $mapsets{$mapset2}->{patient},
                                  $this_comp->{score} ];
           }

           # Spot any good matches to BAMs with different patient ID
           if (($mapsets{$mapset1}->{patient} ne
                $mapsets{$mapset2}->{patient}) and
                $this_comp->{snps_used} > $snps_threshold and
                $this_comp->{score} > 0 and
                $this_comp->{score} <= $score_threshold) {
                push @problems, [ 'Good match to BAM from different donor',
                                  $mapset1,
                                  $mapsets{$mapset1}->{patient},
                                  $mapset2,
                                  $mapsets{$mapset2}->{patient},
                                  $this_comp->{score} ];
           }
        }
    }

    # Now print the problems
    print $outfh "\n",
                 join("\t", qw(Mapset Donor Other_Mapset Other_Donor Score)),
                 "\n";
    foreach my $prob (@problems) {
        print $outfh join("\t", @{$prob}), "\n";
    }

    $outfh->close;
}



__END__

=head1 NAME

qsignature.pl - Compare VCFs from BAMs


=head1 SYNOPSIS

 qsignature.pl -c vcffile1 -c vcffile2 -o outfile [options]


=head1 ABSTRACT

This script takes 2 or more VCF files
and does all pairwise comparisons between the files, where
a comparison means taking the alternate/reference allele ratios form
both files for each SNP position, taking the difference and saving
that for later analysis.


=head1 OPTIONS

 -c | --vcffile       VCF allele frequency file(s)
 -d | --vcfdir        Directory(s) containing .qsig.vcf VCF files
 -n | --mincov        Minimum base count for comparison; default=10
 -o | --outfile       CSV output file; default=vsum.csv
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

This script has 3 modes of operation.

The first mode occurs if a list of VCF files is specified with B<-v>. 
The script does a cartesian product of comparisons - all VCFs in the 
list against all VCFs in the list.  This is useful for testing and to
work out if a group of BAMs belong together.

If a list of
driectories is specified with B<-d> then a cartesian product of
comparisons are done within each directory - all VCFs vs all VCFs but
only within the same directory.  All subdirectories below each B<-d>
directory are also searched for VCFs but this can easily get you into
trouble if you specify a directory at too high a level.
For example, if you were to specify B<-d /panfs/seq_results/icgc_pancreatic/>
then all 1000 VCFs in the Pancreatic project would be found and compared
against each other - a U<million> comparisons!  Even at 3 comparisons a
second, thats 4 days to complete.  This mode is useful if you want to
scan a series of patient directories to make sure the BAMs they contain
all belong together.

The final mode is triggered if both files are directories are specified.
In this case, all VCF files in the B<-v> list are compared against all
VCF files in all of the B<-d> directories.  This mode is useful if you
have a small number of BAMs and you need to work out which donor they
belong to - you specify B<-d /panfs/seq_results/icgc_pancreatic/>
as the single directory and the VCFs will be compared against every VCF
in the pancreatic project.

=head2 Commandline options

=over2

=item B<-c | --vcffile>

VCF file created by Lynn's pileup script.  Contains the reference and
alternate allele counts at dbSNP positions.

=item B<-n | --mincov>

Minimum total of reference plus alternate alleles that must be present
in both VCF files before an actual comparion is done.  Default=10.

=item B<-o | --outfile>

CSV output file.  Patients are always columns ordered either
alphabetically or according to the order established by B<--patfile>.

=item B<-l | --logfile>

Optional log file name.  If this option is not specified then logging
goes to STDOUT.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.

=item B<--version>

Print the script version and exit immediately.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=item 

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: qsignature.pl 4669 2014-07-24 10:48:22Z j.pearson $


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
