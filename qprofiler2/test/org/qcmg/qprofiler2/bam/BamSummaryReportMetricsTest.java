package org.qcmg.qprofiler2.bam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


import java.io.File;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.util.XmlElementUtils;
import org.qcmg.qprofiler2.summarise.ReadGroupSummary;
import org.qcmg.qprofiler2.summarise.ReadGroupSummaryTest;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.testng.Assert;
import org.w3c.dom.Element;

/**
 * 
 * @author christix
 * 
 * check <bamMetrics>...<sequenceMetrics name="..." readCount="...">.
 *
 */
public class BamSummaryReportMetricsTest {
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	Element root; 
	
	@Before
	public void setup() throws Exception {
		root = XmlElementUtils.createRootElement("root",null); 
		
		File input = testFolder.newFile("testInputFile.sam");
		ReadGroupSummaryTest.createInputFile (input);		
		BamSummarizer2 bs = new BamSummarizer2();
		//BamSummarizer2 bs = new BamSummarizer2( 200, null, true);
		BamSummaryReport2 sr = (BamSummaryReport2) bs.summarize(input.getAbsolutePath()); 		
		sr.toXml(root);	
		
		//debug
		XmlElementUtils.asXmlText(root, "/users/christix/Documents/Eclipse/gitHub/qprofiler_jun2019/adamajava/qprofiler2/qprofiler1.xml");		
	}	
	
	
	@Test
	/**
	 *<RNAME><sequenceMetrics readCount="6">
	 *<SEQ> 
	 *<sequenceMetrics name="seqBase" readCount="6">...</sequenceMetrics>
	 *<sequenceMetrics name="seqLength" readCount="6">...</sequenceMetrics>
	 *<sequenceMetrics name="badBase" readCount="6">...</sequenceMetrics>
	 *<sequenceMetrics name="2mers" readCount="6">...</sequenceMetrics>
	 *<sequenceMetrics name="3mers" readCount="6">...</sequenceMetrics>
	 *<sequenceMetrics name="6mers" readCount="6">...</sequenceMetrics>
	 *<QUAL>
	 *<sequenceMetrics name="qualBase" readCount="6">...</sequenceMetrics>
	 *<sequenceMetrics name="qualLength" readCount="6">...</sequenceMetrics>
	 *<sequenceMetrics name="badBase" readCount="6">...</sequenceMetrics>
	 *
	 * readCount: total reads but excludes notProperpair, unmapped, duplicate and  discarded reads 
	 * which is same to the sum of below
	 * <bamSummary>..<value name="unpairedReads">
	 * <sequenceMetrics name="properPairs">...<<value name="firstOfPairs"> and <value name="secondOfPairs">
	 * 
	 */
	public void rnameQualSeqTest() {
		String properCounts = getProperReadCount(null);
		int freq = 0;
		for(String tagName : new String[] { XmlUtils.RNAME, XmlUtils.SEQ , XmlUtils.QUAL }) {
			Element tagE = XmlElementUtils.getOffspringElementByTagName(root, tagName).get(0);
			for(Element ele : XmlElementUtils.getChildElementByTagName(tagE, XmlUtils.SEQUENCE_METRICS)) {
				assertEquals( ele.getAttribute(ReadGroupSummary.READ_COUNT),properCounts);
				freq ++;
			}
		}
		 //make sure totally checked 10 elements
		assertEquals( freq , 10);
	}

	@Test
	/**
	 * <MAPQ><sequenceMetrics readCount="12">
	 * <FLAG><sequenceMetrics readCount="12">
	 * readCount: Total reads including discarded reads. which is same to:
	 * <bamSummary><sequenceMetrics name="Overall"><value name="Total reads including discarded reads">
	 * 
	 */
	public void flagMapqTest() {
		Element ele = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.BAM_SUMMARY).get(0);						
		ele = getElementByFirst(ele, XmlUtils.VALUE,  k -> k.getAttribute(XmlUtils.NAME).equals("Total reads including discarded reads"));
		String totalCount = ele.getTextContent();
		
