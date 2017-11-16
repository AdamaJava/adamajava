#!/usr/bin/env perl

##############################################################################
#
#  Program:  jvptools.pl
#  Author:   John V Pearson
#  Created:  2012-05-25
#
#  A collection of perl bits-n-pieces
#
#  $Id: jvptools.pl 8281 2014-06-20 06:32:55Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use JSON;
use Pod::Usage;
use POSIX qw( floor );
use Storable qw(dclone);
use XML::Simple;

use Grz::Util::Log;
use Grz::Util::Util qw( current_user new_timestamp );

use QCMG::DB::Metadata;
use QCMG::FileDir::Finder;
use QCMG::IO::CnvReader;
use QCMG::IO::DccSnpReader;
use QCMG::IO::FastaReader;
use QCMG::IO::FastqReader;
use QCMG::IO::GffReader;
use QCMG::IO::INIFile;
use QCMG::IO::LogFileReader;
use QCMG::IO::MafReader;
use QCMG::IO::MafWriter;
use QCMG::IO::PropertiesReader;
use QCMG::IO::qSnpGermlineReader;
use QCMG::IO::TelomereReport;
use QCMG::IO::TsvReader;
use QCMG::IO::VcfReader;
use QCMG::IO::VerificationReader;
use QCMG::Util::Util qw( ranges_overlap split_final_bam_name );
use QCMG::XML::Qmotif;

use vars qw( $SVNID $REVISION $CMDLINE $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 8281 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: jvptools.pl 8281 2014-06-20 06:32:55Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

# Setup global data structures


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Print usage message if no arguments supplied
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMANDS' )
         unless (scalar @ARGV > 0);
    
    $CMDLINE = join(' ',@ARGV);
    my $mode = shift @ARGV;
    
    my @valid_modes = qw( help man version 
                          search_bam_headers
                          props_2_gff
                          germline_2_qsig
                          qcmgschema_1
                          ega_20121116_a
                          hiseq_motif
                          pindel_2_dcc1
                          cosmic_vs_dbsnp
                          allele_freq
                          wig_2_bed
                          benchmark_wiki2tsv
                          verona1
                          N_count
                          telomere_analysis
                          Nruns_vs_telomere_motifs
                          align_2_csvs
                          wikitable_media2moin
                          illumina_panel_manifest
                          cnv_format_convert
                          fastq_kmer
                          add_entrezgene_to_maf
                          sequence_lengths
                          qprofiler_genome
                          );

    if ($mode =~ /^$valid_modes[0]$/i or $mode =~ /\?/) {
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => 'SYNOPSIS|COMMANDS' );
    }
    elsif ($mode eq $valid_modes[1]) {
        pod2usage(-exitstatus => 0, -verbose => 2)
    }
    elsif ($mode eq $valid_modes[2]) {
        print "$SVNID\n";
    }
    elsif ($mode eq $valid_modes[3]) {
        search_bam_headers();
    }
    elsif ($mode eq $valid_modes[4]) {
        moved_to_qcovtools( 'props_2_gff' );
    }
    elsif ($mode eq $valid_modes[5]) {
        germline_2_qsig();
    }
    elsif ($mode eq $valid_modes[6]) {
        qcmgschema_1();
    }
    elsif ($mode eq $valid_modes[7]) {
        ega_20121116_a();
    }
    elsif ($mode eq $valid_modes[8]) {
        hiseq_motif();
    }
    elsif ($mode eq $valid_modes[9]) {
        pindel_2_dcc1();
    }
    elsif ($mode eq $valid_modes[10]) {
        cosmic_vs_dbsnp();
    }
    elsif ($mode eq $valid_modes[11]) {
        allele_freq();
    }
    elsif ($mode eq $valid_modes[12]) {
        wig_2_bed();
    }
    elsif ($mode eq $valid_modes[13]) {
        benchmark_wiki2tsv();
    }
    elsif ($mode eq $valid_modes[14]) {
        verona1();
    }
    elsif ($mode eq $valid_modes[15]) {
        N_count();
    }
    elsif ($mode eq $valid_modes[16]) {
        telomere_analysis();
    }
    elsif ($mode eq $valid_modes[17]) {
        Nruns_vs_telomere_motifs();
    }
    elsif ($mode eq $valid_modes[18]) {
        align_2_csvs();
    }
    elsif ($mode eq $valid_modes[19]) {
        wikitable_media2moin();
    }
    elsif ($mode eq $valid_modes[20]) {
        moved_to_qcovtools( 'illumina_panel_manifest' );
    }
    elsif ($mode eq $valid_modes[21]) {
        cnv_format_convert();
    }
    elsif ($mode eq $valid_modes[22]) {
        fastq_kmer();
    }
    elsif ($mode eq $valid_modes[23]) {
        add_entrezgene_to_maf();
    }
    elsif ($mode eq $valid_modes[24]) {
        sequence_lengths();
    }
    elsif ($mode eq $valid_modes[25]) {
        qprofiler_genome();
    }
    else {
        die "jvptools mode [$mode] is unrecognised; valid modes are:\n   ".
            join("\n   ",@valid_modes) ."\n";
    }
}


sub moved_to_qcovtools {
    my $mode = shift;
    warn "mode [$mode] has moved to qcovtools.pl\n";
}


sub add_entrezgene_to_maf {
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/add_entrezgene_to_maf' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infile      => '',
                   outfile     => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           => \$params{infile},        # -i
           'e|entrezfile=s'       => \$params{entrezfile},    # -e
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply the following inputs
    die "You must specify an infile\n" unless $params{infile};
    die "You must specify an entrez (gene2accession) file\n" unless $params{entrezfile};
    die "You must specify an outfile\n" unless $params{outfile};

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin();
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    glogparams( \%params );

    # Open the gene2accession file from NCBI and make sure the header
    # line matches what we are expecting.

    my $enzfh = undef;
    if ( $params{entrezfile} =~ /\.gz$/ ) {
        $enzfh = IO::Zlib->new( $params{entrezfile}, 'r' );
        confess 'Unable to open ', $params{entrexfile}, " for reading: $!"
            unless defined $enzfh;
    }
    else {
        $enzfh = IO::File->new( $params{entrezfile}, 'r' );
        confess 'Unable to open ', $params{entrezfile}, " for reading: $!"
            unless defined $enzfh;
    }
 
    my @expected_headers = qw( tax_id
                               GeneID
                               status
                               RNA_nucleotide_accession.version
                               RNA_nucleotide_gi
                               protein_accession.version
                               protein_gi
                               genomic_nucleotide_accession.version
                               genomic_nucleotide_gi
                               start_position_on_the_genomic_accession
                               end_position_on_the_genomic_accession
                               orientation
                               assembly
                               mature_peptide_accession.version
                               mature_peptide_gi
                               Symbol
                               );

    my $expected_header_line = '#'. join("\t",@expected_headers) ."\n";
    my $observed_header_line = $enzfh->getline;

    if ($expected_header_line ne $observed_header_line) {
       die "found $observed_header_line\n expected $expected_header_line\n";
    }

    # Create the hash mapping Symbol-to-GeneID but only do it for lines
    # where the tax_id = 9606, i.e. human.

    my %symbol2geneid = ();
    my $linectr = 0;
    my $hsctr   = 0;
    while (my $line = $enzfh->getline) {
        $linectr++;
        # Do the only-for-human check immediately
        next unless ($line =~ /^9606\t/);
        $hsctr++;
        chomp $line;
        my @fields = split /\t/, $line;
        $symbol2geneid{ $fields[15] } = $fields[1];
        #glogprint( "$line\n" );
        #glogprint( "$fields[1] $fields[15]\n" );
        #die if $hsctr > 10;
    }
    glogprint( "found $linectr records of which $hsctr were for human (taxid:9606)\n" );
    glogprint( "found ", scalar( keys %symbol2geneid ), " human gene symbols\n" );
    $enzfh->close;

    # Read the extended-maf file

    my $maffh = IO::File->new( $params{infile}, 'r' );
    confess 'Unable to open ', $params{infile}, " for reading: $!"
        unless defined $maffh;
    my $maf_header_line = $maffh->getline;

    # Open the output file.

    my $outfh =IO::File->new( $params{outfile}, 'w' )
        or confess "Can't open file [", $params{outfile},
                 "] for writing: $!";
    $outfh->print( $maf_header_line );

    my %missing_symbols = ();
    my $replaced_ctr     = 0;
    my $not_replaced_ctr = 0;
    while (my $line = $maffh->getline) {
        chomp $line;
        my @fields = split /\t/, $line;
        if ($fields[1] eq '0') {
            if (exists $symbol2geneid{ $fields[0] }) {
                $fields[1] = $symbol2geneid{ $fields[0] };
                $replaced_ctr++;
            }
            else {
                $missing_symbols{ $fields[0] }++;
                $not_replaced_ctr++;
            }
        }
        $outfh->print( join("\t",@fields), "\n" );
    }
    $outfh->close;

    glogprint( "$replaced_ctr missing gene ids were replaced based on gene symbols\n" );
    glogprint( "$not_replaced_ctr missing gene ids were not replaced because the gene symbol could not be matched\n" );
    glogprint( {l=>'WARN'}, scalar( keys %missing_symbols ), " gene symbols from MAF were not found in gene2accession file\n" );

    foreach my $key (sort keys %missing_symbols) {
        glogprint( {l=>'WARN'}, "missing symbol $key appeared in MAF ",$missing_symbols{$key}," times\n" );
    }

    glogend;
}


sub cnv_format_convert {
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/cnv_format_convert' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infile      => '',
                   outfile     => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           => \$params{infile},        # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply a pattern
    die "You must specify an infile\n" unless $params{infile};
    die "You must specify an outfile\n" unless $params{outfile};

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin();
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    glogparams( \%params );

    my @expected_headers = qw( gene_id
                               gene_symbol
                               chromosome 
                               gene_start
                               gene_end
                               transcription_strand
                               biotype
                               status
                               Deletion_SampleCount
                               CNLOH_SampleCount
                               HighGain_SampleCount
                               Deletion_Copynumber_list
                               Deletion_SampleID_list
                               Deletion_Titancall_list
                               Deletion_SegmentDescription_list
                               Deletion_PercentGeneCovered_list
                               CNLOH_Copynumber_list
                               CNLOH_SampleID_list
                               CNLOH_Titancall_list
                               CNLOH_SegmentDescription_list
                               CNLOH_PercentGeneCovered_list
                               HighGain_Copynumber_list
                               HighGain_SampleID_list
                               HighGain_Titancall_list
                               HighGain_SegmentDescription_list
                               HighGain_PercentGeneCovered_list
                               );

    # Read the gene-centric summary spreadsheet from Nic's team that
    # contains all of the copy number information
    my $cnv = QCMG::IO::TsvReader->new( filename => $params{infile},
                                        headers  => \@expected_headers,
                                        verbose  => $params{verbose} );

    my $outfh =IO::File->new( $params{outfile}, 'w' )
        or croak 'Can\'t open file [', $params{outfile},
                 "] for writing: $!";

    my $header = "#Gene\tCNVChange\tNumberPatients\tPatients\n";
    $outfh->print( $header );


    # In the input file, there is a single line for each gene but in the
    # output file there must be a separate line for each type of
    # copynumber change for each gene.  So we will need to deconstruct
    # each input line into one or more output lines.

    while (my $line = $cnv->next_record) {
        chomp $line;
        my @fields = split /\t/, $line;
#        # Remove leading/trailing spaces on all fields
#        foreach my $ctr (0..$#fields) {
#            $fields[$ctr] =~ s/^\s+//g;
#            $fields[$ctr] =~ s/\s+$//g;
#        }

        # Grab key fields for later use
        my $gene_symbol = $fields[1];
        my $del_count   = $fields[8];
        my $cnloh_count = $fields[9];
        my $gain_count  = $fields[10];

        # The input data lines can be a problem - they are not all of
        # the same length and some missing fields have been coded as
        # "0" rather than as a blank.

        # Do the deletions
        if ($#fields < 12) {
            warn "skipping - too few fields to do deletions,CNLOH,gains [$line]\n";
            next;
        }
        if ($del_count != 0) {
            my %dels = ();
            my @del_vals = split /\;/, $fields[11];
            my @del_ids  = split /\;/, $fields[12];
            warn "$#fields Deletion ID count does not match stated count [$line]\n"
               unless (scalar(@del_ids) == $del_count);
            warn "Deletion ID count does not match value count [$line]\n"
               unless (scalar(@del_vals) == scalar(@del_ids));
            foreach my $ctr (0..$#del_vals) {
                push @{ $dels{ $del_vals[$ctr] } }, $del_ids[$ctr];
            }
            foreach my $cn (sort keys %dels) {
                my @ids = @{ $dels{ $cn } };
                $outfh->print( join( "\t", $gene_symbol,
                                           $cn,
                                           scalar(@ids),
                                           join(',',@ids) ), "\n" );
            }
        }

        # Do the copy-neutral LOH
        if ($#fields < 17) {
            warn "skipping - too few fields to do CNLOH,gains [$line]\n";
            next;
        }
        if ($cnloh_count != 0) {
            my %cnlohs = ();
            my @cnloh_vals = split /\;/, $fields[16];
            my @cnloh_ids  = split /\;/, $fields[17];
            warn "$#fields CNLOH ID count does not match stated count [$line]\n"
               unless (scalar(@cnloh_ids) == $cnloh_count);
            warn "CNLOH ID count does not match value count [$line]\n"
               unless (scalar(@cnloh_vals) == scalar(@cnloh_ids));
            foreach my $ctr (0..$#cnloh_vals) {
                push @{ $cnlohs{ $cnloh_vals[$ctr] } }, $cnloh_ids[$ctr];
            }
            foreach my $cn (sort keys %cnlohs) {
                my @ids = @{ $cnlohs{ $cn } };
                $outfh->print( join( "\t", $gene_symbol,
                                           'copy-neutral LOH',
                                           scalar(@ids),
                                           join(',',@ids) ), "\n" );
            }
        }

        # Do the high-gain
        if ($#fields < 22) {
            warn "skipping - too few fields to do gains [$line]\n";
            next;
        }
        if ($gain_count != 0) {
            my %gains = ();
            my @gain_vals = split /\;/, $fields[21];
            my @gain_ids  = split /\;/, $fields[22];
            warn "$#fields HighGain ID count does not match stated count [$line]\n"
               unless (scalar(@gain_ids) == $gain_count);
            warn "HighGain ID count does not match value count [$line]\n"
               unless (scalar(@gain_vals) == scalar(@gain_ids));
            foreach my $ctr (0..$#gain_vals) {
                push @{ $gains{ $gain_vals[$ctr] } }, $gain_ids[$ctr];
            }
            foreach my $cn (sort keys %gains) {
                my @ids = @{ $gains{ $cn } };
                $outfh->print( join( "\t", $gene_symbol,
                                           $cn,
                                           scalar(@ids),
                                           join(',',@ids) ), "\n" );
            }
        }

        #print Dumper \%dels, \%cnlohs, \%gains;
        #die;
    }

    glogend;
}


