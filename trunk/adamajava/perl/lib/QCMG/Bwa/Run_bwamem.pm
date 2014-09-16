package QCMG::Bwa::Run;

##############################################################################
#
#  Module:   QCMG::Bwa::Run.pm
#  Creator:  Lynn Fink
#  Created:  2011-02-28
#
#  This class contains methods for automating the running of bwa on raw
#  sequencing data files
#
# $Id: Run.pm 1287 2011-10-19 03:40:38Z l.fink $
#
##############################################################################

#130423_7001407_0104_BD1VU3ACXX
=pod

=head1 NAME

QCMG::Bwa::Run -- Common functions for running bwa

=head1 SYNOPSIS

Most common use:
 my $qi = QCMG::Bwa::Run->new();

=head1 DESCRIPTION

Contains methods for checking that raw sequencing data files are ready for
mapping, that bwa is ready to be run, and to run bwa

=head1 REQUIREMENTS

 Exporter
 File::Spec
 POSIX 'strftime'
 QCMG::Automation::LiveArc
 QCMG::Automation::Config
 QCMG::DB::Geneus

=cut

use strict;
use warnings;

# standard distro modules
use Data::Dumper;
use File::Spec;				# for parsing paths
use Text::ParseWords;

# in-house modules
use QCMG::Automation::LiveArc;		# for accessing Mediaflux/LiveArc
use QCMG::DB::Metadata;

use QCMG::Automation::Common;
use QCMG::Ingest::MiSeq;
our @ISA = qw(QCMG::Automation::Common QCMG::Ingest::MiSeq);	# inherit Common methods

use vars qw( $SVNID $REVISION $BWA_ATTEMPTED $BWA_SUCCEEDED $IMPORT_ATTEMPTED $IMPORT_SUCCEEDED );

################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 SLIDE_FOLDER	=> directory
 LOG_FILE   	=> path and filename of log file
 FORCE_MAP	=> override LIMS setting and force mapset to be mapped (Y/N)

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

	# path to /panfs/seq_raw/SLIDE
	$self->{'slide_folder'}		= $options->{'SLIDE_FOLDER'};
	my ($v, $d, $f)			= File::Spec->splitpath($self->{'slide_folder'});
	$self->{'slide_name'}		= $f;

	# force mapset to be mapped? default to NO; only change default if
	# setting is 'Y'
	$self->{'force_mapping'}	= 'N';	# default
	if($options->{'FORCE_MAP'} eq 'Y') {
		$self->{'force_mapping'}	= $options->{'FORCE_MAP'};
	}

	# define LOG_FILE
	if($options->{'LOG_FILE'}) {
		$self->{'log_file'}		= $options->{'LOG_FILE'};
	}
	else {
		# DEFAULT: /run/folder/runfolder_ingest.log
		$self->{'log_file'}		= $self->AUTOMATION_LOG_DIR."/".$self->{'slide_name'}."_runbwa.log";
	}

	#print STDERR "LOG FILE: $self->{'log_file'}\n";

	#$self->{'hostname'}		= `hostname`;
	$self->{'hostname'}		= $self->HOSTKEY;
	chomp $self->{'hostname'};

	# RUN FOLDER NAME, should have / on the end
	if($self->{'slide_folder'} !~ /\/$/){
		$self->{'slide_folder'} .= '/';
	}

	$self->init_log_file();

	# must define log file before calling any another methods
	$self->slide_name();

	# set default contains_core; by default, flowcell does not contain core
	# facility samples; use LIMS to override this
	$self->{'contains_core'}	= 'N';

	push @{$self->{'email'}}, $self->DEFAULT_EMAIL;

	# set global flags for checking in DESTROY()  (no/yes)
	$IMPORT_ATTEMPTED = 'no';
	$IMPORT_SUCCEEDED = 'no';
	$BWA_ATTEMPTED = 'no';
	$BWA_SUCCEEDED = 'no';

	return $self;
}

