package org.qcmg.qsv.softclip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.util.QSVUtil;
import org.qcmg.qsv.util.TestUtil;

public class SoftClipClusterTest {

	SoftClipCluster clip;

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();


	@Test
	public void testStartAndEndWith2Breakpoints() throws Exception {
		clip = TestUtil.setUpClipRecord("chr10", "chr10", false, false);
		assertEquals(Integer.valueOf(89700299), clip.getLeftBreakpointObject()
				.getBreakpoint());
		assertEquals(Integer.valueOf(89712341), clip.getRightBreakpointObject()
				.getBreakpoint());
		assertEquals(Integer.valueOf(89700299), clip.getLeftBreakpoint());
		assertEquals(Integer.valueOf(89712341), clip.getRightBreakpoint());
		assertEquals("chr10", clip.getLeftReference());
		assertEquals("chr10", clip.getRightReference());
	}

	@Test
	public void testStartAndEndWithRightBreakpoint() throws Exception {
		Breakpoint b = TestUtil.getBreakpoint(true, false, 20, false);
		b.setMateBreakpoint(89700299);
		b.setMateReference("chr10");
		b.setMateStrand(QSVUtil.MINUS);
		clip = new SoftClipCluster(b);

		assertEquals(Integer.valueOf(89712341), clip.getRightBreakpointObject()
				.getBreakpoint());
		assertNull(clip.getLeftBreakpointObject());
		assertEquals(Integer.valueOf(89700299), clip.getLeftBreakpoint());
		assertEquals(Integer.valueOf(89712341), clip.getRightBreakpoint());
		assertEquals("chr10", clip.getLeftReference());
		assertEquals("chr10", clip.getRightReference());
	}

	@Test
	public void testStartAndEndWithLeftBreakpoint() throws Exception {
		Breakpoint b = TestUtil.getBreakpoint(false, false, 20, false);
		b.setMateBreakpoint(89712341);
		b.setMateReference("chr10");
		b.setMateStrand(QSVUtil.MINUS);
		clip = new SoftClipCluster(b);

		assertEquals(Integer.valueOf(89700299), clip.getLeftBreakpointObject()
				.getBreakpoint());
		assertNull(clip.getRightBreakpointObject());
		assertEquals(Integer.valueOf(89700299), clip.getLeftBreakpoint());
		assertEquals(Integer.valueOf(89712341), clip.getRightBreakpoint());
		assertEquals("chr10", clip.getLeftReference());
		assertEquals("chr10", clip.getRightReference());
	}

	@Test
	public void testDefineMutationTypeAs2SidedDEL() throws Exception {
		clip = new SoftClipCluster(
				getBreakpoint("chr1", 1234, false, QSVUtil.PLUS, QSVUtil.PLUS), getBreakpoint(
						"chr1", 12345, true, QSVUtil.PLUS, QSVUtil.PLUS));
		assertFalse(clip.getLeftBreakpointObject().isLeft());
		assertTrue(clip.getRightBreakpointObject().isLeft());
		assertEquals("DEL/ITX", clip.defineMutationType());
	}

	@Test
	public void testDefineMutationTypeAs2SidedDUP() throws Exception {
		clip = new SoftClipCluster(getBreakpoint("chr1", 1234, true, QSVUtil.PLUS, QSVUtil.PLUS),
				getBreakpoint("chr1", 12345, false, QSVUtil.PLUS, QSVUtil.PLUS));
		assertTrue(clip.getLeftBreakpointObject().isLeft());
		assertFalse(clip.getRightBreakpointObject().isLeft());
		assertEquals("DUP/INS/ITX", clip.defineMutationType());
	}

	@Test
	public void testDefineMutationTypeAs2SidedINVLeft() throws Exception {
		clip = new SoftClipCluster(getBreakpoint("chr1", 1234, true, QSVUtil.PLUS, QSVUtil.MINUS),
				getBreakpoint("chr1", 12345, true, QSVUtil.PLUS, QSVUtil.MINUS));
		assertTrue(clip.getLeftBreakpointObject().isLeft());
		assertTrue(clip.getRightBreakpointObject().isLeft());
		assertEquals("INV/ITX", clip.defineMutationType());
	}

	@Test
	public void testDefineMutationTypeAs2SidedINVWithRight() throws Exception {
		clip = new SoftClipCluster(
				getBreakpoint("chr1", 1234, false, QSVUtil.PLUS, QSVUtil.MINUS), getBreakpoint(
						"chr1", 12345, false, QSVUtil.PLUS, QSVUtil.MINUS));
		assertFalse(clip.getLeftBreakpointObject().isLeft());
		assertFalse(clip.getRightBreakpointObject().isLeft());
		assertEquals("INV/ITX", clip.defineMutationType());
	}

	@Test
	public void testDefineMutationTypeAs2SidedITX() throws Exception {
		clip = new SoftClipCluster(
				getBreakpoint("chr1", 1234, false, QSVUtil.PLUS, QSVUtil.PLUS), getBreakpoint(
						"chr1", 12345, false, QSVUtil.PLUS, QSVUtil.PLUS));
		assertFalse(clip.getLeftBreakpointObject().isLeft());
		assertFalse(clip.getRightBreakpointObject().isLeft());
		assertEquals("ITX", clip.defineMutationType());
	}

	@Test
	public void testDefineMutationTypeAs2SidedCTX() throws Exception {
		clip = new SoftClipCluster(
				getBreakpoint("chr1", 1234, false, QSVUtil.PLUS, QSVUtil.MINUS), getBreakpoint(
						"chr2", 12345, false, QSVUtil.PLUS, QSVUtil.MINUS));
		assertEquals("CTX", clip.defineMutationType());
	}

