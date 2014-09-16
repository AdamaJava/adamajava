package QCMG::RNA_Mate::new_rescue;

use strict;
use Parallel::ForkManager;
use QCMG::RNA_Mate::tools_RNA;

sub new{
         my ($class, $arg)= @_;
         my $self = {
	 config => $arg->{'config'},
	 rescue => $arg->{"rescue"},
	 tag_length => $arg->{"tag_length"},
	 chr_names =>  $arg->{"chromosomes"},
#	 mismatch => $arg->{"num_mismatch"},
	 outdir => $arg->{"output_dir"},
	 exp_name => $arg->{"exp_name"},
	 max_hits => $arg->{"max_multimatch"},
	 window =>$arg->{"rescue_window"},
	 num_parallel_rescue => $arg->{'num_parallel_rescue'},
	 #parallel_tags =>$arg->{'rescue_paralle_tags'}          
 	};                              
	#get the maximum mismatch value from differnt tag length mapping
	my @maps = split(/\,/,$arg->{'mapping_parameters'});
	my $max_mis = 0;
	foreach my $m (@maps){	
		my ($l,$mis, $a) = split(/\./,$m);	
		if($mis > $max_mis){ $max_mis = $mis }
	}
	$self->{'mismatch'} = $max_mis;
	
        bless $self, $class;	
        return $self;  
}
my $self = {};
my $toolobj;

sub pre_rescue{
	$self = shift;
	$toolobj = tools_RNA->new($self->{'config'});	
	my @tag_length = split(/,/,$self->{'tag_length'});

	# create a file which will be used as input for rescue script
	my $f = $self->{outdir}."$self->{exp_name}.genomic.for_rescue";
	open(RESCUE, ">$f") or $toolobj->Log_DIED("can't open $f on pre_rescue\n " );
	close(RESCUE);

	# print a processing information into log file
        $toolobj->Log_PROCESS(" collect data from different tag length into one file for rescue\n ");

	# count tag's mismatch data and add mapping data into the ...for_rescue file
	foreach my $l (@tag_length){  &stats_preRescue($l)}
	
	#check log file
	my $rc = $toolobj->check_died();
	if($rc < 0){ return -1 }
	
	return 1;
}

sub run_rescue{
	$self = shift;
	 $toolobj = tools_RNA->new($self->{'config'});

	 #num_parallel_rescue=>$argv{'num_parallel_rescue'},
	my $pm = new Parallel::ForkManager( $self->{'num_parallel_rescue'} );	
	my @chromosomes = split(/,/,$self->{'chr_names'});
	
       	$toolobj->Log_PROCESS(" rescue multi mapped tags \n ");

	#parallel to run new version rescue
	foreach my $chr (@chromosomes){
		$pm->start and next;
	
		#run rescue
		my $input = $self->{outdir} . "$self->{exp_name}.genomic.for_rescue";
        	my $output = $self->{outdir} . "$chr.$self->{exp_name}.genomic.for_rescue.weighted";
	        my $comm = "python -i $self->{rescue} $input $output $self->{window} $chr ";
	        my $rc = system($comm);
	        if($rc != 0 ){ $toolobj->Log_DIED(" error during rescue\n")}

		$pm->finish;

	}
	#wait all rescue job to be done
	$pm->wait_all_children;

	#check log file
	if( $toolobj->check_died() < 0){ return -1 }       	
	$toolobj->Log_SUCCESS(" rescue tags are done! \n ");
	return 1;
}


