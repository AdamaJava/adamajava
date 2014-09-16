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
        print "\t-f This program takes a one_line fasta file as input - use output from other scripts or unix commands as appropriate\n";
        print "\t-o name of the output file\n";
        die "\n\n";
        }

#set paramaters

$filename=$opt_f;
$output=$opt_o;

open(IN, $filename) or die "Can't open $filename because $!\n";
open(OUT, ">$output") or die "Can't open $output because $!\n";

while($line=<IN>)
	{
	if ($line=~/>/)
		{
		chomp($line);
		@blah=split(/\s+/, $line);
		$blah[0]=~s/\*/-star/g;
		print OUT "$blah[0]\n";
		$line=<IN>;
		$line=~s/U/T/g;
		print OUT "$line";
		}
	}

close(IN);
close(OUT);

exit(0);