	@Test
	public void testDefineMutationTypeAs1SidedDUP() throws Exception {
		clip = new SoftClipCluster(getSingleBreakpoint("chr1", 1234, 12345,
				true, QSVUtil.PLUS, QSVUtil.PLUS));
		assertEquals("DUP/INS/ITX", clip.defineMutationType());

		clip = new SoftClipCluster(getSingleBreakpoint("chr1", 12345, 1234,
				false, QSVUtil.PLUS, QSVUtil.PLUS));
		assertEquals("DUP/INS/ITX", clip.defineMutationType());
	}

	@Test
	public void testDefineMutationTypeAs1SidedDEL() throws Exception {
		clip = new SoftClipCluster(getSingleBreakpoint("chr1", 12345, 1234,
				true, QSVUtil.PLUS, QSVUtil.PLUS));
		assertEquals("DEL/ITX", clip.defineMutationType());

		clip = new SoftClipCluster(getSingleBreakpoint("chr1", 1234, 12345,
				false, QSVUtil.PLUS, QSVUtil.PLUS));
		assertEquals("DEL/ITX", clip.defineMutationType());
	}

	@Test
	public void testDefineMutationTypeAs1SidedITX() throws Exception {
		clip = new SoftClipCluster(getSingleBreakpoint("chr1", 12345, 1234,
				true, QSVUtil.PLUS, QSVUtil.MINUS));
		assertEquals("ITX", clip.defineMutationType());
	}

	@Test
	public void testSwapBreakpoints() throws Exception {
		clip = new SoftClipCluster(
				getBreakpoint("chr1", 1234, false, QSVUtil.PLUS, QSVUtil.PLUS), getBreakpoint(
						"chr2", 12345, false, QSVUtil.PLUS, QSVUtil.PLUS));
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
				true, QSVUtil.PLUS, QSVUtil.MINUS));
		SoftClipCluster compareClip = new SoftClipCluster(getSingleBreakpoint(
				"chr1", 1234, 12345, true, QSVUtil.PLUS, QSVUtil.MINUS));

		assertTrue(clip.findMatchingBreakpoints(compareClip));
	}

	@Test
	public void testFindMatchingBreakpointsIsFalse() throws Exception {
		clip = new SoftClipCluster(getSingleBreakpoint("chr1", 12345, 1234,
				true, QSVUtil.PLUS, QSVUtil.MINUS));
		SoftClipCluster compareClip = new SoftClipCluster(getSingleBreakpoint(
				"chr1", 1245, 12345, true, QSVUtil.PLUS, QSVUtil.MINUS));

		assertFalse(clip.findMatchingBreakpoints(compareClip));
	}

	@Test
	public void isGermlineWithSingleBreakpoint() throws Exception {
		clip = new SoftClipCluster(getSingleBreakpoint("chr1", 12345, 1234,
				true, QSVUtil.PLUS, QSVUtil.MINUS));
		assertFalse(clip.isGermline());
		clip.getSingleBreakpoint().setGermline(true);
		assertTrue(clip.isGermline());
	}

	@Test
	public void isGermlineWithTwoBreakpoints() throws Exception {
		clip = new SoftClipCluster(
				getBreakpoint("chr1", 1234, false, QSVUtil.PLUS, QSVUtil.PLUS), getBreakpoint(
						"chr2", 12345, false, QSVUtil.PLUS, QSVUtil.PLUS));
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
	public void testGetOrphanBreakpoint() throws Exception {
		clip = new SoftClipCluster(getSingleBreakpoint("chr1", 12345, 1234,
				true, QSVUtil.PLUS, QSVUtil.MINUS));
		assertEquals(1234, clip.getOrphanBreakpoint().intValue());

		clip = new SoftClipCluster(getSingleBreakpoint("chr1", 1234, 12345,
				true, QSVUtil.PLUS, QSVUtil.PLUS));
		assertEquals(12345, clip.getOrphanBreakpoint().intValue());
	}

	public Breakpoint getSingleBreakpoint(String chr, int breakpoint,
			int mateBreakpoint, boolean isLeft, char strand, char mateStrand)
			throws Exception {
		Breakpoint b = new Breakpoint(breakpoint, chr, isLeft, 20, 50);
		HashSet<Clip> clips = new HashSet<>();
		clips.add(new Clip(
				"HWI-ST1240:47:D12NAACXX:1:2307:8115:32717:20120608115535190,chr10,89700299,-,right,GCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAG,GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAG,GCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA"));
		for (Clip c : clips) {
			b.addTumourClip(c);
		}
		b.setMateReference(chr);
		b.setMateBreakpoint(mateBreakpoint);
		b.setStrand(strand);
		b.setMateStrand(mateStrand);
		return b;
	}

	public Breakpoint getBreakpoint(String chr, int breakpoint, boolean isLeft,
			char strand, char mateStrand) throws Exception {
		Breakpoint b = new Breakpoint(breakpoint, chr, isLeft, 20, 50);
		HashSet<Clip> clips = new HashSet<>();
		clips.add(new Clip(
				"HWI-ST1240:47:D12NAACXX:1:2307:8115:32717:20120608115535190,chr10,89700299,-,right,GCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAG,GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAG,GCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA"));
		for (Clip c : clips) {
			b.addTumourClip(c);
		}
		b.setStrand(strand);
		b.setMateStrand(mateStrand);
		return b;
	}

}
