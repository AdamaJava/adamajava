package org.qcmg.common.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class IndelUtilsTest {
	
	@Test
	public void testGetFullChromosome() {
		
		assertEquals("chr10", IndelUtils.getFullChromosome("10"));
		assertEquals("chr10", IndelUtils.getFullChromosome("chr10"));
		assertEquals("chrMT", IndelUtils.getFullChromosome("M"));
		assertEquals("chrMT", IndelUtils.getFullChromosome("MT"));
		assertEquals("GL12345", IndelUtils.getFullChromosome("GL12345"));
	}
	
	@Test
	public void testAddChromosomeReference() {
		
		assertEquals("chr10", IndelUtils.getFullChromosome("10"));
		assertEquals("23", IndelUtils.getFullChromosome("23"));
		assertEquals("chr22", IndelUtils.getFullChromosome("22"));
		assertEquals("99", IndelUtils.getFullChromosome("99"));
		assertEquals("100", IndelUtils.getFullChromosome("100"));
		assertEquals("chr1", IndelUtils.getFullChromosome("1"));
		assertEquals("chrY", IndelUtils.getFullChromosome("Y"));
		assertEquals("chrX", IndelUtils.getFullChromosome("X"));
		assertEquals("chrMT", IndelUtils.getFullChromosome("M"));
		assertEquals("chrMT", IndelUtils.getFullChromosome("MT"));
		assertEquals("chrMT", IndelUtils.getFullChromosome("chrMT"));
		assertEquals("chrMT", IndelUtils.getFullChromosome("chrM"));
		assertEquals("MTT", IndelUtils.getFullChromosome("MTT"));
		assertEquals("GL123", IndelUtils.getFullChromosome("GL123"));
		assertEquals("chr10", IndelUtils.getFullChromosome("chr10"));
	}

}
