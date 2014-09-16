#!/usr/bin/perl

##############################################################################
#
#  Program:  extract_raw.pl
#  Author:   Lynn Fink
#  Created:  2011-12-07
#
# Raw 5500 sequencing file extraction script; copies files from LiveArc and prepares
# them for Lifescope mapping on barrine
#
# $Id: extract_raw.pl 1283 2011-10-19 03:36:39Z l.fink $
#
##############################################################################

use strict;
use Getopt::Long;
use lib qw(/share/software/QCMGPerl/lib/);
use QCMG::Extract::5500;
use QCMG::FileDir::Finder;
use File::Spec;
use Data::Dumper;

use vars qw($SVNID $REVISION $VERSION);
use vars qw($ASSETID $EXTRACTTO $LOG_FILE $LOG_LEVEL $ADD_EMAIL);
use vars qw($USAGE);

# set command line, for logging
my $cline	= join " ", @ARGV;

&GetOptions(
		"i=s"		=> \$ASSETID,
		"o=s"		=> \$EXTRACTTO,
		"log=s"		=> \$LOG_FILE,
		"loglevel=s"	=> \$LOG_LEVEL,
		"e=s"		=> \$ADD_EMAIL, 
		"V!"		=> \$VERSION,
		"h!"		=> \$USAGE
	);

# help message
if($USAGE || (! $ASSETID || ! $EXTRACTTO || ! $LOG_FILE) ) {
        my $message = <<USAGE;

        USAGE: $0

	./extract_raw.pl -i 40453 -o /panfs/imb/test/ -log extract.log

	Extract raw 5500 sequencing run files from LiveArc and prepare them for
	Lifescope mapping

        $0 -i asset_id -o /path/to/extract/to -log logfile.log

	Required:
	-i        <id>   LiveArc asset id
	-o	  <dir>  path to extract asset to
	-log      <file> log file to write execution params and info to

	Optional:
	-V        (version information)
        -h        (print this message)

USAGE

        print $message;
        exit(0);
}

umask 0002;

my $qe	= QCMG::Extract::5500->new(	ASSETID		=> $ASSETID,
					EXTRACTTO	=> $EXTRACTTO,
					LOG_FILE	=> $LOG_FILE
				);

if($LOG_LEVEL eq 'DEBUG') {
        $qe->LA_NAMESPACE("test");
        print STDERR "Using LiveArc namespace: ", $qe->LA_NAMESPACE, "\n";
}

# write EXECLOG parameters
# pass command line args for logging
$qe->cmdline(LINE => $cline);
# write start of log file
$qe->init_log_file(LOG_FILE => $LOG_FILE);
$qe->execlog();

# write TOOLLOG parameters, if any
my $toollog	 	 = qq{TOOLLOG: LOG_FILE $LOG_FILE\n};
$toollog		.= qq{TOOLLOG: ASSETID $ASSETID\n};
$toollog		.= qq{TOOLLOG: EXTRACTTO $EXTRACTTO\n};
$qe->writelog(BODY => $toollog);



my $slide_name		= $qe->slide_name();

my $run_folder 		= $qe->run_folder();

$qe->extract();

$qe->check_extract_checksums();

exit(0);
