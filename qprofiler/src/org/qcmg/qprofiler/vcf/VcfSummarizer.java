package org.qcmg.qprofiler.vcf;

import java.io.File;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.qio.vcf.VcfFileReader;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.summarise.Summarizer;

public class VcfSummarizer implements Summarizer {
	private final static QLogger logger = QLoggerFactory.getLogger(VcfSummarizer.class);
		
	@Override
	public SummaryReport summarize(String input, String index, String[] regions) throws Exception{
		
		// set logging level for printing of no of records parsed
		VcfSummaryReport vcfSummaryReport; 
		
		try (VcfFileReader reader = new VcfFileReader(new File(input))) {
			vcfSummaryReport = new VcfSummaryReport(reader.getVcfHeader());
			vcfSummaryReport.setFileName(input);
			vcfSummaryReport.setStartTime(DateUtils.getCurrentDateAsString());
		
        	//no chr in front of position
			for (final VcfRecord vcf : reader) {		
				vcfSummaryReport.parseRecord( vcf);					
			}	
 
		}	
		logger.info("records parsed: " + vcfSummaryReport.getRecordsParsed());
		vcfSummaryReport.setFinishTime(DateUtils.getCurrentDateAsString());
		return vcfSummaryReport;
	}
		
	 
}
