#!/usr/bin/perl

##############################################################################
#
#  Program:  extract_mapped.pl
#  Author:   Lynn Fink
#  Created:  2011-10-03
#
# Mapped file extraction script; copies files from LiveArc to babyjesus
#
# $Id: extract_mapped.pl 2660 2012-08-06 06:34:22Z l.fink $
#
##############################################################################

use strict;
use Getopt::Long;
use QCMG::Automation::LiveArc;
use QCMG::Automation::Common;
use QCMG::DB::Metadata;
use File::Spec;
use Data::Dumper;

use vars qw($SVNID $REVISION $VERSION);
use vars qw($ASSETNAME $EXTRACTTO $LOG_FILE $LOG_LEVEL $ADD_EMAIL $UPDATE);
use vars qw($USAGE);

# set command line, for logging
my $cline	= join " ", @ARGV;

&GetOptions(
		"i=s"		=> \$ASSETNAME,	# root name
		"o=s"		=> \$EXTRACTTO,
		"log=s"		=> \$LOG_FILE,
		"loglevel=s"	=> \$LOG_LEVEL,
		"e=s"		=> \$ADD_EMAIL, 
		"V!"		=> \$VERSION,
		"h!"		=> \$USAGE
	);

$EXTRACTTO	= "/mnt/seq_results/";	# default
$LOG_FILE	= "/panfs/automation_log/".$ASSETNAME."_extractmapped.log";

# help message
#	-u	  only extract assets that have not yet been extracted
if($USAGE || (! $ASSETNAME) ) {
        my $message = <<USAGE;

        USAGE: $0

	Extract mapped sequencing run files from LiveArc 

        $0 -i assetname  -log logfile.log

	$0 -i S17009_20120113_2_LMP.lane_3.nobc

	Required:
	-i        <name> LiveArc asset ID to extract
	-log      <file> log file to write execution params and info to

	Optional:
	-V        (version information)
        -h        (print this message)

USAGE

        print $message;
        exit(0);
}

umask 0002;

my $la		= QCMG::Automation::LiveArc->new(VERBOSE => 1);
# write EXECLOG parameters
# pass command line args for logging
$la->cmdline(LINE => $cline);
# write start of log file
$LOG_FILE =~ s/ //g;
$la->init_log_file(LOG_FILE => $LOG_FILE);
$la->execlog();

# write TOOLLOG parameters, if any
my $toollog	 = qq{TOOLLOG: LOG_FILE $LOG_FILE\n};	# /panfs/automation_log/S17009_20120113_2_LMP.lane_3.nobc_extractmapped.log
$toollog	.= qq{TOOLLOG: ASSETNAME $ASSETNAME\n};	# S17009_20120113_2_LMP.lane_3.nobc
$toollog	.= qq{TOOLLOG: EXTRACTTO $EXTRACTTO\n}; # /mnt/seq_results/
$la->writelog(BODY => $toollog);



### get slide name, lane, and barcode(s)
# S0417_20120103_1_LMP.nopd.nobc
$ASSETNAME	=~ /(.+?)\.(.+?)\.(.+)/;
my $slide	= $1;
my $pd		= $2;
my $barcode	= $3;
my $mapset	= join ".", $slide, $pd, $barcode;
$la->writelog(BODY => "ASSET NAME: $ASSETNAME ($slide, $pd, $barcode)");

my $NAMESPACE	= "/".$la->LA_NAMESPACE()."/".$slide."/".$slide."_mapped";
$la->writelog(BODY => "NAMESPACE: $NAMESPACE");

my $file	= &create_filename($slide, $pd, $barcode, 'bam');
my $bam		= $file;

my $status	= 0;	# initialize exit status

# query Geneus for metadata
my $md		= QCMG::DB::Metadata->new();
my $slide_mapsets	=  $md->slide_mapsets($slide);
$md->find_metadata("mapset", $mapset);
my $d		= $md->mapset_metadata($mapset);
my $project_id	= $d->{'project'};
my $sample_set	= $d->{'donor'};
=cut
alignment_required - 1
genome_file        - /panfs/share/genomes/lifescope/referenceData/internal/qcmg_mm64/reference/Mus_musculus.NCBIM37.64.ALL_validated.E.fa
project - smgres_gemm
failed_qc - 0
primary_library - Library_20120126_G
capture_kit - Mouse Exome (Nimblegen)
mapset - S0428_20120712_1_FragPEBC.nopd.bcB16_12
sample - ICGC-ABMJ-20110714-06-ND
alignment_required - 1
sequencing_type - Multiplex Paired End Sequencing
source_code - 4:Normal control (other site)
library_type - Fragment
aligner - 
donor - GEMM_0203
species_reference_genome - Mus musculus (QCMG_MGSCv37.64)
final - GEMM_0203.exome.ND
genome_file - /panfs/share/genomes/lifescope/referenceData/internal/qcmg_mm64/reference/Mus_musculus.NCBIM37.64.ALL_validated.E.fa
material - 1:DNA
=cut

