package QCMG::IO::qSnpGermlineReader;

###########################################################################
#
#  Module:   QCMG::IO::qSnpGermlineReader
#  Creator:  John V Pearson
#  Created:  2012-06-20
#
#  Reads Germline DCC files as created by qSNP.
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
use QCMG::IO::qSnpGermlineRecord;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    croak "QCMG::IO::qSnpGermlineReader:new() requires a filename parameter" 
        unless ( (exists $params{filename} and defined $params{filename}) );

    my $self = { filename        => $params{filename},
                 filehandle      => undef,
                 headers         => [],
                 record_ctr      => 0,
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;

    my $fh = IO::File->new( $params{filename}, 'r' );
    croak 'Unable to open ', $params{filename}, " for reading: $!"
        unless defined $fh;
    $self->filename( $params{filename} );
    $self->filehandle( $fh );

    $self->_parse_headers;

    return $self;
}


sub _parse_headers {
    my $self = shift;

    # Headers are always a single line at the top of file
    my $line = $self->filehandle->getline();
    chomp $line;
    my @headers = split /\t/, $line;

    my @valid_headers = @{ $QCMG::IO::qSnpGermlineRecord::VALID_HEADERS };
    my $min_fields = scalar(@valid_headers);

    # There could be more headers than the required ones but we only
    # need to validate the columns that appear in the official spec.
    foreach my $ctr (0..$#valid_headers) {
        if ($headers[$ctr] ne $valid_headers[$ctr]) {
           die "Invalid header in column [$ctr] - ".
               'should have been ['. $valid_headers[$ctr] .
               '] but is ['. $headers[$ctr] . ']';
        }
    }

    $self->{headers} = \@headers;
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

sub maf_version {
    my $self = shift;
    return $self->{maf_version};
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

sub _headers {
    my $self = shift;
    return $self->{headers};
}

sub next_record {
    my $self = shift;

    # Read lines, checking for and processing any headers
    # and only return once we have a record

    my $line = $self->filehandle->getline();
    # Catch EOF
    return undef if (! defined $line);
    chomp $line;
    $self->_incr_record_ctr;
    if ($self->verbose) {
        # Print progress messages for every 1M records
        qlogprint( $self->record_ctr, ' qSnpGermline records processed: ',
                   localtime().'', "\n" )
            if $self->record_ctr % 1000000 == 0;
    }
    my @fields = split /\t/, $line;
    my $rec = QCMG::IO::qSnpGermlineRecord->new( data    => \@fields,
                                                 headers => $self->{headers} );
    return $rec;
}


1;

__END__


=head1 NAME

QCMG::IO::qSnpGermlineReader - qSNP Germline SNP call file IO


=head1 SYNOPSIS

 use QCMG::IO::qSnpGermlineReader;


=head1 DESCRIPTION

This module provides an interface for reading germline SNP call files
created by qSNP.


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012-2014

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
