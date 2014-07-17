package org.qcmg.qsv.softclip;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.sf.samtools.SAMFileHeader.SortOrder;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.blat.BLAT;
import org.qcmg.qsv.blat.BLATRecord;
import org.qcmg.qsv.discordantpair.PairGroup;
import org.qcmg.qsv.util.TestUtil;

public class SoftClipClusterTest {

	SoftClipCluster clip;

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

	@Before
	public void setUp() throws Exception {

	}

	@After
	public void tearDown() {
		clip = null;
	}

	@Test
	public void testStartAndEndWith2Breakpoints() throws Exception {
		clip = TestUtil.setUpClipRecord("chr10", "chr10", false, false);
		assertEquals(new Integer(89700299), clip.getLeftBreakpointObject()
				.getBreakpoint());
		assertEquals(new Integer(89712341), clip.getRightBreakpointObject()
				.getBreakpoint());
		assertEquals(new Integer(89700299), clip.getLeftBreakpoint());
		assertEquals(new Integer(89712341), clip.getRightBreakpoint());
		assertEquals("chr10", clip.getLeftReference());
		assertEquals("chr10", clip.getRightReference());
	}

	@Test
	public void testStartAndEndWithRightBreakpoint() throws Exception {
		Breakpoint b = TestUtil.getBreakpoint(true, false, 20, false);
		b.setMateBreakpoint(89700299);
		b.setMateReference("chr10");
		b.setMateStrand("-");
		clip = new SoftClipCluster(b);

		assertEquals(new Integer(89712341), clip.getRightBreakpointObject()
				.getBreakpoint());
		assertNull(clip.getLeftBreakpointObject());
		assertEquals(new Integer(89700299), clip.getLeftBreakpoint());
		assertEquals(new Integer(89712341), clip.getRightBreakpoint());
		assertEquals("chr10", clip.getLeftReference());
		assertEquals("chr10", clip.getRightReference());
	}

	@Test
	public void testStartAndEndWithLeftBreakpoint() throws Exception {
		Breakpoint b = TestUtil.getBreakpoint(false, false, 20, false);
		b.setMateBreakpoint(89712341);
		b.setMateReference("chr10");
		b.setMateStrand("-");
		clip = new SoftClipCluster(b);

		assertEquals(new Integer(89700299), clip.getLeftBreakpointObject()
				.getBreakpoint());
		assertNull(clip.getRightBreakpointObject());
		assertEquals(new Integer(89700299), clip.getLeftBreakpoint());
		assertEquals(new Integer(89712341), clip.getRightBreakpoint());
		assertEquals("chr10", clip.getLeftReference());
		assertEquals("chr10", clip.getRightReference());
	}

	@Test
	public void testDefineMutationTypeAs2SidedDEL() throws Exception {
		clip = new SoftClipCluster(
				getBreakpoint("chr1", 1234, false, "+", "+"), getBreakpoint(
						"chr1", 12345, true, "+", "+"));
		assertFalse(clip.getLeftBreakpointObject().isLeft());
		assertTrue(clip.getRightBreakpointObject().isLeft());
		assertEquals("DEL/ITX", clip.defineMutationType());
	}

	@Test
	public void testDefineMutationTypeAs2SidedDUP() throws Exception {
		clip = new SoftClipCluster(getBreakpoint("chr1", 1234, true, "+", "+"),
				getBreakpoint("chr1", 12345, false, "+", "+"));
		assertTrue(clip.getLeftBreakpointObject().isLeft());
		assertFalse(clip.getRightBreakpointObject().isLeft());
		assertEquals("DUP/INS/ITX", clip.defineMutationType());
	}

	@Test
	public void testDefineMutationTypeAs2SidedINVLeft() throws Exception {
		clip = new SoftClipCluster(getBreakpoint("chr1", 1234, true, "+", "-"),
				getBreakpoint("chr1", 12345, true, "+", "-"));
		assertTrue(clip.getLeftBreakpointObject().isLeft());
		assertTrue(clip.getRightBreakpointObject().isLeft());
		assertEquals("INV/ITX", clip.defineMutationType());
	}

	@Test
	public void testDefineMutationTypeAs2SidedINVWithRight() throws Exception {
		clip = new SoftClipCluster(
				getBreakpoint("chr1", 1234, false, "+", "-"), getBreakpoint(
						"chr1", 12345, false, "+", "-"));
		assertFalse(clip.getLeftBreakpointObject().isLeft());
		assertFalse(clip.getRightBreakpointObject().isLeft());
		assertEquals("INV/ITX", clip.defineMutationType());
	}

