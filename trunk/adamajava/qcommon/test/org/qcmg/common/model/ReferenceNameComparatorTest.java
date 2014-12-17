package org.qcmg.common.model;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import junit.framework.Assert;

import org.junit.Test;

public class ReferenceNameComparatorTest {
	
	@Test
	public void testCompare(){
		Map<String, Integer> map = new TreeMap<String, Integer>(new ReferenceNameComparator());
		map.put("chr20",1);
		map.put("chr21",1);
		map.put("chr22",1);
		map.put("chr6",1);
		map.put("chr2",1);
		map.put("chr19",1);
		map.put("chr3",1);
		map.put("chr1",1);
		map.put("chr4",1);
		map.put("chr16",1);
		map.put("chr15",1);
		map.put("chr11",1);
		map.put("chr17",1);
		map.put("chr10",1);
		map.put("chr5",1);
		map.put("chr7",1);
		map.put("chr13",1);
		map.put("chr18",1);
		map.put("chr9",1);
		map.put("chr8",1);
		map.put("chr12",1);
		map.put("chr14",1);
		
		int i = 1;
		for (Entry<String, Integer> entry : map.entrySet()) {
			Assert.assertEquals(i++, Integer.parseInt(entry.getKey().split("chr")[1]));
		}
	}
	
	@Test
	public void compareNonChr(){
		TreeMap<String, Integer> map = new TreeMap<String, Integer>(new ReferenceNameComparator());
		map.put("20",1);
		map.put("21",1);
		map.put("22",1);
		map.put("6",1);
		map.put("2",1);
		map.put("19",1);
		map.put("3",1);
		map.put("1",1);
		map.put("4",1);
		map.put("16",1);
		map.put("Y",1);
		map.put("15",1);
		map.put("11",1);
		map.put("17",1);
		map.put("10",1);
		map.put("MT",1);
		map.put("5",1);
		map.put("7",1);
		map.put("13",1);
		map.put("18",1);
		map.put("X",1);
		map.put("9",1);
		map.put("8",1);
		map.put("12",1);
		map.put("14",1);
		
		
		int i = 1;
		for (Entry <String, Integer> entry : map.entrySet()) {
			String key = entry.getKey();
			if (i < 23) {
				Assert.assertEquals(i, Integer.parseInt(key));
			} else if (i == 23) {
				Assert.assertEquals("MT", key);
			} else if (i == 24) {
				Assert.assertEquals("X", key);
			} else if (i == 25) {
				Assert.assertEquals("Y", key);
			}
			i++;
		}
	}
	
	@Test
	public void testCompareWithGL(){
		Map<String, Integer> map = new TreeMap<String, Integer>(new ReferenceNameComparator());
		map.put("chr20",1);
		map.put("chr21",1);
		map.put("chr22",1);
		map.put("chr6",1);
		map.put("chr2",1);
		map.put("GL000217.1",1);
		map.put("chr19",1);
		map.put("chr3",1);
		map.put("chr1",1);
		map.put("GL000237.1",1);
		map.put("chr4",1);
		map.put("chr16",1);
		map.put("chr15",1);
		map.put("GL000225.1",1);
		map.put("chr11",1);
		map.put("chr17",1);
		map.put("chr10",1);
		map.put("chr5",1);
		map.put("GL000249.1",1);
		map.put("chr7",1);
		map.put("chr13",1);
		map.put("chr18",1);
		map.put("chr9",1);
		map.put("chr8",1);
		map.put("GL000193.1",1);
		map.put("chr12",1);
		map.put("chr14",1);
		
		int i = 1;
		for (Entry<String, Integer> entry : map.entrySet()) {
			if (entry.getKey().startsWith("chr"))
				Assert.assertEquals(i++, Integer.parseInt(entry.getKey().split("chr")[1]));
		}
	}
	
	@Test
	public void testCompareMix(){
		Map<String, Integer> map = new TreeMap<String, Integer>(new ReferenceNameComparator());
		map.put("chr20",1);
		map.put("chr2",1);
		map.put("chr19",1);
		map.put("chr3",1);
		map.put("chrY",1);
		map.put("chr1",1);
		map.put("chr18",1);
		map.put("chr9",1);
		map.put("chrX",1);
		map.put("chrMT",1);
		map.put("chr8",1);
		map.put("chr12",1);
		map.put("chr14",1);
		
		Assert.assertTrue(((TreeMap<String, Integer>)map).firstKey().equals("chr1"));
		Assert.assertTrue(((TreeMap<String, Integer>)map).lastKey().equals("chrY"));
	}
	
