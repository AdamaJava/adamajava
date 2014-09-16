#!/usr/bin/perl -w

##############################################################################
#
#  Program:  summarise_markdups.pl
#  Author:   John V Pearson
#  Created:  2011-02-26
#
#  Script to traverse a directory and parse any Picard MarkDuplicates
#  metrics files (assumes a .metrics extension).
#
#  $Id: summarise_markdups.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
use Carp qw( carp croak );

use QCMG::Bioscope::Ini::Parameter;
use QCMG::Bioscope::Ini::ParameterCollection;
use QCMG::FileDir::Finder;
use QCMG::IO::MaReader;
use QCMG::Picard::MarkDupMetrics;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: summarise_markdups.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my $dir        = './';
    my $output     = 'markdups_summary.txt';
       $VERBOSE    = 0;
       $VERSION    = 0;
    my $help       = 0;
    my $man        = 0;

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'd|dir=s'              => \$dir,           # -d
           'o|output=s'           => \$output,        # -o
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

    print( "\nsummarise_markdups.pl  v$REVISION  [" . localtime() . "]\n",
           "   dir           $dir\n",
           "   output        $output\n",
           "   verbose       $VERBOSE\n\n" ) if $VERBOSE;

    my $finder = QCMG::FileDir::Finder->new( verbose => $VERBOSE );
    my @files = $finder->find_file( $dir, '\.metrics$' );

    my @records = ( );
    foreach my $file (@files) {
        push @records, parse_metrics_file( $file );
    }

    # Check for duplicate libraries
    my %libs = ();
    foreach my $ra_fields (@records) {
        # Skip reporting for records withou a lib
        if (! $ra_fields->[2]) {
            warn 'Unable to identify library so skipping reporting for [',
                 $ra_fields->[0], "]\n";
            next;
        }
        # Make sure each library only appears once
        if (exists $libs{ $ra_fields->[2] }) {
            print 'There are multiple records for library ',
                  $ra_fields->[2], "\n",
                  '  ', $libs{ $ra_fields->[2] }->[3], "\t",
                        $libs{ $ra_fields->[2] }->[10], "\t",
                        $libs{ $ra_fields->[2] }->[1], "\t",
                        $libs{ $ra_fields->[2] }->[0], "\n",
                  '  ', $ra_fields->[3], "\t",
                        $ra_fields->[10], "\t",
                        $ra_fields->[1], "\t",
                        $ra_fields->[0], "\n";

        }
        else {
            $libs{ $ra_fields->[2] } = $ra_fields;
        }
    }

    my $outfh = IO::File->new( $output, 'w' );
    die "Unable to open output file $output for writing: $!"
        unless defined $outfh;

    $outfh->print( join( "\t", qw( MetricsFile
                                   BAMFile
                                   Library
                                   LibType
                                   UnmappedReads 
                                   ReadPairsExamined
                                   UnpairedReadsExamined 
                                   UnpairedReadDuplicates
                                   ReadPairDuplicates 
                                   ReadPairOpticalDuplicates 
                                   PercentDuplication
                                   EstimatedLibrarySize ) ), "\n" );
    $outfh->print( join("\t", @{$_} ),"\n" ) foreach @records;
    $outfh->close;

    print( '[' . localtime() . "]  Finished.\n") if $VERBOSE;
}


sub parse_metrics_file {
    my $file = shift;
    my $md = QCMG::Picard::MarkDupMetrics->new( file    => $file,
                                                verbose => $VERBOSE );

    my $inputfile = strip_path( $md->cmdline->{params}->{INPUT} );
    my $metricsfile = strip_path( $file );
    my $library = '';
    if ($metricsfile =~ /[_\.\-](\d{8}_\w)\./) {
        $library = $1;
    }
    else {
        warn 'Cannot parse QCMG library ID from name of metrics file ',
             "[$metricsfile]\n";
    }
    return [ $metricsfile,
             $inputfile,
             $library,
             $md->metrics->{LIBRARY},
             $md->metrics->{UNMAPPED_READS},
             $md->metrics->{READ_PAIRS_EXAMINED},
             $md->metrics->{UNPAIRED_READS_EXAMINED},
             $md->metrics->{UNPAIRED_READ_DUPLICATES},
             $md->metrics->{READ_PAIR_DUPLICATES},
             $md->metrics->{READ_PAIR_OPTICAL_DUPLICATES},
             $md->metrics->{PERCENT_DUPLICATION},
             $md->metrics->{ESTIMATED_LIBRARY_SIZE} ]
}

sub strip_path {
    my $file = shift;
    $file =~ s/.*\///g;
    return $file;
}

__END__

=head1 NAME

summarise_markdups.pl - Perl script for summarising Picard MarkDuplicates
metrics files


=head1 SYNOPSIS

 summarise_markdups.pl [options]


=head1 ABSTRACT

This script searches a given directory tree for any files ending in
'.metrics' and parses them as Picard MarkDuplicates metrics files and
outputs a summary.

=head1 OPTIONS

 -d | --dir            directory of Picard MarkDuplicates reports
 -o | --output         output file name
 -v | --verbose        print diagnostic messages
      --version        print version number
 -? | --help           display help
 -m | --man            display man page


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: summarise_markdups.pl 4669 2014-07-24 10:48:22Z j.pearson $


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
