package QCMG::miRNA_Mate::split_BC;
use strict;
use QCMG::miRNA_Mate::tools_miRNA;

sub new{

        my ($class, $arg)= @_;
	
	# get needed parameter value which is stroed in the configure file
        my $self = {
		config => $arg->{'config'},
		exp_name => $arg->{"exp_name"},
		outdir => $arg->{"output_dir"},
		name_BC => $arg->{"name_BC"}
	};
	
	#get all barcode strings from config files. all barcode with same length
	my @BCs = split(/\,/,$arg->{'name_BC'});

	my $tool = QCMG::miRNA_Mate::tools_miRNA->new($self->{'config'});
	foreach $b (@BCs){  
		$self->{$b} = $arg->{$b};	
		if( length($self->{$b}) != length($arg->{$BCs[0]}) ){ $tool->Log_DIED("the length of barcodes are differnt!\n")}
	}
	$self->{'BC_length'} = length($arg->{$BCs[0]}) - 1;
	$self->{'num_BC'} = scalar(@BCs);

        bless $self, $class;	
        return $self;
} 

#goble valarible
# for module self varable
my $self = {};
# an instance of tools_mapping module
my $toolobj;
sub main{
	($self, my  $f_F3, my $f_R3) = @_;
	
	$toolobj = QCMG::miRNA_Mate::tools_miRNA->new($self->{'config'});

	#before start this job, check whether the system should be died since error in log file
	if($toolobj->check_died < 0){ return -1 }

	$toolobj->Log_PROCESS("starting to split tags into different .BCi file based on the barcode\n");
		
	#read R3 file and classify tag's barcode class
	my $f_classify = "$self->{'outdir'}$self->{'exp_name'}.BC_classify";
	&classify_BC($f_R3, $f_classify);

	#split tags from F3 file into BC?.out based on the barcode
	&classify_F3($f_F3, $f_classify);

	#check the log file
        my $rc = $toolobj->check_died();
        if($rc < 0){ return -1 }

	#add success information into log file
	$toolobj->Log_SUCCESS("Splited tag into .BCi file based on the barcode!\n");
}
1;


