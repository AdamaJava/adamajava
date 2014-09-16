package QCMG::RNA_Mate::tools_RNA;

use strict;

sub new{
         my ($class,$conf)= @_;
	
	 # read configure file
	 my $self = {};
         open(CONF, $conf) or die "[DIED]: can't open $conf in tools_mapping.pm\n";
         while(my $line = <CONF>){
           chomp($line);
           my ($key,$value) = split(/=/,$line);
           $self->{$key} = $value;
         }
         close(CONF);

	# get log file name here
        $self->{"log_file"} = "$self->{'output_dir'}$self->{'exp_name'}.log";

        bless $self, $class;	
        return $self;  
}

sub create_csfasta{
	#the $self is the classself
	#$f_raw is the input of original raw data which is csfasta file
	#$f_out is the output eg. S0014_20080714_1_hES_polysome_hES_membrane_F3.mers35.unique.csfasta

	my ($self,$f_raw) = @_;

	my $f_log = $self->{'log_file'};
	
	#delete all digital number start with a "."; eg. "65.12.1,50.5.0,30.3.1" will be converted to "65,50,30"
	my $maps = $self->{'mapping_parameters'};
	$maps =~ s/\.\d+//g;
	#create empty csfasta files with length tags
	my @tag_length = split(/\,/,$maps);
	my $l_max = 0;
	foreach my $l (@tag_length){	
		my $f = $self->{'output_dir'}."$self->{'exp_name'}.mers$l.unique.csfasta";
		open(FF,">$f") or  &Log($f_log,"[DIED]: can't open file $f\n ");
		close(FF);	
		if( $l > $l_max ){ $l_max = $l  }
	}

        # cp raw tag
	my $f_out = $self->{'output_dir'}."$self->{'exp_name'}.mers$l_max.unique.csfasta";
	if(system("cp $f_raw $f_out") != 0){ &Log($f_log, "[DIED]: can't copy raw data to $f_out\n") }
	#while(my $line = <RAW>){	print OUT $line 	}
	

	#add the following success information into log file	
	&Log($f_log,"[SUCCESS]: Created csfasta file for tag with different tag length, in which we ingore tag quality!\n");
	

}
sub chop_tag{
	#input is the tag didn't matched on the genome or juction
	#output is the tag choped last few mers

        my ($self, $f_nonmatched, $f_shorttag,$l_chop) = @_;
	my $f_log = $self->{'log_file'};
	
	&Log($f_log,"[PROCESS]: chopping tag \n");
        
	# open the input file in which the tag are not matched 
        open( IN, $f_nonmatched) or &Log($f_log, "[DIED]: can't open $f_nonmatched\n");
 	# open the output file in which the tag's last few mers will be choped
        open(OUT, ">>$f_shorttag") or &Log($f_log, "[DIED]: can't open $f_shorttag\n");
        while(my $line = <IN>){  
                if($line =~ />/){
			my $id = $line;
			my $sequ = <IN>;
			chomp($sequ);
                        $sequ = substr($sequ, 0, length($sequ) - $l_chop);
			print OUT $id . $sequ . "\n" ;
                }
        }
        close(IN);
	close(OUT);
}
sub check_died{
	my $self = shift;

	#read the log file
	my $f_log = $self->{'log_file'};
        open(LOG,$f_log ) or &Log($f_log, "[DIED]: can not open $f_log tools_mapping.pm (myLog)\n");
	while(my $line = <LOG>){
		chomp($line);
		if($line =~ /^\[DIED\]/){ return -1 }

	}
	close(LOG);
	return 1;

}
sub Log_PROCESS{
        my ($self,$str) = @_;

        # open the log file for this experiment;
        my $f_log = $self->{'log_file'};
        open(LOG,">>$f_log" ) or &Log($f_log, "[DIED]: can not open $f_log tools_mapping.pm (myLog)\n");

        # print current time
        print LOG scalar localtime(),"\n";
        # print the information which is passed into this function by a string value
        print LOG "[PROCESS]: $str";

        close(LOG);

}
sub Log_SUCCESS{
        my ($self,$str) = @_;

        # open the log file for this experiment;
        my $f_log = $self->{'log_file'};
        open(LOG,">>$f_log" ) or &Log($f_log, "[DIED]: can not open $f_log tools_mapping.pm (myLog)\n");

        # print current time
        print LOG scalar localtime(),"\n";
        # print the information which is passed into this function by a string value
        print LOG "[SUCCESS]: $str";

        close(LOG);

}
sub Log_WARNING{
        my ($self,$str) = @_;

        # open the log file for this experiment;
        my $f_log = $self->{'log_file'};
        open(LOG,">>$f_log" ) or &Log($f_log, "[DIED]: can not open $f_log tools_mapping.pm (myLog)\n");

        # print current time
        print LOG scalar localtime(),"\n";
        # print the information which is passed into this function by a string value
        print LOG "[WARNING]: $str";

        close(LOG);

}
sub Log_DIED{
        my ($self,$str) = @_;

        # open the log file for this experiment;
        my $f_log = $self->{'log_file'};
        open(LOG,">>$f_log" ) or &Log($f_log, "[DIED]: can not open $f_log tools_mapping.pm (myLog)\n");

        # print current time
        print LOG scalar localtime(),"\n";
        # print the information which is passed into this function by a string value
        print LOG "[DIED]: $str";

        close(LOG);

        die "[DIED]: $str";
}


sub wait_for_queue{
        my ($self,$q_file) = @_;
	
	my $f_log =  $self->{'log_file'};
	
	# at each one minute check whether all files in this hash table are exist
	# it will continue checking until all files are exist
        foreach my $f (keys %$q_file){
		if($q_file->{$f} != 0 ){&Log("[DIED]: creating $f");last}
		my $i = 0;
		#pirnt "waiting for file $f \n";
		while(! (-e $f)){  sleep(60); $i ++ }
		print "found file $f \n sleeped $i * 60 seconds\n";
        }
}




1;

sub Log{
	my $f_log = shift;
        my $str = shift;

	# write the information contained inside the $str to log file
	# this function is only for information created by this module
        open(LOG,">>$f_log" );

        print LOG scalar localtime(),"\n";
        print LOG $str;

        close(LOG);

        if($str =~ m/^[DIED]/){die $str}
}
