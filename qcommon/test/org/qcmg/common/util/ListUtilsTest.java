package org.qcmg.common.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;

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
	public void positionInArray() {
		assertEquals(0, ListUtils.positionOfStringInArray(new String[]{"hello"}, "hello"));
		assertEquals(-1, ListUtils.positionOfStringInArray(new String[]{"hello"}, "there"));
		assertEquals(1, ListUtils.positionOfStringInArray(new String[]{"hello","there"}, "there"));
		assertEquals(4, ListUtils.positionOfStringInArray(new String[]{"A","A","C","C","G","G","T","T"}, "G"));
		assertEquals(6, ListUtils.positionOfStringInArray(new String[]{"A","A","C","C","G","G","T","T"}, "T"));
		assertEquals(2, ListUtils.positionOfStringInArray(new String[]{"A","A","C","C","G","G","T","T"}, "C"));
	}
	
	
	@Test
	public void removeAdjacent() {
		TLongList list = new TLongArrayList();
		list.add(1);
		list.add(2);
		list.add(3);
		TLongList newList = ListUtils.removeAdjacentPositionsInList(list);
		assertEquals(1, newList.size());
		assertEquals(1, newList.get(0));
		
		list.add(100);
		list.add(200);
		list.add(300);
		newList = ListUtils.removeAdjacentPositionsInList(list);
		assertEquals(4, newList.size());
		long [] newListArray = newList.toArray();
		Arrays.sort(newListArray);
		assertArrayEquals(new long[] {1,100,200,300}, newListArray);
		
		list.add(1000);
		list.add(1002);
		list.add(1005);
		newList = ListUtils.removeAdjacentPositionsInList(list);
		assertEquals(6, newList.size());
		newListArray = newList.toArray();
		Arrays.sort(newListArray);
		assertArrayEquals(new long[] {1,100,200,300, 1000, 1005}, newListArray);
	}
	
	@Test
	public void removeAdjacent2() {
		TLongList list = new TLongArrayList(new long[] {100886306, 163961463, 433534056, 269911082, 379579119, 100886305, 163961462, 269911081, 379579118, 269911080, 379579117});
		TLongList newList = ListUtils.removeAdjacentPositionsInList(list);
		assertEquals(5, newList.size());
		assertEquals(true, newList.contains(269911080));
	}
	
	@Test
	public void removeAdjacent3() {
		TLongList list = new TLongArrayList(new long[] {26607815, 26607796, 26607788, 26607785, 26607782, 26607779, 26607776, 26607855, 26607821, 26607818});
		TLongList newList = ListUtils.removeAdjacentPositionsInList(list, 100);
		assertEquals(1, newList.size());
		assertEquals(true, newList.contains(26607776));
		
		newList = ListUtils.removeAdjacentPositionsInList(list, 40);
		assertEquals(2, newList.size());
		assertEquals(true, newList.contains(26607776));
		
		newList = ListUtils.removeAdjacentPositionsInList(list);
		assertEquals(10, newList.size());
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
		
		assertEquals("A", unsortedList.get(0));
		assertEquals("B", unsortedList.get(1));
		assertEquals("C", unsortedList.get(2));
		assertEquals("D", unsortedList.get(3));
		assertEquals("E", unsortedList.get(4));
		
		unsortedList.add("I");
		unsortedList.add("J");
		unsortedList.add("K");
		unsortedList.add("L");
		
		Collections.sort(unsortedList, ijk);
		
		assertEquals("I", unsortedList.get(0));
		assertEquals("J", unsortedList.get(1));
		assertEquals("K", unsortedList.get(2));
		assertEquals("A", unsortedList.get(3));
		assertEquals("B", unsortedList.get(4));
		assertEquals("C", unsortedList.get(5));
		assertEquals("D", unsortedList.get(6));
		assertEquals("E", unsortedList.get(7));
		assertEquals("L", unsortedList.get(8));
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
