package QCMG::miRNA_Mate::region_picture;
use strict;
use Parallel::ForkManager;
use QCMG::miRNA_Mate::tools_miRNA;

sub new{
        my ($class, $arg)= @_;
        my $self = {
		config => $arg->{'config'},
		library_index => $arg->{'library_index'},
		w_seq_length => $arg->{'w_seq_length'},
		miRNA_freq_filter => $arg->{'miRNA_freq_filter'},
		run_seqlogo => $arg->{'run_seqlogo'}
	};
        bless $self, $class;	
        return $self;   
}
my $toolobj;
sub main{
	my ($self,$f_freq, $f_bc) = @_;

	$toolobj = QCMG::miRNA_Mate::tools_miRNA->new($self->{'config'} );	
	$toolobj->Log_PROCESS("Creating images for high frequence miRNA sequence\n");

	#get each miRNA start, end position and miRNA name on the libray to a thress dimension array
	my @high_freq = &filter_region( $self->{'miRNA_freq_filter'}, $self->{'library_index'},$f_freq );

	#create small .bc file based on same region
	my @f_prePic_region = &prePic_region_top(\@high_freq, $f_bc,$self->{'w_seq_length'});

        #create picture here
        foreach my $f(@f_prePic_region){ 
                #call ./seqlogo to create image 
                my $command = $self->{'run_seqlogo'};
                $command =~ s/\"input\"/$f/;
                $command =~ s/\"output\"/$f/;
                my $rc =  system($command);
	
		#check whether the seqlogo is failed. if failed change parameter (add -M or delete it) and run seqlogo again. 
		if($command =~ m/(\-F\s\S+)/){
			#$1 store the output file formart type from the system comment eg."-F png"
			my $f_image ;
			if($1 =~ m/GIF/i){$f_image = "$f.gif"}
			elsif($1 =~ m/PNG/i){$f_image = "$f.png"}
			elsif($1 =~ m/PDF/i){$f_image = "$f.pdf"}
			elsif($1 =~ m/EPS/i){$f_image = "$f.eps"}
			else{ $toolobj->Log_DIED("wrong output file fomart - $1\n")  }
	
        		if(!(-e "$f_image" ) ){
				if($command =~ m/\-M/){$command =~ s/\-M//}
				else{ $command .= " -M "}
				system($command);
        			if(-e "$f.gif" ){print "the file is reformated and successfully created output: $f_image\n"}
			
			}
		}
	}
        if($toolobj->check_died ){ $toolobj->Log_SUCCESS("Created all image for all miRNA sequences in certain region\n") }

	return 1;
}
1;
sub prePic_region_top{

	#this subroutine only look at top strand tags
	my ($high_freq, $f_bc,$w_seq_length) = @_;

	#creat small .bc files for each high mapped miRNA region 
	my @file_name = ();
	my %file_handle = ();
	foreach my $region (@$high_freq){
		#debug
		#print "$region->[0]\t$region->[1]\t$region->[2]\n";
		my $f = "$f_bc.$region->[2]";
		$f =~ s/\.bc//g;
		push @file_name, $f;
		open(my $handle,">$f") or $toolobj->Log_DIED("can't creat $f in sub region_bc\n");
		$file_handle{$f} = $handle
	}
	
	#sort .bc file based on tag mapped location and tagid
	system("sort -k 6,6n  -k 1,1 $f_bc > $f_bc.sorted");
	open(SORTED_BC, "$f_bc.sorted") or $toolobj->Log_DIED("can't open the sorted bc file in sub \n");
	#sort the high_freq array based on the start positon
	my @start_end = sort{$a->[0] <=> $b->[0]} @$high_freq;

	#split tags from bc file to it's region file, in which the region have high mapping frequency 
	my @current_tag = ();
	foreach my $se (@start_end){
		my $f = "$f_bc.$se->[2]";
		$f =~ s/\.bc//g;
		my $f_handle = $file_handle{$f};
		#deal with the tag which read from last loop but byond last region,
		# it may be belong to current region or next region
		if(@current_tag > 0){
			#throw the tag wich start postion is smaller than region start postion
			if($current_tag[5] < $se->[0]){ @current_tag = () }
			#if it's start postion is greater than the region end positon, we go to next region
			elsif($current_tag[5] > $se->[1]){ next }
			else{
				#the tag is mapped on this region, get is base sequence which may contain SNP
				my $base_seq = &get_ATGC(\@current_tag);
				#add "U" on begin of base sequence
				for(my $i = $current_tag[5]; $i > $se->[0];$i--){ $base_seq = 'U'.$base_seq }
				#add 'U' on end of base sequence make it same length
				for(my $i = length($base_seq); $i < $w_seq_length; $i ++ ){$base_seq = $base_seq . 'U' }
				#print standard lenght based tag into it's region file for creating picture
				print $f_handle ">$current_tag[0]\n$base_seq\n";
				@current_tag = ();
			}
		}
		while(my $line = <SORTED_BC>){
			chomp($line);
			if($line =~ /^\#/){ next } 
			my @bc_tag = split(/\t/,$line);
			#throw the tag which mapped the block between two regions in the miRNA library file
			if($bc_tag[4] =~ /\./){	next }
			
			#throw the tag if it's start positon is smaller than the region postion
			if($bc_tag[5] < $se->[0]){ next }
			#if it positon greater than region, we check this tag for next region
			if($bc_tag[5] > $se->[1]){ @current_tag = @bc_tag; last	}
			#for tag mapped on this region, do it same for the @current_tag
			my $base_seq = &get_ATGC(\@bc_tag);
			#add "U" on begin and end of base sequence, make them same length
			for(my $i = $bc_tag[5]; $i > $se->[0];$i--){ $base_seq = 'U'.$base_seq }
			for(my $i = length($base_seq); $i < $w_seq_length; $i ++ ){$base_seq = $base_seq . 'U' }
			print $f_handle ">$bc_tag[0]\n$base_seq\n";			
		}
	}
	#close all files
	my @region_files_name = ();
	foreach my $f (keys %file_handle){ close($file_handle{$f}); push @region_files_name,$f }
	close(SORTED_BC);

	return @region_files_name;
}
sub get_ATGC{
	
	my $bc_tag = shift;

	
	my @mismatches = split(/,/,$bc_tag->[7]);
	#if mismatched postion less than 2, we return reference sequence as tag base sequence
        #chop the first base from .bc file, which is just ahead of start position, eg "N".
	if(@mismatches < 2){ return substr($bc_tag->[4], 1)}
	
	#check whether adjacent mismatch position is exist or not
	my @adjacents = &check_adjacent(\@mismatches);

	#if adjacent postion, we return reference sequence as tag base sequence
        #chop the first base from .bc file, which is just ahead of start position, eg "N".
	if(scalar(@adjacents) == 0){ return  substr($bc_tag->[4], 1)}

	#check whether the adjacent mismatch posision is snp or not
	#if have, it return the replaced reference base sequence by snp
	#if not, it return the original reference base sequence
	my $base_seq = &valid_SNP_tag(\@adjacents, $bc_tag->[4]);

        #chop the first base from .bc file, which is just ahead of start position, eg "N".
	return substr($base_seq, 1);
}

sub valid_SNP_tag{
	my ($adj_array, $ref_base_sequency) = @_;
	
	#the $adj->[0] and $adj->[1] are adjacent mismatch positon
	#adj->[2] and $adj->[3] are color values from both tag and reference sequence on that postion
       foreach my $adj (@$adj_array){
                my ($solid_a, $ref_a) = split(//, $adj->[2] );
                my ($solid_b, $ref_b) = split(//, $adj->[3] );
                my $solid_ab = $solid_a . $solid_b;
                my $ref_ab = $ref_a . $ref_b;

                #if two adjacent mismatched color value is in same group, they are potential snp
                my $flag = -1;
                if( ($ref_ab =~ /00|11|22|33/) && ( $solid_ab =~ /00|11|22|33/)  ) {   $flag = 1  }
                elsif( ($ref_ab =~ /10|01|32|23/) && ( $solid_ab =~ /10|01|32|23/)  ) {$flag = 1  }
                elsif( ($ref_ab =~ /20|02|31|13/) && ( $solid_ab =~ /20|02|31|13/)  ) {$flag = 1  }
                elsif( ($ref_ab =~ /30|03|12|21/) && ( $solid_ab =~ /30|03|12|21/)  ) {$flag = 1  }
		
		if($flag == -1){ next }
                
                #caculate base space change and add result to $snp_array
                my $bf_snp_base;
                my $ref_snp_base;
                #the position is [0] is always smaller then $adj->[1]
		my $bf_snp_position;
                if( $adj->[0] < 0 ){
                         #the base sequence is always 1 base longer than color sequence
                         #snp position is tag_length - abs(first color change position) - 2
                         my $bf_snp_position = length($ref_base_sequency) + $adj->[0] - 2;
		 }
                 else{
			$bf_snp_position = $adj->[0];
                 }
	         $bf_snp_base = substr($ref_base_sequency, $bf_snp_position, 1);
                 $ref_snp_base = substr($ref_base_sequency, $bf_snp_position + 1, 1);

                 my $di_nt = $bf_snp_base . $solid_a;
                 my $solid_snp_base = "";
                 if($di_nt =~ /A0|C1|G2|T3|a0|c1|g2|t3/){$solid_snp_base = 'A'}
                 elsif($di_nt =~ /A1|C0|G3|T2|a1|c0|g3|t2/){$solid_snp_base = 'C'}
                 elsif($di_nt =~ /A2|C3|G0|T1|a2|c3|g0|t1/){$solid_snp_base = 'G'}
                 elsif($di_nt =~ /A3|C2|G1|T0|a3|c2|g1|t0/){$solid_snp_base = 'T'}

		#if exist snp, we replace the snp on reference base sequence
		substr($ref_base_sequency, $bf_snp_position+1,1,$solid_snp_base);
		
         }
	#return the reference base sequence which repalced by snp if have snp
	return $ref_base_sequency;
}

sub check_adjacent{
	my $mis_array = shift;
	my @adjacents = ();
	
        my %tmp_hash = ();
        foreach my $mis (@$mis_array){
                my ($p,$s) = split(/_/, $mis);
                $tmp_hash{$p} = $s;
        }

        #sort mismatched postion
        my @posi = sort { $a <=> $b } keys %tmp_hash ;
        #check whether  each mismatched position are adjacent
        for (my $i = 0; $i < (scalar(@posi) - 1) ; $i++){
                #since the position can be minice so we look at it's abs value
                if ( (abs($posi[$i + 1] - $posi[$i] ) == 1) ) {
                        #get the adjacent mismatch's postion and color value
                        push(@adjacents, [$posi[$i],$posi[$i+1],$tmp_hash{$posi[$i]},$tmp_hash{$posi[$i+1]} ]);
                }
        }
	
	return @adjacents;
}

sub filter_region{
	my ($min_freq, $lib_index,$f_freq) = @_;

	#read the library index file into a hash table
	my %index = ();
	open(INDEX, $lib_index) or $toolobj->Log_DIED("can't open the miRNA library index file in sub filter_region\n");
	while(<INDEX>){	chomp(); my ($miRNA,$start,$end) = split(/\t/); $index{$miRNA} = "$start\t$end"	}
	close(INDEX);

	#report all high frequency miRNA start and end postion to a two demenion arry
	my @high_freq = ();
	open(FREQ, $f_freq) or $toolobj->Log_DIED("can't open the frequency file $f_freq in sub filter_region\n");
	while(<FREQ>){
		chomp();
		my ($miRNA, $freq) = split(/\t/);
		if($freq >= $min_freq){
			my @start_end = split(/\t/,$index{$miRNA});
			push @start_end, $miRNA;
			push @high_freq, \@start_end;
		}
	}
	close(FREQ);

	#return three dememsion arry(start, end and miRNA name)
	return @high_freq;

}


=head1 NAME

region_picture  - one of the  miRNA detection perl modules for Grimmond Group

=head1 SYNOPSIS

  use region_picture;
  my %argv = &init();
  my $obj = QCMG::miRNA_Mate::region_picture->new(\%argv);
  $obj->main($input_freq,$input_bc);

=head1 DESCRIPTION

This module select all tags which match to certain refernce miRNA sequence, these reference sequence must be matched frequently. After then these tags will be passed to a software called "seqlogo" to create images for visulizing the matching results.
        use region_picture;
        
	my $argv = {
                config => "/data/example.conf",
                library_index => "/data/miRbase_all_12.0.index",
                w_seq_length => 50,
                miRNA_freq_filter => 100,
                run_seqlogo => "/data/weblogo/seqlogo -f \"input\" -F GIF -h 10 -w 30 -k 0 -o \"output\" -c"
        };
        my $obj =QCMG::miRNA_Mate::region_picture->new(%argv);

        my $input1 = "/data/tag_20000.BC1.once.SIM.positive.freq";
        my $input2 = "/data/tag_20000.BC1.once.SIM.positive.bc";
        $obj->main($input1,$input2);

=head2 Methods

=over 2

=item * $obj->main($input1,$input2);
The first input is the frequence file which created by miRNA_freq module. The second input file is the bc file which is created by the script which listed on the parameter "script_editing". The output will be multi images for each miRNA id with high frequence. 


=back

=head1 AUTHOR

qinying Xu (Christina) (q.xu@imb.uq.edu.au)

=head1 COPYRIGHT

Copyright 2009 Grimmond Group, IMB, UQ, AU.  All rights reserved.


=head1 SEE ALSO

perl(1).

=cut
