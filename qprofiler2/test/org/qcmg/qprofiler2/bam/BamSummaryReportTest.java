package org.qcmg.qprofiler2.bam;

import static org.junit.Assert.*;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;



import org.qcmg.qprofiler2.summarise.*;
import org.w3c.dom.Element;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.XmlElementUtils;
import org.qcmg.picard.BwaPair.Pair;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.NodeList;


public class BamSummaryReportTest {

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

			
	@Test
	public void testParseRNameAndPos() throws Exception {
		BamSummaryReport bsr = new BamSummaryReport(-1, false, false);
		final String rg = "rg1";
		bsr.setReadGroups(Arrays.asList(rg));
		
		String rName = "test";
		int position = 999;		
		bsr.parseRNameAndPos(rName, position,rg);
		PositionSummary returnedSummary = bsr.getRNamePosition().get(rName);
		assertEquals(position, returnedSummary.getMax());
		assertEquals(position, returnedSummary.getMin());
		assertEquals(1, returnedSummary.getCoverageByRg(rg).get(0).get());
		
		// and again - min and max should stay the same, count should increase
		bsr.parseRNameAndPos(rName, position,rg);
		returnedSummary = bsr.getRNamePosition().get(rName);
		assertEquals(position, returnedSummary.getMax());
		assertEquals(position, returnedSummary.getMin());
		assertEquals(2, returnedSummary.getCoverageByRg(rg).get(0).get());

		// add another position to this rName
		position = 1000000;
		bsr.parseRNameAndPos(rName, position,rg);
		returnedSummary = bsr.getRNamePosition().get(rName);
		assertEquals(position, returnedSummary.getMax());
		assertEquals(999, returnedSummary.getMin());
		assertEquals(1, returnedSummary.getCoverageByRg(rg).get(1).get());
		
		// add another position to this rName
		position = 0;
		bsr.parseRNameAndPos(rName, position,rg);
		returnedSummary = bsr.getRNamePosition().get(rName);
		assertEquals(1000000, returnedSummary.getMax());
		assertEquals(position, returnedSummary.getMin());
		assertEquals(3, returnedSummary.getCoverageByRg(rg).get(0).get());
		assertEquals(1, returnedSummary.getCoverageByRg(rg).get(1).get());
		
		// add a new rname
		rName = "new rname";
		bsr.parseRNameAndPos(rName, 0,rg);
		returnedSummary = bsr.getRNamePosition().get(rName);
		assertEquals(0, returnedSummary.getMax());
		assertEquals(0, returnedSummary.getMin());
		assertEquals(1, returnedSummary.getCoverageByRg(rg).get(0).get());
		assertEquals(1, returnedSummary.getCoverageByRg(rg).size());
				
	}


			
	@Test
	public void testCompareWithSAMUtils() {
		String inputString = "!''*((((***+))%%%++)(%%%%).1***-+*''))**55CCF>>>>>>CCCCCCC65";
		String expectedOutputString = "BHHKIIIIKKKLJJFFFLLJIFFFFJORKKKNLKHHJJKKVVddg______dddddddWV";
		int counter = 100000;
		String outputString = null;

		for (int i = 0 ; i < counter ; i++) {
			outputString = StringUtils.addASCIIValueToChar(inputString, 33);
		}
								
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
		if (counts.length != values.length)
			throw new Exception("error: values size must be same to counts size");
		
		String nodeName = (isSeq)? XmlUtils.SEQ  : XmlUtils.QUAL ;
		String sLength = isSeq? XmlUtils.SEQ_LENGTH : XmlUtils.QUAL_LENGTH;
		
		// get node <SEQ> or <QUAL>
		List<Element> lists = XmlElementUtils.getOffspringElementByTagName(root, nodeName);
		assertEquals(1, lists.size());
		
		// get node <sequenceMetrics name="seqLength"> or <sequenceMetrics name="qualLength">		
		lists = XmlElementUtils.getOffspringElementByTagName(lists.get(0), XmlUtils.SEQUENCE_METRICS).stream()
			.filter(e -> e.getAttribute(XmlUtils.NAME).equals(sLength)).collect(Collectors.toList());
		assertEquals(1, lists.size());
		
		// <variableGroup name="firstReadInPair"> or <variableGroup name="secondReadInPair">				
		lists = XmlElementUtils.getOffspringElementByTagName(lists.get(0), XmlUtils.VARIABLE_GROUP).stream()
				.filter(e -> e.getAttribute(XmlUtils.NAME).equals(pairName)).collect(Collectors.toList());		
		assertEquals(1, lists.size());				
		
		Element ele = lists.get(0);
		assertEquals(values.length, ele.getChildNodes().getLength());	
			
		// eg.QprofilerXmlUtils.seqLength + "_"+  QprofilerXmlUtils.FirstOfPair;
		for (int i = 0; i < values.length; i ++) {
			String v = values[i] + "";
			String c = counts[i] + "";
			long count = XmlElementUtils.getChildElementByTagName(ele, XmlUtils.TALLY).stream()
					.filter(e -> e.getAttribute(XmlUtils.VALUE).equals(v)
							&& e.getAttribute(XmlUtils.COUNT).equals(c)).count();
			assertTrue(count == 1);
		}	 
	}

