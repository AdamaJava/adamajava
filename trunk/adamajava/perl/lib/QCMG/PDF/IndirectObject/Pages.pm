package QCMG::PDF::IndirectObject::Pages;

###########################################################################
#
#  Module:   QCMG::PDF::IndirectObject::Pages
#  Creator:  John V Pearson
#  Created:  2012-06-04
#
#  Store contents of a PDF Dictionary Indirect Object that contains
#  Pages. This object defines the brankches of a PDF document's page
#  tree while the leaves are Page objects. See section 3.6.2
#  Page Tree (p59 v1.7 PDF spec 6th ed).
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use Carp qw( croak confess );
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    my $self = { id         => undef,
                 generation => 0,
                 parent     => '',
                 entries    => {},
                 kids       => [] };

    bless $self, $class;
}


sub add_page {
    my $self   = shift;
    my $object = shift;

# We need to put this check back in place as soon as we have a proper
# Page object implemented.

    my $ref = ref($object);
    confess "add_page() cannot accept objects of type [$ref] - it only accepts ".
          'objects of type ' . 'QCMG::PDF::IndirectObject::Page or '.
          'QCMG::PDF::IndirectObject::Pages that have '.
          'already been added to a QCMG::PDF::Document'
        unless ( $ref =~ /^QCMG::PDF::IndirectObject::Page/ and 
                 defined($object->id) );

    # A Page must have a reference to its Pages parent
    $object->set_entry( 'Parent', $self->reference_string() );
    push @{ $self->{kids} }, $object;
}


# taken directly out of Dictionary.pm.  Need to get some inheritance
# going here.
sub set_entry {
    my $self  = shift;
    my $key   = shift;
    my $value = shift;

    if (ref($value)) {
        croak 'set_entry() can only accept values that are strings or '.
            'objects of type QCMG::PDF::IndirectObject::* that have '.
            'already been added to a QCMG::PDF::Document'
            unless ( ref($value) =~ /^QCMG::PDF::IndirectObject/ and
                     defined( $value->id));
    }

    croak 'set_entry() can only accept keys of type string'
        if (ref($key));

    $self->{entries}->{ $key } = $value;
}

sub get_entry {
    my $self  = shift;
    my $key   = shift;

    return undef unless defined $self->{entries}->{$key};
    return $self->{entries}->{$key};
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


sub kids {
    my $self = shift;
    return @{ $self->{kids} };
}


sub to_string {
    my $self = shift;

    die "object has no ID and so cannot be converted to text"
        unless defined( $self->id() );

    my $text = $self->id() .' '. $self->generation() ." obj\n<<\n" .
               "/Type /Pages\n";
    
    # /Parent should be present in all /Pages ojects except the root
    # /Pages of each Document (in /Catalog dictionary)
    if ($self->{parent}) {
        $text .= '/Parent '. $self->{parent} . "\n";
    }

    # Any other Indirect Objects should already be a part of the Document
    # so we do not render here, we just include a reference.

    foreach my $key (sort keys %{ $self->{entries} }) {
        my $value = $self->{entries}->{$key};
        if (ref( $value )) {
            $text .= '/'.$key .' '. $value->reference_string() . "\n";
        }
        else {
            $text .= "/$key $value\n";
        }
    }

    $text .= "/Kids [\n";

    # All of the kids should be Page or Pages objects so all we need to
    # do is create a list of the references

    my $tmp_text = '';
    foreach my $kid (@{ $self->{kids} }) {
        $tmp_text .= $kid->reference_string() . ' ';
        # Keep lines to 80 chars or less (255 is non-stream PDF max)
        if (length($tmp_text) > 80) {
            $text .= $tmp_text;
            $tmp_text = '';
        }
    }

    $text .= $tmp_text . "\n]\n" . "/Count " . scalar( @{ $self->{kids} } ) . "\n" .
             ">>\nendobj\n\n";

    return $text;
}


1;

__END__


=head1 NAME

QCMG::PDF::IndirectObject::Pages - A collection of PDF page objects


=head1 SYNOPSIS

 use QCMG::PDF::Document;


=head1 DESCRIPTION

This module provides a very basic object for creating a list of PDF
pages.  PDF considers pages to be a linked tree so every document must
have at least one of these objects to act as the root of the tree.
QCMG::PDF::Document invisibly creates the root for you and any time you
add an object of type Page or Pages, it is added to the default Pages
object (unless you have added it to a subpage).


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
