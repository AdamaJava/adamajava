#! /usr/bin/perl -w

use Getopt::Std;

#get command line options

getopt("fp");
getopts("fp:");

#if insufficient options, print usage

if(! (($opt_f)&&($opt_p)))
        {
        print "\n\nUsage: $0\n\n";
        print "\tREQUIRED:\n";
        print "\t-f name of the index file to re-annotate\n";
        print "\t-p three letter code of the preferred species (eg. hsa) \n";
        die "\n\n";
        }

#set paramaters

$filename=$opt_f;
$prefered=$opt_p;

open(IN, "$filename") or die "Can't open $filename because $!\n";
open(REC, ">$filename.recursive.$prefered") or die "Can't open $filename.recursive.$prefered because $!\n";
open(EDI, ">$filename.isomiR.$prefered") or die "Can't open $filename.isomiR.$prefered because $!\n";

while($line=<IN>)
	{
	chomp($line);
	if ($line=~/\;/)
		{
		if ($line=~/$prefered/) {$output="$prefered".'-'."miRNA-family__"}
			else {$output="miRNA-family__"}
		@blah=split(/\t/, $line);
		@temp=split(/\;/, $blah[0]);
		foreach $miRNA (@temp)
			{
			@parts=split(/\-/, $miRNA);
                	shift(@parts);                                     #remove species name
			$group=join("-", @parts);                
			$groups{$group}++
			}
		$name=join("_", keys(%groups)); 
		if (length($name)>10)
			{
			@parts=keys(%groups);
			foreach $part (@parts) 
				{
				$part=~s/\-star//g;
				$part=~s/\-5p//g;
				$part=~s/\-3p//g;
				$part=~s/miR-//g;
				$part=~s/miR//g;
				while (($part=~/[0-9][a-z]/)&&($part!~/5p/)&&($part!~/3p/)) {chop($part)}
				$check{$part}++;
				}
			$name=join("_", keys(%check));
			if (length($name)>50) {print STDOUT "Oops, didn't work: $name\n"};
			undef(%check);
			}
		$output=$output.$name;
		$hash{$output}++;
		print REC "$output\t$blah[1]\t$blah[2]\n";
		$output=$output.'_'.$hash{$output};
		print EDI "$output\t$blah[1]\t$blah[2]\n";
		undef(%groups);
		undef(@parts);
		undef(@temp);
		}
		else {print REC "$line\n"; print EDI "$line\n";}
	}
close(IN);
close(REC);
close(EDI);
exit(0);	
