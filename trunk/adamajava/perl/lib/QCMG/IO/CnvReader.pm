package QCMG::IO::CnvReader;

###########################################################################
#
#  Module:   QCMG::IO::CnvReader
#  Creator:  John V Pearson
#  Created:  2011-10-18
#
#  Reads QCMG CNV summary files as used by Research Team for the ABO
#  collaboration (QCMG/Baylor/OICR) for Pancreatic Ca variants.
#
#  $Id: CnvReader.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak );
use Data::Dumper;
use IO::File;
use IO::Zlib;
use QCMG::IO::CnvRecord;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4663 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: CnvReader.pm 4663 2014-07-24 06:39:00Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    croak "QCMG::IO::CnvReader:new() requires filename or zipname parameter" 
        unless ( (exists $params{filename} and defined $params{filename}) or
                 (exists $params{zipname} and defined $params{zipname}) );

    my $self = { filename        => $params{filename},
                 filehandle      => undef,
                 headers         => [],
                 record_ctr      => 0,
                 cnv_version     => '',
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

    $self->_parse_headers;

    return $self;
}


sub _parse_headers {
    my $self = shift;

    # Read off version and headers
    my $version = '';
    my @headers = ();
    while (1) {
        my $line = $self->filehandle->getline();
        chomp $line;
        if ($line =~ /^#(version [\d\.]+)$/) {
            $version = $1;
            $version =~ s/[\s\.]/_/g;
        }
        elsif ($line =~ /^#/) {
            # skip any other comments
        }
        else {
            # Must be the headers
            @headers = split /\t/, $line;
            last;
        }
    }

    croak "unsupported CNV file version [$version]"
        unless (exists $QCMG::IO::CnvRecord::VALID_HEADERS->{$version});
    $self->{cnv_version} = $version;
    
    my @valid_headers = @{ $QCMG::IO::CnvRecord::VALID_HEADERS->{$version} };
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

sub _incr_record_ctr {
    my $self = shift;
    return $self->{record_ctr}++;
}

sub record_ctr {
    my $self = shift;
    return $self->{record_ctr};
}

sub cnv_version {
    my $self = shift;
    return $self->{cnv_version};
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

    while (1) {
        my $line = $self->filehandle->getline();
        # Catch EOF
        return undef if (! defined $line);
        chomp $line;
        if ($line =~ /^##/) {
            # do nothing;
        }
        else {
            $self->_incr_record_ctr;
            if ($self->verbose) {
                # Print progress messages for every 100K records
                print( $self->record_ctr, ' CNV records processed: ',
                       localtime().'', "\n" )
                    if $self->record_ctr % 100000 == 0;
            }
            my @fields = split /\t/, $line;
            my $rec = QCMG::IO::CnvRecord->new( version => $self->cnv_version,
                                                data    => \@fields,
                                                headers => $self->{headers} );
            return $rec;
        }
    }
}


1;

__END__


=head1 NAME

QCMG::IO::CnvReader - CNV file IO


=head1 SYNOPSIS

 use QCMG::IO::CnvReader;


=head1 DESCRIPTION

This module provides an interface for reading QCMG CNV summary files as
used by Research Team for the ABO collaboration (QCMG/Baylor/OICR) on
Pancreatic Ca variants.

=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: CnvReader.pm 4663 2014-07-24 06:39:00Z j.pearson $


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
