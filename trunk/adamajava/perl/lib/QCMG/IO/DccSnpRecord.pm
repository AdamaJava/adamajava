package QCMG::IO::DccSnpRecord;

###########################################################################
#
#  Module:   QCMG::IO::DccSnpRecord
#  Creator:  John V Pearson
#  Created:  2010-02-12
#
#  Data container for a SNP record from an IGCG DCC data submission file
#  (tab-separated text).
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Memoize;
use vars qw( $SVNID $REVISION $VALID_HEADERS $VALID_AUTOLOAD_METHODS );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;

our $AUTOLOAD;  # it's a package global

BEGIN {
    my @v_dccq =
        qw( mutation_id mutation_type chromosome chromosome_start
            chromosome_end chromosome_strand refsnp_allele refsnp_strand 
            reference_genome_allele control_genotype tumour_genotype mutation
            quality_score probability read_count is_annotated validation_status
            validation_platform xref_ensembl_var_id note ND TD consequence_type
            aa_mutation cds_mutation protein_domain_affected gene_affected
            transcript_affected gene_build_version note_s gene_symbol
            All_domains All_domains_type All_domains_description );
    my @v_dcc1a =
        qw( analysis_id tumour_sample_id mutation_id
            mutation_type chromosome chromosome_start chromosome_end
            chromosome_strand refsnp_allele refsnp_strand reference_genome_allele
            control_genotype tumour_genotype mutation quality_score probability
            read_count is_annotated validation_status validation_platform
            xref_ensembl_var_id note
            QCMGflag ND TD NNS );
    my @v_dcc1b =
        qw( analysis_id tumour_sample_id mutation_id
            mutation_type chromosome chromosome_start chromosome_end
            chromosome_strand refsnp_allele refsnp_strand reference_genome_allele
            control_genotype tumour_genotype mutation quality_score probability
            read_count is_annotated validation_status validation_platform
            xref_ensembl_var_id note
            QCMGflag normalcount tumourcount );

    # This is a special case of a BADLY formed DCC1 file which exists in
    # wide distribution - it is missing the expressed_allele field after
    # the mutation field.  This record was created so we can read in
    # files of the "bad" type and clone the records to the "good" format
    # as a trivial way to remediate the misformed bad files.
 
    my @v_dcc1_dbssm_BAD_somatic_r11 =
        qw( analysis_id analyzed_sample_id mutation_id mutation_type
            chromosome chromosome_start chromosome_end
            chromosome_strand refsnp_allele refsnp_strand reference_genome_allele
            control_genotype tumour_genotype mutation quality_score probability
            read_count is_annotated validation_status validation_platform
            xref_ensembl_var_id note
            QCMGflag ND TD NNS FlankSeq );

    # As of 2012-11, DCC release 11, the names of DCC files for
    # somatic and germline started to diverge so we had to start
    # providing separate recipes for them.  It's also worth noting that
    # fields QCMGflag and beyond are QCMG-only fields and are not part of
    # the DCC spec.  All QMCG-added fields start with a capital letter.

    my @v_dcc1_dbssm_somatic_r11 =
        qw( analysis_id analyzed_sample_id mutation_id mutation_type
            chromosome chromosome_start chromosome_end
            chromosome_strand refsnp_allele refsnp_strand reference_genome_allele
            control_genotype tumour_genotype mutation expressed_allele quality_score
            probability
            read_count is_annotated validation_status validation_platform
            xref_ensembl_var_id note
            QCMGflag ND TD NNS FlankSeq );

    my @v_dcc1_dbsgm_germline_r11 =
        qw( analysis_id control_sample_id variation_id variation_type
            chromosome chromosome_start chromosome_end
            chromosome_strand refsnp_allele refsnp_strand reference_genome_allele
            control_genotype tumour_genotype expressed_allele quality_score probability
            read_count is_annotated validation_status validation_platform
            xref_ensembl_var_id note
            QCMGflag ND TD NNS FlankSeq Mutation );

    my @v_dcc2_dbssm_somatic_r11 =
        qw( mutation_id
            consequence_type aa_mutation cds_mutation protein_domain_affected
            gene_affected transcript_affected gene_build_version note
            Mutation QCMGflag );

    my @v_dcc2_dbsgm_germline_r11 =
        qw( variation_id
            consequence_type aa_variation cds_variation protein_domain_affected
            gene_affected transcript_affected gene_build_version note
            Variant QCMGflag );

    my @v_dccq_dbssm_somatic_r11 =
        qw( mutation_id mutation_type
            chromosome chromosome_start chromosome_end
            chromosome_strand refsnp_allele refsnp_strand reference_genome_allele
            control_genotype tumour_genotype mutation expressed_allele quality_score
            probability
            read_count is_annotated validation_status validation_platform
            xref_ensembl_var_id note
            ND TD  NNS
            consequence_type aa_mutation cds_mutation protein_domain_affected
            gene_affected transcript_affected gene_build_version note_s
            gene_symbol All_domains All_domains_type All_domains_description
            ChrPosition QCMGflag FlankSeq );

    my @v_dccq_dbsgm_germline_r11 =
        qw( variation_id variation_type
            chromosome chromosome_start chromosome_end
            chromosome_strand refsnp_allele refsnp_strand reference_genome_allele
            control_genotype tumour_genotype expressed_allele quality_score probability
            read_count is_annotated validation_status validation_platform
            xref_ensembl_var_id note
            ND TD NNS
            consequence_type aa_variation cds_variation protein_domain_affected
            gene_affected transcript_affected gene_build_version note_s
            gene_symbol All_domains All_domains_type All_domains_description
            ChrPosition QCMGflag FlankSeq Mutation );

    # Structural variants

    my @v_dcc1_dbstsm_somatic_r11 =
        qw( analysis_id analyzed_sample_id sv_id placement
            annotation interpreted_annotation variant_type
            chr_from chr_from_bkpt chr_from_strand chr_from_range
            chr_from_flanking_seq
            chr_to chr_to_bkpt chr_to_strand chr_to_range chr_to_flanking_seq
            microhomology_sequence non_templated_sequence evidence
            quality_score probablility zygosity
            validation_status validation_platform db_xref note
            QCMGflag Ref Alt Info );

    # Test to see if we can get annotations using this structure
    #my @v_dcc2_dbstsm_somatic_r11 =
    #    qw( mutation_id
    #        consequence_type aa_mutation cds_mutation protein_domain_affected
    #        gene_affected transcript_affected gene_build_version note
    #        Mutation QCMGflag );

    my @v_dcc2_dbstsm_somatic_r11 =
        qw( analysis_id analyzed_sample_id sv_id placement
            bkpt_from_context gene_affected_by_bkpt_from
            transcript_affected_by_bkpt_from
            bkpt_to_context gene_affected_by_bkpt_to
            transcript_affected_by_bkpt_to
            gene_build_version note
            QCMGflag );

    my @v_dcc1_dbstgv_germline_r11 =
        qw( analysis_id control_sample_id sv_id placement
            annotation interpreted_annotation variant_type
            chr_from chr_from_bkpt chr_from_strand chr_from_range
            chr_from_flanking_seq
            chr_to chr_to_bkpt chr_to_strand chr_to_range chr_to_flanking_seq
            microhomology_sequence non_templated_sequence evidence
            quality_score probablility zygosity
            validation_status validation_platform db_xref note
            QCMGflag Ref Alt Info );

    my @v_dcc2_dbstgv_germline_r11 =
        qw( analysis_id control_sample_id sv_id placement
            bkpt_from_context gene_affected_by_bkpt_from
            transcript_affected_by_bkpt_from
            bkpt_to_context gene_affected_by_bkpt_to
            transcript_affected_by_bkpt_to
            gene_build_version note
            QCMGflag );

	# RELEASE 14

    my @v_dcc1_dbssm_somatic_r14 =
        qw( analysis_id analyzed_sample_id mutation_id mutation_type
            chromosome chromosome_start chromosome_end
            chromosome_strand refsnp_allele refsnp_strand reference_genome_allele
            control_genotype tumour_genotype mutation expressed_allele quality_score
            probability
            read_count is_annotated verification_status verification_platform
            xref_ensembl_var_id note
            QCMGflag ND TD NNS FlankSeq );

    my @v_dcc1_dbsgm_germline_r14 =
        qw( analysis_id analyzed_sample_id variant_id variant_type
            chromosome chromosome_start chromosome_end
            chromosome_strand refsnp_allele refsnp_strand reference_genome_allele
            control_genotype tumour_genotype expressed_allele quality_score probability
            read_count is_annotated verification_status verification_platform
            xref_ensembl_var_id note
            QCMGflag ND TD NNS FlankSeq Mutation );

    my @v_dcc2_dbssm_somatic_r14 =
        qw( mutation_id
            consequence_type aa_mutation cds_mutation protein_domain_affected
            gene_affected transcript_affected gene_build_version note
            Mutation QCMGflag );

    my @v_dcc2_dbsgm_germline_r14 =
        qw( variant_id
            consequence_type aa_variant cds_variant protein_domain_affected
            gene_affected transcript_affected gene_build_version note
            Variant QCMGflag );

    my @v_dccq_dbssm_somatic_r14 =
        qw( mutation_id mutation_type
            chromosome chromosome_start chromosome_end
            chromosome_strand refsnp_allele refsnp_strand reference_genome_allele
            control_genotype tumour_genotype mutation expressed_allele quality_score
            probability
            read_count is_annotated verification_status verification_platform
            xref_ensembl_var_id note
            ND TD  NNS
            consequence_type aa_mutation cds_mutation protein_domain_affected
            gene_affected transcript_affected gene_build_version note_s
            gene_symbol All_domains All_domains_type All_domains_description
            ChrPosition QCMGflag FlankSeq );

    my @v_dccq_dbsgm_germline_r14 =
        qw( variant_id variant_type
            chromosome chromosome_start chromosome_end
            chromosome_strand refsnp_allele refsnp_strand reference_genome_allele
            control_genotype tumour_genotype expressed_allele quality_score probability
            read_count is_annotated verification_status verification_platform
            xref_ensembl_var_id note
            ND TD NNS
            consequence_type aa_variant cds_variant protein_domain_affected
            gene_affected transcript_affected gene_build_version note_s
            gene_symbol All_domains All_domains_type All_domains_description
            ChrPosition QCMGflag FlankSeq Mutation );

    # Structural variants

    my @v_dcc1_dbstsm_somatic_r14 =
        qw( analysis_id analyzed_sample_id sv_id placement
            annotation interpreted_annotation variant_type
            chr_from chr_from_bkpt chr_from_strand chr_from_range
            chr_from_flanking_seq
            chr_to chr_to_bkpt chr_to_strand chr_to_range chr_to_flanking_seq
            microhomology_sequence non_templated_sequence evidence
            quality_score probablility zygosity
            verification_status verification_platform note
            QCMGflag Ref Alt Info );

    # Test to see if we can get annotations using this structure
    #my @v_dcc2_dbstsm_somatic_r14 =
    #    qw( mutation_id
    #        consequence_type aa_mutation cds_mutation protein_domain_affected
    #        gene_affected transcript_affected gene_build_version note
    #        Mutation QCMGflag );

    my @v_dcc2_dbstsm_somatic_r14 =
        qw( analysis_id analyzed_sample_id sv_id placement
            bkpt_from_context gene_affected_by_bkpt_from
            transcript_affected_by_bkpt_from
            bkpt_to_context gene_affected_by_bkpt_to
            transcript_affected_by_bkpt_to
            gene_build_version note
            QCMGflag );

    my @v_dcc1_dbstgv_germline_r14 =
        qw( analysis_id analyzed_sample_id sv_id placement
            annotation interpreted_annotation variant_type
            chr_from chr_from_bkpt chr_from_strand chr_from_range
            chr_from_flanking_seq
            chr_to chr_to_bkpt chr_to_strand chr_to_range chr_to_flanking_seq
            microhomology_sequence non_templated_sequence evidence
            quality_score probablility zygosity
            verification_status verification_platform note
            QCMGflag Ref Alt Info );

    my @v_dcc2_dbstgv_germline_r14 =
        qw( analysis_id analyzed_sample_id sv_id placement
            bkpt_from_context gene_affected_by_bkpt_from
            transcript_affected_by_bkpt_from
            bkpt_to_context gene_affected_by_bkpt_to
            transcript_affected_by_bkpt_to
            gene_build_version note
            QCMGflag );


    # This is a synthetic format created from the Bioscope small indel
    # tool GFF by a pipeline originally created by Karin Kassahn and
    # modified by Nic Waddell.  A mouse version can be found at:
    # /panfs/home/nwaddell/devel/QCMGScripts/n.waddell/indels/mm9_mouse_indel_pipe.pbs
    # The format is identical for somatic and germline files.
    # In practical use, we should probably always immediately convert
    # this to a dcc1_dbsgm_germline_r11 or dcc1_dbsgm_somatic_r11r ecord by
    # cloning and filling in the fields with changed names.
    #
    # We should consider converting these to the new DCC1 format or they
    # will always be a pain in our side.

    my @v_dcc1_smallindeltool =
        qw( analysis_id tumour_sample_id mutation_id mutation_type
            chromosome chromosome_start chromosome_end
            chromosome_strand refsnp_allele refsnp_strand reference_genome_allele
            control_genotype tumour_genotype mutation quality_score probability
            read_count is_annotated validation_status validation_platform
            xref_ensembl_var_id note
            QCMGflag normalcount tumourcount );

    ### JVP ###
    #
    #  Why does the Mutation field exist in DCCQ?  It looks identical
    #  to field mutation except that it is formatted as A/B rather than
    #  A>B.  WTF?

    $VALID_HEADERS = {
        dccq                     => \@v_dccq,
        dcc1a                    => \@v_dcc1a,
        dcc1b                    => \@v_dcc1b,
        dcc1_smallindeltool      => \@v_dcc1_smallindeltool,

        dcc1_dbssm_somatic_r11   => \@v_dcc1_dbssm_somatic_r11,
        dcc1_dbsgm_germline_r11  => \@v_dcc1_dbsgm_germline_r11,
        dcc2_dbssm_somatic_r11   => \@v_dcc2_dbssm_somatic_r11,
        dcc2_dbsgm_germline_r11  => \@v_dcc2_dbsgm_germline_r11,
        dccq_dbssm_somatic_r11   => \@v_dccq_dbssm_somatic_r11,
        dccq_dbsgm_germline_r11  => \@v_dccq_dbsgm_germline_r11,
        dcc1_dbstsm_somatic_r11  => \@v_dcc1_dbstsm_somatic_r11,
        dcc1_dbstgv_germline_r11 => \@v_dcc1_dbstgv_germline_r11,
        dcc2_dbstsm_somatic_r11  => \@v_dcc2_dbstsm_somatic_r11,
        dcc2_dbstgv_germline_r11 => \@v_dcc2_dbstgv_germline_r11,

        dcc1_dbssm_somatic_r14   => \@v_dcc1_dbssm_somatic_r14,
        dcc1_dbsgm_germline_r14  => \@v_dcc1_dbsgm_germline_r14,
        dcc2_dbssm_somatic_r14   => \@v_dcc2_dbssm_somatic_r14,
        dcc2_dbsgm_germline_r14  => \@v_dcc2_dbsgm_germline_r14,
        dccq_dbssm_somatic_r14   => \@v_dccq_dbssm_somatic_r14,
        dccq_dbsgm_germline_r14  => \@v_dccq_dbsgm_germline_r14,
        dcc1_dbstsm_somatic_r14  => \@v_dcc1_dbstsm_somatic_r14,
        dcc1_dbstgv_germline_r14 => \@v_dcc1_dbstgv_germline_r14,
        dcc2_dbstsm_somatic_r14  => \@v_dcc2_dbstsm_somatic_r14,
        dcc2_dbstgv_germline_r14 => \@v_dcc2_dbstgv_germline_r14
        };

    # Create AUTOLOAD methods hash from $VALID_HEADERS
    $VALID_AUTOLOAD_METHODS = {};
    foreach my $version (keys %{$VALID_HEADERS}) {
        $VALID_AUTOLOAD_METHODS->{ $version } = {};
        foreach my $method (@{ $VALID_HEADERS->{ $version } }) {
            $VALID_AUTOLOAD_METHODS->{$version}->{$method} = 1;
        }
    }
}

