/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */

package org.qcmg.snp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.qcmg.chrconv.ChrConvFileReader;
import org.qcmg.chrconv.ChromosomeConversionRecord;
import org.qcmg.common.log.QLevel;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QDccMeta;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.meta.QLimsMeta;
import org.qcmg.common.model.Accumulator;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.model.PileupElementLite;
import org.qcmg.common.model.Rule;
import org.qcmg.common.model.VCFRecord;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.BaseUtils;
import org.qcmg.common.util.PileupElementLiteUtil;
import org.qcmg.common.util.PileupUtils;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfHeaderUtils;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.illumina.IlluminaFileReader;
import org.qcmg.illumina.IlluminaRecord;
import org.qcmg.maths.FisherExact;
import org.qcmg.picard.MultiSAMFileIterator;
import org.qcmg.picard.MultiSAMFileReader;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMRecordFilterWrapper;
import org.qcmg.picard.util.PileupElementUtil;
import org.qcmg.picard.util.QDccMetaFactory;
import org.qcmg.picard.util.QLimsMetaFactory;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.pileup.QSnpRecord;
import org.qcmg.pileup.QSnpRecord.Classification;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.record.Record;
import org.qcmg.snp.util.GenotypeComparisonUtil;
import org.qcmg.snp.util.IniFileUtil;
import org.qcmg.snp.util.RulesUtil;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;

public abstract class Pipeline {
	
	static final String ILLUMINA_MOTIF = "GGT";
		
	static final char COLON = ':';
	static final char SEMI_COLON = ';';
	static final char TAB = '\t';
	static final char EQ = '=';
	
	//TODO these will need to become user defined values at some point
	int novelStartsFilterValue = 4;
	int mutantReadsFilterValue = 5;
	
	static int initialTestSumOfCountsLimit = 3;
//	static int minimumBaseQualityScore = 10;
	static int baseQualityPercentage = 10;
	
	
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
	// map to hold chromosome conversion data
	final Map<String, String> ensembleToQCMG = new HashMap<String, String>(128);
	
	// ChrPosition chromosome consists of "chr" and then the number/letter
	final Map<ChrPosition,QSnpRecord> positionRecordMap = new HashMap<ChrPosition,QSnpRecord>(100000);
	final Map<ChrPosition, IlluminaRecord> normalIlluminaMap = new HashMap<ChrPosition, IlluminaRecord>(1250000);	// not expecting more than 1000000
	final Map<ChrPosition, IlluminaRecord> tumourIlluminaMap = new HashMap<ChrPosition, IlluminaRecord>(1250000);	// not expecting more than 1000000
	
	// will contain a zero based array of positions in the pileup string that correspond to normal/tumour data
	// used by pileup based pipelines
	int[] normalStartPositions;
	int[] tumourStartPositions;
	int noOfNormalFiles;
	int noOfTumourFiles;
	boolean includeIndels;
	int mutationId;
	
	boolean vcfOnlyMode;
	
	List<Rule> normalRules = new ArrayList<Rule>();
	List<Rule> tumourRules = new ArrayList<Rule>();
	
	String validation;
	String skipAnnotation;
	boolean runSBIASAnnotation = true;
	
	String referenceFile;
	String query;
	
	FastaSequenceFile sequenceFile;
	byte[] referenceBases;
	int referenceBasesLength;
	
	long noOfRecordsFailingFilter = 1000000;
	String currentChr = "chr1";

	
	//TODO when re-implementing this part, set the capacity to 110000
//	private final Map<String, GeneSymbolRecord> geneSymbolMap = new HashMap<String, GeneSymbolRecord>();
//	private final Map<String, GeneSymbolRecord> geneSymbolMap = new HashMap<String, GeneSymbolRecord>(110000);
	
	////////
	// ids
	///////
//	protected String somaticAnalysisId;
//	protected String germlineAnalysisId;
	protected String normalSampleId;
	protected String tumourSampleId;
	protected String mutationIdPrefix;
	protected String patientId;
	
	////////////////////
	// output files
	////////////////////
	protected String dccSomaticFile;
	protected String dccGermlineFile;
	protected String vcfFile;
	
	//////////////////
	// input files
	//////////////////
	protected String dbSnpFile;
	protected String illuminaNormalFile;
	protected String illuminaTumourFile;
	protected String germlineDBFile;
	protected String chrConvFile;
//	protected String normalBam;
//	protected String tumourBam;
	protected String [] normalBams;
	protected String [] tumourBams;
	
	protected  QLogger logger;
	protected QExec qexec;
	
	/**
	 * default constructor that sets up a logger for the subclass instance 
	 */
	public Pipeline (QExec qexec) {
		logger = QLoggerFactory.getLogger(getClass());
		this.qexec = qexec;
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
//		somaticAnalysisId = IniFileUtil.getEntry(ini, "ids", "somaticAnalysis");
//		germlineAnalysisId = IniFileUtil.getEntry(ini, "ids", "germlineAnalysis");
		normalSampleId = IniFileUtil.getEntry(ini, "ids", "normalSample");
		tumourSampleId = IniFileUtil.getEntry(ini, "ids", "tumourSample");
		
		// INPUT FILES
		dbSnpFile = IniFileUtil.getInputFile(ini, "dbSNP");
		germlineDBFile = IniFileUtil.getInputFile(ini, "germlineDB");
		illuminaNormalFile = IniFileUtil.getInputFile(ini, "illuminaNormal");
		illuminaTumourFile = IniFileUtil.getInputFile(ini, "illuminaTumour");
		chrConvFile = IniFileUtil.getInputFile(ini, "chrConv");
		referenceFile = IniFileUtil.getInputFile(ini, "ref");
		normalBams = IniFileUtil.getInputFiles(ini, "normalBam");
		tumourBams = IniFileUtil.getInputFiles(ini, "tumourBam");
		
		
		// OUTPUT FILES
		dccSomaticFile = IniFileUtil.getOutputFile(ini, "dccSomatic");
		dccGermlineFile = IniFileUtil.getOutputFile(ini, "dccGermline");
		vcfFile = IniFileUtil.getOutputFile(ini, "vcf");
		
		// QBAMFILTER QUERY
		query =  IniFileUtil.getEntry(ini, "parameters", "filter");
		String sFailFilter = IniFileUtil.getEntry(ini, "parameters", "noOfRecordsFailingFilter");
		if (null != sFailFilter)
			noOfRecordsFailingFilter = Long.parseLong(sFailFilter);
		
		// ADDITIONAL SETUP	
		mutationIdPrefix = qexec.getUuid().getValue() + "_SNP_";
//		mutationIdPrefix = patientId + "_SNP_";
		
		// VCF ONLY MODE
		String vcfModeString = IniFileUtil.getEntry(ini, "parameters", "annotateMode"); 
		vcfOnlyMode = (null == vcfModeString || "vcf".equalsIgnoreCase(vcfModeString));
		
		String novelStartsFilterValueString = IniFileUtil.getEntry(ini, "parameters", "numberNovelStarts");
		// default to 4 if not specified
		if ( ! StringUtils.isNullOrEmpty(novelStartsFilterValueString)) {
			novelStartsFilterValue = Integer.parseInt(novelStartsFilterValueString);
		}
		
		String mutantReadsFilterValueString = IniFileUtil.getEntry(ini, "parameters", "numberMutantReads");
		// default to 5 if not specified
		if ( ! StringUtils.isNullOrEmpty(mutantReadsFilterValueString)) {
			mutantReadsFilterValue = Integer.parseInt(mutantReadsFilterValueString);
		}
		
		// validation
		String validationString = IniFileUtil.getEntry(ini, "parameters", "validation");
		if ( ! StringUtils.isNullOrEmpty(validationString)) {
			validation = validationString;
		}
		
		// validation
		String skipAnnotationString = IniFileUtil.getEntry(ini, "parameters", "skipAnnotation");
		if ( ! StringUtils.isNullOrEmpty(skipAnnotationString)) {
			skipAnnotation = skipAnnotationString;
			if (skipAnnotation.contains(SnpUtils.STRAND_BIAS)) 
				runSBIASAnnotation = false; 
		}
		
		// LOG
		logger.tool("**** IDS ****");
		logger.tool("patient ID: " + patientId);
		logger.tool("analysisId: " + qexec.getUuid().getValue());
//		logger.tool("somaticAnalysisId: " + somaticAnalysisId);
//		logger.tool("germlineAnalysisId: " + germlineAnalysisId);
		logger.tool("normalSampleId: " + normalSampleId);
		logger.tool("tumourSampleId: " + tumourSampleId);
		
		logger.tool("**** INPUT FILES ****");
		logger.tool("dbSnpFile: " + dbSnpFile);
		logger.tool("illuminaNormalFile: " + illuminaNormalFile);
		logger.tool("illuminaTumourFile: " + illuminaTumourFile);
		logger.tool("germlineDBFile: " + germlineDBFile);
		logger.tool("chrConvFile: " + chrConvFile);
		if (null != normalBams) {
			logger.tool("normalBam: " + Arrays.deepToString(normalBams));
		}
		if (null != tumourBams) {
			logger.tool("tumourBam: " + Arrays.deepToString(tumourBams));
		}
		if (null != referenceFile) {
			logger.tool("referenceFile: " + referenceFile);
		}
		
		logger.tool("**** OUTPUT FILES ****");
		logger.tool("vcf: " + vcfFile);
		logger.tool("dccSomaticFile: " + dccSomaticFile);
		logger.tool("dccGermlineFile: " + dccGermlineFile);
		
		logger.tool("**** CONFIG ****");
		logger.tool("vcfOnlyMode: " + vcfOnlyMode);
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
		
	}
	
