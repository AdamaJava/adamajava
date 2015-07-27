package QCMG::RNAseq::Run;

##############################################################################
#
#  Module:   QCMG::RNAseq::Run.pm
#  Creator:  Lynn Fink
#  Created:  2013-03-19
#
#  This class contains methods for automating the running of programs on raw
#  RNA sequencing data files
#
# $Id$
#
##############################################################################


=pod

=head1 NAME

QCMG::RNAseq::Run -- Common functions for running RNAseq mapping and QC programs

=head1 SYNOPSIS

Most common use:
 my $qi = QCMG::RNAseq::Run->new();

=head1 DESCRIPTION

Contains methods for checking that raw sequencing data files are ready for
mapping and then running mapping and QC programs on those files

=head1 REQUIREMENTS

 Exporter
 File::Spec
 POSIX 'strftime'
 QCMG::Automation::LiveArc
 QCMG::Automation::Config
 QCMG::DB::Geneus

=cut

$ENV{'PERL5LIB'} = "/share/software/QCMGPerl/lib/";

use strict;

# standard distro modules
use Data::Dumper;
use File::Spec;				# for parsing paths
use Text::ParseWords;

# in-house modules
use QCMG::Automation::LiveArc;		# for accessing Mediaflux/LiveArc
use QCMG::DB::Metadata;

use QCMG::Automation::Common;
our @ISA = qw(QCMG::Automation::Common);	# inherit Common methods

use vars qw( $SVNID $REVISION $MAPPING_ATTEMPTED $MAPPING_SUCCEEDED $IMPORT_ATTEMPTED $IMPORT_SUCCEEDED );

################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 SLIDE_FOLDER	=> directory
 LOG_FILE   	=> path and filename of log file
 FORCE_MAP	=> override LIMS setting and force mapset to be mapped (Y/N)
 RSEM		=> 'Y'/'N'	(default = Y)
 MAPSPLICE	=> 'Y'/'N'	(default = Y)
 QC		=> 'Y'/'N'	(default = Y)

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
		$self->{'log_file'}		= $self->AUTOMATION_LOG_DIR."/".$self->{'slide_name'}."_runrnaseq.log";
	}

	# decide which programs to run; bwa is always run
	$self->{'run_rsem'}			= 'Y';
	if($options->{'RSEM'} !~ /y/i) {
		$self->{'run_rsem'} 		= 'N';
	}
	$self->{'run_mapsplice'}		= 'Y';
	if($options->{'MAPSPLICE'} !~ /y/i) {
		$self->{'run_mapsplice'} 	= 'N';
	}
	$self->{'run_qc'}			= 'Y';
	if($options->{'QC'} !~ /y/i) {
		$self->{'run_qc'} 		= 'N';
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
	$MAPPING_ATTEMPTED = 'no';
	$MAPPING_SUCCEEDED = 'no';

	return $self;
}

# Exit codes:
sub EXIT_NO_FOLDER {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RNA-seq processing -- FAILED";
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
	my $subj	= $self->{'slide_name'}." RNA-seq processing -- FAILED";
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
	my $subj	= $self->{'slide_name'}." RNA-seq processing -- FAILED";
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
	my $subj	= $self->{'slide_name'}." RNA-seq processing -- FAILED";
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
	my $subj	= $self->{'slide_name'}." RNA-seq processing -- FAILED";
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
	my $subj	= $self->{'slide_name'}." RNA-seq processing -- FAILED";
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
	my $subj	= $self->{'slide_name'}." RNA-seq processing -- FAILED";
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
	my $subj	= $self->{'slide_name'}." RNA-seq processing -- FAILED";
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
	my $subj	= $self->{'slide_name'}." RNA-seq processing -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: PBS log file directory could not be created";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 57;
	return 57;
}
sub EXIT_NO_SEQMAPPED {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RNA-seq processing -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: seq_mapped directory could not be created";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 66;
	return 66;
}
sub EXIT_NO_FASTQ {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RNA-seq processing -- FAILED";
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
	my $subj	= $self->{'slide_name'}." RNA-seq processing -- FAILED";
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
	my $subj	= $self->{'slide_name'}." RNA-seq processing -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: Missing library";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 62;
	return 62;
}
sub EXIT_NO_SEQRESDIR {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RNA-seq processing -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: Could not find or create seq_results directory";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 61;
	return 61;
}
sub EXIT_NO_FILELIST {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RNA-seq processing -- FAILED";
	my $body	= "SUBMISSION OF QC JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: Could not create file with a list of BAMs";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 63;
	return 63;
}
sub EXIT_NO_PROJECT {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RNA-seq processing -- FAILED";
	my $body	= "SUBMISSION OF QC JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: No project listed in LIMS";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 64;
	return 64;
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
				#exit($self->EXIT_NO_FASTQ());
			}

			# pair both read ends as key/value pair in hash
			$fastq{$file}	= $pairedfile;
		}
		else {
			#print STDERR "Skipping $file\n";
			delete $fastq{$file};
		}
	}

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

=cut
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
		$self->{'samplesheet'}	= $csv;
		$self->writelog(BODY => "Sample sheet found");
	}
	else {
		$self->writelog(BODY => "Sample sheet not found");
	}

	#print STDERR "SAMPLE SHEET: $self->{'samplesheet'}\n";

	return($self->{'samplesheet'});
}

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

