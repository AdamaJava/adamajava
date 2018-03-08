/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.ini4j.Ini;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QDccMeta;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.Accumulator;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.model.PileupElementLite;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.Pair;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.util.QDccMetaFactory;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.pileup.QSnpRecord;
import org.qcmg.common.model.Classification;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.snp.util.HeaderUtil;
import org.qcmg.snp.util.IniFileUtil;
import org.qcmg.vcf.VCFFileReader;


/**
 */
public final class VcfPipeline extends Pipeline {
	
	private final static ReferenceNameComparator COMPARATOR = new ReferenceNameComparator();
	public final static Comparator<ChrPosition> CHR_COMPARATOR = new ChrPositionComparator();
	
	private final static QLogger logger = QLoggerFactory.getLogger(VcfPipeline.class);

	private static VcfHeader controlVcfHeader;
	private static VcfHeader testVcfHeader;
	
	private final ConcurrentMap<ChrPosition, Accumulator> controlPileup = new ConcurrentHashMap<>();
	private final ConcurrentMap<ChrPosition, Accumulator> testPileup = new ConcurrentHashMap<>();
	
	private final ConcurrentMap<ChrPosition, Accumulator> controlPileupCS = new ConcurrentHashMap<>();
	private final ConcurrentMap<ChrPosition, Accumulator> testPileupCS = new ConcurrentHashMap<>();

	private final ConcurrentMap<ChrPosition, QSnpRecord> controlVCFMap = new ConcurrentHashMap<>(); //not expecting more than 100000
	private final ConcurrentMap<ChrPosition, QSnpRecord> testVCFMap = new ConcurrentHashMap<>(1024 * 128);
	
	//input Files
	private String controlVcfFile, testVcfFile;
	private String controlBam;
	private String testBam;
	private int mutationId;
	
	/**
	 */
	public VcfPipeline(final Ini iniFile, QExec qexec, boolean singleSample) throws SnpException, IOException, Exception {
		super(qexec, singleSample);

		// load rules from ini file
		ingestIni(iniFile);
		
		// setup normalBam and tumourBam variables
		if ( ! singleSampleMode) {
			if (null != controlBams && controlBams.length > 0) {
				controlBam = controlBams[0];
			} else {
				throw new SnpException("No normal bam file specified");
			}
		
		}
		if (null != testBams && testBams.length > 0) {
			testBam = testBams[0];
		} else {
			throw new SnpException("No tumour bam file specified");
		}
		
		// load vcf files
		if ( ! singleSampleMode) {
			logger.info("loading control vcf data");
			loadVCFData(controlVcfFile, controlVCFMap, true);
			logger.info("loading control vcf data - DONE [" + controlVCFMap.size() + "]");
		}
		
		logger.info("loading test vcf data");
		loadVCFData(testVcfFile, testVCFMap, false);
		logger.info("loading test vcf data - DONE [" + testVCFMap.size() + "]");
		
		if (controlVCFMap.isEmpty() && testVCFMap.isEmpty()) {
			throw new SnpException("EMPTY_VCF_FILES");
		} else if (controlVCFMap.isEmpty()) {
			logger.warn("no data in control vcf file");
		} else if (testVCFMap.isEmpty()) {
			logger.warn("no data in test vcf file");
		}
		
		if ( ! singleSampleMode) {
			logger.info("merging data");
			mergeControlAndTestRecords();
			logger.info("merging data - DONE [" + positionRecordMap.size() + "]");
		} else {
			// put test map into positions map
			positionRecordMap.putAll(testVCFMap);
		}
		
		
		// identify potential compound snps up from and only store accumulators for those positions
		logger.info("about to preidentify compound snps");
		preIdentifyCompoundSnps();
		logger.info("about to preidentify compound snps - DONE [" + adjacentAccumulators.size() + "]");
		
		// add pileup from the normal bam file
		logger.info("adding pileup to vcf records map[" + positionRecordMap.size() + "]");
		addPileup();
		logger.info("adding pileup to vcf records map - DONE[" + positionRecordMap.size() + "]");
		
		logger.info("about to populate accumulators");
		populateAccumulators();
		logger.info("about to populate accumulators - DONE [" + adjacentAccumulators.size() + "]");
		
		logger.info("about to clean snp map[" + positionRecordMap.size() + "]");
		cleanSnpMap();
		logger.info("about to clean snp map - DONE[" + positionRecordMap.size() + "]");

		// time for post-processing
		logger.info("about to classify[" + positionRecordMap.size() + "]");
		classifyPileup();
		logger.info("about to classify - DONE[" + positionRecordMap.size() + "]");
		
		
		strandBiasCorrection();
		
		// compound snps!
		logger.info("about to do compound snps");
		compoundSnps();
		logger.info("about to do compound snps - DONE");
		
		// write output
		writeVCF(vcfFile);

	}
	
