package QCMG::IO::BedReader;

###########################################################################
#
#  Module:   QCMG::IO::BedReader
#  Creator:  John V Pearson
#  Created:  2010-10-04
#
#  Reads BED files.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;
use IO::File;
use Data::Dumper;
use Carp qw( carp croak );

use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    croak "BedReader:new() requires a filename" 
        unless (exists $params{filename} and defined $params{filename});

    my $self = { filename        => $params{filename},
                 name            => ($params{name} ? $params{name} : ''),
                 headers         => [],
                 record_ctr      => 0,
                 _record         => '',
                 _filehandle     => '',
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;

    $self->_initialise;
    
    return $self;
}


sub filename {
    my $self = shift;
    return $self->{filename} = shift if @_;
    return $self->{filename};
}

sub name {
    my $self = shift;
    return $self->{name} = shift if @_;
    return $self->{name};
}

sub filehandle {
    my $self = shift;
    return $self->{_filehandle} = shift if @_;
    return $self->{_filehandle};
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub record_ctr {
    my $self = shift;
    return $self->{record_ctr};
}

sub _headers {
    my $self = shift;
    return $self->{headers};
}

sub _incr_record_ctr {
    my $self = shift;
    return $self->{record_ctr}++;
}

sub _initialise {
    my $self = shift;

    my $infh =IO::File->new( $self->filename, 'r' )
       or croak 'Can\'t open BED file [', $self->filename, "] for reading: $!";
    $self->filehandle( $infh );

    # Read off headers and first record
    while (my $line = $self->filehandle->getline) {
        # Read off the header (if any)
        if ($line =~ /^#/) {
            push @{ $self->{headers} }, $line;
            next;
        }
        # To get here we must have hit a record
        chomp $line;
        my @fields = split /\t/, $line;
        $self->{_record} = \@fields;
        $self->_incr_record_ctr;
        last;
    }
}

sub _next_record {
    my $self = shift;

    # If we ran out of records on the last call so the saved record is
    # undef then return undef immediately.
    return undef unless defined $self->{_record};

    # Read the next record into the record buffer and return the 
    # previous buffer contents.
    my $last_record = $self->{_record};
    my $new_record  = $self->filehandle->getline;

    if (! defined $new_record) {
        $self->{_record} = undef;
    }
    else {
        chomp $new_record;
        my @fields = split /\t/, $new_record;
        $self->{_record} = \@fields;
        $self->_incr_record_ctr;
        if ($self->verbose and $self->{record_ctr} % 1000000 == 0) {
            print( '[' . localtime() . '] ' . $self->{record_ctr},
                   " records processed\n" );
        }
    }

    return $last_record;
}

sub next_record_as_text {
    my $self = shift;
    my $ra_fields = $self->_next_record;

    # Exit immediately if we're at EOF;
    return undef unless (defined $ra_fields);
    
    return join("\t",@{$ra_fields});
}

# returns arrayref
sub next_record {
    my $self = shift;
    return $self->_next_record;
}

# returns arrayref
sub current_record {
    my $self = shift;
    return $self->{_record};
}

sub current_record_seq {
    my $self = shift;
    return undef unless defined $self->{_record};
    return $self->{_record}->[0];
}

sub current_record_start {
    my $self = shift;
    return undef unless defined $self->{_record};
    return $self->{_record}->[1];
}

sub current_record_end {
    my $self = shift;
    return undef unless defined $self->{_record};
    return $self->{_record}->[2];
}

# These 2 don't seem to match the expected BED fields as shown on the
# UCSC website at http://genome.ucsc.edu/FAQ/FAQformat.html#format1
#
#sub current_record_strand {
#    my $self = shift;
#    return undef unless defined $self->{_record};
#    return $self->{_record}->[3];
#}
#
#sub current_record_count {
#    my $self = shift;
#    return undef unless defined $self->{_record};
#    return $self->{_record}->[4];
#}



1;

__END__


=head1 NAME

QCMG::IO::BedReader - BED file IO


=head1 SYNOPSIS

 use QCMG::IO::BedReader;


=head1 DESCRIPTION

This module provides an interface for reading BED files.

=over 4

=item B<new()>

=item B<filename()>

=item B<name()>

=item B<filehandle()>

=item B<verbose()>

=item B<record_ctr()>

=item B<next_record_as_text()>

=item B<next_record()>

=item B<current_record()>

=item B<current_record_seq()>

=item B<current_record_start()>

=item B<current_record_end()>

=back

=head1 AUTHORS

John Pearson L<mailto:grendeloz@gmail.com>


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2010-2014
Copyright (c) QIMR Berghofer Medical Research Institute 2015

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
