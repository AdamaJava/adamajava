package QCMG::Ingest::Solid;

##############################################################################
#
#  Module:   QCMG::Ingest::Solid.pm
#  Creator:  Lynn Fink
#  Created:  2011-02-28
#
#  This class contains methods for automating the ingest into Mediaflux/LiveArc
#  of raw SOLiD sequencing  data
#
#  $Id: Solid.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
##############################################################################


=pod

=head1 NAME

QCMG::Ingest::Solid -- Common functions for ingesting raw sequencing files 

=head1 SYNOPSIS

Most common use:
 my $qi = QCMG::Ingest::Solid->new();

=head1 DESCRIPTION

Contains methods for extracting run information from a raw sequencing run
folder, checking the sequencing files, and ingesting them into Mediaflux/LiveArc

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

# in-house modules
use QCMG::Automation::LiveArc;		# for accessing Mediaflux/LiveArc
use QCMG::FileDir::Finder;		# for finding directories
use QCMG::FileDir::SeqRawParser;	# for parsing raw sequencing files
use QCMG::DB::TrackLite;		# for generating Bioscope INI files
use QCMG::Bioscope::Ini::GenerateFiles;	# for generating Bioscope INI files
use QCMG::DB::Metadata;

use QCMG::Automation::Common;
our @ISA = qw(QCMG::Automation::Common);	# inherit Common methods

use vars qw( $SVNID $REVISION $IMPORT_ATTEMPTED $IMPORT_SUCCEEDED);

################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 RUN_FOLDER => directory
 LOG_FILE   => path and filename of log file
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

	# if NO_EMAIL => 1, don't send emails
	$self->{'no_email'}		= $options->{'NO_EMAIL'};

	$self->{'run_folder'}		= $options->{'RUN_FOLDER'};

	# RUN FOLDER NAME, should have / on the end with absolute path
	$self->{'run_folder'}		= Cwd::realpath($self->{'run_folder'})."/";

	$self->{'hostname'}		= `hostname`;
	chomp $self->{'hostname'};

	my $slide_name			= $self->slide_name();

	# define LOG_FILE
	if($options->{'LOG_FILE'}) {
		$self->{'log_file'}		= $options->{'LOG_FILE'};
	}
	else {
		# DEFAULT: /run/folder/ingest.log
		$self->{'log_file'}		= $self->{'run_folder'}.$slide_name."_ingest.log";
	}

	# fail if the user does not have LiveArc credentials
	if(! -e $self->LA_CRED_FILE) {
		print STDERR "LiveArc credentials file not found!\n";
		exit($self->EXIT_LIVEARC_ERROR());
	}

	$self->init_log_file();

	push @{$self->{'email'}}, $self->DEFAULT_EMAIL;

	# get current working directory (canonical path)
	#$self{'cwd'}     =  Cwd::realpath("./")."/";

	# set global flags for checking in DESTROY()  (no/yes)
	$IMPORT_ATTEMPTED = 'no';
	$IMPORT_SUCCEEDED = 'no';

	return $self;
}

# Exit codes:
sub EXIT_NO_FOLDER {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW INGESTION -- FAILED";
	my $body	= "RAW INGESTION of $self->{'run_folder'} failed";
	$body		.= "\nFailed: No run folder: $self->{'run_folder'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 1;
	return 1;
}
sub EXIT_BAD_PERMS {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW INGESTION -- FAILED";
	my $body	= "RAW INGESTION of $self->{'run_folder'} failed";
	$body		.= "\nFailed: Bad permissions on $self->{'run_folder'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 2;
	return 2;
}
sub EXIT_NO_PROP_FILE {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW INGESTION -- FAILED";
	my $body	= "RAW INGESTION of $self->{'run_folder'} failed";
	$body		.= "\nFailed: No properties file";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 3;
	return 3;
}
sub EXIT_NO_DEF_FILE {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW INGESTION -- FAILED";
	my $body	= "RAW INGESTION of $self->{'run_folder'} failed";
	$body		.= "\nFailed: No run_definition.txt file";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 4;
	return 4;
}
sub EXIT_NO_CSFASTA {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW INGESTION -- FAILED";
	my $body	= "RAW INGESTION of $self->{'run_folder'} failed";
	$body		.= "\nFailed: No CSFASTA file(s)";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 5;
	return 5;
}
sub EXIT_NO_QUALS {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW INGESTION -- FAILED";
	my $body	= "RAW INGESTION of $self->{'run_folder'} failed";
	$body		.= "\nFailed:  No QUAL file(s)";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 6;
	return 6;
}
sub EXIT_UNKNOWN_RUN_TYPE {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW INGESTION -- FAILED";
	my $body	= "RAW INGESTION of $self->{'run_folder'} failed";
	$body		.= "\nFailed: Cannot determine run type";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 7;
	return 7;
}
sub EXIT_NO_INI_FILE {
	# should not happen anymore...
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW INGESTION -- FAILED";
	my $body	= "RAW INGESTION of $self->{'run_folder'} failed";
	$body		.= "\nFailed: No ini file";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 8;
	return 8;
}
sub EXIT_LIVEARC_ERROR {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW INGESTION -- FAILED";
	my $body	= "RAW INGESTION of $self->{'run_folder'} failed";
	$body		.= "\nFailed: LiveArc error";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 9;
	return 9;
}
sub EXIT_NO_BATCH_INI {
	# should not happen anymore...
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW INGESTION -- FAILED";
	my $body	= "RAW INGESTION of $self->{'run_folder'} failed";
	$body		.= "\nFailed: No batch ini";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 11;
	return 11;
}
sub EXIT_ASSET_EXISTS {
	# should not happen anymore...
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW INGESTION -- FAILED";
	my $body	= "RAW INGESTION of $self->{'run_folder'} failed";
	$body		.= "\nFailed: LiveArc asset exists";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 12;
	return 12;
}
sub EXIT_BAD_RAWFILES {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW INGESTION -- FAILED";
	my $body	= "RAW INGESTION of $self->{'run_folder'} failed";
	$body		.= "\nFailed: Raw sequencing files are corrupt";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage  $body");

	$self->{'EXIT'} = 13;
	return 13;
}
sub EXIT_MISSING_METADATA {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW INGESTION -- FAILED";
	my $body	= "RAW INGESTION of $self->{'run_folder'} failed";
	$body		.= "\nFailed: Cannot find critical metadata";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 14;
	return 14;
}
sub EXIT_NO_PRIMARY {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW INGESTION -- FAILED";
	my $body	= "RAW INGESTION of $self->{'run_folder'} failed";
	$body		.= "\nFailed: Cannot find a primary directory";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 15;
	return 15;
}
sub EXIT_IMPORT_FAILED {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW INGESTION -- FAILED";
	my $body	= "RAW INGESTION of $self->{'run_folder'} failed";
	$body		.= "\nFailed: LiveArc import failed";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 16;
	return 16;
}

# SET exit(17) = import not attempted (but only used in DESTROY())

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

B<asset_name()>

Return asset name, as derived from run folder name

Parameters:

Returns:
  scalar asset name

=cut
sub asset_name {
	my $self	= shift @_;

	#$self->{'asset_name'} = $self->{'slide_name'}.$LA_ASSET_SUFFIX;
	$self->{'asset_name'} = $self->{'slide_name'}.$self->LA_ASSET_SUFFIX_RAW;

	#print STDERR "Asset name: ", $self->{'asset_name'}, "\n";

	return $self->{'asset_name'};
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

	$self->{'ssr_asset_name'} = $self->{'slide_name'}.".solidstats.xml";

	return $self->{'ssr_asset_name'};
}

################################################################################
=pod

B<def_file()> 
 Return metadata filename

Parameters:

Returns:
  scalar: filename

=cut
sub def_file {
        my $self = shift @_;

	my $rf	= $self->{'run_folder'};
	my $rn	= $self->{'slide_name'};

	# build filename from run folder, slide name, and expected file suffix
	my $rd	= $rf.$rn.$self->RUN_DEF_SUFFIX;

	#$self->writelog(BODY => "DEF_FILE\t$rd");

	return($rd);
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
		exit($self->EXIT_BAD_PERMS());
	}

	$self->writelog(BODY => "Folder exists and is read/writable.");	

	return($status);
}

################################################################################
=pod

B<run_primaries()> 
  Get run primary directories from TrackLite

Parameters:

Returns:
 scalar: ref to hash (f3 => <dir>, f5 => <dir>, r3 => <dir>)

