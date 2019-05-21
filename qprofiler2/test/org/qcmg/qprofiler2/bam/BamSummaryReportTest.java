package org.qcmg.qprofiler2.bam;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import htsjdk.samtools.SAMUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.XmlElementUtils;
import org.qcmg.qprofiler2.bam.BamSummaryReport2;
import org.qcmg.qprofiler2.summarise.CycleSummaryTest;
import org.qcmg.qprofiler2.summarise.PairSummary;
import org.qcmg.qprofiler2.summarise.PairSummaryTest;
import org.qcmg.qprofiler2.summarise.PositionSummary;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;


public class BamSummaryReportTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	public File input;

	@Before
	public void setup() throws IOException {
		input = testFolder.newFile("testInputFile.sam");
	}	
			
	@Test
	public void testParseRNameAndPos() throws Exception {
		BamSummaryReport2 bsr = new BamSummaryReport2( -1, false);
		final String rg = "rg1";
		bsr.setReadGroups(Arrays.asList(rg) );
		
		String rName = "test";
		int position = 999;		
		bsr.parseRNameAndPos( rName, position,rg );
		PositionSummary returnedSummary = bsr.getRNamePosition().get(rName);
		assertEquals( position, returnedSummary.getMax() );
		assertEquals( position, returnedSummary.getMin() );
		assertEquals( 1, returnedSummary.getCoverageByRg(rg).get(0).get() );
		
		// and again - min and max should stay the same, count should increase
		bsr.parseRNameAndPos(rName, position,rg );
		returnedSummary = bsr.getRNamePosition().get(rName);
		assertEquals(position, returnedSummary.getMax());
		assertEquals(position, returnedSummary.getMin());
		assertEquals( 2, returnedSummary.getCoverageByRg(rg).get(0).get() );

		// add another position to this rName
		position = 1000000;
		bsr.parseRNameAndPos(rName, position,rg );
		returnedSummary = bsr.getRNamePosition().get(rName);
		assertEquals(position, returnedSummary.getMax());
		assertEquals(999, returnedSummary.getMin());
		assertEquals(1, returnedSummary.getCoverageByRg(rg).get(1).get() );
		
		// add another position to this rName
		position = 0;
		bsr.parseRNameAndPos(rName, position,rg );
		returnedSummary = bsr.getRNamePosition().get(rName);
		assertEquals(1000000, returnedSummary.getMax());
		assertEquals(position, returnedSummary.getMin());
		assertEquals( 3, returnedSummary.getCoverageByRg(rg).get(0).get() );
		assertEquals( 1, returnedSummary.getCoverageByRg(rg).get(1).get() );
		
		// add a new rname
		rName = "new rname";
		bsr.parseRNameAndPos(rName, 0,rg );
		returnedSummary = bsr.getRNamePosition().get(rName);
		assertEquals(0, returnedSummary.getMax());
		assertEquals(0, returnedSummary.getMin());
		assertEquals(1, returnedSummary.getCoverageByRg(rg).get(0).get() );
		assertEquals(1, returnedSummary.getCoverageByRg(rg).size() );
				
	}
			
	@Test
	public void testCompareWithSAMUtils() {
		String inputString = "!''*((((***+))%%%++)(%%%%).1***-+*''))**55CCF>>>>>>CCCCCCC65";
		String expectedOutputString = "BHHKIIIIKKKLJJFFFLLJIFFFFJORKKKNLKHHJJKKVVddg______dddddddWV";
		int counter = 100000;
		String outputString = null;

		for (int i = 0 ; i < counter ; i++) 
			outputString = StringUtils.addASCIIValueToChar(inputString, 33);					
		assertEquals(expectedOutputString, outputString);
				
		byte [] bytes = inputString.getBytes();
		for (int i = 0 ; i < counter ; i++) 			
			outputString = SAMUtils.phredToFastq(bytes);
		
		assertEquals(expectedOutputString, outputString);		
		for (int i = 0 ; i < counter ; i++)
			outputString = StringUtils.addASCIIValueToChar(inputString, 33);			
		
		assertEquals(expectedOutputString, outputString);
	}
	
	private void checklength(Element root, boolean isSeq,  String pairName, int[] values, int[] counts) throws Exception {
		if(counts.length != values.length)
			throw new Exception("error: values size must be same to counts size");
		
		String nodeName = (isSeq)? XmlUtils.seq  : XmlUtils.qual ;
		String sLength = isSeq? XmlUtils.seqLength : XmlUtils.qualLength;
		
		//get node <SEQ> or <QUAL>
		List<Element> lists = XmlElementUtils.getOffspringElementByTagName( root, nodeName );
		assertEquals(1, lists.size());
		
		//get node <sequenceMetrics name="seqLength"> or <sequenceMetrics name="qualLength">		
		lists = XmlElementUtils.getOffspringElementByTagName( lists.get( 0 ), XmlUtils.metricsEle ).stream()
			.filter( e -> e.getAttribute(XmlUtils.sName).equals( sLength)).collect(Collectors.toList());
		assertEquals(1, lists.size());
		
		//<variableGroup name="firstReadInPair"> or <variableGroup name="secondReadInPair">				
		lists = XmlElementUtils.getOffspringElementByTagName( lists.get( 0 ), XmlUtils.variableGroupEle ).stream()
				.filter( e -> e.getAttribute(XmlUtils.sName).equals(pairName)).collect(Collectors.toList());		
		assertEquals(1, lists.size());				
		
		Element ele = lists.get(0);
		assertEquals(values.length, ele.getChildNodes().getLength());	
			
		//eg   QprofilerXmlUtils.seqLength + "_"+  QprofilerXmlUtils.FirstOfPair;
		for(int i = 0; i < values.length; i ++) {
			String v = values[i] + "";
			String c = counts[i] + "";
			long count = XmlElementUtils.getChildElementByTagName( ele, XmlUtils.sTally ).stream()
					.filter( e -> e.getAttribute(XmlUtils.sValue).equals(v) 
							&& e.getAttribute(XmlUtils.sCount).equals(c) ).count();
			assertTrue(count == 1);
		}	 
	}
		
	private void checkTally(Element root, String groupName, String value, int count, int expectedNo ) {
		
		long findNo = XmlElementUtils.getOffspringElementByTagName( root, XmlUtils.sTally).stream()
			.filter( e -> e.getAttribute(XmlUtils.sValue).equals(value) && e.getAttribute(XmlUtils.sCount).equals(count + "") &&
					((Element) e.getParentNode()).getAttribute(XmlUtils.sName).equals(groupName)).count();

		assertEquals( expectedNo , findNo);
	}
		
	@Test
	//check read length and rname, both ignor readgroup
	public void checkLength_rname_mapq() throws Exception{ 
		CycleSummaryTest.createInputFile(input);
		Element root = XmlElementUtils.createRootElement("root",null);
		BamSummarizer2 bs = new BamSummarizer2();
		BamSummaryReport2 sr = (BamSummaryReport2) bs.summarize( input.getAbsolutePath() ); 
		sr.toXml(root);			
		
		//length
		checklength( root, true,  XmlUtils.firstOfPair, new int[] {141,151}, new int[] { 1,1 });
		checklength( root, true,  XmlUtils.secondOfPair, new int[] {151}, new int[] { 1});
		checklength( root, false, XmlUtils.firstOfPair, new int[] {143,151}, new int[] { 1,1 });
		checklength( root, false, XmlUtils.secondOfPair, new int[] {151}, new int[] { 1});		
		
		//rNAME
		Element node = XmlElementUtils.getOffspringElementByTagName( root, XmlUtils.rname ).get( 0 );	
		checkTally(node, XmlUtils.rname, "chr1", 2, 1 );
		//the second of pair on chr11 is duplicated, so here only the first of pair are counted
		checkTally(node, XmlUtils.rname, "chr11", 1, 1 );
			
		//mapq is for all counted reads disregard duplicate ect, unmapped reads mapq=0
		//Zero mapping quality indicates that the read maps to multiple locations or differet ref
		node = XmlElementUtils.getOffspringElementByTagName( root, XmlUtils.mapq ).get( 0 );	
		checkTally(node,  XmlUtils.firstOfPair, "0", 1, 1 );
		checkTally(node,  XmlUtils.firstOfPair, "25", 1, 1 );
		checkTally(node,  XmlUtils.secondOfPair, "0", 1, 1 );	
		checkTally(node,  XmlUtils.secondOfPair, "6", 1, 1 );	
				
		
	}
		
	
	/** the algorithm is inside ReadGroupSummary::parseRecord::parsePairing
	 * only middleTlenValue > record tLen > 0 exclude discard reads, duplicate, unmapped
	 * @throws Exception
	 */
	@Test
	public void checkTlen() throws Exception {
		Element root = PairSummaryTest.createPairRoot( input);		
		 

		final Element tlenE = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.tlen  ).get(0);
		final List<Element> rgsE =  XmlElementUtils.getOffspringElementByTagName( tlenE, "readGroup" );
		
		//five pairs in 1959T, we only record 13, 26, 2015		
		List<Element> tallyE = getTallys( rgsE, "1959T", "tLenInProperPair" , 4); 
		chekTlen(  tallyE, PairSummary.Pair.F3F5, 1, 93 );
		chekTlen(  tallyE, PairSummary.Pair.F5F3, 1, 2025 );
		chekTlen(  tallyE, PairSummary.Pair.Outward, 1, 13 );
		chekTlen(  tallyE, PairSummary.Pair.Outward, 1, 26 );
				
		//only record popular TLEN that is tLen < middleTlenValue) isize.increment(tLen);	
		//1959N only one overlap pair tlen is 175
		tallyE = getTallys( rgsE, "1959N", "tLenInProperPair" , 1); 
		chekTlen(  tallyE, PairSummary.Pair.Inward, 1, 175 );		
		tallyE = getTallys( rgsE, "1959N", "overlapBaseInProperPair" , 1); 
		chekTlen(  tallyE, PairSummary.Pair.Inward, 1, 62 );
				
		tallyE = getTallys( rgsE, XmlUtils.UNKNOWN_READGROUP, "tLenInProperPair" , 1); 
		chekTlen(  tallyE, PairSummary.Pair.Inward, 1, 76 );		
		tallyE = getTallys( rgsE, XmlUtils.UNKNOWN_READGROUP, "overlapBaseInProperPair" , 1); 
		chekTlen(  tallyE, PairSummary.Pair.Inward, 1, 75 );	
		tallyE = getTallys( rgsE, XmlUtils.UNKNOWN_READGROUP, "tLenInNotProperPair" , 1); 
		chekTlen(  tallyE, PairSummary.Pair.F3F5, 1, 0 );
	}
	private void chekTlen(List<Element> tallyE, PairSummary.Pair p, int count, int value ) {
		 long no = tallyE.stream().filter( e ->   
			((Element) e.getParentNode()).getAttribute(XmlUtils.sName).equals(p.name()) &&
			 e.getAttribute(XmlUtils.sValue).equals( value+"" ) && e.getAttribute(XmlUtils.sCount).equals( count+"" )		 
		 ).count();	
		
		 assertTrue(no == 1);		
	}
	
	private List<Element> getTallys(List<Element> rgsE,String rgName, String metricName,  int size){		
		Element ele = rgsE.stream().filter( e -> e.getAttribute(XmlUtils.sName).equals( rgName )  ).findFirst().get();
		ele = XmlElementUtils.getChildElementByTagName(ele, XmlUtils.metricsEle).stream().filter(e -> e.getAttribute(XmlUtils.sName).equals( metricName )  ).findFirst().get();
		List<Element> eles = XmlElementUtils.getOffspringElementByTagName(ele, XmlUtils.sTally);
		assertEquals(size, eles.size());	
	 
		return eles;
	}
		
}

