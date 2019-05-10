package org.qcmg.qprofiler2.summarise;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.util.XmlElementUtils;
import org.qcmg.qprofiler2.bam.BamSummarizer2;
import org.qcmg.qprofiler2.bam.BamSummaryReport2;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;

import htsjdk.samtools.SAMRecord;

public class PairSummaryTest {
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	public File input;

	@Before
	public void setup() throws IOException {
		input = testFolder.newFile("testInputFile.sam");
	}	
	
	@Test
	public void zeroMinusTlen() {
		PairSummary pairS = new PairSummary(PairSummary.Pair.Others, false );
		SAMRecord recorda = new SAMRecord(null);	
		recorda.setInferredInsertSize(-1000 );
		assertTrue(PairSummary.getOverlapBase(recorda) == 0); 
		recorda.setInferredInsertSize(-1 );
		assertTrue(PairSummary.getOverlapBase(recorda) == 0); 
		try {
			pairS.parse(recorda);
			fail("expect to throw exception");
		}catch(Exception e) {}
		
		
		//set tLen == 0, any second of pair
		recorda.setInferredInsertSize( 0 );
		for(int flag : new int[] {129,131,147, 179}) {
			recorda.setFlags(flag);
			assertTrue(PairSummary.getOverlapBase(recorda) == 0); 
			pairS.parse(recorda);
		}
		
		//ST-E00180:52:H5LNMCCXX:1:1116:24274:5247 113 chr1 27977105 60 5S145M = 27977205 0
		//<-----------| F3F5 reverse firstOfPaie
		//    <-------|
		recorda.setFlags(113);
		recorda.setCigarString("5S145M");
		recorda.setAlignmentStart(27977105);
		recorda.setMateAlignmentStart(27977205);
		assertTrue(PairSummary.getOverlapBase(recorda) == 27977250 - 27977205); 
		pairS.parse(recorda);
		
		//ST-E00180:52:H5LNMCCXX:1:2103:1720:24691 113 chr1 27775356 39 117S33M = 27775354 0
		//    <-------|  F5F3 reverse firstOfPaie
		//<-----------|
		recorda.setCigarString( "117S33M" );
		recorda.setAlignmentStart( 27775356 );
		recorda.setMateAlignmentStart( 27775354 );
		assertTrue(PairSummary.getOverlapBase(recorda) == 27775389 - 27775356); 
		pairS.parse(recorda);
		
		//ST-E00180:52:H5LNMCCXX:1:2101:31703:21983 65 chr1 34492938 60 95M55S = 34492938 0   
		//F5F3 forward firstOfPaie
		//s1 |--------------> e1			s1 |---------> e1
		//s2 |------------> e2		or 		s2 |------------> e2
		recorda.setFlags(65);
		recorda.setCigarString("95M55S");
		recorda.setAlignmentStart(34492938);
		recorda.setMateAlignmentStart(34492938);
		assertTrue(PairSummary.getOverlapBase(recorda) == 95); 
		pairS.parse(recorda);

		
		//ST-E00180:52:H5LNMCCXX:1:1205:9993:36522 97 chr1 121484553 43 149M = 121484404 0   
		//outward firstOfPair   <-------|-------->
		recorda.setFlags(97);
		recorda.setCigarString("95M55S");
		recorda.setAlignmentStart(121484553);
		recorda.setMateAlignmentStart(121484404);
		assertTrue(PairSummary.getOverlapBase(recorda) == 0); 
		pairS.parse(recorda);
		
		assertTrue( pairS.getFirstOfPairCounts() == 4 );
//		assertTrue( pairS.getPairCounts() == 4 );
		assertTrue( pairS.near.get() == 1 ); //only one without overlap		
		assertTrue( pairS.tLenOverall.get(0) == 4 ); 
		
		assertTrue( pairS.getoverlapCounts().get(0) == 0 );
		assertTrue( pairS.getoverlapCounts().get(33) == 1 );
		assertTrue( pairS.getoverlapCounts().get(45) == 1 );
		assertTrue( pairS.getoverlapCounts().get(95) == 1 );
		
	}
	
