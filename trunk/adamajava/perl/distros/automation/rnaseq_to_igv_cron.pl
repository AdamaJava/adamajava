#!/usr/bin/perl

# this should run as a cron job every day (?) to check for new RNAseq data and
# then create an IGV file for visualizing the RNA-seq data; it will crawl
# /mnt/seq_results/ and look for files in
# /mnt/seq_results/PROJECT/DONOR/rna_seq/rsem/ 
#  and
# /mnt/seq_results/PROJECT/DONOR/rna_seq/mapsplice/
#
# the output file will go in 
# /mnt/seq_results/PROJECT/DONOR/rna_seq/rsem/

=cut

SAMPLE SESSION:

[pbailey@qcmg-clustermk2 R_code]$ ./rnaseq_to_igv_v2.R -u /mnt/seq_results/icgc_pancreatic/APGI_1839/rna_seq/mapsplice/130311_7001238_0117_BC1UGCACXX.nopd.APGI_1839.fusions_candidates.txt -x /mnt/seq_results/icgc_pancreatic/APGI_1839/rna_seq/mapsplice/130311_7001238_0117_BC1UGCACXX.nopd.APGI_1839.deletions.txt -j /mnt/seq_results/icgc_pancreatic/APGI_1839/rna_seq/mapsplice/130311_7001238_0117_BC1UGCACXX.nopd.APGI_1839.junctions.txt -i /mnt/seq_results/icgc_pancreatic/APGI_1839/rna_seq/mapsplice/130311_7001238_0117_BC1UGCACXX.nopd.APGI_1839.insertions.txt -c /mnt/seq_results/icgc_pancreatic/APGI_1839/rna_seq/rsem/130311_7001238_0117_BC1UGCACXX.nopd.APGI_1839.genes.results
Saving gct data to file:/mnt/seq_results/icgc_pancreatic/APGI_1839/rna_seq/rsem/130311_7001238_0117_BC1UGCACXX.nopd.APGI_1839.gct
Finished writing to file ...
Warning message:
In write.table(gct, file = paste(savePath, gsub("genes.results",  :
  appending column names to file
[1] "/mnt/seq_results/icgc_pancreatic/APGI_1839/rna_seq/mapsplice/130311_7001238_0117_BC1UGCACXX.nopd.APGI_1839.junctions.bed ready for IGV review!"
[1] "/mnt/seq_results/icgc_pancreatic/APGI_1839/rna_seq/mapsplice/130311_7001238_0117_BC1UGCACXX.nopd.APGI_1839.deletions.bed ready for IGV review!"
[1] "/mnt/seq_results/icgc_pancreatic/APGI_1839/rna_seq/mapsplice/130311_7001238_0117_BC1UGCACXX.nopd.APGI_1839.insertions.bed ready for IGV review!"
Download and preprocess the 'chrominfo' data frame … OK
=cut

use strict;
use File::Spec;
use Time::localtime;
use File::stat;

#my $seq_results	= q{/mnt/seq_results/};
my $seq_results	= q{/mnt/seq_results/icgc_pancreatic/};
my $bin		= $ENV{'QCMG_SVN'}."/QCMGPerl/distros/automation/";
my $script	= "rnaseq_to_igv.R";

# find all potential RNA-seq data sets
my $cmd		= qq{find $seq_results -type f -name "*.genes.results"};
print STDERR "$cmd\n";
my @rsemfiles	= `$cmd`;

foreach my $genes (@rsemfiles) {
	chomp $genes;

	# skip it if it is not in the rsem directory
	# /mnt/seq_results//icgc_pancreatic/APGI_2156/rna_seq/rsem/130227_7001238_0114_BC1ELJACXX.nopd.APGI_2156.genes.results
	next if($genes !~ /\/rna\_seq\/rsem/);

	print STDERR "rsem genes.results file:\t$genes\n";

	my ($v, $d, $f)	= File::Spec->splitpath($genes);

	$f		=~ /^(.+?)\.(nopd)\.(.+?)\.genes/;
	my $slide	= $1;	# 130227_7001238_0114_BC1ELJACXX
	my $donor	= $3;	# APGI_2156
	my $mapset	= join ".", $slide, $2, $donor;

	# get top level RNA-seq analysis directory
	# /mnt/seq_results//icgc_pancreatic/APGI_2156/rna_seq/
	my $rnaseqdir	= $d;
	$rnaseqdir	=~ s/rsem\/$//;
	# create name for mapsplice dir
	# # /mnt/seq_results//icgc_pancreatic/APGI_2156/rna_seq/mapsplice/
	my $msdir	= join "/", $rnaseqdir, "mapsplice";

	# infer component files from mapsplice dir
	my $fus		= join "/", $rnaseqdir, "mapsplice", $mapset.".fusions_candidates.txt";
	my $del		= join "/", $rnaseqdir, "mapsplice", $mapset.".deletions.txt";
	my $ins		= join "/", $rnaseqdir, "mapsplice", $mapset.".insertions.txt";
	my $jun		= join "/", $rnaseqdir, "mapsplice", $mapset.".junctions.txt";

	print STDERR "Checking files:\n";
	print STDERR "$fus\n";
	print STDERR "$del\n";
	print STDERR "$ins\n";
	print STDERR "$jun\n";

	# check if all files exist; if not, skip to next donor (?) 
	unless(-e $fus && -e $del && -e $ins && -e $jun) {
		print STDERR "Not all files present, skipping\n";
		next;
	}

	# get file creation times
	my $ctime_fus	= ctime( stat($fus)->ctime );
	my $ctime_del	= ctime( stat($del)->ctime );
	my $ctime_ins	= ctime( stat($ins)->ctime );
	my $ctime_jun	= ctime( stat($jun)->ctime );
	my $ctime_genes	= ctime( stat($genes)->ctime );

	# infer IGV vis file
	# /mnt/seq_results/icgc_pancreatic/APGI_2156/rna_seq/rsem/130227_7001238_0114_BC1ELJACXX.nopd.APGI_2156.gct
	my $gct		= join "/", $rnaseqdir, "rsem", $donor.".gct";
	my $ctime_gct	= 0;
	my $ctime_gct	= ctime( stat($gct)->ctime ) if(-e $gct);


	if(-e $gct) {
		print STDERR "GCT file exists, checking currency\n";
		# don't bother recreating file if it is current
		if(	$ctime_gct > $ctime_fus &&
			$ctime_gct > $ctime_del &&
			$ctime_gct > $ctime_ins &&
			$ctime_gct > $ctime_jun &&
			$ctime_gct > $ctime_genes) {
			print STDERR "GCT file current, skipping\n";
			next;
		}
	}

	# if file doesn't exist or isn't current, create it
	my $cmd		= qq{$bin/$script -u $fus -x $del -i $ins -j $jun -c $genes};
	print STDERR "$cmd\n";
	#my $rv		= system($cmd);
	#print STDERR "$rv : $cmd\n";
}

exit(0);
