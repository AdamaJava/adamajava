package org.qcmg.qprofiler2.bam;

import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.w3c.dom.Element;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.util.XmlElementUtils;
import org.qcmg.qprofiler2.summarise.PairSummaryTest;
import org.qcmg.qprofiler2.summarise.ReadGroupSummary;
import org.qcmg.qprofiler2.util.XmlUtils;


/**
 * 
 * @author christix
 * 
 * check <bamMetrics>...<sequenceMetrics name="..." readCount="...">.
 *
 */
public class BamSummaryReportMetricsTest {
	@ClassRule
	public static  TemporaryFolder testFolder = new TemporaryFolder();
	
	static Element root; 
	
	@BeforeClass
    public static void beforeClassFunction() {
		try {
			root = XmlElementUtils.createRootElement("root",null);
			File input = testFolder.newFile("testInputFile.sam");
			PairSummaryTest.createPairInputFile(input);
			BamSummarizer bs = new BamSummarizer();
			BamSummaryReport sr = (BamSummaryReport) bs.summarize(input.getAbsolutePath()); 		
			sr.toXml(root);	
		} catch (Exception e) {
			fail("can't create root element!");
		
		} 
	}
	
	/**
	 * it take all inputed reads and then count the reads with nominated tag value; 
	 */
	@Test
	public void tagTest() {
		
		Element bamSummaryE = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.BAM_SUMMARY).get(0);
		
