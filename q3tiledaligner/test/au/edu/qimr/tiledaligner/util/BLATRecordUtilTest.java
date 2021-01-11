package au.edu.qimr.tiledaligner.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.qcmg.common.model.BLATRecord;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionName;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.string.StringUtils;

public class BLATRecordUtilTest {

	

	@Test
	public void getBlatDetails() {
		String bufferedRef = "GTTCTGGAATCCTATGTGACTGACATACATGTAGACCCAGCAGTAGCGTTCTGGAATCCTATGGGAAGGAAAAACATTCAGACCCCAGCATCAGTGTTCTGCAATCCTATGGGAGGGACAATCATTCAGACCCTCGTAGCAGTGTGCAGGAATTCTGTGTGAGAATCAAACCTTCAGACCCTCATAGCAGTGTTCTGGAATCGTATGTGAGGAACAAACATTCAGACCCTCATAGGAGTGTTCTGGAATCCTGTGTGAGGTACAAT";
		String seq = "CTCAGAAAACAGCATTAGTGTTTTGCAATCCTATGGGAGGGACAACATTCACACCCTTGTAGCAGA";
		ChrPosition bufferedCP = new ChrRangePosition("GL000219.1", 165336, 165602);	//GL000219.1:165336-165602
		String [] swDiffs = new String[] {"TCAGACCCCAGCATCAGTGTTCTGCAATCCTATGGGAGGGACAATCATTCAGACCCTCGTAGCAG", 
				                          "|||||...||||||.||||||.|||||||||||||||||||||| ||||||.|||||.|||||||", 
				                          "TCAGAAAACAGCATTAGTGTTTTGCAATCCTATGGGAGGGACAA-CATTCACACCCTTGTAGCAG"};
		
		BLATRecord br = BLATRecordUtil.getBLATRecord(bufferedCP, swDiffs, "GL000219.1_165002_true_+", seq, true, bufferedRef);
		assertEquals(57, br.getMatchCount());
		assertEquals(7, br.getMisMatches());
		assertEquals(1, br.getQueryGapCount());
		assertEquals(0, br.getnCount());
		assertEquals(0, br.getRepMatch());
		assertEquals(1, br.getqBaseInsert());
		assertEquals(0, br.gettNumInsert());
		assertEquals(0, br.gettBaseInsert());
		assertEquals('+', br.getStrand());
		assertEquals("GL000219.1_165002_true_+", br.getQName());
		assertEquals("GL000219.1", br.getTName());
		assertEquals(179198, br.getChromsomeLength());
		assertEquals(66, br.getSize());
		assertEquals(64, br.getQueryLength());
		assertEquals(1, br.getqStart()); 
		assertEquals(65, br.getQueryEnd());
		assertEquals(165413, br.gettStart()); 
		assertEquals(165478, br.gettEnd());
		assertEquals(2, br.getBlockCount());
		assertArrayEquals(new int[] {44,20}, br.getBlockSizes());
		assertEquals("1,45", br.getqStarts());
		assertEquals("165413,165458", br.gettStarts());
	}
	
	@Test
	public void getBlatDetails2() {
		/*
		 * GCCTCCTGCAAGACCTGGAAGAAAGAAAAGTAACGATTCTTCTCGGCCAGAGAGAGAAGCATGACCGGTTTTGCATACCCTGTCCCCAGAAAAAGCACTCTAAGAGTAGGCGCGCTACTCCTGGGCGAGGAAACTGCGGAAGTGAGGCATTGGTGCCAGGTTCAAAGATGGCGCTGAGCAGCCAAGCGCAGAAGCGAAGAGAGGGCGGCAGCCTGCGGCCGTGGCCGGCCCGCGAGGTCTGGGCCTGGGAGCG
swDiffs: ||||..||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.||||||||.||||||||||||||.||||||||||||||.|.|||.|||
swDiffs: GCCTATTGCAAGACCTGGAAGAAAGAAAAGTAACGATTCTTCTCGGCCAGAGAGAGAAGCATGACCGGTTTTGCATACCCTGTCCCCAGAAAAAGCACTCTAAGAGTAGGCGCGCTACTCCTGGGCGAGGAAACTGCGGAAGTGAGGCATTGGTGCCAGGTTCAAAGATGGCGCTGAGCAGCCAAGCGCAGAAGCGAAGAGAGGTCGGCAGCCGGCGGCCGTGGCCGGGCCGCGAGGTCTGGGACGGGGGGCG
		 */
		String bufferedRef = "GCCTCCTGCAAGACCTGGAAGAAAGAAAAGTAACGATTCTTCTCGGCCAGAGAGAGAAGCATGACCGGTTTTGCATACCCTGTCCCCAGAAAAAGCACTCTAAGAGTAGGCGCGCTACTCCTGGGCGAGGAAACTGCGGAAGTGAGGCATTGGTGCCAGGTTCAAAGATGGCGCTGAGCAGCCAAGCGCAGAAGCGAAGAGAGGGCGGCAGCCTGCGGCCGTGGCCGGCCCGCGAGGTCTGGGCCTGGGAGCG";
		String seq = "GCCTATTGCAAGACCTGGAAGAAAGAAAAGTAACGATTCTTCTCGGCCAGAGAGAGAAGCATGACCGGTTTTGCATACCCTGTCCCCAGAAAAAGCACTCTAAGAGTAGGCGCGCTACTCCTGGGCGAGGAAACTGCGGAAGTGAGGCATTGGTGCCAGGTTCAAAGATGGCGCTGAGCAGCCAAGCGCAGAAGCGAAGAGAGGTCGGCAGCCGGCGGCCGTGGCCGGGCCGCGAGGTCTGGGACGGGGGGCG";
		ChrPosition bufferedCP = new ChrRangePosition("GL000219.1", 165336, 165602);	//GL000219.1:165336-165602
		String [] swDiffs = new String[] {"GCCTCCTGCAAGACCTGGAAGAAAGAAAAGTAACGATTCTTCTCGGCCAGAGAGAGAAGCATGACCGGTTTTGCATACCCTGTCCCCAGAAAAAGCACTCTAAGAGTAGGCGCGCTACTCCTGGGCGAGGAAACTGCGGAAGTGAGGCATTGGTGCCAGGTTCAAAGATGGCGCTGAGCAGCCAAGCGCAGAAGCGAAGAGAGGGCGGCAGCCTGCGGCCGTGGCCGGCCCGCGAGGTCTGGGCCTGGGAGCG", "||||..||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.||||||||.||||||||||||||.||||||||||||||.|.|||.|||", "GCCTATTGCAAGACCTGGAAGAAAGAAAAGTAACGATTCTTCTCGGCCAGAGAGAGAAGCATGACCGGTTTTGCATACCCTGTCCCCAGAAAAAGCACTCTAAGAGTAGGCGCGCTACTCCTGGGCGAGGAAACTGCGGAAGTGAGGCATTGGTGCCAGGTTCAAAGATGGCGCTGAGCAGCCAAGCGCAGAAGCGAAGAGAGGTCGGCAGCCGGCGGCCGTGGCCGGGCCGCGAGGTCTGGGACGGGGGGCG"};
		String qName = "GL000219.1_165002_true_+";
		BLATRecord br = BLATRecordUtil.getBLATRecord(bufferedCP, swDiffs, qName, seq, true, bufferedRef);
		assertEquals(245, br.getMatchCount());
		assertEquals(8, br.getMisMatches());
		assertEquals(0, br.getQueryGapCount());
		assertEquals(0, br.getnCount());
		assertEquals(0, br.getRepMatch());
		assertEquals(0, br.getqBaseInsert());
		assertEquals(0, br.gettNumInsert());
		assertEquals(0, br.gettBaseInsert());
		assertEquals('+', br.getStrand());
		assertEquals(qName, br.getQName());
		assertEquals("GL000219.1", br.getTName());
		assertEquals(179198, br.getChromsomeLength());
		assertEquals(253, br.getSize());
		assertEquals(253, br.getQueryLength());
		assertEquals(0, br.getqStart()); 
		assertEquals(253, br.getQueryEnd());
		assertEquals(165336, br.gettStart()); 
		assertEquals(165589, br.gettEnd());
		assertEquals(1, br.getBlockCount());
		assertArrayEquals(null, br.getBlockSizes());
		assertEquals("0", br.getqStarts());
		assertEquals("165336", br.gettStarts());
	}
	
