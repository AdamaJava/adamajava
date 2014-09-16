use strict;
package QCMG::miRNA_Mate::tools_miRNA;

sub new{
         my ($class,$conf)= @_;
	
	 # read configure file
	 my %argv = ();
         open(CONF, $conf) or die "[DIED]: can't open $conf in tools_miRNA.pm\n";
         while(my $line = <CONF>){
           chomp($line);
           my ($key,$value) = split(/=/,$line);
           $argv{$key} = $value;
         }
         close(CONF);

	 # get log file name here
         my $self = { log_file => $argv{'output_dir'} . "$argv{'exp_name'}.log"	};                       
        
	bless $self, $class;	
        return $self;  
}

sub editing{
        my ($self, $input, $f_chr,$argv) =@_;
        my $output = "$input.bc";

        my $comm = "$argv->{'script_editing'} -t single -i $input -g $f_chr -f3 $output";

        #for debug
        print "$comm\n";

        my $rc = system($comm);
        if($rc != 0){  &Log( $self->{'log_file'}, "[DIED]: during create bc file ($f_chr)!\n")  }
}

sub chop_tag{
	#input is the tag didn't matched on the genome or juction
	#output is the tag choped last few mers

        my ($self, $f_nonmatched, $f_shorttag,$l_chop) = @_;
	my $f_log = $self->{'log_file'};
        
	# open the input file in which the tag are not matched 
        open( IN, $f_nonmatched) or &Log($f_log, "[DIED]: can't open $f_nonmatched\n");
 	# open the output file in which the tag's last few mers will be choped
        open(OUT, ">$f_shorttag") or &Log($f_log, "[DIED]: can't open $f_shorttag\n");
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
		#debug
		#print "waiting for file $f \n";
		while(! (-e $f)){  sleep(60); $i ++ }
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



=head1 NAME

tools_miRNA  - one of the  miRNA detection perl modules for Grimmond Group

head1 SYNOPSIS

  use tools_miRNA
  my $obj = QCMG::miRNA_Mate::tools_miRNA->new($conf);
  $obj->chop_tag($f_nonmatched,$f_shorttag,$l_chop);
  $obj->check_died();
  $obj->wait_for_queue(\%q_file);
  $obj->Log_PROCESS($str);
  $obj->Log_SUCCESS($str);
  $obj->Log_WARNING($str);
  $obj->Log_DIED($str);
  $obj->editing($input, $f_chr, $argv);

=head1 DESCRIPTION

This module contains several indepedent functions for supporting SNP pipeline. When the the instant of this module is creating, it read the configure files to get all needed parameters, like:

        use tools_miRNA;

        my $obj = QCMG::miRNA_Mate::tools_miRNA->new("example.conf");



=head2 Methods

=over 8
=item * $obj->editing($input, $f_chr, $argv);

This function pass the input file and miRNA library file to scripts which listed on “script_editing”. 
The input should be a single selected ma file and related genome file, the output is the .bc file. see example below:

my $argv= {'script_editing' => "/data/editing.pl" }

$obj->editing("exp.BC1.once.SiM.positive", "miRNA.lib1.fa", $argv);
the output will be exp.BC1.once.SiM.positive.bc.

=item * $obj->check_died();

This function will read the log file check whether previous steps or paralling job are died. It return "-1" for died job and "1" for all jobs are ok. When we pass chromosome name to it, it will check whether any this chromosome related jobs are died or not. 

=item * $obj->wait_for_queue(\%q_file);

This function will check whether all files in the hash table (%q_file) are created or not. For example, one step of the pipeline is to submit all the mapping jobs to queueing system, meanwhile the next step can’t excute until all these mapping jobs are done. Once one mapping job is done, it will created a small file, 
eg. chr1.tag_20000.mers35.chr10.unique.csfasta.ma.35.3.success (see f2m.pl). 

we add all these output file name into a hash table and pass it to this function, then every 60 seconds it will check these files until all of them are detected.

=item * $obj->Log_PROCESS($str);

This function will add a string -- "[PROCESS]:" at top of the $str and then write into LOG file.

=item * $obj->Log_SUCCESS($str);

This function will add a string -- "[SUCCESS]:" at top of the $str and then write into LOG file.

=item * $obj->Log_WARNING($str);

This function will add a string -- "[WARNING]:" at top of the $str and then write into LOG file.

=item * $obj->Log_DIED($str);

This function will add a string -- "[DIED]:" at top of the $str and then write into LOG file.


=back

=head1 AUTHOR

qinying Xu (Christina) (q.xu@imb.uq.edu.au)

=head1 COPYRIGHT

Copyright 2009 Grimmond Group, IMB, UQ, AU.  All rights reserved.


=head1 SEE ALSO

perl(1).

=cut

