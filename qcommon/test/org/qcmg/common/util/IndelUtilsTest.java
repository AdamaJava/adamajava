package org.qcmg.common.util;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.vcf.VcfFormatFieldRecord;


public class IndelUtilsTest {
	
	
	@Test
	public void getVariantType() {
		
		assertEquals(SVTYPE.SNP, IndelUtils.getVariantType("A","A"));
		assertEquals(SVTYPE.DNP, IndelUtils.getVariantType("AB","AB"));
		assertEquals(SVTYPE.TNP, IndelUtils.getVariantType("ABC","ABC"));
		assertEquals(SVTYPE.ONP, IndelUtils.getVariantType("ABCD","ABCD"));
		assertEquals(SVTYPE.SNP, IndelUtils.getVariantType("A","A,X,S"));
		assertEquals(SVTYPE.DNP, IndelUtils.getVariantType("AB","AB,LL,BQ"));
		assertEquals(SVTYPE.TNP, IndelUtils.getVariantType("ABC","ABC,123,456,789"));
		assertEquals(SVTYPE.ONP, IndelUtils.getVariantType("ABCD","ABCD,qqqq"));
		
		// deletions
		assertEquals(SVTYPE.DEL, IndelUtils.getVariantType("ABCD","A"));
		assertEquals(SVTYPE.DEL, IndelUtils.getVariantType("ABC","A"));
		assertEquals(SVTYPE.DEL, IndelUtils.getVariantType("AB","A"));
		
		// insertions
		assertEquals(SVTYPE.INS, IndelUtils.getVariantType("A","ABC"));
		assertEquals(SVTYPE.INS, IndelUtils.getVariantType("A","AB"));
		assertEquals(SVTYPE.INS, IndelUtils.getVariantType("A","AB12345"));
		
		// unknown
		assertEquals(SVTYPE.UNKNOWN, IndelUtils.getVariantType("A","ABC,B"));
		assertEquals(SVTYPE.UNKNOWN, IndelUtils.getVariantType("ABC","ABC,B"));
		assertEquals(SVTYPE.DEL, IndelUtils.getVariantType("ABC","A,B"));
	}
	
