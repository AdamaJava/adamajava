package QCMG::IO::VcfReader;

###########################################################################
#
#  Module:   QCMG::IO::VcfReader
#  Creator:  John V Pearson
#  Created:  2010-01-26
#
#  Reads Variant Call Format (VCF) files as specified by the 1000
#  Genomes project.  Loosely based on an NHGRI module.
#
#  $Id: VcfReader.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use Carp qw( confess );
use QCMG::IO::VcfRecord;
use vars qw( $SVNID $REVISION @VALID_COLUMNS );

use QCMG::Util::QLog;

( $REVISION ) = '$Revision: 4663 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: VcfReader.pm 4663 2014-07-24 06:39:00Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


BEGIN {
    @VALID_COLUMNS = ( 'CHROM', 'POS', 'ID', 'REF', 'ALT', '',
                       'QUAL', 'FILTER', 'INFO' );
}

sub new {
    my $class = shift;
    my %params = @_;

    confess "VcfReader:new() requires a filename or zipname parameter" 
        unless ( (exists $params{filename} and defined $params{filename}) or
                 (exists $params{zipname} and defined $params{zipname}) );

    my $self = { filename        => ( $params{filename} ?
                                      $params{filename} : 0),
                 zipname         => ( $params{zipname} ?
                                      $params{zipname} : 0),
                 version         => '2',
                 headers         => {},
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

    qlogprint( 'Opening VCF file for reading - ',$self->filename,"\n" )
        if ($self->verbose);

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


sub close {
    my $self = shift;
    return $self->filehandle->close;
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

        $self->_incr_record_count;
        my $vcf = QCMG::IO::VcfRecord->new( $line );
        #warn $vcf->debug if ($self->verbose > 1);

        if ($self->verbose) {
            # Print progress messages for every 1M records
            qlogprint( $self->record_count/1000000, "M VCF records processed\n" )
                if ($self->record_count % 1000000 == 0);
        }
        return $vcf;
    }
}


sub _process_header_line {
    my $self = shift;
    my $line = shift;

    if ($line =~ /^##/) {
        $line =~ s/^\##//;
        my ($key,$value) = split /=/, $line, 2;
        if ($key =~ /INFO/ or $key =~ /FILTER/) {
           if ($value =~ /ID=(\w+)/) {
               $self->{headers}->{$key}->{$1} = $value;
           }
           else {
               warn "No ID= pattern found in $key $value\n";
           }
        }
        else {
            $self->{headers}->{$key} = $value;
        }
    }
    else {
        $line =~ s/^\#//;
        my @columns = split /\t/, $line;
        my $problems = 0;
    
        my @my_valid_columns = @VALID_COLUMNS;
        # Cope with weirdness of Lynn's old VCF with extra tab
        if ($columns[5] eq 'QUAL') {
            splice @my_valid_columns, 5, 1;
        }

        foreach my $expected_column (@my_valid_columns) {
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

QCMG::IO::VcfReader - VCF file IO


=head1 SYNOPSIS

 use QCMG::IO::VcfReader;
  
 my $vcf = QCMG::IO::VcfReader->new( filename => 'sample1.vcf' );


=head1 DESCRIPTION

This module provides an interface for reading and writing VCF Files.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $vcf = QCMG::IO::VcfReader->new( filename => 'sample1.vcf' );

=item B<debug()>

 $vcf->debug(1);

Flag to force increased warnings.  Defaults to 0 (off);

=item B<next_record()>

 my $rec = $vcf->next_record();

This method will rteunr the next record as an QCMG::IO::VCFFile
object.

=back


=head1 AUTHORS

=over

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: VcfReader.pm 4663 2014-07-24 06:39:00Z j.pearson $


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
