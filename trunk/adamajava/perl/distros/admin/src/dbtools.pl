#!/usr/bin/env perl

##############################################################################
#
#  Program:  dbtools.pl
#  Author:   John V Pearson
#  Created:  2013-10-16
#
#  Tasks associated with databases.
#
#  $Id: dbtools.pl 4667 2014-07-24 10:09:43Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak verbose );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;
use POSIX;

use QCMG::FileDir::Finder;
use QCMG::FileDir::QLogFile;
use QCMG::FileDir::QSnpDirParser;
use QCMG::FileDir::GatkDirParser;
use QCMG::FileDir::q3QsnpDirParser;
use QCMG::FileDir::q3GatkDirParser;
use QCMG::IO::EnsemblDomainsReader;
use QCMG::IO::FastaReader;
use QCMG::IO::SamHeader;
use QCMG::IO::SamReader;
use QCMG::IO::VcfReader;
use QCMG::PDF::Document;
use QCMG::QInspect::Sam2Pdf;
use QCMG::Util::FileManipulator;
use QCMG::Util::QLog;
use QCMG::Verify::AutoNames;
use QCMG::QBamMaker::SeqFinalDirectory;

use vars qw( $SVNID $REVISION $CMDLINE );

( $REVISION ) = '$Revision: 4667 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: dbtools.pl 4667 2014-07-24 10:09:43Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


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

    # Each of the modes invokes a subroutine, and these subroutines 
    # are often almost identical.  While this looks like wasteful code 
    # duplication, it is necessary so that each mode has complete 
    # independence in terms of processing input parameters and taking
    # action based on the parameters.

    my @valid_modes = qw( help man version qsnpdir finalbampairs
                          aligner_from_mapset_bam
                          qsnp_vcf_info
                          gatk_vcf_info
                          vcf_info_compare
                        );

    if ($mode =~ /^$valid_modes[0]$/i or $mode =~ /\?/) {
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => 'SYNOPSIS|COMMANDS' );

    }
    elsif ($mode =~ /^$valid_modes[1]$/i) {
        pod2usage(-exitstatus => 0, -verbose => 2)
    }
    elsif ($mode =~ /^$valid_modes[2]$/i) {
        print "$SVNID\n";
    }
    elsif ($mode =~ /^$valid_modes[3]/i) {
        qsnpdir();
    }
    elsif ($mode =~ /^$valid_modes[4]/i) {
        finalbampairs();
    }
    elsif ($mode =~ /^$valid_modes[5]/i) {
        aligner_from_mapset_bam()
    }
    elsif ($mode =~ /^$valid_modes[6]/i) {
        vcf_info( 'qsnp' )
    }
    elsif ($mode =~ /^$valid_modes[7]/i) {
        vcf_info( 'gatk' )
    }
    elsif ($mode =~ /^$valid_modes[8]/i) {
        vcf_info_compare();
    }
    else {
        die "dbtools mode [$mode] is unrecognised; valid modes are: " .
            join(' ',@valid_modes) ."\n";
    }
}


