#!/usr/bin/perl

##############################################################################
#
#  Program:  run_tmap.pl
#  Author:   Lynn Fink
#  Created:  2011-01-04
#
# Check extracted raw sequencing files and run tmap to map them
#
# $Id: run_tmap.pl 1283 2011-10-19 03:36:39Z l.fink $
#
##############################################################################

use strict;
use Getopt::Long;
use QCMG::Automation::LiveArc;
use File::Spec;
use Data::Dumper;
use QCMG::DB::Geneus;
use lib qw(/panfs/home/lfink/devel/QCMGPerl/lib/);
use QCMG::Tmap::Run;

use vars qw($SVNID $REVISION $VERSION);
use vars qw($SLIDE_FOLDER $LOG_FILE $LOG_LEVEL $ADD_EMAIL);
use vars qw($USAGE);

#$JOBNAME	= "map_tmap";

# set command line, for logging
my $cline	= join " ", @ARGV;

&GetOptions(
		"i=s"		=> \$SLIDE_FOLDER,
		"log=s"		=> \$LOG_FILE,
		"loglevel=s"	=> \$LOG_LEVEL,
		"e=s"		=> \$ADD_EMAIL, 
		"V!"		=> \$VERSION,
		"h!"		=> \$USAGE
	);

# help message
if($USAGE || (! $SLIDE_FOLDER || ! $LOG_FILE) ) {
        my $message = <<USAGE;

        USAGE: $0

	Check raw sequencing files and run tmap if they are ok

        $0 -i /panfs/seq_raw/slide_folder -log logfile.log

	Required:
	-i        <name> directory name where raw sequencing data is
	-log      <file> log file to write execution params and info to

	Optional:
	-V        (version information)
        -h        (print this message)

USAGE

        print $message;
        exit(0);
}

umask 0002;

my $rt          = QCMG::Tmap::Run->new(SLIDE_FOLDER => $SLIDE_FOLDER, LOG_FILE => $LOG_FILE);

if($LOG_LEVEL eq 'DEBUG') {
        $rt->LA_NAMESPACE("test");
        print STDERR "Using LiveArc namespace: ", $rt->LA_NAMESPACE, "\n";
}
else {
        $rt->LA_NAMESPACE("/QCMG_torrent/");
}

if($ADD_EMAIL) {
        $rt->add_email(EMAIL => $ADD_EMAIL);
}

# pass command line args for logging
$rt->cmdline(LINE => $cline);
# write start of log file
$rt->execlog();
$rt->toollog();

# check that folder exists and is read/writable
$rt->check_folder();

# get patient, project, etc info from LIMS for each barcode
$rt->query_lims();

# create PBS script files and ingest them
$rt->create_pbs_scripts();

# submit jobs
$rt->run_tmap();

exit(0);

