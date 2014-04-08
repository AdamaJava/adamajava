package org.qcmg.pileup.metrics.record;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.pileup.PileupConstants;

public class ResultsSummaryTest {

	ResultSummary rs;
	
	
	@Before
	public void setUp() {
		String[] headerList = {PileupConstants.METRIC_CLIP, PileupConstants.METRIC_INDEL};
		
		String line = "chr1\t100\t110\t100\t200\t20.2\t6\t7\t10.5\t200";
		rs = new ResultSummary(headerList, line);
	}
	
	@After
	public void tearDown() {
		rs = null;
	}	
	
	@Test
	public void testConstructor() {
		
		assertEquals("chr1", rs.getChromosome());
		assertEquals(100, rs.getStart());
		assertEquals(110, rs.getEnd());
		assertEquals(2, rs.getResultRecords().size());
		assertTrue(rs.getResultRecords().containsKey(PileupConstants.METRIC_CLIP));
		assertTrue(rs.getResultRecords().containsKey(PileupConstants.METRIC_INDEL));
		assertEquals(.026, rs.getAdjustedRegularityScore(), 0.1);
		assertEquals(19.27, rs.getPositionCountScore(), 0.1);
		assertEquals(2.7, rs.getFinalRegularityScore(), 0.1);
	
	}
	
	@Test
	public void testAddRecord() {
		ResultRecord r = new ResultRecord(PileupConstants.METRIC_CLIP, 15, 60, 100);		
		rs = new ResultSummary("chr1", 1000, 2000, 100, true);
		assertFalse(rs.isGreaterThanZero);
		assertFalse(rs.isHasClips());
		assertEquals(0, rs.getMisMapperCount());
		rs.addRecord(PileupConstants.METRIC_CLIP, r);
		assertTrue(rs.isGreaterThanZero);
		assertTrue(rs.isHasClips());
		assertEquals(1, rs.getMisMapperCount());
		assertFalse(rs.isLowQualityMapper());
		r = new ResultRecord(PileupConstants.METRIC_MAPPING, 15, 60, 100);
		rs.addRecord(PileupConstants.METRIC_MAPPING, r);
		assertEquals(1, rs.getMisMapperCount());
		assertTrue(rs.isLowQualityMapper());
		assertEquals(2, rs.resultRecords.size());
	}
	
	@Test
	public void testCheckTmpRecords() {
		String expected = "0\t0\t0\t";
		assertEquals(expected, rs.checkTmpRecords(PileupConstants.METRIC_NONREFBASE));
		
		expected = "100\t200\t20.2\t";
		assertEquals(expected, rs.checkTmpRecords(PileupConstants.METRIC_CLIP));
	}
	
	@Test
	public void testCheckRecords() {
		String expected = "0\t0\t";
		assertEquals(expected, rs.checkRecords(PileupConstants.METRIC_NONREFBASE));
		
		expected = "100\t200\t";
		assertEquals(expected, rs.checkRecords(PileupConstants.METRIC_CLIP));
	}
	
	
	@Test
	public void testGetAdjustedRegularityScore() {
		assertEquals(0.026, rs.getAdjustedRegularityScore(), 0.1);		
		rs.setRegularityScore(0);
		assertEquals(0, rs.getAdjustedRegularityScore(), 0.1);		
	}	
	
	@Test
	public void testGetRegularityPositionCountScore() {
		assertEquals(106, rs.getRegularityPositionCountScore(), 0.1);	
		rs.addRecord(PileupConstants.METRIC_MAPPING, new ResultRecord(PileupConstants.METRIC_MAPPING, 1, 10, 122));
		assertEquals(106, rs.getRegularityPositionCountScore(), 0.1);	
	}
	
	@Test
	public void testGetName() {
		assertEquals(PileupConstants.IRREGULAR, rs.getName());
		rs.addRecord(PileupConstants.METRIC_MAPPING, new ResultRecord(PileupConstants.METRIC_MAPPING, 1, 10, 122));
		assertEquals(PileupConstants.MIXED, rs.getName());
	}
	
	@Test
	public void testGetMisMapperType() {
		assertEquals(PileupConstants.IRREGULAR, rs.getMisMapperType());
		assertFalse(rs.isRegularMapper());
		assertTrue(rs.isMisMapper());
		assertTrue(rs.isErrorRegion());
		assertTrue(rs.isWritableErrorRegion());
		assertTrue(rs.highPositionCount());
		//mixed
		rs.setRegularityScore(10000);
		assertTrue(rs.isRegularMapper());
		assertEquals(PileupConstants.MIXED, rs.getMisMapperType());
		assertEquals(PileupConstants.REGULAR + "," + PileupConstants.IRREGULAR, rs.getMixedCategories());
		
		//regular
		rs.setRegularityScore(120000);
		assertEquals(PileupConstants.REGULAR, rs.getMisMapperType());
	}
	
	@Test
	public void testLowMappingQual() {
		String[] headerList = {PileupConstants.METRIC_MAPPING};
		
		String line = "chr1\t100\t110\t100\t200\t20.2\t200\t";
		rs = new ResultSummary(headerList, line);
		assertFalse(rs.isMisMapper());
		assertTrue(rs.isLowQualityMapper());
		assertEquals(PileupConstants.LOW_MAPPING_QUAL, rs.getName());
		assertEquals("#FF0000", rs.getColour(PileupConstants.LOW_MAPPING_QUAL));
		assertEquals(0, rs.getRegularityPositionCountScore(), 0.1);
		String expected = "Name=LowMappingQual;color=#FF0000;PositionCountScore=18.18;AdjustedRegularityScore=0;RegularityScore=20.2";
		assertEquals(expected, rs.getAttributes());
	}	


	

}
