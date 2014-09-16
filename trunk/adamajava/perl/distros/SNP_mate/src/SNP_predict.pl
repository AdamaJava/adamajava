#! /usr/bin/perl -w

use strict;
use Getopt::Std;
use Cwd;
use lib "/data/cxu/SNPs/xu_SNPs/doc.scripts";

use Parallel::ForkManager;
use tag_quality_filter;
use mapping_filter;
use tools_SNPs;
use err_profile_filter;
use color_space_filter;
use proportion_filter;

#initilization: read config file and create lof files
my %argv = &init();

# create an instance of tools_mapping module 
my $toolobj = tools_SNPs->new($argv{'config'});
$toolobj->myLog( "[PORCESS]: Welcome to our SNPs prediction system!\n");

#if it is not required to run mapping, so we needn't to create new csfasta files
if( ($argv{'filter_quality_check'} eq "false") && ($argv{'filter_mapping'} eq "true" ) ){
        #create a csfasta file, in which the tag'sequence is the original one
        #the tag's id is bead id plus ":1"
        my $fin = $argv{'raw_csfasta'};
        my $fout= $argv{'output_dir'} . "$argv{'exp_name'}.mers$argv{'full_tag_length'}.unique.csfasta";
        #$toolobj->create_csfasta($fin,$fout,\%argv);
        #if(  $toolobj->check_died < 0){$toolobj->myLog("[DIED]: during tag quality check!\n") }
}
elsif( $argv{'filter_quality_check'} eq "true"  ){
        #check tags quality and if not qualified and then chop off last few mers
        my $obj_quality = tag_quality_filter->new(%argv);
        #if($obj_quality->main < 0){$toolobj->myLog("[DIED]: during tag quality check!\n") }
}


my $mapobj = mapping_filter->new(%argv);
if($argv{'filter_mapping'} eq "true" ){
	#map all tag to genomic and junction
#	if($mapobj->genomic_mapping( $argv{'full_tag_length'} ) < 0){ $toolobj->myLog("[DIED]: during genome mapping!\n") }
	if($mapobj->collate_genomic_matches($argv{'full_tag_length'}) < 0 ){ $toolobj->myLog("[DIED]: during collating genome mapped data!\n") }
}
#single one matched positon for each mapped tag
my $input = $argv{'output_dir'} . "$argv{'exp_name'}.mers$argv{'full_tag_length'}.genomic.collated ";
if($mapobj->single_select( $input ) < 0 ){ $toolobj->myLog("[DIED]: during single selection!\n") }

#parallel all job here
my @chromosomes = split(/,/, $argv{'chromosomes'} );
my $pm = new Parallel::ForkManager(4);
$toolobj->myLog("[PROCESS]: parallel SNP prediction for each chromosome.\n ");

foreach my $chr (@chromosomes ){
	$pm->start and next;
	my $toolobj = tools_SNPs->new($argv{'config'});
	
	#call editting.pl to create .bc file
	#the input file is the single selected postion with mismatch > 0
	my $f_in = "$argv{'output_dir'}$chr.$argv{'exp_name'}.mers$argv{'full_tag_length'}.single_selected.mismatched";
	my $f_gen = "$argv{'chr_path'}$chr.fa";
	#the output is "$f_in.bc"
	$toolobj->create_bc($f_in, $f_gen);

	$pm->finish;
}
$pm->wait_all_children;


#filter by error profit -- set a default mask from configure file
if($argv{'filter_err_profile'} eq "true" ){
	#it create a mask which assign 1 for the reliable base
	my $err_profile_obj = err_profile_filter->new(%argv);
	#input is the whole genome .bc file
	if( $err_profile_obj->create_mask( $argv{'mask'} ) < 0 ){ $toolobj->myLog("[DIED]: err_profile_filter::create_mask!\n") }
	else{ $toolobj->myLog("[SUCCESS]: created a new mask to filte the error base!\n$argv{'mask'}\n") }
}


