package org.qcmg.common.util;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Ignore;

public class ConcurrentCollectionSpeedTest {
	
//	@Test
	@Ignore
	public void testCollectionSpeed() {
		Random r = new Random();
		Map<Integer, AtomicLong> testMap = new ConcurrentSkipListMap<Integer, AtomicLong>();
		long start = System.currentTimeMillis();
		
		for (int i = 0 ; i < 2000000 ; i++) {
			updateExisting(testMap, r.nextInt(10000));
		}
		System.out.println("Existing method time taken: " + (System.currentTimeMillis() - start));
		
		start = System.currentTimeMillis();
		for (int i = 0 ; i < 2000000 ; i++) {
			updateNewMethod(testMap, r.nextInt(10000));
		}
		System.out.println("New method time taken: " + (System.currentTimeMillis() - start));
		
		start = System.currentTimeMillis();
		for (int i = 0 ; i < 2000000 ; i++) {
			updateUsingCollectionMethod(testMap, r.nextInt(10000));
		}
		System.out.println("Collection method time taken: " + (System.currentTimeMillis() - start));
		
		start = System.currentTimeMillis();
		for (int i = 0 ; i < 2000000 ; i++) {
			updateExisting(testMap, r.nextInt(10000));
		}
		System.out.println("Existing method time taken: " + (System.currentTimeMillis() - start));
		
		start = System.currentTimeMillis();
		for (int i = 0 ; i < 2000000 ; i++) {
			updateNewMethod(testMap, r.nextInt(10000));
		}
		System.out.println("New method time taken: " + (System.currentTimeMillis() - start));
		
		start = System.currentTimeMillis();
		for (int i = 0 ; i < 2000000 ; i++) {
			updateUsingCollectionMethod(testMap, r.nextInt(10000));
		}
		System.out.println("Collection method time taken: " + (System.currentTimeMillis() - start));
		
	}
	
	private <T> void updateExisting(Map<T, AtomicLong> map, T data) {
		synchronized (map) {
			AtomicLong currentCount = map.get(data);
			if (null == currentCount) {
				map.put(data, new AtomicLong(1));
			} else {
				currentCount.incrementAndGet();
			}
		}
	}
	
	private <T> void updateNewMethod(Map<T, AtomicLong> map, T data) {
		AtomicLong currentCount = map.get(data);
		if (null == currentCount) {
			AtomicLong existingAL= map.put(data, new AtomicLong(1));
			
			if (null != existingAL) {
				map.get(data).addAndGet(existingAL.get());
			}
		} else {
			currentCount.incrementAndGet();
		}
	}
	
	@SuppressWarnings("unchecked")
	private <T> void updateUsingCollectionMethod(Map<T, AtomicLong> map, T data) {
		AtomicLong currentCount = (AtomicLong) ((ConcurrentSkipListMap)map).putIfAbsent(data, new AtomicLong(0));
		if (null != currentCount) {
			map.get(data).addAndGet(currentCount.get());
		} 
	}

}
