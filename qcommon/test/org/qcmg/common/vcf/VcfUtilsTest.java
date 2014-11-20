package org.qcmg.common.vcf;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.model.VCFRecord;

public class VcfUtilsTest {
	
	@Test
	public void testGetADFromGenotypeField() {
		String genotype = "";
		assertEquals(0, VcfUtils.getADFromGenotypeField(genotype));
		
		genotype = "0/1:173,141:282:99:255,0,255";
		assertEquals(314 , VcfUtils.getADFromGenotypeField(genotype));
	}
	
	@Test
	public void testGetDPFromGenotypeField() {
		String genotype = "";
		assertEquals(0 , VcfUtils.getDPFromFormatField(genotype));
		
		genotype = "0/1:173,141:282:99:255,0,255";
		assertEquals(282 , VcfUtils.getDPFromFormatField(genotype));
	}
	
	@Test
	public void testCalculateGTField() {
		assertEquals(null, VcfUtils.calculateGTField(null));
		assertEquals("1/1", VcfUtils.calculateGTField(GenotypeEnum.AA));
		assertEquals("0/1", VcfUtils.calculateGTField(GenotypeEnum.AC));
	}
	
	@Test
	public void testCalculateGenotypeEnum() {
		
		assertEquals(null, VcfUtils.calculateGenotypeEnum(null, '\u0000', '\u0000'));
		assertEquals(null, VcfUtils.calculateGenotypeEnum("", '\u0000', '\u0000'));
		assertEquals(null, VcfUtils.calculateGenotypeEnum("", 'X', 'Y'));
		assertEquals(null, VcfUtils.calculateGenotypeEnum("0/1", 'X', 'Y'));
		
		assertEquals(GenotypeEnum.AA, VcfUtils.calculateGenotypeEnum("0/0", 'A', 'C'));
		assertEquals(GenotypeEnum.CC, VcfUtils.calculateGenotypeEnum("1/1", 'A', 'C'));
		assertEquals(GenotypeEnum.AC, VcfUtils.calculateGenotypeEnum("0/1", 'A', 'C'));
		
		assertEquals(GenotypeEnum.GG, VcfUtils.calculateGenotypeEnum("0/0", 'G', 'G'));
		assertEquals(GenotypeEnum.GG, VcfUtils.calculateGenotypeEnum("0/1", 'G', 'G'));
		assertEquals(GenotypeEnum.GG, VcfUtils.calculateGenotypeEnum("1/1", 'G', 'G'));
		
	}
	
	@Test
	public void testGetPileupElementAsString() {
		assertEquals("FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0", VcfUtils.getPileupElementAsString(null, false));
		assertEquals("NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0", VcfUtils.getPileupElementAsString(null, true));
		List<PileupElement> pileups = new ArrayList<PileupElement>();
		PileupElement pA = new PileupElement('A');
		pA.incrementForwardCount();
		PileupElement pC = new PileupElement('C');
		pC.incrementForwardCount();
		pileups.add(pA);
		assertEquals("NOVELCOV=A:1,C:0,G:0,T:0,N:0,TOTAL:1", VcfUtils.getPileupElementAsString(pileups, true));
		pileups.add(pC);
		assertEquals("NOVELCOV=A:1,C:1,G:0,T:0,N:0,TOTAL:2", VcfUtils.getPileupElementAsString(pileups, true));
		assertEquals("FULLCOV=A:1,C:1,G:0,T:0,N:0,TOTAL:2", VcfUtils.getPileupElementAsString(pileups, false));
	}
	
	@Test
	public void testGetMutationAndGTs() {
		assertArrayEquals(new String[] {".", ".","."}, VcfUtils.getMutationAndGTs(null,  null, null));
		assertArrayEquals(new String[] {"C", "0/0","0/1"} , VcfUtils.getMutationAndGTs("G",  GenotypeEnum.GG, GenotypeEnum.CG));
		assertArrayEquals(new String[] {"C", ".","0/1"} , VcfUtils.getMutationAndGTs("G",  null, GenotypeEnum.CG));
		assertArrayEquals(new String[] {"T", "0/1","."} , VcfUtils.getMutationAndGTs("G",  GenotypeEnum.GT, null));
		assertArrayEquals(new String[] {"T", "1/1","."} , VcfUtils.getMutationAndGTs("G",  GenotypeEnum.TT, null));
		assertArrayEquals(new String[] {"A,T", "2/2","1/2"} , VcfUtils.getMutationAndGTs("G",  GenotypeEnum.TT, GenotypeEnum.AT));
		assertArrayEquals(new String[] {"A,T", "2/2","1/2"} , VcfUtils.getMutationAndGTs("G",  GenotypeEnum.TT, GenotypeEnum.AT));
	}
	
