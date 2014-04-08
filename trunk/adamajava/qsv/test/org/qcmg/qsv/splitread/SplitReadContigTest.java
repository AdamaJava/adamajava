package org.qcmg.qsv.splitread;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.picard.reference.IndexedFastaSequenceFile;
import net.sf.picard.reference.ReferenceSequence;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.Chromosome;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.blat.BLAT;
import org.qcmg.qsv.blat.BLATRecord;
import org.qcmg.qsv.util.QSVConstants;


public class SplitReadContigTest {
	
	BLAT blat;
	QSVParameters p;
	SplitReadContig splitReadContig;
	List<BLATRecord> records;
	SplitReadAlignment left;
	SplitReadAlignment right;
	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

	@Before
	public void setUp() throws Exception {
		records = new ArrayList<BLATRecord>();
		
	}

	@After
	public void tearDown() throws Exception {
		blat = null;
		p = null;
		splitReadContig = null;
		records = null;
		left = null;
		right = null;
	}
	
	@Test
	public void testLeftLower() throws UnsupportedEncodingException, QSVException {
		createStandardObject(1);
		splitReadContig.setSplitReadAlignments(left, right);
		assertTrue(splitReadContig.leftLower());
		splitReadContig.setSplitReadAlignments(right, left);
		assertFalse(splitReadContig.leftLower());
	}
	
	@Test
	public void testGetName() {
		createStandardObject(1);
		assertTrue(splitReadContig.getName().startsWith("splitcon_chr10_89700299_chr10_89712341_1_false"));
	}
	
	@Test
	public void testReorder() {
		createStandardObject(1);
		splitReadContig.setSplitReadAlignments(right, left);
		assertTrue(splitReadContig.getLeft().getStartPos() > splitReadContig.getRight().getStartPos());
		
		assertCorrectSplitReadAlignmentOrder(null);
		
		left = new SplitReadAlignment("chr15", "+", 89700210, 89700299, 1, 90);
		splitReadContig.setSplitReadAlignments(left, right);
		assertEquals("chr15", splitReadContig.getLeft().getReference());
		assertCorrectSplitReadAlignmentOrder("chr10");		
	}
	
	private void assertCorrectSplitReadAlignmentOrder(String chr) {
		createStandardObject(1);
		splitReadContig.reorder();			
		if (chr != null) {
			assertEquals(chr, splitReadContig.getLeft().getReference());
		} else {
			assertTrue(splitReadContig.getLeft().getStartPos() < splitReadContig.getRight().getStartPos());			
		}
	}

	@Test
	public void testCalculateMicrohomologyIsAbsent() throws QSVException, IOException {
		createStandardObject(1);
		splitReadContig.setNonTemplateSequence(null);	
		splitReadContig.calculateMicrohomology();
		assertEquals(QSVConstants.UNTESTED, splitReadContig.getMicrohomology());
		
		splitReadContig.setNonTemplateSequence("ACTG");
		splitReadContig.calculateMicrohomology();
		assertEquals(QSVConstants.NOT_FOUND, splitReadContig.getMicrohomology());
	}
	