	@Test
	public void isOverlapF5F3() {
		//read   : first of pair 	   |---->  or  <------|      
		//read   : second of pair  |---->      or  	        <-----|    
		SAMRecord recorda = new SAMRecord(null);		
		
		//second Of Pair with positive tlen, and both forward
		//ST-E00180:52:H5LNMCCXX:1:2111:15616:38526 129 chr1 56131814 60 150M = 56131947 134 		
		recorda.setFlags(129);
		recorda.setInferredInsertSize(134 );
		recorda.setCigarString("150M");
		recorda.setAlignmentStart(56131814);
		recorda.setMateAlignmentStart(56131947);
		assertTrue(PairSummary.getOverlapBase(recorda) == 56131964 - 56131947); 		
						
		//now set to first of mapped pair, both reverse 
		recorda.setFlags(115);
		recorda.setAlignmentStart(100);
		recorda.setMateAlignmentStart(119);
		//reverse mate end - reverse read end = (119+149) - (100 + 149) + 1 = 20 (just example)
		recorda.setInferredInsertSize(20);
		//overlap = read_ends - max_starts = 249 - 119 + 1 = 131
		assertTrue(PairSummary.getOverlapBase(recorda) == 131);
		 		
		//now set to second of mapped pair, both forward 
		recorda.setFlags(131);
		assertEquals( PairSummary.getPairType(recorda), PairSummary.Pair.F5F3 );
		assertTrue(PairSummary.getOverlapBase(recorda) == 131);
		
		//when there is no overlap
		recorda.setMateAlignmentStart(250);
		recorda.setInferredInsertSize(151);
		assertTrue(PairSummary.getOverlapBase(recorda) == 0);			
		recorda.setMateAlignmentStart(260);
		assertTrue(PairSummary.getOverlapBase(recorda) < 0);	
	}
		
	@Test
	public void isOverlapF3F5() {
		//read   : first of pair    |---->        or  	   <------|      
		//read   : second of pair        |---->   or  <-----|    
		SAMRecord recorda = new SAMRecord(null);
		// second of mapped pair, both forward 
		recorda.setFlags(131);		
		recorda.setAlignmentStart(120);
		recorda.setMateAlignmentStart(100);
		assertFalse( PairSummary.getPairType(recorda).equals(PairSummary.Pair.F5F3 ));
		assertEquals( PairSummary.getPairType(recorda), PairSummary.Pair.F3F5 );
		
		//only count overlap on reads with positive tlen
		recorda.setInferredInsertSize(-20);		//100-120	
		assertTrue(PairSummary.getOverlapBase(recorda) == 0);
				
		//first pair  fully overlap with tLen == 0 since both forward orientation
		recorda.setCigarString("75M");
		//first of mapped pair, both forward
		recorda.setFlags(65);
		recorda.setAlignmentStart(7480169);
		recorda.setMateAlignmentStart(7480169);		
		assertEquals( PairSummary.getPairType(recorda), PairSummary.Pair.F3F5 );
		//same start first of pair
		recorda.setInferredInsertSize(0);	
		assertTrue(PairSummary.getOverlapBase(recorda) == 75); 
		//same start second of pair
		recorda.setFlags(179);
		assertTrue(PairSummary.getOverlapBase(recorda) == 0); 
		
		//now set to second of mapped pair, both reverse 
		//ST-E00180:52:H5LNMCCXX:1:1116:24274:5247 113 chr1 27977105 60 5S145M = 27977205 0 
		recorda.setAlignmentStart(105);
		recorda.setMateAlignmentStart(205);
		assertEquals( PairSummary.getPairType(recorda), PairSummary.Pair.F3F5 );
		
		//reverse mate end - reverse read end = (120+21-1) - (100 + 21-1) = 20 (just example)
		recorda.setInferredInsertSize(20);	
		recorda.setCigarString("121M"); //set cigar, read end is 100 = 25 -1
		//min_end - max_starts = (105+121-1) - 205, since cigar is nothing
		assertTrue(PairSummary.getOverlapBase(recorda) == 21); 		 
		
		// 105 <------------|225
		// 		205 <-------|225		
		recorda.setInferredInsertSize(0);
		//second of pair overlap ignor if tLen = 0
		assertTrue(PairSummary.getOverlapBase(recorda) == 0); 
		//first of pair overlap >= 0 if tLen = 0
		recorda.setFlags(115);
		assertTrue(PairSummary.getOverlapBase(recorda) == 21); 
	}
	
