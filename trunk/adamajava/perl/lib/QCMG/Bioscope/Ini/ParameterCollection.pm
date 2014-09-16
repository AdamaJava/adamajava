package QCMG::Bioscope::Ini::ParameterCollection;

###########################################################################
#
#  Module:   QCMG::Bioscope::Ini::ParameterCollection.pm
#  Creator:  John V Pearson
#  Created:  2010-09-26
#
#  A collection of Bioscope INI file parameter objects.
#
#  $Id: ParameterCollection.pm 4660 2014-07-23 12:18:43Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use IO::File;
use IO::Zlib;
use Data::Dumper;
use Carp qw( croak );
use vars qw( $VERSION @ISA );

( $VERSION ) = '$Revision: 4660 $ ' =~ /\$Revision:\s+([^\s]+)/;


###########################################################################
#                          PUBLIC METHODS                                 #
###########################################################################


sub new {
    my $class  = shift;
    my %params = @_;

    croak 'name parameter is compulsory' unless
       (exists $params{name} and defined $params{name} and $params{name}); 
    croak 'value parameter is compulsory' unless
       (exists $params{name} and defined $params{name} and $params{name}); 

    # For convenience, we will store refs to all input objects in both
    # an array $self->{ra_params} and a hash $self->{rh_params}.

    my $self = { name       => $params{name},
                 comment    => $params{comment},
                 ra_params  => [],
                 rh_params  => {},
                 lines_pre  => ($params{lines_pre} ? $params{lines_pre} : 0),
                 lines_post => ($params{lines_post} ? $params{lines_post} : 0),
                 version    => $VERSION };

    bless $self, $class;
}


sub name {
    my $self = shift;
    return $self->{name} = shift if @_;
    return $self->{name};
}

sub params {
    my $self = shift;
    return $self->{ra_params};
}

sub _params_array {
    my $self = shift;
    return $self->{ra_params};
}

sub _params_hash {
    my $self = shift;
    return $self->{rh_params};
}

sub comment {
    my $self = shift;
    return $self->{comment} = shift if @_;
    return $self->{comment};
}

sub lines_pre {
    my $self = shift;
    return $self->{lines_pre} = shift if @_;
    return $self->{lines_pre};
}

sub lines_post {
    my $self = shift;
    return $self->{lines_post} = shift if @_;
    return $self->{lines_post};
}

sub version {
    my $self = shift;
    return $self->{version};
}


sub name_length {
    my $self = shift;
    # This is tricky because a PC can hold params or more PC's so we'll
    # need to use the name_length() method for each sub-object and this
    # will nicely recurse down however deep the nesting gets.
    my @name_lengths = sort { $a <=> $b }
                       map { $_->name_length(); }
                       values %{ $self->{rh_params} };
    #print Dumper @name_lengths;
    # Return last (biggest) length
    return pop(@name_lengths);
}


sub as_text {
    my $self   = shift;
    my $padlen  = @_ ? shift : 0;   # Name length for value to be padded to
    my $linelen = @_ ? shift : 75;  # Overall line length
    my $prefix  = @_ ? shift : '';  # Leading text on each line

    # All contents get processed in order

    my $text .= "\n" x $self->lines_pre;
    foreach my $obj (@{ $self->params }) {
        #print $obj->name ,' is ', $obj->name_length, " long\n";
        $text .= $obj->as_text( $padlen,$linelen,$prefix );
    }
    $text .= "\n" x $self->lines_post;

    return $text;
}


# This currently unused routine applies a very specific sorting strategy
# prior to outputting the text.  While the sorting may be handy it tends
# to make lines_pre and lines_post effectively useless and it makes
# TextBlock objects a lot less useful since they can't easily be placed
# in a fixed position thanks to the sorting.

sub _as_text_sorted {
    my $self   = shift;
    my $padlen  = @_ ? shift : 0;   # Name length for value to be padded to
    my $linelen = @_ ? shift : 75;  # Overall line length
    my $prefix  = @_ ? shift : '';  # Leading text on each line

    # ParameterCollections get processed first so we need to split
    # params in Parameter and ParameterCollection objects
    my @ps  = ();
    my @pcs = ();

    foreach my $obj (values %{ $self->params }) {
        if (ref($obj) eq 'QCMG::Bioscope::Ini::ParameterCollection') {
            push @pcs, $obj;
        }
        elsif (ref($obj) eq 'QCMG::Bioscope::Ini::Parameter') {
            push @ps, $obj;
        }
        else {
            croak 'Object of unhandleable type [', ref($obj), ']';
        }
    }

    # Sort the obects alphabetically
    my @pc_names = sort map { $_->name } @pcs;
    my @p_names  = sort map { $_->name } @ps;

    my $text = '';
    foreach my $name (@pc_names) {
        $text .= $self->params->{$name}->as_text( $padlen,$linelen,$prefix );
    }
    $text .= "\n" x $self->lines_pre;
    $text .= $self->comment_as_text( $linelen, $prefix );
    foreach my $name (@p_names) {
        $text .= $self->params->{$name}->as_text( $padlen,$linelen,$prefix );
    }
    $text .= "\n" x $self->lines_post;

    return $text;
}


