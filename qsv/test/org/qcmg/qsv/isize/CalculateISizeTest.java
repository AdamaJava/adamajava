package org.qcmg.qsv.isize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.qsv.QSVException;

public class CalculateISizeTest {
	
	CalculateISize isize;
	ConcurrentHashMap<Integer, AtomicInteger>  map;

    @Before
    public void setUp() throws QSVException {
        map = new ConcurrentHashMap<>();

        int[] keys = new int[]{0, 40, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200, 210, 220,
                230, 240, 250, 260, 270, 280, 290, 300, 310, 320, 330, 340, 350, 360, 370, 380, 390,
                400, 410, 420, 430, 440, 450, 460, 470, 480, 490, 500, 510, 520, 530, 540, 550, 560,
                570, 580, 590, 600, 610, 620, 630, 640, 650, 660, 670, 680, 690, 700, 710, 720, 730,
                780, 790, 800, 810, 820, 860};
        int[] values = new int[]{22956, 2, 2, 376, 2820, 5772, 8896, 11358, 12626, 13437, 14032, 14451, 14537, 14615,
                15806, 16309, 17540, 20509, 24656, 28513, 34720, 40961, 49129, 58384, 71585, 73491,
                67551, 57469, 46505, 35617, 26385, 17762, 10119, 4459, 1704, 782, 536, 372, 268, 266,
                234, 230, 254, 214, 180, 148, 178, 64, 44, 30, 44, 20, 30, 22, 32, 28, 42, 22, 18, 10,
                2, 10, 4, 8, 32, 22, 18, 8, 4, 2, 8, 2, 4, 2};

        for (int i = 0; i < keys.length; i++) {
            putValueInMap(keys[i], values[i]);
        }
    }

    private void putValueInMap(int key, int value) {
        map.put(key, new AtomicInteger(value));
    }
	
	@After
	public void tearDown() {
		map = null;
		isize = null;
	}	
	
	@Test
	public void testFindMaxIndex() {
		double[] array = new double[4];
		array[0] = 1.34;
		array[1] = 2.34;
		array[2] = 3.34;
		array[3]= 1.23;

		isize = new CalculateISize(map);
		assertEquals(2, isize.findMaxIndex(array));
	}
	
	@Test(expected=QSVException.class)
	public void  testCalculateThrowsException() throws QSVException {
		map.clear();
		map.put(10, new AtomicInteger(6844));
		
		
		assertTrue(map.size() < 10);
		isize = new CalculateISize(map);
		isize.calculate();
	}
	
	@Test
	public void testCalculate() throws QSVException {
		isize = new CalculateISize(map);
		isize.calculate();
		assertEquals(85, isize.getISizeMin());
		assertEquals(725,isize.getISizeMax());
	}
	
	@Test
	public void testGetLogMaps() {
		map.remove(0);
		isize = new CalculateISize(map);
		isize.getLogMaps();
		assertEquals(73, map.size());
		assertEquals(25, isize.getLeftMap().size());
		assertEquals(49, isize.getRightMap().size());
	}
	
	@Test
	public void findFirstDerivative() {
		map.remove(0);
		isize = new CalculateISize(map);
		Map<Integer, Double> xyMap = new TreeMap<>();
		
		xyMap.put( 80, 0.301029995663981);
		xyMap.put( 90, 2.57518784492766);
		xyMap.put( 100, 3.45024910831936);
		
		double[] dydx = isize.findFirstDerivative(xyMap);
		
		assertEquals(0.227415784926368, dydx[0], 0.000001);
		assertEquals(0.157460955632769, dydx[1], 0.00001);
		assertEquals(0.08750612633917, dydx[2], 0.00001);		
	}

	
	
	

}