	@Test
	public void getBlatDetails3() {
		/*
		 * TATATATATATATATATATATACACAACCATTTTCCACATAGGCAGATTTATTTGCTGAAGTTACTTTGCAGAAATATACCACACCCATGGAGATATATAGATATAGATACAGATATAGATATAGATATCTATATATGTAGATAGATATAGATATCTATATATGTAGATAGATATAGATATCTATATATGTAGATAGATATAGATATCTATATATGTAGATAGAGAGAGATGCTGTGTAACTCTGAAGATTAGTACGGTTAGTGTTTTATTTTTCAAAGGGAGAGAGGTAATAGAGAAAAATGCCAGATGCCCTGGCACACCTTAATTTATTTTTTAATCACATCCAGCTTAAACAGT
swDiffs: |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||                                |||||||.||||              |||||||||||||||||||||||||||||||||                ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
swDiffs: TATATATATATATATATATATACACAACCATTTTCCACATAGGCAGATTTATTTGCTGAAGTTACTTTGCAGAAATATACCACACCCATGGAGATATATAGATATAGAT--------------------------------ATAGATACAGAT--------------ATAGATATAGATATCTATATATGTAGATAGATA----------------TAGATAGAGAGAGATGCTGTGTAACTCTGAAGATTAGTACGGTTAGTGTTTTATTTTTCAAAGGGAGAGAGGTAATAGGGAAAAATGCCAGATGCCCTGGCACACCTTAATTTATTTTTTAATCACATCCAGCTTAAACAGT
		 */
		String bufferedRef = "TATATATATATATATATATATACACAACCATTTTCCACATAGGCAGATTTATTTGCTGAAGTTACTTTGCAGAAATATACCACACCCATGGAGATATATAGATATAGATACAGATATAGATATAGATATCTATATATGTAGATAGATATAGATATCTATATATGTAGATAGATATAGATATCTATATATGTAGATAGATATAGATATCTATATATGTAGATAGAGAGAGATGCTGTGTAACTCTGAAGATTAGTACGGTTAGTGTTTTATTTTTCAAAGGGAGAGAGGTAATAGAGAAAAATGCCAGATGCCCTGGCACACCTTAATTTATTTTTTAATCACATCCAGCTTAAACAGT";
		String seq = "TATATATATATATATATATATACACAACCATTTTCCACATAGGCAGATTTATTTGCTGAAGTTACTTTGCAGAAATATACCACACCCATGGAGATATATAGATATAGATATAGATACAGATATAGATATAGATATCTATATATGTAGATAGATATAGATAGAGAGAGATGCTGTGTAACTCTGAAGATTAGTACGGTTAGTGTTTTATTTTTCAAAGGGAGAGAGGTAATAGGGAAAAATGCCAGATGCCCTGGCACACCTTAATTTATTTTTTAATCACATCCAGCTTAAACAGT";
		ChrPosition bufferedCP = new ChrRangePosition("GL000219.1", 165336, 165602);	//GL000219.1:165336-165602
		String [] swDiffs = new String[] {"TATATATATATATATATATATACACAACCATTTTCCACATAGGCAGATTTATTTGCTGAAGTTACTTTGCAGAAATATACCACACCCATGGAGATATATAGATATAGATACAGATATAGATATAGATATCTATATATGTAGATAGATATAGATATCTATATATGTAGATAGATATAGATATCTATATATGTAGATAGATATAGATATCTATATATGTAGATAGAGAGAGATGCTGTGTAACTCTGAAGATTAGTACGGTTAGTGTTTTATTTTTCAAAGGGAGAGAGGTAATAGAGAAAAATGCCAGATGCCCTGGCACACCTTAATTTATTTTTTAATCACATCCAGCTTAAACAGT", "|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||                                |||||||.||||              |||||||||||||||||||||||||||||||||                ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||", "TATATATATATATATATATATACACAACCATTTTCCACATAGGCAGATTTATTTGCTGAAGTTACTTTGCAGAAATATACCACACCCATGGAGATATATAGATATAGAT--------------------------------ATAGATACAGAT--------------ATAGATATAGATATCTATATATGTAGATAGATA----------------TAGATAGAGAGAGATGCTGTGTAACTCTGAAGATTAGTACGGTTAGTGTTTTATTTTTCAAAGGGAGAGAGGTAATAGGGAAAAATGCCAGATGCCCTGGCACACCTTAATTTATTTTTTAATCACATCCAGCTTAAACAGT"};
		String qName = "GL000219.1_165002_true_+";
		BLATRecord br = BLATRecordUtil.getBLATRecord(bufferedCP, swDiffs, qName, seq, true, bufferedRef);
		assertEquals(294, br.getMatchCount());
		assertEquals(2, br.getMisMatches());
		assertEquals(3, br.getQueryGapCount());
		assertEquals(62, br.getqBaseInsert());
		assertEquals(0, br.getnCount());
		assertEquals(0, br.getRepMatch());
		assertEquals(0, br.gettNumInsert());
		assertEquals(0, br.gettBaseInsert());
		assertEquals('+', br.getStrand());
		assertEquals(qName, br.getQName());
		assertEquals("GL000219.1", br.getTName());
		assertEquals(179198, br.getChromsomeLength());
		assertEquals(296, br.getSize());
		assertEquals(296, br.getQueryLength());
		assertEquals(0, br.getqStart()); 
		assertEquals(296, br.getQueryEnd());
		assertEquals(165336, br.gettStart()); 
		assertEquals(165694, br.gettEnd());
		assertEquals(4, br.getBlockCount());
		assertEquals("109,12,33,142", br.getBlockSizesString());
		assertEquals("0,109,121,154", br.getqStarts());
		assertEquals("165336,165477,165503,165552", br.gettStarts());
	}
	
