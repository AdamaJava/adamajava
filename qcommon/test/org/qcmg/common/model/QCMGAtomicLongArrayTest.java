package org.qcmg.common.model;

import static org.junit.Assert.*;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongArray;

import org.junit.Ignore;
import org.junit.Test;

public class QCMGAtomicLongArrayTest {
	
	@Test
	public void testConstructor() {
		QCMGAtomicLongArray array = new QCMGAtomicLongArray(16);
		
		for (int i = 0 ; i < 17 ; i++) {
			array.increment(i);
		}
		for (int i = 0 ; i < 17 ; i++) {
			assertEquals(1, array.get(i));
		}
		
	}
	
	@Ignore
	public void speedTest() {
		int noOfLoops = 10000000;
		AtomicLongArray array = new AtomicLongArray(100);
		QCMGAtomicLongArray qcmgArray = new QCMGAtomicLongArray(50);
		
		Random r1 = new Random(1);
		Random r2 = new Random(1);
		
		
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			int arrayPosition = r2.nextInt(100);
			qcmgArray.increment(arrayPosition);
		}
//		System.out.println("QCMGAtomicLongArray: " + (System.currentTimeMillis() - start));
		
		start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			int arrayPosition = r2.nextInt(100);
			qcmgArray.increment(arrayPosition);
		}
		System.out.println("QCMGAtomicLongArray: " + (System.currentTimeMillis() - start));
		
		start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			int arrayPosition = r1.nextInt(100);
			array.incrementAndGet(arrayPosition);
		}
		System.out.println("AtomicLongArray: " + (System.currentTimeMillis() - start));
	}
	
	@Test
	public void testResize() throws InterruptedException {
		
		final int noOfLoops = 1000000;
		
		final QCMGAtomicLongArray array = new QCMGAtomicLongArray(10);
		// will create an array of length 20
		
		// create 2 threads, one that just increments values 0-20, and another that resizes
		ExecutorService executor = Executors.newFixedThreadPool(4);
		for (int j = 0 ; j < 3 ; j++) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					for (int i = 0 ; i < noOfLoops ; i++)
						array.increment(i % 20);
				}
			});
		}
		executor.execute(new Runnable() {
			@Override
			public void run() {
				int counter = 1;
				for (int i = 0 ; i < noOfLoops ; i++)
					if (i % 20000 == 0)
						array.increment(20 * counter++);
			}
		});
		
		executor.shutdown();
		executor.awaitTermination(1, TimeUnit.MINUTES);
		
		for (int i = 0 ; i < 20 ; i++) {
			assertEquals((noOfLoops / 20) *3, array.get(i));
		}
	}
	
	@Test
	public void isEmptyTest() {
		final QCMGAtomicLongArray array = new QCMGAtomicLongArray(10);
		assertTrue(array.isEmpty());
		
		array.increment(5);
		assertFalse(array.isEmpty());
		
	}
	
	@Test
	public void getSumTest() {
		final QCMGAtomicLongArray array = new QCMGAtomicLongArray(10);
		
		for(int i = 0; i < 5; i++ ) {
			array.increment(i * 2, 10);
		}		
		assertTrue(array.getSum() == 50);
		
		array.increment(9, -10 );
		assertTrue(array.getSum() == 40);
		
	}

}
