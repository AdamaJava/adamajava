package QCMG::Compare::GenomeLocations;

################################################################################
#
#  Module:   QCMG::Compare::GenomeLocations.pm
#  Creator:  Matthew J Anderson
#  Created:  2012-11-02
#
#  For comparing overlaping genome locations.
#  
#  $Id$
#
################################################################################

use strict;
use warnings;
use Data::Dumper;

use Carp qw( carp croak );

use QCMG::Compare::GenomeLocationsPositions;


use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class   = shift;
    my $debug   = shift;
	    

	# Store the connection and cursor for the life of the class 
    my $self = {
		debug           		=> ( $debug ?  1 :  0  ),
        window_size				=> 0,
		position_lists			=> [], 
										# [ {postition_pointer 	- Int
										#	list_size			- Int
										# 	previous			- Pointer to an array element in postitions
										# 	current				- Pointer to an array element in postitions
										# 	next				- Pointer to an array element in postitions
										#	postitions			- Array of positions
										#	}, 
										# ] 
		position_list_size		=> 0,
			
		matched 				=> [], 
		unmatched 				=> '',
		
		at_contig 				=> "",
		at_previous_position	=> 0,
		at_position				=> 0,
		at_next_position		=> 0
    };    

	bless $self, $class;
		
	return $self;
}

sub DESTROY {
   my $self = shift;
}


sub set_window_size {
	my $self 		= shift;
	my $windowSize 	= shift;
		
	$self->{window_size} = $windowSize;
}

sub add_list {
	my $self 	= shift;
	my $list 	= shift;
		
	my $position_lists = $self->{position_lists};
	#$self->{position_list_size} = push ( @$position_lists, $list);
		
	#	$self->{position_list_size} = push (
	#					@$position_lists,
	#						{ 	postition_pointer	=> 0, 
	#							list_size			=> (scalar @$list)-1, 
	#							previous			=> '', 
	#							current				=> '', 
	#							next				=> '', 
	#							positions			=> $list
	#						}
	#					) -1 ;

	my $new_rec = QCMG::Compare::GenomeLocationsPositions->new($list);
	#print Dumper $new_rec	;
	
	#print Dumper $new_rec->{previous_position} 	;
	#print Dumper $new_rec->{current_position} 		;
	#print Dumper $new_rec->{next_position} 		;
	
	#{ 	postition_pointer	=> 0, 
	#				list_size			=> (scalar @$list)-1, 
	#				previous			=> undef, 
	#				current				=> undef, 
	#		    	_next				=> undef, 
	#				positions			=> $list
	#			};
	push @{$position_lists}, $new_rec;
	$self->{position_list_size} = scalar( @{ $position_lists } ) -1;

	#print "Added ".scalar @$positions)." positions \n";	
		
	return $self->{position_list_size};
}

sub sort_list{
	# If Needed
}

sub compare {
	my $self = shift;
	my $position_lists = $self->{position_lists};
	
	#print Dumper $position_lists; 
	
	my $list = $position_lists->[0];
	
	#foreach my $file ( @$position_lists ){
		#$file->print_previous() ;
		#$file->print_current() 	;
		#$file->print_next() 	;
		
		$list->next_position();
		$list->next_position();
		print Dumper $list;
		
		#$list->next_position();
		
		 
		
		#print $file->next_start()."\n";
		
		#$file->print_previous() ;
		#$file->print_current() 	;
		#$file->print_next() 	;

		
	#}
	#print Dumper $self->{position_lists}, $self->{at_contig}, $self->{at_position}, $self->{at_next_position};
	
	my $count = 0;		
	#do {
	#	if ( $self->_end_of_contig ( $position_lists ) ) {
	#		$self->_change_contig();
	#		
	#		foreach my $file ( @$position_lists ){
	#				#$file->next_position();
	#				#$file->print_current();
	#				#$file->print_next();
	#				
	#			if ( $file->current_contig() ne $self->{at_contig} and $file->next_contig() eq $self->{at_contig}) {
	#				$file->next_position ();
	#			}
	#			print Dumper $file->{previous_position} 	;
	#			print Dumper $file->{current_position} 		;
	#			print Dumper $file->{next_position} 		;
	#			
	#			#$file->print_current();
	#			#$file->print_next();
	#			
	#			#print Dumper $file; 
	#		}
	#		
	#	}
    
		# Cycling trough poitions 
	#	print $self->{at_contig}.':'.$self->{at_position}.' next is '.$self->{at_next_position}."\n";
	#	foreach my $file ( @$position_lists ){
	#	
	#		if ( $file->{current} ){
#	#			print "\t".$file->{current}[1].':'.$file->{current}[2].' next is '.$file->{next}[1].':'.$file->{next}[2]."\n";
	#			
	#			#print "file is at ".$file->{current}[2]."\n";
	#			if ( $file->{current}[2] < $self->{at_position} ) {
	#		    	$self->_next_position ( $file );
	#			}
	#		}
	#		
    #        print Dumper $self->{at_next_position},$self->{at_position};
	#		if ( $self->{at_next_position} <= $self->{at_position} ) {
	#			if ( $file->{_next}) {
	#				$self->{at_next_position} = $file->{_next}[2];
	#			} else{
	#				$self->_next_position ( $file );
	#			}
	#				
	#		}elsif( $file->{current}[2] > $self->{at_position} and $file->{current}[2] < $self->{at_next_position}){
	#			$self->{at_next_position} = $file->{current}[2];
	#		}			
	#	}
	#	
	#	
	#	if ( $self->_at_position() ) {
	#		# compare
	#		my @here;
	#		foreach my $file ( @$position_lists ){
	#			if ( $file->{current} ){
	#				if ( $file->{current}[2] == $self->{at_position} ){
	#					push ( @here,  $file->{current}[0] );
	#				#print "\t";
	#				#print $file->{current}[0].", ";
	#				#print $file->{current}[1].", ";
	#				#print $file->{current}[2].", ";
	#				#print $file->{current}[3]."\n";
	#				}
	#			}
	#		}
	#		if (scalar @here > 1) {
	#			my $matched =  $self->{matched};
	#			print $self->{at_contig}.":".$self->{at_position}." - \t".join( ', ', @here)."\n";
	#			
	#			push (@$matched, [@here] );
	#		}
	#	}
	#	
	#	
	#	$self->{at_position} = $self->{at_next_position};
	#	
	#	#exit if $count++ == 210;
	#} until $self->_finished () ;
	
	print "Finished Comparing at $count \n";
}

