#! /usr/bin/perl -w

while($filename=glob('split_miRbase.*'))
	{
	open(IN, $filename);
	open(OUT, ">$filename.csfasta");

	while($fragment=<IN>)
		{
		chomp($fragment);
		if ($fragment=~/>/) {print OUT "$fragment\n"}
			else
			{
        	        #make sure capitalization is consistent so that pattern matching works.
	                $fragment =~ s/t/T/g;
                	$fragment =~ s/a/A/g;
        	        $fragment =~ s/c/C/g;
	                $fragment =~ s/g/G/g;

                	#convert the fragment to colourspace
	                @nt = split(//, $fragment);
			unshift(@nt, 'T');
        	        $out=$nt[0];
	                $length=@nt;
			for (my $count=0; $count<$length-1; $count++)
                	        {
        	                $di_nt="";
	                        $di_nt=$nt[$count].$nt[$count+1];
                        	if ($di_nt =~ /AA|CC|GG|TT/) {$out=$out.'0'}
                	                elsif ($di_nt =~ /CA|AC|TG|GT/) {$out=$out.'1'}
        	                        elsif ($di_nt =~ /GA|TC|AG|CT/) {$out=$out.'2'}
	                                elsif ($di_nt =~ /TA|GC|CG|AT/) {$out=$out.'3'}
                                	else {$out=$out.'9'};
				}
			print OUT "$out\n";
			}
		}
	close(IN);
	close(OUT);
	}
exit(0);
