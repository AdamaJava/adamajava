#!/usr/bin/perl 

package tag_quality_filter;
use strict;
use tools_SNPs;

sub new{

        my ($class, %arg)= @_;
	
	# get needed parameter value which is stroed in the configure file
        my $self = {
		config => $arg{'config'},
		tag_length => $arg{"full_tag_length"},
		exp_name => $arg{"exp_name"},
		q_file => $arg{"raw_qual"},
		f_file => $arg{"raw_csfasta"},
		mask => $arg{"mask"},
		outdir => $arg{"output_dir"},
 	};
        bless $self, $class;	
        return $self;
} 

#goble valarible
my $self = {};
# store all output files handle
my %output = ();
# an instance of tools_mapping module
my $toolobj;
sub main{
	$self = shift;
	$toolobj = tools_SNPs->new($self->{'config'});
	
	#open input files
	open(CSFASTA, $self->{f_file}) or $toolobj->myLog("[DIED]: Can't open $self->{f_file}\n");
	open(QUALITY, $self->{q_file}) or $toolobj->myLog( "[DIED]: Can't open $self->{q_file}\n");

	#open output files
	my $fname = $self->{outdir} . "$self->{exp_name}.poor_qulity.csfasta";
	open(POOR,">$fname") or $toolobj->myLog("[DIED]: can't open file: $fname");

	#create files for qualitfied tags which is full tag length
	$fname = $self->{outdir} . "$self->{exp_name}.mers$self->{'tag_length'}.unique.csfasta";	
	&check_quality( $fname );

	#close input and outputs files
	foreach my $file (keys %output){ close $output{$file}	}
	close(CSFASTA);
	close(QUALITY);
	close(POOR);

	
	#check the log file
        if( $toolobj->check_died() < 0){ return -1 }
	else{ 
		#add success information into log file
		$toolobj->myLog("[SUCCESS]: Created csfasta file for different tag length, in which tag quality is checked!\n");
		return 1;
	 }

}
1;


sub check_quality{
    my $f_good_tag = shift;
    open(GOOD,">$f_good_tag") or $toolobj->myLog("[DIED]: can't open file: $f_good_tag");
    
    my @mymask =split(// ,$self->{mask});
    my $tag_length = $self->{tag_length};

    # read the raw data
    while(my $qual_line = <QUALITY>){
	my $sequ_line = <CSFASTA>; 
	if($qual_line =~ /^>/ ){
		chomp($qual_line);
		chomp($sequ_line);
		
		#if the quality file and sequence files are in different order, stop the whole system
		if($qual_line ne $sequ_line){  $toolobj->myLog("[DIED]:  The input CSFASTA file and QUALITY file don't use same format!\n")	}
		
		#assign the tag's new id		
		my $tagid = "$qual_line:1";

		#read tag's quality value and sequence
		$qual_line = <QUALITY>;
		$sequ_line = <CSFASTA>;
		chomp($qual_line);
		chomp($sequ_line);
		my @scores = split(/\s/, $qual_line);
		my $crap = 0;
		my $start = 0;

		#check tag quality value 
		for (my $j = 0; $j < $tag_length; $j ++){ 
			if(($mymask[$j] == 1) && ($scores[$j] < 10) ){  $crap ++ } 
		}
         	#when there are more than 5 color space's color intensity is lower than 10
		if($crap > 4) {	 print POOR "$tagid\n$sequ_line\n" }
		else{	print GOOD "$tagid\n$sequ_line\n"	}             	 	
	  }
  	}
	close(GOOD);
}

=head1 NAME

tag_quality_filter  - one of the  SNPs prediction perl modules for Grimmond Group

=head1 SYNOPSIS

  use tag_quality_filter;
  my $obj = err_profit_filter->new(%argv);
  $obj->main;

=head1 DESCRIPTION

This module check check tag's quality in raw data file and output qulified tag with new tag id. There are only two output files, one for qualified tag and another for non-qualified tag and all tags are full tag length. 


=head2 Methods

=over 1

=item * $obj->main()


=back

=head1 AUTHOR

qinying Xu (Christina) (q.xu@imb.uq.edu.au)

=head1 COPYRIGHT

Copyright 2008 Grimmond Group, IMB, UQ, AU.  All rights reserved.


=head1 SEE ALSO

perl(1).

=cut