=cut
sub run_primaries {
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

	$self->writelog(BODY => "---Finding primary dirs in Tracklite---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	#my $tl = QCMG::DB::TrackLite->new();
	#$tl->connect();

	my $tl = $self->tracklite_connect();
	
	#my $sql = qq{	SELECT barcode, f3_reads, f5_reads, r3_reads
	#		FROM tracklite_run
	#		WHERE run_name = '$self->{'slide_name'}'
	#		GROUP BY barcode
	#		ORDER BY barcode
	#	};#'
	my $sql = qq{	SELECT barcode, f3_reads, f5_reads, r3_reads
			FROM tracklite_run
			WHERE run_name = '$self->{'slide_name'}'
		};#'
	my $res = $tl->query_db(QUERY => $sql);
	undef $tl;

	$self->writelog(BODY => $sql);

	#print Dumper $res;

	# if no primaries in database, exit
	if(! $res) {
		$self->writelog(BODY => $sql);
		exit($self->EXIT_NO_PRIMARY());	
	}

	my $primaries = ();

	for my $i (0..$#{$res}) {
		# for non-barcoded runs
		if(! $res->[$i]->{'barcode'}) {
			$res->[$i]->{'barcode'} = 'nobc';
		}
	
		$primaries->{ $res->[$i]->{'barcode'} }->{'f3_reads'} = $res->[$i]->{'f3_reads'};
		$primaries->{ $res->[$i]->{'barcode'} }->{'f5_reads'} = $res->[$i]->{'f5_reads'};
		$primaries->{ $res->[$i]->{'barcode'} }->{'r3_reads'} = $res->[$i]->{'r3_reads'};
	}

	#print Dumper $primaries;

	foreach my $b (keys %{$primaries}) {
		foreach my $p (keys %{$primaries->{$b}}) {
			# skip non-existent runs
			if($primaries->{$b}->{$p} !~ 'primary') {
				delete $primaries->{$b}->{$p};
				next;
			}

			# make sure the primary directory actually exists
			my $cmd = qq{find }.$self->{'run_folder'}.qq{ -name "*}.$primaries->{$b}->{$p}.qq{*"};
			my $res	= `$cmd`;
			if(! $res) {
				$self->writelog(BODY => "$p $b primary directory ".$primaries->{$b}->{$p}." does not exist");
				exit($self->EXIT_NO_PRIMARY());
			}
	
			my $finder = QCMG::FileDir::Finder->new( verbose => 0 );
			my @files = $finder->find_directory($self->{'run_folder'}, $primaries->{$b}->{$p});

			#print Dumper @files;

			my $fcount = 0;
			foreach (@files) {
				# if file is not symlink and matches
				# primary directory name
				if(! -l $_ && /$primaries->{$b}->{$p}/) {

					my @readdirs = $finder->find_directory($_, "reads");

					#print Dumper @readdirs;

					foreach (@readdirs) {
						# skip
						# results.xx/libraries/unassigned/primary.xx
						# dirs
						next if(/unassigned/);
						next if(/missing/);

						# if it is barcoded run, get the
						# dir in the libraries/ path;
						# otherwise, just get the
						# matching dir
						if((/libraries/ && $b ne 'nobc' && /$b/) || ($b eq 'nobc')) {
							$primaries->{$b}->{$p} = $_;

							$self->writelog(BODY => "Using $_");

							$fcount++;
						}
					}
				} 
			}
			if($fcount > 1) {
				my $msg = "Run primaries warning: $fcount ";
				$msg .= "regular files found";
				$self->writelog(BODY => $msg);
			}
		}

	}

	# fail if no primary dir for ANY of the runs
	my $msg = "Check\t(barcode) / (tag) (primary dir)";
	$self->writelog(BODY => $msg);

	foreach my $b (keys %{$primaries}) {
		foreach my $p (keys %{$primaries->{$b}}) {
			$msg = "\t$b / $p ".$primaries->{$b}->{$p};
			$self->writelog(BODY => $msg);
			if(! $primaries->{$b}->{$p}) {
				$msg = "Warning: $b / $p ";
				$msg .= $primaries->{$b}->{$p};
				$self->writelog(BODY => $msg);
				exit($self->EXIT_NO_PRIMARY());
			}
		}
	}

	$self->{'primaries'} = $primaries;

	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}

################################################################################
=pod

B<run_primaries()> 
  Get run primary directories from Geneus

Parameters:

Returns:
 scalar: ref to hash (f3 => <dir>, f5 => <dir>, r3 => <dir>)

=cut
sub run_primaries {
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

	$self->writelog(BODY => "---Finding primary dirs in Geneus---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	my $md		= QCMG::DB::Metadata->new();
	my $primaries	= ();

	my $slide	= $self->{'slide_name'};

	if($md->find_metadata("slide", $slide)) {
		my $slide_mapsets	=  $md->slide_mapsets($slide);
		foreach my $mapset (@$slide_mapsets) {
			$md->find_metadata("mapset", $mapset);
	
	        	my $d		= $md->mapset_metadata($mapset);

			# get barcode for each mapset
			my $mapset	= $d->{'mapset'};
			$mapset		=~ /S0.+?\..+?\.(.+)/;
			my $barcode	= $1;
			$barcode	= 'nobc' if(! $barcode);

			# get primary directories for each mapset
			my $pri		= $md->solid_primaries($slide);
			# if no primaries, exit
			if(! $pri) {
				$self->writelog(BODY => "No primary directories found in Geneus");
				exit($self->EXIT_NO_PRIMARY());	
			}
			$primaries->{$barcode}->{'r3_reads'}	= $pri->{'primaries'}->{'r3_primary_reads'};
			$primaries->{$barcode}->{'f3_reads'}	= $pri->{'primaries'}->{'f3_primary_reads'};
			$primaries->{$barcode}->{'f5_reads'}	= $pri->{'primaries'}->{'f5_primary_reads'};
		}
	}

	#print Dumper $primaries;

	foreach my $b (keys %{$primaries}) {
		foreach my $p (keys %{$primaries->{$b}}) {
			# skip non-existent runs
			if($primaries->{$b}->{$p} !~ 'primary') {
				delete $primaries->{$b}->{$p};
				next;
			}

			# make sure the primary directory actually exists
			my $cmd = qq{find }.$self->{'run_folder'}.qq{ -name "*}.$primaries->{$b}->{$p}.qq{*"};
			my $res	= `$cmd`;
			if(! $res) {
				$self->writelog(BODY => "$p $b primary directory ".$primaries->{$b}->{$p}." does not exist");
				exit($self->EXIT_NO_PRIMARY());
			}
	
			my $finder = QCMG::FileDir::Finder->new( verbose => 0 );
			my @files = $finder->find_directory($self->{'run_folder'}, $primaries->{$b}->{$p});

			#print Dumper @files;

			my $fcount = 0;
			foreach (@files) {
				# if file is not symlink and matches
				# primary directory name
				if(! -l $_ && /$primaries->{$b}->{$p}/) {

					my @readdirs = $finder->find_directory($_, "reads");

					#print Dumper @readdirs;

					foreach (@readdirs) {
						# skip
						# results.xx/libraries/unassigned/primary.xx
						# dirs
						next if(/unassigned/);
						next if(/missing/);

						# if it is barcoded run, get the
						# dir in the libraries/ path;
						# otherwise, just get the
						# matching dir
						if((/libraries/ && $b ne 'nobc' && /$b/) || ($b eq 'nobc')) {
							$primaries->{$b}->{$p} = $_;

							$self->writelog(BODY => "Using $_");

							$fcount++;
						}
					}
				} 
			}
			if($fcount > 1) {
				my $msg = "Run primaries warning: $fcount ";
				$msg .= "regular files found";
				$self->writelog(BODY => $msg);
			}
		}

	}

	# fail if no primary dir for ANY of the runs
	my $msg = "Check\t(barcode) / (tag) (primary dir)";
	$self->writelog(BODY => $msg);

	foreach my $b (keys %{$primaries}) {
		foreach my $p (keys %{$primaries->{$b}}) {
			$msg = "\t$b / $p ".$primaries->{$b}->{$p};
			$self->writelog(BODY => $msg);
			if(! $primaries->{$b}->{$p}) {
				$msg = "Warning: $b / $p ";
				$msg .= $primaries->{$b}->{$p};
				$self->writelog(BODY => $msg);
				exit($self->EXIT_NO_PRIMARY());
			}
		}
	}

	$self->{'primaries'} = $primaries;

	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}

################################################################################
=pod

B<run_reads()> 
  Get reads from primary directories

Parameters:

Returns:

=cut
sub run_reads {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Finding files with reads---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	#print Dumper $self->{'primaries'};

	if(! $self->{'primaries'}) {
		#print Dumper $self->{'primaries'};
		exit($self->EXIT_NO_PRIMARY());
	}

	foreach my $b (keys %{$self->{'primaries'}}) {
		#print STDERR "B: $b\n";
		foreach my $p (keys %{$self->{'primaries'}->{$b}}) {
			my $finder = QCMG::FileDir::Finder->new( verbose => 0 );
	
			my $pri = $self->{'primaries'}->{$b}->{$p};

			#print STDERR "pri $b: $pri\n";
	
			my @files = $finder->find_file( $pri, "csfasta" );
			#print Dumper @files;
			foreach (@files) {
				if(/csfasta$/) {
					my $file = $_;
	
					$self->{'csfasta'}->{$b}->{$p} = $file;
				}
			}
			
			my @files = $finder->find_file( $pri, "qual" );
			foreach (@files) {
				if(/qual$/) {
					my $file = $_;
	
					$self->{'qual'}->{$b}->{$p} = $file;
				}
			}
		}
	}

	foreach my $b (keys %{$self->{'primaries'}}) {
		if(! $self->{'csfasta'}->{$b}) {
			my $msg = "No csfasta file for barcode: $b";
			$self->writelog(BODY => $msg);
			exit($self->EXIT_NO_CSFASTA());
		}
		if(! $self->{'qual'}->{$b}) {
			my $msg = "No qual file for barcode $b";
			$self->writelog(BODY => $msg);
			exit($self->EXIT_NO_QUALS());
		}
	}

	my $msg;
	# CSFASTA:
	# /Volumes/QCMG/ingest/S0449_20100603_1_Frag/Sample1/results.F1B1/primary.20100610005836273/reads/S0449_20100603_1_Frag_Sample1_F3.csfasta
	foreach my $b (keys %{$self->{'csfasta'}}) {
		foreach my $k (keys %{$self->{'csfasta'}->{$b}}) {
			$msg .= $self->{'csfasta'}->{$b}->{$k}."\n";
			$msg .= $self->{'qual'}->{$b}->{$k}."\n";
		}
	}

	if($msg !~ /\w+/) {
		exit($self->EXIT_NO_PRIMARY());
	}

	$self->writelog(BODY => "$msg");
	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}

################################################################################
=pod

B<check_all_files()> 
 Check that all runs have all files

Parameters:

Returns:

=cut

sub check_all_files {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Checking that all runs have all files---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	my $rt = $self->{'run_type'};

	# FRAGMENT/Frag runs should have f3 files only
	# PAIRED-END/PE runs should have f3, f5, r3 (barcode)
	# MATE-PAIR/LMP runs should have ....?


	# fragment run, no barcode
	#if($rt eq 'FRAGMENT' && $self->{'is_barcoded'} == 1) {
	if($rt eq 'Frag' && $self->{'is_barcoded'} == 1) {
		$self->writelog(BODY => "Fragment run, no barcode");

		# should only have one key => 'none'
		if(scalar(keys %{$self->{'primaries'}}) != 1) {
			my $msg = "Confused: fragment run has multiple primary";
			$msg .= " directories, failing";
			$self->writelog(BODY => $msg);
			exit($self->EXIT_UNKNOWN_RUN_TYPE());
		}

		# should have 'none' as barcode
		#if(! $self->{'primaries'}->{'none'}) {
		if(! $self->{'primaries'}->{'nobc'}) {
			my $msg = "Confused: fragment run apparently has ";
			$msg .= " barcode, failing";
			$self->writelog(BODY => $msg);
			exit($self->EXIT_UNKNOWN_RUN_TYPE());
		}
	
		# should have f3 only runs
		foreach my $b (keys %{$self->{'csfasta'}}) {
			foreach my $k (keys %{$self->{'csfasta'}->{$b}}) {
				unless($self->{'csfasta'}->{$b}->{$k} =~ /csfasta/ && $k =~ /f3/i && $self->{'csfasta'}->{$b}->{$k} =~ /f3/i) { 
					$self->writelog(BODY => "Confused: csfasta is missing a f3 or barcode run, failing");
					exit($self->EXIT_NO_CSFASTA());
				}
				unless($self->{'qual'}->{$b}->{$k} =~ /qual/ && $k =~ /f3/i && $self->{'qual'}->{$b}->{$k} =~ /f3/i) {
					$self->writelog(BODY => "Confused: qual is missing a f3 or barcode run, failing");
					exit($self->EXIT_NO_QUALS());
				}
			}
		}
	}

	# fragment run, with barcodes
	#elsif($rt eq 'FRAGMENT' && $self->{'is_barcoded'} == 0) {
	elsif($rt eq 'Frag' && $self->{'is_barcoded'} == 0) {
		$self->writelog(BODY => "Fragment run, barcoded");

		# should have multiple barcodes
		if(scalar(keys %{$self->{'primaries'}}) == 1) {
			$self->writelog(BODY => "Confused: fragment barcode run has only one primary directory, failing");
			exit($self->EXIT_UNKNOWN_RUN_TYPE());
		}

		# should have barcode
		#if($self->{'primaries'}->{'none'}) {
		if($self->{'primaries'}->{'nobc'}) {
			$self->writelog(BODY => "Confused: fragment run apparently has no barcode, failing");
			exit($self->EXIT_UNKNOWN_RUN_TYPE());
		}

		# should have f3 and r3 runs
		foreach my $b (keys %{$self->{'csfasta'}}) {
			foreach my $k (keys %{$self->{'csfasta'}->{$b}}) {

				unless($self->{'csfasta'}->{$b}->{$k} =~ /csfasta/) {
					$self->writelog(BODY => "Confused: csfasta is missing a f3 or barcode run, failing");
					exit($self->EXIT_NO_CSFASTA());
				}
				unless($self->{'qual'}->{$b}->{$k} =~ /qual/) {
					$self->writelog(BODY => "Confused: qual is missing a f3 or barcode run, failing");
					exit($self->EXIT_NO_QUALS());
				}

				unless(	($k =~ /f3/i && $self->{'csfasta'}->{$b}->{$k} =~ /f3/i) ||
					($k =~ /r3/i && $self->{'csfasta'}->{$b}->{$k} =~ /bc/i) ) {
					$self->writelog(BODY => "Confused: csfasta is missing a f3 or barcode run, failing");
					exit($self->EXIT_NO_CSFASTA());
				}
				unless(	($k =~ /f3/i && $self->{'qual'}->{$b}->{$k} =~ /f3/i) ||
					($k =~ /r3/i && $self->{'qual'}->{$b}->{$k} =~ /bc/i) ) {
					$self->writelog(BODY => "Confused: qual is missing a f3 or barcode run, failing");
					exit($self->EXIT_NO_QUALS());
				}
			}
		}
	}

	# paired-end, no barcode
	#elsif($rt eq 'PAIRED-END' && $self->{'is_barcoded'} == 1) {
	elsif($rt eq 'PE' && $self->{'is_barcoded'} == 1) {
		$self->writelog(BODY => "Paired end run, not barcoded");

		if(scalar(keys %{$self->{'primaries'}}) > 1) {
			$self->writelog(BODY => "Confused: paired-end run has multiple primary directories, failing");
			exit($self->EXIT_UNKNOWN_RUN_TYPE());
		}

		# should have barcodes that are 'none'
		#unless($self->{'primaries'}->{'none'}) {
		unless($self->{'primaries'}->{'nobc'}) {
			$self->writelog(BODY => "Confused: paired-end run apparently has a barcode run, failing");
			exit($self->EXIT_UNKNOWN_RUN_TYPE());
		}
	
		# should have f3 and f5 runs
		foreach my $b (keys %{$self->{'csfasta'}}) {
			foreach my $k (keys %{$self->{'csfasta'}->{$b}}) {
				unless($self->{'csfasta'}->{$b}->{$k} =~ /csfasta/) {
					$self->writelog(BODY => "Confused: csfasta is missing a f3 or barcode run, failing");
					exit($self->EXIT_NO_CSFASTA());
				}
				unless($self->{'qual'}->{$b}->{$k} =~ /qual/) {
					$self->writelog(BODY => "Confused: qual is missing a f3 or barcode run, failing");
					exit($self->EXIT_NO_QUALS());
				}

				unless(	($k =~ /f3/i && $self->{'csfasta'}->{$b}->{$k} =~ /f3/i) ||
					($k =~ /f5/i && $self->{'csfasta'}->{$b}->{$k} =~ /f5/i)  ) {
					$self->writelog(BODY => "Confused: csfasta is missing a f3 or f5 run, failing");
					exit($self->EXIT_NO_CSFASTA());
				}
				unless(	($k =~ /f3/i && $self->{'qual'}->{$b}->{$k} =~ /f3/i) ||
					($k =~ /f5/i && $self->{'qual'}->{$b}->{$k} =~ /f5/i)  ) {
					$self->writelog(BODY => "Confused: qual is missing a f3 or f5 run, failing");
					exit($self->EXIT_NO_QUALS());
				}
			}
		}

	}

	# paired-end, with barcodes
	#elsif($rt eq 'PAIRED-END' && $self->{'is_barcoded'} == 0) {
	elsif($rt eq 'PE' && $self->{'is_barcoded'} == 0) {
		$self->writelog(BODY => "Paired end run, barcoded");

		# should have multiple barcodes
		if(scalar(keys %{$self->{'primaries'}}) == 1) {
			$self->writelog(BODY => "Confused: paired-end run has only one primary directories, failing");
			exit($self->EXIT_UNKNOWN_RUN_TYPE());
		}

		# should have barcodes that are not 'none'
		#if($self->{'primaries'}->{'none'}) {
		if($self->{'primaries'}->{'nobc'}) {
			$self->writelog(BODY => "Confused: paired-end run apparently has a non-barcode run, failing");
			exit($self->EXIT_UNKNOWN_RUN_TYPE());
		}
	
		# should have f3, f5, and r3 runs
		foreach my $b (keys %{$self->{'csfasta'}}) {
			foreach my $k (keys %{$self->{'csfasta'}->{$b}}) {
				unless($self->{'csfasta'}->{$b}->{$k} =~ /csfasta/) {
					$self->writelog(BODY => "Confused: csfasta is missing a f3 or barcode run, failing");
					exit($self->EXIT_NO_CSFASTA());
				}
				unless($self->{'qual'}->{$b}->{$k} =~ /qual/) {
					$self->writelog(BODY => "Confused: qual is missing a f3 or barcode run, failing");
					exit($self->EXIT_NO_QUALS());
				}

				unless(	($k =~ /f3/i && $self->{'csfasta'}->{$b}->{$k} =~ /f3/i) ||
					($k =~ /f5/i && $self->{'csfasta'}->{$b}->{$k} =~ /f5/i) ||
					($k =~ /r3/i && $self->{'csfasta'}->{$b}->{$k} =~ /bc/i) ) {
					$self->writelog(BODY => "Confused: csfasta is missing a f3, f5, or barcode run, failing");
					exit($self->EXIT_NO_CSFASTA());
				}
				unless(	($k =~ /f3/i && $self->{'qual'}->{$b}->{$k} =~ /f3/i) ||
					($k =~ /f5/i && $self->{'qual'}->{$b}->{$k} =~ /f5/i) ||
					($k =~ /r3/i && $self->{'qual'}->{$b}->{$k} =~ /bc/i) ) {
					$self->writelog(BODY => "Confused: qual is missing a f3, f5, or barcode run, failing");
					exit($self->EXIT_NO_QUALS());
				}
			}
		}

	}

	# long mate-pair run
	#elsif($rt eq 'MATE-PAIR') {
	elsif($rt eq 'LMP') {
		$self->writelog(BODY => "Mate-pair run, no barcodes");

		# should have 1 key 'none'
		unless(scalar(keys %{$self->{'primaries'}}) == 1) {
			$self->writelog(BODY => "Confused: mate-pair run appears to have barcodes, failing");
			exit($self->EXIT_UNKNOWN_RUN_TYPE());
		}

		# should not have barcodes
		#unless($self->{'primaries'}->{'none'}) {
		unless($self->{'primaries'}->{'nobc'}) {
			$self->writelog(BODY => "Confused: mate-pair run apparently has a barcode, failing");
			exit($self->EXIT_UNKNOWN_RUN_TYPE());
		}
	
		# should have f3 and r3 runs
		foreach my $b (keys %{$self->{'csfasta'}}) {
			foreach my $k (keys %{$self->{'csfasta'}->{$b}}) {
				unless($self->{'csfasta'}->{$b}->{$k} =~ /csfasta/) {
					$self->writelog(BODY => "Confused: csfasta is missing a f3 or r3 run, failing");
					exit($self->EXIT_NO_CSFASTA());
				}
				unless($self->{'qual'}->{$b}->{$k} =~ /qual/) {
					$self->writelog(BODY => "Confused: qual is missing a f3 or r3 run, failing");
					exit($self->EXIT_NO_QUALS());
				}

				unless(	($k =~ /f3/i && $self->{'csfasta'}->{$b}->{$k} =~ /f3/i) ||
					($k =~ /r3/i && $self->{'csfasta'}->{$b}->{$k} =~ /r3/i) ) {
					$self->writelog(BODY => "Confused: csfasta is missing a f3 or r3 run, failing");
					exit($self->EXIT_NO_CSFASTA());
				}
				unless(	($k =~ /f3/i && $self->{'qual'}->{$b}->{$k} =~ /f3/i) ||
					($k =~ /r3/i && $self->{'qual'}->{$b}->{$k} =~ /r3/i) ) {
					$self->writelog(BODY => "Confused: qual is missing a f3 or r3 run, failing");
					exit($self->EXIT_NO_QUALS());
				}
			}
		}
	}

	$self->writelog(BODY => "Files okay.");
	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
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

	#$self->writelog(BODY => "Slide name:\t".$self->{'slide_name'});

        return $self->{'slide_name'};
}

################################################################################
=pod

B<clean_folder()> 
  Removes everthing from a RUN FOLDER that has been deemed
  unnecessary for downstream analysis

Parameters:

Returns:
  scalar error code or 0 if no problems

=cut

sub clean_folder {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Cleaning up run folder prior to ingest---\n");
	$self->writelog(BODY => "START: ".$self->timestamp());


        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->delete_symlinks();
	$self->delete_colorcalls_dirs();
	$self->delete_jobs_dirs();

	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}

################################################################################
=pod

B<delete_symlinks()> 
  Deletes all symbolic links from the $RUN_FOLDER.  Java doesn't
  play nicely with symlinks so that's why we're cleaning them up.  IN the long
  run, this function should also create a shell script that can be used to re-
  create all of the symlinks that it deleted.  For now, it just logs them.

Parameters:

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

	my $rf		= $self->{'run_folder'};

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
	$self->writelog(BODY => "Deleting symlinks from run folder...\n");
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
	my $file = $self->{'run_folder'}.$self->SYMLINK_FILE;

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

B<delete_colorcalls_dirs()> 
  Deletes any folders from the $RUN_FOLDER that contain
  the word "colorcalls".  These are unnecessary for downstream analysis.

Parameters:

Returns:
 scalar: numer of files deleted

=cut
sub delete_colorcalls_dirs {
	my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Deleting colorcall directories---");
	$self->writelog(BODY => "START: ".$self->timestamp());

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $rf		= $self->{'run_folder'};

	my $finder = QCMG::FileDir::Finder->new( verbose => 0 );
	my @files = $finder->find_directory( $rf, 'colorcall' );
	undef $finder;

	use File::Path qw(rmtree);		# for removing directories

	# sum number of deleted files
	my $deletedfiles = 0;
	foreach (@files) {
		$self->writelog(BODY => "Deleting $_\n");

		# return value = number of deletes (0 = no deletes)
		$deletedfiles += rmtree($_);
	}

	# ZIP THESE FILES AND SAVE THEM?

	$self->writelog(BODY => "Deleted $deletedfiles colorcall dirs and files\n");
	$self->writelog(BODY => "END: ".$self->timestamp());

	return($deletedfiles);
}

################################################################################
=pod

B<delete_jobs_dirs()> 
  Deletes any folders from the $RUN_FOLDER that match "jobs". These cause
  archive bloat.

Parameters:

Returns:
 scalar: numer of files deleted

=cut
sub delete_jobs_dirs {
	my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Deleting jobs directories---");
	$self->writelog(BODY => "START: ".$self->timestamp());

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $rf		= $self->{'run_folder'};

	my $finder = QCMG::FileDir::Finder->new( verbose => 0 );
	my @files = $finder->find_directory( $rf, 'jobs' );
	undef $finder;

	#foreach (@files) {
	#	print "$_\n"
	#}

	use File::Path qw(rmtree);		# for removing directories

	# sum number of deleted files
	my $deletedfiles = 0;
	foreach (@files) {
		$self->writelog(BODY => "Deleting $_\n");

		# return value = number of deletes (0 = no deletes)
		$deletedfiles += rmtree($_);
	}

	# ZIP THESE FILES AND SAVE THEM?

	$self->writelog(BODY => "Deleted $deletedfiles jobs dirs and files\n");
	$self->writelog(BODY => "END: ".$self->timestamp());

	return($deletedfiles);
}

################################################################################
=pod

B<get_metadata()> 
 Look in the RUN_FOLDER for a SLIDE_NAME_run_definition.txt.
 This file contains information regarding the run type, read lengths, etc.
 Then checks to see if the run is a type that we know how to build
 metadata for. 

Parameters:

Returns:
  scalar: run_type, run_mask, run_protocol

=cut
sub get_metadata {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};
	my $status	= 0;	# default return value (0 = success)

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	my $rf	= $self->{'run_folder'};
	my $rn	= $self->{'slide_name'};

	# build filename from run folder, slide name, and expected file suffix
	my $rd	= $self->def_file();

	$self->writelog(BODY => "---Getting run metadata---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	# if no file, return exit value; otherwise, get metadata and return that
	if(! -e $rd) {
		#$status	= $self->EXIT_NO_DEF_FILE();
		$self->writelog(BODY => "No _run_definition.txt file, getting metadata from parsing the run folder");
		$self->_get_metadata_seqraw();
		#return($status);
	}
	else {
		$self->writelog(BODY => "Parsing _run_definition.txt file");
		$self->_get_metadata_rundef();
	}

	# fail if can't find at least run type and tag names
	if(! $self->{'run_type'} || ! $self->{'primerSet'}) {
		$self->EXIT_MISSING_METADATA();
	}

	$self->writelog(BODY => "END: ".$self->timestamp());

	return(	$self->{'run_type'}, $self->{'run_mask'}, $self->{'run_protocol'}, 
		$self->{'primerSet'}, $self->{'barcodes'});
}

################################################################################
=pod

B<_get_metadata_seqraw()> 

 Parse run folder and extract metadata from filenames and paths

Parameters:

Returns:
  scalar: run_type, run_mask, run_protocol

=cut
sub _get_metadata_seqraw {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};
	my $status	= 0;	# default return value (0 = success)

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	my $rf	= $self->{'run_folder'};
	my $rn	= $self->{'slide_name'};

	my $raw = QCMG::FileDir::SeqRawParser->new( verbose => 0 );
	$raw->process( $rf );
	my $report = $raw->report;

	# can't get these from run folder structure
	$self->{'run_mask'}	= "";
	$self->{'run_protocol'}	= "";

	# report will produce Frag, FragBC, FragPEBC, LMP, FragPE
	# Predicted run type: FragBC
	$report			=~ /run type:\s+(.+)/;
	$self->{'run_type'}	= $1;

	# don't fail on run type; it may be a type that hasn't been accounted
	# for yet...
	$self->writelog(BODY => "(Unconfirmed) Run type: ".$self->{'run_type'});

	# parse primerSet lines
	# Tags found:         BC,F3
	# no tag length reported
	$report =~ m/Tags found:\s+(.+)/;
	my $tags;
	if($1) {
		$tags = $1;
	}
	#my @pset = split /,/, $tags;
	#foreach (@pset) {
	#	chomp;
	#	y/[A-Z]/[a-z]/;
	#	$self->{'primerSet'}->{$_} = "";
	#}

	$report =~ m/Tag lengths:\s+(.+)/; 
	my $tags;
	$tags = $1 if($1);
	#print STDERR "TAG LENGTHS: $tags\n";
	my @pset = split /\s/, $tags;
	foreach (@pset) {
		#print STDERR "TAG: $_\n";
		chomp;
		y/[A-Z]/[a-z]/;
		/(.+),(.+)/;
		#print STDERR "1: $1, 2: $2\n";
		$self->{'primerSet'}->{$1} = $2;
	}


	# parse CSFASTA files, only do this if there are barcodes
	my $barcodes = ();
	$self->{'is_barcoded'} = 1;	# default: not barcoded
	$report =~ m/Barcodes found:\s+(.+)/;
	my @bc;
	@bc = split /,/, $1 if($1);
	foreach (@bc) {
		# bcB16_01 => 1
		$barcodes->{$_} = $_, unless(/missing/ || /unassigned/ || /Tags/);

		$self->{'barcodes'} = $barcodes;
		$self->{'is_barcoded'} = 0;

	}

	# parse a CSFASTA file to get the tag lengths
	#my $cshead = `head -6 `;

	# print to log file
	local($,) = "\t";
	my $msg	 = qq{Run type:\t}.$self->{'run_type'}."\n";
	$msg 	.= qq{Run mask:\t}.$self->{'run_mask'}."\n";
	$msg 	.= qq{Run protocol:\t}.$self->{'run_protocol'}."\n";
	$msg 	.= qq{Primer set(s):\n[tag\tlength]\n};
	foreach (keys %{$self->{'primerSet'}}) {
		$msg .= qq{$_\t}.$self->{'primerSet'}->{$_}."\n";
	}
	if($barcodes) {
		$msg	.= qq{Barcodes:\n};
		foreach (keys %{$barcodes}) {
			$msg	.= qq{$_ => $barcodes->{$_}\n};
		}
	}

	$self->writelog(BODY => "---Metadata captured from run folder structure ---\n".$msg);

	return(	$self->{'run_type'}, $self->{'run_mask'}, $self->{'run_protocol'}, 
		$self->{'primerSet'}, $self->{'barcodes'});
}

################################################################################
=pod

B<_get_metadata_rundef()> 
 Look in the RUN_FOLDER for a SLIDE_NAME_run_definition.txt.
 This file contains information regarding the run type, read lengths, etc.
 Then checks to see if the run is a type that we know how to build
 metadata for. 


Parameters:

Returns:
  scalar: run_type, run_mask, run_protocol

=cut
sub _get_metadata_rundef {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};
	my $status	= 0;	# default return value (0 = success)

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	my $rf	= $self->{'run_folder'};
	my $rn	= $self->{'slide_name'};

	# build filename from run folder, slide name, and expected file suffix
	my $rd	= $self->def_file();

	# if no file, return exit value; otherwise, get metadata and return that
	if(! -e $rd) {
		$status	= $self->EXIT_NO_DEF_FILE();
		$self->writelog(BODY => "WARNING: No metadata for this run");
		return($status);
	}

	# read def file
	local($/) = undef;
	open(FH, $rd) || exit($self->EXIT_NO_DEF_FILE());
	my $fc = <FH>;
	close(FH);

	# VALID RUN TYPES: PAIRED-END, FRAGMENT, MATE-PAIR (others?)
	# Tracklite: Frag, LMP, PE (also have IQ, null in Tracklite)
	# parse version line
	$fc =~ /version.+?\n(.+?)\n/s;
	my @d = split /\t/, $1;
	#$self->{'run_type'}	= $d[2];
	my $run_type		= $d[2];
	$self->{'run_mask'}	= $d[6];
	$self->{'run_protocol'}	= $d[7];

	$self->writelog(BODY => "Run type: $run_type");

	# set RUN_TYPE to conform to current Tracklite types
	# should never use FRAGMEMT, etc again in this script
	if($run_type eq 'FRAGMENT') {
		$self->{'run_type'}	= 'Frag';
	}
	elsif($run_type eq 'PAIRED-END') {
		$self->{'run_type'}	= 'PE';
	}
	elsif($run_type eq 'MATE-PAIR') {
		$self->{'run_type'}	= 'LMP';
	}

	unless($self->VALID_RUN_TYPES->{$self->{'run_type'}} == 1) {
		$self->writelog(BODY => "Unknown run type: ".$self->{'run_type'});
		exit($self->EXIT_UNKNOWN_RUN_TYPE());
	}

	# parse primerSet lines
	#my @pset = ($fc =~ /primerSet.+?\n(.+)\nsample/s);
	$fc =~ /primerSet.+?\n(.+)\nsample/s;
	my @pset = split /\n/, $1;
	foreach (@pset) {
		chomp;
		my ($ps, $bl) = split /\t/;
		$ps =~ y/[A-Z]/[a-z]/;
		$self->{'primerSet'}->{$ps} = $bl;
	}

	# parse sampleName lines, only do this if there are barcodes
	my $barcodes = ();
	$self->{'is_barcoded'} = 1;	# default: not barcoded
	if($fc =~ /sample.+?barcodes/) {
		$fc =~ /sample.+?\n(.+?\n)$/s;
		my $line = $1;
		my @sn = split /\n/, $line;
		foreach (@sn) {
			chomp;
			my @data = split /\t/;

			$data[8] =~ s/\"//g;

			#push @barcodes, $data[8];
			# "1" => bcA10_01
			#$barcodes->{$data[8]} = $data[4];
			$barcodes->{$data[4]} = $data[4];

		}

		$self->{'barcodes'} = $barcodes;
		$self->{'is_barcoded'} = 0;

	}

	# print to log file
	local($,) = "\t";
	my $msg	 = qq{Run type:\t}.$self->{'run_type'}."\n";
	$msg 	.= qq{Run mask:\t}.$self->{'run_mask'}."\n";
	$msg 	.= qq{Run protocol:\t}.$self->{'run_protocol'}."\n";
	$msg 	.= qq{Primer set(s):\n[tag\tlength]\n};
	foreach (keys %{$self->{'primerSet'}}) {
		$msg .= qq{$_\t}.$self->{'primerSet'}->{$_}."\n";
	}
	if($barcodes) {
		$msg	.= qq{Barcodes:\n};
		foreach (keys %{$barcodes}) {
			$msg	.= qq{$_ => $barcodes->{$_}\n};
		}
	}

	$self->writelog(BODY => "---Metadata captured from $rd ---\n".$msg);

	return(	$self->{'run_type'}, $self->{'run_mask'}, $self->{'run_protocol'}, 
		$self->{'primerSet'}, $self->{'barcodes'});
}

