package QCMG::Ingest::5500;

##############################################################################
#
#  Module:   QCMG::Ingest::5500.pm
#  Creator:  Lynn Fink
#  Created:  2011-06-14
#
#  This class contains methods for automating the ingest into LiveArc
#  of raw SOLiD 5500 sequencing  data
#
#  $Id: 5500.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
##############################################################################

=pod

=head1 NAME

QCMG::Ingest::5500 -- Common functions for ingesting raw sequencing files 

=head1 SYNOPSIS

Most common use:
 my $qi = QCMG::Ingest::5500->new();

=head1 DESCRIPTION

Contains methods for extracting run information from a raw sequencing run
folder, checking the sequencing files, and ingesting them into LiveArc

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
use Cwd 'realpath';			# for relative -> absolute paths

# special modules
#use PDL::IO::HDF5;			# for reading HDF5 files

# in-house modules
use QCMG::Automation::LiveArc;		# for accessing Mediaflux/LiveArc
use QCMG::FileDir::Finder;		# for finding directories
use QCMG::DB::TrackLite;		# for getting run type
use QCMG::Lifescope::Ini::GenerateFiles;

use QCMG::Automation::Common;
our @ISA = qw(QCMG::Automation::Common);	# inherit Common methods

use vars qw( $SVNID $REVISION $IMPORT_ATTEMPTED $IMPORT_SUCCEEDED);