sub vcf_info {
    my $mode = shift;

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/VCF_INFO' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( dirs     => [],
                   outfile  => '',
                   logfile  => '',
                   verbose  => 0 );

    my $results = GetOptions (
           'd|dir=s'              =>  $params{dirs},          # -d
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply an infile or directory
    die "You must specify a directory (-d)\n"
        unless ( scalar( @{ $params{dirs} } ) );
    die "You must specify an outfile (-o)\n" unless $params{outfile};
    
    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");
    qlogparams( \%params );

    # Set up output file
    my $outfh = IO::File->new( $params{outfile}, 'w' );
    croak 'Unable to open ', $params{outfile}, " for writing: $!"
        unless defined $outfh;
    $outfh->autoflush(1);
    $outfh->print( join( "\t", qw( AnalysisDir VCF RecordCount
                                   fileformat fileDate
                                   qSource qPatientId
                                   qUUID qAnalysisId
                                   qControlBamUUID qTestBamUUID
                                   qControlBam qTestBam ) ),
                   "\n" );

    # Parse directories
    my $fact = undef;
    if ($mode eq 'qsnp') {
        $fact = QCMG::FileDir::q3QsnpDirParser->new( verbose => $params{verbose} );
    }
    elsif ($mode eq 'gatk') {
        $fact = QCMG::FileDir::q3GatkDirParser->new( verbose => $params{verbose} );
    }

    foreach my $dir (@{ $params{dirs} }) {
        # Arrayref of q3-*-DirRecord objects
        my $ra_analyses = $fact->parse( $dir );
        foreach my $analysis (@{ $ra_analyses }) {
            my $vcffile = $analysis->get_file( 'main_vcf' );
            if (! defined $vcffile) {
                $outfh->print( join("\t", $analysis->dir, 'NoVcf' ), "\n" );
            }
            else {
                my $vcf = QCMG::IO::VcfReader->new(
                              filename => $vcffile->full_pathname,
                              verbose  => $params{verbose} );
                $vcf->slurp;
                my @fields = ( $analysis->dir, $vcffile->name );
                push @fields, $vcf->record_count;
                push @fields, _vcf_header( $vcf, 'fileformat' );
                push @fields, _vcf_header( $vcf, 'fileDate' );
                # Where possible, handle the older format files
                if (_vcf_header( $vcf, 'qSource' ) ne '.') {
                    push @fields, _vcf_header( $vcf, 'qSource' );
                    push @fields, _vcf_header( $vcf, 'qPatientId' );
                }
                else {
                    push @fields, _vcf_header( $vcf, 'source' );
                    push @fields, _vcf_header( $vcf, 'patient_id' );
                }
                push @fields, _vcf_header( $vcf, 'qUUID' );
                push @fields, _vcf_header( $vcf, 'qAnalysisId' );
                push @fields, _vcf_header( $vcf, 'qControlBamUUID' );
                push @fields, _vcf_header( $vcf, 'qTestBamUUID' );
                push @fields, _vcf_header( $vcf, 'qControlBam' );
                push @fields, _vcf_header( $vcf, 'qTestBam' );
                $outfh->print( join("\t", @fields ), "\n" );
            }
        }
    }

    $outfh->close;
    qlogend();
}


