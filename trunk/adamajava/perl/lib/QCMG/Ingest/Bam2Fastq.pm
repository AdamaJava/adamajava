package QCMG::Ingest::Bam2Fastq;

##############################################################################
#
#  Module:   QCMG::Ingest::Bam2Fastq.pm
#  Creator:  Lynn Fink
#  Created:  2011-06-14
#
#  This class contains methods for automating the ingest into LiveArc
#  IGN BAMs and bam2fastq coverted FASTQ files
#
#  $Id$
#
##############################################################################

=pod

=head1 NAME

QCMG::Ingest::Bam2Fastq -- Common functions for ingesting FASTQ sequencing files 

=head1 SYNOPSIS

Most common use:
 my $qi = QCMG::Ingest::Bam2Fastq->new();

=head1 DESCRIPTION

Contains methods for extracting run information from a raw sequencing run
folder, checking the sequencing files, and ingesting them into LiveArc

=head1 REQUIREMENTS

 Exporter
 File::Spec
 POSIX 'strftime'
 QCMG::Automation::LiveArc
 QCMG::Automation::Config
 QCMG::Automation::Common

=cut

use strict;
# standard distro modules
use Data::Dumper;
use File::Spec;				# for parsing paths
#use Cwd;
use Cwd 'realpath';			# for relative -> absolute paths
# in-house modules
use QCMG::Automation::LiveArc;		# for accessing Mediaflux/LiveArc
use QCMG::Automation::Common;
use QCMG::DB::Geneus;			# for setting LIMS flag for ingest status

our @ISA = qw(QCMG::Automation::Common);	# inherit Common methods

use vars qw( $SVNID $REVISION $IMPORT_ATTEMPTED $IMPORT_SUCCEEDED);

################################################################################
=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 SLIDE_FOLDER	=> directory
 LOG_FILE   	=> path and filename of log file
 GLPID		=> LIMS process ID for setting status flag (not required, but good for tracking)
 UPDATE		=> 1 - update ingest; 0 force ingest from scratch
 NO_EMAIL   	=> 1 (do not send emails), 0 (send emails)

Returns:
 a new instance of this class.

Sets:
 $self->{'update'}
 $self->{'slide_folder'}
 $self->{'hostname'}
 $self->{'log_file'}

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

	#print STDERR "SLIDE folder: $options->{'SLIDE_FOLDER'}\n";

	# if NO_EMAIL => 1, don't send emails
	$self->{'no_email'}		= $options->{'NO_EMAIL'};

	# set update mode if requested
	$self->{'update'}		= 1 if($options->{'UPDATE'});

	$self->{'slide_folder'}		= $options->{'SLIDE_FOLDER'};
	# SLIDe FOLDER NAME, should have / on the end with absolute path
	$self->{'slide_folder'}		= Cwd::realpath($self->{'slide_folder'})."/";
	print STDERR "SLIDE FOLDER: $self->{'slide_folder'}\n";
	print STDERR "SLIDE FOLDER: $options->{'SLIDE_FOLDER'}\n";

	$self->{'hostname'}		= `hostname`;
	chomp $self->{'hostname'};

	# set LIMS process ID (if supplied)
	$self->{'glpid'}		= $options->{'GLPID'};
	$self->{'glpid'}		= '' if($self->{'glpid'} == 0);

	my $slide_name			= $self->slide_name();

	# define LOG_FILE
	if($options->{'LOG_FILE'}) {
		$self->{'log_file'}		= $options->{'LOG_FILE'};
	}
	else {
		# DEFAULT: /run/folder/runfolder_ingest.log
		$self->{'log_file'}		= $self->{'slide_folder'}.$slide_name."_ingest.log";
	}

	# fail if the user does not have LiveArc credentials
	if(! -e $self->LA_CRED_FILE) {
		print STDERR "LiveArc credentials file not found!\n";
		exit($self->EXIT_LIVEARC_ERROR());
	}

	$self->init_log_file();

	push @{$self->{'email'}}, $self->DEFAULT_EMAIL;

	# set global flags for checking in DESTROY()  (no/yes)
	$IMPORT_ATTEMPTED = 'no';
	$IMPORT_SUCCEEDED = 'no';

	return $self;
}

# Exit codes:
sub EXIT_NO_FOLDER {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'slide_folder'} failed";
	my $body	= "Failed: No run folder: $self->{'slide_folder'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 1;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});	
	return 1;
}
sub EXIT_BAD_PERMS {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'slide_folder'} failed";
	my $body	= "Failed: Bad permissions on $self->{'slide_folder'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 2;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});	
	return 2;
}
sub EXIT_LIVEARC_ERROR {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'slide_folder'} failed";
	my $body	= "Failed: LiveArc error";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 9;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});	
	return 9;
}
sub EXIT_ASSET_EXISTS {
	# should not happen anymore...
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'slide_folder'} failed";
	my $body	= "Failed: LiveArc asset exists";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 12;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});	
	return 12;
}
sub EXIT_BAD_RAWFILES {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'slide_folder'} failed";
	my $body	= "Failed: Raw sequencing files are corrupt";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage  $body");

	$self->{'EXIT'} = 13;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});	
	return 13;
}
sub EXIT_IMPORT_FAILED {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'slide_folder'} failed";
	my $body	= "Failed: LiveArc import failed";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 16;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});	
	return 16;
}
sub EXIT_NO_BCL {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'slide_folder'} failed";
	my $body	= "Failed: No BCL files found";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 40;
	return 40;
}
sub EXIT_NO_METAFILES {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'slide_folder'} failed";
	my $body	= "Failed: Required metadata files not found";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 41;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});	
	return 41;
}
sub EXIT_NO_FASTQ {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'slide_folder'} failed";
	my $body	= "Failed: FASTQ files were not generated";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 42;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});	
	return 42;
}
sub EXIT_CASAVA_FAILED {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'slide_folder'} failed";
	my $body	= "Failed: CASAVA error";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 43;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});	
	return 43;
}
sub EXIT_NO_SAMPLESHEET {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'slide_folder'} failed";
	my $body	= "Failed: Cannot read sample sheet";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 49;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});	
	return 49;
}
sub EXIT_BAD_SAMPLESHEET {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'slide_folder'} failed";
	my $body	= "Failed: Bad sample sheet";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 50;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});	
	return 50;
}

