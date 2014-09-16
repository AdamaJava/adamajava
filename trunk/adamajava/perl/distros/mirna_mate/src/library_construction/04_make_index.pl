#! /usr/bin/perl -w


use Getopt::Std;

#get command line options

getopt("fo");
getopts("fo:");

#if insufficient options, print usage

if(! (($opt_f)&&($opt_o)))
        {
        print "\n\nUsage: $0\n\n";
        print "\tREQUIRED:\n";
        print "\t-f The output from script 03 should be fed to this script\n";
	print "\t-o name of the output file\n";
        die "\n\n";
        }

#set paramaters

$filename=$opt_f;
$output=$opt_o;

open(IN, $filename) or die "Can't open $filename because $!\n";
open(OUT, ">$output") or die "Can't open $output because $!\n";

$counter=0;
while ($line=<IN>) 
	{
	if ($line=~/>/)
		{
		$line=~s/>//g;
		chomp($line);	
		$sequence=<IN>;
		chomp($sequence);
		$length=length($sequence);
		print OUT "$line\t$counter\t";
		print OUT $counter+$length-1;
		print OUT "\n";
		$counter=$counter+$length+1;
		}
	}

close(IN);
close(OUT);

exit(0);




