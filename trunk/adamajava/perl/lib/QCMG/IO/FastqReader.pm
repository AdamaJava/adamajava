package QCMG::IO::FastqReader;

##############################################################################
#
#  Module:   QCMG::IO::FastqReader.pm
#  Creator:  John V Pearson
#  Created:  2011-08-25
#
#  Generic FASTQ file parser.  Expects that lines are in groups of 4.
#
#  $Id$
#
##############################################################################

use strict;
use warnings;

#use Moose;
use IO::File;
use IO::Zlib;
use Data::Dumper;
use Carp qw( carp croak confess );
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;

#has 'filename'    => ( is => 'ro', isa => 'Str', required => 1 );
#has 'filehandle'  => ( is => 'rw', isa => 'IO::File', init_arg => undef );
#has 'headers'     => ( is => 'rw', isa => 'ArrayRef[Str]' );
#has 'current_rec' => ( is => 'rw', isa => 'HashRef' );
#has 'rec_ctr'     => ( is => 'rw', isa => 'Int', default => 0 );
#has 'verbose'     => ( is => 'rw', isa => 'Int', default => 0 );

sub new {
    my $class = shift;
    my %params = @_;

    croak "QCMG::IO::FastqReader:new() requires filename parameter"
        unless (exists $params{filename} and defined $params{filename});

    my $self = { filename        => $params{filename},
                 filehandle      => undef,
                 headers         => [],
                 current_rec     => undef,
                 rec_ctr         => 0,
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;

#sub BUILD {
#    my $self = shift;

    # Cope with gzip files.
    my $infh = undef;
    if ( $self->filename =~ /\.gz$/ ) {
        $infh = IO::Zlib->new( $self->filename, 'r' );
        confess 'Unable to open ', $self->filename, " for reading: $!"
            unless defined $infh;
        $self->filehandle( $infh );
    }
    else {
        $infh = IO::File->new( $self->filename, 'r' );
        confess 'Unable to open ', $self->filename, " for reading: $!"
            unless defined $infh;
        $self->filehandle( $infh );
    }

    # Read off headers and first record
    while (1) {
        my $line = $self->filehandle->getline;
        chomp $line;
        if ($line =~ /^#/) {
            push @{ $self->headers }, $line;
            next;
        }

        # If we got here then we must be in the first data record so 
        # we should read the other 3 record lines and bail out

        my @lines = ($line, @{$self->_read_lines(3)} );
        my $rec = $self->_lines2rec( \@lines );
        $self->current_rec( $rec );
        last;
    }

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

sub rec_ctr {
    my $self = shift;
    # We have always already read the next record so we should report 1
    # less than the number we have read.
    return $self->{rec_ctr} -1;
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub headers {
    my $self = shift;
    return $self->{headers};
}

sub current_rec {
    my $self = shift;
    return $self->{current_rec} = shift if @_;
    return $self->{current_rec};
}


sub next_record {
    my $self = shift;

    # Return the previous record and store new (current) rec
    my $last_rec = $self->current_rec;

    # If there are no more lines in the file, we still need to return
    # the current (last) record.  We explicitly set current_rec to undef
    # when we run out of records so the last real record doesn't keep
    # getting handed out.
    my $ra_lines = $self->_read_lines(4);
    if (defined $ra_lines) {
        $self->current_rec( $self->_lines2rec( $ra_lines ) );
    }
    else {
        $self->current_rec( undef );
    }

    return $last_rec;
}


sub _lines2rec {
    my $self     = shift;
    my $ra_lines = shift;

    # Parse $ra_lines into a record hash
    die 'No @ at start of id line: '.$ra_lines->[0]."\n" unless ($ra_lines->[0] =~ /^\@/);
    die 'No + at start of 2nd id line: '.$ra_lines->[2]."\n" unless ($ra_lines->[2] =~ /^\+/);
    $ra_lines->[0] =~ s/^\@//;

    my $rec = { id   => $ra_lines->[0],
                seq  => $ra_lines->[1],
                qual => $ra_lines->[3] };

    $self->{rec_ctr}++;

    return $rec;
}


sub _read_lines {
    my $self  = shift;
    my $count = shift;

    my @lines = ();
    for my $ctr (1..$count) {
        my $line = $self->filehandle->getline;
        if (! defined $line) {
           warn "Not enough lines - read $ctr of $count requested\n";
           return undef;
        }
        chomp $line;
        push @lines, $line;
    }

    return \@lines;
}


#no Moose;

1;
__END__


=head1 NAME

QCMG::FileDir::FastqReader - Perl module for parsing FASTQ files


=head1 SYNOPSIS

 use QCMG::FileDir::FastqReader;


=head1 DESCRIPTION

This module reads FASTQ files record by record.


=head1 PUBLIC METHODS

=over

=item B<new()>

 $fq = QCMG::IO::FastqReader->new( verbose => 0 );

There is currently only one valid parameter that can be supplied to the
constructor - verbose.

=item B<next_record()>

 while (my $rec = $fq->next_record) {
    ...
 }

This method returns a hash representing the next FASTQ record.

=item B<verbose()>

 $fq->verbose();

Returns the verbose status for this object where 0 sets verbose off 
and any other value (traditionally 1) sets verbose mode on.

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011-2014
Copyright (c) QIMR Berghofer Medical Research Institute 2016

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
