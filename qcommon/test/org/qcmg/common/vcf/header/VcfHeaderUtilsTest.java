package org.qcmg.common.vcf.header;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

public class VcfHeaderUtilsTest {
	
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
	public void addQPGEntry() {
		final VcfHeader header = new VcfHeader();
		assertEquals(0, header.getqPGLines().size());
		
		final String tool = "qsnp";
		final String version = "1.0";
		final String cl = "qsnp -i ini_file.ini -log log_file.log";
		VcfHeaderUtils.addQPGLineToHeader(header, tool, version, cl);
		assertEquals(1, header.getqPGLines().size());
		assertEquals(1, header.getqPGLines().get(0).getOrder());
		assertEquals(tool, header.getqPGLines().get(0).getTool());
		assertEquals(version, header.getqPGLines().get(0).getVersion());
		assertEquals("\"" + cl + "\"", header.getqPGLines().get(0).getCommandLine());
		
		VcfHeaderUtils.addQPGLineToHeader(header, tool, version, cl);
		assertEquals(2, header.getqPGLines().size());
		assertEquals(2, header.getqPGLines().get(0).getOrder());
		assertEquals(1, header.getqPGLines().get(1).getOrder());
		assertEquals(tool, header.getqPGLines().get(0).getTool());
		assertEquals(version, header.getqPGLines().get(0).getVersion());
		assertEquals("\"" + cl + "\"", header.getqPGLines().get(0).getCommandLine());
		
		
		VcfHeaderUtils.addQPGLineToHeader(header, tool, version, cl);
		assertEquals(3, header.getqPGLines().size());
		assertEquals(3, header.getqPGLines().get(0).getOrder());
	}
	
