package org.qcmg.common.util;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TLongIntMap;
import org.junit.Test;
import org.qcmg.common.model.Accumulator;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import static org.junit.Assert.*;

public class AccumulatorUtilsTest {

    @Test
    public void baseDetails() {
        AccumulatorUtils.BaseDetails bd1 = new AccumulatorUtils.BaseDetails('A', 0, 0, false);
        AccumulatorUtils.BaseDetails bd2 = new AccumulatorUtils.BaseDetails('B', 0, 0, false);
        List<AccumulatorUtils.BaseDetails> list = Arrays.asList(bd2, bd1);

        list.sort(null);
        assertEquals(bd2, list.getFirst());

        bd1 = new AccumulatorUtils.BaseDetails('A', 1, 0, false);
        list = Arrays.asList(bd2, bd1);
        list.sort(null);
        assertEquals(bd1, list.getFirst());


        bd1 = new AccumulatorUtils.BaseDetails('A', 1, 0, false);
        bd2 = new AccumulatorUtils.BaseDetails('A', 1, 0, false);
        list = Arrays.asList(bd2, bd1);
        list.sort(null);
        assertEquals(bd2, list.getFirst());

        bd1 = new AccumulatorUtils.BaseDetails('A', 1, 1, false);
        list = Arrays.asList(bd2, bd1);
        list.sort(null);
        assertEquals(bd1, list.getFirst());


        bd1 = new AccumulatorUtils.BaseDetails('A', 1, 1, true);
        bd2 = new AccumulatorUtils.BaseDetails('A', 1, 1, false);
        list = Arrays.asList(bd2, bd1);
        list.sort(null);
        assertEquals(bd1, list.getFirst());

        bd1 = new AccumulatorUtils.BaseDetails('A', 1, 1, true);
        bd2 = new AccumulatorUtils.BaseDetails('A', 1, 1, true);
        list = Arrays.asList(bd2, bd1);
        list.sort(null);
        assertEquals(bd2, list.getFirst());
    }

    @Test
    public void getLong() {
        long l = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(true, true, (byte) 'A', (byte) 30, 1);
        assertTrue((l & (1L << AccumulatorUtils.STRAND_BIT_POSITION)) != 0);    // strand
        assertTrue((l & (1L << AccumulatorUtils.END_OF_READ_BIT_POSITION)) != 0);    // end of read
        assertTrue((l & (1L << AccumulatorUtils.A_BASE_BIT_POSITION)) != 0);    // A
        assertEquals(0, (l & (1L << AccumulatorUtils.C_BASE_BIT_POSITION)));    // C
        assertEquals(0, (l & (1L << AccumulatorUtils.G_BASE_BIT_POSITION)));    // G
        assertEquals(0, (l & (1L << AccumulatorUtils.T_BASE_BIT_POSITION)));    // T
        assertEquals(1, (int) l);
        assertEquals(30, AccumulatorUtils.getQualityFromLong(l));
        assertEquals(Accumulator.A_CHAR, AccumulatorUtils.getBaseAsCharFromLong(l));

        l = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(false, false, (byte) 'C', (byte) 60, 100);
        assertEquals(0, (l & (1L << AccumulatorUtils.STRAND_BIT_POSITION)));    // strand
        assertEquals(0, (l & (1L << AccumulatorUtils.END_OF_READ_BIT_POSITION)));    // end of read
        assertEquals(0, (l & (1L << AccumulatorUtils.A_BASE_BIT_POSITION)));    // A
        assertTrue((l & (1L << AccumulatorUtils.C_BASE_BIT_POSITION)) != 0);    // C
        assertEquals(0, (l & (1L << AccumulatorUtils.G_BASE_BIT_POSITION)));    // G
        assertEquals(0, (l & (1L << AccumulatorUtils.T_BASE_BIT_POSITION)));    // T
        assertEquals(100, (int) l);
        assertEquals(60, AccumulatorUtils.getQualityFromLong(l));
        assertEquals(Accumulator.C_CHAR, AccumulatorUtils.getBaseAsCharFromLong(l));

        l = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(false, true, (byte) 'G', (byte) 40, 1234567890);
        assertEquals(0, (l & (1L << AccumulatorUtils.STRAND_BIT_POSITION)));    // strand
        assertTrue((l & (1L << AccumulatorUtils.END_OF_READ_BIT_POSITION)) != 0);    // end of read
        assertEquals(0, (l & (1L << AccumulatorUtils.A_BASE_BIT_POSITION)));    // A
        assertEquals(0, (l & (1L << AccumulatorUtils.C_BASE_BIT_POSITION)));    // C
        assertTrue((l & (1L << AccumulatorUtils.G_BASE_BIT_POSITION)) != 0);    // G
        assertEquals(0, (l & (1L << AccumulatorUtils.T_BASE_BIT_POSITION)));    // T
        assertEquals(1234567890, (int) l);
        assertEquals(40, AccumulatorUtils.getQualityFromLong(l));
        assertEquals(Accumulator.G_CHAR, AccumulatorUtils.getBaseAsCharFromLong(l));

        l = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(true, false, (byte) 'T', (byte) 12, 1);
        assertTrue((l & (1L << AccumulatorUtils.STRAND_BIT_POSITION)) != 0);    // strand
        assertEquals(0, (l & (1L << AccumulatorUtils.END_OF_READ_BIT_POSITION)));    // end of read
        assertEquals(0, (l & (1L << AccumulatorUtils.A_BASE_BIT_POSITION)));    // A
        assertEquals(0, (l & (1L << AccumulatorUtils.C_BASE_BIT_POSITION)));    // C
        assertEquals(0, (l & (1L << AccumulatorUtils.G_BASE_BIT_POSITION)));    // G
        assertTrue((l & (1L << AccumulatorUtils.T_BASE_BIT_POSITION)) != 0);    // T
        assertEquals(1, (int) l);
        assertEquals(12, AccumulatorUtils.getQualityFromLong(l));
        assertEquals(Accumulator.T_CHAR, AccumulatorUtils.getBaseAsCharFromLong(l));
    }

