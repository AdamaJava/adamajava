/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp;

import htsjdk.samtools.SAMFileHeader;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.ini4j.Ini;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.common.model.Classification;
import org.qcmg.snp.util.GenotypeUtil;
import org.qcmg.snp.util.IniFileUtil;
import org.qcmg.snp.util.PipelineUtil;
import org.qcmg.vcf.VCFFileReader;


/**
 */
public final class VcfPipeline extends Pipeline {
	
	public final static Comparator<ChrPosition> CHR_COMPARATOR = new ChrPositionComparator();
	
	public static final String FF_NOT_ENOUGH_INFO = "GT\t./.";
	
	private final static QLogger logger = QLoggerFactory.getLogger(VcfPipeline.class);

	private static VcfHeader controlVcfHeader;
	private static VcfHeader testVcfHeader;
	
//	private final ConcurrentMap<ChrPosition, Accumulator> controlPileup = new ConcurrentHashMap<>(1024 * 1024 * 8);
//	private final ConcurrentMap<ChrPosition, Accumulator> testPileup = new ConcurrentHashMap<>(1024 * 1024 * 8);
	
	private ConcurrentMap<ChrPosition, VcfRecord> controlVCFMap = new ConcurrentHashMap<>(1024 * 1024 * 8); //not expecting more than 100000
	private ConcurrentMap<ChrPosition, VcfRecord> testVCFMap = new ConcurrentHashMap<>(1024 * 1024 * 8);
	
	//input Files
	private String controlVcfFile, testVcfFile;
//	private String controlBam;
//	private String testBam;
	
	/**
	 */
	public VcfPipeline(final Ini iniFile, QExec qexec, boolean singleSample) throws SnpException, IOException, Exception {
		super(qexec, singleSample);

		// load rules from ini file
		ingestIni(iniFile);
		
		// setup normalBam and tumourBam variables
//		if ( ! singleSampleMode) {
//			if (null != controlBams && controlBams.length > 0) {
//				controlBam = controlBams[0];
//			} else {
//				throw new SnpException("No control bam file specified");
//			}
//		
//		}
//		if (null != testBams && testBams.length > 0) {
//			testBam = testBams[0];
//		} else {
//			throw new SnpException("No test bam file specified");
//		}
		
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
			mergeControlAndTestVcfs();
			logger.info("merging data - DONE [" + snps.size() + "]");
		} else {
			
			/*
			 * add all test entries to snps, after adding and removing some fields to prepare for further processing 
			 */
			testVCFMap.values().stream().forEach(vcf -> {VcfUtils.prepareGATKVcfForMerge(vcf); snps.add(vcf);});
		}
		/*
		 * attempt tp clear some memory
		 */
		controlVCFMap.clear();
		controlVCFMap = null;
		testVCFMap.clear();
		testVCFMap = null;
		
		/*
		 * get headers from bam files, and sort snps according to order in bam header
		 */
		Comparator<VcfRecord> c;
		if (null != testBams && testBams.length > 0) {
			List<SAMFileHeader> th = getBamFileHeaders(testBams[0]);
			List<String> contigs = th.get(0).getSequenceDictionary().getSequences().stream().map(r -> r.getSequenceName()).collect(Collectors.toList());
			
			contigs.stream().forEach(s -> logger.info("contigs: " + s));
			c = ChrPositionComparator.getVcfRecordComparator(contigs);
		} else {
			c = ChrPositionComparator.getVcfRecordComparatorForGRCh37();
		}
		/*
		 * sort snps
		 */
		snps.sort(c);
		logger.info("Total number of snps (before compund snps has been run): "  + snps.size());
		
		// add pileup from the normal bam file
//		logger.info("adding pileup to vcf records map[" + snps.size() + "]");
//		addPileup();
//		logger.info("adding pileup to vcf records map - DONE[" + snps.size() + "]");
		
//		logger.info("about to populate accumulators");
//		populateAccumulators();
//		logger.info("about to populate accumulators - DONE [" + adjacentAccumulators.size() + "]");
		

		// time for post-processing
		logger.info("about to classify[" + snps.size() + "]");
		classifyPileup();
		logger.info("about to classify - DONE[" + snps.size() + "]");
		
