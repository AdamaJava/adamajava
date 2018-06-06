package org.qcmg.sig.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SigMetaTest {
	
	@Test
	public void isValid() {
		assertEquals(false, new SigMeta(null, 0, 0, 0, 0f).isValid());
		assertEquals(false, new SigMeta("", 0, 0, 0, 0f).isValid());
		assertEquals(false, new SigMeta("hello", 0, 0, 0, 0f).isValid());
		assertEquals(false, new SigMeta("", 1, 0, 0, 0f).isValid());
		assertEquals(false, new SigMeta("hello", -1, 0, 0, 0f).isValid());
		assertEquals(true, new SigMeta("hello", 1, 0, 0, 0f).isValid());
		assertEquals(true, new SigMeta("hello", 1, 1, 1, 0f).isValid());
	}
	
	@Test
	public void suitableForComparison() {
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta(null, 0, 0, 0, -1f),  new SigMeta(null, 0, 0, 0, -1f)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("", 0, 0, 0, -1f),  new SigMeta("", 0, 0, 0, -1f)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("", 1, 0, 0, -1f),  new SigMeta("", 1, 0, 0, -1f)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 0, 0, 0, -1f),  new SigMeta("asdf", 0, 0, 0, -1f)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 0, 0, -1f),  new SigMeta("asdf", 0, 0, 0, -1f)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 0, 0, 0, -1f),  new SigMeta("asdf", 1, 0, 0, -1f)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 2, 0, 0, -1f),  new SigMeta("asdf", 1, 0, 0, -1f)));
		assertEquals(true, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 0, 0, -1f),  new SigMeta("asdf", 1, 0, 0, -1f)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 0, 0, -1f),  new SigMeta("asdf", 1, 0, 1, -1f)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 10, 0, -1f),  new SigMeta("asdf", 1, 0, 0, -1f)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 10, 0, -1f),  new SigMeta("asdf", 1, 10, 10, -1f)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 10, 0, -1f),  new SigMeta("asdf", 1, 10, 1, -1f)));
		assertEquals(true, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 10, 0, -1f),  new SigMeta("asdf", 1, 10, 0, -1f)));
		assertEquals(true, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 10, 20, -1f),  new SigMeta("asdf", 1, 10, 20, -1f)));
	}
	
	
	@Test
	public void validComparisons() {
		assertEquals(true, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 10, 20, -1f),  new SigMeta("asdf", 1, 10, 20, -1f)));
		assertEquals(true, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 20, 22, -1f),  new SigMeta("asdf", 1, 20, 22, -1f)));
		assertEquals(true, SigMeta.suitableForComparison( new SigMeta("asdf", 1, -1, -1, 0.7f),  new SigMeta("asdf", 1, -1, -1, 0.7f)));
		assertEquals(true, SigMeta.suitableForComparison( new SigMeta("asdf", 1, -1, -1, 0.5123f),  new SigMeta("asdf", 1, -1, -1, 0.5123f)));
		assertEquals(true, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 10, 20, -1f),  new SigMeta("asdf", 1, -1, -1, 0.7f)));
		assertEquals(true, SigMeta.suitableForComparison( new SigMeta("asdf", 1, -1, -1, 0.7f), new SigMeta("asdf", 1, 10, 20, -1f)));
	}
	
	@Test
	public void invalidComparisons() {
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 10, 20, -1f),  new SigMeta("asdf", 1, 10, 21, -1f)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 25, 22, -1f),  new SigMeta("asdf", 1, 20, 22, -1f)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 1, -1, -1, 0.80002f),  new SigMeta("asdf", 1, -1, -1, 0.70001f)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 1, -1, -1, -0.5123f),  new SigMeta("asdf", 1, -1, -1, 0.5123f)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 10, 20, -1f),  new SigMeta("asdf", 1, -1, -1, -1f)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 1, -1, -1, -1f), new SigMeta("asdf", 1, 10, 20, -1f)));
	}

}
