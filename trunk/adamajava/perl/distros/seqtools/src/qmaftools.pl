#!/usr/bin/perl -w

##############################################################################
#
#  Program:  qmaftools.pl
#  Author:   John V Pearson
#  Created:  2011-10-18
#
#  Reads variant files (including MAFs) and creates patient-oriented
#  summaries for use in plotting.
#
#  This script is gradually reworking the MAF-related modes that were 
#  previously collected in the variant_summary.pl scipt.  The old code
#  is included (but not executed) in this script - look for the string
#  "Old Stuff" to find the top of the unconverted code.  As old routines
#  are reinplemented, they are deleted from below "old Stuff" and moved
#  above.
#
#  $Id: qmaftools.pl 4687 2014-08-15 00:49:38Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use DBI;
use File::Path qw( make_path );
use Getopt::Long;
use IO::File;
use JSON qw( decode_json );
use Pod::Usage;
use POSIX qw( floor );
use Storable qw(dclone);

use QCMG::FileDir::Finder;
use QCMG::IO::CnvReader;
use QCMG::IO::FastaReader;
use QCMG::IO::GapSummaryReader;
use QCMG::IO::MafReader;
use QCMG::IO::MafWriter;
use QCMG::IO::MafRecordCollection;
use QCMG::IO::SamReader;
use QCMG::IO::VerificationReader;
use QCMG::Util::QLog;
use QCMG::Util::Util qw( db_credentials );
use QCMG::Variants::VariantSummary;

use vars qw( $SVNID $REVISION $CMDLINE $VERBOSE
             $MAF_VAR_PRIORITY $CAT_2_INT $INT_2_CAT
             %CLINICAL_HEADERS );


( $REVISION ) = '$Revision: 4687 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: qmaftools.pl 4687 2014-08-15 00:49:38Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

# Setup global data structures

BEGIN {
    # Quite a few of these data structures are also listed in the POD so
    # if you change a data structure here, please go to the end of the
    # file and modify the POD accordingly.

    # This hash holds the Variation_Classification values from most
    # damaging (1) to least (9).  Where a patient has multiple mutations
    # in a single gene then the most damaging is chosen based on this table.

    $MAF_VAR_PRIORITY = { Frame_Shift_Del   => 1,
                          Frame_Shift_Ins   => 2,
                          Nonsense_Mutation => 3,
                          Nonstop_Mutation  => 4,
                          In_Frame_Del      => 5,
                          In_Frame_Ins      => 6,
                          Missense_Mutation => 7,
                          Splice_Site       => 8,
                          Silent            => 99 };

    # The various variant categorisation schemes (stransky, kassahn, jones,
    # etc) all code the variants as strings.  When outputting these to a
    # matrix for plotting, we need to recode the strings to numbers.
    # This data structure holds that mapping.  If you need to modify one
    # of these, please append a string (e.g. '_replaced_20131028') to the
    # previous scheme and leave it unchanged in the hash so we have a
    # record of the old schemes.
    #
    # N.B. - the descriptions and num`bers below MUST match those used to
    # create the colScheme objects in the R code that plots this stuff.

    $CAT_2_INT  = { 'stransky' =>
                         { 'indel'          => 1,
                           'A -> mut'       => 2,
                           'CpG+ C -> G/A'  => 3,
                           'CpG+ C -> T'    => 4,
                           'CpG- G -> A/C'  => 5,
                           'CpG- G -> T'    => 6 },
                    'synonymous' =>
                         { 'synonymous'     => 1,
                           'non-synonymous' => 2 },
                    'stratton' =>
                         { 'non-silent SNV'       => 1,
                           'CNV'                  => 2,
                           'non-silent SNV + CNV' => 3 },
                    'stratton2' =>
                         { 'non-silent SNV'       => 1,
                           'loss/high-gain CNV'   => 2,
                           'non-silent SNV + loss/high-gain CNV' => 3 },
                    'jones' =>
                         { 'indel'      => 1,
                           'C:G to T:A' => 2,
                           'C:G to G:C' => 3,
                           'C:G to A:T' => 4,
                           'T:A to C:G' => 5,
                           'T:A to G:C' => 6,
                           'T:A to A:T' => 7 },
                    'kassahn' =>
                         { 'A.T -> G.C'      => 1,
                           'CpG- C.G -> T.A' => 2,
                           'CpG+ C.G -> T.A' => 3,
                           'A.T -> C.G'      => 4,
                           'A.T -> T.A'      => 5,
                           'C.G -> G.C'      => 6,
                           'CpG- C.G -> A.T' => 7,
                           'CpG+ C.G -> A.T' => 8,
                           'indel'           => 9 },
                    'quiddell' =>
                         { 'non-silent SNV'                   => 1,
                           'indel'                            => 2,
                           'high-gain (copy number > 5)'      => 3,
                           'loss (copy number < 2)'           => 4,
                           'indel/non-silent SNV + high-gain' => 5,
                           'indel/non-silent SNV + loss'      => 6 },
                    'nones' =>
                         { 'non-silent SNV'                   => 1,
                           'indel'                            => 2,
                           'high-gain (copy number > 5)'      => 3,
                           'loss (copy number < 2)'           => 4,
                           'indel/non-silent SNV + high-gain' => 5,
                           'indel/non-silent SNV + loss'      => 6,
                           'SV'                               => 7,
                           'SV + other mutation'              => 8 },
                    'grimmond' =>
                         { 'indel/non-silent SNV'             => 1,
                           'Amplification (copy number > 4)'  => 2,
                           'Loss (copy number < 2 or CN-LOH)' => 3,
                           'SV'                               => 4,
                           'SV + indel/non-silent SNV/CNV'    => 5,
                           'CNV + indel/non-silent SNV'       => 6 },
                  };

    # This is the exact reverse of $CAT_2_INT so it is created
    # programmatically.

    foreach my $mode (keys %{ $CAT_2_INT }) {
        foreach my $category (keys %{ $CAT_2_INT->{$mode} }) {
            my $int = $CAT_2_INT->{$mode}->{$category};
            $INT_2_CAT->{$mode}->{$int} = $category;
        }
    }

    %CLINICAL_HEADERS = (
        'qcmg_panc' => [ 
                       qw( Patient_ID Institution_Patient_ID Institution
                           DM ETOH Obesity Ethnicity Grade Grade_Code
                           Histology Age Country Xenograft Frozen Cell_line
                           FHx Gender Pack_Years Date_Of_Diagnosis )
                       ],
        'brain_met' => [
                       qw( Donor tumour_type ER_Breast ER_Brain_met
                           HER2_Breast HER2_Brain_met Histological_type )
                       ],
        'brain_met2' => [
                       qw( Donor tumour_type ER_Breast ER_Brain_met
                           HER2_Breast HER2_Brain_met Histological_type
                           Histological_type_2 )
                       ],
        'oeso' => [ qw( Patient Age Sex Alcohol Smoking Reflux ) ],
        'panc_sv' => [ qw( Donor FINAL type ) ],
        'mela' => [ qw( Donor type ) ],
    );

    $VERBOSE = 1;
}


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
                          add_context
                          select
                          recode
                          recode_abo_id
                          condense
                          qinspect
                          tofastq
                          sammd
                          xref
                          compare
                          dcc_filter
                          variant_proportion
                          cnv_matrix
                          clinical
                          project_maf
                          study_group
                          re_maf_compare
                          );

    if ($mode =~ /^help$/i or $mode =~ /\?/) {
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => 'SYNOPSIS|COMMANDS' );
    }
    elsif ($mode =~ /^man$/i) {
        pod2usage(-exitstatus => 0, -verbose => 2)
    }
    elsif ($mode =~ /^version$/i) {
        print "$SVNID\n";
    }
    elsif ($mode =~ /^select/i) {
        mode_select();
    }
    elsif ($mode =~ /^recode/i) {
        mode_recode();
    }
    elsif ($mode =~ /^condense/i) {
        mode_condense();
    }
    elsif ($mode =~ /^qinspect/i) {
        mode_qinspect();
    }
    elsif ($mode =~ /^tofastq/i) {
        mode_tofastq();
    }
    elsif ($mode =~ /^xref/i) {
        mode_xref();
    }
    elsif ($mode =~ /^sammd/i) {
        mode_sammd();
    }
    elsif ($mode =~ /^compare/i) {
        mode_compare();
    }
    elsif ($mode =~ /^dcc_filter/i) {
        mode_dcc_filter();
    }
    elsif ($mode =~ /^variant_proportion/i) {
        mode_variant_proportion();
    }
    elsif ($mode =~ /^recode_abo_id/i) {
        mode_recode_abo_id();
    }
    elsif ($mode =~ /^add_context/i) {
        mode_add_context();
    }
    elsif ($mode =~ /^clinical/i) {
        mode_clinical();
    }
    elsif ($mode =~ /^cnv_matrix/i) {
        mode_cnv_matrix();
    }
    elsif ($mode =~ /^project_maf/i) {
        mode_project_maf();
    }
    elsif ($mode =~ /^study_group/i) {
        mode_study_group();
    }
    elsif ($mode =~ /^re_maf_compare/i) {
        mode_re_maf_compare();
    }
    else {
        die "qmaftools mode [$mode] is unrecognised or unimplemented; valid ".
            'modes are: '.  join(' ',@valid_modes) ."\n";
    }
}


sub mode_project_maf {

    # This mode is supposed to do the following:
    # - identify all donors in a given study group
    # - predict the home directories for the selected donors
    # - search the directories for all MAFs for each donor
    # - where more than one MAF of a given type exists (qSV, pindel,
    #   SNV maf_compare, etc) then make a decision and pick one to be
    #   the representative MAF
    # - conflate the MAFs.
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/PROJECT_MAF' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( study_group => '',
                   outfile     => '',
                   logfile     => '',
                   database    => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'g|study_group=i'      => \$params{study_group},   # -g
           'd|database=s'         => \$params{database},      # -d
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply study group number and outfile name
    die "You must specify a study group\n" unless $params{study_group};
    die "You must specify an output file\n" unless $params{outfile};

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    qlogparams( \%params );

    # Get database credentials from CLI
    my $db_settings = db_credentials( $params{database} );
    
    # Create connection to Geneus Database
    my $connect_string = 'DBI:Pg:dbname='.$db_settings->{database}.';host='.$db_settings->{host};
    my $geneus_conn    = DBI->connect( $connect_string,
                                       $db_settings->{username},
                                       $db_settings->{password},
                                       { RaiseError         => 1,
                                         ShowErrorStatement => 0,
                                         AutoCommit         => 0 } ) ||
                         die "Can not connect to database server: $!\n";
    $geneus_conn->{'ChopBlanks'} = 1; # copes with padded spaces if type=char

    my %sql_stmts = (
        project_prefix => qq{ 
        SELECT 
          project_prefix_map.parent         AS "parent_project",
          project_prefix_map.prefix         AS "project_prefix",
          project_prefix_map.parent_path    AS "parent_project_path",
          project_prefix_map.label          AS "parent_project_label",
          project_prefix_map.array_samples  AS "array_samples"
        FROM qcmg.project_prefix_map
        },
        study_group => qq{ 
        SELECT 
          project.projectid                 AS "id",
          project.name                      AS "project",
          project.parent_project            AS "parent_project",
          project.study_group               AS "study_group"
        FROM qcmg.project
        }
    );


    # Prepare SQL statement for execution.
    my $geneus_cursor = $geneus_conn->prepare($sql_stmts{study_group});
    die "Unable to prepare SQL statement\n" unless $geneus_cursor;

    # Execute prepared SQL statement and fetch records
    my $execute_status = $geneus_cursor->execute();
    die "Unable to execute SQL statement\n" unless (defined $execute_status);
    my $ra_recs = $geneus_cursor->fetchall_arrayref();
    print Dumper $ra_recs;

    # Close database cursor and connection
    if ( $geneus_conn ) {
        $geneus_cursor->finish() if $geneus_cursor;
        $geneus_conn->disconnect();   
    }

    qlogend;
}


sub mode_study_group {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/STUDY_GROUP' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( study_group => '',
                   database    => '',
                   outfile     => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'g|study_group=s'      => \$params{study_group},   # -g
           'd|database=s'         => \$params{database},      # -d
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply study group number and outfile name
    die "You must specify a study group\n" unless $params{study_group};
    die "You must specify an output file\n" unless $params{outfile};

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    qlogparams( \%params );

    # Get database credentials from CLI
    my $db_settings = db_credentials( $params{database} );
    
    # Create connection to Geneus Database
    my $connect_string = 'DBI:Pg:dbname='.$db_settings->{database}.';host='.$db_settings->{host};
    my $geneus_conn    = DBI->connect( $connect_string,
                                       $db_settings->{username},
                                       $db_settings->{password},
                                       { RaiseError         => 1,
                                         ShowErrorStatement => 0,
                                         AutoCommit         => 0 } ) ||
                         die "Can not connect to database server: $!\n";
    $geneus_conn->{'ChopBlanks'} = 1; # copes with padded spaces if type=char

    my %sql_stmts = (
        study_group => qq{ 
        SELECT 
          project.projectid                 AS "id",
          project.name                      AS "project",
          project.parent_project            AS "parent_project",
          project.study_group               AS "study_group"
        FROM qcmg.project
        ORDER BY project
        }
    );


    # Prepare SQL statement for execution.
    my $geneus_cursor = $geneus_conn->prepare($sql_stmts{study_group});
    die "Unable to prepare SQL statement\n" unless $geneus_cursor;

    # Execute prepared SQL statement 
    my $execute_status = $geneus_cursor->execute();
    die "Unable to execute SQL statement\n" unless (defined $execute_status);

    my $ra_recs = $geneus_cursor->fetchall_arrayref();

    # Open the output file
    my $outfh = IO::File->new( $params{outfile}, 'w' );
    die "unable to open file $params{outfile} for writing: $!" unless
        defined $outfh;

    $outfh->print( join("\t", qw( projectid project parent_project
                                  study_group ) ), "\n" );
    my %sg_tally = ();
    foreach my $rec (@{$ra_recs}) {
        if (defined $rec->[3]) {
            my $json = decode_json( $rec->[3] );
            if (exists $json->{ $params{study_group} } ) {
                $outfh->print( join("\t",@{$rec}),"\n" );
            }
            # Overall tally by study_group
            $sg_tally{$_}++ foreach keys %{$json};
        }
    }
    $outfh->close;

    qlogprint "Study Group Report:\n";
    foreach my $sg (sort keys %sg_tally) {
        qlogprint join( "\t", $sg, $sg_tally{$sg} ),"\n";
    }

    # Close database cursor and connection
    if ( $geneus_conn ) {
        $geneus_cursor->finish() if $geneus_cursor;
        $geneus_conn->disconnect();   
    }

    qlogend;
}


# This routine removes json formating from arround a list of study groups.

sub _studygroup_json_to_array {
  my $self        = shift;
  my $studygroups = shift;
  
  # It may not have a value.
  if ($studygroups) {
    my $decoded_json = decode_json( $studygroups );
    my @studygroup = keys %{$decoded_json};
    return \@studygroup;
  }
  
  return '';
}


