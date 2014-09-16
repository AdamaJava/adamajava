#!/usr/bin/perl -w

eval 'exec /usr/bin/perl -w -S $0 ${1+"$@"}'
    if 0; # not running under some shell

# This is a master script which call all module step by step
# Written by Qinying Xu (Christina) who is a research assistant in Grimmond Group
# Last updated 20 Feb 2009

use Getopt::Std;
use Cwd;
use strict;
#use lib "/data/cxu/MiRNA/1Apr2010/lib";
use Parallel::ForkManager;
use QCMG::miRNA_Mate::tools_miRNA;
use QCMG::miRNA_Mate::split_BC;
use QCMG::miRNA_Mate::chop_adaptor;
use QCMG::miRNA_Mate::miRNA_mapping;
use QCMG::miRNA_Mate::region_picture;
use QCMG::miRNA_Mate::miRNA_freq;
use QCMG::miRNA_Mate::miRNA_summary;

# Initalization: read config file and creat log file
my %argv = &init();

# create an instance of tools_mapping module 
my $toolobj = QCMG::miRNA_Mate::tools_miRNA->new($argv{'config'});
$toolobj->Log_PROCESS( ": Welcome to our miRNA-mate system!\n");

my $f_F3 = $argv{'raw_F3'};
my $f_BC = $argv{'raw_BC'};
my @BCs = split(/\,/,$argv{'name_BC'});
#split tags based on the barcode -- BC file
#output will be BC*.out
if( scalar(@BCs) > 1 ){
	my $split_BC_obj = QCMG::miRNA_Mate::split_BC->new(\%argv);
	$split_BC_obj->main($f_F3,$f_BC);
}
else{
	my $f_BC = "$argv{'output_dir'}$argv{'exp_name'}.$BCs[0].out";
	system("cp $f_F3 $f_BC");
}

#parallel job for each barcode 
my $pm = new Parallel::ForkManager(6);

foreach my $BC (@BCs){
        $pm->start and next;

	#do recrusive mapping and count miRNA freqency here
	if($argv{'recursive_map'} eq "true"){
		#mapping collating and single selection
		$argv{'library'} = $argv{'recursive_library'};
		$argv{'library_index'} = $argv{'recursive_library_index'};
		my $map_obj = QCMG::miRNA_Mate::miRNA_mapping->new(\%argv);
		my $freq_obj = QCMG::miRNA_Mate::miRNA_freq->new(\%argv);
		$map_obj->recursive_mapping($BC);
		#count frequence here	
	        my $input = "$argv{'output_dir'}$argv{'exp_name'}.$BC.recursive.SIM.positive";
		my $output1 = "$input.sorted.ID";
		my $output2 = "$input.freq";
		$freq_obj->main($input,$output1,$output2);
        	$input = "$argv{'output_dir'}$argv{'exp_name'}.$BC.recursive.SIM.negative";
		$output1 = "$input.sorted.ID";
		$output2 = "$input.freq";
		$freq_obj->main($input,$output1,$output2);
	}

        $argv{'selected_adaptor'} = $argv{"Adaptor_$BC"};
	$argv{'library'} = $argv{'once_library'};
	$argv{'library_index'} = $argv{'once_library_index'};

        my $chop_adaptor_obj = QCMG::miRNA_Mate::chop_adaptor->new(\%argv);
	my $map_obj =QCMG::miRNA_Mate::miRNA_mapping->new(\%argv);
	my $freq_obj =QCMG::miRNA_Mate::miRNA_freq->new(\%argv);
	my $pic_obj = QCMG::miRNA_Mate::region_picture->new(\%argv);
	my $summary_obj = QCMG::miRNA_Mate::miRNA_summary->new(\%argv);
 
	#input: "$argv{'outdir'}$argv{'exp_name'}.BC$bc.out";
	#output:  "$argv{'outdir'}$argv{'exp_name'}.BC$bc.NT0, to NT29
	$chop_adaptor_obj->main("$BC");

	#map tag from NT17 to NT29 to library, then collate data and single selection
	$map_obj->once_mapping($BC);
	
	#only deal with expected strand tag
	my $strand;
	if($argv{'expect_strand'} eq '+' ){ $strand = "positive" }
	else{ $strand = 'negative' }
	my $input = "$argv{'output_dir'}$argv{'exp_name'}.$BC.once.SIM.$strand";
	#count maped miRNA ID frequency
	my $output1 = "$input.sorted.ID";
	my $output2 = "$input.freq";
	$freq_obj->main($input,$output1,$output2);
	#create $input.bc file
	$toolobj->editing($input,$argv{'library'},\%argv);
	
	#filter high miRNA frequency tags from .bc file and then prepare for seqlogo
	my $input1 = "$input.freq";
	my $input2 = "$input.bc";
	if($argv{'expect_strand'} eq '+'){ $pic_obj->main($input1,$input2) }
	
	#summary all mapped tag number of same length,start position,sequency with snp or not...
	$input1 = "$input.bc";
	$input2 = $output1;
	$summary_obj->main($input1, $input2);
	$toolobj->Log_SUCCESS( "Done -- tag with barcode $BC\n");

	$pm->finish;
}
$pm->wait_all_children;


$toolobj->Log_SUCCESS( "all done! enjoy the data!\n");
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
        else{ 
        	print "\tOPTIONAL:\n";
	        print "\t-c <file.conf> configure file for experiment\n\n";
	        die "\n\n";
	}

	$argv{'config'} = $conf;
	my $toolobj = QCMG::miRNA_Mate::tools_miRNA->new($conf);

	open(CONF, $conf) or $toolobj->myLog("[DIED]: can't open $conf.\n");
	while(my $line = <CONF>){
	  chomp($line);
	  my ($key,$value) = split(/=/,$line);
	  $argv{$key} = $value;
	}
	close(CONF);

	#creat an log file for this experiment;
	my $f_log = $argv{'output_dir'} . "$argv{'exp_name'}.log";
	open(LOG,">$f_log" );
	close(LOG);
 
	return %argv;
} 
