#!/usr/bin/perl

##############################################################################
#
#  Program:  ingest.pl
#  Author:   Lynn Fink
#  Created:  2011-02-03
#
# Raw sequencing file ingestion script; copies files from sequencing machines to
# archives and cluster
#
# $Id: ingest.pl 2633 2012-08-01 06:21:19Z l.fink $
#
##############################################################################

### TEST ON 10.160.72.29 (Fakey)
# ./ingest.pl -dir /data/results/solid0777/S0449_20100603_1_Frag/

$ENV{'PERL5LIB'} = "/usr/local/mediaflux/QCMGPerl/lib/";

# set local Perl environment - this will include the necessary version of 
#  XML::LibXML (to avoid using the system version which is too old)
my $source      = 'source /usr/local/mediaflux/perlenv';
#my $rv          = `$source`;
my $rv		= system($source);

use lib "/usr/local/mediaflux/perl5";
use strict;
use Getopt::Long;
use QCMG::Ingest::Solid;

use vars qw($SVNID $REVISION $VERSION);
use vars qw($RUN_FOLDER $LOG_FILE $LOG_LEVEL $ADD_EMAIL);
use vars qw($USAGE);

# set command line, for logging
my $cline	= join " ", @ARGV;

&GetOptions(
		"i=s"		=> \$RUN_FOLDER,
		"log=s"		=> \$LOG_FILE,
		"loglevel=s"	=> \$LOG_LEVEL,
		"e=s"		=> \$ADD_EMAIL, 
		"V!"		=> \$VERSION,
		"h!"		=> \$USAGE
	);

# help message
if($USAGE || ! $RUN_FOLDER ) {
        my $message = <<USAGE;

        USAGE: $0

	Ingest raw sequencing run files into Mediaflux

        $0 -i /path/to/run/folder/ -log logfile.log

	Required:
	-i        <dir>  path to run folder to ingest

	Optional:
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

my $qi		= QCMG::Ingest::Solid->new(RUN_FOLDER => $RUN_FOLDER, LOG_FILE => $LOG_FILE);

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

# check that run folder can be accessed
$qi->check_folder();
# get run name
$qi->slide_name();

$qi->asset_name();
# get run information
$qi->get_metadata();
# find read directories
$qi->run_primaries();
# find csfasta and qual files
$qi->run_reads();
# check that all expected files exist
$qi->check_all_files();
# check that csfasta and qual files are not corrupt
$qi->check_rawfiles();
$qi->checksum_rawfiles();

# this will create the INI files and their dir and then import them into LA/MF
$qi->write_bioscope_ini_files();

# generate solid_stats_report for raw sequencing data
$qi->solid_stats_report();

unless($LOG_LEVEL eq 'DEBUG') {
	#$qi->update_run_info();

	# clean up: remove symlinks and colorcall dirs
	$qi->delete_symlinks();
	$qi->delete_colorcalls_dirs();
	$qi->delete_jobs_dirs();
	# or do it all in one step
	#$qi->clean_folder();
}

# initiate ingest
#print STDERR "INGESTING\n";
$qi->ingest();

exit(0);

