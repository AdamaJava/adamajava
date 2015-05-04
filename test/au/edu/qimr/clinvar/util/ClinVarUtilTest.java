package au.edu.qimr.clinvar.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ClinVarUtilTest {
	
	@Test
	public void basicEditDistance() {
		assertEquals(0, ClinVarUtil.getBasicEditDistance("hello", "hello"));
		assertEquals(1, ClinVarUtil.getBasicEditDistance("hello", "hallo"));
		assertEquals(4, ClinVarUtil.getBasicEditDistance("hello", " hell"));
		assertEquals(2, ClinVarUtil.getBasicEditDistance("crap", "carp"));
	}
	
	
	@Test
	public void getEditDistances() {
		assertArrayEquals(new int[]{0,0}, ClinVarUtil.getBasicAndLevenshteinEditDistances("", ""));
		assertArrayEquals(new int[]{0,0}, ClinVarUtil.getBasicAndLevenshteinEditDistances("A", "A"));
		assertArrayEquals(new int[]{0,0}, ClinVarUtil.getBasicAndLevenshteinEditDistances("AC", "AC"));
		assertArrayEquals(new int[]{0,0}, ClinVarUtil.getBasicAndLevenshteinEditDistances("ACG", "ACG"));
		assertArrayEquals(new int[]{0,0}, ClinVarUtil.getBasicAndLevenshteinEditDistances("ACGT", "ACGT"));
		
		assertArrayEquals(new int[]{1,1}, ClinVarUtil.getBasicAndLevenshteinEditDistances("A", "C"));
		assertArrayEquals(new int[]{1,1}, ClinVarUtil.getBasicAndLevenshteinEditDistances("C", "A"));
		assertArrayEquals(new int[]{1,1}, ClinVarUtil.getBasicAndLevenshteinEditDistances("ACC", "AAC"));
		
		assertArrayEquals(new int[]{3,2}, ClinVarUtil.getBasicAndLevenshteinEditDistances("AACCGGTT", "ACCGGTTT"));
		
		assertArrayEquals(new int[]{3,2}, ClinVarUtil.getBasicAndLevenshteinEditDistances("grog", "gog "));
		
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
	public void doesSlidingMethodWork() {
//	14:07:58.810 [main] INFO au.edu.qimr.clinvar.util.FastqProbeMatchUtil - probe 784, r2Overlap: CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA, r1OverlapRC: CCATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAG, basicED: 51, led: 2
//	14:07:58.919 [main] INFO au.edu.qimr.clinvar.util.FastqProbeMatchUtil - probe 784, r2Overlap: CCATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAG, r1OverlapRC: CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA, basicED: 51, led: 2
//	14:07:58.963 [main] INFO au.edu.qimr.clinvar.util.FastqProbeMatchUtil - probe 784, r2Overlap: ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC, r1OverlapRC: CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA, basicED: 52, led: 2
//	14:07:59.105 [main] INFO au.edu.qimr.clinvar.util.FastqProbeMatchUtil - probe 784, r2Overlap: ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC, r1OverlapRC: CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA, basicED: 52, led: 2
//	14:07:59.137 [main] INFO au.edu.qimr.clinvar.util.FastqProbeMatchUtil - probe 784, r2Overlap: CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA, r1OverlapRC: CCATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAG, basicED: 51, led: 2
//	14:07:59.300 [main] INFO au.edu.qimr.clinvar.util.FastqProbeMatchUtil - probe 784, r2Overlap: ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC, r1OverlapRC: CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA, basicED: 52, led: 2
//	14:07:59.352 [main] INFO au.edu.qimr.clinvar.util.FastqProbeMatchUtil - probe 784, r2Overlap: CCATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAG, r1OverlapRC: CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA, basicED: 51, led: 2
//	14:07:59.761 [main] INFO au.edu.qimr.clinvar.util.FastqProbeMatchUtil - probe 784, r2Overlap: ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC, r1OverlapRC: CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA, basicED: 52, led: 2
//	14:07:59.812 [main] INFO au.edu.qimr.clinvar.util.FastqProbeMatchUtil - probe 784, r2Overlap: CATTCGTGCAAGTAGGCATAGTCCCGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGG, r1OverlapRC: CATTCGTTCAAGTAGTCATACTCCCGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA, basicED: 4, led: 4
//	14:07:59.930 [main] INFO au.edu.qimr.clinvar.util.FastqProbeMatchUtil - probe 784, r2Overlap: ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC, r1OverlapRC: CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA, basicED: 52, led: 2
//	14:07:59.947 [main] INFO au.edu.qimr.clinvar.util.FastqProbeMatchUtil - probe 784, r2Overlap: ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC, r1OverlapRC: CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA, basicED: 52, led: 2
		
		
		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA", "CCATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAG"));
		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("CCATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAG", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA", "CCATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAG"));
		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("CCATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAG", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("CATTCGTGCAAGTAGGCATAGTCCCGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGG", "CATTCGTTCAAGTAGTCATACTCCCGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
		
		
	}
}
