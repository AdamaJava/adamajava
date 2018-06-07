/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;

import org.ini4j.Ini;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.snp.util.IniFileUtil;

public class StandardPipeline extends Pipeline {
	
	
	public StandardPipeline(final Ini iniFile, QExec qexec, boolean singleSample) throws Exception {
		super(qexec, singleSample);
		
		ingestIni(iniFile);
		
		checkRules();
		
		loadNextReferenceSequence();
		
		checkBamHeaders();
		
		walkBams();
		
		examineData();
		
		// write output
		writeVCF(vcfFile);
	}
	
	/*
	 * want to print out some basic stats from the snps that we have collected
	 */
	private void examineData() {
		int compoundSnpCount = 0;
		int somaticCount = 0;
		int germlineCount = 0;
		int count = 0;
		Map<String, AtomicInteger> mutationMap = new HashMap<>(); 
		Map<String, AtomicInteger> genotypeMap = new HashMap<>(); 
		
		for (VcfRecord v : snps) {
			count++;
			String ref = v.getRef();
			if (ref.length() > 1) {
				compoundSnpCount++;
			}
			
			boolean isSomatic = VcfUtils.isRecordSomatic(v);
			if (isSomatic) {
				somaticCount++;
			} else {
				germlineCount++;
			}
			
			String refAlts = ref + "->" + v.getAlt();
			mutationMap.computeIfAbsent(refAlts, f -> new AtomicInteger()).incrementAndGet();
			
			List<String> ff = v.getFormatFields();
			String gt = "";
			for (int i = 1 ; i < ff.size() ; i++) {
				String f = ff.get(i); 
				if (gt.length() > 0) {
					gt += "->";
				}
				gt += f.substring(0, f.indexOf(Constants.COLON_STRING));
			}
			genotypeMap.computeIfAbsent(gt, f -> new AtomicInteger()).incrementAndGet();
		}
		
		
		logger.tool("Total number of snps: " + count + ", of which " + somaticCount + " were somatic, and " + germlineCount + " germline.");
		logger.tool("mutation distribution:");
		mutationMap.entrySet().stream().sorted(Comparator.comparingInt(e -> e.getValue().intValue())).forEach( e -> logger.tool("mutation: " + e.getKey() + " occurred " + e.getValue().intValue() + " times"));
		
		logger.tool("genotype distribution:");
		genotypeMap.entrySet().stream().sorted(Comparator.comparingInt(e -> e.getValue().intValue())).forEach( e -> logger.tool("genotype: " + e.getKey() + " occurred " + e.getValue().intValue() + " times"));
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
			logger.tool("number of records failing filter: " + noOfRecordsFailingFilter);
		}
	}

}
