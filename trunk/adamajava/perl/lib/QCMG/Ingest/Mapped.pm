package QCMG::Ingest::Mapped;

##############################################################################
#
#  Module:   QCMG::Ingest::Mapped.pm
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

QCMG::Ingest::Mapped -- Common functions for ingesting mapped sequencing files 

=head1 SYNOPSIS

Most common use:
 my $qi = QCMG::Ingest::Mapped->new();

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
use File::Path;				# for deleting directories
use Cwd;
use Cwd 'realpath';			# for relative -> absolute paths

# in-house modules
use QCMG::Automation::Config;
use QCMG::Automation::Common;		# common methods for automation
use QCMG::Automation::LiveArc;		# for accessing Mediaflux/LiveArc
use QCMG::FileDir::Finder;		# for finding files and dirs
use QCMG::FileDir::SeqRawParser;	# for parsing raw sequencing files

use vars qw( $SVNID $REVISION $BAM_IMPORT_ATTEMPTED $BAM_IMPORT_SUCCEEDED
		$MAP_IMPORT_ATTEMPTED $MAP_IMPORT_SUCCEEDED);

our @ISA = qw(QCMG::Automation::Common);	# inherit Common methods

################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 MAPSET_FOLDER => directory
 LOG_FILE   => path and filename of log file

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

	# /panfs/imb/seq_mapped/S0413_20101129_1_FragPEBC.nopd.bcB16_01
	$self->{'mapset_folder'}	= $options->{'MAPSET_FOLDER'};

	# for re-ingesting failed ingests; only ingest assets that don't yet
	# exist ('Y', 'N')
	$self->{'update_ingest'}	= $options->{'UPDATE'};

	# make sure dir has / at end
	if($self->{'mapset_folder'} !~ /\/$/) {
		$self->{'mapset_folder'} .= "/";
	}

	# create slide name: S0413_20101129_1_FragPEBC
	$self->slide_name();

	# define LOG_FILE
	if($options->{'LOG_FILE'}) {
		$self->{'log_file'}		= $options->{'LOG_FILE'};
	}
	else {
		# DEFAULT: /mapset/folder/ingestmapped.log
		$self->{'log_file'}		= $self->{'run_folder'}."ingestmapped.log";
	}
	$self->{'log_file'}			= Cwd::realpath($self->{'log_file'});

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

	# MAPSET FOLDER NAME, should have / on the end with absolute path
	$self->{'mapset_folder'}	= Cwd::realpath($self->{'mapset_folder'})."/";

	# create mapset name: S0413_20101129_1_FragPEBC.nopd.bcB16_01
	$self->mapset_name();

	push @{$self->{'email'}}, $self->DEFAULT_EMAIL;

	# set global flags for checking in DESTROY()  (no/yes)
	$BAM_IMPORT_ATTEMPTED = 'no';
	$BAM_IMPORT_SUCCEEDED = 'no';
	$MAP_IMPORT_ATTEMPTED = 'no';
	$MAP_IMPORT_SUCCEEDED = 'no';

	return $self;
}

# Exit codes:
sub EXIT_NO_FOLDER {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} failed";
	$body		.= "\nFailed: No mapset folder: $self->{'mapset_folder'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 1;
	return 1;
}
sub EXIT_BAD_PERMS {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} failed";
	$body		.= "\nFailed: Bad permissions on $self->{'mapset_folder'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 2;
	return 2;
}
sub EXIT_LIVEARC_ERROR {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} failed";
	$body		.= "\nFailed: LiveArc error";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 9;
	return 9;
}
sub EXIT_ASSET_EXISTS {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} failed";
	$body		.= "\nFailed: LiveArc asset exists";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 12;
	return 12;
}
sub EXIT_BAD_RAWFILES {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} failed";
	$body		.= "\nFailed: Raw sequencing files are corrupt";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE  $body");

	$self->{'EXIT'} = 13;
	return 13;
}
sub EXIT_MISSING_METADATA {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} failed";
	$body		.= "\nFailed: Cannot find critical metadata";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 14;
	return 14;
}
sub EXIT_NO_BAM {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} failed";
	$body		.= "\nFailed: Cannot find a BAM file";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 18;
	return 18;
}
sub EXIT_CANNOT_MOVE_BAM {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} failed";
	$body		.= "\nFailed: Cannot exclude BAM from seq_mapped ingest";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 23;
	return 23;
}
sub EXIT_ASSET_NOT_EXISTS {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} failed";
	$body		.= "\nFailed: Requested LiveArc asset does not exist";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 24;
	return 24;
}
sub EXIT_NO_MA {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} failed";
	$body		.= "\nFailed: Cannot find a .ma file";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 25;
	return 25;
}

################################################################################

=pod

B<mapset_folder()>

Return mapset folder name, as set in new()

Parameters:

Returns:
  scalar mapset folder name

=cut
sub mapset_folder {
	my $self	= shift @_;
	return $self->{'mapset_folder'};
}

################################################################################

=pod

B<mapset_name()>

Return mapset name, extract from mapset folder

Parameters:

Returns:
  scalar mapset name

=cut
sub mapset_name {
	my $self	= shift @_;

	# /panfs/imb/seq_mapped/S0014_20110411_1_FragPEBC.nopd.bcB16_12/
	my $f		= $self->{'mapset_folder'};
	$f		=~ /.+\/(.+?)\//;

	# S0014_20110411_1_FragPEBC.nopd.bcB16_12
	$self->{'mapset_name'} = $1;

	return $self->{'mapset_name'};
}

################################################################################

=pod

B<slide_name()>

Return slide name, extract from mapset name

Parameters:

Returns:
  scalar slide name

=cut
sub slide_name {
	my $self	= shift @_;

	# /panfs/imb/seq_mapped/S0014_20110411_1_FragPEBC.nopd.bcB16_12/
	my $f		= $self->{'mapset_folder'};
	$f		=~ /.+\/(.+?)\.(.+?)\.(.+)\/$/;

	# S0014_20110411_1_FragPEBC
	$self->{'slide_name'} = $1;

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

	my ($v, $d, $file)	= File::Spec->splitpath($self->{'bam'});

	$self->{'bam_asset_name'} = $file;

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

	my ($v, $d, $file)	= File::Spec->splitpath($self->{'solid_stats_report'});

	$self->{'ssr_asset_name'} = $file;

	return $self->{'ssr_asset_name'};
}

################################################################################

=pod

B<bioscopelog_asset_name()>

Return Bioscope log asset name as mapset name with _bioscope_log

Parameters:

Returns:
  scalar asset name

