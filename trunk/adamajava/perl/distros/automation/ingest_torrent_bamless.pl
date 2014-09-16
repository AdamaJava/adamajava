#!/usr/bin/perl

##############################################################################
#
#  Program:  ingest_torrent.pl
#  Author:   Lynn Fink
#  Created:  2011-03-17
#
# Raw sequencing file ingestion script; copies files from sequencing machines to
# archives and cluster ION TORRENT!
#
#
##############################################################################

### TEST ON 10.160.72.29 (Fakey)
# ./ingest_torrent.pl -dir /data/results/solid0777/S0449_20100603_1_Frag/

use strict;
use Getopt::Long;
use QCMG::Ingest::Torrent;

use vars qw($SVNID $REVISION $VERSION);
use vars qw($RUN_FOLDER $ANALYSIS_FOLDER $LOG_FILE $LOG_LEVEL $ADD_EMAIL);
use vars qw($USAGE);

# set command line, for logging
my $cline	= join " ", @ARGV;

&GetOptions(
		"i=s"		=> \$RUN_FOLDER,
		"a=s"		=> \$ANALYSIS_FOLDER,
		"log=s"		=> \$LOG_FILE,
		"loglevel=s"	=> \$LOG_LEVEL,
		"e=s"		=> \$ADD_EMAIL, 
		"V!"		=> \$VERSION,
		"h!"		=> \$USAGE
	);

# help message
if($USAGE || (! $RUN_FOLDER || ! $LOG_FILE) ) {
        my $message = <<USAGE;

        USAGE: $0

	Ingest raw sequencing Ion Torrent files into LiveArc for runs with no BAM

        $0 -i /path/to/run/folder/ -log logfile.log
        $0 -i /path/to/run/folder/ -a /path/to/analysis/folder/ -log logfile.log

	Required:
	-i        <dir>  path to run folder to ingest
	-log      <file> log file to write execution params and info to

	Optional:
	-a	  <dir>  path to analysis folder
	-e        <email address> additional email addresses to notify of status
	-V        (version information)
        -h        (print this message)

USAGE

        print $message;
        exit(0);
}

my $qi;
if(! $ANALYSIS_FOLDER) {
	$qi		= QCMG::Ingest::Torrent->new(RUN_FOLDER => $RUN_FOLDER, LOG_FILE => $LOG_FILE);
}
else {
	$qi		= QCMG::Ingest::Torrent->new(RUN_FOLDER => $RUN_FOLDER, LOG_FILE => $LOG_FILE, ANALYSIS_FOLDER => $ANALYSIS_FOLDER);
}

if($ADD_EMAIL) {
	$qi->add_email(EMAIL => $ADD_EMAIL);
}

# pass command line args for logging
$qi->cmdline(LINE => $cline);
# write start of log file
$qi->execlog();
$qi->toollog();

#$qi->LA_NAMESPACE("test/ingestion");
print STDERR "Using LiveArc namespace: ", $qi->LA_NAMESPACE, "\n";

my $suffix = $qi->LA_ASSET_SUFFIX_MAP;
#print STDERR "Using asset suffix: ", $suffix, "\n";

# check that run folder can be accessed
$qi->check_folder();
# get run name
$qi->slide_name();
$qi->asset_name_dat();
$qi->asset_name_map();
$qi->check_dat_files();
$qi->check_run_files();
#$qi->check_mapped_files();
$qi->pdf_report();

# write Bioscope ini info to database
#$qi->update_run_info();

# clean up: remove symlinks and colorcall dirs
#$qi->delete_symlinks();

# initiate ingest
$qi->ingest();


exit(0);

