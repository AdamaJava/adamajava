/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2020.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */

package au.edu.qimr.tiledaligner.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import au.edu.qimr.tiledaligner.PositionChrPositionMap;
import au.edu.qimr.tiledaligner.model.IntLongPair;
import au.edu.qimr.tiledaligner.model.IntLongPairs;
import au.edu.qimr.tiledaligner.model.TARecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.common.model.BLATRecord;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionName;
import org.qcmg.common.util.NumberUtils;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TIntObjectMap;

public class TARecordUtilTest {
	
	public static PositionChrPositionMap pcpm;
	
	@BeforeClass
	public static void setup() {
		/*
		 * setup cache
		 */
		pcpm = new PositionChrPositionMap();
		pcpm.loadMap(PositionChrPositionMap.grch37Positions);
	}
	
	@Test
	public void getSplits() {
		String seq = "AAAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDDEEEEEEEEEEFFFFFFFFFF";
		Map<Integer, TLongList> map = new HashMap<>();
		TLongList list = new TLongArrayList();
		long position = 10000000;
		short tilePositionInSequence = 0;
		boolean forwardStrand = true;
		list.add(getLong(position, tilePositionInSequence, forwardStrand));
		int key = NumberUtils.pack2IntsInto1(15, 0);
		map.put(key, list);
		TARecord rec = new TARecord(seq, map);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(rec);
		assertEquals(true, splits.isEmpty());
		/*
		 * add another  
		 */
		TLongList list2 = new TLongArrayList();
		position = 20000000;
		tilePositionInSequence = 30;
		list2.add(getLong(position, tilePositionInSequence, forwardStrand));
		key = NumberUtils.pack2IntsInto1(14, 0);
		map.put(key, list2);
		rec = new TARecord(seq, map);
		splits = TARecordUtil.getSplitStartPositions(rec);
		assertEquals(1, splits.size());
		/*
		 * key in the map has changed from combined tile count to combined bases covered 
		 */
		assertEquals(27 + 26, splits.keys()[0]);
	}
	
	@Test
	public void testSort() {
		int[] tileCounts = new int[]{NumberUtils.pack2IntsInto1(10, 0), NumberUtils.pack2IntsInto1(9, 0), NumberUtils.pack2IntsInto1(12, 0)};
		tileCounts = TARecordUtil.sortTileCount(tileCounts);
		assertEquals(NumberUtils.pack2IntsInto1(12, 0), tileCounts[2]);
		assertEquals(NumberUtils.pack2IntsInto1(9, 0), tileCounts[0]);
		
		tileCounts = new int[]{NumberUtils.pack2IntsInto1(20, 0), NumberUtils.pack2IntsInto1(19, 0), NumberUtils.pack2IntsInto1(12, 0)};
		tileCounts = TARecordUtil.sortTileCount(tileCounts);
		assertEquals(NumberUtils.pack2IntsInto1(20, 0), tileCounts[2]);
		assertEquals(NumberUtils.pack2IntsInto1(12, 0), tileCounts[0]);
		
		/*
		 * now with some mismatches
		 */
		tileCounts = new int[]{NumberUtils.pack2IntsInto1(20, 1), NumberUtils.pack2IntsInto1(19, 0), NumberUtils.pack2IntsInto1(12, 0)};
		tileCounts = TARecordUtil.sortTileCount(tileCounts);
		assertEquals(NumberUtils.pack2IntsInto1(19, 0), tileCounts[2]);
		assertEquals(NumberUtils.pack2IntsInto1(12, 0), tileCounts[0]);
		
		tileCounts = new int[]{NumberUtils.pack2IntsInto1(20, 1), NumberUtils.pack2IntsInto1(20, 2), NumberUtils.pack2IntsInto1(12, 4)};
		tileCounts = TARecordUtil.sortTileCount(tileCounts);
		assertEquals(NumberUtils.pack2IntsInto1(20, 1), tileCounts[2]);
		assertEquals(NumberUtils.pack2IntsInto1(12, 4), tileCounts[0]);
	}
	
	@Test
	public void realLifeSplits() {
		/*
		 * splitcon_chr7_100867120_chr7_100867215
		 */
		String seq = "TGGAAAGAAAAAGAAGAAAACCCGAGGTGATCATTTTAAACTTCGCTTCCGAAAAAAACTTTCAGGCCCTGTTGGAGGAGCAGAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGACCCACCAGGAGACCAGGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGAGAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCATCACCCAATGCAGGGGGAGCTCTTCCTGGACCAAGGGAGGAGCCGCTCATTCACCCG";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr7_100867120_chr7_100867215";
		
		countPosition.put(NumberUtils.getTileCount(165, 0), TARecordUtil.getLongList(86862753118281l));
		countPosition.put(NumberUtils.getTileCount(135, 0), TARecordUtil.getLongList(274879241468242l));
		countPosition.put(NumberUtils.getTileCount(44, 0), TARecordUtil.getLongList(1100846151560l));
		countPosition.put(NumberUtils.getTileCount(19, 0), TARecordUtil.getLongList(57175939168186l));
		countPosition.put(NumberUtils.getTileCount(8, 0), TARecordUtil.getLongList(3299873399669l));
		System.out.println("seq.length(): "  + seq.length());
		
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(1, splits.size());
		
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
				.filter(br -> br.getScore() > passingScore)
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		assertEquals(1, results.size());
		System.out.println(results.get(0).toString());
		assertEquals("393	396	0	0	0	0	0	3	209	+	splitcon_chr7_100867120_chr7_100867215	398	1	396	chr7	159138663	100866756	100867361	4	51,31,167,147	1,52,83,250	100866756,100866806,100866953,100867214", results.get(0).toString());
		
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, "splitcon_chr7_100867120_chr7_100867215", r.getSequence().length(), pcpm);
		assertEquals(1, blatRecs.size());
//		for (BLATRecord br : blatRecs) {
//			System.out.println("blat record: " + br.toString());
//		}
		assertEquals(4, blatRecs.get(0).length);
		assertEquals("31	31	0	0	0	0	0	0	0	+	splitcon_chr7_100867120_chr7_100867215	398	52	83	chr7	159138663	100866806	100866837	1	31	52	100866806", blatRecs.get(0)[0].toString());
		assertEquals("56	56	0	0	0	0	0	0	0	+	splitcon_chr7_100867120_chr7_100867215	398	1	57	chr7	159138663	100866756	100866812	1	56	1	100866756", blatRecs.get(0)[1].toString());
		assertEquals("147	147	0	0	0	0	0	0	0	+	splitcon_chr7_100867120_chr7_100867215	398	250	397	chr7	159138663	100867214	100867361	1	147	250	100867214", blatRecs.get(0)[2].toString());
		assertEquals("177	177	0	0	0	0	0	0	0	+	splitcon_chr7_100867120_chr7_100867215	398	79	256	chr7	159138663	100866949	100867126	1	177	79	100866949", blatRecs.get(0)[3].toString());
	}
	
	@Test
	public void blatRecordLargerThanSequence() {
		/*
		 * IntLongPairs [pairs=[IntLongPair [i=6422528, l=155032316323820], IntLongPair [i=8454144, l=1176806490], null]]
		 */
		int seqLength = 251;
		IntLongPairs pairs = new IntLongPairs(new IntLongPair(6422528, 155032316323820l), new IntLongPair(8454144, 1176806490));
		Optional<BLATRecord> oBR = BLATRecordUtil.blatRecordFromSplits(pairs, "splitcon_chr6_114264670_chr6_114265444_1_true_1591590363423_284336_clip", seqLength, pcpm, TARecordUtil.TILE_LENGTH);
		assertEquals(true, oBR.isPresent());
		System.out.println("blat record: " + oBR.get().toString());
		assertEquals(250, oBR.get().getScore());
	}
	
	@Test
	public void overlapingReference() {
		/*
		 * IntLongPairs [pairs=[IntLongPair [i=7274496, l=309020158], IntLongPair [i=8781824, l=135240239236658]]]
		 */
		int seqLength = 269;
		IntLongPairs pairs = new IntLongPairs(new IntLongPair(7274496, 309020158), new IntLongPair(8781824, 135240239236658l));
		
		Map<ChrPosition, int[]> map = TARecordUtil.getChrPositionAndBlocksFromSplits(pairs, seqLength, pcpm);
		assertEquals(2, map.size());
		
		Optional<BLATRecord> oBR = BLATRecordUtil.blatRecordFromSplits(pairs, "blah", seqLength, pcpm, TARecordUtil.TILE_LENGTH);
		assertEquals(true, oBR.isPresent());
		System.out.println("blat record: " + oBR.get().toString());
		assertEquals(196, oBR.get().getScore());
	}
	
	@Test
	public void negativeLengthSplits() {
		/*
		 * IntLongPairs [pairs=[IntLongPair [i=1900544, l=4611967496085375606], IntLongPair [i=2097152, l=4612018073620253348], IntLongPair [i=3538946, l=4611749792783075755], IntLongPair [i=6881281, l=4611833355666786810]]]
		 */
		int seqLength = 358;
		IntLongPairs pairs = new IntLongPairs(new IntLongPair[]{new IntLongPair(1900544, 4611967496085375606l), 
				new IntLongPair(2097152, 4612018073620253348l), 
				new IntLongPair(3538946, 4611749792783075755l), 
				new IntLongPair(6881281, 4611833355666786810l)});
		
		Map<ChrPosition, int[]> map = TARecordUtil.getChrPositionAndBlocksFromSplits(pairs, seqLength, pcpm);
		assertEquals(4, map.size());
		
		Optional<BLATRecord> oBR =  BLATRecordUtil.blatRecordFromSplits(pairs, "blah", seqLength, pcpm, TARecordUtil.TILE_LENGTH);
		assertEquals(true, oBR.isPresent());
		assertEquals(261, oBR.get().getScore());
		
		/*
		[IntLongPair [i=2097152, l=4612018073620253348], IntLongPair [i=1900544, l=4611967496085375606], IntLongPair [i=4587520, l=4611872938085386782], IntLongPair [i=3538946, l=4611749792783075755]]]
		 * 
		 */
		pairs = new IntLongPairs(new IntLongPair[]{new IntLongPair(2097152, 4612018073620253348l), 
				new IntLongPair(1900544, 4611967496085375606l), 
				new IntLongPair(4587520, 4611872938085386782l), 
				new IntLongPair(3538946, 4611749792783075755l)});
		
		map = TARecordUtil.getChrPositionAndBlocksFromSplits(pairs, seqLength, pcpm);
		assertEquals(4, map.size());
		
		oBR = BLATRecordUtil.blatRecordFromSplits(pairs, "blah", seqLength, pcpm, TARecordUtil.TILE_LENGTH);
		assertEquals(true, oBR.isPresent());
		assertEquals(227, oBR.get().getScore());
		
		/*
		 * pairs=[IntLongPair [i=1310720, l=13196820893984], IntLongPair [i=4194306, l=255089379004927], IntLongPair [i=10878976, l=50580216238402]]]
		 */
		pairs = new IntLongPairs(new IntLongPair[]{new IntLongPair(1310720, 13196820893984l), 
				new IntLongPair(4194306, 255089379004927l), 
				new IntLongPair(10878976, 50580216238402l)});
		
		map = TARecordUtil.getChrPositionAndBlocksFromSplits(pairs, seqLength, pcpm);
		assertEquals(3, map.size());
		
		oBR = BLATRecordUtil.blatRecordFromSplits(pairs, "blah", seqLength, pcpm, TARecordUtil.TILE_LENGTH);
		assertEquals(true, oBR.isPresent());
		assertEquals(281, oBR.get().getScore());
	}
	
	@Test
	public void trimRangesWithChrPositionAgain() {
		int[][] ranges = new int[2][];
		ranges[0] = new int[]{0,100};
		ranges[1] = new int[]{100,200};
		/*
		 * setup a corresponding CP array
		 */
		ChrPosition[] cps = new ChrPosition[2];
		cps[0] = new ChrPositionName("chr1", 0, 100, "");
		cps[1] = new ChrPositionName("chr1", 50, 250, "");
		
		TARecordUtil.trimRangesToRemoveOverlap(ranges, cps);
		assertEquals(2, ranges.length);
		assertArrayEquals(new int[]{0,100}, ranges[0]);
		assertArrayEquals(new int[]{150,150}, ranges[1]);
		assertEquals(new ChrPositionName("chr1", 0, 100, ""), cps[0]);
		assertEquals(new ChrPositionName("chr1", 100, 250, ""), cps[1]);
	}
	
	@Test
	public void trimRangesWithChrPositionRC() {
		/*
		 * [[12, 44], [61, 41], [107, 117], [234, 66]]
		 */
		int[][] ranges = new int[2][];
		ranges[0] = new int[]{12,44};
		ranges[1] = new int[]{61,41};
		/*
		 * setup a corresponding CP array
		 * [ChrPositionName [chromosome=chr19, startPosition=21832769, endPosition=21832813, name=R], ChrPositionName [chromosome=chr19, startPosition=21832723, endPosition=21832764, name=R], ChrPositionName [chromosome=chr19, startPosition=21832599, endPosition=21832716, name=R], ChrPositionName [chromosome=chr19, startPosition=21832520, endPosition=21832586, name=R]]
		 */
		ChrPosition[] cps = new ChrPosition[2];
		cps[0] = new ChrPositionName("chr19", 21832769, 21832813, "R");
		cps[1] = new ChrPositionName("chr19", 21832723, 21832764, "R");
		
		TARecordUtil.trimRangesToRemoveOverlap(ranges, cps);
		assertEquals(2, ranges.length);
		assertArrayEquals(new int[]{12,44}, ranges[0]);
		assertArrayEquals(new int[]{61,41}, ranges[1]);
		assertEquals(new ChrPositionName("chr19", 21832769, 21832813, "R"), cps[0]);
		assertEquals(new ChrPositionName("chr19", 21832723, 21832764, "R"), cps[1]);
		
		/*
		 * and now lets engineer it so that the ChrPosition needs to take a trim
		 */
		ranges[0] = new int[]{12,54};	// added 10
		ranges[1] = new int[]{61,41};
		cps[0] = new ChrPositionName("chr19", 21832769, 21832823, "R");		// added 10
		cps[1] = new ChrPositionName("chr19", 21832723, 21832764, "R");
		TARecordUtil.trimRangesToRemoveOverlap(ranges, cps);
		assertEquals(2, ranges.length);
		assertArrayEquals(new int[]{12,49}, ranges[0]);
		assertArrayEquals(new int[]{61,41}, ranges[1]);
		assertEquals(new ChrPositionName("chr19", 21832769, 21832818, "R"), cps[0]);
		assertEquals(new ChrPositionName("chr19", 21832723, 21832764, "R"), cps[1]);
		
		ranges[0] = new int[]{12,44};
		ranges[1] = new int[]{61,41};
		cps[0] = new ChrPositionName("chr19", 21832769, 21832813, "R");
		cps[1] = new ChrPositionName("chr19", 21832743, 21832784, "R");		// moved 20 bp closer to neighbour 
		TARecordUtil.trimRangesToRemoveOverlap(ranges, cps);
		assertEquals(2, ranges.length);
		assertArrayEquals(new int[]{12,44 - 15}, ranges[0]);
		assertArrayEquals(new int[]{61,41}, ranges[1]);
		assertEquals(new ChrPositionName("chr19", 21832784, 21832813, "R"), cps[0]);
		assertEquals(new ChrPositionName("chr19", 21832743, 21832784, "R"), cps[1]);
		
		/*
		 * and again with the second CP requireing adjustment this time
		 */
		ranges[0] = new int[]{12,44};
		ranges[1] = new int[]{61,51};
		cps[0] = new ChrPositionName("chr19", 21832769, 21832813, "R");
		cps[1] = new ChrPositionName("chr19", 21832743, 21832794, "R");		// moved 20 bp closer to neighbour 
		TARecordUtil.trimRangesToRemoveOverlap(ranges, cps);
		assertEquals(2, ranges.length);
		assertArrayEquals(new int[]{12,44}, ranges[0]);
		assertArrayEquals(new int[]{61 + 25,51 - 25}, ranges[1]);
		assertEquals(new ChrPositionName("chr19", 21832769, 21832813, "R"), cps[0]);
		assertEquals(new ChrPositionName("chr19", 21832743, 21832769, "R"), cps[1]);
		
	}
	
	@Test
	public void trimRangesWithChrPosition() {
		int[][] ranges = new int[2][];
		ranges[0] = new int[]{0,100};
		ranges[1] = new int[]{100,200};
		/*
		 * setup a corresponding CP array
		 */
		ChrPosition[] cps = new ChrPosition[2];
		cps[0] = new ChrPositionName("chr1", 0, 100, "");
		cps[1] = new ChrPositionName("chr1", 100, 300, "");
				
		/*
		 * this method will modify the array in place
		 */
		TARecordUtil.trimRangesToRemoveOverlap(ranges, cps);
		assertEquals(2, ranges.length);
		assertArrayEquals(new int[]{0,100}, ranges[0]);
		assertArrayEquals(new int[]{100,200}, ranges[1]);
		assertEquals(new ChrPositionName("chr1", 0, 100, ""), cps[0]);
		assertEquals(new ChrPositionName("chr1", 100, 300, ""), cps[1]);
		
		ranges[0] = new int[]{0,110};
		ranges[1] = new int[]{100,200};
		cps[0] = new ChrPositionName("chr1", 0, 110, "");
		cps[1] = new ChrPositionName("chr1", 100, 300, "");
		TARecordUtil.trimRangesToRemoveOverlap(ranges, cps);
		assertEquals(2, ranges.length);
		assertArrayEquals(new int[]{0,110}, ranges[0]);
		assertArrayEquals(new int[]{110,190}, ranges[1]);
		assertEquals(new ChrPositionName("chr1", 0, 110, ""), cps[0]);
		assertEquals(new ChrPositionName("chr1", 110, 300, ""), cps[1]);
		
		ranges[0] = new int[]{0,110};
		ranges[1] = new int[]{200,100};
		cps[0] = new ChrPositionName("chr1", 0, 110, "");
		cps[1] = new ChrPositionName("chr1", 200, 300, "");
		TARecordUtil.trimRangesToRemoveOverlap(ranges, cps);
		assertEquals(2, ranges.length);
		assertArrayEquals(new int[]{0,110}, ranges[0]);
		assertArrayEquals(new int[]{200,100}, ranges[1]);
		assertEquals(new ChrPositionName("chr1", 0, 110, ""), cps[0]);
		assertEquals(new ChrPositionName("chr1", 200, 300, ""), cps[1]);
	}
	
	@Test
	public void trimRangesAgain() {
		int[][] ranges = new int[3][];
		ranges[0] = new int[]{0,100};
		ranges[1] = new int[]{100,100};
		ranges[2] = new int[]{200,100};
		
		/*
		 * setup a corresponding CP array
		 */
		ChrPosition[] cps = new ChrPosition[3];
		cps[0] = new ChrPositionName("chr1", 0, 100, "");
		cps[1] = new ChrPositionName("chr1", 100, 200, "");
		cps[2] = new ChrPositionName("chr1", 200, 300, "");
		
		/*
		 * this method will modify the array in place
		 */
		TARecordUtil.trimRangesToRemoveOverlap(ranges, cps);
		assertEquals(3, ranges.length);
		assertArrayEquals(new int[]{0,100}, ranges[0]);
		assertArrayEquals(new int[]{100,100}, ranges[1]);
		assertArrayEquals(new int[]{200,100}, ranges[2]);
		assertEquals(new ChrPositionName("chr1", 0, 100, ""), cps[0]);
		assertEquals(new ChrPositionName("chr1", 100, 200, ""), cps[1]);
		assertEquals(new ChrPositionName("chr1", 200, 300, ""), cps[2]);
		
		ranges[0] = new int[]{0,110};
		ranges[1] = new int[]{100,100};
		ranges[2] = new int[]{200,100};
		cps[0] = new ChrPositionName("chr1", 0, 110, "");
		cps[1] = new ChrPositionName("chr1", 100, 200, "");
		cps[2] = new ChrPositionName("chr1", 200, 300, "");
		TARecordUtil.trimRangesToRemoveOverlap(ranges, cps);
		assertEquals(3, ranges.length);
		assertArrayEquals(new int[]{0,100}, ranges[0]);
		assertArrayEquals(new int[]{100,100}, ranges[1]);
		assertArrayEquals(new int[]{200,100}, ranges[2]);
		assertEquals(new ChrPositionName("chr1", 0, 100, ""), cps[0]);
		assertEquals(new ChrPositionName("chr1", 100, 200, ""), cps[1]);
		assertEquals(new ChrPositionName("chr1", 200, 300, ""), cps[2]);
		
		ranges[0] = new int[]{0,110};
		ranges[1] = new int[]{100,100};
		ranges[2] = new int[]{150,200};
		cps[0] = new ChrPositionName("chr1", 0, 110, "");
		cps[1] = new ChrPositionName("chr1", 100, 200, "");
		cps[2] = new ChrPositionName("chr1", 150, 350, "");
		TARecordUtil.trimRangesToRemoveOverlap(ranges, cps);
		assertEquals(3, ranges.length);
		assertArrayEquals(new int[]{0,100}, ranges[0]);
		assertArrayEquals(new int[]{100,100}, ranges[1]);
		assertArrayEquals(new int[]{200,150}, ranges[2]);
		assertEquals(new ChrPositionName("chr1", 0, 100, ""), cps[0]);
		assertEquals(new ChrPositionName("chr1", 100, 200, ""), cps[1]);
		assertEquals(new ChrPositionName("chr1", 200, 350, ""), cps[2]);
	}
	
	@Test
	public void realLifeSplits2() {
		/*
		 * splitcon_chr8_125551528_chr8_125555328__true_1589928259240_726892
		 */
		String seq = "CGCGGCCGGGGAAGGTCAGCGCCGTAATGGCGTTCTTGGCGTCGGGACCCTACCTGACCCATCAGCAAAAGGTGTTGCGGCTTTATAAGCGGGCGCTACGCCACCTCGAGTCGTGGTGCGTCCAGAGAGACAAATACCGATACTTTGCTTGTTTGATGAGAGCCCGGTTTGAAGAACATAAGAATGAAAAGGATATGGCGAAGGCCACCCAGCTGCTGAAGGAGGCCGAGGAAGAATTCTGGTACCGTCAGCATCCAC";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr8_125551528_chr8_125555328__true_1589928259240_726892";
		
		countPosition.put(NumberUtils.getTileCount(121, 0), TARecordUtil.getLongList(137440471823016l));
		countPosition.put(NumberUtils.getTileCount(115, 0), TARecordUtil.getLongList(1518347092));
		countPosition.put(NumberUtils.getTileCount(23, 0), TARecordUtil.getLongList(4611703612118049933l));
		countPosition.put(NumberUtils.getTileCount(20, 0), TARecordUtil.getLongList(4611728900885488804l));
		countPosition.put(NumberUtils.getTileCount(17, 0), TARecordUtil.getLongList(4611754189652927675l));
		countPosition.put(NumberUtils.getTileCount(12, 0), TARecordUtil.getLongList(4611789374025016524l));
		countPosition.put(NumberUtils.getTileCount(7, 0), TARecordUtil.getLongList(4611896026652910893l));
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
				.filter(br -> br.getScore() > passingScore)
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		System.out.println("singleBR record from results: " + results.get(0).toString());
		assertEquals("257	258	0	0	0	0	0	1	3799	+	splitcon_chr8_125551528_chr8_125555328__true_1589928259240_726892	258	0	257	chr8	146364022	125551401	125555458	2	127,131	0,127	125551401,125555327", results.get(0).toString());
	}
	
	@Test
	public void realLifeSplits3() {
		/*
		 * splitcon_chr10_127633807_chr15_34031839
		 */
		String seq = "TTCGGCGTTGCTCACACTGGGAGCTGTAGACCGGAGCTGTTCCTATTCGGCCATCTTGGCTCCTCCCCCTATAGTGTTATTTCATTTTCCAAGGATACCTGCATTTCCACCAGAAAATATTTAAGGGGTTACACATTTCCCGTTTTGGTTAACCTGGATAAATGCGCGTATTTTATTTCTGTTTTCAG";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		
		countPosition.put(7012352, TARecordUtil.getLongList(75868643634102l));
		countPosition.put(3145728, TARecordUtil.getLongList(4611816862119100198l));	// 4611816862119100198l is the one we want!!!
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(1, splits.size());
		
		/*
		 * should not be able to get this in a single record, as the two components are on different strands and on different contigs
		 */
		Set<IntLongPairs> setOfPairs = splits.get(splits.keys()[0]);
		assertEquals(1, setOfPairs.size());
		IntLongPairs pairs = setOfPairs.iterator().next();
		Optional<BLATRecord> oBR = TARecordUtil.areSplitsCloseEnoughToBeSingleRecord(pairs, "splitcon_chr10_127633807_chr15_34031839", r.getSequence().length(), pcpm, 13);
		assertEquals(false, oBR.isPresent());
		
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, "splitcon_chr10_127633807_chr15_34031839", r.getSequence().length(), pcpm);
		assertEquals(1, blatRecs.size());
		assertEquals(2, blatRecs.get(0).length);
		assertEquals("60	60	0	0	0	0	0	0	0	-	splitcon_chr10_127633807_chr15_34031839	188	9	69	chr10	135534747	127633806	127633866	1	60	119	127633806", blatRecs.get(0)[0].toString());
		assertEquals("119	119	0	0	0	0	0	0	0	+	splitcon_chr10_127633807_chr15_34031839	188	69	188	chr15	102531392	34031838	34031957	1	119	69	34031838", blatRecs.get(0)[1].toString());
	}
	
	@Test
	public void realLifeSplits4() {
		/*
		 *  IntLongPair [i=8323072, l=32985657853490], IntLongPair [i=1179648, l=309020251], name: splitcon_chr2_59769659_chr2_59769589_2_true_1590115888837_35597_clip, seqLength: 169
		 */
		String seq = "TTCGGCGTTGCTCACACTGGGAGCTGTAGACCGGAGCTGTTCCTATTCGGCCATCTTGGCTCCTCCCCCTATAGTGTTATTTCATTTTCCAAGGATACCTGCATTTCCACCAGAAAATATTTAAGGGGTTACACATTTCCCGTTTTGGTTAACCTGGATAAATGCGCGT";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		
		countPosition.put(8323072, TARecordUtil.getLongList(32985657853490l));
		countPosition.put(1179648, TARecordUtil.getLongList(309020251));	
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		/*
		 * no splits - the shorter match is wholly encompassed (on the genomic side rather than the sequence side) by the longer match
		 */
		assertEquals(0, splits.size());
	}
	
	@Test
	public void realLifeSplits5() {
		/*
		 *  splitcon_chr6_114262928_chr6_114264515__true_1591925368416_401364
		 *  GTCAAATTCAGGGGTTGCTGAGCTGTTCTGATTTGGTTCCTTTGGTATCTGTTTTTTCACCACTGTTGTCCTTGGATTTATCTTCTTCCTTAACGTCTGTTTTTTTGTCCTCTGTTTCTTTCTTATCTTCTTCAATTCTAGCTTTCTTTGCTCCTTTCTTATGATCAGCCACATTTCTTCGACCTCCTTCTCCTTCATCCTCAGAATCTGAGAATTCTTCATCACAAGCTATCCGCTTGTCTGATGCTCGAATAGAAATTCTCTTGTCTGGATCTTCTCCATCTTCATCTCCACTGTCTTCATGAACAGCATCTTCTGG
		 */
		String seq = "GTCAAATTCAGGGGTTGCTGAGCTGTTCTGATTTGGTTCCTTTGGTATCTGTTTTTTCACCACTGTTGTCCTTGGATTTATCTTCTTCCTTAACGTCTGTTTTTTTGTCCTCTGTTTCTTTCTTATCTTCTTCAATTCTAGCTTTCTTTGCTCCTTTCTTATGATCAGCCACATTTCTTCGACCTCCTTCTCCTTCATCCTCAGAATCTGAGAATTCTTCATCACAAGCTATCCGCTTGTCTGATGCTCGAATAGAAATTCTCTTGTCTGGATCTTCTCCATCTTCATCTCCACTGTCTTCATGAACAGCATCTTCTGG";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr6_114262928_chr6_114264515__true_1591925368416_401364";
		
		countPosition.put(NumberUtils.getTileCount(144, 0), TARecordUtil.getLongList(105554293072971l));
		countPosition.put(NumberUtils.getTileCount(55, 0), TARecordUtil.getLongList(277078107006956l));	
		countPosition.put(NumberUtils.getTileCount(48, 0), TARecordUtil.getLongList(41782618660320l));	
		countPosition.put(NumberUtils.getTileCount(27, 0), TARecordUtil.getLongList(1176804175));	
		countPosition.put(NumberUtils.getTileCount(8, 0), TARecordUtil.getLongList(4611894926848722163l));	
		countPosition.put(NumberUtils.getTileCount(7, 0), TARecordUtil.getLongList(23090820065296l, 200112345847245l));	
		
		System.out.println("seq length: " + seq.length());
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(2, splits.size());
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		for (IntLongPairs ilps : potentialSplits) {
			System.out.println("ilps: " + ilps.toString());
		}
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
//				.filter(br -> br.getScore() > passingScore)
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		
		assertEquals(1, results.size());
		assertEquals(1, results.stream().filter(br -> br.getScore() >= passingScore).count());
		assertEquals("316	319	0	0	0	0	0	3	2977	+	splitcon_chr6_114262928_chr6_114264515__true_1591925368416_401364	319	0	318	chr6	171115067	114262214	114265510	4	39,59,154,67	0,39,98,252	114262214,114262872,114264516,114265443", results.get(0).toString());
		
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, "splitcon_chr6_114262928_chr6_114264515__true_1591925368416_401364", r.getSequence().length(), pcpm);
		assertEquals(1, blatRecs.size());
