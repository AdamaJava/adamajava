#! /usr/bin/perl -w

use strict;
package tools_SNPs;

sub new{
         my ($class,$conf)= @_;


         # read configure file
	
         my $self = { };
         my %argv = ();
         open(CONF, $conf) or die "[DIED]: can't open $conf in tools_mapping.pm\n";
         while(my $line = <CONF>){
           chomp($line);
           my ($key,$value) = split(/=/,$line);
	   $self->{$key} = $value;
         }
         close(CONF);

         # get log file name here
         $self->{'log_file'} = $self->{'output_dir'} . "$self->{'exp_name'}.SNP.log",
                 

        bless $self, $class;
        return $self;  
}
sub create_csfasta{
        #the $self is the classself
        #$f_raw is the input of original raw data which is csfasta file
        #$f_out is the output eg. S0014_20080714_1_hES_polysome_hES_membrane_F3.mers35.unique.csfasta

        my ($self,$f_raw, $f_out,$argv ) = @_;

        my $f_log = $self->{'log_file'};

        # open the created tag csfata file which will store orginal tag sequence
        # write new tag id into this file, which is bead id add ":1". eg. "1_245_362_F3:1"
        open(OUT,">$f_out") or &Log($f_log,"[DIED]: can't open file $f_out\n ");
        open(RAW, $f_raw) or  &Log($f_log,"[DIED]: can't open file $f_raw\n ");
        while(my $line = <RAW>){
                if( $line =~ m /^>/){
                        chomp($line);
                        my $id = "$line:1";
                        my $sequ = <RAW>;
                        print OUT "$id\n$sequ";
                }
        }

        close(RAW);
        close(OUT);

        #add the following success information into log file    
        &Log($f_log,"[SUCCESS]: Created csfasta file for tag with different tag length, in whichwe ingor tag quality!\n");


}
sub create_bc{
	my $self = shift;
	my $input = shift;
	my $f_chr = shift;
	my $output = "$input.bc";

	my $comm = "$self->{'script_editing'} -t single -i $input -g $f_chr -f3 $output";

	#for debug
	print "$comm\n";

	my $rc = system($comm);
	if($rc == 0 ){ &Log( $self->{'log_file'}, "[SUCCESS]: Created bc file: $output\n")  }
	else{  &Log( $self->{'log_file'}, "[DIED]: during create bc file\n $comm\n")  }

	
}
sub check_died{
	#get all arguments, 
	#argv[0] is class self;
	#argv[1] if exist will be chromosome name
        my @argv = @_;

	
        #read the log file
        my $f_log = $argv[0]->{'log_file'};
        open(LOG,$f_log ) or &Log($f_log, "[DIED]: can not open $f_log tools_mapping.pm (myLog)\n");
	
	
	if(exists $argv[1]){
		while(my $line = <LOG>){
			chomp($line);
			#died on current chromosome snp prediction
			if( ($line =~ /^\[DIED\]/) && ($line =~ /($argv[1])$/ ) ){return -1}
		}

	}
	else{
        	while(my $line = <LOG>){
		      chomp($line);
		      #died before parallel chromosomes snp prediction
		      if($line =~ /^\[DIED\]/){ return -1 }

        	}
	}
        close(LOG);
        return 1;

}




sub myLog{
        my ($self,$str) = @_;

        # open the log file for this experiment;
        my $f_log = $self->{'log_file'};
        open(LOG,">>$f_log" ) or &Log($f_log, "[DIED]: can not open $f_log tools_mapping.pm (myLog)\n");

        # print current time
        print LOG scalar localtime(),"\n";
        # print the information which is passed into this function by a string value
        print LOG $str;

        close(LOG);

        if($str =~ /^\[DIED\]/){ die "died: $str\n"}
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
        open(LOG,">>$f_log" ) or die "can't create log file\n";

        print LOG scalar localtime(),"\n";
        print LOG $str;

        close(LOG);

        if($str =~ m/^[DIED]/){die $str}
}

=head1 NAME

tools_SNP  - one of the  SNPs prediction perl modules for Grimmond Group

head1 SYNOPSIS

  use tools_SNPs
  my $obj = tools_SNPs->new($conf);
  $obj->create_csfasta($rawData,$output,\%argv);
  $obj->create_bc($input, $f_chr, $output);
  $obj->check_died();
  $obj->check_died($f_chr);
  $obj->myLog($str);
  $obj->wait_for_queue(\%q_file);

=head1 DESCRIPTION

This module contains several indepedent functions for supporting SNP pipeline. When the the instant of this module is creating, it read the configure files to get all needed parameters, like:

        use tools_SNPs;

        my $conf = "test.conf";

        my $obj = tools_SNPs->new($conf);



=head2 Methods

=over 5

=item * $obj->create_csfasta($rawData);

This function convert the raw data file into a csfasta file, in which the tag id will be orginal id plus ":1". eg. "1_53_1920_F3:1".

                                
=item * $obj->create_bc($input,$f_chr,$output);

This function passes the input file and genome file into editing.pl script and then create the .bc file.

example of input files (here we use chr1.fa as genome file):

			 chr1.tag_20000.mers35.single_selected.mismatched

           output files : chr1.tag_20000.mers35.single_selected.mismatched.bc

=item * $obj->check_died()  or $obj->check_died($f_chr) ;

This function will read the log file check whether previous steps or paralling job are died. It return "-1" for died job and "1" for all jobs are ok. When we pass chromosome name to it, it will check whether any this chromosome related jobs are died or not. 

=item * $obj->myLog($str);

This function write the message ($str) into log file.

=item * $obj->wait_for_queue(\%q_file);

This function will check whether all files in the hash table (%q_file) are created or not. For example, one step of the SNP pipeline is to submit all the genome mapping jobs to queueing system, meanwhile the next step can't excute until all these mapping jobs are done. Once one mapping job is done, it will created a small file, eg. chr1.tag_20000.mers35.chr10.unique.csfasta.ma.35.3.success (see f2m.pl). we add all these output file name into a hash table and pass it to this function, then every 60 seconds it will check these files until all of them are detected. 


=back

=head1 AUTHOR

qinying Xu (Christina) (q.xu@imb.uq.edu.au)

=head1 COPYRIGHT

Copyright 2008 Grimmond Group, IMB, UQ, AU.  All rights reserved.


=head1 SEE ALSO

perl(1).

=cut

