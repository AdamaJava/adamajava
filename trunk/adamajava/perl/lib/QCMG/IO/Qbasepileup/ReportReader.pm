package QCMG::IO::Qbasepileup::ReportReader;

###########################################################################
#
#  Module:   QCMG::IO::Qbasepileup::ReportReader
#  Creator:  John V Pearson
#  Created:  2014-08-07
#
#  Reads qBasepileup text files written by the java qbasepileup utility.
#
#  $Id: ReportReader.pm 4683 2014-08-07 23:24:18Z j.pearson $
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

( $REVISION ) = '$Revision: 4683 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: ReportReader.pm 4683 2014-08-07 23:24:18Z j.pearson $'
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
                 file_version    => '',
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

    qlogprint( 'Reading from file ',$self->filename."\n" ) if $self->verbose;

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
        if ($line =~ /^##qbasepileup version.([\d\.]+)$/i) {
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

    if ($self->verbose) {
        qlogprint( "file version $version\n" );
        qlogprint( 'header count ', scalar(@headers), "\n" );
    }

    confess( "unsupported qbasepileup file version [$version] in file ",
           $self->filename )
        unless (exists $QCMG::IO::Qbasepileup::ReportRecord::VALID_HEADERS->{$version});
    $self->{file_version} = $version;
    
    my @valid_headers = @{ $QCMG::IO::Qbasepileup::ReportRecord::VALID_HEADERS->{$version} };
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

    # If there are extra columns, warn about them because they will be
    # dropped by the record objects.
    my $fcount = scalar(@headers);
    my $vcount = scalar(@valid_headers);
    if ($fcount > $vcount) {
        my @extra_columns = @headers[ $vcount..($fcount-1) ];
        warn "file contains extra columns - ",
             "these will be lost in processing [",
             join(',',@extra_columns),"\n";
    }

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


sub comments {
    my $self = shift;
    return @{ $self->{comments} };
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
                qlogprint( $self->record_ctr, " qbasepileup records processed\n" )
                    if $self->record_ctr % 100000 == 0;
            }
            my @fields = split /\t/, $line;
            my $rec = QCMG::IO::Qbasepileup::ReportRecord->new(
                          version => $self->version,
                          data    => \@fields );
            return $rec;
        }
    }
}


1;

__END__


=head1 NAME

QCMG::IO::Qbasepileup::ReportReader - qbasepileup file reader


=head1 SYNOPSIS

 use QCMG::IO::Qbasepileup::ReportReader;


=head1 DESCRIPTION

This module provides an interface for reading files written by the
qbasepileup utility.

A qbasepileup file starts with an optional comment block but it MUST 
contain the qbasepileup version number as a comment and the next line 
must be a non-comment line of column headers.  An example qbasepileup file 
showing the first 3 columns of the first few lines:

 ##qbasepileup version 1.0
 ID  Donor      Bam
 1   APGI_2997  /mnt/seq_results/icgc_pancreatic/APGI_2997/seq_final/...
 2   APGI_2997  /mnt/seq_results/icgc_pancreatic/APGI_2997/seq_final/...
 3   APGI_2997  /mnt/seq_results/icgc_pancreatic/APGI_2997/seq_final/...
 4   APGI_2997  /mnt/seq_results/icgc_pancreatic/APGI_2997/seq_final/...

=head1 METHODS

=over

=item B<new()>

=item B<next_record()>

=item B<filename()>

=item B<filehandle()>

=item B<record_ctr()>

=item B<version()>

=item B<verbose()>

=item B<headers()>

=item B<comments()>

Returns an array of strings representing the comment lines read from
this file.

=back


=head1 AUTHORS

John Pearson L<mailto:john.pearson@qimrberghofer.edu.au>


=head1 VERSION

$Id: ReportReader.pm 4683 2014-08-07 23:24:18Z j.pearson $


=head1 COPYRIGHT

This software is copyright 2014 by the QIMR Berghofer Medical Research
Institute.  All rights reserved.

=cut
