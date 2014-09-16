#! /usr/bin/perl -w

use Cwd;
use Getopt::Std;

#get command line options

getopt("lmaps");
getopts("lmaps:");

#if insufficient options, print usage

if(! (($opt_l)&&($opt_m)&&($opt_a)&&($opt_p)&&($opt_s)))
        {
        print "\n\nUsage: $0\n\n";
        print "\tREQUIRED:\n";
        print "\t-l name of the library file (this is the output from script 05)\n";
	print "\t-p location of mapreads program including full path (eg. /data/matching/mapreads)\n";
	print "\t-s full path to the schemas directory (eg. /data/matching/schemas/)\n";
        print "\t-m number of mismatches \n";
	print "\t-a valid adjacent? 0=no 1=yes \n";
	die "\n\n";
        }

#set paramaters

$library=$opt_l;
$mismatch=$opt_m;
$adjacent=$opt_a;
$dir=cwd();
$mapreads=$opt_p;
$schemas=$opt_s;

while($filename=glob('*.csfasta'))
	{	
	$size=$filename;
	$size=~s/split_miRbase\.//g;
	$size=~s/\.csfasta//g;

	$schema="$schemas/schema_".$size."_".$mismatch;

	#write shell script

	open(OUT, ">$filename.sh");	
	print OUT "$mapreads $dir/$filename $dir/$library  M=$mismatch A=$adjacent L=$size T=$schema >$dir/$filename.ma.$size.$mismatch.$adjacent\n";
	close(OUT);

	system("qsub -l walltime=2:00:00 $filename.sh");

	}
exit(0);
