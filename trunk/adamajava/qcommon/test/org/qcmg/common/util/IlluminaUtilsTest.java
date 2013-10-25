package org.qcmg.common.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IlluminaUtilsTest {
	
	@Test
	public void testConverter() {
		assertEquals("A/A", IlluminaUtils.convertIlluminaGenotype("AC", "AA", 'A'));
		assertEquals("A/A", IlluminaUtils.convertIlluminaGenotype("A/C", "AA", 'A'));
		
		assertEquals("A/C", IlluminaUtils.convertIlluminaGenotype("AC", "AB", 'A'));
		assertEquals("A/C", IlluminaUtils.convertIlluminaGenotype("A/C", "AB", 'A'));
		
		assertEquals("C/C", IlluminaUtils.convertIlluminaGenotype("AC", "BB", 'A'));
		assertEquals("C/C", IlluminaUtils.convertIlluminaGenotype("A/C", "BB", 'A'));
		
		assertEquals("C/C", IlluminaUtils.convertIlluminaGenotype("AG", "BB", 'T'));
	}
	
	@Test
	public void testRealLifeExamples() {
		assertEquals("G/G", IlluminaUtils.convertIlluminaGenotype("T/C", "BB", 'G'));
		assertEquals("G/G", IlluminaUtils.convertIlluminaGenotype("A/G", "BB", 'A'));
		assertEquals("G/G", IlluminaUtils.convertIlluminaGenotype("T/C", "BB", 'A'));
		assertEquals("C/C", IlluminaUtils.convertIlluminaGenotype("A/C", "BB", 'C'));
		assertEquals("C/C", IlluminaUtils.convertIlluminaGenotype("A/C", "BB", 'C'));
		assertEquals("T/T", IlluminaUtils.convertIlluminaGenotype("A/G", "AA", 'T'));
		assertEquals("A/G", IlluminaUtils.convertIlluminaGenotype("A/G", "AB", 'G'));
		assertEquals("A/A", IlluminaUtils.convertIlluminaGenotype("A/C", "AA", 'A'));
//		Assert.assertEquals("C/T", IlluminaGenotypeConverter.convertIlluminaGenotype("A/G", "AB", 'C'));
		assertEquals("T/C", IlluminaUtils.convertIlluminaGenotype("A/G", "AB", 'C'));
		
		assertEquals("A/G", IlluminaUtils.convertIlluminaGenotype("A/G", "AB", 'G'));
//		Assert.assertEquals("C/T", IlluminaGenotypeConverter.convertIlluminaGenotype("A/G", "AB", 'C'));
		assertEquals("T/T", IlluminaUtils.convertIlluminaGenotype("T/C", "AA", 'T'));
	}
	
	@Test
	public void testGetAllelicCounts() {
		assertEquals(9, IlluminaUtils.getAllelicCounts(20, -0.0751, 0.5)[0]);
		assertEquals(9, IlluminaUtils.getAllelicCounts(20, -0.0751, 0.5)[1]);
		
		assertEquals(0, IlluminaUtils.getAllelicCounts(20, 0.1781, 1.0)[0]);
		assertEquals(22, IlluminaUtils.getAllelicCounts(20, 0.1781, 1.0)[1]);
		
		assertEquals(15, IlluminaUtils.getAllelicCounts(20, -0.3898, 0.0)[0]);
		assertEquals(0, IlluminaUtils.getAllelicCounts(20, -0.3898, 0.0)[1]);
		
	}
	
	@Test
	public void testGetAllelicCountsIntensities() {
		
		//14621   9636    0.5147  -0.0304
		assertEquals(12, IlluminaUtils.getAllelicCounts(20, -0.0304, 14621, 9636)[0]);
		assertEquals(7, IlluminaUtils.getAllelicCounts(20, -0.0304, 14621, 9636)[1]);
		
		//1780	10559	0.9912	0.2395
		assertEquals(3, IlluminaUtils.getAllelicCounts(20, 0.2395, 1780, 10559)[0]);
		assertEquals(20, IlluminaUtils.getAllelicCounts(20, 0.2395, 1780, 10559)[1]);
		
		
	}

}
