package QCMG::Ingest::Mapped5500;

##############################################################################
#
#  Module:   QCMG::Ingest::Mapped5500.pm
#  Creator:  Lynn Fink
#  Created:  2011-02-28
#
#  This class contains methods for automating the ingest into LiveArc
#  of mapped sequenced data
#
#  $Id$
#
##############################################################################

=pod

=head1 NAME

QCMG::Ingest::Mapped5500 -- Common functions for ingesting mapped sequencing files 

=head1 SYNOPSIS

Most common use:
 my $qi = QCMG::Ingest::Mapped5500->new();

=head1 DESCRIPTION

Contains methods for extracting slide information from a mapped sequencing slide
folder, checking the sequencing files, and ingesting them into LiveArc

=head1 REQUIREMENTS

 Exporter
 File::Spec
 QCMG::FileDir::Finder
 QCMG::FileDir::SeqRawParser
 QCMG::Automation::LiveArc
 QCMG::Automation::Config
 QCMG::Automation::Common

=cut

use strict;

# standard distro modules
use Data::Dumper;
use File::Spec;				# for parsing paths
use Cwd;
use Cwd 'realpath';			# for relative -> absolute paths

# in-house modules
use QCMG::Automation::LiveArc;		# for accessing Mediaflux/LiveArc

use vars qw( $SVNID $REVISION $BAM_IMPORT_ATTEMPTED $BAM_IMPORT_SUCCEEDED );

our @ISA = qw(QCMG::Automation::Common);	# inherit Common methods

################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 PROJECT	=> mapset name
 ANALYSIS	=> run date
 USER		=> name of barrine user that mapped this data
 LOG_FILE	=> path and filename of log file

 Optional:
	any of the variables specified in ini files (LA_HOST, etc.)...

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

	# default path to lifescope_results:
	#$self->{'lifescope_results'}	= '/panfs/imb/lifescope_results/projects/';
	$self->{'lifescope_results'}	= $self->LIFESCOPE_RESULTS;

	# the user that generated the mapped Lifescope data - need to look in
	# their subdirectory for BAMs
	$self->{'user'}		= $options->{'USER'};

	# /panfs/imb/seq_mapped/S0413_20101129_1_FragPEBC.nopd.bcB16_01
	$self->{'project'}	= $options->{'PROJECT'};
	$self->{'analysis'}	= $options->{'ANALYSIS'};

	# for re-ingesting failed ingests; only ingest assets that don't yet
	# exist ('Y', 'N')
	$self->{'update_ingest'}	= $options->{'UPDATE'};

	#print Dumper $self->{'project'};
	#print Dumper $self->{'analysis'};
	#print Dumper $self->{'user'};

	# set location of BAMs (they will be in subdirs)
	$self->project_folder();
	$self->mapset_folder();
	$self->{'mapset_name'}	= $self->{'project'};	# alias

	#print Dumper $self->{'mapset_folder'};
	#print Dumper $self->{'project_folder'};

	# create slide name: S0413_20101129_1_FragPEBC
	$self->slide_name();

	#print STDERR "DEFAULT EMAIL: ".$self->DEFAULT_EMAIL, "\n";
	push @{$self->{'email'}}, $self->DEFAULT_EMAIL;

	# define LOG_FILE
	if($options->{'LOG_FILE'}) {
		$self->{'log_file'}		= $options->{'LOG_FILE'};
	}
	else {
		# DEFAULT: /mapset/folder/ingestmapped.log
		#$self->{'log_file'}		= "/panfs/imb/automation_log/".$self->{'project'}."_ingestmapped.log";
		$self->{'log_file'}		= $self->AUTOMATION_LOG_DIR.$self->{'project'}."_ingestmapped.log";
	}
	$self->{'log_file'}			= Cwd::realpath($self->{'log_file'});
	print STDERR "LOG FILE: ".$self->{'log_file'}."\n";

	# fail if the user does not have LiveArc credentials
	if(! -e $self->LA_CRED_FILE) {
		print STDERR "LiveArc credentials file not found!\n";
		exit($self->EXIT_LIVEARC_ERROR());
	}

	$self->{'EXIT'}			= 0;	# default

	$self->{'hostname'}		= $self->HOSTKEY;

	$self->init_log_file();

	if(! -e $self->{'mapset_folder'}) {
		exit($self->EXIT_NO_FOLDER());
	}

	#my $cmd		= qq{module load samtools};
	#my $rv		= system($cmd);
	#print STDERR "$cmd\nRV: $rv\n";

	#print STDERR "DEFAULT EMAIL: ".$self->DEFAULT_EMAIL, "\n";
	#push @{$self->{'email'}}, $self->DEFAULT_EMAIL;

	# set global flags for checking in DESTROY()  (no/yes)
	$BAM_IMPORT_ATTEMPTED = 'no';
	$BAM_IMPORT_SUCCEEDED = 'no';

	return $self;
}

# Exit codes:
sub EXIT_NO_FOLDER {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} failed";
	$body		.= "\nFailed: No mapset folder: $self->{'mapset_folder'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 1;
	return 1;
}
sub EXIT_BAD_PERMS {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} failed";
	$body		.= "\nFailed: Bad permissions on $self->{'mapset_folder'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 2;
	return 2;
}
sub EXIT_LIVEARC_ERROR {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} failed";
	$body		.= "\nFailed: LiveArc error";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 9;
	return 9;
}
sub EXIT_ASSET_EXISTS {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} failed";
	$body		.= "\nFailed: LiveArc asset exists";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 12;
	return 12;
}
sub EXIT_BAD_RAWFILES {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} failed";
	$body		.= "\nFailed: Raw sequencing files are corrupt";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE  $body");

	$self->{'EXIT'} = 13;
	return 13;
}
sub EXIT_MISSING_METADATA {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} failed";
	$body		.= "\nFailed: Cannot find critical metadata";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 14;
	return 14;
}
sub EXIT_IMPORT_FAILED {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." MAPPED INGESTION -- FAILED";
	my $body	= "RAW INGESTION of $self->{'mapset_folder'} failed";
	$body		.= "\nFailed: LiveArc import failed";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 16;
	return 16;
}
sub EXIT_NO_BAM {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} failed";
	$body		.= "\nFailed: Cannot find a BAM file";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 18;
	return 18;
}
sub EXIT_CANNOT_MOVE_BAM {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} failed";
	$body		.= "\nFailed: Cannot exclude BAM from seq_mapped ingest";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 23;
	return 23;
}
sub EXIT_ASSET_NOT_EXISTS {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} failed";
	$body		.= "\nFailed: Requested LiveArc asset does not exist";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 24;
	return 24;
}
sub EXIT_NO_MA {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} failed";
	$body		.= "\nFailed: Cannot find a .ma file";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 25;
	return 25;
}

################################################################################
=pod

B<project_folder()>

Return project folder name, inferred from project, analysis, and username

Parameters:

Returns:
  scalar project folder name

