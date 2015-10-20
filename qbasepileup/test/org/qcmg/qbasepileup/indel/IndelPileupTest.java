package org.qcmg.qbasepileup.indel;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import htsjdk.samtools.SAMRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qbasepileup.InputBAM;
import org.qcmg.qbasepileup.Options;
import org.qcmg.qbasepileup.QBasePileupException;


public class IndelPileupTest {
	
	private IndelPileup pileup;
	private IndelPosition position;
	private SAMRecord record;
	private final int[] cols = {23, 34, 25, 10, 12}; 
	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	private byte[] expectedMaskedBases;
	private int[] expectedReferencePositions;
	
	@Before
	public void setUp() throws QBasePileupException, IOException {
		String line = "date\ttumour\tind6\t2\tchr1\t1007\t1008\t1\t-999\t-999\tG\t-999\tGTC\t-/GTC\t\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t-999\tPASS\t--\t--\t--\t--";
		Options options = createMock(Options.class);
		expect(options.includeDuplicates()).andReturn(false);
		replay(options);
		position = new IndelPosition(line, false, "pindel", cols);
		pileup = new IndelPileup(options, new InputBAM(testFolder.newFile("tumour.bam")), position, testFolder.newFile("reference.fa"), 13, 10, 3, true);
		record = new SAMRecord(null);
		record.setAlignmentStart(1000);
		record.setFlags(67);
		record.setCigarString("8M1I92M");
		record.setReferenceName("chr1");
		record.setReadString("TTCTTTTTTTTTGAGACAGAGTCTCCCTCTGTCGCCAGGCTGAAGTGCAGTGGCGCAATCTCGGCTCACTGCAACCTCCGCCTCCCGGGGTCAAGCTATTC");
		expectedMaskedBases =new byte[record.getReadLength()];
		expectedReferencePositions =new int[record.getReadLength()];
		for (int i=0; i<expectedMaskedBases.length; i++) {
			expectedMaskedBases[i] = record.getReadBases()[i];
		}
		int refPos = 1000;
		for (int i=0; i<expectedReferencePositions.length; i++) {
			expectedReferencePositions[i] = refPos;
			refPos++;
		}
	}
	
	
	@After
	public void tearDown() {
		pileup = null;
		record = null;
		position = null;
		expectedMaskedBases = null;
		expectedReferencePositions = null;
	}
	
	@Test
	public void testDeconvoluteCigarString() {
		
		int[] referencePositions = new int[record.getReadLength()];
		byte[] maskedBases =new byte[record.getReadLength()];	
		
		//all matches
		record.setCigarString("101M");
		pileup.deconvoluteCigarString(record, maskedBases, referencePositions);
		assertReadBasesMatch(expectedMaskedBases, maskedBases, expectedReferencePositions, referencePositions);
	
		//with soft clipping and indel
		record.setCigarString("2S6M1I92M");
		expectedMaskedBases[0] = (byte)'S';
		expectedMaskedBases[1] = (byte)'S';
		expectedReferencePositions[0] = -1;
		expectedReferencePositions[1] = -1;
		expectedReferencePositions[8] = 0;
		
		int refPos = 1008;
		for (int i=9; i<expectedReferencePositions.length; i++) {
			expectedReferencePositions[i] = refPos;
			refPos++;
		}
		pileup.deconvoluteCigarString(record, maskedBases, referencePositions);
		assertReadBasesMatch(expectedMaskedBases, maskedBases, expectedReferencePositions, referencePositions);	
	}
	
	private void assertReadBasesMatch(byte[] expectedMaskedBases,
			byte[] maskedBases, int[] expectedReferencePositions,
			int[] referencePositions) {
		for (int i=0; i<expectedMaskedBases.length; i++) {			
			assertEquals(expectedMaskedBases[i], maskedBases[i]);
			assertEquals(expectedReferencePositions[i], referencePositions[i]);
		}		
	}
	