	@Test
	public void testDefineMutationTypeAs2SidedITX() throws Exception {
		clip = new SoftClipCluster(
				getBreakpoint("chr1", 1234, false, "+", "+"), getBreakpoint(
						"chr1", 12345, false, "+", "+"));
		assertFalse(clip.getLeftBreakpointObject().isLeft());
		assertFalse(clip.getRightBreakpointObject().isLeft());
		assertEquals("ITX", clip.defineMutationType());
	}

	@Test
	public void testDefineMutationTypeAs2SidedCTX() throws Exception {
		clip = new SoftClipCluster(
				getBreakpoint("chr1", 1234, false, "+", "-"), getBreakpoint(
						"chr2", 12345, false, "+", "-"));
		assertEquals("CTX", clip.defineMutationType());
	}

	@Test
	public void testDefineMutationTypeAs1SidedDUP() throws Exception {
		clip = new SoftClipCluster(getSingleBreakpoint("chr1", 1234, 12345,
				true, "+", "+"));
		assertEquals("DUP/INS/ITX", clip.defineMutationType());

		clip = new SoftClipCluster(getSingleBreakpoint("chr1", 12345, 1234,
				false, "+", "+"));
		assertEquals("DUP/INS/ITX", clip.defineMutationType());
	}

	@Test
	public void testDefineMutationTypeAs1SidedDEL() throws Exception {
		clip = new SoftClipCluster(getSingleBreakpoint("chr1", 12345, 1234,
				true, "+", "+"));
		assertEquals("DEL/ITX", clip.defineMutationType());

		clip = new SoftClipCluster(getSingleBreakpoint("chr1", 1234, 12345,
				false, "+", "+"));
		assertEquals("DEL/ITX", clip.defineMutationType());
	}

	@Test
	public void testDefineMutationTypeAs1SidedITX() throws Exception {
		clip = new SoftClipCluster(getSingleBreakpoint("chr1", 12345, 1234,
				true, "+", "-"));
		assertEquals("ITX", clip.defineMutationType());
	}

	@Test
	public void testSwapBreakpoints() throws Exception {
		clip = new SoftClipCluster(
				getBreakpoint("chr1", 1234, false, "+", "+"), getBreakpoint(
						"chr2", 12345, false, "+", "+"));
		assertEquals(1234, clip.getLeftBreakpointObject().getBreakpoint()
				.intValue());
		assertEquals(12345, clip.getRightBreakpointObject().getBreakpoint()
				.intValue());
		clip.swapBreakpoints();
		assertEquals(12345, clip.getLeftBreakpointObject().getBreakpoint()
				.intValue());
		assertEquals(1234, clip.getRightBreakpointObject().getBreakpoint()
				.intValue());
	}

	@Test
	public void testFindMatchingBreakpointsIsTrue() throws Exception {
		clip = new SoftClipCluster(getSingleBreakpoint("chr1", 12345, 1234,
				true, "+", "-"));
		SoftClipCluster compareClip = new SoftClipCluster(getSingleBreakpoint(
				"chr1", 1234, 12345, true, "+", "-"));

		assertTrue(clip.findMatchingBreakpoints(compareClip));
	}

	@Test
	public void testFindMatchingBreakpointsIsFalse() throws Exception {
		clip = new SoftClipCluster(getSingleBreakpoint("chr1", 12345, 1234,
				true, "+", "-"));
		SoftClipCluster compareClip = new SoftClipCluster(getSingleBreakpoint(
				"chr1", 1245, 12345, true, "+", "-"));

		assertFalse(clip.findMatchingBreakpoints(compareClip));
	}

	@Test
	public void isGermlineWithSingleBreakpoint() throws Exception {
		clip = new SoftClipCluster(getSingleBreakpoint("chr1", 12345, 1234,
				true, "+", "-"));
		assertFalse(clip.isGermline());
		clip.getSingleBreakpoint().setGermline(true);
		assertTrue(clip.isGermline());
	}

