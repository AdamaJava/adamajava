package QCMG::Ingest::MappedHiSeq2000;

##############################################################################
#
#  Module:   QCMG::Ingest::MappedHiSeq2000.pm
#  Creator:  Lynn Fink
#  Created:  2011-02-28
#
#  This class contains methods for automating the ingest into LiveArc
#  of mapped sequenced data
#
#  $Id: Mapped.pm 1353 2011-11-11 01:40:50Z l.fink $
#
##############################################################################

=pod

=head1 NAME

QCMG::Ingest::MappedHiSeq2000 -- Common functions for ingesting mapped sequencing files 

=head1 SYNOPSIS

Most common use:
 my $qi = QCMG::Ingest::MappedHiSeq2000->new();

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
use QCMG::Automation::LiveArc;		# for accessing Mediaflux/LiveArc
use QCMG::DB::Metadata;

use vars qw( $SVNID $REVISION $IMPORT_ATTEMPTED $IMPORT_SUCCEEDED );

our @ISA = qw(QCMG::Automation::Common);	# inherit Common methods

################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 SLIDE		=> name of slide to ingest
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

	# name of slide: 120511_SN7001243_0090_BD116HACXX
	$self->{'slide_name'}	= $options->{'SLIDE'};

	# for re-ingesting failed ingests; only ingest assets that don't yet
	# exist ('Y', 'N')
	$self->{'update_ingest'}	= $options->{'UPDATE'};

	print Dumper $self->{'slide_name'};

	# set default
	$self->{'contains_core'}	= 'N';

	# set location of BAMs
	# /panfs/seq_mapped/120511_SN7001243_0090_BD116HACXX
	$self->{'seq_mapped'}	= join '/', $self->SEQ_MAPPED_PATH, $self->{'slide_name'};

	#print Dumper $self->{'seq_mapped'};

	#print STDERR "DEFAULT EMAIL: ".$self->DEFAULT_EMAIL, "\n";
	push @{$self->{'email'}}, $self->DEFAULT_EMAIL;

	# define LOG_FILE
	if($options->{'LOG_FILE'}) {
		$self->{'log_file'}		= $options->{'LOG_FILE'};
	}
	else {
		$self->{'log_file'}		= $self->AUTOMATION_LOG_DIR.$self->{'slide_name'}."_ingestmapped.log";
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

	if(! -e $self->{'seq_mapped'}) {
		exit($self->EXIT_NO_FOLDER());
	}

	# set global flags for checking in DESTROY()  (no/yes)
	$IMPORT_ATTEMPTED = 'no';
	$IMPORT_SUCCEEDED = 'no';

	return $self;
}

# Exit codes:
sub EXIT_NO_FOLDER {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'seq_mapped'} failed";
	$body		.= "\nFailed: No mapset folder: $self->{'seq_mapped'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 1;
	return 1;
}
sub EXIT_BAD_PERMS {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'seq_mapped'} failed";
	$body		.= "\nFailed: Bad permissions on $self->{'seq_mapped'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 2;
	return 2;
}
sub EXIT_LIVEARC_ERROR {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'seq_mapped'} failed";
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
	my $body	= "MAPPED INGESTION of $self->{'seq_mapped'} failed";
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
	my $body	= "MAPPED INGESTION of $self->{'seq_mapped'} failed";
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
	my $body	= "MAPPED INGESTION of $self->{'seq_mapped'} failed";
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
	my $body	= "RAW INGESTION of $self->{'seq_mapped'} failed";
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
	my $body	= "MAPPED INGESTION of $self->{'seq_mapped'} failed";
	$body		.= "\nFailed: Cannot find a BAM file";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 18;
	return 18;
}
sub EXIT_ASSET_NOT_EXISTS {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." MAPPED INGESTION -- FAILED";
	my $body	= "MAPPED INGESTION of $self->{'seq_mapped'} failed";
	$body		.= "\nFailed: Requested LiveArc asset does not exist";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 24;
	return 24;
}

################################################################################
=pod

B<slide_name()>

