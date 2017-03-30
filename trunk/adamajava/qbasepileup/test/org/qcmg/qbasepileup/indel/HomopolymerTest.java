package org.qcmg.qbasepileup.indel;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qbasepileup.QBasePileupUtil;



public class HomopolymerTest {
	
	private final int hompolymerWindow = 10;
	File referenceFile = new File(this.getClass().getResource("/resources/example.fa").getFile());
	IndexedFastaSequenceFile indexedFastaFile;

	final static String FILE_SEPERATOR = System.getProperty("file.separator");

	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setUp() {
		indexedFastaFile = QBasePileupUtil.getIndexedFastaFile(referenceFile);
	}
	
	@After
	public void tearDown() throws IOException {
		indexedFastaFile.close();
		indexedFastaFile = null;
	}

	@Test
	public void testGetReferenceSequenceWithInsertion() throws IOException {
		//setUpReferenceFile();
		Homopolymer hp = getHomopolymerObject("chr1", 28, 29, IndelPosition.INS, "T");
		
		hp.getReferenceSequence();
		
		assertEquals(hompolymerWindow, hp.getUpstreamReference().length);
		assertSequenceEquals("CCTTGCTGTA", hp.getUpstreamReference());
		
		assertEquals(hp.getPosition().getLength(), hp.getIndelReferenceBases().length);
		assertSequenceEquals("T", hp.getIndelReferenceBases());
		
		assertEquals(hompolymerWindow, hp.getDownstreamReference().length);
		assertSequenceEquals("TTTTATTCTG", hp.getDownstreamReference());
	}
	
	@Test
	public void testGetReferenceSequenceWithDeletion() throws IOException {
		//setUpReferenceFile();
		Homopolymer hp = getHomopolymerObject("chr1", 28, 30, IndelPosition.DEL, "T");		
		//make sure position is set up correctly
		assertPosition("chr1", 28, 30, IndelPosition.DEL, "T", hp);
		
		hp.getReferenceSequence();		
		assertSequenceEquals("CCTTGCTGTA", hp.getUpstreamReference());
		assertSequenceEquals("T", hp.getIndelReferenceBases());
		assertSequenceEquals("TTTATTCTGT", hp.getDownstreamReference());
	}
	
	@Test
	public void testNoHomopolmyerForComplex() {
		Homopolymer hp = getHomopolymerObject("chr1", 28, 30, IndelPosition.CTX, "T");
		hp.findHomopolymer();
		assertEquals("", hp.getType());
		assertEquals("", hp.getHomopolymerCount());
	}
	
	@Test
	public void testFindContiguousHomopolymerWithInsertion() {
		Homopolymer hp = getHomopolymerObject("chr1", 28, 29, IndelPosition.INS, "T");		
		hp.findHomopolymer();
		assertEquals("", hp.getUpType());
		assertEquals(hp.getUpBaseCount(), 1);
		assertEquals(Homopolymer.CONTIGUOUS, hp.getDownType());
		assertEquals(4, hp.getDownBaseCount());		
		assertEquals("4", hp.getHomopolymerCount());
		assertEquals(Homopolymer.CONTIGUOUS, hp.getType());
	}
	
	@Test
	public void testFindContigousHomopolymerWithDeletion() {
		Homopolymer hp = getHomopolymerObject("chr1", 28, 30, IndelPosition.DEL, "T");
		hp.findHomopolymer();
		assertEquals("", hp.getUpType());
		assertEquals(hp.getUpBaseCount(), 1);
		assertEquals(Homopolymer.CONTIGUOUS, hp.getDownType());
		assertEquals(3, hp.getDownBaseCount());		
		assertEquals("3", hp.getHomopolymerCount());
		assertEquals(Homopolymer.CONTIGUOUS, hp.getType());
	}
	
	@Test
	public void testFindDiscontiguousHomopolymerWithInsertion() {
		Homopolymer hp = getHomopolymerObject("chr1", 28, 29, IndelPosition.INS, "C");		
		hp.findHomopolymer();
		assertEquals("", hp.getUpType());
		assertEquals(hp.getUpBaseCount(), 1);
		assertEquals(Homopolymer.DISCONTIGUOUS, hp.getDownType());
		assertEquals(4, hp.getDownBaseCount());		
		assertEquals("4", hp.getHomopolymerCount());
		assertEquals(Homopolymer.DISCONTIGUOUS, hp.getType());
	}
	
