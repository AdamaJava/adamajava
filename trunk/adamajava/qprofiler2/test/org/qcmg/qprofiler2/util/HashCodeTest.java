package org.qcmg.qprofiler2.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Ignore;

public class HashCodeTest {
	
	@Ignore
	public void testHashCodeSpeed() {
		long noOfLoops = 100000000;
		Random r = new Random();
		Map<Integer, AtomicLong> map = new HashMap<Integer, AtomicLong>();
		
		long start = System.currentTimeMillis();
		
		for (int i = 0 ; i < noOfLoops ; i++) {
			
			Integer integer = r.nextInt(41);
			int hashCode = integer.hashCode();
			
		}
		System.out.println("Integer hashcode: " + (System.currentTimeMillis() - start));
		
		start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			r.nextInt(41);
		}
		System.out.println("new  hashcode: " + (System.currentTimeMillis() - start));
	}
	
	private int newHashCode(int value) {
		return value;
	}

}
