package QCMG::Bioscope::Run;

##############################################################################
#
#  Module:   QCMG::Bioscope::Run.pm
#  Creator:  Lynn Fink
#  Created:  2011-02-28
#
#  This class contains methods for automating the running of Bioscope on raw
#  sequencing data files
#
# $Id$
#
##############################################################################


=pod

=head1 NAME

QCMG::Bioscope::Run -- Common functions for running Bioscope

=head1 SYNOPSIS

Most common use:
 my $qi = QCMG::Bioscope::Run->new();

=head1 DESCRIPTION

Contains methods for checking that raw sequencing data files are ready for
mapping, that Bioscope is ready to be run, and to run Bioscope

=head1 REQUIREMENTS

 Exporter
 File::Spec
 POSIX 'strftime'
 QCMG::FileDir::Finder
 QCMG::FileDir::SeqRawParser
 QCMG::Automation::LiveArc
 QCMG::Automation::Config

=cut

use strict;

# standard distro modules
use Data::Dumper;
use File::Spec;				# for parsing paths

# in-house modules
use QCMG::Automation::LiveArc;		# for accessing Mediaflux/LiveArc
use QCMG::FileDir::Finder;		# for finding directories
use QCMG::FileDir::SeqRawParser;	# for parsing raw sequencing files

use QCMG::Automation::Common;
our @ISA = qw(QCMG::Automation::Common);	# inherit Common methods

use vars qw( $SVNID $REVISION $BIOSCOPE_ATTEMPTED $BIOSCOPE_SUCCEEDED $IMPORT_ATTEMPTED $IMPORT_SUCCEEDED );

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
	$self->{'run_folder'}		= $options->{'RUN_FOLDER'};

	$self->{'log_file'}		= $options->{'LOG_FILE'};

	#$self->{'hostname'}		= `hostname`;
	$self->{'hostname'}		= $self->HOSTKEY;
	chomp $self->{'hostname'};

	# RUN FOLDER NAME, should have / on the end
	if($self->{'run_folder'} !~ /\/$/){
		$self->{'run_folder'} .= '/';
	}

	$self->init_log_file();

	# must define log file before calling any another methods
	$self->slide_name();

	push @{$self->{'email'}}, $self->DEFAULT_EMAIL;

	# set global flags for checking in DESTROY()  (no/yes)
	$IMPORT_ATTEMPTED = 'no';
	$IMPORT_SUCCEEDED = 'no';
	$BIOSCOPE_ATTEMPTED = 'no';
	$BIOSCOPE_SUCCEEDED = 'no';

	return $self;
}

# Exit codes:
sub EXIT_NO_FOLDER {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'run_folder'}";
	$body		.= "\nFailed: No run folder: $self->{'run_folder'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 1;
	return 1;
}
sub EXIT_BAD_PERMS {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'run_folder'}";
	$body		.= "\nFailed: Bad permissions on $self->{'run_folder'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 2;
	return 2;
}
sub EXIT_NO_INI_FILE {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'run_folder'}";
	$body		.= "\nFailed: No ini file";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 8;
	return 8;
}
sub EXIT_LIVEARC_ERROR {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'run_folder'}";
	$body		.= "\nFailed: LiveArc error";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 9;
	return 9;
}
sub EXIT_ASSET_EXISTS {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'run_folder'}";
	$body		.= "\nFailed: LiveArc asset exists";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 12;
	return 12;
}
sub EXIT_IMPORT_FAILED {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'run_folder'}";
	$body		.= "\nFailed: LiveArc import failed";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 16;
	return 16;
}
sub EXIT_ASSET_NOT_EXISTS {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'run_folder'}";
	$body		.= "\nFailed: Requested LiveArc asset does not exist";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 24;
	return 24;
}

# SET exit(17) = run not attempted (but only used in DESTROY())

################################################################################

=pod

B<run_folder()>

Return run folder name, as set in new()

Parameters:

Returns:
  scalar run folder name

=cut
sub run_folder {
	my $self	= shift @_;
	return $self->{'run_folder'};
}

################################################################################
=pod

B<slide_name()> 
  Get slide name from run folder name (uses QCMG::FileDir::SeqRawParser)

Parameters:

Returns:
  scalar slide name 

=cut

sub slide_name {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
	# RUN_FOLDER - directory where runs are
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $rf			= $self->{'run_folder'};
	$rf			=~ s/\/$//;
	my ($v, $d, $file)	= File::Spec->splitpath($rf);
	$self->{'slide_name'} 	= $file;

	#print STDERR "Slide name:\t".$self->{'slide_name'}."\n";

        return $self->{'slide_name'};
}

################################################################################

=pod

B<asset_name()>

Return asset name, as derived from run folder name; this is the asset name of
the Bioscope INI files in LiveArc: RUN_FOLDER_bioscope

