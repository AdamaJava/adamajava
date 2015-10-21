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
import org.qcmg.pileup.metrics.record.MetricRecord;
import org.qcmg.pileup.metrics.record.ResultRecord;
import org.qcmg.pileup.model.QPileupRecord;
import org.qcmg.pileup.model.StrandEnum;

public class MatesUnmappedMetricTest {
	
	private MatesUnmappedMetric metric;
	private final static String CHR1 = "chr1";
	
	@Before
	public void setUp() {
		metric = new MatesUnmappedMetric(15.0, 3, 3);
		metric.addChromosome(CHR1);
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
		assertEquals(0, metric.getRecordMap().get(CHR1).size());
		assertEquals(new AtomicLong(1).longValue(), metric.getBaseDistribution().getBaseDistributionCountMap().get(new BigDecimal(2.5)).longValue());
		record = createMockQPileupRecord(100L);
		metric.processRecord(record, 400);
		assertEquals(1, metric.getRecordMap().get(CHR1).size());

		assertEquals(new AtomicLong(1).longValue(), metric.getBaseDistribution().getBaseDistributionCountMap().get(new BigDecimal(25)).longValue());
	}
	
	@Test
	public void testGetWindow() {
		metric.getRecordMap().get(CHR1).put(10, getUnmappedMateRecord(10));
		ResultRecord rr = metric.getWindow(CHR1, 1, 30, 200);		
		assertNull(rr);
		
		metric.getRecordMap().get(CHR1).put(11, getUnmappedMateRecord(11));
		metric.getRecordMap().get(CHR1).put(12, getUnmappedMateRecord(12));
		rr = metric.getWindow(CHR1, 1, 30, 200);	
		assertNotNull(rr);
		assertEquals(PileupConstants.METRIC_UNMAPPED_MATE, rr.getName());
		assertEquals(3, rr.getNumberPositions());
		assertEquals(new AtomicLong(1).longValue(), metric.getWindowDistribution().getBaseDistributionCountMap().get(new BigDecimal(3)).longValue());	
	}
	
	private MetricRecord getUnmappedMateRecord(int pos) {
		MetricRecord c =  new MetricRecord(PileupConstants.METRIC_UNMAPPED_MATE, CHR1, pos, 20, 400);
		return c;
	}

	private QPileupRecord createMockQPileupRecord(long count) {
		QPileupRecord record = createMock(QPileupRecord.class);
		expect(record.getElementCount(StrandEnum.mateUnmapped.toString())).andReturn(count);

		expect(record.getChromosome()).andReturn(CHR1);
		expect(record.getBasePosition()).andReturn(10L);
		expect(record.getBase()).andReturn('A');
		replay(record);
		return record;
	}
}