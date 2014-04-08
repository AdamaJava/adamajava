package org.qcmg.pileup.metrics.record;



import org.junit.After;
import org.junit.Test;
import org.qcmg.pileup.PileupConstants;

import static org.junit.Assert.*;

public class ClipRecordTest {
	
	ClipRecord record;
	
	@After
	public void tearDown() {
		record = null;
	}
	
	@Test
	public void testToTmpString() {
		record = new ClipRecord(PileupConstants.METRIC_CLIP, "chr1", 1000, 100, 200);
		record.setStartCount(40);
		String expected = "chr1\t1000\t1000\tclip\t100\t40\t200\n";
		assertEquals(expected, record.toTmpString());		
	}
	
	@Test
	public void testGetRegularityScore() {
		record = new ClipRecord(PileupConstants.METRIC_CLIP, "chr1", 1000, 100, 200);
		record.setStartCount(40);
		assertEquals(400.0, record.getRegularityScore(), 0.1);
	}
	
	@Test
	public void testGetStartPercentTotalReads() {
		record = new ClipRecord(PileupConstants.METRIC_CLIP, "chr1", 1000, 100, 200);
		assertEquals(0, record.getStartPercentTotalReads(), 0.2);
		record.setStartCount(40);
		assertEquals(20, record.getStartPercentTotalReads(), 0.2);
		record.setStartCount(220);
		assertEquals(100, record.getStartPercentTotalReads(), 0.2);
	}
}
