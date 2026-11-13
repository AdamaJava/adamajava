package org.qcmg.common.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Test;

/**
 * Checks that StringUtils.getStartPositionsAndLengthOfSubStrings gives exactly the same results as the original implementation
 */
public class SubStringTilesEquivalenceTest {

	private static final char[] BASES = {'A', 'C', 'G', 'T'};

	/*
	 * verbatim copy of the original implementation
	 */
	static int[][] original(String reference, String sequence) {
		List<int[]> results = new ArrayList<>();
		int previousRefStartPosition = 0;
		for (int i = 0, maxLength = sequence.length() - 13; i < maxLength; i++) {
			String nextSeqTile = sequence.substring(i, i + 13);
			int refNextStartPosition = reference.indexOf(nextSeqTile, previousRefStartPosition);
			if (refNextStartPosition > -1) {
				int nextBlockLength = StringUtils.getLengthOfSubStringMatch(reference.substring(refNextStartPosition), sequence.substring(i));
				results.add(new int[]{refNextStartPosition, i, nextBlockLength});
				i += (nextBlockLength - 1);
				previousRefStartPosition = refNextStartPosition + nextBlockLength;
			}
		}
		return results.toArray(new int[][]{});
	}

	private static String randomSequence(Random r, int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(BASES[r.nextInt(4)]);
		}
		return sb.toString();
	}

	/*
	 * mostly random, with some repeats, some soft masked (lower case) stretches and some Ns
	 */
	private static String reference(Random r, int length) {
		StringBuilder sb = new StringBuilder(length);
		while (sb.length() < length) {
			int type = r.nextInt(10);
			int len = 1 + r.nextInt(200);
			if (type == 0) {
				sb.append(randomSequence(r, 1 + r.nextInt(4)).repeat(1 + len / 4));
			} else if (type == 1) {
				sb.append(randomSequence(r, len).toLowerCase());
			} else if (type == 2) {
				sb.append("N".repeat(1 + r.nextInt(20)));
			} else {
				sb.append(randomSequence(r, len));
			}
		}
		return sb.substring(0, length);
	}

	private static String mutate(Random r, String s, int errors) {
		StringBuilder sb = new StringBuilder(s);
		for (int e = 0; e < errors && sb.length() > 0; e++) {
			int pos = r.nextInt(sb.length());
			switch (r.nextInt(5)) {
				case 0 -> sb.deleteCharAt(pos);
				case 1 -> sb.insert(pos, BASES[r.nextInt(4)]);
				case 2 -> sb.setCharAt(pos, 'N');
				default -> sb.setCharAt(pos, BASES[r.nextInt(4)]);
			}
		}
		return sb.toString();
	}

	/*
	 * sequence made up of 1 to 4 pieces of the reference (in order, as for a split read, or not), with errors, upper cased as reads are
	 */
	private static String sequence(Random r, String reference) {
		StringBuilder sb = new StringBuilder();
		int pieces = 1 + r.nextInt(4);
		for (int p = 0; p < pieces; p++) {
			int len = 1 + r.nextInt(Math.min(120, reference.length()));
			int start = r.nextInt(reference.length() - len + 1);
			sb.append(reference, start, start + len);
		}
		if (r.nextInt(5) == 0) {
			sb.append(randomSequence(r, r.nextInt(40)));
		}
		String s = mutate(r, sb.toString(), r.nextInt(6));
		return r.nextInt(4) == 0 ? s : s.toUpperCase();
	}

	private static void assertSame(String reference, String sequence) {
		int[][] expected = original(reference, sequence);
		int[][] actual = StringUtils.getStartPositionsAndLengthOfSubStrings(reference, sequence);
		assertEquals("ref length: " + reference.length() + ", seq: " + sequence, Arrays.deepToString(expected), Arrays.deepToString(actual));
	}

	@Test
	public void matchesOriginal() {
		Random r = new Random(5);
		int[] referenceLengths = {20, 200, 1999, 2000, 5000, 50000, 300000};
		int withBlocks = 0;
		int total = 0;
		for (int refLength : referenceLengths) {
			int iterations = refLength > 10000 ? 150 : 1500;
			for (int i = 0; i < iterations; i++) {
				String ref = reference(r, refLength);
				String seq = sequence(r, ref);
				assertSame(ref, seq);
				total++;
				if (StringUtils.getStartPositionsAndLengthOfSubStrings(ref, seq).length > 1) {
					withBlocks++;
				}
			}
		}
		// make sure that we are looking at sequences with more than 1 block
		assertTrue(withBlocks + " of " + total, withBlocks > total / 4);
	}

	@Test
	public void edgeCases() {
		Random r = new Random(6);
		String ref = reference(r, 5000);
		assertSame(ref, "");
		assertSame(ref, "ACGT");
		assertSame(ref, ref.substring(100, 113).toUpperCase());
		assertSame(ref, ref.substring(100, 114).toUpperCase());
		assertSame(ref, ref.substring(100, 115).toUpperCase());
		assertSame("", "ACGTACGTACGTACGTACGT");
		assertSame("ACGTACGTACGTACG", "ACGTACGTACGTACGTACGT");
		assertSame(ref, "N".repeat(50) + ref.substring(4000, 4100));
		// tile only occurs before the previous match
		String a = randomSequence(r, 30);
		String b = randomSequence(r, 30);
		String c = randomSequence(r, 4000);
		assertSame(b + a + c + b, a + "TTTTT" + b + "GGGGG" + b);
		// lots of repeats of the same tile
		assertSame("ACGTACGTACGTACGTAC".repeat(500), "ACGTACGTACGTACGTAC".repeat(3) + "TTT" + "ACGTACGTACGTACGTAC".repeat(5));
	}

	@Test
	public void lengthOfSubStringMatchWithOffsets() {
		assertEquals(StringUtils.getLengthOfSubStringMatch("ABCDEF".substring(2), "XCDEZ".substring(1)), StringUtils.getLengthOfSubStringMatch("ABCDEF", 2, "XCDEZ", 1));
		assertEquals(0, StringUtils.getLengthOfSubStringMatch("ABC", 3, "ABC", 0));
		assertEquals(3, StringUtils.getLengthOfSubStringMatch("ABC", 0, "ABCD", 0));
	}
}
