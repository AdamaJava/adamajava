package QCMG::Annotate::Strelka;

###########################################################################
#
#  Module:   QCMG::Annotate::Strelka.pm
#  Creator:  Lynn Fink
#  Created:  2013-06-05
#
#  Module to provide annotation for variants called by strelka.
#
#  $Id: Strelka.pm 4660 2014-07-23 12:18:43Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;
use Pod::Find qw(pod_where);

use QCMG::Annotate::EnsemblConsequences;
use QCMG::Annotate::EnsemblConsequenceRecord;
use QCMG::Annotate::Util qw( load_ensembl_API_modules
                             transcript_to_domain_lookup 
                             transcript_to_geneid_and_symbol_lookups
                             transcript_lookups 
                             annotation_defaults
                             read_and_filter_dcc1_records
                             read_and_filter_dcc1_sv_records
                             mutation_pindel );
use QCMG::IO::DccSnpReader;
use QCMG::IO::DccSnpWriter;
use QCMG::IO::EnsemblDomainsReader;
use QCMG::IO::EnsemblTranscriptMapReader;
use QCMG::IO::INIFile;
use QCMG::IO::VcfReader;
use QCMG::Run::Annovar;
use QCMG::Util::FileManipulator;
use QCMG::Util::QLog;
use QCMG::Util::Util qw( qexec_header timestamp);

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 3746 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: Strelka.pm 4660 2014-07-23 12:18:43Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class    = shift;
    my %defaults = @_;

	$defaults{genome}	= "/panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa";

    # Print usage message if no arguments supplied
    pod2usage( -input    => pod_where({-inc => 1, verbose => 1}, __PACKAGE__),
               -exitval  => 0,
               -verbose  => 99,
               -sections => 'DESCRIPTION/Commandline parameters' )
        unless (scalar @ARGV);

#    pod2usage( -exitval  => 0,
#               -verbose  => 99,
#               -sections => 'DESCRIPTION/Commandline parameters' )
#        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( 
                   logfile  => '',
                   ensver   => $defaults{ensver},
                   organism => $defaults{organism},
                   release  => $defaults{release},
                   verbose  => $defaults{verbose},
		   genome   => $defaults{genome},
		   repeats  => $defaults{repeats},
                   domains  => '',
                   symbols  => '' );

    my $cmdline = join(' ',@ARGV);
    my $results = GetOptions (
           'i|infile=s'         => \$params{infile},            # -i
           's|sampleid=s'       => \$params{sampleid},          # -s
           '1|dcc1file=s'       => \$params{dcc1file},          # -1
           'o|dcc2file=s'       => \$params{dcc2file},          # -o
           'q|dccqfile=s'       => \$params{dccqfile},          # -q
           'l|logfile=s'        => \$params{logfile},           # -l
	   'rm|repeats=s'	=> \$params{repeats},		# -rm
	   'bt|tumourbam=s'	=> \$params{tumourbam},		# -bt
	   'bn|normalbam=s'	=> \$params{normalbam},		# -bn
	   'rg|genome=s'	=> \$params{genome},		# -rg
           'e|ensembl=s'        => \$params{ensver},            # -e
           'g|organism=s'       => \$params{organism},          # -g
           'r|release=s'        => \$params{release},           # -r
           'd|domains=s'        => \$params{domains},           # -d
           'y|symbols=s'        => \$params{symbols},           # -y
           'v|verbose+'         => \$params{verbose},           # -v
           );

    # Pick default Ensembl files if not specified by user
    $params{domains} = annotation_defaults( 'ensembl_domains', \%params )
        unless ($params{domains});
    $params{symbols} = annotation_defaults( 'ensembl_symbols', \%params )
        unless ($params{symbols});

    $params{dcc1_indel_file} = $params{dcc1file};
    $params{dcc2_indel_file} = $params{dcc2file};
    $params{dccq_indel_file} = $params{dccqfile};

	#print Dumper %params;

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $cmdline\n" );
    qlogprint( "Run parameters:\n" );
    foreach my $key (sort keys %params) {
        qlogprint( "  $key : ", $params{$key}, "\n" );
    }

    # Get those pesky ensembl API modules into the search path
    load_ensembl_API_modules( $params{ensver} );

    #my $pin = QCMG::Run::Strelka->new( verbose => $params{verbose} );
	### READ VCF FILE HERE??
    #my $vcfr	= QCMG::IO::VcfReader->new( filename => $params{infile}, verbose => $params{verbose} );
    my $ann	= QCMG::Run::Annovar->new(  verbose  => $params{verbose} );

	my($dcc1_indel_file, $dcc2_indel_file, $dccq_indel_file);

    # Create the object
    my $self = { %params,
                 ann             => $ann
               };
    bless $self, $class;
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub ensver {
    my $self = shift;
    return $self->{ensver};
}

