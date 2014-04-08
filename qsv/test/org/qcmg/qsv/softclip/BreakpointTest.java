package org.qcmg.qsv.softclip;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.assemble.ConsensusRead;
import org.qcmg.qsv.blat.BLAT;
import org.qcmg.qsv.blat.BLATRecord;
import org.qcmg.qsv.softclip.Breakpoint;
import org.qcmg.qsv.splitread.UnmappedRead;
import org.qcmg.qsv.util.QSVUtil;
import org.qcmg.qsv.util.TestUtil;
import static org.easymock.EasyMock.*;

public class BreakpointTest {
	
	Breakpoint breakpoint;
	NavigableMap<Integer, List<UnmappedRead>> splitReadsMap;
	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
		

	@Before
	public void setUp() throws Exception {
		splitReadsMap = null;
	}
	
	@Test
	public void testDefineBreakpointPassesFilterWithSomaticRight() throws Exception {
		breakpoint = TestUtil.getBreakpoint(false, false, 20, false);
		assertTrue(breakpoint.defineBreakpoint(3, false, null));
		assertFalse(breakpoint.isGermline());
		assertEquals("somatic", breakpoint.getType());
		assertEquals("-", breakpoint.getStrand());
		assertEquals("chr10_89700299_false_-", breakpoint.getName());
		assertEquals("CCCTGCCCTAAGAGCAGCAAATTGCTGAACTCCTCTGGTGGACCTCTTACACAAAGTATAATCTC", breakpoint.getMateConsensus());
	}
	
	@Test
	public void testDefineBreakpointPassesFilterWithGermlineLeft() throws Exception {
		breakpoint = TestUtil.getBreakpoint(true, true, 20, false);
		assertTrue(breakpoint.defineBreakpoint(3, false, null));
		assertTrue(breakpoint.isGermline());
		assertEquals("germline", breakpoint.getType());
		assertEquals("+", breakpoint.getStrand());
		assertEquals("chr10_89712341_true_+", breakpoint.getName());
		assertEquals("AAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTA", breakpoint.getMateConsensus());
	}
	
	@Test
	public void testDefineBreakpointNoPassesFilterClipSizeFilter() throws Exception {
		breakpoint = TestUtil.getBreakpoint(true, true, 20, false);
		assertFalse(breakpoint.defineBreakpoint(10, false, null));
	}
	
	@Test
	public void testDefineBreakpointNoPassesFilterConsensusFilter() throws Exception {
		breakpoint = TestUtil.getBreakpoint(true, true, 100, false);
		assertFalse(breakpoint.defineBreakpoint(3, false, null));
	}
	
	@Test
	public void testDefineBreakpointNoPassesHighNCountFilter() throws Exception {
		breakpoint = TestUtil.getBreakpoint(true, true, 20, true);
		assertFalse(breakpoint.defineBreakpoint(3, false, null));
	}
	
	@Test
	public void testFindMateBreakpointIsTrue() throws Exception {
		String value = "48\t1\t0\t0\t2\t0\t3\t0\t+\tchr10-89712341-true-pos\t66\t0\t48\tchr10\t135534747\t89700251\t89700299\t1\t48,\t0,\t89700251,";
		String[] values =value.split("\t");
		BLATRecord record = new BLATRecord(values);
		
		breakpoint = TestUtil.getBreakpoint(true, false, 20, false);
		assertTrue(breakpoint.findMateBreakpoint(record));
		assertEquals("+", breakpoint.getMateStrand());
		assertEquals("chr10", breakpoint.getMateReference());
		assertEquals(89700299, breakpoint.getMateBreakpoint());
		assertEquals("chr10:chr10", breakpoint.getReferenceKey());
	}
	
	@Test
	public void testFindMateBreakpointWithReordering() throws Exception {
		String value = "48\t1\t0\t0\t2\t0\t3\t0\t+\tchr10-89712341-true-pos\t66\t0\t48\tchr7\t135534747\t89700251\t89700299\t1\t48,\t0,\t89700251,";
		String[] values =value.split("\t");
		BLATRecord record = new BLATRecord(values);
		
		breakpoint = TestUtil.getBreakpoint(false, false, 20, false);
		assertTrue(breakpoint.findMateBreakpoint(record));
		assertEquals("chr7:chr10", breakpoint.getReferenceKey());
	}
	
	@Test
	public void calculateConsensus() {
		breakpoint = new Breakpoint();
		assertEquals("A", breakpoint.getBaseCountString(setUpBases(1,0,0,0,0)));
		assertEquals("C", breakpoint.getBaseCountString(setUpBases(0,1,0,0,0)));
		assertEquals("T", breakpoint.getBaseCountString(setUpBases(0,0,1,0,0)));
		assertEquals("G", breakpoint.getBaseCountString(setUpBases(0,0,0,1,0)));
		assertEquals("", breakpoint.getBaseCountString(setUpBases(0,0,0,0,1)));
		assertEquals("N", breakpoint.getBaseCountString(setUpBases(1,1,0,0,0)));
		assertEquals("A", breakpoint.getBaseCountString(setUpBases(2,1,0,0,0)));
	}
	
