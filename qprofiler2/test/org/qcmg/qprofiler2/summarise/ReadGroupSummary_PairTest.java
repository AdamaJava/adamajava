package org.qcmg.qprofiler2.summarise;

import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Element;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.qprofiler2.bam.BamSummarizer2;
import org.qcmg.qprofiler2.bam.BamSummaryReport2;
import org.qcmg.qprofiler2.summarise.ReadGroupSummary;
import org.qcmg.qprofiler2.util.XmlUtils;
import htsjdk.samtools.SAMRecord;

public class ReadGroupSummary_PairTest {
	private static final String INPUT_FILE = ReadGroupSummary_ReadTest.INPUT_FILE;
	private Element root ; 
	
	@Before
	public void setUp() throws Exception{ 
		createPairInputFile(INPUT_FILE);
		BamSummarizer2 bs = new BamSummarizer2();
		BamSummaryReport2 sr = (BamSummaryReport2) bs.summarize(INPUT_FILE); 
		
		//overall readgroup should manually  setMaxBases(long);
		this.root = QprofilerXmlUtils.createRootElement("root",null);
		sr.toXml(root);	

	}
	
	@After
	public void tearDown() { 
		new File(INPUT_FILE).delete();
	}	
	
	@Test
	public void iSizeTest() throws Exception{
		
		//debug
		QprofilerXmlUtils.asXmlText(root, "/Users/christix/Documents/Eclipse/data/qprofiler/bam/root.xml");
		
		List<Element> pairEles =  QprofilerXmlUtils.getOffspringElementByTagName(root, XmlUtils.metricsEle)		
				.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals( "pairs" )).collect(Collectors.toList());				
				
		//only record popular TLEN that is tLen < middleTlenValue) isize.increment(tLen);	
		//1959N only one overlap pair tlen is 175
		Element ele = pairEles.stream().filter(e -> ( (Element) e.getParentNode()).getAttribute(XmlUtils.Sname).equals("1959N")).findFirst().get(); 		
		chekTlen(ele,  175, 175, 175, 0, 1, 1  );
		
		ele = pairEles.stream().filter(e -> ( (Element) e.getParentNode()).getAttribute(XmlUtils.Sname).equals("1959T")).findFirst().get(); 
		chekTlen(ele,  11025, 522, 13, 867, 4, 5 );
		
		ele = pairEles.stream().filter(e -> ( (Element) e.getParentNode()).getAttribute(XmlUtils.Sname).equals(QprofilerXmlUtils.UNKNOWN_READGROUP)).findFirst().get(); 		
		chekTlen(ele,    76, 76, 76  ,  0,  1,  2 );