################################################################################
=pod

B<fix_rawfiles()> 
 Use readsmeddic.py to fix corrupted raw files
 
Parameters:
 PATH	=> path to corrupted files

Returns:
 void

=cut

sub fix_rawfiles {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Fixing corrupted files---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	# FIX - do not hard-code path here...
	my $cmd	= qq{/usr/local/mediaflux/bin/readsmedic.py -e $options->{'PATH'}};
	$self->writelog(BODY => "$cmd");
	my $rv	= `$cmd`;
	$self->writelog(BODY => "RV: $rv");

	$self->writelog(BODY => "END: ".$self->timestamp());

	return;
}

################################################################################
=pod

B<check_rawfiles()> 
 Check that CSFASTA and QUAL files are not corrupt.
 
Parameters:

Returns:
 void

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

	$self->writelog(BODY => "---Checking raw sequence file integrity---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	foreach my $b (keys %{$self->{'csfasta'}}) {
		foreach my $k (keys %{$self->{'csfasta'}->{$b}}) {

			my $status	= $self->check_rawfilereads(
								CSFASTA	=> $self->{'csfasta'}->{$b}->{$k},
								QUAL	=> $self->{'qual'}->{$b}->{$k}
								);

			# if files are bad, try to fix them
			if($status == 1) {
 				my ($v,$d,$f)	= File::Spec->splitpath($self->{'csfasta'}->{$b}->{$k});
				$self->writelog(BODY => "Attempting to fix files in $d");
				$status		= $self->fix_rawfiles(PATH => $d);
				$self->writelog(BODY => "RV: $status");
			}

			# if files are still bad, manual intervention is
			# required so fail and exit here
			if($status == 1) {
				$self->writelog(BODY => "Attempt to fix files failed");
				exit($self->EXIT_BAD_RAWFILES());
			}
		}
	}

	$self->writelog(BODY => "END: ".$self->timestamp());

	return;
}

