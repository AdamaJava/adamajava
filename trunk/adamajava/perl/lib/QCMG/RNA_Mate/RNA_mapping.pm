package QCMG::RNA_Mate::RNA_mapping;
use strict;
use tools_RNA;

sub new{
        
	# get all experiment parameter 

	my ($class, $arg)= @_;
        my $self = {
	 config => $arg->{'config'},
	 chr_names =>  $arg->{"chromosomes"},
	 chr_path => $arg->{'chr_path'},
	 f2m => $arg->{"f2m"},
	 mapreads => $arg->{'mapreads'},
	 chrdir => $arg->{"chr_path"},
	 mask =>  $arg->{"mask"},
	 outdir => $arg->{"output_dir"},
	 exp_name => $arg->{"exp_name"},
	 exp_strand => $arg->{"expect_strand"},
	 max_hits => $arg->{"max_multimatch"},
	 junction => $arg->{"junction"}, 
	 junc_index => $arg->{"junction_index"}
 	};
	
	# get junction name, eg.hg18_junctions_best_quality 
	my $str =  $self->{junction};
	$str = substr($str,rindex($str,'/')+1);
	($self->{junc_name},$str)= split(/\./,$str);
	

        bless $self, $class;	
        return $self;   
}
my $toolobj;
sub genomic_mapping{

	# to map all genomic files
	my ($self,$map_para) = @_;
	$toolobj = tools_RNA->new($self->{'config'});
	
	#check the log file before run mapping
	if( $toolobj->check_died() < 0 ){ return -1 }
	$toolobj->Log_PROCESS("mapping to all chromosomes\n ");
	my $l = $map_para->[0];
	my $tag_file_name = "$self->{'exp_name'}.mers$l.unique.csfasta";
	my @chromosomes = split(/,/,$self->{chr_names});	
	my %q_file = ();
	foreach my $chr (@chromosomes){
		my $f_genome = "$self->{'chr_path'}$chr.fa";
		my %hash = &mapping($self,$tag_file_name,$map_para,$f_genome);
		%q_file = (%q_file,%hash);
	}

  	## wait and check the qsub job
	$toolobj->wait_for_queue(\%q_file);

	#check the log file again, it make sure whether all mapping is successful
        if( $toolobj->check_died < 0 ){ return -1 }
	$toolobj->Log_SUCCESS("mapped to all chromosomes\n ");
	return 1;
}


sub junction_mapping{
	my ($self,$map_para) = @_;
	
        #check the log file before run mapping
	$toolobj = tools_RNA->new($self->{'config'});
        if( $toolobj->check_died() < 0 ){ return -1 }
        $toolobj->Log_PROCESS("mapping to junciton (mers $map_para->[0].$map_para->[1].$map_para->[2])\n");

	#junction mapping	 
	my $tag_file_name = "$self->{exp_name}.mers$map_para->[0].genomic.non_matched";
	my %q_file = &mapping($self, $tag_file_name, $map_para,$self->{'junction'});
	$toolobj->wait_for_queue(\%q_file);

        if( $toolobj->check_died() < 0 ){ return -1 }
	$toolobj->Log_SUCCESS("mapped to junction (mers $map_para->[0].$map_para->[1].$map_para->[2])\n");
	return 1;
}

