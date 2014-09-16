package qBamMaker::GetMapsets;

# $Id: $

use strict;
use Carp;
 
    my %constraints = ( p => 'Project',
                        d => 'Donor',
                        r => 'Material',
                        s => 'Sample Code',
                        e => 'Sample',
                        b => 'Primary Library',
                        a => 'Aligner',
                        t => 'Mapset',
                        c => 'Capture Kit',
                        q => 'Failed QC');
                        
    my @header = [];                    
 
 	my $defaultOrder = 'pdrsebatcq';
 	my $ra_mapsets = '';
sub _init : Init {

    my ($self, $params) = @_;
    
    
    #S3: query
    my $geneus = QCMG::DB::GeneusReader->new();

    if ( $geneus->all_resources_metadata() ) {
        $ra_mapsets = $geneus->fetch_metadata();
    }
   
    
    #S4: filter query results and store related mapset to $ra_mapsets
    my $conorder = $defaultOrder;
    if($params->{conorder} != ''){ $conorder = $params->{conorder} }
    
    my @constraints = set_constraint_order($conorder );  
  
  	my $ra_mapsets = '';
    #$params{$constraint} eg. paramas{projuct} = 'icgc_pancreatic'
    foreach my $constraint (@constraints) {
        # You can't check defined or 'true' here because 'Failed QC'
        # needs to use '0' as a pattern so length() is the solution
        if (length( $params->{$constraint} )) {
            $ra_mapsets = apply_constraint( $ra_mapsets, $constraint,
                                            $params->{$constraint} );
            # Cope with case that no mapsets passed the constraint
            last if (scalar(@{ $ra_mapsets }) == 0);
        }
    } 
 
}

sub getMapsets{
	
	return $ra_mapsets;
}


sub apply_constraint {
    my $ra_mapsets = shift;
    my $field      = shift;
    my $pattern    = shift;

    die "Field [$field] does not exist in supplied records"
        unless (exists $ra_mapsets->[0]->{$field});

	#each mapset in the ra_mapsets is an hash table, eg. $rec->{'Project' } = "icgc_pancreatic"
    my @passed_recs = ();
    foreach my $rec (@{ $ra_mapsets }) {
        next unless (defined $rec->{$field} and $rec->{$field} =~ /$pattern/i);
        push @passed_recs, $rec;
    }

    qlogprint( scalar(@passed_recs),
               " mapsets passed constraint [$pattern] on field [$field]\n" )
        if $self->verbose;

    return \@passed_recs;
}

=item * $obj->recursive;

This function call QCMG::BaseClass::Mapping, collate mapped tags and then call 
BassClass::Mapping again to map to junction library, then chops the non-mapped tags
recursively.

=cut

sub set_constraint_order {
     
    my $conorder       = shift;  #eg. 'pdrsebatcq';

    # Check that all of the available constraints have been specified
    # and each specified only once.

    my @cons = split(//,$conorder);

    die "-z string [$conorder] did not contain all constraints once\n" unless
       ( join('',sort keys %{$constraints}) eq
         join('',sort @cons) );

    my @ordered_constraints = ();
    foreach my $con (@cons) {
        push @ordered_constraints, $constraints->{$con};
    }

    return @ordered_constraints   #eg [ 'Project','Primary Library'...]
}

sub check_Bam_header {
	
	my $bamfile = shift;
	
	
     #check CO lines
     
     #get the most recent qbammaker commandline
     my $lastcmd = '';
     foreach (@header){
     	my $line = $_;
     	if( $line =~ /^\@CO/ && $line =~ /qBamMaker/ ){
     		$lastcmd = $line;
     	}
     		
     }
       
    

    
    
       
 

}
     
 

1;
__END__

=back

=head1 AUTHORS

=over 3

=item Qinying Xu (Christina) (q.xu@imb.uq.edu.au)


=back

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013

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
