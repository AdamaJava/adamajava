package org.qcmg.qsv.softclip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.util.TestUtil;

public class ClipTest {
	
	Clip clip;
	
	@After
	public void tearDown() throws Exception {
		clip = null;
	}
	
	@Test
	public void testConstructor()  {
		clip = TestUtil.getClip("+", "right");
		assertFalse(clip.getIsReverse());
		assertFalse(clip.isLeft());
	}
	
	@Test
	public void testConstructor2()  {
		clip = TestUtil.getClip("-", "left");
		assertTrue(clip.getIsReverse());
		assertTrue(clip.isLeft());
	}
	
	@Test
	public void testCompareToAreEqual()  {
		Clip clip1 = TestUtil.getClip("-", "left");
		Clip clip2 = TestUtil.getClip("-", "left");
		assertEquals(0, clip1.compareTo(clip2));
	}
	
	@Test
	public void testCompareToDifferentPos() {
		Clip clip1 = TestUtil.getClip("-", "left");
		Clip clip2 = TestUtil.getClip("-", "left", 89700230);
		assertEquals(1, clip1.compareTo(clip2));
	}
	
	@Test
	public void testCompareToDifferentName() {
		Clip clip1 = TestUtil.getClip("-", "left");
		Clip clip2 = TestUtil.getClip("-", "left", "test");
		assertEquals(-44, clip1.compareTo(clip2));
	}
	
	@Test
	public void testGetBases() {
		Clip clip1 = TestUtil.getClip("-", "left");
		int [][] bases = new int[clip1.getClipSequence().length()][5];
		clip1.getClipBases(bases);
		assertEquals(1, bases[0][0]);
		assertEquals(1, bases[1][1]);
		assertEquals(1, bases[2][2]);
		assertEquals(1, bases[3][3]);
		assertEquals(1, bases[4][4]);
	}

}