	@Test
	public void testFindDiscontigousHomopolymerWithDeletion() {
		Homopolymer hp = getHomopolymerObject("chr1", 27, 29, IndelPosition.DEL, "T");
		hp.findHomopolymer();
		assertEquals("", hp.getUpType());
		assertEquals(hp.getUpBaseCount(), 1);
		assertEquals(Homopolymer.DISCONTIGUOUS, hp.getDownType());
		assertEquals(4, hp.getDownBaseCount());		
		assertEquals("4", hp.getHomopolymerCount());
		assertEquals(Homopolymer.DISCONTIGUOUS, hp.getType());
	}
	
	@Test
	public void testInHomopoylmerIsContiguous() {
		
		Homopolymer hp = getHomopolymerObject("chr1", 28, 29, IndelPosition.INS, "T");		
		boolean result = hp.isHomopolymer(Homopolymer.CONTIGUOUS, Homopolymer.CONTIGUOUS, Homopolymer.CONTIGUOUS, 2, 3);
		assertIsInHomopolymer(true, result, Homopolymer.CONTIGUOUS, "3", hp);
		
		hp = getHomopolymerObject("chr1", 28, 29, IndelPosition.INS, "T");		
		result = hp.isHomopolymer(Homopolymer.CONTIGUOUS, Homopolymer.CONTIGUOUS, Homopolymer.CONTIGUOUS, 5, 3);
		assertIsInHomopolymer(true, result, Homopolymer.CONTIGUOUS, "5", hp);
		
		hp = getHomopolymerObject("chr1", 28, 29, IndelPosition.INS, "T");		
		result = hp.isHomopolymer(Homopolymer.CONTIGUOUS, Homopolymer.DISCONTIGUOUS, Homopolymer.CONTIGUOUS, 5, 3);
		assertIsInHomopolymer(true, result, Homopolymer.CONTIGUOUS, "3", hp);
		
		hp = getHomopolymerObject("chr1", 28, 29, IndelPosition.INS, "T");		
		result = hp.isHomopolymer(Homopolymer.CONTIGUOUS, Homopolymer.CONTIGUOUS, Homopolymer.DISCONTIGUOUS, 5, 3);
		assertIsInHomopolymer(true, result, Homopolymer.CONTIGUOUS, "5", hp);
		
		hp = getHomopolymerObject("chr1", 28, 29, IndelPosition.INS, "T");		
		result = hp.isHomopolymer(Homopolymer.CONTIGUOUS, Homopolymer.DISCONTIGUOUS, Homopolymer.DISCONTIGUOUS, 5, 3);
		assertIsInHomopolymer(false, result, "", "", hp);
		
	}
	
	@Test
	public void testInHomopoylmerIsDiscontiguous() {
		
		Homopolymer hp = getHomopolymerObject("chr1", 28, 29, IndelPosition.INS, "T");		
		boolean result = hp.isHomopolymer(Homopolymer.DISCONTIGUOUS, Homopolymer.DISCONTIGUOUS, Homopolymer.DISCONTIGUOUS, 2, 3);
		assertIsInHomopolymer(true, result, Homopolymer.DISCONTIGUOUS, "3", hp);
		
		hp = getHomopolymerObject("chr1", 28, 29, IndelPosition.INS, "T");		
		result = hp.isHomopolymer(Homopolymer.DISCONTIGUOUS, Homopolymer.DISCONTIGUOUS, Homopolymer.DISCONTIGUOUS, 5, 3);
		assertIsInHomopolymer(true, result, Homopolymer.DISCONTIGUOUS, "5", hp);
		
		hp = getHomopolymerObject("chr1", 28, 29, IndelPosition.INS, "T");		
		result = hp.isHomopolymer(Homopolymer.DISCONTIGUOUS, Homopolymer.CONTIGUOUS, Homopolymer.DISCONTIGUOUS, 5, 3);
		assertIsInHomopolymer(true, result, Homopolymer.DISCONTIGUOUS, "3", hp);
		
		hp = getHomopolymerObject("chr1", 28, 29, IndelPosition.INS, "T");		
		result = hp.isHomopolymer(Homopolymer.DISCONTIGUOUS, Homopolymer.DISCONTIGUOUS, Homopolymer.CONTIGUOUS, 5, 3);
		assertIsInHomopolymer(true, result, Homopolymer.DISCONTIGUOUS, "5", hp);
		
		hp = getHomopolymerObject("chr1", 28, 29, IndelPosition.INS, "T");		
		result = hp.isHomopolymer(Homopolymer.DISCONTIGUOUS, Homopolymer.CONTIGUOUS, Homopolymer.CONTIGUOUS, 5, 3);
		assertIsInHomopolymer(false, result, "", "", hp);
		
	}	
	