sub verona1 {
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/verona1' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infile      => '',
                   logfile     => '',
                   fastqlist   => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           => \$params{infile},        # -i
           'l|logfile=s'          => \$params{logfile},       # -l
           'q|fastqlist=s'        => \$params{fastqlist},     # -q
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply a pattern
    die "You must specify an infile\n" unless $params{infile};

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin();
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );

    # Read a list of the FASTQ fils that can be found on disk
    my $fql = QCMG::IO::TsvReader->new( filename => $params{fastqlist},
                                        headers  => [ 'FASTQ_filename' ],
                                        verbose  => $params{verbose} );

    my %fqs_on_disk = ();
    while (my $line = $fql->next_record) {
        chomp $line;
        my @fields = split /\t/, $line;
        $fqs_on_disk{ $fields[0] }++;
    }

    my @expected_headers = qw( FCID.LANE.Patient.Tissue
                               FCID
                               LANE
                               Donor
                               Sample
                               Library
                               Tissue
                               FQ_Read1_filename
                               FQ_Read2_filename
                               Phred_Score_Offset );

    # Read the spreadsheet from Verona showing all of the FASTQ files
    # that we could reasonably expect.
    my $ver = QCMG::IO::TsvReader->new( filename => $params{infile},
                                        headers  => \@expected_headers,
                                        verbose  => $params{verbose} );

    my %fqs_in_xls = ();
    my %donors     = ();
    my @xls        = ();
    while (my $line = $ver->next_record) {
        chomp $line;
        my @fields = split /\t/, $line;
        # Remove leading/trailing spaces on all fields
        foreach my $ctr (0..$#fields) {
            $fields[$ctr] =~ s/^\s+//g;
            $fields[$ctr] =~ s/\s+$//g;
        }
        push @xls, \@fields;
        $fqs_in_xls{ $fields[7] }++;
        $fqs_in_xls{ $fields[8] }++;
    }

    # Start reporting problems
    
    my %problems = ();
    foreach my $ra_rec (@xls) {
        my $donor = $ra_rec->[3];
        my $type  = $ra_rec->[4];
        my $read1 = $ra_rec->[7];
        my $read2 = $ra_rec->[8];

        # Set default to "OK"
        $problems{ $donor }->{ $type }->{score} = 0
            unless exists $problems{ $donor }->{ $type };
        $ra_rec->[10] = '';
        $ra_rec->[11] = '';
        $ra_rec->[12] = '';
        $ra_rec->[13] = '';
        $ra_rec->[14] = '';
        $ra_rec->[15] = '';

        # Check that FASTQ files are on disk
        if (! exists $fqs_on_disk{ $read1 } or
            ! defined $fqs_on_disk{ $read1 }) {
            $problems{ $donor }->{ $type }->{score} = 1;
            $ra_rec->[11] = 1;
        }
        if (! exists $fqs_on_disk{ $read2 } or
            ! defined $fqs_on_disk{ $read2 }) {
            $problems{ $donor }->{ $type }->{score} = 2;
            $ra_rec->[12] = 2;
        }

        # Look for double-ups in read1
        if ($fqs_in_xls{ $read1 } > 1) {
            $problems{ $donor }->{ $type }->{score} = 3;
            $ra_rec->[13] = 3;
        }
        # Look for double-ups in read2
        if ($fqs_in_xls{ $read2 } > 1) {
            $problems{ $donor }->{ $type }->{score} = 4;
            $ra_rec->[14] = 4;
        }

        # Look for phred score is not 33
        if ($ra_rec->[9] ne '33') {
            $problems{ $donor }->{ $type }->{score} = 5;
            $ra_rec->[15] = 5;
        }

        push @{ $problems{ $donor }->{ $type }->{bams} }, $ra_rec;
    }

#    print Dumper \%problems,\%fqs_on_disk;
#    foreach my $rec (@xls) {
#        print join(',',@{$rec}),"\n";
#    }
#    print "\n\n\n";
#    foreach my $ra_rec (@xls) {
#        my $read1 = $ra_rec->[7];
#        my $read2 = $ra_rec->[8];
#        if (defined $fqs_on_disk{ $read1 } or
#            defined $fqs_on_disk{ $read1 }) {
#            print join(',',@{$ra_rec}),"\n";
#        }
#    }
#    print "\n\n\n";

    foreach my $donor (sort keys %problems) {
        my $norm = exists $problems{ $donor }->{'NORM'}->{score} ?
                   $problems{ $donor }->{'NORM'}->{score} : 9;
        my $tum  = exists $problems{ $donor }->{'TUM'}->{score} ?
                   $problems{ $donor }->{'TUM'}->{score} : 9;
        print join("\t",$donor,$norm,$tum),"\n";
        foreach my $ra_rec (@{$problems{ $donor }->{'NORM'}->{bams}}) {
            print '    ',join(',',@{$ra_rec}[11..15]),"\t",
                        ,join(',',@{$ra_rec}[0..9]),"\n";
        }
        foreach my $ra_rec (@{$problems{ $donor }->{'TUM'}->{bams}}) {
            print '    ',join(',',@{$ra_rec}[11..15]),"\t",
                         join(',',@{$ra_rec}[0..9]),"\n";
        }
    }

    glogend;
}


sub pindel_2_vcf {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/pindel_2_vcf' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infiles     => [],
                   outfile     => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infiles=s'          =>  $params{infiles},       # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply a pattern
    die "You must specify an infile\n" unless scalar(@{$params{infiles}});
    die "You must specify an outfile\n" unless $params{outfile};

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin();
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );

    # We'll open the output file early because there's no point doing
    # any work if we can't write the results out to file.

    my $outfh =IO::File->new( $params{outfile}, 'w' )
        or croak 'Can\'t open file [', $params{outfile},
                 "] for writing: $!";

#    foreach my $infile (@{$params{infiles}}) {
#
#        my $infh =IO::File->new( $infile, 'w' )
#            or croak 'Can\'t open file [', $infile,
#                     "] for reading: $!";
#
#        my $rec = QCMG::IO::DccSnpRecord->new(
#                       version  => 'dcc1_somatic1',
#                       verbose  => $params{verbose} );
#       
##        die "input file must be in 'dcc1_somatic1' format but detected file type is [",
##            $dccr->version, "]\n" unless ($dccr->version =~ /dcc1_somatic1/i);
#
#        my $ctr = 0;
#        while (my $rec = $dccr->next_record) {
##            last if ($ctr++ > 50000);
#
#            # We want to tally the QCMGflag values before we start
#            # skipping any records so it has to be done early
#            $flag_tallys{ $rec->QCMGflag }++;
#
#            # Skip any records that don't "PASS"
#            next unless ($rec->QCMGflag eq '--');
#
#            # The first step it to work out the mutant allele
#            my $ref_allele  = $rec->reference_genome_allele;
#            my $germ_allele = '';
#            my $mut_allele  = '';
#
#            my $mutation     = $rec->mutation;
#            my $mutation_rev = $rec->mutation;
#            $mutation_rev    =~ tr/ACGTN/TGCAN/;
#
#            if ($mutation =~ /^([ACGT]{1})\>([ACGT]{1})$/) {
#                $germ_allele = $1;
#                $mut_allele = $2;
#                # Germline often does not match reference so can't use
#                # the human reference here, we need to trust the
#                # mutation which is shown in terms of the germline allele
#                # that was mutated and the base it mutated to.
#                if ($germ_allele ne $rec->reference_genome_allele) {
#                   # For paranoia, report mismatches
#                   warn 'reference allele [', $rec->reference_genome_allele,
#                        '] is not in germline position in mutation [',
#                        $mutation, '] in record ', $dccr->record_ctr, "\n"
#                       if $params{verbose};
#                }
#            }
#            else {
#                   warn 'skipping mutation [', $mutation,
#                        "] because it does not fit pattern [ACGT]>[ACGT]\n"
#                       if $params{verbose};
#                   next;
#            }
#
#            # Get stranded allele counts for Tumour so we can work out
#            # if all of the mutants are on one strand.
#
#            my $pattern = qr/([ACGT]{1}):(\d+)\[([\d\.]+)\],(\d+)\[([\d\.]+)\]/;
#            my %alleles = ();
#            my $allele_string = $rec->TD;
#            while ($allele_string =~ m/$pattern/g) {
#                $alleles{ $1 } = { base   => $1,
#                                   pcount => $2,
#                                   pqual  => $3,
#                                   ncount => $4,
#                                   nqual  => $5,
#                                   count  => $2+$4 };
#            }
#
#            # From here on we need to pay attention to strand
#            my $motif         = $rec->FlankSeq;
#            my $motif_revcomp = revcomp( $motif );
#
#            # We can get messed up here if reading a germline DCC
#            # because there are a lot of germline SNPs where the tumour
#            # matches the reference BUT the normal does not so the
#            # "mutation" shows the allele change in the normal not the
#            # tumour.  So if you then try to find the mutant allele in
#            # the TD breakdown, you can't.  We'll cope with this by
#            # "nexting" in this case
#
#            next unless (exists $alleles{ $mut_allele });
#
#            # We are going to tally actual reads here so we will want
#            # the pcount and ncount numbers from %alleles
#
#            if ( $alleles{ $mut_allele }->{pcount} > 0 and
#                 $alleles{ $mut_allele }->{ncount} == 0 ) {
#                 $motifs{'plus'}->{3}->{ substr( $motif, 3, 3 ) }++;
#                 for my $i (0..10) {
#                     $logos{'plus'}->{$i}->{ substr( $motif, $i, 1 ) }++;
#                 }
#                 $mutation_by_read{plus}->{ $mutation } += $alleles{ $mut_allele }->{pcount};
#                 $mutation_by_pos{plus}->{ $mutation }++;
#            }
#            elsif ( $alleles{ $mut_allele }->{pcount} == 0 and
#                    $alleles{ $mut_allele }->{ncount} > 0 ) {
#                 for my $i (0..10) {
#                     $logos{'minus'}->{$i}->{ substr( $motif_revcomp, $i, 1 ) }++;
#                 }
#                 $motifs{'minus'}->{3}->{ substr( $motif_revcomp, 3, 3 ) }++;
#                 $mutation_by_read{minus}->{ $mutation } += $alleles{ $mut_allele }->{ncount};
#                 $mutation_by_pos{minus}->{ $mutation }++;
#            }
#            else {
#                 $motifs{'both'}->{3}->{ substr( $motif, 3, 3 ) }++;
#                 $motifs{'both'}->{3}->{ substr( $motif_revcomp, 3, 3 ) }++;
#                 for my $i (0..10) {
#                     $logos{'both'}->{$i}->{ substr( $motif, $i, 1 ) }++;
#                 }
#                 $mutation_by_read{both}->{ $mutation } += $alleles{ $mut_allele }->{pcount};
#                 $mutation_by_pos{both}->{ $mutation }++;
#            }
#        }
#    }   
#
#    report( $outfh, \%mutation_by_pos, \%mutation_by_read, \%motifs,
#                    \%flag_tallys, \%logos );

    glogend();
}