=cut
sub read_samplesheet {
        my $self = shift @_;

	$self->writelog(BODY => "---Reading SampleSheet.csv...---");

	my $sscsv;
	if(! $self->{'samplesheet'}) {
		$sscsv	= $self->samplesheet();
	}
	else {
		$sscsv	= $self->{'samplesheet'};
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
			#my $project	= $d->{'project'};
			#my $donor	= $d->{'donor'};
			my $project	= $d->{'parent_project'};
			my $donor	= $d->{'project'};
	
			# !!!!!!!!!!!!!!!! FIX !!!!!!!!!!!!!!!!!!!!!!!!!!!1
			my $seqresdir;
			if($project =~ /smgcore/) {
				$seqresdir	= $self->SEQ_RESULTS_PATH.'/smg_core/'.$project;
			}
			else {
				$seqresdir	= $self->SEQ_RESULTS_PATH.'/'.$project;
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

B<create_bwaqc_pbs_scripst()> 
 Create PBS scripts that will run bwa, create indexes, and copy BAMs to the
  appropriate place

Parameters:

Sets:
 $self->{'logdir'}

Requires:
 $self->{'fastq'}

Returns:

=cut
sub create_bwaqc_pbs_scripts {
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

	# get reference genome for each lane and each patient ID (?)
	$self->read_samplesheet();

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

	# create two scripts to run bwa aln for each read number
	my $bwaalncmd	= $self->pbs_bwaaln_template();
	$bwaalncmd	= $header.$bwaalncmd;
	$bwaalncmd	=~ s/<QUEUE>/mapping/;
	$bwaalncmd	=~ s/<JOBNAME>/rna_bwaaln/;
	$bwaalncmd	=~ s/<NCPUS>/4/;
	$bwaalncmd	=~ s/<WALLTIME>/100:00:00/;
	$bwaalncmd	=~ s/<MEM>/6gb/;

	# create one script to run bwa sampe for both read numbers
	my $bwasampecmd	= $self->pbs_bwasampe_template();
	$bwasampecmd	= $header.$bwasampecmd;
	$bwasampecmd	=~ s/<QUEUE>/mapping/;
	$bwasampecmd	=~ s/<JOBNAME>/rna_bwasampe/;
	$bwasampecmd	=~ s/<NCPUS>/1/;
	$bwasampecmd	=~ s/<WALLTIME>/100:00:00/;
	$bwasampecmd	=~ s/<MEM>/6gb/;

	# create one script to run samtools to make BAM, BAM index and to start
	# ingestion of mapped data
	my $sam2bamcmd	= $self->pbs_sam2bam_template();
	$sam2bamcmd	= $header.$sam2bamcmd;
	$sam2bamcmd	=~ s/<QUEUE>/batch/;
	$sam2bamcmd	=~ s/<JOBNAME>/rna_sam2bam/;
	$sam2bamcmd	=~ s/<NCPUS>/1/;
	$sam2bamcmd	=~ s/<WALLTIME>/100:00:00/;
	$sam2bamcmd	=~ s/<MEM>/22gb/;

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
	# !!!!!!!!!!!!!! FIX??? !!!!!!!!!!!!!!!!!!!!!!!!
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
	my @mapsets	= ();
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
		# get list of all mapsets for QC program
		push @mapsets, $mapset;

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
		next unless($material =~ /RNA/i);

		# get presumed and anticipated filenames
		my $fastqunzipped	= $self->infer_fastq(FILE => $f);
		my $fastqunzipped2	= $self->infer_fastq(FILE => $f2);
		my $sai			= $self->infer_sai(FILE => $f);
		my $sai2		= $self->infer_sai(FILE => $f2);
		my $sam			= $self->infer_sam(FILE => $f);
		my $ref			= $self->get_reference(MAPSET => $mapset);
		if(! $ref && $self->{'is_in_lims'} eq 'N') {
			# if reference is found but sample is not LIMS, get
			# the reference from the sample sheet
			$ref		= $self->mapset2genomefile(MAPSET => $mapset);
		}
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
		#my $newbam		= $self->infer_seqmapped_bam(FILE => $f, MAPSET => $mapset);
		#print STDERR "FILE => $f, PATIENT => $lane, $index -> ".$self->{'lane2project'}->{$lane}->{$index}."\n";

		my $project		= $self->get_project(MAPSET => $mapset);
		if(! $project) {
			# if there no project, don't try to
			# map - there will be no directory to put the files in
			$self->writelog(BODY => "Project name not found!");
			exit($self->EXIT_NO_PROJECT());
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
		# bwa aln for read number 1
		my $olog		= $logdir.$mapset."_bwaaln1_out.log";
		my $bwaaln		= $bwaalncmd;
		$bwaaln			=~ s/<FASTQGZ>/$f/g;
		$bwaaln			=~ s/<FASTQ>/$fastqunzipped/g;
		$bwaaln			=~ s/<SAI>/$sai/g;
		$bwaaln			=~ s/<GENOME>/$ref/g;
		$bwaaln			=~ s/<OUTLOG>/$olog/g;


		# bwa aln for read number 2
		$olog		= $logdir.$mapset."_bwaaln2_out.log";
		my $bwaaln2		= $bwaalncmd;
		$bwaaln2		=~ s/<FASTQGZ>/$f2/g;
		$bwaaln2		=~ s/<FASTQ>/$fastqunzipped2/g;
		$bwaaln2		=~ s/<SAI>/$sai2/g;
		$bwaaln2		=~ s/<GENOME>/$ref/g;
		$bwaaln2		=~ s/<OUTLOG>/$olog/g;


		# bwa sampe
		$olog		= $logdir.$mapset."_bwasampe_out.log";
		my $bwasampe		= $bwasampecmd;
		# bwa sampe -r '\@RG\tID:<RGID>\tPL:Illumina\tPU:lane_<LANE>\tLB:<LIBRARY>\tSM:<DONOR>' <GENOME> <SAI1> <SAI2> <FASTQ1> <FASTQ2> > <SAM>
		$bwasampe		=~ s/<SAI1>/$sai/g;
		$bwasampe		=~ s/<SAI2>/$sai2/g;
		$bwasampe		=~ s/<FASTQ1>/$fastqunzipped/g;
		$bwasampe		=~ s/<FASTQ2>/$fastqunzipped2/g;
		$bwasampe		=~ s/<FASTQ1GZ>/$f/g;
		$bwasampe		=~ s/<FASTQ2GZ>/$f2/g;
		$bwasampe		=~ s/<SAM>/$sam/g;
		$bwasampe		=~ s/<GENOME>/$ref/g;
		$bwasampe		=~ s/<RGID>/$rgid/g;
		$bwasampe		=~ s/<LANE>/$lane/g;	# must be integer
		$bwasampe		=~ s/<LIBRARY>/$library/g;
		$bwasampe		=~ s/<DONOR>/$donor/g;
		$bwasampe		=~ s/<OUTLOG>/$olog/g;


		# samtools +
		$olog		= $logdir.$mapset."_sam2bam_out.log";
		my $sam2bam		= $sam2bamcmd;
		$sam2bam		=~ s/<SAM>/$sam/g;
		$sam2bam		=~ s/<UNSORTEDBAM>/$unsortedbam/g;
		$sam2bam		=~ s/<FIXRGBAM>/$fixrgbam/g;
		$sam2bam		=~ s/<DEDUPBAM>/$dedupbam/g;
		$sam2bam		=~ s/<LIBRARY>/$library/g;
		$sam2bam		=~ s/<DONOR>/$donor/g;
		$sam2bam		=~ s/<GENOME>/$ref/g;
		$sam2bam		=~ s/<BAMPREFIX>/$bamprefix/g;
		$sam2bam		=~ s/<NEWBAM>/$newbam/g;
		$sam2bam		=~ s/<BAM>/$bam/g;
		$sam2bam		=~ s/<BIN_AUTO>/$binauto/g;
		$sam2bam		=~ s/<MAPSET>/$mapset/g;
		$sam2bam		=~ s/<OUTLOG>/$olog/g;


		$self->writelog(BODY => "Writing PBS files for $mapset");

		# create filename for PBS scripts
		my $basefilename	= $pbsdir.$mapset.".<NAME>.pbs";

		my $pbsbwaaln1		= $basefilename;
		$pbsbwaaln1		=~ s/<NAME>/bwaaln1/;
		$self->write_pbs_file(CMD => $bwaaln, FILENAME => $pbsbwaaln1);

		my $pbsbwaaln2		= $basefilename;
		$pbsbwaaln2		=~ s/<NAME>/bwaaln2/;
		$self->write_pbs_file(CMD => $bwaaln2, FILENAME => $pbsbwaaln2);

		my $pbsbwasampe		= $basefilename;
		$pbsbwasampe		=~ s/<NAME>/bwasampe/;
		$self->write_pbs_file(CMD => $bwasampe, FILENAME => $pbsbwasampe);

		my $pbssam2bam		= $basefilename;
		$pbssam2bam		=~ s/<NAME>/sam2bam/;
		$self->write_pbs_file(CMD => $sam2bam, FILENAME => $pbssam2bam);

		# now that we know the other script names, we can fill the
		# pipeline template for this lane
		my $pipeline		= $self->pipeline_template(LANE => $lanekey);
		$pipeline		=~ s/<PBSBWAALN1>/$pbsbwaaln1/;
		$pipeline		=~ s/<PBSBWAALN2>/$pbsbwaaln2/;
		$pipeline		=~ s/<PBSBWASAMPE>/$pbsbwasampe/;
		$pipeline		=~ s/<PBSSAM2BAM>/$pbssam2bam/;

		$alllanespipeline	.= $pipeline;

		# set aligner field in LIMS for each mapset
		my $aligner	= "bwa";
		$self->writelog(BODY => "Setting aligner field in LIMS: $aligner");
		$self->set_lims_aligner(MAPSET => $mapset);
	}

	## This is terrible, but it works...
	# make list of BAMs for QC program
	my ($bamlists, $samples)	= $self->write_qc_filelist(MAPSETS => \@mapsets); # -> THIS IS NOW A REF TO ARRAY
	my $qc_command	= '';
	my @qc_pbs	= ();
	my @donors	= ();
	for my $i (0..$#{$bamlists}) {
		# get donor from filename
		# /mnt/seq_mapped/130227_7001238_0114_BC1ELJACXX/SLJS_Q734_qc_filelist.txt
		my ($v, $d, $f)	= File::Spec->splitpath($bamlists->[$i]);
		$f		=~ /(.+?)\_qc\_filelist\.txt$/;
		my $donor	= $1;
		my $sample	= $samples->[$i];
		
		my $qc		= $self->pbs_qc_template();
		my $olog	= $logdir.$self->{'slide_name'}."_".$donor."_".$sample."_qc_out.log";
		$qc		= $header.$qc;
		$qc		=~ s/<JOBNAME>/rna_qc/;
		$qc		=~ s/<QUEUE>/batch/;
		$qc		=~ s/<NCPUS>/1/;
		$qc		=~ s/<WALLTIME>/20:00:00/;
		$qc		=~ s/<MEM>/22gb/;
		$qc		=~ s/<BAMLIST>/$bamlists->[$i]/g;
		$qc		=~ s/<DONOR>/$donor/g;
		$qc		=~ s/<SEQMAPPED>/$seqmapdir/g;
		$qc		=~ s/<SLIDE>/$self->{'slide_name'}/g;
		$qc		=~ s/<OUTLOG>/$olog/g;
		my $pbsqc	= $pbsdir.$self->{'slide_name'}.".nopd.".$donor.".qc.pbs";
		$self->write_pbs_file(CMD => $qc, FILENAME => $pbsqc);
		push @qc_pbs, $pbsqc;
		push @donors, $donor;
	}
	######

	# create slide-specific template for command to ingest mapped data
	$self->writelog(BODY => "Writing mapped ingest PBS file for $self->{'slide_name'}");
	my $olog	= $logdir.$self->{'slide_name'}."_ingestmapped_out.log";
	my $ingmap	= $self->pbs_ingmap_template();
	$ingmap		= $header.$ingmap;
	$ingmap		=~ s/<JOBNAME>/rna_ingestbwa/;
	$ingmap		=~ s/<QUEUE>/batch/;
	$ingmap		=~ s/<NCPUS>/1/;
	$ingmap		=~ s/<WALLTIME>/6:00:00/;
	$ingmap		=~ s/<MEM>/1gb/;
	$ingmap		=~ s/<OUTLOG>/$olog/;
	$ingmap		=~ s/<BIN_AUTO>/$binauto/g;
	$ingmap		=~ s/<SLIDE>/$self->{'slide_name'}/;
	my $pbsingmap	= $pbsdir.$self->{'slide_name'}.".ingestmapped.pbs";
	$self->write_pbs_file(CMD => $ingmap, FILENAME => $pbsingmap);

	# create slide-specific template for command to ingest QC data
	$self->writelog(BODY => "Writing mapped ingest PBS file for $self->{'slide_name'}");
	$olog	= $logdir.$self->{'slide_name'}."_ingestqc_out.log";
	my $ingqc	= $self->pbs_ingqc_template();
	$ingqc		= $header.$ingqc;
	$ingqc		=~ s/<JOBNAME>/rna_ingestqc/;
	$ingqc		=~ s/<QUEUE>/batch/;
	$ingqc		=~ s/<NCPUS>/1/;
	$ingqc		=~ s/<WALLTIME>/6:00:00/;
	$ingqc		=~ s/<MEM>/1gb/;
	$ingqc		=~ s/<OUTLOG>/$olog/;
	$ingqc		=~ s/<BIN_AUTO>/$binauto/g;
	$ingqc		=~ s/<SLIDE>/$self->{'slide_name'}/;
	my $pbsingqc	= $pbsdir.$self->{'slide_name'}.".ingestqc.pbs";
	$self->write_pbs_file(CMD => $ingqc, FILENAME => $pbsingqc);

	$self->writelog(BODY => "Writing master PBS file for $self->{'slide_name'}");
	# create one script to run them all; this script just qsubs other
	# scripts and set up dependencies
	$olog	= $logdir.$self->{'slide_name'}."_bwaqcmaster_out.log";
	my $master	= $self->pbs_master_template(LANES => \@lanenums, DONORS => \@donors, QCPBS => \@qc_pbs);
	$master		= $header.$master;
	$master		=~ s/<JOBNAME>/rna_master_bwaqc/;
	$master		=~ s/<QUEUE>/mapping/;
	$master		=~ s/<NCPUS>/1/;
	$master		=~ s/<WALLTIME>/00:05:00/;
	$master		=~ s/<MEM>/200mb/;
	$master		=~ s/<OUTLOG>/$olog/;
	$master		=~ s/<PIPELINE>/$alllanespipeline/;
	$master		=~ s/<PBSINGMAP>/$pbsingmap/;
	$master		=~ s/<PBSINGQC>/$pbsingqc/;
	my $pbsmaster	= $pbsdir.$self->{'slide_name'}.".bwaqcmaster.pbs";
	$self->write_pbs_file(CMD => $master, FILENAME => $pbsmaster);

	# save filename of master script
	#push @pbsfiles, $pbsmaster;
	#$self->{'pbsfiles'}	= \@pbsfiles;
	#push @{$self->{'pbsfiles'}, $pbsmaster;
	push @{$self->{'pbsfiles'}}, $pbsmaster;

	$self->writelog(BODY => "END: ".$self->timestamp());

	return($status);
}

################################################################################
=pod

B<create_rsemmapsplice_pbs_scripst()> 
 Create PBS scripts that will run mapsplice and rsem and copy BAMs to the appropriate place

Parameters:

Sets:
 $self->{'logdir'}
 $self->{'rsemmapsplice_pbsfiles'}

Requires:
 $self->{'fastq'}

Returns:

=cut
sub create_rsemmapsplice_pbs_scripts {
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

	my $status 		= 0;

	$self->writelog(BODY => "---Creating PBS scripts for rsem and mapsplice---");
	$self->writelog(BODY => "START: ".$self->timestamp());


	# create directory to put PBS scripts in
	my $seqrawdir		= $self->seq_raw_dir();
	my $pbsdir		= join '/', $seqrawdir, 'pbs/';
	$self->{'pbsdir'}	= $pbsdir;
	$self->writelog(BODY => "Creating dir for PBS scripts: $pbsdir");
	my $rv			= mkdir $pbsdir unless(-e $pbsdir);
	if($rv != 0 && ! -e $pbsdir) {
		$self->writelog(BODY => "$rv: could not create $pbsdir");
		exit($self->EXIT_NO_PBS());
	}
	# create log file directory
	my $logdir		= $self->seq_raw_dir()."/log/";
	my $rv			= mkdir $logdir;
	if($rv != 0 && ! -e $logdir) {
		$self->writelog(BODY => "Cannot create log directory: $logdir (rv: $rv)");
		exit($self->EXIT_NO_LOGDIR());
	}
	$self->{'logdir'}	= $logdir;
	# create directory to put SAMs and BAMs in:
	my $seqmapdir		= $self->seq_mapped_dir();
	$seqmapdir		= join '/', $seqmapdir;
	$self->{'seqmapdir'}	= $pbsdir;
	$self->writelog(BODY => "Creating seq_mapped dir: $seqmapdir");
	$rv			= mkdir $seqmapdir unless(-e $seqmapdir);
	if($rv != 0 && ! -e $seqmapdir) {
		$self->writelog(BODY => "$rv: could not create $seqmapdir");
		exit($self->EXIT_NO_SEQMAPPED());
	}


	# generic PBS header for all scripts
	my $header		= $self->pbs_header_template();
	# create one script header to sanitize FASTQ files
	my $sanitize_fastqcmd	= $self->pbs_sanitize_fastq_template();
	$sanitize_fastqcmd	= $header.$sanitize_fastqcmd;
	$sanitize_fastqcmd	=~ s/<QUEUE>/batch/;
	$sanitize_fastqcmd	=~ s/<JOBNAME>/rna_sanitizefastq/;
	$sanitize_fastqcmd	=~ s/<NCPUS>/1/;
	$sanitize_fastqcmd	=~ s/<WALLTIME>/1:00:00/;
	$sanitize_fastqcmd	=~ s/<MEM>/1gb/;
	# create one script header to run rsem 
	my $rsemcmd		= $self->pbs_rsem_template();
	$rsemcmd		= $header.$rsemcmd;
	$rsemcmd		=~ s/<QUEUE>/batch/;
	$rsemcmd		=~ s/<JOBNAME>/rna_rsem/;
	$rsemcmd		=~ s/<NCPUS>/8/;
	$rsemcmd		=~ s/<WALLTIME>/100:00:00/;
	$rsemcmd		=~ s/<MEM>/20gb/;
	# create one script header to run mapsplice 
	my $mapsplicecmd	= $self->pbs_mapsplice_template();
	$mapsplicecmd		= $header.$mapsplicecmd;
	$mapsplicecmd		=~ s/<QUEUE>/batch/;
	$mapsplicecmd		=~ s/<JOBNAME>/rna_mapsplice/;
	$mapsplicecmd		=~ s/<NCPUS>/8/;
	$mapsplicecmd		=~ s/<WALLTIME>/100:00:00/;
	$mapsplicecmd		=~ s/<MEM>/12gb/;


	$self->read_samplesheet();

	#### FILL TEMPLATES
	my @pbsfiles		= ();
	my @lanenums		= ();
	my $alllanespipeline	= '';
	my @sanfqqsubvars	= ();
	my @rsemqsubvars	= ();
	my @mapspliceqsubvars	= ();
	my $binauto		= $self->{'BIN_AUTO'};
	my %donorsample2mapset	= ();	# hash of array refs; key = donor_sample, value = ref to array of mapsets
	my %donor2mapset	= ();	# hash of array refs; key = donor,        value = ref to array of mapsets
	# should just go through each read 1 file (read 2 file is value)
	#print Dumper $self->{'fastq_files'};
	foreach my $f (keys %{$self->{'fastq_files'}}) {
		$self->writelog(BODY => "Finding donor for $f");
		# 110614_SN103_0846_BD03UMACXX.lane_1.nobc.1.fastq.gz
		$f			=~ /lane\_(\d)\.(\w+)/;

		# keep track of lanes and indexes to map (by "key" = lane and
		# index concatenated; used for PBS variable job names)
		my $lane		= $1;
		my $index		= $2;
		my $lanekey		= join "", $lane, $index;
		#push @lanenums, $lanekey;
		#$self->writelog(BODY	=> "Mapset key: $lanekey");

		# 110614_SN103_0846_BD03UMACXX.lane_1.nobc
		my $mapset		= $self->infer_mapset(FILE => $f);

		# decide if BWA should be run or mapsplice for RNA
		my $material		= $self->get_material(MAPSET => $mapset);
		next unless($material =~ /RNA/i);

		# decide if we should map this mapset
		$self->writelog(BODY => "Checking if $f should be mapped...");
		my $ar			= $self->get_alignment_required(MAPSET => $mapset);
		if(! $ar && $self->{'is_in_lims'} eq 'N') {
			$ar		= $self->{'lane2alignreq'}->{$lane}->{$index};
		}
		if($ar ne 'Y' && $self->{'force_mapping'} ne 'Y') {
			$self->writelog(BODY => "Alignment not required ($ar), skipping");
			next;
		}

		# get paired FASTQ file
		# 110614_SN103_0846_BD03UMACXX.lane_1.nobc.2.fastq.gz
		my $f2			= $self->{'fastq_files'}->{$f};


		my $donor		= $self->get_donor(MAPSET => $mapset);
		my $sample		= $self->get_sample(MAPSET => $mapset);
		if(! $donor) {
			$donor		= $self->{'lane2project'}->{$lane}->{$index};
		}
		my $donorsample		= $donor."_".$sample;

		push @{ $donor2mapset{$donor} }, $mapset;
		push @{ $donorsample2mapset{$donorsample} }, $mapset;

		### build commands
		my $olog		= $logdir.$mapset."_sanitize_fastq_out.log";
		my $sanitize_fastq	= $sanitize_fastqcmd;
		my $fastq1suffix	= $f;
		$fastq1suffix		=~ s/fastq\.gz/suffix\.fastq/;
		my $fastq2suffix	= $f2;
		$fastq2suffix		=~ s/fastq\.gz/suffix\.fastq/;
		$sanitize_fastq		=~ s/<FASTQGZ1>/$f/g;
		$sanitize_fastq		=~ s/<FASTQGZ2>/$f2/g;
		$sanitize_fastq		=~ s/<FASTQ1SUFFIX>/$fastq1suffix/g;
		$sanitize_fastq		=~ s/<FASTQ2SUFFIX>/$fastq2suffix/g;
		$sanitize_fastq		=~ s/<OUTLOG>/$olog/g;

		$self->writelog(BODY => "Writing sanfq PBS files for $mapset");

		# create filename for PBS scripts
		my $basefilename	= $pbsdir.$mapset.".<NAME>.pbs";

		# sanitize pair of FQ files
		my $pbssanfq		= $basefilename;
		$pbssanfq		=~ s/<NAME>/sanfq/;
		$self->write_pbs_file(CMD => $sanitize_fastq, FILENAME => $pbssanfq);

		#SANFQ_LANE2AGTTCC=$(qsub /mnt/seq_raw//130227_7001238_0114_BC1ELJACXX/pbs/130227_7001238_0114_BC1ELJACXX.lane_2.AGTTCC.sanfq.pbs)
		#echo $SANFQ_LANE2AGTTCC
		$alllanespipeline	.= 'SANFQ'.$lanekey.'=$(qsub '.$pbssanfq.')'."\necho \$SANFQ".$lanekey."\n";
		push @sanfqqsubvars, '$SANFQ'.$lanekey;
	}

	#print STDERR "SANFQ PIPELINE: $alllanespipeline\n";

	foreach my $donorsample (keys %donorsample2mapset) {
		$self->writelog(BODY => "Generating PBS script content for $donorsample");

		my $mapsets	= join ", ", @{ $donorsample2mapset{$donorsample} };
		$self->writelog(BODY => "Mapsets: $mapsets");

		# choose a mapset to represent all mapsets for this donor - for
		# determining project, genome, etc
		my $rep_mapset		= $donorsample2mapset{$donorsample}->[0];

		my $donor		= $donorsample;
		$donor			=~ s/^(.+)\_.+?$/$1/;

		# make strings of fastq files for per-donor command line args
		my @fastq1		= ();
		my @fastq2		= ();
		foreach my $mapset (@{ $donorsample2mapset{$donorsample} }) {
			my $f1		= join "/", $self->SEQ_RAW_PATH, $self->{'slide_name'}, $mapset.".1.suffix.fastq";
			my $f2		= join "/", $self->SEQ_RAW_PATH, $self->{'slide_name'}, $mapset.".2.suffix.fastq";
			push @fastq1, $f1;
			push @fastq2, $f2;
		}
		my $f1concat		= join ",", @fastq1;
		my $f2concat		= join ",", @fastq2;


		# get reference genome
		my $ref			= $self->get_reference(MAPSET => $rep_mapset);
		if(! $ref && $self->{'is_in_lims'} eq 'N') {
			# if reference is found but sample is not LIMS, get
			# the reference from the sample sheet
			$ref		= $self->mapset2genomefile(MAPSET => $rep_mapset);
		}
		if(! $ref) {
			# if there is still no reference genome, don't try to
			# map
			$self->writelog(BODY => "Reference genome not found, skipping");
		}
		# get project name
		my $project		= $self->get_project(MAPSET => $rep_mapset);
		if(! $project) {
			# if there no project, don't try to
			# map - there will be no directory to put the files in
			$self->writelog(BODY => "Project name not found, skipping");
		}
		# get primary library, at least for representative mapset
		my $library		= $self->get_library(MAPSET => $rep_mapset);
		# make up read group ID
		my $rgid		= $self->timestamp(FORMAT => 'YYYYMMDDhhmmss');

		# build SAM and BAM names and paths
		# fake name of mapset for collective mapets for this donor
		my $donor_mapset	= join ".", $self->{'slide_name'}, "nopd", $donorsample;
		my $sam			= join ".", $donor_mapset, "sam";

		### build commands
		my $olog		= $logdir.$donor_mapset."_mapsplice_out.log";
		# /mnt/seq_results/PROJECT/DONOR/rna_seq/
		my $seq_results		= $self->infer_seqresults_rootdir(MAPSET => $rep_mapset);
		# /mnt/seq_results/PROJECT/DONOR/rna_seq/mapsplice/
		$seq_results		.= "/mapsplice/";
		my $mapsplicebam	= $seq_results.$donor_mapset.".bam";
		$mapsplicebam		=~ s/\.bam/\.mapsplice\.bam/;
		$mapsplicebam		=~ s/seq\_mapped/rna\_seq\/mapsplice/;
		my $mapsplice		= $mapsplicecmd;
		$mapsplice		=~ s/<FASTQ1SUFFIX>/$f1concat/g;
		$mapsplice		=~ s/<FASTQ2SUFFIX>/$f2concat/g;
		$mapsplice		=~ s/<GENOME>/$ref/g;
		#$mapsplice		=~ s/<SPLIT_GENOME>/$splitref/g;
		$mapsplice		=~ s/<MAPSET>/$donor_mapset/g;
		$mapsplice		=~ s/<NEWBAM>/$mapsplicebam/g;
		$mapsplice		=~ s/<LIBRARY>/$library/g;
		$mapsplice		=~ s/<DONOR>/$donor/g;
		$mapsplice		=~ s/<SEQ_RESULTS>/$seq_results/g;
		$mapsplice		=~ s/<OUTLOG>/$olog/g;

		### build commands
		my $olog		= $logdir.$donor_mapset."_rsem_out.log";
		my $rsem		= $rsemcmd;
		my $seq_results		= $self->infer_seqresults_rootdir(MAPSET => $rep_mapset);
		$seq_results		.= "/rsem/";
		my $rsembam		= $seq_results.$donor_mapset.".rsem.bam";
		#$rsembam		=~ s/\.bam/\.genome\.bam/;
		$rsembam		=~ s/seq _mapped/rna\_seq\/rsem/;
		my $rsem		= $rsemcmd;
		$rsem			=~ s/<FASTQ1SUFFIX>/$f1concat/g;
		$rsem			=~ s/<FASTQ2SUFFIX>/$f2concat/g;
		#$rsem			=~ s/<RSEM_GENOME>/$rsemgenome/g;
		$rsem			=~ s/<MAPSET>/$donor_mapset/g;
		$rsem			=~ s/<NEWBAM>/$rsembam/g;
		$rsem			=~ s/<LIBRARY>/$library/g;
		$rsem			=~ s/<DONOR>/$donor/g;
		$rsem			=~ s/<SEQ_RESULTS>/$seq_results/g;
		$rsem			=~ s/<OUTLOG>/$olog/g;

		$self->writelog(BODY => "Writing PBS files for $donor_mapset");

		# create filename for PBS scripts
		my $basefilename	= $pbsdir.$donor_mapset.".<NAME>.pbs";

		my $pbsmapsplice	= $basefilename;
		$pbsmapsplice		=~ s/<NAME>/mapsplice/;
		$self->write_pbs_file(CMD => $mapsplice, FILENAME => $pbsmapsplice);

		my $pbsrsem		= $basefilename;
		$pbsrsem		=~ s/<NAME>/rsem/;
		$self->write_pbs_file(CMD => $rsem, FILENAME => $pbsrsem);

		# now that we know the other script names, we can fill the
		# pipeline template for this lane
		my $pipeline		= $self->rsemmapsplice_pipeline_template(DONOR => $donorsample);
		my $pbssanfq		= join ":", @sanfqqsubvars;
		$pipeline		=~ s/<PBSSANFQ>/$pbssanfq/g;
		$pipeline		=~ s/<PBSMAPSPLICE>/$pbsmapsplice/;
		$pipeline		=~ s/<PBSRSEM>/$pbsrsem/;
		push @rsemqsubvars, '$RSEM_DONOR'.$donorsample;
		push @mapspliceqsubvars, '$MAPSPLICE_DONOR'.$donorsample;

		$alllanespipeline	.= $pipeline;

		# set aligner field in LIMS for each mapset
		my $aligner	= "bwa;mapsplice;rsem";
		$self->writelog(BODY => "Setting aligner field in LIMS: $aligner");
		$self->set_lims_aligner(MAPSET => $rep_mapset);
	}

	$self->{'rsemmapsplice_pbsfiles'}	= \@pbsfiles;

	# create slide-specific template for command to ingest mapped data
	$self->writelog(BODY => "Writing mapped ingest PBS file for $self->{'slide_name'}");
	my $olog	= $logdir.$self->{'slide_name'}."_ingestmapsplice_out.log";
	my $ingmapsplice	= $self->pbs_ingmapsplice_template();
	$ingmapsplice		= $header.$ingmapsplice;
	$ingmapsplice		=~ s/<JOBNAME>/rna_ingestmapsplice/;
	$ingmapsplice		=~ s/<QUEUE>/batch/;
	$ingmapsplice		=~ s/<NCPUS>/1/;
	$ingmapsplice		=~ s/<WALLTIME>/10:00:00/;
	$ingmapsplice		=~ s/<MEM>/1gb/;
	$ingmapsplice		=~ s/<OUTLOG>/$olog/;
	$ingmapsplice		=~ s/<BIN_AUTO>/$binauto/g;
	$ingmapsplice		=~ s/<SLIDE>/$self->{'slide_name'}/;
	my $pbsingmapsplice	= $pbsdir.$self->{'slide_name'}.".ingestmapsplice.pbs";
	$self->write_pbs_file(CMD => $ingmapsplice, FILENAME => $pbsingmapsplice);

	# create slide-specific template for command to ingest mapped data
	$self->writelog(BODY => "Writing mapped ingest PBS file for $self->{'slide_name'}");
	$olog	= $logdir.$self->{'slide_name'}."_ingestrsem_out.log";
	my $ingrsem	= $self->pbs_ingrsem_template();
	$ingrsem		= $header.$ingrsem;
	$ingrsem		=~ s/<JOBNAME>/rna_ingestrsem/;
	$ingrsem		=~ s/<QUEUE>/batch/;
	$ingrsem		=~ s/<NCPUS>/1/;
	$ingrsem		=~ s/<WALLTIME>/10:00:00/;
	$ingrsem		=~ s/<MEM>/1gb/;
	$ingrsem		=~ s/<OUTLOG>/$olog/;
	$ingrsem		=~ s/<BIN_AUTO>/$binauto/g;
	$ingrsem		=~ s/<SLIDE>/$self->{'slide_name'}/;
	my $pbsingrsem	= $pbsdir.$self->{'slide_name'}.".ingestrsem.pbs";
	$self->write_pbs_file(CMD => $ingrsem, FILENAME => $pbsingrsem);

	$self->writelog(BODY => "Writing master PBS file for $self->{'slide_name'}");
	# create one script to run them all; this script just qsubs other
	# scripts and set up dependencies
	my $pbsmapsplice	= join ":", @mapspliceqsubvars;
	my $pbsrsem	= join ":", @rsemqsubvars;
	$olog	= $logdir.$self->{'slide_name'}."_rsemmapsplice_master_out.log";
	my $master	= $self->pbs_rsemmapsplice_master_template(LANES => \@lanenums);
	$master		= $header.$master;
	$master		=~ s/<JOBNAME>/rna_master_rsemmapsplice/;
	$master		=~ s/<QUEUE>/batch/;
	$master		=~ s/<NCPUS>/1/;
	$master		=~ s/<WALLTIME>/00:05:00/;
	$master		=~ s/<MEM>/200mb/;
	$master		=~ s/<OUTLOG>/$olog/;
	$master		=~ s/<PIPELINE>/$alllanespipeline/;
	$master		=~ s/<PBSMAPSPLICE>/$pbsmapsplice/;
	$master		=~ s/<PBSINGMAPSPLICE>/$pbsingmapsplice/;
	$master		=~ s/<PBSRSEM>/$pbsrsem/;
	$master		=~ s/<PBSINGRSEM>/$pbsingrsem/;
	my $pbsmaster	= $pbsdir.$self->{'slide_name'}.".rsemmapsplice_master.pbs";
	$self->write_pbs_file(CMD => $master, FILENAME => $pbsmaster);

	# save filename of master script
	#push @pbsfiles, $pbsmaster;
	#$self->{'pbsfiles'}	= \@pbsfiles;
	push @{$self->{'pbsfiles'}}, $pbsmaster;

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


#PBS -M l.fink\@imb.uq.edu.au,s.wood1\@uq.edu.au,p.bailey\@uq.edu.au

	my $pbs		= qq{#PBS -N <JOBNAME>
#PBS -q <QUEUE>
#PBS -r n 
#PBS -l ncpus=<NCPUS>,walltime=<WALLTIME>,mem=<MEM>
#PBS -m ae 
#PBS -j oe
#PBS -W umask=0007
#PBS -o <OUTLOG>
#PBS -M l.fink\@imb.uq.edu.au

};

	return($pbs);
}

################################################################################
=pod

B<pipeline_template()> 
 Return bash script template for all commands for each lane pipelined together
  (these will be written to a master script for the whole slide)
	BWA ONLY!!!

Parameters:
 LANE	=> single digit lane number (1,2,3, etc)

Returns:
 scalar string with generic PBS script containing variables for replacing

=cut
sub pipeline_template {
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

	my $var1	= 'ALNFQ_LANE'.$lane.'_R1';
	my $var2	= 'ALNFQ_LANE'.$lane.'_R2';
	my $var3	= 'SAMPE_LANE'.$lane;
	my $var4	= 'SAM2BAM_LANE'.$lane;

	my $pbs		= qq{
#
$var1=\$(qsub <PBSBWAALN1>)
echo \$$var1

$var2=\$(qsub <PBSBWAALN2>)
echo \$$var2

$var3=\$(qsub -W depend=afterok:\$$var1:\$$var2 <PBSBWASAMPE>)
echo \$$var3

$var4=\$(qsub -W depend=afterok:\$$var3 <PBSSAM2BAM>)
echo \$$var4
#

};

	return($pbs);
}

################################################################################
=pod

B<rsemmapsplice_pipeline_template()> 
 Return bash script template for all commands for each lane pipelined together
  (these will be written to a master script for the whole slide)
	rsem and mapsplice ONLY!!!

Parameters:
 DONOR	=> donor ID

Returns:
 scalar string with generic PBS script containing variables for replacing

=cut
sub rsemmapsplice_pipeline_template {
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

	my $donor	= $options->{'DONOR'};

	my $var2	= 'MAPSPLICE_DONOR'.$donor;
	my $var3	= 'RSEM_DONOR'.$donor;

	my $pbs		= qq{
#
$var2=\$(qsub -W depend=afterok:<PBSSANFQ> <PBSMAPSPLICE>)
echo \$$var2

$var3=\$(qsub -W depend=afterok:<PBSSANFQ> <PBSRSEM>)
echo \$$var3
#

};

	return($pbs);
}


################################################################################
=pod

B<pbs_master_template()> 
 Return bash script template master script to run and manage all other scripts
	BWA ONLY!!

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
	my $donors	= $options->{'DONORS'};	# ref to array
	my $qcpbs	= $options->{'QCPBS'};	# ref to array of filenames of QC PBS scripts

	# <LANE> will be replaced by lane and index => 1ATGACG, 4TGCATA
	my $template	= '$SAM2BAM_LANE<LANE>';
	# <DONOR> will be replaced by donor => APGI_2051
	my $dnrtemplate	= '$QC_<DONOR>';

	my @jobs;
	foreach (@{$lanes}) {
		my $var	= $template;
		$var	=~ s/<LANE>/$_/;
		push @jobs, $var;
	}
	my $job	= join ':', @jobs;

	my $qc_pipeline	= '';
	my @qcjobs;
	for my $i (0..$#{$donors}) {
		my $var	= $dnrtemplate;
		$var	=~ s/<DONOR>/$donors->[$i]/;
		push @qcjobs, $var;

		$qc_pipeline	.= $var.qq{=\$(qsub -W depend=afterok:$job $qcpbs->[$i])\necho $var\n\n};
	}
	my $qcjob	= join ':', @qcjobs;


	my $pbs		= qq{
<PIPELINE>

INGMAP=\$(qsub -W depend=afterok:$job <PBSINGMAP>)
echo \$INGMAP


#QC=\$(qsub -W depend=afterok:$job <PBSQC>)
#echo \$QC

$qc_pipeline

INGQC=\$(qsub -W depend=afterok:$qcjob <PBSINGQC>)
echo \$INGQC

};

	return($pbs);
}

################################################################################
=pod

B<pbs_rsemmapsplice_master_template()> 
 Return bash script template master script to run and manage all rsem and
  mapsplice scripts

Parameters:
 LANES	=> ref to array of single digit lane numbers used on slide (1,2,3, etc)

Returns:
 scalar string with generic PBS script containing variables for replacing

=cut
sub pbs_rsemmapsplice_master_template {
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
<PIPELINE>

INGMAPSPLICE=\$(qsub -W depend=afterok:<PBSMAPSPLICE> <PBSINGMAPSPLICE>)
echo \$INGMAPSPLICE

INGRSEM=\$(qsub -W depend=afterok:<PBSRSEM> <PBSINGRSEM>)
echo \$INGRSEM

};

	return($pbs);
}

################################################################################
=pod

B<pbs_mapsplice_template()> 
 Return PBS template for mapsplice commands

Parameters:

Returns:
 scalar string with generic PBS script containing variables for replacing

=cut
sub pbs_mapsplice_template {
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
module load adama/nightly
module load QCMGPerl

mkdir /scratch/<MAPSET>/

chmod 0770 /scratch/<MAPSET>/

set -e

python /panfs/share/software/MapSplice-v2.1.2/mapsplice.py -p 16 -c /panfs/share/genomes/GRCh37_ICGC_standard_v2_split --fusion -x <GENOME> -1 <FASTQ1SUFFIX> -2 <FASTQ2SUFFIX> -o /scratch/<MAPSET>/

qbamfix --input /scratch/<MAPSET>/alignments.sam --output /scratch/<MAPSET>/alignments.bam --log /scratch/<MAPSET>/<MAPSET>.sam.fix.log --RGLB "<LIBRARY>" --RGSM "<DONOR>" --tmpdir \$TMPDIR

mv /scratch/<MAPSET>/alignments.bam         <NEWBAM>

if [ -f /scratch/<MAPSET>/deletions.txt ];
then
	mv /scratch/<MAPSET>/deletions.txt          <SEQ_RESULTS>/<MAPSET>.deletions.txt
fi
if [ -f /scratch/<MAPSET>/fusions_raw.txt ];
then
	mv /scratch/<MAPSET>/fusions_raw.txt        <SEQ_RESULTS>/<MAPSET>.fusions_raw.txt
fi
if [ -f /scratch/<MAPSET>/insertions.txt ];
then
	mv /scratch/<MAPSET>/insertions.txt         <SEQ_RESULTS>/<MAPSET>.insertions.txt
fi
if [ -f /scratch/<MAPSET>/junctions.txt ];
then
	mv /scratch/<MAPSET>/junctions.txt          <SEQ_RESULTS>/<MAPSET>.junctions.txt
fi
if [ -f /scratch/<MAPSET>/stats.txt ];
then
	mv /scratch/<MAPSET>/stats.txt              <SEQ_RESULTS>/<MAPSET>.stats.txt
fi
if [ -f /scratch/<MAPSET>/fusions_candidates.txt ];
then
	mv /scratch/<MAPSET>/fusions_candidates.txt <SEQ_RESULTS>/<MAPSET>.fusions_candidates.txt
fi

if [ ! -d <SEQ_RESULTS>/<MAPSET>_log/ ];
then
	mkdir <SEQ_RESULTS>/<MAPSET>_log/
fi

if [ -d /scratch/<MAPSET>/logs/ ];
then
	mv /scratch/<MAPSET>/logs/* <SEQ_RESULTS>/<MAPSET>_log/
fi

set +e

rm -r /scratch/<MAPSET>/

/share/software/QCMGPerl/distros/automation/sendemail.pl -s "Subject: <MAPSET> MAPSPLICE -- COMPLETED" -b "MAPSPLICE MAPPING OF <MAPSET> has ended. See log file for status: <OUTLOG>"

};

	return($pbs);
}

################################################################################
=pod

B<pbs_sanitize_fastq_template()> 
 Return PBS template to convert CASAVA FASTQ files to FASTQ files that mapsplice
 and RSEM can use

Parameters:

Returns:
 scalar string with generic PBS script containing variables for replacing

=cut
sub pbs_sanitize_fastq_template {
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

	# HWI-ST1238:114:C1ELJACXX:4:1101:4697:1948 1:N:0:AGTTAC ->
	#                                           -
	# @HWI-ST1238:114:C1ELJACXX:4:1101:4697:1948/1
	#                                            -

	my $pbs		= qq{
set -e

zcat <FASTQGZ1> | sed 's/ /_/g' | sed -e "s/_.*/\\/1/g" > <FASTQ1SUFFIX>

zcat <FASTQGZ2> | sed 's/ /_/g' | sed -e "s/_.*/\\/2/g" > <FASTQ2SUFFIX>

};#"

	return($pbs);
}