	@Test
	public void getBlatDetails4() {
		/*
		 * swDiffs: CGTGGGGGTGGGATCCACTGAGCTAGAACACTTGGCTCCCTGGCTTTGGCCCCCTTTCCAGGGGAGTGAACAGTTCTGTCTTGCTGGTGTTCCAGGCGCCACTGGGGTATGAAAAATATTCCTGCAGCTAGCTCAGTGTCTAC----CCAAATGGCCACCTGGTTTTGTGCTTCAA
swDiffs: |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|    |.|..|||.|||    ||||.||.|||.|
swDiffs: CGTGGGGGTGGGATCCACTGAGCTAGAACACTTGGCTCCCTGGCTTTGGCCCCCTTTCCAGGGGAGTGAACAGTTCTGTCTTGCTGGTGTTCCAGGCGCCACTGGGGTATGAAAAATATTCCTGCAGCTAGCTCAGTGTCTTCTTGGCAATGTGGGCAC----TTTTTTGGTTCCA
		 */
		String bufferedRef = "CGTGGGGGTGGGATCCACTGAGCTAGAACACTTGGCTCCCTGGCTTTGGCCCCCTTTCCAGGGGAGTGAACAGTTCTGTCTTGCTGGTGTTCCAGGCGCCACTGGGGTATGAAAAATATTCCTGCAGCTAGCTCAGTGTCTACCCAAATGGCCACCTGGTTTTGTGCTTCAA";
		String seq = "CGTGGGGGTGGGATCCACTGAGCTAGAACACTTGGCTCCCTGGCTTTGGCCCCCTTTCCAGGGGAGTGAACAGTTCTGTCTTGCTGGTGTTCCAGGCGCCACTGGGGTATGAAAAATATTCCTGCAGCTAGCTCAGTGTCTTCTTGGCAATGTGGGCACTTTTTTGGTTCCATATGAATTTTAAAGTAGTTTTTTCCAATTCTGTGAAGAAA";
		ChrPosition bufferedCP = new ChrRangePosition("GL000219.1", 165336, 165602);	//GL000219.1:165336-165602
		String [] swDiffs = new String[] {
				"CGTGGGGGTGGGATCCACTGAGCTAGAACACTTGGCTCCCTGGCTTTGGCCCCCTTTCCAGGGGAGTGAACAGTTCTGTCTTGCTGGTGTTCCAGGCGCCACTGGGGTATGAAAAATATTCCTGCAGCTAGCTCAGTGTCTAC----CCAAATGGCCACCTGGTTTTGTGCTTCAA", 
				"|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|    |.|..|||.|||    ||||.||.|||.|", 
				"CGTGGGGGTGGGATCCACTGAGCTAGAACACTTGGCTCCCTGGCTTTGGCCCCCTTTCCAGGGGAGTGAACAGTTCTGTCTTGCTGGTGTTCCAGGCGCCACTGGGGTATGAAAAATATTCCTGCAGCTAGCTCAGTGTCTTCTTGGCAATGTGGGCAC----TTTTTTGGTTCCA"};
		String qName = "GL000219.1_165002_true_+";
		BLATRecord br = BLATRecordUtil.getBLATRecord(bufferedCP, swDiffs, qName, seq, true, bufferedRef);
		
		assertEquals(160, br.getMatchCount());
		assertEquals(8, br.getMisMatches());
		assertEquals(1, br.getQueryGapCount());
		assertEquals(4, br.getqBaseInsert());
		assertEquals(0, br.getnCount());
		assertEquals(0, br.getRepMatch());
		assertEquals(1, br.gettNumInsert());
		assertEquals(4, br.gettBaseInsert());
		assertEquals('+', br.getStrand());
		assertEquals(qName, br.getQName());
		assertEquals("GL000219.1", br.getTName());
		assertEquals(179198, br.getChromsomeLength());
		assertEquals(212, br.getSize());
		assertEquals(172, br.getQueryLength());
		assertEquals(0, br.getqStart()); 
		assertEquals(172, br.getQueryEnd());
		assertEquals(165336, br.gettStart()); 
		assertEquals(165508, br.gettEnd());
		assertEquals(3, br.getBlockCount());
		assertEquals("143,12,13", br.getBlockSizesString());
		assertEquals("0,147,159", br.getqStarts());
		assertEquals("165336,165479,165495", br.gettStarts());
	}
	
	@Test
	public void getBlatDetails5() {
		/*
		 * swDiffs: 
		 * CTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGACCCACCAGGAGACCAGGTGAGCATGAGACCTGCTGTCCACTCCCACTCCCTCCTTCCCACAGCCTCCCCAGACCTCTCTCCCCTCATCCTGGCTTCCCCTCTGTCTGCAGGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGAGAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCATCACCCAATGCAGG
		 * ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||                                                                                              ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
           CTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGACCCACCAGGAGAC----------------------------------------------------------------------------------------------CAGGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGAGAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCATCACCCAATGCAGGG
		 */
		String bufferedRef = "GGCCAAGGCAAGAAAATCACTTGAACCCAGGAGATGGAGGTTGCAGTGAGTCAAGATCGCACCACTGCACTCCAGCCTGGGTGACAGAGTGAGACTGTCTCAAAAAGAACCAGGAGGGCACATGGGCATGGGGAGTGATGAACCAGAGAAAGCTGCTGTCTTTCTGGGCAAGTGCCAAGCAACGGATCACCCTTGACCCCTAGGAAAGAAAAAGAAGAAAACCCGAGGTGATCATTTTAAACTTCGCTTCCGAAAAAACTTTCAGGCCCTGTTGGAGGAGCAGGTGAGAGGAGGGTCGGCCTGGGAGGACCCCACAGGGAAGGGGTGAGCCTGGCCCGGGCAGGTGTTCGCTGCGTGGGTGGGCGGAGGAGTTCTAGAGCCGGCCCCTTGTCTCTGCAGAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGACCCACCAGGAGACCAGGTGAGCATGAGACCTGCTGTCCACTCCCACTCCCTCCTTCCCACAGCCTCCCCAGACCTCTCTCCCCTCATCCTGGCTTCCCCTCTGTCTGCAGGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGAGAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCATCACCCAATGCAGGGGGAGCTCTTCCTGGACCAAGGGAGGAGCCGCTCATTCACCCAACAAAACTGTGTCTTATCTGCCAGGAAAGACCAGCCTCACTCCTGGGAACTGTCTGGCAGGTAGGCTGGGCCCCCCAGTGCTGTTAGAATAAAAAGCCTCGTGCCGGAAGCCTTCCTGTTTGGTCGTGGTGTGTTTGAGGTGATGGTAATGGGTCACCCGTCTCTCCTGCTCACGGCTCTGTCTCTCTTCCTCCTGCCTCCCACTCACCCCTGCCACCGTCCGCCCCTCTGTGTCCCTGATCGCGAGAGATTCTGTCCCATTTTCCTGCCACCCCCGAGCCCCTGCCCTCCTTGGCTGCTTCTTTAAGTCTTTTTGGTTATTGATTTAGTTGTTTAAACTATTTTATTTATTTATTAGAGACAG";
		String seq = "CTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGACCCACCAGGAGACCAGGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGAGAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCATCACCCAATGCAGGG";
		ChrPosition bufferedCP = new ChrRangePosition("chr7", 100867054 - 500 , 100867054 + 500 + seq.length());	//GL000219.1:165336-165602
		String [] swDiffs = new String[] {"CTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGACCCACCAGGAGACCAGGTGAGCATGAGACCTGCTGTCCACTCCCACTCCCTCCTTCCCACAGCCTCCCCAGACCTCTCTCCCCTCATCCTGGCTTCCCCTCTGTCTGCAGGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGAGAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCATCACCCAATGCAGG",
				"||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||                                                                                              ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||",
				"CTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGACCCACCAGGAGAC----------------------------------------------------------------------------------------------CAGGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGAGAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCATCACCCAATGCAGGG"};
		String qName = "chr7:100867054+";
		BLATRecord br = BLATRecordUtil.getBLATRecord(bufferedCP, swDiffs, qName, seq, true, bufferedRef);
		
		assertEquals(172, br.getMatchCount());
		assertEquals(0, br.getMisMatches());
		assertEquals(1, br.getQueryGapCount());
		assertEquals(94, br.getqBaseInsert());
		assertEquals(0, br.getnCount());
		assertEquals(0, br.getRepMatch());
		assertEquals(0, br.gettNumInsert());
		assertEquals(0, br.gettBaseInsert());
		assertEquals('+', br.getStrand());
		assertEquals(qName, br.getQName());
		assertEquals("chr7", br.getTName());
		assertEquals(159138663, br.getChromsomeLength());
		assertEquals(172, br.getSize());
		assertEquals(0, br.getqStart()); 
		assertEquals(172, br.getQueryEnd());
		assertEquals(100867054, br.gettStart()); 
		assertEquals(100867319, br.gettEnd());
		assertEquals(2, br.getBlockCount());
		assertEquals("66,106", br.getBlockSizesString());
		assertEquals("0,66", br.getqStarts());
		assertEquals("100867054,100867214", br.gettStarts());
		
	}
	
