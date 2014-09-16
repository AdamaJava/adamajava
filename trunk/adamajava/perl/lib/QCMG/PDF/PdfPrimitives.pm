package QCMG::PDF::PdfPrimitives;

##############################################################################
#
#  Module:   QCMG::PDF::PdfPrimitives.pm
#  Creator:  John Pearson
#  Created:  2012-06-11
#
#  This perl module exports a series of routines that return PDF text
#  and function as drawing primitives.  These routines are conditionally exported
#  and all start with 'pp' to denote "PDF primtive" and are named in
#  CamelCase to mke them visually distinctive.
#
#  $Id: PdfPrimitives.pm 4664 2014-07-24 08:17:04Z j.pearson $
#
##############################################################################

use strict;
use warnings;
use Data::Dumper;
use Carp qw( croak carp );

use QCMG::QInspect::PdfPath;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION @ISA @EXPORT_OK );

( $REVISION ) = '$Revision: 4664 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: PdfPrimitives.pm 4664 2014-07-24 08:17:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

@ISA = qw(Exporter);
@EXPORT_OK = qw( ppRectanglePath ppStrokedRectangle ppClippedRectangle
                 ppFilledRectangle ppStrokedClippedRectangle );


sub ppStrokedClippedRectangle {
    my $xmin   = shift;
    my $ymin   = shift;
    my $width  = shift;
    my $height = shift;
    my $margin = shift;

    return ppStrokedRectangle($xmin,$ymin,$width,$height) .
           ppClippedRectangle($xmin,$ymin,$width,$height,$margin);
}


sub ppStrokedRectangle {
    my $xmin   = shift;
    my $ymin   = shift;
    my $width  = shift;
    my $height = shift;

    return ppRectanglePath($xmin,$ymin,$width,$height) ." s\n";
}


sub ppFilledRectangle {
    my $xmin   = shift;
    my $ymin   = shift;
    my $width  = shift;
    my $height = shift;

    return ppRectanglePath($xmin,$ymin,$width,$height) ." f\n";
}


sub ppClippedRectangle {
    my $xmin   = shift;
    my $ymin   = shift;
    my $width  = shift;
    my $height = shift;
    my $margin = shift;

    return ppRectanglePath($xmin+$margin,
                           $ymin+$margin,
                           $width-2*$margin,
                           $height-2*$margin) ." W n\n";
}


sub ppRectanglePath {
    my $xmin   = shift;
    my $ymin   = shift;
    my $width  = shift;
    my $height = shift;

    return join(' ', $xmin, $ymin, 'm',
                     $xmin+$width, $ymin, 'l',
                     $xmin+$width, $ymin+$height, 'l',
                     $xmin, $ymin+$height, 'l' );
}


1;
__END__


=head1 NAME

QCMG::QInspect::PdfPrimitives - Perl module for PDF generation


=head1 SYNOPSIS

 use QCMG::QInspect::PdfPrimtives qw( ppRectanglePath ... );


=head1 DESCRIPTION

This module provides a seried of "PDF Primitives" - routines that
directly retrun PDF text strings suitable for inclusion in a
QCMG::PDF::Stream.  You will need to explicitly import the primitives
you wish to use by adding them to your "use" statement.  These
primitives are not object-oriented.


=head1 PUBLIC METHODS

=over

=item B<ppStrokedClippedRectangle()>

=item B<ppStrokedRectangle()>

=item B<ppClippedRectangle()>

=item B<ppRectanglePath()>

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: PdfPrimitives.pm 4664 2014-07-24 08:17:04Z j.pearson $


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