sub comment_as_text {
    my $self    = shift;
    my $linelen = @_ ? shift : 75;  # Overall line length
    my $prefix  = @_ ? shift : '';  # Leading text on each line

    my $text   = '';

    # Add the comment if present
    if ($self->comment) {
        my @comment_words = split /\s+/, $self->comment;
        my $line = "${prefix}# " . shift @comment_words;
        while (@comment_words) {
            my $word = shift @comment_words;
            if ((length($line) + length($word)) > $linelen) {
                $text .= "$line\n";
                $line = "${prefix}# $word";
            }
            elsif ((length($line) + length($word)) <= $linelen) {
                # Trinary op helps with special case after long word
                $line .= ($line ? ' ' . $word : $word);
            }
            else {
                croak 'Unhandled text-wrangling problem -' .
                      "  word[$word]". length($word) .
                      "  line[$line]". length($line) .
                      "  text[$text]". length($text) . "\n";
            }
        }

        # Catch final line of comments if any
        $text .= "$line\n" if $line;
    }

    return $text;
}


sub add_parameter {
    my $self   = shift;
    my %params = @_;

    my $p = QCMG::Bioscope::Ini::Parameter->new( %params );
    $self->add_object( $p );
}


sub add_textblock {
    my $self   = shift;
    my %params = @_;

    my $t = QCMG::Bioscope::Ini::TextBlock->new( %params );
    $self->add_object( $t );
}


sub add_object {
    my $self = shift;
    my $obj  = shift;

    # Check that this object is something we understand
    if ( (ref($obj) eq 'QCMG::Bioscope::Ini::ParameterCollection')  or
         (ref($obj) eq 'QCMG::Bioscope::Ini::Parameter') or 
         (ref($obj) eq 'QCMG::Bioscope::Ini::TextBlock') ) {
        $self->_params_hash->{ $obj->name } = $obj;
        push @{ $self->_params_array }, $obj;
    }
    else {
        croak 'Object of unhandleable type [', ref($obj), ']';
    }
}


sub get_parameter {
    my $self = shift;
    my $name = shift;

    if  (exists $self->_params_hash->{$name} and defined $self->_params_hash->{$name}) {
        return $self->_params_hash->{ $name };
    }
    else {
        return undef;
    }
}



1;
__END__


=head1 NAME

QCMG::Bioscope::Ini::ParameterCollection - A Collection of Bioscope INI file parameters


=head1 SYNOPSIS

 use QCMG::Bioscope::Ini::ParameterCollection;
 my $pc = QCMG::Bioscope::Ini::ParameterCollection->new(
                name       => 'qcmg.F3.params',
                lines_post => 1
                comment    => 'QCMG F3 parameters' );
 print $pc->as_text;


=head1 DESCRIPTION

This module represents a collection of parameters from a Bioscope INI file.
It can also hold ParameterCollections.


=head1 PUBLIC METHODS

=over

=item B<new()>
 
 my $pc = QCMG::Bioscope::Ini::ParameterCollection->new(
                name       => 'qcmg.F3.params',
                lines_post => 1
                comment    => 'QCMG F3 parameters' );

Takes name, comment, lines_pre and lines_post input parameters of which
only name is required.  The lines_pre and lines_post are used to
determine how many (if any) blank lines should be output before and
after the ParameterCollection when it is written to text.  The comment
will be be output as a series of one or more lines prefixed with a '#'
character.

=item B<add_parameter()>

 $pc->add_parameter( name  => 'primer.set',
                     value => 'R3' );

This method takes a hash of values that are used to create a new
Parameter object and add it to the collection.  If you have a
pre-defined Parameter that you wish to add to the collection, see
B<add_parameter_object()>.

=item B<add_parameter_object()>
 
 my $p = QCMG::Bioscope::Ini::Parameter->new( name  => 'qcmg.run_id',
                                              value => '413' );
 $pc->add_parameter_object( $p );

This method is used to add pre-existing Parameter or ParameterCollection
object to the collection.

=item B<get_parameter()>

 my $p = $pc->get_parameter('qcmg.run_id');

This method retruns a Parameter or ParameterCollection object if one
exists with the specified name.  If a matching object does not exist
then undef is returned.  If it's important to know what sort of object
has been returned, you'll have to use Perl's ref() statement.

=back


=head1 SEE ALSO

=over

=item QCMG::Bioscope::Ini::IniFile
=item QCMG::Bioscope::Ini::Parameter

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: ParameterCollection.pm 4660 2014-07-23 12:18:43Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2009-2014

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
