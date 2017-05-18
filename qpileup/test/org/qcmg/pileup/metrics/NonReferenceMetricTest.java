package org.qcmg.pileup.metrics;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.pileup.PileupConstants;
import org.qcmg.pileup.QPileupException;
import org.qcmg.pileup.metrics.record.MetricRecord;
import org.qcmg.pileup.metrics.record.ResultRecord;
import org.qcmg.pileup.model.Chromosome;
import org.qcmg.pileup.model.QPileupRecord;
import org.qcmg.pileup.model.StrandEnum;

public class NonReferenceMetricTest {
	
	NonReferenceMetric metric;
	
	@Before
	public void setUp() {
		metric = new NonReferenceMetric(5.0, 3, 3);
		metric.addChromosome("chr1");
		metric.setTotalPatients(10);
	}

	@After
	public void tearDown() {
		metric = null;
	}
	
	@Test
	public void testProcessRecord() throws Exception {
		QPileupRecord record = createMockQPileupRecord(10L);
		metric.processRecord(record, 400);
		assertEquals(1, metric.getRecordMap().get("chr1").size());
		assertEquals(new AtomicLong(1).longValue(), metric.getBaseDistribution().getBaseDistributionCountMap().get(new BigDecimal(5.0)).longValue());
		record = createMockQPileupRecord(5L);
		metric.processRecord(record, 400);
		assertEquals(1, metric.getRecordMap().get("chr1").size());
		assertEquals(new AtomicLong(1).longValue(), metric.getBaseDistribution().getBaseDistributionCountMap().get(new BigDecimal(2.5)).longValue());	
	}
	
	@Test
	public void testGetWindow() {
		metric.getRecordMap().get("chr1").put(10, getNonreferenceRecord(10, 20, 100));
		ResultRecord rr = metric.getWindow("chr1", 1, 30, 200);		
		assertNull(rr);
		
		metric.getRecordMap().get("chr1").put(11, getNonreferenceRecord(11, 20, 100));
		metric.getRecordMap().get("chr1").put(12, getNonreferenceRecord(12, 20, 100));
		rr = metric.getWindow("chr1", 1, 30, 200);		
		assertNotNull(rr);
		assertEquals(PileupConstants.METRIC_NONREFBASE, rr.getName());
		assertEquals(3, rr.getNumberPositions());
		assertEquals(60, rr.getTotalCounts());
		assertEquals(1200, rr.getTotalRegularityScore(), 0.1);
		assertEquals(new AtomicLong(1).longValue(), metric.getWindowDistribution().getBaseDistributionCountMap().get(new BigDecimal(3)).longValue());
	}
	
	@Test
	public void getRegularRecord() {
		metric.getRecordMap().get("chr1").put(10, getNonreferenceRecord(10, 90, 100));
		MetricRecord record = metric.getRegularRecord("chr1", 10);
		assertNotNull(record);
		metric.getRecordMap().get("chr1").put(10, getNonreferenceRecord(10, 5, 100));
		record = metric.getRegularRecord("chr1", 10);
		assertNull(record);
	}
	
	@Test
	public void testClear() throws QPileupException {
		metric.getRecordMap().get("chr1").put(10, getNonreferenceRecord(10, 20, 100));
		assertEquals(1, metric.getRecordMap().get("chr1").size());
		metric.clear(new Chromosome("chr1", 100));
		assertEquals(0, metric.getRecordMap().get("chr1").size());
	}	

	private MetricRecord getNonreferenceRecord(int pos, int counts, int total) {
		MetricRecord c =  new MetricRecord(PileupConstants.METRIC_NONREFBASE, "chr1", pos, counts, total);
		return c;
	}

	private QPileupRecord createMockQPileupRecord(long bases) {
		QPileupRecord record = createMock(QPileupRecord.class);
		expect(record.getForwardElement(StrandEnum.nonreferenceNo.toString())).andReturn(bases);
		expect(record.getReverseElement(StrandEnum.nonreferenceNo.toString())).andReturn(bases);
		expect(record.getChromosome()).andReturn("chr1");
		expect(record.getBasePosition()).andReturn(10L);
		replay(record);
		return record;
	}
	

}