sub mode_sammd {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/SAMMD' )
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

    # It is mandatory to supply infile and outfile names
    die "You must specify an input file\n" unless $params{infile};
    die "You must specify an output file\n" unless $params{outfile};

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    qlogparams( \%params );
    
    my %variants = ();
    my %samples  = ();

    my $sam = QCMG::IO::SamReader->new(
                  filename => $params{infile},
                  verbose  => $params{verbose} );

    while (my $rec = $sam->next_record_as_record) {

        my @fields = split /:/, $rec->qname;
        die 'This mode only works for SAM files from tofastq mode'
            unless ($fields[0] eq 'QCMG1');

        my $gene       = '';
        my $ref_allele = '';
        my $alt_allele = '';

        if ($fields[2] =~ /^(.*)_([ACGT])\d+([ACGT])$/) {
            $gene       = $1;
            $ref_allele = $2;
            $alt_allele = $3;
        }
        else {
            die 'Unable to parse gene and alleles from string [',
                $fields[1], "]\n";
        }

        # Need to translate chroms 23,24,25 back into X,Y,MT respectively
        my $chrom     = ( $fields[4] == 23 ) ? 'X' :
                        ( $fields[4] == 24 ) ? 'Y' :
                        ( $fields[4] == 25 ) ? 'MT' : $fields[4];
        my $start_pos = $fields[5];
        my $end_pos   = $fields[6];

        # Create unique key from location and variant so we can sort output
        my $key = join(':', $chrom, $start_pos, $end_pos,
                            $ref_allele, $alt_allele );

        # We did this whole exercise to get our hands on the XM and MD
        # records
        my $XM = $rec->tag('XM');
        $XM =~ s/^XM:\w://;
        my $MD = $rec->tag('MD');
        $MD =~ s/^MD:\w://;

        # Store variants away ready for printing
        $variants{ $key } = [ $gene,
                              $chrom, $start_pos, $end_pos,
                              $ref_allele, $alt_allele,
                              $XM, $MD ];
    }

    # Open the output file
    my $outfh = IO::File->new( $params{outfile}, 'w' );
    die "unable to open file $params{outfile} for writing: $!" unless
        defined $outfh;

    # Construct and print output file headers
    my @headers = qw/ gene chrom start end ref alt XM MD /;
    my $header_line = join( "\t", @headers ) . "\n";
    $outfh->print( join( "\t", @headers ) . "\n" );

    # Now print out the variant matrix lines
    foreach my $key (sort keys %variants) {
        my $rec = $variants{$key};
        $outfh->print( join( "\t", @{$rec} ), "\n" );
    }

    $outfh->close;

    qlogend;
}


sub _read_maf_into_location_hash {
    my $infile  = shift;
    my $verbose = shift;

    # This routine should cope with situations where a MAF file contains
    # multiple records at the same position.

    my $maf = QCMG::IO::MafReader->new(
                  filename => $infile,
                  verbose  => $verbose );

    my $count = 0;
    my %recs = ();
    while (my $rec = $maf->next_record) {
        $count++;
       
        # Create unique key from location and variant
        my $key = join(':', $rec->Chromosome,
                            $rec->Start_Position,
                            $rec->End_Position );

        # If this is the first time we've seen this position then create
        # a new array otherwise push this record into the array
        if (! exists $recs{$key}) {
            $recs{$key} = [ $rec ];
        }
        else {
            push @{ $recs{$key} }, $rec;
        }
    }
    qlogprint( "$count records read from MAF $infile\n" );

    return( \%recs, $maf );
}


