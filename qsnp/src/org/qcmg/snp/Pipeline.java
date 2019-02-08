/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */

package org.qcmg.snp;

import gnu.trove.map.TMap;
import gnu.trove.map.hash.THashMap;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.SamReaderFactory.Option;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.reference.FastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import net.jpountz.xxhash.XXHash64;
import net.jpountz.xxhash.XXHashFactory;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.ini4j.Ini;
import org.qcmg.common.log.QLevel;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QBamId;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.Accumulator;
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.model.Rule;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.AccumulatorUtils;
import org.qcmg.common.util.BaseUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.Pair;
import org.qcmg.common.util.PileupElementLiteUtil;
import org.qcmg.common.util.PileupUtils;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.picard.MultiSAMFileIterator;
import org.qcmg.picard.MultiSAMFileReader;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMRecordFilterWrapper;
import org.qcmg.picard.util.PileupElementUtil;
import org.qcmg.picard.util.QBamIdFactory;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.common.model.Classification;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.snp.util.GenotypeUtil;
import org.qcmg.snp.util.IniFileUtil;
import org.qcmg.snp.util.PipelineUtil;
import org.qcmg.snp.util.RulesUtil;
import org.qcmg.vcf.VCFFileWriter;

public abstract class Pipeline {
	
	private static final String header = VcfHeaderUtils.FORMAT_GENOTYPE + Constants.COLON + 
			VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS + Constants.COLON + 
			VcfHeaderUtils.FORMAT_READ_DEPTH + Constants.COLON + 
			VcfHeaderUtils.FORMAT_END_OF_READ + Constants.COLON + 
			VcfHeaderUtils.FORMAT_FF + Constants.COLON + 
			VcfHeaderUtils.FORMAT_FILTER + Constants.COLON + 
			VcfHeaderUtils.FORMAT_INFO + Constants.COLON + 
			VcfHeaderUtils.FORMAT_NOVEL_STARTS + Constants.COLON + 
			VcfHeaderUtils.FORMAT_OBSERVED_ALLELES_BY_STRAND;
	
//	static final String ILLUMINA_MOTIF = "GGT";
	
	/**
	 * used to buffer the accumulation arrays in case reads go over ends of contigs 
	 */
	final static int ARRAY_BUFFER = 1000;
	
	private int novelStartsFilterValue = 4;
	private int mutantReadsFilterValue = 5;
	int minBaseQual = 10;
	
	static int initialTestSumOfCountsLimit = 3;
	static int baseQualityPercentage = 10;
	
	static int sBiasAltPercentage = 5;
	static int sBiasCovPercentage = 5;
	
	
	/*///////////////////////
	 * COLLECTIONS
	 *///////////////////////
	
	final List<VcfRecord> snps = new ArrayList<>(6 * 1024 * 1024);
	
	final List<VcfRecord> compoundSnps = new ArrayList<>(32 * 1024);
	
	// just used to store adjacent accumulators used by compound snp process
	final ConcurrentMap<VcfRecord, Pair<Accumulator, Accumulator>> adjacentAccumulators = new ConcurrentHashMap<>(2 * 1024 * 1024); 
	
	int[] controlStartPositions;
	int[] testStartPositions;
	int noOfControlFiles;
	int noOfTestFiles;
	boolean includeIndels;
	int mutationId;
	
	List<Rule> controlRules = new ArrayList<>(4);
	List<Rule> testRules = new ArrayList<>(4);
	
	String validation;
	String skipAnnotation;
	boolean runSBIASAnnotation = true;
	
	protected final boolean singleSampleMode;
	
	String referenceFile;
	String query;
	String runMode;
	
	FastaSequenceFile sequenceFile;
	byte[] referenceBases;
	int referenceBasesLength;
	int previousContigReferenceBasesLength;
	
	long noOfRecordsFailingFilter = 1000000;
	String currentChr = "chr1";

	////////
	// ids
	///////
	protected String controlSampleId;
	protected String testSampleId;
	protected String mutationIdPrefix;
	protected String patientId;
	
	////////////////////
	// output files
	////////////////////
	protected String vcfFile;
	
	//////////////////
	// input files
	//////////////////
	protected String [] controlBams;
	protected String [] testBams;
	
	protected SamReader controlSamReader;
	protected SamReader testSamReader;
	
	protected  QLogger logger;
	protected QExec qexec;

	protected boolean includeDuplicates;
	
	/**
	 * default constructor that sets up a logger for the subclass instance 
	 */
	public Pipeline (QExec qexec, boolean singleSampleMode) {
		logger = QLoggerFactory.getLogger(getClass());
		this.qexec = qexec;
		this.singleSampleMode =singleSampleMode;
	}
	
	
	void ingestIni(Ini ini) throws SnpException {
		
		// IDS
		patientId = IniFileUtil.getEntry(ini, "ids", "donor");
		controlSampleId = IniFileUtil.getEntry(ini, "ids", "controlSample");
		testSampleId = IniFileUtil.getEntry(ini, "ids", "testSample");
		
		// INPUT FILES
		referenceFile = IniFileUtil.getInputFile(ini, "ref");
		controlBams = IniFileUtil.getInputFiles(ini, "controlBam");
		testBams = IniFileUtil.getInputFiles(ini, "testBam");
		
		
		// OUTPUT FILES
		vcfFile = IniFileUtil.getOutputFile(ini, "vcf");
		
		// QBAMFILTER QUERY
		query =  IniFileUtil.getEntry(ini, "parameters", "filter");
		if ( ! StringUtils.isNullOrEmpty(IniFileUtil.getEntry(ini, "parameters", "includeDuplicates"))) {
			includeDuplicates =  Boolean.parseBoolean(IniFileUtil.getEntry(ini, "parameters", "includeDuplicates"));
		}
		final String sFailFilter = IniFileUtil.getEntry(ini, "parameters", "noOfRecordsFailingFilter");
		if (null != sFailFilter)
			noOfRecordsFailingFilter = Long.parseLong(sFailFilter);
		
		// run mode
		runMode =  IniFileUtil.getEntry(ini, "parameters", "runMode");
		
		// ADDITIONAL SETUP	
		mutationIdPrefix = qexec.getUuid().getValue() + "_SNP_";
		
		final String novelStartsFilterValueString = IniFileUtil.getEntry(ini, "parameters", "numberNovelStarts");
		// default to 4 if not specified
		if ( ! StringUtils.isNullOrEmpty(novelStartsFilterValueString)) {
			novelStartsFilterValue = Integer.parseInt(novelStartsFilterValueString);
		}
		
		final String mutantReadsFilterValueString = IniFileUtil.getEntry(ini, "parameters", "numberMutantReads");
		// default to 5 if not specified
		if ( ! StringUtils.isNullOrEmpty(mutantReadsFilterValueString)) {
			mutantReadsFilterValue = Integer.parseInt(mutantReadsFilterValueString);
		}
		
		final String minBaseQualString = IniFileUtil.getEntry(ini, "parameters", "minimumBaseQuality");
		// defaults to 10 if not specified
		if ( ! StringUtils.isNullOrEmpty(minBaseQualString)) {
			minBaseQual = Integer.parseInt(minBaseQualString);
		}
		
		// validation
		final String validationString = IniFileUtil.getEntry(ini, "parameters", "validation");
		if ( ! StringUtils.isNullOrEmpty(validationString)) {
			validation = validationString;
		}
		
		// validation
		final String skipAnnotationString = IniFileUtil.getEntry(ini, "parameters", "skipAnnotation");
		if ( ! StringUtils.isNullOrEmpty(skipAnnotationString)) {
			skipAnnotation = skipAnnotationString;
			if (skipAnnotation.contains(SnpUtils.STRAND_BIAS_ALT)) 
				runSBIASAnnotation = false; 
		}
		
		final String sBiasAltPercentageString = IniFileUtil.getEntry(ini, "parameters", "sBiasAltPercentage");
		// default to 5 if not specified
		if ( ! StringUtils.isNullOrEmpty(sBiasAltPercentageString)) {
			sBiasAltPercentage = Integer.parseInt(sBiasAltPercentageString);
		}
		final String sBiasCovPercentageString = IniFileUtil.getEntry(ini, "parameters", "sBiasCovPercentage");
		// default to 5 if not specified
		if ( ! StringUtils.isNullOrEmpty(sBiasCovPercentageString)) {
			sBiasCovPercentage = Integer.parseInt(sBiasCovPercentageString);
		}
		
		// LOG
		if ( ! StringUtils.isNullOrEmpty(runMode)) {
			logger.tool("**** RUN MODE ****");
			logger.tool("runMode: " + runMode);
		}
		logger.tool("**** IDS ****");
		logger.tool("patient ID: " + patientId);
		logger.tool("analysisId: " + qexec.getUuid().getValue());
		logger.tool("controlSampleId: " + controlSampleId);
		logger.tool("testSampleId: " + testSampleId);
		
		logger.tool("**** INPUT FILES ****");
		if (null != controlBams) {
			logger.tool("controlBam: " + Arrays.deepToString(controlBams));
		}
		if (null != testBams) {
			logger.tool("testBam: " + Arrays.deepToString(testBams));
		}
		if (null != referenceFile) {
			logger.tool("referenceFile: " + referenceFile);
		}
		
		logger.tool("**** OUTPUT FILES ****");
		logger.tool("vcf: " + vcfFile);
		
		logger.tool("**** CONFIG ****");
		logger.tool("mutantReadsFilterValue: " + mutantReadsFilterValue);
		logger.tool("novelStartsFilterValue: " + novelStartsFilterValue);
		logger.tool("minimumBaseQuality: " + minBaseQual);
		logger.tool("singleSampleMode: " + singleSampleMode);
		logger.tool("minBaseQual: " + minBaseQual);
		if ( ! StringUtils.isNullOrEmpty(query)) {
			logger.tool("query: " + query);
			logger.tool("noOfRecordsFailingFilter: " + noOfRecordsFailingFilter);
		}
		if ( ! StringUtils.isNullOrEmpty(validation)) {
			logger.tool("validation: " + validation);
		}
		if ( ! StringUtils.isNullOrEmpty(skipAnnotation)) {
			logger.tool("skipAnnotation: " + skipAnnotation);
			logger.tool("runSBIASAnnotation: " + runSBIASAnnotation);
		}
		logger.tool("includeDuplicates: " + includeDuplicates);
	}
	
	
	QBamId [] getBamIdDetails(String [] bamFileName) throws Exception {
		if (null != bamFileName && bamFileName.length > 0) {
			final QBamId [] bamIds = new QBamId[bamFileName.length];
			for (int i = 0 ; i < bamFileName.length ; i++) {
			//	bamIds[i] = QBamIdFactory.getBamId(bamFileName[i]);
				bamIds[i] = QBamIdFactory.getQ3BamId(bamFileName[i]);
			}
			return bamIds;
		} else {
			return null;
		}
	}
	
