#!/usr/bin/perl

##############################################################################
#
#  Program:  run_bioscope.pl
#  Author:   Lynn Fink
#  Created:  2011-01-04
#
# Check extracted raw sequencing files and run Bioscope to map them
#
# $Id: run_bioscope.pl 2639 2012-08-01 11:30:30Z l.fink $
#
##############################################################################

use strict;
use Getopt::Long;
#use lib qw(/panfs/imb/qcmg/software/QCMGPerl/lib/);
use lib qw(/share/software/QCMGPerl/lib/);
use QCMG::Bioscope::Run;
use QCMG::Automation::LiveArc;
use File::Spec;
#use Data::Dumper;
#use Devel::DProf;
#use B::Lint;

use vars qw($SVNID $REVISION $VERSION);
use vars qw($RUN_FOLDER $LOG_FILE $LOG_LEVEL $ADD_EMAIL);
#use vars qw($JOBNAME);
use vars qw($USAGE);

#$JOBNAME	= "map_bioscope";

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
if($USAGE || (! $RUN_FOLDER || ! $LOG_FILE) ) {
        my $message = <<USAGE;

        USAGE: $0

	Check raw sequencing files and run Bioscope if they are ok

        $0 -i /panfs/imb/seq_raw/run_folder -log logfile.log

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

my $br          = QCMG::Bioscope::Run->new(RUN_FOLDER => $RUN_FOLDER, LOG_FILE => $LOG_FILE);

if($LOG_LEVEL eq 'DEBUG') {
        $br->LA_NAMESPACE("test");
        print STDERR "Using LiveArc namespace: ", $br->LA_NAMESPACE, "\n";
}

if($ADD_EMAIL) {
        $br->add_email(EMAIL => $ADD_EMAIL);
}

# pass command line args for logging
$br->cmdline(LINE => $cline);
# write start of log file
$br->execlog();
$br->toollog();

# check that folder exists and is read/writable
$br->check_folder();

# check that Bioscope INI files exist; if not, create them/extract them
$br->check_bioscope_ini_files();

# submit job
$br->run_bioscope();

exit(0);

