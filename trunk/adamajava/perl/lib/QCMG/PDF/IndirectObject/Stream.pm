package QCMG::PDF::IndirectObject::Stream;

###########################################################################
#
#  Module:   QCMG::PDF::IndirectObject::Stream
#  Creator:  John V Pearson
#  Created:  2012-06-05
#
#  Store contents of a PDF Content Stream Indirect Object. See section 3.2.7
#  Stream Objects, p60 of the PDF Reference sixth edition v1.7.
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
                 contents   => '' };

    # Note that /ProcSet is hard-wired as suggested in section 10.1
    # Procedure Sets of the PDF Reference sxith edition v1.7.

    bless $self, $class;
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


sub contents {
    my $self = shift;
    return $self->{contents};
}


sub add_contents {
    my $self = shift;
    my $text = shift;
    $self->{contents} .= $text;
}


sub to_string {
    my $self = shift;

    die "object has no ID and so cannot be converted to text"
        unless defined( $self->id() );

    # Calculating the extent and length of a Stream: from pp60-61 in PDF
    # Reference sixth edition v1.7:
    #
    # "The keyword stream that follows the stream dictionary should be
    #  followed by an end-of-line marker consisting of either a carriage
    #  return and a line feed or just a line feed, and not by a carriage 
    #  return alone. The sequence of bytes that make up a stream lie
    #  between the stream and endstream keywords; the stream dictionary
    #  specifies the exact number of bytes. It is recommended that there
    #  be an end-of-line marker after the data and before endstream;
    #  this marker is not included in the stream length."

    my $text = $self->id() .' '. $self->generation() ." obj\n" .
               '<< /Length ' . (length($self->contents())-1) . " >>\n" .
               "stream\n" .
               $self->contents() .
               "endstream\n" .
               "endobj\n\n";

    return $text;
}


1;

__END__


=head1 NAME

QCMG::PDF::IndirectObject::Stream - A PDF Stream Object


=head1 SYNOPSIS

 use QCMG::PDF::Document;


=head1 DESCRIPTION

This module provides a very basic object for creating PDF stream objects.
To use this object, you should instantiate it and immediately add it to
a document.  It can't get an ID without being added to a document and
you can;t even see a text representation of the object if it doesn't
have an ID.

See section 3.2.7 Stream Objects in the PDF Reference, sixth edition,
Version 1.7, November 2006, Adobe Systems Incorporated.


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
