package org.qcmg.qsv.splitread;

import static org.junit.Assert.*;

import org.junit.Test;
import org.qcmg.qsv.util.QSVConstants;

public class StructuralVariantTest {
	
	
	@Test
	public void testSwap() {
		StructuralVariant sv = new StructuralVariant("chr10", "chr12", 89700200, 89712341, "1");
		assertEquals(89700200, sv.getLeftBreakpoint().intValue());
		assertEquals("chr10", sv.getLeftReference());
		sv.swap();
		assertEquals(89712341, sv.getLeftBreakpoint().intValue());
		assertEquals("chr12", sv.getLeftReference());		
	}
	
	@Test
	public void testSplitReadEquals() {
		StructuralVariant sv = new StructuralVariant("chr10", "chr12", 89700200, 89712341, "1");
		StructuralVariant sv2 = new StructuralVariant("chr10", "chr12", 89700200, 89712341, "1");
		
		assertTrue(sv.splitReadEquals(sv2, QSVConstants.LEVEL_MID, 10));
		
		sv2 = new StructuralVariant("chr10", "chr12", 89700200, 89712341, "2");
		assertFalse(sv.splitReadEquals(sv2,  QSVConstants.LEVEL_MID,10));
		
		sv2 = new StructuralVariant("chr10", "chr13", 89700200, 89712341, "1");
		assertFalse(sv.splitReadEquals(sv2,  QSVConstants.LEVEL_MID,10));
		
		sv2 = new StructuralVariant("chr10", "chr12", 89700200, 89712352, "1");
		assertFalse(sv.splitReadEquals(sv2, QSVConstants.LEVEL_MID, 10));
		
		sv2 = new StructuralVariant("chr10", "chr12", 89700211, 89712341, "1");
		assertFalse(sv.splitReadEquals(sv2,  QSVConstants.LEVEL_MID,10));
	}
	


}
