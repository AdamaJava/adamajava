#!/usr/bin/perl

# this script can be used to 
# -- initially set the Lifescope run status in LiveArc
# -- set the Lifescope run status in LiveArc after a run has finished
# -- check on the status of a current run (from local log file, not LiveArc)
#
# must specify a project and analysis to query; if checking on another user's
# run, that user must also be specified or it is assumed that the user is the
# same as the one running this script
#
# looks for the last line of the summary.log which should say:
#  Analysis finished successfully.
#
# if the last line matches this, exit 0, otherwise exit 1

use strict;
use Getopt::Long;

# determine hostname for setting some variables
my $HOSTKEY = `hostname`;
chomp $HOSTKEY;

# host = barrine or barrine node
our($LA_HOME, $JAVA_HOME, $LA_HOST, $LIFESCOPE_RESULTS, $AUTOMATION_LOG_DIR, $BIN);	

if($HOSTKEY =~ /barrine/ || $HOSTKEY =~ /^[ab][0-9]*[ab][0-9]*/) {
	$HOSTKEY		= 'barrine';
	$LA_HOME		= "/panfs/imb/qcmg/software/";
	$JAVA_HOME		= "/usr/";
	$LA_HOST		= '10.160.72.57';
	$LIFESCOPE_RESULTS	= '/panfs/imb/lifescope_results/projects/';
	$AUTOMATION_LOG_DIR	= '/panfs/imb/automation_log/';
	$BIN			= '/panfs/imb/qcmg/software/QCMGPerl/distros/automation/';
}
# host = babyjesus
elsif($HOSTKEY =~ /qcmg-clustermk/ || $HOSTKEY =~ /minion/) {
	$HOSTKEY		= 'qcmg-cluster';
	$LA_HOME		= "/panfs/share/software/mediaflux/";	
	$JAVA_HOME		= "/usr/";
	$LA_HOST		= "10.160.72.57";
	$LIFESCOPE_RESULTS	= '/panfs/lifescope_results/projects/';
	$AUTOMATION_LOG_DIR	= '/panfs/automation_log/';
	$BIN			= '/share/software/QCMGPerl/distros/automation/';
}


# set global variables
our ($verbose, $atermbin, $livearccmd, $lsrespath, $logfilename);
use vars qw($project $analysis $user $mode $usage);
# summary.log filename and path
# /panfs/imb/lifescope_results/projects/uqlfink/S8006_20110815_1_LMP/20111212/summary.log
#$lsrespath	= "/panfs/imb/lifescope_results/projects";
$lsrespath	= $LIFESCOPE_RESULTS;
$logfilename	= "summary.log";
# livearc command
#$atermbin	= '/panfs/imb/qcmg/software/';
$atermbin	= $LA_HOME;
$livearccmd	= "$JAVA_HOME/bin/java -Dmf.cfg=\$QCMG_HOME/.mediaflux/mf_credentials.cfg -jar $atermbin/aterm.jar --app exec ";

&GetOptions(
		"p=s"	=> \$project,
		"a=s"	=> \$analysis,
		"u=s"	=> \$user,
		"m=s"	=> \$mode,
		"h!"	=> \$usage,
		"v!"	=> \$verbose
	);

if($usage || (! $project && ! $analysis)) {
	my $message = <<USAGE;

	USAGE: $0

	Get exit status of a Lifescope run using the summary.log file

	Check the status of a run:
	$0 -p slide_name -a analysis_name
	$0 -p slide_name -a analysis_name -m check

	Check the status of someone else's run:
	$0 -p slide_name -a analysis_name -u someuser 

	Set the status of a run when starting a new one:
	$0 -p slide_name -a analysis_name -m init

	Set the status of a run after it has been initialized (with -m init):
	$0 -p slide_name -a analysis_name -m set 

	Required:
	-p	slide name (Lifescope project name)
	-a	run date (Lifescope analysis name)

	Optional:
	-u	barrine username [default = whoami]
	-m	run mode (init|check|set) [default=check]
	-v	(verbose mode)
	-h	(print this message)

USAGE

	print $message;
	exit(0);
}
#'

# set user
$user	= &set_user($user);
# set name of logfile and path to file
my $logfile	= &summary_log($project, $analysis, $user);
# set LiveArc namespace and asset name
my $ns		= &project_la_namespace($project);
my $asset	= &project_la_asset($project);