sub _finished {
	my $self = shift;	
	my $position_lists = $self->{position_lists};
		
	foreach my $file ( @$position_lists ){
		return 0 if $file->{current}; # != 0
	}
	print "Finished\n";
	return 1;
}
	 
sub _next_position {
	my $self = shift;
	my $list = shift;	

	my $pointer = $list->{postition_pointer};
	#print "List size is: ".$list->{list_size}." with pointer at: $pointer now ";

#    print Dumper $list,$pointer;
	#print Dumper $pointer;
	if ( $pointer == 0 ) {
		#print "pointer should be 0 and is $pointer \n"; 
		$list->{current} 	= $list->{positions}[$pointer];
		$list->{_next} 		= $list->{positions}[$pointer+1];
		$list->{postition_pointer}++;
		#$pointer++;
		
	}elsif ( $pointer == $list->{list_size} ){ 
		#print "pointer should be EQUAL then list size and is $pointer \n"; 
		$list->{previous} 	= $list->{current};
		$list->{current} 	= $list->{_next};
		$list->{_next} 		= undef;
		$list->{postition_pointer}++;
		#$pointer++;
		
	}elsif ( $list->{current} and $pointer >= $list->{list_size} ) {
		#print "pointer should be GREATER or equal to list size and is $pointer \n"; 
		$list->{previous} 	= $list->{current};
		$list->{current} 	= undef;
		$list->{_next} 		= undef;
		$list->{postition_pointer}++;
		#$pointer++;
		#print $list->{postition_pointer}." \n";
		return 0;
				
	}elsif ( $list->{_next}[1] eq $self->{at_contig} ) {
		#print "pointer should LESS then list size and is $pointer \n"; 
		$list->{previous} 	= $list->{current};
		$list->{current} 	= $list->{_next};
		$list->{_next} 		= $list->{positions}[$pointer+1];
		$list->{postition_pointer}++;
		#$pointer++;
		
	}elsif ( ! defined $list->{_next} ) {
			#print "Current contig is ".$list->{current}[1]." Waiting for next contig ".$list->{next}[1]." still on contig ".$self->{at_contig}." At pointer ".$list->{postition_pointer}."\n";
			#print $list->{postition_pointer}." \n"; 
			return 0;
	
	}else{
		#print " - No more positions left. At pointer ".$list->{postition_pointer}."\n";
		
		#print Dumper $list->{previous} ;
		#print Dumper $list->{current} ;
		#print Dumper $list->{next}; 		
		#print $list->{postition_pointer}." \n"; 
		return 0;
	}
	#print $list->{postition_pointer}." \n"; 
	
	
	#print $list->{current}[1].", ";
	#print $list->{current}[2]."\n";
		
	return $list->{current}[2];
}


sub _at_position {
	my $self 	= shift;
	my $position_lists = $self->{position_lists};	
	
	foreach my $file (@$position_lists){
		if ( $file->{current} ) {
			if ($file->{current}[1] eq $self->{at_contig} and $file->{current}[2] == $self->{at_position} ){
				return 1;
			}
		}
	}
	return 0;
	
}


sub _end_of_contig {
	my $self 	= shift;
	my $position_lists = $self->{position_lists};
	
	if ( ! $self->_finished () ) {
		foreach my $file ( @$position_lists ){
			if ( $file->{_next} ){
				return 0 if $file->{_next}[1] eq $self->{at_contig} ;
			}
		}
	}
	#print "\nEnd of contig ".$self->{at_contig}."\n";
	return 1;
}


sub _change_contig {
	my $self 			= shift;
	my $position_lists 	= $self->{position_lists};
	
	my $contig;
	if ( ! $self->_finished() ) {
		
		foreach my $file ( @$position_lists ){
			if ( $file->{_next} ){
				$contig = $file->{_next}[1] if ! defined $contig;
				
				if ($file->{_next}[1] ne $contig ) {
					print "contigs not sorted: new is ".$file->{_next}[1].", current is ".$contig." \n" 
				}
			}
		}
		#print "Changing to contig $contig ...\t\t";
		if ($contig) {
			$self->{at_contig} = $contig;
			$self->{at_previous_position} 	= 0;
			$self->{at_position} 			= 0;
			$self->{at_next_position}		= 0;
		}
		print "\nComparing posisition for contig ".$self->{at_contig}."\n";
	}
	
	return '';
}
	


sub print_matched {
	my $self = shift;
	
	print "Matched \n";
	foreach my $match ( $self->{matched} ){
		print.join( ', ', @$match)."\n";
	}
	

}


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
