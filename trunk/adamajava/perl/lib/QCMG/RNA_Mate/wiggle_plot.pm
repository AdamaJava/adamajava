package QCMG::RNA_Mate::wiggle_plot;

use strict;
use Parallel::ForkManager;
use QCMG::RNA_Mate::tools_RNA;

sub new{
         my ($class, $arg)= @_;
         my $self = {
	 config => $arg->{'config'},
	 chr_names =>  $arg->{"chromosomes"},
	 rescue => $arg->{"rescue"},
	 mismatch => $arg->{"num_mismatch"},
	 outdir => $arg->{"output_dir"},
	 exp_name => $arg->{"exp_name"},
 	 l_max => $arg->{"max_length_tag"},
	 tag_length => $arg->{"tag_length"},
	 chr_wig => $arg->{'script_chr_wig'},
	 chr_start => $arg->{'script_chr_start'}, 
 	};                              

        bless $self, $class;	
        return $self;  
}
my $self = {}; #global varible
my $toolobj;
sub start_plot_fork{
	$self = shift;
	$toolobj = tools_RNA->new($self->{'config'});
	$toolobj->Log_PROCESS(" Creating start file for wiggle plot!\n");

	 my $pm = new Parallel::ForkManager(5);
	 my @chromosomes = split(/,/,$self->{'chr_names'});
	
	 #parallel to create start files
	 foreach my $chr (@chromosomes){
		$pm->start and next;
		
		my $input = $self->{'outdir'} . "$chr.$self->{'exp_name'}.for_wig.positive";
		my $output = $self->{'outdir'} . "$chr.$self->{'exp_name'}.for_wig.positive.starts";
		my $f_success = $self->{'outdir'} . "$chr.$self->{'exp_name'}.for_wig.positive.starts.success";
		my $comm = "$self->{'chr_start'} -i $input -o $output -s $f_success";
		my $rc = system($comm);
		if( $rc != 0 ){ $toolobj->Log_DIED(" $comm\n ") }
		
		#submit negative chr mapping to queue for wig
		$input = $self->{'outdir'} . "$chr.$self->{'exp_name'}.for_wig.negative";
		$output = $self->{'outdir'} . "$chr.$self->{'exp_name'}.for_wig.negative.starts";
		$f_success = $self->{'outdir'} . "$chr.$self->{'exp_name'}.for_wig.negative.starts.success";
		$comm = "$self->{'chr_start'} -i $input -o $output -s $f_success";
		$rc = system($comm);
		if( $rc != 0 ){ $toolobj->Log_DIED(" $comm\n ") }
		
		$pm->finish;
        }
	
	# wait all paralled job to be done
	$pm->wait_all_children;
	
	#open output files
	my $p_f = $self->{'outdir'} . "$self->{'exp_name'}.positive.starts";
	open(POSI, ">$p_f") or $toolobj->Log_DIED(" can't create file ($p_f)")  ;
	my $n_f = $self->{'outdir'} . "$self->{'exp_name'}.negative.starts";
	open(NEGA, ">$n_f") or $toolobj->Log_DIED(" can't create file ($n_f)")  ;

	#add head line on final wiggle file
	print POSI "track type=bedGraph name=\"[$self->{'exp_name'}] positive starts\" description=\"[$self->{'exp_name'}] positive strand hits\" visibility=full color=255,0,0 altColor=0,100,200 priority=20 \n";
	print NEGA "track type=bedGraph name=\"[$self->{'exp_name'}] negative starts\" description=\"[$self->{'exp_name'}] negative strand hits\" visibility=full color=0,0,255 altColor=0,100,200 priority=20 \n";


	#combine all single chromosome start files into one file
	foreach my $chr (@chromosomes){
         	my $f = $self->{'outdir'} . "$chr.$self->{'exp_name'}.for_wig.positive.starts";
		open(S1,"$f") or $toolobj->Log_DIED(" can't open file ($f)\n");
		while(my $line = <S1>){
			chomp($line);
			my ($start, $end,$score) = split(/\t/,$line);
			my $round = int($score + 0.5);
			if($round > 0){print POSI "$chr\t$start\t$end\t$round\n"}
		}
		close(S1);

          	$f = $self->{'outdir'} . "$chr.$self->{'exp_name'}.for_wig.negative.starts";
		open(S2,"$f")  or $toolobj->Log_DIED(" can't open file ($f)\n");
		while(my $line = <S2>){
			chomp($line);
			my ($start, $end,$score) = split(/\t/,$line);
			my $round = int($score + 0.5);
			if($round > 0){print NEGA "$chr\t$start\t$end\t$round\n"}
		}
		close(S2);

	}
	close(POSI);
	close(NEGA);
	
	#check log file
	my $rc = $toolobj->check_died();
	if($rc < 0){ return -1 }

	#add success information into log file
	$toolobj->Log_SUCCESS(" Created start file:\n $p_f \n $n_f\n ");
	return 1;	

}
sub paralle_wig_fork{
	 $self = shift;

	 $toolobj = tools_RNA->new($self->{'config'});
	 $toolobj->Log_PROCESS(" Creating files for wiggle plot!\n");

	 my $pm = new Parallel::ForkManager(5);
	 my @chromosomes = split(/,/,$self->{'chr_names'});
	
	 #paralledl to create each chromosome's wig file
	 foreach my $chr (@chromosomes){
		$pm->start and next;
		#single chromosome wig file on positive strand
		my $input = $self->{'outdir'} . "$chr.$self->{'exp_name'}.for_wig.positive";
		my $output = $self->{'outdir'} . "$chr.$self->{'exp_name'}.for_wig.positive.wig";
		my $f_success = $self->{'outdir'} . "$chr.$self->{'exp_name'}.for_wig.positive.wig.success";
		my $comm = "$self->{'chr_wig'} -i $input -o $output -s $f_success";
		my $rc = system($comm);
		if( $rc != 0 ){ $toolobj->Log_DIED(" $comm\n ") }
		
		#single chromosome's wig file on negative strand
		$input = $self->{'outdir'} . "$chr.$self->{'exp_name'}.for_wig.negative";
		$output = $self->{'outdir'} . "$chr.$self->{'exp_name'}.for_wig.negative.wig";
		$f_success = $self->{'outdir'} . "$chr.$self->{'exp_name'}.for_wig.negative.wig.success";
		$comm = "$self->{'chr_wig'} -i $input -o $output -s $f_success";
		$rc = system($comm);
		if( $rc != 0 ){ $toolobj->Log_DIED(" $comm\n ") }

		$pm->finish;
        }
        
	$pm->wait_all_children;

	#check log file
	my $rc = $toolobj->check_died();
	if($rc < 0){ return -1 }

	return 1;
}