	@Test
	public void testCalculateMicrohomology() throws QSVException, IOException {
		createStandardObject(1);
		splitReadContig.setFindMH(true);
		splitReadContig.setNonTemplateSequence(QSVConstants.NOT_FOUND);
		splitReadContig.setLeftSequence("CCAATGGAAACCGGACACAGCAGTGGGGTGGACGCATCAGGGTATAAATGACCCTGTCTCCTTTGCTCTGTGTACTCTCGTGGCAA");
		splitReadContig.setRightSequence("CAACAGTTATTTAAAAATGTTCATCATCACTAATCATGAAAGAAATGCAAAGCAAAA");
		splitReadContig.calculateMicrohomology();
		assertEquals("CAA", splitReadContig.getMicrohomology());
		
		splitReadContig.setLeftSequence("TAAATGACCCTGTCTCCTTTGCTCTGTGTACTCTCGTGGC");
		splitReadContig.setRightSequence("AACAGTTATTTAAAAATGTTCATCATCACTAATCATGAAAAAA");
		splitReadContig.calculateMicrohomology();
		assertEquals(QSVConstants.NOT_FOUND, splitReadContig.getMicrohomology());
	}

//	@Test
//	public void testRecheckMicrohomologyWithSingleAlignment() throws UnsupportedEncodingException, QSVException {
//		splitReadContig.setFindMH(false);
//		List<Chromosome> list = new ArrayList<Chromosome>();
//		list.add(new Chromosome("chr1", 72235502));
//		Map<String, List<Chromosome>> map= new HashMap<String, List<Chromosome>>();
//		map.put("chr1", list);
//		splitReadContig.setChromosomes(map);
//		splitReadContig.setSplitreadSV(new StructuralVariant("chr1", "chr1", 72219748, 72234284, "1"));
//		IndexedFastaSequenceFile f = createMock(IndexedFastaSequenceFile.class);	       
//		expect(f.getSubsequenceAt("chr1", 72219749, 72219799)).andReturn(new ReferenceSequence("test", 72219749, new String("CTATCAAAAGAGGA").getBytes()));
//		expect(f.getSubsequenceAt("chr1", 72234233, 72234283)).andReturn(new ReferenceSequence("test", 72219048, new String("GAACTTTGGA").getBytes()));
//		replay(f);
//		assertEquals(new Integer(72219748), splitReadContig.getLeftBreakpoint());
//		assertEquals(new Integer(72234284), splitReadContig.getRightBreakpoint());
//		
//
//		splitReadContig.setLeftSequence("CAGAACTAAGAGAGAGACAGAGGGTA");
//		splitReadContig.setRightSequence("CTTTTAACTTAATGCTGCAATAAGG");
//		splitReadContig.recheckMicrohomologyForSingleAlignment();
//		assertEquals("ACT", splitReadContig.getMicrohomology());
//		assertEquals(new Integer(72219750), splitReadContig.getLeftBreakpoint());
//		assertEquals(new Integer(72234283), splitReadContig.getRightBreakpoint());
//	
//	}
	
	@Test
	public void testDetermineSplitReadPotential() {
		createStandardObject(1);
		splitReadContig.setSplitreadSV(new StructuralVariant("chr10", "chr10", 89700299, 89712341, QSVConstants.ORIENTATION_1));
		splitReadContig.setConfidenceLevel(QSVConstants.LEVEL_HIGH);
		splitReadContig.determineSplitReadPotential();
		assertTrue(splitReadContig.getIsPotentialSplitRead());
	}
	
	@Test
	public void testNeedToReverseComplement() {	
		createStandardObject(1);
		left = new SplitReadAlignment("chr10", "-", 89700210, 89700299, 1, 90);
		right = new SplitReadAlignment("chr10", "-", 89712341, 89712514, 109, 282);
		splitReadContig.setSplitReadAlignments(left, right);
		assertTrue(splitReadContig.needToReverseComplement(QSVConstants.ORIENTATION_1));
	
		left = new SplitReadAlignment("chr10", "+", 89700210, 89700299, 1, 90);
		right = new SplitReadAlignment("chr10", "-", 89712341, 89712514, 109, 282);
		splitReadContig.setSplitReadAlignments(left, right);
		assertTrue(splitReadContig.needToReverseComplement(QSVConstants.ORIENTATION_4));
	}	
	
	@Test
	public void testCat1and2Nontemplate() {
		createStandardObject(1);
		splitReadContig.setCat1and2NonTemplate();
		assertEquals("GAGATTATACTTTGTGTA", splitReadContig.getNonTemplateSequence());
	
		left = new SplitReadAlignment("chr10", "-", 89700210, 89700299,109, 282);
		right = new SplitReadAlignment("chr10", "-", 89712341, 89712514,  1, 90);
		splitReadContig.setSplitReadAlignments(left, right);
		splitReadContig.setCat1and2NonTemplate();
		assertEquals("GAGATTATACTTTGTGTA", splitReadContig.getNonTemplateSequence());	
	}
	
