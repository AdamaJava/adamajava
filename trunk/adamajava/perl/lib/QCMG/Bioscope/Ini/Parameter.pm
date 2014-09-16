package QCMG::Bioscope::Ini::Parameter;

###########################################################################
#
#  Module:   QCMG::Bioscope::Ini::Parameter.pm
#  Creator:  John V Pearson
#  Created:  2010-09-26
#
#  A single Bioscope INI file parameter.
#
#  $Id: Parameter.pm 4660 2014-07-23 12:18:43Z j.pearson $
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

    my $self = { name       => $params{name},
                 value      => $params{value},
                 comment    => $params{comment},
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

sub value {
    my $self = shift;
    return $self->{value} = shift if @_;
    return $self->{value};
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
    return length( $self->name );
}   


sub as_text {
    my $self   = shift;
    my $padlen  = @_ ? shift : 0;   # Name length for value to be padded to
    my $linelen = @_ ? shift : 75;  # Overall line length
    my $prefix  = @_ ? shift : '';  # Leading text on each line

    my $text = '';
    $text .= "\n" x $self->lines_pre;
    $text .= $self->comment_as_text( $linelen, $prefix );
    $text .= $self->name_and_value_as_text( $padlen, $prefix );
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


sub name_and_value_as_text {
    my $self    = shift;
    my $padlen  = @_ ? shift : 0;   # Name length for value to be padded to
    my $prefix  = @_ ? shift : '';  # Leading text on each line

    my $namelen = length($self->name);
    my $padding = ' 'x($namelen < $padlen ? $padlen-$namelen : 0);

    # Add the parameter name and value
    return $prefix . $self->name . $padding . ' = ' . $self->value . "\n";
}


1;
__END__


=head1 NAME

QCMG::Bioscope::Ini::Parameter - A Bioscope INI file parameter


=head1 SYNOPSIS

 use QCMG::Bioscope::Ini::Parameter;
 my $p = QCMG::Bioscope::Ini::Parameter->new(
             name       => 'queue.name',
             value      => 'bioscope',
             comment    => 'PBS queue name',
             lines_pre  => 0,
             lines_post => 0 );
 print $p->as_text;


=head1 DESCRIPTION

This module represents a single parameter from a Bioscope INI file.
In general this module will not be used directly but rather a
ParemeterCollection object would be used to hold all of the parameters
for a given INI file.
A parameter requires at least a name and value to be supplied by the user.
These values would usually be specified in the call to B<new()> although
there are get/set accessors.


=head1 PUBLIC METHODS

=over

=item B<new()>

Takes name, value and comment input parameters of which only comment is
optional.

=item B<name()>

 $p->name( 'qcmg.date' );
 print $p->name;

Get/set the parameter name.

=item B<value()>

 $p->value( '20100819' );
 print $p->value;

Get/set the parameter value.

=item B<comment()>

 $p->comment( 'The date on which the mapping commenced.' );
 print $p->comment;

Get/set a comment to be printed out on the line before the parameter name 
and value when B<as_text()> is called.

=item B<lines_pre()>

 $p->lines_pre( 2 );
 print $p->lines_pre;

Get/set the number of blank lines to be output before this parameter in
B<as_text()>.

=item B<lines_post()>

 $p->lines_post( 2 );
 print $p->lines_post;

Get/set the number of blank lines to be output after this parameter in
B<as_text()>.

=item B<version()>

 print $p->version;

Get the version of the QCMG::Bioscope::Ini::Parameter module.

=item B<name_length()>

 print $p->name_length;

Get the length of the name string for this Parameter.  This is useful to
know when trying to layout a block of Parameters so that their names and
values line up.

=item B<as_text()>

 print $p->as_text;
 print $p->as_text( 30 );            # Pad name to 30 chars
 print $p->as_text( 30, 75 );        # Max line length = 75
 print $p->as_text( 30, 75, '  ' );  # Indent 2 spaces

This method returns the contents of Parameter as a string.  The string
included any blank lines specified by B<lines_pre()> and
B<lines_post()> as well as the B<comment()> (if any) along with the
B<name()> and B<value()>.  It takes 3 optional parameters - the first is
the length to pad the name to so that values can be lined up, the second
is the maximum line length which is used for the comment line but not the
name=value line, and the third is a string which is prefixed to any
comment and name=value lines (this is used for indenting).

=item B<comment_as_text()>

 print $p->comment_as_text;
 print $p->comment_as_text( 75 );        # Max line length = 75
 print $p->comment_as_text( 75, '  ' );  # Indent 2 spaces

This method is used by B<as_text()> but only takes 2 of the 3
B<as_text()> parameters.

=item B<name_and_value_as_text()>

 print $p->name_and_value_as_text;
 print $p->name_and_value_as_text( 30 );        # Pad name to 30 chars
 print $p->name_and_value_as_text( 75, '  ' );  # Indent 2 spaces

This method is used by B<as_text()> but only takes 2 of the 3
B<as_text()> parameters.

=back


=head1 SEE ALSO

=over

=item QCMG::Bioscope::Ini::IniFile

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: Parameter.pm 4660 2014-07-23 12:18:43Z j.pearson $


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
