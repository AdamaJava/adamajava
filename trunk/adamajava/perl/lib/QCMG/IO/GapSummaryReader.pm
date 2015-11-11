package QCMG::IO::GapSummaryReader;

###########################################################################
#
#  Module:   QCMG::IO::GapSummaryReader
#  Creator:  John V Pearson
#  Created:  2013-10-30
#
#  Reads QCMG CNV GAP summary files as used by Research Team.
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
use QCMG::IO::GapSummaryRecord;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    croak "QCMG::IO::GapSummaryReader:new() requires filename or zipname parameter" 
        unless ( (exists $params{filename} and defined $params{filename}) or
                 (exists $params{zipname} and defined $params{zipname}) );

    my $self = { filename        => $params{filename},
                 filehandle      => undef,
                 headers         => [],
                 record_ctr      => 0,
                 version         => ($params{version} ?
                                     $params{version} : 'version_2_0'),
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

    my $version = $self->version;
    croak "unsupported CNV GAP file version [$version]"
        unless (exists $QCMG::IO::GapSummaryRecord::VALID_HEADERS->{$version});
    
    $self->_parse_headers;

    return $self;
}


sub _parse_headers {
    my $self = shift;

    # Read off headers
    my @headers = ();
    while (1) {
        my $line = $self->filehandle->getline();
        next unless defined $line;  # ditch if there are no headers
        chomp $line;
        next unless $line;        # skip blanks
        # If we got to here then we must have the header line
        $line =~ s/^#//;  # ditch leading '#' if present
        @headers = split /\t/, $line;
        last;
    }

    my @valid_headers =
        @{ $QCMG::IO::GapSummaryRecord::VALID_HEADERS->{$self->version} };
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

sub version {
    my $self = shift;
    return $self->{version};
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
            foreach my $ctr (0..$#fields) {
                # If the record has been processed in Excel, some of the
                # test files are going to come enclosed in " chars.
                $fields[$ctr] =~ s/^\"//;
                $fields[$ctr] =~ s/\"$//;
            }
            my $rec = QCMG::IO::GapSummaryRecord->new(
                          version => $self->version,
                          data    => \@fields,
                          headers => $self->{headers} );
            return $rec;
        }
    }
}


1;

__END__


=head1 NAME

QCMG::IO::GapSummaryReader - CNV GAP summary file IO


=head1 SYNOPSIS

 use QCMG::IO::GapSummaryReader;


=head1 DESCRIPTION

This module provides an interface for reading QCMG CNV GAP summary files as
used by Research Team.

=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id$


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
