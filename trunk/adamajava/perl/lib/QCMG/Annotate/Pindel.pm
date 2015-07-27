package QCMG::Annotate::Pindel;

###########################################################################
#
#  Module:   QCMG::Annotate::Pindel.pm
#  Creator:  John V Pearson
#  Created:  2012-11-28
#
#  Module to provide annotation for variants called by pindel.
#
#  N.B. This is being used as a development mule for qSNP annotation
#  using the new EnsemblConsequences module so as of 2012-11-29 it does
#  *NOT* contain any logic relating to pindel.
#
#  As of 2013-01-10 we started the conversion to pindel functionality.
#  We will need at least the following steps:
#
#  1. Determine which pindel files to parse
#  2. Create SV-specific DCC1 files with appropriate .$self->{'meta_somatic'}annotations for
#     each mutation type
#  3. Create DCC1-to-DCCQ and DCC1-to-DCC2 converters
#  4. Work out how to use the Ensembl annotator.
#
#  
#
#  $Id$
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
use QCMG::IO::PindelReader;
use QCMG::IO::VcfReader;
use QCMG::Run::Annovar;
use QCMG::Run::Pindel;
use QCMG::Util::FileManipulator;
use QCMG::Util::QLog;
use QCMG::Util::Util qw( qexec_header );

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
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
		   genome   => $defaults{genome},
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
	   'genome=s'		=> \$params{genome},		# -genome

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
    $params{genome}	= $ini->param('pindel', 'reference');

    # DCC file names are derived from [Pindel]->OUTPUT 
    my $dcc1_sv_file = $ini->param('qpindel','outdir') .'/'.
                       $ini->param('qpindel','output') .'_sv.dcc1';
    my $dcc2_sv_file = $ini->param('qpindel','outdir') .'/'.
                       $ini->param('qpindel','output') .'_sv.dcc2';
    my $dccq_sv_file = $ini->param('qpindel','outdir') .'/'.
                       $ini->param('qpindel','output') .'_sv.dccq';
    my $dcc1_indel_file = $ini->param('qpindel','outdir') .'/'.
                          $ini->param('qpindel','output') .'_indel.dcc1';
    my $dcc2_indel_file = $ini->param('qpindel','outdir') .'/'.
                          $ini->param('qpindel','output') .'_indel.dcc2';
    my $dccq_indel_file = $ini->param('qpindel','outdir') .'/'.
                          $ini->param('qpindel','output') .'_indel.dccq';

    my $dcc1_germline_indel_file = $ini->param('qpindel','outdir') .'/'.
                          $ini->param('qpindel','output') .'_indel_germline.dcc1';
    my $dcc2_germline_indel_file = $ini->param('qpindel','outdir') .'/'.
                          $ini->param('qpindel','output') .'_indel_germline.dcc2';
    my $dccq_germline_indel_file = $ini->param('qpindel','outdir') .'/'.
                          $ini->param('qpindel','output') .'_indel_germline.dccq';

	#print STDERR "$dcc1_sv_file\n";
	#print STDERR "$dcc2_sv_file\n";
	#print STDERR "$dccq_sv_file\n";
	#print STDERR "$dcc1_indel_file\n";
	#print STDERR "$dcc2_indel_file\n";
	#print STDERR "$dccq_indel_file\n";
	#print STDERR "$dcc1_germline_indel_file\n";

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
                 ini             => $ini,
                 ann             => $ann,
                 pin             => $pin,
                 dcc1_sv_file    => $dcc1_sv_file,
                 dcc2_sv_file    => $dcc2_sv_file,
                 dccq_sv_file    => $dccq_sv_file,
                 dcc1_indel_file => $dcc1_indel_file,
                 dcc2_indel_file => $dcc2_indel_file,
                 dccq_indel_file => $dccq_indel_file,
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