	@Test
	public void testCat3and4Nontemplate() {
		createStandardObject(1);
		right = new SplitReadAlignment("chr10", "+", 89712341, 89712514,  1, 90);
		left = new SplitReadAlignment("chr10", "-", 89700210, 89700299,109, 282);
		assertCat3and4NonTemplate(left, right, "3");
		
		right = new SplitReadAlignment("chr10", "-", 89712341, 89712514,  1, 90);
		left = new SplitReadAlignment("chr10", "+", 89700210, 89700299,109, 282);
		assertCat3and4NonTemplate(left, right, "4");
		
		left = new SplitReadAlignment("chr10", "-", 89712341, 89712514,  109, 282);
		right = new SplitReadAlignment("chr10", "+", 89700210, 89700299,1, 90);
		assertCat3and4NonTemplate(left, right, "3");
		
		left = new SplitReadAlignment("chr10", "+", 89712341, 89712514,  109, 282);
		right = new SplitReadAlignment("chr10", "-", 89700210, 89700299,1, 90);
		assertCat3and4NonTemplate(left, right, "4");		
	}
	
	private void assertCat3and4NonTemplate(SplitReadAlignment left,
			SplitReadAlignment right, String cat) {
		createStandardObject(1);
		splitReadContig.setSplitReadAlignments(left, right);
		splitReadContig.setSplitreadSV(new StructuralVariant("chr10", "chr10", 89700299, 89712341, cat));
		splitReadContig.setCat3and4NonTemplate();
		assertEquals(18, splitReadContig.getNonTemplateSequence().length());
		assertEquals("GAGATTATACTTTGTGTA", splitReadContig.getNonTemplateSequence());	
	}

	@Test
	public void testPassesBreakpointFilter() {
		createStandardObject(1);
		splitReadContig.setConfidenceLevel(QSVConstants.LEVEL_HIGH);
		assertTrue(splitReadContig.passesBreakpointFilter(left, right));
		
		splitReadContig.setConfidenceLevel(QSVConstants.LEVEL_SINGLE_CLIP);
		left = new SplitReadAlignment("chr10", "+", 89700210, 89719299, 1, 90);
		right = new SplitReadAlignment("chr10", "-", 89712341, 89712514, 109, 282);
		assertTrue(splitReadContig.passesBreakpointFilter(left, right));
		splitReadContig.setConfidenceLevel(QSVConstants.LEVEL_HIGH);
		assertFalse(splitReadContig.passesBreakpointFilter(left, right));		
	}	
	
	@Test
	public void testQueryStringPositionFilter() {
		createStandardObject(1);
		assertTrue(splitReadContig.passesQueryPositionFilter(right, left));
		left.setQueryStart(1);
		left.setQueryEnd(200);
		right.setQueryStart(20);
		right.setQueryEnd(232);
		assertFalse(splitReadContig.passesQueryPositionFilter(left, right));
		left.setQueryStart(1);
		left.setQueryEnd(120);
		right.setQueryStart(110);
		right.setQueryEnd(232);
		assertTrue(splitReadContig.passesQueryPositionFilter(left, right));
	}
	
	@Test
	public void testQueryLengthFilter() {
		createStandardObject(1);
		assertTrue(splitReadContig.queryLengthFilter(right, left));
		left.setQueryStart(1);
		left.setQueryEnd(10);
		assertFalse(splitReadContig.queryLengthFilter(left, right));
		left.setQueryStart(0);
		left.setQueryEnd(290);
		assertFalse(splitReadContig.queryLengthFilter(left, right));
		left.setQueryStart(1);
		left.setQueryEnd(90);
		right.setQueryStart(1);
		right.setQueryEnd(200);
		assertFalse(splitReadContig.queryLengthFilter(left, right));
		assertFalse(splitReadContig.queryLengthFilter(right, left));	
	}
	
