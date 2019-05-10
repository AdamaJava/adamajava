package org.qcmg.qprofiler.util;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.qvisualise.util.SummaryByCycleNew2;

public class SummaryByCycleTestNew2<T> {
	
	@Test
	public void testSummaryByCycle() {
		SummaryByCycleNew2<Integer> s = new SummaryByCycleNew2<Integer>(1, 50, 50);
		SummaryByCycleNew2<Character> s2 = new SummaryByCycleNew2<Character>('a', 50, 5);
	
		for (int cycle = 1 ;  cycle < 51 ; cycle++) {
			for (int key = 0 ; key < 41 ; key++) {
				
				int arrayPosition = s.getArrayPosition(cycle, key);
				int [] cycleKey = s.getCycleKeyFromArrayPosition(arrayPosition);
				
				Assert.assertEquals(cycle, cycleKey[0]);
				Assert.assertEquals(key, cycleKey[1]);
			}
		}
		
		for (int cycle = 1 ;  cycle < 51 ; cycle++) {
			for (int key = 65 ; key < 92 ; key++) {
				
				int arrayPosition = s2.getArrayPosition(cycle, key);
				int [] cycleKey = s2.getCycleKeyFromArrayPosition(arrayPosition);
				
				Assert.assertEquals(cycle, cycleKey[0]);
				Assert.assertEquals(key, cycleKey[1]);
			}
		}
	}
	
	@Test
	public void testResizeCycle() {
		SummaryByCycleNew2<Integer> s = new SummaryByCycleNew2<Integer>(1, 10, 10);
		// should be set to 31 * 31 for capacity
		
		// will try and increment for 33
		s.increment(31, 10);	// this should be OK
		Assert.assertEquals(1, s.count(31, 10).get());
		s.increment(32, 10);	// this will trigger the resize
		Assert.assertEquals(1, s.count(31, 10).get());	// old value should still be there
		Assert.assertEquals(1, s.count(32, 10).get());	// along with new value
		
		// and again
		s.increment(129, 10);	// this will trigger the resize
		Assert.assertEquals(1, s.count(31, 10).get());	// old value should still be there
		Assert.assertEquals(1, s.count(32, 10).get());	// along with new value
		Assert.assertEquals(1, s.count(129, 10).get());	// along with new value
	}
	
	@Test
	public void testResizeKey() {
		SummaryByCycleNew2<Integer> s = new SummaryByCycleNew2<Integer>(1, 10, 10);
		// should be set to 31 * 31 for capacity
		
		// will try and increment for key 35
		s.increment(31, 31);	// this should be OK
		Assert.assertEquals(1, s.count(31, 31).get());
		s.increment(31, 35);	// this will trigger the resize
		Assert.assertEquals(1, s.count(31, 31).get());	// old value should still be there
		Assert.assertEquals(1, s.count(31, 35).get());	// along with new value
		
		// and again
		s.increment(31, 129);	// this will trigger the resize
		Assert.assertEquals(1, s.count(31, 31).get());	// old value should still be there
		Assert.assertEquals(1, s.count(31, 35).get());	// along with new value
		Assert.assertEquals(1, s.count(31, 129).get());	// along with new value
	}
	
	@Test
	public void testResizeBoth() {
		SummaryByCycleNew2<Integer> s = new SummaryByCycleNew2<Integer>(1, 10, 10);
		// should be set to 31 * 31 for capacity
		
		// will try and increment for key 35
		s.increment(31, 31);	// this should be OK
		Assert.assertEquals(1, s.count(31, 31).get());
		s.increment(32,32);	// this will trigger the resize
		Assert.assertEquals(1, s.count(31, 31).get());	// old value should still be there
		Assert.assertEquals(1, s.count(32, 32).get());	// along with new value
		
		// and again
		s.increment(129, 129);	// this will trigger the resize
		Assert.assertEquals(1, s.count(31, 31).get());	// old value should still be there
		Assert.assertEquals(1, s.count(32, 32).get());	// along with new value
		Assert.assertEquals(1, s.count(129, 129).get());	// along with new value
		
		// last but not least
		s.increment(260, 280);	// this will trigger the resize
		Assert.assertEquals(1, s.count(31, 31).get());	// old value should still be there
		Assert.assertEquals(1, s.count(32, 32).get());	// along with new value
		Assert.assertEquals(1, s.count(129, 129).get());	// along with new value
		Assert.assertEquals(1, s.count(260, 280).get());	// along with new value
	}
	
	@Test
	public void testResizeOutOfBounds() {
		SummaryByCycleNew2<Integer> s = new SummaryByCycleNew2<Integer>(1, 10, 10);
		// should be set to 31 * 31 for capacity
		try {
			s.increment(2049, 2049);	// this should be OK
			Assert.fail("Should have throwsn an Illegal argument Exception");
		} catch (IllegalArgumentException e) {
		}
	}
}