sub cosmic_vs_dbsnp {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/hiseq_motif' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( cosmic      => '',
                   dbsnp       => '',
                   outfile     => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'c|cosmic=s'           => \$params{cosmic},        # -c
           'd|dbsnp=s'            => \$params{dbsnp},         # -d
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply a pattern
    die "You must specify a COSMIC file\n" unless $params{cosmic};
    die "You must specify a dbSNP file\n" unless $params{dbsnp};
    die "You must specify an outfile\n" unless $params{outfile};

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin();
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );

    # We'll open the output file early because there's no point doing
    # any work if we can't write the results out to file.

    my $outfh =IO::File->new( $params{outfile}, 'w' )
        or croak 'Can\'t open file [', $params{outfile},
                 "] for writing: $!";
    $outfh->print( "Location\tdbSNP_version\tGene_name\tCOSMIC_ctr\n" );

    # Open COSMIC database and save records

    my $cos = IO::File->new( $params{cosmic}, 'r' )
        or croak 'Can\'t open file [', $params{cosmic},
                 "] for reading: $!";
    glogprint( 'Opening COSMIC file for reading - ',$params{cosmic},"\n" )
        if ($params{verbose});

    my %cosmic    = ();
    my $recctr    = 0;
    my $hg19ctr   = 0;
    my $cosmicctr = 0;
    while (my $line = $cos->getline()) {
        my @fields = split /\t/, $line;
        glogprint( $recctr/1000000, "M COSMIC records processed\n" )
             if (++$recctr % 1000000 == 0);
        if ($fields[18]) {
            $hg19ctr++;
            # If we've seen this position before in COSMIC then just
            # increment counter, otherwise establish record.
            if (exists $cosmic{ $fields[18] }) {
                $cosmic{ $fields[18] }->[3]++;
            }
            else {
                # hg19_position, dbSNP_ver, gene_name, counter
                $cosmic{ $fields[18] } = [ $fields[18], '', $fields[0], 1];
                $cosmicctr++;
            }
        }
    }
    glogprint( $recctr, " COSMIC records read\n" );
    glogprint( "  $hg19ctr records found with hg19 annotation\n" );
    glogprint( "  $cosmicctr distinct genomic locations\n" );

    # Now open dbSNP and save details for any that relate to COSMIC
    # records

    my $vcfr = QCMG::IO::VcfReader->new(
                       filename => $params{dbsnp},
                       verbose  => $params{verbose} );

    # We do see cases where the same location comes up with multiple
    # dbSNP positions:
    #
    # 1       592051  rs10218594      C       T       .       .
    # RSPOS=592051;dbSNPBuildID=119;SSR=0;SAO=0;VP=050000000005000002000100;WGT=1;VC=SNV;ASP;OTHERKG
    # 1       592051  rs79746121      C       T       .       .
    # RSPOS=592051;dbSNPBuildID=131;SSR=0;SAO=0;VP=050000000005040102000100;WGT=1;VC=SNV;ASP;VLD;GNO;OTHERKG

    my $ctr              = 0;
    my $non_snv_ctr      = 0;
    my $non_cosmic_ctr   = 0;
    my $no_dbsnp_ver_ctr = 0;
    my %dbsnp            = ();

    while (my $vcf = $vcfr->next_record) {
#        last unless $ctr++ < 200000;
        # Skip anything that is not classified as SNV
        if ($vcf->info !~ /VC\=SNV/) {
            $non_snv_ctr++;
            next;
        }
        if ($vcf->info =~ /dbSNPBuildID\=(\d+)/) {
            my $dbver = $1;
            my $loc = $vcf->chrom .':'.
                      $vcf->position .'-'.
                      $vcf->position;

            # If this is not a COSMIC location then we can bail now.
            if (! exists $cosmic{ $loc }) {
                $non_cosmic_ctr++;
                next;
            }

            # Unfortunately we see plenty of cases where the same
            # position appears with multiple rsIDs.  In this case we
            # always keep the oldest SNP version

            if ($cosmic{$loc}->[1]) {
                warn 'record ', $vcfr->record_count, " location $loc has aleady been seen - old:",
                     $cosmic{$loc}->[1], ' new:',$dbver,"\n"
                    if ($params{verbose} > 1);
                # Get the oldest version;     
                my $bestver = ( $dbver < $cosmic{$loc}->[1] )
                              ? $dbver : $cosmic{$loc}->[1];
                $cosmic{$loc}->[1] = $bestver;
            }
            $cosmic{$loc}->[1] = $dbver;
        }
        else {
            $no_dbsnp_ver_ctr++;
            next;
        }
    }
    #print Dumper "hohoho", \%dbsnp;
    glogprint( $vcfr->record_count, " dbSNP records read\n" );
    glogprint( "  $non_snv_ctr records skipped because not annotated as SNV\n" );
    glogprint( "  $no_dbsnp_ver_ctr records skipped because no dbSNP version annotated\n" );
    glogprint( "  $non_cosmic_ctr records skipped because position not in COSMIC\n" );

    # Do the output
    foreach my $loc (sort keys %cosmic) {
        $outfh->print( join("\t", @{ $cosmic{$loc} }), "\n");
    }

    glogend();
}


sub hiseq_motif {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/hiseq_motif' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infiles     => [],
                   outfile     => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infiles=s'          =>  $params{infiles},       # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply a pattern
    die "You must specify an infile\n" unless scalar(@{$params{infiles}});
    die "You must specify an outfile\n" unless $params{outfile};

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin();
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );

    # We'll open the output file early because there's no point doing
    # any work if we can't write the results out to file.

    my $outfh =IO::File->new( $params{outfile}, 'w' )
        or croak 'Can\'t open file [', $params{outfile},
                 "] for writing: $!";

    my %flag_tallys      = ();
    my %motifs           = ();
    my %mutation_by_pos  = ();
    my %mutation_by_read = ();
    my %logos            = ();

    foreach my $infile (@{$params{infiles}}) {

        my $dccr = QCMG::IO::DccSnpReader->new(
                       filename => $infile,
                       verbose  => $params{verbose} );
#        die "input file must be in 'dcc1_somatic1' format but detected file type is [",
#            $dccr->version, "]\n" unless ($dccr->version =~ /dcc1_somatic1/i);

        my $ctr = 0;
        while (my $rec = $dccr->next_record) {
#            last if ($ctr++ > 50000);

            # We want to tally the QCMGflag values before we start
            # skipping any records so it has to be done early
            $flag_tallys{ $rec->QCMGflag }++;

            # Skip any records that don't "PASS"
            next unless ($rec->QCMGflag eq '--');

            # The first step it to work out the mutant allele
            my $ref_allele  = $rec->reference_genome_allele;
            my $germ_allele = '';
            my $mut_allele  = '';

            my $mutation     = $rec->mutation;
            my $mutation_rev = $rec->mutation;
            $mutation_rev    =~ tr/ACGTN/TGCAN/;

            if ($mutation =~ /^([ACGT]{1})\>([ACGT]{1})$/) {
                $germ_allele = $1;
                $mut_allele = $2;
                # Germline often does not match reference so can't use
                # the human reference here, we need to trust the
                # mutation which is shown in terms of the germline allele
                # that was mutated and the base it mutated to.
                if ($germ_allele ne $rec->reference_genome_allele) {
                   # For paranoia, report mismatches
                   warn 'reference allele [', $rec->reference_genome_allele,
                        '] is not in germline position in mutation [',
                        $mutation, '] in record ', $dccr->record_ctr, "\n"
                       if $params{verbose};
                }
            }
            else {
                   warn 'skipping mutation [', $mutation,
                        "] because it does not fit pattern [ACGT]>[ACGT]\n"
                       if $params{verbose};
                   next;
            }

            # Get stranded allele counts for Tumour so we can work out
            # if all of the mutants are on one strand.

            my $pattern = qr/([ACGT]{1}):(\d+)\[([\d\.]+)\],(\d+)\[([\d\.]+)\]/;
            my %alleles = ();
            my $allele_string = $rec->TD;
            while ($allele_string =~ m/$pattern/g) {
                $alleles{ $1 } = { base   => $1,
                                   pcount => $2,
                                   pqual  => $3,
                                   ncount => $4,
                                   nqual  => $5,
                                   count  => $2+$4 };
            }

            # From here on we need to pay attention to strand
            my $motif         = $rec->FlankSeq;
            my $motif_revcomp = revcomp( $motif );

            # We can get messed up here if reading a germline DCC
            # because there are a lot of germline SNPs where the tumour
            # matches the reference BUT the normal does not so the
            # "mutation" shows the allele change in the normal not the
            # tumour.  So if you then try to find the mutant allele in
            # the TD breakdown, you can't.  We'll cope with this by
            # "nexting" in this case

            next unless (exists $alleles{ $mut_allele });

            # We are going to tally actual reads here so we will want
            # the pcount and ncount numbers from %alleles

            if ( $alleles{ $mut_allele }->{pcount} > 0 and
                 $alleles{ $mut_allele }->{ncount} == 0 ) {
                 $motifs{'plus'}->{3}->{ substr( $motif, 3, 3 ) }++;
                 for my $i (0..10) {
                     $logos{'plus'}->{$i}->{ substr( $motif, $i, 1 ) }++;
                 }
                 $mutation_by_read{plus}->{ $mutation } += $alleles{ $mut_allele }->{pcount};
                 $mutation_by_pos{plus}->{ $mutation }++;
            }
            elsif ( $alleles{ $mut_allele }->{pcount} == 0 and
                    $alleles{ $mut_allele }->{ncount} > 0 ) {
                 for my $i (0..10) {
                     $logos{'minus'}->{$i}->{ substr( $motif_revcomp, $i, 1 ) }++;
                 }
                 $motifs{'minus'}->{3}->{ substr( $motif_revcomp, 3, 3 ) }++;
                 $mutation_by_read{minus}->{ $mutation } += $alleles{ $mut_allele }->{ncount};
                 $mutation_by_pos{minus}->{ $mutation }++;
            }
            else {
                 $motifs{'both'}->{3}->{ substr( $motif, 3, 3 ) }++;
                 $motifs{'both'}->{3}->{ substr( $motif_revcomp, 3, 3 ) }++;
                 for my $i (0..10) {
                     $logos{'both'}->{$i}->{ substr( $motif, $i, 1 ) }++;
                 }
                 $mutation_by_read{both}->{ $mutation } += $alleles{ $mut_allele }->{pcount};
                 $mutation_by_pos{both}->{ $mutation }++;
            }
        }
    }   

    report( $outfh, \%mutation_by_pos, \%mutation_by_read, \%motifs,
                    \%flag_tallys, \%logos );

    glogend();
}


