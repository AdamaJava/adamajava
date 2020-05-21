package org.qcmg.qsv.tiledaligner;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.common.util.NumberUtils;
import org.qcmg.qsv.blat.BLATRecord;

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
		assertEquals(15 + 14, splits.keys()[0]);
	}
	
	@Test
	public void realLifeSplits() {
		/*
		 * splitcon_chr7_100867120_chr7_100867215
		 */
		String seq = "TGGAAAGAAAAAGAAGAAAACCCGAGGTGATCATTTTAAACTTCGCTTCCGAAAAAAACTTTCAGGCCCTGTTGGAGGAGCAGAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGACCCACCAGGAGACCAGGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGAGAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCATCACCCAATGCAGGGGGAGCTCTTCCTGGACCAAGGGAGGAGCCGCTCATTCACCCG";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		
		/*
		 *match count: [165, 2], number of starts: 1
p: 86862753118281
match count: [135, 2], number of starts: 1
p: 274879241468242
match count: [61, 2], number of starts: 1
p: 1100846151560
match count: [8, 2], number of starts: 1
p: 69270567073733
match count: [7, 2], number of starts: 4
p: 3299873399669
p: 4611801468519265617
p: 4611849846975127200
p: 4611925713246426543
match count: [6, 2], number of starts: 2
p: 3299862685429
p: 3299927520776
		 */
		
		countPosition.put(NumberUtils.getTileCount(165, 2), TARecordUtil.getLongList(86862753118281l));
		countPosition.put(NumberUtils.getTileCount(135, 2), TARecordUtil.getLongList(274879241468242l));
		countPosition.put(NumberUtils.getTileCount(61, 2), TARecordUtil.getLongList(1100846151560l));
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(2, splits.size());
		List<BLATRecord> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, "splitcon_chr10_127633807_chr15_34031839__true_1586132281792_839799", r.getSequence().length(), pcpm);
//		for (BLATRecord br : blatRecs) {
//			System.out.println("blat record: " + br.toString());
//		}
		assertEquals(2, blatRecs.size());
		assertEquals("73	73	0	0	0	0	0	0	0	+	splitcon_chr10_127633807_chr15_34031839__true_1586132281792_839799	398	1	74	chr7	12345	100866756	100866829	1	73	1	100866756", blatRecs.get(0).toString());
		assertEquals("177	177	0	0	0	0	0	0	0	+	splitcon_chr10_127633807_chr15_34031839__true_1586132281792_839799	398	79	256	chr7	12345	100866949	100867126	1	177	79	100866949", blatRecs.get(1).toString());
	}
	
	@Test
	public void realLifeSplits2() {
		/*
		 * splitcon_chr8_125551528_chr8_125555328__true_1589928259240_726892
		 */
		String seq = "CGCGGCCGGGGAAGGTCAGCGCCGTAATGGCGTTCTTGGCGTCGGGACCCTACCTGACCCATCAGCAAAAGGTGTTGCGGCTTTATAAGCGGGCGCTACGCCACCTCGAGTCGTGGTGCGTCCAGAGAGACAAATACCGATACTTTGCTTGTTTGATGAGAGCCCGGTTTGAAGAACATAAGAATGAAAAGGATATGGCGAAGGCCACCCAGCTGCTGAAGGAGGCCGAGGAAGAATTCTGGTACCGTCAGCATCCAC";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		
		/*
		 *match count: [121, 0], number of starts: 1
p: 137440471823016
match count: [115, 0], number of starts: 1
p: 1518347092
match count: [23, 0], number of starts: 1
p: 4611703612118049933
match count: [20, 0], number of starts: 1
p: 4611728900885488804
match count: [17, 0], number of starts: 1
p: 4611754189652927675
match count: [12, 0], number of starts: 1
p: 4611789374025016524
match count: [7, 0], number of starts: 1
p: 4611896026652910893
		 */
		
		countPosition.put(NumberUtils.getTileCount(121, 0), TARecordUtil.getLongList(137440471823016l));
		countPosition.put(NumberUtils.getTileCount(115, 0), TARecordUtil.getLongList(1518347092));
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(1, splits.size());
		List<BLATRecord> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, "splitcon_chr8_125551528_chr8_125555328__true_1589928259240_726892", r.getSequence().length(), pcpm);