################################################################################
=pod

B<check_rawfilereads()> 
 Check CSFASTA and QUAL files against each other
 
Parameters:
 CSFASTA	=> csfasta file full path and filename
 QUAL		=> qual file full path and filename

Returns:
 scalar: bool 0 if good, 1 if bad

=cut

sub check_rawfilereads {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "Getting line lengths and checking for nulls...");
	$self->writelog(BODY => "START: ".$self->timestamp());

	my $csfasta	= $options->{'CSFASTA'};
	my $qual	= $options->{'QUAL'};

	my $status	= 0;

	# get path to files in case they need to be fixed ($d)
 	my ($v,$d,$f)	= File::Spec->splitpath($csfasta);

	my $msg = $csfasta."\n ".$qual;
	$self->writelog(BODY => "Checking line count in ".$msg);

	# check the number of lines in each file; should be
	# equal
	# should take ~2 min
	# grep -v "#" S0449_20100603_1_Frag_Sample1_F3.csfasta | wc -l
	my $clines = `grep -v "#" $csfasta | wc -l`;
	my $qlines = `grep -v "#" $qual    | wc -l`;

	chomp ($clines, $qlines);

	$self->writelog(BODY => "$clines ".$csfasta);
	$self->writelog(BODY => "$qlines ".$qual);

	if($clines != $qlines) {
		$self->writelog(BODY => "Line counts do not match: $clines vs $qlines");
		$status	= 1;
	}

	unless($status == 1) {
		# now check for null characters
		# cat testnullchar.qual  | tr '\0' '@' | grep -P '@';
		# takes 5 min on barrine on 35GB csfasta file
		$self->writelog(BODY => "Checking for nulls in ".$msg);

		my $cmd = qq{cat $csfasta | tr '\\0' '\@' | grep -P '\@'};
		my $lines = `$cmd`;
		#print STDERR "CSFASTA null chars: $lines\n$cmd\n";
		if($lines) {
			$self->writelog(BODY => $csfasta." contains null characters");
			$status	= 1;
		}
	}

	unless($status == 1) {
		$self->writelog(BODY => "Checking for nulls in ".$msg);

		my $cmd = qq{cat $qual | tr '\\0' '\@' | grep -P '\@'};
		my $lines = `$cmd`;
		#print STDERR "QUAL null chars: $lines\n";
		if($lines) {
			$self->writelog(BODY => $qual." contains null characters");
			$status	= 1;
		}
	}

	if($status == 1) {
		$self->writelog(BODY => "Files are corrupt");
	}
	elsif($status == 0) {
		$self->writelog(BODY => "Files are good");
	}
	$self->writelog(BODY => "END: ".$self->timestamp());

	return($status);
}