	@Test
	public void isRefSameLengthAsAltsInValid() {
		try {
			assertEquals(false, IndelUtils.refSameLengthAsAlts(null, null));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals(false, IndelUtils.refSameLengthAsAlts("", null));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals(false, IndelUtils.refSameLengthAsAlts("", ""));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals(false, IndelUtils.refSameLengthAsAlts(null, ""));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
	}
	
	@Test
	public void isRefSameLengthAsAlts() {
		assertEquals(false, IndelUtils.refSameLengthAsAlts("X", "XY"));
		assertEquals(false, IndelUtils.refSameLengthAsAlts("XY", "X"));
		assertEquals(false, IndelUtils.refSameLengthAsAlts("XY", "XYZ"));
		assertEquals(false, IndelUtils.refSameLengthAsAlts("XYZ", "YZ"));
		assertEquals(true, IndelUtils.refSameLengthAsAlts("XYZ", "123"));
		assertEquals(true, IndelUtils.refSameLengthAsAlts("Z", "1"));
		assertEquals(true, IndelUtils.refSameLengthAsAlts("ZA", "19"));
		assertEquals(true, IndelUtils.refSameLengthAsAlts("Z", "1,2,3,4,5,6,7,8,9"));
		assertEquals(false, IndelUtils.refSameLengthAsAlts("Z", "1,2,3,4,5,6,7,8,9,10"));
		assertEquals(true, IndelUtils.refSameLengthAsAlts("ZX", "11,22,33,44,55,66,77,88,99"));
	}
	
	@Test
	public void getGDCompoundSnp() {
		VcfFormatFieldRecord ff = new VcfFormatFieldRecord("ACCS","CC,10,12,TT,29,14,T_,0,1&CC,10,12,TT,29,14,T_,0,1");
		assertEquals(null, IndelUtils.getGenotypeDetails(ff, "CC","TT"));
		ff = new VcfFormatFieldRecord("GT:ACCS","0/1:CC,10,12,TT,29,14,T_,0,1&CC,10,12,TT,29,14,T_,0,1");
		assertEquals("CC/TT", IndelUtils.getGenotypeDetails(ff, "CC","TT"));
	}
	@Test
	public void getGD() {
		assertEquals("A/A", IndelUtils.getGenotypeDetails( new VcfFormatFieldRecord("GT","0/0"), "A","C"));
		assertEquals("C/C", IndelUtils.getGenotypeDetails( new VcfFormatFieldRecord("GT","1/1"), "A","C"));
		assertEquals("A/C", IndelUtils.getGenotypeDetails( new VcfFormatFieldRecord("GT","0/1"), "A","C"));
		assertEquals("C/T", IndelUtils.getGenotypeDetails( new VcfFormatFieldRecord("GT","1/2"), "A","C,T"));
		assertEquals("G/G", IndelUtils.getGenotypeDetails( new VcfFormatFieldRecord("GT","1/1"), "A","G,T"));
	}
	
	@Test
	public void getMotif() {
		assertEquals("C", IndelUtils.getMotif("C", "A", SVTYPE.SNP));
		assertEquals("ABC", IndelUtils.getMotif("ABC", "DEF", SVTYPE.SNP));
		assertEquals("ABC", IndelUtils.getMotif("ABC", "DEF", SVTYPE.DNP));
		assertEquals("B", IndelUtils.getMotif("AB", "A", SVTYPE.DEL));
		assertEquals("C", IndelUtils.getMotif("BC", "B", SVTYPE.DEL));
		assertEquals("CDEF", IndelUtils.getMotif("B", "BCDEF", SVTYPE.INS));
	}
	
	@Test
	public void areAltsSameLength() {
		try {
			assertEquals(null, IndelUtils.getFirstAltIfSameLength(null));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals(null, IndelUtils.getFirstAltIfSameLength(""));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		
		assertEquals("A", IndelUtils.getFirstAltIfSameLength("A"));
		assertEquals("B", IndelUtils.getFirstAltIfSameLength("B"));
		assertEquals("c", IndelUtils.getFirstAltIfSameLength("c"));
		assertEquals("d", IndelUtils.getFirstAltIfSameLength("d"));
		assertEquals("d", IndelUtils.getFirstAltIfSameLength("d,e"));
		assertEquals("d", IndelUtils.getFirstAltIfSameLength("d,e,f"));
		assertEquals("d", IndelUtils.getFirstAltIfSameLength("d,e,f,g"));
		assertEquals(null, IndelUtils.getFirstAltIfSameLength("d,e,f,gh"));
		assertEquals(null, IndelUtils.getFirstAltIfSameLength("de,f,g,h"));
		assertEquals(null, IndelUtils.getFirstAltIfSameLength("d,e,fg,h"));
		assertEquals("d", IndelUtils.getFirstAltIfSameLength("d,e,f,g,h"));
		assertEquals(null, IndelUtils.getFirstAltIfSameLength("d,eee,fff,ggg,h"));
		assertEquals(null, IndelUtils.getFirstAltIfSameLength("dd,eee,fff,ggg,hhh"));
		assertEquals("ddd", IndelUtils.getFirstAltIfSameLength("ddd,eee,fff,ggg,hhh"));
	}
	
	
	@Test
	public void getVariantTypeDuffData() {
		try {
			assertEquals(SVTYPE.UNKNOWN, IndelUtils.getVariantType(null,null));
			fail("Should have NPE'd");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals(SVTYPE.UNKNOWN, IndelUtils.getVariantType("",""));
			fail("Should have NPE'd");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals(SVTYPE.UNKNOWN, IndelUtils.getVariantType("",null));
			fail("Should have NPE'd");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals(SVTYPE.UNKNOWN, IndelUtils.getVariantType(null,""));
			fail("Should have NPE'd");
		} catch (IllegalArgumentException iae) {}
	}
	
	@Test
	public void getRefForIndels() {
		assertEquals(null, IndelUtils.getRefForIndels(null, SVTYPE.CTX));
		assertEquals(null, IndelUtils.getRefForIndels(null, SVTYPE.SNP));
		assertEquals(null, IndelUtils.getRefForIndels(null, SVTYPE.DNP));
		assertEquals(null, IndelUtils.getRefForIndels(null, SVTYPE.ONP));
		assertEquals(null, IndelUtils.getRefForIndels(null, SVTYPE.TNP));
		assertEquals(null, IndelUtils.getRefForIndels(null, SVTYPE.UNKNOWN));
		assertEquals("", IndelUtils.getRefForIndels("", SVTYPE.CTX));
		assertEquals("", IndelUtils.getRefForIndels("", SVTYPE.SNP));
		assertEquals("", IndelUtils.getRefForIndels("", SVTYPE.DNP));
		assertEquals("", IndelUtils.getRefForIndels("", SVTYPE.ONP));
		assertEquals("", IndelUtils.getRefForIndels("", SVTYPE.TNP));
		assertEquals("", IndelUtils.getRefForIndels("", SVTYPE.UNKNOWN));
		assertEquals("A", IndelUtils.getRefForIndels("A", SVTYPE.CTX));
		assertEquals("A", IndelUtils.getRefForIndels("A", SVTYPE.SNP));
		assertEquals("A", IndelUtils.getRefForIndels("A", SVTYPE.DNP));
		assertEquals("A", IndelUtils.getRefForIndels("A", SVTYPE.ONP));
		assertEquals("A", IndelUtils.getRefForIndels("A", SVTYPE.TNP));
		assertEquals("A", IndelUtils.getRefForIndels("A", SVTYPE.UNKNOWN));
		assertEquals("ABCD", IndelUtils.getRefForIndels("ABCD", SVTYPE.CTX));
		assertEquals("ABCD", IndelUtils.getRefForIndels("ABCD", SVTYPE.SNP));
		assertEquals("ABCD", IndelUtils.getRefForIndels("ABCD", SVTYPE.DNP));
		assertEquals("ABCD", IndelUtils.getRefForIndels("ABCD", SVTYPE.ONP));
		assertEquals("ABCD", IndelUtils.getRefForIndels("ABCD", SVTYPE.TNP));
		assertEquals("ABCD", IndelUtils.getRefForIndels("ABCD", SVTYPE.UNKNOWN));
		
		/*
		 * INS
		 */
		assertEquals("-", IndelUtils.getRefForIndels(null, SVTYPE.INS));
		assertEquals("-", IndelUtils.getRefForIndels("", SVTYPE.INS));
		assertEquals("-", IndelUtils.getRefForIndels("A", SVTYPE.INS));
		assertEquals("-", IndelUtils.getRefForIndels("ABC", SVTYPE.INS));
		assertEquals("-", IndelUtils.getRefForIndels("15299XYZ", SVTYPE.INS));
		
		/*
		 * DEL
		 */
		assertEquals(null, IndelUtils.getRefForIndels(null, SVTYPE.DEL));
		assertEquals("", IndelUtils.getRefForIndels("", SVTYPE.DEL));
		assertEquals("", IndelUtils.getRefForIndels("A", SVTYPE.DEL));
		assertEquals("BC", IndelUtils.getRefForIndels("ABC", SVTYPE.DEL));
		assertEquals("5299XYZ", IndelUtils.getRefForIndels("15299XYZ", SVTYPE.DEL));
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
