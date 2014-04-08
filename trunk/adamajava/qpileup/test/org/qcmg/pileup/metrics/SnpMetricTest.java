package org.qcmg.pileup.metrics;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.pileup.PileupConstants;
import org.qcmg.pileup.metrics.record.ResultRecord;
import org.qcmg.pileup.metrics.record.SnpRecord;
import org.qcmg.pileup.model.Chromosome;
import org.qcmg.pileup.model.QPileupRecord;
import org.qcmg.pileup.model.StrandEnum;
import org.qcmg.tab.TabbedRecord;

public class SnpMetricTest {
	
	private SnpMetric metric;
	private File germlineFile;
	private File dbSNPFile;
	private File snpFile;
	private static String CHR1 = "chr1";
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setUp() throws IOException {
		String testPath = testFolder.getRoot().getAbsolutePath();
		germlineFile = testFolder.newFile("germline.vcf");
		dbSNPFile = testFolder.newFile("dbSNP.vcf");
		snpFile = testFolder.newFile("snp.vcf");
		metric = new SnpMetric("test.h5", testPath, testPath, testPath, 20, 5, 1, dbSNPFile,
				germlineFile, snpFile, "vcf", "TEST", 3, testPath);
		metric.addChromosome(CHR1);
		metric.setTotalPatients(10);
	}

	@After
	public void tearDown() {
		metric = null;
	}
	
	@Test
	public void testProcessRecord() throws Exception {
		QPileupRecord record = createMockQPileupRecord(2L);
		metric.processRecord(record, 400);
		assertEquals(0, metric.getSnpMap().get(CHR1).size());		
		record = createMockQPileupRecord(10L);
		metric.processRecord(record, 400);
		assertEquals(1, metric.getSnpMap().get(CHR1).size());
	}
	
	@Test
	public void testGetWindow() {
		metric.getSnpMap().get("chr1").put(10, getSnpRecord(10, 20, 100));
		ResultRecord rr = metric.getWindow("chr1", 1, 30, 200);		
		assertNull(rr);
		
		metric.getSnpMap().get("chr1").put(11, getSnpRecord(11, 20, 100));
		metric.getSnpMap().get("chr1").put(12, getSnpRecord(12, 20, 100));
		rr = metric.getWindow("chr1", 1, 30, 200);		
		assertNotNull(rr);
		assertEquals(PileupConstants.METRIC_SNP, rr.getName());
		assertEquals(3, rr.getNumberPositions());
		assertEquals(3, rr.getTotalCounts());
		assertEquals(0, rr.getTotalRegularityScore(), 0.1);
		assertEquals(new AtomicLong(1).longValue(), metric.getWindowDistribution().getBaseDistributionCountMap().get(new BigDecimal(3)).longValue());
	}

	@Test
	public void testTabbedRecordFallsInCompareRecord() {
		TreeMap<ChrPosition, TabbedRecord> map = new TreeMap<ChrPosition, TabbedRecord>();
		map.put(new ChrPosition(CHR1, 11, 13), new TabbedRecord());
		assertTrue(metric.tabbedRecordFallsInCompareRecord(new ChrPosition(CHR1, 10, 12), map.firstEntry()));
		assertTrue(metric.tabbedRecordFallsInCompareRecord(new ChrPosition(CHR1, 10, 14), map.firstEntry()));
		assertFalse(metric.tabbedRecordFallsInCompareRecord(new ChrPosition(CHR1, 10, 10), map.firstEntry()));
	}
	
	@Test
	public void testClear() {
		metric.getSnpMap().get("chr1").put(10, getSnpRecord(10, 20, 100));
		assertEquals(1, metric.getSnpMap().get("chr1").size());
		metric.clear(new Chromosome("chr1", 100));
		assertEquals(0, metric.getSnpMap().get("chr1").size());
	}	
	
	@Test
	public void testPassesFilters() {
		assertTrue(metric.passesFilters(10, 11, 12, 10, 11, 12));
		assertFalse(metric.passesFilters(9, 11, 12, 10, 11, 12));
		assertFalse(metric.passesFilters(9, 10, 12, 10, 11, 12));
		assertFalse(metric.passesFilters(10, 11, 11, 10, 11, 12));
	}

	private SnpRecord getSnpRecord(int pos, int counts, int total) {
		SnpRecord c =  new SnpRecord(CHR1, pos, 'A', total);
		return c;
	}

	private QPileupRecord createMockQPileupRecord(long bases) {
		QPileupRecord record = createMock(QPileupRecord.class);
		expect(record.getChromosome()).andReturn(CHR1);
		expect(record.getChromosome()).andReturn(CHR1);
		expect(record.getBase()).andReturn('A');
		expect(record.getBasePosition()).andReturn(10L);
		expect(record.getAltBase(true, true)).andReturn('C');
		expect(record.getTotalBasesString()).andReturn("ACTG");
		expect(record.getDCCBaseCountString()).andReturn("ACTG");
		expect(record.getTotalAltBases('C')).andReturn(10);
		expect(record.getTotalElement(StrandEnum.referenceNo.toString())).andReturn(50L);
		expect(record.getTotalElement(StrandEnum.nonreferenceNo.toString())).andReturn(bases);
		expect(record.getTotalElement(StrandEnum.highNonreference.toString())).andReturn(bases);
		expect(record.getGenotypeEnum()).andReturn(null);
		expect(record.getStrandBiasRecord(10)).andReturn(null);
		replay(record);
		return record;
	}

}
