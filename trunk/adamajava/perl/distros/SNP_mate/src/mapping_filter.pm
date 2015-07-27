#!/usr/bin/perl -w

package mapping_filter;
use strict;
use tools_SNPs;

sub new{
        
	# get all experiment parameter 

	my ($class, %arg)= @_;
        my $self = {
	 tag_length => $arg{'full_tag_length'},
	 config => $arg{'config'},
	 chr_names =>  $arg{"chromosomes"},
	 f2m => $arg{"f2m"},
	 chrdir => $arg{"chr_path"},
	 mask =>  $arg{"mask"},
	 mismatch => $arg{"num_mismatch"},
	 adj_valid => $arg{"adj_valid"},
	 outdir => $arg{"output_dir"},
	 exp_name => $arg{"exp_name"},
	 max_hits => $arg{"max_multimatch"},
 	};
	
        bless $self, $class;	
        return $self;   
}

sub genomic_mapping{

	# to map all genomic files
	my $self = shift;
        my $tag_length = shift;
	my $toolobj = tools_SNPs->new($self->{'config'});
	
	#check the log file before run mapping
	if( $toolobj->check_died < 0){ return -1 }

	$toolobj->myLog("[PROCESS]: mapping to all chromosomes\n ");

	my $tag_file = $self->{outdir} . "$self->{exp_name}.mers$tag_length.unique.csfasta";
	my @chromosomes = split(/,/,$self->{chr_names});	
	if( !(-e $tag_file) ){ $toolobj->myLog("[DIED]: file: $tag_file, is not exist! \n"); return -1   }
	# if input file is zero size, we jump over the mapping process and create an zero size output file.
	if( -z $tag_file ){
                $toolobj->myLog("[DIED]:no tag for genome mapping in $tag_file!\n");
                return -1;
        }

	#start to match all genomic fiels
	my $mymask =  substr($self->{mask},0,$tag_length);	
	 
	my %q_file = ();
	foreach my $chr (@chromosomes){
		my $comm = "$self->{f2m}   -g  $self->{chrdir}" . $chr . ".fa ";
		$comm .= "-r $tag_file -d $self->{outdir} -t $tag_length ";
		$comm .= "-e $self->{mismatch} -z $self->{max_hits} -p $mymask -a $self->{adj_valid}";
		my $mysh = $self->{outdir}. "$chr.$self->{exp_name}.mers$tag_length.sh"; 
		open(OUT,"> $mysh") or $toolobj->myLog("[DIED]: can't open file $mysh");
		print OUT $comm;
		close(OUT);
	
		my $success_file = $self->{'outdir'} . "$chr.$self->{'exp_name'}.mers$tag_length.unique.csfasta.ma.$tag_length.$self->{'mismatch'}.success";
		$comm = "qsub -l walltime=48:00:00,ncpus=2 -o $mysh.out -e $mysh.err $mysh > $mysh.id ";
 		
		my $rc = system($comm);
		
		# add an success file name into the hash table which will be used to wait all mapping finish	
		$q_file{$success_file} = $rc;
	}

  	## wait and check the qsub job
	$toolobj->myLog("[PROCESS]: waitting for queue  \n");
	$toolobj->wait_for_queue(\%q_file);

	#check the log file again, it make sure whether all mapping is successful
        if( $toolobj->check_died < 0 ){ return -1 }
	
	# when all mapping is done, add an successful information into log file
	$toolobj->myLog("[SUCCESS]: mapped to all chromosomes\n ");
	return 1;
}

