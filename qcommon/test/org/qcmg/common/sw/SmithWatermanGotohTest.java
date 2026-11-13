package org.qcmg.common.sw;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

public class SmithWatermanGotohTest {

	private static final char[] BASES = {'A', 'C', 'G', 'T'};

	/*
	 * the two parameter sets used by TiledAlignerUtil.getIntelligentSwDiffs
	 */
	private static final int[][] SCORES = {{5, -4, 16, 4}, {4, -14, 14, 1}};

	private static String randomSequence(Random r, int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(BASES[r.nextInt(4)]);
		}
		return sb.toString();
	}

	/*
	 * Introduces substitutions, insertions, deletions and Ns at roughly the given rate
	 */
	private static String mutate(Random r, String s, double rate) {
		StringBuilder sb = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			double d = r.nextDouble();
			if (d < rate / 4) {
				sb.append(BASES[r.nextInt(4)]);				// substitution
			} else if (d < rate / 2) {
				sb.append(s.charAt(i)).append(randomSequence(r, 1 + r.nextInt(10)));	// insertion
			} else if (d < 3 * rate / 4) {
				i += r.nextInt(10);						// deletion
			} else if (d < rate) {
				sb.append('N');
			} else {
				sb.append(s.charAt(i));
			}
		}
		return sb.toString();
	}

	private static void assertSameAsOriginal(String ref, String seq) {
		for (int[] sc : SCORES) {
			String[] expected = new SmithWatermanGotohOriginal(ref, seq, sc[0], sc[1], sc[2], sc[3]).traceback();
			String[] actual = new SmithWatermanGotoh(ref, seq, sc[0], sc[1], sc[2], sc[3]).traceback();
			assertArrayEquals("ref: " + ref + ", seq: " + seq + ", scores: " + sc[0] + "," + sc[1] + "," + sc[2] + "," + sc[3], expected, actual);
		}
	}

	@Test
	public void matchesOriginalOnRandomAlignments() {
		Random r = new Random(42);
		for (int i = 0; i < 3000; i++) {
			/*
			 * reference with flanking buffer either side of the region the sequence came from, as in TiledAlignerUtil.smithWaterman
			 */
			String core = randomSequence(r, 20 + r.nextInt(300));
			String ref = randomSequence(r, r.nextInt(80)) + core + randomSequence(r, r.nextInt(80));
			String seq = mutate(r, core, r.nextDouble() * 0.3);
			if (seq.isEmpty()) {
				continue;
			}
			assertSameAsOriginal(ref, seq);
			// and the other way round, so that sequence B is longer than A
			assertSameAsOriginal(seq, ref);
		}
	}

	@Test
	public void matchesOriginalOnRepetitiveAndUnrelatedSequences() {
		Random r = new Random(7);
		String[] repeats = {"A", "AC", "CAG", "TTAGGG"};
		for (int i = 0; i < 500; i++) {
			String unit = repeats[r.nextInt(repeats.length)];
			String ref = unit.repeat(10 + r.nextInt(60)) + randomSequence(r, r.nextInt(50));
			String seq = mutate(r, unit.repeat(5 + r.nextInt(40)), 0.1);
			if ( ! seq.isEmpty()) {
				assertSameAsOriginal(ref, seq);
			}
			// completely unrelated sequences
			assertSameAsOriginal(randomSequence(r, 1 + r.nextInt(200)), randomSequence(r, 1 + r.nextInt(200)));
		}
	}

	/*
	 * non-integer gap penalties use the float code path rather than the int one
	 */
	@Test
	public void matchesOriginalWithFractionalGapPenalties() {
		Random r = new Random(99);
		for (int i = 0; i < 500; i++) {
			String core = randomSequence(r, 20 + r.nextInt(200));
			String ref = randomSequence(r, r.nextInt(60)) + core + randomSequence(r, r.nextInt(60));
			String seq = mutate(r, core, r.nextDouble() * 0.3);
			if (seq.isEmpty()) {
				continue;
			}
			String[] expected = new SmithWatermanGotohOriginal(ref, seq, 5, -4, 16.5f, 3.75f).traceback();
			String[] actual = new SmithWatermanGotoh(ref, seq, 5, -4, 16.5f, 3.75f).traceback();
			assertArrayEquals(expected, actual);
		}
	}

	@Test
	public void edgeCases() {
		assertSameAsOriginal("A", "A");
		assertSameAsOriginal("A", "C");
		assertSameAsOriginal("ACGT", "TGCA");
		assertSameAsOriginal("NNNNNNNN", "NNNN");
		assertSameAsOriginal("ACGTACGTACGTACGT", "ACGTACGTTTTTTTTTTTTTACGTACGT");
	}

	@Test
	public void perfectMatch() {
		String[] result = new SmithWatermanGotoh("TTTTACGTACGTGGGG", "ACGTACGT", 5, -4, 16, 4).traceback();
		assertEquals("ACGTACGT", result[0]);
		assertEquals("||||||||", result[1]);
		assertEquals("ACGTACGT", result[2]);
	}
}
