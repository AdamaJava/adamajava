package org.qcmg.qprofiler2.summarise;
 
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
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qprofiler2.bam.BamSummarizer2;
import org.qcmg.qprofiler2.bam.BamSummaryReport2;
import org.qcmg.qprofiler2.summarise.ReadGroupSummary;
import org.qcmg.qprofiler2.util.XmlUtils;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

public class ReadGroupSummary_ReadTest {
	protected static final String INPUT_FILE = "input.sam";

	static void createInputFile() throws IOException{
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
				
		//overlap outward
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

	@After
	public void tearDown() { 	new File(INPUT_FILE).delete(); }
			
	private void checkLostbaseBySum(Element parent) {	 
		double sumPercent = 0, percent = 0;		 
		int sumBase = 0, base = 0;
		for(Element ele: QprofilerXmlUtils.getOffspringElementByTagName(parent, XmlUtils.Svalue)) {
			if(! ele.getParentNode().getNodeName().equals(XmlUtils.variableGroupEle)) {
				if( ele.getAttribute(XmlUtils.Sname).equals( ReadGroupSummary.slostBase )) 
					base = Integer.parseInt(ele.getTextContent());				
				else if( ele.getAttribute(XmlUtils.Sname).equals( QprofilerXmlUtils.lostPercent))
					percent = Double.parseDouble(ele.getTextContent());
				continue;
			}
			if( ele.getAttribute(XmlUtils.Sname).equals( ReadGroupSummary.slostBase ))  
				sumBase += Integer.parseInt(ele.getTextContent());
							 
			else if( ele.getAttribute(XmlUtils.Sname).equals( QprofilerXmlUtils.lostPercent))
				sumPercent += Double.parseDouble(ele.getTextContent());		
		}
		
		assertTrue( sumPercent == percent );
		assertTrue( sumBase == base );
	}
	
	/**
	 * 
	 * @param parent: <sequenceMetrics Name="reads" count="9">
	 * @param counts: array of {totalReads, supplementaryReads, secondaryReads, failedReads}
	 */
	private void checkDiscardReads(Element parent, String... counts) {		
		assertTrue( parent.getAttribute("count").equals(counts[0]) ); //9 reads in this read group
		Element ele1 = QprofilerXmlUtils.getChildElementByTagName(parent, XmlUtils.variableGroupEle)
				   .stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals("discardReads")).findFirst().get() ;				
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
	private Element checkBadReadStats(Element parent, String name, int reads, int base, String percent ){		    
		   Element groupE =  QprofilerXmlUtils.getChildElementByTagName(parent, XmlUtils.variableGroupEle)
				   .stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals(name)).findFirst().get() ;		   
			assertTrue( checkChildValue(groupE,"readCount", String.valueOf(reads)));
			assertTrue( checkChildValue(groupE,"lostBase", String.valueOf(base)));			 		
			assertTrue( checkChildValue(groupE,"basePercent",percent));
			return groupE;
	}
 
