package QCMG::Extract::5500;

##############################################################################
#
#  Module:   QCMG::Extract::5500.pm
#  Creator:  Lynn Fink
#  Created:  2011-12-07
#
#  This class contains methods for automating the extractions of XSQ assets
#  from LiveArc to barrine
#
##############################################################################

=pod

=head1 NAME

QCMG::Extract::5500 -- Common functions for extracting XSQ raw sequencing files 

=head1 SYNOPSIS

Most common use:
 my $qi = QCMG::Extract::5500->new();

=head1 DESCRIPTION

Contains methods for extracting XSQ assets from LiveArc to barrine

=head1 REQUIREMENTS

 Exporter
 File::Spec
 POSIX 'strftime'
 QCMG::Automation::LiveArc

=cut

use strict;

# standard distro modules
use Data::Dumper;
use File::Spec;				# for parsing paths

# in-house modules
use QCMG::Automation::LiveArc;		# for accessing Mediaflux/LiveArc

use QCMG::Automation::Common;
our @ISA = qw(QCMG::Automation::Common);	# inherit Common methods

use vars qw( $SVNID $REVISION $EXTRACT_ATTEMPTED $EXTRACT_SUCCEEDED);

################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 RUN_FOLDER => directory
 LOG_FILE   => path and filename of log file

Returns:
 a new instance of this class.

=cut

sub new {
        my $that  = shift;

	#print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        my $class = ref($that) || $that;
        my $self = bless $that->SUPER::new(), $class;

	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->{'assetid'}		= $options->{'ASSETID'};
	$self->{'extractto'}		= $options->{'EXTRACTTO'};
	$self->{'log_file'}		= $options->{'LOG_FILE'};

	$self->{'hostname'}		= $self->HOSTKEY;

	# fail if the user does not have LiveArc credentials
	if(! -e $self->LA_CRED_FILE) {
		print STDERR "WARNING: LiveArc credentials file not found!\n";
	}

	$self->init_log_file();

	$self->slide_name();
	$self->run_folder();
	$self->asset_name();

	push @{$self->{'email'}}, $self->DEFAULT_EMAIL;

	$self->{'job_name'}	= 'la_extract_rawxsq';

	# set global flags for checking in DESTROY()  (no/yes)
	$EXTRACT_ATTEMPTED = 'no';
	$EXTRACT_SUCCEEDED = 'no';

	return $self;
}

# Exit codes:
sub EXIT_NO_FOLDER {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW EXTRACTION -- FAILED";
	my $body	= "RAW EXTRACTION of $self->{'asset_name'} ($self->{'assetid'})";
	$body		= "\nFailed: No run folder: $self->{'extractto'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 1;
	return 1;
}
sub EXIT_BAD_PERMS {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW EXTRACTION -- FAILED";
	my $body	= "RAW EXTRACTION of $self->{'asset_name'} ($self->{'assetid'})";
	$body		.= "\nFailed: Bad permissions on $self->{'extractto'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 2;
	return 2;
}
sub EXIT_NO_INI_FILE {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW EXTRACTION -- FAILED";
	my $body	= "RAW EXTRACTION of $self->{'asset_name'} ($self->{'assetid'})";
	$body		.= "\nFailed: No ini file (bioscope)";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 8;
	return 8;
}
sub EXIT_LIVEARC_ERROR {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW EXTRACTION -- FAILED";
	my $body	= "RAW EXTRACTION of $self->{'asset_name'} ($self->{'assetid'})";
	$body		.= "\nFailed: LiveArc error";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 9;
	return 9;
}
sub EXIT_BAD_RAWFILES {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW EXTRACTION -- FAILED";
	my $body	= "RAW EXTRACTION of $self->{'asset_name'} ($self->{'assetid'})";
	$body		.= "\nFailed: Raw sequencing files are corrupt";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage  $body");

	$self->{'EXIT'} = 13;
	return 13;
}
sub EXIT_MISSING_METADATA {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW EXTRACTION -- FAILED";
	my $body	= "RAW EXTRACTION of $self->{'asset_name'} ($self->{'assetid'})";
	$body		.= "\nFailed: Cannot find critical metadata";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 14;
	return 14;
}
sub EXIT_EXTRACT_FAILED {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW EXTRACTION -- FAILED";
	my $body	= "RAW EXTRACTION of $self->{'asset_name'} ($self->{'assetid'})";
	$body		.= "\nFailed: LiveArc extraction";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 16;
	return 16;
}
# SET exit(17) = extract not attempted (but only used in DESTROY())
sub EXIT_NO_ASSET {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW EXTRACTION -- FAILED";
	my $body	= "RAW EXTRACTION of $self->{'asset_name'} ($self->{'assetid'})";
	$body		.= "\nFailed: LiveArc asset does not exist";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 19;
	return 19;
}

