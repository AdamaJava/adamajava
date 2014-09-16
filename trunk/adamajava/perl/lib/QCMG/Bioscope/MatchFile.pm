package QCMG::Bioscope::MatchFile;

###########################################################################
#
#  Module:   QCMG::Bioscope::MatchFile.pm
#  Creator:  John V Pearson
#  Created:  2010-03-10
#
#  Reads ma alignment files output by AB SOLiD Bioscope pipeline.
#
###########################################################################

use strict;
use warnings;

use IO::File;
use IO::Zlib;
use Data::Dumper;

use QCMG::Bioscope::MatchFileRecord;


sub new {
    my $class = shift;
    my %params = @_;

    die "MatchFile->new() requires a filename or zipname parameter" 
        unless ( (exists $params{filename} and defined $params{filename}) or
                 (exists $params{zipname} and defined $params{zipname}) );

    my $self = { filename        => $params{filename},
                 headers         => [],
                 _filehandle     => undef,
                 _record_ctr     => 0,
                 _defline        => '',
                 _sequence       => '',
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;

    # If there is a zipname, we use it in preference to filename.  We only
    # process one so if both are specified, the zipname wins.

    if ( $params{zipname} ) {
        my $fh = IO::Zlib->new( $params{zipname}, 'r' );
        die 'Unable to open ', $params{zipname}, "for reading: $!"
            unless defined $fh;
        $self->filename( $params{zipname} );
        $self->_filehandle( $fh );
    }
    elsif ( $params{filename} ) {
        my $fh = IO::File->new( $params{filename}, 'r' );
        die 'Unable to open ', $params{filename}, "for reading: $!"
            unless defined $fh;
        $self->filename( $params{filename} );
        $self->_filehandle( $fh );
    }

    # Read off header lines (if any) and stop when we hit the first defline.

    while (my $line = $self->_filehandle->getline()) {
        if ($line =~ /^#/) {
            push @{ $self->{headers} }, $line;
        }
        elsif ($line =~ /^>/) {
            $self->{_defline}  = $line;
            last;  # if we hit a defline then drop out of the loop
        }
        else {
            die 'I should have found headers or a defline and instead '.
                "I found this: [$line]";
        }
    }

    return $self;
}


sub filename {
    my $self = shift;
    return $self->{filename} = shift if @_;
    return $self->{filename};
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub headers {
    my $self = shift;
    return @{ $self->{headers} };
}

sub record_count {
    my $self = shift;
    return $self->{_record_ctr};
}

sub next_record {
    my $self = shift;

    # Note that there is intentionally NO chomping to simplify output!
    while (my $line = $self->_filehandle->getline()) {
        if ($line =~ /^>/) {
            if ($self->{_defline}) {
                $self->_incr_record_ctr;
                if ($self->verbose) {
                    # Print progress messages for every 1M records
                    print( '[' . localtime() . '] '. $self->record_count,
                           " records processed\n" )
                        if $self->record_count % 1000000 == 0;
                }
                #my $rec = BioscopeMaRecord->new(
                my $rec = QCMG::Bioscope::MatchFileRecord->new(
                           defline  => $self->{_defline},
                           sequence => $self->{_sequence},
                           verbose  => $self->verbose );
                $self->{_defline}  = $line;
                $self->{_sequence} = '';
                return $rec;
            }
            $self->{_defline}  = $line;
            $self->{_sequence} = '';
        }
        else {
            $self->{_sequence} .= $line
        }
    }

    # Handle the special case of the last record
    if ($self->{_defline}) {
        $self->_incr_record_ctr;
        #my $rec = BioscopeMaRecord->new(
        my $rec = QCMG::Bioscope::MatchFileRecord->new(
                   defline  => $self->{_defline},
                   sequence => $self->{_sequence},
                   verbose  => $self->verbose );
        $self->{_defline}  = '';
        $self->{_sequence} = '';
        return $rec;
    }

    return undef;
}


sub _incr_record_ctr {
    my $self = shift;
    return $self->{_record_ctr}++;
}

sub _filehandle {
    my $self = shift;
    return $self->{_filehandle} = shift if @_;
    return $self->{_filehandle};
}


1;
__END__


=head1 NAME

QCMG::Bioscope::MatchFile - Bioscope match file IO


=head1 SYNOPSIS

 use QCMG::Bioscope::MatchFile;


=head1 DESCRIPTION

This module provides an interface for reading Bioscope match files.

A match file contains multiple comment lines that document the
program(s) run to create the match file.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $ma = QCMG::Bioscope::MatchFile->new(
                filename => '300001_20100310_F3.ma',
                verbose  => 1 );  

A file must be supplied to this constructor although it can be a plain
text file (filename attribute) or a zipped file (zipfile attribute).

=item B<filename()>
 
 $ma->filename();

Returns the name of the match file loaded in this object.

=item B<verbose()>

 $ma->verbose();

Returns the verbose status for this object where 0 sets verbose off 
and any other value (traditionally 1) sets verbose mode on.

=item B<headers()>

 my @headers = $ma->headers();

Returns an array of the header lines read from the file.

=item B<next_record()>

 my $rec = $ma->next_record;

Returns an object of type QCMG::Bioscope::MatchFileRecord which contains
the next match file record.

=back


=head1 SEE ALSO

=over

=item QCMG::Bioscope::MatchFile::Record

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: MatchFile.pm 4660 2014-07-23 12:18:43Z j.pearson $


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
