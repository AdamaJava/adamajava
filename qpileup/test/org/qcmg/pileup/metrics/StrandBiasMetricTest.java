package org.qcmg.pileup.metrics;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.pileup.PileupConstants;
import org.qcmg.pileup.metrics.record.ResultRecord;
import org.qcmg.pileup.metrics.record.StrandBiasRecord;
import org.qcmg.pileup.model.Chromosome;
import org.qcmg.pileup.model.QPileupRecord;

public class StrandBiasMetricTest {
	
	StrandBiasMetric metric;
	private static String CHR1 = "chr1";
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setUp() {
		String testPath = testFolder.getRoot().getAbsolutePath();
		metric = new StrandBiasMetric("test", testPath, testPath, 10, 3, 3);
		metric.addChromosome(CHR1);
		metric.setTotalPatients(1);
	}

	@After
	public void tearDown() {
		metric = null;
	}
	
	@Test
	public void testProcessRecord() throws Exception {
		QPileupRecord record = createMockQPileupRecord(20,24, false);
		metric.processRecord(record, 400);
		assertEquals(0, metric.getStrandBiasMap().get(CHR1).size());		
		record = createMockQPileupRecord(20, 400, true);
		metric.processRecord(record, 400);
		assertEquals(1, metric.getStrandBiasMap().get(CHR1).size());		
	}
	
	@Test
	public void testGetWindow() {
		metric.getStrandBiasMap().get(CHR1).put(10, getSBiasRecord(10,100));
		ResultRecord rr = metric.getWindow(CHR1, 1, 30, 200);		
		assertNotNull(rr);
		
		metric.getStrandBiasMap().get(CHR1).put(11, getSBiasRecord(11, 100));
		metric.getStrandBiasMap().get(CHR1).put(12, getSBiasRecord(12, 100));
		rr = metric.getWindow(CHR1, 1, 30, 200);		
		assertNotNull(rr);
		assertEquals(PileupConstants.METRIC_STRAND_BIAS, rr.getName());
		assertEquals(3, rr.getNumberPositions());
		assertEquals(3, rr.getTotalCounts());
		assertEquals(0, rr.getTotalRegularityScore(), 0.1);
		assertEquals(new AtomicLong(1).longValue(), metric.getWindowDistribution().getBaseDistributionCountMap().get(new BigDecimal(3)).longValue());
	}	
	
	@Test
	public void testClear() {
		metric.getStrandBiasMap().get(CHR1).put(10, getSBiasRecord(10, 100));
		assertEquals(1, metric.getStrandBiasMap().get(CHR1).size());
		metric.clear(new Chromosome(CHR1, 100));
		assertEquals(0, metric.getStrandBiasMap().get(CHR1).size());
	}	

	private StrandBiasRecord getSBiasRecord(int pos, int total) {
		StrandBiasRecord c =  new StrandBiasRecord(CHR1, pos, 'A', total);
		return c;
	}

	private QPileupRecord createMockQPileupRecord(int forward, int reverse, boolean isSBias) {
		long total = forward+reverse;
		QPileupRecord record = createMock(QPileupRecord.class);
		expect(record.getTotalBases(true, false)).andReturn(forward);
		expect(record.getTotalBases(false, true)).andReturn(forward);		
		expect(record.getElementCount("nonreferenceNo")).andReturn(total);
		if (isSBias) {			
			StrandBiasRecord r = new StrandBiasRecord(CHR1, 10, 'A', 10);
			r.setPercentForwardAlt(20.0);
			r.setPercentReverseAlt(2);
			expect(record.getStrandBiasRecord(10)).andReturn(r);
		} else {
			expect(record.getStrandBiasRecord(10)).andReturn(null);
		}
		
		replay(record);
		return record;
	}

}
