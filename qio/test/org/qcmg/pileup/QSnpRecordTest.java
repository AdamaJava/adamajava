package org.qcmg.pileup;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.qcmg.common.util.SnpUtils;

public class QSnpRecordTest {
	
	@Test
	public void testAddAnnotation() {
		QSnpRecord rec = new QSnpRecord();
		rec.addAnnotation(SnpUtils.MUTATION_IN_NORMAL);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL, rec.getAnnotation());
		
		// try adding again
		rec.addAnnotation(SnpUtils.MUTATION_IN_NORMAL);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL, rec.getAnnotation());
		
		// and now something else
		rec.addAnnotation(SnpUtils.LESS_THAN_8_READS_TUMOUR);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL + ";" + SnpUtils.LESS_THAN_8_READS_TUMOUR, rec.getAnnotation());
	}
	
	@Test
	public void testRemoveAnnotation() {
		QSnpRecord rec = new QSnpRecord();
		rec.addAnnotation(SnpUtils.MUTATION_IN_NORMAL);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL, rec.getAnnotation());
		rec.removeAnnotation(SnpUtils.LESS_THAN_12_READS_NORMAL);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL, rec.getAnnotation());
		rec.removeAnnotation(SnpUtils.MUTATION_IN_NORMAL);
		assertEquals(null, rec.getAnnotation());
		
		rec.addAnnotation(SnpUtils.MUTATION_IN_NORMAL);
		rec.addAnnotation(SnpUtils.LESS_THAN_12_READS_NORMAL);
		rec.removeAnnotation(SnpUtils.LESS_THAN_12_READS_NORMAL);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL, rec.getAnnotation());
		
		rec.addAnnotation(SnpUtils.LESS_THAN_12_READS_NORMAL);
		rec.removeAnnotation(SnpUtils.MUTATION_IN_NORMAL);
		assertEquals(SnpUtils.LESS_THAN_12_READS_NORMAL, rec.getAnnotation());
	}

}