Return slide name

Parameters:

Requires:
 $self->{'slide_name'}

Returns:
  scalar slide name

=cut
sub slide_name {
	my $self	= shift @_;

	return $self->{'slide_name'};
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

	$self->writelog(BODY => "---Checking ".$self->{'seq_mapped'}." ---");

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                defined($_[($i + 1)]) || 
                        die print STDERR "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	if(	!    $self->{'seq_mapped'} ||
		! -e $self->{'seq_mapped'} || 
		! -d $self->{'seq_mapped'}    ) {

		$self->writelog(BODY => "No valid mapset folder provided\n");
		exit($self->EXIT_NO_FOLDER());
	}

	if(	! -r $self->{'seq_mapped'}) {
		my $msg = " is not a readable directory";
		$self->writelog(BODY => $self->{'seq_mapped'}.$msg);
		exit($self->EXIT_BAD_PERMS());
	}

	$self->writelog(BODY => "Folder exists and is readable.");	

	return($status);
}

################################################################################
=pod

B<rsync_to_surf()> 

 Generate a bash script that rsyncs Core Facility BAM/.metric files to Surf as well
  as a text file that contains the file names to move and move those files

Parameters:

Requires:
 $self->{bam}
 $self->{contains_core}
 $self->{slide_name}
 $self->{lane2project}
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

	# bash script to hold the rsync commands
	my $script	= $self->SEQ_MAPPED_PATH.$self->{'slide_name'}."/".$self->{'slide_name'}."_surf_transfer.sh";
	# text file to list the files to be transferred
	my $filesfrom	= $self->SEQ_MAPPED_PATH.$self->{'slide_name'}."/".$self->{'slide_name'}."_files.txt";
	# text file with list of checksums for the files being transferred
	my $cksum	= $self->SEQ_MAPPED_PATH.$self->{'slide_name'}."/".$self->{'slide_name'}.".bam.cksums";
	# get filename without path for filesfrom file
	my ($v, $d, $c)	= File::Spec->splitpath($cksum);

	# create file to write list of files to; also create checksum file
	open(FH, ">".$filesfrom) 	|| print STDERR "Cannot create $filesfrom for writing\n";
	open(CH, ">".$cksum) 		|| print STDERR "Cannot create $cksum for writing\n";

	print FH $self->{'slide_name'}."\n";
	print FH join "/", $self->{'slide_name'}, $c."\n";

	foreach my $file (@{$self->{'bam'}}) {
		my ($v, $d, $f)	= File::Spec->splitpath($file);

		$f		=~ /(.+?)\.deduped\.bam/;
		my $mapset	= $1;

		my $project	= $self->get_project(MAPSET => $mapset);

		my $metric	= $mapset.".metric";

		if($project =~ /smgcore/) {
			print FH join "/", $self->{'slide_name'}, $f."\n";
			print FH join "/", $self->{'slide_name'}, $metric."\n";

			print CH $f."\t".$self->{'ck_checksum'}->{$f}."\t".$self->{'ck_filesize'}->{$f}."\n";
		}
	}
	close(FH);
	close(CH);

	# create rsync bash script, only if file list file was created and is
	# not empty
	# then run script
	my $nohuplog	= "/panfs/automation_log/".$self->{'slide_name'}.".mapped.rsync.log";
	if(-e $filesfrom && -s $filesfrom > 0) {
		open(FH, ">".$script) || print STDERR "Cannot create $script for writing\n";
		#           rsync -av --chmod=a-rwx,Dg+rxs,u+rw,Fg+r -e "ssh -i /panfs/home/production/.ssh/id_rsa" uploads                               qcmg@surf.genome.at.uq.edu.au:/hox/g/imb-taft/qcmg/tests
		print FH qq{nohup rsync -av --chmod=a-rwx,Dg+rxs,u+rw,Fg+r -e "ssh -i /panfs/home/production/.ssh/id_rsa" --files-from=$filesfrom /mnt/seq_mapped/ qcmg\@surf.genome.at.uq.edu.au:/hox/g/imb-taft/qcmg/ > $nohuplog &\n};
		print FH 'EXIT=$?'."\n";
		print FH "module load QCMGPerl\n\n";
		print FH qq{/share/software/QCMGPerl/distros/automation/sendemail.pl };
		print FH q{-t seq-core@imb.uq.edu.au };
		print FH q{-t l.fink@imb.uq.edu.au };
		print FH qq{-s "Subject: }.$self->{'slide_name'}.qq{ MAPPED DATA surf transfer -- EXIT STATUS \$EXIT" };
		print FH qq{-b "rsync to surf of mapped data for 130809_7001238_0153_AC2ED0ACXX has completed.\n\nLog file:$nohuplog"};
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
			$self->writelog(BODY => "$mapset parent_project: $prj");

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
	my $prj	= $md->project();
	$md	= undef;

	$self->writelog(BODY => "Project: $prj\n");

	return($prj);
}