################################################################################
=pod

B<pbs_rsem_template()> 
 Return PBS template for RSEM commands

Parameters:

Returns:
 scalar string with generic PBS script containing variables for replacing

=cut
sub pbs_rsem_template {
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

#rsem-calculate-expression --paired-end --calc-ci --strand-specific --output-genome-bam -p 16 <FASTQ2SUFFIX> <FASTQ1SUFFIX> <RSEM_GENOME> <MAPSET> 
#qbamfix --input /scratch/<MAPSET>/<MAPSET>.genome.sorted.bam --output /scratch/<MAPSET>/<MAPSET>.genome.bam --log /scratch/<MAPSET>/<MAPSET>.genome.sorted.fix.log --RGLB "<LIBRARY>" --RGSM "<DONOR>" --tmpdir \$TMPDIR
#mv /scratch/<MAPSET>/<MAPSET>.genome.bam        <NEWBAM>
#rm -r /scratch/<MAPSET>/

	my $pbs		= qq{
module load bowtie
module load rsem
module load adama/nightly
module load QCMGPerl


mkdir /scratch/<MAPSET>

chmod 0770 /scratch/<MAPSET>

cd /scratch/<MAPSET>

set -e

rsem-calculate-expression --paired-end --calc-ci --strand-specific --output-genome-bam -p 16 <FASTQ2SUFFIX> <FASTQ1SUFFIX> /panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v70_norRNA <MAPSET> 

mv /scratch/<MAPSET>/<MAPSET>.genome.sorted.bam <NEWBAM>
mv /scratch/<MAPSET>/<MAPSET>.genes.results     <SEQ_RESULTS>
mv /scratch/<MAPSET>/<MAPSET>.isoforms.results  <SEQ_RESULTS>

if [ ! -d <SEQ_RESULTS>/<MAPSET>.stat/ ];
then
	mkdir <SEQ_RESULTS>/<MAPSET>.stat/
fi
cp -R /scratch/<MAPSET>/<MAPSET>.stat/          <SEQ_RESULTS>/<MAPSET>.stat/

set +e

rm -r /scratch/<MAPSET>

/share/software/QCMGPerl/distros/automation/sendemail.pl -s "Subject: <MAPSET> RSEM -- COMPLETED" -b "RSEM MAPPING OF <MAPSET> has ended. See log file for status: <OUTLOG>"

};

	return($pbs);
}

