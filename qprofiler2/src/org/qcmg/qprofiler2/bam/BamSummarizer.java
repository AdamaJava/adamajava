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

import java.io.File;
import java.io.IOException;
import java.util.List;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMProgramRecord;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLevel;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qprofiler2.Summarizer;
import org.qcmg.qprofiler2.report.SummaryReport;

public class BamSummarizer implements Summarizer {
	
	private String [] includes;
	private String [] tags;
	private String [] tagsInt;
	private String [] tagsChar;
	private int maxRecords;
	private String validation;

	private final static QLogger logger = QLoggerFactory.getLogger(BamSummarizer.class);	
	public BamSummarizer() {}	// default constructor	
	public BamSummarizer(String [] includes, int maxRecords, String [] tags, String [] tagsInt, String [] tagsChar, String validation) {
		this.includes = includes;
		this.maxRecords = maxRecords;
		this.tags = tags;
		this.tagsInt = tagsInt;
		this.tagsChar = tagsChar;
		this.validation = validation;
	}
	
	public static BamSummaryReport createReport(File file,String [] includes, int maxRecords, String [] tags, String [] tagsInt, String [] tagsChar) throws IOException{
		
		// create the SummaryReport
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(file);
		SAMSequenceDictionary samSeqDict  = reader.getFileHeader().getSequenceDictionary();
		String bamHeader = reader.getFileHeader().getTextHeader();
		List<SAMProgramRecord> pgLines = reader.getFileHeader().getProgramRecords();
		List<String> readGroupIds = reader.getFileHeader().getReadGroups().stream().map( it -> it.getId()  ).collect(toList()); 
		
		boolean torrentBam = false;
		for (SAMProgramRecord pgLine : pgLines)  
			if ("tmap".equals(pgLine.getId())){ torrentBam = true;break;}		
		reader.close();
				
		BamSummaryReport bamSummaryReport = new BamSummaryReport(includes, maxRecords, tags, tagsInt, tagsChar);		
		if(torrentBam) bamSummaryReport.setTorrentBam();
		bamSummaryReport.setBamHeader(bamHeader);
		bamSummaryReport.setSamSequenceDictionary(samSeqDict);
		bamSummaryReport.setReadGroups(readGroupIds);		
		bamSummaryReport.setFileName(file.getAbsolutePath());
		bamSummaryReport.setStartTime(DateUtils.getCurrentDateAsString());				
		return bamSummaryReport;			
	}
	
	@Override
	public SummaryReport summarize(File file) throws Exception {
		
		boolean logLevelEnabled = logger.isLevelEnabled(QLevel.DEBUG);
		BamSummaryReport bamSummaryReport = createReport(file, includes, maxRecords, tags, tagsInt, tagsChar);
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(file, validation);
		long currentRecordCount = 0;
		for (SAMRecord samRecord : reader) {				 
			bamSummaryReport.parseRecord(samRecord);
			currentRecordCount = bamSummaryReport.getRecordsParsed();				
			if (logLevelEnabled && currentRecordCount % FEEDBACK_LINES_COUNT == 0) 
				logger.debug("Records parsed: " + currentRecordCount);
			 				
			// if maxRecords is non-zero, stop when we hit it
			if (maxRecords > 0 && currentRecordCount == maxRecords)  break;			 
		}			
			
		bamSummaryReport.cleanUp();
		logger.info("records parsed: "+ bamSummaryReport.getRecordsParsed());
		bamSummaryReport.setFinishTime(DateUtils.getCurrentDateAsString());
		return bamSummaryReport;
	}
}
