package org.qcmg.qmule.snppicker;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.tab.TabbedRecord;

public class CompareSnpsTest {
	
	@Test
	public void testIsStopNonSynonymous() {
		try {
			CompareSnps.isStopNonSynonymous(null, -1);
			Assert.fail("should have thrown a wobbly");
		} catch (IllegalArgumentException e) {}
		
		TabbedRecord tr = new TabbedRecord();
		try {		
			CompareSnps.isStopNonSynonymous(tr, -1);
			Assert.fail("should have thrown a wobbly");
		} catch (IllegalArgumentException e) {}
		
		tr.setData("");
		Assert.assertFalse(CompareSnps.isStopNonSynonymous(tr, -1));
		tr.setData("1\t2\t3\t4\t5");
		Assert.assertFalse(CompareSnps.isStopNonSynonymous(tr, -1));
		tr.setData("1\t2\t3\t4\t5\tSTOP\t7\t8");
		Assert.assertFalse(CompareSnps.isStopNonSynonymous(tr, -1));
		Assert.assertTrue(CompareSnps.isStopNonSynonymous(tr, 5));
		tr.setData("1\t2\t3\t4\t5\t6\t7\t8\tNON_SYNONYMOUS");
		Assert.assertTrue(CompareSnps.isStopNonSynonymous(tr, -1));
		Assert.assertFalse(CompareSnps.isStopNonSynonymous(tr, 5));
		
	}
	
	@Ignore
	public void testIsClassAB() {
		try {
			CompareSnps.isClassAB(null, -1);
			Assert.fail("should have thrown a wobbly");
		} catch (IllegalArgumentException e) {}
		
		TabbedRecord tr = new TabbedRecord();
		try {		
			CompareSnps.isClassAB(tr, -1);
			Assert.fail("should have thrown a wobbly");
		} catch (IllegalArgumentException e) {}
		
		tr.setData("");
		Assert.assertFalse(CompareSnps.isClassAB(tr, -1));
		tr.setData("1\t2\t3\t4\t5");
		Assert.assertFalse(CompareSnps.isClassAB(tr, -1));
		tr.setData("1\t2\t3\t4\t5\tSTOP\t7\t8");
		Assert.assertFalse(CompareSnps.isClassAB(tr, -1));
		Assert.assertFalse(CompareSnps.isClassAB(tr, 5));
		tr.setData("1\t2\t3\t4\t5\t6\t7\t8\tNON_SYNONYMOUS");
		Assert.assertFalse(CompareSnps.isClassAB(tr, -1));
		Assert.assertFalse(CompareSnps.isClassAB(tr, 5));
		
		tr.setData("1\t2\t3\t4\t5\t6\t7\t8\t--");
		Assert.assertTrue(CompareSnps.isClassAB(tr, -1));
		tr.setData("1\t2\t3\t" + SnpUtils.LESS_THAN_3_READS_NORMAL + "\t5\t6\t7\t8\t--");
		Assert.assertTrue(CompareSnps.isClassAB(tr, 3));
		Assert.assertFalse(CompareSnps.isClassAB(tr, 4));
		
	}

}