foreach my $chr (@chromosomes ){
	$pm->start and next;
	my $toolobj = tools_SNPs->new($argv{'config'});
	
	#create valid adjacent output file
	my $color_space_obj = color_space_filter->new(%argv);
	my $in = "$argv{'output_dir'}$chr.$argv{'exp_name'}.mers$argv{'full_tag_length'}.single_selected.mismatched.bc";
	my $out = "$argv{'output_dir'}$chr.$argv{'exp_name'}.mers$argv{'full_tag_length'}.valid_adjacent";
	if( $color_space_obj->valid_adjacent($in, $out) < 0 ){ $toolobj->myLog("[DIED]: creating valid adjacent output ($chr) !\n") }
	else{$toolobj->myLog("[SUCCESS]:created the file: $out !\n ")}

	#collect snp information for indepedend or depedent tag
	$in = "$argv{'output_dir'}$chr.$argv{'exp_name'}.mers$argv{'full_tag_length'}.valid_adjacent";
	#output is $str_01($str_02).indepedent or $str_01($str_02).depedent;
	my $str_o1 = "$argv{'output_dir'}$chr.$argv{'exp_name'}.mers$argv{'full_tag_length'}.all_SNPS";
	my $str_o2 = "$argv{'output_dir'}$chr.$argv{'exp_name'}.mers$argv{'full_tag_length'}.SNP_position";
	if( $color_space_obj->potential_SNP($in, $str_o1,$str_o2) < 0 ){ $toolobj->myLog("[DIED]:finding SNP position  ($chr) !\n") }
	else{$toolobj->myLog("[SUCCESS]: find potential SNPs position for $chr!\n") }
	
	#filter by proportion
	#pass the default mask or new mask created by err_profit_filter
	my $proportion_obj = proportion_filter->new(%argv);
	my $in1 = "$argv{'output_dir'}$chr.$argv{'exp_name'}.mers$argv{'full_tag_length'}.single_selected.mismatched.bc";
	my $in2 = "$argv{'output_dir'}$chr.$argv{'exp_name'}.mers$argv{'full_tag_length'}.single_selected.no_mismatched";
	my $in3 = "$argv{'output_dir'}$chr.$argv{'exp_name'}.mers$argv{'full_tag_length'}.valid_adjacent";
	my $in4 = "$argv{'output_dir'}$chr.$argv{'exp_name'}.mers$argv{'full_tag_length'}.valid_adjacent.frequency";
	#the following two files name should add .indepedent for .depedent
	my $str_in5 = "$argv{'output_dir'}$chr.$argv{'exp_name'}.mers$argv{'full_tag_length'}.SNP_position";
	my $str_out = "$argv{'output_dir'}$chr.$argv{'exp_name'}.mers$argv{'full_tag_length'}.SNP_vs_noSNP";
	if($proportion_obj->main($in1,$in2,$in3,$in4,$str_in5,$str_out) < 0){$toolobj->myLog("[DIED]:during proportion_filter ($chr)!\n ")}
	else{$toolobj->myLog("[SUCCESS]:created output after proportion filter: $out\n")}	
	
	$pm->finish;
}
$pm->wait_all_children;

$toolobj->myLog("[SUCCESS]: all SNP prediction for whole genome.\n Please check whether it is succeed for each chromosome.\n ");




exit;

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
        my $toolobj = tools_SNPs->new($conf);

        open(CONF, $conf) or $toolobj->myLog("[DIED]: can't open $conf.\n");
        while(my $line = <CONF>){
          chomp($line);
          my ($key,$value) = split(/=/,$line);
          $argv{$key} = $value;
        }
        close(CONF);
	
        #creat an log file for this experiment;
        my $f_log = $argv{'output_dir'} . "$argv{'exp_name'}.SNP.log";
        open(LOG,">$f_log" );
        close(LOG);
 
        return %argv;
}

sub usage{
        print "\tOPTIONAL:\n";
        print "\t-c <file.conf> configure file for experiment\n\n";
        die "\n\n";
}