################################################################################
=pod

B<pbs_qc_template()> 
 Return PBS template for RNAseQC commands

Parameters:

Returns:
 scalar string with generic PBS script containing variables for replacing

=cut
sub pbs_qc_template {
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

	# SAMPLE COMMAND
	# java -Xmx20G -jar /panfs/home/pbailey/RNA_seQC/RNA-SeQC_v1.1.7.jar 
	#	-n 	1000 
	#	-s 	/mnt/seq_mapped/121203_SN7001243_0134_BC18E3ACXX/121203_SN7001243_0134_BC18E3ACXX.lane_8.TGACCA.bam 
	#	-t	/panfs/home/pbailey/RNA_seQC/gencode.v7.annotation.final.gtf 
	#	-r	/panfs/home/pbailey/RNA_seQC/GRCh37_ICGC_standard_v2.fa 
	#	-o	/mnt/seq_results/smgres_special/RNA_seq/APGI_1839/seq_mapped/QC/ 
	#	-strat	gc 
	#	-gc	/panfs/home/pbailey/RNA_seQC/gencode.v7.gc.txt

	# sample file format for -s:
	# Sample File: tab-delimited description of samples and their bams. This file header is:
	# Sample ID    Bam File    Notes
	# When running on just one sample, this argument can be a string of the form
	# "Sample ID|Bam File|Notes", where Bam File is the path to the input file.
	# Sample ID       Bam File        Notes
	# 121203_SN7001243_0134_BC18E3ACXX.lane_1.ACAGTG.bam      /mnt/seq_mapped/121203_SN7001243_0134_BC18E3ACXX/121203_SN7001243_0134_BC18E3ACXX.lane_1.ACAGTG.bam     APGI_1839
	# 121203_SN7001243_0134_BC18E3ACXX.lane_1.AGTCAA.bam      /mnt/seq_mapped/121203_SN7001243_0134_BC18E3ACXX/121203_SN7001243_0134_BC18E3ACXX.lane_1.AGTCAA.bam     APGI_1839

#	my $pbs		= qq{
#module load modules 3.2.10
#module load RNA-SeQC
#module load QCMGPerl

#mkdir <SEQMAPPED>/qc/

#set -e

#RNA-SeQC -n 1000 -ttype 2 -s <BAMLIST> -t /panfs/share/genomes/GRCh37ensemblv70/GRCh37_ICGC_standard_v70.gtf -r /panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa -o <SEQMAPPED>/qc/ -strat gc -gc /panfs/share/genomes/GRCh37ensemblv70/GRCh37_ICGC_standard_v70.gc.txt 

#set +e 

#/share/software/QCMGPerl/distros/automation/sendemail.pl -s "Subject: <SLIDE> RNASEQ QC -- COMPLETED" -b "QC OF <SLIDE> has ended. See log file for status: <OUTLOG>"

#};


	my $pbs		= qq{
module load RNA-SeQC
module load QCMGPerl

set -e

if [ ! -d <SEQMAPPED>/qc/ ];
then
	mkdir <SEQMAPPED>/qc/
fi

if [ ! -d  <SEQMAPPED>/qc/<DONOR>/ ];
then
	mkdir <SEQMAPPED>/qc/<DONOR>/
fi

RNA-SeQC -n 1000 -ttype 2 -s <BAMLIST> -t /panfs/share/genomes/GRCh37ensemblv70/GRCh37_ICGC_standard_v70.gtf -r /panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa -o <SEQMAPPED>/qc/<DONOR> -strat gc -gc /panfs/share/genomes/GRCh37ensemblv70/GRCh37_ICGC_standard_v70.gc.txt

set +e 

/share/software/QCMGPerl/distros/automation/sendemail.pl -s "Subject: <SLIDE> RNASEQ QC -- COMPLETED" -b "QC OF <SLIDE> for <DONOR> has ended. See log file for status: <OUTLOG>"

/share/software/QCMGPerl/distros/automation/sendemail.pl -t "qcmg-seqteam\@imb.uq.edu.au " -s "Subject: <SLIDE> RNASEQ QC -- COMPLETED" -b "QC OF <SLIDE> for <DONOR> has ended. See log file for status: <OUTLOG>"

};

	return($pbs);
}

