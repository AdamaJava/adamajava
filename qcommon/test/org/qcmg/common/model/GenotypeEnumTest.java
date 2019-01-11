package org.qcmg.common.model;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

public class GenotypeEnumTest {

	@Test
	public void testGetGenotypeEnum() {
		
		assertEquals(GenotypeEnum.AA, GenotypeEnum.getGenotypeEnum('A', 'A'));
		assertEquals(GenotypeEnum.AC, GenotypeEnum.getGenotypeEnum('A', 'C'));
		assertEquals(GenotypeEnum.AG, GenotypeEnum.getGenotypeEnum('A', 'G'));
		assertEquals(GenotypeEnum.AT, GenotypeEnum.getGenotypeEnum('A', 'T'));
		
		assertEquals(GenotypeEnum.AC, GenotypeEnum.getGenotypeEnum('C', 'A'));
		assertEquals(GenotypeEnum.CC, GenotypeEnum.getGenotypeEnum('C', 'C'));
		assertEquals(GenotypeEnum.CG, GenotypeEnum.getGenotypeEnum('C', 'G'));
		assertEquals(GenotypeEnum.CT, GenotypeEnum.getGenotypeEnum('C', 'T'));
		
		assertEquals(GenotypeEnum.AG, GenotypeEnum.getGenotypeEnum('G', 'A'));
		assertEquals(GenotypeEnum.CG, GenotypeEnum.getGenotypeEnum('G', 'C'));
		assertEquals(GenotypeEnum.GG, GenotypeEnum.getGenotypeEnum('G', 'G'));
		assertEquals(GenotypeEnum.GT, GenotypeEnum.getGenotypeEnum('G', 'T'));
		
		assertEquals(GenotypeEnum.AT, GenotypeEnum.getGenotypeEnum('T', 'A'));
		assertEquals(GenotypeEnum.CT, GenotypeEnum.getGenotypeEnum('T', 'C'));
		assertEquals(GenotypeEnum.GT, GenotypeEnum.getGenotypeEnum('T', 'G'));
		assertEquals(GenotypeEnum.TT, GenotypeEnum.getGenotypeEnum('T', 'T'));
		
	}
	
	@Test
	public void getAltAlleleString() {
		for (GenotypeEnum ge : GenotypeEnum.values()) {
			assertEquals(ge.getFirstAllele() + "," + ge.getSecondAllele(), ge.getAltAlleleString());
		}
	}
	
	@Test
	public void getQualifyingAltAlleles() {
		assertEquals(null, GenotypeEnum.AA.getQualifyingAltAlleles('A'));
		assertEquals(null, GenotypeEnum.CC.getQualifyingAltAlleles('C'));
		assertEquals(null, GenotypeEnum.GG.getQualifyingAltAlleles('G'));
		assertEquals(null, GenotypeEnum.TT.getQualifyingAltAlleles('T'));
		
		assertEquals("A", GenotypeEnum.AA.getQualifyingAltAlleles('C'));
		assertEquals("C", GenotypeEnum.CC.getQualifyingAltAlleles('A'));
		assertEquals("G", GenotypeEnum.GG.getQualifyingAltAlleles('T'));
		assertEquals("T", GenotypeEnum.TT.getQualifyingAltAlleles('G'));
		
		assertEquals("A,G", GenotypeEnum.AG.getQualifyingAltAlleles('C'));
		assertEquals("C,T", GenotypeEnum.CT.getQualifyingAltAlleles('A'));
		assertEquals("G,T", GenotypeEnum.GT.getQualifyingAltAlleles('C'));
		assertEquals("C,G", GenotypeEnum.CG.getQualifyingAltAlleles('T'));
		
		assertEquals("C", GenotypeEnum.AC.getQualifyingAltAlleles('A'));
		assertEquals("G", GenotypeEnum.AG.getQualifyingAltAlleles('A'));
		assertEquals("T", GenotypeEnum.AT.getQualifyingAltAlleles('A'));
		
		assertEquals("A", GenotypeEnum.AC.getQualifyingAltAlleles('C'));
		assertEquals("A,G", GenotypeEnum.AG.getQualifyingAltAlleles('C'));
		
		assertEquals("T", GenotypeEnum.AT.getQualifyingAltAlleles('A'));
		assertEquals("A,T", GenotypeEnum.AT.getQualifyingAltAlleles('C'));
		assertEquals("A,T", GenotypeEnum.AT.getQualifyingAltAlleles('G'));
		assertEquals("A", GenotypeEnum.AT.getQualifyingAltAlleles('T'));
	}
	
