#!/usr/bin/perl -w

##############################################################################
#
#  Program:  tally_feature_coverage.pl
#  Author:   John V Pearson
#  Created:  2011-04-28
#
#  Take multiple --per-feature XML reports from qcoverage and summarise
#  the average coverage across all files for each feature.  The intent
#  is to identify any features that have abnormally high or low coverage.
#  The first use case for this is to find poorly performing bait
#  regions from the Agilent SureSelect kit.
#
#  $Id: tally_feature_coverage.pl 3023 2011-05-23 16:01:51Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use XML::Simple;
use Data::Dumper;
use Pod::Usage;
use Carp qw( carp croak );
use Statistics::Descriptive;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 3023 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: tally_feature_coverage.pl 3023 2011-05-23 16:01:51Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my @infiles    = ();
    my $outfile    = '';
    my $wiggle     = '';
    my $wiglabel   = '';
       $VERBOSE    = 0;
       $VERSION    = 0;
    my $help       = 0;
    my $man        = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'i|infile=s'           => \@infiles,       # -i
           'o|outfile=s'          => \$outfile,       # -o
           'w|wiggle=s'           => \$wiggle,        # -w
           'l|wiglabel=s'         => \$wiglabel,      # -l
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

    print( "\ntally_feature_coverage.pl  v$REVISION  [" . localtime() . "]\n",
           '   infile(s)     ', join("\n".' 'x17, @infiles), "\n",
           "   outfile       $outfile\n",
           "   wiggle        $wiggle\n",
           "   verbose       $VERBOSE\n\n" ) if $VERBOSE;

    my %features = ();
    my @files    = ();
    foreach my $file (@infiles) {
        process_file( $file, \%features, \@files );
    }

    print_report( $outfile, \%features, \@files ) if $outfile;
    print_wiggle( $wiggle, \%features, $wiglabel ) if $wiggle;
    print '['.localtime()."]  Finished.\n" if $VERBOSE;
}



sub process_file {
    my $file        = shift;
    my $rh_features = shift;
    my $ra_files    = shift;

    print '['.localtime()."]  Opening file $file\n" if $VERBOSE;
    my $fh = IO::File->new( $file, 'r' );
    die("Cannot open input file $file for reading: $!\n")
        unless defined $fh;

    my $file_total_bases   = 0;
    my $file_features      = 0;
    my $file_feature_bases = 0;  # How many bases in features
    my $file_new_features  = 0;
    
    # Number for this file is one greater than previous file (if any)
    my $filectr = exists $ra_files->[-1] ? $ra_files->[-1]->{filectr}+1 : 0;

    # Slurp file
    print '['.localtime()."]    reading contents\n" if $VERBOSE;
    my $contents = '';
    while (my $line = $fh->getline) {
        $contents .= $line;
    }

    # Pull out all the <coverageReport/> elements using regexes
    print '['.localtime()."]    processing coverageReports\n" if $VERBOSE;
    my $rpt_pattern = qr/<coverageReport.+?<\/coverageReport>/s;  # y
    my $cov_pattern = qr/<coverage bases="(\d+)" at="(\d+)"\/>/s;  # y
    my @matches = $contents =~ m/$rpt_pattern/g;

    # Process each coverageReport
    my $featurectr = 0;
    foreach my $xml (@matches) {
        $file_features++;
        if ($xml =~ /feature="(.*)"/) {
            my $feature = $1;
            my @fields = split /\t/, $feature;

            my $seq   = $fields[0];
            my $start = $fields[3];
            my $end   = $fields[4];
            my $id    = $seq . ':' . $start . '-' . $end;
            my $len   = $end - $start + 1;

            # Tally coverage for this feature
            my @bases = $xml =~ m/$cov_pattern/g;
            my $total = 0;
            while (@bases) {
               my $x = shift @bases;
               my $y = shift @bases;
               $total += ($x * $y);
            }

            # Work out if we've seen this one before
            if (exists $rh_features->{$id}) {
                $rh_features->{$id}->{seen}++;
                $rh_features->{$id}->{total} += $total;
            }
            else {
                $file_new_features++;
                $rh_features->{$id} = { id      => $id,
                                        ctr     => $featurectr++,
                                        seq     => $seq,
                                        start   => $start,
                                        end     => $end,
                                        bases   => $len,
                                        total   => $total,
                                        seen    => 1,
                                        tallies => [ ] };
            }

            # Save total for this file + feature
            $rh_features->{$id}->{tallies}->[$filectr] = $total;

            # Increase total for this file
            $file_total_bases += $total;

            # Count total length of features
            $file_feature_bases += $len;
        }
    }

    # Calculate scaling factor to be applied to the average coverage of
    # each feature to move it to a standard "100x" coverage.  Depends on
    # the number of bases covered by the features and the number of
    # sequenced bases in the file.
    my $scale = 100 / ($file_total_bases / $file_feature_bases);
    push @{ $ra_files }, { name          => $file,
                           filectr       => $filectr,
                           features      => $file_features,
                           feature_bases => $file_feature_bases,
                           scale         => $scale,
                           new_features  => $file_new_features,
                           bases         => $file_total_bases };

    print '['.localtime()."]    processed $file_features coverageReports\n"
        if $VERBOSE;
}


