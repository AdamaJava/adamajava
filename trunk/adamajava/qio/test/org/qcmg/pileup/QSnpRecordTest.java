package org.qcmg.pileup;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.VcfUtils;

public class QSnpRecordTest {
	
	@Test
	public void testAddAnnotation() {
		QSnpRecord rec = new QSnpRecord("chr1", 12345, "ACGT", "TGCA");
		rec.getVcfRecord().addFilter(SnpUtils.MUTATION_IN_NORMAL);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL, rec.getAnnotation());
		
		// try adding again
		rec.getVcfRecord().addFilter(SnpUtils.MUTATION_IN_NORMAL);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL, rec.getAnnotation());
		
		// and now something else
		rec.getVcfRecord().addFilter(SnpUtils.LESS_THAN_8_READS_TUMOUR);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL + ";" + SnpUtils.LESS_THAN_8_READS_TUMOUR, rec.getAnnotation());
	}
	
//	@Test
//	public void getNucleotides() {
//		QSnpRecord rec = new QSnpRecord("chr1", 12345, "ACGT", "TGCA");
//		rec.setNormalOABS(null);
//		assertEquals(null, rec.getNormalNucleotides());
//		rec.setNormalOABS("A1[10]0[0]");
//		assertEquals("A1[10],0[0]", rec.getNormalNucleotides());
//		rec.setNormalOABS("A1[10]0[0];B0[0]35[99]");
//		assertEquals("A1[10],0[0],B0[0],35[99]", rec.getNormalNucleotides());
//		rec.setNormalOABS("A1[10]0[0];B0[0]35[99];C28[82]93[39]");
//		assertEquals("A1[10],0[0],B0[0],35[99],C28[82],93[39]", rec.getNormalNucleotides());
//	}
	
	@Test
	public void testRemoveAnnotation() {
		QSnpRecord rec = new QSnpRecord("chr1", 12345, "ACGT", "TGCA");
		rec.getVcfRecord().addFilter(SnpUtils.MUTATION_IN_NORMAL);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL, rec.getAnnotation());
		VcfUtils.removeFilter(rec.getVcfRecord(), SnpUtils.LESS_THAN_12_READS_NORMAL);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL, rec.getAnnotation());
		VcfUtils.removeFilter(rec.getVcfRecord(), SnpUtils.MUTATION_IN_NORMAL);
		assertEquals(null, rec.getAnnotation());
		
		rec.getVcfRecord().addFilter(SnpUtils.MUTATION_IN_NORMAL);
		rec.getVcfRecord().addFilter(SnpUtils.LESS_THAN_12_READS_NORMAL);
		VcfUtils.removeFilter(rec.getVcfRecord(), SnpUtils.LESS_THAN_12_READS_NORMAL);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL, rec.getAnnotation());
		
		rec.getVcfRecord().addFilter(SnpUtils.LESS_THAN_12_READS_NORMAL);
		VcfUtils.removeFilter(rec.getVcfRecord(), SnpUtils.MUTATION_IN_NORMAL);
		assertEquals(SnpUtils.LESS_THAN_12_READS_NORMAL, rec.getAnnotation());
	}

}