	VcfHeader getExistingVCFHeaderDetails() {
		// override this if dealing with input VCFs and the existing headers are to be kept
		return null;
	}
	
	void writeVCF(String outputFileName) throws Exception {
		if (StringUtils.isNullOrEmpty(outputFileName)) {
			logger.warn("No vcf output file scpecified so can't output vcf");
			return;
		}
 
		logger.info("Writing VCF output");
		
		final QBamId[] normalBamIds = getBamIdDetails(controlBams);
		final QBamId[] tumourBamIds = getBamIdDetails(testBams);
		
		VcfHeader existingHeader = getExistingVCFHeaderDetails();
		
		snps.addAll(compoundSnps);
		/*
		 * order vcf records based on order defined in reference fastq file - helps with vcf validation
		 */
		List<String> refFileContigs = Collections.emptyList();
		if (null != referenceFile) {
			try (FastaSequenceFile fsf = new FastaSequenceFile(new File(referenceFile), true)) {
				if (null != fsf.getSequenceDictionary()) {
					refFileContigs = fsf.getSequenceDictionary().getSequences()
						.stream().map(ssr -> ssr.getSequenceName()).collect(Collectors.toList());
				}
			}
		}
		snps.sort(refFileContigs.isEmpty() ? null : ChrPositionComparator.getVcfRecordComparator(refFileContigs));

		try (VCFFileWriter writer = new VCFFileWriter(new File(outputFileName));) {			
			final VcfHeader header = getHeaderForQSnp(patientId, controlSampleId, testSampleId, "qSNP v" + Main.version, normalBamIds, tumourBamIds, qexec.getUuid().getValue());
			VcfHeaderUtils.addQPGLineToHeader(header, qexec.getToolName().getValue(), qexec.getToolVersion().getValue(), qexec.getCommandLine().getValue() 
					+ (StringUtils.isNullOrEmpty(runMode) ? "" : " [runMode: " + runMode + "]"));
			
			// add in existing header details
			if (null != existingHeader) {
				VcfHeaderUtils.mergeHeaders(header, existingHeader, true);
			}
			for (VcfHeaderRecord hr : header) {
				writer.addHeader(hr.toString());
			}
			
			for (VcfRecord r : snps) {
				writer.add(r);
			}
		}
	}

