package QCMG::Ingest::Torrent;

##############################################################################
#
#  Module:   QCMG::Ingest::Torrent.pm
#  Creator:  Lynn Fink
#  Created:  2011-02-28
#
#  This class contains methods for automating the ingest into Mediaflux/LiveArc
#  of Ion Torrent sequencing  data
#
#  $Id: Torrent.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
##############################################################################


=pod

=head1 NAME

QCMG::Ingest::Torrent -- Common functions for ingesting raw and mapped sequencing files 

=head1 SYNOPSIS

Most common use:
 my $qi = QCMG::Ingest::Torrent->new();

=head1 DESCRIPTION

Contains methods for extracting run information from a raw sequencing run
folder, checking the sequencing files, and ingesting them into Mediaflux/LiveArc

=head1 REQUIREMENTS

 Exporter
 File::Spec
 QCMG::Automation::LiveArc
 QCMG::Automation::Common
 QCMG::DB::Geneus

=cut

use strict;

# standard distro modules
use Data::Dumper;
use File::Spec;				# for parsing paths
use Cwd;
use File::stat;
use Time::localtime;

# in-house modules
use lib qw(/usr/local/QCMG/QCMGPerl/lib/);
use QCMG::DB::Torrent;			# interface to Torrent Suite API
use QCMG::Automation::LiveArc;		# for accessing Mediaflux/LiveArc
use QCMG::FileDir::Finder;
use QCMG::DB::Geneus;			# for LIMS API
use QCMG::Automation::Common;
our @ISA = qw(QCMG::Automation::Common);	# inherit Common methods

use vars qw( $SVNID $REVISION $IMPORT_ATTEMPTED $IMPORT_SUCCEEDED );

################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 LOG_FILE         => path and filename of log file
 #ANALYSIS_FOLDER  => analysis folder
 GLPID	    	  => Geneus procecss ID
 NO_EMAIL   => 1 (do not send emails), 0 (send emails)

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

	print STDERR "CUSTOM VERSION\n";

	#$self->{'log_file'}		= $options->{'LOG_FILE'};

	# if NO_EMAIL => 1, don't send emails
	$self->{'no_email'}		= $options->{'NO_EMAIL'};
	if($self->{'no_email'} == 1) {
		print STDERR "Running silently\n";
	}

	$self->{'hostname'}		= `hostname`;
	chomp $self->{'hostname'};

	# slide process ID in Geneus, passed from LIMS
	$self->{'glpid'}		= $options->{'GLPID'};
	print STDERR "GLPID: $self->{'glpid'}\n";

	# get barcodes, patients, slide name. project
	if($self->{'glpid'}) {
		#print STDERR "Querying LIMS with $self->{'glpid'}\n";
	}
	else {
		# /results/analysis/output/Home/Auto_T01-40_84_141/
		print STDERR "Bypassing LIMS, using analysis folder\n";
		$self->{'analysis_folder'}	= $options->{'ANALYSIS_FOLDER'};
	}

	if(! $self->{'glpid'} && ! $self->{'analysis_folder'}) {
		exit($self->EXIT_NO_FOLDER());
	}

	# define LOG_FILE
	if($options->{'LOG_FILE'}) {
		$self->{'log_file'}		= $options->{'LOG_FILE'};
	}
	else {
		# DEFAULT: /tmp/slidename_ingest.log
		$self->{'log_file'}		= '/tmp/'.$self->{'glpid'}."_ingest.log";
	}

	$self->init_log_file();
	print STDERR "Writing log to $self->{'log_file'}\n";

	push @{$self->{'email'}}, $self->DEFAULT_EMAIL;


	# set global flags for checking in DESTROY()  (no/yes)
	$IMPORT_ATTEMPTED = 'no';
	$IMPORT_SUCCEEDED = 'no';

	# sleep for 8 hours to allow variant calls to happen
	#print STDERR "Sleeping for 8 hours...\n";
	#sleep(28800);
	#print STDERR "Waking up.\n";

	return $self;
}

# Exit codes:
sub EXIT_NO_FOLDER {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'analysis_folder'} failed";
	my $body	= "Failed: No analysis folder: $self->{'analysis_folder'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 1;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
	return 1;
}
sub EXIT_BAD_PERMS {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'analysis_folder'} failed";
	my $body	= "Failed: Bad permissions on $self->{'analysis_folder'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 2;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
	return 2;
}
sub EXIT_LIVEARC_ERROR {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'analysis_folder'} failed";
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
	my $subj	= "Ingestion of $self->{'analysis_folder'} failed";
	my $body	= "Failed: LiveArc asset exists";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 12;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
	return 12;
}
sub EXIT_IMPORT_FAILED {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'analysis_folder'} failed";
	my $body	= "Failed: LiveArc import failed";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 16;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
	return 16;
}
sub EXIT_NO_FASTQ {
	my $self	= shift @_;
	my $body	= "Failed: Cannot find a FASTQ file";
	my $subj	= "Ingestion of $self->{'analysis_folder'} failed";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);

	$self->{'EXIT'} = 17;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
	return 17;
}
sub EXIT_NO_BAM {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'analysis_folder'} failed";
	my $body	= "Failed: Cannot find a BAM file";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 18;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
	return 18;
}
sub EXIT_NO_SFF {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'analysis_folder'} failed";
	my $body	= "Failed: Cannot find a SFF file";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 19;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
	return 19;
}
=cut
sub EXIT_NO_ACQ {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'analysis_folder'} failed";
	my $body	= "Failed: Cannot find ACQ files";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 20;
	return 20;
}
=cut
sub EXIT_NO_ANALYSIS {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'analysis_folder'} failed";
	my $body	= "Failed: Cannot select analysis directory";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 21;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
	return 21;
}
sub EXIT_NO_ANALYSIS_REPORT {
        my $self        = shift @_;
        my $subj        = "Ingestion of $self->{'analysis_folder'} failed";
        my $body        = "Failed: Cannot create analysis report";
        $body           .= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
        $self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
        $self->writelog(BODY => "EXECLOG: errorMessage $body");

        $self->{'EXIT'} = 21;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
        return 21;
}
sub EXIT_NO_WELLS {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'analysis_folder'} failed";
	my $body	= "Failed: Cannot find .wells file";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 28;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
	return 28;
}
sub EXIT_PSQL_ERROR {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'analysis_folder'} failed";
	my $body	= "Failed: Cannot query qtorrentdb";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 29;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
	return 29;
}
sub EXIT_NO_EXPNAME {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'analysis_folder'} failed";
	my $body	= "Failed: Could not generate the final slide name";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 33;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
	return 33;
}
sub EXIT_BAMSORT_FAILED {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'analysis_folder'} failed";
	my $body	= "Failed: Could not sort BAM";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 73;
	$self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
	return 73;
}

################################################################################

=pod

B<patient_ids()>

Get list of patient IDs

Parameters:
 PATIENT_ID => ref to array of IDs (e.g., APGI_2153)

Returns:
 ref to hash of patient IDs and their barcode sequences
 $self->{'patient_id'}->{'APGI_0000'} = 'ATCTGATAC'

=cut
=cut
sub patient_ids {
	my $self	= shift @_;

	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $ids		= $options->{'PATIENT_ID'};

	# check that IDs follow expected patient ID pattern (APGI_2152,
	# AOCS_001, etc)
	foreach (@{$ids}) {
		if(/^[A-Z]+[\-\_]\d+$/) {
			$self->writelog(BODY => "Valid patient ID:\t$_");
			my $barcode	= $self->patient_barcode(ID => $id);
			$self->writelog(BODY => "Barcode:\t$barcode");
			$self->{'patient_id'}->{$_} = $barcode;
		}
		else {
			$self->writelog(BODY => "Invalid patient ID:\t$_, skipping");
		}
	}

	if(scalar(@{$self->{'patient_id'}}) < 1) {
		$self->writelog(BODY => "No valid patient IDs found!");
		#exit??
	}

	# hash of patient IDs and their barcode sequences
	return($self->{'patient_id'});
}
=cut

################################################################################

=pod

B<patient_barcode()>

Get Torrent barcode from a patient ID (via LIMS)

Parameters:
 PATIENT_ID =>  patient ID (e.g., APGI_2153)

Returns:

=cut
=cut
sub patient_barcode {
	my $self	= shift @_;

	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $id		= $options->{'PATIENT_ID'};

	my $gl = QCMG::DB::Geneus->new;
	$gl->connect;
	my ($data, $slide)	= $gl->project_by_patient($id);
	my $barcode		= $slide->{'Ion Torrent Verification Index Sequence'};
	my $barcode_id		= $slide->{'Ion Torrent Verification Index Name'};

	print STDERR "$id barcode: $barcode ($barcode_id)\n";
	print STDERR "WARNING: no barcode defined for $id!\n" if(! $barcode);

	return($barcode);
}
=cut

################################################################################

=pod

B<get_run_type()>

Use API to get run type from 'notes' field in 'experiment'
         "notes" : "Project_ID=icgc_pancreas;Patient_ID=APGI_1992;Primary_Library_ID=Library-20110923_D;RunType=Frag",

Parameters:
 EXP => experiment name (R_2011_09_18_00_28_38_user_T02-52)

Returns:

=cut
sub get_run_type {
	my $self	= shift @_;

	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $exp		= $options->{'EXP'};

	my $ts = QCMG::DB::Torrent->new;
	$ts->connect;

	$self->{'runtype'}	= $ts->slide_runtype($exp);

	return($self->{'runtype'});
}

################################################################################
=pod

B<get_barcodes()>

Get list of all barcodes on the slide

Parameters:

Requires:
 $self->{'mapsets'}->{<MAPSET>}->{'barcode'} ($self->{'mapsets'}->{$name}->{'barcode'}	= $barcode;)

Returns:
 ref to array of barcode strings

=cut
sub get_barcodes {
	my $self	= shift @_;

	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	# ONLY WORKS WHEN RUNNING BY GLPID

	$self->writelog(BODY => "Getting all barcodes");

	my @barcodes;
	foreach my $m (@{$self->{'barcodes'}}) {
		push @barcodes, $m;
		$self->writelog(BODY => "$m");
	}

	return(\@barcodes);
}

################################################################################
=pod

B<asset_name_raw()>

Return asset name, as derived from slide name, for raw data

Parameters:

Returns:
  scalar asset name

=cut
sub asset_name_raw {
	my $self	= shift @_;

	$self->{'asset_name_raw'} = $self->{'slide_name'}.$self->LA_ASSET_SUFFIX_RAW;

	return $self->{'asset_name_raw'};
}

################################################################################
=pod

B<get_namespace()>

Return LiveArc root level namespace

Parameters:

Returns:
  scalar string

=cut
sub get_namespace {
	my $self	= shift @_;

	return $self->LA_NAMESPACE;
}

################################################################################

=pod

B<asset_name_map()>

Return asset name, as derived from analysis folder name, for mapped data

Parameters:

Returns:
  scalar asset name

=cut
sub asset_name_map {
	my $self	= shift @_;

	$self->{'asset_name_map'} = $self->{'slide_name'}.$self->LA_ASSET_SUFFIX_MAP;

	return $self->{'asset_name_map'};
}

################################################################################

=pod

B<asset_name_res()>

Return asset name, for _seq_results

Parameters:

Returns:
  scalar asset name

=cut
sub asset_name_res {
	my $self	= shift @_;

	$self->{'asset_name_res'} = $self->{'slide_name'}.$self->LA_ASSET_SUFFIX_RES;

	return $self->{'asset_name_res'};
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

Requires:
 $self->{'analysis_folder'}

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

	$self->writelog(BODY => "---Checking ".$self->{'analysis_folder'}." ---");

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                defined($_[($i + 1)]) || 
                        die print STDERR "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	if(	!    $self->{'analysis_folder'} ||
		! -e $self->{'analysis_folder'} || 
		! -d $self->{'analysis_folder'}    ) {

		$self->writelog(BODY => "No valid analysis folder provided\n");
		#exit($self->EXIT_NO_FOLDER());
		exit($self->EXIT_NO_FOLDER());
	}

	if(	! -r $self->{'analysis_folder'}) {
		my $msg = " is not a readable directory";
		$self->writelog(BODY => $self->{'analysis_folder'}.$msg);
		exit($self->EXIT_BAD_PERMS());
	}

	if(	! -w $self->{'analysis_folder'}) {
		my $msg = " is not a writable directory";
		$self->writelog(BODY => $self->{'analysis_folder'}.$msg);
		#exit($self->EXIT_BAD_PERMS());
	}

	$self->writelog(BODY => "Analysis folder exists and is read/writable.");	

	return($status);
}

################################################################################
=pod

B<check_files()> 
 Check that the following files and directories exist:
  - .bam
  - .fastq
  - .sff
  - .wells
  - plugin_out/

Parameters:

Requires:
 $self->{'analysis_folder'}

Sets:
 $self->{'bam'}
 $self->{'fastq'}
 $self->{'sff'}
 $self->{'barcode_sff'}
 $self->{'wells'}
 $self->{'plugin_out'}

Returns:

=cut
sub check_files {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Finding .wells, .sff, .fastq, .bam, and plugin_out/ ---");

	### plugin_out/ dir
	my $plugindir	= $self->{'analysis_folder'}."/plugin_out/";
	if(-d $plugindir) {
		#push @{$self->{'plugin_out'}}, $plugindir;
		$self->{'plugin_out'}	= $plugindir;
	}
	else {
		$self->writelog(BODY => "Ingesting without plugin_out directory");
		#exit($self->EXIT_NO_PLUGINDIR());
	}

	my $barcodes	= $self->get_barcodes();

	#print Dumper $barcodes;

	# have to be in analysis folder for rel2abs symlink deconvolution to
	# work
	$self->{'cwd'}	=  Cwd::realpath("./");
	$self->writelog(BODY => "CWD: $self->{'cwd'}");
	chdir $self->{'analysis_folder'};

	### BAM files
	#my $cmd		= qq{ls $self->{'analysis_folder'}/*.bam};
	my $cmd		= qq{ find $self->{'analysis_folder'} $self->{'analysis_folder'}/bc_files/ -maxdepth 1 -type f -name "*.bam" | grep -v nomatch | grep -v tf };
	$self->writelog(BODY => "Find BAMs: $cmd");
	my @files	= `$cmd`;
	#print Dumper @files;
	foreach (@files) {
		chomp;
		#print STDERR "BAM: $_\n";
		#if(/IonX/ || /MID/ || /rawlib\.bam/) {
		my ($v, $d, $f)	= File::Spec->splitpath($_);
		#if($f =~ /.+?\_R\_/ || /rawlib\.bam/) {
		if($f =~ /\.bam/) {
			my $file;
			if(-l $_) {
				$file	= File::Spec->rel2abs( readlink($_) ) ;
				#print STDERR "$_, $file\n";
			}
			else {
				$file	= $_;
			}
			push @{$self->{'bam'}}, $file;
			$self->writelog(BODY => "BAM: $file ($_)");
		}

		#$self->{'bam'}	= $_;
		#$self->writelog(BODY => "BAM: $self->{'bam'}");
	}
	my $count = scalar(@{$self->{'bam'}}) ;
	$self->writelog(BODY => "Found $count *.bam files");

	if($count < 1) {
		exit($self->EXIT_NO_BAM());
	}

	### FASTQ files
	my $cmd		= qq{ls $self->{'analysis_folder'}/*.fastq};
	$self->writelog(BODY => "Find FASTQ: $cmd");
	my @files	= `$cmd`;
	foreach (@files) {
		chomp;

		#next unless(/rawlib\.fastq/);
		$self->{'fastq'}	= $_;
		$self->writelog(BODY => "FASTQ: $self->{'fastq'}");
	}
	my $count = 1 if($self->{'fastq'});
	$self->writelog(BODY => "Found $count *.fastq files");

	if($count < 1) {
		exit($self->EXIT_NO_FASTQ());
	}

	### SFF files
	my $cmd		= qq{ls $self->{'analysis_folder'}/*.sff};
	$self->writelog(BODY => "Find SFFs: $cmd");
	my @files	= `$cmd`;
	foreach (@files) {
		chomp;

		next if(/tf\.sff/);

		#next unless(/rawlib\.sff/);
		$self->{'sff'}	= $_;
		$self->writelog(BODY => "SFF: $self->{'sff'}");
	}
	my $count = 1 if($self->{'sff'});
	$self->writelog(BODY => "Found $count *.sff files");

	if($count < 1) {
		exit($self->EXIT_NO_SFF());
	}

	my $cmd		= qq{ls $self->{'analysis_folder'}/*_R_*.sff};
	$self->writelog(BODY => "Find barcoded .sff files: $cmd");
	my @files	= `$cmd`;
	foreach (@files) {
		chomp;

		next if(/tf\.sff/);

		#next unless(/IonX/ || /MID/);
		next if(/nomatch/);
		my $file;
		if(-l $_) {
			$file = File::Spec->rel2abs( readlink($_) ) ;
		}
		else {
			$file	= $_;
		}
		push @{$self->{'barcode_sff'}}, $file;
		$self->writelog(BODY => "Barcoded SFF: $file");
	}
	my $count = scalar(@{$self->{'barcode_sff'}}) if($self->{'barcode_sff'});
	$self->writelog(BODY => "Found $count barcoded sff files");

	### WELLS file
	my $cmd		= qq{ls $self->{'analysis_folder'}/*.wells};
	$self->writelog(BODY => "Find .wells: $cmd");
	my @files	= `$cmd`;
	foreach (@files) {
		chomp;
		my $file;
		if(-l $_) {
			$file = File::Spec->rel2abs( readlink($_) ) ;
		}
		else {
			$file	= $_;
		}
		$self->{'wells'}	= $file;
		$self->writelog(BODY => "WELLS: $self->{'wells'}");
		#print STDERR "KEEPING WELLS: $_\n";
	}
	my $count = scalar(@files);
	$self->writelog(BODY => "Found $count *.wells files");

	if($count < 1) {
		exit($self->EXIT_NO_WELLS());
	}

	#print Dumper $self->{'bam'};
	#print Dumper $self->{'fastq'};
	#print Dumper $self->{'sff'};
	#print Dumper $self->{'barcode_sff'};

	chdir $self->{'cwd'};

	return();
}

################################################################################
=pod

B<checksum_files()> 
 Perform checksum on .bam, .wells, .fastq files

Parameters:

Requires:
 $self->{'bam'} (full path and filename)
 $self->{'fastq'}
 $self->{'wells'}
 $self->{'sff'}
 $self->{'barcode_sff'}

Sets:
 $self->{'ck_checksum'} (keyed by pathless filename)
 $self->{'ck_filesize'}

Returns:

=cut
sub checksum_files {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->writelog(BODY => "Performing checksums on .wells, .sff, .fastq, .bam ...");

	my @files	= ();
	push @files, @{$self->{'bam'}};
	push @files, $self->{'wells'};
	push @files, $self->{'fastq'};
	push @files, $self->{'sff'};
	if(defined @{$self->{'barcode_sff'}}) {
		push @files, @{$self->{'barcode_sff'}};
	}

	#print Dumper @files;

	foreach (@files) {
		next unless(defined $_);
		my $file	= $_;
		# remove analysis folder from beginning of path
		#my $af		= $self->{'analysis_folder'};
		#$file		=~ s/($af)(.+)/$2/;
		#$file		=~ s/^\///;
		my ($crc, $fsize, $fname) = $self->checksum(FILE => $_);
		$self->writelog(BODY => "Checksum on $file: $crc, $fsize");

		$self->{'ck_checksum'}->{$file} = $crc;
		$self->{'ck_filesize'}->{$file} = $fsize;
	}

	return();
}

################################################################################
=pod

B<query_lims()> 
 Get slide info from LIMS

Parameters:
 GLPID - Geneus process iD

Returns:

Sets:
 $self->{'slide_name'}
 $self->{'barcodes'} (ref to array of strings)

=cut
sub query_lims {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }
=cut

	my $md  = QCMG::DB::Metadata->new();
	
	# barcoded solid4 run in Geneus:        S0428_20111212_2_FragPEBC
	# LMP solid4 run in Geneus:             S0436_20120530_1_LMP
	
	my $slide       = $self->{'slide_name_reformatted'};;
	   
	if($md->find_metadata("slide", $slide)) {
	        my $slide_mapsets       =  $md->slide_mapsets($slide);
	        foreach my $mapset (@$slide_mapsets) {
	                $md->find_metadata("mapset", $mapset);
	
	                my $d           = $md->mapset_metadata($mapset);
	
	                foreach my $details (keys %$d){
	                        print $details." - ".$d->{$details}."\n";
	                }
	                print "\n"
	        }
	}


=cut



	$self->{'gl'} = QCMG::DB::Geneus->new(server   => 'http://qcmg-glprod.imb.uq.edu.au:8080/api/v1/', verbose => 0);

	# TORRENT
	# http://qcmg-gltest:8080/api/v1/processes/PGM-TJB-120419-24-51770
	#my $process_id	= 'PGM-TJB-120419-24-51770';

	my $process_id	= $options->{'GLPID'};

	$self->{'gl'}->connect;

	# get SLIDE
	#print STDERR "Getting SLIDE by process ID\n";
	my ($data, $slide)	= $self->{'gl'}->run_by_process($process_id);
	#print Dumper $slide;

	my $slidename	= '';
	foreach my $mapset (keys %{$slide}) {
		$slidename	= $mapset;

		$self->{'slide_name'}	= $slidename;

		last;
	}

	## get MAPSETS
	#print STDERR "Getting mapsets by slidename $slidename\n";

	my ($data, $slide)	= $self->{'gl'}->torrent_run_by_slide($self->{'slide_name'});
	#print Dumper $slide;

	foreach my $field (keys %{$slide}) {
		my ($name, $barcode, $patient, $project);
		foreach my $key (keys %{$slide->{$field}}) {
			$name		= $slide->{$field}->{$key} if($key eq 'name');
			$barcode	= $slide->{$field}->{$key} if($key eq 'barcode');
			$patient	= $slide->{$field}->{$key} if($key eq 'pid');
			$project	= $slide->{$field}->{$key} if($key eq 'artifact-group');
		}
	}

	return();
}

################################################################################
=pod

B<slide_name()> 
  Get slide name from LIMS

Parameters:
 SLIDENAME => for setting slidename

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

	if($options->{'SLIDENAME'}) {
		$self->{'slide_name'}	= $options->{'SLIDENAME'};
	}

	if(! $self->{'slide_name'}) {
		$self->writelog(BODY => "No slide name provided");
		exit($self->EXIT_NO_FOLDER());
	}

        return $self->{'slide_name'};
}

################################################################################
=pod

B<_infer_analysis_folder()> 
  Infer report (analysis) folder path from slide name 

Parameters:

Requires:
 $self->{'slide_name'}

Returns:
  scalar path 

=cut
sub _infer_analysis_folder {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->writelog(BODY => "Inferring analysis folder from slide name: $self->{'slide_name'}");

	# T00001_20120419_162
	#print STDERR "Getting dir from slidename $self->{'slide_name'}\n";

	#                     1         2       3       4     5       6
	$self->{'slide_name'}		=~ /^(T)\d{3}(\d{2})\_(\d{4})(\d{2})(\d{2})\_(\d+)$/;

	# find /results/analysis/output/Home/ -name "*T01-162*" -type d
	# /results/analysis/output/Home/Auto_T01-162-Library_20120413_E_338_529
	my $pattern	= "*".$1.$2."-".$6."*";

	my $cmd		= qq{find }.$self->TORRENT_ANALYSIS_DIR.qq{ -type d -name "$pattern"};
	$self->writelog(BODY => "Find command: $cmd");
	my @dirs	= `$cmd`;

	# go through all directories and choose the newest (or only) one
	my $csecs_init	= 0;
	foreach my $dir (@dirs) {
		chomp $dir;
		my $datetime_string = ctime(stat($dir)->mtime); 
		my $csecs	=  stat($dir)->mtime; 

		$self->writelog(BODY => "$datetime_string\t$dir");

		# get most recently created directory
		if($csecs > $csecs_init) {
			$self->{'analysis_folder'}	= $dir;
			$csecs_init = $csecs;
		}
	}

	$self->writelog(BODY => "Selected newest analysis folder $self->{'analysis_folder'}");

	if(! $self->{'analysis_folder'}) {
		$self->writelog(BODY => "No analysis folder found");
		exit($self->EXIT_NO_ANALYSIS());
	}

        return $self->{'analysis_folder'};
}

################################################################################
=pod

B<analysis_folder()> 
  Set report (analysis) folder path 

Parameters:

Requires:
 $self->{'slide_name'}
 $self->{'analysis_folder'} (can be empty)

Returns:
  scalar path 

=cut
sub analysis_folder {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->writelog(BODY => "Setting analysis folder $self->{'analysis_folder'}");

	unless($self->{'analysis_folder'}) {
		$self->writelog(BODY => "Analysis folder not supplied by user, inferring....");

		$self->_infer_analysis_folder();

=cut
		# T00001_20120419_162
		my $slide	= $self->{'slide_name'};
	
		#                     1         2       3       4     5       6
		$slide		=~ /^(T)\d{3}(\d{2})\_(\d{4})(\d{2})(\d{2})\_(\d+)$/;
	
		# find /results/analysis/output/Home/ -name "*T01-162*" -type d
		# /results/analysis/output/Home/Auto_T01-162-Library_20120413_E_338_529
		my $pattern	= "*".$1.$2."-".$6."*";
	
		my $cmd		= qq{find }.$self->TORRENT_ANALYSIS_DIR.qq{ -type d -name "$pattern"};
		$self->writelog(BODY => "Find command: $cmd");
		my @dirs	= `$cmd`;
		$self->writelog(BODY => join "\n", @dirs);
	
		if(scalar(@dirs) > 1) {
			$self->writelog(BODY => "Multiple analysis folders found");
			exit($self->EXIT_NO_ANALYSIS());
		}
		elsif(scalar(@dirs) < 1) {
			$self->writelog(BODY => "No analysis folders found");
			exit($self->EXIT_NO_ANALYSIS());
		}
		else {
			$self->{'analysis_folder'}	= shift @dirs;
			chomp $self->{'analysis_folder'};
		}
=cut
	}

        return $self->{'analysis_folder'};
}

################################################################################
=pod

B<reformat_slide_name()> 
  Reformat slide name to conform to our naming convention:
	R_2012_01_09_07_34_14_user_T01-100_Reanalysis_T01-100 
	=>
	T01_20120109_100_nort

Parameters:

Requires:
 $self->{'sff'}

Returns:
  scalar: new slide name 

Sets:
 $self->{'slide_name_reformatted'}
 $self->{'expname'}

=cut
sub reformat_slide_name {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Reformatting slide name...---");
	#print STDERR "REFORMATTING SLIDE NAME\n";

	# /path/R_2012_01_09_07_34_14_user_T01-100_Reanalysis_T01-100.sff
	# just extract date from this name -> 20120109
	my $exp		= $self->{'sff'};
	$self->writelog(BODY => "Using $exp to get experiment name");

	#  ./Reanalysis_T01-100_357//R_2012_01_09_07_34_14_user_T01-100_Reanalysis_T01-100.sff
	$exp		=~ /R\_(\d{4})\_(\d{2})\_(\d{2})\_.+(T\d{2})\-(\d+)/;
	#$exp		=~ /R\_(\d{4})\_(\d{2})\_(\d{2})\_.+?\_(T\d+)\-(\d+)\W/;
	my $date	= join "", $1, $2, $3;
	my $serial	= $4;	# machine "serial number"
	my $flowcell	= $5;	# not really flow cell, but will go in that pos

	#                    R  _2012 _ 02   _ 12   _  00  _37    _ 53                    _user_T01-124
	#                    R _ 2012 _ 01   _ 09   _  07  _ 34   _ 14                    _user_T01-100_Reanalysis_T01-100.sff
	#$exp		=~ /(R\_\d{4}\_\d{2}\_\d{2}\_\d{2}\_\d{2}\_\d{2}\_\w+\_T\d+\-\d+)\_/;
	$exp		=~ /R\_(\d{4})\_(\d{2})\_(\d{2})\_.+(T\d{2})\-(\d+)/;
	my $expname	= $1;
	$self->{'expname'}	= $expname;

	#print STDERR "Experiment name: $expname\n";
	$self->writelog(BODY => "Experiment name: $expname");

	$exp		=~ /R\_(\d{4})\_(\d{2})\_(\d{2})\_.+(T\d{2})\-(\d+)/;
	#$exp		=~ /R\_(\d{4})\_(\d{2})\_(\d{2})\_\d{2}\_\d{2}\_\d{2}\_\w+\_(T\d+)\-(\d+)\_/;
	my $serial	= $4;
	my $date	= join '', $1, $2, $3;
	my $flowcell	= $5;

	# add run type
	my $name	= $serial."_".$date."_".$flowcell;

	if($name eq '__') {
		my $cmd		= qq{find $self->{'analysis_folder'} -maxdepth 1 -name "R_*" | head -n 1};
		$self->writelog(BODY => "$cmd");
		my $file	= `$cmd`;
		chomp $file;

		$self->writelog(BODY => "Experiment name not created, trying $file");
		#print STDERR "Experiment name not created, trying $file\n";

		#R_2012_06_08_15_27_25_user_T01-207
		$file		=~ /R\_(\d{4})\_(\d{2})\_(\d{2})\_.+(T\d{2})\-(\d+)/;

		my $serial	= $4;
		my $date	= join '', $1, $2, $3;
		my $flowcell	= $5;

		# add run type
		$name	= $serial."_".$date."_".$flowcell;
	}

	if($name !~ /T00/) {
		$name	=~ s/T0/T0000/;
	}

	print STDERR "NEW NAME: $name\n";
	$self->writelog(BODY => "New name: $name");

	if(! $name || $name eq '__') {
		$self->writelog(BODY => "Name still undefined, failing");
		#print STDERR "Name still undefined, failing\n";
		exit($self->EXIT_NO_EXPNAME());
	}

	$self->{'slide_name'}	= $name;

        return($self->{'slide_name'});
}

################################################################################
=pod

B<reformat_bam_name()> 
  Reformat BAM name to conform to our naming convention:
	R_2012_01_09_07_34_14_user_T01-100_Reanalysis_T01-100 
	=>
	T01_20120109_100

	or

	IonXpress_001_R_2012_03_04_18_17_54_user_T01-140_Reanalysis_T01-140.bam
	=>
	T01_20120304_140.nopd.IonXpress_001.bam

Parameters:
 BAM -> BAM filename

Requires:
 $self->{'slide_name'}

Returns:
  scalar: new BAM name  (for use as LiveArc asset name)

Sets:

=cut
sub reformat_bam_name {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $bam		= $options->{'BAM'};
	# /results/analysis/output/Home/Auto_T01-260-MET_463_701/bc_files/ACACATACGC_rawlib.bam
	my ($v, $d, $f)	= File::Spec->splitpath($bam);
	#$bam		= $f;

	my $name;
=cut
	if($bam =~ /IonX/) {
		# IonXpress_022_R_2012_03_04_18_17_54_user_T01-140_Reanalysis_T01-140.bam	
		$bam	=~ /^(IonXpress\_\d+)/;
		$name	= $self->{'slide_name'}.".nopd.$1.bam";
	}
	elsif($bam =~ /MID/) {
		# MID_99_R_2012_06_20_12_52_55_user_T01-214-Library_20120403_B_Analysis_T01-214.bam
		$bam	=~ /^(MID\_\d+)/;
		$name	= $self->{'slide_name'}.".nopd.$1.bam";
	}
=cut

	print STDERR "Reformatting $bam -> $f\n";
	# Reformatting /results/analysis/output/Home/Auto_T01-260-MET_463_701/bc_files/ACACATACGC_rawlib.bam -> ACACATACGC_rawlib.bam

	if($f =~ /^R\_20/) {
		$name	= $self->{'slide_name'}.".nopd.nobc.bam";
	}
	elsif($f =~ /IonX/i || $f =~ /MID/) {
		$f	=~ /(.+?\_\d+)\_/;
		$name	= $self->{'slide_name'}.".nopd.$1.bam";
	}
	elsif($f =~ /^[ACTG]+\_/) {
		$f	=~ /(.+?)\_/;
		$name	= $self->{'slide_name'}.".nopd.$1.bam";
	}
	else {
		$name	= $self->{'slide_name'}.".nopd.nobc.bam";
	}

	#print STDERR "NEW BAM NAME: $name\n";

        return($name);
}

################################################################################
=pod

B<pdf_report()> 
 Generate a PDF of the analysis report via
 http://10.160.72.27/output/Home/Auto_T02-6_9_011/Default_Report.php?do_print=True

Parameters:
 none (requires $self->{'slide_name'})

Returns:
 void

=cut
sub pdf_report {
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

	# ANALYSIS_FOLDER => '/results/analysis/output/Home/Auto_T02-6_9_011/'

	# http://10.160.72.27/output/Home/Auto_T02-6_9_011/Default_Report.php?do_print=True

	$self->writelog(BODY => "---Generating PDF report---");

	require LWP::UserAgent;
	require HTTP::Request;

	# get analysis folder dir name without full path
	my $path = $self->{'analysis_folder'};
	$path =~ s/\/$//;
	my ($v,$d,$f) = File::Spec->splitpath($path);
	my $af	= $f;

	my $LINK	= "http://localhost:80/output/Home/$af/Default_Report.php?do_print=True";

        #print STDERR $LINK."\n";

        my $ua = new LWP::UserAgent;
        $ua->agent("LWP::UserAgent/0.0 ".$ua->agent);

        my $req = new HTTP::Request POST => $LINK;

	my $res = $ua->request($req);

	my $dlfile = '/tmp/'.$self->{'slide_name'}.".nopd.nobc.analysis.pdf";
	$self->{'pdf_report'} = $dlfile;

	$self->writelog(BODY => "Report: $dlfile");

	# if the request is successful
	if ($res->is_success) {
		open(OUT, ">$dlfile");
		binmode(OUT);
		print OUT $res->content;
		close OUT;
	}
	else {
		exit($self->EXIT_NO_ANALYSIS_REPORT());
	}

	# if file was not created, exit
	if(! $dlfile) {
		exit($self->EXIT_NO_ANALYSIS_REPORT());
	}

	$self->writelog(BODY => "Report generated");

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

	my $sendmail	= "/usr/sbin/sendmail -t"; 
	#my $reply_to	= "Reply-to: l.fink\@imb.uq.edu.au\n"; 
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
	#print SENDMAIL $reply_to; 
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

B<check_slide_namespace()> 
 Check if toplevel slide namespace already exists in LiveArc (shouldn't exist
 for new ingests)
 
Parameters:
 NAMESPACE => slide name

Returns:
 scalar: 0 if exists, 1 if doesn't exist

=cut
sub check_slide_namespace {
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

	$self->writelog(BODY => "Checking if namespace exists...");

	my $ns		= $options->{'NAMESPACE'};

	my $assetid	= "";
	my $r		= $self->{'mf'}->namespace_exists(NAMESPACE => $ns);

	# if no namespace, create it; try a few times with naps in between
	# attempts 
	my $num_attempt	= 0;
	until($r == 0 || $num_attempt == 3) {
		$self->writelog(BODY => "Creating $ns");
	
		$r	= $self->{'mf'}->create_namespace(NAMESPACE => $ns);

		if($r != 0) {
			$num_attempt++;
			sleep(120);
		}
	}

	# if namespace could not be created,       return 1
	# if namespace exists or was created here, return 0
	return($r);
}

################################################################################
=pod

B<prepare_ingest()> 
 Set up for ingest - make sure namespace is created if it doesn't exist and that
  assets don't exist yet

Parameters:

Returns:
 void

=cut
sub prepare_ingest {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Preparing for ingestion... ---");

	my $mf	= QCMG::Automation::LiveArc->new(VERBOSE => 1);

	$self->{'mf'} = $mf;

	# check if slide name namespace is already in Mediaflux/LiveArc
	my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};

	#print STDERR "SLIDE NAME: ",$self->{'slide_name'},"\n";

	# asset to contain zip of whole directory (excepting files that are
	# ingested separately)
	#my $assetraw	= $self->asset_name_raw();	# seq_raw
	#my $assetmap	= $self->asset_name_map();	# seq_mapped
	my $assetres	= $self->asset_name_res();	# seq_results

	# check that top level namespace for slide exists (or create it)
	my $r		= $self->check_slide_namespace(NAMESPACE => $ns);

	# if namespace exists, check if assets exist
	if($r == 0) {
		$self->writelog(BODY => "Namespace $ns exists");

	}

	# broadcast and record status
	my $subj	= "Ingestion of ".$ns;
	my $body	= "Ingestion initiated\n\nCheck log file in ".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);

	$IMPORT_ATTEMPTED = 'yes';

	my $date = $self->timestamp();

	$self->writelog(BODY => "Ingest import started $date");

	return();
}

################################################################################
=pod

B<ingest_wells()> 
 Ingest .wells file

Parameters:

Requires:
 $self->{'wells'}

Returns:
 void

=cut
sub ingest_wells {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $status = 2;

	$self->writelog(BODY => "---Ingesting .wells file ---");

	my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};

	$self->writelog(BODY => "Preparing to write metadata, if any...");
	my $meta;
	my $f	= $self->{'wells'};
	if($self->{'ck_checksum'}->{$f}) {
		$self->writelog(BODY => "Generating LiveArc metadata...");

		$meta = qq{:meta < :checksums < :source orig };
		$meta .= qq{ :checksum < :chksum };
		$meta .= $self->{'ck_checksum'}->{$f}.' ';
		$meta .= qq{ :filesize };
		$meta .= $self->{'ck_filesize'}->{$f}.' ';
		$meta .= qq{ :filename $f > };
		$meta .= qq{ > >};

		$self->writelog(BODY => "Metadata: $meta");
	}
	else {
		$self->writelog(BODY => "Skipping LiveArc metadata generation...");
	}
	$self->writelog(BODY => "Done checking metadata");

	# build new wells file name to match slide name
	my $wellsname	= $self->{'slide_name'}.".nopd.nobc.wells";

	my $id	= $self->{'mf'}->create_asset(NAMESPACE => $ns, ASSET => $wellsname);
	if(! $id) {
		$self->writelog(BODY => "Could not create asset $wellsname");
		exit($self->EXIT_IMPORT_FAILED());
	}
	$status	= $self->{'mf'}->update_asset_file(NAMESPACE => $ns, ASSET => $wellsname, FILE => $self->{'wells'});
	$self->writelog(BODY => "Import result: $status, $self->{'wells'}");

	# set metadata if import was successful
	$self->writelog(BODY => "Updating metadata...");
	if($status == 0 && $meta) {
		my $r = $self->{'mf'}->update_asset(NAMESPACE => $ns, ASSET => $wellsname, DATA => $meta);
		$self->writelog(BODY => "Update metadata result: $r");
	}

	$self->writelog(BODY => "Import complete");

	if($status > 0) {
		exit($self->EXIT_IMPORT_FAILED());
	}

	return();
}

################################################################################
=pod

B<ingest_sff()> 
 Ingest .sff file

Parameters:

Requires:
 $self->{'sff'}

Returns:
 void

=cut
sub ingest_sff {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $status = 2;

	$self->writelog(BODY => "---Ingesting .sff file ---");

	my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};

	$self->writelog(BODY => "Preparing to write metadata, if any...");
	my $meta;
	my $f	= $self->{'sff'};
	if($self->{'ck_checksum'}->{$f}) {
		$self->writelog(BODY => "Generating LiveArc metadata...");

		$meta = qq{:meta < :checksums < :source orig };
		$meta .= qq{ :checksum < :chksum };
		$meta .= $self->{'ck_checksum'}->{$f}.' ';
		$meta .= qq{ :filesize };
		$meta .= $self->{'ck_filesize'}->{$f}.' ';
		$meta .= qq{ :filename $f > };
		$meta .= qq{ > >};

		$self->writelog(BODY => "Metadata: $meta");
	}
	else {
		$self->writelog(BODY => "Skipping LiveArc metadata generation...");
	}
	$self->writelog(BODY => "Done checking metadata");

	# build new sff file name to match slide name
	my $sffname	= $self->{'slide_name'}.".nopd.nobc.sff";

	my $id = $self->{'mf'}->create_asset(NAMESPACE => $ns, ASSET => $sffname);
	if(! $id) {
		$self->writelog(BODY => "Could not create asset $sffname");
		exit($self->EXIT_IMPORT_FAILED());
	}
        $status = $self->{'mf'}->update_asset_file(NAMESPACE => $ns, ASSET => $sffname, FILE => $self->{'sff'});
	$self->writelog(BODY => "Import result: $status, $self->{'sff'}");

	# set metadata if import was successful
	$self->writelog(BODY => "Updating metadata...");
	if($status == 0 && $meta) {
		my $r = $self->{'mf'}->update_asset(NAMESPACE => $ns, ASSET => $sffname, DATA => $meta);
		$self->writelog(BODY => "Update metadata result: $r");
	}

	$self->writelog(BODY => "Import complete");

	if($status > 0) {
		exit($self->EXIT_IMPORT_FAILED());
	}

	# ingest barcode SFF file if appropriate
	#if($self->{'runtype'} eq 'FragBC') {
	#	$self->ingest_barcode_sff();
	#}

	return();
}

################################################################################
=pod

B<ingest_barcode_sff()> 
 Ingest barcoded sff files

Parameters:

Requires:
 $self->{'barcode_sff'}

Returns:
 void

=cut
sub ingest_barcode_sff {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $status = 2;

	$self->writelog(BODY => "---Ingesting barcoded sff files ---");

	# don't attempt to do this if there is no file to ingest
	unless($self->{'barcode_sff'}) {
		return();
	}

	my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};

=cut
IonXpress_002_R_2012_06_01_10_11_00_user_T01-195-Library_20120525_C_Analysis_T01-195.sff
 (bc_files/IonXpress_002_rawlib.sff)
IonXpress_004_R_2012_06_01_10_11_00_user_T01-195-Library_20120525_C_Analysis_T01-195.sff
IonXpress_014_R_2012_06_01_10_11_00_user_T01-195-Library_20120525_C_Analysis_T01-195.sff
IonXpress_016_R_2012_06_01_10_11_00_user_T01-195-Library_20120525_C_Analysis_T01-195.sff
=cut

	if(scalar(@{$self->{'barcode_sff'}}) < 1) {
		$self->writelog(BODY => "No barcoded sff files to ingest");
		return();
	}


	foreach (@{$self->{'barcode_sff'}}) {
		my $sff		= $_;

		print STDERR "INGESTING BARCODE SFF $sff\n";
		# /results/analysis/output/Home/Auto_T01-260-MET_463_701/bc_files/ACACATACGC_rawlib.sff

		# /results/analysis/output/Home/Analysis_T01-214_623/bc_files/MID_101_rawlib.sff

		my $asset;
=cut
		if($sff		=~ /IonX/) {
			$sff	=~ /(IonXpress\_\d+)\_/;
			$asset	= join '.', $self->{'slide_name'}, 'nopd', $1, "sff";
			print STDERR "ASSET: $asset\n";
		}
		elsif($sff	=~ /MID/) {
			$sff	=~ /(MID\_\d+)\_/;
			$asset	= join '.', $self->{'slide_name'}, 'nopd', $1, "sff";
			print STDERR "ASSET: $asset\n";
		}
=cut
		my ($v, $d, $f)	= File::Spec->splitpath($sff);
		if($f =~ /rawlib/) {
			$f		=~ /(.+)\_rawlib.sff/;
			$asset		= join '.', $self->{'slide_name'}, 'nopd', $1, "sff";
		}
		elsif($f =~ /^(.+?)\_R\_2/) {
			$f		=~ /^(.+?)\_R/;
			$asset		= join '.', $self->{'slide_name'}, 'nopd', $1, "sff";
		}
		print STDERR "ASSET: $asset\n";
		
		$self->writelog(BODY => "Preparing to write metadata, if any...");
		my $meta;
		if($self->{'ck_checksum'}->{$_}) {
			$self->writelog(BODY => "Generating LiveArc metadata...");
	
			$meta = qq{:meta < :checksums < :source orig };
			$meta .= qq{ :checksum < :chksum };
			$meta .= $self->{'ck_checksum'}->{$_}.' ';
			$meta .= qq{ :filesize };
			$meta .= $self->{'ck_filesize'}->{$_}.' ';
			$meta .= qq{ :filename $_ > };
			$meta .= qq{ > >};

			$self->writelog(BODY => "Metadata: $meta");
		}
		else {
			$self->writelog(BODY => "Skipping LiveArc metadata generation...");
		}
		$self->writelog(BODY => "Done checking metadata");

		my $id = $self->{'mf'}->create_asset(NAMESPACE => $ns, ASSET => $asset);
		if(! $id) {
			$self->writelog(BODY => "Could not create asset $asset");
			exit($self->EXIT_IMPORT_FAILED());
		}
      		$status = $self->{'mf'}->update_asset_file(NAMESPACE => $ns, ASSET => $asset, FILE => $sff);
		$self->writelog(BODY => "Import result: $status, $sff");

		# set metadata if import was successful
		$self->writelog(BODY => "Updating metadata...");
		if($status == 0 && $meta) {
			my $r = $self->{'mf'}->update_asset(NAMESPACE => $ns, ASSET => $asset, DATA => $meta);
			$self->writelog(BODY => "Update metadata result: $r");
		}
	}

	$self->writelog(BODY => "Import complete");

	if($status > 0) {
		exit($self->EXIT_IMPORT_FAILED());
	}

	return();
}

################################################################################
=pod

B<ingest_fastq()> 
 Ingest .fastq file

Parameters:

Requires:
 $self->{'fastq'}

Returns:
 void

=cut
sub ingest_fastq {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $status = 2;

	$self->writelog(BODY => "---Ingesting .fastq file ---");

	my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};

	$self->writelog(BODY => "Preparing to write metadata, if any...");
	my $meta;
	my $f	= $self->{'fastq'};
	if($self->{'ck_checksum'}->{$f}) {
		$self->writelog(BODY => "Generating LiveArc metadata...");

		$meta = qq{:meta < :checksums < :source orig };
		$meta .= qq{ :checksum < :chksum };
		$meta .= $self->{'ck_checksum'}->{$f}.' ';
		$meta .= qq{ :filesize };
		$meta .= $self->{'ck_filesize'}->{$f}.' ';
		$meta .= qq{ :filename $f > };
		$meta .= qq{ > >};

		$self->writelog(BODY => "Metadata: $meta");
	}
	else {
		$self->writelog(BODY => "Skipping LiveArc metadata generation...");
	}
	$self->writelog(BODY => "Done checking metadata");

	# build new fastq file name to match  slide name
	my $fastqname	= $self->{'slide_name'}.".nopd.nobc.fastq";

	# initialize error status and number of attempted imports
	my $status	= 2;
	my $attempts	= 0;
	my $maxattempts	= 4;

	my $id = $self->{'mf'}->create_asset(NAMESPACE => $ns, ASSET => $fastqname);
	if(! $id) {
		$self->writelog(BODY => "Could not create asset $fastqname");
		#exit($self->EXIT_IMPORT_FAILED());
	}
        $status = $self->{'mf'}->update_asset_file(NAMESPACE => $ns, ASSET => $fastqname, FILE => $self->{'fastq'});
	$self->writelog(BODY => "Import result: $status, $self->{'fastq'}");

	# set metadata if import was successful
	$self->writelog(BODY => "Updating metadata...");
	if($status == 0 && $meta) {
		my $r = $self->{'mf'}->update_asset(NAMESPACE => $ns, ASSET => $fastqname, DATA => $meta);
		$self->writelog(BODY => "Update metadata result: $r");
	}

	$self->writelog(BODY => "Import complete");

	if($status > 0) {
		exit($self->EXIT_IMPORT_FAILED());
	}

	return();
}

################################################################################
=pod

B<sort_bam()> 
 Sort .bam file(s) and copy them over the existing ones

Parameters:

Requires:
 $self->{'bam'}

Returns:
 void

=cut
sub sort_bam {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Sorting .bam file(s) ---");

	my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};
	my $status	= 2;

	print Dumper $self->{'bam'};

	# keep track of ingested BAMs, don't ingest a second version (? not sure why this happens)
	my %ingested;

	foreach my $f (@{$self->{'bam'}}) {
		my ($v, $d, $file)	= File::Spec->splitpath($f);
		my $srt	= "/tmp/".$file.".sorted";
		my $cmd	= qq{java -jar /share/software/picard-tools-1.62/SortSam.jar INPUT=$f OUTPUT=$srt SO=coordinate};
		my $rv	= system($cmd);
		if($rv != 0) {
			$self->writelog(BODY => "RV: $rv  - $cmd");
			exit($self->EXIT_BAMSORT_FAILED());
		}
		$cmd	= qq{mv $srt $f};
		$rv	= system($cmd);
		if($rv != 0) {
			$self->writelog(BODY => "RV: $rv  - $cmd");
			exit($self->EXIT_BAMSORT_FAILED());
		}
	}

	return();
}

################################################################################
=pod

B<ingest_bam()> 
 Ingest .bam file(s)

Parameters:

Requires:
 $self->{'bam'}

Returns:
 void

=cut
sub ingest_bam {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Ingesting .bam file(s) ---");

	my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};
	my $status	= 2;

	print Dumper $self->{'bam'};

	# keep track of ingested BAMs, don't ingest a second version (? not sure why this happens)
	my %ingested;

	foreach my $f (@{$self->{'bam'}}) {
		# /results/analysis/output/Home/Auto_T01-260-MET_463_701/bc_files/ACACATACGC_rawlib.bam
		print STDERR "Ingesting BAM $f\n";
		$self->writelog(BODY => "Preparing to write metadata, if any...");
		my $meta;
		if($self->{'ck_checksum'}->{$f}) {
			$self->writelog(BODY => "Generating LiveArc metadata...");
	
			$meta = qq{:meta < :checksums < :source orig };
			$meta .= qq{ :checksum < :chksum };
			$meta .= $self->{'ck_checksum'}->{$f}.' ';
			$meta .= qq{ :filesize };
			$meta .= $self->{'ck_filesize'}->{$f}.' ';
			$meta .= qq{ :filename $f > };
			$meta .= qq{ > >};
	
			$self->writelog(BODY => "Metadata: $meta");
		}
		else {
			$self->writelog(BODY => "Skipping LiveArc metadata generation...");
		}
		$self->writelog(BODY => "Done checking metadata");
	
		### FIX WITH BARCODES
		# build new bam file name to match slide name
		my $bamname	= $self->reformat_bam_name(BAM => $f);
		print STDERR "NEW BAM NAME: $bamname\n";

		next if($ingested{$bamname} == 1);
	
		my $id = $self->{'mf'}->create_asset(NAMESPACE => $ns, ASSET => $bamname);
		if(! $id) {
			$self->writelog(BODY => "Could not create asset $bamname");
			exit($self->EXIT_IMPORT_FAILED());
		}
		$status = $self->{'mf'}->update_asset_file(ID => $id, FILE => $f);
		$self->writelog(BODY => "Import result: $status, $f");
	
		# set metadata if import was successful
		$self->writelog(BODY => "Updating metadata...");
		if($status == 0 && $meta) {
			my $r = $self->{'mf'}->update_asset(NAMESPACE => $ns, ASSET => $bamname, DATA => $meta);
			$self->writelog(BODY => "Update metadata result: $r");
		}

		$ingested{$bamname} = 1;
	}

	$self->writelog(BODY => "Import complete");

	if($status > 0) {
		exit($self->EXIT_IMPORT_FAILED());
	}

	return();
}

################################################################################
=pod

B<ingest_plugin_out()> 
 Ingest .plugin_out file

Parameters:

Requires:
 $self->{'plugin_out'}

Returns:
 void

=cut
sub ingest_plugin_out {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $status = 2;

	$self->writelog(BODY => "---Ingesting plugin_out dir ---");

	my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};

	# build new plugin_out file name to match slide name
	# *use _ to indicate it is not a file)
	my $plugin_outname	= $self->{'slide_name'}.".nopd.nobc_plugin_out";

	my $cmd = $self->{'mf'}->laimport(NAMESPACE => $ns, ASSET => $plugin_outname, DIR => $self->{'plugin_out'}, CMD => 1);
	$status = $self->{'mf'}->laimport(NAMESPACE => $ns, ASSET => $plugin_outname, DIR => $self->{'plugin_out'});

	$self->writelog(BODY => "Import result: $status, $self->{'plugin_out'}");

	$self->writelog(BODY => "Import complete");

	if($status > 0) {
		exit($self->EXIT_IMPORT_FAILED());
	}

	return();
}

################################################################################
=pod

B<ingest_report()> 
 Ingest report .pdf file

Parameters:

Requires:
 $self->{'pdf_report'}

Returns:
 void

=cut
sub ingest_report {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $status = 2;

	$self->writelog(BODY => "---Ingesting PDF report file ---");

	my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};

	my ($v, $d, $assetname)	= File::Spec->splitpath($self->{'pdf_report'});

	my $id = $self->{'mf'}->create_asset(NAMESPACE => $ns, ASSET => $assetname);
	if(! $id) {
		$self->writelog(BODY => "Could not create asset $assetname");
		exit($self->EXIT_IMPORT_FAILED());
	}
	$status = $self->{'mf'}->update_asset_file(ID => $id, FILE => $self->{'pdf_report'});
	$self->writelog(BODY => "Import result: $status, $self->{'pdf_report'}");

	$self->writelog(BODY => "Import complete");

	if($status > 0) {
		exit($self->EXIT_IMPORT_FAILED());
	}

	return();
}

################################################################################
=pod

B<ingest_plugin_files()> 
 Ingest plugin variant calls and coverage

Parameters:

Requires:
 $self->{'plugin_out'}

Returns:
 void

=cut
sub ingest_plugin_files {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $status = 0;

	$self->writelog(BODY => "---Ingesting coverage and variant files ---");

	my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};

	my $barcodes	= $self->get_barcodes();

	# coverage data
	my $coveragedir	= $self->{'plugin_out'}."/coverageAnalysis_out/";
	my $coveragefile = $coveragedir."coverageAnalysis.html";
	my $covfilenew	= $self->{'slide_name'}.'.nopd.nobc.coverageAnalysis.html';
	# indel and SNP data
	my $variantdir	= $self->{'plugin_out'}."/variantCaller_out/";
	my $indelfile	= $variantdir."indel_variants.vcf";
	my $SNPfile	= $variantdir."SNP_variants.vcf";
	my $indelfilenew	= $self->{'slide_name'}.'.nopd.nobc.indel.vcf';
	my $SNPfilenew	= $self->{'slide_name'}.'.nopd.nobc.snp.vcf';

	if(defined $barcodes->[0]) {
		for my $b (@{$barcodes}) {
			#print STDERR "BARCODE: $b\n";

			$status = 0;

			# add barcode to coverage directory to get barcode-specific data
			my $cdir	= $coveragedir;
			$cdir		.= "$b/";
			$coveragefile	= $cdir."coverageAnalysis.html";
			$covfilenew	= $self->{'slide_name'}.'.nopd.'.$b.'.coverageAnalysis.html';

			my $vdir	= $variantdir;
			$vdir		.= "$b/";
			$indelfile	= $vdir."indel_variants.vcf";
			$SNPfile	= $vdir."SNP_variants.vcf";
			$indelfilenew	= $self->{'slide_name'}.'.nopd.'.$b.'.indel.vcf';
			$SNPfilenew	= $self->{'slide_name'}.'.nopd.'.$b.'.snp.vcf';

			# ingest coverage
			my $id = $self->{'mf'}->create_asset(NAMESPACE => $ns, ASSET => $covfilenew);
			if(! $id) {
				$self->writelog(BODY => "Could not create asset $covfilenew");
				exit($self->EXIT_IMPORT_FAILED());
			}
			$status = $self->{'mf'}->update_asset_file(ID => $id, FILE => $coveragefile);
			$self->writelog(BODY => "Import result: $status, $coveragefile");
		
			# ingest indel
			my $id = $self->{'mf'}->create_asset(NAMESPACE => $ns, ASSET => $indelfilenew);
			if(! $id) {
				$self->writelog(BODY => "Could not create asset $indelfilenew");
				exit($self->EXIT_IMPORT_FAILED());
			}
			$status = $self->{'mf'}->update_asset_file(ID => $id, FILE => $indelfile);
			$self->writelog(BODY => "Import result: $status, $indelfile");
		
			# ingest SNP
			my $id = $self->{'mf'}->create_asset(NAMESPACE => $ns, ASSET => $SNPfilenew);
			if(! $id) {
				$self->writelog(BODY => "Could not create asset $SNPfilenew");
				exit($self->EXIT_IMPORT_FAILED());
			}
			$status = $self->{'mf'}->update_asset_file(ID => $id, FILE => $SNPfile);
			$self->writelog(BODY => "Import result: $status, $SNPfile");
		
			$self->writelog(BODY => "Import complete");
		
			if($status > 0) {
				exit($self->EXIT_IMPORT_FAILED());
			}
		}
	}
	else {
		# ingest coverage
		my $id = $self->{'mf'}->create_asset(NAMESPACE => $ns, ASSET => $covfilenew);
		if(! $id) {
			$self->writelog(BODY => "Could not create asset $covfilenew");
			exit($self->EXIT_IMPORT_FAILED());
		}
		$status = $self->{'mf'}->update_asset_file(ID => $id, FILE => $coveragefile);
		$self->writelog(BODY => "Import result: $status, $coveragefile");
	
		# ingest indel
		my $id = $self->{'mf'}->create_asset(NAMESPACE => $ns, ASSET => $indelfilenew);
		if(! $id) {
			$self->writelog(BODY => "Could not create asset $indelfilenew");
			exit($self->EXIT_IMPORT_FAILED());
		}
		$status = $self->{'mf'}->update_asset_file(ID => $id, FILE => $indelfile);
		$self->writelog(BODY => "Import result: $status, $indelfile");
	
		# ingest SNP
		my $id = $self->{'mf'}->create_asset(NAMESPACE => $ns, ASSET => $SNPfilenew);
		if(! $id) {
			$self->writelog(BODY => "Could not create asset $SNPfilenew");
			exit($self->EXIT_IMPORT_FAILED());
		}
		$status = $self->{'mf'}->update_asset_file(ID => $id, FILE => $SNPfile);
		$self->writelog(BODY => "Import result: $status, $SNPfile");
	
		$self->writelog(BODY => "Import complete");
	
		if($status > 0) {
			exit($self->EXIT_IMPORT_FAILED());
		}
	}

	return();
}

################################################################################
=pod

B<ingest_analysis()> 
 Ingest whole analysis directory (without the big ingested files)

Parameters:

Requires:
 $self->{'plugin_out'}

Returns:
 void

=cut
sub ingest_analysis {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $status = 2;

	$self->writelog(BODY => "---Ingesting analysis dir ---");

	# get current working directory
	my $cwd = getcwd;
	# chdir to folder to tar there so tarball won't have the full path
	chdir $self->{'analysis_folder'};
	# tar up files excluding big ones that we don't want
	my $tarfile	= "/results/analysis/output/Home/".$self->{'slide_name'}."_analysis_folder.tar";
	my $cmd		= qq{tar -cf $tarfile --ignore-failed-read --exclude=*.wells --exclude=unfiltered --exclude=*bam* };
	$cmd		.= qq{ --exclude=*fastq --exclude=*sff --exclude=*zip --exclude=plugin* };
	$cmd		.= qq{ --exclude=bc_files --exclude=*sam ./*};
	$status		= system($cmd);
	# change back to working dir
	chdir $cwd;

	# fail if tar failed
	$self->writelog(BODY => "Tar exit code: $status : $cmd");
	if($status > 0) {	
		$self->writelog(BODY => "Tar archiving failed");
		exit($self->EXIT_IMPORT_FAILED());
	}
	
	my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};

	my $assetres	= $self->asset_name_res();	# seq_results

	my $cmd = $self->{'mf'}->laimport_file(NAMESPACE => $ns, ASSET => $assetres, FILE => $tarfile, CMD => 1);
	$self->writelog(BODY => "Ingest analysis folder : $cmd");
	$status = $self->{'mf'}->laimport_file(NAMESPACE => $ns, ASSET => $assetres, FILE => $tarfile);
	# returns asset id as status if successful
	$self->writelog(BODY => "Import result: $status");
	$status = 0 if($status > 0);

	unlink($tarfile);

	$self->writelog(BODY => "Import result: $status, $self->{'analysis_folder'}");

	$self->writelog(BODY => "Import complete");

	if($status > 0) {
		exit($self->EXIT_IMPORT_FAILED());
	}

	return();
}

################################################################################
=pod

B<delete_symlinks()> 
  Deletes all symbolic links from the $RUN_FOLDER.  Java doesn't
  play nicely with symlinks so that's why we're cleaning them up.  

Parameters:

Requires:
 $self->{'analysis_folder'}

Returns:
 scalar: number of symlinks found; scalar: number of symlinks deleted

=cut
sub delete_symlinks {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";


        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Deleting symlinks---\n");
	$self->writelog(BODY => "START: ".$self->timestamp());

	# will find links in all subdirs as well
	my $rf		= $self->{'analysis_folder'};

	opendir(DH, $rf) || die "$!";
	my @files	= readdir(DH);
	closedir(DH);

	# remove . and ..
	@files = @files[2..$#files];

	# keep track of symlinks that are found
	my %symlinks;
	my %linkedfiles;

	# remove symlinks; keep track of return values; then check number of
	# successful deletes with number of symlinks
	# 1 = success
	# 0 = fail
	my $sum = 0;	# number of successfully deleted symlinks

	my $finder = QCMG::FileDir::Finder->new( verbose => 0 );
	my @files = $finder->find_symlinks($rf);
	undef $finder;

	foreach my $sl (@files) {
		# get real file name => value
		my $rl				= readlink($sl);

 		my ($volume,$dirs,$file)	= File::Spec->splitpath($sl);
		$linkedfiles{$sl}		= File::Spec->rel2abs($rl, $dirs);
	}

	# write recreate commands to a shell script
	$self->recreate_symlinks(SYMLINKS => \%linkedfiles);

	# then delete the links
	$self->writelog(BODY => "Deleting symlinks from analysis folder...\n");
	foreach my $sl (@files) {
		$symlinks{$sl}			= unlink($sl);
		$sum				+= $symlinks{$sl};
	}

	# print a log of the symlinks to remove
	my $msg = "symlinks and the unlink return value (0 = fail)\n";
	foreach (keys %symlinks) {
		$msg .= "$_\t$symlinks{$_}\n";
	}	

	$self->writelog(BODY => $msg);
	$self->writelog(BODY => "END: ".$self->timestamp());

	return(scalar(keys %symlinks), $sum);
}

################################################################################
=pod

B<recreate_symlinks()> 
 Reads a list of symlink names and filenames and prints a script that allows
 these to be recreated. 

Parameters:
 SYMLINKS - hash of symlink names as keys, filenames as values

Returns:

=cut
sub recreate_symlinks {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	#my $file = $self->{'run_folder'}."symlink.sh";
	my $file = $self->{'analysis_folder'}.'/'.$self->SYMLINK_FILE;
	$self->{'symlink_script'}	= $file;

	$self->writelog(BODY => "Writing symlink creation commands to $file\n");

	open(SL, ">$file") || warn "Cannot write symlink recreation to $file: $!\n";

	print SL qq{#!/bin/sh\n};

	foreach my $k (keys %{$options->{'SYMLINKS'}}) {
		my $cmd = qq{ln -s $options->{'SYMLINKS'}->{$k} $k\n};

		print SL $cmd; 

		#$self->writelog(BODY => $cmd);
	}
	close(SL);

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
	$msg		.= qq{TOOLLOG: ANALYSIS_FOLDER $self->{'analysis_folder'}\n};
	$msg		.= qq{TOOLLOG: LIMS_PROCESS_ID $self->{'glpid'}\n};
	$msg		.= qq{TOOLLOG: DEFAULT_EMAIL }.$self->DEFAULT_EMAIL."\n";

	$self->writelog(BODY => $msg);

	$self->writelog(BODY => "Beginning ingestion process at $date");

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

	# don't try to set the flag if a LIMS entry doesn't exist
	if(! $self->{'glpid'}) {
		return();
	}

	my $flag	= 0;
	$flag		= 1 if($options->{'EXIT'} > 0);

	my ($data, $slide)	= $self->{'gl'}->torrent_run_by_process($self->{'glpid'});
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

B<DESTROY()> 

 Destructor; write logging information to log file

Parameters:

Returns:
 void

=cut
sub DESTROY {
	my $self = shift;

	# override $? with $self->{'EXIT'}

	if($self->{'cwd'}) {
		chdir $self->{'cwd'};
	}

	# recreate symlinks if they have been deleted
	system(qq{ bash $self->{'symlink_script'}}) if($self->{'symlink_script'});

	if(! $self->{'EXIT'}) {
		$self->{'EXIT'} = 0;
	}

	unless($self->{'EXIT'} > 0) {
		if($IMPORT_ATTEMPTED eq 'yes' && $IMPORT_SUCCEEDED eq 'yes') {
			$self->writelog(BODY => "EXECLOG: errorMessage none");

			$self->{'EXIT'} = 0;

			# send success email if no errors
			my $subj	= "Ingestion of $self->{'analysis_folder'} successful";
			my $body	= "See log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
		}
		elsif($IMPORT_ATTEMPTED eq 'no') {
			$self->writelog(BODY => "EXECLOG: errorMessage import not attempted");
	
			$self->{'EXIT'} = 17;
	
			# send success email if no errors
			my $subj	= "Ingestion of $self->{'analysis_folder'} not attempted";
			my $body	= "See log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
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
		my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};
		my $asset	= $self->{'slide_name'}."_ingest_log";
		print STDERR "Ingesting log file\n";
		my $la	= QCMG::Automation::LiveArc->new(VERBOSE => 1);
		my $assetid = $la->asset_exists(NAMESPACE => $ns, ASSET => $asset);
		my $status;
		if(! $assetid) {
			print STDERR "Creating log file asset\n";
			$assetid = $la->create_asset(NAMESPACE => $ns, ASSET => $asset);
		}
		print STDERR "Updating log file\n";
		$status = $la->update_asset_file(ID => $assetid, FILE => $self->{'log_file'});

		print STDERR "$status\n";

		my $subj	= "Ingestion of $self->{'analysis_folder'} successful";
		my $body	= "See log file: ".$self->{'hostname'}.":".$self->{'log_file'};
		$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
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