sub vcf_info_compare {

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/VCF_INFO_COMPARE' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( infile1  => '',
                   infile2  => '',
                   outfile  => 'vcf_info_compare.txt',
                   logfile  => '',
                   verbose  => 0 );

    my $results = GetOptions (
             'infile1=s'          => \$params{infile1},
             'infile2=s'          => \$params{infile2},
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply two infiles
    die "You must specify --infile1 and --infile2\n"
       unless ( $params{infile1} and $params{infile2} );
    
    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");
    qlogparams( \%params );


    my @in_headers = qw( AnalysisDir VCF RecordCount
                         fileformat fileDate
                         qSource qPatientId
                         qUUID qAnalysisId
                         qControlBamUUID qTestBamUUID
                         qControlBam qTestBam );
    my @out_headers = ( 'Source', @in_headers );

    my $in1 = QCMG::IO::TsvReader->new(
                  filename => $params{infile1},
                  headers  => \@in_headers,
                  verbose  => $params{vernose} );

    my $in2 = QCMG::IO::TsvReader->new(
                  filename => $params{infile2},
                  headers  => \@in_headers,
                  verbose  => $params{vernose} );

    # Roll through both files tallying on donor and bams
    my %analyses = ();
    while (my $line = $in1->next_record) {
        my @fields = split /\t/, $line;
        # Create key from donor and both BAM UUIDs
        next unless ( defined $fields[6] and $fields[6] ne '.' and
                      defined $fields[9] and $fields[9] ne '.' and
                      defined $fields[10] and $fields[10] ne '.');
        my $key = join(' ', $fields[6], $fields[9], $fields[10]);
        push @{ $analyses{$key} }, [ '1', @fields ];
    }
    while (my $line = $in2->next_record) {
        my @fields = split /\t/, $line;
        # Create key from donor and both BAM UUIDs
        next unless ( defined $fields[6] and $fields[6] ne '.' and
                      defined $fields[9] and $fields[9] ne '.' and
                      defined $fields[10] and $fields[10] ne '.');
        my $key = join(' ', $fields[6], $fields[9], $fields[10]);
        push @{ $analyses{$key} }, [ '2', @fields ];
    }

    my $outfh = IO::File->new( $params{outfile}, 'w' );
    croak 'Unable to open ', $params{outfile}, " for writing: $!"
        unless defined $outfh;
    $outfh->print( join( "\t", @out_headers ), "\n" );

    # Now we need to do some reporting.

    # Report 1 - full dump of groups and at the same time, classify each
    # group as one of:
    # a. singleton exome
    # b. singleton genome
    # c. pair exome
    # d. pair genome
    # e. too-many exome
    # f. too-many genome
    # g. unknown

    my %grouped_analyses = ();

    $outfh->print( "[GROUPS]\n" );
    foreach my $key (sort keys %analyses) {
        $outfh->print( "# $key\n" );
        my @records = @{ $analyses{$key} };

        my $caller = 'caller_' . $records[0]->[0];
        my $count = scalar @records;
        # Regex against first qControlBam field to guess type
        my $type = $records[0]->[12] =~ /_NoCapture_/ ? 'genome' :
                   $records[0]->[12] =~ /_HumanAllExon50MbSureSelect_/ ? 'exome_sslct' :
                   $records[0]->[12] =~ /_HumanTruSEQExomeEnrichmentTruSEQ_/ ? 'exome_truseq' :
                   $records[0]->[12] =~ /_SeqCapEZHumanExomeLibraryV30Nimbelgen_/ ?  'exome_nmblgn' :
                   'unknown';

        foreach my $ra_rec (@{ $analyses{$key} }) {
            $outfh->print( join( "\t", @{ $ra_rec } ), "\n" );
        }

        my $category = $count == 1 ? join( ' ', 'singleton', $caller, $type ) :
                       $count == 2 ? join( ' ', 'pair', $type ) :
                       join( ' ', $count, $type );
        push @{ $grouped_analyses{ $category } }, $analyses{$key};
    }

    my @category_headers = ( qw( File Donor AnalysisID 
                                 qControlBamUUID qTestBamUUID ) );
    $outfh->print( "\n\n[CATEGORIES]\n" );
    foreach my $key (sort keys %grouped_analyses) {
        $outfh->print( "\n[Category: $key]\n" );
        $outfh->print( join( "\t", @category_headers ), "\n" );
        foreach my $ra_group (@{ $grouped_analyses{$key} }) {
            foreach my $ra_rec (@{ $ra_group }) {
                $outfh->print( join( "\t", $ra_rec->[0],
                                           $ra_rec->[7],
                                           $ra_rec->[9],
                                           $ra_rec->[10],
                                           $ra_rec->[11] ), "\n" );
            }
            $outfh->print( "\n" );
        }
    }

    @category_headers = ( qw( Type Donor AnalysisID1 AnalysisID2
                              qControlBamUUID qTestBamUUID ) );
    $outfh->print( "\n\n[PAIRS]\n" );
    $outfh->print( join( "\t", @category_headers ), "\n" );
    foreach my $key (sort keys %grouped_analyses) {
        next unless ($key =~ /^pair/);
        foreach my $ra_group (@{ $grouped_analyses{$key} }) {
            $outfh->print( join( "\t", $key,
                                       $ra_group->[0]->[7],
                                       $ra_group->[0]->[9],
                                       $ra_group->[1]->[9],
                                       $ra_group->[0]->[10],
                                       $ra_group->[0]->[11],
                                ), "\n" );
        }
    }

    $outfh->close;
    qlogend();
}


