/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
/**
 * Factory class - summarize() method generates SummaryReport objects from input files.
 * 
 * @author jpearson
 * @version $Id: GffSummarizer.java,v 1.3 2009/08/26 00:13:02 jpearson Exp $
 * 
 */

package org.qcmg.qprofiler.fastq;

import java.io.File;

import net.sf.picard.fastq.FastqReader;
import net.sf.picard.fastq.FastqRecord;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.summarise.Summarizer;

public class FastqSummarizer implements Summarizer {
	
	private final static QLogger logger = QLoggerFactory.getLogger(FastqSummarizer.class);
	private String [] excludes;
	
	public FastqSummarizer() {}	// default constructor
	
	public FastqSummarizer(String [] excludes) {
		this.excludes = excludes;
	}
	
	@Override
	public SummaryReport summarize(File file) throws Exception {
		
		
		// create the SummaryReport
		FastqSummaryReport fastqSummaryReport = new FastqSummaryReport(excludes);
		fastqSummaryReport.setFileName(file.getAbsolutePath());
		fastqSummaryReport.setStartTime(DateUtils.getCurrentDateAsString());
		
		// set logging level for printing of no of records parsed
//		final boolean isLevelEnabled = logger.isLevelEnabled(QLevel.DEBUG);
		
		long recordsParsed = 0;
		try (FastqReader reader =  new FastqReader(file);) {
			for (FastqRecord record : reader) {
				if (null != record) {
					
					fastqSummaryReport.parseRecord(record);
					recordsParsed = fastqSummaryReport.getRecordsParsed();
					
					if (recordsParsed % (FEEDBACK_LINES_COUNT) == 0) {
						logger.info("Records parsed: " + (recordsParsed / FEEDBACK_LINES_COUNT) + "M");
					}
				}
			}
		}
		
		logger.info("Records parsed: " + fastqSummaryReport.getRecordsParsed());
		
		fastqSummaryReport.setFinishTime(DateUtils.getCurrentDateAsString());
		return fastqSummaryReport;
	}
	
}