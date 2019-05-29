package org.qcmg.qsv.blat;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.util.QSVUtil;


public class BLATRecordTest {
	
//	@Test
//	public void convertStringArrayToIntArray() {
//		assertEquals(null, BLATRecord.getIntArrayFromStringArray(null));
//		assertEquals(null, BLATRecord.getIntArrayFromStringArray(new String[]{}));
//		assertArrayEquals(new int[]{123}, BLATRecord.getIntArrayFromStringArray(new String[]{"123"}));
//		assertArrayEquals(new int[]{123,123}, BLATRecord.getIntArrayFromStringArray(new String[]{"123","123"}));
//		assertArrayEquals(new int[]{123,123,456}, BLATRecord.getIntArrayFromStringArray(new String[]{"123","123","456"}));
//	}
	
//	@Test
//	public void incrementIntArray() {
//		int [] iArray = new int [] {123};
//		 BLATRecord.incrementIntArrayByOne(iArray);
//		assertArrayEquals(new int[]{124}, iArray);
//		BLATRecord.incrementIntArrayByOne(iArray);
//		assertArrayEquals(new int[]{125}, iArray);
//		BLATRecord.incrementIntArrayByOne(iArray);
//		assertArrayEquals(new int[]{126}, iArray);
//		
//	}
	
	@Test
	public void getCurrentBp() {
		assertEquals(0, BLATRecord.getCurrentBp(0, 1, true, true));
		assertEquals(1, BLATRecord.getCurrentBp(0, 1, true, false));
		assertEquals(1, BLATRecord.getCurrentBp(0, 1, false, true));
		assertEquals(0, BLATRecord.getCurrentBp(0, 1, false, false));
	}
	
	
	@Test
	public void calculateMateBreakpoint() {
		String value = "42	1	0	0	0	0	0	0	+	name	59	0	43	chr10	135534747	89700259	89700301	1	42	1	89700259";
		String[] values =value.split("\t");
		BLATRecord tiledAlignerBlatRecord = new BLATRecord(values);
		assertEquals(89700301, tiledAlignerBlatRecord.calculateMateBreakpoint(true, "chr10", 89712341, '+').intValue());
		
		value = "48\t1\t0\t0\t2\t0\t3\t0\t+\t10-89712341-true-pos\t66\t0\t48\tchr10\t135534747\t89700251\t89700299\t1\t48,\t0,\t89700251,";
		values =value.split("\t");
		BLATRecord record = new BLATRecord(values);
		assertEquals(89700299, record.calculateMateBreakpoint(true, "chr10", 89712341, '+').intValue());
		
	}
	
	@Test
	public void getScore2() {
		String b = "57	7	0	0	1	1	0	0	+	name	66	-1	64	GL000219.1	12345	165413	165478	2	43,19	1,46	165414,165459";
		BLATRecord br = new BLATRecord(b);
		assertEquals(true, br.isValid());
		assertEquals(49, br.getScore());
	}

	@Test
	public void getScore() {
		String b = "51	1	0	0	2	23	2	167	+	GL000219.1_179198_false_+	75	0	75	chr9	141213431	68513272	68513491	3	17,17,18,	0,35,57,	68513272,68513355,68513473,";
		BLATRecord br = new BLATRecord(b);
		assertEquals(true, br.isValid());
		assertEquals(46, br.getScore());
		assertEquals("GL000219.1_179198_false_+", br.getName());
		b = "67	8	0	0	0	0	3	8104	+	GL000219.1_179198_false_+	75	0	75	chrUn_gl000219	179198	169035	177214	4	24,5,5,41,	0,24,29,34,	169035,169081,177165,177173,";
		BLATRecord br2 = new BLATRecord(b);
		assertEquals(true, br2.isValid());
		assertEquals(56, br2.getScore());
		assertEquals("GL000219.1_179198_false_+", br2.getName());
		
		List<BLATRecord> recordsList = Arrays.asList(br, br2);
		recordsList.sort(null);
		assertEquals(br, recordsList.get(0));
		
		/*
		 * create br2 again, this time with a lower score
		 */
		b = "47	8	0	0	0	0	3	8104	+	GL000219.1_179198_false_+	75	0	75	chrUn_gl000219	179198	169035	177214	4	24,5,5,41,	0,24,29,34,	169035,169081,177165,177173,";
		br2 = new BLATRecord(b);
		assertEquals(true, br2.isValid());
		assertEquals(36, br2.getScore());
		assertEquals("GL000219.1_179198_false_+", br2.getName());
		
		recordsList = Arrays.asList(br, br2);
		recordsList.sort(null);
		assertEquals(br2, recordsList.get(0));
	}
	