	@Test
	public void isOverlapInward() {
//		PairSummary pairS = new PairSummary(PairSummary.Pair.Inward, true );	
		// |--------->    <---------|		
		SAMRecord recorda = new SAMRecord(null);		
		// second of mapped pair, read reverse, mate forward
		recorda.setFlags(147);
		recorda.setAlignmentStart(120);
		recorda.setMateAlignmentStart(100);
		assertEquals( PairSummary.getPairType(recorda), PairSummary.Pair.Inward );
	
		//100-120 when read length is 0
		recorda.setInferredInsertSize(-20);	
		//set overlap  = 0 sinve tLen  < 0
		assertTrue(PairSummary.getOverlapBase(recorda) == 0);
		
		// first of mapped pair, read reverse, mate forward
		recorda.setFlags(83);
		//inward disregards first or second
		assertEquals( PairSummary.getPairType(recorda), PairSummary.Pair.Inward );
		assertTrue(PairSummary.getOverlapBase(recorda) ==0);
		 

		// first of mapped pair, read forward, mate reverse
		recorda.setFlags(99);
		recorda.setAlignmentStart(100);
		recorda.setMateAlignmentStart(120);
		assertEquals( PairSummary.getPairType(recorda), PairSummary.Pair.Inward );
		
		//120-100 when read length is 0
		recorda.setInferredInsertSize(20);
		//min_ends - max_starts = 99 -120 (cigar string is nothing)
		assertTrue(PairSummary.getOverlapBase(recorda) == -20);

		//120+20-100 when read length is 20
		recorda.setInferredInsertSize(40);
		//min_ends - max_starts = 99 -120 (cigar string is nothing)
		assertTrue(PairSummary.getOverlapBase(recorda) == -20);
		//min_ends - max_starts = min(140, 125) -120 =25
		
		
		// 100|--------->124
		//       120 <----|	140			 
		recorda.setCigarString("25M");
		//min_ends - max_starts = 124 -120+1 (end = start + cigar.M-1)
		assertTrue(PairSummary.getOverlapBase(recorda) == 5);
		assertEquals( PairSummary.getPairType(recorda), PairSummary.Pair.Inward  );
		
		// 100|--------------------->149
		//       120<----|139		
		// first of mapped pair, read forward, mate reverse
		recorda.setCigarString("50M");
		assertEquals( PairSummary.getPairType(recorda), PairSummary.Pair.Inward );
		//min_ends - max_starts = 140 -120 (read end = start + cigar.M-1 = 149. mateEnd2= readStart-1+tLen = 139)
		assertTrue(PairSummary.getOverlapBase(recorda) == 20);
		
	}
		
	@Test
	public void isOverlapOutward() {

		SAMRecord recorda = new SAMRecord(null);			
		// <---------|(mate)    |--------->(read)	
		// first of mapped pair, read forward, mate reverse
		recorda.setFlags(99);
		recorda.setAlignmentStart(120);
		recorda.setMateAlignmentStart(100);
		assertEquals( PairSummary.getPairType(recorda), PairSummary.Pair.Outward);
		assertTrue(PairSummary.getOverlapBase(recorda) == 0); //tLen == 0
		recorda.setInferredInsertSize(-10); //tLen < 0
		assertTrue(PairSummary.getOverlapBase(recorda) == 0);		
		
		//100 <---------------|150 mate
		//          120|---->124 read
		//set mate length = 50; tlen = (100+50) - 119 = 31
		recorda.setInferredInsertSize(31);
		recorda.setCigarString("5M");
		assertEquals( PairSummary.getPairType(recorda), PairSummary.Pair.Outward);
		//min(120-1+31, 120+5-1) - max(100, 120) +1= 5
		assertTrue(PairSummary.getOverlapBase(recorda) == 5);
			
		// 100<----|104(read)    120|--------->(mate)	
		recorda.setFlags(83);
		recorda.setAlignmentStart(100);
		recorda.setMateAlignmentStart(120);
		assertEquals( PairSummary.getPairType(recorda), PairSummary.Pair.Outward);
		//since readlength = 5, tlen = 120-104 + 1
		recorda.setInferredInsertSize(17);
		//overlap = min_ends - max_start + 1= min(104, 120+?) - 120 + 1
		assertTrue(PairSummary.getOverlapBase(recorda) == -15);
		
	}
	
