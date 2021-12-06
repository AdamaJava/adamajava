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

package org.qcmg.qprofiler2.bam;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLevel;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qprofiler2.Summarizer;
import org.qcmg.qprofiler2.SummaryReport;

public class BamSummarizer implements Summarizer {
	public static final ValidationStringency DEFAULT_VS = ValidationStringency.SILENT;
	
	private int maxRecords;
	private String validation;
	private boolean isFullBamHeader;

	private static final QLogger logger = QLoggerFactory.getLogger(BamSummarizer.class);	
	public BamSummarizer() {}	 // default constructor	
	public BamSummarizer( int maxRecords, String validation, boolean isFullBamHeader) {
		this.maxRecords = maxRecords;
		this.validation = validation;
		this.isFullBamHeader = isFullBamHeader;
	}	
	
	public static BamSummaryReport createReport(SAMFileHeader header, String file, int maxRecords, boolean isFullBamHeader) throws IOException {
		
		// create the SummaryReport
		
		SAMSequenceDictionary samSeqDict  = header.getSequenceDictionary();
		List<String> readGroupIds = header.getReadGroups().stream().map( it -> it.getId()  ).collect(toList()); 		
		readGroupIds.sort(Comparator.comparing( String::toString ) ); // Natural order  
		
		BamSummaryReport bamSummaryReport = new BamSummaryReport( maxRecords, isFullBamHeader );									
		bamSummaryReport.setBamHeader(header, isFullBamHeader);		
		bamSummaryReport.setSamSequenceDictionary(samSeqDict);
		bamSummaryReport.setReadGroups(readGroupIds);		
		bamSummaryReport.setFileName(file);
		bamSummaryReport.setStartTime(DateUtils.getCurrentDateAsString());
				
		return bamSummaryReport;			
	}
	

	@Override
	public SummaryReport summarize(String input, String index) throws IOException {
		ValidationStringency vs = null != validation ? ValidationStringency.valueOf(validation) : DEFAULT_VS;
		SamReader reader = SAMFileReaderFactory.createSAMFileReaderAsStream(input, index, vs);
		
		// create the SummaryReport		
        BamSummaryReport bamSummaryReport = createReport(reader.getFileHeader(), input,  maxRecords, isFullBamHeader);
      		
		boolean logLevelEnabled = logger.isLevelEnabled(QLevel.DEBUG);		
		long currentRecordCount = 0;
	
		for (SAMRecord samRecord : reader) {			
			bamSummaryReport.parseRecord(samRecord);
			currentRecordCount = bamSummaryReport.getRecordsInputed();				
			if (logLevelEnabled && currentRecordCount % FEEDBACK_LINES_COUNT == 0) {
				logger.debug("Records parsed: " + currentRecordCount);
			}
						 				
			// if maxRecords is non-zero, stop when we hit it
			if (maxRecords > 0 && currentRecordCount == maxRecords) {
				break;			 
			}
		}			
			
		bamSummaryReport.cleanUp();
		logger.info("records parsed: " + bamSummaryReport.getRecordsInputed());
		bamSummaryReport.setFinishTime(DateUtils.getCurrentDateAsString());
		return bamSummaryReport;
	}
}
