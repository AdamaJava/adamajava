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

package org.qcmg.qprofiler.gff;

import java.io.File;
import java.io.IOException;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLevel;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.gff.GFFReader;
import org.qcmg.gff.GFFRecord;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.summarise.Summarizer;
import org.qcmg.record.Record;

public class GffSummarizer implements Summarizer {
	
	private static final QLogger logger = QLoggerFactory.getLogger(GffSummarizer.class);
	
	@Override
	public SummaryReport summarize(File file) throws IOException {

		GffSummaryReport gffSummaryReport = new GffSummaryReport();
		gffSummaryReport.setFileName(file.getAbsolutePath());
		gffSummaryReport.setStartTime(DateUtils.getCurrentDateAsString());
		
		// set logging level for printing of no of records parsed
		final boolean isLevelEnabled = logger.isLevelEnabled(QLevel.DEBUG);

		
		try (GFFReader reader = new GFFReader(file);){
			for (Record record : reader) {
				gffSummaryReport.parseRecord((GFFRecord) record);
				
				if (isLevelEnabled && gffSummaryReport.getRecordsParsed() % FEEDBACK_LINES_COUNT == 0) {
					logger.debug("Records parsed: " + gffSummaryReport.getRecordsParsed());
				}
			}
		}

		gffSummaryReport.setFinishTime(DateUtils.getCurrentDateAsString());
		return gffSummaryReport;
	}
}
