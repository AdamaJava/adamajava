package org.qcmg.common.model;

import static org.junit.Assert.assertEquals;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayDeque;
import java.util.Queue;

import org.junit.Ignore;
import org.junit.Test;

public class PileupElementLiteTest {
	
	@Test
	public void testNovelStartCounter() {
		PileupElementLite pel = new PileupElementLite();
		
		// add some forward quals
		pel.addForwardQuality((byte) 0, 1, 1, false);
		assertEquals(1, pel.getNovelStartCount());
		assertEquals(1, pel.getForwardCount());
		assertEquals(1, pel.getTotalCount());
		
		pel = new PileupElementLite();
		pel.addForwardQuality((byte) 0, 1, 1, false);
		pel.addForwardQuality((byte) 0, 1, 1, false);
		
		assertEquals(1, pel.getNovelStartCount());
		assertEquals(2, pel.getForwardCount());
		assertEquals(2, pel.getTotalCount());
		
		pel = new PileupElementLite();
		pel.addForwardQuality((byte) 0, 1, 1, false);
		pel.addForwardQuality((byte) 0, 1, 1, false);
		pel.addForwardQuality((byte) 0, 2, 1, false);
		
		assertEquals(2, pel.getNovelStartCount());
		assertEquals(3, pel.getForwardCount());
		assertEquals(3, pel.getTotalCount());
		
		pel = new PileupElementLite();
		pel.addForwardQuality((byte) 0, 1, 1, false);
		pel.addForwardQuality((byte) 0, 1, 1, false);
		pel.addForwardQuality((byte) 0, 2, 1, false);
		pel.addForwardQuality((byte) 0, 2, 1, false);
		
		assertEquals(2, pel.getNovelStartCount());
		assertEquals(4, pel.getForwardCount());
		assertEquals(4, pel.getTotalCount());
		
		pel = new PileupElementLite();
		pel.addForwardQuality((byte) 0, 1, 1, false);
		pel.addForwardQuality((byte) 0, 1, 1, false);
		pel.addForwardQuality((byte) 0, 2, 1, false);
		pel.addForwardQuality((byte) 0, 2, 1, false);
		pel.addForwardQuality((byte) 0, 3, 1,false);
		
		assertEquals(3, pel.getNovelStartCount());
		assertEquals(5, pel.getForwardCount());
		assertEquals(5, pel.getTotalCount());
		
		// add some reverse quals
		pel = new PileupElementLite();
		pel.addForwardQuality((byte) 0, 1, 1, false);
		pel.addForwardQuality((byte) 0, 1, 1, false);
		pel.addForwardQuality((byte) 0, 2, 1, false);
		pel.addForwardQuality((byte) 0, 2, 1, false);
		pel.addForwardQuality((byte) 0, 3, 1,false);
		pel.addReverseQuality((byte) 0, 5, 1,false);
		
		assertEquals(4, pel.getNovelStartCount());
		assertEquals(1, pel.getReverseCount());
		assertEquals(6, pel.getTotalCount());
		
		pel = new PileupElementLite();
		pel.addForwardQuality((byte) 0, 1, 1, false);
		pel.addForwardQuality((byte) 0, 1, 1, false);
		pel.addForwardQuality((byte) 0, 2, 1, false);
		pel.addForwardQuality((byte) 0, 2, 1, false);
		pel.addForwardQuality((byte) 0, 3, 1,false);
		pel.addReverseQuality((byte) 0, 5,1, false);
		pel.addReverseQuality((byte) 0, 5, 1,false);
		
		assertEquals(4, pel.getNovelStartCount());
		assertEquals(2, pel.getReverseCount());
		assertEquals(7, pel.getTotalCount());
		
		pel = new PileupElementLite();
		pel.addForwardQuality((byte) 0, 1, 1, false);
		pel.addForwardQuality((byte) 0, 1, 1, false);
		pel.addForwardQuality((byte) 0, 2, 1, false);
		pel.addForwardQuality((byte) 0, 2, 1, false);
		pel.addForwardQuality((byte) 0, 3,1, false);
		pel.addReverseQuality((byte) 0, 5,1, false);
		pel.addReverseQuality((byte) 0, 5, 1,false);
		pel.addReverseQuality((byte) 0, 6, 1,false);
		
		assertEquals(5, pel.getNovelStartCount());
		assertEquals(3, pel.getReverseCount());
		assertEquals(8, pel.getTotalCount());
		
		pel = new PileupElementLite();
		pel.addForwardQuality((byte) 0, 1, 1, false);
		pel.addForwardQuality((byte) 0, 1, 1, false);
		pel.addForwardQuality((byte) 0, 2, 1, false);
		pel.addForwardQuality((byte) 0, 2, 1, false);
		pel.addForwardQuality((byte) 0, 3,1, false);
		pel.addReverseQuality((byte) 0, 5,1, false);
		pel.addReverseQuality((byte) 0, 5,1, false);
		pel.addReverseQuality((byte) 0, 6, 1,false);
		pel.addReverseQuality((byte) 0, 6, 1,false);
		
		assertEquals(5, pel.getNovelStartCount());
		assertEquals(4, pel.getReverseCount());
		assertEquals(9, pel.getTotalCount());
		
		pel = new PileupElementLite();
		pel.addForwardQuality((byte) 0, 1, 1, false);
		pel.addForwardQuality((byte) 0, 1, 1, false);
		pel.addForwardQuality((byte) 0, 2, 1, false);
		pel.addForwardQuality((byte) 0, 2, 1, false);
		pel.addForwardQuality((byte) 0, 3, 1,false);
		pel.addReverseQuality((byte) 0, 5, 1,false);
		pel.addReverseQuality((byte) 0, 5, 1,false);
		pel.addReverseQuality((byte) 0, 6,1, false);
		pel.addReverseQuality((byte) 0, 6,1, false);
		pel.addReverseQuality((byte) 0, 7,1, false);
		
		assertEquals(6, pel.getNovelStartCount());
		assertEquals(5, pel.getReverseCount());
		assertEquals(10, pel.getTotalCount());
	}
	
