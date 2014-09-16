#!/usr/bin/perl

##############################################################################
#
#  Program:  extract_raw_torrent.pl
#  Author:   Lynn Fink
#  Created:  2012-04-23
#
# Raw sequencing file extraction script; copies files from LiveArc and prepares
# them for tmap mapping on babyjesus
#
# $Id: extract_raw_torrent.pl 1283 2011-10-19 03:36:39Z l.fink $
#
##############################################################################

use strict;
use Getopt::Long;
use QCMG::Extract::Torrent;
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

	./extract_raw.pl -i 51100 -o /panfs/seq_raw/ -log extract.log

	Extract raw sequencing run files from LiveArc and prepare them for
	tmap mapping

        $0 -i asset_id -o /path/to/extract/to -log logfile.log

	Required:
	-i        <id>   LiveArc asset id (ingest_log asset)
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

my $qe	= QCMG::Extract::Torrent->new(	ASSETID		=> $ASSETID,
					EXTRACTTO	=> $EXTRACTTO,
					LOG_FILE	=> $LOG_FILE
				);

if($LOG_LEVEL eq 'DEBUG') {
        $qe->LA_NAMESPACE("test");
        print STDERR "Using LiveArc namespace: ", $qe->LA_NAMESPACE, "\n";
}
else {
        $qe->LA_NAMESPACE("/QCMG_torrent/");
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



# get slide name from asset name (T00001_20120419_162_ingest_log -> T00001_20120419_162)
my $slide_name		= $qe->slide_name();
#$qe->writelog(BODY => "Run name:\t$slide_name");

my $run_folder 		= $qe->run_folder();
#$qe->writelog(BODY => "Run folder:\t$run_folder");

# find SFF asset in LiveArc to extract (.barcode.sff.zip for barcoded runs; .sff
# for others)
my ($sff, $id)		= $qe->select_sff();
#$qe->writelog(BODY => "SFF asset:\t$sff\nAsset ID:\t$id");

# extract .sff file or .barcode.sff.zip file and unzip it
#$qe->extract();

# confirm checksum of SFF asset file with file on babyjesus
$qe->check_extract_checksums();

# create qsub tmap files for each SFF file
$qe->create_run_pbs();

exit(0);


