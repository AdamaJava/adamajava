#!/usr/bin/perl

##############################################################################
#
#  Program:  ingest_rnaseq_qc.pl
#  Author:   Lynn Fink
#  Created:  2013-04-10
#
# RNA_seQC file ingestion script; copies files from babyjesus
# and archives them in LiveArc
#
# $Id: ingest_rna_seqc.pl 3669 2013-04-10 05:45:22Z l.fink $
#
##############################################################################

use strict;
use Getopt::Long;
use lib qw(/share/software/QCMGPerl/lib/);
#use lib qw(/panfs/home/lfink/devel/QCMGPerl/lib/);
use QCMG::Ingest::RNA_seQC;
use File::Spec;

use vars qw($SVNID $REVISION $VERSION);
use vars qw($SLIDE $LOG_FILE $LOG_LEVEL $ADD_EMAIL $UPDATE);
use vars qw($USAGE);

# set command line, for logging
my $cline	= join " ", @ARGV;

&GetOptions(
		"i=s"		=> \$SLIDE,
		"log=s"		=> \$LOG_FILE,
		"loglevel=s"	=> \$LOG_LEVEL,
		"e=s"		=> \$ADD_EMAIL, 
		"V!"		=> \$VERSION,
		"update!"	=> \$UPDATE,
		"h!"		=> \$USAGE
	);

# help message
if($USAGE || (! $SLIDE)) {
        my $message = <<USAGE;

        USAGE: $0

	Ingest rnaseq_qc files into LiveArc

        $0 -i slide -log logfile.log

	Required:
	-i	<slide>	    slide name (e.g., 120511_SN7001243_0090_BD116HACXX)

	Optional:
	-update		    update mapped assets after a partial ingest
	-log      <file>    log file to write execution params and info to
			    [defaults to "120511_SN7001243_0090_BD116HACXX_ingestmapped.log" under mapset folder]
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

my $qi		= QCMG::Ingest::RNA_seQC->new(
						SLIDE		=> $SLIDE,
						LOG_FILE	=> $LOG_FILE,
						UPDATE		=> $UPDATE
					);

# set namespace to 5500 data
$qi->LA_NAMESPACE("/QCMG_hiseq/");

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

$qi->find_log();

# initiate ingest
$qi->prepare_ingest();

# /mnt/seq_mapped//130227_7001238_0114_BC1ELJACXX/qc/
$qi->ingest();
# /mnt/seq_raw//130227_7001238_0114_BC1ELJACXX/log/130227_7001238_0114_BC1ELJACXX_qc_out.log
$qi->ingest_log();

exit(0);