	@Test
	public void toSummaryXmlTest() throws Exception {	
		
		Element root = createPairRoot(input);		
		List<Element> pairEles =  XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.metricsEle)	
				.stream().filter(e -> e.getAttribute(XmlUtils.Sname).equals( "properPairs" )).collect(Collectors.toList());	
		
		//only one inward pair but overlapped
		Element ele = pairEles.stream().filter(e -> ( (Element) e.getParentNode()).getAttribute(XmlUtils.Sname).equals("1959N")).findFirst().get(); 		
		checkVariableGroup( ele, "Inward", new int[] {1,0,0,0,1,1,1, 0, 0,175} );

		//five pairs
		ele = pairEles.stream().filter(e -> ( (Element) e.getParentNode()).getAttribute(XmlUtils.Sname).equals("1959T")).findFirst().get();	
		checkVariableGroup( ele, "F5F3", new int[] {0,0,0,1,1,1,0, 0, 0,2025} ); //tlen=11205, 2015
		checkVariableGroup( ele, "F3F5", new int[] {0,1,1,0,1,1,1,0, 0,93} ); //paircounts only for number of firstOfpair
		checkVariableGroup( ele, "Outward", new int[] {2,0,0,0,2,1,3,0,0,13} );
				
		ele = pairEles.stream().filter(e -> ( (Element) e.getParentNode()).getAttribute(XmlUtils.Sname).equals(XmlUtils.UNKNOWN_READGROUP)).findFirst().get();	
		checkVariableGroup( ele, "Inward", new int[] {1,0,0,0,1,1,0,0,0,76 } );		
			
