#! /usr/bin/perl -w

use Getopt::Std;
use Cwd;
use strict;

#get command line options
my %opt = ();
getopt("ios",\%opt);

if(! ($opt{i} && $opt{o} && $opt{s} )){&usage()}

my $input = $opt{i};
my $output = $opt{o};
my $f_success = $opt{s};
eval{
	my $f_sort = "$input.sorted";
	system("sort -n -k1 $input > $f_sort");
	my $start_end = &sort_array($f_sort);
	#create wiggle plot file
	&create_wig($start_end,$f_sort);
	
	#creat a mark file if success for waiting queue system
	open(SUCC,">$f_success") or die "can't open file $f_success\n";
	print SUCC "created $output\n";
	close(SUCC);
	
	#print "done\n";
	
};
if($@){
	warn $@; 
}



exit;
sub create_wig{
	my $start_end = shift;
	my $f_sort = shift;

	#initialise output file
	open(OUT,">$output") or die "can't open file: $output\n";
	close(OUT);
	

	open(SORTED,$f_sort) or die "can't open file $f_sort";
	
	my $i_start = shift(@$start_end);
	my @next_start = ();
	my %current_line = (start=>0,end=>0,scort=>0);
	foreach my $i_end (@$start_end){
		if($i_end == $i_start){next}
		#print "foreach: $i_start\t$i_end\n";
        	my @current_start = @next_start;
		# read a line from <SORTED>
		if( ($current_line{'start'} == $i_start) || ($current_line{'start'} == 0 )){
		    if( $current_line{'start'} != 0) { push(@current_start,{%current_line})  }
		    # %current_line = ();
		    while(my $line = <SORTED>){
			chomp($line);
			my ($start,$end,$score) = split(/\t/,$line);
			%current_line = (start=>$start,end=>$end,score=>$score);
			#it belongs to next range
			if($start > $i_start){ 	last }

			# for current range [$i_start,$i_end)
			if($start == $i_start){
				push(@current_start,{start=>$start,end=>$end,score=>$score});
			}
			# wrong range
			else{
				die "error:$line is out of range ($i_start,$i_end)";			
			}
		   }#end while
		}#endif
		#$i_start\t $i_end\t $total_score
		@next_start = &count_score($i_start,$i_end,\@current_start);
		$i_start = $i_end;
	}

	close(SORTED);
}
sub count_score{
	my $i_start = shift;
	my $i_end = shift;
	my $array = shift;
	
	my $total = 0;
	my @next_start = ();
	
	my $flag = 1;
	foreach my $a (@$array){
		if($a->{'start'} < $i_start){
			$flag = 0;
			print "$a->{'start'} : $i_start\n";
			last;
		}

		if($a->{'end'} < $i_end ){
			$flag = 0; 
			print "$a->{'end'} : $i_end \n";
			last;
		}
		if($a->{'start'} > $i_start){ last } #the $a belong to next range
		$total += $a->{'score'};
		$a->{'start'} = $i_end;
		#filter out start == end posion
		if($a->{'start'} < $a->{'end'}){push(@next_start,{%$a})}
	}
	
	open(OUT,">>$output");
	if($flag == 0){
		#print some markd
		foreach my $a (@$array){print "flag=0: $a->{'start'}\t$a->{'end'}\t$a->{'score'} \n"}
		print OUT "$i_start\t$i_end\tERROR\n";
		die "error (flag == 0)  on creating: $output\n"
	 }
	 
	#write right value to output
	if($total > 0) {	print OUT "$i_start\t$i_end\t$total\n" }
	
	close(OUT);

	return @next_start;

}

sub sort_array {
	my $f_sort = shift;

	my  @start_end = ();
	open(SORTED,"$input.sorted") or die "can't open $input.sorted\n" ;
	while(<SORTED>){
		chomp();
		my ($start,$end,$score) = split(/\t/);
		push(@start_end,$start);
		push(@start_end,$end);
	}
	close(SORTED);
	
	#my $i = 0;
	#my $t;
	#my $n = @start_end;
	#foreach $t (@start_end){$i++}
	#print "lines:$i($n) -- end: $t ($start_end[$i-1])\n";

	my @sorted = sort{$a <=> $b} @start_end;
	@start_end = (); # release memory
	
	return \@sorted;

}

sub usage{
	print "\n Usage: $0\n\n";
	print "\t Required: \n";
	print "\t -i input file (eg. chr10.ESEB.for_wig.positive)\n";
	print "\t -o output file (eg. chr10.ESEB.for_wig.positive.wig)\n";
	print "\t -s success file name (eg. chr10.ESEB.for_wig.positive.wig.success)\n";


	return -1;
}
