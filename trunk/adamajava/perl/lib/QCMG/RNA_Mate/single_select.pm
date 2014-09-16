package QCMG::RNA_Mate::single_select;

use Parallel::ForkManager;
use QCMG::RNA_Mate::tools_RNA;
use strict;

sub new{
         my ($class, $arg)= @_;
         my $self = {
	 config => $arg->{'config'},
	 tag_length => $arg->{"tag_length"},
	 chr_names =>  $arg->{"chromosomes"},
	 outdir => $arg->{"output_dir"},
	 exp_name => $arg->{"exp_name"},
	 max_hits => $arg->{"max_multimatch"},
	 window =>$arg->{"rescue_window"},
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


sub main{
	$self = shift;
	$toolobj = tools_RNA->new($self->{'config'});	
	my @tag_length = split(/,/,$self->{'tag_length'});
	my @chromosomes = split( /,/, $self->{'chr_names'}  );
	
	# print a processing information into log file
        $toolobj->Log_PROCESS(" combine data from different tag length, and then classify into different strand and chromosome\n ");
	
	#create output files and put its' file handle into an hash table
	#the output files is prepared for paralled wig plot
	my %f_for_wig = ();
	foreach my $chr (@chromosomes){
		#for positive strand
		my $f = $self->{'outdir'} . "$chr.$self->{'exp_name'}.for_wig.positive ";
		my $h = "$chr.positive";
		open(my $p_handle, ">$f") or $toolobj->Log_DIED(" can't create new file : $f ");
		$f_for_wig{$h} = $p_handle; 
		
		#for negative strand
		$f = $self->{'outdir'} . "$chr.$self->{'exp_name'}.for_wig.negative";
		$h = "$chr.negative";
		open(my $n_handle, ">$f") or $toolobj->Log_DIED(" can't create new file : $f ");
		$f_for_wig{$h} = $n_handle; 
	} 

	
	#read collated files with different length 
	foreach my $l (@tag_length){ 
		#open collated file
		my $f = $self->{'outdir'} . "$self->{'exp_name'}.mers$l.genomic.collated ";
		open(COLLATED, $f) or $toolobj->Log_DIED(" can't open file: $f in single_select.pm\n ");
		
		#create stat file to store each tag's mismatch situation
		$f = $self->{'outdir'} . "$self->{'exp_name'}.mers$l.genomic.stats";
		open(STAT,">$f");		

		while(my $line = <COLLATED>){
			#jump over the tag sequence line
			if($line !~ /^>/){next}
			#select this tag's single position 
			#return a line for stats file which count each tag mismatch frequence
			#return the selected mapping position's mismatch value from the lowest mismacht value
			my ($stat_line,$selected_mismatch ) = &stat_select($line);	
			print STAT "$stat_line\n";			
			#print this selected position into right output files which is prepared for paralling wig
			&classify_outputs(\%f_for_wig,$selected_mismatch,$l,$line);
			
		}
		close(COLLATED);
		close(STAT);

	}
	
	#close all output files
	foreach my $key (keys %f_for_wig){  close( $f_for_wig{ $key } )  }

        #check log file 
        my $rc = $toolobj->check_died();
        if($rc < 0){ return -1 }

	return 1;

}
1;

sub classify_outputs{

	my $outputs = shift;
	my $selected_mismatch = shift;
	my $mers = shift;
	my $line = shift;
	chomp($line);

	#when there is no mapping position selected, this tag will be throw.
	if($selected_mismatch == -1){ return }
	

	#read all mapping position again
	my @matches = split(/\t/,$line);

	#shift first two elements on the array since they are not mapping positon
	my $tagid = shift( @matches );
	my $total = shift(@matches);
	

	foreach my $p (@matches){
		my ($chr,$location,$mis) = split(/\./,$p);

		#caculate start and end position for this selected position
		#print this position into right output file
		if($mis == $selected_mismatch){
			if($location < 0){
				my $end = abs($location);
				my $start = $end - $mers + 1;
				my $f = $outputs->{"$chr.negative"};
				print $f "$start\t$end\t1\n";
				last;

			}
			else{
				my $start = $location;
				my $end = $start + $mers - 1;
				my $f = $outputs->{"$chr.positive"};
				print $f "$start\t$end\t1\n";
				last;
			}
		}
        }
	

	return ;

}

sub stat_select{
	#get a line from .collated file eg. "12_123_45_F3:1	chr2.-1258.2	chrY.125.0"
	my $line = shift;
	my $outputs = shift;

	chomp($line);
	my @matches = split(/\t/,$line);

	#get tagid from the splited line
	my $tagid = shift(@matches);

	#get total mapping position number;
	my $total = shift(@matches);


	#initialize the hash table which store the mismatch's frequence
	my %mismatch = ();
	for(my $i = 0; $i <= $self->{'mismatch'}; $i ++){$mismatch{$i} = 0  }
	
	#read each mapping position
	#count each mismatch's frquence
	foreach my $p (@matches){
		my ($chr,$location,$mis) = split(/\./,$p);
		$mismatch{$mis} ++;
	}

	#add this tag's mismatch stats into file
	my $stat = "$tagid\t$total";
	for(my $i = 0; $i <= $self->{'mismatch'}; $i ++ ){ $stat .= "\t$mismatch{$i}" }

	
	#find single mapping position's mismatch value from the lowest mismatch value
	#once we find the single mapping position which mismatch's frequence is 1;
	#assign this mismatch value to the $unique_mismatch
	my $selected_mismatch = -1;
	for(my $i = 0; $i <= $self->{'mismatch'}; $i ++ ){
		#if the lowest mismatch value is not exit, goto the higher mismatch value
		if( $mismatch{$i} == 0 ){ next }

		#if the lowest mismatch value exist but it is overmapped, we throw this tag
		if($mismatch{$i} != 1){ last }
		# record this single mapping position's mismacth value from the lowest mismatch value
		else {  $selected_mismatch = $i;	last}
	}
	
	return ($stat,$selected_mismatch); 
	
}
