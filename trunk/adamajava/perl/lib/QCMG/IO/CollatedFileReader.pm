package QCMG::IO::CollatedFileReader;

###########################################################################
#
#  Module:   QCMG::IO::CollatedFileReader
#  Creator:  John V Pearson
#  Created:  2010-02-13
#
#  Reads a collated as created by RNA-Mate or X-Mate.  A collated file
#  is very similar to the .ma matches file created by the ABI
#  corona-lite/mapreads pipelein except that the match location is
#  listed in the defline in a slightly different way.  Both files are
#  very similar to a multiple sequence FASTA file, i.e. the matches
#  appear as multiple sequence records where each sequence has a defline
#  followed by the sequence matched.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use Carp qw( confess );
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;

sub new {
    my $class = shift;
    my %params = @_;

    confess "CollatedFileReader->new() requires a file parameter" 
        unless (exists $params{file} and defined $params{file});

    my $self  = { file         => $params{file},
                  verbose      => ($params{verbose} ? $params{verbose} : 0),
                  _defline     => '',
                  _recctr      => 0,
                  CreationTime => localtime().'',
                  Version      => $REVISION, 
                };

    bless $self, $class;
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub file {
    my $self = shift;
    return $self->{file};
}

sub sequences {
    my $self = shift;
    return @{ $self->{sequences} };
}

sub creation_time {
    my $self = shift;
    return $self->{'CreationTime'};
}

sub creator_module_version {
    my $self = shift;
    return $self->{'Version'};
}

sub parse_file {
    my $self = shift;

    my $fh = IO::File->new( $self->file, 'r' );
    confess 'Unable to open FASTA file [', $self->file, "] for reading: $!"
        unless defined $fh;

    # Process FASTA string in case it contains multiple sequences
    print 'processing FASTA file [', $self->file, "]\n"
        if $self->verbose;

    my @seqs    = ();
    my $defline = '';
    my $seq     = '';

    while (my $line = $fh->getline) {
        chomp $line;
        $line =~ s/\s+$//;        # trim trailing spaces
        next if ($line =~ /^#/);  # skip comments
        next unless $line;        # skip blank lines

        if ($line =~ /^>/) {
            if ($defline) {
                push @seqs, Bio::TGen::Util::Sequence->new(
                                defline  => $defline,
                                sequence => $seq,
                                verbose  => $self->verbose );
            }
            $defline = $line;
            $seq     = '';
        }
        else {
            $seq .= $line;
        }
    }

    # Catch last sequence
    if ($defline) {
        push @seqs, Bio::TGen::Util::Sequence->new(
                        defline  => $defline,
                        sequence => $seq,
                        verbose  => $self->verbose );
    }

    $self->{sequences} = \@seqs;
}


sub write_seqs {
    my $self = shift;
    foreach my $seq ($self->sequences) {
       $seq->write_FASTA_file;
    }
}

1;
__END__


=head1 NAME

QCMG::IO::CollatedFileReader - Read X-Mate collated alignment files


=head1 SYNOPSIS

 use QCMG::IO::CollatedFileReader;


=head1 DESCRIPTION

This module provides an interface for reading and writing collated
alignment files as created by RNA-Mate and X-Mate. 


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id$


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
