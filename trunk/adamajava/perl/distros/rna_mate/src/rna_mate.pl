#! /usr/bin/perl -w

# Copyright © 2008, 2009 Nicole Cloonan, Qinying Xu, Geoffrey Faulkne,
# and Sean Grimmond.

#This program is free software: you can redistribute it and/or modify it under the terms of the GNU
#General Public License as published by the Free Software Foundation, either version 3 of the
#License, or (at your option) any later version.

#This package is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
#without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
#PURPOSE. See the GNU General Public License for more details.

#You should have received a copy of the GNU General Public License along with this program. If
#not, see <http://www.gnu.org/licenses/>.


use Getopt::Std;
use Cwd;
use strict;
use QCMG::RNA_Mate::RNA_mapping;
use QCMG::RNA_Mate::tag_quality;
use QCMG::RNA_Mate::new_rescue;
use QCMG::RNA_Mate::wiggle_plot;
use QCMG::RNA_Mate::UCSC_junction;
use QCMG::RNA_Mate::tools_RNA;
use QCMG::RNA_Mate::single_select;

# Initalization: read config file and creat log file
# create an instance of tools_RNA module in subroutine init; 
my $toolobj;
my %argv = &init();

$toolobj->Log_PROCESS( "Welcome to our mapping strategy system!\n");

if($argv{'quality_check'} eq "false"){
        #create a csfasta file, in which the tag'sequence is the original one
        #the output will be  $argv->{'output_dir'}."$argv->{'exp_name'}.mers$l.unique.csfasta";
        my $fin = $argv{'raw_csfasta'};
	$toolobj->create_csfasta($fin);
	if(  $toolobj->check_died < 0){$toolobj->Log_("[DIED]: the whole system!\n") }
}
else{
	#check tags quality and if not qualified and then chop off last few mers
	my $obj_quality = tag_quality->new(%argv);
	if($obj_quality->main < 0){$toolobj->Log_("[DIED]: the whole system!\n") }
}

my @parameters = split(/\,/,$argv{'mapping_parameters'});

#map all tag to genomic and junction
my $l_last = $argv{raw_tag_length};
foreach my $elements (@parameters){
	my @map_para = split(/\./,$elements);
	my $l = $map_para[0];

	#create an instance of mapping module
	$argv{'junction'} = $argv{"junction_$l"};
	my $str = "junction_$l". "_index";
	$argv{'junction_index'} = $argv{"$str"};
	my $mapobj = RNA_mapping->new(\%argv);
	#--chop off last few mers from tag sequence
	my $l_chop = $l_last - $l;
	if($l_chop > 0){
		my $f_nonmatched = $argv{output_dir}.$argv{exp_name}.".mers".$l_last.".junction.non_matched";
		my $f_shorttag = $argv{output_dir}.$argv{exp_name}.".mers".$l.".unique.csfasta"; 
		$toolobj->chop_tag($f_nonmatched, $f_shorttag, $l_chop);	
	}

	if($mapobj->genomic_mapping(\@map_para) < 0){ $toolobj->Log_DIED("the whole system!\n") }
	if($mapobj->genome_collation(\@map_para) < 0 ){ $toolobj->Log_DIED("the whole system!\n") }
	if($mapobj->junction_mapping(\@map_para) < 0 ){ $toolobj->Log_DIED("the whole system!\n") }
	if($mapobj->junction_selection(\@map_para) < 0){ $toolobj->Log_DIED("the whole system!\n") }

	$l_last = $l;
}


if($argv{'run_rescue'} eq "false" ){
	#select single mapping position and prepare for wiggle
	my $singleobj = single_select->new(\%argv);
	if($singleobj->main < 0 ){ $toolobj->Log_DIED("the whole system!\n") }
}
else{
	# do rescue for genome mapping and prepare for wiggle
	my $rescueobj = new_rescue->new(\%argv);
	if( $rescueobj->pre_rescue() < 0){ $toolobj->Log_DIED("the whole system,during pre_rescue!\n") }
	if( $rescueobj->run_rescue() < 0){ $toolobj->Log_DIED("the whole system,during rescue!\n") }
	if( $rescueobj->pre_wig < 0){ $toolobj->Log_DIED("the whole system,during pre_wig!") }
	
}	
# wiggle plot for genome mapping
my $wigobj = wiggle_plot->new(\%argv);
if($wigobj->paralle_wig_fork < 0){ $toolobj->Log_DIED("the whole system,during wig plot!") }
if($wigobj->start_plot_fork < 0){ $toolobj->Log_DIED("the whole system,during start plot!") }
if($wigobj->collect_data < 0){ $toolobj->Log_DIED("the whole system, during collect wig data!") }

