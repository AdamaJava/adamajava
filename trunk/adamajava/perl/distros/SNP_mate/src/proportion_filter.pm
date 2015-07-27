#!/usr/bin/perl 

package proportion_filter;
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

sub main{
	my ($self, $f_mismatch, $f_no_mismatch, $f_valid_adjacent, $f_freq,$str_snp, $str_snp_vs_noSNP) = @_;

	my $toolobj = tools_SNPs->new($self->{'config'});

	#count all noSNP tag's start mapped position and frequency
	my @noSNP_start = &get_noSNP_start($f_mismatch,$f_no_mismatch, $f_valid_adjacent,$self->{'tag_length'},$toolobj);

	#count all SNP tag's start mapped position and frequency
	my @SNP_start = &get_SNP_start($f_freq,$toolobj);

	#join this two arrays
	my @all_start = (@noSNP_start, @SNP_start);
	#release memory 
	@noSNP_start = ();
	@SNP_start = ();
	#sort the start position
	@all_start = sort { $a->[0] <=> $b->[0] } @all_start;

	#verify input file's name for depedent or indepedent option
	#compare both SNP and noSNP frequency on the SNP position; create output file
	if($self->{'independent'} == 1){ $str_snp .= ".independent";  $str_snp_vs_noSNP .= ".independent"}
	else{ $str_snp .= ".dependent"; $str_snp_vs_noSNP .= ".dependent" }
	&SNP_vs_noSNP(\@all_start, $str_snp, $str_snp_vs_noSNP, $self->{'independent'},$self->{'tag_length'},$self->{'mask'}, $toolobj);
	
	if($toolobj->check_died() < 0){return -1}

	return 1;
}

