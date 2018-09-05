package org.qcmg.qprofiler2.util;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import junit.framework.Assert;
import org.junit.Test;
import org.qcmg.common.util.SummaryByCycleUtils;
import org.qcmg.qprofiler2.summarise.PositionSummary;
import org.qcmg.qprofiler2.util.SummaryReportUtils;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SummaryReportUtilsTest {
	
	@Test
	public void testTallyQualScoresInvalid() {
		ConcurrentMap<Integer, AtomicLong> badQualCount = new ConcurrentHashMap<Integer, AtomicLong>();
		
		// null string and null seperator
		SummaryReportUtils.tallyQualScores(null, badQualCount, null);
		Assert.assertTrue(badQualCount.isEmpty());
		
		// empty string and null seperator
		String badQual = "";
		SummaryReportUtils.tallyQualScores(badQual, badQualCount, null);
		Assert.assertTrue(badQualCount.isEmpty());
		
		// empty string
		SummaryReportUtils.tallyQualScores(badQual, badQualCount, "");
		Assert.assertFalse(badQualCount.isEmpty());
		Assert.assertEquals(1, badQualCount.get(Integer.valueOf(0)).get());
		
		// valid string, but incorrect separator
		badQual = "1,1,1,1,1";
		try {
			SummaryReportUtils.tallyQualScores(badQual, badQualCount, "");
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().startsWith("For input string"));
		}
	}
	
	@Test
	public void testAddPositionAndLengthToMap() {
		ConcurrentMap<Integer, AtomicLong> map = new ConcurrentHashMap<Integer, AtomicLong>();
		addPositionAndLengthToMap(map, 10, 100);
		
		Assert.assertEquals(100, map.size());
		Assert.assertNull(map.get(0));
		Assert.assertNull(map.get(9));
		Assert.assertNull(map.get(110));
		Assert.assertEquals(1, map.get(10).get());
		Assert.assertEquals(1, map.get(109).get());
		
		
		addPositionAndLengthToMap(map, 100, 50);
		Assert.assertEquals(140, map.size());
		Assert.assertNull(map.get(0));
		Assert.assertNull(map.get(9));
		Assert.assertNull(map.get(150));
		Assert.assertEquals(1, map.get(10).get());
		Assert.assertEquals(2, map.get(109).get());
		

		// adding 0 positions and size - should not affect anything...
		addPositionAndLengthToMap(map, 0, 0);
		Assert.assertEquals(140, map.size());
		Assert.assertNull(map.get(0));
		Assert.assertNull(map.get(9));
		Assert.assertNull(map.get(150));
		Assert.assertEquals(1, map.get(10).get());
		Assert.assertEquals(2, map.get(109).get());
		
		addPositionAndLengthToMap(map, 100, 10);
		Assert.assertEquals(140, map.size());
		Assert.assertNull(map.get(0));
		Assert.assertNull(map.get(9));
		Assert.assertNull(map.get(150));
		Assert.assertEquals(1, map.get(10).get());
		Assert.assertEquals(3, map.get(109).get());
		
		
		addPositionAndLengthToMap(map, 10000, 2);
		Assert.assertEquals(142, map.size());
		Assert.assertNull(map.get(0));
		Assert.assertNull(map.get(9));
		Assert.assertNull(map.get(150));
		Assert.assertNull(map.get(10002));
		Assert.assertEquals(1, map.get(10).get());
		Assert.assertEquals(3, map.get(109).get());
		Assert.assertEquals(1, map.get(10000).get());
		Assert.assertEquals(1, map.get(10001).get());
		
	}
	
	@Test
	public void testTallyQualScoresValid() {
		ConcurrentMap<Integer, AtomicLong> badQualCount = new ConcurrentHashMap<Integer, AtomicLong>();
		
		// valid string, valid seperator
		String qual = "1,1,1,1,1";
		SummaryReportUtils.tallyQualScores(qual, badQualCount, ",");
		Assert.assertEquals((1), badQualCount.get(Integer.valueOf(5)).get());
		
		qual = "1,2,3,4,5";
		SummaryReportUtils.tallyQualScores(qual, badQualCount, ",");
		Assert.assertEquals((2), badQualCount.get(Integer.valueOf(5)).get());
		
		qual = "9,9,9,9,9";
		SummaryReportUtils.tallyQualScores(qual, badQualCount, ",");
		Assert.assertEquals((3), badQualCount.get(Integer.valueOf(5)).get());
		
		qual = "1,2,3,9,9,10,11,12,13,14,15";
		SummaryReportUtils.tallyQualScores(qual, badQualCount, ",");
		Assert.assertEquals((4), badQualCount.get(Integer.valueOf(5)).get());
		
		// all values over 10
		qual = "10,11,12,13,14,15";
		SummaryReportUtils.tallyQualScores(qual, badQualCount, ",");
		Assert.assertEquals((4), badQualCount.get(Integer.valueOf(5)).get());
		Assert.assertEquals((1), badQualCount.get(Integer.valueOf(0)).get());
		
	}
	
	@Test
	public void testLengthMapToXML() throws Exception {
		Element root = createElement("testLengthMapToXML");
		
		ConcurrentNavigableMap<Integer, AtomicLong> map = new ConcurrentSkipListMap<Integer, AtomicLong>();
		SummaryReportUtils.lengthMapToXml(root, "test", map);
		
		Assert.assertTrue(root.hasChildNodes());
		Assert.assertEquals(1, root.getChildNodes().getLength());
		Assert.assertEquals("test", root.getChildNodes().item(0).getNodeName());
		Assert.assertFalse(root.getChildNodes().item(0).hasChildNodes());
		Assert.assertFalse(root.getChildNodes().item(0).hasAttributes());
		
		
		// same again this time with some data!
		map.put(100, new AtomicLong(42));
		map.put(101, new AtomicLong(41));
		map.put(102, new AtomicLong(40));
		map.put(103, new AtomicLong(39));
		map.put(104, new AtomicLong(38));
		map.put(105, new AtomicLong(37));
		SummaryReportUtils.lengthMapToXml(root, "test42", map);
		
		Assert.assertTrue(root.hasChildNodes());
		Assert.assertEquals(2, root.getChildNodes().getLength());
		Assert.assertEquals("test42", root.getChildNodes().item(1).getNodeName());
		
		Element element42ValueTally = (Element) root.getChildNodes().item(1).getChildNodes().item(0);
		Assert.assertTrue(element42ValueTally.hasChildNodes());
		Assert.assertEquals(6, element42ValueTally.getChildNodes().getLength());
		
		// first element
		Element element42TallyItem = (Element) element42ValueTally.getChildNodes().item(0);
		Assert.assertFalse(element42TallyItem.hasChildNodes());
		Assert.assertTrue(element42TallyItem.hasAttributes());
		Assert.assertEquals(100, Integer.parseInt(element42TallyItem.getAttribute("value")));
		Assert.assertEquals(42, Integer.parseInt(element42TallyItem.getAttribute("count")));
		
		// second element
		element42TallyItem = (Element) element42ValueTally.getChildNodes().item(1);
		Assert.assertFalse(element42TallyItem.hasChildNodes());
		Assert.assertTrue(element42TallyItem.hasAttributes());
		Assert.assertEquals(101, Integer.parseInt(element42TallyItem.getAttribute("value")));
		Assert.assertEquals(41, Integer.parseInt(element42TallyItem.getAttribute("count")));
		
		// last element
		element42TallyItem = (Element) element42ValueTally.getChildNodes().item(5);
		Assert.assertFalse(element42TallyItem.hasChildNodes());
		Assert.assertTrue(element42TallyItem.hasAttributes());
		Assert.assertEquals(105, Integer.parseInt(element42TallyItem.getAttribute("value")));
		Assert.assertEquals(37, Integer.parseInt(element42TallyItem.getAttribute("count")));
	}
	
	@Test
	public void testBinnedLengthMapToRangeTallyXML() throws Exception {
		Element root = createElement("testBinnedLengthMapToRangeTallyXML");
		
		ConcurrentNavigableMap<Integer, AtomicLong> map = new ConcurrentSkipListMap<Integer, AtomicLong>();
		SummaryReportUtils.binnedLengthMapToRangeTallyXml(root, map);
		
		Assert.assertTrue(root.hasChildNodes());
		Assert.assertEquals(1, root.getChildNodes().getLength());
		Assert.assertEquals("RangeTally", root.getChildNodes().item(0).getNodeName());
		Assert.assertFalse(root.getChildNodes().item(0).hasChildNodes());
		Assert.assertFalse(root.getChildNodes().item(0).hasAttributes());
		
		
		// same again this time with some data!
		map.put(100, new AtomicLong(42));
		map.put(110, new AtomicLong(41));
		map.put(120, new AtomicLong(40));
		map.put(130, new AtomicLong(39));
		map.put(140, new AtomicLong(38));
		map.put(150, new AtomicLong(37));
		SummaryReportUtils.binnedLengthMapToRangeTallyXml(root, map);
		
		Assert.assertTrue(root.hasChildNodes());
		Assert.assertEquals(2, root.getChildNodes().getLength());
		Assert.assertEquals("RangeTally", root.getChildNodes().item(1).getNodeName());
		
		Element element42RangeTally = (Element) root.getChildNodes().item(1);
		Assert.assertTrue(element42RangeTally.hasChildNodes());
		Assert.assertEquals(6, element42RangeTally.getChildNodes().getLength());
		
		// first element
		Element element42RangeTallyItem = (Element) element42RangeTally.getChildNodes().item(0);
		Assert.assertFalse(element42RangeTallyItem.hasChildNodes());
		Assert.assertTrue(element42RangeTallyItem.hasAttributes());
		Assert.assertEquals(100, Integer.parseInt(element42RangeTallyItem.getAttribute("start")));
		Assert.assertEquals(100, Integer.parseInt(element42RangeTallyItem.getAttribute("end")));
		Assert.assertEquals(42, Integer.parseInt(element42RangeTallyItem.getAttribute("count")));
		
		// second element
		element42RangeTallyItem = (Element) element42RangeTally.getChildNodes().item(1);
		Assert.assertFalse(element42RangeTallyItem.hasChildNodes());
		Assert.assertTrue(element42RangeTallyItem.hasAttributes());
		Assert.assertEquals(110, Integer.parseInt(element42RangeTallyItem.getAttribute("start")));
		Assert.assertEquals(110, Integer.parseInt(element42RangeTallyItem.getAttribute("end")));
		Assert.assertEquals(41, Integer.parseInt(element42RangeTallyItem.getAttribute("count")));
		
		// third element
		element42RangeTallyItem = (Element) element42RangeTally.getChildNodes().item(2);
		Assert.assertFalse(element42RangeTallyItem.hasChildNodes());
		Assert.assertTrue(element42RangeTallyItem.hasAttributes());
		Assert.assertEquals(120, Integer.parseInt(element42RangeTallyItem.getAttribute("start")));
		Assert.assertEquals(120, Integer.parseInt(element42RangeTallyItem.getAttribute("end")));
		Assert.assertEquals(40, Integer.parseInt(element42RangeTallyItem.getAttribute("count")));
		
		// last element
		element42RangeTallyItem = (Element) element42RangeTally.getChildNodes().item(5);
		Assert.assertFalse(element42RangeTallyItem.hasChildNodes());
		Assert.assertTrue(element42RangeTallyItem.hasAttributes());
		Assert.assertEquals(150, Integer.parseInt(element42RangeTallyItem.getAttribute("start")));
		Assert.assertEquals(150, Integer.parseInt(element42RangeTallyItem.getAttribute("end")));
		Assert.assertEquals(37, Integer.parseInt(element42RangeTallyItem.getAttribute("count")));
	}
	
	
	@Test
	public void testPostionSummaryMapToXml() throws Exception {
		Element root = createElement("testPostionSummaryMapToXml");
		List<String> rgs = Arrays.asList(new String[] {"rg1", "rg2"});
		
		ConcurrentMap<String, PositionSummary> map = new ConcurrentHashMap<String, PositionSummary>();
		SummaryReportUtils.coverageByReadGroupToXml(root, "test", map,  rgs );
		
		Assert.assertTrue(root.hasChildNodes());
		Assert.assertEquals(1, root.getChildNodes().getLength());
		Assert.assertEquals("test", root.getChildNodes().item(0).getNodeName());
		Assert.assertFalse(root.getChildNodes().item(0).hasChildNodes());
		Assert.assertFalse(root.getChildNodes().item(0).hasAttributes());
		
		PositionSummary ps = new PositionSummary( rgs );
		ps.addPosition(42, "rg1" );
		for (int i = 0 ; i <= 10000000 ; i++) { ps.addPosition(i,"rg1"); }
		map.put("chr1", ps);
 
		// same again this time with some data!		
		map.put("chr2", new PositionSummary(rgs)); map.get("chr2").addPosition(41, "rg1");
		map.put("chr3", new PositionSummary(rgs)); map.get("chr3").addPosition(40, "rg1"); 
		map.put("chr4", new PositionSummary(rgs)); map.get("chr4").addPosition(39, "rg1"); 
		map.put("chr5", new PositionSummary(rgs)); map.get("chr5").addPosition(38, "rg1"); 
		map.put("chr6", new PositionSummary(rgs)); map.get("chr6").addPosition(37, "rg1"); 		
		
		SummaryReportUtils.coverageByReadGroupToXml(root, "test42", map , rgs );		
		Assert.assertTrue(root.hasChildNodes());
		Assert.assertEquals(2, root.getChildNodes().getLength());
		Assert.assertEquals("test42", root.getChildNodes().item(1).getNodeName());
		
		Element element42RName = (Element) root.getChildNodes().item(1).getChildNodes().item(0);
		Assert.assertEquals("chr1", element42RName.getAttribute("source"));
		Assert.assertEquals(0, Integer.parseInt(element42RName.getAttribute("minPosition")));
		Assert.assertEquals(10000000, Integer.parseInt(element42RName.getAttribute("maxPosition")));
		Assert.assertEquals(10000002, Integer.parseInt(element42RName.getAttribute("count")));		
		Assert.assertEquals(11, element42RName.getChildNodes().getLength());

		// first element
		Element element42RangeTallyItem = (Element) element42RName.getChildNodes().item(0);
		Assert.assertEquals(0, Integer.parseInt(element42RangeTallyItem.getAttribute("start")));
		Assert.assertEquals(999999, Integer.parseInt(element42RangeTallyItem.getAttribute("end")));
		Assert.assertEquals(1000001, Integer.parseInt(element42RangeTallyItem.getAttribute("count")) );
		Assert.assertEquals("1000001,0", element42RangeTallyItem.getAttribute("counts") );	
 		
		for(int i = 1; i < 10; i ++){
			// 2nd ~ 9th
			element42RangeTallyItem = (Element) element42RName.getChildNodes().item(i);
			Assert.assertEquals(1000000 * i, Integer.parseInt(element42RangeTallyItem.getAttribute("start")));
			Assert.assertEquals(1000000 * i + 999999, Integer.parseInt(element42RangeTallyItem.getAttribute("end")));
			Assert.assertEquals(1000000, Integer.parseInt(element42RangeTallyItem.getAttribute("count")) );
			Assert.assertEquals("1000000,0", element42RangeTallyItem.getAttribute("counts") );	
		}
		
		// last element
		element42RangeTallyItem = (Element) element42RName.getChildNodes().item(10);
		Assert.assertEquals(10000000, Integer.parseInt(element42RangeTallyItem.getAttribute("start")));
		Assert.assertEquals(10999999, Integer.parseInt(element42RangeTallyItem.getAttribute("end")));
		Assert.assertEquals(1, Integer.parseInt(element42RangeTallyItem.getAttribute("count")) );
		Assert.assertEquals("1,0", element42RangeTallyItem.getAttribute("counts") );	

		
		
		// next rname
		element42RName = (Element) root.getChildNodes().item(1).getChildNodes().item(1);
        Assert.assertTrue(element42RName.hasChildNodes());
        Assert.assertTrue(element42RName.hasAttributes());
        Assert.assertEquals("chr2", element42RName.getAttribute("source"));
        Assert.assertEquals(41, Integer.parseInt(element42RName.getAttribute("minPosition")));
        Assert.assertEquals(41, Integer.parseInt(element42RName.getAttribute("maxPosition")));
        Assert.assertEquals(1, Integer.parseInt(element42RName.getAttribute("count")));
        Assert.assertEquals(1, element42RName.getChildNodes().getLength());
	}

	private Element createElement(String methodName) throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		DOMImplementation domImpl = builder.getDOMImplementation();
		Document doc = domImpl.createDocument(null, "SummaryReportUtilsTest." + methodName, null);
		return doc.getDocumentElement();
	}
	
	/**
	 * 
	 * @param map
	 * @param position
	 * @param length
	 */
	public static void addPositionAndLengthToMap(ConcurrentMap<Integer, AtomicLong> map, int position, int length) {
		for (int i = position ; i < position + length ; i++) {
			SummaryByCycleUtils.incrementCount(map, Integer.valueOf(i));
		}
	}
	
}