	void populateAccumulators() {
		
		List<ChrPosition> orderedCPs = new ArrayList<ChrPosition>(positionRecordMap.keySet());
		
		for (ChrPosition cp : orderedCPs) {
			
			// get entries from control and test maps
			Accumulator control = controlPileupCS.get(cp);
			Accumulator test = testPileupCS.get(cp);
			
			adjacentAccumulators.put(cp, new Pair<Accumulator, Accumulator>(control, test));
		}
	}
	
	void preIdentifyCompoundSnps() {
		// populate the accumulators collections - used by the compoundSnps()
		ChrPosition previousCP = null;
		List<ChrPosition> orderedCPs = new ArrayList<ChrPosition>(positionRecordMap.keySet());
		Collections.sort(orderedCPs, CHR_COMPARATOR);
		
		for (ChrPosition cp : orderedCPs) {
			if (null != previousCP) {
				if (ChrPositionUtils.areAdjacent(previousCP, cp)) {
//					logger.info("in preIdentifyCompoundSnps with potential adjacent snps: " + previousCP.toString() + " and " + cp.toString());
					// add to accumulators
					adjacentAccumulators.put(previousCP, new Pair<Accumulator,Accumulator>(null, null));
					adjacentAccumulators.put(cp, new Pair<Accumulator,Accumulator>(null, null));
				}
			}
			previousCP = cp;
		}
	}
	
	@Override
	VcfHeader getExistingVCFHeaderDetails()  {
		VcfHeader existingHeader = new VcfHeader();
		
		if ( ! singleSampleMode) {
			for (VcfHeaderRecord rec : controlVcfHeader.getInfoRecords()) {
				existingHeader.addOrReplace(rec);  
			}
			for (VcfHeaderRecord rec : controlVcfHeader.getFormatRecords()) {
				existingHeader.addOrReplace(rec);
			}
			for (VcfHeaderRecord rec : controlVcfHeader.getFilterRecords()) {
				existingHeader.addOrReplace(rec);
			}
			
			// add in the vcf filename, gatk version and the uuid
			existingHeader.addOrReplace(VcfHeaderUtils.STANDARD_CONTROL_VCF + Constants.EQ + controlVcfFile);
			existingHeader.addOrReplace(VcfHeaderUtils.STANDARD_CONTROL_VCF_UUID + Constants.EQ + VcfHeaderUtils.getUUIDFromHeaderLine(controlVcfHeader.getUUID()));
			existingHeader.addOrReplace(VcfHeaderUtils.STANDARD_CONTROL_VCF_GATK_VER + Constants.EQ + VcfHeaderUtils.getGATKVersionFromHeaderLine(controlVcfHeader));
			
		}
		
		for (VcfHeaderRecord rec : testVcfHeader.getInfoRecords()) {
			existingHeader.addOrReplace(rec);
		}
		for (VcfHeaderRecord rec : testVcfHeader.getFormatRecords()) {
			existingHeader.addOrReplace(rec);
		}
		for (VcfHeaderRecord rec : testVcfHeader.getFilterRecords()) {
			existingHeader.addOrReplace(rec);
		}
		// add in the vcf filename, gatk version and the uuid
		existingHeader.addOrReplace(VcfHeaderUtils.STANDARD_TEST_VCF + Constants.EQ + testVcfFile);
		existingHeader.addOrReplace(VcfHeaderUtils.STANDARD_TEST_VCF_UUID + Constants.EQ + VcfHeaderUtils.getUUIDFromHeaderLine(testVcfHeader.getUUID()));
		existingHeader.addOrReplace(VcfHeaderUtils.STANDARD_TEST_VCF_GATK_VER + Constants.EQ + VcfHeaderUtils.getGATKVersionFromHeaderLine(testVcfHeader));
		
		// override this if dealing with input VCFs and the existing headers are to be kept
		return existingHeader;
	}

	
	@Override
	String getDccMetaData() throws Exception {
		if (null == controlBams || controlBams.length == 0 || 
				StringUtils.isNullOrEmpty(controlBams[0]) 
				|| null == testBams || testBams.length == 0 
				|| StringUtils.isNullOrEmpty(testBams[0])) return null;
		
		final SAMFileHeader controlHeader = SAMFileReaderFactory.createSAMFileReader(new File(controlBams[0])).getFileHeader();
		final SAMFileHeader analysisHeader = SAMFileReaderFactory.createSAMFileReader(new File(testBams[0])).getFileHeader();
		
		final QDccMeta dccMeta = QDccMetaFactory.getDccMeta(qexec, controlHeader, analysisHeader, "GATK");
		
		return dccMeta.getDCCMetaDataToString();
	}
	
		
	private void cleanSnpMap() {
		logger.info("about to clean the map");
		int normalUpdated = 0, tumourUpdated = 0;
		for (final QSnpRecord record : positionRecordMap.values()) {
			// if entry doesn't have a normal genotype, but contains a normal nucleotides string
			// set normal genotype to hom ref
			if (null == record.getNormalGenotype() && null != record.getNormalNucleotides()) {
				final String refString = record.getRef();
				if (refString.length() > 1) {
					logger.warn("refString: " + refString + " in VcfPipeline.cleanSnpMap");
				}
				final char ref = refString.charAt(0);
				record.setNormalGenotype(GenotypeEnum.getGenotypeEnum(ref, ref));
				normalUpdated++;
			}
			
			// if we don't have a tumour vcf call, just classify as Germline, annotate and off we go  
			if ( ! singleSampleMode && null == record.getTumourGenotype()) {
				record.setClassification(Classification.GERMLINE);
				VcfUtils.updateFilter(record.getVcfRecord(), SnpUtils.NO_CALL_IN_TEST);
				
				tumourUpdated++;
			}
		}
		
		logger.info("added genotypes (hom ref) for " + normalUpdated + " normals, and set classification to GERMLINE for " + tumourUpdated + " tumours");
	}