=cut
sub project_folder {
	my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	# /panfs/imb/lifescope_results/projects/uqlfink/S8006_20110815_1_LMP.lane_01.nobc/20111212/outputs/bam/
	# this is a symlink to the directory with BAMs (which are in subdirs)
	my $path	= $self->{'lifescope_results'};
	$path		.= $self->{'user'}."/";
	$path		.= $self->{'project'}."/";
	$path		.= $self->{'analysis'}."/";

	$self->{'project_folder'}	= $path;

	#print STDERR "project folder: $self->{'project_folder'}\n";

	return $self->{'project_folder'};
}

################################################################################


################################################################################
=pod

B<mapset_folder()>

Return mapset folder name, inferred from project, analysis, and username

Parameters:

Returns:
  scalar mapset folder name

=cut
sub mapset_folder {
	my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	# /panfs/imb/lifescope_results/projects/uqlfink/S8006_20110815_1_LMP.lane_01.nobc/20111212/outputs/bam/
	# this is a symlink to the directory with BAMs (which are in subdirs)
	my $path	= $self->{'lifescope_results'};
	$path		.= $self->{'user'}."/";
	$path		.= $self->{'project'}."/";
	$path		.= $self->{'analysis'}."/";
	$path		.= "outputs/bam/";

	#print STDERR "Mapset folder: $path\n";

	#$self->{'mapset_folder'}	= Cwd::realpath($path);
	$self->{'mapset_folder'}	= $path;

	#print Dumper $self->{'mapset_folder'};

	return $self->{'mapset_folder'};
}

################################################################################

=pod

B<slide_name()>

Return slide name, extract from project

Parameters:

Returns:
  scalar slide name

=cut
sub slide_name {
	my $self	= shift @_;

	# S0014_20110411_1_FragPEBC.nopd.bcB16_12 ->
	# S0014_20110411_1_FragPEBC
	my $f		= $self->{'project'};

	# project = S8006_20110815_1_LMP.lane_1.nobc
	#           ----------------
	#$f		=~ /^(\w+\_\d+\_\d\_)/;
	#my $slidens	= $1.'nort';

	# project = S8006_20110815_1_LMP.lane_1.nobc
	#           --------------------
	$f		=~ /^(\w+\_\d+\_\d\_\w+)/;
	my $slidens	= $1;

	# S0014_20110411_1_FragPEBC
	$self->{'slide_name'} = $slidens;

	#print STDERR "SLIDE NAME: $1\n";

	return $self->{'slide_name'};
}

################################################################################

=pod

B<bam_asset_name()>

Return BAM asset name, same as BAM file name

Parameters:

Returns:
  scalar asset name

=cut
sub bam_asset_name {
	my $self	= shift @_;

	$self->{'bam_asset_name'} = $self->{'project'};

	return $self->{'bam_asset_name'};
}

################################################################################

=pod

B<umbam_asset_name()>

Return unmapped BAM asset name, same as unmapped BAM file name

Parameters:

Returns:
  scalar asset name

=cut
sub umbam_asset_name {
	my $self	= shift @_;

	my ($v, $d, $file)	= File::Spec->splitpath($self->{'umbam'});

	$self->{'umbam_asset_name'} = $file;

	return $self->{'umbam_asset_name'};
}

################################################################################

=pod

B<ma_asset_name()>

Return .ma asset name as mapset name with _ma

Parameters:

Returns:
  scalar asset name

=cut
sub ma_asset_name {
	my $self	= shift @_;

	my ($v, $d, $file)	= File::Spec->splitpath($self->{'ma'});

	#$self->{'ma_asset_name'} = $file;
	$self->{'ma_asset_name'} = $self->{'mapset_name'}."_ma";

	return $self->{'ma_asset_name'};
}

################################################################################

=pod

B<ssr_asset_name()>

Return solid stats report asset name, same as file name

Parameters:

Returns:
  scalar asset name

=cut
sub ssr_asset_name {
	my $self	= shift @_;

	my ($v, $d, $file)	= File::Spec->splitpath($self->{'ssr'});

	$self->{'ssr_asset_name'} = $file;

	return $self->{'ssr_asset_name'};
}

################################################################################

=pod

B<lifescopelog_asset_name()>

Return Lifescope log asset name as mapset name with _lifescope_log

Parameters:

Returns:
  scalar asset name

=cut
sub lifescopelog_asset_name {
	my $self	= shift @_;

	$self->{'lifescopelog_asset_name'} = $self->{'project'}."_lifescope_log";

	return $self->{'lifescopelog_asset_name'};
}


################################################################################

=pod

B<mapping_date()>

Extract mapping date from BAM path

Parameters:

Returns:
  scalar date string

=cut
sub mapping_date {
	my $self	= shift @_;

	my ($v, $d, $f)	= File::Spec->splitpath($self->{'bam'});

	# /panfs/imb/seq_mapped/S0416_20110421_1_FragPEBC.nopd.bcB16_05/20110521/pairing/S0416_20110421_1_FragPEBC.nopd.bcB16_05.bam
	#                                                              ----------
	$d		=~ /\/(\d{8})\//;

	$self->{'mapping_date'} = $1;

	return $self->{'mapping_date'};
}

################################################################################

=pod

B<bam_filename()>

Return BAM filename, without path

Parameters:
 BAM	=> BAM with full path

Returns:
  scalar filename

=cut
sub bam_filename {
	my $self	= shift @_;

	my ($v, $d, $file)	= File::Spec->splitpath($self->{'bam'});

	$self->{'bam_filename'} = $file;

	return $self->{'bam_filename'};
}

################################################################################

=pod

B<umbam_filename()>

Return unmapped BAM filename, without path

Parameters:

Returns:
  scalar filename

=cut
sub umbam_filename {
	my $self	= shift @_;

	my ($v, $d, $file)	= File::Spec->splitpath($self->{'umbam'});

	$self->{'umbam_filename'} = $file;

	return $self->{'umbam_filename'};
}

################################################################################

=pod

B<filename()>

Return filename, without path

Parameters:
 FILE	=> file with full path

Returns:
  scalar filename without path

=cut
sub filename {
	my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
        my $options	= {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                defined($_[($i + 1)]) || 
                        die print STDERR "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my ($v, $d, $file)	= File::Spec->splitpath($options->{'FILE'});

	return $file;
}

################################################################################

=pod

B<mapped_asset_name()>

Return seq_mapped asset name, derived from mapset name

Parameters:

Returns:
  scalar asset name

=cut
sub mapped_asset_name {
	my $self	= shift @_;

	$self->{'mapped_asset_name'} = $self->{'mapset_name'}.$self->LA_ASSET_SUFFIX_MAP;

	return $self->{'mapped_asset_name'};
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

	$self->writelog(BODY => "---Checking ".$self->{'mapset_folder'}." ---");

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                defined($_[($i + 1)]) || 
                        die print STDERR "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	if(	!    $self->{'mapset_folder'} ||
		! -e $self->{'mapset_folder'} || 
		! -d $self->{'mapset_folder'}    ) {

		$self->writelog(BODY => "No valid mapset folder provided\n");
		exit($self->EXIT_NO_FOLDER());
	}

	#if(	! -r $self->{'mapset_folder'}) {
	#	my $msg = " is not a readable directory";
	#	$self->writelog(BODY => $self->{'mapset_folder'}.$msg);
	#	exit($self->EXIT_BAD_PERMS());
	#}

	#unless($self->{'mapset_folder'} =~ /(.+?)\.(.+?)\.(.+)\/$/) {
	#	print STDERR "BAD FOLDER FORMAT\n";
	#	exit($self->EXIT_NO_FOLDER());
	#}

	$self->writelog(BODY => "Folder exists and is readable.");	

	return($status);
}

