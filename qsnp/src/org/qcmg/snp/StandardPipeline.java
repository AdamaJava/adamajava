/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp;

import org.ini4j.Ini;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.string.StringUtils;
import org.qcmg.pileup.QSnpRecord;
import org.qcmg.snp.util.HeaderUtil;
import org.qcmg.snp.util.IniFileUtil;

public class StandardPipeline extends Pipeline {
	
	
	public StandardPipeline(final Ini iniFile, QExec qexec) throws Exception {
		super(qexec);
		
		ingestIni(iniFile);
		
		checkRules();
		
		// need chromosome conversion data
		logger.info("Loading chromosome conversion data");
		loadChromosomeConversionData(chrConvFile);
		
		loadNextReferenceSequence();
		
		checkBamHeaders();
		
		walkBams();
		
		logger.info("No of records that have a genotype: " + classifyCount + ", out of " + positionRecordMap.size() 
				+ ". Somatic: " + classifySomaticCount + "[" + classifySomaticLowCoverage + "], germline: " 
				+ classifyGermlineCount + "[" + classifyGermlineLowCoverage + "] no classification: " + classifyNoClassificationCount + ", no mutation: " + classifyNoMutationCount);
		
		
		if ( ! StringUtils.isNullOrEmpty(dbSnpFile) ) {
			logger.info("Loading dbSNP data");
			try {
				addDbSnpData(dbSnpFile);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			logger.info("Skipping loading of dbSNP data - No dbSNP file specified");
		}
		
		incorporateUnfilteredNormal();
		
		// load in IlluminaData - this will need to be appended to with dbSnp location details
		// when we go through the dbSnp file
		if ( ! StringUtils.isNullOrEmpty(illuminaNormalFile ) && ! StringUtils.isNullOrEmpty(illuminaTumourFile)) {
			logger.info("Loading illumina SNP chip normal data");
			loadIlluminaData(illuminaNormalFile, normalIlluminaMap);
			logger.info("Loaded " + normalIlluminaMap.size() + " entries into the illumina map from file: " + illuminaNormalFile);
			logger.info("Loading illumina SNP chip tumour data");
			loadIlluminaData(illuminaTumourFile, tumourIlluminaMap);
			logger.info("Loaded " + tumourIlluminaMap.size() + " entries into the illumina map from file: " + illuminaTumourFile);
		}
		
		if ( ! tumourIlluminaMap.isEmpty() && ! normalIlluminaMap.isEmpty()) {
			try {
				addIlluminaData();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else { 
			logger.info("No Illumina SNP chip data will be used (files don't exist or are empty)");
		}
		
		
		// load GermlineDB data
		if ( ! StringUtils.isNullOrEmpty(germlineDBFile)) {
			logger.info("Loading germline DB data");
			addGermlineDBData(germlineDBFile);
			
		} else {
			logger.info("No GermlineDB data will be used (file doesn't exist or is empty)");
		}
		
//		populateProbabilities();
		
		// write output
		writeVCF(vcfFile);
		if ( ! vcfOnlyMode) writeOutputForDCC();
	}
	
	@Override
	protected String getFormattedRecord(final QSnpRecord record, final String ensemblChr) {
		return record.getDCCDataNSFlankingSeq(mutationIdPrefix, ensemblChr);
	}

	@Override
	protected String getOutputHeader(boolean isSomatic) {
		if (isSomatic) return HeaderUtil.DCC_SOMATIC_HEADER;
		else return HeaderUtil.DCC_GERMLINE_HEADER;
	}
	
	@Override
	void ingestIni(Ini ini) throws SnpException {
		
		super.ingestIni(ini);
		
		// RULES
		normalRules = IniFileUtil.getRules(ini, "normal");
		tumourRules = IniFileUtil.getRules(ini, "tumour");
		initialTestSumOfCountsLimit = IniFileUtil.getLowestRuleValue(ini);
		
		// ADDITIONAL INPUT FILES
		referenceFile = IniFileUtil.getInputFile(ini, "ref");
		
		// QBAMFILTER QUERY
		query =  IniFileUtil.getEntry(ini, "parameters", "filter");
		String sFailFilter = IniFileUtil.getEntry(ini, "parameters", "noOfRecordsFailingFilter");
		if (null != sFailFilter)
			noOfRecordsFailingFilter = Long.parseLong(sFailFilter);
		
		logger.tool("**** QBAMFILTER QUERY ****");
		logger.tool("query: " + query);
		
		if (null != query) {
			logger.tool("noOfRecordsFailingFilter: " + noOfRecordsFailingFilter);
		}
	}

}
