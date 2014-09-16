package QCMG::RNA_Mate::UCSC_junction;

use strict;
use Parallel::ForkManager;
use tools_RNA;

sub new{
        my ($class, $arg)= @_;
        my $self = {
	 config => $arg->{'config'},
	 outdir => $arg->{"output_dir"},
	 exp_name => $arg->{"exp_name"},
	 exp_strand =>$arg->{"expect_strand"}

	};


        bless $self, $class;	
        return $self;   
}
my $self ={};
my $toolobj;

sub main{
	$self = shift;
	$toolobj = tools_RNA->new($self->{'config'} );	
	$toolobj->Log_PROCESS("Creating BED file for junction mapping!\n");

	#create BED file
	my $p_f = $self->{'outdir'} . "$self->{'exp_name'}.positive.junction";
	my $comm = "cat $self->{'outdir'}" . "$self->{'exp_name'}.junction*.SIM.positiveID > $p_f";
	system($comm);
	&create_BED($p_f);
		
	my $n_f = $self->{'outdir'} . "$self->{'exp_name'}.negative.junction";
	$comm = "cat $self->{'outdir'}" . "$self->{'exp_name'}.junction*.SIM.negativeID > $n_f";
	system($comm);
	&create_BED($n_f);

	#change BED file name
	if($self->{exp_strand} eq "+"){
		my $old = $self->{'outdir'} . "$self->{'exp_name'}.positive.junction.BED";
		my $new = $self->{'outdir'} . "$self->{'exp_name'}.expect.junction.BED";
		rename($old, $new);
		$old = $self->{'outdir'} . "$self->{'exp_name'}.negative.junction.BED";
		$new = $self->{'outdir'} . "$self->{'exp_name'}.unexpect.junction.BED";
		rename($old, $new);	
	}
	elsif($self->{exp_strand} eq "-"){
		my $old = $self->{'outdir'} . "$self->{'exp_name'}.negative.junction.BED";
		my $new = $self->{'outdir'} . "$self->{'exp_name'}.expect.junction.BED";
		rename($old, $new);
		$old = $self->{'outdir'} . "$self->{'exp_name'}.positive.junction.BED";
		$new = $self->{'outdir'} . "$self->{'exp_name'}.unexpect.junction.BED";
		rename($old, $new);	
	}

	#check log file
	my $rc = $toolobj->check_died();
	if($rc < 0){ return -1 }
	
	$toolobj->Log_SUCCESS("Created junction BED files\n $p_f.BED\n $n_f.BED\n");
	
	return 1;
}
1;

sub create_BED{
	my $f = shift;
   eval{
	my %hits = ();
	#count each junction id's frequency
	open(ID,$f) or $toolobj->Log_DIED("can't open file: $f") ;
	while(my $line = <ID>){
		chomp($line);
		my ($tagid,$start,$end,$mismatch,$junc_id,$j_start,$j_end) = split(/\t/,$line);
		$hits{$junc_id} ++;	

	}
	close(ID);
	
	#create BED file
	my $output = "$f.BED";
	open(BED,">$output") or $toolobj->Log_DIED("can't open file: $output");

	#add head line here
	print BED "track name=\"[$self->{'exp_name'}] junctions\" description=\"[$self->{'exp_name'}] SiM matches to junctions\" \n";	
	
	#write this junction id's information into BED file
	foreach my $id (keys %hits){
		my @juncs = split(/\_/, $id);
		my $strand = pop(@juncs); #last element is strand
		my $end = pop(@juncs);  #the second last element is junction end position
		my $start = pop(@juncs); #the third last lelement is junction start position
		my $chr = join("_",@juncs); #the rest are chromosome name
		$start -= 10;
		$end += 10;
		my $blockStart2 = $end - $start - 10;
		print BED "$chr\t$start\t$end\t$id\t$hits{$id}\t$strand\t$start\t$end\t0\t2\t10,10\t0,$blockStart2\n";
	}
	close(BED);
   };
   if($@){$toolobj->Log_Warning($@->getErrorMessage())}	
}