sub classify_F3{
	#both F3 and R3 files must be sorted in order of panel_x_y before this module will work

	my ($f_F3, $f_classify ) = @_;
	
	#open all of the final output files only once;
	#create files for outputs in which all tags are classified by the barcode
	# store all output files handle
	my %output = ();
	#for(my $i = 1; $i <= $self->{'num_BC'}; $i ++){
	my @BCs = split(/,/, $self->{'name_BC'});
	foreach my $BC (@BCs){
		my $fname = $self->{outdir} . "$self->{exp_name}.$BC.out";	
		open(my $fhandle,">$fname") or $toolobj->myLog("[DIED]: $fname\n");
		$output{$BC} = $fhandle;
	}	

	#since R3 file in ordered by the panel position, the first three column in $f_BC is on order as well
	#system("sort  -k 1,1n -k 2,2n -k 3,3n $f_BC > $f_BC.sorted");
	
	open(F3, $f_F3) or $toolobj->Log_DIED("can't opent F3 file --$f_F3 in split_BC::classify_F3");
	
	#read the first selected barcode information from $f_classify
        open(CLA, $f_classify) or $toolobj->Log_DIED("Can't open $f_classify\n");
        my $l_cla;
        do{     $l_cla = <CLA>  }while($l_cla =~ /#/);
	#end open inserted loop

		while(my $F3_id = <F3>){
		    #if end of BC file then exit this loop
	#	    if($l_BC eq eof()){last}
	
		    if($F3_id !~ /^>/){ next }
		    chomp($F3_id);
		    $F3_id =~ s/>//g;
		    my ($F_pa, $F_x, $F_y, $F) = split(/\_/, $F3_id);
		    my $sequ = <F3>;
		    #in do{}while, the next and last means repear or exit the outside loop
		    do{
			chomp($l_cla);
			my ($B_pa, $B_x, $B_y, $BCi) = split(/\t/, $l_cla);
			#if find same location in both files, report this tag based on it barcode belongs
			if(($B_pa == $F_pa) && ($B_x == $F_x) && ($B_y == $F_y)){
				my $f_handle = $output{$BCi};
				print $f_handle ">$F3_id\n$sequ";	
				#read next line from the BC file
				#if end of BC file then exit this function
				if( $l_cla = <CLA>){ next  }
				else{ last };
			}
			#if the location in BC file is biger than F3 tag, read next F3 tag
			elsif($B_pa > $F_pa){  next }
			elsif(($B_pa == $F_pa) && ($B_x > $F_x) ){ next }
			elsif(($B_pa == $F_pa) && ($B_x == $F_x) && ($B_y > $F_y) ){ next }
		    #if the loaction in BC is smaller than F3 tag, read next BC 
		    }while($l_cla = <CLA>);
		    #exit the while loop when <BC> is ended
		    last;
		}

	#close inserted loop
		close(F3);
		close(CLA);

	#close outputs files
	foreach my $file (keys %output){ close $output{$file}	}
} 

sub classify_BC{

	my ($f_BC, $f_out) = @_;
	

	#read all possible 1 mismatch BC string into a hash table
	#there are total num_BC * BC_length * 4 (0,1,2,3) posibilities
	my %BC = ();
	my @color = ("0","1","2","3");
	my @BCs = split(/,/, $self->{'name_BC'});
	foreach my $BC (@BCs){
		for(my $j = 1; $j <= $self->{'BC_length'}; $j ++){
			foreach my $c (@color){
				my $BCi = $self->{$BC};
				#replace one char with other color value
				substr($BCi, $j, 1) = $c;
				#this one mismatched BC bleng to that BC calss as well
				$BC{$BCi} = $BC;
			}
		}
	}
	
	open(BC, $f_BC) or $toolobj->Log_DIED("Can't open $f_BC\n");
	open(OUT,">$f_out") or $toolobj->Log_DIED("Can't open $f_out");
	print OUT "#panel\t _X\t _Y\t barcode\n";

	while(my $BC_id = <BC>){
		if($BC_id !~ />/){ next	}
		$BC_id =~ s/>//g;
		my $s = <BC>;
		chomp($s);
		#if this BC string belongs to the one mismathed hash table -- %BC
		#split the information and then write it to output
		if(exists $BC{$s}){
			my ($pa, $x, $y, $r) = split(/\_/, $BC_id);
			print OUT "$pa\t$x\t$y\t$BC{$s}\n";
		}
	}

	close(OUT);
	close(BC);
}


=head1 NAME

split_BC  - one of the  miRNA pipeline perl modules for Grimmond Group

=head1 SYNOPSIS

  use split_BC;
  my %argv = &init();
  my $obj = QCMG::miRNA_Mate::split_BC->new(\%argv);
  $obj->main($f_F3,$f_R3);

=head1 DESCRIPTION

This module classify the tags from raw data file to several output files based on the differnt barcode. There are two main steps in this module. step1: first it use a hash table to store all possible barcode sequence allowed a certain number of error base and its belonging barcode name; then point out barcode name for each tag according to its barcode sequence in R3 file and the hash table; it split tag id to panel and x,y coordinats, report them with barcode name to interal output file and sortthe file as well; finally, it classify the tags from raw data file to the right output file based on the internal output file.


        use split_BC;

        my $argv = {
                config => "/data/example.conf",
                exp_name => "tag_20000",
                outdir => "/data/",
                name_BC => "BC1,BC2"
        };


        my $obj = QCMG::miRNA_Mate::split_BC->new($argv);
	my $f_F3 = "/data/tag_20000_F3.csfasta";
	my $f_R3 = "/data/tag_20000_R3.csfasta";
        $obj->main($f_F3,$f_R3);



=head2 Methods

=over 1

=item * $obj->main($f_F3,$f_R3)

In above example the output file will be "/data/tag_2000_F3.BC1.out" and "/data/tag_2000_F3.BC2.out".


=back

=head1 AUTHOR

qinying Xu (Christina) (q.xu@imb.uq.edu.au)

=head1 COPYRIGHT

Copyright 2008 Grimmond Group, IMB, UQ, AU.  All rights reserved.


=head1 SEE ALSO

perl(1).

=cut

