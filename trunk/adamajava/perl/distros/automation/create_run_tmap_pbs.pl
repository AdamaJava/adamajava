#!/usr/bin/perl

# $Id: create_run_bioscope_pbs.pl 1442 2011-12-12 00:26:12Z l.fink $

use strict;
use lib qw(/panfs/imb/qcmg/software/QCMGPerl/lib/);
#use lib qw(/panfs/imb/qcmg/software/QCMGPerl/lib/thirdparty/);
use QCMG::Automation::Common;

my $qc		= QCMG::Automation::Common->new();

my $bin		= $qc->BIN_AUTO;
#my $tmpdir	= $qc->TMP_DIR;
my $tmpdir	= '/panfs/imb/automation_log/';

# /panfs/imb/test/bin/create_run_bioscope_pbs.pl -i /panfs/imb/seq_raw/$assetname  -log /panfs/imb/test/$logfile

my $assetname	= $ARGV[1];
$assetname	=~ s/.+\/(.+)$/$1/;

my $logfile	= $ARGV[3];
$logfile	=~ s/extract\.log/runbioscope\.log/;

my $argline	= join " ", @ARGV;
my $qcmg_email	= 'scott.wood@imb.uq.edu.au';


my $PBS = qq{#PBS -N run_bioscope
#PBS -S /bin/bash
#PBS -r n
#PBS -l walltime=1:00:00
#PBS -l select=1:ncpus=1:mem=1gb
#PBS -A sf-QCMG
#PBS -m ae
#PBS -j oe
#PBS -M $qcmg_email

umask 0002

$bin/run_bioscope.pl };

$PBS .= $argline;

my $script = $tmpdir."run_bioscope_pbs_$assetname.sh";

print STDERR "Writing to $script\n";

open(FH, ">$script") || die "$!: $script";
print FH $PBS;
close(FH);

print STDERR "Submitting $script\n";

my $rv = system(qq{qsub $script});
print STDERR "RV: $rv\n";

print STDERR "PBS: $bin/run_bioscope.pl $argline\n";

exit(0);
