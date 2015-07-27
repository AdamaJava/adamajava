package QCMG::IO::QiCoordsReader;

###########################################################################
#
#  Module:   QCMG::IO::QiCoordsReader
#  Creator:  John V Pearson
#  Created:  2012-09-20
#
#  Reads lists of coordinates for pre-processing to qInspect files.
#  beacuse the underlying records are so simple, this module does not
#  use a separate "record" module but rather directly parses the file in
#  its entirety.
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
use QCMG::IO::MafRecord;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    croak "QCMG::IO::QiCoordsReader:new() requires a filename parameter" 
        unless (exists $params{filename} and defined $params{filename});

    my $self = { filename        => $params{filename},
                 filehandle      => undef,
                 headers         => {},
                 coords          => [],
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

    $self->_parse_file;

    return $self;
}


sub _parse_file {
    my $self = shift;

    # Process file
    my $lctr = 0;
    while (my $line = $self->filehandle->getline()) {
        chomp $line;
        next unless $line;
        $lctr++;
        if ($line =~ /^#QIPP (\w+)=(.*)$/i) {
            # A header line
            my $key = $1;
            my $val = $2;
            if (uc($key) =~ /^BAM(\w*)$/) {
                # check for description
                my $desc = ($1) ? $1 : '';
                my $bam  = $val;
                push @{$self->{headers}->{BAMs}},
                     { desc => $desc, name => $bam };
            }
            else {
                $self->{headers}->{ uc($1) } = $2;
            }
        }
        elsif ($line =~ /^#/) {
            # skip any other comments
        }
        elsif ($line =~ /^(\w+):(\d+)\-(\d+)\s/i) {
            # A coords line
            push @{$self->{coords}},
                 { chrom => $1, start => $2, end => $3 };
            $self->_incr_record_ctr;
        }
        else {
            croak "Unable to parse line $lctr: $line\n";
        }
    }

    # Check that we have our compulsory headers
    foreach my $header (qw/ID BAMs/) {
        croak "No QIPP $header= line supplied in file ".$self->{filename}."\n"
            unless (exists $self->{headers}->{$header} and
                    defined $self->{headers}->{$header});
    }

    # Check that we can read from our BAM
    foreach my $bam (@{$self->{headers}->{BAMs}}) {
        croak "Unable to read from BAM ".$bam->{name}."\n"
            unless (-r $bam->{name});
    }
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

sub record_ctr {
    my $self = shift;
    return $self->{record_ctr};
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub coords {
    my $self = shift;
    return @{$self->{coords}};
}

sub header {
    my $self = shift;
    my $key  = shift;
    return undef unless exists $self->{headers}->{$key};
    return $self->{headers}->{$key};
}

sub _incr_record_ctr {
    my $self = shift;
    return $self->{record_ctr}++;
}


1;

__END__


=head1 NAME

QCMG::IO::QiCoordsReader - qInspect coordinate list file


=head1 SYNOPSIS

 use QCMG::IO::QiCoordsReader;


=head1 DESCRIPTION

This module reads files of coordinates that are to be translated in
qInspect ".qi" files for conversion to PDF by qInspect


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
