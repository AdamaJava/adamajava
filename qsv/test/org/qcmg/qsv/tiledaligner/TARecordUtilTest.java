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
		assertEquals(NumberUtils.getTileCount(15 + 14, 0), splits.keys()[0]);
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
		assertEquals(1, splits.size());
		System.out.println("r.getSequence().length(): "  + r.getSequence().length());
		List<BLATRecord> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, "splitcon_chr7_100867120_chr7_100867215", r.getSequence().length(), pcpm);
		for (BLATRecord br : blatRecs) {
			System.out.println("blat record: " + br.toString());
		}
		assertEquals(3, blatRecs.size());
		assertEquals("73	73	0	0	0	0	0	0	0	+	splitcon_chr7_100867120_chr7_100867215	398	1	74	chr7	12345	100866756	100866829	1	73	1	100866756", blatRecs.get(0).toString());
		assertEquals("147	147	0	0	0	0	0	0	0	+	splitcon_chr7_100867120_chr7_100867215	398	250	397	chr7	12345	100867214	100867361	1	147	250	100867214", blatRecs.get(1).toString());
		assertEquals("177	177	0	0	0	0	0	0	0	+	splitcon_chr7_100867120_chr7_100867215	398	79	256	chr7	12345	100866949	100867126	1	177	79	100866949", blatRecs.get(2).toString());
		/*
		 * now get the single record version
		 */
		IntLongPairs pairs = splits.valueCollection().iterator().next().iterator().next();
		String[] singleBlatRecArray = TARecordUtil.blatRecordFromSplits(pairs, "splitcon_chr7_100867120_chr7_100867215", r.getSequence().length(), pcpm, TARecordUtil.TILE_LENGTH);
		BLATRecord singleBR = new BLATRecord(singleBlatRecArray);
		System.out.println("blat record: " + singleBR.toString());
		
		assertEquals("388	391	0	0	0	1	5	2	214	+	splitcon_chr7_100867120_chr7_100867215	398	1	396	chr7	12345	100866756	100867361	3	73,171,147	1,79,250	100866756,100866949,100867214", singleBR.toString());
		/*
		 * from blat
		 * 395     0       0       0       1       1       2       210     +       splitcon_chr7_100867120_chr7_100867215  398     1       397     chr7    159138663       100866756       100867361       4       56,25,170,144,  1,58,83,253,    100866756,100866812,100866953,100867217,
		 * from ta
		 * 388	391	0	0	0	1	5	2	214	+	splitcon_chr7_100867120_chr7_100867215	398	1	396	chr7	12345	100866756	100867361	3	73,171,147	1,79,250	100866756,100866949,100867214
		 */
	}
	
	@Test
	public void blatRecordLargerThanSequence() {
		/*
		 * IntLongPairs [pairs=[IntLongPair [i=6422528, l=155032316323820], IntLongPair [i=8454144, l=1176806490], null]]
		 */
		int seqLength = 251;
		IntLongPairs pairs = new IntLongPairs(new IntLongPair(6422528, 155032316323820l), new IntLongPair(8454144, 1176806490));
		String[] singleBlatRecArray = TARecordUtil.blatRecordFromSplits(pairs, "splitcon_chr6_114264670_chr6_114265444_1_true_1591590363423_284336_clip", seqLength, pcpm, TARecordUtil.TILE_LENGTH);
		BLATRecord singleBR = new BLATRecord(singleBlatRecArray);
		System.out.println("blat record: " + singleBR.toString());
		assertEquals(250, singleBR.getScore());
	}
	
	@Test
	public void trimRanges() {
		int[][] ranges = new int[2][];
		ranges[0] = new int[]{0,100};
		ranges[1] = new int[]{100,200};
		
		/*
		 * this method will modify the array in place
		 */
		TARecordUtil.trimRangesToRemoveOverlap(ranges);
		assertEquals(2, ranges.length);
		assertArrayEquals(new int[]{0,100}, ranges[0]);
		assertArrayEquals(new int[]{100,200}, ranges[1]);
		
		ranges[0] = new int[]{0,110};
		ranges[1] = new int[]{100,200};
		TARecordUtil.trimRangesToRemoveOverlap(ranges);
		assertEquals(2, ranges.length);
		assertArrayEquals(new int[]{0,110}, ranges[0]);
		assertArrayEquals(new int[]{110,190}, ranges[1]);
		
		ranges[0] = new int[]{0,110};
		ranges[1] = new int[]{200,100};
		TARecordUtil.trimRangesToRemoveOverlap(ranges);
		assertEquals(2, ranges.length);
		assertArrayEquals(new int[]{0,110}, ranges[0]);
		assertArrayEquals(new int[]{200,100}, ranges[1]);
	}
	@Test
	public void trimRangesAgain() {
		int[][] ranges = new int[3][];
		ranges[0] = new int[]{0,100};
		ranges[1] = new int[]{100,100};
		ranges[2] = new int[]{200,100};
		
		/*
		 * this method will modify the array in place
		 */
		TARecordUtil.trimRangesToRemoveOverlap(ranges);
		assertEquals(3, ranges.length);
		assertArrayEquals(new int[]{0,100}, ranges[0]);
		assertArrayEquals(new int[]{100,100}, ranges[1]);
		assertArrayEquals(new int[]{200,100}, ranges[2]);
		
		ranges[0] = new int[]{0,110};
		ranges[1] = new int[]{100,100};
		ranges[2] = new int[]{200,100};
		TARecordUtil.trimRangesToRemoveOverlap(ranges);
		assertEquals(3, ranges.length);
		assertArrayEquals(new int[]{0,100}, ranges[0]);
		assertArrayEquals(new int[]{100,100}, ranges[1]);
		assertArrayEquals(new int[]{200,100}, ranges[2]);
		
		ranges[0] = new int[]{0,110};
		ranges[1] = new int[]{100,100};
		ranges[2] = new int[]{150,200};
		TARecordUtil.trimRangesToRemoveOverlap(ranges);
		assertEquals(3, ranges.length);
		assertArrayEquals(new int[]{0,100}, ranges[0]);
		assertArrayEquals(new int[]{100,100}, ranges[1]);
		assertArrayEquals(new int[]{200,150}, ranges[2]);
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
		countPosition.put(NumberUtils.getTileCount(23, 0), TARecordUtil.getLongList(4611703612118049933l));
		countPosition.put(NumberUtils.getTileCount(20, 0), TARecordUtil.getLongList(4611728900885488804l));
		countPosition.put(NumberUtils.getTileCount(17, 0), TARecordUtil.getLongList(4611754189652927675l));
		countPosition.put(NumberUtils.getTileCount(12, 0), TARecordUtil.getLongList(4611789374025016524l));
		countPosition.put(NumberUtils.getTileCount(7, 0), TARecordUtil.getLongList(4611896026652910893l));
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(1, splits.size());
		
		Set<IntLongPairs> setOfPairs = splits.get(splits.keys()[0]);
		assertEquals(1, setOfPairs.size());
		IntLongPairs pairs = setOfPairs.iterator().next();
		/*
		 * ascertain whether a single record may be derived from the splits
		 */
		String [] singleBRArray = TARecordUtil.blatRecordFromSplits(pairs, "splitcon_chr8_125551528_chr8_125555328__true_1589928259240_726892", r.getSequence().length(), pcpm, 13);
		BLATRecord singleBR = new BLATRecord(singleBRArray);
		System.out.println("singleBR record: " + singleBR.toString());
		assertEquals("257	258	0	0	0	0	0	1	3797	+	splitcon_chr8_125551528_chr8_125555328__true_1589928259240_726892	258	0	257	chr8	12345	125551401	125555456	2	127,131	0,127	125551401,125555325", singleBR.toString());
		
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
		assertEquals(1, splits.size());
		List<BLATRecord> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, "made_up", r.getSequence().length(), pcpm);
