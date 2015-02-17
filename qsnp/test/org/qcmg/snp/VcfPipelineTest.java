package org.qcmg.snp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.model.Rule;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.pileup.QSnpRecord;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedRecord;
import org.qcmg.vcf.VCFFileReader;

public class VcfPipelineTest {
	
	private final static char NL = '\n';
	
	@org.junit.Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	@org.junit.Rule
    public ExpectedException thrown= ExpectedException.none();

	@SuppressWarnings("unused")
	private void createVcfFile( File vcfFile, List<String> data ) throws IOException {
		
		try (final FileWriter writer = new FileWriter(vcfFile);){
			// add data
			for (final String s : data) {
				writer.write(s + NL);
			}
		}  
	}
	
	
	@Test
	public void getExistingVcfHeader() throws Exception {
		// create an actual GATK vcf header
		List<String> header = new ArrayList<>();
		
		  header.add("##fileformat=VCFv4.1");
		  header.add("##qUUID=5072595e-8aa2-42df-b5da-dc90a5f2e057");
		  header.add("##FILTER=<ID=LowQual,Description=\"Low quality\">");
		  header.add("##FORMAT=<ID=AD,Number=.,Type=Integer,Description=\"Allelic depths for the ref and alt alleles in the order listed\">");
		  header.add("##FORMAT=<ID=DP,Number=1,Type=Integer,Description=\"Approximate read depth (reads with MQ=255 or with bad mates are filtered)\">");
		  header.add("##FORMAT=<ID=GQ,Number=1,Type=Integer,Description=\"Genotype Quality\">");
		  header.add("##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">");
		  header.add("##FORMAT=<ID=PL,Number=G,Type=Integer,Description=\"Normalized, Phred-scaled likelihoods for genotypes as defined in the VCF specification\">");
		  header.add("##GATKCommandLine=<ID=HaplotypeCaller,Version=3.3-0-g37228af,Date=\"Tue Feb 03 06:10:18 EST 2015\",Epoch=1422907818839,CommandLineOptions=\"analysis_type=HaplotypeCaller input_file=[/mnt/genomeinfo_projects/data/oesophageal/OESO_2804/seq_final/SmgresOesophageal_OESO2804_1DNA_7PrimaryTumour_OESOABNW2012061454TD_IlluminaIGNOutsourcing_NoCapture_Bwa_HiSeq.jpearson.bam] showFullBamList=false read_buffer_size=null phone_home=AWS gatk_key=null tag=NA read_filter=[] intervals=[/mnt/genomeinfo_projects/data/oesophageal/OESO_2804/seq_final/SmgresOesophageal_OESO2804_1DNA_7PrimaryTumour_OESOABNW2012061454TD_IlluminaIGNOutsourcing_NoCapture_Bwa_HiSeq.jpearson.bam.queue/HC_C_OESO_2804-1-sg/temp_01_of_25/scatter.intervals] excludeIntervals=null interval_set_rule=UNION interval_merging=ALL interval_padding=0 reference_sequence=/opt/local/genomeinfo/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa nonDeterministicRandomSeed=false disableDithering=false maxRuntime=-1 maxRuntimeUnits=MINUTES downsampling_type=BY_SAMPLE downsample_to_fraction=null downsample_to_coverage=250 baq=OFF baqGapOpenPenalty=40.0 refactor_NDN_cigar_string=false fix_misencoded_quality_scores=false allow_potentially_misencoded_quality_scores=false useOriginalQualities=false defaultBaseQualities=-1 performanceLog=null BQSR=null quantize_quals=0 disable_indel_quals=false emit_original_quals=false preserve_qscores_less_than=6 globalQScorePrior=-1.0 validation_strictness=SILENT remove_program_records=false keep_program_records=false sample_rename_mapping_file=null unsafe=null disable_auto_index_creation_and_locking_when_reading_rods=false no_cmdline_in_header=false sites_only=false never_trim_vcf_format_field=false bcf=false bam_compression=null simplifyBAM=false disable_bam_indexing=false generate_md5=false num_threads=1 num_cpu_threads_per_data_thread=1 num_io_threads=0 monitorThreadEfficiency=false num_bam_file_handles=null read_group_black_list=null pedigree=[] pedigreeString=[] pedigreeValidationType=STRICT allow_intervals_with_unindexed_bam=false generateShadowBCF=false variant_index_type=DYNAMIC_SEEK variant_index_parameter=-1 logging_level=INFO log_to_file=null help=false version=false out=org.broadinstitute.gatk.engine.io.stubs.VariantContextWriterStub likelihoodCalculationEngine=PairHMM heterogeneousKmerSizeResolution=COMBO_MIN graphOutput=null bamOutput=null bamWriterType=CALLED_HAPLOTYPES disableOptimizations=false dbsnp=(RodBinding name=dbsnp source=/opt/local/genomeinfo/dbsnp/135/00-All_chr.vcf) dontTrimActiveRegions=false maxDiscARExtension=25 maxGGAARExtension=300 paddingAroundIndels=150 paddingAroundSNPs=20 comp=[] annotation=[ClippingRankSumTest, DepthPerSampleHC] excludeAnnotation=[SpanningDeletions, TandemRepeatAnnotator] debug=false useFilteredReadsForAnnotations=false emitRefConfidence=NONE annotateNDA=false heterozygosity=0.001 indel_heterozygosity=1.25E-4 standard_min_confidence_threshold_for_calling=30.0 standard_min_confidence_threshold_for_emitting=30.0 max_alternate_alleles=6 input_prior=[] sample_ploidy=2 genotyping_mode=DISCOVERY alleles=(RodBinding name= source=UNBOUND) contamination_fraction_to_filter=0.0 contamination_fraction_per_sample_file=null p_nonref_model=null exactcallslog=null output_mode=EMIT_VARIANTS_ONLY allSitePLs=false sample_name=null kmerSize=[10, 25] dontIncreaseKmerSizesForCycles=false allowNonUniqueKmersInRef=false numPruningSamples=1 recoverDanglingHeads=false doNotRecoverDanglingBranches=false minDanglingBranchLength=4 consensus=false GVCFGQBands=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 70, 80, 90, 99] indelSizeToEliminateInRefModel=10 min_base_quality_score=10 minPruning=2 gcpHMM=10 includeUmappedReads=false useAllelesTrigger=false phredScaledGlobalReadMismappingRate=45 maxNumHaplotypesInPopulation=128 mergeVariantsViaLD=false doNotRunPhysicalPhasing=true pair_hmm_implementation=VECTOR_LOGLESS_CACHING keepRG=null justDetermineActiveRegions=false dontGenotype=false errorCorrectKmers=false debugGraphTransformations=false dontUseSoftClippedBases=false captureAssemblyFailureBAM=false allowCyclesInKmerGraphToGeneratePaths=false noFpga=false errorCorrectReads=false kmerLengthForReadErrorCorrection=25 minObservationsForKmerToBeSolid=20 pcr_indel_model=CONSERVATIVE maxReadsInRegionPerSample=1000 minReadsPerAlignmentStart=5 activityProfileOut=null activeRegionOut=null activeRegionIn=null activeRegionExtension=null forceActive=false activeRegionMaxSize=null bandPassSigma=null maxProbPropagationDistance=50 activeProbabilityThreshold=0.002 min_mapping_quality_score=20 filter_reads_with_N_cigar=false filter_mismatching_base_and_quals=false filter_bases_not_stored=false\">");
		  header.add("##INFO=<ID=AC,Number=A,Type=Integer,Description=\"Allele count in genotypes, for each ALT allele, in the same order as listed\">");
		  header.add("##INFO=<ID=AF,Number=A,Type=Float,Description=\"Allele Frequency, for each ALT allele, in the same order as listed\">");
		  header.add("##INFO=<ID=AN,Number=1,Type=Integer,Description=\"Total number of alleles in called genotypes\">");
		  header.add("##INFO=<ID=BaseQRankSum,Number=1,Type=Float,Description=\"Z-score from Wilcoxon rank sum test of Alt Vs. Ref base qualities\">");
		  header.add("##INFO=<ID=ClippingRankSum,Number=1,Type=Float,Description=\"Z-score From Wilcoxon rank sum test of Alt vs. Ref number of hard clipped bases\">");
		  header.add("##INFO=<ID=DB,Number=0,Type=Flag,Description=\"dbSNP Membership\">");
		  header.add("##INFO=<ID=DP,Number=1,Type=Integer,Description=\"Approximate read depth; some reads may have been filtered\">");
		  header.add("##INFO=<ID=DS,Number=0,Type=Flag,Description=\"Were any of the samples downsampled?\">");
		  header.add("##INFO=<ID=FS,Number=1,Type=Float,Description=\"Phred-scaled p-value using Fisher's exact test to detect strand bias\">");
		  header.add("##INFO=<ID=HaplotypeScore,Number=1,Type=Float,Description=\"Consistency of the site with at most two segregating haplotypes\">");
		  header.add("##INFO=<ID=InbreedingCoeff,Number=1,Type=Float,Description=\"Inbreeding coefficient as estimated from the genotype likelihoods per-sample when compared against the Hardy-Weinberg expectation\">");
		  header.add("##INFO=<ID=MLEAC,Number=A,Type=Integer,Description=\"Maximum likelihood expectation (MLE) for the allele counts (not necessarily the same as the AC), for each ALT allele, in the same order as listed\">");
		  header.add("##INFO=<ID=MLEAF,Number=A,Type=Float,Description=\"Maximum likelihood expectation (MLE) for the allele frequency (not necessarily the same as the AF), for each ALT allele, in the same order as listed\">");
		  header.add("##INFO=<ID=MQ,Number=1,Type=Float,Description=\"RMS Mapping Quality\">");
		  header.add("##INFO=<ID=MQ0,Number=1,Type=Integer,Description=\"Total Mapping Quality Zero Reads\">");
		  header.add("##INFO=<ID=MQRankSum,Number=1,Type=Float,Description=\"Z-score From Wilcoxon rank sum test of Alt vs. Ref read mapping qualities\">");
		  header.add("##INFO=<ID=QD,Number=1,Type=Float,Description=\"Variant Confidence/Quality by Depth\">");
		  header.add("##INFO=<ID=ReadPosRankSum,Number=1,Type=Float,Description=\"Z-score from Wilcoxon rank sum test of Alt vs. Ref read position bias\">");
		  header.add("##INFO=<ID=SOR,Number=1,Type=Float,Description=\"Symmetric Odds Ratio of 2x2 contingency table to detect strand bias\">");
		  header.add("##contig=<ID=chr1,length=249250621>");
		  header.add("##contig=<ID=chr2,length=243199373>");
		  header.add("##contig=<ID=chr3,length=198022430>");
		  header.add("##contig=<ID=chr4,length=191154276>");
		  header.add("##contig=<ID=chr5,length=180915260>");
		  header.add("##contig=<ID=chr6,length=171115067>");
		  header.add("##contig=<ID=chr7,length=159138663>");
		  header.add("##contig=<ID=chr8,length=146364022>");
		  header.add("##contig=<ID=chr9,length=141213431>");
		  header.add("##contig=<ID=chr10,length=135534747>");
		  header.add("##contig=<ID=chr11,length=135006516>");
		  header.add("##contig=<ID=chr12,length=133851895>");
		  header.add("##contig=<ID=chr13,length=115169878>");
		  header.add("##contig=<ID=chr14,length=107349540>");
		  header.add("##contig=<ID=chr15,length=102531392>");
		  header.add("##contig=<ID=chr16,length=90354753>");
		  header.add("##contig=<ID=chr17,length=81195210>");
		  header.add("##contig=<ID=chr18,length=78077248>");
		  header.add("##contig=<ID=chr19,length=59128983>");
		  header.add("##contig=<ID=chr20,length=63025520>");
		  header.add("##reference=file:///opt/local/genomeinfo/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa");
		  header.add("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	OESO_2804");
		  
		  // create tmp file and shove this stuff in
		  File vcfFile = testFolder.newFile();
		  createVcfFile(vcfFile, header);
		  TestVcfPipeline vp = new TestVcfPipeline(true);
		  vp.testVcfFile = vcfFile.getAbsolutePath();
		  VcfHeader headerFromFile = null;
		  try (VCFFileReader reader = new VCFFileReader(vcfFile);) {
			  headerFromFile = reader.getHeader();
		  }
		  TestVcfPipeline.testVcfHeader = headerFromFile;
		  
		  // check that header order is ok
		  int i = 0;
		  for (VcfHeader.Record rec : headerFromFile) {
			  i++;
			  if (i == 1) {
				  assertEquals("##fileformat=VCFv4.1", rec.getData());
			  } else if (i == 2) {
				  assertEquals(true, rec.getData().startsWith(VcfHeaderUtils.STANDARD_UUID_LINE));
			  } else if (i == 3) {
				  assertEquals(true, rec.getData().startsWith("##GATKCommandLine"));
			  } else if (i >=4 && i < 24) {
				  assertEquals(true, rec.getData().startsWith("##contig"));
			  } else if (i == 24) {
				  assertEquals(true, rec.getData().startsWith("##reference"));
			  } else if (i > 24 && i < 44) {
				  assertEquals(true, rec.getData().startsWith(VcfHeaderUtils.HEADER_LINE_INFO));
			  } else if (i == 44) {
				  assertEquals(true, rec.getData().startsWith(VcfHeaderUtils.HEADER_LINE_FILTER));
			  } else if (i > 44 && i < 50) {
				  assertEquals(true, rec.getData().startsWith(VcfHeaderUtils.HEADER_LINE_FORMAT));
			  } else {
				  assertEquals(true, rec.getData().startsWith("#CHROM"));
			  }
		  }
		  assertEquals(50, i);	// no additional header added at this stage
		  
		  VcfHeader existingHeader = vp.getExistingVCFHeaderDetails();
		  
		  // this should only contain entries that are marked for inclusion
		  for (VcfHeader.Record rec : existingHeader) {
			  assertEquals(true, rec instanceof VcfHeader.FormattedRecord 
							  || rec.getData().startsWith(VcfHeaderUtils.STANDARD_CONTROL_VCF)
							  || rec.getData().startsWith(VcfHeaderUtils.STANDARD_CONTROL_VCF_UUID)
							  || rec.getData().startsWith(VcfHeaderUtils.STANDARD_CONTROL_VCF_GATK_VER)
							  || rec.getData().startsWith(VcfHeaderUtils.STANDARD_TEST_VCF)
							  || rec.getData().startsWith(VcfHeaderUtils.STANDARD_TEST_VCF_GATK_VER)
							  || rec.getData().startsWith(VcfHeaderUtils.STANDARD_TEST_VCF_UUID));
		  }
		  
		  // now check that when calling writeToVcf that we get this along with standard qsnp vcf header
		  
		  File qsnpVcfFile = testFolder.newFile();
		  vp.writeVCF(qsnpVcfFile.getAbsolutePath());
		  
		  VcfHeader finalHeader = null;
		  try (VCFFileReader reader2 = new VCFFileReader(qsnpVcfFile);) {
		  		finalHeader = reader2.getHeader();
		  }
		  
		  List<VcfHeader.Record> finalHeaderRecords = new ArrayList<>();
		  for (VcfHeader.Record rec : finalHeader) {
			  finalHeaderRecords.add(rec);
		  }
		  
		  List<VcfHeader.Record> existingHeaderRecords = new ArrayList<>();
		  for (VcfHeader.Record rec : existingHeader) {
			  existingHeaderRecords.add(rec);
		  }
		  System.out.println("finalHeaderRecords.size(): " + finalHeaderRecords.size());
		  System.out.println("existingHeaderRecords.size(): " + existingHeaderRecords.size());
		  
		  for (VcfHeader.Record rec : existingHeaderRecords) {
			  if ( ! finalHeaderRecords.contains(rec)) {
				  System.out.println("rec not contained in final: " + rec);
				  if (rec.toString().contains("BaseQRankSum")) {
					  
					  System.out.println("rec BaseQRankSum hashCode: " + rec.hashCode());
					  // loop through finals
					  for (VcfHeader.Record finalRec : finalHeaderRecords) {
						  if (finalRec.toString().contains("BaseQRankSum")) {
							  System.out.println("finalRec: " + finalRec);
							  assertEquals(rec, finalRec);
							  
							  String line1 = rec.toString();
							  String line2 = finalRec.toString();
							  assertEquals(true, line1.equals(line2));
							  assertEquals(line1.hashCode(), line1.hashCode());
							  
							  System.out.println("finalRec: BaseQRankSum hashCode: " + finalRec.hashCode());
						  }
					  }
				  }
			  } else {
				  System.out.println("rec IS contained in final: " + rec);
			  }
		  }
		  assertEquals(true, finalHeaderRecords.containsAll(existingHeaderRecords));
	}
	
	
	@Test
	public void doesQsnpRecordMergeTheUnderlyingVcfRecords() {
		String [] params1 = "chr1	568824	.	C	T	351.77	.;MIN	AC=1;AF=0.500;AN=2;BaseQRankSum=0.662;ClippingRankSum=-0.489;DP=52;FS=33.273;MLEAC=1;MLEAF=0.500;MQ=36.25;MQ0=0;MQRankSum=-0.576;QD=6.76;ReadPosRankSum=1.835;SOR=2.608;SOMATIC;MR=25;NNS=25	GT:AD:DP:GQ:PL	0/1:40,12:52:99:380,0,2518".split("\t");
		String [] params2 = "chr1	568824	.	C	T	351.77	.;MIN	AC=1;AF=0.500;AN=2;BaseQRankSum=0.662;ClippingRankSum=-0.489;DP=52;FS=33.273;MLEAC=1;MLEAF=0.500;MQ=36.25;MQ0=0;MQRankSum=-0.576;QD=6.76;ReadPosRankSum=1.835;SOR=2.608;SOMATIC;MR=15;NNS=15	GT:AD:DP:GQ:PL	1/1:50,22:62:99:380,0,2518".split("\t");
		VcfRecord vcf1 = new VcfRecord(params1);
		VcfRecord vcf2 = new VcfRecord(params2);
		assertEquals(vcf1.getFormatFields().size(), 2);
		assertEquals(vcf2.getFormatFields().size(), 2);
		QSnpRecord snp1 = new QSnpRecord(vcf1);
		QSnpRecord snp2 = new QSnpRecord(vcf2);
		
		TestVcfPipeline vp = new TestVcfPipeline();
		QSnpRecord merged = vp.getQSnpRecord(snp1, snp2);
		assertEquals(merged.getVcfRecord().getFormatFields().size(), 3);
		
		// header
		assertEquals("GT:AD:DP:GQ:PL", merged.getVcfRecord().getFormatFields().get(0));
		// control
		assertEquals("0/1:40,12:52:99:380,0,2518", merged.getVcfRecord().getFormatFields().get(1));
		// test
		assertEquals("1/1:50,22:62:99:380,0,2518", merged.getVcfRecord().getFormatFields().get(2));
	}
	
	@Test
	public void testPileupPipelineEmptyPileupFile() throws Exception {
		final File logFile = testFolder.newFile("qsnp.log");
		final File iniFile = testFolder.newFile("qsnp.ini");
		IniFileGenerator.createRulesOnlyIni(iniFile);
		
		final File pileupInput = testFolder.newFile("input.pileup");
		final File vcfOutput = testFolder.newFile("output.vcf");
		
//		PileupFileGenerator.createPileupFile(pileupInput);
		
		IniFileGenerator.addInputFiles(iniFile, false, "pileup = " + pileupInput.getAbsolutePath());
		IniFileGenerator.addOutputFiles(iniFile, false, "vcf = " + vcfOutput.getAbsolutePath());
		
		// that should be it
		final String command = "-log " + logFile.getAbsolutePath() + " -i " + iniFile.getAbsolutePath();
		final Executor exec = new Executor(command, "org.qcmg.snp.Main");
		assertEquals(1, exec.getErrCode());
	}
	
	@Test
	public void testPileupPipelineGenerateVCFOnly() throws Exception {
		
		final File logFile = testFolder.newFile("qsnp.log");
		final File iniFile = testFolder.newFile("qsnp.ini");
		IniFileGenerator.createRulesOnlyIni(iniFile);
		
		final File pileupInput = testFolder.newFile("input.pileup");
		final File vcfOutput = testFolder.newFile("output.vcf");
		
		PileupFileGenerator.createPileupFile(pileupInput);
		
		IniFileGenerator.addInputFiles(iniFile, false, "pileup = " + pileupInput.getAbsolutePath());
		IniFileGenerator.addOutputFiles(iniFile, false, "vcf = " + vcfOutput.getAbsolutePath());
		
		// add runType to ini file
		IniFileGenerator.addStringToIniFile(iniFile, "[parameters]\nrunMode = pileup", true);	// append to file
		
		// that should be it
		ExpectedException.none();
		final String command = "-log " + logFile.getAbsolutePath() + " -i " + iniFile.getAbsolutePath();
		final Executor exec = new Executor(command, "org.qcmg.snp.Main");
		assertEquals(0, exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		
		// check the vcf output file
		assertEquals(1, noOfLinesInVCFOutputFile(vcfOutput));
	}
	
	@Test
	public void testPileupPipelineGenerateVCFOnlyIncludeIndels() throws Exception {
		final File logFile = testFolder.newFile("qsnp.log");
		final File iniFile = testFolder.newFile("qsnp.ini");
		IniFileGenerator.createRulesOnlyIni(iniFile);
		
		final File pileupInput = testFolder.newFile("input.pileup");
		final File vcfOutput = testFolder.newFile("output.vcf");
		
		PileupFileGenerator.createPileupFile(pileupInput);
		
		IniFileGenerator.addInputFiles(iniFile, false, "pileup = " + pileupInput.getAbsolutePath());
		IniFileGenerator.addOutputFiles(iniFile, false, "vcf = " + vcfOutput.getAbsolutePath());
		IniFileGenerator.addStringToIniFile(iniFile, "[parameters]\nincludeIndels = true", true);
		
		// add runType to ini file
		IniFileGenerator.addStringToIniFile(iniFile, "\nrunMode = pileup", true);	// append to file
		
		// that should be it
		ExpectedException.none();
		final String command = "-log " + logFile.getAbsolutePath() + " -i " + iniFile.getAbsolutePath();
		final Executor exec = new Executor(command, "org.qcmg.snp.Main");
		assertEquals(0, exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		
		// check the vcf output file
		assertEquals(2, noOfLinesInVCFOutputFile(vcfOutput));
	}
	
	@Test
	public void testPileupPipelineDCCMode() throws Exception{
		final File logFile = testFolder.newFile("qsnp.log");
		final File iniFile = testFolder.newFile("qsnp.ini");
		IniFileGenerator.createRulesOnlyIni(iniFile);
		
		final File pileupInput = testFolder.newFile("input.pileup");
		final File vcfOutput = testFolder.newFile("output.vcf");
		
		PileupFileGenerator.createPileupFile(pileupInput);
		
		IniFileGenerator.addInputFiles(iniFile, false, "pileup = " + pileupInput.getAbsolutePath());
		
		IniFileGenerator.addOutputFiles(iniFile, false, "vcf = " + vcfOutput.getAbsolutePath()); 
		IniFileGenerator.addStringToIniFile(iniFile, "[parameters]\nincludeIndels = true", true);
		
		// add the annotate mode=dcc to the ini file
//		IniFileGenerator.addStringToIniFile(iniFile, "\nannotateMode = dcc", true);
		// add runType to ini file
		IniFileGenerator.addStringToIniFile(iniFile, "\nrunMode = pileup", true);	// append to file
		
		// that should be it
		ExpectedException.none();
		final String command = "-log " + logFile.getAbsolutePath() + " -i " + iniFile.getAbsolutePath();
		final Executor exec = new Executor(command, "org.qcmg.snp.Main");
		assertEquals(0, exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		
		// check the vcf output file
		assertEquals(2, noOfLinesInVCFOutputFile(vcfOutput));
	}
	
	private int noOfLinesInVCFOutputFile(File vcfOutput) throws Exception {
		final VCFFileReader reader = new VCFFileReader(vcfOutput);
		int noOfLines = 0;
		try {
			for (final VcfRecord vcf : reader) noOfLines++;
		} finally {
			reader.close();
		}
		return noOfLines;
	}
	
	private int noOfLinesInDCCOutputFile(File dccFile) throws Exception {
		final TabbedFileReader reader = new TabbedFileReader(dccFile);
		int noOfLines = 0;
		try {
			for (final TabbedRecord vcf : reader) {
				if (vcf.getData().startsWith("analysis")) continue;	// header line
				noOfLines++;
			}
		} finally {
			reader.close();
		}
		return noOfLines;
	}
	
	@Test
	public void testIsRecordAKeeper() {
		// arguments are.....
		// int variantCount, int coverage, Rule rule, List<PileupElement> baseCounts, double percentage
		
		final Rule r = new Rule(0, 20, 1);
		final List<PileupElement> pes = new ArrayList<PileupElement>();
		
		assertEquals(false, PileupPipeline.isPileupRecordAKeeper(0, 0, r, null, 0));
		assertEquals(false, PileupPipeline.isPileupRecordAKeeper(0, 0, r, pes, 0));
		
		try {	// variant count is greater than total count
			assertEquals(false, PileupPipeline.isPileupRecordAKeeper(1, 0, r, pes, 0));
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (final IllegalArgumentException e) {}
		
		assertEquals(false, PileupPipeline.isPileupRecordAKeeper(1, 1, r, pes, 0));
		final PileupElement pe = new PileupElement('A');
		pe.incrementForwardCount((byte)'I');
		pes.add(pe);
		assertEquals(true, PileupPipeline.isPileupRecordAKeeper(1, 1, r, pes, 0));
		
		//. change rule
		assertEquals(false, PileupPipeline.isPileupRecordAKeeper(1, 1, new Rule(0,20,2), pes, 0));
		pe.incrementReverseCount((byte)'I');
		assertEquals(true, PileupPipeline.isPileupRecordAKeeper(2, 2, new Rule(0,20,2), pes, 0));
		assertEquals(false, PileupPipeline.isPileupRecordAKeeper(2, 2, new Rule(0,20,4), pes, 0));
		
		// only use percentage if we are dealing with the upper bounded rule
		assertEquals(true, PileupPipeline.isPileupRecordAKeeper(2, 100, new Rule(0,Integer.MAX_VALUE,4), pes, 0));
		assertEquals(false, PileupPipeline.isPileupRecordAKeeper(1, 100, new Rule(0,Integer.MAX_VALUE,4), pes, 0));
		
		final PileupElement pe2 = new PileupElement('.');
		pe2.incrementForwardCount((byte)'I');
		pe2.incrementForwardCount((byte)'I');
		pe2.incrementForwardCount((byte)'I');
		pe2.incrementForwardCount((byte)'I');
		pe2.incrementForwardCount((byte)'I');
		pe2.incrementForwardCount((byte)'I');
		pe2.incrementForwardCount((byte)'I');
		pes.add(pe2);
		assertEquals(false, PileupPipeline.isPileupRecordAKeeper(2, 9, new Rule(0,20,2), pes, 50));
		assertEquals(true, PileupPipeline.isPileupRecordAKeeper(2, 9, new Rule(0,20,2), pes, 10));
	}
	
	@Test
	public void testIsVariantOnBothStrands() {
		assertEquals(false, PileupPipeline.isVariantOnBothStrands(null));
		final List<PileupElement> pes = new ArrayList<PileupElement>();
		assertEquals(false, PileupPipeline.isVariantOnBothStrands(pes));
		final PileupElement pe = new PileupElement('.');
		pe.incrementForwardCount((byte)'I');
		pes.add(pe);
		assertEquals(false, PileupPipeline.isVariantOnBothStrands(pes));
		final PileupElement pe2 = new PileupElement('C');
		pe2.incrementForwardCount((byte)'I');
		pes.add(pe2);
		assertEquals(false, PileupPipeline.isVariantOnBothStrands(pes));
		pe2.incrementReverseCount((byte)'?');
		assertEquals(true, PileupPipeline.isVariantOnBothStrands(pes));
		
		pes.clear();
		final PileupElement pe3 = new PileupElement('T');
		pe3.incrementReverseCount((byte)'I');
		pes.add(pe3);
		assertEquals(false, PileupPipeline.isVariantOnBothStrands(pes));
		pe3.incrementForwardCount((byte)'I');
		assertEquals(true, PileupPipeline.isVariantOnBothStrands(pes));
	}
}
