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
package org.qcmg.qprofiler.bam;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.List;

import htsjdk.samtools.*;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLevel;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.summarise.Summarizer;

public class BamSummarizer implements Summarizer {
	
	public static final ValidationStringency DEFAULT_VS = ValidationStringency.SILENT;
	
	private String [] includes;
	private String [] tags;
	private String [] tagsInt;
	private String [] tagsChar;
	private int maxRecords;
	private String validation;
	
	private static String bamHeader;
	private static SAMSequenceDictionary samSeqDict;
	private static List<String> readGroupIds;
	private boolean torrentBam = false;
	
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
	
	@Override
	public SummaryReport summarize(String input, String index, String[] regions) throws Exception {
		ValidationStringency vs = null != validation ? ValidationStringency.valueOf(validation) : DEFAULT_VS;
		// create the SummaryReport
		BamSummaryReport bamSummaryReport = new BamSummaryReport(includes, maxRecords, tags, tagsInt, tagsChar);
		bamSummaryReport.setFileName(input);
		bamSummaryReport.setStartTime(DateUtils.getCurrentDateAsString());
		
		try(SamReader reader = SAMFileReaderFactory.createSAMFileReaderAsStream(input, index, vs);) {
			readGroupIds = reader.getFileHeader().getReadGroups().stream().map(SAMReadGroupRecord::getId).collect(toList());
			bamSummaryReport.setReadGroups(readGroupIds);
			
			boolean logLevelEnabled = logger.isLevelEnabled(QLevel.DEBUG);
			
			// iterate over the SAMRecord objects returned, passing them to the summariser
			long currentRecordCount = 0;
		
			for (SAMRecord samRecord : reader) {
				try {
					bamSummaryReport.parseRecord(samRecord);
				} catch (Exception e) {
					logger.error("Error caught parsing SAMRecord with readName: " + samRecord.getReadName(), e);
					throw e;
				}
				
				currentRecordCount = bamSummaryReport.getRecordsParsed();
				
				if (logLevelEnabled && currentRecordCount % FEEDBACK_LINES_COUNT == 0) {
					logger.debug("Records parsed: " + currentRecordCount);
				}
				
				// if maxRecords is non-zero, stop when we hit it
				if (maxRecords > 0 && currentRecordCount == maxRecords) {
					break;
				}
			}
			
			if(bamSummaryReport.getErrMDReadNumber() >= bamSummaryReport.errMDReadLimit)
				logger.warn(  "big number of reads with wrong MD field: " + bamSummaryReport.getErrMDReadNumber());
			
			samSeqDict = reader.getFileHeader().getSequenceDictionary();
			bamHeader = reader.getFileHeader().getTextHeader();
			List<SAMProgramRecord> pgLines = reader.getFileHeader().getProgramRecords();
			for (SAMProgramRecord pgLine : pgLines) {
				if ("tmap".equals(pgLine.getId())) torrentBam = true;
			}
			bamSummaryReport.setTorrentBam(torrentBam);
			bamSummaryReport.setBamHeader(bamHeader);
			bamSummaryReport.setSamSequenceDictionary(samSeqDict);			
		}  
		
		bamSummaryReport.cleanUp();
		logger.info("records parsed: "+ bamSummaryReport.getRecordsParsed());
		bamSummaryReport.setFinishTime(DateUtils.getCurrentDateAsString());
		return bamSummaryReport;
	}
}