sub report {
    my $outfh               = shift;
    my $rh_mutation_by_pos  = shift;
    my $rh_mutation_by_read = shift;
    my $rh_motifs           = shift;
    my $rh_qcmgflags        = shift;
    my $rh_logos            = shift;

    $outfh->print("Mutation Counts (by mutated position):\n",
                  "Mutation\tBothStrands\tPlusOnly\tMinusOnly\tTotal\n");
    # Get a list of all mutations seen in all 3 categories
    my %mutns = map { $_ => 1 }
                (keys %{$rh_mutation_by_pos->{both}},
                 keys %{$rh_mutation_by_pos->{plus}},
                 keys %{$rh_mutation_by_pos->{minus}});
    foreach my $mutn (sort keys %mutns) {
        my $both  = exists $rh_mutation_by_pos->{both}->{$mutn} ?
                    $rh_mutation_by_pos->{both}->{$mutn} : 0;
        my $plus  = exists $rh_mutation_by_pos->{plus}->{$mutn} ?
                    $rh_mutation_by_pos->{plus}->{$mutn} : 0;
        my $minus = exists $rh_mutation_by_pos->{minus}->{$mutn} ?
                    $rh_mutation_by_pos->{minus}->{$mutn} : 0;
        $outfh->print( join("\t", $mutn, $both, $plus, $minus,
                                         $both+$plus+$minus ), "\n" );
    }

    $outfh->print("\n\nMutation Counts (by mutated reads):\n",
                  "Mutation\tBothStrands\tPlusOnly\tMinusOnly\tTotal\n");
    my %reads = map { $_ => 1 }
                (keys %{$rh_mutation_by_read->{both}},
                 keys %{$rh_mutation_by_read->{plus}},
                 keys %{$rh_mutation_by_read->{minus}});
    foreach my $mutnr (sort keys %reads) {
        my $both  = exists $rh_mutation_by_read->{both}->{$mutnr} ?
                    $rh_mutation_by_read->{both}->{$mutnr} : 0;
        my $plus  = exists $rh_mutation_by_read->{plus}->{$mutnr} ?
                    $rh_mutation_by_read->{plus}->{$mutnr} : 0;
        my $minus = exists $rh_mutation_by_read->{minus}->{$mutnr} ?
                    $rh_mutation_by_read->{minus}->{$mutnr} : 0;
        $outfh->print( join("\t", $mutnr, $both, $plus, $minus,
                                          $both+$plus+$minus ), "\n" );
    }

    $outfh->print("\n\nLogos:\n",
                  join("\t", 'Type', 'Base', 0..10 ), "\n");
    foreach my $type (qw/ both plus minus /) {
        foreach my $base (qw/ A C G T /) {
            my @counts = map { exists $rh_logos->{$type}->{$_}->{$base} ?
                               $rh_logos->{$type}->{$_}->{$base} : 0 }
                         (0..10);
            $outfh->print( join("\t", $type, $base, @counts ), "\n" );
        }
    }

    $outfh->print("\n\nQCMGflag Counts:\n");
    foreach my $flag (sort keys %{$rh_qcmgflags}) {
        $outfh->print( join("\t", $rh_qcmgflags->{$flag}, $flag), "\n" );
    }

    $outfh->print("\n\nMotif Counts:\n",
                  "Motif\tBothStrands\tPlusOnly\tMinusOnly\tTotal\n");
    # Get a list of all motifs seen in all 3 categories
    my %motifs = map { $_ => 1 }
                 (keys %{$rh_motifs->{both}->{3}},
                  keys %{$rh_motifs->{plus}->{3}},
                  keys %{$rh_motifs->{minus}->{3}});
    foreach my $motif (sort keys %motifs) {
        my $both  = exists $rh_motifs->{both}->{3}->{$motif} ?
                    $rh_motifs->{both}->{3}->{$motif} : 0;
        my $plus  = exists $rh_motifs->{plus}->{3}->{$motif} ?
                    $rh_motifs->{plus}->{3}->{$motif} : 0;
        my $minus = exists $rh_motifs->{minus}->{3}->{$motif} ?
                    $rh_motifs->{minus}->{3}->{$motif} : 0;
        $outfh->print( join("\t", $motif, $both, $plus, $minus,
                                  $both+$plus+$minus ), "\n" );
    }
}


sub revcomp {
    my $seq = shift;
    $seq =~ tr/CATGN/GTACN/;
    return reverse($seq);
}


sub ega_20121116_a {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/ega_20121116_a' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( inventory   => '',
                   xmlfile     => '',
                   outfile     => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|inventory=s'        => \$params{inventory},     # -i
           'x|xmlfile=s'          => \$params{xmlfile},       # -x
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply a pattern
    die "You must specify an infile\n" unless scalar(@{$params{infiles}});
    die "You must specify an outfile\n" unless $params{outfile};

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin();
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );

    # We'll open the output file early because there's no point doing
    # any work if we can't write the results out to file.

    my $outfh =IO::File->new( $params{outfile}, 'w' )
        or croak 'Can\'t open file [', $params{outfile},
                 "] for writing: $!";

}


sub germline_2_qsig {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/germline_2_qsig' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infiles     => [],
                   outfile     => '',
                   logfile     => '',
                   chromstem   => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           =>  $params{infiles},       # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'c|chromstem=s'        => \$params{chromstem},     # -c
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply a pattern
    die "You must specify an infile\n" unless scalar(@{$params{infiles}});
    die "You must specify an outfile\n" unless $params{outfile};

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin();
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );

    # We'll open the output file early because there's no point doing
    # any work if we can't write the results out to file.

    my $outfh =IO::File->new( $params{outfile}, 'w' )
        or croak 'Can\'t open file [', $params{outfile},
                 "] for writing: $!";

    # Open qSNP germline file(s) and read in all records

    my $snps = {};
    foreach my $file (@{$params{infiles}}) {
        my $qgr = QCMG::IO::qSnpGermlineReader->new(
                      filename => $file,
                      verbose  => $params{verbose} );
        my $rctr = 0;
        while (my $rec = $qgr->next_record()) {
            $rctr++;
            push @{ $snps->{ $rec->chromosome .':'.
                             $rec->chromosome_start() } }, $rec;
        }

        glogprint( {l=>'INFO'}, "found $rctr records in file $file\n" );
    }

    my $sctr      = 0;
    my %multimuts = ();
    my %finals    = ();
    foreach my $snp (keys %{$snps}) {
        my @recs = @{$snps->{$snp}};

        # If we see multiple mutation types at this position then we
        # will be conservative and assume it's a position subject to
        # error so we'll ditch.

        my %mutns = ();
        foreach my $rec (@recs) {

            # JVP !!!!
            # quality_score is the WRONG column but there is a column
            # header missing and it's for the column we want (mutation)
            # so for the moment we will use quality_score and when the
            # files get fixed, we'll have to change this code to use
            # mutation.
            #$rec->quality_score();
            #$rec->mutation();

            $mutns{ $rec->quality_score() }++;
        }
        my @muttypes = keys %mutns;
        if (scalar(@muttypes)>1) {
            $multimuts{ scalar(@muttypes) }++;
            next;
        }
        $sctr++;

        # If we got this far then we have a SNP with one or more records
        # all with a consistent mutation type so we'll output the fields
        # qsignature requires plus a count of the records which should
        # be effectively a count of how many input files contained the
        # mutation.

        my $chrom = $recs[0]->chromosome();
        $chrom =~ s/^chr//;  # strip off leading 'chr' if present

        $finals{ $chrom }->{ $recs[0]->chromosome_start() } =
            [ $params{chromstem} . $recs[0]->chromosome(),
              $recs[0]->chromosome_start(),
              '',
              '',
              $recs[0]->reference_genome_allele(),
              scalar(@recs) ];
    }
    
    # Now we will handle the output but using some sensible sorting
    # options.

    foreach my $chrom (sort { $a <=> $b } keys %finals) {
        foreach my $pos (sort { $a <=> $b } keys %{$finals{$chrom}}) {
            my $rec = $finals{ $chrom }->{ $pos };
            $outfh->print( join("\t", @{$rec}), "\n" );
        }
    }
    $outfh->close;

    glogprint( {l=>'INFO'}, "found $sctr positions with single mutation type\n" );
    foreach my $key (sort keys %multimuts) {
         glogprint( {l=>'INFO'}, 'ignoring ', $multimuts{$key}, 
                                 " positions with $key mutation types\n" );
    }

    glogend();
}


sub search_bam_headers {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/search_bam_headers' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( dir         => '/mnt/seq_results',
                   pattern     => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'd|dir=s'              => \$params{dir},           # -d
           'p|pattern=s'          => \$params{pattern},       # -p
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply a pattern
    die "You must specify a search pattern\n" unless $params{pattern};

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin();
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );

    my $finder = QCMG::FileDir::Finder->new( verbose => $params{verbose} );
    my @bams = $finder->find_file( $params{dir}, '.bam$' );

    my $fctr = 0;
    glogprint( {l=>'INFO'}, 'found ', scalar(@bams),
                            ' BAM files in dir ', $params{dir},"\n" );

    my $nctr = 0;
    foreach my $bam (@bams) {
        # Open BAM headers for reading
        open INSAM,  "samtools view -H $bam |"
             or die "Can't open samtools view -H on BAM [$bam]: $!";
        my $lctr = 0;
        my @found = ();
        while (my $line = <INSAM>) {
            chomp $line;
            $lctr++;
            if ($line =~ /$params{pattern}/i) {
                push @found, [ $lctr, $line ];
            }
        }

        glogprint( {l=>'INFO'}, "searched $lctr header lines from file $bam\n" );

        if (@found) {
            glogprint( {l=>'INFO'}, 'found ',scalar(@found)," matches in file $bam\n" );
            print "Matches found in file $bam\n";
            foreach my $ra_match (@found) {
                print ' ', join("\t",@{$ra_match}),"\n";
            }
        }
    }

    glogend();
}


sub qcmgschema_1 {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/qcmgschema_1' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( Project          => '',
                   Donor            => '',
                   Sample           => '',
                  'Primary Library' => '',
                   Source           => '',
                   Material         => '',
                   Aligner          => '',
                   Mapset           => '',
                  'Capture Kit'     => '',
                   logfile          => '',
                   dumper           => 0,
                   verbose          => 0 );

    my @constraints = ( 'Project', 'Donor', 'Material', 'Source',
                        'Sample', 'Primary Library',
                        'Aligner', 'Mapset', 'Capture Kit' );

    my $results = GetOptions (
           'p|project=s'          => \$params{Project},           # -p
           'd|donor=s'            => \$params{Donor},             # -d
           'e|sample=s'           => \$params{Sample},            # -e
           'b|library=s'          => \$params{'Primary Library'}, # -b
           's|source=s'           => \$params{Source},            # -s
           'r|material=s'         => \$params{Material},          # -r
           'a|aligner=s'          => \$params{Aligner},           # -a
           't|mapset=s'           => \$params{Mapset},            # -t
           'c|capture=s'          => \$params{'Capture Kit'},     # -c
           'l|logfile=s'          => \$params{logfile},           # -l
           'v|verbose+'           => \$params{verbose},           # -v
           'dumper+'              => \$params{dumper},            # --dumper
           );

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin();
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );

    my $geneus = QCMG::DB::GeneusReader->new();

    my $ra_mapsets = ();
    if ( $geneus->all_resources_metadata() ) {
        $ra_mapsets = $geneus->fetch_metadata();
    }
    glogprint( 'Found ', scalar(@{ $ra_mapsets }), " mapsets in Geneus\n" );

    foreach my $constraint (@constraints) {
        if ($params{$constraint}) {
            $ra_mapsets = my_filter( $ra_mapsets, $constraint,
                                     $params{$constraint} );
        }
    }
    glogprint( scalar(@{ $ra_mapsets }), " mapsets passed all constraints\n" );

#    print Dumper $ra_mapsets;
    if ($params{dumper}) {
        print Dumper $ra_mapsets;
    }
    else {
        print mapsets_to_string( $ra_mapsets, \@constraints );
    }

    glogend();
}