################################################################################

=pod

B<run_folder()>

Create run folder name from EXTRACTTO and slide_name

Parameters:

Returns:
  scalar run folder name

=cut
sub run_folder {
	my $self	= shift @_;

	# create RUN FOLDER name
	# namespace = /QCMG/runname/
	# assetname = runname_seq_raw
	# run_folder = /panfs/imb/test/runname
	my $run_folder		= $self->{'extractto'}."/".$self->{'slide_name'};
	$run_folder 		=~ s/\/\//\//g;

	$self->{'run_folder'}	= $run_folder;

	mkdir $run_folder;

	return $self->{'run_folder'};
}

################################################################################

=pod

B<asset_name()>

Return asset file name

Parameters:

Returns:
  scalar asset name

=cut
sub asset_name {
	my $self	= shift @_;

	return $self->{'asset_name'};
}

################################################################################

=pod

B<runpbs_name()>

Return asset name for run.pbs asset/file

Parameters:

Returns:
  scalar asset name

=cut
sub runpbs_name {
	my $self	= shift @_;

	my $runpbs	= $self->{'asset_name'};

	$runpbs		=~ s/\.xsq/\.run\.pbs/;

	$self->{runpbs}	= $runpbs;

	return($runpbs);
}

################################################################################

=pod

B<ls_name()>

Return asset name for .ls asset/file

Parameters:

Returns:
  scalar asset name

=cut
sub ls_name {
	my $self	= shift @_;

	my $ls		= $self->{'asset_name'};

	$ls		=~ s/\.xsq/\.ls/;

	$self->{ls}	= $ls;

	return($ls);
}

################################################################################
=pod

B<log_file()> 
 Return log filename

Parameters:

Returns:
  scalar: filename

=cut
sub log_file {
        my $self = shift @_;

	return($self->{'log_file'});
}

################################################################################
=pod

B<slide_name()> 
  Get slide name from LiveArc asset metadata

Parameters:
 ID	=> LiveArc asset ID

Returns:
  scalar slide name 

=cut
sub slide_name {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	# get asset information from ID
        #:namespace "/test/S0449_20100603_1_nort"
        #:path      "/test/S0449_20100603_1_nort/:S0449_20100603_1_Frag_seq_raw"
        #:name      "S0449_20100603_1_nort_seq_raw"

	#$self->writelog(BODY => "Getting slide name");

	my $la	= QCMG::Automation::LiveArc->new(VERBOSE => 0);
	my $assetdata		= $la->get_asset(ID => $self->{'assetid'});
	$la 	= undef;

	# get slide name from root level namespace; should be like:
	#  /QCMG_5500/S17009_20120113_1_nort
	$self->{'slide_name'}	= $assetdata->{'NAMESPACE'};
	$self->{'slide_name'}	=~ s/^$self->LA_NAMESPACE_5500//;

	$self->{'asset_name'}	= $assetdata->{'NAME'};

	# keep metadata for parsing later
	$self->{'metadata'}	= $assetdata;

	#$self->writelog(BODY => "Metadata:\n$assetdata");

	my ($v, $root, $slide_name)	= File::Spec->splitpath($assetdata->{'NAMESPACE'});

	$self->{'slide_name'}	= $slide_name;
	$self->{'namespace'}	= $assetdata->{'NAMESPACE'};

	$self->writelog(BODY => "Slide name:\t".$self->{'slide_name'});
	$self->writelog(BODY => "Namespace:\t".$self->{'namespace'});
	$self->writelog(BODY => "Asset name:\t".$self->{'asset_name'});

        return $self->{'slide_name'};
}


################################################################################
=pod

B<check_extract_checksums()> 
 Perform checksums on XSQ file and compare to LiveArc
  metadata
 
Parameters:

Returns:
 void

=cut