sub junction_selection{
	#input file, eg. hg18_junctions_best_quality.Solid0039_20080724_1_hES_hES_F3.mers25.genomic.non_matched.ma.25.3
	# there we only filter out the non_matched tags into output file
	my ($self,$map_para) = @_;

	my $tag_length = $map_para->[0];
	$toolobj = tools_RNA->new($self->{'config'});
	$toolobj->Log_PROCESS("single selecting junction position & ID! (mers $tag_length)\n");
	
	# open the input and output file
	my $mismatch = $map_para->[1];
	my $str = substr($self->{'junction'},rindex($self->{'junction'},'/')+1);
	my ($junc_name,$str1) = split(/\./,$str);
	my $f = $self->{outdir}."$junc_name.$self->{exp_name}.mers$tag_length.genomic.non_matched.ma.$tag_length.$mismatch";
	if($map_para->[2] == 1){$f .= ".adj_valid"}
	elsif($map_para->[2] == 2){ $f .= ".adj_all"}
	open(MA, $f) or $toolobj->Log_DIED("can't open file: $f \n");
	$f = $self->{outdir}."$self->{exp_name}.mers$tag_length.junction.non_matched";
	open(NON_MATCHED,">$f") or $toolobj->Log_DIED("can't open file: $f\n");
	$f = "$self->{outdir}$self->{exp_name}.junction$tag_length.SIM.positive";
	open(POS,">$f") or $toolobj->Log_DIED("can't open file: $f\n");
	$f = "$self->{outdir}$self->{exp_name}.junction$tag_length.SIM.negative";
	open(NEG,">$f") or $toolobj->Log_DIED("can't open file: $f\n");
        $f = $self->{'outdir'} . "$self->{'exp_name'}.junction$tag_length.positive.stats";
        open(my $POS_STAT, ">$f") or $toolobj->Log_DIED("can't open file: $f\n");
        $f = $self->{'outdir'} . "$self->{'exp_name'}.junction$tag_length.negative.stats";
        open(my $NEG_STAT, ">$f") or $toolobj->Log_DIED("can't open file: $f\n");


	# when the tag is not matched, then we put this tag into non_mathed file
	while(my $line = <MA>){
		if($line !~ /^>/){ next }

		chomp($line);
		my @matches = split(/\,/,$line);
                my $tagid = shift(@matches);
		my $i = @matches;
		my $sequ = <MA>;
                chomp($sequ);
		if($i == 0){ print NON_MATCHED "$tagid\n$sequ\n" }
		#single selection here
		else{   
	              	my ($strand,$mismatch)  = &stat_select($tagid,\@matches,$map_para,$self->{'exp_strand'},$POS_STAT,$NEG_STAT);
			#if we can't select the single position, we throw this tag into non_matched file
	       	        if(($strand ne "+") && ($strand ne "-")){ print NON_MATCHED "$tagid\n$sequ\n"; next }

		      	# get the selected matched position based on mismatch and strand value
        		my ($start, $end) = &mapping_position($tagid,\@matches,$strand,$mismatch,$tag_length);
	                if(($start < 0) || ($end < 0) ){$toolobj->Log_DIED("Algorithm error (start:$start;end:$end) in juction_single_selection RNA_mapping.pm\n"); exit}
       		        #print the selected position into file;
               		if($strand eq "+"){ print POS "$tagid\t$start\t$end\t$mismatch\n"}
                	elsif($strand eq "-"){print NEG "$tagid\t$start\t$end\t$mismatch\n"}
        	        else{$toolobj->Log_DIED("Algorithm error (strand is $strand) in single_selection UCSC_junction.pm")}
		}
	}
	
	# close both input and output
	close(POS);
	close(NEG);
	close($POS_STAT);
	close($NEG_STAT);
	close(NON_MATCHED);
	close(MA);

	#search junction ID 
	$f = "$self->{outdir}$self->{exp_name}.junction$tag_length.SIM.positive";
        &search_junctionID($self->{'junc_index'},$f);
	$f = "$self->{outdir}$self->{exp_name}.junction$tag_length.SIM.negative";
        &search_junctionID($self->{'junc_index'},$f);


	#check the Log file
        if( $toolobj->check_died() < 0 ){ return -1 }

	$toolobj->Log_SUCCESS("single selected junction position & ID (mers $tag_length)\n");

	return 1;
}

sub genome_collation{
	my ($self,$map_para) = @_;
	my $tag_length = $map_para->[0];
	my $mismatch = $map_para->[1];

	$toolobj = tools_RNA->new($self->{'config'});
	$toolobj->Log_PROCESS("collating genome mers:$tag_length\n");

	my $f = $self->{outdir} . "$self->{exp_name}.mers$tag_length.genomic.collated";
	open(COLLATED,">$f") or $toolobj->Log_DIED("can't open file: $f\n") ; 
        $f =$self->{outdir}."$self->{exp_name}.mers$tag_length.genomic.non_matched";
	open(NON_MATCHED,">$f");

	# open all genomic mapped file and put its file handle into an hash table
        my @chromosomes = split(/,/,$self->{chr_names});
        my %fh = ();
        foreach my $chr (@chromosomes){
	    my $f = "$self->{outdir}$chr.$self->{exp_name}.mers$tag_length.unique.csfasta.ma.$tag_length.$mismatch";
	    if($map_para->[2] == 1){ $f .= ".adj_valid"}
	    elsif($map_para->[2] == 2){$f .=".adj_all"}
	   
	    open(my $handle,$f) or $toolobj->Log_DIED("Can't open $f during genomic collating\n");
            $fh{$chr} = $handle; 	
	} 	

        #read a tag from first genome maped file
	my $first_chr = shift(@chromosomes);
        my $first_f = $fh{$first_chr};
        while(my $l = <$first_f>){
          if($l !~ m/^>/){next}
          chomp($l);
	  my @matches = split(/\,/,$l);  
          my $tagid = shift(@matches);
          foreach my $m (@matches){$m = "$first_chr.$m"}

	  #read a tagid mapping position from each of the rest mapped files (eg.chr1.exp_name.ma.35.3)
	  #combine all mapping position from different genome files into one line and write it into the . collated file
          foreach my $chr (@chromosomes ){
            my $tmp_f = $fh{$chr};
            while(my $line = <$tmp_f>){
		if($line =~ /^>/){
		    chomp($line);
		    my @tmp_match = split(/\,/,$line);
 		    shift(@tmp_match);
		    foreach  my $m (@tmp_match){$m = "$chr.$m"}
		    @matches = (@matches, @tmp_match);
		    last;
		}
            }
          }         

	 # count the curent tag's total matching position in whole genome
	 my $total =  @matches;
	 #we didn't chomp the sequ, so we needn't add "\n" for print the sequ into output file 
         my $sequ = <$first_f>;
		
	 #if this tag didn't match, it will be add into non_matched file
	 # exit current loop step, go to next step
         if($total == 0){ print NON_MATCHED "$tagid\n$sequ";	next	}

	 # when the tag is over matched, throw all matched position
         if($total > $self->{'max_hits'} ) {@matches = ()} 
         	
	 #write this matched tag into collated file
	 print COLLATED "$tagid\t$total\t";
	 foreach my $m (@matches){print COLLATED "$m\t"}
         print COLLATED "\n$sequ";

        }
	
	#closed all files 
	close($fh{$first_chr});
        foreach my $chr (@chromosomes){ close($fh{$chr}); } 
        close(COLLATED);
        close(NON_MATCHED);

	#check log file	
        my $rc = $toolobj->check_died();
        if($rc < 0){ return -1 }

	$toolobj->Log_SUCCESS("collated genome mers:$tag_length\n");
	return 1;
}

