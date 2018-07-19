package org.qcmg.common.model;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ChrPositionRefAltTest {
	
	@Test
	public void testConstructor() {
		try {
			new ChrPositionRefAlt((String)null,-1,-1, "","");
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		try {
			new ChrPositionRefAlt("",-1,-1, "","");
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		try {
			new ChrPositionRefAlt("testing",1,0, "","");
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		
		ChrPositionRefAlt cp1 = new ChrPositionRefAlt("123", -1,-1, "","");
		ChrPositionRefAlt cp2 = new ChrPositionRefAlt("123", -1,-1, "","");
		assertEquals(cp1, cp2);
	}
	
	@Test
	public void getRef() {
		ChrPositionRefAlt cp1 = new ChrPositionRefAlt("1", 1,1, "ref","alt");
		assertEquals("ref", cp1.getName());
		assertEquals("alt", cp1.getAlt());
	}
	
	@Test
	public void testComparator() {
		List<ChrPositionRefAlt> positions = new ArrayList<>();
		ChrPositionRefAlt cp1 = new ChrPositionRefAlt("chr22", 1,1, "","");
		ChrPositionRefAlt cp2 = new ChrPositionRefAlt("chr22", 2,2, "","");
		positions.add(cp1);
		positions.add(cp2);
		positions.sort(null);
		assertEquals(cp1, positions.get(0));
		assertEquals(cp2, positions.get(1));
		
		ChrPositionRefAlt cp3 = new ChrPositionRefAlt("22", 1,1, "","");
		positions.add(cp3);
		
		positions.sort(null);
		assertEquals(cp3, positions.get(0));
		assertEquals(cp1, positions.get(1));
		assertEquals(cp2, positions.get(2));
		
		// empty collection asn re-populate
		positions.clear();
		cp1 = new ChrPositionRefAlt("chr22", 1,3, "","");
		cp2 = new ChrPositionRefAlt("chr22", 1,2, "","");
		cp3 = new ChrPositionRefAlt("chr22", 1,1, "","");
		positions.add(cp1);
		positions.add(cp2);
		positions.add(cp3);
		positions.sort(null);
		assertEquals(cp3, positions.get(0));
		assertEquals(cp2, positions.get(1));
		assertEquals(cp1, positions.get(2));
		
		ChrPositionRefAlt cp4 = new ChrPositionRefAlt("chr22", 10,10, "","");
		positions.add(cp4);
		positions.sort(null);
		assertEquals(cp4, positions.get(3));
	}
	
	@Test
	public void testComparatorWithRefAndAlt() {
		List<ChrPositionRefAlt> positions = new ArrayList<>();
		ChrPositionRefAlt cp1 = new ChrPositionRefAlt("chr22", 2,2, "C","G");
		ChrPositionRefAlt cp2 = new ChrPositionRefAlt("chr22", 2,2, "C","T");
		positions.add(cp1);
		positions.add(cp2);
		positions.sort(null);
		assertEquals(cp1, positions.get(0));
		assertEquals(cp2, positions.get(1));
		/*
		 * this will appear at beginning as A is before C
		 */
		ChrPositionRefAlt cp3 = new ChrPositionRefAlt("chr22", 2,2, "A","C");
		positions.add(cp3);
		positions.sort(null);
		assertEquals(cp1, positions.get(1));
		assertEquals(cp2, positions.get(2));
		assertEquals(cp3, positions.get(0));
		/*
		 * this will appear at end as T is after A & C
		 */
		ChrPositionRefAlt cp4 = new ChrPositionRefAlt("chr22", 2,2, "T","C");
		positions.add(cp4);
		positions.sort(null);
		assertEquals(cp1, positions.get(1));
		assertEquals(cp2, positions.get(2));
		assertEquals(cp3, positions.get(0));
		assertEquals(cp4, positions.get(3));
		/*
		 * this will squeeze in at second last place as T is after A & C, and A is before C
		 */
		ChrPositionRefAlt cp5 = new ChrPositionRefAlt("chr22", 2,2, "T","A");
		positions.add(cp5);
		positions.sort(null);
		assertEquals(cp1, positions.get(1));
		assertEquals(cp2, positions.get(2));
		assertEquals(cp3, positions.get(0));
		assertEquals(cp4, positions.get(4));
		assertEquals(cp5, positions.get(3));
	}
	
	@Test
	public void doesEqualsWork() {
		ChrPositionRefAlt cp1 = new ChrPositionRefAlt("chr22", 1,1,"","");
		assertEquals(true, cp1.equals(cp1));
		ChrPositionRefAlt cp2 = new ChrPositionRefAlt("chr22", 1,1,"","");
		assertEquals(true, cp2.equals(cp2));
		assertEquals(true, cp1.equals(cp2));
		ChrPositionRefAlt cp3 = new ChrPositionRefAlt("chr22", 1,1,"Hello","World");
		assertEquals(true, cp3.equals(cp3));
		ChrPositionRefAlt cp4 = new ChrPositionRefAlt("chr22", 1,1,"Hello","There");
		assertEquals(true, cp4.equals(cp4));
		assertEquals(false, cp3.equals(cp4));
		ChrPositionRefAlt cp5 = new ChrPositionRefAlt("chr22", 1,1,"Hello","There");
		assertEquals(true, cp5.equals(cp5));
		ChrPositionRefAlt cp6 = new ChrPositionRefAlt("chr22", 1,1,"Why Hello","There");
		assertEquals(false, cp5.equals(cp6));
	}
	
	@Ignore
	public void testHashCode() {
		Set<Integer> distinctHashCodes = new HashSet<Integer>();
		int counter = 0;
		for (int cycle = 90000000 ;  cycle < 100000000 ; cycle++) {
			int chrValue = (counter % 20) + 1;
			ChrPositionRefAlt cp = new ChrPositionRefAlt("chr" + chrValue, cycle, cycle, "","");
			distinctHashCodes.add(cp.hashCode());
			counter++;
		}
	}

}
