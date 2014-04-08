/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp;

import java.io.File;
import java.io.IOException;
import java.util.Deque;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.ini4j.Ini;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.pileup.PileupFileReader;
import org.qcmg.pileup.QSnpRecord;
import org.qcmg.pileup.QSnpRecord.Classification;
import org.qcmg.snp.util.HeaderUtil;
import org.qcmg.snp.util.IniFileUtil;
import org.qcmg.snp.util.QJumperWorker;
import org.qcmg.snp.util.QJumperWorker.Mode;

/**
 */
public final class PileupPipeline extends Pipeline {

	//input Files
	private String pileupFile;
	private String unfilteredNormalBamFile;
	
	private final static QLogger logger = QLoggerFactory.getLogger(PileupPipeline.class);

	/**
	 */
	public PileupPipeline(final Ini iniFile, QExec qexec) throws SnpException, IOException, Exception {
		super(qexec);

		// load rules from ini file
		ingestIni(iniFile);
		
		checkRules();
		
		// populate the positions int arrays
		getStringPositions();

		// walk the pileup, keeping a count of all records, and those that pass
		// our initial tests
		logger.info("Loading Samtools mpileup data");
		walkPileup(pileupFile);
		
		if (positionRecordMap.isEmpty()) throw new SnpException("EMPTY_PILEUP_FILE");
		
		logger.info("Loading Samtools mpileup data - DONE");
		
		// time for post-processing
		classifyPileup();
		
		if (null != dbSnpFile ) {
			logger.info("Loading dbSNP data");
			addDbSnpData(dbSnpFile);
		} else {
			logger.info("Skipping loading of dbSNP data - No dbSNP file specified");
		}
		
		
		
		// STOP here if only outputting vcf
		if ( ! vcfOnlyMode) {
		
//			logger.info("perform pileup on unfiltered bam for any SOMATIC positions (that don't already see the mutation in the normal)");
			addUnfilteredBamPileup();
			
			// load in IlluminaData - this will need to be appended to with dbSnp location details
			// when we go through the dbSnp file
			if ( ! StringUtils.isNullOrEmpty(illuminaNormalFile ) && ! StringUtils.isNullOrEmpty(illuminaTumourFile)) {
				logger.info("Loading illumina normal data");
				loadIlluminaData(illuminaNormalFile, normalIlluminaMap);
				logger.info("Loaded " + normalIlluminaMap.size() + " entries into the illumina map from file: " + illuminaNormalFile);
				logger.info("Loading illumina tumour data");
				loadIlluminaData(illuminaTumourFile, tumourIlluminaMap);
				logger.info("Loaded " + tumourIlluminaMap.size() + " entries into the illumina map from file: " + illuminaTumourFile);
			}
			
			if ( ! tumourIlluminaMap.isEmpty() && ! normalIlluminaMap.isEmpty())
				addIlluminaData();
			else 
				logger.info("No Illumina data will be used (files don't exist or are empty");
			
			// need chromosome conversion data
			logger.info("Loading chromosome conversion data");
			loadChromosomeConversionData(chrConvFile);
			
			// load GermlineDB data
			if ( ! StringUtils.isNullOrEmpty(germlineDBFile)) {
				logger.info("Loading germline DB data");
				addGermlineDBData(germlineDBFile);
				
//				if (updateGermlineDB) {
//					logger.info("updating germlineDB with germline snips");
//					updateGermlineDB(germlineDBFile);
//				}
			} else {
				logger.info("No GermlineDB data will be used (file doesn't exist or is empty)");
			}
			
			// write output
			writeOutputForDCC();
		}
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
		for (Entry<ChrPosition, QSnpRecord> entry : positionRecordMap.entrySet()) {
			QSnpRecord record = entry.getValue();
			if (Classification.SOMATIC == record.getClassification()
					&& (null == record.getAnnotation() 
					|| ! record.getAnnotation().contains("mutation also found in pileup of normal"))) {
				somaticNoRecordOfMutationInNormal.put(entry.getKey(), record);
			}
		}
		
		logger.info("number of SOMATIC snps that don't have evidence of mutation in normal: " + somaticNoRecordOfMutationInNormal.size());
		
		Deque<QSnpRecord> deque = new ConcurrentLinkedDeque<QSnpRecord>();
		for (QSnpRecord rec : somaticNoRecordOfMutationInNormal.values()) {
			deque.add(rec);
		}
		
		for (int i = 0 ; i < noOfThreads ; i++) {
			service.execute(new QJumperWorker<QSnpRecord>(latch,unfilteredNormalBamFile, 
					deque, Mode.QSNP_MUTATION_IN_NORMAL));
//			service.execute(new QJumperWorker(latch,unfilteredNormalBamFile, 
//					somaticNoRecordOfMutationInNormal, Mode.QSNP_MUTATION_IN_NORMAL));
//			service.execute(new QJumperWorker(latch,unfilteredNormalBamFile, unfilteredNormalBamIndexFile,
//					somaticNoRecordOfMutationInNormal, Mode.QSNP_MUTATION_IN_NORMAL));
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
		normalRules = IniFileUtil.getRules(ini, "normal");
		tumourRules = IniFileUtil.getRules(ini, "tumour");
		initialTestSumOfCountsLimit = IniFileUtil.getLowestRuleValue(ini);
		
		// MINIMUM BASE QUALITY
//		String baseQualString = IniFileUtil.getEntry(ini, "parameters", "minimumBaseQuality");
//		if (null != baseQualString)
//			minimumBaseQualityScore = Integer.parseInt(baseQualString);
		
		// ADDITIONAL INPUT FILES
		pileupFile = IniFileUtil.getInputFile(ini, "pileup");
		unfilteredNormalBamFile = IniFileUtil.getInputFile(ini, "unfilteredNormalBam");
		
		// ADDITIONAL SETUP	
//		mutationIdPrefix = patientId + "_SNP_";
		noOfNormalFiles = IniFileUtil.getNumberOfFiles(ini, 'N');
		noOfTumourFiles = IniFileUtil.getNumberOfFiles(ini, 'T');
		
		// INCLUDE INDELS
		String includeIndelsString = IniFileUtil.getEntry(ini, "parameters", "includeIndels"); 
		includeIndels = (null != includeIndelsString && "true".equalsIgnoreCase(includeIndelsString));
		
		// log values retrieved from ini file
		logger.tool("**** ADDITIONAL INPUT FILES ****");
		logger.tool("pileupFile: " + pileupFile);
		logger.tool("unfilteredNormalBamFile: " + unfilteredNormalBamFile);
		
		logger.tool("**** ADDITIONAL CONFIG ****");
		logger.tool("No of normal rules: " + normalRules.size());
		logger.tool("No of tumour rules: " + tumourRules.size());
//		logger.tool("Minimum Base Quality Score: " + minimumBaseQualityScore);
		logger.tool("min coverage count for initial test: " + initialTestSumOfCountsLimit);
		logger.tool("number of normal files in pileup: " + noOfNormalFiles);
		logger.tool("number of tumour files in pileup: " + noOfTumourFiles);
		logger.tool("mutationIdPrefix: " + mutationIdPrefix);
	}
	
//	private void checkOutputFiles() throws SnpException {
//		// loop through supplied files - check they can be read
//		String [] files = new String[] {qcmgPileupFile, dccSomaticFile, dccGermlineFile};
//		
//		for (String file : files) {
//			
//			if (null == file || ! FileUtils.canFileBeWrittenTo(file)) {
//				throw new SnpException("OUTPUT_FILE_WRITE_ERROR" , file);
//			}
//		}
//	}

	@Override
	public String getOutputHeader(boolean isSomatic) {
		if (isSomatic) return HeaderUtil.DCC_SOMATIC_HEADER_MINIMAL;
		else return HeaderUtil.DCC_GERMLINE_HEADER_MINIMAL;
	}
	
	@Override
	public String getFormattedRecord(QSnpRecord record, final String ensemblChr) {
		return record.getDCCData(mutationIdPrefix, ensemblChr);
	}

	private void walkPileup(String pileupFileName) throws Exception {
		PileupFileReader reader = new PileupFileReader(new File(pileupFileName));
		long count = 0;
		try {
			for (String record : reader) {
				parsePileup(record);
				if (++count % 1000000 == 0)
					logger.info("hit " + count + " pileup records, with " + mutationId + " keepers.");
			}
		} finally {
			reader.close();
		}
	}
	
}
