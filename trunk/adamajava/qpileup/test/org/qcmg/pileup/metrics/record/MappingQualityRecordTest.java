package org.qcmg.pileup.metrics.record;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class MappingQualityRecordTest {
	
	@Test
	public void testConstructorWithZeroMappingQual() {
		MappingQualityRecord record = new MappingQualityRecord("chr1", 123, 0, 1);
		assertEquals(123, record.getPosition().intValue());
		assertTrue(record.getAvgMappingQual() == 0);
	}
	
	@Test
	public void testConstructorWithAboveZeroMappingQual() {
		MappingQualityRecord record = new MappingQualityRecord("chr1", 123, 50, 10);
		assertEquals(123, record.getPosition().intValue());
		assertTrue(record.getAvgMappingQual() == 5);
	}

}