		// compound snps!
		logger.info("about to do compound snps");
		compoundSnps();
		logger.info("about to do compound snps - DONE");
		
		// write output
		writeVCF(vcfFile);
	}
	
	@Override
	void compoundSnps() {
		List<List<VcfRecord>> loloSnps = PipelineUtil.listOfListOfAdjacentVcfs(snps);
		logger.info("Getting loloSnps [ " + loloSnps.size() + "] - DONE");
		
		if (loloSnps.isEmpty()) {
			return;
		}
		
		Set<VcfRecord> toRemove = new HashSet<>(1024 * 8);
		
		for (List<VcfRecord> loSnps : loloSnps) {
			
			Optional<VcfRecord> optionalVcf = PipelineUtil.createCompoundSnpGATK(loSnps, singleSampleMode);
			
			/*
			 * if present, add to compound snps collection and remove from snps collection, if not present, do nowt
			 */
			if (optionalVcf.isPresent()) {
				compoundSnps.add(optionalVcf.get());
				toRemove.addAll(loSnps);
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
	
	void classifyPileup() {
		for (VcfRecord v : snps) {
			String alts = v.getAlt();
			List<String> ff = v.getFormatFields();
			
			String cGT = null;
			String tGT =  null;
			
			if (singleSampleMode) {
				 tGT = ff.size() > 1 ? ff.get(1) : null;
			} else {
				cGT = ff.size() > 1 ? ff.get(1) : null;
				tGT = ff.size() > 2 ? ff.get(2) : null;
			}
			
			
			if (null != cGT) {
				cGT = cGT.split(Constants.COLON_STRING)[0];
				if (Constants.MISSING_GT.equals(cGT) || Constants.MISSING_DATA_STRING.equals(cGT)) {
					cGT = null;
				}
			}
			if (null != tGT) {
				tGT = tGT.split(Constants.COLON_STRING)[0];
				if (Constants.MISSING_GT.equals(tGT) || Constants.MISSING_DATA_STRING.equals(tGT)) {
					tGT = null;
				}
			}
			
			/*
			 * if cGT is null  set GT field to be "./."
			 */
			boolean controlNoCallInGATK = null == cGT;
			if (controlNoCallInGATK) {
				cGT = "0/0";
				updateGTFieldIFNoCall(v, 0, true);
			}
			boolean testNoCallInGATK = null == tGT;
			if (testNoCallInGATK) {
				tGT = "0/0";
				updateGTFieldIFNoCall(v, 0, false);
			}
			
			/*
			 * classify
			 */
			Classification c = Classification.UNKNOWN;
			if ( ! singleSampleMode) {
				c = GenotypeUtil.getClassification(null,  cGT,  tGT, alts);
			}
			
			
			String controlAdditionalFields = null;
			String testAdditionalFields = testNoCallInGATK ? ".:" + VcfHeaderUtils.FORMAT_NCIG : (Classification.SOMATIC == c ? ".:" + Classification.SOMATIC.toString() : ".:.");
			if ( ! singleSampleMode) {
				controlAdditionalFields = controlNoCallInGATK ? ".:" + VcfHeaderUtils.FORMAT_NCIG : ".:.";
			}
			
			
			String header = VcfHeaderUtils.FORMAT_FILTER + ":" + VcfHeaderUtils.FORMAT_INFO;
			
			/*
			 * Get additional format info
			 * If we are in single sample mode, only return data for the test sample 
			 */
			List<String> additionalFF = singleSampleMode ?  Arrays.asList(header, testAdditionalFields)
					:	Arrays.asList(header , controlAdditionalFields ,testAdditionalFields);
			
			VcfUtils.addFormatFieldsToVcf(v, additionalFF);
		}
	}
//	void classifyPileup() {
//		for (VcfRecord v : snps) {
//			Accumulator cAcc = controlPileup.get(v.getChrPosition());
//			Accumulator tAcc = testPileup.get(v.getChrPosition());
//			
//			String ref = v.getRef();
//			String alts = v.getAlt();
//			String [] aAlts = alts.split(Constants.COMMA_STRING);
//			
//			List<String> ff = v.getFormatFields();
//			
//			String cGT = ff.size() > 1 ? ff.get(1) : null;
//			String tGT = ff.size() > 2 ? ff.get(2) : null;
//			
//			if (null != cGT) {
//				cGT = cGT.split(Constants.COLON_STRING)[0];
//				if (Constants.MISSING_GT.equals(cGT) || Constants.MISSING_DATA_STRING.equals(cGT)) {
//					cGT = null;
//				}
//			}
//			if (null != tGT) {
//				tGT = tGT.split(Constants.COLON_STRING)[0];
//				if (Constants.MISSING_GT.equals(tGT) || Constants.MISSING_DATA_STRING.equals(tGT)) {
//					tGT = null;
//				}
//			}
//			
//			/*
//			 * if cGT is null and we have coverage, set GT field to be "0/0"
//			 * GATK didn't make a call, we have coverage, and so I think its fair to say that is is wildtype
//			 */
//			if (null == cGT) {
//				int refCov = null == cAcc ? 0 : cAcc.getBaseCountForBase(ref.charAt(0));
//				if (refCov > 3) {
//					cGT = "0/0";
//					int totalCov = null == cAcc ? 0 : cAcc.getCoverage();
//					updateGTFieldIFNoCall(v, totalCov, true);
//				}
//			}
//			if (null == tGT) {
//				int refCov = null == tAcc ? 0 : tAcc.getBaseCountForBase(ref.charAt(0));
//				if (refCov > 3) {
//					tGT = "0/0";
//					int totalCov = null == tAcc ? 0 : tAcc.getCoverage();
//					updateGTFieldIFNoCall(v, totalCov, false);
//				}
//			}
//			
//			/*
//			 * classify
//			 */
//			Classification c = GenotypeUtil.getClassification(null != cAcc ? cAcc.getCompressedPileup() : null,  cGT,  tGT, alts);
//			/*
//			 * If classification is somatic, add to info field for test sample
//			 */
////			if (Classification.SOMATIC == c) {
////				v.appendInfo(VcfHeaderUtils.INFO_SOMATIC);
////			}
//			
//			/*
//			 * Get additional format info
//			 */
//			List<String> cMrNns = GenotypeUtil.getMRandNNS(cGT, aAlts, cAcc); 
//			List<String> tMrNns = GenotypeUtil.getMRandNNS(tGT, aAlts, tAcc);
//			String cFT = GenotypeUtil.getFormatFilter(cAcc, cGT, aAlts,  ref.charAt(0), runSBIASAnnotation,  sBiasAltPercentage, sBiasCovPercentage, c, true);
//			String tFT = GenotypeUtil.getFormatFilter(tAcc, tGT, aAlts,  ref.charAt(0), runSBIASAnnotation,  sBiasAltPercentage, sBiasCovPercentage, c, false);
//			
//			List<String> additionalFF = Arrays.asList("FT:INF:MR:NNS:OABS"
//					, cFT + Constants.COLON + Constants.MISSING_DATA + Constants.COLON + cMrNns.get(0)+ Constants.COLON +cMrNns.get(1)+ Constants.COLON + (null != cAcc ? cAcc.getObservedAllelesByStrand() : Constants.MISSING_DATA)
//					,tFT + Constants.COLON + (Classification.SOMATIC == c ? Classification.SOMATIC : Constants.MISSING_DATA) + Constants.COLON +tMrNns.get(0)+ Constants.COLON +tMrNns.get(1)+ Constants.COLON + (null != tAcc ? tAcc.getObservedAllelesByStrand() : Constants.MISSING_DATA));
//			VcfUtils.addFormatFieldsToVcf(v, additionalFF);
//		}
//	}
	
	public static void updateGTFieldIFNoCall(VcfRecord v, int coverage, boolean isControl) {
		Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(v.getFormatFields());
		String [] gtArr = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE);
		if (null != gtArr) {
			int pos = isControl ? 0 : gtArr.length -1;
			/*
			 * update entry pos - should currently be null or missing data
			 */
			if (StringUtils.isNullOrEmptyOrMissingData(gtArr[pos])) {
				gtArr[pos] = "./.";
			}
		
			/*
			 * add entry to dp field while we are at it
			 */
			if (coverage > 0) {
				String [] dpArr = ffMap.get(VcfHeaderUtils.FORMAT_READ_DEPTH);
				/*
				 * update entry pos - should currently be null or missing data
				 */
				if (null != dpArr && StringUtils.isNullOrEmptyOrMissingData(dpArr[pos])) {
					dpArr[pos] = coverage +"";
				}
			}
			
			/*
			 * update vcf record
			 */
			v.setFormatFields(VcfUtils.convertFFMapToList(ffMap));
		}
	}
	
//	void populateAccumulators() {
//		for (VcfRecord vcf : snps) {
//			ChrPosition cp = vcf.getChrPosition();
//			// get entries from control and test maps
//			adjacentAccumulators.put(vcf, new Pair<>( controlPileup.get(cp), testPileup.get(cp)));
//		}
//	}
	
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
	
	
//	private void addPileup() throws Exception {
//		
//		logger.info("setting up pileup threads");
//		final long start = System.currentTimeMillis();
//		
//		final int noOfThreads = singleSampleMode ? 1 : 2;
//		
//		final CountDownLatch latch = new CountDownLatch(noOfThreads);
//		final ExecutorService service = Executors.newFixedThreadPool(noOfThreads);
//		
//		if ( ! singleSampleMode) {
//			service.execute(new Pileup(controlBam, latch, true, query));
//		}
//		service.execute(new Pileup(testBam, latch, false, query));
//		service.shutdown();
//		
//		try {
//			latch.await();
//			logger.info("pileup threads finished in " + ((System.currentTimeMillis() - start)/1000) + " seconds");
//		} catch (final InterruptedException ie) {
//			logger.error("InterruptedException caught",ie);
//			Thread.currentThread().interrupt();
//		}
//		
//	}
	
	private void mergeControlAndTestVcfs() {
		mergeVcfRecords(controlVCFMap, testVCFMap, snps);
//		for (Entry <ChrPosition, VcfRecord> cEntry : controlVCFMap.entrySet()) {
//			VcfRecord tVcf = testVCFMap.remove(cEntry.getKey());
//			VcfRecord cVcf = cEntry.getValue();
//			/*
//			 * control only
//			 */
//			if (null == tVcf) {
//				/*
//				 * add empty 
//				 */
//				VcfUtils.mergeGATKVcfRecs(cVcf, null).ifPresent(v -> snps.add(v));
//			} else {
//				VcfUtils.mergeGATKVcfRecs(cVcf, tVcf).ifPresent(v -> snps.add(v));
//			}
//		}
//		
//		for (VcfRecord vcf : testVCFMap.values()) {
//			VcfUtils.mergeGATKVcfRecs(null, vcf).ifPresent(v -> snps.add(v));
//		}
	}
	
	public static void mergeVcfRecords(Map<ChrPosition,VcfRecord> controlMap, Map<ChrPosition,VcfRecord> testMap, List<VcfRecord> vcfs) {
		for (Entry <ChrPosition, VcfRecord> cEntry : controlMap.entrySet()) {
			VcfRecord tVcf = testMap.remove(cEntry.getKey());
			VcfRecord cVcf = cEntry.getValue();
			/*
			 * control only
			 */
			if (null == tVcf) {
				/*
				 * add empty 
				 */
				VcfUtils.mergeGATKVcfRecs(cVcf, null).ifPresent(v -> vcfs.add(v));
			} else {
				VcfUtils.mergeGATKVcfRecs(cVcf, tVcf).ifPresent(v -> vcfs.add(v));
			}
		}
		
		for (VcfRecord vcf : testMap.values()) {
			VcfUtils.mergeGATKVcfRecs(null, vcf).ifPresent(v -> vcfs.add(v));
		}
	}

	private static void loadVCFData(String vcfFile, Map<ChrPosition,VcfRecord> map, boolean isControl) throws Exception {
		if (FileUtils.canFileBeRead(vcfFile)) {
			
			try (VCFFileReader reader  = new VCFFileReader(new File(vcfFile));) {
				if (isControl) {
					controlVcfHeader = reader.getHeader();
				} else {
					testVcfHeader = reader.getHeader();
				}
				for (final VcfRecord qpr : reader) {
					
					if (VcfUtils.isRecordAMnp(qpr)) {	// input file should be snps only
						map.put(qpr.getChrPosition(), qpr);
					}
				}
			}
		}
	}

	@Override
	protected void ingestIni(Ini ini) throws SnpException {
		
		super.ingestIni(ini);
		
		// RULES
		controlRules = IniFileUtil.getRules(ini, "control");
		testRules = IniFileUtil.getRules(ini, "test");

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

	/**
	 * Class that reads SAMRecords from a Queue and after checking that they satisfy some criteria 
	 * (ie. not duplicates, reasonable mapping quality) attempts to match the record against the current vcf record.
	 * If there is a match (ie. the sam record encompasses the vcf position, then a record of the base and strand at that position is kept.
	 * 
	 */
//	public class Pileup implements Runnable {
//		private final SamReader reader;
//		private QueryExecutor qbamFilter;
////		private final boolean isNormal;
//		private final ConcurrentMap<ChrPosition , Accumulator> pileupMap;
//		private int arraySize;
//		private int arrayPosition;
//		private ChrPosition cp;
//		private Comparator<String> chrNameComparator;
//		private final CountDownLatch latch;
//		private final boolean runqBamFilter;
//		
//		public Pileup(final String bamFile, final CountDownLatch latch, final boolean isNormal, String query) throws Exception {
////			this.isNormal = isNormal;
//			pileupMap = isNormal ? controlPileup : testPileup;
//			reader = SAMFileReaderFactory.createSAMFileReader(new File(bamFile));
//			if ( ! StringUtils.isNullOrEmpty(query) && ! "QCMG".equals(query))
//				qbamFilter = new QueryExecutor(query);
//			runqBamFilter = null != qbamFilter;
//			this.latch = latch;
//		}
//		
//		private void createComparatorFromSAMHeader(SamReader reader) {
//			final SAMFileHeader header = reader.getFileHeader();
//			
//			final List<String> sortedContigs = new ArrayList<>();
//			for (final SAMSequenceRecord contig : header.getSequenceDictionary().getSequences()) {
//				sortedContigs.add(contig.getSequenceName());
//			}
//			
//			chrNameComparator = ChrPositionComparator.getChrNameComparator(sortedContigs);
////			Comparator<ChrPosition> c = ChrPositionComparator.getComparator(chrNameComparator);
//			
//			arraySize = snps.size();
//		}
//		
//		private void advanceCPAndPosition() {
//			if (arrayPosition >= arraySize) {
//				cp = null;
//				return;
//			}
//			cp = snps.get(arrayPosition++).getChrPosition();
//		}
//		
//		
//		
//		private boolean match(SAMRecord rec, ChrPosition thisCPf, boolean updatePointer) {
//			
////			logger.info("in match with rec: " + rec + ", thisCPf: " + thisCPf + ", updatePointer: " + updatePointer);
//			
//			
//			if (null == thisCPf) return false;
//			if (rec.getReferenceName().equals(thisCPf.getChromosome())) {
////				logger.info("in match with ref names are equal");
//				
//				if (rec.getAlignmentEnd() < thisCPf.getStartPosition()) {
//					return false;
//				}
//				
//				if (rec.getAlignmentStart() <= thisCPf.getStartPosition()) {
//					return true;
//				}
//				
//				// finished with this cp - update results and get a new cp
//				if (updatePointer) {
//					advanceCPAndPosition();
//					return match(rec, cp, true);
//				} else {
//					return false;
//				}
//				
//				
//			} else if (chrNameComparator.compare(rec.getReferenceName(), thisCPf.getChromosome()) < 1) {
////				logger.info("in match with rec ref name < thisCPf chr");
//				// keep iterating through bam file 
//				return false;
//			} else {
//				logger.info("in match with rec ref name > thisCPf chr: " + cp.getChromosome() + Constants.COLON +  cp.getStartPosition() + ", rec: " + rec.getContig() + Constants.COLON + rec.getAlignmentStart());
//				if (updatePointer) {
//					// need to get next ChrPos
//					advanceCPAndPosition();
//					return match(rec, cp, true);
//				} else {
//					return false;
//				}
//			}
//		}
//		private void updateResults(ChrPosition cp, SAMRecord sam, int readId) {
//			// get read index
//			final int indexInRead = SAMUtils.getIndexInReadFromPosition(sam, cp.getStartPosition());
//			
//			if (indexInRead > -1 && indexInRead < sam.getReadLength()) {
//				/*
//				 * only proceed if we have met the min base quality requirements
//				 */
//				final byte baseQuality = sam.getBaseQualities()[indexInRead];
//				if (baseQuality >= minBaseQual) {
//				
//					Accumulator acc = pileupMap.get(cp);
//					if (null == acc) {
//						acc = new Accumulator(cp.getStartPosition());
//						final Accumulator oldAcc = pileupMap.putIfAbsent(cp, acc);
//						if (null != oldAcc) acc = oldAcc;
//					}
//					acc.addBase(sam.getReadBases()[indexInRead], baseQuality, ! sam.getReadNegativeStrandFlag(), 
//							sam.getAlignmentStart(), cp.getStartPosition(), sam.getAlignmentEnd(), readId);
//				}
//			}
//		}
//		
//		@Override
//		public void run() {
//			try {
//				createComparatorFromSAMHeader(reader);
//				// reset some key values
//				arrayPosition = 0;
//				cp = null;
//				// load first VCFRecord
//				advanceCPAndPosition();
//				
//				/*
//				 * if we don't have a cp - exit!
//				 */
//				if (cp != null) {
//				
//					long recordCount = 0;
//					
//					// don't think int overflow will affect us here.
//					// if we have more than the 2 billion records in the bam, then it will oveflow to be -ve, but that should be ok
//					// if we get more than 4 billion reads, it *should* go back to 0 - will test this
//					// and that should be ok because 
//					
//					int chrCounter = 0;
//					// take items off the queue and process
//					for (final SAMRecord sam : reader) {
//						chrCounter++;
//						if (++recordCount % 1000000 == 0) {
//							logger.info("processed " + recordCount/1000000 + "M records so far...");
//						}
//						
//						if (includeDuplicates) {
//							// we are in amplicon mode and so we want to keep dups - just check to see if we have the failed vendor flag set
//							if ( ! SAMUtils.isSAMRecordValid(sam)) continue;
//						} else {
//							// quality checks
//							if ( ! SAMUtils.isSAMRecordValidForVariantCalling(sam)) continue;
//						}
//						
//						try {
//							if (runqBamFilter && ! qbamFilter.Execute(sam)) {
//								continue;
//							}
//						} catch (Exception e) {
//							e.printStackTrace();
//							throw new RuntimeException("Exception caught whilst running qbamfilter");
//						}
//						
//						/*
//						 * need to ensure that the cp is on the same contig as the bam record.
//						 * Could cause SO if we have to tail recurse using advanceCPAndPosition
//						 * do this in a while for now.
//						 * May also need to advance the position as well... 
//						 * Assuming of course that the bam file is sorted...
//						 */
//						if (null == cp) break;
//						while (null != cp && chrNameComparator.compare(sam.getReferenceName(), cp.getChromosome()) > 1) {
//							advanceCPAndPosition();
//						}
//						
//						if (match(sam, cp, true)) {
//							updateResults(cp, sam, chrCounter);
//							
//							// get next cp and see if it matches
//							int j = 0;
//							if (arrayPosition < arraySize) {
//								ChrPosition tmpCP = snps.get(arrayPosition + j++).getChrPosition();
//								while (match(sam, tmpCP, false)) {
//									updateResults(tmpCP, sam, chrCounter);
//									if (arrayPosition + j < arraySize)
//										tmpCP = snps.get(arrayPosition + j++).getChrPosition();
//									else tmpCP = null;
//								}
//							}
//						}
//					}
//					logger.info("processed " + recordCount + " records");
//					
//					/*
//					 * make sure final record is updated
//					 */
//					advanceCPAndPosition();
//				}
//				
//			} finally {
//				try {
//					reader.close();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} finally {
//					latch.countDown();
//				}
//			}
//		}
//	}
}
