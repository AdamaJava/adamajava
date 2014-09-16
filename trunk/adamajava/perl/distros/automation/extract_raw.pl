#!/usr/bin/perl

##############################################################################
#
#  Program:  extract_raw.pl
#  Author:   Lynn Fink
#  Created:  2011-10-03
#
# Raw sequencing file extraction script; copies files from LiveArc and prepares
# them for Bioscope mapping on barrine
#
# $Id: extract_raw.pl 2642 2012-08-02 00:50:50Z l.fink $
#
##############################################################################

use strict;
use Getopt::Long;
use lib qw(/share/software/QCMGPerl/lib/);
use QCMG::Extract::Solid;
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

	./extract_raw.pl -i 40453 -o /panfs/seq_raw/ -log extract.log

	Extract raw sequencing run files from LiveArc and prepare them for
	Bioscope mapping

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

# CAPTURE LIVEARC STDERR TO VARIABLE
# First, save away STDERR
=cut
my $stderrcap;
open SAVEERR, ">&STDERR";
close STDERR;
open STDERR, ">", \$stderrcap or warn "Cannot save STDERR to scalar\n";
=cut

my $qe	= QCMG::Extract::Solid->new(	ASSETID		=> $ASSETID,
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
$qe->writelog(BODY => "Run name: $slide_name");

my ($bioini_folder, $bioini_id) = $qe->bioscope_folder();
$qe->writelog(BODY => "Bioscope folder:   $bioini_folder");
$qe->writelog(BODY => "Bioscope asset ID: $bioini_id");

my $run_folder 		= $qe->run_folder();
$qe->writelog(BODY => "Run folder: $run_folder");

#$qe->write_pbs_script();

$qe->extract();

$qe->check_extract_seq_raw();
#$qe->check_extract_bioscope();
$qe->check_extract_checksums();

=cut
# Now close and restore STDERR to original condition.
close STDERR;
open STDERR, ">&SAVEERR";
$qe->writelog(BODY => "LiveArc STDERR: $stderrcap");
=cut

exit(0);


