package QCMG::QInspect::Annots2Pdf;

##############################################################################
#
#  Module:   QCMG::QInspect::Annots2Pdf.pm
#  Creator:  John Pearson
#  Created:  2012-06-12
#
#  This perl module renders annotations from a QiRecord object and creates
#  a PDF content stream for inclusion in a PDF document Page.
#
#  $Id: Annots2Pdf.pm 4665 2014-07-24 08:54:04Z j.pearson $
#
##############################################################################

use strict;
use warnings;
use Data::Dumper;
use Carp qw( croak carp );

use QCMG::PDF::PdfPrimitives qw( ppRectanglePath ppStrokedRectangle
                                 ppClippedRectangle
                                 ppStrokedClippedRectangle );
use QCMG::PDF::Util qw( str_width );
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4665 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: Annots2Pdf.pm 4665 2014-07-24 08:54:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    my $self = { annotations  => ( $params{annotations} ?
                                   $params{annotations} : undef ),
                 verbose      => ( $params{verbose} ?
                                   $params{verbose} : 0 ),
               };
    bless $self, $class;
}


sub verbose {
    my $self = shift;
    return $self->{'verbose'} = shift if @_;
    return $self->{'verbose'};
}


sub create_pdf_stream {
    my $self     = shift;
    my $doc      = shift;
    my $annorder = shift;
    my $pdfre    = shift;

    # Determine limits of plotting area - allows for outer stroked
    # rectangle plus inner clipped rectangle plus a couple of units
    my $plot_x_min = $pdfre->[0]+3;
    my $plot_y_min = $pdfre->[1]+3;
    my $plot_x_max = $pdfre->[2] + $plot_x_min -6;
    my $plot_y_max = $pdfre->[3] + $plot_y_min -6;

    my $spacer = 15;

    # Create stream PDF object and draw a black border
    my $stream = QCMG::PDF::IndirectObject::Stream->new();
    $doc->add_object( $stream );
    $self->{stream} = $stream;

    # We are going to futz with a lot of stuff so let's establish a new
    # "graphics state" as the first thing we do (and end with a "Q")
    $stream->add_contents( "q\n" );

    # We need to set up a clipping path
    my $path = ppClippedRectangle( @{ $pdfre }, 2 );
    $stream->add_contents( $path );

    # Now add the PDF for the Annots themselves
    my @keys = split /\,/, $annorder;

    # F9 = Helvetica
    my @key_lengths = reverse sort map { str_width('F9', 12, $_) } @keys;
    my @str_lengths = reverse sort
                      map { str_width('F9', 12, $self->{annotations}->{$_}) }
                      @keys;

    my $ypos = $plot_y_max - $spacer;
    foreach my $key (@keys) {
        my $pdf_text = $self->_plot_annot( $plot_x_min+5,
                                           $key_lengths[0]+5,
                                           $ypos, $key );
        $stream->add_contents( $pdf_text );
        $ypos -= $spacer;
    }

    $stream->add_contents( "Q\n" );

    return $stream;
}


sub _plot_annot {
    my $self       = shift;
    my $key_x_offs = shift;
    my $val_x_offs = shift;
    my $ypos       = shift;
    my $key        = shift;

    # First Td after BT sets the initial coords in text space and
    # subsequent Td ops are offstes relative to the current pos.

    my $text = "BT /F9 12 Tf $key_x_offs $ypos Td (". $key .") Tj\n" .
               "$val_x_offs 0 Td (" .
               $self->{annotations}->{$key} .") Tj ET\n";

    return $text;
}


1;
__END__


=head1 NAME

QCMG::QInspect::Annots2Pdf - Perl module for plotting text as PDF


=head1 SYNOPSIS

 use QCMG::QInspect::Annots2Pdf;


=head1 DESCRIPTION

This module will take a hash of SAM file annotations and plot them.


=head1 PUBLIC METHODS

=over

=item B<new()>

=item B<create_pdf_stream()>

=item B<verbose()>

 $ini->verbose();

Returns the verbose status for this object where 0 sets verbose off 
and any other value (traditionally 1) sets verbose mode on.

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: Annots2Pdf.pm 4665 2014-07-24 08:54:04Z j.pearson $


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
