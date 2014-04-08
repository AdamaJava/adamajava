package org.qcmg.pileup.metrics;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.pileup.PileupConstants;
import org.qcmg.pileup.metrics.record.IndelRecord;
import org.qcmg.pileup.metrics.record.ResultRecord;

import org.qcmg.pileup.model.QPileupRecord;
import org.qcmg.pileup.model.StrandEnum;

public class IndelMetricTest {
	
	private IndelMetric metric;
	private final static String CHR1 = "chr1";
	
	@Before
	public void setUp() {
		metric = new IndelMetric(5.0, 3, 3);
		metric.addChromosome(CHR1);
		metric.setTotalPatients(10);
	}

	@After
	public void tearDown() {
		metric = null;
	}
	
	@Test
	public void testProcessRecordWithIns() throws Exception {
		QPileupRecord record = createMockQPileupRecord(10L, 0, 0);
		metric.processRecord(record, 400);
		assertEquals(0, metric.getInsMap().get(CHR1).size());
		assertEquals(0, metric.getDelAllMap().get(CHR1).size());
		//assertEquals(new AtomicLong(1).longValue(), metric.getBaseDistribution().getBaseDistributionCountMap().get(new BigDecimal(5.0)).longValue());
		record = createMockQPileupRecord(50L, 0, 0);
		metric.processRecord(record, 400);
		assertEquals(1, metric.getInsMap().get(CHR1).size());
		assertEquals(new AtomicLong(1).longValue(), metric.getBaseDistribution().getBaseDistributionCountMap().get(new BigDecimal(25.0)).longValue());
	}
	
	@Test
	public void testProcessRecordWithDel() throws Exception {
		QPileupRecord record = createMockQPileupRecord(0L, 10, 10);
		metric.processRecord(record, 400);
		assertEquals(0, metric.getInsMap().get(CHR1).size());
		assertEquals(0, metric.getDelAllMap().get(CHR1).size());
		assertEquals(0, metric.getDelStartMap().get(CHR1).size());
		assertEquals(new AtomicLong(1).longValue(), metric.getBaseDistribution().getBaseDistributionCountMap().get(new BigDecimal(5.0)).longValue());
		record = createMockQPileupRecord(0L, 50, 30);
		metric.processRecord(record, 400);
		assertEquals(0, metric.getInsMap().get(CHR1).size());
		assertEquals(1, metric.getDelAllMap().get(CHR1).size());
		assertEquals(1, metric.getDelStartMap().get(CHR1).size());
		assertEquals(new AtomicLong(1).longValue(), metric.getBaseDistribution().getBaseDistributionCountMap().get(new BigDecimal(15.0)).longValue());
	}
	
	@Test
	public void testGetWindowForInsertion() {		
		metric.getInsMap().get(CHR1).put(10, getIndelRecord(10, PileupConstants.INS));
		ResultRecord rr = metric.getWindow(CHR1, 1, 30, 200);		
		assertNull(rr);
		
		metric.getInsMap().get(CHR1).put(11, getIndelRecord(11, PileupConstants.INS));
		metric.getInsMap().get(CHR1).put(12, getIndelRecord(12, PileupConstants.INS));
		rr = metric.getWindow(CHR1, 1, 30, 200);	
		assertGetWindow(rr);
	}

	@Test
	public void testGetWindowForDeletion() {
		metric.getInsMap().get(CHR1).put(10, getIndelRecord(10, PileupConstants.DEL));
		ResultRecord rr = metric.getWindow(CHR1, 1, 30, 200);		
		assertNull(rr);
		
		metric.getInsMap().get(CHR1).put(11, getIndelRecord(11, PileupConstants.DEL));
		metric.getInsMap().get(CHR1).put(12, getIndelRecord(12, PileupConstants.DEL));
		rr = metric.getWindow(CHR1, 1, 30, 200);		
	}	
	
