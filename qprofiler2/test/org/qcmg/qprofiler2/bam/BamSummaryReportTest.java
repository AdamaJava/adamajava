package org.qcmg.qprofiler2.bam;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import htsjdk.samtools.SAMUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.qprofiler2.bam.BamSummaryReport2;
import org.qcmg.qprofiler2.summarise.CycleSummaryTest;
import org.qcmg.qprofiler2.summarise.PositionSummary;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * 
 * @author christix
 * 
 * below is the test lists:
 * bamSummary: ReadGroupSummary_PairTest::* ReadGroupSummary_ReadTest::*	
 * QNAME: FastqSummaryReportTest::qnameTest(); ReadIDSummaryTest::*
 * FLAG: FlagUtilTest::*
 * RNAME:  this.checkLengthNrname()    
 * POS: this.checkLength_rname_mapq()   PositionSummaryTest
 * MAPQ: this.checkLength_rname_mapq()
 * CIGAR: BamSummarizerTest::testSummaryReport(BamSummaryReport2 sr)
 * TLEN:
 * SEQ:  FastqSummaryReportTest::*  CycleSummaryTest:*   KmersSummaryTest::* 	this.checkLengthNrname() 
 * QUAL: FastqSummaryReportTest::*  CycleSummaryTest:*   this.checkLengthNrname() 
 * TAG:  TagSummaryReportTest::*
	
 * ??SummaryReportUtilsTest

 *
 */
public class BamSummaryReportTest {
	
	final String input = "input.sam";
	
	@After
	public void tearDown() { new File(input).delete();	}	
			
	@Test
	public void testParseRNameAndPos() throws Exception {
		BamSummaryReport2 bsr = new BamSummaryReport2( -1);
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
	
	private void checklength(Element root, boolean isSeq, String parentName, int[] values, int[] counts) throws Exception {
		if(counts.length != values.length)
			throw new Exception("error: values size must be same to counts size");
		
		String nodeName = (isSeq)? QprofilerXmlUtils.seq  : QprofilerXmlUtils.qual ;
		String name = isSeq? QprofilerXmlUtils.seqLength : QprofilerXmlUtils.qualLength;
		
		Element node = QprofilerXmlUtils.getOffspringElementByTagName( root, nodeName ).get( 0 );		
		List<Element> elements = QprofilerXmlUtils.getOffspringElementByTagName( node, XmlUtils.variableGroupEle ).stream()
			.filter( e -> e.getAttribute(XmlUtils.Sname).equals( name) &&
					 ((Element) e.getParentNode()).getAttribute(XmlUtils.Sname).equals(parentName)).collect(Collectors.toList());
		Assert.assertEquals(elements.size(), 1);
		
		Element ele = elements.get(0);
		assertEquals(values.length, ele.getChildNodes().getLength());	
			
		//eg   QprofilerXmlUtils.seqLength + "_"+  QprofilerXmlUtils.FirstOfPair;
		for(int i = 0; i < values.length; i ++) {
			String v = values[i] + "";
			String c = counts[i] + "";
			long count = QprofilerXmlUtils.getChildElementByTagName( ele, XmlUtils.Stally ).stream()
					.filter( e -> e.getAttribute(XmlUtils.Svalue).equals(v) 
							&& e.getAttribute(XmlUtils.Scount).equals(c) ).count();
			assertTrue(count == 1);
		}	 
	}
	
	
	private void checkTally(Element root, String groupName, String value, int count, int expectedNo ) {
		
		long findNo = QprofilerXmlUtils.getOffspringElementByTagName( root, XmlUtils.Stally).stream()
			.filter( e -> e.getAttribute(XmlUtils.Svalue).equals(value) && e.getAttribute(XmlUtils.Scount).equals(count + "") &&
					((Element) e.getParentNode()).getAttribute(XmlUtils.Sname).equals(groupName)).count();

		assertEquals( expectedNo , findNo);
	}
	
	
	@Test
	//check read length and rname, both ignor readgroup
	public void checkLength_rname_mapq() throws Exception{ 
		CycleSummaryTest.createInputFile(input);
		Element root = QprofilerXmlUtils.createRootElement("root",null);
		BamSummarizer2 bs = new BamSummarizer2();
		BamSummaryReport2 sr = (BamSummaryReport2) bs.summarize( input ); 
		sr.toXml(root);	
		QprofilerXmlUtils.asXmlText( root, "/Users/christix/Documents/Eclipse/data/qprofiler/unitTest.xml" );
  		
		//length
		checklength( root, true, QprofilerXmlUtils.seqLength + "_"+  QprofilerXmlUtils.FirstOfPair, new int[] {141,151}, new int[] { 1,1 });
		checklength( root, true, QprofilerXmlUtils.seqLength + "_"+  QprofilerXmlUtils.SecondOfPair, new int[] {151}, new int[] { 1});
		checklength( root, false, QprofilerXmlUtils.qualLength + "_"+  QprofilerXmlUtils.FirstOfPair, new int[] {143,151}, new int[] { 1,1 });
		checklength( root, false, QprofilerXmlUtils.qualLength + "_"+  QprofilerXmlUtils.SecondOfPair, new int[] {151}, new int[] { 1});		
		
		//rNAME
		Element node = QprofilerXmlUtils.getOffspringElementByTagName( root, QprofilerXmlUtils.rname ).get( 0 );	
		checkTally(node, QprofilerXmlUtils.rname, "chr1", 2, 1 );
		//the second of pair on chr11 is duplicated, so here only the first of pair are counted
		checkTally(node, QprofilerXmlUtils.rname, "chr11", 1, 1 );
			
		//mapq is for all counted reads disregard duplicate ect, unmapped reads mapq=0
		//Zero mapping quality indicates that the read maps to multiple locations or differet ref
		node = QprofilerXmlUtils.getOffspringElementByTagName( root, QprofilerXmlUtils.mapq ).get( 0 );	
		checkTally(node,  QprofilerXmlUtils.mapq + "_"+  QprofilerXmlUtils.FirstOfPair, "0", 1, 1 );
		checkTally(node,  QprofilerXmlUtils.mapq + "_"+  QprofilerXmlUtils.FirstOfPair, "25", 1, 1 );
		checkTally(node,  QprofilerXmlUtils.mapq + "_"+  QprofilerXmlUtils.SecondOfPair, "0", 1, 1 );	
		checkTally(node,  QprofilerXmlUtils.mapq + "_"+  QprofilerXmlUtils.SecondOfPair, "6", 1, 1 );	
				
		
	}
	
	
	

		
}


