package org.qcmg.sig.model;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

public class ComparisonTest {
	
	static File F1 = new File("f1");
	static File F2 = new File("f2");
	
	@Test
	public void testComparisonConstructor() {
		try {
			new Comparison((String)null,0, (String)null, 0, 0, 0, 0, 0, 0);
			Assert.fail("Should have thrown an IAE");
		}	catch (IllegalArgumentException iae){}
		
		Comparison c = new Comparison(F1, 0, F2, 0, 0, 0, 0, 0, 0);
		assertEquals(Double.NaN, c.getScore(), 0.00001);
		
		try {
			c = new Comparison(F1, 0, F2, 0, 100, 0, 0, 0, 0);
			Assert.fail("Should have thrown an IAE");
		}	catch (IllegalArgumentException iae){}
		
		try {
			c = new Comparison(F1, 0, F2, 0, -1, 0, 0, 0, 0);
			Assert.fail("Should have thrown an IAE");
		}	catch (IllegalArgumentException iae){}
		try {
			c = new Comparison(F1, -1, F2, 0, 0, 0, 0, 0, 0);
			Assert.fail("Should have thrown an IAE");
		}	catch (IllegalArgumentException iae){}
		try {
			c = new Comparison(F1, 0, F2, -1, 0, 0, 0, 0, 0);
			Assert.fail("Should have thrown an IAE");
		}	catch (IllegalArgumentException iae){}
		try {
			c = new Comparison(F1, 0, F2, 0, 0, -1, 0, 0, 0);
			Assert.fail("Should have thrown an IAE");
		}	catch (IllegalArgumentException iae){}
		try {
			c = new Comparison(F1, 0, F2, 0, 0, 0, -1, 0, 0);
			Assert.fail("Should have thrown an IAE");
		}	catch (IllegalArgumentException iae){}
	}
	
	@Test
	public void testComparison() {
		Comparison c = new Comparison(F1, 0, F2, 0, 0, 1, 0, 0, 0);
		assertEquals(0.0, c.getScore(), 0.00001);
		c = new Comparison(F1, 0, F2, 0, 10, 1, 0, 0, 0);
		assertEquals(10.0, c.getScore(), 0.00001);
		c = new Comparison(F1, 0, F2, 0, 10, 20, 0, 0, 0);
		assertEquals(0.5, c.getScore(), 0.00001);
		c = new Comparison(F1, 0, F2, 0, 10, 1000, 0, 0, 0);
		assertEquals(0.01, c.getScore(), 0.00001);
		
		c = new Comparison("f1", 123456, "f2", 234567, 99999, 123456, 1234560, 0, 0);
		assertEquals((99999.0 / 123456), c.getScore(), 0.00001);
		assertEquals("f1", c.getMain());
		assertEquals("f2", c.getTest());
		assertEquals(123456, c.getMainCoverage());
		assertEquals(234567, c.getTestCoverage());
		assertEquals(123456, c.getOverlapCoverage());
		assertEquals(99999.0, c.getTotalScore(), 0.1);
		
		assertEquals("null:f1 (123456) vs null:f2 (234567) : " + (99999.0 / 123456) + ", 123456, 1234560", c.toString());
		
	}
	
	@Test
	public void checkComparator() {
		Comparison c1 = new Comparison(F1, 0, F2, 0, 10, 100, 0, 0, 0);
		Comparison c2 = new Comparison(F1, 0, F2, 0, 20, 100, 0, 0, 0);
		assertEquals(-1, c1.compareTo(c2));
		
		c1 = new Comparison(F1, 0, F2, 0, 20, 100, 0, 0, 0);
		assertEquals(0, c1.compareTo(c2));
		
		c1 = new Comparison(F1, 0, F2, 0, 200, 1000, 0, 0, 0);
		assertEquals(1000 - 100, c1.compareTo(c2));
	}

}
