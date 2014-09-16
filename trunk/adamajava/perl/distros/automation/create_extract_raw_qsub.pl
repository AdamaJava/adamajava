#!/usr/bin/perl

# $Id: create_extract_raw_qsub.pl 2641 2012-08-01 11:37:07Z l.fink $

use strict;
#use lib qw(/panfs/imb/qcmg/software/QCMGPerl/lib/);
use lib qw(/share/software/QCMGPerl/lib/);
use QCMG::Automation::Common;

my $qc 		= QCMG::Automation::Common->new();

my $bin		= $qc->BIN_AUTO;
my $tmpdir	= $qc->TMP_DIR;

my $assetname	= $ARGV[1];
my $qcmg_email	= 'l.fink@imb.uq.edu.au';

my $argline = join " ", @ARGV;

my $PBS = qq{#!/bin/sh
#PBS -N extract_raw
#PBS -r n
#PBS -l walltime=10:00:00
#PBS -l select=1:ncpus=1:mem=1gb
#PBS -A sf-QCMG
#PBS -m ae
#PBS -j oe
#PBS -M $qcmg_email

umask 0002

$bin/extract_raw.pl };

$PBS .= $argline;

# write pbs script to scratch
my $tmpdir	= $qc->TMP_DIR;
#$tmpdir		= "/panfs/imb/test/" if(! $tmpdir);	# DEBUG ONLY
$tmpdir		= "/panfs/automation_log/" if(! $tmpdir);	# DEBUG ONLY

my $script = "$tmpdir/extract_raw_pbs_$assetname.sh";

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