	@Test
	public void merge() {
		List<BLATRecord> recs = Arrays.asList(
			new BLATRecord.Builder("22	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	0	22	chr6	171115067	114262231	114262253	1	22	0	114262231").build(),
			new BLATRecord.Builder("58	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	21	79	chr6	171115067	114262871	114262929	1	58	21	114262871").build()
		);
		
		Optional<BLATRecord> mergedRec = BLATRecordUtil.mergeBLATRecs(recs, 0);
		assertEquals(true, mergedRec.isPresent());
		assertEquals("78	79	0	0	0	0	0	1	619	+	chr6_114264515_true_+	79	0	79	chr6	171115067	114262231	114262929	2	22,57	0,22	114262231,114262872", mergedRec.get().toString());
	}
	
	@Test
	public void merge2() {
		List<BLATRecord> recs = Arrays.asList(
				new BLATRecord.Builder("58	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	21	79	chr6	171115067	114262871	114262929	1	58	21	114262871").build(),
				new BLATRecord.Builder("22	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	0	22	chr6	171115067	114262231	114262253	1	22	0	114262231").build()
				);
		
		Optional<BLATRecord> mergedRec = BLATRecordUtil.mergeBLATRecs(recs, 0);
		assertEquals(true, mergedRec.isPresent());
		assertEquals("78	79	0	0	0	0	0	1	619	+	chr6_114264515_true_+	79	0	79	chr6	171115067	114262231	114262929	2	22,57	0,22	114262231,114262872", mergedRec.get().toString());
		
		/*
		 * different contig
		 */
		recs = Arrays.asList(
				new BLATRecord.Builder("58	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	21	79	chr6	171115067	114262871	114262929	1	58	21	114262871").build(),
				new BLATRecord.Builder("22	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	0	22	chr7	171115067	114262231	114262253	1	22	0	114262231").build()
				);
		
		mergedRec = BLATRecordUtil.mergeBLATRecs(recs, 0);
		assertEquals(false, mergedRec.isPresent());
		
		/*
		 * different strand
		 */
		recs = Arrays.asList(
				new BLATRecord.Builder("58	0	0	0	0	0	0	0	-	chr6_114264515_true_+	79	21	79	chr6	171115067	114262871	114262929	1	58	21	114262871").build(),
				new BLATRecord.Builder("22	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	0	22	chr6	171115067	114262231	114262253	1	22	0	114262231").build()
				);
		
		mergedRec = BLATRecordUtil.mergeBLATRecs(recs, 0);
		assertEquals(false, mergedRec.isPresent());
		
		/*
		 * far away
		 */
		recs = Arrays.asList(
				new BLATRecord.Builder("58	0	0	0	0	0	0	0	-	chr6_114264515_true_+	79	21	79	chr6	171115067	114262871	114262929	1	58	21	114262871").build(),
				new BLATRecord.Builder("22	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	0	22	chr6	171115067	14262231	14262253	1	22	0	14262231").build()
				);
		
		mergedRec = BLATRecordUtil.mergeBLATRecs(recs, 0);
		assertEquals(false, mergedRec.isPresent());
	}
	
	@Test
	public void merge3() {
		List<BLATRecord> recs = Arrays.asList(
				new BLATRecord.Builder("45	0	0	0	0	0	0	0	+	splitcon_chr19_47884249_chr19_47884442_1_true_1606876103304_406441_clip	154	108	153	chr19	59128983	47884441	47884486	1	45	108	47884441").build(),
				new BLATRecord.Builder("109	0	0	0	0	0	0	0	+	splitcon_chr19_47884249_chr19_47884442_1_true_1606876103304_406441_clip	154	0	109	chr19	59128983	47884140	47884249	1	109	0	47884140").build()
				);
		
		Optional<BLATRecord> mergedRec = BLATRecordUtil.mergeBLATRecs(recs, 0);
		assertEquals(true, mergedRec.isPresent());
		assertEquals("152	153	0	0	0	0	0	1	193	+	splitcon_chr19_47884249_chr19_47884442_1_true_1606876103304_406441_clip	154	0	153	chr19	59128983	47884140	47884486	2	108,45	0,108	47884140,47884441", mergedRec.get().toString());
	}
	
	@Test
	public void getCoverageAndOverlap() {
		List<BLATRecord> recs = Arrays.asList(
				new BLATRecord.Builder("255	0	0	0	1	94	0	0	+	splitcon_chr7_100867120_chr7_100867215__true_1605137694083_663070	398	79	334	chr7	159138663	100866949	100867298	2	171,84	79,250	100866949,100867214").build(),
				new BLATRecord.Builder("58	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	21	79	chr6	171115067	114262871	114262929	1	58	21	114262871").build()
				);
		
		Optional<int[]> stats = BLATRecordUtil.getCombinedNonOverlappingScore(recs);
		assertEquals(true, stats.isPresent());
		assertArrayEquals(new int[] {313, 0}, stats.get());
		
		recs = Arrays.asList(
				new BLATRecord.Builder("58	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	21	79	chr6	171115067	114262871	114262929	1	58	21	114262871").build(),
				new BLATRecord.Builder("58	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	21	79	chr6	171115067	114262871	114262929	1	58	21	114262871").build()
				);
		
		stats = BLATRecordUtil.getCombinedNonOverlappingScore(recs);
		assertEquals(true, stats.isPresent());
		assertArrayEquals(new int[] {58, 58}, stats.get());
		
		recs = Arrays.asList(
				new BLATRecord.Builder("21	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	0	21	chr6	171115067	114262871	114262929	1	58	21	114262871").build(),
				new BLATRecord.Builder("58	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	21	79	chr6	171115067	114262871	114262929	1	58	21	114262871").build()
				);
		
		stats = BLATRecordUtil.getCombinedNonOverlappingScore(recs);
		assertEquals(true, stats.isPresent());
		assertArrayEquals(new int[] {79, 0}, stats.get());
		
		recs = Arrays.asList(
				new BLATRecord.Builder("50	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	0	50	chr6	171115067	114262871	114262929	1	58	21	114262871").build(),
				new BLATRecord.Builder("58	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	21	79	chr6	171115067	114262871	114262929	1	58	21	114262871").build()
				);
		
		stats = BLATRecordUtil.getCombinedNonOverlappingScore(recs);
		assertEquals(true, stats.isPresent());
		assertArrayEquals(new int[] {79, 29}, stats.get());
	}
	
