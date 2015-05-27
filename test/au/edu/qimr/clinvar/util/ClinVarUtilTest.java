package au.edu.qimr.clinvar.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.qcmg.common.util.Pair;

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
		swData[0] = "ACGT";
		swData[1] = " |||";
		swData[2] = "-CGT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(-1), p.getLeft());
		assertEquals("A/", p.getRight());
		
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
		swData[0] = "--GT";
		swData[1] = "  ||";
		swData[2] = "ACGT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(-1), p.getLeft());
		assertEquals("/AC", p.getRight());
		
		swData[0] = "A--T";
		swData[1] = "|  |";
		swData[2] = "ACGT";
		
		mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
		p = mutations.get(0);
		assertEquals(Integer.valueOf(0), p.getLeft());
		assertEquals("A/ACG", p.getRight());
		
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
		swData[0] = "--GTACGT";
		swData[1] = "  ||  ||";
		swData[2] = "ACGT--GT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(2, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(-1), p.getLeft());
		assertEquals("/AC", p.getRight());
		
		p = mutations.get(1);
		assertEquals(Integer.valueOf(3), p.getLeft());
		assertEquals("TAC/T", p.getRight());
	}
	
	@Test
	public void getMutationFromSWDataMultiBaseIndelAndSnp() {
		String [] swData = new String[3];
		swData[0] = "--GTACGT";
		swData[1] = "  ||||.|";
		swData[2] = "ACGTACAT";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(2, mutations.size());
		Pair<Integer, String> p = mutations.get(0);
		assertEquals(Integer.valueOf(-1), p.getLeft());
		assertEquals("/AC", p.getRight());
		
		p = mutations.get(1);
		assertEquals(Integer.valueOf(6), p.getLeft());
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
	public void getMutationString() {
		/*
		 * - AACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC
14:41:59.598 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
14:41:59.598 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - AACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC

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
//		assertEquals("C-", ClinVarUtil.getMutationString(7, 2, swDiffs));
//		assertEquals("C-T", ClinVarUtil.getMutationString(7, 3, swDiffs));
//		 posInAmplicon = 13;	// want position 88
//		 rec = VcfUtils.createVcfRecord(new ChrPosition("1", 100, 101), "CT");
//		 assertEquals("C-", ClinVarUtil.getMutationString(rec, posInAmplicon, swDiffs));
		 
		 // this data has no mutations - should just get CT back
		swDiffs[0] = "AACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		swDiffs[1] = "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swDiffs[2] = "AACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		assertEquals("TTTTCT", ClinVarUtil.getMutationString(20, 6, swDiffs));
		
//		assertEquals("CT", ClinVarUtil.getMutationString(rec, posInAmplicon, swDiffs));
		
		// insertion
		swDiffs[0] = "ATTTCTAAAAATATTTCAATGGGTCATATCACAGATTC-TTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		swDiffs[1] = "|||||||||||||||||||||||||||||||||||||| ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swDiffs[2] = "ATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		assertEquals("CT", ClinVarUtil.getMutationString(37, 1, swDiffs));
		
		
		
		

		/*
		 * And a single snp
		 * - AACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC
09:18:08.023 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - |.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
09:18:08.023 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - ACCCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC

		 */
		swDiffs[0] = "AACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		swDiffs[1] = "|.||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swDiffs[2] = "ACCCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
//		rec = VcfUtils.createVcfRecord(new ChrPosition("1", 100, 100), "A");
//		posInAmplicon = 99;	// want position 2
//		assertEquals("C", ClinVarUtil.getMutationString(rec, posInAmplicon, swDiffs));
		 
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
16:06:35.984 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC
16:06:35.984 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
16:06:35.985 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTC-TTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC
		 */
		
		// I have deleted the first 25 bases from each string to make things easier...
		String [] swData = new String[3];
		swData[0] = "TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTCTTTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		swData[1] = "||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||";
		swData[2] = "TAACCCTGGCTATCATTCTGCTTTTCTTGGCTGTCTTTCAGATTTGACTTTATTTCTAAAAATATTTCAATGGGTCATATCACAGATTC-TTTTTTTTAAATTAAAGTAACATTTCCAATCTACTAATGCTAATACTGTTTCGTATTTATAGCTGATTTGATGGAGTTGGACATGGCCATGGAACCAGACAGAAAAGCGGCTGTTAGTCACTGGCAGCAACAGTCTTAC";
		
		List<Pair<Integer, String>> mutations = ClinVarUtil.getPositionRefAndAltFromSW(swData);
		assertEquals(1, mutations.size());
//		Pair<Integer, String> p = mutations.get(0);
//		assertEquals(Integer.valueOf(4), p.getLeft());
//		assertEquals("G/T", p.getRight());
//		
//		p = mutations.get(1);
//		assertEquals(Integer.valueOf(5), p.getLeft());
//		assertEquals("G/T", p.getRight());
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
		
		
//		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA", "CCATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAG"));
//		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("CCATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAG", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
//		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
//		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
//		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA", "CCATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAG"));
//		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
//		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("CCATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAG", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
//		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
//		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("CATTCGTGCAAGTAGGCATAGTCCCGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGG", "CATTCGTTCAAGTAGTCATACTCCCGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
//		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
//		System.out.println("no of slides: " + ClinVarUtil.noOfSlidesToGetPerfectMatch("ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
//		System.out.println("and the new way...");
//		System.out.println("no of slides NW: " + ClinVarUtil.getOutwardSlideCount("CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA", "CCATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAG"));
//		System.out.println("no of slidesNW: " + ClinVarUtil.getOutwardSlideCount("CCATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAG", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
//		System.out.println("no of slidesNW: " + ClinVarUtil.getOutwardSlideCount("ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
//		System.out.println("no of slidesNW: " + ClinVarUtil.getOutwardSlideCount("ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
//		System.out.println("no of slidesNW: " + ClinVarUtil.getOutwardSlideCount("CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA", "CCATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAG"));
//		System.out.println("no of slidesNW: " + ClinVarUtil.getOutwardSlideCount("ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
//		System.out.println("no of slidesNW: " + ClinVarUtil.getOutwardSlideCount("CCATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAG", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
//		System.out.println("no of slidesNW: " + ClinVarUtil.getOutwardSlideCount("ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
//		System.out.println("no of slidesNW: " + ClinVarUtil.getOutwardSlideCount("CATTCGTGCAAGTAGGCATAGTCCCGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGG", "CATTCGTTCAAGTAGTCATACTCCCGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
//		System.out.println("no of slidesNW: " + ClinVarUtil.getOutwardSlideCount("ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
//		System.out.println("no of slidesNW: " + ClinVarUtil.getOutwardSlideCount("ATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGAC", "CATTCGTTCAAGTAGTCATAGTCCTGGTCTTTGTCTGACTCTGAGGAGTTCAGGGAGCTCAGA"));
		
	}
	
}
