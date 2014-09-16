#! /usr/bin/perl -w

use Getopt::Std;

#get command line options

getopt("f");
getopts("f:");

#if insufficient options, print usage

if(! ($opt_f))
        {
        print "\n\nUsage: $0\n\n";
        print "\tREQUIRED:\n";
        print "\t-f The output from script 02 should be fed to this script\n";
        print "\t This file will output a series of files prefixed with \"split_miRbase\" followed by the length of the miRNAs in that file\n";
	die "\n\n";
        }

#set paramaters

$filename=$opt_f;




open(IN, $filename);
while($line=<IN>)
	{
	if ($line=~/>/)
		{
		chomp($line);
		$sequence=<IN>;
		chomp($sequence);
		$length=length($sequence);
		open(OUT, ">>split_miRbase.$length");
		print OUT "$line\n$sequence\n";
		close(OUT);
		}
	}
close(IN);
exit(0);
