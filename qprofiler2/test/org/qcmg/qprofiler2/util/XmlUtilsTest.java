package org.qcmg.qprofiler2.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.qcmg.common.util.XmlElementUtils;
import org.w3c.dom.Element;

public class XmlUtilsTest {
	
	@Test
	public void updateMapWithLimitTest() {
		Map<String, AtomicLong> map = new HashMap<>();
		int limit = 10;
		
		for(int i = 1; i <= limit; i++) {
			assertTrue(XmlUtils.updateMapWithLimit(map, i+"", limit));
			assertTrue(map.get(i+"").get() == 1);
			assertFalse(map.containsKey(XmlUtils.OTHER));
			assertTrue(map.size() == i);
		}
		
		for(int i = 1; i <= limit; i++) {
			assertFalse(XmlUtils.updateMapWithLimit(map, i+"", limit));
			assertTrue(map.get(i+"").get() == 2);
			assertFalse(map.containsKey(XmlUtils.OTHER));
			assertTrue(map.size()==limit);
		}
		
		for(int i = limit+1; i < limit+5; i++) {
			assertFalse(XmlUtils.updateMapWithLimit(map, i+"", limit));
			assertTrue(map.get(XmlUtils.OTHER).get() == i-limit);
			assertTrue(map.size()==limit + 1);
		}
	}
	
	
	@Test
	public void outputTallyGroupWithSizeTest() throws ParserConfigurationException {
		Map<String, AtomicLong> map = new HashMap<>();
		int limit = 10;
		
		for(int i = 1; i <= limit*2; i++) {			
			XmlUtils.updateMapWithLimit(map, i+"", limit);			 
		}
		for(int i = limit-5; i <= limit+5; i++) {			
			XmlUtils.updateMapWithLimit(map, i+"", limit);			 
		}
		
		Element root = XmlElementUtils.createRootElement( "root", null );
		XmlUtils.outputTallyGroupWithSize(root, "test", map, limit);
		
		Element ele = XmlElementUtils.getChildElement(root, XmlUtils.VARIABLE_GROUP,0);
		assertTrue(ele.getAttribute(XmlUtils.COUNT).equals("31"));//[1..20][5..15]
		assertTrue(ele.getAttribute(XmlUtils.TALLY_COUNT).equals(limit+"+"));
					
		int[] counts = new int[] {1,1,1,1,2,2,2,2,2,2};
		int[] values = new int[] {1,2,3,4,5,6,7,8,9,10};
		for(Element e : XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.TALLY)) {
			boolean isFind = false;		 
			if(e.getAttribute(XmlUtils.VALUE).equals(XmlUtils.OTHER)){
					assertTrue(e.getAttribute(XmlUtils.COUNT).equals("15"));
					isFind = true;
			}else {
				for(int i = 0; i<=10; i++) {		 
					if(e.getAttribute(XmlUtils.VALUE).equals(values[i] + "")) {
						assertTrue(e.getAttribute(XmlUtils.COUNT).equals(counts[i]+""));
						isFind = true;
						break;
					}
				}
			}
			assertTrue(isFind);
		}		
	}
}