# SET exit(17) = import not attempted (but only used in DESTROY())

################################################################################
=pod

B<slide_folder()>

Return run folder name, as set in new()

Parameters:

Returns:
  scalar run folder name

=cut
sub slide_folder {
	my $self	= shift @_;
	return $self->{'slide_folder'};
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

	my $path	= $self->{'slide_folder'}."Data/Intensities/BaseCalls/";

	if(-e $path && -d $path) {
		$self->{'basecall_folder'}	= $path;
		$self->writelog(BODY => "Found $path");
	}
	else {
		$self->writelog(BODY => "No valid basecall folder provided; tried $path\n");
		exit($self->EXIT_NO_BCL());
	}

	return($self->{'basecall_folder'});
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
		$self->writelog(BODY => "No RunInfo.xml file found; tried $path\n");
		exit($self->EXIT_NO_METAFILES());
	}

	return($self->{'runinfo'});
}

################################################################################
=pod

B<runparameters()>

Return path to runParameters.xml file

Parameters:

Requires:
 $self->{'slide_folder'}

Returns:
 scalar path

Sets:
 $self->{'runparameters'}

=cut
sub runparameters {
	my $self	= shift @_;

	$self->writelog(BODY => "Finding runParameters.xml file...");

	# should be under slide level directory:
	# /path.../110614_SN103_0846_BD03UMACXX.images/

	my $path	= $self->{'slide_folder'}."/runParameters.xml";

	if(-e $path && -r $path) {
		$self->{'runparameters'}	= $path;
		$self->writelog(BODY => "Found $path");
	}
	else {
		$self->writelog(BODY => "No runParameters.xml file found; tried $path\n");
		exit($self->EXIT_NO_METAFILES());
	}

	return($self->{'runparameters'});
}

################################################################################
=pod

B<interop()>

Return path to InterOp directory

Parameters:

Requires:
 $self->{'slide_folder'}

Returns:
 scalar path

Sets:
 $self->{'interop'}

=cut
sub interop {
	my $self	= shift @_;

	$self->writelog(BODY => "Finding InterOp directory...");

	# should be under slide level directory:
	# /path.../110614_SN103_0846_BD03UMACXX.images/InterOp/

	my $path	= $self->{'slide_folder'}."/InterOp/";

	if(-e $path && -d $path) {
		$self->{'interop'}	= $path;
		$self->writelog(BODY => "Found $path");
	}
	else {
		$self->writelog(BODY => "No InterOp directory found; tried $path\n");
		exit($self->EXIT_NO_METAFILES());
	}

	return($self->{'interop'});
}

################################################################################
=pod

B<asset_name()>

Return asset name, as derived from slide folder name

Parameters:

Requires:
 $self->{'slide_folder'}

Returns:
  scalar asset name

=cut
=cut
sub asset_name {
	my $self	= shift @_;

	#$self->{'asset_name'} = $self->{'slide_name'}.$LA_ASSET_SUFFIX;
	$self->{'asset_name'} = $self->{'slide_name'};

	return $self->{'asset_name'};
}
=cut

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

B<is_barcoded()> 
 Is part of a barcoded run or not (set in get_metadata())

Parameters:

Returns:
  scalar: 0 if yes, 1 if not

=cut
sub is_barcoded {
        my $self = shift @_;

	return($self->{'is_barcoded'});
}

################################################################################
=pod

B<samplesheet()> 
 Sample sheet file (SampleSheet.csv) (resides in basecall folder)

Parameters:

Requires:
 $self->{'basecall_folder'}

Returns:
  scalar path and filename of sample sheet

Sets:
 $self->{'samplesheet'}

=cut
sub samplesheet {
        my $self	= shift @_;

	# infer sample sheet name and location
	my $csv		= $self->{'basecall_folder'}."/SampleSheet.csv";
	#my $csv			= '/panfs/seq_raw/test_lmp/120523_SN7001240_0047_BD12NAACXX/Unaligned/SampleSheet.csv';

	$self->writelog(BODY => "Inferring sample sheet file: $csv");

	# check that file exists and is readable
	if(-e $csv && -r $csv) {
		$self->{'samplesheet'}	= $csv;
		$self->writelog(BODY => "Sample sheet found");
	}
	else {
		$self->writelog(BODY => "Sample sheet not found");
	}

	return($self->{'samplesheet'});
}

################################################################################
=pod

B<get_barcodes()> 
 Get barcodes included on this slide/lane from SampleSheet.csv

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

Parameters:
 $self->{basecall_folder}

Returns:
 $self->{'barcodes'}
 
