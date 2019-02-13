package org.qcmg.snp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.Accumulator;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.Rule;
import org.qcmg.common.util.AccumulatorUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.Pair;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

public class PipelineUtilTest {
	
	public static final List<Rule> cRules = Arrays.asList(new Rule(0,20,3), new Rule(21,50,4), new Rule(51,Integer.MAX_VALUE,10));
	public static final List<Rule> tRules = Arrays.asList(new Rule(0,20,3), new Rule(21,50,4), new Rule(51,Integer.MAX_VALUE,5));
	
	
	@Test
	public void getLoLoRecs() {
		assertEquals(null, PipelineUtil.listOfListOfAdjacentVcfs(null));
		assertEquals(true, PipelineUtil.listOfListOfAdjacentVcfs(new ArrayList<VcfRecord>()).isEmpty());
	}
	
	
	@Test
	public void realLifeException() {
		/*
		 * 10:46:08.032 [main] INFO org.qcmg.snp.StandardPipeline - in compound snp
10:46:22.145 [main] WARNING org.qcmg.snp.util.PipelineUtil - Accumulator objects are not in sequence!:
10:46:22.146 [main] WARNING org.qcmg.snp.util.PipelineUtil - 135495579:
10:46:22.151 [main] WARNING org.qcmg.snp.util.PipelineUtil - 135495580:
10:46:22.151 [main] WARNING org.qcmg.snp.util.PipelineUtil - 135495581:
10:46:22.151 [main] WARNING org.qcmg.snp.util.PipelineUtil - 135495582:
10:46:22.151 [main] WARNING org.qcmg.snp.util.PipelineUtil - 135495584:
10:46:22.151 [main] WARNING org.qcmg.snp.util.PipelineUtil - 135495585:
Exception in thread "main" java.lang.IllegalArgumentException: List of Accumulator objects are not in sequence!
       	at org.qcmg.snp.util.PipelineUtil.getBasesFromAccumulators(PipelineUtil.java:357)
       	at org.qcmg.snp.util.PipelineUtil.createCompoundSnp(PipelineUtil.java:672)
       	at org.qcmg.snp.Pipeline.compoundSnps(Pipeline.java:1843)
       	at org.qcmg.snp.StandardPipeline.<init>(StandardPipeline.java:37)
       	at org.qcmg.snp.Main.setup(Main.java:129)
       	at org.qcmg.snp.Main.main(Main.java:45)
		 */
		List<VcfRecord> l = Arrays.asList( VcfUtils.createVcfRecord("1", 135495579)
				, VcfUtils.createVcfRecord("1", 135495580)
				, VcfUtils.createVcfRecord("1", 135495581)
				, VcfUtils.createVcfRecord("1", 135495582)
				, VcfUtils.createVcfRecord("1", 135495584)
				, VcfUtils.createVcfRecord("1", 135495585)
				);
		
		/*
		 * this should be 2 lists
		 */
		List<List<VcfRecord>> loloRecs = PipelineUtil.listOfListOfAdjacentVcfs(l);
		assertEquals(2, loloRecs.size());
	}
	
	@Test
	public void realLifeException2() {
		/*
		 * 08:17:22.382 [main] WARNING org.qcmg.snp.util.PipelineUtil - Accumulator objects are not in sequence!:
08:17:22.387 [main] WARNING org.qcmg.snp.util.PipelineUtil - 8046419:T
08:17:22.388 [main] WARNING org.qcmg.snp.util.PipelineUtil - 8046421:TTtttt
Exception in thread "main" java.lang.IllegalArgumentException: List of Accumulator objects are not in sequence!
        at org.qcmg.snp.util.PipelineUtil.getBasesFromAccumulators(PipelineUtil.java:363)
        at org.qcmg.snp.util.PipelineUtil.createCompoundSnp(PipelineUtil.java:724)
        at org.qcmg.snp.Pipeline.compoundSnps(Pipeline.java:1870)
        at org.qcmg.snp.VcfPipeline.<init>(VcfPipeline.java:164)
        at org.qcmg.snp.Main.setup(Main.java:168)
        at org.qcmg.snp.Main.main(Main.java:45)
		 */
		List<VcfRecord> l = Arrays.asList( VcfUtils.createVcfRecord("1", 8046419)
				, VcfUtils.createVcfRecord("1", 8046421)
				);
		
		/*
		 * this should be 0 lists
		 */
		List<List<VcfRecord>> loloRecs = PipelineUtil.listOfListOfAdjacentVcfs(l);
		assertEquals(0, loloRecs.size());
	}
	
	@Test
	public void getLoLoRecsDiffChr() {
		List<VcfRecord> snps = new ArrayList<>();
		snps.add(VcfUtils.createVcfRecord("1", 100));
		snps.add(VcfUtils.createVcfRecord("2", 100));
		snps.add(VcfUtils.createVcfRecord("3", 100));
		
		assertEquals(true, PipelineUtil.listOfListOfAdjacentVcfs(snps).isEmpty());
	}
	
