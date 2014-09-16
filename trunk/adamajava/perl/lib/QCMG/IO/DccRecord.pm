package QCMG::IO::DccRecord;

###########################################################################
#
#  Module:   QCMG::IO::DccRecord
#  Creator:  Matthew J Anderson
#  Created:  2011-10-18
#
#  Data container for a DCC Record. 
#
#  $Id: DccRecord.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak );
use Data::Dumper;
use Memoize;
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4663 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: DccRecord.pm 4663 2014-07-24 06:39:00Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

sub new {
    my $class = shift;
    my $line  = shift;

    chomp $line;
    my @fields = split "\t", $line;
    warn 'Saw ', scalar(@fields),
        " fields, should have 18 columns [$line]\n"
        if (scalar(@fields) != 19);
    
    my $self = { analysis_id             => $fields[0],
                 tumour_sample_id        => $fields[1],
                 mutation_id             => $fields[2],
                 mutation_type           => $fields[3],
                 chromosome              => $fields[4],
                 chromosome_start        => $fields[5],
                 chromosome_end          => $fields[6],
                 chromosome_start_range  => $fields[7],
                 chromosome_end_range    => $fields[8],
                 start_probe_id          => $fields[9],
                 end_probe_id            => $fields[10],
                 copy_number             => $fields[12],
                 quality_score           => $fields[13],
                 probability             => $fields[14],
                 is_annotated            => $fields[15],
                 validation_status       => $fields[16],
                 validation_platform     => $fields[17],
                 note                    => $fields[18]
        };

    bless $self, $class;
}


sub analysis_id {
    my $self = shift;
    return $self->{analysis_id};
}           

sub tumour_sample_id {
    my $self = shift;
    return $self->{tumour_sample_id};
} 
     
sub mutation_id {
    my $self = shift;
    return $self->{mutation_id};
}
           
sub mutation_type {
    my $self = shift;
    return $self->{mutation_type};
}
        
sub chromosome {
    my $self = shift;
    return $self->{chromosome};
}
           
sub chromosome_start {
    my $self = shift;
    return $self->{chromosome_start};
}  
   
sub chromosome_end {
    my $self = shift;
    return $self->{chromosome_end};
}
       
sub chromosome_start_range {
    my $self = shift;
    return $self->{chromosome_start_range};
}

sub chromosome_end_range {
    my $self = shift;
    return $self->{chromosome_end_range};
}

sub start_probe_id {
    my $self = shift;
    return $self->{start_probe_id};
}
    
sub end_probe_id {
    my $self = shift;
    return $self->{end_probe_id};
}
        
sub copy_number {
    my $self = shift;
    return $self->{copy_number};
}
         
sub quality_score {
    my $self = shift;
    return $self->{quality_score};
}
       
sub probability {
    my $self = shift;
    return $self->{probability};
}
          
sub is_annotated {
    my $self = shift;
    return $self->{is_annotated};
}
          
sub validation_status {
    my $self = shift;
    return $self->{validation_status};
}
     
sub validation_platform {
    my $self = shift;
    return $self->{validation_platform};
}
  
sub note {
    my $self = shift;
    return $self->{note};
}

1;

__END__              

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011-2014

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
