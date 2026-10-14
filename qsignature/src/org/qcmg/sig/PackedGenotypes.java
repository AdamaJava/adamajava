package org.qcmg.sig;

import java.util.Arrays;

import org.qcmg.sig.model.Comparison;
import org.qcmg.sig.util.SignatureUtil;

import gnu.trove.map.hash.TIntByteHashMap;

/**
 * Immutable genotypes packed into four allele bit planes and a coverage plane.
 * Position IDs are shared by the signature loader. Only occupied 64-position
 * blocks are stored, so sparse or widely separated IDs do not waste memory.
 */
final class PackedGenotypes {
    private final int coverage;
    private final int[] blocks;
    private final long[] present;
    private final long[] a;
    private final long[] c;
    private final long[] g;
    private final long[] t;

    PackedGenotypes(TIntByteHashMap genotypes) {
        coverage = genotypes.size();
        int[] positions = genotypes.keys();
        Arrays.sort(positions);
        int count = 0;
        int previous = 0;
        for (int position : positions) {
            if (SignatureUtil.isCodedGenotypeValid(genotypes.get(position))) {
                int block = position >> 6;
                if (count == 0 || block != previous) {
                    count++;
                    previous = block;
                }
            }
        }
        blocks = new int[count];
        present = new long[count];
        a = new long[count];
        c = new long[count];
        g = new long[count];
        t = new long[count];
        int index = -1;
        for (int position : positions) {
            byte genotype = genotypes.get(position);
            if (!SignatureUtil.isCodedGenotypeValid(genotype)) {
                continue;
            }
            int block = position >> 6;
            if (index < 0 || blocks[index] != block) {
                blocks[++index] = block;
            }
            long bit = 1L << (position & 63);
            // Homozygotes use the upper nibble, heterozygotes the lower nibble.
            int alleles = (genotype & 0xff) >= 16 ? (genotype & 0xff) >>> 4 : genotype;
            present[index] |= bit;
            if ((alleles & 1) != 0) a[index] |= bit;
            if ((alleles & 2) != 0) c[index] |= bit;
            if ((alleles & 4) != 0) g[index] |= bit;
            if ((alleles & 8) != 0) t[index] |= bit;
        }
    }

    int size() {
        return coverage;
    }

    Comparison compare(PackedGenotypes other, String file1, String file2) {
        int overlap = 0;
        int matches = 0;
        int i = 0;
        int j = 0;
        while (i < blocks.length && j < other.blocks.length) {
            if (blocks[i] < other.blocks[j]) {
                i++;
            } else if (blocks[i] > other.blocks[j]) {
                j++;
            } else {
                long common = present[i] & other.present[j];
                long different = (a[i] ^ other.a[j]) | (c[i] ^ other.c[j])
                        | (g[i] ^ other.g[j]) | (t[i] ^ other.t[j]);
                overlap += Long.bitCount(common);
                matches += Long.bitCount(common & ~different);
                i++;
                j++;
            }
        }
        return new Comparison(file1, coverage, file2, other.coverage, matches, overlap);
    }
}