################################################################################
=pod

B<checksum_rawfiles()> 
 Perform checksums on CSFASTA and QUAL files 
 
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

	foreach my $b (keys %{$self->{'csfasta'}}) {
		foreach my $k (keys %{$self->{'csfasta'}->{$b}}) {

			my $msg = $self->{'csfasta'}->{$b}->{$k}."\n ".$self->{'qual'}->{$b}->{$k};

			$self->writelog(BODY => "Performing checksums on ".$msg);

=cut
                    :chksum 3172277343 
                    :filesize 30979912035 
                    :filename "./Sample1/results.F1B1/primary.20100712165604654/reads/S0428_20100706_2_Frag_Sample1_F3.csfasta"
=cut

			my $rf		= $self->{'run_folder'};

			my $file	= $self->{'csfasta'}->{$b}->{$k};
			# remove run folder from beginning of path so file is :
			# Sample1/results.F1B1/primary.20100610005836273/reads/S0449_20100603_1_Frag_Sample1_F3.csfasta
			$file		=~ s/($rf)(.+)/$2/;
			my ($crc, $fsize, $fname) = $self->checksum(FILE => $self->{'csfasta'}->{$b}->{$k});
			$self->writelog(BODY => "Checksum on $file: $crc, $fsize");
			# /Volumes/QCMG/ingest/S0449_20100603_1_Frag/Sample1/ \
			# results.F1B1/primary.20100610005836273/reads/S0449_20100603_1_Frag_Sample1_F3.csfasta

			$self->{'ck_checksum'}->{$file} = $crc;
			$self->{'ck_filesize'}->{$file} = $fsize;

			my $file	= $self->{'qual'}->{$b}->{$k};
			$file		=~ s/($rf)(.+)/$2/;
			my ($crc, $fsize, $fname) = $self->checksum(FILE => $self->{'qual'}->{$b}->{$k});
			$self->writelog(BODY => "Checksum on $file: $crc, $fsize");

			$self->{'ck_checksum'}->{$file} = $crc;
			$self->{'ck_filesize'}->{$file} = $fsize;
		}
	}

	$self->writelog(BODY => "Checksums complete");
	$self->writelog(BODY => "END: ".$self->timestamp());

	return;
}


