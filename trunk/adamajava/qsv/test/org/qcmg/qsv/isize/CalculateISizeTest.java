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
		map = new ConcurrentHashMap<Integer,AtomicInteger>();
		
		map.put(new Integer(0), new AtomicInteger(22956));
		map.put(new Integer(40), new AtomicInteger(2));
		map.put(new Integer(80), new AtomicInteger(2));
		map.put(new Integer(90), new AtomicInteger(376));
		map.put(new Integer(100), new AtomicInteger(2820));
		map.put(new Integer(110), new AtomicInteger(5772));
		map.put(new Integer(120), new AtomicInteger(8896));
		map.put(new Integer(130), new AtomicInteger(11358));
		map.put(new Integer(140), new AtomicInteger(12626));
		map.put(new Integer(150), new AtomicInteger(13437));
		map.put(new Integer(160), new AtomicInteger(14032));
		map.put(new Integer(170), new AtomicInteger(14451));
		map.put(new Integer(180), new AtomicInteger(14537));
		map.put(new Integer(190), new AtomicInteger(14615));
		map.put(new Integer(200), new AtomicInteger(15806));
		map.put(new Integer(210), new AtomicInteger(16309));
		map.put(new Integer(220), new AtomicInteger(17540));
		map.put(new Integer(230), new AtomicInteger(20509));
		map.put(new Integer(240), new AtomicInteger(24656));
		map.put(new Integer(250), new AtomicInteger(28513));
		map.put(new Integer(260), new AtomicInteger(34720));
		map.put(new Integer(270), new AtomicInteger(40961));
		map.put(new Integer(280), new AtomicInteger(49129));
		map.put(new Integer(290), new AtomicInteger(58384));
		map.put(new Integer(300), new AtomicInteger(71585));
		map.put(new Integer(310), new AtomicInteger(73491));
		map.put(new Integer(320), new AtomicInteger(67551));
		map.put(new Integer(330), new AtomicInteger(57469));
		map.put(new Integer(340), new AtomicInteger(46505));
		map.put(new Integer(350), new AtomicInteger(35617));
		map.put(new Integer(360), new AtomicInteger(26385));
		map.put(new Integer(370), new AtomicInteger(17762));
		map.put(new Integer(380), new AtomicInteger(10119));
		map.put(new Integer(390), new AtomicInteger(4459));
		map.put(new Integer(400), new AtomicInteger(1704));
		map.put(new Integer(410), new AtomicInteger(782));
		map.put(new Integer(420), new AtomicInteger(536));
		map.put(new Integer(430), new AtomicInteger(372));
		map.put(new Integer(440), new AtomicInteger(268));
		map.put(new Integer(450), new AtomicInteger(266));
		map.put(new Integer(460), new AtomicInteger(234));
		map.put(new Integer(470), new AtomicInteger(230));
		map.put(new Integer(480), new AtomicInteger(254));
		map.put(new Integer(490), new AtomicInteger(214));
		map.put(new Integer(500), new AtomicInteger(180));
		map.put(new Integer(510), new AtomicInteger(148));
		map.put(new Integer(520), new AtomicInteger(178));
		map.put(new Integer(530), new AtomicInteger(64));
		map.put(new Integer(540), new AtomicInteger(44));
		map.put(new Integer(550), new AtomicInteger(30));
		map.put(new Integer(560), new AtomicInteger(44));
		map.put(new Integer(570), new AtomicInteger(20));
		map.put(new Integer(580), new AtomicInteger(30));
		map.put(new Integer(590), new AtomicInteger(22));
		map.put(new Integer(600), new AtomicInteger(32));
		map.put(new Integer(610), new AtomicInteger(28));
		map.put(new Integer(620), new AtomicInteger(42));
		map.put(new Integer(630), new AtomicInteger(22));
		map.put(new Integer(640), new AtomicInteger(18));
		map.put(new Integer(650), new AtomicInteger(10));
		map.put(new Integer(660), new AtomicInteger(2));
		map.put(new Integer(670), new AtomicInteger(10));
		map.put(new Integer(680), new AtomicInteger(4));
		map.put(new Integer(690), new AtomicInteger(8));
		map.put(new Integer(700), new AtomicInteger(32));
		map.put(new Integer(710), new AtomicInteger(22));
		map.put(new Integer(720), new AtomicInteger(18));
		map.put(new Integer(730), new AtomicInteger(8));
		map.put(new Integer(780), new AtomicInteger(4));
		map.put(new Integer(790), new AtomicInteger(2));
		map.put(new Integer(800), new AtomicInteger(8));
		map.put(new Integer(810), new AtomicInteger(2));
		map.put(new Integer(820), new AtomicInteger(4));
		map.put(new Integer(860), new AtomicInteger(2));

	}
	
	@After
	public void tearDown() {
		map = null;
		isize = null;
	}	
	
	@Test
	public void testFindMaxIndex() throws QSVException {
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
		map.put(new Integer(10), new AtomicInteger(6844));
		
		
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
		map.remove(new Integer(0));
		isize = new CalculateISize(map);
		isize.getLogMaps();
		assertEquals(73, map.size());
		assertEquals(25, isize.getLeftMap().size());
		assertEquals(49, isize.getRightMap().size());
	}
	
	@Test
	public void findFirstDerivative() {
		map.remove(new Integer(0));
		isize = new CalculateISize(map);
		Map<Integer, Double> xyMap = new TreeMap<Integer, Double>();
		
		xyMap.put(new Integer(80), new Double(0.301029995663981));
		xyMap.put(new Integer(90), new Double(2.57518784492766));
		xyMap.put(new Integer(100), new Double(3.45024910831936));		
		
		double[] dydx = isize.findFirstDerivative(xyMap);
		
		assertEquals(0.227415784926368, dydx[0], 0.000001);
		assertEquals(0.157460955632769, dydx[1], 0.00001);
		assertEquals(0.08750612633917, dydx[2], 0.00001);		
	}

	
	
	

}
