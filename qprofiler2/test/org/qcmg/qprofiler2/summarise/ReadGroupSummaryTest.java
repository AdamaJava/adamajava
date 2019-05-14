package org.qcmg.qprofiler2.summarise;
 
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;

import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qprofiler2.bam.BamSummarizer2;
import org.qcmg.qprofiler2.bam.BamSummaryReport2;
import org.qcmg.qprofiler2.summarise.ReadGroupSummary;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.XmlElementUtils;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

public class ReadGroupSummaryTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	File input;

	@Before
	public void setup() throws IOException {
		input = testFolder.newFile("testInputFile.sam");
		createInputFile (input);
	}	
	 
	static void createInputFile(File input) throws IOException{
		List<String> data = new ArrayList<String>();
        data.add("@HD	VN:1.0	SO:coordinate");
        data.add("@RG	ID:1959T	SM:eBeads_20091110_CD	DS:rl=50");
        data.add("@RG	ID:1959N	SM:eBeads_20091110_ND	DS:rl=50");
        data.add("@PG	ID:SOLID-GffToSam	VN:1.4.3");
        data.add("@SQ	SN:chr1	LN:249250621");
        data.add("@SQ	SN:chr11	LN:243199373");

		//unmapped first of pair
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
		
		//count overlap assume both same length so min_end is below read end
		//hard clip both forward but proper pair
		//f3f5: 10075 -------->10111(firstofPair)   10200 ---------->
		data.add("243_146_5	67	chr1	10075	6	3H37M	=	10200	93	" +		 
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
		 			
		//below overlap  62  groupRG:Z:1959N. make sure overlap only calculate once.
		//62 = min_end - max_start +1 = 10161 - 10075 + 1 = 62
		//trimmed, deletion forward   10075 (start~end) 10161
		data.add("243_145_5	99	chr1	10075	6	15M50N22M	=	10100	175	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959N	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");

		//mate reverse   soft clip ???seqbase maybe wrong 10100 (start~end) 10249
		data.add("243_145_5	147	chr1	10100	6	25M100D20M5S	=	10075	-175	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAACACCCTAACCCTAA	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3II##>II$$BIIC3	" +
				"RG:Z:1959N	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
		
		//noRG, unpaired read should be counted but not belong to Pair and no Tlen
		data.add("NS500239:99	16	chr1	7480169	0	75M	*	0	0	AATGAATAGAAGGGTCCAGATCCAGTTCTAATTTGGGGTAGGGACTCAGTTTGTGTTTTTTCACGAGATGAAGAT	" + 
				"EEEA<EEEEEE<<EE/AEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEAEEEEEEEEAEEEEEEEAAAAA	NH:i:14	HI:i:11	AS:i:73	NM:i:0	MD:Z:75	");
		
		try(BufferedWriter out = new BufferedWriter(new FileWriter(input))){	    
			for (String line : data)  out.write(line + "\n");	               
		}		
	}
		
	/**
	 * 
	 * @param ele:<sequenceMetrics name="reads"..>
	 */
	private void checktLen(Element parent, int pairCount, int max, int mean, int mode, int median) {
		Element ele = XmlElementUtils.getChildElementByTagName(parent, XmlUtils.variableGroupEle)
		.stream().filter(e -> e.getAttribute(XmlUtils.Sname).equals("tLen")).findFirst().get() ;	
		assertTrue( checkChildValue( ele, ReadGroupSummary.MAX, max+"" )); 	
		assertTrue( checkChildValue( ele, ReadGroupSummary.MEAN, mean +"")); 	
		assertTrue( checkChildValue( ele, ReadGroupSummary.MODE, mode+"" )); 	
		assertTrue( checkChildValue( ele, ReadGroupSummary.MEDIAN, median +"")); 	
		assertTrue( checkChildValue( ele, ReadGroupSummary.PAIR_COUNT, pairCount+"" )); 		
	}
	
	/**
	 * 
	 * @param parent: <sequenceMetrics Name="reads" count="9">
	 * @param counts: array of {totalReads, supplementaryReads, secondaryReads, failedReads}
	 */
	private void checkDiscardReads(Element parent, int supplementary, int secondary, int failedVendor) {		
		Element ele1 = XmlElementUtils.getChildElementByTagName(parent, XmlUtils.variableGroupEle)
				   .stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals("discardedReads")).findFirst().get() ;				
		assertTrue( checkChildValue(ele1,"supplementaryAlignmentCount", String.valueOf(supplementary)));
		assertTrue( checkChildValue(ele1,"secondaryAlignmentCount", String.valueOf(secondary)));
		assertTrue( checkChildValue(ele1,"failedVendorQualityCount", String.valueOf(failedVendor)));
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
		   Element groupE =  XmlElementUtils.getChildElementByTagName(parent, XmlUtils.variableGroupEle)
				   .stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals(name)).findFirst().get() ;		   
			assertTrue( checkChildValue(groupE,"readCount", String.valueOf(reads)));
			assertTrue( checkChildValue(groupE,"basesLostCount", String.valueOf(base)));			 		
			assertTrue( checkChildValue(groupE,"basesLostPercent",percent));
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
		Element groupE =  XmlElementUtils.getChildElementByTagName(parent, XmlUtils.variableGroupEle)
			.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals(nodeName)).findFirst().get() ;		   
		assertTrue( checkChildValue(groupE, ReadGroupSummary.MIN, String.valueOf(counts[1] )));
		assertTrue( checkChildValue(groupE, ReadGroupSummary.MAX, String.valueOf(counts[2] )));
		assertTrue( checkChildValue(groupE, ReadGroupSummary.MEAN, String.valueOf(counts[3] )));
		assertTrue( checkChildValue(groupE, ReadGroupSummary.MODE, String.valueOf(counts[4] )));	
		assertTrue( checkChildValue(groupE, ReadGroupSummary.MEDIAN, String.valueOf(counts[5] )));
		assertTrue( checkChildValue(groupE, ReadGroupSummary.READ_COUNT, String.valueOf(counts[0])));		
		assertTrue( checkChildValue(groupE, ReadGroupSummary.BASE_LOST_COUNT, String.valueOf(counts[6])));		
		assertTrue( checkChildValue(groupE, ReadGroupSummary.BASE_LOST_PERCENT, percent));		
	}
			
	public static boolean checkChildValue(Element parent,String name, String value) {
		 List<Element> eles = XmlElementUtils.getChildElementByTagName(parent, XmlUtils.Svalue);	
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
	private ReadGroupSummary createRGElement(String rgid) throws IOException, ParserConfigurationException{
		
		ReadGroupSummary rgSumm = new ReadGroupSummary(rgid);		
		SamReader reader = SAMFileReaderFactory.createSAMFileReaderAsStream(input, null, null);
		for (SAMRecord record : reader) {	
			if(rgid == null)
				rgSumm.parseRecord(record);
			else if( rgid.equals(XmlUtils.UNKNOWN_READGROUP) && record.getReadGroup() == null )
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
		final Element root = XmlElementUtils.createRootElement("root",null);
		rgSumm.readSummary2Xml( root );
		
						
		//must be after readSummary2Xml(root)
		assertTrue(rgSumm.getReadCount() == 2);
		
		//<sequenceMetrics name="baseLost">
		Element root1 = XmlElementUtils.getChildElementByTagName(root, XmlUtils.metricsEle)		
				.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals( "basesLost" )).findFirst().get() ;	
		checkBadReadStats(root1, "duplicateReads", 0, 0, "0.00");
		checkBadReadStats(root1, "unmappedReads", 0, 0, "0.00" );
		checkBadReadStats(root1, ReadGroupSummary.NODE_NOT_PROPER_PAIR, 0, 0, "0.00" );
		checkBadReadStats(root1, "trimmedBases", 1, 13, "13.00" );
		checkCountedReadStats(root1, ReadGroupSummary.NODE_SOFTCLIP , new int[] {1, 5, 5, 5, 5, 5,5}, "5.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_HARDCLIP  ,new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_OVERLAP, new int[] {1,62,62,62,62,62,62},"62.00");
		
		
		//<sequenceMetrics name="reads" readCount="2">					
		root1 = XmlElementUtils.getChildElementByTagName(root, XmlUtils.metricsEle)		
			.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals( "reads" )).findFirst().get() ;		
		assertTrue( root1.getAttribute(ReadGroupSummary.READ_COUNT).equals("2"));
		checkDiscardReads(root1, 0,0,0);
		
		//check readCount
		Element groupE =  XmlElementUtils.getChildElementByTagName(root1, XmlUtils.variableGroupEle)
				.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals(ReadGroupSummary.NODE_READ_LENGTH)).findFirst().get() ;		   					
		assertTrue( checkChildValue( groupE, ReadGroupSummary.MAX, "50" )); 	
		assertTrue( checkChildValue( groupE, ReadGroupSummary.MEAN, "43" )); 
		
		groupE =  XmlElementUtils.getChildElementByTagName(root1, XmlUtils.variableGroupEle)
				.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals("countedReads")).findFirst().get() ;	
		assertTrue( checkChildValue( groupE, ReadGroupSummary.UNPAIRED_READ, "0" ));
		assertTrue( checkChildValue( groupE, ReadGroupSummary.READ_COUNT, "2" )); 
		assertTrue( checkChildValue( groupE, ReadGroupSummary.BASE_LOST_COUNT, "80" ));
		//here overlapped base is more than real base number since it alignment end may include skipping/deletion base
		assertTrue( checkChildValue( groupE, ReadGroupSummary.BASE_LOST_PERCENT, "80.00" ));   
		assertTrue( checkChildValue( groupE, ReadGroupSummary.BASE_COUNT, "100" ));
		
	}
	
	@Test 
	public void rgUnkownTest()throws Exception{
		String rgid = XmlUtils.UNKNOWN_READGROUP; //here only test the pair from "1959N" 	
		ReadGroupSummary rgSumm = createRGElement(rgid );
		Element root = XmlElementUtils.createRootElement("root",null);
		rgSumm.readSummary2Xml(root);
		//must be after readSummary2Xml(root)
		assertTrue(rgSumm.getReadCount() == 1); //counted reads is  1	
		
		//<sequenceMetrics name="baseLost">
		Element root1 = XmlElementUtils.getChildElementByTagName(root, XmlUtils.metricsEle)		
				.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals( "basesLost" )).findFirst().get() ;	
		checkBadReadStats(root1, "duplicateReads", 0, 0, "0.00");
		checkBadReadStats(root1, "unmappedReads", 0, 0, "0.00" );
		checkBadReadStats(root1, "trimmedBases", 0, 0, "0.00" );
		checkBadReadStats(root1, ReadGroupSummary.NODE_NOT_PROPER_PAIR, 0, 0, "0.00" );
		checkCountedReadStats(root1, ReadGroupSummary.NODE_SOFTCLIP , new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_HARDCLIP  ,new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_OVERLAP, new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
				
		//<sequenceMetrics name="reads" readCount="2">					
		root1 = XmlElementUtils.getChildElementByTagName(root, XmlUtils.metricsEle)		
			.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals( "reads" )).findFirst().get() ;	
		checktLen(root1, 0, 0,  0, 0,0);
		assertTrue( root1.getAttribute(ReadGroupSummary.READ_COUNT).equals("1"));
		checkDiscardReads(root1, 0,0,0);
		
		//check readCount
		Element groupE =  XmlElementUtils.getChildElementByTagName(root1, XmlUtils.variableGroupEle)
				.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals(ReadGroupSummary.NODE_READ_LENGTH)).findFirst().get() ;		  		
		assertTrue( checkChildValue( groupE, ReadGroupSummary.MAX, "75" )); 	
		assertTrue( checkChildValue( groupE, ReadGroupSummary.MEAN, "75" )); 	
		
		groupE =  XmlElementUtils.getChildElementByTagName(root1, XmlUtils.variableGroupEle)
				.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals("countedReads")).findFirst().get() ;			
		assertTrue( checkChildValue( groupE, ReadGroupSummary.READ_COUNT, "1" )); 
		assertTrue( checkChildValue( groupE, ReadGroupSummary.UNPAIRED_READ, "1" ));
		assertTrue( checkChildValue( groupE, ReadGroupSummary.BASE_LOST_COUNT, "0" ));
		//here overlapped base is more than real base number since it alignment end may include skipping/deletion base
		assertTrue( checkChildValue( groupE, ReadGroupSummary.BASE_LOST_PERCENT, "0.00" ));   
		assertTrue( checkChildValue( groupE, ReadGroupSummary.BASE_COUNT, "75" ));		
		
		
	}
		
	@Test
	public void rgBigTest() throws Exception{
		
		String rgid = "1959T";  
		ReadGroupSummary rgSumm = createRGElement(rgid );
		Element root = XmlElementUtils.createRootElement("root",null);
		rgSumm.readSummary2Xml(root);				
		assertTrue(rgSumm.getReadCount() == 6); //counted reads is 9-1-1-1 =6			
				
		//<sequenceMetrics name="baseLost">
		Element root1 = XmlElementUtils.getChildElementByTagName(root, XmlUtils.metricsEle)		
				.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals( "basesLost" )).findFirst().get() ;	
		checkBadReadStats(root1, "duplicateReads", 2, 80, "33.33"); 
		checkBadReadStats(root1, "unmappedReads", 1, 40, "16.67" );
		checkBadReadStats(root1, "trimmedBases", 0, 0, "0.00" );
		checkBadReadStats(root1, ReadGroupSummary.NODE_NOT_PROPER_PAIR, 0, 0, "0.00" );
		checkCountedReadStats(root1, ReadGroupSummary.NODE_SOFTCLIP , new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_OVERLAP, new int[] {1 ,26,26,26,26,26,26},"10.83" );	
		checkCountedReadStats(root1, ReadGroupSummary.NODE_HARDCLIP, new int[] {3,3,8 ,5 ,3,5,16}, "6.67" );			
				
		//<sequenceMetrics name="reads" readCount="2">					
		root1 = XmlElementUtils.getChildElementByTagName(root, XmlUtils.metricsEle)		
			.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals( "reads" )).findFirst().get() ;	
		checktLen(root1,2, 93,  59, 26,93);
		assertTrue( root1.getAttribute(ReadGroupSummary.READ_COUNT).equals("9"));
		checkDiscardReads(root1, 1,1,1);
		
		//check readCount
		Element groupE =  XmlElementUtils.getChildElementByTagName(root1, XmlUtils.variableGroupEle)
				.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals(ReadGroupSummary.NODE_READ_LENGTH)).findFirst().get() ;		   					
		assertTrue( checkChildValue( groupE, ReadGroupSummary.MAX, "40" )); 	
		assertTrue( checkChildValue( groupE, ReadGroupSummary.MEAN, "39" )); 
		
		groupE =  XmlElementUtils.getChildElementByTagName(root1, XmlUtils.variableGroupEle)
				.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals("countedReads")).findFirst().get() ;		
		assertTrue( checkChildValue( groupE, ReadGroupSummary.READ_COUNT, "6" )); 
		assertTrue( checkChildValue( groupE, ReadGroupSummary.UNPAIRED_READ, "0" ));
		assertTrue( checkChildValue( groupE, ReadGroupSummary.BASE_LOST_COUNT, "162" ));
		//here overlapped base is more than real base number since it alignment end may include skipping/deletion base
		assertTrue( checkChildValue( groupE, ReadGroupSummary.BASE_LOST_PERCENT, "67.50" ));   
		assertTrue( checkChildValue( groupE, ReadGroupSummary.BASE_COUNT, "240" ));				
		
	}
		
	@Test
	public void overallTest() throws Exception {
		//overall readgroup should manually  setMaxBases(long);
		Element root = XmlElementUtils.createRootElement("root",null);
		BamSummarizer2 bs = new BamSummarizer2();
		//BamSummarizer2 bs = new BamSummarizer2( 200, null, true);
		BamSummaryReport2 sr = (BamSummaryReport2) bs.summarize(input.getAbsolutePath()); 
		sr.toXml(root);	

		root = XmlElementUtils.getOffspringElementByTagName( root, "bamSummary" ).get(0);
		Element root1 = XmlElementUtils.getChildElementByTagName( root, XmlUtils.metricsEle )		
		.stream().filter( ele -> ele.getAttribute(XmlUtils.Sname ).equals( "summary2" )).findFirst().get() ;	
		assertTrue( checkChildValue( root1, ReadGroupSummary.READ_COUNT, "9" ));
		assertTrue( checkChildValue( root1, ReadGroupSummary.BASE_COUNT , "415" ));	
		//duplicate 80/415=19.28
		assertTrue( checkChildValue( root1, StringUtils.getJoinedString( ReadGroupSummary.NODE_DUPLICATE,   ReadGroupSummary.BASE_LOST_PERCENT, "_"), "19.28" ) ); //80/415
		assertTrue( checkChildValue( root1, StringUtils.getJoinedString( ReadGroupSummary.NODE_UNMAPPED, ReadGroupSummary.BASE_LOST_PERCENT, "_"), "9.64" ));  //40/415
		assertTrue( checkChildValue( root1, StringUtils.getJoinedString( ReadGroupSummary.NODE_NOT_PROPER_PAIR, ReadGroupSummary.BASE_LOST_PERCENT, "_"), "0.00" ));  //0/415
		assertTrue( checkChildValue( root1, StringUtils.getJoinedString( ReadGroupSummary.NODE_TRIM , ReadGroupSummary.BASE_LOST_PERCENT, "_"), "3.13" ));  //13/415
		assertTrue( checkChildValue( root1, StringUtils.getJoinedString( ReadGroupSummary.NODE_SOFTCLIP, ReadGroupSummary.BASE_LOST_PERCENT, "_"), "1.20" ));  //5/415	 
		assertTrue( checkChildValue( root1, StringUtils.getJoinedString( ReadGroupSummary.NODE_HARDCLIP, ReadGroupSummary.BASE_LOST_PERCENT, "_"), "3.86" ));  //16/415
		assertTrue( checkChildValue( root1, StringUtils.getJoinedString( ReadGroupSummary.NODE_OVERLAP , ReadGroupSummary.BASE_LOST_PERCENT, "_"), "21.20" ));  //88/415	   		
			
		root1 = XmlElementUtils.getChildElementByTagName( root, XmlUtils.metricsEle )		
				.stream().filter( ele -> ele.getAttribute(XmlUtils.Sname ).equals( "summary1" )).findFirst().get() ;
		assertTrue( checkChildValue( root1, "Number of cycles with greater than 1% mismatches", "0" ));  
		assertTrue( checkChildValue( root1, "Average length of first-of-pair reads", "36" )); // (37+35+37)/3
		assertTrue( checkChildValue( root1, "Average length of second-of-pair reads", "41" )); // (32+50)/2
		assertTrue( checkChildValue( root1, "Discarded reads (FailedVendorQuality, secondary, supplementary)", "3" ));  	
		assertTrue( checkChildValue( root1, "Total reads including discarded reads", "12" )); // 
	}				

	@Test
	/**
	 * test some invalid reads, such as 
	 * ST-E00110:380:H3NCKCCXY:3:2220:10084:38684	117	chrY	239007	0	*	=	239007	0	*	*	PG:Z:MarkDuplicates	RG:Z:c9516885-22af-4fbc-8acb-1dafeca5925d	AS:i:0	XS:i:0
 	 * ST-E00110:380:H3NCKCCXY:3:2120:3752:45329	69	chrY	239631	0	*	=	239631	0	*	*	PG:Z:MarkDuplicates	RG:Z:c9516885-22af-4fbc-8acb-1dafeca5925d	AS:i:0	XS:i:0
	 */
	public  void unMappedReadTest() throws Exception {
		SAMRecord record = new SAMRecord(null);
		record.setAlignmentStart(239007);
		record.setReferenceName("chrY");
				
		ReadGroupSummary rgSumm = new ReadGroupSummary(null);
		for(int flag : new int[] {117, 69, 181}) {
			record.setFlags(flag);
			rgSumm.parseRecord(record);
		}
		//add one more read with seq to avoid max lenght is zero
		record.setReadBases(new byte[] {1,2,3,4,5,6,7});
		rgSumm.parseRecord(record);
		
		Element root = XmlElementUtils.createRootElement("root",null);
		rgSumm.readSummary2Xml(root);
		
		//<sequenceMetrics name="basesLost">
		Element	root1 = XmlElementUtils.getChildElementByTagName(root, XmlUtils.metricsEle)		
						.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals( "basesLost" )).findFirst().get() ;	
		checkCountedReadStats(root1, ReadGroupSummary.NODE_SOFTCLIP , new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_HARDCLIP , new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_TRIM , new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
		
		//<sequenceMetrics name="reads"
		root1  = XmlElementUtils.getChildElementByTagName(root, XmlUtils.metricsEle)		
				.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals( "reads" )).findFirst().get() ;	
		assertTrue( root1.getAttribute(ReadGroupSummary.READ_COUNT).equals("4"));
		checktLen(root1,0, 0,  0, 0,0);		
		checkDiscardReads(root1, 0,0,0);
		
		//<variableGroup name="countedReads">
		root1 = XmlElementUtils.getChildElementByTagName(root1, XmlUtils.variableGroupEle)
				.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals("countedReads")).findFirst().get() ;	
		assertTrue( checkChildValue( root1, ReadGroupSummary.READ_COUNT, "4" )); 
		assertTrue( checkChildValue( root1, ReadGroupSummary.UNPAIRED_READ, "0" ));
		assertTrue( checkChildValue( root1, ReadGroupSummary.BASE_LOST_PERCENT, "100.00" ));   
		assertTrue( checkChildValue( root1, ReadGroupSummary.BASE_COUNT, "28" ));		
		assertTrue( checkChildValue( root1, ReadGroupSummary.BASE_LOST_COUNT, "28" ));		
	}
	
	@Test
	public void noSeqReadTest() throws Exception {
		SAMRecord record = new SAMRecord(null);
		record.setAlignmentStart(239007);
		record.setReferenceName("chrY");

		ReadGroupSummary rgSumm = new ReadGroupSummary(null);
		//64: unpaired read, 65: not proper pair read
		for(int flag : new int[] {64, 65}) {
			record.setFlags(flag);
			rgSumm.parseRecord(record);
		}	
		//add one more read with seq to avoid max lenght is zero
		record.setReadBases(new byte[] {1,2,3,4,5,6,7});
		rgSumm.parseRecord(record);
		
		Element root = XmlElementUtils.createRootElement("root",null);
		rgSumm.readSummary2Xml(root);
		
		XmlElementUtils.asXmlText(root, "/Users/christix/Documents/Eclipse/data/qprofiler/bam/20190510/test.xml");	
		
		Element	root1 = XmlElementUtils.getOffspringElementByTagName( root, XmlUtils.variableGroupEle)
				.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals("countedReads")).findFirst().get() ;
		assertTrue( checkChildValue( root1, ReadGroupSummary.READ_COUNT, "3" )); 
		assertTrue( checkChildValue( root1, ReadGroupSummary.UNPAIRED_READ, "1" ));
		assertTrue( checkChildValue( root1, ReadGroupSummary.BASE_LOST_PERCENT, "100.00" ));   
		assertTrue( checkChildValue( root1, ReadGroupSummary.BASE_COUNT, "21" ));		
		assertTrue( checkChildValue( root1, ReadGroupSummary.BASE_LOST_COUNT, "21" ));		
		
		
		//<sequenceMetrics name="basesLost">
		root1 = XmlElementUtils.getChildElementByTagName(root, XmlUtils.metricsEle)		
					.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals( "basesLost" )).findFirst().get() ;	
		checkCountedReadStats(root1, ReadGroupSummary.NODE_SOFTCLIP , new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_HARDCLIP , new int[] {0, 0, 0, 0, 0,0,0}, "0.00");
		checkCountedReadStats(root1, ReadGroupSummary.NODE_TRIM , new int[] {1, 7,7, 7, 7,7,7}, "33.33");		
	}
	
	
}