sub vcf_info_old {
    my $mode = shift;

    # This routine is way too slow because it relies on the
    # QSnpDirParser and GatkDirParser classes.  The new q3 versions of
    # both of those classes are way (5x) faster.

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/VCF_INFO' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( mode     => $mode,
                   dirs     => [],
                   outfile  => '',
                   logfile  => '',
                   verbose  => 0 );

    my $results = GetOptions (
           'd|dir=s'              =>  $params{dirs},          # -d
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply an infile or directory
    die "You must specify a directory (-d)\n"
        unless ( scalar( @{ $params{dirs} } ) );
    die "You must specify an outfile (-o)\n" unless $params{outfile};
    
    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");
    qlogparams( \%params );

    my $outfh = IO::File->new( $params{outfile}, 'w' );
    croak 'Unable to open ', $params{outfile}, " for writing: $!"
        unless defined $outfh;
    $outfh->autoflush(1);
    $outfh->print( join( "\t", qw( AnalysisDir VCF RecordCount
                                   fileformat fileDate
                                   qSource qPatientId
                                   qUUID qAnalysisId
                                   qControlBamUUID qTestBamUUID
                                   qControlBam qTestBam ) ),
                   "\n" );

    my $fact = undef;
    if ($mode eq 'qsnp') {
        $fact = QCMG::FileDir::QSnpDirParser->new( verbose => $params{verbose} );
    }
    elsif ($mode eq 'gatk') {
        $fact = QCMG::FileDir::GatkDirParser->new( verbose => $params{verbose} );
    }
    else {
        die "mode [$mode] is not understood";
    }
    
    # Find all relevant variant directories and check for VCF files
    my $header_written = 0;
    foreach my $dir (@{ $params{dirs} }) {
        my $ra_analyses = $fact->parse( $dir );
        foreach my $analysis (@{ $ra_analyses }) {
            my $vcffile = $analysis->get_file( 'main_vcf' );
            if (! defined $vcffile) {
                $outfh->print( join("\t", $analysis->dir, 'NoVcf' ), "\n" );
            }
            else {
                my $vcf = QCMG::IO::VcfReader->new(
                              filename => $vcffile->full_pathname,
                              verbose  => $params{verbose} );
                my $rec_ctr = 0;
                #my $rec1 = $vcf->next_record;
                while ($vcf->next_record_as_line) { $rec_ctr++ };
                my @fields = ( $analysis->dir, $vcffile->name );
                push @fields, $rec_ctr;
                push @fields, _vcf_header( $vcf, 'fileformat' );
                push @fields, _vcf_header( $vcf, 'fileDate' );
                # Where possible, handle the older format files
                if (_vcf_header( $vcf, 'qSource' ) ne '.') {
                    push @fields, _vcf_header( $vcf, 'qSource' );
                    push @fields, _vcf_header( $vcf, 'qPatientId' );
                }
                else {
                    push @fields, _vcf_header( $vcf, 'source' );
                    push @fields, _vcf_header( $vcf, 'patient_id' );
                }
                push @fields, _vcf_header( $vcf, 'qUUID' );
                push @fields, _vcf_header( $vcf, 'qAnalysisId' );
                push @fields, _vcf_header( $vcf, 'qControlBamUUID' );
                push @fields, _vcf_header( $vcf, 'qTestBamUUID' );
                push @fields, _vcf_header( $vcf, 'qControlBam' );
                push @fields, _vcf_header( $vcf, 'qTestBam' );
                $outfh->print( join("\t", @fields ), "\n" );
            }
        }
    }

    $outfh->close;
    qlogend();
}


sub _vcf_header {
    my $vcf  = shift;
    my $name = shift;
    # Return the value if defined and a '.' string otherwise
    return defined $vcf->headers->{$name} ? $vcf->headers->{$name} : '.';
}


sub qsnpdir {

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/QSNPDIR' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( dirs     => [],
                   outfile  => '',
                   logfile  => '',
                   verbose  => 0 );

    my $results = GetOptions (
           'd|dir=s'              =>  $params{dirs},          # -d
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply an infile or directory
    die "You must specify a directory (-d)\n"
        unless ( scalar( @{ $params{dirs} } ) );
    
    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");
    qlogparams( \%params );

    warn "No output file specified\n" unless $params{outfile};

    my $outfh = IO::File->new( $params{outfile}, 'w' );
    croak 'Unable to open ', $params{outfile}, " for writing: $!"
        unless defined $outfh;

    my $fact = QCMG::FileDir::QSnpDirParser->new( verbose => 0 );
    
    # Find all variants/qSNP directories
    my $header_written = 0;
    foreach my $dir (@{ $params{dirs} }) {
        my $ra_qsnps = $fact->parse( $dir );
        foreach my $qsnp (@{ $ra_qsnps }) {
            if (! $header_written) {
                $outfh->print( '#', $qsnp->completion_report_header );
                $header_written = 1;
            }
            $outfh->print( $qsnp->completion_report_text );
        }
    }

    qlogend();
}


