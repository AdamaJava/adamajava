package org.qcmg.picard;

import java.util.concurrent.PriorityBlockingQueue;

import net.sf.samtools.SAMRecord;

import org.junit.Assert;
import org.junit.Test;

public class SAMRecordFilterWrapperTest {
	
	@Test
	public void testComparator() {
		SAMRecord record = new SAMRecord(null);
		SAMRecordFilterWrapper wrapper1 = new SAMRecordFilterWrapper(record, 1);
		SAMRecordFilterWrapper wrapper2 = new SAMRecordFilterWrapper(record, 2);
		
		PriorityBlockingQueue<SAMRecordFilterWrapper> queue = new PriorityBlockingQueue<SAMRecordFilterWrapper>();
		queue.add(wrapper1);
		queue.add(wrapper2);
		
		Assert.assertEquals(wrapper1, queue.poll());
		Assert.assertEquals(wrapper2, queue.poll());
		
		queue.add(wrapper2);
		queue.add(wrapper1);
		Assert.assertEquals(wrapper1, queue.poll());
		Assert.assertEquals(wrapper2, queue.poll());
		
		
		SAMRecordFilterWrapper wrapper3 = new SAMRecordFilterWrapper(record, 3);
		queue.add(wrapper2);
		queue.add(wrapper3);
		queue.add(wrapper1);
		Assert.assertEquals(wrapper1, queue.poll());
		Assert.assertEquals(wrapper2, queue.poll());
		Assert.assertEquals(wrapper3, queue.poll());
		
	}

}
