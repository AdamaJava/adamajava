package QCMG::IO::qPileupReader;

###########################################################################
#
#  Module:   QCMG::IO::qPileupReader
#  Creator:  John V Pearson
#  Created:  2012-05-18
#
#  Reads qPileup export files.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use Carp qw( confess );
use QCMG::IO::qPileupRecord;
use vars qw( $SVNID $REVISION @VALID_COLUMNS );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


BEGIN {
    @VALID_COLUMNS = @QCMG::IO::qPileupRecord::QPILEUP_COLUMNS;
}

sub new {
    my $class = shift;
    my %params = @_;

    confess "qPileupReader:new() requires a filename or zipname parameter" 
        unless ( (exists $params{filename} and defined $params{filename}) or
                 (exists $params{zipname} and defined $params{zipname}) );

    my $self = { filename        => ( $params{filename} ?
                                      $params{filename} : 0),
                 zipname         => ( $params{zipname} ?
                                      $params{zipname} : 0),
                 version         => '2',
                 headers         => {},
                 metadata        => [],
                 record_count    => 0,
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


sub headers {
    my $self = shift;
    return $self->{headers};
}


sub _incr_record_count {
    my $self = shift;
    return $self->{record_count}++;
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

        if ($line =~ /^#/) {
            $self->_process_header_line( $line );
            next;
        }

        if ($self->verbose) {
            # Print progress messages for every 1M records
            $self->_incr_record_count;
            print( $self->record_count, ' VCF records processed: ',
                   localtime().'', "\n" )
                if $self->record_count % 100000 == 0;
        }
        my $vcf = QCMG::IO::qPileupRecord->new( $line );
        print $vcf->debug if ($self->verbose > 1);
        return $vcf;
    }
}


sub _process_header_line {
    my $self = shift;
    my $line = shift;

    if ($line =~ /^##/) {
        $line =~ s/^\##//;
        my ($key,$value) = split /=/, $line, 2;
        if ($key =~ /METADATA/) {
            push @{ $self->{metadata} }, $value;
        }
        else {
            $self->{headers}->{$key} = $value;
        }
    }
    else {
        $line =~ s/^\#//;
        my @columns = split /\t/, $line;
        my $problems = 0;
    
        foreach my $expected_column (@VALID_COLUMNS) {
            my $actual_column = shift @columns;
            unless ( $expected_column =~ m/$actual_column/i) {
            warn "Column mismatch - ",
                 "expected [$expected_column] found [$actual_column]\n";
                $problems++;
            }
        }
        die "Unable to continue until all column problems have been resolved\n"
           if ($problems > 0);
    }
}


1;

__END__


=head1 NAME

QCMG::IO::qPileupReader - qPileup file IO


=head1 SYNOPSIS

 use QCMG::IO::qPileupReader;
  
 my $vcf = QCMG::IO::qPileupReader->new( file => 'chr12.qpileup.csv' );


=head1 DESCRIPTION

This module provides an interface for reading qPileup files created by
the B<--view> mode..


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $vcf = QCMG::IO::qPileupReader->new( file => 'chr12.qpileup.csv' );

=item B<debug()>

 $vcf->debug(1);

Flag to force increased warnings.  Defaults to 0 (off);

=back


=head1 AUTHORS

=over

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id$


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