sub organism {
    my $self = shift;
    return $self->{organism};
}

sub ann {
    my $self = shift;
    return $self->{ann};
}

sub sampleid {
    my $self = shift;
    return $self->{sampleid};
}

sub genome {
    my $self = shift;
    return $self->{genome};
}

sub tumourbam {
    my $self = shift;
    return $self->{tumourbam};
}

sub normalbam {
    my $self = shift;
    return $self->{normalbam};
}

sub repeats {
    my $self = shift;
    return $self->{repeats};
}

sub vcfr {
    my $self = shift;
    return $self->{vcfr};
}

sub logfile {
    my $self = shift;
    return $self->{logfile};
}

sub infile {
    my $self = shift;
    return $self->{infile};
}

sub dcc1_indel_file {
    my $self = shift;
    return $self->{dcc1_indel_file};
}

sub dcc2_indel_file {
    my $self = shift;
    return $self->{dcc2_indel_file};
}

sub dccq_indel_file {
    my $self = shift;
    return $self->{dccq_indel_file};
}


sub execute {
    my $self = shift;

    qlogprint("Creating DCC1 file...\n");
    $self->_make_indel_dcc1_records;
    qlogprint("Annotating DCC1 file with overlapping repeats...\n");
    $self->_annotate_repeats;
    qlogprint("Annotating DCC1 file with Ensembl consequences...\n");
    $self->_annotate_indels;
    qlogprint("Annotating DCC1 file with filtered indels...\n");
    $self->_filter_indels;

    qlogend();
}

sub _make_indel_dcc1_records {
    my $self    = shift;

    # Read VCF and convert to DCC1
    my $vcf = QCMG::IO::VcfReader->new(
                  filename => $self->infile,
                  verbose  => $self->verbose );

    my $dcc1_version = 'dcc1_dbssm_somatic_r' . $self->{release};
    $self->{dcc1_version}	= $dcc1_version;
    my $dcc1 = QCMG::IO::DccSnpWriter->new(
                   filename => $self->dcc1_indel_file,
                   version  => $dcc1_version,
                   meta     => qexec_header(),
                   verbose  => $self->verbose );

    my $var_id = 1;  # ctr for use in unique ID assigned to each SV
    while (my $rec = $vcf->next_record) {
        my $info = $rec->info;

        my $dcc1rec = QCMG::IO::DccSnpRecord->new(
                          version  => $dcc1_version,
                          verbose  => $self->verbose );

	# build analysis ID from user's initials, mutation type, and date of run
	$ENV{'QCMG_HOME'} =~ /.+\/(\w{2})/;
	my $initials	= uc($1);
	my $atype	= "indel";
	my $date	= QCMG::Util::Util::timestamp(FORMAT => 'YYMMDD');
	my $aid		= join "_", $initials, $atype, $date;
        
        $dcc1rec->analysis_id($aid);
        $dcc1rec->analyzed_sample_id( $self->sampleid );
        $dcc1rec->mutation_id( $dcc1rec->analyzed_sample_id .'_ind'. $var_id++ );
        
        # mutation_type contains a code set by the ICGC DCC:
        # 1 = single base substitution 
        # 2 = insertion of <= 200 bp 
        # 3 = deletion of <= 200 bp 
        # 4 = multiple base substitution (>= 2bp and <= 200bp) 
	my $mut_type	= -999; #default
	$mut_type	= 2 if(length($rec->{'ref_allele'}) == 1);
	$mut_type	= 3 if(length($rec->{'alt_allele'}) == 1);
	$mut_type	= 4 if(length($rec->{'alt_allele'}) > 1 && length($rec->{'ref_allele'}) > 1);
        $dcc1rec->mutation_type( $mut_type );

        $dcc1rec->chromosome( $rec->chrom );
        $dcc1rec->chromosome_start( $rec->position );
        # !!! For the moment we'll say all variants are on plus strand
        $dcc1rec->chromosome_strand( 1 );
	# calculate end coordinate of feature
	my $end		= $rec->position + length($rec->ref_allele);
        $dcc1rec->chromosome_end( $end );

        $dcc1rec->refsnp_allele( '-999' );
        $dcc1rec->refsnp_strand( '-999' );
        $dcc1rec->reference_genome_allele( $rec->ref_allele );
        $dcc1rec->control_genotype( '-999' );
        $dcc1rec->tumour_genotype( $rec->alt_allele );
        $dcc1rec->mutation( $rec->ref_allele .'>'. $rec->alt_allele );
        $dcc1rec->quality_score( '-999' );
        $dcc1rec->probability( '-999' );
        $dcc1rec->read_count( '-999' );
        $dcc1rec->is_annotated( '-999' );
        $dcc1rec->validation_status( '-999' );
        $dcc1rec->validation_platform( '-999' );
        $dcc1rec->xref_ensembl_var_id( '-999' );
        $dcc1rec->note( '-999' );
        
        $dcc1rec->QCMGflag( 'PASS' );
        $dcc1rec->ND( '--' );
        $dcc1rec->TD( '--' );
        $dcc1rec->NNS( '--' );
        $dcc1rec->FlankSeq( '--' );

        $dcc1->write_record( $dcc1rec );
    }

    $vcf->close;
    $dcc1->close;
}

