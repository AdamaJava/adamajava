 



package org.qcmg.qprofiler.bam;
 
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.Assert;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.ProfileType;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qprofiler.QProfiler;
import org.qcmg.qprofiler.QProfilerSummary;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.summarise.Summarizer;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
 
public class BamSummaryRGReportTest {
	private static final String INPUT_FILE = "input.sam";
 
	
	@Before
	public void setup() throws IOException {
		createInputFile();
	}

	@After
	public void tearDown() {
		new File(INPUT_FILE).delete();		
	}
	
//	@Test
//	public void tempTest(){
//		SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(new File(INPUT_FILE) );
//		for (SAMRecord samRecord : reader) {
//			System.out.println(String.format("%s, start:%d, end:%d, readlength:%d, seq length:%d, cigar:%s",
//					samRecord.getReadName(),  samRecord.getAlignmentStart(),samRecord.getAlignmentEnd(), 
//					samRecord.getReadLength(),samRecord.getReadString().length(), samRecord.getCigarString() ));
//			
//		}	
//	}
	
	@Test
	public void parseClipsByRGTest() throws Exception{
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		DOMImplementation domImpl = builder.getDOMImplementation();		
		Document doc = domImpl.createDocument(null, "qProfiler", null);
		Element root = doc.getDocumentElement();					 

		BamSummarizer bs = new BamSummarizer();
		BamSummaryReport sr = (BamSummaryReport) bs.summarize(new File(INPUT_FILE)); 
		sr.toXml(root);
		
//		Assert.assertEquals(sr.getRecordsInputed(), 11);
		Assert.assertEquals(sr.getRecordsParsed(), 11);
		testXmlOutput(root);
	}
	
