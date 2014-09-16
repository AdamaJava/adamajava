#!/usr/bin/perl

# $0 -i neighbour_snp_example.dcc1 -o test.out.dcc1 -l test.log

use strict;
use QCMG::Annotate::QsnpCompoundMutations;
use Getopt::Long;
use Data::Dumper;

my ($in, $out, $logfile, $verbose, $usage);

&GetOptions(
		"i=s"	=> \$in,
		"l=s"	=> \$logfile,
		"v!"	=> \$verbose,
		"h!"	=> \$usage
	);

if($usage || ! $in) {
	print STDERR "USAGE: $0 -i file.dcc1\n";
	exit(1);
}

# create copy of DCC1 file before changing it but don't overwrite an existing
# version
my $newdcc1	= $in.".noncompoundedsnps";
if(-e $newdcc1) {
	print STDERR "Cannot create copy of DCC1 file; please move or delete existing $newdcc1\n";
	exit(1);
}
my $cmd	= qq{cp $in $newdcc1};
my $rv	= system($cmd);
unless($rv == 0) {
	print STDERR "Cannot create copy of DCC1 file: $newdcc1\n";
	exit(2);
}

my $ga	= QCMG::Annotate::QsnpCompoundMutations->new(
					dcc1file	=> $newdcc1, 
					dcc1out		=> $in,
					logfile		=> $logfile,
					ensver		=> '70',
					release		=> '14',
					organism	=> 'human',
					verbose		=> 1
				);

$ga->execute();

exit(0);