		for(String tagName : new String[] { XmlUtils.FLAG, XmlUtils.MAPQ }) {
			ele = XmlElementUtils.getOffspringElementByTagName(root, tagName).get(0);			
			String readCount = XmlElementUtils.getChildElement(ele,  XmlUtils.SEQUENCE_METRICS, 0).getAttribute(ReadGroupSummary.READ_COUNT);
						
			//System.out.println(ele.getTextContent() + ".equals" + count1 );
			assertTrue( totalCount.equals(readCount) );		
		}
	}
		
	@Test
	/**
	 * <bamMetrics><QNAME><readGroups><readGroup name="1959T"><sequenceMetrics name="qnameInfo" readCount="3">
	 * <bamMetrics><POS><readGroups><readGroup name="1959T"><sequenceMetrics readCount="3">
	 * readCount: total reads but excluds notProperpair, unmapped, duplicate and  discarded reads 
	 * which is same to the sum of below
	 * <bamSummary>..<value name="unpairedReads">
	 * <sequenceMetrics name="properPairs">...<<value name="firstOfPairs"> and <value name="secondOfPairs">
	 */
	public void qNamePosTest() {
		
		for(String tagName : new String[] { XmlUtils.QNAME, XmlUtils.POS }) {
			Element tagE = XmlElementUtils.getOffspringElementByTagName(root, tagName).get(0);
			
			for(String rg : new String[] {"1959T", "1959N", "unkown_readgroup_id" }) {			
				Element ele =getElementByFirst(tagE, "readGroup",  k -> k.getAttribute(XmlUtils.NAME).equals(rg));
				String readCount = tagName.equals(XmlUtils.POS ) ? 
					 XmlElementUtils.getChildElement(ele,  XmlUtils.SEQUENCE_METRICS, 0).getAttribute(ReadGroupSummary.READ_COUNT) :
					 getElementByFirst(ele, XmlUtils.SEQUENCE_METRICS,  k -> k.getAttribute(XmlUtils.NAME).equals("qnameInfo")).getAttribute(ReadGroupSummary.READ_COUNT);
				 		
				assertTrue(readCount.equals(getProperReadCount(rg)));			
			}			
		}		
	}
		
	private Element getElementByFirst(Element parent, String tagName, Predicate<? super Element> predicate ) {
		
		return XmlElementUtils.getOffspringElementByTagName(parent, tagName).stream().filter( (Predicate<? super Element>) predicate).findFirst().get();
	}
	
	/**
	 * 
	 * @param rg is readGroup. null means all readGroups
	 * @return the properPaired read count or notPaired read count
	 */
	private String getProperReadCount(String rg) {
		Element bamSummaryE = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.BAM_SUMMARY).get(0);
		
		if(rg != null) {
			Element rgE = getElementByFirst(bamSummaryE, "readGroup",  k -> k.getAttribute(XmlUtils.NAME).equals(rg));
			Element ele = getElementByFirst(rgE, XmlUtils.SEQUENCE_METRICS,  k -> k.getAttribute(XmlUtils.NAME).equals("properPairs"));
			int sum = XmlElementUtils.getOffspringElementByTagName(ele, XmlUtils.VALUE).stream()
					.filter( k -> k.getAttribute(XmlUtils.NAME).equals("firstOfPairs") | k.getAttribute(XmlUtils.NAME).equals("secondOfPairs"))
					.mapToInt(k -> Integer.parseInt( k.getTextContent())).sum();
			
			ele = getElementByFirst(rgE, XmlUtils.SEQUENCE_METRICS,  k -> k.getAttribute(XmlUtils.NAME).equals("reads"));
			sum+= XmlElementUtils.getOffspringElementByTagName(ele, XmlUtils.VALUE).stream().filter( k -> k.getAttribute(XmlUtils.NAME).equals("unpairedReads"))
					.mapToInt(k -> Integer.parseInt( k.getTextContent())).sum();
			
			return sum + "";		
		}
		
		//rg == null for all readGroup
		int sum = 0;
		//counts from properPair
		List<Element> eles =  XmlElementUtils.getOffspringElementByTagName(bamSummaryE, XmlUtils.SEQUENCE_METRICS ).stream()
				.filter( k -> k.getAttribute(XmlUtils.NAME).equals("properPairs") ).collect( Collectors.toList());				
		for(Element ele : eles) {
			sum += XmlElementUtils.getOffspringElementByTagName(ele, XmlUtils.VALUE).stream()
					.filter( k -> k.getAttribute(XmlUtils.NAME).equals("firstOfPairs") | k.getAttribute(XmlUtils.NAME).equals("secondOfPairs"))
					.mapToInt(k -> Integer.parseInt( k.getTextContent())).sum();
			
		}
		//counts from unPairedReads
		sum+= XmlElementUtils.getOffspringElementByTagName(bamSummaryE, XmlUtils.VALUE).stream().filter( k -> k.getAttribute(XmlUtils.NAME).equals("unpairedReads"))
				.mapToInt(k -> Integer.parseInt( k.getTextContent())).sum();
		
		return sum + "";			
	}
	
}