	public static VcfHeader getHeaderForQSnp(final String patientId,  final String normalSampleId, final String tumourSampleId, 
			final String source, QBamId[] normalBamIds, QBamId[] tumourBamIds, String uuid) {
		
		final VcfHeader header = new VcfHeader();
		final DateFormat df = new SimpleDateFormat("yyyyMMdd");

		header.addOrReplace(VcfHeaderUtils.CURRENT_FILE_FORMAT);		
		header.addOrReplace(VcfHeaderUtils.STANDARD_FILE_DATE + Constants.EQ + df.format(Calendar.getInstance().getTime()));		
		header.addOrReplace(VcfHeaderUtils.STANDARD_UUID_LINE + Constants.EQ + QExec.createUUid());		
		header.addOrReplace(VcfHeaderUtils.STANDARD_SOURCE_LINE + Constants.EQ + source);		
		
		header.addOrReplace(VcfHeaderUtils.STANDARD_DONOR_ID + Constants.EQ + patientId);
		header.addOrReplace(VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + Constants.EQ + normalSampleId);		
		header.addOrReplace(VcfHeaderUtils.STANDARD_TEST_SAMPLE + Constants.EQ + tumourSampleId);		
		
		String normalFormatSample = normalSampleId;
		if (null != normalBamIds)  
			for (final QBamId s : normalBamIds) {
				header.addOrReplace( VcfHeaderUtils.STANDARD_CONTROL_BAM  + Constants.EQ + s.getBamName());
				header.addOrReplace( "##qControlBamUUID=" + s.getUUID());
				if (normalBamIds.length == 1) {
					normalFormatSample = StringUtils.isMissingDtaString(s.getUUID()) ? s.getBamName().replaceAll("(?i).bam$", "") : s.getUUID();
				}
 			}
		
		String tumourFormatSample = tumourSampleId;
		if (null != tumourBamIds)  
			for (final QBamId s : tumourBamIds) {
				header.addOrReplace(VcfHeaderUtils.STANDARD_TEST_BAM  + Constants.EQ + s.getBamName());
				header.addOrReplace("##qTestBamUUID=" + s.getUUID());
				if (tumourBamIds.length == 1) {
					tumourFormatSample = StringUtils.isMissingDtaString(s.getUUID()) ? s.getBamName().replaceAll("(?i).bam$", "") : s.getUUID();
				}
			}		
		header.addOrReplace( "##qAnalysisId=" + uuid );
		
		header.addInfo(VcfHeaderUtils.INFO_FLANKING_SEQUENCE, "1", "String","Flanking sequence either side of variant");														
		header.addInfo(VcfHeaderUtils.INFO_SOMATIC, "0", "Flag",VcfHeaderUtils.INFO_SOMATIC_DESC);														
		header.addFilter(VcfHeaderUtils.FILTER_COVERAGE_NORMAL_12, "Less than 12 reads coverage in normal");
		header.addFilter(VcfHeaderUtils.FILTER_COVERAGE_NORMAL_8,"Less than 8 reads coverage in normal");  
		header.addFilter(VcfHeaderUtils.FILTER_COVERAGE_TUMOUR,"Less than 8 reads coverage in tumour"); 
		header.addFilter(VcfHeaderUtils.FILTER_SAME_ALLELE_NORMAL,"Less than 3 reads of same allele in normal");  
		header.addFilter(VcfHeaderUtils.FILTER_SAME_ALLELE_TUMOUR,"Less than 3 reads of same allele in tumour");  
		header.addFilter(VcfHeaderUtils.FILTER_MUTATION_IN_NORMAL,"Mutation also found in pileup of normal");  
		header.addFilter(VcfHeaderUtils.FILTER_MUTATION_IN_UNFILTERED_NORMAL,"Mutation also found in pileup of (unfiltered) normal");  
		header.addFilter(VcfHeaderUtils.FILTER_GERMLINE,"Mutation is a germline variant in another patient");  
		header.addFilter(VcfHeaderUtils.FILTER_NOVEL_STARTS,"Less than 4 novel starts not considering read pair");  
		header.addFilter(VcfHeaderUtils.FILTER_MUTANT_READS,"Less than 5 mutant reads"); 
		header.addFilter(VcfHeaderUtils.FILTER_MUTATION_EQUALS_REF,"Mutation equals reference"); 
		header.addFilter(VcfHeaderUtils.FILTER_NO_CALL_IN_TEST,"No call in test"); 
		header.addFilter(VcfHeaderUtils.FILTER_STRAND_BIAS_ALT,"Alternate allele on only one strand (or percentage alternate allele on other strand is less than " + sBiasAltPercentage + "%)"); 
		header.addFilter(VcfHeaderUtils.FILTER_STRAND_BIAS_COV,"Sequence coverage on only one strand (or percentage coverage on other strand is less than " + sBiasCovPercentage + "%)"); 
	
		header.addFormat(VcfHeaderUtils.FORMAT_GENOTYPE, "1", "String" ,"Genotype");
//		header.addFormat(VcfHeaderUtils.FORMAT_GENOTYPE_DETAILS, "1", "String","Genotype details: specific alleles (A,G,T or C)");
		header.addFormat(VcfHeaderUtils.FORMAT_END_OF_READ, ".", "String",VcfHeaderUtils.FORMAT_END_OF_READ_DESC);
		header.addFormat(VcfHeaderUtils.FORMAT_FILTER, ".", "String","Filters that apply to this sample");
		header.addFormat(VcfHeaderUtils.FORMAT_INFO, ".", "String",VcfHeaderUtils.FORMAT_INFO_DESCRIPTION);
		header.addFormat(VcfHeaderUtils.FORMAT_OBSERVED_ALLELES_BY_STRAND, ".", "String",VcfHeaderUtils.FORMAT_OBSERVED_ALLELES_BY_STRAND_DESC);
		header.addFormat(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS, ".", "Integer",VcfHeaderUtils.FORMAT_AD_DESC);
		header.addFormat(VcfHeaderUtils.FORMAT_FF, ".", "String",VcfHeaderUtils.FORMAT_FF_DESC);
		header.addFormat(VcfHeaderUtils.FORMAT_READ_DEPTH, "1", "Integer","Approximate read depth (reads with MQ=255 or with bad mates are filtered)");
		header.addFormat(VcfHeaderUtils.FORMAT_GENOTYPE_QUALITY, "1", "Integer","Genotype Quality");
		header.addFormat(VcfHeaderUtils.FORMAT_MUTANT_READS,  ".", "Integer","Number of mutant/variant reads");
		header.addFormat(VcfHeaderUtils.FORMAT_NOVEL_STARTS, ".", "Integer","Number of novel starts not considering read pair");		
		header.addFormat(VcfHeaderUtils.FORMAT_QL, "1", "Float",VcfHeaderUtils.FORMAT_QL_DESC);		
			
		header.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + 
				( normalFormatSample != null? normalFormatSample + Constants.TAB : "") + tumourFormatSample);
		return  header;
	}
	
	List<SAMFileHeader> getBamFileHeaders(String ... bams) {
		final List<SAMFileHeader> headers = new ArrayList<>(bams.length +1);
		for (final String bam : bams) {
			final SAMFileHeader header = SAMFileReaderFactory.createSAMFileReader(new File(bam)).getFileHeader();
			headers.add(header);
		}
		return headers;
	}
	
	private void checkHeadersSortOrder(List<SAMFileHeader> headers, boolean isNormal) throws SnpException {
		for (final SAMFileHeader sfh : headers) {
			if (SAMFileHeader.SortOrder.coordinate != sfh.getSortOrder()) {
				throw new SnpException("BAM_FILE_NOT_SORTED", (isNormal ? "Control" : "Test"));
			}
		}
	}
	
	List<SAMSequenceDictionary> getSequenceDictionaries(List<SAMFileHeader> headers) {
		final List<SAMSequenceDictionary> seqDictionaries = new ArrayList<SAMSequenceDictionary>();
		for (final SAMFileHeader header : headers) {
			seqDictionaries.add(header.getSequenceDictionary());
		}
		return seqDictionaries;
	}
	
	private void checkSequenceDictionaries(List<SAMFileHeader> normalHeaders, List<SAMFileHeader> tumourHeaders) throws SnpException {
		final List<SAMSequenceDictionary> normalSeqDictionaries = getSequenceDictionaries(normalHeaders);
		final List<SAMSequenceDictionary> tumourSeqDictionaries = getSequenceDictionaries(tumourHeaders);
		
		for (final SAMSequenceDictionary normalSD : normalSeqDictionaries) {
			final List<SAMSequenceRecord> normalSequences = normalSD.getSequences();
			
			for (final SAMSequenceDictionary tumourSD : tumourSeqDictionaries) {
				if (normalSD.getReferenceLength() != tumourSD.getReferenceLength()) {
					throw new SnpException("SEQUENCE_LENGTHS_DONT_MATCH"
							, ""+normalSD.getReferenceLength() , ""+tumourSD.getReferenceLength());
				}
				
				final List<SAMSequenceRecord> tumourSequences = tumourSD.getSequences();
				int i = 0;
				for (final SAMSequenceRecord normalSeq : normalSequences) {
					final SAMSequenceRecord tumourSeq = tumourSequences.get(i++);
					if ( ! normalSeq.isSameSequence(tumourSeq) ) {
						throw new SnpException("SEQUENCES_DONT_MATCH", 
								new String[] {normalSeq.getSequenceName(), normalSeq.getSequenceLength()+"",
								tumourSeq.getSequenceName(), tumourSeq.getSequenceLength()+""});	
					}
				}
			}
		}
		
		
		// pick the first normal sequence to check against fasta
		final List<SAMSequenceRecord> normalSequences = normalSeqDictionaries.get(0).getSequences();
		
		// now check against the supplied reference file
		final FastaSequenceFile ref = new FastaSequenceFile(new File(referenceFile), true);
		if (null != ref) {
			try {
				ReferenceSequence nextRefSeq = null;
				
				// loop through the normal sequences, compare name and size against supplied reference
				for (final SAMSequenceRecord normalSeq : normalSequences) {
					final String name = normalSeq.getSequenceName();
					final int length = normalSeq.getSequenceLength();
					
					// get next sequence
					nextRefSeq = ref.nextSequence();
					if (null == nextRefSeq) {
						logger.warn("Mismatch in number of sequences - no more reference sequences, normal sequence: " + name + ":" + length);
						break;
					} else {
						
						// if the sequence names don't match - throw an exception
						// if the lengths don't match - log a warning message
						if ( ! nextRefSeq.getName().equalsIgnoreCase(name)) {
							logger.error("reference sequence name (" + nextRefSeq.getName() + ") does not match normal bam file sequence name (" + name + ")");
							throw new SnpException("SEQUENCES_DONT_MATCH", 
									new String[] {name, length+"",
									nextRefSeq.getName(), nextRefSeq.length()+""});	
						} else if (nextRefSeq.length() != length) {
							logger.warn("Reference sequence lengths don't match. Normal sequence: " 
									+ name + ":" + length + ", Reference sequence: " 
									+ nextRefSeq.getName() + ":" + nextRefSeq.length());
						}
					}
				}
			} finally {
				ref.close();
			}
		}
	}
	
	/**
	 * This method aims to determine if the references used to map the normal and tumour bams are the same
	 * It will also check the sort order of the bam files.
	 * And finally, some checking against the supplied reference file is performed
	 * 
	 * @throws SnpException 
	 */
	void checkBamHeaders() throws SnpException {
		logger.info("Checking bam file headers for reference file compatibility");
		
		List<SAMFileHeader> normalHeaders = null;
		if ( ! singleSampleMode) {
			normalHeaders = getBamFileHeaders(controlBams);
			checkHeadersSortOrder(normalHeaders, true);
		}
		final List<SAMFileHeader> tumourHeaders = getBamFileHeaders(testBams);
		
		// check sort order
		checkHeadersSortOrder(tumourHeaders, false);
		
		if ( ! singleSampleMode) {
			checkSequenceDictionaries(normalHeaders, tumourHeaders);
		}
		logger.info("Checking bam file headers for reference file compatibility - DONE");
	}
	
	static boolean isPileupRecordAKeeperFirstPass(int variantCount, int coverage, Rule rule, List<PileupElement> baseCounts, double percentage) {
		// if rule is null, return false
		if (null == rule) return false;
		// first check to see if it passes the rule
		return PileupElementUtil.passesCountCheck(variantCount, coverage, rule) && PileupElementUtil.passesWeightedVotingCheck(baseCounts, percentage);
	}
	
	static boolean isPileupRecordAKeeperSecondPass(int variantCount, int coverage, Rule rule, List<PileupElement> baseCounts, double percentage) {
		// if rule is null, return false
		if (null == rule) return false;
		// first check to see if it passes the rule
		return PileupElementUtil.passesCountCheck(variantCount, coverage, rule, true) 
				&& isVariantOnBothStrands(baseCounts) 
				&& PileupElementUtil.passesWeightedVotingCheck(baseCounts, percentage, true);
	}
	
	static boolean isPileupRecordAKeeper(int variantCount, int coverage, Rule rule, List<PileupElement> baseCounts, double percentage) {
		if (isPileupRecordAKeeperFirstPass(variantCount, coverage, rule, baseCounts, percentage))
			return true;
		else return isPileupRecordAKeeperSecondPass(variantCount, coverage, rule, baseCounts, percentage);
	}
	
	static boolean isVariantOnBothStrands(List<PileupElement> baseCounts) {
		final PileupElement pe = PileupElementUtil.getLargestVariant(baseCounts);
		return null == pe ? false :  pe.isFoundOnBothStrands();
	}
	
	/**
	 * Assumes normal bams come before tumour bams in the pileup file
	 */
	void getStringPositions() {
		controlStartPositions = PileupUtils.getStartPositions(noOfControlFiles, noOfTestFiles, true);
		testStartPositions =PileupUtils.getStartPositions(noOfControlFiles, noOfTestFiles, false);
	}
	
	void checkRules() throws SnpException {
		String rulesErrors = RulesUtil.examineRules(controlRules);
		if (null != rulesErrors) {
			logger.warn("Problem with Control Rules: " + rulesErrors);
		}
		
		rulesErrors = RulesUtil.examineRules(testRules);
		if (null != rulesErrors) {
			logger.warn("Problem with Test Rules: " + rulesErrors);
		}
	}
	
	void loadNextReferenceSequence() {
		if (null == sequenceFile) {
			sequenceFile = new FastaSequenceFile(new File(referenceFile), true);
		}
		referenceBases = null;
		currentChr = null;
		previousContigReferenceBasesLength = referenceBasesLength;
		if (previousContigReferenceBasesLength == 0) {
			previousContigReferenceBasesLength = Integer.MAX_VALUE;
		}
		referenceBasesLength = 0;
		final ReferenceSequence refSeq = sequenceFile.nextSequence();
		
		if (null == refSeq) {	// end of the line
			logger.info("No more chromosomes in reference file - shutting down");
			closeReferenceFile();
		} else {
			currentChr = refSeq.getName();
			referenceBases = refSeq.getBases();
			referenceBasesLength = refSeq.length();
			logger.info("Will process records from: " + currentChr + ", length: " + referenceBasesLength);
		}
	}
	void closeReferenceFile() {
		if (null != sequenceFile) sequenceFile.close();
	}
	
	void walkBams() throws Exception {
		walkBams(includeDuplicates);
	}
	
	/**
	 * Sets up 2 Producer threads, 2 Consumer threads and a Cleaner thread, along with the concurrent collections, queues, and barriers used by them all
	 * 
	 * @param ignoreDuplicates indicates whether duplicate records should be discarded out right. Not useful for torrent mode
	 * @throws Exception
	 */
	void walkBams(boolean includeDups) throws Exception {
		logger.info("About to hit bam files");
		
		final int noOfThreads = singleSampleMode ? 3 : 5;		// 2 for each bam, and a single cleaner
		final int consumerLatchSize = singleSampleMode ? 1 : 2;		// 2 for each bam, and a single cleaner
		
		final AtomicInteger controlMinStart = new AtomicInteger();
		final AtomicInteger testMinStart = new AtomicInteger();
		final Queue<SAMRecordFilterWrapper> normalSAMQueue = new ConcurrentLinkedQueue<>();
		final Queue<SAMRecordFilterWrapper> tumourSAMQueue = new ConcurrentLinkedQueue<>();
		
		// used by Cleaner3 threads
		Accumulator [] controlAccs = new Accumulator[1024 * 1024 * 256];
		Accumulator [] testAccs = new Accumulator[1024 * 1024 * 256];
		
		final CyclicBarrier barrier = new CyclicBarrier(noOfThreads, new Runnable() {
			@Override
			public void run() {
				// reset the minStartPositions values to zero
				controlMinStart.set(0);
				testMinStart.set(0);
				
				// update the reference bases array
				loadNextReferenceSequence();
				
				logger.info("barrier has been reached by all threads - moving onto next chromosome");
			}
		});
		final ExecutorService service = Executors.newFixedThreadPool(noOfThreads);
		final CountDownLatch consumerLatch = new CountDownLatch(consumerLatchSize);
		final CountDownLatch controlProducerLatch = new CountDownLatch(1);
		final CountDownLatch testProducerLatch = new CountDownLatch(1);
		final CountDownLatch cleanerLatch = new CountDownLatch(1);
		
		
		/*
		 * setup exception handler for threads, exit with code 1 if uncaught exception is encountered by worker threads
		 */
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {logger.error("( in uncaughtExceptionHandler) )exception " + e + ", from thread: " + t, e); System.exit(1);});
		final SamReaderFactory factory = SamReaderFactory.makeDefault()
			       .enable(Option.INCLUDE_SOURCE_IN_RECORDS, Option.VALIDATE_CRC_CHECKSUMS)
			       .validationStringency(ValidationStringency.SILENT);
		
		// Control threads (if not single sample)
		if ( ! singleSampleMode) {
			controlSamReader = factory.open(new File(controlBams[0]));
			service.execute(new Producer(controlBams, controlProducerLatch, true, normalSAMQueue, Thread.currentThread(), query, barrier, includeDups, controlAccs));
			service.execute(new Consumer(consumerLatch, controlProducerLatch, testProducerLatch, true, 
					Thread.currentThread(), barrier, controlAccs, normalSAMQueue, controlMinStart));
		}
		
		// test threads
		testSamReader = factory.open(new File(testBams[0]));
		service.execute(new Producer(testBams, testProducerLatch, false, tumourSAMQueue, Thread.currentThread(), query, barrier, includeDups, testAccs));
		service.execute(new Consumer(consumerLatch, controlProducerLatch, testProducerLatch, false, 
				Thread.currentThread(), barrier, testAccs, tumourSAMQueue, testMinStart));
		
		// Cleaner
		service.execute(new Cleaner(cleanerLatch, consumerLatch, Thread.currentThread(),
				barrier, controlMinStart, testMinStart, controlAccs, testAccs));
		
		service.shutdown();
		try {
			if ( ! singleSampleMode) {
				controlProducerLatch.await();
			}
			testProducerLatch.await();
			consumerLatch.await();
			cleanerLatch.await();
		} catch (final InterruptedException e) {
			logger.info("current thread about to be interrupted...");
			logger.error("Error in thread: ", e);
			
			// kill off any remaining threads
			service.shutdownNow();
			
			logger.error("Terminating due to failed Producer/Consumer/Cleaner threads");
			throw e;
		}
		logger.info("bam file access finished!");
	}
	
	
	public class Producer implements Runnable {
		
		private final MultiSAMFileReader reader;
		private MultiSAMFileIterator iter;
		private final boolean isControl;
		private final CountDownLatch latch;
		private final Queue<SAMRecordFilterWrapper> queue;
		private QueryExecutor qbamFilter;
		private final Thread mainThread;
		private long passedFilterCount = 0;
		private long invalidCount = 0;
		private int counter = 0;
		private int higherOrderCounter = 0;
//		private int chrCounter = 0;
		private final CyclicBarrier barrier;
		private final boolean includeDups;
		private final boolean runqBamFilter;
		private Accumulator [] accum;
		private XXHash64 xxhash64;
		private final static int seed = 0x9747b28c; // used to initialize the hash value, use whatever value you want, but always the same
		private final static int ONE_MILLION = 1_000_000;
		
		public Producer(final String[] bamFiles, final CountDownLatch latch, final boolean isNormal, 
				final Queue<SAMRecordFilterWrapper> samQueue, final Thread mainThread, final String query, 
				final CyclicBarrier barrier, boolean includeDups, Accumulator [] accum) throws Exception {
			this.latch = latch;
			final Set<File> bams = new HashSet<File>();
			for (final String bamFile : bamFiles) {
				bams.add(new File(bamFile));
			}
			this.reader = new MultiSAMFileReader(bams, true, validation);
//			this.iter = reader.getMultiSAMFileIterator(currentChr, 0, referenceBasesLength, true);
			this.isControl = isNormal;
			this.mainThread = mainThread;
			this.queue = samQueue;
			if ( ! StringUtils.isNullOrEmpty(query) && ! "QCMG".equals(query))
				qbamFilter = new QueryExecutor(query);
			this.barrier = barrier;
			this.accum = accum;
			this.includeDups = includeDups;
			runqBamFilter = null != qbamFilter;
		}

		@Override
		public void run() {
			logger.info("In Producer run method with isControl: " + isControl);
			logger.info("Use qbamfilter? " + runqBamFilter);
			
			/*
			 * setup hashing factory
			 */
			XXHashFactory factory = XXHashFactory.fastestInstance();
		    xxhash64 = factory.hash64();
			
			
			try {
				boolean keepRunning = true;
				
				while (keepRunning) {
					
					
					/*
					 * setup accumulator
					 */
					if (previousContigReferenceBasesLength < Integer.MAX_VALUE) {
						int upperBound = Math.min(previousContigReferenceBasesLength + (2 * ARRAY_BUFFER), accum.length - 1);
						logger.info("about to null array - upper limit: " + upperBound);
						Arrays.fill(accum, 0, upperBound, null);
						logger.info("about to null array - DONE");
					}
					
					iter = reader.getMultiSAMFileIterator(currentChr, 0, referenceBasesLength, true);
					logger.info("Producer: about to process records from " + currentChr);
					
					
					while (iter.hasNext()) {
						final SAMRecord record = iter.next();
					
						if (++ counter > ONE_MILLION) {
							higherOrderCounter++;
							counter = 0;
//							if (++ counter % 1000000 == 0) {
							int qSize = queue.size();
							logger.info("hit " + higherOrderCounter + "M sam records, passed filter: " + passedFilterCount + ", qsize: " + qSize);
							if (passedFilterCount == 0 && (counter + (ONE_MILLION * higherOrderCounter)) >= noOfRecordsFailingFilter) {
								throw new SnpException("INVALID_FILTER", ""+ (counter + (ONE_MILLION * higherOrderCounter)));
							}
							while (qSize > 10000) {
								try {
									Thread.sleep(200);
								} catch (final InterruptedException e) {
									logger.warn("InterruptedException caught whilst Producer thread was sleeping");
									throw e;
								}
								qSize = queue.size();
							}
						}
					
						processRecord(record);
						
					}
					/*
					 * finished processing records from this chromosome
					 * log and wait at barrier
					 */
					logger.info("Producer: Processed all records in " + currentChr + ", waiting at barrier");
					try {
						barrier.await();
						// don't need to reset barrier, threads waiting at barrier are released when all threads reach barrier... oops
//							if (isNormal) barrier.reset();		// reset the barrier
					} catch (final InterruptedException e) {
						logger.error("Producer: InterruptedException exception caught whilst waiting at barrier: ", e);
						throw e;
					} catch (final BrokenBarrierException e) {
						logger.error("Producer: BrokenBarrier exception caught whilst waiting at barrier: ", e);
						throw e;
					}
					
					if (null == currentChr) {
						// no longer have reference details - exit
						logger.warn("Exiting Producer - null reference chromosome");
						keepRunning = false;
					} else {
						/*
						 * reset counter and move onto to new contig
						 */
//						chrCounter = 1;
//						iter = reader.getMultiSAMFileIterator(currentChr, 0, 1, true);
						// wait until queues are empty
						int qSize = queue.size();
						if (qSize > 0)
							logger.info("Waiting for empty queue before continuing with next chr. qsize: " + qSize);
						while (qSize > 0) {
							try {
								Thread.sleep(10);
							} catch (final InterruptedException e) {
								e.printStackTrace();
								throw e;
							}
							qSize = queue.size();
						}
					}
				}
				
				
			} catch (final Exception e) {
				e.printStackTrace();
				mainThread.interrupt();
			} finally {
				latch.countDown();
				logger.info("Producer: shutting down - processed " + counter + " records, passed filter: " 
						+ passedFilterCount + ", invalidCount: " + invalidCount);
			}
		}
		
		private void processRecord(SAMRecord record) throws Exception {
			
			// if record is not valid for variant calling - discard
			if ( ! SAMUtils.isSAMRecordValidForVariantCalling(record, includeDups)) {
				invalidCount++;
				return;
			}
			
			if (runqBamFilter) {
				final boolean passesFilter = qbamFilter.Execute(record);
				/*
				 * we now want to keep track of reads that don't pass the filter for test as well as control
				 */
				addRecordToQueue(record, passesFilter);
			} else {
				// didn't have any filtering defined - add all
				addRecordToQueue(record, true);
			}
		}
		
		private void addRecordToQueue(final SAMRecord record,  final boolean passesFilter) {
			
			record.getReadBases();					// cache read bases in object
			if (passesFilter) {
				record.getBaseQualities();	// cache base qualities in object
				passedFilterCount++;
			}
			record.getCigar();					// cache cigar for all records
			record.getAlignmentEnd();		// cache alignment end for all records
			
			final SAMRecordFilterWrapper wrapper = new SAMRecordFilterWrapper(record, xxhash64.hash(record.getReadName().getBytes(), 0, record.getReadNameLength(), seed));
			wrapper.setPassesFilter(passesFilter);
			queue.add(wrapper);
		}
	}
	
	public class Consumer implements Runnable {
		
		private final CountDownLatch consumerLatch;
		private final CountDownLatch controlLatch;
		private final CountDownLatch testLatch;
		private final boolean isControl;
		private final Thread mainThread;
		private final  Accumulator[] array;
		private final CyclicBarrier barrier;
		private final Queue<SAMRecordFilterWrapper> queue;
		private final AtomicInteger minStartPosition;
		
		private final int maxMapSize = 100000;
		
		public Consumer(final CountDownLatch consumerLatch, final CountDownLatch normalLatch, 
				final CountDownLatch tumourLatch, final boolean isNormal, final Thread mainThread, 
				final CyclicBarrier barrier, final  Accumulator[] array,
				final Queue<SAMRecordFilterWrapper> queue, final AtomicInteger minStartPosition){
			
			this.consumerLatch = consumerLatch;
			this.controlLatch = normalLatch;
			this.testLatch = tumourLatch;
			this.isControl = isNormal;
			this.mainThread = mainThread;
			this.array =  array;
//			this.map =  map;
			this.barrier = barrier;
			this.queue = queue;
			this.minStartPosition = minStartPosition;
		}
		
		public void processSAMRecord(final SAMRecordFilterWrapper record) {
			final SAMRecord sam = record.getRecord();
			final boolean forwardStrand = ! sam.getReadNegativeStrandFlag();
			final int startPosition = sam.getAlignmentStart();
			// endPosition is just that for reverse strand, but for forward strand reads it is start position
			final int endPosition = sam.getAlignmentEnd();
			final byte[] bases = sam.getReadBases();
			final byte[] qualities = record.getPassesFilter() ? sam.getBaseQualities() : null;
			final Cigar cigar = sam.getCigar();
//			String readName = sam.getReadName();
			
			int referenceOffset = 0, offset = 0;
			
			for (final CigarElement ce : cigar.getCigarElements()) {
				final CigarOperator co = ce.getOperator();
				final int length = ce.getLength();

				if (co.consumesReferenceBases() && co.consumesReadBases()) {
					// we have a number (length) of bases that can be advanced.
					updateMapWithAccums(startPosition, bases,
							qualities, forwardStrand, offset, length, referenceOffset, 
							record.getPassesFilter(), endPosition, record.getPosition());
					// advance offsets
					referenceOffset += length;
					offset += length;
				} else if (co.consumesReferenceBases()) {
					// DELETION
					referenceOffset += length;
				} else if (co.consumesReadBases()){
					// INSERTION, SOFT CLIPPING
					offset += length;
				}
			}
		}
		
		/**
		 * 
		 * @param startPosition start position as reported on the forward strand (getAlignmentStart)
		 * @param bases
		 * @param qualities
		 * @param forwardStrand
		 * @param offset
		 * @param length
		 * @param referenceOffset
		 * @param passesFilter
		 * @param readStartPosition start position of the read - depends on strand as to whether this is the alignemtnEnd or alignmentStart
		 */
		public void updateMapWithAccums(int startPosition, final byte[] bases, final byte[] qualities,
				boolean forwardStrand, int offset, int length, int referenceOffset, final boolean passesFilter, final int readEndPosition, long readNameHash) {
			
			final int startPosAndRefOffset = startPosition + referenceOffset;
			
			for (int i = 0 ; i < length ; i++) {
				Accumulator acc = array[i + startPosAndRefOffset];
				if (null == acc) {
					acc = new Accumulator(i + startPosAndRefOffset);
					array[i + startPosAndRefOffset] = acc;
				}
				if (passesFilter && qualities[i + offset] >= minBaseQual) {
					acc.addBase(bases[i + offset], qualities[i + offset], forwardStrand, 
							startPosition, i + startPosAndRefOffset, readEndPosition, readNameHash);
				} else {
//					if ((i + startPosAndRefOffset) == 4511341) {
//						System.out.println("failed filter: " + readId + " at position 4511341");
//					}
					acc.addFailedFilterBase(bases[i + offset]);
				}
			}
		}
		
		@Override
		public void run() {
			logger.info("In Consumer run method with isControl: " + isControl);
			try {
				int count = 0;
				
				while (true) {
					
					final SAMRecordFilterWrapper rec = queue.poll();
					if (null != rec) {
						
						processSAMRecord(rec);
						minStartPosition.set(rec.getRecord().getAlignmentStart());
						
						if (++ count > maxMapSize) {
							count = 0;
							
//						if (++count % maxMapSize == 0) {
							
							/*
							 * check to see how many non-null entries we have in the acc array
							 * if more than our max amount - have a rest...
							 */
							int currentPos = minStartPosition.get();
							int minPos = Math.max(0,currentPos - 10000000);
							int nonNullPositions = 0;
							for (int i = currentPos; i >= minPos ; i--) {
								if (array[i] != null) {
									nonNullPositions++;
								}
							}
							
							if (nonNullPositions > 1000000) {
								try {
									Thread.sleep(nonNullPositions / 1000);
								} catch (final InterruptedException e) {
									logger.error("InterruptedException caught in Consumer sleep",e);
									throw e;
								}
							}
						}
					} else {
						if ((singleSampleMode || controlLatch.getCount() == 0) && testLatch.getCount() == 0) {
							break;
						}
						// check the barrier - could be zero
						if (barrier.getNumberWaiting() >= (singleSampleMode ? 1 : 2)) {
//							logger.info("null record, barrier count > 2 - what now??? q.size: " + queue.size());
							// just me left
							if (queue.size() == 0 ) {
								logger.info("Consumer: Processed all records in " + currentChr + ", waiting at barrier");
								
								try {
									barrier.await();
//									assert map.isEmpty() : "Consumer: map was not empty following barrier reset";
									count = 0;
								} catch (final InterruptedException e) {
//									logger.error("Consumer: InterruptedException caught with map size: " + map.size(), e);
									throw e;
								} catch (final BrokenBarrierException e) {
//									logger.error("Consumer: BrokenBarrierException caught with map size: " + map.size(), e);
									throw e;
								}
							}
						} else {
							// sleep and try again
							try {
								Thread.sleep(50);
							} catch (final InterruptedException e) {
								logger.error("InterruptedException caught in Consumer sleep",e);
								throw e;
							}
						}
					}
				}
			} catch (final Exception e) {
				logger.error("Exception caught in Consumer thread: ", e);
				mainThread.interrupt();
			} finally {
				consumerLatch.countDown();
				logger.info("Consumer: shutting down");
			}
		}
	}
	
	
	public class Cleaner implements Runnable {
		private final CountDownLatch cleanerLatch;
		private final CountDownLatch consumerLatch;
		private final Thread mainThread;
		private int previousPosition = 0;
		private final CyclicBarrier barrier;
		private final AtomicInteger controlMinStart;
		private final AtomicInteger testMinStart;
		private final  Accumulator[] controlAccums;
		private final  Accumulator[] testAccums;
		private long processMapsCounter = 0;
		private final int buffer = 512;
		private final boolean debugLoggingEnabled;
		
		public Cleaner(CountDownLatch cleanerLatch, CountDownLatch consumerLatch, Thread mainThread, CyclicBarrier barrier,
				final AtomicInteger normalMinStart, final AtomicInteger tumourMinStart,
				final  Accumulator[] cnormalAccs, final  Accumulator[] ctumourAccs) {
			this.consumerLatch = consumerLatch;
			this.cleanerLatch = cleanerLatch;
			this.mainThread = mainThread;
			this.barrier = barrier;
			this.controlMinStart = normalMinStart;
			this.testMinStart = tumourMinStart;
			this.controlAccums = cnormalAccs;
			this.testAccums = ctumourAccs;
			debugLoggingEnabled = logger.isLevelEnabled(QLevel.DEBUG);
		}
		
		private void processMaps() {
			final int minStartPos = singleSampleMode ? testMinStart.intValue() - buffer : Math.min(controlMinStart.intValue(), testMinStart.intValue()) - buffer;
			if (debugLoggingEnabled && ++processMapsCounter % 10 == 0) {
				logger.debug("min start position: " + minStartPos + ", no of keepers so far: " + snps.size());
			}
			if (minStartPos <= 0) return;
				
			for (int i = previousPosition ; i < minStartPos ; i++) {
				
				if (null != controlAccums[i] ) {
					
					if (null != testAccums[i]) {
						processControlAndTest(controlAccums[i], testAccums[i]);
						controlAccums[i] = null;
						testAccums[i] = null;
					} else {
						processControl(controlAccums[i]);
						controlAccums[i] = null;
					}
					
				} else if (null != testAccums[i]) {
					processTest(testAccums[i]);
					testAccums[i] = null;
				}
			}
				
			previousPosition = minStartPos;
		}
		
		private void processMapsAll() {
			if (null != referenceBases ) {
			
				/*
				 * Don't need to look past the end of the contig
				 */
				int minStartPos = 0;
				int limit = referenceBasesLength + ARRAY_BUFFER;
//				int limit = testAccums.length;
				logger.info("minStartPos: " + minStartPos + ", limit: " + limit);
				if ( ! singleSampleMode) {
					
					for (int i = minStartPos ; i < limit ; i++) {
						
						if (null != controlAccums[i] ) {
							
							if (null != testAccums[i]) {
								processControlAndTest(controlAccums[i], testAccums[i]);
							} else {
								processControl(controlAccums[i]);
							}
							
						} else if (null != testAccums[i]) {
							processTest(testAccums[i]);
						}
					}
				} else {
					
					for (int i = minStartPos ; i < limit ; i++) {
						if (null != testAccums[i]) {
							processTest(testAccums[i]);
						}
					}
				}
				logger.info("leaving processMapsAll");
			}
		}
		
		@Override
		public void run() {
			logger.info("In Cleaner run method");
			try {
				Thread.sleep(500);	// sleep to allow initial population of both queues
			} catch (final InterruptedException e1) {
				e1.printStackTrace();
			}
			
			try {
				while (true) {
//					logger.info("Cleaner - about to run processMaps");
					
					processMaps();
//					logger.info("Cleaner - about to run processMaps - DONE, controlMinStart: " + controlMinStart.get() + ", testMinStart: " + testMinStart.get());
						
					if (barrier.getNumberWaiting() == barrier.getParties() - 1) {
						logger.info("Cleaner: about to hit barrier - running processMapsAll");
						
						processMapsAll();
						compoundSnps(true);
						// lets try purging here...
//						purgeNonAdjacentAccumulators();
						try {
							previousPosition = 0;
							barrier.await();
							logger.info("Cleaner: no of keepers so far: " + snps.size());
							Thread.sleep(10);	// sleep to allow initial map population
						} catch (final InterruptedException e) {
							logger.error("InterruptedException caught in Cleaner thread: ", e);
							throw e;
						} catch (final BrokenBarrierException e) {
							logger.error("BrokenBarrierException caught in Cleaner thread: ", e);
							throw e;
						}
					} else if (consumerLatch.getCount() == 0) {
						// if latches are shutdown - process remaining items in map and then exit
						logger.info("Cleaner: consumer latch == 0 - running processMapsAll");
						processMapsAll();
						break;
//					} else {
						
						/*
						 * rather than sleeping, how about some compound snp work instead???
						 */
//						compoundSnps(false);
//						try {
//							Thread.sleep(5);	
//						} catch (final InterruptedException e) {
//							logger.error("InterruptedException caught in Cleaner sleep",e);
//							throw e;
//						}
					}
				}
			} catch (final Exception e) {
				logger.error("Exception caught in Cleaner thread: ", e);
				mainThread.interrupt();
			} finally {
				cleanerLatch.countDown();
				logger.info("Cleaner: finished - counting down cleanerLatch");
			}
		}
	}
	
	private void processTest(Accumulator testAcc) {
		if (AccumulatorUtils.passesInitialCheck(testAcc, (char) referenceBases[testAcc.getPosition() - 1])) {
//		if (testAcc.containsMultipleAlleles() || 
//				(testAcc.getPosition() - 1 < referenceBasesLength 
//						&& ! baseEqualsReference(testAcc.getBase(),testAcc.getPosition() - 1))) {
			interrogateAccumulations(null, testAcc);
		}
	}
	private void processControl(Accumulator controlAcc) {
		if (AccumulatorUtils.passesInitialCheck(controlAcc, (char) referenceBases[controlAcc.getPosition() - 1])) {
//		if (controlAcc.containsMultipleAlleles() || 
//				(controlAcc.getPosition() - 1 < referenceBasesLength
//						&& ! baseEqualsReference(controlAcc.getBase(), controlAcc.getPosition() - 1))) {
			interrogateAccumulations(controlAcc, null);
		}
	}
	private void processControlAndTest(Accumulator controlAcc, Accumulator testAcc) {
		if (AccumulatorUtils.passesInitialCheck(controlAcc, testAcc, (char) referenceBases[testAcc.getPosition() - 1])) {
//		if (controlAcc.containsMultipleAlleles() || testAcc.containsMultipleAlleles() 
//				|| (controlAcc.getBase() != testAcc.getBase())
//				|| (testAcc.getPosition() - 1 < referenceBasesLength 
//						&& ! baseEqualsReference(testAcc.getBase(), testAcc.getPosition() - 1))
//				|| (controlAcc.getPosition() - 1 < referenceBasesLength
//						&& ! baseEqualsReference(controlAcc.getBase(), controlAcc.getPosition() - 1))) {
			interrogateAccumulations(controlAcc, testAcc);
		}
	}
	
