#!/usr/bin/perl

# script to be run by LiveArc trigger to branch into correct mapping pipeline(s)
# based on organism and material on slide

# $Id: run_aligner.pl 4214 2013-08-11 23:51:01Z l.fink $

$ENV{'PERL5LIB'} = "/share/software/QCMGPerl/lib/";

use strict;
use lib qw(/share/software/QCMGPerl/lib/);
use QCMG::DB::Metadata;
use QCMG::Util::Util qw(send_email);

my $slide	= $ARGV[1];
my $bin		= "/share/software/QCMGPerl/distros/automation/";

# decide which aligners to run based on slide material and genome; this is
# mostly to catch slides with both human DNA and RNA

my $has_alignable_dna	= 'n';
my $has_human_rna	= 'n';

my $md	= QCMG::DB::Metadata->new();
if($md->find_metadata("slide", $slide)) {
	my $slide_mapsets	=  $md->slide_mapsets($slide);
	#print Dumper $slide_mapsets;
	foreach my $mapset (@$slide_mapsets) {
		$md->find_metadata("mapset", $mapset);

        	my $d		= $md->mapset_metadata($mapset);

		my $alignreq	= $md->alignment_required($mapset);
		my $material	= $d->{'material'};
		my $genome	= $d->{'genome_file'};

		# if there is human RNA and the flag has not yet been set
		if($has_human_rna eq 'n' && $material =~ /RNA/i && $genome =~ /GRCh37_ICGC_standard_v2/) {
			$has_human_rna	= 'y';
		}
		# if there is DNA to align and flag has not yet been set
		elsif($has_alignable_dna eq 'n' && $alignreq == 1 && $material =~ /DNA/i) {
			$has_alignable_dna	= 'y';
		}
	}
}
else {
	print STDERR "Slide $slide not in LIMS, exiting\n";

	my $subject	= "$slide not in LIMS, cannot align mapsets\n";
	#QCMG::Util::Util->send_email(TO => "QCMG-InfoTeam\@imb.uq.edu.au", SUBJECT => $subject, BODY => "");
	QCMG::Util::Util->send_email(TO => "l.fink\@imb.uq.edu.au", SUBJECT => $subject, BODY => "");
	exit(1);
}

my @commands	= ();
if($has_alignable_dna eq 'y') {
	my $cmd	= qq{$bin/run_bwa.pl -i $slide};
	print STDERR "$cmd\n";
	push @commands, $cmd;
}
if($has_human_rna eq 'y') {
	my $cmd	= qq{$bin/run_rnaseq_mapping.pl -i $slide};
	print STDERR "$cmd\n";
	push @commands, $cmd;
}

# if there are no commands, then this is probably not human, mouse, or zebrafish
foreach (@commands) {
	print STDERR "TO RUN: $_\n";
	my $rv	= system($_);

	my $success_code	= 'FAILED';
	if($rv == 0) {
		$success_code	= 'SUCCEEDED';
	}

	my $subject	= "";
	if(/run_bwa/) {
		$subject	= "INITIATION OF DNA MAPPING for $slide $success_code ($rv)";
	}
	elsif(/run_rnaseq/) {
		$subject	= "INITIATION OF RNA MAPPING for $slide $success_code ($rv)";
	}

	QCMG::Util::Util->send_email(TO => "QCMG-InfoTeam\@imb.uq.edu.au", SUBJECT => $subject, BODY => "");
	#QCMG::Util::Util->send_email(TO => "l.fink\@imb.uq.edu.au", SUBJECT => $subject, BODY => "");
}

exit(0);
