package org.qcmg.picard.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import junit.framework.Assert;
import htsjdk.samtools.SAMUtils;

import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.model.PileupElementComparator;
import org.qcmg.common.model.Rule;
import org.qcmg.picard.util.PileupElementUtil;

public class PileupElementUtilTest {
	
	@Test
	public void testPassesVariantCountCheck() {
		Rule rule = new Rule(0, 1, 1);
		try {
			PileupElementUtil.passesCountCheck(1, 0, rule);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {}
		
		try {
			PileupElementUtil.passesCountCheck(0, 0, null);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {}
		
		try {
			PileupElementUtil.passesCountCheck(-10, -1, rule);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {}
		
		
		Assert.assertFalse(PileupElementUtil.passesCountCheck(0, 0, rule));
		Assert.assertTrue(PileupElementUtil.passesCountCheck(1, 1, rule));
		Assert.assertFalse(PileupElementUtil.passesCountCheck(15, 30, new Rule(0,50,16)));
		Assert.assertTrue(PileupElementUtil.passesCountCheck(16, 30, new Rule(0,50,16)));
		
		// percentage checks - when max is Integer.MAX_VALUE
		Assert.assertFalse(PileupElementUtil.passesCountCheck(4, 100, new Rule(0,Integer.MAX_VALUE, 5)));
		Assert.assertTrue(PileupElementUtil.passesCountCheck(5, 100, new Rule(0,Integer.MAX_VALUE, 5)));
		Assert.assertTrue(PileupElementUtil.passesCountCheck(6, 100, new Rule(0,Integer.MAX_VALUE, 5)));
		Assert.assertFalse(PileupElementUtil.passesCountCheck(5, 100, new Rule(0,Integer.MAX_VALUE, 6)));
		Assert.assertTrue(PileupElementUtil.passesCountCheck(6, 100, new Rule(0,Integer.MAX_VALUE, 6)));
		Assert.assertTrue(PileupElementUtil.passesCountCheck(7, 100, new Rule(0,Integer.MAX_VALUE, 6)));
		Assert.assertTrue(PileupElementUtil.passesCountCheck(100, 100, new Rule(0,Integer.MAX_VALUE, 6)));
		
		Assert.assertTrue(PileupElementUtil.passesCountCheck(6, 200, new Rule(0,Integer.MAX_VALUE-1, 6)));
	}
	
	@Test
	public void testPassesVariantCountCheckSecondPass() {
		Rule rule = new Rule(0, 1, 1);
		try {
			PileupElementUtil.passesCountCheck(1, 0, rule, true);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {}
		
		try {
			PileupElementUtil.passesCountCheck(0, 0, null, true);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {}
		
		try {
			PileupElementUtil.passesCountCheck(-10, -1, rule, true);
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {}
		
		
		Assert.assertFalse(PileupElementUtil.passesCountCheck(0, 0, rule, true));
		Assert.assertTrue(PileupElementUtil.passesCountCheck(1, 1, rule, true));
		Assert.assertFalse(PileupElementUtil.passesCountCheck(15, 30, new Rule(0,50,16), true));
		Assert.assertTrue(PileupElementUtil.passesCountCheck(16, 30, new Rule(0,50,16), true));
		
		// percentage checks - when max is Integer.MAX_VALUE
		// percentage/2 is used as we are in second pass land
		Assert.assertFalse(PileupElementUtil.passesCountCheck(2, 100, new Rule(0,Integer.MAX_VALUE, 5), true));
		Assert.assertTrue(PileupElementUtil.passesCountCheck(3, 100, new Rule(0,Integer.MAX_VALUE, 5), true));
		Assert.assertTrue(PileupElementUtil.passesCountCheck(4, 100, new Rule(0,Integer.MAX_VALUE, 5), true));
		Assert.assertTrue(PileupElementUtil.passesCountCheck(5, 100, new Rule(0,Integer.MAX_VALUE, 5), true));
		Assert.assertTrue(PileupElementUtil.passesCountCheck(6, 100, new Rule(0,Integer.MAX_VALUE, 5), true));
		Assert.assertFalse(PileupElementUtil.passesCountCheck(1, 100, new Rule(0,Integer.MAX_VALUE, 6), true));
		Assert.assertFalse(PileupElementUtil.passesCountCheck(2, 100, new Rule(0,Integer.MAX_VALUE, 6), true));
		Assert.assertTrue(PileupElementUtil.passesCountCheck(6, 100, new Rule(0,Integer.MAX_VALUE, 6), true));
		Assert.assertTrue(PileupElementUtil.passesCountCheck(7, 100, new Rule(0,Integer.MAX_VALUE, 6), true));
		Assert.assertTrue(PileupElementUtil.passesCountCheck(100, 100, new Rule(0,Integer.MAX_VALUE, 6), true));
		
		Assert.assertTrue(PileupElementUtil.passesCountCheck(6, 200, new Rule(0,Integer.MAX_VALUE-1, 6), true));
	}
	
	@Test
	public void testGetPileupCounts() {
		String testBaseString = null;
		List<PileupElement> counts = PileupElementUtil.getPileupCounts(testBaseString);
		Assert.assertNull(counts);
		
		testBaseString = "";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		Assert.assertNull(counts);
		
		testBaseString = "*";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		Assert.assertNull(counts);
		
		testBaseString = "****";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		Assert.assertNotNull(counts);
		Assert.assertTrue(counts.isEmpty());
		
		testBaseString = "........,,,,,,,,^:..$.....,,,";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		int maxBaseCount = counts.get(0).getTotalCount();
		int maxVariantCount = PileupElementUtil.getLargestVariantCount(counts);
		Assert.assertEquals(26, maxBaseCount);
		Assert.assertEquals(0, maxVariantCount);
		
		testBaseString = "^^^";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		Assert.assertTrue(counts.isEmpty());
		
		testBaseString = "^a^c^g";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		Assert.assertTrue(counts.isEmpty());
		
		testBaseString = ".^a.^c.^g..,.,.,";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		maxBaseCount = counts.get(0).getTotalCount();
		maxVariantCount = PileupElementUtil.getLargestVariantCount(counts);
		Assert.assertEquals(10, maxBaseCount);
		Assert.assertEquals(0, maxVariantCount);
		
		testBaseString = ".^aa.^cc.^gg..,.,.,";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		maxBaseCount = counts.get(0).getTotalCount();
		maxVariantCount = PileupElementUtil.getLargestVariantCount(counts);
		Assert.assertEquals(10, maxBaseCount);
		Assert.assertEquals(1, maxVariantCount);
		
		testBaseString = "t";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		maxBaseCount = counts.get(0).getTotalCount();
		maxVariantCount = PileupElementUtil.getLargestVariantCount(counts);
		Assert.assertEquals(1, maxBaseCount);
		Assert.assertEquals(1, maxVariantCount);
		
		testBaseString = "tTcCgGaA";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		maxBaseCount = counts.get(0).getTotalCount();
		maxVariantCount = PileupElementUtil.getLargestVariantCount(counts);
		Assert.assertEquals(2, maxBaseCount);
		Assert.assertEquals(2, maxVariantCount);
		
		testBaseString = "tTxXxxXXXX";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		maxBaseCount = counts.get(0).getTotalCount();
		maxVariantCount = PileupElementUtil.getLargestVariantCount(counts);
		Assert.assertEquals(2, maxBaseCount);
		Assert.assertEquals(2, maxVariantCount);
		
		testBaseString = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa-5GGggGaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		maxBaseCount = counts.get(0).getTotalCount();
		maxVariantCount = PileupElementUtil.getLargestVariantCount(counts);
		Assert.assertEquals(80, maxBaseCount);
		Assert.assertEquals(80, maxVariantCount);
		Assert.assertEquals(80, maxVariantCount);
		
		testBaseString = "TTTTT.....TTTTT,,TTTTT......-3ACG..........+2TT..............TTTTT,TTTTT,,,,,,,,,,,,,,";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		maxBaseCount = counts.get(0).getTotalCount();
		maxVariantCount = PileupElementUtil.getLargestVariantCount(counts);
		Assert.assertEquals(52, maxBaseCount);
		Assert.assertEquals(25, maxVariantCount);
		
		testBaseString = "TTTTT.....NNNNN,,AAAAA.......aaaaa......TTTTT........^T..^C.....CCCCC,ccccc,,,,,,,,,,,,,,";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		maxBaseCount = counts.get(0).getTotalCount();
		maxVariantCount = PileupElementUtil.getLargestVariantCount(counts);
		Assert.assertEquals(50, maxBaseCount);
		Assert.assertEquals(10, maxVariantCount);
	}
	@Test
	public void testGetPileupCountsIndels() {
		String testBaseString = null;
		List<PileupElement> counts = PileupElementUtil.getPileupCounts(testBaseString);
		
		testBaseString = "....+2AG....,,,,,,,,^:..$.....,,,";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		int maxBaseCount = counts.get(0).getTotalCount();
		int maxVariantCount = PileupElementUtil.getLargestVariantCount(counts);
		Assert.assertEquals(26, maxBaseCount);
		Assert.assertEquals(0, maxVariantCount);
		
		testBaseString = ".^a.^c.^g..,.,.+4ACGG,";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		maxBaseCount = counts.get(0).getTotalCount();
		maxVariantCount = PileupElementUtil.getLargestVariantCount(counts);
		Assert.assertEquals(10, maxBaseCount);
		Assert.assertEquals(0, maxVariantCount);
		
		testBaseString = ".+1G^aa.^cc.^gg..,.,.,";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		maxBaseCount = counts.get(0).getTotalCount();
		maxVariantCount = PileupElementUtil.getLargestVariantCount(counts);
		Assert.assertEquals(10, maxBaseCount);
		Assert.assertEquals(1, maxVariantCount);
		
		testBaseString = "tTcCgGaA";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		maxBaseCount = counts.get(0).getTotalCount();
		maxVariantCount = PileupElementUtil.getLargestVariantCount(counts);
		Assert.assertEquals(2, maxBaseCount);
		Assert.assertEquals(2, maxVariantCount);
		
		testBaseString = "tTxXxxXXXX";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		maxBaseCount = counts.get(0).getTotalCount();
		maxVariantCount = PileupElementUtil.getLargestVariantCount(counts);
		Assert.assertEquals(2, maxBaseCount);
		Assert.assertEquals(2, maxVariantCount);
		
		testBaseString = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		maxBaseCount = counts.get(0).getTotalCount();
		maxVariantCount = PileupElementUtil.getLargestVariantCount(counts);
		Assert.assertEquals(80, maxBaseCount);
		Assert.assertEquals(80, maxVariantCount);
		Assert.assertEquals(80, maxVariantCount);
		
		testBaseString = "TTTTT.....TTTTT,,TTTTT..............................TTTTT,TTTTT,,,,,,,,,,,,,,";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		maxBaseCount = counts.get(0).getTotalCount();
		maxVariantCount = PileupElementUtil.getLargestVariantCount(counts);
		Assert.assertEquals(52, maxBaseCount);
		Assert.assertEquals(25, maxVariantCount);
		
		testBaseString = "TTTTT.....NNNNN,,AAAAA.......aaaaa......TTTTT........^T..^C.....CCCCC,ccccc,,,,,,,,,,,,,,";
		counts = PileupElementUtil.getPileupCounts(testBaseString);
		maxBaseCount = counts.get(0).getTotalCount();
		maxVariantCount = PileupElementUtil.getLargestVariantCount(counts);
		Assert.assertEquals(50, maxBaseCount);
		Assert.assertEquals(10, maxVariantCount);
	}
	
	@Test
	public void testGetPileupCountsThatPassRule() {
		String pileupString = "";
		String baseQualities = "";
		Rule rule1 = new Rule(0, 20, 3);
		Rule rule2 = new Rule(50, Integer.MAX_VALUE, 5);
		double percentage = 10.0;
		List<PileupElement> origPileups = PileupElementUtil.getPileupCounts(pileupString, baseQualities);
		
		List<PileupElement> newPileups = PileupElementUtil.getPileupCountsThatPassRule(origPileups, rule1, false, percentage);
		Assert.assertNull(newPileups);
		
		pileupString = ".AcaTgtN*^Ag,";
		origPileups = PileupElementUtil.getPileupCounts(pileupString, baseQualities);
		newPileups = PileupElementUtil.getPileupCountsThatPassRule(origPileups, rule1, false, percentage);
		Assert.assertTrue(newPileups.isEmpty());
		
		pileupString = ".,.,";
		origPileups = PileupElementUtil.getPileupCounts(pileupString, "IIII");
		newPileups = PileupElementUtil.getPileupCountsThatPassRule(origPileups, rule1, false, percentage);
		Assert.assertEquals(1, newPileups.size());
		Assert.assertEquals(4, newPileups.get(0).getTotalCount());
		
		pileupString = ".....aa,,,Tcgtcgt";
		origPileups = PileupElementUtil.getPileupCounts(pileupString, "IIIIIIIIIIIIIIIII");
		newPileups = PileupElementUtil.getPileupCountsThatPassRule(origPileups, rule1, false, percentage);
		Assert.assertEquals(2, newPileups.size());
		Assert.assertEquals(8, newPileups.get(0).getTotalCount());
		Assert.assertEquals(3, newPileups.get(1).getTotalCount());
		
		pileupString = ".....aa,,,Tcgtcgt";
		origPileups = PileupElementUtil.getPileupCounts(pileupString, "IIIIIIIIIIIIIIIII");
		newPileups = PileupElementUtil.getPileupCountsThatPassRule(origPileups, rule1, false, percentage);
		Assert.assertEquals(2, newPileups.size());
		Assert.assertEquals(8, newPileups.get(0).getTotalCount());
		Assert.assertEquals(3, newPileups.get(1).getTotalCount());
		
		pileupString = ".................................................AAACCC";
		origPileups = PileupElementUtil.getPileupCounts(pileupString, "IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII");
		newPileups = PileupElementUtil.getPileupCountsThatPassRule(origPileups, rule2, false, percentage);
		Assert.assertEquals(1, newPileups.size());
		
		// reduce the qualities to pass the 2 variants 
		pileupString = ".................................................AAACCC";
		origPileups = PileupElementUtil.getPileupCounts(pileupString, "0000000000000000000000000000000000000000000000000IIIIII");
		newPileups = PileupElementUtil.getPileupCountsThatPassRule(origPileups, rule2, false, percentage);
		Assert.assertEquals(3, newPileups.size());
		
	}
	
	@Test
	public void testGetGenotype() {
		Rule rule1 = new Rule(0, 20, 3);
		double percentage = 10.0;
		List<PileupElement> origPileups = PileupElementUtil.getPileupCounts(".......,,.,.,.,.,......");
		Assert.assertEquals("A/A", PileupElementUtil.getGenotype(origPileups, 'A').getDisplayString());
		
		origPileups = PileupElementUtil.getPileupCounts(".......,,.,.,.,.,......");
		Assert.assertEquals("C/C", PileupElementUtil.getGenotype(origPileups, 'C').getDisplayString());
		
		origPileups = PileupElementUtil.getPileupCounts(".......,,.,.,.,.,......");
		Assert.assertNotSame("C/C", PileupElementUtil.getGenotype(origPileups, 'T').getDisplayString());
		
		origPileups = PileupElementUtil.getPileupCounts(".....A.,,.,.,.,.,......");
		Assert.assertEquals("A/C", PileupElementUtil.getGenotype(origPileups, 'C').getDisplayString());
		
		origPileups = PileupElementUtil.getPileupCounts(".....A.,,.,.,.,.,......", "IIIIIIIIIIIIIIIIIIIIIII");
		List<PileupElement> newPileups = PileupElementUtil.getPileupCountsThatPassRule(origPileups, rule1, false, percentage);
		Assert.assertEquals("C/C", PileupElementUtil.getGenotype(newPileups, 'C').getDisplayString());
		
		origPileups = PileupElementUtil.getPileupCounts(".....A.,,.,.,.,.,..aa...tt gg.", "ABCDEFGHIABCDEFGHIABCDEFGHIABC");
		newPileups = PileupElementUtil.getPileupCountsThatPassRule(origPileups, rule1, false, percentage);
		Assert.assertEquals("A/C", PileupElementUtil.getGenotype(newPileups, 'C').getDisplayString());
		
		// needs to use the reference as the 2nd allele
		origPileups = PileupElementUtil.getPileupCounts("AAAAAaaaaa...ttt", "ABCDEFGHIABCDEFGHIABC");
		newPileups = PileupElementUtil.getPileupCountsThatPassRule(origPileups, rule1, false, percentage);
		Assert.assertEquals("A/C", PileupElementUtil.getGenotype(newPileups, 'C').getDisplayString());
		
		// needs to use the reference as the 2nd allele
		origPileups = PileupElementUtil.getPileupCounts("AAAAAaaaaa...tttgGg", "ABCDEFGHIABCDEFGHIABC");
		newPileups = PileupElementUtil.getPileupCountsThatPassRule(origPileups, rule1, false, percentage);
		Assert.assertEquals("A/C", PileupElementUtil.getGenotype(newPileups, 'C').getDisplayString());
		
		// first and second most common alleles are non reference
		origPileups = PileupElementUtil.getPileupCounts("AAAAAAATTTTGGG...", "IIIIIIIIIIIIIIIII");
		newPileups = PileupElementUtil.getPileupCountsThatPassRule(origPileups, rule1, false, percentage);
		Assert.assertEquals("A/T", PileupElementUtil.getGenotype(newPileups, 'C').getDisplayString());
		
		// 2 alleles tied in second place - one is reference which wins
		origPileups = PileupElementUtil.getPileupCounts("AAAAAAATTTGGG....", "IIIIIIIIIIIIIIIII");
		newPileups = PileupElementUtil.getPileupCountsThatPassRule(origPileups, rule1, false, percentage);
		Assert.assertEquals("A/C", PileupElementUtil.getGenotype(newPileups, 'C').getDisplayString());
		
		// 2 alleles tied in second place - neither is reference, go on base quality
		origPileups = PileupElementUtil.getPileupCounts("AAAAAAATTTGGG..", "IIIIIIIIGIIIIII");
		newPileups = PileupElementUtil.getPileupCountsThatPassRule(origPileups, rule1, false, percentage);
		Assert.assertEquals("A/G", PileupElementUtil.getGenotype(newPileups, 'C').getDisplayString());
		
	}
	
	@Test
	public void testOrderPileupElementsByReferenceAndQual() {
		Comparator<PileupElement> countRefQualComparator = new PileupElementComparator();
		// 10 A's, 5 C's, 5 T's - reference is G - should return A/T as T quals are better
		String bases = "AAAAAAAAAACCCCCTTTTT";
		String qualities = "IIIIIIIIIIGGGGGIIIII";
		List<PileupElement> origPileups = PileupElementUtil.getPileupCounts(bases, qualities);
		
		Assert.assertTrue(origPileups.get(0).getBase() == 'A');
		Assert.assertTrue(origPileups.get(1).getBase() == 'C');
		Assert.assertTrue(origPileups.get(2).getBase() == 'T');
		
		Collections.sort(origPileups, countRefQualComparator);
		
		Assert.assertTrue(origPileups.get(0).getBase() == 'A');
		Assert.assertTrue(origPileups.get(1).getBase() == 'T');
		Assert.assertTrue(origPileups.get(2).getBase() == 'C');
		
		// 10 A's, 5 references, 5 G's 5T's, equal qual - should return A/ref
		bases = "AAAAAAAAAAGGGGGTTTTT.....";
		qualities = "IIIIIIIIIIIIIIIIIIIIIIIII";
		origPileups = PileupElementUtil.getPileupCounts(bases, qualities);
		Assert.assertTrue(origPileups.get(0).getBase() == 'A');
		Assert.assertTrue(origPileups.get(1).getBase() == 'G');
		Assert.assertTrue(origPileups.get(2).getBase() == 'T');
		Assert.assertTrue(origPileups.get(3).getBase() == '.');
		
		Collections.sort(origPileups, countRefQualComparator);
		
		Assert.assertTrue(origPileups.get(0).getBase() == 'A');
		Assert.assertTrue(origPileups.get(1).getBase() == '.');
		Assert.assertTrue(origPileups.get(2).getBase() == 'G');
		Assert.assertTrue(origPileups.get(3).getBase() == 'T');
		
		// equal numbers of all 4 so should return ref and highest qual
		//despite ref not having great quality
		bases = "AAAAAGGGGGTTTTT.....";
		qualities = "GIIIIIIIIIIIIBIAAAAA";
		origPileups = PileupElementUtil.getPileupCounts(bases, qualities);
		Assert.assertTrue(origPileups.get(0).getBase() == 'A');
		Assert.assertTrue(origPileups.get(1).getBase() == 'G');
		Assert.assertTrue(origPileups.get(2).getBase() == 'T');
		Assert.assertTrue(origPileups.get(3).getBase() == '.');
		
		Collections.sort(origPileups, countRefQualComparator);
		
		Assert.assertTrue(origPileups.get(0).getBase() == '.');
		Assert.assertTrue(origPileups.get(1).getBase() == 'G');
		Assert.assertTrue(origPileups.get(2).getBase() == 'A');
		Assert.assertTrue(origPileups.get(3).getBase() == 'T');
	}
	
	@Ignore
	public void testPhredConversion() {
		String asciiPhredQuality = "IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIGII/III7III<II";
		byte[] phredBytes = SAMUtils.fastqToPhred(asciiPhredQuality);
		for (byte b : phredBytes) {
			System.out.println("b: " + b);
		}
		
		asciiPhredQuality = "IIIIGII%;EFIIII%I&III+-HII*'";
		phredBytes = SAMUtils.fastqToPhred(asciiPhredQuality);
		for (byte b : phredBytes) {
			System.out.println("b: " + b);
		}
	}
	
	@Test
	public void testPileupCountsWithQuality() {
		String bases = ".A..$.$.$.$.$.........................................................^@.^U.^Q.^L.";
		String qualities = "I7\"%(9/6BH@1AII&*%I=III9&II:IIIIIIIIIIIIIIIIIIIIIIIIEIIIIII4IIIEI>BIH";
		
		Assert.assertFalse(bases.length() == qualities.length());	// due to the read start chars in bases string
		
		List<PileupElement> origPileups = PileupElementUtil.getPileupCounts(bases, qualities);
		
		int maxBaseCount = origPileups.get(0).getTotalCount();
		int maxVariantCount = PileupElementUtil.getLargestVariantCount(origPileups);
		Assert.assertEquals(68, maxBaseCount);
		Assert.assertEquals(1, maxVariantCount);
		Assert.assertEquals(68, origPileups.get(0).getQualities().size());
		Assert.assertEquals(1, origPileups.get(1).getQualities().size());
		
//		System.out.println("base qualities: " + origPileups.get(0).getQualitiesAsString());
	}
	
	@Test
	public void testPileupCountsWithQualityInsertion() {
		String bases = ",-1t";
		String qualities = "+";
		
		Assert.assertFalse(bases.length() == qualities.length());	// due to the read start chars in bases string
		
		List<PileupElement> origPileups = PileupElementUtil.getPileupCounts(bases, qualities);
		
		int maxBaseCount = origPileups.get(0).getTotalCount();
		int maxVariantCount = PileupElementUtil.getLargestVariantCount(origPileups);
		Assert.assertEquals(1, maxBaseCount);
		Assert.assertEquals(0, maxVariantCount);
		Assert.assertEquals(1, origPileups.get(0).getQualities().size());
		
		bases = ",+1g";
		qualities = "*";
		Assert.assertFalse(bases.length() == qualities.length());	// due to the read start chars in bases string
		
		origPileups = PileupElementUtil.getPileupCounts(bases, qualities);
		
		maxBaseCount = origPileups.get(0).getTotalCount();
		maxVariantCount = PileupElementUtil.getLargestVariantCount(origPileups);
		Assert.assertEquals(1, maxBaseCount);
		Assert.assertEquals(0, maxVariantCount);
		Assert.assertEquals(1, origPileups.get(0).getQualities().size());
		
		
		bases = ".+2AC.+3ACT..+2AC..................";
		qualities = "86(-91);4:4+?=9+>;;++3";
		Assert.assertFalse(bases.length() == qualities.length());	// due to the read start chars in bases string
		
		origPileups = PileupElementUtil.getPileupCounts(bases, qualities);
		
		maxBaseCount = origPileups.get(0).getTotalCount();
		maxVariantCount = PileupElementUtil.getLargestVariantCount(origPileups);
		Assert.assertEquals(22, maxBaseCount);
		Assert.assertEquals(0, maxVariantCount);
		Assert.assertEquals(22, origPileups.get(0).getQualities().size());
		
		
		
		bases = ",$,$......,...,,....,$.....$...................$..$.$.$.$..$.$..$.,$,$.,$.....,$..,$.,$,$,$,$..........,$.............*...,,,,,,,,,,,,,,,,,,,,,,,"+
",,,,,,,,,,,,,,,,$,,,,,,,,,,,.....,......$......,,.*....C,$,,,..................................,.,......,.,,,,.,,,,,...,.,.,,,....,..............,.....,......................,..,,...,..................,.,,........"+
".....,,.c.,,..,+11ccatgacagta,...........,,...,,,.,,.,..............,..............,,,.,,,,,,,,,,,,,,,,,,....,..,...............................*.................................................................,.,"+
"........,.........,..,.................*...................................,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,.,.,,.,,,,,..,,,,,,,,,,.....,...,,...,,,,,,,,,,,,,,,,.....*.....,,..,..*.."+
"...........C................,,,,,,,,,,,,,,,,,,,,.,,,,,,,,,,,,,.......C..........................................,.,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,.,.,....,,.,,,,.,,,,,,..,.,..,,..,,,........................"+
".................................,.,..............,...,..,,,,........,.....,,..........,,,,,..,.,,,,,,.,..,,,.,.,..,,,..,,..,.,..,,,,,.,,.,.,,,,,,,.,,.,,,*,...,,,.....,,...,,.....,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,"+
",,,,,,,,,,,,,,,,.......,...,,...,,,.........................................................,,,,,,,,,,,,,..,.,.,,,,.,,,,.,.,.,..,.,,,.,,........,.,,.,,.,.,,..,,..,,,,,,,,,,,,.......,,...,,.,..,,,,,,,,,,,,,,.,.,..,"+
".,*...,.,,,...,,,....,,,..,..,,,,,,,,,,,,.,....,..........,,,,,,,,,,,,,,,,,,,,,,,,....,.,,,..,..,,,,,.....,,....,........,,,.....,,,,,,,,,,,,......,....,...............................,.,.....,....,............,.."+
".,.....,..,,.........,.,.....,,.,.,,,,,..,,.,.......,,...,.,.,,.,,,..,.,,.,.,,,.,..,.,,,,.,,,..........,..,....,.,,,,,,,.,,,,,,,,,,,..,.,,,G^t,^~.^q.^C.^b,^b,^~.^e.^b.^~.^b,^~.^b,^],^~.^~.^P,^~.^w,^o,^~.^~.^~.^~.^"+
"~.^~.^~.^~.^:.";
//		bases = ",,......,...,,....,...................................,,.,.....,..,.,,,,..........,................,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,.....,............,,.....C,,,,..................................,.,......,.,,,,.,,,,,...,.,.,,,....,..............,.....,......................,..,,...,..................,.,,.............,,.c.,,..,,...........,,...,,,.,,.,..............,..............,,,.,,,,,,,,,,,,,,,,,,....,..,................................................................................................,.,........,.........,..,....................................................,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,.,.,,.,,,,,..,,,,,,,,,,.....,...,,...,,,,,,,,,,,,,,,,..........,,..,...............C................,,,,,,,,,,,,,,,,,,,,.,,,,,,,,,,,,,.......C..........................................,.,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,.,.,....,,.,,,,.,,,,,,..,.,..,,..,,,.........................................................,.,..............,...,..,,,,........,.....,,..........,,,,,..,.,,,,,,.,..,,,.,.,..,,,..,,..,.,..,,,,,.,,.,.,,,,,,,.,,.,,,,...,,,.....,,...,,.....,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,.......,...,,...,,,.........................................................,,,,,,,,,,,,,..,.,.,,,,.,,,,.,.,.,..,.,,,.,,........,.,,.,,.,.,,..,,..,,,,,,,,,,,,.......,,...,,.,..,,,,,,,,,,,,,,.,.,..,.,...,.,,,...,,,....,,,..,..,,,,,,,,,,,,.,....,..........,,,,,,,,,,,,,,,,,,,,,,,,....,.,,,..,..,,,,,.....,,....,........,,,.....,,,,,,,,,,,,......,....,...............................,.,.....,....,............,...,.....,..,,.........,.,.....,,.,.,,,,,..,,.,.......,,...,.,.,,.,,,..,.,,.,.,,,.,..,.,,,,.,,,..........,..,....,.,,,,,,,.,,,,,,,,,,,..,.,,,G,...,,....,.,,..,.,,.........";
		
		qualities = "@A6A964A?7C45?AA8A:9C5<+5724?<9?;0<C5CCC40=.@6D=CC+A+CC><C=9?;CCD>D8ECB@29@5D4A+;?B=?AC@1C79C+50.8+C18(0?09@:14<-0427<90?)1(7(6>.9>;1=<(39C=@=1=7:0@;8=DD9C=?,A=@+C<D<4<096+7:9.-?9:=06@50E7/<985+@?9.;9;52;A;,86A71?=@3?4:>;88;?7@8?88<<<73,A26?==0>(;234721@?=D@CE@DB5?8=;,8A/DC@<;A9?B7;<969?D>AAA+-.>/1<=C02<<2CA.95;CC<@A@?;2D(<>7;7C;?/4=60?5;C9.;1?CB?ACD>@,9=:37?6>;E@??D2?7?907/<=,CCA+6=:/:;CC==,8D>8=AA<><?,@.:.?=<:<:;:06==@?1<A::?0C08>9??>8??B@2<CCD<:E45985+A6066<4+><6A.43?C8?7>*.<6C<<-4353C62*,@;B,1,58@2D=0;@B=<1;62.C2>;@/97:5?7CA>0630813B/;>/=*?=0>;4<;<><@+:>8->70@8+9>9=A94<+;+86@>2>5?<<4B>.8>;;C=:<4.@>:9@0/-99<(6<.2327=200<>5@1<:128><@@?)07>263?3<@7=>3?=)?;@;@0>9:6<?*46=/8?=C<2;9&8@06@BBCB@,??@(-?CD9:@805?=8<33:7?8;*9,:*9?9==1:C21B;0CD<C5-1=CA>>9*4CA<D?<>?7*D.C?CB.;->18?<;8=1-486=7044>===:999;@?0:C.BBCA3CD;CEC84?C<AE@70EC7CCCCCEDC=ADC>CC9BB?80CE1C:>=5?<=;8788@?(:*<@7)@;7;;649-5>;28857<638?3;CC/7B>2&?@5(3>8:DA56;?C<@<E;;8EEDC90<:@C67C1@@16.,3EDEE5D<@D@@DCDCCCD:@B@CCAA@69CEBECCC2D9>CEA3A@@BCAC;C<B72/CC)/(2E?BCCCDE+E8>>7,=@B8CC;;BDD9;:-=6<7=/+;444C2;C,9;C?E5CC536BC(.AB7D9C:=.5>6@5>E4C?+5:97>:665402-/CEC33:DDCBC>6EE@;;7DCB>>7625<;71?85(0(5268>9-<=::>988=36+.7?:);/6=5/99/2-@D@@EBC5DCE,:-AC)337D=@C8:DD>AC?.CC4A*B@94C98<CCBB?8?@C6DC@?:C2CCCE6DC:C:.=C08-64=68-57/?CD+B1/((@<;-+.370E.?;C>8C564C4;C3C;=CCC?E?@>558;C;=?@==CB-)6/;(4=2?6>8?C@EAD&0D;?78+2EA.83;8(14<@0695;9C72C6;++2DA(C1.7E8>62:C?6C+62E2(DD&..>(970:8234/CC9A?=ECCDCDDBC4;5124282/.0644.774(6-9<DC?.5>2<4CD1C64@609EA.CB%9DDD8288DCCB7<402;B0CC6(710'(@/69;@CC45:71?427??<AC6DCCCCC?500-CEED?C<><?0<C9:<6C8C8E82CCC2CCDCB@@EACC><C@C;BD8B6'C85.2;C?CEBDD642<+;CD8/90?&3644BD28<0CA>E4A0-7.D9/E6?40@/+(AC3D0<C9D;8)<(E9(C(:518:+6D89DB4?CBC8;:2D@CE(D81(2(6220/05&4+6050%9(03:(%;2:C&(C.6C.,05@B085.ACCCCC=CC";
	
Assert.assertFalse(bases.length() == qualities.length());	// due to the read start chars in bases string
		
		origPileups = PileupElementUtil.getPileupCounts(bases, qualities);
		
		maxBaseCount = origPileups.get(0).getTotalCount();
		maxVariantCount = PileupElementUtil.getLargestVariantCount(origPileups);
		Assert.assertEquals(1753, maxBaseCount);
		Assert.assertEquals(4, maxVariantCount);
		Assert.assertEquals(1753, origPileups.get(0).getQualities().size());
	}
	
	@Test
	public void testPassesWeightedVotingCheck() {
		String bases = ".A..$.$.$.$.$.........................................................^@.^U.^Q.^L.";
		String qualities = "I7\"%(9/6BH@1AII&*%I=III9&II:IIIIIIIIIIIIIIIIIIIIIIIIEIIIIII4IIIEI>BIH";
		
		List<PileupElement> origPileups = PileupElementUtil.getPileupCounts(bases, qualities);
		Assert.assertFalse(PileupElementUtil.passesWeightedVotingCheck(origPileups, 10));
		
		bases = "..........AAAAA";
		qualities = "IIIIIIIIIIIIIII";
		origPileups = PileupElementUtil.getPileupCounts(bases, qualities);
		Assert.assertTrue(PileupElementUtil.passesWeightedVotingCheck(origPileups, 10));
		Assert.assertTrue(PileupElementUtil.passesWeightedVotingCheck(origPileups, 33));
		Assert.assertFalse(PileupElementUtil.passesWeightedVotingCheck(origPileups, 34));
		
		bases = "..........AAACC";
		qualities = "IIIIIIIIIIIIIII";
		origPileups = PileupElementUtil.getPileupCounts(bases, qualities);
		Assert.assertTrue(PileupElementUtil.passesWeightedVotingCheck(origPileups, 10));
		Assert.assertFalse(PileupElementUtil.passesWeightedVotingCheck(origPileups, 33));
		
		bases = "..........AAA";
		qualities = "IIIIIIIIII\"\"\"";
		origPileups = PileupElementUtil.getPileupCounts(bases, qualities);
		Assert.assertFalse(PileupElementUtil.passesWeightedVotingCheck(origPileups, 10));
		Assert.assertFalse(PileupElementUtil.passesWeightedVotingCheck(origPileups, 1));
		
		bases = "........AA";
		qualities = "IIIIIIIIII";
		origPileups = PileupElementUtil.getPileupCounts(bases, qualities);
		Assert.assertTrue(PileupElementUtil.passesWeightedVotingCheck(origPileups, 20));
		Assert.assertFalse(PileupElementUtil.passesWeightedVotingCheck(origPileups, 21));
		Assert.assertTrue(PileupElementUtil.passesWeightedVotingCheck(origPileups, 19));
	}
	
	@Test
	public void testPassesWeightedVotingCheckRealLifeExample() {
		String bases = ",,....,.aa,a,,,,^L,^N,";
		String qualities = "IIIIII%I9%\",,'4%,3";
		
		List<PileupElement> origPileups = PileupElementUtil.getPileupCounts(bases, qualities);
		Assert.assertFalse(PileupElementUtil.passesWeightedVotingCheck(origPileups, 10));
		
		bases = ".$..,.....,,,,..,,....,,gg,g,,,,g,g.,,,,^R.";
		qualities = ";II/IIIII%%\"@II5(IDII5%56,-%5%GD'(II+*,I";
		origPileups = PileupElementUtil.getPileupCounts(bases, qualities);
		Assert.assertFalse(PileupElementUtil.passesWeightedVotingCheck(origPileups, 10));
	}
	
	@Test
	public void testCreatePileupElementsFromString() {
//		Pattern pattern = Pattern.compile("[ACGT]:[0-9]+\\[[0-9]+.?[0-9]*\\],[0-9]+\\[[0-9]+.?[0-9]*\\]");
//		String testString = "T:17[40],2[1],G:5[32.68],0[0],C:3[16],123[12.01020304]";
//		
//		Matcher m = pattern.matcher(testString);
//		while (m.find()) {
//			String pileup = m.group();
////			System.out.println(pileup);
//			// first char is the base
//			char base = pileup.charAt(0);
//			PileupElement pe = new PileupElement(base);
//			
//			int forwardStrandCount = Integer.parseInt(pileup.substring(2, pileup.indexOf('[')));
//			int reverseStrandCount = Integer.parseInt(pileup.substring(pileup.indexOf(',')+1, pileup.indexOf('[', pileup.indexOf(','))));
//			
//			for (int i = 0 ; i < forwardStrandCount ; i++) pe.incrementForwardCount();
//			for (int i = 0 ; i < reverseStrandCount ; i++) pe.incrementReverseCount();
//			
////			System.out.println(pe.getFormattedString());
//		}
		Assert.assertEquals(null, PileupElementUtil.createPileupElementsFromString(null));
		Assert.assertEquals(null, PileupElementUtil.createPileupElementsFromString(""));
		Assert.assertEquals(true, PileupElementUtil.createPileupElementsFromString("Hello").isEmpty());
		Assert.assertEquals('A', PileupElementUtil.createPileupElementsFromString("A:0[0],0[0]").get(0).getBase());
		Assert.assertEquals(0, PileupElementUtil.createPileupElementsFromString("A:0[0],0[0]").get(0).getTotalCount());
		Assert.assertEquals(0, PileupElementUtil.createPileupElementsFromString("A:0[0],0[0]").get(0).getTotalQualityScore());
		Assert.assertEquals(1, PileupElementUtil.createPileupElementsFromString("A:1[0],0[0]").get(0).getTotalCount());
		Assert.assertEquals(0, PileupElementUtil.createPileupElementsFromString("A:1[0],0[0]").get(0).getReverseCount());
		Assert.assertEquals(1, PileupElementUtil.createPileupElementsFromString("A:0[0],1[0]").get(0).getTotalCount());
		Assert.assertEquals(0, PileupElementUtil.createPileupElementsFromString("A:0[0],1[0]").get(0).getForwardCount());
		
		Assert.assertEquals(3, PileupElementUtil.createPileupElementsFromString("T:17[40],2[1],G:5[32.68],0[0],C:3[16],123[12.01020304]").size());
		Assert.assertEquals('T', PileupElementUtil.createPileupElementsFromString("T:17[40],2[1],G:5[32.68],0[0],C:3[16],123[12.01020304]").get(0).getBase());
		Assert.assertEquals(17, PileupElementUtil.createPileupElementsFromString("T:17[40],2[1],G:5[32.68],0[0],C:3[16],123[12.01020304]").get(0).getForwardCount());
		Assert.assertEquals(2, PileupElementUtil.createPileupElementsFromString("T:17[40],2[1],G:5[32.68],0[0],C:3[16],123[12.01020304]").get(0).getReverseCount());
		Assert.assertEquals(19, PileupElementUtil.createPileupElementsFromString("T:17[40],2[1],G:5[32.68],0[0],C:3[16],123[12.01020304]").get(0).getTotalCount());
		
		Assert.assertEquals('G', PileupElementUtil.createPileupElementsFromString("T:17[40],2[1],G:5[32.68],0[0],C:3[16],123[12.01020304]").get(1).getBase());
		Assert.assertEquals(5, PileupElementUtil.createPileupElementsFromString("T:17[40],2[1],G:5[32.68],0[0],C:3[16],123[12.01020304]").get(1).getForwardCount());
		Assert.assertEquals(0, PileupElementUtil.createPileupElementsFromString("T:17[40],2[1],G:5[32.68],0[0],C:3[16],123[12.01020304]").get(1).getReverseCount());
		Assert.assertEquals(5, PileupElementUtil.createPileupElementsFromString("T:17[40],2[1],G:5[32.68],0[0],C:3[16],123[12.01020304]").get(1).getTotalCount());
		
		Assert.assertEquals('C', PileupElementUtil.createPileupElementsFromString("T:17[40],2[1],G:5[32.68],0[0],C:3[16],123[12.01020304]").get(2).getBase());
		Assert.assertEquals(3, PileupElementUtil.createPileupElementsFromString("T:17[40],2[1],G:5[32.68],0[0],C:3[16],123[12.01020304]").get(2).getForwardCount());
		Assert.assertEquals(123, PileupElementUtil.createPileupElementsFromString("T:17[40],2[1],G:5[32.68],0[0],C:3[16],123[12.01020304]").get(2).getReverseCount());
		Assert.assertEquals(126, PileupElementUtil.createPileupElementsFromString("T:17[40],2[1],G:5[32.68],0[0],C:3[16],123[12.01020304]").get(2).getTotalCount());
	}
	
	@Test
	public void testGetPileupElementString() {
		Assert.assertEquals(null, PileupElementUtil.getPileupElementString(null, '\u0000'));
		
		List<PileupElement> pileups = new ArrayList<PileupElement>();
		Assert.assertEquals(null, PileupElementUtil.getPileupElementString(pileups, '\u0000'));
		PileupElement pe = new PileupElement('A');
		pileups.add(pe);
		Assert.assertEquals("A:0[0],0[0]", PileupElementUtil.getPileupElementString(pileups, '\u0000'));
		pe.incrementForwardCount((byte) 10);
		Assert.assertEquals("A:1[10],0[0]", PileupElementUtil.getPileupElementString(pileups, '\u0000'));
		pe.incrementReverseCount((byte) 40);
		Assert.assertEquals("A:1[10],1[40]", PileupElementUtil.getPileupElementString(pileups, '\u0000'));
		
		PileupElement pe2 = new PileupElement('T');
		pileups.add(pe2);
		Assert.assertEquals("A:1[10],1[40],T:0[0],0[0]", PileupElementUtil.getPileupElementString(pileups, '\u0000'));
		for (int i = 0 ; i < 15 ; i++) pe2.incrementForwardCount();
		Assert.assertEquals("A:1[10],1[40],T:15[0],0[0]", PileupElementUtil.getPileupElementString(pileups, '\u0000'));
		for (int i = 0 ; i < 104 ; i++) pe2.incrementReverseCount((byte)38);
		Assert.assertEquals("A:1[10],1[40],T:15[0],104[38]", PileupElementUtil.getPileupElementString(pileups, '\u0000'));
		
		PileupElement pe3 = new PileupElement(PileupElementUtil.DOT);
		pileups.add(pe3);
		Assert.assertEquals("A:1[10],1[40],T:15[0],104[38],C:0[0],0[0]", PileupElementUtil.getPileupElementString(pileups, 'C'));
		for (int i = 0 ; i < 7 ; i++) pe3.incrementForwardCount();
		Assert.assertEquals("A:1[10],1[40],T:15[0],104[38],C:7[0],0[0]", PileupElementUtil.getPileupElementString(pileups, 'C'));
		for (int i = 0 ; i < 62 ; i++) pe3.incrementReverseCount((byte)24);
		Assert.assertEquals("A:1[10],1[40],T:15[0],104[38],C:7[0],62[24]", PileupElementUtil.getPileupElementString(pileups, 'C'));
	}
	
	@Test
	public void testGetBasesFromPileupElements() {
		Assert.assertEquals(null, PileupElementUtil.getBasesFromPileupElements(null, '\u0000'));
		
		List<PileupElement> pileups = new ArrayList<PileupElement>();
		Assert.assertEquals(null, PileupElementUtil.getBasesFromPileupElements(pileups, '\u0000'));
		PileupElement pe = new PileupElement('A');
		pileups.add(pe);
		Assert.assertEquals("A", PileupElementUtil.getBasesFromPileupElements(pileups, '\u0000'));
		pileups.add(new PileupElement('A'));
		Assert.assertEquals("AA", PileupElementUtil.getBasesFromPileupElements(pileups, '\u0000'));
		pileups.add(new PileupElement('A'));
		Assert.assertEquals("AAA", PileupElementUtil.getBasesFromPileupElements(pileups, '\u0000'));
		pileups.add(new PileupElement('X'));
		Assert.assertEquals("AAAX", PileupElementUtil.getBasesFromPileupElements(pileups, '\u0000'));
		pileups.add(new PileupElement('Y'));
		Assert.assertEquals("AAAXY", PileupElementUtil.getBasesFromPileupElements(pileups, '\u0000'));
		pileups.add(new PileupElement(PileupElementUtil.DOT));
		Assert.assertEquals("AAAXYZ", PileupElementUtil.getBasesFromPileupElements(pileups, 'Z'));
	}
	
	@Test
	public void testGetCoverageFromPileupList() {
		Assert.assertEquals(0, PileupElementUtil.getCoverageFromPileupList(null));
		List<PileupElement> pileups = new ArrayList<PileupElement>();
		Assert.assertEquals(0, PileupElementUtil.getCoverageFromPileupList(pileups));
		PileupElement pe = new PileupElement('A');
		pileups.add(pe);
		Assert.assertEquals(0, PileupElementUtil.getCoverageFromPileupList(pileups));
		for (int i = 0 ; i < 15 ; i++) pe.incrementForwardCount();
		Assert.assertEquals(15, PileupElementUtil.getCoverageFromPileupList(pileups));
		for (int i = 0 ; i < 10 ; i++) pe.incrementReverseCount();
		Assert.assertEquals(25, PileupElementUtil.getCoverageFromPileupList(pileups));
		PileupElement pe2 = new PileupElement('B');
		pileups.add(pe2);
		Assert.assertEquals(25, PileupElementUtil.getCoverageFromPileupList(pileups));
		for (int i = 0 ; i < 100 ; i++) pe2.incrementReverseCount();
		Assert.assertEquals(125, PileupElementUtil.getCoverageFromPileupList(pileups));
	}
	
	@Test
	public void testGetPileupFromPileupList() {
		Assert.assertEquals(null, PileupElementUtil.getPileupFromPileupList(null));
		List<PileupElement> pileups = new ArrayList<PileupElement>();
		Assert.assertEquals(null, PileupElementUtil.getPileupFromPileupList(pileups));
		PileupElement pe = new PileupElement('A');
		pileups.add(pe);
		Assert.assertEquals("", PileupElementUtil.getPileupFromPileupList(pileups));
		pe.incrementForwardCount();
		Assert.assertEquals("A", PileupElementUtil.getPileupFromPileupList(pileups));
		for (int i = 0 ; i < 4 ; i++) pe.incrementReverseCount();
		Assert.assertEquals("AAAAA", PileupElementUtil.getPileupFromPileupList(pileups));
		PileupElement pe2 = new PileupElement('Z');
		pileups.add(pe2);
		Assert.assertEquals("AAAAA", PileupElementUtil.getPileupFromPileupList(pileups));
		for (int i = 0 ; i < 2 ; i++) pe2.incrementReverseCount();
		Assert.assertEquals("AAAAAZZ", PileupElementUtil.getPileupFromPileupList(pileups));
		for (int i = 0 ; i < 2 ; i++) pe2.incrementReverseCount();
		Assert.assertEquals("AAAAAZZZZ", PileupElementUtil.getPileupFromPileupList(pileups));
		for (int i = 0 ; i < 2 ; i++) pe2.incrementReverseCount();
		Assert.assertEquals("AAAAAZZZZZZ", PileupElementUtil.getPileupFromPileupList(pileups));
		
	}
	
	@Ignore
	public void testGetBytesVsCharAt() {
		String testString = "...AAACCCGGG...aaagggccc";
		int noOfLoops = 100000;
		long counter = 0;
		
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			for (int j = 0 , end = testString.length() ; j < end ; j++) {
				char c = testString.charAt(j);
				counter++;
			}
		}
//		for (int i = 0 ; i < noOfLoops ; i++) {
//			for (byte b : testString.getBytes()) {
//				counter++;
//			}
//		}
		System.out.println("charAt: " + (System.currentTimeMillis() - start) + " counter: " + counter);
//		System.out.println("getBytes: " + (System.currentTimeMillis() - start) + " counter: " + counter);
		
		counter = 0;
		start = System.currentTimeMillis();
//		for (int i = 0 ; i < noOfLoops ; i++) {
//			for (int j = 0 , end = testString.length() ; j < end ; j++) {
//				char c = testString.charAt(j);
//				counter++;
//			}
//		}
		for (int i = 0 ; i < noOfLoops ; i++) {
			for (byte b : testString.getBytes()) {
				counter++;
			}
		}
		System.out.println("getBytes: " + (System.currentTimeMillis() - start) + " counter: " + counter);
//		System.out.println("charAt: " + (System.currentTimeMillis() - start) + " counter: " + counter);
	}
	
}
