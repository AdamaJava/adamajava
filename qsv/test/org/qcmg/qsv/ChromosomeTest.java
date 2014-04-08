package org.qcmg.qsv;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ChromosomeTest {
	
	@Test
	public void testCompareTo1() {
		Chromosome c1 = new Chromosome("chr1", 1234);
		Chromosome c2 = new Chromosome("chr2", 123);
		
		int result = c1.compareTo(c2);
		
		assertEquals(result, -1);
	}
	
	@Test
	public void testCompareTo2() {
		Chromosome c1 = new Chromosome("chr1", 1234);
		Chromosome c2 = new Chromosome("chr1", 123);
		
		int result = c1.compareTo(c2);
		
		assertEquals(result, 0);
	}
	
	@Test
	public void testCompareTo3() {
		Chromosome c1 = new Chromosome("chrX", 1234);
		Chromosome c2 = new Chromosome("chrY", 123);
		
		int result = c1.compareTo(c2);
		
		assertEquals(result, -1);
	}

}
