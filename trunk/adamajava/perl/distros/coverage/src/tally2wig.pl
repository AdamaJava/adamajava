#!/usr/bin/perl -w

##############################################################################
#
#  Program:  tally2wig.pl
#  Author:   John V Pearson
#  Created:  2011-05-05
#
#  Quick-n-dirty conversion of output from tally_feature_coverage.pl
#  into wig format that can be converted into IGV-usable TDF by
#  igvtools.
#
#  $Id: tally2wig.pl 2859 2011-05-05 16:16:43Z j.pearson $
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

( $REVISION ) = '$Revision: 2859 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: tally2wig.pl 2859 2011-05-05 16:16:43Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my $infile     = '';
    my $wiggle     = '';
       $VERBOSE    = 0;
       $VERSION    = 0;
    my $help       = 0;
    my $man        = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'i|infile=s'           => \$infile,        # -i
           'w|wiggle=s'           => \$wiggle,        # -w
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

    print( "\ntally2wig.pl  v$REVISION  [" . localtime() . "]\n",
           "   infile        $infile\n",
           "   wiggle        $wiggle\n",
           "   verbose       $VERBOSE\n\n" ) if $VERBOSE;

    convert( $infile, $wiggle );
    print '['.localtime()."]  Finished.\n" if $VERBOSE;
}


sub convert {
    my $infile = shift;
    my $wiggle = shift;

    # This subroutine puts out the bait coverages in wiggle format:
    # http://genome.ucsc.edu/goldenPath/help/wiggle.html

    print '['.localtime()."]  Opening file $infile\n" if $VERBOSE;
    my $infh = IO::File->new( $infile, 'r' );
    die("Cannot open input file $infile for reading: $!\n")
        unless defined $infh;

    print '['.localtime()."]  Opening file $wiggle\n" if $VERBOSE;
    my $outfh = IO::File->new( $wiggle, 'w' );
    die("Cannot open output file $wiggle for writing: $!\n")
        unless defined $outfh;

    print $outfh "track type=wiggle_0 name=SureSelect_50Mb_baits\n";

    my %features = ();
    while (my $line = $infh->getline) {
        chomp $line;
        next if $line =~ /^#/; # skip comments
        my @fields = split /\t/, $line;

        # Push values into hash on chrom
        push @{ $features{ $fields[0] } }, \@fields;
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

    print Dumper \@sorted_seqs;

    foreach my $seq (@sorted_seqs) { 
        # Sort by start position with each sequence
        my @sorted_feats = map  { $_->[0] }
                           sort { $a->[1] <=> $b->[1] }
                           map  { [ $_, $_->[1] ] }
                           @{ $features{ $seq } };

        foreach my $feat (@sorted_feats) {
            print $outfh "variableStep chrom=", $feat->[0],
                         " span=", $feat->[3], "\n",
                         $feat->[1], ' ', $feat->[6], "\n";
        }
    }
}

__END__

=head1 NAME

tally2wig.pl - Perl script for converting a tally file to a wig


=head1 SYNOPSIS

 tally2wig.pl -i infile -o outfile


=head1 ABSTRACT

Quick-n-dirty conversion of output from tally_feature_coverage.pl into 
wig format that can be converted into IGV-usable TDF by igvtools.  Tyhe
wig format is documented at 
 http://genome.ucsc.edu/goldenPath/help/wiggle.html

=head1 OPTIONS

 -i | --infile        output file from tally_feature_converage.pl
 -w | --wiggle        same data as infile but in wiggle format for IGV
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

$Id: tally2wig.pl 2859 2011-05-05 16:16:43Z j.pearson $


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