sub collect_data{
	$self = shift;
	$toolobj = tools_RNA->new($self->{'config'});
	
	#combine all positive or negative wig data into one.
	#creat output file for both positive and negative wiggle plot data
	my $p_f = $self->{'outdir'} . "$self->{'exp_name'}.positive.wiggle";
	open(POSI, ">$p_f") or $toolobj->Log_DIED(" can't create file ($p_f)")  ;
	my $n_f = $self->{'outdir'} . "$self->{'exp_name'}.negative.wiggle";
	open(NEGA, ">$n_f") or $toolobj->Log_DIED(" can't create file ($n_f)")  ;

	#add head line on final wiggle file
	print POSI "track type=bedGraph name=\"[$self->{'exp_name'}] positive\" description=\"[$self->{'exp_name'}] positive strand hits\" visibility=full color=255,0,0 altColor=0,100,200 priority=20 \n";

	print NEGA "track type=bedGraph name=\"[$self->{'exp_name'}] negative\" description=\"[$self->{'exp_name'}] negative strand hits\" visibility=full color=0,0,255 altColor=0,100,200 priority=20 \n";

	#read each single chromosome wiggle data 
	#combine all positive wiggle data file into the final positive file
	#combine all negative wiggle data file into the final negative file
	my @chromosomes = split(/,/,$self->{'chr_names'});
	foreach my $chr (@chromosomes){
         	my $f = $self->{'outdir'} . "$chr.$self->{'exp_name'}.for_wig.positive.wig";
		open(WIG1,"$f") or $toolobj->Log_DIED(" can't open file ($f)\n");
		while(my $line = <WIG1>){
			chomp($line);
			#throw the wiggle position which score is 0
			#otherwise write it into final wiggle file
			my ($start,$end,$score) = split(/\t/,$line);
			my $round = int($score + 0.5);
			if($round > 0){	print POSI "$chr\t$start\t$end\t$round\n" }
		}
		close(WIG1);

          	$f = $self->{'outdir'} . "$chr.$self->{'exp_name'}.for_wig.negative.wig";
		open(WIG2,"$f")  or $toolobj->Log_DIED(" can't open file ($f)\n");
		while(my $line = <WIG2>){
			chomp($line);
			my ($start,$end,$score) = split(/\t/,$line);
			my $round = int($score + 0.5);
			if($round > 0){	print NEGA  "$chr\t$start\t$end\t$round\n" }
		}
		close(WIG2);

	}
	close(POSI);
	close(NEGA);

	#check log file
	my $rc = $toolobj->check_died();
	if($rc < 0){ return -1 }

	
	#add success information into log file
	$toolobj->Log_SUCCESS(" Created wiggle plot file:\n $p_f \n $n_f\n ");

	#compress data here
	
	return 1;
}

1;
