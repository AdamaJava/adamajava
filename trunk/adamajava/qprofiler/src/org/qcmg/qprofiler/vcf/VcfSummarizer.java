package org.qcmg.qprofiler.vcf;

import java.io.File;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLevel;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.summarise.Summarizer;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;

public class VcfSummarizer implements Summarizer {
	private final static QLogger logger = QLoggerFactory.getLogger(VcfSummarizer.class);
	private String[] excludes;
		
	@Override
	public SummaryReport summarize(String input, String index, String[] regions) throws Exception{
		
		// set logging level for printing of no of records parsed
		final boolean isLevelEnabled = logger.isLevelEnabled(QLevel.DEBUG);
		VcfSummaryReport vcfSummaryReport; 
		
		try (VCFFileReader reader = new VCFFileReader(new File(input))) {
			vcfSummaryReport = new VcfSummaryReport(reader.getHeader());
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
