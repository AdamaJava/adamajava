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

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qbasepileup.InputBAM;
import org.qcmg.qbasepileup.Options;
import org.qcmg.qbasepileup.QBasePileupException;


public class IndelPositionPileupTest {
	
	int[] cols = {23, 34, 25, 10, 12}; 
	IndelPositionPileup p;	//somatic
	IndelPositionPileup pG;
	@Before
	public void setUp() throws QBasePileupException, IOException {
		String line = "date\ttumour\tind6\t2\tchr1\t3237948\t3237949\t1\t-999\t-999\tG\t-999\tGTC\tG/GTC\t\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t-999\tPASS;\t--\t--\t--\t--";
		IndelPosition positionSomatic  = new IndelPosition(line, false, "pindel", cols);
		IndelPosition positionGermline = new IndelPosition(line, true, "pindel", cols);
		Options optionS = createMockOptions();
		Options optionG = createMockOptions();
		IndexedFastaSequenceFile indexedFasta = createMockIndexedFasta();
		p = new IndelPositionPileup(new InputBAM(testFolder.newFile("tumour.bam")), new InputBAM(testFolder.newFile("normal.bam")), positionSomatic, optionS, indexedFasta);
		pG = new IndelPositionPileup(new InputBAM(testFolder.newFile("tumour.bam")), new InputBAM(testFolder.newFile("normal.bam")), positionGermline, optionG, indexedFasta);

	}
	
	@After
	public void tearDown() {
		p = null;
	}
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void testToDCCString() throws QBasePileupException, IOException {
		String expected = "date\ttumour\tind6\t2\tchr1\t3237948\t3237949\t1\t-999\t-999\tG\t-999\tGTC\tG/GTC\t\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t-999\tPASS\t--\t0;0;0;0[0|0];0;0;0\t--\t--\t\n";
		assertEquals(expected, p.toDCCString());
		p.setPileupFlags("MIN");
		expected = "date\ttumour\tind6\t2\tchr1\t3237948\t3237949\t1\t-999\t-999\tG\t-999\tGTC\tG/GTC\t\t-999\t-999\t-999\t-999\t-999\t-999\t-999\t-999\tPASS;MIN\t--\t0;0;0;0[0|0];0;0;0\t--\t--\t\n";
		assertEquals(expected, p.toDCCString());
	}
	
	@Test
	public void testCalculateQCMGFlagInSomatic() throws QBasePileupException, IOException {		
		assertEquals("NNS;COVN12", p.calculatePileupFlags());
		assertTrue(p.getPosition().isSomatic());
		assertFalse(p.getPosition().isGermline());
		Set<Integer> nStarts = new HashSet<Integer>();
		nStarts.add(1);
		p.getNormalPileup().setNovelStarts(nStarts);
		p.getNormalPileup().setTotalReads(10);
		p.getNormalPileup().setInformativeReads(1);
		assertEquals("NNS;MIN;COVN12", p.calculatePileupFlags());
	}	
	
	@Test
	public void testCalculateQCMGFlagInGermline() throws QBasePileupException, IOException {
//		p.getPosition().setGermline(true);
		assertTrue(pG.getPosition().isGermline());
		assertFalse(pG.getPosition().isSomatic());
		assertEquals("COVT;COVN8", pG.calculatePileupFlags());
	
		pG.getNormalPileup().setTotalReads(1001);
		pG.getNormalPileup().setHighCoverage(true);
		assertEquals("COVT;HCOVN", pG.calculatePileupFlags());
	}
	
