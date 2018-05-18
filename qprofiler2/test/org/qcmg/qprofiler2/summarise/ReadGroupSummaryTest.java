package org.qcmg.qprofiler2.summarise;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.qprofiler2.bam.BamSummarizer2;
import org.qcmg.qprofiler2.bam.BamSummaryReport2;
import org.qcmg.qprofiler2.bam.TagSummaryReportTest;
import org.qcmg.qprofiler2.summarise.ReadGroupSummary;
import org.qcmg.qprofiler2.util.SummaryReportUtils;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import javafx.util.Pair;
import junit.framework.Assert;

public class ReadGroupSummaryTest {
	private static final String INPUT_FILE = "input.sam";
	private static final String TEST_FILE = "test.xml";
	
	@Before
	public void setUp() throws Exception{ createPairInputFile(); }
	
//	@After
//	public void tearDown() { 
//		new File(INPUT_FILE).delete();
//		new File(TEST_FILE).delete();		
//	}	
	
	@Test
	public void iSizeTest() throws Exception{
		createPairInputFile();		
		Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation().createDocument(null, "qProfiler", null).getDocumentElement();
		BamSummaryReport2 sr = (BamSummaryReport2) new BamSummarizer2().summarize( INPUT_FILE ); 
		sr.toXml(root);
		
		root = QprofilerXmlUtils.getChildElement( root, ProfileType.bam +"Report", 0 );				
		//from summary node
		Element  pairHead =  QprofilerXmlUtils.getChildElement( QprofilerXmlUtils.getChildElement(root, QprofilerXmlUtils.summary, 0), QprofilerXmlUtils.readPairs,0);
		List<Element> eleList =  QprofilerXmlUtils.getChildElementByTagName(pairHead, QprofilerXmlUtils.readGroup   );	
		Assert.assertEquals( 4, eleList.size() );
		for( Element ele : eleList ){
			Element ele1 = QprofilerXmlUtils.getChildElement(ele, "tlen", 0);
			if( ele.getAttribute("id").equals("1959T") ){
				Assert.assertEquals( ele1.getAttribute( ReadGroupSummary.modal_isize ), "26" );
				Assert.assertEquals( ele1.getAttribute( "stdDev" ),"706.40" );
			}else if( ele.getAttribute("id").equals("overall") ){
				Assert.assertEquals( ele1.getAttribute( ReadGroupSummary.modal_isize ), "26" );
				Assert.assertEquals( ele1.getAttribute( "stdDev" ),"538.93" );
			} 
		}
		
		//from ISIZE node
		Element ele = QprofilerXmlUtils.getChildElement(root, "ISIZE", 0);
		ele = QprofilerXmlUtils.getChildElement(ele,  QprofilerXmlUtils.valueTally, 0);
		String[] possibles =  ele.getAttribute( QprofilerXmlUtils.possibles ).split(QprofilerXmlUtils.COMMA);		
		int order = 0;
		for (int i = 0; i < possibles.length; i++)
			if(possibles[i].equals("1959T")){ order = i; break;}
		
		eleList = QprofilerXmlUtils.getChildElementByTagName( ele, QprofilerXmlUtils.tallyItem );	
		Assert.assertEquals( 5, eleList.size() );
		for(Element ele1: eleList){
			possibles = ele1.getAttribute(QprofilerXmlUtils.counts ).split(QprofilerXmlUtils.COMMA);
			String value =  ele1.getAttribute(QprofilerXmlUtils.value );
			if(value.equals("0")) Assert.assertEquals( possibles[order],"0" );
			else if(value.equals("26")) Assert.assertEquals( possibles[order],"1" );
			else if(value.equals("175")) Assert.assertEquals( possibles[order],"0" );
			else if(value.equals("1025")) Assert.assertEquals( possibles[order],"1" );
			else Assert.assertEquals( possibles[order],"1" );
		}
		
	}
 	
