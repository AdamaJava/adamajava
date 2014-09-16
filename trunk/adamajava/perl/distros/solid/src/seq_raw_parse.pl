#!/usr/bin/perl -w

##############################################################################
#
#  Program:  seq_raw_parse.pl
#  Author:   John V Pearson
#  Created:  2010-10-13
#
#  Find directories and files.
#
#  $Id: seq_raw_parse.pl 4670 2014-07-24 10:50:59Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
use Carp qw( carp croak );

use QCMG::FileDir::Finder;
use QCMG::FileDir::SeqRawParser;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 4670 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: seq_raw_parse.pl 4670 2014-07-24 10:50:59Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my $dir        ='';
       $VERBOSE    = 0;
       $VERSION    = 0;
    my $help       = 0;
    my $man        = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'd|dir=s'              => \$dir,           # -d
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

    print( "\nseq_raw_parse.pl  v$REVISION  [" . localtime() . "]\n",
           "   dir           $dir\n",
           "   verbose       $VERBOSE\n\n" ) if $VERBOSE;

    my $raw = QCMG::FileDir::SeqRawParser->new( verbose => $VERBOSE );
    $raw->process( $dir );
    print $raw->report;
}



__END__

=head1 NAME

seq_raw_parse.pl - Perl script for parsing seq_raw directories


=head1 SYNOPSIS

 seq_raw_parse.pl


=head1 ABSTRACT

This script will take a seq_raw directory and parse it and its contents
to find the full run name, the primary directories and the names of all
.csfasta files, among other things.


=head1 OPTIONS

 -d | --dir           directory to be searched
 -v | --verbose       print progress and diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

There are 2 general patterns for run directory names with the only
difference being whether or not barcodes were used:

 .../<run_name>/<segment>/.../primary.\d+/reads/<csfasta_filename>
 .../<run_name>/<segment>/.../<barcode>/primary.\d+/reads/<csfasta_filename>

and where '...' represents one or more directories.

For this program, we will introduce the concept of a mapset which is a
collection of reads from a SOLiD slide which logically should be aligned
as a unit.  The reads that make up a mapset are:

  1. the whole slide for non barcoded, non-segmented runs
  2. for barcoded runs, each barcode defines a mapset
  3. for segmented runs, each segment defines a mapset
  4. for barcoded segmented runs each barcode/segment combination is a mapset

This does not change for frag, fragPE or LMP runs although each run type
will have different tags and so will have more or less CSFASTA files.

One minor wrinkle is that if a run is segmented or barcoded (or both)
and a single library was loaded onto multiple segments or barcodes, then
those segments/barcodes shold be considered to be separate mapsets.
Post-alignment, BAMs from multiple mapsets for the same library 
should be merged and de-dedup'd.

So what information do we need to identify for each mapset?

 1. Mapset_id - this will have to be determined from a query into
        icgc_runs_log based on the values we find for the rest of
        the columns.
 2. Run_name
 3. Barcode (if any)
 4. Segment (if any)
 5. Tags, and for each tag (F3,R3,F5-P2,F5-BC,BC,etc):
    a. primary directory
    b. full path (from run_name down) to directory containing CSFASTA/QUAL
    c. CSFASTA filename
    d. CSFASTA size
    e. QUAL filename
    f. QUAL size


W
138000413_20100731_1_MP


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: seq_raw_parse.pl 4670 2014-07-24 10:50:59Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2010

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