	@Test
	public void getLoLoRecsDiffClassification() throws Exception {
		VcfRecord v1 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 100),null,"A","C");
		VcfRecord v2 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 101),null,"C","G");
		v2.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		
		/*
		 * 1 som 1 germ
		 */
		List<VcfRecord> snps = new ArrayList<>();
		snps.add(v1);
		snps.add(v2);
		
		assertEquals(true, PipelineUtil.listOfListOfAdjacentVcfs(snps).isEmpty());
		/*
		 * both som
		 */
		v1.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		assertEquals(1, PipelineUtil.listOfListOfAdjacentVcfs(snps).size());
		/*
		 * reset to both germ
		 */
		v1 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 100),null,"A","C");
		v2 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 101),null,"C","G");
		snps = new ArrayList<>();
		snps.add(v1);
		snps.add(v2);
		assertEquals(1, PipelineUtil.listOfListOfAdjacentVcfs(snps).size());
		/*
		 * 1 germ 1 som
		 */
		v1.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		assertEquals(true, PipelineUtil.listOfListOfAdjacentVcfs(snps).isEmpty());
	}
	
	@Test
	public void getUniques() {
		TIntIntMap m  = new TIntIntHashMap();
		TIntList l  = new TIntArrayList();
		assertEquals(0, PipelineUtil.getUniqueCount(m, l));
		m.put(100, 10);
		assertEquals(0, PipelineUtil.getUniqueCount(m, l));
		l.add(100);
		assertEquals(1, PipelineUtil.getUniqueCount(m, l));
		l.add(101);
		assertEquals(1, PipelineUtil.getUniqueCount(m, l));
	}
	
	@Test
	public void getLoLoRecsSameChrLongWayAway() {
		List<VcfRecord> snps = new ArrayList<>();
		snps.add(VcfUtils.createVcfRecord("1", 100));
		snps.add(VcfUtils.createVcfRecord("1", 200));
		snps.add(VcfUtils.createVcfRecord("1", 300));
		
		assertEquals(true, PipelineUtil.listOfListOfAdjacentVcfs(snps).isEmpty());
	}
	@Test
	public void getLoLoRecsSameChrShortWayAway() {
		List<VcfRecord> snps = new ArrayList<>();
		snps.add(VcfUtils.createVcfRecord("1", 100));
		snps.add(VcfUtils.createVcfRecord("1", 102));
		snps.add(VcfUtils.createVcfRecord("1", 104));
		snps.add(VcfUtils.createVcfRecord("1", 106));
		snps.add(VcfUtils.createVcfRecord("1", 108));
		
		assertEquals(true, PipelineUtil.listOfListOfAdjacentVcfs(snps).isEmpty());
	}
	
	@Test
	public void getLoLoRecsSameChr() {
		List<VcfRecord> snps = new ArrayList<>();
		snps.add(VcfUtils.createVcfRecord("1", 100));
		snps.add(VcfUtils.createVcfRecord("1", 101));
		snps.add(VcfUtils.createVcfRecord("1", 300));
		
		assertEquals(1, PipelineUtil.listOfListOfAdjacentVcfs(snps).size());
		assertEquals(2, PipelineUtil.listOfListOfAdjacentVcfs(snps).get(0).size());
	}
	@Test
	public void getLoLoRecsSameChr2() {
		List<VcfRecord> snps = new ArrayList<>();
		snps.add(VcfUtils.createVcfRecord("1", 100));
		snps.add(VcfUtils.createVcfRecord("1", 101));
		snps.add(VcfUtils.createVcfRecord("1", 102));
		snps.add(VcfUtils.createVcfRecord("1", 103));
		snps.add(VcfUtils.createVcfRecord("1", 104));
		snps.add(VcfUtils.createVcfRecord("1", 300));
		
		assertEquals(1, PipelineUtil.listOfListOfAdjacentVcfs(snps).size());
		assertEquals(5, PipelineUtil.listOfListOfAdjacentVcfs(snps).get(0).size());
	}
	
	@Test
	public void getLoLoRecsSameChr3() {
		List<VcfRecord> snps = new ArrayList<>();
		snps.add(VcfUtils.createVcfRecord("1", 100));
		snps.add(VcfUtils.createVcfRecord("1", 101));
		snps.add(VcfUtils.createVcfRecord("1", 102));
		snps.add(VcfUtils.createVcfRecord("1", 103));
		snps.add(VcfUtils.createVcfRecord("1", 104));
		snps.add(VcfUtils.createVcfRecord("1", 105));
		
		assertEquals(1, PipelineUtil.listOfListOfAdjacentVcfs(snps).size());
		assertEquals(6, PipelineUtil.listOfListOfAdjacentVcfs(snps).get(0).size());
	}
	
	@Test
	public void getLoLoRecsSameChr4() {
		List<VcfRecord> snps = new ArrayList<>();
		snps.add(VcfUtils.createVcfRecord("1", 99));
		snps.add(VcfUtils.createVcfRecord("1", 101));
		snps.add(VcfUtils.createVcfRecord("1", 102));
		snps.add(VcfUtils.createVcfRecord("1", 103));
		snps.add(VcfUtils.createVcfRecord("1", 104));
		snps.add(VcfUtils.createVcfRecord("1", 106));
		
		assertEquals(1, PipelineUtil.listOfListOfAdjacentVcfs(snps).size());
		assertEquals(4, PipelineUtil.listOfListOfAdjacentVcfs(snps).get(0).size());
	}
	
	@Test
	public void getLoLoRecsSameChr5() {
		List<VcfRecord> snps = new ArrayList<>();
		snps.add(VcfUtils.createVcfRecord("1", 99));
		snps.add(VcfUtils.createVcfRecord("1", 101));
		snps.add(VcfUtils.createVcfRecord("1", 102));
		snps.add(VcfUtils.createVcfRecord("1", 104));
		snps.add(VcfUtils.createVcfRecord("1", 106));
		snps.add(VcfUtils.createVcfRecord("1", 107));
		snps.add(VcfUtils.createVcfRecord("1", 108));
		snps.add(VcfUtils.createVcfRecord("1", 109));
		snps.add(VcfUtils.createVcfRecord("1", 111));
		
		assertEquals(2, PipelineUtil.listOfListOfAdjacentVcfs(snps).size());
		assertEquals(2, PipelineUtil.listOfListOfAdjacentVcfs(snps).get(0).size());
		assertEquals(4, PipelineUtil.listOfListOfAdjacentVcfs(snps).get(1).size());
	}
	
	@Test
	public void getLoLoRecsSameChr6() {
		List<VcfRecord> snps = new ArrayList<>();
		snps.add(VcfUtils.createVcfRecord("1", 100));
		snps.add(VcfUtils.createVcfRecord("1", 101));
		snps.add(VcfUtils.createVcfRecord("1", 102));
		snps.add(VcfUtils.createVcfRecord("1", 104));
		snps.add(VcfUtils.createVcfRecord("1", 106));
		snps.add(VcfUtils.createVcfRecord("1", 107));
		snps.add(VcfUtils.createVcfRecord("1", 108));
		snps.add(VcfUtils.createVcfRecord("1", 109));
		snps.add(VcfUtils.createVcfRecord("1", 110));
		
		assertEquals(2, PipelineUtil.listOfListOfAdjacentVcfs(snps).size());
		assertEquals(3, PipelineUtil.listOfListOfAdjacentVcfs(snps).get(0).size());
		assertEquals(5, PipelineUtil.listOfListOfAdjacentVcfs(snps).get(1).size());
	}
	
	@Test
	public void getRef() {
		List<VcfRecord> snps = new ArrayList<>();
		snps.add(VcfUtils.createVcfRecord(new ChrPointPosition("1", 100), null, "A", null));
		snps.add(VcfUtils.createVcfRecord(new ChrPointPosition("1", 101), null, "B", null));
		snps.add(VcfUtils.createVcfRecord(new ChrPointPosition("1", 102), null, "C", null));
		
		assertEquals("ABC", PipelineUtil.getReference(snps).get());
	}
	
	@Test
	public void getMR() {
		Accumulator acc1 = new Accumulator(150);
		acc1.addBase((byte)'G', (byte) 1, true, 100, 150, 200, 1);
		acc1.addBase((byte)'G', (byte) 1, true, 100, 150, 200, 2);
		acc1.addBase((byte)'G', (byte) 1, false, 100, 150, 200, 3);
		List<Accumulator> accs = Arrays.asList(acc1);
		
		Map<String, short[]> basesAndCounts = PipelineUtil.getBasesFromAccumulators(accs);
		/*
		 * coverage
		 */
		assertEquals(2, basesAndCounts.get("G")[0]);
		assertEquals(1, basesAndCounts.get("G")[2]);
		/*
		 * nns
		 */
		assertEquals(1, basesAndCounts.get("G")[1]);
		assertEquals(1, basesAndCounts.get("G")[3]);
		
		assertArrayEquals(new String[]{".","."}, PipelineUtil.getMR(basesAndCounts, new String[]{"T"}, 0,0));
		assertArrayEquals(new String[]{".","."},  PipelineUtil.getMR(basesAndCounts, new String[]{"A","T"}, 0,0));
		assertArrayEquals(new String[]{".","."}, PipelineUtil.getMR(basesAndCounts, new String[]{"A","C","T"}, 0,0));
		assertArrayEquals(new String[]{"3","2"}, PipelineUtil.getMR(basesAndCounts, new String[]{"G"}, 1,1));
		assertArrayEquals(new String[]{"3","2"}, PipelineUtil.getMR(basesAndCounts, new String[]{"G","T"}, 1,1));
		assertArrayEquals(new String[]{"3","2"}, PipelineUtil.getMR(basesAndCounts, new String[]{"C","G","T"}, 2,2));
		assertArrayEquals(new String[]{".,3",".,2"}, PipelineUtil.getMR(basesAndCounts, new String[]{"C","G","T"}, 1,2));
	}
	
	@Test
	public void getCount() {
		Accumulator acc1 = new Accumulator(1);
		acc1.addBase((byte)'G', (byte) 1, true, 1, 1, 2, 1);
		acc1.addBase((byte)'G', (byte) 1, true, 1, 1, 2, 2);
		acc1.addBase((byte)'G', (byte) 1, false, 1, 1, 2, 3);
//		Accumulator acc2 = new Accumulator(2);
//		acc2.addBase((byte)'A', (byte) 1, true, 2, 2, 3, 1);
		List<Accumulator> accs = Arrays.asList(acc1);
		
		Map<String, short[]> basesAndCounts = PipelineUtil.getBasesFromAccumulators(accs);
		assertEquals(3, PipelineUtil.getCount(basesAndCounts, "G", 0));
		
		acc1.addBase((byte)'T', (byte) 1, false, 1, 1, 2, 4);
		acc1.addBase((byte)'T', (byte) 1, true, 1, 1, 2, 5);
		basesAndCounts = PipelineUtil.getBasesFromAccumulators(accs);
		assertEquals(3, PipelineUtil.getCount(basesAndCounts, "G", 0));
		assertEquals(2, PipelineUtil.getCount(basesAndCounts, "T", 0));
	}
	
	@Test
	public void nonAdjacentAccums() {
		Accumulator acc1 = new Accumulator(1);
		Accumulator acc4 = new Accumulator(4);
		Accumulator acc5 = new Accumulator(5);
		
		List<Accumulator> accs = Arrays.asList(acc1, acc4,acc5);
		
		try {
			PipelineUtil.getBasesFromAccumulators(accs);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae){};
		
		assertEquals(true, PipelineUtil.getBasesFromAccumulators(null).isEmpty());
		assertEquals(true, PipelineUtil.getBasesFromAccumulators(new ArrayList<>()).isEmpty());
	}
	
	@Test
	public void getAltsAndGTsNullInputs() {
		try {
			PipelineUtil.getAltStringAndGenotypes(null, null, null);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae){}
		try {
			PipelineUtil.getAltStringAndGenotypes(null, null, "");
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae){}
		
		List<String> altsGTs = PipelineUtil.getAltStringAndGenotypes(null, null, "A");
		assertEquals(3, altsGTs.size());
		assertEquals(Constants.MISSING_DATA_STRING, altsGTs.get(0));
		assertEquals(Constants.MISSING_GT, altsGTs.get(1));
		assertEquals(Constants.MISSING_GT, altsGTs.get(2));
		
		List<String> control = Arrays.asList("ABC");
		
		altsGTs = PipelineUtil.getAltStringAndGenotypes(control, null, "XYZ");
		assertEquals(3, altsGTs.size());
		assertEquals("ABC", altsGTs.get(0));
		assertEquals("1/1", altsGTs.get(1));
		assertEquals(Constants.MISSING_GT, altsGTs.get(2));
		
		List<String> test = Arrays.asList("123");
		
		altsGTs = PipelineUtil.getAltStringAndGenotypes(null, test, "XYZ");
		assertEquals(3, altsGTs.size());
		assertEquals("123", altsGTs.get(0));
		assertEquals(Constants.MISSING_GT, altsGTs.get(1));
		assertEquals("1/1", altsGTs.get(2));
	}
	
