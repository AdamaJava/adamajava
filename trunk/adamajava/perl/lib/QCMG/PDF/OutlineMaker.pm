package QCMG::PDF::OutlineMaker;

###########################################################################
#
#  Module:   QCMG::PDF::OutlineMaker
#  Creator:  John V Pearson
#  Created:  2012-06-12
#
#  Construct a tree from the annotations pulled from a QiRecord array.
#
#  $Id: OutlineMaker.pm 4664 2014-07-24 08:17:04Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use Carp qw( croak );
use vars qw( $SVNID $REVISION %ENTRIES );

( $REVISION ) = '$Revision: 4664 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: OutlineMaker.pm 4664 2014-07-24 08:17:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

sub new {
    my $class = shift;
    my %params = @_;

    my $self = { tree   => {},
                 doc    => $params{doc},
                 annots => undef };
    bless $self, $class;
}


sub add_annot {
    my $self   = shift;
    my %params = @_;

    croak "No Dest key in supplied hash"
        unless (exists $params{Dest} and defined $params{Dest});

    # The Dest val must look like an indirect object reference
    die 'Dest param [',$params{Dest},'] does not match the pattern '.
        'for a PDF indirect object reference.'
        unless ($params{Dest} =~ /^\s*\d+\s+\d+\s*R\s*$/);

    # Check that the keys are identical to that of the first annot
    if (defined $self->{annots}) {
        my @newkeys = sort keys %params;
        my @oldkeys = sort keys %{ $self->{annots}->[0] };
        die 'new annotation has '. scalar(@newkeys) .' keys but '.
            'existing annotations have '. scalar(@oldkeys)
            unless (scalar(@oldkeys) == scalar(@newkeys));
        foreach my $ctr (0..$#oldkeys) {
            die 'key mismatch ['. $newkeys[$ctr] .' vs'.
                $oldkeys[$ctr]. ']'
                unless ($newkeys[$ctr] eq $oldkeys[$ctr]);
        }
        push @{ $self->{annots} }, \%params;
    }
    else {
        push @{ $self->{annots} }, \%params;
    }
}


sub process {
    my $self = shift;
    my @annots = @_;

    my %tree = ();
    my %outline = ();

    # Create flattened array structure
    my @arrays = ();
    foreach my $annot (@{ $self->{annots} }) {
        my @array = map { $annot->{$_} } (@annots, 'Dest');
        push @arrays, \@array;
        
    }

    $self->_crack( \%tree, \@arrays );

    my $outline = QCMG::PDF::IndirectObject::Outlines->new();
    $self->{doc}->add_outlines( $outline );

    $self->_build( \%tree, $outline, $self->{doc} );
}


sub _crack {
    my $self      = shift;
    my $rh_tree   = shift;
    my $ra_arrays = shift;

    # Distribute the arrays
    my $finished = 0;
    my %remains = ();
    foreach my $ra_array (@{ $ra_arrays }) {
        my @vals = @{ $ra_array };
        my $val = shift @vals;
        #print join(' ', $val, @vals), "\n"; 

        # If @vals has only one value left then it is the indirect
        # reference that is the point of this whol exercise.

        if (scalar(@vals) == 1) {
            $rh_tree->{ $val } = shift @vals;
            $finished = 1;
        }
        else {
            push @{ $remains{$val} }, \@vals;
        }
    }

    if (! $finished) {
        # Process the remains
        foreach my $val (sort keys %remains) {
           $rh_tree->{$val} = {};
           $self->_crack( $rh_tree->{$val}, $remains{$val} );
        }
    }
}

sub _build {
    my $self    = shift;
    my $rh_tree = shift; 
    my $topitem = shift; 
    my $doc     = shift;

    my @items = ();
    foreach my $key (sort keys %{ $rh_tree }) {
        my $item = QCMG::PDF::IndirectObject::OutlineItem->new();
        $item->set_entry( 'Title', $key );
        $doc->add_object( $item );
        push @items, $item;

        # If $rh_tree->{$key} is a ref then there is more tree below
        # this level but if it's not a ref then we have hit a leaf so
        # set the /Dest for this item and exit
        if (ref( $rh_tree->{$key} )) {
            $self->_build( $rh_tree->{$key}, $item, $doc );
        }
        else {
            $item->set_entry( 'Dest', $rh_tree->{$key} );
        }
    }

    # Process @items so $topitem can get first/last set and so items in
    # @item can get prev/next/parent set.
    my $count = scalar(@items);
    $topitem->set_entry( 'First', $items[0]->reference_string() );
    $topitem->set_entry( 'Last',  $items[$count-1]->reference_string() );
    $topitem->set_entry( 'Count', $count );
    foreach my $ctr (0..($count-1)) {
        # Every item gets a parent
        $items[$ctr]->set_entry( 'Parent', $topitem->reference_string() );
        # Every item except the first gets a Prev
        if ($ctr > 0) {
            $items[$ctr]->set_entry( 'Prev',   $items[$ctr-1]->reference_string() );
        }
        # Every item except the last gets a Next
        if ($ctr < ($count-1)) {
            $items[$ctr]->set_entry( 'Next',   $items[$ctr+1]->reference_string() );
        }
    }
}


1;

__END__


=head1 NAME

QCMG::PDF::OutlineMaker - A piece of JP trickery


=head1 SYNOPSIS

 use QCMG::PDF::OutlineMaker;


=head1 DESCRIPTION

This module provides nothing ... yet.


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: OutlineMaker.pm 4664 2014-07-24 08:17:04Z j.pearson $


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