	@Test
	public void splitsShortCut() {
		String seq = "GTCAAATTCAGGGGTTGCTGAGCTGTTCTGATTTGGTTCCTTTGGTATCTGTTTTTTCACCACTGTTGTCCTTGGATTTATCTTCTTCCTTAACGTCTGTTTTTTTGTCCTCTGTTTCTTTCTTATCTTCTTCAATTCTAGCTTTCTTTGCTCCTTTCTTATGATCAGCCACATTTCTTCGACCTCCTTCTCCTTCATCCTCAGAATCTGAGAATTCTTCATCACAAGCTATCCGCTTGTCTGATGCTCGAATAGAAATTCTCTTGTCTGGATCTTCTCCATCTTCATCTCCACTGTCTTCATGAACAGCATCTTCTGG";
		
		int[][]startPositionsAndLengths = new int[4][];
		startPositionsAndLengths[0] = new int[] {0,0,39};
		startPositionsAndLengths[1] = new int[] {658,39,59};
		startPositionsAndLengths[2] = new int[] {2302,98,154};
		startPositionsAndLengths[3] = new int[] {3229,252,67};
		
		ChrPosition bufferredCP = new ChrRangePosition("chr6", 114262214, 114262214 + 3283);
		
		BLATRecord br = BLATRecordUtil.getRecordFromStartPositionsAndLengths(bufferredCP, startPositionsAndLengths, "splitcon_chr6_114262929_chr6_114264515__true_1605145420035_711092", seq, true);
		assertEquals("316	319	0	0	0	0	0	3	2974	+	splitcon_chr6_114262929_chr6_114264515__true_1605145420035_711092	319	0	319	chr6	171115067	114262214	114265510	4	39,59,154,67	0,39,98,252	114262214,114262872,114264516,114265443", br.toString());
	}
	
	@Test
	public void getBlatDetails6() {
		/*
		 * swDiffs: 
		 *swDiffs: CTGACAGTTTGCCTGGTGTAAGT-CATGTGTGTCTTGTTAAAAAAAATTTAATAAGAACAAAACAACTGGGGGGAGGAGCCAAGATGGCCGAATAGGAACAGCTCCGGTCTACAGCTCCCAGTGTGAGCGACG
swDiffs: ||.|.|..||..||||||.||.| ||.||.| .||||..|||..||||           ||.||.| |.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|||
swDiffs: CTTAAATATTTTCTGGTGGAAATGCAGGTAT-CCTTGGAAAATGAAAT-----------AACACTA-TAGGGGGAGGAGCCAAGATGGCCGAATAGGAACAGCTCCGGTCTACAGCTCCCAGTGTGAGCAACG
		 */
		String bufferedRef = "ACTTATTTGAGTTCCCACTTTATTTTGTACATATTTTCCCTTATTTTGATTAATTCTCCATGAGCTATTTTGGATAAATAAAAGTCATATTTGTCCTTCTACCTTTATAGTTTTCCAGGCACTCATGTAAAAATTGATCAAATCTAGAAATCAATTGGGTGATTCCTAGAAATTTCTGTTACTTGAAGAACACAACTTTTTCATTGTGATCTCTGATGAATAAACCTTTTTCACTTAACAATTCAATACCTTCCTGTTGGGAGCAGGCCCCCCAAAATCTGGCGATAAACCAGCCCCAAAACTGGCCATAAACAATCTCTGCAGCACTGTAACATGTTCATAATGGCCCTAACGCCCAAGCTGGAAGGTTGTGGGTTTACAGGAATGAGGGCAAGGAACACCTGGCCTGCCCAGGGTGGAAAACCGCTTAAAGGCATTCTTAAGCTACAAACAATAGCATGAGCGATCTGTGCCTTAAGGACATGCTCCTGCTGCAGTTAACTAGCCTAACCTATTCCTTTAATTCGGCCCATCCCTTCCTTTCCCATAAGGGATACTTTCAGTTAATTTAACATCTATAGAAACAATGCTAATGACTGGTTTGCTGTCAGTAAATACGTGGGTAAATCTCTGTTGGGGGCTGTCAGTTCTGAAGGCTGCGAGACCCCTGATTTCCCACTTCACACCTCTGTATTTCTGTGTGTGTGTCTTTAATTCCTCTAGTGCTGCTAGGTTAGGGTCTCCCCAACCGAGCTTGTCTCCACACCTTCCACTGTTCTTCTTAGTACTTCAGCATAATACTGTATGTTCCTTCTTTGTCCTGCAAGACAACGGTAAAGTGCATTATAATTAATTTATGTGTAATCCAAGTAAACAAGCCTCATTGTGTTGTATGTGTCTTATGATTAAGAGCTCAATACATTTAATCTAGTCTGACAGTTTGCCTGGTGTAAGTCATGTGTGTCTTGTTAAAAAAAATTTAATAAGAACAAAACAACTGGGGGGAGGAGCCAAGATGGCCGAATAGGAACAGCTCCGGTCTACAGCTCCCAGTGTGAGCGACGCAGAAGACGGGTGATTTCTGCATTTCCATCTGAGGTACCGGGTTTGTCTCACTAGGGAGTGCCAGACAGTGGGCGCAGGCCAGTGGGTGCGCGCACCGTGCGCAAGCCGAAGCAGGGCGAGGCATTGCCTCACTTGGGAAGCGCAAGGGGTCAGGGAGTTCCCTTTCCGAGTCAAAGAAAGGGGTGACAGACGCACCTGGAAAATCGGGTCACTCCCACCCGAATATTGTGCTTTTCAGACCGGCTTAAAAAACGGCGAACCACGAGATTATATCCCACACCTGGCTGGGAGGGTCCTACGCCCACGGAATCTCGCTGACTGCTAGCACAGCAGTCTGAGATCAAACTGCAAGGCGGCAGCGAGGCTGGGGGAGGGGTGCCTGCTATTGCCCAGGCTTGCTTAGGTAAACAAAGCAGCCAGGAGGCTCGAACTGGGTGGAGCCCACCACAACTCAAGGAGGCCTGCCTGCCTCTGTAGGCTCCACCTCTGGGGGCAGGGCACAGACAAACAAAAAGACAGCAGTAACCTCTGCAGGCTTAAGTGTCCCTGTCTGACAGCTTTGAAGAGAGCAGTGGTTCTCCCAGCACGCAGCTGGAGATCTGAGAACCGGCAGACTGCCTCCTCAAGTGGGTCCCTGACCACTGACCCCTGACCCCCGAGCAGCCTAACTGGGAGGCACCCCGCAGCAGGGGCACACTGACACCTCACACGGCAGGGTATTCCAACAGACCTGCAGCTGAGGGTCCTGTCTGTTAGAAGGAAAACTAACAAACAGAAAGGACATCCACATCGAAAACCCATCTGTACATCACCATCATCAAAGACCAAAAGTAGATAAAACCACAAAGATGGGAAAAAAACAGAACAGAAAAACTGGAAACTCTAAAACGCAGAGCACCTCTCCTCCTCCAAAGGAACGCAGTTCCTCACCAGCAACGGAACAAAGCTGGATGGAGAATGACTTTGACGAGCTGAGAGAAGAAGGCTTCAGACGATCAAATTACTCTGAGCTACGGGAGGACATTCAAACCAAAGGCAAAGAAGTTGAAAACTTTGAAAAAAATTTAGAAGAATGTATAACTAGAATAACCAATACAGAGAAGTGCTTAAAGGAGCTG";
		String seq = "TTCGGCGTTGCTCACACTGGGAGCTGTAGACCGGAGCTGTTCCTATTCGGCCATCTTGGCTCCTCCCCCTATAGTGTTATTTCATTTTCCAAGGATACCTGCATTTCCACCAGAAAATATTTAAGGGGTTACACATTTCCCGTTTTGGTTAACCTGGATAAATGCGCGTATTTTATTTCTGTTTTCAG";
		ChrPosition bufferedCP = new ChrRangePosition("chr10", 127633806 - 1000 , 127633806 + 1000 + seq.length());	//GL000219.1:165336-165602
		String [] swDiffs = new String[] {"GGGGGAGGAGCCAAGATGGCCGAATAGGAACAGCTCCGGTCTACAGCTCCCAGTGTGAGCGACG",
				                          "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|||",
				                          "GGGGGAGGAGCCAAGATGGCCGAATAGGAACAGCTCCGGTCTACAGCTCCCAGTGTGAGCAACG"};
		String qName = "splitcon_chr10_127633807_chr15_34031839";
		BLATRecord br =  BLATRecordUtil.getBLATRecord(bufferedCP, swDiffs, qName, seq, false, bufferedRef);
		
		assertEquals(63, br.getMatchCount());
		assertEquals(1, br.getMisMatches());
		assertEquals(0, br.getQueryGapCount());
		assertEquals(0, br.getqBaseInsert());
		assertEquals(0, br.getnCount());
		assertEquals(0, br.getRepMatch());
		assertEquals(0, br.gettNumInsert());
		assertEquals(0, br.gettBaseInsert());
		assertEquals('-', br.getStrand());
		assertEquals(qName, br.getQName());
		assertEquals("chr10", br.getTName());
		assertEquals(135534747, br.getChromsomeLength());
		assertEquals(188, br.getSize());
		assertEquals(5, br.getqStart()); 
		assertEquals(69, br.getQueryEnd());
		assertEquals(127633806, br.gettStart()); 
		assertEquals(127633870, br.gettEnd());
		assertEquals(1, br.getBlockCount());
		assertEquals("64", br.getBlockSizesString());
		assertEquals("119", br.getqStarts());
		assertEquals("127633806", br.gettStarts());
	}
	
