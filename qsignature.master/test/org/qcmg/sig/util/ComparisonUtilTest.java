package org.qcmg.sig.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.sig.model.Comparison;

public class ComparisonUtilTest {
	
	static File F1 = new File("f1");
	static File F2 = new File("f2");
	
	double[] allAs = new double[] {1,0,0,0,20};
	double[] allCs = new double[] {0,1,0,0,20};
	double[] allGs = new double[] {0,0.0,1,0,20};
	double[] allTs = new double[] {0,0,0,1,20};
	
	double[] asAndCs = new double[] {0.48,0.52,0,0,40};
	double[] asAndGs = new double[] {0.5,0,0.5,0,40};
	double[] asAndTs = new double[] {0.4,0,0,0.6,40};
	
	double[] csAndGs = new double[] {0,0.4789,0.5123,0,35};
	
	
	@Test
	public void doesTheSnpMethodWorkSame() {
		ChrPosition cp = ChrPositionUtils.getChrPosition("1", 100, 100);
		Map<ChrPosition, double[]> file1Ratios = new HashMap<>();
		file1Ratios.put(cp,allAs);
		Map<ChrPosition, double[]> file2Ratios = new HashMap<>();
		file2Ratios.put(cp,allAs);
		
		Comparison comp = ComparisonUtil.compareRatiosUsingSnps(file1Ratios, file2Ratios, new File("blah1"), new File("blah2"), null);
		assertEquals(1, comp.getScore(), 0.0001);
		
		for (int i = 0 ; i < 1000000 ; i++) {
			cp = ChrPositionUtils.getChrPosition("2", i, i);
			int mod = i % 16;
			double [] arrayToUse = mod > 10 ? allCs : (mod > 5) ? asAndTs : csAndGs;
			file1Ratios.put(cp,arrayToUse);
			file2Ratios.put(cp,arrayToUse);
		}
		
		comp = ComparisonUtil.compareRatiosUsingSnps(file1Ratios, file2Ratios, new File("blah1"), new File("blah2"), null);
		assertEquals(1, comp.getScore(), 0.0001);
	}
	
	@Test
	public void doesTheSnpMethodWorkDifferent() {
		ChrPosition cp = ChrPositionUtils.getChrPosition("1", 100, 100);
		Map<ChrPosition, double[]> file1Ratios = new HashMap<>();
		file1Ratios.put(cp,allAs);
		Map<ChrPosition, double[]> file2Ratios = new HashMap<>();
		file2Ratios.put(cp,allCs);
		
		Comparison comp = ComparisonUtil.compareRatiosUsingSnps(file1Ratios, file2Ratios, new File("blah1"), new File("blah2"), null);
		assertEquals(0, comp.getScore(), 0.0001);
		
		for (int i = 0 ; i < 1000000 ; i++) {
			cp = ChrPositionUtils.getChrPosition("2", i, i);
			file1Ratios.put(cp,allAs);
			file2Ratios.put(cp,allCs);
		}
		
		comp = ComparisonUtil.compareRatiosUsingSnps(file1Ratios, file2Ratios, new File("blah1"), new File("blah2"), null);
		assertEquals(0, comp.getScore(), 0.0001);
	}
	
	@Test
	public void doesTheSnpMethodWorkMixed() {
		ChrPosition cp = null;
		Map<ChrPosition, double[]> file1Ratios = new HashMap<>();
		Map<ChrPosition, double[]> file2Ratios = new HashMap<>();
		
		for (int i = 0 ; i < 1000000 ; i++) {
			cp = ChrPositionUtils.getChrPosition("2", i, i);
			
			int mod = i % 16;
			double [] arrayToUse = mod > 7 ? asAndCs : csAndGs;
			
			file1Ratios.put(cp,arrayToUse);
			file2Ratios.put(cp,mod > 7 ? arrayToUse : allTs);
		}
		
		Comparison comp = ComparisonUtil.compareRatiosUsingSnps(file1Ratios, file2Ratios, new File("blah1"), new File("blah2"), null);
		assertEquals(0.5, comp.getScore(), 0.0001);
	}
	
	
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
		comps.add(new Comparison("f1", 0, "f2", 0, 1, 100));
		assertEquals("f1\t0.01[100]", ComparisonUtil.getComparisonsBody(comps));
		
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
		
		comps.add(new Comparison(F1, 0, F2, 0, 0, 0));
		assertEquals(false, ComparisonUtil.containsDodgyComparisons(comps, 0.0));
		
		comps.add(new Comparison(F1, 0, F2, 0, 10000, 10000));
		assertEquals(true, ComparisonUtil.containsDodgyComparisons(comps, 0.0));
		assertEquals(false, ComparisonUtil.containsDodgyComparisons(comps, 1));
	}

}
