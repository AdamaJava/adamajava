package QCMG::IO::VcfWriter;

###########################################################################
#
#  Module:   QCMG::IO::VcfWriter
#  Creator:  John V Pearson
#  Created:  2010-01-26
#
#  Reads Variant Call Format (VCF) files as specified by the 1000
#  Genomes project.  Loosely based on an NHGRI module.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use Carp qw( confess );
use QCMG::IO::VcfRecord;
use vars qw( $SVNID $REVISION @VALID_COLUMNS );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


BEGIN {
    @VALID_COLUMNS = ( 'CHROM', 'POS', 'ID', 'REF', 'ALT',
                       'QUAL', 'FILTER', 'INFO' );
}

sub new {
    my $class = shift;
    my %params = @_;

    confess "VcfWriter:new() requires a filename or zipname parameter" 
        unless ( (exists $params{filename} and defined $params{filename}) or
                 (exists $params{zipname} and defined $params{zipname}) );

    my $self = { filename        => ( $params{filename} ?
                                      $params{filename} : '' ),
                 zipname         => ( $params{zipname} ?
                                      $params{zipname} : '' ),
                 version         => '4.0',
                 headers         => {},
                 filters         => {},
                 infos           => {},
                 record_count    => 0,
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;

    # If there a zipname, we use it in preference to filename.  We only
    # process one so if both are specified, the zipname wins.

    if ( $params{zipname} ) {
        my $fh = IO::Zlib->new( $params{zipname}, 'w' );
        confess 'Unable to open ', $params{zipname}, " for writing: $!"
            unless defined $fh;
        $self->filename( $params{zipname} );
        $self->filehandle( $fh );
    }
    elsif ( $params{filename} ) {
        my $fh = IO::File->new( $params{filename}, 'w' );
        confess 'Unable to open ', $params{filename}, " for writing: $!"
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


sub version {
    my $self = shift;
    return $self->{version} = shift if @_;
    return $self->{version};
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub record_count {
    my $self = shift;
    return $self->{record_count};
}


sub header {
    my $self = shift;
    my $name = shift;
    return undef unless $name;
    return $self->{headers}->{$name} = shift if @_;
    return $self->{headers}->{$name};
}


sub filter {
    my $self = shift;
    my $name = shift;
    return undef unless $name;
    return $self->{filters}->{$name} = shift if @_;
    return $self->{filters}->{$name};
}


sub info {
    my $self = shift;
    my $name = shift;
    return undef unless $name;
    return $self->{infos}->{$name} = shift if @_;
    return $self->{infos}->{$name};
}


sub write_headers {
    my $self = shift;
    
    my $outfh = $self->filehandle;
    
    print $outfh '##fileformat=VCFv' . $self->version . "\n";
    foreach my $name (sort keys %{ $self->{headers} }) {
        print $outfh "##$name=" . $self->{headers}->{$name} ."\n"; 
    }
    foreach my $name (sort keys %{ $self->{filters} }) {
        print $outfh "##FILTER=<ID=$name," . $self->{filters}->{$name} .">\n"; 
    }
    foreach my $name (sort keys %{ $self->{infos} }) {
        print $outfh "##INFO=<ID=$name," . $self->{infos}->{$name} .">\n"; 
    }
    print $outfh '#' . join("\t",@VALID_COLUMNS) . "\n";
}


sub write_record {
    my $self = shift;
    my $rec  = shift;
    
    my $outfh = $self->filehandle;
    print $outfh $rec->to_text . "\n";
    $self->_incr_record_count;
}


sub write_text {
    my $self = shift;
    my $rec  = shift;
    
    my $outfh = $self->filehandle;
    print $outfh $rec;
    $self->_incr_record_count;
}


sub _incr_record_count {
    my $self = shift;
    return $self->{record_count}++;
}


sub close {
    my $self = shift;
    $self->filehandle->close;
}


1;

__END__


=head1 NAME

QCMG::IO::VcfWriter - VCF file IO


=head1 SYNOPSIS

 use QCMG::IO::VcfWriter;
  
 my $vcf = QCMG::IO::VcfWriter->new( file => 'sample1.vcf' );
 $vcf->header( 'fileformat', 'VCFv4.0' );
 $vcf->header( 'patient_id', 'APGI_1992' );
 $vcf->info( 'COV', 'Number=.,Type=Number,Description="coverage"' );
 $vcf->write_headers
 $vcf->write_record( $vcf_record );
 $vcf->close;


=head1 DESCRIPTION

This module provides an interface for writing VCF Files.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $vcf = QCMG::IO::VcfWriter->new( filename => 'sample1.vcf' );

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