		//notProperPair
		pairEles =  XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.metricsEle)	
				.stream().filter(e  -> e .getAttribute(XmlUtils.Sname).equals( "notProperPairs" )).collect(Collectors.toList());
		ele = pairEles.stream().filter(e -> ( (Element) e.getParentNode()).getAttribute(XmlUtils.Sname).equals(XmlUtils.UNKNOWN_READGROUP)).findFirst().get();	
		checkVariableGroup( ele, "F3F5", new int[] {1,0,0,0,1,1,0,0,0, 0} ); //notProperPair
		
		
	}
	
	private void checkVariableGroup(Element root, String name, int[] counts ) {
		
		Element variableEle = XmlElementUtils.getChildElementByTagName(root, XmlUtils.variableGroupEle).stream()
			.filter(e -> e.getAttribute(XmlUtils.Sname).equals(name) ).findFirst().get();
		
		List<Element> childEles = XmlElementUtils.getChildElementByTagName(variableEle, XmlUtils.Svalue);
		assertTrue( childEles.size() == 9 );
		
		for(Element ele : childEles) {
			switch (ele.getAttribute(XmlUtils.Sname)) {
				case "overlappedPairs": assertTrue( ele.getTextContent().equals(counts[0] + "") ); break;
				case  "tlenUnder1500Pairs" : assertTrue( ele.getTextContent().equals(counts[1] + "") ); break;
				case  "tlenOver10000Pairs" : assertTrue( ele.getTextContent().equals(counts[2] + "") ); break;
				case  "tlenBetween1500And10000Pairs" : assertTrue( ele.getTextContent().equals(counts[3] + "") ); break;
				case  "pairCountUnderTlen5000" : assertTrue( ele.getTextContent().equals(counts[4] + "") ); break;
				case  "firstOfPairs" : assertTrue( ele.getTextContent().equals(counts[5] + "") ); break;
				case  "secondOfPairs" : assertTrue( ele.getTextContent().equals(counts[6] + "") ); break;
				case  "mateUnmappedPair" : assertTrue( ele.getTextContent().equals(counts[7] + "") ); break;
				case  "mateDifferentReferencePair" : assertTrue( ele.getTextContent().equals(counts[8] + "") ); break;
				default: assertTrue(false); //not allowed
			}			
		}	
	}
	
	public static Element createPairRoot(File input) throws Exception {
		createPairInputFile(input);
		BamSummarizer2 bs = new BamSummarizer2();
		BamSummaryReport2 sr = (BamSummaryReport2) bs.summarize(input.getAbsolutePath()); 
		Element root = XmlElementUtils.createRootElement("root",null);
		sr.toXml(root);		
		
		return root; 
	}
	

	private static void createPairInputFile(File input) throws IOException{
		
		List<String> data = new ArrayList<>();
		// first read of proper mapped pair; proper pair (tlen > 0 will be counted), f5f3, tlen(2025>1500)
		// f5f3: 10075<--(F3:firstOfPair)    2015<--(F5:secondOfPair)
		data.add("243_146_a	115	chr1	10075	6	3H37M	=	12100	2025	ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
				
		// proper pair (second in pair tlen > 10,000  but not count to totalPairs) 
		//f3f5: 10075<--(F5:secongOfPair)    21100<--(F3:firstOfPair) 
		data.add("243_146_b	179	chr1	10075	6	3H37M	=	21100	11025	ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
		
		// canonical (second in pair tlen = 13  outward ) 
		data.add("243_146_c	163	chr1	10075	6	3H37M	=	10050	13	ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");

		// canonical  outward pair (second in pair tlen = -13 discard from pair ) 
		data.add("243_146_d	163	chr1	10075	6	5H37M	=	10000	-36	ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");		
						
		//non-canonical pair (Not counted as pair as it is second in pair ) noRG and mate unmapped.
		data.add("NS500239:a	25	chr1	7480169	0	75M	*	0	0	AATGAATAGAAGGGTCCAGATCCAGTTCTAATTTGGGGTAGGGACTCAGTTTGTGTTTTTTCACGAGATGAAGAT	" + 
		"EEEA<EEEEEE<<EE/AEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEAEEEEEEEEAEEEEEEEAAAAA	NH:i:14	HI:i:11	AS:i:73	NM:i:0	MD:Z:75	");

		//non-canonical pair noRG and mate unmapped. first in pair 
		data.add("NS500239:b	89	chr1	7480169	0	75M	*	0	0	AATGAATAGAAGGGTCCAGATCCAGTTCTAATTTGGGGTAGGGACTCAGTTTGTGTTTTTTCACGAGATGAAGAT	" + 
		"EEEA<EEEEEE<<EE/AEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEAEEEEEEEEAEEEEEEEAAAAA	NH:i:14	HI:i:11	AS:i:73	NM:i:0	MD:Z:75	");
		
		//non-canonical pair noRG and mate different ref
		data.add("NS500239:c	83	chr1	7480169	0	75M	chr2	10	0	AATGAATAGAAGGGTCCAGATCCAGTTCTAATTTGGGGTAGGGACTCAGTTTGTGTTTTTTCACGAGATGAAGAT	" + 
				"EEEA<EEEEEE<<EE/AEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEAEEEEEEEEAEEEEEEEAAAAA	NH:i:14	HI:i:11	AS:i:73	NM:i:0	MD:Z:75	");

		//first pair noRG and fully overlap with tLen > 0 since pair with different orientation
		data.add("NS500239:d	99	chr1	7480169	0	75M	=	7480169	76	AATGAATAGAAGGGTCCAGATCCAGTTCTAATTTGGGGTAGGGACTCAGTTTGTGTTTTTTCACGAGATGAAGAT	" + 
				"EEEA<EEEEEE<<EE/AEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEAEEEEEEEEAEEEEEEEAAAAA	NH:i:14	HI:i:11	AS:i:73	NM:i:0	MD:Z:75	");

		//first pair noRG and fully overlap with tLen == 0 since both forward orientation
		//F3F5: notProperPair, firstOfPair bothforward same start
		data.add("NS500239:d	65	chr1	7480169	0	75M	=	7480169	0	AATGAATAGAAGGGTCCAGATCCAGTTCTAATTTGGGGTAGGGACTCAGTTTGTGTTTTTTCACGAGATGAAGAT	" + 
				"EEEA<EEEEEE<<EE/AEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEAEEEEEEEEAEEEEEEEAAAAA	NH:i:14	HI:i:11	AS:i:73	NM:i:0	MD:Z:75	");

				
		//noRG: pairNumber==0
		//1959N: pairNumber==1, inward overlapped pair
		//1959T: pairNumber==2, f3f5 tlen(1025 < 1500) pair;  and inward overlapped pair
		ReadGroupSummaryTest.createInputFile(input);		
		//append new pairs
		try(BufferedWriter out = new BufferedWriter(new FileWriter(input, true))){	    
			for (String line : data)  out.write(line + "\n");	               
		}		
	}	
}
