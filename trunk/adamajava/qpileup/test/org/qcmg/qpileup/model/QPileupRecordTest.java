package org.qcmg.qpileup.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.pileup.hdf.StrandDS;
import org.qcmg.pileup.model.Base;
import org.qcmg.pileup.model.PositionElement;
import org.qcmg.pileup.model.QPileupRecord;
import org.qcmg.pileup.model.StrandElement;
import org.qcmg.pileup.model.StrandEnum;


public class QPileupRecordTest {
	
	private QPileupRecord record;
	private final String CHR1 = "chr1";
	
	@Before
	public void setUp() {				
		record = createQPileupRecord(CHR1, 10, 'A', false); 
	}
	
	private QPileupRecord createQPileupRecord(String chr, int pos, char base, boolean isStrandBias) {
		PositionElement e = new PositionElement(chr, pos, base);
		
		StrandDS forward = new StrandDS(CHR1, false, 1);
		StrandDS reverse = new StrandDS(CHR1, true, 1);
		
		Map<String, StrandElement> forwardElementMap = forward.getElementsMap();
		Map<String, StrandElement> reverseElementMap = reverse.getElementsMap();
		setUpMap(forwardElementMap, false, false);
		setUpMap(reverseElementMap, true, false);	
		return new QPileupRecord(e, forward.getElementsMap(), reverse.getElementsMap());
	}

	private void setUpMap(Map<String, StrandElement> map, boolean isReverse, boolean isStrandBias) {
		int count = 1;
		
		/*
		 * Need to have a consistent means of looping through the maps, as we are assigning values based on iteration order
		 */
		List<String> orderedList = new ArrayList<>(map.keySet());
		Collections.sort(orderedList);
		for (String s : orderedList) {
			StrandElement se = map.get(s);
			if (se.isLong()) {				
				se.setLongDataMembers(new long[]{isReverse ? count * 2 : count});
			} else {
				int[] intDataMembers = new int[]{isReverse ? count * 2 : count};
				if (s.equals("referenceNo")) {					
					intDataMembers[0] = isReverse ? 24 : 12;
				} 
				if (s.equals("nonreferenceNo")) {
					intDataMembers[0] = isReverse ? 58 : 29;
				}				
				se.setIntDataMembers(intDataMembers);
			}
			count++;
		}		
//		for (Entry<String, StrandElement> entry: map.entrySet()) {
//			if (entry.getValue().isLong()) {				
//				long[] longDataMembers = new long[1];
//				if (isReverse) {
//					longDataMembers[0] = count * 2;
//				} else {
//					longDataMembers[0] = count;
//				}				
//				entry.getValue().setLongDataMembers(longDataMembers);
//			} else {
//				int[] intDataMembers = new int[1];
//				if (isReverse) {
//					intDataMembers[0] = count * 2;
//				} else {
//					intDataMembers[0] = count;
//				}
//				if (entry.getKey().equals("referenceNo")) {					
//					if (isReverse) {
//						intDataMembers[0] = 24;
//					} else {
//						intDataMembers[0] = 12;
//					}
//				} 
//				if (entry.getKey().equals("nonreferenceNo")) {
//					if (isReverse) {
//						intDataMembers[0] = 58;
//					} else {
//						intDataMembers[0] = 29;
//					}
//				}				
//				entry.getValue().setIntDataMembers(intDataMembers);
//			}
//			count++;
//		}		
	}

	@After
	public void tearDown() {
		record = null;
	}
	
	@Test
	public void testGetPositionString() {
		String expected = CHR1 + "\t10\tA\t";
		assertEquals(expected, record.getPositionString());
	}
	
	@Test
	public void testGetTotalBases() {
		assertEquals(45, record.getTotalBases(true, true));
		assertEquals(15, record.getTotalBases(true, false));
		assertEquals(30, record.getTotalBases(false, true));
//		assertEquals(168, record.getTotalBases(true, true));
//		assertEquals(56, record.getTotalBases(true, false));
//		assertEquals(112, record.getTotalBases(false, true));
	}
	
//	@Test
//	public void testGetTotalReads() {
//		assertEquals(237, record.getTotalReads(true, true));
//		assertEquals(79, record.getTotalReads(true, false));
//		assertEquals(158, record.getTotalReads(false, true));
//	}
	
	@Test
	public void testGetForwardElement() {
		for (StrandEnum e: StrandEnum.values()) {
			assertTrue(record.getForwardElement(e.toString()) > 0);
		}
	}
	
	@Test
	public void testGetReverseElement() {
		for (StrandEnum e: StrandEnum.values()) {
			assertTrue(record.getReverseElement(e.toString()) > 0);
		}
	}
	
	@Test
	public void testGetAltBase() {
		assertEquals('T', record.getAltBase(true, true).charValue());
		assertEquals('T', record.getAltBase(true, false).charValue());
		assertEquals('T', record.getAltBase(false, true).charValue());
	}
	
	@Test
	public void createBase() {
		Base b = record.createBase('A', true, true);
		
		assertEquals(1, b.getForwardCount());
		assertEquals(2, b.getReverseCount());
		assertEquals(3, b.getCount());
		assertEquals(21, b.getForTotalQual());
		assertEquals(42, b.getRevTotalQual());
		assertEquals(21.0, b.getForAvgBaseQual(), 0.1);
//		assertEquals(12, b.getForwardCount());
//		assertEquals(24, b.getReverseCount());
//		assertEquals(36, b.getCount());
//		assertEquals(26, b.getForTotalQual());
//		assertEquals(52, b.getRevTotalQual());
//		assertEquals(2.16, b.getForAvgBaseQual(), 0.1);
		assertEquals(21.0, b.getRevAvgBaseQual(), 0.1);
	}
	
	@Test
	public void testGetGenotypeEnum() {
		GenotypeEnum e = record.getGenotypeEnum();
		assertEquals("GT", e.toString());
		assertEquals("G/T", e.getDisplayString());
//		assertEquals("AT", e.toString());
//		assertEquals("A/T", e.getDisplayString());
	}
	
	@Test
	public void testInRequiredRegion() {
		assertTrue(record.inRequiredRegion(9, 12));
		assertFalse(record.inRequiredRegion(9, 9));
	}
	
	@Test
	public void testGetStrandBiasRecord() {
		assertNull(record.getStrandBiasRecord(20));
		int[] forMembers = {50};
		int[] revMembers = {10};
		record.getForwardElementMap().get("baseT").setIntDataMembers(forMembers);
		record.getReverseElementMap().get("baseT").setIntDataMembers(revMembers);
		assertNotNull(record.getStrandBiasRecord(20));
		assertNull(record.getStrandBiasRecord(60));
	}
	

}