sub _annotate_indels {
    my $self = shift;

    # Because there are so many types of DCC1 file, we are going to let
    # the DccSnpReader guess the version rather than trying to specify it
    
    my $dcc1 = QCMG::IO::DccSnpReader->new(
                   filename => $self->dcc1_indel_file,
                   verbose  => $self->verbose );
    
    # Die if the file is not a DCC1 variant or if we can't determine 
    # somatic/germline from file type

    die "input file must be a DCC1 variant but detected file type is [",
        $dcc1->version, "]\n" unless ($dcc1->dcc_type eq 'dcc1');
    die "cannot parse type (somatic/germline) from DCC1 file type [",
        $dcc1->version, "]\n" unless (defined $dcc1->variant_type);

    # Read in the variant positions from the DCC1
    my ($ra_targets, $ra_skips) = read_and_filter_dcc1_records( $dcc1 );

    my @ens_recs = ();
    foreach my $rec (@{ $ra_targets }) {

        my $mutation = '';
        if ($rec->mutation_type == 2) {
            $mutation = '-/'. $rec->tumour_genotype;
        }
        elsif ($rec->mutation_type == 3) {
            $mutation = $rec->reference_genome_allele . '/-';
        }

#print Dumper $rec->chromosome."\n";
#print Dumper $rec->chromosome_start."\n";
#print Dumper $rec->chromosome_end."\n";
#print Dumper $mutation."\n";
#print Dumper $rec->mutation_id."\n";
#print Dumper $rec."\n";

        # Create an annotatable record for the DCC1 record
        push @ens_recs,
             QCMG::Annotate::EnsemblConsequenceRecord->new(
                 chrom       => $rec->chromosome,
                 chrom_start => $rec->chromosome_start,
                 chrom_end   => $rec->chromosome_end,
                 mutation    => $mutation,
                 var_name    => $rec->mutation_id,
                 dcc1        => $rec );
    }
    # Load Ensembl lookups for domains, geneids and symbols
    my ( $tr2domain, $tr2geneid, $tr2symbol ) =
        transcript_lookups( $self->{domains}, $self->{symbols}, $self->verbose );

    # Lock-n-load an annotator ...
    my $ens = QCMG::Annotate::EnsemblConsequences->new(
                  ensver   => $self->ensver,
                  organism => $self->organism,
                  verbose  => $self->verbose );

    # Annotate !!!
    $ens->get_consequences( $tr2domain,
                            $tr2geneid,
                            $tr2symbol,
                            \@ens_recs );

    # Open DCC2 that is of the same "flavour" as the source DCC1 file and
    # copy out "skipped" records, skipping any non-PFAM domains

    my $dcc2_version = $dcc1->version;
    $dcc2_version =~ s/dcc1/dcc2/;

    my $dcc2 = QCMG::IO::DccSnpWriter->new(
                   filename => $self->dcc2_indel_file,
                   version  => $dcc2_version,
                   meta     => qexec_header(),
                   verbose  => $self->verbose );
    
    foreach my $rec (@ens_recs) {
        my $ra_dcc2recs = $rec->as_dcc2_records;

        #qlogprint( $rec->dcc1->mutation_id,  
        #           ' ('. $rec->dcc1->mutation_type .') has ',
        #           scalar @{ $ra_dcc2recs }, " consequence records\n" );

        # There is a dangerous and subtle bug introduced here because
        # the DCC currently only accepts PFAM domain annotations.  If
        # the variant was smack in the middle of an exon and affected
        # multiple transcripts BUT all of the annotated domains were
        # non-PFAM, then ditching the PFAM domains would effectvely
        # drop all annotation for the variant.
        #
        # What we have to do instead is, if a record contains a
        # non-PFAM domain then we want to keep the record because we
        # want to know the affected transcript BUT we will need to zero
        # out the protein_domain_affected field.  We need to do this
        # check on every record (PFAM records match /^PF\d+$/).

        foreach my $dcc2rec (@{ $ra_dcc2recs }) {
            if ($dcc2rec->protein_domain_affected ne '-888' and
                $dcc2rec->protein_domain_affected !~ /^PF\d+$/) {
                warn "zeroing non-PFAM domain [",
                     $dcc2rec->protein_domain_affected,
                     '] for ',
                     $dcc2rec->mutation_id, ' ',
                     $dcc2rec->transcript_affected, "\n"
                    if ($self->verbose > 1);
                $dcc2rec->protein_domain_affected( '-888' );
            }
            $dcc2->write_record( $dcc2rec );
        }
    }


    # Create corresponding DCCQ file type from DCC1 file type
    my $dccq_version = $dcc2_version;
    $dccq_version =~ s/dcc2/dccq/;

    my $dccq = QCMG::IO::DccSnpWriter->new(
                   filename => $self->dccq_indel_file,
                   version  => $dccq_version,
                   meta     => qexec_header(),
                   verbose  => $self->verbose );

    foreach my $rec (@ens_recs) {
        my $dccqrec = $rec->as_dccq_record;
        $dccq->write_record( $dccqrec );
    }

	qlogprint("Ensembl annotation finished.\n");

#    my @annot_chrs = sort keys %counts;
#    qlogprint( "Annotated SNPs on chromosomes:\n" );
#    foreach my $key (@annot_chrs) {
#        qlogprint( "  $key : ", $counts{$key}, "\n" );
#    }
#    my @conseq_chrs = sort keys %consequences;
#    qlogprint( "Found consequences on chromosomes:\n" );
#    foreach my $key (@conseq_chrs) {
#        qlogprint( "  $key : ", $consequences{$key}, "\n" );
#    }

}