//	@Test
//	public void csFilter() {
//		assertEquals(".", PipelineUtil.getCSFilters(null, -1, -1, 0, null, 3, 3, true, true));
//		assertEquals(".", PipelineUtil.getCSFilters(null, -1, -1, 0, null, 3, 3, true, false));
//	}
	
	@Test
	public void getAltsAndGTs() {
		List<String> control = Arrays.asList("ABC");
		List<String> test = Arrays.asList("ABC");
		
		List<String> altsGTs = PipelineUtil.getAltStringAndGenotypes(control, test, "XYZ");
		assertEquals(3, altsGTs.size());
		assertEquals("ABC", altsGTs.get(0));
		assertEquals("1/1", altsGTs.get(1));
		assertEquals("1/1", altsGTs.get(2));
		
		control = Arrays.asList("XYZ");
		test = Arrays.asList("ABC");
		altsGTs = PipelineUtil.getAltStringAndGenotypes(control, test, "XYZ");
		assertEquals(3, altsGTs.size());
		assertEquals("ABC", altsGTs.get(0));
		assertEquals("0/0", altsGTs.get(1));
		assertEquals("1/1", altsGTs.get(2));
		
		control = Arrays.asList("XYZ");
		test = Arrays.asList("XYZ");
		altsGTs = PipelineUtil.getAltStringAndGenotypes(control, test, "XYZ");
		assertEquals(3, altsGTs.size());
		assertEquals(Constants.MISSING_DATA_STRING, altsGTs.get(0));
		assertEquals("0/0", altsGTs.get(1));
		assertEquals("0/0", altsGTs.get(2));
		
		control = Arrays.asList("XYZ", "POP");
		test = Arrays.asList("XYZ", "POP");
		altsGTs = PipelineUtil.getAltStringAndGenotypes(control, test, "XYZ");
		assertEquals(3, altsGTs.size());
		assertEquals("POP", altsGTs.get(0));
		assertEquals("0/1", altsGTs.get(1));
		assertEquals("0/1", altsGTs.get(2));
		
		control = Arrays.asList("BAB", "POP");
		test = Arrays.asList("XYZ", "POP");
		altsGTs = PipelineUtil.getAltStringAndGenotypes(control, test, "XYZ");
		assertEquals(3, altsGTs.size());
		assertEquals("BAB,POP", altsGTs.get(0));
		assertEquals("1/2", altsGTs.get(1));
		assertEquals("0/2", altsGTs.get(2));
		
		control = Arrays.asList("BAB", "POP");
		test = Arrays.asList("BAB", "POP");
		altsGTs = PipelineUtil.getAltStringAndGenotypes(control, test, "XYZ");
		assertEquals(3, altsGTs.size());
		assertEquals("BAB,POP", altsGTs.get(0));
		assertEquals("1/2", altsGTs.get(1));
		assertEquals("1/2", altsGTs.get(2));
		
		control = Arrays.asList("BAB", "POP");
		test = Arrays.asList("QWE", "POP");
		altsGTs = PipelineUtil.getAltStringAndGenotypes(control, test, "XYZ");
		assertEquals(3, altsGTs.size());
		assertEquals("BAB,POP,QWE", altsGTs.get(0));
		assertEquals("1/2", altsGTs.get(1));
		assertEquals("2/3", altsGTs.get(2));
		
		control = Arrays.asList("BAB", "LAT");
		test = Arrays.asList("QWE", "POP");
		altsGTs = PipelineUtil.getAltStringAndGenotypes(control, test, "XYZ");
		assertEquals(3, altsGTs.size());
		assertEquals("BAB,LAT,QWE,POP", altsGTs.get(0));
		assertEquals("1/2", altsGTs.get(1));
		assertEquals("3/4", altsGTs.get(2));
	}
	
	@Test
	public void csAltsCantContainRef() {
		/*
		 * The following cs is not a cs!
		 * chr1    985449  .       GG      AG      .       .       .       GT:DP:MR:OABS   1/1:12:9:AG1[]8[];GA2[]0[];_G1[]0[]     1/1:10:6:AG3[]3[];GA2[]1[];_G1[]0[]
		 */
		List<String> control = Arrays.asList("AG");
		List<String> test = Arrays.asList("AG");
		
		List<String> altsGTs = PipelineUtil.getAltStringAndGenotypes(control, test, "GG");
		assertEquals(3, altsGTs.size());
		assertEquals(Constants.MISSING_DATA_STRING, altsGTs.get(0));
		assertEquals(Constants.MISSING_GT, altsGTs.get(1));
		assertEquals(Constants.MISSING_GT, altsGTs.get(2));
	}
	
	@Test
	public void getEmptyOABS() {
		assertEquals(false, PipelineUtil.getOABS(null).isPresent());
		Map<String, short[]> basesAndCounts = new HashMap<>();
		assertEquals(false, PipelineUtil.getOABS(basesAndCounts).isPresent());
		
		basesAndCounts.put("XYZ", new short[]{});
		assertEquals(false, PipelineUtil.getOABS(basesAndCounts).isPresent());
		
		basesAndCounts.put("XYZ", new short[4]);
		assertEquals(true, PipelineUtil.getOABS(basesAndCounts).isPresent());
		assertEquals("XYZ0[]0[]", PipelineUtil.getOABS(basesAndCounts).get());
		
		basesAndCounts.get("XYZ")[0] = 1;
		basesAndCounts.get("XYZ")[2] = 1;
		assertEquals("XYZ1[]1[]", PipelineUtil.getOABS(basesAndCounts).get());
	}
	
	@Test
	public void getOABS() {
		Map<String, short[]> basesAndCounts = new HashMap<>();
		
		basesAndCounts.put("XYZ", new short[]{10,5,15,6});
		assertEquals("XYZ10[]15[]", PipelineUtil.getOABS(basesAndCounts).get());
		basesAndCounts.put("ABC", new short[]{21,2,14,1});
		assertEquals("ABC21[]14[];XYZ10[]15[]", PipelineUtil.getOABS(basesAndCounts).get());
	}
	
	@Test
	public void getBasesForGentype() {
		Map<String, short[]> basesAndCounts = new HashMap<>();
		basesAndCounts.put("XYZ", new short[]{10,3,11,10});
		assertEquals(1, PipelineUtil.getBasesForGenotype(basesAndCounts, 10,"AAA").size());
		assertEquals("XYZ", PipelineUtil.getBasesForGenotype(basesAndCounts, 10,"AAA").get(0));
		basesAndCounts.put("ABC", new short[]{5,2,11,10});
		assertEquals(1, PipelineUtil.getBasesForGenotype(basesAndCounts, 10,"AAA").size());
		assertEquals("XYZ", PipelineUtil.getBasesForGenotype(basesAndCounts, 10,"AAA").get(0));
		assertEquals(2, PipelineUtil.getBasesForGenotype(basesAndCounts, 10,"DDD").size());
		assertEquals("XYZ", PipelineUtil.getBasesForGenotype(basesAndCounts, 10,"DDD").get(0));
		assertEquals("ABC", PipelineUtil.getBasesForGenotype(basesAndCounts, 10,"DDD").get(1));
		basesAndCounts.put("HBH", new short[]{0,0,16,1});
		assertEquals(2, PipelineUtil.getBasesForGenotype(basesAndCounts, 10,"AAA").size());
		assertEquals("XYZ", PipelineUtil.getBasesForGenotype(basesAndCounts, 10,"AAA").get(0));
		assertEquals("HBH", PipelineUtil.getBasesForGenotype(basesAndCounts, 10,"AAA").get(1));
	}
	
	@Test
	public void getBasesFromAccs() {
		Accumulator acc1 = new Accumulator(100);
		acc1.addBase((byte)'G', (byte) 1, true, 100, 100, 200, 1);
		acc1.addBase((byte)'G', (byte) 1, true, 100, 100, 200, 2);
		acc1.addBase((byte)'G', (byte) 1, false, 100, 100, 200, 3);
		Accumulator acc2 = new Accumulator(101);
		acc2.addBase((byte)'A', (byte) 1, true, 100, 101, 200, 1);
		acc2.addBase((byte)'A', (byte) 1, true, 100, 101, 200, 2);
		acc2.addBase((byte)'A', (byte) 1, false, 100, 101, 200, 3);
		List<Accumulator> accs = Arrays.asList(acc1, acc2);
		
		Map<String, short[]> basesAndCounts = PipelineUtil.getBasesFromAccumulators(accs);
		assertEquals(1, basesAndCounts.size());
		Assert.assertArrayEquals(new short[]{2,1,1,1}, basesAndCounts.get("GA"));
		
		acc1.addBase((byte)'C', (byte) 1, false, 100, 100, 201, 4);
		basesAndCounts = PipelineUtil.getBasesFromAccumulators(accs);
		assertEquals(2, basesAndCounts.size());
		Assert.assertArrayEquals(new short[]{2,1,1,1}, basesAndCounts.get("GA"));
		Assert.assertArrayEquals(new short[]{0,0,1,1}, basesAndCounts.get("C_"));
		
		acc1.addBase((byte)'C', (byte) 1, false, 100, 100, 199, 6);
		basesAndCounts = PipelineUtil.getBasesFromAccumulators(accs);
		assertEquals(2, basesAndCounts.size());
		Assert.assertArrayEquals(new short[]{2,1,1,1}, basesAndCounts.get("GA"));
		Assert.assertArrayEquals(new short[]{0,0,2,2}, basesAndCounts.get("C_"));
		
		acc2.addBase((byte)'T', (byte) 1, true, 101, 101, 200, 5);
		basesAndCounts = PipelineUtil.getBasesFromAccumulators(accs);
		assertEquals(3, basesAndCounts.size());
		Assert.assertArrayEquals(new short[]{2,1,1,1}, basesAndCounts.get("GA"));
		Assert.assertArrayEquals(new short[]{0,0,2,2}, basesAndCounts.get("C_"));
		Assert.assertArrayEquals(new short[]{1,1,0,0}, basesAndCounts.get("_T"));
		
		Accumulator acc3 = new Accumulator(102);
		acc3.addBase((byte)'C', (byte) 1, true, 100, 102, 200, 1);
		acc3.addBase((byte)'C', (byte) 1, true, 100, 102, 200, 2);
		acc3.addBase((byte)'C', (byte) 1, true,101, 102, 200, 5);
		accs = Arrays.asList(acc1, acc2, acc3);
		
		basesAndCounts = PipelineUtil.getBasesFromAccumulators(accs);
		assertEquals(4, basesAndCounts.size());
		Assert.assertArrayEquals(new short[]{2,1,0,0}, basesAndCounts.get("GAC"));
		Assert.assertArrayEquals(new short[]{0,0,1,1}, basesAndCounts.get("GA_"));
		Assert.assertArrayEquals(new short[]{0,0,2,2}, basesAndCounts.get("C__"));
		Assert.assertArrayEquals(new short[]{1,1,0,0}, basesAndCounts.get("_TC"));
	}
	
	@Test
	public void createCSSinglePos() {
//		List<VcfRecord> snps = new ArrayList<>();
//		snps.add(VcfUtils.createVcfRecord(new ChrPointPosition("1", 100),null,"A","C"));
		VcfRecord origV = VcfUtils.createVcfRecord(new ChrPointPosition("1", 100),null,"A","C");
		
		Accumulator controlAcc100 = new Accumulator(100);
		controlAcc100.addBase((byte)'A', (byte) 1, true, 100, 100, 200, 1);
		controlAcc100.addBase((byte)'A', (byte) 1, true, 100, 100, 200, 2);
		controlAcc100.addBase((byte)'A', (byte) 1, false, 100, 100, 200, 3);
		
		Accumulator testAcc100 = new Accumulator(100);
		testAcc100.addBase((byte)'C', (byte) 1, true, 100, 100, 200, 1);
		testAcc100.addBase((byte)'C', (byte) 1, true, 100, 100, 200, 2);
		testAcc100.addBase((byte)'C', (byte) 1, false, 100, 100, 200, 3);
		Map<VcfRecord, Pair<Accumulator, Accumulator>> map = new HashMap<>();
		map.put(origV, new Pair<>(controlAcc100, testAcc100));
		
		VcfRecord v = PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3).get();
		assertEquals(origV.getChrPosition(), v.getChrPosition());
		assertEquals("C", v.getAlt());
		assertEquals("A", v.getRef());
		assertEquals("GT:AD:DP:FT:INF:NNS:OABS", v.getFormatFields().get(0));
		assertEquals("0/0:3,0:3:.:.:.:A2[]1[]", v.getFormatFields().get(1));
		assertEquals("1/1:0,3:3:.:SOMATIC:2:C2[]1[]", v.getFormatFields().get(2));
	}
	
	@Test
	public void createCS2Pos() {
		VcfRecord origV = VcfUtils.createVcfRecord(new ChrPointPosition("1", 100),null,"A","G");
		VcfRecord origV2 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 101),null,"C","T");
		
		Accumulator controlAcc100 = new Accumulator(100);
		controlAcc100.addBase((byte)'A', (byte) 1, true, 100, 100, 200, 1);
		controlAcc100.addBase((byte)'A', (byte) 1, true, 100, 100, 200, 2);
		controlAcc100.addBase((byte)'A', (byte) 1, false, 100, 100, 200, 3);
		
		Accumulator controlAcc101 = new Accumulator(101);
		controlAcc101.addBase((byte)'C', (byte) 1, true, 101, 101, 200, 1);
		controlAcc101.addBase((byte)'C', (byte) 1, true, 101, 101, 200, 2);
		controlAcc101.addBase((byte)'C', (byte) 1, false, 101, 101, 200, 3);
		
		Accumulator testAcc100 = new Accumulator(100);
		testAcc100.addBase((byte)'G', (byte) 1, true, 100, 100, 200, 1);
		testAcc100.addBase((byte)'G', (byte) 1, true, 100, 100, 200, 2);
		testAcc100.addBase((byte)'G', (byte) 1, false, 100, 100, 200, 3);
		
		Accumulator testAcc101 = new Accumulator(101);
		testAcc101.addBase((byte)'T', (byte) 1, true, 101, 101, 200, 1);
		testAcc101.addBase((byte)'T', (byte) 1, true, 101, 101, 200, 2);
		testAcc101.addBase((byte)'T', (byte) 1, false, 101, 101, 200, 3);
		
		Map<VcfRecord, Pair<Accumulator, Accumulator>> map = new HashMap<>();
		map.put(origV, new Pair<>(controlAcc100, testAcc100));
		map.put(origV2, new Pair<>(controlAcc101, testAcc101));
		
		VcfRecord v = PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3).get();
		assertEquals(2, v.getChrPosition().getLength());
		assertEquals("GT", v.getAlt());
		assertEquals("AC", v.getRef());
		assertEquals("GT:AD:DP:FT:INF:NNS:OABS", v.getFormatFields().get(0));
		assertEquals("0/0:3,0:3:.:.:.:AC2[]1[]", v.getFormatFields().get(1));
		assertEquals("1/1:0,3:3:.:SOMATIC:2:GT2[]1[]", v.getFormatFields().get(2));
		
		/*
		 * add some noise
		 */
		controlAcc100.addBase((byte)'T', (byte) 1, false, 100, 100, 200, 4);
		controlAcc100.addBase((byte)'T', (byte) 1, false, 100, 100, 200, 5);
		controlAcc100.addBase((byte)'T', (byte) 1, false, 100, 100, 200, 6);
		
		 v = PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3).get();
		assertEquals(2, v.getChrPosition().getLength());
		assertEquals("GT", v.getAlt());
