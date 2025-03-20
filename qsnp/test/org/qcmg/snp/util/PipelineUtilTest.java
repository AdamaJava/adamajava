package org.qcmg.snp.util;

import java.util.*;

import org.junit.Assert;
import org.junit.Ignore;
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

import gnu.trove.list.TIntList;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TLongIntHashMap;

import static org.junit.Assert.*;

public class PipelineUtilTest {
	
	public static final List<Rule> cRules = Arrays.asList(new Rule(0,20,3), new Rule(21,50,4), new Rule(51,Integer.MAX_VALUE,10));
	public static final List<Rule> tRules = Arrays.asList(new Rule(0,20,3), new Rule(21,50,4), new Rule(51,Integer.MAX_VALUE,5));
	
	
	@Test
	public void getLoLoRecs() {
        assertNull(PipelineUtil.listOfListOfAdjacentVcfs(null));
        assertTrue(PipelineUtil.listOfListOfAdjacentVcfs(new ArrayList<>()).isEmpty());
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

        assertTrue(PipelineUtil.listOfListOfAdjacentVcfs(snps).isEmpty());
	}
	
	@Test
	public void getLoLoRecsDiffClassification() {
		VcfRecord v1 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 100),null,"A","C");
		VcfRecord v2 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 101),null,"C","G");
		v2.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		
		/*
		 * 1 som 1 germ
		 */
		List<VcfRecord> snps = new ArrayList<>();
		snps.add(v1);
		snps.add(v2);

        assertTrue(PipelineUtil.listOfListOfAdjacentVcfs(snps).isEmpty());
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
        assertTrue(PipelineUtil.listOfListOfAdjacentVcfs(snps).isEmpty());
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
	public void getUniquesLong() {
		TLongIntMap m  = new TLongIntHashMap();
		TLongList l  = new TLongArrayList();
		assertEquals(0, PipelineUtil.getUniqueCount(m, l, true));
		assertEquals(0, PipelineUtil.getUniqueCount(m, l, false));
		m.put(100, 10);
		assertEquals(0, PipelineUtil.getUniqueCount(m, l, true));
		assertEquals(0, PipelineUtil.getUniqueCount(m, l, false));
		l.add(100);
		assertEquals(1, PipelineUtil.getUniqueCount(m, l, true));
		assertEquals(0, PipelineUtil.getUniqueCount(m, l, false));
		l.add(101);
		assertEquals(1, PipelineUtil.getUniqueCount(m, l, true));
		assertEquals(0, PipelineUtil.getUniqueCount(m, l, false));
		
		/*
		 * add a reverse strand entry
		 */
		m.put(101, -20);
		assertEquals(1, PipelineUtil.getUniqueCount(m, l, true));
		assertEquals(1, PipelineUtil.getUniqueCount(m, l, false));
		m.put(101, -20);
		assertEquals(1, PipelineUtil.getUniqueCount(m, l, true));
		assertEquals(1, PipelineUtil.getUniqueCount(m, l, false));
	}
	
	@Test
	public void getLoLoRecsSameChrLongWayAway() {
		List<VcfRecord> snps = new ArrayList<>();
		snps.add(VcfUtils.createVcfRecord("1", 100));
		snps.add(VcfUtils.createVcfRecord("1", 200));
		snps.add(VcfUtils.createVcfRecord("1", 300));

        assertTrue(PipelineUtil.listOfListOfAdjacentVcfs(snps).isEmpty());
	}
	@Test
	public void getLoLoRecsSameChrShortWayAway() {
		List<VcfRecord> snps = new ArrayList<>();
		snps.add(VcfUtils.createVcfRecord("1", 100));
		snps.add(VcfUtils.createVcfRecord("1", 102));
		snps.add(VcfUtils.createVcfRecord("1", 104));
		snps.add(VcfUtils.createVcfRecord("1", 106));
		snps.add(VcfUtils.createVcfRecord("1", 108));

        assertTrue(PipelineUtil.listOfListOfAdjacentVcfs(snps).isEmpty());
	}
	
	@Test
	public void getLoLoRecsSameChr() {
		List<VcfRecord> snps = new ArrayList<>();
		snps.add(VcfUtils.createVcfRecord("1", 100));
		snps.add(VcfUtils.createVcfRecord("1", 101));
		snps.add(VcfUtils.createVcfRecord("1", 300));
		
		assertEquals(1, PipelineUtil.listOfListOfAdjacentVcfs(snps).size());
		assertEquals(2, PipelineUtil.listOfListOfAdjacentVcfs(snps).getFirst().size());
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
		assertEquals(5, PipelineUtil.listOfListOfAdjacentVcfs(snps).getFirst().size());
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
		assertEquals(6, PipelineUtil.listOfListOfAdjacentVcfs(snps).getFirst().size());
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
		assertEquals(4, PipelineUtil.listOfListOfAdjacentVcfs(snps).getFirst().size());
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
		
		assertEquals("ABC", PipelineUtil.getReference(snps).orElse(null));
	}
	
	@Test
	public void doesStringContainLC() {
        assertFalse(PipelineUtil.isStringLowerCase(null));
        assertFalse(PipelineUtil.isStringLowerCase(""));
        assertFalse(PipelineUtil.isStringLowerCase("."));
        assertFalse(PipelineUtil.isStringLowerCase("_"));
        assertFalse(PipelineUtil.isStringLowerCase("_A"));
        assertFalse(PipelineUtil.isStringLowerCase("A_"));
        assertFalse(PipelineUtil.isStringLowerCase("A_B"));
        assertTrue(PipelineUtil.isStringLowerCase("a"));
        assertTrue(PipelineUtil.isStringLowerCase("_a"));
        assertTrue(PipelineUtil.isStringLowerCase("_a_"));
        assertTrue(PipelineUtil.isStringLowerCase("__x"));
	}
	
	@Test
	public void getMR() {
		Accumulator acc1 = new Accumulator(150);
		acc1.addBase((byte)'G', (byte) 1, true, 100, 150, 200, 1);
		acc1.addBase((byte)'G', (byte) 1, true, 100, 150, 200, 2);
		acc1.addBase((byte)'G', (byte) 1, false, 100, 150, 200, 3);
		List<Accumulator> accs = List.of(acc1);
		
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
		List<Accumulator> accs = List.of(acc1);
		
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
		} catch (IllegalArgumentException iae){}

        assertTrue(PipelineUtil.getBasesFromAccumulators(null).isEmpty());
        assertTrue(PipelineUtil.getBasesFromAccumulators(new ArrayList<>()).isEmpty());
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
		
		List<String> control = List.of("ABC");
		
		altsGTs = PipelineUtil.getAltStringAndGenotypes(control, null, "XYZ");
		assertEquals(3, altsGTs.size());
		assertEquals("ABC", altsGTs.get(0));
		assertEquals("1/1", altsGTs.get(1));
		assertEquals(Constants.MISSING_GT, altsGTs.get(2));
		
		List<String> test = List.of("123");
		
		altsGTs = PipelineUtil.getAltStringAndGenotypes(null, test, "XYZ");
		assertEquals(3, altsGTs.size());
		assertEquals("123", altsGTs.get(0));
		assertEquals(Constants.MISSING_GT, altsGTs.get(1));
		assertEquals("1/1", altsGTs.get(2));
	}
	
	@Test
	public void getAltsAndGTs() {
		List<String> control = List.of("ABC");
		List<String> test = List.of("ABC");
		
		List<String> altsGTs = PipelineUtil.getAltStringAndGenotypes(control, test, "XYZ");
		assertEquals(3, altsGTs.size());
		assertEquals("ABC", altsGTs.get(0));
		assertEquals("1/1", altsGTs.get(1));
		assertEquals("1/1", altsGTs.get(2));
		
		control = List.of("XYZ");
		test = List.of("ABC");
		altsGTs = PipelineUtil.getAltStringAndGenotypes(control, test, "XYZ");
		assertEquals(3, altsGTs.size());
		assertEquals("ABC", altsGTs.get(0));
		assertEquals("0/0", altsGTs.get(1));
		assertEquals("1/1", altsGTs.get(2));
		
		control = List.of("XYZ");
		test = List.of("XYZ");
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
		List<String> control = List.of("AG");
		List<String> test = List.of("AG");
		
		List<String> altsGTs = PipelineUtil.getAltStringAndGenotypes(control, test, "GG");
		assertEquals(3, altsGTs.size());
		assertEquals(Constants.MISSING_DATA_STRING, altsGTs.get(0));
		assertEquals(Constants.MISSING_GT, altsGTs.get(1));
		assertEquals(Constants.MISSING_GT, altsGTs.get(2));
	}
	
	@Test
	public void getEmptyOABS() {
        assertFalse(PipelineUtil.getOABS(null).isPresent());
		Map<String, short[]> basesAndCounts = new HashMap<>();
        assertFalse(PipelineUtil.getOABS(basesAndCounts).isPresent());
		
		basesAndCounts.put("XYZ", new short[]{});
        assertFalse(PipelineUtil.getOABS(basesAndCounts).isPresent());
		
		basesAndCounts.put("XYZ", new short[4]);
        assertTrue(PipelineUtil.getOABS(basesAndCounts).isPresent());
		assertEquals("XYZ0[]0[]", PipelineUtil.getOABS(basesAndCounts).get());
		
		basesAndCounts.get("XYZ")[0] = 1;
		basesAndCounts.get("XYZ")[2] = 1;
		assertEquals("XYZ1[]1[]", PipelineUtil.getOABS(basesAndCounts).get());
	}
	
	@Test
	public void getOABS() {
		Map<String, short[]> basesAndCounts = new HashMap<>();
		
		basesAndCounts.put("XYZ", new short[]{10,5,15,6});
		assertEquals("XYZ10[]15[]", PipelineUtil.getOABS(basesAndCounts).orElse(null));
		basesAndCounts.put("ABC", new short[]{21,2,14,1});
		assertEquals("ABC21[]14[];XYZ10[]15[]", PipelineUtil.getOABS(basesAndCounts).orElse(null));
	}
	
	@Test
	public void getBasesForGenotype() {
		Map<String, short[]> basesAndCounts = new HashMap<>();
		basesAndCounts.put("XYZ", new short[]{10,3,11,10});
		assertEquals(1, PipelineUtil.getBasesForGenotype(basesAndCounts, 10,"AAA").size());
		assertEquals("XYZ", PipelineUtil.getBasesForGenotype(basesAndCounts, 10,"AAA").getFirst());
		basesAndCounts.put("ABC", new short[]{5,2,11,10});
		assertEquals(1, PipelineUtil.getBasesForGenotype(basesAndCounts, 10,"AAA").size());
		assertEquals("XYZ", PipelineUtil.getBasesForGenotype(basesAndCounts, 10,"AAA").getFirst());
		assertEquals(2, PipelineUtil.getBasesForGenotype(basesAndCounts, 10,"DDD").size());
		assertEquals("XYZ", PipelineUtil.getBasesForGenotype(basesAndCounts, 10,"DDD").get(0));
		assertEquals("ABC", PipelineUtil.getBasesForGenotype(basesAndCounts, 10,"DDD").get(1));
		basesAndCounts.put("HBH", new short[]{0,0,16,1});
		assertEquals(2, PipelineUtil.getBasesForGenotype(basesAndCounts, 10,"AAA").size());
		assertEquals("XYZ", PipelineUtil.getBasesForGenotype(basesAndCounts, 10,"AAA").get(0));
		assertEquals("HBH", PipelineUtil.getBasesForGenotype(basesAndCounts, 10,"AAA").get(1));
	}

	@Test
	public void getBasesForGenotype2() {
		Map<String, short[]> basesAndCounts = new HashMap<>();
		basesAndCounts.put("CG", new short[]{11,11,0,0});
		basesAndCounts.put("TG", new short[]{1,1,0,0});
		basesAndCounts.put("CA", new short[]{6,6,0,0});
		assertEquals(1, PipelineUtil.getBasesForGenotype(basesAndCounts, 3,"TG").size());
		assertEquals("CA", PipelineUtil.getBasesForGenotype(basesAndCounts, 3,"TG").getFirst());
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
		
		VcfRecord v = PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3).orElse(null);
        assert v != null;
        assertEquals(origV.getChrPosition(), v.getChrPosition());
		assertEquals("C", v.getAlt());
		assertEquals("A", v.getRef());
		assertEquals("GT:AD:DP:FF:FT:INF:NNS:OABS", v.getFormatFields().get(0));
		assertEquals("0/0:3,0:3:.:.:.:.:A2[]1[]", v.getFormatFields().get(1));
		assertEquals("1/1:0,3:3:.:.:SOMATIC:2:C2[]1[]", v.getFormatFields().get(2));
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
		
		VcfRecord v = PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3).orElse(null);
        assert v != null;
        assertEquals(2, v.getChrPosition().getLength());
		assertEquals("GT", v.getAlt());
		assertEquals("AC", v.getRef());
		assertEquals("GT:AD:DP:FF:FT:INF:NNS:OABS", v.getFormatFields().get(0));
		assertEquals("0/0:3,0:3:.:.:.:.:AC2[]1[]", v.getFormatFields().get(1));
		assertEquals("1/1:0,3:3:.:.:SOMATIC:2:GT2[]1[]", v.getFormatFields().get(2));
		
		/*
		 * add some noise
		 */
		controlAcc100.addBase((byte)'T', (byte) 1, false, 100, 100, 200, 4);
		controlAcc100.addBase((byte)'T', (byte) 1, false, 100, 100, 200, 5);
		controlAcc100.addBase((byte)'T', (byte) 1, false, 100, 100, 200, 6);
		
		 v = PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3).orElse(null);
        assert v != null;
        assertEquals(2, v.getChrPosition().getLength());
		assertEquals("GT", v.getAlt());
