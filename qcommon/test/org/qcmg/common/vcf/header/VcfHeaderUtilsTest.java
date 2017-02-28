package org.qcmg.common.vcf.header;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class VcfHeaderUtilsTest {
	
	 
	 
	@Test
	public void addSampleIdTest(){
		//VcfHeader header, String id, boolean isTest
		VcfHeader header = new VcfHeader();
		header.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE);
		
		VcfHeaderUtils.addSampleId(header, "TEST", 1);			
		Assert.assertArrayEquals( header.getSampleId(), new String[]{"TEST"});
				 
		VcfHeaderUtils.addSampleId(header, "CONTROL", 3);	
		Assert.assertArrayEquals( header.getSampleId(), new String[]{"TEST","null","CONTROL"});
	}
	
	//@Test unit test don't have jar file so won't have tool name and version
	@Test
	public void addQPGEntryNullOrEmpty() {
		
		try {
			VcfHeaderUtils.addQPGLineToHeader(null, null, null, null);
			Assert.fail("Should have thrown an IAE");
		} catch (final IllegalArgumentException iae){}
		
		final VcfHeader header = new VcfHeader();
		
		try {
			VcfHeaderUtils.addQPGLineToHeader(header, null, null, null);
			Assert.fail("Should have thrown an IAE");
		} catch (final IllegalArgumentException iae){}
		try {
			VcfHeaderUtils.addQPGLineToHeader(header, "", "", "");
			Assert.fail("Should have thrown an IAE");
		} catch (final IllegalArgumentException iae){}
	}
	
	@Test
	public void mergeHeaders() {
		
		try {
			VcfHeaderUtils.mergeHeaders(null, null,true);
			Assert.fail("Should have thrown an IAE");
		} catch (final IllegalArgumentException iae){}
		
		VcfHeader header = new VcfHeader();
		assertEquals(header, VcfHeaderUtils.mergeHeaders(header, null,true));
		assertEquals(header, VcfHeaderUtils.mergeHeaders(null, header,true));
		assertEquals(header, VcfHeaderUtils.mergeHeaders(header, header,true));
		assertEquals(header, VcfHeaderUtils.mergeHeaders(header, header,false));
		
		
		VcfHeader h2 = new VcfHeader();
//		h2.addOrReplace("##hello");		
//		VcfHeaderRecord h2HelloRec = h2.getOtherRecords().get(0);
//		// not a filter, format, info or meta line, it won't be merged into header
//		assertEquals(false, VcfHeaderUtils.mergeHeaders(header, h2,false).getOtherRecords().contains(h2HelloRec));
		
		h2.addFilter("mySpecialFilter", "foo > bar");		// add a filter line
		VcfHeaderRecord h2FilterRec = h2.getFilterRecord("mySpecialFilter");
		assertEquals(true, VcfHeaderUtils.mergeHeaders(header, h2,false).getFilterRecord(h2FilterRec.getId()) != null );
		
		h2.addFilter("mySpecialFilter", "foo == bar");		// add a filter line //replace previous filter
//		VcfHeaderRecord h2FilterRec2 = h2.getFilterRecord("mySpecialFilter"); // get filter with foo == bar
		assertEquals(true, VcfHeaderUtils.mergeHeaders(header, h2, false).getFilterRecords().contains(h2FilterRec));
		assertEquals(false, VcfHeaderUtils.mergeHeaders(header, h2,true).getFilterRecords().contains(h2FilterRec));
		
		h2.addInfo("hotline", "1", "Flag", "This is my info line");		// add a filter line
		VcfHeaderRecord h2InfoRec = h2.getInfoRecord("hotline");
		assertEquals(true, VcfHeaderUtils.mergeHeaders(header, h2,false).getInfoRecords().contains(h2InfoRec));
		h2.addInfo("hotline", "2", "Flag", "This is my info line");		// add a filter line
		h2InfoRec = h2.getInfoRecord("hotline");
		assertEquals(false, VcfHeaderUtils.mergeHeaders(header, h2,false).getInfoRecords().contains(h2InfoRec));
		assertEquals(true, VcfHeaderUtils.mergeHeaders(header, h2,true).getInfoRecords().contains(h2InfoRec));
		
		h2.addFormat("BAM", "1", "Flag", "This is my format line");		// add a filter line
		VcfHeaderRecord h2FormatRec = h2.getFormatRecord("BAM");
		assertEquals(true, VcfHeaderUtils.mergeHeaders(header, h2,false).getFormatRecords().contains(h2FormatRec));
		h2.addFormat("BAM", "1", "String", "This is my format line");		// add a filter line
		h2FormatRec = h2.getFormatRecord("BAM");
		assertEquals(false, VcfHeaderUtils.mergeHeaders(header, h2,false).getFormatRecords().contains(h2FormatRec));
		assertEquals(true, VcfHeaderUtils.mergeHeaders(header, h2,true).getFormatRecords().contains(h2FormatRec));
	}
	
	@Test
	public void mergeHeadersGATK() {
		List<String> headerLines = new ArrayList<>();
		headerLines.add("##fileformat=VCFv4.2");
		headerLines.add("##fileDate=20150217");
//		headerLines.add("##"); throw exception
		headerLines.add("##qUUID=542b1d48_a8ea_44e7_9e17_31bf8b526a6d");
		headerLines.add("##qSource=qSNP v2.0 (521)");
		headerLines.add("##qPG=<ID=1,Tool=qsnp,Version=2.0 (521),Date=2015-02-17 11:21:23,CL=\"qsnp -i /mnt/genomeinfo_projects/data/oesophageal/OESO_0054/variants/GATK/0a8c58d2_c97b_4f65_9fc9_5114e91fc526/OESO_0054.ini -log /mnt/genomeinfo_projects/data/oesophageal/OESO_0054/variants/GATK/0a8c58d2_c97b_4f65_9fc9_5114e91fc526/qsnp.log [runMode: vcf]\">");
		headerLines.add("##qDonorId=OESO_0054");
		headerLines.add("##qControlSample=SMGres-ABNW-20140115-006");
		headerLines.add("##qTestSample=SMGres-ABNW-20140115-031");
		headerLines.add("##qControlBam=/mnt/genomeinfo_projects/data/oesophageal/OESO_0054/seq_final/SmgresOesophageal_OESO0054_1DNA_1NormalBlood_SMGresABNW20140115006_IlluminaTruSEQMultiplexedManual_HumanTruSEQExomeEnrichmentTruSEQ_Bwa_HiSeq.jpearson.bam");
		headerLines.add("##qControlBamUUID=2a01aa35-a89a-4cfb-804b-e48ce6c1249f");
		headerLines.add("##qTestBam=/mnt/genomeinfo_projects/data/oesophageal/OESO_0054/seq_final/SmgresOesophageal_OESO0054_1DNA_7PrimaryTumour_SMGresABNW20140115031_IlluminaTruSEQMultiplexedManual_HumanTruSEQExomeEnrichmentTruSEQ_Bwa_HiSeq.jpearson.bam");
		headerLines.add("##qTestBamUUID=acea6562-c714-4502-a9e9-c88bc85ece47");
		headerLines.add("##qAnalysisId=0a8c58d2_c97b_4f65_9fc9_5114e91fc526");
		headerLines.add("##qControlVcf=/mnt/genomeinfo_projects/data/oesophageal/OESO_0054/seq_final/SmgresOesophageal_OESO0054_1DNA_1NormalBlood_SMGresABNW20140115006_IlluminaTruSEQMultiplexedManual_HumanTruSEQExomeEnrichmentTruSEQ_Bwa_HiSeq.jpearson.unfiltered.vcf");
		headerLines.add("##qControlVcfUUID=5072595e-8aa2-42df-b5da-dc90a5f2e057");
		headerLines.add("##qControlVcfGATKVersion=3.3-0-g37228af");
		headerLines.add("##qTestVcf=/mnt/genomeinfo_projects/data/oesophageal/OESO_0054/seq_final/SmgresOesophageal_OESO0054_1DNA_7PrimaryTumour_SMGresABNW20140115031_IlluminaTruSEQMultiplexedManual_HumanTruSEQExomeEnrichmentTruSEQ_Bwa_HiSeq.jpearson.unfiltered.vcf");
		headerLines.add("##qTestVcfUUID=8b87f064-677e-4708-8c0f-8ecea9084eb9");
		headerLines.add("##qTestVcfGATKVersion=3.3-0-g37228af");
		VcfHeader gatkHeader = new VcfHeader(headerLines);
		VcfHeader orig = new VcfHeader();
		
		orig = VcfHeaderUtils.mergeHeaders(orig, gatkHeader, true);
		assertEquals(0, orig.getFilterRecords().size());
		assertEquals(0, orig.getFormatRecords().size());
		assertEquals(0, orig.getInfoRecords().size());
		assertEquals(18, orig.getAllMetaRecords().size()); //excludes qPG, empty ##
//		assertEquals(14, orig.getMetaRecords().size());
	}
	
	@Test
	public void addQPGEntry() {
		final VcfHeader header = new VcfHeader();
		assertEquals(0, VcfHeaderUtils.getqPGRecords(header).size());
		
		final String tool = "qsnp";
		final String version = "1.0";
		final String cl = "qsnp -i ini_file.ini -log log_file.log";
		VcfHeaderUtils.addQPGLineToHeader(header, tool, version, cl);
		assertEquals(1, VcfHeaderUtils.getqPGRecords(header).size());
		VcfHeaderRecord qpg = VcfHeaderUtils.getqPGRecords(header).get(0);
		assertEquals(1, VcfHeaderUtils.getQPGOrder(  qpg));
		assertEquals(tool, VcfHeaderUtils.getQPGTool(qpg));
		assertEquals(version, VcfHeaderUtils.getQPGVersion(qpg));
		assertEquals("\"" + cl + "\"", VcfHeaderUtils.getQPGCommandLine(qpg));
		
		VcfHeaderUtils.addQPGLineToHeader(header, tool, version, cl);
 		assertEquals(2, VcfHeaderUtils.getqPGRecords(header).size());
 		assertEquals(2, VcfHeaderUtils.getQPGOrder(VcfHeaderUtils.getqPGRecords(header).get(0))); 		
 		assertEquals(1, VcfHeaderUtils.getQPGOrder(VcfHeaderUtils.getqPGRecords(header).get(1))); 		
 		assertEquals(tool, VcfHeaderUtils.getQPGTool(VcfHeaderUtils.getqPGRecords(header).get(0))); 		
 		assertEquals(version, VcfHeaderUtils.getQPGVersion(VcfHeaderUtils.getqPGRecords(header).get(0))); 		
 		assertEquals("\"" + cl + "\"", VcfHeaderUtils.getQPGCommandLine(VcfHeaderUtils.getqPGRecords(header).get(0)));
				
		VcfHeaderUtils.addQPGLineToHeader(header, tool, version, cl);		
		assertEquals(3, VcfHeaderUtils.getqPGRecords(header).size());
		assertEquals(3, VcfHeaderUtils.getQPGOrder(VcfHeaderUtils.getqPGRecords(header).get(0)));
	}
	
	@Test
	public void checkOutGATKVersion() {
		try {
			VcfHeaderUtils.getGATKVersionFromHeaderLine(null);
			Assert.fail("Should have thrown an IAE");
		} catch (final IllegalArgumentException iae){}
		
		VcfHeader header = new VcfHeader();
		header.addOrReplace("##fileformat=VCFv4.1");
		header.addOrReplace("##qUUID=8b87f064-677e-4708-8c0f-8ecea9084eb9");
		header.addOrReplace("##FILTER=<ID=LowQual,Description=\"Low quality\">");
		header.addOrReplace("##FORMAT=<ID=AD,Number=.,Type=Integer,Description=\"Allelic depths for the ref and alt alleles in the order listed\">");
		header.addOrReplace("##GATKCommandLine=<ID=HaplotypeCaller,Version=3.3-0-g37228af,Date=\"Mon Feb 16 15:28:52 EST 2015\",Epoch=1424064532168,CommandLineOptions=\"analysis_type=HaplotypeCaller input_file=[/mnt/genomeinfo_projects/data/oesophageal/OESO_0054/seq_final/SmgresOesophageal_OESO0054_1DNA_7PrimaryTumour_SMGresABNW20140115031_IlluminaTruSEQMultiplexedManual_HumanTruSEQExomeEnrichmentTruSEQ_Bwa_HiSeq.jpearson.bam] showFullBamList=false read_buffer_size=null phone_home=AWS gatk_key=null tag=NA read_filter=[] intervals=[/mnt/genomeinfo_projects/data/oesophageal/OESO_0054/seq_final/SmgresOesophageal_OESO0054_1DNA_7PrimaryTumour_SMGresABNW20140115031_IlluminaTruSEQMultiplexedManual_HumanTruSEQExomeEnrichmentTruSEQ_Bwa_HiSeq.jpearson.bam.queue/HC_C_OESO_0054-1-sg/temp_01_of_25/scatter.intervals] excludeIntervals=null interval_set_rule=UNION interval_merging=ALL interval_padding=0 reference_sequence=/opt/local/genomeinfo/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa nonDeterministicRandomSeed=false disableDithering=false maxRuntime=-1 maxRuntimeUnits=MINUTES downsampling_type=BY_SAMPLE downsample_to_fraction=null downsample_to_coverage=250 baq=OFF baqGapOpenPenalty=40.0 refactor_NDN_cigar_string=false fix_misencoded_quality_scores=false allow_potentially_misencoded_quality_scores=false useOriginalQualities=false defaultBaseQualities=-1 performanceLog=null BQSR=null quantize_quals=0 disable_indel_quals=false emit_original_quals=false preserve_qscores_less_than=6 globalQScorePrior=-1.0 validation_strictness=SILENT remove_program_records=false keep_program_records=false sample_rename_mapping_file=null unsafe=null disable_auto_index_creation_and_locking_when_reading_rods=false no_cmdline_in_header=false sites_only=false never_trim_vcf_format_field=false bcf=false bam_compression=null simplifyBAM=false disable_bam_indexing=false generate_md5=false num_threads=1 num_cpu_threads_per_data_thread=1 num_io_threads=0 monitorThreadEfficiency=false num_bam_file_handles=null read_group_black_list=null pedigree=[] pedigreeString=[] pedigreeValidationType=STRICT allow_intervals_with_unindexed_bam=false generateShadowBCF=false variant_index_type=DYNAMIC_SEEK variant_index_parameter=-1 logging_level=INFO log_to_file=null help=false version=false out=org.broadinstitute.gatk.engine.io.stubs.VariantContextWriterStub likelihoodCalculationEngine=PairHMM heterogeneousKmerSizeResolution=COMBO_MIN graphOutput=null bamOutput=null bamWriterType=CALLED_HAPLOTYPES disableOptimizations=false dbsnp=(RodBinding name=dbsnp source=/opt/local/genomeinfo/dbsnp/135/00-All_chr.vcf) dontTrimActiveRegions=false maxDiscARExtension=25 maxGGAARExtension=300 paddingAroundIndels=150 paddingAroundSNPs=20 comp=[] annotation=[ClippingRankSumTest, DepthPerSampleHC] excludeAnnotation=[SpanningDeletions, TandemRepeatAnnotator] debug=false useFilteredReadsForAnnotations=false emitRefConfidence=NONE annotateNDA=false heterozygosity=0.001 indel_heterozygosity=1.25E-4 standard_min_confidence_threshold_for_calling=30.0 standard_min_confidence_threshold_for_emitting=30.0 max_alternate_alleles=6 input_prior=[] sample_ploidy=2 genotyping_mode=DISCOVERY alleles=(RodBinding name= source=UNBOUND) contamination_fraction_to_filter=0.0 contamination_fraction_per_sample_file=null p_nonref_model=null exactcallslog=null output_mode=EMIT_VARIANTS_ONLY allSitePLs=false sample_name=null kmerSize=[10, 25] dontIncreaseKmerSizesForCycles=false allowNonUniqueKmersInRef=false numPruningSamples=1 recoverDanglingHeads=false doNotRecoverDanglingBranches=false minDanglingBranchLength=4 consensus=false GVCFGQBands=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 70, 80, 90, 99] indelSizeToEliminateInRefModel=10 min_base_quality_score=10 minPruning=2 gcpHMM=10 includeUmappedReads=false useAllelesTrigger=false phredScaledGlobalReadMismappingRate=45 maxNumHaplotypesInPopulation=128 mergeVariantsViaLD=false doNotRunPhysicalPhasing=true pair_hmm_implementation=VECTOR_LOGLESS_CACHING keepRG=null justDetermineActiveRegions=false dontGenotype=false errorCorrectKmers=false debugGraphTransformations=false dontUseSoftClippedBases=false captureAssemblyFailureBAM=false allowCyclesInKmerGraphToGeneratePaths=false noFpga=false errorCorrectReads=false kmerLengthForReadErrorCorrection=25 minObservationsForKmerToBeSolid=20 pcr_indel_model=CONSERVATIVE maxReadsInRegionPerSample=1000 minReadsPerAlignmentStart=5 activityProfileOut=null activeRegionOut=null activeRegionIn=null activeRegionExtension=null forceActive=false activeRegionMaxSize=null bandPassSigma=null maxProbPropagationDistance=50 activeProbabilityThreshold=0.002 min_mapping_quality_score=20 filter_reads_with_N_cigar=false filter_mismatching_base_and_quals=false filter_bases_not_stored=false\">");
		
		assertEquals("3.3-0-g37228af", VcfHeaderUtils.getGATKVersionFromHeaderLine(header));
	}
	@Test
	public void checkOutUUIDRetrieval() {
		VcfHeader header = new VcfHeader();
		header.addOrReplace("##fileformat=VCFv4.1");
		header.addOrReplace("##FILTER=<ID=LowQual,Description=\"Low quality\">");
		header.addOrReplace("##FORMAT=<ID=AD,Number=.,Type=Integer,Description=\"Allelic depths for the ref and alt alleles in the order listed\">");
		header.addOrReplace("##GATKCommandLine=<ID=HaplotypeCaller,Version=3.3-0-g37228af,Date=\"Mon Feb 16 15:28:52 EST 2015\",Epoch=1424064532168,CommandLineOptions=\"analysis_type=HaplotypeCaller input_file=[/mnt/genomeinfo_projects/data/oesophageal/OESO_0054/seq_final/SmgresOesophageal_OESO0054_1DNA_7PrimaryTumour_SMGresABNW20140115031_IlluminaTruSEQMultiplexedManual_HumanTruSEQExomeEnrichmentTruSEQ_Bwa_HiSeq.jpearson.bam] showFullBamList=false read_buffer_size=null phone_home=AWS gatk_key=null tag=NA read_filter=[] intervals=[/mnt/genomeinfo_projects/data/oesophageal/OESO_0054/seq_final/SmgresOesophageal_OESO0054_1DNA_7PrimaryTumour_SMGresABNW20140115031_IlluminaTruSEQMultiplexedManual_HumanTruSEQExomeEnrichmentTruSEQ_Bwa_HiSeq.jpearson.bam.queue/HC_C_OESO_0054-1-sg/temp_01_of_25/scatter.intervals] excludeIntervals=null interval_set_rule=UNION interval_merging=ALL interval_padding=0 reference_sequence=/opt/local/genomeinfo/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa nonDeterministicRandomSeed=false disableDithering=false maxRuntime=-1 maxRuntimeUnits=MINUTES downsampling_type=BY_SAMPLE downsample_to_fraction=null downsample_to_coverage=250 baq=OFF baqGapOpenPenalty=40.0 refactor_NDN_cigar_string=false fix_misencoded_quality_scores=false allow_potentially_misencoded_quality_scores=false useOriginalQualities=false defaultBaseQualities=-1 performanceLog=null BQSR=null quantize_quals=0 disable_indel_quals=false emit_original_quals=false preserve_qscores_less_than=6 globalQScorePrior=-1.0 validation_strictness=SILENT remove_program_records=false keep_program_records=false sample_rename_mapping_file=null unsafe=null disable_auto_index_creation_and_locking_when_reading_rods=false no_cmdline_in_header=false sites_only=false never_trim_vcf_format_field=false bcf=false bam_compression=null simplifyBAM=false disable_bam_indexing=false generate_md5=false num_threads=1 num_cpu_threads_per_data_thread=1 num_io_threads=0 monitorThreadEfficiency=false num_bam_file_handles=null read_group_black_list=null pedigree=[] pedigreeString=[] pedigreeValidationType=STRICT allow_intervals_with_unindexed_bam=false generateShadowBCF=false variant_index_type=DYNAMIC_SEEK variant_index_parameter=-1 logging_level=INFO log_to_file=null help=false version=false out=org.broadinstitute.gatk.engine.io.stubs.VariantContextWriterStub likelihoodCalculationEngine=PairHMM heterogeneousKmerSizeResolution=COMBO_MIN graphOutput=null bamOutput=null bamWriterType=CALLED_HAPLOTYPES disableOptimizations=false dbsnp=(RodBinding name=dbsnp source=/opt/local/genomeinfo/dbsnp/135/00-All_chr.vcf) dontTrimActiveRegions=false maxDiscARExtension=25 maxGGAARExtension=300 paddingAroundIndels=150 paddingAroundSNPs=20 comp=[] annotation=[ClippingRankSumTest, DepthPerSampleHC] excludeAnnotation=[SpanningDeletions, TandemRepeatAnnotator] debug=false useFilteredReadsForAnnotations=false emitRefConfidence=NONE annotateNDA=false heterozygosity=0.001 indel_heterozygosity=1.25E-4 standard_min_confidence_threshold_for_calling=30.0 standard_min_confidence_threshold_for_emitting=30.0 max_alternate_alleles=6 input_prior=[] sample_ploidy=2 genotyping_mode=DISCOVERY alleles=(RodBinding name= source=UNBOUND) contamination_fraction_to_filter=0.0 contamination_fraction_per_sample_file=null p_nonref_model=null exactcallslog=null output_mode=EMIT_VARIANTS_ONLY allSitePLs=false sample_name=null kmerSize=[10, 25] dontIncreaseKmerSizesForCycles=false allowNonUniqueKmersInRef=false numPruningSamples=1 recoverDanglingHeads=false doNotRecoverDanglingBranches=false minDanglingBranchLength=4 consensus=false GVCFGQBands=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 70, 80, 90, 99] indelSizeToEliminateInRefModel=10 min_base_quality_score=10 minPruning=2 gcpHMM=10 includeUmappedReads=false useAllelesTrigger=false phredScaledGlobalReadMismappingRate=45 maxNumHaplotypesInPopulation=128 mergeVariantsViaLD=false doNotRunPhysicalPhasing=true pair_hmm_implementation=VECTOR_LOGLESS_CACHING keepRG=null justDetermineActiveRegions=false dontGenotype=false errorCorrectKmers=false debugGraphTransformations=false dontUseSoftClippedBases=false captureAssemblyFailureBAM=false allowCyclesInKmerGraphToGeneratePaths=false noFpga=false errorCorrectReads=false kmerLengthForReadErrorCorrection=25 minObservationsForKmerToBeSolid=20 pcr_indel_model=CONSERVATIVE maxReadsInRegionPerSample=1000 minReadsPerAlignmentStart=5 activityProfileOut=null activeRegionOut=null activeRegionIn=null activeRegionExtension=null forceActive=false activeRegionMaxSize=null bandPassSigma=null maxProbPropagationDistance=50 activeProbabilityThreshold=0.002 min_mapping_quality_score=20 filter_reads_with_N_cigar=false filter_mismatching_base_and_quals=false filter_bases_not_stored=false\">");
		
		assertEquals(null, VcfHeaderUtils.getUUIDFromHeaderLine(header.getUUID()));
		
		header.addOrReplace("##qUUID=8b87f064-677e-4708-8c0f-8ecea9084eb9");
		assertEquals("8b87f064-677e-4708-8c0f-8ecea9084eb9", VcfHeaderUtils.getUUIDFromHeaderLine(header.getUUID()));
	}
	
//	@Test
//	public void splitMetaRecordNull() {
//		try {
//			VcfHeaderUtils.splitMetaRecord(null);
//			Assert.fail("Should have thrown an IAE");
//		} catch (final IllegalArgumentException iae){}
//	}		
//	
//	@Test
//	public void splitMetaRecords() {
//		VcfHeaderRecord rec = new VcfHeaderRecord("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO");
//		String [] results = VcfHeaderUtils.splitMetaRecord(rec);
//		assertEquals(2, results.length);
//		assertEquals(VcfHeader.STANDARD_FINAL_HEADER_LINE, results[0]);
//		assertEquals(null, results[1]);
//		
//		rec = new VcfHeaderRecord("##hello=world");
//		results = VcfHeaderUtils.splitMetaRecord(rec);
//		assertEquals(2, results.length);
//		assertEquals("##hello", results[0]);
//		assertEquals("world", results[1]);
//		
//		//new vcfHeader split header on the first =
//		rec = new VcfHeaderRecord("##hello=world=foo=bar");
//		results = VcfHeaderUtils.splitMetaRecord(rec);
//		assertEquals(2, results.length);
//		assertEquals("##hello", results[0]);
//		assertEquals("world=foo=bar", results[1]);
//	}

}
