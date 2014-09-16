package QCMG::miRNA_Mate::miRNA_summary;
use strict;
use Parallel::ForkManager;
use QCMG::miRNA_Mate::tools_miRNA;

sub new{
        my ($class, $arg)= @_;
        my $self = {
	 config => $arg->{'config'},
	};

        bless $self, $class;	
        return $self;   
}
my $toolobj;
sub main{
	my ($self,$f_bc, $f_sort_id) = @_;

	$toolobj = QCMG::miRNA_Mate::tools_miRNA->new($self->{'config'});
	$toolobj->Log_PROCESS("starting summary mapped tag!\n");

	
	#compare each tag with mapped miRNA which is in the library file, result into .PreSummary file
	#output file format: miRNA_name tagid canonical isomir diff_length diff_start diff_seq
	my $output = "$f_bc.PreSummary";
	$output =~ s/\.bc//g;
	unless(-e "$f_bc.sorted" ){system( "sort -k 6,6n -k 1,1 $f_bc > $f_bc.sorted")};
	&summary_tag("$f_bc.sorted",$f_sort_id, $output);	

	#sum all row's value into one line when the miRNA_name are identical
	my $input = $output;
	$output =~ s/PreSummary/summary/g;
	&summary_miRNA($input, $output);
	
	$toolobj->Log_SUCCESS("finished summary -- (see output: $output)!\n");

	return 1;
}
1;

sub summary_miRNA{
	my ($input, $output) = @_;
	
	#sort the input file by miRNA id
	system("sort -k 1,1 $input > $input.sorted");

	open(IN, "$input.sorted") or $toolobj->Log_DIED("can't open input file $input -- miRNA_summary::summary_miRNA\n");
	open(OUT, ">$output") or $toolobj->Log_DIED("can't open output file $output -- miRNA_summary::summary_miRNA\n");
	#print the comments on the first line of ouput
	print OUT "#miRNA\tTOTAL\tCANONICAL\tISOMIR\tdiff_length\tdiff_start\tdiff_sequence\n";
	
	#read the first line from input
	my $line = <IN>;
	chomp($line);
	my ($miRNA, $tagid, $canoni, $isomir, $diff_length, $diff_start, $diff_seq) = split(/\t/, $line);
	my $total = 1;
	while($line = <IN>){
		chomp($line);
		my @li_in = split(/\t/,$line);
		#for all line with same miRNA id
		if($li_in[0] eq $miRNA){
			$total ++;
			$canoni += $li_in[2];
			$isomir += $li_in[3];
			$diff_length += $li_in[4];
			$diff_start += $li_in[5];
			$diff_seq += $li_in[6];
		}
		else{
			#print the summary for previous miRNA to output
			print OUT "$miRNA\t$total\t$canoni\t$isomir\t$diff_length\t$diff_start\t$diff_seq\n";
			#save current line imformation
			($miRNA, $tagid, $canoni, $isomir, $diff_length, $diff_start, $diff_seq) = split(/\t/, $line);
			$total = 1;
		}
	}
	#print the last miRNA summary
	print OUT "$miRNA\t$total\t$canoni\t$isomir\t$diff_length\t$diff_start\t$diff_seq\n";
	
	close(IN);
	close(OUT);

}