1;
sub  SNP_vs_noSNP{
	my ($start_array, $f_snp, $f_vs, $indepedent,$tag_length,$m, $toolobj) = @_;
	my @mask = split(//,$m);

	#sort the snp file based on snp position
	system("sort -n -k1 $f_snp > $f_snp.sorted");

	open(SNP, "$f_snp.sorted") or $toolobj->myLog("[DIED]: can't open input file: $f_snp, in proportion_filter::SNP_vs_noSNP");	
	open(VS, ">$f_vs") or $toolobj->myLog("[DIED]: can't open output file: $f_vs, in proportion_filter::SNP_vs_noSNP");	
	#first point to the $start_array->[0][0]	
	my $p_start = 0;
	my $array_num = @$start_array;
	while(<SNP>){
	
		#jump over the comments line
		if(/#/){ next }
		chomp();
		#get snp position from the smallest location
		my ($posi,$freq) = split(/\t/);
		my $total  = 0;
		#point to the smallest start position, after which the tag covers the snp position as well
		while( ($p_start < $array_num) && (($posi - $start_array->[$p_start][0]) >= $tag_length) ) { $p_start ++  }
		
		#find the the biggest start position which is bigger than the snp position
		my $p_end = $p_start;
		while( ($p_end < $array_num) && (($posi - $start_array->[$p_end][0]) >= 0) ){ $p_end ++  }
		
		for(my $i = $p_start; $i < $p_end; $i++){
			my $d = $posi - $start_array->[$i][0];
			#only look at the reliable base
			if($mask[$d] == 1  ){ 
				#we count all depedent tag as one indepedent tag 	
				if($indepedent == 1){$total ++  } 
				#we count all depedent tag number, which is store the second column of the start array 
				else{ $total += $start_array->[$i][1] }
			}
		}
		#the number of no_snp tag is the total - snp tag
		my $no_snp = $total - $freq;
		print VS "$posi\t$freq\t$no_snp\n";
	}

	close(VS);
	close(SNP);
}

sub get_SNP_start{
	my ($f_freq, $toolobj) = @_;

	my @SNP_start = ();
	#add all snp starts and frequency into array
	#if there are several same start means they mapped on same position but with differnt snp or strand
	open(FREQ, $f_freq) or $toolobj->myLog("[DIED]:can't open file: $f_freq in proportion_filter::get_SNP_start.\n ");
	while(<FREQ>){
		if(/#/){ next }
		chomp();
		my ($start,$strand,$snp1,$bc1,$snp2,$bc2,$freq) = split(/\t/);
		push @SNP_start, [$start,$freq]
	}
	close(FREQ);

	return @SNP_start;
}

sub get_noSNP_start{
	my ( $f_bc, $f_no_mismatch, $f_valid_adjacent,$tag_length, $toolobj ) = @_;

	my %mismatch = ();
	#mark all tag with mismatch as -1
	open(BC, $f_bc) or $toolobj->myLog("[DIED]: can't open input file $f_bc in proportion_filter::get_noSNP_start,\n ");	
	while(<BC>){
		if( /#/){ next }
		#read the $line starting, eg ">4_22_12_F3:1,25636.2"
		chomp();
		my ($tagid,@array) = split(/\t/);
		$mismatch{$tagid} = -1;		
	}
	close(BC);

	#mark all tag belong to valid_adjacent files to 1
	open(VALID, $f_valid_adjacent) or $toolobj->myLog("[DIED]: can't open input file $f_valid_adjacent in proportion_filter::get_noSNP_start,\n ");	
	while(<VALID>){
		if(/#/){ next }
		chomp();
		my ($tagid, @array) = split(/\t/);
		$mismatch{$tagid} = 1;
	}
	close(VALID);

	my %top_start = ();
	my %reverse_start = ();
	open(BC, $f_bc) or $toolobj->myLog("[DIED]: can't open input file $f_bc in proportion_filter::get_noSNP_start,\n ");	

	while(<BC>){
		if( /#/){ next }
		chomp();
		my ($tagid, $strand, $solid, $ref, $ref_base, $start, @array) = split(/\t/);
		if($mismatch{$tagid} == -1) {
			if($strand eq "top" ){ $top_start{$start} ++  }
			elsif($strand eq "reverse"){ $start = $start - $tag_length + 1; $reverse_start{$start} ++  }
		}
	}
	close(BC);

	#release memory
	%mismatch = ();
	
	open(NO_MIS, $f_no_mismatch) or $toolobj->myLog("[DIED]: can't open input file $f_no_mismatch in proportion_filter::get_noSNP_start,\n ");	
	while(<NO_MIS>){
		if(! m/^>/){ next }
		chomp();
		my ($tagid, $map) = split(/,/);
		my ($p,$zero) = split(/\./,$map);
		
		if($p < 0){ my $start = $p + $tag_length + 1; $reverse_start{abs($start)} ++  }
		else{ $top_start{$p} ++}
	}	
	close(NO_MIS);

	#add all counted noSNP tag start and its frequency into two demension array
	#since the tags with different strand are indepedent, so we push it to arry twice
	my @tag_start = ();
	foreach my $key (keys %top_start){ push @tag_start,[$key, $top_start{$key}]	};
	foreach my $key (keys %reverse_start){ push @tag_start, [$key, $reverse_start{$key}] 	}

	return @tag_start;

}

=head1 NAME

tag_quality_filter  - Perl module for SNPs prediction

=head1 SYNOPSIS

  use NewModule;
  my $obj = new tag_quality_filter;
  $obj->valid_adjacent(aa.bc, aa.bc.valid_adjacent);

=head1 DESCRIPTION

This module check input files which is bc file formart, 
filter out the valid and adjacent mismatch information

=head2 Methods

=over 1

=item * $object->valid_adjacent(input, output)


=back

=head1 AUTHOR

qinying Xu (Christina) (q.xu@imb.uq.edu.au)

=head1 COPYRIGHT

Copyright 2008 Grimmond Group, IMB, UQ, AU.  All rights reserved.


=head1 SEE ALSO

perl(1).

=cut





