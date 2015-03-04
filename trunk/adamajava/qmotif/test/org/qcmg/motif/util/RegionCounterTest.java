package org.qcmg.motif.util;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class RegionCounterTest {
	
	private RegionCounter rcEx;
	private RegionCounter rcInc;
	private RegionCounter rcUn;
	private RegionCounter rcO;

	@Before
	public void setup() {
		rcEx = new RegionCounter(RegionType.EXCLUDES);
		rcInc = new RegionCounter(RegionType.INCLUDES);
		rcUn = new RegionCounter(RegionType.UNMAPPED);
		rcO = new RegionCounter(RegionType.GENOMIC);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void nullString() {
		rcInc.addMotif(null, true, true);
	}
	@Test(expected=IllegalArgumentException.class)
	public void emptyString() {
		rcInc.addMotif("", true, true);
	}
	
	@Test
	public void excludedRegion() {
		rcEx.addMotif("ABCD", true, true);
		assertEquals(false, rcEx.hasMotifs());
		rcEx.addMotif("ABCD", false, true);
		assertEquals(false, rcEx.hasMotifs());
		rcEx.addMotif("ABCD", true, false);
		assertEquals(false, rcEx.hasMotifs());
		rcEx.addMotif("ABCD", false, false);
		assertEquals(false, rcEx.hasMotifs());
	}
	
	@Test
	public void includedRegionUnmappedReadsFS() {
		rcInc.addMotif("ABCD", true, false);
		assertEquals(true, rcInc.hasMotifs());
//		assertEquals(true, rcInc.getMotifsForwardStrand().containsKey("ABCD"));
		assertEquals("ABCD", rcInc.getMotifsForwardStrand());
		
		rcInc.addMotif("ABCD", true, false);
//		assertEquals(2, rcInc.getMotifsForwardStrand().get("ABCD").intValue());
		assertEquals("ABCD:ABCD", rcInc.getMotifsForwardStrand());
		rcInc.addMotif("XYZ", true, false);
//		assertEquals(2, rcInc.getMotifsForwardStrand().size());
//		assertEquals(2, rcInc.getMotifsForwardStrand().get("ABCD").intValue());
//		assertEquals(1, rcInc.getMotifsForwardStrand().get("XYZ").intValue());
		assertEquals("ABCD:ABCD:XYZ", rcInc.getMotifsForwardStrand());
	}
//	@Test
//	public void includedRegionUnmappedReadsFS() {
//		rcInc.addMotif("ABCD", true, false);
//		assertEquals(true, rcInc.hasMotifs());
//		assertEquals(true, rcInc.getMotifsForwardStrand().containsKey("ABCD"));
////		assertEquals("ABCD", rcInc.getMotifsForwardStrand());
//		
//		rcInc.addMotif("ABCD", true, false);
//		assertEquals(2, rcInc.getMotifsForwardStrand().get("ABCD").intValue());
////		assertEquals("ABCD:ABCD", rcInc.getMotifsForwardStrand());
//		rcInc.addMotif("XYZ", true, false);
//		assertEquals(2, rcInc.getMotifsForwardStrand().size());
//		assertEquals(2, rcInc.getMotifsForwardStrand().get("ABCD").intValue());
//		assertEquals(1, rcInc.getMotifsForwardStrand().get("XYZ").intValue());
////		assertEquals("ABCD:ABCD:XYZ", rcInc.getMotifsForwardStrand());
//	}
	@Test
	public void ncludedRegionMappedReadsFS() {
		rcInc.addMotif("ABCD", true, true);
		assertEquals(true, rcInc.hasMotifs());
//		assertEquals(true, rcInc.getMotifsForwardStrand().containsKey("ABCD"));
		assertEquals("ABCD", rcInc.getMotifsForwardStrand());
		assertEquals(null, rcInc.getMotifsReverseStrand());
		
		rcInc.addMotif("ABCD", true, true);
		assertEquals("ABCD:ABCD", rcInc.getMotifsForwardStrand());
		rcInc.addMotif("XYZ", true, true);
		assertEquals("ABCD:ABCD:XYZ", rcInc.getMotifsForwardStrand());
//		assertEquals(2, rcInc.getMotifsForwardStrand().size());
//		assertEquals(2, rcInc.getMotifsForwardStrand().get("ABCD").intValue());
//		assertEquals(1, rcInc.getMotifsForwardStrand().get("XYZ").intValue());
	}
//	@Test
//	public void ncludedRegionMappedReadsFS() {
//		rcInc.addMotif("ABCD", true, true);
//		assertEquals(true, rcInc.hasMotifs());
//		assertEquals(true, rcInc.getMotifsForwardStrand().containsKey("ABCD"));
////		assertEquals("ABCD", rcInc.getMotifsForwardStrand());
//		assertEquals(null, rcInc.getMotifsReverseStrand());
//		
//		rcInc.addMotif("ABCD", true, true);
////		assertEquals("ABCD:ABCD", rcInc.getMotifsForwardStrand());
//		rcInc.addMotif("XYZ", true, true);
////		assertEquals("ABCD:ABCD:XYZ", rcInc.getMotifsForwardStrand());
//		assertEquals(2, rcInc.getMotifsForwardStrand().size());
//		assertEquals(2, rcInc.getMotifsForwardStrand().get("ABCD").intValue());
//		assertEquals(1, rcInc.getMotifsForwardStrand().get("XYZ").intValue());
//	}
	
	@Test
	public void includedRegionUnmappedReadsRS() {
		rcInc.addMotif("ABCD", false, false);
		assertEquals(true, rcInc.hasMotifs());
		assertEquals(null, rcInc.getMotifsForwardStrand());
//		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("ABCD"));
		assertEquals("ABCD", rcInc.getMotifsReverseStrand());
		
		rcInc.addMotif("ABCD", false, false);
		assertEquals("ABCD:ABCD", rcInc.getMotifsReverseStrand());
		rcInc.addMotif("XYZ", false, false);
		assertEquals("ABCD:ABCD:XYZ", rcInc.getMotifsReverseStrand());
//		assertEquals(2, rcInc.getMotifsReverseStrand().size());
//		assertEquals(2, rcInc.getMotifsReverseStrand().get("ABCD").intValue());
//		assertEquals(1, rcInc.getMotifsReverseStrand().get("XYZ").intValue());
	}
//	@Test
//	public void includedRegionUnmappedReadsRS() {
//		rcInc.addMotif("ABCD", false, false);
//		assertEquals(true, rcInc.hasMotifs());
//		assertEquals(null, rcInc.getMotifsForwardStrand());
//		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("ABCD"));
////		assertEquals("ABCD", rcInc.getMotifsReverseStrand());
//		
//		rcInc.addMotif("ABCD", false, false);
////		assertEquals("ABCD:ABCD", rcInc.getMotifsReverseStrand());
//		rcInc.addMotif("XYZ", false, false);
////		assertEquals("ABCD:ABCD:XYZ", rcInc.getMotifsReverseStrand());
//		assertEquals(2, rcInc.getMotifsReverseStrand().size());
//		assertEquals(2, rcInc.getMotifsReverseStrand().get("ABCD").intValue());
//		assertEquals(1, rcInc.getMotifsReverseStrand().get("XYZ").intValue());
//	}
	@Test
	public void includedRegionMappedReadsRS() {
		rcInc.addMotif("ABCD", false, true);
		assertEquals(true, rcInc.hasMotifs());
		assertEquals(null, rcInc.getMotifsForwardStrand());
		assertEquals("ABCD", rcInc.getMotifsReverseStrand());
//		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("ABCD"));
		
		rcInc.addMotif("ABCD", false, true);
		assertEquals("ABCD:ABCD", rcInc.getMotifsReverseStrand());
		rcInc.addMotif("XYZ", false, true);
		assertEquals("ABCD:ABCD:XYZ", rcInc.getMotifsReverseStrand());
//		assertEquals(2, rcInc.getMotifsReverseStrand().size());
//		assertEquals(2, rcInc.getMotifsReverseStrand().get("ABCD").intValue());
//		assertEquals(1, rcInc.getMotifsReverseStrand().get("XYZ").intValue());
	}
//	@Test
//	public void includedRegionMappedReadsRS() {
//		rcInc.addMotif("ABCD", false, true);
//		assertEquals(true, rcInc.hasMotifs());
//		assertEquals(null, rcInc.getMotifsForwardStrand());
////		assertEquals("ABCD", rcInc.getMotifsReverseStrand());
//		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("ABCD"));
//		
//		rcInc.addMotif("ABCD", false, true);
////		assertEquals("ABCD:ABCD", rcInc.getMotifsReverseStrand());
//		rcInc.addMotif("XYZ", false, true);
////		assertEquals("ABCD:ABCD:XYZ", rcInc.getMotifsReverseStrand());
//		assertEquals(2, rcInc.getMotifsReverseStrand().size());
//		assertEquals(2, rcInc.getMotifsReverseStrand().get("ABCD").intValue());
//		assertEquals(1, rcInc.getMotifsReverseStrand().get("XYZ").intValue());
//	}
	
	@Test
	public void includedRegion() {
		rcInc.addMotif("ABCD", false, true);
		assertEquals(true, rcInc.hasMotifs());
		assertEquals(null, rcInc.getMotifsForwardStrand());
//		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("ABCD"));
		assertEquals("ABCD", rcInc.getMotifsReverseStrand());
		
		rcInc.addMotif("DCBA", true, true);
//		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("ABCD"));
//		assertEquals(true, rcInc.getMotifsForwardStrand().containsKey("DCBA"));
		assertEquals("DCBA", rcInc.getMotifsForwardStrand());
		assertEquals("ABCD", rcInc.getMotifsReverseStrand());
		
		rcInc.addMotif("123", false, false);
//		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("ABCD"));
//		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("123"));
//		assertEquals(true, rcInc.getMotifsForwardStrand().containsKey("DCBA"));
		assertEquals("DCBA", rcInc.getMotifsForwardStrand());
		assertEquals("ABCD:123", rcInc.getMotifsReverseStrand());
		
		rcInc.addMotif("321", true, false);
//		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("ABCD"));
//		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("123"));
//		assertEquals(true, rcInc.getMotifsForwardStrand().containsKey("DCBA"));
//		assertEquals(true, rcInc.getMotifsForwardStrand().containsKey("321"));
		assertEquals("DCBA:321", rcInc.getMotifsForwardStrand());
		assertEquals("ABCD:123", rcInc.getMotifsReverseStrand());
	}
//	@Test
//	public void includedRegion() {
//		rcInc.addMotif("ABCD", false, true);
//		assertEquals(true, rcInc.hasMotifs());
//		assertEquals(null, rcInc.getMotifsForwardStrand());
//		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("ABCD"));
////		assertEquals("ABCD", rcInc.getMotifsReverseStrand());
//		
//		rcInc.addMotif("DCBA", true, true);
//		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("ABCD"));
//		assertEquals(true, rcInc.getMotifsForwardStrand().containsKey("DCBA"));
////		assertEquals("DCBA", rcInc.getMotifsForwardStrand());
////		assertEquals("ABCD", rcInc.getMotifsReverseStrand());
//		
//		rcInc.addMotif("123", false, false);
//		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("ABCD"));
//		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("123"));
//		assertEquals(true, rcInc.getMotifsForwardStrand().containsKey("DCBA"));
////		assertEquals("DCBA", rcInc.getMotifsForwardStrand());
////		assertEquals("ABCD:123", rcInc.getMotifsReverseStrand());
//		
//		rcInc.addMotif("321", true, false);
//		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("ABCD"));
//		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("123"));
//		assertEquals(true, rcInc.getMotifsForwardStrand().containsKey("DCBA"));
//		assertEquals(true, rcInc.getMotifsForwardStrand().containsKey("321"));
////		assertEquals("DCBA:321", rcInc.getMotifsForwardStrand());
////		assertEquals("ABCD:123", rcInc.getMotifsReverseStrand());
//	}
	
	@Test
	public void unmappedRegion() {
		rcUn.addMotif("ABCD", false, true);
		assertEquals(false, rcUn.hasMotifs());
		
		rcUn.addMotif("ABCD", true, true);
		assertEquals(false, rcUn.hasMotifs());
		
		rcUn.addMotif("ABCD", true, false);
		assertEquals(true, rcUn.hasMotifs());
		assertEquals("ABCD", rcUn.getMotifsForwardStrand());
//		assertEquals(true, rcUn.getMotifsForwardStrand().containsKey("ABCD"));
		
		rcUn.addMotif("ABCD", false, false);
//		assertEquals(true, rcUn.hasMotifs());
//		assertEquals(true, rcUn.getMotifsForwardStrand().containsKey("ABCD"));
//		assertEquals(true, rcUn.getMotifsReverseStrand().containsKey("ABCD"));
		assertEquals("ABCD", rcUn.getMotifsForwardStrand());
		assertEquals("ABCD", rcUn.getMotifsReverseStrand());
	}
//	@Test
//	public void unmappedRegion() {
//		rcUn.addMotif("ABCD", false, true);
//		assertEquals(false, rcUn.hasMotifs());
//		
//		rcUn.addMotif("ABCD", true, true);
//		assertEquals(false, rcUn.hasMotifs());
//		
//		rcUn.addMotif("ABCD", true, false);
//		assertEquals(true, rcUn.hasMotifs());
////		assertEquals("ABCD", rcUn.getMotifsForwardStrand());
//		assertEquals(true, rcUn.getMotifsForwardStrand().containsKey("ABCD"));
//		
//		rcUn.addMotif("ABCD", false, false);
//		assertEquals(true, rcUn.hasMotifs());
//		assertEquals(true, rcUn.getMotifsForwardStrand().containsKey("ABCD"));
//		assertEquals(true, rcUn.getMotifsReverseStrand().containsKey("ABCD"));
////		assertEquals("ABCD", rcUn.getMotifsForwardStrand());
////		assertEquals("ABCD", rcUn.getMotifsReverseStrand());
//	}
	
	@Test
	public void otherRegion() {
		rcO.addMotif("ABCD", false, true);
		assertEquals(false, rcO.hasMotifs());
		
		rcO.addMotif("ABCD", true, true);
		assertEquals(false, rcO.hasMotifs());
		
		rcO.addMotif("ABCD", true, false);
		assertEquals(true, rcO.hasMotifs());
//		assertEquals(true, rcO.getMotifsForwardStrand().containsKey("ABCD"));
		assertEquals("ABCD", rcO.getMotifsForwardStrand());
		
		rcO.addMotif("ABCD", false, false);
		assertEquals(true, rcO.hasMotifs());
//		assertEquals(true, rcO.getMotifsForwardStrand().containsKey("ABCD"));
//		assertEquals(true, rcO.getMotifsReverseStrand().containsKey("ABCD"));
		assertEquals("ABCD", rcO.getMotifsForwardStrand());
		assertEquals("ABCD", rcO.getMotifsReverseStrand());
		
	}
//	@Test
//	public void otherRegion() {
//		rcO.addMotif("ABCD", false, true);
//		assertEquals(false, rcO.hasMotifs());
//		
//		rcO.addMotif("ABCD", true, true);
//		assertEquals(false, rcO.hasMotifs());
//		
//		rcO.addMotif("ABCD", true, false);
//		assertEquals(true, rcO.hasMotifs());
//		assertEquals(true, rcO.getMotifsForwardStrand().containsKey("ABCD"));
////		assertEquals("ABCD", rcO.getMotifsForwardStrand());
//		
//		rcO.addMotif("ABCD", false, false);
//		assertEquals(true, rcO.hasMotifs());
//		assertEquals(true, rcO.getMotifsForwardStrand().containsKey("ABCD"));
//		assertEquals(true, rcO.getMotifsReverseStrand().containsKey("ABCD"));
////		assertEquals("ABCD", rcO.getMotifsForwardStrand());
////		assertEquals("ABCD", rcO.getMotifsReverseStrand());
//		
//	}
	
}