################################################################################
=pod

B<qc_command()> 
 Return QC command line template

Parameters:

Returns:
 scalar string with generic command line containing variables for replacing

=cut
=cut
sub qc_command {
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

	my $cmd	= qq{mkdir <SEQMAPPED>/qc/<DONOR>\nRNA-SeQC -n 1000 -ttype 2 -s <BAMLIST> -t /panfs/share/genomes/GRCh37ensemblv70/GRCh37_ICGC_standard_v70.gtf -r /panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa -o <SEQMAPPED>/qc/<DONOR> -strat gc -gc /panfs/share/genomes/GRCh37ensemblv70/GRCh37_ICGC_standard_v70.gc.txt\n\n};

	return($cmd);
}
=cut

################################################################################
=pod

B<pbs_bwaaln_template()> 
 Return PBS template for bwa commands

Parameters:

Returns:
 scalar string with generic PBS script containing variables for replacing

=cut
sub pbs_bwaaln_template {
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
module load bwa

bwa aln -t 4 -f <SAI> <GENOME> <FASTQGZ>

};

	return($pbs);
}

################################################################################
=pod

B<pbs_bwasampe_template()> 
 Return PBS template for bwa commands

Parameters:

Returns:
 scalar string with generic PBS script containing variables for replacing

=cut
sub pbs_bwasampe_template {
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
module load bwa

bwa sampe <GENOME> <SAI1> <SAI2> <FASTQ1GZ> <FASTQ2GZ> > <SAM> 

};

	return($pbs);
}

