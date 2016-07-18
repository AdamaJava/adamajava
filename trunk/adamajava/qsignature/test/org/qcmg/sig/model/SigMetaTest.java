package org.qcmg.sig.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SigMetaTest {
	
	@Test
	public void isValid() {
		assertEquals(false, new SigMeta(null, 0, 0, 0).isValid());
		assertEquals(false, new SigMeta("", 0, 0, 0).isValid());
		assertEquals(false, new SigMeta("hello", 0, 0, 0).isValid());
		assertEquals(false, new SigMeta("", 1, 0, 0).isValid());
		assertEquals(false, new SigMeta("hello", -1, 0, 0).isValid());
		assertEquals(true, new SigMeta("hello", 1, 0, 0).isValid());
		assertEquals(true, new SigMeta("hello", 1, 1, 1).isValid());
	}
	
	@Test
	public void suitableForComparison() {
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta(null, 0, 0, 0),  new SigMeta(null, 0, 0, 0)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("", 0, 0, 0),  new SigMeta("", 0, 0, 0)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("", 1, 0, 0),  new SigMeta("", 1, 0, 0)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 0, 0, 0),  new SigMeta("asdf", 0, 0, 0)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 0, 0),  new SigMeta("asdf", 0, 0, 0)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 0, 0, 0),  new SigMeta("asdf", 1, 0, 0)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 2, 0, 0),  new SigMeta("asdf", 1, 0, 0)));
		assertEquals(true, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 0, 0),  new SigMeta("asdf", 1, 0, 0)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 0, 0),  new SigMeta("asdf", 1, 0, 1)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 10, 0),  new SigMeta("asdf", 1, 0, 0)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 10, 0),  new SigMeta("asdf", 1, 10, 10)));
		assertEquals(false, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 10, 0),  new SigMeta("asdf", 1, 10, 1)));
		assertEquals(true, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 10, 0),  new SigMeta("asdf", 1, 10, 0)));
		assertEquals(true, SigMeta.suitableForComparison( new SigMeta("asdf", 1, 10, 20),  new SigMeta("asdf", 1, 10, 20)));
	}

}
