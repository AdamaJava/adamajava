package org.qcmg.pileup.metrics.record;



import org.junit.After;
import org.junit.Test;
import org.qcmg.pileup.PileupConstants;

import static org.junit.Assert.*;

public class MetricRecordTest {
	
	MetricRecord record;
	
	@After
	public void tearDown() {
		record = null;
	}
	
	@Test
	public void testGetPercentTotalReads() {
		record = new MetricRecord(PileupConstants.METRIC_CLIP, "chr1", 100, 10, 100);
		assertEquals(10, MetricRecord.getPercentage(record.getCount().longValue(), record.getTotalReads()), 0.1);
		record.setCount(0L);
		assertEquals(0, MetricRecord.getPercentage(record.getCount().longValue(), record.getTotalReads()), 0.1);
		record.setCount(110L);
		assertEquals(100, MetricRecord.getPercentage(record.getCount().longValue(), record.getTotalReads()), 0.1);
	}
	
	@Test
	public void testGetRegularityScore() {
		record = new MetricRecord(PileupConstants.METRIC_CLIP, "chr1", 100, 10, 100);
		assertEquals(100, record.getRegularityScore(), 0.1);
		
		record = new MetricRecord(PileupConstants.METRIC_NONREFBASE, "chr1", 100, 10, 100);
		assertEquals(100, record.getRegularityScore(), 0.1);		
	}
	
	@Test
	public void testToTmpString() {
		record = new MetricRecord(PileupConstants.METRIC_CLIP, "chr1", 100, 10, 100);
		String expected = "chr1\t100\t100\tclip\t10\t100\n";
		assertEquals(expected, record.toTmpString());
	}
	
	@Test
	public void testToGFFString() {
		record = new MetricRecord(PileupConstants.METRIC_CLIP, "chr1", 100, 10, 100);
		String expected = "chr1\tqpileup\t.\t100\t100\t10.00\t.\t.\tName=CLIP;color=#C0C0C0;PercentScore=10.00\n";
		assertEquals(expected, record.toGFFString());
	}

	

	
	

}
