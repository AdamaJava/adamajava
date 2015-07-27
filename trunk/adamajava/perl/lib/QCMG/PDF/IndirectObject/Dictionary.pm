package QCMG::PDF::IndirectObject::Dictionary;

###########################################################################
#
#  Module:   QCMG::PDF::IndirectObject::Dictionary
#  Creator:  John V Pearson
#  Created:  2012-06-04
#
#  Store contents of a PDF Dictionary Indirect Object. See section 3.2.6
#  Dictionary Objects, p59 of the sixth edition of the v1.7 PDF spec.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use Carp qw( croak );
use vars qw( $SVNID $REVISION %ENTRIES );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;

BEGIN {
    # Define package globals.  Notes: 
    # 1. Page intentionally does not include /Contents as an entry because
    #    we need to handle this in a custom fashion

    %ENTRIES = ( Resources => { ExtGState => undef,
                                ColorSpace => undef,
                                Pattern => undef,
                                Shading => undef,
                                XObject => undef,
                                Font => undef,
                                ProcSet => '[/PDF/Text/ImageB/ImageC/ImageI]',
                                Properties => undef },
                 Page      => { Type => '/Page',
                                Parent => undef,
                                MediaBox => undef,
                                CropBox => undef,
                                Resources => undef },
                 Font      => { Type => '/Font',
                                Subtype => undef,
                                BaseFont => undef,
                                Encoding => undef },
                 Outlines  => { Type => '/Outlines',
                                First => undef,
                                Last => undef,
                                Count => undef },
                 OutlineItem => { Title => undef,
                                  Parent => undef,
                                  Next => undef,
                                  Prev => undef,
                                  First => undef,
                                  Last => undef,
                                  Dest => undef,
                                  Count => undef },
               );
}


sub new {
    my $class = shift;
    my %params = @_;

    my $self = { id         => undef,
                 generation => 0,
                 entries    => {} };

    bless $self, $class;
}


sub set_entry {
    my $self  = shift;
    my $key   = shift;
    my $value = shift;

    if (ref($value)) { 
        croak 'set_entry() can only accept values that are strings or '.
            'objects of type QCMG::PDF::IndirectObject::* that have '.
            'already been added to a QCMG::PDF::Document'
            unless ( ref($value) =~ /^QCMG::PDF::IndirectObject/ and 
                     defined( $value->id ) );
    }                

    croak 'set_entry() can only accept keys of type string'
        if (ref($key));

    # If we are a Dictionary subclass then work out what sort and
    # enforce the allowable entries using %ENTRIES
    if (ref($self) !~ /::Dictionary$/) {
        my $class = ref($self);
        $class =~ s/^.*:://g;
        croak "set_entry() - entry [$key] is not valid for class [$class]"
            unless exists $ENTRIES{$class}->{$key};
    }

    $self->{entries}->{ $key } = $value;
}


sub get_entry {
    my $self  = shift;
    my $key   = shift;
            
    return undef unless defined $self->{entries}->{$key};
    return $self->{entries}->{$key};
}           


# Every subclass should add their defaults into %ENTRIES and then call
# this routine from their new() so the default entries all get set.

sub _set_defaults {
    my $self  = shift;

    # If we are a Dictionary subclass then work out what sort and
    # set any defaults that were defined in %ENTRIES
    if (ref($self) !~ /::Dictionary$/) {
        my $class = ref($self);
        $class =~ s/^.*:://g;
        foreach my $key (keys %{ $ENTRIES{$class} }) {
            my $value = $ENTRIES{$class}->{$key};
            next unless defined $value;
            $self->set_entry( $key, $value );
        }
    }
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


sub entries {
    my $self = shift;
    return %{ $self->{entries} };
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

    $text .= ">>\nendobj\n\n";

    return $text;
}


1;

__END__


=head1 NAME

QCMG::PDF::IndirectObject::Dictionary - A PDF Dictionary Object


=head1 SYNOPSIS

 use QCMG::PDF::Document;


=head1 DESCRIPTION

This module provides a very basic object for creating PDF dictionary
objects which are associative arrays analogous to perl hashes.
To use this object, you should instantiate it and immediately add it to
a document.  It can't get an ID without being added to a document and
you can't even see a text representation of the object if it doesn't
have an ID.  This class is subclassed to provide a lot of other classes
in the QCMG::PDF hierachy.

See section 3.2.6 Dictionary Objects in the PDF Reference, sixth edition,
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
