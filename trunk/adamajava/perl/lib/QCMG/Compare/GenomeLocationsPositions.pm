package	QCMG::Compare::GenomeLocationsPositions;

use strict;
use warnings;
use Data::Dumper;

use Carp qw( carp croak );

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class   = shift;
	my $list	= shift;
	
	my $self = { postition_pointer	=> 0, 
				 list_size			=> (scalar @{$list})-1, 
				 previous_position	=> undef, 
				 current_position	=> undef, 
				 next_position		=> 'notset', 
				 positions			=> $list
				};
					
	bless $self, $class;
		
	return $self;
}

sub next_position {
	my $self = shift;
	
	print Dumper $self->{postition_pointer};
	my $pointer = $self->{postition_pointer};
	print "postition_pointer: $pointer is \t";
	print Dumper $self->{postition_pointer};
	#print Dumper $self->{ list_size			};
	#print Dumper $self->{ previous_position	};
	#print Dumper $self->{ current_position	    };
	#print Dumper $self->{ next_position		};
	#print Dumper $self->{ positions			};
	
	
	
	
	if ( $pointer == 0 ) {
		print "pointer should be 0 and is $pointer \n"; 
		$self->{current_position} 	= $self->{positions}[$pointer];
		$self->{next_position} 		= $self->{positions}[$pointer+1];
		$self->{postition_pointer}++;
		
	}elsif ( $pointer == $self->{list_size} ){ 
		$self->{previous_position} 	= $self->{current_position};
		$self->{current_position} 	= $self->{next_position};
		#$self->{next_position} 		= undef;
		$self->{postition_pointer}++;
		
	}elsif ( $self->{current} and $pointer >= $self->{list_size} ) {
		$self->{previous_position} 	= $self->{current_position};
		$self->{current_position} 	= undef;
		#$self->{next_position} 		= undef;
		$self->{postition_pointer}++;
		return 0;
				
	}elsif ( $self->next_contig() eq $self->{at_contig} ) {
		$self->{previous_position} 	= $self->{current_position};
		$self->{current_position} 	= $self->{next_position};
		#$self->{next_position} 		= $self->{positions}[$pointer+1];
		$self->{postition_pointer}++;
		
	}elsif ( ! $self->next_isset() ) {
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
		
	return $self->current_start();
	
}


sub previous_isset{
	my $self  = shift;
	return 1 if ! undef $self->{previous_position}; 
	
	return 0;
}

sub current_isset {
	my $self  = shift;
	return 1 if ! undef $self->{current_position}; 
	
	return 0;
}

sub next_isset {
	my $self  = shift;
	return 1 if ! undef $self->{next_position}; 
	
	return 0;
}

sub num_postitions {
	my $self  = shift;	
	return $self->{list_size}; 
}

sub postition_pointer {
	my $self  = shift;	
	return $self->{postition_pointer}; 
}


sub previous_id {
	my $self  = shift;
	return $self->{previous_position}[0] if $self->previous_isset() ;
}

sub current_id {
	my $self  = shift;
	return $self->{current_position}[0] if $self->current_isset() ;
}

sub next_id {
	my $self  = shift;
	return $self->{next_position}[0] if $self->next_isset() ;
}



sub previous_contig {
	my $self  = shift;
	return $self->{previous_position}[1] if $self->previous_isset() ;
	return '';
}

sub current_contig {
	my $self  = shift;
	return $self->{current_position}[1] if $self->current_isset() ;
	return '';
}

sub next_contig {
	my $self  = shift;
	return $self->{next_position}[1] if $self->next_isset() ;
	return '';
}


sub previous_start {
	my $self  = shift;
	return $self->{previous_position}[2] if $self->previous_isset() ;
}

sub current_start {
	my $self  = shift;
	return $self->{current_position}[2] if $self->current_isset() ;
}

sub next_start {
	my $self  = shift;
	return $self->{next_position}[2] if $self->next_isset() ;
}


sub previous_end {
	my $self  = shift;
	return $self->{previous_position}[3] if $self->previous_isset() ;	
}

sub current_end {
	my $self  = shift;
	return $self->{current_position}[3] if $self->current_isset() ;	
}

sub next_end {
	my $self  = shift;
	return $self->{next_position}[3] if $self->next_isset() ;	
}


sub print_previous {	
	my $self  = shift;
	if ($self->previous_isset() ){
		#print $self->previous_id().", ";
		print $self->previous_contig().", ";
		print $self->previous_start().", ";
		print $self->previous_end()."\n";
	}else{
		print "Undef\n";
	}
}

sub print_current {
	my $self  = shift;
	if ($self->current_isset()){
		# print $self->current_id().", ";
		print $self->current_contig().", ";
		print $self->current_start().", ";
		print $self->current_end()."\n";
	}else{
		print "Undef\n";
	}
}

sub print_next {
	my $self  = shift;
	if ($self->next_isset()){
		#print $self->next_id().", ";
		print $self->next_contig().", ";
		print $self->next_start().", ";
		print $self->next_end()."\n";
	}else{
		print "Undef\n";
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