###########################################################################
#                             PUBLIC METHODS                              #
###########################################################################


sub new {
    my $class  = shift;
    my %params = @_;

    confess "no version parameter to new()" unless
        (exists $params{version} and defined $params{version});

    # Based on requested version, we will set the valid headers and
    # autoload methods. Default is @v_1.
    my @valid_headers = ();
    if (exists $VALID_HEADERS->{ $params{version} }) {
        @valid_headers = @{ $VALID_HEADERS->{ $params{version} } };
    }
    else {
        confess 'the version parameter [', $params{version},
                '] passed to DccSnpRecord::new() is unknown';
    }

    my $self = { valid_headers          => $VALID_HEADERS->{ $params{version} },
                 valid_autoload_methods => $VALID_AUTOLOAD_METHODS->{ $params{version} },
                 version                => $params{version},
                 verbose                => $params{verbose} || 0 };
    bless $self, $class;

    # This is for the (rare) use case where the user passes in a
    # array of data to be used to populate the record.  The data must
    # have exactly as many fields as the header count and it's up to the
    # user to make sure the order matches.  This is tres dangerous!

    if ($params{data} and defined $params{data}) {
        my $dcount = scalar @{$params{data}};
        my $hcount = scalar @valid_headers;
        confess "data [$dcount] and header [$hcount] counts do not match" 
            if ($dcount != $hcount and $self->verbose);

        foreach my $ctr (0..($dcount-1)) {
            if (! defined $valid_headers[$ctr]) {
                carp "Empty header value in column [$ctr] - ignoring data"
                    if $self->verbose;
                next;
            }
            $self->{ $valid_headers[$ctr] } = $params{data}->[$ctr];
        }
    }

    return $self;
}


