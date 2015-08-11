package au.edu.qimr.clinvar.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import gnu.trove.list.array.TIntArrayList;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Pair;

import au.edu.qimr.clinvar.model.Probe;

public class ClinVarUtilTest {
	
	
	@Test
	public void createSAMRecord() {
		try {
			ClinVarUtil.createSAMRecord(null, null, 1, 1, 1, "", "", 1, 1, "", 0);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		try {
			ClinVarUtil.createSAMRecord(null, null, 1, 1, 1, "ABCD", "", 1, 1, "", 0);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		Cigar cigar = new Cigar();
		SAMRecord rec = ClinVarUtil.createSAMRecord(null, cigar, 1, 1, 1, "ABCD", "chr1", 1, 1, "AAAA", 0);
		assertEquals("0", rec.getAttribute("MD"));	// no cigar elements defined
		
		CigarElement ce = new CigarElement(4, CigarOperator.MATCH_OR_MISMATCH);
		cigar.add(ce);
		rec = ClinVarUtil.createSAMRecord(null, cigar, 1, 1, 1, "AAAA", "chr1", 1, 0, "AAAA", 0);
		assertEquals("4", rec.getAttribute("MD"));
		rec = ClinVarUtil.createSAMRecord(null, cigar, 1, 1, 1, "ABCA", "chr1", 1, 0, "AAAA", 0);
//		System.out.println("rec: " + rec.getSAMString());
		assertEquals("1B0C1", rec.getAttribute("MD"));
	}
	
//	@Test
//	public void fixSWResultsBothEndsDiff() {
//		assertArrayEquals(new String[] {"AABCDE",".||||.","XABCDF"}, ClinVarUtil.rescueSWData(new String[] {"ABCD","||||","ABCD"}, "AABCDE", "XABCDF"));
//	}
	
	@Test
	public void fixSWResultsOneEndDiff() {
		assertArrayEquals(new String[] {"ABCD","||||","ABCD"}, ClinVarUtil.rescueSWData(new String[] {"ABCD","||||","ABCD"}, "ABCD", "ABCD"));
		assertArrayEquals(new String[] {"ABCDE","||||.","ABCDF"}, ClinVarUtil.rescueSWData(new String[] {"ABCD","||||","ABCD"}, "ABCDE", "ABCDF"));
		assertArrayEquals(new String[] {"ABCDEF","||||..","ABCDFG"}, ClinVarUtil.rescueSWData(new String[] {"ABCD","||||","ABCD"}, "ABCDEF", "ABCDFG"));
		assertArrayEquals(new String[] {"AABCD",".||||","XABCD"}, ClinVarUtil.rescueSWData(new String[] {"ABCD","||||","ABCD"}, "AABCD", "XABCD"));
		assertArrayEquals(new String[] {"AAABCD","..||||","ZXABCD"}, ClinVarUtil.rescueSWData(new String[] {"ABCD","||||","ABCD"}, "AAABCD", "ZXABCD"));
	}
	
	@Test
	public void fixSWResultsNullOrEmpty() {
		try {
			ClinVarUtil.rescueSWData(null, null, null);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		try {
			ClinVarUtil.rescueSWData(null, "", null);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		try {
			ClinVarUtil.rescueSWData(null, null, "");
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		try {
			ClinVarUtil.rescueSWData(null, "", "");
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
	}
	
	@Test
	public void getSequenceDictonaryFromProbes() {
		List<Probe> list = new ArrayList<>();
		try {
			ClinVarUtil.getSequenceDictionaryFromProbes(null);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		
		assertEquals(0, ClinVarUtil.getSequenceDictionaryFromProbes(list).getReferenceLength());
		
		Probe p1 = new Probe(1, null, null, null, null, 100, 0, 0, 200, null, 0, 0, "chr1", false, null);
		list.add(p1);
		assertEquals(200, ClinVarUtil.getSequenceDictionaryFromProbes(list).getSequence("chr1").getSequenceLength());
		Probe p2 = new Probe(2, null, null, null, null, 100, 0, 0, 250, null, 0, 0, "chr1", false, null);
		list.add(p2);
		assertEquals(250, ClinVarUtil.getSequenceDictionaryFromProbes(list).getSequence("chr1").getSequenceLength());
		Probe p3 = new Probe(3, null, null, null, null, 100, 0, 0, 300, null, 0, 0, "chr2", false, null);
		list.add(p3);
		assertEquals(250, ClinVarUtil.getSequenceDictionaryFromProbes(list).getSequence("chr1").getSequenceLength());
		assertEquals(300, ClinVarUtil.getSequenceDictionaryFromProbes(list).getSequence("chr2").getSequenceLength());
		Probe p4 = new Probe(4, null, null, null, null, 100, 0, 0, 250, null, 0, 0, "chr2", false, null);
		list.add(p4);
		assertEquals(250, ClinVarUtil.getSequenceDictionaryFromProbes(list).getSequence("chr1").getSequenceLength());
		assertEquals(300, ClinVarUtil.getSequenceDictionaryFromProbes(list).getSequence("chr2").getSequenceLength());
	}
	
	@Test
	public void getOverlappingProbes() {
		ChrPosition cp = ChrPositionUtils.getChrPositionFromString("chr1:100-200");
		try {
			ClinVarUtil.getAmpliconsOverlappingPosition(null, null);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		try {
			ClinVarUtil.getAmpliconsOverlappingPosition(cp, null);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		
		Set<Probe> set = new HashSet<>();
		assertEquals(0, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).size());
		
		// setup some probes
		Probe p1 = new Probe(1, null, null, null, null, 100, 0, 0, 200, null, 0, 0, "chr1", false, null);
		assertEquals(p1.getCp(), cp);
		
		set.add(p1);
		assertEquals(1, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).size());
		assertEquals(p1, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).get(0));
		Probe p2 = new Probe(2, null, null, null, null, 100, 0, 0, 200, null, 0, 0, "chr2", false, null);
		set.add(p2);
		assertEquals(1, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).size());
		assertEquals(p1, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).get(0));
		Probe p3 = new Probe(3, null, null, null, null, 99, 0, 0, 201, null, 0, 0, "chr1", false, null);
		set.add(p3);
		assertEquals(2, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).size());
		assertEquals(true, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).contains(p1));
		assertEquals(true, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).contains(p3));
		Probe p4 = new Probe(4, null, null, null, null, 201, 0, 0, 202, null, 0, 0, "chr1", false, null);
		set.add(p4);
		assertEquals(2, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).size());
		assertEquals(true, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).contains(p1));
		assertEquals(true, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).contains(p3));
		Probe p5 = new Probe(5, null, null, null, null, 200, 0, 0, 202, null, 0, 0, "chr1", false, null);
		set.add(p5);
		assertEquals(2, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).size());
		assertEquals(true, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).contains(p1));
		assertEquals(true, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).contains(p3));
		Probe p6 = new Probe(6, null, null, null, null, 10, 0, 0, 2000, null, 0, 0, "chr1", false, null);
		set.add(p6);
		assertEquals(3, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).size());
		assertEquals(true, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).contains(p1));
		assertEquals(true, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).contains(p3));
		assertEquals(true, ClinVarUtil.getAmpliconsOverlappingPosition(cp, set).contains(p6));
		
	}
	
	
	@Test
	public void getMapEntryWithHighestSWScore() {
		assertEquals(null, ClinVarUtil.getPositionWithBestScore(null));
		assertEquals(null, ClinVarUtil.getPositionWithBestScore(new HashMap<ChrPosition, String[]>()));
		
		HashMap<ChrPosition, String[]> map = new HashMap<>();
		ChrPosition cp = ChrPositionUtils.getChrPositionFromString("chr1:12345-12345");
		map.put(cp, new String[3]);
		
		assertEquals(cp, ClinVarUtil.getPositionWithBestScore(map));
		ChrPosition cp2 = ChrPositionUtils.getChrPositionFromString("chr1:12346-12346");
		map.put(cp2, new String[3]);
		
		assertEquals(null, ClinVarUtil.getPositionWithBestScore(map));
		
		String[] swDiffs = new String[]{"ABCDEFG","|||||||","ABCDEFG"};
		map.put(cp2, swDiffs);
		assertEquals(cp2, ClinVarUtil.getPositionWithBestScore(map));
		map.put(cp, swDiffs);
		assertEquals(null, ClinVarUtil.getPositionWithBestScore(map));
		
		String[] swDiffs2 = new String[]{"ABCDEFGH","||||||||","ABCDEFGH"};
		ChrPosition cp3 = ChrPositionUtils.getChrPositionFromString("chr1:12347-12347");
		map.put(cp3, swDiffs2);
		assertEquals(cp3, ClinVarUtil.getPositionWithBestScore(map));
	}
	
	
	@Test
	public void editDistanceDist() {
		try {
			assertEquals("", ClinVarUtil.breakdownEditDistanceDistribution(null));
			Assert.fail("sHould have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		assertEquals("", ClinVarUtil.breakdownEditDistanceDistribution(new TIntArrayList()));
		assertEquals("1:1", ClinVarUtil.breakdownEditDistanceDistribution(new TIntArrayList(new int[]{1})));
		assertEquals("1:2", ClinVarUtil.breakdownEditDistanceDistribution(new TIntArrayList(new int[]{1,1})));
		assertEquals("1:3", ClinVarUtil.breakdownEditDistanceDistribution(new TIntArrayList(new int[]{1,1,1})));
		assertEquals("2:1,1:3", ClinVarUtil.breakdownEditDistanceDistribution(new TIntArrayList(new int[]{1,1,1,2})));
		assertEquals("2:2,1:3", ClinVarUtil.breakdownEditDistanceDistribution(new TIntArrayList(new int[]{1,1,2,1,2})));
		assertEquals("12:1,2:2,1:3", ClinVarUtil.breakdownEditDistanceDistribution(new TIntArrayList(new int[]{1,1,2,12,2,1})));
	}
	
	
	@Test
	public void basicEditDistanceDouble() {
		try {
			assertArrayEquals(new int[]{0,0}, ClinVarUtil.getDoubleEditDistance(null, null, null, null,0));
			Assert.fail("sHould have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		try {
			assertArrayEquals(new int[]{0,0}, ClinVarUtil.getDoubleEditDistance("hello", "ello", "hello", null,0));
			Assert.fail("sHould have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		
		assertArrayEquals(new int[] {3,Integer.MAX_VALUE}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "XXX", "YYY", 2));
		assertArrayEquals(new int[] {3,3}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "XXX", "YYY", 3));
		assertArrayEquals(new int[] {0,3}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "ABC", "YYY", 2));
		assertArrayEquals(new int[] {3,Integer.MAX_VALUE}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "DEF", "NOP", 2));
		assertArrayEquals(new int[] {3,Integer.MAX_VALUE}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "EFG", "NOP", 2));
		assertArrayEquals(new int[] {0,3}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "ABC", "NOP", 2));
		assertArrayEquals(new int[] {0,3}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "ABC", "KLM", 2));
		assertArrayEquals(new int[] {0,3}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "ABC", "KLM", 2));
		assertArrayEquals(new int[] {0,3}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "ABC", "JKL", 2));
		assertArrayEquals(new int[] {0,2}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "ABC", "IJK", 2));
		assertArrayEquals(new int[] {0,1}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "ABC", "XIJ", 2));
		assertArrayEquals(new int[] {0,0}, ClinVarUtil.getDoubleEditDistance("ABCDEFG", "HIJKLMNOP", "ABC", "HIJ", 2));
	}
	
