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
# $Id: ingest_torrent.pl 2005 2012-06-04 03:29:43Z l.fink $
#
##############################################################################

### TEST ON 10.160.72.29 (Fakey)
# ./ingest_torrent.pl -dir /data/results/solid0777/S0449_20100603_1_Frag/

use strict;
use Getopt::Long;
use lib qw(./);
#use lib qw(/usr/local/QCMG/QCMGPerl/lib/);
use QCMG::Ingest::TorrentSuite3;

use vars qw($SVNID $REVISION $VERSION);
use vars qw($GLPID $ANALYSIS_FOLDER $LOG_FILE $LOG_LEVEL $ADD_EMAIL $NO_EMAIL);
use vars qw($USAGE);

# set command line, for logging
my $cline       = join " ", @ARGV;

&GetOptions(
                "i=s"           => \$ANALYSIS_FOLDER,
		"S!"		=> \$NO_EMAIL,
		"glpid=s"	=> \$GLPID,
                "log=s"         => \$LOG_FILE,
                "loglevel=s"    => \$LOG_LEVEL,
                "e=s"           => \$ADD_EMAIL, 
                "V!"            => \$VERSION,
                "h!"            => \$USAGE
        );

# help message
if($USAGE || (! $ANALYSIS_FOLDER && ! $GLPID) ) {
        my $message = <<USAGE;

        USAGE: $0

        Ingest raw sequencing Ion Torrent files into LiveArc from iontorrent4

        $0 -i /path/to/analysis/folder/ -log logfile.log

        Required:
        -i        <dir>  path to analysis folder to ingest
        -log      <file> log file to write execution params and info to

        Optional:
        -e        <email address> additional email addresses to notify of status
	-S	  (silent - do not send emails)
        -V        (version information)
        -h        (print this message)

USAGE

        print $message;
        exit(0);
}

#print STDERR "GLPID: $GLPID\n";

my $qi	= QCMG::Ingest::TorrentSuite3->new(LOG_FILE => $LOG_FILE, ANALYSIS_FOLDER => $ANALYSIS_FOLDER, GLPID => $GLPID, NO_EMAIL => $NO_EMAIL);

if($LOG_LEVEL eq 'DEBUG') {
        $qi->LA_NAMESPACE("/test");
        print STDERR "Using LiveArc namespace: ", $qi->LA_NAMESPACE, "\n";
}
else {
	$qi->LA_NAMESPACE("/QCMG_torrent/");
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

#print STDERR "Querying LIMS from script\n";
$qi->query_lims(GLPID => $GLPID);

# use supplied analysis folder path or infer the path from the slide name
#print STDERR "Inferring analysis folder\n";
$qi->analysis_folder();
# check that run folder can be accessed
#print STDERR "Checking folder\n";
print STDERR "Reformatting slide name\n";
$qi->reformat_slide_name();
$qi->check_folder();
#print STDERR "Getting barcodes\n";
$qi->get_barcodes();
# make sure everything is there before we go ahead
#print STDERR "Checking that all files are present\n";
$qi->check_files();	# make sure .wells, .fastq, .bam, etc. files exist
# get checksums for .wells, .fastq, .bam
#print STDERR "Generating checksums on all files\n";
$qi->checksum_files();
# generate the PDF report that the browser usually makes
#print STDERR "Generating the PDF report\n";
$qi->pdf_report();	# generate PDF report

# remove symlinks to BAMs and other large files (will make a shell script that can recreate them)
#print STDERR "Deleting symlinks\n";
$qi->delete_symlinks();

# initiate ingest
#print STDERR "Initiating ingests\n";
$qi->prepare_ingest();
$qi->ingest_wells();
#$qi->ingest_sff();
#$qi->ingest_barcode_sff();
#$qi->ingest_fastq();
$qi->ingest_bam();
$qi->ingest_plugin_out();
$qi->ingest_report();
#$qi->ingest_plugin_files();
$qi->ingest_analysis(); # whole analysis dir (remove other files/dirs first)

exit(0);