$la->writelog(BODY => "BAM:        $file");
$la->writelog(BODY => "Project ID: $project_id");
$la->writelog(BODY => "Sample Set: $sample_set");

# create extraction folder name; can expect /mnt/seq_raw/ to exist
# namespace = /QCMG/runname/
# assetname = runname_seq_mapped
$status 	= 2;
my $extractto	= $EXTRACTTO;
$extractto	.= $project_id."/";
$la->writelog(BODY => "Checking $extractto");
if(! -e $extractto) {
	# create /panfs/seq_results/icgc_pancreatic/ if it doesn't exist
	my $rv = mkdir($extractto); # rv = 1 for success, 0 for failure
	$status = 1 if($rv != 1);
}
$extractto	.= $sample_set."/";
$la->writelog(BODY => "Checking $extractto");
if(! -e $extractto) {
	# create /panfs/seq_results/icgc_pancreatic/APGI_2214 if it doesn't exist
	my $rv = mkdir($extractto); # rv = 1 for success, 0 for failure
	$status = 1 if($rv != 1);
}
$extractto	.= "seq_mapped/";
$la->writelog(BODY => "Checking $extractto");
if(! -e $extractto) {
	# create /panfs/seq_results/icgc_pancreatic/APGI_2214/seq_mapped if it doesn't exist
	my $rv = mkdir($extractto); # rv = 1 for success, 0 for failure
	$status = 1 if($rv != 1);
}

if($status == 1) {
	$la->writelog(BODY => "$extractto does not exist, failing");

	my $date	= $la->timestamp();

	$la->writelog(BODY => "EXECLOG: ERRORMESSAGE Directory does not exist: $extractto");
	$la->writelog(BODY => "EXECLOG: FINISHTIME $date");
	$la->writelog(BODY => "EXECLOG: EXITSTATUS $status");

	my $subj	= $ASSETNAME." MAPPED EXTRACTION -- FAILED";
	my $body	= "MAPPED EXTRACTION of $ASSETNAME failed";
	$body		.= "\nSee log file: ".$la->HOSTKEY.":".$LOG_FILE;

	$la->send_email(EMAIL => 'QCMG-InfoTeam@imb.uq.edu.au', SUBJECT => $subj, BODY => $body);
	exit($status);
}

unless(-w $extractto) {
	$la->writelog(BODY => "$extractto has bad permissions, failing");

	my $date	= $la->timestamp();

	$la->writelog(BODY => "EXECLOG: ERRORMESSAGE Bad permissions");
	$la->writelog(BODY => "EXECLOG: FINISHTIME $date");
	$la->writelog(BODY => "EXECLOG: EXITSTATUS $status");

	my $subj	= $ASSETNAME." MAPPED EXTRACTION -- FAILED";
	my $body	= "MAPPED EXTRACTION of $ASSETNAME failed";
	$body		.= "\nSee log file: ".$la->HOSTKEY.":".$LOG_FILE;

	$la->send_email(EMAIL => 'QCMG-InfoTeam@imb.uq.edu.au', SUBJECT => $subj, BODY => $body);
	exit(2);
}

# get extract asset command for BAM
# build asset path:
#:path "/QCMG/S0416_20110525_2_FragPEBC/S0416_20110525_2_FragPEBC_mapped/:S0416_20110525_2_FragPEBC.nopd.bcB16_08.bam"
my $bamid	= $la->asset_exists(NAMESPACE => $NAMESPACE, ASSET => $bam);
if(! $bamid) {
	$la->writelog(BODY => "Asset $NAMESPACE / $bam does not exist, failing");

	my $date	= $la->timestamp();

	$la->writelog(BODY => "EXECLOG: ERRORMESSAGE BAM asset does not exist");
	$la->writelog(BODY => "EXECLOG: FINISHTIME $date");
	$la->writelog(BODY => "EXECLOG: EXITSTATUS $status");

	my $subj	= $ASSETNAME." MAPPED EXTRACTION -- FAILED";
	my $body	= "MAPPED EXTRACTION of $ASSETNAME failed";
	$body		.= "\nSee log file: ".$la->HOSTKEY.":".$LOG_FILE;

	$la->send_email(EMAIL => 'QCMG-InfoTeam@imb.uq.edu.au', SUBJECT => $subj, BODY => $body);
	exit(24);
}