	@Test
	public void testGetWindowForDeletionAndInsertion() {
		metric.getInsMap().get(CHR1).put(10, getIndelRecord(10, PileupConstants.INS));
		ResultRecord rr = metric.getWindow(CHR1, 1, 30, 200);		
		assertNull(rr);
		
		metric.getInsMap().get(CHR1).put(11, getIndelRecord(11, PileupConstants.DEL));
		metric.getInsMap().get(CHR1).put(12, getIndelRecord(12, PileupConstants.DEL));
		rr = metric.getWindow(CHR1, 1, 30, 200);		
	}
	
	@Test
	public void testAddChromosome() {
		String chr = CHR1;
		metric.addChromosome(chr);
		assertTrue(metric.getInsMap().containsKey(chr));
		assertTrue(metric.getDelAllMap().containsKey(chr));
		assertTrue(metric.getDelStartMap().containsKey(chr));
		assertTrue(metric.getInsMap().containsKey(chr));
		assertTrue(metric.getSummaryMap().containsKey(chr));
	}
	
	@Test
	public void testGetRegularInsRecord() {
		metric.getInsMap().get(CHR1).put(10, new IndelRecord(CHR1, 10, 10, 'A', PileupConstants.INS, 20, 100));
		assertNull(metric.getRegularInsRecord(CHR1, 10));
		
		metric.getInsMap().get(CHR1).put(10, new IndelRecord(CHR1, 10, 10, 'A', PileupConstants.INS, 90, 100));
		assertNotNull(metric.getRegularInsRecord(CHR1, 10));
	}
	
	@Test
	public void testGetRegularDelRecord() {
		metric.getDelStartMap().get(CHR1).put(10, new IndelRecord(CHR1, 10,10, 'A', PileupConstants.DEL, 20, 100));
		metric.getDelAllMap().get(CHR1).put(10, new IndelRecord(CHR1, 10, 10,'A', PileupConstants.DEL, 20, 100));
		assertNull(metric.getRegularDelRecord(CHR1, 10));
		
		metric.getDelStartMap().get(CHR1).put(10, new IndelRecord(CHR1, 10, 10,'A', PileupConstants.DEL, 90, 100));
		metric.getDelAllMap().get(CHR1).put(11, new IndelRecord(CHR1, 11, 10,'A', PileupConstants.DEL, 90, 100));
		IndelRecord r = metric.getRegularDelRecord(CHR1, 10);
		assertNotNull(r);
		assertEquals(10, r.getStartPosition());
		assertEquals(11, r.getEndPosition());
	}	

	private void assertGetWindow(ResultRecord rr) {
		assertNotNull(rr);
		assertEquals(PileupConstants.METRIC_INDEL, rr.getName());
		assertEquals(3, rr.getNumberPositions());
		assertEquals(60, rr.getTotalCounts());
		assertEquals(1200, rr.getTotalRegularityScore(), 0.1);
		assertEquals(new AtomicLong(1).longValue(), metric.getWindowDistribution().getBaseDistributionCountMap().get(new BigDecimal(3)).longValue());	
	}

	private IndelRecord getIndelRecord(int pos, String mutationType) {		
		IndelRecord c =  new IndelRecord(CHR1, pos, pos, 'A', mutationType, 20, 100);
		return c;
	}

	private QPileupRecord createMockQPileupRecord(long insCount, long delCount, long delStartCount) {
		QPileupRecord record = createMock(QPileupRecord.class);
		expect(record.getForwardElement(StrandEnum.cigarI.toString())).andReturn(insCount);
		expect(record.getReverseElement(StrandEnum.cigarI.toString())).andReturn(insCount);
		expect(record.getForwardElement(StrandEnum.cigarDStart.toString())).andReturn(delStartCount);
		expect(record.getReverseElement(StrandEnum.cigarDStart.toString())).andReturn(delStartCount);
		expect(record.getElementCount(StrandEnum.cigarD.toString())).andReturn(delCount);
		expect(record.getChromosome()).andReturn(CHR1);
		expect(record.getBasePosition()).andReturn(10L);
		expect(record.getBase()).andReturn('A');
		replay(record);
		return record;
	}

}