	@Test
	public void testGetStrandBias() {
		pileup.setTotalReads(100);
		pileup.setInformativeReads(90);
		pileup.setSupportingReads(2);

		//not enough supporting reads
		assertFalse(pileup.hasStrandBias());
		
		//enough supporting reads
		pileup.setSupportingReads(3);
		assertTrue(pileup.hasStrandBias());
		
		//enough supporting reads
		pileup.setSupportingReads(30);
		//enough supporting reads & one side has zero
		pileup.setForwardSupportingReads(30);
		pileup.setReverseSupportingReads(0);
		assertTrue(pileup.hasStrandBias());
		pileup.setForwardSupportingReads(0);
		pileup.setReverseSupportingReads(30);
		assertTrue(pileup.hasStrandBias());
		
		//tumour percentages
		pileup.setForwardSupportingReads(1);
		pileup.setReverseSupportingReads(29);
		assertTrue(pileup.hasStrandBias());
		
		pileup.setForwardSupportingReads(29);
		pileup.setReverseSupportingReads(1);
		assertTrue(pileup.hasStrandBias());
		
		//tumour percentages, no bias
		pileup.setForwardSupportingReads(26);
		pileup.setReverseSupportingReads(4);
		assertFalse(pileup.hasStrandBias());
		
		//normal percentages no strand bias
		pileup.setIsTumour(false);
		pileup.setForwardSupportingReads(28);
		pileup.setReverseSupportingReads(2);
		assertFalse(pileup.hasStrandBias());
	}
	
	
	@Test
	public void testToDCCString() {
		pileup.setTotalReads(100);
		pileup.setInformativeReads(90);
		pileup.setSupportingReads(30);
		pileup.setNearbySoftClipCount(3);
		pileup.setNearbyIndelCount(5);
		pileup.setPartialIndelCount(2);
		pileup.setForwardSupportingReads(14);
		pileup.setReverseSupportingReads(16);
		pileup.setPartialIndelCount(2);
		Set<Integer> nStarts = new HashSet<Integer>();
		nStarts.add(1);
		pileup.setNovelStarts(nStarts);
		Homopolymer hp = new Homopolymer();		
		hp.setType(Homopolymer.CONTIGUOUS);
		hp.setHomopolymerCount("10");
		hp.setSequence("TTTTTTTTTTTTTTTTTTTTtAGTC");	
		pileup.setHomopolymer(hp);		
		
		//tumour
		String expected = "1;100;90;30[14|16];2;5;3;\"10 contiguous TTTTTTTTTTTTTTTTTTTTtAGTC\"";
		assertEquals(expected, pileup.toDCCString());
		
		//normal
		expected = "1;100;90;30[14|16];2;5;3";
		pileup.setIsTumour(false);
		assertEquals(expected, pileup.toDCCString());
	}
	
	@Test
	public void testAnnotateIndelPresentInRead() {
		assertEquals(0, pileup.getSupportingReads());
		assertEquals(0, pileup.getNovelStartCount());
		pileup.annotateIndelPresentInRead(record);
		assertEquals(1, pileup.getSupportingReads());
		assertEquals(1, pileup.getNovelStartCount());
	}
	
	@Test
	public void testAddToNovelStarts() {
		
		assertEquals(0, pileup.getNovelStartCount());
		//forward strand
		pileup.addToNovelStarts(record);
		assertEquals(1, pileup.getNovelStartCount());
		//shouldn't be able to add again
		pileup.addToNovelStarts(record);
		assertEquals(1, pileup.getNovelStartCount());
		
		//reverse strand 
		record.setFlags(83);
		record.setCigarString("8M1I92M");
		pileup.addToNovelStarts(record);
		assertEquals(2, pileup.getNovelStartCount());
		assertTrue(pileup.getNovelStarts().contains(1000));		
		assertTrue(pileup.getNovelStarts().contains(1099));
	}
	
	@Test
	public void testParseComplexIndel() {
		String line = "date\ttumour\tind6\t4\tchr1\t1007\t1012\t1\t-999\t-999\tG\t-999\tGTC\tG>GTC\t\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t-999\tPASS\t--\t--\t--\t--";
		
		position = new IndelPosition(line, false, "pindel", cols);
		assertTrue(position.isComplex());
		pileup.setIndelPosition(position);
		assertEquals(0, pileup.getInformativeReads());
		assertEquals(0, pileup.getPartialIndelCount());
		assertEquals(0, pileup.getNearbyIndelCount());
		pileup.parseComplexIndel(record, expectedMaskedBases);
		assertEquals(1, pileup.getInformativeReads());
		assertEquals(1, pileup.getPartialIndelCount());
		assertEquals(0, pileup.getNearbyIndelCount());
		record.setCigarString("3M2D4M1I91M");
		pileup.parseComplexIndel(record, expectedMaskedBases);
		assertEquals(2, pileup.getInformativeReads());
		assertEquals(2, pileup.getPartialIndelCount());
		assertEquals(0, pileup.getNearbyIndelCount());
		
	}
	
	
	
	

}
