package QCMG::Annotate::GATKindel;

###########################################################################
#
#  Module:   QCMG::Annotate::GATKindel.pm
#  Creator:  Lynn Fink
#  Created:  2013-06-05
#
#  Module to provide annotation for indels called by GATK.
#
#  $Id: GATKindel.pm 3923 2013-06-14 03:54:46Z l.fink $
#
###########################################################################

use strict;
#use warnings;

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
( $SVNID ) = '$Id: GATKindel.pm 3923 2013-06-14 03:54:46Z l.fink $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class    = shift;
    my %defaults = @_;

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
    my %params = ( config   => '',
                   logfile  => '',
                   ensver   => $defaults{ensver},
                   organism => $defaults{organism},
                   release  => $defaults{release},
                   repeats  => $defaults{repeats},
                   verbose  => $defaults{verbose},
                   domains  => '',
                   symbols  => '' );

    my $cmdline = join(' ',@ARGV);
    my $results = GetOptions (
           'c|config=s'         => \$params{config},            # -c
           'l|logfile=s'        => \$params{logfile},           # -l
           'e|ensembl=s'        => \$params{ensver},            # -e

	   # for repeatmasker annotation and indel filtering, default supplied
	   # by qannotate.pl -> human simple repeats and satellites
	   'rm|repeats=s'	=> \$params{repeats},		# -rm

           'g|organism=s'       => \$params{organism},          # -g
           'r|release=s'        => \$params{release},           # -r
           'd|domains=s'        => \$params{domains},           # -d
           'y|symbols=s'        => \$params{symbols},           # -y
           'v|verbose+'         => \$params{verbose},           # -v
           );

    # It is mandatory to supply input params that cannot be defaulted
    die "--config, is required\n" unless $params{config};

    # Pick default Ensembl files if not specified by user
    $params{domains} = annotation_defaults( 'ensembl_domains', \%params )
        unless ($params{domains});
    $params{symbols} = annotation_defaults( 'ensembl_symbols', \%params )
        unless ($params{symbols});

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $cmdline\n" );
    qlogprint( "Run parameters:\n" );
    foreach my $key (sort keys %params) {
        qlogprint( "  $key : ", $params{$key}, "\n" );
    }

    # Load the config file
    my $ini = QCMG::IO::INIFile->new(
                    file    => $params{config},
                    verbose => $params{verbose} );

    die "The INI file must contain a [Filter] section" 
        unless $ini->section('filter');

    # BAMs are needed for filtering indels via pileups
    $params{tumourbam}	= $ini->param('tumour', 'bam');
    $params{normalbam}	= $ini->param('normal', 'bam');
    $params{genome}	= $ini->param('gatkindel', 'reference');

    # input VCF file names are derived from [QGATKindel]->OUTPUT 
    my $vcf_normal_file = $ini->param('qgatkindel','indir') .'/'.  $ini->param('qgatkindel','infile_normal');
    my $vcf_tumour_file = $ini->param('qgatkindel','indir') .'/'.  $ini->param('qgatkindel','infile_tumour');

    # DCC file names are derived from [QGATKindel]->OUTPUT 
    # somatic:
    my $dcc1_indel_file = $ini->param('qgatkindel','outdir') .'/'. $ini->param('qgatkindel','output') .'_indel.dcc1';
    my $dcc2_indel_file = $ini->param('qgatkindel','outdir') .'/'. $ini->param('qgatkindel','output') .'_indel.dcc2';
    my $dccq_indel_file = $ini->param('qgatkindel','outdir') .'/'. $ini->param('qgatkindel','output') .'_indel.dccq';
    # germline:
    my $dcc1_germline_indel_file = $ini->param('qgatkindel','outdir') .'/'.  $ini->param('qgatkindel','output') .'_indel_germline.dcc1';
    my $dcc2_germline_indel_file = $ini->param('qgatkindel','outdir') .'/'.  $ini->param('qgatkindel','output') .'_indel_germline.dcc2';
    my $dccq_germline_indel_file = $ini->param('qgatkindel','outdir') .'/'.  $ini->param('qgatkindel','output') .'_indel_germline.dccq';

    my @filters = map { $ini->param( 'filter', $_ ) }
                  sort keys %{ $ini->section( 'filter' ) };

    if ($params{verbose}) {
        qlogprint( "Parsed contents of INI file ",$params{config},":\n" );
        qlogprint( "  $_\n" ) foreach split( /\n/, $ini->to_text );
    }

	#print Dumper %params;

    # Get those pesky ensembl API modules into the search path
    load_ensembl_API_modules( $params{ensver} );

    my $pin = QCMG::Run::Pindel->new( verbose => $params{verbose} );
    my $ann = QCMG::Run::Annovar->new( verbose => $params{verbose} );

    # Create the object
    my $self = { %params,
                 ini             	  => $ini,
                 ann             	  => $ann,
		 vcf_normal_file	  => $vcf_normal_file,
		 vcf_tumour_file  	  => $vcf_tumour_file,
                 dcc1_indel_file	  => $dcc1_indel_file,
                 dcc2_indel_file	  => $dcc2_indel_file,
                 dccq_indel_file	  => $dccq_indel_file,
                 dcc1_germline_indel_file => $dcc1_germline_indel_file,
                 dcc2_germline_indel_file => $dcc2_germline_indel_file,
                 dccq_germline_indel_file => $dccq_germline_indel_file
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

sub ini {
    my $self = shift;
    return $self->{ini};
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

sub vcf_normal_file {
    my $self = shift;
    return $self->{vcf_normal_file};
}

sub vcf_tumour_file {
    my $self = shift;
    return $self->{vcf_tumour_file};
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

sub dcc1_germline_indel_file {
    my $self = shift;
    return $self->{dcc1_germline_indel_file};
}

sub dcc2_germline_indel_file {
    my $self = shift;
    return $self->{dcc2_germline_indel_file};
}

sub dccq_germline_indel_file {
    my $self = shift;
    return $self->{dccq_germline_indel_file};
}


sub execute {
    my $self = shift;

    #qlogprint("Read normal and tumour VCF files and split indels into somatic/germline DCC1...\n");
    $self->_combine_vcf;

    # fix DCC1 headers to contain same UUID for both somatic and germline
    qlogprint("Fixing somatic and germline DCC1 headers to contain uuids, etc...\n");
    $self->_fix_dcc1_headers;

    # handle GERMLINE indels
    qlogprint("Annotating germline DCC1 file with overlapping repeats...\n");
    $self->_annotate_germline_repeats;

    qlogprint("Annotating germline DCC1 file with filtered indels...\n");
    $self->_filter_germline_indels;

    # handle SOMATIC indels
    qlogprint("Annotating DCC1 file with overlapping repeats...\n");
    $self->_annotate_repeats;

    qlogprint("Annotating DCC1 file with filtered indels...\n");
    $self->_filter_indels;

    # recategorize indels based on filtering results
    qlogprint("Transferring obvious germline indels from somatic DCC1 to germline DCC1...\n");
    $self->_revise_indel_assignment;

    qlogprint("Removing low-confidence germline calls...\n");
    $self->_remove_lowconf_germline_indel;

    qlogprint("Annotating DCC1 file with Ensembl consequences...\n");
    $self->_annotate_indels;

    # annotate revised files with Ensembl
    qlogprint("Annotating germline DCC1 file with Ensembl consequences...\n");
    $self->_annotate_germline_indels;

    # create MAFs
    qlogprint("Generating somatic MAF...\n");
    $self->_create_somatic_maf;
    qlogprint("Generating germline MAF...\n");
    $self->_create_germline_maf;

    qlogend();
}

sub _combine_vcf {
	my $self    = shift;

	# We use the ini file params so often, it's simpler to do this
	my $ini = $self->ini;
	my $lowconfgermdcc	= $self->dcc1_germline_indel_file.".longhrun";

	qlogprint("\tReading                        normal    VCF: ".$self->vcf_normal_file."\n");
	qlogprint("\tReading                        tumour    VCF: ".$self->vcf_tumour_file."\n");
	qlogprint("\tWriting                        somatic  DCC1: ".$self->dcc1_indel_file."\n");
	qlogprint("\tWriting                        germline DCC1: ".$self->dcc1_germline_indel_file."\n");
	qlogprint("\tWriting homopolymer-associated germline DCC1: ".$lowconfgermdcc."\n");

	# Read VCF and convert to DCC1
	# remove all unnecessary records before more parsing
	#print STDERR ("\tReading tumour    VCF: ".$self->vcf_tumour_file."\n");
	my $tvcfrcount	= 0;
	my @t_vcf_recs	= ();
	my $t_vcf = QCMG::IO::VcfReader->new(
	              filename => $self->vcf_tumour_file,
	              verbose  => $self->verbose );
	while (my $t_rec = $t_vcf->next_record) {
		$tvcfrcount++;
		# skip SNPs
		next if(length($t_rec->ref_allele) == 1 && length($t_rec->alt_allele) == 1);
		# skip Ns and tri-allelic SNPs
		next if($t_rec->alt_allele =~ /,/ || $t_rec->alt_allele =~ /N/i);
		# skip lines with dbSNP ID
		next if($t_rec->id ne '.');

		push @t_vcf_recs, $t_rec;
	}
	$t_vcf->close;
	#print STDERR "Tumour VCF record count: $tvcfrcount\n";
	#print STDERR "Tumour VCF record count, good indels only: ".scalar(@t_vcf_recs)."\n";

	print STDERR ("\tReading normal    VCF: ".$self->vcf_normal_file."\n");
	my $nvcfrcount		= 0;
	my @all_n_vcf_recs	= ();
	my %n_vcf_rec_bychr	= ();
	my $n_vcf = QCMG::IO::VcfReader->new( filename => $self->vcf_normal_file);
	while (my $n_rec = $n_vcf->next_record) {
		$nvcfrcount++;
		# skip SNPs
		next if(length($n_rec->ref_allele) == 1 && length($n_rec->alt_allele) == 1);
		# skip Ns and tri-allelic SNPs
		next if($n_rec->alt_allele =~ /,/ || $n_rec->alt_allele =~ /N/i);
		# skip lines with dbSNP ID
		next if($n_rec->id ne '.');

		my $chr	= $n_rec->chrom;

		push @all_n_vcf_recs, 		$n_rec;
		push @{$n_vcf_rec_bychr{$chr}}, $n_rec;
	}
	$n_vcf->close;
	#print STDERR "Normal VCF record count: $nvcfrcount\n";
	#print STDERR "Normal VCF record count, good indels only: ".scalar(@all_n_vcf_recs)."\n";

	my $dcc1_version = 'dcc1_dbssm_somatic_r'.$self->{release};
	my $dcc1 = QCMG::IO::DccSnpWriter->new(
	               	filename => $self->dcc1_indel_file,
	               	version  => $dcc1_version,
	               	meta     => qexec_header(),
	               	verbose  => $self->verbose );
	
	
	my $var_id = 1;  # ctr for use in unique ID assigned to each SV
	my $normal_indels	= 0;
	my $tumour_indels	= 0;
	my $matching_indels	= 0;
	my $somatic_indels	= 0;
	my $germline_indels	= 0;
	my $lcg_germline_indels	= 0;

	# build analysis ID from user's initials, mutation type, and date of run
	$ENV{'QCMG_HOME'} =~ /.+\/(\w{2})/;
	my $initials	= uc($1);
	my $atype	= "indel";
	my $date	= QCMG::Util::Util::timestamp(FORMAT => 'YYMMDD');
	my $aid		= join "_", $initials, $atype, $date;

	print STDERR "Writing somatic DCC1 file\n";

	my $reccount	= 0;
	my $n_idx	= 0;
	for my $idx (0..$#t_vcf_recs) {
		my $t_rec		= $t_vcf_recs[$idx];
		my $seen_in_normal	= 'n';
	
	        my $dcc1rec = QCMG::IO::DccSnpRecord->new(
	                          version  => $dcc1_version,
	                          verbose  => $self->verbose );
	
		$tumour_indels++;

		my $tchr	= $t_rec->chrom;

		if(! defined $tchr) {
			print "WARNING: undefined chr at index $idx: $tchr\n";
			next;
		}
	
		# Read normal VCF
		#for my $idx (0..$#n_vcf_recs) {
		#print STDERR "Checking only normal records on chr $tchr\n";
		#print STDERR "Num normal recs on $tchr: ".scalar(@{$n_vcf_rec_bychr{$tchr}})."\n";
		#print STDERR "Getting tumour chr $tchr in normal file\n";
		unless(! defined  @{$n_vcf_rec_bychr{$tchr}} ) {
			my @n_vcf_recs	= @{$n_vcf_rec_bychr{$tchr}};
			#print STDERR "Starting with normal index $n_idx\n";
			for my $idx ($n_idx..$#n_vcf_recs) {
				my $n_rec	= $n_vcf_recs[$idx];
				#print STDERR "Normal record index; $idx\n";
	
				# don't bother comparing if records are on different chromsomes
				#next unless($t_rec->chrom eq $n_rec->chrom);
				#print STDERR $t_rec->chrom." <=> ".$n_rec->chrom."\n";
		
				# if normal and tumour have the same indel, call it germline
				# if only normal has the indel, call it germline
				if($n_rec->position eq $t_rec->position && $n_rec->alt_allele eq $t_rec->alt_allele) {
					# germline
					#print STDERR $n_rec->position." eq ".$t_rec->position." && ".$n_rec->alt_allele." eq ".$t_rec->alt_allele."\n";
					#print STDERR "Found germline, skipping\n\n";
					$seen_in_normal	= 'y';
					$matching_indels++;
	
					last;
				}
			}
		}
	
		next if($seen_in_normal eq 'y');
	
		#print STDERR "Seen in normal? $seen_in_normal\n";
		if($seen_in_normal eq 'n') {
			#print STDERR "Writing somatic DCC1 record on $tchr:".$tumour_indels."\n";
			# WRITE NEW SOMATIC RECORD
			# these fields will be corrected to the actual sample ID
			# during the DCC1 reheadering step
		        $dcc1rec->analysis_id($aid);
		        $dcc1rec->analyzed_sample_id( 'somatic' );
		        $dcc1rec->mutation_id( $dcc1rec->analyzed_sample_id .'_ind'. $var_id++ );
		        
		        # mutation_type contains a code set by the ICGC DCC:
		        # 1 = single base substitution 
		        # 2 = insertion of <= 200 bp 
		        # 3 = deletion of <= 200 bp 
		        # 4 = multiple base substitution (>= 2bp and <= 200bp) 
			my $mut_type	= -999; #default
			# skip SNPs
			next if(length($t_rec->{'ref_allele'}) == 1 && length($t_rec->{'alt_allele'}) == 1);
			next if($t_rec->alt_allele =~ /^\w{1},/ || $t_rec->alt_allele =~ /N/i);
			# keep indels
			$mut_type	= 2 if(length($t_rec->{'ref_allele'}) == 1);
			$mut_type	= 3 if(length($t_rec->{'alt_allele'}) == 1);
			$mut_type	= 4 if(length($t_rec->{'alt_allele'}) > 1 && length($t_rec->{'ref_allele'}) > 1);
		        $dcc1rec->mutation_type( $mut_type );
		
		        $dcc1rec->chromosome( $t_rec->chrom );
		        $dcc1rec->chromosome_start( $t_rec->position );
		        # !!! For the moment we'll say all variants are on plus strand
		        $dcc1rec->chromosome_strand( 1 );
			# calculate end coordinate of feature
			my $end		= $t_rec->position + length($t_rec->ref_allele);
		        $dcc1rec->chromosome_end( $end );
		
		        $dcc1rec->refsnp_allele( '-999' );
		        $dcc1rec->refsnp_strand( '-999' );
		        $dcc1rec->reference_genome_allele( $t_rec->ref_allele );
		        $dcc1rec->control_genotype( '-999' );
		        $dcc1rec->tumour_genotype( $t_rec->alt_allele );
		        $dcc1rec->mutation( $t_rec->ref_allele .'>'. $t_rec->alt_allele );
		        $dcc1rec->quality_score( '-999' );
		        $dcc1rec->probability( '-999' );
		        $dcc1rec->read_count( '-999' );
		        $dcc1rec->is_annotated( '-999' );
		        $dcc1rec->verification_status( '-999' );
		        $dcc1rec->verification_platform( '-999' );
		        $dcc1rec->xref_ensembl_var_id( '-999' );
	       		#$dcc1rec->uri( '-999' );
		        #$dcc1rec->db_xref( '-999' );
		        $dcc1rec->note( '-999' );
		        
		        $dcc1rec->QCMGflag( 'PASS' );
		        $dcc1rec->ND( '--' );
		        $dcc1rec->TD( '--' );
		        $dcc1rec->NNS( '--' );
		        $dcc1rec->FlankSeq( '--' );

			$dcc1rec	= $self->_unify_coordinates($dcc1rec, 'somatic');

			# transform coordinates, alleles, and mutation to comply
			# with DCC format
		        $dcc1->write_record( $dcc1rec );
	
			$somatic_indels++;
		}
	}
	$t_vcf->close;
	$dcc1->close;

	print STDERR "Writing germline DCC1 files\n";

 
	$dcc1_version = 'dcc1_dbsgm_germline_r' . $self->{release};
	$dcc1 = QCMG::IO::DccSnpWriter->new(
	               filename => $self->dcc1_germline_indel_file,
	               version  => $dcc1_version,
	               meta     => qexec_header(),
		       annotations => { Organism => $self->{organism}, "QAnnotateMode" => "gatkindel"},
	               verbose  => $self->verbose );

	$dcc1_version = 'dcc1_dbsgm_germline_r' . $self->{release};
	my $dcc1lcg = QCMG::IO::DccSnpWriter->new(
	               filename => $lowconfgermdcc,
	               version  => $dcc1_version,
	               meta     => qexec_header(),
		       annotations => { Organism => $self->{organism}, "QAnnotateMode" => "gatkindel"},
	               verbose  => $self->verbose );

	
	# Read normal VCF
	#my $n_vcf = QCMG::IO::VcfReader->new( filename => $self->vcf_normal_file);
	#while (my $n_rec = $n_vcf->next_record) {
	foreach my $n_rec (@all_n_vcf_recs) {
		# skip SNPs
		next if(length($n_rec->ref_allele) == 1 && length($n_rec->alt_allele) == 1);
		next if($n_rec->alt_allele =~ /^\w{1},/ || $n_rec->alt_allele =~ /N/i);
	
		$normal_indels++;
	
	        my $dcc1rec = QCMG::IO::DccSnpRecord->new(
	                          version  => $dcc1_version,
	                          verbose  => $self->verbose );
	
		# WRITE NEW GERMLINE RECORD
		# these will get renamed later
	        $dcc1rec->analysis_id($aid);
	        $dcc1rec->analyzed_sample_id( 'germline' );
	        $dcc1rec->variant_id( "germline" .'_ind'. $var_id++ );
	        
	        # mutation_type contains a code set by the ICGC DCC:
	        # 1 = single base substitution 
	        # 2 = insertion of <= 200 bp 
	        # 3 = deletion of <= 200 bp 
	        # 4 = multiple base substitution (>= 2bp and <= 200bp) /mnt/seq_results/icgc_pancreatic/APGI_1992/seq_mapped/
		my $mut_type	= -999; #default
		# skip SNPs
		next if(length($n_rec->{'ref_allele'}) == 1 && length($n_rec->{'alt_allele'}) == 1);
		# keep indels
		$mut_type	= 2 if(length($n_rec->{'ref_allele'}) == 1);
		$mut_type	= 3 if(length($n_rec->{'alt_allele'}) == 1);
		$mut_type	= 4 if(length($n_rec->{'alt_allele'}) > 1 && length($n_rec->{'ref_allele'}) > 1);
	        $dcc1rec->variant_type( $mut_type );

	        $dcc1rec->chromosome( $n_rec->chrom );
	        $dcc1rec->chromosome_start( $n_rec->position );
	        # !!! For the moment we'll say all variants are on plus strand
	        $dcc1rec->chromosome_strand( 1 );
		# calculate end coordinate of feature
		my $end		= $n_rec->position + length($n_rec->ref_allele);
	        $dcc1rec->chromosome_end( $end );
	
	        $dcc1rec->refsnp_allele( '-999' );
	        $dcc1rec->refsnp_strand( '-999' );
	        $dcc1rec->reference_genome_allele( $n_rec->ref_allele );
	        $dcc1rec->control_genotype( '-999' );
	        $dcc1rec->tumour_genotype( $n_rec->alt_allele );
	        #$dcc1rec->mutation( $n_rec->ref_allele .'>'. $n_rec->alt_allele );
	        $dcc1rec->quality_score( '-999' );
	        $dcc1rec->probability( '-999' );
	        $dcc1rec->read_count( '-999' );
	        $dcc1rec->is_annotated( '-999' );
	        $dcc1rec->verification_status( '-999' );
	        $dcc1rec->verification_platform( '-999' );
	        $dcc1rec->xref_ensembl_var_id( '-999' );
	        #$dcc1rec->uri( '-999' );
	        #$dcc1rec->db_xref( '-999' );
	        $dcc1rec->note( '-999' );
	        
	        $dcc1rec->QCMGflag( 'PASS' );
	        $dcc1rec->ND( '--' );
	        $dcc1rec->TD( '--' );
	        $dcc1rec->NNS( '--' );
	        $dcc1rec->FlankSeq( '--' );
	        $dcc1rec->Mutation( $n_rec->ref_allele .'>'. $n_rec->alt_allele );

		# transform coordinates, alleles, and mutation to comply
		# with DCC format
		$dcc1rec	= $self->_unify_coordinates($dcc1rec, 'germline');

		# check for homopolymer run in VCF INFO field; if it is a long
		# run, write record directly to low-conf germline file to avoid
		# further annotation
		my $info	= $n_rec->{'info'};
		$info 		=~ /HRun=(\d+);/;
		my $hrun	= $1;
		if($hrun > 12) {
			$dcc1lcg->write_record( $dcc1rec );
			$lcg_germline_indels++;
		}
		else {
	        	$dcc1->write_record( $dcc1rec );
			$germline_indels++;
		}
	
	}
	#$n_vcf->close;
	$dcc1->close;
	$dcc1lcg->close;
	
	qlogprint("\tRead\t$tumour_indels tumour indels (should be sum of matching and somatic)\n");
	qlogprint("\t Found\t$matching_indels matching indels\n");
	qlogprint("\t Created\t$somatic_indels somatic indels\n");
	qlogprint("\tRead\t$normal_indels normal indels (should be sum of germline and hp-associated germline\n");
	qlogprint("\t Kept\t$germline_indels germline indels\n");
	qlogprint("\t Kept\t$lcg_germline_indels homopolymer-associated germline indels\n");

	return;
}

sub _unify_coordinates {
	my $self	= shift;
	my $rec		= shift;	# DCC1 record to alter
	my $rectype	= shift;	# "somatic" or "germline"

	#print Dumper $rec;

	unless($rec->chromosome =~ /\w/ && $rec->chromosome_start =~ /\d/) {
		# if the coordinates cannot be changed, just return the
		# unchanged record
		qlogprint("\tWARNING: Invalid DCC1 record - cannot unify coordinates\n");
		return($rec);
	}

	# get tool-specific genomic coordinates and alleles and alter them so
	# they conform to the DCC standard
	my $chr		= $rec->chromosome;
	my $s		= $rec->chromosome_start;
	my $e		= $rec->chromosome_end;
	my $mut_type;
	if($rectype eq 'somatic') {
		$mut_type	= $rec->mutation_type;
	}
	elsif($rectype eq 'germline') {
		$mut_type	= $rec->variant_type;
	}
	my $ref		= $rec->reference_genome_allele;
	my $alt		= $rec->tumour_genotype;
	my $mut;
	if($rectype eq 'somatic') {
		$mut		= $rec->mutation;
	}
	elsif($rectype eq 'germline') {
		$mut		= $rec->Mutation;
	}

	# GATK deletion
	if($mut_type == 3) {
		$s	+= 1;
		$e	-= 1;
		# remove first base of $alt
		$ref	=~ s/^.//;
		$alt	= '-' x length($ref);
		$mut	= join "/", $ref, $alt;

		#print STDERR "DCC1\t$mut_type: $ref -> $alt\t$mut\n";
		$rec->chromosome_start($s);
		$rec->chromosome_end($e);
		$rec->reference_genome_allele($ref);
		$rec->tumour_genotype($alt);
		if($rectype eq 'somatic') {
			$mut		= $rec->mutation($mut);
		}
		elsif($rectype eq 'germline') {
			$mut		= $rec->Mutation($mut);
		}
		#$rec->mutation($mut);
	}
	# GATK insertion
	elsif($mut_type == 2) {
		# remove first base of $alt
		$alt	=~ s/^.//;
		$ref	= '-' x length($alt);
		$mut	= join "/", $ref, $alt;

		#print STDERR "DCC1\t$mut_type: $ref -> $alt\t$mut\n";
		$rec->reference_genome_allele($ref);
		$rec->tumour_genotype($alt);
		if($rectype eq 'somatic') {
			$mut		= $rec->mutation($mut);
		}
		elsif($rectype eq 'germline') {
			$mut		= $rec->Mutation($mut);
		}
		#$rec->mutation($mut);
	}
	elsif($mut_type == 4) {
		# remove first base of $alt
		$alt	=~ s/^.//;	# GUESSING
		$ref	=~ s/^.//;	# GUESSING
		$ref	= '-' x length($alt);
		$mut	= join "/", $ref, $alt;

		#print STDERR "DCC1\t$mut_type: $ref -> $alt\t$mut\n";
		$rec->reference_genome_allele($ref);
		$rec->tumour_genotype($alt);
		if($rectype eq 'somatic') {
			$mut		= $rec->mutation($mut);
		}
		elsif($rectype eq 'germline') {
			$mut		= $rec->Mutation($mut);
		}
	}

	return($rec);
}

sub _fix_dcc1_headers {
	my $self    = shift;

	# We use the ini file params so often, it's simpler to do this
	my $ini = $self->ini;

	qlogprint("\tFixing somatic  DCC1:  ".$self->dcc1_indel_file."\n");
	qlogprint("\tFixing germline DCC1:  ".$self->dcc1_germline_indel_file."\n");

	my $origfile		= $self->dcc1_indel_file.".orig";
	my $fixed_dcc1		= $self->dcc1_indel_file.".fixedheader";
	my $fixed_dcc1_log	= $self->dcc1_indel_file.".fixedheader.log";

	my $gorigfile		= $self->dcc1_germline_indel_file.".orig";
	my $gfixed_dcc1		= $self->dcc1_germline_indel_file.".fixedheader";

	my $cmd	= qq{qmule org.qcmg.qmule.IndelDCCHeader --input }.$self->dcc1_indel_file.qq{ --input }.$self->dcc1_germline_indel_file.qq{ --tumour }.$self->tumourbam.qq{ --normal }.$self->normalbam.qq{ --output }.$fixed_dcc1.qq{ --output }.$gfixed_dcc1.qq{ --mode gatk -log  }.$fixed_dcc1_log; 
	qlogprint("\t$cmd\n");
	my $rv	= system($cmd);

	# this doesn't seem to work properly...
	#if($rv != 0) {
	#	die qlogprint("\tDCC1 re-headering failed\n");
		#return();
	#}

	# move files around; replace original DCC1 with re-headered DCC1
	# file and save original file 
	$cmd		= qq{cp }.$self->dcc1_indel_file.qq{ $origfile};
	qlogprint("\t$cmd\n");
	$rv		= system($cmd) if($rv == 0);
	$cmd		= qq{cp $fixed_dcc1 }.$self->dcc1_indel_file;
	qlogprint("\t$cmd\n");
	system($cmd) if($rv == 0);

	$cmd		= qq{cp }.$self->dcc1_germline_indel_file.qq{ $gorigfile};
	qlogprint("\t$cmd\n");
	$rv		= system($cmd) if($rv == 0);
	$cmd		= qq{cp $gfixed_dcc1 }.$self->dcc1_germline_indel_file;
	qlogprint("\t$cmd\n");
	system($cmd) if($rv == 0);

	#### Get header info for later use
	# get somatic header info
	my $dcc		= QCMG::IO::DccSnpReader->new(
				filename	=> $self->dcc1_indel_file,
				verbose		=> $self->{'verbose'},
			);
	my $smeta        = $dcc->qcmgmeta_string();
	$smeta		=~ /\#Q_DCCMETA\s+analysisId\s+(.+)/;
	my $analysis_id	= $1;
	$smeta           =~ /\#Q_DCCMETA\s+analyzed_sample_id\s+(.+)/;
	my $analyzed_sample_id	= $1;
	# get germline header info
	$dcc		= QCMG::IO::DccSnpReader->new(
				filename	=> $self->dcc1_germline_indel_file,
				verbose		=> $self->{'verbose'},
			);
	my $gmeta        = $dcc->qcmgmeta_string();
	$gmeta           =~ /\#Q_DCCMETA\s+matched_sample_id\s+(.+)/;
	my $matched_sample_id	= $1;
	# set vars
	$self->{'analysis_id'}		= $analysis_id;
	$self->{'analyzed_sample_id'}	= $analyzed_sample_id;
	$self->{'matched_sample_id'}	= $matched_sample_id;
	$self->{'meta_somatic'} 	= $smeta;
	$self->{'meta_germline'} 	= $gmeta;

	return();
}

# look at pileups and move any indels that are obviously germline indels to the
# germline DCC1 file from the somatic DCC1 indel file
sub _revise_indel_assignment {
	my $self = shift;

	# need for normal sample ID 
	my $ini = $self->ini;
	
	my $s_outfile	= $self->dcc1_indel_file.".revised";
	my $g_outfile	= $self->dcc1_germline_indel_file.".revised";

	# read somatic indels from DCC1 file
	my $dcc1 = QCMG::IO::DccSnpReader->new(
				filename => $self->dcc1_indel_file,
				verbose  => $self->{verbose} );

	# somatic/germline from file type
	die "input file must be a DCC1 variant but detected file type is [", $dcc1->version, "]\n" unless ($dcc1->dcc_type eq 'dcc1');
	
	# create somatic output file in same DCC1 format
	my $dcc1_version = 'dcc1_dbssm_somatic_r' . $self->{release};
	my $s_dcc1out	= QCMG::IO::DccSnpWriter->new(
				filename	=> $s_outfile,
				version		=> $dcc1_version,
				verbose		=> $self->{verbose},
	               		meta     	=> qexec_header().$self->{'meta_somatic'},
		   		annotations => { Organism => $self->{organism}, "QAnnotateMode" => "gatkindel"}

				);

	# create germline output file in same DCC1 format
	my $dcc1_g_version = 'dcc1_dbsgm_germline_r' . $self->{release};
	my $g_dcc1out	= QCMG::IO::DccSnpWriter->new(
				filename	=> $g_outfile,
				version		=> $dcc1_g_version,
				verbose		=> $self->{verbose},
	               		meta     	=> qexec_header().$self->{'meta_germline'},
			   annotations => { Organism => $self->{organism}, "QAnnotateMode" => "gatkindel"}

				);


	my $count_new_germline	= 0;
	my $count_new_somatic	= 0;
	my $count_total		= 0;
	my $lowsupcount		= 0;
	my $nnscount		= 0;
	my @new_germline_recs	= ();
	while(my $rec	= $dcc1->next_record) {
		#my $flag	= $rec->QCMGflag;

		my $nd		= $rec->ND;

		if($nd	!~ /[\d;]+/) {
			die "ND field in ".$self->dcc1_indel_file." missing\n";
		}

		#my ($nnovel_starts, $nfall, $nfinf, $nfpres, $npartial_indel, $nindel_near, $nsoftclip_near)		= split ";", $nd;
		#my ($tnovel_starts, $tfall, $tfinf, $tfpres, $tpartial_indel, $tindel_near, $tsoftclip_near, $thp)	= split ";", $td;
		# thp: ;"0 discontiguous CAGAATG__ATATATATATATATATATATATATATATATATATATATATATGCAAAATTTAAAATTAGAGTATTTCATATGAGAAGATGAGTAATTTAAATAA"
		#$thp				=~ s/"//g;
		#my ($numhom, $contig, $seq)	= split " ", $thp;

		my ($nns, $nt, $ni, $nsi)	= split /;/, $nd;
		#print STDERR "$nd\n$nns, $nt, $ni, $nsi\n";
		my $lowsupperc			= 1;	# default
		if(defined $ni && $ni > 0) {
			$lowsupperc		= $nsi / $ni;
		}

		if($lowsupperc > 0.10) {
			$rec->analyzed_sample_id( $self->{'matched_sample_id'} );
			# we call this as mutation_id because it is in a somatic
			# file; it will become variant_id in the germline
			$rec->mutation_id( $self->{'analysis_id'}."_".$self->{'matched_sample_id'}."_ind".$self->{'last_germline_var_id'}++ );
			push @new_germline_recs, $rec;
			#$g_dcc1out->write_record($rec);
			$count_new_germline++;
			$lowsupcount++;
		}
		# if indel has more than 3 novel-start reads in the normal
		# sample, call it germline (don't bother using the MIN flag
		# because we use normal novel starts to set that flag)
		elsif($nns > 3) {
			$rec->analyzed_sample_id( $self->{'matched_sample_id'} );
			# we call this as mutation_id because it is in a somatic
			# file; it will become variant_id in the germline
			$rec->mutation_id( $self->{'analysis_id'}."_".$self->{'matched_sample_id'}."_ind".$self->{'last_germline_var_id'}++ );
			push @new_germline_recs, $rec;
			#$g_dcc1out->write_record($rec);
			$count_new_germline++;
			$nnscount++;
		}
		else {
			$s_dcc1out->write_record($rec);
			$count_new_somatic++;
		}

		$count_total++;
	}

	$s_dcc1out->close;


	# translate somatic records into germline records; then write new file
	my $s2g_recs	= $self->_somatic_dcc1_rec_to_germline_dcc1_rec(\@new_germline_recs);
	foreach my $rec (@{$s2g_recs}) {
		$g_dcc1out->write_record($rec);
	}
	$g_dcc1out->close;

	qlogprint("\tRead\t\t$count_total\tsomatic DCC1 records\n");
	qlogprint("\tReassigned\t$count_new_germline\tDCC1 records as germline\n");
	qlogprint("\tKept\t$count_new_somatic\tDCC1 records as somatic\n");
	qlogprint("\tReads with low allele frequency support $lowsupcount\n");
	qlogprint("\tReads with more than 3 novel start reads in normal $nnscount\n");

	# copy revised somatic file back to original filename
	my $cmd		= qq{cp $s_outfile }.$self->dcc1_indel_file;
	qlogprint("\t$cmd\n");
	system($cmd);

	# combine old germline and revised germline files into one DCC1
	# remove header from revised germline DCC1
	$cmd	= qq{sed '/^#/ d' -i }.$g_outfile;
	qlogprint("\t$cmd\n");
	system($cmd);
	$cmd	= qq{sed '/^analysis_id/ d' -i }.$g_outfile;
	qlogprint("\t$cmd\n");
	system($cmd);
	my $unsorteddcc1	= $self->dcc1_germline_indel_file.".unsorted";
	$cmd	= qq{cat }.$self->dcc1_germline_indel_file.qq{ }.$g_outfile.qq{ > }.$unsorteddcc1;
	qlogprint("\t$cmd\n");
	system($cmd);

	# sort germline DCC1 file so all indels are in order by genomic
	# coordinate
	$cmd	= qq{/share/software/QCMGPerl/distros/annotation/src/sort_dcc1.pl -i $unsorteddcc1 -o }.$self->dcc1_germline_indel_file;
	qlogprint("\t$cmd\n");
	system($cmd);

	return;
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
                   meta     => qexec_header().$self->{'meta_somatic'},
		   annotations => { Organism => $self->{organism}, "QAnnotateMode" => "gatkindel"},
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
                   meta     => qexec_header().$self->{'meta_somatic'},
		   annotations => { Organism => $self->{organism}, "QAnnotateMode" => "gatkindel"},
                   verbose  => $self->verbose );

    foreach my $rec (@ens_recs) {
        my $dccqrec = $rec->as_dccq_record;
        $dccq->write_record( $dccqrec );
    }

	qlogprint("\tEnsembl annotation finished.\n");

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

sub _annotate_germline_indels {
    my $self = shift;

    # Because there are so many types of DCC1 file, we are going to let
    # the DccSnpReader guess the version rather than trying to specify it
    
    my $dcc1 = QCMG::IO::DccSnpReader->new(
                   filename => $self->dcc1_germline_indel_file,
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
        if ($rec->variant_type == 2) {
            $mutation = '-/'. $rec->tumour_genotype;
        }
        elsif ($rec->variant_type == 3) {
            $mutation = $rec->reference_genome_allele . '/-';
        }

        # Create an annotatable record for the DCC1 record
        push @ens_recs,
             QCMG::Annotate::EnsemblConsequenceRecord->new(
                 chrom       => $rec->chromosome,
                 chrom_start => $rec->chromosome_start,
                 chrom_end   => $rec->chromosome_end,
                 mutation    => $mutation,
                 var_name    => $rec->variant_id,
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
                   filename => $self->dcc2_germline_indel_file,
                   version  => $dcc2_version,
                   meta     => qexec_header().$self->{'meta_germline'},
		   annotations => { Organism => $self->{organism}, "QAnnotateMode" => "gatkindel"},
                   verbose  => $self->verbose );
    
    foreach my $rec (@ens_recs) {
        my $ra_dcc2recs = $rec->as_dcc2_records;

        #qlogprint( $rec->dcc1->variant_id,  
        #           ' ('. $rec->dcc1->variant_type .') has ',
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
                     $dcc2rec->variant_id, ' ',
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
                   filename => $self->dccq_germline_indel_file,
                   version  => $dccq_version,
                   meta     => qexec_header().$self->{'meta_germline'},
		   annotations => { Organism => $self->{organism}, "QAnnotateMode" => "gatkindel"},
                   verbose  => $self->verbose );

    foreach my $rec (@ens_recs) {
        my $dccqrec = $rec->as_dccq_record;
        $dccq->write_record( $dccqrec );
    }

	qlogprint("\tEnsembl annotation finished.\n");

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
		qlogprint("\tSkipping repeat annotation, no file provided\n");
		return;
	}

    # if repeatmasked genome file is provided, edit the DCC1 file and append
    # repeat information to the QCMGflag field
	my $origfile	= $self->dcc1_indel_file.".unannotated";
	my $outfile	= $self->dcc1_indel_file.".repeatmasked";

	qlogprint("\tAnnotating repeats with ".$self->repeats."\n");

	#my $qag	= QCMG::Annotate::Gff->new(	infile	=> $self->dcc1_indel_file,
	#					dcc1out	=> $outfile,
	#					gfffile	=> $self->repeats
	#				);
	#$qag->execute;

	my $cmd	= qq{qmule org.qcmg.qmule.AnnotateDCCWithGFFRegions --input }.$self->dcc1_indel_file.qq{ --input }.$self->repeats.qq{ --output $outfile -log $outfile.log};
	qlogprint("\t$cmd\n");
	my $rv	= system($cmd);

	# move files around; replace unannotated dcc1 file with repeatmasked
	# file and save original file 
	$cmd		= qq{cp }.$self->dcc1_indel_file.qq{ $origfile};
	qlogprint("\t$cmd\n");
	$rv		= system($cmd) if($rv == 0);
	$cmd		= qq{cp $outfile }.$self->dcc1_indel_file;
	qlogprint("\t$cmd\n");
	system($cmd) if($rv == 0);

	return;
}

# determine if mutation resides in known repeat regions
sub _annotate_germline_repeats {
    my $self = shift;

	# if no repeatmasked genome file is provided, don't do anything
	if(! defined $self->repeats) {
		qlogprint("\tSkipping repeat annotation, no file provided\n");
		return;
	}

    # if repeatmasked genome file is provided, edit the DCC1 file and append
    # repeat information to the QCMGflag field
	my $origfile	= $self->dcc1_germline_indel_file.".unannotated";
	my $outfile	= $self->dcc1_germline_indel_file.".repeatmasked";

	qlogprint("\tAnnotating repeats with ".$self->repeats."\n");

	#my $qag	= QCMG::Annotate::Gff->new(	infile	=> $self->dcc1_germline_indel_file,
	#					dcc1out	=> $outfile,
	#					gfffile	=> $self->repeats
	#				);
	#$qag->execute;

	my $cmd	= qq{qmule org.qcmg.qmule.AnnotateDCCWithGFFRegions --input }.$self->dcc1_germline_indel_file.qq{ --input }.$self->repeats.qq{ --output $outfile -log $outfile.log};
	qlogprint("\t$cmd\n");
	my $rv	= system($cmd);

	# move files around; replace unannotated dcc1 file with repeatmasked
	# file and save original file 
	$cmd		= qq{cp }.$self->dcc1_germline_indel_file.qq{ $origfile};
	qlogprint("\t$cmd\n");
	$rv		= system($cmd) if($rv == 0);
	$cmd		= qq{cp $outfile }.$self->dcc1_germline_indel_file;
	qlogprint("\t$cmd\n");
	system($cmd) if($rv == 0);

	return;
}

# filter indels and add info to ND/TD fields
sub _filter_indels {
    my $self = shift;

	my $outfile	= $self->dcc1_indel_file.".filtered";

	my $cmd		.= qq{qbasepileup -t 4 --gatk };
	$cmd		.= qq{ --it }.$self->tumourbam;
	$cmd		.= qq{ --in }.$self->normalbam;
	$cmd		.= qq{ -r  }.$self->genome;
	$cmd		.= qq{ -is }.$self->dcc1_indel_file;
	$cmd		.= qq{ -os }.$outfile;
	$cmd		.= qq{ -m indel};
	qlogprint("\t$cmd\n");
	my $rv		= system($cmd);
	#print STDERR "QBASEPILEUP SOMATIC RV: $rv\n";
	die "qbasepileup failed ($rv)\n" if($rv != 0);

	$cmd		= qq{cp $outfile }.$self->dcc1_indel_file;
	qlogprint("\t$cmd\n");
	system($cmd) if($rv == 0);

	return;
}

# filter germline indels and add info to ND/TD fields
sub _filter_germline_indels {
    my $self = shift;

	my $outfile	= $self->dcc1_germline_indel_file.".filtered";

	my $cmd		.= qq{qbasepileup -t 4 --gatk };
	$cmd		.= qq{ --it }.$self->tumourbam;
	$cmd		.= qq{ --in }.$self->normalbam;
	$cmd		.= qq{ -r  }.$self->genome;
	$cmd		.= qq{ -ig }.$self->dcc1_germline_indel_file;
	$cmd		.= qq{ -og }.$outfile;
	$cmd		.= qq{ -m indel};
	qlogprint("\t$cmd\n");
	my $rv		= system($cmd);
	#print STDERR "QBASEPILEUP GERMLINE RV: $rv\n";
	die "qbasepileup failed ($rv)\n" if($rv != 0);

	$cmd		= qq{cp $outfile }.$self->dcc1_germline_indel_file;
	qlogprint("\t$cmd\n");
	system($cmd) if($rv == 0);

	return;
}

# look at repeat- and filter-annotated germine indels and move any indels that
# are in repeatmasked regions, have extremely high coverage, or are in a
# homopolymeric region to a different file to avoid Ensembl-annotating them
sub _remove_lowconf_germline_indel {
	my $self = shift;

	my $glc_outfile	= $self->dcc1_germline_indel_file.".lowconf";
	my $gk_outfile	= $self->dcc1_germline_indel_file.".not_lowconf";

	# read somatic indels from DCC1 file
	my $dcc1 = QCMG::IO::DccSnpReader->new(
				filename => $self->dcc1_germline_indel_file,
				verbose  => $self->{verbose} );

	# somatic/germline from file type
	die "input file must be a DCC1 variant but detected file type is [", $dcc1->version, "]\n" unless ($dcc1->dcc_type eq 'dcc1');
	
	# create low conf germline output file in same DCC1 format
	my $glc_dcc1out	= QCMG::IO::DccSnpWriter->new(
				filename	=> $glc_outfile,
				version		=> $dcc1->version,
				verbose		=> $self->{verbose},
	               		meta     => qexec_header().$self->{'meta_germline'},
		   		annotations => { Organism => $self->{organism}, "QAnnotateMode" => "gatkindel"}
				);

	# create remaining germline output file in same DCC1 format
	my $gk_dcc1out	= QCMG::IO::DccSnpWriter->new(
				filename	=> $gk_outfile,
				version		=> $dcc1->version,
				verbose		=> $self->{verbose},
	               		meta     => qexec_header().$self->{'meta_somatic'},
		   		annotations => { Organism => $self->{organism}, "QAnnotateMode" => "gatkindel"}
				);



	my $count_lowconf	= 0;
	my $count_hpcon		= 0;
	my $count_hpemb		= 0;
	my $count_rp_sat	= 0;
	my $count_rp_sr		= 0;
	my $count_hcov		= 0;
	my $count_lowsupport	= 0;
	my $count_keep		= 0;
	my $count_total		= 0;
	while(my $rec	= $dcc1->next_record) {
		my $flag	= $rec->QCMGflag;
		my $nd		= $rec->ND;

		if($nd	!~ /[\d;]+/) {
			die "ND field in ".$self->dcc1_germline_indel_file." missing\n";
		}

		# get number of normal supporting reads and number of normal
		# informative reads
		#number of novel starts, number of total reads, number of informative reads, number of supporting informative reads, partial matches in reads, number of insertions nearby, number of softclipped reads nearby
		#4;39;24;4;4;0;5
		my ($nns, $nt, $ni, $nsi)	= split /;/, $nd;
		#print STDERR "$nd\n$nns, $nt, $ni, $nsi\n";
		my $lowsupperc			= 1;	# default
		if(defined $ni && $ni > 0) {
			$lowsupperc		= $nsi / $ni;
		}

                my $hpcon_len			= 0;
		my $hpemb_len			= 0;
                if($flag =~ /HOMCON\_(\d+)/) {
                        $hpcon_len      = $1;
                }
                if($flag =~ /HOMEMB\_(\d+)/) {
                        $hpemb_len      = $1;
                }

		# if indel is 
		# - has very high coverage
		# - in a repeatmasked region
		# - is contiguous with a homopolymer of length 7 or more
		# - is embedded in a homopolymer of length 7 or more
		if($flag =~ /HCOV/) {
			$glc_dcc1out->write_record($rec);
			$count_lowconf++;
			$count_hcov++;
		}
		elsif($flag =~ /Simple\_repeat/) { 
			$glc_dcc1out->write_record($rec);
			$count_lowconf++;
			$count_rp_sr++;
		}
		elsif($flag =~ /Satellite/) {
			$glc_dcc1out->write_record($rec);
			$count_lowconf++;
			$count_rp_sat++;
		}
		elsif($hpcon_len >= 7) {
			$glc_dcc1out->write_record($rec);
			$count_lowconf++;
			$count_hpcon++;
		}
		elsif($hpemb_len >= 7) {
			$glc_dcc1out->write_record($rec);
			$count_lowconf++;
			$count_hpemb++;
		}
		elsif($lowsupperc < 0.10) {
			$glc_dcc1out->write_record($rec);
			$count_lowconf++;
			$count_lowsupport++;
		}
		else {
			$gk_dcc1out->write_record($rec);
			$count_keep++;
		}

		$count_total++;
	}

	$glc_dcc1out->close;
	$gk_dcc1out->close;

	qlogprint("\tRead\t\t$count_total\tgermline DCC1 records\n");
	qlogprint("\tRemoved\t$count_lowconf\tDCC1 records\n");
	qlogprint("\t\t\t$count_hcov\tHCOV records\n");
	qlogprint("\t\t\t$count_rp_sr\tSimple_repeat records\n");
	qlogprint("\t\t\t$count_rp_sat\tSatellite records\n");
	qlogprint("\t\t\t$count_hpcon\thomopolymer-contiguous records\n");
	qlogprint("\t\t\t$count_hpemb\thomopolymer-embedded records\n");
	qlogprint("\t\t\t$count_lowsupport\trecords where less than 10% of reads have indel\n");
	qlogprint("\tKept\t$count_keep\tDCC1 records\n");

	# copy revised somatic file back to original filename
	my $cmd		= qq{cp $gk_outfile }.$self->dcc1_germline_indel_file;
	qlogprint("\t$cmd\n");
	system($cmd);

	return;
}

sub _somatic_dcc1_rec_to_germline_dcc1_rec {
	my $self	= shift @_;
	my $recs	= shift @_;	# somatic records to translate to germline records

	my $dcc1_version = 'dcc1_dbsgm_germline_r' . $self->{release};

	my @germrecs	= ();
	foreach my $dcc1rec (@{$recs}) {

	        my $gdcc1 = QCMG::IO::DccSnpRecord->new(
	                          version  => $dcc1_version,
	                          verbose  => 1 );

		my $mut	= join "/", $dcc1rec->reference_genome_allele, $dcc1rec->tumour_genotype;
		my $variant_id	= $dcc1rec->mutation_id;
		$variant_id	=~ /^.+\_ind(\d+)/;
		$variant_id	= join "_". $self->{'analysis_id'}, $self->{'matched_sample_id'}, "ind".$1;

	        $gdcc1->analysis_id( $dcc1rec->analysis_id );
	        $gdcc1->analyzed_sample_id( $self->{'matched_sample_id'} );
	        $gdcc1->variant_id( $variant_id );
	        $gdcc1->variant_type( $dcc1rec->mutation_type );
	        $gdcc1->chromosome( $dcc1rec->chromosome );
	        $gdcc1->chromosome_start( $dcc1rec->chromosome_start );
	        $gdcc1->chromosome_strand( $dcc1rec->chromosome_strand );
	        $gdcc1->chromosome_end( $dcc1rec->chromosome_end );
	        $gdcc1->refsnp_allele( $dcc1rec->refsnp_allele );
	        $gdcc1->refsnp_strand( $dcc1rec->refsnp_strand );
	        $gdcc1->reference_genome_allele( $dcc1rec->reference_genome_allele );
	        $gdcc1->control_genotype( $dcc1rec->control_genotype );
	        $gdcc1->tumour_genotype( $dcc1rec->tumour_genotype );
	        $gdcc1->quality_score( $dcc1rec->quality_score );
	        $gdcc1->probability( $dcc1rec->probability );
	        $gdcc1->read_count( $dcc1rec->read_count );
	        $gdcc1->is_annotated( $dcc1rec->is_annotated );
	        $gdcc1->verification_status( $dcc1rec->verification_status );
	        $gdcc1->verification_platform( $dcc1rec->verification_platform );
	        $gdcc1->xref_ensembl_var_id( $dcc1rec->xref_ensembl_var_id );
	        $gdcc1->note( $dcc1rec->note );
	        $gdcc1->QCMGflag( $dcc1rec->QCMGflag );
	        $gdcc1->ND( $dcc1rec->ND );
	        $gdcc1->TD( $dcc1rec->TD );
	        $gdcc1->NNS( $dcc1rec->NNS );
	        $gdcc1->FlankSeq( $dcc1rec->FlankSeq );
	        $gdcc1->Mutation( $mut );

		push @germrecs, $gdcc1;
	}

	return(\@germrecs);
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
        $dccqrec->aa_variant('-888');
        $dccqrec->cds_variant('-888');
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

# generate MAF
sub _create_somatic_maf {
    my $self = shift;

	my $cmd	= qq{groovy /panfs/home/production/Devel/QCMGScripts/o.holmes/groovy/maf_TEST.groovy -dcc }.$self->{dccq_indel_file}.qq{ -indel -somatic};
	qlogprint($cmd."\n");
	my $rv	= system($cmd);
	qlogprint("RV: $rv\n");

	return;
}
sub _create_germline_maf {
    my $self = shift;

	my $cmd	= qq{groovy /panfs/home/production/Devel/QCMGScripts/o.holmes/groovy/maf_TEST.groovy -dcc }.$self->{dccq_germline_indel_file}.qq{ -indel -germline};
	qlogprint($cmd."\n");
	my $rv	= system($cmd);
	qlogprint("RV: $rv\n");

	return;
}

1;

__END__

=head1 NAME

QCMG::Annotate::GATKindel - Annotate GATK indel output files


=head1 SYNOPSIS

 use QCMG::Annotate::GATKindel;

my %defaults = ( ensver   => '70',
                 organism => 'human',
                 release  => '14',
                 verbose  => 0 );
 my $qsnp = QCMG::Annotate::GATKindel->new( %defaults );
 $qsnp->execute;


=head1 DESCRIPTION

This module provides functionality for annotating GATK indel output.  It
includes commandline parameter processing.

This module is based on a series of SNP annotation scripts originally
created by Karin Kassahn and JP and elaborated by Karin to adapt to new
organisms (Mouse etc) and new versions for the EnsEMBL API.  This
version has been rewritten by JP for better diagnostics and to support
the annotation of SNPs from different organisms and EnsEMBL versions
within a single executable.

=head2 Commandline parameters

 -c | --config        config file
 -l | --logfile       log file (optional)
 -s | --sampleid      Sample ID
 -rm| --repeats       RepeatMasked genome file (optional)
 -g | --organism      Organism; default=human
 -e | --ensver        EnsEMBL version; default=70
 -r | --release       DCC release; default=14
 -d | --domains       Ensembl domains file
 -y | --symbols       Ensembl gene ids and symbols file
 -v | --verbose       print progress and diagnostic messages
      --version       print version number and exit immediately
 -h | -? | --help     print help message


=over

=item B<-c | --config>

Mandatory. The config file is in a QCMG-specific format. Here is an example:

Filename: APGI_1839_1NormalBlood_vs_10XenograftCellLine_qanno.ini

File contents:
[Normal]
BAM=IcgcPancreatic_APGI1839_1DNA_1NormalBlood_ICGCABMB2012050507ND_IlluminaTruSEQMultiplexedManual_NoCapture_Bwa_HiSeq.jpearson.bam
LABEL=1NormalBlood

[Tumour]
BAM=IcgcPancreatic_APGI1839_1DNA_10CellLineDerivedFromXenograft_ICGCABMJ2010102219CD_IlluminaTruSEQMultiplexedManual_NoCapture_Bwa_HiSeq.jpearson.bam
LABEL=10CellLineDerivedFromXenograft

[GATKindel]
REFERENCE=/panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa
REFERENCE_LABEL=GRCh37_ICGC_standard_v2
REFERENCE_DATE=2010-06-17
OUTPUT=APGI1839_1DNA_1NormalBlood_10CellLineDerivedFromXenograft
CHROM=ALL
THREADS=8

[QGATKindel]
INDIR=/mnt/seq_results/icgc_pancreatic/APGI_1839/variants/GATK/hiseq_normal_blood_xenograft_cellline/
OUTDIR=/mnt/seq_results/icgc_pancreatic/APGI_1839/variants/GATK/hiseq_normal_blood_xenograft_cellline/
OUTPUT=APGI1839_1DNA_1NormalBlood_10CellLineDerivedFromXenograft

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
between the found and expected columns.  The default value is '14'.

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

$Id: GATKindel.pm 3923 2013-06-14 03:54:46Z l.fink $


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
