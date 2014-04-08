package org.qcmg.pileup.metrics;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.pileup.PileupConstants;

public class SummaryMetricTest {
	
	SummaryMetric metric;
	String testPath;
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setUp() throws Exception {
		testPath = testFolder.getRoot().getAbsolutePath();
		metric = new SummaryMetric("test", testPath, testPath, testPath, testPath, testPath);
		metric.addMetric(PileupConstants.METRIC_CLIP, 1.0, 1, 1);		
		MappingQualityMetric m = new MappingQualityMetric(1, 1, 1);
		metric.addMetric(PileupConstants.METRIC_MAPPING,m);
		assertEquals(2, metric.getMetrics().size());
	}
	
	@After
	public void tearDown() {
		metric = null;
	}
	
	@Test
	public void testAddMetric() throws Exception {
		StrandBiasMetric m = new StrandBiasMetric("test", testPath, testPath, 1, 1, 1);
		assertEquals(2, metric.getMetrics().size());
		metric.addMetric(PileupConstants.METRIC_STRAND_BIAS, m);
		assertEquals(3, metric.getMetrics().size());
		
		metric.addMetric(PileupConstants.METRIC_NONREFBASE, 1.0, 1, 1);
		assertEquals(4, metric.getMetrics().size());
		metric.addMetric(PileupConstants.METRIC_UNMAPPED_MATE, 1.0, 1, 1);
		assertEquals(5, metric.getMetrics().size());		
	}
	
	@Test
	public void testToHeaderString() {
		String expected = "no_of_clip_positions\ttotal_clip_counts\tno_of_mapping_qual_positions\ttotal_mapping_qual_average\t";
		assertEquals(expected, metric.toHeaderString());
	}

}
