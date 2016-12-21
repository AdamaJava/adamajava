package org.qcmg.qprofiler.util;

import static org.junit.Assert.assertEquals;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.model.SummaryByCycle;
import org.qcmg.common.model.SummaryByCycleNew2;
import org.qcmg.qprofiler.summarise.PositionSummary;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SummaryReportUtilsTest {
	@Test
	public void testTallyBadReads() {
		ConcurrentMap<Integer, AtomicLong> badReadCount = new ConcurrentHashMap<Integer, AtomicLong>();
		
		// empty string
		String badRead = "";
		SummaryReportUtils.tallyBadReads(badRead, badReadCount);
		Assert.assertNotNull(badReadCount);
		Assert.assertEquals(1, badReadCount.get(Integer.valueOf(0)).get());
		
		badRead = "1234";
		SummaryReportUtils.tallyBadReads(badRead, badReadCount);
		Assert.assertEquals((2), badReadCount.get(Integer.valueOf(0)).get());
		
		badRead = "1.34";
		SummaryReportUtils.tallyBadReads(badRead, badReadCount);
		Assert.assertEquals((2), badReadCount.get(Integer.valueOf(0)).get());
		Assert.assertEquals((1), badReadCount.get(Integer.valueOf(1)).get());
		
		badRead = ".234";
		SummaryReportUtils.tallyBadReads(badRead, badReadCount);
		Assert.assertEquals((2), badReadCount.get(Integer.valueOf(0)).get());
		Assert.assertEquals((2), badReadCount.get(Integer.valueOf(1)).get());
		
		badRead = ".23.";
		SummaryReportUtils.tallyBadReads(badRead, badReadCount);
		Assert.assertEquals((1), badReadCount.get(Integer.valueOf(2)).get());
		
		badRead = "N23.";
		SummaryReportUtils.tallyBadReads(badRead, badReadCount);
		Assert.assertEquals((2), badReadCount.get(Integer.valueOf(2)).get());
		
		badRead = "N...N.NNN.";
		SummaryReportUtils.tallyBadReads(badRead, badReadCount);
		Assert.assertEquals((1), badReadCount.get(Integer.valueOf(10)).get());
		
		badRead = "N.";
		SummaryReportUtils.tallyBadReads(badRead, badReadCount);
		Assert.assertEquals((3), badReadCount.get(Integer.valueOf(2)).get());
		
		badRead = "N";
		SummaryReportUtils.tallyBadReads(badRead, badReadCount);
		Assert.assertEquals((3), badReadCount.get(Integer.valueOf(1)).get());

		
		badRead = "....1....1....1";
		SummaryReportUtils.tallyBadReads(badRead, badReadCount);
		Assert.assertEquals((1), badReadCount.get(Integer.valueOf(12)).get());
		
		// null string
		SummaryReportUtils.tallyBadReads(null, badReadCount);
		
		// null map
		SummaryReportUtils.tallyBadReads(null, null);
		
		// null map
		try {
			SummaryReportUtils.tallyBadReads("anything in here", null);
			Assert.fail("Should have thrown an AssertionError");
		} catch (AssertionError ae) {
			Assert.assertTrue(ae.getMessage().startsWith("Null map"));
		}
	}
	
	@Test
	public void testMatcherVsByteArray() {
		String inputString = "12345.12345.12345.12345...12345";
		int counter = 100000;
		
		ConcurrentMap<Integer, AtomicLong> map = new ConcurrentHashMap<Integer, AtomicLong>();
		
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < counter ; i++) {
			SummaryReportUtils.tallyBadReads(inputString, map);
		}
//		System.out.println("time taken using Matcher: " + (System.currentTimeMillis() - start));
		Assert.assertEquals(counter, map.get(6).get());
		
		
		map = new ConcurrentHashMap<Integer, AtomicLong>();
		start = System.currentTimeMillis();
		for (int i = 0 ; i < counter ; i++) {
			SummaryReportUtils.tallyBadReadsAsString(inputString, map);
		}
//		System.out.println("time taken using String: " + (System.currentTimeMillis() - start));
		Assert.assertEquals(counter, map.get(6).get());
		
		
		map = new ConcurrentHashMap<Integer, AtomicLong>();
		start = System.currentTimeMillis();
		for (int i = 0 ; i < counter ; i++) {
			SummaryReportUtils.tallyBadReads(inputString, map);
		}
//		System.out.println("time taken using Matcher: " + (System.currentTimeMillis() - start));
		Assert.assertEquals(counter, map.get(6).get());
	}
	@Test
	public void testMatcherVsString() {
		String inputString = "ACGT1NNNN1TCGA1";
		long counter = 100000;
		
		ConcurrentMap<Integer, AtomicLong> map = new ConcurrentHashMap<Integer, AtomicLong>();
		ConcurrentMap<String, AtomicLong> mapMD = new ConcurrentHashMap<String, AtomicLong>();
		
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < counter ; i++) {
			SummaryReportUtils.tallyBadReads(inputString, map, SummaryReportUtils.BAD_MD_PATTERN);
		}
//		System.out.println("testMatcherVsString: time taken using Matcher: " + (System.currentTimeMillis() - start));
		Assert.assertEquals(counter, map.get(12).get());
		
		start = System.currentTimeMillis();
		for (int i = 0 ; i < counter ; i++) {
			SummaryReportUtils.tallyBadReadsMD(inputString, mapMD);
		}
//		System.out.println("testMatcherVsString: time taken using String looping: " + (System.currentTimeMillis() - start));
		Assert.assertEquals(counter * 3, mapMD.get("4M").get());
		
		start = System.currentTimeMillis();
		map = new ConcurrentHashMap<Integer, AtomicLong>();
		for (int i = 0 ; i < counter ; i++) {
			SummaryReportUtils.tallyBadReads(inputString, map, SummaryReportUtils.BAD_MD_PATTERN);
		}