//	private boolean baseEqualsReference(char base, int position) {
//		final char refBase = (char) referenceBases[position];
//		if (base == refBase) return true;
//		if (Character.isLowerCase(refBase)) {
//			return base == Character.toUpperCase(refBase);
//		} else return false;
//	}
	
	private void interrogateAccumulations(final Accumulator control, final Accumulator test) {
		
		// get coverage for both normal and tumour
		int controlCoverage = null != control ? control.getCoverage() : 0;
		int testCoverage = null != test ? test.getCoverage() : 0;
		
		if (controlCoverage + testCoverage < initialTestSumOfCountsLimit) return;
		
		/*
		 * if both test and control are not null, ensure that positions match
		 */
		if (null != control && null != test) {
			if (control.getPosition() != test.getPosition()) {
				throw new IllegalArgumentException("Control and test accumulator positions do not match!!! control: " + control.toString() + ", and test: " + test.toString());
			}
		}
		final int position = control != null ? control.getPosition() : test.getPosition();
		
		// if we are over the length of this particular sequence - return
		if (position-1 >= referenceBasesLength) return;
		
		char ref = (char) referenceBases[position-1];
		if ( ! BaseUtils.isACGT(ref)) {
			logger.warn("ignoring potential snp at " + currentChr + ":" + position + " - don't deal with ref values of: " + ref);
		} else {
			if (Character.isLowerCase(ref)) ref = Character.toUpperCase(ref);
			
			// get rule for normal and tumour
			Rule controlRule = RulesUtil.getRule(controlRules, controlCoverage);
			Rule testRule = RulesUtil.getRule(testRules, testCoverage);
			
			
			boolean [] controlPass = PileupElementLiteUtil.isAccumulatorAKeeper(control,  ref, controlRule, baseQualityPercentage);
			boolean [] testPass = PileupElementLiteUtil.isAccumulatorAKeeper(test,  ref, testRule, baseQualityPercentage);
			
			
			if (controlPass[0] || controlPass[1] || testPass[0] || testPass[1]) {
				
				/*
				 * remove overlapping reads and then check again to see if this position is of interest
				 */
				
				if (null != control) {
					AccumulatorUtils.removeOverlappingReads(control);
				}
				if (null != test) {
					AccumulatorUtils.removeOverlappingReads(test);
				}
				
				controlCoverage = null != control ? control.getCoverage() : 0;
				testCoverage = null != test ? test.getCoverage() : 0;
				if (controlCoverage + testCoverage < initialTestSumOfCountsLimit) return;
				
				
				controlRule = RulesUtil.getRule(controlRules, controlCoverage);
				testRule = RulesUtil.getRule(testRules, testCoverage);
				controlPass = PileupElementLiteUtil.isAccumulatorAKeeper(control,  ref, controlRule, baseQualityPercentage);
				testPass = PileupElementLiteUtil.isAccumulatorAKeeper(test,  ref, testRule, baseQualityPercentage);
				
				if (controlPass[0] || controlPass[1] || testPass[0] || testPass[1]) {
				
					GenotypeEnum controlGE = null != control ? AccumulatorUtils.getGenotype(control, ref, controlRule, controlPass[1], baseQualityPercentage) : null;
					GenotypeEnum testGE = null != test ?  AccumulatorUtils.getGenotype(test, ref, testRule, testPass[1], baseQualityPercentage) : null;
					String alt = GenotypeUtil.getAltAlleles(controlGE, testGE, ref);
					
					String cGT = VcfUtils.getGTStringWhenAltHasCommas(alt, ref, controlGE);
					String tGT = VcfUtils.getGTStringWhenAltHasCommas(alt, ref, testGE);
					
					Classification c = Classification.UNKNOWN;
					if ( ! singleSampleMode) {
						c = GenotypeUtil.getClassification(null != control ? AccumulatorUtils.getUniqueBasesAsString(control) : null, cGT, tGT, alt);
					}
					
					if (null == alt) {
						logger.warn("Null alt received from control GE: " + controlGE + ", testGE: " + testGE +", and ref: " + ref);
						logger.warn("Null alt received from control: " + control + ", test: " + test +", and ref: " + ref);
					}
					
					VcfRecord v = new VcfRecord.Builder(currentChr, position, ref + Constants.EMPTY_STRING).allele(alt).build();
					
					/*
					 * Flanking sequence
					 */
					final char[] cpgCharArray = new char[11];
					for (int i = 0 ; i < 11 ; i++) {
						final int refPosition = position - (6 - i);
						if (i == 5) {
							cpgCharArray[i] = alt.charAt(0);
						} else if ( refPosition >= 0 && refPosition < referenceBasesLength) {
							cpgCharArray[i] = Character.toUpperCase((char) referenceBases[refPosition]);
						} else {
							cpgCharArray[i] = '-';
						}
					}
					v.appendInfo(VcfHeaderUtils.INFO_FLANKING_SEQUENCE +Constants.EQ +String.valueOf(cpgCharArray));
					
					
					/*
					 * attempt to add format field information
					 */
					List<String> ff = new ArrayList<String>(4);
					ff.add(header);
					
					if ( ! singleSampleMode) {
						ff.add(GenotypeUtil.getFormatValues(control, cGT, alt, ref, runSBIASAnnotation, sBiasAltPercentage,sBiasCovPercentage,  c, true));
					}
					ff.add(GenotypeUtil.getFormatValues(test, tGT, alt, ref, runSBIASAnnotation, sBiasAltPercentage, sBiasCovPercentage, c, false));
					
					v.setFormatFields(ff);
					
					snps.add(v);
					
					/*
					 * populate adjacentAccumulators so that compound snp decision can be made
					 */
					adjacentAccumulators.put(v, new Pair<Accumulator, Accumulator>(control, test));
				}
			}
		}
	}
	
	/**
	 * Overloaded method
	 * @see compoundSnps(boolean complete)
	 */
	void compoundSnps() {
		compoundSnps(true);
	}
	
