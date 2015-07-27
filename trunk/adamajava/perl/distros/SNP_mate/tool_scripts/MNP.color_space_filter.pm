#!/usr/bin/perl 

package color_space_filter;
use strict;
use tools_SNPs;

sub new{
        my ($class,%argv) = @_;

	my $self = {  config => $argv{'config'} 	};

	bless $self, $class;
        return $self;
} 

sub valid_adjacent{
	my ($self, $mask, $f_in, $f_out) = @_;

	my $toolobj = new tools_SNPs($self->{'config'});
	
	#read from input: eg. chr1.exp_name.mers35.bc_reliable
	open(IN, $f_in) or $toolobj->myLog("[DIED]: can't open input file: $f_in for tools_SNPs::valid_adjacent!\n ");
	open(OUT,">$f_out" ) or $toolobj->myLog("[DIED]: can't open output file: $f_out for tools_SNPs::valid_adjacent!\n ");
	while(my $line = <IN>){
		#jump over the comment line and the sequence with dots mark
		if($line =~ /^# | \./){ next }
		
		#jump over the line, where the mismatched postion is less than 2
		chomp($line);
		my @array = split(/\t/, $line);

		#get all deatiled mismatches information
		my @info_mis = split(/,/,$array[7]);
		if(scalar(@info_mis) < 2){ next }

		#get each mismatch position and sort it
		my @posi_mis = ();
		foreach my $m (@info_mis){ my ($bef,$aft) = split(/_/,$m); push (@posi_mis, $bef)  }
		@posi_mis = sort {$a <=> $b} @posi_mis;

		my $start = shift(@posi_mis);		
		


	}

	close(IN);
	close(OUT);
}

sub potential_SNP{
	my ($self, $f_valid_adjacent,$f_all_SNPs, $f_SNP_position ) = @_;

	
	my $toolobj = new tools_SNPs($self->{'config'});
	
	#read from input: eg. chr1.exp_name.mers35.bc_reliable
	open(VALID, $f_valid_adjacent) or $toolobj->myLog("[DIED]: can't open input file: $f_valid_adjacent for tools_SNPs::valid_adjacent!\n ");

	my %SNP_pos = ();
	my %SNP_all = ();
	while(my $line = <VALID>){
		#jump over the line which contains # or .
		if($line =~ /[#|\.]/){ next }
	
		chomp($line);
		my @array = split(/\t/,$line);
		
		my $key = "$array[6] . $array[10] . $array[9]";
		$SNP_all{$key} ++;
		$SNP_pos{$array[6]} ++;
	}

	close(IN);
	
	open(ALL,"> $f_all_SNPs" ) or $toolobj->myLog("[DIED]: can't open output file: $f_all_SNPs for tools_SNPs::valid_adjacent!\n ");
	open(POSITION, "> $f_SNP_position"  ) or $toolobj->myLog("[DIED]: can't open output file: $f_SNP_position for tools_SNPs::valid_adjacent!\n ");
	
	foreach my $all (sort keys %SNP_all){print ALL "$all\t$SNP_all{$all}\n"}	
	foreach my $pos (sort keys %SNP_pos){print POSITION "$pos\t$SNP_pos{$pos}\n"}

	close(POSITION);
	close(ALL);
}

1;

sub valid_SNP_tag{
	my $adjacent_hash = shift;
	my $ref_base_sequency = shift;
	my $snp_array = shift;
	
	#get all kesys from the input $adjacent_hash table
	my @posi = keys %$adjacent_hash;
	
	#compare two adjacent mismatched position's color space, from solid sequence to reference sequence
	for( my $i = 0; $i < (@posi - 1); $i ++ ){
		my ($solid_a, $ref_a) = split(//, $adjacent_hash->{$posi[$i]} );
		my ($solid_b, $ref_b) = split(//, $adjacent_hash->{$posi[$i + 1] } );
		my $solid_ab = $solid_a . $solid_b;
		my $ref_ab = $ref_a . $ref_b;

		my $flag = -1;
		#if two adjacent mismatched color value is in same group, they are potential snp
		if( ($ref_ab =~ /00|11|22|33/) && ( $solid_ab =~ /00|11|22|33/)  ) {   $flag = 1  }
		elsif( ($ref_ab =~ /10|01|32|23/) && ( $solid_ab =~ /10|01|32|23/)  ) {$flag = 1  }
		elsif( ($ref_ab =~ /20|02|31|13/) && ( $solid_ab =~ /20|02|31|13/)  ) {$flag = 1  }
		elsif( ($ref_ab =~ /30|03|12|21/) && ( $solid_ab =~ /30|03|12|21/)  ) {$flag = 1  }


		#caculate base space change and add result to $snp_array
		if($flag == 1){
			my $bf_snp_base = substr($ref_base_sequency, abs($posi[$i]), 1);
			my $ref_snp_base = substr($ref_base_sequency,abs($posi[$i + 1]), 1);

			my $di_nt = $bf_snp_base . $solid_a;
			my $solid_snp_base = "";
			if($di_nt =~ /A0|C1|G2|T3/){$solid_snp_base = 'A'}
			elsif($di_nt =~ /A1|C0|G3|T2/){$solid_snp_base = 'C'}
			elsif($di_nt =~ /A2|C3|G0|T1/){$solid_snp_base = 'G'}
			elsif($di_nt =~ /A3|C2|G1|T0/){$solid_snp_base = 'T'}
			
			my $str = "$posi[$i]\t$posi[$i+1]\t$solid_ab\t$ref_ab\t$solid_snp_base\t$ref_snp_base";
			push(@$snp_array, $str);

			
		}
	}
	
	if(@$snp_array == 0){return -1}
	else{return 1}

}

sub check_adjacent{
	my $mismatch_array = shift;	#an array for input, eg. ("14_10","15_22")
	my $adjacent_hash = shift;	#an hash for output, eg. (14=>"10",15=>"22" )

	my %tmp_hash = ();
	foreach my $mis (@$mismatch_array){
		my ($p,$s) = split(/_/, $mis);
		$tmp_hash{$p} = $s;
	}

	#sort mismatched postion
	my @posi = sort { $a <=> $b } keys %tmp_hash ;
	
	my $flag = -1;
	#check whether  each mismatched position are adjacent
	for (my $i = 0; $i < (@posi - 1) ; $i++){
		#if they are adjacent, then add these position into the output hash table which will be used for sub valid_adjacent
		#since the position can be minice so we look at it's abs value
		if(abs($posi[$i + 1] - $posi[$i] ) == 1 ){
			$adjacent_hash->{$posi[$i]} = $tmp_hash{$posi[$i]};
			$adjacent_hash->{$posi[$i + 1]} = $tmp_hash{$posi[$i + 1]};
			$flag = 1;
		}
	}
	
	#if these position are not adjacent, return -1; else return 1.
	return $flag;

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