	@Test
	public void testCompareMix2(){
		Map<String, Integer> map = new TreeMap<String, Integer>(new ReferenceNameComparator());
		map.put("chr20",1);
		map.put("chr2",1);
		map.put("chr19",1);
		map.put("chr3",1);
		map.put("chrY",1);
		map.put("chr10",1);
		map.put("chr18",1);
		map.put("chr9",1);
		map.put("chrX",1);
		map.put("chrMT",1);
		map.put("chr8",1);
		map.put("chr12",1);
		map.put("chr14",1);
		
		Assert.assertTrue(((TreeMap<String, Integer>)map).firstKey().equals("chr2"));
		Assert.assertTrue(((TreeMap<String, Integer>)map).lastKey().equals("chrY"));
	}
	
	@Test
	public void testCompareWithRandom(){
		Map<String, Integer> map = new TreeMap<String, Integer>(new ReferenceNameComparator());
		map.put("chr20",1);
		map.put("chr2",1);
		map.put("chr19",1);
		map.put("chr3",1);
		map.put("chrY",1);
		map.put("chr1",1);
		map.put("chr18",1);
		map.put("chr9",1);
		map.put("chrX",1);
		map.put("chrMT",1);
		map.put("chr8",1);
		map.put("chr12",1);
		map.put("chr14",1);
		
		Assert.assertTrue(((TreeMap<String, Integer>)map).firstKey().equals("chr1"));
		Assert.assertTrue(((TreeMap<String, Integer>)map).lastKey().equals("chrY"));
		
	}
	
	@Test
	public void testWithLongContigName(){
		Map<String, Integer> map = new TreeMap<String, Integer>(new ReferenceNameComparator());
		map.put("chr20",1);
		map.put("chr2",1);
		map.put("chr19",1);
		map.put("chr3",1);
		map.put("chrY",1);
		map.put("chr1_ctg5_hap1",1);
		map.put("chr1",1);
		map.put("chr17_ctg5_hap1",1);
		map.put("chr18",1);
		map.put("chr9",1);
		map.put("chrX",1);
		map.put("chrMT",1);
		map.put("chr8",1);
		map.put("chr12",1);
		map.put("chr14",1);
		map.put("chr17",1);
		
		assertEquals(16, map.size());
		
		Assert.assertTrue(((TreeMap<String, Integer>)map).firstKey().equals("chr1"));
		Assert.assertTrue(((TreeMap<String, Integer>)map).containsKey("chr1_ctg5_hap1"));
		Assert.assertTrue(((TreeMap<String, Integer>)map).containsKey("chr17_ctg5_hap1"));
		Assert.assertTrue(((TreeMap<String, Integer>)map).containsKey("chr17"));
		Assert.assertTrue(((TreeMap<String, Integer>)map).lastKey().equals("chrY"));
	}
	
	@Test
	public void testStringCompare() {
		int compareResult = "chr5".compareToIgnoreCase("GL012345");
		Assert.assertEquals(true, compareResult < 0);
	}
	
	@Test
	public void testCompareChrAndGL(){
		Map<String, Integer> map = new TreeMap<String, Integer>(new ReferenceNameComparator());
		map.put("chr10",1);
		map.put("chr5",1);
		map.put("GL000249.1",1);
		map.put("chr7",1);
		map.put("chr13",1);
		map.put("chr18",1);
		map.put("chr9",1);
		map.put("chr8",1);
		map.put("GL000193.1",1);
		map.put("chr12",1);
		map.put("chr14",1);
		
		assertEquals("chr5", ((TreeMap<String, Integer>)map).firstKey());
		Assert.assertTrue(((TreeMap<String, Integer>)map).lastKey().equals("GL000249.1"));
		
	}
	
	@Test
	public void testNewReferenceChrNames() {
		TreeMap<String, Integer> map = new TreeMap<String, Integer>(new ReferenceNameComparator());
		map.put("chr1_gl000191_random", 1);
		map.put("chr1", 1);
		assertEquals(2, map.size());
		
		map.put("chr10_gl000191_random", 1);
		map.put("chr10", 1);
		assertEquals(4, map.size());
		
		Assert.assertTrue(map.firstKey().equals("chr1"));
		Assert.assertTrue(map.lastKey().equals("chr10_gl000191_random"));
	}
}
