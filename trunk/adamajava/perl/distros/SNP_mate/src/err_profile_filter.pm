#!/usr/bin/perl 

package err_profile_filter;
use strict;
use tools_SNPs;
use Statistics::Descriptive;


sub new{
        my ($class,%argv) = @_;

	my $self = {
		config => $argv{'config'},
		output_dir => $argv{'output_dir'},
		chromosomes => $argv{'chromosomes'},
		exp_name => $argv{'exp_name'},
		tag_length => $argv{'full_tag_length'},

	};
#        return $self( {},$class );
	bless $self, $class;
	return $self;
} 

sub create_mask{
	my ($self,$mask) = @_;
	my $toolobj = new tools_SNPs($self->{'config'});
	
	#initilize the array with 0 value for each element	
	my $tag_length = $self->{'tag_length'};
	my @base_mismatch = ();
	for(my $i = 0; $i < $tag_length; $i++ ){ $base_mismatch[$i] = 0 }
	
	my @chromosomes = split(/,/,$self->{'chromosomes'});
	foreach my $chr (@chromosomes){
	        my $f_in = "$self->{'output_dir'}$chr.$self->{'exp_name'}.mers$self->{'tag_length'}.single_selected.mismatched.bc";
		open(IN, $f_in) or $toolobj->myLog("[DIED]: can't open input file: $f_in for err_profit_filter!\n ");
		while(my $line = <IN>){
			#jump over the comment line
			if($line =~ /^#/){ next }
		
			chomp($line);
			my @array = split(/\t/, $line);
			my @mismatch_array = split(/,/,$array[7]);
		
			foreach my $mis (@mismatch_array){
				my ($posi, $change) = split(/_/,$mis);
				#when there is no mismatched happened, jump over this tag
				if($posi eq "-" ){ last }

				my $base = abs($posi);
				if($base >= $tag_length){ $toolobj->myLog("[DIED]:error on reading $f_in.\n the mismatched position $base >= $tag_length\n ") }
				else{ $base_mismatch[$base] ++ }
			}
		}
		close(IN);
	}
	#read original mapping mask
	my @map_mask = split(//,$mask);

	#only look at the base on which the mask value is 1
	my @mask_mismatch = ();
	for(my $i = 0; $i < $tag_length; $i ++){
		if($map_mask[$i] == 1){ push( @mask_mismatch, $base_mismatch[$i]) }
	}

	#get means and STD for base mismatch which mask if 1
	my $stat = Statistics::Descriptive::Full->new();
	$stat->add_data( @mask_mismatch );
	my $avg_std = $stat->mean() + $stat->standard_deviation();
	#my $sum = $stat->sum();

	#get new mask, if the mismatching frequence of the base is higher than means+std, 
	#this base is not reliable and assign 0 on the mask else assign 1
	$mask = "";
	for(my $i = 0; $i < $tag_length; $i++ ){
		if(($base_mismatch[$i]  > $avg_std ) || ($map_mask[$i] == 0) ){ $mask .= "0" }
		else{ $mask .= "1"	}
	}
	#retrun mask to reference
	$_[1] = $mask;

       #check log file 
        if( $toolobj->check_died() < 0 ){ return -1 }

	return 1 ;
}

1;


=head1 NAME

err_profile_filter  - one of the  SNPs prediction perl modules for Grimmond Group

=head1 SYNOPSIS

  use NewModule;
  my $obj = new err_profit_filter(%argv);
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





