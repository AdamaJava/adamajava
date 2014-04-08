package org.qcmg.pileup.metrics.record;



import org.junit.After;
import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.*;

public class IndelRecordTest {
	
	IndelRecord record;
	
	@Before
	public void setUp() {		
		record = new IndelRecord("chr1", 100, 100,  'A', "INS", 10, 200);		
	}
	
	@After
	public void tearDown() {
		record = null;
	}
	
	@Test
	public void testAddRecord() {
		IndelRecord r = new IndelRecord("chr1", 101, 101, 'A', "INS", 10, 5, 5, 200);		
		assertTrue(record.addIndelRecord(r));
		
		r = new IndelRecord("chr1", 103, 103, 'A', "INS", 10, 200);		
		assertFalse(record.addIndelRecord(r));
	}
	
	@Test
	public void testGetAllCountPercentTotalReads() {
		record.setAllCount(200);
		assertEquals(100, record.getAllCountPercentTotalReads(), .1);
		record.setAllCount(10);
		assertEquals(5, record.getAllCountPercentTotalReads(), .1);
		record.setAllCount(0);
		assertEquals(0, record.getAllCountPercentTotalReads(), .1);
	}
	
	@Test
	public void testToTmpString() {
		String expected = "chr1\t100\t100\tindel\t10\t200\tINS\t0\t0\tfalse\n";
		assertEquals(expected, record.toTmpString());
	}
	
	

}
