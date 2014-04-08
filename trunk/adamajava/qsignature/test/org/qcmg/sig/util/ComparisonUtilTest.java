package org.qcmg.sig.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.sig.model.Comparison;

public class ComparisonUtilTest {
	
	static File F1 = new File("f1");
	static File F2 = new File("f2");
	
	@Test
	public void testGetComparisonsBody() {
		try {
			ComparisonUtil.getComparisonsBody(null);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		try {
			ComparisonUtil.getComparisonsBody(new ArrayList<Comparison>());
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		
		
		List<Comparison> comps = new ArrayList<>();
		comps.add(new Comparison(F1, 0, F2, 0, 1, 100, 0));
		assertEquals("f1\t0.01[100,0]", ComparisonUtil.getComparisonsBody(comps));
		
	}
	
	@Test
	public void testContainsDodgyComparisons() {
		try {
			ComparisonUtil.containsDodgyComparisons(null, 0.0);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		
		List<Comparison> comps = new ArrayList<>();
		try {
			ComparisonUtil.containsDodgyComparisons(comps, 0.0);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		
		comps.add(new Comparison(F1, 0, F2, 0, 0, 0, 0));
		assertEquals(false, ComparisonUtil.containsDodgyComparisons(comps, 0.0));
		
		comps.add(new Comparison(F1, 0, F2, 0, 10000, 10000, 100000));
		assertEquals(true, ComparisonUtil.containsDodgyComparisons(comps, 0.0));
		assertEquals(false, ComparisonUtil.containsDodgyComparisons(comps, 1));
	}

}
