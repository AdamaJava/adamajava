#!/usr/bin/perl

##############################################################################
#
#  Program:  ingest_hiseq.pl
#  Author:   Lynn Fink
#  Created:  2011-02-03
#
# Raw sequencing file ingestion script; copies files from sequencing machines to
# archives and cluster for Illumina HiSeq machines
#
# $Id: ingest_hiseq.pl 1786 2012-04-30 05:53:28Z l.fink $
#
##############################################################################

$ENV{'PERL5LIB'} = "/share/software/QCMGPerl/lib/";

use strict;
use Getopt::Long;
use QCMG::Ingest::HiSeq2000;



use vars qw($SVNID $REVISION $VERSION);
use vars qw($SLIDE_FOLDER $GLPID $LOG_FILE $LOG_LEVEL $ADD_EMAIL $NO_EMAIL
		$CASAVA_ARGS $SAMPLESHEET);
use vars qw($USAGE);

# defaults
$NO_EMAIL	= 0;
$GLPID		= 0;
$CASAVA_ARGS	= 0;
$SAMPLESHEET	= 0;

# set command line, for logging
my $cline	= join " ", @ARGV;

&GetOptions(
		"i=s"		=> \$SLIDE_FOLDER,
		"glpid=s"	=> \$GLPID,
		"SILENT!"	=> \$NO_EMAIL,
		"s=s"		=> \$SAMPLESHEET,
		"log=s"		=> \$LOG_FILE,
		"loglevel=s"	=> \$LOG_LEVEL,
		"e=s"		=> \$ADD_EMAIL, 
		"o=s"		=> \$CASAVA_ARGS,
		"V!"		=> \$VERSION,
		"h!"		=> \$USAGE
	);

# help message
if($USAGE || ! $SLIDE_FOLDER ) {
        my $message = <<USAGE;

        USAGE: $0

	Ingest raw sequencing run files into LiveArc

        $0 -i /path/to/slide/folder/

	Required:
	-i        <dir>  path to slide folder to ingest or just slide name (path will be added)

	Optional:
	-s	  <file> path to alternative sample sheet
	-glpid	  <string> Geneus process ID (for setting ingest status flag)
	-log      <file> log file to write execution params and info to
			 [defaults to "slidename_ingest.log" under run folder]
	-loglevel <string> DEBUG: reverts to "/test/" LiveArc namespace
	-e        <email address> additional email addresses to notify of status
	-o	  <string> (in quotes) command line parameters for CASAVA (replaces all defaults)
	-S	  (silent - do not send emails)
	-V        (version information)
        -h        (print this message)

USAGE

        print $message;
        exit(0);
}

umask 0002;

#print STDERR "Slide Folder: $SLIDE_FOLDER\n";

#120511_SN7001243_0090_BD116HACXX
if($SLIDE_FOLDER !~ /\/mnt\/HiSeq\//) {
	# add path if only a slide name is provided
	$SLIDE_FOLDER = '/mnt/HiSeq/runs/'.$SLIDE_FOLDER;
	print STDERR "Guessing at slide folder: $SLIDE_FOLDER\n";

	# try one more time (temporary measure until the disks are remounted)
	if(! -e $SLIDE_FOLDER) {
		$SLIDE_FOLDER =~ s/HiSeq/HiSeqNew/;
		print STDERR "Guessing at slide folder: $SLIDE_FOLDER\n";
	}
}

# ADD NO_EMAIL => 1 to suppress emails
my $qi		= QCMG::Ingest::HiSeq2000->new(SLIDE_FOLDER => $SLIDE_FOLDER, LOG_FILE => $LOG_FILE, NO_EMAIL => $NO_EMAIL, GLPID => $GLPID, CASAVA_ARGS => $CASAVA_ARGS, SAMPLESHEET => $SAMPLESHEET);

if($LOG_LEVEL eq 'DEBUG') {
	$qi->LA_NAMESPACE("test");
	print STDERR "Using LiveArc namespace: ", $qi->LA_NAMESPACE, "\n";
}
else {
	$qi->LA_NAMESPACE("/QCMG_hiseq/");
	print STDERR "Using LiveArc namespace: ", $qi->LA_NAMESPACE, "\n";
}

if($ADD_EMAIL) {
	$qi->add_email(EMAIL => $ADD_EMAIL);
}

# pass command line args for logging
$qi->cmdline(LINE => $cline);
# write start of log file
$qi->execlog();
$qi->toollog();


# check that slide folder can be accessed and that related metadata files exist
# as well
$qi->check_folder();

# log non-unique sheet lines and check for illegal chars
$qi->validate_samplesheet();

# get lane and barcode information from the sample sheet
$qi->get_lanes();
$qi->get_barcodes();

$qi->get_pairing_status();

# convert BCL files to FASTQ files, demultiplexing at the same time
# (can skip this step if demultiplexing has happened already and only the next
# steps need to run)
$qi->bcl2fastq();

# make sure a FASTQ file exists for each lane and barcode and for each end of
# the pair
$qi->check_fastq();

# rename FASTQ files to our naming scheme and move them from Unaligned/ to
# seq_raw/SLIDE/
$qi->rename_fastq();

# do checksums on the FASTQ files
$qi->checksum_fastq();

# check for Core Facility samples
$qi->check_for_core_samples();
# transfer files to surf if core samples exist on thie flowcell
$qi->rsync_to_surf();

# ingest supporting files and FASTQ files
$qi->prepare_ingest();

$qi->ingest_interop();
$qi->ingest_samplesheet();
$qi->ingest_runinfo();
$qi->ingest_runparameters();
$qi->ingest_casava();	# ingest Unaligned/ dir now that FASTQ files are moved
$qi->ingest_fastq();	# ingest each FASTQ file

exit(0);

