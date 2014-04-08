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
import org.qcmg.pileup.metrics.record.MappingQualityRecord;
import org.qcmg.pileup.metrics.record.ResultRecord;
import org.qcmg.pileup.model.QPileupRecord;
import org.qcmg.pileup.model.StrandEnum;

public class MappingQualityMetricTest {
	
	private MappingQualityMetric metric;
	private final static String CHR1 = "chr1";
	
	@Before
	public void setUp() {
		metric = new MappingQualityMetric(15.0, 3, 3);
		metric.addChromosome(CHR1);
		metric.setTotalPatients(10);
	}

	@After
	public void tearDown() {
		metric = null;
	}
	
	@Test
	public void testProcessRecord() throws Exception {
		QPileupRecord record = createMockQPileupRecord(10000L);
		metric.processRecord(record, 400);
		assertEquals(0, metric.getQualRecordMap().get(CHR1).size());
		assertEquals(new AtomicLong(1).longValue(), metric.getBaseDistribution().getBaseDistributionCountMap().get(new BigDecimal(25.0)).longValue());
		record = createMockQPileupRecord(100L);
		metric.processRecord(record, 400);
		assertEquals(1, metric.getQualRecordMap().get(CHR1).size());
		BigDecimal bd = new BigDecimal(0.3);
		BigDecimal rounded = bd.setScale(1, BigDecimal.ROUND_HALF_UP);
		assertEquals(new AtomicLong(1).longValue(), metric.getBaseDistribution().getBaseDistributionCountMap().get(rounded).longValue());
	}
	
	@Test
	public void testGetWindow() {
		metric.getQualRecordMap().get(CHR1).put(10, getMQRecord(10));
		ResultRecord rr = metric.getWindow(CHR1, 1, 30, 200);		
		assertNull(rr);
		
		metric.getQualRecordMap().get(CHR1).put(11, getMQRecord(11));
		metric.getQualRecordMap().get(CHR1).put(12, getMQRecord(12));
		rr = metric.getWindow(CHR1, 1, 30, 200);	
		assertNotNull(rr);
		assertEquals(PileupConstants.METRIC_MAPPING, rr.getName());
		assertEquals(3, rr.getNumberPositions());
		assertEquals(new AtomicLong(1).longValue(), metric.getWindowDistribution().getBaseDistributionCountMap().get(new BigDecimal(3)).longValue());	
	}
	
	private MappingQualityRecord getMQRecord(int pos) {
		MappingQualityRecord c =  new MappingQualityRecord(CHR1, pos, 20, 400);
		return c;
	}

	private QPileupRecord createMockQPileupRecord(long count) {
		QPileupRecord record = createMock(QPileupRecord.class);
		expect(record.getElementCount(StrandEnum.mapQual.toString())).andReturn(count);

		expect(record.getChromosome()).andReturn(CHR1);
		expect(record.getBasePosition()).andReturn(10L);
		expect(record.getBase()).andReturn('A');
		replay(record);
		return record;
	}

}
