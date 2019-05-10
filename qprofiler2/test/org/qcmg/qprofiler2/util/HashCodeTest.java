package org.qcmg.qprofiler2.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.qprofiler2.summarise.PairSummary;
import org.testng.Assert;

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
	
	@Test
	public void pairSummaryHashCodeTest(){
		
		PairSummary previous = new PairSummary(null, false);
		for(PairSummary.Pair p : PairSummary.Pair.values()) {
			for(boolean isTrue : new boolean[] {true, false}) {
				PairSummary summary = new PairSummary(p, isTrue);	 				
				assertTrue(previous.hashCode() != summary.hashCode());
				assertFalse(previous.equals(summary));
				Assert.assertNotEquals(previous, summary);
				previous = summary;
			}
			
		}		
	}

}