sub pre_wig{
	
	$self = shift;
	$toolobj = tools_RNA->new($self->{'config'});

	my $pm = new Parallel::ForkManager( $self->{'num_parallel_rescue'} );	
	my @chromosomes = split(/,/,$self->{'chr_names'});
       	$toolobj->Log_PROCESS(" prepare data for wiggle plot... \n ");

	#parallel to prepare pre wig files
	foreach my $chr (@chromosomes){
        	$pm->start and next;
	        my $f = $self->{outdir} . "$chr.$self->{'exp_name'}.genomic.for_rescue.weighted";
        	open(IN, $f) or $toolobj->Log_DIED(" can't open file $f\n");
	        $f = $self->{outdir} . "$chr.$self->{'exp_name'}.for_wig.positive";
        	open(POS,">$f") or $toolobj->Log_DIED(" can't open file $f\n");
	        $f = $self->{outdir}."$chr.$self->{'exp_name'}.for_wig.negative";
        	open(NEG,">$f") or $toolobj->Log_DIED(" can't open file $f\n");
	        while(<IN>){
                	chomp();
        	        my ($id,$freq,$chr_rescue,$start,$end,$strand,$flag,$weight,$score) = split(/\t/);
	                if($chr eq $chr_rescue){
                        	if( ($strand eq "+") && ($score > 0) ){print POS "$start\t$end\t$score\n"}
                	        elsif( ($strand eq "-") && ($score > 0) ){print NEG "$start\t$end\t$score\n"}
        	        }
	        }
        	close(IN);
	        close(NEG);
	        close(POS);
        	$pm->finish;
	}

	#wait all pre_wig files are created
	$pm->wait_all_children;

	#check log file
	my $rc = $toolobj->check_died();
	if($rc < 0){ return -1 }
	$toolobj->Log_SUCCESS(" prepared data file for parallel wig plot.\n");
	return 1;
}
1;

sub stats_preRescue{
	my $mers = shift;

	my $f = $self->{outdir}."$self->{exp_name}.mers$mers.genomic.collated";

	#open input and output files
	open(COLLATED, $f) or $toolobj->Log_DIED("can't open $f in sub stats_preRescue");
	$f = $self->{outdir}."$self->{exp_name}.mers$mers.genomic.stats";
        open(STATS,"> $f") or $toolobj->Log_DIED("can't open $f in sub stats_preRescue");
	$f = $self->{outdir}."$self->{exp_name}.genomic.for_rescue";
	open(RESCUE,">>$f");
	
	# read all lines from input file
	while(my $line = <COLLATED>){
	  #jump over the tag sequence line
	  if($line !~ /^>/){next}
	
	  chomp($line);
          my @matches = split(/\t/,$line); 
	  my $tagid = shift(@matches);
          my $total = shift(@matches);

	  # throw this non mapped or over mapped tag
	  if($total == 0 ){print STATS "$tagid\t$total\n"; next}
	  if($total > $self->{'max_hits'}){print STATS "$tagid\t$total\n"; next}
#	  my ($beadid, $freq) = split(/:/,$tagid);

	  # initialize this tag's mismatch 
	  my %mis = ();
          for(my $i =0; $i <= $self->{'mismatch'};$i ++){$mis{$i} = 0}
	  # count this tag matched times at each mismatch value
	  foreach my $p (@matches){
		my ($chr,$position,$mismatch) = split(/\./,$p);
		$mis{$mismatch}++;
	  }  
	  my $s_mismatch = -1; 
	  for(my $i = 0; $i <= $self->{'mismatch'}; $i ++){
		# check the matched times at higher mismatch value, 
		#if there is no matched postion at current mismatch value.
		if($mis{$i} == 0){next}
		#if there are matched positions at current mismatch value, these positions will be selected
		#we throw all other matched postion at higher mismatch value.
		else{ $s_mismatch = $i; last}
	  }
	  if($s_mismatch == -1){print "error on <COLLATED> ($line) \n";next}

	  #the selected position must have same mismatch value
	  #if more than two position with lowest mismatch value makes as MuM
	  my $flag;
	  if($mis{$s_mismatch} == 1){$flag = "SiM"}
	  else{$flag = "MuM"}

	  
	  #collect all position with lowest mismatch value; each tag are treated unique, so that the frequency is 1
	  my $freq = 1;
	  foreach my $m (@matches){
		my ($chr,$position,$mismatch) = split(/\./,$m);
		if($mismatch != $s_mismatch){ next }
		if($position < 0){
                   my $end = abs($position );
		   my $start = $end - $mers + 1;
		   print RESCUE "$tagid\t$freq\t$chr\t$start\t$end\t-\t$flag\n";
		}
		else{	
		   my $start = $position;
		   my $end = $start + $mers - 1;
		   print RESCUE "$tagid\t$freq\t$chr\t$start\t$end\t+\t$flag\n";
		   
		}
	  }
		
	  #collect all mapping postion's mismatch value
	  print STATS "$tagid\t$total";
	  for(my $i = 0; $i <= $self->{'mismatch'};$i ++){print STATS "\t$mis{$i}"}
	  print STATS "\n";
	
	}
	close(RESCUE);
	close(COLLATED);
        close(STATS);


}