################################################################################
=pod

B<find_bam()> 
 Find BAM file(s) 
 
Parameters:

Requires:
 $self->{'seq_mapped'}

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

	$self->writelog(BODY => "---Getting list of BAMs from PBS files---");
	# /mnt/seq_raw/120917_SN7001243_0116_BD1AFAACXX/pbs
	# 120917_SN7001243_0116_BD1AFAACXX.lane_1.nobc.sam2bam.pbs
	my $cmd		= "find ".$self->{'seq_raw'}."/".$self->{'slide_name'}."/pbs/ -name \" ".$self->{'slide_name'}.".lane_*.*.sam2bam.pbs\"";
	my @bams	= `$cmd`;

	my %expected_bams;
	foreach (@bams) {
		chomp;
		my $bam			= $_;
		$bam			=~ s/sam2bam\.pbs/bam/;
		$expected_bams{$bam}	= 1;

		$self->writelog(BODY => "Expected BAM: $bam");
	}

	$self->writelog(BODY => "Searching for BAMs in $self->{'seq_mapped'} (*.deduped.bam)...");

	my @files	= `find $self->{'seq_mapped'} -name "*.deduped.bam"`;
	#my $cmd		= qq{find /mnt/seq_results/ -name "}.$self->{'slide_name'}.qq{*.bam"};
	#my @files	= `$cmd`;

	# get all BAM files, only keep sorted BAMs
	my $count	= 0;
	foreach (@files) {
		chomp;
		#$self->writelog(BODY => "BAM: $_");
		next if(/unsorted/i);

		push @{$self->{'bam'}}, $_;
		$self->writelog(BODY => "BAM: $_");

		$expected_bams{$_}	= 2;

		$count++;
	}

	# no BAMs found
	if($count < 1) {
		$self->writelog(BODY => "No valid BAMs found, trying *.bam...");

		my @files	= `find $self->{'seq_mapped'} -name "*.bam"`;
		#my $cmd		= qq{find /mnt/seq_results/ -name "}.$self->{'slide_name'}.qq{*.bam"};
		#my @files	= `$cmd`;
	
		# get all BAM files, only keep sorted BAMs
		my $count	= 0;
		foreach (@files) {
			chomp;
			#$self->writelog(BODY => "BAM: $_");
			next if(/unsorted/i);
	
			push @{$self->{'bam'}}, $_;
			$self->writelog(BODY => "BAM: $_");
	
			$expected_bams{$_}	= 2;
	
			$count++;
		}


		if($count < 1) {
			$self->writelog(BODY => "No valid BAMs found");
			exit($self->EXIT_NO_BAM());
		}
	}

	my $count_expected	= scalar(keys %expected_bams);
	$self->writelog(BODY => "Found $count_expected BAMs and $count mapped BAMs");

	foreach (keys %expected_bams) {
		if($expected_bams{$_}	== 1) {
			$self->writelog(BODY => "BAM missing?");
			exit($self->EXIT_NO_BAM());
		}
	}

	return();
}


################################################################################
=pod

B<rename_bam()> 
 Rename BAM file(s) (replace $self->{'bam'} files with new file name)
 