		// check RG
		Element ele = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.SEQUENCE_METRICS).stream()
				.filter(k -> k.getAttribute(XmlUtils.NAME).equals("tags:RG:Z")).findFirst().get();
		
		int scount = 0;
		for (Element ele1 : XmlElementUtils.getOffspringElementByTagName(ele, XmlUtils.TALLY)) {
			String rg = ele1.getAttribute(XmlUtils.VALUE);
			
			// bamSummary			
			Element ele2 = getElementByFirst(bamSummaryE, "readGroup",  k -> k.getAttribute(XmlUtils.NAME).equals(rg));
			ele2 = getElementByFirst(ele2, XmlUtils.VARIABLE_GROUP ,  k -> k.getAttribute(XmlUtils.NAME).equals("countedReads"));
			ele2 = getElementByFirst(ele2, XmlUtils.VALUE,  k -> k.getAttribute(XmlUtils.NAME).equals(ReadGroupSummary.READ_COUNT));
			assertEquals(ele1.getAttribute(XmlUtils.COUNT) , ele2.getTextContent());			
			scount += Integer.parseInt(ele1.getAttribute(XmlUtils.COUNT));	
		}
		
		assertEquals(scount+"", ele.getAttribute(ReadGroupSummary.READ_COUNT));
						 
		// get tag read counts from grep command		
		int tagMetriNo = (int) XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.SEQUENCE_METRICS).stream()
				.filter(k -> k.getAttribute(XmlUtils.NAME).startsWith("tags:")).count();
		String[] tags = new String[] {"tags:MD:Z", "tags:AS:i","tags:CQ:Z", "tags:CS:Z", "tags:HI:i" ,"tags:NH:i" , "tags:NM:i" };	
		assertEquals(tags.length+1, tagMetriNo);
		
		int[] readCounts = new int[] {6, 6, 12, 12, 6, 6, 6 };		
		for (int i = 0; i < tags.length; i ++) {
			String tag = tags[i];
			ele = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.SEQUENCE_METRICS).stream()
					.filter(k -> k.getAttribute(XmlUtils.NAME).equals(tag)).findFirst().get();
			assertEquals(ele.getAttribute(ReadGroupSummary.READ_COUNT),readCounts[i]+ "");
		}
		
	}
	
	
	/**
	 * tLen take properpair or notProperpair as input, due to RAM limitation, only count pair with tLen < 5000
	 */
	@Test
	public void tLenTest() {	
		Element tagE = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.TLEN).get(0);
		for (String rg : new String[] {"1959T", "1959N", "unkown_readgroup_id" }) {
			Element ele =getElementByFirst(tagE, "readGroup",  k -> k.getAttribute(XmlUtils.NAME).equals(rg));						
			
			// for (String mName: new String[] {"tLenInProperPair", "overlapBaseInProperPair" ,    })
			// "tLenInProperPair" vs bamSummary
			Element ele1 = getElementByFirst(ele, XmlUtils.SEQUENCE_METRICS,  k -> k.getAttribute(XmlUtils.NAME).equals("tLenInProperPair"));
			String readCount = ele1 == null ? "0" : ele1.getAttribute(ReadGroupSummary.PAIR_COUNT);
			int summaryCount = getSummaryRgCounts(rg, "properPairs", null, k -> k.getAttribute(XmlUtils.NAME).equals("pairCountUnderTlen5000")) ;
			assertEquals(readCount, summaryCount+"");
			
			// "overlapBaseInProperPair" vs bamSummary
			ele1 = 	getElementByFirst(ele, XmlUtils.SEQUENCE_METRICS,  k -> k.getAttribute(XmlUtils.NAME).equals("overlapBaseInProperPair"));
			readCount  = ele1 == null ? "0" : ele1.getAttribute(ReadGroupSummary.PAIR_COUNT);			
			summaryCount = getSummaryRgCounts(rg, "properPairs", null, k -> k.getAttribute(XmlUtils.NAME).equals("overlappedPairs")) ;
			assertEquals(readCount, summaryCount+"");
			
			// "tLenInNotProperPair"  vs bamSummary
			ele1 = getElementByFirst(ele, XmlUtils.SEQUENCE_METRICS,  k -> k.getAttribute(XmlUtils.NAME).equals("tLenInNotProperPair"));
			readCount  = ele1 == null ? "0" : ele1.getAttribute(ReadGroupSummary.PAIR_COUNT);			
			summaryCount = getSummaryRgCounts(rg, "notProperPairs", null, k -> k.getAttribute(XmlUtils.NAME).equals("pairCountUnderTlen5000")) ;
			assertEquals(readCount, summaryCount+"");
			
			
			// "overlapBaseInNotProperPair"  vs bamSummary			
			ele1 = getElementByFirst(ele, XmlUtils.SEQUENCE_METRICS,  k -> k.getAttribute(XmlUtils.NAME).equals("overlapBaseInNotProperPair"));
			readCount  = ele1 == null ? "0" : ele1.getAttribute(ReadGroupSummary.PAIR_COUNT);	
			summaryCount = getSummaryRgCounts(rg, "notProperPairs", null, k -> k.getAttribute(XmlUtils.NAME).equals("overlappedPairs")) ;
			assertEquals(readCount, summaryCount+"");			
		}		
		
	}
	
	
	/**
	 * <bamSummary><readGroups>...<variableGroup name="countedReads"><value name="readCount">6</value>
	 * <bamMetrics><CIGAR><readGroups>...<sequenceMetrics readCount="6">
	 */
	@Test
	public void cigarTest() {
		Element summaryE = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.BAM_SUMMARY).get(0);			
		Element tagE = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.CIGAR).get(0);
		for (String rg : new String[] {"1959T", "1959N", "unknown_readgroup_id" }) {
			// get counts from <bamMetrics>
			Element ele =getElementByFirst(tagE, "readGroup",  k -> k.getAttribute(XmlUtils.NAME).equals(rg));
			String readCount = XmlElementUtils.getChildElement(ele,  XmlUtils.SEQUENCE_METRICS, 0).getAttribute(ReadGroupSummary.READ_COUNT);

			// get counts from <bamSummary>
			ele = getElementByFirst(summaryE, "readGroup",  k -> k.getAttribute(XmlUtils.NAME).equals(rg));
			ele = getElementByFirst(ele, XmlUtils.VARIABLE_GROUP,  k -> k.getAttribute(XmlUtils.NAME).equals("countedReads"));
			ele = getElementByFirst(ele, XmlUtils.VALUE,  k -> k.getAttribute(XmlUtils.NAME).equals(ReadGroupSummary.READ_COUNT));
			
			assertEquals(readCount, ele.getTextContent());
		}		
	}
	
	
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
	@Test
	public void rnameQualSeqTest() {
		String properCounts = getProperReadCount(null);
		int freq = 0;
		for (String tagName : new String[] {XmlUtils.RNAME, XmlUtils.SEQ , XmlUtils.QUAL }) {
			Element tagE = XmlElementUtils.getOffspringElementByTagName(root, tagName).get(0);
			for (Element ele : XmlElementUtils.getChildElementByTagName(tagE, XmlUtils.SEQUENCE_METRICS)) {
				
				assertEquals(ele.getAttribute(ReadGroupSummary.READ_COUNT),properCounts);
				freq ++;
			}
		}
		// make sure totally checked 10 elements
		assertEquals(freq , 10);
	}

	
	/**
	 * <MAPQ><sequenceMetrics readCount="12">
	 * <FLAG><sequenceMetrics readCount="12">
	 * readCount: Total reads including discarded reads. which is same to:
	 * <bamSummary><sequenceMetrics name="Overall"><value name="Total reads including discarded reads">
	 * 
	 */
	@Test
	public void flagMapqTest() {
		Element ele = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.BAM_SUMMARY).get(0);						
		ele = getElementByFirst(ele, XmlUtils.VALUE,  k -> k.getAttribute(XmlUtils.NAME).equals("Total reads including discarded reads"));
		String totalCount = ele.getTextContent();
		
		for (String tagName : new String[] {XmlUtils.FLAG, XmlUtils.MAPQ }) {
			ele = XmlElementUtils.getOffspringElementByTagName(root, tagName).get(0);			
			String readCount = XmlElementUtils.getChildElement(ele,  XmlUtils.SEQUENCE_METRICS, 0).getAttribute(ReadGroupSummary.READ_COUNT);
						
			assertTrue(totalCount.equals(readCount));		
		}
	}
	
	
