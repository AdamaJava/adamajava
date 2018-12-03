package org.qcmg.qprofiler2.cohort;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.qprofiler2.cohort.CohortSummaryReport;
import org.qcmg.qprofiler2.vcf.VcfSummarizer;
import org.qcmg.qprofiler2.vcf.VcfSummaryReport;
import org.qcmg.qprofiler2.vcf.VcfSummaryReportTest;
import org.w3c.dom.Element;

public class CohortSummaryReportTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();	
	
	private String sample = "https://test.sample";
	private final static String[] category = new String[] {"FT","INF"};
	private final static String[] category1 = new String[] {"FT=PASS","INF"};
		

	@Test
	public void outputCountsTest() throws Exception{
		
		File input = testFolder.newFile("input.vcf");	
		createInput( input);
	
		//new  VcfSummarizer(null); cause exception since for(Sting cat: null)
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();			 
		Element root = builder.getDOMImplementation().createDocument(null, "qProfiler", null).getDocumentElement();
		VcfSummaryReport report = (VcfSummaryReport) (new  VcfSummarizer(new String[0])).summarize( input.getAbsolutePath()) ;
		report.toXml( root );
		Element sampleE = (Element) root.getElementsByTagName(VcfSummaryReport.Sample).item(0);
		CohortSummaryReport xReport = new CohortSummaryReport( input,sampleE );
		
		List<String> output = xReport.outputCounts();
		assertEquals(output.size(), 1);
		String counts[] = output.get(0).split(CohortSummaryReport.outputSeperate );
		
		assertEquals( counts[0].trim(), input.getCanonicalPath() );  //input
		assertEquals( counts[1].trim(), sample );   //sample
		assertEquals( counts[2].trim(), "-" );      // ReportingCategory set to - if not exist
		assertEquals( counts[3].trim(), "SNV" );	  // svn counts
		assertEquals( counts[4].trim(), "30" );	  // svn counts
		assertEquals( counts[5].trim(), "0.667" );	// dbsnp rate
		assertEquals( counts[6].trim(), "0.50" );	    // dbsnp rate
	}
	
	@Test
	public void withCategoryTest() throws Exception{
		File input = testFolder.newFile( "input.vcf" );	
		VcfSummaryReportTest.createVcfFile( null, input );
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();			 
		Element root = builder.getDOMImplementation().createDocument(null, "qProfiler", null).getDocumentElement();
		VcfSummaryReport report = (VcfSummaryReport) (new  VcfSummarizer(category)).summarize( input.getAbsolutePath()) ;
		report.toXml( root );
		
		String sample1 = "test1";	
		int outputSize = 0;
		for(Element ele :  QprofilerXmlUtils.getOffspringElementByTagName (root, VcfSummaryReport.Sample)){
			CohortSummaryReport xReport = new CohortSummaryReport( input, ele );
			List<String> outputs = xReport.outputCounts();
			outputSize += outputs.size();
			//System.out.println(Arrays.toString(outputs.toArray()));
			
			if(ele.getAttribute("value").equals("test1") ){
				assertTrue(outputs.size() == 4);
				for(String output : outputs ){
				 
					String[] subs = new String[]{"5BP=3;SOMATIC;GERM=42,185", "5BP=3;SOMATIC", "PASS;SOMATIC\tSNV" ,"PASS;SOMATIC\tDNV" };					
					if(output.contains(subs[0]))
						assertEquals( input.getCanonicalPath() + "\ttest1\t" + subs[0] + "\tSNV\t10\t1.000\t0.00",  output );
					else if(output.contains(subs[1]))
						assertEquals( input.getCanonicalPath() + "\ttest1\t" + subs[1] + "\tTNV\t10\t0.000\t-",  output );
					else if(output.contains(subs[2]))
						assertEquals( input.getCanonicalPath() + "\ttest1\t" + subs[2] + "\t10\t1.000\t1.00",  output );					
					else
						assertEquals( input.getCanonicalPath() + "\ttest1\t" + subs[3] + "\t10\t0.000\t-",  output );	 
				}
			}else if(ele.getAttribute("value").equals("control1") ){
				assertTrue(outputs.size() == 3);
				for(String output : outputs ){
					
					if(output.contains("\tSNV\t"))
						assertEquals( input.getCanonicalPath() + "\tcontrol1\tPASS;.\tSNV\t20\t1.000\t0.00",  output  ) ;
					else if(output.contains("\tDNV\t"))
						assertEquals( input.getCanonicalPath() + "\tcontrol1\tPASS;.\tDNV\t10\t0.000\t-",  output  ) ;
					else
						assertEquals( input.getCanonicalPath() + "\tcontrol1\tPASS;.\tTNV\t10\t0.000\t-",  output  ) ;
					}
			}else 
				assertTrue(outputs.size() == 3);
		}	
		assertTrue(13 == outputSize);
	}
	
	
	//S1: create vcf file eg. 

	private void createInput(File inputfile) throws IOException{
		  final List<String> data = new ArrayList<String>();
		  data.add("##fileformat=VCFv4.3");	
		  data.add("##INFO=<ID=DB,Number=.,Type=Integer,Description=\"Indicates which INput file this vcf record came from. Multiple values are allowed which indicate that the record has been merged from more than 1 input file\">");
		  data.add("##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">");	
		  data.add("##INFO=<ID=DP,Number=1,Type=Integer,Description=\"Approximate read depth; some reads may have been filtered\">");	
		  data.add("##INFO=<ID=AD,Number=1,Type=Integer,Description=\"Approximate read depth; some reads may have been filtered\">");	

		  data.add("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\t"+sample);	
		  
		  for(int i = 0; i < 10; i++){	
			  data.add("chr1\t10177\trs201752861\tA\tT\t.\t.\t.\tGT:AD:DP\t0/1:,9:27");	 //tv			  
			  data.add("chr1\t80930980\trs7354844\tG\tT\t.\t.\tDB\tGT:AD:DP\t0/1:,52:182"); //tv
			  data.add("chr21\t10725791\t.\tC\tT\t.\t.\t.\tGT:AD:DP\t0/1:,7:73");	 	 //ti
		  }
		  
		  try( BufferedWriter out = new BufferedWriter( new FileWriter(inputfile) ); ){      
		     for ( final String line : data )  out.write( line + "\n");
		  } 		  
	}
	
	
	//S2: create second vcf according to VcfSummaryReportTest::createVcfFile(...)
	
	//S3: create xml by calling qprofiler vcf mode
	
	
}
