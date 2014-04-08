package org.qcmg.pileup.metrics.record;



import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.pileup.PileupConstants;

import static org.junit.Assert.*;

public class ResultRecordTest {
	
	ResultRecord record;
	
	@Before
	public void setUp() {
		record = new ResultRecord(PileupConstants.METRIC_CLIP, 20, 150, 160.0);
	}
	
	@After
	public void tearDown() {
		record = null;
	}
	
	@Test
	public void testToTmpString() {		
		String expected = "20\t150\t160.0\t";
		assertEquals(expected, record.toTmpString());
	}
	
	@Test
	public void testMergeRecord() {
		ResultRecord merge = new ResultRecord(PileupConstants.METRIC_CLIP, 10, 100, 110.0);
		record.mergeRecords(merge);
		assertEquals(30, record.getNumberPositions());
		assertEquals(250, record.getTotalCounts());
		assertEquals(270, record.getTotalRegularityScore(), 0.1);		
	}
	
	

}