sub collate_genomic_matches{
	my $self = shift;
	my $tag_length = shift;
	my $toolobj = tools_SNPs->new($self->{'config'});

	$toolobj->myLog("[PROCESS]: collating genome mers:$tag_length\n");

	my $f = $self->{outdir} . "$self->{exp_name}.mers$tag_length.genomic.collated";
	open(COLLATED,">$f") or $toolobj->myLog("[DIED]: can't open file: $f\n") ; 

	# open all genomic mapped file and put its file handle into an hash table
        my @chromosomes = split(/,/,$self->{chr_names});
        my %fh = ();
        foreach my $chr (@chromosomes){
	    my $f = "$self->{outdir}$chr.$self->{exp_name}.mers$tag_length.unique.csfasta.ma.$tag_length.$self->{mismatch}";
	    open(my $handle,$f) or $toolobj->myLog( "[DIED]: Can't open $f during genomic collating\n");
            $fh{$chr} = $handle; 	
	} 	

        #read a tag from first genome maped file
	my $first_chr = shift(@chromosomes);
        my $first_f = $fh{$first_chr};
	#the flag is for check whether all tag are in same order in different mapped files
	my $flag = 1;
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
 		    my $new_tagid = shift(@tmp_match);
		    if($tagid ne $new_tagid){ $flag = -1; last }  
		    foreach  my $m (@tmp_match){$m = "$chr.$m"}
		    @matches = (@matches, @tmp_match);
		    last;
		}
            }
          }         
	 #if the tagid order is not consistent in all mapped files, we exist this system.
	 if($flag == -1){$toolobj->myLog("[DIED]: during collating -- the tagid order is not consistent in *.ma.$tag_length.$self->{mismatch}\n"); last}

	 # count the curent tag's total matching position in whole genome
	 my $total =  @matches;
		
	 # Here we needn't collect the non mapped tag for junction mapping
	 # when the tag is over matched, throw all matched position
         if($total > $self->{'max_hits'} ) {@matches = ()} 
         	
	 #write this matched tag into collated file
	 print COLLATED "$tagid\t$total\t";
	 foreach my $m (@matches){print COLLATED "$m\t"}
	 # write the sequence to collated file, this line is not chomped so no "\n"
	 $l = <$first_f>;
         print COLLATED "\n$l";

        }
	
	#closed all files 
	close($fh{$first_chr});
        foreach my $chr (@chromosomes){ close($fh{$chr}); } 
        close(COLLATED);

	#check log file	
        if( $toolobj->check_died < 0 ){ return -1 }

	$toolobj->myLog("[SUCCESS]: collated genome mers:$tag_length\n");
	return 1;
}

sub single_select{
	my $self = shift;
	my $f_collated = shift;

	my $toolobj = tools_SNPs->new($self->{'config'});	
	my @chromosomes = split( /,/, $self->{'chr_names'}  );

	# print a processing information into log file
        $toolobj->myLog("[PROCESS]: single selection tag mapping position\n ");
	
	#create output files and put its' file handle into an hash table
	my $tag_length = $self->{'tag_length'};
	my %f_single_select = ();
	foreach my $chr (@chromosomes){
		#for mismatche value > 0
		my $f = $self->{'outdir'} . "$chr.$self->{'exp_name'}.mers$tag_length.single_selected.mismatched";
		open(my $handle1, ">$f") or $toolobj->myLog("[DIED]: can't create new file : $f ");
		$f_single_select{"mismatched_$chr"} = $handle1; 
		#for mismatche value ==  0
		$f = $self->{'outdir'} . "$chr.$self->{'exp_name'}.mers$tag_length.single_selected.no_mismatched";
		open(my $handle2, ">$f") or $toolobj->myLog("[DIED]: can't create new file : $f ");
		$f_single_select{"no_mismatched_$chr"} = $handle2; 
	} 

	#read collated files  
	my $f = $self->{'outdir'} . "$self->{'exp_name'}.mers$tag_length.genomic.collated ";
	open(COLLATED, $f_collated) or $toolobj->myLog("[DIED]: can't open file: $f in single_select.pm\n ");
		
	while(my $line = <COLLATED>){
		#select this tag's single position 
		#return the selected mapping position's mismatch value from the lowest mismacht value
		my $selected_mismatch  = &stat_select($line, $self->{'mismatch'});	
		#print this selected position into right output files which is prepared for paralling SNP prediction
		my $sequ = <COLLATED>;
		&classify_outputs(\%f_single_select,$selected_mismatch,$line,$sequ);
		
	}
	close(COLLATED);
	
	#close all output files
	foreach my $key (keys %f_single_select) {  close( $f_single_select{ $key } )  }

        #check log file 
        if($toolobj->check_died < 0){ return -1 }
	
	$toolobj->myLog("[SUCCESS]: selected a single mapping position for each mapped tag\n");

	return 1;

}
1;

sub classify_outputs{

	my $outputs = shift;
	my $selected_mismatch = shift;
	my $line = shift;
	my $sequ = shift;

	#when there is no mapping position selected, this tag will be throw.
	if($selected_mismatch == -1){ return }
	
	chomp($line);
	#read all mapping position again
	my @matches = split(/\t/,$line);

	#shift first two elements on the array since they are not mapping positon
	my $tagid = shift( @matches );
	my $total = shift(@matches);

	
	my ( $start, $end );
	foreach my $p (@matches){
		my ($chr,$location,$mis) = split(/\./,$p);
		#caculate start and end position for this selected position
		#print this position into right output file and the output file is csfasta format
		if($mis == $selected_mismatch){
			my $f;
			if($selected_mismatch == 0){ $f = $outputs->{"no_mismatched_$chr"} }
			else{ $f = $outputs->{"mismatched_$chr"} }
			print $f "$tagid,$location.$mis\n$sequ";
			last;
		}
        }
	
	return ;
}

