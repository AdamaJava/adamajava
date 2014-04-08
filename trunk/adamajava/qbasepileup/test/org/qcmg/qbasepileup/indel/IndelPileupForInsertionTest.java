package org.qcmg.qbasepileup.indel;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qbasepileup.InputBAM;
import org.qcmg.qbasepileup.Options;
import org.qcmg.qbasepileup.QBasePileupException;


public class IndelPileupForInsertionTest {
	
	private IndelPileup pileup;
	private IndelPosition position;
	private SAMRecord record;
	private final int[] cols = {23, 34, 25, 10, 12}; 
	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	private byte[] expectedMaskedBases;

	
	@Before
	public void setUp() throws QBasePileupException, IOException {
		String line = "date\ttumour\tind6\t2\tchr1\t1007\t1008\t1\t-999\t-999\t-\t-999\tTC\t-/TC\t\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t-999\tPASS\t--\t--\t--\t--";
		Options options = createMock(Options.class);
		expect(options.includeDuplicates()).andReturn(false);
		replay(options);
		position = new IndelPosition(line, false, "pindel", cols);
		pileup = new IndelPileup(options, new InputBAM(testFolder.newFile("tumour.bam")), position, testFolder.newFile("reference.fa"), 5, 10, 3, true);
		record = new SAMRecord(null);
		record.setAlignmentStart(1000);
		record.setFlags(67);
		record.setCigarString("8M1I92M");
		record.setReferenceName("chr1");
		record.setReadString("TTCTTTTTTTTTGAGACAGAGTCTCCCTCTGTCGCCAGGCTGAAGTGCAGTGGCGCAATCTCGGCTCACTGCAACCTCCGCCTCCCGGGGTCAAGCTATTC");
		expectedMaskedBases =new byte[record.getReadLength()];
		for (int i=0; i<expectedMaskedBases.length; i++) {
			expectedMaskedBases[i] = record.getReadBases()[i];
		}
		
		assertTrue(position.isInsertion());
	}
	
	
	@After
	public void tearDown() {
		pileup = null;
		record = null;
		position = null;
		expectedMaskedBases = null;
	}
	
	@Test
	public void testParseInsertionIndel() {
		assertEquals(0, pileup.getInformativeReads());
		pileup.parseInsertionIndel(record, expectedMaskedBases);
		assertEquals(1, pileup.getInformativeReads());
	}
	
	@Test
	public void testSoftClipInWindow() {
		
		//not near window
		record.setCigarString("101M");
		record.setAlignmentStart(990);
		assertFalse(pileup.inSoftClipWindow(record));
		
		//near window, no soft clipping		
		record.setAlignmentStart(1004);
		assertFalse(pileup.inSoftClipWindow(record));
		
		//left hand clipping
		record.setCigarString("1S100M");
		assertTrue(pileup.inSoftClipWindow(record));
		
		//right hand clipping
		record.setAlignmentStart(908);
		record.setCigarString("100M1S");
		assertTrue(pileup.inSoftClipWindow(record));
	}
	
	@Test
	public void testRecordSpansIndel() {
		record.setCigarString("101M");
		record.setAlignmentStart(1007);
		assertTrue(pileup.recordSpansIndel(record));
		record.setAlignmentStart(908);
		assertTrue(pileup.recordSpansIndel(record));
		record.setAlignmentStart(1008);
		assertFalse(pileup.recordSpansIndel(record));
		record.setAlignmentStart(907);
		assertFalse(pileup.recordSpansIndel(record));
	}
	
	
	
	@Test
	public void testNearbyIndel() {
		assertFalse(pileup.nearbyIndel(record));
		
		//I present at start
		record.setCigarString("5M2I94M");
		assertTrue(pileup.nearbyIndel(record));
		
		//I outside of window at start
		record.setCigarString("4M1I96M");
		assertFalse(pileup.nearbyIndel(record));
		
		//I present at end
		record.setAlignmentStart(910);
		record.setCigarString("95M1I5M");
		assertTrue(pileup.nearbyIndel(record));
		
		//I present at end
		record.setAlignmentStart(910);
		record.setCigarString("94M1I6M");
		assertFalse(pileup.nearbyIndel(record));
		
		record.setAlignmentStart(1000);
		//D present at inserted base position
		record.setCigarString("7M1D91M");
		assertTrue(pileup.nearbyIndel(record));
				
		//D outside of window at start
		record.setCigarString("3M1D97M");
	    assertFalse(pileup.nearbyIndel(record));
				
		//D present present at end
		record.setAlignmentStart(910);
		record.setCigarString("94M1D6M");
		assertTrue(pileup.nearbyIndel(record));
				
		//D present at end
		record.setAlignmentStart(910);
		record.setCigarString("93M1D7M");
		assertFalse(pileup.nearbyIndel(record));
	}
	
