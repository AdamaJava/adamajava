#!/usr/bin/perl -w

##############################################################################
#
#  Program:  mark_bad_baits.pl
#  Author:   John V Pearson
#  Created:  2011-05-10
#
#  Read in output file from tally_feature_coverage.pl and based on
#  user-specified min/max values for coverage_per_base_per_file (column
#  7), convert labels from bait to badbait for any features with
#  coverages outside the specified limits.
#
#  $Id: mark_bad_baits.pl 2939 2011-05-15 12:41:15Z j.pearson $
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

use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 2939 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: mark_bad_baits.pl 2939 2011-05-15 12:41:15Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my $infile     = '';
    my $outfile    = '';
    my $covfile    = '';
    my $mincov     = '';
    my $maxcov     = '';
       $VERBOSE    = 0;
       $VERSION    = 0;
    my $help       = 0;
    my $man        = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'i|infile=s'           => \$infile,        # -i
           'o|outfile=s'          => \$outfile,       # -o
           'c|covfile=s'          => \$covfile,       # -c
           'n|mincov=s'           => \$mincov,        # -n
           'x|maxcov=s'           => \$maxcov,        # -x
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

    print( "\nmark_bad_baits.pl  v$REVISION  [" . localtime() . "]\n",
           "   infile        $infile\n",
           "   outfile       $outfile\n",
           "   covfile       $covfile\n",
           "   mincov        $mincov\n",
           "   maxcov        $maxcov\n",
           "   verbose       $VERBOSE\n\n" ) if $VERBOSE;

    my $rh_badbaits = read_coverages( $covfile, $mincov, $maxcov );
    process_file( $infile, $outfile, $rh_badbaits );

    print '['.localtime()."]  Finished.\n" if $VERBOSE;
}


sub read_coverages {
    my $covfile = shift;
    my $mincov  = shift;
    my $maxcov  = shift;

    my $fh = IO::File->new( $covfile, 'r' );
    die("Cannot open input file $covfile for reading: $!\n")
        unless defined $fh;
    
    my %badbaits = ();
    while (my $line = $fh->getline) {
        chomp $line;
        next if ($line =~ /^#/);   # skip headers

        my @fields = split /\t/, $line;
        if ( $fields[6] < $mincov ) {
            my $key =  $fields[0] .':'. $fields[1] .'-'.  $fields[2];
            $badbaits{ $key } = 'lowbait';
        }
        elsif ( $fields[6] > $maxcov ) {
            my $key =  $fields[0] .':'. $fields[1] .'-'.  $fields[2];
            $badbaits{ $key } = 'highbait';
        }
    }

    return \%badbaits;
}


sub process_file {
    my $infile      = shift;
    my $outfile     = shift;
    my $rh_badbaits = shift;

    my $infh = IO::File->new( $infile, 'r' );
    die("Cannot open input file $infile for reading: $!\n")
        unless defined $infh;
    
    my $outfh = IO::File->new( $outfile, 'w' );
    die("Cannot open output file $outfile for writing: $!\n")
        unless defined $outfh;

    my $ctr = 0;
    while (my $line = $infh->getline) {
        $ctr++;
        #last if $ctr > 100;
        # Copy any headers straight across
        $outfh->print( $line ) if ($line =~ /^#/);

        chomp $line;
        my @fields = split /\t/, $line;

        # If we found a match then edit it, otherwise print out the
        # input line and move on to the next baited region
        my $key =  $fields[0] .':'. $fields[3] .'-'.  $fields[4];
        if ( exists $rh_badbaits->{ $key } ) {
            # Edit bait record and output
            print "[$ctr] found bait $key\n";
            $fields[2] = $rh_badbaits->{ $key } if $fields[2] eq 'bait';
            $outfh->print( join("\t", @fields), "\n" );
            # Delete each found badbait so we can check at the end that
            # there are no entries left in the hash, i.e. no unfound baits
            delete $rh_badbaits->{ $key };
        }
        else {
            $outfh->print( "$line\n" );
        }
    }

    # Check to see if there were any unfound baits
    my @unfounds = sort keys %{ $rh_badbaits };
    if (@unfounds) {
        print "Baits in -i but not found in -c:\n";
        print " $_\n" foreach @unfounds;
    }

    $infh->close;
    $outfh->close;
}


__END__

=head1 NAME

mark_bad_baits.pl - Perl script for marking poorly performing baits


=head1 SYNOPSIS

 mark_bad_baits.pl -i <infile> -o <outfile> -n <mincov> -x <maxcov>


=head1 ABSTRACT

This script will take a GFF3 file of baits, the per-feature coverage
filef output by B<tally_feature_coverage.pl> and user-supplied
minimum and maximum acceptable average coverages.  It will edit any
records from the GFF3 file to change the label of records from bait to 
lowbait or highbait if the coverage is lower or higher than the
specified acceptable min and max numbers.  A new GFF3 file is written
that incorpoartes all of the records form the original but with the
edits in place so most records should still be labelled bait and with a
(hopefully) small number labelled as lowbait and highbait.

For Agilent SureSelect 50mb Human All Exon, the input file should be:

 $QCMG_SVN/QCMGProduction/exomes/
     SureSelect_All_Exon_50mb_with_annotation.hg19.gff3

A typical invocation might look like:

 /mark_bad_baits.pl -v 
   -i $QCMG_SVN/QCMGProduction/exomes/SureSelect_All_Exon_50mb_with_annotation.hg19.gff3 \
   -o jp1_badbait.gff3 -n 1 -x 200 -c per_feature_coverage_report.txt

If we check the output file using the command:

 cut -f 3 jp1_badbait.gff3 | sort | uniq -c

Then we see the following counts of the different labels in the output:
tally of labeled baits

 194094 bait
    473 highbait
  18816 lowbait

This looks like success!


=head1 OPTIONS

 -i | --infile        GFF3 file of baits
 -o | --outfile       GFF3 file of baits
 -n | --mincov        minimum acceptable coverage
 -x | --maxcov        maximum acceptable coverage
 -v | --verbose       print progress and diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: mark_bad_baits.pl 2939 2011-05-15 12:41:15Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2010,2011

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
