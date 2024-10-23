package org.qcmg.coverage;

import org.junit.Test;
import org.qcmg.qio.gff3.Gff3Record;

import static org.junit.Assert.*;

public class JobQueueTest {

	@Test
	public void isGFF3RecordValid() {
        assertFalse(JobQueue.isGff3RecordValid(null));
        assertTrue(JobQueue.isGff3RecordValid(new Gff3Record()));
		Gff3Record rec = new Gff3Record();
		rec.setStart(1);
		rec.setEnd(0);
        assertFalse(JobQueue.isGff3RecordValid(rec));
		rec.setEnd(1);
        assertTrue(JobQueue.isGff3RecordValid(rec));
	}
}