	@Test
	public void testPassesSizeFilter() {
		createStandardObject(1);
		assertFalse(splitReadContig.passesSizeFilter(new SplitReadAlignment("chr10", "+", 2970200, 2970220, 1, 19)));
		assertFalse(splitReadContig.passesSizeFilter(new SplitReadAlignment("chr10", "+", 2970200, 2970220, 1, 270)));
		assertTrue(splitReadContig.passesSizeFilter(new SplitReadAlignment("chr10", "+", 2970200, 2970220, 1, 100)));
	}
	
	@Test
	public void testPassesBreakpointFilterSingleAlignment() {
		createStandardObject(1);
		splitReadContig.setConfidenceLevel(QSVConstants.LEVEL_MID);
		assertTrue(splitReadContig.passesBreakpointFilter(left));
		assertTrue(splitReadContig.passesBreakpointFilter(right));
		right.setStartPos(100);
		right.setEndPos(200);
		assertFalse(splitReadContig.passesBreakpointFilter(right));
	}
	
	@Test
	public void testCheckBlockSize() throws QSVException {
		createStandardObject(1);
		assertFalse(splitReadContig.checkBlockSize(new BLATRecord("187\t5\t0\t0\t2\t61\t1\t17709\t-\tsplitcon_chr22_21353096_chr22_21566646_3_true\t253\t0\t253\tchr22\t51304566\t25002854\t25020755\t3\t17,10,165,\t0,22,88,\t25002854,25002871,25020590,\t")));
		assertTrue(splitReadContig.checkBlockSize(new BLATRecord("187\t5\t0\t0\t2\t61\t1\t17709\t-\tsplitcon_chr22_21353096_chr22_21566646_3_true\t253\t0\t253\tchr22\t51304566\t25002854\t25020755\t2\t270,165,\t22,88,\t25002854,25020590,\t")));
	}
	
	@Test
	public void testGetMatch() {
		createStandardObject(1);
		assertTrue(splitReadContig.getMatch(100, 125));
		assertFalse(splitReadContig.getMatch(100, 155));
	}
	
	@Test
	public void testMatchingQueryString() {
		createStandardObject(1);
		assertEquals(new Integer(5), splitReadContig.matchingQueryString(286, 85, 232, left));
		assertEquals(null, splitReadContig.matchingQueryString(286, 20, 232, left));
		assertEquals(new Integer(14), splitReadContig.matchingQueryString(286, 1, 95, right));
		assertEquals(null, splitReadContig.matchingQueryString(286, 20, 232, right));
	}
	
	private void createStandardObject(int num) {
		p = createMock(QSVParameters.class);
		expect(p.getPairingType()).andReturn("pe");
		expect(p.getPairingType()).andReturn("pe");
		if (num ==1) {
			expect(p.getRepeatCountCutoff()).andReturn(1000);
		} else {
			expect(p.getRepeatCountCutoff()).andReturn(1000);
			expect(p.getRepeatCountCutoff()).andReturn(1000);
		}
		
		replay(p);
		blat = createMock(BLAT.class);
		replay(blat);
		splitReadContig = new SplitReadContig(blat, p, "chr10", "chr10", 89700299, 89712341, QSVConstants.ORIENTATION_1);
		splitReadContig.setConsensus("CAGATAGGCAACAGATCGAGACCTTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAGTAATGGTGACAAAGCAATGAAAAGGAAAGGAAGAAGTGATAAGACATGGCAGCAAGCTGAAGTATGATGAGTAAAGAATAGGAATCA");				
		left = new SplitReadAlignment("chr10", "+", 89700210, 89700299, 1, 90);
		right = new SplitReadAlignment("chr10", "+", 89712341, 89712514, 109, 282);
		splitReadContig.setSplitReadAlignments(left, right);
	}

