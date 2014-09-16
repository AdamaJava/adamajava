package QCMG::miRNA_Mate::miRNA_mapping;
use strict;
use QCMG::miRNA_Mate::tools_miRNA;

sub new{
        
	# get all experiment parameter 

	my ($class, $arg)= @_;
        my $self = {
		config => $arg->{'config'},
		f2m => $arg->{"f2m"},
		mapreads => $arg->{'mapreads'},
		mask =>  $arg->{"mask"},
		outdir => $arg->{"output_dir"},
		exp_name => $arg->{"exp_name"},
		max_hits => $arg->{"max_multimatch"},
		exp_strand => $arg->{"expect_strand"},
		recursive_map_para =>$arg->{'recursive_map_para'},
		once_map_para =>$arg->{'once_map_para'},
		raw_tag_length => $arg->{'raw_tag_length'},
		library => $arg->{"library"}, 
 	};

        # get library name, eg.hg18_junctions_best_quality 
        my $str =  $self->{library};
        $str = substr($str,rindex($str,'/')+1);
        ($self->{lib_name},$str)= split(/\./,$str);
	
        bless $self, $class;	
        return $self;   
}
my $toolobj;
sub once_mapping{
	my ( $self, $BCi ) = @_;

        #map tag from NT17 to NT29 to library
        #for testing purpose we use forkmanager, later we will use queue system
	$toolobj = QCMG::miRNA_Mate::tools_miRNA->new( $self->{'config'} );
	$toolobj->Log_PROCESS("Mapping miRNA sequence to libray, each tag only map once -- $BCi\n");


	#get all mapping parameter and get the maxumum mismatch value from all mapping jobs
        my @maps_dot = split(/,/,$self->{'once_map_para'});
        my @maps = ();
        my $max_mis = 0;
        foreach my $m (@maps_dot){
                my ($l,$mis,$adj) = split(/\./,$m);
                push @maps, [$l,$mis,$adj];
                #get the maxsimum mismatch value from all mapping parameters
                if($mis > $max_mis){$max_mis = $mis }
        }

	my %q_file = ();
        my @single_map_length = split(/,/,$self->{'once_map_para'});
	#map to library and then wait it, if failed then exit this pipeline 
        foreach my $m (@maps){
                my $tag_file_name = "$self->{'exp_name'}.$BCi.NT$m->[0]";
		#after submit mapping job it return the success file name into a hash table
                my %q = &mapping($self, $tag_file_name, $m);
		%q_file = (%q_file, %q);
        }
	$toolobj->wait_for_queue(\%q_file);
	if($toolobj->check_died < 0){ return -1 }
	
	#To collate mapped tag into one file
        my $output = "$self->{'outdir'}$self->{'exp_name'}.$BCi.once.collated";
        open(my $h_output, ">$output") or $toolobj->Log_DIED("can't open output: $output!\n");
        foreach my $m (@maps){  
                my $bf_ma = "$self->{outdir}$self->{'lib_name'}.$self->{'exp_name'}.$BCi.NT$m->[0]";
		&collation($bf_ma,$m, $h_output,"once");
	}
        close($h_output);
	if($toolobj->check_died < 0){ return -1 }

        #single selection
        my $input = "$self->{'outdir'}$self->{'exp_name'}.$BCi.once.collated";
        my $output_pos = "$self->{'outdir'}$self->{'exp_name'}.$BCi.once.SIM.positive";
        my $output_neg = "$self->{'outdir'}$self->{'exp_name'}.$BCi.once.SIM.negative";
        &single_selection($self,$max_mis,$input, $output_pos, $output_neg);
	if($toolobj->check_died){ $toolobj->Log_SUCCESS("finished once mapping for $BCi. See output:\n$output_pos\n$output_neg\n")}
	
	#delete all .non_matched, .sh files

	return 1;

}
sub recursive_mapping{
	my ($self, $BCi) = @_;
	$toolobj = QCMG::miRNA_Mate::tools_miRNA->new($self->{'config'});
        $toolobj->Log_PROCESS("Runing recursive mapping -- $BCi\n");

	#sort tag length for recursive mapping
        my @maps_dot = split(/,/,$self->{'recursive_map_para'});
	my @maps_split = ();
	my $max_mis = 0;
	foreach my $m (@maps_dot){
		my ($l,$mis,$adj) = split(/\./,$m);
		push @maps_split, [$l,$mis,$adj];
		#get the maxsimum mismatch value from all mapping parameters
		if($mis > $max_mis){$max_mis = $mis }
	}
	my @maps = sort{$b->[0] <=> $a->[0]} @maps_split;
	
	my $l_last = $self->{'raw_tag_length'};
	#copy input file:
	my $f_BC = "$self->{'outdir'}$self->{'exp_name'}.$BCi.out";
	my $f_nonmatched = $f_BC;

	#create the collated file 
	my $f_collated = "$self->{'outdir'}$self->{'exp_name'}.$BCi.recursive.collated";
        open(my $h_collated, ">$f_collated") or $toolobj->Log_DIED("can't open output: $f_collated!\n");
	#recursive mapping and collated
	foreach my $m (@maps){
		if($m->[0] > $self->{'raw_tag_length'}){$toolobj->Log_DIED("the mapping tag length $m->[0] is great than raw tag length")}
		my $tag_file_name = "$self->{'exp_name'}.$BCi.mers$m->[0].csfasta";
		#create tag file when first mapping and for raw tag length
		if($m->[0] == $self->{'raw_tag_length'}){
			if(system("cp $f_BC $self->{'outdir'}$tag_file_name")!=0){ $toolobj->Log_DIED("cant cp $f_BC $self->{'outdir'}$tag_file_name")}
		}
	        #--chop off last few mers from tag sequence
        	else{
			my $l_chop = $l_last - $m->[0];
	        	if($l_chop > 0){
                		my $f_shorttag = $self->{'outdir'} . $tag_file_name;
	                	$toolobj->chop_tag($f_nonmatched, $f_shorttag, $l_chop);
        		}
		}
		#map to library and then wait it, if failed then exit this pipeline 
		my %q_file =  &mapping($self, $tag_file_name, $m);
		$toolobj->wait_for_queue(\%q_file);
		if($toolobj->check_died < 0){ return -1 }

		#collated mapped tags
		my $bf_ma = "$self->{'outdir'}$self->{'lib_name'}.$tag_file_name";
		&collation($bf_ma,$m,$h_collated,"recursive");
		if($toolobj->check_died < 0){ last }
		$f_nonmatched = "$bf_ma.non_matched";
         	$l_last = $m->[0];
	}	
        close($h_collated);
	if($toolobj->check_died){$toolobj->Log_SUCCESS("Created $f_collated\n")}

	#single selection

        $toolobj->Log_PROCESS("Single selecting mapped position for collated tags\n");
        my $output_pos = "$self->{'outdir'}$self->{'exp_name'}.$BCi.recursive.SIM.positive";
        my $output_neg = "$self->{'outdir'}$self->{'exp_name'}.$BCi.recursive.SIM.negative";
        &single_selection($self,$max_mis,$f_collated, $output_pos, $output_neg);
	if($toolobj->check_died){$toolobj->Log_SUCCESS("Finished recursive mapping for $BCi. See Output:\n$output_pos\n$output_neg\n")}

	#delete all .non_matched, .sh files

	return 1;

}
1;


