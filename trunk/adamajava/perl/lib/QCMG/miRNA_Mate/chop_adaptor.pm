package QCMG::miRNA_Mate::chop_adaptor;
use strict;
use QCMG::miRNA_Mate::tools_miRNA;
use Parallel::ForkManager;

sub new{

        my ($class, $arg )= @_;
	
	# get needed parameter value which is stroed in the configure file
        my $self = {
		config => $arg->{'config'},
		exp_name => $arg->{"exp_name"},
		outdir => $arg->{"output_dir"},
		Adaptor_start => $arg->{"Adaptor_start"},
		Adaptor_start_mismatch => $arg->{"Adaptor_start_mismatch"},
		max_adaptor_mismatch => $arg->{"max_adaptor_mismatch"},
		raw_tag_length => $arg->{"raw_tag_length"},
		max_miRNA_length => $arg->{"max_miRNA_length"},
		Adaptor => $arg->{'selected_adaptor'},
 	};

        bless $self, $class;	
        return $self;
} 

#goble valarible
my $self = {};
# an instance of tools_miRNA module
my $toolobj;
sub main{
	$self = shift;
	my $BCi = shift;
	$toolobj = QCMG::miRNA_Mate::tools_miRNA->new($self->{'config'});
	$toolobj->Log_PROCESS("chopping adaptor at the tail of each tag (Adaptor_$BCi)\n");
        my $input = "$self->{'outdir'}$self->{'exp_name'}.$BCi.out";

	#get all strings which mismatch<=1, when map to adaptor start string
	my %start_mismatch1 = &mis_regu_string($self->{'Adaptor_start'}, $self->{'Adaptor_start_mismatch'});
	
	#get all input and output file handles
	#outputs are 30 files the sequence length from 0 to 29, such as, exp_name.BC1.NT29
	my %f_handle = ();
	open(IN, $input) or $toolobj->Log_DIED("Can't open input: $input\n");
	for(my $j = 0; $j <= $self->{max_miRNA_length}; $j ++){
		my $output = "$self->{'outdir'}$self->{'exp_name'}.$BCi.NT$j";
		open(my $out, ">$output") or $toolobj->DIED_log("Can't open output: $output\n");
		$f_handle{"OUT$j"} = $out;						
	}
	#output for the tags in which the adaport sequence can't be found
	my $output = "$self->{'outdir'}$self->{'exp_name'}.$BCi.NT$self->{'raw_tag_length'}";
	open(my $out, ">$output") or $toolobj->DIED_log("Can't open output: $output\n");
	$f_handle{"OUT$self->{'raw_tag_length'}"} = $out;						

	while(my $tagid = <IN>){
		if($tagid =~ />/){
			my $seq = <IN>;		
			chomp($tagid);
			chomp($seq);	
	        	my ($best_pos,$best_mis) = &chop_for_BCi($tagid, $seq, \%start_mismatch1);
			#chop tag at at the best_pos position and classify it into it's output
			if($best_pos > 0 ){
				my $miRNA_sequ = substr($seq, 0, $best_pos );
				my $nt = length($miRNA_sequ)-1;
				my $out = $f_handle{"OUT$nt"};
				print $out "$tagid\t$best_pos\t$best_mis\n$miRNA_sequ\n";
			}
			#throw the tag in which the adaptor sequence can't be detected into an unchoped output
			else{
				my $nt = $self->{'raw_tag_length'};
				my $out = $f_handle{"OUT$nt"};
				print $out "$tagid\n$seq\n";
			}	
		}
	}

	#close all input and output
	close(IN);
	foreach my $key (keys %f_handle){ close($f_handle{$key}) }
		
        if( $toolobj->check_died() < 0){ return -1 }
	$toolobj->Log_SUCCESS("choped adaptor for all tags in $BCi\n");
	
	return 1;
}
1;
sub chop_for_BCi{
	my ($tagid, $seq,  $start_mismatch) = @_;

	my @ada_array = split(//,$self->{'Adaptor'});

	#search all adaptor start string
	my %adaptor_offset = ();
	foreach my $key ( keys %$start_mismatch ){
		while($seq =~ m/$key/g ){ 	
			#this position is the next nt order in the seq after the $key NTs
			my $pos = pos($seq);
			#modify the position for searching next $key 
			pos($seq ) = $pos - length($self->{'Adaptor_start'}) + 1; 
			$adaptor_offset{$pos} = $start_mismatch->{$key};			
		}
	}

	my $best_pos = -1;
	my $best_mis = $self->{'max_adaptor_mismatch'};
	my @seq_array = split(//,$seq);
	foreach my $key ( keys %adaptor_offset ){
		#count mismatch value
		my $mis = $adaptor_offset{$key};
		for(my $i = $key, my $j = length($self->{'Adaptor_start'}); $i <= $self->{'raw_tag_length'}; $i ++, $j ++){
			if($seq_array[$i] !~ $ada_array[$j]){	$mis ++	}
		}
		#search smaller mismatched position
		if($mis < $best_mis){ $best_pos = $key; $best_mis = $mis}
		#throw the position with same mismatch value except the first found position
		elsif(( $mis == $best_mis) && (($best_pos == -1) || ($best_pos > $key)) ){$best_pos = $key}
	}
	
	#chop the found adaptor sequence, which is offset postion - length(adaptor start string) -1(base)
	#eg. adaptor_start_string: "33020", tag: T...1233020, we chop "233020" rather than "33020".
	$best_pos -= length($self->{'Adaptor_start'}) + 1;
	return ($best_pos,$best_mis);
}
sub mis_regu_string{

	my ($ref_string, $max_mismatch) = @_;

	my %adaptor_starts = ();
	#add string and mismatch value 0 into hash table
	if($max_mismatch >= 0 ){ 	$adaptor_starts{ $ref_string } = 0 }
	
	#we add all mismatch=1 start string to the hash table
	if( $max_mismatch >= 1){
		my @start = split(//,$ref_string);	
		my $l_start = @start;
		for(my $i = 0; $i < $l_start; $i ++){
			my $s ="[0123]";
			#delete it's own color value, since in own string  mismatch=0
			$s =~ s/$start[$i]//;
			my $str = $ref_string;
			#replace one color value to $s 
			substr($str, $i, 1, $s);
			#mismatch value is 1
			$adaptor_starts{ $str } = 1;
		}
	}

	#add all mismatch=2 string to the hash table
	if( $max_mismatch >= 2){
		my @start = split(//,$ref_string);	
		my $l_start = @start;
		
		#delete it's own color value from 0,1,2,3
		for(my $i = 0; $i < $l_start; $i ++){
			my $s1 ="[0123]";
			$s1 =~ s/$start[$i]//;
			for(my $j = 0; $j < $l_start; $j ++){
				if($i == $j) {next}
				my $str = $ref_string;
				#replace the first value to a mismatched value, that the [0123] delete it's own value
				substr($str,$i, 1,$s1);
				my $s2 ="[0123]";
				$s2 =~ s/$start[$j]//;
				#replace the second value to mismatched value
				if($i < $j){	substr($str, $j+5,1,$s2)}
				else{ substr($str, $j,1, $s2)  }
				#set the mismatch value --  2
				$adaptor_starts{$str} = 2;
			}
		}	
	} 

	return %adaptor_starts;
}


=head1 NAME

chop_adaptor  - one of the  miRNA pipeline perl modules for Grimmond Group

=head1 SYNOPSIS

  use chop_adaptor;
  my %argv = &init();
  $argv{'selected_adaptor'} = "33020103031311231200032032222031220201003000312";
  my $obj = QCMG::miRNA_Mate::chop_adaptor->new(\%argv);
  my $BC = "BC1";
  $obj->main($BC);

=head1 DESCRIPTION

This module check each tag whether it contain adaptor sequence and then chop it from tail of the tag. In this way it make sure the left tag sequence is real miRNA sequence. The chopped tag with orginal tagid will be classify to outputs based on different tag length.
eg. in a tag "T2122...121133020103", the last eight mers "33020103" are belong to adaptor sequence. This module will chop last nine (8+1) mers from this tag.

        use chop_adaptor;

        my $argv = {
                config => "example.conf",
                exp_name => "tag_20000",
                outdir => "/data/",
                Adaptor_start => "33020",
                Adaptor_start_mismatch => 1,
                max_adaptor_mismatch => 5,
                raw_tag_length => 35,
                max_miRNA_length => 29,
                Adaptor => "33020103031311231200032032222031220201003000312"
        };


        my $obj = QCMG::miRNA_Mate::chop_adaptor->new($argv);
	my $BC = "barcode_name1";
	$obj->main($BC);



=head2 Methods

=over 1

=item * $obj->main($BC)

This is the only function in this module, which search input file and create output file automatically. The input file name should follow the formart: "$argv->{'outdir'}$argv->{'exp_name'}.$BC.out"; the output file formart is "$argv->{'outdir'}$argv->{'exp_name'}.$BC.NT$j" ($j is chopped tag length). In above example, there are 29 outputs for tag length between 1 to 29, all non_chopped tag will be reported to the file named "tag_20000.$BC.NT35".


=back

=head1 AUTHOR

qinying Xu (Christina) (q.xu@imb.uq.edu.au)

=head1 COPYRIGHT

Copyright 2008 Grimmond Group, IMB, UQ, AU.  All rights reserved.


=head1 SEE ALSO

perl(1).

=cut