//		for (BLATRecord br : blatRecs) {
//			System.out.println("blat record: " + br.toString());
//		}
		assertEquals(2, blatRecs.size());
		assertEquals("30	30	0	0	0	0	0	0	0	+	made_up	169	0	30	chr2	12345	59769629	59769659	1	30	0	59769629", blatRecs.get(0).toString());
		assertEquals("139	139	0	0	0	0	0	0	0	+	made_up	169	30	169	chr2	12345	59769588	59769727	1	139	30	59769588", blatRecs.get(1).toString());
	}
	
	@Test
	public void realLifeSplits5() {
		/*
		 *  splitcon_chr6_114262928_chr6_114264515__true_1591925368416_401364
		 *  GTCAAATTCAGGGGTTGCTGAGCTGTTCTGATTTGGTTCCTTTGGTATCTGTTTTTTCACCACTGTTGTCCTTGGATTTATCTTCTTCCTTAACGTCTGTTTTTTTGTCCTCTGTTTCTTTCTTATCTTCTTCAATTCTAGCTTTCTTTGCTCCTTTCTTATGATCAGCCACATTTCTTCGACCTCCTTCTCCTTCATCCTCAGAATCTGAGAATTCTTCATCACAAGCTATCCGCTTGTCTGATGCTCGAATAGAAATTCTCTTGTCTGGATCTTCTCCATCTTCATCTCCACTGTCTTCATGAACAGCATCTTCTGG
		 */
		String seq = "GTCAAATTCAGGGGTTGCTGAGCTGTTCTGATTTGGTTCCTTTGGTATCTGTTTTTTCACCACTGTTGTCCTTGGATTTATCTTCTTCCTTAACGTCTGTTTTTTTGTCCTCTGTTTCTTTCTTATCTTCTTCAATTCTAGCTTTCTTTGCTCCTTTCTTATGATCAGCCACATTTCTTCGACCTCCTTCTCCTTCATCCTCAGAATCTGAGAATTCTTCATCACAAGCTATCCGCTTGTCTGATGCTCGAATAGAAATTCTCTTGTCTGGATCTTCTCCATCTTCATCTCCACTGTCTTCATGAACAGCATCTTCTGG";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		
		
		countPosition.put(NumberUtils.getTileCount(144, 0), TARecordUtil.getLongList(105554293072971l));
		countPosition.put(NumberUtils.getTileCount(55, 0), TARecordUtil.getLongList(277078107006956l));	
		countPosition.put(NumberUtils.getTileCount(48, 0), TARecordUtil.getLongList(41782618660320l));	
		countPosition.put(NumberUtils.getTileCount(27, 0), TARecordUtil.getLongList(1176804175));	
		countPosition.put(NumberUtils.getTileCount(8, 0), TARecordUtil.getLongList(4611894926848722163l));	
		countPosition.put(NumberUtils.getTileCount(7, 0), TARecordUtil.getLongList(23090820065296l, 200112345847245l));	
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(1, splits.size());
		
		Set<IntLongPairs> setOfPairs = splits.get(splits.keys()[0]);
		assertEquals(1, setOfPairs.size());
		IntLongPairs pairs = setOfPairs.iterator().next();
		/*
		 * ascertain whether a single record may be derived from the splits
		 */
		String [] singleBRArray = TARecordUtil.blatRecordFromSplits(pairs, "splitcon_chr6_114262928_chr6_114264515__true_1591925368416_401364", r.getSequence().length(), pcpm, 13);
		BLATRecord singleBR = new BLATRecord(singleBRArray);
		System.out.println("singleBR record: " + singleBR.toString());
		assertEquals(true, singleBR.getScore() <= seq.length());
		assertEquals("316	319	0	0	0	0	0	3	2977	+	splitcon_chr6_114262928_chr6_114264515__true_1591925368416_401364	319	0	318	chr6	12345	114262214	114265510	4	39,59,154,67	0,39,98,252	114262214,114262871,114264514,114265443", singleBR.toString());
		
		List<BLATRecord> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, "splitcon_chr6_114262928_chr6_114264515__true_1591925368416_401364", r.getSequence().length(), pcpm);
		blatRecs.sort(null);
		for (BLATRecord br : blatRecs) {
			System.out.println("blat record: " + br.toString());
		}
		
		assertEquals(4, blatRecs.size());
		assertEquals("39	39	0	0	0	0	0	0	0	+	splitcon_chr6_114262928_chr6_114264515__true_1591925368416_401364	319	0	39	chr6	12345	114262214	114262253	1	39	0	114262214", blatRecs.get(0).toString());
		assertEquals("60	60	0	0	0	0	0	0	0	+	splitcon_chr6_114262928_chr6_114264515__true_1591925368416_401364	319	38	98	chr6	12345	114262871	114262931	1	60	38	114262871", blatRecs.get(1).toString());
		assertEquals("67	67	0	0	0	0	0	0	0	+	splitcon_chr6_114262928_chr6_114264515__true_1591925368416_401364	319	252	319	chr6	12345	114265443	114265510	1	67	252	114265443", blatRecs.get(2).toString());
		assertEquals("156	156	0	0	0	0	0	0	0	+	splitcon_chr6_114262928_chr6_114264515__true_1591925368416_401364	319	96	252	chr6	12345	114264514	114264670	1	156	96	114264514", blatRecs.get(3).toString());
	}
	
	@Test
	public void realLifeSplits6() {
		/*
		 *  splitcon_chr22_41657586_chr22_41660668_1_true_1590714106685_44497
		 *  TCCTCCAGGGTCCCGATGACCCTAAAAGCTTCTGCCAAGGCAGTGGCGCCATCATTCTCCAGACGGTTTCTGCCAGCCACAAAGACCTTCAGGGCCAGAGGCTTGCCTTGGGCACTGGATTTCCGGTGACATTCGGTCAGAGCTGCAGCCAGGATCTTGCCGCCGCCAATGCCCATGCCACAGTTGTTGAGCTTGAGTTCCTGCAGGGTGAAGCAGGCTGAGCTCTTGAGCAGGGCCTCGAAGCCTTGCACACCGTCGGGCCCGAATGCGTTGTCGCTTAAGTCCAGCTCCA
		 *  
		 *  match count: [127, 0], number of starts: 1
p: 19794080686136
match count: [125, 0], number of starts: 1
p: 170427173694668
match count: [9, 0], number of starts: 1
p: 2871382811
match count: [6, 0], number of starts: 1
p: 4611730001741593112
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
		String [] singleBRArray = TARecordUtil.blatRecordFromSplits(pairs, "splitcon_chr8_125551528_chr8_125555328__true_1589928259240_726892", r.getSequence().length(), pcpm, 13);
		BLATRecord singleBR = new BLATRecord(singleBRArray);
		System.out.println("singleBR record: " + singleBR.toString());
		assertEquals(true, singleBR.getScore() <= seq.length());
		assertEquals("290	292	0	0	0	0	0	2	6420	+	splitcon_chr8_125551528_chr8_125555328__true_1589928259240_726892	292	0	291	chr22	12345	41654090	41660802	3	21,136,135	0,21,157	41654090,41657447,41660667", singleBR.toString());
		
		List<BLATRecord> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, "splitcon_chr22_41657586_chr22_41660668_1_true_1590714106685_44497", r.getSequence().length(), pcpm);
		for (BLATRecord br : blatRecs) {
			System.out.println("blat record: " + br.toString());
		}
		assertEquals(3, blatRecs.size());
		assertEquals("21	21	0	0	0	0	0	0	0	+	splitcon_chr22_41657586_chr22_41660668_1_true_1590714106685_44497	292	0	21	chr22	12345	41654090	41654111	1	21	0	41654090", blatRecs.get(0).toString());
		assertEquals("139	139	0	0	0	0	0	0	0	+	splitcon_chr22_41657586_chr22_41660668_1_true_1590714106685_44497	292	18	157	chr22	12345	41657447	41657586	1	139	18	41657447", blatRecs.get(1).toString());
		assertEquals("137	137	0	0	0	0	0	0	0	+	splitcon_chr22_41657586_chr22_41660668_1_true_1590714106685_44497	292	155	292	chr22	12345	41660667	41660804	1	137	155	41660667", blatRecs.get(2).toString());
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
		
		
		countPosition.put(NumberUtils.getTileCount(93, 4), TARecordUtil.getLongList(2707328463l));
		countPosition.put(NumberUtils.getTileCount(34, 4), TARecordUtil.getLongList(118749963128572l));	
		countPosition.put(NumberUtils.getTileCount(8, 4), TARecordUtil.getLongList(41782676974449l, 4611791573772632679l, 4611793773061013150l));	
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
		String [] singleBRArray = TARecordUtil.blatRecordFromSplits(pairs, "splitcon_chr8_125551528_chr8_125555328__true_1589928259240_726892", r.getSequence().length(), pcpm, 13);
		BLATRecord singleBR = new BLATRecord(singleBRArray);
		System.out.println("singleBR record: " + singleBR.toString());
		assertEquals("149	151	0	0	0	1	3	1	196	+	splitcon_chr8_125551528_chr8_125555328__true_1589928259240_726892	154	0	153	chr19	12345	47884140	47884487	2	105,46	0,108	47884140,47884441", singleBR.toString());
		assertEquals(true, singleBR.getScore() <= seq.length());
		
		List<BLATRecord> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, "chr19_47884141_47884486", r.getSequence().length(), pcpm);
		blatRecs.sort(null);
		for (BLATRecord br : blatRecs) {
			System.out.println("blat record: " + br.toString());
		}
		assertEquals(2, blatRecs.size());
		assertEquals("46	46	0	0	0	0	0	0	0	+	chr19_47884141_47884486	154	108	154	chr19	12345	47884441	47884487	1	46	108	47884441", blatRecs.get(0).toString());
		assertEquals("105	105	0	0	0	0	0	0	0	+	chr19_47884141_47884486	154	0	105	chr19	12345	47884140	47884245	1	105	0	47884140", blatRecs.get(1).toString());
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
		assertEquals(1, splits.size());
		
		Set<IntLongPairs> setOfPairs = splits.get(splits.keys()[0]);
		assertEquals(1, setOfPairs.size());
		IntLongPairs pairs = setOfPairs.iterator().next();
		/*
		 * ascertain whether a single record may be derived from the splits
		 */
		String [] singleBRArray = TARecordUtil.blatRecordFromSplits(pairs, "chr2_219093573_false_", r.getSequence().length(), pcpm, 13);
		BLATRecord singleBR = new BLATRecord(singleBRArray);
		System.out.println("singleBR record: " + singleBR.toString());
		assertEquals("87	88	0	0	0	0	0	1	4266	+	chr2_219093573_false_	88	0	87	chr2	12345	219099074	219103428	2	43,45	0,43	219099074,219103383", singleBR.toString());
		
		/*
		 * from BLAT
		 * 88      0       0       0       0       0       1       4266    +       chr2_219093573_false_   88      0       88      chr2    243199373       219099074       219103428       2       46,42,  0,46,   219099074,219103386,
		 */
		
		List<BLATRecord> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, "chr2_219093573_false_+", r.getSequence().length(), pcpm);
		blatRecs.sort(null);