my $rv = 1;

# add BAM file name to path
my $bamfilepath	= $extractto.$bam;
$la->writelog(BODY => "BAM: $bamfilepath");
#print STDERR "BAM: $bamfilepath\n";
my $bamcmd;
if(! -e $bamfilepath) {
	# extract BAM
	$la->writelog(BODY => "$bamfilepath does not exist, getting extract command");

	$bamcmd = $la->extract_asset(ID => $bamid, PATH => $bamfilepath, RAW => 1, CMD => 1);
	$la->writelog(BODY => "$bamcmd");
	$la->writelog(BODY => "LiveArc command: $bamcmd");

	$la->writelog(BODY => "Executing $bamcmd");	# 0 = success
	$rv	= system($bamcmd);
	$la->writelog(BODY => "RV: $rv");	# 0 = success
}
else {
	$rv = 0;
	$la->writelog(BODY => "$bamfilepath exists, skipping extraction");
}

# get extract asset command for solid stats report
# build asset path:
#:path "/QCMG/S0416_20110525_2_FragPEBC/S0416_20110525_2_FragPEBC_mapped/:S0416_20110525_2_FragPEBC.nopd.bcB16_08_solidstats"
=cut
my $ssr	= $bam;
$ssr		=~ s/\.bam$/\.solidstats\.xml/;
my $ssrid	= $la->asset_exists(NAMESPACE => $NAMESPACE, ASSET => $ssr);
if(! $ssrid) {
	$la->writelog(BODY => "Asset $NAMESPACE / $ssr does not exist, failing");

	my $date	= $la->timestamp();

	$la->writelog(BODY => "EXECLOG: ERRORMESSAGE solid stats report asset does not exist");
	$la->writelog(BODY => "EXECLOG: FINISHTIME $date");
	$la->writelog(BODY => "EXECLOG: EXITSTATUS $status");

	my $subj	= $ASSETNAME." MAPPED EXTRACTION -- FAILED";
	my $body	= "MAPPED EXTRACTION of $ASSETNAME failed";
	$body		.= "\nSee log file: ".$la->HOSTKEY.":".$LOG_FILE;

	$la->send_email(EMAIL => 'QCMG-InfoTeam@imb.uq.edu.au', SUBJECT => $subj, BODY => $body);
	exit(24);
}

# add SSR file name to path
my $ssrfilepath	= $extractto.$ssr;
$la->writelog(BODY => "SOLiD stats report: $ssrfilepath");
my ($ssrcmd, $rv);
if(! -e $ssrfilepath) {
	$la->writelog(BODY => "$ssrfilepath does not exist, getting extract command");

	$ssrcmd = $la->extract_asset(ID => $ssrid, PATH => $ssrfilepath, RAW => 1, CMD => 1);
	$la->writelog(BODY => "$ssrcmd");
	$la->writelog(BODY => "LiveArc command: $ssrcmd");

	$la->writelog(BODY => "Executing $ssrcmd");	# 0 = success
	$rv	= system($ssrcmd);
	$la->writelog(BODY => "RV: $rv");	# 0 = success
}
#else {
#	$rv = 0;
#	$la->writelog(BODY => "$ssrfilepath exists, skipping extraction");
#}
=cut

## create BAM index
# -- check file name...
my $bin_samtools	= $la->BIN_SAMTOOLS;
my $idxcmd		= qq{$bin_samtools/samtools index $bamfilepath};
if($rv == 0) {
	my $rv	= system($idxcmd);
	$la->writelog(BODY => "RV for $idxcmd : $rv");	# 0 = success
}
if($rv == 0) {
	$status = 0;
}

# check that files exist
unless( -e $bamfilepath) {
	$la->writelog(BODY => "BAM not extracted");
	$status = 1;
}
#unless(-e $ssrfilepath) {
#	$la->writelog(BODY => "SOLiD stats report not extracted");
#	$status = 1;
#}
unless(-e $bamfilepath.".bai") {
	$la->writelog(BODY => "BAM index not created");
	$status = 1;
}