if($mode eq 'init') {
	my $rv	= &status_init($ns, $asset);

	print STDERR "$rv\n";
	#print STDERR "$project / $analysis / $user status initialize metadata\n";
}
elsif($mode eq 'set') {
	my ($rv, $note)	= &status_set($ns, $asset, $project, $analysis, $user);

	print STDERR "$rv, $note\n";
	#print STDERR "$project / $analysis / $user status set metadata\n";

	# if Lifescope was successful, ingest BAMs by qsubbing a job on barrine
	# or by ingesting from the head node on babyjesus
	if($rv == 0) {
		if($HOSTKEY eq 'barrine') {
			print STDERR "Lifescope run succeeded, qsubbing ingest_mapped_5500.pl\n";
			&qsub_ingest($user, $project, $analysis);
		}
		else {
			print STDERR "Lifescope run succeeded, nohupping ingest_mapped_5500.pl\n";
			&run_ingest($user, $project, $analysis);
		}
	}
}
else {
	# assume -m check
	my ($rv, $note)	= &status_check($project, $analysis, $user, $logfile);

	print STDERR "$rv, $note\n";
	print STDOUT "$rv, $note\n";
	#print STDERR "$project / $analysis / $user status from $logfile\n";
}

exit(0);

######################################################################
# Subroutines
######################################################################

sub status_init {
	my $ns		= shift @_;
	my $asset	= shift @_;

	my $rv		= &create_asset($ns, $asset);

	$rv		= &init_metadata($ns, $asset);

	return($rv);
}

sub status_set {
	my $ns		= shift @_;
	my $asset	= shift @_;
	my $project	= shift @_;
	my $analysis	= shift @_;
	my $user	= shift @_;

	my ($rv, $note)	= &summary_log_status($logfile);

	my $status	= 'SUCCEEDED';
	if($rv == 1) {
		$status	= 'FAILED';
	}
	$rv		= &set_metadata($ns, $asset, $status, $note);

	return($rv);
}

sub status_check {
	my $project	= shift @_;
	my $analysis	= shift @_;
	my $user	= shift @_;
	my $logfile	= shift @_;

	my ($rv, $note)		= &summary_log_status($logfile);

	return($rv, $note);
}

# set user; can be yourself or someone else
sub set_user {
	my $user	= shift @_;

	# set user
	unless($user) {
		$user = `whoami`;
		chomp $user;
	}
	print STDERR "As user $user\n" if($verbose);

	return($user);
}

# create filename and path for summary.log file; exit(1) if file doesn't exist
sub summary_log {
	my $project	= shift @_;
	my $analysis	= shift @_;
	my $user	= shift @_;

	# define logfile path and name
	my $logfile	= join "/", $lsrespath, $user, $project, $analysis, $logfilename;
	if(! -f $logfile) {
		print STDERR "Cannot find $logfile\n";
		exit(1);
	}

	return($logfile);
}

# get last line of summary.log and translate it into a bool
sub summary_log_status {
	my $logfile	= shift @_;

	# get last line of logfile
	my $cmd		= qq{tail -1 $logfile};
	my $line	= `$cmd`;
	chomp $line;
	
	# translate last line into boolean status
	my $rv	= 0;
	unless($line =~ /finished successfully/) {
		$rv	= 1;
	}
	
	print STDERR "Exit:   $rv\n" if($verbose);

	return($rv, $line);
}

# LiveArc namespace for this project's mapping data
sub project_la_namespace {
	my $project	= shift @_;
=cut
  :namespace -path "/QCMG_5500/S8006_20110815_1_LMP"
        :asset -id "47173" -version "1" "S8006_20110815_1_LMP.lane_1.nobc.ls"
        :asset -id "47174" -version "1" "S8006_20110815_1_LMP.lane_1.nobc.run.pbs"
        :asset -id "47175" -version "2" "S8006_20110815_1_LMP.lane_1.nobc.xsq"

        S8006_20110815_1_LMP.lane_01.nobc_mapped"
=cut

	# project = S8006_20110815_1_LMP.lane_1.nobc
	#           -----------------
	#$project	=~ /^(\w+\_\d+\_\d\_)/;
	#my $slidens	= $1.'nort';

	# project = S8006_20110815_1_LMP.lane_1.nobc
	#           --------------------
	$project	=~ /^(\w+\_\d+\_\d\_\w+)/;
	my $slidens	= $1;

	my $jobid	= 0;
	my $ns		= '/QCMG_5500/'.$slidens;
	$ns		.= '/'.$project."_mapped";

	# replace barcode in namespace with nobc
	$ns		=~ s/\.bcB.+?\_mapped/\.nobc\_mapped/;

	return($ns);
}

