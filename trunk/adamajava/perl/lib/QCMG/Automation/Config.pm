package QCMG::Automation::Config;

##############################################################################
#
#  Module:   QCMG::Automation::Config.pm
#  Creator:  Lynn Fink
#  Created:  2011-03-01
#
#  This class contains variables common to the automated sequencing data
#  processing pipeline
#
#  $Id$
#
##############################################################################


=pod

=head1 NAME

QCMG::Automation::Config -- Common variables for use in automated pipeline

=head1 SYNOPSIS

Most common use:

 use Cnfg;

=head1 DESCRIPTION

Contains variables such as paths, settings, filename patterns, etc. for use in
automating the processing of sequencing data from the sequencing machines to the
final BAM. All variables are defined in a hash which is passed to the module
including this module as %c.

=head1 REQUIREMENTS


=cut

use strict;

use vars qw(%c);

# determine hostname for setting some variables
my $HOSTKEY = `hostname`;
chomp $HOSTKEY;

my $HOME = `echo $ENV{"HOME"}`;
chomp $HOME;

my $TMP_DIR	= $ENV{'TMPDIR'};

# host = barrine or barrine node
my( $LA_HOME, $JAVA_HOME, $LA_HOST, $BIN_SEQTOOLS, $BIN_AUTO, $DEFAULT_EMAIL,
$BIN_SAMTOOLS, $BIN_H5CHECK, $BIN_LOCAL, $PYTHON_HOME, $LIFESCOPE_RESULTS,
$AUTOMATION_LOG_DIR, $SEQ_RAW_PATH, $SEQ_MAPPED_PATH, $SEQ_RESULTS_PATH, $QUEUE,
$BIOSCOPE_INSTALL, $PANFS_MOUNT, $BIN_GLS_CLIENT);	# not exported/inherited

