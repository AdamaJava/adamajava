package org.qcmg.coverage;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.qcmg.gff3.GFF3Record;

public class JobQueueTest {

	@Test
	public void isGFF3RecordValid() {
		assertEquals(false, JobQueue.isGff3RecordValid(null));
		assertEquals(true, JobQueue.isGff3RecordValid(new GFF3Record()));
		GFF3Record rec = new GFF3Record();
		rec.setStart(1);
		rec.setEnd(0);
		assertEquals(false, JobQueue.isGff3RecordValid(rec));
		rec.setEnd(1);
		assertEquals(true, JobQueue.isGff3RecordValid(rec));
	}
}