//		System.out.println("testMatcherVsString: time taken using Matcher: " + (System.currentTimeMillis() - start));
		Assert.assertEquals(counter, map.get(12).get());
		
	}
	
	@Test
	public void testTallyMDMismatches() {
		SummaryByCycle<Character> summary = new SummaryByCycle<Character>();
		String mdString = "50";
		String readBases = "AAAAAAAAAACCCCCCCCCCGGGGGGGGGGTTTTTTTTTTAAAAAAAAAAT";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases);
		Assert.assertTrue(summary.cycles().isEmpty());
		
		mdString = "20A4";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases);
		Assert.assertFalse(summary.cycles().isEmpty());
		Assert.assertEquals(1, summary.count(21, 'G').intValue());

		// update tests...
		mdString = "14C35";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases);
		Assert.assertEquals(1, summary.count(15, 'C').intValue());
		mdString = "0N49";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases);
		Assert.assertEquals(1, summary.count(1, 'A').intValue());
		mdString = "21G28";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases);
		Assert.assertEquals(1, summary.count(22, 'G').intValue());
		mdString = "13T0G32";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases);
		Assert.assertEquals(1, summary.count(14, 'C').intValue());
		Assert.assertEquals(2, summary.count(15, 'C').intValue());
		mdString = "5A0G43";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases);
		Assert.assertEquals(1, summary.count(6, 'A').intValue());
		Assert.assertEquals(1, summary.count(7, 'A').intValue());
		mdString = "16G2G30";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases);
		Assert.assertEquals(1, summary.count(17, 'C').intValue());
		Assert.assertEquals(1, summary.count(20, 'C').intValue());
		mdString = "20^G5";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases);
		Assert.assertEquals(1, summary.count(21, 'G').intValue());	// won't have increased the count - deletion
		mdString = "17^TTCCAGCTG7A0";	// CIGAR: 17M9D8M, seq: AGAGTGAGAATCTGTTGATGACTCN
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases);
		Assert.assertEquals(1, summary.count(25, 'G').intValue());
		mdString = "17^TTT7T0";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases);
		Assert.assertEquals(2, summary.count(25, 'G').intValue());
		mdString = "3^GTTGAC22";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases);
		Assert.assertEquals(1, summary.count(21, 'G').intValue());	// won't have increased the count - deletion
		mdString = "3A0T2^CCTGGATCAA18";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases);
		Assert.assertEquals(1, summary.count(4, 'A').intValue());
		Assert.assertEquals(1, summary.count(5, 'A').intValue());
		mdString = "39A6^TGCTGTGGCC4T0";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases);
		Assert.assertEquals(1, summary.count(40, 'T').intValue());
		Assert.assertEquals(1, summary.count(51, 'T').intValue());
		
		// and finally where the variant is the first character....
		mdString = "TC23";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases);
		Assert.assertEquals(2, summary.count(1, 'A').intValue());
		Assert.assertEquals(1, summary.count(2, 'A').intValue());
		
		mdString = "T24G";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases);
		Assert.assertEquals(3, summary.count(1, 'A').intValue());
		Assert.assertEquals(1, summary.count(26, 'G').intValue());
		
		//same again with 0 in front of the variation
		mdString = "0T0C23";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases);
		Assert.assertEquals(4, summary.count(1, 'A').intValue());
		Assert.assertEquals(2, summary.count(2, 'A').intValue());
		
		mdString = "0T24G";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases);
		Assert.assertEquals(5, summary.count(1, 'A').intValue());
		Assert.assertEquals(2, summary.count(26, 'G').intValue());
		
		
		// extra long mds.... taken from real life bam file
		mdString = "30^AGAAAATGTTTTTCATTTTCTTGATTTATTTCTGAATTCAGCTTGCTCTTCATTAGCGCTACATAGCTGMCTTATTATTCGTGGTC" +
				"CCCTATGACCCCCTGATCATTTTCCCTGAGGGTGCATATTTATTCACTAACTATGTTACAATCATGTGATCTGCTGGATTTTTTCTGATA" +
				"GTCTACTCTAGATTTGTTCTAAATTAATAAATCCCATTATTTTTGGCTTCTACTACTTCTATTTATTAAATTCATTCTGAATATGAAGTTTATTT" +
				"TCAAAGGAATTCATAATTCTTTACTCCRRGCTTGGTTCTAACAATGAATTTAATAAGAATTGTATTTAATCAATGTTTAAATATATTAAGGGC" +
				"AAATTTTGTAAAAATGTTAGTGTTCCAAGCTTTCCATTTCCCCACAAATTAATTTTTTTAGCCTTTCCCCTTAATCCACTTTCTT19G0";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases);
		Assert.assertEquals(1, summary.count(50, 'A').intValue());
		
		mdString = "17C2^CCACTTAATTTCATGTGATAATTTTCCCCAATGACTAACCAAATATGCTTCACTATTATATAAATCAATTCTTTCTTAATGCC" +
				"ACAAGTGAAAGTGCAAAGGTAGCTAATGGTTTTCTTCTCATAAAAATCACACTTTGGCTTTTTCCTTTCATATGTAATTAATCATATT" +
				"TGTGACAATCTTCCAAACTTACTTGAAATTTTTCTGAATCCCTTTCAAATCAGGACAAGAACTAGAAATGTCTATACAGGTTTAATAT" +
				"GAAGTAAAGAAAATGTTTTTCATTTTCTTGATTTATTTCTGAATTCAGCTTGCTCTTCATTAGCGCTACATAGCTGMCTTATTATTCG" +
				"TGGTCCCCTATGACCCCCTGATCATTTTCCCTGAGGGTGCATATTTATTCACTAACTATGTTACAATCATGTGATCTGCTGGATTTT" +
				"TTCTGATAGTCTACTCTAGATTTGTTCTAAATTAATAAATCCCATTATTTTTG30";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases);
		Assert.assertEquals(1, summary.count(18, 'C').intValue());		
	}
	
	@Test
	public void testTallyMDMismatchesRealLifeData() {
		SummaryByCycleNew2<Character> summary = new SummaryByCycleNew2<Character>(Character.MAX_VALUE, 64);
		QCMGAtomicLongArray forwardArray = new QCMGAtomicLongArray(32);
		QCMGAtomicLongArray reverseArray = new QCMGAtomicLongArray(32);
		String readBasesString = "GCTCTCCGATCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCC";
		byte[] readBases = readBasesString.getBytes();
		SummaryReportUtils.tallyMDMismatches("0T2G95", summary, readBases, false, forwardArray, reverseArray);
		
		readBasesString = "CCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACACTAAGATCGGAAGAG";
		readBases = readBasesString.getBytes();
		SummaryReportUtils.tallyMDMismatches("3C4C1T4A4", summary, readBases, false, forwardArray, reverseArray);
	}
	
	@Test
	public void testTallyMDMismatchesRealLifeData2() {
		SummaryByCycleNew2<Character> summary = new SummaryByCycleNew2<Character>(Character.MAX_VALUE, 64);
		QCMGAtomicLongArray forwardArray = new QCMGAtomicLongArray(32);
		QCMGAtomicLongArray reverseArray = new QCMGAtomicLongArray(32);
		
		//md: 99, cigar: 3M1I96M, readBases: ACAGGGATTTCGCCATGTTGGCCAGGTTGGAGATTTTATTTTTCTTAAGTCTCACTCTGTCCAGCTGGAGTGCAGCAGTGTGATCTGGGTGACTGTAGCC
		String readBasesString = "ACAGGGATTTCGCCATGTTGGCCAGGTTGGAGATTTTATTTTTCTTAAGTCTCACTCTGTCCAGCTGGAGTGCAGCAGTGTGATCTGGGTGACTGTAGCC";
		byte[] readBases = readBasesString.getBytes();
		SummaryReportUtils.tallyMDMismatches("99", summary, readBases, false, forwardArray, reverseArray);
		
		// arrays should be empty
		for (int i = 0 ; i < forwardArray.length() ; i++) {
			assertEquals(0, forwardArray.get(i));
		}
		for (int i = 0 ; i < reverseArray.length() ; i++) {
			assertEquals(0, reverseArray.get(i));
		}
		
		// md: 23C74, cigar: 80M2I18M, readBases: ACCTAACATCAGCAGTGCTTTCAATTACGTTCCTTGAACACTGTTTCTTATGTCTTATGTTATGTCATATATTTCATTACATATATATATTACATTACAT
		readBasesString = "ACCTAACATCAGCAGTGCTTTCAATTACGTTCCTTGAACACTGTTTCTTATGTCTTATGTTATGTCATATATTTCATTACATATATATATTACATTACAT";
		readBases = readBasesString.getBytes();
		SummaryReportUtils.tallyMDMismatches("23C74", summary, readBases, false, forwardArray, reverseArray);
		
		// would expect C>A on the forward as the mismatch happens before the insertion
		for (int i = 0 ; i < forwardArray.length() ; i++) {
			if (i == SummaryReportUtils.getIntFromChars('C', 'A')) {
				assertEquals(1, forwardArray.get(i));
			} else {
				assertEquals(0, forwardArray.get(i));
			}
		}
		
		
		// md: 83G12, cigar: 38M4I58M, readBases: GGCAGGGGGATCTTTTTATTTATTTATTTATTTATTTATTTTTTTTTTTGAGATAGAGTTTCACTTTGTCTCCCAGGATGGAGTAGATTGGTGTGATCTC
		readBasesString = "GGCAGGGGGATCTTTTTATTTATTTATTTATTTATTTATTTTTTTTTTTGAGATAGAGTTTCACTTTGTCTCCCAGGATGGAGTAGAATGGTGTGATCTC";
		readBases = readBasesString.getBytes();
		forwardArray = new QCMGAtomicLongArray(32);
		SummaryReportUtils.tallyMDMismatches("83G12", summary, readBases, false, forwardArray, reverseArray);
		
		// if no insertion is taken into account will be G>T
		for (int i = 0 ; i < forwardArray.length() ; i++) {
			if (i == SummaryReportUtils.getIntFromChars('G', 'T')) {
				assertEquals(1, forwardArray.get(i));
			} else {
				assertEquals(0, forwardArray.get(i));
			}
		}
		
		// if insertions are taken into account, should be G>A		
	}
	
	@Test
	public void getInsertionAdjustedReadOffsetMatch() {
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(100, CigarOperator.M));
		assertEquals(100, cigar.getReadLength());
		assertEquals(100, cigar.getReferenceLength());
		
		for (int i = 1 ; i <= cigar.getReadLength(); i++) {
			assertEquals(0, SummaryReportUtils.getInsertionAdjustedReadOffset(cigar, i));
		}
	}
	
	@Test
	public void getInsertionAdjustedReadOffsetDeletion() {
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(56, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.D));
		cigar.add(new CigarElement(45, CigarOperator.M));
		// 56 + 45 = 101
		assertEquals(101, cigar.getReadLength());
		assertEquals(102, cigar.getReferenceLength());
		
		for (int i = 1 ; i <= cigar.getReferenceLength(); i++) {
			assertEquals(0, SummaryReportUtils.getInsertionAdjustedReadOffset(cigar, i));
		}
	}
	
	@Test
	public void getInsertionAdjustedReadOffsetInsertion() {
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(56, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.I));
		cigar.add(new CigarElement(45, CigarOperator.M));
		// 56 + 1 + 45 = 102
		assertEquals(102, cigar.getReadLength());
		assertEquals(101, cigar.getReferenceLength());
		
		for (int i = 1 ; i <= cigar.getReferenceLength(); i++) {
			if (i > 56) {
				assertEquals(1, SummaryReportUtils.getInsertionAdjustedReadOffset(cigar, i));
			} else {
				assertEquals(0, SummaryReportUtils.getInsertionAdjustedReadOffset(cigar, i));
			}
		}
	}
	
	@Test
	public void getInsertionAdjustedReadOffsetInsertionAtStart() {
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(1, CigarOperator.I));
		cigar.add(new CigarElement(55, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.I));
		cigar.add(new CigarElement(45, CigarOperator.M));
		// 1 + 55 + 1 + 45 = 102
		assertEquals(102, cigar.getReadLength());
		assertEquals(100, cigar.getReferenceLength());
	}
	
	@Test
	public void getInsertionAdjustedReadOffsetInsertion2() {
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(27, CigarOperator.M));
		cigar.add(new CigarElement(3, CigarOperator.I));
		cigar.add(new CigarElement(40, CigarOperator.M));
		cigar.add(new CigarElement(4, CigarOperator.I));
		cigar.add(new CigarElement(26, CigarOperator.M));
		//27 + 3 + 40 + 4 + 26 = 100
		assertEquals(100, cigar.getReadLength());
		assertEquals(93, cigar.getReferenceLength());
		
		for (int i = 1 ; i <= cigar.getReferenceLength(); i++) {
			if (i <= 27) {
				assertEquals(0, SummaryReportUtils.getInsertionAdjustedReadOffset(cigar, i));
			} else if (i > 27 && i <= 67) {
				assertEquals(3, SummaryReportUtils.getInsertionAdjustedReadOffset(cigar, i));
			} else if (i > 67) {
				assertEquals(7, SummaryReportUtils.getInsertionAdjustedReadOffset(cigar, i));
			} else {
				assertEquals(7, SummaryReportUtils.getInsertionAdjustedReadOffset(cigar, i));
			}
		}
	}
	
	@Test
	public void getInsertionAdjustedReadOffsetInsertionDeletion() {
		//md: 	24C2C8A2A2A6A15A2A3^C7A5, cigar: 17M1I55M1D2M1I11M13S, 
		//readBases: CCAATAGGGTATAAGCATTTTTTTTTTGAACTCCCCCTTAGGCTTTTGTTTTTTTTTGTCTTTTTGTTATTAATATTTCTATTGGGGTTACGTTACATTG
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(17, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.I));
		cigar.add(new CigarElement(55, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.D));
		cigar.add(new CigarElement(2, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.I));
		cigar.add(new CigarElement(11, CigarOperator.M));
		cigar.add(new CigarElement(13, CigarOperator.S));
		//17 + 1 + 55 + 2 + 1 + 11 + 13 = 100
		assertEquals(100, cigar.getReadLength());
		//17 + 55 + 1 +  2 + 11 = 86
		assertEquals(86, cigar.getReferenceLength());
		
		for (int i = 1 ; i < cigar.getReferenceLength(); i++) {
			if (i <= 17) {
				assertEquals(0, SummaryReportUtils.getInsertionAdjustedReadOffset(cigar, i));
			} else if (i >= 18 && i <= 72) {
				assertEquals(1, SummaryReportUtils.getInsertionAdjustedReadOffset(cigar, i));
			} else if (i == 73 || i == 74) {
				assertEquals(1, SummaryReportUtils.getInsertionAdjustedReadOffset(cigar, i));
			} else if (i > 74) {
				assertEquals(2, SummaryReportUtils.getInsertionAdjustedReadOffset(cigar, i));
			}
		}
	}
	
	@Ignore
	public void tallyMDMismatchesRefEqualsAltReverse() {
		//Found refBase == altBase, md: 18C4T10G41A30A43 , cigar: 151M, 
		//seq: TTTGTTAGCTGTTCCTGGCCCAATTGCATTGTTGTTGTTGCGAGTTGTCTCCTCTCCTTTACGACCCATTTTGAACCCAAATAAGAAAATCGCTCAAATTTGCTTTTCGTCTCACATCATTTCCATAGTCTAAAATAAAACAGCAAGTAGT
		//reverse strand: true
		QCMGAtomicLongArray forwardArray = new QCMGAtomicLongArray(32);
		QCMGAtomicLongArray reverseArray = new QCMGAtomicLongArray(32);
		SummaryByCycleNew2<Character> summary = new SummaryByCycleNew2<Character>(Character.MAX_VALUE, 64);
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(151, CigarOperator.M));
		SummaryReportUtils.tallyMDMismatches("18C4T10G41A30A43", cigar, summary, "TTTGTTAGCTGTTCCTGGCCCAATTGCATTGTTGTTGTTGCGAGTTGTCTCCTCTCCTTTACGACCCATTTTGAACCCAAATAAGAAAATCGCTCAAATTTGCTTTTCGTCTCACATCATTTCCATAGTCTAAAATAAAACAGCAAGTAGT".getBytes(), true, forwardArray, reverseArray);
		
		assertEquals(1, reverseArray.get(SummaryReportUtils.getIntFromChars('C', 'G')));
		assertEquals(1, reverseArray.get(SummaryReportUtils.getIntFromChars('T', 'A')));
		assertEquals(1, reverseArray.get(SummaryReportUtils.getIntFromChars('G', 'A')));
		assertEquals(2, reverseArray.get(SummaryReportUtils.getIntFromChars('A', 'G')));
	}
	
	@Test
	public void tallyMDMismatchesRefEqualsAltReverse2() {
//		Found refBase == altBase, md: 76A24 , cigar: 101M, seq:
//			GTCCCCTGGTGTGCCTGGTAATCTTGTGTTGAATGCTAGACATTGTGCATGAAAAGCTATAGAGATGATTCATGGCTCTAAATCAGACATTGACCAGCTAT,
//			reverse strand: true
		QCMGAtomicLongArray forwardArray = new QCMGAtomicLongArray(32);
		QCMGAtomicLongArray reverseArray = new QCMGAtomicLongArray(32);
		SummaryByCycleNew2<Character> summary = new SummaryByCycleNew2<Character>(Character.MAX_VALUE, 64);
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(101, CigarOperator.M));
		SummaryReportUtils.tallyMDMismatches("76A24", cigar, summary, "GTCCCCTGGTGTGCCTGGTAATCTTGTGTTGAATGCTAGACATTGTGCATGAAAAGCTATAGAGATGATTCATGGCTCTAAATCAGACATTGACCAGCTAT".getBytes(), true, forwardArray, reverseArray);
		
		assertEquals(1, reverseArray.get(SummaryReportUtils.getIntFromChars('T', 'A')));
	}
	
	@Test
	public void tallyMDMismatchesRealLife() {
//		Found refBase == altBase, md: 1T0C1A0G0G0T0C0G0G0T0T0T0C0T0A0T0C0T0A0C0N0T0T0C0A0A0A0T0T0C0C0T0C0C0C0T0G0T0A1G0A3G3A10G19T6T3T2 , 
//		cigar: 28S96M2S, seq: CNNNNNNNNNNNNNNTNNANNNNNNNNTANNCNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNCNNAAGNACANGAGAAATAAGNCCTACTTCACAAAGCGCCTNCCCCCGNAAANGANN, 
//		reverse strand: true
		QCMGAtomicLongArray forwardArray = new QCMGAtomicLongArray(32);
		QCMGAtomicLongArray reverseArray = new QCMGAtomicLongArray(32);
		SummaryByCycleNew2<Character> summary = new SummaryByCycleNew2<Character>(Character.MAX_VALUE, 64);
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(28, CigarOperator.S));
		cigar.add(new CigarElement(96, CigarOperator.M));
		cigar.add(new CigarElement(2, CigarOperator.S));
		String result = SummaryReportUtils.tallyMDMismatches("1T0C1A0G0G0T0C0G0G0T0T0T0C0T0A0T0C0T0A0C0N0T0T0C0A0A0A0T0T0C0C0T0C0C0C0T0G0T0A1G0A3G3A10G19T6T3T2", cigar, summary, "CNNNNNNNNNNNNNNTNNANNNNNNNNTANNCNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNCNNAAGNACANGAGAAATAAGNCCTACTTCACAAAGCGCCTNCCCCCGNAAANGANN".getBytes(), true, forwardArray, reverseArray);
