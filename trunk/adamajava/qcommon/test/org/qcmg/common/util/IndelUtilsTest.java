package org.qcmg.common.util;

import static org.junit.Assert.*;

import org.junit.Test;
import org.qcmg.common.util.IndelUtils.SVTYPE;


public class IndelUtilsTest {
	
	
	@Test
	public void getVariantType() {
		
		assertEquals(SVTYPE.SNP, IndelUtils.getVariantType("A","A"));
		assertEquals(SVTYPE.DNP, IndelUtils.getVariantType("AB","AB"));
		assertEquals(SVTYPE.TNP, IndelUtils.getVariantType("ABC","ABC"));
		assertEquals(SVTYPE.ONP, IndelUtils.getVariantType("ABCD","ABCD"));
		
		// deletions
		assertEquals(SVTYPE.DEL, IndelUtils.getVariantType("ABCD","A"));
		
		// insertions
		assertEquals(SVTYPE.INS, IndelUtils.getVariantType("A","ABC"));
		
		// unknown
		assertEquals(SVTYPE.UNKOWN, IndelUtils.getVariantType("A","ABC,B"));
	}
	
	
	@Test
	public void getVariantTypeDuffData() {
		try {
			assertEquals(SVTYPE.UNKOWN, IndelUtils.getVariantType(null,null));
			fail("Should have NPE'd");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals(SVTYPE.UNKOWN, IndelUtils.getVariantType("",""));
			fail("Should have NPE'd");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals(SVTYPE.UNKOWN, IndelUtils.getVariantType("",null));
			fail("Should have NPE'd");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals(SVTYPE.UNKOWN, IndelUtils.getVariantType(null,""));
			fail("Should have NPE'd");
		} catch (IllegalArgumentException iae) {}
	}
	
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
