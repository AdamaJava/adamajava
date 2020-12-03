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
		
		String [] blatDetails = BLATRecordUtil.getDetailsForBLATRecord(bufferedCP, swDiffs, "GL000219.1_165002_true_+", seq, true, bufferedRef);
		assertEquals(21, blatDetails.length);
		assertEquals("57", blatDetails[0]); // matches
		assertEquals("7", blatDetails[1]); // mis-matches
		assertEquals("1", blatDetails[4]); // q gap counts
		assertEquals("1", blatDetails[5]); // q gap bases
		assertEquals("0", blatDetails[6]); // t gap counts
		assertEquals("0", blatDetails[7]); // t gap bases
		assertEquals("+", blatDetails[8]); // strand
		assertEquals("66", blatDetails[10]); // query size
		assertEquals("1", blatDetails[11]); // query start
		assertEquals("65", blatDetails[12]); // query end
		assertEquals("" + (seq.length() - 1), blatDetails[12]); // query end
		assertEquals("2", blatDetails[17]); // block size
		assertEquals("44,20", blatDetails[18]); // block lengths
		assertEquals("1,45", blatDetails[19]); // Q starts
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
		
		String [] blatDetails = BLATRecordUtil.getDetailsForBLATRecord(bufferedCP, swDiffs, "GL000219.1_165002_true_+", seq, true, bufferedRef);
		assertEquals(21, blatDetails.length);
		assertEquals("245", blatDetails[0]); // matches
		assertEquals("8", blatDetails[1]); // mis-matches
		assertEquals("0", blatDetails[4]); // q gap counts
		assertEquals("0", blatDetails[5]); // q gap bases
		assertEquals("0", blatDetails[6]); // t gap counts
		assertEquals("0", blatDetails[7]); // t gap bases
		assertEquals("+", blatDetails[8]); // strand
		assertEquals("" + seq.length(), blatDetails[10]); // query size
		assertEquals("0", blatDetails[11]); // query start
		assertEquals("" + seq.length(), blatDetails[12]); // query end
		assertEquals("1", blatDetails[17]); // block size
		assertEquals("" + seq.length(), blatDetails[18]); // block lengths
		assertEquals("0", blatDetails[19]); // Q starts
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
		
		String [] blatDetails = BLATRecordUtil.getDetailsForBLATRecord(bufferedCP, swDiffs, "GL000219.1_165002_true_+", seq, true, bufferedRef);
		assertEquals(21, blatDetails.length);
		assertEquals("294", blatDetails[0]); // matches
		assertEquals("2", blatDetails[1]); // mis-matches
		assertEquals("3", blatDetails[4]); // q gap counts
		assertEquals("62", blatDetails[5]); // q gap bases
		assertEquals("0", blatDetails[6]); // t gap counts
		assertEquals("0", blatDetails[7]); // t gap bases
		assertEquals("+", blatDetails[8]); // strand
		assertEquals("" + seq.length(), blatDetails[10]); // query size
		assertEquals("0", blatDetails[11]); // query start
		assertEquals("" + seq.length(), blatDetails[12]); // query end
		assertEquals("4", blatDetails[17]); // block size
		assertEquals("109,12,33,142", blatDetails[18]); // block lengths
		assertEquals("0,109,121,154", blatDetails[19]); // Q starts
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
		
		String [] blatDetails = BLATRecordUtil.getDetailsForBLATRecord(bufferedCP, swDiffs, "GL000219.1_165002_true_+", seq, true, bufferedRef);
		assertEquals(21, blatDetails.length);
		assertEquals("160", blatDetails[0]); // matches
		assertEquals("8", blatDetails[1]); // mis-matches
		assertEquals("1", blatDetails[4]); // q gap counts
		assertEquals("4", blatDetails[5]); // q gap bases
		assertEquals("1", blatDetails[6]); // t gap counts
		assertEquals("4", blatDetails[7]); // t gap bases
		assertEquals("+", blatDetails[8]); // strand
		assertEquals("" + seq.length(), blatDetails[10]); // query size
		assertEquals("0", blatDetails[11]); // query start
		assertEquals("172", blatDetails[12]); // query end
		assertEquals("3", blatDetails[17]); // block size
		assertEquals("143,12,13", blatDetails[18]); // block lengths
		assertEquals("0,147,159", blatDetails[19]); // Q starts
		assertEquals("165336,165479,165495", blatDetails[20]); // T starts
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
		
		String [] blatDetails = BLATRecordUtil.getDetailsForBLATRecord(bufferedCP, swDiffs, "chr7:100867054+", seq, true, bufferedRef);
		assertEquals(21, blatDetails.length);
		assertEquals("172", blatDetails[0]); // matches
		assertEquals("0", blatDetails[1]); // mis-matches
		assertEquals("0", blatDetails[2]); //
		assertEquals("0", blatDetails[3]); // 
		assertEquals("1", blatDetails[4]); // q gap counts
		assertEquals("94", blatDetails[5]); // q gap bases
		assertEquals("0", blatDetails[6]); // t gap counts
		assertEquals("0", blatDetails[7]); // t gap bases
		assertEquals("+", blatDetails[8]); // strand
		assertEquals("" + seq.length(), blatDetails[10]); // query size
		assertEquals("0", blatDetails[11]); // query start
		assertEquals("172", blatDetails[12]); // query end
		assertEquals("2", blatDetails[17]); // block size
		assertEquals("66,106", blatDetails[18]); // block lengths
		assertEquals("0,66", blatDetails[19]); // Q starts
		int templateBlock2Start = 100867054 + 66 + 94;
		assertEquals("100867054," + templateBlock2Start, blatDetails[20]); // template starts
	}
	
	@Test
	public void merge() {
		List<BLATRecord> recs = Arrays.asList(
			new BLATRecord("22	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	0	22	chr6	171115067	114262231	114262253	1	22	0	114262231"),
			new BLATRecord("58	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	21	79	chr6	171115067	114262871	114262929	1	58	21	114262871")
		);
		
		Optional<BLATRecord> mergedRec = BLATRecordUtil.mergeBLATRecs(recs, 0);
		assertEquals(true, mergedRec.isPresent());
		assertEquals("78	79	0	0	0	0	0	1	619	+	chr6_114264515_true_+	79	0	79	chr6	171115067	114262231	114262929	2	22,57	0,22	114262231,114262872", mergedRec.get().toString());
	}
	
	@Test
	public void merge2() {
		List<BLATRecord> recs = Arrays.asList(
				new BLATRecord("58	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	21	79	chr6	171115067	114262871	114262929	1	58	21	114262871"),
				new BLATRecord("22	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	0	22	chr6	171115067	114262231	114262253	1	22	0	114262231")
				);
		
		Optional<BLATRecord> mergedRec = BLATRecordUtil.mergeBLATRecs(recs, 0);
		assertEquals(true, mergedRec.isPresent());
		assertEquals("78	79	0	0	0	0	0	1	619	+	chr6_114264515_true_+	79	0	79	chr6	171115067	114262231	114262929	2	22,57	0,22	114262231,114262872", mergedRec.get().toString());
		
		/*
		 * different contig
		 */
		recs = Arrays.asList(
				new BLATRecord("58	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	21	79	chr6	171115067	114262871	114262929	1	58	21	114262871"),
				new BLATRecord("22	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	0	22	chr7	171115067	114262231	114262253	1	22	0	114262231")
				);
		
		mergedRec = BLATRecordUtil.mergeBLATRecs(recs, 0);
		assertEquals(false, mergedRec.isPresent());
		
		/*
		 * different strand
		 */
		recs = Arrays.asList(
				new BLATRecord("58	0	0	0	0	0	0	0	-	chr6_114264515_true_+	79	21	79	chr6	171115067	114262871	114262929	1	58	21	114262871"),
				new BLATRecord("22	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	0	22	chr6	171115067	114262231	114262253	1	22	0	114262231")
				);
		
		mergedRec = BLATRecordUtil.mergeBLATRecs(recs, 0);
		assertEquals(false, mergedRec.isPresent());
		
		/*
		 * far away
		 */
		recs = Arrays.asList(
				new BLATRecord("58	0	0	0	0	0	0	0	-	chr6_114264515_true_+	79	21	79	chr6	171115067	114262871	114262929	1	58	21	114262871"),
				new BLATRecord("22	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	0	22	chr6	171115067	14262231	14262253	1	22	0	14262231")
				);
		
		mergedRec = BLATRecordUtil.mergeBLATRecs(recs, 0);
		assertEquals(false, mergedRec.isPresent());
	}
	
	@Test
	public void merge3() {
		List<BLATRecord> recs = Arrays.asList(
				new BLATRecord("45	0	0	0	0	0	0	0	+	splitcon_chr19_47884249_chr19_47884442_1_true_1606876103304_406441_clip	154	108	153	chr19	59128983	47884441	47884486	1	45	108	47884441"),
				new BLATRecord("109	0	0	0	0	0	0	0	+	splitcon_chr19_47884249_chr19_47884442_1_true_1606876103304_406441_clip	154	0	109	chr19	59128983	47884140	47884249	1	109	0	47884140")
				);
		
		Optional<BLATRecord> mergedRec = BLATRecordUtil.mergeBLATRecs(recs, 0);
		assertEquals(true, mergedRec.isPresent());
		assertEquals("152	153	0	0	0	0	0	1	193	+	splitcon_chr19_47884249_chr19_47884442_1_true_1606876103304_406441_clip	154	0	153	chr19	59128983	47884140	47884486	2	108,45	0,108	47884140,47884441", mergedRec.get().toString());
	}
	
	@Test
	public void getCoverageAndOverlap() {
		List<BLATRecord> recs = Arrays.asList(
				new BLATRecord("255	0	0	0	1	94	0	0	+	splitcon_chr7_100867120_chr7_100867215__true_1605137694083_663070	398	79	334	chr7	159138663	100866949	100867298	2	171,84	79,250	100866949,100867214"),
				new BLATRecord("58	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	21	79	chr6	171115067	114262871	114262929	1	58	21	114262871")
				);
		
		Optional<int[]> stats = BLATRecordUtil.getCombinedNonOverlappingScore(recs);
		assertEquals(true, stats.isPresent());
		assertArrayEquals(new int[] {313, 0}, stats.get());
		
		recs = Arrays.asList(
				new BLATRecord("58	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	21	79	chr6	171115067	114262871	114262929	1	58	21	114262871"),
				new BLATRecord("58	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	21	79	chr6	171115067	114262871	114262929	1	58	21	114262871")
				);
		
		stats = BLATRecordUtil.getCombinedNonOverlappingScore(recs);
		assertEquals(true, stats.isPresent());
		assertArrayEquals(new int[] {58, 58}, stats.get());
		
		recs = Arrays.asList(
				new BLATRecord("21	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	0	21	chr6	171115067	114262871	114262929	1	58	21	114262871"),
				new BLATRecord("58	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	21	79	chr6	171115067	114262871	114262929	1	58	21	114262871")
				);
		
		stats = BLATRecordUtil.getCombinedNonOverlappingScore(recs);
		assertEquals(true, stats.isPresent());
		assertArrayEquals(new int[] {79, 0}, stats.get());
		
		recs = Arrays.asList(
				new BLATRecord("50	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	0	50	chr6	171115067	114262871	114262929	1	58	21	114262871"),
				new BLATRecord("58	0	0	0	0	0	0	0	+	chr6_114264515_true_+	79	21	79	chr6	171115067	114262871	114262929	1	58	21	114262871")
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
		
		String [] blatDetails = BLATRecordUtil.getDetailsForBLATRecord(bufferedCP, swDiffs, "splitcon_chr10_127633807_chr15_34031839", seq, false, bufferedRef);
		assertEquals(21, blatDetails.length);
		assertEquals("63", blatDetails[0]); // matches
		assertEquals("1", blatDetails[1]); // mis-matches
		assertEquals("0", blatDetails[2]); //
		assertEquals("0", blatDetails[3]); // 
		assertEquals("0", blatDetails[4]); // q gap counts
		assertEquals("0", blatDetails[5]); // q gap bases
		assertEquals("0", blatDetails[6]); // t gap counts
		assertEquals("0", blatDetails[7]); // t gap bases
		assertEquals("-", blatDetails[8]); // strand
		assertEquals("" + seq.length(), blatDetails[10]); // query size
		assertEquals("5", blatDetails[11]); // query start
		assertEquals("69", blatDetails[12]); // query end
		assertEquals("127633806", blatDetails[15]); // target start
		assertEquals("127633870", blatDetails[16]); // target end
		assertEquals("1", blatDetails[17]); // block size
		assertEquals("64", blatDetails[18]); // block lengths
		assertEquals("119", blatDetails[19]); // Q starts
		assertEquals("127633806", blatDetails[20]); // template starts
	}
	
	@Test
	public void findRecordInRange() {
		List<BLATRecord> recs = Arrays.asList(
				new BLATRecord("65	2	0	0	0	0	0	0	-	splitcon_chr10_127633807_chr15_34031839__true_1604619619623_732066	188	4	71	chr10	135534747	86039721	86039788	1	67	117	86039721"),
				new BLATRecord("64	1	0	0	0	0	0	0	-	splitcon_chr10_127633807_chr15_34031839__true_1604619619623_732066	188	4	69	chr10	135534747	127633806	127633871	1	65	119	127633806"),
				new BLATRecord("65	2	0	0	0	0	0	0	+	splitcon_chr10_127633807_chr15_34031839__true_1604619619623_732066	188	4	71	chr10	135534747	1273430	1273497	1	67	4	1273430"),
				new BLATRecord("76	1	0	0	2	2	2	7	-	splitcon_chr10_127633807_chr15_34031839__true_1604619619623_732066	188	4	88	chr15	102531392	82162994	82163073	5	7,3,2,4,61	100,107,114,119,123	82162994,82163002,82163005,82163007,82163012"),
				new BLATRecord("75	2	0	0	1	3	1	1	-	splitcon_chr10_127633807_chr15_34031839__true_1604619619623_732066	188	7	85	chr10	135534747	111365186	111365266	3	11,3,63	103,115,118	111365186,111365197,111365203")
				);
		Optional<BLATRecord> optionalBR = BLATRecordUtil.findRecordInRange(recs, 0, 69);
		assertEquals(true, optionalBR.isPresent());
		assertEquals(new BLATRecord("64	1	0	0	0	0	0	0	-	splitcon_chr10_127633807_chr15_34031839__true_1604619619623_732066	188	4	69	chr10	135534747	127633806	127633871	1	65	119	127633806"), optionalBR.get());
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
		
		String [] blatDetails = BLATRecordUtil.getDetailsForBLATRecord(bufferedCP, swDiffs, name, seq, false, bufferedRef);
		assertEquals(21, blatDetails.length);
		assertEquals("155", blatDetails[0]); // matches
		assertEquals("1", blatDetails[1]); // mis-matches
		assertEquals("0", blatDetails[2]); //
		assertEquals("0", blatDetails[3]); // 
		assertEquals("4", blatDetails[4]); // q gap counts
		assertEquals("5", blatDetails[5]); // q gap bases
		assertEquals("6", blatDetails[6]); // t gap counts
		assertEquals("16", blatDetails[7]); // t gap bases
		assertEquals("-", blatDetails[8]); // strand
		assertEquals(name, blatDetails[9]); // name
		assertEquals("" + seq.length(), blatDetails[10]); // query size
		assertEquals("2", blatDetails[11]); // query start
		assertEquals("174", blatDetails[12]); // query end
		assertEquals("151381475", blatDetails[15]); // target start
		assertEquals("151381636", blatDetails[16]); // target end
		assertEquals("11", blatDetails[17]); // block size
		assertEquals("131,1,2,2,4,2,3,2,2,5,2", blatDetails[18]); // block lengths
		assertEquals("1,132,134,138,140,147,157,160,162,165,171", blatDetails[19]); // Q starts
		assertEquals("151381475,151381607,151381608,151381610,151381613,151381617,151381619,151381624,151381627,151381629,151381634", blatDetails[20]); // template starts
		/*
		 * 144     155     1       0       0       4       5       6       16      -       chr6_151381607  175     134     174     chr6    12345   151381476       151381637       11      131,1,2,2,4,2,3,2,2,5,2 1,0,2,6,8,15,25,2,30,33,7       151381476,151378479,151378486,151378497,151378484,151378479,151378486,151378486,151378500,151378583,151378491
		 */
	}
	
	@Test
	public void getBLATRecFromCompoundChrPositions() {
		String seq = "CTGTTGTCCACTGCCCCAGCCACATCATCCCTGTGCGGGTTGCAGATGCTGCTAAAAACACAGAAGTCTGTGATGAACTAATGAGCAGACATAACATCTACGTGCAAGCAATCAATTACCCTACGGTGCCCCGGGGAGAAGAGCTCCTACGGATTGCCCCCACCCCTCACCACACACCCCAGATGATGAACTACTTCCTTGGTGAGTACCTGGGGAGCTGCTGGTGCCTCACTGAGGAGTTGCATAAAGCTGTCTTTGCAGTGTTTATAATTGAAGCCCTTCGGAGGGCTTCAGATTTGTTTCTTCTTCTTTTTTTATTTTTTTTTTTTTTTCCATTATTTTCGTTCTTTTTTCCCTTCCTTGGTTTTTTTTGCCCAATCCCT";
		List<ChrPosition> positions = Arrays.asList(
				new ChrPositionName("chr3", 52245530, 52245568, "CTGTTGTCCACTGCCCCAGCCACATCATCCCTGTGCGG"),
				new ChrPositionName("chr3", 52246274, 52246517, "GTTGCAGATGCTGCTAAAAACACAGAAGTCTGTGATGAACTAATGAGCAGACATAACATCTACGTGCAAGCAATCAATTACCCTACGGTGCCCCGGGGAGAAGAGCTCCTACGGATTGCCCCCACCCCTCACCACACACCCCAGATGATGAACTACTTCCTTGGTGAGTACCTGGGGAGCTGCTGGTGCCTCACTGAGGAGTTGCATAAAGCTGTCTTTGCAGTGTTTATAATTGAAGCCCTTC"));
		
		String [] blatRecDetails = BLATRecordUtil.getDetailsForBLATRecord(positions, "name", seq, true);
		/*
		 * 324	4	0	0	0	0	1	706	+	YourSeq	383	0	328	chr3	198022430	52245529	52246563	2	38,290,	0,38,	52245529,52246273
		 */
		assertEquals("281", blatRecDetails[0]);
		assertEquals("0", blatRecDetails[1]);
		assertEquals("0", blatRecDetails[2]);
		assertEquals("0", blatRecDetails[3]);
		assertEquals("0", blatRecDetails[4]);
		assertEquals("0", blatRecDetails[5]);
		assertEquals("1", blatRecDetails[6]);
		assertEquals("706", blatRecDetails[7]);
		assertEquals("+", blatRecDetails[8]);
		assertEquals("name", blatRecDetails[9]);
		assertEquals("" + seq.length(), blatRecDetails[10]);
		assertEquals("0", blatRecDetails[11]);
		assertEquals("281", blatRecDetails[12]);
		assertEquals("chr3", blatRecDetails[13]);
		assertEquals("198022430", blatRecDetails[14]);
		assertEquals("52245530", blatRecDetails[15]);
		assertEquals("52246517", blatRecDetails[16]);
		assertEquals("2", blatRecDetails[17]);
		assertEquals("38,243", blatRecDetails[18]);
		assertEquals("0,38", blatRecDetails[19]);
		assertEquals("52245530,52246274", blatRecDetails[20]);
	}
	
	@Test
	public void getBLATRecordFromCSRealLife() {
		ChrPosition cp1 = new ChrPositionName("chr7", 100866787, 100866837, "ATTTTAAACTTCGCTTCCGAAAAAACTTTCAGGCCCTGTTGGAGGAGCAG");
		ChrPosition cp2 = new ChrPositionName("chr7", 100866949, 100867067, "GCAGAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTC");
		String seq = "ATTTTAAACTTCGCTTCCGAAAAAACTTTCAGGCCCTGTTGGAGGAGCAGAACTTGAGTGTGGCCGAGGGCCCTAACTACCTGACGGCCTGTGCGGGACCCCCATCGCGGCCCCAGCGCCCCTTCTGTGCTGTCTGTGGCTTCCCATCCCCCTACACCTGTGTC";
		
		String [] blatRecDetails = BLATRecordUtil.getDetailsForBLATRecord(Arrays.asList(cp1, cp2), "name", seq, true);
		assertEquals("164", blatRecDetails[0]);
		assertEquals("0", blatRecDetails[1]);
		assertEquals("0", blatRecDetails[2]);
		assertEquals("0", blatRecDetails[3]);
		assertEquals("0", blatRecDetails[4]);
		assertEquals("0", blatRecDetails[5]);
		assertEquals("1", blatRecDetails[6]);
		assertEquals("" + (100866949 - 100866837 + 4), blatRecDetails[7]);	// Q gap bases
		assertEquals("+", blatRecDetails[8]);
		assertEquals("name", blatRecDetails[9]);
		assertEquals("" + seq.length(), blatRecDetails[10]);
		assertEquals("0", blatRecDetails[11]);
		assertEquals("" + seq.length(), blatRecDetails[12]);					// Q end
		assertEquals("chr7", blatRecDetails[13]);
		assertEquals("159138663", blatRecDetails[14]);
		assertEquals("100866787", blatRecDetails[15]);
		assertEquals("100867067", blatRecDetails[16]);
		assertEquals("2", blatRecDetails[17]);
		assertEquals("50,114", blatRecDetails[18]);
		assertEquals("0,50", blatRecDetails[19]);
		assertEquals("100866787,100866953", blatRecDetails[20]);
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
		
		String [] blatRecDetails = BLATRecordUtil.getDetailsForBLATRecord(Arrays.asList(cp1, cp2), "name", seq, true);
		assertArrayEquals(new String[]{}, blatRecDetails);
	}
	
	@Test
	public void getBLATRecFromPerfectMatch() {
		String seq = "CTGTTGTCCACTGCCCCAGCCACATCATCCCTGTGCGG";
		List<ChrPosition> positions = Arrays.asList(
				new ChrPositionName("chr3", 52245530, 52245568, seq));
		
		String [] blatRecDetails = BLATRecordUtil.getDetailsForBLATRecord(positions, "name", seq, true);
		assertEquals("38", blatRecDetails[0]);
		assertEquals("0", blatRecDetails[1]);
		assertEquals("0", blatRecDetails[2]);
		assertEquals("0", blatRecDetails[3]);
		assertEquals("0", blatRecDetails[4]);
		assertEquals("0", blatRecDetails[5]);
		assertEquals("0", blatRecDetails[6]);
		assertEquals("0", blatRecDetails[7]);
		assertEquals("+", blatRecDetails[8]);
		assertEquals("name", blatRecDetails[9]);
		assertEquals("" + seq.length(), blatRecDetails[10]);
		assertEquals("0", blatRecDetails[11]);
		assertEquals("38", blatRecDetails[12]);
		assertEquals("chr3", blatRecDetails[13]);
		assertEquals("198022430", blatRecDetails[14]);
		assertEquals("52245530", blatRecDetails[15]);
		assertEquals("52245568", blatRecDetails[16]);
		assertEquals("1", blatRecDetails[17]);
		assertEquals("38", blatRecDetails[18]);
		assertEquals("0", blatRecDetails[19]);
		assertEquals("52245530", blatRecDetails[20]);
	}
	
	@Test
	public void getBLATRecFromPerfectMatch2() {
		String seq = "AAACTACACACACACACACACACACACACACACACACACACACACACACA";
		List<ChrPosition> positions = Arrays.asList(
				new ChrPositionName("chr2",3240987, 3240987 + seq.length(), seq));
		
		String [] blatRecDetails = BLATRecordUtil.getDetailsForBLATRecord(positions, "name", seq, true);
		assertEquals("50", blatRecDetails[0]);
		assertEquals("0", blatRecDetails[1]);
		assertEquals("0", blatRecDetails[2]);
		assertEquals("0", blatRecDetails[3]);
		assertEquals("0", blatRecDetails[4]);
		assertEquals("0", blatRecDetails[5]);
		assertEquals("0", blatRecDetails[6]);
		assertEquals("0", blatRecDetails[7]);
		assertEquals("+", blatRecDetails[8]);
		assertEquals("name", blatRecDetails[9]);
		assertEquals("" + seq.length(), blatRecDetails[10]);
		assertEquals("0", blatRecDetails[11]);
		assertEquals("50", blatRecDetails[12]);
		assertEquals("chr2", blatRecDetails[13]);
		assertEquals("243199373", blatRecDetails[14]);
		assertEquals("3240987", blatRecDetails[15]);
		assertEquals("3241037", blatRecDetails[16]);
		assertEquals("1", blatRecDetails[17]);
		assertEquals("50", blatRecDetails[18]);
		assertEquals("0", blatRecDetails[19]);
		assertEquals("3240987", blatRecDetails[20]);
	}
	
	@Test
	public void removeOverlappingRecs() {
		BLATRecord r1 = new BLATRecord("34\t0\t0\t0\t0\t0\t0\t0\t+\tchr6_168677484_false_+\t95\t0\t34\tchr6\t171115067\t168677450\t168677484\t1\t34\t0\t168677450");
		BLATRecord r2 = new BLATRecord("63\t0\t0\t0\t0\t0\t0\t0\t+\tchr6_168677484_false_+\t95\t32\t95\tchr6\t171115067\t168677414\t168677477\t1\t63\t32\t168677414");
		BLATRecord r3 = new BLATRecord("68\t0\t0\t0\t0\t0\t0\t0\t+\tchr6_168677484_false_+\t95\t0\t68\tchr6\t171115067\t168677416\t168677484\t1\t68\t0\t168677416");
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
		
		String [] blatDetails1 = BLATRecordUtil.getDetailsForBLATRecord(bufferedCP, swDiffs, "GL000219.1_165002_true_+", seq, true, bufferedRef);
		BLATRecord r1 = new BLATRecord(blatDetails1);
		bufferedCP = new ChrRangePosition("GL000219.1", 16546, 165612);	//GL000219.1:165336-165602
		String [] blatDetails2 = BLATRecordUtil.getDetailsForBLATRecord(bufferedCP, swDiffs, "GL000219.1_165002_true_+", seq, true, bufferedRef);
		BLATRecord r2 = new BLATRecord(blatDetails2);
		
		assertEquals(true, BLATRecordUtil.doRecordsOverlapReference(r1, r2));
	}

}