# determine if mutation resides in known repeat regions
sub _annotate_repeats {
    my $self = shift;

	# if no repeatmasked genome file is provided, don't do anything
	if(! defined $self->repeats) {
		qlogprint("Skipping repeat annotation, no file provided\n");
		return;
	}

    # if repeatmasked genome file is provided, edit the DCC1 file and append
    # repeat information to the QCMGflag field
	my $origfile	= $self->dcc1_indel_file.".unannotated";
	my $outfile	= $self->dcc1_indel_file.".repeatmasked";

	qlogprint("Annotating repeats with ".$self->repeats."\n");

	my $qag	= QCMG::Annotate::Gff->new(	infile	=> $self->dcc1_indel_file,
						dcc1out	=> $outfile,
						gfffile	=> $self->repeats
					);
	$qag->execute;

	# move files around; replace unannotated dcc1 file with repeatmasked
	# file and save original file 
	my $cmd		= qq{cp }.$self->dcc1_indel_file.qq{ $origfile};
	qlogprint("$cmd\n");
	my $rv		= system($cmd);
	$cmd		= qq{cp $outfile }.$self->dcc1_indel_file if($rv == 0);
	qlogprint("$cmd\n");
	system($cmd);

	return;
}

# filter indels and add info to ND/TD fields
sub _filter_indels {
    my $self = shift;

	my $outfile	= $self->dcc1_indel_file.".filtered";

	my $cmd		= $ENV{'QCMG_SVN'}.qq{/QCMGScripts/l.fink/indel_pileup/indel_pileup_dcc1.pl };
	$cmd		.= qq{ -it }.$self->tumourbam;
	$cmd		.= qq{ -in }.$self->normalbam;
	$cmd		.= qq{ -g  }.$self->genome;
	$cmd		.= qq{ -is }.$self->dcc1_indel_file;
	$cmd		.= qq{ -os }.$outfile;
	$cmd		.= qq{ -m strelka};
	$cmd		.= qq{ -dcc1v }.$self->{dcc1_version};
	qlogprint("$cmd\n");
	my $rv		= system($cmd);

	$cmd		= qq{cp $outfile }.$self->dcc1_indel_file if($rv == 0);
	qlogprint("$cmd\n");
	print STDERR "$cmd\n";
	#print STDERR "DCC VERSION: ".$self->{dcc1_version}."\n";
	system($cmd);

	return;
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
    my $self    = shift;
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

    $dcc2rec->bkpt_from_context('-999');
    $dcc2rec->gene_affected_by_bkpt_from('-999');
    $dcc2rec->transcript_affected_by_bkpt_from('-999');
    $dcc2rec->bkpt_to_context('-999');
    $dcc2rec->gene_affected_by_bkpt_to('-999');
    $dcc2rec->transcript_affected_by_bkpt_to('-999');
    $dcc2rec->gene_build_version('-999');
    $dcc2rec->note('-999');

    return $dcc2rec;
}


