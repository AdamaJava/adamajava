package org.qcmg.qmule;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Ignore;

public class TestThreading {
	
	private static final int testRuns = 50000000;

	@Ignore
	public void testLongUpdate() {
		
		long counter = 0L;
		
		long start = System.currentTimeMillis();
		
		for (int i = 0 ; i < testRuns ; i++) counter++;
		
		long end = System.currentTimeMillis();
		System.out.println("counter: " + counter);
		System.out.println("Time taken: " + (end - start) + "ms");
		
	}
	
	@Ignore
	public void testLongUpdateSynchronised() {
		
		long counter = 0L;
		
		long start = System.currentTimeMillis();
		
		for (int i = 0 ; i < testRuns ; i++) synchronized(this){counter++;}
		
		long end = System.currentTimeMillis();
		System.out.println("counter: " + counter);
		System.out.println("Time taken (synchronised): " + (end - start) + "ms");
		
	}
	
	@Ignore
	public void testAtomicLongUpdate() {
		
		AtomicLong counter = new AtomicLong();
		
		long start = System.currentTimeMillis();
		
		for (int i = 0 ; i < testRuns ; i++) counter.getAndIncrement();
		
		long end = System.currentTimeMillis();
		System.out.println("counter: " + counter.longValue());
		System.out.println("Time taken (Atomic): " + (end - start) + "ms");
		
	}
}
