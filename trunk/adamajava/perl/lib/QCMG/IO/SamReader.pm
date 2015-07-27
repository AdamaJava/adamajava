package QCMG::IO::SamReader;

###########################################################################
#
#  Module:   QCMG::IO::SamReader
#  Creator:  John V Pearson
#  Created:  2010-10-02
#
#  Reads SAM and BAM files (via conversion to SAM).
#
#  $Id$
#
###########################################################################

use strict;
use warnings;
use IO::File;
use Data::Dumper;
use Carp qw( carp croak );

use QCMG::IO::SamRecord;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    croak "SamReader:new() requires a filename" 
        unless (exists $params{filename} and defined $params{filename});

    my $self = { filename        => $params{filename},
                 headers         => [],
                 samtoolsbin     => ($params{samtoolsbin} ?
                                     $params{samtoolsbin} : 'samtools'),
                 record_ctr      => 0,
                 _this_record    => '',
                 _filehandle     => undef,
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;

    # Determine file type from extension, open, read off headers if any
    my $filename = $self->{filename};
    if ($filename =~ /\.bam/) {
        $self->_open_string( $self->{samtoolsbin} ." view -h $filename |" );
        my $infh =IO::File->new( $self->_open_string )
           or croak "Can't open samtools view on BAM [$filename]: $!";
        $self->_filehandle( $infh );
    }
    elsif ($filename =~ /\.sam/) {
        $self->_open_string( "<$filename" );
        my $infh =IO::File->new( $self->_open_string )
           or croak "Can't open SAM file [$filename]: $!";
        $self->_filehandle( $infh );
    }
    else {
        croak "File type for [$filename] cannot be determined ",
              'because extension is not .bam or .sam'; 
    }
    $self->_initialise;

    return $self;
}


sub filename {
    my $self = shift;
    return $self->{filename} = shift if @_;
    return $self->{filename};
}

sub _filehandle {
    my $self = shift;
    return $self->{_filehandle} = shift if @_;
    return $self->{_filehandle};
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub _open_string {
    my $self = shift;
    return $self->{_open_string} = shift if @_;
    return $self->{_open_string};
}

sub _headers {
    my $self = shift;
    return $self->{headers};
}

sub headers_text {
    my $self = shift;
    # Header lines were not chomped so newlines still in place
    return join('', @{$self->{headers}});
}

sub _incr_record_ctr {
    my $self = shift;
    return $self->{record_ctr}++;
}

sub _initialise {
    my $self = shift;

    qlogprint 'initialising reader for file '.$self->filename ."\n" 
       if $self->verbose;

    # Read off headers and first record
    while (my $line = $self->_filehandle->getline) {
        # Read off the header (if any)
        if ($line =~ /^\@/) {
            push @{ $self->{headers} }, $line;
            next;
        }
        # To get here we must have hit a record
        chomp $line;
        $self->{_record} = $line;
        $self->_incr_record_ctr;
        last;
    }

    if ($self->verbose > 1) {
        my @headers = @{ $self->{headers} };
        qlogprint 'read ',scalar(@headers), ' header lines from ',
                  $self->filename,"\n";
    }
}


sub current_record {
    my $self = shift;
    return QCMG::IO::SamRecord->new( sam => $self->{_record} );
}


sub _next_record {
    my $self = shift;

    # Read the next record into the record buffer and return the 
    # previous (current) buffer contents.  If there is nothing in
    # current record then we must have hit EOF on the last 
    # _next_record call so return undef.

    return undef unless $self->{_record};
    my $record = $self->{_record};
    my $line = $self->_filehandle->getline;

#    my $status = 'defined? ';
#    $status .= (defined $line) ? ' yes ' : 'no ';
#    $status .= ' true? ';
#    $status .= ($line) ? ' yes ' : 'no ';
#    $status .= "\n";
#    print "$status\n";

    $self->{_record} = $line;

    return $record unless defined $line;
    
    chomp $self->{_record};
    $self->_incr_record_ctr;
    # If verbose, print progress line every 1M records
    if ($self->verbose and $self->{record_ctr} % 1000000 == 0) {
        print( '[' . localtime() . '] ' . $self->{record_ctr},
               ' records processed from ' . $self->{filename} ."\n" );
    }
    return $record;
}

sub next_record_as_text {
    my $self = shift;
    return $self->_next_record;
}

sub next_record_as_record {
    my $self = shift;

    # This was the only contents of this method when it was first
    # created for the BAM pairing annotation task
    #my @fields = split /\t/, $self->_next_record;
    #my ($zb,$zf) = int_to_bit_string( $fields[1] ); 

    return QCMG::IO::SamRecord->new( sam => $self->_next_record );
}


sub int_to_bit_string {
    my $int = shift;

    # Special case
    return ( '0000000000000000', '0' ) if ($int == 0);

    my $zb = '';
    foreach my $powerof2 (32768,16384,8192,4096,2048,1024,512,256,128,64,32,16,8,4,2,1) {
        my $this_bit = floor( $int / $powerof2 );
        if ($this_bit > 0) {
            $zb .= '1';
            $int -= $powerof2;
        }
        else {
            $zb .= '0';
        }
    }

    my $zf = '';
    my $zf_mask = '     dfs21RrUuPp';
    foreach (0..15) {
        if (substr($zb,15-$_,1) eq '1') {
            $zf .= substr($zf_mask,15-$_,1);
        }
    }
    return ($zb,$zf);
}


1;

__END__


=head1 NAME

QCMG::IO::SamReader - SAM/BAM file IO


=head1 SYNOPSIS

 use QCMG::IO::SamReader;

 my $bam = QCMG::IO::SamReader->new( filename => $infile );
 while (my $rec = $bam->next_record_as_record) {
   ...
 }


=head1 DESCRIPTION

This module provides an interface for reading SAM and BAM files.  It
uses the file extension to tell whether the file is a BAM or SAM and if
the file is a BAM, it uses "samtools view" pipe to access the records.


=head1 PUBLIC METHODS

=over 2

=item B<new()>

 my $bam = QCMG::IO::SamReader->new( filename => 'mybam.bam',
                                     verbose  => 1 );

The B<new()> method takes 1 mandatory and 2 optional parameters.  The
mandatory param is I<filename> which is the pathname to the file to be
read and the optional params are I<verbose> which default to 0 and
indicates the level of verbosity in reporting, and I<samtoolsbin> which
default to "samtools" but which can be used to give a full pathname to
the samtools binary in cases where the binary may not be in $PATH.

=item B<filename()>

 $bam->filename( 'hohoho.bam' );
 my $name = $bam->filename;

Accessor for filename.  No point using the setter because the only
way to trigger processing of a new file is via B<new()>.

=item B<verbose()>

 $bam->verbose( 2 );
 my $verb = $bam->verbose;

Accessor for verbosity level of progress reporting.

=item B<current_record()>

Returns the current record as a QCMG::IO::SamRecord object.
Does not advance the record reader.

=item B<next_record_as_text()>

Returns the next record from the file as a text string.
Advances the record reader.
Returns undef if there are no more records to be read.

=item B<next_record_as_record()>

Returns the next record from the file as a QCMG::IO::SamRecord object.
Advances the record reader.
Returns undef if there are no more records to be read.

=back


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
