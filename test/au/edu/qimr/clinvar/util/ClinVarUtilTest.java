package au.edu.qimr.clinvar.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.qcmg.common.util.Pair;

public class ClinVarUtilTest {
	
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
