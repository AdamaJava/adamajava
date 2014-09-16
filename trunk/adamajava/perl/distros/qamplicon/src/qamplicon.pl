#!/usr/bin/perl

##############################################################################
#
#  Program:  qamplicon.pl
#  Author:   Lynn Fink
#  Created:  2011-06-04
#
# Optimizes primers for Ion Torrent sequencing
#
# $Id: qamplicon.pl 4583 2014-04-09 03:02:34Z j.pearson $
#
##############################################################################

use strict;
use Getopt::Long;
use QCMG::Automation::QAmplicon;

# set command line, for logging
my $cline	= join " ", @ARGV;

use vars qw(
		$SVNID $REVISION $VERSION $LOG_FILE $LOG_LEVEL $USAGE $DBSNP
		$infile $genome $blastgenome $col $pos $exhaustive $with_dbsnp
		$torrent $outfile $dbsnp $threads $l_adapter $r_adapter $barcode
		$p3template $p3length $miseq
	);

# defaults
$col		= 1;
$LOG_FILE	= "qamplicon.log";
$with_dbsnp	= 0;	# default = no
$exhaustive	= 0;	# default = no
$p3length	= 500;	# default; automatically set to 1000 in miseq mode
$genome		= 'GRCh37_ICGC_standard_v2.fa';
#$genome		= '/panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa';
$l_adapter	= 'CCATCTCATCCCTGCGTGTCTCCGACTCAG';
$r_adapter	= 'CCTCTCTATGGGCAGTCGGTGAT';
$barcode	= '';
$p3template	= '';

# get command line options
&GetOptions(
		"i=s"		=> \$infile,
		"p3t=s"		=> \$p3template,
		"l=i"		=> \$p3length,
		"p=s"		=> \$pos,
		"g=s"		=> \$genome,
		"bg=s"		=> \$blastgenome,
		"o=s"		=> \$outfile,
		"t=i"		=> \$threads,
		"e!"		=> \$exhaustive,
		"dbsnp!"	=> \$with_dbsnp,
		"torrent!"	=> \$torrent,
		"miseq!"	=> \$miseq,
		"la=s"		=> \$l_adapter,
		"ra=s"		=> \$r_adapter,
		"barcode=s"	=> \$barcode,
		"log=s"		=> \$LOG_FILE,
		"loglevel=s"	=> \$LOG_LEVEL,
		"V!"		=> \$VERSION,
		"h!"		=> \$USAGE
	);

# help message
if($USAGE || ((! $pos || ! $infile) && ! $outfile)) {
        my $message = <<USAGE;

        USAGE: $0

        $0 -i pos.file -log logfile.log

	Required:
	-i        <file>  path to file with genomic positions
	or
	-p        <char>  genomic coordinate (chrN:X-Y)

	-o	  <file>  output file

	Optional:
	-p3t      <file>  primer3 configuration template file
	-l        <int>   length of genomic range around primer to explore [default=500; in miseq mode=1000]
	-t	  <int>   number of threads for BLAST to use [default=1; max=8]
	-dbsnp            (avoid primers overlapping known SNPs) [default=no]
        -torrent          (optimize design for Ion Torrent sequencing)
        -miseq            (optimize design for MiSeq sequencing)
	-e		  (exhaustive - try really hard to find a pair)
	-g        <file>  reference genome [default=$genome]
	-bg       <file>  reference genome to use when BLASTing primers for uniqueness [default=$genome]
	-la	  <chars> sequence of left adapter  [default=$l_adapter; null in miseq mode]
	-ra	  <chars> sequence of right adapter [default=$r_adapter; null in miseq mode]
	-barcode  <chars> barcode sequence for left primer

	-log      <file>  log file to write execution params and info to [default="qamplicon.log"]
	-loglevel         (INFO|DEBUG) INFO prints info to screen; DEBUG keeps all temp files
	-V                (version information)
        -h                (print this message)

USAGE

        print $message;
        exit(0);
}

umask 0002;