################################################################################
=pod

B<find_bam()> 
 Find primary BAM file(s) (not unmapped)
 
Parameters:

Requires:
 $self->{'mapset_folder'}

Returns:
 void

Sets:
 $self->{'bam'} = array of BAM filenames with full path

=cut

sub find_bam {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Searching for BAM in $self->{'mapset_folder'}---");

	my @files	= `find $self->{'mapset_folder'} -name "*.bam"`;

	# get all mapped BAM files
	my $count	= 0;
	foreach (@files) {
		chomp;
		$self->writelog(BODY => "BAM: $_");
		next if(/unmapped/i);
		next if(/\.bai/i);
		push @{$self->{'bam'}}, $_;
		$self->writelog(BODY => "Keeping BAM: $_");

		#print STDERR "Getting barcode...\n";
		my $bc	= $self->get_barcode(BAM => $_);

		$count++;
	}

	# no BAMs found
	if($count < 1) {
		$self->writelog(BODY => "No valid BAMs found");
		exit($self->EXIT_NO_BAM());
	}

	return();
}

################################################################################
=pod

B<find_umbam()> 
 Find unmapped BAM file
 
Parameters:

Returns:
 void

=cut

sub find_umbam {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Searching for unmapped BAM in $self->{'project_folder'}---");

	my @files	= `find $self->{'project_folder'} -name "*.bam"`;

	# get all unmapped BAM files
	foreach (@files) {
		chomp;
		$self->writelog(BODY => "Found BAM: $_");
		next unless(/unmapped/i);
		next if(/\.bai/i);
		push @{$self->{'umbam'}},  $_;
		$self->writelog(BODY => "Keeping unmapped BAM: $_");
	}

	# try to create an unmapped BAM by filtering the mapped BAM
	if(not defined $self->{'umbam'}->[0]) {
		$self->writelog(BODY => "No valid unmapped BAMs found, attempting to split mapped BAM");
		my $bam		= $self->{'bam'}->[0];
		my $umbam	= $bam;
		$umbam		=~ s/\.bam/\.unmapped\.bam/;
		my $cmd		= $self->BIN_SAMTOOLS.qq{samtools view -u -f 4 -F256 $bam > $umbam};
		$self->writelog(BODY => "Creating unmapped BAM...");
		my $rv		= system($cmd);
		$self->writelog(BODY => "$rv: $cmd");
		if(-s $umbam) {
			push @{$self->{'umbam'}},  $umbam;
			$self->writelog(BODY => "Keeping unmapped BAM: $umbam");
		}
	}

	# no BAMs found
	if(not defined $self->{'umbam'}->[0]) {
		$self->writelog(BODY => "No valid unmapped BAMs found");
		exit($self->EXIT_NO_BAM());
	}

	return();
}

################################################################################
=pod

B<find_lifescope_logs()> 
 Find Lifescope log files
 
Parameters:

Returns:
 void

=cut

sub find_lifescope_logs {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Searching for Lifescope logs for $self->{'mapset_folder'}---");

	# /panfs/imb/lifescope_results/projects/uqlfink/S8006_20110815_1_LMP.lane_02.nobc/20111212/log/
	# /panfs/imb/lifescope_results/projects/uqlfink/S8006_20110815_1_LMP.lane_02.nobc/20111212/summary.log
	my $slidedir	= join "/", $self->{'lifescope_results'}, $self->{'user'}, $self->{'project'}, $self->{'analysis'};
	my $logdir	= join "/", $slidedir, "log/";
	$self->writelog(BODY => "Log directory: $logdir");

	# copy summary.log into log/
	my $cmd		= qq{cp $slidedir/summary.log $logdir};
	$self->writelog(BODY => "Copying summary.log to $logdir:\n$cmd");
	system($cmd);

	unless(-e $logdir) {
		$self->writelog(BODY => "No Lifescope logs found");
		exit($self->EXIT_MISSING_METADATA());
	}

	$self->writelog(BODY => "Lifescope logs in $logdir");
	$self->{'lifescope_log'} =  $logdir;

	return();
}

################################################################################
=pod

B<get_barcode()> 
 Get the barcode for the run from the BAM RG field; translate it into 'nobc' if
   it is not an actual barcode
 
Parameters:
 BAM -> the BAM to get a barcode from

Requires:

Returns:
 scalar: barcode

Sets:
 $self->{'bam'} (must have already been defined by find_bam() first)

=cut

sub get_barcode {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	my $bam		= $options->{'BAM'};
	my $barcode	= 'nobc';	# default

	$self->writelog(BODY => "----Getting barcode for $bam----");

	# get BAM header and extract @RG line
	# @RG     ID:bcB16_01_1 ...
	#my $cmd	= qq{/panfs/imb/qcmg/software/LifeScope_2.5/lifescope/bin/samtools view -H $bam | grep '\@RG'};
	my $cmd		= $self->BIN_SAMTOOLS.qq{samtools view -H $bam | grep '\@RG'};
	$self->writelog(BODY => "BAM barcode fetch command: $cmd");
	my $rg		= `$cmd`;
	chomp $rg;

	if(! $rg) {
		$self->writelog(BODY => "Cannot get RG line from BAM header");
		exit($self->EXIT_MISSING_METADATA());
	}

	$rg		=~ /ID\:(.+?)\s/;
	my $id		= $1;

	#print STDERR "RG: $rg\nID: $id\n";
	# remove last digits: bcB16_04_4 -> bcB16_04
	$id	=~ s/\_\d+$//;

	if($id =~ /^bc/) {
		#print STDERR "Changing barcode from $barcode to $id\n";
		$barcode	= $id;
	}

	$self->writelog(BODY => "Using barcode $barcode");

	return($barcode);

}

################################################################################
=pod

B<rename_bam()> 
 Rename primary BAM files (not unmapped) so they have the barcode (when
  applicable) or "nobc"
 
Parameters:

Requires:
 $self->{'bam'}

Returns:
 void

