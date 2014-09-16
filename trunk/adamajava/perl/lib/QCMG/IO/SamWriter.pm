package QCMG::IO::SamWriter;

###########################################################################
#
#  Module:   QCMG::IO::SamWriter
#  Creator:  John V Pearson
#  Created:  2010-01-22
#
#  Creates v0.1.2 SAM file (1000 Genomes) to hold sequencing read
#  alignment records.  Loosely based on an NHGRI module.
#
#  $Id: SamWriter.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4663 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: SamWriter.pm 4663 2014-07-24 06:39:00Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    die "SamWriter:new() requires a filename parameter" 
        unless (exists $params{filename} and defined $params{filename});

    my $self = { filename        => $params{filename},
                 sam_version     => '0.1.2',
                 sort_order      => 'unsorted',
                 group_order     => 'none',
                 p_name          => ($params{p_name} ?
                                     $params{p_name} : ''),
                 p_cmdline       => ($params{p_cmdline} ?
                                     $params{p_cmdline} : ''),
                 p_version       => ($params{p_version} ?
                                     $params{p_version} : 0),
                 description     => ($params{description} ?
                                     $params{description} : 0),
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;

    # Open file and make sure it is writable
    my $fh = IO::File->new( $params{filename}, 'w' );
    die 'Unable to open ', $params{filename}, " for writing: $!"
        unless defined $fh;

    $self->filehandle( $fh );
    return $self;
}


sub filename {
    my $self = shift;
    return $self->{filename} = shift if @_;
    return $self->{filename};
}

sub filehandle {
    my $self = shift;
    return $self->{filehandle} = shift if @_;
    return $self->{filehandle};
}

sub sam_version {
    my $self = shift;
    return $self->{sam_version};
}

sub p_name {
    my $self = shift;
    return $self->{p_name};
}

sub p_cmdline {
    my $self = shift;
    return $self->{p_cmdline};
}

sub p_version {
    my $self = shift;
    return $self->{p_version};
}

sub group_order {
    my $self = shift;
    return $self->{group_order} = shift if @_;
    return $self->{group_order};
}


sub sort_order {
    my $self = shift;

    if (@_) {
        my $order = lc( shift );
        if ($order =~ /unsorted/i or
            $order =~ /queryname/i or
            $order =~ /coordinate/i ) {
            $self->{sort_order} = lc( $order );
        }
        else {
            die "Invalid value [$order] supplied to sort_order(). ",
                "Valid values are: unsorted, queryname, coordinate\n";
        }
    }

    return $self->{sort_order};
}


sub header {
    my $self = shift;

    my $header = '@HD' ."\t" . 'VN:' . $self->sam_version . "\t" .
                               'SO:' . $self->sort_order . "\t" .
                               'GO:' . $self->group_order . "\n";
    $header .=  '@PG' . "\t" . $self->program . "\n";

    return $header;
}


sub program {
    my $self = shift;

    my $program = 'ID:' . $self->p_name . "\t" .
                  'VN:' . $self->p_version  . "\t" .
                  'CL:' . $self->p_cmdline;
    return $program;
}


sub write {
    my $self = shift;
    my $line = shift;

    $self->filehandle->print( $line );
}

1;

__END__


=head1 NAME

QCMG::IO::SamWriter - SAM file IO


=head1 SYNOPSIS

 use QCMG::IO::SamWriter;


=head1 DESCRIPTION

This module provides an interface for reading and writing SAM files.


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: SamWriter.pm 4663 2014-07-24 06:39:00Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2010-2014

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