sub print_report {
    my $file        = shift;
    my $rh_features = shift;
    my $ra_files    = shift;

    print '['.localtime()."]  Opening file $file\n" if $VERBOSE;
    my $fh = IO::File->new( $file, 'w' );
    die("Cannot open output file $file for writing: $!\n")
        unless defined $fh;

    my $heredoc = <<EOH;
##############################################################################
#
#  File:     FFFFF
#  Creator:  CCCCC
#  Created:  DDDDD
#
#  This file was created by the tally_feature_coverage.pl program
#  written by John Pearson.  It summarises coverage per-feature across
#  multiple per-feature qcoverage XML files.  It is particularly useful
#  for examining the performance of enrichment bait sets such as the
#  Agilent SureSelect 50 Mb All Exon kit.
#
#  The header contains a table of information about the XML files that
#  were included in the summary.  The fields that are included for each
#  file are:
#
#   1. ctr - numeric id that also denotes the offset of the tally for this
#          file in the array of coverages.
#   2. total_bases - number of sequenced bases that were counted.
#   3. features - number of features found in this file.
#   4. novel_features - how many features were seen for the first time in
#          this file.  Usually 0 for all except first file.
#   5. file_name - file name.
#
#  The primary contents of this file is a table that lists key
#  parameters about each feature.  The per-feature data fields are:
#
#   0. seq - name of the sequence the feature is located on, e.g. chr1.
#   1. start - start base of the feature (inclusive).
#   2. end - end base of the feature (inclusive).
#   3. region_length - number of bases in the feature .
#   4. coverage_total - total bases the fell within the feature across
#          all of the processed files.
#   5. coverage_per_base - field 4 / field 3.
#   6. mean - mean of the normalised values in field 8.
#   7. std_dev - std_dev of the normalised values in field 8.
#   8+ file0..n - normalised coverage per base, calculated by taking each
#          file's base count dividing by the region length and then again
#          by the total bases for that file (in GB).
#
#   1. ctr - numeric id that also denotes the offset of the tally for this
#          file in the array of coverages.
#   2. total_bases - number of sequenced bases that were counted.
#   3. scale - number used to convert all per-feature coverages to a scale
#          where 100x is the nominal coverage regardless of how much
#          sequence was actually in the file.
#   3. features - number of features found in this file.
#   4. novel_features - how many features were seen for the first time in
#          this file.  Usually 0 for all except first file.
#   5. file_name - file name.
#  
#  The primary contents of the file is a table that lists key
#  about each feature.  The per-feature data fields are:
#  
#   0. seq - name of the sequence the feature is located on, e.g. chr1.
#   1. start - start base of the feature (inclusive).
#   2. end - end base of the feature (inclusive).
#   3. region_length - number of bases in the feature .
#   4. coverage_total - total bases the fell within the feature across
#          all of the processed files.
#   5. coverage_per_base - field 4 / field 3.
#   6. mean - mean of the normalised values in field 8.
#   7. std_dev - std_dev of the normalised values in field 8.
#   8+ file0..n - normalised coverage per base, calculated by taking each
#          file's base count dividing by the region length and then
#          multiplying by the scaling factor for the file. This has the 
#          effect of normalising all coverages to a nominal 100x coverage.
#  
#  Here's a scaling example:
#  
#    1. Capture platform is 2 Mbases of sequence
#    2. File contains 500 Mbases of sequence
#    3. Scaling factor is 0.4 which is calculated as:
#       100 / ( 500 / 2 )
#  
##############################################################################
#
#
EOH

    my $datestamp = localtime() . ''; 
    $heredoc =~ s/ FFFFF / $file /x;
    $heredoc =~ s/ CCCCC / $ENV{USER} /x;
    $heredoc =~ s/ DDDDD / $datestamp /x;

    print $fh $heredoc,
          '#', join("\t", qw( ctr total_bases feature_bases scale
                              features novel_features file_name ) ), "\n";

    my @coverage_headers = qw( seq start end region_length
                               coverage_total coverage_per_base
                               mean std_dev );
                               
    my @files = @{ $ra_files };
    foreach my $rh_file (@files) {
        print $fh '# ', join("\t", $rh_file->{filectr},
                                   $rh_file->{bases},
                                   $rh_file->{feature_bases},
                                   sprintf("%.2f",$rh_file->{scale}),
                                   $rh_file->{features},
                                   $rh_file->{new_features},
                                   $rh_file->{name} ),"\n";
        push @coverage_headers, 'file'.$rh_file->{filectr};
    }

    print $fh '#', join("\t", @coverage_headers ), "\n";

    # Push values into hash keyed on sequence name
    my %features = ();
    foreach my $key (sort keys %{ $rh_features }) {
        push @{ $features{ $rh_features->{$key}->{seq} } },
             $rh_features->{$key};
    }

    # Sort the sequence names with some extra magic
    my @sorted_seqs = map  { $_->[0] }
                      sort { $a->[1] <=> $b->[1] }
                      map  { my $chr = $_; $chr =~ s/^chr//;
                             my $val = $chr eq 'M' ? 25 :
                                       $chr eq 'Y' ? 24 :
                                       $chr eq 'X' ? 23 : $chr;
                             [ $_, $val ] }
                      keys %features;

    foreach my $seq (@sorted_seqs) { 
        # Sort by start position with each sequence
        my @sorted_feats = map  { $_->[0] }
                           sort { $a->[1] <=> $b->[1] }
                           map  { [ $_, $_->{start} ] }
                           @{ $features{ $seq } };

        my $stats = Statistics::Descriptive::Sparse->new();
   
        foreach my $feat (@sorted_feats) {
            # Calculate normalised per-base coverages for each file
            my @covs  = ();
            my @norm_covs = ();
            foreach my $fctr (0..$#files) {
                my $tally = exists $feat->{tallies}->[ $fctr ] ?
                            $feat->{tallies}->[ $fctr ] : 0;
                $covs[ $fctr ] = $tally;
                # Use scaling factor for this file to normalise coverage
                $norm_covs[ $fctr ] = $tally / $feat->{bases} * $files[ $fctr ]->{scale};
            }

            # Calculate some derivative numbers
            my $coverage_per_base = $feat->{total} / $feat->{bases} ;
            $feat->{coverage_per_base} = $coverage_per_base;
            $stats->add_data( @norm_covs );
            $feat->{mean} = $stats->mean;
            $feat->{stddev} = $stats->standard_deviation;
            $stats->clear;

            # Print it all out
            my @stringy_covs = map { sprintf("%.2f",$_) } @norm_covs;
            print $fh join("\t", $feat->{seq},
                                 $feat->{start},
                                 $feat->{end},
                                 $feat->{bases},
                                 $feat->{total},
                                 sprintf("%.2f",$feat->{coverage_per_base}),
                                 sprintf("%.2f",$feat->{mean}),
                                 sprintf("%.2f",$feat->{stddev}),
                                 join("\t",@stringy_covs) ), "\n";
        }
    }
}