sub summary_tag{
	my ($f_bc_sorted, $f_sort_id, $f_out) = @_;

	open(BC, $f_bc_sorted) or $toolobj->Log_DIED("can't open input file: $f_bc_sorted in miRNA_summary::summary_tag\n");
	open(ID, $f_sort_id) or $toolobj->Log_DIED("can't open input file: $f_sort_id in miRNA_summary::summary_tag\n");
	open(OUT, ">$f_out") or $toolobj->Log_DIED("can't open output file: $f_out in miRNA_summary::summary_tag\n");

	while(my $bc_line = <BC>){

		#jump over the comment line
		if($bc_line =~ /\#/){next}
		#throw the both input file's tag which mapped the junction of two miRNA
		my $id_line = <ID>;
		if($bc_line =~ /\./){   next   }
		
		#if both input file don't follow the same tagid order, we stop this pipeline
		chomp($bc_line);
		chomp($id_line);
		my @li_bc = split(/\t/,$bc_line);
		my @li_id = split(/\t/,$id_line);
		if($li_bc[0] ne $li_id[0]){ 
			#debug
			print "bc tag: $li_bc[0] | ID tag: $li_id[0]\n";

			$toolobj->Log_DIED("both input file in miRNA_summary don't follow the same tagid order\n");
			 last;
		}
		
		#summary the correct data and properily mapped tags
		my ($canoni, $isomir,$diff_length, $diff_start, $diff_seq) = (1, 0, 0, 0, 0);
		if( (length($li_bc[4]) - 1) != $li_id[3] ){ $diff_length = 1; $isomir = 1 }
		if($li_bc[5] != $li_id[2]){ $diff_start = 1; $isomir = 1 }
		if( &check_SNP( $li_bc[7]  ) eq "SNP" ){ $diff_seq = 1;  $isomir = 1 }
		if($isomir == 1){ $canoni = 0 }
		#print out the comparision result between tag and coresponding miRNA
		#print "miRNA_di tagid same diff diff_in_lengh diff_at_start diff_in_seq(SNP)"
		print OUT "$li_id[1]\t$li_id[0]\t$canoni\t$isomir\t$diff_length\t$diff_start\t$diff_seq\n";
	}

	close(OUT);
	close(BC);
	close(ID);
	
}

sub check_SNP{

	my $mismatches = shift;

	my @mis = split(/\,/, $mismatches);

	#mismatched number less than 2 means no SNP
	if(@mis < 2){ return "no SNP"  }

	#check adjacent
	my @adjacents = ();
        my %tmp_hash = ();
        foreach my $m (@mis){ my ($p,$s) = split(/_/, $m);  $tmp_hash{$p} = $s        }

        #sort mismatched postion
        my @posi = sort { $a <=> $b } keys %tmp_hash ;
        #check whether  each mismatched position are adjacent
        for (my $i = 0; $i < (scalar(@posi) - 1) ; $i++){
                #since the position can be minice so we look at it's abs value
                if ( (abs($posi[$i + 1] - $posi[$i] ) == 1) ) {
                        #get the adjacent mismatch's postion and color value
                        push(@adjacents, [$posi[$i],$posi[$i+1],$tmp_hash{$posi[$i]},$tmp_hash{$posi[$i+1]} ]);
                }
        }

	#check valid SNP from the adjacent position
       foreach my $adj (@adjacents){
                my ($solid_a, $ref_a) = split(//, $adj->[2] );
                my ($solid_b, $ref_b) = split(//, $adj->[3] );
                my $solid_ab = $solid_a . $solid_b;
                my $ref_ab = $ref_a . $ref_b;

                #if two adjacent mismatched color value is in same group, they are potential snp
                if( ($ref_ab =~ /00|11|22|33/) && ( $solid_ab =~ /00|11|22|33/)  ) {   return "SNP"  }
                elsif( ($ref_ab =~ /10|01|32|23/) && ( $solid_ab =~ /10|01|32|23/)  ) {return "SNP"  }
                elsif( ($ref_ab =~ /20|02|31|13/) && ( $solid_ab =~ /20|02|31|13/)  ) {return "SNP"  }
                elsif( ($ref_ab =~ /30|03|12|21/) && ( $solid_ab =~ /30|03|12|21/)  ) {return "SNP"  }
	}
	
	return "no SNP"
}



=head1 NAME

miRNA_summary  - one of the  miRNA detection perl modules for Grimmond Group

=head1 SYNOPSIS

  use miRNA_summary;
  my %argv = (conf=>"example.conf");
  my $obj = QCMG::miRNA_Mate::miRNA_summary->new(\%argv);
  $obj->main($input_bc,$input_id);

=head1 DESCRIPTION

This module summary the comparison results between the reference miRNA sequence in library and the single seleted tag sequence. if the tag sequence is differnt with reference sequence, this module will point out whether they diffe in sequence length, mapping start postion or SNP. The formart of output file is miRNA id, the number of tag whose single seleted postion is mapped on the reference sequence, the number of canonical tag, the number of isomeric tag, the number of isomeric tag with different length with reference, the number of isomeric tag with differnt start position with reference and the number of isomeric tags with SNP. 

        use miRNA_summary;
        my %argv = (config => "/data/example.conf" );
        my $obj = QCMG::miRNA_Mate::miRNA_mapping->new(%argv);

        my $input1 = "/data/tag_20000.BC1.once.SIM.positive.bc";
        my $input2 = "/data/tag_20000.BC1.once.SIM.positive.sorted.ID";
        $obj->main($input1,$input2);

=head2 Methods

=over 2

=item * $obj->main($input1,$input2);
The first input is the bc file which is created by program list on "script_editing", the second input is the sorted mapped miRNA id information file which is created by miRNA_freq module. The output will be the first input file name plus ".summary".

=back

=head1 AUTHOR

qinying Xu (Christina) (q.xu@imb.uq.edu.au)

=head1 COPYRIGHT

Copyright 2009 Grimmond Group, IMB, UQ, AU.  All rights reserved.


=head1 SEE ALSO

perl(1).

=cut
