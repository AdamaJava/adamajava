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

package org.qcmg.qprofiler.fasta;

import java.io.File;
import java.util.concurrent.BlockingQueue;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLevel;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.summarise.Summarizer;
import org.qcmg.record.Record;
import org.qcmg.record.SimpleRecord;
import org.qcmg.simple.SimpleFileReader;


public class FastaSummarizer implements Summarizer {
	
	private final static QLogger logger = QLoggerFactory.getLogger(FastaSummarizer.class);
	private boolean endOfFileReached;
//	private boolean allRecordsParsed;
	
	private Throwable thrown;
	private String [] excludes;
	
	public FastaSummarizer() {}	// default constructor
	
	public FastaSummarizer(String [] excludes) {
		this.excludes = excludes;
	}
	
	@Override
	public SummaryReport summarize(File file) throws Exception {
		// create the SummaryReport
		FastaSummaryReport fastaSummaryReport = new FastaSummaryReport(excludes);
		fastaSummaryReport.setFileName(file.getAbsolutePath());
		fastaSummaryReport.setStartTime(DateUtils.getCurrentDateAsString());
		
		// set logging level for printing of no of records parsed
		final boolean isLevelEnabled = logger.isLevelEnabled(QLevel.DEBUG);
		long recordsParsed = 0;
		
		SimpleFileReader reader =  new SimpleFileReader(file);
		try {
			for (Record record : reader) {
				if (null != record) {
					
					fastaSummaryReport.parseRecord((SimpleRecord) record);
					recordsParsed = fastaSummaryReport.getRecordsParsed();
					
					if (isLevelEnabled && recordsParsed % (FEEDBACK_LINES_COUNT * 2) == 0) {
						logger.debug("Records parsed: " + recordsParsed);
					}
				}
			}
		
		} finally {
			reader.close();
		}
		
		logger.info("Records parsed: " + fastaSummaryReport.getRecordsParsed());
		fastaSummaryReport.setFinishTime(DateUtils.getCurrentDateAsString());
		return fastaSummaryReport;
	}
	
	public class FileReader implements Runnable {
		
		private final SimpleFileReader reader;
		private final BlockingQueue<Record> queue;
		
		public FileReader(SimpleFileReader reader, BlockingQueue<Record> queue) throws Exception {
			this.queue = queue;
			this.reader = reader;
		}

		@Override
		public void run() {
			logger.info("Fasta FileReader thread started!!!");
				
			long count = 0;
			try {
				for (Record record : reader) {
					queue.put(record);
					if (++count % 1000000 == 0) {
//							if (++count % FEEDBACK_LINES_COUNT == 0) {
						logger.debug("Records parsed in FastaSummarizer: " + count);
//						logger.debug("Records parsed in FastaSummarizer: {}", count);
					}
				}
			} catch (InterruptedException ie) {
				logger.warn("FileReader thread interrupted!!!");
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				logger.error("Exception caught by FileReader thread");
				thrown = e;
			} finally {
				endOfFileReached = true;
				logger.debug("Fasta FileReader thread finished");
			}
		}
	}
	
	public static class FASTARecordConsumer implements Runnable {
		
		private final BlockingQueue<Record> queue;
		private final FastaSummaryReport report;
		
		public FASTARecordConsumer(FastaSummaryReport report, BlockingQueue<Record> queue) {
			this.queue = queue;
			this.report = report;
		}
		
		@Override
		public void run() {
			logger.debug("FASTARecordConsumer thread started!!!" );
			long count = 0;
			try {
				while (true) {
					report.parseRecord((SimpleRecord) queue.take());
					if (++count % 1000000 == 0)
						logger.info("count: " + count); 
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
}