	@Test
	public void calculateStrand() {
		assertStrand("+", "+", false, 2,0);
		assertStrand("-", "-", false, 0, 2);
		assertStrand("+", "+", true, 4,0);
		assertStrand("-", "-", true, 0, 4);
	}
	
	@Test
	public void testCompare() {
		breakpoint = new Breakpoint();
		breakpoint.setMateBreakpoint(12345);
		breakpoint.setMateReference("chr1");
		assertNull(breakpoint.compare("chr2", 12345));
		assertNull(breakpoint.compare("chr1", 12356));
		assertEquals(new Integer(12350), breakpoint.compare("chr1", 12350));
	}
	
	@Test
	public void testFindRescuedMateBreakpoint() throws Exception {
		breakpoint = new Breakpoint();	
		HashSet<Clip> set = new HashSet<Clip>();
		set.add(new Clip("test,chr10,89712341,+,left,ACTTTGAAAAAACAGTAATTAA,ACTTTGAAAAAACAGTAATT,AA"));		
		set.add(new Clip("test2,chr10,89712341,+,left,ACTTTGAAAAAACAGTAATTAA,ACTTTGAAAAAACAGTAATT,AA"));		
		breakpoint.setTumourClips(set);	
		breakpoint.setName("chr10_89712341_true_+");
		breakpoint.setReference("chr10");
		breakpoint.setStrand("+");
		breakpoint.setBreakpoint(89712341);
		BLAT blat = createMock(BLAT.class);
		String softClipDir = testFolder.newFolder().getAbsolutePath();
		Map<String, BLATRecord> expected = new HashMap<String, BLATRecord>();
        String value = "48\t1\t0\t0\t2\t0\t3\t0\t-\tchr10_89712341_true_+\t66\t0\t48\tchr10\t135534747\t89700251\t89700299\t1\t48,\t0,\t89700251,";
	    expected.put("chr10_89712341_true_+", new BLATRecord(value));
		expect(blat.align(softClipDir + QSVUtil.getFileSeparator() +  ("chr10_89712341_true_+.fa"), softClipDir + QSVUtil.getFileSeparator() + "chr10_89712341_true_+.psl")).andReturn(expected);
		replay(blat);
        QSVParameters p = createMock(QSVParameters.class);             
        
        assertTrue(breakpoint.findRescuedMateBreakpoint(blat, p, softClipDir));
        assertEquals("-", breakpoint.getMateStrand());
        assertEquals(89700299, breakpoint.getMateBreakpoint());
        assertEquals("chr10", breakpoint.getMateReference());
	}

	private void assertStrand(String strand1, String strand2, boolean isGermline,
			int posStrandCount, int negStrandCount) {
		breakpoint = new Breakpoint();	
		HashSet<Clip> set = new HashSet<Clip>();
		set.add(new Clip("test,chr10,89712341,"+strand1+",left,ACTTTGAAAAAACAGTAATTAA,ACTTTGAAAAAACAGTAATT,AA"));		
		set.add(new Clip("test2,chr10,89712341,"+strand2+",left,ACTTTGAAAAAACAGTAATTAA,ACTTTGAAAAAACAGTAATT,AA"));		
		breakpoint.setTumourClips(set);		
		if (isGermline) {
			breakpoint.setGermline(true);
			set.clear();
			set.add(new Clip("test3,chr10,89712341,"+strand1+",left,ACTTTGAAAAAACAGTAATTAA,ACTTTGAAAAAACAGTAATT,AA"));		
			set.add(new Clip("test4,chr10,89712341,"+strand2+",left,ACTTTGAAAAAACAGTAATTAA,ACTTTGAAAAAACAGTAATT,AA"));				
			breakpoint.setNormalClips(set);		
		}
		breakpoint.calculateStrand();
		assertEquals(strand1, breakpoint.getStrand());
		assertEquals(posStrandCount, breakpoint.getPosStrandCount());
		assertEquals(negStrandCount, breakpoint.getNegStrandCount());
	}
	
	@Test
	public void testConsensusRead() throws Exception {
		breakpoint = new Breakpoint();
		breakpoint.setStrand("+");
		breakpoint.setConsensusRead(new ConsensusRead("test", "ACTTTGAAAAAACAGTAATTAA","ACTTTGAAAAAACAGTAATT","AA"));
		
		assertEquals("ACTTTGAAAAAACAGTAATTAA", breakpoint.getCompleteConsensus());
		assertEquals("ACTTTGAAAAAACAGTAATT", breakpoint.getMateConsensus());
		assertEquals("AA", breakpoint.getBreakpointConsenus());
		breakpoint.setStrand("-");
		assertEquals("TTAATTACTGTTTTTTCAAAGT", breakpoint.getCompleteConsensus());
		assertEquals("ACTTTGAAAAAACAGTAATT", breakpoint.getMateConsensus());
		assertEquals("TT", breakpoint.getBreakpointConsenus());
	}
	

	private int[][] setUpBases(int a, int b, int c, int d, int e) {
		int[][] bases = new int[1][5];
		bases[0][0] = a;
		bases[0][1] = b;
		bases[0][2] = c;
		bases[0][3] = d;
		bases[0][4] = e;
		return bases;
	}
	
}
