package QCMG::PDF::Document;

###########################################################################
#
#  Module:   QCMG::PDF::Document
#  Creator:  John V Pearson
#  Created:  2012-06-04
#
#  Writes out version 1.7 PDF documents.
#
#  $Id: Document.pm 4664 2014-07-24 08:17:04Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak );
use Data::Dumper;
use Digest::MD5 qw( md5_hex );
use vars qw( $SVNID $REVISION );

use QCMG::PDF::IndirectObject::Array;
use QCMG::PDF::IndirectObject::Dictionary;
use QCMG::PDF::IndirectObject::Font;
use QCMG::PDF::IndirectObject::Outlines;
use QCMG::PDF::IndirectObject::OutlineItem;
use QCMG::PDF::IndirectObject::Page;
use QCMG::PDF::IndirectObject::Pages;
use QCMG::PDF::IndirectObject::Resources;
use QCMG::PDF::IndirectObject::Stream;
use QCMG::PDF::OutlineMaker;
use QCMG::PDF::Util;

( $REVISION ) = '$Revision: 4664 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: Document.pm 4664 2014-07-24 08:17:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    my $self = { filename        => ($params{filename} ?
                                     $params{filename} : 'my.pdf'),
                 version         => '1.7',
                 objects         => {},
                 pages           => [],
                 fontdict        => undef,
                 next_obj_ctr    => 1,
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };
    bless $self, $class;
    
    my $catalog = QCMG::PDF::IndirectObject::Dictionary->new();
    $catalog->set_entry( 'Type', '/Catalog' );
    $catalog->set_entry( 'Version', $self->version() );
    $self->add_object( $catalog );

    my $info = QCMG::PDF::IndirectObject::Dictionary->new();
    $info->set_entry( 'Type', '/Info' );
    $info->set_entry( 'CreationDate', '('.localtime().')' );
    $self->add_object( $info );

    my $pages = QCMG::PDF::IndirectObject::Pages->new();
    $self->add_object( $pages );
    # Set user space units to default to A4
    $pages->set_entry( 'MediaBox', '[0 0 595 841]' );

    $catalog->set_entry( 'Info', $info );
    $catalog->set_entry( 'Pages', $pages );

    $self->add_default_fonts();

    return $self;
}


sub add_object {
    my $self = shift;
    my $object = shift;

    croak 'add_object() can only accept objects of type '.
          'QCMG::PDF::IndirectObject::* '.
          'but you passed an object of type '. ref($object)
        unless ( ref($object) =~ /^QCMG::PDF::IndirectObject/ );

    croak 'you cannot add an object twice' if defined($object->id);

    my $obj_id = $self->{next_obj_ctr};
    $object->_set_id( $obj_id );
    $self->{objects}->{ $obj_id } = $object;
    $self->{next_obj_ctr}++;

    return $obj_id;
}


sub add_page {
    my $self   = shift;
    my $object = shift;
    $self->_pages->add_page( $object );
}


sub add_outlines {
    my $self   = shift;
    my $object = shift;

    croak 'add_outline() can only accept objects of type '.
          'QCMG::PDF::IndirectObject::Outlines '.
          'but you passed an object of type '. ref($object)
        unless ( ref($object) eq 'QCMG::PDF::IndirectObject::Outlines' );

    croak 'you cannot add an object twice' if defined($object->id);

    $self->add_object( $object );
    $self->_catalog->set_entry( 'Outlines', $object );
}


sub _catalog {
    my $self   = shift;
    # The Catalog object is always object 1
    return $self->{objects}->{1};
}


sub _info {
    my $self   = shift;
    # The Info object is always object 2
    return $self->{objects}->{2};
}


sub _pages {
    my $self   = shift;
    # The root Pages object is always object 3
    return $self->{objects}->{3};
}


sub filename {
    my $self = shift;
    return $self->{filename} = shift if @_;
    return $self->{filename};
}


sub version {
    my $self = shift;
    return $self->{version};
}


sub objects {
    my $self = shift;
    return @{ $self->{objects} };
}