//	@Test
//	public void getPositionOfLargestInArray() {
//		assertEquals(0, ClinVarUtil.getPositionWithBestScore(new int[] {1}));
//		assertEquals(0, ClinVarUtil.getPositionWithBestScore(new int[] {10}));
//		assertEquals(0, ClinVarUtil.getPositionWithBestScore(new int[] {100}));
//		assertEquals(0, ClinVarUtil.getPositionWithBestScore(new int[] {100,1}));
//		assertEquals(1, ClinVarUtil.getPositionWithBestScore(new int[] {1,100,1}));
//		assertEquals(-1, ClinVarUtil.getPositionWithBestScore(new int[] {100,100,1}));
//		assertEquals(0, ClinVarUtil.getPositionWithBestScore(new int[] {101,100,1}));
//		assertEquals(2, ClinVarUtil.getPositionWithBestScore(new int[] {101,100,103}));
//	}
	
	@Test
	public void getBestPosition() {
		
		long[][] tilePositions = new long[1][];
		tilePositions[0] = new long[]{100};
		assertArrayEquals(new long[]{100,1}, ClinVarUtil.getBestStartPosition(tilePositions, 13, 0));
		tilePositions[0] = new long[]{100, 1000};
		assertArrayEquals(new long[]{1000,1,100,1}, ClinVarUtil.getBestStartPosition(tilePositions, 13, 0));
		tilePositions[0] = new long[]{100, 1000,10000};
		assertArrayEquals(new long[]{10000,1,1000,1,100,1}, ClinVarUtil.getBestStartPosition(tilePositions, 13, 0));
	
		
		tilePositions = new long[2][];
		tilePositions[0] = new long[]{100};
		tilePositions[1] = new long[]{113};
		assertArrayEquals(new long[]{100,2}, ClinVarUtil.getBestStartPosition(tilePositions, 13, 0));
		tilePositions[0] = new long[]{100, 1000};
		tilePositions[1] = new long[]{113};
		assertArrayEquals(new long[]{100,2}, ClinVarUtil.getBestStartPosition(tilePositions, 13, 0));
		
		tilePositions = new long[3][];
		tilePositions[0] = new long[]{100};
		tilePositions[1] = new long[]{113};
		tilePositions[2] = new long[]{126};
		assertArrayEquals(new long[]{100,3}, ClinVarUtil.getBestStartPosition(tilePositions, 13, 0));
		tilePositions[0] = new long[]{100, 1000};
		tilePositions[1] = new long[]{113};
		tilePositions[2] = new long[]{126, 1013};
		assertArrayEquals(new long[]{100,3}, ClinVarUtil.getBestStartPosition(tilePositions, 13, 0));
	}
	
	@Test
	public void bestPositionsRealLifeData() {
		/*
		 * QIMR13579:data oliverh$ grep -w TCAAAAAAAGGAA q3tiledaligner.out.gz.condensed
TCAAAAAAAGGAA	C643
QIMR13579:data oliverh$
QIMR13579:data oliverh$
QIMR13579:data oliverh$
QIMR13579:data oliverh$ grep -w TTCCATAACTTCT q3tiledaligner.out.gz.condensed
TTCCATAACTTCT	54441301,81004583,91993417,119383169,158841860,171177226,187449800,195842600,238696762,246645164,249907876,263512805,290664374,413525506,442042580,516502511,532313353,543900614,545914178,557033451,570539881,619567319,660817481,692183056,720334805,737404231,763939540,780329395,825243023,840024227,844739893,894152354,904823369,906179614,910993866,916934338,938768668,949235902,970632708,983875719,1001889192,1027416205,1043834398,1079585857,1135078366,1212848363,1241051369,1241743198,1250009141,1261914836,1266039985,1353060209,1385926859,1398339451,1422701441,1493785729,1499634944,1632342192,1634265466,1660879831,1683113904,1688546475,1696070843,1724658502,1730534307,1736825538,1744955547,1763612917,1846691662,1882841257,1884705941,1900518458,1938961645,1963966293,1968123935,1968173529,1970981465,1976292989,1977036653,2010368967,2010369283,2028165352,2055169346,2057690489,2059613402,2113415446,2125704662,2131365251,2142635021,2151129028,2158614647,2175299682,2245537376,2272456933,2330078621,2332649455,2351038537,2364825490,2387773275,2404946193,2432058312,2463593115,2539267700,2539268013,2543194015,2545546255,2547462156,2569183788,2587608326,2621061713,2646899912,2654728923,2716688667,2758309228,2763598036,2766568503,2773002306,2898978353,2901924904,2918511117,2935092487,2944787668,2952404849,2953877262,2976620086,2992612126,3016130929,3031260578
QIMR13579:data oliverh$
QIMR13579:data oliverh$
QIMR13579:data oliverh$
QIMR13579:data oliverh$ grep -w TGCTAAGTCCTGA q3tiledaligner.out.gz.condensed
TGCTAAGTCCTGA	68236928,181877274,289174219,379871042,407464590,433734841,531132793,617626140,635267381,715900883,760044870,800473049,848398617,917477066,1009939623,1125070610,1169151520,1175631072,1185537588,1360190696,1414582806,1427478754,1427478845,1427478936,1427479027,1519792815,1611882893,1643538072,1793223481,1838300648,1861258204,1880846658,1976293002,2017227347,2173524984,2186432022,2282138469,2337043497,2463890593,2486972151,2531042628,2549985692,2611145621,2627424189,2636419256,2849128607,2897877268,2898412131,2910339381,2972513655,3020093124
QIMR13579:data oliverh$
		 */
		long[][] tilePositions = new long[3][];
		tilePositions[0] = new long[]{Long.MAX_VALUE};
		tilePositions[1] = new long[] {1963966293,1968123935,1968173529,1970981465,1976292989,1977036653,2010368967,2131365251,2142635021,2151129028l,2330078621l};
		tilePositions[2] = new long[]{1861258204,1880846658,1976293002,2017227347,2173524984l,2186432022l};
//		tilePositions[1] = new long[] {54441301l,81004583l,91993417l,119383169l,158841860l,171177226l,187449800l,195842600l,238696762l,246645164l,249907876l,263512805l,290664374,413525506,442042580,516502511,532313353,543900614,545914178,557033451,570539881,619567319,660817481,692183056,720334805,737404231,763939540,780329395,825243023,840024227,844739893,894152354,904823369,906179614,910993866,916934338,938768668,949235902,970632708,983875719,1001889192,1027416205,1043834398,1079585857,1135078366,1212848363,1241051369,1241743198,1250009141,1261914836,1266039985,1353060209,1385926859,1398339451,1422701441,1493785729,1499634944,1632342192,1634265466,1660879831,1683113904,1688546475,1696070843,1724658502,1730534307,1736825538,1744955547,1763612917,1846691662,1882841257,1884705941,1900518458,1938961645,1963966293,1968123935,1968173529,1970981465,1976292989,1977036653,2010368967,2010369283,2028165352,2055169346,2057690489,2059613402,2113415446,2125704662,2131365251,2142635021,2151129028l,2158614647l,2175299682l,2245537376l,2272456933l,2330078621l,2332649455l,2351038537l,2364825490l,2387773275l,2404946193l,2432058312l,2463593115l,2539267700l,2539268013l,2543194015l,2545546255l,2547462156l,2569183788l,2587608326l,2621061713l,2646899912l,2654728923l,2716688667l,2758309228l,2763598036l,2766568503l,2773002306l,2898978353l,2901924904l,2918511117l,2935092487l,2944787668l,2952404849l,2953877262l,2976620086l,2992612126l,3016130929l,3031260578l};
//		tilePositions[2] = new long[]{68236928,181877274,289174219,379871042,407464590,433734841,531132793,617626140,635267381,715900883,760044870,800473049,848398617,917477066,1009939623,1125070610,1169151520,1175631072,1185537588,1360190696,1414582806,1427478754,1427478845,1427478936,1427479027,1519792815,1611882893,1643538072,1793223481,1838300648,1861258204,1880846658,1976293002,2017227347,2173524984l,2186432022l,2282138469l,2337043497l,2463890593l,2486972151l,2531042628l,2549985692l,2611145621l,2627424189l,2636419256l,2849128607l,2897877268l,2898412131l,2910339381l,2972513655l,3020093124l};
		long [] results = ClinVarUtil.getBestStartPosition(tilePositions, 13, 0);
		assertEquals(2, results.length);
		
	}
	
	
	@Test
	public void addMDAndNMTags() {
		SAMRecord rec = new SAMRecord(null);
		rec.setAlignmentStart(1);
		rec.setReferenceName("chr1");
		CigarElement ce = new CigarElement(225, CigarOperator.MATCH_OR_MISMATCH);
		List<CigarElement> ces = new ArrayList<>();
		ces.add(ce);
		rec.setCigar(new Cigar(ces));
		rec.setReadString("TGGGGGTCTGAGTGATGGGGTCCAGGAATACATTTAGGTCCAATGGCAAGCTGGCTGAAATTCTTGTATAATAAAATAGGTTGGTAATATGGCTCTTCTCAGACATGTGATCAAGATTCCTTGACTAACAAGATATATATATATATCTTTCTAGCTCATCATACTGGCTAGTGGTGGACCCCAAGCTTTAGTAAATATAATGAGGACCTATACTTACGAAAAACT");
		rec.setReadName("1_1_1");
		ClinVarUtil.calculateMdAndNmTags(rec, "TGGGGGTCTGAGTGATGGGGTCCAGGAATACATTTAGGTCCAATGGCAAGCTGGCTGAAATTCTTGTATAATAAAATAGGTTGGTAATATGGCTCTTCTCAGACATGTGATCAAGATTCCTTGACTAACAAGATATATATATATATCTTTCTAGCTCATCATACTGGCTAGTGGTGGACCCCAAGCTTTAGTAAATATAATGAGGACCTATACTTACGAAAAACT".getBytes(), true, true);
		rec.setAlignmentStart(100);
		
		assertEquals("225", rec.getAttribute("MD"));
		
		ce = new CigarElement(230, CigarOperator.MATCH_OR_MISMATCH);
		ces = new ArrayList<>();
		ces.add(ce);
		rec.setCigar(new Cigar(ces));
		rec.setReadString("CCAAGCACCTCAGGGGAACAGGCTCCTCCCGCCGCGGGAGTCCGACCGTCCTCGACCTGCGGTGGCGGCTCGGCGGGGACTGAAGCTGCTCCTCAGACCTTCCTCCGTCTCCGCCTCCCCTCGCTCTCCGCTCCCGGGGCCGGGCCAACGCTGCTGCCACAGACCGAGAGGCTTAAAATGGCGCCGCACAAGGAGCTCTTATAAGTCGCGCAGAAGCCGCTGTATCCTGC");
		rec.setReadName("1_1_1");
		rec.setAlignmentStart(1);
		ClinVarUtil.calculateMdAndNmTags(rec, "CCAAGCACCTCAGGGGAACAGGCTCCTCCCGCCGCGGGAGTCCGACCGTCCTCGACCTGCGGTGGCGGCTCGGCGGGGACTGAAGCTGCTCCTCAGACCTTCCTCCGTCTCCGCCTCCCCTCGCTCTCCGCTCCCGGGGCCGGGCCAACGCTGCTGCCACAGACCGAGAGGCTTAAAATGGCGCCGCACAAGGAGCTCTTATAAGTCGCGCAGAAGCCGCTGTATCCTGC".getBytes(), true, true);
		rec.setAlignmentStart(100);
		
		assertEquals("230", rec.getAttribute("MD"));
		
		ces = new ArrayList<>();
		CigarElement indel = new CigarElement(1, CigarOperator.SOFT_CLIP);
		ce = new CigarElement(226, CigarOperator.MATCH_OR_MISMATCH);
		ces.add(indel);
		ces.add(ce);
		rec.setCigar(new Cigar(ces));
		rec.setReadString("GAGAATCATCTGGATTATAGACCAGTGGCACTGTTGTTTCACAAGATGATGTTTGAAACTATTCCAATGTTCAGTGGCGGAACTTGCAGTAAGTGCTTGAAATTCTCATCCTTCCATGTATTGGAACAGTTTTCTTAACCATATCTAGAAGTTTACATAAAAATTTAGAAAGAAATTTACCACATTTGAAATTTATGCAGGAGACTATATTTCTGAAGCATTTGAAC");
		rec.setReadName("1_1_1");
		rec.setAlignmentStart(1);
		ClinVarUtil.calculateMdAndNmTags(rec, "AGAATCATCTGGATTATAGACCAGTGGCACTGTTGTTTCACAAGATGATGTTTGAAACTATTCCAATGTTCAGTGGCGGAACTTGCAGTAAGTGCTTGAAATTCTCATCCTTCCATGTATTGGAACAGTTTTCTTAACCATATCTAGAAGTTTACATAAAAATTTAGAAAGAAATTTACCACATTTGAAATTTATGCAGGAGACTATATTTCTGAAGCATTTGAAC".getBytes(), true, true);
		rec.setAlignmentStart(100);
		
		assertEquals("226", rec.getAttribute("MD"));
		
		/*
		 * 56M1I171M
		 */
		ces = new ArrayList<>();
		ce = new CigarElement(56, CigarOperator.MATCH_OR_MISMATCH);
		indel = new CigarElement(1, CigarOperator.INSERTION);
		CigarElement match = new CigarElement(170, CigarOperator.MATCH_OR_MISMATCH);
		ces.add(ce);
		ces.add(indel);
		ces.add(match);
		rec.setCigar(new Cigar(ces));
		rec.setReadString("TTATCAAGAGGGATAAAACACCATGAAAATAAACTTGAATAAACTGAAAATGGACCTTTTTTTTTTTTAATGGCAATAGGACATTGTGTCAGATTACCAGTTATAGGAACAATTCTCTTTTCCTGACCAATCTTGTTTTACCCTATACATCCACAGGGTTTTGACACTTGTTGTCCAGTTGAAAAAAGGTTGTGTAGCTGTGTCATGTATATACCTTTTTGTGTCAA");
		rec.setReadName("1_1_1");
		rec.setAlignmentStart(1);
		ClinVarUtil.calculateMdAndNmTags(rec, "TTATCAAGAGGGATAAAACACCATGAAAATAAACTTGAATAAACTGAAAATGGACCTTTTTTTTTTTAATGGCAATAGGACATTGTGTCAGATTACCAGTTATAGGAACAATTCTCTTTTCCTGACCAATCTTGTTTTACCCTATACATCCACAGGGTTTTGACACTTGTTGTCCAGTTGAAAAAAGGTTGTGTAGCTGTGTCATGTATATACCTTTTTGTGTCAA".getBytes(), true, true);
		rec.setAlignmentStart(100);
		
		assertEquals("226", rec.getAttribute("MD"));
	}
	
	@Test
	public void doesComparatorWork() {
		String origBB = "A,471,1/4;C,17,1/1";
		assertEquals("C,17,1/1;A,471,1/4", ClinVarUtil.getSortedBBString(origBB, "A"));
		assertEquals(origBB, ClinVarUtil.getSortedBBString(origBB, "C"));
		assertEquals(origBB, ClinVarUtil.getSortedBBString(origBB, "G"));
		assertEquals(origBB, ClinVarUtil.getSortedBBString(origBB, "T"));
		
	}
	@Test
	public void doesComparatorWorkINdel() {
		//CT	C,CTT	.	.	END=41265954	BB	C,29,96/11579(29);CT,431,96/11790(396),96/11834(18),96/11722(17);CTT,28,96/11736(28)
		String origBB = "C,29,96/11579(29);CT,431,96/11790(396),96/11834(18),96/11722(17);CTT,28,96/11736(28)";
		assertEquals("C,29,96/11579(29);CTT,28,96/11736(28);CT,431,96/11790(396),96/11834(18),96/11722(17)", ClinVarUtil.getSortedBBString(origBB, "CT"));
		
	}
	
	@Test
	public void basicEditDistance() {
		try {
			assertEquals("", ClinVarUtil.getBasicEditDistance(null,null));
			Assert.fail("sHould have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals("", ClinVarUtil.getBasicEditDistance("",""));
			Assert.fail("sHould have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals("", ClinVarUtil.getBasicEditDistance("hello","oh my goodness"));
			Assert.fail("sHould have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		assertEquals(0, ClinVarUtil.getBasicEditDistance("hello", "hello"));
		assertEquals(1, ClinVarUtil.getBasicEditDistance("hello", "hallo"));
		assertEquals(4, ClinVarUtil.getBasicEditDistance("hello", " hell"));
		assertEquals(2, ClinVarUtil.getBasicEditDistance("crap", "carp"));
	}
	
	
	@Test
	public void getEditDistances() {
		try {
			assertArrayEquals(new int[]{0,0}, ClinVarUtil.getBasicAndLevenshteinEditDistances(null, null));
			Assert.fail("sHould have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		try {
			assertArrayEquals(new int[]{0,0}, ClinVarUtil.getBasicAndLevenshteinEditDistances("", ""));
			Assert.fail("sHould have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		
		assertArrayEquals(new int[]{0,0}, ClinVarUtil.getBasicAndLevenshteinEditDistances("A", "A"));
		assertArrayEquals(new int[]{0,0}, ClinVarUtil.getBasicAndLevenshteinEditDistances("AC", "AC"));
		assertArrayEquals(new int[]{0,0}, ClinVarUtil.getBasicAndLevenshteinEditDistances("ACG", "ACG"));
		assertArrayEquals(new int[]{0,0}, ClinVarUtil.getBasicAndLevenshteinEditDistances("ACGT", "ACGT"));
		
		assertArrayEquals(new int[]{1,1}, ClinVarUtil.getBasicAndLevenshteinEditDistances("A", "C"));
		assertArrayEquals(new int[]{1,1}, ClinVarUtil.getBasicAndLevenshteinEditDistances("C", "A"));
		assertArrayEquals(new int[]{1,1}, ClinVarUtil.getBasicAndLevenshteinEditDistances("ACC", "AAC"));
		
		assertArrayEquals(new int[]{3,2}, ClinVarUtil.getBasicAndLevenshteinEditDistances("AACCGGTT", "ACCGGTTT"));
		
		assertArrayEquals(new int[]{3,2}, ClinVarUtil.getBasicAndLevenshteinEditDistances("frog", "fog "));
		
		assertArrayEquals(new int[]{13,12}, ClinVarUtil.getBasicAndLevenshteinEditDistances("GCCCCGTGCCCCAGCCCTGCGCCCCTTCCTC", "GCCCTGCGCCCCTTCCTCTCCCGTCGTCACC"));
	}
	
	@Test
	public void getEditDistancesRealLife() {
		//CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA, r1OverlapRC: CCATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAG, basicED: 51, led: 2
		String s = "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA";
		String t = "CCATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAG";
		assertArrayEquals(new int[]{51,2}, ClinVarUtil.getBasicAndLevenshteinEditDistances(s, t));
		
		assertEquals(1, ClinVarUtil.noOfSlidesToGetPerfectMatch(s, t));
		
		t = t.substring(1);
		s = s.substring(0, s.length() -1);
		
		assertArrayEquals(new int[]{0,0}, ClinVarUtil.getBasicAndLevenshteinEditDistances(s, t));
	}
	
	@Test
	public void getMutationFromSWDataNoMut() {
		String [] swData = new String[3];
		swData[0] = "ACGT";
		swData[1] = "||||";
		swData[2] = "ACGT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(0, mutations.size());
	}
	
	@Test
	public void getMutationFromSWDataMut() {
		String [] swData = new String[3];
		swData[0] = "ACGT";
		swData[1] = "|.||";
		swData[2] = "ATGT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(1), p.getLeft());
		assertEquals("C/T", p.getRight());
		
		swData[0] = "ACGT";
		swData[1] = ".|||";
		swData[2] = "TCGT";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(0), p.getLeft());
		assertEquals("A/T", p.getRight());
		
		swData[0] = "ACGT";
		swData[1] = "||.|";
		swData[2] = "ACAT";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(2), p.getLeft());
		assertEquals("G/A", p.getRight());
		
		swData[0] = "ACGT";
		swData[1] = "|||.";
		swData[2] = "ACGC";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(3), p.getLeft());
		assertEquals("T/C", p.getRight());
	}
	
	@Test
	public void getMutationFromSWDataMultipleMut() {
		String [] swData = new String[3];
		swData[0] = "ACGTACGT";
		swData[1] = "|..||..|";
		swData[2] = "ATTTAGTG";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(4, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(1), p.getLeft());
		assertEquals("C/T", p.getRight());
		
		p = mutations.get(1);
		assertEquals(Integer.valueOf(2), p.getLeft());
		assertEquals("G/T", p.getRight());
		
		p = mutations.get(2);
		assertEquals(Integer.valueOf(5), p.getLeft());
		assertEquals("C/G", p.getRight());
		
		p = mutations.get(3);
		assertEquals(Integer.valueOf(6), p.getLeft());
		assertEquals("G/T", p.getRight());
		
	}
	
	@Test
	public void getMutationFromSWDataDel() {
		String [] swData = new String[3];
		swData[0] = "AACGT";
		swData[1] = "| |||";
		swData[2] = "A-CGT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(0), p.getLeft());
		assertEquals("AA/A", p.getRight());
		
		swData[0] = "ACGT";
		swData[1] = "| ||";
		swData[2] = "A-GT";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(0), p.getLeft());
		assertEquals("AC/A", p.getRight());
		
		swData[0] = "ACGT";
		swData[1] = "|| |";
		swData[2] = "AC-T";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(1), p.getLeft());
		assertEquals("CG/C", p.getRight());
		
		swData[0] = "ACGT";
		swData[1] = "||| ";
		swData[2] = "ACG-";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(2), p.getLeft());
		assertEquals("GT/G", p.getRight());
	}
	
	@Test
	public void getMutationFromSWDataMultiBaseDel() {
		String [] swData = new String[3];
		swData[0] = "ACGT";
		swData[1] = "  ||";
		swData[2] = "--GT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(-1), p.getLeft());
		assertEquals("AC/", p.getRight());
		
		swData[0] = "ACGT";
		swData[1] = "|  |";
		swData[2] = "A--T";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(0), p.getLeft());
		assertEquals("ACG/A", p.getRight());
		
		swData[0] = "ACGT";
		swData[1] = "||  ";
		swData[2] = "AC--";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(1), p.getLeft());
		assertEquals("CGT/C", p.getRight());
	}
	
	@Test
	public void getMutationFromSWDataMultiBaseIns() {
		String [] swData = new String[3];
		swData[0] = "A--GT";
		swData[1] = "|  ||";
		swData[2] = "AACGT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(0), p.getLeft());
		assertEquals("A/AAC", p.getRight());
		
		swData[0] = "A--TAC--A";
		swData[1] = "|  |||  |";
		swData[2] = "ACGTACGTA";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(2, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(0), p.getLeft());
		assertEquals("A/ACG", p.getRight());
		p = mutations.get(1);
		assertEquals(Integer.valueOf(3), p.getLeft());
		assertEquals("C/CGT", p.getRight());
		
		swData[0] = "AC--";
		swData[1] = "||  ";
		swData[2] = "ACGT";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(1), p.getLeft());
		assertEquals("C/CGT", p.getRight());
	}
	
	@Test
	public void getMutationFromSWDataMultiBaseInsAndDel() {
		String [] swData = new String[3];
		swData[0] = "A--GTACGT";
		swData[1] = "|  ||  ||";
		swData[2] = "AACGT--GT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(2, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(0), p.getLeft());
		assertEquals("A/AAC", p.getRight());
		
		p = mutations.get(1);
		assertEquals(Integer.valueOf(2), p.getLeft());
		assertEquals("TAC/T", p.getRight());
	}
	
	@Test
	public void getMutationFromSWDataMultiBaseIndelAndSnp() {
		String [] swData = new String[3];
		swData[0] = "A--GTACGT";
		swData[1] = "|  ||||.|";
		swData[2] = "AACGTACAT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(2, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(0), p.getLeft());
		assertEquals("A/AAC", p.getRight());
		
		p = mutations.get(1);
		assertEquals(Integer.valueOf(5), p.getLeft());
		assertEquals("G/A", p.getRight());
		
		swData[0] = "ACGTACGT";
		swData[1] = "||.| |||";
		swData[2] = "ACTT-CGT";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(2, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(2), p.getLeft());
		assertEquals("G/T", p.getRight());
		
		p = mutations.get(1);
		assertEquals(Integer.valueOf(3), p.getLeft());
		assertEquals("TA/T", p.getRight());
		
		
		swData[0] = "AC-TACGT";
		swData[1] = "|| |.|||";
		swData[2] = "ACGTTCGT";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(2, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(1), p.getLeft());
		assertEquals("C/CG", p.getRight());
		
		p = mutations.get(1);
		assertEquals(Integer.valueOf(3), p.getLeft());
		assertEquals("A/T", p.getRight());
		
		swData[0] = "ACGTACGT";
		swData[1] = "||||.|||";
		swData[2] = "ACGTTCGT";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(4), p.getLeft());
		assertEquals("A/T", p.getRight());
	}
	
	@Test
	public void getMutationFromSWDataIns() {
		String [] swData = new String[3];
		swData[0] = "-CGT";
		swData[1] = " |||";
		swData[2] = "ACGT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(-1), p.getLeft());
		assertEquals("/A", p.getRight());
		
		swData[0] = "A-GT";
		swData[1] = "| ||";
		swData[2] = "ACGT";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(0), p.getLeft());
		assertEquals("A/AC", p.getRight());
		
		swData[0] = "AC-T";
		swData[1] = "|| |";
		swData[2] = "ACGT";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(1), p.getLeft());
		assertEquals("C/CG", p.getRight());
		
		swData[0] = "ACG-";
		swData[1] = "||| ";
		swData[2] = "ACGT";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(2), p.getLeft());
		assertEquals("G/GT", p.getRight());
	}
	
	@Test
	public void areDeletionPositionsAccurate() {
		/*
		 * TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC
||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTC-TTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC

		* expecting a single deletion at position 89
		 */
		String [] swData = new String[3];
		swData[0] = "TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		swData[1] = "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swData[2] = "TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTC-TTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(88), p.getLeft());
		assertEquals("CT/C", p.getRight());
	}
	
	
	@Test
	public void getMutationFromSWDataSingleSmallInsertion() {
		String [] swData = new String[3];
		swData[0] = "AAGGTGAGTTCTGGAATGTAGAAGTAGGAGGCTGCTGGGGAGTCTGCGAGGAAACTTGATTTCTAGCAAAATCTTGTGTGATAATTTGCTGTGAATGAGAAATGAAGGAAGTGGTAAAATTCATTGAGTACTTGC-AAAAAAAAAATAGTATTAAGAAATCTAGATATCTTTATTATAAATTTCTTTTTCTATATGAAATCTGCTTTCCCCATGATCAAAAAAGAAAAATTAACTAATAAGAATAATGAAAAACTTACACAGATGTGA";
		swData[1] = "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swData[2] = "AAGGTGAGTTCTGGAATGTAGAAGTAGGAGGCTGCTGGGGAGTCTGCGAGGAAACTTGATTTCTAGCAAAATCTTGTGTGATAATTTGCTGTGAATGAGAAATGAAGGAAGTGGTAAAATTCATTGAGTACTTGCAAAAAAAAAAATAGTATTAAGAAATCTAGATATCTTTATTATAAATTTCTTTTTCTATATGAAATCTGCTTTCCCCATGATCAAAAAAGAAAAATTAACTAATAAGAATAATGAAAAACTTACACAGATGTGA";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(134), p.getLeft());
		assertEquals("C/CA", p.getRight());
	}
	@Test
	public void getMutationFromSWDataSingleSmallDeletion() {
		/*
		 * CAGATCATGTCAGAGAGAGAGCTTGGTTAACTTGGGAGAAAGTTTCATCTGTGGATGGAGTATTGGTAAGGATTTTCTTAAAACGTTTTGAAATTTTTTTTTCTCATTTTAAAAACAACTTCAAATCACTATACAAAAATTGAAAGATAGAAAAATATAAAGACAATAAAAGCTAATAATAATTCCATTACCCAGAGGAAATTTACCTCTGCTAACATTAAAAATG
20:34:46.042 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
20:34:46.042 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - CAGATCATGTCAGAGAGAGAGCTTGGTTAACTTGGGAGAAAGTTTCATCTGTGGATGGAGTATTGGTAAGGATTTTCTTAAAACGTTTTGAAA-TTTTTTTTCTCATTTTAAAAACAACTTCAAATCACTATACAAAAATTGAAAGATAGAAAAATATAAAGACAATAAAAGCTAATAATAATTCCATTACCCAGAGGAAATTTACCTCTGCTAACATTAAAAATG
2
		 */
		String [] swData = new String[3];
		swData[0] = "CAGATCATGTCAGAGAGAGAGCTTGGTTAACTTGGGAGAAAGTTTCATCTGTGGATGGAGTATTGGTAAGGATTTTCTTAAAACGTTTTGAAATTTTTTTTTCTCATTTTAAAAACAACTTCAAATCACTATACAAAAATTGAAAGATAGAAAAATATAAAGACAATAAAAGCTAATAATAATTCCATTACCCAGAGGAAATTTACCTCTGCTAACATTAAAAATG";
		swData[1] = "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swData[2] = "CAGATCATGTCAGAGAGAGAGCTTGGTTAACTTGGGAGAAAGTTTCATCTGTGGATGGAGTATTGGTAAGGATTTTCTTAAAACGTTTTGAAA-TTTTTTTTCTCATTTTAAAAACAACTTCAAATCACTATACAAAAATTGAAAGATAGAAAAATATAAAGACAATAAAAGCTAATAATAATTCCATTACCCAGAGGAAATTTACCTCTGCTAACATTAAAAATG";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(92), p.getLeft());
		assertEquals("AT/A", p.getRight());
	}
	
	@Test
	public void getMutationFromSWDataSnpAndDeletion() {
	/*
	 * GAAACCACAGAGAACAGTTCCCCTGAGTGCACAGTCCATTTAGAGAAAACTGGAAAAGGATTATGTGCTACAAAATTGAGTGCCAGTTCAGAGGACATTTCTGAGAGACTGGCCAGCATTTCAGTAGGACCTTCTAGTTCAACAACAACAACAACAACAACAACAGAGCAACCAAAGCCAATGGTTCAAACAAAAGGCAGACCCCACAGTCAGTGTTTGAACTCCTCT
21:14:57.649 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - |||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||      |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
21:14:57.649 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - GAAACCACAGAGAACAGTTCCCCTGAGTGCACAATCCATTTAGAGAAAACTGGAAAAGGATTATGTGCTACAAAATTGAGTGCCAGTTCAGAGGACATTTCTGAGAGACTGGCCAGCATTTCAGTAGGACCTTCTAGTT------CAACAACAACAACAACAACAGAGCAACCAAAGCCAATGGTTCAAACAAAAGGCAGACCCCACAGTCAGTGTTTGAACTCCTCT
*/
		String [] swData = new String[3];
		swData[0] = "GAAACCACAGAGAACAGTTCCCCTGAGTGCACAGTCCATTTAGAGAAAACTGGAAAAGGATTATGTGCTACAAAATTGAGTGCCAGTTCAGAGGACATTTCTGAGAGACTGGCCAGCATTTCAGTAGGACCTTCTAGTTCAACAACAACAACAACAACAACAACAGAGCAACCAAAGCCAATGGTTCAAACAAAAGGCAGACCCCACAGTCAGTGTTTGAACTCCTCT";
		swData[1] = "|||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||      |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swData[2] = "GAAACCACAGAGAACAGTTCCCCTGAGTGCACAATCCATTTAGAGAAAACTGGAAAAGGATTATGTGCTACAAAATTGAGTGCCAGTTCAGAGGACATTTCTGAGAGACTGGCCAGCATTTCAGTAGGACCTTCTAGTT------CAACAACAACAACAACAACAGAGCAACCAAAGCCAATGGTTCAAACAAAAGGCAGACCCCACAGTCAGTGTTTGAACTCCTCT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(2, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(33), p.getLeft());
		assertEquals("G/A", p.getRight());
		p = mutations.get(1);
		assertEquals(Integer.valueOf(138), p.getLeft());
		assertEquals("TCAACAA/T", p.getRight());
	}
	
	@Test
	public void getMutationFromSWDataMultipleSnpAndDeletion() {
		/*
		 *  TGCAAGTGAGTGGTACAAGAGTGCAGACTCACAGTTTAAATTATTCTCTTACCATTAGACGCAGGCATATAGGGTCTGCACATGCTACAATCAAAACCAATGTCTGCTACATTTTCCACTTCTTCCTCAGTATTTAAGTTCTGACAAACTGCATGCATCCATCTAAAAAGACCATATTTGTACATTTTTTTTAAAAAATGGAATATACTGAGAACTGCTACCTTTTAAAACCTGTAACACTGAGTCTTCAAACTTAAAAGCCCTAAGCCTCAC
21:36:50.089 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - |||||||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||| |||||..|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
21:36:50.089 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - TGCAAGTGAGTGGTACAAGAGTGCAGACTCACAGTTTAAATTACTCTCTTACCATTAGACGCAGGCATATAGGGTCTGCACATGTTACAATCAAAACCAATGTCTGCTACATTTTCCACTTCTTCCTCAGTATTTAAGTTCTGACGAACTGCATGCATCCATCTAAAAAGACCATATTTGTACA-TTTTTAAAAAAAATGGAATATACTGAGAACTGCTACCTTTTAAAACCTGTAACACTGAGTCTTCAAACTTAAAAGCCCTAAGCCTCAC
21:36:50.093 [main] INFO au.
		 */
		String [] swData = new String[3];
		swData[0] = "TGCAAGTGAGTGGTACAAGAGTGCAGACTCACAGTTTAAATTATTCTCTTACCATTAGACGCAGGCATATAGGGTCTGCACATGCTACAATCAAAACCAATGTCTGCTACATTTTCCACTTCTTCCTCAGTATTTAAGTTCTGACAAACTGCATGCATCCATCTAAAAAGACCATATTTGTACATTTTTTTTAAAAAATGGAATATACTGAGAACTGCTACCTTTTAAAACCTGTAACACTGAGTCTTCAAACTTAAAAGCCCTAAGCCTCAC";
		swData[1] = "|||||||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||| |||||..|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swData[2] = "TGCAAGTGAGTGGTACAAGAGTGCAGACTCACAGTTTAAATTACTCTCTTACCATTAGACGCAGGCATATAGGGTCTGCACATGTTACAATCAAAACCAATGTCTGCTACATTTTCCACTTCTTCCTCAGTATTTAAGTTCTGACGAACTGCATGCATCCATCTAAAAAGACCATATTTGTACA-TTTTTAAAAAAAATGGAATATACTGAGAACTGCTACCTTTTAAAACCTGTAACACTGAGTCTTCAAACTTAAAAGCCCTAAGCCTCAC";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(6, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(43), p.getLeft());
		assertEquals("T/C", p.getRight());
		p = mutations.get(1);
		assertEquals(Integer.valueOf(84), p.getLeft());
		assertEquals("C/T", p.getRight());
		p = mutations.get(2);
		assertEquals(Integer.valueOf(145), p.getLeft());
		assertEquals("A/G", p.getRight());
		p = mutations.get(3);
		assertEquals(Integer.valueOf(183), p.getLeft());
		assertEquals("AT/A", p.getRight());
		p = mutations.get(4);
		assertEquals(Integer.valueOf(190), p.getLeft());
		assertEquals("T/A", p.getRight());
		p = mutations.get(5);
		assertEquals(Integer.valueOf(191), p.getLeft());
		assertEquals("T/A", p.getRight());
	}
	@Test
	public void getMutationFromSWDataMultipleDeletionsAndSnp() {
		/*
		 *   TACAAATAAGGTTCAAGCACTGTATTTAAATATTTAAAAGATAGAGGAGTTTCTTAAAATACCACATATGGTGCTCTTTCTTGTGAGCTTGCTTTTCTCCACAATTTGGCAATTTGCTTCACTCTAGTAGTCCAATCTGCAACAAAAGAACAGAGTATAACACTTTCTCAGAGCCATGCTAATGATGTGTTGTAATAAAGAATGTTGATGAACTGCTGACAGTTAATCTTATTCAGGCCGTATTCTCATGAGG
08:57:51.079 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - ||||||||||||||||||||||||||||||||||||||||||  ||||||||||||||||||||||||||||||    ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
08:57:51.079 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - TACAAATAAGGTTCAAGCACTGTATTTAAATATTTAAAAGAT--AGGAGTTTCTTAAAATACCACATATGGTGC----TCTTGTGAGCTTGCTTTTCTCCACAATTTGGCAATTTGCTTCACTCTAGTAGTCCAATCTGCAACAAAAGAACAGAATATAACACTTTCTCAGAGCCATGCTAATGATGTGTTGTATTAAAGAATGTTGATGAACTGCTGACAGTTAATCTTATTCAGGCCGTATTCTCATGAGG

		 */
		String [] swData = new String[3];
		swData[0] = "TACAAATAAGGTTCAAGCACTGTATTTAAATATTTAAAAGATAGAGGAGTTTCTTAAAATACCACATATGGTGCTCTTTCTTGTGAGCTTGCTTTTCTCCACAATTTGGCAATTTGCTTCACTCTAGTAGTCCAATCTGCAACAAAAGAACAGAGTATAACACTTTCTCAGAGCCATGCTAATGATGTGTTGTAATAAAGAATGTTGATGAACTGCTGACAGTTAATCTTATTCAGGCCGTATTCTCATGAGG";
		swData[1] = "||||||||||||||||||||||||||||||||||||||||||  ||||||||||||||||||||||||||||||    ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swData[2] = "TACAAATAAGGTTCAAGCACTGTATTTAAATATTTAAAAGAT--AGGAGTTTCTTAAAATACCACATATGGTGC----TCTTGTGAGCTTGCTTTTCTCCACAATTTGGCAATTTGCTTCACTCTAGTAGTCCAATCTGCAACAAAAGAACAGAATATAACACTTTCTCAGAGCCATGCTAATGATGTGTTGTATTAAAGAATGTTGATGAACTGCTGACAGTTAATCTTATTCAGGCCGTATTCTCATGAGG";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(4, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
//		assertEquals(Integer.valueOf(43), p.getLeft());
//		assertEquals("AG/--", p.getRight());
//		p = mutations.get(1);
//		assertEquals(Integer.valueOf(84), p.getLeft());
//		assertEquals("C/T", p.getRight());
		p = mutations.get(2);
		assertEquals(Integer.valueOf(154), p.getLeft());
		assertEquals("G/A", p.getRight());
//		p = mutations.get(3);
//		assertEquals(Integer.valueOf(145), p.getLeft());
//		assertEquals("A/G", p.getRight());
	}
	
	
	
	@Test
	public void getMutationFromSWDataMultipleSnp() {
		/*
11:23:14.844 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - AAATATGCTTCACTTCAGAAGACATTTTCAGGTCTTCACTATCAACTTCATTAGAAATCTGTTTTTCCAATTCAGTATTCACTGTATGTTGGGATGATACTACAAAATTCAGAACATTTGTTATGGCAATGTACAAACAAATTTTAAATTTTCTAACTATAGATATATAAAACATTTGGCTACACTAGAACTTAAATCAGAAGGTATTCATCAAAGCAGACAATT
11:23:14.844 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - ||||||||||||||||||||||||||||||..||||||||||||...||||||||||||||||||||||||||.|||||||||||||||||||||||||.||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
11:23:14.844 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - AAATATGCTTCACTTCAGAAGACATTTTCATTTCTTCACTATCAGTCTCATTAGAAATCTGTTTTTCCAATTCGGTATTCACTGTATGTTGGGATGATATTACAAAATTCAGAACATTTGTTATGGTAATGTACAAACAAATTTTAAATTTTCTAACTATAGATATATAAAACATTTGGCTACACTAGAACTTAAATCAGAAGGTATTCATCAAAGCAGACAATT
n] INFO au.edu.qimr.clinvar.Q3ClinVar - TACAAATAAGGTTCAAGCACTGTATTTAAATATTTAAAAGAT--AGGAGTTTCTTAAAATACCACATATGGTGC----TCTTGTGAGCTTGCTTTTCTCCACAATTTGGCAATTTGCTTCACTCTAGTAGTCCAATCTGCAACAAAAGAACAGAATATAACACTTTCTCAGAGCCATGCTAATGATGTGTTGTATTAAAGAATGTTGATGAACTGCTGACAGTTAATCTTATTCAGGCCGTATTCTCATGAGG

		 */
		
		// I have deleted the first 25 bases from each string to make things easier...
		String [] swData = new String[3];
		swData[0] = "TTTCAGGTCTTCACTATCAACTTCATTAGAAATCTGTTTTTCCAATTCAGTATTCACTGTATGTTGGGATGATACTACAAAATTCAGAACATTTGTTATGGCAATGTACAAACAAATTTTAAATTTTCTAACTATAGATATATAAAACATTTGGCTACACTAGAACTTAAATCAGAAGGTATTCATCAAAGCAGACAATT";
		swData[1] = "|||||..||||||||||||...||||||||||||||||||||||||||.|||||||||||||||||||||||||.||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swData[2] = "TTTCATTTCTTCACTATCAGTCTCATTAGAAATCTGTTTTTCCAATTCGGTATTCACTGTATGTTGGGATGATATTACAAAATTCAGAACATTTGTTATGGTAATGTACAAACAAATTTTAAATTTTCTAACTATAGATATATAAAACATTTGGCTACACTAGAACTTAAATCAGAAGGTATTCATCAAAGCAGACAATT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(8, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(5), p.getLeft());
		assertEquals("G/T", p.getRight());
		
		p = mutations.get(1);
		assertEquals(Integer.valueOf(6), p.getLeft());
		assertEquals("G/T", p.getRight());
		
		p = mutations.get(2);
		assertEquals(Integer.valueOf(19), p.getLeft());
		assertEquals("A/G", p.getRight());
		
		p = mutations.get(3);
		assertEquals(Integer.valueOf(20), p.getLeft());
		assertEquals("C/T", p.getRight());
		
		p = mutations.get(4);
		assertEquals(Integer.valueOf(21), p.getLeft());
		assertEquals("T/C", p.getRight());
	}
	
	@Test
	public void getMutationFromSWDataMultipleSnpDiffBin() {
		/*
11																						AATATGCTTCACTTCAGAAGACATTTTCAGGTCTTCACTATCAACTTCATTAGAAATCTGTTTTTCCAATTCAGTATTCACTGTATGTTGGGATGATACTACAAAATTCAGAACATTTGTTATGGCAATGTACAAACAAATTTTAAATTTTCTAACTATAGATATATAAAACATTTGGCTACACTAGAACTTAAATCAGAAGGTATTCATCAAAGCAGACAATT
11:51:37.621 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - |||||||||||||||||||||||||||||..||||||||||||...||||||||||||||||||||||||||.|||||||||||||||||||||||||.||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
11:51:37.621 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - AATATGCTTCACTTCAGAAGACATTTTCATTTCTTCACTATCAGTCTCATTAGAAATCTGTTTTTCCAATTCGGTATTCACTGTATGTTGGGATGATATTACAAAATTCAGAACATTTGTTATGGTAATGTACAAACAAATTTTAAATTTTCTAACTATAGATATATAAAACATTTGGCTACACTAGAACTTAAATCAGAAGGTATTCATCAAAGCAGACAATT
		 */
		
		// I have deleted the first 25 bases from each string to make things easier...
		String [] swData = new String[3];
		swData[0] = "TTCAGGTCTTCACTATCAACTTCATTAGAAATCTGTTTTTCCAATTCAGTATTCACTGTATGTTGGGATGATACTACAAAATTCAGAACATTTGTTATGGCAATGTACAAACAAATTTTAAATTTTCTAACTATAGATATATAAAACATTTGGCTACACTAGAACTTAAATCAGAAGGTATTCATCAAAGCAGACAATT";
		swData[1] = "||||..||||||||||||...||||||||||||||||||||||||||.|||||||||||||||||||||||||.||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swData[2] = "TTCATTTCTTCACTATCAGTCTCATTAGAAATCTGTTTTTCCAATTCGGTATTCACTGTATGTTGGGATGATATTACAAAATTCAGAACATTTGTTATGGTAATGTACAAACAAATTTTAAATTTTCTAACTATAGATATATAAAACATTTGGCTACACTAGAACTTAAATCAGAAGGTATTCATCAAAGCAGACAATT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(8, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(4), p.getLeft());
		assertEquals("G/T", p.getRight());
		
		p = mutations.get(1);
		assertEquals(Integer.valueOf(5), p.getLeft());
		assertEquals("G/T", p.getRight());
		
		p = mutations.get(2);
		assertEquals(Integer.valueOf(18), p.getLeft());
		assertEquals("A/G", p.getRight());
		
		p = mutations.get(3);
		assertEquals(Integer.valueOf(19), p.getLeft());
		assertEquals("C/T", p.getRight());
		
		p = mutations.get(4);
		assertEquals(Integer.valueOf(20), p.getLeft());
		assertEquals("T/C", p.getRight());
	}
	
	
	@Test
	public void getMutationFromSWDataMultipleSnpAndIndel() {
		/*
		 * swData
ATCCTTATTTGATGAAATATCTGCAGTAGACACCTAT-AAAAAGCAAAATACACAGAATACGAAGTTATATTTTTCACTTGTTTTACACTTAACTGGAAAGCTTCAGAAAATTCATAATCAAAACATATATTTTGGCTAAGGTCTAGAATAACAATTCCAAATATTAATGCTAAGATACTACCGTAAAATGGAGTCGTGACATTTTATTATTCACCTAATTCTCTCTTTAGAGGTAG
||||||||||||||||||||||||||||||||||||| |||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||||||||.|||..||||||||||||.||||||||||||||||||||||||||||||||||||||||
ATCCTTATTTGATGAAATATCTGCAGTAGACACCTATAAAAAAGCAAAATACACAAAATACGAAGTTATATTTTTCACTTGTTTTACACTTAACTGGAAAGCTTCAGAAAATTCATAATCAAAACATATATTTTTGCTAAGGTCTAGAATAACAATTCCAAATATTAATGCTAAGATAGTACTATAAAATGGAGTCATGACATTTTATTATTCACCTAATTCTCTCTTTAGAGGTAG

xxData - missing the initial insertion
 ATCCTTATTTGATGAAATATCTGCAGTAGACACCTATAAAAAGCAAAATACACAGAATACGAAGTTATATTTTTCACTTGTTTTACACTTAACTGGAAAGCTTCAGAAAATTCATAATCAAAACATATATTTTGGCTAAGGTCTAGAATAACAATTCCAAATATTAATGCTAAGATACTACCGTAAAATGGAGTCGTGACATTTTATTATTCACCTAATTCTCTCTTTAGAGGTAG
||||||||||||||||||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||||||||.|||..||||||||||||.||||||||||||||||||||||||||||||||||||||||
ATCCTTATTTGATGAAATATCTGCAGTAGACACCTATAAAAAGCAAAATACACAAAATACGAAGTTATATTTTTCACTTGTTTTACACTTAACTGGAAAGCTTCAGAAAATTCATAATCAAAACATATATTTTTGCTAAGGTCTAGAATAACAATTCCAAATATTAATGCTAAGATAGTACTATAAAATGGAGTCATGACATTTTATTATTCACCTAATTCTCTCTTTAGAGGTAG

		 */
		
		// I have deleted the first 25 bases from each string to make things easier...
		String [] swData = new String[3];
		swData[0] = "ATCCTTATTTGATGAAATATCTGCAGTAGACACCTAT-AAAAAGCAAAATACACAGAATACGAAGTTATATTTTTCACTTGTTTTACACTTAACTGGAAAGCTTCAGAAAATTCATAATCAAAACATATATTTTGGCTAAGGTCTAGAATAACAATTCCAAATATTAATGCTAAGATACTACCGTAAAATGGAGTCGTGACATTTTATTATTCACCTAATTCTCTCTTTAGAGGTAG";
		swData[1] = "||||||||||||||||||||||||||||||||||||| |||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||||||||.|||..||||||||||||.||||||||||||||||||||||||||||||||||||||||";
		swData[2] = "ATCCTTATTTGATGAAATATCTGCAGTAGACACCTATAAAAAAGCAAAATACACAAAATACGAAGTTATATTTTTCACTTGTTTTACACTTAACTGGAAAGCTTCAGAAAATTCATAATCAAAACATATATTTTTGCTAAGGTCTAGAATAACAATTCCAAATATTAATGCTAAGATAGTACTATAAAATGGAGTCATGACATTTTATTATTCACCTAATTCTCTCTTTAGAGGTAG";
		String [] xxData = new String[3];
		xxData[0] = "ATCCTTATTTGATGAAATATCTGCAGTAGACACCTATAAAAAGCAAAATACACAGAATACGAAGTTATATTTTTCACTTGTTTTACACTTAACTGGAAAGCTTCAGAAAATTCATAATCAAAACATATATTTTGGCTAAGGTCTAGAATAACAATTCCAAATATTAATGCTAAGATACTACCGTAAAATGGAGTCGTGACATTTTATTATTCACCTAATTCTCTCTTTAGAGGTAG";
		xxData[1] = "||||||||||||||||||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.|||||||||||||||||||||||||||||||||||||||||||.|||..||||||||||||.||||||||||||||||||||||||||||||||||||||||";
		xxData[2] = "ATCCTTATTTGATGAAATATCTGCAGTAGACACCTATAAAAAGCAAAATACACAAAATACGAAGTTATATTTTTCACTTGTTTTACACTTAACTGGAAAGCTTCAGAAAATTCATAATCAAAACATATATTTTTGCTAAGGTCTAGAATAACAATTCCAAATATTAATGCTAAGATAGTACTATAAAATGGAGTCATGACATTTTATTATTCACCTAATTCTCTCTTTAGAGGTAG";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(7, mutations.size());
		List<Pair<Integer, String>> mutations2 = ClinVarUtil.getPositionRefAndAltFromSW(xxData);
		assertEquals(6, mutations2.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(36), p.getLeft());
		assertEquals("T/TA", p.getRight());
		
		p = mutations.get(1);
		assertEquals(Integer.valueOf(54), p.getLeft());
		assertEquals("G/A", p.getRight());
		Pair<Integer, String> p2 = mutations2.get(0);
		assertEquals(Integer.valueOf(54), p2.getLeft());
		assertEquals("G/A", p2.getRight());
//		
//		p = mutations.get(2);
//		assertEquals(Integer.valueOf(18), p.getLeft());
//		assertEquals("A/G", p.getRight());
//		
//		p = mutations.get(3);
//		assertEquals(Integer.valueOf(19), p.getLeft());
//		assertEquals("C/T", p.getRight());
//		
//		p = mutations.get(4);
//		assertEquals(Integer.valueOf(20), p.getLeft());
//		assertEquals("T/C", p.getRight());
	}
	
	@Test
	public void getMutationString() {
		/*
		 * AACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC
 ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
 AACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC

		 */
		
		String [] swDiffs = new String[3];
		swDiffs[0] = "AACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		swDiffs[1] = "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swDiffs[2] = "AACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		
		assertEquals(null, ClinVarUtil.getMutationString(-1, 1, swDiffs));
		assertEquals(null, ClinVarUtil.getMutationString(0, 0, swDiffs));
		assertEquals("A", ClinVarUtil.getMutationString(0, 1, swDiffs));
		assertEquals("A", ClinVarUtil.getMutationString(1, 1, swDiffs));
		assertEquals("C", ClinVarUtil.getMutationString(2, 1, swDiffs));
		assertEquals("C", ClinVarUtil.getMutationString(3, 1, swDiffs));
		
		swDiffs[0] = "ACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		swDiffs[1] = "|||||||| |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swDiffs[2] = "ACAGATTC-TTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		
		assertEquals("C", ClinVarUtil.getMutationString(7, 1, swDiffs));
		assertEquals("C", ClinVarUtil.getMutationString(7, 2, swDiffs));
		assertEquals("CT", ClinVarUtil.getMutationString(7, 3, swDiffs));
		 
		 // this data has no mutations - should just get CT back
		swDiffs[0] = "AACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		swDiffs[1] = "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swDiffs[2] = "AACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		assertEquals("TTTTCT", ClinVarUtil.getMutationString(20, 6, swDiffs));
		
		
		// insertion
		swDiffs[0] = "ATTTCTAAAAATATTTCAATGGGTCATATCACAGATTC-TTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		swDiffs[1] = "|||||||||||||||||||||||||||||||||||||| ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swDiffs[2] = "ATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		assertEquals("CT", ClinVarUtil.getMutationString(37, 1, swDiffs));
	}
	
	@Test
	public void getMutationStringRealLife() {
		/*
		 * 	AGGGGAAAAATATGACAAAGAAAGCTATATAAGATATTATTTTATTTTACAGAGTAACAGACTAGCTAGAGACAATGAATTAAGGGAAAATGACAAAGAACAGCTCAAAGCAATTTCTACACGAGATCCTCTCTCTGAAATCACTGAGCAGGAGAAAGATTTTCTATGGAGTCACAGGTAAGTGCTAAAATGGAGATTCTCTGTTTCTTTTTCTTTATTACAGAAAAAATAACTGAATTTGGCTGATCTCAGC
			||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||| .|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
			AGGGGAAAAATATGACAAAGAAAGCTATATAAGATATTATTTTATTTTACAGAGTAACAGACTAGCTAGAGACAATGAATTAAGGGAAAATGACAAAGAACAGCTCAAAGCAATTTCTACACGAGATCCTCTCTCTGAAATCACTGCGCAGGAGAAAGATTTTCTATGGA-CCACAGGTAAGTGCTAAAATGGAGATTCTCTGTTTCTTTTTCTTTATTACAGAAAAAATAACTGAATTTGGCTGATCTCAGC
		 */
		String [] swDiffs = new String[3];
		swDiffs[0] = "AGGGGAAAAATATGACAAAGAAAGCTATATAAGATATTATTTTATTTTACAGAGTAACAGACTAGCTAGAGACAATGAATTAAGGGAAAATGACAAAGAACAGCTCAAAGCAATTTCTACACGAGATCCTCTCTCTGAAATCACTGAGCAGGAGAAAGATTTTCTATGGAGTCACAGGTAAGTGCTAAAATGGAGATTCTCTGTTTCTTTTTCTTTATTACAGAAAAAATAACTGAATTTGGCTGATCTCAGC";
		swDiffs[1] = "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||| .|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swDiffs[2] = "AGGGGAAAAATATGACAAAGAAAGCTATATAAGATATTATTTTATTTTACAGAGTAACAGACTAGCTAGAGACAATGAATTAAGGGAAAATGACAAAGAACAGCTCAAAGCAATTTCTACACGAGATCCTCTCTCTGAAATCACTGCGCAGGAGAAAGATTTTCTATGGA-CCACAGGTAAGTGCTAAAATGGAGATTCTCTGTTTCTTTTTCTTTATTACAGAAAAAATAACTGAATTTGGCTGATCTCAGC";
		assertEquals("C", ClinVarUtil.getMutationString(146, 1, swDiffs));
		assertEquals("A", ClinVarUtil.getMutationString(169, 2, swDiffs));
		assertEquals("C", ClinVarUtil.getMutationString(171, 1, swDiffs));
	}
	
	@Test
	public void getMutationStringRealLife2() {
		/*
		 * 	AGGGGAAAAATATGACAAAGAAAGCTATATAAGATATTATTTTATTTTACAGAGTAACAGACTAGCTAGAGACAATGAATTAAGGGAAAATGACAAAGAACAGCTCAAAGCAATTTCTACACGAGATCCTCTCTCTGAAATCACTGAGCAGGAGAAAGATTTTCTATGGAGTCACAGGTAAGTGCTAAAATGGAGATTCTCTGTTTCTTTTTCTTTATTACAGAAAAAATAACTGAATTTGGCTGATCTCAGC
			||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||| .|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
			AGGGGAAAAATATGACAAAGAAAGCTATATAAGATATTATTTTATTTTACAGAGTAACAGACTAGCTAGAGACAATGAATTAAGGGAAAATGACAAAGAACAGCTCAAAGCAATTTCTACACGAGATCCTCTCTCTGAAATCACTGCGCAGGAGAAAGATTTTCTATGGA-CCACAGGTAAGTGCTAAAATGGAGATTCTCTGTTTCTTTTTCTTTATTACAGAAAAAATAACTGAATTTGGCTGATCTCAGC
		 */
		String [] swDiffs = new String[3];
		swDiffs[0] = "AGGGGAAAAATATGACAAAGAAAGCTATATAAGATATTATTTTATTTTACAGAGTAACAGACTAGCTAGAGACAATGAATTAAGGGAAAATGACAAAGAACAGCTCAAAGCAATTTCTACACGAGATCCTCTCTCTGAAATCACTGAGCAGGAGAAAGATTTTCTATGGAGTCACAGGTAAGTGCTAAAATGGAGATTCTCTGTTTCTTTTTCTTTATTACAGAAAAAATAACTGAATTTGGCTGATCTCAGC";
		swDiffs[1] = "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||.||||||||||||||||||||||| .|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swDiffs[2] = "AGGGGAAAAATATGACAAAGAAAGCTATATAAGATATTATTTTATTTTACAGAGTAACAGACTAGCTAGAGACAATGAATTAAGGGAAAATGACAAAGAACAGCTCAAAGCAATTTCTACACGAGATCCTCTCTCTGAAATCACTGCGCAGGAGAAAGATTTTCTATGGA-CCACAGGTAAGTGCTAAAATGGAGATTCTCTGTTTCTTTTTCTTTATTACAGAAAAAATAACTGAATTTGGCTGATCTCAGC";
		assertEquals("C", ClinVarUtil.getMutationString(146, 1, swDiffs));
		assertEquals("A", ClinVarUtil.getMutationString(169, 2, swDiffs));
		assertEquals("C", ClinVarUtil.getMutationString(171, 1, swDiffs));
	}
	
	@Test
	public void getPositionInString() {
		assertEquals(0, ClinVarUtil.getZeroBasedPositionInString(0, 0));
		assertEquals(0, ClinVarUtil.getZeroBasedPositionInString(1, 1));
		assertEquals(0, ClinVarUtil.getZeroBasedPositionInString(2, 2));
		assertEquals(1, ClinVarUtil.getZeroBasedPositionInString(3, 2));
		
		assertEquals(2, ClinVarUtil.getZeroBasedPositionInString(41265867, 41265865));
	}
	
	@Test
	public void getMutationFromSWRealLifeData() {
		/*
TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC
||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTC-TTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC
		 */
		
		// I have deleted the first 25 bases from each string to make things easier...
		String [] swData = new String[3];
		swData[0] = "TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		swData[1] = "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swData[2] = "TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTC-TTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
	}
	
}