	@Test
	public void PairsByRGTest() throws Exception{
		
		Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation().createDocument(null, "qProfiler", null).getDocumentElement();
		BamSummaryReport2 sr = (BamSummaryReport2) new BamSummarizer2().summarize( INPUT_FILE); 
		sr.toXml(root);
		QprofilerXmlUtils.asXmlText(root, TEST_FILE);  //debug
	
		Assert.assertEquals( sr.getRecordsParsed(), 19 );					
		Element  pairHead =  QprofilerXmlUtils.getChildElement( QprofilerXmlUtils.getChildElement( root, "SUMMARY", 0 ), "Pairs",0 );
		List<Element> nodes =  QprofilerXmlUtils.getChildElementByTagName( pairHead, ReadGroupSummary.node_readgroup );
		
		for(Element node : nodes){		 
		 	String rg = node.getAttribute("id");
		 	
		 	String pairsS = node.getAttribute("TotalPairs") ;
		 	String mateDiffRefS = node.getElementsByTagName("MateDiffRef").item(0).getAttributes().getNamedItem("pairNumber").getNodeValue();		 	
		 	String mateUnmappedS  = node.getElementsByTagName("MateUnmapped").item(0).getAttributes().getNamedItem("pairNumber").getNodeValue();
		 	
		 	String f5f3S = node.getElementsByTagName( ReadGroupSummary.node_f5f3 ).item(0).getAttributes().getNamedItem("pairNumber").getNodeValue();
		 	String f3f5S = node.getElementsByTagName( ReadGroupSummary.node_f3f5 ).item(0).getAttributes().getNamedItem("pairNumber").getNodeValue();
		 	String inwardS = node.getElementsByTagName( ReadGroupSummary.node_inward ).item(0).getAttributes().getNamedItem("pairNumber").getNodeValue();
		 	String outwardS = node.getElementsByTagName( ReadGroupSummary.node_outward ).item(0).getAttributes().getNamedItem("pairNumber").getNodeValue();
		 	
		 	//isize will be check at other place
		 	Element isizeElement = (Element) node.getElementsByTagName("ISIZE").item(0);
		 	node.removeChild(isizeElement) ;
		 	
		 	int pairs = Integer.parseInt( pairsS );
		 	int mateDiffRef = Integer.parseInt( mateDiffRefS );
		 	int mateUnmapped = Integer.parseInt( mateUnmappedS );
		 	
		 	int f5f3 = Integer.parseInt( f5f3S );
		 	int f3f5 = Integer.parseInt( f3f5S );
		 	int inward = Integer.parseInt( inwardS );
		 	int outward = Integer.parseInt( outwardS );
		 	
			 switch ( rg ) {
			 	case "1959N" :
			 		Assert.assertTrue( pairs  == 1 );	
			 		Assert.assertTrue( inward == 1 );
			 		inward = Integer.parseInt( node.getElementsByTagName(ReadGroupSummary.node_inward ).item(0).getAttributes().getNamedItem( "overlappedPairs" ).getNodeValue());
					Assert.assertTrue( inward == 1 );
					Assert.assertTrue(pairs == ( mateDiffRef + mateUnmapped + f5f3 + f3f5 + inward + outward ) );	
					
					node.getElementsByTagName(ReadGroupSummary.node_inward ).item(0).getAttributes().removeNamedItem("overlappedPairs");
					node.getElementsByTagName(ReadGroupSummary.node_inward ).item(0).getAttributes().removeNamedItem("pairNumber");
					NodeList  nList =   node.getElementsByTagName("*");
					for(int ii = 0; ii < nList.getLength(); ii++ ){	
						if(nList.item(ii).getNodeName().contains("flag_p")) continue; //not interested in flag_p
						NamedNodeMap  map = nList.item(ii).getAttributes();
						for(int iii = 0; iii < map.getLength(); iii ++) 
							Assert.assertTrue( map.item(iii).getNodeValue().equals("0") );							 
					}					
			 		break;
			 	case "1959T" :		 	
			 		Assert.assertTrue( pairs  == 3 );
			 		Assert.assertTrue( f5f3 == 1 ); //one of them are second of pair, so not belong to pairs
			 		Assert.assertTrue( outward == 2 );	
			 		Assert.assertTrue( f3f5 == 2 );
			 		
			 		f5f3  = Integer.parseInt( node.getElementsByTagName(ReadGroupSummary.node_f5f3 ).item(0).getAttributes().getNamedItem( "TlenOver1500" ).getNodeValue() );
			 		Assert.assertTrue( f5f3 == 1 );			 					 		 		
			 		outward  = Integer.parseInt( node.getElementsByTagName(ReadGroupSummary.node_outward ).item(0).getAttributes().getNamedItem( "overlappedPairs" ).getNodeValue());
			 		Assert.assertTrue( outward  == 2 );			 		
			 		f3f5  = Integer.parseInt( node.getElementsByTagName(ReadGroupSummary.node_f3f5).item(0).getAttributes().getNamedItem( "TlenLess1500" ).getNodeValue());
			 		Assert.assertTrue( f3f5 == 1 );
			 		f3f5  = Integer.parseInt( node.getElementsByTagName(ReadGroupSummary.node_f3f5).item(0).getAttributes().getNamedItem( "TlenOver10000" ).getNodeValue());
			 		Assert.assertTrue( f3f5 == 1 );
			 		node.getElementsByTagName(ReadGroupSummary.node_outward).item(0).getAttributes().removeNamedItem("pairNumber");
			 		node.getElementsByTagName(ReadGroupSummary.node_outward).item(0).getAttributes().removeNamedItem("overlappedPairs");			 		
			 		node.getElementsByTagName(ReadGroupSummary.node_f3f5).item(0).getAttributes().removeNamedItem("pairNumber");
			 		node.getElementsByTagName(ReadGroupSummary.node_f3f5).item(0).getAttributes().removeNamedItem("TlenLess1500");
			 		node.getElementsByTagName(ReadGroupSummary.node_f3f5).item(0).getAttributes().removeNamedItem("TlenOver10000");			 		
			 		node.getElementsByTagName(ReadGroupSummary.node_f5f3).item(0).getAttributes().removeNamedItem("pairNumber");
			 		node.getElementsByTagName(ReadGroupSummary.node_f5f3).item(0).getAttributes().removeNamedItem("TlenOver1500");
			 		
			 		NodeList  nnList =   node.getElementsByTagName("*");
					for(int ii = 0; ii < nnList.getLength(); ii++ ){
						NamedNodeMap  map = nnList.item(ii).getAttributes();
						for(int iii = 0; iii < map.getLength(); iii ++)
							Assert.assertTrue( map.item(iii).getNodeValue().equals("0") );						
					}
			 		break;				 		
			 	case QprofilerXmlUtils.UNKNOWN_READGROUP:
			 		Assert.assertTrue( pairs  == 3 );			 		
			 		Assert.assertTrue( mateDiffRef == 1 );
			 		Assert.assertTrue( mateUnmapped == 2 );
			 		Assert.assertTrue( f3f5 == 0 );
			 		Assert.assertTrue( outward == 0 );			 					 		
			 		break;
			 	case QprofilerXmlUtils.All_READGROUP :		
			 		Assert.assertTrue( pairs  == 7 );
			 		Assert.assertTrue( f5f3 == 1 ); //one of them are second of pair, so not belong to pairs
			 		Assert.assertTrue( outward == 2 );	
			 		Assert.assertTrue( f3f5 == 2 );
			 		Assert.assertTrue( inward == 1 );
			 		Assert.assertTrue( mateDiffRef == 1 );
			 		Assert.assertTrue( mateUnmapped == 2 );			 		
			 		break;	
			 }			 
		}
		
	}	