	/**
	 * 
	 * @param parent
	 * @param nodeName
	 * @param counts new int[]{reads, min, max, mean, mode, median, lostBase}
	 * @param percent: basePercent
	 */
	private void checkCountedReadStats(Element parent, String nodeName, int[] counts, String percent ) {
		//check readCount		 
		Element groupE =  QprofilerXmlUtils.getChildElementByTagName(parent, XmlUtils.variableGroupEle)
			.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals(nodeName)).findFirst().get() ;		   
		assertTrue( checkChildValue(groupE, ReadGroupSummary.smin, String.valueOf(counts[1] )));
		assertTrue( checkChildValue(groupE, ReadGroupSummary.smax, String.valueOf(counts[2] )));
		assertTrue( checkChildValue(groupE, ReadGroupSummary.smean, String.valueOf(counts[3] )));
		assertTrue( checkChildValue(groupE, ReadGroupSummary.smode, String.valueOf(counts[4] )));	
		assertTrue( checkChildValue(groupE, ReadGroupSummary.smedian, String.valueOf(counts[5] )));
		assertTrue( checkChildValue(groupE, ReadGroupSummary.sreadCount, String.valueOf(counts[0])));		
		assertTrue( checkChildValue(groupE, ReadGroupSummary.slostBase, String.valueOf(counts[6])));		
		assertTrue( checkChildValue(groupE, QprofilerXmlUtils.lostPercent, percent));		
	}

	static boolean checkChildValue(Element parent,String name, String value) {
		 List<Element> eles = QprofilerXmlUtils.getChildElementByTagName(parent, XmlUtils.Svalue);	
		 Element ele = eles.stream().filter( e -> e.getAttribute(XmlUtils.Sname).equals(name)).findFirst().get() ;
		 return ele.getTextContent().equals(value);		
	}
	/**
	 * parse read with specified rgid; it will parse read without readgroupid if the input rgid=QprofilerXmlUtils.UNKNOWN_READGROUP
	 * it will parse every reads if the input rgid is null
	 * @param rgid : readgroup id, allow QprofilerXmlUtils.UNKNOWN_READGROUP and null
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	static ReadGroupSummary createRGElement(String rgid) throws IOException, ParserConfigurationException{
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
		
		return rgSumm;
	}
	
	@Test
	public void rgSmallTest() throws Exception {		
		String rgid = "1959N"; //here only test the pair from "1959N" 	
		ReadGroupSummary rgSumm = createRGElement(rgid );
		Element root = QprofilerXmlUtils.createRootElement("root",null);
		rgSumm.readSummary2Xml( root );
				
		//must be after readSummary2Xml(root)
		assertTrue(rgSumm.getMaxBases() == 100 ); //2 * maxReadLength
		assertTrue(rgSumm.getCountedReads() == 2);
							
		root = QprofilerXmlUtils.getChildElementByTagName(root, XmlUtils.metricsEle)		
			.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals( "reads" )).findFirst().get() ;
				
		assertTrue(root.getChildNodes().getLength() == 14); //includes comments
		List<Element> valueEles = QprofilerXmlUtils.getOffspringElementByTagName(root, XmlUtils.Svalue);
		assertTrue(valueEles.size() == 49);				
		assertTrue( checkChildValue( root, "readMaxLength", "50" )); 
		assertTrue( checkChildValue( root, ReadGroupSummary.sreadCount, "2" ));
		assertTrue( checkChildValue( root, "countedBase", "100" ));
		assertTrue( checkChildValue( root, ReadGroupSummary.slostBase, "30" ));
		assertTrue( checkChildValue( root, QprofilerXmlUtils.lostPercent, "30.00" ));  //304/415
						
		checkDiscardReads(root, new String[] {"2", "0", "0","0"});		
		//counted reads is 9-1-1-1 =6							
		checkBadReadStats(root, "duplicateReads", 0, 0, "0.00");
		checkBadReadStats(root, "unmappedReads", 0, 0, "0.00" );
		checkBadReadStats(root, "nonCanonicalPair", 0, 0, "0.00" );
		//total counted read Base is 2*50=100
		//int[]{reads, min, max, mean, mode, median, lostBase}
		checkCountedReadStats(root, ReadGroupSummary.node_trim , new int[] {1,13,13,13,13,13,13}, "13.00");
		checkCountedReadStats(root, ReadGroupSummary.node_softClip , new int[] {1, 5, 5, 5, 5, 5,5}, "5.00");		
		checkCountedReadStats(root, ReadGroupSummary.node_hardClip  ,new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
		//12/100= 12.00%
		checkCountedReadStats(root, ReadGroupSummary.node_overlap, new int[] {1,12,12,12,12,12,12},"12.00");
		checkLostbaseBySum(root);
	}
	
	@Test 
	public void rgUnkownTest()throws Exception{
		String rgid = QprofilerXmlUtils.UNKNOWN_READGROUP; //here only test the pair from "1959N" 	
		ReadGroupSummary rgSumm = createRGElement(rgid );
		Element root = QprofilerXmlUtils.createRootElement("root",null);
		rgSumm.readSummary2Xml(root);
						
		//must be after readSummary2Xml(root)
		assertTrue(rgSumm.getMaxBases() == 75 ); //0 * maxReadLength
		assertTrue(rgSumm.getCountedReads() == 1); //counted reads is  1					
		root = QprofilerXmlUtils.getChildElementByTagName(root, XmlUtils.metricsEle)		
				.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals( "reads" )).findFirst().get() ;	
				
		assertTrue( checkChildValue( root, "readMaxLength", "75" )); 
		assertTrue( checkChildValue( root, ReadGroupSummary.sreadCount, "1" ));
		assertTrue( checkChildValue( root, "countedBase", "75" ));
		assertTrue( checkChildValue( root, ReadGroupSummary.slostBase, "75" ));
		assertTrue( checkChildValue( root, QprofilerXmlUtils.lostPercent, "100.00" ));  //304/415

		List<Element> valueEles = QprofilerXmlUtils.getOffspringElementByTagName(root, XmlUtils.Svalue);
		assertTrue(valueEles.size() == 49); //less child under trimmed base on summary		
		checkDiscardReads(root, new String[] {"1", "0", "0","0"});
		checkBadReadStats(root, "duplicateReads", 0, 0, "0.00");
		checkBadReadStats(root, "unmappedReads", 0, 0, "0.00" );
		checkBadReadStats(root, "nonCanonicalPair", 1, 75, "100.00" );
		
		//total counted read Base is 6*40 = 240
		checkCountedReadStats(root,  ReadGroupSummary.node_trim , new int[] {0,0,0,0,0,0,0}, "0.00");
		checkCountedReadStats(root, ReadGroupSummary.node_softClip, new int[] {0,0,0,0,0,0,0}, "0.00");
		checkCountedReadStats(root, ReadGroupSummary.node_hardClip, new int[] {0,0,0,0,0,0,0}, "0.00");
		checkCountedReadStats(root, ReadGroupSummary.node_overlap, new int[] {0,0,0,0,0,0,0}, "0.00");		
		
		checkLostbaseBySum(root);
	}
		
	@Test
	public void rgBigTest() throws Exception{
		
		String rgid = "1959T"; //here only test the pair from "1959N" 	
		ReadGroupSummary rgSumm = createRGElement(rgid );
		Element root = QprofilerXmlUtils.createRootElement("root",null);
		rgSumm.readSummary2Xml(root);
				
		//must be after readSummary2Xml(root)
		assertTrue(rgSumm.getMaxBases() == 240 ); //2 * maxReadLength
		assertTrue(rgSumm.getCountedReads() == 6); //counted reads is 9-1-1-1 =6		
		
		root = QprofilerXmlUtils.getChildElementByTagName(root, XmlUtils.metricsEle)		
				.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals( "reads" )).findFirst().get() ;					
		assertTrue( checkChildValue(root,"readMaxLength", "40")); //max read length is 40 for this group
		
		checkDiscardReads(root, new String[] {"9", "1", "1","1"});		
		//counted reads is 9-1-1-1 =6							
		checkBadReadStats(root, "duplicateReads",2, 80, "33.33" );
		checkBadReadStats(root, "unmappedReads", 1,40, "16.67" );
		checkBadReadStats(root, "nonCanonicalPair", 1, 40, "16.67" );
		//total counted read Base is 6*40 = 240
		checkCountedReadStats(root,  ReadGroupSummary.node_trim , new int[] {0,0,0,0,0,0,0},  "0.00");
		checkCountedReadStats(root, ReadGroupSummary.node_softClip, new int[] {0,0,0,0,0,0,0},  "0.00");
		checkCountedReadStats(root, ReadGroupSummary.node_hardClip, new int[] {2,5,8 ,6 ,5,8,13}, "5.42" );
		checkCountedReadStats(root, ReadGroupSummary.node_overlap, new int[] {1 ,26,26,26,26,26,26},"10.83" );			
		checkLostbaseBySum(root);
	}
		
	@Test
	public void overallTest() throws Exception {
		createInputFile();
		//overall readgroup should manually  setMaxBases(long);
		Element root = QprofilerXmlUtils.createRootElement("root",null);
		BamSummarizer2 bs = new BamSummarizer2();
		BamSummaryReport2 sr = (BamSummaryReport2) bs.summarize(INPUT_FILE); 
		sr.toXml(root);	
			 
		root = QprofilerXmlUtils.getOffspringElementByTagName( root, "bamSummary" ).get(0);
		root = QprofilerXmlUtils.getChildElementByTagName( root, XmlUtils.metricsEle )		
				.stream().filter( ele -> ele.getAttribute(XmlUtils.Sname ).equals( "reads" )).findFirst().get() ;		
		assertTrue( checkChildValue( root, "readMaxLength", "75" )); 
		assertTrue( checkChildValue( root, ReadGroupSummary.sreadCount, "9" ));
		assertTrue( checkChildValue( root, "countedBase", "415" ));
		assertTrue( checkChildValue( root, ReadGroupSummary.slostBase, "304" ));
		assertTrue( checkChildValue( root, QprofilerXmlUtils.lostPercent, "73.25" ));  //304/415

		List<Element> valueEles = QprofilerXmlUtils.getOffspringElementByTagName(root, XmlUtils.Svalue);
		assertTrue(valueEles.size() == 44); //less child under trimmed base on summary
		
		checkDiscardReads( root, new String[] {"12", "1", "1","1"});
		checkBadReadStats( root, "duplicateReads", 2, 80, "19.28");
		checkBadReadStats( root, "unmappedReads", 1, 40, "9.64" );
		checkBadReadStats( root, "nonCanonicalPair", 2, 115, "27.71" );
		checkBadReadStats( root, ReadGroupSummary.node_trim , 1, 13, "3.13" );				
		checkCountedReadStats( root, ReadGroupSummary.node_softClip, new int[] { 1,5,5,5,5,5,5 },  "1.20" );
		checkCountedReadStats( root, ReadGroupSummary.node_hardClip, new int[] { 2,5,8,6,5,8,13 }, "3.13" );
		checkCountedReadStats( root, ReadGroupSummary.node_overlap, new int[] { 2,12,26,19,12,26,38 }, "9.16" );			
		checkLostbaseBySum( root ); 
	}
				

	@Test
	/**
	 * there check stats when there are more than three reads
	 * @throws Exception
	 */
	public void lostBaseStatsTest() throws Exception {
		
		ReadGroupSummary_PairTest.createPairInputFile(INPUT_FILE);
		Element root = QprofilerXmlUtils.createRootElement("root",null);
		BamSummarizer2 bs = new BamSummarizer2();
		BamSummaryReport2 sr = (BamSummaryReport2) bs.summarize(INPUT_FILE); 
		sr.toXml(root);	
		
		root = QprofilerXmlUtils.getOffspringElementByTagName(root, "bamSummary").get(0);
		root = QprofilerXmlUtils.getChildElementByTagName(root, XmlUtils.metricsEle)		
				.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals( "reads" )).findFirst().get() ;
		checkCountedReadStats( root, ReadGroupSummary.node_hardClip, new int[] { 4,3,8,5,5,5,21 }, "2.56" );		
		checkCountedReadStats( root, ReadGroupSummary.node_overlap, new int[] { 3,12,26,17,12,13,51 }, "6.22" );	 				
	}

	
}

