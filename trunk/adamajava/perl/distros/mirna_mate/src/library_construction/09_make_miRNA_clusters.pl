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
        print "\t-f name of the index file for miRNAs (output from script 04)\n";
	print "\t-m name of the concatenated matches file\n";
	print "\t-o name of the output file\n";
        die "\n\n";
        }

#set paramaters

$filename=$opt_f;
$mums=$opt_m;
$output=$opt_o;

#read index into memory
#index is only tiny so this can be sped up dramatically by creating a base for each position in the index

open(IN, $filename);
while($line=<IN>)
	{
	chomp($line);
	($miRNA, $start, $end)=split(/\t/, $line);
	for ($i=$start; $i<=$end; $i++) {$index{$i}=$miRNA};
	}
close(IN);



#decode the miRNAs

open(IN, $mums);
open(TEMP, ">$mums.temp");
$lines=0;
while($line=<IN>)
	{
	$lines++;
	next if ($line=~/#/);
	if ($line=~/>/)
		{
		chomp($line);
		@blah=split(/\,/, $line);
		$id=shift(@blah);
		$id=~s/>//;
	
		#this piece of code pulls all of the miRNA names from the match line... once it hits the end it backs up one so that the 
		#only thing left in the temp directory is the matches. 
		
		$temp=shift(@blah);
		while(($temp=~/miR/)||($temp=~/let/)||($temp=~/lin/)||($temp=~/lsy/)||($temp=~/bantam/))
			{
			$temp=shift(@blah);
			}
		unshift(@blah, $temp);

		@bleah=();
	
		foreach $position (@blah)
			{
			#get rid of the mismatch numbers
			@split=split(/\./, $position);
			$position=$split[0];

			#ignore matches to the negative strand. miRNA-MATE will preferentially pull out positive matches
			#so leave everything in as sense strand
			if ($position>=0)
				{
				#identify the miRNA from the index and push it into a temporary array for printing out
				if (exists($index{$position})) {push(@bleah, $index{$position})} else {push(@bleah, "NOT FOUND")};
				}
			}
#		print TEMP "$id:";
		$outline='';
		foreach $miRNA (@bleah) {$outline=$outline.$miRNA.','};
		$outline=~s/\,$//g;
		print TEMP "$outline\n";
		undef(@bleah);
		}
	}
close(IN);
close(TEMP);



#read the temp file into memory

open(OUT, ">$output");
open(TEMP, "$mums.temp");

$groupID=1;

#read and assign group numbers (basically a first pass clustering, but can be iterated later 
while($line=<TEMP>)
	{
	chomp($line);
	@miRNAs=split(/\,/, $line);
	
	foreach $miRNA (@miRNAs)
		{
		if (exists($miRNAgroups{$miRNA})) {$miRNAgroups{$miRNA}=$miRNAgroups{$miRNA}.','.$groupID} else {$miRNAgroups{$miRNA}=$groupID};
		if (exists($groupedmiRNAs{$groupID})) {$groupedmiRNAs{$groupID}=$groupedmiRNAs{$groupID}.','.$miRNA} else {$groupedmiRNAs{$groupID}=$miRNA};
		}
	$groupID++; 
	}
close(TEMP);


#iterate until all miRNAs are accounted for

foreach $miRNA (keys %miRNAgroups)
	{
	if (exists($miRNAgroups{$miRNA})) #might not exist after miRNA is sucked into a cluster
		{
		@groupTemp=split(/\,/, $miRNAgroups{$miRNA});
		foreach $group (@groupTemp) {$groups{$group}++};
		$cluster{$miRNA}++;
		$newmiRNAs=1;
		$newGroups=1;
		while(($newmiRNAs>0)||($newGroups>0))
			{
			$newmiRNAs=0;
			$newGroups=0;
	
			#collect every group belonging to the miRNAs
			foreach $miR (keys %cluster)
				{
				@groupTemp=split(/\,/, $miRNAgroups{$miR});
                                foreach $group (@groupTemp) {if (exists($groups{$group})) {} else {$newGroups++};$groups{$group}++};
				}

			#check every miRNA belonging to the groups	
			foreach $groupID (keys %groups)
                                {
                                @miRNAtemp=split(/\,/, $groupedmiRNAs{$groupID});
                                foreach $miR (@miRNAtemp) {if (exists($cluster{$miR})) {} else {$newmiRNAs++}; $cluster{$miR}++};
				}
			}

		#print out cluster and delete the miRNAs from the miRNAgroups array: a miRNA should only ever appear in one cluster
		$output='';

		foreach $check (sort keys %cluster)
			{
			$output=$output.';'.$check;
			delete($miRNAgroups{$check});
			}

		#remove first semicolon
		$output=~s/\;//;
		print OUT "$output\n";
		undef(%cluster);
		undef(%groups);
		}
	}
close(OUT);

unlink("$mums.temp");


exit(0);
