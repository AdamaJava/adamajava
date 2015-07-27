package QCMG::IO::DccSnpReader;

###########################################################################
#
#  Module:   QCMG::IO::DccSnpReader
#  Creator:  John V Pearson
#  Created:  2012-02-09
#
#  Reads ICGC DCC SNP data submission tab-separated text file tables.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak );
use Data::Dumper;
use IO::File;
use IO::Zlib;
use QCMG::IO::DccSnpRecord;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    croak "QCMG::IO::DccSnpReader:new() requires filename or zipname parameter" 
        unless ( (exists $params{filename} and defined $params{filename}) or
                 (exists $params{zipname} and defined $params{zipname}) );

    my $self = { filename        => $params{filename},
                 filehandle      => undef,
                 headers         => [],
                 qexec           => [],
                 qcmgmeta        => [],
                 annotations     => {},
                 record_ctr      => 0,
                 dcc_release     => undef,
                 dcc_type        => undef,
                 dcc_table       => undef,
                 variant_type    => undef,
                 version         => ($params{version} ?
                                     $params{version} : ''),
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;

    # If there a zipname, we use it in preference to filename.  We only
    # process one so if both are specified, the zipname wins.

    if ( $params{zipname} ) {
        my $fh = IO::Zlib->new( $params{zipname}, 'r' );
        croak 'Unable to open ', $params{zipname}, " for reading: $!"
            unless defined $fh;
        $self->filename( $params{zipname} );
        $self->filehandle( $fh );
    }
    elsif ( $params{filename} ) {
        my $fh = IO::File->new( $params{filename}, 'r' );
        croak 'Unable to open ', $params{filename}, " for reading: $!"
            unless defined $fh;
        $self->filename( $params{filename} );
        $self->filehandle( $fh );
    }

    qlogprint( 'Reading from file '. $self->filename ."\n") if $self->verbose;

    $self->_parse_headers();

    return $self;
}


sub _parse_headers {
    my $self = shift;

    # Read off version and headers
    my @headers = ();
    while (1) {
        my $line = $self->filehandle->getline();
        chomp $line;
        if ($line =~ /^#(.*?): (.*)$/i) {
            $self->{annotations}->{$1} = $2;
        }
        elsif ($line =~ /^#Q_EXEC/) {
            # Capture '#Q_EXEC' lines that refer to the creation of this file
            push @{ $self->{qexec} }, $line;
        }
        elsif ($line =~ /^#Q_/) {
            # Capture any other QCMG metadata lines 
            push @{ $self->{qcmgmeta} }, $line;
        }
        else {
            # Must be the headers
            @headers = split /\t/, $line;
            last;
        }
    }

    # If no version was supplied then we need to guess.  This is not
    # without risk so specifying the version is preferable

    my $version = '';
    if ($self->version) {
        $version = $self->version;

        # Check that the supplied DCC file version actually exists
        die "Unknown DccSnpReader version [$version]\n" 
            unless exists $QCMG::IO::DccSnpRecord::VALID_HEADERS->{$version};
        my @valid_headers =
            @{ $QCMG::IO::DccSnpRecord::VALID_HEADERS->{$version} };

        # There could be more headers than the required ones but we only
        # need to validate the columns that appear in the official spec.
        foreach my $ctr (0..$#valid_headers) {
            if ($headers[$ctr] ne $valid_headers[$ctr]) {
               die "Invalid header in column [$ctr] - ".
                   'should have been ['. $valid_headers[$ctr] .
                   '] but is ['. $headers[$ctr] . ']';
            }
        }
    }
    else {
        $version = $self->_guess_version_from_headers( \@headers );
        die "unable to determine DCC file type from headers\n" unless defined $version;
        $self->{version} = $version;
        qlogprint( "based on headers, DCC file appears to be version [$version]\n" );
    }

    # Now that we have the version (input by user or guessed from headers),
    # we will extract the DCC filetype, release version, and somatic/germline
    # from the version string.

    $self->{dcc_release}  = ($self->version =~ /_r(\d+)$/)   ? $1 : undef;
    $self->{dcc_type}     = ($self->version =~ /^(dcc.*?)_/) ? $1 : undef;
    $self->{dcc_table}    = ($self->version =~ /_db(.*?)_/)  ? $1 : undef;
    $self->{variant_type} = ($self->version =~ /_somatic/) ? 'somatic' :
                            ($self->version =~ /_germline/) ? 'germline' : undef;

    # If verbose, report on what sort of file we think we have loaded
    qlogprint( 'characterising file as type:',
               ($self->{dcc_type} ? $self->{dcc_type} : 'unknown'),
               ', release:',
               ($self->{dcc_release} ? $self->{dcc_release} : 'unknown'),
               ', variant:',
               ($self->{variant_type} ? $self->{variant_type} : 'unknown'),
               ', table:',
               ($self->{dcc_table} ? $self->{dcc_table} : 'unknown'),
               ' based on file type [',
               $self->version, "]\n" ) if $self->verbose;

    $self->{headers} = \@headers;
}