################################################################################
=pod

B<run_type()> 
 Query a system that contains run definition data (ie TrackLite, Geneous) to
 get run type 

Parameters:

Returns:
  scalar: run_type

=cut

sub run_type {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	if(! $self->{'run_type'}) {
		$self->get_metadata();
	}

	return($self->{'run_type'});


}
################################################################################
=pod

B<run_mask()> 
 Query a system that contains run definition data (ie TrackLite, Geneous) to
 get run mask 

Parameters:

Returns:
  scalar: run_mask

=cut

sub run_mask {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	if(! $self->{'run_mask'}) {
		$self->get_metadata();
	}

	return($self->{'run_mask'});


}
################################################################################
=pod

B<run_protocol()> 
 Query a system that contains run definition data (ie TrackLite, Geneous) to
 get run protocol 

Parameters:

Returns:
  scalar: run_protocol

=cut

sub run_protocol {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	if(! $self->{'run_protocol'}) {
		$self->get_metadata();
	}

	return($self->{'run_protocol'});


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

Returns:
 void

=cut

sub ingest {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $status = 0;

	# SET UP LiveArc FOR THIS INGESTION

	$self->writelog(BODY => "---Preparing for ingestion... ---");

	my $mf	= QCMG::Automation::LiveArc->new(VERBOSE => 1);

	#$self->writelog(BODY => "Logging in to LiveArc...");
	#$mf->login();

	# CAPTURE LIVEARC STDER TO VARIABLE
	# First, save away STDERR
	my $stderrcap;
	open SAVEERR, ">&STDERR";
	close STDERR;
	open STDERR, ">", \$stderrcap or warn "Cannot save STDERR to scalar in ingest()\n";
	
	# check if slide name namespace is already in Mediaflux/LiveArc
	my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};
	my $rawasset	= $self->asset_name();	# _seq_raw asset
	#my $rawassetid	= "";

	$self->writelog(BODY => "Checking if namespace $ns exists...");
	my $r		= $mf->namespace_exists(NAMESPACE => $ns);
	$self->writelog(BODY => "RV: $r");

	#$self->writelog(BODY => "Dealing with namespace...");

	# if no namespace, create it
	if($r == 1) {
		$self->writelog(BODY => "Creating $ns");
	
		my $cmd			= $mf->create_namespace(NAMESPACE => $ns, CMD => 1);
		$self->writelog(BODY => $cmd);

		my $r			= $mf->create_namespace(NAMESPACE => $ns);
	}
	elsif($r == 0) {
		$self->writelog(BODY => "Namespace $ns exists");

		$self->writelog(BODY => "Checking if asset $rawasset exists...");
		my $r			= $mf->asset_exists(NAMESPACE => $ns, ASSET => $rawasset);
		$self->writelog(BODY => "Result: $r");
		if($r) {
			$self->writelog(BODY => "_seq_raw asset $rawasset exists");
			exit($self->EXIT_ASSET_EXISTS());
		}
		$self->writelog(BODY => "Done checking namespaces and assets");
	}

	$self->writelog(BODY => "Preparing to write metadata, if any...");


	# set tags for description of asset in MediaFlux/LiveArc

	# confirmed = orig if data from a sequencing machine; can also be "both"
	# or "livearc" if the checksums are performed after extracting the asset
	# from LiveArc; "both" is only used if the checksum is confirmed to
	# match both before import and after extraction
	# ** only do this if checksums have been created **
=cut
 /usr/bin/java -Dmf.result=shell -Dmf.cfg=/Users/l.fink/.mediaflux/mf_credentials.cfg -jar /Users/l.fink/0_projects/qcmg/bin/aterm.jar --app exec import -namespace test/S0449_20100603_1_Frag -archive 1 -name S0449_20100603_1_Frag_seq_raw /Volumes/QCMG/ingest/S0449_20100603_1_Frag/  :meta ':checksums < :source \"orig\" :checksum < :chksum 1487819556  :filesize 7613  :filename Sample1/results.F1B1/primary.20100610005836273/reads/S0449_20100603_1_Frag_Sample1_F3_QV.qual] >  :checksum < :chksum 149038259  :filesize 3562  :filename Sample1/results.F1B1/primary.20100610005836273/reads/S0449_20100603_1_Frag_Sample1_F3.csfasta] >  > >'
=cut
	my $meta;
	if($self->{'ck_checksum'}) {
		$self->writelog(BODY => "Generating LiveArc metadata...");

		$meta = qq{:meta < :checksums < :source orig };
		foreach my $k (keys %{$self->{'ck_checksum'}}) {
			$meta .= qq{ :checksum < :chksum };
			$meta .= $self->{'ck_checksum'}->{$k}.' ';
			$meta .= qq{ :filesize };
			$meta .= $self->{'ck_filesize'}->{$k}.' ';
			$meta .= qq{ :filename $k > };
		}
		$meta .= qq{ > >};

		#print STDERR "$meta\n";
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

	$IMPORT_ATTEMPTED = 'yes';

	### PERFORM INGESTION
	my $date = $self->timestamp();
	$self->writelog(BODY => "Ingest import started $date");

	# broadcast and record status
	my $subj	= $self->{'slide_name'}." RAW INGESTION -- INITIATED";
	my $body	= "RAW INGESTION of ".$ns.":".$rawasset." initiated.";
	$body		.= "\nCheck log file in ".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);

	# import Bioscope INI files into LiveArc in the same namespace as the
	# data
	#
	# asset = S0449_20100603_1_Frag_bioscope
	my $biasset = $self->{'slide_name'}.$self->LA_ASSET_SUFFIX_BIOSCOPE_INI;
	$self->writelog(BODY => "_bioscope_ini: Importing $biasset to $ns");
	my $rv = $mf->laimport(		NAMESPACE	=> $ns,
					ASSET		=> $biasset,
					DIR		=> $self->{'ini_root_dir'}
				);
	$self->writelog(BODY => "RV: $r");

	# import solid_stats_report into LiveArc in the same namespace as the
	# data
	#
	# asset = S0449_20100603_1_Frag.solidstats.xml
	# file  = /run/folder/S0449_20100603_1_Frag.solidstats.xml
	#my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};
	#my $ssasset	= $self->{'slide_name'}.$self->LA_ASSET_SUFFIX_SOLIDSTATS;
	my $ssasset	= $self->ssr_asset_name();
	$self->writelog(BODY => ".solidstats.xml: Importing $ssasset to $ns");
	my $la	= QCMG::Automation::LiveArc->new(VERBOSE => 1);
	my $ssassetid = $la->asset_exists(NAMESPACE => $ns, ASSET => $ssasset);
	my $status;
	if(! $ssassetid) {
		$self->writelog(BODY => "Creating raw data solid_stats_report asset");
		$ssassetid = $la->create_asset(NAMESPACE => $ns, ASSET => $ssasset);
	}
	$self->writelog(BODY => "Importing file into raw data solid_stats_report asset");
	$status = $la->update_asset_file(ID => $ssassetid, FILE => $self->{'solid_stats_report'});
	$self->writelog(BODY => "RV: $status");
	$status = 2;
	# should check here to make sure ingest worked...

	# import _seq_raw; needs to happen AFTER _bioscope import for proper
	# triggering
	$self->writelog(BODY => "_seq_raw: Importing $rawasset");
	# status starts at 2, will be set to 0 on success, 1 on fail; try 3
	# times, then give up
	until($status == 0 || $attempts > $maxattempts) {
		$status = $mf->laimport(NAMESPACE => $ns, ASSET => $rawasset, RUN_FOLDER => $self->{'run_folder'});
		$self->writelog(BODY => "Import result: $status");

		$attempts++;

		sleep(60) if($status != 0);
	}

	$self->writelog(BODY => "Updating metadata...");
	# set metadata if import was successful
	if($status == 0 && $meta) {
		my $r = $mf->update_asset(NAMESPACE => $ns, ASSET => $rawasset, DATA => $meta);
		$self->writelog(BODY => "Update metadata result: $r");
	}

	$self->writelog(BODY => "Import complete");

	# Now close and restore STDERR to original condition.
	close STDERR;
	open STDERR, ">&SAVEERR";
	$self->writelog(BODY => "$stderrcap");

	if($status != 0) {
		exit($self->EXIT_IMPORT_FAILED());
	}
	else {
		$IMPORT_SUCCEEDED = 'yes';
	}

	return();
}

