/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */

package org.qcmg.snp;

import gnu.trove.map.TIntCharMap;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.picard.reference.FastaSequenceFile;
import net.sf.picard.reference.ReferenceSequence;
import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;

import org.ini4j.Ini;
import org.qcmg.common.log.QLevel;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QBamId;
import org.qcmg.common.meta.QDccMeta;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.Accumulator;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.model.PileupElementLite;
import org.qcmg.common.model.Rule;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.Pair;
import org.qcmg.common.util.PileupElementLiteUtil;
import org.qcmg.common.util.PileupUtils;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.maths.FisherExact;
import org.qcmg.picard.MultiSAMFileIterator;
import org.qcmg.picard.MultiSAMFileReader;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMRecordFilterWrapper;
import org.qcmg.picard.util.PileupElementUtil;
import org.qcmg.picard.util.QBamIdFactory;
import org.qcmg.picard.util.QDccMetaFactory;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.pileup.QSnpRecord;
import org.qcmg.pileup.QSnpRecord.Classification;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.snp.util.GenotypeComparisonUtil;
import org.qcmg.snp.util.IniFileUtil;
import org.qcmg.snp.util.RulesUtil;
import org.qcmg.vcf.VCFFileWriter;

public abstract class Pipeline {
	
	static final String ILLUMINA_MOTIF = "GGT";
	
	//TODO these will need to become user defined values at some point
	int novelStartsFilterValue = 4;
	int mutantReadsFilterValue = 5;
	
	static int initialTestSumOfCountsLimit = 3;
	static int baseQualityPercentage = 10;
	
	static int sBiasAltPercentage = 5;
	static int sBiasCovPercentage = 5;
	
	// STATS FOR CLASSIFIER
	long classifyCount = 0;
	long classifyGermlineCount = 0;
	long classifySomaticCount = 0;
	long classifyGermlineLowCoverage = 0;
	long classifySomaticLowCoverage = 0;
	long classifyNoClassificationCount = 0;
	long classifyNoMutationCount = 0;
	
	long pValueCount = 0;

	/*///////////////////////
	 * COLLECTIONS
	 *///////////////////////
	
	// ChrPosition chromosome consists of "chr" and then the number/letter
	final Map<ChrPosition,QSnpRecord> positionRecordMap = new HashMap<>(100000);
	
	final Map<ChrPosition,VcfRecord> compoundSnps = new HashMap<>();
	
	// just used to store adjacent accumulators used by compound snp process
	final ConcurrentMap<ChrPosition, Pair<Accumulator, Accumulator>> adjacentAccumulators = new ConcurrentHashMap<>(); 
	
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
	
	/*/////////////////////////////////////
	 * ABSTRACT METHODS
	 *////////////////////////////////////
	
//	protected abstract void ingestIni(Wini ini) throws SnpException;
	protected abstract String getFormattedRecord(final QSnpRecord record, final String ensemblChr);
	protected abstract String getOutputHeader(final boolean isSomatic);
	
	
	/*////////////////////////////////////////
	 * IMPLEMENTED METHODS
	 *////////////////////////////////////////
	
	
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
	
	String getDccMetaData() throws Exception {
		if (null == controlBams || controlBams.length == 0 || 
				StringUtils.isNullOrEmpty(controlBams[0]) 
				|| null == testBams || testBams.length == 0 
				|| StringUtils.isNullOrEmpty(testBams[0])) return null;
		
		final SAMFileHeader controlHeader = SAMFileReaderFactory.createSAMFileReader(controlBams[0]).getFileHeader();
		final SAMFileHeader analysisHeader = SAMFileReaderFactory.createSAMFileReader(testBams[0]).getFileHeader();
		
		final QDccMeta dccMeta = QDccMetaFactory.getDccMeta(qexec, controlHeader, analysisHeader, "qSNP");
		
		return dccMeta.getDCCMetaDataToString();
	}
	