	@Test
	public void testOrientation1() throws Exception {
		records.add(new BLATRecord("263\t1\t0\t0\t1\t18\t1\t12041\t+\tsplitcon-chr10_89700299_chr10_89712341_1_true\t282\t0\t282\tchr10\t135534747\t89700209\t89712514\t2\t90,174,\t0,108,\t89700209,89712340,\t"));
		createStandardObject(2);
		testOrientationCategory("chr10", "chr10", 89700299, 89712341, QSVConstants.ORIENTATION_1, 
			"CAGATAGGCAACAGATCGAGACCTTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAGTAATGGTGACAAAGCAATGAAAAGGAAAGGAAGAAGTGATAAGACATGGCAGCAAGCTGAAGTATGATGAGTAAAGAATAGGAATCA");		
		assertEquals("GAGATTATACTTTGTGTA", splitReadContig.getNonTemplateSequence());
		assertEquals(QSVConstants.NOT_FOUND, splitReadContig.getMicrohomology());
	}
	
	@Test
	public void testOrientation2() throws Exception {
		
		createStandardObject(2);
		records.add(new BLATRecord("103\t0\t0\t0\t0\t0\t0\t0\t+\tsplitcon-chr7_104612302_chr7_104485067_2_true\t255\t0\t103\tchr7\t159138663\t104612199\t104612302\t1\t103,\t0,\t104612199,\t"));
		records.add(new BLATRecord("154\t0\t0\t0\t0\t0\t0\t0\t+\tsplitcon-chr7_104612302_chr7_104485067_2_true\t255\t101\t255\tchr7\t159138663\t104485066\t104485220\t1\t154,\t101,\t104485066,\t"));

		testOrientationCategory("chr7", "chr7", 104485067, 104612302, QSVConstants.ORIENTATION_2, 
			"ACTGGTCTGTTCAAATAGTGTGTAATTCAAAGCAATTGCTGCTCCCTAGGCAGGAATTTACAATTGCTACATGTGAAGGGAGGATAATGTGTCATTCCAAGGGTATTGCCTGAGGAAACCAAAGATAAAATAAAAACAACAATGATGTCTAACTCTTAGATAATGCTTGGTATTGACTAGGAATATTCTTTCCTTGTAGAATAATAGAGATATTCTAAGTATTTCTTTTCTTTCTTTTTTGAGACGCAATTTTGC");		
		
		assertEquals(QSVConstants.NOT_FOUND, splitReadContig.getNonTemplateSequence());
		assertEquals("GG", splitReadContig.getMicrohomology());
	}
	
	@Test
	public void testOrientation3() throws Exception {
		createStandardObject(2);
		records.add(new BLATRecord("78\t8\t0\t0\t0\t0\t0\t0\t-\tsplitcon-chr15_23831661_chr15_23992703_3_false\t230\t144\t230\tchr15\t102531392\t24563867\t24563953\t1\t86,\t0,\t24563867,\t"));
		records.add(new BLATRecord("80\t6\t0\t0\t0\t0\t0\t0\t-\tsplitcon-chr15_23831661_chr15_23992703_3_false\t230\t144\t230\tchr15\t102531392\t24928398\t24928484\t1\t86,\t0,\t24928398,\t"));
		records.add(new BLATRecord("80\t6\t0\t0\t0\t0\t0\t0\t-\tsplitcon-chr15_23831661_chr15_23992703_3_false\t230\t144\t230\tchr15\t102531392\t24771250\t24771336\t1\t86,\t0,\t24771250,\t"));
		records.add(new BLATRecord("86\t0\t0\t0\t0\t0\t0\t0\t-\tsplitcon-chr15_23831661_chr15_23992703_3_false\t230\t144\t230\tchr15\t102531392\t23831575\t23831661\t1\t86,\t0,\t23831575,\t"));
		records.add(new BLATRecord("225\t5\t0\t0\t0\t0\t2\t405222\t+\tsplitcon-chr15_23831661_chr15_23992703_3_false\t230\t0\t230\tchr15\t102531392\t23992556\t24398008\t3\t147,4,79,\t0,147,151,\t23992556,23993156,24397929,\t"));
	
		testOrientationCategory("chr15", "chr15", 
				23831661,23992703, QSVConstants.ORIENTATION_3, 
			"TTTTCCCCACATCCTTGCCAGTATTCATTATCGCCTGTCTATTTGAACACAAAGCCATTTTACCTGGGGTAAGATGATATTTCATTGTGATTTTGCTTTGCATTTCTTTCATGATTAGTGATGATGAACATTTTTAAATAACTGTTGCCACGAGAGTACACAGAGCAAAGGAGACAGGGTCATTTATACCCTGATGCGTCCACCCCACTGCTGTGTCCGGTTTCCATTGG");		
		assertEquals(QSVConstants.NOT_FOUND, splitReadContig.getNonTemplateSequence());
		assertEquals("CAA", splitReadContig.getMicrohomology());
	}