	@Test
	public void testCalculateQCMGFlagForSBias() throws QBasePileupException, IOException {
		//tbias
		p.getTumourPileup().setTotalReads(50);
		p.getTumourPileup().setInformativeReads(40);
		p.getTumourPileup().setSupportingReads(30);
		p.getTumourPileup().setForwardSupportingReads(0);
		p.getTumourPileup().setSupportingReads(30);
//		p.getPosition().setGermline(false);
		assertEquals("NNS;COVN12;TBIAS", p.calculatePileupFlags());
		//nbias
		pG.getTumourPileup().setTotalReads(50);
		pG.getTumourPileup().setInformativeReads(40);
		pG.getTumourPileup().setSupportingReads(30);
		pG.getTumourPileup().setForwardSupportingReads(0);
		pG.getTumourPileup().setSupportingReads(30);
		pG.getNormalPileup().setTotalReads(50);
		pG.getNormalPileup().setInformativeReads(40);
		pG.getNormalPileup().setSupportingReads(30);
		pG.getNormalPileup().setForwardSupportingReads(0);
		pG.getNormalPileup().setSupportingReads(30);
//		p.getPosition().setGermline(true);
		assertEquals("NBIAS", pG.calculatePileupFlags());
	}
	
	@Test
	public void testCalculateQCMGFlagForPart() throws QBasePileupException, IOException {
		//tbias
		p.getTumourPileup().setTotalReads(29);
		p.getTumourPileup().setPartialIndelCount(4);
		assertEquals("NNS;COVN12;TPART", p.calculatePileupFlags());
		
		p.getNormalPileup().setTotalReads(29);
		p.getNormalPileup().setPartialIndelCount(4);
		assertEquals("NNS;NPART;TPART", p.calculatePileupFlags());
		
	}
	
	@Test
	public void testCalculateQCMGFlagWithHomopolymer() {
		Homopolymer hp = new Homopolymer();
		p.getTumourPileup().setHomopolymer(hp);
		assertEquals("NNS;COVN12", p.calculatePileupFlags());
		
		hp.setType(Homopolymer.CONTIGUOUS);
		hp.setHomopolymerCount("10");
		hp.setSequence("TTTTTTTTTTTTTTTTTTTTtAGTC");		
		assertEquals("NNS;COVN12;HOMCON_10", p.calculatePileupFlags());
		
		p.getTumourPileup().getHomopolymer().setType(Homopolymer.DISCONTIGUOUS);
		hp.setHomopolymerCount("10");
		hp.setSequence("TTTTTTTTTTTTTTTTTTTTtAGTC");		
		assertEquals("NNS;COVN12;HOMADJ_10", p.calculatePileupFlags());
		
		p.getTumourPileup().getHomopolymer().setType(Homopolymer.EMBEDDED);
		hp.setHomopolymerCount("10");
		hp.setSequence("TTTTTTTTTTTTTTTTTTTTtAGTC");		
		assertEquals("NNS;COVN12;HOMEMB_10", p.calculatePileupFlags());
	}
	
	
	private Options createMockOptions() throws IOException {
		Options options = createMock(Options.class);
		expect(options.getSoftClipWindow()).andReturn(new Integer(13));
		expect(options.getSoftClipWindow()).andReturn(new Integer(13));
		expect(options.getNearbyHomopolymerWindow()).andReturn(new Integer(10));
		expect(options.getNearbyHomopolymerWindow()).andReturn(new Integer(10));
		expect(options.getNearbyIndelWindow()).andReturn(new Integer(3));
		expect(options.getNearbyIndelWindow()).andReturn(new Integer(3));
		expect(options.getReference()).andReturn(testFolder.newFile("reference.fa"));
		expect(options.getReference()).andReturn(testFolder.newFile("reference.fa"));
		replay(options);
		return options;
	}
	
	private IndexedFastaSequenceFile createMockIndexedFasta() {
		IndexedFastaSequenceFile indexedFasta = createMock(IndexedFastaSequenceFile.class);
		byte[] bytes = new byte[1];
		bytes[0] = 1;
		expect(indexedFasta.getSubsequenceAt("chr7", 140188962, 140188962)).andReturn(new ReferenceSequence("test", 1234, "C".getBytes()));
		replay(indexedFasta);
		return indexedFasta;
	}

}