//	void checkForOverlaps(VcfRecord rec) {
//		
//		/*
//		 * get overlapping reads from both bam files for this position.
//		 */
//		Map<String, String[]> ffMap = rec.getFormatFieldsAsMap();
//		String [] gtArray = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE);
//		
//		/*
//		 * control first
//		 */
//		if (null != gtArray && gtArray.length > 1) {
//			
//			if ( ! StringUtils.isNullOrEmptyOrMissingData(gtArray[0]) && ! "./.".equals(gtArray[0])) {
//				/*
//				 * just get data from first control bam - need to update MultiSAMFileITerator....
//				 */
//				List<SAMRecord> recs = getRecordsAtPosition(controlSamReader, rec.getChromosome(), rec.getPosition());
//				int uniqueCount = containsOverlaps(recs);
////				logger.info("uniqueCount: " + uniqueCount + ", size: " + recs.size());
//				if (uniqueCount != recs.size()) {
//					logger.info("controlBam contains overlaps for rec, unique count: " + uniqueCount + ", recs size: " + recs.size() + ", vcf:" + rec.toSimpleString());
//				}
//			}
//			if ( ! StringUtils.isNullOrEmptyOrMissingData(gtArray[1]) && ! "./.".equals(gtArray[1])) {
//				/*
//				 * just get data from first control bam - need to update MultiSAMFileITerator....
//				 */
//				List<SAMRecord> recs = getRecordsAtPosition(testSamReader, rec.getChromosome(), rec.getPosition());
//				int uniqueCount = containsOverlaps(recs);
////				logger.info("uniqueCount: " + uniqueCount + ", size: " + recs.size());
//				if (uniqueCount != recs.size()) {
//					logger.info("testBam contains overlaps for rec, unique count: " + uniqueCount + ", recs size: " + recs.size() + ", vcf:" + rec.toSimpleString());
//				}
//			}
//		}
//	}
	