//		System.out.println("result: " + result);
		result = SummaryReportUtils.tallyMDMismatches("1T0C1A0G0G0T0C0G0G0T0T0T0C0T0A0T0C0T0A0C0T0T0C0A0A0A0T0T0C0C0T0C0C0C0T0G0T0A0C0G0A0A1G1A0C0A1G0A0G0A2T0A1G1C1T0A0C0T1C0A0C0A2G0C0G0C1T1C4G0T0A2T0G0A0T0", cigar, summary, "CNNNNNNNNNNNNNNTNNANNNNNNNNTANNCNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNCNNAAGNACANGAGAAATAAGNCCTACTTCACAAAGCGCCTNCCCCCGNAAANGANN".getBytes(), true, forwardArray, reverseArray);
//		System.out.println("result: " + result);
//		assertEquals(1, reverseArray.get(SummaryReportUtils.getIntFromChars('T', 'N')));
	}
	
	
	@Test
	public void tallyMDMismatchesCheckMutations() {
		// 0A94A3, cigar: 35M1I64M, 
		//readBases: GCCCTAACCCTAACCCTAACCCTAACCCTAACCCTAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCAACCCTACCCC
		QCMGAtomicLongArray forwardArray = new QCMGAtomicLongArray(32);
		QCMGAtomicLongArray reverseArray = new QCMGAtomicLongArray(32);
		SummaryByCycleNew2<Character> summary = new SummaryByCycleNew2<Character>(Character.MAX_VALUE, 64);
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(35, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.I));
		cigar.add(new CigarElement(64, CigarOperator.M));
		SummaryReportUtils.tallyMDMismatches("0A94A3", cigar, summary, "GCCCTAACCCTAACCCTAACCCTAACCCTAACCCTAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCAACCCTACCCC".getBytes(), false, forwardArray, reverseArray);
		
		//expecting to see 1 A>G and 1 A>C - but this one could be A>A
		
		assertEquals(1, forwardArray.get(SummaryReportUtils.getIntFromChars('A', 'G')));
		assertEquals(1, forwardArray.get(SummaryReportUtils.getIntFromChars('A', 'C')));
	}
	
	@Test
	public void tallyMDMismatchesCheckMutations2() {
		// 94C2G0 , cigar: 92M1I1M1I5M, 
		//seq: CAGTAAGGTTAGATAGAAAACATTCCCCTCAAGCAACATAATAAACAATGAGAAGTAACTATACCTAGTTTGGATATATAAAGTCCTGCATCTGTCTCTT, reverse strand: false
		QCMGAtomicLongArray forwardArray = new QCMGAtomicLongArray(32);
		QCMGAtomicLongArray reverseArray = new QCMGAtomicLongArray(32);
		SummaryByCycleNew2<Character> summary = new SummaryByCycleNew2<Character>(Character.MAX_VALUE, 64);
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(92, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.I));
		cigar.add(new CigarElement(1, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.I));
		cigar.add(new CigarElement(5, CigarOperator.M));
		SummaryReportUtils.tallyMDMismatches("94C2G0", cigar, summary, "CAGTAAGGTTAGATAGAAAACATTCCCCTCAAGCAACATAATAAACAATGAGAAGTAACTATACCTAGTTTGGATATATAAAGTCCTGCATCTGTCTCTT".getBytes(), false, forwardArray, reverseArray);
		
		//expecting to see 1 C>T and 1 G>T
		
		assertEquals(1, forwardArray.get(SummaryReportUtils.getIntFromChars('G', 'T')));
		assertEquals(1, forwardArray.get(SummaryReportUtils.getIntFromChars('C', 'T')));
	}
	
	@Test
	public void tallyMDMismatchesCheckMutations3() {
		//md: 4A9T5 , cigar: 22S7M1I13M57S, 
		//seq: GCAAAGGACCCTGTGGTCAGTGGCGGGGGAGGGGGCTGGTGGGGGGCGGGGGGAGAGAGGTTCCTGGTCGCCTGGTGATGGCAGCTCCTCCCCCCGCCTC, reverse strand: false
		QCMGAtomicLongArray forwardArray = new QCMGAtomicLongArray(32);
		QCMGAtomicLongArray reverseArray = new QCMGAtomicLongArray(32);
		SummaryByCycleNew2<Character> summary = new SummaryByCycleNew2<Character>(Character.MAX_VALUE, 64);
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(22, CigarOperator.S));
		cigar.add(new CigarElement(7, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.I));
		cigar.add(new CigarElement(13, CigarOperator.M));
		cigar.add(new CigarElement(57, CigarOperator.S));
		SummaryReportUtils.tallyMDMismatches("4A9T5", cigar, summary, "GCAAAGGACCCTGTGGTCAGTGGCGGGGGAGGGGGCTGGTGGGGGGCGGGGGGAGAGAGGTTCCTGGTCGCCTGGTGATGGCAGCTCCTCCCCCCGCCTC".getBytes(), false, forwardArray, reverseArray);
		
		//expecting to see 1 A>G and 1 T>G
		
		assertEquals(1, forwardArray.get(SummaryReportUtils.getIntFromChars('A', 'G')));
		assertEquals(1, forwardArray.get(SummaryReportUtils.getIntFromChars('T', 'G')));
	}
	
	@Test
	public void tallyMDMismatchesCheckMutations4() {
		//md: 4A9T5 , cigar: 22S7M1I13M57S, 
		//seq: GCAAAGGACCCTGTGGTCAGTGGCGGGGGAGGGGGCTGGTGGGGGGCGGGGGGAGAGAGGTTCCTGGTCGCCTGGTGATGGCAGCTCCTCCCCCCGCCTC, reverse strand: false
		QCMGAtomicLongArray forwardArray = new QCMGAtomicLongArray(32);
		QCMGAtomicLongArray reverseArray = new QCMGAtomicLongArray(32);
		SummaryByCycleNew2<Character> summary = new SummaryByCycleNew2<Character>(Character.MAX_VALUE, 64);
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(22, CigarOperator.S));
		cigar.add(new CigarElement(7, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.I));
		cigar.add(new CigarElement(13, CigarOperator.M));
		cigar.add(new CigarElement(57, CigarOperator.S));
		SummaryReportUtils.tallyMDMismatches("4A9T5", cigar, summary, "GCAAAGGACCCTGTGGTCAGTGGCGGGGGAGGGGGCTGGTGGGGGGCGGGGGGAGAGAGGTTCCTGGTCGCCTGGTGATGGCAGCTCCTCCCCCCGCCTC".getBytes(), false, forwardArray, reverseArray);
		
		//expecting to see 1 A>G and 1 T>G
		
		assertEquals(1, forwardArray.get(SummaryReportUtils.getIntFromChars('A', 'G')));
		assertEquals(1, forwardArray.get(SummaryReportUtils.getIntFromChars('T', 'G')));
	}
	
	@Test
	public void tallyMDMismatchesDeletion() {
		//md: 92^AA2G2T1 , cigar: 92M2D1M1I6M, 
		//seq: GGATAGCTGTATACCCTTCAGGTCTTTTCCCCAAATACGATTGCCTAAAACAAAACATTATTAAAAGTTGTTCAAGGTCATGATCCTCCAACCTGTCTCT, reverse strand: false
		QCMGAtomicLongArray forwardArray = new QCMGAtomicLongArray(32);
		QCMGAtomicLongArray reverseArray = new QCMGAtomicLongArray(32);
		SummaryByCycleNew2<Character> summary = new SummaryByCycleNew2<Character>(Character.MAX_VALUE, 64);
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(92, CigarOperator.M));
		cigar.add(new CigarElement(2, CigarOperator.D));
		cigar.add(new CigarElement(1, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.I));
		cigar.add(new CigarElement(6, CigarOperator.M));
		SummaryReportUtils.tallyMDMismatches("92^AA2G2T1", cigar, summary, "GGATAGCTGTATACCCTTCAGGTCTTTTCCCCAAATACGATTGCCTAAAACAAAACATTATTAAAAGTTGTTCAAGGTCATGATCCTCCAACCTGTCTCT".getBytes(), false, forwardArray, reverseArray);
		
		//expecting to see 1 C>T and 1 G>T
		
		assertEquals(1, forwardArray.get(SummaryReportUtils.getIntFromChars('T', 'C')));
		assertEquals(1, forwardArray.get(SummaryReportUtils.getIntFromChars('G', 'T')));
	}
	
	@Ignore
	public void tallyMDMismatchesInsertion() {
		//HWI-ST1445:86:C4CKMACXX:2:2308:11384:83325       163     chr1    450820  0       98I3M   =       450820  129     GTCTTTTTTTTTTTTTTTTTTTTTTTAAAAGGGGGGGGGCGGGGGGGCCCCCCCCTGTAACCCCAGCAATTTGGGGGACTGGGGGGGGGGGGTCTCTTGGG   BBBFFFFFFFFFFIIIIIFFFFFFB0<BBB#######################################################################   XA:i:2  MD:Z:0G0T0T2A1A1A0A0A0A0A0A0A0A0A0A0A0A1A0A0A0A0A0A0A3A0A0A0A0A0A0A0A0A0A0A1A1A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A23A1A0   NM:i:67 ZW:f:0.0
		QCMGAtomicLongArray forwardArray = new QCMGAtomicLongArray(32);
		QCMGAtomicLongArray reverseArray = new QCMGAtomicLongArray(32);
		SummaryByCycleNew2<Character> summary = new SummaryByCycleNew2<Character>(Character.MAX_VALUE, 64);
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(98, CigarOperator.I));
		cigar.add(new CigarElement(3, CigarOperator.M));
		SummaryReportUtils.tallyMDMismatches("0G0T0T2A1A1A0A0A0A0A0A0A0A0A0A0A0A1A0A0A0A0A0A0A3A0A0A0A0A0A0A0A0A0A0A1A1A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A23A1A0", cigar, summary, "GTCTTTTTTTTTTTTTTTTTTTTTTTAAAAGGGGGGGGGCGGGGGGGCCCCCCCCTGTAACCCCAGCAATTTGGGGGACTGGGGGGGGGGGGTCTCTTGGG".getBytes(), false, forwardArray, reverseArray);
		
		
	}
	
	@Test
	public void tallyMDMismatchesDeletion2() {
		//md: 94^AG2G1G0 , cigar: 94M2D1M1I4M, 
		//seq: TCTCACATGAGAGTAACTAGCATCTTTCTCTCAGATGATGAAGATGATGAAGAGGAAGATGAAGAGGAAGAAATCGACGTGGTCACTGTGGAGACTGTCT, reverse strand: false
		QCMGAtomicLongArray forwardArray = new QCMGAtomicLongArray(32);
		QCMGAtomicLongArray reverseArray = new QCMGAtomicLongArray(32);
		SummaryByCycleNew2<Character> summary = new SummaryByCycleNew2<Character>(Character.MAX_VALUE, 64);
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(94, CigarOperator.M));
		cigar.add(new CigarElement(2, CigarOperator.D));
		cigar.add(new CigarElement(1, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.I));
		cigar.add(new CigarElement(4, CigarOperator.M));
		SummaryReportUtils.tallyMDMismatches("94^AG2G1G0", cigar, summary, "TCTCACATGAGAGTAACTAGCATCTTTCTCTCAGATGATGAAGATGATGAAGAGGAAGATGAAGAGGAAGAAATCGACGTGGTCACTGTGGAGACTGTCT".getBytes(), false, forwardArray, reverseArray);
		
		//expecting to see 2 G>T
		
		assertEquals(2, forwardArray.get(SummaryReportUtils.getIntFromChars('G', 'T')));
	}
	
	@Test
	public void tallyMDMismatchesNastyCigar() {
		//md: 16A1T6A2^G5A6T1A0G2C4T0G2^A3^GA5T2T3T8 , cigar: 28M1D4M1I23M1D3M2D3M2I18M18S, 
		//seq: AGTCTAGAGT CCAAAAGGAA TTCTTCCTCC TG*C*CTTTTCAT CCCTTTTTTT CACATCTTTC A*CC*TCCGCCGGG CCAATTTCT>TCAGTTCT CGTTTTAAGC, reverse strand: false
		QCMGAtomicLongArray forwardArray = new QCMGAtomicLongArray(32);
		QCMGAtomicLongArray reverseArray = new QCMGAtomicLongArray(32);
		SummaryByCycleNew2<Character> summary = new SummaryByCycleNew2<Character>(Character.MAX_VALUE, 64);
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(28, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.D));
		cigar.add(new CigarElement(4, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.I));
		cigar.add(new CigarElement(23, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.D));
		cigar.add(new CigarElement(3, CigarOperator.M));
		cigar.add(new CigarElement(2, CigarOperator.D));
		cigar.add(new CigarElement(3, CigarOperator.M));
		cigar.add(new CigarElement(2, CigarOperator.I));
		cigar.add(new CigarElement(18, CigarOperator.M));
		cigar.add(new CigarElement(18, CigarOperator.S));
		
		SummaryReportUtils.tallyMDMismatches("16A1T6A2^G5A6T1A0G2C4T0G2^A3^GA5T2T3T8", cigar, summary, "AGTCTAGAGTCCAAAAGGAATTCTTCCTCCTGCCTTTTCATCCCTTTTTTTCACATCTTTCACCTCCGCCGGGCCAATTTCTTCAGTTCTCGTTTTAAGC".getBytes(), false, forwardArray, reverseArray);
		
		//expecting to see A>G, T>A, 2x A>C,...... 
		
		assertEquals(1, forwardArray.get(SummaryReportUtils.getIntFromChars('A', 'G')));
		assertEquals(2, forwardArray.get(SummaryReportUtils.getIntFromChars('A', 'C')));
		assertEquals(1, forwardArray.get(SummaryReportUtils.getIntFromChars('A', 'T')));
		assertEquals(1, forwardArray.get(SummaryReportUtils.getIntFromChars('C', 'T')));
		assertEquals(1, forwardArray.get(SummaryReportUtils.getIntFromChars('G', 'T')));
		assertEquals(1, forwardArray.get(SummaryReportUtils.getIntFromChars('G', 'C')));
		assertEquals(4, forwardArray.get(SummaryReportUtils.getIntFromChars('T', 'C')));
		assertEquals(2, forwardArray.get(SummaryReportUtils.getIntFromChars('T', 'A')));
	}
	
	@Test
	public void testTallyMDMismatchesSecondaryAlignment() {
		SummaryByCycleNew2<Character> summary = new SummaryByCycleNew2<Character>(Character.MAX_VALUE, 64);
		QCMGAtomicLongArray forwardArray = new QCMGAtomicLongArray(32);
		QCMGAtomicLongArray reverseArray = new QCMGAtomicLongArray(32);
		SAMRecord sam = new SAMRecord(null);
		sam.setReadString("*");
		SummaryReportUtils.tallyMDMismatches("0N0N0N0T0A0A1C0C0T0A0A0C0C0C0T0A0A0C0C0C0T0A0A0C1C0T0A0A0C1C0T0A1C0C0C1A0A0C0C0C1A0A0C0C0C0T0A0A0C0C45", summary, sam.getReadBases(), false, forwardArray, reverseArray);
		
		Assert.assertEquals(0, summary.cycles().size());
		for (int i = 0 ; i < forwardArray.length() ; i++) {
			Assert.assertEquals(0,  forwardArray.get(i));
		}
		for (int i = 0 ; i < reverseArray.length() ; i++) {
			Assert.assertEquals(0,  reverseArray.get(i));
		}
		
		SummaryReportUtils.tallyMDMismatches("0C0C0C0T0A0A1C0C0T0A0A0C0C0C0T0A0A0C0C0C0T0A0A0C1C0T0A0A0C1C0T0A1C0C0C1A0A0C0C0C1A0A0C0C0C0T0A0A0C0C8A0A0C0C0C0T0A1C0C0C1A0A0C1C0T0A1C0C0", summary, sam.getReadBases(), false, forwardArray, reverseArray);
		
		Assert.assertEquals(0, summary.cycles().size());
		for (int i = 0 ; i < forwardArray.length() ; i++) {
			Assert.assertEquals(0,  forwardArray.get(i));
		}
		for (int i = 0 ; i < reverseArray.length() ; i++) {
			Assert.assertEquals(0,  reverseArray.get(i));
		}
	}
	
	@Test
	public void testTallyMDMismatchesWithStrand() {
		SummaryByCycleNew2<Character> summary = new SummaryByCycleNew2<Character>(Character.MAX_VALUE, 64);
		QCMGAtomicLongArray mdRefAltLengthsForward = new QCMGAtomicLongArray(32);
		QCMGAtomicLongArray mdRefAltLengthsReverse = new QCMGAtomicLongArray(32);
		String mdString = "51";
		final String readBasesString = "AAAAAAAAAACCCCCCCCCCGGGGGGGGGGTTTTTTTTTTAAAAAAAAAAT";
		final byte[] readBases = readBasesString.getBytes();
		final int readLength = readBases.length;
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, true, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertTrue(summary.cycles().isEmpty());
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, false, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertTrue(summary.cycles().isEmpty());
		
		mdString = "20A30";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, true, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertFalse(summary.cycles().isEmpty());
		Assert.assertEquals(1, summary.count(31, 'C').intValue());
		Assert.assertEquals(1, mdRefAltLengthsReverse.get(SummaryReportUtils.getIntFromChars('T', 'C')));
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, false, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertFalse(summary.cycles().isEmpty());
		Assert.assertEquals(1, summary.count(21, 'G').intValue());
		Assert.assertEquals(1, mdRefAltLengthsForward.get(SummaryReportUtils.getIntFromChars('A', 'G')));
		
		// update tests...
		mdString = "14C35";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, false, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(15, 'C').intValue());
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, true, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(readLength - 15 + 1, 'G').intValue());
		mdString = "0N49";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, false, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(1, 'A').intValue());
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, true, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(readLength - 1 + 1, 'T').intValue());
		mdString = "21G28";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, false, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(22, 'G').intValue());
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, true, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(readLength - 22 + 1, 'C').intValue());
		mdString = "13T0G32";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, false, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(14, 'C').intValue());
		Assert.assertEquals(2, summary.count(15, 'C').intValue());
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, true, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(readLength - 14 + 1, 'G').intValue());
		Assert.assertEquals(2, summary.count(readLength - 15 + 1, 'G').intValue());
		mdString = "5A0G43";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, false, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(6, 'A').intValue());
		Assert.assertEquals(1, summary.count(7, 'A').intValue());
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, true, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(readLength - 6 + 1, 'T').intValue());
		Assert.assertEquals(1, summary.count(readLength - 7 + 1, 'T').intValue());
		mdString = "16G2G30";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, false, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(17, 'C').intValue());
		Assert.assertEquals(1, summary.count(20, 'C').intValue());
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, true, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(readLength - 17 + 1, 'G').intValue());
		Assert.assertEquals(1, summary.count(readLength - 20 + 1, 'G').intValue());
		mdString = "20^G5";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, false, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(21, 'G').intValue());	// won't have increased the count - deletion
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, true, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(readLength - 21 + 1, 'C').intValue());	// won't have increased the count - deletion
		mdString = "17^TTCCAGCTG7A0";	// CIGAR: 17M9D8M, seq: AGAGTGAGAATCTGTTGATGACTCN
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, false, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(25, 'G').intValue());
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, true, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(readLength - 25 + 1, 'C').intValue());
		mdString = "17^TTT7T0";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, false, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(2, summary.count(25, 'G').intValue());
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, true, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(2, summary.count(readLength - 25 + 1, 'C').intValue());
		mdString = "3^GTTGAC22";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, false, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(21, 'G').intValue());	// won't have increased the count - deletion
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, true, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(readLength - 21 + 1, 'C').intValue());	// won't have increased the count - deletion
		mdString = "3A0T2^CCTGGATCAA18";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, false, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(4, 'A').intValue());
		Assert.assertEquals(1, summary.count(5, 'A').intValue());
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, true, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(readLength - 4 + 1, 'T').intValue());
		Assert.assertEquals(1, summary.count(readLength - 5 + 1, 'T').intValue());
		mdString = "39A6^TGCTGTGGCC4T0";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, false, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(40, 'T').intValue());
		Assert.assertEquals(2, summary.count(51, 'T').intValue());
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, true, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(readLength - 40 + 1, 'A').intValue());
		Assert.assertEquals(2, summary.count(readLength - 51 + 1, 'A').intValue());
		
		// and finally where the variant is the first character....
		mdString = "TC23";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, false, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(3, summary.count(1, 'A').intValue());
		Assert.assertEquals(1, summary.count(2, 'A').intValue());
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, true, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(3, summary.count(readLength - 1 + 1, 'T').intValue());
		Assert.assertEquals(1, summary.count(readLength - 2 + 1, 'T').intValue());
		
		mdString = "T24G";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, false, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(4, summary.count(1, 'A').intValue());
		Assert.assertEquals(1, summary.count(26, 'G').intValue());
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, true, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(4, summary.count(readLength - 1 + 1, 'T').intValue());
		Assert.assertEquals(1, summary.count(readLength - 26 + 1, 'C').intValue());
		
		//same again with 0 in front of the variation
		mdString = "0T0C23";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, false, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(5, summary.count(1, 'A').intValue());
		Assert.assertEquals(2, summary.count(2, 'A').intValue());
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, true, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(5, summary.count(readLength - 1 + 1, 'T').intValue());
		Assert.assertEquals(2, summary.count(readLength - 2 + 1, 'T').intValue());
		
		mdString = "0T24G";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, false, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(6, summary.count(1, 'A').intValue());
		Assert.assertEquals(2, summary.count(26, 'G').intValue());
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, true, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(6, summary.count(readLength - 1 + 1, 'T').intValue());
		Assert.assertEquals(2, summary.count(readLength - 26 + 1, 'C').intValue());
		
		
		// extra long mds.... taken from real life bam file
		mdString = "30^AGAAAATGTTTTTCATTTTCTTGATTTATTTCTGAATTCAGCTTGCTCTTCATTAGCGCTACATAGCTGMCTTATTATTCGTGGTC" +
		"CCCTATGACCCCCTGATCATTTTCCCTGAGGGTGCATATTTATTCACTAACTATGTTACAATCATGTGATCTGCTGGATTTTTTCTGATA" +
		"GTCTACTCTAGATTTGTTCTAAATTAATAAATCCCATTATTTTTGGCTTCTACTACTTCTATTTATTAAATTCATTCTGAATATGAAGTTTATTT" +
		"TCAAAGGAATTCATAATTCTTTACTCCRRGCTTGGTTCTAACAATGAATTTAATAAGAATTGTATTTAATCAATGTTTAAATATATTAAGGGC" +
		"AAATTTTGTAAAAATGTTAGTGTTCCAAGCTTTCCATTTCCCCACAAATTAATTTTTTTAGCCTTTCCCCTTAATCCACTTTCTT19G0";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, false, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(50, 'A').intValue());
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, true, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(readLength - 50 + 1, 'T').intValue());
		
		
		
		mdString = "17C2^CCACTTAATTTCATGTGATAATTTTCCCCAATGACTAACCAAATATGCTTCACTATTATATAAATCAATTCTTTCTTAATGCC" +
		"ACAAGTGAAAGTGCAAAGGTAGCTAATGGTTTTCTTCTCATAAAAATCACACTTTGGCTTTTTCCTTTCATATGTAATTAATCATATT" +
		"TGTGACAATCTTCCAAACTTACTTGAAATTTTTCTGAATCCCTTTCAAATCAGGACAAGAACTAGAAATGTCTATACAGGTTTAATAT" +
		"GAAGTAAAGAAAATGTTTTTCATTTTCTTGATTTATTTCTGAATTCAGCTTGCTCTTCATTAGCGCTACATAGCTGMCTTATTATTCG" +
		"TGGTCCCCTATGACCCCCTGATCATTTTCCCTGAGGGTGCATATTTATTCACTAACTATGTTACAATCATGTGATCTGCTGGATTTT" +
		"TTCTGATAGTCTACTCTAGATTTGTTCTAAATTAATAAATCCCATTATTTTTG30";
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, false, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(18, 'C').intValue());
		SummaryReportUtils.tallyMDMismatches(mdString, summary, readBases, true, mdRefAltLengthsForward, mdRefAltLengthsReverse);
		Assert.assertEquals(1, summary.count(readLength - 18 + 1, 'G').intValue());
		
	}
	
	@Test
	public void testTallyBadMDReads() {
		ConcurrentMap<Integer, AtomicLong> badMDReadCount = new ConcurrentHashMap<Integer, AtomicLong>();
		
		// empty string
		String badRead = "";
		SummaryReportUtils.tallyBadReads(badRead, badMDReadCount, SummaryReportUtils.BAD_MD_PATTERN);
		Assert.assertNotNull(badMDReadCount);
		Assert.assertEquals(1, badMDReadCount.get(Integer.valueOf(0)).get());
		
		badRead = "1234";
		SummaryReportUtils.tallyBadReads(badRead, badMDReadCount, SummaryReportUtils.BAD_MD_PATTERN);
		Assert.assertEquals((2), badMDReadCount.get(Integer.valueOf(0)).get());
		
		badRead = "1A34";
		SummaryReportUtils.tallyBadReads(badRead, badMDReadCount, SummaryReportUtils.BAD_MD_PATTERN);
		Assert.assertEquals(2, badMDReadCount.get(Integer.valueOf(0)).get());
		Assert.assertEquals(1, badMDReadCount.get(Integer.valueOf(1)).get());
		
		badRead = "C234";
		SummaryReportUtils.tallyBadReads(badRead, badMDReadCount, SummaryReportUtils.BAD_MD_PATTERN);
		Assert.assertEquals((2), badMDReadCount.get(Integer.valueOf(0)).get());
		Assert.assertEquals((2), badMDReadCount.get(Integer.valueOf(1)).get());
		
		badRead = "T23N";
		SummaryReportUtils.tallyBadReads(badRead, badMDReadCount, SummaryReportUtils.BAD_MD_PATTERN);
		Assert.assertEquals((1), badMDReadCount.get(Integer.valueOf(2)).get());
		
		badRead = "N23G";
		SummaryReportUtils.tallyBadReads(badRead, badMDReadCount, SummaryReportUtils.BAD_MD_PATTERN);
		Assert.assertEquals((2), badMDReadCount.get(Integer.valueOf(2)).get());
		
		badRead = "NATGNANNNC";
		SummaryReportUtils.tallyBadReads(badRead, badMDReadCount, SummaryReportUtils.BAD_MD_PATTERN);
		Assert.assertEquals((1), badMDReadCount.get(Integer.valueOf(10)).get());
		
		badRead = "NT";
		SummaryReportUtils.tallyBadReads(badRead, badMDReadCount, SummaryReportUtils.BAD_MD_PATTERN);
		Assert.assertEquals((3), badMDReadCount.get(Integer.valueOf(2)).get());
		
		badRead = "N";
		SummaryReportUtils.tallyBadReads(badRead, badMDReadCount, SummaryReportUtils.BAD_MD_PATTERN);
		Assert.assertEquals((3), badMDReadCount.get(Integer.valueOf(1)).get());
		
		
		badRead = "ACGT1NNNN1TCGA1";
		SummaryReportUtils.tallyBadReads(badRead, badMDReadCount, SummaryReportUtils.BAD_MD_PATTERN);
		Assert.assertEquals((1), badMDReadCount.get(Integer.valueOf(12)).get());
		
		// null string
		SummaryReportUtils.tallyBadReads(null, badMDReadCount, SummaryReportUtils.BAD_MD_PATTERN);
		
		// null map
		SummaryReportUtils.tallyBadReads(null, null, SummaryReportUtils.BAD_MD_PATTERN);
		
		// null map
		try {
			SummaryReportUtils.tallyBadReads("anything in here", null, SummaryReportUtils.BAD_MD_PATTERN);
			Assert.fail("Should have thrown an AssertionError");
		} catch (AssertionError ae) {
			Assert.assertTrue(ae.getMessage().startsWith("Null map "));
		}
	}
	
	@Test
	public void testTallyBadMDReadsNEW() {
		ConcurrentMap<String, AtomicLong> badMDReadCount = new ConcurrentHashMap<String, AtomicLong>();
		
		// empty string
		String badRead = "";
		SummaryReportUtils.tallyBadReadsMD(badRead, badMDReadCount);
		Assert.assertNotNull(badMDReadCount);
		Assert.assertTrue(badMDReadCount.isEmpty());
		
		badRead = "1234";
		SummaryReportUtils.tallyBadReadsMD(badRead, badMDReadCount);
		Assert.assertTrue(badMDReadCount.isEmpty());
		
		badRead = "1A34";
		SummaryReportUtils.tallyBadReadsMD(badRead, badMDReadCount);
		Assert.assertEquals(1, badMDReadCount.get("1M").get());
		
		badRead = "C234";
		SummaryReportUtils.tallyBadReadsMD(badRead, badMDReadCount);
		Assert.assertEquals(2, badMDReadCount.get("1M").get());
		
		badRead = "T23N";
		SummaryReportUtils.tallyBadReadsMD(badRead, badMDReadCount);
		Assert.assertEquals((4), badMDReadCount.get("1M").get());
		
		badRead = "N23G";
		SummaryReportUtils.tallyBadReadsMD(badRead, badMDReadCount);
		Assert.assertEquals((6), badMDReadCount.get("1M").get());
		
		badRead = "NATGNANNNC";
		SummaryReportUtils.tallyBadReadsMD(badRead, badMDReadCount);
		Assert.assertEquals((1), badMDReadCount.get("10M").get());
		
		badRead = "NT";
		SummaryReportUtils.tallyBadReadsMD(badRead, badMDReadCount);
		Assert.assertEquals((1), badMDReadCount.get("2M").get());
		
		badRead = "N";
		SummaryReportUtils.tallyBadReadsMD(badRead, badMDReadCount);
		Assert.assertEquals(7, badMDReadCount.get("1M").get());
		
		
		badRead = "ACGT1NNNN1TCGA1";
		SummaryReportUtils.tallyBadReadsMD(badRead, badMDReadCount);
		Assert.assertEquals((3), badMDReadCount.get("4M").get());
		
		
		//re-set collection
		badMDReadCount = new ConcurrentHashMap<String, AtomicLong>();
		badRead = "10AC^G5";
		SummaryReportUtils.tallyBadReadsMD(badRead, badMDReadCount);
		Assert.assertEquals((1), badMDReadCount.get("2M").get());
		Assert.assertEquals((1), badMDReadCount.get("1D").get());
		
		//re-set collection
		badMDReadCount = new ConcurrentHashMap<String, AtomicLong>();
		badRead = "^ACGNTACGNT10AC^G5ACGNT";
		SummaryReportUtils.tallyBadReadsMD(badRead, badMDReadCount);
		Assert.assertEquals((1), badMDReadCount.get("2M").get());
		Assert.assertEquals((1), badMDReadCount.get("5M").get());
		Assert.assertEquals((1), badMDReadCount.get("1D").get());
		Assert.assertEquals((1), badMDReadCount.get("10D").get());
		
		//re-set collection
		badMDReadCount = new ConcurrentHashMap<String, AtomicLong>();
		badRead = "^^^^";
		SummaryReportUtils.tallyBadReadsMD(badRead, badMDReadCount);
		Assert.assertTrue(badMDReadCount.isEmpty());
		
		
		// null string
		SummaryReportUtils.tallyBadReadsMD(null, badMDReadCount);
		
		// null map
		SummaryReportUtils.tallyBadReadsMD(null, null);
		
		// null map
		try {
			SummaryReportUtils.tallyBadReadsMD("AAA", null);
			Assert.fail("Should have thrown an AssertionError");
		} catch (AssertionError ae) {
			Assert.assertTrue(ae.getMessage().startsWith("Null map "));
		}
	}
	
	@Test
	public void testTallyQualScoresInvalid() {
		ConcurrentMap<Integer, AtomicLong> badQualCount = new ConcurrentHashMap<Integer, AtomicLong>();
		
		// null string and null seperator
		SummaryReportUtils.tallyQualScores(null, badQualCount, null);
		Assert.assertTrue(badQualCount.isEmpty());
		
		// empty string and null seperator
		String badQual = "";
		SummaryReportUtils.tallyQualScores(badQual, badQualCount, null);
		Assert.assertTrue(badQualCount.isEmpty());
		
		// empty string
		SummaryReportUtils.tallyQualScores(badQual, badQualCount, "");
		Assert.assertFalse(badQualCount.isEmpty());
		Assert.assertEquals(1, badQualCount.get(Integer.valueOf(0)).get());
		
		// valid string, but incorrect separator
		badQual = "1,1,1,1,1";
		try {
			SummaryReportUtils.tallyQualScores(badQual, badQualCount, "");
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().startsWith("For input string"));
		}
	}
	
	@Test
	public void testAddPositionAndLengthToMap() {
		ConcurrentMap<Integer, AtomicLong> map = new ConcurrentHashMap<Integer, AtomicLong>();
		SummaryReportUtils.addPositionAndLengthToMap(map, 10, 100);
		
		Assert.assertEquals(100, map.size());
		Assert.assertNull(map.get(0));
		Assert.assertNull(map.get(9));
		Assert.assertNull(map.get(110));
		Assert.assertEquals(1, map.get(10).get());
		Assert.assertEquals(1, map.get(109).get());
		
		
		SummaryReportUtils.addPositionAndLengthToMap(map, 100, 50);
		Assert.assertEquals(140, map.size());
		Assert.assertNull(map.get(0));
		Assert.assertNull(map.get(9));
		Assert.assertNull(map.get(150));
		Assert.assertEquals(1, map.get(10).get());
		Assert.assertEquals(2, map.get(109).get());
		

		// adding 0 positions and size - should not affect anything...
		SummaryReportUtils.addPositionAndLengthToMap(map, 0, 0);
		Assert.assertEquals(140, map.size());
		Assert.assertNull(map.get(0));
		Assert.assertNull(map.get(9));
		Assert.assertNull(map.get(150));
		Assert.assertEquals(1, map.get(10).get());
		Assert.assertEquals(2, map.get(109).get());
		
		SummaryReportUtils.addPositionAndLengthToMap(map, 100, 10);
		Assert.assertEquals(140, map.size());
		Assert.assertNull(map.get(0));
		Assert.assertNull(map.get(9));
		Assert.assertNull(map.get(150));
		Assert.assertEquals(1, map.get(10).get());
		Assert.assertEquals(3, map.get(109).get());
		
		
		SummaryReportUtils.addPositionAndLengthToMap(map, 10000, 2);
		Assert.assertEquals(142, map.size());
		Assert.assertNull(map.get(0));
		Assert.assertNull(map.get(9));
		Assert.assertNull(map.get(150));
		Assert.assertNull(map.get(10002));
		Assert.assertEquals(1, map.get(10).get());
		Assert.assertEquals(3, map.get(109).get());
		Assert.assertEquals(1, map.get(10000).get());
		Assert.assertEquals(1, map.get(10001).get());
		
	}
	
	@Test
	public void testTallyQualScoresValid() {
		ConcurrentMap<Integer, AtomicLong> badQualCount = new ConcurrentHashMap<Integer, AtomicLong>();
		
		// valid string, valid seperator
		String qual = "1,1,1,1,1";
		SummaryReportUtils.tallyQualScores(qual, badQualCount, ",");
		Assert.assertEquals((1), badQualCount.get(Integer.valueOf(5)).get());
		
		qual = "1,2,3,4,5";
		SummaryReportUtils.tallyQualScores(qual, badQualCount, ",");
		Assert.assertEquals((2), badQualCount.get(Integer.valueOf(5)).get());
		
		qual = "9,9,9,9,9";
		SummaryReportUtils.tallyQualScores(qual, badQualCount, ",");
		Assert.assertEquals((3), badQualCount.get(Integer.valueOf(5)).get());
		
		qual = "1,2,3,9,9,10,11,12,13,14,15";
		SummaryReportUtils.tallyQualScores(qual, badQualCount, ",");
		Assert.assertEquals((4), badQualCount.get(Integer.valueOf(5)).get());
		
		// all values over 10
		qual = "10,11,12,13,14,15";
		SummaryReportUtils.tallyQualScores(qual, badQualCount, ",");
		Assert.assertEquals((4), badQualCount.get(Integer.valueOf(5)).get());
		Assert.assertEquals((1), badQualCount.get(Integer.valueOf(0)).get());
		
	}
	
	@Test
	public void testLengthMapToXML() throws Exception {
		Element root = createElement("testLengthMapToXML");
		
		ConcurrentNavigableMap<Integer, AtomicLong> map = new ConcurrentSkipListMap<Integer, AtomicLong>();
		SummaryReportUtils.lengthMapToXml(root, "test", map);
		
		Assert.assertTrue(root.hasChildNodes());
		Assert.assertEquals(1, root.getChildNodes().getLength());
		Assert.assertEquals("test", root.getChildNodes().item(0).getNodeName());
		Assert.assertFalse(root.getChildNodes().item(0).hasChildNodes());
		Assert.assertFalse(root.getChildNodes().item(0).hasAttributes());
		
		
		// same again this time with some data!
		map.put(100, new AtomicLong(42));
		map.put(101, new AtomicLong(41));
		map.put(102, new AtomicLong(40));
		map.put(103, new AtomicLong(39));
		map.put(104, new AtomicLong(38));
		map.put(105, new AtomicLong(37));
		SummaryReportUtils.lengthMapToXml(root, "test42", map);
		
		Assert.assertTrue(root.hasChildNodes());
		Assert.assertEquals(2, root.getChildNodes().getLength());
		Assert.assertEquals("test42", root.getChildNodes().item(1).getNodeName());
		
		Element element42ValueTally = (Element) root.getChildNodes().item(1).getChildNodes().item(0);
//		System.out.println("element42ValueTally = " + element42ValueTally.getNodeName());
		Assert.assertTrue(element42ValueTally.hasChildNodes());
		Assert.assertEquals(6, element42ValueTally.getChildNodes().getLength());
		
		// first element
		Element element42TallyItem = (Element) element42ValueTally.getChildNodes().item(0);
		Assert.assertFalse(element42TallyItem.hasChildNodes());
		Assert.assertTrue(element42TallyItem.hasAttributes());
		Assert.assertEquals(100, Integer.parseInt(element42TallyItem.getAttribute("value")));
		Assert.assertEquals(42, Integer.parseInt(element42TallyItem.getAttribute("count")));
		
		// second element
		element42TallyItem = (Element) element42ValueTally.getChildNodes().item(1);
		Assert.assertFalse(element42TallyItem.hasChildNodes());
		Assert.assertTrue(element42TallyItem.hasAttributes());
		Assert.assertEquals(101, Integer.parseInt(element42TallyItem.getAttribute("value")));
		Assert.assertEquals(41, Integer.parseInt(element42TallyItem.getAttribute("count")));
		
		// last element
		element42TallyItem = (Element) element42ValueTally.getChildNodes().item(5);
		Assert.assertFalse(element42TallyItem.hasChildNodes());
		Assert.assertTrue(element42TallyItem.hasAttributes());
		Assert.assertEquals(105, Integer.parseInt(element42TallyItem.getAttribute("value")));
		Assert.assertEquals(37, Integer.parseInt(element42TallyItem.getAttribute("count")));
	}
	
	@Test
	public void testBinnedLengthMapToRangeTallyXML() throws Exception {
		Element root = createElement("testBinnedLengthMapToRangeTallyXML");
		
		ConcurrentNavigableMap<Integer, AtomicLong> map = new ConcurrentSkipListMap<Integer, AtomicLong>();
		SummaryReportUtils.binnedLengthMapToRangeTallyXml(root, map);
		
		Assert.assertTrue(root.hasChildNodes());
		Assert.assertEquals(1, root.getChildNodes().getLength());
		Assert.assertEquals("RangeTally", root.getChildNodes().item(0).getNodeName());
		Assert.assertFalse(root.getChildNodes().item(0).hasChildNodes());
		Assert.assertFalse(root.getChildNodes().item(0).hasAttributes());
		
		
		// same again this time with some data!
		map.put(100, new AtomicLong(42));
		map.put(110, new AtomicLong(41));
		map.put(120, new AtomicLong(40));
		map.put(130, new AtomicLong(39));
		map.put(140, new AtomicLong(38));
		map.put(150, new AtomicLong(37));
		SummaryReportUtils.binnedLengthMapToRangeTallyXml(root, map);
		
		Assert.assertTrue(root.hasChildNodes());
		Assert.assertEquals(2, root.getChildNodes().getLength());
		Assert.assertEquals("RangeTally", root.getChildNodes().item(1).getNodeName());
		
		Element element42RangeTally = (Element) root.getChildNodes().item(1);
//		System.out.println("element42RangeTally = " + element42RangeTally.getNodeName());
		Assert.assertTrue(element42RangeTally.hasChildNodes());
		Assert.assertEquals(6, element42RangeTally.getChildNodes().getLength());
		
		// first element
		Element element42RangeTallyItem = (Element) element42RangeTally.getChildNodes().item(0);
		Assert.assertFalse(element42RangeTallyItem.hasChildNodes());
		Assert.assertTrue(element42RangeTallyItem.hasAttributes());
		Assert.assertEquals(100, Integer.parseInt(element42RangeTallyItem.getAttribute("start")));
		Assert.assertEquals(100, Integer.parseInt(element42RangeTallyItem.getAttribute("end")));
		Assert.assertEquals(42, Integer.parseInt(element42RangeTallyItem.getAttribute("count")));
		
		// second element
		element42RangeTallyItem = (Element) element42RangeTally.getChildNodes().item(1);
		Assert.assertFalse(element42RangeTallyItem.hasChildNodes());
		Assert.assertTrue(element42RangeTallyItem.hasAttributes());
		Assert.assertEquals(110, Integer.parseInt(element42RangeTallyItem.getAttribute("start")));
		Assert.assertEquals(110, Integer.parseInt(element42RangeTallyItem.getAttribute("end")));
		Assert.assertEquals(41, Integer.parseInt(element42RangeTallyItem.getAttribute("count")));
		
		// third element
		element42RangeTallyItem = (Element) element42RangeTally.getChildNodes().item(2);
		Assert.assertFalse(element42RangeTallyItem.hasChildNodes());
		Assert.assertTrue(element42RangeTallyItem.hasAttributes());
		Assert.assertEquals(120, Integer.parseInt(element42RangeTallyItem.getAttribute("start")));
		Assert.assertEquals(120, Integer.parseInt(element42RangeTallyItem.getAttribute("end")));
		Assert.assertEquals(40, Integer.parseInt(element42RangeTallyItem.getAttribute("count")));
		
		// last element
		element42RangeTallyItem = (Element) element42RangeTally.getChildNodes().item(5);
		Assert.assertFalse(element42RangeTallyItem.hasChildNodes());
		Assert.assertTrue(element42RangeTallyItem.hasAttributes());
		Assert.assertEquals(150, Integer.parseInt(element42RangeTallyItem.getAttribute("start")));
		Assert.assertEquals(150, Integer.parseInt(element42RangeTallyItem.getAttribute("end")));
		Assert.assertEquals(37, Integer.parseInt(element42RangeTallyItem.getAttribute("count")));
	}
	
	
	@Test
	public void testPostionSummaryMapToXml() throws Exception {
		Element root = createElement("testPostionSummaryMapToXml");
		List<String> rgs = Arrays.asList(new String[] {"rg1", "rg2"});
		
		ConcurrentMap<String, PositionSummary> map = new ConcurrentHashMap<String, PositionSummary>();
		SummaryReportUtils.postionSummaryMapToXml(root, "test", map,  rgs );
		
		Assert.assertTrue(root.hasChildNodes());
		Assert.assertEquals(1, root.getChildNodes().getLength());
		Assert.assertEquals("test", root.getChildNodes().item(0).getNodeName());
		Assert.assertFalse(root.getChildNodes().item(0).hasChildNodes());
		Assert.assertFalse(root.getChildNodes().item(0).hasAttributes());
		
		PositionSummary ps = new PositionSummary( rgs );
		ps.addPosition(42, "rg1" );
		for (int i = 0 ; i <= 10000000 ; i++) { ps.addPosition(i,"rg1"); }
		map.put("chr1", ps);
 
		// same again this time with some data!		
		map.put("chr2", new PositionSummary(rgs)); map.get("chr2").addPosition(41, "rg1");
		map.put("chr3", new PositionSummary(rgs)); map.get("chr3").addPosition(40, "rg1"); 
		map.put("chr4", new PositionSummary(rgs)); map.get("chr4").addPosition(39, "rg1"); 
		map.put("chr5", new PositionSummary(rgs)); map.get("chr5").addPosition(38, "rg1"); 
		map.put("chr6", new PositionSummary(rgs)); map.get("chr6").addPosition(37, "rg1"); 		
		
		SummaryReportUtils.postionSummaryMapToXml(root, "test42", map , rgs );		
		Assert.assertTrue(root.hasChildNodes());
		Assert.assertEquals(2, root.getChildNodes().getLength());
		Assert.assertEquals("test42", root.getChildNodes().item(1).getNodeName());
		
		Element element42RName = (Element) root.getChildNodes().item(1).getChildNodes().item(0);
		Assert.assertTrue(element42RName.hasChildNodes());
		Assert.assertTrue(element42RName.hasAttributes());
		Assert.assertEquals("chr1", element42RName.getAttribute("value"));
		Assert.assertEquals(0, Integer.parseInt(element42RName.getAttribute("minPosition")));
		Assert.assertEquals(10000000, Integer.parseInt(element42RName.getAttribute("maxPosition")));
		Assert.assertEquals(10000002, Integer.parseInt(element42RName.getAttribute("count")));		
		Assert.assertEquals(1, element42RName.getChildNodes().getLength());

		// first element
		Element element42RangeTallyItem = (Element) element42RName.getChildNodes().item(0).getChildNodes().item(0);
		Assert.assertEquals(0, Integer.parseInt(element42RangeTallyItem.getAttribute("start")));
		Assert.assertEquals(999999, Integer.parseInt(element42RangeTallyItem.getAttribute("end")));
		Assert.assertEquals(1000001, Integer.parseInt(element42RangeTallyItem.getAttribute("count")) );
		Assert.assertEquals("1000001,0", element42RangeTallyItem.getAttribute("rgCount") );	
 		
		// second element
		element42RangeTallyItem = (Element) element42RName.getChildNodes().item(0).getChildNodes().item(1);
		Assert.assertEquals(1000000, Integer.parseInt(element42RangeTallyItem.getAttribute("start")));
		Assert.assertEquals(1999999, Integer.parseInt(element42RangeTallyItem.getAttribute("end")));
		Assert.assertEquals(1000000, Integer.parseInt(element42RangeTallyItem.getAttribute("count")) );
		Assert.assertEquals("1000000,0", element42RangeTallyItem.getAttribute("rgCount") );	
		
		// last element
		element42RangeTallyItem = (Element) element42RName.getChildNodes().item(0).getChildNodes().item(9);
		Assert.assertEquals(9000000, Integer.parseInt(element42RangeTallyItem.getAttribute("start")));
		Assert.assertEquals(9999999, Integer.parseInt(element42RangeTallyItem.getAttribute("end")));
		Assert.assertEquals(1000000, Integer.parseInt(element42RangeTallyItem.getAttribute("count")) );
		Assert.assertEquals("1000000,0", element42RangeTallyItem.getAttribute("rgCount") );	
		
		// next rname
		element42RName = (Element) root.getChildNodes().item(1).getChildNodes().item(1);
        Assert.assertTrue(element42RName.hasChildNodes());
        Assert.assertTrue(element42RName.hasAttributes());
        Assert.assertEquals("chr2", element42RName.getAttribute("value"));
        Assert.assertEquals(41, Integer.parseInt(element42RName.getAttribute("minPosition")));
        Assert.assertEquals(41, Integer.parseInt(element42RName.getAttribute("maxPosition")));
        Assert.assertEquals(1, Integer.parseInt(element42RName.getAttribute("count")));
        Assert.assertEquals(1, element42RName.getChildNodes().getLength());
	}

	private Element createElement(String methodName) throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		DOMImplementation domImpl = builder.getDOMImplementation();
		Document doc = domImpl.createDocument(null, "SummaryReportUtilsTest." + methodName, null);
		return doc.getDocumentElement();
	}
}