	@Test
	public void testIsEmbeddedHomopolymer() {
		
		byte[] up = {65, 67, 84};
		byte[] down = {84, 84, 67, 65};
		//true
		Homopolymer hp = getHomopolymerObject("chr1", 28, 29, IndelPosition.INS, "T");
		hp.setUpstreamReference(up);
		hp.setDownstreamReference(down);
		hp.setIndelReferenceBases(hp.getPosition().getMotif());
		assertEquals(true, hp.isEmbeddedHomopolymer(2, Homopolymer.CONTIGUOUS, Homopolymer.CONTIGUOUS, 1, 2));
		assertEquals(hp.getType(), Homopolymer.EMBEDDED);
		assertEquals("3", hp.getHomopolymerCount());
		
		//false - 
		hp = getHomopolymerObject("chr1", 28, 29, IndelPosition.INS, "G");
		hp.setUpstreamReference(up);
		hp.setDownstreamReference(down);
		hp.setIndelReferenceBases(hp.getPosition().getMotif());
		assertEquals(false, hp.isEmbeddedHomopolymer(2, Homopolymer.CONTIGUOUS, Homopolymer.CONTIGUOUS, 1, 2));
		assertEquals(hp.getType(), "");
		assertEquals("", hp.getHomopolymerCount());
		
		//not contiguous
		hp = getHomopolymerObject("chr1", 28, 29, IndelPosition.INS, "G");
		hp.setUpstreamReference(up);
		hp.setDownstreamReference(down);
		hp.setIndelReferenceBases(hp.getPosition().getMotif());
		assertEquals(false, hp.isEmbeddedHomopolymer(2, Homopolymer.DISCONTIGUOUS, Homopolymer.DISCONTIGUOUS, 1, 2));
	}
	
	private void assertIsInHomopolymer(boolean expected, boolean result, String type, String count,
			Homopolymer hp) {
		assertEquals(expected, result);
		assertEquals(type, hp.getType());
		assertEquals(count, hp.getHomopolymerCount());		
	}

	public void assertPosition(String chr, int start, int end, String type,
			String base, Homopolymer hp) {
		assertEquals(chr, hp.getPosition().getChromosome());
		assertEquals(type, hp.getPosition().getMutationType());
		if (type.equals(IndelPosition.DEL)) {
			assertEquals(start+1, hp.getPosition().getStart());
			assertEquals(end-1, hp.getPosition().getEnd());
		}
		if (type.equals(IndelPosition.INS) || type.equals(IndelPosition.CTX)) {
			assertEquals(start, hp.getPosition().getStart());
			assertEquals(end, hp.getPosition().getEnd());
		}		
	}	

	private void assertSequenceEquals(String expected, byte[] sequenceArray) {
		assertEquals(expected, new String(sequenceArray));
		
	}

	private Homopolymer getHomopolymerObject(String chr, Integer start, Integer end, String mutationType, String indelBases) {
		IndelPosition p = new IndelPosition("1", chr, start, end, mutationType, "pindel", "", false);
		Homopolymer hp = new Homopolymer(p, indexedFastaFile, hompolymerWindow);
		p.setMotif(indelBases);
		
		return hp;
	}

}