	@Test
	public void testGetGTString() {
		assertEquals(".", VcfUtils.getGTString(null, '\u0000', null));
		assertEquals(".", VcfUtils.getGTString("", '\u0000', null));
		assertEquals(".", VcfUtils.getGTString("A", 'C', null));
		assertEquals("0/0", VcfUtils.getGTString("A", 'C', GenotypeEnum.CC));
		assertEquals("0/0", VcfUtils.getGTString("AG", 'C', GenotypeEnum.CC));
		assertEquals("0/0", VcfUtils.getGTString("AG", 'T', GenotypeEnum.TT));
		assertEquals("0/1", VcfUtils.getGTString("AG", 'C', GenotypeEnum.AC));
		assertEquals("1/2", VcfUtils.getGTString("AG", 'C', GenotypeEnum.AG));
		assertEquals("0/2", VcfUtils.getGTString("AG", 'C', GenotypeEnum.CG));
		assertEquals("0/2", VcfUtils.getGTString("AGT", 'C', GenotypeEnum.CG));
		assertEquals("1/3", VcfUtils.getGTString("AGT", 'C', GenotypeEnum.AT));
		assertEquals("0/1", VcfUtils.getGTString("AGT", 'C', GenotypeEnum.AC));
		assertEquals("1/2", VcfUtils.getGTString("AGT", 'C', GenotypeEnum.AG));
		assertEquals("1/1", VcfUtils.getGTString("AGT", 'C', GenotypeEnum.AA));
		assertEquals("0/0", VcfUtils.getGTString("AGT", 'C', GenotypeEnum.CC));
		assertEquals("0/2", VcfUtils.getGTString("AGT", 'C', GenotypeEnum.CG));
		assertEquals("0/3", VcfUtils.getGTString("AGT", 'C', GenotypeEnum.CT));
		assertEquals("2/2", VcfUtils.getGTString("AGT", 'C', GenotypeEnum.GG));
		assertEquals("2/3", VcfUtils.getGTString("AGT", 'C', GenotypeEnum.GT));
		assertEquals("3/3", VcfUtils.getGTString("AGT", 'C', GenotypeEnum.TT));
	}
	
	
	@Test
	public void testGetStringFromCharSet() {
		assertEquals("", VcfUtils.getStringFromCharSet(null));
		Set<Character> set = new TreeSet<Character>();
		
		assertEquals("", VcfUtils.getStringFromCharSet(set));
		set.add('T');
		assertEquals("T", VcfUtils.getStringFromCharSet(set));
		set.add('G');
		assertEquals("GT", VcfUtils.getStringFromCharSet(set));
		set.add('C');
		assertEquals("CGT", VcfUtils.getStringFromCharSet(set));
		set.add('A');
		assertEquals("ACGT", VcfUtils.getStringFromCharSet(set));
		set.add('A');
		set.add('C');
		set.add('G');
		set.add('T');
		assertEquals("ACGT", VcfUtils.getStringFromCharSet(set));
		set.add('X');
		set.add('Y');
		set.add('Z');
		assertEquals("ACGTXYZ", VcfUtils.getStringFromCharSet(set));
	}
	
	@Test
	public void isRecordAMnp() {
		
		VCFRecord rec = VcfUtils.createVcfRecord("1", 1, "A");
		assertEquals(false, VcfUtils.isRecordAMnp(rec));
		
		rec.setAlt("A");
		assertEquals(false, VcfUtils.isRecordAMnp(rec));
		
		 rec = VcfUtils.createVcfRecord("1", 1, "AC");
		rec.setAlt("A");
		assertEquals(false, VcfUtils.isRecordAMnp(rec));
		
		rec.setAlt("ACG");
		assertEquals(false, VcfUtils.isRecordAMnp(rec));
		
		rec = VcfUtils.createVcfRecord("1", 1, "G");
		rec.setAlt("G");
		assertEquals(false, VcfUtils.isRecordAMnp(rec));		// ref == alt
		rec = VcfUtils.createVcfRecord("1", 1, "CG");
		rec.setAlt("GA");
		assertEquals(true, VcfUtils.isRecordAMnp(rec));
		rec = VcfUtils.createVcfRecord("1", 1, "CGTTT");
		rec.setAlt("GANNN");
		assertEquals(true, VcfUtils.isRecordAMnp(rec));
	}
	@Test
	public void isRecordAMnpCheckIndels() {
		
		VCFRecord rec = VcfUtils.createVcfRecord("1", 1, "ACCACCACC");
		assertEquals(false, VcfUtils.isRecordAMnp(rec));
		
		rec.setAlt("A,AACCACC");
		assertEquals(false, VcfUtils.isRecordAMnp(rec));
		
	}
	
	@Test
	public void addFormatFields() {
		VCFRecord rec = VcfUtils.createVcfRecord("1", 1, "ACCACCACC");
		VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("GT:AD:DP:GQ:PL", "0/1:6,3:9:62:62,0,150"));
		
		List<String> newStuff = new ArrayList<>();
		newStuff.add("GT");
		newStuff.add("blah");
		
		VcfUtils.addFormatFieldsToVcf(rec, newStuff);
		
		assertEquals("GT:AD:DP:GQ:PL", rec.getFormatFields().get(0));
		assertEquals("0/1:6,3:9:62:62,0,150", rec.getFormatFields().get(1));
		
		newStuff = new ArrayList<>();
		newStuff.add("QT");
		newStuff.add("blah");
		
		VcfUtils.addFormatFieldsToVcf(rec, newStuff);
		
		assertEquals("GT:AD:DP:GQ:PL:QT", rec.getFormatFields().get(0));
		assertEquals("0/1:6,3:9:62:62,0,150:blah", rec.getFormatFields().get(1));
		
		// and again
		rec = VcfUtils.createVcfRecord("1", 1, "ACCACCACC");
		VcfUtils.addFormatFieldsToVcf(rec, Arrays.asList("GT:AD:DP:GQ:PL", "0/1:6,3:9:62:62,0,150"));
		
		newStuff = new ArrayList<>();
		newStuff.add("GT:GD:AC");
		newStuff.add("0/1:A/C:A10[12.5],2[33],C20[1],30[2]");
		
		VcfUtils.addFormatFieldsToVcf(rec, newStuff);
		
		assertEquals("GT:AD:DP:GQ:PL:GD:AC", rec.getFormatFields().get(0));
		assertEquals("0/1:6,3:9:62:62,0,150:A/C:A10[12.5],2[33],C20[1],30[2]", rec.getFormatFields().get(1));
		
	}
}
