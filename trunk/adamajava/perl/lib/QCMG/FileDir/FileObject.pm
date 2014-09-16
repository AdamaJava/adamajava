package QCMG::FileDir::FileObject;

##############################################################################
#
#  Module:   QCMG::FileDir::FileObject.pm
#  Creator:  John V Pearson
#  Created:  2010-09-16
#
#  This class implements a file object.
#
#  $Id: FileObject.pm 4662 2014-07-23 12:39:59Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak );
use Data::Dumper;

use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION $BYTES_PER_BLOCK );

( $REVISION ) = '$Revision: 4662 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: FileObject.pm 4662 2014-07-23 12:39:59Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


BEGIN {
    # This is the assumed number of bytes per stat[12] block.  We
    # *REALLY* don't want to use this to do file size calculations.
    $BYTES_PER_BLOCK = 512;
}

sub new {
    my $class  = shift;
    my %params = @_;
    
    croak "You must supply a name parameter to a new FileObject"
       unless (exists $params{name} and $params{name});
    croak "You must supply a parent parameter to a new FileObject"
       unless (exists $params{parent});

    my $self = { name    => $params{name},
                 parent  => $params{parent},  # DirectoryObject object
                 stat    => [],
                 verbose => ($params{verbose} ? $params{verbose} : 0) };
    bless $self, $class;

    qlogprint 'new object for file ',$self->full_pathname,"\n"
        if ($self->verbose > 1);

    $self->{stat} = [ stat($self->full_pathname) ];

    return $self;
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub name {
    my $self = shift;
    return $self->{name};
}


# To do this, we'll need to know the name of the parent directory as
# well.

sub full_pathname {
    my $self = shift;
    if (defined $self->parent) {
        return $self->parent->full_pathname . '/'. $self->name;
    }
    else {
        $self->name;
    }
}


sub parent {
    my $self = shift;
    return $self->{parent};
}


sub filestat {
    my $self = shift;
    my $idx  = shift;

    return $self->{stat}[$idx];
}


sub to_text {
    my $self   = shift;
    my $indent = shift;

    return $self->size_as_string .
           ( $indent x ($self->parent->level+1) ) .
           $self->name . "\n";
}


sub size {
    my $self = shift;
    # Size of the actual file - can be weirdly wrong sometimes
    return $self->{stat}->[7];
    # Alternative but very very dangerous way of ?guessing? file size!
    #return $BYTES_PER_BLOCK * $self->{stat}->[12];
}


sub size_as_string {
    my $self = shift;
    my $size = $self->size;
    my $stmp = ( $size > 1024**3 ) ? sprintf( "%6.1f", $size / 1024**3 ).'G' :
               ( $size > 1024**2 ) ? sprintf( "%6.1f", $size / 1024**2 ).'M' :
               ( $size > 1024**1 ) ? sprintf( "%6.1f", $size / 1024**1 ).'K' :
                                     sprintf( "%6.0f",   $size ).'B' ;
    return $stmp;
}


1;

__END__

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
