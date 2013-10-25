package org.qcmg.common.model;

import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.junit.Test;
import org.qcmg.common.model.CigarStringComparator;

public class CigarStringComparatorTest {
	
	@Test
	public void testCompare(){
		Map<String, Integer> map = new TreeMap<String, Integer>(new CigarStringComparator());
		map.put("2H",1);
		map.put("3H",1);
		map.put("10H",1);
		map.put("11H",1);
		map.put("20H",1);
		map.put("300H",1);
		map.put("1I",1);
		map.put("1H",1);
		map.put("2I",1);
		map.put("399C",1);
		map.put("90C",1);
		map.put("2D",1);
		map.put("3M",1);
		map.put("399C",1);
		map.put("1M",1);
		map.put("35C",1);
		map.put("100M",1);
		map.put("500D",1);
		map.put("200M",1);
		map.put("50M",1);
		
		int i = 0;
		String s = "C";		// set to the lowest cigar character
		
		for (Entry<String, Integer> entry : map.entrySet()) {
			String[] array = entry.getKey().split("[HIDCM]");
			
			int number = Integer.parseInt(array[0]);
			String string = entry.getKey().substring(entry.getKey().length()-1);
			if (string.equals(s) && number < i)
				Assert.fail("number: " + number + " is less than i: " + i);
			if (string.compareTo(s) < 0)
				Assert.fail("string: " + string + " is less than s: " + s);
			
			i = number;
			s = string;
		}
		
		Assert.assertTrue(((TreeMap<String, Integer>)map).firstKey().equals("35C"));
		Assert.assertTrue(((TreeMap<String, Integer>)map).lastKey().equals("200M"));
	}
	
	@Test
	public void testEmptyCigarString() {
		Map<String, Integer> map = new TreeMap<String, Integer>(new CigarStringComparator());
		map.put("1D",1);
		try {
			map.put("",1);
			Assert.fail("Should have thrown a ClassCastException");
		} catch (ClassCastException cce) {}
	}
	
}