	@Test
	public void ReadsByRGTest() throws Exception{ 
		createReadsInputFile();		
		
		Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation().createDocument(null, "qProfiler", null).getDocumentElement();
		BamSummaryReport2 sr = (BamSummaryReport2) new BamSummarizer2().summarize( INPUT_FILE ); 
		sr.toXml(root);
		Assert.assertEquals(sr.getRecordsParsed(), 12);				
		NodeList nodes = ((Element) root.getElementsByTagName( QprofilerXmlUtils.summary ).item(0)).getElementsByTagName( QprofilerXmlUtils.reads  );   
		nodes = ( (Element) nodes.item(0) ).getElementsByTagName(ReadGroupSummary.node_readgroup);
		Assert.assertNotNull(nodes);
		
		for (int i = 0 ; i < nodes.getLength() ; i++) {
			 	String rg =  ((Element) nodes.item(i)).getAttribute("id");
			 	Element node = (Element) nodes.item(i);
				 switch ( rg) {
				 	case "1959N" :
				 		checkReads( node, "2", "30.00", "13.00", "5.00", "0.00", "12.00", "50", "43", "0.00", "0.00", "0.00", "0", "0", "0");
				 		break;
				 	case "1959T" :
				 		checkReads( node, "6", "82.92", "0.00", "0.00",  "5.42", "10.83", "40", "40", "16.67", "16.67", "33.33", "1", "1", "1");
				 		break;	
				 	case QprofilerXmlUtils.UNKNOWN_READGROUP:
				 		checkReads( node, "1", "100.00", "0.00", "0.00", "0.00", "0.00", "0", "0", "0.00", "100.00", "0.00", "0", "0", "0");

				 		break;
				 	case QprofilerXmlUtils.All_READGROUP :
				 		//here the only reads from unkown group is not paired and belong to non-canonical. so the maxBase for this group is 0; total maxBase=340 not 415
				 		checkReads( node, "9", "75.85", "3.82", "1.47", "3.82", "11.18", "50", "41", "11.11", "22.22", "22.22", "1", "1", "1");
				 		break;	
				 		
				 	default:
				 		throw new Exception("unexpected read group appears on xml SUMMARY section");
				 }
			}
		}
		
