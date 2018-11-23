package org.qcmg.qprofiler2.bam;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMUtils;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.MAPQMiniMatrix;
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.common.util.SummaryByCycleUtils;
import org.qcmg.qprofiler2.bam.BamSummaryReport2;
import org.qcmg.qprofiler2.summarise.CycleSummaryTest;
import org.qcmg.qprofiler2.summarise.PositionSummary;
import org.qcmg.qprofiler2.util.SummaryReportUtilsTest;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;


public class BamSummaryReportTest {
		
	@Test
	public void testParseRNameAndPos() throws Exception {
		BamSummaryReport2 bsr = new BamSummaryReport2(new String [] {"matrices","coverage"}, -1, null, null, null);
		final String rg = "rg1";
		List<String> rgs = Arrays.asList(rg);
		bsr.setReadGroups(rgs );
		
		String rName = "test";
		int position = 999;		
		bsr.parseRNameAndPos( rName, position,rg );
		PositionSummary returnedSummary = bsr.getRNamePosition().get(rName);
		assertEquals( position, returnedSummary.getMax() );
		assertEquals( position, returnedSummary.getMin() );
		assertEquals( 1, returnedSummary.getCoverage().get(0).get() );
		
		// and again - min and max should stay the same, count should increase
		bsr.parseRNameAndPos(rName, position,rg );
		returnedSummary = bsr.getRNamePosition().get(rName);
		assertEquals(position, returnedSummary.getMax());
		assertEquals(position, returnedSummary.getMin());
		assertEquals(2, returnedSummary.getCoverage().get(0).get());
		
		// add another position to this rName
		position = 1000000;
		bsr.parseRNameAndPos(rName, position,rg );
		returnedSummary = bsr.getRNamePosition().get(rName);
		assertEquals(position, returnedSummary.getMax());
		assertEquals(999, returnedSummary.getMin());
		assertEquals(1, returnedSummary.getCoverageByRgs(rgs).get(1).get(0) );
		
		// add another position to this rName
		position = 0;
		bsr.parseRNameAndPos(rName, position,rg );
		returnedSummary = bsr.getRNamePosition().get(rName);
		assertEquals(1000000, returnedSummary.getMax());
		assertEquals(position, returnedSummary.getMin());
		assertEquals(3, returnedSummary.getCoverageByRgs(rgs).get(0).get(0) );
		assertEquals(1, returnedSummary.getCoverageByRgs(rgs).get(1).get(0) );
		
		// add a new rname
		rName = "new rname";
		bsr.parseRNameAndPos(rName, 0,rg );
		returnedSummary = bsr.getRNamePosition().get(rName);
		assertEquals(0, returnedSummary.getMax());
		assertEquals(0, returnedSummary.getMin());
		assertEquals(1, returnedSummary.getCoverageByRgs(rgs).get(0).get(0) );
		assertEquals(1, returnedSummary.getCoverageByRgs(rgs).get(0).length() );
		
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
	
	@Test
	public void getLengthsFromSummaryByCycleTest() throws Exception{ 
		String input = "input.sam";
		CycleSummaryTest.createInputFile(input);
		Element root = QprofilerXmlUtils.createRootElement("root",null);
		BamSummarizer2 bs = new BamSummarizer2();
		BamSummaryReport2 sr = (BamSummaryReport2) bs.summarize( input ); 
		sr.toXml(root);	
  		
		checklength( root, true, QprofilerXmlUtils.seqLength + "_"+  QprofilerXmlUtils.FirstOfPair, new int[] {141,151}, new int[] { 1,1 });
		checklength( root, true, QprofilerXmlUtils.seqLength + "_"+  QprofilerXmlUtils.SecondOfPair, new int[] {151}, new int[] { 1});
		checklength( root, false, QprofilerXmlUtils.qualLength + "_"+  QprofilerXmlUtils.FirstOfPair, new int[] {143,151}, new int[] { 1,1 });
		checklength( root, false, QprofilerXmlUtils.qualLength + "_"+  QprofilerXmlUtils.SecondOfPair, new int[] {151}, new int[] { 1});		
	}
		
}