sub finalbampairs {

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/FINALBAMPAIRS' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( dirs     => [],
                   aligner  => '',
                   outfile  => '',
                   logfile  => '',
                   verbose  => 0 );

    my $results = GetOptions (
           'd|dir=s'              =>  $params{dirs},          # -d
           'a|aligner=s'          => \$params{aligner},       # -a
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply a directory and output file
    die "You must specify a directory (-d)\n" unless scalar( @{ $params{dirs} } );
    die "You must specify an output file (-o)\n" unless $params{outfile};

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");
    qlogparams( \%params );

    warn "No output file specified\n" unless $params{outfile};

    my $outfh = IO::File->new( $params{outfile}, 'w' );
    croak 'Unable to open ', $params{outfile}, " for writing: $!"
        unless defined $outfh;
    $outfh->print( join("\t", qw( parent_project project ctr bam1 bam2 )), "\n" );

    my $fact = QCMG::QBamMaker::SeqFinalDirectory->new( verbose => $params{verbose} );

    foreach my $dir (@{ $params{dirs} }) {

        # Create QCMG::QBamMaker::SeqFinalBamCollection
        my $coll = $fact->process_directory( $dir );
        my $rh_collections = $coll->new_collections_sorted_by_project_and_donor;

        foreach my $project (sort keys %{ $rh_collections }) {
            foreach my $donor (sort keys %{ $rh_collections->{$project} }) {
                my $this_coll = $rh_collections->{$project}->{$donor};
                qlogprint join( "\t", $project, $donor, 
                                  $this_coll->record_count ), "\n";
                $outfh->print( '[',$project,':',$donor,"]\n" ); 

                # Look for Bams paired for variant calling.  Data 
                # structure is pairs of QCMG::QBamMaker::SeqFinalBam objects
                my $ra_bam_pairs = $this_coll->bams_paired_for_variant_calling;
                my $ctr = 1;
                foreach my $ra_bam_pair (@{ $this_coll->bams_paired_for_variant_calling }) {
                    if ($params{aligner}) {
                       next unless ($ra_bam_pair->[0]->aligner eq $params{aligner});
                    }
                    $outfh->print( join("\t", $project,
                                              $donor,
                                              $ctr++,
                                              $ra_bam_pair->[0]->BamName,
                                              $ra_bam_pair->[1]->BamName), "\n" );
                }
            }
        }

    }

    qlogend();
}


sub testqlogfile {
    qlogbegin();

    my $dir = '/mnt/seq_results/icgc_pancreatic/APGI_1992/variants/qSNP/6bcf8d4d_0394_47df_b9ac_4e488b542e49';

    my $file = QCMG::FileDir::QLogFile->new( file    => "$dir/qsnp.log",
                                             verbose => 0 );


    #print Dumper $file->lines_by_loglevel( 'EXEC' );
    foreach my $rh_line (@{ $file->lines_by_loglevel( 'EXEC' ) }) {
        print '   ',
              join( ' ', $rh_line->{'timestamp'},
                         $rh_line->{'thread'},
                         $rh_line->{'loglevel'},
                         $rh_line->{'class'},
                         $rh_line->{'message'} ),
              "\n";
    }

    print Dumper $file->{unparsed};
    print Dumper $file->attributes_from_exec_lines;

    qlogend();
}


sub autonames {
    qlogbegin();

    my $dir = '/mnt/seq_results/icgc_pancreatic/APGI_1992/variants/';
    my $find = QCMG::FileDir::Finder->new( verbose => 0 );
    my $verf = QCMG::Verify::AutoNames->new( verbose => 0 );

    my @mafs = $find->find_file( $dir, '\.Somatic\.HighConfidence\.snv\.maf$' );

    print Dumper \@mafs;

    qlogprint 'Mafs found ',scalar(@mafs),"\n";

    foreach my $maf (@mafs) {
        print Dumper $maf, $verf->params_from_maf($maf);
    }

    qlogend();
}