	@Test
	public void findRecordInRange() {
		List<BLATRecord> recs = Arrays.asList(
				new BLATRecord.Builder("65	2	0	0	0	0	0	0	-	splitcon_chr10_127633807_chr15_34031839__true_1604619619623_732066	188	4	71	chr10	135534747	86039721	86039788	1	67	117	86039721").build(),
				new BLATRecord.Builder("64	1	0	0	0	0	0	0	-	splitcon_chr10_127633807_chr15_34031839__true_1604619619623_732066	188	4	69	chr10	135534747	127633806	127633871	1	65	119	127633806").build(),
				new BLATRecord.Builder("65	2	0	0	0	0	0	0	+	splitcon_chr10_127633807_chr15_34031839__true_1604619619623_732066	188	4	71	chr10	135534747	1273430	1273497	1	67	4	1273430").build(),
				new BLATRecord.Builder("76	1	0	0	2	2	2	7	-	splitcon_chr10_127633807_chr15_34031839__true_1604619619623_732066	188	4	88	chr15	102531392	82162994	82163073	5	7,3,2,4,61	100,107,114,119,123	82162994,82163002,82163005,82163007,82163012").build(),
				new BLATRecord.Builder("75	2	0	0	1	3	1	1	-	splitcon_chr10_127633807_chr15_34031839__true_1604619619623_732066	188	7	85	chr10	135534747	111365186	111365266	3	11,3,63	103,115,118	111365186,111365197,111365203").build()
				);
		Optional<BLATRecord> optionalBR = BLATRecordUtil.findRecordInRange(recs, 0, 69);
		assertEquals(true, optionalBR.isPresent());
		assertEquals(new BLATRecord.Builder("64	1	0	0	0	0	0	0	-	splitcon_chr10_127633807_chr15_34031839__true_1604619619623_732066	188	4	69	chr10	135534747	127633806	127633871	1	65	119	127633806").build(), optionalBR.get());
	}
	@Test
	public void findRecordInRange2() {
		List<BLATRecord> recs = Arrays.asList(
				new BLATRecord.Builder("105	0	0	0	0	0	0	0	+	splitcon_chr7_124989445_chr7_124989504__true_1607391016216_844626	281	176	281	chr7	159138663	124989542	124989647	1	105	176	124989542").build()
				);
		Optional<BLATRecord> optionalBR = BLATRecordUtil.findRecordInRange(recs, 152, 281);
		assertEquals(true, optionalBR.isPresent());
		assertEquals(new BLATRecord.Builder("105	0	0	0	0	0	0	0	+	splitcon_chr7_124989445_chr7_124989504__true_1607391016216_844626	281	176	281	chr7	159138663	124989542	124989647	1	105	176	124989542").build(), optionalBR.get());
	}
	
	@Test
	public void getBlatDetails8() {
		String name = "chr6_151381607";
		String bufferedRef = "CCCTTTTGGTACACAATCCTCAGATGAACGGAGATGATTCATTAGGCCATTCTAGCTTAATGGATGCATCACTGTGCAACCACGCAAATGCCTCATTTCTGATTCACTCATTTAATATTTATTGAGGAGTGCTGGGTACTAGGCCATTAGAAGAACAAACAACAACAACATAGGCCTGGACCTTGCACTTGCAGAGTTCACAGGCTAGAGAGGCACACAAGTTAAATATAGAGACAGCTGAAAGAGCTGACGTCTGGGACAGAGGGAGCCCCCTGCTCCAGGAGCCAGGGAGCAGATGCCATGGGGGGCTGGCAGGGATAGGGAAGT";
		String seq = "TAGTACCCAGCACTCCTCAATAAATATTAAATGAGTGAATCATTATATTTAACTTGTGTGCCTCTCTAGCCTGTGAACTCTGCAAGTGCAAGGTCCAGGCCTATGTTGTTGTTGTTTGTTCTTCTAATGGCCTAGTACCCAGCACTCCTCAATAAATATTAAATGAGTGAATCAT";
		ChrPosition bufferedCP = new ChrRangePosition("chr6", 151381376 , 151381376 + 200 + seq.length());	
		String [] swDiffs = new String[] {  "TGATTCACTCATTTAATATTTATTGAGGAGTGCTGGGTACTAGGCCATTAGAAGAACAAACAACAACAACATAGGCCTGGACCTTGCACTTGCAGAGTTCACAGGCTAGAGAGGCACACAAGTTAAATATAGA-GA--CAGCTGA---AA--------GAGCTGACGT-CTGGG-AC",
											"||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| | ||  || ||.|   ||        |||  || || ||||| ||",
											"TGATTCACTCATTTAATATTTATTGAGGAGTGCTGGGTACTAGGCCATTAGAAGAACAAACAACAACAACATAGGCCTGGACCTTGCACTTGCAGAGTTCACAGGCTAGAGAGGCACACAAGTTAAATATA-ATGATTCA-CTCATTTAATATTTATTGAG--GA-GTGCTGGGTAC"};
		/*
		 * 144     155     1       0       0       4       5       6       16      -       chr6_151381607  175     134     174     chr6    12345   151381476       151381637       11      131,1,2,2,4,2,3,2,2,5,2 1,0,2,6,8,15,25,2,30,33,7       151381476,151378479,151378486,151378497,151378484,151378479,151378486,151378486,151378500,151378583,151378491
		 */
		BLATRecord br = BLATRecordUtil.getBLATRecord(bufferedCP, swDiffs, name, seq, false, bufferedRef);
		
		assertEquals(155, br.getMatchCount());
		assertEquals(1, br.getMisMatches());
		assertEquals(4, br.getQueryGapCount());
		assertEquals(5, br.getqBaseInsert());
		assertEquals(0, br.getnCount());
		assertEquals(0, br.getRepMatch());
		assertEquals(6, br.gettNumInsert());
		assertEquals(16, br.gettBaseInsert());
		assertEquals('-', br.getStrand());
		assertEquals(name, br.getQName());
		assertEquals("chr6", br.getTName());
		assertEquals(171115067, br.getChromsomeLength());
		assertEquals(175, br.getSize());
		assertEquals(2, br.getqStart()); 
		assertEquals(174, br.getQueryEnd());
		assertEquals(151381475, br.gettStart()); 
		assertEquals(151381636, br.gettEnd());
		assertEquals(11, br.getBlockCount());
		assertEquals("131,1,2,2,4,2,3,2,2,5,2", br.getBlockSizesString());
		assertEquals("1,132,134,138,140,147,157,160,162,165,171", br.getqStarts());
		assertEquals("151381475,151381607,151381608,151381610,151381613,151381617,151381619,151381624,151381627,151381629,151381634", br.gettStarts());
	}
	
