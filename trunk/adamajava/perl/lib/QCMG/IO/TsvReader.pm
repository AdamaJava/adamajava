package QCMG::IO::TsvReader;

###########################################################################
#
#  Module:   QCMG::IO::TsvReader
#  Creator:  John V Pearson
#  Created:  2012-11-24
#
#  Generic reader for tab-separated data files.  The intent is that this
#  module be subclassed to create Readers for specific flavours of data
#  file.
#
#  $Id: TsvReader.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak );
use Data::Dumper;
use IO::File;
use IO::Zlib;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4663 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: TsvReader.pm 4663 2014-07-24 06:39:00Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    croak "QCMG::IO::TsvReader:new() requires filename or zipname parameter" 
        unless ( (exists $params{filename} and defined $params{filename}) or
                 (exists $params{zipname} and defined $params{zipname}) );
    croak "QCMG::IO::TsvReader:new() requires an array of headers"
        unless (exists $params{headers} and defined $params{headers});

    my $self = { filename        => $params{filename},
                 headers         => $params{headers},
                 filehandle      => undef,
                 comments        => [],
                 record_ctr      => 0,
                 version         => ($params{version} ?
                                     $params{version} : ''),
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;

    # If there is a zipname, we use it in preference to filename.  We only
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

    # Read off comments and headers
    my @headers = ();
    while (1) {
        my $line = $self->filehandle->getline();
        chomp $line;
        if ($line =~ /^#/) {
            push @{ $self->{comments} }, $line;
        }
        else {
            # Must be the headers
            @headers = split /\t/, $line;
            last;
        }
    }

    my @valid_headers = @{ $self->{headers} };

    die 'expected ', scalar(@valid_headers),
         ' headers but found ', scalar(@headers), "\n"
        unless (scalar(@headers) == scalar(@valid_headers));
            
    # There could be more headers than the required ones but we only
    # need to validate the columns that appear in the official spec.

    foreach my $ctr (0..$#valid_headers) {
        if ($headers[$ctr] ne $valid_headers[$ctr]) {
            die "header mismatch at position $ctr - ",
                $headers[$ctr], ' vs ', $valid_headers[$ctr], "\n";
        }
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

sub headers {
    my $self = shift;
    return @{ $self->{headers} };
}


sub next_record {
    my $self = shift;

    # Read lines, skipping comments

    while (1) {
        my $line = $self->filehandle->getline();
        # Catch EOF
        return undef if (! defined $line);
        chomp $line;

        # Skip comments
        next if ($line =~ /^#/);

        $self->_incr_record_ctr;
        if ($self->verbose) {
             # Print progress messages for every 1M records
             qlogprint( $self->record_ctr, " records processed\n" )
                 if $self->record_ctr % 1000000 == 0;
        }

        return $line;
    }
}


1;

__END__


=head1 NAME

QCMG::IO::TsvReader - Generic reader for TSV files


=head1 SYNOPSIS

 use QCMG::IO::TsvReader;


=head1 DESCRIPTION

This module provides an interface for reading generic TSV files with
column headers as the first no-blank, non-comment line.


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 METHODS

=over

=item B<new()>

 my $tsv = QCMG::IO::TsvReader->new(
               filename => 'infile.txt',
               headers  => \@headers,
               verbose  => 1 );

You must supply a list of headers to be checked against the headers in
the file.
The two lists must match perfectly including case or the file
cannot be read.
The file can contain more headers than the supplied list so you can have
files with a set of fixed columns and one or more additional columns
which can have any column name or none at all.

=item B<next_record()>

 my $line = $tsv->next_record();

Returns a line of text.

=item B<record_ctr()>

Returns a count of how many records have been processed.

=back


=head1 VERSION

$Id: TsvReader.pm 4663 2014-07-24 06:39:00Z j.pearson $


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