Parameters:

Returns:
  scalar asset name

=cut
sub asset_name {
	my $self	= shift @_;

	#$self->{'asset_name'} = $self->{'slide_name'}.$LA_ASSET_SUFFIX;
	$self->{'asset_name'} = $self->{'slide_name'}.$self->LA_ASSET_SUFFIX_BIOSCOPE;

	#print STDERR "Asset name: ", $self->{'asset_name'}, "\n";

	return $self->{'asset_name'};
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

B<check_folder()>

Check that directory provided by user exists, is a directory, and is readable

Parameters:

Returns:
 scalar: 0 if success; otherwise fill exit on fail

=cut
sub check_folder {
	my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
        my $options	= {};

	my $status = 1;

	$self->writelog(BODY => "---Checking ".$self->{'run_folder'}." ---");

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                defined($_[($i + 1)]) || 
                        die print STDERR "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	if(	!    $self->{'run_folder'} ||
		! -e $self->{'run_folder'} || 
		! -d $self->{'run_folder'}    ) {

		$self->writelog(BODY => "No valid run folder provided\n");
		#exit($self->EXIT_NO_FOLDER());
		exit($self->EXIT_NO_FOLDER());
	}

	if(	! -r $self->{'run_folder'}) {
		my $msg = " is not a readable directory";
		$self->writelog(BODY => $self->{'run_folder'}.$msg);
		exit($self->EXIT_BAD_PERMS());
	}

	if(	! -w $self->{'run_folder'}) {
		my $msg = " is not a writable directory";
		$self->writelog(BODY => $self->{'run_folder'}.$msg);
		#exit($self->EXIT_BAD_PERMS());
	}

	$self->writelog(BODY => "Folder exists and is read/writable.");	

	return($status);
}

################################################################################

=pod

B<check_bioscope_ini_files()>

Check that Bioscope INI files exist and are in the correct directory

Parameters:

Returns:
 scalar: 0 if success; otherwise fill exit on fail