sub _guess_version_from_headers {
    my $self       = shift;
    my $ra_headers = shift;

    my @versions = sort keys %{ $QCMG::IO::DccSnpRecord::VALID_HEADERS };

    my $chosen_version = undef;
    foreach my $version (@versions) {
#        warn "checking version $version\n";
        my @valid_headers =
            @{ $QCMG::IO::DccSnpRecord::VALID_HEADERS->{$version} };

        # We are looking for a perfect match so if the numbers of
        # headers don't match then we are done with this version

        if (scalar(@{ $ra_headers }) != scalar(@valid_headers)) {
#            warn "not version $version because we have ",
#                scalar(@{ $ra_headers }), ' headers and it has ',
#                scalar(@valid_headers), "\n";
            next;
        }

        # There could be more headers than the required ones but we only
        # need to validate the columns that appear in the official spec.
        $chosen_version = $version;
        foreach my $ctr (0..$#valid_headers) {
            if ($ra_headers->[$ctr] ne $valid_headers[$ctr]) {
#                warn "header mismatch at position $ctr - ",
#                    $ra_headers->[$ctr], ' vs ', $valid_headers[$ctr], "\n";
                $chosen_version = undef;
                last;
            }
        }

        # Exit if we failed to find a mismatch with this version
        last if defined $chosen_version;
    }

#    qlogprint( "chosen version: $chosen_version\n" );
    return $chosen_version;
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

sub _incr_record_ctr {
    my $self = shift;
    return $self->{record_ctr}++;
}

sub record_ctr {
    my $self = shift;
    return $self->{record_ctr};
}

sub version {
    my $self = shift;
    return $self->{version};
}

sub dcc_type {
    my $self = shift;
    return $self->{dcc_type};
}

sub dcc_release {
    my $self = shift;
    return $self->{dcc_release};
}

sub variant_type {
    my $self = shift;
    return $self->{variant_type};
}

sub dcc_table {
    my $self = shift;
    return $self->{dcc_table};
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


sub qexec_string {
    my $self = shift;
    return undef unless (@{ $self->{qexec} });
    return join("\n", @{ $self->{qexec} }) ."\n";
}


sub qcmgmeta_string {
    my $self = shift;
    return undef unless (@{ $self->{qcmgmeta} });
    return join("\n", @{ $self->{qcmgmeta} }) ."\n";
}


sub next_record {
    my $self = shift;

    # Read lines, checking for and processing any headers
    # and only return once we have a record

    while (1) {
        my $line = $self->filehandle->getline();

        # Catch EOF
        if (! defined $line) {
            qlogprint( 'read ',$self->record_ctr, " records\n" );
            return undef;
        }

        chomp $line;

        $self->_incr_record_ctr;
        if ($self->verbose) {
             # Print progress messages for every 1M records
             qlogprint( $self->record_ctr, " DCC records processed\n" )
                 if $self->record_ctr % 100000 == 0;
        }
        my @fields = split /\t/, $line;
        my $rec = QCMG::IO::DccSnpRecord->new( data    => \@fields,
                                               version => $self->version,
                                               headers => $self->{headers} );
        return $rec;
    }
}


1;

__END__


=head1 NAME

QCMG::IO::DccSnpReader - ICGC DCC SNP annotation file IO


=head1 SYNOPSIS

 use QCMG::IO::DccSnpReader;


=head1 DESCRIPTION

This module provides an interface for reading and writing SNP annotation
files in the format(s) required by the ICGC DCC.


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 METHODS

=over

=item B<new()>

my $dccr = QCMG::IO::DccSnpReader->new(
               filename => 'infile.txt',
               version  => 'dcc1a',
               verbose  => 1 );

The version parameter determines the column headers that the object
expects to find in the supplied file.  It will check them and will die
immediately if there is a mismatch.  If all is well, each time you call
B<next_record()> you will get a DccSnpRecord objects back with a matching
version number.

If you do not supply a version, the module will use the observed headers
to predict the DCC file type.  This is not without risk so it is
preferable to specify a version.  There are times when you cannot
predict the version but in those cases, unless you are only dealing with
fields that are common to all versions, you are going to need to provide
your own handlers for each file version.

Ther valid versions are shown below along with the headers that are
expected for each.  You can actually have a file with more
headers and it will be parsed correctly and all of the data will be
visible using accessors named for the column headers BUT if you call the
B<to_text()> method on that DccSnpRecord, only the fields that are in
the official version spec will be written back out to disk.  

The current legal versions and their column headers are:

 Version : dccq
 Headers : mutation_id mutation_type chromosome chromosome_start
           chromosome_end chromosome_strand refsnp_allele refsnp_strand 
           reference_genome_allele control_genotype tumour_genotype
           mutation quality_score probability read_count is_annotated
           validation_status validation_platform xref_ensembl_var_id 
           note ND TD consequence_type aa_mutation cds_mutation
           protein_domain_affected gene_affected
           transcript_affected gene_build_version note_s gene_symbol
           All_domains All_domains_type All_domains_description

 Version : dcc1a
 Headers : analysis_id tumour_sample_id mutation_id
           mutation_type chromosome chromosome_start chromosome_end
           chromosome_strand refsnp_allele refsnp_strand
           reference_genome_allele control_genotype tumour_genotype
           mutation quality_score probability read_count is_annotated
           validation_status validation_platform xref_ensembl_var_id
           note QCMGflag ND TD NNS

 Version : dcc1b
 Headers : analysis_id tumour_sample_id mutation_id
           mutation_type chromosome chromosome_start chromosome_end
           chromosome_strand refsnp_allele refsnp_strand
           reference_genome_allele control_genotype tumour_genotype
           mutation quality_score probability read_count is_annotated
           validation_status validation_platform xref_ensembl_var_id
           note QCMGflag normalcount tumourcount

 Version : dcc1_somatic1
 Headers : analysis_id analyzed_sample_id mutation_id mutation_type
           chromosome chromosome_start chromosome_end
           chromosome_strand refsnp_allele refsnp_strand
           reference_genome_allele control_genotype tumour_genotype
           mutation quality_score probability read_count is_annotated
           validation_status validation_platform xref_ensembl_var_id
           note QCMGflag ND TD NNS FlankSeq

 Version : dcc1_germline1
 Headers : analysis_id control_sample_id variation_id variation_type
           chromosome chromosome_start chromosome_end
           chromosome_strand refsnp_allele refsnp_strand
           reference_genome_allele control_genotype tumour_genotype
           mutation quality_score probability read_count is_annotated
           validation_status validation_platform xref_ensembl_var_id
           note QCMGflag ND TD NNS FlankSeq

=item B<next_record()>

 my $rec = $dccr->next_record();

Returns a QCMG::IO::DccSnpRecord.

=item B<record_ctr()>

Returns a count of how many records have been processed.

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