sub print_wiggle {
    my $file        = shift;
    my $rh_features = shift;
    my $wiglabel    = shift;

    # This subroutine puts out the bait coverages in wiggle format:
    # http://genome.ucsc.edu/goldenPath/help/wiggle.html

    print '['.localtime()."]  Opening file $file\n" if $VERBOSE;
    my $fh = IO::File->new( $file, 'w' );
    die("Cannot open output file $file for writing: $!\n")
        unless defined $fh;

    print $fh "track type=wiggle_0 name=$wiglabel\n";

    # Push values into hash keyed on sequence name
    my %features = ();
    foreach my $key (sort keys %{ $rh_features }) {
        push @{ $features{ $rh_features->{$key}->{seq} } },
             [ $rh_features->{$key}->{seq},
               $rh_features->{$key}->{start},
               $rh_features->{$key}->{bases},
               sprintf("%.2f",$rh_features->{$key}->{mean}) ];
    }

    # Sort the sequences
    my @sorted_seqs = map  { $_->[0] }
                      sort { $a->[1] <=> $b->[1] }
                      map  { my $chr = $_; $chr =~ s/^chr//;
                             my $val = $chr eq 'M' ? 25 :
                                       $chr eq 'Y' ? 24 :
                                       $chr eq 'X' ? 23 : $chr;
                             [ $_, $val ] }
                      keys %features;

    foreach my $seq (@sorted_seqs) { 
        # Sort by start position with each sequence
        my @sorted_feats = map  { $_->[0] }
                           sort { $a->[1] <=> $b->[1] }
                           map  { [ $_, $_->[1] ] }
                           @{ $features{ $seq } };

        foreach my $feat (@sorted_feats) {
            print $fh "variableStep chrom=", $feat->[0],
                      " span=", $feat->[2], "\n",
                      $feat->[1], ' ', $feat->[3], "\n";
        }
    }
}