	QBamId [] getBamIdDetails(String [] bamFileName) throws Exception {
		if (null != bamFileName && bamFileName.length > 0) {
			final QBamId [] bamIds = new QBamId[bamFileName.length];
			for (int i = 0 ; i < bamFileName.length ; i++) {
				bamIds[i] = QBamIdFactory.getBamId(bamFileName[i]);
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
		
		final List<ChrPosition> orderedList = new ArrayList<ChrPosition>(positionRecordMap.keySet());
		orderedList.addAll(compoundSnps.keySet());
		Collections.sort(orderedList);
		

		try (VCFFileWriter writer = new VCFFileWriter(new File(outputFileName));) {			
			final VcfHeader header = getHeaderForQSnp(patientId, controlSampleId, testSampleId, "qSNP v" + Main.version, normalBamIds, tumourBamIds, qexec.getUuid().getValue());
			VcfHeaderUtils.addQPGLineToHeader(header, qexec.getToolName().getValue(), qexec.getToolVersion().getValue(), qexec.getCommandLine().getValue() 
					+ (StringUtils.isNullOrEmpty(runMode) ? "" : " [runMode: " + runMode + "]"));
			
			// add in existing header details
			if (null != existingHeader) {
				VcfHeader mergedHeader = VcfHeaderUtils.mergeHeaders(header, existingHeader, true);
			}
			for (VcfHeader.Record hr : header) {
				writer.addHeader(hr.toString());
			}

			for (final ChrPosition position : orderedList) {
				
				final QSnpRecord record = positionRecordMap.get(position);
				if (null != record && null != record.getClassification()) {									
					// only write output if its been classified
					writer.add(convertQSnpToVCF(record));
				} else {
					// get from copoundSnp map
					VcfRecord vcf = compoundSnps.get(position);
					
					// if filter is set to missing data, then set to PASS
					if (Constants.MISSING_DATA_STRING.equals(vcf.getFilter())) {
						vcf.setFilter(SnpUtils.PASS);
					}
					
					writer.add(vcf);
				}
			}
		}
	}

	private VcfHeader getHeaderForQSnp(final String patientId,  final String normalSampleId, final String tumourSampleId, 
			final String source, QBamId[] normalBamIds, QBamId[] tumourBamIds, String uuid) throws Exception {
		
		final VcfHeader header = new VcfHeader();
		final DateFormat df = new SimpleDateFormat("yyyyMMdd");

		header.parseHeaderLine(VcfHeaderUtils.CURRENT_FILE_VERSION);		
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + df.format(Calendar.getInstance().getTime()));		
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + QExec.createUUid());		
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=" + source);		
		
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_DONOR_ID + "=" + patientId);
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=" + normalSampleId);		
		header.parseHeaderLine(VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=" + tumourSampleId);		
		
		if (null != normalBamIds)  
			for (final QBamId s : normalBamIds) {
				header.parseHeaderLine( VcfHeaderUtils.STANDARD_CONTROL_BAM  + "=" + s.getBamName());
				header.parseHeaderLine( "##qControlBamUUID=" + s.getUUID());
 			}

		if (null != tumourBamIds)  
			for (final QBamId s : tumourBamIds) {
				header.parseHeaderLine(VcfHeaderUtils.STANDARD_TEST_BAM  + "=" + s.getBamName());
				header.parseHeaderLine("##qTestBamUUID=" + s.getUUID());
			}		
		header.parseHeaderLine( "##qAnalysisId=" + uuid );
		
		header.addInfoLine(VcfHeaderUtils.INFO_FLANKING_SEQUENCE, "1", "String","Flanking sequence either side of variant");																	

		header.addFilterLine(VcfHeaderUtils.FILTER_COVERAGE_NORMAL_12, "Less than 12 reads coverage in normal");
		header.addFilterLine(VcfHeaderUtils.FILTER_COVERAGE_NORMAL_8,"Less than 8 reads coverage in normal");  
		header.addFilterLine(VcfHeaderUtils.FILTER_COVERAGE_TUMOUR,"Less than 8 reads coverage in tumour"); 
		header.addFilterLine(VcfHeaderUtils.FILTER_SAME_ALLELE_NORMAL,"Less than 3 reads of same allele in normal");  
		header.addFilterLine(VcfHeaderUtils.FILTER_SAME_ALLELE_TUMOUR,"Less than 3 reads of same allele in tumour");  
		header.addFilterLine(VcfHeaderUtils.FILTER_MUTATION_IN_NORMAL,"Mutation also found in pileup of normal");  
		header.addFilterLine(VcfHeaderUtils.FILTER_MUTATION_IN_UNFILTERED_NORMAL,"Mutation also found in pileup of (unfiltered) normal");  
		header.addFilterLine(VcfHeaderUtils.FILTER_GERMLINE,"Mutation is a germline variant in another patient");  
		header.addFilterLine(VcfHeaderUtils.FILTER_NOVEL_STARTS,"Less than 4 novel starts not considering read pair");  
		header.addFilterLine(VcfHeaderUtils.FILTER_MUTANT_READS,"Less than 5 mutant reads"); 
		header.addFilterLine(VcfHeaderUtils.FILTER_MUTATION_EQUALS_REF,"Mutation equals reference"); 
		header.addFilterLine(VcfHeaderUtils.FILTER_NO_CALL_IN_TEST,"No call in test"); 
		header.addFilterLine(VcfHeaderUtils.FILTER_STRAND_BIAS_ALT,"Alternate allele on only one strand (or percentage alternate allele on other strand is less than " + sBiasAltPercentage + "%)"); 
		header.addFilterLine(VcfHeaderUtils.FILTER_STRAND_BIAS_COV,"Sequence coverage on only one strand (or percentage coverage on other strand is less than " + sBiasCovPercentage + "%)"); 
	
		header.addFormatLine(VcfHeaderUtils.FORMAT_GENOTYPE, "1", "String" ,"Genotype: 0/0 homozygous reference; 0/1 heterozygous for alternate allele; 1/1 homozygous for alternate allele");
		header.addFormatLine(VcfHeaderUtils.FORMAT_GENOTYPE_DETAILS, "1", "String","Genotype details: specific alleles (A,G,T or C)");
		header.addFormatLine(VcfHeaderUtils.FORMAT_ALLELE_COUNT, "1", "String","Allele Count: lists number of reads on forward strand [avg base quality], reverse strand [avg base quality]");
		header.addFormatLine(VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP, "1", "String","Allele Count Compound Snp: lists read sequence and count (forward strand, reverse strand)");
		header.addFormatLine(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS, "1", "String","Allelic depths for the ref and alt alleles in the order listed");
		header.addFormatLine(VcfHeaderUtils.FORMAT_READ_DEPTH, "1", "String","Approximate read depth (reads with MQ=255 or with bad mates are filtered)");
		header.addFormatLine(VcfHeaderUtils.FORMAT_GENOTYPE_QUALITY, "1", "String","Genotype Quality");
		header.addFormatLine(VcfHeaderUtils.FORMAT_MUTANT_READS,  "1", "String","Number of mutant/variant reads");
		header.addFormatLine(VcfHeaderUtils.FORMAT_NOVEL_STARTS, "1", "String","Number of novel starts not considering read pair");		

		if (singleSampleMode) {
			header.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + testSampleId);
		} else {
			header.parseHeaderLine(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + controlSampleId + "\t" + testSampleId);
		}
		return  header;
	}
	
	void classifyPileupRecord(QSnpRecord record) {
		if (null != record.getNormalGenotype() && null != record.getTumourGenotype()) {
			classifyCount++;
			GenotypeComparisonUtil.compareGenotypes(record);
		} else if (singleSampleMode) {
			GenotypeComparisonUtil.singleSampleGenotype(record);
		} else {
			// new code to deal with only 1 genotype being available (due to low/no coverage in normal or tumour)
			// only go in here if we don't have a classification set
			// in vcf mode this can be set when there is low coverage (ie. not enough to provide a genotype)
			// need to make sure that in torrent mode, when we come to re-assessing a position that this doesn't bite us...
			if (null == record.getClassification()) {
				GenotypeComparisonUtil.compareSingleGenotype(record);
			}
			
			if (null == record.getNormalGenotype())  {
				classifyGermlineCount++;
				if (record.getNormalCount() > 0) classifyGermlineLowCoverage++;
			} else  {
				classifySomaticCount++;
				if (record.getTumourCount() > 0) classifySomaticLowCoverage++;
			}
			if (null == record.getClassification()) classifyNoClassificationCount++;
			if (null == record.getMutation()) classifyNoMutationCount++;
		}
		
		if (Classification.SOMATIC == record.getClassification() || Classification.UNKNOWN == record.getClassification()) {
			
			final String altString = null != record.getMutation() ? SnpUtils.getAltFromMutationString(record.getMutation()) : null;
//			final char alt = null != record.getMutation() ? SnpUtils.getAltFromMutationString(record.getMutation()) : '\u0000';
			final int mutantReadCount = SnpUtils.getCountFromNucleotideString(record.getTumourNucleotides(), altString);
			if (mutantReadCount < mutantReadsFilterValue) {
				VcfUtils.updateFilter(record.getVcfRecord(), SnpUtils.MUTANT_READS);
			}
			if (record.getNovelStartCount() < novelStartsFilterValue) {
				VcfUtils.updateFilter(record.getVcfRecord(), SnpUtils.NOVEL_STARTS);
			}
		}
		
		// set to PASS if no other filters have been set
		if (StringUtils.isNullOrEmpty(record.getAnnotation()) || "--".equals(record.getAnnotation())) {
			VcfUtils.updateFilter(record.getVcfRecord(), VcfHeaderUtils.FILTER_PASS);
		}
	}
	
	void classifyPileup() {
		
		for (final QSnpRecord record : positionRecordMap.values()) {
			classifyPileupRecord(record);
		}
		logger.info("No of records that have a genotype: " + classifyCount + ", out of " + positionRecordMap.size() 
				+ ". Somatic: " + classifySomaticCount + "[" + classifySomaticLowCoverage + "], germline: " 
				+ classifyGermlineCount + "[" + classifyGermlineLowCoverage + "] no classification: " + classifyNoClassificationCount + ", no mutation: " + classifyNoMutationCount);
	}
	
	
	
	private List<SAMFileHeader> getBamFileHeaders(String[] bams) {
		final List<SAMFileHeader> headers = new ArrayList<SAMFileHeader>();
		for (final String bam : bams) {
			final SAMFileHeader header = SAMFileReaderFactory.createSAMFileReader(bam).getFileHeader();
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
	
	private List<SAMSequenceDictionary> getSequenceDictionaries(List<SAMFileHeader> headers) {
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
	
	/**
	 * For SOMATIC positions, that don't currently have evidence of the mutation in the normal, examine
	 * the unfiltered normal to see if any evidence exists there
	 * 
	 */
	void incorporateUnfilteredNormal() {
		int noOfAffectedRecords = 0;
		for (final Entry<ChrPosition, QSnpRecord> entry : positionRecordMap.entrySet()) {
			final QSnpRecord record = entry.getValue();
			if (Classification.SOMATIC == record.getClassification()
					&& (null == record.getAnnotation() 
					|| ! record.getAnnotation().contains(SnpUtils.MUTATION_IN_NORMAL))	// PASS will pass this test :)
					&& ! StringUtils.isNullOrEmpty(record.getUnfilteredNormalPileup())) {
				
				final char alt = record.getMutation().charAt(record.getMutation().length()-1);
				
				if (record.getUnfilteredNormalPileup().indexOf(Character.toUpperCase(alt)) > -1) {
					VcfUtils.updateFilter(record.getVcfRecord(), SnpUtils.MUTATION_IN_UNFILTERED_NORMAL);
					noOfAffectedRecords++;
				}
			}
		}
		
		logger.info("number of SOMATIC snps that have evidence of mutation in unfiltered normal: " + noOfAffectedRecords);
	}
	
	public  VcfRecord convertQSnpToVCF(QSnpRecord rec) throws Exception {
		final VcfRecord vcf = rec.getVcfRecord();
		
		String altString = null != rec.getMutation() ? SnpUtils.getAltFromMutationString(rec.getMutation()) : null;
		// if this is null, see if we can get it from vcf record
		if (null == altString) {
			altString = vcf.getAlt();
		}
		
		final int controlMutantReadCount = SnpUtils.getCountFromNucleotideString(rec.getNormalNucleotides(), altString);
		final int testMutantReadCount = SnpUtils.getCountFromNucleotideString(rec.getTumourNucleotides(), altString);
		final int controlNovelStartCount = rec.getNormalNovelStartCount();
		final int testNovelStartCount = rec.getTumourNovelStartCount();
		
		vcf.addFilter(rec.getAnnotation());		// don't overwrite existing annotations
		
		// if filter field is set to "." - update to PASS
		if (Constants.MISSING_DATA_STRING.equals(vcf.getFilter())) {
			VcfUtils.updateFilter(vcf, SnpUtils.PASS);
		}
		
		
		//work with INFO field 
		String info = "";
		if (Classification.SOMATIC == rec.getClassification()) {
			info = StringUtils.addToString(info, Classification.SOMATIC.toString(), Constants.SEMI_COLON);			
		}
		
		// add Number of Mutations (MR - Mutated Reads)
//		if (mutantReadCount > 0) {
//			info = StringUtils.addToString(info,VcfHeaderUtils.FORMAT_MUTANT_READS +Constants.EQ +mutantReadCount  , Constants.SEMI_COLON);
//		}
//		
//		if (novelStartCount > 0) {
//			info = StringUtils.addToString(info, VcfHeaderUtils.FORMAT_NOVEL_STARTS +Constants.EQ +novelStartCount  , Constants.SEMI_COLON);
//		}
		
		// cpg data
		if ( ! StringUtils.isNullOrEmpty(rec.getFlankingSequence())) {
			info = StringUtils.addToString(info, VcfHeaderUtils.INFO_FLANKING_SEQUENCE +Constants.EQ +rec.getFlankingSequence()  , Constants.SEMI_COLON);
		}
		
		vcf.setInfo(info);
		
		
		//Alt Field
		final String [] altAndGTs = VcfUtils.getMutationAndGTs(rec.getRef(), rec.getNormalGenotype(), rec.getTumourGenotype());
		vcf.setAlt(altAndGTs[0]);
		
		// get existing format field info from vcf record
		final List<String> additionalformatFields = new ArrayList<>();
		
		// FORMAT field - contains GT field (and others)
		final StringBuilder formatField = new StringBuilder();
		// add in the columns
		formatField.append(VcfHeaderUtils.FORMAT_GENOTYPE).append(Constants.COLON);
		formatField.append(VcfHeaderUtils.FORMAT_GENOTYPE_DETAILS).append(Constants.COLON);
		formatField.append(VcfHeaderUtils.FORMAT_ALLELE_COUNT).append(Constants.COLON);
		formatField.append(VcfHeaderUtils.FORMAT_MUTANT_READS).append(Constants.COLON);
		formatField.append(VcfHeaderUtils.FORMAT_NOVEL_STARTS);
		additionalformatFields.add(formatField.toString());
		
		final String normalGDField = null != rec.getNormalGenotype() ? rec.getNormalGenotype().getDisplayString() : Constants.MISSING_DATA_STRING;
		final String tumourGDField = null != rec.getTumourGenotype() ? rec.getTumourGenotype().getDisplayString() : Constants.MISSING_DATA_STRING;
		
		// add in normal format details first, then tab, then tumour
		if ( ! singleSampleMode) {
			formatField.setLength(0);
			formatField.append(altAndGTs[1]).append(Constants.COLON);
			formatField.append(normalGDField).append(Constants.COLON);
			final String nNucleotides = StringUtils.isNullOrEmpty(rec.getNormalNucleotides()) ? Constants.MISSING_DATA_STRING : rec.getNormalNucleotides(); 
			
			formatField.append(nNucleotides.replace(":", "")).append(Constants.COLON);// remove colons in nucleotide strings
			
			// add in MR and NNS data
			formatField.append(controlMutantReadCount).append(Constants.COLON);
			formatField.append(controlNovelStartCount);
//			formatField.append(Constants.TAB);
//			vcf.addFormatField(1, formatField.toString());
			additionalformatFields.add(formatField.toString());
		}
		
		// tumour
		final String tNucleatides = StringUtils.isNullOrEmpty(rec.getTumourNucleotides()) ? Constants.MISSING_DATA_STRING : rec.getTumourNucleotides();
		formatField.setLength(0);
		formatField.append(altAndGTs[2]).append(Constants.COLON);
		formatField.append(tumourGDField).append(Constants.COLON);
		formatField.append(tNucleatides.replace(":", "")).append(Constants.COLON);// remove colons in nucleotide strings
		formatField.append(testMutantReadCount).append(Constants.COLON);
		formatField.append(testNovelStartCount);
		additionalformatFields.add(formatField.toString());
		
		VcfUtils.addFormatFieldsToVcf(vcf, additionalformatFields);

//		vcf.addFormatField(singleSampleMode ? 1 : 2, formatField.toString());		
//		vcf.setFormatField(formatField.toString());
		return vcf;
	}
	
	/**
	 * TODO
	 * add the appropriate flag should the motif be found
	 * 
	 */
	void incorporateCPGData() {
		final int noOfAffectedRecords = 0;
		for (final QSnpRecord record : positionRecordMap.values()) {
			
			final String cpg = record.getFlankingSequence(); 
			if (null != cpg) {
				/*
				 * if (motif is in cpg)
				 * 	noOfAffectedRecords++;
				 */
			}
		}
		
		logger.info("number of snps that have evidence of mutation in unfiltered normal: " + noOfAffectedRecords);
	}
	
	void parsePileup(String record) throws IOException {
		final String[] params = TabTokenizer.tokenize(record);

		// get coverage for both normal and tumour
		final int normalCoverage = PileupUtils.getCoverageCount(params, controlStartPositions);
		final int tumourCoverage = PileupUtils.getCoverageCount(params, testStartPositions);
		
		if (normalCoverage + tumourCoverage < initialTestSumOfCountsLimit) return;

		final String normalBases = PileupUtils.getBases(params, controlStartPositions);
		final String tumourBases = PileupUtils.getBases(params, testStartPositions);
		
		if ( ! includeIndels) {
			// means there is an indel at this position - ignore
			if (PileupUtils.doesPileupContainIndel(normalBases)) return;
			if (PileupUtils.doesPileupContainIndel(tumourBases)) return;
		}
		
		final String normalBaseQualities = PileupUtils.getQualities(params, controlStartPositions);
		final String tumourBaseQualities = PileupUtils.getQualities(params, testStartPositions);

		// get bases as PileupElement collections
		final List<PileupElement> normalBaseCounts = PileupElementUtil.getPileupCounts(normalBases, normalBaseQualities);
		final List<PileupElement> tumourBaseCounts = PileupElementUtil.getPileupCounts(tumourBases, tumourBaseQualities);

		// get variant count for both
		final int normalVariantCount = PileupElementUtil.getLargestVariantCount(normalBaseCounts);
		final int tumourVariantCount = PileupElementUtil.getLargestVariantCount(tumourBaseCounts);
		
		// new - hope this doesn't bugger things up!!
		// only proceed if we actually have at least 1 variant
		if (normalVariantCount + tumourVariantCount == 0) return;
		
		// get rule for normal and tumour
		final Rule normalRule = RulesUtil.getRule(controlRules, normalCoverage);
		final Rule tumourRule = RulesUtil.getRule(testRules, tumourCoverage);
		
		
		final boolean normalFirstPass = isPileupRecordAKeeperFirstPass(normalVariantCount, normalCoverage, normalRule, normalBaseCounts, baseQualityPercentage);;
		boolean normalSecondPass = false;
		boolean tumourFirstPass = false;
		boolean tumourSecondPass = false;
		
		if ( ! normalFirstPass) {
			normalSecondPass = isPileupRecordAKeeperSecondPass(normalVariantCount, normalCoverage, normalRule, normalBaseCounts, baseQualityPercentage);
			
			if ( ! normalSecondPass) {
				tumourFirstPass = isPileupRecordAKeeperFirstPass(tumourVariantCount, tumourCoverage, tumourRule, tumourBaseCounts, baseQualityPercentage);
				
				if ( ! tumourFirstPass) {
					tumourSecondPass = isPileupRecordAKeeperSecondPass(tumourVariantCount, tumourCoverage, tumourRule, tumourBaseCounts, baseQualityPercentage);
				}
			}
		}
		
		// only keep record if it has enough variants
		if (normalFirstPass || normalSecondPass || tumourFirstPass || tumourSecondPass) {
			
			// if normal passed, need to test tumour to see what rule to use
			if (normalFirstPass || normalSecondPass) {
				tumourFirstPass = isPileupRecordAKeeperFirstPass(tumourVariantCount, tumourCoverage, tumourRule, tumourBaseCounts, baseQualityPercentage);
				
				if ( ! tumourFirstPass) {
					tumourSecondPass = isPileupRecordAKeeperSecondPass(tumourVariantCount, tumourCoverage, tumourRule, tumourBaseCounts, baseQualityPercentage);
				}
			}

			//TODO check that this is right (param[3] == alt)
			final QSnpRecord qRecord = new QSnpRecord(params[0], Integer.parseInt(params[1]), params[2], params[3]);
			qRecord.setPileup(record);
			// setup some values on the record
//			qRecord.setRef(params[2].charAt(0));
			qRecord.setNormalCount(normalCoverage);
			qRecord.setTumourCount(tumourCoverage);
			
			
			final String refString = qRecord.getRef();
			if (refString.length() > 1) {
				logger.warn("refString: " + refString + " in Pipeline.parePileup");
			}
			final char ref = refString.charAt(0) ;
			
			// set normal pileup to only contain the different bases found in normal, rather than the whole pileup string
			// which contains special chars indicating start/end of reads along with mapping qualities
			if (normalCoverage > 0)
				qRecord.setNormalPileup(PileupElementUtil.getBasesFromPileupElements(normalBaseCounts, ref));

			// use all base counts to form genotype
			final List<PileupElement> normalBaseCountsPassRule = PileupElementUtil
				.getPileupCountsThatPassRule(normalBaseCounts, normalRule, normalSecondPass, baseQualityPercentage);
			final List<PileupElement> tumourBaseCountsPassRule = PileupElementUtil
				.getPileupCountsThatPassRule(tumourBaseCounts, tumourRule, tumourSecondPass, baseQualityPercentage);
			
			qRecord.setNormalGenotype(PileupElementUtil.getGenotype(normalBaseCountsPassRule, ref));
			qRecord.setTumourGenotype(PileupElementUtil.getGenotype(tumourBaseCountsPassRule, ref));
			
			qRecord.setNormalNucleotides(PileupElementUtil.getPileupElementString(normalBaseCounts, ref));
			qRecord.setTumourNucleotides(PileupElementUtil.getPileupElementString(tumourBaseCounts, ref));
			
			// set Id
			qRecord.setId(++mutationId);
			positionRecordMap.put(new ChrPosition(qRecord.getChromosome(), qRecord.getPosition()), qRecord);
		}
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
	
	/**
	 * use the available threads to get the fisher exact test two tailed pvalues into the probability field of the qsnp records
	 */
	void populateProbabilities() {
		logger.info("About to hit Fisher Exact Test two tailed pvalue");
		final Queue<QSnpRecord> queue = new ConcurrentLinkedQueue<QSnpRecord>();
		for (final QSnpRecord rec : positionRecordMap.values()) {
			queue.add(rec);
		}
		
		final int noOfThreadsToUse = 5;
		final ExecutorService service = Executors.newFixedThreadPool(noOfThreadsToUse);
		for (int i = 0 ; i < noOfThreadsToUse ; i++) {
			service.execute(new Runnable() {

				@Override
				public void run() {
					// take a QSnpRecord, if none left we are done
					while (true) {
						final QSnpRecord record = queue.poll();
						if (null == record) break;
						
						final String mutation = record.getMutation();
						if (StringUtils.isNullOrEmpty(mutation)) continue;
						
						final String ref = record.getRef();
						final String alt = SnpUtils.getAltFromMutationString(mutation);
						
						final int aNormalAlt = SnpUtils.getCountFromNucleotideString(record.getNormalNucleotides(), alt);
						final int bTumourAlt = SnpUtils.getCountFromNucleotideString(record.getTumourNucleotides(), alt);
						final int cNormalRef = SnpUtils.getCountFromNucleotideString(record.getNormalNucleotides(), ref);
						final int dTumourRef = SnpUtils.getCountFromNucleotideString(record.getTumourNucleotides(), ref);
						
						final double pValue = FisherExact.getTwoTailedFET(aNormalAlt, bTumourAlt, cNormalRef, dTumourRef);
	//					logger.info("pvalue for following values (a,b,c,d:pvalue): " + normalAlt + "," + tumourAlt + "," + normalRef + "," + tumourRef + ": " + pValue);
						record.setProbability(pValue);
					}
				}});
		}
		service.shutdown();
		
		try {
			service.awaitTermination(10, TimeUnit.HOURS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
		
		logger.info("About to hit Fisher Exact Test two tailed pvalue - DONE");
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
		final Queue<SAMRecordFilterWrapper> normalSAMQueue = new ConcurrentLinkedQueue<SAMRecordFilterWrapper>();
		final Queue<SAMRecordFilterWrapper> tumourSAMQueue = new ConcurrentLinkedQueue<SAMRecordFilterWrapper>();
		
		// used by Cleaner3 threads
		final ConcurrentMap<Integer, Accumulator> cnormalAccs = new ConcurrentHashMap<Integer, Accumulator>();
		final ConcurrentMap<Integer, Accumulator> ctumourAccs = new ConcurrentHashMap<Integer, Accumulator>(1024 * 1024);
		
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
		
		
		// Control threads (if not single sample)
		if ( ! singleSampleMode) {
			service.execute(new Producer(controlBams, controlProducerLatch, true, normalSAMQueue, Thread.currentThread(), query, barrier, includeDups));
			service.execute(new Consumer(consumerLatch, controlProducerLatch, testProducerLatch, true, 
					Thread.currentThread(), barrier, cnormalAccs, normalSAMQueue, controlMinStart));
		}
		
		// test threads
		service.execute(new Producer(testBams, testProducerLatch, false, tumourSAMQueue, Thread.currentThread(), query, barrier, includeDups));
		service.execute(new Consumer(consumerLatch, controlProducerLatch, testProducerLatch, false, 
				Thread.currentThread(), barrier, ctumourAccs, tumourSAMQueue, testMinStart));
		
		// Cleaner
		service.execute(new Cleaner(cleanerLatch, consumerLatch, Thread.currentThread(),
				barrier, controlMinStart, testMinStart, cnormalAccs, ctumourAccs));
		
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
		private final MultiSAMFileIterator iter;
		private final boolean isControl;
		private final CountDownLatch latch;
		private final Queue<SAMRecordFilterWrapper> queue;
		private QueryExecutor qbamFilter;
		private final Thread mainThread;
		private long passedFilterCount = 0;
		private long invalidCount = 0;
		private long counter = 0;
		private int chrCounter = 0;
		private final CyclicBarrier barrier;
		private final boolean includeDups;
		private final boolean runqBamFilter;
		
		public Producer(final String[] bamFiles, final CountDownLatch latch, final boolean isNormal, 
				final Queue<SAMRecordFilterWrapper> samQueue, final Thread mainThread, final String query, 
				final CyclicBarrier barrier, boolean includeDups) throws Exception {
			this.latch = latch;
			final Set<File> bams = new HashSet<File>();
			for (final String bamFile : bamFiles) {
				bams.add(new File(bamFile));
			}
			this.reader = new MultiSAMFileReader(bams, true, validation);
			this.iter = reader.getMultiSAMFileIterator();
			this.isControl = isNormal;
			this.mainThread = mainThread;
			this.queue = samQueue;
			if ( ! StringUtils.isNullOrEmpty(query) && ! "QCMG".equals(query))
				qbamFilter = new QueryExecutor(query);
			this.barrier = barrier;
			this.includeDups = includeDups;
			runqBamFilter = null != qbamFilter;
		}

		@Override
		public void run() {
			logger.info("In Producer run method with isControl: " + isControl);
			logger.info("Use qbamfilter? " + runqBamFilter);
			try {
				
				while (iter.hasNext()) {
					final SAMRecord record = iter.next();
					chrCounter++;
					if (++ counter % 1000000 == 0) {
						int qSize = queue.size();
						logger.info("hit " + counter/1000000 + "M sam records, passed filter: " + passedFilterCount + ", qsize: " + qSize);
						if (passedFilterCount == 0 && counter >= noOfRecordsFailingFilter) {
							throw new SnpException("INVALID_FILTER", ""+counter);
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
					
					if (record.getReferenceName().equals(currentChr)) {
						processRecord(record);
					} else if (null == currentChr) {
						// no longer have reference details - exit
						logger.warn("Exiting Producer despite records remaining in file - null reference chromosome");
						logger.warn("extra record: " + SAMUtils.getSAMRecordAsSting(record));
						break;
					} else {
						logger.info("Producer: Processed all records in " + currentChr + ", waiting at barrier");
						try {
							barrier.await();
							// don't need to reset barrier, threads waiting at barrier are released when all threads reach barrier... oops
//							if (isNormal) barrier.reset();		// reset the barrier
						} catch (final InterruptedException e) {
							logger.error("Producer: InterruptedException exception caught whilst processing record: " + SAMUtils.getSAMRecordAsSting(record), e);
							throw e;
						} catch (final BrokenBarrierException e) {
							logger.error("Producer: BrokenBarrier exception caught whilst processing record: " + SAMUtils.getSAMRecordAsSting(record), e);
							throw e;
						}
						
						// reset counter
						chrCounter = 1;
						
						// wait until queues are empty
						int qSize = queue.size();
						if (qSize > 0)
							logger.info("Waiting for empty queue before continuing with next chr. qsize: " + qSize);
						while (qSize > 0) {
							try {
								Thread.sleep(100);
							} catch (final InterruptedException e) {
								e.printStackTrace();
								throw e;
							}
							qSize = queue.size();
						}
						// deal with this record
						processRecord(record);
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
				if (isControl || passesFilter) {
					addRecordToQueue(record, passesFilter);
				}
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
//			if (record.getReadNegativeStrandFlag()) record.getAlignmentEnd();		// cache alignment end if its on reverse strand
			
			final SAMRecordFilterWrapper wrapper = new SAMRecordFilterWrapper(record, chrCounter);
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
		private final  ConcurrentMap<Integer, Accumulator> map;
		private final CyclicBarrier barrier;
		private final Queue<SAMRecordFilterWrapper> queue;
		private final AtomicInteger minStartPosition;
//		private final boolean singleSampleMode;
		
		private final int maxMapSize = 100000;
		
		public Consumer(final CountDownLatch consumerLatch, final CountDownLatch normalLatch, 
				final CountDownLatch tumourLatch, final boolean isNormal, final Thread mainThread, 
				final CyclicBarrier barrier, final ConcurrentMap<Integer, Accumulator> map,
				final Queue<SAMRecordFilterWrapper> queue, final AtomicInteger minStartPosition){
			
			this.consumerLatch = consumerLatch;
			this.controlLatch = normalLatch;
			this.testLatch = tumourLatch;
			this.isControl = isNormal;
			this.mainThread = mainThread;
			this.map =  map;
			this.barrier = barrier;
			this.queue = queue;
			this.minStartPosition = minStartPosition;
		}
		
		private void processSAMRecord(final SAMRecordFilterWrapper record) {
			final SAMRecord sam = record.getRecord();
			final boolean forwardStrand = ! sam.getReadNegativeStrandFlag();
			final int startPosition = sam.getAlignmentStart();
			// endPosition is just that for reverse strand, but for forward strand reads it is start position
			final int endPosition = sam.getAlignmentEnd();
			final byte[] bases = sam.getReadBases();
			final byte[] qualities = record.getPassesFilter() ? sam.getBaseQualities() : null;
			final Cigar cigar = sam.getCigar();
			
			int referenceOffset = 0, offset = 0;
			
			for (final CigarElement ce : cigar.getCigarElements()) {
				final CigarOperator co = ce.getOperator();
				final int length = ce.getLength();

				if (co.consumesReferenceBases() && co.consumesReadBases()) {
					// we have a number (length) of bases that can be advanced.
					updateMapWithAccums(startPosition, bases,
							qualities, forwardStrand, offset, length, referenceOffset, 
							record.getPassesFilter(), endPosition, (int) record.getPosition());
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
				boolean forwardStrand, int offset, int length, int referenceOffset, final boolean passesFilter, final int readEndPosition, int readId) {
			
			final int startPosAndRefOffset = startPosition + referenceOffset;
			
			for (int i = 0 ; i < length ; i++) {
				Accumulator acc = map.get(i + startPosAndRefOffset);
				if (null == acc) {
					acc = new Accumulator(i + startPosAndRefOffset);
					final Accumulator oldAcc = map.putIfAbsent(i + startPosAndRefOffset, acc);
					if (null != oldAcc) acc = oldAcc;
				}
				if (passesFilter) {
					acc.addBase(bases[i + offset], qualities[i + offset], forwardStrand, 
							startPosition, i + startPosAndRefOffset, readEndPosition, readId);
				} else {
					acc.addUnfilteredBase(bases[i + offset]);
				}
			}
		}
		
		@Override
		public void run() {
			logger.info("In Consumer run method with isControl: " + isControl);
			try {
				long count = 0;
				while (true) {
					
					final SAMRecordFilterWrapper rec = queue.poll();
					if (null != rec) {
						
						processSAMRecord(rec);
//						int alignmentStart = rec.getRecord().getAlignmentStart(); 
						minStartPosition.set(rec.getRecord().getAlignmentStart());
						
						if (++count % maxMapSize == 0) {
							int mapSize = map.size();
							if (mapSize > maxMapSize) {
								
//								stealWork();
								
								
								for (int i = 0 ; i < 100 ; i++) {
									final int sleepInterval = mapSize / 1000;
									if (sleepInterval > 1000) logger.info("sleeping for " + sleepInterval + ", map size: " + mapSize);
									try {
										Thread.sleep(sleepInterval);
									} catch (final InterruptedException e) {
										logger.error("InterruptedException caught in Consumer sleep",e);
										throw e;
									}
									if (map.size() < maxMapSize) break;
								}
								mapSize = map.size();
								if (mapSize > maxMapSize) {
									logger.warn("map size still over 100000 despite multiple sleeps: " + mapSize);
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
									assert map.isEmpty() : "Consumer: map was not empty following barrier reset";
									count = 0;
								} catch (final InterruptedException e) {
									logger.error("Consumer: InterruptedException caught with map size: " + map.size(), e);
									throw e;
								} catch (final BrokenBarrierException e) {
									logger.error("Consumer: BrokenBarrierException caught with map size: " + map.size(), e);
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

//		private void stealWork() {
//			// check that work steal map is not too full
//			logger.info("Stealing work...");
//			while ( map.size() > maxMapSize && workStealMap.size() < maxMapSize) {
//				final SAMRecordFilterWrapper rec = workStealQueue.poll();
//				if (null != rec) {
//					processSAMRecord(rec, true);
////					workStealMinStartPosition.set(rec.getRecord().getAlignmentStart());
//				}
//			}
//		}
	}
	
	
	public class Cleaner implements Runnable {
		private final CountDownLatch cleanerLatch;
		private final CountDownLatch consumerLatch;
		private final Thread mainThread;
		private int previousPosition = 0;
		private final CyclicBarrier barrier;
		private final AtomicInteger controlMinStart;
		private final AtomicInteger testMinStart;
		private final  ConcurrentMap<Integer, Accumulator> controlAccums;
		private final  ConcurrentMap<Integer, Accumulator> testAccums;
		private long processMapsCounter = 0;
		private final boolean debugLoggingEnabled;
		
		public Cleaner(CountDownLatch cleanerLatch, CountDownLatch consumerLatch, Thread mainThread, CyclicBarrier barrier,
				final AtomicInteger normalMinStart, final AtomicInteger tumourMinStart,
				final ConcurrentMap<Integer, Accumulator> cnormalAccs, final ConcurrentMap<Integer, Accumulator> ctumourAccs) {
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
			final int minStartPos = singleSampleMode ? testMinStart.intValue() -1 : Math.min(controlMinStart.intValue(), testMinStart.intValue()) - 1;
			if (debugLoggingEnabled && ++processMapsCounter % 10 == 0) {
				logger.debug("min start position: " + minStartPos + ", no of keepers so far: " + positionRecordMap.size());
			}
			if (minStartPos <= 0) return;
			final boolean useContainsKey = (minStartPos - previousPosition) > 100000; 
				
			for (int i = previousPosition ; i < minStartPos ; i++) {
				final Accumulator controlAcc = singleSampleMode ? null : useContainsKey ? (controlAccums.containsKey(i) ? controlAccums.remove(i) : null) : controlAccums.remove(i);
				final Accumulator testAcc = useContainsKey ? (testAccums.containsKey(i) ? testAccums.remove(i) : null) : testAccums.remove(i);
				if (null != testAcc && null != controlAcc) {
					processControlAndTest(controlAcc, testAcc);
				} else if (null != controlAcc){
					processControl(controlAcc);
				} else if (null != testAcc){
					processTest(testAcc);
				}
			}
				
			previousPosition = minStartPos;
		}
		
		private void processMapsAll() {
			if (null != referenceBases && ( ( ! singleSampleMode && ! controlAccums.isEmpty()) || ! testAccums.isEmpty())) {
			
				if ( ! singleSampleMode) {
					for (final Map.Entry<Integer, Accumulator> entry : controlAccums.entrySet()) {
						final Integer i = entry.getKey();
						final Accumulator normAcc = entry.getValue();
						if (i.intValue() >  referenceBasesLength) {
							logger.warn("Found position greater than reference array length: " + i.intValue() + " for chr: " + currentChr);
						} else {
							if (null == normAcc) {
								logger.info("null control acc for key: " + i);
							} else {
								final Accumulator tumAcc = testAccums.remove(i);
								if (null != tumAcc) {
									processControlAndTest(normAcc, tumAcc);
								} else {
									processControl(normAcc);
								}
							}
						}
					}
					controlAccums.clear();
				}
				
				// same for tumour
				for (final Map.Entry<Integer, Accumulator> entry : testAccums.entrySet()) {
					final Integer i = entry.getKey();
					final Accumulator tumAcc = entry.getValue();
					if (i.intValue() >  referenceBasesLength) {
						logger.warn("Found position greater than reference array length: " + i.intValue() + " for chr: " + currentChr);
					} else {
						if (null == tumAcc) {
							logger.info("null value for key: " + i);
						} else {
							processTest(tumAcc);
						}
					}
				}
				testAccums.clear();
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
					
					processMaps();
						
					if (barrier.getNumberWaiting() == barrier.getParties() - 1) {
						logger.info("Cleaner: about to hit barrier - running processMapsAll");
						
						processMapsAll();
						// lets try purging here...
						purgeNonAdjacentAccumulators();
						try {
							previousPosition = 0;
							barrier.await();
							logger.info("Cleaner: no of keepers so far: " + positionRecordMap.size());
							Thread.sleep(500);	// sleep to allow initial map population
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
					} else {
						
						try {
							Thread.sleep(5);	
						} catch (final InterruptedException e) {
							logger.error("InterruptedException caught in Cleaner sleep",e);
							throw e;
						}
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
		if (testAcc.containsMultipleAlleles() || 
				(testAcc.getPosition() -1 < referenceBasesLength 
						&& ! baseEqualsReference(testAcc.getBase(),testAcc.getPosition() -1))) {
			interrogateAccumulations(null, testAcc);
		}
	}
	private void processControl(Accumulator controlAcc) {
		if (controlAcc.containsMultipleAlleles() || 
				(controlAcc.getPosition() -1 < referenceBasesLength
						&& ! baseEqualsReference(controlAcc.getBase(), controlAcc.getPosition() -1))) {
			interrogateAccumulations(controlAcc, null);
		}
	}
	private void processControlAndTest(Accumulator controlAcc, Accumulator testAcc) {
		if (controlAcc.containsMultipleAlleles() || testAcc.containsMultipleAlleles() 
				|| (controlAcc.getBase() != testAcc.getBase())
				|| (testAcc.getPosition() -1 < referenceBasesLength 
						&& ! baseEqualsReference(testAcc.getBase(), testAcc.getPosition() -1))
				|| (controlAcc.getPosition() -1 < referenceBasesLength
						&& ! baseEqualsReference(controlAcc.getBase(), controlAcc.getPosition() -1))) {
			interrogateAccumulations(controlAcc, testAcc);
		}
	}
	
	private boolean baseEqualsReference(char base, int position) {
		final char refBase = (char) referenceBases[position];
		if (base == refBase) return true;
		if (Character.isLowerCase(refBase)) {
			final char upperCaseRef = Character.toUpperCase(refBase);
			return base == upperCaseRef;
		} else return false;
	}
	
	// strand bias check
	void checkForStrandBias(QSnpRecord rec, Accumulator normal, Accumulator tumour, char ref ) {
		if (runSBIASAnnotation) {
			final PileupElementLite pel = Classification.GERMLINE != rec.getClassification() ? (null != tumour? tumour.getLargestVariant(ref) : null) : 
				(null != normal ? normal.getLargestVariant(ref) : null);
			
			if (null != pel && ! PileupElementLiteUtil.areBothStrandsRepresented(pel, sBiasAltPercentage)) {
				VcfUtils.updateFilter(rec.getVcfRecord(), SnpUtils.STRAND_BIAS_ALT);
			}
//			if (null != pel && ! pel.isFoundOnBothStrands()) {
//				VcfUtils.updateFilter(rec.getVcfRecord(), SnpUtils.STRAND_BIAS);
//			}
		}
	}
	
	// ends of reads check
	void checkForEndsOfReads(QSnpRecord rec, Accumulator normal, Accumulator tumour, char ref ) {
		
		final PileupElementLite pel = Classification.GERMLINE != rec.getClassification() ? (null != tumour? tumour.getLargestVariant(ref) : null) : 
			(null != normal ? normal.getLargestVariant(ref) : null);
		
		if (null != pel && pel.getEndOfReadCount() > 0) {
			
			if (pel.getMiddleOfReadCount() >= 5 && pel.isFoundOnBothStrandsMiddleOfRead()) {
				// all good
			} else {
				VcfUtils.updateFilter(rec.getVcfRecord(), SnpUtils.END_OF_READ + pel.getEndOfReadCount());
			}
		}
	}
	
//	void checkForMutationInNormal() {
//		int minCount = 0;
//		for (final QSnpRecord record : positionRecordMap.values()) {
//			if (null != record.getAnnotation() && record.getAnnotation().contains(SnpUtils.MUTATION_IN_NORMAL)) {
//				// check to see if mutant count in normal is 3% or more
//				// if not, remove annotation
//				final String ND = record.getNormalNucleotides();
//				final int normalCount = record.getNormalCount();
//				final String alt = SnpUtils.getAltFromMutationString(record.getMutation());
//				final int altCount = SnpUtils.getCountFromNucleotideString(ND, alt);
//				
//				if (((float)altCount / normalCount) * 100 < 3.0f) {
//					VcfUtils.removeFilter(record.getVcfRecord(), SnpUtils.MUTATION_IN_NORMAL);
//					minCount++;
//				}
//			}
//		}
//		logger.info("no of records with " + SnpUtils.MUTATION_IN_NORMAL + " annotation removed: " + minCount);
//	}
	
	private void interrogateAccumulations(final Accumulator normal, final Accumulator tumour) {
		
		// get coverage for both normal and tumour
		final int normalCoverage = null != normal ? normal.getCoverage() : 0;
		final int tumourCoverage = null != tumour ? tumour.getCoverage() : 0;
		
		if (normalCoverage + tumourCoverage < initialTestSumOfCountsLimit) return;
		
		final int position = normal != null ? normal.getPosition() : tumour.getPosition();
		
		// if we are over the length of this particular sequence - return
		if (position-1 >= referenceBasesLength) return;
		
		char ref = (char) referenceBases[position-1];
		if (Character.isLowerCase(ref)) ref = Character.toUpperCase(ref);
		
		// get rule for normal and tumour
		final Rule normalRule = RulesUtil.getRule(controlRules, normalCoverage);
		final Rule tumourRule = RulesUtil.getRule(testRules, tumourCoverage);
		
		
		final boolean [] normalPass = PileupElementLiteUtil.isAccumulatorAKeeper(normal,  ref, normalRule, baseQualityPercentage);
		final boolean [] tumourPass = PileupElementLiteUtil.isAccumulatorAKeeper(tumour,  ref, tumourRule, baseQualityPercentage);
		
		
		if (normalPass[0] || normalPass[1] || tumourPass[0] || tumourPass[1]) {
		
			final String normalBases = null != normal ? normal.toSamtoolsPileupString(ref) : "";
			final String tumourBases = null != tumour ? tumour.toSamtoolsPileupString(ref) : "";
			final QSnpRecord qRecord = new QSnpRecord(currentChr, position, ref+"");
			qRecord.setPileup((null != normal ? normal.toPileupString(normalBases) : "") 
					+ "\t" + (null != tumour ? tumour.toPileupString(tumourBases) : ""));
			// setup some values on the record
//			qRecord.setRef(ref);
			qRecord.setNormalCount(normalCoverage);
			qRecord.setTumourCount(tumourCoverage);
			
			// set normal pileup to only contain the different bases found in normal, rather than the whole pileup string
			// which contains special chars indicating start/end of reads along with mapping qualities
			if (normalCoverage > 0)
				qRecord.setNormalPileup(normal.getCompressedPileup());
			// use all base counts to form genotype
			qRecord.setNormalGenotype(null != normal ? normal.getGenotype(ref, normalRule, normalPass[1], baseQualityPercentage) : null);
			qRecord.setTumourGenotype(null != tumour ? tumour.getGenotype(ref, tumourRule, tumourPass[1], baseQualityPercentage) : null);
			
			qRecord.setNormalNucleotides(null != normal ? normal.getPileupElementString() : null);
			qRecord.setTumourNucleotides(null != tumour ? tumour.getPileupElementString() : null);
			// add unfiltered normal
			if (null != normal)
				qRecord.setUnfilteredNormalPileup(normal.getUnfilteredPileup());
			
			
			qRecord.setNormalNovelStartCount(PileupElementLiteUtil.getLargestVariantNovelStarts(normal, ref));
			qRecord.setTumourNovelStartCount(PileupElementLiteUtil.getLargestVariantNovelStarts(tumour, ref));
			
			classifyPileupRecord(qRecord);
			
			if (null != qRecord.getMutation()) {
				final String altString =SnpUtils.getAltFromMutationString(qRecord.getMutation());
				if (altString.length() > 1) {
					logger.warn("altString: " + altString + " in Pipeline.interrogateAccumulations");
				}
				final char alt = altString.charAt(0);
				
				// get and set cpg
				final char[] cpgCharArray = new char[11];
				for (int i = 0 ; i < 11 ; i++) {
					final int refPosition = position - (6 - i);
					if (i == 5) {
						cpgCharArray[i] = alt;
					} else if ( refPosition >= 0 && refPosition < referenceBasesLength) {
						cpgCharArray[i] = Character.toUpperCase((char) referenceBases[refPosition]);
					} else {
						cpgCharArray[i] = '-';
					}
				}
				qRecord.setFlankingSequence(String.valueOf(cpgCharArray));
				
				// only do fisher for SOMATICS
				if (Classification.SOMATIC == qRecord.getClassification()) {
					// fisher test work here - we need an alt to do this
					final int normalAlt = null != normal ? normal.getBaseCountForBase(alt) : 0;		// a
					final int normalRef = null != normal ? normal.getBaseCountForBase(ref) : 0;	// c
					final int tumourAlt = null != tumour ? tumour.getBaseCountForBase(alt) : 0;		// b
					final int tumourRef = null != tumour ? tumour.getBaseCountForBase(ref) : 0;	// d
					
					
					// don't run this if we have crazy large coverage - takes too long and holds up the other threads
					if (normalCoverage + tumourCoverage > 100000) {
						logger.info("skipping FisherExact pValue calculation - coverage too large: " + normalCoverage + ", tCov: " + tumourCoverage + ", at " + currentChr + ":" + position);
					} else {
						final double pValue = FisherExact.getTwoTailedFETMath(normalAlt, tumourAlt, normalRef, tumourRef);
	//					if (normalAlt > 1000 || normalRef > 1000 || tumourAlt > 1000 || tumourRef > 1000) {
	//						logger.info("pValue: " + pValue );
	//					}
		//				logger.info("pvalue for following values (a,b,c,d:pvalue): " + normalAlt + "," + tumourAlt + "," + normalRef + "," + tumourRef + ": " + pValue);
						qRecord.setProbability(pValue);
						if (++pValueCount % 1000 == 0) logger.info("hit " + pValueCount + " pValue calculations");
					}
				}
				
				// strand bias check
				checkForStrandBias(qRecord, normal, tumour, ref);
				
				// ends of read check
				checkForEndsOfReads(qRecord, normal, tumour, ref);
			}
			
			// set Id
			qRecord.setId(++mutationId);
			logger.debug("adding: " + qRecord.getDCCDataNSFlankingSeq(null, null));
			positionRecordMap.put(qRecord.getChrPos(), qRecord);
			
			// to save on space, only put entry into acculumators map if it is empty, or if there is a position just before the current position
			
			adjacentAccumulators.put(qRecord.getChrPos(), new Pair<Accumulator, Accumulator>(normal, tumour));
//			if (mutationId % 1000 == 0) {
//				purgeNonAdjacentAccumulators();
//			}
		}
	}
	
	/**
	 * Need 3 ChrPos objects to be able to make a decision as to whether the middle one is adjacent to either of its neighbouring objects
	 */
	void purgeNonAdjacentAccumulators() {
		int noRemoved = 0;
		List<ChrPosition> list = new ArrayList<>(adjacentAccumulators.keySet());
		Collections.sort(list);

		ChrPosition left = null;
		ChrPosition middle = null;
		
		for (ChrPosition right : list) {
			if (null != left && null != middle) {
				if ( ! ChrPositionUtils.areAdjacent(left, middle)
						&& ! ChrPositionUtils.areAdjacent(middle, right)) {
					// remove middle from adjacentAccums
//					if ( (middle.getPosition() == 118124 || middle.getPosition() == 118125)
//							&& middle.getChromosome().startsWith("GL000216") ) {
//						logger.warn("removing middle : " + middle + ", left: " + left + ", right: " + right);
//					}
					adjacentAccumulators.remove(middle);
					noRemoved++;
				}
			}
			left = middle;
			middle = right;
		}
		logger.debug("removed " +noRemoved + " from adjacentAccumulators");
	}
	
	
	void compoundSnps() {
		logger.info("in compound snp");
		// loop through our snps
		// if we have adjacent ones, need to do some digging to see if they might be compoundium snps
		
		final int size = positionRecordMap.size();
		
		final List<ChrPosition> keys = new ArrayList<>(positionRecordMap.keySet());
		Collections.sort(keys);
		
		 int noOfCompSnpsSOM = 0;
		 int noOfCompSnpsGERM = 0;
		 int noOfCompSnpsMIXED = 0;
		 int noOfCompSnpsLowAltCount = 0;
		 int [] compSnpSize = new int[10000];
		Arrays.fill(compSnpSize, 0);
 		
		for (int i = 0 ; i < size-1 ; ) {
			
			ChrPosition thisPosition = keys.get(i++);
			ChrPosition nextPosition = keys.get(i);
			
			if (ChrPositionUtils.areAdjacent(thisPosition, nextPosition)) {
				
				// check to see if they have the same classification
				QSnpRecord qsnpr1 = positionRecordMap.get(thisPosition);
				QSnpRecord qsnpr2 = positionRecordMap.get(nextPosition);
				
				if (qsnpr1.getClassification() != qsnpr2.getClassification() ) {
					continue;
				}
				
				final ChrPosition start = thisPosition;		// starting point of compound snp
				final int startPosition= start.getPosition();
				
				while (i < size-1) {
					thisPosition = nextPosition;
					nextPosition = keys.get(++i);		// end point of compound snp
					if ( ! ChrPositionUtils.areAdjacent(thisPosition, nextPosition)) {
						break;
					}
					// check to see if they have the same classification
					qsnpr1 = positionRecordMap.get(thisPosition);
					qsnpr2 = positionRecordMap.get(nextPosition);
					
					if (qsnpr1.getClassification() != qsnpr2.getClassification() ) {
						break;
					}
				}
				
				if (start.equals(thisPosition)) {
					// set thisPosition to be nextPosition
					thisPosition = nextPosition;
				}
				
				// thisPosition should now be the end of the compound snp
				
				final int endPosition = thisPosition.getPosition();
				// how many bases does our compound snp cover?
				final int noOfBases = (endPosition - startPosition) + 1;
				
				// create new ChrPosition object that spans the region we are interested and create a list with the QSnpRecords
				
				final ChrPosition csChrPos = new ChrPosition(start.getChromosome(), startPosition, endPosition);
				final List<QSnpRecord> csRecords = new ArrayList<>();
				String ref = "", alt = "", classification = "", flag = "";
				for (int k = 0 ; k < csChrPos.getLength() ; k++) {
					final QSnpRecord rec = positionRecordMap.get(new ChrPosition(start.getChromosome(), startPosition + k));
					csRecords.add(rec);
					ref += rec.getRef();
//					if (alt.length() > 0) {
//						alt += ",";
//					}
					if (null != rec.getMutation()) {
						alt += rec.getMutation().charAt(rec.getMutation().length() -1);
					}
					if (classification.length() > 0) {
						classification += ",";
					}
					classification += rec.getClassification();
					if (flag.length() > 0) {
						flag += Constants.SEMI_COLON;
					}
					flag += rec.getAnnotation();
				}
				
				// get unique list of flags
				final String [] flagArray = flag.split(Constants.SEMI_COLON_STRING);
				final Set<String> uniqueFlags = new HashSet<>(Arrays.asList(flagArray));
				
				final StringBuilder uniqueFlagsSb = new StringBuilder();
				for (final String s : uniqueFlags) {
					if (uniqueFlagsSb.length() > 0) {
						uniqueFlagsSb.append(Constants.SEMI_COLON);
					}
					uniqueFlagsSb.append(s);
				}
				
				
				final Map<Long, StringBuilder> controlReadSeqMap = new HashMap<>();
				final Map<Long, StringBuilder> testReadSeqMap = new HashMap<>();
				
				// get accumulation objects for this position
				for (int j = startPosition ; j <= endPosition ; j++) {
					final ChrPosition cp = new ChrPosition(csChrPos.getChromosome(), j);
					final Pair<Accumulator, Accumulator> accums = adjacentAccumulators.remove(cp);
//					final Pair<Accumulator, Accumulator> accums = adjacentAccumulators.get(cp);
					
					if (null == accums) {
						logger.warn("null accums in adjacentAccumulators for cp: " + cp);
						logger.warn("chChrPos: " + csChrPos);
					}
					
					final Accumulator control = accums.getLeft();
					final Accumulator test = accums.getRight();
					if (null != control) {
						accumulateReadBases(control, controlReadSeqMap, (j - startPosition));
					}
					if (null != test) {
						accumulateReadBases(test, testReadSeqMap, (j - startPosition));
					}
				}
				
				final Map<String, AtomicInteger> controlMutationCount = new HashMap<>();
				for (final Entry<Long, StringBuilder> entry : controlReadSeqMap.entrySet()) {
					final AtomicInteger count = controlMutationCount.get(entry.getValue().toString());
					if (null == count) {
						controlMutationCount.put(entry.getValue().toString(), new AtomicInteger(1));
					} else {
						count.incrementAndGet();
					}
				}
				
				final Map<String, AtomicInteger> testMutationCount = new HashMap<>();
				for (final Entry<Long, StringBuilder> entry : testReadSeqMap.entrySet()) {
					final AtomicInteger count = testMutationCount.get(entry.getValue().toString());
					if (null == count) {
						testMutationCount.put(entry.getValue().toString(), new AtomicInteger(1));
					} else {
						count.incrementAndGet();
					}
				}
				
				final AtomicInteger controlAltCountFS = controlMutationCount.get(alt);
				final AtomicInteger testAltCountFS = testMutationCount.get(alt);
				final AtomicInteger controlAltCountRS = controlMutationCount.get(alt.toLowerCase());
				final AtomicInteger testAltCountRS = testMutationCount.get(alt.toLowerCase());
				final int nc = (null != controlAltCountFS ? controlAltCountFS.get() : 0) + (null != controlAltCountRS ? controlAltCountRS.get() : 0) ;
				final int tc = (null != testAltCountFS ? testAltCountFS.get() : 0) + (null != testAltCountRS ? testAltCountRS.get() : 0);
				
				final int totalAltCount = nc + tc;
				final boolean somatic = classification.contains(Classification.SOMATIC.toString());
				final boolean germline = classification.contains(Classification.GERMLINE.toString());
				// don't care about UNKNOWNs at the moment...
				
				if (totalAltCount >= 4 && ! (somatic && germline)) {
//					logger.info("will create CS, totalAltCount: " + totalAltCount + " classification: " + classification + " : " + csChrPos.toString());
					
					
					final StringBuilder controlSb = new StringBuilder();
					List<String> bases = new ArrayList<>(controlMutationCount.keySet());
					Collections.sort(bases);
					
					for (final String key : bases) {
						final String upperCaseKey = key.toUpperCase();
						if (controlSb.indexOf(upperCaseKey) == -1) {
							if (controlSb.length() > 0) {
								controlSb.append(",");
							}
							final AtomicInteger fsAI = controlMutationCount.get(upperCaseKey);
							final AtomicInteger rsAI = controlMutationCount.get(upperCaseKey.toLowerCase());
							final int fs = null != fsAI ? fsAI.get() : 0;
							final int rs = null != rsAI ? rsAI.get() : 0;
							controlSb.append(upperCaseKey + "," + fs + "," + rs);
						}
					}
					final StringBuilder testSb = new StringBuilder();
					bases = new ArrayList<>(testMutationCount.keySet());
					Collections.sort(bases);
					for (final String key : bases) {
						final String upperCaseKey = key.toUpperCase();
						if (testSb.indexOf(upperCaseKey) == -1) {
							if (testSb.length() > 0) {
								testSb.append(",");
							}
							final AtomicInteger fsAI = testMutationCount.get(upperCaseKey);
							final AtomicInteger rsAI = testMutationCount.get(upperCaseKey.toLowerCase());
							final int fs = null != fsAI ? fsAI.get() : 0;
							final int rs = null != rsAI ? rsAI.get() : 0;
							testSb.append(upperCaseKey + "," + fs + "," + rs);
						}
					}
					
//					qlogger.info(csChrPos.toString() + " - ref bases: " + ref + ", alt: " + alt + " : " + classification + ", flag: " + flag + "\nnormal: " + normalSb.toString() + "\ntumour: " + tumourSb.toString());
					
					// create new VCFRecord with start position
					final VcfRecord cs = VcfUtils.createVcfRecord(csChrPos, null, ref, alt);
					if (somatic) {
						cs.appendInfo(Classification.SOMATIC.toString());
						noOfCompSnpsSOM++;
					} else {
						noOfCompSnpsGERM++;
					}
					cs.setFilter(uniqueFlagsSb.toString());		// unique list of filters seen by snps making up this cs
					if (singleSampleMode) {
						cs.setFormatFields(Arrays.asList(VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP,
										StringUtils.isNullOrEmpty(testSb.toString()) ? Constants.MISSING_DATA_STRING : testSb.toString()));
					} else {
						cs.setFormatFields(Arrays.asList(VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP,
							 StringUtils.isNullOrEmpty(controlSb.toString()) ? Constants.MISSING_DATA_STRING : controlSb.toString(), 
									 StringUtils.isNullOrEmpty(testSb.toString()) ? Constants.MISSING_DATA_STRING : testSb.toString()));
					}
					//cs.setFormatField(Arrays.asList(VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP,
					//	 normalSb.toString(), tumourSb.toString())); 
					
					compoundSnps.put(csChrPos, cs);
					
					// remove old records from map, and add new one
					for (final QSnpRecord rec : csRecords) {
						positionRecordMap.remove(rec.getChrPos());
					}
					compSnpSize[noOfBases] ++;
				} else {
					if (somatic && germline) {
						noOfCompSnpsMIXED++;
					} else {
						noOfCompSnpsLowAltCount++;
					}
//					logger.info("won't create CS, totalAltCount: " + totalAltCount + " classification: " + classification + " : " + csChrPos.toString());
				}
			}
		}
		
		logger.info("found " + (noOfCompSnpsSOM +noOfCompSnpsGERM + noOfCompSnpsMIXED)  + " compound snps - no in map: " + compoundSnps.size());
		logger.info("noOfCompSnpsSOM: " + noOfCompSnpsSOM + ", noOfCompSnpsGERM: " + noOfCompSnpsGERM +", noOfCompSnpsMIXED: " + noOfCompSnpsMIXED + ", noOfCompSnpsLowAltCount: " + noOfCompSnpsLowAltCount);
		
		for (int i = 0 ; i < compSnpSize.length ; i++) {
			if (compSnpSize[i] > 0) {
				logger.info("no of compound snps of length: " + i + " : " + compSnpSize[i]);
			}
		}
	}
	
	void accumulateReadBases(Accumulator acc, Map<Long, StringBuilder> readSeqMap, int position) {
		final TIntCharMap map = acc.getReadIdBaseMap();
		// get existing seq for each id
		int[] keys = map.keys();
		
		for (int l : keys) {
			char c = map.get(l);
			Long longObject = Long.valueOf(l);
			StringBuilder seq = readSeqMap.get(longObject);
			if (null == seq) {
				// initialise based on how far away we are from the start
				seq = new StringBuilder();
				for (int q = (position) ; q > 0 ; q--) {
					seq.append("_");
				}
				readSeqMap.put(longObject, seq);
			}
			seq.append(c);
		}
		// need to check that all elements have enough data in them
		for (final StringBuilder sb : readSeqMap.values()) {
			if (sb.length() <= position) {
				sb.append("_");
			}
		}
	}
//	void accumulateReadBases(Accumulator acc, Map<Long, StringBuilder> readSeqMap, int position) {
//		final Map<Long, Character> map = acc.getReadIdBaseMap();
//		// get existing seq for each id
//		for (final Entry<Long, Character> entry : map.entrySet()) {
//			StringBuilder seq = readSeqMap.get(entry.getKey());
//			if (null == seq) {
//				// initialise based on how far away we are from the start
//				seq = new StringBuilder();
//				for (int q = (position) ; q > 0 ; q--) {
//					seq.append("_");
//				}
//				readSeqMap.put(entry.getKey(), seq);
//			}
//			seq.append(entry.getValue());
//		}
//		
//		// need to check that all elements have enough data in them
//		for (final StringBuilder sb : readSeqMap.values()) {
//			if (sb.length() <= position) {
//				sb.append("_");
//			}
//		}
//	}

	protected void strandBiasCorrection() {
			// remove SBIAS flag should there be no reads at all on the opposite strand
			int noOfSnpsWithSBIAS = 0, removed = 0;
			for (final QSnpRecord record : positionRecordMap.values()) {
				if (record.getAnnotation().contains(SnpUtils.STRAND_BIAS_ALT)) {
					noOfSnpsWithSBIAS++;
					
					// check to see if we have any reads at all on the opposite strand
					// just checking the germline for the moment as we are currently in single file mode
					final String ND = singleSampleMode ? record.getTumourNucleotides() : record.getNormalNucleotides();
	//				logger.info("sending ND to check for strand bias: " + ND);
					final boolean onBothStrands = SnpUtils.doesNucleotideStringContainReadsOnBothStrands(ND, sBiasCovPercentage);
					if ( ! onBothStrands) {
	//					logger.info("removing SBIAS annotation for ND: " + ND);
						VcfUtils.removeFilter(record.getVcfRecord(), SnpUtils.STRAND_BIAS_ALT);
						VcfUtils.updateFilter(record.getVcfRecord(), SnpUtils.STRAND_BIAS_COVERAGE);
						removed++;
					}
				}
			}
			logger.info("no of snps with SBIAS: " + noOfSnpsWithSBIAS + ", and no removed: " + removed);
			
		}
	
}
