/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp;

import java.io.File;
import java.io.IOException;
import java.util.Deque;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.ini4j.Ini;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.pileup.PileupFileReader;
import org.qcmg.pileup.QSnpRecord;
import org.qcmg.common.model.Classification;
import org.qcmg.snp.util.IniFileUtil;
import org.qcmg.snp.util.QJumperWorker;
import org.qcmg.snp.util.QJumperWorker.Mode;

/**
 */
@Deprecated
public final class PileupPipeline extends Pipeline {

	//input Files
	private String pileupFile;
	private String unfilteredNormalBamFile;
	

	/**
	 */
	@Deprecated
	public PileupPipeline(final Ini iniFile, QExec qexec, boolean singleSample) throws SnpException, IOException, Exception {
		super(qexec, singleSample);

		// load rules from ini file
		ingestIni(iniFile);
		
		checkRules();
		
		// populate the positions int arrays
		getStringPositions();

		// walk the pileup, keeping a count of all records, and those that pass
		// our initial tests
		logger.info("Loading Samtools mpileup data");
		walkPileup(pileupFile);
		
//		if (positionRecordMap.isEmpty()) throw new SnpException("EMPTY_PILEUP_FILE");
		
		logger.info("Loading Samtools mpileup data - DONE");
		
		// time for post-processing
//		classifyPileup();
		
		// STOP here if only outputting vcf
		
//			logger.info("perform pileup on unfiltered bam for any SOMATIC positions (that don't already see the mutation in the normal)");
			addUnfilteredBamPileup();
			
		writeVCF(vcfFile);
	}
		
	private void  addUnfilteredBamPileup() throws Exception{
		if (StringUtils.isNullOrEmpty(unfilteredNormalBamFile)) {
			logger.info("No unfiltered bam file provided");
			return;
		} else {
			logger.info("About to query unfiltered bam file");
		}
		
		long start = System.currentTimeMillis();
		int noOfThreads = 2;
		
		final CountDownLatch latch = new CountDownLatch(noOfThreads);
		final ExecutorService service = Executors.newFixedThreadPool(noOfThreads);
		
		// we want a submap of normal vcfs that don't have tumour entries
		Map<ChrPosition, QSnpRecord> somaticNoRecordOfMutationInNormal = new TreeMap<ChrPosition, QSnpRecord>();
//		for (Entry<ChrPosition, QSnpRecord> entry : positionRecordMap.entrySet()) {
//			QSnpRecord record = entry.getValue();
//			if (Classification.SOMATIC == record.getClassification()
//					&& (null == record.getAnnotation() 
//					|| ! record.getAnnotation().contains("mutation also found in pileup of normal"))) {
//				somaticNoRecordOfMutationInNormal.put(entry.getKey(), record);
//			}
//		}
		
		logger.info("number of SOMATIC snps that don't have evidence of mutation in normal: " + somaticNoRecordOfMutationInNormal.size());
		
		Deque<QSnpRecord> deque = new ConcurrentLinkedDeque<QSnpRecord>();
		for (QSnpRecord rec : somaticNoRecordOfMutationInNormal.values()) {
			deque.add(rec);
		}
		
		for (int i = 0 ; i < noOfThreads ; i++) {
			service.execute(new QJumperWorker<QSnpRecord>(latch,unfilteredNormalBamFile, 
					deque, Mode.QSNP_MUTATION_IN_NORMAL));
		}
		service.shutdown();
		
		try {
			latch.await();
			logger.info("QJumper threads finished in " + ((System.currentTimeMillis() - start)/1000) + " seconds");
		} catch (InterruptedException ie) {
			logger.error("InterruptedException caught",ie);
			Thread.currentThread().interrupt();
		}
	}
	
	@Override
	void ingestIni(Ini ini) throws SnpException {
		
		super.ingestIni(ini);
		
		// RULES
		controlRules = IniFileUtil.getRules(ini, "control");
		testRules = IniFileUtil.getRules(ini, "test");
		initialTestSumOfCountsLimit = IniFileUtil.getLowestRuleValue(ini);
		
		// ADDITIONAL INPUT FILES
		pileupFile = IniFileUtil.getInputFile(ini, "pileup");
		unfilteredNormalBamFile = IniFileUtil.getInputFile(ini, "unfilteredNormalBam");
		
		// ADDITIONAL SETUP	
		noOfControlFiles = IniFileUtil.getNumberOfFiles(ini, 'N');
		noOfTestFiles = IniFileUtil.getNumberOfFiles(ini, 'T');
		
		// INCLUDE INDELS
		String includeIndelsString = IniFileUtil.getEntry(ini, "parameters", "includeIndels"); 
		includeIndels = (null != includeIndelsString && "true".equalsIgnoreCase(includeIndelsString));
		
		// log values retrieved from ini file
		logger.tool("**** ADDITIONAL INPUT FILES ****");
		logger.tool("pileupFile: " + pileupFile);
		logger.tool("unfilteredNormalBamFile: " + unfilteredNormalBamFile);
		
		logger.tool("**** ADDITIONAL CONFIG ****");
		logger.tool("No of normal rules: " + controlRules.size());
		logger.tool("No of tumour rules: " + testRules.size());
		logger.tool("min coverage count for initial test: " + initialTestSumOfCountsLimit);
		logger.tool("number of normal files in pileup: " + noOfControlFiles);
		logger.tool("number of tumour files in pileup: " + noOfTestFiles);
		logger.tool("mutationIdPrefix: " + mutationIdPrefix);
	}
	
//	@Override
//	public String getOutputHeader(boolean isSomatic) {
//		if (isSomatic) return HeaderUtil.DCC_SOMATIC_HEADER_MINIMAL;
//		else return HeaderUtil.DCC_GERMLINE_HEADER_MINIMAL;
//	}
//	
//	@Override
//	public String getFormattedRecord(QSnpRecord record, final String ensemblChr) {
//		return record.getDCCData(mutationIdPrefix, ensemblChr);
//	}

	private void walkPileup(String pileupFileName) throws Exception {
		PileupFileReader reader = new PileupFileReader(new File(pileupFileName));
		long count = 0;
		try {
			for (String record : reader) {
//				parsePileup(record);
				if (++count % 1000000 == 0)
					logger.info("hit " + count + " pileup records, with " + mutationId + " keepers.");
			}
		} finally {
			reader.close();
		}
	}
	
}