//	public static int containsOverlaps(List<SAMRecord> records) {
//		return (int) records.stream().map(SAMRecord::getReadName).distinct().count();
////		return uniqueCount == records.size();
//	}
	
	
	public static List<SAMRecord> getRecordsAtPosition(SamReader reader, String contig, int position) {
//		System.out.println("about to query " + contig + " at " + position + ", with reader: " + reader.getResourceDescription());
		SAMRecordIterator iter = reader.query(contig, position, position, false);
		List<SAMRecord> recs = new ArrayList<>();
		while (iter.hasNext()) {
			SAMRecord r = iter.next();
			if (SAMUtils.isSAMRecordValidForVariantCalling(r)) {
				recs.add(r);
			}
		}
		iter.close();
		return recs;
	}
	
	
	
	/**
	 * If complete is set to true, then we can assume that there are no more snps to be added to the <code>snps</code> collection, and so we can traverse the whole collection.<br>
	 * 
	 * If set to false, then <code>snps</code> is still being added to, and so we ignore the latest few (???) entries from the list when calculating the loloSnps list.
	 * This is because we don't know if the latest entries in <code>snps</code> are part of a compound snp.
	 * 
	 * <br><b>NOTE</b> that it is the same thread that adds to the snps collection that calls this method (apart from when it is run with <code>complete=true</code>) 
	 * and so modifying the <code>snps</code> collection should be ok. ie. should not get concurrent modification excpetions. 
	 * 
	 */
	void compoundSnps(boolean complete) {
		if (complete) {
			logger.info("in compound snp");
		}
		/*
		 *  loop through our snps
		 * if we have adjacent ones, need to do some digging to see if they might be compoundium snps
		 * 
		 * if not complete, don't send complete list to <code>PipelineUtil.listOfListOfAdjacentVcfs</code>
		 */
		if ( ! complete && snps.size() < 1000) {
			return;
		}
		
		List<List<VcfRecord>> loloSnps = PipelineUtil.listOfListOfAdjacentVcfs(complete ? snps : snps.subList(0, snps.size() - 1000));
		logger.info("Getting loloSnps [ " + loloSnps.size() + "] - DONE");
		
		if (loloSnps.isEmpty()) {
			return;
		}
		
		Set<VcfRecord> toRemove = new HashSet<>(1024 * 8);
		
		for (List<VcfRecord> loSnps : loloSnps) {
			TMap<VcfRecord, Pair<Accumulator, Accumulator>> map = new THashMap<>(loSnps.size() * 2);
			for (VcfRecord vcf : loSnps) {
				Pair<Accumulator, Accumulator> p = adjacentAccumulators.remove(vcf);
				if (null == p || null == p.getLeft() || p.getRight() == null) {
					/*
					 * We don't want to try and create a compound snp when we don't have coverage at one of the positions....
					 */
//					logger.warn("null pair of accs (or null control or test acc) in adjacentAccumulators map for vcf: " + vcf.toString() + ", part of a list of vcfs: " + loSnps.size() + " in size. ");
				} else {
					map.put(vcf, p);
				}
			}
			
			/*
			 * check that map has same number of entries as loSnps
			 */
			if (map.size() == loSnps.size()) {
				Optional<VcfRecord> optionalVcf = PipelineUtil.createCompoundSnp(map, controlRules, testRules, runSBIASAnnotation, sBiasCovPercentage, sBiasAltPercentage);
				/*
				 * if present, add to compound snps collection and remove from snps collection, if not present, do nowt
				 */
				if (optionalVcf.isPresent()) {
//					logger.info("created cs: " + optionalVcf.get().toString());
					compoundSnps.add(optionalVcf.get());
					toRemove.addAll(loSnps);
				}
			}
		}
		
		if (toRemove.size() > 0) {
			logger.info("About to call remove with toRemove size: " + toRemove.size());
			Iterator<VcfRecord> iter = snps.iterator();
			while (iter.hasNext()) {
				VcfRecord v = iter.next();
				if (toRemove.contains(v)) {
					iter.remove();
				}
			}
			logger.info("About to call remove with toRemove size: " + toRemove.size() + " - DONE");
		}
		
		logger.info("Created " + compoundSnps.size() + " compound snps so far");
	}
	
//	void accumulateReadBases(Accumulator acc, Map<Long, StringBuilder> readSeqMap, int position) {
//		final TIntCharMap map = acc.getReadIdBaseMap();
//		// get existing seq for each id
//		int[] keys = map.keys();
//		
//		for (int l : keys) {
//			char c = map.get(l);
//			Long longObject = Long.valueOf(l);
//			StringBuilder seq = readSeqMap.get(longObject);
//			if (null == seq) {
//				// initialise based on how far away we are from the start
//				seq = new StringBuilder();
//				for (int q = (position) ; q > 0 ; q--) {
//					seq.append("_");
//				}
//				readSeqMap.put(longObject, seq);
//			}
//			seq.append(c);
//		}
//		// need to check that all elements have enough data in them
//		for (final StringBuilder sb : readSeqMap.values()) {
//			if (sb.length() <= position) {
//				sb.append("_");
//			}
//		}
//	}
	
}
