package org.qcmg.motif.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RegionTypeTest {
	
	@Test
	public void keepCounts() {
		assertTrue(RegionType.INCLUDES.keepCounts());
		assertFalse(RegionType.EXCLUDES.keepCounts());
		assertTrue(RegionType.GENOMIC.keepCounts());
		assertTrue(RegionType.UNMAPPED.keepCounts());
	}
	
	@Test
	public void includeMapped() {
		assertTrue(RegionType.INCLUDES.includeMapped());
		assertFalse(RegionType.EXCLUDES.includeMapped());
		assertFalse(RegionType.GENOMIC.includeMapped());
		assertFalse(RegionType.UNMAPPED.includeMapped());
	}
	
	@Test
	public void acceptsReads() {
		assertTrue(RegionType.INCLUDES.acceptRead(true));
		assertTrue(RegionType.INCLUDES.acceptRead(false));
		
		// OTHER only accepts unmapped reads
		assertTrue(RegionType.GENOMIC.acceptRead(false));
		
		// UNMAPPED only accepts unmapped reads
		assertTrue(RegionType.UNMAPPED.acceptRead(false));
	}
	
	@Test
	public void doesntAcceptReads() {
		assertFalse(RegionType.EXCLUDES.acceptRead(true));
		assertFalse(RegionType.EXCLUDES.acceptRead(false));
		
		// OTHER only accepts unmapped reads
		assertFalse(RegionType.GENOMIC.acceptRead(true));
		
		// UNMAPPED only accepts unmapped reads
		assertFalse(RegionType.UNMAPPED.acceptRead(true));
	}

}
