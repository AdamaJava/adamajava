package org.qcmg.qvisualise;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.qcmg.qvisualise.util.CycleDetailUtils;

public class CycleDetailutilsTest {
	
	@Test
	public void getQualFileCycle() {
		List<String> l = CycleDetailUtils.getQualFileCycle();
		assertEquals(44, l.size());
		int k = 0;
		for (int i = 42 ; i >= -1 ; i--) {
			assertEquals(i + "", l.get(k++));
		}
		
		assertEquals("42", l.get(0));
		assertEquals("41", l.get(1));
		assertEquals("32", l.get(10));
		assertEquals("0", l.get(42));
		assertEquals("-1", l.get(43));
	}
	
	@Test
	public void getQualCycleAscii() {
		List<String> l = CycleDetailUtils.getQualCycleASCII();
		assertEquals(41, l.size());
		int k = 0;
		for (int i = 40 ; i >= 0 ; i--) {
			assertEquals(i + "", l.get(k++));
		}
		
		assertEquals("40", l.get(0));
		assertEquals("39", l.get(1));
		assertEquals("30", l.get(10));
		assertEquals("1", l.get(39));
		assertEquals("0", l.get(40));
	}

}