//	/**
//	 * <bamMetrics><QNAME><readGroups><readGroup name="1959T"><sequenceMetrics name="qnameInfo" readCount="3">
//	 * readCount: total reads but excluds notProperpair, unmapped, duplicate and  discarded reads
//	 * which is same to the sum of below
//	 * <bamSummary>..<value name="unpairedReads">
//	 * <sequenceMetrics name="properPairs">...<<value name="firstOfPairs"> and <value name="secondOfPairs">
//	 */
//	@Test
//	public void qNameTest() {
//		Element nameE = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.QNAME).get(0);
//		Element bamSummaryE = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.BAM_SUMMARY).get(0);
//
//		for (String rg : new String[] {"1959T", "1959N", "unknown_readgroup_id" }) {
//			Element ele =getElementByFirst(nameE, "readGroup",  k -> k.getAttribute(XmlUtils.NAME).equals(rg));
//			String count1 = XmlElementUtils.getChildElement(ele,  XmlUtils.SEQUENCE_METRICS, 0).getAttribute(ReadGroupSummary.READ_COUNT);
//
//			Element eleSum = getElementByFirst(bamSummaryE, "readGroup",  k -> k.getAttribute(XmlUtils.NAME).equals(rg));
//			String count2 = getElementByFirst(eleSum, XmlUtils.SEQUENCE_METRICS,  k -> k.getAttribute(XmlUtils.NAME).equals("reads")).getAttribute(ReadGroupSummary.READ_COUNT);
//
//			assertTrue(count1.equals(count2));
//		}
//	}
	
	/**
	 * <bamMetrics><QNAME><readGroups><readGroup name="1959T"><sequenceMetrics name="qnameInfo" readCount="3">
	 * <bamMetrics><POS><readGroups><readGroup name="1959T"><sequenceMetrics readCount="3">
	 * readCount: total reads but excluds notProperpair, unmapped, duplicate and  discarded reads 
	 * which is same to the sum of below
	 * <bamSummary>..<value name="unpairedReads">
	 * <sequenceMetrics name="properPairs">...<<value name="firstOfPairs"> and <value name="secondOfPairs">
	 */
	@Test
	public void qNamePosTest() {

		Element tagE = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.POS).get(0);
		
		for (String rg : new String[] {"1959T", "1959N", "unknown_readgroup_id" }) {			
			Element ele =getElementByFirst(tagE, "readGroup",  k -> k.getAttribute(XmlUtils.NAME).equals(rg));
		   
			// test pos element for good reads only
			String readCount = XmlElementUtils.getChildElement(ele,  XmlUtils.SEQUENCE_METRICS, 0).getAttribute(ReadGroupSummary.READ_COUNT);
			String readCountSum = getProperReadCount(rg);			  	   
			assertTrue(readCount.equals(readCountSum));	 									
		}						
	}
		
	private Element getElementByFirst(Element parent, String tagName, Predicate<? super Element> predicate) {
				
		return XmlElementUtils.getOffspringElementByTagName(parent, tagName).stream().filter(predicate).findFirst().orElse(null);		 
	}

	/**
	 * 
	 * @param rg is readGroup. null means all readGroups
	 * @return the properPaired read count or notPaired read count
	 */
	private String getProperReadCount(String rg) {		
		int sum = getSummaryRgCounts(rg,"properPairs", null, k -> k.getAttribute(XmlUtils.NAME).equals("firstOfPairs") | k.getAttribute(XmlUtils.NAME).equals("secondOfPairs"));
		sum +=  getSummaryRgCounts(rg, "reads", null,  k -> k.getAttribute(XmlUtils.NAME).equals("unpairedReads"));
		return String.valueOf(sum); 		
	}
	
	private int getSummaryRgCounts(String rg, String metricName, String variableName, Predicate<? super Element> filter) {
		
		Element bamSummaryE = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.BAM_SUMMARY).get(0);
		if (rg != null) {
			bamSummaryE = getElementByFirst(bamSummaryE, "readGroup",  k -> k.getAttribute(XmlUtils.NAME).equals(rg));
		}
		List<Element> eles =  XmlElementUtils.getOffspringElementByTagName(bamSummaryE, XmlUtils.SEQUENCE_METRICS);
		if (metricName != null) {	
			eles = 	eles.stream().filter(k -> k.getAttribute(XmlUtils.NAME).equals(metricName)).collect(Collectors.toList());			
		}
		
		int sum = 0;
		for (Element ele : eles) {
			List<Element> eleVGs = XmlElementUtils.getOffspringElementByTagName(ele, XmlUtils.VARIABLE_GROUP);
			if (variableName != null) {
				eleVGs = eleVGs.stream().filter(k -> k.getAttribute(XmlUtils.NAME).equals(variableName)).collect(Collectors.toList());			
			}
						
			for (Element ele1 : eleVGs) {
				sum += XmlElementUtils.getOffspringElementByTagName(ele1, XmlUtils.VALUE).stream().filter(filter)
						.mapToInt(k -> Integer.parseInt(k.getTextContent())).sum();
			}
		}
		
		return sum;		
	}
	
	
	@Test
	public void speedTest() throws IOException {
		List<String> myList  = new ArrayList<>();
		
		for (int i = 0; i < 101; i ++) {
			myList.add(i + "::"+UUID.randomUUID().toString());
		}
		myList.sort(Comparator.comparing(String::toString));
		
		// test accuracy
		for (int i = 0; i < 1000; i ++) {
			String str1 = myList.get(i%100);
			int order = Collections.binarySearch(myList, str1, null); 
			assertEquals(order, myList.indexOf(str1));
		}
		
		int freq = 10000;
		String str;
		long start = System.currentTimeMillis();		
		for (int i = 0; i < freq; i ++) {
			str = myList.get(i%100);
			Collections.binarySearch(myList, str, null);  
		}
		QLogger.getRunTime(start, System.currentTimeMillis());
		
		start = System.currentTimeMillis();		
		for (int i = 0; i < freq; i ++) {
			str = myList.get(i%100);
			myList.indexOf(str);
		}
		QLogger.getRunTime(start, System.currentTimeMillis());		
	}
	
}