sub mode_compare {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/COMPARE' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infile1     => '',
                   infile2     => '',
                   label1      => 'File1Only',
                   label2      => 'File2Only',
                   label3      => 'BothFiles',
                   outfile     => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
             'infile1=s'          => \$params{infile1},
             'infile2=s'          => \$params{infile2},
             'label1=s'           => \$params{label1},
             'label2=s'           => \$params{label2},
             'label3=s'           => \$params{label3},
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply infile and outfile names
    die "You must specify 2 input files\n" 
        unless ($params{infile1} and $params{infile2});
    die "You must specify an output file\n" unless $params{outfile};

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    qlogparams( \%params );
    
    my %variants = ();
    my %samples  = ();
    
    # I'm not certain that a MAF can only contain 1 record at a given
    # position so a simple hash of positions won't work - we will need
    # to read both MAF collections into hashes of arrays and then
    # compare the hashes.

    my ( $rh_recs1, $maf1 ) =
        _read_maf_into_location_hash( $params{infile1}, $params{verbose} );
    my $count1 = scalar( keys %{ $rh_recs1 } );
    die "No records found in $params{infile1}\n" unless $count1;

    my ( $rh_recs2, $maf2 ) =
        _read_maf_into_location_hash( $params{infile2}, $params{verbose} );
    my $count2 = scalar( keys %{ $rh_recs2 } );
    die "No records found in $params{infile2}\n" unless $count2;

    # If the versions do not match then it's too hard to keep going
    die( "MAF versions do not match ($maf1->maf_version vs $maf2->maf_version)\n" )
       if ($maf1->maf_version ne $maf2->maf_version);

    # We should output a warning if the column lists are not identical
    my @missing_from_1 = ();
    my @missing_from_2 = ();
    my %cols1 = map { $_ => 1 } $maf1->headers;
    my %cols2 = map { $_ => 1 } $maf2->headers;
    foreach my $key (keys %cols1) {
        push @missing_from_2, $key unless (exists $cols2{ $key });
    }
    foreach my $key (keys %cols2) {
        push @missing_from_1, $key unless (exists $cols1{ $key });
    }

    warn 'These columns are in MAF1 and not MAF2: ',
         join(',',@missing_from_2),"\n" if @missing_from_2;
    warn 'These columns are in MAF2 and not MAF1: ',
         join(',',@missing_from_1),"\n" if @missing_from_1;
         
    # Loop through file2 hash looking for the same key in file1.  If
    # found then we move the matching recs FROM FILE1 to the shared
    # hash and delete them from both of the individual collections.
    # N.B. for shared records, only those from file 1 are kept.

    my %shared = ();
    my @locns = keys %{ $rh_recs2 };
    foreach my $locn (@locns) {
        if (exists $rh_recs1->{$locn}) {
            my @recs = ( @{ $rh_recs1->{$locn} } );
            $shared{ $locn } = \@recs;
            delete $rh_recs1->{$locn};
            delete $rh_recs2->{$locn};
        }
    }

    $count1 = scalar( keys %{ $rh_recs1 } );
    $count2 = scalar( keys %{ $rh_recs2 } );
    my $count3 = scalar( keys %shared );

    qlogprint( "Count of records unique to file 1: $count1\n" );
    qlogprint( "Count of records unique to file 2: $count2\n" );
    qlogprint( "Count of records shared: $count3\n" );

    # Set up any non-standard MAF columns that we want our out MAF to
    # contain, i.e. those from MAF1 plus the new CompareStatus column
    my @extra_fields = ( $maf1->extra_fields, 'CompareStatus' );

    # We want to keep the headers from MAF1 but only the '#Q_*' lines,
    # not the lines that relate to dbSNP percentages etc as they will no
    # longer be true for the compare MAF.
    my @keep_comments = grep { /^#Q_/ } $maf1->comments;
    my $comments = '';
    $comments .= "$_\n" foreach @keep_comments;
    
    # Open the output file
    my $outmaf = QCMG::IO::MafWriter->new(
                    filename      => $params{outfile},
                    version       => $maf1->maf_version,
                    comments      => $comments,
                    extra_fields => \@extra_fields,
                    verbose       => $params{verbose} );

    # Write out the records
    foreach my $ra_recs (values %{ $rh_recs1 }) {
        foreach my $rec (@{ $ra_recs }) {
           # Don't forget to set the CompareStatus extra field
           $rec->extra_field( 'CompareStatus', $params{label1} );
           $outmaf->write( $rec );
        }
    }
    foreach my $ra_recs (values %{ $rh_recs2 }) {
        foreach my $rec (@{ $ra_recs }) {
           # Don't forget to set the CompareStatus extra field
           $rec->extra_field( 'CompareStatus', $params{label2} );
           $outmaf->write( $rec );
        }
    }
    foreach my $ra_recs (values %shared ) {
        foreach my $rec (@{ $ra_recs }) {
           # Don't forget to set the CompareStatus extra field
           $rec->extra_field( 'CompareStatus', $params{label3} );
           $outmaf->write( $rec );
        }
    }
 
    qlogprint( 'Wrote ',$outmaf->record_ctr,' records to ',
               $params{outfile},"\n" );

    qlogend;
}


sub mode_xref {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/XREF' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infiles     => [],
                   outfile     => '',
                   logfile     => '',
                   sammd       => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           =>  $params{infiles},       # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           's|sammd=s'            => \$params{sammd},         # -s
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply infile and outfile names
    die "You must specify an input file\n" unless scalar(@{$params{infiles}});
    die "You must specify an output file\n" unless $params{outfile};

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    qlogparams( \%params );
    
    # Open the sammd input file if present
    my %sammd = ();
    my $sammdfh = IO::File->new( $params{sammd}, 'r' );
    die "unable to open file $params{sammd} for reading: $!" unless
        defined $sammdfh;
    my $header = $sammdfh->getline;
    while (my $line = $sammdfh->getline()) {
        chomp $line;
        my @fields = split "\t", $line;

        #  [ $gene, $chrom, $start_pos, $end_pos,
        #    $ref_allele, $alt_allele, $XM, $MD ]

        # Create unique key from location and variant
        my $key = join(':', $fields[1],
                            $fields[2],
                            $fields[3],
                            $fields[4],
                            $fields[5] );

        $sammd{ $key } = \@fields;
    }

    my %variants = ();
    my %samples  = ();

    foreach my $infile (@{ $params{infiles} }) {

        my $maf = QCMG::IO::MafReader->new(
                      filename => $infile,
                      verbose  => $params{verbose} );

        my $count    = 0;
        while (my $rec = $maf->next_record) {
            $count++;
       
            # Work out what the mutant allele is
            my $alt_allele = ( $rec->Reference_Allele ne $rec->Tumor_Seq_Allele1 ) ?
                               $rec->Tumor_Seq_Allele1 : $rec->Tumor_Seq_Allele2;
            my $ref_allele = $rec->Reference_Allele;

            # Create unique key from location and variant

            my $key = join(':', $rec->Chromosome,
                                $rec->Start_Position,
                                $rec->End_Position,
                                $ref_allele,
                                $alt_allele );

            # Cut down gene name if it has multiple elements
            my $gene = $rec->Hugo_Symbol;
            $gene =~ s/\;.*$//g;

            # If this is the first time we've seen this variant then create slot
            if (! exists $variants{$key}) {
                $variants{$key} = { chrom   => $rec->Chromosome,
                                    start   => $rec->Start_Position,
                                    end     => $rec->End_Position,
                                    gene    => $gene,
                                    refbase => $ref_allele,
                                    altbase => $alt_allele,
                                    samples => { } };
            }

            # Keep a tally of samples we've seen and how often
            $samples{ $rec->Tumor_Sample_Barcode }++;

            # Tease apart allele counts for tumour
            my $pattern = qr/([ACGT]{1}):(\d+)\[([\d\.]+)\],(\d+)\[([\d\.]+)\]/;
            my %alleles = ();
            my $allele_string = $rec->extra_field('TD');
            while ($allele_string =~ m/$pattern/g) {
                $alleles{ $1 } = { base   => $1,
                                   pcount => $2,
                                   pqual  => $3,
                                   ncount => $4,
                                   nqual  => $5,
                                   count  => $2+$4 };
            }

            $variants{$key}->{samples}->{$rec->Tumor_Sample_Barcode} =
                { sample   => $rec->Tumor_Sample_Barcode,
                  refcount => (exists $alleles{ $ref_allele }->{count} 
                              ? $alleles{ $ref_allele }->{count} : 0),
                  altcount => (exists $alleles{ $alt_allele }->{count} 
                              ? $alleles{ $alt_allele }->{count} : 0) };
        }
        qlogprint( "Records read from MAF : $count\n" );
    }

    # Open the output file
    my $outfh = IO::File->new( $params{outfile}, 'w' );
    die "unable to open file $params{outfile} for writing: $!" unless
        defined $outfh;

    my @samples = sort keys %samples;

    # Construct and print output file headers
    my @headers = qw/ gene chrom start end ref alt /;
    push( @headers, $_, '.', '.' ) foreach @samples;
    my @sammd_headers = qw/ gene chrom start end ref alt xm md /;
    push( @headers, @sammd_headers );
    my $header_line = join( "\t", @headers ) . "\n";
    $outfh->print( join( "\t", @headers ) . "\n" );

    # Now print out the variant matrix lines
    foreach my $key (sort keys %variants) {
        my $rec = $variants{$key};
        my @fields = ( $rec->{gene},
                       $rec->{chrom},
                       $rec->{start},
                       $rec->{end},
                       $rec->{refbase},
                       $rec->{altbase} );

        foreach my $sample (@samples) {
            if (exists $rec->{samples}->{$sample}) {
                my $ref_count = $rec->{samples}->{$sample}->{refcount};
                my $alt_count = $rec->{samples}->{$sample}->{altcount};
                push @fields, $ref_count, $alt_count,
                              sprintf( '%.2f', $alt_count/($ref_count+$alt_count) );
            }
            else {
                push @fields, '', '', '';
            }
        }

        # If we have the sammd info, add it in
        if (exists $sammd{$key}) {
            push @fields, @{ $sammd{$key} };
        }

        $outfh->print( join( "\t", @fields ) . "\n" );
    }

    $outfh->close;

    qlogend;
}


sub mode_tofastq {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/TOFASTQ' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infiles     => [],
                   outfile     => '',
                   fasta       => '/panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa',
                   surrounds   => 25,
                   phred       => 33,
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           =>  $params{infiles},       # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'r|fasta=s'            => \$params{fasta},         # -r
           's|surrounds=i'        => \$params{surrounds},     # -s
           'p|phred=i'            => \$params{phred},         # -p
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply infile, outfile and bamfile names
    die "You must specify an input file\n" unless scalar(@{$params{infiles}});
    die "You must specify an output file\n" unless $params{outfile};

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    qlogparams( \%params );
    
    my $ref = QCMG::IO::FastaReader->new(
                  filename => $params{fasta},
                  verbose  => $params{verbose} );

    my $ra_seqs = $ref->sequences;
    my %seqs = ();
    foreach my $seq (@{ $ref->sequences }) {
        my $name = $seq->{defline};
        $name =~ s/\>//;
        $name =~ s/^chr//;
        $name =~ s/\s.*//g;
        $seqs{ $name } = $seq->{sequence};
    }
    foreach my $chr (sort keys %seqs) {
        qlogprint( "  $chr : ", length( $seqs{$chr} ), "\n" );
    }

    # Open the FASTQ output file
    my $outfh = IO::File->new( $params{outfile}, 'w' );
    die "unable to open file $params{outfile} for writing: $!" unless
        defined $outfh;

    my %fastq_recs = ();
    foreach my $infile (@{ $params{infiles} }) {

        # Open this MAF file
        my $maf = QCMG::IO::MafReader->new(
                      filename => $infile,
                      verbose  => $params{verbose} );

        my $count = 0;
        while (my $rec = $maf->next_record) {
            $count++;
       
            # Work out what the mutant allele is
            my $alt_allele = ( $rec->Reference_Allele ne $rec->Tumor_Seq_Allele1 ) ?
                               $rec->Tumor_Seq_Allele1 : $rec->Tumor_Seq_Allele2;

            # Our human reference has mitochondria as chrMT so if the MAF
            # contains any plain "M"s, then do the conversion to MT
            my $chrom = ($rec->Chromosome ne 'M' ?  $rec->Chromosome : 'MT');

            my $seqlen = 2 * $params{surrounds} +1;
            my $seq    = substr( $seqs{ $chrom },
                                 $rec->Start_Position - $params{surrounds} -1,
                                 $seqlen );
            substr($seq,$params{surrounds},1) = $alt_allele;
            my $qual   = chr( $params{phred} + 30 ) x $seqlen;

            # Convert chromosome into an int in range 1-25
            my $tile   = ( $chrom =~ /x/i ) ? 23 :
                         ( $chrom =~ /y/i ) ? 24 :
                         ( $chrom =~ /m/i ) ? 25 :
                           $chrom;

            # Cut down gene name if it has multiple elements
            my $gene = $rec->Hugo_Symbol;
            $gene =~ s/\;.*$//g;

            # Create string to show which base in read is the mutant and
            # what the non-mut base is
            my $mutant = $rec->Reference_Allele .($params{surrounds}+1). $alt_allele;

            my $id = join( ':', '@QCMG1:1',
                                $gene .'_'. $mutant,
                                '1',
                                $tile,
                                $rec->Start_Position,
                                $rec->End_Position );

            print join( "\t", $rec->Tumor_Sample_Barcode,
                              $rec->Matched_Norm_Sample_Barcode,
                              $rec->Hugo_Symbol,
                              $chrom,
                              $rec->Start_Position,
                              $rec->dbSNP_RS,
                              $rec->Reference_Allele,
                              $rec->Tumor_Seq_Allele1,
                              $rec->Tumor_Seq_Allele2,
                              $seq,
                              ), "\n";

            # Save the FASTQ record for this mutant in a hash so across
            # all of the MAF files, each variant will only appear in the 
            # FASTQ file once.
            $fastq_recs{ $id } = join("\n",$id,$seq,'+',$qual)."\n";

        }
        qlogprint( "Records read from MAF : $count\n" );

    }

    # Write out the FASTQ records from hash
    foreach my $key (sort keys %fastq_recs) {
        $outfh->print( $fastq_recs{ $key } );
    }

    $outfh->close;

    qlogend;
}


sub mode_qinspect {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/QINSPECT' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infiles     => [],
                   bamfile     => '',
                   outfile     => '',
                   surrounds   => 100,
                   tumourid    => '',
                   normalid    => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           =>  $params{infiles},       # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'b|bamfile=s'          => \$params{bamfile},       # -b
           's|surrounds=i'        => \$params{surrounds},     # -s
           't|tumourid=s'         => \$params{tumourid},      # -t
           'n|normalid=s'         => \$params{normalid},      # -n
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply infile, outfile and bamfile names
    die "You must specify an input file\n" unless scalar(@{$params{infiles}});
    die "You must specify an output file\n" unless $params{outfile};
    die "You must specify a BAM file\n" unless $params{bamfile};
    die "You must specify either a tumour or normal ID\n"
        unless ($params{tumourid} or $params{normalid});

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    qlogparams( \%params );

    my $mfc_orig = parse_mafs_new( @{$params{infiles}} );
    my $source   = '';
    my @mafrecs  = ();

    if ($params{tumourid}) {
        my $mfc_new = $mfc_orig->filter_by_tumour_ids( [ $params{tumourid} ] );
        push @mafrecs, $mfc_new->records();
        $source = $params{tumourid};
    }
    elsif ($params{normalid}) {
        my $mfc_new = $mfc_orig->filter_by_normal_ids( [ $params{normalid} ] );
        push @mafrecs, $mfc_new->records();
        $source = $params{normalid};
    }

    # Check that the BAM file exists and is readable
    die "BAM file is not readable" unless (-r $params{bamfile});

    my $outfh = IO::File->new( $params{outfile}, 'w' );
    die "unable to open file $params{outfile} for writing: $!" unless
        defined $outfh;

    # Now that we have our MAF, pull out the variant locations and make
    # ranges that can be used to select reads using 'samtools view'
    foreach my $rec (@mafrecs) {
         my $range = 'chr' . $rec->Chromosome .':'.
                     ($rec->Start_Position - $params{surrounds}) .'-'.
                     ($rec->End_Position + $params{surrounds});
         my $cmd = "samtools view $params{bamfile} $range";
         my $recs = `$cmd`;
         $outfh->print( "#QI SOURCE = $source\n",
                        "#QI BAM = $params{bamfile}\n",
                        "#QI RANGE = $range\n",
                        $recs ); 

    }

    $outfh->close;

    qlogend;
}


sub mode_recode {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/RECODE' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infile      => '',
                   outfile     => '',
                   recodefile  => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           => \$params{infile},        # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'r|recodefile=s'       => \$params{recodefile},    # -r
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply infile, outfile and recodefile names
    die "You must specify an input file\n" unless $params{infile};
    die "You must specify an output file\n" unless $params{outfile};
    die "You must specify a recodefile\n" unless $params{recodefile};

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    qlogparams( \%params );

    my $mfc        = parse_maf_new( $params{infile} );
    my $rh_recodes = parse_recodes( $params{recodefile} );

    # We need to process the Tumor and Matched_Norm IDs in separate loops
    # because for either ID, as soon as we get a match in the recodes,
    # we want to stop further matching on that ID.  This is easier to
    # handle if the 2 IDs are processed independently.

    my %recode_tally = ();
    my %not_seen     = ();

    my $tctr = 0;
    my $nctr = 0;
    foreach my $mfr ($mfc->records) {
        if (exists $rh_recodes->{ $mfr->Tumor_Sample_Barcode }) {
            my $new_id = $rh_recodes->{ $mfr->Tumor_Sample_Barcode };
            $tctr++;
            $mfr->Tumor_Sample_Barcode( $new_id );
        }
        else {
            $not_seen{ $mfr->Tumor_Sample_Barcode }++;
        }
        if (exists $rh_recodes->{ $mfr->Matched_Norm_Sample_Barcode }) {
            my $new_id = $rh_recodes->{ $mfr->Matched_Norm_Sample_Barcode };
            $nctr++;
            $mfr->Matched_Norm_Sample_Barcode( $new_id );
        }
        else {
            $not_seen{ $mfr->Matched_Norm_Sample_Barcode }++;
        }
    }

    qlogprint( {l=>'INFO'}, "recoded $tctr Tumor_Sample_Barcode IDs\n" );
    qlogprint( {l=>'INFO'}, "recoded $nctr Matched_Norm_Sample_Barcode IDs\n" );

    my @not_recoded = sort keys %not_seen;
    if (scalar(@not_recoded)) {
        foreach my $id (@not_recoded) {
            my $count = $not_seen{ $id };
            warn "$count records not recoded for $id\n";
        }
    }

    $mfc->write_maf_file( file => $params{outfile} );

    qlogend();
}


sub mode_recode_abo_id {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/RECODE_ABO' )
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

    # It is mandatory to supply infile, outfile and recodefile names
    die "You must specify an input file\n" unless $params{infile};
    die "You must specify an output file\n" unless $params{outfile};

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    qlogparams( \%params );

    my $mfc = parse_maf_new( $params{infile} );

    my $tctr = 0;
    my $nctr = 0;
    foreach my $mfr ($mfc->records) {
        # Recode tumour ID
        my $old_tid = $mfr->Tumor_Sample_Barcode;
        my $new_tid = _recode_abo_id( $old_tid );
        if ($new_tid != $old_tid) {
            $mfr->Tumor_Sample_Barcode( $new_tid );
            $tctr++;
        }
        # Recode normal ID
        my $old_nid = $mfr->Matched_Norm_Sample_Barcode;
        my $new_nid = _recode_abo_id( $old_nid );
        if ($new_nid != $old_nid) {
            $mfr->Matched_Norm_Sample_Barcode( $new_nid );
            $nctr++;
        }
    }
    qlogprint( {l=>'INFO'}, "recoded $tctr Tumor_Sample_Barcode IDs\n" );
    qlogprint( {l=>'INFO'}, "recoded $nctr Matched_Norm_Sample_Barcode IDs\n" );

    $mfc->write_maf_file( file => $params{outfile} );

    qlogend();
}


sub _recode_abo_id {
    my $id = shift;

    # Apparently we can't trust anyone (including us) to have a standard
    # format for their IDs and to stick to it.  Consequently we have to
    # be ready to do ID recoding by matching a *lot* of patterns.

    my %patterns = ( QCMG => qr/^APGI_\d+$/,
                     BCM  => qr/^PACA_\d+$/,
                     OICR => qr/^PCSI\d+$/,
                     TCGA => qr/^TCGA\-\w{2}\-\w{4}$/ );

    # Transform original ID
    my $new_id = $id;
    $new_id =~ s/^\s+//;
    $new_id =~ s/\s+$//;

    # Remember to order pattern matched from most specific to least
    # within a centre or you may not hit the transform you were
    # aiming for.

    if ($new_id =~ $patterns{QCMG} or 
        $new_id =~ $patterns{BCM} or 
        $new_id =~ $patterns{OICR} or
        $new_id =~ $patterns{TCGA}) {
        # Do nothing because ID matches one of the desired patterns
    }
    elsif ($new_id =~ /(PACA[ -]{1}\d+)-T/) {
        $new_id = $1;
        $new_id =~ s/\s/_/g;  # swap out spaces
        $new_id =~ s/-/_/g;   # swap out dashes
    }
    elsif ($new_id =~ /PACA(\d+)(_T)?/) {
        $new_id =~ s/(\d+).*$/_$1/;
    }
    elsif ($new_id =~ /PACA/) {
        $new_id =~ s/\s/_/g;  # swap out spaces
        $new_id =~ s/-/_/g;   # swap out dashes
    }
    elsif ($new_id =~ /((?:PC){0,1}SI\d+)_T/) {
        $new_id = $1;
    }
    elsif ($new_id =~ /PCSI/) {
        $new_id =~ s/\s+//g;
    }
    elsif ($new_id =~ /(APGI_\d+)-ICGC/) {
        $new_id = $1;
    }
    elsif ($new_id =~ /(TCGA\-\w{2}\-\w{4})\-.*/) {
        $new_id = $1;
    }
    else {
        warn "unable to transform unrecognised ID [$id]\n";
        return $id;
    }

    # Check that we were successful
    croak "unsuccessful transformation of ID [$id]\n" unless
        ($new_id =~ $patterns{QCMG} or 
         $new_id =~ $patterns{BCM} or 
         $new_id =~ $patterns{OICR} or
         $new_id =~ $patterns{TCGA});

    return $new_id;
}


sub mode_condense {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/MAFCONDENSE' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infiles     => [],
                   outfile     => '',
                   veriffile   => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           =>  $params{infiles},       # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'r|veriffile=s'        => \$params{veriffile},     # -r
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply infile, outfile and veriffile names
    die "You must specify an input file\n" unless scalar(@{$params{infiles}});
    die "You must specify an output file\n" unless $params{outfile};
    die "You must specify a veriffile\n" unless $params{veriffile};

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    qlogparams( \%params );

    # Process the file specs looking for groups and tags. It's all 
    # or nothing - every file must have groups and tags or none
    # so the first thing to do is check that this is true.

    my $file_count = scalar( @{ $params{infiles} }); 
    my $groups = 0;
    foreach my $fctr (0..($file_count-1)) {
        my $file  = $params{infiles}->[$fctr];
        $groups++ if ($file =~ /.*\:(\d+)\:(\d+)$/);
    }
    die "All MAF files must use the :group:tag notation or none. ".
        "You have $groups files out of $file_count using the notation."
        if ($groups != 0 and $groups != $file_count);

    # Create by-group data structure of files;

    my %files = ();
    foreach my $fctr (0..($file_count-1)) {
        my $file  = $params{infiles}->[$fctr];
        my $group = $fctr+1;
        my $tag   = 1;

        if ($file =~ /.*\:(\d+)\:(\d+)$/) {
            $group = $1;
            $tag   = $2;
            $file =~ s/\:\d+\:\d+$//;
        }

        push @{ $files{ $group } }, [ $file, $group, $tag ];
    }

    # Read the MAFs by group and assign tags

    my %positions = ();
    my $file_text = '';
    my @groups = sort { $a <=> $b } keys %files;

    foreach my $gctr (0..$#groups) {
        my $group = $groups[ $gctr ];

        foreach my $ra_file (@{$files{$group}}) {
            my $file = $ra_file->[0];
            my $tag  = $ra_file->[2];

            $file_text .= "# Group:$group Tag:$tag File:$file\n";

            my $mfc = parse_maf_new( $file );
            foreach my $mfr ($mfc->records) {
                my $pos = 'chr' .
                          $mfr->Chromosome . ':' .
                          $mfr->Start_Position . '-' .
                          $mfr->End_Position;
                if (! exists $positions{$pos}) {
                    $positions{$pos} = [ $mfr->Chromosome,
                                         $mfr->Start_Position,
                                         $mfr->End_Position,
                                         $pos,
                                         '' ] ;
                }
                $positions{$pos}->[ $gctr+5 ] = $tag;
            }
        }
    }

    # Read the results of QCMG's verification program for these MAFs
    # and add results into %positions
 
    my $rh_verifs = parse_verification( $params{veriffile} );
    foreach my $pos (sort keys %{$rh_verifs}) {
        my $verif = $rh_verifs->{$pos};

        # If the verified position is not in any MAF then we have to add
        # a record for it
        if (! exists $positions{$pos}) {
            warn "Position $pos was verified but is not in any MAF\n";
            die "Could not parse position $pos\n" unless
                ($pos =~ /^chr(\w+)\:(\d+)\-(\d+)$/);
            $positions{$pos} = [ $1, $2, $3, $pos, $verif ];
        }
        $positions{$pos}->[4] = $verif;
    }

    write_condense_output( $params{outfile}, \%positions, \@groups,
                           $file_text );

    qlogend();
}


sub write_condense_output {
    my $file         = shift;
    my $rh_positions = shift;
    my $ra_groups    = shift;
    my $file_header  = shift;

    # Get the detailed output file ready
    my $outfh = IO::File->new( $file, 'w' );
    croak "Can't open patient file $file for writing: $!\n"
        unless defined $outfh;

    my @headers = qw( Chromosome Start_Position End_Position
                      ChrPos Verification );
    my @groups = @{ $ra_groups };
    push @headers, 'Group_'.$_ foreach @groups;

    print $outfh "# Condensed MAF output\n#\n" . $file_header . "#\n" .
                 join("\t", @headers) ."\n";

    foreach my $pos (sort keys %{ $rh_positions }) {
        my $rec = $rh_positions->{$pos};
        my @values = @{$rec}[0..4];
        # We have to fill in zeroes for any missing values so we must
        # tediously look at every group value to see if it is defined
        foreach my $ctr (0..$#groups) {
            if (defined $rec->[5+$ctr]) {
                push @values, $rec->[5+$ctr];
            }
            else {
                push @values, 0;
            }
        }
        print $outfh join("\t",@values) . "\n";
    }

    $outfh->close;
}


# N.B. Can't call this routine "select()" because perl gets snippy

sub mode_select {

    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/SELECT' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infiles     => [],
                   outfile     => '',
                   tidfile     => '',
                   nidfile     => '',
                   genefile    => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           =>  $params{infiles},       # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'g|genefile=s'         => \$params{genefile},      # -g
           't|tidfile=s'          => \$params{tidfile},       # -t
           'n|nidfile=s'          => \$params{nidfile},       # -n
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply infile and outfile names
    die "You must specify an input file\n" unless scalar(@{$params{infiles}});
    die "You must specify an output file\n" unless $params{outfile};

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    qlogparams( \%params );

    # If a file has been specified AND if the file actually existed and
    # contained records, then we need to do the filtering.
    
    # Work out which filters have been requested
    my @genes      = parse_genes( $params{genefile} );
    my @tumour_ids = parse_patients( $params{tidfile} );
    my @normal_ids = parse_patients( $params{nidfile} );

    # Exit immediately if no filters requested
    unless (@genes or @tumour_ids or @normal_ids) {
        qlogprint "No filters (-g,-t,-n) specified so no filtering done\n";
        qlogend();
        return;
    }

    my $mfc_orig = parse_mafs_new( @{$params{infiles}} );
    my $mfc_new  = $mfc_orig;

    if (@genes) {
        $mfc_new = $mfc_orig->filter_by_genes( \@genes );
    }
    if (@tumour_ids) {
        # Have to do this so we can run multiple filters consecutively
        my $mfc_old = $mfc_new;
        $mfc_new = $mfc_old->filter_by_tumour_ids( \@tumour_ids );
    }
    if (@normal_ids) {
        # Have to do this so we can run multiple filters consecutively
        my $mfc_old = $mfc_new;
        $mfc_new = $mfc_old->filter_by_normal_ids( \@normal_ids );
    }

    $mfc_new->write_maf_file( file => $params{outfile} );

    qlogend();
}


sub mode_variant_proportion {

    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/VARIANT_PROPORTION' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infiles     => [],
                   outfile     => '',
                   logfile     => '',
                   genefile    => '',
                   germline    => '',
                   tidfile     => '',
                   cnvfile     => '',
                   mode        => 'stransky',
                   factor      => 'patient',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           =>  $params{infiles},       # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'g|genefile=s'         => \$params{genefile},      # -g
           't|tidfile=s'          => \$params{tidfile},       # -t
           'c|cnvfile=s'          => \$params{cnvfile},       # -c
           'r|germline=s'         => \$params{germline},      # -r
           'm|mode=s'             => \$params{mode},          # -m
           'f|factor=s'           => \$params{factor},        # -f
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply infile and outfile names
    die "You must specify an input file\n" unless scalar(@{$params{infiles}});
    die "You must specify an output file\n" unless $params{outfile};

    # mode   = stransky/kassahn/jones/quiddell/stratton/nones etc
    # factor = patient/gene/matrix

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    qlogparams( \%params );

    # Read the snv/indel MAF(s) and create the matrix
    my $mfc = parse_mafs_new( @{$params{infiles}} );

    # Read any sorted gene or donor lists that were given
    my @genes      = parse_genes( $params{genefile} );
    my @tumour_ids = parse_patients( $params{tidfile} );

    # We operate a little differently from here on depending on mode.
    # All modes use SNV/indel for categorisation but some schemes also
    # require CNV and SV information in which case additional files will
    # need to be parsed and processed.

    my $vs = undef;
    if ($params{mode} eq 'stransky') {
        $vs = $mfc->categorise_variants_stransky;
    }
    elsif ($params{mode} eq 'kassahn') {
        $vs = $mfc->categorise_variants_kassahn;
    }
    elsif ($params{mode} eq 'jones') {
        $vs = $mfc->categorise_variants_jones;
    }
    elsif ($params{mode} eq 'synonymous') {
        $vs = $mfc->categorise_variants_synonymous;
    }
    elsif ($params{mode} eq 'stratton') {
        # Stratton plot codes combined SNVs and CNVs
        my $snv_vs = $mfc->categorise_variants;
        my $ra_cnvrecs = parse_cnvs( $params{cnvfile}, \@genes );
        my $cnv_vs = cnv_variant_matrix( $ra_cnvrecs );
        qlogprint "Conflating SNV/indel and CNV calls to make stratton matrix\n";
        $vs = create_stratton_matrix( $snv_vs, $cnv_vs );
    }
    elsif ($params{mode} eq 'stratton2') {
        # Stratton plot codes combined SNVs and CNVs
        my $snv_vs = $mfc->categorise_variants;
        my $ra_cnvrecs = parse_cnvs( $params{cnvfile}, \@genes );
        my $cnv_vs = cnv_variant_matrix( $ra_cnvrecs );
        qlogprint "Conflating SNV/indel and CNV calls to make stratton2 matrix\n";
        $vs = create_stratton2_matrix( $snv_vs, $cnv_vs );
    }
    elsif ($params{mode} eq 'quiddell') {
        # Quiddell plot codes combined SNVs and CNVs
        my $snv_vs = $mfc->categorise_variants_quiddell;
        my $ra_cnvrecs = parse_cnvs( $params{cnvfile}, \@genes );
        my $cnv_vs = cnv_variant_matrix( $ra_cnvrecs );
        qlogprint "Conflating SNV/indel and CNV calls to make quiddell matrix\n";
        $vs = create_quiddell_matrix( $snv_vs, $cnv_vs );
    }
    elsif ($params{mode} eq 'nones') {
        # Nones plot codes combined SNVs, CNVs and SVs
        my $snv_vs = $mfc->categorise_variants_quiddell;
        my $ra_cnvrecs = parse_cnvs( $params{cnvfile}, \@genes );
        my $cnv_vs = cnv_variant_matrix( $ra_cnvrecs );
        my $ra_svrecs = parse_svs( $mfc, \@genes );
        my $sv_vs = sv_variant_matrix( $ra_svrecs );
        qlogprint "Conflating SNV/indel, CNV and SV calls to make nones matrix\n";
        $vs = create_nones_matrix( $snv_vs, $cnv_vs, $sv_vs );
    }
    elsif ($params{mode} eq 'grimmond') {
        # Grimmond plot codes combined SNVs, CNVs and SVs
        my $snv_vs = $mfc->categorise_variants_quiddell;
        my $ra_cnvrecs = parse_cnvs( $params{cnvfile}, \@genes );
        my $cnv_vs = cnv_variant_matrix( $ra_cnvrecs );
        my $ra_svrecs = parse_svs( $mfc, \@genes );
        my $sv_vs = sv_variant_matrix( $ra_svrecs );
        qlogprint "Conflating SNV/indel, CNV and SV calls to make grimmond matrix\n";
        $vs = create_grimmond_matrix( $snv_vs, $cnv_vs, $sv_vs );
    }
    else {
        confess 'mode ',$params{mode},
            " is not recognised; valid values are stransky/kassahn/jones/stratton/quiddell\n";
    }

    # Now write the report
    write_variant_proportion_report( $params{mode},
                                     $params{factor},
                                     $params{outfile},
                                     \@genes,
                                     \@tumour_ids,
                                     $vs );

    qlogend();
}


sub parse_mafs_new {
    my @files = @_;

    my $mfc = QCMG::IO::MafRecordCollection->new( verbose => $VERBOSE );
    foreach my $file (@files) {
        $mfc->add_records( [ parse_maf_new( $file )->records ] );
    }

    return $mfc;
}


sub parse_maf_new {
    my $file = shift;

    #my @mafs = ();
    my $mr = QCMG::IO::MafReader->new( filename => $file,
                                       verbose  => $VERBOSE );
    my $mc = QCMG::IO::MafRecordCollection->new( verbose  => $VERBOSE );
    while (my $rec = $mr->next_record) {
        $mc->add_record( $rec );
    }
    qlogprint 'read '. $mc->record_count . " MAF records from $file\n";

    return $mc;
}


sub parse_patients {
    my $file = shift;

    # Return immediately if the file name is ''
    return () unless $file;

    my $infh = IO::File->new( $file, 'r' );
    croak "Can't open patient file $file for reading: $!\n"
        unless defined $infh;

    my @ids = ();
    while (my $line = $infh->getline) {
        chomp $line;
        next unless $line;      # skip blanks
        next if $line =~ /^#/;  # skip comments
        my @fields = split /\s+/, $line;
        push @ids, $fields[0];
    }

    qlogprint( {l=>'INFO'}, 'loaded '.scalar(@ids)." IDs from $file\n");
    return @ids;
}


sub parse_genes {
    my $file = shift;

    # Return immediately if the file name is ''
    return () unless $file;

    my $infh = IO::File->new( $file, 'r' );
    croak "Can't open gene file $file for reading: $!\n"
        unless defined $infh;

    my @genes = ();
    while (my $line = $infh->getline) {
        chomp $line;
        next unless $line;      # skip blanks
        next if $line =~ /^#/;  # skip comments
        my @fields = split /\s+/, $line;
        push @genes, $fields[0];
    }

    qlogprint( {l=>'INFO'}, 'loaded '.scalar(@genes)." genes from $file\n");
    return @genes;
}


sub parse_recodes {
    my $file = shift;

    my $infh = IO::File->new( $file, 'r' );
    croak "Can't open recode file $file for reading: $!\n"
        unless defined $infh;

    my %recodes = ();
    #my @recodes = ();
    my $good_ctr = 0;
    my $duplicate_ctr = 0;
    while (my $line = $infh->getline) {
        chomp $line;
        next unless $line;      # skip blank lines
        next if $line =~ /^#/;  # skip comments
        my ($key, $val) = split /\s+/, $line, 2;
        # check for duplicate recodes
        if (! exists $recodes{ $key }) {
            $good_ctr++;
            $recodes{$key} = $val;
            #push @recodes, [$key,$val];
        }
        else {
            $duplicate_ctr++;
            warn "duplicate recode [$val] found for [$key]\n";
        }
    }
    qlogprint( {l=>'INFO'}, "loaded $good_ctr recodes from $file\n" );
    if ($duplicate_ctr) {
       qlogprint( {l=>'INFO'},
                  "ignored $duplicate_ctr duplicate recodes from $file\n");
    }

    return \%recodes;
}


sub mode_dcc_filter {
 
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/DCC_FILTER' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infile      => '',
                   outfile     => '',
                   filter      => 1,
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           => \$params{infile},        # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'f|filter=s'           => \$params{filter},        # -f
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply infile and outfile names
    die "You must specify an input file (-i)\n" unless $params{infile};
    die "You must specify an output file (-o)\n" unless $params{outfile};
    die "You must specify a filter (-f)\n" unless $params{outfile};

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    qlogparams( \%params );

    # Get input and output MAFs setup.  Note that we need to use some
    # info form the infile when setting up the outfile so we carry over
    # the headers and get the same version and columns.

    my $inmaf = QCMG::IO::MafReader->new(
                    filename => $params{infile},
                    verbose  => $params{verbose} );

    # We want to keep the headers from MAF1 but only the '#Q_*' lines,
    # not the lines that relate to dbSNP percentages etc as they will no
    # longer be true for the filtered MAF.
    my @keep_comments = grep { /^#Q_/ } $inmaf->comments;
    my $comments = '';
    $comments .= "$_\n" foreach @keep_comments;
    
    my @extra_fields = ( $inmaf->extra_fields );

    my $outmaf = QCMG::IO::MafWriter->new(
                    filename      => $params{outfile},
                    version       => $inmaf->maf_version,
                    comments      => $comments,
                    extra_fields  => \@extra_fields,
                    verbose       => $params{verbose} );

    while (my $rec = $inmaf->next_record) {
        if ($params{filter} == 1) {
            my $compstat = $rec->extra_field( 'CompareStatus' );
            my $validstat = $rec->Validation_Status;
            if (($compstat =~ /BothFiles/i) or
                ($compstat =~ /qSNPOnly/i and $validstat =~ /verified/i)) {
                $outmaf->write( $rec );
            }
        }
        elsif ($params{filter} == 2) {
            my $compstat = $rec->extra_field( 'CompareStatus' );
            my $validstat = $rec->Validation_Status;
            if (($compstat =~ /BothFiles/i) or
                ($compstat =~ /qSNPOnly/i and $validstat =~ /verified/i) or
                ($compstat =~ /GATKOnly/i and $validstat =~ /verified/i)) {
                $outmaf->write( $rec );
            }
        }
        else {
            die "Filter $params{filter} is not implemented\n";
        }
    }
       
    qlogprint 'Read ',$inmaf->record_ctr,' records from ',$params{infile},"\n";
    qlogprint 'Wrote ',$outmaf->record_ctr,' records to ',$params{outfile},"\n";

    qlogend();
}


sub mode_add_context {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/ADD_CONTEXT' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infile      => '',
                   outfile     => '',
                   fasta       => '/panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa',
                   surrounds   => 5,
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           => \$params{infile},        # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'r|fasta=s'            => \$params{fasta},         # -r
           's|surrounds=i'        => \$params{surrounds},     # -s
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply infile and outfile names
    die "You must specify an input file (-i)\n" unless $params{infile};
    die "You must specify an output file (-o)\n" unless $params{outfile};

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    qlogparams( \%params );

    # Get input and output MAFs setup.  Note that we need to use some
    # info from the infile when setting up the outfile so we carry over
    # the headers and get the same version and columns.

    my $inmaf = QCMG::IO::MafReader->new(
                    filename => $params{infile},
                    verbose  => $params{verbose} );

    # We want to keep the headers from MAF1 but only the '#Q_*' lines,
    # not the lines that relate to dbSNP percentages etc as they will no
    # longer be true for the filtered MAF.
    my @keep_comments = grep { /^#Q_/ } $inmaf->comments;
    my $comments = '';
    $comments .= "$_\n" foreach @keep_comments;

    # We may already have CPG as a header in which case we still go
    # through with looking up the context but we will be replacing the
    # existing content, not adding a new column
    
    my $found_cpg_field = 0;
    my @extra_fields = $inmaf->extra_fields;
    foreach my $field (@extra_fields) {
        $found_cpg_field = 1 if ($field eq 'CPG');
    }
    push( @extra_fields, 'CPG') unless $found_cpg_field;

    my $outmaf = QCMG::IO::MafWriter->new(
                    filename     => $params{outfile},
                    version      => $inmaf->maf_version,
                    comments     => $comments,
                    extra_fields => \@extra_fields,
                    verbose      => $params{verbose} );

    my $ref = QCMG::IO::FastaReader->new(
                  filename => $params{fasta},
                  verbose  => $params{verbose} );

    my $ra_seqs = $ref->sequences;
    my %seqs = ();
    foreach my $seq (@{ $ref->sequences }) {
        my $name = $seq->{defline};
        $name =~ s/\>//;
        $name =~ s/^chr//;
        $name =~ s/\s.*//g;
        $seqs{ $name } = $seq->{sequence};
    }

    # Sort must be alphabetic because of sequences with non-numeric names
    foreach my $chr (sort {$a cmp $b} keys %seqs) {
        qlogprint( "Sequence length of $chr is ", length( $seqs{$chr} ), "\n" );
    }

    # Keep a tally of any unknown sequences as this is the most common
    # "missing" value.
    my %missing_chroms = ();

    while (my $rec = $inmaf->next_record) {
        # chrom coords start at 1 but strings start at 0 - be careful
        # with the math for the substring
        my $first  = $rec->Start_Position - $params{surrounds} -1;
        my $length = ($rec->End_Position - $rec->Start_Position + 1) +
                     2 * $params{surrounds};
        my $chrom = $rec->Chromosome;

        # Cope with the annoying "M" vs "MT" mitochondrial problem plus
        # any chroms coded with 23/24/25
        $chrom = ($chrom eq 'M') ? 'MT' :
                 ($chrom eq '23')  ? 'X' :
                 ($chrom eq '24')  ? 'Y' :
                 ($chrom eq '25')  ? 'MT' : $chrom;

        # Tally missing chromosomes
        $missing_chroms{ $chrom }++ unless (exists $seqs{ $chrom });

        # We have to cope if we don't have the info required to add context
        my $oligomer='---';
        if (! defined $first or
            ! defined $length or
            ! defined $chrom or
            ! exists $seqs{ $chrom }) {
            warn 'mode_add_context() skipping MAF record because of missing values: chrom[',
                 $chrom, "] first[$first] length[$length] chromExists[",
                 exists $seqs{ $chrom }, "]\n";
        }
        elsif ( ($first + $length) > length($seqs{ $chrom }) ) {
            warn 'mode_add_context() skipping MAF record because context beyond chrom end - chrom[',
                 $chrom, "] first[$first] length[$length] chromEnd[",
                 length( $seqs{ $chrom } ), "]\n";
        }
        else {
            $oligomer = substr( $seqs{ $chrom }, $first, $length );
        }

        # Add CPG field and value and write record
        $rec->add_extra_field( 'CPG' );
        $rec->extra_field( 'CPG', $oligomer );
        $outmaf->write( $rec );
    }

    # Report any missing chromosomes
    foreach my $missing (sort keys %missing_chroms) {
        warn 'mode_add_context() chromosome ',$missing,
             ' is not in reference but was seen in MAF ',
             $missing_chroms{$missing}," times\n";
    }
       
    qlogprint 'Read ',$inmaf->record_ctr,' records from ',$params{infile},"\n";
    qlogprint 'Wrote ',$outmaf->record_ctr,' records to ',$params{outfile},"\n";

    qlogend();
}


sub mode_clinical {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/CLINICAL' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infile      => '',
                   outfile     => '',
                   logfile     => '',
                   tidfile     => '',
                   mode        => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           => \$params{infile},        # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           't|tidfile=s'          => \$params{tidfile},       # -t
           'm|mode=s'             => \$params{mode},          # -m
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply infile and outfile names
    die "You must specify an input file (-i)\n" unless $params{infile};
    die "You must specify an output file (-o)\n" unless $params{outfile};
    die "You must specify a mode file (-m)\n" unless $params{mode};

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    qlogparams( \%params );

    # Read sorted donor list
    my @tumour_ids = parse_patients( $params{tidfile} );

    my $rh_clinical = parse_clinical( $params{mode}, $params{infile} );
    write_clinical_report( $params{mode},
                           $params{outfile},
                           \@tumour_ids,
                           $rh_clinical );

    qlogend();
}


sub _sorted_genes {
    my $ra_genes = shift;
    my $vs       = shift;  # QCMG::Variants:VariantSummary 

    my @user_supplied_genes = @{ $ra_genes };

    if (scalar(@user_supplied_genes)) {
        # Check that each of the requested genes is available and return 
        # the list as supplied by the user.

        qlogprint scalar(@user_supplied_genes)," genes in user-supplied ordered list\n";
        foreach my $id (@user_supplied_genes) {
            warn "Requested gene [$id] is not present in data\n"
                unless $vs->gene_was_seen($id);
        }
        return @user_supplied_genes;
    }
    else {
        # If the user did not specify a gene list then we sort the
        # list according to the highest variant counts

        my @sorted_genes = map  { $_->[1] }
                           sort { $b->[0] <=> $a->[0] }
                           map  { [ $vs->summary_by_gene($_), $_ ] }
                           $vs->genes;

        qlogprint scalar(@sorted_genes)," genes found in variant summary\n";
        return @sorted_genes;
    }
}


sub _sorted_ids {
    my $ra_ids = shift;
    my $vs     = shift;  # QCMG::Variants:VariantSummary 

    my @user_supplied_ids = @{ $ra_ids };

    if (scalar(@user_supplied_ids)) {
        # Check that each of the requested ids is available and return 
        # the list as supplied by the user.

        qlogprint scalar(@user_supplied_ids)," ids in user-supplied ordered list\n";
        foreach my $id (@user_supplied_ids) {
            warn "Requested patient [$id] is not present in data\n"
                unless $vs->patient_was_seen($id);
        }
        return @user_supplied_ids;
    }
    else {
        # If the user did not specify a patient list then we sort the
        # list according to the highest variant counts

        my @sorted_ids = map  { $_->[1] }
                         sort { $b->[0] <=> $a->[0] }
                         map  { [ $vs->summary_by_patient($_), $_ ] }
                         $vs->patients;

        qlogprint scalar(@sorted_ids)," ids found in variant summary\n";
        return @sorted_ids;
    }
}


# This was previously called maf_var_priority()
sub category_2_integer {
    my $mode = shift;
    my $var  = shift;
    if (exists $CAT_2_INT->{$mode}) {
        if (! defined $var or ! $var) {
            # undef and empty both get coded as ''
            return '';
        }
        elsif (exists $CAT_2_INT->{$mode}->{$var}) {
            return $CAT_2_INT->{$mode}->{$var};
        }
        else {
            # New or external MAFs might contain classifications
            # that are not in our scheme.  If so, warn about it and
            # do nothing, i.e. assign a priority of 98 to the
            # unknown classification so it is effectively ignored.
            warn "MAF variant category [$var] is not in categorisation scheme [$mode]\n";
            return 98;
        }
    }
    else {
        confess "categorisation scheme [$mode] is unknown\n";
    }
}


sub category_order {
    my $mode = shift;
    if (exists $INT_2_CAT->{$mode}) {
        my @cats = map { $INT_2_CAT->{$mode}->{ $_ } }
                   sort keys %{ $INT_2_CAT->{$mode} };
        return @cats;
    }
    else {
        confess "categorisation scheme [$mode] is unknown\n";
    }
}


sub write_variant_proportion_report {
    my $mode     = shift;
    my $factor   = shift;
    my $file     = shift;
    my $ra_genes = shift;  # sorted gene list
    my $ra_ids   = shift;  # sorted tumour ID list
    my $vs       = shift;  # QCMG::Variants::VariantSummary object
    
    # Get the detailed output file ready
    my $outfh = IO::File->new( $file, 'w' );
    croak "Can't open output file $file for writing: $!"
        unless defined $outfh;

    # The order of genes and patients is determined by the lists passed
    # in but for the order of the variant classes, we will use the
    # order defined for each categorisation scheme in global $INT_2_CAT.

    my @sorted_types = category_order( $mode );
    qlogprint 'sorted variants: ',join(',',@sorted_types),"\n";

    # Sort genes by variant total or as per supplied list
    my @sorted_genes = _sorted_genes( $ra_genes, $vs );
    qlogprint 'sorted genes: ',join(',',@sorted_genes),"\n";

    # Sort patients by variant total or as per supplied list
    my @sorted_ids = _sorted_ids( $ra_ids, $vs );
    qlogprint 'sorted patients: ',join(',',@sorted_ids),"\n";

    # Diagnostics
    if ($VERBOSE) {
        no warnings;
        qlogprint "Variant types:\n";
        foreach my $type (@sorted_types) {
            qlogprint "  $type\t" . $vs->summary_by_variant($type) ."\n";
        }
        my $top = 10;
        qlogprint "Top $top patients:\n";
        foreach my $ctr (0..($top-1)) {
            my $patient = $sorted_ids[$ctr];
            qlogprint "  patient $ctr: $patient\t" . $vs->summary_by_patient($patient) ."\n";
        }
        qlogprint "Top $top genes:\n";
        foreach my $ctr (0..($top-1)) {
            my $gene = $sorted_genes[$ctr];
            qlogprint "  gene $ctr: $gene\t" . $vs->summary_by_gene($gene) ."\n";
        }
    }

    # This is the big divergence - by-gene (vertical marginal) vs
    # by-patient (horizontal marginal) plots

    if ($factor eq 'patient') {

        # Print patient IDs as column headers
        print $outfh ',';
        print $outfh join(',', @sorted_ids), "\n";
        # Print variant types
        foreach my $type (@sorted_types) {
            print $outfh $type;
            foreach my $patient (@sorted_ids) {
                print $outfh ',', $vs->summary_by_patient_and_variant( $patient,$type );
            }
            print $outfh "\n";
        }

    }
    elsif ($factor eq 'gene') {

        # Print types as column headers
        print $outfh ',';
        print $outfh join(',', @sorted_types), "\n";
        # Print variant types
        foreach my $gene (@sorted_genes) {
            print $outfh $gene;
            foreach my $type (@sorted_types) {
                print $outfh ',', $vs->summary_by_gene_and_variant( $gene,$type );
            }
            print $outfh "\n";
        }

    }
    elsif ($factor eq 'matrix') {

        # Print patient IDs as column headers
        print $outfh ',';
        print $outfh join(',', @sorted_ids), "\n";

        # Print genes
        foreach my $gene (@sorted_genes) {
            print $outfh $gene;
            foreach my $id (@sorted_ids) {
                # Write out variant or a blank
                my $var = $vs->variant_by_patient_and_gene( $id, $gene );
                # Convert undef to '' for easy of use when printing
                $var = defined $var ? $var : '';
                if ($mode eq 'nocode') {
                    # Return values direct from the matrix without recoding
                    print $outfh ','. $var;
                }
                elsif ($mode) {
                    #qlogprint "gene [$gene]  id [$id]  factor [$factor]  mode [$mode]  var [$var]  ",
                    #          'cat2int [', category_2_integer( $mode, $var ), "]\n";
                    print $outfh ','. category_2_integer( $mode, $var );
                }
                else {
                    confess "Unknown mode [$mode] in [$factor] mode of write_variant_proportion_report()";
                }
            }
            print $outfh "\n";
        }

    }
    else {
        confess "factor $factor is not valid - must be gene/patient";
    }

    $outfh->close;
}


sub parse_clinical {
    my $mode = shift;
    my $file = shift;

    # Pull out the expected_headers
    die "mode [$mode] not supported in parse_clinical\n"
        unless exists $CLINICAL_HEADERS{$mode};
    my @expected_headers = @{ $CLINICAL_HEADERS{$mode} };

    my $infh = IO::File->new( $file, 'r' );
    croak "Can't open clinical file $file for reading: $!\n"
        unless defined $infh;

    my $header_line = $infh->getline;
    chomp $header_line;
    $header_line =~ s/^#//;     # ditch leading '#' char if present
    $header_line =~ s/\s+$//g;  # ditch trailing space
    #my $sep = ',';
    my $sep = "\t";
    my @headers = split /$sep/, $header_line;

    # Check that the headers match
    foreach my $ctr (0..$#expected_headers) {
        if ($headers[$ctr] ne $expected_headers[$ctr]) {
            croak "Invalid header in column [$ctr] - ".
                'should have been ['. $expected_headers[$ctr] .
                '] but is ['. $headers[$ctr] . "]\n";
        }
    }

    # Read the records
    my %clinical = ();
    while (my $line = $infh->getline) {
        chomp $line;
        $line =~ s/\s+$//g;  # ditch trailing space
        my @fields = split /$sep/, $line;

        # Construct hash by patient ID and field type
        my %record = ();
        foreach my $i (0..$#expected_headers) {
            $record{ $expected_headers[$i] } = $fields[$i];
        }
        $clinical{'By_Patient'}->{ $fields[0] } = \%record;
    }

    return \%clinical;
}


sub write_clinical_report {
    my $mode    = shift;
    my $file    = shift;
    my $ra_ids  = shift;
    my $rh_data = shift;

    # Pull out the expected_headers
    die "mode [$mode] not supported in parse_clinical\n"
        unless exists $CLINICAL_HEADERS{$mode};
    my @expected_headers = @{ $CLINICAL_HEADERS{$mode} };

    # Get the detailed output file ready
    my $outfh = IO::File->new( $file, 'w' );
    croak "Can't open output file $file for writing: $!\n"
        unless defined $outfh;
    
    # Sort patients as per supplied list or alphabetically
    my @user_supplied_ids = @{ $ra_ids };
    my @sorted_ids = ();
    if (scalar(@user_supplied_ids)) {
        push @sorted_ids, @user_supplied_ids;
    }
    else {
        push @sorted_ids, sort keys %{$rh_data->{'By_Patient'}};
    }

    # This report will be flipped so patients are columns and attributes
    # are rows

    my $rh_clinical = $rh_data->{'By_Patient'};
    foreach my $attr (@expected_headers) {
        my @vals = ( $attr );
        foreach my $id (@sorted_ids) {
            if (exists $rh_clinical->{$id}->{$attr} and
                defined $rh_clinical->{$id}->{$attr}) {
                push @vals, $rh_clinical->{$id}->{$attr};
            }
            else {
                warn "No value for $attr for patient $id\n";
                push @vals, '';
            }
        }
        print $outfh join(',',@vals),"\n";
    }

    $outfh->close;
}


sub mode_cnv_matrix {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/CNV' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infile      => '',
                   outfile     => '',
                   logfile     => '',
                   genefile    => '',
                   tidfile     => '',
                   mode        => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           => \$params{infile},        # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'g|genefile=s'         => \$params{genefile},      # -g
           't|tidfile=s'          => \$params{tidfile},       # -t
           'm|mode=s'             => \$params{mode},          # -m
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply infile and outfile names
    die "You must specify an input file (-i)\n" unless $params{infile};
    die "You must specify an output file (-o)\n" unless $params{outfile};

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    qlogparams( \%params );

    # Read sorted gene and donor lists
    my @genes      = parse_genes( $params{genefile} );
    my @tumour_ids = parse_patients( $params{tidfile} );

    my $ra_cnvrecs = parse_cnvs( $params{infile}, \@genes );
    my $cnv_vs = cnv_variant_matrix( $ra_cnvrecs );

    write_cnv_report( $params{outfile},
                      \@genes,
                      \@tumour_ids,
                      $cnv_vs );

    qlogend();
}


sub ensg_2_hugo {
    my $file = shift;

    qlogprint( "reading Ensembl gene model GTF file for ENSG-to-gene-symbol mappings\n" );

    my $infh = IO::File->new( $file, 'r' );
    croak 'Unable to open ', $file, " for reading: $!"
        unless defined $infh;

    my %ensg2symbol = ();

    # Read each GTF record, only keep CDS (this keeps the processing
    # load down and if the gene doesn't have a CDS then variants in it
    # can't make the HighConfidenceConsequence.

    my $rec_ctr = 0;
    while (my $line = $infh->getline()) {
        chomp $line;
        my @fields = split /\t/, $line;
        next unless $fields[2] eq 'CDS';
        $rec_ctr++;
        # Weirdly, v70 had a leading space in the final column and this
        # buggered our splits so let's deal with this if present
        $fields[8] =~ s/^\s+//g;
        my @attrs = split /; /, $fields[8];
        $attrs[$#attrs] =~ s/;$//;  # remove final ; not caught by split
        my %attrs = ();
        foreach my $attr (@attrs) {
            my ($key,$val) = split /\s/, $attr, 2;
            $val =~ s/"//g;
            $attrs{$key} = $val;
        }

        # Skip records with empty Ensembl gene_id  or HUGO gene symbol
        next unless (exists $attrs{gene_id} and exists $attrs{gene_name});
        
        $ensg2symbol{ $attrs{gene_id} } = $attrs{gene_name};
    }

    my @ensgs = keys %ensg2symbol;
    qlogprint( scalar(@ensgs), " ENSG ids observed\n" );

    return \%ensg2symbol;
}


sub parse_cnvs {
    my $file     = shift;
    my $ra_genes = shift || [];  # cope if no filtering requested

    my @cnvs = ();
    my $cr = QCMG::IO::GapSummaryReader->new( filename => $file,
                                              verbose  => $VERBOSE );

    # Work out whether we have a gene filter list and do prep work
    my $filter   = 0;
    my %genes    = ();
    my @genes    = @{ $ra_genes };
    if (scalar(@genes)) {
        $genes{$_} = 1 foreach @genes;
        $filter = 1;
    }

    while (my $rec = $cr->next_record) {
        if ($filter) {
            next unless (defined $rec->Gene and
                         exists $genes{ $rec->Gene } );
            push @cnvs, $rec;
        }
        else {
            push @cnvs, $rec;
        }
    }

    qlogprint 'read '. $cr->record_ctr . " CNV records from $file\n";
    if ($filter) {
        qlogprint 'kept '. scalar(@cnvs), " CNV records after filtering by gene\n";
    }

    return \@cnvs;
}


sub parse_svs {
    my $mfc      = shift;
    my $ra_genes = shift || [];  # cope if no filtering requested

    # The SVs will be in a MAF but there might be SNVs/indels as well so
    # we'll need to pull them out.

    # Work out whether we have a gene filter list and do prep work
    my $filter   = 0;
    my %genes    = ();
    my @genes    = @{ $ra_genes };
    if (scalar(@genes)) {
        $genes{$_} = 1 foreach @genes;
        $filter = 1;
    }

    my @svs      = ();
    my $all_recs = 0;
    foreach my $rec ($mfc->records) {
        next unless ($rec->Variant_Classification eq 'SV');
        $all_recs++;
        if ($filter) {
            next unless (defined $rec->Hugo_Symbol and
                         exists $genes{ $rec->Hugo_Symbol } );
            push @svs, $rec;
        }
        else {
            push @svs, $rec;
        }
    }

    qlogprint 'read '. $all_recs . " SV records from MAF file(s)\n";
    if ($filter) {
        qlogprint 'kept '. scalar(@svs), " SV records after filtering by gene\n";
    }

    return \@svs;
}

sub filter_cnvs_by_gene {
    my $ra_recs  = shift;
    my $ra_genes = shift;

    my @keep_records = ();
    if (scalar(@{$ra_genes})) {
        my %genes = ();
        $genes{$_} = 1 foreach @{ $ra_genes };
        my $ctr = 0;
        foreach my $rec (@{$ra_recs}) {
            next unless (defined $rec->Gene and
                         exists $genes{ $rec->Gene } );
            push @keep_records, $rec;
        }
    }

    qlogprint scalar(@keep_records), " CNV records left after filtering by gene\n";

    return \@keep_records;
}


sub cnv_variant_matrix {
    my $ra_cnvrecs = shift;

    # Recode the CNVs into a new object
    my $vs = QCMG::Variants::VariantSummary->new();

    my $count = 0;

    foreach my $cnv (@{ $ra_cnvrecs }) {

        # Cope with multiple versions of the CNV record

        my @patients = ();
        my $gene     = '';
        my $copynum  = '';
        if ($cnv->version eq 'version_2_0') {
            @patients = split /,/, $cnv->Patients;
            $gene     = $cnv->Gene;
            $copynum  = $cnv->CNVChange;
        }
        else {
            warn 'routine cnv_variant_matrix() can not handle '.
                 'GapSummaryRecord objects of version ' . $cnv->version;
            next;
        }

        # Skip any records with "n/a" for gene
        next if ($gene =~ /n\/a/);

        foreach my $id (@patients) {
            # ditch brain-met-specific leading cruft on ids
            $id =~ s/^SLJS_//;
            # Use special CNV version of add_variant() method.
            qlogprint( join( "\t", $id, $gene, $copynum ), "\n" );
            $vs->add_cnv_variant( patient => $id,
                                  gene    => $gene,
                                  variant => $copynum );
            $count++;
        }
    }

    qlogprint( "gene/patient copynumber changes observed: $count\n" );

    return $vs;
}


sub write_cnv_report {
    my $file     = shift;
    my $ra_genes = shift;
    my $ra_ids   = shift;
    my $vs       = shift; # QCMG::Variants::VariantSummary

    # Get the detailed output file ready
    my $outfh = IO::File->new( $file, 'w' );
    croak "Can't open output file $file for writing: $!"
        unless defined $outfh;

    # Sort genes by variant total or as per supplied list
    my @sorted_genes = _sorted_genes( $ra_genes, $vs );
    qlogprint 'sorted genes: ',join(',',@sorted_genes),"\n";

    # Sort ids by variant total or as per supplied list
    my @sorted_ids = _sorted_ids( $ra_ids, $vs );
    qlogprint 'sorted ids: ',join(',',@sorted_ids),"\n";

    # Diagnostics
    if ($VERBOSE) {
        no warnings;
        my $top = 10;
        qlogprint "Top $top CNV genes:\n";
        foreach my $ctr (0..($top-1)) {
            my $gene = $sorted_genes[$ctr];
            qlogprint "  gene $ctr: $gene\t" . $vs->summary_by_gene( $gene ) ."\n";
        }
        qlogprint "Top $top CNV patients:\n";
        foreach my $ctr (0..($top-1)) {
            my $patient = $sorted_ids[$ctr];
            qlogprint "  patient $ctr: $patient\t" . $vs->summary_by_patient( $patient ) ."\n";
        }
    }

    # Print patient IDs as column headers
    print $outfh ',';
    print $outfh join(',', @sorted_ids), "\n";

    # Print genes
    foreach my $gene (@sorted_genes) {
        print $outfh $gene;
        foreach my $patient (@sorted_ids) {
            # Write out variant or a blank
            my $var = $vs->variant_by_patient_and_gene( $patient, $gene );
            # Convert undef to '' for warning-free printing
            $var = defined $var ? $var : '';
            print $outfh ",$var";
        }
        print $outfh "\n";
    }

    $outfh->close;
}


sub sv_variant_matrix {
    my $ra_recs = shift;

    # Recode the SVs into a new object
    my $vs = QCMG::Variants::VariantSummary->new();

    my $count = 0;

    foreach my $rec (@{$ra_recs}) {
        my $gene = $rec->Hugo_Symbol;
        my $id   = $rec->Tumor_Sample_Barcode;
        my $sv   = 1;

        # Skip any records with no gene
        next unless (defined $gene and $gene);

        $vs->add_variant( patient => $id,
                          gene    => $gene,
                          variant => $sv );
        $count++;
    }

    qlogprint( "gene/patient structural variants observed: $count\n" );

    return $vs;
}


sub create_stratton_matrix {
    my $snvs = shift; # QCMG::Variants::VariantSummary
    my $cnvs = shift; # QCMG::Variants::VariantSummary

    # SNVs come in with original MAF coding
    # CNVs come in coded as actual copy number

    # Collapse all SNV and CNV data to 3 numbers:
    # 1 = any non-silent SNV
    # 2 = any CNV
    # 3 = both 1 and 2

    # Recode the SNVs/indels into a new object
    my $new_vs = QCMG::Variants::VariantSummary->new();

    # Non-silent SNV
    foreach my $id ($snvs->patients) {
        foreach my $gene ($snvs->genes) {
            my $var = $snvs->variant_by_patient_and_gene( $id, $gene );

            # Not all patient/gene combinations will have a SNV
            # (obviously) so skip forward if undefined or if there is a
            # variant but it's silent
            next unless (defined $var and $var !~ /^silent$/i);

            $new_vs->add_variant( patient => $id, 
                                  gene    => $gene,
                                  variant => $INT_2_CAT->{stratton}->{1} );
        }
    }
    
    # CNV-only and CNV if it already had a SNV
    foreach my $id ($cnvs->patients) {
        foreach my $gene ($cnvs->genes) {
            my $copynum = $cnvs->variant_by_patient_and_gene( $id, $gene );

            # Not all patient/gene combinations will have a CNV
            # (obviously) so skip forward if undefined 
            next unless defined $copynum;

            # If there was already a SNV then recode, else set code
            my $old_var = $new_vs->variant_by_patient_and_gene( $id, $gene );
            my $new_var = undef;

            if (defined $old_var) {
                $new_var = $INT_2_CAT->{stratton}->{3};
            }
            else {
                $new_var = 'CNV';
                $new_var = $INT_2_CAT->{stratton}->{2};
            }

            # If we found a codeable variant (which we must) set it
            if (defined $new_var) {
                $new_vs->add_variant( patient => $id, 
                                      gene    => $gene,
                                      variant => $new_var );
            }

        }
    }

    return $new_vs;
}


sub create_stratton2_matrix {
    my $snvs = shift; # QCMG::Variants::VariantSummary
    my $cnvs = shift; # QCMG::Variants::VariantSummary

    # SNVs come in with original MAF coding
    # CNVs come in coded as actual copy number

    # Collapse all SNV and CNV data to 3 numbers:
    # 1 = any non-silent SNV
    # 2 = loss/high-gain CNV
    # 3 = both 1 and 2

    # Recode the SNVs/indels into a new object
    my $new_vs = QCMG::Variants::VariantSummary->new();

    # Non-silent SNV
    foreach my $id ($snvs->patients) {
        foreach my $gene ($snvs->genes) {
            my $var = $snvs->variant_by_patient_and_gene( $id, $gene );

            # Not all patient/gene combinations will have a SNV
            # (obviously) so skip forward if undefined or if there is a
            # variant but it's silent
            next unless (defined $var and $var !~ /^silent$/i);

            $new_vs->add_variant( patient => $id, 
                                  gene    => $gene,
                                  variant => $INT_2_CAT->{stratton2}->{1} );
        }
    }
    
    # CNV-only and CNV if it already had a SNV
    foreach my $id ($cnvs->patients) {
        foreach my $gene ($cnvs->genes) {
            my $copynum = $cnvs->variant_by_patient_and_gene( $id, $gene );

            # Not all patient/gene combinations will have a CNV
            # (obviously) so skip forward if undefined 
            next unless defined $copynum;

            # If there was already a SNV then recode, else set code
            my $old_var = $new_vs->variant_by_patient_and_gene( $id, $gene );
            my $new_var = undef;

            if (defined $old_var and $old_var eq $INT_2_CAT->{stratton2}->{1}) {
                if ($copynum =~ /copy-neutral LOH/i) {
                    # do nothing
                }
                elsif ($copynum > 5 or $copynum < 2) {
                    $new_var = $INT_2_CAT->{stratton2}->{3};
                }
            }
            else {
                if ($copynum =~ /copy-neutral LOH/i) {
                    # do nothing
                }
                elsif ($copynum > 5 or $copynum < 2) {
                    $new_var = $INT_2_CAT->{stratton2}->{2};
                }
            }

            # If we found a codeable variant (which we must) set it
            if (defined $new_var) {
                $new_vs->add_variant( patient => $id, 
                                      gene    => $gene,
                                      variant => $new_var );
            }
        }
    }

    return $new_vs;
}


sub create_quiddell_matrix {
    my $snvs = shift; # QCMG::Variants::VariantSummary
    my $cnvs = shift; # QCMG::Variants::VariantSummary

    # SNVs come in coded as: 'indel'
    #                        'non-silent SNV'
    #                        'silent SNV'
    # CNVs come in coded as actual copy number

    # Recode the SNVs/indels into a new object
    my $new_vs = QCMG::Variants::VariantSummary->new();

    foreach my $id ($snvs->patients) {
        foreach my $gene ($snvs->genes) {
            my $var = $snvs->variant_by_patient_and_gene( $id, $gene );
            if (defined $var and $var !~ /^silent SNV$/i) {
                $new_vs->add_variant( patient => $id, 
                                      gene    => $gene,
                                      variant => $var );
            }
        }
    }
    
    # Add in the CNV data
    foreach my $id ($cnvs->patients) {
        foreach my $gene ($cnvs->genes) {
            my $copynum = $cnvs->variant_by_patient_and_gene( $id, $gene );

            # Cope with LOH
            if (defined $copynum and $copynum =~ /copy-neutral LOH$/) {
                $copynum = 2;
                #warn "redefining copy-neutral LOH as 2 for $id / $gene\n";
            }

            # Not all patient/gene combinations will have a CNV
            # (obviously) so skip forward if empty
            next unless defined $copynum;

            # If there was already a SNV/indel then work out the new code
            my $old_var = $new_vs->variant_by_patient_and_gene( $id, $gene );
            my $new_var = undef;

            if (defined $old_var) {
                if ($old_var eq 'non-silent SNV') {
                    if ($copynum > 5) {
                        $new_var = 'indel/non-silent SNV + high-gain';
                    }
                    elsif ($copynum < 2) {
                        $new_var = 'indel/non-silent SNV + loss';
                    }
                }
                elsif ($old_var eq 'indel') {
                    if ($copynum > 5) {
                        $new_var = 'indel/non-silent SNV + high-gain';
                    }
                    elsif ($copynum < 2) {
                        $new_var = 'indel/non-silent SNV + loss';
                    }
                }
            }
            else {
                if ($copynum > 5) {
                    $new_var = 'high-gain (copy number > 5)';
                }
                elsif ($copynum < 2) {
                    $new_var = 'loss (copy number < 2)';
                }
            }

            # If we found a codeable variant, regardless of whether it
            # was a new CNV variant or a recode of a SNP variant, we
            # need to set the new_variant
            if (defined $new_var) {
                $new_vs->add_variant( patient => $id, 
                                      gene    => $gene,
                                      variant => $new_var );
            }

        }
    }

    return $new_vs;
}


sub create_nones_matrix {
    my $snvs = shift; # QCMG::Variants::VariantSummary
    my $cnvs = shift; # QCMG::Variants::VariantSummary
    my $svs  = shift; # QCMG::Variants::VariantSummary

    # In draft form, this routine is exactly like the quiddell matrix
    # code with SVs overlaid at the end but this could easily change in
    # the future.

    # SNVs come in coded as: 'indel'
    #                        'non-silent SNV'
    #                        'silent SNV'
    # CNVs come in coded as actual copy number
    # SVs come in coded as '1'
    #
    # Output MUST be in the scheme as it appears in CAT_2_INT

    # Recode the SNVs/indels into a new object
    my $new_vs = QCMG::Variants::VariantSummary->new();

    foreach my $id ($snvs->patients) {
        foreach my $gene ($snvs->genes) {
            my $var = $snvs->variant_by_patient_and_gene( $id, $gene );
            if (defined $var and $var !~ /^silent SNV$/i) {
                $new_vs->add_variant( patient => $id, 
                                      gene    => $gene,
                                      variant => $var );
            }
        }
    }
    
    # Add in the CNV data
    foreach my $id ($cnvs->patients) {
        foreach my $gene ($cnvs->genes) {
            my $copynum = $cnvs->variant_by_patient_and_gene( $id, $gene );

            # Cope with LOH
            if (defined $copynum and $copynum =~ /copy-neutral LOH$/) {
                $copynum = 2;
                #warn "redefining copy-neutral LOH as 2 for $id / $gene\n";
            }

            # Not all patient/gene combinations will have a CNV
            # (obviously) so skip forward if empty
            next unless defined $copynum;

            # If there was already a SNV/indel then work out the new code
            my $old_var = $new_vs->variant_by_patient_and_gene( $id, $gene );
            my $new_var = undef;

            if (defined $old_var) {
                if ($old_var eq 'non-silent SNV') {
                    if ($copynum > 5) {
                        $new_var = 'indel/non-silent SNV + high-gain';
                    }
                    elsif ($copynum < 2) {
                        $new_var = 'indel/non-silent SNV + loss';
                    }
                }
                elsif ($old_var eq 'indel') {
                    if ($copynum > 5) {
                        $new_var = 'indel/non-silent SNV + high-gain';
                    }
                    elsif ($copynum < 2) {
                        $new_var = 'indel/non-silent SNV + loss';
                    }
                }
            }
            else {
                if ($copynum > 5) {
                    $new_var = 'high-gain (copy number > 5)';
                }
                elsif ($copynum < 2) {
                    $new_var = 'loss (copy number < 2)';
                }
            }

            # If we found a codeable variant, regardless of whether it
            # was a new CNV variant or a recode of a SNP variant, we
            # need to set the new_variant
            if (defined $new_var) {
                $new_vs->add_variant( patient => $id, 
                                      gene    => $gene,
                                      variant => $new_var );
            }

        }
    }
    
    # Add in the SV data
    foreach my $id ($svs->patients) {
        foreach my $gene ($svs->genes) {
            my $var = $svs->variant_by_patient_and_gene( $id, $gene );

            # Not all patient/gene combinations will have a SV
            # (obviously) so skip forward if empty
            next unless defined $var;

            # If there is already a SNV/indel/SV then work out the new code
            my $old_var = $new_vs->variant_by_patient_and_gene( $id, $gene );
            my $new_var = 'SV';

            $new_var = 'SV + other mutation' if (defined $old_var);

            $new_vs->add_variant( patient => $id, 
                                  gene    => $gene,
                                  variant => $new_var );
        }
    }
    
    return $new_vs;
}


sub create_grimmond_matrix {
    my $snvs = shift; # QCMG::Variants::VariantSummary
    my $cnvs = shift; # QCMG::Variants::VariantSummary
    my $svs  = shift; # QCMG::Variants::VariantSummary

    # This routine is base on create_nones_matrix().

    # SNVs come in coded as: 'indel'
    #                        'non-silent SNV'
    #                        'silent SNV'
    # CNVs come in coded as actual copy number
    # SVs come in coded as '1'
    #
    # Output MUST be in the scheme as it appears in CAT_2_INT
    # 
    #  { 'indel/non-silent SNV'             => 1,
    #    'Amplification (copy number > 4)'  => 2,
    #    'Loss (copy number < 2 or CN-LOH)' => 3,
    #    'SV'                               => 4,
    #    'SV + indel/non-silent SNV/CNV'    => 5,      
    #    'CNV + indel/non-silent SNV'       => 6 },

    # We are going to build a big old hash where for each id/gene
    # combination we have a hash of all of the observed variants.  This
    # lets us do the final categorisation by looking for the legal
    # combination PLUS we can identify any combinations that are not
    # coped with in our categorisation scheme.

    my %vars = ();

    foreach my $id ($snvs->patients) {
        foreach my $gene ($snvs->genes) {
            my $var = $snvs->variant_by_patient_and_gene( $id, $gene );
            next unless defined $var;
            if ($var !~ /^silent SNV$/i) {
                $vars{ $id }->{ $gene }->{ 'indel/non-silent SNV' }++;
            }
        }
    }
    
    # Add in the CNV data
    foreach my $id ($cnvs->patients) {
        foreach my $gene ($cnvs->genes) {
            my $var = $cnvs->variant_by_patient_and_gene( $id, $gene );
            next unless defined $var;

            # Cope with LOH
            if ($var =~ /copy-neutral LOH$/) {
                $var = 'Loss (copy number < 2 or CN-LOH)';
                $vars{ $id }->{ $gene }->{ $var }++;
            } 
            elsif ($var > 4) {
                $var = 'Amplification (copy number > 4)';
                $vars{ $id }->{ $gene }->{ $var }++;
            }
            elsif ($var < 2) {
                $var = 'Loss (copy number < 2 or CN-LOH)';
                $vars{ $id }->{ $gene }->{ $var }++;
            }
        }
    }
    
    # Add in the SV data
    foreach my $id ($svs->patients) {
        foreach my $gene ($svs->genes) {
            my $var = $svs->variant_by_patient_and_gene( $id, $gene );
            next unless defined $var;
            $vars{ $id }->{ $gene }->{ 'SV' }++;
        }
    }

    # Recode the SNVs/indels/CNV/SVs into a new object
    my $new_vs = QCMG::Variants::VariantSummary->new();
    foreach my $id (keys %vars) {
        foreach my $gene (keys %{ $vars{$id} }) {
            my $here = $vars{$id}->{$gene};
            my @variants = keys %{$vars{$id}->{$gene}};
            # The order of tests in this structure is critical because
            # you have to test the more complicated combinations BEFORE
            # falling through to test for the simpler single-variant
            # scenarios.
            if ( exists $here->{'SV'} and
                    ( exists $here->{'indel/non-silent SNV'} or
                      exists $here->{'Amplification (copy number > 4)'} or
                      exists $here->{'Loss (copy number < 2 or CN-LOH)'} )
                      ) {
                $new_vs->add_variant( patient => $id, 
                                      gene    => $gene,
                                      variant => 'SV + indel/non-silent SNV/CNV' );
            }
            elsif (scalar(@variants) > 2) {
                warn "unable to categorise $id/$gene in grimmond scheme: ",
                     join(',',@variants),"\n"; 
            }
            elsif ( exists $here->{'indel/non-silent SNV'} and
                    ( exists $here->{'Amplification (copy number > 4)'} or
                      exists $here->{'Loss (copy number < 2 or CN-LOH)'} )
                      ) {
                $new_vs->add_variant( patient => $id, 
                                      gene    => $gene,
                                      variant => 'CNV + indel/non-silent SNV' );
            }
            elsif ( exists $here->{'indel/non-silent SNV'} ) {
                $new_vs->add_variant( patient => $id, 
                                      gene    => $gene,
                                      variant => 'indel/non-silent SNV' );
            }
            elsif ( exists $here->{'SV'} ) {
                $new_vs->add_variant( patient => $id, 
                                      gene    => $gene,
                                      variant => 'SV' );
            }
            elsif ( exists $here->{'Amplification (copy number > 4)'} ) {
                $new_vs->add_variant( patient => $id, 
                                      gene    => $gene,
                                      variant => 'Amplification (copy number > 4)' );
            }
            elsif ( exists $here->{'Loss (copy number < 2 or CN-LOH)'} ) {
                $new_vs->add_variant( patient => $id, 
                                      gene    => $gene,
                                      variant => 'Loss (copy number < 2 or CN-LOH)' );
            }
            else {
                warn "unable to categorise $id/$gene in grimmond scheme: ",
                     join(',',@variants),"\n"; 
            }
        }
    }

    return $new_vs;
}


sub parse_verification {
    my $file = shift;

    my $vcr = QCMG::IO::VerificationReader->new( filename => $file );

    # Subsequent logic assumes that each record has the field 'ChrPos'
    # in position 2 and field 'Verification' in position 39 so we should
    # check the headers to make sure this is actually true.

    die "Column 2 of verification file must contain 'ChrPos'\n"
       unless ($QCMG::IO::VerificationReader::VALID_COLUMNS[2] =~
               /ChrPos/i) ;
    die "Column 38 of verification file must contain 'Verification'\n"
       unless ($QCMG::IO::VerificationReader::VALID_COLUMNS[38] =~
               /Verification/i) ;

    my %verifs = ();
    while (my $ra_fields = $vcr->next_record) {
        $verifs{ $ra_fields->[2] } = $ra_fields->[38];
    }
    qlogprint( {l=>'INFO'}, 'loaded ' . $vcr->record_count .
                            " records from $file\n" );
    return \%verifs;
}


sub mode_re_maf_compare {

    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/RE_MAF_COMPARE' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( directory   => '',
                   copydir     => '',
                   outfile     => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'd|directory=s'        => \$params{directory},     # -d
           'c|copydir=s'          => \$params{copydir},       # -c
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply some input params
    die "You must specify a directory\n" unless $params{directory};
    die "You must specify a copydir\n" unless $params{copydir};
    die "You must specify an output file\n" unless $params{outfile};

    die "directory parameter must be an absolute path\n"
        unless ($params{directory} =~ /^\//);

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    qlogparams( \%params );

    # Check that the copydir is writable
    die 'Unable to write to copydir [',$params{copydir},"]\n"
        unless (-w $params{copydir});

    # Open the output file
    my $outfh = IO::File->new( $params{outfile}, 'w' );
    die "unable to open file $params{outfile} for writing: $!" unless
        defined $outfh;
    $outfh->print( join(' ',qw( donor qsnp_uuid gatk_uuid )), "\n" );

    my $finder = QCMG::FileDir::Finder->new( verbose => $params{verbose} );

    my @dirs = $finder->find_directory( $params{directory}, 'maf_compare' );

    foreach my $dir (@dirs) {
        my @compdirs = $finder->find_directory( $dir, '_vs_' );
        #qlogprint "  $_\n" foreach @compdirs;
        # Parse directory
        foreach my $compdir (@compdirs) {
            if ($compdir =~ /.*\/([^\/]*)\/variants\/maf_compare\/(.*)_vs_(.*)$/) {
                qlogprint "processing $compdir\n";
                my $donor     = $1;
                my $qsnp_uuid = $2;
                my $gatk_uuid = $3;
                
                my $donor_dir = $compdir;
                $donor_dir =~ s/maf_compare\/.*//;
                my $qsnp_dir = $donor_dir . "qSNP/$qsnp_uuid";
                my $gatk_dir = $donor_dir . "GATK/$gatk_uuid";
                warn "qSNP dir not found - $qsnp_dir\n" unless (-r $qsnp_dir);
                warn "GATK dir not found - $gatk_dir\n" unless (-r $gatk_dir);

                my $qsnp_dir_copy = $params{copydir} .'/'. $qsnp_dir;
                my $gatk_dir_copy = $params{copydir} .'/'. $gatk_dir;
                my $compdir_copy  = $params{copydir} .'/'. $compdir;

                make_path( $qsnp_dir_copy, $gatk_dir_copy, $compdir_copy );
                system( "cp $qsnp_dir/* $qsnp_dir_copy" );
                system( "cp $gatk_dir/* $gatk_dir_copy" );
                system( "cp $compdir/*  $compdir_copy" );

                $outfh->print( join(' ',$donor,$qsnp_uuid,$gatk_uuid), "\n" );
            } 
            else {
                confess "unable to parse directory [$compdir]\n";
            }
        }
    }

    qlogend;
}


__END__

=head1 NAME

qmaftools.pl - Manipulate and query MAF files


=head1 SYNOPSIS

 qmaftools.pl command [options]


=head1 ABSTRACT

This script operates on MAF files including producing a range of report
CSV files for plotting.  The command-line interface is patterned on samtools
so the first CLI param is the command which is followed by any options
specific to that command.  The full documentation for this script is on
the QCMG wiki at http://qcmg-wiki.imb.uq.edu.au/index.php/Qmaftools.pl


=head1 COMMANDS

 add_context    - get genomic surrounds for SNV variants
 compare        - compare records from2 MAF files
 condense       - create a variant summary report from multiple MAFs
 dcc_filter     - select records based on validation and CompareStatus
 qinspect       - create PDFs of variants from a MAF file
 recode         - recode ID numbers based on supplied table
 sammd          -
 select         - select records based on list of genes and/or patients
 tofastq        - create synthetic reads from MAF file variants
 xref           - create a TSV that collates variants from different samples

 variant_counts
 variant_proportion
 cnv_matrix
 clinical

 project_maf    - create conflated MAF for donors for a study group ID
 study_group    - show donors for a given study group ID
 re_maf_compare - prepare to rerun qverify/maf_compare

 version        - print version number and exit immediately
 help           - display usage summary
 man            - display full man page


=head1 COMMAND DETAILS

=head2 RECODE

 -i | --infile        MAF input file
 -o | --outfile       MAF output file
 -r | --recodefile    Text file containing ID recode pairs
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages

MAF files use sample IDs as identifiers but in pretty much every other
case we use donor (patient) IDs as identifiers so this mode was created
to allow us to rewrite MAF files to use donor IDs.
You will need to supply a recodefile which is a simple text file of
pairs of old and new IDs.  The file can contain comments and blank
lines.  You should expect to have to supply codes for both the 
Tumor_Sample_Barcode and Matched_Norm_Sample_Barcode columns (16 and
17) as can be seen in this example recode file from the Brain Met
project (some rows have been elided ... for clarity):

 ...
 # Tumor_Sample_Barcode recodes
 QCMG-66-SLJS_Q030-SMGres-SLJS-20130628-004 Q030_T
 QCMG-66-SLJS_Q178-SMGres-SLJS-20120912-001 Q178_T
 QCMG-66-SLJS_Q349-SMGres-SLJS-20130628-010 Q349_T
 ...
 # Matched_Norm_Sample_Barcode recodes
 # QCMG-66-SLJS_Q030-SMGres-SLJS-20130628-006 Q030_N
 # QCMG-66-SLJS_Q178-SMGres-SLJS-20120912-002 Q178_N
 # QCMG-66-SLJS_Q349-SMGres-SLJS-20130628-012 Q349_N
 ...

You should always check the logfile which will output warnings for any
records in the MAF files where you did not supply a recoded ID.  The new
MAF file will get created in any case but it will contain the old IDs
for any IDs that were missing from the recode file.

=head2 RECODE_ABO_ID

 -i | --infile        MAF input file
 -o | --outfile       MAF output file
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages

This mode was written to support the Australia, Baylor, OICR (ABO)
collaboration on pancreatic cancer plus data from TCGA.
The collaborators all use different ID schemes and even within a 
centre, the ID schemes can differ slightly from file to file.
This mode attempts to provide a one-stop-shop for recoding ABO IDs.
The patterns matched and recoded are very specific to the ABO centres 
so this mode is probably of no use for data from any other centres.

The following 4 regex patterns show the desirable ID formats for MAFs
from various sources:

 QCMG => qr/^APGI_\d+$/
 BCM  => qr/^PACA_\d+$/
 OICR => qr/^PCSI\d+$/
 TCGA => qr/^TCGA\-\w{2}\-\w{4}$/

In most cases, the IDs in the MAFs are very similar to the desired
patterns and removing spaces, replacing dashes with underscores and
removing unwanted trailing text will be enough.  You need to bear these
transforms in mind as any ID that you place in a B<--tidfile> or
B<--nidfile> file must match the MAF ID but the MAF IDs *may* be 
transformed.
A warning is output if an ID from a patient file is not represented in 
the MAF so it is sensible to watch for these warnings and if all or most
of your requested IDs are not in the MAF then you may have an ID format 
mismatch rather than true absence of mutations.  Caveat emptor.

=head2 MAFCONDENSE

 -i | --infile        MAF input files
 -o | --outfile       CSV output file
 -r | --veriffile     Text file containing verification results
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages

=head2 SELECT

 -i | --infile        MAF input files
 -o | --outfile       MAF output file
 -t | --tidfile       ordered list of patient tumour IDs
 -n | --nidfile       ordered list of patient normal IDs
 -g | --genefile      ordered list of genes (optional)
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages

This mode lets you filter a MAF file and discard any records that don't
match supplied lists of genes, tumour IDs or normal IDs.  This is very
useful as a precursor to the variant_matrix_*() class of modes which
produce matrices ready for plotting.

See the section B<Common Commandline Options> for detailed descriptions
of the formats required for the input files.

=head2 QINSPECT

 -i | --infile        MAF input file(s)
 -o | --outfile       SAM output file
 -b | --bamfile       BAM file to pull records from
 -s | --surrounds     number of bases to include either side of variant
 -t | --tumourid      exact match for tumour ID of MAF records to pull
 -n | --normalid      exact match for normal ID of MAF records to pull
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages

=head2 TOFASTQ

 -i | --infile        MAF input file
 -o | --outfile       FASTQ output file
 -r | --fasta         FASTA reference file used to get sequence for reads
 -s | --surrounds     number of bases to include either side of variant
 -p | --phred         system for creating base qual chars; default=33
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages

This mode takes a MAF file and a FASTA file containing the genome
sequence tat the MAF relates to and it creates a FASTQ file with a read
for each MAF.  The read includes --surrounds bases before and after the
variant. This FASTQ file can be used for aligning against other
sequences, e.g. to test whether the variants align perfectly against
another organism (e.g. mouse) and therefore represent contamination
rather than true variants.

=head2 SAMMD

 -i | --infile        SAM input file
 -o | --outfile       Summary output file in tsv format
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages

This mode extract information from SAM files that are cretaed by
aligning FASTQ files that were by mode B<tofastq>.  Can be used to work
out how well the N<tofastq> reads aligned.  Useful when assessing
whether variants in a MAF are due to contamination by another organism
rather than real mutations in the sample.

=head2 PROJECT_MAF

 -g | --study_group   Study Group (string)
 -d | --database      database connection string
 -o | --outfile       Output file in MAF format
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages

This mode takes a study group (e.g. Pancreas_StudyID_12 for PDAC),
finds all of the
donors for the project and looks through their variants directories to
try to create a single MAF for thwe project.  If more than one pair of
BAMs has been variant called for a given donor and variant type (SNP,
indel, SV etc) then one MAF will have to be chosen.
The --database string lists host, database, username and
password separated by ':::', e.g.

 mydb.mydomain.com:::mydatabase:::username:::password

=head2 STUDY_GROUP

 -g | --study_group   Study Group (string, required)
 -d | --database      database connection string
 -o | --outfile       outfile file (required)
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages

This mode takes a study group (e.g. Pancreas_StudyID_12 for PDAC),
and lists all of the donors that contain this study group.  Note that
donors may have multiple study groups.  A full summary of all study
groups is always output so if you don't know what string you need to use
to get your study group, put in a made-up string and you'll get to see
the list.
The --database string is as per mode PROJECT_MAF.

=head2 XREF

 -i | --infile        MAF input file(s)
 -o | --outfile       XREF file in tsv format
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages

=head2 COMPARE

      --infile1       MAF input file 1
      --infile2       MAF input file 2
      --label1        label for records only in file 1
      --label2        label for records only in file 2
      --label3        label for records in both files
 -o | --outfile       MAF file with extra fields
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages

This mode was originally created for processing SNVs where we have MAF
files from qSNP and GATK and we want to overlap them.

The three --labelX commandline parameters are all optional and all have
default values:

 --label1  => File1Only
 --label2  => File2Only
 --label3  => BothFiles

=head2 DCC_FILTER

 -i | --infile        MAF input file
 -o | --outfile       MAF ouput file
 -f | --filter        filter to apply
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages

The mode reads a MAF file and outputs selected records to a new MAF
file.  The criteria used for the selection depends on the filter
applied and you must specify a filter or the program will exit
immediately.  The list of available filters is:

 1. As used for ICGC release 14 (August 2013) - two classes of records
    are sent to the output MAF:
    a. records that are called by both callers (qSNP and GATK) as shown
       by the string "BothFiles" in the CompareStatus field
    b. records that have "qSNPonly" CompareStatus but that have
       Validation_Status of "verified" (usually meaning that qverify has
       been run and there is RNA or other confirmatory evidence of the
       variant)

 2. A special filter used in release 17 (August 2014) when trying to 
    manually drag forward icgc_pancreatic SNPs that were verified using 
    MiSeq deep amplicon sequencing over KRAS - three classes of records
    are sent to the output MAF:
    a. records that are called by both callers (qSNP and GATK) as shown
       by the string "BothFiles" in the CompareStatus field
    b. records that have "qSNPonly" CompareStatus but that have
       Validation_Status of "verified"
    c. records that have "GATKonly" CompareStatus but that have
       Validation_Status of "verified"

=head2 ADD_CONTEXT

 -i | --infile        MAF input file
 -o | --outfile       MAF ouput file
 -r | --fasta         Reference genome FASTA file
 -s | --surrounds     Number of bases around SNP to be output
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages

This mode takes a MAF file of variants and pulls out the genomic
context around the variants.
The context is a number of bases before and after the variant (-s).
Note that this sequence is always an odd number (5 base surrounds
= 5 bases beforeu plus the variant position plus 5 bases
after) and the sequence output is the reference and does NOT
contain the variant base - it a straight substring cut out of the
reference genome.
The original use case was to enable us to categorise variants as
occuring or not occuring at CpG islands.

=head2 VARIANT_PROPORTION

 -m | --mode          mode; stransky/kassahn/jones etc
 -i | --infile        MAF input file
 -o | --outfile       report file (CSV)
 -l | --logfile       log file (optional)
 -g | --genefile      list of genes
 -t | --tidfile       list fo tumour ID
 -c | --cnvfile
 -f | --factor
 -v | --verbose       print progress and diagnostic messages

There are multiple different ways to calculate variant proportions
depending on which scheme is used to categorise the variants.
The schemes are each implemented s a mode here and the current valid
modes are:
 
 stransky
 kassahn
 jones
 synonymous
 stratton
 quiddell - SNVs + CNVs
 nones    - SNVs + CNVs + SVs
 grimmond - SNV (somatic & germline) + CNVs + SVs

=head3 Worst Consequence

One nasty little complication is that the MAF files could contain more
than one variant called for a given gene and patient because of multiple
transcripts at the same position or multiple variants at different
positions.  The plotted matrices are gene by patient so we can only show
one colored box regardless of how many variants are reported.
In such cases, we have decided to always show the variant with the worst
consequence, so we need a mechanism for
choosing the most-damaging or "worst" consequence.  In these cases, the
following table is used to assign scores to each mutation type where the
smaller the score, the worse the consequence.

 Frame_Shift_Del   => 1
 Frame_Shift_Ins   => 2
 Nonsense_Mutation => 3
 Nonstop_Mutation  => 4
 In_Frame_Del      => 5
 In_Frame_Ins      => 6
 Missense_Mutation => 7
 Splice_Site       => 8
 Silent            => 99 

Regardless of whether we are going to use the Stransky, Kassahn or some
other categorisation scheme for the matrix, we will choose "worst 
consequence" based on the table above.

=head3 Modes

=head4 mode : stransky

This mode produces a CSV that could be used to reproduce the variant
proportion plot from the Stransky et at 2011 
Science paper on head and neck squamous cell carcinoma.  The 12 base
substitutions are grouped into 5 categories of which 4 depend on the CpG
status of the substituted base, plus a 6th category for indels:
 
  1  indel                        (insertions, deletions)
  2  A -> mut                     (A->[CGT], T->[GCA])
  3  CpG+ -> G/A                  (C->G, C->A, G->C, G->T)
  4  CpG+ -> T                    (C->T, G->A)
  5  CpG- G->A/C                  (G->A, G->C, C->T, C->G)
  6  CpG- G->T                    (G->T, C->A)

The odd thing about this scheme is that the mutations in the CpG 
categories (3, 4) are grouped differently from the mutations in the
non-CpG categories (5, 6) which makes it difficult to compare figures
from this mode to those from other modes.
  
=head4 mode : kassahn

This mode is similar to B<stransky> except that it
uses a 9-category system proposed by Karin Kassahn:

 A. Transitions (~66%):

  1  A.T -> G.C                   (A->G, T->C)
  2  C.G -> T.A CpG-              (C->T, G->A)
  3  C.G -> T.A CpG+              (C->T, G->A)

 B. Transversions (~33%):

  4  A.T -> C.G                   (A->C, T->G)
  5  A.T -> T.A                   (A->T, T->A)
  6  C.G -> G.C                   (C->G, G->C)
  7  C.G -> A.T CpG-              (C->A, G->T)
  8  C.G -> A.T CpG+              (C->A, G->T)

 C. Indels

  9  Insertions & Deletions

We expect higher frequency of category 3 due to high rate of mutation of
methylated cytosines to thymine; also a signature of UV damage (along
with CC>TT).

=head4 mode : jones

The 2008 Science paper by Jones S et al. on "Core Signaling Pathways in
Human Pancreatic Cancers Revealed by Global Genomic Analyses" provided a
table where the 12 substitutions were divided into 6 pairs:

 A. Substitutions at C:G base pairs 

  1  C:G to T:A                   (C->T, G->A)
  2  C:G to G:C                   (C->G, G->C)
  3. C:G to A:T                   (C->A, G->T)

 B. Substitutions at T:A base pairs 

  4  T:A to C:G                   (T->C, A->G)
  5  T:A to G:C                   (T->G, A->C)
  6  T:A to A:T                   (T->A, A->T)

To this, we will have to add a 7th category for indels.  Additionally
they had a separate analysis which showed 2 categories of substitutions
at specific dinucleotides:

  1  5'-CpG-3' 
  2  5'-TpC-3'
 
=head4 mode : synonymous

=head4 mode : stratton

=head4 mode : quiddell

This mode combines SNVs + CNVs.

=head4 mode : nones

This mode combines SNVs + CNVs + SVs

=head4 mode : grimmond

This mode combines SNVs (somatic & germline) + CNVs + SVs.  The coding
scheme is:

1. Point mutations in non-silent
2. COPY number of 5 or greater as amplification
3. LOSS, CNLOH and HD as LOSS
4. SV alone
5. SV and Mutation or SV and CNV
6. CNV and mutation


=head3 Worst Consequence

=head1 COMMON COMMANDLINE OPTIONS

Many of the qmaftools.pl modes use the same set of commandline options
so common options are documented here rather than having the
descriptions replicated within the documentation for each mode.

=over

=item B<-c | --cnvfile>

QCMG-format text file containing summary copy number variant data
for each patient from the GAP software. This file comes in multiple
versions - see the POD for class QCMG::IO::GapSummaryRecord to see the
current formats.

=item B<-d | --clinical>

Garvan-format text file containing clinical information for each
patient.

=item B<-a | --clnattfile>

Text file listing the clinical attributes to be included in the analysis.
The attributes must appear one per line and the order in which they appear
is used as the order in which attributes appear in the output file.  The
attribute names must B<exactly> match the column headers used in the
clinical CSV data file (--clinical).

=item B<-p | --patfile>

Text file listing the IDs of patients to be included in the analysis.
The IDs must appear one per line and the order in which the IDs appear
is used as the order in which patients appear in the output file.  If
you are going to plot the data from multiple runs of variant_summary.pl
on the same plot then you almost certainly want to establish one of
these files early so all the output files have the same patients in the
same order.  Also note that in an attempt to standardise IDs, all ID's
in the MAF are subject to transformation so that they conform to a
standard pattern.  See the section Patient IDs below for more details.

=item B<-g | --genefile>

Text file listing the names of genes to be included in the analysis.
The IDs must appear one per line and the order in which the IDs appear
is used as the order in which genes appear in the output file.  If
you are going to plot the data from multiple runs of variant_summary.pl
on the same plot then you almost certainly want to establish one of
these files early so all the output files have the same genes in the
same order.

=item B<-o | --outfile>

CSV output file.  Patients are always columns ordered either
alphabetically or according to the order established by B<--patfile>.

=item B<-l | --logfile>

Optional log file name.  If this option is not specified then logging
goes to STDOUT.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.

=item B<--version>

Print the script version and exit immediately.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back

=head2 RE_MAF_COMPARE

 -d | --directory     absolute parent project directory
 -c | --copydir       directory to hold copies
 -o | --outfile       outfile
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages

This mode makes copies of all existing maf_compare directories as well as
producing a list of the qSNP and GATK directories that were analysed to
create the maf_compare directories.  The --directory parameter must be
an absolute pathname (starts with '/'), not a relative pathname.

An example invocation that copies all of the maf_compare directories
from the BrainMet project to the invoking user's tmp/ directory:

 qmaftools.pl re_maf_compare -d /mnt/seq_results/smgres_brainmet/ \
                             -c ~/tmp \
                             -o brainmet_20140506.txt

=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: qmaftools.pl 4687 2014-08-15 00:49:38Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011-2014

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