# write STDERR to log file in standard mode
my $stderrcap;
unless($LOG_LEVEL) {
	open SAVEERR, ">&STDERR";
	close STDERR;
	open STDERR, ">", \$stderrcap or warn "Cannot save STDERR to scalar\n";
}


# check for presence of input file, read positions and put into an array
my $pos_count	= 0;	# count positions found
my @positions	= ();
if($infile) {
	unless(-e $infile && -r $infile) {
		print STDERR "Cannot find input file; $infile : $!\n";
		exit(5);
	}

	open(FH, $infile) || die "Cannot open $infile: $!\n";
	while(<FH>) {
		chomp;
		next if(/^\#/);
		# check that position looks like a genomic coordinate; keep an 
		#  array of all positions
		if(/.+?\:\d+/) {
			push @positions, $_;
			$pos_count++;
		}
	}
	close(FH);
}
elsif($pos) {
	push @positions, $pos;
	$pos_count++;
}
print STDERR "Positions found:\t$pos_count\n";
exit(6) if($pos_count < 1);

# open output file for writing primer information to
open(FH, ">".$outfile) || die "Cannot open $outfile : $!\n";
# only print header once; keep track of whether or not that has happened
my $printed_header	= 0;

# set mode
my $mode	= 'normal';
$mode		= 'torrent' if($torrent);
$mode		= 'miseq'   if($miseq);

# no adapters are required for MiSeq sequencing
if($mode =~ /miseq/i) {
	$l_adapter	= "";
	$r_adapter	= "";
}

# design a primer pair for each position requested
foreach (@positions) {
	# try to find primer pair quickly
	print STDERR "Finding pair:\t\t$_\n";
	my ($header, $primer) = &find_pairs(	$_, 
						$genome,
						$mode,
						$blastgenome,
						$DBSNP,
						0,
						$l_adapter,
						$r_adapter,
						$barcode,
						$p3template,
						$p3length
					);

	# if no pair found, try exhaustively if requested
	if($primer !~ /\w+/ && $exhaustive == 1) {
		print STDERR "Finding pair exhaustively\n";
		$exhaustive	= 2;	# set to skip pairs that were just tried
		($header, $primer) = &find_pairs(
						$_,
						$genome,
						$mode,
						$blastgenome,
						$DBSNP,
						$exhaustive,
						$l_adapter,
						$r_adapter,
						$barcode,
						$p3template,
						$p3length
					);
	}

	# if MiSeq mode and no pairs found, reduce the required distance between
	# the primer and the variant position 
	if($primer !~ /\w+/ && $mode =~ /miseq/) {
		print STDERR "Reducing primer/variant distance to find a pair\n";
		$mode		= 'miseq_desperate';	
		($header, $primer) = &find_pairs(
						$_,
						$genome,
						$mode,
						$blastgenome,
						$DBSNP,
						$exhaustive,
						$l_adapter,
						$r_adapter,
						$barcode,
						$p3template,
						$p3length
					);
	}


	# print header (once)
	if($printed_header == 0) {
		print FH $header;
		$printed_header	= 1;
	}

	# print primer, if there is one
	if($primer) {
		# if a primer was found, print out its details
		print FH $primer;
	}
	else {
		# if no primer was found, print out the position and blank
		# fields to recapitulate input file
		print STDERR "No primer pair found for $_\n";
		print FH "$_\n";
	}
}
close(FH);

# close and restore STDERR to original condition.
unless($LOG_LEVEL) {
	close STDERR;
	open STDERR, ">&SAVEERR";
	#$qc->writelog(BODY => $stderrcap);
}

#$qc->writelog(BODY => "EXECLOG: errorMessage none");

#my $eltime	= $qc->elapsed_time();
#my $date	= $qc->timestamp();
#$qc->writelog(BODY => "EXECLOG: elapsedTime $eltime");
#$qc->writelog(BODY => "EXECLOG: stopTime $date");
#$qc->writelog(BODY => "EXECLOG: exitStatus 0");

exit(0);

###############################################################################
sub find_pairs {
	my $pos		= shift @_;
	my $genome	= shift @_;
	my $mode	= shift @_;
	my $blastgenome	= shift @_;
	my $DBSNP	= shift @_;
	my $exhaustive	= shift @_;
	my $l_adapter	= shift @_;
	my $r_adapter	= shift @_;
	my $barcode	= shift @_;
	my $p3template	= shift @_;
	my $p3length	= shift @_;

	my $result	= "";

	# start time in seconds
	my $starttime	= time;

	my $qp = QCMG::Automation::QAmplicon->new(	POS		=> $_,
							GENOME		=> $genome,
							MODE		=> $mode,
							BLASTGENOME 	=> $blastgenome,
							DBSNP		=> $DBSNP,
							EXHAUSTIVE 	=> $exhaustive,
							LADAPTER	=> $l_adapter,
							RADAPTER	=> $r_adapter,
							BARCODE		=> $barcode,
							P3TEMPLATE	=> $p3template,
							P3LENGTH	=> $p3length,
							THREADS		=> $threads,
							LOG_LEVEL	=> $LOG_LEVEL
						);

	my $pairs	= $qp->run_primer3(INFILE => $infile);
	print STDERR "TOTAL_PAIRS\t".scalar(@{$pairs})."\n";

	# evaluate pairs (amplicon length, Tm, ID, dbSNP, etc.; keep good ones)
	$pairs		= $qp->evaluate_pairs(PAIRS => $pairs, DBSNP => $with_dbsnp);
	print STDERR scalar(@{$pairs})." acceptable pairs found\n" if($LOG_LEVEL);
	print STDERR "VIABLE_PAIRS\t".scalar(@{$pairs})."\n";

	# check pairs some more
	my $goodpair	= '';
	my $found_pair	= 1;
	my $bad_blast_pairs	= 0;
	my $bad_dimer_pairs	= 0;
	my $bad_pal_pairs	= 0;
	foreach my $pair (@{$pairs}) {
		my $status	= $qp->blast_for_specificity(PAIR => $pair);

		$bad_blast_pairs	+= 1 if($status == 1);

		next unless($status == 0);


		# add barcode, if applicable
		if($barcode) {
			$pair	= $qp->add_barcode(PAIR => $pair, BARCODE => $barcode);
		}

		# add torrent adapters (after barcode!)
		if(defined $l_adapter || defined $r_adapter) {
			$pair = $qp->add_adapters(PAIR => $pair, LEFT => $l_adapter, RIGHT => $r_adapter);
		}

		# check for primer dimers (BLAST)
		$status			= $qp->check_autodimer(PAIR => $pair);

		$bad_dimer_pairs	+= 1 if($status == 1);

		next unless($status == 0);

		# check for palindromes
		$status	= $qp->check_palindrome(PAIR => $pair);

		$bad_pal_pairs		+= 1 if($status == 1);

		next unless($status == 0);

		if($status == 0) {
			# GOOD PAIR, KEEP
			print STDERR "Good pair, ending search for this position\n";
			$goodpair	= $pair;

			$found_pair	= 0;

			#print STDERR "PAIR ($found_pair): $pair\n";
	
			last;
		}
	}

	print STDERR "BAD_BLAST_PAIRS\t$bad_blast_pairs\n";
	print STDERR "BAD_DIMER_PAIRS\t$bad_dimer_pairs\n";
	print STDERR "BAD_PAL_PAIRS\t$bad_pal_pairs\n";
	my $endtime	= time;
	my $eltime	= $endtime - $starttime;
	print STDERR "ELTIME_SECS\t$eltime\n";

	my ($header, $primer);
	if($found_pair == 1) {
		# could not find primer pair
		print STDERR "Failed to find primer pair\n";
	}
	else {
		# found good primer pair
		 $header	= $qp->format_header();
		 $primer	= $qp->format_primer(PAIR => $goodpair);

		#print STDERR "PRIMER: $primer\n";
	}


	$qp->delete_tempfiles() unless($LOG_LEVEL eq 'DEBUG');

	print STDERR "-" x 70, "\n";

	return($header, $primer);
}