	private void addPileup() throws Exception {
		
		logger.info("setting up pileup threads");
		final long start = System.currentTimeMillis();
		
		final int noOfThreads = singleSampleMode ? 1 : 2;
		
		final CountDownLatch latch = new CountDownLatch(noOfThreads);
		final ExecutorService service = Executors.newFixedThreadPool(noOfThreads);
		
		if ( ! singleSampleMode) {
			service.execute(new Pileup(controlBam, latch, true, query));
		}
		service.execute(new Pileup(testBam, latch, false, query));
		service.shutdown();
		
		try {
			latch.await();
			logger.info("pileup threads finished in " + ((System.currentTimeMillis() - start)/1000) + " seconds");
		} catch (final InterruptedException ie) {
			logger.error("InterruptedException caught",ie);
			Thread.currentThread().interrupt();
		}
		
		
	}
	
	
	private void mergeControlAndTestRecords() {
		
		// loop through normal vcf map - get corresponding tumour if present
		for (final Entry<ChrPosition, QSnpRecord> entry : controlVCFMap.entrySet()) {
			final QSnpRecord tumour = testVCFMap.get(entry.getKey());
			positionRecordMap.put(entry.getKey(), getQSnpRecord(entry.getValue(), tumour));
		}
		
		// loop through tumour, making sure entry exists in qsnp map
		for (final Entry<ChrPosition, QSnpRecord> entry : testVCFMap.entrySet()) {
			
			final QSnpRecord snpRecord = positionRecordMap.get(entry.getKey());
			if (null == snpRecord) {
				positionRecordMap.put(entry.getKey(), getQSnpRecord(null, entry.getValue()));
			}
		}
	}
	
	QSnpRecord getQSnpRecord(QSnpRecord normal, QSnpRecord tumour) {
		QSnpRecord qpr = null;
		
		//TODO need to merge the underlying VcfRecords should both exist
		
		if (null != normal && null != tumour) {
			// create new VcfRecord
			VcfRecord mergedVcf = normal.getVcfRecord();
			// add filter and format fields
			mergedVcf.addFilter(tumour.getVcfRecord().getFilter());
			
			VcfUtils.addAdditionalSampleToFormatField(mergedVcf, tumour.getVcfRecord().getFormatFields());
			
//			List<String> formatFields = mergedVcf.getFormatFields();
//			formatFields.add(tumour.getVcfRecord().getFormatFields().get(1));
//			mergedVcf.setFormatFields(formatFields);
			
			// create new QSnpRecord with the mergedVcf details
			qpr = new QSnpRecord(mergedVcf);
			qpr.setNormalNucleotides(normal.getNormalNucleotides());
			qpr.setNormalGenotype(normal.getNormalGenotype());
			qpr.setTumourNucleotides(tumour.getTumourNucleotides());
			qpr.setTumourGenotype(tumour.getTumourGenotype());
			
			
		} else if (null != normal) {
			qpr = normal;
			// need to add a format field entry for the empty tumoursample
			VcfUtils.addMissingDataToFormatFields(qpr.getVcfRecord(), 2);
		} else {
			// tumour only
			qpr = tumour;
			VcfUtils.addMissingDataToFormatFields(qpr.getVcfRecord(), 1);
		}
		
		qpr.setId(++mutationId);
		return qpr;
	}