	final void writeOutputForDCC() throws Exception {
		if (StringUtils.isNullOrEmpty(dccSomaticFile) || StringUtils.isNullOrEmpty(dccGermlineFile)) {
			logger.warn("No dcc output files were specified - can't write dcc output");
			return;
		}
		logger.info("Writing DCC output");
		FileWriter somaticWriter = new FileWriter(new File(dccSomaticFile));
		FileWriter germlineWriter = new FileWriter(new File(dccGermlineFile));
			
		List<ChrPosition> orderedList = new ArrayList<ChrPosition>(positionRecordMap.keySet());
		Collections.sort(orderedList);
		
		String analysisId = qexec.getUuid().getValue();
		String dccMetaData = getDccMetaData();
		
		String controlLimsMeta = null != normalBams && normalBams.length > 0 ? getLimsMetaData("control", normalBams[0]) : "";
		String testLimsMeta = null != tumourBams && tumourBams.length > 0 ? getLimsMetaData("test", tumourBams[0]) : "";
		
		try {
			
			// exec stuff
			somaticWriter.write(qexec.getExecMetaDataToString());
			germlineWriter.write(qexec.getExecMetaDataToString());
			// dcc meta stuff
			somaticWriter.write(null != dccMetaData ? dccMetaData : "");
			germlineWriter.write(null != dccMetaData ? dccMetaData : "");
			// lims meta stuff
			somaticWriter.write(controlLimsMeta);
			germlineWriter.write(controlLimsMeta);
			somaticWriter.write(testLimsMeta);
			germlineWriter.write(testLimsMeta);
			
			somaticWriter.write(getOutputHeader(true));
			germlineWriter.write(getOutputHeader(false));
			
			for (ChrPosition position : orderedList) {
					
				QSnpRecord record = positionRecordMap.get(position);
				String ensemblChr = record.getChromosome();
				for (Map.Entry<String, String> entry : ensembleToQCMG.entrySet()) {
					if (record.getChromosome().equals(entry.getValue())) {
						ensemblChr = entry.getKey();
						break;
					}
				}
				
				// default this to the record's chromosome - if an entry exists in the map, this will be overwritten
				// get ensembl chromosome
				
				// only write output if its been classified
				if (Classification.SOMATIC == record.getClassification())
					somaticWriter.write(analysisId + "\t" + tumourSampleId + "\t" 
							 + getFormattedRecord(record, ensemblChr) +"\n");
				else if (Classification.GERMLINE == record.getClassification())
					germlineWriter.write(analysisId + "\t" + normalSampleId + "\t" 
						 + getFormattedRecord(record, ensemblChr) +"\t" + record.getMutation() + "\n");
			}
		} finally {
			try {
				somaticWriter.close();
			} finally {
				germlineWriter.close();
			}
		}
	}
	
	final void writeOutputForDCCQ() throws Exception {
		if (StringUtils.isNullOrEmpty(dccSomaticFile) || StringUtils.isNullOrEmpty(dccGermlineFile)) {
			logger.warn("No dcc output files were specified - can't write dcc output");
			return;
		}
		logger.info("Writing DCCQ output");
		
		String dccqSomaticFile = dccSomaticFile.substring(0, dccSomaticFile.length() - 1) + "q";
		String dccqGermlineFile = dccGermlineFile.substring(0, dccGermlineFile.length() - 1) + "q";
		
		List<ChrPosition> orderedList = new ArrayList<ChrPosition>(positionRecordMap.keySet());
		Collections.sort(orderedList);
		
		String analysisId = qexec.getUuid().getValue();
		String dccMetaData = getDccMetaData();
		
		String controlLimsMeta = null != normalBams && normalBams.length > 0 ? getLimsMetaData("control", normalBams[0]) : "";
		String testLimsMeta = null != tumourBams && tumourBams.length > 0 ? getLimsMetaData("test", tumourBams[0]) : "";
		
//		FileWriter somaticWriter = new FileWriter(new File(dccqSomaticFile));
//		FileWriter germlineWriter = new FileWriter(new File(dccqGermlineFile));
		
		try (FileWriter somaticWriter = new FileWriter(new File(dccqSomaticFile));
				FileWriter germlineWriter = new FileWriter(new File(dccqGermlineFile));){
			
			// exec stuff
			somaticWriter.write(qexec.getExecMetaDataToString());
			germlineWriter.write(qexec.getExecMetaDataToString());
			// dcc meta stuff
			somaticWriter.write(null != dccMetaData ? dccMetaData : "");
			germlineWriter.write(null != dccMetaData ? dccMetaData : "");
			// lims meta stuff
			somaticWriter.write(controlLimsMeta);
			germlineWriter.write(controlLimsMeta);
			somaticWriter.write(testLimsMeta);
			germlineWriter.write(testLimsMeta);
			
			somaticWriter.write(getOutputHeader(true));
			germlineWriter.write(getOutputHeader(false));
			
			for (ChrPosition position : orderedList) {
				
				QSnpRecord record = positionRecordMap.get(position);
				String ensemblChr = record.getChromosome();
				for (Map.Entry<String, String> entry : ensembleToQCMG.entrySet()) {
					if (record.getChromosome().equals(entry.getValue())) {
						ensemblChr = entry.getKey();
						break;
					}
				}
				
				// default this to the record's chromosome - if an entry exists in the map, this will be overwritten
				// get ensembl chromosome
				
				// only write output if its been classified
				if (Classification.SOMATIC == record.getClassification())
					somaticWriter.write(analysisId + "\t" + tumourSampleId + "\t" 
							+ getFormattedRecord(record, ensemblChr) +"\n");
				else if (Classification.GERMLINE == record.getClassification())
					germlineWriter.write(analysisId + "\t" + normalSampleId + "\t" 
							+ getFormattedRecord(record, ensemblChr) +"\t" + record.getMutation() + "\n");
			}
//		} finally {
//			try {
//				somaticWriter.close();
//			} finally {
//				germlineWriter.close();
//			}
		}
	}
	
	
	String getDccMetaData() throws Exception {
		if (null == normalBams || normalBams.length == 0 || 
				StringUtils.isNullOrEmpty(normalBams[0]) 
				|| null == tumourBams || tumourBams.length == 0 
				|| StringUtils.isNullOrEmpty(tumourBams[0])) return null;
		
		SAMFileHeader controlHeader = SAMFileReaderFactory.createSAMFileReader(normalBams[0]).getFileHeader();
		SAMFileHeader analysisHeader = SAMFileReaderFactory.createSAMFileReader(tumourBams[0]).getFileHeader();
		
		QDccMeta dccMeta = QDccMetaFactory.getDccMeta(qexec, controlHeader, analysisHeader, "qSNP");
		
		return dccMeta.getDCCMetaDataToString();
	}
	
	String getLimsMetaData(String type, String bamFileName) throws Exception {
		SAMFileHeader header = SAMFileReaderFactory.createSAMFileReader(bamFileName).getFileHeader();
		QLimsMeta limsMeta = QLimsMetaFactory.getLimsMeta(type, bamFileName, header);
		return limsMeta.getLimsMetaDataToString();
	}
	
	void writeVCF(String outputFileName) throws IOException {
		if (StringUtils.isNullOrEmpty(outputFileName)) {
			logger.warn("No vcf output file scpecified so can't output vcf");
			return;
		}
		logger.info("Writing VCF output");
		
		VCFFileWriter writer = new VCFFileWriter(new File(outputFileName));
		String header = VcfUtils.getHeaderForQSnp(patientId, normalSampleId, tumourSampleId, "qSNP v" + Main.version);
		
		List<ChrPosition> orderedList = new ArrayList<ChrPosition>(positionRecordMap.keySet());
		Collections.sort(orderedList);
		
		try {
			writer.addHeader(header);
			for (ChrPosition position : orderedList) {
				
				QSnpRecord record = positionRecordMap.get(position);
				// only write output if its been classified
				if (null != record.getClassification()) {
					writer.add(convertQSnpToVCF(record));
				}
			}
		} finally {
			writer.close();
		}
	}
	
