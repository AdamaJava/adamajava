package QCMG::QBamMaker::Up2Date;


use lib '/Users/q.xu/Documents/MyWork/EclipseProject/QCMGPerl/lib';
use strict;
use Carp;
use QCMG::IO::SamReader;
use QCMG::QBamMaker::MapsetCollection;

sub new {
   
   my ($class,$params)= @_;
     
    #S1: check final BAM header
    my $bam = QCMG::IO::SamReader->new( filename => $params->{uptodate}, verbose  => 1 );
  	my @header = split (/\n/, $bam->headers_text);

    my @coArray =  split_CO_line( \@header);	
    my %params = parse_CO_line(\@coArray);
    my @mapsets = get_mapsets(\%params);
    
    my $message = compare2RG(\@mapsets, \@header);
    my $self = {errmessage => $message};
    
    bless $self, $class;
    return $self;    	
}

sub compare2RG {
	my $mapsets = shift;
	my $header = shift;

	my @RGs;
	while (my $line = shift @{$header}) {
        if ($line =~ /^\@RG/ && $line =~ /zc\:\d+\:(\S+)\t/i) {       	
             push @RGs, $1;            
        }
	}
	
	#check mapset number
	my $n_rg = scalar @RGs;
	my $n_qu = scalar @{$mapsets};
	if( $n_rg != $n_qu ){
		return "Read group number ($n_rg) isn't equal to queried mapsets number ($n_qu)!"
	}
	
	#check each mapset
	my $flag;
	while (my $set = shift @{$mapsets}) {
		$flag = 0;
		foreach my $rg  (@RGs){
			 if($rg =~ /$set->{Mapset}/i){
			 	$flag = 1;
			 	last;
			 }			
		}
		if($flag == 0){
			return "can't find mapset in RG line: " . $set->{Mapset};
		}
	}
	
 	 return "";
	
}
#return errmessage length. 0 means don't require update; otherwise require update since there are err message
sub requireUp2date {
	my $self = shift;
	
	return length($self->{errmessage});
 
}


sub reportErrMessage {
	my $self = shift;
	return $self->{errmessage};
}
 
#@CO     CN:QCMG PN:qbammaker    CL:--failedqc 0 --mapset SN -s ^10: -d 1992 --pbs /panfs/home/qxu/qBamMaker/PBS/renamePBS/APGI_1992.HiSeq_Xenograft_CellLine.pbs --outdir /mnt/seq_results/icgc_pancreatic/APGI_1992/seq_final --name HiSeq_Xenograft_CellLine
#will split to ("failedqc 0", "mapset SN","s ^10:","d 1992", "pbs /panfs/home/manderson/job_pbs/qBamMaker/APGI_2202.HiSeq_Normal_Blood.pbs","outdir /mnt/seq_results/icgc_pancreatic/APGI_2202/seq_final","name HiSeq_Normal_Blood" )

sub get_mapsets {
	my $rh_params = shift;
	my %params = %{ $rh_params };
	
	my $msc = QCMG::QBamMaker::MapsetCollection->new(
                  verbose => $params{verbose} );
    $msc->initialise_from_lims;
    $msc->set_constraint_order( $params{conorder} );
  
    foreach my $constraint ($msc->get_constraint_order) {
    	
        # You can't check defined or 'true' here because 'Failed QC'
        # needs to use '0' as a pattern so length() is the solution
        if (length($params{$constraint})) {
            $msc->apply_constraint( $constraint, $params{$constraint} );
            # Cope with case that no mapsets passed the constraint
            last if ($msc->mapset_count == 0);
        }
    }   
    
  #  print "mapset2string: ". $msc->mapsets_to_string;
    return $msc->mapsets;
}
sub parse_CO_line {
	my $args = shift;	
	 my %params = ( Project          => '',
                   Donor            => '',
                   Sample           => '',
                  'Primary Library' => '',
                  'Sample Code'     => '',
                   Material         => '',
                   Aligner          => '',
                   Mapset           => '',
                  'Capture Kit'     => '',
                  'Failed QC'       => '',
                  'Sequencing Platform' => '',                   
                   'conorder'         => 'pdrsebatcfq');
   				   
	for (my $i = 0; $i < scalar (@$args); $i ++){
		 
		if($args->[$i] =~ /^(p|project)\s(.+)$/i ){
			$params{Project} = trim($2);
		}elsif($args->[$i] =~ /^(d|donor)\s(.+)$/i ){
			$params{Donor} = trim($2);
		}elsif($args->[$i] =~ /^(e|sample)\s(.+)$/i ){
			$params{Sample} = trim($2);
		}elsif($args->[$i] =~ /^(b|library)\s(.+)$/){
			$params{'Primary Library'} = trim($2);
		}elsif($args->[$i] =~ /^(s|smcode)\s(.+)$/){
			$params{'Sample Code'} = trim($2);
		}elsif($args->[$i] =~ /^(r|material)\s(.+)$/){
			$params{Material} = trim($2);
		}elsif($args->[$i] =~ /^(a|aligner)\s(.+)$/){
			$params{Aligner} = trim($2);
		}elsif($args->[$i] =~ /^(t|mapset)\s(.+)$/){
			$params{Mapset} = trim($2);
		}elsif($args->[$i] =~ /^(c|capture)\s(.+)$/){
			$params{'Capture Kit'} = trim($2);
		}elsif($args->[$i] =~ /^(f|platform)\s(.+)$/){
			$params{'Sequencing Platform'} = trim($2);
		}elsif($args->[$i] =~ /^(q|failedqc)\s(.+)$/){
			$params{'Failed QC'} = trim($2);
		}elsif($args->[$i] =~ /^(z|conorder)\s(.+)$/){
			$params{conorder} = trim($2);
		} 		
		
	}
	
	return   %params;	
}

sub trim {
	my $string = shift;
	$string =~ s/^\s+//; #remove leading spaces
    $string =~ s/\s+$//; #remove trailing spaces
    return $string;
}
sub split_CO_line { 
	my $header = shift;
	
	#get the last qbammaker CO line
	my $COline = ''; 
#	while (my $line = shift @{$header}) {
	foreach my $line (@{$header}) {
        if ($line =~ /^\@CO/ && $line =~ /PN:qbammaker/) {       	
            $COline = $line;
        }
	}

	#return an empty array
	if( $COline eq "" ){ return () }
	
	#split co line with "--" and "-"		
	my ($co,$cl)= split('CL:', $COline);
	my @clArray = split('--', $cl); 

	for ( my $index = $#clArray; $index >= 0; --$index ){
	 	my @array1 = split('-',$clArray[$index]);	 	
	 	if(  scalar(@array1)  > 1){
	 		$clArray[$index] = ""; #delete it
	 		push @clArray, @array1;
	 	}
 		 
	}
	
	#remove all empty elements
	my @removed;
	for ( my $index = $#clArray; $index >= 0; --$index ){ 
		if( $clArray[$index] ne ""){ 
	 		push @removed, $clArray[$index];
	 		 
	 	}
	}
	 	
	return @removed;
		
}

1;

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