//		assertEquals("T_,GT", v.getAlt());
		assertEquals("AC", v.getRef());
		assertEquals("GT:AD:DP:FT:INF:NNS:OABS", v.getFormatFields().get(0));
		assertEquals("0/0:3,0:3:.:.:.:AC2[]1[];T_0[]3[]", v.getFormatFields().get(1));
		assertEquals("1/1:0,3:3:.:SOMATIC:2:GT2[]1[]", v.getFormatFields().get(2));
	}
	
	@Test
	public void containsRef() {
		/*
		 * chr1    16862501        .       TA      CG      .       PASS    .       ACCS    CG,28,7,C_,1,0,TA,48,15,TG,65,38,_G,1,2,_A,0,1  CG,22,4,TA,37,14,TG,52,18,_G,1,1,C_,0,1
		 * 
		 * Largest 2 genotypes are TA (ref) and TG, but TG won't make it as it contains the ref!
		 */
		TMap<String,short[]> basesAndCounts = new THashMap<>();
		basesAndCounts.put("CG", new short[]{28,1,7,1});
		basesAndCounts.put("C_", new short[]{1,1,0,0});
		basesAndCounts.put("TA", new short[]{48,10,15,12});
		basesAndCounts.put("TG", new short[]{65,65,38,38});
		basesAndCounts.put("_G", new short[]{1,1,2,1});
		basesAndCounts.put("_A", new short[]{0,0,1,1});
		List<String> basesForGenotype = PipelineUtil.getBasesForGenotype(basesAndCounts, 4,"TA");
		assertEquals(2, basesForGenotype.size());
		assertEquals("TA", basesForGenotype.get(0));
		assertEquals("CG", basesForGenotype.get(1));
		
	}
	
	@Test
	public void underscoresInAlt() {
		/*
		 * chr1    3418615 .       GG      _G,_A   .       .       .       GT:DP:MR:OABS   1/2:121:64,24:CG6[]5[];GG8[]13[];TG0[]1[];_A12[]12[];_G30[]34[] 0/1:112:74,.:CG3[]9[];GG4[]10[];_A9[]2[];_C1[]0[];_G39[]35[]
		 */
		Map<String,short[]> basesAndCounts = new HashMap<>();
		basesAndCounts.put("CC", new short[]{6,5,5,4});
		basesAndCounts.put("GG", new short[]{8,8,13,8});
		basesAndCounts.put("TG", new short[]{0,0,0,1});
		basesAndCounts.put("_A", new short[]{12,9,12,8});
		basesAndCounts.put("_G", new short[]{30,12,34,11});
		List<String> basesForGenotype = PipelineUtil.getBasesForGenotype(basesAndCounts, 4,"GG");
		assertEquals(2, basesForGenotype.size());
		assertEquals("GG", basesForGenotype.get(0));
		assertEquals("CC", basesForGenotype.get(1));
		
		/*
		 * chr1    521366  .       NNN     __G,TAG .       .       .       GT:DP:MR:OABS   1/1:8:7,.:TAG1[]0[];__G7[]0[]   1/2:10:5,4:TAG4[]0[];_AG1[]0[];__G5[]0[]
		 */
		basesAndCounts = new HashMap<>();
		basesAndCounts.put("TAG", new short[]{4,1,0,0});
		basesAndCounts.put("_AG", new short[]{1,1,0,0});
		basesAndCounts.put("__G", new short[]{5,1,0,0});
		basesForGenotype = PipelineUtil.getBasesForGenotype(basesAndCounts, 4,"NNN");
		assertEquals(1, basesForGenotype.size());
		assertEquals("TAG", basesForGenotype.get(0));
	}
	
	@Test
	public void compoundSnp() throws Exception {
		VcfRecord v1 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 100),null,"A","C");
		v1.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		VcfRecord v2 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 101),null,"C","G");
		v2.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		
		
		final Accumulator tumour100 = new Accumulator(100);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 1);
		final Accumulator tumour101 = new Accumulator(101);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 1);
		
		
		Map<VcfRecord, Pair<Accumulator, Accumulator>> map = new HashMap<>();
		map.put(v1,  new Pair<>(null, tumour100));
		map.put(v2,  new Pair<>(null, tumour101));
		assertEquals(false, PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3).isPresent());
		
		// need 4 reads with the cs to register
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 2);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 2);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 3);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 3);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 4);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 4);
		Optional<VcfRecord> ov = PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3); 
		assertEquals(true, ov.isPresent());
		VcfRecord v = ov.get();
	
		List<String> ff = v.getFormatFields();
		assertEquals(true, ff.get(2).contains("CG4[]0[]"));	// tumour
		assertEquals("./.:.:0:.:.:.:.", ff.get(1));	// control
	}
	
	@Test
	public void compoundSnpOneGermlineOneSomatic() throws Exception {
		VcfRecord v1 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 100),null,"A","C");
		VcfRecord v2 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 101),null,"C","G");
		v2.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		
		
		final Accumulator tumour100 = new Accumulator(100);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 1);
		final Accumulator tumour101 = new Accumulator(101);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 1);
		
		
		Map<VcfRecord, Pair<Accumulator, Accumulator>> map = new HashMap<>();
		map.put(v1,  new Pair<>(null, tumour100));
		map.put(v2,  new Pair<>(null, tumour101));
		assertEquals(false, PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3).isPresent());
		
		// need 4 reads with the cs to register
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 2);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 2);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 3);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 3);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 4);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 4);
		Optional<VcfRecord> ov = PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3); 
		assertEquals(true, ov.isPresent());
		VcfRecord v = ov.get();
		
		List<String> ff = v.getFormatFields();
		assertEquals(true, ff.get(2).contains("CG4[]0[]"));	// tumour
		assertEquals("./.:.:0:.:.:.:.", ff.get(1));	// control
	}
	
	@Test
	public void noCompoundSnpMissingAccs() throws Exception {
		/*
		 * chr4    8046419 .       G       T       .       .       BaseQRankSum=0.694;ClippingRankSum=1.157;DP=24;FS=5.815;MQ=60.00;MQRankSum=-0.602;QD=15.87;ReadPosRankSum=0.787;SOR=2.258       GT:AD:DP:GQ:FT:INF:MR:NNS:OABS  0/1:5,14:19:99:SAN3:.:.:.:.     0/1:8,32:40:99:SBIASCOV;5BP=1;SAT3:.:1:1:T1[22]0[0]
chr4    8046420 .       A       C       .       .       BaseQRankSum=1.528;ClippingRankSum=0.787;DP=24;FS=5.815;MQ=60.00;MQRankSum=-1.713;QD=15.87;ReadPosRankSum=0.787;SOR=2.258       GT:AD:DP:GQ:FT:INF:MR:NNS:OABS  0/1:5,14:19:99:SAN3:.:.:.:.     0/1:10,28:38:99:SAT3:.:.:.:.
chr4    8046421 .       A       T       .       .       BaseQRankSum=0.727;ClippingRankSum=0.168;DP=24;FS=6.539;MQ=60.00;MQRankSum=-0.727;QD=31.45;ReadPosRankSum=0.950;SOR=1.975       GT:AD:DP:GQ:FT:INF:MR:NNS:OABS  0/1:3,20:23:36:SAN3:.:2:2:T1[42]1[32]   1/1:2,45:47:99:COVT:.:6:6:T2[39.5]4[42]
		 */
		VcfRecord v1 = new VcfRecord(new String[]{"chr4","8046419",".","G","T",".",".",".","GT:AD:DP:GQ:FT:INF:MR:NNS:OABS","0/1:5,14:19:99:SAN3:.:.:.:.","0/1:8,32:40:99:SBIASCOV;5BP=1;SAT3:.:1:1:T1[22]0[0]"});
		VcfRecord v2 = new VcfRecord(new String[]{"chr4","8046420",".","A","C",".",".",".","GT:AD:DP:GQ:FT:INF:MR:NNS:OABS","0/1:5,14:19:99:SAN3:.:.:.:.","0/1:10,28:38:99:SAT3:.:.:.:."});
		VcfRecord v3 = new VcfRecord(new String[]{"chr4","8046421",".","A","T",".",".",".","GT:AD:DP:GQ:FT:INF:MR:NNS:OABS","0/1:3,20:23:36:SAN3:.:2:2:T1[42]1[32]","1/1:2,45:47:99:COVT:.:6:6:T2[39.5]4[42]"});
		
		
		final Accumulator t8046419 = AccumulatorUtils.createFromOABS("T1[22]0[0]", 8046419);
		final Accumulator t8046421 = AccumulatorUtils.createFromOABS("T2[39.5]4[42]", 8046421);
				
		
		Map<VcfRecord, Pair<Accumulator, Accumulator>> map = new HashMap<>(8);
		map.put(v1,  new Pair<>(null, t8046419));
		map.put(v2,  new Pair<>(null, null));
		map.put(v3,  new Pair<>(null, t8046421));
		try {
			PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		
	}
	
	@Test
	public void noCompoundSnpRefInAlt() throws Exception {
		/*
		 * Don't want this happening
		 * chr1    985449  .       GG      AG      .       .       .       GT:DP:MR:OABS   1/1:12:9:AG1[]8[];GA2[]0[];_G1[]0[]     1/1:10:6:AG3[]3[];GA2[]1[];_G1[]0[]
		 * which was made up from
		 * chr1    985449  rs56255212      G       A       421.77  PASS    AC=2;AF=1.00;AN=2;DB;DP=10;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=31.82;SOR=2.303        GT:AD:DP:GQ:PL:GD:AC:OABS:MR:NNS        1/1:0,10:10:30:450,30,0:A/A:A1[37],8[35.12],G2[38],0[0]:.:9:9   0/1:3,6:9:99:208,0,138:A/G:A3[34],3[38.67],G2[37],1[35]:.:6:5
		 * chr1    985450  .       G       A       67.77   MIN;MR;NNS      SOMATIC GT:AD:DP:GQ:PL:GD:AC:OABS:MR:NNS        .:.:.:.:.:G/G:A2[37],0[0],G2[37],8[36.5]:.:2:2  0/1:7,3:10:96:96,0,236:A/G:A2[35],1[35],G4[36.5],3[39]:.:3:3
		 */
		VcfRecord v1 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 100),null,"G","A");
		v1.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		VcfRecord v2 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 101),null,"G","A");
		v2.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		/*
		 * A3[34],3[38.67],G2[37],1[35]
		 */
		final Accumulator tumour100 = new Accumulator(100);
		tumour100.addBase((byte)'A', (byte)30, true, 100, 100, 200, 1);
		tumour100.addBase((byte)'A', (byte)30, true, 100, 100, 200, 2);
		tumour100.addBase((byte)'A', (byte)30, true, 100, 100, 200, 3);
		tumour100.addBase((byte)'A', (byte)30, false, 100, 100, 200, 4);
		tumour100.addBase((byte)'A', (byte)30, false, 100, 100, 200, 5);
		tumour100.addBase((byte)'A', (byte)30, false, 100, 100, 200, 6);
		tumour100.addBase((byte)'G', (byte)30, true, 100, 100, 200, 7);
		tumour100.addBase((byte)'G', (byte)30, true, 100, 100, 200, 8);
		tumour100.addBase((byte)'G', (byte)30, false, 100, 100, 200, 9);
		/*
		 * A2[35],1[35],G4[36.5],3[39]
		 */
		final Accumulator tumour101 = new Accumulator(101);
		tumour101.addBase((byte)'A', (byte)30, true, 101, 101, 200, 7);
		tumour101.addBase((byte)'A', (byte)30, true, 101, 101, 200, 8);
		tumour101.addBase((byte)'A', (byte)30, false, 101, 101, 200, 9);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 1);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 2);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 3);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 4);
		tumour101.addBase((byte)'G', (byte)30, false, 101, 101, 200, 5);
		tumour101.addBase((byte)'G', (byte)30, false, 101, 101, 200, 6);
		tumour101.addBase((byte)'G', (byte)30, false, 101, 101, 200, 10);
		
		
		/*
		 * A1[37],8[35.12],G2[38],0[0]
		 */
		final Accumulator control100 = new Accumulator(100);
		control100.addBase((byte)'A', (byte)30, true, 100, 100, 200, 1);
		control100.addBase((byte)'A', (byte)30, false, 100, 100, 200, 2);
		control100.addBase((byte)'A', (byte)30, false, 100, 100, 200, 3);
		control100.addBase((byte)'A', (byte)30, false, 100, 100, 200, 4);
		control100.addBase((byte)'A', (byte)30, false, 100, 100, 200, 5);
		control100.addBase((byte)'A', (byte)30, false, 100, 100, 200, 6);
		control100.addBase((byte)'A', (byte)30, false, 100, 100, 200, 7);
		control100.addBase((byte)'A', (byte)30, false, 100, 100, 200, 8);
		control100.addBase((byte)'A', (byte)30, false, 100, 100, 200, 9);
		control100.addBase((byte)'G', (byte)30, true, 100, 100, 200, 10);
		control100.addBase((byte)'G', (byte)30, true, 100, 100, 200, 11);
		/*
		 * A2[37],0[0],G2[37],8[36.5]
		 */
		final Accumulator control101 = new Accumulator(101);
		control101.addBase((byte)'A', (byte)30, true, 101, 101, 200, 10);
		control101.addBase((byte)'A', (byte)30, true, 101, 101, 200, 11);
		control101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 12);
		control101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 1);
		control101.addBase((byte)'G', (byte)30, false, 101, 101, 200, 2);
		control101.addBase((byte)'G', (byte)30, false, 101, 101, 200, 3);
		control101.addBase((byte)'G', (byte)30, false, 101, 101, 200, 4);
		control101.addBase((byte)'G', (byte)30, false, 101, 101, 200, 5);
		control101.addBase((byte)'G', (byte)30, false, 101, 101, 200, 6);
		control101.addBase((byte)'G', (byte)30, false, 101, 101, 200, 7);
		control101.addBase((byte)'G', (byte)30, false, 101, 101, 200, 8);
		control101.addBase((byte)'G', (byte)30, false, 101, 101, 200, 9);
		
		
		Map<VcfRecord, Pair<Accumulator, Accumulator>> map = new HashMap<>();
		map.put(v1,  new Pair<>(control100, tumour100));
		map.put(v2,  new Pair<>(control101, tumour101));
		assertEquals(false, PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3).isPresent());
		
	}
	
	@Test
	public void compoundSnpReverseStrand() throws Exception {
		VcfRecord v1 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 100),null,"A","C");
		v1.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		VcfRecord v2 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 101),null,"C","G");
		v2.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		
		final Accumulator tumour100 = new Accumulator(100);
		tumour100.addBase((byte)'C', (byte)30, false, 100, 100, 200, 1);
		final Accumulator tumour101 = new Accumulator(101);
		tumour101.addBase((byte)'G', (byte)30, false, 101, 101, 200, 1);
		
		Map<VcfRecord, Pair<Accumulator, Accumulator>> map = new HashMap<>();
		map.put(v1,  new Pair<>(null, tumour100));
		map.put(v2,  new Pair<>(null, tumour101));
		assertEquals(false, PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3).isPresent());
		
		// need 4 reads with the cs to register
		tumour100.addBase((byte)'C', (byte)30, false, 100, 100, 200, 2);
		tumour101.addBase((byte)'G', (byte)30, false, 101, 101, 200, 2);
		tumour100.addBase((byte)'C', (byte)30, false, 100, 100, 200, 3);
		tumour101.addBase((byte)'G', (byte)30, false, 101, 101, 200, 3);
		tumour100.addBase((byte)'C', (byte)30, false, 100, 100, 200, 4);
		tumour101.addBase((byte)'G', (byte)30, false, 101, 101, 200, 4);
		Optional<VcfRecord> ov = PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3); 
		assertEquals(true, ov.isPresent());
		VcfRecord v = ov.get();
		
		List<String> ff = v.getFormatFields();
		assertEquals(true, ff.get(2).contains("CG0[]4[]"));	// tumour
		assertEquals("./.:.:0:.:.:.:.", ff.get(1));	// control
	}
	
	@Test
	public void compoundSnpBothStrands() throws Exception {
		VcfRecord v1 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 100),null,"A","C");
		v1.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		VcfRecord v2 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 101),null,"C","G");
		v2.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		
		final Accumulator tumour100 = new Accumulator(100);
		tumour100.addBase((byte)'C', (byte)30, false, 100, 100, 200, 1);
		final Accumulator tumour101 = new Accumulator(101);
		tumour101.addBase((byte)'G', (byte)30, false, 101, 101, 200, 1);
		
		Map<VcfRecord, Pair<Accumulator, Accumulator>> map = new HashMap<>();
		map.put(v1,  new Pair<>(null, tumour100));
		map.put(v2,  new Pair<>(null, tumour101));
		assertEquals(false, PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3).isPresent());
		
		// need 4 reads with the cs to register
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 2);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 2);
		tumour100.addBase((byte)'C', (byte)30, false, 100, 100, 200, 3);
		tumour101.addBase((byte)'G', (byte)30, false, 101, 101, 200, 3);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 4);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 4);
		
		Optional<VcfRecord> ov = PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3); 
		assertEquals(true, ov.isPresent());
		VcfRecord v = ov.get();
		List<String> ff = v.getFormatFields();
		assertEquals(true, ff.get(2).contains("CG2[]2[]"));	// tumour
		assertEquals("./.:.:0:.:.:.:.", ff.get(1));	// control
	}
	
	@Test
	public void compoundSnpWithOverlappingReads() {
		VcfRecord v1 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 100),null,"A","C");
		v1.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		VcfRecord v2 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 101),null,"C","G");
		v2.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		
		final Accumulator tumour100 = new Accumulator(100);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 1);
		final Accumulator tumour101 = new Accumulator(101);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 1);
		
		Map<VcfRecord, Pair<Accumulator, Accumulator>> map = new HashMap<>();
		map.put(v1,  new Pair<>(null, tumour100));
		map.put(v2,  new Pair<>(null, tumour101));
		assertEquals(false, PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3).isPresent());
		
		// need 4 reads with the cs to register
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 2);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 2);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 3);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 3);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 4);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 4);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 5);

		Optional<VcfRecord> ov = PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3); 
		assertEquals(true, ov.isPresent());
		VcfRecord v = ov.get();
		List<String> ff = v.getFormatFields();
		assertEquals("./.:.:0:.:.:.:.", ff.get(1));	// control
		assertEquals(true, ff.get(2).contains("CG4[]0[];_G1[]0[]"));	// tumour
	}
	
	@Test
	public void compoundSnpWithOverlappingReadsOtherEnd() {
		VcfRecord v1 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 100),null,"A","C");
		v1.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		VcfRecord v2 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 101),null,"C","G");
		v2.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		
		final Accumulator tumour100 = new Accumulator(100);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 1);
		final Accumulator tumour101 = new Accumulator(101);
		tumour101.addBase((byte)'G', (byte)30, true, 100, 101, 200, 1);
		
		Map<VcfRecord, Pair<Accumulator, Accumulator>> map = new HashMap<>();
		map.put(v1,  new Pair<>(null, tumour100));
		map.put(v2,  new Pair<>(null, tumour101));
		assertEquals(false, PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3).isPresent());
		
		// need 4 reads with the cs to register
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 2);
		tumour101.addBase((byte)'G', (byte)30, true, 100, 101, 200, 2);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 3);
		tumour101.addBase((byte)'G', (byte)30, true, 100, 101, 200, 3);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 4);
		tumour101.addBase((byte)'G', (byte)30, true, 100, 101, 200, 4);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 5);
		
		Optional<VcfRecord> ov = PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3); 
		assertEquals(true, ov.isPresent());
		VcfRecord v = ov.get();
		assertEquals("AC", v.getRef());
		assertEquals("CG", v.getAlt());
		List<String> ff = v.getFormatFields();
		assertEquals("./.:.:0:.:.:.:.", ff.get(1));	// control
		/*
		 * filters are now applied in qannotate
		 */
		assertEquals("1/1:0,4:4:.:SOMATIC:1:CG4[]0[];C_1[]0[]", ff.get(2));	// tumour
	}
	
	@Test
	public void csRealLife() {
		/*
		 * chr1	154701381	.	GG	AA,AC	.	.	IN=1,2	
		 * GT:CCC:CCM:DP:FT:INF:MR:NNS:OABS	
		 * 0/0:Reference:15:35:PASS:.:.:.:GG19[]16[];G_1[]0[]	
		 * 1/2:DoubleSomatic:15:67:SBIASALT:SOMATIC:65,1:61,1:AA34[]31[];AC1[]0[];CA1[]0[]	
		 * 0/0:Reference:15:35:PASS:.:.:.:GG19[]16[];G_1[]0[]	
		 * 1/2:DoubleSomatic:15:67:SBIASALT:SOMATIC:65,1:61,1:AA34[]31[];AC1[]0[];CA1[]0[]
		 * 
		 * This should not be 1/2 in the test - only 1 read supporting the second alt
		 */
		VcfRecord v1 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 154701381),null,"G","A");
		v1.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		VcfRecord v2 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 154701382),null,"G","A");
		v2.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		final Accumulator tumour100 = new Accumulator(154701381);
		final Accumulator tumour101 = new Accumulator(154701382);
		for (int i = 1 ; i <= 34 ; i++) {
			tumour100.addBase((byte)'A', (byte)30, true, 154701261 + i, 154701381, 154701391, i);
			tumour101.addBase((byte)'A', (byte)30, true, 154701261 + i, 154701382, 154701391, i);
		}
		for (int i = 35 ; i < 35 + 31 ; i++) {
			tumour100.addBase((byte)'A', (byte)30, false, 154701261 + i, 154701381, 154701391 + i, i);
			tumour101.addBase((byte)'A', (byte)30, false, 154701261 + i, 154701382, 154701391 + i, i);
		}
		
		tumour100.addBase((byte)'A', (byte)30, true, 154701262, 154701381, 154701391, 70);
		tumour101.addBase((byte)'C', (byte)30, true, 154701262, 154701382, 154701391, 70);
		tumour100.addBase((byte)'C', (byte)30, true, 154701262, 154701381, 154701391, 71);
		tumour101.addBase((byte)'A', (byte)30, true, 154701262, 154701382, 154701391, 71);
		
		Map<VcfRecord, Pair<Accumulator, Accumulator>> map = new HashMap<>(4);
		map.put(v1,  new Pair<>(null, tumour100));
		map.put(v2,  new Pair<>(null, tumour101));
		
		Optional<VcfRecord> ov = PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3); 
		assertEquals(true, ov.isPresent());
		VcfRecord v = ov.get();
		assertEquals("AA", v.getAlt());
		assertEquals("GG", v.getRef());
		List<String> ff = v.getFormatFields();
		assertEquals("1/1:0,65:67:.:SOMATIC:65:AA34[]31[];AC1[]0[];CA1[]0[]", ff.get(2));	// tumour
	}
	
	@Test
	public void csGATK() {
		VcfRecord v1 = new VcfRecord(new String[]{"chr1","39592384",".","G","T",".",".","BaseQRankSum=-0.893;ClippingRankSum=0.266;DP=46;FS=14.005;MQ=60.00;MQRankSum=1.801;QD=31.52;ReadPosRankSum=-0.517;SOR=0.028;IN=2;HOM=3,ACTTGAGCTTtGGAGGCAGAG;","GT:AD:CCC:CCM:DP:FT:GQ:INF:QL","./.:.:Reference:13:.:PASS:.:NCIG:.","0/1:7,38:Somatic:13:45:PASS:99:SOMATIC:1449.77"});
		VcfRecord v2 = new VcfRecord(new String[]{"chr1","39592385",".","G","A",".",".","BaseQRankSum=0.767;ClippingRankSum=-0.266;DP=46;FS=14.005;MQ=60.00;MQRankSum=-0.423;QD=31.52;ReadPosRankSum=-0.611;SOR=0.028;IN=2;HOM=0,CTTGAGCTTGaGAGGCAGAGA;","GT:AD:CCC:CCM:DP:FT:GQ:INF:QL","./.:.:Reference:13:.:PASS:.:NCIG:.","0/1:7,38:Somatic:13:45:PASS:99:SOMATIC:1449.77"});
		List<VcfRecord> vcfs = Arrays.asList(v1,v2);
		Optional<VcfRecord> oVcf = PipelineUtil.createCompoundSnpGATK(vcfs);
		assertEquals(true, oVcf.isPresent());
		VcfRecord v = oVcf.get();
		assertEquals("GG", v.getRef());
		assertEquals("TA", v.getAlt());
		Map<String, String[]>ffMap = VcfUtils.getFormatFieldsAsMap(v.getFormatFields());
		String[] gtArr = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE);
		String[] dpArr = ffMap.get(VcfHeaderUtils.FORMAT_READ_DEPTH);
		String[] adArr = ffMap.get(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS);
		String[] gqArr = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE_QUALITY);
		String[] qlArr = ffMap.get(VcfHeaderUtils.FORMAT_QL);
		String[] infArr = ffMap.get(VcfHeaderUtils.FORMAT_INFO);
		assertEquals("./.", gtArr[0]);
		assertEquals("0/1", gtArr[1]);
		assertEquals(".", dpArr[0]);
		assertEquals("45", dpArr[1]);
		assertEquals(".", adArr[0]);
		assertEquals("7,38", adArr[1]);
		assertEquals(".", gqArr[0]);
		assertEquals("99", gqArr[1]);
		assertEquals(".", qlArr[0]);
		assertEquals("1449.77", qlArr[1]);
		assertEquals("NCIG", infArr[0]);
		assertEquals("SOMATIC", infArr[1]);
	}
	
	@Test
	public void csGATK2() {
		VcfRecord v1 = new VcfRecord(new String[]{"chr1","40615301",".","C","A",".",".","BaseQRankSum=-0.657;ClippingRankSum=-1.055;DP=70;FS=2.380;MQ=60.00;MQRankSum=-1.014;QD=30.43;ReadPosRankSum=-0.835;SOR=0.573;IN=2;HOM=0,GACCTGTAATaCCAGCTACTC;EFF=intergenic_region(MODIFIER||||||||||1)","GT:AD:CCC:CCM:DP:FT:GQ:INF:QL","./.:.:Reference:13:.:PASS:.:NCIG:.","0/1:17,53:Somatic:13:70:PASS:99:SOMATIC:2129.77"});
		VcfRecord v2 = new VcfRecord(new String[]{"chr1","40615302",".","C","T",".",".","BaseQRankSum=-0.104;ClippingRankSum=0.439;DP=69;FS=2.380;MQ=60.00;MQRankSum=-0.369;QD=30.87;ReadPosRankSum=-0.717;SOR=0.524;IN=2;HOM=0,ACCTGTAATCtCAGCTACTCG;EFF=intergenic_region(MODIFIER||||||||||1)","GT:AD:CCC:CCM:DP:FT:GQ:INF:QL","./.:.:Reference:13:.:PASS:.:NCIG:.","0/1:17,52:Somatic:13:69:PASS:99:SOMATIC:2129.77"});
		List<VcfRecord> vcfs = Arrays.asList(v1,v2);
		Optional<VcfRecord> oVcf = PipelineUtil.createCompoundSnpGATK(vcfs);
		assertEquals(true, oVcf.isPresent());
		VcfRecord v = oVcf.get();
		assertEquals("CC", v.getRef());
		assertEquals("AT", v.getAlt());
		Map<String, String[]>ffMap = VcfUtils.getFormatFieldsAsMap(v.getFormatFields());
		String[] gtArr = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE);
		String[] dpArr = ffMap.get(VcfHeaderUtils.FORMAT_READ_DEPTH);
		String[] adArr = ffMap.get(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS);
		String[] gqArr = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE_QUALITY);
		String[] qlArr = ffMap.get(VcfHeaderUtils.FORMAT_QL);
		String[] infArr = ffMap.get(VcfHeaderUtils.FORMAT_INFO);
		assertEquals("./.", gtArr[0]);
		assertEquals("0/1", gtArr[1]);
		assertEquals(".", dpArr[0]);
		assertEquals("69", dpArr[1]);
		assertEquals(".", adArr[0]);
		assertEquals("17,52", adArr[1]);
		assertEquals(".", gqArr[0]);
		assertEquals("99", gqArr[1]);
		assertEquals(".", qlArr[0]);
		assertEquals("2129.77", qlArr[1]);
		assertEquals("NCIG", infArr[0]);
		assertEquals("SOMATIC", infArr[1]);
	}
	
	@Test
	public void csGATK3() {
		VcfRecord v1 = new VcfRecord(new String[]{"chr1","47083665",".","G","T",".",".","BaseQRankSum=0.409;ClippingRankSum=-1.524;DP=72;FS=2.630;MQ=60.00;MQRankSum=-0.944;QD=31.83;ReadPosRankSum=-0.572;SOR=0.399;IN=2;HOM=0,AGAATACATAtATACTAGGA","GT:AD:CCC:CCM:DP:FT:GQ:INF:QL","./.:.:Reference:13:.:PASS:.:NCIG:.","0/1:13,58:Somatic:13:71:PASS:99:SOMATIC:2291.77"});
		VcfRecord v2 = new VcfRecord(new String[]{"chr1","47083666",".","A","T",".",".","BaseQRankSum=0.203;ClippingRankSum=0.452;DP=69;FS=1.235;MQ=60.00;MQRankSum=-0.717;QD=33.21;ReadPosRankSum=-0.733;SOR=0.436;IN=2;HOM=2,GAATACATAGtTACTAGGAGG","GT:AD:CCC:CCM:DP:FT:GQ:INF:QL","./.:.:Reference:13:.:PASS:.:NCIG:.","0/1:13,55:Somatic:13:68:PASS:99:SOMATIC:2291.77"});
		List<VcfRecord> vcfs = Arrays.asList(v1,v2);
		Optional<VcfRecord> oVcf = PipelineUtil.createCompoundSnpGATK(vcfs);
		assertEquals(true, oVcf.isPresent());
		VcfRecord v = oVcf.get();
		assertEquals("GA", v.getRef());
		assertEquals("TT", v.getAlt());
		Map<String, String[]>ffMap = VcfUtils.getFormatFieldsAsMap(v.getFormatFields());
		String[] gtArr = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE);
		String[] dpArr = ffMap.get(VcfHeaderUtils.FORMAT_READ_DEPTH);
		String[] adArr = ffMap.get(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS);
		String[] gqArr = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE_QUALITY);
		String[] qlArr = ffMap.get(VcfHeaderUtils.FORMAT_QL);
		String[] infArr = ffMap.get(VcfHeaderUtils.FORMAT_INFO);
		assertEquals("./.", gtArr[0]);
		assertEquals("0/1", gtArr[1]);
		assertEquals(".", dpArr[0]);
		assertEquals("68", dpArr[1]);
		assertEquals(".", adArr[0]);
		assertEquals("13,55", adArr[1]);
		assertEquals(".", gqArr[0]);
		assertEquals("99", gqArr[1]);
		assertEquals(".", qlArr[0]);
		assertEquals("2291.77", qlArr[1]);
		assertEquals("NCIG", infArr[0]);
		assertEquals("SOMATIC", infArr[1]);
	}
	
	@Test
	public void csGATK4() {
		VcfRecord v1 = new VcfRecord(new String[]{"chr1","169423269",".","G","A",".",".","DP=120;FS=0.000;MQ=60.00;QD=34.82;SOR=1.034;IN=2;HOM=2,TCCTTCTTCAaGACCAAATAG","GT:AD:CCC:CCM:DP:FT:GQ:INF:QL","./.:.:Reference:14:.:PASS:.:NCIG:.","1/1:0,120:SomaticNoReference:14:120:PASS:99:SOMATIC:5348.77"});
		VcfRecord v2 = new VcfRecord(new String[]{"chr1","169423270",".","G","A",".",".","DP=121;FS=0.000;MQ=60.00;QD=29.54;SOR=1.096;IN=2;HOM=2,CCTTCTTCAGaACCAAATAGA","GT:AD:CCC:CCM:DP:FT:GQ:INF:QL","./.:.:Reference:14:.:PASS:.:NCIG:.","1/1:0,121:SomaticNoReference:14:121:PASS:99:SOMATIC:5348.77"});
		List<VcfRecord> vcfs = Arrays.asList(v1,v2);
		Optional<VcfRecord> oVcf = PipelineUtil.createCompoundSnpGATK(vcfs);
		assertEquals(true, oVcf.isPresent());
		VcfRecord v = oVcf.get();
		assertEquals("GG", v.getRef());
		assertEquals("AA", v.getAlt());
		Map<String, String[]>ffMap = VcfUtils.getFormatFieldsAsMap(v.getFormatFields());
		String[] gtArr = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE);
		String[] dpArr = ffMap.get(VcfHeaderUtils.FORMAT_READ_DEPTH);
		String[] adArr = ffMap.get(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS);
		String[] gqArr = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE_QUALITY);
		String[] qlArr = ffMap.get(VcfHeaderUtils.FORMAT_QL);
		String[] infArr = ffMap.get(VcfHeaderUtils.FORMAT_INFO);
		assertEquals("./.", gtArr[0]);
		assertEquals("1/1", gtArr[1]);
		assertEquals(".", dpArr[0]);
		assertEquals("120", dpArr[1]);
		assertEquals(".", adArr[0]);
		assertEquals("0,120", adArr[1]);
		assertEquals(".", gqArr[0]);
		assertEquals("99", gqArr[1]);
		assertEquals(".", qlArr[0]);
		assertEquals("5348.77", qlArr[1]);
		assertEquals("NCIG", infArr[0]);
		assertEquals("SOMATIC", infArr[1]);
	}
	
	@Test
	public void csGATK5() {
		VcfRecord v1 = new VcfRecord(new String[]{"chr2","65487954",".","C","T",".",".","BaseQRankSum=0.984;ClippingRankSum=0.343;DP=64;FS=3.774;MQ=59.60;MQRankSum=0.270;QD=10.76;ReadPosRankSum=1.115;SOR=0.328;IN=2;HOM=2,AGCTCTGCCTtCCGGGTTCAC","GT:AD:CCC:CCM:DP:FT:GQ:INF:QL","./.:.:Reference:13:.:PASS:.:NCIG:.","0/1:42,21:Somatic:13:63:PASS:99:SOMATIC:688.77"});
		VcfRecord v2 = new VcfRecord(new String[]{"chr2","65487955",".","C","T",".",".","BaseQRankSum=2.173;ClippingRankSum=-0.100;DP=65;FS=2.303;MQ=59.61;MQRankSum=-0.186;QD=10.60;ReadPosRankSum=1.101;SOR=0.364;IN=2;HOM=0,GCTCTGCCTCtCGGGTTCACG","GT:AD:CCC:CCM:DP:FT:GQ:INF:QL","./.:.:Reference:13:.:PASS:.:NCIG:.","0/1:43,21:Somatic:13:64:PASS:99:SOMATIC:688.77"});
		List<VcfRecord> vcfs = Arrays.asList(v1,v2);
		Optional<VcfRecord> oVcf = PipelineUtil.createCompoundSnpGATK(vcfs);
		assertEquals(true, oVcf.isPresent());
		VcfRecord v = oVcf.get();
		assertEquals("CC", v.getRef());
		assertEquals("TT", v.getAlt());
		Map<String, String[]>ffMap = VcfUtils.getFormatFieldsAsMap(v.getFormatFields());
		String[] gtArr = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE);
		String[] dpArr = ffMap.get(VcfHeaderUtils.FORMAT_READ_DEPTH);
		String[] adArr = ffMap.get(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS);
		String[] gqArr = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE_QUALITY);
		String[] qlArr = ffMap.get(VcfHeaderUtils.FORMAT_QL);
		String[] infArr = ffMap.get(VcfHeaderUtils.FORMAT_INFO);
		assertEquals("./.", gtArr[0]);
		assertEquals("0/1", gtArr[1]);
		assertEquals(".", dpArr[0]);
		assertEquals("63", dpArr[1]);
		assertEquals(".", adArr[0]);
		assertEquals("42,21", adArr[1]);
		assertEquals(".", gqArr[0]);
		assertEquals("99", gqArr[1]);
		assertEquals(".", qlArr[0]);
		assertEquals("688.77", qlArr[1]);
		assertEquals("NCIG", infArr[0]);
		assertEquals("SOMATIC", infArr[1]);
	}
	
	@Test
	public void csGATK6() {
		VcfRecord v1 = new VcfRecord(new String[]{"chr6","32495871",".","C","T","107.28",".","AC=2;AF=1.00;AN=2;DP=0;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=0.00;SOR=0.693","GT:AD:DP:GQ:INF:QL","./.:.:.:.:NCIG:.","1/1:.:.:9:.:."});
		VcfRecord v2 = new VcfRecord(new String[]{"chr6","32495872",".","G","T","107.28",".","AC=2;AF=1.00;AN=2;DP=0;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=0.00;SOR=0.693","GT:AD:DP:GQ:INF:QL","./.:.:.:.:NCIG:.","1/1:.:.:9:.:."});
		List<VcfRecord> vcfs = Arrays.asList(v1,v2);
		Optional<VcfRecord> oVcf = PipelineUtil.createCompoundSnpGATK(vcfs);
		assertEquals(true, oVcf.isPresent());
		VcfRecord v = oVcf.get();
		assertEquals("CG", v.getRef());
		assertEquals("TT", v.getAlt());
		Map<String, String[]>ffMap = VcfUtils.getFormatFieldsAsMap(v.getFormatFields());
		String[] gtArr = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE);
		String[] dpArr = ffMap.get(VcfHeaderUtils.FORMAT_READ_DEPTH);
		String[] adArr = ffMap.get(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS);
		String[] gqArr = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE_QUALITY);
		String[] qlArr = ffMap.get(VcfHeaderUtils.FORMAT_QL);
		String[] infArr = ffMap.get(VcfHeaderUtils.FORMAT_INFO);
		assertEquals("./.", gtArr[0]);
		assertEquals("1/1", gtArr[1]);
		assertEquals(".", dpArr[0]);
		assertEquals(".", dpArr[1]);
		assertEquals(".", adArr[0]);
		assertEquals(".", adArr[1]);
		assertEquals(".", gqArr[0]);
		assertEquals("9", gqArr[1]);
		assertEquals(".", qlArr[0]);
		assertEquals(".", qlArr[1]);
		assertEquals("NCIG", infArr[0]);
		assertEquals(".", infArr[1]);
	}
	
	@Test
	public void csGATK6SingleSample() {
		VcfRecord v1 = new VcfRecord(new String[]{"chr6","32495871",".","C","T","107.28",".","AC=2;AF=1.00;AN=2;DP=0;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=0.00;SOR=0.693","GT:AD:DP:GQ:INF:QL","1/1:.:.:9:.:."});
		VcfRecord v2 = new VcfRecord(new String[]{"chr6","32495872",".","G","T","107.28",".","AC=2;AF=1.00;AN=2;DP=0;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=0.00;SOR=0.693","GT:AD:DP:GQ:INF:QL","1/1:.:.:9:.:."});
		List<VcfRecord> vcfs = Arrays.asList(v1,v2);
		Optional<VcfRecord> oVcf = PipelineUtil.createCompoundSnpGATK(vcfs, true);
		assertEquals(true, oVcf.isPresent());
		VcfRecord v = oVcf.get();
		assertEquals("CG", v.getRef());
		assertEquals("TT", v.getAlt());
		Map<String, String[]>ffMap = VcfUtils.getFormatFieldsAsMap(v.getFormatFields());
		String[] gtArr = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE);
		String[] dpArr = ffMap.get(VcfHeaderUtils.FORMAT_READ_DEPTH);
		String[] adArr = ffMap.get(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS);
		String[] gqArr = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE_QUALITY);
		String[] qlArr = ffMap.get(VcfHeaderUtils.FORMAT_QL);
		String[] infArr = ffMap.get(VcfHeaderUtils.FORMAT_INFO);
		assertEquals("1/1", gtArr[0]);
		assertEquals(".", dpArr[0]);
		assertEquals(".", adArr[0]);
		assertEquals("9", gqArr[0]);
		assertEquals(".", qlArr[0]);
		assertEquals(".", infArr[0]);
	}
	
	@Test
	public void csGATK7() {
		VcfRecord v1 = new VcfRecord(new String[]{"chrY","13487853",".","G","A,C","107.28",".","AC=2;AF=1.00;AN=2;DP=0;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=0.00;SOR=0.693","GT:AD:DP:GQ:INF:QL","0/1:.:.:.:NCIG:.","0/2:.:.:9:.:."});
		VcfRecord v2 = new VcfRecord(new String[]{"chrY","13487854",".","C","A,T","107.28",".","AC=2;AF=1.00;AN=2;DP=0;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=0.00;SOR=0.693","GT:AD:DP:GQ:INF:QL","0/1:.:.:.:NCIG:.","0/2:.:.:9:.:."});
		List<VcfRecord> vcfs = Arrays.asList(v1,v2);
		Optional<VcfRecord> oVcf = PipelineUtil.createCompoundSnpGATK(vcfs);
		assertEquals(false, oVcf.isPresent());
	}
	
	@Test
	public void csGATKSingleSample() {
		VcfRecord v1 = new VcfRecord(new String[]{"chrY","13487853",".","G","A,C","107.28",".","AC=2;AF=1.00;AN=2;DP=0;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=0.00;SOR=0.693","GT:AD:DP:GQ:INF:QL","0/2:.:.:9:.:."});
		VcfRecord v2 = new VcfRecord(new String[]{"chrY","13487854",".","C","A,T","107.28",".","AC=2;AF=1.00;AN=2;DP=0;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=0.00;SOR=0.693","GT:AD:DP:GQ:INF:QL","0/2:.:.:9:.:."});
		List<VcfRecord> vcfs = Arrays.asList(v1,v2);
		Optional<VcfRecord> oVcf = PipelineUtil.createCompoundSnpGATK(vcfs, true);
		assertEquals(false, oVcf.isPresent());
	}
	
	@Test
	public void csGATKOneSomaticOneGermline() {
		VcfRecord v1 = new VcfRecord(new String[]{"chr1","39592384",".","G","T",".",".","BaseQRankSum=-0.893;ClippingRankSum=0.266;DP=46;FS=14.005;MQ=60.00;MQRankSum=1.801;QD=31.52;ReadPosRankSum=-0.517;SOR=0.028;IN=2;HOM=3,ACTTGAGCTTtGGAGGCAGAG;","GT:AD:CCC:CCM:DP:FT:GQ:INF:QL","0/0:.:Reference:13:.:PASS:.:NCIG:.","0/1:7,38:Somatic:13:45:PASS:99:.:1449.77"});
		VcfRecord v2 = new VcfRecord(new String[]{"chr1","39592385",".","G","A",".",".","BaseQRankSum=0.767;ClippingRankSum=-0.266;DP=46;FS=14.005;MQ=60.00;MQRankSum=-0.423;QD=31.52;ReadPosRankSum=-0.611;SOR=0.028;IN=2;HOM=0,CTTGAGCTTGaGAGGCAGAGA;","GT:AD:CCC:CCM:DP:FT:GQ:INF:QL","0/0:.:Reference:13:.:PASS:.:NCIG:.","0/1:7,38:Somatic:13:45:PASS:99:SOMATIC:1449.77"});
		List<VcfRecord> vcfs = Arrays.asList(v1,v2);
		Optional<VcfRecord> oVcf = PipelineUtil.createCompoundSnpGATK(vcfs);
		assertEquals(false, oVcf.isPresent());
	}
	
	//TOTO awaiting decision on whether 1/1 -> 0/0 is SOMATIC