=cut
sub bioscopelog_asset_name {
	my $self	= shift @_;

	my ($v, $d, $file)	= File::Spec->splitpath($self->{'bam'});

	$self->{'bioscopelog_asset_name'} = $self->{'mapset_name'}.$self->LA_ASSET_SUFFIX_BIOSCOPE_LOG;

	return $self->{'bioscopelog_asset_name'};
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

	if(	! -r $self->{'mapset_folder'}) {
		my $msg = " is not a readable directory";
		$self->writelog(BODY => $self->{'mapset_folder'}.$msg);
		exit($self->EXIT_BAD_PERMS());
	}

	unless($self->{'mapset_folder'} =~ /(.+?)\.(.+?)\.(.+)\/$/) {
		print STDERR "BAD FOLDER FORMAT\n";
		exit($self->EXIT_NO_FOLDER());
	}

	$self->writelog(BODY => "Folder exists and is readable.");	

	return($status);
}

################################################################################
=pod

B<find_bam()> 
 Find primary BAM file (not unmapped)
 
Parameters:

Returns:
 void

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

	#my $finder	= QCMG::FileDir::Finder->new( verbose => 0 );
	#my @files	= $finder->find_file( $self->{'mapset_folder'}, ".bam" );

	#print Dumper @files;

	# get all unmapped BAM files
	#my @bam		= ();
	my $count		= 0;
	foreach (@files) {
		chomp;
		$self->writelog(BODY => "BAM: $_");
		next if(/unmapped/i);
		next if(/\.bai/i);
		#push @bam, $_ unless(/unmapped/i);
		$self->{'bam'} = $_;
		$self->writelog(BODY => "Using BAM: $_");
		$count++;
		last;
	}

	# get mapping date from path
	#($self->{'bam'});

	### ????? should there be only one?
	# too many BAMs found
	if($count > 1) {
		$self->writelog(BODY => "Too many BAMs found, unsure which one to use");
		exit($self->EXIT_NO_BAM());
	}
	# not enough BAMs found
	if(! $self->{'bam'}) {
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

	$self->writelog(BODY => "---Searching for unmapped BAM in $self->{'mapset_folder'}---");

	my @files	= `find $self->{'mapset_folder'} -name "*.bam"`;

	#my $finder	= QCMG::FileDir::Finder->new( verbose => 0 );
	#my @files	= $finder->find_file( $self->{'mapset_folder'}, ".bam" );

	#print Dumper @files;

	# get all unmapped BAM files
	#my @bam		= ();
	my $count		= 0;
	foreach (@files) {
		chomp;
		$self->writelog(BODY => "UNMAPPED BAM: $_");
		next unless(/unmapped/i);
		next if(/\.bai/i);
		$self->{'umbam'} = $_;
		$self->writelog(BODY => "Using unmapped BAM: $_");
		$count++;
		last;
	}

	### ????? should there be only one?
	# too many BAMs found
	if($count > 1) {
		$self->writelog(BODY => "Too many unmapped BAMs found, unsure which one to use");
		exit($self->EXIT_NO_BAM());
	}
	# not enough BAMs found
	if(! $self->{'umbam'}) {
		$self->writelog(BODY => "No valid unmapped BAMs found");
		exit($self->EXIT_NO_BAM());
	}

	return();
}

################################################################################
=pod

B<find_ma()> 
 Find primary .ma file
 
Parameters:

Returns:
 void

=cut

sub find_ma {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Searching for .ma in $self->{'mapset_folder'}---");

	my $cmd		= qq{find }.$self->{'mapset_folder'}.qq{ -name "*.ma"};
	my @files	= `$cmd`;

	#my $finder	= QCMG::FileDir::Finder->new( verbose => 0 );

	#/panfs/imb/seq_mapped/S0449_20100603_1_Frag.nopd.nobc/20110530/F3/s_mapping/S0449_20100603_1_Frag_Sample1_F3.csfasta.ma
	#my @files	= $finder->find_file( $self->{'mapset_folder'}, ".ma" );
	#print Dumper @files;

	# get all unmapped BAM files
	#my @bam		= ();
	#my $count		= 0;
	foreach (@files) {
		chomp;
		$self->writelog(BODY => ".ma: $_");
		next if(/unmapped/i);
		next unless(/\.ma$/);	# S0428_20110517_1_FragPEBC_bcSample1_F3_bcB16_07.csfasta.ma.idx
		push @{$self->{'ma'}}, $_;
		$self->writelog(BODY => "Using .ma: $_");
		#$count++;
	}

	# not enough .ma files found
	if(! $self->{'ma'}) {
		$self->writelog(BODY => "No valid .ma files found");
		exit($self->EXIT_NO_MA());
	}

	return();
}


################################################################################
=pod

B<find_bioscope_logs()> 
 Find Bioscope log files
 
Parameters:

Returns:
 void

=cut

sub find_bioscope_logs {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Searching for Bioscope logs for $self->{'mapset_folder'}---");

	# /panfs/imb/seq_raw/S0428_20110517_1_FragPEBC/_bioscope_ini/S0428_20110517_1_FragPEBC.nopd.bcB16_05/20110607/log/
	my $logdir	= $self->SEQ_RAW_PATH.$self->{'slide_name'}."/".$self->LA_ASSET_SUFFIX_BIOSCOPE_INI."/".$self->{'mapset_name'}."/";

	# if these are solid4 runs that were mapped natively in bioscope
	if($self->{'mapset_name'} !~ /^S[18]/) {
		$self->writelog(BODY => "Trying $logdir");
	
		#my $finder	= QCMG::FileDir::Finder->new( verbose => 0 );
		#my @files	= $finder->find_file( $logdir, "log" );
	
		# get most recently created date dir; log dir should be directly
		# underneath
		my $cmd 	= qq{ls -1t $logdir};
		my @files	= `$cmd`;
	
		my $datedir	= shift @files;
		chomp $datedir;
	
		# build whole dir
		$logdir		.= $datedir."/log/";
	}
	# define a different path if these files are remapped from lifescope
	# into bioscope 
	else {
		$self->writelog(BODY => "Expecting a different log directory due to xsq->csq conversion");
		# /panfs/imb/seq_raw/S88006_20120525_2_LMP/_bioscope_ini/lane_2/
		$self->{'mapset_name'}	=~ /(lane\_\d)/;
		my $lane		= $1;
		$logdir	= $self->SEQ_RAW_PATH.$self->{'slide_name'}."/".$self->LA_ASSET_SUFFIX_BIOSCOPE_INI."/".$lane."/log/";
	}

	$self->writelog(BODY => "Using $logdir");

	unless(-e $logdir) {
		$self->writelog(BODY => "No Bioscope logs found");
		exit($self->EXIT_MISSING_METADATA());
	}

	$self->writelog(BODY => "Bioscope logs in $logdir");
	$self->{'bioscope_log'} =  $logdir;

	return();
}

################################################################################
=pod

B<checksum_bam()> 
 Perform checksum on BAM 
 
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

	$self->writelog(BODY => "---Performing checksum on BAM---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	my ($crc, $fsize, $fname) = $self->checksum(FILE => $self->{'bam'});
	$self->writelog(BODY => "Checksum on ".$self->{'bam'}.": $crc, $fsize");

	my $bam	= $self->bam_filename();	# no path

	$self->{'ck_checksum'}->{$bam} = $crc;
	$self->{'ck_filesize'}->{$bam} = $fsize;

	$self->writelog(BODY => "Checksum complete");
	$self->writelog(BODY => "END: ".$self->timestamp());

	return;
}

