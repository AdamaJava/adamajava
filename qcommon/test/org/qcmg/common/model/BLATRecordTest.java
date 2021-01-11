package org.qcmg.common.model;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.qcmg.common.model.BLATRecord;


public class BLATRecordTest {
	
	public static final char MINUS = '-';
	
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
		BLATRecord tiledAlignerBlatRecord = new BLATRecord.Builder(values).build();
		assertEquals(89700301, tiledAlignerBlatRecord.calculateMateBreakpoint(true, "chr10", 89712341, '+').intValue());
		
		value = "48\t1\t0\t0\t2\t0\t3\t0\t+\t10-89712341-true-pos\t66\t0\t48\tchr10\t135534747\t89700251\t89700299\t1\t48,\t0,\t89700251,";
		values =value.split("\t");
		BLATRecord record = new BLATRecord.Builder(values).build();
		assertEquals(89700299, record.calculateMateBreakpoint(true, "chr10", 89712341, '+').intValue());
	}
	
	@Test
	public void getScore2() {
		String b = "57	7	0	0	1	1	0	0	+	name	66	-1	64	GL000219.1	12345	165413	165478	2	43,19	1,46	165414,165459";
		BLATRecord br = new BLATRecord.Builder(b).build();
		assertEquals(true, br.isValid());
		assertEquals(49, br.getScore());
	}

	@Test
	public void getScore() {
		String b = "51	1	0	0	2	23	2	167	+	GL000219.1_179198_false_+	75	0	75	chr9	141213431	68513272	68513491	3	17,17,18,	0,35,57,	68513272,68513355,68513473,";
		BLATRecord br = new BLATRecord.Builder(b).build();
		assertEquals(true, br.isValid());
		assertEquals(46, br.getScore());
		assertEquals("GL000219.1_179198_false_+", br.getQName());
		b = "67	8	0	0	0	0	3	8104	+	GL000219.1_179198_false_+	75	0	75	chrUn_gl000219	179198	169035	177214	4	24,5,5,41,	0,24,29,34,	169035,169081,177165,177173,";
		BLATRecord br2 = new BLATRecord.Builder(b).build();
		assertEquals(true, br2.isValid());
		assertEquals(56, br2.getScore());
		assertEquals("GL000219.1_179198_false_+", br2.getQName());
		
		List<BLATRecord> recordsList = Arrays.asList(br, br2);
		recordsList.sort(null);
		assertEquals(br, recordsList.get(0));
		
		/*
		 * create br2 again, this time with a lower score
		 */
		b = "47	8	0	0	0	0	3	8104	+	GL000219.1_179198_false_+	75	0	75	chrUn_gl000219	179198	169035	177214	4	24,5,5,41,	0,24,29,34,	169035,169081,177165,177173,";
		br2 = new BLATRecord.Builder(b).build();
		assertEquals(true, br2.isValid());
		assertEquals(36, br2.getScore());
		assertEquals("GL000219.1_179198_false_+", br2.getQName());
		
		recordsList = Arrays.asList(br, br2);
		recordsList.sort(null);
		assertEquals(br2, recordsList.get(0));
	}
	
	@Test
	public void sorting() {
		BLATRecord br1 = new BLATRecord.Builder("21\t0\t0\t0\t0\t0\t0\t0\t+\tname\t30\t0\t21\tchr8\t12345\t140578675\t140578696\t1\t21\t0\t140578675").build();
		BLATRecord br2 = new BLATRecord.Builder("23\t2\t0\t0\t1\t3\t2\t3\t-\tname\t30\t2\t30\tchr1\t12345\t28209783\t28209811\t3\t4,21,3\t2,6,27\t28209785,28209789,28209810").build();
		BLATRecord br3 = new BLATRecord.Builder("24\t1\t0\t0\t1\t4\t1\t5\t-\tname\t30\t0\t29\tchr6\t12345\t92780855\t92780885\t2\t4,25\t0,4\t92780855,92780859").build();
		BLATRecord br4 = new BLATRecord.Builder("21\t0\t0\t0\t1\t6\t1\t1\t-\tname\t30\t0\t27\tchr7\t12345\t106750037\t106750059\t2\t23,4\t0,23\t106750037,106750060").build();
		BLATRecord br5 = new BLATRecord.Builder("25\t1\t0\t0\t2\t2\t2\t5\t-\tname\t30\t1\t29\tchr9\t12345\t107229780\t107229811\t3\t22,1,5\t1,2,24\t107229781,107229782,107229804").build();
		BLATRecord br6 = new BLATRecord.Builder("25\t1\t0\t0\t2\t3\t1\t2\t-\tname\t30\t0\t29\tchr16\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410").build();
		List<BLATRecord> list = Arrays.asList(br1, br2, br3, br4, br5, br6);
		
		list.sort(null);
		assertEquals(br1, list.get(5));
		assertEquals(br2, list.get(0));
	}
	
	@Test
	public void sortingSameScore() {
		BLATRecord br1 = new BLATRecord.Builder("25\t1\t0\t0\t2\t3\t1\t2\t-\tname\t30\t0\t29\tchr16\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410").build();
		BLATRecord br2 = new BLATRecord.Builder("25\t1\t0\t0\t2\t2\t2\t5\t-\tname\t30\t1\t29\tchr9\t12345\t107229780\t107229811\t3\t22,1,5\t1,2,24\t107229781,107229782,107229804").build();
		BLATRecord br3 = new BLATRecord.Builder("25\t1\t0\t0\t2\t3\t1\t2\t+\tname\t30\t0\t29\tchr16\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410").build();
		BLATRecord br4 = new BLATRecord.Builder("25\t1\t0\t0\t2\t2\t2\t5\t+\tname\t30\t1\t29\tchr9\t12345\t107229780\t107229811\t3\t22,1,5\t1,2,24\t107229781,107229782,107229804").build();
		List<BLATRecord> list = Arrays.asList(br1, br2, br3, br4);
		
		list.sort(null);
		assertEquals(br2, list.get(0));
		assertEquals(br3, list.get(3));
	}
	
	@Test
	public void sortingDiffScoreSameEverythingElse() {
		BLATRecord br1 = new BLATRecord.Builder("21\t0\t0\t0\t0\t0\t0\t0\t-\tname\t30\t0\t29\tchr16\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410").build();
		BLATRecord br2 = new BLATRecord.Builder("22\t0\t0\t0\t0\t0\t0\t0\t-\tname\t30\t0\t29\tchr16\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410").build();
		BLATRecord br3 = new BLATRecord.Builder("23\t0\t0\t0\t0\t0\t0\t0\t-\tname\t30\t0\t29\tchr16\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410").build();
		BLATRecord br4 = new BLATRecord.Builder("24\t0\t0\t0\t0\t0\t0\t0\t-\tname\t30\t0\t29\tchr16\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410").build();
		BLATRecord br5 = new BLATRecord.Builder("25\t0\t0\t0\t0\t0\t0\t0\t-\tname\t30\t0\t29\tchr16\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410").build();
		BLATRecord br6 = new BLATRecord.Builder("26\t0\t0\t0\t0\t0\t0\t0\t-\tname\t30\t0\t29\tchr16\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410").build();
		List<BLATRecord> list = Arrays.asList(br1, br2, br3, br4, br5, br6);
		
		list.sort(null);
		assertEquals(br1, list.get(0));
		assertEquals(br6, list.get(5));
	}
	
	@Test
	public void realLifeSorting() {
		List<BLATRecord> list = Arrays.asList(new BLATRecord.Builder("56	1	0	0	0	0	0	0	+	chr15_34031839_true_-	64	1	58	chr1	249250621	40831199	40831256	1	57,	1,	40831199,").build()
		,new BLATRecord.Builder("56	1	0	0	0	0	0	0	+	chr15_34031839_true_-	64	6	63	chr1	249250621	45750240	45750297	1	57,	6,	45750240,").build()
		,new BLATRecord.Builder("61	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	1	64	chr1	249250621	59834595	59834658	1	63,	1,	59834595,").build()
		,new BLATRecord.Builder("57	1	0	0	0	0	0	0	+	chr15_34031839_true_-	64	2	60	chr1	249250621	60955804	60955862	1	58,	2,	60955804,").build()
		,new BLATRecord.Builder("61	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	1	64	chr1	249250621	119394974	119395037	1	63,	1,	119394974,").build()
		,new BLATRecord.Builder("62	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	64	chr1	249250621	146894945	146895009	1	64,	0,	146894945,").build()
		,new BLATRecord.Builder("53	0	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	53	chr1	249250621	160752870	160752923	1	53,	0,	160752870,").build()
		,new BLATRecord.Builder("62	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	64	chr10	135534747	10692820	10692884	1	64,	0,	10692820,").build()
		,new BLATRecord.Builder("62	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	64	chr10	135534747	86039723	86039787	1	64,	0,	86039723,").build()
		,new BLATRecord.Builder("62	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	64	chr10	135534747	111365204	111365268	1	64,	0,	111365204,").build()
		,new BLATRecord.Builder("62	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	64	chr10	135534747	113863683	113863747	1	64,	0,	113863683,").build()
		,new BLATRecord.Builder("63	1	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	64	chr10	135534747	127633806	127633870	1	64,	0,	127633806,").build()
		,new BLATRecord.Builder("53	0	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	53	chr10	135534747	128365040	128365093	1	53,	0,	128365040,").build()
		,new BLATRecord.Builder("58	1	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	59	chr11	135006516	38213290	38213349	1	59,	0,	38213290,").build()
		,new BLATRecord.Builder("62	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	64	chr11	135006516	92869804	92869868	1	64,	0,	92869804,").build()
		,new BLATRecord.Builder("61	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	1	64	chr11	135006516	95169380	95169443	1	63,	1,	95169380,").build()
		,new BLATRecord.Builder("61	3	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	64	chr11	135006516	108244261	108244325	1	64,	0,	108244261,").build()
		,new BLATRecord.Builder("56	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	58	chr11	135006516	108131353	108131411	1	58,	0,	108131353,").build()
		,new BLATRecord.Builder("58	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	4	64	chr11	135006516	132189030	132189090	1	60,	4,	132189030,").build()
		,new BLATRecord.Builder("58	1	0	0	0	0	0	0	+	chr15_34031839_true_-	64	1	60	chr12	133851895	11450590	11450649	1	59,	1,	11450590,").build()
		,new BLATRecord.Builder("61	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	1	64	chr12	133851895	73788589	73788652	1	63,	1,	73788589,").build()
		,new BLATRecord.Builder("62	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	64	chr13	115169878	30833495	30833559	1	64,	0,	30833495,").build()
		,new BLATRecord.Builder("61	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	1	64	chr2	243199373	19505600	19505663	1	63,	1,	19505600,").build()
		,new BLATRecord.Builder("61	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	1	64	chr2	243199373	43216615	43216678	1	63,	1,	43216615,").build()
		,new BLATRecord.Builder("58	1	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	59	chr2	243199373	60416158	60416217	1	59,	0,	60416158,").build()
		,new BLATRecord.Builder("62	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	64	chr2	243199373	63062613	63062677	1	64,	0,	63062613,").build()
		,new BLATRecord.Builder("53	0	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	53	chr2	243199373	72291105	72291158	1	53,	0,	72291105,").build()
		,new BLATRecord.Builder("61	3	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	64	chr2	243199373	83315928	83315992	1	64,	0,	83315928,").build()
		,new BLATRecord.Builder("62	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	64	chr2	243199373	144618525	144618589	1	64,	0,	144618525,").build()
		,new BLATRecord.Builder("53	0	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	53	chr2	243199373	154575960	154576013	1	53,	0,	154575960,").build()
		,new BLATRecord.Builder("62	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	64	chr2	243199373	159379129	159379193	1	64,	0,	159379129,").build()
		,new BLATRecord.Builder("55	1	0	0	0	0	0	0	+	chr15_34031839_true_-	64	3	59	chr2	243199373	159134650	159134706	1	56,	3,	159134650,").build()
		,new BLATRecord.Builder("62	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	64	chr2	243199373	184714250	184714314	1	64,	0,	184714250,").build()
		,new BLATRecord.Builder("62	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	64	chr2	243199373	186729324	186729388	1	64,	0,	186729324,").build()
		,new BLATRecord.Builder("62	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	64	chr2	243199373	220796539	220796603	1	64,	0,	220796539,").build()
		,new BLATRecord.Builder("58	1	0	0	0	0	0	0	+	chr15_34031839_true_-	64	1	60	chr3	198022430	851059	851118	1	59,	1,	851059,").build()
		,new BLATRecord.Builder("55	0	0	0	0	0	0	0	+	chr15_34031839_true_-	64	0	55	chr3	198022430	1429244	1429299	1	55,	0,	1429244,").build()
		,new BLATRecord.Builder("60	2	0	0	0	0	0	0	+	chr15_34031839_true_-	64	2	64	chr3	198022430	4004760	4004822	1	62,	2,	4004760,").build());
		
		list.sort(null);
		BLATRecord br = list.get(list.size() - 1);
		assertEquals(62, br.getScore());
		assertEquals("chr10", br.getTName());
		assertEquals('+', br.getStrand());
		assertEquals(127633806 + 1, br.getStartPos());
	}
	
	@Test
	public void sortingSameScoreAndStrand() {
		BLATRecord br1 = new BLATRecord.Builder("25\t1\t0\t0\t2\t3\t1\t2\t-\tname\t30\t0\t29\tchr1\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410").build();
		BLATRecord br2 = new BLATRecord.Builder("25\t1\t0\t0\t2\t3\t1\t2\t-\tname\t30\t0\t29\tchr16\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410").build();
		List<BLATRecord> list = Arrays.asList(br1, br2);
		list.sort(null);
		assertEquals(br1, list.get(1));
		
		br1 = new BLATRecord.Builder("25\t1\t0\t0\t2\t3\t1\t2\t+\tname\t30\t0\t29\tchr1\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410").build();
		br2 = new BLATRecord.Builder("25\t1\t0\t0\t2\t3\t1\t2\t+\tname\t30\t0\t29\tchr16\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410").build();
		list = Arrays.asList(br1, br2);
		list.sort(null);
		assertEquals(br1, list.get(1));
		
		br1 = new BLATRecord.Builder("25\t1\t0\t0\t2\t3\t1\t2\t-\tname\t30\t0\t29\tchr16\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410").build();
		br2 = new BLATRecord.Builder("25\t1\t0\t0\t2\t3\t1\t2\t+\tname\t30\t0\t29\tchr16\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410").build();
		list = Arrays.asList(br1, br2);
		list.sort(null);
		assertEquals(br2, list.get(1));
		
		br1 = new BLATRecord.Builder("25\t1\t0\t0\t2\t3\t1\t2\t-\tname\t30\t0\t29\tchr16\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410").build();
		br2 = new BLATRecord.Builder("25\t1\t0\t0\t2\t3\t1\t2\t-\tname\t30\t0\t29\tchr9\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410").build();
		list = Arrays.asList(br1, br2);
		list.sort(null);
		assertEquals(br1, list.get(1));
		
		br1 = new BLATRecord.Builder("25\t1\t0\t0\t2\t3\t1\t2\t-\tname\t30\t0\t29\tchr16\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410").build();
		br2 = new BLATRecord.Builder("26\t1\t0\t0\t2\t3\t1\t2\t-\tname\t30\t0\t29\tchr9\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410").build();
		list = Arrays.asList(br1, br2);
		list.sort(null);
		assertEquals(br2, list.get(1));
		
		br1 = new BLATRecord.Builder("27\t1\t0\t0\t2\t3\t1\t2\t-\tname\t30\t0\t29\tchr16\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410").build();
		br2 = new BLATRecord.Builder("26\t1\t0\t0\t2\t3\t1\t2\t-\tname\t30\t0\t29\tchr16\t12345\t23903405\t23903433\t2\t5,24\t0,5\t23903405,23903410").build();
		list = Arrays.asList(br1, br2);
		list.sort(null);
		assertEquals(br1, list.get(1));
	}
	
	@Test
	public void ctor() {
		String ctor = "41\t1\t0\t0\t1\t4\t1\t5\t-\t12345_123455_12345\t70\t14\t60\tchr9\t141213431\t81359142\t81359189\t2\t17,25,\t10,31,\t81359142,81359164,";
		BLATRecord br = new BLATRecord.Builder(ctor).build();
		assertEquals(true, br.isValid());
		assertEquals(41 - 1 - 1 - 1, br.getScore());
		assertEquals("12345_123455_12345", br.getQName());
		assertEquals("chr9", br.getTName());
		assertEquals(2, br.getBlockCount());
		assertArrayEquals(new int[] {81359143,81359165}, br.getTStarts());
		assertEquals(MINUS, br.getStrand());
		assertEquals(81359142 + 1, br.getStartPos());
		assertEquals(81359189, br.getEndPos());
		assertEquals(14 + 1, br.getQueryStart());
		assertEquals(60, br.getQueryEnd());
		assertEquals(70, br.getSize());
		assertArrayEquals(new int[] {17,25}, br.getBlockSizes());
	}
	
	@Test
	public void unmodifiedStarts() {
		String ctor = "41\t1\t0\t0\t1\t4\t1\t5\t-\t12345_123455_12345\t70\t14\t60\tchr9\t141213431\t81359142\t81359189\t2\t17,25,\t10,31,\t81359142,81359164,";
		BLATRecord br = new BLATRecord.Builder(ctor).build();
		assertEquals(true, br.isValid());
		int i = 70 - 17 - 10 + 1;
		int j = 70 - 25 - 31 + 1;
		assertArrayEquals(new int[] {i ,j }, br.getUnmodifiedStarts());
		
		/*
		 * flip strand - should get different results
		 */
		ctor = "41\t1\t0\t0\t1\t4\t1\t5\t+\t12345_123455_12345\t70\t14\t60\tchr9\t141213431\t81359142\t81359189\t2\t17,25,\t10,31,\t81359142,81359164,";
		br = new BLATRecord.Builder(ctor).build();
		assertEquals(true, br.isValid());
		i = 10 + 1;
		j = 31 + 1;
		assertArrayEquals(new int[] {i ,j}, br.getUnmodifiedStarts());
	}
}
