package QCMG::PDF::IndirectObject::Font;

###########################################################################
#
#  Module:   QCMG::PDF::IndirectObject::Font
#  Creator:  John V Pearson
#  Created:  2012-06-06
#
#  Store contents of a PDF Stream Indirect Object that contains a single
#  PDF Font. This object extends Dictionary.  See section 5.5 Simple Fonts
#  on p412 of the PDF Reference sixth edition v1.7.
#
#  $Id: Font.pm 4664 2014-07-24 08:17:04Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use Carp qw( croak );
use vars qw( $SVNID $REVISION @ISA );


( $REVISION ) = '$Revision: 4664 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: Font.pm 4664 2014-07-24 08:17:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

@ISA = qw( QCMG::PDF::IndirectObject::Dictionary );

sub new {
    my $class = shift;
    my %params = @_;

    # Get $self from superclass
    my $self = $class->SUPER::new( %params );

    # Defaults for all Dictionary subclasses are enforced
    # at the Dictionary level.
    $self->_set_defaults;

    $self->{contents} = [];

    bless $self, $class;
}


1;

__END__


=head1 NAME

QCMG::PDF::IndirectObject::Font - A PDF Font object


=head1 SYNOPSIS

 use QCMG::PDF::Document;


=head1 DESCRIPTION

This module provides a very basic object for creating a PDF
font.  It is only designed to cope with "standard 14 fonts" defined by
PDF and which every PDF reader/viewer must supply without the PDF
document itself containing the full information about the fonts.  The 14
standard fonts are:

 Times-Roman       Helvetica              Courier              Symbol
 Times-Bold        Helvetica-Bold         Courier-Bold         ZapfDingbats
 Times-Italic      Helvetica-Oblique      Courier-Oblique
 Times-BoldItalic  Helvetica-BoldOblique  Courier-BoldOblique

For more details about fonts, see section 5.5 Simple Fonts
on p412 of the PDF Reference sixth edition v1.7.

=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: Font.pm 4664 2014-07-24 08:17:04Z j.pearson $


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
