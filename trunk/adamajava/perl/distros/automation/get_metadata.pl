#!/usr/bin/perl

use strict;
#use lib qw(/panfs/home/lfink/devel/QCMGPerl/lib/);
use QCMG::DB::Metadata;
use Data::Dumper;
use Getopt::Long;

my ($slide, $short, $mapset, $usage);

&GetOptions(
		"s=s"	=> \$slide,
		"b!"	=> \$short,
		"m=s"	=> \$mapset,
		"h!"	=> \$usage
);

# help message
if($usage || (! $slide && ! $mapset) ) {
	my $message = <<USAGE;

	USAGE: $0

	Get metadata for a slide or mapset

	$0 -s S88006_20111219_2_FragPEBC

	$0 -m 120523_SN7001240_0047_BD12NAACXX.lane_1.nobc

	Required:
	-s	<slide>
	OR
	-m	<mapset>

	Optional:
	-b      (only print short output for certain fields)
	-h	(print this message)

USAGE

	print $message;
	exit(0);
}


my $md	= QCMG::DB::Metadata->new();
=cut
project - icgc_pancreatic
failed_qc - 0
primary_library - Library_20120627_U
capture_kit - 
mapset - 130220_7001243_0154_AH08R6ADXX.lane_1.nobc
isize_min - 0
sample - ICGC-ABMJ-20101022-23-CD
alignment_required - /panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa
sequencing_type - Paired End Sequencing
source_code - 10:Cell line derived from xenograft
library_type - Fragment
isize_manually_reviewed - 0
aligner - 
donor - APGI_1976
species_reference_genome - 1
isize_max - 0
genome_file - Homo sapiens (GRCh37_ICGC_standard_v2)
material - 1:DNA
=cut

if($slide && $short) {
	if($md->find_metadata("slide", $slide)) {
		my $slide_mapsets	=  $md->slide_mapsets($slide);
		foreach my $mapset (@$slide_mapsets) {
			$md->find_metadata("mapset", $mapset);
	
	        	my $d		= $md->mapset_metadata($mapset);
	
			#print "alignment_required - ".$md->alignment_required($mapset)."\n";
			#print "genome_file        - ".$md->genome_file($mapset)."\n";
			#print "gff_file           - ".$md->gff_file($mapset)."\n";

			print "Mapset:\t\t$d->{'mapset'}\n";
			print "Project:\t$d->{'project'}\n";
			print "Donor:\t\t$d->{'donor'}\n";
			print "Code:\t\t$d->{'source_code'}\n";
			print "Seq type:\t$d->{'sequencing_type'}\n";
			print "Lib type:\t$d->{'library_type'}\n";
			print "Capture kit:\t$d->{'capture_kit'}\n";
			print "Material:\t$d->{'material'}\n";
			print "Genome file:\t$d->{'genome_file'}\n";
			print "----------\n"
		}
	}
}
elsif($slide) {
	if($md->find_metadata("slide", $slide)) {
		my $slide_mapsets	=  $md->slide_mapsets($slide);
		foreach my $mapset (@$slide_mapsets) {
			$md->find_metadata("mapset", $mapset);
	
	        	my $d		= $md->mapset_metadata($mapset);
	
			#print "alignment_required - ".$md->alignment_required($mapset)."\n";
			#print "genome_file        - ".$md->genome_file($mapset)."\n";
			#print "gff_file           - ".$md->gff_file($mapset)."\n";
	
		        foreach my $details (keys %$d){
	       	     		print $details." - ".$d->{$details}."\n";
	        	}
			print "\n"
		}
	}
}
elsif($mapset) {
	$md->find_metadata("mapset", $mapset);

       	my $d		= $md->mapset_metadata($mapset);

	#print "alignment_required - ".$md->alignment_required($mapset)."\n";
	#print "genome_file        - ".$md->genome_file($mapset)."\n";
	#print "gff_file           - ".$md->gff_file($mapset)."\n";

        foreach my $details (keys %$d){
      	     		print $details." - ".$d->{$details}."\n";
       	}
	print "\n"
}