	private static void loadVCFData(String vcfFile, Map<ChrPosition,QSnpRecord> map, boolean isControl) throws Exception {
		if (FileUtils.canFileBeRead(vcfFile)) {
			
			try (VCFFileReader reader  = new VCFFileReader(new File(vcfFile));) {
				if (isControl) {
					controlVcfHeader = reader.getHeader();
				} else {
					testVcfHeader = reader.getHeader();
				}
				for (final VcfRecord qpr : reader) {
					
					
					
//					if (SnpUtils.PASS.equals(qpr.getFilter())) {	// only deal with top notch snps
					
					
					// if the length of the reference bases != length of the alt bases, its not a snp (or compound snp)
					if (VcfUtils.isRecordAMnp(qpr)) {	// input file should be snps only
						final QSnpRecord snpRecord = new QSnpRecord(qpr);
						if (null != qpr.getFormatFields() && ! qpr.getFormatFields().isEmpty()) {
							//	 set genotype
							if (isControl) {
								snpRecord.setNormalNucleotides(Constants.MISSING_DATA_STRING);
//								snpRecord.setNormalNucleotides(VcfUtils.getGenotypeFromGATKVCFRecord(qpr));
								snpRecord.setNormalGenotype(VcfUtils.getGEFromGATKVCFRec(qpr));
							} else {
								snpRecord.setTumourNucleotides(Constants.MISSING_DATA_STRING);
//								snpRecord.setTumourNucleotides(VcfUtils.getGenotypeFromGATKVCFRecord(qpr));
								snpRecord.setTumourGenotype(VcfUtils.getGEFromGATKVCFRec(qpr));
							}
						}
						map.put(qpr.getChrPosition() , snpRecord);
					}
//					map.put(new ChrPosition(qpr.getChromosome(), qpr.getPosition()),new QSnpGATKRecord(qpr));
				}
			}
		}
	}

	@Override
	protected void ingestIni(Ini ini) throws SnpException {
		
		super.ingestIni(ini);

		// ADDITIONAL INPUT FILES
		controlVcfFile = IniFileUtil.getInputFile(ini, "controlVcf");
		testVcfFile = IniFileUtil.getInputFile(ini, "testVcf");
		
		// log values retrieved from in file
		logger.tool("**** ADDITIONAL INPUT FILES ****");
		logger.tool("control vcf file: " + controlVcfFile);
		logger.tool("test vcf file: " + testVcfFile);
		
		logger.tool("**** OTHER CONFIG ****");
		logger.tool("mutationIdPrefix: " + mutationIdPrefix);
	}

	@Override
	protected String getFormattedRecord(QSnpRecord record, final String ensemblChr) {
		return record.getDCCDataNSFlankingSeq(mutationIdPrefix, ensemblChr);
	}

	@Override
	protected String getOutputHeader(boolean isSomatic) {
		if (isSomatic) return HeaderUtil.DCC_SOMATIC_HEADER;
		else return HeaderUtil.DCC_GERMLINE_HEADER;
	}
	
	public static  void updateQsnpRecord(ChrPosition chrPos, QSnpRecord rec, Accumulator acc, boolean normal) {
		final String refString = rec.getRef();
		if (refString.length() > 1) {
			logger.warn("refString: " + refString + " in VcfPipeline.cleanSnpMap");
		}
		final char ref = refString.charAt(0);
		
		final PileupElementLite pel = acc.getLargestVariant(ref);
		if (normal) {
			rec.setNormalNucleotides(acc.getPileupElementString());
			rec.setNormalCount(acc.getCoverage());
			rec.setNormalPileup(acc.getPileup());
			rec.setNormalNovelStartCount(null != pel ? pel.getNovelStartCount() : 0);
		} else {
			// tumour fields
			rec.setTumourCount(acc.getCoverage());
			rec.setTumourNucleotides(acc.getPileupElementString());
			rec.setTumourNovelStartCount(null != pel ? pel.getNovelStartCount() : 0);
		}
	}
	