	@Test
	public void testOrientation4() throws Exception {
		createStandardObject(2);
		records.add(new BLATRecord("90\t0\t0\t0\t0\t0\t0\t0\t-\tsplitcon-chr3_24565106_chr3_24566179_3_true\t266\t0\t90\tchr3\t198022430\t24565105\t24565195\t1\t90,\t176,\t24565105,\t"));
		records.add(new BLATRecord("178\t1\t0\t0\t0\t0\t0\t0\t+\tsplitcon-chr3_24565106_chr3_24566179_3_true\t266\t87\t266\tchr3\t198022430\t24566178\t24566357\t1\t179,\t87,\t24566178,\t"));
	
		testOrientationCategory("chr3", "chr3", 24565106,24566179, QSVConstants.ORIENTATION_4, 
			"TTCATAACCAACAATATGTAGGAAGCCATTATCTGAAGTGTAAGCAACTGCATAGTGCTATTTTAATTATGCATTGCAGGGAAACTGTGAGCAGAGCTATATATTTAGGTAGACTGCTCTCAGGCAGAATGAAACACGATGGCACCTGCCACTCACGACCAGGAACCAAACAGGAAAGAATCCAAATTCTGTGTTTACAGGGCTTTCATGCTCAGTAAAATGCATAAGCACTTTTATTAGGGTTCTTAAAATTAGAAATCTATACT");		
		assertEquals(QSVConstants.NOT_FOUND, splitReadContig.getNonTemplateSequence());
		assertEquals("TGA", splitReadContig.getMicrohomology());
	}
	
	@Test
	public void testTranslocationOrientation1() throws Exception {
		records.add(new BLATRecord("100\t0\t0\t0\t0\t0\t0\t0\t+\tsplitcon-chr10_13231026_chr17_12656098_1_true\t204\t0\t100\tchr10\t135534747\t13230926\t13231026\t1\t100,\t0,\t13230926,\t"));
		records.add(new BLATRecord("100\t0\t0\t0\t0\t0\t0\t0\t+\tsplitcon-chr10_13231026_chr17_12656098_1_true\t204\t104\t204\tchr17\t81195210\t12656099\t12656199\t1\t100,\t104,\t12656099,\t"));
		createStandardObject(2);
		testOrientationCategory("chr10", "chr17", 13231026, 12656098, QSVConstants.ORIENTATION_1, 
			"AGCTCAGCGCAAAGCGTGCGGATCTGCAGTCCACCTTCTCTGGAGGACGAATTCCAAAGAAGTTTGCCCGCAGAGGCACCAGCCTCAAAGAACGGCTGTGTTTTAGCAGCCTGAATGGGGGCTCTGTTCCTTCTGAGCTGGATGGGCTGGACTCCGAGAAGGACAAGATGCTGGTGGAGAAGCAGAAGGTGATCAATGAACTCA");		
	}
	
