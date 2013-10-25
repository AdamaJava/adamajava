package org.qcmg.common.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class MapUtilsTest {
	
	@Test
	public void testSplitMap() {
		Map<Integer, String> testMap = new HashMap<Integer, String>();
		
		testMap.put(1, "one");
		
		Assert.assertEquals(1, MapUtils.splitMap(testMap, 1).size());
		testMap.put(2, "two");
		Assert.assertEquals(1, MapUtils.splitMap(testMap, 1).size());
		Assert.assertEquals(2, MapUtils.splitMap(testMap, 2).size());
		Assert.assertEquals(3, MapUtils.splitMap(testMap, 3).size());
		testMap.put(3, "three");
		testMap.put(4, "four");
		testMap.put(5, "five");
		testMap.put(6, "six");
		testMap.put(7, "seven");
		testMap.put(8, "eight");
		Assert.assertEquals(4, MapUtils.splitMap(testMap, 4).size());
		Assert.assertEquals(2, MapUtils.splitMap(testMap, 4).get(0).size());
		Assert.assertEquals(2, MapUtils.splitMap(testMap, 4).get(1).size());
		Assert.assertEquals(2, MapUtils.splitMap(testMap, 4).get(2).size());
		Assert.assertEquals(2, MapUtils.splitMap(testMap, 4).get(3).size());
		
		Assert.assertEquals(2, MapUtils.splitMap(testMap, 2).size());
		Assert.assertEquals(4, MapUtils.splitMap(testMap, 2).get(0).size());
		Assert.assertEquals(4, MapUtils.splitMap(testMap, 2).get(1).size());
		
		Assert.assertEquals(1, MapUtils.splitMap(testMap, 1).size());
		Assert.assertEquals(8, MapUtils.splitMap(testMap, 1).get(0).size());
		
		Assert.assertEquals(3, MapUtils.splitMap(testMap, 3).size());
		Assert.assertEquals(8, MapUtils.splitMap(testMap, 1).get(0).size());
		
		Assert.assertEquals(8, MapUtils.splitMap(testMap, 8).size());
		Assert.assertEquals(1, MapUtils.splitMap(testMap, 8).get(0).size());
		Assert.assertEquals(1, MapUtils.splitMap(testMap, 8).get(3).size());
		Assert.assertEquals(1, MapUtils.splitMap(testMap, 8).get(7).size());
	}

}
