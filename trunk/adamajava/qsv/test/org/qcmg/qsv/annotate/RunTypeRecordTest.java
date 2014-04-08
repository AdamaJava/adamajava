package org.qcmg.qsv.annotate;

import static org.junit.Assert.*;

import org.junit.Test;
import org.qcmg.qsv.annotate.RunTypeRecord;

public class RunTypeRecordTest {
    

	@Test
	public void testAddToMapIsAdded() {
		RunTypeRecord record = new RunTypeRecord("20120524035008880", 120, 1000, "test");
		assertEquals(record.getCount(), 0);
		record.addToMap(1000);
		assertEquals(1, record.getIsizeMap().get(1000).intValue());
		assertEquals(record.getCount(), 1);
	}
	
	@Test
	public void testAddToMapIsNotAdded() {
		RunTypeRecord record = new RunTypeRecord("20120524035008880", 120, 1000, "test");
		assertEquals(record.getCount(), 0);
		record.addToMap(6000);
		assertEquals(null, record.getIsizeMap().get(6000));
		assertEquals(record.getCount(), 0);
	}


}
