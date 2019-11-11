package org.qcmg.common.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class NumberUtilsTest {
	
	@Test
	public void pack2IntsInto1() {
		assertEquals(0, NumberUtils.pack2IntsInto1(0, 0));
		assertEquals(1, NumberUtils.pack2IntsInto1(0, 1));
		
		assertEquals(1 << NumberUtils.SHORT_DIVIDER, NumberUtils.pack2IntsInto1(1, 0));
		assertEquals(65536, NumberUtils.pack2IntsInto1(1, 0));
		
		/*
		 * should be 65536 + 1
		 */
		assertEquals((1 << NumberUtils.SHORT_DIVIDER) + 1, NumberUtils.pack2IntsInto1(1, 1));
		assertEquals(65537, NumberUtils.pack2IntsInto1(1, 1));
		/*
		 * should be 65536 * 2
		 */
		assertEquals(131072, NumberUtils.pack2IntsInto1(2, 0));
		assertEquals(2 << NumberUtils.SHORT_DIVIDER, NumberUtils.pack2IntsInto1(2, 0));
		
		assertEquals(851974, NumberUtils.pack2IntsInto1(13, 6));
		assertEquals(((13 << NumberUtils.SHORT_DIVIDER) + 6), NumberUtils.pack2IntsInto1(13, 6));
	}
	
	@Test
	public void setBitOnByte() {
		assertEquals(1, NumberUtils.setBit((byte)0, 0));
		assertEquals(2, NumberUtils.setBit((byte)1, 0));
		assertEquals(3, NumberUtils.setBit((byte)2, 0));
		
		assertEquals(2, NumberUtils.setBit((byte)0, 1));
		assertEquals(4, NumberUtils.setBit((byte)0, 2));
		assertEquals(8, NumberUtils.setBit((byte)0, 3));
		assertEquals(16, NumberUtils.setBit((byte)0, 4));
		assertEquals(32, NumberUtils.setBit((byte)0, 5));
		assertEquals(64, NumberUtils.setBit((byte)0, 6));
		assertEquals(-128, NumberUtils.setBit((byte)0, 7));
		assertEquals(0, NumberUtils.setBit((byte)0, 8));
		
		assertEquals(3, NumberUtils.setBit((byte)1, 1));
		assertEquals(3, NumberUtils.setBit((byte)2, 0));
		assertEquals(4, NumberUtils.setBit((byte)2, 1));
		assertEquals(6, NumberUtils.setBit((byte)2, 2));
	}
	
	@Test
	public void splitIntInto2() {
		assertArrayEquals(new int[] {0,0}, NumberUtils.splitIntInto2(0));
		assertArrayEquals(new int[] {0,1}, NumberUtils.splitIntInto2(1));
		assertArrayEquals(new int[] {1,0}, NumberUtils.splitIntInto2(65536));
		assertArrayEquals(new int[] {1,1}, NumberUtils.splitIntInto2(65537));
		assertArrayEquals(new int[] {2,0}, NumberUtils.splitIntInto2(1 << 17));
		assertArrayEquals(new int[] {2,2}, NumberUtils.splitIntInto2((1 << 17) + 2));
		assertArrayEquals(new int[] {6,16}, NumberUtils.splitIntInto2(393232));
	}
	
	@Test
	public void getPartOfPackedInt() {
		int packedInt = NumberUtils.pack2IntsInto1(10, 15);
		assertEquals(10, NumberUtils.getPartOfPackedInt(packedInt, true));
		assertEquals(15, NumberUtils.getPartOfPackedInt(packedInt, false));
	}
	
	@Test
	public void sumPackedInt() {
		assertEquals(0, NumberUtils.sumPackedInt(0));
		assertEquals(1, NumberUtils.sumPackedInt(1));
		assertEquals(2, NumberUtils.sumPackedInt(2));
		assertEquals(1, NumberUtils.sumPackedInt(65536));
		assertEquals(2, NumberUtils.sumPackedInt(65537));
	}
	
}