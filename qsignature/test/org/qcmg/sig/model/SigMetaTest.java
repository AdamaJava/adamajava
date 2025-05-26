package org.qcmg.sig.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class SigMetaTest {
	
	@Test
	public void isValid() {
        assertFalse(new SigMeta(null, 0, 0, 0f).isValid());
        assertFalse(new SigMeta("", 0, 0, 0f).isValid());
        assertFalse(new SigMeta("", 0, 0, 0f).isValid());
        assertTrue(new SigMeta("hello", 0, 0, 0f).isValid());
        assertTrue(new SigMeta("hello", 1, 1, 0f).isValid());
	}
	
	@Test
	public void suitableForComparison() {
        assertFalse(SigMeta.suitableForComparison(new SigMeta(null, 0, 0, -1f), new SigMeta(null, 0, 0, -1f)));
        assertFalse(SigMeta.suitableForComparison(new SigMeta("", 0, 0, -1f), new SigMeta("", 0, 0, -1f)));
        assertFalse(SigMeta.suitableForComparison(new SigMeta("", 0, 0, -1f), new SigMeta("", 0, 0, -1f)));
        assertTrue(SigMeta.suitableForComparison(new SigMeta("asdf", 0, 0, -1f), new SigMeta("asdf", 0, 0, -1f)));
        assertTrue(SigMeta.suitableForComparison(new SigMeta("asdf", 0, 0, -1f), new SigMeta("asdf", 0, 0, -1f)));
        assertFalse(SigMeta.suitableForComparison(new SigMeta("asdf", 0, 0, -1f), new SigMeta("asdf", 0, 1, -1f)));
        assertFalse(SigMeta.suitableForComparison(new SigMeta("asdf", 10, 0, -1f), new SigMeta("asdf", 0, 0, -1f)));
        assertFalse(SigMeta.suitableForComparison(new SigMeta("asdf", 10, 0, -1f), new SigMeta("asdf", 10, 10, -1f)));
        assertFalse(SigMeta.suitableForComparison(new SigMeta("asdf", 10, 0, -1f), new SigMeta("asdf", 10, 1, -1f)));
        assertTrue(SigMeta.suitableForComparison(new SigMeta("asdf", 10, 0, -1f), new SigMeta("asdf", 10, 0, -1f)));
        assertTrue(SigMeta.suitableForComparison(new SigMeta("asdf", 10, 20, -1f), new SigMeta("asdf", 10, 20, -1f)));
	}
	
	
	@Test
	public void validComparisons() {
        assertTrue(SigMeta.suitableForComparison(new SigMeta("asdf", 10, 20, -1f), new SigMeta("asdf", 10, 20, -1f)));
        assertTrue(SigMeta.suitableForComparison(new SigMeta("asdf", 20, 22, -1f), new SigMeta("asdf", 20, 22, -1f)));
        assertTrue(SigMeta.suitableForComparison(new SigMeta("asdf", -1, -1, 0.7f), new SigMeta("asdf", -1, -1, 0.7f)));
        assertTrue(SigMeta.suitableForComparison(new SigMeta("asdf", -1, -1, 0.5123f), new SigMeta("asdf", -1, -1, 0.5123f)));
        assertTrue(SigMeta.suitableForComparison(new SigMeta("asdf", 10, 20, -1f), new SigMeta("asdf", -1, -1, 0.7f)));
        assertTrue(SigMeta.suitableForComparison(new SigMeta("asdf", -1, -1, 0.7f), new SigMeta("asdf", 10, 20, -1f)));
	}
	
	@Test
	public void invalidComparisons() {
        assertFalse(SigMeta.suitableForComparison(new SigMeta("asdf", 10, 20, -1f), new SigMeta("asdf", 10, 21, -1f)));
        assertFalse(SigMeta.suitableForComparison(new SigMeta("asdf", 25, 22, -1f), new SigMeta("asdf", 20, 22, -1f)));
        assertFalse(SigMeta.suitableForComparison(new SigMeta("asdf", -1, -1, 0.80002f), new SigMeta("asdf", -1, -1, 0.70001f)));
        assertFalse(SigMeta.suitableForComparison(new SigMeta("asdf", -1, -1, -0.5123f), new SigMeta("asdf", -1, -1, 0.5123f)));
        assertFalse(SigMeta.suitableForComparison(new SigMeta("asdf", 10, 20, -1f), new SigMeta("asdf", -1, -1, -1f)));
        assertFalse(SigMeta.suitableForComparison(new SigMeta("asdf", -1, -1, -1f), new SigMeta("asdf", 10, 20, -1f)));
	}

}