sub collation{
        my ($bf_ma,$para,$h_output,$flag )= @_;

        # open the input file and create the non_matched file
	my $input = "$bf_ma.ma.$para->[0].$para->[1]";
	if($para->[2] == 1){ $input .= ".adj_valid" }
	elsif($para->[2] == 2){ $input .= ".adj_all" }
        
	open(MA, $input) or $toolobj->Log_DIED("can't open input: $input \n");
        my $f_nonmatch = "$bf_ma.non_matched";
        open(NON_MATCHED,">$f_nonmatch") or $toolobj->Log_DIED("can't open output: $f_nonmatch\n");
 
        #we put mapped tag into collated file and non mapped tag to non_matched file
	while(my $line = <MA>){
             if($line =~ /^>/){
                chomp($line);
                my @matches = split(/\,/,$line);
                my $tagid = shift(@matches);
                my $sequ = <MA>;
		my $n_match = scalar(@matches);
                if( $n_match == 0){ if($flag eq "recursive"){ print NON_MATCHED "$tagid\n$sequ" }	}
                else{

                        print $h_output "$tagid\t$n_match";
                        foreach my $m (@matches){       print $h_output "\t$m"}
                        print $h_output "\n$sequ";
                }
	
             }
        }
	
	
        # close both input and output
        close(MA);
	close(NON_MATCHED);

	if($flag eq "once"){unlink($f_nonmatch)}

}

