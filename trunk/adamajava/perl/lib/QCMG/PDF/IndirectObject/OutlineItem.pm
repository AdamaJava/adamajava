package QCMG::PDF::IndirectObject::OutlineItem;

###########################################################################
#
#  Module:   QCMG::PDF::IndirectObject::OutlineItem
#  Creator:  John V Pearson
#  Created:  2012-06-12
#
#  Store contents of a PDF Stream OutlineItem that acts as a node in the
#  the Outline object hierachy.  See section 8.2 Document-Level
#  navigation on p581 of the PDF Reference sixth edition v1.7.
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


# We need our own custom to_string() because /Dest needs to be output in
# a special way and /Title is a string

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
        if ($key eq 'Dest') {
            $text .= "/$key [ $value /Fit ]\n";
        }
        elsif ($key eq 'Title') {
            $text .= "/$key ($value)\n";
        }
        elsif (ref( $value )) {
            $text .= '/'.$key .' '. $value->reference_string() . "\n";
        }
        else {
            $text .= "/$key $value\n";
        }
    }

    $text .= ">>\nendobj\n\n";

    return $text;
}


1;

__END__


=head1 NAME

QCMG::PDF::IndirectObject::OutlineItem - A node in the Outlines hierachy


=head1 SYNOPSIS

 use QCMG::PDF::Document;


=head1 DESCRIPTION

This module provides a node object for the Outlines hierachy.


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