	private void checkReads(Element rgNode, String total, String lost, String trimmed, String soft, String hard, String overlap, String maxLength, String aveLength,
			String unmapped, String nonCanonical, String duplicate, String failed, String supplementary, String secondary){
		
		//nonCountedReads
		NamedNodeMap attrs = rgNode.getElementsByTagName(QprofilerXmlUtils.filteredReads ).item(0).getAttributes();
		Assert.assertEquals(attrs.getNamedItem( "failedVendorQualityCount").getNodeValue(), failed);
		Assert.assertEquals(attrs.getNamedItem( "secondaryAlignmentCount").getNodeValue(), secondary);
		Assert.assertEquals(attrs.getNamedItem( "supplementaryAlignmentCount").getNodeValue(), supplementary);
						
		//overall
		attrs = rgNode.getElementsByTagName(QprofilerXmlUtils.All_READGROUP  ).item(0).getAttributes();				
		Assert.assertEquals(attrs.getNamedItem(QprofilerXmlUtils.readCount  ).getNodeValue(), total);
		Assert.assertEquals(attrs.getNamedItem( "lostBasesPercent").getNodeValue(), lost);
		Assert.assertEquals(attrs.getNamedItem("averageLength").getNodeValue(), aveLength);
		Assert.assertEquals(attrs.getNamedItem( "maxLength").getNodeValue(), maxLength);
		
		//bad reads
		Assert.assertEquals(rgNode.getElementsByTagName(QprofilerXmlUtils.duplicateReads  ).item(0).getAttributes().getNamedItem(QprofilerXmlUtils.basePercent).getNodeValue(), duplicate );
		Assert.assertEquals(rgNode.getElementsByTagName(QprofilerXmlUtils.unmappedReads ).item(0).getAttributes().getNamedItem(QprofilerXmlUtils.basePercent).getNodeValue(), unmapped );
		Assert.assertEquals(rgNode.getElementsByTagName(QprofilerXmlUtils.nonCanonicalPair  ).item(0).getAttributes().getNamedItem(QprofilerXmlUtils.basePercent).getNodeValue(), nonCanonical );
 				
		//chopped bases
		Assert.assertEquals(rgNode.getElementsByTagName(QprofilerXmlUtils.softClippedBases).item(0).getAttributes().getNamedItem(QprofilerXmlUtils.basePercent).getNodeValue(), soft );
		Assert.assertEquals(rgNode.getElementsByTagName(QprofilerXmlUtils.overlapBases).item(0).getAttributes().getNamedItem(QprofilerXmlUtils.basePercent).getNodeValue(), overlap );
		Assert.assertEquals(rgNode.getElementsByTagName(QprofilerXmlUtils.hardClippedBases).item(0).getAttributes().getNamedItem(QprofilerXmlUtils.basePercent).getNodeValue(), hard );
		Assert.assertEquals(rgNode.getElementsByTagName(QprofilerXmlUtils.trimmedBase ).item(0).getAttributes().getNamedItem(QprofilerXmlUtils.basePercent).getNodeValue(), trimmed  );
	}
	
