#!/usr/bin/env perl

##############################################################################
#
#  Program:  ingest_mapped.pl
#  Author:   Lynn Fink
#  Created:  2011-05-19
#
# Mapped sequencing file ingestion script; copies files from barrine and
# archives them in LiveArc
#
# $Id: ingest_mapped.pl 2667 2012-08-07 00:26:35Z l.fink $
#
##############################################################################

use strict;
use Getopt::Long;
use lib qw(/panfs/imb/qcmg/software/QCMGPerl/lib/);
#use lib qw(/panfs/imb/qcmg/software/QCMGPerl/lib/thirdparty/);
use QCMG::Ingest::Mapped;
use QCMG::FileDir::Finder;
use File::Spec;

use vars qw($SVNID $REVISION $VERSION);
use vars qw($MAPSET_FOLDER $LOG_FILE $LOG_LEVEL $ADD_EMAIL $UPDATE);
use vars qw($USAGE);

# set command line, for logging
my $cline	= join " ", @ARGV;

&GetOptions(
		"i=s"		=> \$MAPSET_FOLDER,
		"log=s"		=> \$LOG_FILE,
		"loglevel=s"	=> \$LOG_LEVEL,
		"e=s"		=> \$ADD_EMAIL, 
		"V!"		=> \$VERSION,
		"u!"		=> \$UPDATE,
		"h!"		=> \$USAGE
	);

# help message
if($USAGE || ! $MAPSET_FOLDER ) {
        my $message = <<USAGE;

        USAGE: $0

	Ingest mapped sequencing run files into LiveArc

        $0 -i /path/to/mapset/folder/ -log logfile.log

	Required:
	-i        <dir>     path to mapset folder to ingest

	Optional:
	-u		    update mapped assets after a partial ingest
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

my $qi		= QCMG::Ingest::Mapped->new(MAPSET_FOLDER => $MAPSET_FOLDER, LOG_FILE => $LOG_FILE, UPDATE => $UPDATE);

if(! -e $MAPSET_FOLDER) {
	print STDERR "$MAPSET_FOLDER does not exist\n";
	exit(1);
}

if($LOG_LEVEL eq 'DEBUG') {
	$qi->LA_NAMESPACE("/test");
	print STDERR "Using LiveArc namespace: ", $qi->LA_NAMESPACE, "\n";
}
# set livearc namespace for 5500 slides
if($MAPSET_FOLDER =~ /\/S[18]\d{4}\_/) {
	$qi->LA_NAMESPACE("/QCMG_5500");
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

# get BAM
$qi->find_bam();
$qi->find_umbam();
#$qi->find_ma();
$qi->find_bioscope_logs();

# generate checksum on BAM
$qi->checksum_bam();
$qi->checksum_umbam();
#$qi->checksum_ma();

# generate solid stats report
#$qi->solid_stats_report();

# initiate ingest
$qi->ingest_bam();
$qi->ingest_umbam();
#$qi->ingest_ma();
#$qi->ingest_ssr();	# solid stats report
$qi->ingest_mapped();
$qi->ingest_bioscope_logs();

# delete mapped and raw files if everything completed successfully
$qi->clean();

exit(0);