# LiveArc asset name that carries Lifescope run status metadata
sub project_la_asset {
	my $project	= shift @_;

	my $asset	= $project."_seq_mapped";

	return($asset);
}

# create LiveArc asset SLIDE_seq_mapped
sub create_asset {
	my $ns		= shift @_;
	my $asset	= shift @_;

	# path/QCMG_5500/S8006_20110815_1_LMP/S8006_20110815_1_LMP_mapped/:S8006_20110815_1_LMP_seq_mapped
	# asset.create :namespace /test/S8006_20110815_1_LMP/S8006_20110815_1_LMP_mapped/ :name S8006_20110815_1_LMP_seq_mapped
	my $cmd		 = $livearccmd;
	$cmd		.= " asset.namespace.create :namespace $ns ";

	my $rv		= system($cmd);

	print STDERR "$rv : $cmd\n" if($verbose);

	$cmd		= $livearccmd;
	$cmd		.= " asset.create :namespace $ns :name $asset ";

	$rv		= system($cmd);

	print STDERR "$rv : $cmd\n" if($verbose);

	return($rv);
}

sub init_metadata {
	my $ns		= shift @_;
	my $asset	= shift @_;

	# path=/QCMG_5500/S8006_20110815_1_LMP/S8006_20110815_1_LMP_mapped/:S8006_20110815_1_LMP_seq_mapped
	my $cmd		 = $livearccmd;
	$cmd		.= ' asset.set :id "path=<PATH>" ';
	$cmd		.= ' :meta -action merge \< :mappinginfo \< :job_id "0" :map_start "now" :map_end "now" :status "RUNNING" :notes " " \> \>';

	#print STDERR "$cmd\n" if($verbose);
	
	$cmd		=~ s/<PATH>/$ns\/$asset/;

	print STDERR "$cmd\n" if($verbose);

	my $rv		= system($cmd);

	return($rv);
}

sub set_metadata {
	my $ns		= shift @_;
	my $asset	= shift @_;
	my $status	= shift @_;
	my $notes	= shift @_;

	# path/QCMG_5500/S8006_20110815_1_LMP/S8006_20110815_1_LMP_mapped/:S8006_20110815_1_LMP_seq_mapped
	my $cmd		 = $livearccmd;
	$cmd		.= ' asset.set :id "path=<PATH>" ';
	$cmd		.= ' :meta -action merge \< :mappinginfo \< :map_end "now" :status <STATUS> :notes "<NOTES>" \> \>';

	#print STDERR "$cmd\n" if($verbose);
	
	$cmd		=~ s/<PATH>/$ns\/$asset/;
	$cmd		=~ s/<STATUS>/$status/;
	$cmd		=~ s/<NOTES>/$notes/;

	print STDERR "$cmd\n" if($verbose);

	my $rv		= system($cmd);

	return($rv);
}

sub qsub_ingest {
	my $user	= shift @_;
	my $project	= shift @_;
	my $analysis	= shift @_;

	# write PBS script
	my $pbsfile	= "/scratch/".$project.".ingest_mapped.pbs";

	my $pbsfc	= qq{#!/bin/bash
#PBS -N ingmap5500
#PBS -q workq
#PBS -A sf-QCMG
#PBS -S /bin/bash
#PBS -r n
#PBS -j oe
#PBS -l walltime=10:00:00
#PBS -l select=1:ncpus=2:mem=1gb
#PBS -m ae

$BIN/ingest_mapped_5500.pl -p $project -u $user -a $analysis

};

	open(FH, ">".$pbsfile) || exit(2);
	print FH $pbsfc;
	close(FH);


	print STDERR "qsubbing $pbsfile\n";
	system(qq{qsub $pbsfile});

	return();
}

sub run_ingest {
	my $user	= shift @_;
	my $project	= shift @_;
	my $analysis	= shift @_;


	my $cmd		= qq{nohup $BIN/ingest_mapped_5500.pl -p $project -u $user -a $analysis &};

	print STDERR "Starting ingest...\n";
	system($cmd);

	return();
}
