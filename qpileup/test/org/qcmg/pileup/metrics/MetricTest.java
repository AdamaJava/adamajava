package org.qcmg.pileup.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.pileup.QPileupException;

public class MetricTest {
	
	private Metric clip;
	private final static String CHR1 = "chr1";
	
	@Before
	public void setUp() {
		clip = new ClipMetric(15.0, 3, 3);
		clip.addChromosome(CHR1);
		clip.setTotalPatients(10);
	}
	
	@After
	public void tearDown() {
		clip = null;
	}
	
	@Test
	public void testPassesMinAvgBases() throws QPileupException {
		assertTrue(clip.passesMinAvgBases(50));
		assertFalse(clip.passesMinAvgBases(10));
	}
	
	@Test
	public void testGetAvgBasesPerPatient() throws QPileupException {
		assertEquals(3, clip.getAvgBasesPerPatient(30), 0.1);
	}
	
	@Test(expected=QPileupException.class)
	public void testGetAvgBasesPerPatientThrowsException() throws QPileupException {
		clip.setTotalPatients(0);
		assertEquals(3, clip.getAvgBasesPerPatient(30), 0.1);
	}
}