sub allele_freq {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/allele_freq' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( maf              => '',
                   logfile          => '',
                   verbose          => 0 );

    my $results = GetOptions (
           'f|maf=s'              => \$params{maf},               # -f
           'l|logfile=s'          => \$params{logfile},           # -l
           'v|verbose+'           => \$params{verbose},           # -v
           );

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin();
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    glogparams( \%params );

    my $maf = QCMG::IO::MafReader->new( filename => $params{maf} );
 
    my $ref_count = 0;
    my $var_count = 0;
    my $rec_ctr = 0;
    while (my $rec = $maf->next_record) {
        my $ra_info = $rec->allele_freq;
        $ref_count += $ra_info->[ 4 ];
        $var_count += $ra_info->[ 5 ];
        $rec_ctr++;
    };

    my $project = '';
    if ($params{maf} =~ /(APGI_\d+)\//) {
        $project = $1;
    }

    print join("\t", 'allele_freq',
                     $project,
                     $rec_ctr,
                     $var_count/($ref_count + $var_count),
                     $params{maf} ),"\n";


    glogend();
}


sub my_filter {
    my $ra_recs = shift;
    my $field   = shift;
    my $pattern = shift;

    die "Field [$field] does not exist in supplied records"
        unless (exists $ra_recs->[0]->{$field});

    my @passed_recs = ();
    foreach my $rec (@{ $ra_recs }) {
        next unless (defined $rec->{$field} and
                     $rec->{$field} =~ /$pattern/i);
        push @passed_recs, $rec;
    }

    glogprint( scalar(@passed_recs),
               " mapsets passed constraint [$pattern] on field [$field]\n" );

    return \@passed_recs;
}

sub mapsets_to_string {
    my $ra_recs        = shift;
    my $ra_constraints = shift;

    my @headers = @{ $ra_constraints };

    my $string = '';
    $string .= join(',',@headers)."\n";
    no warnings;
    foreach my $rec (@{ $ra_recs }) {
        my @values = map { $rec->{$_} } @headers;
        $string .= join(',',@values)."\n";
    }
    use warnings;

    return $string;
}


sub wig_2_bed {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/wig_2_bed' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infile      => '',
                   outfile     => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           => \$params{infile},        # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin();
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );

    my $infh =IO::File->new( $params{infile}, 'r' )
        or croak 'Can\'t open WIG file [', $params{infile},
                 "] for reading: $!";

    my $outfh =IO::File->new( $params{outfile}, 'w' )
        or croak 'Can\'t open BED file [', $params{outfile},
                 "] for writing: $!";

    $outfh->print( '#', join("\t", qw{ sequence start end score } ), "\n" );

    my $rec_ctr   = 0;
    my $step_type = 0;  # fixedStep = 1; variableStep = 2;
    my $track     = '';

    my $chrom = '';
    my $pos   = 0;
    my $step  = 1;
    my $span  = 1;

    while (my $line = $infh->getline) {
        chomp $line;
        # ...
        $rec_ctr++;

        #print "$rec_ctr - $line\n";

        if ( $line =~ m/^#/) {
            # skip comments
        }
        elsif ( $line =~ m/^track /) {
            # save the track line
            $track = $line;
        }
        elsif ( $line =~ m/^fixedStep / || $line =~ m/^variableStep /) {
            # Reset values for each new "step"
            $chrom = '';
            $pos   = 0;
            $step  = 1;
            $span  = 1;
            my @fields = split('\s', $line);
            # Set step type from first element
            $step_type = ( $fields[0] =~ m/^fixedStep/ )    ? 1 :
                         ( $fields[0] =~ m/^variableStep/ ) ? 2 : 0;
            # Process values from the remaining elements
            for my $fctr (1..$#fields) {
                my ($key, $val) = split(/=/,$fields[$fctr]);
                if ($key eq 'chrom') {
                    $chrom = $val;
                }
                elsif ($key eq 'start') {
                    $pos = $val-1;
                }
                elsif ($key eq 'step') {
                    $step = $val;
                }
                elsif ($key eq 'span') {
                    $span = $val;
                }
                else {
                    die "unrecognised elements in Step line: $line\n";
                }
            }
            #print join("\t", "step:$step_type", "chrom:$chrom", "pos:$pos",
            #                 "span:$span", "step:$step"),"\n"
        }
        else {
            my @fields = split('\s', $line);

            if ($step_type == 1) {
                $outfh->print( join("\t", $chrom, $pos, $pos+$span, $fields[0]),"\n" );
                $pos += $step;
            }
            elsif ($step_type == 2) {
                $pos = $fields[0];
                $outfh->print( join("\t", $chrom, $pos, $pos+$span, $fields[1]),"\n" );
            }
            else {
                # If we see data before determining the step type then exit
                die "fixedStep/variableStep type not determined\n";
            }
        }
    }
    $outfh->close;

    glogprint( {l=>'INFO'}, "found $rec_ctr records in wig file ",
                            $params{infile},"\n" );

    glogend();
}


sub benchmark_wiki2tsv {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/benchmark_wiki2tsv' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infile     => '',
                   outfile    => 'benchmark_wiki2tsv.tsv',
                   logfile    => '',
                   verbose    => 0 );

    my $results = GetOptions (
           'i|infile=s'           => \$params{infile},            # -i
           'o|outfile=s'          => \$params{outfile},           # -o
           'l|logfile=s'          => \$params{logfile},           # -l
           'v|verbose+'           => \$params{verbose},           # -v
           );

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin();
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );

    die( "An input file must be specified with the -i option\n" )
        unless ($params{infile});

    # Open the input file
    my $infh = IO::File->new( $params{infile}, 'r' );
    die( 'Cannot open input file ',$params{infile}," for reading: $!\n" )
        unless defined $infh;
    glogprint( 'Reading from file: ',$params{infile},"\n" )
        if $params{verbose};

    # Read all the lines from the file
    my $text = '';
    while (my $line = $infh->getline) {
        $text .= $line;
    }
    $infh->close();

    my @wiki_lines = split /\|\-\n/, $text;

    # Take off 2 text blocks related to wiki table header
    my $h1 = shift @wiki_lines;
    my $h2 = shift @wiki_lines;

    my @recs = ();
    while (1) {
        last unless scalar(@wiki_lines);

        # read off a trio of lines
        my $line1 = shift @wiki_lines;
        chomp $line1;
        my @fields1 = split /\n!/, $line1;
        foreach (@fields1) {
            $_ =~ s/^[\s\n\|\!]*//;
            $_ =~ s/[\s\n]*$//;
        }

        # We need to do some work on the lib_size string
        my $lib_size = $fields1[9];
        $lib_size =~ s/[M,]//g;
        $lib_size *= 1000000;

        my $line2 = shift @wiki_lines;
        chomp $line2;
        my @fields2 = split /\|/, $line2;
        foreach (@fields2) {
            $_ =~ s/^[\s\n\|\!]*//;
            $_ =~ s/[\s\n]*$//;
        }

        my $line3 = shift @wiki_lines;
        chomp $line3;
        my @fields3 = split /\|/, $line3;
        foreach (@fields3) {
            $_ =~ s/^[\s\n\|\!]*//;
            $_ =~ s/[\s\n]*$//;
        }

        push @recs, [ $fields1[1],    # normal/tumour
                      $fields1[2],    # frag size
                      $fields1[6],    # isize
                      $fields1[8],    # unmapped %
                      $lib_size,      # lib size
                      $fields1[0],    # bam
                      $fields2[2] ];  # fastq
        push @recs, [ $fields1[1],    # normal/tumour
                      $fields1[2],    # frag size
                      $fields1[6],    # isize
                      $fields1[8],    # unmapped %
                      $lib_size,      # lib size
                      $fields1[0],    # bam
                      $fields3[2] ];  # fastq
    }

    # Open the output file
    my $outfh = IO::File->new( $params{outfile}, 'w' );
    die( "Cannot open output file ",$params{outfile}," for writing: $!\n" )
          unless defined $outfh;
    glogprint( "Reading from file: ",$params{outfile},"\n" ) if $params{verbose};

    # Print header
    $outfh->print( join("\t",qw( T/N Frag_size TLEN Unmapped%
                                 Lib_complexity BAM FASTQ )), "\n" );

    foreach my $rec (@recs) {
        $outfh->print( join("\t",@{$rec}),"\n" );
    }
    $outfh->close;

    # Log program exit
    glogend;
}


sub wikitable_media2moin {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/wikitable_media2moin' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infile     => '',
                   outfile    => 'wikitable_media2moin.txt',
                   logfile    => '',
                   verbose    => 0 );

    my $results = GetOptions (
           'i|infile=s'           => \$params{infile},            # -i
           'o|outfile=s'          => \$params{outfile},           # -o
           'l|logfile=s'          => \$params{logfile},           # -l
           'v|verbose+'           => \$params{verbose},           # -v
           );

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin();
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );

    die( "An input file must be specified with the -i option\n" )
        unless ($params{infile});

    # Open the input file
    my $infh = IO::File->new( $params{infile}, 'r' );
    die( 'Cannot open input file ',$params{infile}," for reading: $!\n" )
        unless defined $infh;
    glogprint( 'Reading from file: ',$params{infile},"\n" )
        if $params{verbose};

    # Read all the lines from the file
    my $text = '';
    while (my $line = $infh->getline) {
        $text .= $line;
    }
    $infh->close();

    # Now we do some global find-n-replace ops:
    # rowspan=99|
    # colspan=99|

    $text =~ s/\{|//g;
    $text =~ s/|\}//g;
    $text =~ s/\s+rowspan=(\d+)\|/<\|$1>/g;
    $text =~ s/\s+colspan=(\d+)\|/<\-$1>/g;

    # Split into table lines by looking for line-sep string: "|-"
    my @old_lines = split /\|\-\n/, $text;

    my @new_lines = ();
    foreach my $line (@old_lines) {
        chomp $line;

        # Collapse end-of-lines followed by |
        $line =~ s/\n\|/||/g;

        # *Try* to cope with inline <pre> tags
        $line =~ s/\s*<pre>/<style="font-family:monospace;">/g;
        $line =~ s/<\/pre>//g;
        $line =~ s/\n/<<BR>>/g;

        #$line =~ s/<pre>/{{{/g;
        #$line =~ s/<\/pre>/}}}/g;
        #$line =~ s/\n/<<BR>>/g;

        # Double up any singleton | chars
        $line =~ s/([^|<]{1})\|([^|>]{1})/$1\|\|$2/g;

        # Make sure line starts and ends in a ||
        $line .= '||' unless $line =~ /\|\|$/;
        $line = '|' . $line unless $line =~ /^\|\|$/;

        push @new_lines, $line;
    }

    # Open the output file
    my $outfh = IO::File->new( $params{outfile}, 'w' );
    die( "Cannot open output file ",$params{outfile}," for writing: $!\n" )
          unless defined $outfh;
    glogprint( "Writing to file: ",$params{outfile},"\n" ) if $params{verbose};

    foreach my $line (@new_lines) {
        $outfh->print( "$line\n" );
    }
    $outfh->close;

    # Log program exit
    glogend;
}


sub N_count {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/N_count' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( fasta       => '',
                   outfile     => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'r|fasta=s'            => \$params{fasta},         # -r
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply an outfile name
    die "You must specify an output file\n" unless $params{outfile};

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin;
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    glogparams( \%params );
    
    my $ref = QCMG::IO::FastaReader->new(
                  filename => $params{fasta},
                  verbose  => $params{verbose} );

    my $ra_seqs = $ref->sequences;

    # Open the output file
    my $outfh = IO::File->new( $params{outfile}, 'w' );
    die "unable to open file $params{outfile} for writing: $!" unless
        defined $outfh;

    my @runs = ();
    foreach my $seq (@{ $ref->sequences }) {
        my $name = $seq->{defline};
        $name =~ s/\>//;
        $name =~ s/\s.*//g;

        my $bases  = $seq->{sequence};
        my $chrlen = length($bases);

        while ($bases =~ /(N+)/g) {
            my $motif     = $1;
            my $endbase   = pos $bases;
            my $mlen      = length($motif);
            my $startbase = $endbase - $mlen;
            glogprint length($motif), " ends at position $endbase\n";

            # Determine how close this motif is from the closest end of
            # the chromosome.
            my $distance = ( $startbase < ($chrlen - $endbase) ) ?
                           $startbase : ($chrlen - $endbase); 
            push @runs, [ $name, $chrlen, $distance, $mlen, ($startbase .'-'. $endbase), ];
        }
    }

    $outfh->print( join( "\t", qw( Seq SeqLen EndDist NLen NLocation ) ),"\n" );
    foreach my $ra_vals (@runs) {
        $outfh->print( join("\t",@{$ra_vals}),"\n" );
    }

    $outfh->close;

    glogend;
}


