package au.edu.qimr.tiledaligner.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.BLATRecord;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.NumberUtils;

import au.edu.qimr.tiledaligner.PositionChrPositionMap.LongRange;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * Checks that running the tiled aligner over multiple threads gives exactly the same results as running it on one.
 */
public class TiledAlignerUtilThreadsTest {

	private static final char[] BASES = {'A', 'C', 'G', 'T'};

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

	private static String randomSequence(Random r, int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(BASES[r.nextInt(4)]);
		}
		return sb.toString();
	}

	private static String mutate(Random r, String s, int substitutions) {
		StringBuilder sb = new StringBuilder(s);
		for (int i = 0; i < substitutions; i++) {
			sb.setCharAt(r.nextInt(sb.length()), BASES[r.nextInt(4)]);
		}
		return sb.toString();
	}

	@Test
	public void multiThreadedResultsMatchSingleThreaded() throws IOException {
		Random r = new Random(13);
		String[] contigs = {"chr1", "chr2"};
		String[] bases = {randomSequence(r, 4000), randomSequence(r, 4000)};

		/*
		 * reference fasta and index
		 */
		File fa = testFolder.newFile("ref.fa");
		File fai = new File(fa.getAbsolutePath() + ".fai");
		StringBuilder fasta = new StringBuilder();
		StringBuilder index = new StringBuilder();
		long offset = 0;
		for (int i = 0; i < contigs.length; i++) {
			String header = ">" + contigs[i] + "\n";
			offset += header.length();
			fasta.append(header).append(bases[i]).append('\n');
			index.append(contigs[i]).append('\t').append(bases[i].length()).append('\t').append(offset).append('\t').append(bases[i].length()).append('\t').append(bases[i].length() + 1).append('\n');
			offset += bases[i].length() + 1;
		}
		Files.writeString(fa.toPath(), fasta);
		Files.writeString(fai.toPath(), index);
		Map<ChrPosition, LongRange> refIndexMap = TiledAlignerUtil.loadReferenceIndexMap(fa.getAbsolutePath(), fai.getAbsolutePath());

		/*
		 * tile cache: every 13-mer and its (1-based, genome wide) start positions
		 */
		Map<Integer, TIntArrayList> tiles = new HashMap<>();
		int genomeOffset = 1;
		for (String contig : bases) {
			for (int i = 0; i + TiledAlignerUtil.TILE_LENGTH <= contig.length(); i++) {
				tiles.computeIfAbsent(NumberUtils.convertTileToInt(contig.substring(i, i + TiledAlignerUtil.TILE_LENGTH)), k -> new TIntArrayList()).add(genomeOffset + i);
			}
			genomeOffset += contig.length();
		}
		TIntObjectMap<int[]> cache = new TIntObjectHashMap<>();
		tiles.forEach((k, v) -> cache.put(k, v.toArray()));

		/*
		 * sequences to align: exact and mutated fragments, plus fragments that are split across the two contigs
		 */
		Map<String, String> sequences = new LinkedHashMap<>();
		for (int i = 0; i < 60; i++) {
			int c = r.nextInt(2);
			int length = 60 + r.nextInt(150);
			int start = r.nextInt(bases[c].length() - length);
			String fragment = bases[c].substring(start, start + length);
			switch (i % 3) {
				case 0 -> sequences.put(fragment, "frag_" + i);
				case 1 -> sequences.put(mutate(r, fragment, 1 + r.nextInt(5)), "mutated_" + i);
				default -> {
					int other = 1 - c;
					int otherStart = r.nextInt(bases[other].length() - 80);
					sequences.put(fragment.substring(0, length / 2) + bases[other].substring(otherStart, otherStart + 80), "splitcon_" + contigs[c] + "_" + start + "_" + contigs[other] + "_" + otherStart);
				}
			}
		}

		Map<String, List<BLATRecord>> single = TiledAlignerUtil.runTiledAlignerCache(fa.getAbsolutePath(), refIndexMap, cache, sequences, TiledAlignerUtil.TILE_LENGTH, "test", false, false, 1);
		Map<String, List<BLATRecord>> multi = TiledAlignerUtil.runTiledAlignerCache(fa.getAbsolutePath(), refIndexMap, cache, sequences, TiledAlignerUtil.TILE_LENGTH, "test", false, false, 4);

		assertEquals(sequences.size(), single.size());
		assertEquals(new ArrayList<>(single.keySet()), new ArrayList<>(multi.keySet()));
		int nonEmpty = 0;
		for (Map.Entry<String, List<BLATRecord>> entry : single.entrySet()) {
			assertEquals(entry.getValue().toString(), multi.get(entry.getKey()).toString());
			if ( ! entry.getValue().isEmpty()) {
				nonEmpty++;
			}
		}
		// make sure the test is actually aligning things
		assertTrue("only " + nonEmpty + " sequences aligned", nonEmpty > sequences.size() / 2);
	}
}
