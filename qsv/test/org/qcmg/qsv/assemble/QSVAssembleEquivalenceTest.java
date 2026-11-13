package org.qcmg.qsv.assemble;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.qcmg.qsv.splitread.UnmappedRead;
import org.qcmg.qsv.util.QSVUtil;

/**
 * Checks that QSVAssemble gives exactly the same results as the original implementation (QSVAssembleOriginal)
 */
public class QSVAssembleEquivalenceTest {

	private static final char[] BASES = {'A', 'C', 'G', 'T'};

	private static String randomSequence(Random r, int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(BASES[r.nextInt(4)]);
		}
		return sb.toString();
	}

	private static String repetitiveSequence(Random r, int length) {
		String unit = randomSequence(r, 1 + r.nextInt(4));
		StringBuilder sb = new StringBuilder(length);
		while (sb.length() < length) {
			sb.append(r.nextInt(10) == 0 ? randomSequence(r, 3) : unit);
		}
		return sb.substring(0, length);
	}

	/*
	 * Reads taken from a template, some reverse complemented, with errors and Ns, and with read names that are sometimes shared (as mates are)
	 */
	private static List<String[]> randomReads(Random r, String template, int count) {
		List<String[]> reads = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			int length = 30 + r.nextInt(Math.min(120, template.length() - 30));
			int start = r.nextInt(template.length() - length + 1);
			StringBuilder seq = new StringBuilder(template.substring(start, start + length));
			int errors = r.nextInt(4);
			for (int e = 0; e < errors; e++) {
				seq.setCharAt(r.nextInt(seq.length()), r.nextInt(5) == 0 ? 'N' : BASES[r.nextInt(4)]);
			}
			String s = r.nextInt(4) == 0 ? QSVUtil.reverseComplement(seq.toString()) : seq.toString();
			// about a quarter of the reads share a name with an earlier read
			String name = ( ! reads.isEmpty() && r.nextInt(4) == 0) ? reads.get(r.nextInt(reads.size()))[0] : "read_" + i;
			reads.add(new String[] {name, s});
		}
		return reads;
	}

	private static List<Read> toReads(List<String[]> reads) throws Exception {
		List<Read> list = new ArrayList<>();
		for (String[] read : reads) {
			list.add(new Read(read[0], read[1]));
		}
		return list;
	}

	private static List<UnmappedRead> toUnmappedReads(List<String[]> reads) {
		List<UnmappedRead> list = new ArrayList<>();
		for (String[] read : reads) {
			list.add(new UnmappedRead("unmapped," + read[0] + ",chr1,1000," + read[1], true));
		}
		return list;
	}

	private static String describe(ConsensusRead c) {
		return c == null ? "null" : c.getHeader() + "|" + c.getSequence() + "|" + c.getClipMateSequence() + "|" + c.getReferenceSequence();
	}

	private static String template(Random r) {
		return r.nextInt(5) == 0 ? repetitiveSequence(r, 150 + r.nextInt(300)) : randomSequence(r, 150 + r.nextInt(300));
	}

	@Test
	public void getFinalContigMatchesOriginal() throws Exception {
		Random r = new Random(1);
		int assembled = 0;
		for (int i = 0; i < 1500; i++) {
			List<String[]> reads = randomReads(r, template(r), 1 + r.nextInt(60));
			String expected = describe(new QSVAssembleOriginal().getFinalContig(toUnmappedReads(reads)));
			String actual = describe(new QSVAssemble().getFinalContig(toUnmappedReads(reads)));
			assertEquals("iteration " + i, expected, actual);
			if ( ! "null".equals(actual)) assembled++;
		}
		assertEquals(true, assembled > 750);
	}

	@Test
	public void getContigsMatchesOriginal() throws Exception {
		Random r = new Random(2);
		int assembled = 0;
		for (int i = 0; i < 1500; i++) {
			String template = template(r);
			List<String[]> reads = randomReads(r, template, r.nextInt(60));
			int clipStart = r.nextInt(template.length() - 40);
			String clip = template.substring(clipStart, clipStart + 20 + r.nextInt(20));
			boolean clipReverse = r.nextBoolean();
			boolean isLeft = r.nextBoolean();
			int bp = r.nextInt(1000000);

			QSVAssembleOriginal original = new QSVAssembleOriginal();
			String expected = describe(original.getContigs(bp, new Read("clip", clip), toReads(reads), clipReverse, isLeft));
			QSVAssemble assemble = new QSVAssemble();
			String actual = describe(assemble.getContigs(bp, new Read("clip", clip), toReads(reads), clipReverse, isLeft));
			assertEquals("iteration " + i, expected, actual);
			assertEquals("iteration " + i, original.getMatchingSplitReads(), assemble.getMatchingSplitReads());
			if (assemble.getMatchingSplitReads() > 0) assembled++;
		}
		assertEquals(true, assembled > 750);
	}

	@Test
	public void getFinalClipContigMatchesOriginal() throws Exception {
		Random r = new Random(3);
		for (int i = 0; i < 2000; i++) {
			String template = template(r);
			int leftStart = r.nextInt(template.length() / 2);
			String left = template.substring(leftStart, leftStart + 20 + r.nextInt(template.length() / 2 - 20));
			int rightStart = r.nextInt(template.length() - 30);
			String right = template.substring(rightStart, rightStart + 20 + r.nextInt(Math.min(100, template.length() - rightStart - 20)));
			if (r.nextInt(3) == 0) {
				right = QSVUtil.reverseComplement(right);
			}
			assertEquals("iteration " + i, new QSVAssembleOriginal().getFinalClipContig(left, right), new QSVAssemble().getFinalClipContig(left, right));
		}
	}

	@Test
	public void removeReadsRemovesFirstOccurrencesOfEachHeader() throws Exception {
		List<Read> reads = toReads(List.of(new String[] {"a", "AAAAAAAAAAC"}, new String[] {"b", "AAAAAAAAAAG"}, new String[] {"a", "AAAAAAAAAAT"},
				new String[] {"a", "AAAAAAAAAAA"}, new String[] {"c", "CCCCCCCCCCC"}));
		java.util.Map<String, Integer> removals = new java.util.HashMap<>();
		removals.put("a", 2);
		removals.put("c", 3);
		QSVAssemble.removeReads(reads, removals);
		assertEquals(2, reads.size());
		assertEquals("AAAAAAAAAAG", reads.get(0).getSequence());
		assertEquals("AAAAAAAAAAA", reads.get(1).getSequence());
	}
}