	private void checklengthLongRead(Element root, boolean isSeq,  String pairName) throws Exception {

		String nodeName = (isSeq)? XmlUtils.SEQ  : XmlUtils.QUAL ;
		String sLength = isSeq? XmlUtils.SEQ_LENGTH : XmlUtils.QUAL_LENGTH;

		// get node <SEQ> or <QUAL>
		List<Element> lists = XmlElementUtils.getOffspringElementByTagName(root, nodeName);
		assertEquals(1, lists.size());

		// get node <sequenceMetrics name="seqLength"> or <sequenceMetrics name="qualLength">
		lists = XmlElementUtils.getOffspringElementByTagName(lists.get(0), XmlUtils.SEQUENCE_METRICS).stream()
				.filter(e -> e.getAttribute(XmlUtils.NAME).equals(sLength)).collect(Collectors.toList());
		assertEquals(1, lists.size());

		// <variableGroup name="firstReadInPair"> or <variableGroup name="secondReadInPair">
		//Long read so these elements should not exists
		lists = XmlElementUtils.getOffspringElementByTagName(lists.get(0), XmlUtils.VARIABLE_GROUP).stream()
				.filter(e -> e.getAttribute(XmlUtils.NAME).equals(pairName)).collect(Collectors.toList());
		assertEquals(0, lists.size());
	}
		
	private void checkTally(Element root, String groupName, String value, int count, int expectedNo) {
		
		long findNo = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.TALLY).stream()
			.filter(e -> e.getAttribute(XmlUtils.VALUE).equals(value) && e.getAttribute(XmlUtils.COUNT).equals(count + "") &&
					((Element) e.getParentNode()).getAttribute(XmlUtils.NAME).equals(groupName)).count();

