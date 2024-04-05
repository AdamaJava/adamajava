package org.qcmg.common.model;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ChrPositionTest {
	
	@Test
	public void testConstructor() {
		try {
			new ChrRangePosition(null,-1,-1);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException ignored) {}
		try {
			new ChrRangePosition("",-1,-1);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException ignored) {}
		try {
			new ChrRangePosition("testing",1,0);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException ignored) {}
		
		ChrRangePosition cp1 = new ChrRangePosition("123", -1,-1);
		ChrRangePosition cp2 = new ChrRangePosition("123", -1,-1);
		assertEquals(cp1, cp2);
	}
	
	@Test
	public void testComparator() {
		List<ChrRangePosition> positions = new ArrayList<>();
		ChrRangePosition cp1 = new ChrRangePosition("chr22", 1,1);
		ChrRangePosition cp2 = new ChrRangePosition("chr22", 2,2);
		positions.add(cp1);
		positions.add(cp2);
		
		Collections.sort(positions);
		assertEquals(cp1, positions.get(0));
		assertEquals(cp2, positions.get(1));
		
		ChrRangePosition cp3 = new ChrRangePosition("22", 1,1);
		positions.add(cp3);
		
		Collections.sort(positions);
		assertEquals(cp3, positions.get(0));
		assertEquals(cp1, positions.get(1));
		assertEquals(cp2, positions.get(2));
		
		// empty collection asn re-populate
		positions.clear();
		cp1 = new ChrRangePosition("chr22", 1,3);
		cp2 = new ChrRangePosition("chr22", 1,2);
		cp3 = new ChrRangePosition("chr22", 1,1);
		positions.add(cp1);
		positions.add(cp2);
		positions.add(cp3);
		Collections.sort(positions);
		assertEquals(cp3, positions.get(0));
		assertEquals(cp2, positions.get(1));
		assertEquals(cp1, positions.get(2));
		
		ChrRangePosition cp4 = new ChrRangePosition("chr22", 10,10);
		positions.add(cp4);
		Collections.sort(positions);
		assertEquals(cp4, positions.get(3));
	}
	
	@Test
	public void doesGetLengthWork() {
		ChrRangePosition cp1 = new ChrRangePosition("chr22", 1,1);
		ChrRangePosition cp2 = new ChrRangePosition("chr22", 2,3);
		ChrRangePosition cp3 = new ChrRangePosition("chr22", 2,4);
		assertEquals(1, cp1.getLength());
		assertEquals(2, cp2.getLength());
		assertEquals(3, cp3.getLength());
	}

}
