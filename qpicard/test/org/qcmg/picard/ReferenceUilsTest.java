package org.qcmg.picard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ReferenceUilsTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	
	@Test
	public void getRegionFromReferenceFileWithInvalidDetails() throws IOException {
		
		try {
			ReferenceUtils.getRegionFromReferenceFile(null, null, -1, -1);
			fail();
		} catch (IllegalArgumentException iae) {}
		try {
			ReferenceUtils.getRegionFromReferenceFile("hello", null, -1, -1);
			fail();
		} catch (IllegalArgumentException iae) {}
		try {
			ReferenceUtils.getRegionFromReferenceFile("hello", "there", -1, -1);
			fail();
		} catch (IllegalArgumentException iae) {}
		
		final File refFile = testFolder.newFile("ref.fa");
		try {
			ReferenceUtils.getRegionFromReferenceFile(refFile.getAbsolutePath(), "there", -1, -1);
			fail();
		} catch (IllegalArgumentException iae) {}
		final File refIndexFile = testFolder.newFile("ref.fa.fai");
		setupReferenceFile(refFile, refIndexFile);
		try {
			ReferenceUtils.getRegionFromReferenceFile(refFile.getAbsolutePath(), "there", -1, -1);
			fail();
		} catch (IllegalArgumentException iae) {}
		try {
			ReferenceUtils.getRegionFromReferenceFile(refFile.getAbsolutePath(), "1", 1, 100);
			fail();
		} catch (IllegalArgumentException iae) {}
		try {
			ReferenceUtils.getRegionFromReferenceFile(refFile.getAbsolutePath(), "chr1", 10000, 100000);
			fail();
		} catch (IllegalArgumentException iae) {}
		ReferenceUtils.getRegionFromReferenceFile(refFile.getAbsolutePath(), "chr1", 10000, 100000);
		
	}
	
	@Test
	public void getRegionFromReferenceFile() throws IOException {
		final File refFile = testFolder.newFile("ref.fa");
		final File refIndexFile = testFolder.newFile("ref.fa.fai");
		setupReferenceFile(refFile, refIndexFile);
		
		byte [] refBases = ReferenceUtils.getRegionFromReferenceFile(refFile.getAbsolutePath(), "chr1", 0, 9);
		assertEquals("GATCACAGGT", new String(refBases));
		
		refBases = ReferenceUtils.getRegionFromReferenceFile(refFile.getAbsolutePath(), "chr1", 69, 78);
		assertEquals("GTATGCACGC", new String(refBases));
		refBases = ReferenceUtils.getRegionFromReferenceFile(refFile.getAbsolutePath(), "chr1", 344, 353);
		assertEquals("AACCCCAAAA", new String(refBases));
		
		refBases = ReferenceUtils.getRegionFromReferenceFile(refFile.getAbsolutePath(), "chr1", 2530, 2539);
		assertEquals("AACCGTGCAA", new String(refBases));
	}
	
	private void setupReferenceFile(File file, File indexFile) throws IOException {
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file));) {
			writer.write(">chr1\n");
			writer.write("GATCACAGGTCTATCACCCTATTAACCACTCACGGGAGCTCTCCATGCATTTGGTATTTTCGTCTGGGGG");
			writer.write("GTATGCACGCGATAGCATTGCGAGACGCTGGAGCCGGAGCACCCTATGTCGCAGTATCTGTCTTTGATTC");
			writer.write("CTGCCTCATCCTATTATTTATCGCACCTACGTTCAATATTACAGGCGAACATACTTACTAAAGTGTGTTA");
			writer.write("ATTAATTAATGCTTGTAGGACATAATAATAACAATTGAATGTCTGCACAGCCACTTTCCACACAGACATC");
			writer.write("ATAACAAAAAATTTCCACCAAACCCCCCCTCCCCCGCTTCTGGCCACAGCACTTAAACACATCTCTGCCA");
			writer.write("AACCCCAAAAACAAAGAACCCTAACACCAGCCTAACCAGATTTCAAATTTTATCTTTTGGCGGTATGCAC");
			writer.write("TTTTAACAGTCACCCCCCAACTAACACATTATTTTCCCCTCCCACTCCCATACTACTAATCTCATCAATA");
			writer.write("CAACCCCCGCCCATCCTACCCAGCACACACACACCGCTGCTAACCCCATACCCCGAACCAACCAAACCCC");
			writer.write("AAAGACACCCCCCACAGTTTATGTAGCTTACCTCCTCAAAGCAATACACTGAAAATGTTTAGACGGGCTC");
			writer.write("ACATCACCCCATAAACAAATAGGTTTGGTCCTAGCCTTTCTATTAGCTCTTAGTAAGATTACACATGCAA");
			writer.write("GCATCCCCGTTCCAGTGAGTTCACCCTCTAAATCACCACGATCAAAAGGAACAAGCATCAAGCACGCAGC");
			writer.write("AATGCAGCTCAAAACGCTTAGCCTAGCCACACCCCCACGGGAAACAGCAGTGATTAACCTTTAGCAATAA");
			writer.write("ACGAAAGTTTAACTAAGCTATACTAACCCCAGGGTTGGTCAATTTCGTGCCAGCCACCGCGGTCACACGA");
			writer.write("TTAACCCAAGTCAATAGAAGCCGGCGTAAAGAGTGTTTTAGATCACCCCCTCCCCAATAAAGCTAAAACT");
			writer.write("CACCTGAGTTGTAAAAAACTCCAGTTGACACAAAATAGACTACGAAAGTGGCTTTAACATATCTGAACAC");
			writer.write("ACAATAGCTAAGACCCAAACTGGGATTAGATACCCCACTATGCTTAGCCCTAAACCTCAACAGTTAAATC");
			writer.write("AACAAAACTGCTCGCCAGAACACTACGAGCCACAGCTTAAAACTCAAAGGACCTGGCGGTGCTTCATATC");
			writer.write("CCTCTAGAGGAGCCTGTTCTGTAATCGATAAACCCCGATCAACCTCACCACCTCTTGCTCAGCCTATATA");
			writer.write("CCGCCATCTTCAGCAAACCCTGATGAAGGCTACAAAGTAAGCGCAAGTACCCACGTAAAGACGTTAGGTC");
			writer.write("AAGGTGTAGCCCATGAGGTGGCAAGAAATGGGCTACATTTTCTACCCCAGAAAACTACGATAGCCCTTAT");
			writer.write("GAAACTTAAGGGTCGAAGGTGGATTTAGCAGTAAACTAAGAGTAGAGTGCTTAGTTGAACAGGGCCCTGA");
			writer.write("AGCGCGTACACACCGCCCGTCACCCTCCTCAAGTATACTTCAAAGGACATTTAACTAAAACCCCTACGCA");
			writer.write("TTTATATAGAGGAGACAAGTCGTAACATGGTAAGTGTACTGGAAAGTGCACTTGGACGAACCAGAGTGTA");
			writer.write("GCTTAACACAAAGCACCCAACTTACACTTAGGAGATTTCAACTTAACTTGACCGCTCTGAGCTAAACCTA");
			writer.write("GCCCCAAACCCACTCCACCTTACTACCAGACAACCTTAGCCAAACCATTTACCCAAATAAAGTATAGGCG");
			writer.write("ATAGAAATTGAAACCTGGCGCAATAGATATAGTACCGCAAGGGAAAGATGAAAAATTATAACCAAGCATA");
			writer.write("ATATAGCAAGGACTAACCCCTATACCTTCTGCATAATGAATTAACTAGAAATAACTTTGCAAGGAGAGCC");
			writer.write("AAAGCTAAGACCCCCGAAACCAGACGAGCTACCTAAGAACAGCTAAAAGAGCACACCCGTCTATGTAGCA");
			writer.write("AAATAGTGGGAAGATTTATAGGTAGAGGCGACAAACCTACCGAGCCTGGTGATAGCTGGTTGTCCAAGAT");
			writer.write("AGAATCTTAGTTCAACTTTAAATTTGCCCACAGAACCCTCTAAATCCCCTTGTAAATTTAACTGTTAGTC");
			writer.write("CAAAGAGGAACAGCTCTTTGGACACTAGGAAAAAACCTTGTAGAGAGAGTAAAAAATTTAACACCCATAG");
			writer.write("TAGGCCTAAAAGCAGCCACCAATTAAGAAAGCGTTCAAGCTCAACACCCACTACCTAAAAAATCCCAAAC");
			writer.write("ATATAACTGAACTCCTCACACCCAATTGGACCAATCTATCACCCTATAGAAGAACTAATGTTAGTATAAG");
			writer.write("TAACATGAAAACATTCTCCTCCGCATAAGCCTGCGTCAGATTAAAACACTGAACTGACAATTAACAGCCC");
			writer.write("AATATCTACAATCAACCAACAAGTCATTATTACCCTCACTGTCAACCCAACACAGGCATGCTCATAAGGA");
			writer.write("AAGGTTAAAAAAAGTAAAAGGAACTCGGCAAATCTTACCCCGCCTGTTTACCAAAAACATCACCTCTAGC");
			writer.write("ATCACCAGTATTAGAGGCACCGCCTGCCCAGTGACACATGTTTAACGGCCGCGGTACCCTAACCGTGCAA");
		}
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile));) {
			writer.write("chr1\t16571\t7\t50\t51\n");
		}
    }
	

}