	private void testXmlOutput(Element root) throws Exception {
		
		NodeList summaryNodes = root.getElementsByTagName("SUMMARY").item(0).getChildNodes();		
		Assert.assertNotNull(summaryNodes);
		for (int i = 0 ; i < summaryNodes.getLength() ; i++) 
			if(summaryNodes.item(i).getNodeName().equals("BaseCount")){
				 NamedNodeMap node =  summaryNodes.item(i).getAttributes();
				 String rg = node.getNamedItem("rg").getNodeValue();
				 switch ( rg) {
				 	case "1959N" :
				 		Assert.assertEquals(node.getNamedItem("overlap").getNodeValue(), "12.00%");
				 		Assert.assertEquals(node.getNamedItem("trimmedBase").getNodeValue(), "13.00%");
				 		Assert.assertEquals(node.getNamedItem("softClip").getNodeValue(), "5.00%");
				 		Assert.assertEquals(node.getNamedItem("hardClip").getNodeValue(), "0.00%");
				 		Assert.assertEquals(node.getNamedItem("totalLost").getNodeValue(), "30.00%");				 		
				 		Assert.assertEquals(node.getNamedItem("maxLength").getNodeValue(), "50");	
				 		Assert.assertEquals(node.getNamedItem("totalReads").getNodeValue(), "2");
				 		break;
				 	case "1959T" :
				 		Assert.assertEquals(node.getNamedItem("unmapped").getNodeValue(), "16.67%");	
				 		Assert.assertEquals(node.getNamedItem("nonCanonicalPair").getNodeValue(), "16.67%");
				 		Assert.assertEquals(node.getNamedItem("duplicate").getNodeValue(), "33.33%");			 		
				 		Assert.assertEquals(node.getNamedItem("softClip").getNodeValue(), "0.00%");
				 		Assert.assertEquals(node.getNamedItem("hardClip").getNodeValue(), "5.42%");
				 		Assert.assertEquals(node.getNamedItem("trimmedBase").getNodeValue(), "0.00%");				 		
				 		Assert.assertEquals(node.getNamedItem("overlap").getNodeValue(), "10.83%");				 		
				 		Assert.assertEquals(node.getNamedItem("totalLost").getNodeValue(), "82.92%");
				 		Assert.assertEquals(node.getNamedItem("maxLength").getNodeValue(), "40");	
				 		Assert.assertEquals(node.getNamedItem("totalReads").getNodeValue(), "6");
				 		break;	
				 	case "Total" :
				 		Assert.assertEquals(node.getNamedItem("totalReads").getNodeValue(), "8");	
				 		Assert.assertEquals(node.getNamedItem("unmapped").getNodeValue(), "11.76%");	
				 		Assert.assertEquals(node.getNamedItem("nonCanonicalPair").getNodeValue(), "11.76%");
				 		Assert.assertEquals(node.getNamedItem("duplicate").getNodeValue(), "23.53%");			 		
				 		Assert.assertEquals(node.getNamedItem("softClip").getNodeValue(), "1.47%");
				 		Assert.assertEquals(node.getNamedItem("hardClip").getNodeValue(), "3.82%" );
				 		Assert.assertEquals(node.getNamedItem("trimmedBase").getNodeValue(), "3.82%" );				 		
				 		Assert.assertEquals(node.getNamedItem("overlap").getNodeValue(), "11.18%");				 		
				 		Assert.assertEquals(node.getNamedItem("totalLost").getNodeValue(), "67.35%");
				 		Assert.assertEquals(node.getNamedItem("maxLength").getNodeValue(), "-");					 					 				 		
				 		break;	
				 		
				 	default:
				 		throw new Exception("unexpected read group appears on xml SUMMARY section");
				 }
			}
		}
		 
 	
	private static void createInputFile() throws IOException{
		List<String> data = new ArrayList<String>();
        data.add("@HD	VN:1.0	SO:coordinate");
        data.add("@RG	ID:1959T	SM:eBeads_20091110_CD	DS:rl=50");
        data.add("@PG	ID:SOLID-GffToSam	VN:1.4.3");
        data.add("@SQ	SN:chr1	LN:249250621");
        data.add("@SQ	SN:chr11	LN:243199373");

		//unmapped
		data.add("243_146_1	101	chr1	10075	0	*	=	10167	0	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
 
		//duplicated
		data.add("243_146_2	1171	chr1	10075	6	3H37M	=	10167	142	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");

		data.add("243_146_4	1121	chr1	10075	6	3H37M	=	10167	142	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");		
		
		//secondary = not primary
		data.add("243_146_2	353	chr1	10075	6	3H37M	=	10167	142	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
				
		//vendorcheck failed
		data.add("243_146_3	609	chr1	10075	6	3H37M	=	10167	142	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");		
				
		//supplementary
		data.add("243_146_5	2147	chr1	10075	6	3H37M	=	10167	142	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
		
		//hard clip both forward non-canonical pair
		data.add("243_146_5	67	chr1	10075	6	3H37M	=	10100	25	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
				
		//overlap
		//forward ??? why -59, it should be reverse.mate_end -forward.read_start
		data.add("970_1290_1068	163	chr1	10176	3	22M50D10M8H	=	10167	26	" +
				"AACCTAACCCTAACCCTAACCCTAACCCTAAC	I&&HII%%IIII4CII=4?IIF0B((!!7F@+	RG:Z:1959T	" +
				"CS:Z:G202023020023010023010023000.2301002302002330000000	CQ:Z:@A&*?=9%;?:A-(<?8&/1@?():(9!,,;&&,'35)69&)./?11)&=");		
		//reverse
		data.add("970_1290_1068	83	chr1	10167	1	5H35M	=	10176	-26	" +
				"CCTAACNCTAACCTAACCCTAACCCTAACCCTAAC	.(01(\"!\"&####07=?$$246/##<>,($3HC3+	RG:Z:1959T	" +
				"CS:Z:T11032031032301032201032311322310320133320110020210	CQ:Z:#)+90$*(%:##').',$,4*.####$#*##&,%$+$,&&)##$#'#$$)");
		 			
		//below overlap is not 52 but 37 groupRG:Z:1959N. make sure overlap only calculate once. 
		//trimmed, deletion forward   
		data.add("243_145_5	99	chr1	10075	6	15M50N22M	=	10100	175	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959N	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");

		//mate reverse   soft clip ???seqbase maybe wrong
		data.add("243_145_5	147	chr1	10100	6	25M100D20M5S	=	10075	-175	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAACACCCTAACCCTAA	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3II##>II$$BIIC3	" +
				"RG:Z:1959N	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
		
		try(BufferedWriter out = new BufferedWriter(new FileWriter(INPUT_FILE))){	    
			for (String line : data)  out.write(line + "\n");	               
		}
		
	}

}
