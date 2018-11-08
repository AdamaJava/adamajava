package org.qcmg.qprofiler2.bam;
 
import static org.junit.Assert.assertTrue;
import org.junit.After;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qprofiler2.summarise.ReadGroupSummary;
import org.qcmg.qprofiler2.util.XmlUtils;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

public class BamSummaryRGReportTest {
	private static final String INPUT_FILE = "input.sam";

	@After
	public void tearDown() { 	new File(INPUT_FILE).delete(); }
	
	@Test
	public void tempTest() throws Exception{
		createMDerrFile();
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		DOMImplementation domImpl = builder.getDOMImplementation();		
		Document doc = domImpl.createDocument(null, "qProfiler", null);
		Element root = doc.getDocumentElement();					 

		BamSummarizer2 bs = new BamSummarizer2();
		BamSummaryReport2 sr = (BamSummaryReport2) bs.summarize(INPUT_FILE); 
		sr.toXml(root);		 
	}

	
	@Test
	public void overlapRGTest() throws IOException, ParserConfigurationException {
		
		String rgid = "1959N"; //here only test the pair from "1959N" 	
		ReadGroupSummary rgSumm = createRGElement(rgid );
		Element root = QprofilerXmlUtils.createRootElement("root",null);
		rgSumm.readSummary2Xml(root);
		//must be after readSummary2Xml(root)
		assertTrue(rgSumm.getMaxBases() == 100 ); //2 * maxReadLength
		assertTrue(rgSumm.getCountedReads() == 2);
							
		Element readsEle = QprofilerXmlUtils.getChildElementByTagName(root, XmlUtils.metricsEle)		
			.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals( "reads" )).findFirst().get() ;
				
		assertTrue(readsEle.getChildNodes().getLength() == 11);
		List<Element> valueEles = QprofilerXmlUtils.getOffspringElementByTagName(readsEle, XmlUtils.Svalue);
		assertTrue(valueEles.size() == 40);
		
		assertTrue( checkChildValue(readsEle,"maxLength", "50")); 
		assertTrue( checkChildValue(readsEle,"averageLength", "43")); 		
		assertTrue( checkChildValue(readsEle,"lostBasesPercent", "30.00"));  //(13+5+12)/100=30%
				
