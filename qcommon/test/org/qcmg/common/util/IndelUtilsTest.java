package org.qcmg.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
		
		assertTrue(IndelUtils.addChromosomeReference("10"));		
		assertFalse(IndelUtils.addChromosomeReference("23"));		
		assertFalse(IndelUtils.addChromosomeReference("99"));		
		assertFalse(IndelUtils.addChromosomeReference("100"));		
		assertTrue(IndelUtils.addChromosomeReference("1"));
		assertTrue(IndelUtils.addChromosomeReference("Y"));
		assertTrue(IndelUtils.addChromosomeReference("X"));
		assertTrue(IndelUtils.addChromosomeReference("M"));
		assertTrue(IndelUtils.addChromosomeReference("MT"));
		assertFalse(IndelUtils.addChromosomeReference("MTT"));
		assertFalse(IndelUtils.addChromosomeReference("GL123"));
		assertFalse(IndelUtils.addChromosomeReference("chr10"));
	}

}