	@Test
	public void testNSCounterBothStrands() {
		PileupElementLite pel = new PileupElementLite();
		// add some forward quals
		pel.addForwardQuality((byte) 0, 1, 1, false);
		assertEquals(1, pel.getNovelStartCount());
		pel.addReverseQuality((byte) 0, 1, 1, false);
		assertEquals(2, pel.getNovelStartCount());
		pel.addForwardQuality((byte) 0, 1, 1, false);
		assertEquals(2, pel.getNovelStartCount());
		pel.addReverseQuality((byte) 0, 1, 1, false);
		assertEquals(2, pel.getNovelStartCount());
		
		pel.addReverseQuality((byte) 0, 99,1, false);
		assertEquals(3, pel.getNovelStartCount());
		pel.addForwardQuality((byte) 0, 100, 1,false);
		assertEquals(4, pel.getNovelStartCount());
		pel.addReverseQuality((byte) 0, 100,1, false);
		assertEquals(5, pel.getNovelStartCount());
		
		// doesn't increment count as we are adding a value less than current forward start position of 100
		//NOTE that this is no longer true, we no longer keep track of the current forward start position, instead electing to keep all start positions
		pel.addForwardQuality((byte) 0, 99,1, false);
		assertEquals(6, pel.getNovelStartCount());
	}
	
	@Ignore
	public void speedTest() {
		// add some ints and Integers into a queue and a trove array to see what wins...
		
		int no = 10000000;
		
		Queue<Integer> queue = new ArrayDeque<>();
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < no ; i++) {
			queue.add(Integer.valueOf(i));
		}
		System.out.println("time taken: " + (System.currentTimeMillis() - start));
		
		// and now the trove list
		TIntArrayList trove = new TIntArrayList();
		start = System.currentTimeMillis();
		for (int i = 0 ; i < no ; i++) {
			trove.add(i);
		}
		System.out.println("time taken trove: " + (System.currentTimeMillis() - start));
		
		queue = new ArrayDeque<>();
		
		start = System.currentTimeMillis();
		for (int i = 0 ; i < no ; i++) {
			queue.add(Integer.valueOf(i));
		}
		System.out.println("time taken: " + (System.currentTimeMillis() - start));
		
	}

}
