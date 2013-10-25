package org.qcmg.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ListUtilsTest {
	
	public static List<String> ABCDEF = new ArrayList<String>();
	public static List<String> IJK = new ArrayList<String>();
	static {
		ABCDEF.add("A");
		ABCDEF.add("B");
		ABCDEF.add("C");
		ABCDEF.add("D");
		ABCDEF.add("E");
		IJK.add("I");
		IJK.add("J");
		IJK.add("K");
	}
	
	@Test
	public void testListComparator() {
		Comparator<String> abcde = ListUtils.createComparatorFromList(ABCDEF);
		Comparator<String> ijk = ListUtils.createComparatorFromList(IJK);
		
		List<String> unsortedList = new ArrayList<String>();
		unsortedList.add("E");
		unsortedList.add("D");
		unsortedList.add("C");
		unsortedList.add("B");
		unsortedList.add("A");
		
		Collections.sort(unsortedList, abcde);
		
		Assert.assertEquals("A", unsortedList.get(0));
		Assert.assertEquals("B", unsortedList.get(1));
		Assert.assertEquals("C", unsortedList.get(2));
		Assert.assertEquals("D", unsortedList.get(3));
		Assert.assertEquals("E", unsortedList.get(4));
		
		unsortedList.add("I");
		unsortedList.add("J");
		unsortedList.add("K");
		unsortedList.add("L");
		
		Collections.sort(unsortedList, ijk);
		
		Assert.assertEquals("I", unsortedList.get(0));
		Assert.assertEquals("J", unsortedList.get(1));
		Assert.assertEquals("K", unsortedList.get(2));
		Assert.assertEquals("A", unsortedList.get(3));
		Assert.assertEquals("B", unsortedList.get(4));
		Assert.assertEquals("C", unsortedList.get(5));
		Assert.assertEquals("D", unsortedList.get(6));
		Assert.assertEquals("E", unsortedList.get(7));
		Assert.assertEquals("L", unsortedList.get(8));
	}
	
	@Test
	public void testListComparatorDuplicateListEntries() {
		List<String> sortedList = new ArrayList<String>();
		sortedList.add("A");
		sortedList.add("B");
		sortedList.add("C");
		sortedList.add("C");
		sortedList.add("D");
		sortedList.add("E");
		
		Comparator<String> c = ListUtils.createComparatorFromList(sortedList);
		
		List<String> unsortedList = new ArrayList<String>();
		unsortedList.add("E");
		unsortedList.add("D");
		unsortedList.add("C");
		unsortedList.add("B");
		unsortedList.add("A");
		
		Collections.sort(unsortedList, c);
		
		Assert.assertEquals("A", unsortedList.get(0));
		Assert.assertEquals("B", unsortedList.get(1));
		Assert.assertEquals("C", unsortedList.get(2));
		Assert.assertEquals("D", unsortedList.get(3));
		Assert.assertEquals("E", unsortedList.get(4));
	}
	
	@Test
	public void testListComparatorEmptyList() {
		List<String> sortedList = new ArrayList<String>();
		Comparator<String> c = ListUtils.createComparatorFromList(sortedList);
		
		List<String> unsortedList = new ArrayList<String>();
		unsortedList.add("E");
		unsortedList.add("D");
		unsortedList.add("C");
		unsortedList.add("B");
		unsortedList.add("A");
		
		Collections.sort(unsortedList, c);
		
		Assert.assertEquals("E", unsortedList.get(0));
		Assert.assertEquals("D", unsortedList.get(1));
		Assert.assertEquals("C", unsortedList.get(2));
		Assert.assertEquals("B", unsortedList.get(3));
		Assert.assertEquals("A", unsortedList.get(4));
	}

}
