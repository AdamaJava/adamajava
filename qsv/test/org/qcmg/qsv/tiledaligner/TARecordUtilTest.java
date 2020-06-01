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
		List<BLATRecord> blatRecs = TARecordUtil.blatRecordsFromSplits(splits, "splitcon_chr22_41657586_chr22_41660668_1_true_1590714106685_44497", r.getSequence().length(), pcpm);
		for (BLATRecord br : blatRecs) {
			System.out.println("blat record: " + br.toString());
		}
		assertEquals(2, blatRecs.size());
		assertEquals("30	30	0	0	0	0	0	0	0	+	splitcon_chr22_41657586_chr22_41660668_1_true_1590714106685_44497	169	0	30	chr2	12345	59769629	59769659	1	30	0	59769629", blatRecs.get(0).toString());
		assertEquals("139	139	0	0	0	0	0	0	0	+	splitcon_chr22_41657586_chr22_41660668_1_true_1590714106685_44497	169	30	169	chr2	12345	59769588	59769727	1	139	30	59769588", blatRecs.get(1).toString());
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
//		for (BLATRecord br : blatRecs) {
//			System.out.println("blat record: " + br.toString());
//		}
		assertEquals(2, blatRecs.size());
		assertEquals("45	45	0	0	0	0	0	0	0	+	chr2_219093573_false_+	88	43	88	chr2	12345	219103383	219103428	1	45	43	219103383", blatRecs.get(0).toString());
		assertEquals("47	47	0	0	0	0	0	0	0	+	chr2_219093573_false_+	88	0	47	chr2	12345	219099074	219099121	1	47	0	219099074", blatRecs.get(1).toString());
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
//		for (BLATRecord br : blatRecs) {
//			System.out.println("blat record: " + br.toString());
//		}
		assertEquals(2, blatRecs.size());
		assertEquals("33	33	0	0	0	0	0	0	0	+	chr2_219093458_true_+	83	50	83	chr2	12345	219090641	219090674	1	33	50	219090641", blatRecs.get(0).toString());
		assertEquals("51	51	0	0	0	0	0	0	0	+	chr2_219093458_true_+	83	0	51	chr2	12345	219082216	219082267	1	51	0	219082216", blatRecs.get(1).toString());
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
