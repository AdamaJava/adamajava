package QCMG::IO::FastaReader;

###########################################################################
#
#  Module:   QCMG::IO::FastaReader
#  Creator:  John V Pearson
#  Created:  2012-11-03
#
#  Reads a FASTA file and creates an array of sequence hashes.
#
#  $Id: FastaReader.pm 4663 2014-07-24 06:39:00Z j.pearson $
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
( $SVNID ) = '$Id: FastaReader.pm 4663 2014-07-24 06:39:00Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    croak "QCMG::IO::FastaReader:new() requires filename or zipname parameter" 
        unless ( (exists $params{filename} and defined $params{filename}) or
                 (exists $params{zipname} and defined $params{zipname}) );

    my $self = { filename        => $params{filename},
                 filehandle      => undef,
                 sequences       => [],
                 line_ctr        => 0,
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

    $self->_parse_file();

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

sub line_ctr {
    my $self = shift;
    return $self->{line_ctr};
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub sequences {
    my $self = shift;
    return $self->{sequences};
}


sub _parse_file {
    my $self = shift;

    qlogprint( 'Processing FASTA file ', $self->filename, "\n" );

    my $fh      = $self->filehandle;
    my @seqs    = ();
    my $defline = '';
    my $seq     = '';

    while (my $line = $fh->getline) {
        chomp $line;
        $self->{line_ctr}++;
        $line =~ s/\s+$//;        # trim trailing spaces
        next if ($line =~ /^#/);  # skip comments
        next unless $line;        # skip blank lines

        if ($line =~ /^>/) {
            # defline means we've hit a new (or first) sequence
            if ($defline) {
                push @seqs, { defline  => $defline,
                              sequence => $seq };
            }
            qlogprint( "Reading sequence $line\n" ) if $self->verbose;
            $defline = $line;
            $seq     = '';
        }
        else {
            # we are extending our current sequence
            $seq .= $line;
        }
    }

    # There should be one last sequence
    if ($defline) {
        push @seqs, { defline  => $defline,
                      sequence => $seq };
    }

    $self->{sequences} = \@seqs;
}


1;

__END__


=head1 NAME

QCMG::IO::FastaReader - Reads FASTA format files


=head1 SYNOPSIS

 use QCMG::IO::FastaReader;


=head1 DESCRIPTION

This module provides an interface for reading FASTA format files.


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 METHODS

=over

=item B<new()>

my $far = QCMG::IO::FastaReader->new(
              filename => 'infile.fa',
              verbose  => 1 );

=item B<sequences()>

 my $ra_seqs = $far->sequences();

Returns an arrayref of hashes where each hash represents a sequence and
contains contains a defline and a sequence.

=back


=head1 VERSION

$Id: FastaReader.pm 4663 2014-07-24 06:39:00Z j.pearson $


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
