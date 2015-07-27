package QCMG::IO::RmskRecord;

###########################################################################
#
#  Module:   QCMG::IO::RmskRecord
#  Creator:  Matthew J Anderson
#  Created:  2011-01-16
#
#  Data container for a Repeat Masker Regions.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use Memoize;
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;
    

sub new {
    my $class = shift;
    my $line  = shift;

    chomp $line;
    my @fields = split "\t", $line;
    warn 'Saw ', scalar(@fields), " fields, should have 16 columns [$line]\n"
        if ( scalar(@fields) != 16 );

    my $self = { bin        => $fields[0],  # Indexing field to speed chromosome range queries.
                 swScore    => $fields[1],  # Smith Waterman alignment score
                 milliDiv   => $fields[2],  # Base mismatches in parts per thousand
                 milliDel   => $fields[3],  # Bases deleted in parts per thousand
                 milliIns   => $fields[4],  # Bases inserted in parts per thousand
                 genoName   => $fields[5],  # Genomic sequence name
                 genoStart  => $fields[6],  # Start in genomic sequence
                 genoEnd    => $fields[7],  # End in genomic sequence
                 genoLeft   => $fields[8],  # -#bases after match in genomic sequence
                 strand     => $fields[9],  # Relative orientation + or -
                 repName    => $fields[10], # Name of repeat
                 repClass   => $fields[11], # Class of repeat
                 repFamily  => $fields[12], # Family of repeat
                 repStart   => $fields[13], # Start (if strand is +) or -#bases after match (if strand is -) in repeat sequence
                 repEnd     => $fields[14], # End in repeat sequence
                 repLeft    => $fields[15], # -#bases after match (if strand is +) or start (if strand is -) in repeat sequence
                 id         => $fields[16]  # First digit of id field in RepeatMasker .out file. Best ignored.
        };

    bless $self, $class;
}

sub bin {
    my $self = shift;
    return $self->{bin};
}

sub swScore {
    my $self = shift;
    return $self->{swScore};
}

sub milliDiv {
    my $self = shift;
    return $self->{milliDiv};
}

sub milliDel {
    my $self = shift;
    return $self->{milliDel};
}

sub milliIns {
    my $self = shift;
    return $self->{milliIns};
}

sub genoName {
    my $self = shift;
    return $self->{genoName};
}

sub genoStart {
    my $self = shift;
    return $self->{genoStart};
}

sub genoEnd {
    my $self = shift;
    return $self->{genoEnd};
}

sub genoLeft {
    my $self = shift;
    return $self->{genoLeft};
}

sub strand {
    my $self = shift;
    return $self->{strand};
}

sub repName {
    my $self = shift;
    return $self->{repName};
}

sub repClass {
    my $self = shift;
    return $self->{repClass};
}

sub repFamily {
    my $self = shift;
    return $self->{repFamily};
}

sub repStart {
    my $self = shift;
    return $self->{repStart};
}

sub repEnd {
    my $self = shift;
    return $self->{repEnd};
}

sub repLeft {
    my $self = shift;
    return $self->{repLeft};
}

sub id {
    my $self = shift;
    return $self->{id};
}


1;

__END__

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013-2014

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
