#!/usr/bin/perl

# $Id: create_run_lifescope_pbs.pl 1283 2011-10-19 03:36:39Z l.fink $

use strict;
use lib qw(/panfs/imb/qcmg/software/QCMGPerl/lib/);
use QCMG::Automation::Common;

my $qc		= QCMG::Automation::Common->new();

my $bin		= $qc->BIN_AUTO;
#my $tmpdir	= $qc->TMP_DIR;
my $tmpdir	= '/panfs/imb/automation_log/';

# /panfs/imb/test/bin/create_run_lifescope_pbs.pl -i /panfs/imb/seq_raw/$assetname  -log /panfs/imb/test/$logfile
# $assetname = S8006_20110815_1_nort.lane_02.nobc.xsq
# .ls file: /panfs/imb/seq_raw/S8006_20110815_1_nort/S8006_20110815_1_nort/20111209/S8006_20110815_1_nort.lane_02.nobc.ls

# ARGV  0  1                              2    3
#       -i /panfs/imb/seq_raw/$assetname  -log /panfs/imb/test/$logfile
my $assetname	= $ARGV[1];	# path on barrine to exxtracted .xsq file
$assetname	=~ s/.+\/(.+)\.xsq$/$1/;	# remove .xsq suffix to get slide/lane name
# S8006_20110815_1_nort.lane_01.nobc.xsq_extract.log
my $logfile	= $ARGV[3];
$logfile	=~ s/_extract\.log/_runlifescope\.log/;	# SHOULD NOT HARDCODE...

# use same command line for next script as this one, although assetname and
# logfile have been altered:
# run_lifescope.pl -i $assetname  -log $logfile
my $argline	= join " ", @ARGV;
#my $qcmg_email	= 'scott.wood@imb.uq.edu.au';
my $qcmg_email	= 'l.fink@imb.uq.edu.au';

# create PBS script content from template and append command line for next
# script
my $PBS = qq{#PBS -N run_lifescope
#PBS -S /bin/bash
#PBS -r n
#PBS -l walltime=1:00:00
#PBS -l select=1:ncpus=1:mem=1gb
#PBS -A sf-QCMG
#PBS -m ae
#PBS -j oe
#PBS -M $qcmg_email

umask 0002

$bin/run_lifescope.pl };
$PBS .= $argline;

# filename for PBS script
my $script = $tmpdir."run_lifescope_pbs_$assetname.sh";

# write PBS script content to file
print STDERR "Writing to $script\n";
open(FH, ">$script") || die "$!: $script";
print FH $PBS;
close(FH);

# qsub PBS script
print STDERR "Submitting $script\n";
my $rv = system(qq{qsub $script});
print STDERR "RV: $rv\n";

#print STDERR "PBS: $bin/run_lifescope.pl $argline\n";

# remove PBS script
#unlink($script);

exit(0);
