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
	
	private ConcurrentMap<ChrPosition, VcfRecord> controlVCFMap = new ConcurrentHashMap<>(1024 * 1024 * 8); //not expecting more than 100000
	private ConcurrentMap<ChrPosition, VcfRecord> testVCFMap = new ConcurrentHashMap<>(1024 * 1024 * 8);
	
	//input Files
	private String controlVcfFile, testVcfFile;
	
	/**
	 */
	public VcfPipeline(final Ini iniFile, QExec qexec, boolean singleSample) throws SnpException, IOException, Exception {
		super(qexec, singleSample);

		// load rules from ini file
		ingestIni(iniFile);
		
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
	
	private void mergeControlAndTestVcfs() {
		mergeVcfRecords(controlVCFMap, testVCFMap, snps);
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

}