sub dccqrec_from_dcc1rec {
    my $self    = shift;
    my $dcc1rec = shift;
    my $version = shift;

    # 2013-01-12  This routine has not yet been subjected to the
    # required "SV-ification" so we will die if called.
    confess "This routine is not implemented yet";

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
        $dccqrec->aa_variation('-888');
        $dccqrec->cds_variation('-888');
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

1;

__END__

=head1 NAME

QCMG::Annotate::Strelka - Annotate strelka output files


=head1 SYNOPSIS

 use QCMG::Annotate::Strelka;

my %defaults = ( ensver   => '70',
                 organism => 'human',
                 release  => '11',
                 verbose  => 0 );
 my $qsnp = QCMG::Annotate::Strelka->new( %defaults );
 $qsnp->execute;


=head1 DESCRIPTION

This module provides functionality for annotating strelka output.  It
includes commandline parameter processing.

This module is based on a series of SNP annotation scripts originally
created by Karin Kassahn and JP and elaborated by Karin to adapt to new
organisms (Mouse etc) and new versions for the EnsEMBL API.  This
version has been rewritten by JP for better diagnostics and to support
the annotation of SNPs from different organisms and EnsEMBL versions
within a single executable.

=head2 Commandline parameters

 -i | --infile        Input file in VCF format
 -1 | --dcc1out       Output file in DCC1 format
 -o | --dcc2out       Output file in DCC2 format
 -q | --dccqout       Output file in DCCQ format
 -l | --logfile       log file (optional)
 -s | --sampleid      Sample ID
 -rm| --repeats       RepeatMasked genome file (optional)
 -g | --organism      Organism; default=human
 -e | --ensver        EnsEMBL version; default=70
 -r | --release       DCC release; default=11
 -d | --domains       Ensembl domains file
 -y | --symbols       Ensembl gene ids and symbols file
 -v | --verbose       print progress and diagnostic messages
      --version       print version number and exit immediately
 -h | -? | --help     print help message


=over

=item B<-i | --infile>

Mandatory. The input file is in VCF format (see QCMG wiki)

=item B<-1 | --dcc1out>

Mandatory. The DCC1 format output file (see QCMG wiki).

=item B<-o | --dcc2out>

Mandatory. The DCC2 format output file (see QCMG wiki).

=item B<-q | --dccqout>

Mandatory. The DCCQ format output file (see QCMG wiki).

=item B<-l | --logfile>

Optional log file name.  If this option is not specified then logging
goes to STDOUT.

=item B<-rm | --repeats>

Optional RepeatMasked genome file name.  If this option is not specified then
the DCC1 file is not annotated (in the QCMGflag field) for mutations that fall
in/over repeat regions. For now, we will just consider simple repeats
(homopolymeric regions).

=item B<-g | --organism>

This must be one of 'human' or 'mouse'.  The default value is 'human'.

=item B<-e | --ensver>

The version of the (1) EnsEMBL annotation files, (2) local EnsEMBL
database, and (3) perl API.  All three of these items B<must> be from the
same version or weird shit will happen.  You have been warned.  The
default value is '70'.

=item B<-r | --release>

DCC release.  This paramter is important because unfortunately, the
column names and contents of the various DCC files changes from release
to release.  If you dont set this appropriately, it is likely that the
script will exit while attetping to load the DCC1 file due to mismatches
between the found and expected columns.  The default value is '11'.

=item B<-d | --domains>

Ensembl domains file.  If none is specified then the --organism and 
--ensver values will be used to pick a default file.  If a
default cannot be chosen then the module will die.

=item B<-d | --symbols>

Ensembl gene symbols file.  If none is specified then the --organism and 
--ensver values will be used to pick a default file.  If a
default cannot be chosen then the module will die.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back


=head1 AUTHORS

=over 2

=item Lynn Fink, L<mailto:l.fink@imb.uq.edu.au>

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=item Karin Kassahn, L<mailto:k.kassahn@uq.edu.au>

=back


=head1 VERSION

$Id: Strelka.pm 4660 2014-07-23 12:18:43Z j.pearson $


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
