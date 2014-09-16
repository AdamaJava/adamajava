#!/usr/bin/perl

# convert a strelka indel vcf to a dcc1 file

use strict;
use Data::Dumper;
use Getopt::Long;
use QCMG::Util::Util;
use QCMG::IO::VcfReader;
use QCMG::IO::DccRecord;
use QCMG::IO::Dcc1IndelWriter;

my ($vcf, $dcc1, $donorid, $sampleid);

&GetOptions(
		"i=s"	=> \$vcf,
		"o=s"	=> \$dcc1,
		"d=s"	=> \$donorid,
		"s=s"	=> \$sampleid
	);

if(! $vcf || ! $dcc1 || ! $sampleid) {
	print STDERR "$0 -i file.vcf -o file.dcc1 -s sampleid\n";
	exit(0);
}

my $date	= QCMG::Util::Util::timestamp(FORMAT => 'YYMMDD');

# read single indel VCF, eg. from strelka
my $vr		= QCMG::IO::VcfReader->new(filename => $vcf);
my $dccw	= QCMG::IO::Dcc1IndelWriter->new(
     	          	filename    => $dcc1,
       	       		#version     => 'dcc1_somatic1',
       	       		version     => 'dcc1_indel',
       	       		annotations => {
                 	 	#PatientID        => $donorid,
                   		AnalyzedSampleID => $sampleid,
                   		#ControlSampleID  => 'ICGC-ABMJ-20110401-04-ND',
                   		Tool             => 'strelka',
                   		AnalysisDate     => $date
                   		#AnalysisID       => '1A', 
                   	},
               		verbose  => 0 );

$ENV{'QCMG_HOME'} =~ /.+\/(\w{2})/;
my $initials	= uc($1);
my $atype	= "indel";
my $aid		= join "_", $initials, $atype, $date;
my $mutcount	= 1;

while(my $rec = $vr->next_record()) {
	# parse record to get necessary bits for DCC1 file
	#print Dumper $rec;
=cut
$VAR1 = bless( {
                 'info' => 'IC=0;IHP=3;NT=ref;QSI=39;QSI_NT=38;RC=1;RU=TC;SGT=ref->het;SOMATIC;TQSI=1;TQSI_NT=1',
                 'calls' => [
                              '101:101:50,51:32,36:16,12:90.99:2.15:0.00'
                            ],
                 'filter' => 'PASS',
                 'qual' => '.',
                 'position' => '1126602',
                 'chrom' => 'chr1',
                 'ref_allele' => 'TTC',
                 'format' => 'DP:DP2:TAR:TIR:TOR:DP50:FDP50:SUBDP50',
                 'id' => '.',
                 'alt_allele' => 'T'
               }, 'QCMG::IO::VcfRecord' );
=cut
	my $annot	= $dccw->annotations();

	# get ending position of mutation
	my $end		= $rec->{'position'} + length($rec->{'ref_allele'});

	#my $mut_id	= $annot->{'AnalyzedSampleID'}."_ind".$mutcount++;
	my $mut_id	= $sampleid."_ind".$mutcount++;

	my $mut_type	= 4;	# complex
	$mut_type	= 2 if(length($rec->{'ref_allele'}) == 1);
	$mut_type	= 3 if(length($rec->{'alt_allele'}) == 1);

	my $expallele	= join ">", $rec->{'ref_allele'}, $rec->{'alt_allele'};

	my @recordline	= ();
	push @recordline,
				#$annot->{'AnalysisID'},	# analysis_id		JP_sv1_20130111
				$aid,				# analysis_id		JP_sv1_20130111
				#$annot->{'AnalyzedSampleID'},	# analyzed_sample_id	10XenograftCellLine
				$sampleid,			# analyzed_sample_id	10XenograftCellLine
				#$rec->{'mutation_id'},		# mutation_id		10XenograftCellLine_ind1
				$mut_id,			# mutation_id		10XenograftCellLine_ind1
				$mut_type,			# mutation_type		{2,3,4}
				$rec->{'chrom'},		# chromosome		chr1
				$rec->{'position'},		# chromosome_start	817120
				$end,				# chromosome_end	817130
				"1",				# chromosome_strand	1
				"-999",				# refsnp_allele		-999
				"-999",				# refsnp_strand		-999
				$rec->{'ref_allele'},		# reference_genome_allele CTAAGTCACC
				"-999",				# control_genotype	-999
				"-999",				# tumour_genotype	-999
				$rec->{'alt_allele'},		# mutation		CCTTCAAGATTCAACCTGAATAAATCGCT
				$expallele,			# expressed_allele	CTAAGTCACC>CCTTCAAGATTCAACCTGAATAAATCGCT
				"-999",				# quality_score		-999
				"-999",				# probability		-999
				"-999",				# read_count		-999
				"-999",				# is_annotated		-999
				"-999",				# validation_status	-999
				"-999",				# validation_platform	-999
				"-999",				# xref_esnembl_var_id	-999
				"-999",				# note			-999
				$rec->{'filter'},		# QCMGflag		PASS
				"--",				# ND			--
				"--",				# TD			--
				"--",				# NNS			--
				"--";				# FLankSeq		--

	$dccw->write_record( \@recordline );
}

# print final count of parsed records; decrement counter because it started at 1
# not 0
$mutcount--;
print STDERR $mutcount." records parsed\n";

exit(0);
