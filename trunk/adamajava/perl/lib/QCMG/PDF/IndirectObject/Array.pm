package QCMG::PDF::IndirectObject::Array;

###########################################################################
#
#  Module:   QCMG::PDF::IndirectObject::Array
#  Creator:  John V Pearson
#  Created:  2012-06-04
#
#  Store contents of a PDF Array Indirect Object. See section 3.2.5
#  Array Objects, p58 of the sixth edition of the v1.7 PDF spec.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use Carp qw( croak );
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    my $self = { id         => undef,
                 generation => 0,
                 objects    => [] };

    bless $self, $class;
}


sub add_object {
    my $self = shift;
    my $object = shift;

    if (ref($object)) {
        die 'add_object() can only accept strings or objects of type '.
            'QCMG::PDF::IndirectObject::* that have already been added to '.
            'a QCMG::PDF::Document'
            unless ( ref($object) =~ /^QCMG::PDF::IndirectObject/ and
                     defined( $object->id ));
    }

    push @{ $self->{objects} }, $object;

    return $object;
}


# There is intentionally no setter for this attribute as the only time
# the ID will be set is when the object is added to a QCMG::PDF::Document
# object which will use the _set_id() private method.
sub id {
    my $self = shift;
    return $self->{id};
}


# This private method is only for use by a QCMG::PDF::Document object
sub _set_id {
    my $self = shift;
    return $self->{id} = shift;
}


sub generation {
    my $self = shift;
    return $self->{generation} = shift if @_;
    return $self->{generation};
}


sub reference_string {
    my $self = shift;

    die "object has no ID and so cannot be referenced"
        unless defined( $self->id() );

    return $self->id() . ' ' .
           $self->generation() . ' R';
}


sub objects {
    my $self = shift;
    return @{ $self->{objects} };
}


sub to_string {
    my $self = shift;

    die "object has no ID and so cannot be converted to text"
        unless defined( $self->id() );

    my $text = $self->id() .' '. $self->generation() ." obj\n[\n";

    # Any other Indirect Objects should already be a part of the Document
    # so we do not render it here, we just include a reference.

    foreach my $obj (self->objects()) {
        if (ref($obj) and ref($obj) =~ /QCMG::PDF::IndirectObject/) {
            $text .= ' ' . $obj->reference_string() . "\n";
        }
        elsif (ref($obj)) {
            croak 'to_string() cannot cope with direct objects of type ' .
                  ref($obj);
        }
        else {
            $text .= $obj . "\n";
        }
    }

    $text .= "]\nendobj\n\n";

    return $text;
}


1;

__END__


=head1 NAME

QCMG::PDF::IndirectObject::Array - A PDF Array Object


=head1 SYNOPSIS

 use QCMG::PDF::Document;


=head1 DESCRIPTION

This module provides a very basic object for creating PDF array objects.
To use this object, you shoudl instantiate it and immediately add it to
a document.  It can't get an ID without being added to a document and
you can;t even see a text representation of the object if it doesn't
have an ID.

See section 3.2.5 Array Objects in the PDF Reference, sixth edition,
Version 1.7, November 2006, Adobe Systems Incorporated.


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012
Copyright (c) John Paerson 2012

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
