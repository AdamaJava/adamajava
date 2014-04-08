package org.qcmg.pileup.metrics.record;



import org.junit.After;
import org.junit.Test;


import static org.junit.Assert.*;

public class SnpRecordTest {
	
	SnpRecord record;
	
	@After
	public void tearDown() {
		record = null;
	}
	
	@Test
	public void testGetNotes() {
		record = new SnpRecord("chr1", 10, 'A', "", 'C', "test", 110, 100, 10);
		String expected = "";
		assertEquals(expected, record.getNotes(false));
		record.setStrandBias(true);
		record.setDbSnpId("rs1234");
		record.setGermdb("GERMDB");
		record.setNearestNeighbour(100);
		record.setComparisonFileAnnotation("TEST");
		record.setInMismapRegion(true);
		expected = "dbSNP=rs1234;GERMDB;TEST;NN:100;SBIAS;MISMAP";
		assertEquals(expected, record.getNotes(false));
		expected = "GERMDB;TEST;NN:100;SBIAS;MISMAP";
		assertEquals(expected, record.getNotes(true));
	}

}
