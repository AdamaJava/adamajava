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
import org.qcmg.picard.BwaPair;
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
	public void toSummaryXmlTest() throws Exception {	
		
		Element root = createPairRoot(input);		
		List<Element> pairEles =  XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.SEQUENCE_METRICS)	
				.stream().filter(e -> e.getAttribute(XmlUtils.NAME).equals( "properPairs" )).collect(Collectors.toList());	
		
		//only one inward pair but overlapped
		Element ele = pairEles.stream().filter(e -> ( (Element) e.getParentNode()).getAttribute(XmlUtils.NAME).equals("1959N")).findFirst().get(); 		
		checkVariableGroup( ele, "Inward", new int[] {1,0,0,0,1,1,1, 0, 0,175} );

		//five pairs
		ele = pairEles.stream().filter(e -> ( (Element) e.getParentNode()).getAttribute(XmlUtils.NAME).equals("1959T")).findFirst().get();	
		checkVariableGroup( ele, "F5F3", new int[] {0,0,0,1,1,1,0, 0, 0,2025} ); //tlen=11205, 2015
		checkVariableGroup( ele, "F3F5", new int[] {0,1,1,0,1,1,1,0, 0,93} ); //paircounts only for number of firstOfpair
		checkVariableGroup( ele, "Outward", new int[] {2,0,0,0,2,1,3,0,0,13} );
				
		ele = pairEles.stream().filter(e -> ( (Element) e.getParentNode()).getAttribute(XmlUtils.NAME).equals(XmlUtils.UNKNOWN_READGROUP)).findFirst().get();	
		checkVariableGroup( ele, "Inward", new int[] {1,0,0,0,1,1,0,0,0,76 } );		
			
		//notProperPair
		pairEles =  XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.SEQUENCE_METRICS)	
				.stream().filter(e  -> e .getAttribute(XmlUtils.NAME).equals( "notProperPairs" )).collect(Collectors.toList());
		ele = pairEles.stream().filter(e -> ( (Element) e.getParentNode()).getAttribute(XmlUtils.NAME).equals(XmlUtils.UNKNOWN_READGROUP)).findFirst().get();	
		checkVariableGroup( ele, "F3F5", new int[] {1,0,0,0,1,1,0,0,0, 0} ); //notProperPair
		
		
	}
	
	private void checkVariableGroup(Element root, String name, int[] counts ) {
		
		Element variableEle = XmlElementUtils.getChildElementByTagName(root, XmlUtils.VARIABLE_GROUP).stream()
			.filter(e -> e.getAttribute(XmlUtils.NAME).equals(name) ).findFirst().get();
		
		List<Element> childEles = XmlElementUtils.getChildElementByTagName(variableEle, XmlUtils.VALUE);
		assertTrue( childEles.size() == 9 );
		
		for(Element ele : childEles) {
			switch (ele.getAttribute(XmlUtils.NAME)) {
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
	
	@Test
	public void zeroMinusTlen() {
		PairSummary pairS = new PairSummary(BwaPair.Pair.Others, false );
		SAMRecord recorda = new SAMRecord(null);	
		recorda.setInferredInsertSize(-1000 );
		assertTrue(BwaPair.getOverlapBase(recorda) == 0); 
		recorda.setInferredInsertSize(-1 );
		assertTrue(BwaPair.getOverlapBase(recorda) == 0); 
		try {
			pairS.parse(recorda);
			fail("expect to throw exception");
		}catch(Exception e) {}
		
		
		//set tLen == 0, any second of pair
		recorda.setInferredInsertSize( 0 );
		for(int flag : new int[] {129,131,147, 179}) {
			recorda.setFlags(flag);
			assertTrue(BwaPair.getOverlapBase(recorda) == 0); 
			pairS.parse(recorda);
		}
		
		//ST-E00180:52:H5LNMCCXX:1:1116:24274:5247 113 chr1 27977105 60 5S145M = 27977205 0
		//<-----------| F3F5 reverse firstOfPaie
		//    <-------|
		recorda.setFlags(113);
		recorda.setCigarString("5S145M");
		recorda.setAlignmentStart(27977105);
		recorda.setMateAlignmentStart(27977205);
		assertTrue(BwaPair.getOverlapBase(recorda) == 27977250 - 27977205); 
		pairS.parse(recorda);
		
		//ST-E00180:52:H5LNMCCXX:1:2103:1720:24691 113 chr1 27775356 39 117S33M = 27775354 0
		//    <-------|  F5F3 reverse firstOfPaie
		//<-----------|
		recorda.setCigarString( "117S33M" );
		recorda.setAlignmentStart( 27775356 );
		recorda.setMateAlignmentStart( 27775354 );
		assertTrue(BwaPair.getOverlapBase(recorda) == 27775389 - 27775356); 
		pairS.parse(recorda);
		
		//ST-E00180:52:H5LNMCCXX:1:2101:31703:21983 65 chr1 34492938 60 95M55S = 34492938 0   
		//F5F3 forward firstOfPaie
		//s1 |--------------> e1			s1 |---------> e1
		//s2 |------------> e2		or 		s2 |------------> e2
		recorda.setFlags(65);
		recorda.setCigarString("95M55S");
		recorda.setAlignmentStart(34492938);
		recorda.setMateAlignmentStart(34492938);
		assertTrue(BwaPair.getOverlapBase(recorda) == 95); 
		pairS.parse(recorda);
	
		
		//ST-E00180:52:H5LNMCCXX:1:1205:9993:36522 97 chr1 121484553 43 149M = 121484404 0   
		//outward firstOfPair   <---mate----|----read---->
		recorda.setFlags(97);
		recorda.setCigarString("95M55S");		
		recorda.setAlignmentStart(121484553);
		recorda.setMateAlignmentStart(121484404);		
		assertTrue(BwaPair.getOverlapBase(recorda) == 1); 
		pairS.parse(recorda);
		
		assertTrue( pairS.getFirstOfPairCounts() == 4 );
		assertTrue( pairS.near.get() == 0 ); //only one without overlap		
		assertTrue( pairS.tLenOverall.get(0) == 4 ); 
		
		assertTrue( pairS.getoverlapCounts().get(0) == 0 );
		assertTrue( pairS.getoverlapCounts().get(33) == 1 );
		assertTrue( pairS.getoverlapCounts().get(45) == 1 );
		assertTrue( pairS.getoverlapCounts().get(95) == 1 );
		
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