	@Test
	public void getBLATRecFromCompoundChrPositions() {
		String seq = "CTGTTGTCCACTGCCCCAGCCACATCATCCCTGTGCGGGTTGCAGATGCTGCTAAAAACACAGAAGTCTGTGATGAACTAATGAGCAGACATAACATCTACGTGCAAGCAATCAATTACCCTACGGTGCCCCGGGGAGAAGAGCTCCTACGGATTGCCCCCACCCCTCACCACACACCCCAGATGATGAACTACTTCCTTGGTGAGTACCTGGGGAGCTGCTGGTGCCTCACTGAGGAGTTGCATAAAGCTGTCTTTGCAGTGTTTATAATTGAAGCCCTTCGGAGGGCTTCAGATTTGTTTCTTCTTCTTTTTTTATTTTTTTTTTTTTTTCCATTATTTTCGTTCTTTTTTCCCTTCCTTGGTTTTTTTTGCCCAATCCCT";
		List<ChrPosition> positions = Arrays.asList(
				new ChrPositionName("chr3", 52245530, 52245568, "CTGTTGTCCACTGCCCCAGCCACATCATCCCTGTGCGG"),
				new ChrPositionName("chr3", 52246274, 52246517, "GTTGCAGATGCTGCTAAAAACACAGAAGTCTGTGATGAACTAATGAGCAGACATAACATCTACGTGCAAGCAATCAATTACCCTACGGTGCCCCGGGGAGAAGAGCTCCTACGGATTGCCCCCACCCCTCACCACACACCCCAGATGATGAACTACTTCCTTGGTGAGTACCTGGGGAGCTGCTGGTGCCTCACTGAGGAGTTGCATAAAGCTGTCTTTGCAGTGTTTATAATTGAAGCCCTTC"));
		
		Optional<BLATRecord> obr = BLATRecordUtil.getDetailsForBLATRecord(positions, "name", seq, true);
		assertEquals(true, obr.isPresent());
		BLATRecord br = obr.get();
		/*
		 * 324	4	0	0	0	0	1	706	+	YourSeq	383	0	328	chr3	198022430	52245529	52246563	2	38,290,	0,38,	52245529,52246273
		 */
		assertEquals(281, br.getMatchCount());
		assertEquals(0, br.getMisMatches());
		assertEquals(0, br.getQueryGapCount());
		assertEquals(0, br.getqBaseInsert());
		assertEquals(0, br.getnCount());
		assertEquals(0, br.getRepMatch());
		assertEquals(1, br.gettNumInsert());
		assertEquals(706, br.gettBaseInsert());
		assertEquals('+', br.getStrand());
		assertEquals("name", br.getQName());
		assertEquals("chr3", br.getTName());
		assertEquals(198022430, br.getChromsomeLength());
		assertEquals(383, br.getSize());
		assertEquals(0, br.getqStart()); 
		assertEquals(281, br.getQueryEnd());
		assertEquals(52245530, br.gettStart()); 
		assertEquals(52246517, br.gettEnd());
		assertEquals(2, br.getBlockCount());
		assertEquals("38,243", br.getBlockSizesString());
		assertEquals("0,38", br.getqStarts());
		assertEquals("52245530,52246274", br.gettStarts());
	}
	
	@Test
	public void getBLATRecordFromCSRealLife() {
		ChrPosition cp1 = new ChrPositionName("chr7", 100866787, 100866837, "ATTTTAAACTTCGCTTCCGAAAAAACTTTCAGGCCCTGTTGGAGGAGCAG");
		ChrPosition cp2 = new ChrPositionName("chr7", 100866949, 100867067, "GCAGAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTC");
		String seq = "ATTTTAAACTTCGCTTCCGAAAAAACTTTCAGGCCCTGTTGGAGGAGCAGAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTC";
		
		Optional<BLATRecord> obr = BLATRecordUtil.getDetailsForBLATRecord(Arrays.asList(cp1, cp2), "name", seq, true);
		assertEquals(true, obr.isPresent());
		BLATRecord br = obr.get();
		
		assertEquals(164, br.getMatchCount());
		assertEquals(0, br.getMisMatches());
		assertEquals(0, br.getQueryGapCount());
		assertEquals(0, br.getqBaseInsert());
		assertEquals(0, br.getnCount());
		assertEquals(0, br.getRepMatch());
		assertEquals(1, br.gettNumInsert());
		assertEquals(116, br.gettBaseInsert());
		assertEquals('+', br.getStrand());
		assertEquals("name", br.getQName());
		assertEquals("chr7", br.getTName());
		assertEquals(159138663, br.getChromsomeLength());
		assertEquals(164, br.getSize());
		assertEquals(0, br.getqStart()); 
		assertEquals(164, br.getQueryEnd());
		assertEquals(100866787, br.gettStart()); 
		assertEquals(100867067, br.gettEnd());
		assertEquals(2, br.getBlockCount());
		assertEquals("50,114", br.getBlockSizesString());
		assertEquals("0,50", br.getqStarts());
		assertEquals("100866787,100866953", br.gettStarts());
	}
	
	@Test
	public void getBLATRecordFromCSRealLife2() {
		/*
		 * chromosome=chr7, startPosition=100866764, endPosition=100866843, name=AAAGAAGAAAACCCGAGGTGATCATTTTAAACTTCGCTTCCGAAAAAACTTTCAGGCCCTGTTGGAGGAGCAGGTGAGA]
in getDetailsForBLATRecord with cp: ChrPositionName [chromosome=chr7, startPosition=100866949, endPosition=100867128, name=GCAGAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGACCCACCAGGAGACCAGGTGAG
		 */
		ChrPosition cp1 = new ChrPositionName("chr7", 100866764, 100866843, "AAAGAAGAAAACCCGAGGTGATCATTTTAAACTTCGCTTCCGAAAAAACTTTCAGGCCCTGTTGGAGGAGCAGGTGAGA");
		ChrPosition cp2 = new ChrPositionName("chr7", 100866949, 100867128, "GCAGAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGACCCACCAGGAGACCAGGTGAG");
		String seq = "TGGAAAGAAAAAGAAGAAAACCCGAGGTGATCATTTTAAACTTCGCTTCCGAAAAAAACTTTCAGGCCCTGTTGGAGGAGCAGAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTCAGCTGCGGTGCCCGGTACTGCACTGTGCGCTGTCTGGGGACCCACCAGGAGACCAGGTGTCTGAAGTGGACTGTGTGAGCCTGGGCATTCCCAGAGAGGAAGGGCCGCTGTGCACTGCCCGGCCTTCAGAAAGACAGAATTTCATCACCCAATGCAGGGGGAGCTCTTCCTGGACCAAGGGAGGAGCCGCTCATTCACCCG";
		
		Optional<BLATRecord> obr = BLATRecordUtil.getDetailsForBLATRecord(Arrays.asList(cp1, cp2), "name", seq, true);
		assertEquals(false, obr.isPresent());
	}
	
