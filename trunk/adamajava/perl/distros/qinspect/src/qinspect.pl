#!/usr/bin/perl -w

##############################################################################
#
#  Program:  qinspect.pl
#  Author:   John V Pearson
#  Created:  2012-06-08
#
#  This script reads one or more SAM-like "qi" files as created by
#  "qmaftools qinspect" or other means.  It interprets the qi file and
#  creates a PDF output.
#
#  $Id: qinspect.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
use PDF::Reuse;

use QCMG::IO::QiReader;
use QCMG::Util::QLog;
use QCMG::PDF::Document;
use QCMG::QInspect::Annots2Pdf;
use QCMG::QInspect::Sam2Pdf;
use QCMG::PDF::PdfPrimitives qw( ppStrokedRectangle );

use vars qw( $CVSID $REVISION $VERSION $VERBOSE $LOGFH );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $CVSID ) = '$Id: qinspect.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my @infiles        = ();
    my $outfile        = 'qi.pdf';
    my $logfile        = '';
    my $annorder       = 'SOURCE,BAM,RANGE';
    my $title          = 'qInspect PDF Report';
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
           'o|outfile=s'          => \$outfile,       # -o
           'l|logfile=s'          => \$logfile,       # -l
           'a|annorder=s'         => \$annorder,      # -a
           't|title=s'            => \$title,         # -t
           'v|verbose+'           => \$VERBOSE,       # -v
           'version!'             => \$VERSION,       # --version
           'h|help|?'             => \$help,          # -?
           'man|m'                => \$man            # -m
           );

    if ($VERSION) {
        print "$CVSID\n";
        exit;
    }
    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;

    qlogfile($logfile) if $logfile;
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $cmdline\n");

    # Allow for ,-separated lists of infiles
    @infiles = map { split /\,/,$_ } @infiles;

    die "No input files specified" unless scalar @infiles;

    my $ra_ranges = process_infiles( \@infiles );

    create_pdf( $outfile, $ra_ranges, $annorder );

    qlogend;
}


sub process_infiles {
    my $ra_infiles = shift;

    my @recs = ();
    foreach my $infile (@{$ra_infiles}) {
        my $ra_recs = process_infile( $infile );
        push @recs, @{ $ra_recs };
    }

    return \@recs;
}


sub process_infile {
    my $infile = shift;

    qlogprint( {l=>'INFO'}, "Processing input file $infile\n" );

    my $qir = QCMG::IO::QiReader->new( filename => $infile );
    my @recs = $qir->records();

    qlogprint( {l=>'INFO'}, 'Read ',scalar(@recs), " records\n" );

    return \@recs;
}


sub string_box {
    my $x      = shift;
    my $y      = shift;
    my $font   = shift;
    my $fsize  = shift;
    my $string = shift;
    my $doc    = shift;

    my $len = $doc->strlen( $font, $fsize, $string );

    my $text = "0.3 0.2 0.8 rg\n" .
               ($x-$fsize)      .' '. ($y-$fsize) .' m '.
               ($x+$fsize+$len) .' '. ($y-$fsize) .' l '.
               ($x+$fsize+$len) .' '. ($y+1.5*$fsize) .' l '.
               ($x-$fsize)      .' '. ($y+1.5*$fsize) ." l b\n" .
               "0 0 0 rg\n" .
               "BT /$font $fsize Tf $x $y Td ($string) Tj ET\n";

    return $text;
}


