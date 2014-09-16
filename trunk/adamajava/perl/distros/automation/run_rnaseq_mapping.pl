#!/usr/bin/perl

##############################################################################
#
#  Program:  run_rnaseq_mapping.pl
#  Author:   Lynn Fink
#  Created:  2013-03-19
#
# Run all necessary software to map/analyse RNAseq data from HiSeqs
#
# $Id: run_rnaseq_mapping.pl 3807 2013-05-20 01:05:39Z l.fink $
#
##############################################################################

use strict;
use Getopt::Long;
#use lib qw(/share/software/QCMGPerl/lib/);
#use lib qw(/panfs/home/lfink/devel/QCMGPerl/lib/);
use QCMG::RNAseq::Run;

use vars qw($SVNID $REVISION $VERSION);
use vars qw($SLIDE_FOLDER $LOG_FILE $LOG_LEVEL $ADD_EMAIL $SAMPLESHEET $FORCE_MAP);
use vars qw($USAGE);

$FORCE_MAP	= 'N';	# default

# set command line, for logging
my $cline	= join " ", @ARGV;

&GetOptions(
		"i=s"		=> \$SLIDE_FOLDER,
		"s=s"		=> \$SAMPLESHEET,
		"f!"		=> \$FORCE_MAP,
		"log=s"		=> \$LOG_FILE,
		"loglevel=s"	=> \$LOG_LEVEL,
		"e=s"		=> \$ADD_EMAIL, 
		"V!"		=> \$VERSION,
		"h!"		=> \$USAGE
	);

# help message
if($USAGE || (! $SLIDE_FOLDER) ) {
        my $message = <<USAGE;

        USAGE: $0

	Check raw sequencing files and run RNAseq mapping pipeline if they are ok

        $0 -i /panfs/seq_raw/slide_folder -log logfile.log

        $0 -i slide_folder 

	Required:
	-i        <name> directory name where raw sequencing data is or slidename (/panfs/seq_raw/ will be prepended)
	-log      <file> log file to write execution params and info to

	Optional:
	-s	  <file> path and filename of a non-default sample sheet (for unusual situations)
	-f        (force mapping of slide, even if LIMS indicates otherwise)
	-V        (version information)
        -h        (print this message)

USAGE

        print $message;
        exit(0);
}

umask 0002;

if($SLIDE_FOLDER !~ /seq\_raw/) {
	#$SLIDE_FOLDER	= "/panfs/seq_raw/".$SLIDE_FOLDER;
	$SLIDE_FOLDER	= "/mnt/seq_raw/".$SLIDE_FOLDER;
}

$FORCE_MAP = 'Y' if($FORCE_MAP);

my $rb          = QCMG::RNAseq::Run->new(SLIDE_FOLDER => $SLIDE_FOLDER, LOG_FILE => $LOG_FILE, FORCE_MAP => $FORCE_MAP);

# set non-default sample sheet, if specified
$rb->samplesheet(FILE => $SAMPLESHEET) if($SAMPLESHEET);

if($LOG_LEVEL eq 'DEBUG') {
        $rb->LA_NAMESPACE("test");
        print STDERR "Using LiveArc namespace: ", $rb->LA_NAMESPACE, "\n";
}
else {
        $rb->LA_NAMESPACE("/QCMG_hiseq/");
}

if($ADD_EMAIL) {
        $rb->add_email(EMAIL => $ADD_EMAIL);
}

### pass command line args for logging
$rb->cmdline(LINE => $cline);
# write start of log file
$rb->execlog();
$rb->toollog();
###

# check that folder exists and is read/writable
$rb->check_folder();

# check that seq_results directory exists to copy BAMs to
$rb->check_project_dir();

# find all fastq files in seq_raw
$rb->find_fastq_files();

# create PBS script files and ingest them
$rb->create_bwaqc_pbs_scripts();

# create PBS script files and ingest them
$rb->create_rsemmapsplice_pbs_scripts();

# submit jobs
$rb->run_master();

exit(0);