sub telomere_analysis {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/telomere_analysis' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( config      => '',
                   outstem     => 'qmotif_'.time(),
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'c|config=s'           => \$params{config},        # -c
           's|outstem=s'          => \$params{outstem},       # -s
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply an outfile name
    die "You must specify a config file\n" unless $params{config};

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin;
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    glogparams( \%params );

    my $ini = QCMG::IO::INIFile->new(
                  file    => $params{config},
                  verbose => $params{verbose},
                  allow_duplicate_rules => 1 );

    # Generate output file names
    my $out_summary_bam   = $params{outstem} . '_summary_by_bam.txt';
    #my $out_summary_donor = $params{outstem} . '_summary_by_donor.txt';
    my $out_matrix        = $params{outstem} . '_matrix.txt';
    my $out_motif         = $params{outstem} . '_motif.txt';
    
    glogprint( "output summary_by_bam file: $out_summary_bam\n" );
    #glogprint( "output summary_by_donor file: $out_summary_donor\n" );
    glogprint( "output matrix file: $out_matrix\n" );
    glogprint( "output motif file: $out_motif\n" );

    # Open the output files
    my $sumfh = IO::File->new( $out_summary_bam, 'w' );
    die "unable to open file $out_summary_bam for writing: $!" unless
        defined $sumfh;
    #my $donorfh = IO::File->new( $out_summary_donor, 'w' );
    #die "unable to open file $out_summary_donor for writing: $!" unless
    #    defined $donorfh;

    my @fields_from_bam_name = qw( parent_project
                                   project
                                   material
                                   sample_code
                                   sample
                                   library_protocol
                                   capture_kit
                                   aligner
                                   platform
                                   user );

    # Write header
    $sumfh->print( join( "\t", @fields_from_bam_name,
                               qw( WindowSize
                                   Cutoff
                                   TotalReads
                                   UniqueMotifs
                                   RawUnmapped
                                   RawIncludes
                                   RawGenomic
                                   RawTotal
                                   ScaledUnmapped
                                   ScaledIncludes
                                   ScaledGenomic
                                   ScaledTotal
                                   IniFileSection
                                   File ) ), "\n" );


    # This data structure is going to hold the values for all of the
    # windows across all of the samples and files.  We will report it as
    # a matrix of section/file vs window once we have processed all
    # files.  To simplify construction of the matrix, we will build
    # hashes of all observed section/file and windows as well as
    # recording the counts from each window.

    my %regions = ( sections => {},
                    windows  => {},
                    counts   => {} );

    # Loop through sections (donors) and files
    my @section_names = $ini->section_names;
    foreach my $section_name (@section_names) {
        glogprint "Processing $section_name\n";
        my $section = $ini->section( $section_name );
        die "no file= rules specified in section $section_name\n"
            unless (exists $section->{file});

        my %stats = ();

        # TO_DO:
        # We should parse the SUMMARY line to make sure that the same
        # windowsize and cutoff was used across all files.  We should
        # also require the same includes and excludes but this will be a
        # bit harder to do unless we require the config file contents to
        # be included in the logfile or output.

        foreach my $file (@{ $section->{file} }) {
            $regions{ sections }->{ $section_name }->{ $file }++;

            # Parse BAM file name onto array of values for output report
            my $rh_values = split_final_bam_name( $file );
            my @bam_values = map { (exists $rh_values->{$_}) ? $rh_values->{$_} : '' }
                            @fields_from_bam_name;

            # Drop out straight away if we can't find the file
            if (! -e $file) {
                warn "cannot find file on disk [$file]\n";
                next;
            }

            if ($file =~ /\.log$/) {
                glogprint "loading qMotif log file $file\n";
                my $log = QCMG::IO::LogFileReader->new(
                              filename => $file,
                              verbose  => $params{verbose} );
                my @lines = $log->lines;
 
                my $telo = QCMG::IO::TelomereReport->new(
                               lines   => \@lines,
                               verbose => $params{verbose} );
 
                my $rh_report = $telo->report;
                $sumfh->print( join( "\t", @bam_values,
                                           $rh_report->{windowsize},
                                           $rh_report->{cutoff},
                                           $rh_report->{totalreads},
                                           $rh_report->{unique_motifs},
                                           $rh_report->{raw_unmapped},
                                           $rh_report->{raw_includes},
                                           $rh_report->{raw_genomic},
                                           $rh_report->{raw_total},
                                           $rh_report->{normalised_unmapped},
                                           $rh_report->{normalised_includes},
                                           $rh_report->{normalised_genomic},
                                           $rh_report->{normalised_total},
                                           $section_name,
                                           $file ), 
                               "\n" );

                # Create the big data structure of windows.  Note that this
                # will require scaling to total reads for the file
                my @windows = $telo->windows;
                foreach my $window (@windows) {
                    my $loc = $window->{location} .'.'. $window->{category};
                    $regions{windows}->{ $loc }++;
                    my $normalised_val = sprintf( "%.0f",
                                                  $window->{stage1_reads} /
                                                  $telo->totalreads *
                                                  1000000000 );
                    $regions{counts}->{ $file }->{ $loc } = $normalised_val;
                }
            }
            elsif ($file =~ /\.xml$/) {
                glogprint "loading qMotif XML file $file\n";
                #my $xs = XML::Simple->new;
                #my $ref = $xs->XMLin( $file );  
                my $qm = QCMG::XML::Qmotif->new( file => $file );

                my $raw_total = $qm->get_value( 'rawUnmapped' ) +
                                $qm->get_value( 'rawIncludes' ) +
                                $qm->get_value( 'rawGenomic' );
                my $scaled_total = $qm->get_value( 'scaledUnmapped' ) +
                                   $qm->get_value( 'scaledIncludes' ) +
                                   $qm->get_value( 'scaledGenomic' );

                $sumfh->print( join( "\t", @bam_values,
                                           $qm->get_value( 'windowSize' ),
                                           $qm->get_value( 'cutoff' ),
                                           $qm->get_value( 'totalReadCount' ),
                                           $qm->get_value( 'noOfMotifs' ),
                                           $qm->get_value( 'rawUnmapped' ),
                                           $qm->get_value( 'rawIncludes' ),
                                           $qm->get_value( 'rawGenomic' ),
                                           $raw_total,
                                           $qm->get_value( 'scaledUnmapped' ),
                                           $qm->get_value( 'scaledIncludes' ),
                                           $qm->get_value( 'scaledGenomic' ),
                                           $scaled_total,
                                           $file ),
                               "\n" );
                $qm->log_top_motif_percentages;
            }
        }
    }
    $sumfh->close;

    # Open the matrix output file
    my $matrixfh = IO::File->new( $out_matrix, 'w' );
    die "unable to open file $out_matrix for writing: $!" unless
        defined $matrixfh;

    # Create headers - includes construction of the file list that will
    # be used in the matrix output section below
    my @headers_sections = ();
    my @headers_files    = ();
    foreach my $section_name (sort keys %{ $regions{sections} }) {
        foreach my $file (sort keys %{ $regions{sections}->{$section_name} }) {
            push @headers_sections, $section_name;
            push @headers_files, $file;
        }
    }

    $matrixfh->print( join("\t", ('','','', @headers_sections)),"\n" );
    $matrixfh->print( join("\t", ('Window','Count','Total', @headers_files)),"\n" );

    foreach my $window (sort keys %{ $regions{windows} }) {
        my @values = ();
        my $total  = 0;

        foreach my $file (@headers_files) {
            # If a value exists use it, otherwise set 0
            my $value = 0;
            if (exists $regions{counts}->{ $file }->{ $window }) {
                $value = $regions{counts}->{ $file }->{ $window };
            }
            push @values, $value;
            $total += $value;
        }

        # Add windowname, count and totalreads to front of the matrix line
        unshift @values, $window, $regions{windows}->{$window}, $total;
        $matrixfh->print( join("\t",@values),"\n" );
    }
    $matrixfh->close;

    # Open the motif output file
    my $motiffh = IO::File->new( $out_motif, 'w' );
    die "unable to open file $out_motif for writing: $!" unless
        defined $motiffh;
    $motiffh->close;

    glogend;
}


sub Nruns_vs_telomere_motifs {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/Nruns_vs_telomere_motifs' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( nruns       => '',
                   telomotifs  => '',
                   outfile     => '',
                   logfile     => '',
                   distance    => 1000,
                   verbose     => 0 );

    my $results = GetOptions (
           'n|nruns=s'            => \$params{nruns},         # -n
           't|telomotifs=s'       => \$params{telomotifs},    # -t
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'd|distance=s'         => \$params{distance},      # -d
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply an outfile name
    die "You must specify an nruns file\n" unless $params{nruns};
    die "You must specify an telomotifs file\n" unless $params{telomotifs};
    die "You must specify an output file\n" unless $params{outfile};

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin;
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    glogparams( \%params );
    
    # Open the nruns file
    my $nfh = IO::File->new( $params{nruns}, 'r' );
    die "unable to open file $params{nruns} for reading: $!" unless
        defined $nfh;
    my %nruns = ();
    my $ctr = 0;
    my $headers = $nfh->getline; # ditch header line
    chomp $headers;
    while (my $line = $nfh->getline) {
        $ctr++;
        chomp $line;
        my @fields = split "\t", $line;
        next unless $fields[3] > 100; 
        push @{ $nruns{ $fields[0] } }, \@fields;
    }
    $nfh->close;
    glogprint( "read $ctr records from ",$params{nruns},"\n" );

    # Open the output file
    my $outfh = IO::File->new( $params{outfile}, 'w' );
    die "unable to open file $params{outfile} for writing: $!" unless
        defined $outfh;

    $outfh->print( "status\tdistance\tMotifStart\tMotifEnd\t$headers\n");
    
    my $gff = QCMG::IO::GffReader->new(
                  filename => $params{telomotifs},
                  verbose  => $params{verbose} );

    while (my $rec = $gff->next_record) {
        my $motif_start = $rec->start;
        my $motif_end   = $rec->end;
        # Loop through nruns
        next unless exists $nruns{ $rec->seqname };
        foreach my $ra_nrun (@{ $nruns{ $rec->seqname } }) {
            # Look for overlap
            $ra_nrun->[4] =~ /(\d+)\-(\d+)/;
            my $nrun_start = $1;
            my $nrun_end   = $2;
            my $result = ranges_overlap( $nrun_start, $nrun_end,
                                         $motif_start, $motif_end );
            if ($result) {
                my $string = ($result == 4) ? 'overlap' :
                             ($result == 3) ? 'contains' :
                             ($result == 2) ? 'contains' :
                             ($result == 1) ? 'identical' : '???';
                glogprint( "$string: ", $rec->seqname, 
                           " Nrun $nrun_start\-$nrun_end vs",
                           " Motif $motif_start\-$motif_end\n"
                          );
                my $overlap = ($nrun_start < $motif_start) ?
                               $nrun_end - $motif_start :
                               $motif_end - $nrun_start;
                $outfh->print( join( "\t", $string,
                                           $overlap,
                                           $rec->start,
                                           $rec->end,
                                           @{$ra_nrun} ), "\n" );
            }
            else {
                # How far apart are they?
                my $separation = ($nrun_start < $motif_start) ?
                                 $motif_start - $nrun_end :
                                 $nrun_start - $motif_end;
                if ($separation <= $params{distance}) {
                    glogprint( 'close: ', $rec->seqname, 
                               " Nrun $nrun_start\-$nrun_end vs",
                               " Motif $motif_start\-$motif_end are $separation apart\n" );
                    $outfh->print( join( "\t", 'close',
                                               $separation,
                                               $rec->start,
                                               $rec->end,
                                               @{$ra_nrun} ), "\n" );
                }
            }
        }
    }

    $outfh->close;

    glogend;
}