Parameters:

Requires:
 $self->{'bam'}

Returns:
 void

Sets:
 $self->{'bam'} = array of BAM filenames with full path

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

	$self->writelog(BODY => "---Renaming BAMs---");

	my @newbams		= ();

	foreach (@{$self->{'bam'}}) {
		my $ddbam	= $_;
		my $bam		= $ddbam;
		$bam		=~ s/deduped\.//;

		my $cmd		= qq{mv $ddbam $bam};
		my $rv		= system($cmd);
		$self->writelog(BODY => "RV: $rv - $cmd");

		push @newbams, $bam;
	}

	# replace BAM object with new BAM names
	$self->{'bam'}		= undef;
	$self->{'bam'}		= \@newbams;

	return();
}

################################################################################
=pod

B<find_logs()> 
 Find log files
 
Parameters:

Sets:
 $self->{'logdir'}

Requires:
 $self->{'slide_name'}
 $self->{'seq_mapped'}

Returns:
 void

=cut
sub find_logs {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Searching for log files for $self->{'seq_mapped'}---");

	my $logdir		= join "/", $self->SEQ_RAW_PATH, $self->{'slide_name'}."/log/";
	$self->{'logdir'}	= $logdir;

	$self->writelog(BODY => "Log directory: $logdir");

	unless(-e $logdir) {
		$self->writelog(BODY => "No log files found");
		#exit($self->EXIT_MISSING_METADATA());
	}

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

	$self->writelog(BODY => "---Generating stats reports for BAM(s) in $self->{'seq_mapped'}---");

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
=cut

################################################################################
=pod

B<checksum_bam()> 
 Perform checksum on BAM(s)
 
Parameters:

Requires:
 $self->{'bam'} = array of BAM filenames with full path

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

		my $bam	= $self->filename(FILE => $_);	# no path

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

B<prepare_ingest()> 
 Make sure LiveArc is ready to accept ingestion; then perform ingestion.

Parameters:

Sets:
 $self->{'mf'}
 $self->{'ns'}

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

	# check if slide name namespace is already in LiveArc
	$self->{'ns'}	= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};

	$self->writelog(BODY => "Checking if namespace $self->{'ns'} exists...");
	my $r		= $self->{'mf'}->namespace_exists(NAMESPACE => $self->{'ns'});
	$self->writelog(BODY => "RV: $r");

	# if no namespace, create it
	if($r == 1) {
		$self->writelog(BODY => "Creating $self->{'ns'}");
	
		my $cmd			= $self->{'mf'}->create_namespace(NAMESPACE => $self->{'ns'}, CMD => 1);
		$self->writelog(BODY => $cmd);

		my $r			= $self->{'mf'}->create_namespace(NAMESPACE => $self->{'ns'});
		$self->writelog(BODY => "RV: $r");

		# fail if ns cannot be created
		if($r =~ /false/) {
			exit($self->EXIT_LIVEARC_ERROR());
		}
	}
	elsif($r == 0) {
		$self->writelog(BODY => "Namespace $self->{'ns'} exists");
	}

	# check if slide name mapped namespace is already in LiveArc
	# /QCMG_hiseq/120511_SN7001243_0090_BD116HACXX/120511_SN7001243_0090_BD116HACXX_seq_mapped
	$self->{'ns'}	= join "/", $self->LA_NAMESPACE, $self->{'slide_name'}, $self->{'slide_name'}.$self->LA_ASSET_SUFFIX_MAP;

	$self->writelog(BODY => "Checking if namespace $self->{'ns'} exists...");
	my $r		= $self->{'mf'}->namespace_exists(NAMESPACE => $self->{'ns'});
	$self->writelog(BODY => "RV: $r");

	# if no namespace, create it
	if($r == 1) {
		$self->writelog(BODY => "Creating $self->{'ns'}");
	
		my $cmd			= $self->{'mf'}->create_namespace(NAMESPACE => $self->{'ns'}, CMD => 1);
		$self->writelog(BODY => $cmd);

		my $r			= $self->{'mf'}->create_namespace(NAMESPACE => $self->{'ns'});
		$self->writelog(BODY => "RV: $r");

		# fail if ns cannot be created
		if($r =~ /false/) {
			exit($self->EXIT_LIVEARC_ERROR());
		}
	}
	elsif($r == 0) {
		$self->writelog(BODY => "Namespace $self->{'ns'} exists");
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

B<ingest_bam()> 
 Ingest BAMs  

Parameters:

Requires:
 $self->{'bam'} = array of BAM filenames with full path

Returns:
 void

=cut
sub ingest_bam {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Preparing for BAM ingestion... ---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	### PERFORM INGESTION
	my $date = $self->timestamp();
	$self->writelog(BODY => "BAM ingest started $date");

	# ingest each BAM
	my $status	= 2;
	foreach (@{$self->{'bam'}}) {
		# get pathless filename of BAM to use as asset name
 		my $bamasset	= $self->filename(FILE => $_);

		$self->writelog(BODY => "Checking if asset $bamasset exists...");
		my $r			= $self->{'mf'}->asset_exists(NAMESPACE => $self->{'ns'}, ASSET => $bamasset);
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

		my $id = $self->{'mf'}->laimport_file(NAMESPACE => $self->{'ns'}, ASSET => $bamasset, FILE => $_);
		$self->writelog(BODY => "Import result: $id");
	
		$self->writelog(BODY => "Checking if asset $bamasset exists...");
		my $r			= $self->{'mf'}->asset_exists(NAMESPACE => $self->{'ns'}, ASSET => $bamasset);
		# if asset exists, returns ID
		$self->writelog(BODY => "Result: $r");

		$status = 0 unless($r =~ /error/i);
	
		$self->writelog(BODY => "Updating metadata...");
		# set metadata if import was successful
		if($status == 0 && $meta) {
			my $r = $self->{'mf'}->update_asset(NAMESPACE => $self->{'ns'}, ASSET => $bamasset, DATA => $meta);
			$self->writelog(BODY => "Update metadata result: $r");
		}
	}

	$self->writelog(BODY => "Import complete");

	if($status != 0) {
		exit($self->EXIT_IMPORT_FAILED());
	}
	else {
		$IMPORT_SUCCEEDED = 'yes';
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
=cut

################################################################################
=pod

B<logdir()> 
 Return seq_raw/ingest log directory

Parameters:

Requires:
 $self->{'logdir'}

Returns:
 void

=cut
sub logdir {
	my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	return($self->{'logdir'});
}

################################################################################
=pod

B<ingest_logs()> 
 Ingest log files

Parameters:

Returns:
 void

=cut
sub ingest_logs {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $status = 0;

	$self->writelog(BODY => "---Preparing for log file ingestion... ---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	my $asset	= $self->{'slide_name'}."_bwa_log";
	#my $ns		= join "/", $self->{'ns'}, $self->{'slide_name'};

	# initialize error status and number of attempted imports
	$status		= 2;

	### PERFORM INGESTION
	my $date = $self->timestamp();
	$self->writelog(BODY => "bwa log file ingest started $date");

	$self->writelog(BODY => "Importing $asset");
	# status starts at 2, will be set to 0 on success, 1 on fail; try 3
	# times, then give up
	$status = $self->{'mf'}->laimport(	
				NAMESPACE	=> $self->{'ns'},
				ASSET		=> $asset,
				DIR		=> $self->{'logdir'}
			);
	#print STDERR "IMPORT: $status\n";

	$self->writelog(BODY => "RV: $status");

	$self->writelog(BODY => "Import complete");

	if($status != 0) {
		exit($self->EXIT_IMPORT_FAILED());
	}

	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}

################################################################################
=pod

B<ingest_logs()> 
 Ingest log files

Parameters:

Returns:
 void

=cut
sub extract_sample_sheet {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $status = 0;

	$self->writelog(BODY => "---Extracting sample sheet from LiveArc to seq_mapped... ---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	#        :asset -id "58541" -version "2" "120529_SN7001243_0092_AD1321ACXX_SampleSheet.csv"
	my $asset	= $self->{'ns'}."/".$self->{'slide_name'}."_SampleSheet.csv";

	### PERFORM INGESTION
	my $date = $self->timestamp();

	$self->writelog(BODY => "Extracting $asset");
	# status starts at 2, will be set to 0 on success, 1 on fail; try 3
	# times, then give up
	my $status = $self->{'mf'}->get_asset(	
				ASSET		=> $asset,
				PATH		=> $self->SEQ_MAPPED_PATH."/".$self->{'slide_name'}."/".$self->{'slide_name'}."_SampleSheet.csv",
				RAW		=> 1
			);
	#print STDERR "IMPORT: $status\n";

	$self->writelog(BODY => "RV: $status");

	$self->writelog(BODY => "Extract complete");

	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}

################################################################################
=pod

B<rsync_to_surf()> 

 Generate a bash script that rsyncs Core Facility fastq/gz files to Surf as well
  as a text file that contains the file names to move and move those files

  ** These files have to be transferred prior to mapping because the first step
     of mapping is gunzipping the files. The current plan is to transfer them
     and allow mapping to be delayed.

There will be no need to make the folder on surf. rsync can do it. If you make a 
file that contains the slide name, a list of fastq.gz files, the SampleSheet.csv, 
and the cksum file, then you can rsync them with the following (rough) command:

rsync -av --chmod=a-rwx,Dg+rxs,u+rw,Fg+r -e "ssh -i /panfs/home/production/.ssh/id_rsa" uploads qcmg@surf.genome.at.uq.edu.au:/hox/g/imb-taft/qcmg/tests

A sample file list would look like this:
120911_SN7001240_0074_AD1A9AACXX
120911_SN7001240_0074_AD1A9AACXX/SampleSheet.csv
120911_SN7001240_0074_AD1A9AACXX/120911_SN7001240_0074_AD1A9AACXX.cksums
120911_SN7001240_0074_AD1A9AACXX/120911_SN7001240_0074_AD1A9AACXX.lane_1.nobc.1.fastq.gz
120911_SN7001240_0074_AD1A9AACXX/120911_SN7001240_0074_AD1A9AACXX.lane_1.nobc.2.fastq.gz
120911_SN7001240_0074_AD1A9AACXX/120911_SN7001240_0074_AD1A9AACXX.lane_2.nobc.1.fastq.gz

Parameters:

Requires:
 $self->{samplesheet}
 $self->{contains_core}
 $self->{slide_name}
 $self->{lane2project}
 $self->{ck_checksum}
 $self->{ck_filesize}

Returns:

=cut
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

	# print SampleSheet.csv and checksum files to list of files to transfer
	print FH $self->{'slide_name'}."\n";
	print FH join "/", $self->{'slide_name'}, $s."\n";
	print FH join "/", $self->{'slide_name'}, $c."\n";

	# print all FASTQ files to list of files to transfer
	foreach my $fastq (@{$self->{'fastq'}}) {
		my ($v, $d, $f)	= File::Spec->splitpath($fastq);

		# 120910_SN7001238_0075_AC14GNACXX.lane_1.nobc.1.fastq.gz
		$f		=~ /^(.+?)\.\d\.fastq/;
		my $mapset	= $1;

		my $project	= $self->get_project(MAPSET => $mapset);

		if($project =~ /smgcore/) {
			print FH join "/", $self->{'slide_name'}, $f."\n";

			# for each FASTQ file, get checksums and print to
			# checksum file 
			print CH $f."\t".$self->{'ck_checksum'}->{$fastq}."\t".$self->{'ck_filesize'}->{$fastq}."\n";
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
		print FH qq{nohup rsync -av --chmod=a-rwx,Dg+rxs,u+rw,Fg+r -e "ssh -i /panfs/home/production/.ssh/id_rsa" --files-from=$filesfrom /mnt/seq_mapped/ qcmg\@surf.genome.at.uq.edu.au:/hox/g/imb-taft/qcmg/ &};
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
=cut


################################################################################
=pod

B<clean_seq_raw()> 
 Remove seq_raw directory entirely

Parameters:

Sets:

Requires:
  $self->{'slide_name'}

Returns:
 void

=cut
sub clean_seq_raw {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Deleting seq_raw directory... ---");

	my $seq_raw	= $self->SEQ_RAW_PATH."/$self->{'slide_name'}";
	my $seq_mapped	= $self->SEQ_MAPPED_PATH."/$self->{'mapset_name'}";

	my $la		= QCMG::Automation::LiveArc->new();

	# check that all files are in LiveArc first, just to be safe; they
	# should be unzipped if they were mapped, but maybe some core samples
	# won't be mapped so use *fastq* to cover all possibilities
	my $cmd		= qq{ls -1 $seq_raw/*fastq*};
	my @fastq	= `$cmd`;
	# /mnt/seq_raw/120925_SN7001238_0079_AC15E5ACXX/120925_SN7001238_0079_AC15E5ACXX.lane_1.nobc.1.fastq
	foreach (@fastq) {
		chomp;
		my ($v, $d, $f)	= File::Spec->splitpath($_);
		$f		.= ".gz" if($f !~ /gz$/);
		my $meta	= $la->get_asset(	NAMESPACE	=> "QCMG_hiseq/".$self->{'slide_name'},
							ASSET		=> $f );

		unless($meta->{'CONTENT'} =~ /:size/) {
			# if asset is empty, do not delete file; just email
			# warning

			# send success email if no errors
			my $subj	= $self->{'project'}." SEQ_RAW CLEANUP -- FAILED";
			my $body	= "Automated clean of $self->{'slide_name'} did not complete -> FASTQ file missing from LiveArc\n";
			$body		.= "$f";
	
			$self->send_email(SUBJECT => $subj, BODY => $body);
			return();
		}
	}

	# check Casava log file asset
	my $meta	= $la->get_asset(	NAMESPACE	=> "QCMG_hiseq/".$self->{'slide_name'},
						ASSET		=> $self->{'slide_name'}."_casava" );
	unless($meta->{'CONTENT'} =~ /:size/) {
		# if asset is empty, do not delete file; just email
		# warning

		# send success email if no errors
		my $subj	= $self->{'project'}." SEQ_RAW CLEANUP -- FAILED";
		my $body	= "Automated clean of $self->{'slide_name'} did not complete -> CASAVA log missing from LiveArc\n";

		$self->send_email(SUBJECT => $subj, BODY => $body);
		return();
	}

	# change to seq_raw root directory and, if that is successful, delete
	# the directory for the slide
	my $cwd		= getcwd;
	print STDERR "$cwd\n";
	$self->writelog(BODY => "CWD: $cwd");
	my $rv		= chdir $self->SEQ_RAW_PATH;
	my $cwd2		= getcwd;
	print STDERR "$cwd2\n";
	$self->writelog(BODY => "chdir ".$self->SEQ_RAW_PATH." : $rv");
	if($rv == 1) {
		rmtree($self->{'slide_name'}) if(defined $self->{'slide_name'});
		$self->writelog(BODY => "rmtree $self->{'slide_name'} : $rv");
	}
	# change back to the working directory we started in
	chdir $cwd;

	$self->writelog(BODY => "Cleaning completed");

	return();
}

################################################################################
=pod

B<clean_seq_mapped()> 
 Remove seq_mapped directory

Parameters:

Sets:

Requires:
  $self->{'slide_name'}

Returns:
 void

=cut
sub clean_seq_mapped {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	# don't delete in case rsync to surf is still happening...
	return() if($self->{'contains_core'} eq 'Y');



	my $seq_mapped	= $self->SEQ_MAPPED_PATH."/$self->{'mapset_name'}";

	my $la		= QCMG::Automation::LiveArc->new();

	# check that all files are in LiveArc first, just to be safe; they
	# should be unzipped if they were mapped, but maybe some core samples
	# won't be mapped so use *fastq* to cover all possibilities
	my $cmd		= qq{ls -1 $seq_mapped/*bam | grep -v fixrg | grep -v unsorted};
	my @bam	= `$cmd`;
	# /mnt/seq_mapped/120925_SN7001238_0079_AC15E5ACXX/120925_SN7001238_0079_AC15E5ACXX.lane_1.nobc.bam
	foreach (@bam) {
		chomp;
		my ($v, $d, $f)	= File::Spec->splitpath($_);
		my $meta	= $la->get_asset(	NAMESPACE	=> "QCMG_hiseq/".$self->{'slide_name'}."/".$self->{'slide_name'}."_seq_mapped",
							ASSET		=> $f );

		unless($meta->{'CONTENT'} =~ /:size/) {
			# if asset is empty, do not delete file; just email
			# warning

			# send success email if no errors
			my $subj	= $self->{'project'}." SEQ_MAPPED CLEANUP -- FAILED";
			my $body	= "Automated clean of $self->{'slide_name'} did not complete -> BAM file missing from LiveArc\n";
			$body		.= "$f";
	
			$self->send_email(SUBJECT => $subj, BODY => $body);
			return();
		}
	}

	if($IMPORT_ATTEMPTED eq 'yes' && $IMPORT_SUCCEEDED eq 'yes') {
		$self->writelog(BODY => "---Deleting seq_mapped directory... ---");
	
		my $seq_mapped	= $self->SEQ_MAPPED_PATH."/$self->{'slide_name'}";
	
		my $cwd		= getcwd;
		$self->writelog(BODY => "CWD: $cwd");
		# change to seq_mapped directory and, if that is successful AND a solid
		# stats report was successfully made, delete the directory for the
		# mapset
		my $rv		= chdir $self->SEQ_MAPPED_PATH;
		$self->writelog(BODY => "chdir ".$self->SEQ_MAPPED_PATH." : $rv");
		if($rv == 1) {
			rmtree($self->{'slide_name'}) if(defined $self->{'slide_name'});
			$self->writelog(BODY => "rmtree $self->{'slide_name'} : $rv");
		}
		# change back to the working directory we started in
		chdir $cwd;
	
		$self->writelog(BODY => "Cleaning completed");
	}
	else {
		$self->writelog(BODY => "---seq_mapped directory will not be deleted---");
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
	$msg		.= qq{TOOLLOG: MAPPED_FOLDER $self->{'seq_mapped'}\n};
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
		if(	$IMPORT_ATTEMPTED eq 'yes' && $IMPORT_SUCCEEDED eq 'yes') {

			$self->writelog(BODY => "EXECLOG: errorMessage none");

			$self->{'EXIT'} = 0;

			# send success email if no errors
			my $subj	= $self->{'project'}." MAPPED INGESTION -- SUCCEEDED";
			my $body	= "MAPPED INGESTION of $self->{'slide_name'} complete\n";
			$body		.= "See log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			$self->send_email(SUBJECT => $subj, BODY => $body);
		}
		# import was not attempted; script ended for some other
		# reason
		elsif($IMPORT_ATTEMPTED eq 'no') {
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
		my $asset	= $self->{'slide_name'};
		$asset		.= "_ingestmapped_log";

		#print STDERR "NS: $ns\nASSET: $asset\n";

		#print STDERR "Ingesting log file\n";
		my $assetid = $self->{'mf'}->asset_exists(NAMESPACE => $self->{'ns'}, ASSET => $asset);
		my $status;
	
		#print STDERR "$assetid\n";	

		if(! $assetid) {
			#print STDERR "Creating log file asset\n";
			$assetid = $self->{'mf'}->create_asset(NAMESPACE => $self->{'ns'}, ASSET => $asset);
		}
		#print STDERR "Updating log file\n";
		$status = $self->{'mf'}->update_asset_file(ID => $assetid, FILE => $self->{'log_file'});

		#print STDERR "$status\n";
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
