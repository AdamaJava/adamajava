package QCMG::Annotate::Util;

###########################################################################
#
#  Module:   QCMG::Annotate::Util.pm
#  Creator:  John V Pearson
#  Created:  2012-11-22
#
#  Non-OO utility functions for key annotation operations.
#
#  $Id: Util.pm 4660 2014-07-23 12:18:43Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( confess );
use Clone qw( clone );
use Data::Dumper;
use Exporter qw( import );

use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION @EXPORT_OK %PATTERNS );

( $REVISION ) = '$Revision: 4660 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: Util.pm 4660 2014-07-23 12:18:43Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

@EXPORT_OK = qw( load_ensembl_API_modules initialise_dcc2_file
                 transcript_to_domain_lookup 
                 transcript_to_geneid_and_symbol_lookups
                 transcript_lookups 
                 dcc2rec_from_dcc1rec dccqrec_from_dcc1rec 
                 annotation_defaults read_and_filter_dcc1_records
                 read_and_filter_dcc1_sv_records
                 mutation_snp mutation_indel mutation_pindel );


sub load_ensembl_API_modules {
    my $version = shift;

    # The default location for the Ensembl Perl API used to be Karin
    # Kassahn's home directory but is now in /panfs/share/software
    my $module_dir='/panfs/share/software/EnsemblPerlAPI_v' . $version;

    confess "Ensembl API directory for version $version ($module_dir) does not exist\n"
        unless (-d $module_dir);
    confess "Ensembl API directory for version $version ($module_dir) exists but is not readable\n"
        unless (-r $module_dir);
             
    # Directly manipulate the @INC module search path list
    unshift @INC, "$module_dir/ensembl/modules";
    unshift @INC, "$module_dir/ensembl-variation/modules";
    unshift @INC, "$module_dir/ensembl-functgenomics/modules";
    unshift @INC, "/panfs/share/software/bioperl-1.2.3";

    # Using 'require' rather than 'use' so we can delay execution of
    # these statements until after option processing.
    require Bio::EnsEMBL::Registry;
    require Bio::EnsEMBL::Variation::VariationFeature;
}


sub initialise_dcc2_file {
    my $filename = shift;
    my $version  = shift;
    my $verbose  = shift;
    my $ensver   = shift;
    my $ra_recs  = shift; # dcc1 records
    my $meta     = shift;

    my $dcc2 = QCMG::IO::DccSnpWriter->new(
                   filename => $filename,
                   version  => $version,
                   meta     => $meta,
                   verbose  => $verbose );


    foreach my $rec (@{$ra_recs}) {
#        my $mutation = '';
#        if ($mode eq 'snp') {
#            $mutation = mutation_snp( $vartype,
#                                      $rec->control_genotype,
#                                      $rec->mutation,
#                                      $rec->reference_genome_allele );
#        }
#        elsif ($mode eq 'indel') {
#            $mutation = mutation_indel( $vartype,
#                                        $rec->control_genotype,
#                                        $rec->mutation,
#                                        $rec->reference_genome_allele,
#                                        $rec->mutation_id );
#        }
#        else {
#            confess "mode [$mode] is illegal - must be snp or indel\n";
#        }


        my $rec2 = dcc2rec_from_dcc1rec( $rec, $version );
        $rec2->gene_build_version( $ensver );

        $dcc2->write_record( $rec2 );
    }

    return $dcc2;
}


