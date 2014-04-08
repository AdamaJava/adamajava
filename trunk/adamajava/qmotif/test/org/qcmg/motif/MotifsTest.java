package org.qcmg.motif;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class MotifsTest {
	
	@Test(expected=IllegalArgumentException.class)
	public void nullArgument() {
		new Motifs(true, null);
	}
	@Test
	public void emptyArgument() {
		Motifs m = new Motifs(true, "");
		assertEquals(0, m.getMotifsSize());
	}
	
	@Test
	public void singleArgumentNoRevComp() {
		Motifs m = new Motifs(false, "hello");
		assertEquals(1, m.getMotifsSize());
	}
	@Test
	public void singleArgumentWithRevComp() {
		Motifs m = new Motifs(true, "hello");
		assertEquals(2, m.getMotifsSize());
		List<String> motifs = m.getMotifs();
		assertEquals("hello", motifs.get(0));
		assertEquals("olleh", motifs.get(1));
	}
	@Test
	public void singleArgumentWithRevCompAndRealBases() {
		Motifs m = new Motifs(true, "ACGT");	// rev comp of this is the same as the original
		assertEquals(1, m.getMotifsSize());
		List<String> motifs = m.getMotifs();
		assertEquals("ACGT", motifs.get(0));
	}
	@Test
	public void singleArgumentWithRevCompAndRealBases2() {
		Motifs m = new Motifs(true, "AAAGGTTT");
		assertEquals(2, m.getMotifsSize());
		List<String> motifs = m.getMotifs();
		assertEquals("AAAGGTTT", motifs.get(0));
		assertEquals("AAACCTTT", motifs.get(1));
	}

}