sub font_dictionary {
    my $self = shift;
    return $self->{fontdict};
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub _header {
    my $self = shift;

    my $header = "%PDF-1.7\n%" . chr(ord('Q')+128) .
                                 chr(ord('C')+128) .
                                 chr(ord('M')+128) .
                                 chr(ord('G')+128) . "\n" .
                 "% Created using the QCMG::PDF perl framework\n";
    return $header;
}


sub _trailer {
    my $self         = shift;
    my $object_count = shift;
    my $file_id      = shift;
    my $xref_start   = shift;

    # We will assume that Info is always object 2 and Catalog is always
    # object 1.  Everything falls apart if this is not true.

    my $trailer = "trailer\n" .
                  '<<' .
                  ' /Root 1 0 R' . 
                  ' /Info 2 0 R' .
                  " /Size $object_count" .
                  " /ID [ $file_id $file_id ] " .
                  ">>\n" .
                  "startxref\n" .
                  "$xref_start\n" .
                  "%%EOF\n";

    return $trailer;
}


# These methods all insert values into the dictionary for /Info (object 1):
sub title { 
    my $self = shift;
    return $self->_info->entity( '/Title', shift ) if @_;
    return $self->_info->entity( '/Title' );
}

# sub author { }
# sub subject { }
# sub keywords { }
# sub creator { }
# sub producer { }
# We should also have a non-user-settable value in the Info object for:
# sub creationdate { }


sub to_string {
    my $self = shift;

    my $text = $self->_header() . "\n";
   
    my $xref = "xref\n" .
               '0 ' . $self->{next_obj_ctr} . "\n" .
               "0000000000 65535 f \n";

    my $byte_offset = length($text) + 1;
    foreach my $key (sort keys %{ $self->{objects} }) {
         $xref .= substr( '0000000000'.$byte_offset, -10 ) ." 00000 n \n";
         my $obj_text = $self->{objects}->{$key}->to_string();
         $text .= $obj_text;
         $byte_offset += length( $obj_text );
    }

    $text .= $xref;

    # The list of items to be used in the MD5 hash is roughly based on
    # the list suggested in section 10.3 of the v1.7 PDF spec, p848.
    my $id = '<' .
             md5_hex( localtime() .
                      length($text) .
                      $self->{objects}->{2}->to_string() ) .
             '>';

    my $trailer = $self->_trailer( $self->{next_obj_ctr},
                                   $id,
                                   $byte_offset );

    return $text ."\n". $trailer;
}


sub strlen {
    my $self   = shift;
    my $font   = shift;
    my $fsize  = shift;
    my $string = shift;
    return str_width( $font, $fsize, $string );
}


sub add_default_fonts {
    my $self = shift;

    my $fontdict  = QCMG::PDF::IndirectObject::Dictionary->new();
    $self->add_object( $fontdict );

    foreach my $ctr (1..12) {
        my $font = _font( $ctr );
        $self->add_object( $font );
        $fontdict->set_entry( 'F'.$ctr, $font->reference_string() );
    }
    $self->{fontdict} = $fontdict;
}


sub _font {
    my $num = shift;

    my $font = QCMG::PDF::IndirectObject::Font->new();
    $font->set_entry( 'Subtype', '/Type1' );

    # This list must stay in sync with %FONTS and %FONT_WIDTHS in
    # QCMG::PDF::Util !!!

    if ($num == 1) {
        $font->set_entry( 'BaseFont', '/Courier' );
    }
    elsif ($num == 2) {
        $font->set_entry( 'BaseFont', '/Courier-Bold' );
    }
    elsif ($num == 3) {
        $font->set_entry( 'BaseFont', '/Courier-Oblique' );
    }
    elsif ($num == 4) {
        $font->set_entry( 'BaseFont', '/Courier-BoldOblique' );
    }
    elsif ($num == 5) {
        $font->set_entry( 'BaseFont', '/Times-Roman' );
    }
    elsif ($num == 6) {
        $font->set_entry( 'BaseFont', '/Times-Bold' );
    }
    elsif ($num == 7) {
        $font->set_entry( 'BaseFont', '/Times-Italic' );
    }
    elsif ($num == 8) {
        $font->set_entry( 'BaseFont', '/Times-BoldItalic' );
    }
    elsif ($num == 9) {
        $font->set_entry( 'BaseFont', '/Helvetica' );
    }
    elsif ($num == 10) {
        $font->set_entry( 'BaseFont', '/Helvetica-Bold' );
    }
    elsif ($num == 11) {
        $font->set_entry( 'BaseFont', '/Helvetica-Oblique' );
    }
    elsif ($num == 12) {
        $font->set_entry( 'BaseFont', '/Helvetica-BoldOblique' );
    }
    else {
        $font->set_entry( 'BaseFont', '/Times-Roman' );
    }

    return $font;
}


### Generators ###

# The following methods all create new objects from the
# QCMG::PDF::IndirectObject hierachy.  The advantage of having them
# generated out of $doc is that the add_object() methods can be run
# behind the scenes so there is no requirement for the use to remember
# to do this.

sub new_page {
    my $self = shift;
    my $obj  = QCMG::PDF::IndirectObject::Page->new();
    $self->add_object( $obj );
    $self->add_page( $obj );
    return $obj;
}


sub new_resources {
    my $self = shift;
    my $obj  = QCMG::PDF::IndirectObject::Resources->new();
    $self->add_object( $obj );
    return $obj;
}


sub new_resources_with_fonts {
    my $self = shift;
    my $obj  = $self->new_resources();
    $obj->set_entry( 'Font', $self->font_dictionary->reference_string() );
    return $obj;
}



1;

__END__


=head1 NAME

QCMG::PDF::Document - A PDF Document


=head1 SYNOPSIS

 use QCMG::PDF::Document;


=head1 DESCRIPTION

This module provides a very basic object for creating PDF documents.  It
does not attempt to hide the complexity of PDF files and to make
effective use of this class, you will almost certainly need to have some
familiarity with the PDF Reference, sixth edition, Version 1.7, November
2006, Adobe Systems Incorporated.


=head1 PUBLIC METHODS

=over

=item B<new()>

=back


=head2 Generators

These methods all create new PDF objects and pre-register the object
with the document.

=over

=item B<new_page()>

Returns an object of type QCMG::PDF::IndirectObject::Page.

=item B<new_resources()>

Returns an object of type QCMG::PDF::IndirectObject::Resources.

=item B<new_resources_with_fonts()>

As per B<new_resources()> except the page has a /Font entry already
in place pointing to a dictionary that has definitions for the 12
standard PDF text fonts.

=back


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: Document.pm 4664 2014-07-24 08:17:04Z j.pearson $


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