//	@Test
//	public void csRealLife2() {
//		/*
//		 * chr10	54817257	rs386743785	AG	GA	.	.	IN=1,2;DB;HOM=0,TTTAACCTTCgaCTTGCCCACA	GT:AD:CCC:CCM:DP:FT:INF:MR:NNS:OABS	1/1:0,19:Germline:32:34:PASS:.:19:19:AA8[]6[];GA8[]11[];TA0[]1[]	0/0:2,0:ReferenceNoVariant:32:55:PASS:SOMATIC:.:.:AA33[]20[];AG1[]1[];A_2[]0[]	1/1:0,19:Germline:32:34:PASS:.:19:19:AA8[]6[];GA8[]11[];TA0[]1[]	0/0:2,0:ReferenceNoVariant:32:55:PASS:SOMATIC:.:.:AA33[]20[];AG1[]1[];A_2[]0[]
//		 * 
//		 * This should not be SOMATIC
//		 */
//		VcfRecord v1 = VcfUtils.createVcfRecord(new ChrPointPosition("10", 54817257),null,"A","G");
//		VcfRecord v2 = VcfUtils.createVcfRecord(new ChrPointPosition("10", 54817258),null,"G","A");
//		final Accumulator tumour100 = new Accumulator(54817257);
//		final Accumulator tumour101 = new Accumulator(54817258);
//		for (int i = 1 ; i <= 34 ; i++) {
//			tumour100.addBase((byte)'A', (byte)30, true, 154701261 + i, 154701381, 154701391, i);
//			tumour101.addBase((byte)'A', (byte)30, true, 154701261 + i, 154701382, 154701391, i);
//		}
//		for (int i = 35 ; i < 35 + 31 ; i++) {
//			tumour100.addBase((byte)'A', (byte)30, false, 154701261 + i, 154701381, 154701391 + i, i);
//			tumour101.addBase((byte)'A', (byte)30, false, 154701261 + i, 154701382, 154701391 + i, i);
//		}
//		
//		tumour100.addBase((byte)'A', (byte)30, true, 154701262, 154701381, 154701391, 70);
//		tumour101.addBase((byte)'C', (byte)30, true, 154701262, 154701382, 154701391, 70);
//		tumour100.addBase((byte)'C', (byte)30, true, 154701262, 154701381, 154701391, 71);
//		tumour101.addBase((byte)'A', (byte)30, true, 154701262, 154701382, 154701391, 71);
//		
//		Map<VcfRecord, Pair<Accumulator, Accumulator>> map = new HashMap<>(4);
//		map.put(v1,  new Pair<>(null, tumour100));
//		map.put(v2,  new Pair<>(null, tumour101));
//		
//		Optional<VcfRecord> ov = PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3); 
//		assertEquals(true, ov.isPresent());
//		VcfRecord v = ov.get();
//		assertEquals("AA", v.getAlt());
//		assertEquals("GG", v.getRef());
//		List<String> ff = v.getFormatFields();
//		assertEquals("1/1:0,65:67:.:SOMATIC:65:65:AA34[]31[];AC1[]0[];CA1[]0[]", ff.get(2));	// tumour
//	}
	
	@Test
	public void getSkeletonVcf() {
		List<VcfRecord> snps = new ArrayList<>();
		snps.add(VcfUtils.createVcfRecord(new ChrPointPosition("1", 99),null,"A","C"));
		snps.add(VcfUtils.createVcfRecord(new ChrPointPosition("1", 101),null,"A","C"));
		snps.add(VcfUtils.createVcfRecord(new ChrPointPosition("1", 102),null,"A","C"));
		snps.add(VcfUtils.createVcfRecord(new ChrPointPosition("1", 103),null,"A","C"));
		snps.add(VcfUtils.createVcfRecord(new ChrPointPosition("1", 104),null,"A","C"));
		snps.add(VcfUtils.createVcfRecord(new ChrPointPosition("1", 106),null,"A","C"));
		
		List<List<VcfRecord>> loloVcfs = PipelineUtil.listOfListOfAdjacentVcfs(snps); 
		assertEquals(1, loloVcfs.size());
		VcfRecord v = PipelineUtil.createSkeletonCompoundSnp(loloVcfs.get(0));
		assertEquals("1", v.getChrPosition().getChromosome());
		assertEquals(101, v.getChrPosition().getStartPosition());
		assertEquals("AAAA", v.getRef());
		assertEquals("CCCC", v.getAlt());
		
	}
}
