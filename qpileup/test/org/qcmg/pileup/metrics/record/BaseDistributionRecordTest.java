package org.qcmg.pileup.metrics.record;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Test;
import org.qcmg.pileup.PileupConstants;
import static org.junit.Assert.assertEquals;

public class BaseDistributionRecordTest {
	
	BaseDistributionRecord record;
	
	@After
	public void tearDown() {
		record = null;
	}
	
	@Test
	public void testBaseConstructor() {
		record = new BaseDistributionRecord(PileupConstants.METRIC_NONREFBASE, false);
		assertEquals(1001, record.getBaseDistributionCountMap().size());
	}
	
	@Test
	public void testWindowConstructor() {
		record = new BaseDistributionRecord(PileupConstants.METRIC_NONREFBASE, true);
		assertEquals(PileupConstants.WINDOW_SIZE+1, record.getBaseDistributionCountMap().size());
	}
	
	@Test
	public void testAddBaseCountsWithNonRefBaseMetric() {
		record = new BaseDistributionRecord(PileupConstants.METRIC_NONREFBASE, true);
		record.addBaseCounts(0, 10);
		assertEquals(1L, record.getBaseDistributionCountMap().get(new BigDecimal(100)).longValue());
		record.addBaseCounts(10, 0);
		assertEquals(1L, record.getBaseDistributionCountMap().get(new BigDecimal(0)).longValue());
		record.addBaseCounts(200, 100);
		assertEquals(1L, record.getBaseDistributionCountMap().get(new BigDecimal(50)).longValue());
	}
	
	@Test
	public void testAddBaseCountsWithMappingQualMetric() {
		record = new BaseDistributionRecord(PileupConstants.METRIC_MAPPING, true);
		record.addBaseCounts(10, 0);
		assertEquals(1L, record.getBaseDistributionCountMap().get(new BigDecimal(0)).longValue());
		record.addBaseCounts(200, 400);
		assertEquals(1L, record.getBaseDistributionCountMap().get(new BigDecimal(2)).longValue());
	}
	
	@Test
	public void testAddBaseCountsWithSnpMetric() {
		record = new BaseDistributionRecord(PileupConstants.METRIC_SNP, true);
		record.addBaseCounts(100, 2);
		assertEquals(1L, record.getBaseDistributionCountMap().get(new BigDecimal(2)).longValue());
	}
	
	@Test
	public void testAddBaseCountsWithStrandBiasMetric() {
		record = new BaseDistributionRecord(PileupConstants.METRIC_STRAND_BIAS, true);
		record.addBaseCounts(100, 2);
		assertEquals(1L, record.getBaseDistributionCountMap().get(new BigDecimal(2)).longValue());
	}
	
	@Test
	public void testGetFinalTotal() {
		record = new BaseDistributionRecord(PileupConstants.METRIC_NONREFBASE, false);
		ConcurrentSkipListMap<BigDecimal, AtomicLong> map = getMap();		
		assertEquals(5L, record.getFinalTotal(map));
	}
	
	private ConcurrentSkipListMap<BigDecimal, AtomicLong> getMap() {
		ConcurrentSkipListMap<BigDecimal, AtomicLong> map  = new ConcurrentSkipListMap<BigDecimal, AtomicLong>();
			map.put(new BigDecimal(1), new AtomicLong(3));
			map.put(new BigDecimal(2), new AtomicLong(2));
		return map;
	}

	@Test
	public void testToStringForBaseDistribution() {
		record = new BaseDistributionRecord(PileupConstants.METRIC_NONREFBASE, false);
		record.setBaseDistributionCountMap(getMap());		
		String[] actual = record.toString().split("\n");
		assertEquals(4, actual.length);
		assertEquals("#BASELINE_DISTRIBUTION: nonreference_base", actual[0]);
		assertEquals("#%nonreference_base\tnumber_positions\t%_total_positions\t%_total_positions", actual[1]);
		assertEquals("1.0\t3\t60.0000\t60.0000", actual[2]);
		assertEquals("2.0\t2\t40.0000\t40.0000", actual[3]);
	}
	
	@Test
	public void testToStringForWindowDistribution() {
		record = new BaseDistributionRecord(PileupConstants.METRIC_NONREFBASE, true);
		record.setBaseDistributionCountMap(getMap());		
		String[] actual = record.toString().split("\n");
		assertEquals(4, actual.length);
		assertEquals("#BASELINE_DISTRIBUTION: nonreference_base", actual[0]);
		assertEquals("#%nonreference_base\tnumber_positions\t%_total_windows\t%_total_windows", actual[1]);
		assertEquals("1.0\t3\t60.0000\t60.0000", actual[2]);
		assertEquals("2.0\t2\t40.0000\t40.0000", actual[3]);
	}

}
