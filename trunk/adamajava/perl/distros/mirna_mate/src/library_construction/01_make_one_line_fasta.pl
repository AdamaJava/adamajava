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
        print "\t-f This program takes a fasta file as input - download from miRBase as appropriate\n";
        print "\t-o name of the output file\n";
        die "\n\n";
        }

#set paramaters

$filename=$opt_f;
$output=$opt_o;

open(IN, $filename) or die "Can't open $filename because $!\n";
open(OUT, ">$output") or die "Can't open $output because $!\n";

$first_line='yes';
while ($line=<IN>)
	{
	chomp($line);
	if ($line=~/>/)
		{
		#line is a header line
		if ($first_line eq 'yes') 
			{
			$first_line='no';
			$output=$line."\n";
			}
			else
			{
			$output="\n".$line."\n";
			}
		}
		else
		{
		$output=$line;
		}
	print OUT "$output";
	}
exit(0);