//		assertEquals("T_,GT", v.getAlt());
		assertEquals("AC", v.getRef());
		assertEquals("GT:AD:DP:FF:FT:INF:NNS:OABS", v.getFormatFields().get(0));
		assertEquals("0/0:3,0:3:.:.:.:.:AC2[]1[];T_0[]3[]", v.getFormatFields().get(1));
		assertEquals("1/1:0,3:3:.:.:SOMATIC:2:GT2[]1[]", v.getFormatFields().get(2));
	}
	
	@Test
	public void containsRef() {
		/*
		 * chr1    16862501        .       TA      CG      .       PASS    .       ACCS    CG,28,7,C_,1,0,TA,48,15,TG,65,38,_G,1,2,_A,0,1  CG,22,4,TA,37,14,TG,52,18,_G,1,1,C_,0,1L);
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
		assertEquals("TAG", basesForGenotype.getFirst());
	}
	
	@Test
	public void compoundSnp() {
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
        assertFalse(PipelineUtil.createCompoundSnp(map, cRules, tRules, true, 3, 3).isPresent());
		
		// need 4 reads with the cs to register
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 2);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 2);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 3);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 3);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 4);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 4);
		Optional<VcfRecord> ov = PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3);
        assertTrue(ov.isPresent());
		VcfRecord v = ov.get();
	
		List<String> ff = v.getFormatFields();
        assertTrue(ff.get(2).contains("CG4[]0[]"));	// tumour
		assertEquals("./.:.:0:.:.:.:.:.", ff.get(1));	// control
	}
	
	@Test
	public void compoundSnpOneGermlineOneSomatic() {
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
        assertFalse(PipelineUtil.createCompoundSnp(map, cRules, tRules, true, 3, 3).isPresent());
		
		// need 4 reads with the cs to register
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 2);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 2);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 3);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 3);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 4);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 4);
		Optional<VcfRecord> ov = PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3);
        assertTrue(ov.isPresent());
		VcfRecord v = ov.get();
		
		List<String> ff = v.getFormatFields();
        assertTrue(ff.get(2).contains("CG4[]0[]"));	// tumour
		assertEquals("./.:.:0:.:.:.:.:.", ff.get(1));	// control
	}
	
	@Test
	public void noCompoundSnpMissingAccs() {
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
	public void noCompoundSnpRefInAlt() {
		/*
		 * Don't want this happening
		 * chr1    985449  .       GG      AG      .       .       .       GT:DP:MR:OABS   1/1:12:9:AG1[]8[];GA2[]0[];_G1[]0[]     1/1:10:6:AG3[]3[];GA2[]1[];_G1[]0[]
		 * which was made up from
		 * chr1    985449  rs56255212      G       A       421.77  PASS    AC=2;AF=1.00;AN=2;DB;DP=10;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=31.82;SOR=2.303        GT:AD:DP:GQ:PL:GD:AC:OABS:MR:NNS        1/1:0,10:10:30:450,30,0:A/A:A1[37],8[35.12],G2[38],0[0]:.:9:9   0/1:3,6:9:99:208,0,138:A/G:A3[34],3[38.67],G2[37],1[35]:.:6:5L);
		 * chr1    985450  .       G       A       67.77   MIN;MR;NNS      SOMATIC GT:AD:DP:GQ:PL:GD:AC:OABS:MR:NNS        .:.:.:.:.:G/G:A2[37],0[0],G2[37],8[36.5]:.:2:2  0/1:7,3:10:96:96,0,236:A/G:A2[35],1[35],G4[36.5],3[39]:.:3:3L);
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
        assertFalse(PipelineUtil.createCompoundSnp(map, cRules, tRules, true, 3, 3).isPresent());
		
	}
	
	@Test
	public void compoundSnpReverseStrand() {
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
        assertFalse(PipelineUtil.createCompoundSnp(map, cRules, tRules, true, 3, 3).isPresent());
		
		// need 4 reads with the cs to register
		tumour100.addBase((byte)'C', (byte)30, false, 100, 100, 200, 2);
		tumour101.addBase((byte)'G', (byte)30, false, 101, 101, 200, 2);
		tumour100.addBase((byte)'C', (byte)30, false, 100, 100, 200, 3);
		tumour101.addBase((byte)'G', (byte)30, false, 101, 101, 200, 3);
		tumour100.addBase((byte)'C', (byte)30, false, 100, 100, 200, 4);
		tumour101.addBase((byte)'G', (byte)30, false, 101, 101, 200, 4);
		Optional<VcfRecord> ov = PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3);
        assertTrue(ov.isPresent());
		VcfRecord v = ov.get();
		
		List<String> ff = v.getFormatFields();
        assertTrue(ff.get(2).contains("CG0[]4[]"));	// tumour
		assertEquals("./.:.:0:.:.:.:.:.", ff.get(1));	// control
	}
	
	@Test
	public void compoundSnpBothStrands() {
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
        assertFalse(PipelineUtil.createCompoundSnp(map, cRules, tRules, true, 3, 3).isPresent());
		
		// need 4 reads with the cs to register
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 2);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 2);
		tumour100.addBase((byte)'C', (byte)30, false, 100, 100, 200, 3);
		tumour101.addBase((byte)'G', (byte)30, false, 101, 101, 200, 3);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 4);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 4);
		
		Optional<VcfRecord> ov = PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3);
        assertTrue(ov.isPresent());
		VcfRecord v = ov.get();
		List<String> ff = v.getFormatFields();
        assertTrue(ff.get(2).contains("CG2[]2[]"));	// tumour
		assertEquals("./.:.:0:.:.:.:.:.", ff.get(1));	// control
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
        assertFalse(PipelineUtil.createCompoundSnp(map, cRules, tRules, true, 3, 3).isPresent());
		
		// need 4 reads with the cs to register
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 2);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 2);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 3);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 3);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 4);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 4);
		tumour101.addBase((byte)'G', (byte)30, true, 101, 101, 200, 5);

		Optional<VcfRecord> ov = PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3);
        assertTrue(ov.isPresent());
		VcfRecord v = ov.get();
		List<String> ff = v.getFormatFields();
		assertEquals("./.:.:0:.:.:.:.:.", ff.get(1));	// control
        assertTrue(ff.get(2).contains("CG4[]0[];_G1[]0[]"));	// tumour
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
        assertFalse(PipelineUtil.createCompoundSnp(map, cRules, tRules, true, 3, 3).isPresent());
		
		// need 4 reads with the cs to register
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 2);
		tumour101.addBase((byte)'G', (byte)30, true, 100, 101, 200, 2);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 3);
		tumour101.addBase((byte)'G', (byte)30, true, 100, 101, 200, 3);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 4);
		tumour101.addBase((byte)'G', (byte)30, true, 100, 101, 200, 4);
		tumour100.addBase((byte)'C', (byte)30, true, 100, 100, 200, 5);
		
		Optional<VcfRecord> ov = PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3);
        assertTrue(ov.isPresent());
		VcfRecord v = ov.get();
		assertEquals("AC", v.getRef());
		assertEquals("CG", v.getAlt());
		List<String> ff = v.getFormatFields();
		assertEquals("./.:.:0:.:.:.:.:.", ff.get(1));	// control
		/*
		 * filters are now applied in qannotate
		 */
		assertEquals("1/1:0,4:4:.:.:SOMATIC:1:CG4[]0[];C_1[]0[]", ff.get(2));	// tumour
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
		VcfRecord v1 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 154701381), null, "G", "A");
		v1.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		VcfRecord v2 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 154701382), null, "G", "A");
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
        assertTrue(ov.isPresent());
		VcfRecord v = ov.get();
		assertEquals("AA", v.getAlt());
		assertEquals("GG", v.getRef());
		List<String> ff = v.getFormatFields();
		assertEquals("1/1:0,65:67:.:.:SOMATIC:65:AA34[]31[];AC1[]0[];CA1[]0[]", ff.get(2));	// tumour
	}
	
	@Test
	public void csRealLife2() {
		/*
		 * chr1    79760719        .       TG      CA      .       .       .       GT:AD:DP:FT:INF:NNS:OABS        1/1:0,5:12:.:.:5:CA5[]0[];CG7[]0[]      1/1:0,5:24:.:.:5:CA5[]0[];CG19[]0[];_G1[]0[]
		 * 
		 * I think this should be TG -> CA,CG with 1/2 in the GT fields for both samples
		 * This needs some thought and a separate ticket
		 */
		VcfRecord v1 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 154701381),null,"T","C");
		v1.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		VcfRecord v2 = VcfUtils.createVcfRecord(new ChrPointPosition("1", 154701382),null,"G","A");
		v2.setInfo(VcfHeaderUtils.INFO_SOMATIC);
		final Accumulator tumour100 = new Accumulator(154701381);
		final Accumulator tumour101 = new Accumulator(154701382);
		for (int i = 1 ; i <= 5 ; i++) {
			tumour100.addBase((byte)'C', (byte)30, true, 154701261 + i, 154701381, 154701391, i);
			tumour101.addBase((byte)'A', (byte)30, true, 154701261 + i, 154701382, 154701391, i);
		}
		for (int i = 6 ; i <= 12 ; i++) {
			tumour100.addBase((byte)'C', (byte)30, false, 154701261 + i, 154701381, 154701391 + i, i);
			tumour101.addBase((byte)'G', (byte)30, false, 154701261 + i, 154701382, 154701391 + i, i);
		}
		
		tumour100.addBase((byte)'A', (byte)30, true, 154701262, 154701381, 154701391, 70);
		tumour101.addBase((byte)'C', (byte)30, true, 154701262, 154701382, 154701391, 70);
		tumour100.addBase((byte)'C', (byte)30, true, 154701262, 154701381, 154701391, 71);
		tumour101.addBase((byte)'A', (byte)30, true, 154701262, 154701382, 154701391, 71);
		
		Map<VcfRecord, Pair<Accumulator, Accumulator>> map = new HashMap<>(4);
		map.put(v1,  new Pair<>(null, tumour100));
		map.put(v2,  new Pair<>(null, tumour101));
		
		Optional<VcfRecord> ov = PipelineUtil.createCompoundSnp(map, cRules,tRules, true, 3, 3);
        assertTrue(ov.isPresent());
		VcfRecord v = ov.get();
		assertEquals("CA", v.getAlt());
		assertEquals("TG", v.getRef());
