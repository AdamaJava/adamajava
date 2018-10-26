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
	
	@Test@Deprecated
	public void testLengthMapToXML() throws Exception {

	}
	
	@Test @Deprecated
	public void testBinnedLengthMapToRangeTallyXML() throws Exception {
	
	}
	
	
	@Test @Deprecated
	public void testPostionSummaryMapToXml() throws Exception {
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