# Exit codes:
sub EXIT_NO_FOLDER {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: No run folder: $self->{'slide_folder'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 1;
	return 1;
}
sub EXIT_BAD_PERMS {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: Bad permissions on $self->{'slide_folder'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 2;
	return 2;
}
sub EXIT_NO_FASTQ_FILE {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: No FASTQ file";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 88;
	return 88;
}
sub EXIT_LIVEARC_ERROR {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: LiveArc error";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 9;
	return 9;
}
sub EXIT_ASSET_EXISTS {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: LiveArc asset exists";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 12;
	return 12;
}
sub EXIT_IMPORT_FAILED {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: LiveArc import failed";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 16;
	return 16;
}
sub EXIT_ASSET_NOT_EXISTS {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: Requested LiveArc asset does not exist";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 24;
	return 24;
}
sub EXIT_NO_PBS {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: PBS scripts were not created";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 51;
	return 51;
}
sub EXIT_NO_LOGDIR {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: PBS log file directory could not be created";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 57;
	return 57;
}
sub EXIT_NO_FASTQ {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: Missing FASTQ file";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 59;
	return 59;
}
sub EXIT_NO_REFGENOME {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: A reference genome could not be inferred";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 52;
	return 52;
}
sub EXIT_NO_LIB {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: Missing library";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 57;
	return 57;
}
sub EXIT_NO_SEQRESDIR {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: Could not find or create seq_results directory";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 61;
	return 61;
}

# SET exit(17) = run not attempted (but only used in DESTROY())

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
	# SLIDE_FOLDER - directory where runs are
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $rf			= $self->{'slide_folder'};
	$rf			=~ s/\/$//;
	my ($v, $d, $file)	= File::Spec->splitpath($rf);
	$self->{'slide_name'} 	= $file;

	#print STDERR "Slide name:\t".$self->{'slide_name'}."\n";

        return $self->{'slide_name'};
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

	$self->writelog(BODY => "---Checking ".$self->{'slide_folder'}." ---");

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                defined($_[($i + 1)]) || 
                        die print STDERR "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	if(	!    $self->{'slide_folder'} ||
		! -e $self->{'slide_folder'} || 
		! -d $self->{'slide_folder'}    ) {

		$self->writelog(BODY => "No valid run folder provided\n");
		#exit($self->EXIT_NO_FOLDER());
		exit($self->EXIT_NO_FOLDER());
	}

	if(	! -r $self->{'slide_folder'}) {
		my $msg = " is not a readable directory";
		$self->writelog(BODY => $self->{'slide_folder'}.$msg);
		exit($self->EXIT_BAD_PERMS());
	}

	if(	! -w $self->{'slide_folder'}) {
		my $msg = " is not a writable directory";
		$self->writelog(BODY => $self->{'slide_folder'}.$msg);
		#exit($self->EXIT_BAD_PERMS());
	}

	$self->writelog(BODY => "Folder exists and is read/writable.");	

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

B<find_fastq_files()> 
 Find all FASTQ files to map

Parameters:

Requires:
 $self->{'slide_name'}

Returns:
 scalar ref to hash of FASTQ filenames with paths where key = read 1 file, value
   = read 2 file

Sets:
 $self->{'fastq_files'}

=cut

sub find_fastq_files {
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

	$self->writelog(BODY => "---Finding FASTQ Files---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	my $seqrawdir	= $self->seq_raw_dir();
	#my $cmd		= qq{find $seqrawdir -name "*fastq.gz"};
	my $cmd		= qq{find $seqrawdir -name "*fastq*"};	#### FIX!!!!
	my @files	= `$cmd`;

	my $slidename	= $self->{'slide_name'};

	my %fastq	= ();
	# just to be safe, make sure that only FASTQ files from the slide are
	# kept
	foreach (@files) {
		chomp;

		$fastq{$_} = 1 if(/$slidename/);
	}

	if(scalar(@files) < 1) {
		$self->writelog(BODY => "No FASTQ files found");
		exit($self->EXIT_NO_FASTQ());
	}

	# ...SHOULD HAVE STEP TO CHECK LIMS FOR LANES AND BARCODES...

	# check that each FASTQ has a paired file
	foreach my $file (keys %fastq) {
		#print STDERR "FILE: $file\n";
		# 110614_SN103_0846_BD03UMACXX.lane_1.nobc.1.fastq.gz
		# 110614_SN103_0846_BD03UMACXX.lane_1.nobc.2.fastq.gz
		if($file =~ /\.1\.fastq/) {
			#print STDERR "Keeping $file\n";
			my $pairedfile	= $file;
			$pairedfile	=~ s/\.1\.fastq/\.2\.fastq/;
			#print STDERR "Making pair $pairedfile\n";

			if(! -e $pairedfile || ! -e $file) {
				$self->writelog(BODY => "Missing either $file or $pairedfile");
				exit($self->EXIT_NO_FASTQ());
			}

			# pair both read ends as key/value pair in hash
			$fastq{$file}	= $pairedfile;
		}
		else {
			#print STDERR "Skipping $file\n";
			delete $fastq{$file};
		}
	}

        $self->writelog(BODY => "Found: $_\nFound: $fastq{$_}") for sort keys %fastq;

	$self->{'fastq_files'}	= \%fastq;

	#print Dumper $self->{'fastq_files'};

	$self->writelog(BODY => "END: ".$self->timestamp());
	$self->writelog(BODY => "---Done---");

	return();
}

################################################################################
=pod

B<seq_raw_dir()> 
 Return path to seq_raw directory with FASTQ files

Parameters:
 $self->{'slide_name'}
 $self->SEQ_RAW_PATH

Returns:
 scalar string of path to seq_raw

=cut
sub seq_raw_dir {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $dir	= join "/", $self->SEQ_RAW_PATH, $self->{'slide_name'};

	return($dir);
}

################################################################################
=pod

B<seq_mapped_dir()> 
 Return path to seq_mapped directory for BAM files

Parameters:
 $self->{'slide_name'}
 $self->SEQ_MAPPED_PATH

Returns:
 scalar string of path to seq_mapped

=cut
sub seq_mapped_dir {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $dir	= join "/", $self->SEQ_MAPPED_PATH, $self->{'slide_name'};

	return($dir);
}

################################################################################
=pod

B<samplesheet()> 
 Sample sheet file (SampleSheet.csv) (resides in basecall folder)

Parameters:
 FILE	=> path and name of a non-default sample sheet (optional)

Requires:
 $self->{'basecall_folder'}

Returns:
  scalar path and filename of sample sheet

Sets:
 $self->{'samplesheet'}

#cut
sub samplesheet {
        my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	#print STDERR "REDEFINING SAMPLE SHEET: $options->{'FILE'}\n";

	my $csv;
	if($options->{'FILE'}) {
		$csv		= $options->{'FILE'};
		$self->writelog(BODY => "Using non-default sample sheet file: $csv"); 
	}
	else {
		# define default sample sheet if user has not specified one or it
		# doesn't exist

		# infer sample sheet name and location
		$self->basecall_folder();
		$csv	= $self->{'basecall_folder'}."/SampleSheet.csv";
		$self->writelog(BODY => "Using default sample sheet file: $csv"); 
	}
	
	# check that file exists and is readable
	if(-e $csv && -r $csv) {
		$self->{'SampleSheet.csv'}	= $csv;
		$self->writelog(BODY => "Sample sheet found");
	}
	else {
		$self->writelog(BODY => "Sample sheet not found");
	}

	#print STDERR "SAMPLE SHEET: $self->{'samplesheet'}\n";

	return;
}
=cut
################################################################################
=pod

B<basecall_folder()>

Return path to directory with basecall (bcl) files

Parameters:

Requires:
 $self->{'slide_folder'}

Returns:
 scalar path

Sets:
 $self->{'basecall_folder'}

=cut
sub basecall_folder {
	my $self	= shift @_;

	$self->writelog(BODY => "Finding basecall folder...");

	# should be under slide level directory:
	# /path.../110614_SN103_0846_BD03UMACXX.images/Data/Intensities/BaseCalls/
#
	$self->hiseq_folder();

	my $path	= $self->{'hiseq_folder'}."/Data/Intensities/BaseCalls/";

	if(-e $path && -d $path) {
		$self->{'basecall_folder'}	= $path;
		$self->writelog(BODY => "Found $path");
	}
	else {
		$self->writelog(BODY => "No valid basecall folder provided; tried $path\n");
		#exit($self->EXIT_NO_BCL());
	}

	return($self->{'basecall_folder'});
}

################################################################################
=pod

B<hiseq_folder()>

Return path to /mnt/HiSeq/ directory

Parameters:

Requires:
 $self->{'slide_name'}

Returns:
 scalar path

Sets:
 $self->{'hiseq_folder'}

=cut
sub hiseq_folder {
	my $self	= shift @_;

	$self->writelog(BODY => "Finding HiSeq folder...");

	my $path	= '/mnt/HiSeqNew/runs/'.$self->{'slide_name'};

	if(-e $path && -d $path) {
		$self->{'hiseq_folder'}	= $path;
		$self->writelog(BODY => "Found $path");
	}
	else {
		$self->writelog(BODY => "HiSeq folder not found; tried $path\n");

		$path	= '/mnt/HiSeq/runs/'.$self->{'slide_name'};

		if(-e $path && -d $path) {
			$self->{'hiseq_folder'}	= $path;
			$self->writelog(BODY => "Found $path");
		}
		else {
			$self->writelog(BODY => "No valid HiSeq folder provided; tried $path\n");
			exit($self->EXIT_NO_FOLDER());
		}
	}

	return($self->{'hiseq_folder'});
}

################################################################################
=pod

B<read_samplesheet()> 
 Read SampleSheet.csv and extract necessary info

 The sample sheet contains the following columns:

 FCID		Flow cell ID
 Lane		Positive integer, indicating the lane number (1-8)
 SampleID	ID of the sample
 SampleRef	The name of the reference
 Index		Index sequence(s)
 Description	Description of the sample
 Control	Y = control lane; N = sample lane
 Recipe		Recipe used during sequencing
 Operator	Name or ID of the operator
 SampleProject	The project the sample belongs to
 AND OPTIONAL EXTENDED COLUMNS:
 Project	The base project (smgres_gemm, icgc_pancreatic, etc)
 Library	Primary library
 AlignmentReq	Is alignment required (1=yes, 0=no)

Parameters:
 $self->{basecall_folder}

Returns:
 
Sets:
 $self->{'lane2ref'}        (ref to hash, key = lane numbers (1,2,3. etc), key = index, value = ref genome
 $self->{'lane2project'}    (ref to hash, key = lane numbers (1,2,3. etc), key = index, value = donor 
 $self->{'lane2sampleid'}   (ref to hash, key = lane numbers (1,2,3. etc), key = index, value = sample ID
 $self->{'lane2projectdir'} (ref to hash, key = lane numbers (1,2,3. etc), key = index, value = base project directory
 $self->{'lane2library'}    (ref to hash, key = lane numbers (1,2,3. etc), key = index, value = library ID
 $self->{'lane2alignreq'}   (ref to hash, key = lane numbers (1,2,3. etc), key = index, value = Y/N

#cut
sub read_samplesheet {
        my $self = shift @_;

	$self->writelog(BODY => "---Reading SampleSheet.csv...---");

	my $sscsv;
	if(! $self->{'samplesheet'}) {
		$sscsv	= $self->samplesheet();
	}
	else {
		my $sscsv	= $self->{'SampleSheet.csv'};
	}

	my %lane2ref		= ();
	my %lane2project	= ();
	my %lane2sampleid	= ();
	my %lane2projectdir	= ();
	my %lane2library	= ();
	my %lane2alignreq	= ();

	# get contents of sample sheet file
	open(FH, $sscsv);
	while(<FH>) {
		chomp;

		# remove DOS new lines
		s/\r\n?//g;

		# skip header
		next if(/^FCID/);

		#my ($fc, $lane, $sampleid, $sampleref, $index, $desc, $control, $recipe, $operator, $sampleproject, $project, $library, $alignment_req)	= split /,/;
		my ($fc, $lane, $sampleid, $sampleref, $index, $desc, $control, $recipe, $operator, $sampleproject, $project, $library, $alignment_req) = quotewords(',', 0, $_);

		#print STDERR "$project, $library, $alignment_req\n";

		#$index	= 'NoIndex' if(! $index);
		$index	= 'nobc' if(! $index);
		$index	= 'nobc' if($index =~ /noindex/i);

		#print STDERR "$lane, $index, $sampleref, $sampleid, $sampleproject\n";

		$lane2project{$lane}{$index}	= $sampleproject;
		#$lane2ref{$lane}{$index}	= $genomes{$sampleref};
		$lane2ref{$lane}{$index}	= $sampleref;
		$lane2sampleid{$lane}{$index}	= $sampleid;
		$lane2projectdir{$lane}{$index}	= $project;
		$lane2library{$lane}{$index}	= $library;
		$lane2alignreq{$lane}{$index}	= $alignment_req;

		$self->writelog(BODY => "$lane\t$index\t$sampleref\t$sampleproject");
	}
	close(FH);

	$self->{'lane2ref'}		= \%lane2ref;
	$self->{'lane2project'}		= \%lane2project;
	$self->{'lane2sampleid'}	= \%lane2sampleid;
	$self->{'lane2projectdir'}	= \%lane2projectdir;
	$self->{'lane2library'}		= \%lane2library;
	$self->{'lane2alignreq'}	= \%lane2alignreq;

	#print Dumper $self->{'lane2library'};
	#print Dumper $self->{'lane2ref'};

	$self->writelog(BODY => "---done---");

	return();
}
=cut

################################################################################
sub split_up_slide_data {
#
# formats the lims data into five nice hashes for easy access.
#
    my $self = shift;

    for my $lane (keys %{ $self->{'Slide data'} }) {

        for my $index (keys %{ $self->{'Slide data'}{$lane} }) {

            $self->{'lane2sampleid'}{$lane}{$index} = $self->{'Slide data'}{$lane}{$index}{'Sample Name'};

            $self->{'lane2alignreq'}{$lane}{$index} = $self->{'Slide data'}{$lane}{$index}{'Alignment Required'};

            $self->{'lane2ref'}{$lane}{$index} = $self->{'Slide data'}{$lane}{$index}{'Species Reference Genome'};

            $self->{'lane2library'}{$lane}{$index} = $self->{'Slide data'}{$lane}{$index}{'Primary Library'};

            $self->{'lane2project'}{$lane}{$index} = $self->{'Slide data'}{$lane}{$index}{'Donor'};
            $self->{'lane2project'}{$lane}{$index} =~ s/_/-/;
        }

    }

    delete $self->{'Slide data'};
}

################################################################################
=pod

B<check_project_dir()> 
 Look for the expected directory in /mnt/seq_results/ and if it does not exist,
  create it

Parameters:

Sets:
 $self->{'is_in_lims'} (y/n)

Requires:
 $self->{'slide_name'}

Returns:

=cut
sub check_project_dir {
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

	my $status = 0;

	$self->writelog(BODY => "---Checking/creating project directories in seq_results...---");

	my $md		= QCMG::DB::Metadata->new();
	if($md->find_metadata("slide", $self->{'slide_name'})) {
		my $mapsets 	=  $md->slide_mapsets($self->{'slide_name'});

		foreach my $mapset (@{$mapsets}) {
			$md->find_metadata("mapset", $mapset);	# required for following step to work
	
			my $d	= $md->mapset_metadata($mapset);
			my $project	= $d->{'parent_project'};
			my $donor	= $d->{'project'};
	
			my $seqresdir;
			if($project =~ /smgcore/) {
				$seqresdir	= '/mnt/seq_results/smg_core/'.$project;
			}
			else {
				$seqresdir	= '/mnt/seq_results/'.$project;
			}
	
			$self->writelog(BODY => "Checking $seqresdir");
				
			if(! -d $seqresdir) {
				$self->writelog(BODY => "Making project dir");
				my $cmd	= qq{mkdir $seqresdir};
				my $rv	= system($cmd);
				$self->writelog(BODY => "Failed to make project dir $seqresdir") if($rv != 0);
				exit($self->EXIT_NO_SEQRESDIR()) if($rv != 0);
			}
			else {
				$self->writelog(BODY => "$seqresdir exists");
			}
	
			$seqresdir	.= "/$donor/";
	
			if(! -d $seqresdir) {
				$self->writelog(BODY => "Making donor dir");
				my $cmd	= qq{mkdir $seqresdir};
				my $rv	= system($cmd);
				$self->writelog(BODY => "Failed to make donor dir $seqresdir") if($rv != 0);
				exit($self->EXIT_NO_SEQRESDIR()) if($rv != 0);
			}
			else {
				$self->writelog(BODY => "$seqresdir exists");
			}
	
			$seqresdir	.= 'seq_mapped/';
	
			if(! -d $seqresdir) {
				$self->writelog(BODY => "Making seq_mapped dir");
				my $cmd	= qq{mkdir $seqresdir};
				my $rv	= system($cmd);
				$self->writelog(BODY => "Failed to make seq_mapped dir $seqresdir") if($rv != 0);
				exit($self->EXIT_NO_SEQRESDIR()) if($rv != 0);
			}
			else {
				$self->writelog(BODY => "$seqresdir exists");
			}
		}
		$md	= undef;

		$self->writelog(BODY => "Directories all exist");
		$self->{'is_in_lims'}	= 'Y';
	}
	else {
		$self->writelog(BODY => "WARNING: slide not found in LIMS, cannot confirm directories");
		$self->{'is_in_lims'}	= 'N';
	}

	return();
}


################################################################################
=pod

B<create_pbs_scripts()> 
 Create PBS scripts that will run bwa, create indexes, and copy BAMs to the
  appropriate place

Parameters:

Sets:
 $self->{'logdir'}

Requires:
 $self->{'fastq'}

Returns:

=cut
sub create_pbs_scripts {
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

	my $status = 0;

	$self->writelog(BODY => "---Creating PBS scripts for bwa, etc---");
	$self->writelog(BODY => "START: ".$self->timestamp());


	# check if run is paired-end or single-end; run bwa sampe for paired,
	# bwa samse for single-end
	$self->is_paired();

	# create log file directory
	my $logdir		= $self->seq_raw_dir()."/log/";
	my $rv			= mkdir $logdir;
	if($rv != 0 && ! -e $logdir) {
		$self->writelog(BODY => "Cannot create log directory: $logdir (rv: $rv)");
		exit($self->EXIT_NO_LOGDIR());
	}
	$self->{'logdir'}	= $logdir;

	# generic PBS header for all scripts
	my $header	= $self->pbs_header_template();

	my $bwamemcmd	= $self->pbs_bwamem_template();
	$bwamemcmd	= $header.$bwamemcmd;
	$bwamemcmd	=~ s/<QUEUE>/mapping/;
	$bwamemcmd	=~ s/<JOBNAME>/bwamem/;
	$bwamemcmd	=~ s/<NCPUS>/8/;
	$bwamemcmd	=~ s/<WALLTIME>/30:00:00/;
	$bwamemcmd	=~ s/<MEM>/15gb/;

	if($self->{'is_paired'} eq 'n') {
		# remove second read file from template if only single end run
		$bwamemcmd	=~ s/<FASTQGZ2>//;
	}

	# create directory to put PBS scripts in:
	# /panfs/seq_raw/SLIDENAME/pbs/
	my $seqrawdir	= $self->seq_raw_dir();
	my $pbsdir	= join '/', $seqrawdir, 'pbs/';
	#my $pbsdir	= join '/', $seqrawdir, 'pbstest/';
	$self->{'pbsdir'}	= $pbsdir;
	$self->writelog(BODY => "Creating dir for PBS scripts: $pbsdir");
	$rv		= mkdir $pbsdir unless(-e $pbsdir);
	if($rv != 0 && ! -e $pbsdir) {
		$self->writelog(BODY => "$rv: could not create $pbsdir");
		exit($self->EXIT_NO_PBS());
	}

	# create directory to put SAMs and BAMs in:
	# /panfs/seq_mapped/SLIDENAME/
	my $seqmapdir	= $self->seq_mapped_dir();
	$seqmapdir	= join '/', $seqmapdir;
	$self->{'seqmapdir'}	= $pbsdir;
	$self->writelog(BODY => "Creating seq_mapped dir: $seqmapdir");
	$rv		= mkdir $seqmapdir unless(-e $seqmapdir);
	if($rv != 0 && ! -e $seqmapdir) {
		$self->writelog(BODY => "$rv: could not create $seqmapdir");
		exit($self->EXIT_NO_PBS());
	}

	#print Dumper $self->{'fastq_files'};

	#### FILL TEMPLATES
	my @pbsfiles	= ();
	my @lanenums	= ();
	my $alllanespipeline;
	my $binauto	= $self->{'BIN_AUTO'};
	# should just go through each read 1 file (read 2 file is value)
	foreach my $f (keys %{$self->{'fastq_files'}}) {
		$self->writelog(BODY => "Generating PBS script content for $f");

		# 110614_SN103_0846_BD03UMACXX.lane_1.nobc.1.fastq.gz
		$f			=~ /lane\_(\d)\.(\w+)/;

		# keep track of lanes and indexes to map (by "key" = lane and
		# index concatenated; used for PBS variable job names)
		my $lane		= $1;
		my $index		= $2;
		my $lanekey		= join "", $lane, $index;
		push @lanenums, $lanekey;
		$self->writelog(BODY	=> "Mapset key: $lanekey");

		# get paired FASTQ file
		# 110614_SN103_0846_BD03UMACXX.lane_1.nobc.2.fastq.gz
		my $f2			= $self->{'fastq_files'}->{$f};
		#print STDERR "FASTQ1: $f\nFASTQ2: $f2\n";

		# 110614_SN103_0846_BD03UMACXX.lane_1.nobc
		my $mapset		= $self->infer_mapset(FILE => $f);


		# check if run is mate-pair; use different subroutine
		if($self->is_matepair(MAPSET => $mapset) eq 'y') {
			$self->_create_pbs_scripts_matepair;
			return;
		}

		# do not bother mapping if mapset failed QC (as marked in LIMS)
		if($self->get_qc(MAPSET => $mapset) != 0) {
			$self->writelog(BODY => "$mapset failed QC, skipping");
			next;
		}

		# decide if PBS should be made to map this mapset
		$self->writelog(BODY => "Checking if $f should be mapped...");
		my $ar			= $self->get_alignment_required(MAPSET => $mapset);
		if(! $ar && $self->{'is_in_lims'} eq 'N') {
			$ar		= $self->{'lane2alignreq'}->{$lane}->{$index};
		}
		if($ar ne 'Y' && $self->{'force_mapping'} ne 'Y') {
			$self->writelog(BODY => "Alignment not required ($ar), skipping");
			next;
		}

		# decide if BWA should be run or mapsplice for RNA
		my $material		= $self->get_material(MAPSET => $mapset);
		next unless($material =~ /DNA/i);

		# get presumed and anticipated filenames
		my $ref			= $self->get_reference(MAPSET => $mapset);
		if(! $ref) {
			# if there is still no reference genome, don't try to
			# map
			$self->writelog(BODY => "Reference genome not found, skipping");
		}

		#print STDERR "Using reference genome: $ref\n";

		# anticipated seq_mapped BAM
		my $bam			= $self->infer_seqmapped_bam(FILE => $f);
		my $unsortedbam		= $bam;
		$unsortedbam		=~ s/\.bam/\.unsorted\.bam/;
		my $fixrgbam		= $bam;
		$fixrgbam		=~ s/\.bam/\.fixrg\.bam/;
		my $dedupbam		= $bam;
		$dedupbam		=~ s/\.bam/\.deduped\.bam/;
		my $bamprefix		= $bam;
		$bamprefix		=~ s/\.bam//;
		# anticipated seq_results BAM
		#my $newbam		= $self->infer_seqresults_bam(FILE => $f, PATIENT => $self->{'lane2project'}->{$lane}->{$index});
		my $newbam		= $self->infer_seqresults_bam(FILE => $f, MAPSET => $mapset);
		#print STDERR "FILE => $f, PATIENT => $lane, $index -> ".$self->{'lane2project'}->{$lane}->{$index}."\n";

		my $project		= $self->get_project(MAPSET => $mapset);
		if(! $project) {
			# if there no project, don't try to
			# map - there will be no directory to put the files in
			$self->writelog(BODY => "Project name not found, skipping");
		}
		my $library		= $self->get_library(MAPSET => $mapset);
		if(! $library) {
			$library	= $self->{'lane2library'}->{$lane}->{$index};
		}
		my $donor		= $self->get_donor(MAPSET => $mapset);
		if(! $donor) {
			$donor		= $self->{'lane2project'}->{$lane}->{$index};
		}
		my $rgid		= $self->timestamp(FORMAT => 'YYYYMMDDhhmmss');

		### build commands
		my $olog	= $logdir.$mapset."_bwamem_out.log";
		my $bwamem	= $bwamemcmd;
		$bwamem		=~ s/<FASTQGZ1>/$f/g;
		$bwamem		=~ s/<FASTQGZ2>/$f2/g if($self->{'is_paired'} eq 'y');
		$bwamem		=~ s/<GENOME>/$ref/g;
		$bwamem		=~ s/<OUTLOG>/$olog/g;
		$bwamem		=~ s/<LIBRARY>/$library/g;
		$bwamem		=~ s/<DONOR>/$donor/g;
		$bwamem		=~ s/<GENOME>/$ref/g;
		$bwamem		=~ s/<BAMPREFIX>/$bamprefix/g;
		$bwamem		=~ s/<NEWBAM>/$newbam/g;
		$bwamem		=~ s/<MAPSET>/$mapset/g;

		$self->writelog(BODY => "Writing PBS files for $mapset");

		# create filename for PBS scripts
		my $basefilename	= $pbsdir.$mapset.".<NAME>.pbs";

		my $pbsbwamem		= $basefilename;
		$pbsbwamem		=~ s/<NAME>/bwamem/;
		$self->write_pbs_file(CMD => $bwamem, FILENAME => $pbsbwamem);

		# now that we know the other script names, we can fill the
		# pipeline template for this lane
		my $pipeline		= $self->pipeline_template_bwamem(LANE => $lanekey);
		$pipeline		=~ s/<PBSBWAMEM>/$pbsbwamem/;

		$alllanespipeline	.= $pipeline;

		# set aligner field in LIMS with 'bwa' for each mapset
		$self->set_lims_aligner(MAPSET => $mapset);
	}

	# create slide-specific template for command to ingest mapped data
	$self->writelog(BODY => "Writing mapped ingest PBS file for $self->{'slide_name'}");
	my $olog	= $logdir.$self->{'slide_name'}."_ingestmapped_out.log";
	my $ingmap	= $self->pbs_ingmap_template();
	$ingmap		= $header.$ingmap;
	$ingmap		=~ s/<JOBNAME>/ingestmapped/;
	$ingmap		=~ s/<QUEUE>/mapping/;
	$ingmap		=~ s/<NCPUS>/1/;
	$ingmap		=~ s/<WALLTIME>/6:00:00/;
	$ingmap		=~ s/<MEM>/600mb/;
	$ingmap		=~ s/<OUTLOG>/$olog/;
	$ingmap		=~ s/<BIN_AUTO>/$binauto/g;
	$ingmap		=~ s/<SLIDE>/$self->{'slide_name'}/;
	my $pbsingmap	= $pbsdir.$self->{'slide_name'}.".ingestmapped.pbs";
	$self->write_pbs_file(CMD => $ingmap, FILENAME => $pbsingmap);

	$self->writelog(BODY => "Writing master PBS file for $self->{'slide_name'}");
	# create one script to run them all; this script just qsubs other
	# scripts and set up dependencies
	$olog	= $logdir.$self->{'slide_name'}."_master_out.log";
	my $master	= $self->pbs_master_template(LANES => \@lanenums);
	$master		= $header.$master;
	$master		=~ s/<JOBNAME>/maphiseq/;
	$master		=~ s/<QUEUE>/mapping/;
	$master		=~ s/<NCPUS>/1/;
	$master		=~ s/<WALLTIME>/00:05:00/;
	$master		=~ s/<MEM>/200mb/;
	$master		=~ s/<OUTLOG>/$olog/;
	$master		=~ s/<PIPELINE>/$alllanespipeline/;
	$master		=~ s/<PBSINGMAP>/$pbsingmap/;
	my $pbsmaster	= $pbsdir.$self->{'slide_name'}.".master.pbs";
	$self->write_pbs_file(CMD => $master, FILENAME => $pbsmaster);

	# save filename of master script
	push @pbsfiles, $pbsmaster;
	$self->{'pbsfiles'}	= \@pbsfiles;

	$self->writelog(BODY => "END: ".$self->timestamp());

	return($status);
}


################################################################################
=pod

B<create_pbs_scripts_matepair()> 
 Create PBS scripts that will run bwa, create indexes, and copy BAMs to the
  appropriate place

Parameters:

Sets:
 $self->{'logdir'}

Requires:
 $self->{'fastq'}

Returns:

=cut
sub _create_pbs_scripts_matepair {
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

	my $status = 0;

	# create log file directory
	my $logdir		= $self->seq_raw_dir()."/log/";
	my $rv			= mkdir $logdir;
	if($rv != 0 && ! -e $logdir) {
		$self->writelog(BODY => "Cannot create log directory: $logdir (rv: $rv)");
		exit($self->EXIT_NO_LOGDIR());
	}
	$self->{'logdir'}	= $logdir;

	# generic PBS header for all scripts
	my $header	= $self->pbs_header_template();

	my $trimcmd	= $self->pbs_trim_template();
	$trimcmd	= $header.$trimcmd;
	$trimcmd	=~ s/<QUEUE>/mapping/;
	$trimcmd	=~ s/<JOBNAME>/trimfq/;
	$trimcmd	=~ s/<NCPUS>/1/;
	$trimcmd	=~ s/<WALLTIME>/10:00:00/;
	$trimcmd	=~ s/<MEM>/1gb/;

	my $healcmd	= $self->pbs_healfq_template();
	$healcmd	= $header.$healcmd;
	$healcmd	=~ s/<QUEUE>/mapping/;
	$healcmd	=~ s/<JOBNAME>/healfq/;
	$healcmd	=~ s/<NCPUS>/2/;
	$healcmd	=~ s/<WALLTIME>/20:00:00/;
	$healcmd	=~ s/<MEM>/8gb/;

	my $bwamemcmd	= $self->pbs_bwamem_template();
	$bwamemcmd	= $header.$bwamemcmd;
	$bwamemcmd	=~ s/<QUEUE>/mapping/;
	$bwamemcmd	=~ s/<JOBNAME>/bwamem_mp/;
	$bwamemcmd	=~ s/<NCPUS>/8/;
	$bwamemcmd	=~ s/<WALLTIME>/30:00:00/;
	$bwamemcmd	=~ s/<MEM>/15gb/;


	# create directory to put PBS scripts in:
	# /panfs/seq_raw/SLIDENAME/pbs/
	my $seqrawdir	= $self->seq_raw_dir();
	my $pbsdir	= join '/', $seqrawdir, 'pbs/';
	#my $pbsdir	= join '/', $seqrawdir, 'pbstest/';
	$self->{'pbsdir'}	= $pbsdir;
	$self->writelog(BODY => "Creating dir for PBS scripts: $pbsdir");
	$rv		= mkdir $pbsdir unless(-e $pbsdir);
	if($rv != 0 && ! -e $pbsdir) {
		$self->writelog(BODY => "$rv: could not create $pbsdir");
		exit($self->EXIT_NO_PBS());
	}

	# create directory to put SAMs and BAMs in:
	# /panfs/seq_mapped/SLIDENAME/
	my $seqmapdir	= $self->seq_mapped_dir();
	$seqmapdir	= join '/', $seqmapdir;
	$self->{'seqmapdir'}	= $pbsdir;
	$self->writelog(BODY => "Creating seq_mapped dir: $seqmapdir");
	$rv		= mkdir $seqmapdir unless(-e $seqmapdir);
	if($rv != 0 && ! -e $seqmapdir) {
		$self->writelog(BODY => "$rv: could not create $seqmapdir");
		exit($self->EXIT_NO_PBS());
	}

	#print Dumper $self->{'fastq_files'};

	#### FILL TEMPLATES
	my @pbsfiles	= ();
	my @lanenums	= ();
	my $alllanespipeline;
	my $binauto	= $self->{'BIN_AUTO'};
	# should just go through each read 1 file (read 2 file is value)
	foreach my $f (keys %{$self->{'fastq_files'}}) {
		$self->writelog(BODY => "Generating PBS script content for $f");

		# 110614_SN103_0846_BD03UMACXX.lane_1.nobc.1.fastq.gz
		$f			=~ /lane\_(\d)\.(\w+)/;

		# keep track of lanes and indexes to map (by "key" = lane and
		# index concatenated; used for PBS variable job names)
		my $lane		= $1;
		my $index		= $2;
		my $lanekey		= join "", $lane, $index;
		push @lanenums, $lanekey;
		$self->writelog(BODY	=> "Mapset key: $lanekey");

		# get paired FASTQ file
		# 110614_SN103_0846_BD03UMACXX.lane_1.nobc.2.fastq.gz
		my $f2			= $self->{'fastq_files'}->{$f};
		#print STDERR "FASTQ1: $f\nFASTQ2: $f2\n";

		# 110614_SN103_0846_BD03UMACXX.lane_1.nobc
		my $mapset		= $self->infer_mapset(FILE => $f);

		# do not bother mapping if mapset failed QC (as marked in LIMS)
		if($self->get_qc(MAPSET => $mapset) != 0) {
			$self->writelog(BODY => "$mapset failed QC, skipping");
			next;
		}

		# decide if PBS should be made to map this mapset
		$self->writelog(BODY => "Checking if $f should be mapped...");
		my $ar			= $self->get_alignment_required(MAPSET => $mapset);
		if(! $ar && $self->{'is_in_lims'} eq 'N') {
			$ar		= $self->{'lane2alignreq'}->{$lane}->{$index};
		}
		if($ar ne 'Y' && $self->{'force_mapping'} ne 'Y') {
			$self->writelog(BODY => "Alignment not required ($ar), skipping");
			next;
		}

		# decide if BWA should be run or mapsplice for RNA
		my $material		= $self->get_material(MAPSET => $mapset);
		next unless($material =~ /DNA/i);

		# get presumed and anticipated filenames
		my $ref			= $self->get_reference(MAPSET => $mapset);
		if(! $ref) {
			# if there is still no reference genome, don't try to
			# map
			$self->writelog(BODY => "Reference genome not found, skipping");
		}

		#print STDERR "Using reference genome: $ref\n";

		# anticipated seq_mapped BAM
		my $bam			= $self->infer_seqmapped_bam(FILE => $f);
		my $unsortedbam		= $bam;
		$unsortedbam		=~ s/\.bam/\.unsorted\.bam/;
		my $fixrgbam		= $bam;
		$fixrgbam		=~ s/\.bam/\.fixrg\.bam/;
		my $dedupbam		= $bam;
		$dedupbam		=~ s/\.bam/\.deduped\.bam/;
		my $bamprefix		= $bam;
		$bamprefix		=~ s/\.bam//;
		# anticipated seq_results BAM
		#my $newbam		= $self->infer_seqresults_bam(FILE => $f, PATIENT => $self->{'lane2project'}->{$lane}->{$index});
		my $newbam		= $self->infer_seqresults_bam(FILE => $f, MAPSET => $mapset);
		#print STDERR "FILE => $f, PATIENT => $lane, $index -> ".$self->{'lane2project'}->{$lane}->{$index}."\n";

		my $project		= $self->get_project(MAPSET => $mapset);
		if(! $project) {
			# if there no project, don't try to
			# map - there will be no directory to put the files in
			$self->writelog(BODY => "Project name not found, skipping");
		}
		my $library		= $self->get_library(MAPSET => $mapset);
		if(! $library) {
			$library	= $self->{'lane2library'}->{$lane}->{$index};
		}
		my $donor		= $self->get_donor(MAPSET => $mapset);
		if(! $donor) {
			$donor		= $self->{'lane2project'}->{$lane}->{$index};
		}
		my $rgid		= $self->timestamp(FORMAT => 'YYYYMMDDhhmmss');

		### build commands
		# cutadapt
		my $olog	= $logdir.$mapset."_trim1_out.log";
		my $trim1	= $trimcmd;
		my $funzipped	= $f;
		$funzipped	=~ s/\.gz//;
		$trim1		=~ s/<FASTQGZ>/$f/g;
		$trim1		=~ s/<FASTQ>/$funzipped/g;
		$trim1		=~ s/<OUTLOG>/$olog/g;

		$olog	= $logdir.$mapset."_trim2_out.log";
		my $trim2	= $trimcmd;
		my $funzipped2	= $f2;
		$funzipped2	=~ s/\.gz//;
		$trim2		=~ s/<FASTQGZ>/$f2/g;
		$trim2		=~ s/<FASTQ>/$funzipped2/g;
		$trim2		=~ s/<OUTLOG>/$olog/g;

		$olog	= $logdir.$mapset."_healfq_out.log";
		my $healfq	= $healcmd;
		$healfq		=~ s/<FASTQ1>/$funzipped/g;
		$healfq		=~ s/<FASTQ2>/$funzipped2/g;
		$healfq		=~ s/<FASTQGZ1>/$f/g;
		$healfq		=~ s/<FASTQGZ2>/$f2/g;
		$healfq		=~ s/<OUTLOG>/$olog/g;

		# bwa-mem
		$olog	= $logdir.$mapset."_bwamem_out.log";
		my $bwamem	= $bwamemcmd;
		$bwamem		=~ s/<FASTQGZ1>/$f/g;
		$bwamem		=~ s/<FASTQGZ2>/$f2/g if($self->{'is_paired'} eq 'y');
		$bwamem		=~ s/<GENOME>/$ref/g;
		$bwamem		=~ s/<OUTLOG>/$olog/g;
		$bwamem		=~ s/<LIBRARY>/$library/g;
		$bwamem		=~ s/<DONOR>/$donor/g;
		$bwamem		=~ s/<GENOME>/$ref/g;
		$bwamem		=~ s/<BAMPREFIX>/$bamprefix/g;
		$bwamem		=~ s/<NEWBAM>/$newbam/g;
		$bwamem		=~ s/<MAPSET>/$mapset/g;

		$self->writelog(BODY => "Writing PBS files for $mapset");

		# create filename for PBS scripts
		my $basefilename	= $pbsdir.$mapset.".<NAME>.pbs";

		my $pbstrim1		= $basefilename;
		$pbstrim1		=~ s/<NAME>/trim1/;
		$self->write_pbs_file(CMD => $trim1, FILENAME => $pbstrim1);

		my $pbstrim2		= $basefilename;
		$pbstrim2		=~ s/<NAME>/trim2/;
		$self->write_pbs_file(CMD => $trim2, FILENAME => $pbstrim2);

		my $pbshealfq		= $basefilename;
		$pbshealfq		=~ s/<NAME>/healfq/;
		$self->write_pbs_file(CMD => $healfq, FILENAME => $pbshealfq);

		my $pbsbwamem		= $basefilename;
		$pbsbwamem		=~ s/<NAME>/bwamem/;
		$self->write_pbs_file(CMD => $bwamem, FILENAME => $pbsbwamem);

		# now that we know the other script names, we can fill the
		# pipeline template for this lane
		my $pipeline		= $self->pipeline_template_matepair(LANE => $lanekey);
		$pipeline		=~ s/<PBSTRIM1>/$pbstrim1/;
		$pipeline		=~ s/<PBSTRIM2>/$pbstrim2/;
		$pipeline		=~ s/<PBSHEALFQ>/$pbshealfq/;
		$pipeline		=~ s/<PBSBWAMEM>/$pbsbwamem/;

		$alllanespipeline	.= $pipeline;

		# set aligner field in LIMS with 'bwa' for each mapset
		$self->set_lims_aligner(MAPSET => $mapset);
	}

	# create slide-specific template for command to ingest mapped data
	$self->writelog(BODY => "Writing mapped ingest PBS file for $self->{'slide_name'}");
	my $olog	= $logdir.$self->{'slide_name'}."_ingestmapped_out.log";
	my $ingmap	= $self->pbs_ingmap_template();
	$ingmap		= $header.$ingmap;
	$ingmap		=~ s/<JOBNAME>/ingestmapped/;
	$ingmap		=~ s/<QUEUE>/mapping/;
	$ingmap		=~ s/<NCPUS>/1/;
	$ingmap		=~ s/<WALLTIME>/6:00:00/;
	$ingmap		=~ s/<MEM>/600mb/;
	$ingmap		=~ s/<OUTLOG>/$olog/;
	$ingmap		=~ s/<BIN_AUTO>/$binauto/g;
	$ingmap		=~ s/<SLIDE>/$self->{'slide_name'}/;
	my $pbsingmap	= $pbsdir.$self->{'slide_name'}.".ingestmapped.pbs";
	$self->write_pbs_file(CMD => $ingmap, FILENAME => $pbsingmap);

	$self->writelog(BODY => "Writing master PBS file for $self->{'slide_name'}");
	# create one script to run them all; this script just qsubs other
	# scripts and set up dependencies
	$olog	= $logdir.$self->{'slide_name'}."_master_out.log";
	my $master	= $self->pbs_master_template(LANES => \@lanenums);
	$master		= $header.$master;
	$master		=~ s/<JOBNAME>/maphiseq/;
	$master		=~ s/<QUEUE>/mapping/;
	$master		=~ s/<NCPUS>/1/;
	$master		=~ s/<WALLTIME>/00:05:00/;
	$master		=~ s/<MEM>/200mb/;
	$master		=~ s/<OUTLOG>/$olog/;
	$master		=~ s/<PIPELINE>/$alllanespipeline/;
	$master		=~ s/<PBSINGMAP>/$pbsingmap/;
	my $pbsmaster	= $pbsdir.$self->{'slide_name'}.".master.pbs";
	$self->write_pbs_file(CMD => $master, FILENAME => $pbsmaster);

	# save filename of master script
	push @pbsfiles, $pbsmaster;
	$self->{'pbsfiles'}	= \@pbsfiles;

	$self->writelog(BODY => "END: ".$self->timestamp());

	return($status);
}

################################################################################
=pod

B<pbs_header_template()> 
 Return PBS header template

Parameters:

Returns:
 scalar string with generic PBS header containing variables for replacing

=cut
sub pbs_header_template {
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

	my $pbs		= qq{#!/bin/sh 
#PBS -N <JOBNAME>
#PBS -q <QUEUE>
#PBS -r n 
#PBS -l ncpus=<NCPUS>,walltime=<WALLTIME>,mem=<MEM>
#PBS -m ae 
#PBS -j oe
#PBS -o <OUTLOG>
#PBS -W umask=0007
#PBS -M l.fink\@imb.uq.edu.au,s.wood1\@uq.edu.au,s.kazakoff\@uq.edu.au

};

	return($pbs);
}

################################################################################
=pod

B<pipeline_template_matepair()> 
 Return bash script template for all commands for each lane pipelined together
  (these will be written to a master script for the whole slide)

Parameters:
 LANE	=> single digit lane number (1,2,3, etc)

Returns:
 scalar string with generic PBS script containing variables for replacing

=cut
sub pipeline_template_matepair {
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

	my $lane	= $options->{'LANE'};

	my $var1	= 'TRIM_LANE'.$lane.'_R1';
	my $var2	= 'TRIM_LANE'.$lane.'_R2';
	my $var3	= 'HEALFQ_LANE'.$lane;
	my $var4	= 'BWAMEM_LANE'.$lane;

	my $pbs		= qq{
#
$var1=\$(qsub <PBSTRIM1>)
echo \$$var1

$var2=\$(qsub <PBSTRIM2>)
echo \$$var2

$var3=\$(qsub -W depend=afterok:\$$var1:\$$var2 <PBSHEALFQ>)
echo \$$var3

$var4=\$(qsub -W depend=afterok:\$$var3 <PBSBWAMEM>)
echo \$$var4
#

};

	return($pbs);
}

################################################################################
=pod

B<pipeline_template_bwamem()> 
 Return bash script template for all commands for each lane pipelined together
  (these will be written to a master script for the whole slide)

Parameters:
 LANE	=> single digit lane number (1,2,3, etc)

Returns:
 scalar string with generic PBS script containing variables for replacing

=cut
sub pipeline_template_bwamem {
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

	my $lane	= $options->{'LANE'};

	my $var1	= 'BWAMEM_LANE'.$lane;

	my $pbs		= qq{
#
$var1=\$(qsub <PBSBWAMEM>)
echo \$$var1
#

};

	return($pbs);
}

################################################################################
=pod

B<pbs_master_template()> 
 Return bash script template master script to run and manage all other scripts

Parameters:
 LANES	=> ref to array of single digit lane numbers used on slide (1,2,3, etc)

Returns:
 scalar string with generic PBS script containing variables for replacing

=cut
sub pbs_master_template {
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

	my $lanes	= $options->{'LANES'};	# ref to array

	# <LANE> will be replaced by lane and index => 1ATGACG, 4TGCATA
	my $template	= '$BWAMEM_LANE<LANE>';

	my @jobs;
	foreach (@{$lanes}) {
		my $var	= $template;
		$var	=~ s/<LANE>/$_/;
		push @jobs, $var;
	}

	my $job	= join ':', @jobs;

	my $pbs		= qq{
<PIPELINE>

INGMAP=\$(qsub -W depend=afterok:$job <PBSINGMAP>)
echo \$INGMAP

};

	return($pbs);
}

################################################################################
=pod

B<pbs_bwamem_template()> 
 Return PBS template for running bwa-mem
Parameters:

Returns:
 scalar string with generic PBS script containing variables for replacing

=cut
sub pbs_bwamem_template {
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

	my $pbs		= qq{
module load bwa/0.7.5a
module load samtools/0.1.18
module load picard/1.91
module load adama/nightly
module load QCMGPerl

set -e

bwa mem -M -t 8 <GENOME> <FASTQGZ1> <FASTQGZ2> | samtools view -buSt <GENOME>.fai - > <BAMPREFIX>.bam

qbamfix --input <BAMPREFIX>.bam --output <BAMPREFIX>.fixrg.bam --log <BAMPREFIX>.fix.log --RGLB "<LIBRARY>" --RGSM "<DONOR>" --tmpdir \$TMPDIR --CO "\@PG	ID:bwa	PN:bwa	VN:bwa/0.7.5a"

java -jar /share/software/picard-tools-1.62/MarkDuplicates.jar INPUT=<BAMPREFIX>.fixrg.bam OUTPUT=<BAMPREFIX>.deduped.bam METRICS_FILE=<BAMPREFIX>.metric OPTICAL_DUPLICATE_PIXEL_DISTANCE=100 VALIDATION_STRINGENCY=LENIENT REMOVE_DUPLICATES=false ASSUME_SORTED=false MAX_SEQUENCES_FOR_DISK_READ_ENDS_MAP=50000 MAX_FILE_HANDLES_FOR_READ_ENDS_MAP=8000 SORTING_COLLECTION_SIZE_RATIO=0.25 READ_NAME_REGEX=".+:[0-9]+:([A-Z0-9]+):([0-9]+):([0-9]+).*" VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false CREATE_MD5_FILE=false

samtools fillmd -b <BAMPREFIX>.deduped.bam <GENOME> > <BAMPREFIX>.fillmd.bam

cp <BAMPREFIX>.fillmd.bam <NEWBAM>

cp <BAMPREFIX>.metric <NEWBAM>.metric

# make sure BAM copy succeeds
diff <BAMPREFIX>.fillmd.bam <NEWBAM>

set +e

#rm <BAMPREFIX>.bam
#rm <BAMPREFIX>.fixrg.bam
#rm <BAMPREFIX>.fix.log

samtools index <NEWBAM>

chmod 664 <NEWBAM>*

set -e

/share/software/QCMGPerl/distros/automation/sendemail.pl -s "Subject: <MAPSET> BWA MAPPING -- COMPLETED" -b "MAPPING OF <MAPSET> has ended. See log file for status: <OUTLOG>"

};

	return($pbs);
}

################################################################################
=pod

B<pbs_trim_template()> 
 Return PBS template for trimming adapters
Parameters:

Returns:
 scalar string with generic PBS script containing variables for replacing

=cut
sub pbs_trim_template {
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

	my $pbs		= qq{
module load python/2.7.2
module load cutadapt

set -e

gunzip <FASTQGZ> 

cutadapt -b GATCGGAAGAGCACACGTCTGAACTCCAGTCAC -b GATCGGAAGAGCGTCGTGTAGGGAAAGAGTGT -m 20 <FASTQ>          > <FASTQ>.trimtmp1

cutadapt -b CTGTCTCTTATACACATCT               -b AGATGTGTATAAGAGACAG              -m 20 <FASTQ>.trimtmp1 > <FASTQ>.trimtmp2

cutadapt -b TAAGAGACAG -b TGTCTCTTAT          -b ATAAGAGACA -b CTGTCTCTTA         -m 20 <FASTQ>.trimtmp2 > <FASTQ>.trimmed

gzip <FASTQ>.trimmed

};

	return($pbs);
}

################################################################################
=pod

B<pbs_healfq_template()> 
 Return PBS template for healing trimmed FASTQs
Parameters:

Returns:
 scalar string with generic PBS script containing variables for replacing

=cut
sub pbs_healfq_template {
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

	my $pbs		= qq{
module load python/2.7.2

set -e

$ENV{'QCMG_SVN'}/QCMGScripts/c.leonard/utils/fastqmedic_2.py -S 4G --heal -l <FASTQ1>.heal.log <FASTQ1>.trimmed.gz <FASTQ2>.trimmed.gz

mv <FASTQ1>.trimmed.sorted.healed.gz  <FASTQGZ1>
mv <FASTQ2>.trimmed.sorted.healed.gz  <FASTQGZ2>

};

	return($pbs);
}

################################################################################
=pod

B<pbs_ingmap_template()> 
 Return PBS template for ingesting the mapped data for the whole slide

Parameters:

Returns:
 scalar string with generic PBS script containing variables for replacing

=cut
sub pbs_ingmap_template {
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

	my $pbs		= qq{\n<BIN_AUTO>/ingest_mapped_hiseq.pl -i <SLIDE>\n};

	return($pbs);
}

################################################################################
=pod

B<write_pbs_file()> 
 Write PBS file for each barcode

Parameters:
 CMD		=> all commands to be run by PBS script
 FILENAME	=> path and name of file to write

Returns:

=cut
sub write_pbs_file {
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


	my $cmd		= $options->{'CMD'};
	my $filename	= $options->{'FILENAME'};

	$self->writelog(BODY => "\tWriting $filename");

	# Now we are ready to write out the file
	open(FH, ">".$filename) || exit($self->EXIT_NO_PBS());
	print FH $cmd;
	close(FH);

	return();
}

################################################################################
=pod

B<get_library()> 
 Get primary library ID from Geneus for a mapset

Parameters:
 MAPSET	=>  mapset name

Returns:
 scalar string of library name

=cut
sub get_library {
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

	$self->writelog(BODY => "Getting primary library ID for $options->{'MAPSET'}");

	my $md	= QCMG::DB::Metadata->new();
	$md->find_metadata("mapset", $options->{'MAPSET'});	# required for following step to work
	my $lib	= $md->primary_library($options->{'MAPSET'});
	$md	= undef;

	$self->writelog(BODY => "Library ID: $lib\n");

	#if(! $lib) {
	#	exit($self->EXIT_NO_LIB());
	#}

	return($lib);
}

################################################################################
=pod

B<get_libraryprotocol()> 
 Get library protocol from Geneus for a mapset

Parameters:
 MAPSET	=>  mapset name

Returns:
 scalar string of library name

=cut
sub get_libraryprotocol {
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

	$self->writelog(BODY => "Getting library protocol for $options->{'MAPSET'}");

	my $md	= QCMG::DB::Metadata->new();
	$md->find_metadata("mapset", $options->{'MAPSET'});	# required for following step to work
	my $lib	= $md->library_protocol($options->{'MAPSET'});
	$md	= undef;

	$self->writelog(BODY => "Library Protocol: $lib\n");

	#if(! $lib) {
	#	exit($self->EXIT_NO_LIB());
	#}

	return($lib);
}

################################################################################
=pod

B<get_material()> 
 Get source material (DNA/RNA) from Geneus for a mapset

Parameters:
 MAPSET	=>  mapset name

Returns:
 scalar string of material type

=cut
sub get_material {
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

	$self->writelog(BODY => "Getting material type for $options->{'MAPSET'}");

	my $md	= QCMG::DB::Metadata->new();
	$md->find_metadata("mapset", $options->{'MAPSET'});	# required for following step to work
	my $mat	= $md->material($options->{'MAPSET'});
	$md	= undef;

	$self->writelog(BODY => "Material: $mat\n");

	return($mat);
}

################################################################################
=pod

B<get_qc()> 
 Get failed_qc flag from Geneus for a mapset

Parameters:
 MAPSET	=>  mapset name

Returns:
 scalar string of failed_qc flag

Sets:
 $self->{'qc'}->{<MAPSET>}

=cut
sub get_qc {
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

	$self->writelog(BODY => "Getting failed_qc flag for $options->{'MAPSET'}");

	my $md	= QCMG::DB::Metadata->new();
	$md->find_metadata("mapset", $options->{'MAPSET'});	# required for following step to work
	my $d	= $md->mapset_metadata($options->{'MAPSET'});
	my $qc	= $d->{'failed_qc'};

	$self->writelog(BODY => "failed_qc: $qc\n");

	$self->{'qc'}->{$options->{'MAPSET'}}	= $qc;

	return($qc);
}


################################################################################
=pod

B<get_project()> 
 Get project from Geneus for a mapset

Parameters:
 MAPSET	=>  mapset name

Returns:
 scalar string of project name

=cut
sub get_project {
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

	$self->writelog(BODY => "Getting project ID for $options->{'MAPSET'}");

	my $md	= QCMG::DB::Metadata->new();
	$md->find_metadata("mapset", $options->{'MAPSET'});	# required for following step to work
	#my $prj	= $md->project();
	my $prj	= $md->parent_project();
	$md	= undef;

	$self->writelog(BODY => "Project: $prj\n");

	return($prj);
}

################################################################################
=pod

B<get_donor()> 
 Get donor from Geneus for a mapset

Parameters:
 MAPSET	=>  mapset name

Returns:
 scalar string of donor name

=cut
sub get_donor {
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

	$self->writelog(BODY => "Getting donor ID for $options->{'MAPSET'}");

	my $md	= QCMG::DB::Metadata->new();
	$md->find_metadata("mapset", $options->{'MAPSET'});	# required for following step to work
	#my $don	= $md->donor();
	my $don	= $md->project();
	$md	= undef;

	$self->writelog(BODY => "Donor: $don\n");

	return($don);
}

################################################################################
=pod

B<get_alignment_required()> 
 Get value of alignment_required flag from LIMS

 SELECT mapset.name, sample.alignment_required FROM mapset m JOIN sample s ON
  m.sampleid = s.sampleid ... -> TRUE or (NULL/FALSE)

Parameters:
 MAPSET	=>  mapset name

Returns:
 scalar, 'Y' or 'N'

=cut
sub get_alignment_required {
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

	$self->writelog(BODY => "Getting alignment_required flag for $options->{'MAPSET'}");

	# LIMS value = 1 if TRUE, 0 if FALSE, NULL if mapset doesn't exist
	my $md	= QCMG::DB::Metadata->new();
	$md->find_metadata("mapset", $options->{'MAPSET'});	# required for following step to work
	my $ar	= $md->alignment_required($options->{'MAPSET'});
	if($ar =~ /\d/ && $ar == 0) {
		$ar	= 'N';
	}
	else {
		$ar	= 'Y'; # default to TRUE (as LIMS does)
	}
	#print STDERR "map $options->{'MAPSET'}? $ar\n";

	$md	= undef;

	$self->writelog(BODY => "Is alignment required? $ar");

	return($ar);
}

################################################################################
=pod

B<get_reference()> 
 Get value of genome_file field from LIMS

Parameters:
 MAPSET	=>  mapset name

Returns:
 scalar, path to reference genome file

=cut
sub get_reference {
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

	$self->writelog(BODY => "Getting reference genome file for $options->{'MAPSET'}");

	# LIMS value = 1 if TRUE, 0 if FALSE, NULL if mapset doesn't exist
	my $md	= QCMG::DB::Metadata->new();
	$md->find_metadata("mapset", $options->{'MAPSET'});	# required for following step to work
	my $ref	= $md->genome_file($options->{'MAPSET'});
	$md	= undef;

	$self->writelog(BODY => "genome_file: $ref");

	return($ref);
}

################################################################################
=pod

B<mapset2genomefile()> 
 Get path and filename of reference genome for a specified mapset

Parameters:
 MAPSET	=>  mapset name

Returns:
 scalar string of path and filename (on babyjesus)

=cut
sub mapset2genomefile {
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

	$self->writelog(BODY => "Translating reference genome to file for $options->{'MAPSET'}");

	#my $refpath	= '/usr/local/genomes/';
	#if(! -e $refpath) {
		my $refpath	= '/panfs/share/genomes/';
	#}

	print STDERR "REF GENOME: $refpath\n";

	my %genomes	= (
				'GRCh37_ICGC_standard_v2'	=> $refpath.'GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa',
				'GRCh37_ICGC_standard_v2.fa'	=> $refpath.'GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa',
				'Homo sapiens (GRCh37_ICGC_standard_v2)'	=> $refpath.'GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa',
				'hg19'				=> $refpath.'GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa',
				'Homo sapiens (ucsc.hg19)'	=> $refpath.'ucsc.hg19/ucsc.hg19.fasta',
				'ICGC_HG19'			=> $refpath.'GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa',
				'qcmg_mm64'			=> $refpath.'lifescope/referenceData/internal/qcmg_mm64/reference/Mus_musculus.NCBIM37.64.ALL_validated.E.fa',
				'QCMG_MGSCv37.64'		=> $refpath.'lifescope/referenceData/internal/qcmg_mm64/reference/Mus_musculus.NCBIM37.64.ALL_validated.E.fa'
			);

	#print STDERR "TRANSLATING GENOME: $options->{'MAPSET'}\n";

	$options->{'MAPSET'}	=~ /lane\_(\d)\.(\w+)/;
	my $lane	= $1;
	my $index	= $2;
	#$index		= 'NoIndex' if($index eq 'nobc');
	my $ref;

	print Dumper $self->{'lane2ref'};

	foreach (keys %genomes) {
		print STDERR "Checking $_ -> $lane , $index ".$self->{'lane2ref'}->{$lane}->{$index}."\n";
		if($self->{'lane2ref'}->{$lane}->{$index} =~ /$_/) {
			$ref = $genomes{$_};
			#print STDERR "GENOME: $ref\n";
			last;
		}
	}

	$self->writelog(BODY => "Reference genome file: $ref");

	return($ref);
}

################################################################################
=pod

B<infer_mapset()> 
 Infer mapset from slidename and barcode

Parameters:
 FILE	=> a FASTQ file

Returns:
 scalar string of mapset name

=cut
sub infer_mapset {
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

	# 110614_SN103_0846_BD03UMACXX.lane_1.nobc.1.fastq.gz
	# 110614_SN103_0846_BD03UMACXX.lane_1.nobc.1.fastq
	my $file	= $options->{'FILE'};
	my ($v, $d, $f)	= File::Spec->splitpath($file);
	$f		=~ s/\.\d\.fastq\.gz//;
	$f		=~ s/\.\d\.fastq//;	# in case files are not gzipped

	return ($f);
}

################################################################################
=pod

B<infer_fastq()> 
 Infer gunzipped FASTQ filename (remove .gz from filename)

Parameters:
 FILE	=> a FASTQ file

Returns:
 scalar string of file name

=cut
sub infer_fastq {
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

	# 110614_SN103_0846_BD03UMACXX.lane_1.nobc.1.fastq.gz ->
	# 110614_SN103_0846_BD03UMACXX.lane_1.nobc.1.fastq
	my $file	= $options->{'FILE'};
	$file		=~ s/\.gz//;

	return ($file);
}

################################################################################
=pod

B<infer_seqmapped_bam()> 
 Infer BAM filename from slidename and barcode (seq_mapped BAM)

Parameters:
 FILE	=> a FASTQ file

Returns:
 scalar string of anticipated BAM file name, with path

=cut
sub infer_seqmapped_bam {
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

	my $file	= $options->{'FILE'};

	# T00001_20120419_162.nopd.IonXpress_009.bam
	my $mapset	= $self->infer_mapset(FILE => $file);
	my $bam		= join ".", $mapset, "bam";
	$bam		= join "/", $self->SEQ_MAPPED_PATH, $self->{'slide_name'}, $bam;

	return ($bam);
}

################################################################################
=pod

B<infer_seqresults_bam()> 
 Infer BAM filename from slidename and barcode (seq_results BAM)

Parameters:
 FILE		=> a FASTQ file
 MAPSET		=> mapset name

Returns:
 scalar string of anticipated BAM file name, with path

=cut
sub infer_seqresults_bam {
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

	my $file	= $options->{'FILE'};

	#my $patient	= $options->{'PATIENT'};
	#$patient	=~ s/[\-\.]+/\_/g;
	#$patient	=~ y/[a-z]/[A-Z]/;

	my $project	= $self->get_project(MAPSET => $options->{'MAPSET'});
	my $donor	= $self->get_donor(MAPSET => $options->{'MAPSET'});

	# T00001_20120419_162.nopd.IonXpress_009.bam
	my $mapset	= $self->infer_mapset(FILE => $file);
	my $bam		= join ".", $mapset, "bam";
	if($project =~ /smgcore/) {
		$bam		= join "/", $self->SEQ_RESULTS_PATH, "smg_core", $project, $donor, 'seq_mapped', $bam;
	}
	else {
		$bam		= join "/", $self->SEQ_RESULTS_PATH, $project, $donor, 'seq_mapped', $bam;
	}

	return ($bam);
}

################################################################################
=pod

B<set_lims_aligner()> 
 Set aligner field in LIMS with aligner used. This uses an
  external python script.

Parameters:
 MAPSET		=> mapset for which to set aligner field
 MATERIAL	=> optional, material type; if RNA, set aligner to mapsplice; 
			default = bwa for DNA matieral

Returns:

=cut
sub set_lims_aligner {
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

	# 110614_SN103_0846_BD03UMACXX.lane_1.nobc
	my $mapset	= $options->{'MAPSET'};

	my $aligner	= 'bwa';
	#$aligner	= 'mapsplice' if($options->{'MATERIAL'} eq 'RNA');

	# $PATH/gls_client.py -s qcmg-glprod.imb.uq.edu.au mapset_set S0413_20110603_1_FragPEBC.nopd.bcB16_09 "Aligner" bioscope

	my $bin		= qq{export PYTHONPATH=".$self->BIN_GLS_CLIENT ; };
	$bin		= $self->BIN_GLS_CLIENT."gls_client.py";
	my $args	= qq{ -s qcmg-glprod.imb.uq.edu.au mapset_set $mapset "Aligner" $aligner};

	$self->writelog(BODY => "Setting Aligner field in LIMS: $bin $args"); 
	my $rv		= `$bin $args`;
	$self->writelog(BODY => "RV: $rv"); 

	return();
}


################################################################################
=pod

B<check_for_core_samples()> 
 Check LIMS to see if flowcell contains Core Facility samples

Requires:
 $self->{'slide_name'}

Returns:
 scalar - 'Y' or 'N'

=cut
sub check_for_core_samples {
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

	$self->writelog(BODY => "--- Checking to see if $self->{'slide_name'} contains Core Facility samples...");

	my $md	= QCMG::DB::Metadata->new();
	if($md->find_metadata("slide", $self->{'slide_name'})) {
		my $slide_mapsets	= $md->slide_mapsets($self->{'slide_name'});
		foreach my $mapset (@$slide_mapsets) {
			$md->find_metadata("mapset", $mapset);
	
	        	my $d		= $md->mapset_metadata($mapset);

			my $prj		= $d->{'parent_project'};
			$self->writelog(BODY => "$mapset project: $prj");

			# default is set to 'N' in new()
			if($prj	=~ /smgcore/) {
				$self->writelog(BODY => "Core project found: $prj");
				$self->{'contains_core'}	= 'Y';
				last;
			}
		}
	}

	$self->writelog(BODY => "\tcontains_core flag set to $self->{'contains_core'}");

	return($self->{'contains_core'});
}

################################################################################
=pod

B<rsync_to_surf()> 

 Generate a bash script that rsyncs Core Facility BAM files to Surf as well
  as a text file that contains the file names to move and move those files

  ** These files have to be transferred prior to mapping because the first step
     of mapping is gunzipping the files. The current plan is to transfer them
     and allow mapping to be delayed.

There will be no need to make the folder on surf. rsync can do it. If you make a 
file that contains the slide name, a list of BAM files, 
and the cksum file, then you can rsync them with the following (rough) command:

rsync -av --chmod=a-rwx,Dg+rxs,u+rw,Fg+r -e "ssh -i /panfs/home/production/.ssh/id_rsa" uploads qcmg@surf.genome.at.uq.edu.au:/hox/g/imb-taft/qcmg/tests

A sample file list would look like this:
120911_SN7001240_0074_AD1A9AACXX
120911_SN7001240_0074_AD1A9AACXX/SampleSheet.csv
120911_SN7001240_0074_AD1A9AACXX/120911_SN7001240_0074_AD1A9AACXX.cksums
120911_SN7001240_0074_AD1A9AACXX/120911_SN7001240_0074_AD1A9AACXX.lane_1.nobc.bam

Parameters:

Requires:
 $self->{samplesheet}
 $self->{contains_core}
 $self->{slide_name}
 $self->{ck_checksum}
 $self->{ck_filesize}

Returns:

=cut
sub rsync_to_surf {
	my $self = shift;

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }
	
	$self->writelog(BODY => "--- Generating rsync script to transfer files to Surf... ---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	# check to see if any core samples were included on the slide; only
	# transfer if they were
	if($self->{'contains_core'} eq 'N') {
		$self->writelog(BODY => "No Core facility samples included; no transfer required; skipping");
		return();
	}

	# copy sample sheet to /mnt/seq_raw/SLIDE/
	my $s		= $self->{'slide_name'}."_SampleSheet.csv";
	my $cmd		= "cp ".$self->{'samplesheet'}." ".$self->SEQ_RAW_PATH.$self->{'slide_name'}."/".$s;
	my $rv		= system($cmd);
	$self->writelog(BODY => "RV: $rv : $cmd");
	# get filename without path for filesfrom file
	#my ($v, $d, $s)	= File::Spec->splitpath($self->{'samplesheet'});
	
	# bash script to hold the rsync commands
	my $script	= $self->SEQ_RAW_PATH.$self->{'slide_name'}."/".$self->{'slide_name'}."_surf_transfer.sh";
	# text file to list the files to be transferred
	my $filesfrom	= $self->SEQ_RAW_PATH.$self->{'slide_name'}."/".$self->{'slide_name'}."_files.txt";
	# text file with list of checksums for the files being transferred
	my $cksum	= $self->SEQ_RAW_PATH.$self->{'slide_name'}."/".$self->{'slide_name'}.".cksums";
	# get filename without path for filesfrom file
	my ($v, $d, $c)	= File::Spec->splitpath($cksum);

	# create file to write list of files to; also create checksum file
	open(FH, ">".$filesfrom) 	|| print STDERR "Cannot create $filesfrom for writing\n";
	open(CH, ">".$cksum) 		|| print STDERR "Cannot create $cksum for writing\n";

	# print checksum file to list of files to transfer
	print FH $self->{'slide_name'}."\n";
	print FH join "/", $self->{'slide_name'}, $c."\n";

	# print all FASTQ files to list of files to transfer
	foreach my $bam (@{$self->{'bam'}}) {
		my ($v, $d, $f)	= File::Spec->splitpath($bam);

		# 120910_SN7001238_0075_AC14GNACXX.lane_1.nobc.bam
		$f		=~ /^(.+?)\.bam/;
		my $mapset	= $1;

		# do not transfer BAM if mapset failed QC (shouldn't have mapped
		# anyway)
		next if($self->get_qc(MAPSET => $mapset) != 0);

		my $project	= $self->get_project(MAPSET => $mapset);

		if($project =~ /smgcore/) {
			print FH join "/", $self->{'slide_name'}, $f."\n";

			# for each FASTQ file, get checksums and print to
			# checksum file 
			print CH $f."\t".$self->{'ck_checksum'}->{$bam}."\t".$self->{'ck_filesize'}->{$bam}."\n";
		}
	}
	close(FH);
	close(CH);

	# create rsync bash script, only if file list file was created and is
	# not empty
	# then run script
	if(-e $filesfrom && -s $filesfrom > 0) {
		open(FH, ">".$script) || print STDERR "Cannot create $script for writing\n";
		#           rsync -av --chmod=a-rwx,Dg+rxs,u+rw,Fg+r -e "ssh -i /panfs/home/production/.ssh/id_rsa" uploads                               qcmg@surf.genome.at.uq.edu.au:/hox/g/imb-taft/qcmg/tests
		print FH qq{nohup rsync -av --chmod=a-rwx,Dg+rxs,u+rw,Fg+r -e "ssh -i /panfs/home/production/.ssh/id_rsa" --files-from=$filesfrom /mnt/seq_raw/ qcmg\@surf.genome.at.uq.edu.au:/hox/g/imb-taft/qcmg/ &};
		close(FH);

		# run script
		$self->writelog(BODY => "Running $script to transfer files to Surf (list: $filesfrom)");
		my $rv	= system("bash $script");	# UNCOMMENT FOR TESTING
		$self->writelog(BODY => "RV: $rv");	# UNCOMMENT FOR TESTING
	}

	$self->writelog(BODY => "END: ".$self->timestamp());
	$self->writelog(BODY => " --- Done transferring to Surf ---");

	return();
}

################################################################################
=pod

B<run_bwa()> 
 Run bwa using PBS scripts

Parameters:

Requires:
 $self->{'pbsfiles'}

Returns:
 void

=cut
sub run_bwa {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Preparing for bwa runs... ---");

	$BWA_ATTEMPTED			= 'yes';

	# define LiveArc namespace
	$self->{'ns'}		=  join "/", $self->LA_NAMESPACE, $self->{'slide_name'};
	$self->writelog(BODY => "Ingesting PBS script dir to $self->{'ns'}");
	$self->{'la'}	= QCMG::Automation::LiveArc->new(VERBOSE => 1);
	my $status = $self->{'la'}->laimport(		
				NAMESPACE	=> $self->{'ns'},
				ASSET		=> $self->{'slide_name'}."_pbs",
				DIR		=> $self->{'pbsdir'}
			);

	if($status != 0) {
		exit($self->EXIT_LIVEARC_ERROR());
	}

	foreach my $pbs (@{$self->{'pbsfiles'}}) {
		# submit mapping job
		$self->writelog(BODY => "Running bwa run: $pbs");
		print STDERR "Running bwa run: $pbs\n";
		#my $rv = `qsub $pbs`;
		my $rv	= `bash $pbs`;
		### ADD ERROR CHECKING!!!
		$rv =~ /^(\d+)\./;
		my $jobid = $1;
		$self->writelog(BODY => "Ran $pbs: $rv , job id: $jobid");

		my $subj	= $self->{'slide_name'}." BWA.PBS SUBMITTED";
		my $body	= "BWA.PBS SUBMITTED: ".$pbs."\n\nJob IDs: ".$rv;
		#$self->send_email(FROM => 'mediaflux@imq.uq.edu.au', TO => $self->DEFAULT_EMAIL, SUBJECT => $subj, BODY => $body);
	}

	$self->writelog(BODY => "Completed bwa run job submissions");

	$BWA_SUCCEEDED			= 'yes';

	return();
}


################################################################################
=pod

B<is_paired()>

Parse RunInfo.xml file to determine if this is a paired-end run or not

Parameters:

Requires:
 $self->{'slide_folder'}

Returns:
 $self->{'is_paired'}

Sets:
 $self->{'is_paired'}

=cut
sub is_paired {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Checking RunInfo.xml for paired end status ---");

	#                  /mnt/HiSeqNew/runs/130214_7001243_0153_BD1JKMACXX/RunInfo.xml
	my $runinfo	= "/mnt/HiSeqNew/runs/".$self->{'slide_name'}."/RunInfo.xml";
	local($/)	= undef;
	open(FH, $runinfo);
	my $fc		= <FH>;
	close(FH);

	$self->{'is_paired'}	= 'y';
	if($fc =~ /<Read Number="1"/ && $fc =~ /<Read Number="2" NumCycles="(\d+)" IsIndexedRead="Y"/ && $fc !~ /<Read Number="3"/) {
		$self->{'is_paired'}	= 'n';
	}

	return($self->{'is_paired'});
}

################################################################################
=pod

B<is_matepair()>

Parse RunInfo.xml file to determine if this is a matepair-end run or not

Parameters:

Requires:
 MAPSET 

Returns:
 $self->{'is_matepair'}

Sets:
 $self->{'is_matepair'}

=cut
sub is_matepair {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $lib	= $self->get_libraryprotocol(MAPSET => $options->{'MAPSET'});

	$self->{'is_matepair'}	= 'n';

	if($lib =~ /Mate Pair/) {
		$self->{'is_matepair'}	= 'y';
	}

	return($self->{'is_matepair'});
}

################################################################################
=pod

B<runinfo()>

Return path to RunInfo.xml file

Parameters:

Requires:
 $self->{'slide_folder'}

Returns:
 scalar path

Sets:
 $self->{'runinfo'}

=cut
sub runinfo {
	my $self	= shift @_;

	$self->writelog(BODY => "Finding RunInfo.xml file...");

	# should be under slide level directory:
	# /path.../110614_SN103_0846_BD03UMACXX.images/

	my $path	= $self->{'slide_folder'}."/RunInfo.xml";

	if(-e $path && -r $path) {
		$self->{'runinfo'}	= $path;
		$self->writelog(BODY => "Found $path");
	}
	else {
		$self->writelog(BODY => "No RunInfo.xml file found; tried
$path\n");
		exit($self->EXIT_NO_METAFILES());
	}

	return($self->{'runinfo'});
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
	$msg		.= qq{TOOLLOG: SLIDE_FOLDER $self->{'slide_folder'}\n};
	$msg		.= qq{TOOLLOG: DEFAULT_EMAIL }.$self->DEFAULT_EMAIL."\n";
	$msg		.= qq{TOOLLOG: SLIDE_NAME }.$self->{'slide_name'}."\n";

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

	if(! $self->{'EXIT'}) {
		$self->{'EXIT'} = 0;
	}

	unless($self->{'EXIT'} > 0) {
		if($BWA_ATTEMPTED eq 'yes') {
			$self->writelog(BODY => "EXECLOG: errorMessage none");

			$self->{'EXIT'} = 0;

			# send success email if no errors
			my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- SUCCEEDED";
			my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'} complete";
			$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			#$self->send_email(SUBJECT => $subj, BODY => $body);
		}
		# running bwa was not attempted; script ended for some other
		# reason
		elsif($BWA_ATTEMPTED eq 'no') {
			$self->writelog(BODY => "EXECLOG: errorMessage bwa run not attempted");
	
			$self->{'EXIT'} = 17;
	
			# send success email if no errors
			my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- NOT ATTEMPTED";
			my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'} not attempted";
			$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			#$self->send_email(SUBJECT => $subj, BODY => $body);
		}
	}

	# complete log file after successful run

	my $eltime	= ($self->elapsed_time ? $self->elapsed_time : 0);
	my $date	= $self->timestamp();

	$self->writelog(BODY => "EXECLOG: elapsedTime $eltime");
	$self->writelog(BODY => "EXECLOG: stopTime $date");
	$self->writelog(BODY => "EXECLOG: exitStatus $self->{'EXIT'}");

	close($self->{'log_fh'});

	# if exit value is 0, ingest log file as a new asset in same namespace
	if($self->{'EXIT'} == 0) {
		# ingest this log file
		#my $asset	= $self->{'slide_name'}.$self->LA_ASSET_SUFFIX_RUNBWA_LOG;
		my $asset	= $self->{'slide_name'}."_runbwa_log";

		my $assetid = $self->{'la'}->asset_exists(NAMESPACE => $self->{'ns'}, ASSET => $asset);
		my $status;
		if(! $assetid) {
			$assetid = $self->{'la'}->create_asset(NAMESPACE => $self->{'ns'}, ASSET => $asset);
		}
		$status = $self->{'la'}->update_asset_file(ID => $assetid, FILE => $self->{'log_file'});
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
