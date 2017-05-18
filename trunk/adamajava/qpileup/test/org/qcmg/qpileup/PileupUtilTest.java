package org.qcmg.qpileup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.qcmg.pileup.PileupConstants;
import org.qcmg.pileup.PileupUtil;

public class PileupUtilTest {
	
	@Test
	public void testWriteTime() {
		String time = PileupUtil.getCurrentTime(":");
		String[] splitTime = time.split(":");
		assertNotNull(time);
		assertEquals(3, splitTime.length);
		Integer hour = new Integer(splitTime[0]);
		Integer min = new Integer(splitTime[1]);
		Integer sec = new Integer(splitTime[2]);
		assertTrue(hour.intValue() >= 0 && hour.intValue() <=24);
		assertTrue(min.intValue() >= 0 && hour.intValue() <=59);
		assertTrue(sec.intValue() >= 0 && sec.intValue() <=59);
	}
	
	@Test
	public void testGetRunTime() {
		assertEquals("00:00:00", PileupUtil.getRunTime(1335244037771L, 1335244037771L));
		assertEquals("00:00:01", PileupUtil.getRunTime(1335244037771L, 1335244038771L));
		assertEquals("00:10:01", PileupUtil.getRunTime(1335244037771L, 1335244638771L));
		assertEquals("277:56:41", PileupUtil.getRunTime(1335244037771L, 1336244638771L));
	}
	
	@Test
	public void testGetBlockSize() {
		assertEquals(1000000, PileupUtil.getBlockSize("merge", 10));
		assertEquals(2000000, PileupUtil.getBlockSize("merge", 5));
		assertEquals(2000000, PileupUtil.getBlockSize("add", 5));
		assertEquals(5000000, PileupUtil.getBlockSize("add", 4));
	}
	
	@Test
	public void testIsRegularityType() {
		assertFalse(PileupUtil.isRegularityType(PileupConstants.METRIC_MAPPING));
		assertTrue(PileupUtil.isRegularityType(PileupConstants.METRIC_CLIP));
		assertTrue(PileupUtil.isRegularityType(PileupConstants.METRIC_NONREFBASE));
		assertTrue(PileupUtil.isRegularityType(PileupConstants.METRIC_INDEL));
	}
	
	@Test
	public void testGetChromosomeReference() {
		assertEquals("chr2", PileupUtil.getFullChromosome("2"));
		assertEquals("chr2", PileupUtil.getFullChromosome("chr2"));
		assertEquals("chrMT", PileupUtil.getFullChromosome("MT"));
		assertEquals("GL1", PileupUtil.getFullChromosome("GL1"));
	}
	
	@Test
	public void addChromosomeReference() {
		assertFalse(PileupUtil.addChromosomeReference("chr2"));
		assertTrue(PileupUtil.addChromosomeReference("2"));
		assertFalse(PileupUtil.addChromosomeReference("chr21"));
		assertTrue(PileupUtil.addChromosomeReference("21"));
		assertFalse(PileupUtil.addChromosomeReference("chr24"));
		assertFalse(PileupUtil.addChromosomeReference("24"));
		assertTrue(PileupUtil.addChromosomeReference("X"));
		assertTrue(PileupUtil.addChromosomeReference("Y"));
		assertTrue(PileupUtil.addChromosomeReference("M"));
		assertTrue(PileupUtil.addChromosomeReference("MT"));
	}
	

}