//		for (BLATRecord br : blatRecs.get(0)) {
//			System.out.println("blat record: " + br.toString());
//		}
		
		assertEquals(4, blatRecs.get(0).length);
		assertEquals("39	39	0	0	0	0	0	0	0	+	splitcon_chr6_114262928_chr6_114264515__true_1591925368416_401364	319	0	39	chr6	171115067	114262214	114262253	1	39	0	114262214", blatRecs.get(0)[0].toString());
		assertEquals("60	60	0	0	0	0	0	0	0	+	splitcon_chr6_114262928_chr6_114264515__true_1591925368416_401364	319	38	98	chr6	171115067	114262871	114262931	1	60	38	114262871", blatRecs.get(0)[1].toString());
		assertEquals("67	67	0	0	0	0	0	0	0	+	splitcon_chr6_114262928_chr6_114264515__true_1591925368416_401364	319	252	319	chr6	171115067	114265443	114265510	1	67	252	114265443", blatRecs.get(0)[2].toString());
		assertEquals("156	156	0	0	0	0	0	0	0	+	splitcon_chr6_114262928_chr6_114264515__true_1591925368416_401364	319	96	252	chr6	171115067	114264514	114264670	1	156	96	114264514", blatRecs.get(0)[3].toString());
	}
	
	@Test
	public void realLifeSplits6() {
		/*
		 *  splitcon_chr22_41657586_chr22_41660668_1_true_1590714106685_44497
		 *  TCCTCCAGGGTCCCGATGACCCTAAAAGCTTCTGCCAAGGCAGTGGCGCCATCATTCTCCAGACGGTTTCTGCCAGCCACAAAGACCTTCAGGGCCAGAGGCTTGCCTTGGGCACTGGATTTCCGGTGACATTCGGTCAGAGCTGCAGCCAGGATCTTGCCGCCGCCAATGCCCATGCCACAGTTGTTGAGCTTGAGTTCCTGCAGGGTGAAGCAGGCTGAGCTCTTGAGCAGGGCCTCGAAGCCTTGCACACCGTCGGGCCCGAATGCGTTGTCGCTTAAGTCCAGCTCCA
		 *  
		 */
		String seq = "TCCTCCAGGGTCCCGATGACCCTAAAAGCTTCTGCCAAGGCAGTGGCGCCATCATTCTCCAGACGGTTTCTGCCAGCCACAAAGACCTTCAGGGCCAGAGGCTTGCCTTGGGCACTGGATTTCCGGTGACATTCGGTCAGAGCTGCAGCCAGGATCTTGCCGCCGCCAATGCCCATGCCACAGTTGTTGAGCTTGAGTTCCTGCAGGGTGAAGCAGGCTGAGCTCTTGAGCAGGGCCTCGAAGCCTTGCACACCGTCGGGCCCGAATGCGTTGTCGCTTAAGTCCAGCTCCA";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		
		
		countPosition.put(NumberUtils.getTileCount(127, 0), TARecordUtil.getLongList(19794080686136l));
		countPosition.put(NumberUtils.getTileCount(125, 0), TARecordUtil.getLongList(170427173694668l));	
		countPosition.put(NumberUtils.getTileCount(9, 0), TARecordUtil.getLongList(2871382811l));	
		countPosition.put(NumberUtils.getTileCount(6, 0), TARecordUtil.getLongList(4611730001741593112l));	
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(1, splits.size());
		
		Set<IntLongPairs> setOfPairs = splits.get(splits.keys()[0]);
		assertEquals(1, setOfPairs.size());
		IntLongPairs pairs = setOfPairs.iterator().next();
		/*
		 * ascertain whether a single record may be derived from the splits
		 */
		Optional<BLATRecord> oBR = TARecordUtil.areSplitsCloseEnoughToBeSingleRecord(pairs, "splitcon_chr8_125551528_chr8_125555328__true_1589928259240_726892", r.getSequence().length(), pcpm, 13);
		assertEquals(true, oBR.isPresent());
		BLATRecord singleBR = oBR.get();
		System.out.println("singleBR record: " + singleBR.toString());
		/*
		 * 290	292	0	0	0	0	0	2	6422	+	splitcon_chr8_125551528_chr8_125555328__true_1589928259240_726892	292	0	291	chr22	12345	41654090	41660804	3	21,136,135	0,21,157	41654090,41657450,41660669
		 */
		assertEquals(true, singleBR.getScore() <= seq.length());
		assertEquals("290	292	0	0	0	0	0	2	6422	+	splitcon_chr8_125551528_chr8_125555328__true_1589928259240_726892	292	0	291	chr22	51304566	41654090	41660804	3	21,136,135	0,21,157	41654090,41657450,41660669", singleBR.toString());
		
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, "splitcon_chr22_41657586_chr22_41660668_1_true_1590714106685_44497", r.getSequence().length(), pcpm);
		assertEquals(1, blatRecs.size());
		for (BLATRecord br : blatRecs.get(0)) {
			System.out.println("blat record: " + br.toString());
		}
		assertEquals(3, blatRecs.get(0).length);
		assertEquals("21	21	0	0	0	0	0	0	0	+	splitcon_chr22_41657586_chr22_41660668_1_true_1590714106685_44497	292	0	21	chr22	51304566	41654090	41654111	1	21	0	41654090", blatRecs.get(0)[0].toString());
		assertEquals("137	137	0	0	0	0	0	0	0	+	splitcon_chr22_41657586_chr22_41660668_1_true_1590714106685_44497	292	155	292	chr22	51304566	41660667	41660804	1	137	155	41660667", blatRecs.get(0)[1].toString());
		assertEquals("139	139	0	0	0	0	0	0	0	+	splitcon_chr22_41657586_chr22_41660668_1_true_1590714106685_44497	292	18	157	chr22	51304566	41657447	41657586	1	139	18	41657447", blatRecs.get(0)[2].toString());
	}
	
	@Test
	public void realLifeSplits7() {
		/*
		 *  chr19_47884141_47884486
		 *  CCCAGCCACCCCCCATCTTCCTGGCCTCTTTGGCAGCTCCACCCTGTCCCCCCACCCCACAAAGGGGGGCTACGCAGTCACTGACTTCCTCACCTACAACTGCCTCACGAATGACACAGACCTGTACAGCGACTGTCTCCGAACCTTCTGGACN
		 *  
		 */
		String seq = "CCCAGCCACCCCCCATCTTCCTGGCCTCTTTGGCAGCTCCACCCTGTCCCCCCACCCCACAAAGGGGGGCTACGCAGTCACTGACTTCCTCACCTACAACTGCCTCACGAATGACACAGACCTGTACAGCGACTGTCTCCGAACCTTCTGGACN";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "chr19_47884141_47884486";
		
		
		countPosition.put(NumberUtils.getTileCount(93, 4), TARecordUtil.getLongList(2707328463l));
		countPosition.put(NumberUtils.getTileCount(34, 4), TARecordUtil.getLongList(118749963128572l));	
		countPosition.put(NumberUtils.getTileCount(8, 4), TARecordUtil.getLongList(41782676974449l, 4611791573772632679l, 4611793773061013150l));	
		countPosition.put(NumberUtils.getTileCount(6, 0), TARecordUtil.getLongList(4611730001741593112l));	
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(2, splits.size());
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
				.filter(br -> br.getScore() > passingScore)
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		System.out.println("singleBR record from results: " + results.get(0).toString());
		assertEquals("141	151	8	0	0	1	3	1	196	+	chr19_47884141_47884486	154	0	153	chr19	59128983	47884140	47884487	2	105,46	0,108	47884140,47884441", results.get(0).toString());
	}
	
	@Test
	public void realLifeSplits8() {
		/*
		 *  splitcon_chr5_96617965_chr5_96618035__true_1592967296845_370082_clip
		 *  CACAGTGTATAAGACTTCCTTATACATTTCCACCAACAGTGTATAAGACTTATACACAACAGTGTATAAGACTTATACACAACAGTGTATAAGACAGTGTATAAGATGTTGGCAAGGTCCTCTACAACCTTGCCAACATCTGTTATTTTCTCTCTTTTTAATAATAGCCAT,
		 *  
		 */
		String seq = "CACAGTGTATAAGACTTCCTTATACATTTCCACCAACAGTGTATAAGACTTATACACAACAGTGTATAAGACTTATACACAACAGTGTATAAGACAGTGTATAAGATGTTGGCAAGGTCCTCTACAACCTTGCCAACATCTGTTATTTTCTCTCTTTTTAATAATAGCCAT";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr5_96617965_chr5_96618035__true_1592967296845_370082_clip";
		
		countPosition.put(NumberUtils.getTileCount(126, 0), TARecordUtil.getLongList(36284861961343l));
		countPosition.put(NumberUtils.getTileCount(82, 0), TARecordUtil.getLongList(1100489872410l));	
		countPosition.put(NumberUtils.getTileCount(50, 0), TARecordUtil.getLongList(36284861961297l, 36284861961320l));	
		countPosition.put(NumberUtils.getTileCount(17, 1), TARecordUtil.getLongList(149534583776394l));	
		countPosition.put(NumberUtils.getTileCount(17, 0), TARecordUtil.getLongList(24190234055693l));
		countPosition.put(NumberUtils.getTileCount(16, 0), TARecordUtil.getLongList(135240888562480l));
		
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(3, splits.size());
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		
		for (IntLongPairs pairs : potentialSplits) {
			System.out.println("pot split pairs: " + pairs.toDetailedString());
		}
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		/*
		 * only 1 single record splits here
		 */
		assertEquals(1, results.size());
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		assertEquals(1, results.stream().filter(br -> br.getScore() >= passingScore).count());
		assertEquals("169	170	0	0	0	0	0	1	69	+	splitcon_chr5_96617965_chr5_96618035__true_1592967296845_370082_clip	171	1	170	chr5	180915260	96617933	96618172	2	94,76	1,95	96617933,96618096", results.get(0).toString());
		
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, name, r.getSequence().length(), pcpm);
		assertEquals(1, blatRecs.size());
		blatRecs.sort(null);
