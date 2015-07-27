package QCMG::IO::MafReader;

###########################################################################
#
#  Module:   QCMG::IO::MafReader
#  Creator:  John V Pearson
#  Created:  2011-10-18
#
#  Reads MAF (Mutation Annotation Format) files as used by TCGA and the
#  ABO collaboration (QCMG/Baylor/OICR) for Pancreatic Ca variants.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak confess );
use Data::Dumper;
use IO::File;
use IO::Zlib;

use QCMG::IO::MafRecord;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    confess "new() requires filename or zipname parameter" 
        unless ( (exists $params{filename} and defined $params{filename}) or
                 (exists $params{zipname} and defined $params{zipname}) );

    my $self = { filename        => $params{filename},
                 filehandle      => undef,
                 headers         => [],
                 comments        => [],
                 record_ctr      => 0,
                 maf_version     => '',
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;

    # If there a zipname, we use it in preference to filename.  We only
    # process one so if both are specified, the zipname wins.

    if ( $params{zipname} ) {
        my $fh = IO::Zlib->new( $params{zipname}, 'r' );
        confess 'Unable to open ', $params{zipname}, " for reading: $!"
            unless defined $fh;
        $self->filename( $params{zipname} );
        $self->filehandle( $fh );
    }
    elsif ( $params{filename} ) {
        my $fh = IO::File->new( $params{filename}, 'r' );
        confess 'Unable to open ', $params{filename}, " for reading: $!"
            unless defined $fh;
        $self->filename( $params{filename} );
        $self->filehandle( $fh );
    }

    qlogprint( 'Reading from MAF file ',$self->filename."\n" ) if $self->verbose;

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
        if ($line =~ /^#version.([\d\.]+)$/i) {
            $version = $1;
        }
        elsif ($line =~ /^#/) {
            push @{ $self->{comments} }, $line;
        }
        else {
            # Must be the headers
            @headers = split /\t/, $line;
            last;
        }
    }

    # Set default
    if (! $version) {
        $version = '2.2';
        qlogprint( "no version header in MAF file - defaulting to $version\n" );
    }

    qlogprint( "MAF version $version\n" );
    qlogprint( 'MAF file header count ', scalar(@headers), "\n" );

    confess( "unsupported MAF file version [$version] in file ",
           $self->filename )
        unless (exists $QCMG::IO::MafRecord::VALID_HEADERS->{$version});
    $self->{maf_version} = $version;
    
    my @valid_headers = @{ $QCMG::IO::MafRecord::VALID_HEADERS->{$version} };
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

    # Make separate lists of regular and extra headers
    my $vcount = scalar(@valid_headers);
    my @extra_fields = @headers[ $vcount..(scalar(@headers)-1) ];

    $self->{regular_fields} = \@valid_headers;
    $self->{extra_fields}   = \@extra_fields;
    $self->{headers}        = \@headers;
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

sub headers {
    my $self = shift;
    return @{ $self->{headers} };
}


sub comments {
    my $self = shift;
    return @{ $self->{comments} };
}


sub regular_fields {
    my $self = shift;
    return @{ $self->{regular_fields} };
}


sub extra_fields {
    my $self = shift;
    return @{ $self->{extra_fields} };
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
                # Print progress messages for every 1M records
                qlogprint( $self->record_ctr, " MAF records processed\n" )
                    if $self->record_ctr % 100000 == 0;
            }
            my @fields = split /\t/, $line;
            my $maf = QCMG::IO::MafRecord->new( version => $self->maf_version,
                                                data    => \@fields,
                                                headers => $self->{headers} );
            return $maf;
        }
    }
}


1;

__END__


=head1 NAME

QCMG::IO::MafReader - MAF file IO


=head1 SYNOPSIS

 use QCMG::IO::MafReader;


=head1 DESCRIPTION

This module provides an interface for reading Mutation
Annotation Format (MAF) files as a series of MafRecord objects.

A MAF file starts with an optional comment block but it MUST contain the
MAF version number as a comment and the next line must be a non-comment
line of comment headers.  An example MAF file showing the first 5
columns of the first few lines:

 #version 2.2
 Hugo_Symbol  Entrez_Gene_Id  Center          NCBI_Build  Chromosome
 SCNN1D       0               qcmg.uq.edu.au  37          1
 AJAP1        0               qcmg.uq.edu.au  37          1
 PIK3CD       0               qcmg.uq.edu.au  37          1
 PTCHD2       0               qcmg.uq.edu.au  37          1
 FHAD1        0               qcmg.uq.edu.au  37          1 


=head1 METHODS

=over

=item B<new()>

=item B<next_record()>

=item B<filename()>

=item B<filehandle()>

=item B<record_ctr()>

=item B<maf_version()>

=item B<verbose()>

=item B<headers()>

=item B<comments()>

Returns an array of strings representing the comment lines read from
this file.

=item B<regular_fields()>

=item B<extra_fields()>

=back


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id$


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