	@Test
	public void getBLATRecFromPerfectMatch() {
		String seq = "CTGTTGTCCACTGCCCCAGCCACATCATCCCTGTGCGG";
		List<ChrPosition> positions = Arrays.asList(
				new ChrPositionName("chr3", 52245530, 52245568, seq));
		
		Optional<BLATRecord> obr = BLATRecordUtil.getDetailsForBLATRecord(positions, "name", seq, true);
		assertEquals(true, obr.isPresent());
		BLATRecord br = obr.get();
		
		assertEquals(38, br.getMatchCount());
		assertEquals(0, br.getMisMatches());
		assertEquals(0, br.getQueryGapCount());
		assertEquals(0, br.getqBaseInsert());
		assertEquals(0, br.getnCount());
		assertEquals(0, br.getRepMatch());
		assertEquals(0, br.gettNumInsert());
		assertEquals(0, br.gettBaseInsert());
		assertEquals('+', br.getStrand());
		assertEquals("name", br.getQName());
		assertEquals("chr3", br.getTName());
		assertEquals(198022430, br.getChromsomeLength());
		assertEquals(38, br.getSize());
		assertEquals(0, br.getqStart()); 
		assertEquals(38, br.getQueryEnd());
		assertEquals(52245530, br.gettStart()); 
		assertEquals(52245568, br.gettEnd());
		assertEquals(1, br.getBlockCount());
		assertEquals("38", br.getBlockSizesString());
		assertEquals("0", br.getqStarts());
		assertEquals("52245530", br.gettStarts());
	}
	
	@Test
	public void getBLATRecFromPerfectMatch2() {
		String seq = "AAACTACACACACACACACACACACACACACACACACACACACACACACA";
		List<ChrPosition> positions = Arrays.asList(
				new ChrPositionName("chr2",3240987, 3240987 + seq.length(), seq));
		
		Optional<BLATRecord> obr = BLATRecordUtil.getDetailsForBLATRecord(positions, "name", seq, true);
		assertEquals(true, obr.isPresent());
		BLATRecord br = obr.get();
		
		assertEquals(50, br.getMatchCount());
		assertEquals(0, br.getMisMatches());
		assertEquals(0, br.getQueryGapCount());
		assertEquals(0, br.getqBaseInsert());
		assertEquals(0, br.getnCount());
		assertEquals(0, br.getRepMatch());
		assertEquals(0, br.gettNumInsert());
		assertEquals(0, br.gettBaseInsert());
		assertEquals('+', br.getStrand());
		assertEquals("name", br.getQName());
		assertEquals("chr2", br.getTName());
		assertEquals(243199373, br.getChromsomeLength());
		assertEquals(50, br.getSize());
		assertEquals(0, br.getqStart()); 
		assertEquals(50, br.getQueryEnd());
		assertEquals(3240987, br.gettStart()); 
		assertEquals(3241037, br.gettEnd());
		assertEquals(1, br.getBlockCount());
		assertEquals("50", br.getBlockSizesString());
		assertEquals("0", br.getqStarts());
		assertEquals("3240987", br.gettStarts());
	}
	
	@Test
	public void removeOverlappingRecs() {
		BLATRecord r1 = new BLATRecord.Builder("34\t0\t0\t0\t0\t0\t0\t0\t+\tchr6_168677484_false_+\t95\t0\t34\tchr6\t171115067\t168677450\t168677484\t1\t34\t0\t168677450").build();
		BLATRecord r2 = new BLATRecord.Builder("63\t0\t0\t0\t0\t0\t0\t0\t+\tchr6_168677484_false_+\t95\t32\t95\tchr6\t171115067\t168677414\t168677477\t1\t63\t32\t168677414").build();
		BLATRecord r3 = new BLATRecord.Builder("68\t0\t0\t0\t0\t0\t0\t0\t+\tchr6_168677484_false_+\t95\t0\t68\tchr6\t171115067\t168677416\t168677484\t1\t68\t0\t168677416").build();
		List<BLATRecord> recs = Arrays.asList(r1, r2, r3);
		
		List<BLATRecord> nonOverlappingRecs = BLATRecordUtil.removeOverlappingRecords(recs);
		assertEquals(1, nonOverlappingRecs.size());
		assertEquals(r3, nonOverlappingRecs.get(0));	// should be the record with the highest score that is left
	}
	
	@Test
	public void doRecordsOverlap() {
		String bufferedRef = "GCCTCCTGCAAGACCTGGAAGAAAGAAAAGTAACGATTCTTCTCGGCCAGAGAGAGAAGCATGACCGGTTTTGCATACCCTGTCCCCAGAAAAAGCACTCTAAGAGTAGGCGCGCTACTCCTGGGCGAGGAAACTGCGGAAGTGAGGCATTGGTGCCAGGTTCAAAGATGGCGCTGAGCAGCCAAGCGCAGAAGCGAAGAGAGGGCGGCAGCCTGCGGCCGTGGCCGGCCCGCGAGGTCTGGGCCTGGGAGCG";
		String seq = "GCCTATTGCAAGACCTGGAAGAAAGAAAAGTAACGATTCTTCTCGGCCAGAGAGAGAAGCATGACCGGTTTTGCATACCCTGTCCCCAGAAAAAGCACTCTAAGAGTAGGCGCGCTACTCCTGGGCGAGGAAACTGCGGAAGTGAGGCATTGGTGCCAGGTTCAAAGATGGCGCTGAGCAGCCAAGCGCAGAAGCGAAGAGAGGTCGGCAGCCGGCGGCCGTGGCCGGGCCGCGAGGTCTGGGACGGGGGGCG";
		ChrPosition bufferedCP = new ChrRangePosition("GL000219.1", 16536, 165602);	//GL000219.1:165336-165602
		String [] swDiffs = new String[] {"GCCTCCTGCAAGACCTGGAAGAAAGAAAAGTAACGATTCTTCTCGGCCAGAGAGAGAAGCATGACCGGTTTTGCATACCCTGTCCCCAGAAAAAGCACTCTAAGAGTAGGCGCGCTACTCCTGGGCGAGGAAACTGCGGAAGTGAGGCATTGGTGCCAGGTTCAAAGATGGCGCTGAGCAGCCAAGCGCAGAAGCGAAGAGAGGGCGGCAGCCTGCGGCCGTGGCCGGCCCGCGAGGTCTGGGCCTGGGAGCG", "||||..||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.||||||||.||||||||||||||.||||||||||||||.|.|||.|||", "GCCTATTGCAAGACCTGGAAGAAAGAAAAGTAACGATTCTTCTCGGCCAGAGAGAGAAGCATGACCGGTTTTGCATACCCTGTCCCCAGAAAAAGCACTCTAAGAGTAGGCGCGCTACTCCTGGGCGAGGAAACTGCGGAAGTGAGGCATTGGTGCCAGGTTCAAAGATGGCGCTGAGCAGCCAAGCGCAGAAGCGAAGAGAGGTCGGCAGCCGGCGGCCGTGGCCGGGCCGCGAGGTCTGGGACGGGGGGCG"};
		
		BLATRecord r1 = BLATRecordUtil.getBLATRecord(bufferedCP, swDiffs, "GL000219.1_165002_true_+", seq, true, bufferedRef);
		bufferedCP = new ChrRangePosition("GL000219.1", 16546, 165612);	//GL000219.1:165336-165602
		BLATRecord r2 = BLATRecordUtil.getBLATRecord(bufferedCP, swDiffs, "GL000219.1_165002_true_+", seq, true, bufferedRef);
		
		assertEquals(true, BLATRecordUtil.doRecordsOverlapReference(r1, r2));
	}

}