	@Test
	public void testTranslocationOrientation2() throws Exception {
		createStandardObject(2);
		records.add(new BLATRecord("100\t0\t0\t0\t0\t0\t0\t0\t-\tsplitcon-chr17_12656199_chr10_13230926_2_true\t204\t0\t100\tchr10\t135534747\t13230926\t13231026\t1\t100,\t104,\t13230926,\t"));
		records.add(new BLATRecord("100\t0\t0\t0\t0\t0\t0\t0\t-\tsplitcon-chr17_12656199_chr10_13230926_2_true\t204\t104\t204\tchr17\t81195210\t12656099\t12656199\t1\t100,\t0,\t12656099,\t"));
		
		testOrientationCategory("chr10", "chr17", 13230926, 12656199, QSVConstants.ORIENTATION_2, 
			"CACAGCCGTTCTTTGAGGCTGGTGCCTCTGCGGGCAAACTTCTTTGGAATTCGTCCTCCAGAGAAGGTGGACTGCAGATCCGCACGCTTTGCGCTGAGCTAAAATGAGTTCATTGATCACCTTCTGCTTCTCCACCAGCATCTTGTCCTTCTCGGAGTCCAGCCCATCCAGCTCAGAAGGAACAGAGCCCCCATTCAGGCTGCT");		
	
	}
	
	@Test
	public void testTranslocationOrientation3() throws Exception {
		createStandardObject(2);
		records.add(new BLATRecord("100\t0\t0\t0\t0\t0\t0\t0\t+\tsplitcon-chr10_13231026_chr17_12656200_3_true\t204\t0\t100\tchr17\t81195210\t12656099\t12656199\t1\t100,\t0,\t12656099,\t"));
		records.add(new BLATRecord("100\t0\t0\t0\t0\t0\t0\t0\t-\tsplitcon-chr10_13231026_chr17_12656200_3_true\t204\t104\t204\tchr10\t135534747\t13230926\t13231026\t1\t100,\t0,\t13230926,\t"));
	
		testOrientationCategory("chr10", "chr17", 13231026, 12656200, QSVConstants.ORIENTATION_3, 
			"AGCAGCCTGAATGGGGGCTCTGTTCCTTCTGAGCTGGATGGGCTGGACTCCGAGAAGGACAAGATGCTGGTGGAGAAGCAGAAGGTGATCAATGAACTCAAAAACACAGCCGTTCTTTGAGGCTGGTGCCTCTGCGGGCAAACTTCTTTGGAATTCGTCCTCCAGAGAAGGTGGACTGCAGATCCGCACGCTTTGCGCTGAGCT");		
	}
	
	@Test
	public void testTranslocationOrientation4() throws Exception {
		createStandardObject(2);
		records.add(new BLATRecord("100\t0\t0\t0\t0\t0\t0\t0\t-\tsplitcon-chr10_13230927_chr17_12656100_4_false\t204\t0\t100\tchr17\t81195210\t12656099\t12656199\t1\t100,\t104,\t12656099,\t"));
		records.add(new BLATRecord("101\t0\t0\t0\t0\t0\t0\t0\t+\tsplitcon-chr10_13230927_chr17_12656100_4_false\t204\t103\t204\tchr10\t135534747\t13230925\t13231026\t1\t101,\t103,\t13230925,\t"));
		testOrientationCategory("chr10", "chr17", 13230927, 12656100, QSVConstants.ORIENTATION_4, 
			"TGAGTTCATTGATCACCTTCTGCTTCTCCACCAGCATCTTGTCCTTCTCGGAGTCCAGCCCATCCAGCTCAGAAGGAACAGAGCCCCCATTCAGGCTGCTAAAAAGCTCAGCGCAAAGCGTGCGGATCTGCAGTCCACCTTCTCTGGAGGACGAATTCCAAAGAAGTTTGCCCGCAGAGGCACCAGCCTCAAAGAACGGCTGTG");		
	}

	private void testOrientationCategory(String leftReference, String rightReference, 
			int leftBreakpoint, int rightBreakpoint, String orientation, String consensus) throws Exception {
		splitReadContig = new SplitReadContig(blat, p, leftReference, rightReference, leftBreakpoint, 
				rightBreakpoint, orientation);
		splitReadContig.setConsensus(consensus);
		splitReadContig.setConfidenceLevel(QSVConstants.LEVEL_HIGH);
		splitReadContig.parseConsensusAlign(records);
		assertTrue(splitReadContig.getIsPotentialSplitRead());
		assertEquals(splitReadContig.getOrientationCategory(), orientation);
	}
	

}
