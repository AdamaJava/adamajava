package QCMG::RNA_Mate::tag_quality;

use QCMG::RNA_Mate::tools_RNA;
use strict;

sub new{

        my ($class, %arg)= @_;
	
	# get needed parameter value which is stroed in the configure file
        my $self = {
		config => $arg{'config'},
 		l_max => $arg{"raw_tag_length"},
		tag_length => $arg{"tag_length"},
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
# an instance of tools_RNA module
my $toolobj;

sub main{
	$self = shift;
	$toolobj = tools_RNA->new($self->{'config'});
	$toolobj->Log_PROCESS("Checking tag quality\n");
	
	#open input files
	open(CSFASTA, $self->{f_file}) or $toolobj->Log_DIED("Can't open $self->{f_file}\n");
	open(QUALITY, $self->{q_file}) or $toolobj->Log_DIED("Can't open $self->{q_file}\n");

	#create files for qualitfied tags which is classified with tag length
	my @bf_lengths = split(/,/,$self->{tag_length});
	my @lengths = split(/\,/,$self->{'tag_length'});
	@lengths = sort{$a <=> $b} @lengths;
	
	# store all output files handle
	my %output = ();
	foreach my $i (@lengths){
		my $fname = $self->{outdir} . "$self->{exp_name}.mers$i.unique.csfasta";	
		open(my $fhandle,">$fname") or $toolobj->Log_DIED("$fname\n");
		my $fid = "mers".$i;
		$output{$fid} = $fhandle;
	}	
	#open output files
	my $fname = $self->{outdir} . "$self->{exp_name}.poor_qulity.csfasta";
	open(my $poor,">$fname") or $toolobj->Log_DIED("can't open file: $fname");
	$output{'poor'} = $poor;

	# check quality
	&check_quality(\%output, \@lengths);

	#close input and outputs files
	foreach my $file (keys %output){ close $output{$file}	}
	close(CSFASTA);
	close(QUALITY);

	#check the log file
        my $rc = $toolobj->check_died();
        if($rc < 0){ return -1 }

	#add success information into log file
	$toolobj->Log_SUCCESS("Created csfasta file for different tag length, in which tag quality is checked!\n");
}
1;


sub check_quality{
    my ($output, $lengths) = @_;
    my @mymask =split(// ,$self->{mask});

    # read the raw data
    while(my $qual_line = <QUALITY>){
	my $sequ_line = <CSFASTA>; 
	if($qual_line =~ />/ ){
		chomp($qual_line);
		chomp($sequ_line);
		
		#if the quality file and sequence files are in different order, stop the whole system
		if($qual_line ne $sequ_line){  $toolobj->Log_DIED("The input CSFASTA file and QUALITY file don't use same format!\n")	}
		
		#assign the tag's new id		
		my $tagid = "$qual_line";

		#read tag's quality value and sequence
		$qual_line = <QUALITY>;
		$sequ_line = <CSFASTA>;
		chomp($qual_line);
		chomp($sequ_line);
		my @scores = split(/\s/, $qual_line);
		my $crap = 0;
		my $start = 0;

		#check tag quality value from shortest allowed length
		foreach my $i (@$lengths){
		    for (my $j = $start; $j < $i; $j ++){ if(($mymask[$j] == 1) && ($scores[$j] < 10) ){  $crap ++ }  }
         	    #when there are more than 5 color space's color intensity is lower than 10
		    #chop the tag last few color space value or throw it into poor quality file	
		    if($crap > 4) { 
			# if there are more than 4 low color value during sequence range [0,shortest tag allowed length], 
			# we treat this tag as poor quality tag
			if($start == 0){
				 my $f = $output->{'poor'};
				 print $f "$tagid\n$sequ_line\n";
				 last; 
			}
			# if more than four low color value happed during sequence range [0,current tag length], 
			# chop tag at last tag length.
			# eg. if $crap = 6 during [0,30], we chop tag at the 25th color space. that means this tag length is 25. 
			else{
				$sequ_line = substr($sequ_line,0,$start+1);
				#get file handle which is for qulified tag with length is $start
				my $f = $output->{"mers$start"};
				print $f "$tagid\n$sequ_line\n";
				last;
			}
 		    }
             	    $start = $i;	
		}
	
		# when the total low color value is less than 5, we keep this tag's full sequence
		if($crap <= 4 ){ my $f = $output->{"mers$start"}; print $f "$tagid\n$sequ_line\n"  }
	    }
   	}
}

