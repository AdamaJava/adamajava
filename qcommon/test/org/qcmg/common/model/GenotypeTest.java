package org.qcmg.common.model;

import org.junit.Assert;
import org.junit.Test;

public class GenotypeTest {
	
	@Test
	public void testIsHeterozygous() {
		Assert.assertFalse(new Genotype('A','A').isHeterozygous());
		Assert.assertFalse(new Genotype('C','C').isHeterozygous());
		Assert.assertFalse(new Genotype('G','G').isHeterozygous());
		Assert.assertFalse(new Genotype('T','T').isHeterozygous());
		Assert.assertFalse(new Genotype('x','x').isHeterozygous());
		
		Assert.assertTrue(new Genotype('T','A').isHeterozygous());
		Assert.assertTrue(new Genotype('A','T').isHeterozygous());
		Assert.assertTrue(new Genotype('G','C').isHeterozygous());
		Assert.assertTrue(new Genotype('C','G').isHeterozygous());
		Assert.assertTrue(new Genotype('A','G').isHeterozygous());
		Assert.assertTrue(new Genotype('C','A').isHeterozygous());
		
	}
	
	@Test
	public void testOrdering() {
		Assert.assertEquals("A/C", new Genotype('A','C').getFormattedGenotype());
		Assert.assertEquals("A/C", new Genotype('C','A').getFormattedGenotype());
		Assert.assertEquals("T/T", new Genotype('T','T').getFormattedGenotype());
		Assert.assertEquals("G/T", new Genotype('T','G').getFormattedGenotype());
		Assert.assertEquals("C/T", new Genotype('C','T').getFormattedGenotype());
	}
	
	@Test
	public void testEquals() {
		Assert.assertEquals(new Genotype('A','C'), new Genotype('A','C'));
		Assert.assertEquals(new Genotype('C','A'), new Genotype('A','C'));
		Assert.assertNotSame(new Genotype('T','C'), new Genotype('A','T'));
		Assert.assertEquals(new Genotype('T','T'), new Genotype('T','T'));
		Assert.assertEquals(new Genotype('G','A'), new Genotype('A','G'));
		Assert.assertNotSame(new Genotype('A','C'), new Genotype('A','A'));
	}

}