###############################################################################
=pod

B<update_run_info()> 
 Update a database with ingest information and status

Parameters:

Returns:

=cut

sub update_run_info {
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
=cut
tracklite_run values:
run_type        Frag
run_type        LMP
run_type        PE
run_type
run_type        IQ
=cut

	$self->writelog(BODY => "---Updating TrackLite---");

	my ($f3_reads, $f5_read, $r3_reads, $f3_tag_length, $f5_tag_length,
		$r3_tag_length, $f3_casta, $f5_csfasta, $r3_csfasta, $f3_size, 
		$f5_size, $r3_size, $f3_primary, $f5_primary, $r3_primary);

	my ($slide_name, $run_type, $barcode);
	my (%tag_length, %csfasta, %primary);

	foreach my $b (keys %{$self->{'primaries'}}) {
		$slide_name	= $self->{'slide_name'};
		$barcode	= $b;

		$self->writelog(BODY => "Updating run: $slide_name, barcode: $barcode");

		$run_type	= $self->{'run_type'};

		foreach my $p (keys %{$self->{'primerSet'}}) {
			$tag_length{$p}	= $self->{'primerSet'}->{$p};
		}
	
		foreach my $p (keys %{$self->{'primaries'}->{$b}}) {
			$primary{$p}	= $self->{'primaries'}->{$b}->{$p};

 			my ($volume,$dirs,$file) = File::Spec->splitpath($self->{'csfasta'}->{$b}->{$p});
			$csfasta{$p}	= $file;
		}
	
		my $tl = QCMG::DB::TrackLite->new();
		$tl->connect();


		my $tl = $self->tracklite_connect();
	
		my $rows = $tl->update_ingest_solid(	TAG_LENGTH => \%tag_length,
							CSFASTA => \%csfasta,
							PRIMARY => \%primary,
							RUN_NAME => $slide_name,
							RUN_TYPE => $run_type,
							BARCODE => $barcode
						);

		$rows = sprintf("%i", $rows);

		$self->writelog(BODY => "Updated $rows row(s).");
	}

	return;
}

###############################################################################
=pod

B<write_bioscope_ini_files()> 
 Generate Bioscope ini files to ingest with raw data using Geneus

Parameters:

Returns:

