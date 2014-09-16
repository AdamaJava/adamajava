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
	print "\t-f name of the input file\n";
	print "\t-o name of the output file\n";
        die "\n\n";
        }

#set paramaters
$filename=$opt_f;
$output=$opt_o;


#load all the sims and mums into separate hashes
open(IN, $filename) or die "Can't open $filename because $!\n";
while($line=<IN>)
	{
	chomp($line);
	if ($line=~/>/)
		{
		$line=~s/>//g;
		$sequence=<IN>;
		chomp($sequence);
		if (exists($groups{$line}))
			{
			$groups{$line}=$groups{$line}.'NNNNNNNNNN.NNNNNNNNNN'.$sequence;
			}
			else {$groups{$line}=$sequence};
		}
	}
close(IN);

open(OUT, ">$output") or die "Can't open $output because $!\n";
foreach $group (sort {length($b) <=> length($a)} keys %groups)
	{
	if (exists($groups{$group})) 					#sanity check as it may have been deleted before the loop gets to it
		{
		%names=();
		@blah=split(/\;/, $group);
		foreach $miRNA (@blah)
			{
			########========build up a list of names and base names that belong to this miRNA family for condensing
			
			@temp=split(/\-/, $miRNA);
			shift(@temp); 			#remove species name
			$miRNA=join("-", @temp);

			$names{$miRNA}++;  		#but now also add the base name to catch any of those little bastard name changing miRNAs.
			@temp=split(/\./, $miRNA);
			$names{$temp[0]}++;
			while (($miRNA=~/[0-9][a-z]$/)&&($miRNA!~/5p/)&&($miRNA!~/3p/)&&($miRNA!~/star/))
				{
				chop($miRNA);
				}
			$names{$miRNA}++;
			if (($miRNA=~/[0-9][a-z]$/)&&(($miRNA=~/5p/)||($miRNA=~/3p/)||($miRNA=~/star/)))
				{
				$suffix=pop(@temp);
				$miRNA=join("-", @temp);
				while ($miRNA=~/[0-9][a-z]$/) {chop($miRNA)};
				$miRNA=$miRNA.'-'.$suffix;
				$names{$miRNA}++
				}
			########========end name building
			}
		
		########======== Now go and search through the remaining miRNA groups, looking for family members based on the names hash
		$sequence=$groups{$group};
		delete($groups{$group});
		foreach $g2 (keys %groups)
			{
			@blah=split(/\;/, $g2);
	                foreach $miRNA (@blah)
        	                {       
                        	@temp=split(/\-/, $miRNA);
	                        shift(@temp);                   #remove species name
        	                $check=join("-", @temp);
				if (exists($names{$check})&&exists($groups{$g2})) 
					{
					$group=$group.';'.$miRNA; 
					$sequence=$sequence.'NNNNNNNNNN.NNNNNNNNNN'.$groups{$g2};
					delete($groups{$g2});
					}
					else
					{
                        		@temp=split(/\./, $check);
        	                	if (exists($names{$check})&&exists($groups{$g2})) 
	                                        {
        	                                $group=$group.';'.$miRNA; 
                	                        $sequence=$sequence.'NNNNNNNNNN.NNNNNNNNNN'.$groups{$g2};
						delete($groups{$g2});
                        	                }
                                	        else
						{
						while (($check=~/[0-9][a-z]$/)&&($check!~/5p/)&&($check!~/3p/)&&($check!~/star/))
		                                        {
                		                        chop($check);
                                		        }
						if (exists($names{$check})&&exists($groups{$g2}))
	                                                {
        	                                        $group=$group.';'.$miRNA;
                	                                $sequence=$sequence.'NNNNNNNNNN.NNNNNNNNNN'.$groups{$g2};
							delete($groups{$g2});
                        	                        }
                                	                else
							{
							 if (($check=~/[0-9][a-z]$/)&&(($check=~/5p/)||($check=~/3p/)||($check=~/star/)))
	        		                                {
        	                		                $suffix=pop(@temp);
		                        	                $check=join("-", @temp);
                			                        while ($check=~/[0-9][a-z]$/) {chop($check)};
                        	        		        $check=$check.'-'.$suffix;
								if (exists($names{$check})&&exists($groups{$g2}))
		                                                        {
                		                                        $group=$group.';'.$check;
                                		                        $sequence=$sequence.'NNNNNNNNNN.NNNNNNNNNN'.$groups{$g2};
                                                		        delete($groups{$g2});
									}
			                                        }							
							}
						}
					}
				}
			}

		$sequence=~s/N//g;
		@blah=split(/\./, $sequence);
		foreach $whatever (@blah) {print OUT ">$group\n$whatever\n"}
		undef(@blah);
		undef(@temp);
		undef(%names);
		}
        }
close(IN);
close(OUT);

exit(0);