	private static void createReadsInputFile() throws IOException{
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

		data.add("243_146_3	1121	chr1	10075	6	3H37M	=	10167	142	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");		
		
		//secondary = not primary
		data.add("243_146_4	353	chr1	10075	6	3H37M	=	10167	142	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
				
		//vendorcheck failed
		data.add("243_146_5	609	chr1	10075	6	3H37M	=	10167	142	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");		
				
		//supplementary
		data.add("243_146_6	2147	chr1	10075	6	3H37M	=	10167	142	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
		
		//hard clip both forward non-canonical pair (first in pair)
		data.add("243_146_7	67	chr1	10075	6	3H37M	=	11100	1025	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
				
		//overlap
		//forward ??? why -59, it should be reverse.mate_end -forward.read_start (second in pair)
		data.add("970_1290_1068	163	chr1	10176	3	22M50D10M8H	=	10167	26	" +
				"AACCTAACCCTAACCCTAACCCTAACCCTAAC	I&&HII%%IIII4CII=4?IIF0B((!!7F@+	RG:Z:1959T	" +
				"CS:Z:G202023020023010023010023000.2301002302002330000000	CQ:Z:@A&*?=9%;?:A-(<?8&/1@?():(9!,,;&&,'35)69&)./?11)&=");	
		
		//reverse (first in pair)
		data.add("970_1290_1068	83	chr1	10167	1	5H35M	=	10176	-26	" +
				"CCTAACNCTAACCTAACCCTAACCCTAACCCTAAC	.(01(\"!\"&####07=?$$246/##<>,($3HC3+	RG:Z:1959T	" +
				"CS:Z:T11032031032301032201032311322310320133320110020210	CQ:Z:#)+90$*(%:##').',$,4*.####$#*##&,%$+$,&&)##$#'#$$)");
		 			
		//below overlap is not 52 but 37 groupRG:Z:1959N. make sure overlap only calculate once. 
		//trimmed, deletion forward (first in pair)
		data.add("243_145_5	99	chr1	10075	6	15M50N22M	=	10100	175	" +		 
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959N	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");

		//mate reverse   soft clip ???seqbase maybe wrong (second in pair)
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
	
	private static void createPairInputFile() throws IOException{
		
		List<String> data = new ArrayList<String>();
		// first read of proper mapped pair; non-canonical pair (tlen > 0 will be counted), f5f3, tlen(2025>1500)
		data.add("243_146_a	115	chr1	10075	6	3H37M	=	12100	2025	ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
		
		// non-canonical pair (second in pair tlen > 10,000  f5f3 but not count to totalPairs) 
		data.add("243_146_b	179	chr1	10075	6	3H37M	=	21100	11025	ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
		
		// canonical (second in pair tlen = 13  outward but not count to totalPairs) 
		data.add("243_146_c	163	chr1	10075	6	3H37M	=	10050	13	ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");

		// outward pair (second in pair tlen = -13 discard from pair ) 
		data.add("243_146_d	163	chr1	10075	6	3H37M	=	10000	-36	ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
				
		//(Not counted as pair as it is second in pair ) noRG and mate unmapped.
		data.add("NS500239:a	25	chr1	7480169	0	75M	*	0	0	AATGAATAGAAGGGTCCAGATCCAGTTCTAATTTGGGGTAGGGACTCAGTTTGTGTTTTTTCACGAGATGAAGAT	" + 
		"EEEA<EEEEEE<<EE/AEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEAEEEEEEEEAEEEEEEEAAAAA	NH:i:14	HI:i:11	AS:i:73	NM:i:0	MD:Z:75	");

		//noRG and mate unmapped. first in pair 
		data.add("NS500239:b	89	chr1	7480169	0	75M	*	0	0	AATGAATAGAAGGGTCCAGATCCAGTTCTAATTTGGGGTAGGGACTCAGTTTGTGTTTTTTCACGAGATGAAGAT	" + 
		"EEEA<EEEEEE<<EE/AEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEAEEEEEEEEAEEEEEEEAAAAA	NH:i:14	HI:i:11	AS:i:73	NM:i:0	MD:Z:75	");
		
		//noRG and mate different ref
		data.add("NS500239:c	83	chr1	7480169	0	75M	chr2	10	0	AATGAATAGAAGGGTCCAGATCCAGTTCTAATTTGGGGTAGGGACTCAGTTTGTGTTTTTTCACGAGATGAAGAT	" + 
				"EEEA<EEEEEE<<EE/AEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEAEEEEEEEEAEEEEEEEAAAAA	NH:i:14	HI:i:11	AS:i:73	NM:i:0	MD:Z:75	");
		
		//noRG: pairNumber==0
		//1959N: pairNumber==1, inward overlapped pair
		//1959T: pairNumber==2, f3f5 tlen(1025 < 1500) pair;  and inward overlapped pair
		createReadsInputFile();		
		//append new pairs
		try(BufferedWriter out = new BufferedWriter(new FileWriter(INPUT_FILE, true))){	    
			for (String line : data)  out.write(line + "\n");	               
		}
		
	}
}
