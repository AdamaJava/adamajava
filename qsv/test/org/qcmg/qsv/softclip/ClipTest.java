package org.qcmg.qsv.softclip;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.qcmg.qsv.util.TestUtil;

public class ClipTest {
	
	
	@Test
	public void testConstructor()  {
		Clip clip = TestUtil.getClip("+", "right");
		assertFalse(clip.getIsReverse());
		assertFalse(clip.isLeft());
	}
	
	@Test
	public void testConstructor2()  {
		Clip clip = TestUtil.getClip("-", "left");
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
		Clip.getClipBases(bases, clip1.isLeft(), clip1.getClipSequence());
		assertEquals(1, bases[0][0]);
		assertEquals(1, bases[1][1]);
		assertEquals(1, bases[2][2]);
		assertEquals(1, bases[3][3]);
		assertEquals(1, bases[4][4]);
	}
	
	@Test
	public void getReferenceBases() {
		Clip clip1 = TestUtil.getClip("-", "left");
		int [][] bases = new int[clip1.getReferenceSequence().length()][5];
		Clip.getReferenceBases(bases, clip1.isLeft(), clip1.getReferenceSequence());
		/*
		 * ACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACA
		 */
		assertArrayEquals(new int[] {1,0,0,0,0},  bases[0]);
		assertArrayEquals(new int[] {0,1,0,0,0},  bases[1]);
		assertArrayEquals(new int[] {1,0,0,0,0},  bases[clip1.getReferenceSequence().length() - 1]);
	}
	
	@Test
	public void getClipBases() {
		String s = "";
		int [][] bases = new int[s.length() + 1][5];
		Clip.getClipBases(bases, true, s);
		assertEquals(0, bases[0][0]);
		assertEquals(0, bases[0][1]);
		assertEquals(0, bases[0][2]);
		assertEquals(0, bases[0][3]);
		assertEquals(0, bases[0][4]);
		
		s = "A";
		bases = new int[s.length()][5];
		Clip.getClipBases(bases, true, s);
		assertArrayEquals(new int[] {1,0,0,0,0},  bases[0]);
		
		s = "AC";
		bases = new int[s.length()][5];
		Clip.getClipBases(bases, true, s);
		assertArrayEquals(new int[] {1,0,0,0,0},  bases[0]);
		assertArrayEquals(new int[] {0,1,0,0,0},  bases[1]);
		Clip.getClipBases(bases, true, s);
		assertArrayEquals(new int[] {2,0,0,0,0},  bases[0]);
		assertArrayEquals(new int[] {0,2,0,0,0},  bases[1]);
		Clip.getClipBases(bases, true, s);
		assertArrayEquals(new int[] {3,0,0,0,0},  bases[0]);
		assertArrayEquals(new int[] {0,3,0,0,0},  bases[1]);
		s = "GT";
		Clip.getClipBases(bases, true, s);
		assertArrayEquals(new int[] {3,0,0,1,0},  bases[0]);
		assertArrayEquals(new int[] {0,3,1,0,0},  bases[1]);
	}
	@Test
	public void getClipBasesRight() {
		String s = "";
		int [][] bases = new int[s.length() + 1][5];
		Clip.getClipBases(bases, false, s);
		assertEquals(0, bases[0][0]);
		assertEquals(0, bases[0][1]);
		assertEquals(0, bases[0][2]);
		assertEquals(0, bases[0][3]);
		assertEquals(0, bases[0][4]);
		
		s = "A";
		bases = new int[s.length()][5];
		Clip.getClipBases(bases, false, s);
		assertArrayEquals(new int[] {1,0,0,0,0},  bases[0]);
		
		s = "AC";
		bases = new int[s.length()][5];
		Clip.getClipBases(bases, false, s);
		assertArrayEquals(new int[] {1,0,0,0,0},  bases[0]);
		assertArrayEquals(new int[] {0,1,0,0,0},  bases[1]);
		Clip.getClipBases(bases, false, s);
		assertArrayEquals(new int[] {2,0,0,0,0},  bases[0]);
		assertArrayEquals(new int[] {0,2,0,0,0},  bases[1]);
		Clip.getClipBases(bases, false, s);
		assertArrayEquals(new int[] {3,0,0,0,0},  bases[0]);
		assertArrayEquals(new int[] {0,3,0,0,0},  bases[1]);
		s = "GT";
		Clip.getClipBases(bases, false, s);
		assertArrayEquals(new int[] {3,0,0,1,0},  bases[0]);
		assertArrayEquals(new int[] {0,3,1,0,0},  bases[1]);
	}
	
	@Test
	public void addBase() {
		int [][] array = new int[1][5];
		Clip.addBase(0, 'A', array);
		assertEquals(1, array[0][0]);
		Clip.addBase(0, 'A', array);
		assertEquals(2, array[0][0]);
		Clip.addBase(0, 'A', array);
		assertEquals(3, array[0][0]);
		Clip.addBase(0, 'G', array);
		Clip.addBase(0, 'G', array);
		assertEquals(2, array[0][3]);
		Clip.addBase(0, 'T', array);
		assertEquals(1, array[0][2]);
		Clip.addBase(0, 'C', array);
		Clip.addBase(0, 'C', array);
		Clip.addBase(0, 'C', array);
		Clip.addBase(0, 'C', array);
		assertEquals(4, array[0][1]);
		assertEquals(0, array[0][4]);	// N's
	}

}
