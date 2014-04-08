package org.qcmg.qbasepileup.indel;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import net.sf.samtools.SAMRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qbasepileup.InputBAM;
import org.qcmg.qbasepileup.Options;
import org.qcmg.qbasepileup.QBasePileupException;


public class IndelPileupForDeletionTest {
	
	private IndelPileup pileup;
	private IndelPosition position;
	private SAMRecord record;
	private final int[] cols = {23, 34, 25, 10, 12}; 
	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setUp() throws QBasePileupException, IOException {
		String line = "date\ttumour\tind6\t3\tchr1\t1008\t1008\t1\t-999\t-999\tA\t-999\t-\tA/-\t\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t-999\tPASS\t--\t--\t--\t--";
		Options options = createMock(Options.class);
		expect(options.includeDuplicates()).andReturn(false);
		replay(options);
		position = new IndelPosition(line, false, "pindel", cols);
		pileup = new IndelPileup(options, new InputBAM(testFolder.newFile("tumour.bam")), position, testFolder.newFile("reference.fa"), 5, 10, 3, true);
		record = new SAMRecord(null);
		record.setAlignmentStart(1000);
		record.setFlags(67);
		record.setCigarString("8M1D92M");
		record.setReferenceName("chr1");
		record.setReadString("TTCTTTTTTTTTGAGACAGAGTCTCCCTCTGTCGCCAGGCTGAAGTGCAGTGGCGCAATCTCGGCTCACTGCAACCTCCGCCTCCCGGGGTCAAGCTATTC");
		assertTrue(position.isDeletion());
	}
	
	@After
	public void tearDown() {
		pileup = null;
		record = null;
		position = null;
	}

	@Test
	public void testRecordSpansIndel() {
		record.setCigarString("101M");
		record.setAlignmentStart(1007);
		assertTrue(pileup.recordSpansIndel(record));
		record.setAlignmentStart(909);
		assertTrue(pileup.recordSpansIndel(record));
		record.setAlignmentStart(1008);
		assertFalse(pileup.recordSpansIndel(record));
		record.setAlignmentStart(907);
		assertFalse(pileup.recordSpansIndel(record));
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
	public void testNearbyIndel() {
		assertFalse(pileup.nearbyIndel(record));
		
		//I present at start
		record.setCigarString("5M1D94M");
		assertTrue(pileup.nearbyIndel(record));
		
		//I outside of window at start
		record.setCigarString("4M1I96M");
		assertFalse(pileup.nearbyIndel(record));
		
		//I present at end
		record.setAlignmentStart(911);
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
		record.setCigarString("95M1D7M");
		assertTrue(pileup.nearbyIndel(record));
				
		//D present at end
		record.setAlignmentStart(910);
		record.setCigarString("94M1D6M");
		assertFalse(pileup.nearbyIndel(record));
		
		//Partial indel at postion start
		record.setAlignmentStart(1000);		
		record.setCigarString("7M2D92M");
		assertFalse(pileup.nearbyIndel(record));
		
		//Partial indel at postion end		
		record.setCigarString("8M2D92M");
		assertFalse(pileup.nearbyIndel(record));
	}
	
	@Test
	public void testGetDeletionIndelLength() throws QBasePileupException, IOException {
		assertEquals(1, pileup.getDeletionIndelLength(record));
		
		//del in wrong positions
		record.setCigarString("4M1D96M");
		assertEquals(0, pileup.getDeletionIndelLength(record));
		
		//partial
		String line = "date\ttumour\tind6\t3\tchr1\t1008\t1009\t1\t-999\t-999\tA\t-999\t-\tAA/-\t\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t-999\tPASS\t--\t--\t--\t--";
		Options options = createMock(Options.class);
		expect(options.includeDuplicates()).andReturn(false);
		replay(options);
		position = new IndelPosition(line, false, "pindel", cols);
		pileup = new IndelPileup(options, new InputBAM(testFolder.newFile("tumour.bam")), position, testFolder.newFile("reference.fa"), 5, 10, 3, true);
		
//		pileup.getIndelPosition().setEnd(1009);
		record.setCigarString("8M1D92M");
		assertEquals(1, pileup.getDeletionIndelLength(record));
	}
	
	@Test
	public void testParseDeletionIndelWithFullDeletion() {
		assertEquals(0, pileup.getInformativeReads());
		assertEquals(0, pileup.getSupportingReads());
		assertEquals(0, pileup.getPartialIndelCount());
		
		pileup.parseDeletionIndel(record);
		assertEquals(1, pileup.getInformativeReads());
		assertEquals(1, pileup.getSupportingReads());
		assertEquals(0, pileup.getPartialIndelCount());
	}
	
	@Test
	public void testParseDeletionIndelWithPartialDeletion() throws QBasePileupException, IOException {
		assertEquals(0, pileup.getInformativeReads());
		assertEquals(0, pileup.getSupportingReads());
		assertEquals(0, pileup.getPartialIndelCount());		
		
		String line = "date\ttumour\tind6\t3\tchr1\t1008\t1009\t1\t-999\t-999\tAA\t-999\t-\tA/-\t\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t-999\tPASS\t--\t--\t--\t--";
		Options options = createMock(Options.class);
		expect(options.includeDuplicates()).andReturn(false);
		replay(options);
		position = new IndelPosition(line, false, "pindel", cols);
		pileup = new IndelPileup(options, new InputBAM(testFolder.newFile("tumour.bam")), position, testFolder.newFile("reference.fa"), 5, 10, 3, true);
		
		
//		pileup.getIndelPosition().setLength(2);
		pileup.parseDeletionIndel(record);
		assertEquals(1, pileup.getInformativeReads());
		assertEquals(0, pileup.getSupportingReads());
		assertEquals(1, pileup.getPartialIndelCount());
	}
	
	@Test
	public void testParseDeletionIndelWithPartialDeletionMatchingAtFirstBase() {
		assertEquals(0, pileup.getInformativeReads());
		assertEquals(0, pileup.getSupportingReads());
		assertEquals(0, pileup.getPartialIndelCount());		
		record.setCigarString("8M2D91M");
		pileup.parseDeletionIndel(record);
		assertEquals(1, pileup.getInformativeReads());
		assertEquals(0, pileup.getSupportingReads());
		assertEquals(1, pileup.getPartialIndelCount());
	}
	
	
	
	

}