//Element ele = pairEles.stream().filter(e -> ( (Element) e.getParentNode()).getAttribute(XmlUtils.Sname).equals("1959N")).findFirst().get(); 		
//chekTlen(ele,  175, 175, 175, 0, 1, 1  );		
//ele = pairEles.stream().filter(e -> ( (Element) e.getParentNode()).getAttribute(XmlUtils.Sname).equals("1959T")).findFirst().get(); 
//chekTlen(ele,  11025, 522, 13, 867, 4, 5 );
//
//ele = pairEles.stream().filter(e -> ( (Element) e.getParentNode()).getAttribute(XmlUtils.Sname).equals(QprofilerXmlUtils.UNKNOWN_READGROUP)).findFirst().get(); 		
//chekTlen(ele,    76, 76, 76  ,  0,  1,  2 );
//
////check after bamMetric
//ele = pairEles.stream().filter(e -> ( (Element) e.getParentNode()).getAttribute(XmlUtils.Sname).equals("")).findFirst().get(); 	
//chekTlen(ele,  11025 ,  390, 13 , 733 , 6 , 8  );		
//
//new File(input).delete();


//Element ele2 = QprofilerXmlElementUtils.getOffspringElementByTagName(ele1, XmlUtils.variableGroupEle).stream()
//		.filter( e -> e.getAttribute(XmlUtils.Sname).equals( "tLen" )  ).findFirst().get();
//List<Element> eles1 = checkOffSpring( ele2, XmlUtils.Stally, 4);
//assertEquals(1, eles1.stream().filter( e -> e.getAttribute(XmlUtils.Svalue).equals( "13" ) && e.getAttribute(XmlUtils.Scount).equals( "1" )  ).count());
//assertEquals(1, eles1.stream().filter( e -> e.getAttribute(XmlUtils.Svalue).equals( "25" )  ).count());
//assertEquals(1, eles1.stream().filter( e -> e.getAttribute(XmlUtils.Svalue).equals( "26" )  ).count());
//assertEquals(1, eles1.stream().filter( e -> e.getAttribute(XmlUtils.Svalue).equals( "2025" )  ).count());
////tLenByBin
//eles1 = checkOffSpring( ele1, XmlUtils.Sbin, 3);
//assertEquals(1, eles1.stream().filter( e -> e.getAttribute(XmlUtils.Sstart).equals( "1" ) && e.getAttribute(XmlUtils.Scount).equals( "3" )  ).count());
//assertEquals(1, eles1.stream().filter( e -> e.getAttribute(XmlUtils.Send).equals( "10100" )  ).count());
//
//
////empty for unkown_readgroup_id
//ele1 = rgsE.stream().filter( e -> e.getAttribute(XmlUtils.Sname).equals( QprofilerXmlUtils.UNKNOWN_READGROUP )  ).findFirst().get();
//checkOffSpring( ele1, XmlUtils.variableGroupEle, 3);
//
//// only one pair inside 1959N
//ele1 = rgsE.stream().filter( e -> e.getAttribute(XmlUtils.Sname).equals( "1959N" )  ).findFirst().get();
//ele2 = QprofilerXmlElementUtils.getOffspringElementByTagName(ele1, XmlUtils.variableGroupEle).stream()
//		.filter( e -> e.getAttribute(XmlUtils.Sname).equals( "tLen" )  ).findFirst().get();
//eles1 = checkOffSpring( ele2, XmlUtils.Stally, 1);
//assertEquals(1, eles1.stream().filter( e -> e.getAttribute(XmlUtils.Svalue).equals( "175" )  ).count());
//eles1 = checkOffSpring( ele1, XmlUtils.Sbin, 1);
//assertEquals(1, eles1.stream().filter( e -> e.getAttribute(XmlUtils.Sstart).equals( "101" )  && e.getAttribute(XmlUtils.Scount).equals( "1" )  ).count());