sub pin {
    my $self = shift;
    return $self->{pin};
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

sub genome {
    my $self = shift;
    return $self->{genome};
}

sub dcc1_sv_file {
    my $self = shift;
    return $self->{dcc1_sv_file};
}

sub dcc2_sv_file {
    my $self = shift;
    return $self->{dcc2_sv_file};
}

sub dccq_sv_file {
    my $self = shift;
    return $self->{dccq_sv_file};
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

    # Here's what we need to do:
    # 1. Read all of the records from all of the files
    # 2. Filter the records based on INI file filters.
    # 3. Sort records into those that the DCC would consider SSM vs
    #    those that would be SV's:
    #    _LI - SV's
    #    _INV - ???
    #    _TD - ???
    #    _D - a mix of SSM and SV depending on length
    #    _SI - a mix of SSM and SV depending on length
    qlogprint("Reading and filtering raw pindel records...\n");
    my ($ra_recs, $ra_germline_recs) = $self->_read_and_filter_pindel_records;

    qlogprint("Sorting raw pindel germline records...\n");
    my ($ra_germsv_recs, $ra_germindel_recs) = $self->_sort_pindel_records( $ra_germline_recs );

    qlogprint("Making germline DCC1...\n");
    $self->_make_germline_indel_dcc1_records( $ra_germindel_recs );

    qlogprint("Sorting raw pindel somatic records...\n");
    my ($ra_sv_recs, $ra_indel_recs) = $self->_sort_pindel_records( $ra_recs );

    qlogprint("Making somatic DCC1...\n");
    $self->_make_indel_dcc1_records( $ra_indel_recs );


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
    $self->_annotate_indel;

    # annotate revised files with Ensembl
    qlogprint("Annotating germline DCC1 file with Ensembl consequences...\n");
    $self->_annotate_germline_indel;

    warn "Not currently attempting annotation of records marked as SV!\n";
#    $self->_make_sv_dcc1_records( $ra_sv_recs );
#    $self->_annotate_sv;

    # create MAFs
    qlogprint("Generating somatic MAF...\n");
    $self->_create_somatic_maf;
    qlogprint("Generating germline MAF...\n");
    $self->_create_germline_maf;

    qlogend();
}


sub _sort_pindel_records {
    my $self    = shift;
    my $ra_recs = shift;

    my %recs_by_type = ();
    my @svs          = ();
    my @indels       = ();
    foreach my $rec (@{ $ra_recs }) {
        $recs_by_type{ $rec->VarType }++;
        if ($rec->VarType eq 'INV') {
            push @svs, $rec;
        }
        elsif ($rec->VarType eq 'TD') {
            push @svs, $rec;
        }
        elsif ($rec->VarType eq 'LI') {
            push @svs, $rec;
        }
        elsif ($rec->VarType eq 'D') {
            if ($rec->VarLength > 200) {
                push @svs, $rec;
            }
            else {
                push @indels, $rec;
            }
        }
        elsif ($rec->VarType eq 'I') {
            if ($rec->VarLength > 200) {
                push @svs, $rec;
            }
            else {
                push @indels, $rec;
            }
        }
        else {
            warn "Could not categorise records with VarType of:",
                 $rec->VarType, "\n";
        }
    }

    if ($self->verbose) {
        qlogprint( "Variant Types:\n" );
        foreach my $key (sort keys %recs_by_type) {
            qlogprint( "  $key:\t", $recs_by_type{$key}, "\n" );
        }
    }
    qlogprint( 'Variants classed as SVs: ',scalar(@svs),"\n" );
    qlogprint( 'Variants classed as Indels: ',scalar(@indels),"\n" );

    return \@svs, \@indels;
}


sub _read_and_filter_pindel_records {
    my $self = shift;

    # We use the ini file params so often, it's simpler to do this
    my $ini = $self->ini;

    my @filters = map { $ini->param( 'filter', $_ ) }
                  sort keys %{ $ini->section( 'filter' ) };

	#print STDERR $ini->param('qpindel','indir'), "\n";
	#print STDERR $ini->param('pindel','output'), "\n";

    # Filter pindel output
    my $ra_indel_files = $self->_std_pindel_indel_filenames( 
                               $ini->param('qpindel','indir'),
                               $ini->param('pindel','output') );



    my $ra_sv_files = $self->_std_pindel_sv_filenames( 
                               $ini->param('qpindel','indir'),
                               $ini->param('pindel','output') );

	#print Dumper $ra_sv_files;

    my @infiles = (@{ $ra_sv_files }, @{ $ra_indel_files });

    #my $ra_passed_recs =$self->_do_filter( \@infiles,
    my ($ra_passed_recs, $ra_germline_recs) =$self->_do_filter( \@infiles,
                                           \@filters,
                                           $ini->param('tumour','label'),
                                           $ini->param('normal','label'),
                                           $self->verbose );

    #return $ra_passed_recs;
    return ($ra_passed_recs, $ra_germline_recs);
}


sub _make_sv_dcc1_records {
    my $self   = shift;
    my $ra_recs = shift;

    # We use the ini file params so often, it's simpler to do this
    my $ini = $self->ini;

    # We are going to need to put the filtered SV records somewhere
    my $filtfile = $ini->param('qpindel','outdir') .'/'.
                   $ini->param('qpindel','output') .'_filtered_sv.txt';

    # Write the filtered records to an outfile
    my $outfh = IO::File->new( $filtfile, 'w' );
    croak "Can't open output file $filtfile for writing: $!"
        unless defined $outfh;
    qlogprint( "writing to file: $filtfile\n" ) if $self->verbose;
    $outfh->print( $_->to_text ) foreach (@{ $ra_recs });
    $outfh->close;

    # Run pindel2vcf to create a VCF file
    my $vcffile = $filtfile .'.vcf';

    # During debugging, we can comment this out once it has run the first
    # time and created a VCF.  Even with cut down pindel input files this
    # step is VERY slow because it has to read and substr on the human
    # genome in order to interpet the variations.

    $self->pin->pindel2vcf(
                  infile          => $filtfile,
                  outfile         => $vcffile,
                  reference       => $ini->param('pindel','reference'),
                  reference_label => $ini->param('pindel','reference_label'),
                  reference_date  => $ini->param('pindel','reference_date'),
                  verbose         => $self->verbose );

    # Read VCF and convert to DCC1
    my $vcf = QCMG::IO::VcfReader->new(
                  filename => $vcffile,
                  verbose  => $self->verbose );

    my $dcc1_version = 'dcc1_dbstsm_somatic_r' . $self->{release};
    my $dcc1 = QCMG::IO::DccSnpWriter->new(
                   filename => $self->dcc1_sv_file,
                   version  => $dcc1_version,
                   meta     => qexec_header().$self->{'meta_somatic'},
		   annotations => { Organism => $self->{organism}, "QAnnotateMode" => "pindel"},
                   verbose  => $self->verbose );

    my $svid = 1;  # ctr for use in unique ID assigned to each SV
    while (my $rec = $vcf->next_record) {
        my $info = $rec->info;

        my $dcc1rec = QCMG::IO::DccSnpRecord->new(
                          version  => $dcc1_version,
                          verbose  => $self->verbose );
        
        $dcc1rec->analysis_id( 'JP_sv1_20130111' );
        $dcc1rec->analyzed_sample_id( $ini->param('tumour','label') );
        $dcc1rec->placement( 1 );
        $dcc1rec->sv_id( $dcc1rec->analyzed_sample_id .'_sv'. $svid++ );
        $dcc1rec->interpreted_annotation( '-999' );

        # This is probably not the corrcet content for these fields but it'll do for testing
        if ($info =~ /SVTYPE=([\w:]+)/) {
            $dcc1rec->annotation( $1 );
            $dcc1rec->variant_type( $1 );
        }
        else {
            $dcc1rec->annotation( '-999' );
            $dcc1rec->variant_type( '-999' );
        }

        $dcc1rec->chr_from( $rec->chrom );
        $dcc1rec->chr_from_bkpt( $rec->position );
        # For the moment we'll say all variants are on plus strand
        $dcc1rec->chr_from_strand( 1 );
        $dcc1rec->chr_from_range( '-999' );
        $dcc1rec->chr_from_flanking_seq( '-999' );

        $dcc1rec->chr_to( $rec->chrom );
        if ($info =~ /END=(\w+)/) {
            $dcc1rec->chr_to_bkpt( $1 );
        }
        else {
            $dcc1rec->chr_to_bkpt( '-999' );
        }
        # For the moment we'll say all variants are on plus strand
        $dcc1rec->chr_to_strand( 1 );
        $dcc1rec->chr_to_range( '-999' );
        $dcc1rec->chr_to_flanking_seq( '-999' );

        $dcc1rec->microhomology_sequence( '-999' );
        $dcc1rec->non_templated_sequence( '-999' );
        $dcc1rec->evidence( '-999' );
        $dcc1rec->quality_score( '-999' );
        $dcc1rec->probability( '-999' );
        $dcc1rec->zygosity( '-999' );
        #$dcc1rec->validation_status( '-999' );
        #$dcc1rec->validation_status( '-999' );
        $dcc1rec->verification_platform( '-999' );
        $dcc1rec->verification_platform( '-999' );
        #$dcc1rec->uri( '-999' );
        #$dcc1rec->db_xref( '-999' );
        $dcc1rec->note( '-999' );
        
        $dcc1rec->Ref( $rec->ref_allele );
        $dcc1rec->Alt( $rec->alt_allele );
        $dcc1rec->Info( $rec->info );

        $dcc1->write_record( $dcc1rec );
    }

    $vcf->close;
    $dcc1->close;
}


sub _make_indel_dcc1_records {
    my $self    = shift;
    my $ra_recs = shift;

    # We use the ini file params so often, it's simpler to do this
    my $ini = $self->ini;

    # We are going to need to put the filtered SV records somewhere
    my $filtfile = $ini->param('qpindel','outdir') .'/'.
                   $ini->param('qpindel','output') .'_filtered_indel.txt';

    # Write the filtered records to an outfile
    my $outfh = IO::File->new( $filtfile, 'w' );
    croak "Can't open output file $filtfile for writing: $!"
        unless defined $outfh;
    qlogprint( "writing to file: $filtfile\n" ) if $self->verbose;
    $outfh->print( $_->to_text ) foreach (@{ $ra_recs });
    $outfh->close;

    # Run pindel2vcf to create a VCF file
    my $vcffile = $filtfile .'.vcf';

    # During debugging, we can comment this out once it has run the first
    # time and created a VCF.  Even with cut down pindel input files this
    # step is VERY slow because it has to read and substr on the human
    # genome in order to interpet the variations.

    $self->pin->pindel2vcf(
                  infile          => $filtfile,
                  outfile         => $vcffile,
                  reference       => $ini->param('pindel','reference'),
                  reference_label => $ini->param('pindel','reference_label'),
                  reference_date  => $ini->param('pindel','reference_date'),
                  verbose         => $self->verbose );

    # Read VCF and convert to DCC1
    my $vcf = QCMG::IO::VcfReader->new(
                  filename => $vcffile,
                  verbose  => $self->verbose );

    my $dcc1_version = 'dcc1_dbssm_somatic_r' . $self->{release};
    my $dcc1 = QCMG::IO::DccSnpWriter->new(
                   filename => $self->dcc1_indel_file,
                   version  => $dcc1_version,
                   meta     => qexec_header().$self->{'meta_somatic'},
		   annotations => { Organism => $self->{organism}, "QAnnotateMode" => "pindel"},
                   verbose  => $self->verbose );

	qlogprint("Setting dcc1 version: $dcc1_version\n");
	$self->{'somatic_dcc1_version'}	= $dcc1_version;

    my $var_id = 1;  # ctr for use in unique ID assigned to each SV
    while (my $rec = $vcf->next_record) {
        my $info = $rec->info;

        my $dcc1rec = QCMG::IO::DccSnpRecord->new(
                          version  => $dcc1_version,
                          verbose  => $self->verbose );
        
        $dcc1rec->analysis_id( 'JP_sv1_20130111' );
        $dcc1rec->analyzed_sample_id( $ini->param('tumour','label') );
        $dcc1rec->mutation_id( $dcc1rec->analyzed_sample_id .'_ind'. $var_id++ );
        
        # mutation_type contains a code set by the ICGC DCC:
        # 1 = single base substitution 
        # 2 = insertion of <= 200 bp 
        # 3 = deletion of <= 200 bp 
        # 4 = multiple base substitution (>= 2bp and <= 200bp) 

        # This is probably not the correct content for these fields but it'll do for testing
        if ($info =~ /SVTYPE=([\w]+)/) {
            my $vtype = $1;
            if ($vtype eq 'INS') {
                $dcc1rec->mutation_type( 2 );
            }
            elsif ($vtype eq 'DEL') {
                $dcc1rec->mutation_type( 3 );
            }
            elsif ($vtype eq 'RPL') {
                $dcc1rec->mutation_type( 4 );
            }
            else {
                confess "Can not process variant type: $vtype\n";
            }
        }
        else {
            $dcc1rec->mutation_type( '-999' );
        }

        $dcc1rec->chromosome( $rec->chrom );
        $dcc1rec->chromosome_start( $rec->position );
        # !!! For the moment we'll say all variants are on plus strand
        $dcc1rec->chromosome_strand( 1 );
        if ($info =~ /END=(\w+)/) {
            $dcc1rec->chromosome_end( $1 );
        }
        else {
            $dcc1rec->chr_to_bkpt( '-999' );
        }

        $dcc1rec->refsnp_allele( '-999' );
        $dcc1rec->refsnp_strand( '-999' );
        $dcc1rec->reference_genome_allele( $rec->ref_allele );
        $dcc1rec->control_genotype( '-999' );
        $dcc1rec->tumour_genotype( $rec->alt_allele );
        $dcc1rec->mutation( $rec->ref_allele .'/'. $rec->alt_allele );
        #$dcc1rec->mutation( $rec->ref_allele .'>'. $rec->alt_allele );
        $dcc1rec->quality_score( '-999' );
        $dcc1rec->probability( '-999' );
        $dcc1rec->read_count( '-999' );
        $dcc1rec->is_annotated( '-999' );
        #$dcc1rec->validation_status( '-999' );
        #$dcc1rec->validation_platform( '-999' );
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

	# transform coordinates, alleles, and mutation to comply
	# with DCC format
	$dcc1rec	= $self->_unify_coordinates($dcc1rec, 'somatic');

        $dcc1->write_record( $dcc1rec );
    }

    $vcf->close;
    $dcc1->close;

	return();
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

	my $cmd	= qq{qmule org.qcmg.qmule.IndelDCCHeader --input }.$self->dcc1_indel_file.qq{ --input }.$self->dcc1_germline_indel_file.qq{ --tumour }.$self->tumourbam.qq{ --normal }.$self->normalbam.qq{ --output }.$fixed_dcc1.qq{ --output }.$gfixed_dcc1.qq{ --mode pindel -log  }.$fixed_dcc1_log; 
	qlogprint("\t$cmd\n");
	my $rv	= system($cmd);

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

=cut
#Q_DCCMETA      analysisId      d9e3f868_9a9b_4cf6_88a7_f7688bda2c49
#Q_DCCMETA      analyzed_sample_id      ICGC-ABMB-20121116-052
#Q_DCCMETA      matched_sample_id       ICGC-ABMB-20121116-056
=cut
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


sub _make_germline_indel_dcc1_records {
    my $self    = shift;
    my $ra_recs = shift;

    # We use the ini file params so often, it's simpler to do this
    my $ini = $self->ini;

    # We are going to need to put the filtered SV records somewhere
    my $filtfile = $ini->param('qpindel','outdir') .'/'.
                   $ini->param('qpindel','output') .'_filtered_indel_germline.txt';


    # Write the filtered records to an outfile
    my $outfh = IO::File->new( $filtfile, 'w' );
    croak "Can't open output file $filtfile for writing: $!"
        unless defined $outfh;
    qlogprint( "writing to file: $filtfile\n" ) if $self->verbose;
    $outfh->print( $_->to_text ) foreach (@{ $ra_recs });
    $outfh->close;

    # Run pindel2vcf to create a VCF file
    my $vcffile = $filtfile .'.vcf';

    # During debugging, we can comment this out once it has run the first
    # time and created a VCF.  Even with cut down pindel input files this
    # step is VERY slow because it has to read and substr on the human
    # genome in order to interpet the variations.

    $self->pin->pindel2vcf(
                  infile          => $filtfile,
                  outfile         => $vcffile,
                  reference       => $ini->param('pindel','reference'),
                  reference_label => $ini->param('pindel','reference_label'),
                  reference_date  => $ini->param('pindel','reference_date'),
                  verbose         => $self->verbose );

    # Read VCF and convert to DCC1
    my $vcf = QCMG::IO::VcfReader->new(
                  filename => $vcffile,
                  verbose  => $self->verbose );

	qlogprint("Writing germline indels to ".$self->dcc1_germline_indel_file."\n");

    #my $dcc1_version = 'dcc1_dbssm_somatic_r' . $self->{release};
    my $dcc1_version = 'dcc1_dbsgm_germline_r' . $self->{release};
    my $dcc1 = QCMG::IO::DccSnpWriter->new(
                   filename => $self->dcc1_germline_indel_file,
                   version  => $dcc1_version,
                   meta     => qexec_header().$self->{'meta_germline'},
		   annotations => { Organism => $self->{organism}, "QAnnotateMode" => "pindel"},
                   verbose  => $self->verbose );

	$self->{'germline_dcc1_version'}	= $dcc1_version;

    my $var_id = 1;  # ctr for use in unique ID assigned to each SV
    while (my $rec = $vcf->next_record) {
        my $info = $rec->info;

        my $dcc1rec = QCMG::IO::DccSnpRecord->new(
                          version  => $dcc1_version,
                          verbose  => $self->verbose );
        
        $dcc1rec->analysis_id( 'JP_sv1_20130111' );
        #$dcc1rec->control_sample_id( $ini->param('normal','label') );
        $dcc1rec->analyzed_sample_id( $ini->param('normal','label') );
        #$dcc1rec->variation_id( $dcc1rec->control_sample_id .'_ind'. $var_id++ );
        #$dcc1rec->variation_id( $dcc1rec->analyzed_sample_id .'_ind'. $var_id++ );
        $dcc1rec->variant_id( $dcc1rec->analyzed_sample_id .'_ind'. $var_id++ );
        
        # mutation_type contains a code set by the ICGC DCC:
        # 1 = single base substitution 
        # 2 = insertion of <= 200 bp 
        # 3 = deletion of <= 200 bp 
        # 4 = multiple base substitution (>= 2bp and <= 200bp) 

        # This is probably not the correct content for these fields but it'll do for testing
        if ($info =~ /SVTYPE=([\w]+)/) {
            my $vtype = $1;
            if ($vtype eq 'INS') {
                $dcc1rec->variant_type( 2 );
            }
            elsif ($vtype eq 'DEL') {
                $dcc1rec->variant_type( 3 );
            }
            elsif ($vtype eq 'RPL') {
                $dcc1rec->variant_type( 4 );
            }
            else {
                confess "Can not process variant type: $vtype\n";
            }
        }
        else {
            $dcc1rec->mutation_type( '-999' );
        }

        $dcc1rec->chromosome( $rec->chrom );
        $dcc1rec->chromosome_start( $rec->position );
        # !!! For the moment we'll say all variants are on plus strand
        $dcc1rec->chromosome_strand( 1 );
        if ($info =~ /END=(\w+)/) {
            $dcc1rec->chromosome_end( $1 );
        }
        else {
            $dcc1rec->chr_to_bkpt( '-999' );
        }
=cut
    my @v_dcc1_dbssm_somatic_r11 =
        qw( analysis_id analyzed_sample_id mutation_id mutation_type
            chromosome chromosome_start chromosome_end
            chromosome_strand refsnp_allele refsnp_strand reference_genome_allele
            control_genotype tumour_genotype mutation expressed_allele quality_score probability
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
=cut

        $dcc1rec->refsnp_allele( '-999' );
        $dcc1rec->refsnp_strand( '-999' );
        $dcc1rec->reference_genome_allele( $rec->ref_allele );
        $dcc1rec->control_genotype( '-999' );
        $dcc1rec->tumour_genotype( $rec->alt_allele );
        #$dcc1rec->mutation( $rec->ref_allele .'>'. $rec->alt_allele );
        $dcc1rec->quality_score( '-999' );
        $dcc1rec->probability( '-999' );
        $dcc1rec->read_count( '-999' );
        $dcc1rec->is_annotated( '-999' );
        #$dcc1rec->validation_status( '-999' );
        #$dcc1rec->validation_platform( '-999' );
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
        #$dcc1rec->Mutation( $rec->ref_allele .'>'. $rec->alt_allele );
	$dcc1rec->Mutation( $rec->ref_allele .'/'. $rec->alt_allele );
	# transform coordinates, alleles, and mutation to comply
	# with DCC format
	$dcc1rec	= $self->_unify_coordinates($dcc1rec, 'germline');

        $dcc1->write_record( $dcc1rec );
    }

    # save the highest germline variant_id number for use later
    $self->{'last_germline_var_id'}	= $var_id;

    $vcf->close;
    $dcc1->close;

=cut
	qlogprint("Fixing germline DCC1 headers:  ".$self->dcc1_germline_indel_file."\n");

	my $origfile		= $self->dcc1_germline_indel_file.".orig";
	my $fixed_dcc1		= $self->dcc1_germline_indel_file.".fixedheader";
	my $fixed_dcc1_log	= $self->dcc1_germline_indel_file.".fixedheader.log";

	my $cmd	= qq{qmule org.qcmg.qmule.IndelDCCHeader --input }.$self->dcc1_germline_indel_file.qq{ --tumour }.$self->tumourbam.qq{ --normal }.$self->normalbam.qq{ --output }.$fixed_dcc1.qq{ --mode gatk -log  }.$fixed_dcc1_log; 
	my $rv	= system($cmd);

	# move files around; replace original DCC1 with re-headered DCC1
	# file and save original file 
	$cmd		= qq{cp }.$self->dcc1_germline_indel_file.qq{ $origfile};
	qlogprint("$cmd\n");
	$rv		= system($cmd) if($rv == 0);
	$cmd		= qq{cp $fixed_dcc1 }.$self->dcc1_germline_indel_file;
	qlogprint("$cmd\n");
	system($cmd) if($rv == 0);
=cut

	return();
}

sub _unify_coordinates {
	my $self	= shift;
	my $rec		= shift;	# DCC1 record
	my $rectype	= shift;	# "somatic" or "germline"

	unless($rec->chromosome =~ /\w/ && $rec->chromosome_start =~ /\d/) {
		# if the coordinates cannot be changed, just return the
		# unchanged record
		qlogprint("WARNING: Invalid DCC1 record - cannot unify coordinates\n");
		return($rec);
	}

	# get tool-specific genomic coordinates and alleles and alter them so
	# they conform to the DCC standard
	my $chr		= $rec->chromosome;
	my $s		= $rec->chromosome_start;
	my $e		= $rec->chromosome_end;
	my $mut_type;
	my $mut;
	if($rectype eq 'somatic') {
		$mut_type	= $rec->mutation_type;
		$mut		= $rec->mutation;
	}
	elsif($rectype eq 'germline') {
		$mut_type	= $rec->variant_type;
		$mut		= $rec->Mutation;
	}

	my $ref		= $rec->reference_genome_allele;
	my $alt		= $rec->tumour_genotype;

	# GATK deletion
	if($mut_type == 3) {
		$s	+= 1;
		$e	-= 1;
		# remove first base of $alt
		$ref	=~ s/^.//;
		my $alt	= '-' x length($ref);
		$mut	= join "/", $ref, $alt;
		#$mut	= join ">", $ref, $alt;

		#print STDERR "DCC1\t$mut_type: $ref -> $alt\t$mut\n";
		$rec->chromosome_start($s);
		$rec->chromosome_end($e);
		$rec->reference_genome_allele($ref);
		$rec->tumour_genotype($alt);
		if($rectype eq 'somatic') {
			$rec->mutation($mut);
		}
		elsif($rectype eq 'germline') {
			$rec->Mutation($mut);
		}
	}
	# GATK insertion
	elsif($mut_type == 2) {
		# remove first base of $alt
		$alt	=~ s/^.//;
		$ref	= '-' x length($alt);
		$mut	= join "/", $ref, $alt;
		#$mut	= join ">", $ref, $alt;

		#print STDERR "DCC1\t$mut_type: $ref -> $alt\t$mut\n";
		$rec->reference_genome_allele($ref);
		$rec->tumour_genotype($alt);
		if($rectype eq 'somatic') {
			$rec->mutation($mut);
		}
		elsif($rectype eq 'germline') {
			$rec->Mutation($mut);
		}
	}

	return($rec);
}

sub _std_pindel_indel_filenames {
    my $self = shift;
    my $dir  = shift;
    my $stem = shift;
    return $self->_std_pindel_filenames( $dir, $stem, [qw(_D _SI _LI)] );
}


sub _std_pindel_sv_filenames {
    my $self = shift;
    my $dir  = shift;
    my $stem = shift;
    return $self->_std_pindel_filenames( $dir, $stem, [qw(_INV _TD)] );
}


sub _std_pindel_filenames {
    my $self    = shift;
    my $dir     = shift;
    my $stem    = shift;
    my $ra_exts = shift;

    my %infiles = ();
    foreach my $ext (@{$ra_exts}) {
        my $infile = $dir .'/'. $stem . $ext;
        if (-r $infile) {
            $infiles{ $infile }++;
            # We could print out whether or not we'd already seen this
            # file but we are not currently doing this
            qlogprint( "found file: $infile\n" ) if $self->verbose;
        }
        else {
            qlogprint( "could not find file: $infile\n" );
		die;
        };
    }

    my @files = keys %infiles;

    return \@files;
}


sub _do_filter {
    my $self          = shift;
    my $ra_infiles    = shift;
    my $ra_limits     = shift;
    my $tumour_label  = shift;
    my $normal_label  = shift;

    # Start compiling the list of files to be processed
    my @infiles = @{ $ra_infiles };

    my %legal_comps = ( '<' => 1,
                        '>' => 1, 
                        '=' => 1, 
                        '<=' => 1, 
                        '>=' => 1 );
    my %legal_types = ( 'TUMOUR'        => 1,
                        'TUMOUR_UNIQUE' => 1,
                        'NORMAL'        => 1,
                        'NORMAL_UNIQUE' => 1 );

    # Test data
#    my @limits = ( 'TUMOUR_UNIQUE > 4',
#                   'TUMOUR_UNIQUE < 1000',
#                   'NORMAL_UNIQUE < 1',
#                   'NORMAL < 2' );
#    my $tumour_label = 'APGI_2353_HiSeq_genome_TD';
#    my $normal_label = 'APGI_2353_HiSeq_genome_ND';
    
    my @recs = ();		# somatic indels
    my @germline_recs = ();	# "germline" indels
    my $fctr = 0;
    foreach my $infile (sort @infiles) {
        qlogprint( "processing file $infile\n" ) if $self->verbose;
	print STDERR "processing file $infile\n";

        my $prr = QCMG::IO::PindelReader->new( filename => $infile,
                                               verbose  => $self->verbose );
        my $passed_ctr = 0;
        my $tumour_label_index = undef;
        my $normal_label_index = undef;
        while (my $rec = $prr->next_record()) {
		print STDERR "PINDEL LABEL $tumour_label_index vs INI LABEL $tumour_label\n";

            # Using first record, we need to set the indexes for tumour
            # and normal files based on the labels.

            if (! defined $tumour_label_index and ! defined $normal_label_index) {
                my @files = $rec->files;
                foreach my $ctr (0..$#files) {
                    $tumour_label_index = $ctr
                        if ($files[$ctr]->{SampleName} eq $tumour_label);
                    $normal_label_index = $ctr
                        if ($files[$ctr]->{SampleName} eq $normal_label);
                }

                # If we get here and we don't have indexes for tumour
                # and normal then we have a problem and cannot continue

                die "Unable to match tumour label from INI file to pindel file\n"
                    unless (defined $tumour_label_index);
                die "Unable to match normal label from INI file to pindel file\n"
                    unless (defined $normal_label_index);
            }

            # Should we be looking at File9UpstreamReadsCount or
            # File9UpstreamUniqueReadsCount ?  The following example
            # shows why it makes a difference:
            #
            # 412095  I 1 NT 1 "C"    ChrID chrX  BP 152864477    152864478
            # BP_range  152864477    152864481   Supports 5  4   + 4 3   - 1 1
            # S1 10   SUM_MS   300 2   NumSupSamples 2 1
            # APGI_2353_HiSeq_genome_ND 1 0 0 0
            # APGI_2353_HiSeq_genome_XD 3 3 1 1
            # 
            # If we look at the last 2 lines we see that the ND has 1
            # upstream read but 0 unique upstream reads so depending on
            # which count you chose it does or doesn't have reads in the
            # normal with evidence for the variant - this can cause us
            # to mislabel a germline as somatic - so maybe we should use
            # the read count, not unique read count.  BUT, the XD file
            # shows 3 upstream and 3 downstread reads but only 1 of each
            # is unique so there is a lot more evidence in the
            # non-unique reads (duplicates?).  Overall, if we use reads
            # count we actually get more somatic calls made because more
            # TD samples have enough evidence to trigger calling.

            ### What do we do ? ###

            my $failed = 0;
            foreach my $limit (@{ $ra_limits }) {
                if ($limit =~ /^\s*([A-Z_]+)\s*([<>=]{1,2})\s*(\d+)\s*$/) {
                    my $type=$1;
                    my $comp=$2;
                    my $value=$3;

                    # Make sure that all the values are legit
                    die "Unable to parse limit: [$limit]\n"
                       unless (defined $type and
                               defined $comp and
                               defined $value and
                               exists $legal_types{ $type } and
                               exists $legal_comps{ $comp });

                    # Work out which type of count the limit applies to
                    # and set our read_count appropriately.
                    my $read_count = undef;
                    if ($rec->VarType eq 'LI') {
			#print STDERR "VarType eq LI\n";
                        if ($type eq 'TUMOUR') {
				#print STDERR "TYPE eq TUMOUR\n";
                            $read_count = $rec->file($tumour_label_index)->{LeftReadsCount} +
                                          $rec->file($tumour_label_index)->{RightReadsCount};
                        }
                        elsif ($type eq 'NORMAL') {
				#print STDERR "TYPE eq NORMAL\n";
                            $read_count = $rec->file($normal_label_index)->{LeftReadsCount} +
                                          $rec->file($normal_label_index)->{RightReadsCount};
                        }
                        else {
                            # skip to next limit if any
                            next;
                        }
                    }
                    else {
			#print STDERR "VarType ne LI\n";
                        if ($type eq 'TUMOUR_UNIQUE') {
				#print STDERR "TYPE eq TUMOUR_UNIQUE\n";
				#print STDERR "tumour_label_index: $tumour_label_index\n";
				#print STDERR "Upstream Uniq Reads Count: ".$rec->file( $tumour_label_index )->{UpstreamUniqueReadsCount}."\n";
				#print STDERR "Downstream Uniq Reads Count: ".$rec->file( $tumour_label_index )->{DownstreamUniqueReadsCount}."\n";

				next if(! defined $rec->file( $tumour_label_index));

                            	$read_count = $rec->file( $tumour_label_index )->{UpstreamUniqueReadsCount} +
                                          $rec->file( $tumour_label_index )->{DownstreamUniqueReadsCount};
                        }
                        elsif ($type eq 'TUMOUR') {
				#print STDERR "TYPE eq NORMAL\n";
				next if(! defined $rec->file( $tumour_label_index ));

                            $read_count = $rec->file( $tumour_label_index )->{UpstreamReadsCount} +
                                          $rec->file( $tumour_label_index )->{DownstreamReadsCount};
                        }
                        elsif ($type eq 'NORMAL_UNIQUE') {
				#print STDERR "TYPE eq NORMAL_UNIQUE\n";

				next if(! defined $rec->file($normal_label_index));

                            $read_count = $rec->file( $normal_label_index )->{UpstreamUniqueReadsCount} +
                                          $rec->file( $normal_label_index )->{DownstreamUniqueReadsCount};
                        }
                        elsif ($type eq 'NORMAL') {
				#print STDERR "TYPE eq NORMAL\n";
				next if(! defined $rec->file( $normal_label_index ) );

                            $read_count = $rec->file( $normal_label_index )->{UpstreamReadsCount} +
                                          $rec->file( $normal_label_index )->{DownstreamReadsCount};
                        }
                    }

                    # Now we build the expression and evaluate it
                    my $expression = "$read_count $comp $value";
                    my $success = eval $expression; warn $@ if $@;
                    $failed = 1 unless $success;
                    #print Dumper "expr: $expression  success: $success  failed: $failed"; 
                }
                else {
                    die "Unable to process limit: [$limit]\n";
                }
            }

            $fctr++;
            if ($self->verbose and $fctr % 100000 == 0) {
                 qlogprint "  processed $fctr records (".scalar(@recs)." passed)\n";
            }

            # If we didn't fail any criteria then this record is a
            # keeper so now we have to decide what type of variant it
            # counts as - SSM or SV.  For ICGC DCC, it's an SSM if it
            # fits any of these categories:
            # 1 = single base substitution 
            # 2 = insertion of <= 200 bp 
            # 3 = deletion of <= 200 bp 
            # 4 = multiple base substitution (>= 2bp and <= 200bp) 

            if (! $failed) {
                $passed_ctr++;
                push @recs, $rec;
#                $outfh->print( $rec->to_text );
            }
### NEW FOR GERMLINE
            else {
                # keep germline mutations
                push @germline_recs, $rec;
            }
### ###############
        }

	print STDERR "done processing file\n";

        qlogprint( "  $passed_ctr of ". $prr->record_ctr() .
                   " records passed filter from file $infile\n");
    }
#    $outfh->close;

	print STDERR "done processing all pindel input files\n";

    qlogprint( scalar(@recs). " records passed filters\n");
    qlogprint( scalar(@germline_recs). " records are considered germline mutations\n");

    #return \@recs;
    return (\@recs, \@germline_recs);
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

	#my $qag	= QCMG::Annotate::Gff->new(	infile	=> $self->dcc1_indel_file,
	#					dcc1out	=> $outfile,
	#					gfffile	=> $self->repeats
	#				);
	#$qag->execute;

	my $cmd	= qq{qmule org.qcmg.qmule.AnnotateDCCWithGFFRegions --input }.$self->dcc1_indel_file.qq{ --input }.$self->repeats.qq{ --output $outfile --log $outfile.log};
	qlogprint($cmd."\n");
	system($cmd);

	# move files around; replace unannotated dcc1 file with repeatmasked
	# file and save original file 
	$cmd		= qq{cp }.$self->dcc1_indel_file.qq{ $origfile};
	qlogprint("$cmd\n");
	my $rv		= system($cmd);
	$cmd		= qq{cp $outfile }.$self->dcc1_indel_file if($rv == 0);
	qlogprint("$cmd\n");
	system($cmd);

	return;
}

# determine if mutation resides in known repeat regions
sub _annotate_germline_repeats {
    my $self = shift;

	# if no repeatmasked genome file is provided, don't do anything
	if(! defined $self->repeats) {
		qlogprint("Skipping repeat annotation, no file provided\n");
		return;
	}

    # if repeatmasked genome file is provided, edit the DCC1 file and append
    # repeat information to the QCMGflag field
	my $origfile	= $self->dcc1_germline_indel_file.".unannotated";
	my $outfile	= $self->dcc1_germline_indel_file.".repeatmasked";

	qlogprint("Annotating repeats with ".$self->repeats."\n");

	#my $qag	= QCMG::Annotate::Gff->new(	infile	=> $self->dcc1_germline_indel_file,
	#					dcc1out	=> $outfile,
	#					gfffile	=> $self->repeats
	#				);
	#$qag->execute;

	my $cmd	= qq{qmule org.qcmg.qmule.AnnotateDCCWithGFFRegions --input }.$self->dcc1_germline_indel_file.qq{ --input }.$self->repeats.qq{ --output $outfile --log $outfile.log};
	qlogprint($cmd."\n");
	system($cmd);

	# move files around; replace unannotated dcc1 file with repeatmasked
	# file and save original file 
	$cmd		= qq{cp }.$self->dcc1_germline_indel_file.qq{ $origfile};
	qlogprint("$cmd\n");
	my $rv		= system($cmd);
	$cmd		= qq{cp $outfile }.$self->dcc1_germline_indel_file if($rv == 0);
	qlogprint("$cmd\n");
	system($cmd);

	return;
}


# filter indels and add info to ND/TD fields
sub _filter_indels {
	my $self = shift;
	
	my $outfile	= $self->dcc1_indel_file.".filtered";

=cut
qbasepileup 
-t 6 
-m indel 
--it /mnt/seq_results/smgres_gemm/GEMM_0208/seq_final/SmgresGemm_GEMM0208_1DNA_9CellLineDerivedFromTumour_ICGCABMJ2011071415CD_IlluminaTruSEQPCRFreeMultiplexedManual_NoCapture_Bwa_HiSeq.jpearson.bam 
--in /mnt/seq_results/smgres_gemm/GEMM_0208/seq_final/SmgresGemm_GEMM0208_1DNA_4NormalControlOtherSite_ICGCABMJ2011071416ND_IlluminaTruSEQPCRFreeMultiplexedManual_NoCapture_Bwa_HiSeq.jpearson.bam 
-is  /mnt/seq_results/smgres_gemm/GEMM_0208/variants/pindel/4NormalControlOtherSite_vs_9CellLineDerivedFromTumour_NoCapture_HiSeq/GEMM_0208_4NormalControlOtherSite_vs_9CellLineDerivedFromTumour_indel.dcc1 
-os  /panfs/home/lfink/projects/indel_pileup/pindel/GEMM_0208/output.somatic.dcc1 
-ig  /mnt/seq_results/smgres_gemm/GEMM_0208/variants/pindel/4NormalControlOtherSite_vs_9CellLineDerivedFromTumour_NoCapture_HiSeq/GEMM_0208_4NormalControlOtherSite_vs_9CellLineDerivedFromTumour_indel_germline.dcc1 
-og  /panfs/home/lfink/projects/indel_pileup/pindel/GEMM_0208/output.germline.dcc1 
--log /panfs/home/lfink/projects/indel_pileup/pindel/GEMM_0208/test.log 
-r  /panfs/share/genomes/lifescope/referenceData/internal/qcmg_mm64/reference/Mus_musculus.NCBIM37.64.ALL_validated.E.fa
--pindel
=cut
	
	#my $cmd		= $ENV{'QCMG_SVN'}.qq{/QCMGScripts/l.fink/indel_pileup/indel_pileup_dcc1.pl };
	my $cmd		.= qq{qbasepileup -t 4 --pindel };
	$cmd		.= qq{ --it }.$self->tumourbam;
	$cmd		.= qq{ --in }.$self->normalbam;
	$cmd		.= qq{ -r  }.$self->genome;
	$cmd		.= qq{ -is }.$self->dcc1_indel_file;
	$cmd		.= qq{ -os }.$outfile;
	$cmd		.= qq{ -m indel};
	#$cmd		.= qq{ -dcc1v }.$self->{'somatic_dcc1_version'};
	qlogprint("$cmd\n");
	my $rv		= system($cmd);
	
	$cmd		= qq{cp $outfile }.$self->dcc1_indel_file;
	qlogprint("$cmd\n");
	#print STDERR "$cmd\n";
	#print STDERR "DCC VERSION: ".$self->{dcc1_version}."\n";
	system($cmd) if($rv == 0);

	return;
}

# filter germline indels and add info to ND/TD fields
sub _filter_germline_indels {
	my $self = shift;
	
	my $outfile	= $self->dcc1_germline_indel_file.".filtered";

	my $cmd		.= qq{qbasepileup -t 4 --pindel };
	$cmd		.= qq{ --it }.$self->tumourbam;
	$cmd		.= qq{ --in }.$self->normalbam;
	$cmd		.= qq{ -r  }.$self->genome;
	$cmd		.= qq{ -ig }.$self->dcc1_germline_indel_file;
	$cmd		.= qq{ -og }.$outfile;
	$cmd		.= qq{ -m indel};
	qlogprint("$cmd\n");
	my $rv		= system($cmd);
	
	$cmd		= qq{cp $outfile }.$self->dcc1_germline_indel_file;
	qlogprint("$cmd\n");
	system($cmd) if($rv == 0);

	return;
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
		   		annotations => { Organism => $self->{organism}, "QAnnotateMode" => "pindel"}

				);

	# create germline output file in same DCC1 format
	my $dcc1_g_version = 'dcc1_dbsgm_germline_r' . $self->{release};
	my $g_dcc1out	= QCMG::IO::DccSnpWriter->new(
				filename	=> $g_outfile,
				version		=> $dcc1_g_version,
				verbose		=> $self->{verbose},
	               		meta     	=> qexec_header().$self->{'meta_germline'},
			        annotations => { Organism => $self->{organism}, "QAnnotateMode" => "pindel"}

				);


	my $count_new_germline	= 0;
	my $count_new_somatic	= 0;
	my $count_total		= 0;
	my $lowsupcount		= 0;
	my $nnscount		= 0;
	my @new_germline_recs	= ();
	while(my $rec	= $dcc1->next_record) {
		my $nd		= $rec->ND;

		if($nd	!~ /[\d;]+/) {
			die "ND field in ".$self->dcc1_indel_file." missing\n";
		}

		#my ($nnovel_starts, $nfall, $nfinf, $nfpres, $npartial_indel, $nindel_near, $nsoftclip_near)		= split ";", $nd;
		#my ($tnovel_starts, $tfall, $tfinf, $tfpres, $tpartial_indel, $tindel_near, $tsoftclip_near, $thp)	= split ";", $td;
		# thp: ;"0 discontiguous CAGAATG__ATATATATATATATATATATATATATATATATATATATATATGCAAAATTTAAAATTAGAGTATTTCATATGAGAAGATGAGTAATTTAAATAA"

		my ($nns, $nt, $ni, $nsi)	= split /;|\[/, $nd;
		print STDERR "$nd\n$nns, $nt, $ni, $nsi\n";
		my $lowsupperc			= 1;	# default
		if(defined $ni && $ni > 0) {
			$lowsupperc		= $nsi / $ni;
		}
		print STDERR "LS%: $lowsupperc\n";

		if($lowsupperc > 0.10) {
			$rec->analyzed_sample_id( $self->{'matched_sample_id'} );
			# we call this as mutation_id because it is in a somatic
			# file; it will become variant_id in the germline
			#$rec->mutation_id( $self->{'analysis_id'}."_".$self->{'matched_sample_id'}."_ind".$self->{'last_germline_var_id'}++ );
			$rec->mutation_id( $self->{'analysis_id'}."_ind".$self->{'last_germline_var_id'}++ );
			push @new_germline_recs, $rec;

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
			#$rec->mutation_id( $self->{'analysis_id'}."_".$self->{'matched_sample_id'}."_ind".$self->{'last_germline_var_id'}++ );
			$rec->mutation_id( $self->{'analysis_id'}."_ind".$self->{'last_germline_var_id'}++ );
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
	qlogprint("\tKept\t\t$count_new_somatic\tDCC1 records as somatic\n");
	qlogprint("\tReads with low allele frequency support\t$lowsupcount\n");
	qlogprint("\tReads with more than 3 novel start reads in normal\t$nnscount\n");

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
		   		annotations => { Organism => $self->{organism}, "QAnnotateMode" => "pindel"}
				);

	# create remaining germline output file in same DCC1 format
	my $gk_dcc1out	= QCMG::IO::DccSnpWriter->new(
				filename	=> $gk_outfile,
				version		=> $dcc1->version,
				verbose		=> $self->{verbose},
	               		meta     => qexec_header().$self->{'meta_somatic'},
		   		annotations => { Organism => $self->{organism}, "QAnnotateMode" => "pindel"}
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

                        # some recs exist like: 49;180;168;63[46|17];0;0;1
                        $nsi =~ s/\[.*//;

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
		#$variant_id	= join "_". $self->{'analysis_id'}, $self->{'matched_sample_id'}, "ind".$1;
		$variant_id	= join "_". $self->{'analysis_id'}, "ind".$1;

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

sub _annotate_indel {
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
            #$mutation = '->'. $rec->tumour_genotype;
        }
        elsif ($rec->mutation_type == 3) {
            $mutation = $rec->reference_genome_allele . '/-';
            #$mutation = $rec->reference_genome_allele . '>-';
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
	qlogprint("Writing somatic DCC2 file:\t".$self->dcc2_indel_file."\n");

    my $dcc2 = QCMG::IO::DccSnpWriter->new(
                   filename => $self->dcc2_indel_file,
                   version  => $dcc2_version,
                   meta     => qexec_header().$self->{'meta_somatic'},
		   annotations => { Organism => $self->{organism}, "QAnnotateMode" => "pindel"},
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

	qlogprint("Writing somatic DCCQ file:\t".$self->dccq_indel_file."\n");

    my $dccq = QCMG::IO::DccSnpWriter->new(
                   filename => $self->dccq_indel_file,
                   version  => $dccq_version,
                   meta     => qexec_header().$self->{'meta_somatic'},
		   annotations => { Organism => $self->{organism}, "QAnnotateMode" => "pindel"},
                   verbose  => $self->verbose );

    foreach my $rec (@ens_recs) {
        my $dccqrec = $rec->as_dccq_record;
        $dccq->write_record( $dccqrec );
    }

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

sub _annotate_germline_indel {
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
            #$mutation = '->'. $rec->tumour_genotype;
        }
        elsif ($rec->variant_type == 3) {
            $mutation = $rec->reference_genome_allele . '/-';
            #$mutation = $rec->reference_genome_allele . '>-';
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

	qlogprint("Writing germline DCC2 file:\t".$self->dcc2_germline_indel_file."\n");

    my $dcc2 = QCMG::IO::DccSnpWriter->new(
                   filename => $self->dcc2_germline_indel_file,
                   version  => $dcc2_version,
                   meta     => qexec_header().$self->{'meta_germline'},
		   annotations => { Organism => $self->{organism}, "QAnnotateMode" => "pindel"},
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

	qlogprint("Writing germline DCCQ file:\t".$self->dccq_germline_indel_file."\n");

    my $dccq = QCMG::IO::DccSnpWriter->new(
                   filename => $self->dccq_germline_indel_file,
                   version  => $dccq_version,
                   meta     => qexec_header().$self->{'meta_germline'},
		   annotations => { Organism => $self->{organism}, "QAnnotateMode" => "pindel"},
                   verbose  => $self->verbose );

    foreach my $rec (@ens_recs) {
        my $dccqrec = $rec->as_dccq_record;
        $dccq->write_record( $dccqrec );
    }

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

sub _annotate_sv {
    my $self = shift;

    # Because there are so many types of DCC1 file, we are going to let
    # the DccSnpReader guess the version rather than trying to specify it
    
    my $dcc1 = QCMG::IO::DccSnpReader->new(
                   filename => $self->dcc1_sv_file,
                   verbose  => $self->verbose );
    
    # Die if the file is not a DCC1 variant or if we can't determine 
    # somatic/germline from file type

    die "input file must be a DCC1 variant but detected file type is [",
        $dcc1->version, "]\n" unless ($dcc1->dcc_type eq 'dcc1');
    die "cannot parse type (somatic/germline) from DCC1 file type [",
        $dcc1->version, "]\n" unless (defined $dcc1->variant_type);

    # Read in the variant positions from the DCC1
    my ($ra_targets, $ra_skips) = read_and_filter_dcc1_sv_records( $dcc1 );

    my @ens_recs = ();
    foreach my $rec (@{ $ra_targets }) {

        my $mutation = mutation_pindel( $rec->variant_type,
                                        $rec->Ref,
                                        $rec->Alt );

        # Create an annotatable record for the DCC1 record
        push @ens_recs,
             QCMG::Annotate::EnsemblConsequenceRecord->new(
                 chrom       => $rec->chr_from,
                 chrom_start => $rec->chr_from_bkpt,
                 chrom_end   => $rec->chr_to_bkpt,
                 mutation    => $mutation,
                 var_name    => $rec->sv_id,
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

    # In the interests of speed, we'll just pull a handful of record for
    # actual annotation during testing
    #my @recs = @ens_recs[30..45];
    #my @recs = @ens_recs[0..45];

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
                   filename => $self->dcc2_sv_file,
                   version  => $dcc2_version,
                   meta     => qexec_header().$self->{'meta_somatic'},
		   annotations => { Organism => $self->{organism}, "QAnnotateMode" => "pindel"},
                   verbose  => $self->verbose );
    
    foreach my $rec (@ens_recs) {
        my $ra_dcc2recs = $rec->as_dcc2_records;

        #qlogprint( $rec->dcc1->sv_id,  
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

#    my $dccq = QCMG::IO::DccSnpWriter->new(
#                   filename => $self->dccqfile,
#                   version  => $dccq_version,
#                   verbose  => $self->verbose );
#
#    foreach my $rec (@ens_recs) {
#        my $dccqrec = $rec->as_dccq_record;
#        $dccq->write_record( $dccqrec );
#    }

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

QCMG::Annotate::Pindel - Annotate pindel output files


=head1 SYNOPSIS

 use QCMG::Annotate::Pindel;

my %defaults = ( ensver   => '70',
                 organism => 'human',
                 release  => '14',
                 verbose  => 0 );
 my $qsnp = QCMG::Annotate::Pindel->new( %defaults );
 $qsnp->execute;


=head1 DESCRIPTION

This module provides functionality for annotating pindel output.  It
includes commandline parameter processing.

This module is based on a series of SNP annotation scripts originally
created by Karin Kassahn and JP and elaborated by Karin to adapt to new
organisms (Mouse etc) and new versions for the EnsEMBL API.  This
version has been rewritten by JP for better diagnostics and to support
the annotation of SNPs from different organisms and EnsEMBL versions
within a single executable.

=head2 Commandline parameters

 -i | --infile        Input file in DCC1 format
 -o | --dcc2out       Output file in DCC2 format
 -q | --dccqout       Output file in DCCQ format
 -l | --logfile       log file (optional)
 -g | --organism      Organism; default=human
 -e | --ensver        EnsEMBL version; default=70
 -r | --release       DCC release; default=14
 -d | --domains       Ensembl domains file
 -y | --symbols       Ensembl gene ids and symbols file
 -v | --verbose       print progress and diagnostic messages
      --version       print version number and exit immediately
 -h | -? | --help     print help message


=over

=item B<-i | --infile>

Mandatory. The input file is in pindel output format (see QCMG wiki)

=item B<-o | --dcc2out>

Mandatory. The DCC2 format output file (see QCMG wiki).

=item B<-q | --dccqout>

Mandatory. The DCCQ format output file (see QCMG wiki).

=item B<-l | --logfile>

Optional log file name.  If this option is not specified then logging
goes to STDOUT.

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

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=item Karin Kassahn, L<mailto:k.kassahn@uq.edu.au>

=back


=head1 VERSION

$Id$


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
