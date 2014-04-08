package org.qcmg.maf;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.util.SnpUtils;

public class MafFilterTest {

	@Test
	public void testPassesCountCheck() {
		assertEquals(false, MafFilter.passesCountCheck(null, -1, null));
		assertEquals(false, MafFilter.passesCountCheck("A:0[0],0[0]", -1, null));
		assertEquals(true, MafFilter.passesCountCheck("A:0[0],0[0]", 0, "A"));
		assertEquals(false, MafFilter.passesCountCheck("A:0[0],0[0]", 0, "T"));
		assertEquals(false, MafFilter.passesCountCheck("A:0[0],0[0]", 1, "A"));
		assertEquals(true, MafFilter.passesCountCheck("A:1[0],0[0]", 1, "A"));
		assertEquals(true, MafFilter.passesCountCheck("A:0[0],1[0]", 1, "A"));
		assertEquals(false, MafFilter.passesCountCheck("A:0[0],0[0],C:2[0],1[0]", 1, "A"));
		assertEquals(true, MafFilter.passesCountCheck("A:0[0],0[0],C:2[0],1[0]", 1, "C"));
		assertEquals(false, MafFilter.passesCountCheck("A:0[0],0[0],C:2[0],1[0]", 1, "G"));
	}
	
	@Test
	public void testPassesHighConfidenceFilter() {
		try {
			MafFilter.passesHighConfidenceFilter(null, null,null,null,null);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		try {
			MafFilter.passesHighConfidenceFilter("", null,null,null,null);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		try {
			MafFilter.passesHighConfidenceFilter(SnpUtils.PASS, null,null,null,null);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		
		assertEquals(false, MafFilter.passesHighConfidenceFilter(SnpUtils.PASS, "DEL",null,null,null));
		assertEquals(false, MafFilter.passesHighConfidenceFilter(SnpUtils.PASS, "DEL",null,"rs123456",null));
		assertEquals(true, MafFilter.passesHighConfidenceFilter(SnpUtils.PASS, "DEL",null,"novel",null));
		
		assertEquals(false, MafFilter.passesHighConfidenceFilter(SnpUtils.PASS, "INS",null,null,null));
		assertEquals(false, MafFilter.passesHighConfidenceFilter(SnpUtils.PASS, "INS",null,"rs123456",null));
		assertEquals(true, MafFilter.passesHighConfidenceFilter(SnpUtils.PASS, "INS",null,"novel",null));
		
		assertEquals(false, MafFilter.passesHighConfidenceFilter(SnpUtils.PASS, "SNP",null,null,null));
		assertEquals(false, MafFilter.passesHighConfidenceFilter(SnpUtils.PASS, "SNP",null,"rs123456",null));
		assertEquals(false, MafFilter.passesHighConfidenceFilter(SnpUtils.PASS, "SNP",null,"novel",null));
		assertEquals(true, MafFilter.passesHighConfidenceFilter(SnpUtils.PASS, "SNP","A:0[0],0[0],C:20[0],1[0]",null,"C"));
		assertEquals(false, MafFilter.passesHighConfidenceFilter("Hello", "SNP","A:0[0],0[0],C:20[0],1[0]",null,"C"));
		assertEquals(true, MafFilter.passesHighConfidenceFilter(SnpUtils.PASS, "SNP","A:0[0],0[0],C:3[0],2[0]",null,"C"));
		assertEquals(false, MafFilter.passesHighConfidenceFilter(SnpUtils.LESS_THAN_12_READS_NORMAL, "SNP","A:0[0],0[0],C:20[0],1[0]",null,"C"));
		assertEquals(true, MafFilter.passesHighConfidenceFilter(SnpUtils.PASS, "SNP","A:0[0],0[0],C:20[0],1[0]","novel","C"));
	}
	
	@Test
	public void testPassesProbableNoiseFilter() {
		try {
			MafFilter.passesProbableNoiseFilter(null, null,null,null,null);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		try {
			MafFilter.passesProbableNoiseFilter("", null,null,null,null);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		try {
			MafFilter.passesProbableNoiseFilter(SnpUtils.PASS, null,null,null,null);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		
		assertEquals(true, MafFilter.passesProbableNoiseFilter(SnpUtils.PASS, "DEL",null,null,null));
		assertEquals(true, MafFilter.passesProbableNoiseFilter(SnpUtils.LESS_THAN_12_READS_NORMAL, "DEL",null,null,null));
		assertEquals(true, MafFilter.passesProbableNoiseFilter(SnpUtils.LESS_THAN_3_READS_NORMAL, "DEL",null,null,null));
		assertEquals(false, MafFilter.passesProbableNoiseFilter(SnpUtils.LESS_THAN_3_READS_TUMOUR, "DEL",null,null,null));
		assertEquals(false, MafFilter.passesProbableNoiseFilter(SnpUtils.LESS_THAN_8_READS_TUMOUR, "DEL",null,null,null));
		assertEquals(true, MafFilter.passesProbableNoiseFilter(SnpUtils.MUTATION_IN_UNFILTERED_NORMAL, "DEL",null,null,null));
		assertEquals(false, MafFilter.passesProbableNoiseFilter("Hello", "DEL",null,null,null));
		
		assertEquals(true, MafFilter.passesProbableNoiseFilter(SnpUtils.PASS, "INS",null,null,null));
		assertEquals(true, MafFilter.passesProbableNoiseFilter(SnpUtils.PASS, "INS",null,"rs123456",null));
		assertEquals(true, MafFilter.passesProbableNoiseFilter(SnpUtils.LESS_THAN_3_READS_NORMAL, "INS",null,"rs123456",null));
		assertEquals(false, MafFilter.passesProbableNoiseFilter("blah", "INS",null,"rs123456",null));
		
		assertEquals(false, MafFilter.passesProbableNoiseFilter(SnpUtils.PASS, "SNP",null,null,null));
		assertEquals(false, MafFilter.passesProbableNoiseFilter(SnpUtils.LESS_THAN_3_READS_NORMAL, "SNP",null,null,null));
		assertEquals(false, MafFilter.passesProbableNoiseFilter(SnpUtils.PASS, "SNP","A:1[0],2[0],C:3[0],2[0]",null,"A"));
		assertEquals(true, MafFilter.passesProbableNoiseFilter(SnpUtils.LESS_THAN_3_READS_NORMAL, "SNP","A:0[0],0[0],C:3[0],2[0]",null,"C"));
	}
}
