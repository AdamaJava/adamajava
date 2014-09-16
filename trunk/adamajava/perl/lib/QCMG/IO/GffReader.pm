package QCMG::IO::GffReader;

###########################################################################
#
#  Module:   QCMG::IO::GffReader
#  Creator:  John V Pearson
#  Created:  2010-01-26
#
#  Reads GFF v2 files output by AB SOLiD pipeline.
#
#  $Id: GffReader.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use QCMG::IO::SamRecord;
use QCMG::IO::GffRecord;
use IO::File;
use Carp qw( confess );

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4663 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: GffReader.pm 4663 2014-07-24 06:39:00Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    confess "QCMG::IO::GffReader:new() requires filename or zipname parameter" 
        unless ( (exists $params{filename} and defined $params{filename}) or
                 (exists $params{zipname} and defined $params{zipname}) );

    my $self = { filename        => $params{filename},
                 version         => '2',
                 headers         => {},
                 valid_headers   => {},
                 record_ctr      => 0,
                 matepair        => ($params{matepair} ?
                                     $params{matepair} : 0),
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;

    # GFF headers specified by AB come in 2 flavours: unique and multiple.
    # We also force all header tags to be lc to avoid complications.

    my @valid_unique_headers   = qw( solid-gff-version gff-version
                                     source-version date time type
                                     color-code primer-base
                                     max-num-mismatches max-read-length
                                     line-order hdr );
    my @valid_multiple_headers = qw( history contig );
    $self->{valid_headers} = {}; 
    $self->{valid_headers}->{ $_ } = 1 foreach @valid_unique_headers;
    $self->{valid_headers}->{ $_ } = 2 foreach @valid_multiple_headers;

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

sub matepair {
    my $self = shift;
    return $self->{matepair};
}

sub cmdline {
    my $self = shift;
    return $self->{cmdline};
}


sub _valid_headers {
    my $self = shift;
    return $self->{valid_headers};
}

sub _headers {
    my $self = shift;
    return $self->{headers};
}

sub header {
    my $self   = shift;
    my $attrib = shift;

    if (exists  $self->{headers}->{$attrib} and
        defined $self->{headers}->{$attrib}) {
        return $self->{headers}->{$attrib};
    }
    else {
        return undef;
    }
}


sub _incr_record_ctr {
    my $self = shift;
    return $self->{record_ctr}++;
}


sub record_ctr {
    my $self = shift;
    return $self->{record_ctr};
}


sub next_record {
    my $self = shift;

    # Read lines, checking for and processing any headers
    # and only return once we have a record

    while (1) {
        my $line = $self->filehandle->getline();
        # Catch EOF
        return undef if (! defined $line);
        if ($line =~ /^##/) {
            $self->process_gff2_header_line( $line );
        }
        elsif ($line =~ /^#/) {
            # skip comment line
        }
        else {
            if ($self->verbose) {
                # Print progress messages for every 1M records
                $self->_incr_record_ctr;
                print( $self->record_ctr, ' GFF records processed: ',
                       localtime().'', "\n" )
                    if $self->record_ctr % 100000 == 0;
            }
            my $gff = QCMG::IO::GffRecord->new( $line );
            $gff->matepair( $self->matepair );
            print $gff->debug if ($self->verbose > 1);
            return $gff;
        }
    }
}


sub process_gff2_header_line {
    my $self = shift;
    my $line = shift;

    chomp $line;
    $line =~ s/^##//;

    my ($tag, $value) = split ' ', $line, 2;
    $tag = lc( $tag );

    confess "Unknown GFF2 header [$tag]"
        unless (exists $self->_valid_headers->{$tag} and
                defined $self->_valid_headers->{$tag} );

    # Headers can be classed as unique or multiple
    if ($self->_valid_headers->{$tag} == 1) {
        $self->{headers}->{$tag} = $value;
    }
    if ($self->_valid_headers->{$tag} == 2) {
        push @{ $self->{headers}->{$tag} }, $value;
    }
}


sub history_as_sam_pg {
    my $self = shift;

    my $pg ='';
    
    # Return empty string if no history present
    return $pg unless ( exists $self->{headers}->{history} and
                        scalar( @{ $self->{headers}->{history} } ) > 0 );

    foreach (@{ $self->{headers}->{history} }) {
       my $cmdline = $_;
       $cmdline =~ /(.*?) /;
       my $ptmp = $1;
       my @ptmps = split /\//, $ptmp;
       my $program = pop @ptmps;
       $pg .= "\@PG\t" . 'ID:' . $program . "\t" .
                       'CL:' . $cmdline . "\n";
    }

    return $pg;
}


sub contigs_as_sam_sq {
    my $self = shift;

    my $sq ='';

    # Return empty string if no contigs present
    return $sq unless ( exists $self->{headers}->{contig} and
                        scalar( @{ $self->{headers}->{contig} } ) > 0 );

    foreach (@{ $self->{headers}->{contig} }) {
       my ($id, $defline) = split /\s/, $_, 2;
       $sq .= "\@SQ\t" . 'SN:' . 'seq_' . $id . "\t" .
                         'UR:' . $defline . "\n";
    }

    return $sq;
}


sub next_record_as_sam {
    my $self = shift;

    my $gff = $self->next_record;
    return undef unless defined $gff;
    my $sam = QCMG::IO::SamRecord->new( gff => $gff );
    return $sam;
}

1;

__END__


=head1 NAME

QCMG::IO::GffReader - GFF file IO


=head1 SYNOPSIS

 use QCMG::IO::GffReader;


=head1 DESCRIPTION

This module provides an interface for reading and writing GFF Files


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: GffReader.pm 4663 2014-07-24 06:39:00Z j.pearson $


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