	@Test
	public void sorting() {
		BLATRecord br1 = new BLATRecord("21\t0\t0\t0\t0\t0\t0\t0\t+\tname\t30\t0\t21\tchr8\t12345\t140578675\t140578696\t1\t21\t0\t140578675");
		BLATRecord br2 = new BLATRecord("23\t2\t0\t0\t1\t3\t2\t3\t-\tname\t30\t2\t30\tchr1\t12345\t28209783\t28209811\t3\t4,21,3\t2,6,27\t28209785,28209789,28209810");
		BLATRecord br3 = new BLATRecord("24\t1\t0\t0\t1\t4\t1\t5\t-\tname\t30\t0\t29\tchr6\t12345\t92780855\t92780885\t2\t4,25\t0,4\t92780855,92780859");
		BLATRecord br4 = new BLATRecord("21\t0\t0\t0\t1\t6\t1\t1\t-\tname\t30\t0\t27\tchr7\t12345\t106750037\t106750059\t2\t23,4\t0,23\t106750037,106750060");
		BLATRecord br5 = new BLATRecord("25\t1\t0\t0\t2\t2\t2\t5\t-\tname\t30\t1\t29\tchr9\t12345\t107229780\t107229811\t3\t22,1,5\t1,2,24\t107229781,107229782,107229804");
		BLATRecord br6 = new BLATRecord("25\t1\t0\t0\t2\t3\t1\t2\t-\tname\t30\t0\t29\tchr16\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410");
		List<BLATRecord> list = Arrays.asList(br1, br2, br3, br4, br5, br6);
		
		list.sort(null);
		assertEquals(br1, list.get(5));
	}
	
	@Test
	public void ctor() throws QSVException {
		String ctor = "41\t1\t0\t0\t1\t4\t1\t5\t-\t12345_123455_12345\t70\t14\t60\tchr9\t141213431\t81359142\t81359189\t2\t17,25,\t10,31,\t81359142,81359164,";
		BLATRecord br = new BLATRecord(ctor);
		assertEquals(true, br.isValid());
		assertEquals(41 - 1 - 1 - 1, br.getScore());
		assertEquals("12345_123455_12345", br.getName());
		assertEquals("chr9", br.getReference());
		assertEquals(2, br.getBlockCount());
		assertArrayEquals(new int[] {81359143,81359165}, br.gettStarts());
		assertEquals(QSVUtil.MINUS, br.getStrand());
		assertEquals(81359142 + 1, br.getStartPos());
		assertEquals(81359189, br.getEndPos());
		assertEquals(14 + 1, br.getQueryStart());
		assertEquals(60, br.getQueryEnd());
		assertEquals(70, br.getSize());
		assertArrayEquals(new int[] {17,25}, br.getBlockSizes());
	}
	
	@Test
	public void unmodifiedStarts() throws QSVException {
		String ctor = "41\t1\t0\t0\t1\t4\t1\t5\t-\t12345_123455_12345\t70\t14\t60\tchr9\t141213431\t81359142\t81359189\t2\t17,25,\t10,31,\t81359142,81359164,";
		BLATRecord br = new BLATRecord(ctor);
		assertEquals(true, br.isValid());
		int i = 70 - 17 - 10 + 1;
		int j = 70 - 25 - 31 + 1;
		assertArrayEquals(new int[] {i ,j }, br.getUnmodifiedStarts());
		
		/*
		 * flip strand - should get different results
		 */
		ctor = "41\t1\t0\t0\t1\t4\t1\t5\t+\t12345_123455_12345\t70\t14\t60\tchr9\t141213431\t81359142\t81359189\t2\t17,25,\t10,31,\t81359142,81359164,";
		br = new BLATRecord(ctor);
		assertEquals(true, br.isValid());
		i = 10 + 1;
		j = 31 + 1;
		assertArrayEquals(new int[] {i ,j}, br.getUnmodifiedStarts());
	}
}
