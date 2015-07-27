package QCMG::IO::GenomeStudioReader;

###########################################################################
#
#  Module:   QCMG::IO::GenomeStudioReader
#  Creator:  John Pearson
#  Created:  2012-03-07
#
#  Reads text files from Genome Studio for Illumina SNP chips.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use Carp qw( croak confess );
use QCMG::IO::DccRecord;
use vars qw( $SVNID $REVISION @HEADERS );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;

@HEADERS = ( 'SNP Name',
             'Sample ID',
             'Allele1 - Top',
             'Allele2 - Top',
             'GC Score',
             'Sample Name',
             'Sample Group',
             'Sample Index',
             'SNP Index',
             'SNP Aux',
             'Allele1 - Forward',
             'Allele2 - Forward',
             'Allele1 - Design',
             'Allele2 - Design',
             'Allele1 - AB',
             'Allele2 - AB',
             'Chr',
             'Position',
             'GT Score',
             'Cluster Sep',
             'SNP',
             'ILMN Strand',
             'Customer Strand',
             'Top Genomic Sequence',
             'Theta',
             'R',
             'X',
             'Y',
             'X Raw',
             'Y Raw',
             'B Allele Freq',
             'Log R Ratio' );

sub new {
    my $class = shift;
    my %params = @_;
    
    croak "DccReader:new() requires a filename" 
        unless (exists $params{filename} and defined $params{filename});

      my $self = { filename     => $params{filename},
                   headers      => {},
                   filehandle   => '',
                   record_count      => 0,
                   verbose         => ($params{verbose} ?
                                       $params{verbose} : 0),
                 };

      bless $self, $class;

      my $fh = IO::File->new( $params{filename}, 'r' );
      confess 'Unable to open ', $params{filename}, " for reading: $!"
          unless defined $fh;
      $self->filename( $params{filename} );
      $self->filehandle( $fh );

      $self->_initialise;

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

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub record_count {
    my $self = shift;
    return $self->{record_count};
}

sub _valid_headers {
    my $self = shift;
    return $self->{valid_headers};
}

sub _headers {
    my $self = shift;
    return $self->{headers};
}

sub _increment_record_count {
    my $self = shift;
    return $self->{record_count}++;
}


sub _initialise {
    my $self = shift;
    
    # Read off headers and column headers

    my $line = $self->filehandle->getline();
    die 'Must start with [Header]' unless $line =~ /^\[Header\]/;

    while (1) {
        my $line = $self->filehandle->getline();
        chomp $line;
        last if $line =~ /\[Data\]/;
        my ($key,$val) = split /\t/, $line, 2;
        $self->{headers}->{$key} = $val;
    }

    # Check column names

    my $headers = $self->filehandle->getline();
    chomp $headers;
    my @headers = split /\t/, $headers;
    my $problems = 0;

    foreach my $expected_header (@HEADERS) {
        my $actual_header = shift @headers;
        unless ( $expected_header =~ m/$actual_header/i) {
            warn "Header mismatch - ",
                 "expected: $expected_header, found: $actual_header";
                $problems++;
        }
    }
    die "Unable to continue until all header problems have been resolved"
       if ($problems > 0);
}


sub next_record {
    my $self = shift;
    
    while (my $line = $self->filehandle->getline()) {
        chomp $line;
        
        if ($self->verbose) {
            # Print progress messages for every 1M records
            $self->_increment_record_count;
            print( $self->record_count, ' VCF records processed: ',
                localtime().'', "\n" )
                if $self->record_count % 100000 == 0;
        }

        my @vals = split /\t/, $line;
        my %data = ();
        foreach my $i (0..$#HEADERS) {
            $data{ $HEADERS[$i] } = $vals[$i];
        }

        return \%data;
    }

    return undef;
}


1;

__END__

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
