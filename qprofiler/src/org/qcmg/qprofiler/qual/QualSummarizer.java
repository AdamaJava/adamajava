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

package org.qcmg.qprofiler.qual;

import java.io.File;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLevel;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.qio.fasta.FastaReader;
import org.qcmg.qio.fasta.FastaRecord;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.summarise.Summarizer;


public class QualSummarizer implements Summarizer {
	
	private final static QLogger logger = QLoggerFactory.getLogger(QualSummarizer.class);
	private String[] excludes;
	
	public QualSummarizer() {}	// default constructor
	
	public QualSummarizer(String [] excludeArray) {
		this.excludes = excludeArray;
	}
	
	@Override
	public SummaryReport summarize(String input, String index, String[] regions) throws Exception {

		// create the SummaryReport
		QualSummaryReport qualSummaryReport = new QualSummaryReport(excludes);
		qualSummaryReport.setFileName(input);
		qualSummaryReport.setStartTime(DateUtils.getCurrentDateAsString());
		
		// set logging level for printing of no of records parsed
		final boolean isLevelEnabled = logger.isLevelEnabled(QLevel.DEBUG);
		
		try (FastaReader reader = new FastaReader(new File(input));){
			for (FastaRecord record : reader) {
				if (null != record) {
					
					try {
						qualSummaryReport.parseRecord( record);
					} catch (Exception e) {
						logger.error("Exception caught in QualSummarizer, number of records parsed: " 
								+ qualSummaryReport.getRecordsParsed());
						throw e;
					}
					
					if (isLevelEnabled && qualSummaryReport.getRecordsParsed() % (FEEDBACK_LINES_COUNT * 2) == 0) {
						logger.debug("Records parsed: " + qualSummaryReport.getRecordsParsed());
					}
				}
			}
		}
			
		logger.info("records parsed: " + qualSummaryReport.getRecordsParsed());
		qualSummaryReport.setFinishTime(DateUtils.getCurrentDateAsString());
		return qualSummaryReport;
	}
}
