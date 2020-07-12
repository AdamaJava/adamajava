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
		assertEquals(true, rcInc.getMotifsForwardStrand().containsKey("ABCD"));
		
		rcInc.addMotif("ABCD", true, false);
		assertEquals(2, rcInc.getMotifsForwardStrand().get("ABCD").intValue());
		rcInc.addMotif("XYZ", true, false);
		assertEquals(2, rcInc.getMotifsForwardStrand().get("ABCD").intValue());
		assertEquals(1, rcInc.getMotifsForwardStrand().get("XYZ").intValue());
	}
	@Test
	public void ncludedRegionMappedReadsFS() {
		rcInc.addMotif("ABCD", true, true);
		assertEquals(true, rcInc.hasMotifs());
		assertEquals(true, rcInc.getMotifsForwardStrand().containsKey("ABCD"));
		
		rcInc.addMotif("ABCD", true, true);
		rcInc.addMotif("XYZ", true, true);
		assertEquals(2, rcInc.getMotifsForwardStrand().size());
		assertEquals(2, rcInc.getMotifsForwardStrand().get("ABCD").intValue());
		assertEquals(1, rcInc.getMotifsForwardStrand().get("XYZ").intValue());
	}
	
	@Test
	public void includedRegionUnmappedReadsRS() {
		rcInc.addMotif("ABCD", false, false);
		assertEquals(true, rcInc.hasMotifs());
		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("ABCD"));
		
		rcInc.addMotif("ABCD", false, false);
		rcInc.addMotif("XYZ", false, false);
		assertEquals(2, rcInc.getMotifsReverseStrand().size());
		assertEquals(2, rcInc.getMotifsReverseStrand().get("ABCD").intValue());
		assertEquals(1, rcInc.getMotifsReverseStrand().get("XYZ").intValue());
	}
	@Test
	public void includedRegionMappedReadsRS() {
		rcInc.addMotif("ABCD", false, true);
		assertEquals(true, rcInc.hasMotifs());
		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("ABCD"));
		
		rcInc.addMotif("ABCD", false, true);
		rcInc.addMotif("XYZ", false, true);
		assertEquals(2, rcInc.getMotifsReverseStrand().size());
		assertEquals(2, rcInc.getMotifsReverseStrand().get("ABCD").intValue());
		assertEquals(1, rcInc.getMotifsReverseStrand().get("XYZ").intValue());
	}
	
	@Test
	public void includedRegion() {
		rcInc.addMotif("ABCD", false, true);
		assertEquals(true, rcInc.hasMotifs());
		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("ABCD"));
		
		rcInc.addMotif("DCBA", true, true);
		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("ABCD"));
		assertEquals(true, rcInc.getMotifsForwardStrand().containsKey("DCBA"));
		
		rcInc.addMotif("123", false, false);
		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("ABCD"));
		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("123"));
		assertEquals(true, rcInc.getMotifsForwardStrand().containsKey("DCBA"));
		
		rcInc.addMotif("321", true, false);
		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("ABCD"));
		assertEquals(true, rcInc.getMotifsReverseStrand().containsKey("123"));
		assertEquals(true, rcInc.getMotifsForwardStrand().containsKey("DCBA"));
		assertEquals(true, rcInc.getMotifsForwardStrand().containsKey("321"));
	}
	
	@Test
	public void unmappedRegion() {
		rcUn.addMotif("ABCD", false, true);
		assertEquals(false, rcUn.hasMotifs());
		
		rcUn.addMotif("ABCD", true, true);
		assertEquals(false, rcUn.hasMotifs());
		
		rcUn.addMotif("ABCD", true, false);
		assertEquals(true, rcUn.hasMotifs());
		assertEquals(1, rcUn.getMotifsForwardStrand().get("ABCD").intValue());
		
		rcUn.addMotif("ABCD", false, false);
		assertEquals(true, rcUn.getMotifsForwardStrand().containsKey("ABCD"));
		assertEquals(true, rcUn.getMotifsReverseStrand().containsKey("ABCD"));
	}
	
	@Test
	public void otherRegion() {
		rcO.addMotif("ABCD", false, true);
		assertEquals(false, rcO.hasMotifs());
		
		rcO.addMotif("ABCD", true, true);
		assertEquals(false, rcO.hasMotifs());
		
		rcO.addMotif("ABCD", true, false);
		assertEquals(true, rcO.hasMotifs());
		assertEquals(true, rcO.getMotifsForwardStrand().containsKey("ABCD"));
		
		rcO.addMotif("ABCD", false, false);
		assertEquals(true, rcO.hasMotifs());
		assertEquals(true, rcO.getMotifsForwardStrand().containsKey("ABCD"));
		assertEquals(true, rcO.getMotifsReverseStrand().containsKey("ABCD"));
	}
	
}