//		for (BLATRecord br : blatRecs) {
//			System.out.println("blat record: " + br.toString());
//		}
		assertEquals(2, blatRecs.size());
		assertEquals("45	45	0	0	0	0	0	0	0	+	chr2_219093573_false_+	88	43	88	chr2	12345	219103383	219103428	1	45	43	219103383", blatRecs.get(0).toString());
		assertEquals("47	47	0	0	0	0	0	0	0	+	chr2_219093573_false_+	88	0	47	chr2	12345	219099074	219099121	1	47	0	219099074", blatRecs.get(1).toString());
	}
	
	@Test
	public void realLifeNotASplit4() {
		/*
		 * splitcon_chr5_121312856_chr7_73144655__true_1591670142587_431145+
		 */
		String seq = "TGGTATGGTGGCGAGTGCCTGTAGTCCCAGCTACTCAGGAGGCTGAGGTGGGAGGATGGCTGGAGTCCAGGAGATTGAGGCTGCAGTGAGCCATGATTACACCACTGCACTCCAGCCTGGGAAACAGAGTGAGACCCTGTCTTAAAAAAAAAAAAAAAAAAAA";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		
		/*
		 *match count: [77, 74], number of starts: 1
p: 1306800986
match count: [34, 74], number of starts: 1
p: 93459795162031
match count: [19, 74], number of starts: 1
p: 75867252657105
match count: [18, 74], number of starts: 1
p: 4611717905531571448
match count: [17, 74], number of starts: 1
p: 4611699213901862229
match count: [16, 74], number of starts: 1
p: 4611699213878244189
match count: [15, 74], number of starts: 1
p: 5498859273329
		 */
		
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
		
		/*
		 *match count: [39, 0], number of starts: 1
p: 468332838
match count: [21, 0], number of starts: 1
p: 54976049730063
		 */
		
		countPosition.put(NumberUtils.getTileCount(39, 0), TARecordUtil.getLongList(468332838));
		countPosition.put(NumberUtils.getTileCount(21, 0), TARecordUtil.getLongList(54976049730063l));
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(1, splits.size());
		
		String [] singleBLATRecArray = TARecordUtil.areSplitsCloseEnoughToBeSingleRecord(splits.get(splits.keys()[0]).iterator().next(), "chr2_219093458_true_+", r.getSequence().length(), pcpm, 13);
		
//		String [] singleBLATRecArray = TARecordUtil.blatRecordFromSplits(splits.get(splits.keys()[0]).iterator().next(), "chr2_219093458_true_+", r.getSequence().length(), pcpm, 13);
		BLATRecord singleBLATRec = new BLATRecord(singleBLATRecArray);
		System.out.println("singleBLATRec blat record: " + singleBLATRec.toString());
		System.out.println("blat rec from blat: 83      0       0       0       0       0       1       8375    +       chr2_219093458  83      0       83      chr2    243199373       219082216       219090674       2       51,32,  0,51,   219082216,219090642,");
		
		
		List<BLATRecord> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, "chr2_219093458_true_+", r.getSequence().length(), pcpm);
		blatRecs.sort(null);