sub mapping{
	# to map all genomic files
	my ($argv,$tag_file_name,$para) =@_;
	
	#check the log file before run mapping
	if( $toolobj->check_died() < 0 ){ return -1 }

	my $tag_file = $argv->{'outdir'}.$tag_file_name;
	if( !(-e $tag_file) ){ $toolobj->Log_DIED("file: $tag_file, is not exist! \n")   }
	# if input file is zero size, we jump over the mapping process and create an zero size output file.
#	if( -z $tag_file ){
#                $toolobj->Log_WARNING("no tag for genome mapping in $tag_file!\n");
                #create empty files
#                my $fname = $argv->{outdir}. "$argv->{lib_name}.$tag_file_name.ma.$para->[0].$para->[1]";
#                open(IN, ">$fname");
#                close(IN);
#                return 1;
#        }

	#start to match all genomic fiels
	my $mymask =  substr($argv->{mask},0,$para->[0]);	
	my $comm = "$argv->{f2m} -program $argv->{mapreads}  -g  $argv->{'library'} ";
	$comm .= "-r $tag_file -d $argv->{outdir} -t $para->[0] ";
	$comm .= "-e $para->[1] -z $argv->{max_hits} -p $mymask -a $para->[2] ";
	my $mysh = $argv->{outdir}. "$argv->{lib_name}.$tag_file_name.ma.$para->[0].$para->[1].sh";
	open(OUT,"> $mysh") or $toolobj->Log_DIED("can't open file $mysh");
	print OUT $comm." 1\>\&2>>$mysh.std";
	close(OUT);
	
	
	my $success_file = $argv->{'outdir'} . "$argv->{'lib_name'}.$tag_file_name.ma.$para->[0].$para->[1]";
	if($para->[2] == 1) { $success_file .= ".adj_valid.success" }
	elsif($para->[2] == 2) {$success_file .= ".adj_all.success" }
	else{$success_file .= ".success"}

#	$comm = "qsub -l walltime=48:00:00 -o $mysh.out -e $mysh.out $mysh > $mysh.id ";
#	my $rc = system($comm);
	my $rc = system($comm." 1\>\&2>>$mysh.std");

	my %hash = ($success_file => $rc);

	return %hash;
}

sub single_selection{
	my ($argv,$max_mis, $input,$output_pos, $output_neg) = @_;


        #open all input and output files        
        open(COLLATED, "$input") or $toolobj->Log_DIED("can't open input collated file: $input!\n");
        open(POS, ">$output_pos") or $toolobj->Log_DIED("can't open positive single selection output: $output_pos!\n");
        open(NEG, ">$output_neg") or $toolobj->Log_DIED("can't open neagtive single selection output: $output_neg!\n");

        # read all lines from the mapped file. eg. ...ma.35.3
        while(<COLLATED>){
                chomp();
		if(! m/^>/){next}
	        my @matches = split(/\t/);
                my $tagid = shift(@matches);

                #get single matched position's mismatch and strand value
                #if can't find single matched position on the lowest mismatch value, throw this tag
                my $selected  = &select($tagid,\@matches,$max_mis,$argv->{'exp_strand'});

		#throw the tag in which we can't select single position
                if($selected == -1){next}

                #print the selected position into file;
		my $sequ = <COLLATED>;
                if($selected >= 0){ print POS "$tagid,$selected\n$sequ" }
                elsif($selected < 0 ){ print NEG "$tagid,$selected\n$sequ" }
        }

        #close all files
        close(NEG);
        close(POS);
        close(COLLATED);
}


