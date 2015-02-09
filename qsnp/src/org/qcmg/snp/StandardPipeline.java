/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp;

import org.ini4j.Ini;
import org.qcmg.common.meta.QExec;
import org.qcmg.pileup.QSnpRecord;
import org.qcmg.snp.util.HeaderUtil;
import org.qcmg.snp.util.IniFileUtil;

public class StandardPipeline extends Pipeline {
	
	
	public StandardPipeline(final Ini iniFile, QExec qexec, boolean singleSample) throws Exception {
		super(qexec, singleSample);
		
		ingestIni(iniFile);
		
		checkRules();
		
		loadNextReferenceSequence();
		
		checkBamHeaders();
		
		walkBams();
		
		logger.info("No of records that have a genotype: " + classifyCount + ", out of " + positionRecordMap.size() 
				+ ". Somatic: " + classifySomaticCount + "[" + classifySomaticLowCoverage + "], germline: " 
				+ classifyGermlineCount + "[" + classifyGermlineLowCoverage + "] no classification: " + classifyNoClassificationCount + ", no mutation: " + classifyNoMutationCount);
		
		incorporateUnfilteredNormal();
		
		compoundSnps();
		
		// write output
		writeVCF(vcfFile);
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
		controlRules = IniFileUtil.getRules(ini, "control");
		testRules = IniFileUtil.getRules(ini, "test");
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
