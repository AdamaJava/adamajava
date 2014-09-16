#!/usr/bin/perl
##############################################################################
#
#  Program:  ingest_miseq.pl
#  Author:   Lynn Fink
#  Created:  2011-02-03
#
# Raw sequencing file ingestion script; copies files from sequencing machines to
# archives and cluster for Illumina HiSeq machines
#
# $Id: ingest_miseq.pl 4657 2014-07-20 23:22:51Z s.kazakoff $
#
##############################################################################

$ENV{'PERL5LIB'} = "/share/software/QCMGPerl/lib/";

use strict;
use warnings;

use Getopt::Long;
use Data::Dumper;
#use lib qw(/panfs/home/skazakoff/devel/QCMGPerl/lib/);
use QCMG::Ingest::MiSeq;

use vars qw($SVNID $REVISION $VERSION);
use vars qw($SLIDE_FOLDER $GLPID $LOG_FILE $LOG_LEVEL $ADD_EMAIL $NO_EMAIL);
use vars qw($USAGE);

BEGIN {
    select STDERR; $| = 1;
    select STDOUT; $| = 1;
}   

# defaults
$NO_EMAIL       = 0;
$GLPID          = 0;

# set command line, for logging
my $cmdline       = join " ", @ARGV;

&GetOptions(
                "i=s"           => \$SLIDE_FOLDER,
                "glpid=s"       => \$GLPID,
                "S!"            => \$NO_EMAIL,
                "log=s"         => \$LOG_FILE,
                "loglevel=s"    => \$LOG_LEVEL,
                "e=s"           => \$ADD_EMAIL,
                "V!"            => \$VERSION,
                "h!"            => \$USAGE
        );

# help message
if($USAGE || ! $SLIDE_FOLDER ) {
        my $message = <<USAGE;

        USAGE: $0

        Ingest raw sequencing run files into LiveArc

        $0 -i SLIDENAME

        Required:
        -i        <dir> just slide name (path will be added)

        Optional:
        -glpid    <string> Geneus process ID (for setting ingest status flag)
        -log      <file> log file to write execution params and info to
                         [defaults to "slidename_ingest.log" under run folder]
        -loglevel <string> DEBUG: reverts to "/test/" LiveArc namespace
        -e        <email address> additional email addresses to notify of status
        -S        (silent - do not send emails)
        -V        (version information)
        -h        (print this message)

USAGE

        print $message;
        exit(0);
}

umask 0002;

if($SLIDE_FOLDER !~ /\/mnt\/HiSeq\//) {
        # add path if only a slide name is provided
        $SLIDE_FOLDER = '/mnt/HiSeq/runs/'.$SLIDE_FOLDER;
        print STDERR "Guessing at slide folder: $SLIDE_FOLDER\n";

        # try one more time (temporary measure until the disks are remounted)
        if(! -e $SLIDE_FOLDER) {
                $SLIDE_FOLDER =~ s/HiSeq/HiSeqNew/;
                print STDERR "Guessing at slide folder: $SLIDE_FOLDER\n";
        }
}

# ADD NO_EMAIL => 1 to suppress emails
my $qi          = QCMG::Ingest::MiSeq->new(SLIDE_FOLDER => $SLIDE_FOLDER, LOG_FILE => $LOG_FILE, NO_EMAIL => $NO_EMAIL, GLPID => $GLPID);

if ($LOG_LEVEL && $LOG_LEVEL eq 'DEBUG') {
        $qi->LA_NAMESPACE("test");
        print STDERR "Using LiveArc namespace: ", $qi->LA_NAMESPACE, "\n";
}
else {
        $qi->LA_NAMESPACE("/QCMG_miseq/");
        print STDERR "Using LiveArc namespace: ", $qi->LA_NAMESPACE, "\n";
}

if($ADD_EMAIL) {
        $qi->add_email(EMAIL => $ADD_EMAIL);
}

$qi->cmdline(LINE => $cmdline);

$qi->execlog();
$qi->toollog();

sleep 1800;

$qi->check_folder;
$qi->check_metafiles;

$qi->read_samplesheet;
$qi->get_slide_data_from_Geneus;
$qi->get_parent_path_prefixes;

$qi->find_fastq_files();
$qi->checksum_files();

$qi->copy_FASTQ_to_seq_raw();
$qi->apply_adapter_masking();

exit;

##############################################################################