sub create_pdf {
    my $outfile   = shift;
    my $ra_ranges = shift;
    my $annorder  = shift;

    my $doc = QCMG::PDF::Document->new();

    # A blank page - will be the title page in future
    my $title_page = $doc->new_page();

    # Get a Resources object with all default fonts specified
    my $resource = $doc->new_resources_with_fonts();
    
    # Create an "Outlines" object to take the per-page annotations
    my $outline = QCMG::PDF::OutlineMaker->new( doc => $doc );
    my @annots = split /\,/, $annorder;

    # Create a page for each QiRecord object
    foreach my $qi (@{ $ra_ranges }) {

        # Get a new page ready
        my $page = $doc->new_page();
        $page->set_entry( 'Resources', $resource );

        # Write out the annotations
        my $ann = QCMG::QInspect::Annots2Pdf->new(
                       annotations => $qi->annotations(),
                       verbose     => $VERBOSE );
        my $astream = $ann->create_pdf_stream( $doc,
                                               $annorder,
                                               [5,740,585,97] );
        $page->add_content_stream( $astream );

        # Create a PDF content stream to draw the reads
        my $sampdf = QCMG::QInspect::Sam2Pdf->new(
                           sam_records => $qi->sam_records(),
                           verbose     => $VERBOSE );
        my $rstream = $sampdf->create_pdf_stream( $doc,
                                                  $qi->get_annot( 'RANGE' ),
                                                  [5,5,400,700] );

        # Until we get the rest of the display sorted, we'll just add
        # some border boxes to the reads stream.
        # A4 = 8.27x11.69 inches = 595x842
        $rstream->add_contents( ppStrokedRectangle( 5,5,400,730 ) );
        $rstream->add_contents( ppStrokedRectangle( 5,740,585,97 ) );
        $rstream->add_contents( ppStrokedRectangle( 410,5,180,730 ) );

        $page->add_content_stream( $rstream );

        # Put the page into the data structure that will be used to
        # generate the Outlines hierachy
        my %params = map { $_ => $qi->get_annot( $_ ) } @annots;
        $params{Dest} = $page->reference_string();
        $outline->add_annot( %params );
    }

    # Generate objects that for the Outline while specifying the order
    # of the outline hierachy
    $outline->process( @annots );

    my $outfh = IO::File->new( $outfile, 'w' ); 
    die "unable to open file $outfile for writing: $!\n"
        unless defined $outfh;
    $outfh->print( $doc->to_string );
    $outfh->close;
}


__END__

=head1 NAME

qinspect.pl - Perl script for creating PDF variant reports


=head1 SYNOPSIS

 qinspect.pl [options]


=head1 ABSTRACT

This script takes one or more "qi" input files and creates a single PDF
file with a page to visualise each region found in the qi files.  The
pages are arrangeas input the name of a file containing ...


=head1 OPTIONS

 -i | --infile        name of input file(s); Required!
 -o | --outfile       output filename; default=qi.pdf
 -l | --logfile       logfile name
 -a | --annorder      default = SOURCE,BAM,RANGE
 -t | --title         report title
 -v | --verbose       print progress and diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

B<qinspect.pl> was designed to support the ICGC Pancreatic Cancer
sequencing project.

B<qinspect.pl> produces a single PDF file as output.


=head2 Commandline Options

=over

=item B<-i | --infile>

Multiple input files are allowed and may be specified 2 ways.  Firstly,
the B<-i> option may be specified multiple times, and secondly each
B<-i> may be followed by a comma-separated list (no spaces!) of
filenames.  You may also mix the 2 modes.

=item B<-l | --logfile>

By default, error and diagnostic messages are output to STDOUT but if
a logfile is specified, the messages will be written to the logfile 
instead.  This can be particularly useful if the script is intended to
be made available via a web wrapper.

=item B<-o | --outfile>

The output file is a v1.7 PDF file suitable for viewing or printing.
PDF looks like text but is actually binary so you must NOT edit this
file or let it go through line-ending conversion etc.  Treat it like any
other PDF and you'll be golden.
If no name is supplied for the output file then the default
(gi.pdf) is used.

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

=head2 Example

The following usage example will do blah

  template -i blah.fa -c blah -s 15736064

B<N.B.> The spaces between the options (B<-i>, B<-c> etc) and the actual
values are compulsory.  If you do not use the spaces then the value will
be ignored.


=head1 SEE ALSO

=over 2

=item perl

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:jpearson@tgen.org>

=back


=head1 VERSION

$Id: qinspect.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012
Copyright (c) John Pearson 2012

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
