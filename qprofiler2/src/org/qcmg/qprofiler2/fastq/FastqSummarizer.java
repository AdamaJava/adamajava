/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
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

package org.qcmg.qprofiler2.fastq;

import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.fastq.FastqRecord;

import java.io.File;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.qprofiler2.Summarizer;
import org.qcmg.qprofiler2.report.SummaryReport;

public class FastqSummarizer implements Summarizer {
	
	private final static QLogger logger = QLoggerFactory.getLogger(FastqSummarizer.class);
	
	public FastqSummarizer() {}	// default constructor
	
	
	@Override
	public SummaryReport summarize(String file, String index) throws Exception {
				
		// create the SummaryReport
		FastqSummaryReport fastqSummaryReport = new FastqSummaryReport();
		fastqSummaryReport.setFileName(file );
		fastqSummaryReport.setStartTime(DateUtils.getCurrentDateAsString());
		
		long recordsParsed = 0;
		try (FastqReader reader =  new FastqReader(new File(file))) {
			for (FastqRecord record : reader) {
				if (null != record) {
					
					fastqSummaryReport.parseRecord(record);
					recordsParsed = fastqSummaryReport.getRecordsParsed();
					
					if (recordsParsed % (FEEDBACK_LINES_COUNT) == 0) {
						logger.info("records parsed: " + (recordsParsed / FEEDBACK_LINES_COUNT) + "M");
					}
				}
			}
		}
		
		logger.info("records parsed: " + fastqSummaryReport.getRecordsParsed());
		
		fastqSummaryReport.setFinishTime(DateUtils.getCurrentDateAsString());
		return fastqSummaryReport;
	}
	
}
