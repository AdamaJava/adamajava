#!/usr/bin/perl

##############################################################################
#
#  Program:  ingest_mapped_5500.pl
#  Author:   Lynn Fink
#  Created:  2011-12-14
#
# Mapped 5500 sequencing file ingestion script; copies files from barrine and
# archives them in LiveArc
#
# $Id: ingest_mapped_5500.pl 1404 2011-12-05 04:01:16Z l.fink $
#
##############################################################################

use strict;
use Getopt::Long;
use lib qw(/panfs/imb/qcmg/software/QCMGPerl/lib/);
#use lib qw(/panfs/imb/test/);
use QCMG::Ingest::Mapped5500;
use File::Spec;

use vars qw($SVNID $REVISION $VERSION);
use vars qw($USER $PROJECT $ANALYSIS $LOG_FILE $LOG_LEVEL $ADD_EMAIL $UPDATE);
use vars qw($USAGE);

# set command line, for logging
my $cline	= join " ", @ARGV;

&GetOptions(
		"u=s"		=> \$USER,
		"p=s"		=> \$PROJECT,
		"a=s"		=> \$ANALYSIS,
		"log=s"		=> \$LOG_FILE,
		"loglevel=s"	=> \$LOG_LEVEL,
		"e=s"		=> \$ADD_EMAIL, 
		"V!"		=> \$VERSION,
		"update!"	=> \$UPDATE,
		"h!"		=> \$USAGE
	);

# help message
if($USAGE || (! $PROJECT && ! $USER && ! $ANALYSIS)) {
        my $message = <<USAGE;

        USAGE: $0

	Ingest mapped 5500 sequencing run files into LiveArc

        $0 -p mapset -u user -a rundate -log logfile.log

	Required:
	-u	<username>	user who ran Lifescope for this project
	-p	<project>	mapset/project name
	-a	<analysis>	analysis name (run date)

	Optional:
	-update		    update mapped assets after a partial ingest
	-p        <dir>     path to mapset folder to ingest
	-log      <file>    log file to write execution params and info to
			    [defaults to "ingest_mapped.log" under mapset folder]
	-loglevel <string>  DEBUG: reverts to "/test/" LiveArc namespace
	-e        <email>   additional email addresses to notify of status

	-V        (version information)
        -h        (print this message)

USAGE

        print $message;
        exit(0);
}

# only ingest assets that haven't been done yet if a previous attempt has been
# made
if($UPDATE) {
	$UPDATE = 'Y';
}
else {
	$UPDATE = 'N';
}

my $qi		= QCMG::Ingest::Mapped5500->new(
						PROJECT		=> $PROJECT,
						USER		=> $USER,
						ANALYSIS	=> $ANALYSIS,
						LOG_FILE	=> $LOG_FILE,
						UPDATE		=> $UPDATE
					);

# set namespace to 5500 data
$qi->LA_NAMESPACE_5500;

if($LOG_LEVEL eq 'DEBUG') {
	$qi->LA_NAMESPACE("/test");
	print STDERR "Using LiveArc namespace: ", $qi->LA_NAMESPACE, "\n";
}
$qi->add_email(EMAIL => 'QCMG-InfoTeam@imb.uq.edu.au');

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
# get BAM
# - rename BAMs with barcodes (if they have them) by getting the barcode out of
# the BAM header - replace the Idx number with this
$qi->find_bam();
$qi->find_umbam();
$qi->find_lifescope_logs();
$qi->rename_bam();
$qi->rename_umbam();

# generate checksum on BAM
$qi->checksum_bam();
$qi->checksum_umbam();

# generate solid stats report
$qi->generate_stats_report();

# initiate ingest
$qi->ingest_bam();
$qi->ingest_umbam();
$qi->ingest_ssr();	# solid stats report
$qi->ingest_lifescope_logs();

exit(0);