	/**
	 * Class that reads SAMRecords from a Queue and after checking that they satisfy some criteria 
	 * (ie. not duplicates, reasonable mapping quality) attempts to match the record against the current vcf record.
	 * If there is a match (ie. the sam record encompasses the vcf position, then a record of the base and strand at that position is kept.
	 * 
	 */
	public class Pileup implements Runnable {
		private final SamReader reader;
		private QueryExecutor qbamFilter;
		private final boolean isNormal;
		private final ConcurrentMap<ChrPosition , Accumulator> pileupMap;
		private int arraySize;
		private int arrayPosition;
		private ChrPosition cp;
		private Comparator<String> chrNameComparator;
		private final List<ChrPosition> snps;
		private final CountDownLatch latch;
		private final boolean runqBamFilter;
		
		public Pileup(final String bamFile, final CountDownLatch latch, final boolean isNormal, String query) throws Exception {
			this.isNormal = isNormal;
			pileupMap = isNormal ? controlPileup : testPileup;
			reader = SAMFileReaderFactory.createSAMFileReader(new File(bamFile));
			snps = new ArrayList<ChrPosition>(positionRecordMap.keySet());
			if ( ! StringUtils.isNullOrEmpty(query) && ! "QCMG".equals(query))
				qbamFilter = new QueryExecutor(query);
			runqBamFilter = null != qbamFilter;
			this.latch = latch;
		}
		
		private void createComparatorFromSAMHeader(SamReader reader) {
			final SAMFileHeader header = reader.getFileHeader();
			
			final List<String> sortedContigs = new ArrayList<String>();
			for (final SAMSequenceRecord contig : header.getSequenceDictionary().getSequences()) {
				sortedContigs.add(contig.getSequenceName());
			}
			
			// try and sort according to the ordering of the bam file that is about to be processed
			// otherwise, resort to alphabetic ordering and cross fingers...
			
			chrNameComparator = ChrPositionComparator.getChrNameComparator(sortedContigs);
			Comparator<ChrPosition> c = ChrPositionComparator.getComparator(chrNameComparator);
			snps.sort(c);
			arraySize = snps.size();
		}
		
		private void advanceCPAndPosition() {
			if (arrayPosition >= arraySize) {
//				logger.info("in advanceCPAndPosition - arrayPosition > arraySize, arrayPosition: " + arrayPosition + ",  arraySize: " +arraySize);
				// reached the end of the line
				
				/*
				 * check to see if we still have this acc in the map, and update if we do
				 */
				if (null != cp && pileupMap.containsKey(cp)) {
					Accumulator acc = pileupMap.remove(cp);
					if (null != acc) {
						updateCompoundSnpDetails(cp, acc);
						updateQsnpRecord(cp, positionRecordMap.get(cp), acc, isNormal);
					}
				}
				cp = null;
				return;
			}
			if (null != cp) {
				// update QSnpRecord with our findings
				final Accumulator acc = pileupMap.remove(cp);
				if (null != acc) {
					updateCompoundSnpDetails(cp, acc);
					updateQsnpRecord(cp, positionRecordMap.get(cp), acc, isNormal);
//					logger.info("in advanceCPAndPosition -null != acc");
					
					// if position is a potential CS, keep accumulation data
				}
			}
			cp = snps.get(arrayPosition++);
		}
		
