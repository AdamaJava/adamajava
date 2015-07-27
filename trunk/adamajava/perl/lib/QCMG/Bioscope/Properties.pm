package QCMG::Bioscope::Properties;

##############################################################################
#
#  Module:   QCMG::Bioscope::Properties.pm
#  Creator:  John V Pearson
#  Created:  2010-03-10
#
#  Reads properties files output by AB SOLiD Bioscope pipeline.
#
###########################################################################

use strict;
use warnings;

use IO::File;
use Data::Dumper;

sub new {
    my $class = shift;
    my %params = @_;

    die "QCMG::Bioscope::Properties->new() requires a filename parameter" 
        unless (exists $params{filename} and defined $params{filename});

    my $self = { filename           => $params{filename},
                 headers            => [],
                 contigs            => {},
                'reference.length'  => 0,
                'number.of.contigs' => 0,
                 version            => '',
                 verbose            => ($params{verbose} ?
                                       $params{verbose} : 0),
               };

    bless $self, $class;

    $self->_parse_file;

    return $self;
}


sub filename {
    my $self = shift;
    return $self->{filename};
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub reference_length {
    my $self = shift;
    return $self->{'reference.length'};
}

sub number_of_contigs {
    my $self = shift;
    return $self->{'number.of.contigs'};
}

sub contig_name {
    my $self   = shift;
    my $contig = shift;

    if (exists $self->{contigs}->{ $contig }) {
        return $self->{contigs}->{ $contig }->{name};
    }
    return undef;
}


sub contig_length {
    my $self   = shift;
    my $contig = shift;

    if (exists $self->{contigs}->{ $contig }) {
        return $self->{contigs}->{ $contig }->{length};
    }
    return undef;
}


sub _headers {
    my $self = shift;
    return $self->{headers};
}

sub _parse_file {
    my $self = shift;

    my $fh = IO::File->new( $self->filename, 'r' );
    die 'Unable to open properties file [', $self->filename,
        "] for reading: $!" unless defined $fh;

    print 'processing properties file [', $self->filename, "]\n"
        if $self->verbose;

    my %contigs = ();
    while (my $line = $fh->getline) {
        chomp $line;
        $line =~ s/\s+$//;        # trim trailing spaces
        next if ($line =~ /^#/);  # skip comments
        next unless $line;        # skip blank lines

        if ($line =~ /^#/) {
            push @{ $self->headers }, $line;
        }
        elsif ($line =~ /^reference\.length=(\d*)$/) {
            $self->{'reference.length'} = $1;
        }
        elsif ($line =~ /^number\.of\.contigs=(\d*)$/) {
            $self->{'number.of.contigs'} = $1;
        }
        elsif ($line =~ /^version=(.*)$/) {
            $self->{version} = $1;
        }
        elsif ($line =~ /^c\.(\d+)\.H=(.*)$/) {
            $self->{contigs}->{$1}->{name} = $2;
        }
        elsif ($line =~ /^c\.(\d+)\.L=(.*)$/) {
            $self->{contigs}->{$1}->{length} = $2;
        }
        else {
            die "Can't parse this line: $line\n";
        }
    }

}

1;
__END__


=head1 NAME

QCMG::Bioscope::Properties - Bioscope Properties file IO


=head1 SYNOPSIS

 use QCMG::Bioscope::Properties;


=head1 DESCRIPTION

This module provides an interface for reading and writing Bioscope
Properties files that provide a mapping between the contig IDs in
Bioscope alignment files and the original reference FASTA files.

A Properties file contains a couple of comment lines, some lines that
describe the contents of the properties file and a list of the names and
lengths of the sequences (contigs) that were used for the alignment.  An
example file (with contigs 4-24 elided) looks like:

 #Created by ReferenceProperies
 #Tue Feb 09 00:52:54 GMT-07:00 2010
 reference.length=3095693983
 number.of.contigs=25
 version=3.5.0
 c.1.H=chr10
 c.1.L=135534747
 c.2.H=chr11
 c.2.L=135006516
 c.3.H=chr12
 c.3.L=133851895
 ...
 c.25.H=chrY
 c.25.L=59373566


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $prop = QCMG::Bioscope::Properties->new(
                file => 'hg19.properties' );  

A file parameter must be supplied to this constructor.

=item B<filename()>
 
 $prop->filename();

Returns the name of the properties file loaded in this object.

=item B<verbose()>

 $prop->verbose();

Returns the verbose status for this object where 0 sets verbose off 
and any other value (traditionally 1) sets verbose mode on.

=item B<reference_length()>

 $prop->reference_length();

Returns the summed length of all contigs.  Read from the header, not
by actually summing the contig lengths.

=item B<contig_count()>

 $prop->contig_count();

Returns the number of contigs in the properties file.
Read from the header, not by actually counting the contigs.

=item B<contig_name()>

 $prop->contig_name( 2 );

Returns the name of the contig number specified.
Returns undef if the contig specified does not exist.

=item B<contig_length()>

 $prop->contig_length( 2 );

Returns the length of the contig number specified.
Returns undef if the contig specified does not exist.

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2009-2014

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