## CHECK CHECKSUMS
$la->writelog(BODY => "---Performing checksums on raw files---");
$la->writelog(BODY => "START: ".$la->timestamp());
my $assetdata		= $la->get_asset(ID => $bamid);
if($assetdata->{'META'} !~ /:checksums/) {
	# if no checksums available, skip this step
	$la->writelog(BODY => "Checksums not available for this asset, skipping check");
}
else {
	# if checksums available, check them
	my @checksums = ($assetdata->{'META'} =~ /:checksum\n\s+:chksum\s+\"(\d+)\"\n\s+:filesize\s+\"(\d+)\"\n\s+:filename\s+\"(.+?)\"/sg);
	my $i = 0;
	my $cksum = $checksums[$i];
	my $fsize = $checksums[$i+1];
	my $fname = $checksums[$i+2];

	#print STDERR "$fname: checksum $cksum, size $fsize\n";

	my ($crc, $blocks, $fname) = $la->checksum(FILE => $bamfilepath);

	$la->writelog(BODY => "LiveArc $fname: checksum $cksum, size $fsize");
	$la->writelog(BODY => "Current $fname: checksum $crc, size $blocks");

	#print STDERR "$fname: checksum $crc, size $blocks\n";

	unless($cksum == $crc && $fsize == $blocks) {
		$la->writelog(BODY => "Checksum invalid");
		exit(13);
	}
	$la->writelog(BODY => "Checksums complete");
	$la->writelog(BODY => "END: ".$la->timestamp());
}

if($status != 0) {
	$la->writelog(BODY => "EXECLOG: ERRORMESSAGE Extraction failed");
	my $date = $la->timestamp();
	$la->writelog(BODY => "EXECLOG: FINISHTIME $date");
	$la->writelog(BODY => "EXECLOG: EXITSTATUS $status");

	my $subj	= $ASSETNAME." MAPPED EXTRACTION -- FAILED";
	my $body	= "See log file: ".$la->HOSTKEY.":".$LOG_FILE;

	$la->send_email(EMAIL => 'QCMG-InfoTeam@imb.uq.edu.au', SUBJECT => $subj, BODY => $body);
	exit(16);
}

# finish EXECLOG
my $date = $la->timestamp();
$la->writelog(BODY => "EXECLOG: ERRORMESSAGE none");
$la->writelog(BODY => "EXECLOG: FINISHTIME $date");
$la->writelog(BODY => "EXECLOG: EXITSTATUS 0");

# send success email if no errors
my $subj	= $ASSETNAME." MAPPED EXTRACTION -- SUCCEEDED";
my $body	= "MAPPED EXTRACTION of $ASSETNAME successful. See log file: ".$la->HOSTKEY.":".$LOG_FILE;
$la->send_email(EMAIL => 'QCMG-InfoTeam@imb.uq.edu.au', SUBJECT => $subj, BODY => $body);


if($status== 0) {
	my $asset		= $ASSETNAME."_extractmapped_log";

	my $la	= QCMG::Automation::LiveArc->new(VERBOSE => 0);
	my $assetid = $la->asset_exists(NAMESPACE => $NAMESPACE, ASSET => $asset);
	my $status;

	if(! $assetid) {
		#print STDERR "Creating log file asset\n";
		$assetid = $la->create_asset(NAMESPACE => $NAMESPACE, ASSET => $asset);
	}
	#print STDERR "Updating log file\n";
	$status = $la->update_asset_file(ID => $assetid, FILE => $LOG_FILE);

	#print STDERR "$status\n";
}

exit(0);

#####################################################################
sub create_filename {
	my $slide	= shift @_;	# S8006_20111219_2_FragPEBC
	my $pd		= shift @_;	# lane_3 | nopd
	my $bc		= shift @_;	# bcB16_01 | nobc
	my $type	= shift @_;	# bam | ssr

	#print STDERR "$slide, $pd, $bc, $type\n";

	my $name	= join ".", $slide, $pd, $bc;
	if($type =~ /bam/i) {
		$name	.= ".bam";
	}
	elsif($type =~ /ssr/i) {
		$name	.= ".solidstats.xml";
	}

	#print STDERR "$name\n";

	return($name);
}