//		for (BLATRecord br : blatRecs.get(0)) {
//			System.out.println("blat record: " + br.toString());
//		}
		assertEquals(2, blatRecs.get(0).length);
		assertEquals("94	94	0	0	0	0	0	0	0	+	splitcon_chr5_96617965_chr5_96618035__true_1592967296845_370082_clip	171	1	95	chr5	180915260	96617933	96618027	1	94	1	96617933", blatRecs.get(0)[0].toString());
		assertEquals("138	138	0	0	0	0	0	0	0	+	splitcon_chr5_96617965_chr5_96618035__true_1592967296845_370082_clip	171	33	171	chr5	180915260	96618034	96618172	1	138	33	96618034", blatRecs.get(0)[1].toString());
	}
	
	@Test
	public void realLifeSplits9() {
		/*
		 * This sequence spans over 400kbp as reported by BLAT. BEats the 10kbp limit that was set on finding splits...
		 * 
		 *  splitcon_chr4_5182939_chr4_5675244__true_1594701165894_840304
		 *  AGCAGCATGAGTGTTTCAACACTTTGAGGTCTGACCTCCCAGGTTCTCATCCTAGCTCTGCCTCTAGCTGGCTATGATGTTTCAACATATGAATTTGTGTCCCCCTGCCTCAAATTCATATGTTGAAACCCTAGCCCCCAGTGTCAAAACCTCATTGTCTAAGAATAAAGAAAAAGAGATGGTTATCCCGGCTTCCCACCCCCCAGAGTCCTCCTCACTTTCCTTCTTGTCTCTGGAAAAGAATCATCCACGATTACCACCCTTTAATCACATATTAGCAGGAGCAGATGCCTTT,
		 *  
		 */
		String seq = "AGCAGCATGAGTGTTTCAACACTTTGAGGTCTGACCTCCCAGGTTCTCATCCTAGCTCTGCCTCTAGCTGGCTATGATGTTTCAACATATGAATTTGTGTCCCCCTGCCTCAAATTCATATGTTGAAACCCTAGCCCCCAGTGTCAAAACCTCATTGTCTAAGAATAAAGAAAAAGAGATGGTTATCCCGGCTTCCCACCCCCCAGAGTCCTCCTCACTTTCCTTCTTGTCTCTGGAAAAGAATCATCCACGATTACCACCCTTTAATCACATATTAGCAGGAGCAGATGCCTTT";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr4_5182939_chr4_5675244__true_1594701165894_840304";
		
		
		countPosition.put(NumberUtils.getTileCount(85, 1), TARecordUtil.getLongList(217703998447365l));
		countPosition.put(NumberUtils.getTileCount(67, 0), TARecordUtil.getLongList(695655218));
		countPosition.put(NumberUtils.getTileCount(39, 1), TARecordUtil.getLongList(160529393802964l));	
		countPosition.put(NumberUtils.getTileCount(34, 1), TARecordUtil.getLongList(103354788666256l));	
		countPosition.put(NumberUtils.getTileCount(12, 1), TARecordUtil.getLongList(120946983753377l));
		countPosition.put(NumberUtils.getTileCount(11, 1), TARecordUtil.getLongList(120946996001436l, 120947046443537l));
		countPosition.put(NumberUtils.getTileCount(10, 1), TARecordUtil.getLongList(117648551146731l, 122046505277795l, 122046603764683l));
		countPosition.put(NumberUtils.getTileCount(10, 0), TARecordUtil.getLongList(4611864140062071056l));
		countPosition.put(NumberUtils.getTileCount(9, 1), TARecordUtil.getLongList(83563602760479l, 117648490513400l, 120947011893536l, 123145998326992l));
		
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(1, splits.size());
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		assertEquals(1, results.size());
		assertEquals(0, results.stream().filter(br -> br.getScore() >= passingScore).count());
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		
		System.out.println("results BR: " + results.get(0).toString());
		assertEquals("264	271	3	0	0	1	24	3	492325	+	splitcon_chr4_5182939_chr4_5675244__true_1594701165894_840304	295	0	294	chr4	191154276	5182793	5675389	4	79,46,51,95	0,94,146,200	5182793,5182887,5675243,5675294", results.get(0).toString());
		
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, name, r.getSequence().length(), pcpm);
		assertEquals(1, blatRecs.size());
		blatRecs.sort(null);
		for (BLATRecord br : blatRecs.get(0)) {
			System.out.println("blat record: " + br.toString());
		}
		assertEquals(4, blatRecs.get(0).length);
		assertEquals("45	46	1	0	0	0	0	0	0	+	splitcon_chr4_5182939_chr4_5675244__true_1594701165894_840304	295	94	140	chr4	191154276	5182887	5182933	1	46	94	5182887", blatRecs.get(0)[0].toString());
		assertEquals("50	51	1	0	0	0	0	0	0	+	splitcon_chr4_5182939_chr4_5675244__true_1594701165894_840304	295	146	197	chr4	191154276	5675243	5675294	1	51	146	5675243", blatRecs.get(0)[1].toString());
		assertEquals("79	79	0	0	0	0	0	0	0	+	splitcon_chr4_5182939_chr4_5675244__true_1594701165894_840304	295	0	79	chr4	191154276	5182793	5182872	1	79	0	5182793", blatRecs.get(0)[2].toString());
		assertEquals("96	97	1	0	0	0	0	0	0	+	splitcon_chr4_5182939_chr4_5675244__true_1594701165894_840304	295	198	295	chr4	191154276	5675292	5675389	1	97	198	5675292", blatRecs.get(0)[3].toString());
	}
	
	@Test
	public void realLifeSplits10() {
		/*
		 * This sequence spans a large region, but the order of the splits are not contiguous.
		 * 
		 *  splitcon_chr11_68535215_chr11_68688420__true_1594705425399_247953
		 *  GTCTTGCTCTGTTGCTCAGACTGGAGTGCAGAGGCGTGATCATAACTTGCTGTAGCCTTGCATTCTTTGGCTCAAGTAATCCTCCCACCTCAGCTTCCCGAGTAGCTAGTACTGTAGGCATGAGCCATTACACCTGGCTAATTTTTTTCTTTTGTTCCCAAGCACAGAGTGACGGATAAAAGGCGAAATGTCTCCACTCTGGGTTCACCGGGTTTTGTGTAACAAATATGTAACTGACGATTCCCCTACCGCCCCTTTTCTCTTGCATCATGGGAATTCC
		 *  
		 */
		String seq = "GTCTTGCTCTGTTGCTCAGACTGGAGTGCAGAGGCGTGATCATAACTTGCTGTAGCCTTGCATTCTTTGGCTCAAGTAATCCTCCCACCTCAGCTTCCCGAGTAGCTAGTACTGTAGGCATGAGCCATTACACCTGGCTAATTTTTTTCTTTTGTTCCCAAGCACAGAGTGACGGATAAAAGGCGAAATGTCTCCACTCTGGGTTCACCGGGTTTTGTGTAACAAATATGTAACTGACGATTCCCCTACCGCCCCTTTTCTCTTGCATCATGGGAATTCC";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr11_68535215_chr11_68688420__true_1594705425399_247953";
		
		countPosition.put(NumberUtils.getTileCount(133, 0), TARecordUtil.getLongList(3300419479497l));
		countPosition.put(NumberUtils.getTileCount(125, 0), TARecordUtil.getLongList(157232047215073l));	
		countPosition.put(NumberUtils.getTileCount(64, 2), TARecordUtil.getLongList(4611686020313597686l));	
		countPosition.put(NumberUtils.getTileCount(40, 1), TARecordUtil.getLongList(4611772881732192067l));	
		countPosition.put(NumberUtils.getTileCount(34, 1), TARecordUtil.getLongList(4611720105174058773l));
		countPosition.put(NumberUtils.getTileCount(27, 0), TARecordUtil.getLongList(74768700815525l));
		
		System.out.println("seqLength: " + seq.length());
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(3, splits.size());
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		assertEquals(1, results.size());
		assertEquals(0, results.stream().filter(br -> br.getScore() >= passingScore).count());
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		assertEquals("123	128	3	0	0	1	3	1	1	-	splitcon_chr11_68535215_chr11_68688420__true_1594705425399_247953	280	204	200	chr11	135006516	70301891	70302020	2	76,52	0,79	70301891,70301968", results.get(0).toString());
		
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, name, r.getSequence().length(), pcpm);
		assertEquals(1, blatRecs.size());
		blatRecs.sort(null);
		for (BLATRecord br : blatRecs.get(0)) {
			System.out.println("blat record: " + br.toString());
		}
		assertEquals(2, blatRecs.get(0).length);
		assertEquals("137	137	0	0	0	0	0	0	0	+	splitcon_chr11_68535215_chr11_68688420__true_1594705425399_247953	280	143	280	chr11	135006516	68535214	68535351	1	137	143	68535214", blatRecs.get(0)[0].toString());
		assertEquals("145	145	0	0	0	0	0	0	0	+	splitcon_chr11_68535215_chr11_68688420__true_1594705425399_247953	280	3	148	chr11	135006516	68688278	68688423	1	145	3	68688278", blatRecs.get(0)[1].toString());
	}
	
	@Test
	public void realLifeSplits12() {
		/*
		 *  GL000232.1_3413_false
		 *  CACTTTTGTTGAAAATTAATTTAGCTGTCCCTCCCATAGGATTCCAGAACGC
		 *  
		 */
		String seq = "CACTTTTGTTGAAAATTAATTTAGCTGTCCCTCCCATAGGATTCCAGAACGC";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "GL000232.1_3413_false";
		
		countPosition.put(NumberUtils.getTileCount(16, 0), TARecordUtil.getLongList(4611712409752715004l, 4611712409807521324l));
		countPosition.put(NumberUtils.getTileCount(14, 0), TARecordUtil.getLongList(4611714608830778186l));	
		countPosition.put(NumberUtils.getTileCount(13, 0), TARecordUtil.getLongList( 27489398850136l,27489398851711l,27490009933828l, 27490636997025l, 27490888461371l,27490888479382l, 27490891131388l, 4611688219669829321l, 4611688220296896290l, 4611688220496904249l, 4611688220550258880l, 4611688220550996199l));	
		countPosition.put(NumberUtils.getTileCount(12, 0), TARecordUtil.getLongList(28587684403510l, 4611688219554484768l, 4611688220246743779l, 4611716807799226451l));	
		
		System.out.println("seqLength: " + seq.length());
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		System.out.println("single record split: " + results.get(0).toString());
		assertEquals(1, results.stream().filter(br -> br.getScore() >= passingScore).count());
		assertEquals("47	49	0	0	0	1	1	1	1	-	GL000232.1_3413_false	52	25	23	chrY	59373566	9956946	9956996	2	25,24	2,28	9956946,9956972", results.get(0).toString());
	}
	
	@Test
	public void realLifeSplits13() {
		/*
		 *  splitcon_chr15_79475029_chr15_79475126__true_1595570520496_795513
		 *  TCCCTCCCTTCCTCTTTCTGTCTTTTCTTCCTTCCTTCATTTCTTTCTCTTTCTCTTTCCTTCTTTCTTTCTTTTCCTCCCTCCCTCCCCTTCCTTCCTTTCTCTTTCTTTCTTCCCTTTCTTTCTCTCTTTCTTTTGTCCCTTTCTTTCTCACTTTCTAGTCTTTCATTCTTTATTTTTCTCACTCCCTACCTTCCTCCCTCAACCTTAATTTACATCCATCCATCCATTCACTCTCTATCTTTCTTTATTTTCATAATTTCATTCCTTATTTTTCAAAAAAAATGT
		 */
		String seq = "TCCCTCCCTTCCTCTTTCTGTCTTTTCTTCCTTCCTTCATTTCTTTCTCTTTCTCTTTCCTTCTTTCTTTCTTTTCCTCCCTCCCTCCCCTTCCTTCCTTTCTCTTTCTTTCTTCCCTTTCTTTCTCTCTTTCTTTTGTCCCTTTCTTTCTCACTTTCTAGTCTTTCATTCTTTATTTTTCTCACTCCCTACCTTCCTCCCTCAACCTTAATTTACATCCATCCATCCATTCACTCTCTATCTTTCTTTATTTTCATAATTTCATTCCTTATTTTTCAAAAAAAATGT";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr15_79475029_chr15_79475126__true_1595570520496_795513";
		
		countPosition.put(NumberUtils.getTileCount(61, 7), TARecordUtil.getLongList(70371130938500l));
		countPosition.put(NumberUtils.getTileCount(26, 0), TARecordUtil.getLongList(2386760682l));	
		countPosition.put(NumberUtils.getTileCount(13, 11), TARecordUtil.getLongList( 4611892729000614663l));	
		countPosition.put(NumberUtils.getTileCount(13, 3), TARecordUtil.getLongList( 58276479368216l));	
		countPosition.put(NumberUtils.getTileCount(12, 15), TARecordUtil.getLongList(112152562656770l));	
		countPosition.put(NumberUtils.getTileCount(12, 8), TARecordUtil.getLongList(4611879534844943768l));	
		countPosition.put(NumberUtils.getTileCount(12, 7), TARecordUtil.getLongList(78067697618203l));	
		countPosition.put(NumberUtils.getTileCount(12, 3), TARecordUtil.getLongList(51679401124067l));
		
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		assertEquals(0, results.size());
	}
	
	@Test
	public void realLifeSplits13Again() {
		/*
		 *  splitcon_chr15_79475029_chr15_79475126__true_1595570520496_795513
		 *  TCCCTCCCTTCCTCTTTCTGTCTTTTCTTCCTTCCTTCATTTCTTTCTCTTTCTCTTTCCTTCTTTCTTTCTTTTCCTCCCTCCCTCCCCTTCCTTCCTTTCTCTTTCTTTCTTCCCTTTCTTTCTCTCTTTCTTTTGTCCCTTTCTTTCTCACTTTCTAGTCTTTCATTCTTTATTTTTCTCACTCCCTACCTTCCTCCCTCAACCTTAATTTACATCCATCCATCCATTCACTCTCTATCTTTCTTTATTTTCATAATTTCATTCCTTATTTTTCAAAAAAAATGT
		 */
		String seq = "TCCCTCCCTTCCTCTTTCTGTCTTTTCTTCCTTCCTTCATTTCTTTCTCTTTCTCTTTCCTTCTTTCTTTCTTTTCCTCCCTCCCTCCCCTTCCTTCCTTTCTCTTTCTTTCTTCCCTTTCTTTCTCTCTTTCTTTTGTCCCTTTCTTTCTCACTTTCTAGTCTTTCATTCTTTATTTTTCTCACTCCCTACCTTCCTCCCTCAACCTTAATTTACATCCATCCATCCATTCACTCTCTATCTTTCTTTATTTTCATAATTTCATTCCTTATTTTTCAAAAAAAATGT";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr15_79475029_chr15_79475126__true_1595570520496_795513";
		System.out.println("seq length: " + seq.length());
		
		countPosition.put(NumberUtils.getTileCount(61, 0), TARecordUtil.getLongList(70371130938500l));
		countPosition.put(NumberUtils.getTileCount(42, 1), TARecordUtil.getLongList(2386760682l));	
		countPosition.put(NumberUtils.getTileCount(21, 1), TARecordUtil.getLongList( 106655026780407l));	
		countPosition.put(NumberUtils.getTileCount(19, 1), TARecordUtil.getLongList( 25291154199591l, 25291154199629l));	
		countPosition.put(NumberUtils.getTileCount(13, 0), TARecordUtil.getLongList( 58276479368216l, 4611892729000614663l));	
		
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		assertEquals(1, results.size());
		System.out.println(results.get(0).toString());
		assertEquals(0, results.stream().filter(br -> br.getScore() >= passingScore).count());
	}
	
	@Test
	public void realLifeSplits14() {
		/*
		 *  splitcon_chrX_58390230_chrX_58390403__true_1595572151098_385828
		 *  GGATTCAGCCGTTTGGAAACACTGTATTGTGTAGAAACTGTAAAGGGATATTTTGGAGAGCATTGTGGCCTGTGGTGAAAAAAGATACATCTTCAGATAAAATAGAAAGAAGCATTCTGAGAAGGTGCTTTGTGATGTGTCCATTCATCTCACAGAGTTAAACCTTTCTTTGGATTATGCAGGTTGGAAAATCTTTATCTGAAGAATCTGCTAGTGGATATTTATGAGCCCACTGAGTCCTAGGGGGAAAAACAGGATACCCATCAAAGAAAACTACAAAGAAACTAACTGTGAAACTG
		 */
		String seq = "GGATTCAGCCGTTTGGAAACACTGTATTGTGTAGAAACTGTAAAGGGATATTTTGGAGAGCATTGTGGCCTGTGGTGAAAAAAGATACATCTTCAGATAAAATAGAAAGAAGCATTCTGAGAAGGTGCTTTGTGATGTGTCCATTCATCTCACAGAGTTAAACCTTTCTTTGGATTATGCAGGTTGGAAAATCTTTATCTGAAGAATCTGCTAGTGGATATTTATGAGCCCACTGAGTCCTAGGGGGAAAAACAGGATACCCATCAAAGAAAACTACAAAGAAACTAACTGTGAAACTG";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chrX_58390230_chrX_58390403__true_1595572151098_385828";
		
		countPosition.put(NumberUtils.getTileCount(23, 1), TARecordUtil.getLongList(155034078926976l,155034078928694l, 155034078940292l, 155034078981930l));
		countPosition.put(NumberUtils.getTileCount(23, 0), TARecordUtil.getLongList(4611821261297089557l, 4611821261297096271l, 4611821261297102676l));	
		countPosition.put(NumberUtils.getTileCount(21, 0), TARecordUtil.getLongList( 4611821261297035770l, 4611823460320255604l, 4611823460320354734l));	
		countPosition.put(NumberUtils.getTileCount(19, 1), TARecordUtil.getLongList( 140740431298817l, 155034078941825l, 155034082441102l));	
		countPosition.put(NumberUtils.getTileCount(19, 0), TARecordUtil.getLongList(4611825659343617192l));	
		
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		assertEquals(0, results.size());
		assertEquals(0, results.stream().filter(br -> br.getScore() >= passingScore).count());
	}
	
	@Test
	public void realLifeSplits15() {
		/*
		 *  splitcon_chr5_96617965_chr5_96618035__true_1595856043374_646700
		 *  CACAGTGTATAAGACTTCCTTATACATTTCCACCAACAGTGTATAAGACTTATACACAACAGTGTATAAGACTTATACACAACAGTGTATAAGACAGTGTATAAGATGTTGGCAAGGAACTCTACAACCTTGCCAACAGCTGTTATTTTCTCTCTTTTTAATAATAGCAATCTTTAATGTGTTAAGATTATAACTTTTATTTTTAATTTGAATTTATATGATTATTATATAACTTGAGAATTTTTTCTTATACAAGTTAGCCA
		 */
		String seq = "CACAGTGTATAAGACTTCCTTATACATTTCCACCAACAGTGTATAAGACTTATACACAACAGTGTATAAGACTTATACACAACAGTGTATAAGACAGTGTATAAGATGTTGGCAAGGAACTCTACAACCTTGCCAACAGCTGTTATTTTCTCTCTTTTTAATAATAGCAATCTTTAATGTGTTAAGATTATAACTTTTATTTTTAATTTGAATTTATATGATTATTATATAACTTGAGAATTTTTTCTTATACAAGTTAGCCA";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr5_96617965_chr5_96618035__true_1595856043374_646700";
		
		countPosition.put(NumberUtils.getTileCount(82, 0), TARecordUtil.getLongList(1100489872410l));
		countPosition.put(NumberUtils.getTileCount(72, 2), TARecordUtil.getLongList(36284861961343l));	
		countPosition.put(NumberUtils.getTileCount(46, 2), TARecordUtil.getLongList( 40682908472428l));	
		countPosition.put(NumberUtils.getTileCount(17, 2), TARecordUtil.getLongList( 152833094505705l));	
		countPosition.put(NumberUtils.getTileCount(17, 0), TARecordUtil.getLongList(24190234055693l));	
		countPosition.put(NumberUtils.getTileCount(14, 0), TARecordUtil.getLongList(24190297952852l));	
		countPosition.put(NumberUtils.getTileCount(13, 2), TARecordUtil.getLongList(4611923513906431087l));	
		countPosition.put(NumberUtils.getTileCount(13, 0), TARecordUtil.getLongList(24190288217943l));	
		countPosition.put(NumberUtils.getTileCount(11, 2), TARecordUtil.getLongList(4611923513880686311l));	
		countPosition.put(NumberUtils.getTileCount(10, 2), TARecordUtil.getLongList(139638926789045l, 4611926812479300845l));	
		
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		for (BLATRecord br : results) {
			System.out.println("br: " + br.toString());
		}
		assertEquals(2, results.size());
		assertEquals(0, results.stream().filter(br -> br.getScore() >= passingScore).count());
	}
	
	@Test
	public void realLifeSplits16() {
		/*
		 *  splitcon_chr19_4210529_chr19_4210587__true_1595864127799_629191_clip,
		 *  GGCCTCCTCGCTGTTCCTCCAACTCGCCAGGCGCAGTCCTGCCTCAGGGCCTTTGCACCACCAGTGTCCTCTGCCTGGAATGCTCTAGCCCCAGATGGCCCCACAGCTCTTTACCTCACCTACTTTATGCAGTAGGCTCCTCGCCAAAGACACTTCCCATGCCCACTTCCC
		 */
		String seq = "GGCCTCCTCGCTGTTCCTCCAACTCGCCAGGCGCAGTCCTGCCTCAGGGCCTTTGCACCACCAGTGTCCTCTGCCTGGAATGCTCTAGCCCCAGATGGCCCCACAGCTCTTTACCTCACCTACTTTATGCAGTAGGCTCCTCGCCAAAGACACTTCCCATGCCCACTTCCC";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr19_4210529_chr19_4210587__true_1595864127799_629191_clip";
		
		countPosition.put(NumberUtils.getTileCount(96, 0), TARecordUtil.getLongList(69271896204830l));
		countPosition.put(NumberUtils.getTileCount(77, 0), TARecordUtil.getLongList(2663654822l));	
		countPosition.put(NumberUtils.getTileCount(20, 0), TARecordUtil.getLongList(32988012488189l));	
		countPosition.put(NumberUtils.getTileCount(14, 0), TARecordUtil.getLongList(35187036832507l, 35187037098376l));	
		
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		assertEquals(1, results.size());
		System.out.println(results.get(0).toString());
		assertEquals(1, results.stream().filter(br -> br.getScore() >= passingScore).count());
	}
	
	@Test
	public void realLifeSplits17() {
		/*
		 *  splitcon_chr21_11122856_chr21_11124370__false_1596182570085_99163,
		 *  CCACATGAGGATTAGGATGTGGACATCTTTGGGGCCATTATTCTGTCTACCACATGGGGATTAGGACATGGATATCTTTGGGGCCATTATTCTGTCTACCACATGGGGATTAGGACATGGACATCTCTGGGGACATTATTCTGTCTACCACATGGGGAATAGGATATTGGTAACATCTTTAGGGCCATTATTCTGTCTACCACATGGGGATTAGGACATGGATATCTTTGGGGCCATTATTCTGTCTACCACATGGGGATTAGGACATGGACATCTTTGGGGACATTATTCTGTCTACCACAAGGGGTTTAGTAGGTGAAGGTCTTCGGGGCATT
		 */
		String seq = "CCACATGAGGATTAGGATGTGGACATCTTTGGGGCCATTATTCTGTCTACCACATGGGGATTAGGACATGGATATCTTTGGGGCCATTATTCTGTCTACCACATGGGGATTAGGACATGGACATCTCTGGGGACATTATTCTGTCTACCACATGGGGAATAGGATATTGGTAACATCTTTAGGGCCATTATTCTGTCTACCACATGGGGATTAGGACATGGATATCTTTGGGGCCATTATTCTGTCTACCACATGGGGATTAGGACATGGACATCTTTGGGGACATTATTCTGTCTACCACAAGGGGTTTAGTAGGTGAAGGTCTTCGGGGCATT";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr21_11122856_chr21_11124370__false_1596182570085_99163";
		
		countPosition.put(NumberUtils.getTileCount(154, 0), TARecordUtil.getLongList(83565676432469l));
		countPosition.put(NumberUtils.getTileCount(52, 1), TARecordUtil.getLongList(8798885743682l));	
		countPosition.put(NumberUtils.getTileCount(50, 2), TARecordUtil.getLongList(257288513620400l));	
		countPosition.put(NumberUtils.getTileCount(49, 0), TARecordUtil.getLongList(34087653182654l));	
		countPosition.put(NumberUtils.getTileCount(46, 2), TARecordUtil.getLongList(8798885743095l));	
		countPosition.put(NumberUtils.getTileCount(46, 1), TARecordUtil.getLongList(8798885743144l));	
		countPosition.put(NumberUtils.getTileCount(39, 1), TARecordUtil.getLongList(17594978765213l));	
		countPosition.put(NumberUtils.getTileCount(36, 1), TARecordUtil.getLongList(2792721172l));	
		countPosition.put(NumberUtils.getTileCount(35, 1), TARecordUtil.getLongList(1102304348803l, 61575443876731l));	
		
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		
		for (BLATRecord br : results) {
			System.out.println("br: " + br.toString());
		}
		assertEquals(1, results.size());
		System.out.println(results.get(0).toString());
		assertEquals(0, results.stream().filter(br -> br.getScore() >= passingScore).count());
	}
	
	@Test
	public void realLifeSplits18() {
		/*
		 *  splitcon_chrMT_16130_chrMT_16213__true_1596188549171_252324_clip,
		 *  AGGGGGTTTTNNTGTGGATTGGGTTTTTATGTACTACAGGTGGTCAAGTATTTATGGTACCGTGCAATCAACTCTCAACTATCACACATCAACTGCAACTCCAAAGCCACCCCTCACCCACTAGGATACCAACAAACCTACCCACCCTTAACAGTACATAGTACATAAAGCCATTTACCGTAATAAAA,
		 */
		String seq = "AGGGGGTTTTNNTGTGGATTGGGTTTTTATGTACTACAGGTGGTCAAGTATTTATGGTACCGTGCAATCAACTCTCAACTATCACACATCAACTGCAACTCCAAAGCCACCCCTCACCCACTAGGATACCAACAAACCTACCCACCCTTAACAGTACATAGTACATAAAGCCATTTACCGTAATAAAA,";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chrMT_16130_chrMT_16213__true_1596188549171_252324_clip";
		
		countPosition.put(NumberUtils.getTileCount(97, 0), TARecordUtil.getLongList(80267450632041l));
		countPosition.put(NumberUtils.getTileCount(40, 0), TARecordUtil.getLongList(4611823460482664201l));	
		
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		assertEquals(0, results.size());
		
		/*
		 * get individual blat recs
		 */
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, name, r.getSequence().length(), pcpm);
		assertEquals(1, blatRecs.size());
		blatRecs.sort(null);
		for (BLATRecord br : blatRecs.get(0)) {
			System.out.println("blat record: " + br.toString());
		}
		assertEquals(2, blatRecs.get(0).length);
		assertEquals("52	52	0	0	0	0	0	0	0	-	splitcon_chrMT_16130_chrMT_16213__true_1596188549171_252324_clip	189	12	64	chrMT	16569	16126	16178	1	52	125	16126", blatRecs.get(0)[0].toString());
		assertEquals("109	109	0	0	0	0	0	0	0	+	splitcon_chrMT_16130_chrMT_16213__true_1596188549171_252324_clip	189	73	182	chrMT	16569	16222	16331	1	109	73	16222", blatRecs.get(0)[1].toString());
	}
	
	@Test
	public void realLifeSplits19() {
		/*
		 *  splitcon_chrX_58390230_chrX_58390403__false_1596787635795_567776,
		 *  TTTGAAGGTTTCATTCATCTCACAGATTTAAACCTTTCTTCGGATTTAGCGGCTTGGAGACACTGTTATTGTACCTTTGAGAATCAAGTAGCTCATTGAGGTCAATGCAAAAAAAAGCGAATATCCCAGGATAAAAGCTAGAAGGAAGAGATCTGAGAAACTGCTTTGTGATGTGTGCCTTCATCTCACAGAGTTACACCTTTCCTTGGATTCAGAAGTTTGGAAACTGTTTTTGCCCATTCTGTGAATGTATATTTGGGAGCTCATTGAGGCCAATGGCGATAAAGTGAATATCCCAGTTTAAAAACGAA,
		 */
		String seq = "TTTGAAGGTTTCATTCATCTCACAGATTTAAACCTTTCTTCGGATTTAGCGGCTTGGAGACACTGTTATTGTACCTTTGAGAATCAAGTAGCTCATTGAGGTCAATGCAAAAAAAAGCGAATATCCCAGGATAAAAGCTAGAAGGAAGAGATCTGAGAAACTGCTTTGTGATGTGTGCCTTCATCTCACAGAGTTACACCTTTCCTTGGATTCAGAAGTTTGGAAACTGTTTTTGCCCATTCTGTGAATGTATATTTGGGAGCTCATTGAGGCCAATGGCGATAAAGTGAATATCCCAGTTTAAAAACGAA,";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chrX_58390230_chrX_58390403__false_1596787635795_567776";
		
		countPosition.put(NumberUtils.getTileCount(108, 0), TARecordUtil.getLongList(45082916162722l));
		countPosition.put(NumberUtils.getTileCount(36, 2), TARecordUtil.getLongList(4611801470087732099l));	
		countPosition.put(NumberUtils.getTileCount(23, 0), TARecordUtil.getLongList(168228218458882l));	
		countPosition.put(NumberUtils.getTileCount(18, 5), TARecordUtil.getLongList(277079869623160l));	
		countPosition.put(NumberUtils.getTileCount(18, 2), TARecordUtil.getLongList(4611828957878476764l));	
		countPosition.put(NumberUtils.getTileCount(17, 5), TARecordUtil.getLongList(275980357974295l));	
		countPosition.put(NumberUtils.getTileCount(17, 3), TARecordUtil.getLongList(182521869761109l, 182521873151281l, 182521873154823l, 4611983989017945253l, 4611983989017964033l));	
		
		System.out.println("seq length: " + seq.length());
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		assertEquals(0, results.size());
		assertEquals(0, results.stream().filter(br -> br.getScore() >= passingScore).count());
	}
	
	@Test
	public void realLifeSplits20() {
		/*
		 *  splitcon_chr7_61969207_chr12_38009070__true_1596788372148_152220,
		 *  CTTTGAGGCCTTCGTTGGAAACGGGATTTCTTCATTTCATGCTAGACAGAAGAATTCTCAGTAACTTCTTTGTGTTGTGTGTATTCAACTCACAGATTGGAACGTCCCTTTACACAGAGCAGATTTGAAACACTCTTTTTGTGGAATTTGCAAGTGGAGATTTCAAGCGATTTGATGCCAACAGTAGAAAAGGAAATATCTTCAAATAAAAACTAGACAGAATCATTCTCAGAAAGTGCTTTGTGATGTGTGCGTTCAACTCACAGAGTTTAACCTTTCTTTTCATAGAGGAGTTTGGAAACACACTGTTTGTAAAGTCTGCAATTGGATATATGGACCTGTTTGA,
		 */
		String seq = "CTTTGAGGCCTTCGTTGGAAACGGGATTTCTTCATTTCATGCTAGACAGAAGAATTCTCAGTAACTTCTTTGTGTTGTGTGTATTCAACTCACAGATTGGAACGTCCCTTTACACAGAGCAGATTTGAAACACTCTTTTTGTGGAATTTGCAAGTGGAGATTTCAAGCGATTTGATGCCAACAGTAGAAAAGGAAATATCTTCAAATAAAAACTAGACAGAATCATTCTCAGAAAGTGCTTTGTGATGTGTGCGTTCAACTCACAGAGTTTAACCTTTCTTTTCATAGAGGAGTTTGGAAACACACTGTTTGTAAAGTCTGCAATTGGATATATGGACCTGTTTGA,";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr7_61969207_chr12_38009070__true_1596788372148_152220";
		
		countPosition.put(NumberUtils.getTileCount(132, 4), TARecordUtil.getLongList(222102644437979l));
		countPosition.put(NumberUtils.getTileCount(126, 0), TARecordUtil.getLongList(106653923520372l));	
		countPosition.put(NumberUtils.getTileCount(107, 0), TARecordUtil.getLongList(127544644455266l));	
		countPosition.put(NumberUtils.getTileCount(98, 4), TARecordUtil.getLongList(235296783969092l, 235296783970623l));	
		countPosition.put(NumberUtils.getTileCount(94, 0), TARecordUtil.getLongList(106653923518503l));	
		countPosition.put(NumberUtils.getTileCount(84, 0), TARecordUtil.getLongList(117649039796953l));	
		countPosition.put(NumberUtils.getTileCount(74, 0), TARecordUtil.getLongList(10996411902584l, 10996411903093l));	
		
		System.out.println("seq length: " + seq.length());
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		assertEquals(2, results.size());
		System.out.println(results.get(0).toString());
		System.out.println(results.get(1).toString());
		assertEquals(1, results.stream().filter(br -> br.getScore() >= passingScore).count());
		assertEquals("328	335	4	0	0	1	1	2	2212	+	splitcon_chr7_61969207_chr12_38009070__true_1596788372148_152220	347	10	345	chr7	159138663	61967796	61970343	3	86,138,111	10,97,235	61967796,61969072,61970232", results.stream().filter(br -> br.getScore() >= passingScore).map(BLATRecord::toString).collect(Collectors.joining(",")));
	}
	
	@Test
	public void realLifeSplits21() {
		/*
		 *  splitcon_chr7_61969207_chr12_38009070__true_1596788372148_152220,
		 *  CTTTGAGGCCTTCGTTGGAAACGGGATTTCTTCATTTCATGCTAGACAGAAGAATTCTCAGTAACTTCTTTGTGTTGTGTGTATTCAACTCACAGATTGGAACGTCCCTTTACACAGAGCAGATTTGAAACACTCTTTTTGTGGAATTTGCAAGTGGAGATTTCAAGCGATTTGATGCCAACAGTAGAAAAGGAAATATCTTCAAATAAAAACTAGACAGAATCATTCTCAGAAAGTGCTTTGTGATGTGTGCGTTCAACTCACAGAGTTTAACCTTTCTTTTCATAGAGGAGTTTGGAAACACACTGTTTGTAAAGTCTGCAATTGGATATATGGACCTGTTTGA
		 */
		String seq = "CTTTGAGGCCTTCGTTGGAAACGGGATTTCTTCATTTCATGCTAGACAGAAGAATTCTCAGTAACTTCTTTGTGTTGTGTGTATTCAACTCACAGATTGGAACGTCCCTTTACACAGAGCAGATTTGAAACACTCTTTTTGTGGAATTTGCAAGTGGAGATTTCAAGCGATTTGATGCCAACAGTAGAAAAGGAAATATCTTCAAATAAAAACTAGACAGAATCATTCTCAGAAAGTGCTTTGTGATGTGTGCGTTCAACTCACAGAGTTTAACCTTTCTTTTCATAGAGGAGTTTGGAAACACACTGTTTGTAAAGTCTGCAATTGGATATATGGACCTGTTTGA";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr7_61969207_chr12_38009070__true_1596788372148_152220";
		
		countPosition.put(NumberUtils.getTileCount(221, 2), TARecordUtil.getLongList(2200318881557l));
		countPosition.put(NumberUtils.getTileCount(181, 2), TARecordUtil.getLongList(10996411903093l));	
		countPosition.put(NumberUtils.getTileCount(150, 1), TARecordUtil.getLongList(202311435137993l));	
		countPosition.put(NumberUtils.getTileCount(140, 1), TARecordUtil.getLongList(56076388640761l));	
		countPosition.put(NumberUtils.getTileCount(120, 3), TARecordUtil.getLongList(235296783968412l));	
		countPosition.put(NumberUtils.getTileCount(120, 1), TARecordUtil.getLongList(235296783969092l, 235296783969601l));	
		countPosition.put(NumberUtils.getTileCount(107, 0), TARecordUtil.getLongList(127544644455266l));	
		
		System.out.println("seq length: " + seq.length());
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + pcpm.getChrPositionFromLongPosition(NumberUtils.getLongPositionValueFromPackedLong(l)).toIGVString() + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		assertEquals(3, results.size());
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		for (BLATRecord br : results) {
			System.out.println(br.toString());
		}
		assertEquals(2, results.stream().filter(br -> br.getScore() >= passingScore).count());
	}
	
	@Test
	public void realLifeSplits22() {
		/*
		 *  chr15_34031839_true_-,
		 *  CTTTGAGGCCTTCGTTGGAAACGGGATTTCTTCATTTCATGCTAGACAGAAGAATTCTCAGTAACTTCTTTGTGTTGTGTGTATTCAACTCACAGATTGGAACGTCCCTTTACACAGAGCAGATTTGAAACACTCTTTTTGTGGAATTTGCAAGTGGAGATTTCAAGCGATTTGATGCCAACAGTAGAAAAGGAAATATCTTCAAATAAAAACTAGACAGAATCATTCTCAGAAAGTGCTTTGTGATGTGTGCGTTCAACTCACAGAGTTTAACCTTTCTTTTCATAGAGGAGTTTGGAAACACACTGTTTGTAAAGTCTGCAATTGGATATATGGACCTGTTTGA,
		 */
		String seq = "GGGGGAGGAGCCAAGATGGCCGAATAGGAACAGCTCCGGTCTACAGCTCCCAGTGTGAGCAACG";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "chr15_34031839_true_-";
		
		countPosition.put(NumberUtils.getTileCount(52, 1), TARecordUtil.getLongList(4611686018867029445l));
		countPosition.put(NumberUtils.getTileCount(50, 0), TARecordUtil.getLongList(1102472321803l, 4611686019344588891l));	
		countPosition.put(NumberUtils.getTileCount(49, 1), TARecordUtil.getLongList(4611688219174044208l));	
		countPosition.put(NumberUtils.getTileCount(48, 2), TARecordUtil.getLongList(294553505, 326382629, 574836645, 1972351592, 1996049507, 2771972355l, 4611690416539915602l));	
		
		System.out.println("seq length: " + seq.length());
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(0, splits.size());
	}
	
	@Test
	public void realLifeSplits23() {
		/*
		 *  splitcon_chr10_127633807_chr15_34031839__true_1597229449894_602255-,
		 *  TTCGGCGTTGCTCACACTGGGAGCTGTAGACCGGAGCTGTTCCTATTCGGCCATCTTGGCTCCTCCCCCTATAGTGTTATTTCATTTTCCAAGGATACCTGCATTTCCACCAGAAAATATTTAAGGGGTTACACATTTCCCGTTTTGGTTAACCTGGATAAATGCGCGTATTTTATTTCTGTTTTCAG,
		 */
		String seq = "TTCGGCGTTGCTCACACTGGGAGCTGTAGACCGGAGCTGTTCCTATTCGGCCATCTTGGCTCCTCCCCCTATAGTGTTATTTCATTTTCCAAGGATACCTGCATTTCCACCAGAAAATATTTAAGGGGTTACACATTTCCCGTTTTGGTTAACCTGGATAAATGCGCGTATTTTATTTCTGTTTTCAG";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr10_127633807_chr15_34031839__true_1597229449894_602255";
		
		countPosition.put(NumberUtils.getTileCount(107, 0), TARecordUtil.getLongList(75868643634102l));
		countPosition.put(NumberUtils.getTileCount(49, 1), TARecordUtil.getLongList(7698304795184l));	
		countPosition.put(NumberUtils.getTileCount(48, 1), TARecordUtil.getLongList(4611816862105330075l));	
		countPosition.put(NumberUtils.getTileCount(48, 0), TARecordUtil.getLongList(4611816862119100198l));	
		countPosition.put(NumberUtils.getTileCount(47, 1), TARecordUtil.getLongList(9897416559913l));	
		
		System.out.println("seq length: " + seq.length());
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		assertEquals(0, results.size());
		
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, name, seq.length(), pcpm);
		assertEquals(1, blatRecs.size());
		