		//check after bamMetric
		ele = pairEles.stream().filter(e -> ( (Element) e.getParentNode()).getAttribute(XmlUtils.Sname).equals("")).findFirst().get(); 	
		chekTlen(ele,  11025 ,  390, 13 , 733 , 6 , 8  );
	}
	
	private void chekTlen(Element node, int maxtl, int meantl, int modetl, int std, int pcount5000, int pcount ) {
		Element groupE = QprofilerXmlUtils.getChildElementByTagName(node, XmlUtils.variableGroupEle).stream()
				.filter(e -> e.getAttribute(XmlUtils.Sname).equals("overall") ).findFirst().get();		
		List<Element> childEles = QprofilerXmlUtils.getChildElementByTagName(groupE, XmlUtils.Svalue);
		assertTrue(childEles.size() == 6);
				
		assertTrue( ReadGroupSummary_ReadTest.checkChildValue(groupE, "pairCount", String.valueOf(pcount )));
		assertTrue( ReadGroupSummary_ReadTest.checkChildValue(groupE, "pairCountUnderTlen5000", String.valueOf( pcount5000)));
		assertTrue( ReadGroupSummary_ReadTest.checkChildValue(groupE, "maxTlen", String.valueOf(maxtl )));
		assertTrue( ReadGroupSummary_ReadTest.checkChildValue(groupE, "meanUnderTlen5000", String.valueOf(meantl )));
		assertTrue( ReadGroupSummary_ReadTest.checkChildValue(groupE, "modeUnderTlen5000", String.valueOf(modetl )));	
		assertTrue( ReadGroupSummary_ReadTest.checkChildValue(groupE, "stdDevUnderTlen5000", String.valueOf(std)));		
			
	}
		
	private void checkPairsValue(Element pairEle, int count, int mate1, int mate2) {	
				
		assertTrue( pairEle.getAttribute( XmlUtils.Scount ).equals(count+"") );
				
		Element groupE = QprofilerXmlUtils.getChildElementByTagName(pairEle, XmlUtils.variableGroupEle).stream()
				.filter(e -> e.getAttribute(XmlUtils.Sname).equals( "unPaired") ).findFirst().get();	
		List<Element> childEles = QprofilerXmlUtils.getChildElementByTagName(groupE, XmlUtils.Svalue);		
		assertTrue(childEles.size() == 2);
		
		for(Element ele : childEles)
			if(ele.getAttribute(XmlUtils.Sname).equals("mateUnmappedPair"))
				assertTrue(ele.getTextContent().equals( mate1 + "") );
			else {
				assertTrue( ele.getAttribute(XmlUtils.Sname).equals("mateDifferentReferencePair") );
				assertTrue( ele.getTextContent().equals( mate2 + "") );
			}	
	}
	
	private void checkVariableGroup(Element root, String name, int[] counts ) {
		
		Element variableEle = QprofilerXmlUtils.getChildElementByTagName(root, XmlUtils.variableGroupEle).stream()
			.filter(e -> e.getAttribute(XmlUtils.Sname).equals(name) ).findFirst().get();
		
		List<Element> childEles = QprofilerXmlUtils.getChildElementByTagName(variableEle, XmlUtils.Svalue);
		assertTrue( childEles.size() == 5 );
		
		for(Element ele : childEles) {
			switch (ele.getAttribute(XmlUtils.Sname)) {
				case "overlappedPairs": assertTrue( ele.getTextContent().equals(counts[0] + "") ); break;
				case  "tlenUnder1500Pairs" : assertTrue( ele.getTextContent().equals(counts[1] + "") ); break;
				case  "tlenOver10000Pairs" : assertTrue( ele.getTextContent().equals(counts[2] + "") ); break;
				case  "tlenBetween1500And10000Pairs" : assertTrue( ele.getTextContent().equals(counts[3] + "") ); break;
				case  "pairCount" : assertTrue( ele.getTextContent().equals(counts[4] + "") ); break;
		//		case  "tlenis0Pairs" : assertTrue( ele.getTextContent().equals(counts[5] + "") ); break;
				default: assertTrue(false); //not allowed
			}
			
		}
	}
		

	
	@Test
	public void PairsByRGTest() throws Exception {	
		//debug
		QprofilerXmlUtils.asXmlText(root, "/Users/christix/Documents/Eclipse/data/qprofiler/bam/root.xml");
			
		List<Element> pairEles =  QprofilerXmlUtils.getOffspringElementByTagName(root, XmlUtils.metricsEle)		
				.stream().filter(ele -> ele.getAttribute(XmlUtils.Sname).equals( "pairs" )).collect(Collectors.toList());	
	 		
		//only one inward pair but overlapped
		Element ele = pairEles.stream().filter(e -> ( (Element) e.getParentNode()).getAttribute(XmlUtils.Sname).equals("1959N")).findFirst().get(); 		
		checkPairsValue( ele, 1, 0, 0);
		checkVariableGroup( ele, "F5F3", new int[] {0,0,0,0,0,0} );
		checkVariableGroup( ele, "F3F5", new int[] {0,0,0,0,0,0} );
		checkVariableGroup( ele, "Outward", new int[] {0,0,0,0,0,0} );
		checkVariableGroup( ele, "Inward", new int[] {1,0,0,0,1,0} );

		//five pairs
		ele = pairEles.stream().filter(e -> ( (Element) e.getParentNode()).getAttribute(XmlUtils.Sname).equals("1959T")).findFirst().get();
		checkPairsValue( ele, 5, 0, 0);		
		checkVariableGroup( ele, "F5F3", new int[] {0,0,0,1,1,0} ); //tlen=11205, 2015
		checkVariableGroup( ele, "F3F5", new int[] {1,0,1,0,2,0} );
		checkVariableGroup( ele, "Outward", new int[] {2,0,0,0,2,0} );
		checkVariableGroup( ele, "Inward", new int[] {0,0,0,0,0,0} );	
		
		ele = pairEles.stream().filter(e -> ( (Element) e.getParentNode()).getAttribute(XmlUtils.Sname).equals(QprofilerXmlUtils.UNKNOWN_READGROUP)).findFirst().get();
		checkPairsValue( ele, 4, 1, 1);		
		checkVariableGroup( ele, "F5F3", new int[] {0,0,0,0,0,0} ); //tlen=11205, 2015		
		//|--->
		//|---->
		checkVariableGroup( ele, "F3F5", new int[] {1,0,0,0,1,0} ); //???at 
		checkVariableGroup( ele, "Outward", new int[] {0,0,0,0,0,0} );
		checkVariableGroup( ele, "Inward", new int[] {1,0,0,0,1,0} );			
				
		//overall
		ele = pairEles.stream().filter(e -> ( (Element) e.getParentNode()).getAttribute(XmlUtils.Sname).equals("")).findFirst().get();		
		checkPairsValue(ele, 10, 1, 1);		
		checkVariableGroup(ele, "F5F3", new int[] {0,0,0,1,1,0} ); //tlen=11205, 2015
		checkVariableGroup(ele, "F3F5", new int[] {2,0,1,0,3,0}  );
		checkVariableGroup(ele, "Outward", new int[] {2,0,0,0,2,0} );
		checkVariableGroup(ele, "Inward", new int[] {2,0,0,0,2,0} );	
	}	
	
	public static void createPairInputFile(String fname) throws IOException{
		
		List<String> data = new ArrayList<>();
		// first read of proper mapped pair; non-canonical pair (tlen > 0 will be counted), f5f3, tlen(2025>1500)
		// f5f3: 10075<--(F3:firstOfPair)    2015<--(F5:secondOfPair)
		data.add("243_146_a	115	chr1	10075	6	3H37M	=	12100	2025	ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
				
		// non-canonical pair (second in pair tlen > 10,000  f5f3 but not count to totalPairs) 
		//f3f5: 10075<--(F5:secongOfPair)    21100<--(F3:firstOfPair) 
		data.add("243_146_b	179	chr1	10075	6	3H37M	=	21100	11025	ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
		
		// canonical (second in pair tlen = 13  outward but not count to totalPairs) 
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
		data.add("NS500239:d	65	chr1	7480169	0	75M	=	7480169	0	AATGAATAGAAGGGTCCAGATCCAGTTCTAATTTGGGGTAGGGACTCAGTTTGTGTTTTTTCACGAGATGAAGAT	" + 
				"EEEA<EEEEEE<<EE/AEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEAEEEEEEEEAEEEEEEEAAAAA	NH:i:14	HI:i:11	AS:i:73	NM:i:0	MD:Z:75	");

				
		//noRG: pairNumber==0
		//1959N: pairNumber==1, inward overlapped pair
		//1959T: pairNumber==2, f3f5 tlen(1025 < 1500) pair;  and inward overlapped pair
		ReadGroupSummary_ReadTest.createInputFile();		
		//append new pairs
		try(BufferedWriter out = new BufferedWriter(new FileWriter(fname, true))){	    
			for (String line : data)  out.write(line + "\n");	               
		}		
	}
}
