package QCMG::Compare::GenomeLocationsSimple;

##############################################################################
#
#  Class:	QCMG::Compare::GenomeLocationsSimple.pm
#  Author:  Matthew J Anderson
#  Created: 2012-10-30
#
#  This module is for comparing 2 lists of genome locations.
#  Each list should be an array of postions. Where the poisition is array of
#  ID (users choice), Chrom, Start and Stop 
#
#  Usage: 
#  my $genomeLocations = QCMG::Compare::GenomeLocationsSimple->new();
#  my $matches = $genomeLocations->compare( $list_1, $list_2, $window_size );
#
# $Id: GenomeLocationsSimple.pm 4661 2014-07-23 12:26:01Z j.pearson $
#
##############################################################################

my $GenomeLocations = QCMG::Compare::GenomeLocationsSimple->new();
	
use strict;
use warnings;
use Data::Dumper;

# Creats a new instatnce
sub new {
    my $class   		= shift;
	my $debug   		= shift;
	
    my $self = {
		debug           => ( $debug ?  1 :  0  ),
		matches 		=> [ ], # [ [List 1], [List 2], ...]
		list1_unmatched 		=> [],
		#list2_unmatched 		=> [],
		list1_by_contigs => {},
		list2_by_contigs => {}		
	};
	
	bless $self, $class;
			
	return $self;
}

# This creats a hash index of contigs to a list of positions
# Input List of positions to be indexed.
sub _index_list_by_contig {
	my $self 		= shift;
	my $list		= shift;
	
	
	my $hashed_list = {};
	foreach my $postition ( @$list ){
		
		my $index 	= $postition->[0];
		my $contig 	= $postition->[1];
		my $start	= $postition->[2];
		my $end		= $postition->[3];
		
		if ( ! exists $hashed_list->{$contig} ) {
			$hashed_list->{$contig} = [];
		}
		push ( @{ $hashed_list->{$contig} }, $postition ); 
	}
	
	return $hashed_list;
}

# prints positions that match.
sub print_matches {
	my $self 		= shift;
	
	#my $matched_in_region = $self->{matched_in_region};
	print "\tList 1\t\t List2 \nID Chrom:Start-Stop \tID Chrom:Start-Stop\n";
	foreach my $region ( @{ $self->{matches} } ) {
		printf "%s %s:%s-%s",  $region->[0][0], $region->[0][1],  $region->[0][2], $region->[0][3];
		printf "\t%s %s:%s-%s", $region->[1][0], $region->[1][1], $region->[1][2], $region->[1][3];
		print "\n";
		
	 };
}


#
#
# 
sub compare {
	my $self 			= shift;
	my $list1			= shift;
	my $list2			= shift;
	my $window_size		= shift;
	
	my $list1_size = $#$list1;
	my $list2_size = $#$list2;
	
	# Indexing lists by contigs
	$self->{list1_by_contigs} = $self->_index_list_by_contig ($list1);
	$self->{list2_by_contigs} = $self->_index_list_by_contig ($list2);
	
	
	foreach my $contig ( keys %{ $self->{list1_by_contigs} } ) {
		# Only bother compare posistions if contigs are in both lists
		if ( exists $self->{list2_by_contigs}{$contig} ) {
			
			my $list1_positions_list = $self->{list1_by_contigs}{$contig};
			my $list2_positions_list = $self->{list2_by_contigs}{$contig};
			
			# for each postion for the current contig in list1			
			foreach my $list1_position ( @$list1_positions_list ){			
				my $l1_matched 	= 0 ;
				my $l1_index 	= $list1_position->[0];
				my $l1_chrom 	= $list1_position->[1];
				my $l1_start	= $list1_position->[2];
				my $l1_end		= $list1_position->[3];
				my $l1_stop		= $l1_end;
				if ( $l1_end == 0) {
					$l1_stop = $l1_start+1;
				}
				
				# Compare is to postions for current contig in list 2
				foreach my $list2_position ( @$list2_positions_list ){
					my $l2_index 	= $list2_position->[0];
					my $l2_chrom 	= $list2_position->[1];
					my $l2_start	= $list2_position->[2];
					my $l2_end		= $list2_position->[3];
					my $l2_stop		= $l2_end;
					if ( $l2_end == 0) {
						$l2_stop = $l2_start+1;
					}
					
					# States where positon overlap result in a postitve match.
					
					# Start of postion in list 2 is inside the start and stop of postion in list 1
					if ( $l2_start >= $l1_start-$window_size and $l2_start-$window_size <= $l1_stop+$window_size ){
						$l1_matched = 1;
						push ( @{ $self->{matches} }, [$list1_position, $list2_position] );
					
					# Start of postion in list 1 is inside the start and stop of postion in list 2
					}elsif ( $l1_start >= $l2_start-$window_size and $l1_start-$window_size <= $l2_stop+$window_size ){
						$l1_matched = 1;
						push ( @{ $self->{matches} }, [$list1_position, $list2_position] );
					}
					
				# End of list 2 loop
				}
				# recording positions in list1 that have no matches. - not used
				if ( ! $l1_matched ) {
					push ( @{ $self->{list1_unmatched} }, $list1_position);
				}
			# End of list 1 loop
			} 
			
		}
	}
	# End of Contig loop
	
	return $self->{matches}; 
}


1;

__END__

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2009-2014

Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