if($HOSTKEY =~ /barrine/ || $HOSTKEY =~ /^[ab][0-9]*[ab][0-9]*/) {
	$LA_HOME		= "/panfs/imb/qcmg/software/";
	$JAVA_HOME		= "/usr/";
	#cirrus.hpcu.uq.edu.au (10.160.72.57) 
	$LA_HOST		= '10.160.72.57';
	$HOSTKEY		= 'barrine';
	$QUEUE			= 'workq';
	$BIOSCOPE_INSTALL	= '/sw/bioscope/1.2.1/bioscope/';
	$PANFS_MOUNT		= '/panfs/imb/';
	# path to generate_bioscope_ini_files.pl; can reset in script if necessary
	$BIN_SEQTOOLS		= "/panfs/imb/qcmg/software/QCMGPerl/distros/seqtools/src/";
	# path to automation scripts
	$BIN_AUTO		= "/panfs/imb/qcmg/software/QCMGPerl/distros/automation/";
	# path to samtools
	$BIN_SAMTOOLS		= "/panfs/imb/qcmg/software/LifeScope_2.5/lifescope/bin/";
	# default email address
	$DEFAULT_EMAIL		= 'QCMG-InfoTeam@imb.uq.edu.au';
	$LIFESCOPE_RESULTS	= '/panfs/imb/lifescope_results/projects/';
	$AUTOMATION_LOG_DIR	= '/panfs/imb/automation_log/';
	$SEQ_RAW_PATH		= '/panfs/imb/seq_raw/';
	$SEQ_MAPPED_PATH	= '/panfs/imb/seq_mapped/';
}
# host = babyjesus
elsif($HOSTKEY =~ /qcmg-clustermk/ || $HOSTKEY =~ /minion/) {
	$LA_HOME		= "/panfs/share/software/mediaflux/";	# /panfs/share/software/mediaflux/aterm.jar
	$JAVA_HOME		= "/usr/";
	$PYTHON_HOME		= "/usr/local/mediaflux/python27/";
	$LA_HOST		= "qcmg-dispatch.imb.uq.edu.au";
	$QUEUE			= "batch";
	#$PANFS_MOUNT		= '/panfs/';
	$PANFS_MOUNT		= '/mnt/';
	$BIOSCOPE_INSTALL	= '/share/software/Bioscope/bioscope/';
	$BIN_SEQTOOLS		= "/share/software/QCMGPerl/distros/seqtools/src/";
	$BIN_AUTO		= "/share/software/QCMGPerl/distros/automation/";
	$BIN_SAMTOOLS		= "/panfs/share/software/samtools-0.1.14/";
	$BIN_H5CHECK		= "/share/install/h5check-2.0.1/tool/";
	$BIN_GLS_CLIENT		= '/panfs/home/lfink/devel/QCMGProduction/geneus/api/python/other_scripts/glsapi_version_1.0.1/';
	$DEFAULT_EMAIL		= 'QCMG-InfoTeam@imb.uq.edu.au';
	$LIFESCOPE_RESULTS	= '/panfs/lifescope_results/projects/';
	$AUTOMATION_LOG_DIR	= '/panfs/automation_log/';
	#$SEQ_RAW_PATH		= '/panfs/seq_raw/';
	$SEQ_RAW_PATH		= '/mnt/seq_raw/';
	#$SEQ_MAPPED_PATH	= '/panfs/seq_mapped/';
	$SEQ_MAPPED_PATH	= '/mnt/seq_mapped/';
	$SEQ_RESULTS_PATH	= '/mnt/seq_results/';
}
# host = sequencer
elsif($HOSTKEY =~ /solid/) {
	$LA_HOME		= "/usr/local/mediaflux/";
	$JAVA_HOME		= "/usr/local/mediaflux/jdk1.6.0_16/";
	$PYTHON_HOME		= "/usr/local/mediaflux/python27/";
	$LA_HOST		= "10.160.72.57";
	$BIN_SEQTOOLS		= "/usr/local/mediaflux/QCMGPerl/distros/seqtools/src/";
	$BIN_AUTO		= "/usr/local/mediaflux/QCMGPerl/distros/automation/";
	$DEFAULT_EMAIL		= 'QCMG-InfoTeam@imb.uq.edu.au';
}
# host = ion torrent servers
elsif($HOSTKEY =~ /TORRENT/i) {
	$LA_HOME		= "/home/distribution/";
	$JAVA_HOME		= "/usr/";
	$PYTHON_HOME		= "/usr/local/mediaflux/python27/";
	$LA_HOST		= "qcmg-dispatch.imb.uq.edu.au";
	$BIN_SEQTOOLS		= "/usr/local/mediaflux/QCMGPerl/distros/seqtools/src/";
	$BIN_AUTO		= "/usr/local/mediaflux/QCMGPerl/distros/automation/";
	$DEFAULT_EMAIL		= 'QCMG-InfoTeam@imb.uq.edu.au';
}
elsif($HOSTKEY =~ /5500/) {
	# qcmg-5500-machinename.imb.uq.edu.au
	# path to h5check
	$BIN_H5CHECK		= "/share/install/h5check-2.0.1/tool/";
	$BIN_LOCAL		= "/usr/local/mediaflux/bin/";
	$LA_HOME		= "/usr/local/mediaflux/";
	$JAVA_HOME		= "/usr/java/default/";
	$PYTHON_HOME		= "/usr/local/mediaflux/python27/";
	#cirrus.hpcu.uq.edu.au (10.160.72.57) 
	$LA_HOST		= '10.160.72.57';
	$BIN_SEQTOOLS		= "/usr/local/mediaflux/QCMGPerl/distros/seqtools/src/";
	$BIN_AUTO		= "/usr/local/mediaflux/QCMGPerl/distros/automation/";
	# default email address
	$DEFAULT_EMAIL		= 'QCMG-InfoTeam@imb.uq.edu.au';
}
else {
	# "generic" configuration; probably won't work for everything, but
	# better than having nothing set...
	$LA_HOME		= "/Users/l.fink/projects/bin/";
	$JAVA_HOME		= "/usr/";
	$PYTHON_HOME		= "/usr/local/mediaflux/python27/";
	$LA_HOST		= '10.160.72.57';
	$BIN_SEQTOOLS		= "/Users/l.fink/projects/devel/QCMGPerl/distros/seqtools/src/";
	$BIN_AUTO		= "/Users/l.fink/projects/devel/QCMGPerl/distros/automation/";
	$DEFAULT_EMAIL		= 'l.fink@imb.uq.edu.au';
}