//		for (BLATRecord[] brs : blatRecs) {
//			for (BLATRecord br: brs) {
//				System.out.println("br from brs: " + br.toString());
//			}
//		}
		assertEquals("60	60	0	0	0	0	0	0	0	-	splitcon_chr10_127633807_chr15_34031839__true_1597229449894_602255	188	9	69	chr10	135534747	127633806	127633866	1	60	119	127633806", blatRecs.get(0)[0].toString());
		assertEquals("119	119	0	0	0	0	0	0	0	+	splitcon_chr10_127633807_chr15_34031839__true_1597229449894_602255	188	69	188	chr15	102531392	34031838	34031957	1	119	69	34031838", blatRecs.get(0)[1].toString());
	}
	
	@Test
	public void realLifeSplits24() {
		/*
		 *  splitcon_chr5_96617965_chr5_96618035__true_1596696734820_781431-,
		 */
		String seq = "CACAGTGTATAAGACTTCCTTATACATTTCCACCAACAGTGTATAAGACTTATACACAACAGTGTATAAGACTTATACACAACAGTGTATAAGACAGTGTATAAGATGTTGGCAAGGAACTCTACAACCTTGCCAACAGCTGTTATTTTCTCTCTTTTTAATAATAGCAATCTTTAATGTGTTAAGATTATAACTTTTATTTTTAATTTGAATTTATATGATTATTATATAACTTGAGAATTTTTTCTTATACAAGTTAGCCA";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr5_96617965_chr5_96618035__true_1596696734820_781431";
		
		countPosition.put(NumberUtils.getTileCount(82, 0), TARecordUtil.getLongList(1100489872410l));
		countPosition.put(NumberUtils.getTileCount(72, 0), TARecordUtil.getLongList(36284861961343l));
		countPosition.put(NumberUtils.getTileCount(50, 0), TARecordUtil.getLongList(36284861961297l, 36284861961320l));
		countPosition.put(NumberUtils.getTileCount(37, 1), TARecordUtil.getLongList(130842861950165l));
		countPosition.put(NumberUtils.getTileCount(17, 0), TARecordUtil.getLongList(24190234055693l));
		countPosition.put(NumberUtils.getTileCount(14, 0), TARecordUtil.getLongList(24190297952852l));
		countPosition.put(NumberUtils.getTileCount(13, 0), TARecordUtil.getLongList(24190288217943l, 4611923513906431087l));
		countPosition.put(NumberUtils.getTileCount(10, 0), TARecordUtil.getLongList(23090722491985l, 24190167578688l, 24190182328473l, 24190236397296l, 24190245576419l, 24190251357525l, 24190264495532l, 24190265372582l, 24190272786434l, 139638926789045l, 4611926812479300845l));
		
		System.out.println("seq length: " + seq.length());
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		
		for (BLATRecord br : results) {
			System.out.println("br: " + br.toString());
		}
		assertEquals(3, results.size());
		assertEquals(0, results.stream().filter(br -> br.getScore() >= passingScore).count());
	}
	
	@Test
	public void realLifeSplits25() {
		/*
		 *  splitcon_chr19_40134782_chr20_47762412__true_1598270196826_578106-,
		 */
		String seq = "AAGCCTTCCAAAAGGCTCAAGGAGAAGGTGAAAGGCATCATAGGACCTGGCTTTGAAAGGCAGACAGTGTCACTTCTATGACATTATATTGATCAAAGTGAGCCATACACAAGCCCAAATCAAAGGGAGGGGGGTTTCATAAGATGTGACCACAGTGGAGTGCCCACTGCACTCCAGCCTGGGCAACAATAGTGAGACCTGTCTCTGTTAGAAAAAAATAAAAAAGAAAAAGAAAACTAGGTCCAATGAACACTGAGGAAACAAG";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr19_40134782_chr20_47762412__true_1598270196826_578106";
		
		countPosition.put(NumberUtils.getTileCount(136, 0), TARecordUtil.getLongList(2699578957l));
		countPosition.put(NumberUtils.getTileCount(76, 0), TARecordUtil.getLongList(194616324452069l));
		countPosition.put(NumberUtils.getTileCount(50, 0), TARecordUtil.getLongList(53878769284487l));
		countPosition.put(NumberUtils.getTileCount(35, 1), TARecordUtil.getLongList(53878769293323l));
		countPosition.put(NumberUtils.getTileCount(19, 1), TARecordUtil.getLongList(34087560093447l));
		countPosition.put(NumberUtils.getTileCount(13, 0), TARecordUtil.getLongList(85764606598966l));
		countPosition.put(NumberUtils.getTileCount(12, 0), TARecordUtil.getLongList(63774374007687l, 63774374056428l));
		countPosition.put(NumberUtils.getTileCount(11, 0), TARecordUtil.getLongList(194616235376301l));
		
		System.out.println("seq length: " + seq.length());
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		assertEquals(0, results.size());
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, name, seq.length(), pcpm);
		assertEquals(1, blatRecs.size());
		
