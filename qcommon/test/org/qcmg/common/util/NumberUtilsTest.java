package org.qcmg.common.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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
    public void getContinousCountFromArray() {
        int[] array = new int[]{29, 43, 44, 45, 136, 150, 151, 152, 226, 231, 232, 233, 234, 235, 236};
        assertEquals(0, NumberUtils.getContinuousCountFromValue(0, array));
        assertEquals(1, NumberUtils.getContinuousCountFromValue(29, array));
        assertEquals(0, NumberUtils.getContinuousCountFromValue(42, array));
        assertEquals(3, NumberUtils.getContinuousCountFromValue(43, array));
        assertEquals(1, NumberUtils.getContinuousCountFromValue(45, array));
        assertEquals(0, NumberUtils.getContinuousCountFromValue(46, array));
        assertEquals(0, NumberUtils.getContinuousCountFromValue(149, array));
        assertEquals(3, NumberUtils.getContinuousCountFromValue(150, array));
        assertEquals(1, NumberUtils.getContinuousCountFromValue(226, array));
        assertEquals(6, NumberUtils.getContinuousCountFromValue(231, array));
        array = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        assertEquals(11, NumberUtils.getContinuousCountFromValue(0, array));
    }

    @Test
    public void getContinousCountFromArrayRtoL() {
        int[] array = new int[]{29, 43, 44, 45, 136, 150, 151, 152, 226, 231, 232, 233, 234, 235, 236};
        assertEquals(0, NumberUtils.getContinuousCountFromValue(0, array, false));
        assertEquals(1, NumberUtils.getContinuousCountFromValue(29, array, false));
        assertEquals(0, NumberUtils.getContinuousCountFromValue(42, array, false));
        assertEquals(1, NumberUtils.getContinuousCountFromValue(43, array, false));
        assertEquals(3, NumberUtils.getContinuousCountFromValue(45, array, false));
        assertEquals(0, NumberUtils.getContinuousCountFromValue(46, array, false));
        assertEquals(0, NumberUtils.getContinuousCountFromValue(149, array, false));
        assertEquals(1, NumberUtils.getContinuousCountFromValue(150, array, false));
        assertEquals(1, NumberUtils.getContinuousCountFromValue(226, array, false));
        assertEquals(1, NumberUtils.getContinuousCountFromValue(231, array, false));
        array = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        assertEquals(1, NumberUtils.getContinuousCountFromValue(0, array, false));
        assertEquals(11, NumberUtils.getContinuousCountFromValue(10, array, false));
    }

    @Test
    public void setBitOnByte() {
        assertEquals(1, NumberUtils.setBit((byte) 0, 0));
        assertEquals(2, NumberUtils.setBit((byte) 1, 0));
        assertEquals(3, NumberUtils.setBit((byte) 2, 0));

        assertEquals(2, NumberUtils.setBit((byte) 0, 1));
        assertEquals(4, NumberUtils.setBit((byte) 0, 2));
        assertEquals(8, NumberUtils.setBit((byte) 0, 3));
        assertEquals(16, NumberUtils.setBit((byte) 0, 4));
        assertEquals(32, NumberUtils.setBit((byte) 0, 5));
        assertEquals(64, NumberUtils.setBit((byte) 0, 6));
        assertEquals(-128, NumberUtils.setBit((byte) 0, 7));
        assertEquals(0, NumberUtils.setBit((byte) 0, 8));

        assertEquals(3, NumberUtils.setBit((byte) 1, 1));
        assertEquals(3, NumberUtils.setBit((byte) 2, 0));
        assertEquals(4, NumberUtils.setBit((byte) 2, 1));
        assertEquals(6, NumberUtils.setBit((byte) 2, 2));
    }

    @Test
    public void splitIntInto2() {
        assertArrayEquals(new int[]{0, 0}, NumberUtils.splitIntInto2(0));
        assertArrayEquals(new int[]{0, 1}, NumberUtils.splitIntInto2(1));
        assertArrayEquals(new int[]{1, 0}, NumberUtils.splitIntInto2(65536));
        assertArrayEquals(new int[]{1, 1}, NumberUtils.splitIntInto2(65537));
        assertArrayEquals(new int[]{2, 0}, NumberUtils.splitIntInto2(1 << 17));
        assertArrayEquals(new int[]{2, 2}, NumberUtils.splitIntInto2((1 << 17) + 2));
        assertArrayEquals(new int[]{6, 16}, NumberUtils.splitIntInto2(393232));
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

    @Test
    public void getOverlap() {
        assertEquals(1, NumberUtils.getOverlap(1, 2, 1, 2));
        assertEquals(0, NumberUtils.getOverlap(1, 1, 1, 1));
        assertEquals(10, NumberUtils.getOverlap(10, 20, 10, 20));
        assertEquals(10, NumberUtils.getOverlap(10, 20, 10, 30));
        assertEquals(10, NumberUtils.getOverlap(10, 20, 1, 30));
        assertEquals(10, NumberUtils.getOverlap(2, 25, 10, 20));
        assertEquals(0, NumberUtils.getOverlap(2, 25, 30, 40));
        assertEquals(0, NumberUtils.getOverlap(20, 250, 10, 19));
        assertEquals(1, NumberUtils.getOverlap(20, 250, 10, 21));
        assertEquals(1, NumberUtils.getOverlap(19, 250, 10, 20));
    }

    @Test
    public void getRangesOverlap() {
        assertEquals(0, NumberUtils.getOverlap(1, 100, 100, 200));
        assertEquals(0, NumberUtils.getOverlap(1, 100, 1, 1));
        assertEquals(1, NumberUtils.getOverlap(1, 100, 1, 2));
        assertEquals(60, NumberUtils.getOverlap(1, 100, 20, 80));
        assertEquals(99, NumberUtils.getOverlap(1, 100, 1, 200));
        assertEquals(0, NumberUtils.getOverlap(100, 200, 1, 100));
        assertEquals(1, NumberUtils.getOverlap(100, 200, 99, 101));
        assertEquals(50, NumberUtils.getOverlap(100, 200, 99, 150));
        assertEquals(100, NumberUtils.getOverlap(100, 200, 99, 2000));
        assertEquals(100, NumberUtils.getOverlap(100, 200, 99, 201));

        assertEquals(12, NumberUtils.getOverlap(0, 12, 0, 161));
    }

}
