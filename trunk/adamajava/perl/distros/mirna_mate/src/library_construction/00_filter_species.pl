#! /usr/bin/perl -w

use Getopt::Std;

#get command line options

getopt("fos");
getopts("fos:");

#if insufficient options, print usage

if(! (($opt_f)&&($opt_o)&&($opt_s)))
        {
        print "\n\nUsage: $0\n\n";
        print "\tREQUIRED:\n";
        print "\t-f This program takes a fasta file as input - download from miRBase as appropriate\n";
        print "\t-o name of the output file\n";
	print "\t-s search term (three letter miRBase species code; eg. hsa)\n";
        die "\n\n";
        }

#set paramaters

$filename=$opt_f;
$output=$opt_o;
$search=$opt_s;

open(IN, $filename) or die "Can't open $filename because $!\n";
open(OUT, ">$output") or die "Can't open $output because $!\n";

while($line=<IN>)
	{
	if($line=~/>/)
		{
		$sequence=<IN>;
		if($line=~/$search/) {print OUT "$line"; print OUT "$sequence"};
		}
	}
close(IN);
close(OUT);
	
exit(0);