		private void updateCompoundSnpDetails(ChrPosition chrPos, Accumulator acc) {
			if (adjacentAccumulators.containsKey(chrPos)) {
				if (isNormal) {
					controlPileupCS.put(chrPos, acc);
				} else {
					testPileupCS.put(chrPos, acc);
				}
			}
		}
		
		
		private boolean match(SAMRecord rec, ChrPosition thisCPf, boolean updatePointer) {
			
//			logger.info("in match with rec: " + rec + ", thisCPf: " + thisCPf + ", updatePointer: " + updatePointer);
			
			
			if (null == thisCPf) return false;
			if (rec.getReferenceName().equals(thisCPf.getChromosome())) {
//				logger.info("in match with ref names are equal");
				
				if (rec.getAlignmentEnd() < thisCPf.getStartPosition()) {
					return false;
				}
				
				if (rec.getAlignmentStart() <= thisCPf.getStartPosition()) {
					return true;
				}
				
				// finished with this cp - update results and get a new cp
				if (updatePointer) {
					advanceCPAndPosition();
					return match(rec, cp, true);
				} else {
					return false;
				}
				
				
			} else if (chrNameComparator.compare(rec.getReferenceName(), thisCPf.getChromosome()) < 1) {
//				logger.info("in match with rec ref name < thisCPf chr");
				// keep iterating through bam file 
				return false;
			} else {
//				logger.info("in match with rec ref name > thisCPf chr");
				if (updatePointer) {
					// need to get next ChrPos
					advanceCPAndPosition();
					return match(rec, cp, true);
				} else {
					return false;
				}
			}
		}
		private void updateResults(ChrPosition cp, SAMRecord sam, int readId) {
			// get read index
			final int indexInRead = SAMUtils.getIndexInReadFromPosition(sam, cp.getStartPosition());
			
			
			if (indexInRead > -1 && indexInRead < sam.getReadLength()) {
				
				/*
				 * only proceed if we have met the min base quality requirements
				 */
				final byte baseQuality = sam.getBaseQualities()[indexInRead];
				if (baseQuality >= minBaseQual) {
				
					Accumulator acc = pileupMap.get(cp);
					if (null == acc) {
						acc = new Accumulator(cp.getStartPosition());
						final Accumulator oldAcc = pileupMap.putIfAbsent(cp, acc);
						if (null != oldAcc) acc = oldAcc;
					}
					acc.addBase(sam.getReadBases()[indexInRead], baseQuality, ! sam.getReadNegativeStrandFlag(), 
							sam.getAlignmentStart(), cp.getStartPosition(), sam.getAlignmentEnd(), readId);
				}
			}
		}
		
		@Override
		public void run() {
			try {
				createComparatorFromSAMHeader(reader);
				// reset some key values
				arrayPosition = 0;
				cp = null;
				// load first VCFRecord
				advanceCPAndPosition();
				
				/*
				 * if we don't have a cp - exit!
				 */
				if (cp != null) {
				
					long recordCount = 0;
					
					// don't think int overflow will affect us here.
					// if we have more than the 2 billion records in the bam, then it will oveflow to be -ve, but that should be ok
					// if we get more than 4 billion reads, it *should* go back to 0 - will test this
					// and that should be ok because 
					
					int chrCounter = 0;
					// take items off the queue and process
					for (final SAMRecord sam : reader) {
						chrCounter++;
						if (++recordCount % 1000000 == 0) {
							logger.info("processed " + recordCount/1000000 + "M records so far...");
						}
						
						if (includeDuplicates) {
							// we are in amplicon mode and so we want to keep dups - just check to see if we have the failed vendor flag set
							if ( ! SAMUtils.isSAMRecordValid(sam)) continue;
						} else {
							// quality checks
							if ( ! SAMUtils.isSAMRecordValidForVariantCalling(sam)) continue;
						}
						
						try {
							if (runqBamFilter && ! qbamFilter.Execute(sam)) {
								continue;
							}
						} catch (Exception e) {
							e.printStackTrace();
							throw new RuntimeException("Exception caught whilst running qbamfilter");
						}
						
						/*
						 * need to ensure that the cp is on the same contig as the bam record.
						 * Could cause SO if we have to tail recurse using advanceCPAndPosition
						 * do this in a while for now.
						 * May also need to advance the position as well... 
						 * Assuming of course that the bam file is sorted...
						 */
						if (null == cp) break;
						while (null != cp && chrNameComparator.compare(sam.getReferenceName(), cp.getChromosome()) > 1) {
							advanceCPAndPosition();
						}
						
						if (match(sam, cp, true)) {
							updateResults(cp, sam, chrCounter);
							
							// get next cp and see if it matches
							int j = 0;
							if (arrayPosition < arraySize) {
								ChrPosition tmpCP = snps.get(arrayPosition + j++);
								while (match(sam, tmpCP, false)) {
									updateResults(tmpCP, sam, chrCounter);
									if (arrayPosition + j < arraySize)
										tmpCP = snps.get(arrayPosition + j++);
									else tmpCP = null;
								}
							}
						}
					}
					logger.info("processed " + recordCount + " records");
					
					/*
					 * make sure final record is updated
					 */
					advanceCPAndPosition();
				}
				
			} finally {
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					latch.countDown();
				}
			}
		}
	}
}