#!/usr/bin/perl 

package color_space_filter;
use strict;
use tools_SNPs;

sub new{
        my ($class,%argv) = @_;

	my $self = {
		config => $argv{'config'},
		tag_length => $argv{'full_tag_length'},
		mask => $argv{'mask'},
	};

       if($argv{'independent_check'} eq "true" ){$self->{'independent'} = 1}
        else{$self->{ 'independent'} = 0 }


	bless $self, $class;
        return $self;
} 

sub valid_adjacent{
	my ($self,$f_in,$f_out) = @_;
	my $mask = $self->{'mask'};
	my $toolobj = new tools_SNPs($self->{'config'});
	
	#read from input: eg. chr1.exp_name.mers35.bc_reliable
	open(IN, $f_in) or $toolobj->myLog("[DIED]: can't open input file: $f_in for tools_SNPs::valid_adjacent!\n ");
	open(OUT,">$f_out" ) or $toolobj->myLog("[DIED]: can't open output file: $f_out for tools_SNPs::valid_adjacent!\n ");
	while(my $line = <IN>){
		#jump over the comment line
		if($line =~ /^#/){ next }
		
		#jump over the line, in which the sequence contain dots mark
		if($line =~ /\./){ next }

		#jump over the line, where the mismatched postion is less than 2
		chomp($line);
		my @array = split(/\t/, $line);
		my @mismatches = split(/,/,$array[7]);
		if(@mismatches < 2){ next }

		#jump over the line, where the mismatched position is not adjacent
		#or return this adjacent mismatched position to an array
		#eg.[ [-30,-29,01,01],[-20,-19,13,31] )
		my @adjacents = ();
		my $flag;
		if( ($flag =  &check_adjacent($mask, \@mismatches, \@adjacents) ) < 0 ){ next }

		#jump over the line, in which the adjacent mismatched position is not valid on color space
		#or get changed base uncleartide from reference base uncleartide sequency ($array[4])
		#and return this valid_adjacent mismatched position to @snp_array
		#example for @snp_array: [[-30,-29,00,11,A,T],[snp_position1,snp_position2,solid_ab,ref_ab,solid_base,ref_base]]
		my @snps = ();
		if(&valid_SNP_tag(\@adjacents,$array[4], \@snps) < 0 ){ next }

		#write the SNP to output file
		#there is one line for each SNP in output file, if there are two SNPs in one tag, it will be two lines.
		foreach my $snp_str (@snps){
			my ($posi_a,$posi_b,$solid_ab,$ref_ab, $solid_snp_base, $ref_snp_base) = split(/\t/,$snp_str);
			#for tag with positive strand
			my $position;
			if($array[1] eq "top"){ $position = $array[5] + $posi_a  }
			else{ $position = $array[5] + $posi_b }
			print OUT "$array[0]\t$array[1]\t$array[2]\t$array[3]\t$array[4]\t$array[5]\t$position\t$solid_ab\t$ref_ab\t$solid_snp_base\t$ref_snp_base\n";
		
		}
	}

	close(IN);
	close(OUT);

	if($toolobj->check_died() < 0){return -1}

	return 1;
}

sub potential_SNP{
	my ($self, $f_valid,$str_all_snp, $str_snp_position ) = @_;

	my $toolobj = new tools_SNPs($self->{'config'});

	#count SNP tag frequency and get following information:
	#start strand position_snp1 snp1 postion_snp2 snp2 frequency
	my $f_freq = "$f_valid.frequency";
	&count_frequency($f_valid,$f_freq,\$toolobj, $self->{tag_length});
	
	#output all SNP position 
		#get new output file name
	&output_SNP($f_freq, $str_all_snp, $str_snp_position, $self->{'independent'}, $toolobj);

	if($toolobj->check_died() < 0 ){ return -1 }

	return 1;
}

1;
sub output_SNP{
	my ($f_freq, $f_all_snp, $f_snp_position, $independent, $toolobj) = @_;

	#read snp position, base and freq into hash table
	my %all_snp = ();
	#read snp position and freq into hash table
	my %snp_position = ();
	open(FREQ, $f_freq) or $$toolobj->myLog("[DIED]:can't open input file: $f_freq, in color_space_filter::output_SNP!\n");	
	if($independent == 1){
		#modify output file name
		$f_all_snp .= ".independent";
                $f_snp_position .= ".independent";
		#read the freq file, we cound all depedent tags as 1
		while(my $line = <FREQ>){
			chomp($line);
			#the first snp
			my @array = split(/\t/,$line);
			$all_snp{"$array[2].$array[3]"} ++;
			$snp_position{$array[2]} ++;
			#if exist the second snp
			if($array[4] ne "-"){
				$all_snp{"$array[4].$array[5]"} ++;
				$snp_position{$array[4]} ++;
			}
		}
	}
	else{
		#modify output file name
		$f_all_snp .= ".dependent";
		$f_snp_position .= ".dependent";
		#read the freq file, we use all depedent tag's frequency
		while(my $line = <FREQ>){
			chomp($line);
			#the first snp
			my @array = split(/\t/,$line);
			$all_snp{"$array[2].$array[3]"} += $array[6];
			$snp_position{$array[2]} += $array[6];
			#if exist the second snp
			if($array[4] ne "-"){
				$all_snp{"$array[4].$array[5]"} += $array[6];
				$snp_position{$array[4]} += $array[6];
			}
		}
	}
	close(FREQ);

	#print the snps
	open(ALL, ">$f_all_snp") or $$toolobj->myLog("[DIED]: can't open output file: $f_all_snp in color_space_filter::output_SNP!\n");
	foreach my $key (keys %all_snp){
		my ($p, $s1,$s2) = split(/\./,$key);
		print ALL "$p\t$s1.$s2\t$all_snp{$key}\n";
	}
	close(ALL);

	open(POSI, ">$f_snp_position") or $$toolobj->myLog("[DIED]: can't open output file: $f_snp_position in color_space_filter::output_SNP!\n");
	foreach my $key (keys %snp_position){ print POSI "$key\t$snp_position{$key}\n"	}	
	close(POSI);
}


sub count_frequency{
	my ($f_valid, $f_freq,$toolobj, $tag_length) = @_;
	
	#read each SNP tag into hash table
	#eg $tag_snp{'256_12_53_F3,1025,top'} = "1030:A.T,1040:G.T"
	my %tag_snp = ();
	open(VALID, $f_valid) or $$toolobj->myLog("[DIED]: can't open input file: $f_valid during color_filter::count_frequency!\n");
	while(my $line = <VALID>){
		chomp($line);
		my ($id, $strand, $solid, $ref, $base, $start, $posi, $sold_ab, $ref_ab, $solid_base, $ref_base) = split(/\t/, $line);
		#in freq file the start position is the smallest location of all base position
		if($strand eq "reverse"){$start = $start - $tag_length + 1 }

		#unique snp tag's id, maped position and strand as hash key, the snp position and base as hash value
		#if exist the second snp in same tag, we add this snp
		my $key = "$id" . ",$start" . ",$strand";
		if(exists $tag_snp{$key}){ $tag_snp{$key} .= "," . "$posi:$solid_base.$ref_base" }
		else{ $tag_snp{$key} = "$posi:$solid_base.$ref_base"  }

	}
	close(VALID);

	#count the depedent tag's frequency
	#if the tags have same start, strand and snp, we call them depedent tag
	#eg $tag_freq{'1025,top,1030:A.T,1040:G.T'}=2
	my %tag_freq = ();
	foreach my $key (keys %tag_snp){
		my ($id,$start, $strand) = split(/,/, $key);	
		my $k = "$start".",$strand".",$tag_snp{$key}";
		$tag_freq{$k} ++	
	}
	#release the memory
	%tag_snp = ();
		
	#print indepedent snp tag into FREQ file: start strand position_snp1 base_snp1 position_snp2 base_snp2 frequency
	open(FREQ, ">$f_freq") or $$toolobj->myLog("[DIED]: can't open output file: $f_freq during color_filter::count_frequency\n");
	foreach my $key (keys %tag_freq){
		my @array = split(/,/,$key);

		#print tag with one snp to freq file, eg.
		#100	top	105	AT	-	-	2	
		if(scalar(@array) == 3 ){
			my ($p,$s) = split(/:/,$array[2]);
			print FREQ "$array[0]\t$array[1]\t$p\t$s\t-\t-\t$tag_freq{$key}\n";
		}
		#print tag with two snps, eg.
		#100	reverse	105	AT	121	CT	1		
		elsif(scalar(@array) == 4){
			my ($p1, $s1) = split(/:/, $array[2]);
			my ($p2, $s2) = split(/:/, $array[3]);
			print FREQ "$array[0]\t$array[1]\t$p1\t$s1\t$p2\t$s2\t$tag_freq{$key}\n";
		}
	}
	close(FREQ);

}

sub valid_SNP_tag{
	my $adjacents = shift;
	my $ref_base_sequency = shift;
	my $snp_array = shift;
	
	#example for @$adjacent: [[-30,-29,01,01],[-20,-19,13,31]]	
	#compare two adjacent mismatched position's color space, from solid sequence to reference sequence
	#example for @snp_array: [[-30,-29,00,11,A,T],[snp_position1,snp_position2,solid_ab,ref_ab,solid_base,ref_base]]
	foreach my $adj (@$adjacents){
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

		#caculate base space change and add result to $snp_array
		if($flag == 1){
			my $bf_snp_base;
			my $ref_snp_base;
			#the position is [0] is always smaller then $adj->[1]
			if( $adj->[0] < 0 ){
				#the base sequence is always 1 base longer than color sequence
				#snp position is tag_length - abs(first color change position) - 2
				my $d = length($ref_base_sequency) + $adj->[0] - 2;
				$bf_snp_base = substr($ref_base_sequency, $d, 1);
				$ref_snp_base = substr($ref_base_sequency, $d + 1, 1);
			}
			else{
				$bf_snp_base = substr($ref_base_sequency, $adj->[0], 1);
				$ref_snp_base = substr($ref_base_sequency, $adj->[1], 1);
			}
			my $di_nt = $bf_snp_base . $solid_a;
			my $solid_snp_base = "";
			if($di_nt =~ /A0|C1|G2|T3|a0|c1|g2|t3/){$solid_snp_base = 'A'}
			elsif($di_nt =~ /A1|C0|G3|T2|a1|c0|g3|t2/){$solid_snp_base = 'C'}
			elsif($di_nt =~ /A2|C3|G0|T1|a2|c3|g0|t1/){$solid_snp_base = 'G'}
			elsif($di_nt =~ /A3|C2|G1|T0|a3|c2|g1|t0/){$solid_snp_base = 'T'}

			my $str = "$adj->[0]\t$adj->[1]\t$solid_ab\t$ref_ab\t$solid_snp_base\t$ref_snp_base";
			push(@$snp_array, $str);
		}

	}

	if(scalar(@$snp_array) == 0){ return -1 }
	else{ return 1 }

}

sub check_adjacent{
	#an two dimension array for output, each element is the adjacent mismatch posion and color value
	#maxisum two element, since maximum two adjacent mismatches maybe four normal mismatches
	#eg.[ [-30,-29,01,01],[-20,-19,13,31] )
	#an array for input, eg. ("-30_10","-29_22","-20_13","-19_31")
	my ($mask_str,  $mismatch_array, $adjacents) = @_;	

	#for both strands tag
	my %tmp_hash = ();
	foreach my $mis (@$mismatch_array){
		my ($p,$s) = split(/_/, $mis);
		$tmp_hash{$p} = $s;
	}

	#sort mismatched postion
	my @posi = sort { $a <=> $b } keys %tmp_hash ;
	
	my $flag = -1;
	my @mask = split(//,$mask_str);
	#check whether  each mismatched position are adjacent
	for (my $i = 0; $i < (scalar(@posi) - 1) ; $i++){
		#if they are adjacent and reliable, then add these position into the output hash table which will be used for sub valid_adjacent
		#since the position can be minice so we look at it's abs value
		if( (abs($posi[$i + 1] - $posi[$i] ) == 1) && ($mask[abs($posi[$i+1])] == 1 ) && ($mask[abs($posi[$i])] == 1)  ){
			#get the adjacent mismatch's postion and color value
			push(@$adjacents, [$posi[$i],$posi[$i+1],$tmp_hash{$posi[$i]},$tmp_hash{$posi[$i+1]} ]);
			$flag = 1;

		}
	}
	#if these position are not adjacent, return -1; else return 1.
	return $flag;
}


=head1 NAME

color_space_filter  - one of the  SNPs prediction perl modules for Grimmond Group

=head1 SYNOPSIS

  use color_space_filter;
  my $obj = color_space_filter->new(%argv);
  $obj->valid_adjacent($input, $output);
  $obj->potential_SNP();

=head1 DESCRIPTION

This module check input files which is bc file formart, filter the valid and adjacent mismatched tags into output. it can also show SNP position, frequency and SNP type. it is like:

	use color_space_filter;	
	
	my %argv = ( config => "test.conf", full_tag_length => 35, mask => "11111111111111111111111111111111111" );
	
	my $obj = color_space_filter->new(%argv);
	
        my $input = "/data/chr1.tag_20000.mers35.single_selected.mismatched.bc";
        
	my $output = "/data/chr1.tag_20000.mers35.valid_adjacent";
        
	if( $obj->valid_adjacent($input, $output) < 0 ){ die" creating valid adjacent output ($chr) !\n" }

        $input = "/data/chr1.tag_20000.mers35.valid_adjacent";
	
        my $output1 = "/data/chr1.tag_20000.mers35.all_SNPS";

        my $output2 = "/data/chr1.tag_20000.mers35.SNP_position";
	
	if( $obj->potential_SNP($input, $output1,$output2) < 0 ){ die "[DIED]:finding SNP position !\n") }


=head2 Methods

=over 2

=item * $obj->valid_adjacent($input, $output)

	This function detect the valid adjacent mismatch tags as potential SNPs and report it into output file. 

=item * $obj->potential_SNP($input, $output1, $output2);
	
	This function catch the informations from the output of the above function, then print all SNP position and frequency into the output1 file, the SNP position, frequency and SNP type into output2 file.


=back

=head1 AUTHOR

qinying Xu (Christina) (q.xu@imb.uq.edu.au)

=head1 COPYRIGHT

Copyright 2008 Grimmond Group, IMB, UQ, AU.  All rights reserved.


=head1 SEE ALSO

perl(1).

=cut
