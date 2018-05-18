package org.qcmg.qprofiler2.cohort;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.qprofiler2.Summarizer;
import org.qcmg.qprofiler2.report.SummaryReport;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class CohortSummarizer implements Summarizer {
	private final static QLogger logger = QLoggerFactory.getLogger(CohortSummarizer.class);
	private int errNo = 0;
	
	List<CohortSummaryReport> reports = new ArrayList<>();
	public CohortSummarizer( ) { }	
		
	@Override
	public SummaryReport summarize(String file, String index) throws Exception{
		logger.info("processing file " + file);
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); 		 		
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(file);
		doc.getDocumentElement().normalize();
		
		NodeList sampleNS =	 doc.getElementsByTagName("Sample");
		
		for(int i = 0; i < sampleNS.getLength(); i ++) 
			reports.add( new CohortSummaryReport(new File(file), (Element) sampleNS.item(i)) );
				
		return null; 
	}
	
	 
	public void  outputSumamry(  File output) throws IOException {
 		
		long[] sumCounts = new long[]{0,0,0,0}; //for log file 
		int order = 0; 	//output
		try (BufferedWriter writer =  new BufferedWriter(new FileWriter(output))) {  
			writer.write( "No\t" + CohortSummaryReport.headerline + "\n");
 			for(CohortSummaryReport report : reports ){
				//output all category of each sample
				for(String str : report.outputCounts())
					writer.write(( order ++) + "\t" + str + "\n");	
				//summry to log file
				
				//System.out.println(report.sampleId);
				for(int i = 0; i < sumCounts.length; i++){
					long[] reportCounts = report.getCountSum();					
					//debug
					//System.out.println(i + " (summCount[i]): " + sumCounts[i] + " += " + reportCounts[i]);
					sumCounts[i] += reportCounts[i];
				}			
			}
		} // end of try
		
 		String summary = "summary: \nVariantCount\tDbSnpProportion\tTiTvRatio\n";
 		summary += String.format("%d\t%.3f\t%.3f\n", sumCounts[0], 
 				(double) sumCounts[1] / sumCounts[0] ,  (double) sumCounts[2] / sumCounts[3] );
 		logger.info(summary);		
 		
//		for( String key :  sumCounts.keySet())		
//			summary +=  String.format( "%s, %d, %.3f, %.3f\n", key, sumCounts.get(key)[0], 
//					(double) sumCounts.get(key)[1] / sumCounts.get(key)[0] , 
//					(double) sumCounts.get(key)[2] / sumCounts.get(key)[3] );
//		
//		logger.info("total output records is " + order);
//		

	}	
	
}