=cut
sub write_bioscope_ini_files {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options		= {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Generating Bioscope INI files---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	# get run ID from Tracklite
	my $slide_name		= $self->{'slide_name'};

	# set email so INI files are written correctly
	$ENV{'QCMG_EMAIL'}	= $self->DEFAULT_EMAIL;
	my $iniscript		= $self->BIN_SEQTOOLS.'generate_bioscope_ini_files.pl ';

	my $md	= QCMG::DB::Metadata->new();

	$self->writelog(BODY => "Connecting to Geneus for $slide_name");

	# directory where bioscope files should be written; create and define
	$self->{'ini_root_dir'}	= $self->{'run_folder'}.$self->LA_ASSET_SUFFIX_BIOSCOPE_INI; 
	# if a ini folder for this date already exists, delete it
	if(-e $self->{'ini_root_dir'} && -d $self->{'ini_root_dir'}) {
		my $cmd = qq{rm -r }.$self->{'ini_root_dir'};
		$self->writelog(BODY => $self->{'ini_root_dir'}." exists, deleting: $cmd");
		my $suffix = $self->LA_ASSET_SUFFIX_BIOSCOPE_INI;
		if($self->{'ini_root_dir'} ne '/' && $self->{'ini_root_dir'} =~ /$suffix/) {
			system($cmd);
		}
	}
	else {
		$self->writelog(BODY => $self->{'ini_root_dir'}." does not exist (destination of INI files)");
	}
	$self->writelog(BODY => "Creating Bioscope INI root dir: ".$self->{'ini_root_dir'});
	my $rv = mkdir $self->{'ini_root_dir'};
	$self->writelog(BODY => "RV: $rv");


	$self->writelog(BODY => "Generating INI files...");
=cut
~/devel/QCMGPerl/distros/seqtools/src/generate_bioscope_ini_files.pl -r S0436_20120626_2_FragPEBC -t fragment -s qcmg-clustermk2 -e l.fink@imb.uq.edu.au -i ./test_ini/ -a 20120718 -q workq -b /share/software/bioscoperoot -p /panfs/imb/ --barcode bcB16_09 --physical_division nopd --f3_primary primary.20120711172452896 --f3_primary_dir bcSample1/blah/ --f3_csfasta SLIDE.F3.csfasta --f3_read_length 50 --f3_qual SLIDE.F3.qual --f5_primary_dir bcSample1/blah/ --f5_read_length 35 --f5_csfasta SLIDE.F5.csfasta --f5_qual SLIDE.F5.qual --primary_lib_id Library_test
=cut

	my $run_date	= $self->timestamp(FORMAT => 'YYMMDD');

	if($md->find_metadata("slide", $slide_name)) {
		my $slide_mapsets	=  $md->slide_mapsets($slide_name);
			foreach my $mapset (@$slide_mapsets) {
			$md->find_metadata("mapset", $mapset);
	
	        	my $d		=  $md->mapset_metadata($mapset);

			my $shortname	= $md->solid4_shortname_mapping($slide_name);	
			# { ip => "10.160.72.13", 	resultsdir => "/data/results/solid0433",	nickname =>	"JubJub"} 
			my $rootdir	= $shortname->{'resultsdir'}.'/'.$slide_name.'/';
	
			my $pridir	= $md->solid_primary_resultsdir($mapset);
			$pridir		=~ s/^.+?$slide_name\///;
	
			my $pri		= $md->solid_primaries($mapset);	

			# make sure that the directory is in results.F1B1; if
			# not, change fix it
			my $cmd		= qq{find $rootdir -name "}.$pri->{'primaries'}->{'f3_primary_reads'}.qq{"};
			my $resdir	= `$cmd`;
			if($resdir	!~ /results\.F1B1/) {
				$resdir	=~ /(results\.\w{4})/;
				my $newres	= $1;
				$pridir	=~ s/results\.F1B1/$newres/g;
			}

			my $run_type	= 'fragment';	# default
			my $seq_type	= $d->{'sequencing_type'};
			my $lib_type	= $d->{'library_type'};
			if($lib_type =~ /LMP/) {
				$run_type	= 'matepair';
			}
			elsif($lib_type	=~ /frag/i && $seq_type =~ /pair/i) {
				$run_type	= 'pairedendBC';
			}
	
			my $mapset	= $d->{'mapset'};
			$mapset		=~ /S0.+?\.(.+?)\.(.+)/;
			my $pd		= $1;
			my $barcode	= $2;

			## BABYJESUS HARD-CODED	
=cut
/usr/local/mediaflux/QCMGPerl/distros/seqtools/src/generate_bioscope_ini_files.pl  
-r S0428_20120712_1_FragPEBC 
-t pairedendBC 
-s qcmg-clustermk2 
-e QCMG-InfoTeam@imb.uq.edu.au 
-i /data/results/solid0428/S0428_20120712_1_FragPEBC/_bioscope_ini 
-a 20120801  
-q QCMG::Ingest::Solid=HASH(0x1034d40)->QUEUE 
-b QCMG::Ingest::Solid=HASH(0x1034d40)->BIOSCOPE_INSTALL 
-p QCMG::Ingest::Solid=HASH(0x1034d40)->PANFS_MOUNT 
--barcode bcB16_12 
--physical_division nopd
--primary_lib_id Library_20120126_G 
--f3_primary primary.20120729091207208 
--f3_primary_dir bcSample1/results.F1B1/libraries/bcB16_12/  
--f3_csfasta 
--f3_read_length 50 
--f3_qual  
--f5_csfasta  
--f5_read_length  
--f5_qual
=cut
			#print Dumper $self->{'csfasta'};
			#print Dumper $self->{'qual'};
			#print Dumper $self->{'primerSet'};
			#print Dumper $self->{'primaries'};

			# remove sequencer-specific path to csfasta/qual files
			$self->{'csfasta'}->{$barcode}->{'f3_reads'}	=~ s/.+\/(primar.+)/$1/;
			$self->{'qual'}->{$barcode}->{'f3_reads'}	=~ s/.+\/(primar.+)/$1/;

			my $cmd	= qq{$iniscript };
			$cmd	.= qq{ -r $slide_name };
			$cmd	.= qq{ -t $run_type };
			$cmd	.= qq{ -s qcmg-clustermk2 };
			$cmd	.= qq{ -e $ENV{'QCMG_EMAIL'} };
			$cmd	.= qq{ -i $self->{'ini_root_dir'} };
			$cmd	.= qq{ -a $run_date };
			#$cmd	.= qq{ -q }.$self->QUEUE;
			$cmd	.= qq{ -q batch };
			$cmd	.= qq{ -b }.$self->BIOSCOPE_INSTALL;
			#$cmd	.= qq{ -p }.$self->PANFS_MOUNT;
			$cmd	.= qq{ -p /panfs/ };
			$cmd	.= qq{ --barcode $barcode };
			$cmd	.= qq{ --physical_division $pd };
			$cmd	.= qq{ --primary_lib_id $d->{'primary_library'} };
			$cmd	.= qq{ --f3_primary     }.$pri->{'primaries'}->{'f3_primary_reads'};
			$cmd	.= qq{ --f3_primary_dir   $pridir };
			$cmd	.= qq{ --f3_csfasta     }.$self->{'csfasta'}->{$barcode}->{'f3_reads'};
			$cmd	.= qq{ --f3_read_length }.$self->{'primerSet'}->{'f3'};
			$cmd	.= qq{ --f3_qual        }.$self->{'qual'}->{$barcode}->{'f3_reads'};

			if($run_type eq 'pairedendBC') {
				$self->{'csfasta'}->{$barcode}->{'f5_reads'}	=~ s/.+\/(primar.+)/$1/;
				$self->{'qual'}->{$barcode}->{'f5_reads'}	=~ s/.+\/(primar.+)/$1/;

				$cmd	.= qq{ --f5_csfasta $self->{'csfasta'}->{$barcode}->{'f5_reads'} --f5_read_length $self->{'primerSet'}->{'f5-bc'} --f5_qual $self->{'qual'}->{$barcode}->{'f5_reads'} };
				$cmd	.= qq{ --f5_primary_dir   $pridir };
			}
			elsif($run_type eq 'matepair') {
				$self->{'csfasta'}->{$barcode}->{'r3_reads'}	=~ s/.+\/(primar.+)/$1/;
				$self->{'qual'}->{$barcode}->{'r3_reads'}	=~ s/.+\/(primar.+)/$1/;

				$cmd	.= qq{ --r3_csfasta $self->{'csfasta'}->{$barcode}->{'r3_reads'} --r3_read_length $self->{'primerSet'}->{'r3'} --r3_qual $self->{'qual'}->{$barcode}->{'r3_reads'} };
				$cmd	.= qq{ --r3_primary_dir   $pridir };
			}
	
			my $rv	= `$cmd`;

			$self->writelog(BODY => "$rv : $cmd");

			if($rv != 0) {
				$self->writelog(BODY => "Could not generate INI files");
				exit($self->EXIT_NO_INI_FILE());
			}
		}
	}

	$self->writelog(BODY => "...done");
	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}

###############################################################################
=pod

B<write_bioscope_ini_files()> 
 Generate Bioscope ini files to ingest with raw data

Parameters:

Returns:

=cut
=cut
sub write_bioscope_ini_files {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options		= {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Generating Bioscope INI files---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	# get run ID from Tracklite
	my $slide_name		= $self->{'slide_name'};

	# set email so INI files are written correctly
	$ENV{'QCMG_EMAIL'}	= $self->DEFAULT_EMAIL;

	# Identify the run_ids that belong to $slide and create ini files for each mapset
	my $lims		= QCMG::DB::TrackLite->new();
	$lims->connect;

	$self->writelog(BODY => "Connecting to TrackLite for $slide_name");

	# directory where bioscope files should be written; create and define
	$self->{'ini_root_dir'}	= $self->{'run_folder'}.$self->LA_ASSET_SUFFIX_BIOSCOPE_INI; 

	# if a ini folder for this date already exists, delete it
	if(-e $self->{'ini_root_dir'} && -d $self->{'ini_root_dir'}) {
		my $cmd = qq{rm -r }.$self->{'ini_root_dir'};
		$self->writelog(BODY => $self->{'ini_root_dir'}." exists, deleting: $cmd");
		my $suffix = $self->LA_ASSET_SUFFIX_BIOSCOPE_INI;
		if($self->{'ini_root_dir'} ne '/' && $self->{'ini_root_dir'} =~ /$suffix/) {
			system($cmd);
		}
	}
	else {
		$self->writelog(BODY => $self->{'ini_root_dir'}." does not exist (destination of INI files)");
	}

	$self->writelog(BODY => "Creating Bioscope INI root dir: ".$self->{'ini_root_dir'});
	my $rv = mkdir $self->{'ini_root_dir'};
	$self->writelog(BODY => "RV: $rv");

	$self->writelog(BODY => "Generating INI files...");
	my $ra_rows = $lims->mapsets_from_slide_name( $slide_name );
	foreach my $mapset (@{ $ra_rows }) {
		$self->writelog(BODY => "Getting TrackLite data for mapset $mapset->[0]");
		my $ini = QCMG::Bioscope::Ini::GenerateFiles->new(
							run_id		=> $mapset->[0],
							ini_root_dir	=> $self->{'ini_root_dir'}
							);
		$ini->generate;
	}
	$self->writelog(BODY => "...done");
	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
}
=cut

###############################################################################
=pod

B<solid_stats_report()> 
 Generate solid_stats_report for raw sequencing data

Parameters:
 requires 'run_folder' and 'slide_name'

Returns:

=cut
sub solid_stats_report {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options		= {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Generating solid_stats_report---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	# get path to raw sequencing data folder
	my $run_folder	= $self->{'run_folder'};
	# create filename for XML output file (under run folder)
	my $report	= $run_folder.$self->{'slide_name'}.".solidstats.xml";

	my $fcrxml	= $run_folder.$self->{'slide_name'}.".xml";

	my $cmd		= $self->BIN_SEQTOOLS.qq{solid_stats_report.pl };
	$cmd		.= qq{ -d $run_folder -f $fcrxml -x $report};

	# if run is barcoded, get barcode stats
	if($self->{'is_barcoded'} == 0) {
		# find BarcodeStatistics.*.txt
        	my $cmd         = qq{find $run_folder -name "BarcodeStat*"};
        	my @files       = `$cmd`;
        	my $bcstats     = shift @files;
        	chomp $bcstats;

		$cmd	.= qq{ -b $bcstats };
	}

	$self->writelog(BODY => $cmd);

	my $rv		= system($cmd);

	$self->writelog(BODY => "RV: $rv");

	$self->{'solid_stats_report'}	= $report;

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

	# override $? with $self->{'EXIT'}

	unless($self->{'EXIT'} > 0) {
		if($IMPORT_ATTEMPTED eq 'yes' && $IMPORT_SUCCEEDED eq 'yes') {
			$self->writelog(BODY => "EXECLOG: errorMessage none");

			$self->{'EXIT'} = 0;

			# send success email if no errors
			my $subj	= "Re: ".$self->{'slide_name'}." RAW INGESTION -- SUCCEEDED";
			my $body	= "RAW INGESTION of $self->{'run_folder'} successful\n";
			$body		.= "See log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			#$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
		}
		elsif($IMPORT_ATTEMPTED eq 'no') {
			$self->writelog(BODY => "EXECLOG: errorMessage import not attempted");
	
			$self->{'EXIT'} = 17;
	
			# send success email if no errors
			my $subj	= "Re: ".$self->{'slide_name'}." RAW INGESTION -- NOT ATTEMPTED";
			my $body	= "RAW INGESTION of $self->{'run_folder'} not attempted\n";
			$body		.= "See log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			#$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
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