	@Test
	public void testIsHomozygous() {
		GenotypeEnum ge = GenotypeEnum.getGenotypeEnum('A', 'A');
		assertEquals(GenotypeEnum.AA, ge);
		Assert.assertTrue(ge.isHomozygous());
		Assert.assertFalse(ge.isHeterozygous());
		
		ge = GenotypeEnum.getGenotypeEnum('A', 'G');
		assertEquals(GenotypeEnum.AG, ge);
		Assert.assertFalse(ge.isHomozygous());
		Assert.assertTrue(ge.isHeterozygous());
	}
	
	@Test
	public void testGetDisplayString() {
		GenotypeEnum ge = GenotypeEnum.getGenotypeEnum('G', 'C');
		assertEquals(GenotypeEnum.CG, ge);
		Assert.assertFalse(ge.isHomozygous());
		Assert.assertTrue(ge.isHeterozygous());
		assertEquals("C/G", ge.getDisplayString());
	}
	
	@Test
	public void testGetComplement() {
		assertEquals(GenotypeEnum.TT, GenotypeEnum.AA.getComplement());
		assertEquals(GenotypeEnum.AA, GenotypeEnum.TT.getComplement());
		assertEquals(GenotypeEnum.AT, GenotypeEnum.AT.getComplement());
		assertEquals(GenotypeEnum.GG, GenotypeEnum.CC.getComplement());
		assertEquals(GenotypeEnum.CC, GenotypeEnum.GG.getComplement());
		assertEquals(GenotypeEnum.CG, GenotypeEnum.CG.getComplement());
		assertEquals(GenotypeEnum.AC, GenotypeEnum.GT.getComplement());
		assertEquals(GenotypeEnum.GT, GenotypeEnum.AC.getComplement());
		assertEquals(GenotypeEnum.AG, GenotypeEnum.CT.getComplement());
		assertEquals(GenotypeEnum.CT, GenotypeEnum.AG.getComplement());
	}
	
	@Test
	public void testContainsAllele() {
		assertEquals(true, GenotypeEnum.AA.containsAllele('A'));
		assertEquals(false, GenotypeEnum.AA.containsAllele('B'));
		assertEquals(true, GenotypeEnum.AC.containsAllele('C'));
		assertEquals(false, GenotypeEnum.AC.containsAllele('a'));
		assertEquals(false, GenotypeEnum.AC.containsAllele('c'));
		assertEquals(false, GenotypeEnum.AC.containsAllele('G'));
	}
	
	@Test
	public void testAllelePosition() {
		assertEquals('A', GenotypeEnum.AA.getFirstAllele());
		assertEquals('A', GenotypeEnum.AA.getSecondAllele());
		assertEquals('C', GenotypeEnum.AC.getSecondAllele());
		assertEquals('C', GenotypeEnum.CG.getFirstAllele());
		assertEquals('G', GenotypeEnum.CG.getSecondAllele());
		assertEquals('G', GenotypeEnum.GT.getFirstAllele());
		assertEquals('T', GenotypeEnum.GT.getSecondAllele());
		assertEquals('T', GenotypeEnum.TT.getFirstAllele());
		assertEquals('T', GenotypeEnum.TT.getSecondAllele());
	}
	
}
