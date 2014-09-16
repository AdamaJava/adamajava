package QCMG::miRNA_Mate::miRNA_freq;
use strict;
use Parallel::ForkManager;
use QCMG::miRNA_Mate::tools_miRNA;

sub new{
        my ($class, $arg)= @_;
        my $self = {
	 config => $arg->{'config'},
	 library_index => $arg->{"library_index"},
	 miRNA_start_offset => $arg->{'miRNA_start_offset'},
	 miRNA_end_offset => $arg->{'miRNA_end_offset'}
	};

        bless $self, $class;	
        return $self;   
}
my $toolobj;
sub main{
	my ($self,$f_single,$f_out_id,$f_out_freq) = @_;

	$toolobj = QCMG::miRNA_Mate::tools_miRNA->new($self->{'config'} );	
	$toolobj->Log_PROCESS("Counting miRNA ID for mapped tags!\n");

	#sort each SIM start position and  output to "$f_single.start_end.sorted"
	my $f_start_end_sort = &sort_start_end($f_single);

	#get the miRNA ID based on the SIM sorted start position
	#it will report all miRNA ID for mapped tags to output -- "$f_single.sorted.ID"
	&search_ID($self,$f_start_end_sort,$f_out_id );

	#count frequency for both positive miRNA and negative miRNA
	&count_freq($f_out_id, $f_out_freq);

	#check log file
	if($toolobj->check_died() < 0){ return -1 }
	
	$toolobj->Log_SUCCESS("Counted all miRNA ID's frequency for mapped tags\n");
	
	#delete some internal output files

	return 1;
}
1;
sub count_freq{

        my ($input, $output) = @_;

        my %freq_id = ();
        #read all output file from sub search_ID; count the miRNA frequency
        my $f = $input;
        open(FF, $f) or $toolobj->Log_DIED("can't open input file: $f\n");
        while( <FF>){   my ($tag,$miRNA,$mi_start,$mi_lenght) = split(/\t/); $freq_id{$miRNA} ++ }
        close(FF);

	#delete all tag marked "noid" since it mapped crossing two adjacant 
	delete($freq_id{"noid"});

	#report the count miRNA frequency to output
        open(OUT, ">$output") or $toolobj->Log_DIED("can't open output: $output\n");
        foreach my $key (keys %freq_id){print OUT "$key\t$freq_id{$key}\n"}
        close(OUT);

}


sub sort_start_end{
	my $input = shift;

        #count each single selected postion's start and end
        open(IN, $input) or $toolobj->Log_DIED("can't open $input file on sub sort_start_end");
        open(OUT, ">$input.start_end") or $toolobj->Log_DIED("can't creat file $input.start_end\n");
        while(my $tag = <IN>){
                chomp($tag);
                if($tag =~ m/^>/){
                        my ($tagid, $selected) = split(/\,/,$tag);
			$tagid =~ s/\>//g;
                        my ($pla,$mis) = split(/\./,$selected);
                        my $seq = <IN>;
                        chomp($seq);
                        my $l = length($seq);
			my ($start, $end);
                        if($pla < 0){ $end = abs($pla); $start = $end - $l + 1 }
                        else{ $start = $pla; $end = $start + $l - 1 }
                        print OUT "$tagid\t$start\t$end\n";
                }
        }
        close(IN);
        close(OUT);
        
	#sort input file by matched position and tagid
        system("sort -k 2,2n -k 1,1 $input.start_end > $input.start_end.sorted");
	unlink("$input.start_end");

	return "$input.start_end.sorted";

}
sub search_ID{
        my ($self,$input,$output) = @_;
	
        # read all junction id into array
        open(INDEX,$self->{'library_index'}) or $toolobj->Log_DIED("can't open index file: $self->{'library_index'}");
        my @all_lib_id = <INDEX>;
        close(INDEX);

        open(SORTED,"$input") or $toolobj->Log_DIED("can't open SIM start sorted file: $input");
        open(OUT,">$output")  or $toolobj->Log_DIED("can't create writable file: $output");
        my $i_search = 0;
        my $i_end = @all_lib_id;
        while(my $line = <SORTED>){
                chomp($line);
                my ($tagid,$tag_start,$tag_end,$mismatch) = split(/\t/,$line);
                #if we can't find miRNA id,this tag will be assign "...\tnoid\t$tagid\n"
                my ($found_id,$id_start,$id_length) =("noid",-1,-1);
                for(my $i = $i_search; $i < $i_end; $i ++ ){
                        chomp($all_lib_id[$i]);
                        my($mi_id,$mi_start,$mi_end) = split(/\t/,$all_lib_id[$i]);
                        if(($tag_start >= $mi_start) &&($tag_end <= $mi_end)){
				$found_id = $mi_id;
				$id_start = $mi_start + $self->{'miRNA_start_offset'};
				$id_length = $mi_end - $mi_start - $self->{'miRNA_start_offset'} - $self->{'miRNA_end_offset'} + 1;
				# the $i_search will start from last found junction id position
                                $i_search = $i;
                                #go to next tag on the sorted file
                                last;
                        }
                }
		#delete the '>' from the tagid
#		$tagid =~ s/\>//g;
		print OUT "$tagid\t$found_id\t$id_start\t$id_length\n";
        }
        close(SORTED);
        close(OUT);

}


=head1 NAME

miRNA_freq  - one of the  miRNA detection perl modules for Grimmond Group

=head1 SYNOPSIS

  use miRNA_freq;
  $obj->main($input,$output1,$output2);

=head1 DESCRIPTION

This module retrieve the junciton id according the miRNA library index and single selected file, the related tagid, miRNA id and miRNA information are reported to the the first output file. it reports the frequency of each miRNA id to the second output.

        use miRNA_mapping;

        my %argv = (

	        miRNA_start_offset => 10,
	        miRNA_end_offset => 10,
                config => "/data/example.conf",
                library_index => "/data/miRbase_all_12.0.index", 
        );

  	my $obj = QCMG::miRNA_Mate::miRNA_freq->new(%argv);
	my $input = "/data/tag_20000.BC1.once.SIM.positive";
	my $output1 = "/data/tag_20000.BC1.once.SIM.positive.sorted.ID";
	my $output2 = "/data/tag_20000.BC1.once.SIM.positive.freq";
	$obj->main($input,$output1,$output2);

=head2 Methods

=over 2

=item * $obj->main($input, $ouput1,$output2);

you can nominate output file name with full path but the input file must be the single seleted file. 

=back

=head1 AUTHOR

qinying Xu (Christina) (q.xu@imb.uq.edu.au)

=head1 COPYRIGHT

Copyright 2009 Grimmond Group, IMB, UQ, AU.  All rights reserved.


=head1 SEE ALSO

perl(1).

=cut