//		for (BLATRecord br : blatRecs) {
//			System.out.println("blat record: " + br.toString());
//		}
		assertEquals(2, blatRecs.size());
		assertEquals("33	33	0	0	0	0	0	0	0	+	chr2_219093458_true_+	83	50	83	chr2	12345	219090641	219090674	1	33	50	219090641", blatRecs.get(0).toString());
		assertEquals("51	51	0	0	0	0	0	0	0	+	chr2_219093458_true_+	83	0	51	chr2	12345	219082216	219082267	1	51	0	219082216", blatRecs.get(1).toString());
	}
	@Test
	public void realLifeNotASplit5() {
		/*
		 * chr2_219093573
		 */
		String seq = "TTATTAAAGAGGGTGTACGGGAGTTTCTTGGTAAATCCAGAATCAGGATACAATGTCTCTTTGCTATATGACCTTGAAAATCTTCCGG";
		Map<Integer, TLongList> countPosition = new HashMap<>();
		
		/*
		 *match count: [39, 0], number of starts: 1
p: 468332838
match count: [21, 0], number of starts: 1
p: 54976049730063
		 */
		
		countPosition.put(NumberUtils.getTileCount(35, 0), TARecordUtil.getLongList(468349696));
		countPosition.put(NumberUtils.getTileCount(33, 0), TARecordUtil.getLongList(47279468348373l));
		
		TARecord r =  new TARecord(seq, countPosition);
		TIntObjectMap<Set<IntLongPairs>> splits = TARecordUtil.getSplitStartPositions(r);
		assertEquals(1, splits.size());
		
		String [] singleBLATRecArray = TARecordUtil.areSplitsCloseEnoughToBeSingleRecord(splits.get(splits.keys()[0]).iterator().next(), "chr2_219093573", r.getSequence().length(), pcpm, 13);
		
//		String [] singleBLATRecArray = TARecordUtil.blatRecordFromSplits(splits.get(splits.keys()[0]).iterator().next(), "chr2_219093458_true_+", r.getSequence().length(), pcpm, 13);
		BLATRecord singleBLATRec = new BLATRecord(singleBLATRecArray);
		System.out.println("singleBLATRec blat record: " + singleBLATRec.toString());
		System.out.println("blat rec from blat: 88      0       0       0       0       0       1       4266    +       chr2_219093573_false_   88      0       88      chr2    243199373       219099074       219103428       2       46,42,  0,46,   219099074,219103386,");
		assertEquals("87	88	0	0	0	0	0	1	4266	+	chr2_219093573	88	0	87	chr2	12345	219099074	219103428	2	43,45	0,43	219099074,219103383", singleBLATRec.toString());
		
//		List<BLATRecord> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, "chr2_219093458_true_+", r.getSequence().length(), pcpm);
////		for (BLATRecord br : blatRecs) {
////			System.out.println("blat record: " + br.toString());
////		}
//		assertEquals(2, blatRecs.size());
//		assertEquals("33	33	0	0	0	0	0	0	0	+	chr2_219093573	83	50	83	chr2	12345	219090641	219090674	1	33	50	219090641", blatRecs.get(0).toString());
//		assertEquals("51	51	0	0	0	0	0	0	0	+	chr2_219093573	83	0	51	chr2	12345	219082216	219082267	1	51	0	219082216", blatRecs.get(1).toString());
	}
	
	@Test
	public void realLifeNotASplit3() {
		/*
		 * chr2_219093458_true_+
		 * 
		 * This should not return any results as the max tile count is less than 1/3 of the sequence length
		 * 
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
	public void splitsClose() {
		/*
		 * IntLongPair [i=8323072, l=32985657853490], IntLongPair [i=1179648, l=309020251], name: splitcon_chr2_59769659_chr2_59769589_2_true_1590115888837_35597_clip, seqLength: 169
		 * TODO this needs to be investigated
		 */
		IntLongPair pair1 = new IntLongPair(8323072, 32985657853490l);
		IntLongPair pair2 = new IntLongPair(1179648, 309020251);
		
		IntLongPairs pairs = new IntLongPairs(pair1, pair2);
		String [] singleBLATRecArray = TARecordUtil.areSplitsCloseEnoughToBeSingleRecord(pairs, "splitcon_chr2_59769659_chr2_59769589_2_true_1590115888837_35597_clip", 169, pcpm, 13);
		
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
		assertEquals(true, TARecordUtil.doesPositionFitWithinRange(new int[]{0,19}, 10, 6, 10));
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
	
	private static long getLong(long position, short sequenceTilePosition, boolean forwardStrand) {
		long l = NumberUtils.addShortToLong(position, sequenceTilePosition, TARecordUtil.TILE_OFFSET);
		if ( ! forwardStrand) {
			l = NumberUtils.setBit(l, TARecordUtil.REVERSE_COMPLEMENT_BIT);
		}
		return l;
	}
}
