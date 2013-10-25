package org.qcmg.common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class PositionRangeTest {
	
	@Test
	public void testConstructor() {
		try {
			new PositionRange(-1,-1);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		try {
			new PositionRange(0,-1);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		try {
			new PositionRange(1,0);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException e) {}
		
		Assert.assertNotNull(new PositionRange(0,0));
		
		PositionRange pr1 = new PositionRange(1, 1);
		PositionRange pr2 = new PositionRange(1, 1);
		Assert.assertEquals(pr1, pr2);
		
		pr1 = new PositionRange(1, 2);
		pr2 = new PositionRange(2, 3);
		Assert.assertEquals( false, pr1.equals(pr2));
	}
	
	@Test
	public void testComparator() {
		// its just the start position that is taken into account by the comaparator
		List<PositionRange> positions = new ArrayList<PositionRange>();
		PositionRange pr1 = new PositionRange(10, 10);
		PositionRange pr2 = new PositionRange(11, 11);
		positions.add(pr1);
		positions.add(pr2);
		
		Collections.sort(positions);
		Assert.assertEquals(pr1, positions.get(0));
		Assert.assertEquals(pr2, positions.get(1));
		
		PositionRange pr3 = new PositionRange(1, 10);
		positions.add(pr3);
		
		Collections.sort(positions);
		Assert.assertEquals(pr3, positions.get(0));
		Assert.assertEquals(pr1, positions.get(1));
		Assert.assertEquals(pr2, positions.get(2));
		
		// empty collection and re-populate
		positions.clear();
		pr1 = new PositionRange(3,3);
		pr2 = new PositionRange(2,2);
		pr3 = new PositionRange(1,1);
		positions.add(pr1);
		positions.add(pr2);
		positions.add(pr3);
		Collections.sort(positions);
		Assert.assertEquals(pr3, positions.get(0));
		Assert.assertEquals(pr2, positions.get(1));
		Assert.assertEquals(pr1, positions.get(2));
		
		PositionRange pr4 = new PositionRange(10,10);
		positions.add(pr4);
		Collections.sort(positions);
		Assert.assertEquals(pr4, positions.get(3));
	}
	
	@Test
	public void testIsAdjacentToEnd() {
		PositionRange pr1 = new PositionRange(10, 10);
		Assert.assertEquals(true, pr1.isAdjacentToEnd(11));
		Assert.assertEquals(false, pr1.isAdjacentToEnd(0));
		Assert.assertEquals(false, pr1.isAdjacentToEnd(-1));
		Assert.assertEquals(false, pr1.isAdjacentToEnd(9));
		Assert.assertEquals(false, pr1.isAdjacentToEnd(12));
		Assert.assertEquals(false, pr1.isAdjacentToEnd(Integer.MIN_VALUE));
		Assert.assertEquals(false, pr1.isAdjacentToEnd(Integer.MAX_VALUE));
	}
	
	@Test
	public void testExtendRange() {
		PositionRange pr1 = new PositionRange(10, 10);
		try {
			pr1.extendRange(9);
			Assert.fail("Should have thrown an IllegalAgumentException");
		} catch (IllegalArgumentException e) {}
		
		pr1.extendRange(11);
		Assert.assertEquals(11, pr1.getEnd());
		pr1.extendRange(1100);
		Assert.assertEquals(1100, pr1.getEnd());
	}
	
	@Test
	public void testContainsPosition() {
		PositionRange pr1 = new PositionRange(1, 10);
		Assert.assertEquals(false, pr1.containsPosition(-1));
		Assert.assertEquals(false, pr1.containsPosition(0));
		Assert.assertEquals(true, pr1.containsPosition(1));
		Assert.assertEquals(true, pr1.containsPosition(5));
		Assert.assertEquals(true, pr1.containsPosition(10));
		Assert.assertEquals(false, pr1.containsPosition(11));
		Assert.assertEquals(false, pr1.containsPosition(Integer.MAX_VALUE));
		Assert.assertEquals(false, pr1.containsPosition(Integer.MIN_VALUE));
	}
}
