package org.qcmg.common.model;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ChrPositionRefAltTest {
	
	@Test
	public void testConstructor() {
		try {
			new ChrPositionRefAlt(null,-1,-1, "","");
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException ignored) {}
		try {
			new ChrPositionRefAlt("",-1,-1, "","");
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException ignored) {}
		try {
			new ChrPositionRefAlt("testing",1,0, "","");
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException ignored) {}
		
		ChrPositionRefAlt cp1 = new ChrPositionRefAlt("123", -1,-1, "","");
		ChrPositionRefAlt cp2 = new ChrPositionRefAlt("123", -1,-1, "","");
		assertEquals(cp1, cp2);
	}
	
	@Test
	public void getRef() {
		ChrPositionRefAlt cp1 = new ChrPositionRefAlt("1", 1,1, "ref","alt");
		assertEquals("ref", cp1.getRef());
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
		ChrPositionRefAlt cp2 = new ChrPositionRefAlt("chr22", 1,1,"","");
        assertEquals(cp1, cp2);
		ChrPositionRefAlt cp3 = new ChrPositionRefAlt("chr22", 1,1,"Hello","World");
		ChrPositionRefAlt cp4 = new ChrPositionRefAlt("chr22", 1,1,"Hello","There");
        assertNotEquals(cp3, cp4);
		ChrPositionRefAlt cp5 = new ChrPositionRefAlt("chr22", 1,1,"Hello","There");
		ChrPositionRefAlt cp6 = new ChrPositionRefAlt("chr22", 1,1,"Why Hello","There");
        assertNotEquals(cp5, cp6);
	}

}