	@Test
	public void checkOutGATKVersion() {
		VcfHeader header = new VcfHeader();
		header.parseHeaderLine("##fileformat=VCFv4.1");
		header.parseHeaderLine("##qUUID=8b87f064-677e-4708-8c0f-8ecea9084eb9");
		header.parseHeaderLine("##FILTER=<ID=LowQual,Description=\"Low quality\">");
		header.parseHeaderLine("##FORMAT=<ID=AD,Number=.,Type=Integer,Description=\"Allelic depths for the ref and alt alleles in the order listed\">");
		header.parseHeaderLine("##GATKCommandLine=<ID=HaplotypeCaller,Version=3.3-0-g37228af,Date=\"Mon Feb 16 15:28:52 EST 2015\",Epoch=1424064532168,CommandLineOptions=\"analysis_type=HaplotypeCaller input_file=[/mnt/genomeinfo_projects/data/oesophageal/OESO_0054/seq_final/SmgresOesophageal_OESO0054_1DNA_7PrimaryTumour_SMGresABNW20140115031_IlluminaTruSEQMultiplexedManual_HumanTruSEQExomeEnrichmentTruSEQ_Bwa_HiSeq.jpearson.bam] showFullBamList=false read_buffer_size=null phone_home=AWS gatk_key=null tag=NA read_filter=[] intervals=[/mnt/genomeinfo_projects/data/oesophageal/OESO_0054/seq_final/SmgresOesophageal_OESO0054_1DNA_7PrimaryTumour_SMGresABNW20140115031_IlluminaTruSEQMultiplexedManual_HumanTruSEQExomeEnrichmentTruSEQ_Bwa_HiSeq.jpearson.bam.queue/HC_C_OESO_0054-1-sg/temp_01_of_25/scatter.intervals] excludeIntervals=null interval_set_rule=UNION interval_merging=ALL interval_padding=0 reference_sequence=/opt/local/genomeinfo/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa nonDeterministicRandomSeed=false disableDithering=false maxRuntime=-1 maxRuntimeUnits=MINUTES downsampling_type=BY_SAMPLE downsample_to_fraction=null downsample_to_coverage=250 baq=OFF baqGapOpenPenalty=40.0 refactor_NDN_cigar_string=false fix_misencoded_quality_scores=false allow_potentially_misencoded_quality_scores=false useOriginalQualities=false defaultBaseQualities=-1 performanceLog=null BQSR=null quantize_quals=0 disable_indel_quals=false emit_original_quals=false preserve_qscores_less_than=6 globalQScorePrior=-1.0 validation_strictness=SILENT remove_program_records=false keep_program_records=false sample_rename_mapping_file=null unsafe=null disable_auto_index_creation_and_locking_when_reading_rods=false no_cmdline_in_header=false sites_only=false never_trim_vcf_format_field=false bcf=false bam_compression=null simplifyBAM=false disable_bam_indexing=false generate_md5=false num_threads=1 num_cpu_threads_per_data_thread=1 num_io_threads=0 monitorThreadEfficiency=false num_bam_file_handles=null read_group_black_list=null pedigree=[] pedigreeString=[] pedigreeValidationType=STRICT allow_intervals_with_unindexed_bam=false generateShadowBCF=false variant_index_type=DYNAMIC_SEEK variant_index_parameter=-1 logging_level=INFO log_to_file=null help=false version=false out=org.broadinstitute.gatk.engine.io.stubs.VariantContextWriterStub likelihoodCalculationEngine=PairHMM heterogeneousKmerSizeResolution=COMBO_MIN graphOutput=null bamOutput=null bamWriterType=CALLED_HAPLOTYPES disableOptimizations=false dbsnp=(RodBinding name=dbsnp source=/opt/local/genomeinfo/dbsnp/135/00-All_chr.vcf) dontTrimActiveRegions=false maxDiscARExtension=25 maxGGAARExtension=300 paddingAroundIndels=150 paddingAroundSNPs=20 comp=[] annotation=[ClippingRankSumTest, DepthPerSampleHC] excludeAnnotation=[SpanningDeletions, TandemRepeatAnnotator] debug=false useFilteredReadsForAnnotations=false emitRefConfidence=NONE annotateNDA=false heterozygosity=0.001 indel_heterozygosity=1.25E-4 standard_min_confidence_threshold_for_calling=30.0 standard_min_confidence_threshold_for_emitting=30.0 max_alternate_alleles=6 input_prior=[] sample_ploidy=2 genotyping_mode=DISCOVERY alleles=(RodBinding name= source=UNBOUND) contamination_fraction_to_filter=0.0 contamination_fraction_per_sample_file=null p_nonref_model=null exactcallslog=null output_mode=EMIT_VARIANTS_ONLY allSitePLs=false sample_name=null kmerSize=[10, 25] dontIncreaseKmerSizesForCycles=false allowNonUniqueKmersInRef=false numPruningSamples=1 recoverDanglingHeads=false doNotRecoverDanglingBranches=false minDanglingBranchLength=4 consensus=false GVCFGQBands=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 70, 80, 90, 99] indelSizeToEliminateInRefModel=10 min_base_quality_score=10 minPruning=2 gcpHMM=10 includeUmappedReads=false useAllelesTrigger=false phredScaledGlobalReadMismappingRate=45 maxNumHaplotypesInPopulation=128 mergeVariantsViaLD=false doNotRunPhysicalPhasing=true pair_hmm_implementation=VECTOR_LOGLESS_CACHING keepRG=null justDetermineActiveRegions=false dontGenotype=false errorCorrectKmers=false debugGraphTransformations=false dontUseSoftClippedBases=false captureAssemblyFailureBAM=false allowCyclesInKmerGraphToGeneratePaths=false noFpga=false errorCorrectReads=false kmerLengthForReadErrorCorrection=25 minObservationsForKmerToBeSolid=20 pcr_indel_model=CONSERVATIVE maxReadsInRegionPerSample=1000 minReadsPerAlignmentStart=5 activityProfileOut=null activeRegionOut=null activeRegionIn=null activeRegionExtension=null forceActive=false activeRegionMaxSize=null bandPassSigma=null maxProbPropagationDistance=50 activeProbabilityThreshold=0.002 min_mapping_quality_score=20 filter_reads_with_N_cigar=false filter_mismatching_base_and_quals=false filter_bases_not_stored=false\">");
		
		assertEquals("3.3-0-g37228af", VcfHeaderUtils.getGATKVersionFromHeaderLine(header));
	}
	@Test
	public void checkOutUUIDRetrieval() {
		VcfHeader header = new VcfHeader();
		header.parseHeaderLine("##fileformat=VCFv4.1");
		header.parseHeaderLine("##qUUID=8b87f064-677e-4708-8c0f-8ecea9084eb9");
		header.parseHeaderLine("##FILTER=<ID=LowQual,Description=\"Low quality\">");
		header.parseHeaderLine("##FORMAT=<ID=AD,Number=.,Type=Integer,Description=\"Allelic depths for the ref and alt alleles in the order listed\">");
		header.parseHeaderLine("##GATKCommandLine=<ID=HaplotypeCaller,Version=3.3-0-g37228af,Date=\"Mon Feb 16 15:28:52 EST 2015\",Epoch=1424064532168,CommandLineOptions=\"analysis_type=HaplotypeCaller input_file=[/mnt/genomeinfo_projects/data/oesophageal/OESO_0054/seq_final/SmgresOesophageal_OESO0054_1DNA_7PrimaryTumour_SMGresABNW20140115031_IlluminaTruSEQMultiplexedManual_HumanTruSEQExomeEnrichmentTruSEQ_Bwa_HiSeq.jpearson.bam] showFullBamList=false read_buffer_size=null phone_home=AWS gatk_key=null tag=NA read_filter=[] intervals=[/mnt/genomeinfo_projects/data/oesophageal/OESO_0054/seq_final/SmgresOesophageal_OESO0054_1DNA_7PrimaryTumour_SMGresABNW20140115031_IlluminaTruSEQMultiplexedManual_HumanTruSEQExomeEnrichmentTruSEQ_Bwa_HiSeq.jpearson.bam.queue/HC_C_OESO_0054-1-sg/temp_01_of_25/scatter.intervals] excludeIntervals=null interval_set_rule=UNION interval_merging=ALL interval_padding=0 reference_sequence=/opt/local/genomeinfo/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa nonDeterministicRandomSeed=false disableDithering=false maxRuntime=-1 maxRuntimeUnits=MINUTES downsampling_type=BY_SAMPLE downsample_to_fraction=null downsample_to_coverage=250 baq=OFF baqGapOpenPenalty=40.0 refactor_NDN_cigar_string=false fix_misencoded_quality_scores=false allow_potentially_misencoded_quality_scores=false useOriginalQualities=false defaultBaseQualities=-1 performanceLog=null BQSR=null quantize_quals=0 disable_indel_quals=false emit_original_quals=false preserve_qscores_less_than=6 globalQScorePrior=-1.0 validation_strictness=SILENT remove_program_records=false keep_program_records=false sample_rename_mapping_file=null unsafe=null disable_auto_index_creation_and_locking_when_reading_rods=false no_cmdline_in_header=false sites_only=false never_trim_vcf_format_field=false bcf=false bam_compression=null simplifyBAM=false disable_bam_indexing=false generate_md5=false num_threads=1 num_cpu_threads_per_data_thread=1 num_io_threads=0 monitorThreadEfficiency=false num_bam_file_handles=null read_group_black_list=null pedigree=[] pedigreeString=[] pedigreeValidationType=STRICT allow_intervals_with_unindexed_bam=false generateShadowBCF=false variant_index_type=DYNAMIC_SEEK variant_index_parameter=-1 logging_level=INFO log_to_file=null help=false version=false out=org.broadinstitute.gatk.engine.io.stubs.VariantContextWriterStub likelihoodCalculationEngine=PairHMM heterogeneousKmerSizeResolution=COMBO_MIN graphOutput=null bamOutput=null bamWriterType=CALLED_HAPLOTYPES disableOptimizations=false dbsnp=(RodBinding name=dbsnp source=/opt/local/genomeinfo/dbsnp/135/00-All_chr.vcf) dontTrimActiveRegions=false maxDiscARExtension=25 maxGGAARExtension=300 paddingAroundIndels=150 paddingAroundSNPs=20 comp=[] annotation=[ClippingRankSumTest, DepthPerSampleHC] excludeAnnotation=[SpanningDeletions, TandemRepeatAnnotator] debug=false useFilteredReadsForAnnotations=false emitRefConfidence=NONE annotateNDA=false heterozygosity=0.001 indel_heterozygosity=1.25E-4 standard_min_confidence_threshold_for_calling=30.0 standard_min_confidence_threshold_for_emitting=30.0 max_alternate_alleles=6 input_prior=[] sample_ploidy=2 genotyping_mode=DISCOVERY alleles=(RodBinding name= source=UNBOUND) contamination_fraction_to_filter=0.0 contamination_fraction_per_sample_file=null p_nonref_model=null exactcallslog=null output_mode=EMIT_VARIANTS_ONLY allSitePLs=false sample_name=null kmerSize=[10, 25] dontIncreaseKmerSizesForCycles=false allowNonUniqueKmersInRef=false numPruningSamples=1 recoverDanglingHeads=false doNotRecoverDanglingBranches=false minDanglingBranchLength=4 consensus=false GVCFGQBands=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 70, 80, 90, 99] indelSizeToEliminateInRefModel=10 min_base_quality_score=10 minPruning=2 gcpHMM=10 includeUmappedReads=false useAllelesTrigger=false phredScaledGlobalReadMismappingRate=45 maxNumHaplotypesInPopulation=128 mergeVariantsViaLD=false doNotRunPhysicalPhasing=true pair_hmm_implementation=VECTOR_LOGLESS_CACHING keepRG=null justDetermineActiveRegions=false dontGenotype=false errorCorrectKmers=false debugGraphTransformations=false dontUseSoftClippedBases=false captureAssemblyFailureBAM=false allowCyclesInKmerGraphToGeneratePaths=false noFpga=false errorCorrectReads=false kmerLengthForReadErrorCorrection=25 minObservationsForKmerToBeSolid=20 pcr_indel_model=CONSERVATIVE maxReadsInRegionPerSample=1000 minReadsPerAlignmentStart=5 activityProfileOut=null activeRegionOut=null activeRegionIn=null activeRegionExtension=null forceActive=false activeRegionMaxSize=null bandPassSigma=null maxProbPropagationDistance=50 activeProbabilityThreshold=0.002 min_mapping_quality_score=20 filter_reads_with_N_cigar=false filter_mismatching_base_and_quals=false filter_bases_not_stored=false\">");
		
		assertEquals("8b87f064-677e-4708-8c0f-8ecea9084eb9", VcfHeaderUtils.getUUIDFromHeaderLine(header.getUUID()));
	}

}