    @Test
    public void largestVariant() {

        Accumulator acc = new Accumulator(150);
        for (int i = 0; i < 10; i++) {
            acc.addBase((byte) 'G', (byte) 1, true, 100, 150, 200, i);
        }
        for (int i = 10; i < 20; i++) {
            acc.addBase((byte) 'G', (byte) 1, false, 100, 150, 200, i);
        }
        assertArrayEquals(new int[]{0, 0, 0, 0, 0, 0}, AccumulatorUtils.getLargestVariant(acc, 'G'));
        assertArrayEquals(new int[]{10, 10, 0, 10, 10, 0}, AccumulatorUtils.getLargestVariant(acc, 'C'));
        assertArrayEquals(new int[]{10, 10, 0, 10, 10, 0}, AccumulatorUtils.getLargestVariant(acc, 'A'));
        assertArrayEquals(new int[]{10, 10, 0, 10, 10, 0}, AccumulatorUtils.getLargestVariant(acc, 'T'));

        for (int i = 20; i < 25; i++) {
            acc.addBase((byte) 'T', (byte) 1, true, 100, 150, 200, i);
        }
        for (int i = 25; i < 30; i++) {
            acc.addBase((byte) 'T', (byte) 1, false, 100, 150, 200, i);
        }
        assertArrayEquals(new int[]{10, 10, 0, 10, 10, 0}, AccumulatorUtils.getLargestVariant(acc, 'T'));
        assertArrayEquals(new int[]{5, 5, 0, 5, 5, 0}, AccumulatorUtils.getLargestVariant(acc, 'G'));

        /*
         * add another base - 10 reads, but quality is better - this should be returned when ref is 'G'
         */
        for (int i = 30; i < 35; i++) {
            acc.addBase((byte) 'A', (byte) 2, true, 100, 150, 200, i);
        }
        for (int i = 35; i < 40; i++) {
            acc.addBase((byte) 'A', (byte) 2, false, 100, 150, 200, i);
        }
        assertArrayEquals(new int[]{10, 10, 0, 10, 10, 0}, AccumulatorUtils.getLargestVariant(acc, 'T'));
        assertArrayEquals(new int[]{10, 10, 0, 10, 10, 0}, AccumulatorUtils.getLargestVariant(acc, 'A'));
        assertArrayEquals(new int[]{5, 10, 0, 5, 10, 0}, AccumulatorUtils.getLargestVariant(acc, 'G'));
    }

