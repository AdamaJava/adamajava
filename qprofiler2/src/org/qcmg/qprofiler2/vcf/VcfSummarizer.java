package org.qcmg.qprofiler2.vcf;

import java.io.File;
import java.util.Arrays;

import org.qcmg.common.date.DateUtils;
import org.qcmg.common.log.QLevel;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.qprofiler2.Summarizer;
import org.qcmg.qprofiler2.report.SummaryReport;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;

public class VcfSummarizer implements Summarizer {
	private final static QLogger logger = QLoggerFactory.getLogger(VcfSummarizer.class);
	private String[] formats;
	private int errNo = 0;
	
	public VcfSummarizer(String[] formatArgs) {
		this.formats = formatArgs; 
	}
		
	@Override
	public SummaryReport summarize(String input, String index) throws Exception{
	 		
		// set logging level for printing of no of records parsed
//		final boolean isLevelEnabled = logger.isLevelEnabled( QLevel.DEBUG );
		VcfSummaryReport vcfSummaryReport; 
		
		try (VCFFileReader reader = new VCFFileReader(new File(input))  ) {
			VcfHeader header = reader.getHeader();
			if(header == null || header.getSampleId() == null) 
				throw new Exception("Invalid Vcf Header: please specify Sample id after Format column on Header line!");
 
			vcfSummaryReport = new VcfSummaryReport(header, formats);
			vcfSummaryReport.setFileName(input);
			vcfSummaryReport.setStartTime(DateUtils.getCurrentDateAsString());
		
        	//no chr in front of position
			for (final VcfRecord vcf : reader) {
				try{
					vcfSummaryReport.parseRecord( vcf );		
					
				}catch(Exception e){
					if( ++errNo < 500 )
						logger.warn("error in vcf record: " + vcf.toString());
					else 
						throw e; 
				}			
			}	
		}	
		
		logger.info("records parsed: " + vcfSummaryReport.getRecordsParsed());
		vcfSummaryReport.setFinishTime(DateUtils.getCurrentDateAsString());
		return vcfSummaryReport;
	}
}