		checkfilteredReads(readsEle, new String[] {"2", "0", "0","0"});		
		//counted reads is 9-1-1-1 =6							
		checkDiscardReadStats(readsEle, "duplicateReads",0, 2 );
		checkDiscardReadStats(readsEle, "unmappedReads", 0,2 );
		checkDiscardReadStats(readsEle, "nonCanonicalPair", 0,2 );
		//total counted read Base is 2*50=100
		checkCountedReadStats(readsEle,  "trimmedBase", new String[] {"1","13","13","13","13","0"}, 2, 100, "13.00");
		checkCountedReadStats(readsEle, "softClippedBases", new String[] {"1","5","5","5","5","0"}, 2, 100, "5.00");		
		checkCountedReadStats(readsEle, "hardClippedBases",new String[] {"0","0","0","0","0","0"}, 2, 100, "0.00");
		//12/100= 12.00%
		checkCountedReadStats(readsEle, "overlapBases", new String[] {"1","12","12","12","12","0"}, 2, 100, "12.00");			 
	
	}
	
	
	/**
	 * 
	 * @param parent: <sequenceMetrics Name="reads" count="9">
	 * @param counts: array of {totalReads, supplementaryReads, secondaryReads, failedReads}
	 */
	private void checkfilteredReads(Element parent, String... counts) {
		
		assertTrue( parent.getAttribute("count").equals(counts[0]) ); //9 reads in this read group
		Element ele1 = QprofilerXmlUtils.getChildElementByTagName(parent, XmlUtils.variableGroupEle)
				   .stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals("filteredReads")).findFirst().get() ;
				
		assertTrue( checkChildValue(ele1,"supplementaryAlignmentCount", counts[1]));
		assertTrue( checkChildValue(ele1,"secondaryAlignmentCount", counts[2]));
		assertTrue( checkChildValue(ele1,"failedVendorQualityCount", counts[3]));	
	}
	
	/**
	 * 
	 * @param parent: eg. <sequenceMetrics Name="reads" count="9">
	 * @param name: variableGroup name
	 * @param counts: read number for duplicate, unmapped or non-canonical
	 * @param totalReads : total counted reads
	 * @return
	 */
	private Element checkDiscardReadStats(Element parent, String name, int counts, int totalReads ){
		    
		   Element groupE =  QprofilerXmlUtils.getChildElementByTagName(parent, XmlUtils.variableGroupEle)
				   .stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals(name)).findFirst().get() ;
		   
			assertTrue( checkChildValue(groupE,"readCount", String.valueOf(counts)));		
			String v = String.format("%,.2f",  100 *  (float)  counts / totalReads) ;		
			assertTrue( checkChildValue(groupE,"basePercent", v));
			return groupE;
	}
	
	/**
	 * 
	 * @param parent: eg. <sequenceMetrics Name="reads" count="9">
	 * @param name: variableGroup name
	 * @param counts: read number for trimmed, softClipped, hardClipped or overlapped
	 * @param totalReads: counted reads
	 * @param totalBase: counted_reads * max_read_length
	 * @param percent: lostbase/totalBase
	 */
	
	private void checkCountedReadStats(Element parent, String nodeName, String[] counts, int totalReads, int totalBase, String percent ) {
		 //check readCount
		 
	   Element groupE =  QprofilerXmlUtils.getChildElementByTagName(parent, XmlUtils.variableGroupEle)
			   .stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals(nodeName)).findFirst().get() ;
	    assertTrue( checkChildValue(groupE,"readCount", counts[0]));	
	   
		assertTrue( checkChildValue(groupE,"min", counts[1] ));
		assertTrue( checkChildValue(groupE,"max", counts[2] ));
		assertTrue( checkChildValue(groupE,"mean", counts[3] ));
		assertTrue( checkChildValue(groupE,"mode", counts[4] ));	
		assertTrue( checkChildValue(groupE,"median", counts[5] ));	
		assertTrue( checkChildValue(groupE,"basePercent", percent));
		
	}
	
	@Test 
	public void badRGTest()throws Exception{
		String rgid = QprofilerXmlUtils.UNKNOWN_READGROUP; //here only test the pair from "1959N" 	
		ReadGroupSummary rgSumm = createRGElement(rgid );
		Element root = QprofilerXmlUtils.createRootElement("root",null);
		rgSumm.readSummary2Xml(root);
						
		//must be after readSummary2Xml(root)
		assertTrue(rgSumm.getMaxBases() == 75 ); //0 * maxReadLength
		assertTrue(rgSumm.getCountedReads() == 1); //counted reads is  1	
		
		
		//debug
		QprofilerXmlUtils.asXmlText(root, "/Users/christix/Documents/Eclipse/data/qprofiler/unitTest.xml");

		
		root = QprofilerXmlUtils.getChildElementByTagName(root, XmlUtils.metricsEle)		
				.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals( "reads" )).findFirst().get() ;					
		assertTrue( checkChildValue(root,"maxLength", "0"));  
		assertTrue( checkChildValue(root,"averageLength", "0")); 		
		assertTrue( checkChildValue(root,"lostBasesPercent", "100.00")); 
		
		for(Element ele : QprofilerXmlUtils.getChildElementByTagName(root, XmlUtils.variableGroupEle) )
			if(ele.getAttribute(XmlUtils.Sname).equals("nonCanonicalPair") ) {
				assertTrue( checkChildValue(ele,"readCount", "1"));  
				assertTrue( checkChildValue(ele,"basePercent",  "100.00"));
			}else 
				for( Element e :  QprofilerXmlUtils.getOffspringElementByTagName(ele, XmlUtils.Svalue) )
					if(e.getAttribute(XmlUtils.Sname).toLowerCase().contains("percent"))
						assertTrue(e.getTextContent().equals("0.00") );
					else
						assertTrue(e.getTextContent().equals("0") );
			 
		
		
	}
	
	@Test
	public void bigRGTest() throws Exception{
		
		String rgid = "1959T"; //here only test the pair from "1959N" 	
		ReadGroupSummary rgSumm = createRGElement(rgid );
		Element root = QprofilerXmlUtils.createRootElement("root",null);
		rgSumm.readSummary2Xml(root);
				
		//must be after readSummary2Xml(root)
		assertTrue(rgSumm.getMaxBases() == 240 ); //2 * maxReadLength
		assertTrue(rgSumm.getCountedReads() == 6); //counted reads is 9-1-1-1 =6		
		
		root = QprofilerXmlUtils.getChildElementByTagName(root, XmlUtils.metricsEle)		
				.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals( "reads" )).findFirst().get() ;
					
		assertTrue( checkChildValue(root,"maxLength", "40")); //max read length is 40 for this group
		
		checkfilteredReads(root, new String[] {"9", "1", "1","1"});		
		//counted reads is 9-1-1-1 =6							
		checkDiscardReadStats(root, "duplicateReads",2, 6 );
		checkDiscardReadStats(root, "unmappedReads", 1,6 );
		checkDiscardReadStats(root, "nonCanonicalPair", 1,6 );
		//total counted read Base is 6*40 = 240
		checkCountedReadStats(root,  "trimmedBase", new String[] {"0","0","0","0","0","0"}, 6, 240, "0.00");
		checkCountedReadStats(root, "softClippedBases", new String[] {"0","0","0","0","0","0"}, 6, 240, "0.00");
		
		//only count hardclip on counted read not from discard reads there are 2 reads with 13 base hard clipped 
		//hard clip base is 13/(40*6) = 0.05416666 = 5.42%
		checkCountedReadStats(root, "hardClippedBases", new String[] {"2","5","8","6","5","5"}, 6, 240, "5.42" );
		//26/240=0.108333=10.83%
		checkCountedReadStats(root, "overlapBases", new String[] {"1","26","26","26","26","0"}, 6, 240, "10.83" );			 

	}
	
	private static boolean checkChildValue(Element parent,String name, String value) {
		 List<Element> eles = QprofilerXmlUtils.getChildElementByTagName(parent, XmlUtils.Svalue);	
		 Element ele = eles.stream().filter( e -> e.getAttribute(XmlUtils.Sname).equals(name)).findFirst().get() ;
		 return ele.getTextContent().equals(value);		
	}
	
	@Test
	public void totalTest() throws Exception {
		createInputFile();
		//overall readgroup should manually  setMaxBases(long);
		Element root = QprofilerXmlUtils.createRootElement("root",null);
		BamSummarizer2 bs = new BamSummarizer2();
		BamSummaryReport2 sr = (BamSummaryReport2) bs.summarize(INPUT_FILE); 
		sr.toXml(root);	
		
		//debug
		QprofilerXmlUtils.asXmlText(root, "/Users/christix/Documents/Eclipse/data/qprofiler/unitTest.xml");
 	 
		root = QprofilerXmlUtils.getOffspringElementByTagName(root, "bamSummary").get(0);

		root = QprofilerXmlUtils.getChildElementByTagName(root, XmlUtils.metricsEle)		
				.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals( "reads" )).findFirst().get() ;
	
	
		assertTrue(root.getChildNodes().getLength() == 14);
		List<Element> valueEles = QprofilerXmlUtils.getOffspringElementByTagName(root, XmlUtils.Svalue);
		assertTrue(valueEles.size() == 42); //less child under trimmed base on summary
		
		//totalBase: 6*40 + 2*50 + 75 = 415
		//totalLostBase: 57.25% *40*6 + 75 + 30 = 245
		assertTrue( checkChildValue(root,"maxLength", "75")); 
		assertTrue( checkChildValue(root,"averageLength", "43")); 		
		assertTrue( checkChildValue(root,"lostBasesPercent", "75.07"));  //???

		
		
		
	}
	
	/**
	 * parse read with specified rgid; it will parse read without readgroupid if the input rgid=QprofilerXmlUtils.UNKNOWN_READGROUP
	 * it will parse every reads if the input rgid is null
	 * @param rgid : readgroup id, allow QprofilerXmlUtils.UNKNOWN_READGROUP and null
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	private ReadGroupSummary createRGElement(String rgid) throws IOException, ParserConfigurationException{
		createInputFile();
		
		ReadGroupSummary rgSumm = new ReadGroupSummary(rgid);		
		SamReader reader = SAMFileReaderFactory.createSAMFileReaderAsStream(new File( INPUT_FILE), null, null);
		for (SAMRecord record : reader) {	
			if(rgid == null)
				rgSumm.parseRecord(record);
			else if( rgid.equals(QprofilerXmlUtils.UNKNOWN_READGROUP) && record.getReadGroup() == null )
				rgSumm.parseRecord(record);	
			else if(record.getReadGroup() != null && record.getReadGroup().getId().equals( rgid)) 
				rgSumm.parseRecord(record);						 	 
		}						
		reader.close();
		
		ReadGroupSummary rgUnknown = new ReadGroupSummary( QprofilerXmlUtils.UNKNOWN_READGROUP);
		
		
		return rgSumm;
	}
		
//		for (int i = 0 ; i < summaryNodes.getLength() ; i++) 
//			if(summaryNodes.item(i).getNodeName().equals( "BaseCount")){
//				 NamedNodeMap node =  summaryNodes.item(i).getAttributes();
//				 String rg = node.getNamedItem("rg").getNodeValue();
//				 switch ( rg) {
//				 	case "1959N" :
//				 		Assert.assertEquals(node.getNamedItem(QprofilerXmlUtils.overlapBases  ).getNodeValue(), "12.00");
//				 		Assert.assertEquals(node.getNamedItem("trimmedBase").getNodeValue(), "13.00%");
//				 		Assert.assertEquals(node.getNamedItem("softClip").getNodeValue(), "5.00%");
//				 		Assert.assertEquals(node.getNamedItem("hardClip").getNodeValue(), "0.00%");
//				 		Assert.assertEquals(node.getNamedItem("totalLost").getNodeValue(), "30.00%");				 		
//				 		Assert.assertEquals(node.getNamedItem("maxLength").getNodeValue(), "50");	
//				 		Assert.assertEquals(node.getNamedItem("totalReads").getNodeValue(), "2");
//				 		break;
//				 	case "1959T" :
//				 		Assert.assertEquals(node.getNamedItem("unmapped").getNodeValue(), "16.67%");	
//				 		Assert.assertEquals(node.getNamedItem("nonCanonicalPair").getNodeValue(), "16.67%");
//				 		Assert.assertEquals(node.getNamedItem("duplicate").getNodeValue(), "33.33%");			 		
//				 		Assert.assertEquals(node.getNamedItem("softClip").getNodeValue(), "0.00%");
//				 		Assert.assertEquals(node.getNamedItem("hardClip").getNodeValue(), "5.42%");
//				 		Assert.assertEquals(node.getNamedItem("trimmedBase").getNodeValue(), "0.00%");				 		
//				 		Assert.assertEquals(node.getNamedItem("overlap").getNodeValue(), "10.83%");				 		
//				 		Assert.assertEquals(node.getNamedItem("totalLost").getNodeValue(), "82.92%");
//				 		Assert.assertEquals(node.getNamedItem("maxLength").getNodeValue(), "40");	
//				 		Assert.assertEquals(node.getNamedItem("totalReads").getNodeValue(), "6");
//				 		break;	
//				 	case QprofilerXmlUtils.UNKNOWN_READGROUP:
//				 		Assert.assertEquals(node.getNamedItem("overlap").getNodeValue(), "0.00%");
//				 		Assert.assertEquals(node.getNamedItem("trimmedBase").getNodeValue(), "0.00%");
//				 		Assert.assertEquals(node.getNamedItem("softClip").getNodeValue(), "0.00%");
//				 		Assert.assertEquals(node.getNamedItem("hardClip").getNodeValue(), "0.00%");
//				 		Assert.assertEquals(node.getNamedItem("totalLost").getNodeValue(), "0.00%");				 		
//				 		Assert.assertEquals(node.getNamedItem("maxLength").getNodeValue(), "75");	
//				 		Assert.assertEquals(node.getNamedItem("totalReads").getNodeValue(), "1");
//				 		break;
//				 	case "Total" :				 		
//				 		Assert.assertEquals(node.getNamedItem("totalReads").getNodeValue(), "9");	
//				 		Assert.assertEquals(node.getNamedItem("softClip").getNodeValue(), "1.20%");				 		
//				 		Assert.assertEquals(node.getNamedItem("unmapped").getNodeValue(), "9.64%");				 		
//				 		Assert.assertEquals(node.getNamedItem("nonCanonicalPair").getNodeValue(), "9.64%");				 		
//				 		Assert.assertEquals(node.getNamedItem("duplicate").getNodeValue(), "19.28%");			 							
//				 		Assert.assertEquals(node.getNamedItem("hardClip").getNodeValue(), "3.13%" );
//				 		Assert.assertEquals(node.getNamedItem("trimmedBase").getNodeValue(), "3.13%" );				 		
//				 		Assert.assertEquals(node.getNamedItem("overlap").getNodeValue(), "9.16%");					 		
//				 		Assert.assertEquals(node.getNamedItem("totalLost").getNodeValue(), "55.18%");
//				 		Assert.assertEquals(node.getNamedItem("maxLength").getNodeValue(), "-");					 					 				 		
//				 		break;	
//				 		
//				 	default:
//				 		throw new Exception("unexpected read group appears on xml SUMMARY section");
//				 }
//			}
	
/*	
	
	List<Element> nodes = QprofilerXmlUtils.getOffspringElementByTagName(root, QprofilerXmlUtils.readGroup) ;	
//	assertTrue(nodes.size() == 3);
	int size = 0;
	for( Element rgEle : nodes ) {
		//debug
		System.out.println((size++) + " th node is " + rgEle.getTagName() + "::"+ rgEle.getAttribute("RGID"));
		
		System.out.println(QprofilerXmlUtils.getChildElementByTagName(rgEle, XmlUtils.metricsEle).get(0).getAttributes().item(0));
		
		
		Element ele = QprofilerXmlUtils.getChildElementByTagName(rgEle, XmlUtils.metricsEle)
				   .stream().filter(e -> e.getAttribute(XmlUtils.Sname).equals("reads")).findFirst().get() ;
 
		if( rgEle.getAttribute(XmlUtils.Srgid).equals("1959T")  ) {
			
		}	
	}
//		}
 * */
	
	private static void createInputFile() throws IOException{
		List<String> data = new ArrayList<String>();
        data.add("@HD	VN:1.0	SO:coordinate");
        data.add("@RG	ID:1959T	SM:eBeads_20091110_CD	DS:rl=50");
        data.add("@RG	ID:1959N	SM:eBeads_20091110_ND	DS:rl=50");
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
		//trimmed, deletion forward   10075 (start~end) 10161
		data.add("243_145_5	99	chr1	10075	6	15M50N22M	=	10100	175	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959N	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");

		//mate reverse   soft clip ???seqbase maybe wrong 10100 (start~end) 10244
		data.add("243_145_5	147	chr1	10100	6	25M100D20M5S	=	10075	-175	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAACACCCTAACCCTAA	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3II##>II$$BIIC3	" +
				"RG:Z:1959N	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
		
		//noRG and unpaired read
		data.add("NS500239:99	16	chr1	7480169	0	75M	*	0	0	AATGAATAGAAGGGTCCAGATCCAGTTCTAATTTGGGGTAGGGACTCAGTTTGTGTTTTTTCACGAGATGAAGAT	" + 
				"EEEA<EEEEEE<<EE/AEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEAEEEEEEEEAEEEEEEEAAAAA	NH:i:14	HI:i:11	AS:i:73	NM:i:0	MD:Z:75	");
		
		try(BufferedWriter out = new BufferedWriter(new FileWriter(INPUT_FILE))){	    
			for (String line : data)  out.write(line + "\n");	               
		}		
	}
	
	private static void createMDerrFile() throws IOException{
		List<String> data = new ArrayList<String>();
        data.add("@HD	VN:1.0	SO:coordinate");
        data.add("@RG	ID:1959T	SM:eBeads_20091110_CD	DS:rl=50");
        data.add("@PG	ID:SOLID-GffToSam	VN:1.4.3");
        data.add("@SQ	SN:chr1	LN:249250621");
        data.add("@SQ	SN:chr11	LN:243199373");
	
	    data.add("HWI-ST1408:8	147	chrM	3085	255	28S96M2S	=	3021	-160	" + 
	    "CNNNNNNNNNNNNNNTNNANNNNNNNNTANNCNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNCNNAAGNACANGAGAAATAAGNCCTACTTCACAAAGCGCCTNCCCCCGNAAANGANN	" + 
	    "########################################################################F==#F==#EGGGGGGE<=#FE1GGGGGGGGGGGGGF=?#GGGE@=#E@?#<=##	" + 
	    "MD:Z:1T0C1A0G0G0T0C0G0G0T0T0T0C0T0A0T0C0T0A0C0N0T0T0C0A0A0A0T0T0C0C0T0C0C0C0T0G0T0A1G0A3G3A10G19T6T3T2	NH:i:1	HI:i:1	NM:i:47	AS:i:169	RG:Z:1959T");
	
		try(BufferedWriter out = new BufferedWriter(new FileWriter(INPUT_FILE))){	    
			for (String line : data)  out.write(line + "\n");	               
		}	
	}
	
	

	
}

