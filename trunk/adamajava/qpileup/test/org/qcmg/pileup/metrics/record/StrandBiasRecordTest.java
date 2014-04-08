package org.qcmg.pileup.metrics.record;



import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StrandBiasRecordTest {
	
	StrandBiasRecord record;
	
	@Before
	public void setUp() {
		record = new StrandBiasRecord("chr1", 10, 'A', 32);
		
	}
	
	@After
	public void tearDown() {
		record = null;
	}
	
	@Test
	public void testForwardSBias() {
		record.addForwardBaseCounts('C', 10, 90, 100);
		record.addReverseBaseCounts('C', 100, 0, 100);
		assertEquals(90, record.getAltBaseCountDifference(), 0.1);
		assertEquals('C', record.getForwardAltBase());
		assertEquals(90, record.getForwardAltCount());
		assertEquals('C', record.getReverseAltBase());
		assertEquals(0, record.getReverseAltCount());
		assertEquals('A', record.getRefBase());
		assertEquals(90, record.getPercentForwardAlt(), 0.1);
		assertEquals(0, record.getPercentReverseAlt(), 0.1);
		assertTrue(record.hasStrandBias());
	}
	
	@Test
	public void testReverseSBias() {
		record.addReverseBaseCounts('C', 10, 90, 100);
		record.addForwardBaseCounts('C', 100, 0, 100);
		assertEquals(90, record.getAltBaseCountDifference(), 0.1);
		assertEquals('C', record.getForwardAltBase());
		assertEquals(0, record.getForwardAltCount());
		assertEquals('C', record.getReverseAltBase());
		assertEquals(90, record.getReverseAltCount());
		assertEquals('A', record.getRefBase());
		assertEquals(0, record.getPercentForwardAlt(), 0.1);
		assertEquals(90, record.getPercentReverseAlt(), 0.1);
		assertTrue(record.hasStrandBias());
	}
	
	@Test
	public void testNoStrandBias() {
		record.addReverseBaseCounts('C', 10, 90, 100);
		record.addForwardBaseCounts('C', 100, 80, 100);
		assertEquals(10, record.getAltBaseCountDifference(), 0.1);
		assertEquals('C', record.getForwardAltBase());
		assertEquals(80, record.getForwardAltCount());
		assertEquals('C', record.getReverseAltBase());
		assertEquals(90, record.getReverseAltCount());
		assertEquals('A', record.getRefBase());
		assertEquals(80, record.getPercentForwardAlt(), 0.1);
		assertEquals(90, record.getPercentReverseAlt(), 0.1);
		assertFalse(record.hasStrandBias());
	}
	

	

}