__END__

=head1 NAME

tally_feature_coverage.pl - Perl script for processing per-feature coverage files


=head1 SYNOPSIS

 tally_feature_coverage.pl [options] -o outfile


=head1 ABSTRACT

This script will take multiple XML coverage files output by qcoverage
using the --per-feature option and it will assemble information for each
feature from all of the XML files in which it is reported.  The output
report shows for each feature, how long the feature is, how many bases
covered the feature, how much average coverage that represents for each
base of the feature and the average coverage per feature base per input
file.


=head1 OPTIONS

 -i | --infile        qcoverage --per-feature XML file
 -o | --outfile       per feature coverage report (tab-separated plain text)
 -w | --wiggle        same data as -o but in wiggle format for IGV
 -v | --verbose       print progress and diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

tally_feature_coverage.pl summarises coverage per-feature across
multiple per-feature qcoverage XML files.  It is particularly useful
for examining the performance of enrichment bait sets such as the
Agilent SureSelect 50 Mb All Exon kit.

The output is a plain-text tab-separated data file ready for import and
further analysis in R or Excel.  The file header contains a table of
information about the XML files that were included in the summary.
The fields that are written for each included file are:

 1. ctr - numeric id that also denotes the offset of the tally for this
        file in the array of coverages.
 2. total_bases - number of sequenced bases that were counted.
 3. scale - number used to convert all per-feature coverages to a scale
        where 100x is the nominal coverage regardless of how much
        sequence was actually in the file.
 3. features - number of features found in this file.
 4. novel_features - how many features were seen for the first time in
        this file.  Usually 0 for all except first file.
 5. file_name - file name.

The primary contents of the file is a table that lists key
about each feature.  The per-feature data fields are:

 0. seq - name of the sequence the feature is located on, e.g. chr1.
 1. start - start base of the feature (inclusive).
 2. end - end base of the feature (inclusive).
 3. region_length - number of bases in the feature .
 4. coverage_total - total bases the fell within the feature across
        all of the processed files.
 5. coverage_per_base - field 4 / field 3.
 6. mean - mean of the normalised values in field 8.
 7. std_dev - std_dev of the normalised values in field 8.
 8+ file0..n - normalised coverage per base, calculated by taking each
        file's base count dividing by the region length and then
        multiplying by the scaling factor for the file. This has the 
        effect of normalising all coverages to a nominal 100x coverage.


Here's a scaling example:

  1. Capture platform is 2 Mbases of sequence
  2. File contains 500 Mbases of sequence
  3. Scaling factor is 0.4 which is calculated as:
     100 / ( 500 / 2 )


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: tally_feature_coverage.pl 3023 2011-05-23 16:01:51Z j.pearson $


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