//		for (BLATRecord[] brs : blatRecs) {
//			for (BLATRecord br: brs) {
//				System.out.println("br from brs: " + br.toString());
//			}
//		}
	}
	
	@Test
	public void realLifeSplits26() {
		/*
		 *  splitcon_chr2_59769597_chr2_59769659__true_1599457097433_514634-,
		 */
		String seq = "GCATGGTTCTAGGATCACAGAATGTTCTCCATGAAGACTTTCCCATTCATTGATGGATTAATTGACTGGTTGTCCTTCATATCCTGCACTAACCTCTATTACAGCATGGAGAGGTACTTTTGTATGGATTAATTGACTGGTTGTCCTTCATATCCTGCACTAACCTCTATTACAGCATGGAGAGGTACTTTTGTTTCTCTGTCCCACTGCTACAACCCCTTCCCCTCACTTCTCATGGCTTCTCAGTAGAAAAAGACCAAACTTAGACA";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr2_59769597_chr2_59769659__true_1599457097433_514634";
		
		countPosition.put(NumberUtils.getTileCount(134, 0), TARecordUtil.getLongList(135240239236658l));
		countPosition.put(NumberUtils.getTileCount(111, 0), TARecordUtil.getLongList(309020158));
		countPosition.put(NumberUtils.getTileCount(7, 0), TARecordUtil.getLongList(4611922413758236086l));
		countPosition.put(NumberUtils.getTileCount(6, 0), TARecordUtil.getLongList(23090107560703l));
		
		System.out.println("seq length: " + seq.length());
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		for (IntLongPairs ilps : potentialSplits) {
			System.out.println("ilps: " + ilps.toString());
		}
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		assertEquals("196	198	0	0	0	1	71	1	0	+	splitcon_chr2_59769597_chr2_59769659__true_1599457097433_514634	269	0	268	chr2	243199373	59769536	59769734	2	123,75	0,194	59769536,59769659", results.get(0).toString());
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, name, seq.length(), pcpm);
		assertEquals(1, blatRecs.size());
		
//		for (BLATRecord[] brs : blatRecs) {
//			for (BLATRecord br: brs) {
//				System.out.println("br from brs: " + br.toString());
//			}
//		}
		assertEquals(1, results.size());
		System.out.println(results.get(0).toString());
		assertEquals(0, results.stream().filter(br -> br.getScore() >= passingScore).count());
	}
	
	@Test
	public void realLifeSplits28() {
		/*
		 *  splitcon_chr8_39360286_chr8_39360413__true_1603194926354_561727-,
		 */
		String seq = "TGGTATAAAAATTTACTAATTAATAAAAAAGAGAATAAGGGCCCTATTTTTATTATTTTATTCTTGTAACTTTTCTAAAAGTTTAAAATTATTTCCAAAACAAATGTTTAAAAAGTGCAGATGAAATAAAGACAATTTCAGATGAAAAGAAACTGAAGAGAATCCAGCAGGCTAAAGAGAAATATCATACATTAAGTAACCTACTACAAGGTATGAAAAGCTCTGCAAGTGAAAAATGTATGTGTGTGTGTGTGTATATATATATTCATATATATGAATATTTATGTATATTCATATATATGAAATACACATCTATGAATATATATGAATATATGAATATTTATGACCATATATGAATATATATACATATATGAATATTTATGAACATATATACATATATGAA";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr8_39360286_chr8_39360413__true_1603194926354_561727";
		
		countPosition.put(NumberUtils.getTileCount(203, 2), TARecordUtil.getLongList(54977013544407l));
		countPosition.put(NumberUtils.getTileCount(58, 0), TARecordUtil.getLongList(279277385610937l));
		countPosition.put(NumberUtils.getTileCount(22, 0), TARecordUtil.getLongList(360641246066435l, 402422687921951l, 402422687921989l));
		
		System.out.println("seq length: " + seq.length());
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		for (IntLongPairs ilps : potentialSplits) {
			System.out.println("ilps: " + ilps.toString());
		}
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.filter(sa -> sa.isPresent())
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		
		assertEquals(2, results.size());
		assertEquals(0, results.stream().filter(br -> br.getScore() >= passingScore).count());
		assertEquals("66	68	0	0	0	1	4	1	32	+	splitcon_chr8_39360286_chr8_39360413__true_1603194926354_561727	403	328	399	chr8	146364022	39360216	39360316	2	34,34	328,366	39360216,39360282", results.get(0).toString());
		assertEquals("330	336	2	0	0	1	14	3	26	+	splitcon_chr8_39360286_chr8_39360413__true_1603194926354_561727	403	50	399	chr8	146364022	39359916	39360278	4	204,70,28,34	50,254,328,366	39359916,39360142,39360216,39360244", results.get(1).toString());
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, name, seq.length(), pcpm);
		assertEquals(1, blatRecs.size());
		
//		for (BLATRecord[] brs : blatRecs) {
//			for (BLATRecord br: brs) {
//				System.out.println("br from brs: " + br.toString());
//			}
//		}
	}
	@Test
	public void realLifeSplits29() {
		/*
		 *  splitcon_chr5_178939187_chr5_178939496__true_1603195044083_189550_clip,-,
		 */
		String seq = "TAATTTTTTGTATTTTTAGTAGAAAAGGGGTTTCACCGTGTTAGCCAGGATGGTTTCCATCTCTTGCCCTTGTGATCCGCCCGCCTTGGCCTCCCAAAGTGCTGGGATTACAGGTGTGAGCCACCGCGCCCAGCCTTGCTCATTTCTGAATTTGATTGTTAGGTTTTTGTTATTGAGTGTGTTTTTTTAATTGAG";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr5_178939187_chr5_178939496__true_1603195044083_189550_clip";
		
		countPosition.put(NumberUtils.getTileCount(112, 0), TARecordUtil.getLongList(14294711726852l));
		countPosition.put(NumberUtils.getTileCount(60, 0), TARecordUtil.getLongList(135240990782630l));
		countPosition.put(NumberUtils.getTileCount(12, 0), TARecordUtil.getLongList(14294590348378l, 14294712305623l));
		
		System.out.println("seq length: " + seq.length());
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + pcpm.getChrPositionFromLongPosition(NumberUtils.getLongPositionValueFromPackedLong(l)).toIGVString() + " (" + NumberUtils.getLongPositionValueFromPackedLong(l) + "), start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		for (IntLongPairs ilps : potentialSplits) {
			System.out.println("ilps: " + ilps.toString());
		}
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.filter(sa -> sa.isPresent())
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		
		assertEquals(1, results.size());
		assertEquals(1, results.stream().filter(br -> br.getScore() >= passingScore).count());
		assertEquals("181	182	0	0	0	0	0	1	308	+	splitcon_chr5_178939187_chr5_178939496__true_1603195044083_189550_clip	195	13	194	chr5	180915260	178939063	178939553	2	110,72	13,123	178939063,178939481", results.get(0).toString());
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, name, seq.length(), pcpm);
		assertEquals(1, blatRecs.size());
		
//		for (BLATRecord[] brs : blatRecs) {
//			for (BLATRecord br: brs) {
//				System.out.println("br from brs: " + br.toString());
//			}
//		}
	}
	@Test
	public void realLifeSplits30() {
		/*
		 *  splitcon_chr14_64866810_chr14_64867112__true_1603769470012_401600_clip
		 */
		String seq = "ATGAACCATTGCCTGCTCCTGGCCAGTGTTTTTGATTTAAAAGAGAAATACAGGCCGGGTGCAGTGGCTCACGCCTGTAATCCCAGCACTTTGGGAGGCCGAGGTCCTAGATCGCTTGAGCCCAGGAATTCAAGACCAGCTTGGGCAATGGTGAAACCCATCTCTACCA";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr14_64866810_chr14_64867112__true_1603769470012_401600_clip";
		
		countPosition.put(NumberUtils.getTileCount(119, 0), TARecordUtil.getLongList(41783706658779l));
		countPosition.put(NumberUtils.getTileCount(93, 0), TARecordUtil.getLongList(2264802952l));
		countPosition.put(NumberUtils.getTileCount(20, 1), TARecordUtil.getLongList(127545580591165l));
		countPosition.put(NumberUtils.getTileCount(20, 0), TARecordUtil.getLongList(118749515111536l));
		
		System.out.println("seq length: " + seq.length());
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + pcpm.getChrPositionFromLongPosition(NumberUtils.getLongPositionValueFromPackedLong(l)).toIGVString() + " (" + NumberUtils.getLongPositionValueFromPackedLong(l) + "), start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		for (IntLongPairs ilps : potentialSplits) {
			System.out.println("ilps: " + ilps.toString());
		}
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.filter(sa -> sa.isPresent())
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		
		assertEquals(1, results.size());
		assertEquals(1, results.stream().filter(br -> br.getScore() >= passingScore).count());
		assertEquals("168	169	0	0	0	0	0	1	301	+	splitcon_chr14_64866810_chr14_64867112__true_1603769470012_401600_clip	169	0	168	chr14	107349540	64866772	64867242	2	105,64	0,105	64866772,64867178", results.get(0).toString());
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, name, seq.length(), pcpm);
		assertEquals(1, blatRecs.size());
		
//		for (BLATRecord[] brs : blatRecs) {
//			for (BLATRecord br: brs) {
//				System.out.println("br from brs: " + br.toString());
//			}
//		}
	}
	
	@Test
	public void realLifeSplits31() {
		/*
		 *  splitcon_chr17_80666697_chr17_80666858__true_1603769669765_514144,
		 */
		String seq = "AAAGGAAGACTCTGTCTAAAAAAAAAAAAAAAAAGAAAAGAAAAGAAAGAAAGAAAACGTATCTTAAGCAAAACTGTAACCTCATGGATGGCCCAGATGTACCAGAGCCCTGAAGACAAAACCCATTTTCACACAGGGCTCCCGCCACAGCCCCGCCCATGGCCCCACCCACTGCCGCCATGCCCTGCCCCCTTACCACCAGCCCCACCCACTGCCCCCGTGCCCTGCCCCCTTGCCACCCATCCTGCCTACTGCCAAGTTCCCTCTCCCCTTACCACCCACCCAACCCACTGCCCCCATGCCCTCTCCCCT";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr17_80666697_chr17_80666858__true_1603769669765_514144";
		
		countPosition.put(NumberUtils.getTileCount(177, 1), TARecordUtil.getLongList(49480604088360l));
		countPosition.put(NumberUtils.getTileCount(117, 1), TARecordUtil.getLongList(194616138955084l));
		countPosition.put(NumberUtils.getTileCount(48, 0), TARecordUtil.getLongList(197914673838335l));
		countPosition.put(NumberUtils.getTileCount(42, 1), TARecordUtil.getLongList(197914673838295l));
		countPosition.put(NumberUtils.getTileCount(9, 0), TARecordUtil.getLongList(3301069788360l, 3301071379954l));
		
		System.out.println("seq length: " + seq.length());
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + pcpm.getChrPositionFromLongPosition(NumberUtils.getLongPositionValueFromPackedLong(l)).toIGVString() + " (" + NumberUtils.getLongPositionValueFromPackedLong(l) + "), start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		for (IntLongPairs ilps : potentialSplits) {
			System.out.println("ilps: " + ilps.toString());
		}
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.filter(sa -> sa.isPresent())
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		
		assertEquals(0, results.size());
		assertEquals(0, results.stream().filter(br -> br.getScore() >= passingScore).count());
//		assertEquals("168	169	0	0	0	0	0	1	301	+	splitcon_chr14_64866810_chr14_64867112__true_1603769470012_401600_clip	169	0	168	chr14	12345	64866772	64867242	2	105,64	0,105	64866772,64867178", results.get(0).toString());
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplitsNew(splits, name, seq.length(), pcpm);
		assertEquals(1, blatRecs.size());
		assertEquals(2, blatRecs.get(0).length);
		assertEquals("21	21	0	0	0	0	0	0	0	+	splitcon_chr17_80666697_chr17_80666858__true_1603769669765_514144	312	3	24	chr17	81195210	36324761	36324782	1	21	3	36324761", blatRecs.get(0)[0].toString());
		assertEquals("258	261	2	0	0	0	0	1	160	+	splitcon_chr17_80666697_chr17_80666858__true_1603769669765_514144	312	45	305	chr17	81195210	80666575	80666996	2	132,129	45,177	80666575,80666867", blatRecs.get(0)[1].toString());
		
//		for (BLATRecord[] brs : blatRecs) {
//			for (BLATRecord br: brs) {
//				System.out.println("br from brs: " + br.toString());
//			}
//		}
	}
	
	@Test
	public void realLifeSplits32() {
		/*
		 *  splitcon_chrX_47024632_chrX_47024936__true_1603769762689_724626,
		 */
		String seq = "GGGGTCAGAATACTTAATGCCACTGAAATAGATAAAAAAGAATGGGTGGCTGGGCGCGGTGGCTCACACCTGTAATCCCAGCACTTTGGGAGGCTGAGGCAGGCGGATCACCTGAAGTCAGAAGTTCGAGACTGGCCTGGCCAACATGGTGAAACTCCATCTCTACTAAAAATACAAAAATTAGCCAGGAGTGGTGGTGCATGTCTGTAATCCCAGCTACTCGGGAGGCTGAGGCAGGAGAATCGCTTGAACCCGGGAGGCGGAGGCTGCAGTGAGCCGAGATTGTGCCACTGGACTCCAGCCTGGGTGACAGGGCGAGACTCCGT";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chrX_47024632_chrX_47024936__true_1603769762689_724626";
		
		countPosition.put(NumberUtils.getTileCount(84, 0), TARecordUtil.getLongList(102257509441436l));
		countPosition.put(NumberUtils.getTileCount(65, 0), TARecordUtil.getLongList(4611750892623640414l));
		countPosition.put(NumberUtils.getTileCount(63, 1), TARecordUtil.getLongList(50580469672470l));
		countPosition.put(NumberUtils.getTileCount(59, 0), TARecordUtil.getLongList(50580552512082l));
		countPosition.put(NumberUtils.getTileCount(25, 0), TARecordUtil.getLongList(133043812568530l));
		
		System.out.println("seq length: " + seq.length());
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + pcpm.getChrPositionFromLongPosition(NumberUtils.getLongPositionValueFromPackedLong(l)).toIGVString() + " (" + NumberUtils.getLongPositionValueFromPackedLong(l) + "), start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		for (IntLongPairs ilps : potentialSplits) {
			System.out.println("ilps: " + ilps.toString());
		}
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.filter(sa -> sa.isPresent())
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		
		assertEquals(0, results.size());
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplitsNew(splits, name, seq.length(), pcpm);
		assertEquals(0, blatRecs.size());
	}
	@Test
	public void realLifeSplits33() {
		/*
		 *  splitcon_chr7_25345087_chr7_25345211__false_1604284945574_644092,
		 */
		String seq = "GTCTCAGGATTATATATATAATCGCTTTGATACATATATATGCATATATGTATATATAAAATCCCTATTCATATGTATATCTCATATATACATAGGATTATTTCCAGCTTGATTGGTCTGGTGCATAATCGCTTTGATACATATATATGCATATATGTATATATAAAATCCCTATTCATATGTATATCTCATATATACATAGGATTATTTCCAGCTTGATTGGTCTGGTGCATATATATATACACACACTCTCACACCTGCCTCAGACCAATCACAAGATGGAAATAATCCAGTTGAATCCTGATTCGTGCTCAGCATACATGGAAATACTAATGGGTATACTTTCTGGAGAACAGGGTAAATGTCATCCA";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr7_25345087_chr7_25345211__false_1604284945574_644092";
		
		countPosition.put(NumberUtils.getTileCount(115, 0), TARecordUtil.getLongList(1259001994));
		countPosition.put(NumberUtils.getTileCount(106, 0), TARecordUtil.getLongList(136340700846235l));
		countPosition.put(NumberUtils.getTileCount(85, 0), TARecordUtil.getLongList(301267445012906l));
		countPosition.put(NumberUtils.getTileCount(34, 0), TARecordUtil.getLongList(250689910135166l));
		countPosition.put(NumberUtils.getTileCount(13, 0), TARecordUtil.getLongList(36285265937339l));
		countPosition.put(NumberUtils.getTileCount(12, 0), TARecordUtil.getLongList(37384726210153l));
		
		System.out.println("seq length: " + seq.length());
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + pcpm.getChrPositionFromLongPosition(NumberUtils.getLongPositionValueFromPackedLong(l)).toIGVString() + " (" + NumberUtils.getLongPositionValueFromPackedLong(l) + "), start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		for (IntLongPairs ilps : potentialSplits) {
			System.out.println("ilps: " + ilps.toDetailedString());
		}
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.filter(sa -> sa.isPresent())
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		
		assertEquals(1, results.size());
		assertEquals(0, results.stream().filter(br -> br.getScore() >= passingScore).count());
//		assertEquals("168	169	0	0	0	0	0	1	301	+	splitcon_chr14_64866810_chr14_64867112__true_1603769470012_401600_clip	169	0	168	chr14	12345	64866772	64867242	2	105,64	0,105	64866772,64867178", results.get(0).toString());
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplitsNew(splits, name, seq.length(), pcpm);
		assertEquals(1, blatRecs.size());
		assertEquals(2, blatRecs.get(0).length);
		assertEquals("25	25	0	0	0	0	0	0	0	+	splitcon_chr7_25345087_chr7_25345211__false_1604284945574_644092	371	33	58	chr7	159138663	148563703	148563728	1	25	33	148563703", blatRecs.get(0)[0].toString());
		assertEquals("242	245	0	0	0	1	2	2	123	+	splitcon_chr7_25345087_chr7_25345211__false_1604284945574_644092	371	124	370	chr7	159138663	25344983	25345351	3	104,46,95	124,228,276	25344983,25345210,25345256", blatRecs.get(0)[1].toString());
		
//		for (BLATRecord[] brs : blatRecs) {
//			for (BLATRecord br: brs) {
//				System.out.println("br from brs: " + br.toString());
//			}
//		}
	}
	
	@Test
	public void realLifeSplits34() {
		/*
		 *  splitcon_chr7_25345087_chr7_25345211__false_1604284945574_644092,
		 */
		String seq = "GGATTGAAAATATTCATGGCATTTGGGAAGAAGAGAAAGGGCAGGTCTTTGCAGTATGCTTCTAGATCTATTTTAAATATGTAATAATAGTGGAGGGATCAGTTTATTTTTAGCCTGTTTACCTTAAGAACATATATAATCTCAGTTAATTATGTGTTGATACAATAGGTGAAGATAGGGATATATGAAGGAATCTATGCAAACTCAAGATAATCACACTTAATTTTTTAAAGTGCAAGTGAGAACACAAATCATTTCTAG";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr6_153932501_chr6_153932720_1_true_1606270441498_475180";
		
		countPosition.put(NumberUtils.getTileCount(148, 0), TARecordUtil.getLongList(111051890879997l));
		countPosition.put(NumberUtils.getTileCount(132, 2), TARecordUtil.getLongList(6598286241023l));
		countPosition.put(NumberUtils.getTileCount(22, 0), TARecordUtil.getLongList(64872402513363l));
		countPosition.put(NumberUtils.getTileCount(10, 0), TARecordUtil.getLongList(4611875135647880198l));
		
		System.out.println("seq length: " + seq.length());
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + pcpm.getChrPositionFromLongPosition(NumberUtils.getLongPositionValueFromPackedLong(l)).toIGVString() + " (" + NumberUtils.getLongPositionValueFromPackedLong(l) + "), start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		for (IntLongPairs ilps : potentialSplits) {
			System.out.println("ilps: " + ilps.toDetailedString());
		}
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.filter(sa -> sa.isPresent())
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		
		assertEquals(1, results.size());
		assertEquals(1, results.stream().filter(br -> br.getScore() >= passingScore).count());
		assertEquals("252	255	2	0	0	0	0	1	159	+	splitcon_chr6_153932501_chr6_153932720_1_true_1606270441498_475180	261	6	260	chr6	171115067	153932406	153932820	2	144,111	6,150	153932406,153932709", results.get(0).toString());
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplitsNew(splits, name, seq.length(), pcpm);
		assertEquals(1, blatRecs.size());
		assertEquals(1, blatRecs.get(0).length);
		assertEquals("252	255	2	0	0	0	0	1	159	+	splitcon_chr6_153932501_chr6_153932720_1_true_1606270441498_475180	261	6	260	chr6	171115067	153932406	153932820	2	144,111	6,150	153932406,153932709", blatRecs.get(0)[0].toString());
		