Sets:
 $self->{'bam'} (must have already been defined by find_bam() first)

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

	$self->writelog(BODY => "---Renaming BAM(s) in $self->{'mapset_folder'}---");

	# get all BAM files
	foreach (@{$self->{'bam'}}) {
		chomp;
		$self->writelog(BODY => "BAM: $_");

		my $barcode	= $self->get_barcode(BAM => $_);

		$self->writelog(BODY => "Using barcode $barcode for $_");

		my ($v, $d, $f)	= File::Spec->splitpath($_);

		# S8006_20110815_1_LMP.lane_02.nobc-1-1.bam (LMP)
		# S8006_20111011_1_FragPEBC.lane_03.nobc-1-Idx_1-1.bam (BC)
		# S8006_20111219_1_FragPEBC.lane_2.nobc.bam (from earlier ingest)
		#print STDERR "barcode $barcode for BAM $_\n";
		my $newbam	= $f;
		if($newbam =~ /nobc/) {
			$newbam		=~ s/(.+)(nobc.*?)(\.bam)/$1$barcode$3/;
		}
		elsif($newbam =~ /bcB16/) {
			# if already properly named, don't do anything
			$newbam	= $f;
		}
		else {
			# if files have not been through the raw ingest so were
			# not renamed properly:
			# S8006_20111219_2_03-1-Idx_13-13.bam
			#                      ser  date  fc   lane idx  .bam
			$newbam		=~ /^(\w+\_\d+\_\d+\_)(\d+)(.+)(\.bam)/;
			my $lane	= $2;
			$lane		=~ s/^0//;
			# get run type from project name: # S8006_20111219_2_FragPEBC.lane_3.nobc
			$self->{project}	=~ /^(\w+\_\d+\_\d+\_)(\w+)/;
			my $runtype	= $2;
			$newbam		= $1.$runtype.".lane_".$lane.".".$barcode.".bam";
		}
		#print STDERR "BAM: $f -> $1 $2 $3\n$newbam";
		# add full path to new filename
		$newbam		= $d."/".$newbam;

		# kludge...
		$newbam		=~ s/\_\_/\_/g;

		my $cmd		= qq{mv $_ $newbam};
		$self->writelog(BODY => "$cmd");
		my $rv		= system($cmd);
		$self->writelog(BODY => "RV: $rv");

		# reassign BAM filename to new name
		$_		= $newbam;
	}

	#print Dumper $self->{'bam'};

	return();
}

################################################################################
=pod

B<rename_umbam()> 
 Rename unmapped BAM files so they have the barcode (when
  applicable) or "nobc" as well as the "unmapped" tag
 
Parameters:

Returns:
 void

=cut

sub rename_umbam {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Renaming unmapped BAM(s) in $self->{'mapset_folder'}---");

	# get all BAM files
	foreach (@{$self->{'umbam'}}) {
		chomp;
		$self->writelog(BODY => "BAM: $_");

		my $barcode	= $self->get_barcode(BAM => $_);

		$self->writelog(BODY => "Using barcode $barcode for $_");

		my ($v, $d, $f)	= File::Spec->splitpath($_);

		my $newbam	= $f;
		if($newbam =~ /nobc/) {
			# if tried to rename previously, but had nobc instead of
			# barcode
			#$newbam		=~ s/(.+)(nobc.*?)(\.bam)/$1$barcode$3/;
			$newbam		=~ /(.+)(nobc.*?)(\.bam)/;
			$newbam		= $1.$barcode.".unmapped.bam";
		}
		elsif($newbam =~ /bcB/ && $newbam =~ /unmapped/) {
			# if already properly named, don't do anything
			$newbam	= $f;
		}
		else {
			# if files have not been through the raw ingest so were
			# not renamed properly:
			# S8006_20111219_2_03-1-Idx_13-13.bam
			#                      ser  date  fc   lane idx  .bam
			$newbam		=~ /^(\w+\_\d+\_\d+\_)(\d+)(.+)(\.bam)/;
			my $lane	= $2;
			$lane		=~ s/^0//;
			# get run type from project name: # S8006_20111219_2_FragPEBC.lane_3.nobc
			$self->{project}	=~ /^(\w+\_\d+\_\d+\_)(\w+)/;
			my $runtype	= $2;
			# get run type from project name: # S8006_20111219_2_FragPEBC.lane_3.nobc
			$newbam		= $1.$runtype.".lane_".$lane.".".$barcode.".unmapped.bam";
		}
		#print STDERR "BAM: $f -> $1 $2 $3\n$newbam";
		# add full path to new filename
		$newbam		= $d."/".$newbam;

		# kludge...
		$newbam		=~ s/\_\_/\_/g;

		#my $newbam	= $f;
		#$newbam		=~ s/(.+)(nobc.*?)(\.bam)/$1$barcode\.unmapped$3/;
		#$newbam		= $d."/".$newbam;

		my $cmd		= qq{mv $_ $newbam};
		$self->writelog(BODY => "$cmd");
		my $rv		= system($cmd);
		$self->writelog(BODY => "RV: $rv");

		$_		= $newbam;
	}

	#print Dumper $self->{'umbam'};

	return();
}

################################################################################
=pod

B<generate_stats_report()> 
 Run solid_stats_report.pl for each mapped BAM
 
Parameters:

Requires:
 $self->{'bam'}

Returns:
 void

Sets:
 $self->{'ssr'} - array of solid stat report names (with full path)

=cut

sub generate_stats_report {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Generating stats reports for BAM(s) in $self->{'mapset_folder'}---");

	# get all BAM files
	foreach (@{$self->{'bam'}}) {
		chomp;
		$self->writelog(BODY => "BAM: $_");

		# remove file from BAM name to get path to outputs dir
		# change /bam/ to /bamstats/
		my ($v, $d, $f)	= File::Spec->splitpath($_);
		$d		=~ s/outputs\/.+?\/Group/outputs\/bamstats\/Group/;
		$d		=~ s/\/\//\//g;
		# create report filename and add path
		$f		=~ s/\.bam/\.solidstats\.xml/;
		$f		= $d."/".$f;

		$self->writelog(BODY => "Using bamstats path $d and report name $f");

		my $cmd		= $self->BIN_SEQTOOLS.qq{solid_stats_report.pl -c $d -x $f};
		$self->writelog(BODY => "$cmd");
		my $rv		= system($cmd);
		$self->writelog(BODY => "RV: $rv");

		push @{$self->{'ssr'}}, $f;	# full path and filename

	}

	print Dumper $self->{'ssr'};

	return();
}

################################################################################
=pod

B<checksum_bam()> 
 Perform checksum on BAM(s)
 
Parameters:

Returns:
 void

=cut
sub checksum_bam {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Performing checksum on BAM(s)---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	foreach (@{$self->{'bam'}}) {
		my ($crc, $fsize, $fname) = $self->checksum(FILE => $_);
		$self->writelog(BODY => "Checksum on ".$_.": $crc, $fsize");

		my $bam	= $self->bam_filename();	# no path

		$self->{'ck_checksum'}->{$bam} = $crc;
		$self->{'ck_filesize'}->{$bam} = $fsize;
	}

	#print Dumper $self->{'ck_checksum'};

	$self->writelog(BODY => "Checksum complete");
	$self->writelog(BODY => "END: ".$self->timestamp());

	return;
}

################################################################################
=pod

B<checksum_umbam()> 
 Perform checksum on unmapped BAM(s)
 
Parameters:

Returns:
 void