sub align_2_csvs {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/align_2_csvs' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( primary     => '',
                   secondary   => '',
                   outfile     => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'p|primary=s'          => \$params{primary},       # -p
           's|secondary=s'        => \$params{secondary},     # -s
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply primary, secondary and output file names
    die "You must specify a primary CSV file\n" unless $params{primary};
    die "You must specify a secondary CSV file\n" unless $params{secondary};
    die "You must specify an output CSV file\n" unless $params{outfile};

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin;
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    glogparams( \%params );

    my $ofh = IO::File->new( $params{outfile}, 'w');
    die "unable to open file $params{outfile} for writing: $!" unless
        defined $ofh;

    my %d1 = ();
    my $pfh = IO::File->new( $params{primary}, 'r');
    die "unable to open file $params{primary} for reading: $!" unless
        defined $pfh;
    while (<$pfh>) {
        chomp $_;
        my @fields = split /\t/, $_;
        $d1{$fields[0]} = \@fields;
    }
    $pfh->close;

    my $sfh = IO::File->new( $params{secondary}, 'r');
    die "unable to open file $params{secondary} for reading: $!" unless
        defined $sfh;
    while (<$sfh>) {
        chomp $_;
        my @fields = split /\t/, $_;
        if (exists $d1{ $fields[0] }) {
            $ofh->print( join("\t", @fields,
                                    @{ $d1{ $fields[0] } }), "\n" );
        }
        else {
            $ofh->print( join("\t", @fields),"\n" );
        }
    }
    $sfh->close;
    $ofh->close;

    glogend;
}


sub fastq_kmer {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/fastq_kmer' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infile      => '',
                   outfile     => '',
                   logfile     => '',
                   kmerlen     => 3,
                   positions   => '',
                   maxrec      => 0,
                   threshold   => 0.001,
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           => \$params{infile},        # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'k|kmerlen=i'          => \$params{kmerlen},       # -k
           'p|positions=s'        => \$params{positions},     # -p
           'x|maxrec=s'           => \$params{maxrec},        # -x
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply infile and outfile
    die "You must specify an input file\n" unless $params{infile};
    die "You must specify an output file\n" unless $params{outfile};

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin;
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    glogparams( \%params );
    
    my $fq = QCMG::IO::FastqReader->new(
                 filename => $params{infile},
                 verbose  => $params{verbose} );

    # Open the output file
    my $outfh = IO::File->new( $params{outfile}, 'w' );
    die "unable to open file $params{outfile} for writing: $!" unless
        defined $outfh;

    # We will assume that all reads are of the same length so the
    # current_rec will drive our selection of positions if no
    # positions are specified.
    my @positions = ();
    if (! $params{positions}) {
        my $len = length( $fq->current_rec->{seq} );
        push @positions, (0..($len - $params{kmerlen}));
    }
    else {
        push @positions, split( ',', $params{positions} );
    }

    glogprint( 'Initiating kmer-', $params{kmerlen},
               ' analysis at ', scalar(@positions), " positions\n" );

    # Do tally keyed on position, then on kmer
    my %kmers = ();
    while (my $read = $fq->next_record) {
        foreach my $start (@positions) {
            my $kmer = substr( $read->{seq}, $start, $params{kmerlen} );
            $kmers{ $start }->{ $kmer }++;
        }
        last if ($params{maxrec} and $fq->rec_ctr >= $params{maxrec});
    }

    glogprint( 'Read ', $fq->rec_ctr, " records from FASTQ\n" );

    # Collect single list of all observed kmers - any single position
    # may not show the full spectrum.
    my %seen_kmers_orig = ();
    foreach my $start (@positions) {
        $seen_kmers_orig{ $_ }++ foreach (keys %{ $kmers{$start} } );
    }

    glogprint( 'Found ', scalar( keys %seen_kmers_orig ), " distinct kmers\n" );


    # Apply threshold - any kmer that falls below the threshold is
    # consolidated into a single 'X-Other' kmer

    # Step 1 - work out denominator for each position
    my %kmers_per_position = ();
    foreach my $start (@positions) {
        my @vals = values %{ $kmers{ $start } };
        $kmers_per_position{ $start } += $_ foreach @vals;
    }

    # Step 2 - apply threshold
    foreach my $start (@positions) {
        my $threshold = $kmers_per_position{ $start } * $params{threshold};
        foreach my $kmer (keys %{ $kmers{ $start } }) {
            if ($kmers{$start}->{$kmer} < $threshold) {
                $kmers{$start}->{'X-other'} += $kmers{$start}->{$kmer};
                delete $kmers{$start}->{$kmer};
            }
        }
    }

    # Redo list of all observed kmers to see if threholding removed any

    my %seen_kmers = ();
    foreach my $start (@positions) {
        $seen_kmers{ $_ }++ foreach (keys %{ $kmers{$start} } );
    }

    glogprint( 'Found ', scalar( keys %seen_kmers ), " distinct kmers after threshold applied\n" );

    # Write report
    $outfh->print( "\t", join("\t",@positions), "\n" );
    foreach my $kmer (sort keys %seen_kmers) {
        my $line = "$kmer";
        foreach my $start (@positions) {
            if (exists $kmers{$start}->{$kmer}) {
                $line .= "\t". $kmers{$start}->{$kmer};
            }
            else {
                $line .= "\t";
            }
        }
        $outfh->print( $line, "\n" );
    }

    $outfh->close;

    glogend;
}


sub sequence_lengths {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/sequence_lengths' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( fasta       => '',
                   outfile     => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'r|fasta=s'            => \$params{fasta},         # -r
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply fasta and outfile names
    die "You must specify a FASTA input file\n" unless $params{fasta};
    die "You must specify an output file\n" unless $params{outfile};

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin;
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    glogparams( \%params );
    
    my $ref = QCMG::IO::FastaReader->new(
                  filename => $params{fasta},
                  verbose  => $params{verbose} );

    my $ra_seqs = $ref->sequences;

    # Open the output file
    my $outfh = IO::File->new( $params{outfile}, 'w' );
    die "unable to open file $params{outfile} for writing: $!" unless
        defined $outfh;
    $outfh->print( join( "\t", qw( Seq SeqLen SeqInfo ) ),"\n" );

    my @runs = ();
    my $seqctr = 0;
    my $seqlen_total = 0;
    foreach my $seq (@{ $ref->sequences }) {
        my $name = $seq->{defline};
        $name =~ s/\>//;
        $name =~ s/\s.*//g;

        # If the line has |-separated elements, pull off the first one
        my @fields = split /\|/, $name;
        $name = shift @fields;
        my $extra = '';
        $extra = join( '|', @fields );

        my $bases  = $seq->{sequence};
        my $chrlen = length($bases);

        $outfh->print( join( "\t", $name, $chrlen, $extra ),"\n" );

        $seqctr++;
        $seqlen_total += $chrlen;
    }

    $outfh->close;

    glogprint( "Processed sequences: $seqctr\n" );
    glogprint( "Total length: $seqlen_total\n" );
    glogend;
}


sub qprofiler_genome {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/qprofiler_genome' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( windowsize  => 1000000,
                   name        => '',
                   fasta       => '',
                   outfile     => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'w|windowsize=i'       => \$params{windowsize},    # -w
           'n|name=s'             => \$params{name},          # -n
           'r|fasta=s'            => \$params{fasta},         # -r
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply fasta and outfile names
    die "You must specify a FASTA input file\n" unless $params{fasta};
    die "You must specify an output file\n" unless $params{outfile};

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin;
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    glogparams( \%params );

    # This is where we will put stuff for conversion to JSON
    my $qprofiler_genome = { name       => $params{name},
                             run_params => \%params,
                             exec_vals  => glogexecvals() };
    
    my $ref = QCMG::IO::FastaReader->new(
                  filename => $params{fasta},
                  verbose  => $params{verbose} );

    my $ra_seqs = $ref->sequences;

    # Open the output file
    my $outfh = IO::File->new( $params{outfile}, 'w' );
    die "unable to open file $params{outfile} for writing: $!" unless
        defined $outfh;

    my @runs = ();
    my $seqctr = 0;
    my $seqlen_total = 0;
    foreach my $seq (@{ $ref->sequences }) {
        my $name = $seq->{defline};
        $name =~ s/\>//;
        $name =~ s/\s.*//g;
    
        glogprint( "Processing sequence $name\n" );

        # If the line has |-separated elements, pull off the first one
        my @fields = split /\|/, $name;
        $name = shift @fields;
        my $extra = '';
        $extra = join( '|', @fields );

        # Get length
        my $bases  = $seq->{sequence};
        my $chrlen = length($bases);
        $qprofiler_genome->{ 'chromosome_lengths' }->{ $name } = $chrlen;

        # Get base composition for each window so we can do GC%
        my @bases             = split //, $bases;
        my $bases_processed   = 0;
        my $bases_unprocessed = scalar(@bases);
        my $window_ctr        = 0;

        while ( $bases_unprocessed ) {
            my $limit = ($bases_unprocessed > $params{windowsize})
                        ? $params{windowsize}
                        : $bases_unprocessed;

            my $gc_count = 0;
            my %tally = ( 'N'=> 0, 'A'=>0, 'C'=>0, 'G'=>0, 'T'=>0 );
            # Tally bases and remember to force upper case for simplicity
            my $loop_start = $bases_processed;
            my $loop_end   = $loop_start + $limit -1;
            foreach my $i ($loop_start..$loop_end) {
                $tally{ uc( $bases[ $i ] ) }++;
            }

            # For windows entirely full of N's, cope with zero denominator
            my $total_nonN_bases = $tally{A}+$tally{C}+$tally{G}+$tally{T};
            my $gc_percent = ($total_nonN_bases > 0) 
                             ? ($tally{C}+$tally{G}) / $total_nonN_bases
                             : 0;

            #glogprint( join("\t", $bases_processed, $bases_unprocessed, $limit,
            #                      $loop_start, $loop_end, sprintf('%.3f', $gc_percent),
            #                      $tally{'N'}, $tally{'A'}, $tally{'C'}, $tally{'G'}, $tally{'T'}
            #               ), "\n" );

            my $label = $loop_start .'-'. $loop_end;
            $qprofiler_genome->{ 'gcpercent_windows' }->{ $name }->{ $label } = 
                sprintf( '%.3f', $gc_percent );
            
            $bases_processed   += $limit;
            $bases_unprocessed -= $limit;
            $window_ctr++;
        }

        glogprint( "  windows: $window_ctr\n" );

        $seqctr++;
        $seqlen_total += $chrlen;
    }

    $outfh->print( encode_json( $qprofiler_genome ) );
    $outfh->close;

    glogprint( "Processed sequences: $seqctr\n" );
    glogprint( "Total length: $seqlen_total\n" );
    glogend;
}


__END__

=head1 NAME

jvptools.pl - Perl bits-b-pieces


=head1 SYNOPSIS

 jvptools.pl command [options]


=head1 ABSTRACT

This script is a collection of snippets of perl code that do various
small but useful tasks.  In the past I would have made each of these as
a separate script but that way leads to madness as more and more scripts
get written and placed in more and more directories.  At least this way
there is only one script and POD block to grep when you are looking for
that routine you know you wrote but can't remember where it is.


__END__

=head1 NAME

jvptools.pl - Perl bits-b-pieces


=head1 SYNOPSIS

 jvptools.pl command [options]


=head1 ABSTRACT

This script is a collection of snippets of perl code that do various
small but useful tasks.  In the past I would have made each of these as
a separate script but that way leads to madness as more and more scripts
get written and placed in more and more directories.  At least this way
there is only one script and POD block to grep when you are looking for
that routine you know you wrote but can't remember where it is.

=head1 COMMANDS

 search_bam_headers - search BAM headers for a pattern
 germline_2_qsig    - create a qSignature SNP file from qSNP germline file
 qcmgschema_1
 ega_20121116_a
 hiseq_motif        - 
 cosmic_vs_dbsnp    - for each COSMIC mutations that appear in dbSNP
 allele_freq        - tally alleles from a MAF file
 wig_2_bed          - convert a WIG file to a BED file
 benchmark_wiki2tsv - convert ICGC benchmark wiki table to TSV
 verona1            - investigate PNET FASTQ files sent from verona
 N_count            - find runs of N on FASTA sequences
 telomere_analysis  - parse qMotif telomere log files
 align_2_csvs       - align 2 CSVs based on ID in first column
 wikitable_media2moin - convert MediaWiki tables to Moin syntax
 cnv_format_convert - convert TITAN format to GAP
 add_entrezgene_to_maf - add Entrez Gene Id numbers to MAF
 sequence_lengths   - output the length of each sequence in a FASTA
 qprofiler_genome   - output JSON file with properties from a genome FASTA
 version            - print version number and exit immediately
 help               - display usage summary
 man                - display full man page