//		List<String> ff = v.getFormatFields();
//		assertEquals("1/1:0,65:67:.:SOMATIC:65:AA34[]31[];AC1[]0[];CA1[]0[]", ff.get(2));	// tumour
	}
	
	@Test
	public void csGATK() {
		VcfRecord v1 = new VcfRecord(new String[]{"chr1","39592384",".","G","T",".",".","BaseQRankSum=-0.893;ClippingRankSum=0.266;DP=46;FS=14.005;MQ=60.00;MQRankSum=1.801;QD=31.52;ReadPosRankSum=-0.517;SOR=0.028;IN=2;HOM=3,ACTTGAGCTTtGGAGGCAGAG;","GT:AD:CCC:CCM:DP:FT:GQ:INF:QL","./.:.:Reference:13:.:PASS:.:NCIG:.","0/1:7,38:Somatic:13:45:PASS:99:SOMATIC:1449.77"});
		VcfRecord v2 = new VcfRecord(new String[]{"chr1","39592385",".","G","A",".",".","BaseQRankSum=0.767;ClippingRankSum=-0.266;DP=46;FS=14.005;MQ=60.00;MQRankSum=-0.423;QD=31.52;ReadPosRankSum=-0.611;SOR=0.028;IN=2;HOM=0,CTTGAGCTTGaGAGGCAGAGA;","GT:AD:CCC:CCM:DP:FT:GQ:INF:QL","./.:.:Reference:13:.:PASS:.:NCIG:.","0/1:7,38:Somatic:13:45:PASS:99:SOMATIC:1449.77"});
		List<VcfRecord> vcfs = Arrays.asList(v1,v2);
		Optional<VcfRecord> oVcf = PipelineUtil.createCompoundSnpGATK(vcfs);
        assertTrue(oVcf.isPresent());
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
        assertTrue(oVcf.isPresent());
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
        assertTrue(oVcf.isPresent());
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
        assertTrue(oVcf.isPresent());
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
        assertTrue(oVcf.isPresent());
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
        assertTrue(oVcf.isPresent());
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
        assertTrue(oVcf.isPresent());
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
        assertFalse(oVcf.isPresent());
	}
	
	@Test
	public void csGATKSingleSample() {
		VcfRecord v1 = new VcfRecord(new String[]{"chrY","13487853",".","G","A,C","107.28",".","AC=2;AF=1.00;AN=2;DP=0;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=0.00;SOR=0.693","GT:AD:DP:GQ:INF:QL","0/2:.:.:9:.:."});
		VcfRecord v2 = new VcfRecord(new String[]{"chrY","13487854",".","C","A,T","107.28",".","AC=2;AF=1.00;AN=2;DP=0;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=0.00;SOR=0.693","GT:AD:DP:GQ:INF:QL","0/2:.:.:9:.:."});
		List<VcfRecord> vcfs = Arrays.asList(v1,v2);
		Optional<VcfRecord> oVcf = PipelineUtil.createCompoundSnpGATK(vcfs, true);
        assertFalse(oVcf.isPresent());
	}
	
	@Test
	public void csGATKOneSomaticOneGermline() {
		VcfRecord v1 = new VcfRecord(new String[]{"chr1","39592384",".","G","T",".",".","BaseQRankSum=-0.893;ClippingRankSum=0.266;DP=46;FS=14.005;MQ=60.00;MQRankSum=1.801;QD=31.52;ReadPosRankSum=-0.517;SOR=0.028;IN=2;HOM=3,ACTTGAGCTTtGGAGGCAGAG;","GT:AD:CCC:CCM:DP:FT:GQ:INF:QL","0/0:.:Reference:13:.:PASS:.:NCIG:.","0/1:7,38:Somatic:13:45:PASS:99:.:1449.77"});
		VcfRecord v2 = new VcfRecord(new String[]{"chr1","39592385",".","G","A",".",".","BaseQRankSum=0.767;ClippingRankSum=-0.266;DP=46;FS=14.005;MQ=60.00;MQRankSum=-0.423;QD=31.52;ReadPosRankSum=-0.611;SOR=0.028;IN=2;HOM=0,CTTGAGCTTGaGAGGCAGAGA;","GT:AD:CCC:CCM:DP:FT:GQ:INF:QL","0/0:.:Reference:13:.:PASS:.:NCIG:.","0/1:7,38:Somatic:13:45:PASS:99:SOMATIC:1449.77"});
		List<VcfRecord> vcfs = Arrays.asList(v1,v2);
		Optional<VcfRecord> oVcf = PipelineUtil.createCompoundSnpGATK(vcfs);
        assertFalse(oVcf.isPresent());
	}

	@Test
	public void testGetFailedFilterCS_NullAccumulators() {
		// When input is null
		String result = PipelineUtil.getFailedFilterCS(null);

		// Expecting MISSING_DATA_STRING
		Assert.assertEquals(Constants.MISSING_DATA_STRING, result);
	}

	@Test
	public void testGetFailedFilterCS_EmptyAccumulators() {
		// When input is an empty list
		List<Accumulator> accumulators = Collections.emptyList();
		String result = PipelineUtil.getFailedFilterCS(accumulators);

		// Expecting empty output
		Assert.assertEquals(".", result);
	}

	@Test
	public void testGetFailedFilterCS_SingleAccumulator() {
		// Mocking a single Accumulator
		Accumulator accumulator = new Accumulator(12345);
		for (int i = 1 ; i < 4 ; i++) {
			accumulator.addFailedFilterBase((byte) 'A', i);
		}
		for (int i = 4 ; i < 6 ; i++) {
			accumulator.addFailedFilterBase((byte) 'C', i);
		}
		for (int i = 6 ; i < 7 ; i++) {
			accumulator.addFailedFilterBase((byte) 'G', i);
		}
		for (int i = 7 ; i < 8 ; i++) {
			accumulator.addFailedFilterBase((byte) 'T', i);
		}
		assertEquals("A3;C2;G1;T1", PipelineUtil.getFailedFilterCS(Collections.singletonList(accumulator)));
	}

	@Test
	public void testGetFailedFilterCS_MultipleAccumulators() {
		// Mocking two Accumulators
		Accumulator acc1 = new Accumulator(12345);
		for (int i = 1 ; i < 4 ; i++) {
			acc1.addFailedFilterBase((byte) 'A', i);
		}
		for (int i = 4 ; i < 6 ; i++) {
			acc1.addFailedFilterBase((byte) 'C', i);
		}
		Accumulator acc2 = new Accumulator(12346);
		for (int i = 1 ; i < 4 ; i++) {
			acc2.addFailedFilterBase((byte) 'G', i);
		}
		for (int i = 4 ; i < 6 ; i++) {
			acc2.addFailedFilterBase((byte) 'T', i);
		}
		List<Accumulator> accumulators = Arrays.asList(acc1, acc2);
		assertEquals("AG3;CT2", PipelineUtil.getFailedFilterCS(accumulators));

		for (int i = 6 ; i < 10 ; i++) {
			acc1.addFailedFilterBase((byte) 'T', i);
		}
		for (int i = 10 ; i < 11 ; i++) {
			acc2 .addFailedFilterBase((byte) 'T', i);
		}
		assertEquals("AG3;CT2;T_4;_T1", PipelineUtil.getFailedFilterCS(accumulators));

	}

	@Test
	public void testGetFailedFilterCS_Triple() {
		// Mocking multiple Accumulators
		Accumulator acc1 = new Accumulator(12345);
		acc1.addFailedFilterBase((byte) 'A', 1);
		Accumulator acc2 = new Accumulator(12346);
		acc2.addFailedFilterBase((byte) 'C', 1);
		Accumulator acc3 = new Accumulator(12347);
		acc3.addFailedFilterBase((byte) 'G', 1);

		List<Accumulator> accumulators = Arrays.asList(acc1, acc2, acc3);
		assertEquals("ACG1", PipelineUtil.getFailedFilterCS(accumulators));

		acc1.addFailedFilterBase((byte) 'A', 2);
		assertEquals("ACG1;A__1", PipelineUtil.getFailedFilterCS(accumulators));
		acc2.addFailedFilterBase((byte) 'C', 3);
		assertEquals("ACG1;A__1;_C_1", PipelineUtil.getFailedFilterCS(accumulators));
		acc3.addFailedFilterBase((byte) 'G', 4);
		assertEquals("ACG1;A__1;_C_1;__G1", PipelineUtil.getFailedFilterCS(accumulators));
		acc3.addFailedFilterBase((byte) 'G', 2);
		assertEquals("ACG1;A_G1;_C_1;__G1", PipelineUtil.getFailedFilterCS(accumulators));
	}

	@Test
	public void testGetFailedFilterCS_RealLife() {
		// Mocking two Accumulators
		Accumulator acc1 = new Accumulator(12345);

		acc1.addFailedFilterBase((byte) 'C', 391785L);
		acc1.addFailedFilterBase((byte) 'C', 391815L);
		acc1.addFailedFilterBase((byte) 'C', 391817L);
		acc1.addFailedFilterBase((byte) 'C', 391832L);
		acc1.addFailedFilterBase((byte) 'C', 391843L);
		acc1.addFailedFilterBase((byte) 'C', 391867L);
		acc1.addFailedFilterBase((byte) 'T', 391748L);
		acc1.addFailedFilterBase((byte) 'T', 391752L);
		acc1.addFailedFilterBase((byte) 'T', 391754L);
		acc1.addFailedFilterBase((byte) 'T', 391755L);
		acc1.addFailedFilterBase((byte) 'T', 391756L);
		acc1.addFailedFilterBase((byte) 'T', 391757L);
		acc1.addFailedFilterBase((byte) 'T', 391760L);
		acc1.addFailedFilterBase((byte) 'T', 391761L);
		acc1.addFailedFilterBase((byte) 'T', 391762L);
		acc1.addFailedFilterBase((byte) 'T', 391763L);
		acc1.addFailedFilterBase((byte) 'T', 391765L);
		acc1.addFailedFilterBase((byte) 'T', 391766L);
		acc1.addFailedFilterBase((byte) 'T', 391768L);
		acc1.addFailedFilterBase((byte) 'T', 391769L);
		acc1.addFailedFilterBase((byte) 'T', 391770L);
		acc1.addFailedFilterBase((byte) 'T', 391771L);
		acc1.addFailedFilterBase((byte) 'T', 391773L);
		acc1.addFailedFilterBase((byte) 'T', 391776L);
		acc1.addFailedFilterBase((byte) 'T', 391777L);
		acc1.addFailedFilterBase((byte) 'T', 391778L);
		acc1.addFailedFilterBase((byte) 'T', 391779L);
		acc1.addFailedFilterBase((byte) 'T', 391780L);
		acc1.addFailedFilterBase((byte) 'T', 391782L);
		acc1.addFailedFilterBase((byte) 'T', 391783L);
		acc1.addFailedFilterBase((byte) 'T', 391787L);
		acc1.addFailedFilterBase((byte) 'T', 391788L);
		acc1.addFailedFilterBase((byte) 'T', 391789L);
		acc1.addFailedFilterBase((byte) 'T', 391790L);
		acc1.addFailedFilterBase((byte) 'T', 391791L);
		acc1.addFailedFilterBase((byte) 'T', 391792L);
		acc1.addFailedFilterBase((byte) 'T', 391795L);
		acc1.addFailedFilterBase((byte) 'T', 391796L);
		acc1.addFailedFilterBase((byte) 'T', 391797L);
		acc1.addFailedFilterBase((byte) 'T', 391799L);
		acc1.addFailedFilterBase((byte) 'T', 391800L);
		acc1.addFailedFilterBase((byte) 'T', 391802L);
		acc1.addFailedFilterBase((byte) 'T', 391803L);
		acc1.addFailedFilterBase((byte) 'T', 391806L);
		acc1.addFailedFilterBase((byte) 'T', 391809L);
		acc1.addFailedFilterBase((byte) 'T', 391810L);
		acc1.addFailedFilterBase((byte) 'T', 391811L);
		acc1.addFailedFilterBase((byte) 'T', 391812L);
		acc1.addFailedFilterBase((byte) 'T', 391813L);
		acc1.addFailedFilterBase((byte) 'T', 391814L);
		acc1.addFailedFilterBase((byte) 'T', 391816L);
		acc1.addFailedFilterBase((byte) 'T', 391818L);
		acc1.addFailedFilterBase((byte) 'T', 391819L);
		acc1.addFailedFilterBase((byte) 'T', 391821L);
		acc1.addFailedFilterBase((byte) 'T', 391822L);
		acc1.addFailedFilterBase((byte) 'T', 391823L);
		acc1.addFailedFilterBase((byte) 'T', 391826L);
		acc1.addFailedFilterBase((byte) 'T', 391827L);
		acc1.addFailedFilterBase((byte) 'T', 391828L);
		acc1.addFailedFilterBase((byte) 'T', 391831L);
		acc1.addFailedFilterBase((byte) 'T', 391834L);
		acc1.addFailedFilterBase((byte) 'T', 391835L);
		acc1.addFailedFilterBase((byte) 'T', 391836L);
		acc1.addFailedFilterBase((byte) 'T', 391838L);
		acc1.addFailedFilterBase((byte) 'T', 391839L);
		acc1.addFailedFilterBase((byte) 'T', 391840L);
		acc1.addFailedFilterBase((byte) 'T', 391841L);
		acc1.addFailedFilterBase((byte) 'T', 391844L);
		acc1.addFailedFilterBase((byte) 'T', 391845L);
		acc1.addFailedFilterBase((byte) 'T', 391846L);
		acc1.addFailedFilterBase((byte) 'T', 391847L);
		acc1.addFailedFilterBase((byte) 'T', 391848L);
		acc1.addFailedFilterBase((byte) 'T', 391850L);
		acc1.addFailedFilterBase((byte) 'T', 391851L);
		acc1.addFailedFilterBase((byte) 'T', 391853L);
		acc1.addFailedFilterBase((byte) 'T', 391854L);
		acc1.addFailedFilterBase((byte) 'T', 391855L);
		acc1.addFailedFilterBase((byte) 'T', 391857L);
		acc1.addFailedFilterBase((byte) 'T', 391859L);
		acc1.addFailedFilterBase((byte) 'T', 391860L);
		acc1.addFailedFilterBase((byte) 'T', 391862L);
		acc1.addFailedFilterBase((byte) 'T', 391863L);
		acc1.addFailedFilterBase((byte) 'T', 391865L);
		acc1.addFailedFilterBase((byte) 'T', 391866L);
		acc1.addFailedFilterBase((byte) 'T', 391868L);
		acc1.addFailedFilterBase((byte) 'T', 391869L);
		acc1.addFailedFilterBase((byte) 'T', 391870L);
		acc1.addFailedFilterBase((byte) 'T', 391871L);
		acc1.addFailedFilterBase((byte) 'T', 391872L);
		acc1.addFailedFilterBase((byte) 'T', 391873L);
		acc1.addFailedFilterBase((byte) 'T', 391874L);
		acc1.addFailedFilterBase((byte) 'T', 391875L);
		acc1.addFailedFilterBase((byte) 'T', 391877L);
		acc1.addFailedFilterBase((byte) 'T', 391878L);
		acc1.addFailedFilterBase((byte) 'T', 391879L);
		acc1.addFailedFilterBase((byte) 'T', 391880L);
		acc1.addFailedFilterBase((byte) 'T', 391881L);
		acc1.addFailedFilterBase((byte) 'T', 391882L);
		acc1.addFailedFilterBase((byte) 'T', 391884L);
		acc1.addFailedFilterBase((byte) 'T', 391885L);
		acc1.addFailedFilterBase((byte) 'T', 391886L);
		acc1.addFailedFilterBase((byte) 'T', 391887L);
		acc1.addFailedFilterBase((byte) 'T', 391889L);

		Accumulator acc2 = new Accumulator(12346);
		acc2.addFailedFilterBase((byte) 'A', 391815L);
		acc2.addFailedFilterBase((byte) 'A', 391817L);
		acc2.addFailedFilterBase((byte) 'G', 391748L);
		acc2.addFailedFilterBase((byte) 'G', 391752L);
		acc2.addFailedFilterBase((byte) 'G', 391754L);
		acc2.addFailedFilterBase((byte) 'G', 391755L);
		acc2.addFailedFilterBase((byte) 'G', 391756L);
		acc2.addFailedFilterBase((byte) 'G', 391757L);
		acc2.addFailedFilterBase((byte) 'G', 391760L);
		acc2.addFailedFilterBase((byte) 'G', 391761L);
		acc2.addFailedFilterBase((byte) 'G', 391762L);
		acc2.addFailedFilterBase((byte) 'G', 391763L);
		acc2.addFailedFilterBase((byte) 'G', 391765L);
		acc2.addFailedFilterBase((byte) 'G', 391766L);
		acc2.addFailedFilterBase((byte) 'G', 391768L);
		acc2.addFailedFilterBase((byte) 'G', 391769L);
		acc2.addFailedFilterBase((byte) 'G', 391770L);
		acc2.addFailedFilterBase((byte) 'G', 391771L);
		acc2.addFailedFilterBase((byte) 'G', 391773L);
		acc2.addFailedFilterBase((byte) 'G', 391776L);
		acc2.addFailedFilterBase((byte) 'G', 391777L);
		acc2.addFailedFilterBase((byte) 'G', 391778L);
		acc2.addFailedFilterBase((byte) 'G', 391779L);
		acc2.addFailedFilterBase((byte) 'G', 391780L);
		acc2.addFailedFilterBase((byte) 'G', 391782L);
		acc2.addFailedFilterBase((byte) 'G', 391783L);
		acc2.addFailedFilterBase((byte) 'G', 391785L);
		acc2.addFailedFilterBase((byte) 'G', 391787L);
		acc2.addFailedFilterBase((byte) 'G', 391788L);
		acc2.addFailedFilterBase((byte) 'G', 391789L);
		acc2.addFailedFilterBase((byte) 'G', 391790L);
		acc2.addFailedFilterBase((byte) 'G', 391791L);
		acc2.addFailedFilterBase((byte) 'G', 391792L);
		acc2.addFailedFilterBase((byte) 'G', 391795L);
		acc2.addFailedFilterBase((byte) 'G', 391796L);
		acc2.addFailedFilterBase((byte) 'G', 391797L);
		acc2.addFailedFilterBase((byte) 'G', 391799L);
		acc2.addFailedFilterBase((byte) 'G', 391800L);
		acc2.addFailedFilterBase((byte) 'G', 391802L);
		acc2.addFailedFilterBase((byte) 'G', 391803L);
		acc2.addFailedFilterBase((byte) 'G', 391806L);
		acc2.addFailedFilterBase((byte) 'G', 391809L);
		acc2.addFailedFilterBase((byte) 'G', 391810L);
		acc2.addFailedFilterBase((byte) 'G', 391811L);
		acc2.addFailedFilterBase((byte) 'G', 391812L);
		acc2.addFailedFilterBase((byte) 'G', 391813L);
		acc2.addFailedFilterBase((byte) 'G', 391814L);
		acc2.addFailedFilterBase((byte) 'G', 391816L);
		acc2.addFailedFilterBase((byte) 'G', 391818L);
		acc2.addFailedFilterBase((byte) 'G', 391819L);
		acc2.addFailedFilterBase((byte) 'G', 391821L);
		acc2.addFailedFilterBase((byte) 'G', 391822L);
		acc2.addFailedFilterBase((byte) 'G', 391823L);
		acc2.addFailedFilterBase((byte) 'G', 391826L);
		acc2.addFailedFilterBase((byte) 'G', 391827L);
		acc2.addFailedFilterBase((byte) 'G', 391828L);
		acc2.addFailedFilterBase((byte) 'G', 391831L);
		acc2.addFailedFilterBase((byte) 'G', 391832L);
		acc2.addFailedFilterBase((byte) 'G', 391834L);
		acc2.addFailedFilterBase((byte) 'G', 391835L);
		acc2.addFailedFilterBase((byte) 'G', 391836L);
		acc2.addFailedFilterBase((byte) 'G', 391838L);
		acc2.addFailedFilterBase((byte) 'G', 391839L);
		acc2.addFailedFilterBase((byte) 'G', 391840L);
		acc2.addFailedFilterBase((byte) 'G', 391841L);
		acc2.addFailedFilterBase((byte) 'G', 391843L);
		acc2.addFailedFilterBase((byte) 'G', 391844L);
		acc2.addFailedFilterBase((byte) 'G', 391845L);
		acc2.addFailedFilterBase((byte) 'G', 391846L);
		acc2.addFailedFilterBase((byte) 'G', 391847L);
		acc2.addFailedFilterBase((byte) 'G', 391848L);
		acc2.addFailedFilterBase((byte) 'G', 391850L);
		acc2.addFailedFilterBase((byte) 'G', 391851L);
		acc2.addFailedFilterBase((byte) 'G', 391853L);
		acc2.addFailedFilterBase((byte) 'G', 391854L);
		acc2.addFailedFilterBase((byte) 'G', 391855L);
		acc2.addFailedFilterBase((byte) 'G', 391857L);
		acc2.addFailedFilterBase((byte) 'G', 391859L);
		acc2.addFailedFilterBase((byte) 'G', 391860L);
		acc2.addFailedFilterBase((byte) 'G', 391862L);
		acc2.addFailedFilterBase((byte) 'G', 391863L);
		acc2.addFailedFilterBase((byte) 'G', 391865L);
		acc2.addFailedFilterBase((byte) 'G', 391866L);
		acc2.addFailedFilterBase((byte) 'G', 391867L);
		acc2.addFailedFilterBase((byte) 'G', 391868L);
		acc2.addFailedFilterBase((byte) 'G', 391869L);
		acc2.addFailedFilterBase((byte) 'G', 391870L);
		acc2.addFailedFilterBase((byte) 'G', 391871L);
		acc2.addFailedFilterBase((byte) 'G', 391872L);
		acc2.addFailedFilterBase((byte) 'G', 391873L);
		acc2.addFailedFilterBase((byte) 'G', 391874L);
		acc2.addFailedFilterBase((byte) 'G', 391875L);
		acc2.addFailedFilterBase((byte) 'G', 391877L);
		acc2.addFailedFilterBase((byte) 'G', 391878L);
		acc2.addFailedFilterBase((byte) 'G', 391879L);
		acc2.addFailedFilterBase((byte) 'G', 391880L);
		acc2.addFailedFilterBase((byte) 'G', 391881L);
		acc2.addFailedFilterBase((byte) 'G', 391882L);
		acc2.addFailedFilterBase((byte) 'G', 391884L);
		acc2.addFailedFilterBase((byte) 'G', 391885L);
		acc2.addFailedFilterBase((byte) 'G', 391886L);
		acc2.addFailedFilterBase((byte) 'G', 391887L);
		acc2.addFailedFilterBase((byte) 'G', 391889L);
		acc2.addFailedFilterBase((byte) 'G', 391890L);
		acc2.addFailedFilterBase((byte) 'G', 391891L);

		List<Accumulator> accumulators = Arrays.asList(acc1, acc2);
		assertEquals("CA2;CG4;TG97;_G2", PipelineUtil.getFailedFilterCS(accumulators));

	}
	
	@Test
	public void getUnique() {
		Accumulator acc1 = new Accumulator(12345);
		acc1.addFailedFilterBase((byte) 'C', 7715117792191186532L);
		acc1.addFailedFilterBase((byte) 'C', 419015429944394057L);
		acc1.addFailedFilterBase((byte) 'C',  -4982705001061907857L);
		acc1.addFailedFilterBase((byte) 'C',  -5310912577898632174L);
		acc1.addFailedFilterBase((byte) 'C',  4442000349200974418L);
		acc1.addFailedFilterBase((byte) 'C',  -6357506197841616687L);
		acc1.addFailedFilterBase((byte) 'T', -7438533981666644002L);
		acc1.addFailedFilterBase((byte) 'T', 1782036110835987330L);
		acc1.addFailedFilterBase((byte) 'T', -8751309811463707606L);
		acc1.addFailedFilterBase((byte) 'T', -1006789886807925312L);
		acc1.addFailedFilterBase((byte) 'T', 267604890061977987L);
		acc1.addFailedFilterBase((byte) 'T', -5752866221558017591L);
		acc1.addFailedFilterBase((byte) 'T', 8813529100103331852L);
		acc1.addFailedFilterBase((byte) 'T', 8740981370654066209L);
		acc1.addFailedFilterBase((byte) 'T', 3875546135377654529L);
		acc1.addFailedFilterBase((byte) 'T', 3156183271998102851L);
		acc1.addFailedFilterBase((byte) 'T', 496025280786752802L);
		acc1.addFailedFilterBase((byte) 'T', -5370540407260949421L);
		acc1.addFailedFilterBase((byte) 'T', -1034241958712548931L);
		acc1.addFailedFilterBase((byte) 'T', 4760541127041820054L);
		acc1.addFailedFilterBase((byte) 'T', -478460491748683719L);
		acc1.addFailedFilterBase((byte) 'T', 1080824909688009888L);
		acc1.addFailedFilterBase((byte) 'T', -248833557815058818L);
		acc1.addFailedFilterBase((byte) 'T', -836224118038877802L);
		acc1.addFailedFilterBase((byte) 'T', -6251926633067930061L);
		acc1.addFailedFilterBase((byte) 'T', 5693373976150522013L);
		acc1.addFailedFilterBase((byte) 'T', -8618732021493771414L);
		acc1.addFailedFilterBase((byte) 'T', -3535500610440114311L);
		acc1.addFailedFilterBase((byte) 'T', -8607579577620952717L);
		acc1.addFailedFilterBase((byte) 'T', 3617903064101974186L);
		acc1.addFailedFilterBase((byte) 'T', -292355879921902716L);
		acc1.addFailedFilterBase((byte) 'T', -3333658515894473867L);
		acc1.addFailedFilterBase((byte) 'T', -6061011605950203461L);
		acc1.addFailedFilterBase((byte) 'T', -2558972151765706823L);
		acc1.addFailedFilterBase((byte) 'T', 8278189905233837843L);
		acc1.addFailedFilterBase((byte) 'T', 3158165356931543161L);
		acc1.addFailedFilterBase((byte) 'T', 8095381987075818260L);
		acc1.addFailedFilterBase((byte) 'T', 4995250372108758135L);
		acc1.addFailedFilterBase((byte) 'T', 4308956381034063213L);
		acc1.addFailedFilterBase((byte) 'T', -3975576866416630705L);
		acc1.addFailedFilterBase((byte) 'T',  215674860906757583L);
		acc1.addFailedFilterBase((byte) 'T',  9130708757120638831L);
		acc1.addFailedFilterBase((byte) 'T',  2603036776856178377L);
		acc1.addFailedFilterBase((byte) 'T',  5735814374756852238L);
		acc1.addFailedFilterBase((byte) 'T',  7540064501637539247L);
		acc1.addFailedFilterBase((byte) 'T',  6419930603122219958L);
		acc1.addFailedFilterBase((byte) 'T',  8210168556586079108L);
		acc1.addFailedFilterBase((byte) 'T',  3268524321762958948L);
		acc1.addFailedFilterBase((byte) 'T',  -138463099035032818L);
		acc1.addFailedFilterBase((byte) 'T',  2609032967337224118L);
		acc1.addFailedFilterBase((byte) 'T',  1180519307271286853L);
		acc1.addFailedFilterBase((byte) 'T',  3647809539552025230L);
		acc1.addFailedFilterBase((byte) 'T',  8493692125897398515L);
		acc1.addFailedFilterBase((byte) 'T',  3315994573281898804L);
		acc1.addFailedFilterBase((byte) 'T',  724501628528950317L);
		acc1.addFailedFilterBase((byte) 'T',  2324044325092459430L);
		acc1.addFailedFilterBase((byte) 'T',  4824931694165828140L);
		acc1.addFailedFilterBase((byte) 'T',  1027175994126509642L);
		acc1.addFailedFilterBase((byte) 'T',  -2957957358873393133L);
		acc1.addFailedFilterBase((byte) 'T',  3268524321762958948L);
		acc1.addFailedFilterBase((byte) 'T',  -8828184361023553865L);
		acc1.addFailedFilterBase((byte) 'T',  -8251184108896989485L);
		acc1.addFailedFilterBase((byte) 'T',  4329503331942280002L);
		acc1.addFailedFilterBase((byte) 'T',  3244033920476022406L);
		acc1.addFailedFilterBase((byte) 'T',  6461699796293877052L);
		acc1.addFailedFilterBase((byte) 'T',  -1679260168527306503L);
		acc1.addFailedFilterBase((byte) 'T',  1216738602136290533L);
		acc1.addFailedFilterBase((byte) 'T',  1144424180764508213L);
		acc1.addFailedFilterBase((byte) 'T',  7433969058982544698L);
		acc1.addFailedFilterBase((byte) 'T',  8740981370654066209L);
		acc1.addFailedFilterBase((byte) 'T',  -7503905741508074480L);
		acc1.addFailedFilterBase((byte) 'T',  9219990827317843535L);
		acc1.addFailedFilterBase((byte) 'T',  -562526361002022550L);
		acc1.addFailedFilterBase((byte) 'T',  -1583125034918006130L);
		acc1.addFailedFilterBase((byte) 'T',  7486429413782649127L);
		acc1.addFailedFilterBase((byte) 'T',  -1476485875783679687L);
		acc1.addFailedFilterBase((byte) 'T',  -7726467777379464574L);
		acc1.addFailedFilterBase((byte) 'T',  6145111076905202179L);
		acc1.addFailedFilterBase((byte) 'T',  -6962289282457110290L);
		acc1.addFailedFilterBase((byte) 'T',  1829825168833198510L);
		acc1.addFailedFilterBase((byte) 'T',  -8597867675373289290L);
		acc1.addFailedFilterBase((byte) 'T',  -5752866221558017591L);
		acc1.addFailedFilterBase((byte) 'T',  1015854181370342302L);
		acc1.addFailedFilterBase((byte) 'T',  35473122144577186L);
		acc1.addFailedFilterBase((byte) 'T',  7825657632162557305L);
		acc1.addFailedFilterBase((byte) 'T',  -4454819942067680881L);
		acc1.addFailedFilterBase((byte) 'T',  -3618246714841591676L);
		acc1.addFailedFilterBase((byte) 'T',  8557524143668341863L);
		acc1.addFailedFilterBase((byte) 'T',  7092326401254028467L);
		acc1.addFailedFilterBase((byte) 'T',  4031901574327385806L);
		acc1.addFailedFilterBase((byte) 'T',  7382490855834685218L);
		acc1.addFailedFilterBase((byte) 'T',  -7609961797280175578L);
		acc1.addFailedFilterBase((byte) 'T',  6264118358119675396L);
		acc1.addFailedFilterBase((byte) 'T',  3774827528163625704L);
		acc1.addFailedFilterBase((byte) 'T',  8493692125897398515L);
		acc1.addFailedFilterBase((byte) 'T',  3728160140633686841L);
		acc1.addFailedFilterBase((byte) 'T',  876929434758225646L);
		acc1.addFailedFilterBase((byte) 'T',  4853207379767922037L);
		acc1.addFailedFilterBase((byte) 'T',  7915916741906599442L);
		acc1.addFailedFilterBase((byte) 'T',  -8874197118979016859L);
		acc1.addFailedFilterBase((byte) 'T',  -2135344415274795553L);
		acc1.addFailedFilterBase((byte) 'T',  7092828207829028047L);
		acc1.addFailedFilterBase((byte) 'T',  8518363537084736565L);

		Accumulator acc2 = new Accumulator(12346);

		acc2.addFailedFilterBase((byte) 'A',  419015429944394057L);
		acc2.addFailedFilterBase((byte) 'A',  -4982705001061907857L);
		acc2.addFailedFilterBase((byte) 'G', -7438533981666644002L);
		acc2.addFailedFilterBase((byte) 'G', 1782036110835987330L);
		acc2.addFailedFilterBase((byte) 'G', -8751309811463707606L);
		acc2.addFailedFilterBase((byte) 'G', -1006789886807925312L);
		acc2.addFailedFilterBase((byte) 'G', 267604890061977987L);
		acc2.addFailedFilterBase((byte) 'G',  -5752866221558017591L);
		acc2.addFailedFilterBase((byte) 'G',  8813529100103331852L);
		acc2.addFailedFilterBase((byte) 'G',  8740981370654066209L);
		acc2.addFailedFilterBase((byte) 'G',  3875546135377654529L);
		acc2.addFailedFilterBase((byte) 'G',  3156183271998102851L);
		acc2.addFailedFilterBase((byte) 'G',  496025280786752802L);
		acc2.addFailedFilterBase((byte) 'G',  -5370540407260949421L);
		acc2.addFailedFilterBase((byte) 'G',  -1034241958712548931L);
		acc2.addFailedFilterBase((byte) 'G',  4760541127041820054L);
		acc2.addFailedFilterBase((byte) 'G',  -478460491748683719L);
		acc2.addFailedFilterBase((byte) 'G',  1080824909688009888L);
		acc2.addFailedFilterBase((byte) 'G',  -248833557815058818L);
		acc2.addFailedFilterBase((byte) 'G',  -836224118038877802L);
		acc2.addFailedFilterBase((byte) 'G',  -6251926633067930061L);
		acc2.addFailedFilterBase((byte) 'G',  5693373976150522013L);
		acc2.addFailedFilterBase((byte) 'G',  -8618732021493771414L);
		acc2.addFailedFilterBase((byte) 'G',  -3535500610440114311L);
		acc2.addFailedFilterBase((byte) 'G',  -8607579577620952717L);
		acc2.addFailedFilterBase((byte) 'G',  3617903064101974186L);
		acc2.addFailedFilterBase((byte) 'G',  7715117792191186532L);
		acc2.addFailedFilterBase((byte) 'G',  -292355879921902716L);
		acc2.addFailedFilterBase((byte) 'G',  -3333658515894473867L);
		acc2.addFailedFilterBase((byte) 'G',  -6061011605950203461L);
		acc2.addFailedFilterBase((byte) 'G',  -2558972151765706823L);
		acc2.addFailedFilterBase((byte) 'G',  8278189905233837843L);
		acc2.addFailedFilterBase((byte) 'G',  3158165356931543161L);
		acc2.addFailedFilterBase((byte) 'G',  8095381987075818260L);
		acc2.addFailedFilterBase((byte) 'G',  4995250372108758135L);
		acc2.addFailedFilterBase((byte) 'G',  4308956381034063213L);
		acc2.addFailedFilterBase((byte) 'G',  -3975576866416630705L);
		acc2.addFailedFilterBase((byte) 'G',  215674860906757583L);
		acc2.addFailedFilterBase((byte) 'G',  9130708757120638831L);
		acc2.addFailedFilterBase((byte) 'G',  2603036776856178377L);
		acc2.addFailedFilterBase((byte) 'G',  5735814374756852238L);
		acc2.addFailedFilterBase((byte) 'G',  7540064501637539247L);
		acc2.addFailedFilterBase((byte) 'G',  6419930603122219958L);
		acc2.addFailedFilterBase((byte) 'G',  8210168556586079108L);
		acc2.addFailedFilterBase((byte) 'G',  3268524321762958948L);
		acc2.addFailedFilterBase((byte) 'G',  -138463099035032818L);
		acc2.addFailedFilterBase((byte) 'G',  2609032967337224118L);
		acc2.addFailedFilterBase((byte) 'G',  1180519307271286853L);
		acc2.addFailedFilterBase((byte) 'G',  3647809539552025230L);
		acc2.addFailedFilterBase((byte) 'G',  8493692125897398515L);
		acc2.addFailedFilterBase((byte) 'G',  3315994573281898804L);
		acc2.addFailedFilterBase((byte) 'G',  724501628528950317L);
		acc2.addFailedFilterBase((byte) 'G',  2324044325092459430L);
		acc2.addFailedFilterBase((byte) 'G',  4824931694165828140L);
		acc2.addFailedFilterBase((byte) 'G',  1027175994126509642L);
		acc2.addFailedFilterBase((byte) 'G',  -2957957358873393133L);
		acc2.addFailedFilterBase((byte) 'G',  3268524321762958948L);
		acc2.addFailedFilterBase((byte) 'G',  -5310912577898632174L);
		acc2.addFailedFilterBase((byte) 'G',  -8828184361023553865L);
		acc2.addFailedFilterBase((byte) 'G',  -8251184108896989485L);
		acc2.addFailedFilterBase((byte) 'G',  4329503331942280002L);
		acc2.addFailedFilterBase((byte) 'G',  3244033920476022406L);
		acc2.addFailedFilterBase((byte) 'G',  6461699796293877052L);
		acc2.addFailedFilterBase((byte) 'G',  -1679260168527306503L);
		acc2.addFailedFilterBase((byte) 'G',  1216738602136290533L);
		acc2.addFailedFilterBase((byte) 'G',  4442000349200974418L);
		acc2.addFailedFilterBase((byte) 'G',  1144424180764508213L);
		acc2.addFailedFilterBase((byte) 'G',  7433969058982544698L);
		acc2.addFailedFilterBase((byte) 'G',  8740981370654066209L);
		acc2.addFailedFilterBase((byte) 'G',  -7503905741508074480L);
		acc2.addFailedFilterBase((byte) 'G',  9219990827317843535L);
		acc2.addFailedFilterBase((byte) 'G',  -562526361002022550L);
		acc2.addFailedFilterBase((byte) 'G',  -1583125034918006130L);
		acc2.addFailedFilterBase((byte) 'G',  7486429413782649127L);
		acc2.addFailedFilterBase((byte) 'G',  -1476485875783679687L);
		acc2.addFailedFilterBase((byte) 'G',  -7726467777379464574L);
		acc2.addFailedFilterBase((byte) 'G',  6145111076905202179L);
		acc2.addFailedFilterBase((byte) 'G',  -6962289282457110290L);
		acc2.addFailedFilterBase((byte) 'G',  1829825168833198510L);
		acc2.addFailedFilterBase((byte) 'G',  -8597867675373289290L);
		acc2.addFailedFilterBase((byte) 'G',  -5752866221558017591L);
		acc2.addFailedFilterBase((byte) 'G',  1015854181370342302L);
		acc2.addFailedFilterBase((byte) 'G',  35473122144577186L);
		acc2.addFailedFilterBase((byte) 'G',  -6357506197841616687L);
		acc2.addFailedFilterBase((byte) 'G',  7825657632162557305L);
		acc2.addFailedFilterBase((byte) 'G',  -4454819942067680881L);
		acc2.addFailedFilterBase((byte) 'G',  -3618246714841591676L);
		acc2.addFailedFilterBase((byte) 'G',  8557524143668341863L);
		acc2.addFailedFilterBase((byte) 'G',  7092326401254028467L);
		acc2.addFailedFilterBase((byte) 'G',  4031901574327385806L);
		acc2.addFailedFilterBase((byte) 'G',  7382490855834685218L);
		acc2.addFailedFilterBase((byte) 'G',  -7609961797280175578L);
		acc2.addFailedFilterBase((byte) 'G',  6264118358119675396L);
		acc2.addFailedFilterBase((byte) 'G',  3774827528163625704L);
		acc2.addFailedFilterBase((byte) 'G',  8493692125897398515L);
		acc2.addFailedFilterBase((byte) 'G',  3728160140633686841L);
		acc2.addFailedFilterBase((byte) 'G',  876929434758225646L);
		acc2.addFailedFilterBase((byte) 'G',  4853207379767922037L);
		acc2.addFailedFilterBase((byte) 'G',  7915916741906599442L);
		acc2.addFailedFilterBase((byte) 'G',  -8874197118979016859L);
		acc2.addFailedFilterBase((byte) 'G',  -2135344415274795553L);
		acc2.addFailedFilterBase((byte) 'G',  7092828207829028047L);
		acc2.addFailedFilterBase((byte) 'G',  8518363537084736565L);
		acc2.addFailedFilterBase((byte) 'G',  -8751309811463707606L);
		acc2.addFailedFilterBase((byte) 'G',  -8252885584572880588L);

		List<Accumulator> accumulators = Arrays.asList(acc1, acc2);
		assertEquals("CA2;CG4;TG93;_G1", PipelineUtil.getFailedFilterCS(accumulators));

	}
	
	@Test
	public void realLifeFunnyBusiness() {
		List<String> readNames = Arrays.asList("DCW97JN1:295:D1B5AACXX:3:2309:14480:47693" ,
				"DCW97JN1:295:D1B5AACXX:5:1111:15402:19442" ,
				"DCW97JN1:295:D1B5AACXX:3:2210:16730:78147" ,
				"HWI-ST526:219:C16B2ACXX:1:2304:7016:24438" ,
				"HWI-ST526:219:C16B2ACXX:1:1307:1391:41225" ,
				"HWI-ST526:219:C16B2ACXX:1:1103:16387:29528" ,
				"HWI-ST526:219:C16B2ACXX:1:2112:18851:73892" ,
				"DCW97JN1:295:D1B5AACXX:4:2114:16640:6591" ,
				"DCW97JN1:295:D1B5AACXX:3:1303:9721:20997" ,
				"HWI-ST526:219:C16B2ACXX:1:2207:13419:94062" ,
				"DCW97JN1:295:D1B5AACXX:3:2309:14480:47693" ,
				"DCW97JN1:295:D1B5AACXX:5:2305:3789:14633" ,
				"HWI-ST526:219:C16B2ACXX:1:1104:4560:57572" ,
				"DCW97JN1:295:D1B5AACXX:6:2111:9627:7792" ,
				"DCW97JN1:295:D1B5AACXX:5:2113:4992:94218" ,
				"DCW97JN1:295:D1B5AACXX:4:2202:16591:44438" ,
				"DCW97JN1:295:D1B5AACXX:4:2205:19357:57058" ,
				"DCW97JN1:295:D1B5AACXX:6:2307:12495:71902" ,
				"DCW97JN1:295:D1B5AACXX:4:2212:17500:6828" ,
				"DCW97JN1:295:D1B5AACXX:5:1109:19970:67964" ,
				"DCW97JN1:295:D1B5AACXX:3:2310:15515:71252" ,
				"DCW97JN1:295:D1B5AACXX:5:2201:4467:9303" ,
				"DCW97JN1:295:D1B5AACXX:6:1115:2311:14137" ,
				"DCW97JN1:295:D1B5AACXX:5:1302:19127:9050" ,
				"DCW97JN1:295:D1B5AACXX:6:1114:11214:2476" ,
				"DCW97JN1:295:D1B5AACXX:5:1212:1161:16540" ,
				"DCW97JN1:295:D1B5AACXX:5:1301:16241:83752" ,
				"DCW97JN1:295:D1B5AACXX:3:1116:13596:47139" ,
				"DCW97JN1:295:D1B5AACXX:5:1307:3521:66193" ,
				"DCW97JN1:295:D1B5AACXX:5:2208:16724:61398" ,
				"DCW97JN1:295:D1B5AACXX:3:2107:10686:79006" ,
				"DCW97JN1:295:D1B5AACXX:4:2107:13457:26494" ,
				"HWI-ST526:219:C16B2ACXX:1:2205:13081:23009" ,
				"DCW97JN1:295:D1B5AACXX:6:1213:17550:51242" ,
				"HWI-ST526:219:C16B2ACXX:1:2312:4528:65210" ,
				"DCW97JN1:295:D1B5AACXX:3:1209:5045:36182" ,
				"DCW97JN1:295:D1B5AACXX:6:2309:19872:67179" ,
				"DCW97JN1:295:D1B5AACXX:4:2216:7610:96187" ,
				"DCW97JN1:295:D1B5AACXX:5:1109:17596:79874" ,
				"DCW97JN1:295:D1B5AACXX:4:1308:6864:53678" ,
				"DCW97JN1:295:D1B5AACXX:6:1314:5251:24654" ,
				"DCW97JN1:295:D1B5AACXX:4:1201:1616:97707" ,
				"DCW97JN1:295:D1B5AACXX:3:2210:16730:78147" ,
				"DCW97JN1:295:D1B5AACXX:3:1310:11233:73883" ,
				"HWI-ST526:219:C16B2ACXX:1:2312:13402:11372" ,
				"HWI-ST526:219:C16B2ACXX:1:2307:2775:76985" ,
				"DCW97JN1:295:D1B5AACXX:6:2313:6436:3202" ,
				"DCW97JN1:295:D1B5AACXX:5:1109:5309:44655" ,
				"DCW97JN1:295:D1B5AACXX:4:1210:13752:91386" ,
				"DCW97JN1:295:D1B5AACXX:4:1210:13752:91386" ,
				"DCW97JN1:295:D1B5AACXX:4:2116:13414:59472" ,
				"DCW97JN1:295:D1B5AACXX:4:2304:8591:34902" ,
				"DCW97JN1:295:D1B5AACXX:4:1206:17734:74060" ,
				"DCW97JN1:295:D1B5AACXX:3:1214:4999:4158" ,
				"DCW97JN1:295:D1B5AACXX:4:2205:19357:57058");
		assertEquals(55, readNames.size());
		Set<String> readNameSet = new HashSet<>();
		for (String s : readNames) {
			if (!readNameSet.add(s)) {
				System.out.println("dup read: " + s);
			}
		}
		assertEquals(51, readNameSet.size());


		Accumulator acc1 = new Accumulator(12345);
		acc1.addFailedFilterBase((byte) 'A', 4086900006971767854L);
		acc1.addFailedFilterBase((byte) 'A', -4716832531774407080L);
		acc1.addFailedFilterBase((byte) 'A', 6700611398151820155L);
		acc1.addFailedFilterBase((byte) 'A', 5186962780778754199L);
		acc1.addFailedFilterBase((byte) 'A', 5122404141805319601L);
		acc1.addFailedFilterBase((byte) 'A', 8958843543082877714L);
		acc1.addFailedFilterBase((byte) 'A', 8831294537041955437L);
		acc1.addFailedFilterBase((byte) 'A', 6048261452686045528L);
		acc1.addFailedFilterBase((byte) 'A', -27981145301803143L);
		acc1.addFailedFilterBase((byte) 'A', -1895240219048882894L);
		acc1.addFailedFilterBase((byte) 'A', -432780264239122912L);
		acc1.addFailedFilterBase((byte) 'T', -1441445426238881582L);
		acc1.addFailedFilterBase((byte) 'T', 7422662345407810992L);
		acc1.addFailedFilterBase((byte) 'T', -27981145301803143L);
		acc1.addFailedFilterBase((byte) 'T', -3745913329893305916L);
		acc1.addFailedFilterBase((byte) 'T', -5872199506007072810L);
		acc1.addFailedFilterBase((byte) 'T', 8018589148027039207L);
		acc1.addFailedFilterBase((byte) 'T', -6926956303777753425L);
		acc1.addFailedFilterBase((byte) 'T', -8269550325241252261L);
		acc1.addFailedFilterBase((byte) 'T', -1441445426238881582L);
		acc1.addFailedFilterBase((byte) 'T', -4183403039113486709L);
		acc1.addFailedFilterBase((byte) 'T', -2252558088007944815L);
		acc1.addFailedFilterBase((byte) 'T', 5095097565078919292L);
		acc1.addFailedFilterBase((byte) 'T', -5149194738896197730L);
		acc1.addFailedFilterBase((byte) 'T', -8386429281397821662L);
		acc1.addFailedFilterBase((byte) 'T', 9018607088384783716L);
		acc1.addFailedFilterBase((byte) 'T', -4582515424865507576L);
		acc1.addFailedFilterBase((byte) 'T', 5006058289362654996L);
		acc1.addFailedFilterBase((byte) 'T', 1570085767318999878L);
		acc1.addFailedFilterBase((byte) 'T', 6902405747651132852L);
		acc1.addFailedFilterBase((byte) 'T', 550354402160307514L);
		acc1.addFailedFilterBase((byte) 'T', -8462486584110695116L);
		acc1.addFailedFilterBase((byte) 'T', 3461374088017820567L);
		acc1.addFailedFilterBase((byte) 'T', 281625484296755152L);

		Accumulator acc2 = new Accumulator(12346);
		acc2.addFailedFilterBase((byte) 'A',-1441445426238881582L);
		acc2.addFailedFilterBase((byte) 'A',7422662345407810992L);
		acc2.addFailedFilterBase((byte) 'A',-27981145301803143L);
		acc2.addFailedFilterBase((byte) 'A',-3745913329893305916L);
		acc2.addFailedFilterBase((byte) 'A',-5872199506007072810L);
		acc2.addFailedFilterBase((byte) 'A',8018589148027039207L);
		acc2.addFailedFilterBase((byte) 'A',-8780768932796289538L);
		acc2.addFailedFilterBase((byte) 'A',-6926956303777753425L);
		acc2.addFailedFilterBase((byte) 'A',-8269550325241252261L);
		acc2.addFailedFilterBase((byte) 'A',-1441445426238881582L);
		acc2.addFailedFilterBase((byte) 'A',-4183403039113486709L);
		acc2.addFailedFilterBase((byte) 'A',-2252558088007944815L);
		acc2.addFailedFilterBase((byte) 'A',5095097565078919292L);
		acc2.addFailedFilterBase((byte) 'A',-5149194738896197730L);
		acc2.addFailedFilterBase((byte) 'A',-8386429281397821662L);
		acc2.addFailedFilterBase((byte) 'A',9018607088384783716L);
		acc2.addFailedFilterBase((byte) 'A',-4582515424865507576L);
		acc2.addFailedFilterBase((byte) 'A',5006058289362654996L);
		acc2.addFailedFilterBase((byte) 'A',1570085767318999878L);
		acc2.addFailedFilterBase((byte) 'A',6902405747651132852L);
		acc2.addFailedFilterBase((byte) 'A',550354402160307514L);
		acc2.addFailedFilterBase((byte) 'A',-8462486584110695116L);
		acc2.addFailedFilterBase((byte) 'A',3461374088017820567L);
		acc2.addFailedFilterBase((byte) 'A',281625484296755152L);
		acc2.addFailedFilterBase((byte) 'T', 7172062261984156803L);
		acc2.addFailedFilterBase((byte) 'T', 4537039380766474115L);
		acc2.addFailedFilterBase((byte) 'T', 4086900006971767854L);
		acc2.addFailedFilterBase((byte) 'T', -4716832531774407080L);
		acc2.addFailedFilterBase((byte) 'T', -1351966021709934743L);
		acc2.addFailedFilterBase((byte) 'T', -330036681603147982L);
		acc2.addFailedFilterBase((byte) 'T', 1382422122404187806L);
		acc2.addFailedFilterBase((byte) 'T', 6700611398151820155L);
		acc2.addFailedFilterBase((byte) 'T', 5186962780778754199L);
		acc2.addFailedFilterBase((byte) 'T', 5122404141805319601L);
		acc2.addFailedFilterBase((byte) 'T', 8958843543082877714L);
		acc2.addFailedFilterBase((byte) 'T', -3052003932123722238L);
		acc2.addFailedFilterBase((byte) 'T', 6488180788633260269L);
		acc2.addFailedFilterBase((byte) 'T', 8831294537041955437L);
		acc2.addFailedFilterBase((byte) 'T', 6048261452686045528L);
		acc2.addFailedFilterBase((byte) 'T', -27981145301803143L);
		acc2.addFailedFilterBase((byte) 'T', 8691136542396127591L);
		acc2.addFailedFilterBase((byte) 'T', 7868192676364940616L);
		acc2.addFailedFilterBase((byte) 'T', -1895240219048882894L);
		acc2.addFailedFilterBase((byte) 'T', -432780264239122912L);

		List<Accumulator> accumulators = Arrays.asList(acc1, acc2);
		assertEquals("AT10;TA21;_A1;_T9", PipelineUtil.getFailedFilterCS(accumulators));
	}

	@Test
	public void testGetFailedFilterCS() {
		Accumulator acc1 = new Accumulator(12345);
		acc1.addFailedFilterBase((byte) 'A', 2609032967337224118L);
		acc1.addFailedFilterBase((byte) 'A', 1180519307271286853L);
		acc1.addFailedFilterBase((byte) 'A', 3647809539552025230L);

		Accumulator acc2 = new Accumulator(12346);
		acc2.addFailedFilterBase((byte) 'A', 2609032967337224118L);
		acc2.addFailedFilterBase((byte) 'C', 2609032967337224118L);
		List<Accumulator> accumulators = Arrays.asList(acc1, acc2);
		assertEquals("A_2", PipelineUtil.getFailedFilterCS(accumulators));

		acc1.addFailedFilterBase((byte) 'C', 3647809539552025230L);
		assertEquals("A_1", PipelineUtil.getFailedFilterCS(accumulators));
		acc1.addFailedFilterBase((byte) 'G', 555);
		acc2.addFailedFilterBase((byte) 'G', 555);
		assertEquals("A_1;GG1", PipelineUtil.getFailedFilterCS(accumulators));

	}
}