################################################################################
=pod

B<checksum_umbam()> 
 Perform checksum on unmapped BAM 
 
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

	$self->writelog(BODY => "---Performing checksum on unmapped BAM---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	my ($crc, $fsize, $fname) = $self->checksum(FILE => $self->{'umbam'});
	$self->writelog(BODY => "Checksum on ".$self->{'umbam'}.": $crc, $fsize");

	my $umbam	= $self->umbam_filename();	# no path

	$self->{'ck_checksum'}->{$umbam} = $crc;
	$self->{'ck_filesize'}->{$umbam} = $fsize;

	$self->writelog(BODY => "Checksum complete");
	$self->writelog(BODY => "END: ".$self->timestamp());

	return;
}

################################################################################
=pod

B<checksum_ma()> 
 Perform checksum on .ma
 
Parameters:

Returns:
 void

=cut
sub checksum_ma {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Performing checksum on .ma---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	foreach my $file (@{$self->{'ma'}}) {
		my ($crc, $fsize, $fname) = $self->checksum(FILE => $file);
		$self->writelog(BODY => "Checksum on $file: $crc, $fsize");

		my ($v, $d, $ma) = File::Spec->splitpath($file);

		# get filename without path
		my $ma	= $self->filename(FILE => $ma);

		$self->{'ck_checksum'}->{$ma} = $crc;
		$self->{'ck_filesize'}->{$ma} = $fsize;
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

B<solid_stats_report()> 
 Generate SOLiD stats report for each mapset
 
Parameters:

Returns:
 void

=cut

sub solid_stats_report {
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

	$self->writelog(BODY => "---Generating solid_stats_report---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	# get path to mapset dir (/panfs/imb/seq_mapped/S0449_20100603_1_Frag.nopd.nobc/)
	my $dir		= $self->{'mapset_folder'};

        my $mapset      = $self->{'mapset_name'};

        # S0014_20110627_2_FragPEBC.nopd.bcB16_10
        # S0014_20110627_2_Frag.nopd.nobc
        $mapset         =~ /([\w\_]+)\.(.+?)\.(.+?)$/;
        my $barcode     = $3;

	#print STDERR "MAPSET: $mapset\nBARCODE: $barcode\n";

        # find seq_mapped mapset dir
        #my $finder      = QCMG::FileDir::Finder->new( verbose => 0 );
        #my @files       = $finder->find_directory($self->SEQ_MAPPED_PATH, $mapset);
	my $cmd		= qq{find }.$self->SEQ_MAPPED_PATH.qq{ -name "$mapset"};
	my @files	= `$cmd`;
        my $dir         = shift @files;

        # date
        # /panfs/imb/seq_mapped/S0014_20110627_2_FragPEBC.nopd.bcB16_10/20110726/pairing/S0014_20110627_2_FragPEBC.nopd.bcB16_10.bam
        #                                                               --------
        #my $finder      = QCMG::FileDir::Finder->new( verbose => 0 );
        my $bampat      = $mapset.".bam";
        my $cmd         = qq{find }.$self->SEQ_MAPPED_PATH.qq{$mapset/ -name "$bampat"};
	#print STDERR "$cmd\n";
        my @files       = `$cmd`;
	#print Dumper @files;
        my $bam         = shift @files;
        chomp $bam;
        $bam            =~ /\/panfs\/imb\/seq\_mapped\/$mapset\/(\d+)\//;
        my $date        = $1;
	#print STDERR "DATE: $bam -> $date\n";

        # seq_mapped + date
        $bam            =~ /(\/panfs\/imb\/seq\_mapped\/$mapset\/\d+\/)/;
        my $seq_mapped  = $1;

        # seq_raw
        $mapset         =~ /^(.+?)\..+?\..+?$/;
        my $slide       = $1;
        my $seq_raw     = $self->SEQ_RAW_PATH."$slide/bcSample1/results.F1B1/";

	# S0014_20110627_2_FragPEBC
	#                  --------
	$slide		=~ /\_(\w+)$/;
	my $run_type	= $1;

        my $f3mapstats;
        my $r3mapstats;
        my $f5mapstats;
	my $pairstats;
	my $bcstats;

        # log files
        # /panfs/imb/seq_raw/S0014_20110627_2_FragPEBC/_bioscope_ini/S0014_20110627_2_FragPEBC.nopd.bcB16_10/20110726/log/
        #my $log         = $self->SEQ_RAW_PATH."$slide/_bioscope_ini/$mapset/$date/log/";
	my $log		= $self->{'bioscope_log'};
	# /panfs/imb/seq_raw/S0014_20110627_1_FragPEBC/_bioscope_ini/S0014_20110627_1_FragPEBC.nopd.bcB16_08/S0014_20110627_1_FragPEBC/log/

	# executable
	my $ssrexe	= $self->BIN_SEQTOOLS.qq{solid_stats_report.pl};
        # write to S0014_20110627_2_FragPEBC.nopd.bcB16_10.solidstats.xml
        my $ssr         = $self->{'mapset_folder'}."/".$mapset.".solidstats.xml";
        my $ssrcmd      = qq{$ssrexe -v -d }.$self->SEQ_RAW_PATH.qq{$slide/ -d $seq_mapped -l $log -f };
	$ssrcmd		.= $self->SEQ_RAW_PATH;
	$ssrcmd		.= qq{$slide/$slide.xml -x $ssr };

        # mapping-stats.txt
        my $cmd         = qq{find }.$self->SEQ_MAPPED_PATH.qq{/$mapset/ -name "mapping-stats.txt"};
        #print STDERR $cmd, "\n";
        my @files       = `$cmd`;
	#print Dumper @files;
        foreach (@files) {
                chomp;
                if(/F3/) {
                        $f3mapstats = $_;
			$ssrcmd	.= qq{ -a $f3mapstats };
                }
                if(/F5/) {
                        $f5mapstats = $_;
			$ssrcmd	.= qq{ -a $f5mapstats };
                }
		if(/R3/) {
                        $r3mapstats = $_;
			$ssrcmd	.= qq{ -a $r3mapstats };
		}
        }

	# pairing-stats.txt
	if($run_type =~ /PE/ || $run_type =~ /LMP/) {
        	my $cmd         = qq{find }.$self->SEQ_MAPPED_PATH.qq{/$mapset/ -name "pairing*tat*"};
	        #print STDERR $cmd, "\n";
	        my @files       = `$cmd`;
		#print Dumper @files;
	        $pairstats   = shift @files;
	        chomp $pairstats;
		$ssrcmd	.= qq{ -p $pairstats };
	}

        # BarcodeStat....txt
	if($barcode ne 'nobc') {
       	 	my $cmd         = qq{find $seq_raw -name "BarcodeStat*"};
        	#print STDERR $cmd, "\n";
        	my @files       = `$cmd`;
		#print Dumper @files;
        	my $bcstats     = shift @files;
        	chomp $bcstats;
		$ssrcmd	.= qq{ -b $bcstats };
	}

	$self->writelog(BODY => $ssrcmd);

	my $rv		= system($ssrcmd);

	$self->writelog(BODY => "RV: $rv");

	# this step fails frequently so send an email about it if it does; not
	# worth stopping the whole ingest for a failure here, though
	if($rv > 0) {
		my $subj	= "SSR for $bampat creation failed";
		my $body	= "solid_stats_report.pl returned a non-zero exit status.\n\nRecreate the report with the following command:\n\n$ssrcmd\n";
		#$self->send_email(SUBJECT => $subj, BODY => $body);

		$self->{'ssr_failed'}	= 1;

		$self->{'ssr_failed'}	= 1;
	}

	$self->{'solid_stats_report'}	= $ssr;

	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
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

	# CAPTURE LIVEARC STDER TO VARIABLE
	# First, save away STDERR
	my $stderrcap;
	open SAVEERR, ">&STDERR";
	close STDERR;
	open STDERR, ">", \$stderrcap or warn "Cannot save STDERR to scalar in ingest_bam()\n";
	
	# check if slide name mapped namespace is already in LiveArc
	#                 /QCMG/S0449_20100603_1_Frag/S0449_20100603_1_Frag_mapped
	my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'}, $self->{'slide_name'}.$self->LA_NAMESPACE_SUFFIX_MAP;

	my $bamasset	= $self->bam_asset_name();
	my $bamassetid	= "";

	$self->writelog(BODY => "Checking if namespace $ns exists...");
	my $r		= $mf->namespace_exists(NAMESPACE => $ns);
	# 0 if exists, 1 if does not exist
	$self->writelog(BODY => "RV: $r");

	# if no mapped namespace in the slide namespace, create it
	if($r == 1) {
		#$self->writelog(BODY => "Creating $ns");

		# check for top level namespace; if that doesn't exist, create
		# it first
		my $topns	= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};
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

		$self->writelog(BODY => "RV: $r");
		if($r == 1) {
			$self->writelog(BODY => "Cannot create $ns");
			exit($self->EXIT_LIVEARC_ERROR());
		}
	}
	elsif($r == 0) {
		$self->writelog(BODY => "Namespace $ns exists");

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
	}

	$self->writelog(BODY => "Preparing to write metadata, if any...");

	# set tags for description of asset checksums in LiveArc
	my $meta;
	if($self->{'ck_checksum'}->{$self->bam_filename()}) {
		$self->writelog(BODY => "Generating LiveArc metadata...");

		$meta = qq{:meta < :checksums < :source orig };
		$meta .= qq{ :checksum < :chksum };
		$meta .= $self->{'ck_checksum'}->{$self->bam_filename()}.' ';
		$meta .= qq{ :filesize };
		$meta .= $self->{'ck_filesize'}->{$self->bam_filename()}.' ';
		$meta .= qq{ :filename }.$self->bam_filename().qq{ > };
		$meta .= qq{ > >};

		$self->writelog(BODY => "Metadata: $meta");
	}
	else {
		$self->writelog(BODY => "Skipping LiveArc metadata generation...");
	}
	$self->writelog(BODY => "Done checking metadata");

	# initialize error status and number of attempted imports
	my $status	= 2;
	my $attempts	= 0;
	my $maxattempts	= 4;

	$BAM_IMPORT_ATTEMPTED = 'yes';
	#print STDERR "BAM_IMPORT_ATTEMPTED: $BAM_IMPORT_ATTEMPTED\n";

	### PERFORM INGESTION
	my $date = $self->timestamp();
	$self->writelog(BODY => "BAM ingest started $date");

	# broadcast and record status
	#my $subj	= "Ingestion of ".$ns.":".$bamasset;
	#my $body	= "Ingestion initiated\n\nCheck log file in ".$self->{'log_file'};
	##$self->send_email(SUBJECT => $subj, BODY => $body);

	$self->writelog(BODY => "Importing $bamasset");
	# status starts at 2, will be set to 0 on success, 1 on fail; try 3
	# times, then give up
=cut
b03a06:/home/uqlfink>  /usr/bin/java -Dmf.result=shell -Dmf.cfg=/home/uqlfink/.mediaflux/mf_credentials.cfg -jar /panfs/imb/qcmg/software/aterm.jar --app exec  asset.create :namespace QCMG/S0411_20110913_2_LMP/S0411_20110913_2_LMP_mapped :name S0411_20110913_2_LMP.nopd.nobc.bam :in file:/panfs/imb/seq_mapped/S0411_20110913_2_LMP.nopd.nobc/20110928/pairing/S0411_20110913_2_LMP.nopd.nobc.bam
    :id "45498"
=cut
	until($status == 0 || $attempts > $maxattempts) {
		my $id = $mf->laimport_file(NAMESPACE => $ns, ASSET => $bamasset, FILE => $self->{'bam'});
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

	$self->writelog(BODY => "Import complete");

	# Now close and restore STDERR to original condition.
	close STDERR;
	open STDERR, ">&SAVEERR";
	$self->writelog(BODY => "$stderrcap");

	if($status != 0) {
		exit($self->EXIT_LIVEARC_ERROR());
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
	my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'}, $self->{'slide_name'}.$self->LA_NAMESPACE_SUFFIX_MAP;

	my $asset	= $self->ssr_asset_name();
	my $assetid	= "";

	$self->writelog(BODY => "Checking if namespace $ns exists...");
	my $r		= $mf->namespace_exists(NAMESPACE => $ns);
	$self->writelog(BODY => "RV: $r");

	# if no mapped namespace in the slide namespace, create it
	if($r == 1) {
		$self->writelog(BODY => "Creating $ns");

		# check for top level namespace; if that doesn't exist, create
		# it first
		my $topns	= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};
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

	$self->writelog(BODY => "Importing $asset to $ns");
	my $la		= QCMG::Automation::LiveArc->new(VERBOSE => 1);

	# status starts at 2, will be set to 0 on success, 1 on fail; try 3
	# times, then give up
	#until($status == 0 || $attempts > $maxattempts) {
		my $id = $mf->laimport_file(NAMESPACE => $ns, ASSET => $asset, FILE => $self->{'solid_stats_report'});
		$self->writelog(BODY => "Import result: $id");
		$status = 0 if($id > 0);

	#	$attempts++;

	#	sleep(60) if($status != 0);
	#}

	$self->writelog(BODY => "Import complete");

	# Now close and restore STDERR to original condition.
	close STDERR;
	open STDERR, ">&SAVEERR";
	$self->writelog(BODY => "$stderrcap");

	#if($status != 0) {
	#	exit($self->EXIT_LIVEARC_ERROR());
	#}

	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}


################################################################################
=pod

B<ingest_umbam()> 
 Ingest unmapped BAM  

Parameters:

Returns:
 void

=cut

sub ingest_umbam {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Preparing for unmapped BAM ingestion... ---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	my $mf	= QCMG::Automation::LiveArc->new(VERBOSE => 1);

	# CAPTURE LIVEARC STDER TO VARIABLE
	# First, save away STDERR
	my $stderrcap;
	open SAVEERR, ">&STDERR";
	close STDERR;
	open STDERR, ">", \$stderrcap or warn "Cannot save STDERR to scalar in ingest_umbam()\n";
	
	# check if slide name mapped namespace is already in LiveArc
	#                 /QCMG/S0449_20100603_1_Frag/S0449_20100603_1_Frag_mapped
	my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'}, $self->{'slide_name'}.$self->LA_NAMESPACE_SUFFIX_MAP;

	my $bamasset	= $self->umbam_asset_name();
	my $bamassetid	= "";

=cut
	# SLIDE_mapped namespace: QCMG/S0417_20110905_1_LMP/S0417_20110905_1_LMP_mapped
	$self->writelog(BODY => "Checking if namespace $ns exists...");
	my $r		= $mf->namespace_exists(NAMESPACE => $ns);
	$self->writelog(BODY => "RV: $r");

	# if no mapped namespace in the slide namespace, create it; check for
	# top level namespace first
	if($r == 1) {
		$self->writelog(BODY => "Creating $ns");

		# check for top level namespace; if that doesn't exist, create
		# it first
		my $topns	= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};
		$self->writelog(BODY => "Checking if slide namespace $topns exists...");
		# 1 = if does not exist; 0 = if does exist
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
=cut

		$self->writelog(BODY => "Checking if asset $bamasset exists...");
		my $r			= $mf->asset_exists(NAMESPACE => $ns, ASSET => $bamasset);
		$self->writelog(BODY => "Result: $r");
		if($r) {
			if($self->{'update_ingest'} eq 'Y') {
				# if updating ingest and unmapped BAM asset exists, don't do it
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
=cut
		$self->writelog(BODY => "Done checking mapped namespace and unmapped BAM asset");
	}
=cut

	$self->writelog(BODY => "Done checking unmapped BAM asset");
	$self->writelog(BODY => "Preparing to write metadata, if any...");

	# set tags for description of asset checksums in LiveArc
	my $meta;
	if($self->{'ck_checksum'}->{$self->umbam_filename()}) {
		$self->writelog(BODY => "Generating LiveArc metadata...");

		$meta = qq{:meta < :checksums < :source orig };
		$meta .= qq{ :checksum < :chksum };
		$meta .= $self->{'ck_checksum'}->{$self->umbam_filename()}.' ';
		$meta .= qq{ :filesize };
		$meta .= $self->{'ck_filesize'}->{$self->umbam_filename()}.' ';
		$meta .= qq{ :filename }.$self->umbam_filename().qq{ > };
		$meta .= qq{ > >};

		$self->writelog(BODY => "Metadata: $meta");
	}
	else {
		$self->writelog(BODY => "Skipping LiveArc metadata generation...");
	}
	$self->writelog(BODY => "Done checking metadata");

	# initialize error status and number of attempted imports
	my $status	= 2;
	my $attempts	= 0;
	my $maxattempts	= 4;

	### PERFORM INGESTION
	my $date = $self->timestamp();
	$self->writelog(BODY => "Unmapped BAM ingest started $date");

	# broadcast and record status
	#my $subj	= "Ingestion of ".$ns.":".$bamasset;
	#my $body	= "Ingestion initiated\n\nCheck log file in ".$self->{'log_file'};
	##$self->send_email(SUBJECT => $subj, BODY => $body);

	$self->writelog(BODY => "Importing $bamasset");
	# status starts at 2, will be set to 0 on success, 1 on fail; try 3
	# times, then give up
	until($status == 0 || $attempts > $maxattempts) {
		my $id = $mf->laimport_file(NAMESPACE => $ns, ASSET => $bamasset, FILE => $self->{'umbam'});
		$self->writelog(BODY => "Import result: $id");
		#$status = 0 if($id > 0);

		my $r			= $mf->asset_exists(NAMESPACE => $ns, ASSET => $bamasset);
		# if asset exists, returns ID
		$self->writelog(BODY => "Result: $r");

		$status = 0 unless($r =~ /error/i);

		$attempts++;

		sleep(60) if($status != 0);
	}

	$self->writelog(BODY => "Updating metadata...");
	# set metadata if import was successful
	if($status == 0 && $meta) {
		my $r = $mf->update_asset(NAMESPACE => $ns, ASSET => $bamasset, DATA => $meta);
		$self->writelog(BODY => "Update metadata result: $r");
	}

	$self->writelog(BODY => "Import complete");

	# Now close and restore STDERR to original condition.
	close STDERR;
	open STDERR, ">&SAVEERR";
	$self->writelog(BODY => "$stderrcap");

	if($status != 0) {
		exit($self->EXIT_LIVEARC_ERROR());
	}

	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}

################################################################################
=pod

B<ingest_ma()> 
 Ingest .ma files as a directory

Parameters:

Returns:
 void

=cut

sub ingest_ma {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Preparing for .ma ingestion... ---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	my $mf	= QCMG::Automation::LiveArc->new(VERBOSE => 1);

	# CAPTURE LIVEARC STDER TO VARIABLE
	# First, save away STDERR
	my $stderrcap;
	open SAVEERR, ">&STDERR";
	close STDERR;
	open STDERR, ">", \$stderrcap or warn "Cannot save STDERR to scalar in ingest_ma()\n";
	
	# check if slide name mapped namespace is already in LiveArc
	#                 /QCMG/S0449_20100603_1_Frag/S0449_20100603_1_Frag_mapped
	my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'}, $self->{'slide_name'}.$self->LA_NAMESPACE_SUFFIX_MAP;

	my $maasset	= $self->ma_asset_name();
	my $maassetid	= "";

	$self->writelog(BODY => "Checking if namespace $ns exists...");
	my $r		= $mf->namespace_exists(NAMESPACE => $ns);
	$self->writelog(BODY => "RV: $r");

	# if no mapped namespace in the slide namespace, create it
	if($r == 1) {
		$self->writelog(BODY => "Creating $ns");

		# check for top level namespace; if that doesn't exist, create
		# it first
		my $topns	= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};
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

		$self->writelog(BODY => "Checking if asset $maasset exists...");
		my $r			= $mf->asset_exists(NAMESPACE => $ns, ASSET => $maasset);
		$self->writelog(BODY => "Result: $r");
		if($r) {
			if($self->{'update_ingest'} eq 'Y') {
				# if updating ingest and _ma asset exists, don't do it
				# again
				$self->writelog(BODY => "Asset $maasset already exists, not ingesting it on update");
				$self->writelog(BODY => "END: ".$self->timestamp());
				return();
			}
			else {
				$self->writelog(BODY => "Asset $maasset exists");
				exit($self->EXIT_ASSET_EXISTS());
			}
		}
		$self->writelog(BODY => "Done checking mapped namespace and .ma assets");
	}

	$self->writelog(BODY => "Preparing to write metadata, if any...");

	# set tags for description of asset checksums in LiveArc
	my $meta;
	foreach my $file (@{$self->{'ma'}}) {
		if($self->{'ck_checksum'}->{$file}) {
			$self->writelog(BODY => "Generating LiveArc metadata...");
	
			$meta = qq{:meta < :checksums < :source orig };
			$meta .= qq{ :checksum < :chksum };
			$meta .= $self->{'ck_checksum'}->{$file}.' ';
			$meta .= qq{ :filesize };
			$meta .= $self->{'ck_filesize'}->{$file}.' ';
			$meta .= qq{ :filename }.$file.qq{ > };
			$meta .= qq{ > >};
	
			$self->writelog(BODY => "Metadata: $meta");
		}
	}
	if(! $meta) {
		$self->writelog(BODY => "Skipping LiveArc metadata generation...");
	}
	$self->writelog(BODY => "Done checking metadata");

	# initialize error status and number of attempted imports
	my $status	= 2;
	my $attempts	= 0;
	my $maxattempts	= 4;

	### PERFORM INGESTION
	my $date = $self->timestamp();
	$self->writelog(BODY => ".ma ingest started $date");

	# broadcast and record status
	#my $subj	= "Ingestion of ".$ns.":".$maasset;
	#my $body	= "Ingestion initiated\n\nCheck log file in ".$self->{'log_file'};
	##$self->send_email(SUBJECT => $subj, BODY => $body);

	# make directory for .ma files and copy them into it
	my $ma_dir	= $self->{'mapset_folder'}.qq{/_ma/};
	$self->{'ma_dir'}	= $ma_dir;

	# first, check if dir already exists from previous ingest attempt; fail
	# if the dir exists, but doesn't contain all the files
	#_ma directory exists, but does not contain an expected .ma file (/panfs/imb/seq_mapped/S0436_20110525_1_FragPEBC.nopd.bcB16_10//_ma//panfs/imb/seq_mapped/S0436_20110525_1_FragPEBC.nopd.bcB16_10/_ma/S0436_20110525_1_FragPEBC_bcSample1_F3_bcB16_10.csfasta.ma)
	if(-e $ma_dir) {
		foreach my $file (@{$self->{'ma'}}) {
			unless(-e $file) {
				$self->writelog(BODY => "_ma directory exists, but does not contain an expected .ma file ($file)");
				exit($self->CANNOT_MOVE_BAM());
			}
		}
	}
	else {
		my $cmd	= qq{mkdir $ma_dir};
		my $rv	= system($cmd);
		unless($rv == 0) {
			$self->writelog(BODY => "Cannot create _ma dir $ma_dir for .ma files");
			exit($self->CANNOT_MOVE_BAM());
		}
		foreach my $file (@{$self->{'ma'}}) {
			my $cmd	= qq{mv $file $ma_dir};
			my $rv	= system($cmd);
			$self->writelog(BODY => "$cmd\nRV: $rv");
		}
	}

	$self->writelog(BODY => "Importing $maasset");
	# status starts at 2, will be set to 0 on success, 1 on fail; try 3
	# times, then give up
	until($status == 0 || $attempts > $maxattempts) {
		my $id = $mf->laimport(NAMESPACE => $ns, ASSET => $maasset, DIR => $ma_dir);
		$self->writelog(BODY => "Import result: $id");
		$status = 0 if($id > 0);

		$attempts++;

		sleep(60) if($status != 0);
	}

	$self->writelog(BODY => "Updating metadata...");
	# set metadata if import was successful
	if($status == 0 && $meta) {
		my $r = $mf->update_asset(NAMESPACE => $ns, ASSET => $maasset, DATA => $meta);
		$self->writelog(BODY => "Update metadata result: $r");
	}

	$self->writelog(BODY => "Import complete");

	# Now close and restore STDERR to original condition.
	close STDERR;
	open STDERR, ">&SAVEERR";
	$self->writelog(BODY => "$stderrcap");

	if($status != 0) {
		exit($self->EXIT_LIVEARC_ERROR());
	}

	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}

################################################################################
=pod

B<ingest_mapped()> 
 Ingest everything else in Bioscope directories except the BAM

Parameters:

Returns:
 void

=cut

sub ingest_mapped {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Preparing for seq_mapped ingestion... ---");
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
	my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'}, $self->{'slide_name'}.$self->LA_NAMESPACE_SUFFIX_MAP;

	my $mapasset	= $self->mapped_asset_name();
	my $mapassetid	= "";

	$self->writelog(BODY => "Checking if namespace $ns exists...");
	my $r		= $mf->namespace_exists(NAMESPACE => $ns);
	$self->writelog(BODY => "RV: $r");

	# if no mapped namespace in the slide namespace, create it
	if($r == 1) {
		$self->writelog(BODY => "Creating $ns");

		# check for top level namespace; if that doesn't exist, create
		# it first
		my $topns	= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};
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

		$self->writelog(BODY => "Checking if asset $mapasset exists...");
		# S0428_20110517_1_FragPEBC.nopd.bcB16_05_seq_mapped
		my $r			= $mf->asset_exists(NAMESPACE => $ns, ASSET => $mapasset);
		$self->writelog(BODY => "Result: $r");
		unless($r) {
			# expect asset to exist if running after run_bioscope.pl
			# - which will create this asset and add metadata to it
			#$self->writelog(BODY => "Asset $mapasset does not exist, but is required");
 			#exit($self->EXIT_ASSET_NOT_EXISTS());
			$self->writelog(BODY => "Asset $mapasset does not exist (run.pbs will not have set metadata)");
		}
		$self->writelog(BODY => "Done checking mapped namespace and mapped asset");
	}

	# check to see if seq_mapped asset contains a tar archive already
	#-id "1" -stime "93908"
        #    :type -ext "tar" "application/x-tar"
        #    :size -units "bytes" "7946240"
        #    :csum -base "16" "91A513A"
        #    :store "qcmg_data"
        #    :url "file:/QCMG/mediaflux/mediaflux/volatile/stores/qcmg_data/data/0/0/0/0/49/4966"
	my $seqmap_meta	= $mf->get_asset(NAMESPACE => $ns, ASSET => $mapasset);
	if($seqmap_meta->{'CONTENT'} =~ /\:type\s+\-ext\s+\"tar\"/) {
		# skip rest of subroutine
		$self->writelog(BODY => "$mapasset already contains tar archive: ");
		$self->writelog(BODY => $seqmap_meta->{'CONTENT'});
		$self->writelog(BODY => "Skipping ingestion of $mapasset");
		$MAP_IMPORT_ATTEMPTED = 'yes';
		$MAP_IMPORT_SUCCEEDED = 'yes';
		return();
	}
=cut

	# move BAM out of mapset directory for ingestion to avoid having it
	# twice
	my $tmpfilename	= $self->{'bam'}.$self->timestamp(FORMAT => 'YYMMDDhhmmss');
	my $mvbamcmd	= qq{mv }.$self->{'bam'}." /panfs/imb/automation_log/";
	my $getbamcmd	= qq{mv /panfs/imb/automation_log/}.$self->{'bam_filename'}." ".$self->{'bam'};
	$self->writelog(BODY => "Moving BAM during seq_mapped ingest:\n$mvbamcmd");
	my $rv = system($mvbamcmd);
	$self->writelog(BODY => "RV: $rv");
	if($rv > 1) {
		exit($self->CANNOT_MOVE_BAM());
	}

	# move unmapped BAM out of mapset directory for ingestion to avoid having it
	# twice
	my $tmpfilename	= $self->{'umbam'}.$self->timestamp(FORMAT => 'YYMMDDhhmmss');
	my $mvbamcmd	= qq{mv }.$self->{'umbam'}." /panfs/imb/automation_log/";
	my $getumbamcmd	= qq{mv /panfs/imb/automation_log/}.$self->{'umbam_filename'}." ".$self->{'umbam'};
	$self->writelog(BODY => "Moving unmapped BAM during seq_mapped ingest:\n$mvbamcmd");
	my $rv = system($mvbamcmd);
	$self->writelog(BODY => "RV: $rv");
	if($rv > 1) {
		exit($self->CANNOT_MOVE_BAM());
	}

	# make directory for .ma files and copy them into it
	my $ma_dir	= $self->{'mapset_folder'}.qq{/_ma/};
	$self->{'ma_dir'}	= $ma_dir;

	# first, check if dir already exists from previous ingest attempt; fail
	# if the dir exists, but doesn't contain all the files
	#_ma directory exists, but does not contain an expected .ma file (/panfs/imb/seq_mapped/S0436_20110525_1_FragPEBC.nopd.bcB16_10//_ma//panfs/imb/seq_mapped/S0436_20110525_1_FragPEBC.nopd.bcB16_10/_ma/S0436_20110525_1_FragPEBC_bcSample1_F3_bcB16_10.csfasta.ma)
	if(-e $ma_dir) {
		foreach my $file (@{$self->{'ma'}}) {
			unless(-e $file) {
				$self->writelog(BODY => "_ma directory exists, but does not contain an expected .ma file ($file)");
				exit($self->CANNOT_MOVE_BAM());
			}
		}
	}
	else {
		my $cmd	= qq{mkdir $ma_dir};
		my $rv	= system($cmd);
		unless($rv == 0) {
			$self->writelog(BODY => "Cannot create _ma dir $ma_dir for .ma files");
			exit($self->CANNOT_MOVE_BAM());
		}
		foreach my $file (@{$self->{'ma'}}) {
			my $cmd	= qq{mv $file $ma_dir};
			my $rv	= system($cmd);
			$self->writelog(BODY => "$cmd\nRV: $rv");
		}
	}
=cut

	# initialize error status and number of attempted imports
	my $status	= 2;
	my $attempts	= 0;
	my $maxattempts	= 4;

	$MAP_IMPORT_ATTEMPTED = 'yes';

	# tar mapset dir to make ingest into existing asset possible
	my $tarfile	= $self->{'mapset_folder'}.$self->{'mapset_name'}.".tar";
	# chdir to mapset folder before tarring so archive starts with top level
	# directory = mapset folder (not /)
	my $cwd	=  getcwd;
	chdir $self->{'mapset_folder'};
	chdir "..";
	#my $cmd		= "tar -cf $tarfile --exclude \"_ma\" ".$self->{'mapset_folder'};
	my $cmd		= qq{tar -cf $tarfile --exclude=*.bam --exclude=*.bai --exclude=*_ma }.$self->{'mapset_name'};
	$self->writelog(BODY => "Tarring _seq_mapped dir: $cmd");
	my $rv		= system($cmd);
	$self->writelog(BODY => "RV: $rv");
	chdir $cwd;

	### PERFORM INGESTION
	my $date = $self->timestamp();
	$self->writelog(BODY => "seq_mapped ingest started $date");

	$self->writelog(BODY => "Importing $mapasset");
	# status starts at 2, will be set to 0 on success, 1 on fail; try 4
	# times, then give up
	until($status == 0 || $attempts > $maxattempts) {
		my $lacmd	= $mf->get_asset(ASSET => $mapasset, NAMESPACE => $ns, CMD => 1);
		$self->writelog(BODY => "asset.get cmd: $lacmd");

		my $assetmeta	= $mf->get_asset(ASSET => $mapasset, NAMESPACE => $ns);
		my $assetid	= $assetmeta->{'ID'};

		my $lacmd = $mf->update_asset_file(ID => $assetid, FILE => $tarfile, CMD => 1);
		$self->writelog(BODY => "Import cmd: $lacmd");

		# returns 0 if success, 1 if fail
		$status = $mf->update_asset_file(ID => $assetid, FILE => $tarfile);
		$self->writelog(BODY => "Import result: $status (0 = success, 1 = failure)");

		$attempts++;

		sleep(60) if($status != 0);
	}


	$self->writelog(BODY => "Import complete");

	# clean up tar file
	$self->writelog(BODY => "Removing tar file: $tarfile");
	unlink($tarfile);

	# import solid_stats_report into LiveArc in the same namespace as the
	# data
	#
	# asset = S0449_20100603_1_Frag.nopd.nobc_solidstats
	# file  = /panfs/imb/seq_mapped/S0449_20100603_1_Frag.nopd.nobc/S0449_20100603_1_Frag.nopd.nobc_solidstats.xml


	#my $asset	= $self->{'mapset_name'}.$self->LA_ASSET_SUFFIX_SOLIDSTATS;
	my $asset	= $self->{'mapset_name'}.".solidstats.xml";
	$self->writelog(BODY => "Importing $asset to $ns");
	my $la	= QCMG::Automation::LiveArc->new(VERBOSE => 1);
	my $assetid = $la->asset_exists(NAMESPACE => $ns, ASSET => $asset);
	my $status;
	if(! $assetid) {
		$self->writelog(BODY => "Creating mapped data solid_stats_report asset");
		$assetid = $la->create_asset(NAMESPACE => $ns, ASSET => $asset);
	}
	$self->writelog(BODY => "Importing file into mapped data solid_stats_report asset");
	$status = $la->update_asset_file(ID => $assetid, FILE => $self->{'solid_stats_report'});
	$self->writelog(BODY => "RV: $status");
	# should check here to make sure ingest worked...
=cut

	# move BAMs and .ma file back after ingest
	$self->writelog(BODY => "Moving BAM back to mapset dir:\n$getbamcmd");
	$rv = system($getbamcmd);
	$self->writelog(BODY => "RV: $rv");
	if($rv > 0) {
		$self->writelog(BODY => "Moving BAM back to mapset dir:\n$getbamcmd");
		$rv = system($getbamcmd);
		$self->writelog(BODY => "RV: $rv");
	}

	$self->writelog(BODY => "Moving unmapped BAM back to mapset dir:\n$getumbamcmd");
	$rv = system($getumbamcmd);
	$self->writelog(BODY => "RV: $rv");
	if($rv > 0) {
		# try again;
		$self->writelog(BODY => "Moving unmapped BAM back to mapset dir:\n$getumbamcmd");
		$rv = system($getumbamcmd);
		$self->writelog(BODY => "RV: $rv");
	}

	# mv /panfs/imb/seq_mapped/S0413_20110719_1_LMP.nopd.nobc//_ma//S0413_20110719_1_LMP_Sample1_F3.csfasta.ma
	#	/panfs/imb/seq_mapped/S0413_20110719_1_LMP.nopd.nobc/20110808/F3/s_mapping/S0413_20110719_1_LMP_Sample1_F3.csfasta.ma
	$self->writelog(BODY => "Moving .ma files back to mapset dir");
	foreach my $file (@{$self->{'ma'}}) {
		my $f = $self->filename(FILE => $file);

		my $cmd = qq{mv }.$self->{'ma_dir'}.qq{/$f $file};
		my $rv = system($cmd);
		$self->writelog(BODY => "$cmd\nRV: $rv");
	}
	# remove _ma dir
	$self->writelog(BODY => "Removing _ma dir");
	my $cmd = qq{ rmdir $self->{'ma_dir'} };
	my $rv = system($cmd);
=cut
	$self->writelog(BODY => "$cmd\nRV: $rv");


	# Now close and restore STDERR to original condition.
	close STDERR;
	open STDERR, ">&SAVEERR";
	$self->writelog(BODY => "$stderrcap");

	if($status != 0) {
		exit($self->EXIT_LIVEARC_ERROR());
	}
	else {
		$MAP_IMPORT_SUCCEEDED = 'yes';
		#print STDERR "MAP_IMPORT_SUCCEEDED: $MAP_IMPORT_SUCCEEDED\n";
	}

	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}

################################################################################
=pod

B<ingest_bioscope_logs()> 
 Ingest Bioscope log directory

Parameters:

Returns:
 void

=cut

sub ingest_bioscope_logs {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $status = 0;

	$self->writelog(BODY => "---Preparing for Bioscope log ingestion... ---");
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
	my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'}, $self->{'slide_name'}.$self->LA_NAMESPACE_SUFFIX_MAP;

	my $asset	= $self->bioscopelog_asset_name();
	my $assetid	= "";

	$self->writelog(BODY => "Checking if namespace $ns exists...");
	my $r		= $mf->namespace_exists(NAMESPACE => $ns);
	$self->writelog(BODY => "RV: $r");

	# if no mapped namespace in the slide namespace, create it
	if($r == 1) {
		$self->writelog(BODY => "Creating $ns");

		# check for top level namespace; if that doesn't exist, create
		# it first
		my $topns	= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};
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
		$self->writelog(BODY => "Done checking mapped namespace and Bioscope log asset");
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
					DIR		=> $self->{'bioscope_log'}
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
		exit($self->EXIT_LIVEARC_ERROR());
	}

	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}

################################################################################
=pod

B<clean()> 
 Delete seq_raw and seq_mapped directories after a successful run
 
Parameters:

Requires:
 $self->{'ssr_failed'} (1 = failed, undef = succeeded)
 $self->{'slide_name'}
 $self->{'mapset_name'}

Returns:
 void

=cut

sub clean {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Cleaning up directories---");

	my $seq_raw	= $self->SEQ_RAW_PATH."/$self->{'slide_name'}";
	my $seq_mapped	= $self->SEQ_MAPPED_PATH."/$self->{'mapset_name'}";

	# change to seq_raw root directory and, if that is successful, delete
	# the directory for the slide; this may not always be successful because
	# another mapset process may have already deleted it
	my $cwd		= getcwd;
	$self->writelog(BODY => "CWD: $cwd");
	my $rv		= chdir $self->SEQ_RAW_PATH;
	$self->writelog(BODY => "chdir ".$self->SEQ_RAW_PATH." : $rv");
	if($rv == 1) {
		#$rv	= rmtree($self->{'slide_name'});
		$self->writelog(BODY => "rmtree $self->{'slide_name'} : $rv");
	}
	# change to seq_mapped directory and, if that is successful AND a solid
	# stats report was successfully made, delete the directory for the
	# mapset
	$rv		= chdir $self->SEQ_MAPPED_PATH;
	$self->writelog(BODY => "chdir ".$self->SEQ_MAPPED_PATH." : $rv");
	$self->writelog(BODY => "ssr_failed status: $self->{'ssr_failed'}");
	if($rv == 1 && $self->{'ssr_failed'} != 1) {
		#$rv	= rmtree($self->{'mapset_name'});
		$self->writelog(BODY => "rmtree $self->{'mapset_name'} : $rv");
	}
	# change back to the working directory we started in
	chdir $cwd;

	$self->writelog(BODY => "Cleaning completed");

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

	#print STDERR "FINAL: $BAM_IMPORT_ATTEMPTED, $BAM_IMPORT_SUCCEEDED, $MAP_IMPORT_ATTEMPTED, $MAP_IMPORT_SUCCEEDED\n";
	#print STDERR "EXIT: ".$self->{'EXIT'}."\n";

	unless($self->{'EXIT'} > 0) {
		# import was attempted and import succeeded
		if(	$BAM_IMPORT_ATTEMPTED eq 'yes' && $BAM_IMPORT_SUCCEEDED eq 'yes' && 
			$MAP_IMPORT_ATTEMPTED eq 'yes' && $MAP_IMPORT_SUCCEEDED eq 'yes') {

			$self->writelog(BODY => "EXECLOG: errorMessage none");

			$self->{'EXIT'} = 0;

			# send success email if no errors
			my $subj	= $self->{'slide_name'}." MAPPED INGESTION -- SUCCEEDED";
			my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} and BAM complete\n";
			$body		.= "See log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			#$self->send_email(SUBJECT => $subj, BODY => $body);
		}
		# import was not attempted; script ended for some other
		# reason
		elsif($BAM_IMPORT_ATTEMPTED eq 'no') {
			$self->writelog(BODY => "EXECLOG: errorMessage BAM import not attempted");
	
			$self->{'EXIT'} = 17;
	
			# send success email if no errors
			my $subj	= $self->{'slide_name'}." MAPPED INGESTION -- NOT ATTEMPTED";
			my $body	= "MAPPED INGESTION of $self->{'bam'} not attempted";
			$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			#$self->send_email(SUBJECT => $subj, BODY => $body);
		}
		elsif($MAP_IMPORT_ATTEMPTED eq 'no') {
			$self->writelog(BODY => "EXECLOG: errorMessage seq_mapped import not attempted");
	
			$self->{'EXIT'} = 17;
	
			# send success email if no errors
			my $subj	= $self->{'slide_name'}." MAPPED INGESTION -- NOT ATTEMPTED";
			my $body	= "MAPPED INGESTION of $self->{'mapset_folder'} not attempted";
			$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			#$self->send_email(SUBJECT => $subj, BODY => $body);
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
		#my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'}, $self->{'slide_name'}.$self->LA_NAMESPACE_SUFFIX_MAP;
		my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'}, $self->{'slide_name'}."_mapped";

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
