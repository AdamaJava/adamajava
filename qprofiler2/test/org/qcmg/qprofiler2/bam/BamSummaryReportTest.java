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
import org.qcmg.picard.BwaPair.Pair;
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
		
		String nodeName = (isSeq)? XmlUtils.SEQ  : XmlUtils.QUAL ;
		String sLength = isSeq? XmlUtils.SEQ_LENGTH : XmlUtils.QUAL_LENGTH;
		
		//get node <SEQ> or <QUAL>
		List<Element> lists = XmlElementUtils.getOffspringElementByTagName( root, nodeName );
		assertEquals(1, lists.size());
		
		//get node <sequenceMetrics name="seqLength"> or <sequenceMetrics name="qualLength">		
		lists = XmlElementUtils.getOffspringElementByTagName( lists.get( 0 ), XmlUtils.METRICS_ELE ).stream()
			.filter( e -> e.getAttribute(XmlUtils.NAME).equals( sLength)).collect(Collectors.toList());
		assertEquals(1, lists.size());
		
		//<variableGroup name="firstReadInPair"> or <variableGroup name="secondReadInPair">				
		lists = XmlElementUtils.getOffspringElementByTagName( lists.get( 0 ), XmlUtils.VARIABLE_GROUP_ELE ).stream()
				.filter( e -> e.getAttribute(XmlUtils.NAME).equals(pairName)).collect(Collectors.toList());		
		assertEquals(1, lists.size());				
		
		Element ele = lists.get(0);
		assertEquals(values.length, ele.getChildNodes().getLength());	
			
		//eg   QprofilerXmlUtils.seqLength + "_"+  QprofilerXmlUtils.FirstOfPair;
		for(int i = 0; i < values.length; i ++) {
			String v = values[i] + "";
			String c = counts[i] + "";
			long count = XmlElementUtils.getChildElementByTagName( ele, XmlUtils.TALLY ).stream()
					.filter( e -> e.getAttribute(XmlUtils.VALUE).equals(v) 
							&& e.getAttribute(XmlUtils.COUNT).equals(c) ).count();
			assertTrue(count == 1);
		}	 
	}
		
	private void checkTally(Element root, String groupName, String value, int count, int expectedNo ) {
		
		long findNo = XmlElementUtils.getOffspringElementByTagName( root, XmlUtils.TALLY).stream()
			.filter( e -> e.getAttribute(XmlUtils.VALUE).equals(value) && e.getAttribute(XmlUtils.COUNT).equals(count + "") &&
					((Element) e.getParentNode()).getAttribute(XmlUtils.NAME).equals(groupName)).count();

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
		checklength( root, true,  XmlUtils.FIRST_PAIR, new int[] {141,151}, new int[] { 1,1 });
		checklength( root, true,  XmlUtils.SECOND_PAIR, new int[] {151}, new int[] { 1});
		checklength( root, false, XmlUtils.FIRST_PAIR, new int[] {143,151}, new int[] { 1,1 });
		checklength( root, false, XmlUtils.SECOND_PAIR, new int[] {151}, new int[] { 1});		
		
		//rNAME
		Element node = XmlElementUtils.getOffspringElementByTagName( root, XmlUtils.RNAME ).get( 0 );	
		checkTally(node, XmlUtils.RNAME, "chr1", 2, 1 );
		//the second of pair on chr11 is duplicated, so here only the first of pair are counted
		checkTally(node, XmlUtils.RNAME, "chr11", 1, 1 );
			
		//mapq is for all counted reads disregard duplicate ect, unmapped reads mapq=0
		//Zero mapping quality indicates that the read maps to multiple locations or differet ref
		node = XmlElementUtils.getOffspringElementByTagName( root, XmlUtils.MAPQ ).get( 0 );	
		checkTally(node,  XmlUtils.FIRST_PAIR, "0", 1, 1 );
		checkTally(node,  XmlUtils.FIRST_PAIR, "25", 1, 1 );
		checkTally(node,  XmlUtils.SECOND_PAIR, "0", 1, 1 );	
		checkTally(node,  XmlUtils.SECOND_PAIR, "6", 1, 1 );			
	}
		
	
	/** the algorithm is inside ReadGroupSummary::parseRecord::parsePairing
	 * only middleTlenValue > record tLen > 0 exclude discard reads, duplicate, unmapped
	 * @throws Exception
	 */
	@Test
	public void checkTlen() throws Exception {
		Element root = PairSummaryTest.createPairRoot( input);		
		 

		final Element tlenE = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.TLEN  ).get(0);
		final List<Element> rgsE =  XmlElementUtils.getOffspringElementByTagName( tlenE, "readGroup" );
		
		//five pairs in 1959T, we only record 13, 26, 2015		
		List<Element> tallyE = getTallys( rgsE, "1959T", "tLenInProperPair" , 4); 
		chekTlen(  tallyE, Pair.F3F5, 1, 93 );
		chekTlen(  tallyE, Pair.F5F3, 1, 2025 );
		chekTlen(  tallyE, Pair.Outward, 1, 13 );
		chekTlen(  tallyE, Pair.Outward, 1, 26 );
				
		//only record popular TLEN that is tLen < middleTlenValue) isize.increment(tLen);	
		//1959N only one overlap pair tlen is 175
		tallyE = getTallys( rgsE, "1959N", "tLenInProperPair" , 1); 
		chekTlen(  tallyE, Pair.Inward, 1, 175 );		
		tallyE = getTallys( rgsE, "1959N", "overlapBaseInProperPair" , 1); 
		chekTlen(  tallyE, Pair.Inward, 1, 62 );
				
		tallyE = getTallys( rgsE, XmlUtils.UNKNOWN_READGROUP, "tLenInProperPair" , 1); 
		chekTlen(  tallyE, Pair.Inward, 1, 76 );		
		tallyE = getTallys( rgsE, XmlUtils.UNKNOWN_READGROUP, "overlapBaseInProperPair" , 1); 
		chekTlen(  tallyE, Pair.Inward, 1, 75 );	
		tallyE = getTallys( rgsE, XmlUtils.UNKNOWN_READGROUP, "tLenInNotProperPair" , 1); 
		chekTlen(  tallyE, Pair.F3F5, 1, 0 );
	}
	private void chekTlen(List<Element> tallyE, Pair p, int count, int value ) {
		 long no = tallyE.stream().filter( e ->   
			((Element) e.getParentNode()).getAttribute(XmlUtils.NAME).equals(p.name()) &&
			 e.getAttribute(XmlUtils.VALUE).equals( value+"" ) && e.getAttribute(XmlUtils.COUNT).equals( count+"" )		 
		 ).count();	
		
		 assertTrue(no == 1);		
	}
	
	private List<Element> getTallys(List<Element> rgsE,String rgName, String metricName,  int size){		
		Element ele = rgsE.stream().filter( e -> e.getAttribute(XmlUtils.NAME).equals( rgName )  ).findFirst().get();
		ele = XmlElementUtils.getChildElementByTagName(ele, XmlUtils.METRICS_ELE).stream().filter(e -> e.getAttribute(XmlUtils.NAME).equals( metricName )  ).findFirst().get();
		List<Element> eles = XmlElementUtils.getOffspringElementByTagName(ele, XmlUtils.TALLY);
		assertEquals(size, eles.size());	
	 
		return eles;
	}
		
}