		assertEquals(expectedNo , findNo);
	}

	@Test
	public void checkLength_rname_mapq_for_longread() throws Exception {
		File input = testFolder.newFile("testLongReadInputFile.sam");
		Element root = createLongReadRoot(input);

		// length
		checklengthLongRead(root, true,  XmlUtils.FIRST_PAIR);
		checklengthLongRead(root, true,  XmlUtils.SECOND_PAIR);
		checklengthLongRead(root, false, XmlUtils.FIRST_PAIR);
		checklengthLongRead(root, false, XmlUtils.SECOND_PAIR);

		// rNAME
		Element node = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.RNAME).get(0);
		checkTally(node, XmlUtils.RNAME, "chr22", 3, 1);

		// mapq is for all counted reads disregard duplicate ect, unmapped reads mapq=0
		// Zero mapping quality indicates that the read maps to multiple locations or differet ref
		node = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.MAPQ).get(0);
		checkTally(node,  XmlUtils.UNPAIRED, "1", 1, 1);
		checkTally(node,  XmlUtils.UNPAIRED, "60", 3, 1);

	}
		
	@Test
	// check read length and rname, both ignor readgroup
	public void checkLength_rname_mapq() throws Exception {
		String input = CycleSummaryTest.createInputFile(testFolder);
		Element root = XmlElementUtils.createRootElement("root",null);
		BamSummarizer bs = new BamSummarizer();
		BamSummaryReport sr = (BamSummaryReport) bs.summarize(input); 
		sr.toXml(root);			
		
		// length
		checklength(root, true,  XmlUtils.FIRST_PAIR, new int[] {141,151}, new int[] {1,1 });
		checklength(root, true,  XmlUtils.SECOND_PAIR, new int[] {151}, new int[] {1});
		checklength(root, false, XmlUtils.FIRST_PAIR, new int[] {143,151}, new int[] {1,1 });
		checklength(root, false, XmlUtils.SECOND_PAIR, new int[] {151}, new int[] {1});		
		
		// rNAME
		Element node = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.RNAME).get(0);	
		checkTally(node, XmlUtils.RNAME, "chr1", 2, 1);
		// the second of pair on chr11 is duplicated, so here only the first of pair are counted
		checkTally(node, XmlUtils.RNAME, "chr11", 1, 1);
			
		// mapq is for all counted reads disregard duplicate ect, unmapped reads mapq=0
		// Zero mapping quality indicates that the read maps to multiple locations or differet ref
		node = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.MAPQ).get(0);	
		checkTally(node,  XmlUtils.FIRST_PAIR, "0", 1, 1);
		checkTally(node,  XmlUtils.FIRST_PAIR, "25", 1, 1);
		checkTally(node,  XmlUtils.SECOND_PAIR, "0", 1, 1);	
		checkTally(node,  XmlUtils.SECOND_PAIR, "6", 1, 1);			
	}
		
	
	/** the algorithm is inside ReadGroupSummary::parseRecord::parsePairing
	 * only middleTlenValue > record tLen > 0 exclude discard reads, duplicate, unmapped
	 * @throws Exception
	 */
	@Test
	public void checkTlen() throws Exception {
		File input = testFolder.newFile("testInputFile.sam");
		Element root = PairSummaryTest.createPairRoot(input);		
		 

		final Element tlenE = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.TLEN).get(0);
		final List<Element> rgsE =  XmlElementUtils.getOffspringElementByTagName(tlenE, "readGroup");

		//Check overall length
		List<Element> tallyE = getTallys(rgsE, "1959T", "Overall" , 4);
		chekOverallTlen(tallyE, "Overall", 1, 13);
		chekOverallTlen(tallyE, "Overall", 1, 26);
		chekOverallTlen(tallyE, "Overall", 1, 93);
		chekOverallTlen(tallyE, "Overall", 1, 2025);

		tallyE = getTallys(rgsE, "1959N", "Overall" , 1);
		chekOverallTlen(tallyE, "Overall", 1, 175);

		tallyE = getTallys(rgsE, "unknown_readgroup_id", "Overall" , 2);
		chekOverallTlen(tallyE, "Overall", 3, 0);
		chekOverallTlen(tallyE, "Overall", 1, 76);


		// five pairs in 1959T, we only record 13, 26, 2015		
		tallyE = getTallys(rgsE, "1959T", "tLenInProperPair" , 4);
		chekTlen(tallyE, Pair.F3F5, 1, 93);
		chekTlen(tallyE, Pair.F5F3, 1, 2025);
		chekTlen(tallyE, Pair.Outward, 1, 13);
		chekTlen(tallyE, Pair.Outward, 1, 26);
				
		// only record popular TLEN that is tLen < middleTlenValue) isize.increment(tLen);	
		// 1959N only one overlap pair tlen is 175
		tallyE = getTallys(rgsE, "1959N", "tLenInProperPair" , 1); 
		chekTlen(tallyE, Pair.Inward, 1, 175);		
		tallyE = getTallys(rgsE, "1959N", "overlapBaseInProperPair" , 1); 
		chekTlen(tallyE, Pair.Inward, 1, 62);
				
		tallyE = getTallys(rgsE, XmlUtils.UNKNOWN_READGROUP, "tLenInProperPair" , 1); 
		chekTlen(tallyE, Pair.Inward, 1, 76);		
		tallyE = getTallys(rgsE, XmlUtils.UNKNOWN_READGROUP, "overlapBaseInProperPair" , 1); 
		chekTlen(tallyE, Pair.Inward, 1, 75);	
		tallyE = getTallys(rgsE, XmlUtils.UNKNOWN_READGROUP, "tLenInNotProperPair" , 1); 
		chekTlen(tallyE, Pair.F3F5, 1, 0);
	}


	@Test
	public void checkRLength() throws Exception {
		File input = testFolder.newFile("testLongReadInputFile.sam");
		Element root = createLongReadRoot(input);

		final Element rlength = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.RLENGTH).get(0);
		final List<Element> rgsE =  XmlElementUtils.getOffspringElementByTagName(rlength, "readGroup");

		// five pairs in 1959T, we only record 13, 26, 2015
		List<Element> tallyE = getTallys(rgsE, "COLO829_BL.28464", "readLength" , 3);
		chekRlen(tallyE, 1, 212);
		chekRlen(tallyE, 1, 387);
		chekRlen(tallyE, 1, 2000);

	}

	private void chekRlen(List<Element> tallyE, int count, int value) {
		long no = tallyE.stream().filter(e ->
				((Element) e.getParentNode()).getAttribute(XmlUtils.NAME).equals("readLength") &&
						e.getAttribute(XmlUtils.VALUE).equals(value+"") && e.getAttribute(XmlUtils.COUNT).equals(count+"")
		).count();

		assertTrue(no == 1);
	}

	private void chekTlen(List<Element> tallyE, Pair p, int count, int value) {
		 long no = tallyE.stream().filter(e ->   
			((Element) e.getParentNode()).getAttribute(XmlUtils.NAME).equals(p.name()) &&
			 e.getAttribute(XmlUtils.VALUE).equals(value+"") && e.getAttribute(XmlUtils.COUNT).equals(count+"")		 
		).count();	
		
		 assertTrue(no == 1);		
	}

	private void chekOverallTlen(List<Element> tallyE, String name, int count, int value) {
		long no = tallyE.stream().filter(e ->
				((Element) e.getParentNode()).getAttribute(XmlUtils.NAME).equals(name) &&
						e.getAttribute(XmlUtils.VALUE).equals(value+"") && e.getAttribute(XmlUtils.COUNT).equals(count+"")
		).count();

		assertTrue(no == 1);
	}
	
	private List<Element> getTallys(List<Element> rgsE,String rgName, String metricName,  int size) {		
		Element ele = rgsE.stream().filter(e -> e.getAttribute(XmlUtils.NAME).equals(rgName)).findFirst().get();
		ele = XmlElementUtils.getChildElementByTagName(ele, XmlUtils.SEQUENCE_METRICS).stream().filter(e -> e.getAttribute(XmlUtils.NAME).equals(metricName)).findFirst().get();
		List<Element> eles = XmlElementUtils.getOffspringElementByTagName(ele, XmlUtils.TALLY);
		assertEquals(size, eles.size());	
	 
		return eles;
	}
	
	
	
	@Test
	public void unKnownIdTest() throws Exception {
		final Element root = XmlElementUtils.createRootElement("root",null);
		BamSummaryReport bsr = new BamSummaryReport(-1, false, false);
		SAMRecord record = new SAMRecord(null);
		record.setReadName("243_146_1");
		record.setBaseQualities(new byte[] {1,2,3,4,5,6,7});
		record.setReadBases(new byte[] {1,2,3,4,5,6,7});				
		record.setAlignmentStart(239007);
		record.setReferenceName("chrY");
		record.setFlags(64);		
		bsr.parseRecord(record);
		bsr.toXml(root);
		
		for (Element ele: XmlElementUtils.getOffspringElementByTagName(root, "readGroup")) {
			assertEquals(ele.getAttribute("name"), "unknown_readgroup_id");
		} 		
	}


	public static Element createLongReadRoot(File input) throws Exception {
		ReadGroupSummaryTest.createLongReadInputFile(input);
		BamSummarizer bs = new BamSummarizer(1000, null, false, true);
		BamSummaryReport sr = (BamSummaryReport) bs.summarize(input.getAbsolutePath());
		Element root = XmlElementUtils.createRootElement("root",null);
		sr.toXml(root);

		return root;
	}

	@Test
	public void testReadGroupOrder() throws Exception {
		BamSummaryReport report = new BamSummaryReport(3, false, false);
		String[] rg = new String[] {"cd90dd75-8a1f-4fd0-a352-0364d8dd5300","69f81d0d-c430-4a6f-9ccd-05ea88b22c1d","374ed445-b8ee-4a1d-9337-f3fdd661f408"};
		List list= Arrays.asList(rg);
		report.setReadGroups(Arrays.asList(rg));

		//Before sort
		assertEquals(list.get(0),"cd90dd75-8a1f-4fd0-a352-0364d8dd5300");
		assertEquals(list.get(1),"69f81d0d-c430-4a6f-9ccd-05ea88b22c1d");
		assertEquals(list.get(2),"374ed445-b8ee-4a1d-9337-f3fdd661f408");

		Element root =  XmlElementUtils.createRootElement("root", null);
		report.setReadGroupSummaries(list);
 		report.summaryToXml(root);

		List<String> readGroupNames = getAllReadGroupNames(root);
		assertTrue(readGroupNames.size() == 3);
		assertEquals(readGroupNames.get(0),"374ed445-b8ee-4a1d-9337-f3fdd661f408");
		assertEquals(readGroupNames.get(1),"69f81d0d-c430-4a6f-9ccd-05ea88b22c1d");
		assertEquals(readGroupNames.get(2),"cd90dd75-8a1f-4fd0-a352-0364d8dd5300");

	}

	public List<String> getAllReadGroupNames(Element root) {
		List<String> readGroupNames = new ArrayList<>();
		NodeList bamSummaryList = root.getElementsByTagName("bamSummary");
		for (int i = 0; i < bamSummaryList.getLength(); i++) {
			Element bamSummary = (Element) bamSummaryList.item(i);
			NodeList readGroupsList = bamSummary.getElementsByTagName("readGroups");
			for (int j = 0; j < readGroupsList.getLength(); j++) {
				Element readGroups = (Element) readGroupsList.item(j);
				NodeList readGroupList = readGroups.getElementsByTagName("readGroup");
				for (int k = 0; k < readGroupList.getLength(); k++) {
					Element readGroup = (Element) readGroupList.item(k);
					readGroupNames.add(readGroup.getAttribute("name"));
				}
			}
		}
		return readGroupNames;
	}
	
	@Test
	public void unpairedTest() throws ParserConfigurationException {
		BamSummaryReport report  = new BamSummaryReport(3, false, false);
		
		SAMRecord record = new SAMRecord(null);

		record.setReadName("TESTDATA");

		// first read
		record.setReadBases("ACCCT AACCC CAACC CTAAC CNTAA CCCTA ACCCA AC".replace(" ","").getBytes());		
		report.parseRecord(record);// unapired		
		record.setFlags(67); // firstInPair
		report.parseRecord(record);
				
		Element root =  XmlElementUtils.createRootElement("root", null);
		report.toXml(root);

		// check each node, make sure both "unPaired" and "firstReadinPair" are reported
		Element seqEle = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.SEQ).get(0);
		assertEquals(XmlElementUtils.getChildElementByTagName(seqEle, XmlUtils.SEQUENCE_METRICS).size(), 6);
		for (Element ele : XmlElementUtils.getChildElementByTagName(seqEle, XmlUtils.SEQUENCE_METRICS)) {
			assertEquals(ele.getAttribute(ReadGroupSummary.READ_COUNT) ,  "2");			
			assertEquals(XmlElementUtils.getChildElementByTagName(ele, XmlUtils.VARIABLE_GROUP).size(),2);			
			boolean hasUnpaired = false;
			boolean hasFirst = false;
			for (Element vele :  XmlElementUtils.getChildElementByTagName(ele, XmlUtils.VARIABLE_GROUP)) {				
				if (vele.getAttribute(XmlUtils.NAME).equals("unPaired")) {
					hasUnpaired = true;
				} else if (vele.getAttribute(XmlUtils.NAME).equals(XmlUtils.FIRST_PAIR)) {
					hasFirst = true;
				}				
			}			
			assertTrue(hasUnpaired && hasFirst); // must have both 
		}
		
		
		
	}




}
