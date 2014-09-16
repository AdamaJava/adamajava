#! /usr/bin/perl -w 

use Getopt::Std;

#get command line options

getopt("fmo");
getopts("fmo:");

#if insufficient options, print usage

if(! (($opt_f)&&($opt_m)&&($opt_o)))
        {
        print "\n\nUsage: $0\n\n";
        print "\tREQUIRED:\n";
        print "\t-f name of the fastafile to get sequences from\n";
        print "\t-m name of the final clusters file\n";
        print "\t-o name of the output file\n";
        die "\n\n";
        }

#set paramaters

$filename=$opt_f;
$clusters=$opt_m;
$output=$opt_o;


open(IN, $filename) or die "Can't open filename $filename because $!\n";;
while($line=<IN>)
	{
	chomp($line);
	$line=~s/>//g;
	$sequence=<IN>;
	chomp($sequence);
	$tags{$line}=$sequence;
	}
close(IN);


open(IN, $clusters) or die "Can't open $clusters because $!\n";
open(OUT, ">$output") or die "Can't open $output because $!\n";
while ($line=<IN>)
	{
	chomp($line);
	@blah=split(/\;/, $line);
	foreach $miRNA (@blah)
		{
		#gather all of the sequences first... cluster unique ones...
		$sequence=$tags{$miRNA};
		$temp{$sequence}++;
		}

	#annotate each sequence with a size
	foreach $sequence (keys %temp)
		{
		$length=length($sequence);
		$temp{$sequence}=$length;
		}

	#collapse any sequences where they are fully contained within another sequence in this cluster
	#when doing this collapse, make sure that all the short sequences are done first

	foreach $sequence (sort {$temp{$b}<=>$temp{$a}} keys %temp)
		{
		$found='no';
		delete($temp{$sequence});
		foreach $reference (keys %temp)
			{
			if (exists($temp{$reference}))
				{
				if ($reference=~/$sequence/) {$found='no'} else {$found='yes'}
				}
			}
		if ($found eq 'no') {$temp{$sequence}++};
		}

	#print out non-redundant sequences
	foreach $sequence (keys %temp) {print OUT ">$line\n$sequence\n";}
	undef(%temp);
	}
close(IN);
close(OUT);
exit(0);
