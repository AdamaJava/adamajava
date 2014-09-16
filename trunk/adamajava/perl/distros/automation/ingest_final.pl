#!/usr/bin/perl

##############################################################################
#
#  Program:  ingest_final.pl
#  Author:   Scott Wood
#  Created:  2013-05-16
#
# seq_final bam file ingestion script; copies files from babyjesus
# and archives them in LiveArc
#
# $Id: ingest_final.pl 4667 2014-07-24 10:09:43Z j.pearson $
#
##############################################################################
	# DO NOT RUN WHILE THIS IS HERE
	#########################################################################
	# Work so far below is to give me references to the subroutines that I
	# will be using.  A better understanding of the subroutines, their use,
	# and their requirements is still necessary to flesh this out.
	#########################################################################

use strict;
use warnings;
use Getopt::Long;
use Pod::Usage;
use QCMG::Automation::LiveArc;
use QCMG::DB::Metadata;

use vars qw($SVNID $REVISION $VERSION $VERBOSE $OVERWRITE);

MAIN: {

	# Setup defaults for important variables.
	# The GetOpt stuff:
	my @emails        = ();
	my $file          = '';
	my $logfile       = '';
	my $overwrite     = '';
	my $prune         = '';
	   $VERBOSE       = 0;
	   $VERSION       = 0;
	my $help          = 0;
	my $man           = 0;


	# Print usage message if no arguments supplied
	# Stolen directly from JP's scripts 
	pod2usage(1) unless (scalar @ARGV > 0);

	# Print a copy of the command lina as supplied
	my $commandline = join(' ',@ARGV);

	# Use GetOptions module to parse commandline options
	my $results = GetOptions (
		'e|email=s'     => \@emails,
		'f|file=s'      => \$file,
		'l|logfile=s'   => \$logfile,
		'o|overwrite'   => \$OVERWRITE,
		'p|prune'       => \$prune,
		'v|verbose+'    => \$VERBOSE,
		'version!'      => \$VERSION,
		'h|help|?'      => \$help,
		'man|m'         => \$man
	);

	#########################################################################
	# Spoon!
	#########################################################################

	my $bam = undef;
	# Check that the file exists and is a readable bam
	## From QCMG::SeqResults::Util.pm ##
	if (-e $file and -r $file) {
		$bam = QCMG::SamTools::Bam->open( $file );
		die if (! defined $bam);
	}
	
	my $mf = '';
	# Fail if we can't connect to LiveArc
	$mf = QCMG::Automation::LiveArc->new();

	# Sort out project/donor

	# Fail if the project or donor namespace doeesn't exist in LiveArc


	my $id        = '';
	my $name      = '';
	my $namespace = '';
	# Check for the asset in LiveArc
	$id = $mf->asset_exists( ASSET->"$name", NAMESPACE->"$namespace" );
	
	# End if the asset exists, is newer, and force is not set

	my $meta = '';
	# Build the metadata for the file
	$meta = build_meta_string( $bam );

	# Ingest the file

	# Set the metadata

	# Prune the assset if prune is set
}

sub build_meta_string {
	my $bam    = shift();
	my $header = '';
	my $meta   = '';


	# run `cksum` on the file to get cksum/size/filename
	# <checksum>
	#	<chksum/>
	#	<filesize/>
	#	<filename/>
	# </checksum>	

	# Get the header to make <bam_header> element
	$header = $bam->header->text;

	# Parse the header to strip out <contained_bam> elements

	# Build the $meta string

	return $meta;
}