	void classifyPileupRecord(QSnpRecord record) {
		if (null != record.getNormalGenotype() && null != record.getTumourGenotype()) {
			classifyCount++;
			GenotypeComparisonUtil.compareGenotypes(record);
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
		
		if (Classification.SOMATIC == record.getClassification()) {
			final char alt = null != record.getMutation() ? SnpUtils.getAltFromMutationString(record.getMutation()) : '\u0000';
			final int mutantReadCount = SnpUtils.getCountFromNucleotideString(
					Classification.SOMATIC == record.getClassification() ? record.getTumourNucleotides() : record.getNormalNucleotides(), alt);
			if (mutantReadCount < mutantReadsFilterValue) {
				updateAnnotation(record, SnpUtils.MUTANT_READS);
			}
			if (record.getNovelStartCount() < novelStartsFilterValue) {
				updateAnnotation(record, SnpUtils.NOVEL_STARTS);
			}
		}
		
		if (StringUtils.isNullOrEmpty(record.getAnnotation()) || "--".equals(record.getAnnotation())) {
			updateAnnotation(record, VcfUtils.FILTER_PASS);
		}
	}
	
	void classifyPileup() {
		
		for (QSnpRecord record : positionRecordMap.values()) {
			classifyPileupRecord(record);
		}
		logger.info("No of records that have a genotype: " + classifyCount + ", out of " + positionRecordMap.size() 
				+ ". Somatic: " + classifySomaticCount + "[" + classifySomaticLowCoverage + "], germline: " 
				+ classifyGermlineCount + "[" + classifyGermlineLowCoverage + "] no classification: " + classifyNoClassificationCount + ", no mutation: " + classifyNoMutationCount);
	}
	
	void addDbSnpData(String fileName) throws Exception {
		VCFFileReader reader = new VCFFileReader(new File(fileName));
		
		int count = 0;
		try {
			for (VCFRecord dbSNPVcf : reader) {
				if (++count % 1000000 == 0)
					logger.info("hit " + count + " dbsnp records");
				
				// vcf dbSNP record chromosome does not contain "chr", whereas the positionRecordMap does - add
				QSnpRecord snpRecord = positionRecordMap.get(new ChrPosition("chr"+dbSNPVcf.getChromosome(), dbSNPVcf.getPosition()));
				if (null == snpRecord) continue;
				
				// only proceed if we have a SNP variant record
				if ( ! StringUtils.doesStringContainSubString(dbSNPVcf.getInfo(), "VC=SNV", false)) continue;
				
				GenotypeEnum genotype = snpRecord.getClassification() == Classification.SOMATIC 
						? snpRecord.getTumourGenotype() : snpRecord.getNormalGenotype();
				if (null == genotype) continue;
				
				// multiple dbSNP entries can exist for a position.
				// if we already have dbSNP info for this snp, check to see if the dbSNP alt is shorter than the existing dbSNP record
				// if so, proceed, and re-write dbSNP details (if applicable).
				int dbSNPAltLengh = dbSNPVcf.getAlt().length(); 
				if (snpRecord.getDbSnpAltLength() > 0 && dbSNPAltLengh > snpRecord.getDbSnpAltLength()) {
					continue;
				}
				
				// deal with multiple alt bases
				String [] alts = null;
				if (dbSNPAltLengh == 1) {
					alts = new String[] {dbSNPVcf.getAlt()};
				} else if (dbSNPAltLengh > 1){
					alts = TabTokenizer.tokenize(dbSNPVcf.getAlt(), ',');
				}
				
				if (null != alts) {
					for (String alt : alts) {
						
						GenotypeEnum dbSnpGenotype = BaseUtils.getGenotypeEnum(dbSNPVcf.getRef() +  alt);
						if (null == dbSnpGenotype) {
							logger.warn("Couldn't get Genotype from dbSNP position with variant: " + alt);
							continue;
						}
//				// no longer flip the genotype as dbSNP is reporting on the +ve strand
////				if (reverseStrand) {
////					dbSnpGenotype = dbSnpGenotype.getComplement();
////				}
						if (genotype == dbSnpGenotype || (genotype.isHomozygous() && dbSnpGenotype.containsAllele(genotype.getFirstAllele()))) {
							boolean reverseStrand = StringUtils.doesStringContainSubString(dbSNPVcf.getInfo(), "RV", false);
//							boolean reverseStrand = VcfUtils.isDbSNPVcfRecordOnReverseStrand(dbSNPVcf.getInfo());
							snpRecord.setDbSnpStrand(reverseStrand ? '-' : '+');
							snpRecord.setDbSnpId(dbSNPVcf.getId());
							snpRecord.setDbSnpGenotype(dbSnpGenotype);
							snpRecord.setDbSnpAltLength(dbSNPAltLengh);
							break;
						}
					}
				}
			}
		} finally {
			reader.close();
		}
	}
	
	void addIlluminaData() throws Exception {
		for (Entry<ChrPosition, QSnpRecord> entry : positionRecordMap.entrySet()) {
			ChrPosition cp = new ChrPosition(entry.getKey().getChromosome(), entry.getKey().getPosition());
			
			// update the illumina map
			IlluminaRecord normalIllRecord = normalIlluminaMap.get(cp);
			IlluminaRecord tumourIllRecord = tumourIlluminaMap.get(cp);
			
			if (null == normalIllRecord || null == tumourIllRecord) continue;
			
			QSnpRecord snpRecord = entry.getValue();
			
			// update illumina data if it exists
			snpRecord.setIlluminaNormalGenotype(
					GenotypeEnum.getGenotypeEnum(normalIllRecord.getFirstAllele(), normalIllRecord.getSecondAllele()));
			snpRecord.setIlluminaTumourGenotype(
					GenotypeEnum.getGenotypeEnum(tumourIllRecord.getFirstAllele(), tumourIllRecord.getSecondAllele()));
			
			// curious....
			if (null != snpRecord.getDbSnpId() 
					&& ( ! tumourIllRecord.getSnpId().equals(snpRecord.getDbSnpId()) 
							|| ! normalIllRecord.getSnpId().equals(snpRecord.getDbSnpId()))) {
				logger.info("mismatching dbSNP ids, qsnp: " + snpRecord.getDbSnpId() + ", normal Illumina: " + normalIllRecord.getSnpId()
						 + ", tumour Illumina: " + tumourIllRecord.getSnpId());
			}
		}
		
		// clear up some no longer needed resources
		normalIlluminaMap.clear();
		tumourIlluminaMap.clear();
	}
	
	private List<SAMFileHeader> getBamFileHeaders(String[] bams) {
		List<SAMFileHeader> headers = new ArrayList<SAMFileHeader>();
		for (String bam : bams) {
			SAMFileHeader header = SAMFileReaderFactory.createSAMFileReader(bam).getFileHeader();
			headers.add(header);
		}
		return headers;
	}
	
	private void checkHeadersSortOrder(List<SAMFileHeader> headers, boolean isNormal) throws SnpException {
		for (SAMFileHeader sfh : headers) {
			if (SAMFileHeader.SortOrder.coordinate != sfh.getSortOrder()) {
				throw new SnpException("BAM_FILE_NOT_SORTED", (isNormal ? "Normal" : "Tumour"));
			}
		}
	}
	
	private List<SAMSequenceDictionary> getSequenceDictionaries(List<SAMFileHeader> headers) {
		List<SAMSequenceDictionary> seqDictionaries = new ArrayList<SAMSequenceDictionary>();
		for (SAMFileHeader header : headers) {
			seqDictionaries.add(header.getSequenceDictionary());
		}
		return seqDictionaries;
	}
	
	private void checkSequenceDictionaries(List<SAMFileHeader> normalHeaders, List<SAMFileHeader> tumourHeaders) throws SnpException {
		List<SAMSequenceDictionary> normalSeqDictionaries = getSequenceDictionaries(normalHeaders);
		List<SAMSequenceDictionary> tumourSeqDictionaries = getSequenceDictionaries(tumourHeaders);
		
		for (SAMSequenceDictionary normalSD : normalSeqDictionaries) {
			List<SAMSequenceRecord> normalSequences = normalSD.getSequences();
			
			for (SAMSequenceDictionary tumourSD : tumourSeqDictionaries) {
				if (normalSD.getReferenceLength() != tumourSD.getReferenceLength()) {
					throw new SnpException("SEQUENCE_LENGTHS_DONT_MATCH"
							, ""+normalSD.getReferenceLength() , ""+tumourSD.getReferenceLength());
				}
				
				List<SAMSequenceRecord> tumourSequences = tumourSD.getSequences();
				int i = 0;
				for (SAMSequenceRecord normalSeq : normalSequences) {
					SAMSequenceRecord tumourSeq = tumourSequences.get(i++);
					if ( ! normalSeq.isSameSequence(tumourSeq) ) {
						throw new SnpException("SEQUENCES_DONT_MATCH", 
								new String[] {normalSeq.getSequenceName(), normalSeq.getSequenceLength()+"",
								tumourSeq.getSequenceName(), tumourSeq.getSequenceLength()+""});	
					}
				}
			}
		}
		
		
		// pick the first normal sequence to check against fasta
		List<SAMSequenceRecord> normalSequences = normalSeqDictionaries.get(0).getSequences();
		
		// now check against the supplied reference file
		FastaSequenceFile ref = new FastaSequenceFile(new File(referenceFile), true);
		if (null != ref) {
			try {
				ReferenceSequence nextRefSeq = null;
				
				// loop through the normal sequences, compare name and size against supplied reference
				for (SAMSequenceRecord normalSeq : normalSequences) {
					String name = normalSeq.getSequenceName();
					int length = normalSeq.getSequenceLength();
					
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
//						if ( ! nextRefSeq.getName().equalsIgnoreCase(name) || nextRefSeq.length() != length) {
//							logger.warn("Reference sequence names and lengths don't match. Normal sequence: " 
//									+ name + ":" + length + ", Reference sequence: " 
//									+ nextRefSeq.getName() + ":" + nextRefSeq.length());
//						}
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
		List<SAMFileHeader> normalHeaders = getBamFileHeaders(normalBams);
		List<SAMFileHeader> tumourHeaders = getBamFileHeaders(tumourBams);
		
		// check sort order
		checkHeadersSortOrder(normalHeaders, true);
		checkHeadersSortOrder(tumourHeaders, false);
		
		checkSequenceDictionaries(normalHeaders, tumourHeaders);
		logger.info("Checking bam file headers for reference file compatibility - DONE");
	}
	
	static void loadIlluminaData(String illuminaFile, Map<ChrPosition, IlluminaRecord> illuminaMap) throws IOException {
		IlluminaFileReader reader = new IlluminaFileReader(new File(illuminaFile));
		IlluminaRecord tempRec;
		try {
			for (Record rec : reader) {
				tempRec = (IlluminaRecord) rec;
				
				// only interested in illumina data if it has a gc score above 0.7, and a valid chromosome
				if (tempRec.getGCScore() >= 0.7f ) {
					
					//get XY, 0 for chromosome
					// ignore chromosome 0, and for XY, create 2 records, one for each!
					if (null != tempRec.getChr() && ! "0".equals(tempRec.getChr())) {
						
						if ("XY".equals(tempRec.getChr())) {
							// add both X and Y to map
							illuminaMap.put(new ChrPosition("chrX", tempRec.getStart()), tempRec);
							illuminaMap.put(new ChrPosition("chrY", tempRec.getStart()), tempRec);
							continue;
						}
						
						// Illumina record chromosome does not contain "chr", whereas the positionRecordMap does - add
						illuminaMap.put(new ChrPosition("chr" + tempRec.getChr(), tempRec.getStart()), tempRec);
					}
				}
			}
		} finally{
			reader.close();
		}
	}
	
	void loadChromosomeConversionData(String chrConvFile) throws IOException  {
		if ( ! StringUtils.isNullOrEmpty(chrConvFile)) {
			ChrConvFileReader reader = new ChrConvFileReader(new File(chrConvFile));
			try {
				for (ChromosomeConversionRecord record : reader) {
					// add extra map inserts here as required
					if ( ! "n/a".equals(record.getEnsembleV55()))
						ensembleToQCMG.put(record.getEnsembleV55(), record.getQcmg());
				}
			} finally {
				reader.close();
			}
		}
	}
	
	void addGermlineDBData(String germlineDBFile) throws Exception {
		
		if (StringUtils.isNullOrEmpty(germlineDBFile) || "null".equalsIgnoreCase(germlineDBFile)) {
			logger.info("Null or empty germlineDB entry in ini file - skipping....");
		} else {
		
			VCFFileReader reader = new VCFFileReader(new File(germlineDBFile));
			
			int [] idArray = VcfHeaderUtils.getIdsForPatient(VcfHeaderUtils.getMapFromInfoHeader(reader.getHeader()), patientId);
			boolean idArrayNotEmpty = null != idArray && idArray.length > 0;
			if (idArrayNotEmpty) Arrays.sort(idArray);
				
			// create map of SOMATIC classified SNPs
			Map<ChrPosition, QSnpRecord> somaticPileupMap = new HashMap<ChrPosition, QSnpRecord>(positionRecordMap.size(), 1);
			for (QSnpRecord pileupRecord : positionRecordMap.values()) {
				if (Classification.SOMATIC == pileupRecord.getClassification() 
						&& null != pileupRecord.getMutation() 
						&& (null == pileupRecord.getAnnotation() 
						|| ! pileupRecord.getAnnotation().contains(SnpUtils.MUTATION_GERMLINE_IN_ANOTHER_PATIENT)))
					
					somaticPileupMap.put(new ChrPosition(pileupRecord.getChromosome(), pileupRecord.getPosition()), pileupRecord);
			}
			
			int updateCount = 0, count = 0;
			try {
				for (VCFRecord rec : reader) {
					if (++count % 1000000 == 0) logger.info("hit " + count + " germline records");
					
					// if the VCFRecord is already a snp position for this patient, skip
					// UPDATE - only skip if it is only this patient that has a germline snp at this position
					String vcfInfo  = rec.getInfo();
					if (idArrayNotEmpty) {
						
						// get the number of ids that saw this germline variant
						String [] ids = (vcfInfo.contains(";")) ? TabTokenizer.tokenize(vcfInfo, ';') : new String[] {vcfInfo};
						
						if (ids.length > idArray.length) {
							// do nowt - more patients have seen this snp that just the one we are dealing with
						} else {
							boolean foundInAnotherPatient = false;
							for (String s : ids) {
								if (Arrays.binarySearch(idArray, Integer.parseInt(s)) == -1) {
									foundInAnotherPatient = true;
									break;
								}
							}
							
							if ( ! foundInAnotherPatient) continue;
						}
					}
					
					// get QCMG chromosome from map
					String chr = ensembleToQCMG.get(rec.getChromosome());
					if (null == chr) continue;
					
					ChrPosition id = new ChrPosition(chr, rec.getPosition());
					
					if (somaticPileupMap.containsKey(id)) {
						QSnpRecord qpr = somaticPileupMap.get(id);
						
						String mutation = qpr.getMutation();
						if (mutation.length() == 3) {
							char c = mutation.charAt(2);
							
							if ( ! StringUtils.isNullOrEmpty(rec.getAlt()) && rec.getAlt().indexOf(c) != -1) {
								updateAnnotation(qpr, SnpUtils.MUTATION_GERMLINE_IN_ANOTHER_PATIENT);
								updateCount++;
							}
							
						} else {
							logger.info("mutation string length: " + mutation.length());
						}
					}
				}
			} finally {
				reader.close();
			} 
			logger.info("updated: " + updateCount + " somatic positions with germlineDB info");
			
			// log the number of entries in the germline file - may help debugging if too few GERM annotation are reported
			logger.info("Number of entries in Germline file: " + count);
		}
	}
	
	/**
	 * For SOMATIC positions, that don't currently have evidence of the mutation in the normal, examine
	 * the unfiltered normal to see if any evidence exists there
	 * 
	 */
	void incorporateUnfilteredNormal() {
		int noOfAffectedRecords = 0;
		for (Entry<ChrPosition, QSnpRecord> entry : positionRecordMap.entrySet()) {
			QSnpRecord record = entry.getValue();
			if (Classification.SOMATIC == record.getClassification()
					&& (null == record.getAnnotation() 
					|| ! record.getAnnotation().contains(SnpUtils.MUTATION_IN_NORMAL))	// PASS will pass this test :)
					&& ! StringUtils.isNullOrEmpty(record.getUnfilteredNormalPileup())) {
				
				char alt = record.getMutation().charAt(record.getMutation().length()-1);
				
				if (record.getUnfilteredNormalPileup().indexOf(Character.toUpperCase(alt)) > -1) {
					updateAnnotation(record, SnpUtils.MUTATION_IN_UNFILTERED_NORMAL);
					noOfAffectedRecords++;
				}
			}
		}
		
		logger.info("number of SOMATIC snps that have evidence of mutation in unfiltered normal: " + noOfAffectedRecords);
	}
	
	public  VCFRecord convertQSnpToVCF(QSnpRecord rec) {
		VCFRecord vcf = new VCFRecord();
		vcf.setChromosome(rec.getChromosome());
		vcf.setPosition(rec.getPosition());
		vcf.setId(rec.getDbSnpId());
		vcf.setRef(rec.getRef());
		final char alt = null != rec.getMutation() ? SnpUtils.getAltFromMutationString(rec.getMutation()) : '\u0000';  
//		if (alt != '\u0000') {
//			vcf.setAlt(""+alt);
//		}
		final int mutantReadCount = SnpUtils.getCountFromNucleotideString(
				Classification.SOMATIC == rec.getClassification() ? rec.getTumourNucleotides() : rec.getNormalNucleotides(), alt);
		final int novelStartCount = Classification.SOMATIC == rec.getClassification() 
					? rec.getTumourNovelStartCount() : rec.getNormalNovelStartCount();
		
//		if (StringUtils.isNullOrEmpty(rec.getAnnotation()) || "--".equals(rec.getAnnotation())) {
//			vcf.setFilter(VcfUtils.FILTER_PASS);
//		} else {
			vcf.setFilter(rec.getAnnotation());
//		}
//		vcf.setFilter(StringUtils.isNullOrEmpty(rec.getAnnotation()) || "--".equals(rec.getAnnotation()) ? "PASS" : rec.getAnnotation());
		
		StringBuilder info = new StringBuilder();
		if (Classification.SOMATIC == rec.getClassification())
			info.append(rec.getClassification().toString());
		
		// add Number of Mutations (MR - Mutated Reads)
		if (mutantReadCount > 0) {
			if (info.length() > 0) info.append(SEMI_COLON);
			info.append(VcfUtils.INFO_MUTANT_READS).append(EQ).append(mutantReadCount);
		}
		
		if (novelStartCount > 0) {
			if (info.length() > 0) info.append(SEMI_COLON);
			info.append(VcfUtils.INFO_NOVEL_STARTS).append(EQ).append(novelStartCount);
		}
		
		// cpg data
		if ( ! StringUtils.isNullOrEmpty(rec.getFlankingSequence())) {
			if (info.length() > 0) info.append(SEMI_COLON);
			info.append(VcfUtils.INFO_FLANKING_SEQUENCE).append(EQ).append(rec.getFlankingSequence());
		}
		
		String [] altAndGTs = VcfUtils.getMutationAndGTs(rec.getRef(), rec.getNormalGenotype(), rec.getTumourGenotype());
		vcf.setAlt(altAndGTs[0]);
		
		
		// FORMAT field - contains GT field (and others)
		StringBuilder formatField = new StringBuilder();
		// add in the columns
		formatField.append(VcfUtils.FORMAT_GENOTYPE).append(COLON);
		formatField.append(VcfUtils.FORMAT_GENOTYPE_DETAILS).append(COLON);
		formatField.append(VcfUtils.FORMAT_ALLELE_COUNT).append(TAB);
		
//		String normalGTField = null != rec.getNormalGenotype() ? rec.getNormalGenotype().getGTString(rec.getRef()) : ""+VCFRecord.MISSING_DATA;
//		String tumourGTField = null != rec.getTumourGenotype() ? rec.getTumourGenotype().getGTString(rec.getRef()) : ""+VCFRecord.MISSING_DATA;
		
		String normalGDField = null != rec.getNormalGenotype() ? rec.getNormalGenotype().getDisplayString() : ""+VCFRecord.MISSING_DATA;
		String tumourGDField = null != rec.getTumourGenotype() ? rec.getTumourGenotype().getDisplayString() : ""+VCFRecord.MISSING_DATA;
		
		// add in normal format details first, then tab, then tumour
		formatField.append(altAndGTs[1]).append(COLON);
		formatField.append(normalGDField).append(COLON);
		String nNucleotides = StringUtils.isNullOrEmpty(rec.getNormalNucleotides()) ? ""+VCFRecord.MISSING_DATA : rec.getNormalNucleotides(); 
		String tNucleatides = StringUtils.isNullOrEmpty(rec.getTumourNucleotides()) ? ""+VCFRecord.MISSING_DATA : rec.getTumourNucleotides();
		
		formatField.append(nNucleotides.replace(":", ""));// remove colons in nucleotide strings
		formatField.append(TAB);
		// tumour
		formatField.append(altAndGTs[2]).append(COLON);
		formatField.append(tumourGDField).append(COLON);
		formatField.append(tNucleatides.replace(":", ""));// remove colons in nucleotide strings
		
		vcf.setInfo(info.toString());
		vcf.addExtraField(formatField.toString());
		return vcf;
	}
	
	/**
	 * TODO
	 * add the appropriate flag should the motif be found
	 * 
	 */
	void incorporateCPGData() {
		int noOfAffectedRecords = 0;
		for (QSnpRecord record : positionRecordMap.values()) {
			
			String cpg = record.getFlankingSequence(); 
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
		String[] params = TabTokenizer.tokenize(record);

		// get coverage for both normal and tumour
		int normalCoverage = PileupUtils.getCoverageCount(params, normalStartPositions);
		int tumourCoverage = PileupUtils.getCoverageCount(params, tumourStartPositions);
		
		if (normalCoverage + tumourCoverage < initialTestSumOfCountsLimit) return;

		String normalBases = PileupUtils.getBases(params, normalStartPositions);
		String tumourBases = PileupUtils.getBases(params, tumourStartPositions);
		
		if ( ! includeIndels) {
			// means there is an indel at this position - ignore
			if (PileupUtils.doesPileupContainIndel(normalBases)) return;
			if (PileupUtils.doesPileupContainIndel(tumourBases)) return;
		}
		
		String normalBaseQualities = PileupUtils.getQualities(params, normalStartPositions);
		String tumourBaseQualities = PileupUtils.getQualities(params, tumourStartPositions);

		// get bases as PileupElement collections
		List<PileupElement> normalBaseCounts = PileupElementUtil.getPileupCounts(normalBases, normalBaseQualities);
		List<PileupElement> tumourBaseCounts = PileupElementUtil.getPileupCounts(tumourBases, tumourBaseQualities);

		// get variant count for both
		int normalVariantCount = PileupElementUtil.getLargestVariantCount(normalBaseCounts);
		int tumourVariantCount = PileupElementUtil.getLargestVariantCount(tumourBaseCounts);
		
		// new - hope this doesn't bugger things up!!
		// only proceed if we actually have at least 1 variant
		if (normalVariantCount + tumourVariantCount == 0) return;
		
		// get rule for normal and tumour
		Rule normalRule = RulesUtil.getRule(normalRules, normalCoverage);
		Rule tumourRule = RulesUtil.getRule(tumourRules, tumourCoverage);
		
		
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

			QSnpRecord qRecord = new QSnpRecord();
			qRecord.setPileup(record);
//			qRecord.setPileup(record.getPileup());
			// setup some values on the record
			qRecord.setChromosome(params[0]);
			qRecord.setPosition(Integer.parseInt(params[1]));
			qRecord.setRef(params[2].charAt(0));
			qRecord.setNormalCount(normalCoverage);
			qRecord.setTumourCount(tumourCoverage);
			
			// set normal pileup to only contain the different bases found in normal, rather than the whole pileup string
			// which contains special chars indicating start/end of reads along with mapping qualities
			if (normalCoverage > 0)
				qRecord.setNormalPileup(PileupElementUtil.getBasesFromPileupElements(normalBaseCounts, qRecord.getRef()));

			// use all base counts to form genotype
			List<PileupElement> normalBaseCountsPassRule = PileupElementUtil
				.getPileupCountsThatPassRule(normalBaseCounts, normalRule, normalSecondPass, baseQualityPercentage);
			List<PileupElement> tumourBaseCountsPassRule = PileupElementUtil
				.getPileupCountsThatPassRule(tumourBaseCounts, tumourRule, tumourSecondPass, baseQualityPercentage);
			
			qRecord.setNormalGenotype(PileupElementUtil.getGenotype(normalBaseCountsPassRule, qRecord.getRef()));
			qRecord.setTumourGenotype(PileupElementUtil.getGenotype(tumourBaseCountsPassRule, qRecord.getRef()));
			
			qRecord.setNormalNucleotides(PileupElementUtil.getPileupElementString(normalBaseCounts, qRecord.getRef()));
			qRecord.setTumourNucleotides(PileupElementUtil.getPileupElementString(tumourBaseCounts, qRecord.getRef()));
			
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
		PileupElement pe = PileupElementUtil.getLargestVariant(baseCounts);
		return null == pe ? false :  pe.isFoundOnBothStrands();
	}
	
	/**
	 * Assumes normal bams come before tumour bams in the pileup file
	 */
	void getStringPositions() {
		normalStartPositions = PileupUtils.getStartPositions(noOfNormalFiles, noOfTumourFiles, true);
		tumourStartPositions =PileupUtils.getStartPositions(noOfNormalFiles, noOfTumourFiles, false);
	}
	
	void checkRules() throws SnpException {
		String rulesErrors = RulesUtil.examineRules(normalRules);
		if (null != rulesErrors) logger.warn("Problem with Normal Rules: " + rulesErrors);
		
		rulesErrors = RulesUtil.examineRules(tumourRules);
		if (null != rulesErrors) logger.warn("Problem with Tumour Rules: " + rulesErrors);
	}
	
	void loadNextReferenceSequence() {
		if (null == sequenceFile) {
			sequenceFile = new FastaSequenceFile(new File(referenceFile), true);
		}
		referenceBases = null;
		currentChr = null;
		referenceBasesLength = 0;
		ReferenceSequence refSeq = sequenceFile.nextSequence();
		
		// debugging code
//		while ( ! "chr1".equals(refSeq.getName()))
//			refSeq = sequenceFile.nextSequence();
//			 debugging code
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
		for (QSnpRecord rec : positionRecordMap.values()) {
			queue.add(rec);
		}
		
		int noOfThreadsToUse = 5;
		ExecutorService service = Executors.newFixedThreadPool(noOfThreadsToUse);
		for (int i = 0 ; i < noOfThreadsToUse ; i++) {
			service.execute(new Runnable() {

				@Override
				public void run() {
					// take a QSnpRecord, if none left we are done
					while (true) {
						QSnpRecord record = queue.poll();
						if (null == record) break;
						
						String mutation = record.getMutation();
						if (StringUtils.isNullOrEmpty(mutation)) continue;
						
						char ref = record.getRef();
						char alt = SnpUtils.getAltFromMutationString(mutation);
						
						int aNormalAlt = SnpUtils.getCountFromNucleotideString(record.getNormalNucleotides(), alt);
						int bTumourAlt = SnpUtils.getCountFromNucleotideString(record.getTumourNucleotides(), alt);
						int cNormalRef = SnpUtils.getCountFromNucleotideString(record.getNormalNucleotides(), ref);
						int dTumourRef = SnpUtils.getCountFromNucleotideString(record.getTumourNucleotides(), ref);
						
						double pValue = FisherExact.getTwoTailedFET(aNormalAlt, bTumourAlt, cNormalRef, dTumourRef);
	//					logger.info("pvalue for following values (a,b,c,d:pvalue): " + normalAlt + "," + tumourAlt + "," + normalRef + "," + tumourRef + ": " + pValue);
						record.setProbability(pValue);
					}
					
				}});
		}
		service.shutdown();
		
		try {
			service.awaitTermination(10, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		logger.info("About to hit Fisher Exact Test two tailed pvalue - DONE");
		
		
	}
	
	
	
	void walkBams() throws Exception {
		walkBams(true);
	}
	
	/**
	 * Sets up 2 Producer threads, 2 Consumer threads and a Cleaner thread, along with the concurrent collections, queues, and barriers used by them all
	 * 
	 * @param ignoreDuplicates indicates whether duplicate records should be discarded out right. Not useful for torrent mode
	 * @throws Exception
	 */
	void walkBams(boolean ignoreDuplicates) throws Exception {
		logger.info("About to hit bam files");
		final AtomicInteger normalMinStart = new AtomicInteger();
		final AtomicInteger tumourMinStart = new AtomicInteger();
		final Queue<SAMRecordFilterWrapper> normalSAMQueue = new ConcurrentLinkedQueue<SAMRecordFilterWrapper>();
		final Queue<SAMRecordFilterWrapper> tumourSAMQueue = new ConcurrentLinkedQueue<SAMRecordFilterWrapper>();
		
		// used by Cleaner3 threads
		final ConcurrentMap<Integer, Accumulator> cnormalAccs = new ConcurrentHashMap<Integer, Accumulator>(1024 * 1024);
		final ConcurrentMap<Integer, Accumulator> ctumourAccs = new ConcurrentHashMap<Integer, Accumulator>(1024 * 1024);
		
		final CyclicBarrier barrier = new CyclicBarrier(5, new Runnable() {
			@Override
			public void run() {
				// reset the minStartPositions values to zero
				normalMinStart.set(0);
				tumourMinStart.set(0);
				
				// update the reference bases array
				loadNextReferenceSequence();
				logger.info("barrier has been reached by all threads - moving onto next chromosome");
			}
		});
		ExecutorService service = Executors.newFixedThreadPool(5);
		CountDownLatch consumerLatch = new CountDownLatch(2);
		CountDownLatch normalProducerLatch = new CountDownLatch(1);
		CountDownLatch tumourProducerLatch = new CountDownLatch(1);
		CountDownLatch cleanerLatch = new CountDownLatch(1);
//		ExecutorService service = Executors.newFixedThreadPool(2);
		
		
		// Producers
		service.execute(new Producer(normalBams, normalProducerLatch, true, normalSAMQueue, Thread.currentThread(), query, barrier, ignoreDuplicates));
		service.execute(new Producer(tumourBams, tumourProducerLatch, false, tumourSAMQueue, Thread.currentThread(), query, barrier, ignoreDuplicates));
		
		// Cleaner
		service.execute(new Consumer(consumerLatch, normalProducerLatch, tumourProducerLatch, true, 
				Thread.currentThread(), barrier, cnormalAccs, normalSAMQueue, normalMinStart));
		service.execute(new Consumer(consumerLatch, normalProducerLatch, tumourProducerLatch, false, 
				Thread.currentThread(), barrier, ctumourAccs, tumourSAMQueue, tumourMinStart));
		
		service.execute(new Cleaner(cleanerLatch, consumerLatch, Thread.currentThread(),
				barrier, normalMinStart, tumourMinStart, cnormalAccs, ctumourAccs));
		
		service.shutdown();
		try {
			normalProducerLatch.await();
			tumourProducerLatch.await();
			consumerLatch.await();
			cleanerLatch.await();
		} catch (InterruptedException e) {
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
		private final boolean isNormal;
		private final CountDownLatch latch;
		private final Queue<SAMRecordFilterWrapper> queue;
		private QueryExecutor qbamFilter;
		private final Thread mainThread;
		private long passedFilterCount = 0;
		private long invalidCount = 0;
		private long counter = 0;
		private final CyclicBarrier barrier;
		private final boolean ignoreDuplicates;
		private final boolean runqBamFilter;
		
		public Producer(final String[] bamFiles, final CountDownLatch latch, final boolean isNormal, 
				final Queue<SAMRecordFilterWrapper> samQueue, final Thread mainThread, final String query, 
				final CyclicBarrier barrier, boolean ignoreDuplicates) throws Exception {
			this.latch = latch;
			Set<File> bams = new HashSet<File>();
			for (String bamFile : bamFiles) {
				bams.add(new File(bamFile));
			}
			this.reader = new MultiSAMFileReader(bams, true, validation);
			this.iter = reader.getMultiSAMFileIterator();
			this.isNormal = isNormal;
			this.mainThread = mainThread;
			this.queue = samQueue;
			if ( ! StringUtils.isNullOrEmpty(query) && ! "QCMG".equals(query))
				qbamFilter = new QueryExecutor(query);
			this.barrier = barrier;
			this.ignoreDuplicates = ignoreDuplicates;
			runqBamFilter = null != qbamFilter;
		}

		@Override
		public void run() {
			logger.info("In Producer run method with isNormal: " + isNormal);
			logger.info("Use qbamfilter? " + runqBamFilter);
			try {
//				boolean runqBamFilter = null != qbamFilter;
//				logger.info("Use qbamfilter? " + runqBamFilter);
				
//				SAMRecordIterator iter = reader.query(currentChr, 0, 0, true);
				while (iter.hasNext()) {
					SAMRecord record = iter.next();
//				for (SAMRecord record : reader) {
					
					if (++ counter % 1000000 == 0) {
						int qSize = queue.size();
						logger.info("hit " + counter/1000000 + "M sam records, passed filter: " + passedFilterCount + ", qsize: " + qSize);
						if (passedFilterCount == 0 && counter >= noOfRecordsFailingFilter) {
							throw new SnpException("INVALID_FILTER", ""+counter);
						}
						while (qSize > 10000) {
							try {
								Thread.sleep(200);
							} catch (InterruptedException e) {
								logger.warn("InterruptedException caught whilst Producer thread was sleeping");
								throw e;
							}
							qSize = queue.size();
						}
					}
					
					if (record.getReferenceName().equals(currentChr)) {
						processRecord(record);
						
//						// if its a dup - don't even add as unfiltered record - discard
//						if (ignoreDuplicates && record.getDuplicateReadFlag()) {
//							dupCounter++;
//							continue;
//						}
//						
//						if (runqBamFilter) {
//							boolean passesFilter = qbamFilter.Execute(record);
//							if (isNormal || passesFilter) {
//								addRecordToQueue(record, counter, passesFilter);
//							}
//						} else {
//							// didn't have any filtering defined - add all
//							addRecordToQueue(record, counter, true);
//						}
						
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
						} catch (InterruptedException e) {
							logger.error("Producer: InterruptedException exception caught whilst processing record: " + SAMUtils.getSAMRecordAsSting(record), e);
							throw e;
						} catch (BrokenBarrierException e) {
							logger.error("Producer: BrokenBarrier exception caught whilst processing record: " + SAMUtils.getSAMRecordAsSting(record), e);
							throw e;
						}
						
						// wait until queues are empty
						int qSize = queue.size();
						if (qSize > 0)
							logger.info("Waiting for empty queue before continuing with next chr. qsize: " + qSize);
						while (qSize > 0) {
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								e.printStackTrace();
								throw e;
							}
							qSize = queue.size();
						}
						// deal with this record
						processRecord(record);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				mainThread.interrupt();
			} finally {
				latch.countDown();
				logger.info("Producer: shutting down - processed " + counter + " records, passed filter: " 
						+ passedFilterCount + ", invalidCount: " + invalidCount);
			}
		}
		
		private void processRecord(SAMRecord record) throws Exception {
			
			// if record is not valid for variatn calling - discard
			if ( ! SAMUtils.isSAMRecordValidForVariantCalling(record)) {
				invalidCount++;
				return;
			}
			
			// if its a dup - don't even add as unfiltered record - discard
//			if (ignoreDuplicates && record.getDuplicateReadFlag()) {
//				duplicateCount++;
//				return;
//			}
			
			if (runqBamFilter) {
				boolean passesFilter = qbamFilter.Execute(record);
				if (isNormal || passesFilter) {
					addRecordToQueue(record, counter, passesFilter);
				}
			} else {
				// didn't have any filtering defined - add all
				addRecordToQueue(record, counter, true);
			}
		}
		
		private void addRecordToQueue(final SAMRecord record, final long counter, final boolean passesFilter) {
			
			record.getReadBases();					// cache read bases in object
			if (passesFilter) {
				record.getBaseQualities();	// cache base qualities in object
				passedFilterCount++;
			}
			record.getCigar();					// cache cigar for all records
			record.getAlignmentEnd();		// cache alignment end for all records
//			if (record.getReadNegativeStrandFlag()) record.getAlignmentEnd();		// cache alignment end if its on reverse strand
			
			final SAMRecordFilterWrapper wrapper = new SAMRecordFilterWrapper(record, counter);
			wrapper.setPassesFilter(passesFilter);
			queue.add(wrapper);
		}
	}
	
	public class Consumer implements Runnable {
		
		private final CountDownLatch consumerLatch;
		private final CountDownLatch normalLatch;
		private final CountDownLatch tumourLatch;
		private final boolean isNormal;
		private final Thread mainThread;
		private final  ConcurrentMap<Integer, Accumulator> map;
		private final CyclicBarrier barrier;
		private final Queue<SAMRecordFilterWrapper> queue;
		private final AtomicInteger minStartPosition;
		
		private final int maxMapSize = 100000;
		
		public Consumer(final CountDownLatch consumerLatch, final CountDownLatch normalLatch, 
				final CountDownLatch tumourLatch, final boolean isNormal, final Thread mainThread, 
				final CyclicBarrier barrier, final ConcurrentMap<Integer, Accumulator> map,
				final Queue<SAMRecordFilterWrapper> queue, final AtomicInteger minStartPosition){
			
			this.consumerLatch = consumerLatch;
			this.normalLatch = normalLatch;
			this.tumourLatch = tumourLatch;
			this.isNormal = isNormal;
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
//			final int endPosition = forwardStrand ? startPosition : sam.getAlignmentEnd();
			final byte[] bases = sam.getReadBases();
			final byte[] qualities = record.getPassesFilter() ? sam.getBaseQualities() : null;
			final Cigar cigar = sam.getCigar();
			
			int referenceOffset = 0, offset = 0;
			
			for (CigarElement ce : cigar.getCigarElements()) {
				CigarOperator co = ce.getOperator();
				int length = ce.getLength();

				if (co.consumesReferenceBases() && co.consumesReadBases()) {
					// we have a number (length) of bases that can be advanced.
//					if (debugOn) {
//					}
//					if ( 169798 == startPosition || 169809 == startPosition) {
//						logger.info("about to call updateListWithAccums(startPosition: " + startPosition +", bases: " + bases + 
//							", qualities: " + qualities + ", forwardStrand: " + forwardStrand + ", offset: "
//							+ offset + ",cigar: " + cigar.toString() + ", length: " + length + ", referenceOffset: " + referenceOffset + ", isNormal: " + isNormal + ")");
//					}
					updateMapWithAccums(startPosition, bases,
							qualities, forwardStrand, offset, length, referenceOffset, 
							record.getPassesFilter(), endPosition);
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
				boolean forwardStrand, int offset, int length, int referenceOffset, final boolean passesFilter, final int readEndPosition) {
			final int startPosAndRefOffset = startPosition + referenceOffset;
			
			
			for (int i = 0 ; i < length ; i++) {
				Accumulator acc = map.get(i + startPosAndRefOffset);
				if (null == acc) {
					acc = new Accumulator(i + startPosAndRefOffset);
					Accumulator oldAcc = map.putIfAbsent(i + startPosAndRefOffset, acc);
					if (null != oldAcc) acc = oldAcc;
				}
				if (passesFilter) {
					acc.addBase(bases[i + offset], qualities[i + offset], forwardStrand, 
							startPosition, i + startPosAndRefOffset, readEndPosition);
				} else {
					acc.addUnfilteredBase(bases[i + offset]);
				}
			}
		}
		
		@Override
		public void run() {
			logger.info("In Consumer run method with isNormal: " + isNormal);
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
									} catch (InterruptedException e) {
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
						if (normalLatch.getCount() == 0 && tumourLatch.getCount() == 0) {
							break;
						}
						// check the barrier - could be zero
						if (barrier.getNumberWaiting() >= 2) {
//							logger.info("null record, barrier count > 2 - what now??? q.size: " + queue.size());
							// just me left
							if (queue.size() == 0 ) {
								logger.info("Consumer: Processed all records in " + currentChr + ", waiting at barrier");
								
								try {
									barrier.await();
									assert map.isEmpty() : "Consumer: map was not empty following barrier reset";
									count = 0;
								} catch (InterruptedException e) {
									logger.error("Consumer: InterruptedException caught with map size: " + map.size(), e);
									throw e;
								} catch (BrokenBarrierException e) {
									logger.error("Consumer: BrokenBarrierException caught with map size: " + map.size(), e);
									throw e;
								}
							}
						} else {
							// sleep and try again
							try {
								Thread.sleep(50);
							} catch (InterruptedException e) {
								logger.error("InterruptedException caught in Consumer sleep",e);
								throw e;
							}
						}
					}
				}
			} catch (Exception e) {
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
		private final AtomicInteger normalMinStart;
		private final AtomicInteger tumourMinStart;
		private final  ConcurrentMap<Integer, Accumulator> cnormalAccs;
		private final  ConcurrentMap<Integer, Accumulator> ctumourAccs;
		private long processMaspCounter = 0;
		private final boolean debugLoggingEnabled;
		
		public Cleaner(CountDownLatch cleanerLatch, CountDownLatch consumerLatch, Thread mainThread, CyclicBarrier barrier,
				final AtomicInteger normalMinStart, final AtomicInteger tumourMinStart,
				final ConcurrentMap<Integer, Accumulator> cnormalAccs, final ConcurrentMap<Integer, Accumulator> ctumourAccs) {
			this.consumerLatch = consumerLatch;
			this.cleanerLatch = cleanerLatch;
			this.mainThread = mainThread;
			this.barrier = barrier;
			this.normalMinStart = normalMinStart;
			this.tumourMinStart = tumourMinStart;
			this.cnormalAccs = cnormalAccs;
			this.ctumourAccs = ctumourAccs;
			debugLoggingEnabled = logger.isLevelEnabled(QLevel.DEBUG);
		}
		
		private void processMaps() {
			final int minStartPos = Math.min(normalMinStart.intValue(), tumourMinStart.intValue()) - 1;
			if (debugLoggingEnabled && ++processMaspCounter % 10 == 0) {
				logger.debug("min start position: " + minStartPos + ", no of keepers so far: " + positionRecordMap.size());
			}
			if (minStartPos <= 0) return;
			final boolean useContainsKey = (minStartPos - previousPosition) > 100000; 
				
			for (int i = previousPosition ; i < minStartPos ; i++) {
				Accumulator normAcc = useContainsKey ? (cnormalAccs.containsKey(i) ? cnormalAccs.remove(i) : null) : cnormalAccs.remove(i);
				Accumulator tumAcc = useContainsKey ? (ctumourAccs.containsKey(i) ? ctumourAccs.remove(i) : null) : ctumourAccs.remove(i);
				if (null != tumAcc && null != normAcc) {
					processNormalAndTumour(normAcc, tumAcc);
				} else if (null != normAcc){
					processNormal(normAcc);
				} else if (null != tumAcc){
					processTumour(tumAcc);
				}
			}
				
			previousPosition = minStartPos;
		}
		
		private void processMapsAll() {
			if (null != referenceBases && ( ! cnormalAccs.isEmpty() || ! ctumourAccs.isEmpty())) {
			
				for (Map.Entry<Integer, Accumulator> entry : cnormalAccs.entrySet()) {
					Integer i = entry.getKey();
					Accumulator normAcc = entry.getValue();
					if (i.intValue() >  referenceBasesLength) {
						logger.warn("Found position greater than reference array length: " + i.intValue() + " for chr: " + currentChr);
					} else {
						if (null == normAcc) {
							logger.info("null normal acc for key: " + i);
						} else {
							Accumulator tumAcc = ctumourAccs.remove(i);
							if (null != tumAcc) {
								processNormalAndTumour(normAcc, tumAcc);
							} else {
								processNormal(normAcc);
							}
						}
					}
				}
				cnormalAccs.clear();
				
				// same for tumour
				for (Map.Entry<Integer, Accumulator> entry : ctumourAccs.entrySet()) {
					Integer i = entry.getKey();
					Accumulator tumAcc = entry.getValue();
					if (i.intValue() >  referenceBasesLength) {
						logger.warn("Found position greater than reference array length: " + i.intValue() + " for chr: " + currentChr);
					} else {
						if (null == tumAcc) {
							logger.info("null value for key: " + i);
						} else {
							processTumour(tumAcc);
						}
					}
				}
				ctumourAccs.clear();
			}
		}
		
		@Override
		public void run() {
			logger.info("In Cleaner run method");
			try {
				Thread.sleep(500);	// sleep to allow initial population of both queues
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			
			try {
				while (true) {
					
					processMaps();
						
					if (barrier.getNumberWaiting() == barrier.getParties() - 1) {
						logger.info("Cleaner: about to hit barrier - running processMapsAll");
						
						processMapsAll();
						try {
							previousPosition = 0;
							barrier.await();
							logger.info("Cleaner: no of keepers so far: " + positionRecordMap.size());
							Thread.sleep(500);	// sleep to allow initial map population
						} catch (InterruptedException e) {
							logger.error("InterruptedException caught in Cleaner thread: ", e);
							throw e;
						} catch (BrokenBarrierException e) {
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
						} catch (InterruptedException e) {
							logger.error("InterruptedException caught in Cleaner sleep",e);
							throw e;
						}
					}
				}
			} catch (Exception e) {
				logger.error("Exception caught in Cleaner thread: ", e);
				mainThread.interrupt();
			} finally {
				cleanerLatch.countDown();
				logger.info("Cleaner: finished - counting down cleanerLatch");
			}
		}
	}
	
	private void processTumour(Accumulator tumourAcc) {
		if (tumourAcc.containsMutation() || 
				(tumourAcc.getPosition() -1 < referenceBasesLength 
						&& ! baseEqualsReference(tumourAcc.getBase(),tumourAcc.getPosition() -1))) {
			interrogateAccumulations(null, tumourAcc);
		}
	}
	private void processNormal(Accumulator normalAcc) {
		if (normalAcc.containsMutation() || 
				(normalAcc.getPosition() -1 < referenceBasesLength
						&& ! baseEqualsReference(normalAcc.getBase(), normalAcc.getPosition() -1))) {
			interrogateAccumulations(normalAcc, null);
		}
	}
	private void processNormalAndTumour(Accumulator normalAcc, Accumulator tumourAcc) {
		if (normalAcc.containsMutation() || tumourAcc.containsMutation() 
				|| (normalAcc.getBase() != tumourAcc.getBase())
				|| (tumourAcc.getPosition() -1 < referenceBasesLength 
						&& ! baseEqualsReference(tumourAcc.getBase(), tumourAcc.getPosition() -1))
				|| (normalAcc.getPosition() -1 < referenceBasesLength
						&& ! baseEqualsReference(normalAcc.getBase(), normalAcc.getPosition() -1))) {
			interrogateAccumulations(normalAcc, tumourAcc);
		}
	}
	
	private boolean baseEqualsReference(char base, int position) {
		char refBase = (char) referenceBases[position];
		if (base == refBase) return true;
		if (Character.isLowerCase(refBase)) {
			char upperCaseRef = Character.toUpperCase(refBase);
			return base == upperCaseRef;
		} else return false;
	}
	
	/**
	 * Checks to see if the existing annotation is a PASS.
	 * If it is, then the annotation is replaced with the supplied annotation.
	 * If its not, the supplied annotation is appended to the existing annotation(s)
	 * 
	 * Also, if the supplied annotation is a PASS, then all previous annotations are removed.
	 * 
	 * @param rec qsnp record
	 * @param ann String representation of the annotation
	 */
	public static void updateAnnotation(QSnpRecord rec, String ann) {
		// perform some null guarding
		if (null == rec) throw new IllegalArgumentException("Null qsnp record passed to updateAnnotation");
		
		if (SnpUtils.PASS.equals(rec.getAnnotation()) || SnpUtils.PASS.equals(ann)) {
			rec.setAnnotation(ann);
		} else {
			rec.addAnnotation(ann);
		}
	}
	
	// strand bias check
	void checkForStrandBias(QSnpRecord rec, Accumulator normal, Accumulator tumour, char ref ) {
		if (runSBIASAnnotation) {
			PileupElementLite pel = Classification.SOMATIC == rec.getClassification() ? (null != tumour? tumour.getLargestVariant(ref) : null) : 
				(null != normal ? normal.getLargestVariant(ref) : null);
			
			if (null != pel && ! pel.isFoundOnBothStrands()) {
				updateAnnotation(rec, SnpUtils.STRAND_BIAS);
			}
		}
	}
	
	// ends of reads check
	void checkForEndsOfReads(QSnpRecord rec, Accumulator normal, Accumulator tumour, char ref ) {
		
		PileupElementLite pel = Classification.SOMATIC == rec.getClassification() ? (null != tumour? tumour.getLargestVariant(ref) : null) : 
			(null != normal ? normal.getLargestVariant(ref) : null);
		
		if (null != pel && pel.getEndOfReadCount() > 0) {
			
			if (pel.getMiddleOfReadCount() >= 5 && pel.isFoundOnBothStrandsMiddleOfRead()) {
				// all good
			} else {
				updateAnnotation(rec, SnpUtils.END_OF_READ + pel.getEndOfReadCount());
			}
		}
	}
	
	void checkForMutationInNormal() {
		int minCount = 0;
		for (QSnpRecord record : positionRecordMap.values()) {
			if (null != record.getAnnotation() && record.getAnnotation().contains(SnpUtils.MUTATION_IN_NORMAL)) {
				// check to see if mutant count in normal is 3% or more
				// if not, remove annotation
				final String ND = record.getNormalNucleotides();
				final int normalCount = record.getNormalCount();
				final char alt = SnpUtils.getAltFromMutationString(record.getMutation());
				final int altCount = SnpUtils.getCountFromNucleotideString(ND, alt);
				
				if (((float)altCount / normalCount) * 100 < 3.0f) {
					record.removeAnnotation(SnpUtils.MUTATION_IN_NORMAL);
					minCount++;
				}
			}
		}
		logger.info("no of records with " + SnpUtils.MUTATION_IN_NORMAL + " annotation removed: " + minCount);
	}
	
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
		final Rule normalRule = RulesUtil.getRule(normalRules, normalCoverage);
		final Rule tumourRule = RulesUtil.getRule(tumourRules, tumourCoverage);
		
//		boolean normalFirstPass = false;
//		boolean normalSecondPass = false;
//		boolean tumourFirstPass = false;
//		boolean tumourSecondPass = false;
		
		boolean [] normalPass = PileupElementLiteUtil.isAccumulatorAKeeper(normal,  ref, normalRule, baseQualityPercentage);
		boolean [] tumourPass = PileupElementLiteUtil.isAccumulatorAKeeper(tumour,  ref, tumourRule, baseQualityPercentage);
		
//		normalFirstPass = PileupElementLiteUtil.isAccumulatorAKeeperFirstPass(normal,  ref, normalRule, baseQualityPercentage);
//		
//		if ( ! normalFirstPass) {
//			normalSecondPass = PileupElementLiteUtil.isAccumulatorAKeeperSecondPass(normal,  ref, normalRule, baseQualityPercentage);
//			
//			if ( ! normalSecondPass) {
//				tumourFirstPass = PileupElementLiteUtil.isAccumulatorAKeeperFirstPass(tumour,  ref, tumourRule, baseQualityPercentage);
//				
//				if ( ! tumourFirstPass) {
//					tumourSecondPass = PileupElementLiteUtil.isAccumulatorAKeeperSecondPass(tumour,  ref, tumourRule, baseQualityPercentage);
//				}
//			}
//		}
		
		if (normalPass[0] || normalPass[1] || tumourPass[0] || tumourPass[1]) {
//			if (normalFirstPass || normalSecondPass || tumourFirstPass || tumourSecondPass) {
			
			// need to know what rule to use for tumour if we got here due to variant in normal 
//			if (normalFirstPass || normalSecondPass) {
//				tumourFirstPass = PileupElementLiteUtil.isAccumulatorAKeeperFirstPass(tumour,  ref, tumourRule, baseQualityPercentage);
//				
//				if ( ! tumourFirstPass) {
//					tumourSecondPass = PileupElementLiteUtil.isAccumulatorAKeeperSecondPass(tumour,  ref, tumourRule, baseQualityPercentage);
//				}
//			}
		
			String normalBases = null != normal ? normal.toSamtoolsPileupString(ref) : "";
			String tumourBases = null != tumour ? tumour.toSamtoolsPileupString(ref) : "";
			QSnpRecord qRecord = new QSnpRecord();
			qRecord.setPileup((null != normal ? normal.toPileupString(normalBases) : "") 
					+ "\t" + (null != tumour ? tumour.toPileupString(tumourBases) : ""));
			// setup some values on the record
			qRecord.setChromosome(currentChr);
			qRecord.setPosition(position);
			qRecord.setRef(ref);
			qRecord.setNormalCount(normalCoverage);
			qRecord.setTumourCount(tumourCoverage);
			
			// set normal pileup to only contain the different bases found in normal, rather than the whole pileup string
			// which contains special chars indicating start/end of reads along with mapping qualities
			if (normalCoverage > 0)
				qRecord.setNormalPileup(normal.getCompressedPileup());
			// use all base counts to form genotype
			qRecord.setNormalGenotype(null != normal ? normal.getGenotype(ref, normalRule, normalPass[1], baseQualityPercentage) : null);
			qRecord.setTumourGenotype(null != tumour ? tumour.getGenotype(ref, tumourRule, tumourPass[1], baseQualityPercentage) : null);
//			qRecord.setNormalGenotype(null != normal ? normal.getGenotype(ref, normalRule, normalSecondPass, baseQualityPercentage) : null);
//			qRecord.setTumourGenotype(null != tumour ? tumour.getGenotype(ref, tumourRule, tumourSecondPass, baseQualityPercentage) : null);
			
			qRecord.setNormalNucleotides(null != normal ? normal.getPileupElementString() : null);
			qRecord.setTumourNucleotides(null != tumour ? tumour.getPileupElementString() : null);
			// add unfiltered normal
			if (null != normal)
				qRecord.setUnfilteredNormalPileup(normal.getUnfilteredPileup());
			
			
			qRecord.setNormalNovelStartCount(PileupElementLiteUtil.getLargestVariantNovelStarts(normal, ref));
			qRecord.setTumourNovelStartCount(PileupElementLiteUtil.getLargestVariantNovelStarts(tumour, ref));
			
			classifyPileupRecord(qRecord);
			
			if (null != qRecord.getMutation()) {
				char alt =SnpUtils.getAltFromMutationString(qRecord.getMutation());
				
				// get and set cpg
				final char[] cpgCharArray = new char[11];
				for (int i = 0 ; i < 11 ; i++) {
					int refPosition = position - (6 - i);
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
					int normalAlt = null != normal ? normal.getBaseCountForBase(alt) : 0;		// a
					int normalRef = null != normal ? normal.getBaseCountForBase(ref) : 0;	// c
					int tumourAlt = null != tumour ? tumour.getBaseCountForBase(alt) : 0;		// b
					int tumourRef = null != tumour ? tumour.getBaseCountForBase(ref) : 0;	// d
					
//					if (normalAlt > 1000 || normalRef > 1000 || tumourAlt > 1000 || tumourRef > 1000) {
//						logger.info("normalAlt: " + normalAlt + ", normalRef: " + normalRef + ", tumourAlt: " + tumourAlt + ", tumourRef: "  + tumourRef);
//					}
					
					
					// don't run this if we have crazy large coverage - takes too long and holds up the other threads
					if (normalCoverage + tumourCoverage > 100000) {
						logger.info("skipping FisherExact pValue calculation - coverage too large: " + normalCoverage + ", tCov: " + tumourCoverage + ", at " + currentChr + ":" + position);
					} else {
						double pValue = FisherExact.getTwoTailedFETMath(normalAlt, tumourAlt, normalRef, tumourRef);
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
			positionRecordMap.put(new ChrPosition(qRecord.getChromosome(), qRecord.getPosition()), qRecord);
		}
	}
}