	@Test
	public void isGermlineWithTwoBreakpoints() throws Exception {
		clip = new SoftClipCluster(
				getBreakpoint("chr1", 1234, false, "+", "+"), getBreakpoint(
						"chr2", 12345, false, "+", "+"));
		assertFalse(clip.isGermline());
		clip.getLeftBreakpointObject().setGermline(true);
		assertTrue(clip.isGermline());
	}

	@Test
	public void testGetClipCount() throws Exception {
		clip = TestUtil.setUpClipRecord("chr10", "chr10", true, false);

		// tumour
		assertEquals(6, clip.getClipCount(true, true));
		assertEquals(5, clip.getClipCount(true, false));

		// normal
		assertEquals(6, clip.getClipCount(false, true));
		assertEquals(5, clip.getClipCount(false, false));
	}

	@Test
	public void testRescueClips() throws Exception {
		String tumourFile = testFolder.newFile("tumor.bam").getAbsolutePath();
		TestUtil.createHiseqBamFile(tumourFile, PairGroup.AAC,
				SortOrder.coordinate);
		String normalFile = testFolder.newFile("normal.bam").getAbsolutePath();
		TestUtil.createHiseqBamFile(normalFile, PairGroup.AAC,
				SortOrder.coordinate);
		QSVParameters tumor = TestUtil.getQSVParameters(testFolder, tumourFile,
				normalFile, true, "both", "both");
		clip = TestUtil.setUpClipRecord("chr10", "chr10", true, true);

		assertNull(clip.getRightBreakpointObject());
		assertEquals(6, clip.getClipCount(true, true));
		assertEquals(0, clip.getClipCount(true, false));

		BLAT blat = createMock(BLAT.class);
		Map<String, BLATRecord> expected = new HashMap<String, BLATRecord>();
		String value = "48\t1\t0\t0\t2\t0\t3\t0\t+\tchr10_89712341_true_+\t66\t0\t48\tchr10\t135534747\t89700251\t89700299\t1\t48,\t0,\t89700251,";

		expected.put("chr10_89712341_true_+", new BLATRecord(value.split("\t")));
		expect(
				blat.align(testFolder.newFile("chr10_89712341_true_+.fa")
						.getAbsolutePath(),
						testFolder.newFile("chr10_89712341_true_+.psl")
								.getAbsolutePath())).andReturn(expected);
		replay(blat);

		clip.rescueClips(tumor, blat, new File(tumourFile),
				new File(normalFile), testFolder.getRoot().toString(), 20, 200,
				50);
	}

	@Test
	public void testGetOrphanBreakpoint() throws Exception {
		clip = new SoftClipCluster(getSingleBreakpoint("chr1", 12345, 1234,
				true, "+", "-"));
		assertEquals(1234, clip.getOrphanBreakpoint().intValue());

		clip = new SoftClipCluster(getSingleBreakpoint("chr1", 1234, 12345,
				true, "+", "+"));
		assertEquals(12345, clip.getOrphanBreakpoint().intValue());
	}

	public Breakpoint getSingleBreakpoint(String chr, int breakpoint,
			int mateBreakpoint, boolean isLeft, String strand, String mateStrand)
			throws Exception {
		Breakpoint b = new Breakpoint(breakpoint, chr, isLeft, 20, 50);
		HashSet<Clip> clips = new HashSet<Clip>();
		clips.add(new Clip(
				"HWI-ST1240:47:D12NAACXX:1:2307:8115:32717:20120608115535190,chr10,89700299,-,right,GCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAG,GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAG,GCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA"));
		b.setTumourClips(clips);
		b.setMateReference(chr);
		b.setMateBreakpoint(mateBreakpoint);
//		b.setName("chr1-" + breakpoint + "-" + isLeft);
		b.setStrand(strand);
		b.setMateStrand(mateStrand);
		return b;
	}

	public Breakpoint getBreakpoint(String chr, int breakpoint, boolean isLeft,
			String strand, String mateStrand) throws Exception {
		Breakpoint b = new Breakpoint(breakpoint, chr, isLeft, 20, 50);
		HashSet<Clip> clips = new HashSet<Clip>();
		clips.add(new Clip(
				"HWI-ST1240:47:D12NAACXX:1:2307:8115:32717:20120608115535190,chr10,89700299,-,right,GCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAG,GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAG,GCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA"));
		b.setTumourClips(clips);
//		b.setName("chr1-" + breakpoint + "-" + isLeft);
		b.setStrand(strand);
		b.setMateStrand(mateStrand);
		return b;
	}

}