//		for (BLATRecord[] brs : blatRecs) {
//			for (BLATRecord br: brs) {
//				System.out.println("br from brs: " + br.toString());
//			}
//		}
	}
	@Test
	public void realLifeSplits35() {
		/*
		 *  splitcon_chr6_153932501_chr6_153932720_1_true_1606270441498_475180,
		 */
		String seq = "GCAGTTGCATGTAAATTTCACATGCGTTGTTTTTTAAAAATTGATGTATATATATATATCATGTATATATGTATATATGCGTATATGTGTATATATGTGTATATATGTATGTGTGTATATATGTGTATAAATGTGTATACATATATATATATATGATGTGTATATATATGTATATAGTTATACATATTTGGGGGTACATGTGAT";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "splitcon_chr6_153932501_chr6_153932720_1_true_1606270441498_475180";
		
		countPosition.put(NumberUtils.getTileCount(76, 0), TARecordUtil.getLongList(73667887999280l));
		countPosition.put(NumberUtils.getTileCount(58, 2), TARecordUtil.getLongList(608938145));
		countPosition.put(NumberUtils.getTileCount(49, 0), TARecordUtil.getLongList(157230771710330l));
//		countPosition.put(NumberUtils.getTileCount(10, 0), TARecordUtil.getLongList(4611875135647880198l));
		
		System.out.println("seq length: " + seq.length());
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + pcpm.getChrPositionFromLongPosition(NumberUtils.getLongPositionValueFromPackedLong(l)).toIGVString() + " (" + NumberUtils.getLongPositionValueFromPackedLong(l) + "), start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		for (IntLongPairs ilps : potentialSplits) {
			System.out.println("ilps: " + ilps.toDetailedString());
		}
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.filter(sa -> sa.isPresent())
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		
		assertEquals(1, results.size());
		assertEquals(1, results.stream().filter(br -> br.getScore() >= passingScore).count());
		assertEquals("200	204	2	0	0	0	0	2	74	+	splitcon_chr6_153932501_chr6_153932720_1_true_1606270441498_475180	204	0	203	chr3	198022430	116488150	116488428	3	70,73,61	0,70,143	116488150,116488296,116488367", results.get(0).toString());
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplitsNew(splits, name, seq.length(), pcpm);
		assertEquals(1, blatRecs.size());
		assertEquals(1, blatRecs.get(0).length);
		assertEquals("200	204	2	0	0	0	0	2	74	+	splitcon_chr6_153932501_chr6_153932720_1_true_1606270441498_475180	204	0	203	chr3	198022430	116488150	116488428	3	70,73,61	0,70,143	116488150,116488296,116488367", blatRecs.get(0)[0].toString());
		
//		for (BLATRecord[] brs : blatRecs) {
//			for (BLATRecord br: brs) {
//				System.out.println("br from brs: " + br.toString());
//			}
//		}
	}
//	@Test
//	public void realLifeSplits36() {
//		/*
//		 *  splitcon_chr7_124989445_chr7_124989504__true_1607391016216_844626,
//		 */
//		String seq = "TTATGCTGGAAATACGGAAAGAAAGTTTTTACAGGACTCTATTGATCGTACATTGATTCATTGAATTCTTTACAAGCAGACTATTTATAATTCGTACTTGTTCATAAATTTTTTTGAAGTAATTTATTGAGTGTCACATATATATATGTGTATATATATATATGTGTATATATATGTGTGTATATATATATGTATTTGGAGATACATATATCTAAATAAAAATATATACCAGGTGCCATGGGGAAATGACCAGAGACACACTAATTGAAGTGAGCACTTTG";
//		Map<Integer, TLongList> countPosition = new HashMap<>();
//		String name = "splitcon_chr7_124989445_chr7_124989504__true_1607391016216_844626";
//		
//		countPosition.put(NumberUtils.getTileCount(131, 0), TARecordUtil.getLongList(2200381901890l));
//		countPosition.put(NumberUtils.getTileCount(90, 0), TARecordUtil.getLongList(196813940018477l));
//		countPosition.put(NumberUtils.getTileCount(18, 0), TARecordUtil.getLongList(181420727737187l, 181420752010439l, 181420760465759l));
//		
//		System.out.println("seq length: " + seq.length());
//		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
//			for (long l : entry.getValue().toArray()) {
//				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + pcpm.getChrPositionFromLongPosition(NumberUtils.getLongPositionValueFromPackedLong(l)).toIGVString() + " (" + NumberUtils.getLongPositionValueFromPackedLong(l) + "), start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
//			}
//		}
//		
//		TARecord r =  new TARecord(seq, countPosition);
//		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
//		
//		List<IntLongPairs> potentialSplits = new ArrayList<>();
//		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
//		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
//		for (IntLongPairs ilps : potentialSplits) {
//			System.out.println("ilps: " + ilps.toDetailedString());
//		}
//		/*
//		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
//		 */
//		int passingScore = (int)(0.9 * seq.length());
//		List<BLATRecord> results = new ArrayList<>();
//		results.addAll(potentialSplits.stream()
//				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
//				.map(ilp ->  TARecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
//				.filter(sa -> sa.length > 0)
//				.map(s -> new BLATRecord(s))
//				.collect(Collectors.toList()));
//		
//		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
//		
//		assertEquals(1, results.size());
//		assertEquals(1, results.stream().filter(br -> br.getScore() >= passingScore).count());
//		assertEquals("200	204	2	0	0	0	0	2	74	+	splitcon_chr6_153932501_chr6_153932720_1_true_1606270441498_475180	204	0	203	chr3	12345	116488150	116488428	3	70,73,61	0,70,143	116488150,116488296,116488367", results.get(0).toString());
//		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplitsNew(splits, name, seq.length(), pcpm);
//		assertEquals(1, blatRecs.size());
//		assertEquals(1, blatRecs.get(0).length);
//		assertEquals("200	204	2	0	0	0	0	2	74	+	splitcon_chr6_153932501_chr6_153932720_1_true_1606270441498_475180	204	0	203	chr3	12345	116488150	116488428	3	70,73,61	0,70,143	116488150,116488296,116488367", blatRecs.get(0)[0].toString());
//		
////		for (BLATRecord[] brs : blatRecs) {
////			for (BLATRecord br: brs) {
////				System.out.println("br from brs: " + br.toString());
////			}
////		}
//	}
	
//	@Test
//	public void realLifeSplits28() {
//		/*
//		 *  GGNNCACNNAATAGATGTGTTTTCAAATGAAAATACAAGCTGGGTGCGGTGGCTCATGCCTGTAATCCCAGCACTTTGGGAGGCCAAGGTGGACAGATCACGAGGTCAGGAGTTCAAGACCAGTCTAGGCAATATGGTGAAACCCCATCTCTACTAAAAATACAAAAATT, splitcon_chr7_6685037_chr7_6685350__true_1600141584323_305840_clip
//		 */
//		String seq = "GGNNCACNNAATAGATGTGTTTTCAAATGAAAATACAAGCTGGGTGCGGTGGCTCATGCCTGTAATCCCAGCACTTTGGGAGGCCAAGGTGGACAGATCACGAGGTCAGGAGTTCAAGACCAGTCTAGGCAATATGGTGAAACCCCATCTCTACTAAAAATACAAAAATT";
//		Map<Integer, TLongList> countPosition = new HashMap<>();
//		String name = "splitcon_chr7_6685037_chr7_6685350__true_1600141584323_305840_clip";
//		
//		countPosition.put(NumberUtils.getTileCount(31, 0), TARecordUtil.getLongList(87962170564495l));
//		countPosition.put(NumberUtils.getTileCount(28, 0), TARecordUtil.getLongList(10996356619793l));
//		countPosition.put(NumberUtils.getTileCount(18, 0), TARecordUtil.getLongList(122047031025582l));
//		countPosition.put(NumberUtils.getTileCount(13, 0), TARecordUtil.getLongList(4611757488035486928l));
//		countPosition.put(NumberUtils.getTileCount(12, 0), TARecordUtil.getLongList(122047094920675l));
//		countPosition.put(NumberUtils.getTileCount(11, 0), TARecordUtil.getLongList(90161284695075l));
//		
//		System.out.println("seq length: " + seq.length());
//		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
//			for (long l : entry.getValue().toArray()) {
//				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + NumberUtils.getLongPositionValueFromPackedLong(l) + ", start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
//			}
//		}
//		
//		TARecord r =  new TARecord(seq, countPosition);
//		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
//		
//		List<IntLongPairs> potentialSplits = new ArrayList<>();
//		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
//		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
//		for (IntLongPairs ilps : potentialSplits) {
//			System.out.println("ilps: " + ilps.toString());
//		}
//		/*
//		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
//		 */
//		int passingScore = (int)(0.9 * seq.length());
//		List<BLATRecord> results = new ArrayList<>();
//		results.addAll(potentialSplits.stream()
//				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
//				.map(ilp ->  TARecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
//				.map(s -> new BLATRecord(s))
//				.collect(Collectors.toList()));
//		
//		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
//		
//		assertEquals(3, results.size());
//		assertEquals(0, results.stream().filter(br -> br.getScore() >= passingScore).count());
//		assertEquals("261	268	3	0	0	1	20	3	25	-	splitcon_chr19_21832833_chr19_21916389_4_true_1599654284052_407169	358	234	55	chr19	12345	21832520	21832813	4	66,117,41,44	58,134,256,302	21832520,21832599,21832723,21832769", results.get(0).toString());
//		assertEquals("281	286	2	0	0	1	10	2	13	+	splitcon_chr19_21832833_chr19_21916389_4_true_1599654284052_407169	358	12	307	chr19	12345	21916349	21916648	3	32,178,76	12,46,232	21916349,21916383,21916572", results.get(1).toString());
//		assertEquals("227	233	2	0	0	1	55	3	60	-	splitcon_chr19_21832833_chr19_21916389_4_true_1599654284052_407169	358	234	55	chr19	12345	21832520	21832813	4	66,82,41,44	58,170,256,302	21832520,21832635,21832723,21832769", results.get(2).toString());
//		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, name, seq.length(), pcpm);
//		assertEquals(1, blatRecs.size());
//		
////		for (BLATRecord[] brs : blatRecs) {
////			for (BLATRecord br: brs) {
////				System.out.println("br from brs: " + br.toString());
////			}
////		}
//	}
	
	@Test
	public void realLifeNotASplit1() {
		/*
		 * chr2_219093573_false_+
		 */
		String seq = "TTATTAAAGAGGGTGTACGGGAGTTTCTTGGTAAATCCAGAATCAGGATACAATGTCTCTTTGCTATATGACCTTGAAAATCTTCCGG";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "chr2_219093573_false_+";
		
		countPosition.put(NumberUtils.getTileCount(35, 0), TARecordUtil.getLongList(468349696));
		countPosition.put(NumberUtils.getTileCount(33, 0), TARecordUtil.getLongList(47279468348373l));
		countPosition.put(NumberUtils.getTileCount(8, 0), TARecordUtil.getLongList(63771988073074l));
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		
		for (IntLongPairs ilp : potentialSplits) {
			System.out.println("potential ilp: " + ilp.toDetailedString());
		}
		
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.filter(sa -> sa.isPresent())
				.map(s -> s.get())
				.filter(br -> br.getScore() > passingScore)
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		assertEquals("87	88	0	0	0	0	0	1	4266	+	chr2_219093573_false_+	88	0	87	chr2	243199373	219099074	219103428	2	43,45	0,43	219099074,219103383", results.get(0).toString());
		
		/*
		 * from BLAT
		 * 88      0       0       0       0       0       1       4266    +       chr2_219093573_false_   88      0       88      chr2    243199373       219099074       219103428       2       46,42,  0,46,   219099074,219103386,
		 */
		
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, name, r.getSequence().length(), pcpm);
		assertEquals(1, blatRecs.size());
		assertEquals(2, blatRecs.get(0).length);
		assertEquals("45	45	0	0	0	0	0	0	0	+	chr2_219093573_false_+	88	43	88	chr2	243199373	219103383	219103428	1	45	43	219103383", blatRecs.get(0)[0].toString());
		assertEquals("47	47	0	0	0	0	0	0	0	+	chr2_219093573_false_+	88	0	47	chr2	243199373	219099074	219099121	1	47	0	219099074", blatRecs.get(0)[1].toString());
	}
	@Test
	public void realLifeNotASplit7() {
		/*
		 * chr4_65841131_false_-
		 */
		String seq = "CTGTAGAAACTTCTACAGAAACTGCTAAGTTTCTGTAGAAACTTCTACAGAAGCTGG";
		String rev = "CCAGCTTCTGTAGAAGTTTCTACAGAAACTTAGCAGTTTCTGTAGAAGTTTCTACAG";
		String name = "chr4_65841131_false_-";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		
		countPosition.put(NumberUtils.getTileCount(24, 0), TARecordUtil.getLongList(4611691516741840313l));
		countPosition.put(NumberUtils.getTileCount(12, 0), TARecordUtil.getLongList(4611722303067418037l));
		countPosition.put(NumberUtils.getTileCount(10, 0), TARecordUtil.getLongList(4611697014299979166l));
		countPosition.put(NumberUtils.getTileCount(6, 0), TARecordUtil.getLongList(4611697013843744131l, 4611700313523032794l, 4611701413056446682l, 4611711309159304981l, 4611723403618781526l));
		System.out.println("seq length: " + seq.length());
		for (Entry<Integer, TLongList> entry : countPosition.entrySet()) {
			for (long l : entry.getValue().toArray()) {
				System.out.println("tile count: " + Arrays.toString(NumberUtils.splitIntInto2(entry.getKey())) + ", long position: " + (NumberUtils.isBitSet(l, 62) ? "- " : "+ ") + pcpm.getChrPositionFromLongPosition(NumberUtils.getLongPositionValueFromPackedLong(l)).toIGVString() + " (" + NumberUtils.getLongPositionValueFromPackedLong(l) + "), start position in sequence: " + IntLongPairsUtil.getStartPositionInSequence( new IntLongPair(entry.getKey(), l), seq.length()));
			}
		}
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(1, splits.size());
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		for (IntLongPairs ilp : potentialSplits) {
			System.out.println("ilp in pot splits: " + ilp.toDetailedString());
		}
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.filter(sa -> sa.isPresent())
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		assertEquals(1, results.size());
		assertEquals("44	46	0	0	0	1	1	1	1	-	chr4_65841131_false_-	57	25	23	chr4	191154276	65841077	65841124	2	22,24	10,33	65841077,65841100", results.get(0).toString());
		assertEquals(0, results.stream().filter(br -> br.getScore() >= passingScore).count());
		
		/*
		 * 44	46	0	0	0	1	1	1	1	-	chr4_65841131_false_-	57	25	23	chr4	12345	65841077	65841124	2	22,24	25,0	65841077,65841100
		 */
		
		/*
		 * from BLAT
		 * 88      0       0       0       0       0       1       4266    +       chr2_219093573_false_   88      0       88      chr2    243199373       219099074       219103428       2       46,42,  0,46,   219099074,219103386,
		 */
		
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplitsNew(splits, name, r.getSequence().length(), pcpm);
		assertEquals(1, blatRecs.size());
		for (BLATRecord br : blatRecs.get(0)) {
			System.out.println("blat record: " + br.toString());
		}
		assertEquals(1, blatRecs.get(0).length);
		assertEquals("44	46	0	0	0	1	1	1	1	-	chr4_65841131_false_-	57	25	23	chr4	191154276	65841077	65841124	2	22,24	10,33	65841077,65841100", blatRecs.get(0)[0].toString());
//		assertEquals("24	24	0	0	0	0	0	0	0	-	chr4_65841131_false_-	57	0	24	chr4	12345	65841100	65841124	1	24	33	65841100", blatRecs.get(0)[0].toString());
//		assertEquals("36	36	0	0	0	0	0	0	0	-	chr4_65841131_false_-	57	16	52	chr4	12345	65841104	65841140	1	36	5	65841104", blatRecs.get(0)[1].toString());
	}
	
	@Test
	public void realLifeNotASplit4() {
		/*
		 * splitcon_chr5_121312856_chr7_73144655__true_1591670142587_431145+
		 */
		String seq = "TGGTATGGTGGCGAGTGCCTGTAGTCCCAGCTACTCAGGAGGCTGAGGTGGGAGGATGGCTGGAGTCCAGGAGATTGAGGCTGCAGTGAGCCATGATTACACCACTGCACTCCAGCCTGGGAAACAGAGTGAGACCCTGTCTTAAAAAAAAAAAAAAAAAAAA";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		
		countPosition.put(NumberUtils.getTileCount(77, 74), TARecordUtil.getLongList(1306800986));
		countPosition.put(NumberUtils.getTileCount(34, 74), TARecordUtil.getLongList(93459795162031l));
		countPosition.put(NumberUtils.getTileCount(19, 74), TARecordUtil.getLongList(75867252657105l));
		countPosition.put(NumberUtils.getTileCount(18, 74), TARecordUtil.getLongList(4611717905531571448l));
		countPosition.put(NumberUtils.getTileCount(17, 74), TARecordUtil.getLongList(4611699213901862229l));
		countPosition.put(NumberUtils.getTileCount(16, 74), TARecordUtil.getLongList(4611699213878244189l));
		countPosition.put(NumberUtils.getTileCount(15, 74), TARecordUtil.getLongList(5498859273329l));
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		/*
		 * not expecting any splits as the commonly occurring tile count is high
		 */
		assertEquals(0, splits.size());
	}
	
	@Test
	public void realLifeNotASplit2() {
		/*
		 * chr2_219093458_true_+
		 */
		String seq = "CCGCATCATCGAGGAGACGCTCGCGCTCAAGTTCGAGAACGCGGCCGCCGGAAACAAACCGGAAGCAGTAGAAGTAACATTTG";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		
		countPosition.put(NumberUtils.getTileCount(39, 0), TARecordUtil.getLongList(468332838));
		countPosition.put(NumberUtils.getTileCount(21, 0), TARecordUtil.getLongList(54976049730063l));
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(1, splits.size());
		
		Optional<BLATRecord> oBR = TARecordUtil.areSplitsCloseEnoughToBeSingleRecord(splits.get(splits.keys()[0]).iterator().next(), "chr2_219093458_true_+", r.getSequence().length(), pcpm, 13);
		assertEquals(true, oBR.isPresent());
		System.out.println("singleBLATRec blat record: " + oBR.get().toString());
		System.out.println("blat rec from blat: 83      0       0       0       0       0       1       8375    +       chr2_219093458  83      0       83      chr2    243199373       219082216       219090674       2       51,32,  0,51,   219082216,219090642,");
		
		
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, "chr2_219093458_true_+", r.getSequence().length(), pcpm);
		assertEquals(1, blatRecs.size());
//		for (BLATRecord br : blatRecs) {
//			System.out.println("blat record: " + br.toString());
//		}
		assertEquals(2, blatRecs.get(0).length);
		assertEquals("33	33	0	0	0	0	0	0	0	+	chr2_219093458_true_+	83	50	83	chr2	243199373	219090641	219090674	1	33	50	219090641", blatRecs.get(0)[0].toString());
		assertEquals("51	51	0	0	0	0	0	0	0	+	chr2_219093458_true_+	83	0	51	chr2	243199373	219082216	219082267	1	51	0	219082216", blatRecs.get(0)[1].toString());
	}
	
	@Test
	public void realLifeNotASplit6() {
		/*
		 * chr8_6780650_false_++
		 */
		String seq = "CCTCCCTCGGCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCNCCCCCCCCCCCCCCCCCNCCGCNCGACCCCCTGCCCCCCCGCCCGTGCGCAGCCGG";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		String name = "chr8_6780650_false_+";
		
		countPosition.put(NumberUtils.getTileCount(45, 0), TARecordUtil.getLongList(4611723402105124227l));
		countPosition.put(NumberUtils.getTileCount(43, 0), TARecordUtil.getLongList(4611725601128379778l, 4611725601128380012l));
		countPosition.put(NumberUtils.getTileCount(39, 0), TARecordUtil.getLongList(4611726700640007654l));
		countPosition.put(NumberUtils.getTileCount(37, 0), TARecordUtil.getLongList(4611725601128379880l));
		countPosition.put(NumberUtils.getTileCount(33, 0), TARecordUtil.getLongList(4611736596244657530l));
		countPosition.put(NumberUtils.getTileCount(30, 0), TARecordUtil.getLongList(4611725601128379927l));
		countPosition.put(NumberUtils.getTileCount(28, 0), TARecordUtil.getLongList(4611742093802796564l, 4611742093802796649l));
		countPosition.put(NumberUtils.getTileCount(26, 0), TARecordUtil.getLongList(4611725601128379824l));
		countPosition.put(NumberUtils.getTileCount(25, 0), TARecordUtil.getLongList(4611742093802796604l));
		countPosition.put(NumberUtils.getTileCount(22, 0), TARecordUtil.getLongList(4611725601128379967l));
		countPosition.put(NumberUtils.getTileCount(11, 0), TARecordUtil.getLongList(4611699212824942509l, 4611723402095585225l, 4611723402147823381l, 4611723402313647256l, 4611723402451070354l, 4611723403553864282l, 4611723404245212377l, 4611723404254063266l, 4611723404487427564l, 4611723404706013687l, 4611723404734974280l, 4611723404861234247l,4611724503877178351l, 4611725600864842386l, 4611725601348799315l, 4611725601828463389l, 4611725602072099791l, 4611725603878327014l, 4611726702826088963l, 4611729999434819072l, 4611731100968196686l, 4611733298405409725l, 4611734397637893091l, 4611734399692440489l, 4611735497499013331l, 4611739895805990442l, 4611743193506804922l, 4611743194718568863l, 4611744292940548370l));
		countPosition.put(NumberUtils.getTileCount(10, 0), TARecordUtil.getLongList(4611724501577997285l, 4611724501759438076l, 4611724502055551144l, 4611724502643525304l, 4611724502887527219l, 4611724502943305523l, 4611724503998276830l, 4611725601242876179l, 4611725601468027393l));
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(3, splits.size());
		
		List<IntLongPairs> potentialSplits = new ArrayList<>();
		splits.forEachValue(s -> potentialSplits.addAll(s.stream().collect(Collectors.toList())));
		System.out.println("Number of IntLongPairs in potentialSplits list: " + potentialSplits.size());
		
		for (IntLongPairs ilp : potentialSplits) {
			System.out.println("potential ilp: " + ilp.toDetailedString());
		}
		
		/*
		 * loop through them all, if valid single record splits, create BLAT recs, otherwise fall through to SW
		 */
		int passingScore = (int)(0.9 * seq.length());
		List<BLATRecord> results = new ArrayList<>();
		results.addAll(potentialSplits.stream()
				.filter(ilp -> IntLongPairsUtil.isIntLongPairsAValidSingleRecord(ilp))
				.map(ilp ->  BLATRecordUtil.blatRecordFromSplits(ilp, name, seq.length(), pcpm, 13))
				.filter(sa -> sa.isPresent())
				.map(s -> s.get())
				.collect(Collectors.toList()));
		
		System.out.println("Number of records in results after looking in potentialSplits list: " + results.size());
		
		for (Set<IntLongPairs> setsOfPairs: splits.valueCollection()) {
			for (IntLongPairs pairs : setsOfPairs) {
				Optional<BLATRecord> oBR = TARecordUtil.areSplitsCloseEnoughToBeSingleRecord(pairs, name, r.getSequence().length(), pcpm, 13);
				assertEquals(false, oBR.isPresent());
			}
		}
		
		List<BLATRecord[]> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, name, r.getSequence().length(), pcpm);
		assertEquals(1, blatRecs.size());