sub select{
        my ($tagid,$matches,$max_mismatch, $exp_strand) = @_;

	my $total_match = shift(@$matches);
	#return the position if there are only one mapped position
	if($total_match == 1){
		my ($pla, $mis) = split(/\./, $matches->[0]);
		if($pla < 0){return ("-",$matches->[0]) }
		else{ return ("+",$matches->[0])  }
	}

        #initialise this tag all mismatch value
        my $p_total = 0; #total positive position number
        my $n_total = 0; #total negative position number
        my %p_mis = ();
        my %n_mis = ();
        for(my $i = 0; $i <= $max_mismatch; $i ++){$p_mis{$i} = 0; $n_mis{$i} = 0 }

        #count all mismatch value;
        foreach my $m (@$matches){
                my ($pla,$mis) = split(/\./,$m);
                if($pla < 0){$n_mis{$mis} ++; $n_total ++}
                else{$p_mis{$mis} ++; $p_total ++ }
        }

        #get lowest mismatch value
        my $s_strand = "nothing"; #select single postion's direction
        my $s_mis = -1;          #select single position's mismatch value
        if($exp_strand eq "+"){
                for(my $i = 0; $i <= $max_mismatch; $i ++){
                        if($p_mis{$i} == 0){next}
                        #select single mapped postion
                        if($p_mis{$i} == 1){$s_strand = "+";$s_mis = $i; last }
                        #throw this tag's all positive postion
                        if($p_mis{$i} > 1){last}
                 }
                 if($s_strand eq "nothing"){
                    for(my $i = 0; $i <= $max_mismatch; $i ++){
                        if($n_mis{$i} == 0){next}
                        #select single mapped postion
                        if($n_mis{$i} == 1){$s_strand = "-";$s_mis = $i; last }
                        #throw this tag's all positive postion
                        if($n_mis{$i} > 1) {last};
                    }
                 }
        }
        elsif($exp_strand eq "-"){
                for(my $i = 0; $i <= $max_mismatch; $i ++){
                        if($n_mis{$i} == 0){next}
                        #select single mapped postion
                        if($n_mis{$i} == 1){$s_strand = "-";$s_mis = $i; last }
                        #throw this tag's all positive postion
                        if($n_mis{$i} > 1) {last};
                }
                if($s_strand eq "nothing"){
                    for(my $i = 0; $i <= $max_mismatch; $i ++){
                        if($p_mis{$i} == 0){next}
                        #select single mapped postion
                        if($p_mis{$i} == 1){$s_strand = "+";$s_mis = $i; last }
                        #throw this tag's all positive postion
                        if($p_mis{$i} > 1){last}
                   }
                }
        }

	#if we can't find the single position, we return nothing
	if($s_strand eq "nothing"){return -1}
	
	#get the single select position 
	my $start = -1;
        my $end = -1;

        #get the single position, if this postion's mismatch value is the selected mismatched value 
        foreach my $m (@$matches){
                my ($pla, $mis)= split(/\./,$m);
                if(($pla < 0) && ($s_strand eq "-") && ($s_mis == $mis)){ return $m }
                elsif(($pla >= 0) && ($s_strand eq "+") && ($s_mis == $mis)){ return $m }
        }

}


=head1 NAME

miRNA_mapping  - one of the  miRNA detection perl modules for Grimmond Group

=head1 SYNOPSIS

  use miRNA_mapping;
  my %argv = &init();
  my $obj = QCMG::miRNA_Mate::miRNA_mapping->new(%argv);
  $obj->once_mapping($BC);
  $obj->recursive_mapping($BC);

=head1 DESCRIPTION

This module do both once mapping and recursive mapping, then it collates mapped tags and single select mapped positiong. The code for users would look something like this:

        use miRNA_mapping;

        my %argv = (
                config => "/data/example.conf",
                f2m => "/data/f2m.pl",
                mapreads => "/data/mapreads",
                mask =>  "11111111111111111111111111111111111111",
                outdir => "/data/",
                exp_name => "tag_20000",
                max_hits => 5,
                library => "/data/miRbase_all_12.0.fa", 
                exp_strand => "+",
                recursive_map_para =>"30.1.0,29.1.0,28.1.0,27.1.0,26.1.0,25.1.0,24.1.0,23.1.0,22.1.0,21.1.0,20.1.0,19.1.0,18.1.0,17.1.0",
		once_map_para="29.2.1,28.2.1,27.2.1,26.2.1,25.2.1,24.2.1,23.2.1,22.2.1,21.2.1,20.2.1,19.2.1,18.2.1,17.2.1",
                raw_tag_length => 35,
        );

        my $obj = QCMG::miRNA_Mate::miRNA_mapping->new(\%argv);
	my $BC = "barcode_name1";
        if($obj->recursive_mapping( $BC ) < 0){ die" during genome mapping!\n" }
        if($obj->once_mapping( $BC ) < 0){ die" during genome mapping!\n" }

=head2 Methods

=over 2

=item * $obj->recursive_mapping($BC );

this function first sumit a job to queueing system, which maps tags to miRNA library; after mapping it collates mapped tag and chop the non_mapped tag map it again until the non_mapped tag reached shortest length. Finally it select the single mapping position for the collated tags. It search the input file and created output file automatically. 

example of input files (tag file): tag_20000.BC1.mers35.csfasta

           final output files:	tag_20000.BC1.recursive.SIM.positive
           			tag_20000.BC1.recursive.SIM.negative
                                
=item * $obj->once_mapping($BC );

this function submit all mapping jobs to queueing system, after then collated all mapped tag and select the single mapped position.
example of input file: tag_20000.BC1.NT29
		       tag_20000.BC1.NT28
			......
		       tag_20000.BC1.NT17

	final output:	tag_20000.BC1.once.SIM.positive
			tag_20000.BC1.once.SIM.negative




=back

=head1 AUTHOR

qinying Xu (Christina) (q.xu@imb.uq.edu.au)

=head1 COPYRIGHT

Copyright 200999999999d Group, IMB, UQ, AU.  All rights reserved.


=head1 SEE ALSO

perl(1).

=cut

    