//		for (BLATRecord br : blatRecs) {
//			System.out.println("blat record: " + br.toString());
//		}
		assertEquals(2, blatRecs.size());
		assertEquals("127	127	0	0	0	0	0	0	0	+	splitcon_chr8_125551528_chr8_125555328__true_1589928259240_726892	258	0	127	chr8	12345	125551401	125551528	1	127	0	125551401", blatRecs.get(0).toString());
		assertEquals("133	133	0	0	0	0	0	0	0	+	splitcon_chr8_125551528_chr8_125555328__true_1589928259240_726892	258	125	258	chr8	12345	125555325	125555458	1	133	125	125555325", blatRecs.get(1).toString());
	}
	
	@Test
	public void realLifeSplits3() {
		/*
		 * splitcon_chr10_127633807_chr15_34031839
		 */
		String seq = "TTCGGCGTTGCTCACACTGGGAGCTGTAGACCGGAGCTGTTCCTATTCGGCCATCTTGGCTCCTCCCCCTATAGTGTTATTTCATTTTCCAAGGATACCTGCATTTCCACCAGAAAATATTTAAGGGGTTACACATTTCCCGTTTTGGTTAACCTGGATAAATGCGCGTATTTTATTTCTGTTTTCAG";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		
		/*
		 * match count: [107, 0], number of starts: 1
p: 75868643634102
match count: [48, 0], number of starts: 1
p: 4611816862119100198
match count: [44, 0], number of starts: 1
p: 4611821260747053084
match count: [43, 0], number of starts: 2
p: 17593867691002
p: 4611814663054250561
match count: [42, 0], number of starts: 3
p: 17594573114243
p: 4611815762591203819
p: 4611815762608203655
match count: [41, 0], number of starts: 12
p: 17593872835136
p: 17593884743111
p: 17593973560642
p: 17593999726488
p: 17594546763755
p: 17594567136894
p: 4611816862002159212
p: 4611816862074935759
p: 4611816862656098325
p: 4611816862669844665
p: 4611816862676796898
p: 4611816862705450293
		 */
		
		countPosition.put(7012352, TARecordUtil.getLongList(75868643634102l));
		countPosition.put(3145728, TARecordUtil.getLongList(4611816862119100198l));	// 4611816862119100198l is the one we want!!!
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(1, splits.size());
		List<BLATRecord> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, "splitcon_chr10_127633807_chr15_34031839", r.getSequence().length(), pcpm);
//		for (BLATRecord br : blatRecs) {
//			System.out.println("blat record: " + br.toString());
//		}
		assertEquals(2, blatRecs.size());
		assertEquals("60	60	0	0	0	0	0	0	0	-	splitcon_chr10_127633807_chr15_34031839	188	9	69	chr10	12345	127633806	127633866	1	60	9	127633806", blatRecs.get(0).toString());
		assertEquals("119	119	0	0	0	0	0	0	0	+	splitcon_chr10_127633807_chr15_34031839	188	69	188	chr15	12345	34031838	34031957	1	119	69	34031838", blatRecs.get(1).toString());
	}
	
	@Test
	public void realLifeNotASplit1() {
		/*
		 * chr2_219093573_false_+
		 */
		String seq = "TTATTAAAGAGGGTGTACGGGAGTTTCTTGGTAAATCCAGAATCAGGATACAATGTCTCTTTGCTATATGACCTTGAAAATCTTCCGG";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		
		/*
		 *match count: [35, 0], number of starts: 1
p: 468349696
match count: [33, 0], number of starts: 1
p: 47279468348373
match count: [8, 0], number of starts: 1
p: 63771988073074
match count: [7, 0], number of starts: 1
p: 53877916035375
match count: [6, 0], number of starts: 7
p: 58274970605716
p: 4611692615989682352
p: 4611693715107916180
p: 4611697015203414897
p: 4611698114944208468
p: 4611711308928080428
p: 4611735497011484286
		 */
		
		countPosition.put(NumberUtils.getTileCount(35, 0), TARecordUtil.getLongList(468349696));
		countPosition.put(NumberUtils.getTileCount(33, 0), TARecordUtil.getLongList(47279468348373l));
		countPosition.put(NumberUtils.getTileCount(8, 0), TARecordUtil.getLongList(63771988073074l));
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(2, splits.size());
		List<BLATRecord> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, "chr2_219093573_false_+", r.getSequence().length(), pcpm);
		for (BLATRecord br : blatRecs) {
			System.out.println("blat record: " + br.toString());
		}
		assertEquals(2, blatRecs.size());
		assertEquals("45	45	0	0	0	0	0	0	0	+	chr2_219093573_false_+	88	43	88	chr2	12345	219103383	219103428	1	45	43	219103383", blatRecs.get(0).toString());
		assertEquals("47	47	0	0	0	0	0	0	0	+	chr2_219093573_false_+	88	0	47	chr2	12345	219099074	219099121	1	47	0	219099074", blatRecs.get(1).toString());
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
		 * add another but there will not be enough tiles in the split for it to pass the min tile count check
		 */
		TLongList list2 = new TLongArrayList();
		position = 20000000;
		tilePositionInSequence = 20;
		list2.add(getLong(position, tilePositionInSequence, forwardStrand));
		key = NumberUtils.pack2IntsInto1(12, 0);
		map.put(key, list2);
		rec = new TARecord(seq, map);
		splits = TARecordUtil.getSplitStartPositions(rec);
		assertEquals(true, splits.isEmpty());
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
		assertArrayEquals(new int[]{7,68}, TARecordUtil.getForwardStrandStartAndStop(119, 48, 13, 188));
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
	
	private static long getLong(long position, short sequenceTilePosition, boolean forwardStrand) {
		long l = NumberUtils.addShortToLong(position, sequenceTilePosition, TARecordUtil.TILE_OFFSET);
		if ( ! forwardStrand) {
			l = NumberUtils.setBit(l, TARecordUtil.REVERSE_COMPLEMENT_BIT);
		}
		return l;
	}

}