    @Test
    public void getReadNameHashStartPositionMap() {
        Accumulator acc = new Accumulator(150);
        for (int i = 0; i < 10; i++) {
            acc.addBase((byte) 'G', (byte) 1, true, 100, 150, 200, i);
        }
        for (int i = 10; i < 20; i++) {
            acc.addBase((byte) 'G', (byte) 1, false, 100, 150, 200, i);
        }

        TLongIntMap map = AccumulatorUtils.getReadNameHashStartPositionMap(acc);
        assertEquals(20, map.size());
        for (int i = 0; i < 10; i++) {
            assertEquals(100, map.get(i));
        }
        for (int i = 10; i < 20; i++) {
            assertEquals(-200, map.get(i));        // reverse strand, and so negative
        }
    }

    @Test
    public void getNNs() {
        Accumulator acc = new Accumulator(150);
        for (int i = 0; i < 10; i++) {
            acc.addBase((byte) 'G', (byte) 1, true, 100, 150, 200, i);
        }
        for (int i = 10; i < 20; i++) {
            acc.addBase((byte) 'G', (byte) 1, false, 100, 150, 200, i);
        }

        assertEquals(2, AccumulatorUtils.getNovelStartsForBase(acc, 'G'));
        assertEquals(0, AccumulatorUtils.getNovelStartsForBase(acc, 'A'));
        assertEquals(0, AccumulatorUtils.getNovelStartsForBase(acc, 'T'));
        assertEquals(0, AccumulatorUtils.getNovelStartsForBase(acc, 'C'));

        for (int i = 20; i < 30; i++) {
            acc.addBase((byte) 'C', (byte) 1, true, 100 + i, 150, 200, i);
        }
        for (int i = 30; i < 40; i++) {
            acc.addBase((byte) 'C', (byte) 1, false, 100, 150, 200 + i, i);
        }
        assertEquals(2, AccumulatorUtils.getNovelStartsForBase(acc, 'G'));
        assertEquals(0, AccumulatorUtils.getNovelStartsForBase(acc, 'A'));
        assertEquals(0, AccumulatorUtils.getNovelStartsForBase(acc, 'T'));
        assertEquals(20, AccumulatorUtils.getNovelStartsForBase(acc, 'C'));
    }

    @Test
    public void getOABS() {
        assertNull(AccumulatorUtils.toObservedAlleleByStrand(null, null));
        assertNull(AccumulatorUtils.toObservedAlleleByStrand(null, new int[]{}));
        assertNull(AccumulatorUtils.toObservedAlleleByStrand("blah", null));
        assertNull(AccumulatorUtils.toObservedAlleleByStrand("blah", new int[]{}));
        assertEquals("blah1[2]4[1.25]", AccumulatorUtils.toObservedAlleleByStrand("blah", new int[]{1, 2, 3, 4, 5, 6}));
        assertEquals("blah210[20]4[25]", AccumulatorUtils.toObservedAlleleByStrand("blah2", new int[]{10, 200, 0, 4, 100, 0}));
    }

    @Test
    public void getReadNameHashStartPositionMapForMultipleAccs() {
        Accumulator acc1 = new Accumulator(150);
        for (int i = 0; i < 10; i++) {
            acc1.addBase((byte) 'G', (byte) 1, true, 100, 150, 200, i);
        }
        for (int i = 10; i < 20; i++) {
            acc1.addBase((byte) 'G', (byte) 1, false, 100, 150, 200, i);
        }
        Accumulator acc2 = new Accumulator(151);
        for (int i = 0; i < 10; i++) {
            acc2.addBase((byte) 'A', (byte) 1, true, 100, 151, 200, i);
        }
        for (int i = 10; i < 20; i++) {
            acc2.addBase((byte) 'A', (byte) 1, false, 100, 151, 200, i);
        }

        TLongIntMap map = AccumulatorUtils.getReadIdStartPosMap(Arrays.asList(acc1, acc2));
        assertEquals(20, map.size());
        for (int i = 0; i < 10; i++) {
            assertEquals(100, map.get(i));
        }
        for (int i = 10; i < 20; i++) {
            assertEquals(-200, map.get(i));        // reverse strand, and so negative
        }
    }