=cut
sub checksum_umbam {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Performing checksum on unmapped BAM(s)---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	foreach (@{$self->{'umbam'}}) {
		my ($crc, $fsize, $fname) = $self->checksum(FILE => $_);
		$self->writelog(BODY => "Checksum on ".$_.": $crc, $fsize");

		my $umbam	= $self->umbam_filename();	# no path

		$self->{'ck_checksum'}->{$umbam} = $crc;
		$self->{'ck_filesize'}->{$umbam} = $fsize;
	}

	$self->writelog(BODY => "Checksum complete");
	$self->writelog(BODY => "END: ".$self->timestamp());

	return;
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

B<send_email()> 
 Send email to notify of ingestion.
 
Parameters:
 SUBJECT
 BODY

Returns:
 void

=cut

sub send_email {
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

	my $emails = join ", ", @{$self->{'email'}};
	$self->writelog(BODY => "Sending email to: $emails");

	# requires editing /etc/mail/sendmail.mc:
	# dnl define(`SMART_HOST',`smtp.your.provider')dnl -> 
	#     define(`SMART_HOST',`smtp.imb.uq.edu.au')
	# then recompiling sendmail, allegedly
	my $fromemail	= 'mediaflux@uq.edu.au';

	my $toemail	= join ",", @{$self->{'email'}};

	#my $body	 = qq{To: $toemail\n};
	#$body		.= qq{Subject: $options->{'SUBJECT'}\n};
	#$body		.= qq{Reply-To: l.fink\@imb.uq.edu.au\n\n};

	my $sendmail	= "/usr/sbin/sendmail -t"; 
	my $reply_to	= "Reply-to: l.fink\@imb.uq.edu.au\n"; 
	my $subject	= "Subject: $options->{'SUBJECT'}\n"; 
	my $body	= $options->{'BODY'};
	my $from	= "From: $fromemail\n"; 
	my $to		= "To: $toemail\n"; 

	#print STDERR $reply_to; 
	#print STDERR $subject; 
	#print STDERR $from; 
	#print STDERR $to; 
	#print STDERR "Content-type: text/html\n\n"; 
	#print STDERR $body;

	open(SENDMAIL, "|$sendmail") or die "Cannot open $sendmail: $!"; 
	print SENDMAIL $reply_to; 
	print SENDMAIL $subject; 
	print SENDMAIL $from; 
	print SENDMAIL $to; 
	print SENDMAIL "Content-type: text/html\n\n"; 
	print SENDMAIL $body;
	close(SENDMAIL);

	return;
}

################################################################################
=pod

B<ingest_bam()> 
 Ingest BAMs  

Parameters:

Returns:
 void

=cut

sub ingest_bam {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Preparing for BAM ingestion... ---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	my $mf	= QCMG::Automation::LiveArc->new(VERBOSE => 1);

=cut
	# CAPTURE LIVEARC STDER TO VARIABLE
	# First, save away STDERR
	my $stderrcap;
	open SAVEERR, ">&STDERR";
	close STDERR;
	open STDERR, ">", \$stderrcap or warn "Cannot save STDERR to scalar in ingest_bam()\n";
=cut
	
	# check if slide name mapped namespace is already in LiveArc
	#                 /QCMG/S0449_20100603_1_Frag/S0449_20100603_1_Frag_mapped
	my $ns		= join "/", $self->LA_NAMESPACE_5500, $self->{'slide_name'}, $self->{'project'}.$self->LA_NAMESPACE_SUFFIX_MAP;
	$ns		=~ s/bcB16\_\d+/nobc/;	# replace barcode in namespace with nobc

	$ns		= "/".$ns unless($ns =~ /^\//);	# prepend with / if not already there

	#print STDERR "NS: $ns\n";

	$self->writelog(BODY => "Checking if namespace $ns exists...");
	my $r		= $mf->namespace_exists(NAMESPACE => $ns);
	# 0 if exists, 1 if does not exist
	$self->writelog(BODY => "RV: $r");

	# if no mapped namespace in the slide namespace, create it
	if($r == 1) {
		#print STDERR "$ns does not already exist, creating it\n";
		$self->writelog(BODY => "Creating $ns");

		# check for top level namespace; if that doesn't exist, create
		# it first
		my $topns	= join "/", $self->LA_NAMESPACE_5500, $self->{'slide_name'};
		$self->writelog(BODY => "Checking if slide namespace $topns exists...");
		my $r		= $mf->namespace_exists(NAMESPACE => $topns);
		$self->writelog(BODY => "RV: $r");
		if($r == 1) {
			#print STDERR "top level $topns does not already exist, creating it\n";
			my $cmd	= $mf->create_namespace(NAMESPACE => $topns, CMD => 1);
			$self->writelog(BODY => $cmd);
			my $r	= $mf->create_namespace(NAMESPACE => $topns);

			if($r == 1) {
				$self->writelog(BODY => "Cannot create $topns");
				exit($self->EXIT_LIVEARC_ERROR());
			}
			
		}
	
		# create mapped namespace
		my $cmd		= $mf->create_namespace(NAMESPACE => $ns, CMD => 1);
		$self->writelog(BODY => $cmd);
		my $r		= $mf->create_namespace(NAMESPACE => $ns);

		if($r == 1) {
			$self->writelog(BODY => "Cannot create $ns");
			exit($self->EXIT_LIVEARC_ERROR());
		}
	}
	elsif($r == 0) {
		$self->writelog(BODY => "Namespace $ns exists");
	}

	#print STDERR "Done checking namespace\n";

	### PERFORM INGESTION
	$BAM_IMPORT_ATTEMPTED = 'yes';
	my $date = $self->timestamp();
	$self->writelog(BODY => "BAM ingest started $date");

	# ingest each BAM
	my $status	= 2;
	my $attempts	= 0;
	my $maxattempts	= 4;
	foreach (@{$self->{'bam'}}) {
		# get pathless filename of BAM to use as asset name
 		my $bamasset	= $self->filename(FILE => $_);

		$self->writelog(BODY => "Checking if asset $bamasset exists...");
		my $r			= $mf->asset_exists(NAMESPACE => $ns, ASSET => $bamasset);
		# if asset exists, returns ID
		$self->writelog(BODY => "Result: $r");
		if($r) {
			if($self->{'update_ingest'} eq 'Y') {
				# if updating ingest and BAM exists, don't do it
				# again
				$self->writelog(BODY => "Asset $bamasset already exists, not ingesting it on update");
				$self->writelog(BODY => "END: ".$self->timestamp());
				$BAM_IMPORT_ATTEMPTED = 'yes';
				$BAM_IMPORT_SUCCEEDED = 'yes';
				return();
			}
			else {
				$self->writelog(BODY => "Asset $bamasset exists");
				exit($self->EXIT_ASSET_EXISTS());
			}
		}
		$self->writelog(BODY => "Done checking mapped namespace and BAM asset");

		$self->writelog(BODY => "Preparing to write metadata, if any...");
	
		# set tags for description of asset checksums in LiveArc
		my $meta;
		if($self->{'ck_checksum'}->{$bamasset}) {
			$self->writelog(BODY => "Generating LiveArc metadata...");
	
			$meta = qq{:meta < :checksums < :source orig };
			$meta .= qq{ :checksum < :chksum };
			$meta .= $self->{'ck_checksum'}->{$bamasset}.' ';
			$meta .= qq{ :filesize };
			$meta .= $self->{'ck_filesize'}->{$bamasset}.' ';
			$meta .= qq{ :filename }.$bamasset.qq{ > };
			$meta .= qq{ > >};
	
			$self->writelog(BODY => "Metadata: $meta");
		}
		else {
			$self->writelog(BODY => "Skipping LiveArc metadata generation...");
		}
		$self->writelog(BODY => "Done checking metadata");

		# initialize error status and number of attempted imports
		$status		= 2;
		$attempts	= 0;
	
		$self->writelog(BODY => "Importing $bamasset");
		# status starts at 2, will be set to 0 on success, 1 on fail; try 3
		# times, then give up
		until($status == 0 || $attempts > $maxattempts) {
			my $id = $mf->laimport_file(NAMESPACE => $ns, ASSET => $bamasset, FILE => $_);
			$self->writelog(BODY => "Import result: $id");
	
			$self->writelog(BODY => "Checking if asset $bamasset exists...");
			my $r			= $mf->asset_exists(NAMESPACE => $ns, ASSET => $bamasset);
			# if asset exists, returns ID
			$self->writelog(BODY => "Result: $r");
	
			$status = 0 unless($r =~ /error/i);
	
			#$status = 0 if($id > 0);
	
			$attempts++;
	
			sleep(60) if($status != 0);
		}
	
		$self->writelog(BODY => "Updating metadata...");
		# set metadata if import was successful
		if($status == 0 && $meta) {
			my $r = $mf->update_asset(NAMESPACE => $ns, ASSET => $bamasset, DATA => $meta);
			$self->writelog(BODY => "Update metadata result: $r");
		}
	}

	$self->writelog(BODY => "Import complete");

=cut
	# Now close and restore STDERR to original condition.
	close STDERR;
	open STDERR, ">&SAVEERR";
	$self->writelog(BODY => "$stderrcap");
=cut

	if($status != 0) {
		exit($self->EXIT_IMPORT_FAILED());
	}
	else {
		$BAM_IMPORT_SUCCEEDED = 'yes';
		#print STDERR "BAM_IMPORT_SUCCEEDED: $BAM_IMPORT_SUCCEEDED\n";
	}

	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}

################################################################################
=pod

B<ingest_umbam()> 
 Ingest unmapped BAMs  

Parameters:

Returns:
 void

=cut

sub ingest_umbam {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Preparing for BAM ingestion... ---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	my $mf	= QCMG::Automation::LiveArc->new(VERBOSE => 1);

=cut
	# CAPTURE LIVEARC STDER TO VARIABLE
	# First, save away STDERR
	my $stderrcap;
	open SAVEERR, ">&STDERR";
	close STDERR;
	open STDERR, ">", \$stderrcap or warn "Cannot save STDERR to scalar in ingest_bam()\n";
=cut
	
	# check if slide name mapped namespace is already in LiveArc
	#                 /QCMG/S0449_20100603_1_Frag/S0449_20100603_1_Frag_mapped
	my $ns		= join "/", $self->LA_NAMESPACE_5500, $self->{'slide_name'}, $self->{'project'}.$self->LA_NAMESPACE_SUFFIX_MAP;
	$ns		=~ s/bcB16\_\d+/nobc/;	# replace barcode in namespace with nobc

	#print STDERR "NS: $ns\n";

	$self->writelog(BODY => "Checking if namespace $ns exists...");
	my $r		= $mf->namespace_exists(NAMESPACE => $ns);
	# 0 if exists, 1 if does not exist
	$self->writelog(BODY => "RV: $r");

	# if no mapped namespace in the slide namespace, create it
	if($r == 1) {
		$self->writelog(BODY => "Creating $ns");

		# check for top level namespace; if that doesn't exist, create
		# it first
		my $topns	= join "/", $self->LA_NAMESPACE_5500, $self->{'slide_name'};
		$self->writelog(BODY => "Checking if slide namespace $topns exists...");
		my $r		= $mf->namespace_exists(NAMESPACE => $topns);
		$self->writelog(BODY => "RV: $r");
		if($r == 1) {
			my $cmd	= $mf->create_namespace(NAMESPACE => $topns, CMD => 1);
			$self->writelog(BODY => $cmd);
			my $r	= $mf->create_namespace(NAMESPACE => $topns);

			if($r == 1) {
				$self->writelog(BODY => "Cannot create $topns");
				exit($self->EXIT_LIVEARC_ERROR());
			}
			
		}
	
		# create mapped namespace
		my $cmd		= $mf->create_namespace(NAMESPACE => $ns, CMD => 1);
		$self->writelog(BODY => $cmd);
		my $r		= $mf->create_namespace(NAMESPACE => $ns);

		if($r == 1) {
			$self->writelog(BODY => "Cannot create $ns");
			exit($self->EXIT_LIVEARC_ERROR());
		}
	}
	elsif($r == 0) {
		$self->writelog(BODY => "Namespace $ns exists");
	}

	#print STDERR "Done checking namespace\n";

	### PERFORM INGESTION
	#$BAM_IMPORT_ATTEMPTED = 'yes';
	my $date = $self->timestamp();
	$self->writelog(BODY => "unmapped BAM ingest started $date");

	# ingest each BAM
	my $status	= 2;
	my $attempts	= 0;
	my $maxattempts	= 4;
	foreach (@{$self->{'umbam'}}) {
		# get pathless filename of BAM to use as asset name
 		my $bamasset	= $self->filename(FILE => $_);

		$self->writelog(BODY => "Checking if asset $bamasset exists...");
		my $r			= $mf->asset_exists(NAMESPACE => $ns, ASSET => $bamasset);
		# if asset exists, returns ID
		$self->writelog(BODY => "Result: $r");
		if($r) {
			if($self->{'update_ingest'} eq 'Y') {
				# if updating ingest and BAM exists, don't do it
				# again
				$self->writelog(BODY => "Asset $bamasset already exists, not ingesting it on update");
				$self->writelog(BODY => "END: ".$self->timestamp());
				return();
			}
			else {
				$self->writelog(BODY => "Asset $bamasset exists");
				exit($self->EXIT_ASSET_EXISTS());
			}
		}
		$self->writelog(BODY => "Done checking mapped namespace and BAM asset");

		$self->writelog(BODY => "Preparing to write metadata, if any...");
	
		# set tags for description of asset checksums in LiveArc
		my $meta;
		if($self->{'ck_checksum'}->{$bamasset}) {
			$self->writelog(BODY => "Generating LiveArc metadata...");
	
			$meta = qq{:meta < :checksums < :source orig };
			$meta .= qq{ :checksum < :chksum };
			$meta .= $self->{'ck_checksum'}->{$bamasset}.' ';
			$meta .= qq{ :filesize };
			$meta .= $self->{'ck_filesize'}->{$bamasset}.' ';
			$meta .= qq{ :filename }.$bamasset.qq{ > };
			$meta .= qq{ > >};
	
			$self->writelog(BODY => "Metadata: $meta");
		}
		else {
			$self->writelog(BODY => "Skipping LiveArc metadata generation...");
		}
		$self->writelog(BODY => "Done checking metadata");

		# initialize error status and number of attempted imports
		$status		= 2;
		$attempts	= 0;
	
		$self->writelog(BODY => "Importing $bamasset");
		# status starts at 2, will be set to 0 on success, 1 on fail; try 3
		# times, then give up
		until($status == 0 || $attempts > $maxattempts) {
			my $id = $mf->laimport_file(NAMESPACE => $ns, ASSET => $bamasset, FILE => $_);
			$self->writelog(BODY => "Import result: $id");
	
			$self->writelog(BODY => "Checking if asset $bamasset exists...");
			my $r			= $mf->asset_exists(NAMESPACE => $ns, ASSET => $bamasset);
			# if asset exists, returns ID
			$self->writelog(BODY => "Result: $r");
	
			$status = 0 unless($r =~ /error/i);
	
			#$status = 0 if($id > 0);
	
			$attempts++;
	
			sleep(60) if($status != 0);
		}
	
		$self->writelog(BODY => "Updating metadata...");
		# set metadata if import was successful
		if($status == 0 && $meta) {
			my $r = $mf->update_asset(NAMESPACE => $ns, ASSET => $bamasset, DATA => $meta);
			$self->writelog(BODY => "Update metadata result: $r");
		}
	}

	$self->writelog(BODY => "Import complete");

=cut
	# Now close and restore STDERR to original condition.
	close STDERR;
	open STDERR, ">&SAVEERR";
	$self->writelog(BODY => "$stderrcap");
=cut

	if($status != 0) {
		exit($self->EXIT_IMPORT_FAILED());
	}

	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}
################################################################################
=pod

B<ingest_ssr()> 
 Ingest solid stats report

Parameters:

Returns:
 void

=cut

sub ingest_ssr {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Preparing for SOLiD stats report ingestion... ---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	my $mf	= QCMG::Automation::LiveArc->new(VERBOSE => 1);

	# CAPTURE LIVEARC STDER TO VARIABLE
	# First, save away STDERR
	my $stderrcap;
	open SAVEERR, ">&STDERR";
	close STDERR;
	open STDERR, ">", \$stderrcap or warn "Cannot save STDERR to scalar in ingest_ssr()\n";
	
	# check if slide name mapped namespace is already in LiveArc
	#                 /QCMG/S0449_20100603_1_Frag/S0449_20100603_1_Frag_mapped
	my $ns		= join "/", $self->LA_NAMESPACE_5500, $self->{'slide_name'}, $self->{'project'}.$self->LA_NAMESPACE_SUFFIX_MAP;
	$ns		=~ s/bcB16\_\d+/nobc/;	# replace barcode in namespace with nobc


	#my $asset	= $self->ssr_asset_name();
	my $assetid	= "";

	$self->writelog(BODY => "Checking if namespace $ns exists...");
	my $r		= $mf->namespace_exists(NAMESPACE => $ns);
	$self->writelog(BODY => "RV: $r");

	# if no mapped namespace in the slide namespace, create it
	if($r == 1) {
		$self->writelog(BODY => "Creating $ns");

		# check for top level namespace; if that doesn't exist, create
		# it first
		my $topns	= join "/", $self->LA_NAMESPACE_5500, $self->{'slide_name'};
		$self->writelog(BODY => "Checking if slide namespace $topns exists...");
		my $r		= $mf->namespace_exists(NAMESPACE => $topns);
		$self->writelog(BODY => "RV: $r");
		if($r == 1) {
			my $cmd	= $mf->create_namespace(NAMESPACE => $topns, CMD => 1);
			$self->writelog(BODY => $cmd);
			my $r	= $mf->create_namespace(NAMESPACE => $topns);

			if($r == 1) {
				$self->writelog(BODY => "Cannot create $topns");
				#exit($self->EXIT_LIVEARC_ERROR());
			}
			
		}
	
		# create mapped namespace
		my $cmd		= $mf->create_namespace(NAMESPACE => $ns, CMD => 1);
		$self->writelog(BODY => $cmd);
		my $r		= $mf->create_namespace(NAMESPACE => $ns);

		if($r == 1) {
			$self->writelog(BODY => "Cannot create $ns");
			#exit($self->EXIT_LIVEARC_ERROR());
		}
	}
	elsif($r == 0) {
		$self->writelog(BODY => "Namespace $ns exists");

=cut
		$self->writelog(BODY => "Checking if asset $asset exists...");
		my $r			= $mf->asset_exists(NAMESPACE => $ns, ASSET => $asset);
		$self->writelog(BODY => "Result: $r");
		if($r) {
			if($self->{'update_ingest'} eq 'Y') {
				# if updating ingest and SSR asset exists, don't do it
				# again
				$self->writelog(BODY => "Asset $asset already exists, not ingesting it on update");
				$self->writelog(BODY => "END: ".$self->timestamp());
				return();
			}
			else {
				$self->writelog(BODY => "Asset $asset exists");
				exit($self->EXIT_ASSET_EXISTS());
			}
		}
		$self->writelog(BODY => "Done checking mapped namespace and SSR assets");
=cut
	}

	# initialize error status and number of attempted imports
	my $status	= 2;
	my $attempts	= 0;
	my $maxattempts	= 4;

	### PERFORM INGESTION
	my $date = $self->timestamp();
	$self->writelog(BODY => "SOLiD stats report ingest started $date");

	# import solid_stats_report into LiveArc in the same namespace as the
	# data
	#
	# asset = S0449_20100603_1_Frag.nopd.nobc.solidstats.xml
	# file  = /panfs/imb/seq_mapped/S0449_20100603_1_Frag.nopd.nobc/S0449_20100603_1_Frag.nopd.nobc.solidstats.xml

	$self->writelog(BODY => "Importing solid stats report(s) to $ns");
	my $la		= QCMG::Automation::LiveArc->new(VERBOSE => 1);

	# status starts at 2, will be set to 0 on success, 1 on fail; try 3
	# times, then give up
	until($status == 0 || $attempts > $maxattempts) {
		foreach (@{$self->{'ssr'}}) {
			my ($v, $d, $asset) = File::Spec->splitpath($_);
			my $id = $mf->laimport_file(NAMESPACE => $ns, ASSET => $asset, FILE => $_);
			$self->writelog(BODY => "Import result: $id");
			$status = 0 if($id > 0);

			$attempts++;

			sleep(60) if($status != 0);
		}
	}

	$self->writelog(BODY => "Import complete");

	# Now close and restore STDERR to original condition.
	close STDERR;
	open STDERR, ">&SAVEERR";
	$self->writelog(BODY => "$stderrcap");

	if($status != 0) {
		exit($self->EXIT_IMPORT_FAILED());
	}

	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}

################################################################################
=pod

B<ingest_lifescope_logs()> 
 Ingest Lifescope log directory

Parameters:

Returns:
 void

=cut

sub ingest_lifescope_logs {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $status = 0;

	$self->writelog(BODY => "---Preparing for Lifescope log ingestion... ---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	my $mf	= QCMG::Automation::LiveArc->new(VERBOSE => 1);

	# CAPTURE LIVEARC STDER TO VARIABLE
	# First, save away STDERR
	my $stderrcap;
	open SAVEERR, ">&STDERR";
	close STDERR;
	open STDERR, ">", \$stderrcap or warn "Cannot save STDERR to scalar in ingest_mapped()\n";
	
	# check if slide name mapped namespace is already in LiveArc
	#                 /QCMG/S0449_20100603_1_Frag/S0449_20100603_1_Frag_mapped
	my $ns		= join "/", $self->LA_NAMESPACE_5500, $self->{'slide_name'}, $self->{'project'}.$self->LA_NAMESPACE_SUFFIX_MAP;
	$ns		=~ s/bcB16\_\d+/nobc/;	# replace barcode in namespace with nobc


	my $asset	= $self->lifescopelog_asset_name();
	my $assetid	= "";

	$self->writelog(BODY => "Checking if namespace $ns exists...");
	my $r		= $mf->namespace_exists(NAMESPACE => $ns);
	$self->writelog(BODY => "RV: $r");

	# if no mapped namespace in the slide namespace, create it
	if($r == 1) {
		$self->writelog(BODY => "Creating $ns");

		# check for top level namespace; if that doesn't exist, create
		# it first
		my $topns	= join "/", $self->LA_NAMESPACE_5500, $self->{'slide_name'};
		$self->writelog(BODY => "Checking if slide namespace $topns exists...");
		my $r		= $mf->namespace_exists(NAMESPACE => $topns);
		$self->writelog(BODY => "RV: $r");
		if($r == 1) {
			my $cmd	= $mf->create_namespace(NAMESPACE => $topns, CMD => 1);
			$self->writelog(BODY => $cmd);
			my $r	= $mf->create_namespace(NAMESPACE => $topns);

			if($r == 1) {
				$self->writelog(BODY => "Cannot create $topns");
				#exit($self->EXIT_LIVEARC_ERROR());
			}
			
		}
	
		# create mapped namespace
		my $cmd		= $mf->create_namespace(NAMESPACE => $ns, CMD => 1);
		$self->writelog(BODY => $cmd);
		my $r		= $mf->create_namespace(NAMESPACE => $ns);

		if($r == 1) {
			$self->writelog(BODY => "Cannot create $ns");
			#exit($self->EXIT_LIVEARC_ERROR());
		}
	}
	elsif($r == 0) {
		$self->writelog(BODY => "Namespace $ns exists");

		$self->writelog(BODY => "Checking if asset $asset exists...");
		my $r			= $mf->asset_exists(NAMESPACE => $ns, ASSET => $asset);
		$self->writelog(BODY => "Result: $r");
		if($r) {
			if($self->{'update_ingest'} eq 'Y') {
				# if updating ingest and logfiles asset exists, don't do it
				# again
				$self->writelog(BODY => "Asset $asset already exists, not ingesting it on update");
				$self->writelog(BODY => "END: ".$self->timestamp());
				return();
			}
			else {
				$self->writelog(BODY => "Asset $asset exists");
				exit($self->EXIT_ASSET_EXISTS());
			}
		}
		$self->writelog(BODY => "Done checking mapped namespace and Lifecope log asset");
	}

	# initialize error status and number of attempted imports
	$status		= 2;
	my $attempts	= 0;
	my $maxattempts	= 4;

	### PERFORM INGESTION
	my $date = $self->timestamp();
	$self->writelog(BODY => " ingest started $date");

	$self->writelog(BODY => "Importing $asset");
	# status starts at 2, will be set to 0 on success, 1 on fail; try 3
	# times, then give up
	until($status == 0 || $attempts > $maxattempts) {
		$status = $mf->laimport(	
					NAMESPACE	=> $ns,
					ASSET		=> $asset,
					DIR		=> $self->{'lifescope_log'}
				);
		$self->writelog(BODY => "RV: $status");

		$attempts++;

		sleep(60) if($status != 0);
	}

	$self->writelog(BODY => "Import complete");

	# Now close and restore STDERR to original condition.
	close STDERR;
	open STDERR, ">&SAVEERR";
	$self->writelog(BODY => "$stderrcap");

	if($status != 0) {
		exit($self->EXIT_IMPORT_FAILED());
	}

	$self->writelog(BODY => "END: ".$self->timestamp());

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
	$msg		.= qq{TOOLLOG: MAPPED_FOLDER $self->{'mapset_folder'}\n};
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
		# import was attempted and import succeeded
		if(	$BAM_IMPORT_ATTEMPTED eq 'yes' && $BAM_IMPORT_SUCCEEDED eq 'yes') {

			$self->writelog(BODY => "EXECLOG: errorMessage none");

			$self->{'EXIT'} = 0;

			# send success email if no errors
			my $subj	= $self->{'project'}." MAPPED INGESTION -- SUCCEEDED";
			my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} and BAM complete\n";
			$body		.= "See log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			$self->send_email(SUBJECT => $subj, BODY => $body);
		}
		# import was not attempted; script ended for some other
		# reason
		elsif($BAM_IMPORT_ATTEMPTED eq 'no') {
			$self->writelog(BODY => "EXECLOG: errorMessage BAM import not attempted");
	
			$self->{'EXIT'} = 17;
	
			# send success email if no errors
			my $subj	= $self->{'project'}." MAPPED INGESTION -- NOT ATTEMPTED";
			my $body	= "MAPPED INGESTION of $self->{'bam'} not attempted";
			$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			$self->send_email(SUBJECT => $subj, BODY => $body);
		}
	}

	# complete log file after successful ingest

	my $eltime	= $self->elapsed_time();
	my $date	= $self->timestamp();

	$self->writelog(BODY => "EXECLOG: elapsedTime $eltime");
	$self->writelog(BODY => "EXECLOG: stopTime $date");
	$self->writelog(BODY => "EXECLOG: exitStatus $self->{'EXIT'}");

	close($self->{'log_fh'});

	# if exit value is 0, ingest log file as a new asset in same namespace
	if($self->{'EXIT'} == 0) {
		#my $ns		= join "/", $self->LA_NAMESPACE_5500, $self->{'slide_name'}, $self->{'slide_name'}.$self->LA_NAMESPACE_SUFFIX_MAP;
		my $ns		= join "/", $self->LA_NAMESPACE_5500, $self->{'slide_name'}, $self->{'project'}."_mapped";
		$ns		=~ s/bcB16\_\d+/nobc/;	# replace barcode in namespace with nobc


		my $asset	= $self->{'mapset_name'};
		$asset		.= "_ingestmapped_log";

		#print STDERR "NS: $ns\nASSET: $asset\n";

		#print STDERR "Ingesting log file\n";
		my $la	= QCMG::Automation::LiveArc->new(VERBOSE => 0);
		my $assetid = $la->asset_exists(NAMESPACE => $ns, ASSET => $asset);
		my $status;
	
		#print STDERR "$assetid\n";	

		if(! $assetid) {
			#print STDERR "Creating log file asset\n";
			$assetid = $la->create_asset(NAMESPACE => $ns, ASSET => $asset);
		}
		#print STDERR "Updating log file\n";
		$status = $la->update_asset_file(ID => $assetid, FILE => $self->{'log_file'});

		print STDERR "$status\n";
	}

	return;
}


1;

__END__

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011-2014

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
