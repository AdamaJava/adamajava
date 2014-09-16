package QCMG::IO::PropertiesReader;

###########################################################################
#
#  Module:   QCMG::IO::PropertiesReader
#  Creator:  John V Pearson
#  Created:  2012-05-29
#
#  Reads Bioscope "properties" file.
#
#  $Id: PropertiesReader.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
###########################################################################

use strict;
use warnings;
use IO::File;
use Data::Dumper;
use Carp qw( carp croak );

use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 4663 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: PropertiesReader.pm 4663 2014-07-24 06:39:00Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    croak "PropertiesReader:new() requires a filename" 
        unless (exists $params{filename} and defined $params{filename});

    my $self = { filename        => $params{filename},
                 headers         => [],
                 params          => {},
                 records         => {},
                 record_ctr      => 0,
                 _filehandle     => '',
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;

    $self->_initialise;
    
    return $self;
}


sub filename {
    my $self = shift;
    return $self->{filename} = shift if @_;
    return $self->{filename};
}

sub name {
    my $self = shift;
    return $self->{name} = shift if @_;
    return $self->{name};
}

sub filehandle {
    my $self = shift;
    return $self->{_filehandle} = shift if @_;
    return $self->{_filehandle};
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub record_ctr {
    my $self = shift;
    return $self->{record_ctr};
}

sub headers {
    my $self = shift;
    return $self->{headers};
}

sub params {
    my $self = shift;
    return $self->{params};
}

sub records {
    my $self = shift;
    return $self->{records};
}

sub record_count {
    my $self = shift;
    my @seqs = keys %{ $self->{records} };
    return scalar @seqs;
}

sub _incr_record_ctr {
    my $self = shift;
    return $self->{record_ctr}++;
}


# This process will read all records.
sub _initialise {
    my $self = shift;

    my $infh =IO::File->new( $self->filename, 'r' )
       or croak 'Can\'t open properties file [', $self->filename, "] for reading: $!";
    $self->filehandle( $infh );

    my %seqs = ();

    # Read off headers and first record
    while (my $line = $self->filehandle->getline) {
        # Read off the header (if any)
        if ($line =~ /^#/) {
            push @{ $self->{headers} }, $line;
            next;
        }
        # To get here we must have hit a record
        chomp $line;
        my ($name,$value) = split /=/, $line, 2;
        if ($name =~ /c\.(\d+)\.(\w+)/) {
            $seqs{ $1 }->{ $2 } = $value;
        }
        elsif ($name =~ /[\.\w]/) {
            $self->{params}->{ $name } = $value;
        }
        else {
            die "Unable to parse line [$line]\n";
        }
    }

    $self->{records} = \%seqs;
}



1;

__END__


=head1 NAME

QCMG::IO::PropertiesReader - Properties file IO


=head1 SYNOPSIS

 use QCMG::IO::SamReader;


=head1 DESCRIPTION

This module provides an interface for reading properties files as used
by Bioscope.  The properties file defines the names, lengths and
cumulative base pairs in a series of sequences (usually chromosomes)
that collectively constitute a "genome" for Bioscope to align against.
A properties file looks like this:

 #Created by ReferenceProperies
 #Sun Jul 04 08:25:38 EST 2010
 version=4.0.0
 reference.length=3101804739
 number.of.contigs=84
 c.1.H=chr1
 c.1.L=249250621
 c.1.P=0
 c.2.H=chr2
 c.2.L=243199373
 c.2.P=252811351
 c.3.H=chr3
 ...

For an example file, see:
/panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.properties

=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: PropertiesReader.pm 4663 2014-07-24 06:39:00Z j.pearson $


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