    @Test
    public void getBaseCountByStrand() {
        Accumulator acc1 = new Accumulator(150);
        for (int i = 0; i < 10; i++) {
            acc1.addBase((byte) 'G', (byte) 1, true, 100, 150, 200, i);
        }
        for (int i = 10; i < 20; i++) {
            acc1.addBase((byte) 'G', (byte) 1, false, 100, 150, 200, i);
        }
        assertArrayEquals(new int[]{0, 0, 0, 0, 10, 10, 0, 0}, AccumulatorUtils.getBaseCountByStrand(acc1));
        Accumulator acc2 = new Accumulator(151);
        for (int i = 0; i < 10; i++) {
            acc2.addBase((byte) 'A', (byte) 1, true, 100, 151, 200, i);
        }
        for (int i = 10; i < 20; i++) {
            acc2.addBase((byte) 'A', (byte) 1, false, 100, 151, 200, i);
        }
        assertArrayEquals(new int[]{10, 10, 0, 0, 0, 0, 0, 0}, AccumulatorUtils.getBaseCountByStrand(acc2));

        for (int i = 0; i < 10; i++) {
            acc2.addBase((byte) 'G', (byte) 1, true, 100, 151, 200, i);
        }
        for (int i = 10; i < 20; i++) {
            acc2.addBase((byte) 'G', (byte) 1, false, 100, 151, 200, i);
        }
        assertArrayEquals(new int[]{10, 10, 0, 0, 10, 10, 0, 0}, AccumulatorUtils.getBaseCountByStrand(acc2));
    }

    @Test
    public void getBaseCountByStrand2() {
        int[] array = new int[]{1, 2, 3, 4, 5, 6, 7, 8};
        assertArrayEquals(new int[]{1, 2}, AccumulatorUtils.getBaseCountByStrand(array, 'A'));
        assertArrayEquals(new int[]{3, 4}, AccumulatorUtils.getBaseCountByStrand(array, 'C'));
        assertArrayEquals(new int[]{5, 6}, AccumulatorUtils.getBaseCountByStrand(array, 'G'));
        assertArrayEquals(new int[]{7, 8}, AccumulatorUtils.getBaseCountByStrand(array, 'T'));
    }

    @Test
    public void decrementBaseCountByStrandArray() {
        int[] array = new int[]{1, 2, 3, 4, 5, 6, 7, 8};
        AccumulatorUtils.decrementBaseCountByStrandArray(array, 'A', false);
        assertArrayEquals(new int[]{1, 1, 3, 4, 5, 6, 7, 8}, array);
        AccumulatorUtils.decrementBaseCountByStrandArray(array, 'T', true);
        assertArrayEquals(new int[]{1, 1, 3, 4, 5, 6, 6, 8}, array);
        AccumulatorUtils.decrementBaseCountByStrandArray(array, 'C', true);
        assertArrayEquals(new int[]{1, 1, 2, 4, 5, 6, 6, 8}, array);
        AccumulatorUtils.decrementBaseCountByStrandArray(array, 'G', true);
        assertArrayEquals(new int[]{1, 1, 2, 4, 4, 6, 6, 8}, array);
        AccumulatorUtils.decrementBaseCountByStrandArray(array, 'G', false);
        assertArrayEquals(new int[]{1, 1, 2, 4, 4, 5, 6, 8}, array);
    }