################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 SLIDE_FOLDER => directory
 LOG_FILE   => path and filename of log file
 UPDATE		=< 1 - update ingest; 0 force ingest from scratch

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

	# set update mode if requested
	$self->{'update'}		= 1 if($options->{'UPDATE'});

	$self->{'slide_folder'}		= $options->{'SLIDE_FOLDER'};
	# SLIDe FOLDER NAME, should have / on the end with absolute path
	$self->{'slide_folder'}		= Cwd::realpath($self->{'slide_folder'})."/";

	$self->{'hostname'}		= `hostname`;
	chomp $self->{'hostname'};

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
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 1;
	return 1;
}
sub EXIT_BAD_PERMS {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'slide_folder'} failed";
	my $body	= "Failed: Bad permissions on $self->{'slide_folder'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 2;
	return 2;
}
sub EXIT_LIVEARC_ERROR {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'slide_folder'} failed";
	my $body	= "Failed: LiveArc error";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 9;
	return 9;
}
sub EXIT_ASSET_EXISTS {
	# should not happen anymore...
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'slide_folder'} failed";
	my $body	= "Failed: LiveArc asset exists";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 12;
	return 12;
}
sub EXIT_BAD_RAWFILES {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'slide_folder'} failed";
	my $body	= "Failed: Raw sequencing files are corrupt";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage  $body");

	$self->{'EXIT'} = 13;
	return 13;
}
sub EXIT_IMPORT_FAILED {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'slide_folder'} failed";
	my $body	= "Failed: LiveArc import failed";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 16;
	return 16;
}
sub EXIT_NO_XSQ {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'slide_folder'} failed";
	my $body	= "Failed: LiveArc import failed";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 26;
	return 26;
}
sub XSQ_EDIT_FAILED {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'slide_folder'} failed";
	my $body	= "Failed: XSQ file was not edited successfully";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 28;
	return 28;
}
sub XSQ_FILE_RENAME_FAILED {
	my $self	= shift @_;
	my $subj	= "Ingestion of $self->{'slide_folder'} failed";
	my $body	= "Failed: XSQ file could not be renamed";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 29;
	return 29;
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

B<lane_folders()>

Return lane folder names

Parameters:

Requires:
 $self->{'slide_folder'}

Returns:
  ref to array of lane folder names

Sets:
 $self->{'lane_folders'}
 $self->{'lanes'}

=cut
sub lane_folders {
	my $self	= shift @_;

	$self->writelog(BODY => "Finding lane folders...");

	my @lane_folders	= ();
	my @lanes		= ();

	#find /data/results/S234088006/S8006_20110718_2c -name "lane*"
	my $cmd		= qq{find $self->{'slide_folder'} -name "lane*"};
	#print STDERR $cmd, "\n";

	# /data/results/S234088006/S8006_20110718_2c/result/lane2
	# /data/results/S234088006/S8006_20110718_2c/result/lane4
	# /data/results/S234088006/S8006_20110718_2c/result/lane5
	my @dirs	= `$cmd`;
	foreach my $d (@dirs) {
		$d	=~ s/\W$//;

		#print STDERR "Found: $d\n";
		# /scratch/5500_ingest/S8006_20110718_2_LMP/result/lane4
		my $pattern = $self->{'slide_folder'}."result/lane";
		if($d =~ /$pattern/) {
			#print STDERR "Keeping $d\n";
			push @lane_folders, $d;

			# simultaneously get list of lane IDs
			$d	=~ /\/(lane)(\d+)/;
			my $l	= join "_", $1, $2;
			push @lanes, $l;
		}
	}

	$self->{'lane_folders'} = \@lane_folders;

	$self->{'lanes'}	= \@lanes;

	$self->writelog(BODY => "done");

	return($self->{'lane_folders'});
}

################################################################################

=pod

B<asset_name()>

Return asset name, as derived from run folder name

Parameters:

Returns:
  scalar asset name

=cut
sub asset_name {
	my $self	= shift @_;

	#$self->{'asset_name'} = $self->{'slide_name'}.$LA_ASSET_SUFFIX;
	$self->{'asset_name'} = $self->{'slide_name'};

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

B<get_barcodes()> 
 Get barcodes included on this slide/lane so we only map those (lifescope by
  default will create BAMs for all barcodes; also this avoids a bug with job
  queuing when the number of RSs (chromosomes) gets large (eg, 80+ with our
  reference genome)

Parameters:
 $self->{slide_name}
 $self->{lanes}

Returns:
 void

Sets:
 $self->{'lane_N'} (for each lane; value = ref to array of barcode strings)

=cut
sub get_barcodes {
        my $self = shift @_;

	# first, get all lanes on slide
	$self->lane_folders();	# set $self->{'lanes'} if not already

	$self->writelog(BODY => "---Getting barcodes for all lanes...---");

	# for Tracklite query
	my $sql 	= qq{ 
			SELECT run_name, run_type, sample_set, project_id, 
				individual_sample_location, barcode
                        FROM tracklite_run
                        WHERE run_name = '$self->{'slide_name'}' 
			AND individual_sample_location = '<LANE>' 
			ORDER BY individual_sample_location, barcode
		};	#'
	#print STDERR "$sql\n";
	my $tl	= QCMG::DB::TrackLite->new();
	$tl->connect;

	#print Dumper $self->{'lanes'};

	# get barcodes for each lane
	foreach (@{$self->{'lanes'}}) {
		my @barcodes		= ();

		my $lanesql	= $sql;
		$lanesql	=~ s/<LANE>/$_/;
		$self->writelog(BODY => "Lane: $_");

		#print STDERR "$lanesql\n";

		my $res	= $tl->query_db(QUERY => $lanesql);
		#print Dumper $res;

		for my $i (0..$#{$res}) {
			push @barcodes, $res->[$i]->{'barcode'};
			$self->writelog(BODY => "$res->[$i]->{'barcode'}");
		}

		# key = lane_N, value = ref to array of barcodes
		$self->{$_}	= \@barcodes;
		#print Dumper $self->{$_};
	}

	$self->writelog(BODY => "---done---");

	return();
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

B<find_xsqs()> 
 Find all XSQ files

Parameters:

Returns:
 sets 'xsq' with ref to hash of filename and file with full path

=cut
sub find_xsqs {
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

	$self->writelog(BODY => "---Finding XSQs---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	# first, get all lanes on slide
	$self->lane_folders();

	my %xsq = ();

	#print Dumper  $self->{'lane_folders'};

	my $fcount = 0;	# count XSQs
	my $dcount = 0; # count lanes
	foreach my $dir (@{$self->{'lane_folders'}}) {
		#print STDERR "Trying lane folder $dir\n";

		$dcount++;
		my $cmd		= qq{find $dir -name "*.xsq"};
		#print STDERR "$cmd\n";
		my @files = `$cmd`;

		#print Dumper @files;

		if(scalar(@files) == 0) {
			$self->writelog(BODY => "No XSQ in $dir");
			exit($self->EXIT_NO_XSQ());
		}

		# files with full path
		foreach my $file (@files) {
			next if($file =~ /\:/);
			$file	=~ s/\W$//;	# remove newline chars ar the end of filename
			$file	=~ s/$dir\///;	# remove path from filename

			# rebuild path
			my $fullpathtofile = $dir."/".$file;	## MAY NEED TO FIX!!

			# maintain filename and full path with filename
			$xsq{$file} = $fullpathtofile;	

			#print STDERR "$file\t$fullpathtofile\n";

			$self->writelog(BODY => "Using $file ($fullpathtofile)");
			$fcount++;
			last;
		}

	}

	if($dcount != $fcount) {
		$self->writelog(BODY => "Missing an XSQ file");
		exit($self->EXIT_NO_XSQ());
	}

	$self->{'xsq'} = \%xsq;

	#print Dumper %xsq;

	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}

################################################################################
=pod

B<get_run_type()> 
 Get run types for each lane from Tracklite 

Parameters:

Returns:
 ref to hash of run types (key = lane, value = run type)

 Sets:
  $self->{'run_types'}->{$lane}

Requires:
 $self->{'slide_name'}

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

################################################################################
=pod

B<format_lanename()> 
 Reformat lane name so they are all consistent; may need to be padded with zero,
  may need to have case changed, may need to be split into multiple names
  "Different models of the 5500 support from 6 to 12 lanes"

Parameters:
 LANE	=> lane name from Tracklite

Returns:
 scalar - new lane name

=cut
sub format_lanename {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $lane	= $options->{'LANE'};

	# lane_2, Lane_2, lane_02
	if($lane =~ /lane/i) {
		$lane		=~ /^lane\_(.+)$/i;
		my $num		= $1;

		# pad with zero if necessary
=cut
		if(length($num) == 1) {
			$num = '0'.$num;
		}
=cut
		# add proper 'lane_' prefix
		$lane	= 'lane_'.$num;
	}
	# 02
	elsif($lane =~ /^0\d$/) {
		# remove padding zero
		$lane =~ s/0//;

		# add proper 'lane_' prefix
		$lane	= 'lane_'.$lane;
	}

	return($lane);
}

################################################################################
=pod

B<generate_ini_files()> 
 Equivalent of making Bioscope INI files; make Lifescope .ls files for each
  lane; creates directory and writes .ls files; also make run.pbs script to
  launch each .ls file

Parameters:
 none

 Requires:
  $self->{'xsq'}

Returns:
 void

=cut
sub generate_ini_files {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Generating .ls and run.pbs files...---");

	$self->rename_namespace();

	# write .ls and run.pbs files to top level of slide folder
	my $ini_root_dir	= $self->{'slide_folder'};
	$self->{'ini_root_dir'}	= $ini_root_dir;

	# usual notification email address
	my $email		= $self->DEFAULT_EMAIL;

	#print Dumper $self->{'xsq'};

	# get barcodes for all lanes
	$self->get_barcodes();

	# create .ls and run.pbs file for each XSQ/lane (do not need full path
	# for this, just the filename)
	foreach my $xsq (keys %{$self->{'xsq'}}) {

		# get file with full path for getting species
		my $fullxsq	= $self->{'xsq'}{$xsq};

		# get species from XSQ file
		$self->writelog(BODY => "Getting species from $xsq");
		#my $cmd		= $self->{'PYTHON_HOME'}."bin/python2.7 ".$self->{'BIN_LOCAL'}.qq{xsq2species.py -f }.$xsq;
		#my $cmd		= $self->{'PYTHON_HOME'}."bin/python2.7 "."/tmp/".qq{xsq2species.py -f }.$fullxsq;
		my $cmd		= $self->{'PYTHON_HOME'}."bin/python2.7 ".$self->{'BIN_LOCAL'}.qq{xsq2species.py -f }.$fullxsq;
		my @species	= `$cmd`;	# multiline output, all lines should be same
		my $species	= shift @species;
		$species	=~ y/A-Z/a-z/;
		chomp $species;
		$self->writelog(BODY => "$species : ".$cmd);
		# must be 'homo sapiens', 'human', 'mus musculus', 'mouse'
		# (case-independent)

		#print STDERR "Generating lifescope files for $xsq\n";
		$xsq		=~ /(lane\_\d+)/;
		my $lane	= $1;
		my $run_type	= $self->{'run_types'}->{$lane};

		# get barcodes for this lane
		my $barcodes	= $self->{$lane};

		#print STDERR "Lane, Run type, Barcodes:\n";
		#print Dumper $lane;
		#print Dumper $run_type;
		#print Dumper $barcodes;

		#print STDERR "SLIDE NAME: $self->{'slide_name_new'}\n";

		foreach (@{$barcodes}) {
			#print STDERR "Generating .ls for $lane, $_\n";
			my $ls	= QCMG::Lifescope::Ini::GenerateFiles->new(
					ini_root_dir	=> $ini_root_dir,
					run_type	=> $run_type,
					slide_name	=> $self->{'slide_name_new'},
					xsqfile		=> $xsq,
					barcode		=> $_,
					species		=> $species,
					email 		=> $email
				);
	
			# .ls file
			my $file	= $ls->generate_ls();
			my $fname	= $ls->ls_filename();
			#$ls->write_file($fname, $file);
			#print STDERR "writing ls $fname\n";
			push @{$self->{'ls_files'}->{$xsq}}, $fname;
			#$self->{'ls_file'}->{$xsq} = $fname;
	
			# run.pbs file
			my $file	= $ls->generate_runpbs();
			my $fname	= $ls->runpbs_filename();
			#$ls->write_file($fname, $file);
			#print STDERR "writing run.pbs $fname\n";
			push @{$self->{'runpbs_files'}->{$xsq}}, $fname;
			#$self->{'runpbs_files'}->{$xsq} = $files;
		}
	}

	print Dumper $self->{'ls_files'};
	print Dumper $self->{'runpbs_files'};

	$self->writelog(BODY => "---done---");

	return();
}

################################################################################
=pod

B<slide_name()> 
  Get slide name from slide folder name

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

        return $self->{'slide_name'};
}

################################################################################
=pod

B<check_rawfiles()> 
 Check that XSQ files are not corrupt. (??)
 
Parameters:

Returns:
 void

Requires:
 $self->{'xsq'}

=cut
sub check_rawfiles {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Checking XSQ sequence file integrity---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	foreach my $x (keys %{$self->{'xsq'}}) {
		my $file = $self->{'xsq'}->{$x};

		$self->writelog(BODY => "h5checking ".$file);
		# run h5check on XSQ file
		my $cmd		= $self->{'BIN_H5CHECK'}.qq{h5check $file};
		my $rv = system($cmd);
		#print STDERR "$rv: $cmd\n";
		$self->writelog(BODY => $rv.": ".$cmd);

		# 0 = successful validation
		if($rv != 0) {
			$self->writelog(BODY => "h5check is unhappy");
			exit($self->EXIT_BAD_RAWFILES());
		}
	}

	$self->writelog(BODY => "XSQ files are good");
	$self->writelog(BODY => "END: ".$self->timestamp());

	return;
}

################################################################################
=pod

B<checksum_rawfiles()> 
 Perform checksums on XSQ files 
 
Parameters:

Returns:
 void

=cut

sub checksum_rawfiles {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Performing checksums on raw files---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	foreach my $xsq (keys %{$self->{'xsq'}}) {
		$self->writelog(BODY => "Performing checksums on $xsq");

		my $fullpathtofile = $self->{'xsq'}->{$xsq};

		my ($crc, $fsize, $fname) = $self->checksum(FILE => $fullpathtofile);
		$self->writelog(BODY => "Checksum on $xsq: $crc, $fsize");
		#print STDERR "Checksum on $xsq: $crc, $fsize\n";

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

B<edit_xsq_runname()> 
 Rename RunMetadata/RunName attribute so it matches our naming convention:
 e.g.: S8006_20110607_2 -> S8006_20110607_2_LMP
 
Parameters:

Returns:
 void

Requires:
 $self->{'BIN_LOCAL'}

=cut
sub edit_xsq_runname {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Editing XSQ files to change RunName attribute---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	foreach my $x (keys %{$self->{'xsq'}}) {
		my $file	= $self->{'xsq'}->{$x};

		$self->writelog(BODY => "Editing ".$file);
		my $cmd		= $self->{'PYTHON_HOME'}."bin/python2.7 ".$self->{'BIN_LOCAL'}.qq{runnamexsq.py -n }.$self->{'slide_name_new'}." ".$self->{'slide'}.qq{ $file};
		my $rv = system($cmd);
		#print STDERR "$rv: $cmd\n";
		$self->writelog(BODY => $rv.": ".$cmd);

		# 0 = successful validation
		if($rv != 0) {
			$self->writelog(BODY => "XSQ edit failed - $rv: $cmd");
			exit($self->XSQ_EDIT_FAILED());
		}
	}

	$self->writelog(BODY => "XSQ RunName changed");
	$self->writelog(BODY => "END: ".$self->timestamp());

	return;
}

################################################################################
=pod

B<rename_xsq()> 
 Rename XSQ files so they conform to our naming scheme 
 e.g.: lane1/S8006_20110815_1_1_01.xsq -> S8006_20110815_1_LMP.lane_01.nobc.xsq
 
Parameters:

 Requires:
  $self->{'xsq'}

Returns:
 void

Requires:

Sets:
 $self->{'slide_name_new'}
 $self->{'xsq'} (edits existing hash)

=cut
sub rename_xsq {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	#print STDERR "***Renaming XSQ files to have correct slide name format---\n";
	$self->writelog(BODY => "---Renaming XSQ files to have correct slide name format---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	#/data/results/S234088006/S8006_20110815_1/result/lane1/S8006_20110815_1_01.xsq 
	#-> 
	# /data/results/S234088006/S8006_20110815_1/result/lane1/S8006_20110815_1_LMP.lane_1.nobc.xsq

	# S17001_20111221_1_03.xsq_2012.01.09-02:04:59-EST.xsq

	# $self->{'run_types'}->{$lane}
	# $lane = 'lane_02'

	foreach my $x (keys %{$self->{'xsq'}}) {
		#next if($x =~ /\:/);
		my $f		= $self->{'xsq'}->{$x};
		#print STDERR "XSQ: $f\n";

		my ($v, $d, $file)	= File::Spec->splitpath($f);

		# skip if file already has a valid name
		if($file =~ /^(\w{5,6})\_(\d{8})\_(\d{1})\_(\w+)\.lane\_(\d)\.(.+)\.xsq/) {

			$self->{'slide_name_new'}	= join "_", $1, $2, $3, $4;
			#print STDERR "SLIDE_NAME_NEW ".$self->{'slide_name_new'}."\n";

			#print STDERR "Skipping rename, filename good: $file\n";
			$self->writelog(BODY => "File already named appropriately: $file");
			next;
		}

		# parse XSQ name to get serial number of sequencer, run date,
		# and lane
		$file		=~ /^(\w{5,6})\_(\d{8})\_(\d{1})\_(\d{1,2})/;
		my $serial	= $1;
		my $date	= $2;
		my $flowcell	= $3;
		my $lane	= $4;
		$lane		= $self->format_lanename(LANE => $lane);
		
		# get run type for lane
		my $runtype	= $self->{'run_types'}->{$lane};
		#print Dumper $runtype;

		# rename PE runs to FragPEBC; can leave LMP and Frag types as they are
		if($runtype =~ /PE/) {
			$runtype	= 'FragPEBC';
		}

		# build new file name and file name with full path
		my $newfile	= $serial."_".$date."_".$flowcell."_".$runtype.".".$lane.".nobc.xsq";
		my $newfullpath	= $d.$newfile;

		#print STDERR "XSQ ORIG: $file\n";
		#print STDERR "XSQ NEW:  $newfile\n";

		# set new slide name 
		$self->{'slide_name_new'}	= $serial."_".$date."_".$flowcell."_".$runtype;
		#print STDERR "SLIDE_NAME_NEW ".$self->{'slide_name_new'}."\n";

		# skip if file already has a valid name
		#if($file eq $newfile) {
		#	$self->writelog(BODY => "File already named appropriately: $file");
		#	next;
		#}

		# if new proposed filename does not look like it is supposed to,
		# exit
		# S8006_20110815_1_LMP.lane_1.nobc.xsq
		if($newfile !~ /^(\w{5,6})\_(\d{8})\_(\d{1})\_(\w+)\.lane\_(\d)\.(.+)\.xsq/) {
			$self->writelog(BODY => "XSQ file renaming failed - bad new filename:\n\t$newfile");
			exit($self->XSQ_FILE_RENAME_FAILED());
		}

		$self->writelog(BODY => "Renaming ".$f);
		my $cmd		= qq{mv "$f" $newfullpath};
		my $rv		= system($cmd);	# returns 0 if successful
		#print STDERR "$rv: $cmd\n";

		$self->writelog(BODY => $rv.": ".$cmd);

		# 0 = successful validation
		if($rv != 0) {
			$self->writelog(BODY => "XSQ file renaming failed - $rv: $cmd");
			exit($self->XSQ_FILE_RENAME_FAILED());
		}

		# add new name and path to hash
		$self->{'xsq'}->{$newfile}	= $newfullpath;
		# remove old name from hash
		delete($self->{'xsq'}->{$x});
	}

	foreach my $x (keys %{$self->{'xsq'}}) {
		my $f		= $self->{'xsq'}->{$x};
		$self->writelog(BODY => "XSQ: $x ($f)");
	}

	$self->writelog(BODY => "END: ".$self->timestamp());

	return;
}

################################################################################
=pod

B<rename_namespace()> 
 Rename slide namespace so it conform to our naming scheme and renamed XSQ files
 e.g.: S8006_20110815_1_1 -> S8006_20110815_1_LMP
 
Parameters:

Returns:
 void

Requires:

=cut
sub rename_namespace {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "Renaming slide namespace...");
	$self->writelog(BODY => "START: ".$self->timestamp());

	$self->{'slide_namespace'}	= $self->{'slide_name_new'};

	$self->writelog(BODY => "Renamed $self->{'slide_name'} to $self->{'slide_namespace'}");
	
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

B<ingest()> 
 Make sure Mediaflux/LiveArc is ready to accept ingestion; then perform ingestion.

Parameters:

 Requires:
  $self->{'slide_name_new'}
  $self->{'xsq'}

Returns:
 void

=cut

sub ingest {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Preparing for ingestion... ---");

	my $mf	= QCMG::Automation::LiveArc->new(VERBOSE => 1);

	# CAPTURE LIVEARC STDER TO VARIABLE
	# First, save away STDERR
	my $stderrcap;
	#open SAVEERR, ">&STDERR";
	#close STDERR;
	#open STDERR, ">", \$stderrcap or warn "Cannot save STDERR to scalar in ingest()\n";

	# rename slide to out format for namespace creation - must include
	# generic run type: /QCMG_5500/S17009_20120113_1_LMP
	$self->{'slide_namespace'}	= $self->{'slide_name_new'};
	
	# check if slide name namespace is already in Mediaflux/LiveArc
	my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_namespace'};

	$self->writelog(BODY => "Checking if namespace $ns exists...");
	my $r		= $mf->namespace_exists(NAMESPACE => $ns);
	$self->writelog(BODY => "RV: $r");

	# if no namespace, create it
	if($r == 1) {
		$self->writelog(BODY => "Creating $ns");
	
		my $cmd			= $mf->create_namespace(NAMESPACE => $ns, CMD => 1);
		$self->writelog(BODY => $cmd);

		my $r			= $mf->create_namespace(NAMESPACE => $ns);
		$self->writelog(BODY => "RV: $r");

		# fail if ns cannot be created
		if($r =~ /false/) {
			exit($self->EXIT_LIVEARC_ERROR());
		}
	}
	elsif($r == 0) {
		$self->writelog(BODY => "Namespace $ns exists");
	}

	### PERFORM INGESTION
	$IMPORT_ATTEMPTED = 'yes';
	my $date = $self->timestamp();
	$self->writelog(BODY => "Ingest import started $date");

	# broadcast and record status
	my $subj	= $self->{'slide_name'}." RAW INGESTION -- INITIATED";
	my $body	= "RAW INGESTION of ".$ns." initiated.";
	$body		.= "\nCheck log file in ".$self->{'log_file'};

	my $status;
	foreach my $x (keys %{$self->{'xsq'}}) {
		# initialize error status and number of attempted imports
		$status		= 2;
		my $attempts	= 0;
		my $maxattempts	= 4;

		$self->writelog(BODY => "Checking if asset $x exists...");
		my $r			= $mf->asset_exists(NAMESPACE => $ns, ASSET => $x);
		$self->writelog(BODY => "Result: $r");
		if($r) {
			$self->writelog(BODY => "XSQ (seq_raw) asset $x exists");
			if($self->{'update'})  {
				$self->writelog(BODY => "XSQ (seq_raw) asset $x exists; in update mode, skipping this file");
				next;
			}
			else {
				#exit($self->EXIT_ASSET_EXISTS());
				#next;
			}
		}
		$self->writelog(BODY => "Done checking XSQ asset $x");

		$self->writelog(BODY => "Preparing to create metadata, if any...");
		# create metadata -> for each XSQ - here, then ingest here too
		my $meta;
		my $fullxsq	= $self->{'xsq'}->{$x};
		if($self->{'ck_filesize'}->{$fullxsq}) {
			$meta = qq{:meta < :checksums < :source orig };
			$meta .= qq{ :checksum < :chksum };
			$meta .= $self->{'ck_checksum'}->{$fullxsq}.' ';
			$meta .= qq{ :filesize };
			$meta .= $self->{'ck_filesize'}->{$fullxsq}.' ';
			$meta .= qq{ :filename $fullxsq > };
			$meta .= qq{ > >};
		}
		else {
			$self->writelog(BODY => "Skipping LiveArc metadata generation...");
		}
		$self->writelog(BODY => "Done with metadata");
		#print STDERR "META: $meta\n";


		# import each .ls and run.pbs file for each barcode, if a lane
		# has barcodes
		# array has full path and filenames -> need to split to get
		# filename alone
		#$self->{'ls_files'}->{$xsq}	= $files;
		#print Dumper $self->{'ls_files'}->{$x}, "\n";
		$self->writelog(BODY => "*** Importing .ls files(s) for $x");
		foreach (@{$self->{'ls_files'}->{$x}}) {
			# status starts at 2, will be set to 0 on success, 1 on fail; try 3
			# times, then give up
			#my $ls_asset	= $self->{'ls_file'}->{$x};
			my $ls_asset	= $_;
			my $ls_file	= $self->{'slide_folder'}."/".$ls_asset;

			#my ($v, $d, $ls_asset)	= File::Spec->splitpath($_);
			#my $ls_file		= $_;
			until($status == 0 || $attempts > $maxattempts) {
				my $cmd = $mf->laimport_file(NAMESPACE => $ns, ASSET => $ls_asset, FILE => $ls_file, CMD => 1);
		
				my $assetid	= $mf->laimport_file(NAMESPACE => $ns, ASSET => $ls_asset, FILE => $ls_file);
				$self->writelog(BODY => "Import result (asset version): $status");
	
				if($assetid > 0) {
					$status	= 0;
					$self->writelog(BODY => "Import result (status): $status");
				}
		
				$attempts++;
		
				sleep(60) if($status != 0);
			}
			if($status != 0) {
				$self->writelog(BODY => ".ls file ingestion failed");
				#$self->send_email(SUBJECT => $self->{'slide_namespace'}." .ls ingestion problem", BODY => "");
			}
			$status		= 2;
		}

		$self->writelog(BODY => "*** Importing run.pbs file(s) for $x");
		$attempts = 0;
		foreach (@{$self->{'runpbs_files'}->{$x}}) {
			# status starts at 2, will be set to 0 on success, 1 on fail; try 3
			# times, then give up
			#my $rp_asset	= $self->{'runpbs_file'}->{$x};
			my $rp_asset	= $_;
			my $rp_file	= $self->{'slide_folder'}."/".$rp_asset;
			#my ($v, $d, $rp_asset)	= File::Spec->splitpath($_);
			#my $rp_file		= $_;
			until($status == 0 || $attempts > $maxattempts) {
				my $cmd = $mf->laimport_file(NAMESPACE => $ns, ASSET => $rp_asset, FILE => $rp_file, CMD => 1);
		
				my $assetid	= $mf->laimport_file(NAMESPACE => $ns, ASSET => $rp_asset, FILE => $rp_file);
				$self->writelog(BODY => "Import result (asset version): $status");
	
				if($assetid > 0) {
					$status	= 0;
					$self->writelog(BODY => "Import result (status): $status");
				}
		
				$attempts++;
		
				sleep(60) if($status != 0);
			}
			if($status != 0) {
				$self->writelog(BODY => "run.pbs file ingestion failed");
				#$self->send_email(SUBJECT => $self->{'slide_namespace'}." run.pbs ingestion problem", BODY => "");
			}
			$status		= 2;
		}


		$self->writelog(BODY => "*** Importing .xsq file: $x");
		$attempts = 0;
		until($status == 0 || $attempts > $maxattempts) {
			my $cmd = $mf->laimport_file(NAMESPACE => $ns, ASSET => $x, FILE => $self->{'xsq'}->{$x}, CMD => 1);
			$self->writelog(BODY => "$cmd");

			my $assetid	= $mf->laimport_file(NAMESPACE => $ns, ASSET => $x, FILE => $self->{'xsq'}->{$x});
			$self->writelog(BODY => "Import result (asset version): $status");

			if($assetid > 0) {
				$status	= 0;
				$self->writelog(BODY => "Import result (status): $status");
			}
	
			$attempts++;
	
			sleep(60) if($status != 0);
		}
		if($status != 0) {
			$self->writelog(BODY => ".xsq file ingestion failed");
			exit($self->EXIT_IMPORT_FAILED());
		}

		$self->writelog(BODY => "Updating metadata...");
		$self->writelog(BODY => "$meta");
		# set metadata if import was successful
		if($status == 0 && $meta) {
			my $r = $mf->update_asset(NAMESPACE => $ns, ASSET => $x, DATA => $meta);
			$self->writelog(BODY => "Update metadata result: $r");
		}
	}

	$self->writelog(BODY => "Import complete");

	# Now close and restore STDERR to original condition.
	#close STDERR;
	#open STDERR, ">&SAVEERR";
	#$self->writelog(BODY => "$stderrcap");

	if($status != 0) {
		exit($self->EXIT_IMPORT_FAILED());
	}
	else {
		$IMPORT_SUCCEEDED = 'yes';
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
			my $subj	= "Re: ".$self->{'slide_namespace'}." RAW INGESTION -- SUCCEEDED";
			my $body	= "RAW INGESTION of $self->{'slide_namespace'} successful\n";
			$body		.= "See log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			#$self->send_email(SUBJECT => $subj, BODY => $body);
		}
		elsif($IMPORT_ATTEMPTED eq 'no') {
			$self->writelog(BODY => "EXECLOG: errorMessage import not attempted");
	
			$self->{'EXIT'} = 17;
	
			# send success email if no errors
			my $subj	= "Re: ".$self->{'slide_namespace'}." RAW INGESTION -- NOT ATTEMPTED";
			my $body	= "RAW INGESTION of $self->{'slide_namespace'} not attempted\n";
			$body		.= "See log file: ".$self->{'hostname'}.":".$self->{'log_file'};

	
			#$self->send_email(SUBJECT => $subj, BODY => $body);
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
		my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_namespace'};

		#print STDERR "Logfile NS: $ns\n";

		# rename log file to have new name
		my ($v, $d, $file)	= File::Spec->splitpath($self->{'log_file'});
		my $logfilepath		= Cwd::realpath($d)."/";
		my $newlog		= $logfilepath.$self->{'slide_namespace'}.".ingest.log";
		my $mvcmd		= qq{mv $self->{'log_file'} $newlog};
		my $rv			= `$mvcmd`;
		#print STDERR "$rv: $mvcmd\n";

		my $asset	= $self->{'slide_namespace'}."_ingest_log";
		#print STDERR "Ingesting log file $newlog into $ns\n";
		my $la	= QCMG::Automation::LiveArc->new(VERBOSE => 1);
		my $assetid = $la->asset_exists(NAMESPACE => $ns, ASSET => $asset);

		my $status;
		if(! $assetid) {
			#print STDERR "Creating log file asset\n";
			$assetid = $la->create_asset(NAMESPACE => $ns, ASSET => $asset);
		}
		#print STDERR "Updating log file\n";
		$status = $la->update_asset_file(ID => $assetid, FILE => $newlog);

		#print STDERR "Update asset status: $status\n";
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