################################################################################
=pod

B<pbs_sam2bam_template()> 
 Return PBS template for converting SAM, etc

Parameters:

Returns:
 scalar string with generic PBS script containing variables for replacing

=cut
sub pbs_sam2bam_template {
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
module load samtools

module load adama/nightly

set -e

qbamfix --input <SAM> --output <BAMPREFIX>.fixrg.bam --log <BAMPREFIX>.fix.log --RGLB "<LIBRARY>" --RGSM "<DONOR>" --tmpdir \$TMPDIR

java -jar /share/software/picard-tools-1.62/MarkDuplicates.jar INPUT=<BAMPREFIX>.fixrg.bam OUTPUT=<BAMPREFIX>.deduped.bam METRICS_FILE=<BAMPREFIX>.metric OPTICAL_DUPLICATE_PIXEL_DISTANCE=100 VALIDATION_STRINGENCY=LENIENT REMOVE_DUPLICATES=false ASSUME_SORTED=false MAX_SEQUENCES_FOR_DISK_READ_ENDS_MAP=50000 MAX_FILE_HANDLES_FOR_READ_ENDS_MAP=8000 SORTING_COLLECTION_SIZE_RATIO=0.25 READ_NAME_REGEX=".+:[0-9]+:([A-Z0-9]+):([0-9]+):([0-9]+).*" VERBOSITY=INFO QUIET=false COMPRESSION_LEVEL=5 MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false CREATE_MD5_FILE=false

cp <BAMPREFIX>.deduped.bam <NEWBAM>

cp <BAMPREFIX>.metric <NEWBAM>.metric

set +e

samtools index <NEWBAM>

chmod 664 <NEWBAM>*

module load QCMGPerl

/share/software/QCMGPerl/distros/automation/sendemail.pl -s "Subject: <MAPSET> BWA MAPPING -- COMPLETED" -b "MAPPING OF <MAPSET> has ended. See log file for status: <OUTLOG>"

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

B<pbs_ingqc_template()> 
 Return PBS template for ingesting the QC data for the whole slide

Parameters:

Returns:
 scalar string with generic PBS script containing variables for replacing

=cut
sub pbs_ingqc_template {
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

	my $pbs		= qq{\n<BIN_AUTO>/ingest_rna_seqc.pl -i <SLIDE>\n};

	return($pbs);
}

################################################################################
=pod

B<pbs_ingmapsplice_template()> 
 Return PBS template for ingesting the QC data for the whole slide

Parameters:

Returns:
 scalar string with generic PBS script containing variables for replacing

=cut
sub pbs_ingmapsplice_template {
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

	my $pbs		= qq{\n<BIN_AUTO>/ingest_mapsplice.pl -i <SLIDE>\n};

	return($pbs);
}

################################################################################
=pod

B<pbs_ingrsem_template()> 
 Return PBS template for ingesting the QC data for the whole slide

Parameters:

Returns:
 scalar string with generic PBS script containing variables for replacing

=cut
sub pbs_ingrsem_template {
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

	my $pbs		= qq{\n<BIN_AUTO>/ingest_rsem.pl -i <SLIDE>\n};

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

B<write_qc_filelist()> 
 Write text files with list of mapsets for QC program on a per-donor basis

Parameters:
 MAPSETS	=> ref to array of mapsets

Returns:
 ref to array of filenames

=cut
sub write_qc_filelist {
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

	my $mapsets	= $options->{'MAPSETS'};

	# keep track of lines to write to a donor BAM list
	my %donor2mapset	= ();
	my %donorsample2mapset	= ();

	# for each mapset, get path to seq_results BAM and donor
	foreach (@{$mapsets}) {
		my $project		= $self->get_project(MAPSET => $_);
		my $donor		= $self->get_donor(MAPSET => $_);
		my $sample		= $self->get_sample(MAPSET => $_);
		my $donorsample		= $donor."_".$sample;

		my $seqresbam		= join "/", $self->SEQ_RESULTS_PATH, $project, $donor, "seq_mapped", $_;
		$seqresbam		.= ".bam";

		my $line		= join "\t", $_, $seqresbam, $donorsample;
		$line			.= "\n";
		push @{$donorsample2mapset{$donorsample}}, $line;
	}

	# Sample ID       Bam File        Notes
	# 121203_SN7001243_0134_BC18E3ACXX.lane_1.ACAGTG.bam      /mnt/seq_mapped/121203_SN7001243_0134_BC18E3ACXX/121203_SN7001243_0134_BC18E3ACXX.lane_1.ACAGTG.bam     APGI_1839
	# 121203_SN7001243_0134_BC18E3ACXX.lane_1.AGTCAA.bam      /mnt/seq_mapped/121203_SN7001243_0134_BC18E3ACXX/121203_SN7001243_0134_BC18E3ACXX.lane_1.AGTCAA.bam     APGI_1839

	my $header	= "Sample ID\tBam File\tNotes\n";

	my @filenames	= ();
	my @samples	= ();
	foreach my $donorsample (keys %donorsample2mapset) {
		my $filename	= $self->SEQ_MAPPED_PATH.$self->{'slide_name'}."/".$donorsample."_qc_filelist.txt";
		my $sample	= $donorsample;
		$sample		= s/^.+\_(.+?)$/$1/;

		$self->writelog(BODY => "\tWriting $filename");

		# Now we are ready to write out the file
		open(FH, ">".$filename) || exit($self->EXIT_NO_FILELIST());
		# print file header
		print FH $header;
		foreach my $line (@{$donorsample2mapset{$donorsample}}) {
			#print FH "$_\t$seqresbam\t$donor\n";
			print FH $line;
		}
		close(FH);

		push @filenames, $filename;
		push @samples, $sample;
	}

	#return($filename);
	return(\@filenames, \@samples);
}

################################################################################
=pod

B<get_gene_model()> 
 Get gene model GTF file appopriate for BAM

Parameters:
 MAPSET	=>  mapset name

Returns:
 scalar string of path and filename

=cut
sub get_gene_model {
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

	$self->writelog(BODY => "Getting gene model GTF file for $options->{'MAPSET'}");

	#my $md	= QCMG::DB::Metadata->new();
	#$md->find_metadata("mapset", $options->{'MAPSET'});
	#my $lib	= $md->primary_library($options->{'MAPSET'});
	#$md	= undef;

	# NOT ACTUAL FILE, JUST A PLACEHOLDER, # FIX!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	my $file	= "/panfs/share/gene_models/gencode.v7.annotation.final.gtf";

	$self->writelog(BODY => "File: $file\n");

	return($file);
}

################################################################################
=pod

B<get_gc_annotation()> 
 Get GC annotation file appopriate for BAM

Parameters:
 MAPSET	=>  mapset name

Returns:
 scalar string of path and filename

=cut
sub get_gc_annotation {
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

	$self->writelog(BODY => "Getting GC annotation GTF file for $options->{'MAPSET'}");

	#my $md	= QCMG::DB::Metadata->new();
	#$md->find_metadata("mapset", $options->{'MAPSET'});
	#my $lib	= $md->primary_library($options->{'MAPSET'});
	#$md	= undef;

	# NOT ACTUAL FILE, JUST A PLACEHOLDER, # FIX!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	my $file	= "/panfs/share/gene_models/gencode.v7.gc.txt";

	$self->writelog(BODY => "File: $file\n");

	return($file);
}

################################################################################
=pod

B<get_rsem_genome()> 
 Get RSEM reference genome file

Parameters:
 MAPSET	=>  mapset name

Returns:
 scalar string of path and filename

=cut
sub get_rsem_genome {
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

	$self->writelog(BODY => "Getting RSEM reference genome file for $options->{'MAPSET'}");

	#my $md	= QCMG::DB::Metadata->new();
	#$md->find_metadata("mapset", $options->{'MAPSET'});
	#my $lib	= $md->primary_library($options->{'MAPSET'});
	#$md	= undef;

	# ACTUAL FILE BASENAME? # FIX??
	my $file	= "/panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v70";

	$self->writelog(BODY => "File base name: $file\n");

	return($file);
}

################################################################################
=pod

B<get_split_genome()> 
 Get split reference genome file (each RS in a .fa file)

Parameters:
 MAPSET	=>  mapset name

Returns:
 scalar string of path

=cut
sub get_split_genome {
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

	$self->writelog(BODY => "Getting split reference genome file directory for $options->{'MAPSET'}");

	#my $md	= QCMG::DB::Metadata->new();
	#$md->find_metadata("mapset", $options->{'MAPSET'});
	#my $lib	= $md->primary_library($options->{'MAPSET'});
	#$md	= undef;

	my $dir	= "/panfs/share/genomes/GRCh37_ICGC_standard_v2_split/";

	$self->writelog(BODY => "Directory: $dir\n");

	return($dir);
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

B<get_sample()> 
 Get sample ID from Geneus for a mapset

Parameters:
 MAPSET	=>  mapset name

Returns:
 scalar string of library name

=cut
sub get_sample {
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

	$self->writelog(BODY => "Getting sample ID for $options->{'MAPSET'}");

	my $md	= QCMG::DB::Metadata->new();
	$md->find_metadata("mapset", $options->{'MAPSET'});	# required for following step to work
	my $sam	= $md->sample($options->{'MAPSET'});
	$md	= undef;

	$sam	=~ s/\-//g;

	$self->writelog(BODY => "Sample: $sam\n");

	return($sam);
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

	# !!!!!!!!!!!!!!!!!!!!!!!!!! FIX! !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1
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

B<infer_sai()> 
 Infer .sai filename

Parameters:
 FILE	=> a FASTQ file

Returns:
 scalar string of file name

=cut
sub infer_sai {
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
	# 110614_SN103_0846_BD03UMACXX.lane_1.nobc.1.sai
	my $file	= $options->{'FILE'};
	$file		=~ s/\.fastq\.gz/\.sai/;
	$file		=~ s/\.fastq/\.sai/;	# in case fastq is not gzipped

	return ($file);
}

################################################################################
=pod

B<infer_sam()> 
 Infer SAM filename from slidename and barcode

Parameters:
 FILE	=> a FASTQ file

Returns:
 scalar string of SAM file name, with path

=cut
sub infer_sam {
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

	# T00001_20120419_162.nopd.IonXpress_009.sam
	my $mapset	= $self->infer_mapset(FILE => $file);
	my $sam		= join ".", $mapset, "sam";
	$sam		= join "/", $self->SEQ_MAPPED_PATH, $self->{'slide_name'}, $sam;

	return ($sam);
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

	# !!!!!!!!!!!!!!!!!!!!!!!! FIX! !!!!!!!!!!!!!!!!!!!!!!!!!11
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

	my $project	= $self->get_project(MAPSET => $options->{'MAPSET'});
	my $donor	= $self->get_donor(MAPSET => $options->{'MAPSET'});

	my $bam		= join ".", $options->{'MAPSET'}, "bam";
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

B<infer_seqresults_donor_bam()> 
 Infer BAM filename from a compound BAM (many mapsets for a single donor)

Parameters:
 MAPSET		=> "mapset" name (SLIDE.nopd.DONOR)

Returns:
 scalar string of anticipated BAM file name, with path

=cut
sub infer_seqresults_donor_bam {
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

	my $project	= $self->get_project(MAPSET => $options->{'MAPSET'});
	my $donor	= $self->get_donor(MAPSET => $options->{'MAPSET'});

	my $bam		= join ".", $options->{'MAPSET'}, "bam";
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

B<infer_seqresults_rootdir()> 
 Infer top level directory that contains all RNA-seq data for a donor

Parameters:
 MAPSET	=> mapset name

Returns:
 scalar string of path to rna_seq data

=cut
sub infer_seqresults_rootdir {
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

	my $project	= $self->get_project(MAPSET => $options->{'MAPSET'});
	my $donor	= $self->get_donor(MAPSET => $options->{'MAPSET'});

	my $bam;
	if($project =~ /smgcore/) {
		$bam		= join "/", $self->SEQ_RESULTS_PATH, "smg_core", $project, $donor, 'rna_seq';
	}
	else {
		$bam		= join "/", $self->SEQ_RESULTS_PATH, $project, $donor, 'rna_seq';
	}
	$bam	.= "/";

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

	#my $aligner	= 'bwa';
	my $aligner	= qq{"bwa;mapsplice;rsem"};

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

			my $prj		= $d->{'project'};
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

B<run_master()> 
 Run all scripts by qsubbing master script(s)

Parameters:

Requires:
 $self->{'pbsfiles'}

Returns:
 void

=cut
sub run_master {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Preparing for qsubbing jobs... ---");

	$MAPPING_ATTEMPTED			= 'yes';

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
		$self->writelog(BODY => "Running $pbs");
		print STDERR "Running $pbs\n";
		my $rv = `qsub $pbs`;
		### ADD ERROR CHECKING!!!
		$rv =~ /^(\d+)\./;
		my $jobid = $1;
		$self->writelog(BODY => "Ran $pbs: $rv , job id: $jobid");

		#my $subj	= $self->{'slide_name'}." $pbs SUBMITTED";
		#my $body	= "RNA_SEQ PBS SUBMITTED: ".$pbs."\n\nJob IDs: ".$rv;
	}

	$self->writelog(BODY => "Completed job submissions");

	$MAPPING_SUCCEEDED			= 'yes';

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
		if($MAPPING_ATTEMPTED eq 'yes') {
			$self->writelog(BODY => "EXECLOG: errorMessage none");

			$self->{'EXIT'} = 0;

			# send success email if no errors
			my $subj	= $self->{'slide_name'}." RNA-seq processing -- SUCCEEDED";
			my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'} complete";
			$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			#$self->send_email(SUBJECT => $subj, BODY => $body);
		}
		# running bwa was not attempted; script ended for some other
		# reason
		elsif($MAPPING_ATTEMPTED eq 'no') {
			$self->writelog(BODY => "EXECLOG: errorMessage bwa run not attempted");
	
			$self->{'EXIT'} = 17;
	
			# send success email if no errors
			my $subj	= $self->{'slide_name'}." RNA-seq processing -- NOT ATTEMPTED";
			my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'} not attempted";
			$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			#$self->send_email(SUBJECT => $subj, BODY => $body);
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
		#my $asset	= $self->{'slide_name'}.$self->LA_ASSET_SUFFIX_RUNBWA_LOG;
		my $asset	= $self->{'slide_name'}."_bwa_log";

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

Copyright (c) The University of Queensland 2013

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