sub mutation_snp {
    my $type             = shift;
    my $germline_alleles = shift;
    my $mutant_alleles   = shift;
    my $ref              = shift;

    #print Dumper [ $type, $mutant_alleles, $germline_alleles, $ref ];
    my $output = '';

    if ($type eq 'somatic') {
    	my @muts = split(/>/,$mutant_alleles);
    	$output = $ref.'/'.$muts[1];
    }
    elsif ($type eq 'germline') {
    	my @muts = split(/\//,$germline_alleles);
    	if ($muts[0] ne $ref) {
    		$output = $ref.'/'.$muts[0];
    	}
    	else {
    		$output = $ref.'/'.$muts[1];
    	}
    }
    else {
        confess "type [$type] is illegal - should be somatic or germline\n";
    }

    return $output;
}


sub mutation_pindel {
    my $type       = shift;
    my $ref_allele = shift;
    my $alt_allele = shift;

    my $output = '';
 	if ($type eq 'INS') {
        $output = '-/'. $alt_allele;
    }
    elsif ($type eq 'DEL') {
        $output = $ref_allele . '/-';
    }
    elsif ($type eq 'INV') {
    	#
    }
    elsif ($type eq 'DUP:TANDEM') {
    	#
    }
    else {
        confess "type [$type] is unknown";
    }

    return $output;
}


sub mutation_indel {
    my $type             = shift;
    my $germline_alleles = shift;
    my $mutant_alleles   = shift;
    my $ref              = shift;
    my $indel_type       = shift;

    my @muts = ();
 	if ($type eq 'somatic') {
    	@muts = split(/>/,$mutant_alleles);
    }
    elsif ($type eq 'germline') {
    	@muts = split(/\//,$germline_alleles);
    }
    else {
        confess "type [$type] is illegal - should be somatic or germline";
    }

    my $output = '';
    if ($indel_type =~ /insertion/i) {
        $output = '-/'. $muts[1];
    }
    elsif ($indel_type =~ /deletion/i) {
        $output = $ref .'/-';
    }
    else {
        confess "type [$indel_type] is illegal - should include insertion or deletion\n";
    }

    return $output;
}


sub transcript_lookups {
    my $domain_filename = shift;
    my $gene_filename   = shift;
    my $verbose         = shift;

    my $tr2domain = transcript_to_domain_lookup( $domain_filename,
                                                 $verbose );
    my ( $tr2geneid, $tr2symbol ) =
        transcript_to_geneid_and_symbol_lookups( $gene_filename,
                                                 $verbose );

    return $tr2domain, $tr2geneid, $tr2symbol;
}


sub transcript_to_domain_lookup {
    my $filename = shift;
    my $verbose  = shift;

    my %t2domains = ();

    qlogprint( "Creating lookup for transcript to domain\n" );

    # Load ensembl domains records
    my $ensdr = QCMG::IO::EnsemblDomainsReader->new(
                    filename => $filename,
                    verbose  => $verbose );
    while (my $rec = $ensdr->next_record) {
        push @{ $t2domains{ $rec->transcript_id } }, $rec;
    }

    return \%t2domains;
}


sub transcript_to_geneid_and_symbol_lookups {
    my $filename = shift;
    my $verbose  = shift;

    my %t2geneid  = ();
    my %t2symbol  = ();
    my %counts    = ();

    qlogprint( "Creating lookups for transcript to gene_id and gene_symbol:\n" );

    # Load ensembl domains records
    my $ensdr = QCMG::IO::EnsemblTranscriptMapReader->new(
                    filename => $filename,
                    verbose  => $verbose );

    while (my $rec = $ensdr->next_record) {

        # Create hash of transcript_id to gene_id but watch out for any cases
        # where transcript maps to multiple genes (should never happen!)
        if (exists $t2geneid{ $rec->transcript_id }) {
            $counts{geneid_doubles}++;
            confess 'Problem with inconsistent mapping of transcript to gene: ',
                $rec->transcript_id, ' => [', 
                $t2geneid{ $rec->transcript_id }, ',',  $rec->gene_id, "]\n"
                if ($t2geneid{ $rec->transcript_id } ne $rec->gene_id);
        }
        else {
            $counts{geneid_uniques}++;
            $t2geneid{ $rec->transcript_id } = $rec->gene_id;
        }

        # Create hash of transcript_id to symbol but watch out for any cases
        # where transcript maps to multiple symbols (should never happen!)
        if (exists $t2symbol{ $rec->transcript_id }) {
            $counts{symbol_doubles}++;
            confess 'Problem with inconsistent mapping of transcript to symbol: ',
                $rec->transcript_id, ' => [', 
                $t2symbol{ $rec->transcript_id }, ',', $rec->gene_symbol, "]\n"
                if ($t2symbol{ $rec->transcript_id } ne $rec->gene_symbol);
        }
        else {
            $counts{symbol_uniques}++;
            $t2symbol{ $rec->transcript_id } = $rec->gene_symbol
        }
    }

    return \%t2geneid, \%t2symbol;
}


#  In the following 2 routines we create "empty" DCC records cloned from
#  existing records.  We also initialise any new fields that do not
#  carry over values from the cloned record.  Where the field is one
#  that is specified by the DCC or identical to a DCC-specified field
#  then we use DCC codes.  If the field is QCMG-specific then we
#  initialise it to '--'.
#  As of 20121125, '-999' means "data not supplied at this time",
#  and '-888' means "not applicable".


sub dcc2rec_from_dcc1rec {
    my $dcc1rec = shift;
    my $version = shift;

    # Check the input parameters
    confess 'the [', $dcc1rec->version, '] record passed to dcc2rec_from_dcc1rec()',
        " does not appear to be a DCC1 record\n"
        unless ($dcc1rec->version =~ /dcc1/i);
    confess 'the version [', $version, '] passed to dcc2rec_from_dcc1rec()',
        " does not appear to be a DCC2 type\n"
        unless ($version =~ /dcc2/i);

    # Create new record by cloning DCC1 and flipping version
    my $dcc2rec = clone( $dcc1rec );
    $dcc2rec->version( $version );
    $dcc2rec->consequence_type('-888');
    if ($dcc2rec->is_somatic) {
        $dcc2rec->aa_mutation('-888');
        $dcc2rec->cds_mutation('-888');
    }
    elsif ($dcc2rec->is_germline) {
        # Release 14 changed some field names
        if ($version =~ /_r14$/) {
            $dcc2rec->aa_variant('-888');
            $dcc2rec->cds_variant('-888');
        }
        else {
            $dcc2rec->aa_variation('-888');
            $dcc2rec->cds_variation('-888');
        }
    }
    else {
        confess "Couldn't tell if record was somatic or germline\n";
    }
    $dcc2rec->protein_domain_affected('-888');
    $dcc2rec->gene_affected('-888');
    $dcc2rec->transcript_affected('-888');
    $dcc2rec->gene_build_version('-888');
    $dcc2rec->note('-999');

    return $dcc2rec;
}


sub dccqrec_from_dcc1rec {
    my $dcc1rec = shift;
    my $version = shift;

    # Check the input parameters
    confess 'the [', $dcc1rec->version, '] record passed to dccqrec_from_dcc1rec()',
        " does not appear to be a DCC1 record\n"
        unless ($dcc1rec->version =~ /dcc1/i);
    confess 'the version [', $version, '] passed to dccqrec_from_dcc1rec()',
        " does not appear to be a DCCQ type\n"
        unless ($version =~ /dccq/i);

    # Create new record by cloning DCC1 and flipping version
    my $dccqrec = clone( $dcc1rec );
    $dccqrec->version( $version );
    $dccqrec->consequence_type('-888');
    if ($dccqrec->is_somatic) {
        $dccqrec->aa_mutation('-888');
        $dccqrec->cds_mutation('-888');
    }
    elsif ($dccqrec->is_germline) {
        # Release 14 changed some field names
        if ($version =~ /_r14$/) {
            $dccqrec->aa_variant('-888');
            $dccqrec->cds_variant('-888');
        }
        else {
            $dccqrec->aa_variation('-888');
            $dccqrec->cds_variation('-888');
        }
    }
    else {
        confess "Couldn't tell if record was somatic or germline\n";
    }
    $dccqrec->protein_domain_affected('-888');
    $dccqrec->gene_affected('-888');
    $dccqrec->transcript_affected('-888');
    $dccqrec->gene_build_version('-888');
    $dccqrec->note_s('-999');
    $dccqrec->gene_symbol('--');
    $dccqrec->All_domains('--');
    $dccqrec->All_domains_type('--');
    $dccqrec->All_domains_description('--');

    # Check whether chromosome already has chr prepended.
    my $chrom = $dccqrec->chromosome =~ /^chr/ ? $dccqrec->chromosome :
                'chr' . $dccqrec->chromosome;
    $dccqrec->ChrPosition( $chrom .':'.
                           $dccqrec->chromosome_start .'-'.
                           $dccqrec->chromosome_end );

    return $dccqrec;
}


sub annotation_defaults {
    my $mode      = shift;
    my $rh_params = shift;

    confess "Cannot choose default annotations without specifying organism\n"
        unless (exists $rh_params->{organism} and
                defined $rh_params->{organism});
    confess "Cannot choose default annotations without specifying ensembl version\n"
        unless (exists $rh_params->{ensver} and
                defined $rh_params->{ensver});

    my $organism = $rh_params->{organism};
    my $ensver   = $rh_params->{ensver};

    # Default species is Human
    my $species = $organism =~ /mouse/i ? 'Mm' : 'Hs';

    my $path = '/panfs/share/qcmg_created/ensembl_domains/';

    if ($mode =~ /ensembl_domains/i) {
        return $path . join('_', 'Ensembl', "v$ensver", $species,
                                 'domains.txt' );
    }
    elsif ($mode =~ /ensembl_symbols/i) {
        return $path . join('_', 'Ensembl', "v$ensver", $species,
                                 'genes_transcripts_symbols.txt' );
    }
    else {
        confess "mode [$mode] unknown in call to annotation_defaults()\n";
    }
}


sub read_and_filter_dcc1_records {
    my $dcc1 = shift;

    my @keeps    = ();
    my @skips    = ();
    my %problems = ();

    while (my $rec = $dcc1->next_record) {

        # We are going to skip annotating SNPs that are on sequences that we
        # know cannot be annotated, e.g. HSCHRUN_RANDOM_CTG22.  We still
        # write these records to the outfile but we do NOT put them on the
        # list to be passed through the annotation loop

    	if (($rec->chromosome =~ /^H/) or
    	    ($rec->chromosome =~ /^GL/) or
            ($rec->chromosome =~ /n/)) {
            $problems{$rec->chromosome}++;
            push @skips, $rec;
    	}
    	else {
            push @keeps, $rec;
    	}
    }

    # If we skipped any records, write out a report.
    my $skip_total = 0;
    $skip_total += $_ foreach values %problems;
    my @skipped_chrs = sort keys %problems;
    if (scalar @skipped_chrs) {
        qlogprint( "No annotation was attempted on $skip_total SNPs on chromosomes:\n" );
        foreach my $key (@skipped_chrs) {
            qlogprint( "  $key : ", $problems{$key}, "\n" );
        }
    }
    qlogprint( 'Read and filter DCC1 records from ', $dcc1->filename, "\n");
    qlogprint( '  keeping ', scalar(@keeps), " records\n" );
    qlogprint( '  dropping ', scalar(@skips), " records\n" );

    return \@keeps, \@skips;
}


sub read_and_filter_dcc1_sv_records {
    my $dcc1 = shift;

    my @keeps    = ();
    my @skips    = ();
    my %problems = ();

    while (my $rec = $dcc1->next_record) {

        # We are going to skip annotating SNPs that are on sequences that we
        # know cannot be annotated, e.g. HSCHRUN_RANDOM_CTG22.  We still
        # write these records to the outfile but we do NOT put them on the
        # list to be passed through the annotation loop

    	if (($rec->chr_from =~ /^H/) or
    	    ($rec->chr_from =~ /^GL/) or
            ($rec->chr_from =~ /n/)) {
            $problems{$rec->chr_from}++;
            push @skips, $rec;
    	}
    	else {
            push @keeps, $rec;
    	}
    }

    # If we skipped any records, write out a report.
    my $skip_total = 0;
    $skip_total += $_ foreach values %problems;
    my @skipped_chrs = sort keys %problems;
    if (scalar @skipped_chrs) {
        qlogprint( "No annotation was attempted on $skip_total SVs on chromosomes:\n" );
        foreach my $key (@skipped_chrs) {
            qlogprint( "  $key : ", $problems{$key}, "\n" );
        }
    }

    return \@keeps, \@skips;
}


1;


__END__

=head1 NAME

QCMG::Annotate::Util - Annotation utility subroutines


=head1 SYNOPSIS

 use QCMG::Annotate::Util;


=head1 DESCRIPTION

This module provides a collection of utility subroutines that might be
of use in multiple annotation classes.  It also serves to centralise
functionality such as choosing default annotation files.

=head2 Subroutines

=head3 load_ensembl_API_modules

 load_ensembl_API_modules( '70' );

This routine takes a single parameter - an integer representing the
version of the Ensembl API that you wish to use.  This routine directly
manipulates the perl @INC special array that stores the directories to
be searched when loading modules.  It then loads the modules that are
required for Ensembl annotation.

=head3 initialise_dcc2_file

=head3 transcript_to_domain_lookup

=head3 transcript_to_geneid_and_symbol_lookups

=head3 dcc2rec_from_dcc1rec

=head3 dccqrec_from_dcc1rec

=head3 annotation_defaults

=head3 read_and_filter_dcc1_records


=head1 AUTHORS

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: Util.pm 4660 2014-07-23 12:18:43Z j.pearson $


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