sub check_extract_checksums {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Performing checksum on XSQ file---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	my $meta	= $self->{'metadata'};
=cut
       :meta -stime "81609"
            :mf-revision-history -id "1"
                :user -id "47"
                    :domain "infoteam"
                    :name "lfink"
                :type "modify"
            :checksums -id "2"
                :source "orig"
                :checksum
                    :chksum "1487819556"
                    :filesize "7613"
                    :filename "Sample1/results.F1B1/primary.20100610005836273/reads/S0449_20100603_1_Frag_Sample1_F3_QV.qual"
                :checksum
                    :chksum "149038259"
                    :filesize "3562"
                    :filename "Sample1/results.F1B1/primary.20100610005836273/reads/S0449_20100603_1_Frag_Sample1_F3.csfasta"
=cut

	if($meta->{'META'} !~ /:checksums/) {
		$self->writelog(BODY => "Checksums not available for this asset, skipping check");
		return;
	}

	my @checksums = ($meta->{'META'} =~ /:checksum\n\s+:chksum\s+\"(\d+)\"\n\s+:filesize\s+\"(\d+)\"\n\s+:filename\s+\"(.+?)\"/sg);

	for (my $i=0;$i<=$#checksums;$i+=3) {
		my $cksum = $checksums[$i];
		my $fsize = $checksums[$i+1];
		my $fname = $checksums[$i+2];
	
		#print STDERR "$fname: checksum $cksum, size $fsize\n";
	
		my ($crc, $blocks, $file) = $self->checksum(FILE => $self->{'run_folder'}."/".$self->{'asset_name'});

		$self->writelog(BODY => "LiveArc $file: checksum $cksum, size $fsize");

		$self->writelog(BODY => "Current $file: checksum $crc, size $blocks");
	
		#print STDERR "$fname: checksum $crc, size $blocks\n";
	
		unless($cksum == $crc && $fsize == $blocks) {
			$self->writelog(BODY => "Checksum invalid");
			exit($self->EXIT_BAD_RAWFILES());
		}
	}

	$self->writelog(BODY => "Checksums complete");
	$self->writelog(BODY => "END: ".$self->timestamp());

	return;
}

################################################################################
=pod

B<extract()> 
 Generate LiveArc commands and extract the data from LiveArc

Parameters:

Returns:

=cut
sub extract {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$EXTRACT_ATTEMPTED = 'yes';

	$self->writelog(BODY => "---Extracting LiveArc data---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	my $la	= QCMG::Automation::LiveArc->new(VERBOSE => 0);
	my $status;
=cut

	# get extract asset command for XSQ; 0 if success, 1 if failure
	$self->writelog(BODY => "Preparing to extract .xsq file...");
	my $cmd_seq_raw		= $la->extract_asset(ID => $self->{'assetid'}, RAW => 1, PATH => $self->{'run_folder'}."/".$self->{'asset_name'}, CMD => 1);
	$self->writelog(BODY => "LiveArc command: $cmd_seq_raw");
	my $status		= $la->extract_asset(ID => $self->{'assetid'}, RAW => 1, PATH => $self->{'run_folder'}."/".$self->{'asset_name'});
	# translate exit status: successful extraction = 1 so status should be 0
	if($status == 1) {
		$status		= 0;
	}
	else {
		$status		= 1;
	}
	$self->writelog(BODY => "RV: $status");
	if($status != 0) {
		exit($self->EXIT_EXTRACT_FAILED());
	}

=cut
	# get list of all assets in namespace
	# $assets = ref to hash; key = asset path, value = asset id
	my $ns		= $self->{'namespace'};	# from asset ID
	my $assets	= $la->list_assets(NAMESPACE => $ns);
	# S8006_20111021_2_FragPEBC_ingest_log -> S8006_20111021_2_FragPEBC
	#print STDERR "ASSETNAME: $self->{'asset_name'}\n";
	my $slide	= $self->{'asset_name'};
	$slide		=~ /(S\d+\_\d{8}\_\d\_\w+)\_ingest\_log/;
	$slide		= $1;

	#print Dumper $assets;

	my $status = 2;
	$self->writelog(BODY => "Preparing to extract all assets...");
	foreach my $aname (keys %{$assets}) {
		#print STDERR "Checking $aname, matching to $slide.\n";
		if($aname =~ /$slide\./ && $aname =~ /[\.ls|\.pbs|\.xsq]/) {
			#print STDERR "Extracting $aname, matching to $slide.\n";
			my $file	= $aname;
			$file		=~ s/^.+\:(.+)$/$1/;
			# extract matching assets (.ls and .run.pbs files)
			my $id		= $assets->{$aname};
			$self->writelog(BODY => "Extracting $aname (ID $id)...");
			my $cmd		= $la->extract_asset(ID => $id, RAW => 1, PATH => $self->{'run_folder'}."/".$file, CMD => 1);
			my $status	= $la->extract_asset(ID => $id, RAW => 1, PATH => $self->{'run_folder'}."/".$file);
			#print STDERR "$status : $cmd\n";
			$self->writelog(BODY => "$status : $cmd");

			if($status == 1) {
				$status		= 0;
			}
			else {
				$status		= 1;
			}
			$self->writelog(BODY => "RV: $status");
			if($status != 0) {
				exit($self->EXIT_EXTRACT_FAILED());
			}

			$status = 2;
		}
	}
=cut
	# get asset ID of .ls file
	$self->writelog(BODY => "Preparing to extract .ls file...");
	my $ns		= $la->LA_NAMESPACE_5500."/".$self->{'slide_name'};
	$self->writelog(BODY => "NS: $ns");
	$self->writelog(BODY => $self->ls_name()." is .ls asset name");
	my $meta	= $la->get_asset(NAMESPACE => $ns, ASSET => $self->ls_name());
	my $lsassetid	= $meta->{'ID'};
	$self->writelog(BODY => "Extracting from $ns, asset ID: $lsassetid");
	# get extract asset command for .ls file; 0 if success, 1 if failure
	my $cmd_seq_raw		= $la->extract_asset(ID => $lsassetid, RAW => 1, PATH => $self->{'run_folder'}."/".$self->ls_name(), CMD => 1);
	$self->writelog(BODY => "LiveArc command: $cmd_seq_raw");
	my $status		= $la->extract_asset(ID => $lsassetid, RAW => 1, PATH => $self->{'run_folder'}."/".$self->ls_name());
	# translate exit status: successful extraction = 1 so status should be 0
	if($status == 1) {
		$status		= 0;
	}
	else {
		$status		= 1;
	}
	$self->writelog(BODY => "RV: $status");
	if($status != 0) {
		exit($self->EXIT_EXTRACT_FAILED());
	}

	# get asset ID of .run.pbs file
	$self->writelog(BODY => "Preparing to extract .run.pbs file...");
	my $ns		= $la->LA_NAMESPACE_5500."/".$self->{'slide_name'};
	my $meta	= $la->get_asset(NAMESPACE => $ns, ASSET => $self->runpbs_name());
	my $rpassetid	= $meta->{'ID'};
	$self->writelog(BODY => "Extracting from $ns, asset ID: $rpassetid");
	# get extract asset command for .run.pbs file; 0 if success, 1 if failure
	my $cmd_seq_raw		= $la->extract_asset(ID => $rpassetid, RAW => 1, PATH => $self->{'run_folder'}."/".$self->runpbs_name(), CMD => 1);
	$self->writelog(BODY => "LiveArc command: $cmd_seq_raw");
	my $status		= $la->extract_asset(ID => $rpassetid, RAW => 1, PATH => $self->{'run_folder'}."/".$self->runpbs_name());
	# translate exit status: successful extraction = 1 so status should be 0
	if($status == 1) {
		$status		= 0;
	}
	else {
		$status		= 1;
	}
	$self->writelog(BODY => "RV: $status");

	if($status != 0) {
		exit($self->EXIT_EXTRACT_FAILED());
	}
	else {
		$EXTRACT_SUCCEEDED = 'yes';
	}
=cut

	$self->writelog(BODY => "Extract succeeded");

	$self->writelog(BODY => "Chgrping and chmoding...");
	# if successul, process dirs and files
	## This has no effect on barrine as not-user "qcmg"...
	my $seqrawdir = $self->{'run_folder'};
	my $chgrp = qq{chgrp -R qcmg $seqrawdir};
	$self->writelog(BODY => $chgrp);
	my $rv	= `$chgrp`;
	#print STDERR "chgrp $rv\n";

	my $chmod = qq{find $seqrawdir -type d -exec chmod 775 {} \\;};
		$self->writelog(BODY => $chmod);
	$rv	= `$chmod`;
	#print STDERR "chmod d $rv\n";

	$chmod	= qq{find $seqrawdir -type f -exec chmod 664 {} \\;};
	$self->writelog(BODY => $chmod);
	$rv	= `$chmod`;
	#print STDERR "chmod f $rv\n";

	$self->writelog(BODY => "END: ".$self->timestamp());

	return($status);
}

################################################################################
=pod

B<add_email()> 
 Enter an email address to which a notification will be mailed upon completion of 
 the ingestion to let them know if it succeeded or if it failed ingestion.

Parameters:
 EMAIL

Returns:
	$self->writelog(BODY => "Extract status: $status");
 void

=cut
sub add_email {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	push @{$self->{'email'}}, $options->{'EMAIL'};

	my $emails = join ", ", @{$self->{'email'}};

	$self->writelog(BODY => "Setting additional email: $emails");

	return;
}

################################################################################
=pod

B<get_email()> 
 Get all email addresses that will receive notifications

Parameters:

Returns:
 scalar: ref to array of addresses

=cut
sub get_email {
        my $self = shift @_;

	return($self->{'email'});
}


################################################################################
=pod

B<toollog()> 

Parameters:
 Write TOOLLOG names and values to log file and any extra EXECLOG lines

Returns:

=cut
sub toollog {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	# java version for Mediaflux/LiveArc
	my $j		= $self->LA_JAVA;
	my $javaver	= `$j -version 2>&1 >/dev/null`;
	$javaver	=~ s/.+?version\s+\"(.+?)\".+/$1/s;

	my $date = $self->timestamp();
	
	my $msg		.= qq{EXECLOG: javaVersion $javaver\n};
	$msg		.= qq{EXECLOG: javaHome $j\n};

	$msg		.= qq{TOOLLOG: LOG_FILE $self->{'log_file'}\n};
	$msg		.= qq{TOOLLOG: RUN_FOLDER $self->{'run_folder'}\n};
	$msg		.= qq{TOOLLOG: DEFAULT_EMAIL }.$self->DEFAULT_EMAIL."\n";

	$self->writelog(BODY => $msg);

	$self->writelog(BODY => "Beginning ingestion process at $date");

	return();
}

################################################################################
=pod

B<DESTROY()> 

 Destructor; write logging information to log file

Parameters:

Returns:
 void

=cut
sub DESTROY {
	my $self = shift;

	#$self->writelog(BODY => "DESTROY()");

	# override $? with $self->{'EXIT'}

	unless($self->{'EXIT'} > 0) {
		if($EXTRACT_ATTEMPTED eq 'yes' && $EXTRACT_SUCCEEDED eq 'yes') {
			$self->writelog(BODY => "EXECLOG: errorMessage none");

			$self->{'EXIT'} = 0;

			# send success email if no errors
			my $subj	= $self->{'asset_name'}." RAW EXTRACTION -- SUCCEEDED";
			my $body	= "RAW EXTRACTION of $self->{'asset_name'} successful\n";
			$body		.= "See log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			$self->send_email(SUBJECT => $subj, BODY => $body);
		}
		elsif($EXTRACT_ATTEMPTED eq 'no') {
			$self->writelog(BODY => "EXECLOG: errorMessage extract not attempted");
	
			$self->{'EXIT'} = 17;
	
			# send success email if no errors
			my $subj	= $self->{'asset_name'}." RAW EXTRACTION -- NOT ATTEMPTED";
			my $body	= "RAW EXTRACTION of $self->{'asset_name'} not attempted\n";
			$body		.= "See log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			$self->send_email(SUBJECT => $subj, BODY => $body);
		}
	}

	my $eltime	= $self->elapsed_time();
	my $date	= $self->timestamp();

	$self->writelog(BODY => "EXECLOG: elapsedTime $eltime");
	$self->writelog(BODY => "EXECLOG: stopTime $date");
	$self->writelog(BODY => "EXECLOG: exitStatus $self->{'EXIT'}");

	close($self->{'log_fh'});

	# if exit value is 0, ingest log file as a new asset in same namespace
	if($self->{'EXIT'} == 0) {
		my $ns		= join "/", $self->LA_NAMESPACE_5500, $self->{'slide_name'};
		my $asset	= $self->{'asset_name'}."_extract_log";
		print STDERR "Ingesting log file\n";
		my $la	= QCMG::Automation::LiveArc->new(VERBOSE => 1);
		my $assetid = $la->asset_exists(NAMESPACE => $ns, ASSET => $asset);
		my $status;
		if(! $assetid) {
			print STDERR "Importing log file asset\n";
			$assetid = $la->laimport_file(NAMESPACE => $ns, ASSET => $asset, FILE => $self->{'log_file'});
		}
		print STDERR "Updating log file\n";
		$status = $la->update_asset_file(ID => $assetid, FILE => $self->{'log_file'});

		print STDERR "$status\n";
	}

	return;
}


1;

__END__

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2009-2014

Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