sub stat_select{
	#get a line from .collated file eg. "12_123_45_F3:1	chr2.-1258.2	chrY.125.0"
	my $line = shift;
	my $max_mismatch = shift;

	chomp($line);
	my @matches = split(/\t/,$line);

	#get tagid from the splited line
	my $tagid = shift(@matches);

	#get total mapping position number;
	my $total = shift(@matches);


	#initialize the hash table which store the mismatch's frequence
	my %mismatch = ();
	for(my $i = 0; $i <= $max_mismatch; $i ++){$mismatch{$i} = 0  }
	
	#read each mapping position
	#count each mismatch's frquence
	foreach my $p (@matches){
		my ($chr,$location,$mis) = split(/\./,$p);
		$mismatch{$mis} ++;
	}

	
	#find single mapping position's mismatch value from the lowest mismatch value
	#once we find the single mapping position which mismatch's frequence is 1;
	#assign this mismatch value to the $unique_mismatch
	my $selected_mismatch = -1;
	for(my $i = 0; $i <= $max_mismatch; $i ++ ){
		#if the lowest mismatch value is not exit, goto the higher mismatch value
		if( $mismatch{$i} == 0 ){ next }

		#if the lowest mismatch value exist but it is overmapped, we throw this tag
		if($mismatch{$i} != 1){ last }
		# record this single mapping position's mismacth value from the lowest mismatch value
		else {  $selected_mismatch = $i;	last}
	}
	
	return $selected_mismatch; 
	
}


=head1 NAME

mapping_filter  - one of the  SNPs prediction perl modules for Grimmond Group

=head1 SYNOPSIS

  use mapping_filter;
  my $obj = mapping_filter->new(%argv);
  $obj->main;

=head1 DESCRIPTION

This module can submit mapping jobs to queueing system, after then collate all mapped positions  and  select single position. The code for users would look something like this:

	use mapping_filter;

	my %argv = (
		full_tag_length => 35,
		config => "/data/example.conf",
		chromosomes => "chr1,chr2",
		f2m => "/data/f2m.pl",
		...
	);

	my $obj = mapping_filter->new($argv);
	if($obj->genomic_mapping( $argv{'full_tag_length'} ) < 0){ die" during genome mapping!\n" }
        if($mapobj->collate_genomic_matches($argv{'full_tag_length'}) < 0 ){ die "during collating genome mapped data!\n" }
	if($mapobj->single_select($argv{'full_tag_length'}) < 0 ){ die "[DIED]: during single selection!\n" }



=head2 Methods

=over 3

=item * $obj->genomic_mapping( %argv );

to sumit jobs to queueing system and each job is mapping tag files to different genomic files

example of input files (tag file): tag_20000.mers35.unique.csfasta

           output files (file number is as same as the genome file number):
			 
			chr1.tag_20000.mers35.unique.ma.35.3
			 	 ...
			 chrX.tag_20000.mers35.unique.ma.35.3
				
=item * $obj->collate_genomic_matches( $tag_length);

to collect whole genome mapped position for each tag

example of input files (file number is as same as the genome file number):

			 chr1.tag_20000.mers35.unique.ma.35.3
                                 ...
                         chrX.tag_20000.mers35.unique.ma.35.3

	   output files (only one file): tag_20000.mers35.genomic.collated

=itme * $obj->single_select( $tag_length );

to select a single mapped position for each tag from the .collated file, here the low mismatch value position is on high priority. Afterthen it classify the single selected position into different output files based on mapped various genome name and mismatched value. 

example of input file: tag_20000.mers35.genomic.collated

	   output file (the output file number is twice of the genome file number):

			chr1.tag_20000.mers35.single_selected.mismatched 
			chr1.tag_20000.mers35.single_selected.no_mismatched
			......

=back

=head1 AUTHOR

qinying Xu (Christina) (q.xu@imb.uq.edu.au)

=head1 COPYRIGHT

Copyright 2008 Grimmond Group, IMB, UQ, AU.  All rights reserved.


=head1 SEE ALSO

perl(1).

=cut