sub aligner_from_mapset_bam {

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/ALIGNER_FROM_MAPSET_BAM' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( dir      => '',
                   outfile  => '',
                   logfile  => '',
                   verbose  => 0 );

    my $results = GetOptions (
           'd|dir=s'              => \$params{dir},           # -d
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply a directory and output file
    die "You must specify a directory (-d)\n" unless $params{dir};
    die "You must specify an output file (-o)\n" unless $params{outfile};

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");
    qlogparams( \%params );


    qlogprint( "Looking for BAM files ...\n");

    # Find BAM files
    my $find = QCMG::FileDir::Finder->new( verbose => 0 );
    my @bam_files = $find->find_file( $params{dir}, '\.bam$' );

    qlogprint( "Parsing BAM file headers ...\n");

    # Create output file
    my $outfh = IO::File->new( $params{outfile}, 'w' );
    croak 'Unable to open ', $params{outfile}, " for writing: $!"
        unless defined $outfh;

    # Parse the header of each BAM file and tally the results
    my %tally = ();
    foreach my $bam_file (@bam_files) {
        # We only want mapset-level BAMs
        next unless ($bam_file =~ /\/seq_mapped\//);

        my $bam = QCMG::IO::SamReader->new( filename => $bam_file );
        my $head = QCMG::IO::SamHeader->new( header => $bam->headers_text );

        my $aligner = '';

        my @pgs = @{ $head->PG };
        foreach my $pg (@pgs) {

            # Parse @PG line
            my @fields = split /\t/, $pg;
            my %fields = ();
            foreach my $field (@fields) {
                my ($key,$val) = split /:/, $field, 2;
                $fields{ $key } = $val;
            }

            if (exists $fields{PN} and $fields{PN} =~ /bwa/) {
                # bwa mem runs can only be told by looking at CL:
                if (exists $fields{CL} and $fields{CL} =~ /^bwa\smem/) {
                    $fields{PN} = 'bwamem';
                }

                $aligner = join '_', $fields{PN}, $fields{VN};
                last;  # Once we have a match we can exit the @pgs loop
            }
            elsif (exists $fields{PN} and $fields{PN} =~ /novoalign/) {
                $aligner = join '_', $fields{PN}, $fields{VN};
                last;  # Once we have a match we can exit the @pgs loop
            }
            elsif (exists $fields{PN} and $fields{PN} =~ /LifeScope/) {
                $aligner = join '_', lc($fields{PN}), $fields{VN};
                last;  # Once we have a match we can exit the @pgs loop
            }
            elsif (exists $fields{PN} and $fields{PN} =~ /bowtie/) {
                $aligner = join '_', $fields{PN}, $fields{VN};
                last;  # Once we have a match we can exit the @pgs loop
            }
            elsif (exists $fields{ID} and $fields{ID} =~ /tmap/) {
                $aligner = join '_', 'tmap', $fields{VN};
                last;  # Once we have a match we can exit the @pgs loop
            }
            elsif (exists $fields{ID} and $fields{ID} =~ /MiSeq Reporter/) {
                $aligner = 'miseqreporter';
                last;  # Once we have a match we can exit the @pgs loop
            }
            elsif (exists $fields{ID} and $fields{ID} =~ /Isis/) {
                $aligner = 'isis';
                last;  # Once we have a match we can exit the @pgs loop
            }
            elsif (exists $fields{ID} and $fields{ID} =~ /bioscope-genome-mapping/) {
                $aligner = $fields{VN};
                last;  # Once we have a match we can exit the @pgs loop
            }
        }

        # SOLID/bioscope runs need to use the @RG lines.
        if (! $aligner) {
            my @rgs = @{ $head->RG };
            foreach my $rg (@rgs) {

                # Parse @RG line
                my @fields = split /\t/, $rg;
                my %fields = ();
                foreach my $field (@fields) {
                    my ($key,$val) = split /:/, $field, 2;
                    $fields{ $key } = $val;
                }

                if (exists $fields{PU} and $fields{PU} =~ /bioscope/) {
                    $aligner = $fields{PU};
                    last;  # Once we have a match we can exit the @rgs loop
                }
                elsif (exists $fields{PL} and $fields{PL} =~ /SOLiD/) {
                    $aligner = 'solid';
                    last;  # Once we have a match we can exit the @rgs loop
                }
            }
        }

        # Catch any mapsets where we couldn't get the aligner.
        $aligner = 'unknown' if (! $aligner);

        # Print out the aligner and keep tally
        push @{ $tally{ $aligner } }, $bam_file;
        $outfh->print( "$aligner\t$bam_file\n");
    }
    
    # Log the final tallies
    foreach my $aligner (sort keys %tally) {
        my @found_bams = @{ $tally{ $aligner } };
        qlogprint( scalar(@found_bams), " - $aligner\n" );
    }

    $outfh->close;
    qlogend();
}



sub gatk_variantdir {
    qlogbegin();

    # /mnt/seq_results/icgc_pancreatic/APGI_2185/variants/GATK/d3cba824_6b2f_41e9_bf8b_95027315f298
    my $dir = '/mnt/seq_results/icgc_pancreatic/APGI_2185';
    #my $dir = '/mnt/seq_results/icgc_pancreatic/APGI_3205';
    #my $dir = '/mnt/seq_results/icgc_pancreatic';
    my $fact = QCMG::FileDir::GatkDirParser->new( verbose => 0 );
    my $ra_dirs = $fact->parse( $dir );

    $fact->completion_report( 'icgc_pancreatic_gatk_20130801.txt' );

    #print Dumper $gatk;

    foreach my $dir (@{ $ra_dirs }) {
        print Dumper $dir->maf_details;
    }

    qlogend();
}


__END__

=head1 NAME

dbtools.pl - tool to create tables for QCMGschema


=head1 SYNOPSIS

 dbtools.pl command [options]


=head1 ABSTRACT

This script is a collection of modes that write text file reports
suitable for importing into QCMGschema.


=head1 COMMANDS

 qsnp_vcf_info  - info about qSNP v2.0 VCFs
 gatk_vcf_info  - info about qSNP v2.0 GATK VCFs
 vcf_info_compare - compare qsnp_vcf_info and gatk_vcf_info
 qsnpdir        - progress of qSNP calling
 finalbampairs  - list of callable pairs of BAMs
 aligner_from_mapset_bam - drive aligner from mapset bam
 version        - print version number and exit immediately
 help           - display usage summary
 man            - display full man page


=head1 COMMAND DETAILS

=head2 VCF_INFO_COMPARE

Compare output files from modes qsp_vcf_info and gatk_vcf_info.

      --infile1       qsnp_vcf_info output file
      --infile2       gatk_vcf_info output file
 -o | --outfile       output file
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages

This mode compares output files from modes qsnp_vcf_info and
gatk_vcf_info to try to identify pairs of calls that were done on the
same BAM files.

=head2 VCF_INFO

Process calling directories for VCFs.

 -d | --dir           root directory(s) under which to search
 -o | --outfile       output file
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages

This mode comes in 2 flavours - qsnp_vcf_info and gatk_vcf_info.
It will look for DONOR.vcf files and parse the information out of them.
You can specify multiple search directories.

=head2 QSNPDIR

Process calling directories for qSNP

 -d | --dir           root directory under which to search
 -o | --outfile       output file
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages

This mode starts at the specified directory and recursively parses every 
subdirectory looking for directories called 'qSNP' assuming that for
each donor, qSNP calls are all in directories under 'variants/qSNP'.
All direct
subdirectories of qSNP are parsed as though they were qSNP call
directories and a line is added to the report for each one.

Example 1.  Report on all qSNP calls in icgc_pancreatic:

 dbtools qsnpdir -d /mnt/seq_results/icgc_pancreatic \
                 -o qsnp_completion_report.txt -v

=head2 FINALBAMPAIRS

Identify callable pairs of BAMs

 -d | --dir           root directory under which to search
 -o | --outfile       output file
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages

This mode starts at the specified directory and recursively parses every 
subdirectory looking for seq_final BAMs.  It sorts the BAMs by
parent_project and project and tries to identify "callable pairs" of
BAMs.

=head2 ALIGNER_FROM_MAPSET_BAM

Examine BAM header to determine which aligner was used.

 -d | --dir           root directory under which to search
 -o | --outfile       output file
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages

This mode starts at the specified directory and recursively parses every 
subdirectory looking for seq_mapped BAMs.  It looks at the @PG lines in
the BAM header to work out which aligner was used for the BAM.


=head1 DESCRIPTION


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:grendeloz@gmail.com>

=back


=head1 VERSION

$Id: dbtools.pl 4667 2014-07-24 10:09:43Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013-2014
Copyright (c) QIMR Berghofer Medical Research Institute 2015

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
