package au.edu.qimr.clinvar;

import static org.junit.Assert.assertEquals;
import htsjdk.samtools.util.SequenceUtil;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class EditDistance {
	
	@Test
	public void editDistanceOfZero() {
		String s1 = "GGGGAGTAGAAGGCAAATAAGGAAAAGGATTAGAAATAATAATAATAATCAACCCGCATAGCAGCTGATTTCTCTAGCCCAACCTGCTAATTAATATATGCTAAAGTGAGAGGGGGCAATACTGGAGGGAGGGGAGAATTGGGGAGACCAC";
		String s2 = "GGGGAGTAGAAGGCAAATAAGGAAAAG";
		
		assertEquals(0, StringUtils.getLevenshteinDistance(s2, s1.subSequence(0, s2.length())));
	}
	
	
	@Test
	public void fragmentMaking() {
		String read1String = "CGTTGTAACCCGAATGGGGAAGCCTCCACCGGCGGTTTTCTCCTCCCCCTCCTTGCCTGGGCTAGAGACGGAATATCTGTTTTCGAAATCACACCAAACAAAACAAATGCCGAAAGCGCCCCGGACTTTTCGAGGGCCTTTCCTTCCTGGT";
		String read2String = "CTTTCGTGGTTCACATCCCGCGGCTCACGGGGGAGTGGGCAGCGCCAGGGGCGCCCGCCGCTGTGGCCCTCGTGCTGATGCTACTGAGGAGCCAGCGTCTAGGGCAGCAGCCGCTTCCTAGAAGACCAGGTAGGAAAGGCCCTCGAAAAGT";
		int expectedFragLen = 275;
		int combinedReadLength = read1String.length() + read2String.length();
		int expectedOverlap = combinedReadLength - expectedFragLen;
		
		System.out.println("expectedOverlap: " + expectedOverlap);
//		13:40:06.067 [main] INFO au.edu.qimr.clinvar.Q3ClinVar - for probe Probe [id=703, dlsoSeq=GCTTCCCCATTCGGGTTACAACG, dlsoSeqRC=CGTTGTAACCCGAATGGGGAAGC, ulsoSeq=TTTTCGTGGTTCACATCCCGCGG, ulsoSeqRC=CCGCGGGATGTGAACCACGAAAA] we have expected frag length of 275, combined read length of 302, which should give an overlap of: 27
		
		String r1Overlap = read1String.substring(read1String.length() - expectedOverlap);
		String r2Overlap = read2String.substring(read2String.length() - expectedOverlap);
		System.out.println("r1 overlap: " + r1Overlap);
		System.out.println("r2 overlap: " + r2Overlap);
		
		String r1OverlapRC = SequenceUtil.reverseComplement(r1Overlap);
		System.out.println("r1 overlap RC: " + r1OverlapRC);
		assertEquals(1, StringUtils.getLevenshteinDistance(r2Overlap,r1OverlapRC));
	}

}