sub AUTOLOAD {
    my $self  = shift;
    my $value = shift || undef;

    my $type       = ref($self) or confess "$self is not an object";
    my $invocation = $AUTOLOAD;
    my $method     = undef;

    if ($invocation =~ m/^.*::([^:]*)$/) {
        $method = $1;
    }
    else {
        confess "QCMG::IO::DccSnpRecord AUTOLOAD problem with [$invocation]";
    }

    # We don't need to report on missing DESTROY methods.
    return undef if ($method eq 'DESTROY');

    if (! exists $self->{valid_autoload_methods}->{$method}) {
        print Dumper $self;
        confess "QCMG::IO::DccSnpRecord can't access method [$method] via AUTOLOAD";
    }

    # If this is a setter call then do the set
    if (defined $value) {
        $self->{$method} = $value;
    }

    # Return current value
    return $self->{$method};
}


sub to_text {
    my $self = shift;
    my @columns = @{ $self->{valid_headers} };
    my @values = map { exists $self->{ $_ } ? $self->{ $_ } : '' } @columns;
    return join("\t",@values);
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub version {
    my $self = shift;
    if (@_) {
        my $new_version = shift;
        # Check that requested version actually exists otherwise die
        if (exists $VALID_HEADERS->{ $new_version }) {
            $self->{valid_headers}           = $VALID_HEADERS->{ $new_version };
            $self->{valid_autoload_methods}  = $VALID_AUTOLOAD_METHODS->{ $new_version };
            $self->{version}                 = $new_version;
        }
        else {
            confess 'the version parameter [', $new_version,
                  '] passed to DccSnpRecord::version() is unknown';
        }
    }
    return $self->{version};
}


sub is_somatic {
    my $self = shift;
    return ($self->version =~ /_somatic_/) ? 1 : 0;
}

sub is_germline {
    my $self = shift;
    return ($self->version =~ /_germline_/) ? 1 : 0;
}



1;

__END__


=head1 NAME

QCMG::IO::DccSnpRecord - ICGC DCC SNP Record data container


=head1 SYNOPSIS

 use QCMG::IO::DccSnpRecord;


=head1 DESCRIPTION

This module provides a data container for a ICGC DCC SNP file record.

You should almost certainly B<not> be using this module directly and
should instead be using DccSnpReader.pm - see explanation in
documentation for B<new()> method.

This module allows for reading a number of different DCC file formats -
primary, secondary, primary annotated, QCMG DCC combined, etc.  You can
use more than one format in the same program BUT you may never be
reading from more than one sort of DCC file at a time.  This is because
package globals are used to monitor the column headers and set the
accessor methods using AUTOLOAD so every call to
new() will reset the headers and accessor.  So if you start reading a DCC1
file and then open a DCC2 file, both files will become DCC2 files and
you will not be able to access any DCC1-specific fields.

Just as you would never cross the streams, never mix DCC file accesses.

B<JVP:>

We need to revise the nomenclature for these files to match the DCC,
i.e. the top level distinction is between:

 1. SSM - simple somatic mutations
 2. SGM - simple germline mutations
 3. CNSM - copy number somatic mutations
 4. CNGM - copy number germline mutations
 5. STSM - structural somatic mutations
 6. STGM - structural germline mutations

And within each of the categories above we find:

 1. Metadata file
 2. Primary analysis file (we call this DCC1)
 3. Secondary analysis file (we call this DCC2)
 4. QCMG-only unified DCC1+DCC2 file (we call this DCCQ)

=head2 Casting between categories

There are times when it might be advantageous to be able to convert a
record of one type to another, for exmaple when creating a DCCQ record
it would be simpler if we could take the existing DCC1 record and add to
it to create the DCCQ record.  Because we are using AUTOLOAD under the
hood to provide getters and setters, this turns out to be quite easy -
you take an existing record and use the B<version()> method to change it
to a new type.

When you use B<version()> to change the version of a record,
any data fields from the starting record that do not have identically
named equivalents in the new version become inaccessible but any fields
that are identically named simply carry over into the new record.  All you
have to do to complete your new record is fill in values for any fields
that did not exist in the old record or were named differently.

Note that if a field changes names between the record version, you will
have to save the old value yourself I<before> changing the record
version and then copy the value into the changed record because the old
data fields will become inaccessible once the version has changed.


=head1 METHODS

=over

=item B<new()>

  $snp = QCMG::IO::DccSnpRecord->new( version => 'dcc1b',
                                      data    => \@fields );

The version parameter determines the headers used to associate the data
fields with column headers.  If you mess this up you are royally screwed
which is why you should never instantiate objects of this class unless
you really know what you are doing.  Instead you should create a
QCMG::IO::DccSnpReader class and tell it the file name and the version
you think the file is.  DccSnpReader will then check file headers
against the expected headers and fail if there is any mismatch.  If all
is well, you call next_record() and you get DccSnpRecord objects back
where the version is already set for you.

In the unlikely event you need to set the version manually, the valid
values for version are documented in QCMG::IO::DccSnpReader.

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