%c = (
# Torrent machine directories
TMP_DIR				=> $TMP_DIR,
TORRENT_ANALYSIS_DIR		=> '/results/analysis/output/Home/',
TORRENT_GENOME_DIR		=> '/panfs/share/genomes/tmap-f3/',	# ref genomes on cluster
SEGMENTS_DIR			=> '/panfs/share/qsegments/',
# Bioscope machine and install directory
BIOSCOPE_SERVER			=> 'barrine',
#BIOSCOPE_INSTALL		=> '/sw/bioscope/1.2.1/bioscope/',
BIOSCOPE_INSTALL		=> $BIOSCOPE_INSTALL,
QUEUE				=> $QUEUE,
PANFS_MOUNT			=> $PANFS_MOUNT,
#SEQ_RAW_PATH			=> '/panfs/imb/seq_raw/',
SEQ_RAW_PATH			=> $SEQ_RAW_PATH,
#SEQ_MAPPED_PATH			=> '/panfs/imb/seq_mapped/',
SEQ_MAPPED_PATH			=> $SEQ_MAPPED_PATH,
SEQ_RESULTS_PATH		=> $SEQ_RESULTS_PATH,
HISEQ_RAW_PATH			=> '/mnt/hiseq/',
# Lifescope results directory
LIFESCOPE_RESULTS		=> $LIFESCOPE_RESULTS,
# Illumina-related software
BIN_CASAVA			=> '/panfs/share/software/CASAVA-1.8.2/bin/',
BIN_BWA				=> '/panfs/share/software/BWA/bwa-0.6.1/bin/',
# path to generate_bioscope_ini_files.pl; can reset in script if necessary
BIN_SEQTOOLS			=> $BIN_SEQTOOLS,
BIN_AUTO			=> $BIN_AUTO,
BIN_SAMTOOLS			=> $BIN_SAMTOOLS,
BIN_H5CHECK			=> $BIN_H5CHECK,
BIN_GLS_CLIENT			=> $BIN_GLS_CLIENT,
BIN_LOCAL			=> $BIN_LOCAL,
PYTHON_HOME			=> $PYTHON_HOME,
LA_HOME				=> $LA_HOME,
JAVA_HOME			=> $JAVA_HOME,
# LA_ASSET_SUFFIX is used to append a suffix to the name of the folder being
# ingested to give the asset an informative name.  For example, when run on the
# sequencers, this is "_seq_raw" so that, if folder S0449_20100414_2_Frag is
# being ingested, the asset would be named S0449_20100414_2_Frag_seq_raw
LA_NAMESPACE_SUFFIX_MAP		=> '_mapped',
LA_ASSET_SUFFIX_RAW		=> '_seq_raw', 
LA_ASSET_SUFFIX_MAP		=> '_seq_mapped',
LA_ASSET_SUFFIX_RES		=> '_seq_results',
LA_ASSET_SUFFIX_BIOSCOPE_INI	=> '_bioscope_ini',
LA_ASSET_SUFFIX_LIFESCOPE_LS	=> '_lifescope_ls',
LA_ASSET_SUFFIX_BIOSCOPE_LOG	=> '_bioscope_log',
LA_ASSET_SUFFIX_RUNBIOSCOPE_LOG	=> '_runbioscope_log',
LA_ASSET_SUFFIX_RUNTMAP_LOG	=> '_runtmap_log',
LA_ASSET_SUFFIX_INGEST_LOG	=> '_ingest_log',
LA_ASSET_SUFFIX_INGESTMAPPED_LOG	=> '_ingestmapped_log',
LA_ASSET_SUFFIX_EXTRACT_LOG	=> '_extract_log',
LA_ASSET_SUFFIX_SOLIDSTATS	=> '_solidstats',

# PBS job names
PBS_JOBNAME_RUN_PBS		=> 'bs_run_pbs',

# automation directory - for log files, temp files, etc. (barrine only)
AUTOMATION_LOG_DIR		=> $AUTOMATION_LOG_DIR,

# expected to have the following set up in $HOME
LA_CRED_FILE			=> "$HOME/.mediaflux/mf_credentials.cfg",

# suffix of run definition file, generated occasionally when raw sequencing data
# are produced
RUN_DEF_SUFFIX			=> '_run_definition.txt',

# name of symlink re-creation file
SYMLINK_FILE			=> "symlink.sh",

# default email address to send notifications to
#DEFAULT_EMAIL			=> 'l.fink@imb.uq.edu.au',
#DEFAULT_EMAIL			=> 'QCMG-InfoTeam@imb.uq.edu.au',
DEFAULT_EMAIL			=> $DEFAULT_EMAIL,

# LiveArc email address
#LIVEARC_EMAIL			=> 'mediaflux@imb.uq.edu.au',
LIVEARC_EMAIL			=> 'l.fink@imb.uq.edu.au',

# LA_HOST is the fully qualified name of the server running Mediaflux/LiveArc
LA_HOST				=> $LA_HOST,

# LA_PORT is the port that Mediaflux/LiveArc is listening on at $LA_HOST
LA_PORT				=> '8443',

# LA_TRANSPORT is one of "http", "https", or "tcp"
LA_TRANSPORT			=> "https",

# LA_NAMESPACE is the namespace within the Mediaflux/LiveArc instance at $LA_HOST
# that assets will import to.  In production it is currently "QCMG"
LA_NAMESPACE			=> "QCMG_solid",
LA_NAMESPACE_5500		=> "QCMG_5500",
LA_NAMESPACE_TORRENT		=> "QCMG_torrent",
LA_NAMESPACE_HISEQ		=> "QCMG_hiseq",
LA_NAMESPACE_MISEQ		=> "QCMG_miseq",


# LA_ADMIN_EMAILS is a comma separated list of email addresses that should
# be notified of the results of ingests.
#LA_ADMIN_EMAILS			=> 'scott.wood@imb.uq.edu.au,j.pearson@uq.edu.au,d.taylor@imb.uq.edu.au,l.fink@imb.uq.edu.au',
LA_ADMIN_EMAILS		=> 'l.fink@imb.uq.edu.au',

# LA_REPLY_TO is the email address that is send as the "Reply-to" with
# notifications
LA_REPLY_TO			=> 'l.fink@imb.uq.edu.au',
LA_JAVA				=> $JAVA_HOME."bin/java",

# LA_ATERM is the jar provided by arcitecta that talks to Mediaflux/LiveArc
LA_ATERM			=> $LA_HOME."aterm.jar",
LA_SID_FILE			=> $LA_HOME.".la_sid",

HOSTKEY				=> $HOSTKEY,

# These exit codes can be returned by aterm.jar
# From the Mediaflux Command.pdf doc:
LACOMMAND_EXIT 			=> {
					0	=> "Normal successful completion.",
					1	=> "An error has occurred – an stack trace will generally be printed to the mediafluxserver.log.",
					2	=> "Authentication failure during logon.",
					3	=> "Session is invalid or has timed out. Need to logon again."
				},

# VALID RUN TYPES: PAIRED-END, FRAGMENT, MATE-PAIR (others??)
#VALID_RUN_TYPES		=> {'PAIRED-END' => 1, 'FRAGMENT' => 1, 'MATE-PAIR' => 1}
VALID_RUN_TYPES			=> {'PE' => 1, 'Frag' => 1, 'LMP' => 1}
);

# example for how to set hash variables via accessor methods:
=cut
my $exitvals = $u->LACOMMAND_EXIT;
print "EXIT: $exitvals->{0}\n";
$exitvals->{0} = "new";
$u->LACOMMAND_EXIT($exitvals);
print $exitvals->{0}, "\n";
=cut

1;

__END__

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2009-2014

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
