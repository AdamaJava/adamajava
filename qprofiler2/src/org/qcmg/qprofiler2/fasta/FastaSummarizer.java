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

package org.qcmg.qprofiler2.fasta;

import java.io.File;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLevel;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.qprofiler2.Summarizer;
import org.qcmg.qprofiler2.report.SummaryReport;
import org.qcmg.record.Record;
import org.qcmg.record.SimpleRecord;
import org.qcmg.simple.SimpleFileReader;


public class FastaSummarizer implements Summarizer {
	
	private final static QLogger logger = QLoggerFactory.getLogger(FastaSummarizer.class);
	
	private String [] excludes;
	
	public FastaSummarizer() {}	// default constructor
	
	public FastaSummarizer(String [] excludes) {
		this.excludes = excludes;
	}
	
	@Override
	public SummaryReport summarize(String file, String index) throws Exception {
		// create the SummaryReport
		FastaSummaryReport fastaSummaryReport = new FastaSummaryReport();
		fastaSummaryReport.setFileName(file );
		fastaSummaryReport.setStartTime(DateUtils.getCurrentDateAsString());
		
		// set logging level for printing of no of records parsed
		final boolean isLevelEnabled = logger.isLevelEnabled(QLevel.DEBUG);
		long recordsParsed = 0;
		
		SimpleFileReader reader =  new SimpleFileReader(new File(file));
		try {
			for (Record record : reader) {
				if (null != record) {					
					fastaSummaryReport.parseRecord((SimpleRecord) record);
					
					//debug level
					recordsParsed = fastaSummaryReport.getRecordsParsed();					
					if (isLevelEnabled && recordsParsed % (FEEDBACK_LINES_COUNT * 2) == 0) 
						logger.debug("records parsed: " + recordsParsed);					 
				}
			}
		
		} finally {
			reader.close();
		}
		
		logger.info("records parsed: " + fastaSummaryReport.getRecordsParsed());
		fastaSummaryReport.setFinishTime(DateUtils.getCurrentDateAsString());
		return fastaSummaryReport;
	}
	
}
