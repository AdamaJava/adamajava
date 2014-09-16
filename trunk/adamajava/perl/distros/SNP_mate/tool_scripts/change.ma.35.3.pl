#! /usr/bin/perl -w

use strict;

my $f_in = "/data/cxu/SNPs/xu_SNPs/test_output/chr10.tag_20000_F3.mers35.unique.csfasta.ma.35.3";
my $f_out = "/data/cxu/SNPs/xu_SNPs/test_output/chr10.tag_20000_F3.mers35.unique.csfasta.ma.35.3.changed";
open(IN, $f_in) or die "can't open $f_in\n";
open(OUT, ">$f_out") or die "can't open $f_out\n";
while(my $line = <IN>){
	if($line =~ m/^>/){ $line =~ s/_\d/_F3/  }
	print OUT $line;
}

close(OUT);
close(IN);

