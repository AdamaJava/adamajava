#!/usr/bin/perl

# $Id: create_extract_raw_qsub.pl 1283 2011-10-19 03:36:39Z l.fink $

use strict;
#use lib qw(/panfs/imb/qcmg/software/QCMGPerl/lib/);
#use lib qw(/panfs/imb/qcmg/software/QCMGPerl/lib/thirdparty/);
use QCMG::Automation::Common;

my $qc 		= QCMG::Automation::Common->new();

my $bin		= $qc->BIN_AUTO;
my $tmpdir	= $qc->TMP_DIR;

my $assetname	= $ARGV[1];
my $qcmg_email	= 'l.fink@imb.uq.edu.au';

my $argline = join " ", @ARGV;

my $PBS = qq{#!/bin/sh
#PBS -N extract_raw
#PBS -q batch
#PBS -r n 
#PBS -l ncpus=4,walltime=10:00:00,mem=1gb
#PBS -m ae 
#PBS -M $qcmg_email

umask 0002

$bin/extract_raw_torrent.pl };

$PBS .= $argline;

# write pbs script to scratch
#my $tmpdir	= $qc->TMP_DIR;
#$tmpdir		= "/panfs/imb/test/" if(! $tmpdir);	# DEBUG ONLY
my $tmpdir		= $qc->AUTOMATION_LOG_DIR;# if(! $tmpdir);	# DEBUG ONLY

my $script = "$tmpdir/extract_raw_torrent_pbs_$assetname.sh";

print STDERR "Writing to $script\n";

open(FH, ">$script") || die;
print FH $PBS;
close(FH);

print STDERR "Submitting $script\n";

my $rv = system(qq{qsub $script});
print STDERR "RV: $rv\n";

print STDERR "PBS: $argline\n";

#unlink($script);

exit(0);
