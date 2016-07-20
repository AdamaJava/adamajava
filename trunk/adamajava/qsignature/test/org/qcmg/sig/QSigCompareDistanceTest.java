package org.qcmg.sig;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.sig.model.Comparison;

public class QSigCompareDistanceTest {
	
	static final File F1 = new File("f1");
	static final File F2 = new File("f2");
	
	@Test
	public void testCompareRatios() {
		try {
			QSigCompareDistance.compareRatios(null, null, (File)null, (File)null);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		try {
			QSigCompareDistance.compareRatios(new HashMap<ChrPosition, double[]>(), new HashMap<ChrPosition, double[]>(), null, null);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		
		Comparison c = QSigCompareDistance.compareRatios(new HashMap<ChrPosition, double[]>(), new HashMap<ChrPosition, double[]>(), F1, F2);
		testEmptyComparison(c, 0 , 0);
		
		ChrPosition cp1 = ChrPointPosition.valueOf("1",1);
		ChrPosition cp2 = ChrPointPosition.valueOf("2",2);
		
		double[] ratios1 = new double[] {0.0, 10.0, 15.0, 0.0, 0.0};
		double[] ratios2 = new double[] {8.0, 0.0, 0.0, 0.0, 0.0};
		
		Map<ChrPosition, double[]> map1 = new HashMap<>();
		map1.put(cp1, ratios1);
		Map<ChrPosition, double[]> map2 = new HashMap<>();
		map2.put(cp2, ratios2);
		
		c = QSigCompareDistance.compareRatios(map1, map2, F1, F2);
		testEmptyComparison(c, 1,1);
	}
	
	@Test
	public void testCompareRatiosSameFile() {
		ChrPointPosition cp1 = ChrPointPosition.valueOf("1",1);
		ChrPointPosition cp2 = ChrPointPosition.valueOf("2",2);
		
		double[] ratios1 = new double[] {0.0, 10.0, 15.0, 0.0, 0.0};
		double[] ratios2 = new double[] {0.0, 0.0, 15.0, 0.0, 0.0};
		
		Map<ChrPosition, double[]> map1 = new HashMap<>();
		map1.put(cp1, ratios1);
		map1.put(cp2, ratios2);
		Map<ChrPosition, double[]> map2 = new HashMap<>();
		map2.put(cp1, ratios1);
		map2.put(cp2, ratios2);
		
		Comparison c = QSigCompareDistance.compareRatios(map1, map2, F1, F1);
		assertEquals(F1.getAbsolutePath(), c.getMain());
		assertEquals(F1.getAbsolutePath(), c.getTest());
		assertEquals(map1.size(), c.getOverlapCoverage());
	}
	
	@Test
	public void testCompareRatios2() {
		ChrPointPosition cp1 =  ChrPointPosition.valueOf("1",1);
		ChrPointPosition cp2 =  ChrPointPosition.valueOf("2",2);
		ChrPointPosition cp3 =  ChrPointPosition.valueOf("3",3);
		ChrPointPosition cp4 =  ChrPointPosition.valueOf("4",4);
		
		double[] ratios1 = new double[] {0.0, 10.0, 15.0, 0.0, 0.0};
		double[] ratios2 = new double[] {0.0, 20.0, 0.0, 0.0, 0.0};
		
		Map<ChrPosition, double[]> map1 = new HashMap<>();
		map1.put(cp1, ratios1);
		map1.put(cp2, ratios1);
		Map<ChrPosition, double[]> map2 = new HashMap<>();
		map2.put(cp1, ratios1);
		map2.put(cp2, ratios1);
		
		Comparison c = QSigCompareDistance.compareRatios(map1, map2, F1, F2);
		
		assertEquals(0.0, c.getTotalScore(), 0.0001);
		assertEquals(2 * 4, c.getNumberOfCalculations());
		assertEquals(2, c.getOverlapCoverage());
		assertEquals(2, c.getMainCoverage());
		assertEquals(2, c.getTestCoverage());
		assertEquals(F1.getAbsolutePath(), c.getMain());
		assertEquals(F2.getAbsolutePath(), c.getTest());
		
		map1.put(cp3, ratios2);
		map1.put(cp4, ratios1);
		map2.put(cp3, ratios1);
		map2.put(cp4, ratios2);
		
		c = QSigCompareDistance.compareRatios(map1, map2, F1, F2);
		
		// sqr((20-10)^2 + 15^2) * 2
		assertEquals(36.0555, c.getTotalScore(), 0.0001);
		assertEquals(4 * 4, c.getNumberOfCalculations());
		assertEquals(4, c.getOverlapCoverage());
		assertEquals(4, c.getMainCoverage());
		assertEquals(4, c.getTestCoverage());
		assertEquals(F1.getAbsolutePath(), c.getMain());
		assertEquals(F2.getAbsolutePath(), c.getTest());
		
		// now add in a positionsOfInterest collection
		Map<ChrPosition, ChrPosition> positionsOfInterest = new HashMap<>();
		
		c = QSigCompareDistance.compareRatios(map1, map2, F1, F2, positionsOfInterest);
		// as before
		// sqr((20-10)^2 + 15^2) * 2
		assertEquals(36.0555, c.getTotalScore(), 0.0001);
		assertEquals(4 * 4, c.getNumberOfCalculations());
		assertEquals(4, c.getOverlapCoverage());
		assertEquals(4, c.getMainCoverage());
		assertEquals(4, c.getTestCoverage());
		assertEquals(F1.getAbsolutePath(), c.getMain());
		assertEquals(F2.getAbsolutePath(), c.getTest());
		
		// add in positions of interest
		positionsOfInterest.put(cp1, cp1);
		c = QSigCompareDistance.compareRatios(map1, map2, F1, F2, positionsOfInterest);
		assertEquals(0.0, c.getTotalScore(), 0.0001);
		assertEquals(1, c.getOverlapCoverage());
		
		// add in positions of interest
		positionsOfInterest.put(cp2, cp2);
		c = QSigCompareDistance.compareRatios(map1, map2, F1, F2, positionsOfInterest);
		assertEquals(0.0, c.getTotalScore(), 0.0001);
		assertEquals(2, c.getOverlapCoverage());
		
		// add in positions of interest
		positionsOfInterest.put(cp3, cp3);
		c = QSigCompareDistance.compareRatios(map1, map2, F1, F2, positionsOfInterest);
		assertEquals(18.0277, c.getTotalScore(), 0.0001);
		assertEquals(3, c.getOverlapCoverage());
		
		// add in positions of interest
		positionsOfInterest.put(cp4, cp4);
		c = QSigCompareDistance.compareRatios(map1, map2, F1, F2, positionsOfInterest);
		assertEquals(36.0555, c.getTotalScore(), 0.0001);
		assertEquals(4, c.getOverlapCoverage());
	}
	
	private void testEmptyComparison(Comparison c, int mainCov, int testCov) {
		assertEquals(0.0, c.getTotalScore(), 0.0001);
		assertEquals(0, c.getNumberOfCalculations());
		assertEquals(0, c.getOverlapCoverage());
		assertEquals(mainCov, c.getMainCoverage());
		assertEquals(testCov, c.getTestCoverage());
		assertEquals(F1.getAbsolutePath(), c.getMain());
		assertEquals(F2.getAbsolutePath(), c.getTest());
	}
	
	@Test
	public void testDoComparison() {
		ChrPosition cp = new ChrRangePosition("1", 1, 1);
		
		/*
		 * 	FRAC 0
			FRAC 0.944444444444444
			FRAC 0
			FRAC 0.0555555555555556
			FRAC 0
			FRAC 0.95
			FRAC 0
			FRAC 0.05
			dist 0
		 */
		
		double[] x = new double[] {QSigCompare.getDiscretisedValue(0.0),
				QSigCompare.getDiscretisedValue(0.944444444444444),
				QSigCompare.getDiscretisedValue(0.0),
				QSigCompare.getDiscretisedValue(0.0555555555555556)};
		double[] y = new double[] {QSigCompare.getDiscretisedValue(0.0),
				QSigCompare.getDiscretisedValue(0.95), 
				QSigCompare.getDiscretisedValue(0.0), 
				QSigCompare.getDiscretisedValue(0.05)};
		
		Map<ChrPosition, double[]> mapX = new HashMap<ChrPosition, double[]>();
		Map<ChrPosition, double[]> mapY = new HashMap<ChrPosition, double[]>();
		mapX.put(cp, x);
		mapY.put(cp, y);
		
		double [] results = QSigCompareDistance.compareRatios(mapX, mapY);
		
		assertEquals(0.0, results[0], 0.000001);
		assertEquals(1.0, results[1], 0.000001);
		
		/*
			FRAC 0
			FRAC 0
			FRAC 0
			FRAC 1
			FRAC 0
			FRAC 0.428571428571429
			FRAC 0
			FRAC 0.571428571428571
			dist 0.5
		 */
		
		x = new double[] {QSigCompare.getDiscretisedValue(0.0),
				QSigCompare.getDiscretisedValue(0.0),
				QSigCompare.getDiscretisedValue(0.0),
				QSigCompare.getDiscretisedValue(1.0)};
		y = new double[] {QSigCompare.getDiscretisedValue(0.0),
				QSigCompare.getDiscretisedValue(0.428571428571429), 
				QSigCompare.getDiscretisedValue(0.0), 
				QSigCompare.getDiscretisedValue(0.571428571428571)};
		
		mapX.clear();
		mapY.clear();
		mapX.put(cp, x);
		mapY.put(cp, y);
		
		results = QSigCompareDistance.compareRatios(mapX, mapY);
		assertEquals(Math.sqrt(0.5), results[0], 0.000001);
		assertEquals(1.0, results[1], 0.000001);
	}

}
