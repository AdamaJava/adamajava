package QCMG::IO::PindelReader;

###########################################################################
#
#  Module:   QCMG::IO::PindelReader
#  Creator:  John V Pearson
#  Created:  2011-10-18
#
#  Reads visual read display files as output by the pindel variant
#  caller.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak );
use Data::Dumper;
use IO::File;
use IO::Zlib;
use QCMG::IO::PindelRecord;
use QCMG::IO::PindelRecordSV;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    croak "QCMG::IO::PindelReader:new() requires filename or zipname parameter" 
        unless ( (exists $params{filename} and defined $params{filename}) or
                 (exists $params{zipname} and defined $params{zipname}) );

    my $self = { filename        => $params{filename},
                 filehandle      => undef,
                 headers         => [],
                 record_ctr      => 0,
                 maf_version     => '',
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;

    # If there a zipname, we use it in preference to filename.  We only
    # process one so if both are specified, the zipname wins.

    if ( $params{zipname} ) {
        my $fh = IO::Zlib->new( $params{zipname}, 'r' );
        croak 'Unable to open ', $params{zipname}, " for reading: $!"
            unless defined $fh;
        $self->filename( $params{zipname} );
        $self->filehandle( $fh );
    }
    elsif ( $params{filename} ) {
        my $fh = IO::File->new( $params{filename}, 'r' );
        croak 'Unable to open ', $params{filename}, " for reading: $!"
            unless defined $fh;
        $self->filename( $params{filename} );
        $self->filehandle( $fh );
    }

    # Read off first '###' header
    $self->filehandle->getline();

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

sub _incr_record_ctr {
    my $self = shift;
    return $self->{record_ctr}++;
}

sub record_ctr {
    my $self = shift;
    return $self->{record_ctr};
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub next_record {
    my $self = shift;

    # Assume that we will always arrive here at the start of the record
    # with the '###' line already removed because we read off the first
    # one in new() and this routine stops reading a record when it eats
    # the '###' line off the start of the next record or hits EOF

    my $line = $self->filehandle->getline();

    # Catch EOF
    return undef if (! defined $line);

    # Process record starting with header line
    $self->_incr_record_ctr;
    chomp $line;

    # Decide what sort of pindel record we are dealing with
    my $rec = undef;
    if ($line =~ /^\d+\tLI\t/) {
        $rec = QCMG::IO::PindelRecordSV->new( headers => $line );
    }
    else {
        $rec = QCMG::IO::PindelRecord->new( headers => $line );
    }

    # Read block of supporting reads
    my $reads_text = '';
    while (my $line = $self->filehandle->getline()) {
        if ($line =~ /^##/) {
            # do nothing;
            $rec->reads_text( $reads_text );
            return $rec;
        }
        else {
            $reads_text .= $line;
        }
    }

    # Catch last record which will not have a '###' to trigger return
    $rec->reads_text( $reads_text );
    return $rec;
}


1;

__END__


=head1 NAME

QCMG::IO::PindelReader - Pindel output file IO


=head1 SYNOPSIS

 use QCMG::IO::PindelReader;


=head1 DESCRIPTION

This module provides an interface for reading pindel variant report
files.


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011-2014

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