Sets:
 $self->{'barcodes'} (ref to hash ref, key = lane; value = barcode sequence

=cut
sub get_barcodes {
        my $self = shift @_;

	$self->writelog(BODY => "---Getting barcodes for all lanes...---");

	my $sscsv	= $self->samplesheet();

	# if sample sheet is not defined, return empty barcodes
	if(! $sscsv) {
		$self->{'barcodes'}	= '';
		return($self->{'barcodes'});
	}

	# get contents of sample sheet file
	open(FH, $sscsv);
	while(<FH>) {
		# skip header
		next if(/^FCID/);

		my ($fc, $lane, $sampleid, $sampleref, $index, $desc, $control, $recipe, $operator, $sampleproject)	= split /,/;
		$self->{'barcodes'}->{$lane}	= $index;
		$self->writelog(BODY => "Lane $lane, Index $index");
	}
	close(FH);

	$self->writelog(BODY => "---done---");

	return($self->{'barcodes'});
}

################################################################################
=pod

B<get_lanes()> 
 Get lanes included on this slide from SampleSheet.csv

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

Parameters:
 $self->{basecall_folder}

Returns:
 $self->{'lanes'}
 
Sets:
 $self->{'lanes'} (ref to arry ref of lane numbers (1,2,3. etc)

=cut
sub get_lanes {
        my $self = shift @_;

	$self->writelog(BODY => "---Getting lanes for all lanes...---");

	my $sscsv	= $self->samplesheet();

	# if sample sheet is not defined, return empty barcodes
	if(! $sscsv) {
		$self->{'lanes'}	= '';
		return($self->{'lanes'});
	}

	# get contents of sample sheet file
	open(FH, $sscsv) || exit($self->EXIT_NO_SAMPLESHEET());
	while(<FH>) {
		s/\r\n?/\n/g;	# fix DOS new line chars
		#print STDERR "$_\n";
		chomp;

		# skip header
		next if(/^FCID/);

		my ($fc, $lane, $sampleid, $sampleref, $index, $desc, $control, $recipe, $operator, $sampleproject)	= split /,/;
		print STDERR "LANE $lane\n";
		push @{$self->{'lanes'}}, $lane;
		$self->writelog(BODY => "Lane $lane");
	}
	close(FH);

	$self->writelog(BODY => "---done---");

	return($self->{'lanes'});
}

################################################################################
=pod

B<validate_samplesheet()> 
 Go through each line of a sample sheet and make sure it is unique according to
  lane, index, sampleid, and sampleproject

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

Parameters:
 $self->{basecall_folder}

Returns:
 $self->{'lanes'}
 
Sets:
 $self->{'lanes'} (ref to arry ref of lane numbers (1,2,3. etc)

=cut
sub validate_samplesheet {
        my $self = shift @_;

	$self->writelog(BODY => "--- Validating sample sheet... ---");

	my $sscsv	= $self->samplesheet();

	# get contents of sample sheet file
	my %ssentries;
	open(FH, $sscsv) || exit($self->EXIT_NO_SAMPLESHEET());
	while(<FH>) {
		s/\r\n?/\n/g;	# fix DOS new line chars
		#print STDERR "$_\n";
		chomp;

		# skip header
		next if(/^FCID/);

		my ($fc, $lane, $sampleid, $sampleref, $index, $desc, $control, $recipe, $operator, $sampleproject)	= split /,/;

		my $entry		= join "\t", $lane, $index, $sampleid, $sampleproject;

		$ssentries{$entry}	+= 1;
	}
	close(FH);

	# fail if there are any non-unique sample sheet lines; print out all
	# non-unique lines first
	my $nrline	= 'n';
	foreach (keys %ssentries) {
		if($ssentries{$_} > 1) {
			$self->writelog(BODY => "Non-unique sample sheet line: $_ ($ssentries{$_})");
			$nrline	= 'y';
		}
	}
	if($nrline eq 'y') {
		exit($self->EXIT_BAD_SAMPLESHEET());
	}

	$self->writelog(BODY => "---done---");

	return($self->{'lanes'});
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

	# check that the basecalls folder exists
	$self->basecall_folder();

	# check that the necessary metadata files and directory exist
	$self->runinfo();
	$self->runparameters();
	$self->interop();

	$self->writelog(BODY => "Folder and related files exist and are read/writable.");	

	return($status);
}

################################################################################
=pod

B<find_bcls()> 
 Find at least one .bcl file

Parameters:

Requires:
 $self->{'basecall_folder'}

Returns:

=cut
sub find_bcls {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Finding .bcl filess---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	# 110614_SN103_0846_BD03UMACXX.images/Data/Intensities/BaseCalls/
	# L001, L002, L003, L004, L005, L006, L007, L008/
	# C1.1/ .... C202.1/

	# 110614_SN103_0846_BD03UMACXX.images/Data/Intensities/BaseCalls/L001/C1.1/
	# s_1_1101.bcl
	# s_1_1101.stats
	# ...

	# first, get all lanes on slide
	#$self->get_lanes();

	#foreach my $lane (@{$self->{'lanes'}}) {
		#my $dir	= $self->{'basecall_folder'}."/L00".$lane."/";
		#print STDERR "Lane $lane dir is $dir\n";
		my $dir		= $self->{'basecall_folder'};

		my $cmd		= qq{find $dir -name "*.bcl"};
		#print STDERR "$cmd\n";
		my @files = `$cmd`;

		#print Dumper @files;

		if(scalar(@files) == 0) {
			$self->writelog(BODY => "No BCLs in $dir");
			exit($self->EXIT_NO_BCL());
		}
	#}

	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}

################################################################################
=pod

B<get_run_type()> 
 Get run types for each lane from sample sheet

Parameters:

Returns:
 ref to hash of run types (key = lane, value = run type)

 Sets:
  $self->{'run_types'}->{$lane}

Requires:
 $self->{'slide_name'}

=cut
=cut
sub get_run_type {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Getting run types from Tracklite...---");

	# this isn't good, but we should be replacing it with Geneus soon...
	my $sql	= qq{SELECT 
			DISTINCT run_type, run_name, flowcell, 
				individual_sample_location 
			FROM tracklite_run
			WHERE run_name = '$self->{'slide_name'}'
		}; #'

	my $tl	= QCMG::DB::TrackLite->new();
	$tl->connect;
	my $res	= $tl->query_db(QUERY => $sql);

	# run types: Frag, LMP, PE -> don't need to translate these for making
	# .ls files

	for my $i (0..$#{$res}) {
		my $run_type	= $res->[$i]->{'run_type'};
		my $lane	= $res->[$i]->{'individual_sample_location'};

		#print STDERR "RUN TYPE: $run_type\n";

		$lane		=~ /lane\_(.+)/i;
		my $num		= $1;

		# if lane is like this: lane_2_3_4_5 , separate into multiple
		# lanes
		if($num =~ /\_/) {
			my @lanes	= split /\_/, $num;

			# prepend 'lane' 2 -> lane_2
			foreach (@lanes) {
				$lane	= $self->format_lanename(LANE => $_);
				$self->{'run_types'}->{$lane} = $run_type;

				$self->writelog(BODY => "$lane\t$run_type");
			}
		}
		# if lane is like this: lane_2, Lane_2, lane_02, just fix the
		# case and zero-padding if necessary
		# STANDARD CASE
		else {

			#$lane	= $self->format_lanename(LANE => $lane);
			$self->{'run_types'}->{$lane} = $run_type;

			$self->writelog(BODY => "$lane\t$run_type");
		}
	}

	$self->writelog(BODY => "---done---");

	#print Dumper $self->{'run_types'};

        return $self->{'run_types'};
}
=cut

################################################################################
=pod

B<slide_name()> 
  Get slide name from slide folder name

Parameters:

Requires:
 $self->{'slide_folder'}

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

	# /scratch/platinumgenomes/primary/110614_SN103_0846_BD03UMACXX.images/
	# -> 110614_SN103_0846_BD03UMACXX

	my $rf			= $self->{'slide_folder'};
	$rf			=~ s/\/$//;
	my ($v, $d, $file)	= File::Spec->splitpath($rf);
	$file			=~ s/\.images//;
	$self->{'slide_name'} 	= $file;

        return $self->{'slide_name'};
}

################################################################################
=pod

B<fastq_root()> 
 Set or return $self->{'fastq_root'}

Parameters:

Sets:
 $self->{'fastq_root'}	path to seq_raw CASAVA directory

Returns:
  scalar path to CASAVA directory

=cut
sub fastq_root {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	unless($self->{'fastq_root'}) {
		$self->{'fastq_root'}	= $self->SEQ_RAW_PATH."/".$self->{'slide_name'}."/Unaligned/";
		### FOR TESTING ONLY!!!
		#$self->{'fastq_root'}	= "/panfs/seq_raw/test_lmp"."/".$self->{'slide_name'}."/Unaligned/";
	}

	return($self->{'fastq_root'});
}

################################################################################
=pod

B<bcl2fastq()> 
 Convert BCL files to FASTQ files 

Parameters:

Sets:
 $self->{'fastq_root'} - path to seq_raw CASAVA directory

Returns:
  scalar slide name 

=cut
sub bcl2fastq {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->writelog(BODY => "--- Demultiplexing with CASAVA ---");

	# top-level destination directory for FASTQ files
	$self->{'fastq_root'}	= $self->fastq_root;

	# make a slide directory underneath /panfs/seq_raw/ first
	my $seqraw	= $self->{'fastq_root'};
	$seqraw		=~ s/Unaligned//;
	mkdir $seqraw;

	# command to demultiplex and convert to FASTQ
	my $cmd	=  $self->BIN_CASAVA.qq{/configureBclToFastq.pl };
	$cmd	.= qq{--input-dir    $self->{'basecall_folder'} };
	$cmd	.= qq{--output-dir   $self->{'fastq_root'} };
	$cmd	.= qq{--fastq-cluster-count 0 };
	$cmd	.= qq{--mismatches 1 };
	$cmd	.= qq{--sample-sheet $self->{'samplesheet'} };
	#$cmd	.= qq{--force};	# SHOULD REMOVE??

	# create make files for CASAVA
	$self->writelog(BODY => "$cmd");
	my $rv	= system($cmd);
	$self->writelog(BODY => "RV: $rv");

	if($rv != 0) {
		exit($self->EXIT_CASAVA_FAILED());
	}

	my $cwd		=  Cwd::realpath("./");
	$self->writelog(BODY => "CWD: $cwd");

	$self->writelog(BODY => "chdiring to $self->{'fastq_root'}");
	chdir $self->{'fastq_root'};

	# run CASAVA; ncpus=4, mem=22gb
	my $cmd	= qq{make -j 8};
	$self->writelog(BODY => "$cmd");
	my $rv	= system($cmd);
	$self->writelog(BODY => "RV: $rv");

	chdir $cwd;

	if($rv != 0) {
		exit($self->EXIT_CASAVA_FAILED());
	}

	# FASTQ files are created here
	# 110614_SN103_0846_BD03UMACXX.images/Unaligned/Project_D03UMACXX/Sample_lane1
	# 110614_SN103_0846_BD03UMACXX.images/Unaligned/Project_D03UMACXX/Sample_lane2
	# etc.
	# lane2_NoIndex_L002_R1.fastq.gz / lane2_NoIndex_L002_R2.fastq.gz

	$self->writelog(BODY => "--- Done ---");

	return();
}

################################################################################
=pod

B<checksum_fastq()> 
 Perform checksums on FASTQ files 
 
Parameters:

Requires:
 $self->{'fastq'}

Returns:
 void

Sets:
 $self->{'ck_checksum'}
 $self->{'ck_filesize'}

=cut
sub checksum_fastq {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

=cut
minion1:/panfs/home/lfink/IlluminaData/illumina/scratch/platinumgenomes/primary/110614_SN103_0846_BD03UMACXX.images/Unaligned/Project_FakeProject$ cd Sa
Sample_Sample1/ Sample_Sample2/ Sample_Sample3/ Sample_Sample4/ Sample_Sample5/ Sample_Sample6/ Sample_Sample7/ Sample_Sample8/

minion1:/panfs/home/lfink/IlluminaData/illumina/scratch/platinumgenomes/primary/110614_SN103_0846_BD03UMACXX.images/Unaligned/Project_FakeProject/Sample_Sample1$ ll
total 42G
-rw-rw-r-- 1 lfink 19G May  4 13:34 Sample1_NoIndex_L001_R1_001.fastq.gz
-rw-rw-r-- 1 lfink 19G May  4 13:34 Sample1_NoIndex_L001_R2_001.fastq.gz
-rw-rw-r-- 1 lfink 167 May  3 17:00 SampleSheet.csv
=cut

	$self->writelog(BODY => "---Performing checksums on raw files---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	foreach my $fastq (@{$self->{'fastq'}}) {
		$self->writelog(BODY => "Performing checksums on $fastq");

		my ($crc, $fsize, $fname) = $self->checksum(FILE => $fastq);
		$self->writelog(BODY => "Checksum on $fastq: $crc, $fsize");
		#print STDERR "Checksum on $fastq: $crc, $fsize\n";

		# key = full path and filename
		$self->{'ck_checksum'}->{$fname} = $crc;
		$self->{'ck_filesize'}->{$fname} = $fsize;
	}

	#print Dumper $self->{'ck_checksum'};
	#print Dumper $self->{'ck_filesize'};

	$self->writelog(BODY => "Checksums complete");
	$self->writelog(BODY => "END: ".$self->timestamp());

	return;
}

################################################################################
=pod

B<check_bam()> 
 Check that all BAM files exist
 
Parameters:

Requires:
 $self->{'lanes'}
 $self->{'barcodes'}
 $self->{'fastq_root'}

Returns:
 void

Sets:
 $self->{am'}

=cut
sub check_bam {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Checking that FASTQ files exist for each lane and barcode---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	$self->fastq_root();

	# FASTQ files are created here
	# 110614_SN103_0846_BD03UMACXX.images/Unaligned/Project_D03UMACXX/Sample_lane1
	# 110614_SN103_0846_BD03UMACXX.images/Unaligned/Project_D03UMACXX/Sample_lane2
	# etc.
	# lane2_NoIndex_L002_R1.fastq.gz / lane2_NoIndex_L002_R2.fastq.gz

	my @fastq_files;

	my $sscsv	= $self->samplesheet();

	# get contents of sample sheet file
	open(FH, $sscsv);
	while(<FH>) {
		s/\r\n?/\n/g;
		chomp;

		# skip header
		next if(/^FCID/);

		$self->writelog(BODY => "$_\n");

		my ($fc, $lane, $sampleid, $sampleref, $index, $desc, $control, $recipe, $operator, $sampleproject)	= split /,/;

		$index		= 'NoIndex' if(! $index);

		# /panfs/seq_raw//120511_SN7001243_0090_BD116HACXX/Unaligned//Project_PhiX/Sample_Sample1/
		my $path	= $self->{'fastq_root'}."/Project_".$sampleproject."/Sample_".$sampleid."/";

		my $fastq1	= join "_", $sampleid, $index, 'L00'.$lane, 'R1_001';
		$fastq1		.= ".fastq.gz";
		my $fastq2	= join "_", $sampleid, $index, 'L00'.$lane, 'R2_001';
		$fastq2		.= ".fastq.gz";

		$fastq1		= $path.$fastq1;
		$fastq2		= $path.$fastq2;

		$self->writelog(BODY => "\t$fastq1\n\t$fastq2");

		if(! -e $fastq1) {
			# if file doesn't exist, try removing the _001 after the
			# read number; not sure why this changes...
			$self->writelog(BODY => "\tFile not found, attempting a new name\n\t$fastq1");
			$fastq1		=~ s/(R\d)\_\d{3}/$1/;
			if(! -e $fastq1) {
				$self->writelog(BODY => "Cannot find $fastq1");
				exit($self->EXIT_NO_FASTQ());
			}
		}
 		if(! -e $fastq2) {
			$self->writelog(BODY => "\tFile not found, attempting a new name\n\t$fastq2");
			$fastq2		=~ s/(R\d)\_\d{3}/$1/;
			if(! -e $fastq2) {
				$self->writelog(BODY => "Cannot find $fastq2");
				exit($self->EXIT_NO_FASTQ());
			}
		}

		push @fastq_files, $fastq1, $fastq2;
	}
	close(FH);

	$self->{'fastq'}	= \@fastq_files;

	$self->writelog(BODY => "Check complete");
	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}

################################################################################
=pod

B<check_fastq()> 
 Check that all FASTQ files exist
 
Parameters:

Requires:
 $self->{'lanes'}
 $self->{'barcodes'}
 $self->{'fastq_root'}

Returns:
 void

Sets:
 $self->{'fastq'}

=cut
sub check_fastq {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Checking that FASTQ files exist for each lane and barcode---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	$self->fastq_root();

	# FASTQ files are created here
	# 110614_SN103_0846_BD03UMACXX.images/Unaligned/Project_D03UMACXX/Sample_lane1
	# 110614_SN103_0846_BD03UMACXX.images/Unaligned/Project_D03UMACXX/Sample_lane2
	# etc.
	# lane2_NoIndex_L002_R1.fastq.gz / lane2_NoIndex_L002_R2.fastq.gz

	my @fastq_files;

	my $sscsv	= $self->samplesheet();

	# get contents of sample sheet file
	open(FH, $sscsv);
	while(<FH>) {
		s/\r\n?/\n/g;
		chomp;

		# skip header
		next if(/^FCID/);

		$self->writelog(BODY => "$_\n");

		my ($fc, $lane, $sampleid, $sampleref, $index, $desc, $control, $recipe, $operator, $sampleproject)	= split /,/;

		$index		= 'NoIndex' if(! $index);

		# /panfs/seq_raw//120511_SN7001243_0090_BD116HACXX/Unaligned//Project_PhiX/Sample_Sample1/
		my $path	= $self->{'fastq_root'}."/Project_".$sampleproject."/Sample_".$sampleid."/";

		my $fastq1	= join "_", $sampleid, $index, 'L00'.$lane, 'R1_001';
		$fastq1		.= ".fastq.gz";
		my $fastq2	= join "_", $sampleid, $index, 'L00'.$lane, 'R2_001';
		$fastq2		.= ".fastq.gz";

		$fastq1		= $path.$fastq1;
		$fastq2		= $path.$fastq2;

		$self->writelog(BODY => "\t$fastq1\n\t$fastq2");

		if(! -e $fastq1) {
			# if file doesn't exist, try removing the _001 after the
			# read number; not sure why this changes...
			$self->writelog(BODY => "\tFile not found, attempting a new name\n\t$fastq1");
			$fastq1		=~ s/(R\d)\_\d{3}/$1/;
			if(! -e $fastq1) {
				$self->writelog(BODY => "Cannot find $fastq1");
				exit($self->EXIT_NO_FASTQ());
			}
		}
 		if(! -e $fastq2) {
			$self->writelog(BODY => "\tFile not found, attempting a new name\n\t$fastq2");
			$fastq2		=~ s/(R\d)\_\d{3}/$1/;
			if(! -e $fastq2) {
				$self->writelog(BODY => "Cannot find $fastq2");
				exit($self->EXIT_NO_FASTQ());
			}
		}

		push @fastq_files, $fastq1, $fastq2;
	}
	close(FH);

	$self->{'fastq'}	= \@fastq_files;

	$self->writelog(BODY => "Check complete");
	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}

################################################################################
=pod

B<rename_bam()> 
 Rename BAM files
 
Parameters:

Requires:
 $self->{'bam'}

Returns:
 void

Sets:
 $self->{'bam'}

=cut
sub rename_bam {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Renaming BAM files for each lane and barcode---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	my @newfastq;

	foreach my $file (@{$self->{'bam'}}) {
		my ($v, $d, $f)	= File::Spec->splitpath($file);

		# FASTQ files are created here
		# 110614_SN103_0846_BD03UMACXX.images/Unaligned/Project_D03UMACXX/Sample_lane1
		# 110614_SN103_0846_BD03UMACXX.images/Unaligned/Project_D03UMACXX/Sample_lane2
		# /panfs/seq_raw//120511_SN7001243_0090_BD116HACXX/Unaligned//Project_PhiX/Sample_Sample1/Sample1_NoIndex_L001_R2_001.fastq.gz
        	# /panfs/seq_raw//120511_SN7001243_0090_BD116HACXX/Unaligned//Project_Colo-829/Sample_Library_20120510_D/Library_20120510_D_NoIndex_L002_R1_001.fastq.gz

		# if a barcode is used, it should be a sequence: NA10831_ATCACG_L002_R1_001.fastq.gz

		# sampleid               barcode     lane     read    001-may not be present
		# Sample1             _  NoIndex  _  L001  _  R2    _ 001.fastq.gz
		# NA10831             _  ATCACG   _  L002  _  R1    _ 001.fastq.gz
		# Library_20120510_D  _  NoIndex  _  L002  _  R1    _ 001.fastq.gz

		$f		=~ /^.+\_(\w+)\_L00(\d)\_R(\d)/;
		my $barcode	= $1;
		my $lane	= $2;
		$lane		= 'lane_'.$lane;
		my $read	= $3;

		$barcode	= 'nobc' if($barcode eq 'NoIndex');

		#print STDERR "RENAME $f: $barcode , $lane , $read\n";

		my $fastq	= join ".", $self->{'slide_name'}, $lane, $barcode, $read, "fastq.gz";

		my $path	= $self->SEQ_RAW_PATH.$self->{'slide_name'}."/";

		$fastq		= $path.$fastq;

		my $cmd		= qq{mv $file $fastq};
		my $rv		= system($cmd);
		# in case files were renamed incorrectly, make it easy to put
		# them back to the initial name and path
		my $backout	= qq{mv $fastq $file};
		$self->writelog(BODY => "BACKOUT $backout");

		$self->writelog(BODY => "$rv : $cmd");

		if($rv != 0) {
			$self->writelog(BODY => "Cannot rename or move $file");
			exit($self->EXIT_NO_FASTQ());
		}

		push @newfastq, $fastq;
	}

	$self->{'fastq'}	= undef;
	$self->{'fastq'}	= \@newfastq;

	$self->writelog(BODY => "Complete");
	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}

################################################################################
=pod

B<rename_fastq()> 
 Rename and move FASTQ files
 
Parameters:

Requires:
 $self->{'fastq'}

Returns:
 void

Sets:
 $self->{'fastq'}

=cut
sub rename_fastq {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Renaming and moving FASTQ files for each lane and barcode---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	my @newfastq;

	foreach my $file (@{$self->{'fastq'}}) {
		my ($v, $d, $f)	= File::Spec->splitpath($file);

		# FASTQ files are created here
		# 110614_SN103_0846_BD03UMACXX.images/Unaligned/Project_D03UMACXX/Sample_lane1
		# 110614_SN103_0846_BD03UMACXX.images/Unaligned/Project_D03UMACXX/Sample_lane2
		# /panfs/seq_raw//120511_SN7001243_0090_BD116HACXX/Unaligned//Project_PhiX/Sample_Sample1/Sample1_NoIndex_L001_R2_001.fastq.gz
        	# /panfs/seq_raw//120511_SN7001243_0090_BD116HACXX/Unaligned//Project_Colo-829/Sample_Library_20120510_D/Library_20120510_D_NoIndex_L002_R1_001.fastq.gz

		# if a barcode is used, it should be a sequence: NA10831_ATCACG_L002_R1_001.fastq.gz

		# sampleid               barcode     lane     read    001-may not be present
		# Sample1             _  NoIndex  _  L001  _  R2    _ 001.fastq.gz
		# NA10831             _  ATCACG   _  L002  _  R1    _ 001.fastq.gz
		# Library_20120510_D  _  NoIndex  _  L002  _  R1    _ 001.fastq.gz

		$f		=~ /^.+\_(\w+)\_L00(\d)\_R(\d)/;
		my $barcode	= $1;
		my $lane	= $2;
		$lane		= 'lane_'.$lane;
		my $read	= $3;

		$barcode	= 'nobc' if($barcode eq 'NoIndex');

		#print STDERR "RENAME $f: $barcode , $lane , $read\n";

		my $fastq	= join ".", $self->{'slide_name'}, $lane, $barcode, $read, "fastq.gz";

		my $path	= $self->SEQ_RAW_PATH.$self->{'slide_name'}."/";

		$fastq		= $path.$fastq;

		my $cmd		= qq{mv $file $fastq};
		my $rv		= system($cmd);
		# in case files were renamed incorrectly, make it easy to put
		# them back to the initial name and path
		my $backout	= qq{mv $fastq $file};
		$self->writelog(BODY => "BACKOUT $backout");

		$self->writelog(BODY => "$rv : $cmd");

		if($rv != 0) {
			$self->writelog(BODY => "Cannot rename or move $file");
			exit($self->EXIT_NO_FASTQ());
		}

		push @newfastq, $fastq;
	}

	$self->{'fastq'}	= undef;
	$self->{'fastq'}	= \@newfastq;

	$self->writelog(BODY => "Complete");
	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}

################################################################################
=pod

B<bam2fastq()> 
 Convert BAM files to FASTQ files
 
Parameters:

Requires:
 $self->{'bam'}

Returns:
 void

Sets:
 $self->{'fastq'}

=cut
sub bam2fastq {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Converting BAM files to FASTQ files---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	foreach my $bam (@{$self->{'bam'}}) {
		my ($v, $d, $f)	= File::Spec->splitpath($bam);

		# 120523_SN7001240_0047_BD12NAACXX.lane_1.nobc.bam
		my ($fastq1, $fastq2)	= $bam;
		$fastq1		=~ s/\.bam/\.1\.fastq/;
		$fastq2		=~ s/\.bam/\.2\.fastq/;

		my $cmd		= qq{java -jar /share/software/picard-tools-1.62/SamToFastq.jar INPUT=<BAM> FASTQ=<FASTQ1> SECOND_END_FASTQ=<FASTQ2> VALIDATION_STRINGENCY=LENIENT};

		$cmd		=~ s/<BAM>/$bam/;
		$cmd		=~ s/<FASTQ1>/$fastq1/;
		$cmd		=~ s/<FASTQ2>/$fastq2/;

		# don't bother converting if they've already been done
		next if(-e $fastq1 && -e $fastq2);

		# convert the BAM
		## FORK THESE!!!!
		my $rv		= system($cmd);

		$self->writelog(BODY => "RV: $rv : $cmd");
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

B<prepare_ingest()> 
 Make sure LiveArc is ready to accept ingestion; then perform ingestion.

Parameters:

 Requires:
  $self->{'slide_name'}

Returns:
 void

=cut
sub prepare_ingest {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Preparing for ingestion... ---");

	$self->{'mf'}	= QCMG::Automation::LiveArc->new(VERBOSE => 1);

	$self->writelog(BODY => "Inferring namespace");

	# check if slide name namespace is already in Mediaflux/LiveArc
	$self->{'ns'}	= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};

	$self->writelog(BODY => $self->{'ns'});

	$self->writelog(BODY => "Checking if namespace $self->{'ns'} exists...");
	my $r		= $self->{'mf'}->namespace_exists(NAMESPACE => $self->{'ns'});
	$self->writelog(BODY => "RV: $r");	# 1 = does not exist; 0 = exists

	# if no namespace, create it
	unless($r == 0) {
		$self->writelog(BODY => "Creating $self->{'ns'}");
	
		my $cmd			= $self->{'mf'}->create_namespace(NAMESPACE => $self->{'ns'}, CMD => 1);
		$self->writelog(BODY => $cmd);

		$r			= $self->{'mf'}->create_namespace(NAMESPACE => $self->{'ns'});
		$self->writelog(BODY => "RV: $r");

		$self->writelog(BODY => "Checking if namespace $self->{'ns'} exists...");
		$r		= $self->{'mf'}->namespace_exists(NAMESPACE => $self->{'ns'});
		$self->writelog(BODY => "RV: $r");	# 1 = does not exist; 0 = exists

		unless($r == 0) {
			$self->writelog(BODY => "Namespace $self->{'ns'} does not exist");
			exit($self->EXIT_LIVEARC_ERROR());
		}
	}

	### PERFORM INGESTION
	$IMPORT_ATTEMPTED = 'yes';
	my $date = $self->timestamp();
	$self->writelog(BODY => "Ingest import started $date");

	# broadcast and record status
	my $subj	= $self->{'slide_name'}." RAW INGESTION -- INITIATED";
	my $body	= "RAW INGESTION of ".$self->{'slide_name'}." initiated.";
	$body		.= "\nCheck log file in ".$self->{'log_file'};
	#$self->send_email(SUBJ => $subj, BODY => $body);

	return();
}

################################################################################
=pod

B<ingest_samplesheet()> 
 Ingest SampleSheet.csv file

Parameters:

 Requires:
  $self->{'samplesheet'}

Returns:
 void

=cut
sub ingest_samplesheet {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Ingesting SampleSheet.csv File---");

	my $status	= 1;

	my $asset	= $self->{'slide_name'}."_SampleSheet.csv";

	my $id = $self->{'mf'}->asset_exists(NAMESPACE => $self->{'ns'}, ASSET => $asset);
	if(! $id) {
		$self->writelog(BODY => "Creating asset");
		$id = $self->{'mf'}->create_asset(NAMESPACE => $self->{'ns'}, ASSET => $asset);
		$self->writelog(BODY => "Created asset with ID: $id");
	}

	if($id > 0) {
		$self->writelog(BODY => "Importing file into asset");
		$status = $self->{'mf'}->update_asset_file(ID => $id, FILE => $self->{'samplesheet'});
		$self->writelog(BODY => "RV: $status");
	}

	if($status != 0) {
		exit($self->EXIT_IMPORT_FAILED());
	}

	return();
}

################################################################################
=pod

B<ingest_bam()> 
 Ingest all BAM files

Parameters:

 Requires:
  $self->{'bam'}

Returns:
 void

=cut
sub ingest_bam {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Ingesting BAM Files---");

	my $status;

	foreach my $bam (@{$self->{'bam'}}) {
		my ($v, $d, $f)	= File::Spec->splitpath($bam);
		my $asset	= $f;

		my $id = $self->{'mf'}->asset_exists(NAMESPACE => $self->{'ns'}, ASSET => $asset);
		if(! $id) {
			$self->writelog(BODY => "Creating asset");
			$id = $self->{'mf'}->create_asset(NAMESPACE => $self->{'ns'}, ASSET => $asset);
		}
		$self->writelog(BODY => "Importing file into asset");
		$status = $self->{'mf'}->update_asset_file(ID => $id, FILE => $bam);
		$self->writelog(BODY => "RV: $status");

		my $meta;
		if($self->{'ck_checksum'}->{$bam}) {
			$self->writelog(BODY => "Generating LiveArc metadata...");
	
			$meta = qq{:meta < :checksums < :source orig };
				$meta .= qq{ :checksum < :chksum };
				$meta .= $self->{'ck_checksum'}->{$bam}.' ';
				$meta .= qq{ :filesize };
				$meta .= $self->{'ck_filesize'}->{$bam}.' ';
				$meta .= qq{ :filename $bam > };
			$meta .= qq{ > >};
	
			#print STDERR "$meta\n";
			$self->writelog(BODY => "Metadata: $meta");
		}
		else {
			$self->writelog(BODY => "Skipping LiveArc metadata generation...");
		}
		$self->writelog(BODY => "Done checking metadata");

		if($status == 0 && $meta) {
			$self->writelog(BODY => "Updating metadata...");
			my $r = $self->{'mf'}->update_asset(NAMESPACE => $self->{'ns'}, ASSET => $asset, DATA => $meta);
			$self->writelog(BODY => "Update metadata result: $r");
		}

		if($status != 0) {
			exit($self->EXIT_IMPORT_FAILED());
		}
	}

	
	$IMPORT_SUCCEEDED = 'yes';

	return();
}

################################################################################
=pod

B<ingest_fastq()> 
 Ingest all FASTQ files

Parameters:

 Requires:
  $self->{'fastq'}

Returns:
 void

=cut
sub ingest_fastq {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Ingesting FASTQ Files---");

	my $status;



	foreach my $fastq (@{$self->{'fastq'}}) {
		my ($v, $d, $f)	= File::Spec->splitpath($fastq);
		my $asset	= $f;

		my $id = $self->{'mf'}->asset_exists(NAMESPACE => $self->{'ns'}, ASSET => $asset);
		if(! $id) {
			$self->writelog(BODY => "Creating asset");
			$id = $self->{'mf'}->create_asset(NAMESPACE => $self->{'ns'}, ASSET => $asset);
		}
		$self->writelog(BODY => "Importing file into asset");
		$status = $self->{'mf'}->update_asset_file(ID => $id, FILE => $fastq);
		$self->writelog(BODY => "RV: $status");

		my $meta;
		if($self->{'ck_checksum'}->{$fastq}) {
			$self->writelog(BODY => "Generating LiveArc metadata...");
	
			$meta = qq{:meta < :checksums < :source orig };
				$meta .= qq{ :checksum < :chksum };
				$meta .= $self->{'ck_checksum'}->{$fastq}.' ';
				$meta .= qq{ :filesize };
				$meta .= $self->{'ck_filesize'}->{$fastq}.' ';
				$meta .= qq{ :filename $fastq > };
			$meta .= qq{ > >};
	
			#print STDERR "$meta\n";
			$self->writelog(BODY => "Metadata: $meta");
		}
		else {
			$self->writelog(BODY => "Skipping LiveArc metadata generation...");
		}
		$self->writelog(BODY => "Done checking metadata");

		if($status == 0 && $meta) {
			$self->writelog(BODY => "Updating metadata...");
			my $r = $self->{'mf'}->update_asset(NAMESPACE => $self->{'ns'}, ASSET => $asset, DATA => $meta);
			$self->writelog(BODY => "Update metadata result: $r");
		}

		if($status != 0) {
			exit($self->EXIT_IMPORT_FAILED());
		}
	}

	
	$IMPORT_SUCCEEDED = 'yes';

	return();
}

################################################################################
=pod

B<set_lims_flag()> 

 On exit, set the ingest status flag in the LIMS

Parameters:
 EXIT => exit status
 BODY => status description

Returns:
 void

=cut
sub set_lims_flag {
	my $self = shift;

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->{'gl'} = QCMG::DB::Geneus->new(server   => 'http://qcmg-glprod.imb.uq.edu.au:8080/api/v1/', verbose => 0);
	$self->{'gl'}->connect;

	# don't try to set the flag if a LIMS entry doesn't exist
	if(! $self->{'glpid'}) {
		return();
	}

	my $flag	= 0;
	$flag		= 1 if($options->{'EXIT'} > 0);

	my ($data, $slide)	= $self->{'gl'}->torrent_run_by_process($self->{'glpid'}); ## ???
	#print Dumper $slide;
	my $uri;
	foreach (keys %{$slide}) {
		$uri	= $slide->{$_}->{'output_uri'};
	}
	
	#						uri, status, is_error (0=not
	#						error, 1= error)
	my ($xml)	= $self->{'gl'}->set_raw_ingest_status($uri, $options->{'BODY'}, $flag);
	if(! $xml) {
		$self->{'gl'}->unset_raw_ingest_status($uri);
		$xml	= $self->{'gl'}->set_raw_ingest_status($uri, $options->{'BODY'}, $flag);
	}

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
		if($IMPORT_ATTEMPTED eq 'yes' && $IMPORT_SUCCEEDED eq 'yes') {
			$self->writelog(BODY => "EXECLOG: errorMessage none");

			$self->{'EXIT'} = 0;

			# send success email if no errors
			my $subj	= "Re: ".$self->{'slide_name'}." RAW INGESTION -- SUCCEEDED";
			my $body	= "RAW INGESTION of $self->{'slide_name'} successful\n";
			$body		.= "See log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
		}
		elsif($IMPORT_ATTEMPTED eq 'no') {
			$self->writelog(BODY => "EXECLOG: errorMessage import not attempted");
	
			$self->{'EXIT'} = 17;
	
			# send success email if no errors
			my $subj	= "Re: ".$self->{'slide_name'}." RAW INGESTION -- NOT ATTEMPTED";
			my $body	= "RAW INGESTION of $self->{'slide_name'} not attempted\n";
			$body		.= "See log file: ".$self->{'hostname'}.":".$self->{'log_file'};

	
			$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
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
		my $asset	= $self->{'slide_name'}."_ingest_log";

		my $assetid = $self->{'mf'}->asset_exists(NAMESPACE => $self->{'ns'}, ASSET => $asset);

		my $status;
		if(! $assetid) {
			$assetid = $self->{'mf'}->create_asset(NAMESPACE => $self->{'ns'}, ASSET => $asset);
		}
		$status = $self->{'mf'}->update_asset_file(ID => $assetid, FILE => $self->{'log_file'});
	}

	return;
}


1;

__END__

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012-2014

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
