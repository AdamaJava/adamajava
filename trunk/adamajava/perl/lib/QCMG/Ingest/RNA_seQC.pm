package QCMG::Ingest::RNA_seQC;

##############################################################################
#
#  Module:   QCMG::Ingest::RNA_seQC.pm
#  Creator:  Lynn Fink
#  Created:  2013-04-10
#
#  This class contains methods for automating the ingest into LiveArc
#  of RNAseq QC data
#
#  $Id: RNA_seQC.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
##############################################################################

=pod

=head1 NAME

QCMG::Ingest::RNA_seQC -- Common functions for ingesting RNA_seQC files 

=head1 SYNOPSIS

Most common use:
 my $qi = QCMG::Ingest::RNA_seQC->new();

=head1 DESCRIPTION

Contains methods for extracting slide information from a mapped sequencing slide
folder and ingesting them into LiveArc

=head1 REQUIREMENTS

 Exporters
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

	#print Dumper $self->{'slide_name'};

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
		$self->{'log_file'}		= $self->AUTOMATION_LOG_DIR.$self->{'slide_name'}."_ingestqc.log";
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
	my $subj	= $self->{'project'}." RNA_seQC INGEST -- FAILED";
	my $body	= "RNA_seQC INGEST of $self->{'seq_mapped'} failed";
	$body		.= "\nFailed: No mapset folder: $self->{'seq_mapped'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 1;
	return 1;
}
sub EXIT_BAD_PERMS {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." RNA_seQC INGEST -- FAILED";
	my $body	= "RNA_seQC INGEST of $self->{'seq_mapped'} failed";
	$body		.= "\nFailed: Bad permissions on $self->{'seq_mapped'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 2;
	return 2;
}
sub EXIT_LIVEARC_ERROR {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." RNA_seQC INGEST -- FAILED";
	my $body	= "RNA_seQC INGEST of $self->{'seq_mapped'} failed";
	$body		.= "\nFailed: LiveArc error";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 9;
	return 9;
}
sub EXIT_ASSET_EXISTS {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." RNA_seQC INGEST -- FAILED";
	my $body	= "RNA_seQC INGEST of $self->{'seq_mapped'} failed";
	$body		.= "\nFailed: LiveArc asset exists";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 12;
	return 12;
}
sub EXIT_BAD_RAWFILES {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." RNA_seQC INGEST -- FAILED";
	my $body	= "RNA_seQC INGEST of $self->{'seq_mapped'} failed";
	$body		.= "\nFailed: Raw sequencing files are corrupt";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE  $body");

	$self->{'EXIT'} = 13;
	return 13;
}
sub EXIT_MISSING_METADATA {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." RNA_seQC INGEST -- FAILED";
	my $body	= "RNA_seQC INGEST of $self->{'seq_mapped'} failed";
	$body		.= "\nFailed: Cannot find critical metadata";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 14;
	return 14;
}
sub EXIT_IMPORT_FAILED {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." RNA_seQC INGEST -- FAILED";
	my $body	= "RAW INGESTION of $self->{'seq_mapped'} failed";
	$body		.= "\nFailed: LiveArc import failed";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 16;
	return 16;
}
sub EXIT_ASSET_NOT_EXISTS {
	my $self	= shift @_;
	my $subj	= $self->{'project'}." RNA_seQC INGEST -- FAILED";
	my $body	= "RNA_seQC INGEST of $self->{'seq_mapped'} failed";
	$body		.= "\nFailed: Requested LiveArc asset does not exist";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
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

B<check_for_core_samples()> 
 Check LIMS to see if flowcell contains Core Facility samples

Requires:
 $self->{'slide_name'}

Returns:
 scalar - 'Y' or 'N'

=cut
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
=cut

################################################################################
=pod

B<find_log()> 
 Find log files
 
Parameters:

Sets:
 $self->{'logdir'}
 $self->{'logfiles'}

Requires:
 $self->{'slide_name'}
 $self->{'seq_mapped'}

Returns:
 void

=cut
sub find_log {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Searching for log files for $self->{'seq_raw'}---");

	my $logdir		= join "/", $self->SEQ_RAW_PATH, $self->{'slide_name'}."/log/";
	$self->{'logdir'}	= $logdir;
	$self->writelog(BODY => "Log directory: $logdir");

	unless(-e $logdir) {
		$self->writelog(BODY => "No log files found");
		exit($self->EXIT_MISSING_METADATA());
	}

	my $cmd			= qq{find $logdir -name "*_qc_out.log"};
	$self->writelog(BODY => "Finding RNA_seQC log file: $cmd");
	my @logfiles		= `$cmd`;

	foreach (@logfiles) {
		chomp;
	}

	$self->{'logfiles'}	= \@logfiles;

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
	# /QCMG_hiseq/120511_SN7001243_0090_BD116HACXX/120511_SN7001243_0090_BD116HACXX_qc
	$self->{'ns'}	= join "/", $self->LA_NAMESPACE, $self->{'slide_name'}, $self->{'slide_name'}."_qc";

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
	##$self->send_email(SUBJ => $subj, BODY => $body);

	return();
}


################################################################################
=pod

B<ingest()> 
 Ingest whole RNA_seQC directory

Parameters:

Requires:

Returns:
 void

=cut
sub ingest {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Preparing for RNA_seQC ingestion... ---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	### PERFORM INGESTION
	my $date = $self->timestamp();
	$self->writelog(BODY => "Ingest started $date");

	my $qcdir	= join "/", $self->SEQ_MAPPED_PATH, $self->{'slide_name'}, "qc";

 	#my $qcasset	= $self->filename(FILE => $_);
	my $qcasset	= $self->{'slide_name'}."_qcdirs";

	$self->writelog(BODY => "Checking if asset $qcasset exists...");
	my $r			= $self->{'mf'}->asset_exists(NAMESPACE => $self->{'ns'}, ASSET => $qcasset);
	# if asset exists, returns ID
	$self->writelog(BODY => "Result: $r");
	if($r) {
		if($self->{'update_ingest'} eq 'Y') {
			# if updating ingest and BAM exists, don't do it
			# again
			$self->writelog(BODY => "Asset $qcasset already exists, not ingesting it on update");
			$self->writelog(BODY => "END: ".$self->timestamp());
			return();
		}
		else {
			$self->writelog(BODY => "Asset $qcasset exists");
			exit($self->EXIT_ASSET_EXISTS());
		}
	}
	$self->writelog(BODY => "Done checking mapped namespace and QC asset");

	my $status	= 2;

	my $id = $self->{'mf'}->laimport(NAMESPACE => $self->{'ns'}, ASSET => $qcasset, DIR => $qcdir);
	$self->writelog(BODY => "Import result: $id");

	$self->writelog(BODY => "Checking if asset $qcasset exists...");
	my $r			= $self->{'mf'}->asset_exists(NAMESPACE => $self->{'ns'}, ASSET => $qcasset);
	# if asset exists, returns ID
	$self->writelog(BODY => "Result: $r");

	$status = 0 unless($r =~ /error/i);

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

B<ingest_log()> 
 Ingest log files

Parameters:

Requires:
 $self->{'logfiles'}

Returns:
 void

=cut
sub ingest_log {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $status = 0;

	$self->writelog(BODY => "---Preparing for log file ingestion... ---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	# ingest each BAM
	my $status	= 2;
	foreach (@{$self->{'logfiles'}}) {
		# get pathless filename of file to use as asset name
 		my $logfileasset	= $self->filename(FILE => $_);

		$self->writelog(BODY => "Checking if asset $logfileasset exists...");
		my $r			= $self->{'mf'}->asset_exists(NAMESPACE => $self->{'ns'}, ASSET => $logfileasset);
		# if asset exists, returns ID
		$self->writelog(BODY => "Result: $r");
		if($r) {
			if($self->{'update_ingest'} eq 'Y') {
				# if updating ingest and BAM exists, don't do it
				# again
				$self->writelog(BODY => "Asset $logfileasset already exists, not ingesting it on update");
				$self->writelog(BODY => "END: ".$self->timestamp());
				return();
			}
			else {
				$self->writelog(BODY => "Asset $logfileasset exists");
				exit($self->EXIT_ASSET_EXISTS());
			}
		}
		$self->writelog(BODY => "Done checking mapped namespace and logfile asset");

		my $id = $self->{'mf'}->laimport_file(NAMESPACE => $self->{'ns'}, ASSET => $logfileasset, FILE => $_);
		$self->writelog(BODY => "Import result: $id");
	
		$self->writelog(BODY => "Checking if asset $logfileasset exists...");
		my $r			= $self->{'mf'}->asset_exists(NAMESPACE => $self->{'ns'}, ASSET => $logfileasset);
		# if asset exists, returns ID
		$self->writelog(BODY => "Result: $r");

		$status = 0 unless($r =~ /error/i);
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
			my $subj	= $self->{'project'}." RNA-seQC INGESTION -- SUCCEEDED";
			my $body	= "RNA-seQC INGESTION of $self->{'slide_name'} complete\n";
			$body		.= "See log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			#$self->send_email(SUBJECT => $subj, BODY => $body);
		}
		# import was not attempted; script ended for some other
		# reason
		elsif($IMPORT_ATTEMPTED eq 'no') {
			$self->writelog(BODY => "EXECLOG: errorMessage BAM import not attempted");
	
			$self->{'EXIT'} = 17;
	
			# send success email if no errors
			my $subj	= $self->{'project'}." RNA-seQC INGESTION -- NOT ATTEMPTED";
			my $body	= "RNA-seQC INGESTION of $self->{'bam'} not attempted";
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
		my $asset	= $self->{'slide_name'};
		$asset		.= "_ingestqc_log";

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

Copyright (c) The University of Queensland 2013-2014

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
