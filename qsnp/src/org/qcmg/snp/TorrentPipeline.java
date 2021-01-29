/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp;

import java.io.IOException;
import org.ini4j.Ini;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;

import org.qcmg.snp.util.IniFileUtil;


/**
 */
@Deprecated
public final class TorrentPipeline extends Pipeline {
		
	final static QLogger logger = QLoggerFactory.getLogger(TorrentPipeline.class);

	/**
	 */
	@Deprecated
	public TorrentPipeline(final Ini iniFile, QExec qexec, boolean singleSample) throws SnpException, IOException, Exception {
		super(qexec, singleSample);
		
		// load rules from ini file
		ingestIni(iniFile);
		
		checkRules();
		
		loadNextReferenceSequence();
		
		checkBamHeaders();

		walkBams();
		
		logger.info("Finished walking bams");
		
		compoundSnps();
				
		// write output
		writeVCF(vcfFile);
	}
		
	@Override
	void ingestIni(Ini ini) throws SnpException {
		
		super.ingestIni(ini);
		
		// RULES
		controlRules = IniFileUtil.getRules(ini, "control");
		testRules = IniFileUtil.getRules(ini, "test");
		initialTestSumOfCountsLimit = IniFileUtil.getLowestRuleValue(ini);
				
		// INCLUDE INDELS
		includeIndels = true;
				
		logger.tool("**** OTHER CONFIG ****");
		logger.tool("No of control rules: " + controlRules.size());
		logger.tool("No of test rules: " + testRules.size());
		logger.tool("min coverage count for initial test: " + initialTestSumOfCountsLimit);
		logger.tool("mutationIdPrefix: " + mutationIdPrefix);
	}

}