=cut
sub check_bioscope_ini_files {
	my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
        my $options	= {};

	my $status = 1;

	# where Bioscope INI files should live
	#  /panfs/imb/seq_raw/RUN_FOLDER/_bioscope_ini
	my $ini_folder = $self->{'run_folder'}.$self->LA_ASSET_SUFFIX_BIOSCOPE_INI;
	$self->writelog(BODY => "---Checking top level INI folder: $ini_folder ---");	

=cut
_bioscope_ini/
  S0449_20100603_1_Frag.nopd.nobc/
    20110512/
	bioscope.plan
	global.ini
	maToBam.ini
	mappingF3.ini
	posErrors.ini
	run.pbs
	smallIndel.ini
=cut

	# get all mapset directories
	my $cmd		= qq{ls -1t $ini_folder};
	$self->writelog(BODY => "$cmd");
	my @mapset_dirs	= `$cmd`;

	# quit if there are no mapset directories
	if(scalar(@mapset_dirs) < 1) {
		$self->writelog(BODY => "No mapset directories found in $ini_folder)");	
		exit($self->EXIT_NO_INI_FILE());
	}

	# get dated folder in each mapset
	foreach (@mapset_dirs) {
		chomp;
		my $mapsetpath = $ini_folder."/".$_;

		my $cmd		= qq{ls -1t $mapsetpath};
		$self->writelog(BODY => "$cmd");	
		my @date_dirs	= `$cmd`;

		# quit if there are no dated directories
		if(scalar(@mapset_dirs) < 1) {
			$self->writelog(BODY => "No dated directories found in $mapsetpath)");	
			exit($self->EXIT_NO_INI_FILE());
		}

		# get most recently created dated folder
		my $dir			= shift @date_dirs;
		chomp $dir;
		$self->{'run_date'}	= $dir;
		$self->writelog(BODY => "Found most recently created dir: $dir");	

		# create full path to subdirectory and check for contents
		my $mapsetdatepath	= $mapsetpath."/".$dir;
		$cmd			= qq{ls $mapsetdatepath};
		$self->writelog(BODY => "$cmd");	
		my $inifiles		= `ls $mapsetdatepath`;
	
		# require that the directory contains at least a .plan, .ini, or
		# .pbs script
		if($inifiles =~ /\.plan/ || $inifiles =~ /\.ini/ || $inifiles =~ /\.pbs/) {
			push @{$self->{'run.pbs'}}, $mapsetdatepath."/run.pbs";
		}

		#### copy INI files to qcmg's SVN QCMGProduction dir 
		$self->writelog(BODY => "Copying INI files to SVN directory...");	
		my $svndir	= $ENV{'QCMG_SVN'}.'/QCMGProduction/bioscope/';
		$cmd		= qq{cp -R }.$ini_folder.qq{/* }.$svndir;
		$self->writelog(BODY => "$cmd");	
		my $rv = system($cmd);
		$self->writelog(BODY => "RV: $rv");	
		####
	}

	# quit if there is no run.pbs script
	unless(@{$self->{'run.pbs'}}) {
		$self->writelog(BODY => "No run.pbs script found in $ini_folder)");	
		exit($self->EXIT_NO_INI_FILE());
	}

	my $pbsscripts = join "\n", @{$self->{'run.pbs'}};	# for printing
	$self->writelog(BODY => "Found the following run.pbs scripts:\n$pbsscripts");	

	# if the subroutine came this far, things are good
	$status	= 0;

	return($status);
}

################################################################################
=pod

B<create_mapped_asset()>
 Create SLIDENAME_seq_mapped asset in LiveArc which will keep track of the PBS job
 ID of the bioscope run and the bioscope run status

Parameters:
 MAPSET  - mapset name to create asset for

Returns:
 void

=cut

sub create_mapped_asset {
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

	$self->writelog(BODY => "---Creating _seq_mapped asset(s)---");

	my $mapset	= $options->{'MAPSET'};

	if(! $mapset) {
		$self->writelog(BODY => "No mapset provided: ".$options->{'MAPSET'});
		exit($self->EXIT_ASSET_NOT_EXISTS());
	}

	# NS:    /test/S0449_20100603_1_Frag/S0449_20100603_1_Frag_mapped/
	# ASSET: S0449_20100603_1_Frag.nopd.nobc_seq_mapped
	# define LiveArc namespace
	my $ns			=  join "/", $self->LA_NAMESPACE, $self->{'slide_name'}, $self->{'slide_name'};
	$ns			.= $self->LA_NAMESPACE_SUFFIX_MAP;

	# define LiveArc asset name
	# $self->LA_ASSET_SUFFIX_MAP -> _seq_mapped
	my $asset		= $mapset.$self->LA_ASSET_SUFFIX_MAP;

	# create asset in LiveArc; this asset will hold metadata
	# describing the Bioscope run (date, status, etc.)
	my $la	= QCMG::Automation::LiveArc->new(VERBOSE => 1);
	my $assetid = $la->asset_exists(NAMESPACE => $ns, ASSET => $asset);
	my $status;
	if(! $assetid) {
		$self->writelog(BODY => "Creating _seq_mapped asset: $ns / $asset");
		$assetid = $la->create_asset(NAMESPACE => $ns, ASSET => $asset);

		# if asset.create fails, quit with error
		if(! $assetid) {
			$self->writelog(BODY => "$ns / $asset create failed");
			exit($self->EXIT_ASSET_NOT_EXISTS());
		}
		else {
			$self->writelog(BODY => "$ns / $asset created");
		}
	}

	return();
}

################################################################################
=pod

B<add_email()> 
 Enter an email address to which a notification will be mailed upon completion of 
 the ingestion to let them know if it succeeded or if it failed ingestion.

Parameters:
 EMAIL

Returns:
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

B<runpbs2mapset()> 
 Extract the mapset name from a run.pbs file

Parameters:
 FILE - run.pbs file name

Returns:
 scalar mapset name

=cut

sub runpbs2mapset {
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

	# seq_raw/S0449_20100603_1_Frag/_bioscope_ini/S0449_20100603_1_Frag.nopd.nobc/20110512/run.pbs
	my ($v, $d, $f)	= File::Spec->splitpath($options->{'FILE'});
	# $d = seq_raw/S0449_20100603_1_Frag/_bioscope_ini/S0449_20100603_1_Frag.nopd.nobc/20110512/
	$d =~ s/\/$//;
	# $d = seq_raw/S0449_20100603_1_Frag/_bioscope_ini/S0449_20100603_1_Frag.nopd.nobc/20110512
	($v, $d, $f)	= File::Spec->splitpath($d);
	# $d = seq_raw/S0449_20100603_1_Frag/_bioscope_ini/S0449_20100603_1_Frag.nopd.nobc/
	$d =~ s/\/$//;
	# $d = seq_raw/S0449_20100603_1_Frag/_bioscope_ini/S0449_20100603_1_Frag.nopd.nobc
	($v, $d, $f)	= File::Spec->splitpath($d);
	# $f = S0449_20100603_1_Frag.nopd.nobc

	$self->writelog(BODY => "Mapset: $f");

	return($f);
}

################################################################################
=pod

B<run_bioscope()> 
 Run bioscope using PBS script

Parameters:

Returns:
 void

=cut

sub run_bioscope {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Preparing for Bioscope run... ---");

	$BIOSCOPE_ATTEMPTED	= 'yes';

	$self->writelog(BODY => "---Creating _mapped namespace---");

	# NS:    /test/S0449_20100603_1_Frag/S0449_20100603_1_Frag_mapped/
	# define LiveArc namespace
	my $ns			=  join "/", $self->LA_NAMESPACE, $self->{'slide_name'}, $self->{'slide_name'};
	$ns			.= $self->LA_NAMESPACE_SUFFIX_MAP;

	$self->writelog(BODY => "_mapped namespace: $ns");

	my $la	= QCMG::Automation::LiveArc->new(VERBOSE => 1);
	my $nsstatus = $la->namespace_exists(NAMESPACE => $ns);	# 0 = exists; 1 = not exists
	$self->writelog(BODY => "RV: $nsstatus");
	unless($nsstatus == 0) {
		$self->writelog(BODY => "Namespace does not exist, creating _mapped namespace: $ns");
		$nsstatus = $la->create_namespace(NAMESPACE => $ns);
		$self->writelog(BODY => "RV: $nsstatus"); 

		# if asset.namespace.create fails, quit with error
		unless($nsstatus == 1) {
			$self->writelog(BODY => "$ns create failed");
			exit($self->EXIT_ASSET_NOT_EXISTS());
		}
	}

	$self->writelog(BODY => "Creating _mapped assets");

	my @e_files = ();
	my @o_files = ();
	my %pbs = ();
	foreach my $pbs (@{$self->{'run.pbs'}}) {
		# create _seq_mapped asset using run.pbs filename as template
		$self->writelog(BODY => "Creating _mapped asset from run.pbs: $pbs");
		my $mapset = $self->runpbs2mapset(FILE => $pbs);
		$self->create_mapped_asset(MAPSET => $mapset);

		# submit mapping job
		$self->writelog(BODY => "Submitting bioscope run: $pbs");
		my $rv = `qsub $pbs`;
		$rv =~ /^(\d+)\./;
		my $jobid = $1;
		$self->writelog(BODY => "Submitted $pbs as job $jobid");

		my $subj	= $self->{'slide_name'}." RUN.PBS SUBMITTED";
		my $body	= "RUN.PBS SUBMITTED for ".$mapset.": ".$pbs;
		$self->send_email(FROM => 'mediaflux@imq.uq.edu.au', TO => $self->DEFAULT_EMAIL, SUBJECT => $subj, BODY => $body);
	}

	$self->writelog(BODY => "Completed Bioscope run job submission");

	return();
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
	$msg		.= qq{TOOLLOG: SLIDE_NAME}.$self->{'slide_name'}."\n";

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

	# override $? with $self->{'EXIT'}

	unless($self->{'EXIT'} > 0) {
		# run_bioscope completed and bioscope completed
		#if($BIOSCOPE_ATTEMPTED eq 'yes' && $BIOSCOPE_SUCCEEDED eq 'yes') {
		if($BIOSCOPE_ATTEMPTED eq 'yes') {
			$self->writelog(BODY => "EXECLOG: errorMessage none");

			$self->{'EXIT'} = 0;

			# send success email if no errors
			my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- SUCCEEDED";
			my $body	= "SUBMISSION OF MAPPING JOB for $self->{'run_folder'} complete";
			$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			$self->send_email(SUBJECT => $subj, BODY => $body);
		}
		# running bioscope was not attempted; script ended for some other
		# reason
		elsif($BIOSCOPE_ATTEMPTED eq 'no') {
			$self->writelog(BODY => "EXECLOG: errorMessage Bioscope run not attempted");
	
			$self->{'EXIT'} = 17;
	
			# send success email if no errors
			my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- NOT ATTEMPTED";
			my $body	= "SUBMISSION OF MAPPING JOB for $self->{'run_folder'} not attempted";
			$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			$self->send_email(SUBJECT => $subj, BODY => $body);
		}
	}

	# complete log file after successful run

	my $eltime	= $self->elapsed_time();
	my $date	= $self->timestamp();

	$self->writelog(BODY => "EXECLOG: elapsedTime $eltime");
	$self->writelog(BODY => "EXECLOG: stopTime $date");
	$self->writelog(BODY => "EXECLOG: exitStatus $self->{'EXIT'}");

	close($self->{'log_fh'});

	# if exit value is 0, ingest log file as a new asset in same namespace
	if($self->{'EXIT'} == 0) {
		# ingest this log file
		my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};
		my $asset	= $self->{'slide_name'}.$self->LA_ASSET_SUFFIX_RUNBIOSCOPE_LOG;

		print STDERR "Ingesting run_bioscope log file\n";

		my $la	= QCMG::Automation::LiveArc->new(VERBOSE => 1);
		my $assetid = $la->asset_exists(NAMESPACE => $ns, ASSET => $asset);
		my $status;
		if(! $assetid) {
			print STDERR "Creating run_bioscope log file asset\n";
			$assetid = $la->create_asset(NAMESPACE => $ns, ASSET => $asset);
		}
		print STDERR "Updating run_bioscope log file\n";
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
