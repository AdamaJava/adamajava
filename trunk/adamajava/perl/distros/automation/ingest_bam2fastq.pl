#!/usr/bin/perl

##############################################################################
#
#  Program:  ingest_bam2fastq.pl
#  Author:   Lynn Fink
#  Created:  2012-09-27
#
# Ingest IGN BAM, convert BAM to fastq files, ingest fastq files, ingest log
#
# $Id: ingest_hiseq.pl 1786 2012-04-30 05:53:28Z l.fink $
#
##############################################################################

$ENV{'PERL5LIB'} = "/share/software/QCMGPerl/lib/";

use strict;
use Getopt::Long;
use lib qw(/panfs/home/lfink/devel/QCMGPerl/lib/);
use QCMG::Ingest::Bam2Fastq;

use vars qw($SVNID $REVISION $VERSION);
use vars qw($SLIDE_FOLDER $GLPID $LOG_FILE $LOG_LEVEL $ADD_EMAIL $NO_EMAIL);
use vars qw($USAGE);

# defaults
$NO_EMAIL	= 0;
$GLPID		= 0;

# set command line, for logging
my $cline	= join " ", @ARGV;

&GetOptions(
		"i=s"		=> \$SLIDE_FOLDER,
		"glpid=s"	=> \$GLPID,
		"S!"		=> \$NO_EMAIL,
		"log=s"		=> \$LOG_FILE,
		"loglevel=s"	=> \$LOG_LEVEL,
		"e=s"		=> \$ADD_EMAIL, 
		"V!"		=> \$VERSION,
		"h!"		=> \$USAGE
	);

# help message
if($USAGE || ! $SLIDE_FOLDER ) {
        my $message = <<USAGE;

        USAGE: $0

	Convert IGN BAMs to FASTQ files and ingest both into LiveArc

        $0 -i /path/to/slide/folder/

	Required:
	-i        <dir>  path to slide folder to ingest or just slide name (path will be added)

	Optional:
	-glpid	  <string> Geneus process ID (for setting ingest status flag)
	-log      <file> log file to write execution params and info to
			 [defaults to "slidename_ingest.log" under run folder]
	-loglevel <string> DEBUG: reverts to "/test/" LiveArc namespace
	-e        <email address> additional email addresses to notify of status
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
}

# ADD NO_EMAIL => 1 to suppress emails
my $qi		= QCMG::Ingest::HiSeq2000->new(SLIDE_FOLDER => $SLIDE_FOLDER, LOG_FILE => $LOG_FILE, NO_EMAIL => $NO_EMAIL, GLPID => $GLPID);

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

# get lane and barcode information from the sample sheet
$qi->get_lanes();
$qi->get_barcodes();

# read sample sheet
$qi->read_samplesheet();

# make sure a BAM file exists for each lane and barcode and for each end of
# the pair
$qi->check_bam();
# rename BAM files to our naming scheme 
$qi->rename_bam();
# do checksums on BAMs
$qi->checksum_bam();

# convert BAMs to FASTQ files
$qi->bam2fastq();

# make sure FASTQ files were created for each BAM
$qi->check_fastq();
# do checksums on the FASTQ files
$qi->checksum_fastq();

# ingest supporting files and FASTQ files
$qi->prepare_ingest();
$qi->ingest_samplesheet();
$qi->ingest_bam();
$qi->ingest_fastq();

exit(0);

