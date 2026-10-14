package org.qcmg.sig;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;
import org.qcmg.sig.model.Comparison;
import org.qcmg.sig.util.ComparisonUtil;

import gnu.trove.map.hash.TIntByteHashMap;

public class PackedGenotypesTest {
    private static final byte[] GENOTYPES = {16, 32, 64, -128, 3, 5, 9, 6, 10, 12};

    @Test
    public void everyGenotypePairAndWordBoundary() {
        for (byte first : GENOTYPES) {
            for (byte second : GENOTYPES) {
                TIntByteHashMap left = new TIntByteHashMap();
                TIntByteHashMap right = new TIntByteHashMap();
                for (int position : new int[]{Integer.MIN_VALUE, -65, -64, -1, 0, 1, 63, 64, 65,
                        127, 128, Integer.MAX_VALUE}) {
                    left.put(position, first);
                    right.put(position, second);
                }
                assertEquivalent(left, right);
            }
        }
    }

    @Test
    public void emptyDisjointAndInvalidGenotypes() {
        TIntByteHashMap left = new TIntByteHashMap();
        TIntByteHashMap right = new TIntByteHashMap();
        assertEquivalent(left, right);
        left.put(0, (byte) 16);
        assertEquivalent(left, right);
        right.put(64, (byte) 16);
        assertEquivalent(left, right);
        // Invalid codes must neither overlap nor match, even when equal.
        for (int code = -128; code < 128; code++) {
            left.put(code, (byte) code);
            right.put(code, (byte) code);
        }
        assertEquivalent(left, right);
    }

    @Test
    public void randomizedDenseAndSparseSignaturesMatchHashComparisons() {
        Random random = new Random(20261013);
        for (int trial = 0; trial < 200; trial++) {
            TIntByteHashMap left = new TIntByteHashMap();
            TIntByteHashMap right = new TIntByteHashMap();
            for (int i = 0; i < 2000; i++) {
                int position = trial % 2 == 0 ? i : random.nextInt();
                if (random.nextBoolean()) {
                    left.put(position, GENOTYPES[random.nextInt(GENOTYPES.length)]);
                }
                if (random.nextBoolean()) {
                    right.put(position, GENOTYPES[random.nextInt(GENOTYPES.length)]);
                }
            }
            assertEquivalent(left, right);
            assertEquivalent(right, left);
        }
    }

    private static void assertEquivalent(TIntByteHashMap left, TIntByteHashMap right) {
        Comparison expected = ComparisonUtil.compareRatiosUsingSnpsFloat(left, right, "left", "right");
        Comparison actual = new PackedGenotypes(left).compare(new PackedGenotypes(right), "left", "right");
        assertEquals(expected.getMainCoverage(), actual.getMainCoverage());
        assertEquals(expected.getTestCoverage(), actual.getTestCoverage());
        assertEquals(expected.getOverlapCoverage(), actual.getOverlapCoverage());
        assertEquals(expected.getScore(), actual.getScore(), 0.0);
    }
}
