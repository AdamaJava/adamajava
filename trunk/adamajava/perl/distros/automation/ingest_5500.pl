#!/usr/bin/perl

##############################################################################
#
#  Program:  ingest_5500.pl
#  Author:   Lynn Fink
#  Created:  2011-02-03
#
# Raw sequencing file ingestion script; copies files from sequencing machines to
# archives and cluster
#
# $Id: ingest.pl 1283 2011-10-19 03:36:39Z l.fink $
#
##############################################################################

### TEST ON 10.160.72.29 (Fakey)
# ./ingest.pl -dir /data/results/solid0777/S0449_20100603_1_Frag/

# set local Perl environment - this will include the necessary version of XML::LibXML (to avoid using the system version which is too old)
my $source      = '. /usr/local/mediaflux/perlenv';
my $rv          = `$source`;

use strict;
use Getopt::Long;
use lib qw(/usr/local/mediaflux/QCMGPerl/lib/);
use QCMG::Ingest::5500;

use vars qw($SVNID $REVISION $VERSION);
use vars qw($SLIDE_FOLDER $LOG_FILE $LOG_LEVEL $UPDATE $ADD_EMAIL);
use vars qw($USAGE);

# set command line, for logging
my $cline	= join " ", @ARGV;

&GetOptions(
		"i=s"		=> \$SLIDE_FOLDER,
		"log=s"		=> \$LOG_FILE,
		"loglevel=s"	=> \$LOG_LEVEL,
		"e=s"		=> \$ADD_EMAIL, 
		"u!"		=> \$UPDATE,
		"V!"		=> \$VERSION,
		"h!"		=> \$USAGE
	);

# help message
if($USAGE || ! $SLIDE_FOLDER ) {
        my $message = <<USAGE;

        USAGE: $0

	Ingest raw sequencing run files into Mediaflux

        $0 -i /path/to/run/folder/ -log logfile.log

	Required:
	-i        <dir>  path to run folder to ingest

	Optional:
	-u	  (run in update mode; do not ingest assets that already exist)
	-log      <file> log file to write execution params and info to
			 [defaults to "ingest.log" under run folder]
	-loglevel <string> DEBUG: reverts to "/test/" LiveArc namespace
	-e        <email address> additional email addresses to notify of status
	-V        (version information)
        -h        (print this message)

USAGE

        print $message;
        exit(0);
}

# if user wants to update a failed ingestion this will perform as usual except
# when it is time to ingest as XSQ file, it will skip any existing XSQ assets
if($UPDATE) {
	$UPDATE = 1;
}
else {
	$UPDATE = 0;
}

my $qi		= QCMG::Ingest::5500->new(SLIDE_FOLDER => $SLIDE_FOLDER, LOG_FILE => $LOG_FILE, UPDATE => $UPDATE);

# set namespace to 5500 data
$qi->LA_NAMESPACE("QCMG_5500");

if($LOG_LEVEL eq 'DEBUG') {
	$qi->LA_NAMESPACE("test");
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

# check that run folder exists and can be accessed
$qi->check_folder();
# get slide name from run folder name
#$qi->slide_name();
# generate LiveArc asset name for raw data: SLIDE_seq_raw
$qi->asset_name();
# find all XSQ files (and also all lanes)
$qi->find_xsqs();

# make sure file is ok - do h5check
#$qi->check_rawfiles();
# perform checksums on XSQs
$qi->checksum_rawfiles();

# get run type for each lane
$qi->get_run_type();

# rename XSQ files(s) so it conforms to our naming scheme
$qi->rename_xsq();

# edit XSQ file(s) to change slide name from default to our naming scheme
# - calls python script that changes only that attribute
$qi->edit_xsq_runname();

# generate INI files
$qi->generate_ini_files();

# initiate ingest
#print STDERR "INGESTING\n";
$qi->ingest();

exit(0);