	@Test
	public void testFindInsertionInRecordWithCompleteInsertion() {
		pileup.getIndelPosition().setMotif("T");
		assertEquals(0, pileup.getNovelStartCount());
		assertEquals(0, pileup.getSupportingReads());
		assertEquals(0, pileup.getPartialIndelCount());
		pileup.findInsertionInRecord(record, expectedMaskedBases);
		assertEquals(1, pileup.getNovelStartCount());
		assertEquals(1, pileup.getSupportingReads());
		assertEquals(0, pileup.getPartialIndelCount());
	}
	
	
	@Test
	public void testFindInsertionInRecordWithNoInsertion() {
		assertEquals(0, pileup.getNovelStartCount());
		assertEquals(0, pileup.getSupportingReads());
		assertEquals(0, pileup.getPartialIndelCount());
		record.setCigarString("92M1I8M");
		pileup.findInsertionInRecord(record, expectedMaskedBases);
		assertEquals(0, pileup.getNovelStartCount());
		assertEquals(0, pileup.getSupportingReads());
		assertEquals(0, pileup.getPartialIndelCount());
	}
	
	@Test
	public void testFindInsertionInRecordWithPartialInsertion() {
		//first base matches, but second doesn't
		
		assertEquals(0, pileup.getNovelStartCount());
		assertEquals(0, pileup.getSupportingReads());
		assertEquals(0, pileup.getPartialIndelCount());
		pileup.findInsertionInRecord(record, expectedMaskedBases);
		assertEquals(0, pileup.getNovelStartCount());
		assertEquals(0, pileup.getSupportingReads());
		assertEquals(1, pileup.getPartialIndelCount());
		
		
		record.setCigarString("8M1I92M");
		pileup.getIndelPosition().setMotif("AA");
		pileup.findInsertionInRecord(record, expectedMaskedBases);
		assertEquals(0, pileup.getNovelStartCount());
		assertEquals(0, pileup.getSupportingReads());
		assertEquals(2, pileup.getPartialIndelCount());
		
	}

	@Test
	public void testCigarOperatorAtReadPosition() {		
		assertTrue(pileup.cigarOperatorAtReadPostion(record, CigarOperator.INSERTION, 8));
		//not in record
		record.setCigarString("101M");
		assertFalse(pileup.cigarOperatorAtReadPostion(record, CigarOperator.INSERTION, 8));
		
		record.setCigarString("7M1I93M");
		assertFalse(pileup.cigarOperatorAtReadPostion(record, CigarOperator.INSERTION, 8));
	
		record.setCigarString("9M1I93M");
		assertFalse(pileup.cigarOperatorAtReadPostion(record, CigarOperator.INSERTION, 8));
	
	}
	
	@Test
	public void testnNBasePresentInInsertion() {
		byte[] readBases = record.getReadBases();
		assertFalse(pileup.nBasePresentInInsertion(record, readBases));
		
		//reference base before
		readBases[7] = (byte) 'N';
		assertTrue(pileup.nBasePresentInInsertion(record, readBases));
		
		//reset
		readBases[7] = (byte) 'A';
		assertFalse(pileup.nBasePresentInInsertion(record, readBases));
		
		//inserted base
		readBases[8] = (byte) 'N';
		assertTrue(pileup.nBasePresentInInsertion(record, readBases));
		
		//reset
		readBases[8] = (byte) 'A';
		assertFalse(pileup.nBasePresentInInsertion(record, readBases));
		
		//reset
		readBases[9] = (byte) 'N';
		assertTrue(pileup.nBasePresentInInsertion(record, readBases));		
	}

	

	
	
	
	

}
