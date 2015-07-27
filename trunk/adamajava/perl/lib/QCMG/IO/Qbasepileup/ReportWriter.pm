package QCMG::IO::Qbasepileup::ReportWriter;

###########################################################################
#
#  Module:   QCMG::IO::Qbasepileup::ReportWriter
#  Creator:  John V Pearson
#  Created:  2014-08-07
#
#  Writes qBasepileup text files in the same format as is written by the 
#  java qbasepileup utility.
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

use QCMG::IO::Qbasepileup::ReportRecord;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    confess "new() requires filename" 
        unless (exists $params{filename} and defined $params{filename});
    confess "new() requires version" 
        unless (exists $params{version} and defined $params{version});

    confess( "unsupported version [$params{version}]" )
        unless (exists $QCMG::IO::Qbasepileup::ReportRecord::VALID_HEADERS->{$params{version}});

    my $self = { filename        => $params{filename},
                 file_version    => $params{version},
                 record_ctr      => 0,
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;

    my $fh = IO::File->new( $params{filename}, 'w' );
    confess 'Unable to open ', $params{filename}, " for writing: $!"
        unless defined $fh;
    $self->filename( $params{filename} );
    $self->filehandle( $fh );

    qlogprint( 'Writing to file ',$self->filename."\n" ) if $self->verbose;

    $self->_write_headers;

    return $self;
}


sub _write_headers {
    my $self = shift;

    my $fh = $self->filehandle;

    $fh->print( '##qbasepileup version '. $self->version ."\n" );

    my @headers = @{ $QCMG::IO::Qbasepileup::ReportRecord::VALID_HEADERS->{$self->version} };
    $self->{headers} = \@headers;
    $fh->print( join("\t",@headers),"\n" );

    $self->{headers} = \@headers;
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
    return $self->{file_version};
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


sub write_record {
    my $self = shift;
    my $rec  = shift;

    # Check that it's an object of the correct type
    my $ref = ref( $rec );
    die "illegal object type [$ref]"
        unless ($ref eq 'QCMG::IO::Qbasepileup::ReportRecord');

    # Check that the file and record versions match
    die 'version mismatch: file [', $self->version,
        '] vs record [', $rec->version, "]\n"
        unless ($self->version eq $rec->version);

    $self->filehandle->print( $rec->to_text, "\n" );
    $self->_incr_record_ctr;
}


1;

__END__


=head1 NAME

QCMG::IO::Qbasepileup::ReportWriter - qbasepileup file writer


=head1 SYNOPSIS

 use QCMG::IO::Qbasepileup::ReportWriter;


=head1 DESCRIPTION

This module provides an interface for writing files  in the same format
as files written by the qbasepileup utility.

A qbasepileup file starts with an optional comment block but it MUST 
contain the qbasepileup version number as a comment and the next line 
must be a non-comment line of column headers.  An example version 1.0 
qbasepileup file showing the first 3 columns of the first few lines:

 ##qbasepileup version 1.0
 ID  Donor      Bam
 1   APGI_2997  /mnt/seq_results/icgc_pancreatic/APGI_2997/seq_final/...
 2   APGI_2997  /mnt/seq_results/icgc_pancreatic/APGI_2997/seq_final/...
 3   APGI_2997  /mnt/seq_results/icgc_pancreatic/APGI_2997/seq_final/...
 4   APGI_2997  /mnt/seq_results/icgc_pancreatic/APGI_2997/seq_final/...

=head1 METHODS

=over

=item B<new()>

=item B<write_record()>

=item B<filename()>

=item B<filehandle()>

=item B<record_ctr()>

The nummber of records written.

=item B<version()>

=item B<verbose()>

=item B<headers()>

=back


=head1 AUTHORS

John Pearson L<mailto:john.pearson@qimrberghofer.edu.au>


=head1 VERSION

$Id$


=head1 COPYRIGHT

This software is copyright 2014 by the QIMR Berghofer Medical Research
Institute.  All rights reserved.

=cut