1;

sub search_junctionID{
        my ($f_index,$input) = @_;

        # read all junction id into array
        open(INDEX,$f_index) or $toolobj->Log_DIED("can't open junction index file: $f_index\n");
        my @all_junc_id = <INDEX>;
        close(INDEX);

        my $ff = $input . "ID";
        open(JUNC_ID,">$ff");
        #sort input file by matched position 
        system("sort -n -k2 $input > $input.sorted");
        open(SORTED,"$input.sorted");
        my $i_search = 0;
        my $i_end = @all_junc_id;
        while(my $line = <SORTED>){
                chomp($line);
                my ($tagid,$tag_start,$tag_end,$mismatch) = split(/\t/,$line);
                #if we can't find junction id for this matched position, 
                #this tag will be assign "...\tnoid\t0\t0\n"
                my $juncid = "noid";
                my $j_start=0;
                my $j_end = 0;
		#old index  for(my $i = $i_search; $i < $i_end; $i += 2){
		#new index
                for(my $i = $i_search; $i < $i_end; $i ++){
                        chomp($all_junc_id[$i]);
                        my($id,$junc_start,$junc_end) = split(/\t/,$all_junc_id[$i]);
                        if(($tag_start >= $junc_start) &&($tag_end <= $junc_end)){
                                $juncid = $id;
                                $j_start = $junc_start;
                                $j_end = $junc_end;
                                #stay at the current junction id for next tag on the sorted file
                                $i_search = $i -1;
                                #go to next tag on the sorted file
                                last;
                        }
                }
                #if we can't find junction id for this tag, the $i_search will start from last found junction id position
                print JUNC_ID "$tagid\t$tag_start\t$tag_end\t$mismatch\t$juncid\t$j_start\t$j_end\n";
        }
        close(POS);
        close(JUNC_ID);

}

sub mapping{
	my ($argv, $tag_file_name,$map_para, $f_genome) = @_;
       #check the log file before run mapping
        if( $toolobj->check_died() < 0 ){ return -1 }
	my $str = substr($f_genome,rindex($f_genome,'/')+1);
	my ($lib_name,$str1) = split(/\./,$str); 

        # check input file,if input file is zero size, we jump over the mapping process and create an zero size output file.
	my $tag_file = $argv->{'outdir'} . $tag_file_name;
        if( !(-e $tag_file) ){ $toolobj->Log_DIED("file: $tag_file, is not exist! \n"); return -1   }
	my $output = $argv->{outdir}. "$lib_name.$tag_file_name.ma.$map_para->[0].$map_para->[1]";
        if($map_para->[2] == 1) { $output .= ".adj_valid" }
        elsif($map_para->[2] == 2) {$output .= ".adj_all" }

        if( -z $tag_file ){
                $toolobj->Log_WARNING("no tag for genome mapping in $tag_file!\n");
                #create empty files
                open(IN, ">$output");
                close(IN);
                return 1;
        }

        #call mapread program
        my $mymask =  substr($argv->{mask},0,$map_para->[0]);
        my $comm = "$argv->{f2m} -program $argv->{mapreads}  -g  $f_genome ";
        $comm .= "-r $tag_file -d $argv->{outdir} -t $map_para->[0] ";
        $comm .= "-e $map_para->[1] -z $argv->{max_hits} -p $mymask -a $map_para->[2] ";
        my $mysh = "$output.sh";
        open(OUT,">$mysh") or $toolobj->Log_DIED("can't open file $mysh");
        print OUT $comm;
        close(OUT);

        my $success_file = "$output.success";

	#debug
	#$comm = "sh $mysh";
       	$comm = "qsub -l walltime=48:00:00 -o $mysh.out -e $mysh.err $mysh > $mysh.id ";

        my $rc = system($comm);
        my %hash = ($success_file => $rc);

        return %hash;
}