//		for (BLATRecord br : blatRecs) {
//			System.out.println("blat record: " + br.toString());
//		}
		assertEquals(2, blatRecs.get(0).length);
		assertEquals("23	23	0	0	0	0	0	0	0	-	chr8_6780650_false_+	101	66	89	chr2	243199373	8770671	8770694	1	23	12	8770671", blatRecs.get(0)[0].toString());
		assertEquals("57	57	0	0	0	0	0	0	0	-	chr8_6780650_false_+	101	10	67	chr2	243199373	33141317	33141374	1	57	34	33141317", blatRecs.get(0)[1].toString());
	}
	@Test
	public void realLifeNotASplit5() {
		/*
		 * chr2_219093573
		 */
		String seq = "TTATTAAAGAGGGTGTACGGGAGTTTCTTGGTAAATCCAGAATCAGGATACAATGTCTCTTTGCTATATGACCTTGAAAATCTTCCGG";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		
		countPosition.put(NumberUtils.getTileCount(35, 0), TARecordUtil.getLongList(468349696));
		countPosition.put(NumberUtils.getTileCount(33, 0), TARecordUtil.getLongList(47279468348373l));
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(1, splits.size());
		
		Optional<BLATRecord> oBR = TARecordUtil.areSplitsCloseEnoughToBeSingleRecord(splits.get(splits.keys()[0]).iterator().next(), "chr2_219093573", r.getSequence().length(), pcpm, 13);
		assertEquals(true, oBR.isPresent());
//		BLATRecord singleBLATRec = new BLATRecord(singleBLATRecArray);
		System.out.println("singleBLATRec blat record: " + oBR.get().toString());
		System.out.println("blat rec from blat: 88      0       0       0       0       0       1       4266    +       chr2_219093573_false_   88      0       88      chr2    243199373       219099074       219103428       2       46,42,  0,46,   219099074,219103386,");
		assertEquals("87	88	0	0	0	0	0	1	4266	+	chr2_219093573	88	0	87	chr2	243199373	219099074	219103428	2	43,45	0,43	219099074,219103383", oBR.get().toString());
		
	}
	
	@Test
	public void realLifeNotASplit3() {
		/*
		 * chr2_219093458_true_+
		 * 
		 * This should not return any results as the max tile count is less than 1/3 of the sequence length
		 * 
		 */
		String seq = "TTTAATNTNAAGTCCNGCCTCTCCTCCTTCCCNCCCTTCCCCCNATTCTCCCCTCTGGGGACCTTCCCCTCAGCTTTCGCCCGGCCGCCACTTCCCGG";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		
		countPosition.put(NumberUtils.getTileCount(11, 0), TARecordUtil.getLongList(18694208486406l, 18694208486423l, 18694208486440l, 4611746493793328204l));
		countPosition.put(NumberUtils.getTileCount(10, 0), TARecordUtil.getLongList(23089781686284l, 4611746492140022818l, 4611746492298352748l, 4611746492611383193l, 4611746492814325754l, 4611747592869916944l, 4611749792597365989l));
		countPosition.put(NumberUtils.getTileCount(9, 0), TARecordUtil.getLongList(15393217590899l, 19792925818198l,20893231742043l, 23090748275645l, 23091179545971l, 24189293314045l, 24189293314103l, 24189590597294l, 24189696501195l, 24190174935040l, 24190575961585l, 4611747592123010986l, 4611747592532306583l, 4611747592532306598l, 4611748692446867427l, 4611749791898315168l, 4611749792987228214l, 4611757488484130851l));
		countPosition.put(NumberUtils.getTileCount(8, 0), TARecordUtil.getLongList(14295682726983l, 14296429881159l, 16493914386631l, 18692069421207l, 18692860652876l, 19791826198623l, 21991747599100l, 21991747599116l, 21991747599132l, 21992530811149l, 21992580400103l, 21992605472203l, 21992732909656l, 21993283469916l, 23090321664111l, 23092173196802l,23092798082608l, 24189374784027l, 24190053753471l, 24190053753489l, 24191038884692l, 24191724000868l, 24191890892604l, 25290497431835l,4611724504280285384l, 4611746492282365670l, 4611746494205622742l, 4611747591536345565l,4611747593240711779l, 4611747593240711930l, 4611747593378322464l, 4611747593943584737l, 4611747594046215571l, 4611748693259627179l, 4611748693308946413l, 4611749790591948759l, 4611749792641660306l, 4611749792899102540l, 4611749793151325986l, 4611750891409942969l, 4611757489407404161l));
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(0, splits.size());
	}
	
	@Test
	public void getRangesAndChrPositions() {
		IntLongPair pair1 = new IntLongPair(8323072, 32985657853490l);
		IntLongPair pair2 = new IntLongPair(1179648, 309020251);
		IntLongPairs pairs = new IntLongPairs(pair1, pair2);
		PositionChrPositionMap pcpm = new PositionChrPositionMap();
		pcpm.loadMap(PositionChrPositionMap.grch37Positions);
		Map<ChrPosition, int[]> map = TARecordUtil.getChrPositionAndBlocksFromSplits(pairs, 169, pcpm);
		
		assertEquals(2, map.size());
//		assertArrayEquals(new int[] {1,30}, map.get(new ChrRangePosition("chr2", 12345, 123456)));
	}
	
	@Test
	public void getRangesAndChrPositions2() {
//		 IntLongPairs [pairs=[IntLongPair [i=524288, l=4611732199562315553], IntLongPair [i=1900544, l=4611686020073948919]]]
		IntLongPair p1 = new IntLongPair(524288, 4611732199562315553l);
		IntLongPair p2 = new IntLongPair(1900544, 4611686020073948919l);
		IntLongPairs pairs = new IntLongPairs(p1, p2);
		
		long long1 = NumberUtils.getLongPositionValueFromPackedLong(p1.getLong());
		long long2 = NumberUtils.getLongPositionValueFromPackedLong(p2.getLong());
		System.out.println("p1: tile count: " + NumberUtils.getPartOfPackedInt(p1.getInt(), true) + ", long position: " + long1 + ", start position in sequence(RS): " + NumberUtils.getShortFromLong(p1.getLong(), 40));
		System.out.println("p2: tile count: " + NumberUtils.getPartOfPackedInt(p2.getInt(), true) + ", long position: " + long2 + ", start position in sequence(RS): " + NumberUtils.getShortFromLong(p2.getLong(), 40));
		
		PositionChrPositionMap pcpm = new PositionChrPositionMap();
		pcpm.loadMap(PositionChrPositionMap.grch37Positions);
		Map<ChrPosition, int[]> map = TARecordUtil.getChrPositionAndBlocksFromSplits(pairs, 62, pcpm);
		
		assertEquals(2, map.size());
		assertArrayEquals(new int[]{0, 20}, map.get(new ChrPositionName("chr9", 107401344, 107401364, "R")));
		assertArrayEquals(new int[]{21, 41}, map.get(new ChrPositionName("chr9", 107401302, 107401343, "R")));
//		for(Entry<ChrPosition, int[]> entry : map.entrySet()) {
//			System.out.println("cp: " + entry.getKey() + ", int array: " + Arrays.toString(entry.getValue()));
//		}
	}
	
	@Test
	public void getRangesAndChrPositions3() {
//		 IntLongPairs [pairs=[IntLongPair [i=786432, l=4611722303067418037], IntLongPair [i=1572864, l=4611691516741840313]]]
		IntLongPair p1 = new IntLongPair(786432, 4611722303067418037l);
		IntLongPair p2 = new IntLongPair(1572864, 4611691516741840313l);
		IntLongPairs pairs = new IntLongPairs(p1, p2);
		
		long long1 = NumberUtils.getLongPositionValueFromPackedLong(p1.getLong());
		long long2 = NumberUtils.getLongPositionValueFromPackedLong(p2.getLong());
		System.out.println("p1: tile count: " + NumberUtils.getPartOfPackedInt(p1.getInt(), true) + ", long position: " + long1 + ", start position in sequence: " + NumberUtils.getShortFromLong(p1.getLong(), 40) + ", IntLongPairsUtil.getStartPositionInSequence: " + IntLongPairsUtil.getStartPositionInSequence(p1, 57));
		System.out.println("p2: tile count: " + NumberUtils.getPartOfPackedInt(p2.getInt(), true) + ", long position: " + long2 + ", start position in sequence: " + NumberUtils.getShortFromLong(p2.getLong(), 40) + ", IntLongPairsUtil.getStartPositionInSequence: " + IntLongPairsUtil.getStartPositionInSequence(p2, 57));
		
		PositionChrPositionMap pcpm = new PositionChrPositionMap();
		pcpm.loadMap(PositionChrPositionMap.grch37Positions);
		Map<ChrPosition, int[]> map = TARecordUtil.getChrPositionAndBlocksFromSplits(pairs, 57, pcpm);
		
		assertEquals(2, map.size());
		assertArrayEquals(new int[]{0, 24}, map.get(new ChrPositionName("chr4", 65841100, 65841124, "R")));
		assertArrayEquals(new int[]{24, 28}, map.get(new ChrPositionName("chr4", 65841112, 65841140, "R")));
		
//		for(Entry<ChrPosition, int[]> entry : map.entrySet()) {
//			System.out.println("cp: " + entry.getKey() + ", int array: " + Arrays.toString(entry.getValue()));
//		}
	}
	
	@Test
	public void splitsClose() {
		/*
		 * IntLongPair [i=8323072, l=32985657853490], IntLongPair [i=1179648, l=309020251], name: splitcon_chr2_59769659_chr2_59769589_2_true_1590115888837_35597_clip, seqLength: 169
		 * TODO this needs to be investigated
		 */
		IntLongPair pair1 = new IntLongPair(8323072, 32985657853490l);
		IntLongPair pair2 = new IntLongPair(1179648, 309020251);
		
		IntLongPairs pairs = new IntLongPairs(pair1, pair2);
		Optional<BLATRecord> oBR = TARecordUtil.areSplitsCloseEnoughToBeSingleRecord(pairs, "splitcon_chr2_59769659_chr2_59769589_2_true_1590115888837_35597_clip", 169, pcpm, 13);
		assertEquals(false, oBR.isPresent());
	}
	
	@Test
	public void splitsClose2() {
		/*
		 * CGTGTGCCAGGCACACCCAGCCACCTGCCCAGAGCCTCAGTCCCTCAAGGGGAGCCCCAGGCTGCTCCTCCTTCCTGTGGGGGATGGTGCCGTCTGCAAACAGACTTAGTCTACCTGCCGACTAAGATGGGGCAATCCTGAAAAGGTGAGGAGGGCAGGGCTGGAGGACATGGCCGGAAGTGGGGCCTGTCCCCTGCATATGCTGAGCATGGAGCTTCCCCATTGAGCTCAGATGCAGGCTCAAGAGTCATTCCCAGCACAGCTCTGCAGCCAGCCACAATCAATCAGTCAGCCCGTAGGGCCAATCC
		 */
		IntLongPair p1 = new IntLongPair(589824, 315561373177018l);
		IntLongPair p2 = new IntLongPair(17956864, 1536005032);
		IntLongPairs pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		Optional<BLATRecord> oBR = TARecordUtil.areSplitsCloseEnoughToBeSingleRecord(pairs, "splitcon_chr8_143209050_GL000220.1_128906__true_1592201891996_211329", 308, pcpm, 13);
		assertEquals(true, oBR.isPresent());
		System.out.println("singleBLATRec blat record: " + oBR.get().toString());
		assertEquals("293	295	0	0	0	1	13	1	0	+	splitcon_chr8_143209050_GL000220.1_128906__true_1592201891996_211329	308	0	307	chr8	146364022	143209341	143209636	2	274,21	0,287	143209341,143209615", oBR.get().toString());
		/*
		 * from blat:
		 * 295     0       0       0       1       13      0       0       +       splitcon_chr8_143209050_GL000220.1_128906__true_1592201891996_211329    308     0       308     chr8    146364022       143209341       143209636       2       154,141,
        0,167,  143209341,143209495,
		 */
	}
	
	@Test
	public void splitsClose3() {
		/*
		 * String seq = "CCTCCCTCGGCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCNCCCCCCCCCCCCCCCCCNCCGCNCGACCCCCTGCCCCCCCGCCCGTGCGCAGCCGG";
		 *  [pairs=[IntLongPair [i=2949120, l=4611723402105124227], IntLongPair [i=720896, l=4611699212824942509]]
		 */
		String seq = "CCTCCCTCGGCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCNCCCCCCCCCCCCCCCCCNCCGCNCGACCCCCTGCCCCCCCGCCCGTGCGCAGCCGG";
		IntLongPair p1 = new IntLongPair(2949120, 4611723402105124227l);
		IntLongPair p2 = new IntLongPair(720896, 4611699212824942509l);
		IntLongPairs pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		Optional<BLATRecord> oBR = TARecordUtil.areSplitsCloseEnoughToBeSingleRecord(pairs, "splitcon_chr8_143209050_GL000220.1_128906__true_1592201891996_211329", seq.length(), pcpm, 13);
		assertEquals(false, oBR.isPresent());
//		System.out.println("singleBLATRec blat record: " + br.toString());
//		assertEquals("305	307	0	0	0	1	1	1	-12	+	splitcon_chr8_143209050_GL000220.1_128906__true_1592201891996_211329	308	0	307	chr8	12345	143209341	143209636	2	286,21	0,287	143209341,143209615", br.toString());
		/*
		 * from blat:
		 * 295     0       0       0       1       13      0       0       +       splitcon_chr8_143209050_GL000220.1_128906__true_1592201891996_211329    308     0       308     chr8    146364022       143209341       143209636       2       154,141,
        0,167,  143209341,143209495,
		 */
	}
	
	@Test
	public void getSplits2() {
		String seq = "AAAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDDEEEEEEEEEE";
		Map<Integer, TLongList> map = new HashMap<>();
		TLongList list = new TLongArrayList();
		long position = 10000000;
		short tilePositionInSequence = 0;
		boolean forwardStrand = true;
		list.add(getLong(position, tilePositionInSequence, forwardStrand));
		int key = NumberUtils.pack2IntsInto1(15, 0);
		map.put(key, list);
		TARecord rec = new TARecord(seq, map);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(rec);
		assertEquals(true, splits.isEmpty());
		/*
		 * add another but there will not be enough tiles in the split for it to pass the min tile count check
		 */
		TLongList list2 = new TLongArrayList();
		position = 20000000;
		tilePositionInSequence = 30;
		list2.add(getLong(position, tilePositionInSequence, forwardStrand));
		key = NumberUtils.pack2IntsInto1(6, 0);
		map.put(key, list2);
		rec = new TARecord(seq, map);
		splits = TARecordUtil.getSplitStartPositions(rec);
		assertEquals(true, splits.isEmpty());
	}
	
	@Test
	public void getSplitsTilesOverlap() {
		String seq = "AAAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDDEEEEEEEEEE";
		Map<Integer, TLongList> map = new HashMap<>();
		TLongList list = new TLongArrayList();
		long position = 10000000;
		short tilePositionInSequence = 10;
		boolean forwardStrand = true;
		list.add(getLong(position, tilePositionInSequence, forwardStrand));
		int key = NumberUtils.pack2IntsInto1(15, 0);
		map.put(key, list);
		TARecord rec = new TARecord(seq, map);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(rec);
		assertEquals(true, splits.isEmpty());
		/*
		 * add another, overlap is more than the current buffer (10) and so no splits
		 */
		TLongList list2 = new TLongArrayList();
		position = 20000000;
		tilePositionInSequence = 15;
		list2.add(getLong(position, tilePositionInSequence, forwardStrand));
		key = NumberUtils.pack2IntsInto1(12, 0);
		map.put(key, list2);
		rec = new TARecord(seq, map);
		splits = TARecordUtil.getSplitStartPositions(rec);
		assertEquals(true, splits.isEmpty());
		
		/*
		 * try adding again, this time within buffer range
		 */
		list2.clear();
		tilePositionInSequence = 20;
		list2.add(getLong(position, tilePositionInSequence, forwardStrand));
		key = NumberUtils.pack2IntsInto1(12, 0);
		map.put(key, list2);
		rec = new TARecord(seq, map);
		splits = TARecordUtil.getSplitStartPositions(rec);
		assertEquals(true, splits.isEmpty());
	}
	
	@Test
	public void fitWithinRange() {
		assertEquals(true, TARecordUtil.doesPositionFitWithinRange(new int[]{0,100}, 10, 10, 10));
		assertEquals(false, TARecordUtil.doesPositionFitWithinRange(new int[]{0,10}, 10, 10, 10));
		assertEquals(false, TARecordUtil.doesPositionFitWithinRange(new int[]{0,19}, 10, 15, 10));
		assertEquals(false, TARecordUtil.doesPositionFitWithinRange(new int[]{0,19}, 10, 7, 10));
		/*
		 * need at least 13 bases overlap to be considered
		 */
		assertEquals(false, TARecordUtil.doesPositionFitWithinRange(new int[]{0,19}, 10, 6, 10));
		assertEquals(true, TARecordUtil.doesPositionFitWithinRange(new int[]{0,19}, 5, 6, 10));
	}
	
	@Test
	public void getSplitsEqualTileCounts() {
		String seq = "AAAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDDEEEEEEEEEEFFFFFFFFFF";
		Map<Integer, TLongList> map = new HashMap<>();
		TLongList list = new TLongArrayList();
		long position = 10000000;
		short tilePositionInSequence = 0;
		boolean forwardStrand = true;
		list.add(getLong(position, tilePositionInSequence, forwardStrand));
		int key = NumberUtils.pack2IntsInto1(15, 0);
		map.put(key, list);
		TARecord rec = new TARecord(seq, map);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(rec);
		assertEquals(true, splits.isEmpty());
		/*
		 * add another but there will not be enough tiles in the split for it to pass the min tile count check
		 */
		TLongList list2 = new TLongArrayList();
		position = 20000000;
		tilePositionInSequence = 30;
		list2.add(getLong(position, tilePositionInSequence, forwardStrand));
		list.addAll(list2);
//		key = NumberUtils.pack2IntsInto1(15, 0);
//		map.put(key, list2);
		rec = new TARecord(seq, map);
		splits = TARecordUtil.getSplitStartPositions(rec);
		assertEquals(false, splits.isEmpty());
	}
	
	@Test
	public void getRangesEnd() {
		int length = 100;
		int startPosition = 0;
		int tileLength = 13;
		int tileCount = 10;
		int minSize = 20;
		List<int[]> ranges = TARecordUtil.getPossibleTileRanges(length, startPosition, tileLength, tileCount, minSize);
		assertEquals(1, ranges.size());
		assertArrayEquals(new int[]{23, 100}, ranges.get(0));
		
		/*
		 * up the tile count so that there is no space for a range
		 */
		tileCount = 66;
		ranges = TARecordUtil.getPossibleTileRanges(length, startPosition, tileLength, tileCount, minSize);
		assertEquals(1, ranges.size());
		assertArrayEquals(new int[]{79, 100}, ranges.get(0));
		tileCount = 67;
		ranges = TARecordUtil.getPossibleTileRanges(length, startPosition, tileLength, tileCount, minSize);
		assertEquals(0, ranges.size());
		tileCount = 68;
		ranges = TARecordUtil.getPossibleTileRanges(length, startPosition, tileLength, tileCount, minSize);
		assertEquals(0, ranges.size());
	}
	
	@Test
	public void getRangesStart() {
		int length = 100;
		int startPosition = 30;
		int tileLength = 13;
		int tileCount = 50;
		int minSize = 20;
		List<int[]> ranges = TARecordUtil.getPossibleTileRanges(length, startPosition, tileLength, tileCount, minSize);
		assertEquals(1, ranges.size());
		assertArrayEquals(new int[]{0, 29}, ranges.get(0));
		
		/*
		 * change start position so that there is no space for a range
		 */
		startPosition = 18;
		ranges = TARecordUtil.getPossibleTileRanges(length, startPosition, tileLength, tileCount, minSize);
		assertEquals(0, ranges.size());
		startPosition = 19;
		ranges = TARecordUtil.getPossibleTileRanges(length, startPosition, tileLength, tileCount, minSize);
		assertEquals(0, ranges.size());
		startPosition = 20;
		ranges = TARecordUtil.getPossibleTileRanges(length, startPosition, tileLength, tileCount, minSize);
		assertEquals(1, ranges.size());
		assertArrayEquals(new int[]{0, 19}, ranges.get(0));
	}
	
	@Test
	public void getFSStartAndStop() {
		assertArrayEquals(new int[]{9,69}, TARecordUtil.getForwardStrandStartAndStop(119, 48, 13, 188));
		assertArrayEquals(new int[]{60,105 + 60 + 12}, TARecordUtil.getForwardStrandStartAndStop(60, 105, 13, 188, true));
		assertArrayEquals(new int[]{0,57 - 33}, TARecordUtil.getForwardStrandStartAndStop(33, 12, 13, 57, false));
	}
	
	@Test
	public void getRangesStartAndEnd() {
		int length = 100;
		int startPosition = 30;
		int tileLength = 13;
		int tileCount = 20;
		int minSize = 20;
		List<int[]> ranges = TARecordUtil.getPossibleTileRanges(length, startPosition, tileLength, tileCount, minSize);
		assertEquals(2, ranges.size());
		assertArrayEquals(new int[]{0, 29}, ranges.get(0));
		assertArrayEquals(new int[]{63, 100}, ranges.get(1));
		
		/*
		 * change start position so that there is no space for a range
		 */
		startPosition = 18;
		ranges = TARecordUtil.getPossibleTileRanges(length, startPosition, tileLength, tileCount, minSize);
		assertEquals(1, ranges.size());
		assertArrayEquals(new int[]{51, 100}, ranges.get(0));
		
		startPosition = 30;
		tileCount = 30;
		ranges = TARecordUtil.getPossibleTileRanges(length, startPosition, tileLength, tileCount, minSize);
		assertEquals(2, ranges.size());
		assertArrayEquals(new int[]{0, 29}, ranges.get(0));
		assertArrayEquals(new int[]{73, 100}, ranges.get(1));
	}
	
	@Test
	public void getBestPair() {
		/*
		 * 
		 * tileCountAndCommon	1048576	
		 * l	4611712409752715004	
		 * 
		 * [IntLongPair [i=851968, l=27489398850136], IntLongPair [i=851968, l=27489398851711], IntLongPair [i=851968, l=27490009933828], IntLongPair [i=851968, l=27490636997025], IntLongPair [i=851968, l=27490888461371], IntLongPair [i=851968, l=27490888479382], IntLongPair [i=851968, l=27490891131388], IntLongPair [i=851968, l=4611688219669829321], IntLongPair [i=851968, l=4611688220296896290], IntLongPair [i=851968, l=4611688220496904249], IntLongPair [i=851968, l=4611688220550258880], IntLongPair [i=851968, l=4611688220550996199], IntLongPair [i=786432, l=28587684403510], IntLongPair [i=786432, l=4611688219554484768], IntLongPair [i=786432, l=4611688220246743779]]
		 * 
		 * looking at getILPSFromLists method here
		 * 
		 * All the entries in the list patch from a positional perspective, but there is one that is super close, and on the same strand as p1
		 * This is the one that should be chosen, when multiple options exist.
		 */
		IntLongPair p1 = new IntLongPair(1048576, 4611712409752715004l);
		List<IntLongPair> potentialMatches = Arrays.asList(new IntLongPair(851968, 27489398850136l),
				new IntLongPair(851968, 27489398851711l),
				new IntLongPair(851968, 27490009933828l),
				new IntLongPair(851968, 27490636997025l),
				new IntLongPair(851968, 27490888461371l),
				new IntLongPair(851968, 27490888479382l),
				new IntLongPair(851968, 27490891131388l),
				new IntLongPair(851968, 4611688219669829321l),
				new IntLongPair(851968, 4611688220296896290l),
				new IntLongPair(851968, 4611688220496904249l),
				new IntLongPair(851968, 4611688220550258880l),
				new IntLongPair(851968, 4611688220550996199l),
				new IntLongPair(786432, 28587684403510l),
				new IntLongPair(786432, 4611688219554484768l),
				new IntLongPair(786432, 4611688220246743779l));
		
		System.out.println("p1: 4611712409752715004: " + NumberUtils.getLongPositionValueFromPackedLong(4611712409752715004l));
		System.out.println("4611688220496904249: " + NumberUtils.getLongPositionValueFromPackedLong(4611688220496904249l));
		System.out.println("4611688220550258880: " + NumberUtils.getLongPositionValueFromPackedLong(4611688220550258880l));
		System.out.println("4611688220550996199: " + NumberUtils.getLongPositionValueFromPackedLong(4611688220550996199l));
		System.out.println("4611688220296896290: " + NumberUtils.getLongPositionValueFromPackedLong(4611688220296896290l));
		
		Optional<IntLongPair> bestILP = TARecordUtil.getBestILPFromList(potentialMatches, p1);
		assertEquals(true, bestILP.isPresent());
		assertEquals(new IntLongPair(851968, 4611688220496904249l), bestILP.get());
		
		
		IntLongPairs ilp = TARecordUtil.getILPSFromLists(potentialMatches, null, p1);
		assertEquals(new IntLongPair(851968, 4611688220496904249l), ilp.getPairs()[0]);
		
		assertEquals(p1, ilp.getPairs()[1]);
		
	}
	
	@Test
	public void getBasePairs2() {
		/*
		 * main ilp: LongPair p6 = new IntLongPair(NumberUtils.getTileCount(13,0), 4611688220496904249l);
		 * [IntLongPair [i=1048576, l=4611712409752715004], IntLongPair [i=1048576, l=4611712409807521324], IntLongPair [i=917504, l=4611714608830778186], IntLongPair [i=786432, l=4611716807799226451]]
		 */
		IntLongPair p1 =  new IntLongPair(NumberUtils.getTileCount(13,0), 4611688220496904249l);
		List<IntLongPair> potentialMatches = Arrays.asList(new IntLongPair(1048576, 4611712409752715004l),
				new IntLongPair(1048576, 4611712409807521324l),
				new IntLongPair(917504, 4611714608830778186l),
				new IntLongPair(786432, 4611716807799226451l));
		Optional<IntLongPair> bestILP = TARecordUtil.getBestILPFromList(potentialMatches, p1);
		assertEquals(true, bestILP.isPresent());
		assertEquals(new IntLongPair(786432, 4611716807799226451l), bestILP.get());
		
	}
	
	@Test
	public void getBasePairs3() {
		/*
		 * main ilp: LongPair p6 = new IntLongPair(7012352, 75868643634102);
		 * [IntLongPair [i=3145728, l=4611816862119100198], IntLongPair [i=3211265, l=7698304795184], IntLongPair [i=3145729, l=4611816862105330075], IntLongPair [i=3080193, l=9897416559913]]
		 */
		IntLongPair p1 =  new IntLongPair(7012352, 75868643634102l);
		List<IntLongPair> potentialMatches = Arrays.asList(new IntLongPair(3145728, 4611816862119100198l),
				new IntLongPair(3211265, 7698304795184l),
				new IntLongPair(3145729, 4611816862105330075l),
				new IntLongPair(3080193, 9897416559913l));
		Optional<IntLongPair> bestILP = TARecordUtil.getBestILPFromList(potentialMatches, p1);
		assertEquals(true, bestILP.isPresent());
		assertEquals(new IntLongPair(3145728, 4611816862119100198l), bestILP.get());
		
	}
	
	@Test
	public void getRangesStartAndEnd2() {
		int length = 188;
		int startPosition = 69;
		int tileLength = 13;
		int tileCount = 107;
		int minSize = 20;
		List<int[]> ranges = TARecordUtil.getPossibleTileRanges(length, startPosition, tileLength, tileCount, minSize, false);
		assertEquals(1, ranges.size());
		assertArrayEquals(new int[]{0, 68}, ranges.get(0));
	}
	
	@Test
	public void getRangesStartAndEndReverse() {
		int length = 188;
		int startPosition = 119;
		int tileLength = 13;
		int tileCount = 48;
		int minSize = 20;
		List<int[]> ranges = TARecordUtil.getPossibleTileRanges(length, startPosition, tileLength, tileCount, minSize, true);
		assertEquals(1, ranges.size());
		assertArrayEquals(new int[]{69, 188}, ranges.get(0));
	}
	
	@Test
	public void getRanges() {
		int length = 398;
		int startPosition = 79;
		int tileLength = 13;
		int tileCount = 165;
		int minSize = 20;
		List<int[]> ranges = TARecordUtil.getPossibleTileRanges(length, startPosition, tileLength, tileCount, minSize, false);
		assertEquals(2, ranges.size());
		assertArrayEquals(new int[]{0, 78}, ranges.get(0));
		assertArrayEquals(new int[]{257, 398}, ranges.get(1));
	}
	
	@Test
	public void getRanges2() {
		int length = 100;
		int startPosition = 10;
		int tileLength = 13;
		int tileCount = 20;
		int minSize = 20;
		List<int[]> ranges = TARecordUtil.getPossibleTileRanges(length, startPosition, tileLength, tileCount, minSize, false);
		assertEquals(1, ranges.size());
		assertArrayEquals(new int[]{43, 100}, ranges.get(0));
	}
	
	@Test
	public void getRemainingRanges() {
		IntLongPair p1 = new IntLongPair(NumberUtils.pack2IntsInto1(10, 0), NumberUtils.addShortToLong(100000, (short) 0, 40));
		IntLongPair p2 = new IntLongPair(NumberUtils.pack2IntsInto1(10, 0), NumberUtils.addShortToLong(100000, (short) 50, 40));
		IntLongPairs pairs = new IntLongPairs(p1, p2);
		int[][] ranges = TARecordUtil.getRemainingRangeFromIntLongPairs(pairs, 100);
		assertEquals(2, ranges.length);
		assertArrayEquals(new int[]{22,49}, ranges[0]);
		assertArrayEquals(new int[]{72,100}, ranges[1]);
	}
	@Test
	public void getRemainingRanges2() {
		IntLongPair p1 = new IntLongPair(NumberUtils.pack2IntsInto1(10, 0), NumberUtils.addShortToLong(100000, (short) 9, 40));
		IntLongPair p2 = new IntLongPair(NumberUtils.pack2IntsInto1(10, 0), NumberUtils.addShortToLong(100000, (short) 32, 40));
		IntLongPairs pairs = new IntLongPairs(p1, p2);
		int[][] ranges = TARecordUtil.getRemainingRangeFromIntLongPairs(pairs, 100);
		assertEquals(1, ranges.length);
		assertArrayEquals(new int[]{54,100}, ranges[0]);
	}
	@Test
	public void getRemainingRanges5() {
		IntLongPair p1 = new IntLongPair(NumberUtils.pack2IntsInto1(10, 0), NumberUtils.addShortToLong(100000, (short) 10, 40));
		IntLongPair p2 = new IntLongPair(NumberUtils.pack2IntsInto1(10, 0), NumberUtils.addShortToLong(100000, (short) 32, 40));
		IntLongPairs pairs = new IntLongPairs(p1, p2);
		int[][] ranges = TARecordUtil.getRemainingRangeFromIntLongPairs(pairs, 100);
		assertEquals(1, ranges.length);
		assertArrayEquals(new int[]{54,100}, ranges[0]);
	}
	
	@Test
	public void getRemainingRanges6() {
//		IntLongPairs [pairs=[IntLongPair [i=3145728, l=41782618660320], IntLongPair [i=3604480, l=277078107006956], IntLongPair [i=9437184, l=105554293072971]]]
		IntLongPair p1 = new IntLongPair(3145728, 41782618660320l);
		IntLongPair p2 = new IntLongPair(3604480, 277078107006956l);
		IntLongPair p3 = new IntLongPair(9437184, 105554293072971l);
		IntLongPairs pairs = new IntLongPairs(new IntLongPair[]{p1, p2, p3});
		int[][] ranges = TARecordUtil.getRemainingRangeFromIntLongPairs(pairs, 319);
		assertEquals(1, ranges.length);
		assertArrayEquals(new int[]{0,37}, ranges[0]);
	}
	@Test
	public void getRemainingRanges7() {
//		IntLongPairs [pairs=[IntLongPair [i=3145728, l=4611816862119100198], IntLongPair [i=7012352, l=75868643634102]]]
		IntLongPair p1 = new IntLongPair(3145728, 4611816862119100198l);
		IntLongPair p2 = new IntLongPair(7012352, 75868643634102l);
		IntLongPairs pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		int[][] ranges = TARecordUtil.getRemainingRangeFromIntLongPairs(pairs, 188);
		assertEquals(0, ranges.length);
	}
	@Test
	public void getRemainingRanges8() {
//		IntLongPairs [pairs=[IntLongPair [i=589824, l=315561373177018], IntLongPair [i=17956864, l=1536005032]]]
		IntLongPair p1 = new IntLongPair(589824, 315561373177018l);
		IntLongPair p2 = new IntLongPair(17956864, 1536005032);
		IntLongPairs pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		long long1 = NumberUtils.getLongPositionValueFromPackedLong(p1.getLong());
		long long2 = NumberUtils.getLongPositionValueFromPackedLong(p2.getLong());
		long diff = long1 - long2;
		System.out.println("diff: " + diff);
		System.out.println("p1: tile count: " + NumberUtils.getPartOfPackedInt(p1.getInt(), true) + ", long position: " + NumberUtils.getLongPositionValueFromPackedLong(p1.getLong()) + ", start position in sequence: " + NumberUtils.getShortFromLong(p1.getLong(), 40));
		System.out.println("p2: tile count: " + NumberUtils.getPartOfPackedInt(p2.getInt(), true) + ", long position: " + NumberUtils.getLongPositionValueFromPackedLong(p2.getLong()) + ", start position in sequence: " + NumberUtils.getShortFromLong(p2.getLong(), 40));
		int[][] ranges = TARecordUtil.getRemainingRangeFromIntLongPairs(pairs, 308);
		assertEquals(0, ranges.length);
	}
	@Test
	public void getRemainingRanges9() {
//		[pairs=[IntLongPair [i=7012352, l=75868643634102], IntLongPair [i=3145728, l=4611816862119100198]]]
		IntLongPair p1 = new IntLongPair(7012352, 75868643634102l);
		IntLongPair p2 = new IntLongPair(3145728, 4611816862119100198l);
		IntLongPairs pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		long long1 = NumberUtils.getLongPositionValueFromPackedLong(p1.getLong());
		long long2 = NumberUtils.getLongPositionValueFromPackedLong(p2.getLong());
		long diff = long1 - long2;
		System.out.println("diff: " + diff);
		System.out.println("p1: tile count: " + NumberUtils.getPartOfPackedInt(p1.getInt(), true) + ", long position: " + long1 + ", start position in sequence: " + NumberUtils.getShortFromLong(p1.getLong(), 40));
		System.out.println("p2: tile count: " + NumberUtils.getPartOfPackedInt(p2.getInt(), true) + ", long position: " + long2 + ", start position in sequence: " + NumberUtils.getShortFromLong(p2.getLong(), 40));
		int[][] ranges = TARecordUtil.getRemainingRangeFromIntLongPairs(pairs, 188);
		assertEquals(0, ranges.length);
	}
	
	@Test
	public void getRemainingRanges10() {
//		IntLongPairs [pairs=[IntLongPair [i=2752512, l=4611705811171869861], IntLongPair [i=786432, l=4611775080404419812], IntLongPair [i=720896, l=87962594069495]]
		IntLongPair p1 = new IntLongPair(2752512, 4611705811171869861l);
		IntLongPair p2 = new IntLongPair(786432, 4611775080404419812l);
		IntLongPair p3 = new IntLongPair(720896, 87962594069495l);
		IntLongPairs pairs = new IntLongPairs(new IntLongPair[]{p1, p2, p3});
		long long1 = NumberUtils.getLongPositionValueFromPackedLong(p1.getLong());
		long long2 = NumberUtils.getLongPositionValueFromPackedLong(p2.getLong());
		long long3 = NumberUtils.getLongPositionValueFromPackedLong(p3.getLong());
		System.out.println("p1: tile count: " + NumberUtils.getPartOfPackedInt(p1.getInt(), true) + ", long position: " + long1 + ", start position in sequence: " + NumberUtils.getShortFromLong(p1.getLong(), 40));
		System.out.println("p2: tile count: " + NumberUtils.getPartOfPackedInt(p2.getInt(), true) + ", long position: " + long2 + ", start position in sequence: " + NumberUtils.getShortFromLong(p2.getLong(), 40));
		System.out.println("p3: tile count: " + NumberUtils.getPartOfPackedInt(p3.getInt(), true) + ", long position: " + long3 + ", start position in sequence: " + NumberUtils.getShortFromLong(p3.getLong(), 40));
		int[][] ranges = TARecordUtil.getRemainingRangeFromIntLongPairs(pairs, 105);
		assertEquals(0, ranges.length);
	}
	
	@Test
	public void getRemainingRanges11() {
//		 [pairs=[IntLongPair [i=720896, l=4611699212824942509], IntLongPair [i=2949120, l=4611723402105124227]]]
		IntLongPair p1 = new IntLongPair(720896, 4611699212824942509l);
		IntLongPair p2 = new IntLongPair(2949120, 4611723402105124227l);
		IntLongPairs pairs = new IntLongPairs(new IntLongPair[]{p1, p2});
		long long1 = NumberUtils.getLongPositionValueFromPackedLong(p1.getLong());
		long long2 = NumberUtils.getLongPositionValueFromPackedLong(p2.getLong());
		System.out.println("p1: tile count: " + NumberUtils.getPartOfPackedInt(p1.getInt(), true) + ", long position: " + long1 + ", start position in sequence: " + NumberUtils.getShortFromLong(p1.getLong(), 40));
		System.out.println("p2: tile count: " + NumberUtils.getPartOfPackedInt(p2.getInt(), true) + ", long position: " + long2 + ", start position in sequence: " + NumberUtils.getShortFromLong(p2.getLong(), 40));
		int[][] ranges = TARecordUtil.getRemainingRangeFromIntLongPairs(pairs, 101);
		assertEquals(0, ranges.length);
	}
	@Test
	public void getRemainingRanges3() {
		IntLongPair p1 = new IntLongPair(NumberUtils.pack2IntsInto1(10, 0), NumberUtils.addShortToLong(100000, (short) 9, 40));
		IntLongPair p2 = new IntLongPair(NumberUtils.pack2IntsInto1(60, 0), NumberUtils.addShortToLong(100000, (short) 32, 40));
		IntLongPairs pairs = new IntLongPairs(p1, p2);
		int[][] ranges = TARecordUtil.getRemainingRangeFromIntLongPairs(pairs, 100);
		assertEquals(0, ranges.length);
	}
	@Test
	public void getRemainingRanges4() {
		IntLongPair p1 = new IntLongPair(NumberUtils.pack2IntsInto1(10, 0), NumberUtils.addShortToLong(100000, (short) 18, 40));
		IntLongPair p2 = new IntLongPair(NumberUtils.pack2IntsInto1(10, 0), NumberUtils.addShortToLong(100000, (short) 60, 40));
		IntLongPairs pairs = new IntLongPairs(p1, p2);
		int[][] ranges = TARecordUtil.getRemainingRangeFromIntLongPairs(pairs, 100);
		assertEquals(3, ranges.length);
		assertArrayEquals(new int[]{0,17}, ranges[0]);
		assertArrayEquals(new int[]{40,59}, ranges[1]);
		assertArrayEquals(new int[]{82,100}, ranges[2]);
	}
	
	@Test
	public void longsOverlapping() {
		assertEquals(false, TARecordUtil.doGenomicPositionsOverlap(1, 1000, 1001, 2000, 10));
		assertEquals(false, TARecordUtil.doGenomicPositionsOverlap(1, 1000, 999, 2000, 10));
		assertEquals(false, TARecordUtil.doGenomicPositionsOverlap(1, 1000, 991, 2000, 10));
		assertEquals(true, TARecordUtil.doGenomicPositionsOverlap(1, 1000, 990, 2000, 10));
		assertEquals(true, TARecordUtil.doGenomicPositionsOverlap(1000, 2000, 1000, 2000, 10));
		assertEquals(true, TARecordUtil.doGenomicPositionsOverlap(1000, 2000, 1100, 2000, 10));
		assertEquals(true, TARecordUtil.doGenomicPositionsOverlap(1000, 2000, 1100, 1900, 10));
		assertEquals(true, TARecordUtil.doGenomicPositionsOverlap(1000, 2000, 1100, 1900, 10));
		assertEquals(true, TARecordUtil.doGenomicPositionsOverlap(1000, 2000, 500, 2001, 10));
		assertEquals(false, TARecordUtil.doGenomicPositionsOverlap(100, 1000, 0, 109, 10));
		assertEquals(true, TARecordUtil.doGenomicPositionsOverlap(100, 1000, 0, 109, 9));
	}
	
	@Test
	public void longsOverlapping2() {
		assertEquals(true, TARecordUtil.doGenomicPositionsOverlap(1994, 2121, 2011, 2129, 109));
		assertEquals(false, TARecordUtil.doGenomicPositionsOverlap(1994, 2121, 2011, 2129, 149));
	}
	
	private static long getLong(long position, short sequenceTilePosition, boolean forwardStrand) {
		long l = NumberUtils.addShortToLong(position, sequenceTilePosition, TARecordUtil.TILE_OFFSET);
		if ( ! forwardStrand) {
			l = NumberUtils.setBit(l, TARecordUtil.REVERSE_COMPLEMENT_BIT);
		}
		return l;
	}
}