=head1 COMMAND DETAILS

=head2 search_bam_headers

 -d | --dir           root directory to search for BAMs;
                      default=/mnt/seq_results
 -p | --pattern       MAF output file
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages

=head3 Commandline options

=over

=item B<-d | --dir>

Root directory to be searched for BAMs.  The default for this is
B</mnt/seq_results/> so if you wish to search a subtree, you will need
to specify this option.

=item B<-p | --pattern>

Perl-ish regex pattern to be used to search the headers of BAMs.

=item B<-l | --logfile>

Optional log file name.  If this option is not specified then logging
goes to STDOUT.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.

=back

=head2 germline_2_qsig

 -i | --infile        qSNP germline call file
 -o | --outfile       output qSignature SNP file
 -l | --logfile       log file (optional)
 -c | --chromstem     text to be prepended to sequence names (optional)
 -v | --verbose       print progress and diagnostic messages

For samples/organisms where we want to use qSignature but there is
limited dbSNP coverage, it can be useful to be able to use qSNP germline
SNP calls as a basis for the creation of a list of SNP positions to be
used in qSignature.  This mode reads one or more qSNP germline SNP call
files and constructs a position file suitable for use in qSignature.

=head3 Commandline options

=over

=item B<-i | --infile>

qSNP germline SNP call file. This file is a tab-separated file with at
least 22 columns.  An example showing just the first 3 columns is shown
below:

 analysis_id            control_sample_id   variation_id     ...
 qcmg_sgv_20120518_1A   GEMM_0101_ND        GEMM_0101_SNP_1
 qcmg_sgv_20120518_1A   GEMM_0101_ND        GEMM_0101_SNP_2
 qcmg_sgv_20120518_1A   GEMM_0101_ND        GEMM_0101_SNP_3
 qcmg_sgv_20120518_1A   GEMM_0101_ND        GEMM_0101_SNP_4
 qcmg_sgv_20120518_1A   GEMM_0101_ND        GEMM_0101_SNP_6
 ...

=item B<-o | --outfile>

A tab-separated file containing the positions to be used by qSignature.
This file has 5 columns of which only 1, 2 and 5 contain data.
The file will look like this:

 ...

=item B<-l | --logfile>

Optional log file name.  If this option is not specified then logging
goes to STDOUT.

=item B<-c | --chromstem>

This is an optional text string which is prepended to all sequence
(chromosome names) in the output file.
The qsignature SNP file sequence names must match those in the BAM 
files so if the BAM sequences are chr1..chrMT and the qSNP germline
positions are for sequences 1..MT, this option will let you add 
the 'chr' to each SNP record so they will match the BAM nomenclature.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.

=back

=head2 qcmgschema_1

 -p | --project       project pattern
 -d | --donor         donor pattern
 -e | --sample        sample pattern
 -b | --library       library pattern
 -s | --source        source pattern
 -r | --material      material pattern
 -a | --aligner       aligner pattern
 -t | --mapset        mapset pattern
 -c | --capture       capture kit pattern
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages
      --dumper        print out details of all records using Data::Dumper

Except for -l and -v, the rest of these CLI parameters specify strings
that will be pattern-matched (case-insensitive) against mapsets pulled
from the Geneus LIMS.  These params are called constraints. By
specifying multiple constraints you can assemble a list of BAMs that you
might want to assemble for an analysis.

Adding --dumper to your query can be very useful as is does a full
Data::Dumper display of the records that satisfied your query.  This can
be more useful than the 10-field CSV view that is returned if the
--dumper flag is not specified.

=head3 Commandline options

=over

=item B<-i | --infile>

=back

=head2 ega_20121116_a

 -i | --inventory     text file of pathnames from c.leonard
 -x | --xmlfile       XML fragment from timelord.pl
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages

Conrad Leonard has produced an inventory of all of the BAM files
that he thinks need to be sent to the EGA from each ICGC project as part
of our 2012-11-16 data submission.  I (JP) would like to rehead some of
these BAMs based on bad sample and library information in the @RG lines.
We need to identify the overlap between
the files in CL's inventory and the "bad" files from JP's timelord XML.
This routine does the comparison.

B<N.B. this routine was never completed because we ran out of time
to do the reheading>

=head2 benchmark_wiki2tsv

 -i | --infile        name of input file; Required!
 -o | --outfile       output filename; default=benchmark_wiki2tsv.tsv
 -l | --logfile       logfile name
 -v | --verbose       print progress and diagnostic messages

 This routine takes a text file of wiki markup for the table on the ICGC
 Benchmarking 2013 
 wiki page under "Summary of BAMs post mapping" and converts it into a
 TSV file.  Note that the input file must not have wrapped lines.

=head3 Commandline options

=over

=item B<-i | --infile>

A single text file containing the MediaWiki markup of the GEMM table.

=item B<-o | --outfile>

The output file is a tab-separated plain text with unix-style line
endings.  If no name is supplied for the output file then the default
(rs_2_gene.txt) is used.  The output file contains 7 columns of data:
    
 1.  T/N             - tumour or normal
 2.  Frag_size       - fragment size from qprofiler
 3.  TLEN            - modal TLEN from qprofiler
 4.  Unmapped%       - unmapped percentage (range:0-100) from qprofiler
 5.  Lib_complexity  - estimated library complexity from markdups METRICS file
 6.  BAM             - name of BAM used for qprofiler measures
 7.  FASTQ           - FASTQ file that we want to annotate

=head2 wikitable_media2moin

 -i | --infile        name of input file; Required!
 -o | --outfile       output filename; default=wikitable_media2moin.txt
 -l | --logfile       logfile name
 -v | --verbose       print progress and diagnostic messages

This routine takes a text file of a table in MediaWiki markup and tries
to convert it to Moin wiki syntax.  The intent is to make it easier to
move tables from the QCMG MediaWiki to the QIMR Berghofer Moin wiki.

=head3 Commandline options

=over

=item B<-i | --infile>

A single text file containing the MediaWiki markup of the table.

=item B<-o | --outfile>

The output file is Moin wiki syntax with one table row per line of text.

=item B<-l | --logfile>

By default, error and diagnostic messages are output to STDOUT but if
a logfile is specified, the messages will be written to the logfile 
instead.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.
The default level is 0 so no diagnostic messages are output.  At level
1, a small number of progress-related messages are written.  These
messages will be printed to STDOUT unless the B<-l> option is used to
specify a logfile.

=back

=head2 allele_freq

 -i | --maf        MAF file
 -l | --logfile    log file (optional)
 -v | --verbose    print progress and diagnostic messages

This routine parses all of the records in the given MAF file and prints
out information about the average non-reference allele frequency.  It
relies on the QCMG fields ND and TD so it doesn't work on non-QCMG MAFs.

It is expected that this routine will be run on lots of MAF files
serially so the only output is a single line with 5 tab-separated
values:

 1. The string 'allele_freq'
 2. The donor ID (assumes APGI) parsed out of the MAF pathname
 3. Number of records counted to create fraction
 4. Non-reference allele frequency as a percentage
 5. The name of the MAF file analysed.

=head2 wig_2_bed

Reads a WIG file and converts it to a BED file.  This was designed so we
could do a liftover via UCSC, which requires a BED file, of the 
replication fork data from the Chen et al "Impact of replication timing..."
2010 paper which has a hg18 WIG file.

=head2 N_count

 -f | --fasta      FASTA file
 -o | --outfile    output file (TSV-format)
 -l | --logfile    log file (optional)
 -v | --verbose    print progress and diagnostic messages

Takes a FASTA file and finds all runs (if any) of "N" characters.  This
was originally intended to identify the lengths of telomeric N-runs but
there are long and short N runs at numerous places in the genome.
The output is 5-item line for each run of N's showing the name and
length of the sequence, the distance of the motif from the nearest
chromosome end, and the length and location of the run of N's.
For example, a selection of the N-runs in chromosome 2 of the
GRCh37_ICGC_standard_v2.fa human genome looks like:

 Seq     SeqLen      EndDist    NLen    NLocation
 chr2    243199373   0          10000   0-10000
 chr2    243199373   3529312    50000   3529312-3579312
 chr2    243199373   5018788    100000  5018788-5118788
 chr2    243199373   16279724   50000   16279724-16329724
 chr2    243199373   21153113   25000   21153113-21178113
 chr2    243199373   31705550   1       31705550-31705551
 chr2    243199373   31725939   851     31725939-31726790
 chr2    243199373   31816827   1       31816827-31816828
 ...
 chr2    243199373   110109337  142000  110109337-110251337
 chr2    243199373   93408791   100000  149690582-149790582
 chr2    243199373   9145632    50000   234003741-234053741
 chr2    243199373   3367395    30000   239801978-239831978
 chr2    243199373   2390241    25000   240784132-240809132
 chr2    243199373   46897      50000   243102476-243152476
 chr2    243199373   0          10000   243189373-243199373

=head2 verona1

 -i | --infile        TSV from verona spreadsheet
 -q | --fastqlist     list of FASTQ files on disk
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages

This routine looks at the spreadsheet of all PNET mapsets sent to us by
Verona plus the list of FASTQ files that were actually on the disk and
tries to work out which donors have a full set of tumour and normal
FASTQ files, i.e. they are ready for mapping and analysis.  For each
mapset it looks at the read 1 and read 2 FASTQ files and reports codes
for any problems it finds.

Problem codes reported for each mapset:

 0 - No problems, mapsets complete
 1 - Read 1 FASTQ is not on disk
 2 - Read 2 FASTQ is not on disk
 3 - Read 1 double-ups
 4 - Read 2 double-ups
 5 - Base quality scheme is not 33-based (usually this means it's 64-based)

=head2 telomere_analysis

 -c | --config     INI-style config file
 -o | --outfile    output file (TSV-format)
 -l | --logfile    log file (optional)
 -v | --verbose    print progress and diagnostic messages

Takes an INI file listing qMotif files to be analysed.

=head2 Nruns_vs_telomere_motifs

Takes the output of mode N_count plus a GFF file of genomic regions and
tries to work out whether any N_coutn regions are close to the GFF
regions.  This will be used to take a GFF of telomeric motif locations
plus the N_count regions (some of which should be telomeric) and works
out how close the motifs are to the end of N-runs.

=head2 align_2_csvs

 -p | --primary    primary CSV (determines output order)
 -s | --secondary  secondary CSV file
 -o | --outfile    output CSV file
 -l | --logfile    log file (optional)
 -v | --verbose    print progress and diagnostic messages

Takes 2 CSV files and outputs a single file where the two files have
been meregd based on the first column in each file being a common
identifier.  The output is in the same order as the primary file.  The
lines from the 2 files are just concatenated so if every line in the
primary file does not contain the full number of fields then the columns
in the output file are not going to line up properly.

=head2 cnv_format_convert

 -i | --infile     CNV file in NicW summary format
 -o | --outfile    output CNV file in GapSummary format
 -l | --logfile    log file (optional)
 -v | --verbose    print progress and diagnostic messages

Converts one CNV file format into another.

=head2 add_entrezgene_to_maf

 -i | --infile     CNV file in NicW summary format
 -o | --outfile    output CNV file in GapSummary format
 -l | --logfile    log file (optional)
 -v | --verbose    print progress and diagnostic messages

Our older MAF files have "0" for all records for the field
Entrez_Gene_Id.  This field is supposed to hold the Entrez gene if which
is an integer.  However, we use Ensembl rather than Refseq for our gene 
model and Ensembl gene identifiers are not integers (all have "ENSG" as a
prefix) so they do not fit in this integer field.  Consequently we leave
it empty.

The MAF spec is at:

https://wiki.nci.nih.gov/display/TCGA/Mutation+Annotation+Format+%28MAF%29+Specification


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>
=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: jvptools.pl 8281 2014-06-20 06:32:55Z j.pearson $


=head1 COPYRIGHT

Copyright 2012-2014  The University of Queensland
Copyright 2012-2014  John V Pearson
Copyright 2014-2016  QIMR Berghofer Medical Research Institute

All rights reserved.  This License is limited to, and you
may use the Software solely for, your own internal and non-commercial
use for academic and research purposes. Without limiting the foregoing,
you may not use the Software as part of, or in any way in connection with 
the production, marketing, sale or support of any commercial product or
service or for any governmental purposes.

=cut