sub stat_select{
        my ($tagid,$matches,$map_para,$exp_strand,$POS_STAT,$NEG_STAT) = @_;

        #initialise this tag all mismatch value
        my $p_total = 0; #total positive position number
        my $n_total = 0; #total negative position number
        my %p_mis = ();
        my %n_mis = ();
        for(my $i = 0; $i <= $map_para->[1]; $i ++){$p_mis{$i} = 0; $n_mis{$i} = 0 }

        #count all mismatch value 
        foreach my $m (@$matches){
                my ($pla,$mis) = split(/\./,$m);
                if($pla < 0){$n_mis{$mis} ++; $n_total ++}
                else{$p_mis{$mis} ++; $p_total ++ }
        }

        #print stats positive mismatch into file
        print $POS_STAT "$tagid\t$p_total";
        for(my $i = 0; $i <= $map_para->[1]; $i ++){print $POS_STAT "\t$p_mis{$i}"}
        print $POS_STAT "\n";
        #print stats negative mismatch into file
        print $NEG_STAT "$tagid\t$n_total";
        for(my $i = 0; $i <= $map_para->[1]; $i ++){print $NEG_STAT "\t$n_mis{$i}"}
        print $NEG_STAT "\n";
        
	#get lowest mismatch value
        my $s_strand = "nothing"; #select single postion's direction
        my $s_mis = -1;          #select single position's mismatch value
        if($exp_strand eq "+"){
                for(my $i = 0; $i <= $map_para->[1]; $i ++){
                        if($p_mis{$i} == 0){next}
                        #select single mapped postion
                        if($p_mis{$i} == 1){$s_strand = "+";$s_mis = $i; last }
                        #throw this tag's all positive postion
                        if($p_mis{$i} > 1){last}
                 }
                 if($s_strand eq "nothing"){
                    for(my $i = 0; $i <= $map_para->[1]; $i ++){
                        if($n_mis{$i} == 0){next}
                        #select single mapped postion
                        if($n_mis{$i} == 1){$s_strand = "-";$s_mis = $i; last }
                        #throw this tag's all positive postion
                        if($n_mis{$i} > 1) {last};
                    }
                 }
        }
       elsif($exp_strand eq "-"){
                for(my $i = 0; $i <= $map_para->[1]; $i ++){
                        if($n_mis{$i} == 0){next}
                        #select single mapped postion
                        if($n_mis{$i} == 1){$s_strand = "-";$s_mis = $i; last }
                        #throw this tag's all positive postion
                        if($n_mis{$i} > 1) {last};
                }
                if($s_strand eq "nothing"){
                    for(my $i = 0; $i <= $map_para->[1]; $i ++){
                        if($p_mis{$i} == 0){next}
                        #select single mapped postion
                        if($p_mis{$i} == 1){$s_strand = "+";$s_mis = $i; last }
                        #throw this tag's all positive postion
                        if($p_mis{$i} > 1){last}
                   }
                }

        }
        else{ $toolobj->Log_DIED("exception on expected strand in stat_seletion UCSC_junction.pm ") }

        return ($s_strand,$s_mis);
}

sub mapping_position{

        my ($tagid,$matches,$strand,$mismatch,$mers) = @_;

        my $start = -1;
        my $end = -1;

        #get the single position, if this postion's mismatch value is the selected mismatched value which is count at subroutinue &stat_select
        #count this position's start and end value based on positived or negative strand
        foreach my $m (@$matches){
                my ($pla, $mis)= split(/\./,$m);
                if(($pla < 0) && ($strand eq "-") && ($mismatch == $mis)){
                        $end = abs($pla);
                        $start = $end - $mers + 1;
                }
                elsif(($pla >= 0) && ($strand eq "+") && ($mismatch == $mis)){
                        $start = $pla;
                        $end = $start + $mers - 1;
                }
        }

        return ($start, $end);
}



