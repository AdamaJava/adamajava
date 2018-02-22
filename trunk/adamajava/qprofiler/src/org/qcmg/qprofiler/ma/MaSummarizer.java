/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler.ma;

import java.io.File;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLevel;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.ma.MAFileReader;
import org.qcmg.ma.MARecord;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.summarise.Summarizer;

public class MaSummarizer implements Summarizer {
	
	private final static QLogger logger = QLoggerFactory.getLogger(MaSummarizer.class);
	
	@Override
	public SummaryReport summarize(String input, String index, String[] regions) throws Exception {
		
		MAFileReader reader = null;
		try {
			reader = new MAFileReader(new File(input));
		} catch (Exception e) {
			logger.error("Exception caught whilst trying to instantiate MAFileReader with file: " + input, e);
		}
		
		// create the SummaryReport
		MaSummaryReport maSummaryReport = new MaSummaryReport();
		maSummaryReport.setFileName(input);
		maSummaryReport.setStartTime(DateUtils.getCurrentDateAsString());
		
		// set logging level for printing of no of records parsed
		final boolean isLevelEnabled = logger.isLevelEnabled(QLevel.DEBUG);
		try {
			for (MARecord maRecord : reader) {
				maSummaryReport.parseRecord(maRecord);
				
				if (isLevelEnabled && maSummaryReport.getRecordsParsed() % FEEDBACK_LINES_COUNT == 0) {
					logger.debug("Records parsed in MaSummarizer: " + maSummaryReport.getRecordsParsed());
				}
			}
		} finally {
			reader.close();
		}
		
		logger.info("records parsed: " + maSummaryReport.getRecordsParsed());
		
		maSummaryReport.setFinishTime(DateUtils.getCurrentDateAsString());
		return maSummaryReport;
	}
}
