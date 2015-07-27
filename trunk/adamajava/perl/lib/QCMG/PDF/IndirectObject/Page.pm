package QCMG::PDF::IndirectObject::Page;

###########################################################################
#
#  Module:   QCMG::PDF::IndirectObject::Page
#  Creator:  John V Pearson
#  Created:  2012-06-04
#
#  Store contents of a PDF Stream Indirect Object that contains a single
#  PDF page. This object extends Dictionary with an array of Stream 
#  objects to hold the page contents.  See section 3.6.2 Page Tree
#  on p143 of the PDF Reference sixth edition v1.7.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use Carp qw( croak );
use vars qw( $SVNID $REVISION @ISA );


( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
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


sub add_content_stream {
    my $self   = shift;
    my $object = shift;

    croak 'add_content_stream() can only accept objects of type ' .
          'QCMG::PDF::IndirectObject::Stream that have '.
          'already been added to a QCMG::PDF::Document'
        unless ( ref($object) eq 'QCMG::PDF::IndirectObject::Stream' and 
                 defined($object->id) );

    push @{ $self->{contents} }, $object;
}


sub to_string {
    my $self = shift;

    die "object has no ID and so cannot be converted to text"
        unless defined( $self->id() );

    my $text = $self->id() .' '. $self->generation() ." obj\n<<\n";

    # Any other Indirect Objects should already be a part of the Document
    # so we do not render them here, we just include a reference.

    foreach my $key (sort keys %{ $self->{entries} }) {
        my $value = $self->{entries}->{$key};
        # Skip any undef entries
        next unless defined $value;
        if (ref( $value )) {
            $text .= '/'.$key .' '. $value->reference_string() . "\n";
        }
        else {
            $text .= "/$key $value\n";
        }
    }

    # All of the contents should be Stream objects so so all we need to
    # do is create a list of the references

    $text .= "/Contents [\n";

    my $tmp_text = '';
    foreach my $stream (@{ $self->{contents} }) {
        $tmp_text .= $stream->reference_string() . ' ';
        # Keep lines to 80 chars or less (255 is non-stream PDF max)
        if (length($tmp_text) > 80) {
            $text .= $tmp_text;
            $tmp_text = '';
        }
    }

    $text .= $tmp_text . "\n]\n" .
             ">>\nendobj\n\n";

    return $text;
}


1;

__END__


=head1 NAME

QCMG::PDF::IndirectObject::Page - A PDF page object


=head1 SYNOPSIS

 use QCMG::PDF::Document;


=head1 DESCRIPTION

This module provides a very basic object for creating a PDF
page.  You can add resources and contents to a Page.


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id$


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