#create BED file for junction mapping
my $juncobj = UCSC_junction->new(\%argv);
if( $juncobj->main < 0){ $toolobj->Log_DIED("the whole system,during UCSC junction data collection!") }  
$toolobj->Log_SUCCESS("all done! enjoy the data!\n");
exit;

sub usage{
        print "\tOPTIONAL:\n";
        print "\t-c <file.conf> configure file for experiment\n\n";
        die "\n\n";
}

sub init{

	#get command line options
	my %opt = ();
	getopt("c",\%opt);

	#read configure file
	my %argv = ();
	my $conf;
        if(exists $opt{c} ){  $conf = $opt{c} } 
        elsif(keys(%opt) == 0 ){ $conf =  "default.conf"  }
        else{ &usage }
	$argv{'config'} = $conf;
	open(CONF, $conf) or die("can't open $conf.\n");
	while(my $line = <CONF>){
	  chomp($line);
	  my ($key,$value) = split(/=/,$line);
	  $argv{$key} = $value;
	}
	close(CONF);

	#creat an log file for this experiment;
	$toolobj = tools_RNA->new($conf);
	my $f_log = $argv{'output_dir'} . "$argv{'exp_name'}.log";
	open(LOG,">$f_log" );
	close(LOG);

	#verify all parameters in this configure file
	if((my $str = &verify_config(\%argv)) ne "true" ){ $toolobj->Log_DIED( "Error in configure file: $str" )}

	#create arguments tag_length
	my $tag_length = $argv{"mapping_parameters"};
	$tag_length =~ s/\.\d+//g;
	$argv{'tag_length'} = $tag_length;
 
	return %argv;
}

sub verify_config{
	my $argv = shift;
	if(length($argv->{'mask'}) != $argv->{'raw_tag_length'} ){return "the mask length is not equal to raw tag length!\n"}
	if($argv->{'mask'} =~ /[2-9]|\D/){ return "each base value must be 1 or 0 without space!\n"}
	if(($argv->{'max_multimatch'} =~ /\D/) || ($argv->{'max_multimatch'} < 1) || ($argv->{'max_multimatch'} > 100) ){return "the max_multimatch value is byond [1,100]\n"}
	if(($argv->{'expect_strand'} ne "-") && ($argv->{'expect_strand'} ne "+")){ return "the expect_strand value must be '+' or '-' \n"  }
	if(($argv->{'quality_check'} ne "true") && ($argv->{'quality_check'} ne "false")){return "the quality_check value must be 'true' or 'false'\n"}
	if($argv->{'run_rescue'} eq "true"){ 
		if( !($argv->{'rescue_window'} =~ /\d+/) ){ return "the rescue_window value must be a digital number\n" } 
		elsif( !($argv->{'num_parallel_rescue'} =~ /\d+/) ){return "the num_parallel_rescue value must be a digital number"}
		elsif(!(-e $argv->{'rescue'})){return "can't find rescue scripts!\n"}
	}
	elsif($argv->{'run_rescue'} ne "false"){return "the run_rescue value must be 'true' or 'false'\n"}

	if(!(-e $argv->{'output_dir'})){ return "the output directory is not exist\n" }


	#check mapping parameters
	my @chrs = split(/\,/,$argv->{'chromosomes'});
	foreach my $c (@chrs){
		my $f = "$argv->{'chr_path'}$c.fa";
		if(!(-e $f)){return "can't find genome file $f\n" }

	}
	my $tag_lengths = $argv->{'mapping_parameters'};
	$tag_lengths =~ s/\.\d+//g;
	my @l_map = split(/\,/,$tag_lengths); 
	my %hash = ();
	foreach my $l (@l_map){ 
		$hash{$l} ++;
		if($hash{$l} > 1){return "tag with same length only can be map one, check the value of mapping_parameters!\n"}
		if(!(-e $argv->{"junction_$l"})){ return "we can't find junciton library for junction_$l\n"  }		
	 }

	#check whether all listed scripts are exist or not
	if(!(-e $argv->{'script_chr_wig'})){return "can't find the script: $argv->{'script_chr_wig'}\n"}
	if(!(-e $argv->{'script_chr_start'})){return "can't find the script: $argv->{'script_chr_start'}\n"}
	if(!(-e $argv->{'f2m'})){return "can't find the script: $argv->{'f2m'}\n"}
	if(!(-e $argv->{'mapreads'})){return "can't find the script: $argv->{'mapreads'}\n"}
	
	return "true";
} 


