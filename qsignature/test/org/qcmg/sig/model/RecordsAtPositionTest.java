package org.qcmg.sig.model;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMRecord;

import org.junit.Test;
import org.qcmg.common.model.ChrPosition;

public class RecordsAtPositionTest {
	
	@Test
	public void testRecordsAtPosition() {
		RecordsAtPosition rap = new RecordsAtPosition(null, null);
		assertEquals(null, rap.getCp());
		assertEquals(null, rap.getRecords());
		
		ChrPosition cp = new ChrPosition("1", 999);
		List<SAMRecord> recs = new ArrayList<>();
		
		rap = new RecordsAtPosition(cp, recs);
		assertEquals(cp, rap.getCp());
		assertEquals(recs, rap.getRecords());
		assertEquals(0, rap.getRecords().size());
	}

}
