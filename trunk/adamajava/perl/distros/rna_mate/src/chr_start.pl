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
	#sort the input file
        my $f_sort = "$input.sorted";
	if( !(-e $f_sort) ){ system("sort -n -k1 $input > $f_sort") }

        #create wiggle plot file
	&create_starts($f_sort,$output);
	
        #creat a mark file if success for waiting queue system
        open(SUCC,">$f_success") or die "can't open file $f_success\n";
        print SUCC "created $output\n";
        close(SUCC);

        #print "done: $input\n";

};
if($@){
        warn $@;
}


exit;

sub create_starts{

	my $fin = shift;
	my $fout = shift;
	
	#open both input and output file
	open(IN,$fin) or die "can't open file: $fin \n";
	open(OUT,">$fout") or die "can't open file: $fout \n";

	#if the input file exist and nonzero size
	if(-s $fin){
		#read the first line from the input file
		my $line = <IN>;
		chomp($line);
		my ($current_start,$current_end,$current_score) = split(/\t/,$line);
	
		#for following lines
		my $next_line;	
		my $total_score = $current_score;
		while($next_line = <IN>){
			chomp($next_line);
			my ($next_start,$next_end,$next_score) = split(/\t/,$next_line);
			if ($next_start == $current_start){
				$total_score += $next_score;
			}else{
				my $end = $current_start + 1;
				print OUT "$current_start\t$end\t$total_score\n";
				$total_score = $next_score;
				$current_start = $next_start;
			}
		}
		
		#print last start position into output file
		my $end = $current_start + 1;
		print OUT "$current_start\t$end\t$total_score\n";
	}
	
	
	
	close(IN);
	close(OUT);

}