    @Test
    public void longToKeepFromOverlapPair() {
        int[] array = new int[]{1, 2, 3, 4, 5, 6, 7, 8};
        TLongList list = new TLongArrayList();
        long l1 = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(true, false, (byte) 'T', (byte) 10, 12345);
        long l2 = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(true, false, (byte) 'T', (byte) 10, 12345);
        list.add(l1);
        list.add(l2);
        assertEquals(l1, AccumulatorUtils.longToKeepFromOverlapPair(list, array));
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6, 6, 8}, array);

        list = new TLongArrayList();
        l1 = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(true, false, (byte) 'T', (byte) 10, 12345);
        l2 = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(false, false, (byte) 'T', (byte) 10, 12345);
        list.add(l1);
        list.add(l2);
        assertEquals(l1, AccumulatorUtils.longToKeepFromOverlapPair(list, array));
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6, 6, 7}, array);

        list = new TLongArrayList();
        l1 = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(false, false, (byte) 'G', (byte) 10, 12345);
        l2 = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(false, false, (byte) 'G', (byte) 20, 12345);
        list.add(l1);
        list.add(l2);
        assertEquals(l2, AccumulatorUtils.longToKeepFromOverlapPair(list, array));
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 5, 6, 7}, array);

        list = new TLongArrayList();
        l1 = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(false, false, (byte) 'G', (byte) 10, 12345);
        l2 = AccumulatorUtils.convertStrandEORBaseQualAndPositionToLong(true, false, (byte) 'G', (byte) 20, 12345);
        list.add(l1);
        list.add(l2);
        assertEquals(l1, AccumulatorUtils.longToKeepFromOverlapPair(list, array));
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 4, 6, 7}, array);

    }

    @Test
    public void overlapsTakingIntoAccountStrand() {
        Accumulator acc = new Accumulator(150);
        /*
         * add different reads all on same strand
         */
        acc.addBase((byte) 'G', (byte) 1, false, 100, 150, 200, 1);
        acc.addBase((byte) 'G', (byte) 1, false, 100, 150, 200, 2);
        acc.addBase((byte) 'G', (byte) 1, false, 100, 150, 200, 3);
        acc.addBase((byte) 'G', (byte) 1, false, 100, 150, 200, 4);
        acc.addBase((byte) 'G', (byte) 1, false, 100, 150, 200, 5);
        assertEquals(5, acc.getCoverage());
        AccumulatorUtils.removeOverlappingReads(acc);
        assertEquals(5, acc.getCoverage());
        assertArrayEquals(new int[]{0, 5, 0, 0}, AccumulatorUtils.getCountAndEndOfReadByStrand(acc));

        /*
         * add read with same id on same strand - should be removed
         */
        acc.addBase((byte) 'G', (byte) 1, false, 100, 150, 200, 3);
        assertEquals(6, acc.getCoverage());
        AccumulatorUtils.removeOverlappingReads(acc);
        assertEquals(5, acc.getCoverage());
        assertArrayEquals(new int[]{0, 5, 0, 0}, AccumulatorUtils.getCountAndEndOfReadByStrand(acc));

        /*
         * this time add read with same id on opposite strand - will keep that one and remove the original read
         */
        acc.addBase((byte) 'G', (byte) 1, true, 100, 150, 200, 3);
        assertEquals(6, acc.getCoverage());
        AccumulatorUtils.removeOverlappingReads(acc);
        assertEquals(5, acc.getCoverage());
        assertArrayEquals(new int[]{1, 4, 0, 0}, AccumulatorUtils.getCountAndEndOfReadByStrand(acc));

    }

    @Test
    public void overlaps() {
        Accumulator acc = new Accumulator(150);
        acc.addBase((byte) 'G', (byte) 1, true, 100, 150, 200, 1);
        acc.addBase((byte) 'G', (byte) 1, false, 100, 150, 200, 2);
        assertEquals(2, acc.getCoverage());
        AccumulatorUtils.removeOverlappingReads(acc);
        assertEquals(2, acc.getCoverage());

        /*
         * add another base with the same id
         * count will go up, but back down once removeOverlappingReads is called
         */
        acc.addBase((byte) 'G', (byte) 1, false, 100, 150, 200, 2);
        assertEquals(3, acc.getCoverage());
        AccumulatorUtils.removeOverlappingReads(acc);
        assertEquals(2, acc.getCoverage());

        /*
         * add a different base with the same id
         * count will go up, but back down by 2 once removeOverlappingReads is called
         */
        acc.addBase((byte) 'A', (byte) 1, false, 100, 150, 200, 2);
        assertEquals(3, acc.getCoverage());
        AccumulatorUtils.removeOverlappingReads(acc);
        assertEquals(1, acc.getCoverage());
        assertEquals('G', AccumulatorUtils.getBaseAsCharFromLong(acc.getData().get(1)));
    }

    @Test
    public void getBitSet() {
        TLongList list = new TLongArrayList();
        list.add(576460756598390984L);
        list.add(576460756598390984L);
        list.add(2305843013508661448L);
        list.add(2305843013508661448L);
        BitSet bs1 = AccumulatorUtils.getUniqueBases(list);
        assertTrue(AccumulatorUtils.doesBitSetContainMoreThan1True(bs1));
    }

    @Test
    public void getBitSetAll() {
        TLongList list = new TLongArrayList();
        BitSet bs1 = AccumulatorUtils.getUniqueBasesUseAllList(list);
        assertFalse(AccumulatorUtils.doesBitSetContainMoreThan1True(bs1));
        list.add(0);
        bs1 = AccumulatorUtils.getUniqueBasesUseAllList(list);
        assertFalse(AccumulatorUtils.doesBitSetContainMoreThan1True(bs1));
        list.add(576460756598390984L);
        bs1 = AccumulatorUtils.getUniqueBasesUseAllList(list);
        assertFalse(AccumulatorUtils.doesBitSetContainMoreThan1True(bs1));
        list.add(2305843013508661448L);
        bs1 = AccumulatorUtils.getUniqueBasesUseAllList(list);
        assertTrue(AccumulatorUtils.doesBitSetContainMoreThan1True(bs1));
    }

    @Test
    public void getUniqueBases() {
        Accumulator acc = new Accumulator(150);
        acc.addBase((byte) 'G', (byte) 1, true, 100, 150, 200, 1);
        acc.addBase((byte) 'G', (byte) 1, false, 100, 150, 200, 2);
        BitSet bs = AccumulatorUtils.getUniqueBases(acc);
        assertFalse(bs.get(0));
        assertFalse(bs.get(1));
        assertTrue(bs.get(2));
        assertFalse(bs.get(3));
        assertFalse(AccumulatorUtils.doesBitSetContainMoreThan1True(bs));

        acc.addBase((byte) 'A', (byte) 1, true, 100, 150, 200, 1);
        acc.addBase((byte) 'A', (byte) 1, false, 100, 150, 200, 2);
        bs = AccumulatorUtils.getUniqueBases(acc);
        assertTrue(bs.get(0));
        assertFalse(bs.get(1));
        assertTrue(bs.get(2));
        assertFalse(bs.get(3));
        assertTrue(AccumulatorUtils.doesBitSetContainMoreThan1True(bs));

        acc.addBase((byte) 'T', (byte) 1, true, 100, 150, 200, 1);
        acc.addBase((byte) 'T', (byte) 1, false, 100, 150, 200, 2);
        bs = AccumulatorUtils.getUniqueBases(acc);
        assertTrue(bs.get(0));
        assertFalse(bs.get(1));
        assertTrue(bs.get(2));
        assertTrue(bs.get(3));
        assertTrue(AccumulatorUtils.doesBitSetContainMoreThan1True(bs));

        acc.addBase((byte) 'C', (byte) 1, true, 100, 150, 200, 1);
        acc.addBase((byte) 'C', (byte) 1, false, 100, 150, 200, 2);
        bs = AccumulatorUtils.getUniqueBases(acc);
        assertTrue(bs.get(0));
        assertTrue(bs.get(1));
        assertTrue(bs.get(2));
        assertTrue(bs.get(3));
        assertTrue(AccumulatorUtils.doesBitSetContainMoreThan1True(bs));
    }

    @Test
    public void doBitSetsMatch() {
        assertFalse(AccumulatorUtils.doBitSetsHaveSameBitsSet(null, null));
        assertTrue(AccumulatorUtils.doBitSetsHaveSameBitsSet(new BitSet(4), new BitSet(4)));
        assertTrue(AccumulatorUtils.doBitSetsHaveSameBitsSet(new BitSet(4), new BitSet(5)));
        BitSet bs1 = new BitSet(4);
        bs1.set(0);
        BitSet bs2 = new BitSet(4);
        bs2.set(0);
        assertTrue(AccumulatorUtils.doBitSetsHaveSameBitsSet(bs1, bs2));
        bs2.set(1);
        assertFalse(AccumulatorUtils.doBitSetsHaveSameBitsSet(bs1, bs2));
        bs1.set(1);
        assertTrue(AccumulatorUtils.doBitSetsHaveSameBitsSet(bs1, bs2));

        bs1.set(0, 3);
        bs2.set(0, 3);
        assertTrue(AccumulatorUtils.doBitSetsHaveSameBitsSet(bs1, bs2));
        bs1.set(0, 3, false);
        assertFalse(AccumulatorUtils.doBitSetsHaveSameBitsSet(bs1, bs2));
        bs2.set(0, 3, false);
        assertTrue(AccumulatorUtils.doBitSetsHaveSameBitsSet(bs1, bs2));
    }

    @Test
    public void bothStrandsAsPerc() {
        assertFalse(AccumulatorUtils.areBothStrandsRepresented(0, 0, 0));
        assertFalse(AccumulatorUtils.areBothStrandsRepresented(0, 1, 0));
        assertTrue(AccumulatorUtils.areBothStrandsRepresented(1, 1, 0));
        assertFalse(AccumulatorUtils.areBothStrandsRepresented(1, 1, 100));
        assertFalse(AccumulatorUtils.areBothStrandsRepresented(10, 10, 100));
        assertFalse(AccumulatorUtils.areBothStrandsRepresented(10, 10, 50));
        assertFalse(AccumulatorUtils.areBothStrandsRepresented(10, 10, 51));
        assertTrue(AccumulatorUtils.areBothStrandsRepresented(10, 10, 49));
    }
}
