package org.qcmg.qsv.splitread;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.BLATRecord;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.blat.BLAT;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.QSVUtil;

import gnu.trove.map.hash.TIntObjectHashMap;


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
	public void getMutationType() {
		createStandardObject(1);
		splitReadContig.setConfidenceLevel(QSVConstants.LEVEL_SINGLE_CLIP);
		splitReadContig.determineSplitReadPotential();
		assertEquals(null, splitReadContig.getMutationType());
	}

	@Test
	public void testLeftLower() throws UnsupportedEncodingException, QSVException {
		createStandardObject(1);
		splitReadContig.setSplitReadAlignments(left, right);
		assertTrue(SplitReadContig.leftLower(left.getQueryStart(), right.getQueryStart()));
		splitReadContig.setSplitReadAlignments(right, left);
		assertFalse(SplitReadContig.leftLower(right.getQueryStart(), left.getQueryStart()));
	}

	@Test
	public void getSingleSplitReadAlignment() {
		createStandardObject(1);
		assertEquals(null, SplitReadContig.getSingleSplitReadAlignment(left, right));
		assertEquals(left, SplitReadContig.getSingleSplitReadAlignment(left, null));
		assertEquals(right, SplitReadContig.getSingleSplitReadAlignment(null, right));
		assertEquals(null, SplitReadContig.getSingleSplitReadAlignment(null, null));
	}

	@Test
	public void testGetName() {
		createStandardObject(1);
		assertTrue(splitReadContig.getName().startsWith("splitcon_xxx_chr10_xxx_89700299_xxx_chr10_xxx_89712341_xxx_1_xxx_false"));
	}

	@Test
	public void testGetChrUnName() {
		createStandardObjectWithChrUn(1);
		assertTrue(splitReadContig.getName().startsWith("splitcon_xxx_chrUn_KI270442v1_xxx_44617_xxx_chrUn_KI270442v1_xxx_45730_xxx_2_xxx_false"));
	}

	@Test
	public void testReorder() {
		createStandardObject(1);
		splitReadContig.setSplitReadAlignments(right, left);
		assertTrue(splitReadContig.getLeft().getStartPos() > splitReadContig.getRight().getStartPos());

		assertCorrectSplitReadAlignmentOrder(null);

		left = new SplitReadAlignment("chr15", QSVUtil.PLUS, 89700210, 89700299, 1, 90);
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
		assertEquals(false, SplitReadContig.needToReverseComplement(QSVConstants.ORIENTATION_4, QSVUtil.MINUS));
		assertEquals(true, SplitReadContig.needToReverseComplement(QSVConstants.ORIENTATION_3, QSVUtil.MINUS));
		assertEquals(true, SplitReadContig.needToReverseComplement(QSVConstants.ORIENTATION_2, QSVUtil.MINUS));
		assertEquals(true, SplitReadContig.needToReverseComplement(QSVConstants.ORIENTATION_1, QSVUtil.MINUS));
		assertEquals(true, SplitReadContig.needToReverseComplement(QSVConstants.ORIENTATION_4, QSVUtil.PLUS));
		assertEquals(false, SplitReadContig.needToReverseComplement(QSVConstants.ORIENTATION_3, QSVUtil.PLUS));
		assertEquals(false, SplitReadContig.needToReverseComplement(QSVConstants.ORIENTATION_2, QSVUtil.PLUS));
		assertEquals(false, SplitReadContig.needToReverseComplement(QSVConstants.ORIENTATION_1, QSVUtil.PLUS));
	}

	@Test
	public void testCat1and2Nontemplate() {
		createStandardObject(1);
		splitReadContig.setCat1and2NonTemplate();
		assertEquals("GAGATTATACTTTGTGTA", splitReadContig.getNonTemplateSequence());

		left = new SplitReadAlignment("chr10", QSVUtil.MINUS, 89700210, 89700299,109, 282);
		right = new SplitReadAlignment("chr10", QSVUtil.MINUS, 89712341, 89712514,  1, 90);
		splitReadContig.setSplitReadAlignments(left, right);
		splitReadContig.setCat1and2NonTemplate();
		assertEquals("GAGATTATACTTTGTGTA", splitReadContig.getNonTemplateSequence());
	}

	@Test
	public void testCat3and4Nontemplate() {
		createStandardObject(1);
		right = new SplitReadAlignment("chr10", QSVUtil.PLUS, 89712341, 89712514,  1, 90);
		left = new SplitReadAlignment("chr10", QSVUtil.MINUS, 89700210, 89700299,109, 282);
		assertCat3and4NonTemplate(left, right, "3");

		right = new SplitReadAlignment("chr10", QSVUtil.MINUS, 89712341, 89712514,  1, 90);
		left = new SplitReadAlignment("chr10", QSVUtil.PLUS, 89700210, 89700299,109, 282);
		assertCat3and4NonTemplate(left, right, "4");

		left = new SplitReadAlignment("chr10", QSVUtil.MINUS, 89712341, 89712514,  109, 282);
		right = new SplitReadAlignment("chr10", QSVUtil.PLUS, 89700210, 89700299,1, 90);
		assertCat3and4NonTemplate(left, right, "3");

		left = new SplitReadAlignment("chr10", QSVUtil.PLUS, 89712341, 89712514,  109, 282);
		right = new SplitReadAlignment("chr10", QSVUtil.MINUS, 89700210, 89700299,1, 90);
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
	public void setSplitReadAlignments() {
		/*
		 * consensus: CTAGTGTGTGTGTGTGTGTGTGTGTGTGTGTGTGTGTGTGTGTGTGTGTGTGTGTGTGTAGACAAGGTCTCGCTGTGTTGCCCAGGCTGGCCTTGAACTCCTGGCCTTGAGTGAGCCTCCCACCTCAACCTCCCGAGGTGCTGAGGTTACAAATGTGAGCTACTGCACCTGGCACTAGAAATTAGCTTTTATTTACACTTTCTAAGCATTCACACTGTGCCTGGTTCCGGTT
		 * 214		215	1	0	0	0	0	0	0+	splitcon_chr7_101126970_chr7_156838178__true_1557450089062_845461	232	15	231	chr7	12345	101127272	101127488	1	216	15	101127287	,
		 * r2: 74		118	39	0	0	3	3	2	2	-	splitcon_chr7_101126970_chr7_156838178__true_1557450089062_845461	232	-1	144	chr7	12345	100341191	100341351	2	14,145	-1,-1	100341190,100341190	,
		 * left: null, right: null, lhsBp: 101126970, rhsBp: 156838178, confidenceLevel: 6, length: 232
		 */
		BLATRecord r1 = new BLATRecord.Builder("215	1	0	0	0	0	0	0	+	splitcon_xxx_chr7_xxx_101126970_xxx_chr7_xxx_156838178_xxx__xxx_true_xxx_1557450089062_xxx_845461	232	15	231	chr7	12345	101127272	101127488	1	216	15	101127287".split("\t")).build();
		BLATRecord r2 = new BLATRecord.Builder("118	39	0	0	3	3	2	2	-	splitcon_xxx_chr7_xxx_101126970_xxx_chr7_xxx_156838178_xxx__xxx_true_xxx_1557450089062_xxx_845461	232	-1	144	chr7	12345	100341191	100341351	2	14,145	-1,-1	100341190,100341190".split("\t")).build();
		SplitReadAlignment left = null;
		SplitReadAlignment right = null;
		int lhsBp = 101126970;
		int rhsBp = 156838178;
		String confidenceLevel = "6";
		int length = 232;
		Pair<SplitReadAlignment, SplitReadAlignment> pair = SplitReadContig.setSplitReadAlignments(r1, r2, lhsBp, rhsBp, confidenceLevel, length);
		left = pair.getLeft();
		right = pair.getRight();
		assertEquals(false, left == null);
		assertEquals(true, right == null);
	}

	@Test
	public void testknownAndPotentialReferencesMatch() {
		StructuralVariant knownSV = new StructuralVariant("chr10", "chr10", 89700299, 89712341, "1");
		SplitReadAlignment left = new SplitReadAlignment("chr10", QSVUtil.MINUS, 89700210, 89700299, 109, 282);
		SplitReadAlignment right = new SplitReadAlignment("chr10", QSVUtil.MINUS, 89700210, 89700299, 109, 282);
		assertTrue(SplitReadContig.knownAndPotentialReferencesMatch(knownSV, left, right));

		knownSV = new StructuralVariant("chr10", "chr10", 89700299, 89712341, "1");
		left = new SplitReadAlignment("chr12", QSVUtil.MINUS, 89700210, 89700299, 109, 282);
		right = new SplitReadAlignment("chr10", QSVUtil.MINUS, 89700210, 89700299, 109, 282);
		assertFalse(SplitReadContig.knownAndPotentialReferencesMatch(knownSV, left, right));

		knownSV = new StructuralVariant("chr10", "chr10", 89700299, 89712341, "1");
		left = new SplitReadAlignment("chr10", QSVUtil.MINUS, 89700210, 89700299, 109, 282);
		right = new SplitReadAlignment("chr12", QSVUtil.MINUS, 89700210, 89700299, 109, 282);
		assertFalse(SplitReadContig.knownAndPotentialReferencesMatch(knownSV, left, right));

		knownSV = new StructuralVariant("chr10", "chr12", 89700299, 89712341, "1");
		left = new SplitReadAlignment("chr12", QSVUtil.MINUS, 89700210, 89700299, 109, 282);
		right = new SplitReadAlignment("chr10", QSVUtil.MINUS, 89700210, 89700299, 109, 282);
		assertTrue(SplitReadContig.knownAndPotentialReferencesMatch(knownSV, left, right));

		knownSV = new StructuralVariant("chr10", "chr12", 89700299, 89712341, "1");
		left = new SplitReadAlignment("chr10", QSVUtil.MINUS, 89700210, 89700299, 109, 282);
		right = new SplitReadAlignment("chr12", QSVUtil.MINUS, 89700210, 89700299, 109, 282);
		assertTrue(SplitReadContig.knownAndPotentialReferencesMatch(knownSV, left, right));

		knownSV = new StructuralVariant("chr10", "chr12", 89700299, 89712341, "1");
		left = new SplitReadAlignment("chr10", QSVUtil.MINUS, 89700210, 89700299, 109, 282);
		right = new SplitReadAlignment("chr10", QSVUtil.MINUS, 89700210, 89700299, 109, 282);
		assertFalse(SplitReadContig.knownAndPotentialReferencesMatch(knownSV, left, right));


	}

	@Test
	public void setSplitReadAlignments2() {
		/*
		 * consensus: CGTGGGGGTGGGATCCACTGAGCTAGAACACTTGGCTCCCTGGCTTTGGCCCCCTTTCCAGGGGAGTGAACAGTTCTGTCTTGCTGGTGTTCCAGGCGCCACTGGGGTATGAAAAATATTCCTGCAGCTAGCTCAGTGTCTTCTTGGCAATGTGGGCACTTTTTTGGTTCCATATGAATTTTAAAGTAGTTTTTTCCAATTCTGTGAAGAAA
		 *r1: 144		160	8	0	0	4	4	4	4+	name	212	0	172	chr9	12345	22496387	22496559	2	159,13	0,159	22496387,22496546	,
		 *r2: 116		146	22	0	0	2	26	6	-	name	212	-1	110	chr9	12345	79456284	79456454	2	63,111	-1,-1	79456283,79456283	,
		 *left: null, right: null, lhsBp: 22496527, rhsBp: 22504346, confidenceLevel: 6, length: 212
		 */
		BLATRecord r1 = new BLATRecord.Builder("160	8	0	0	4	4	4	4	+	name	212	0	172	chr9	12345	22496387	22496559	2	159,13	0,159	22496387,22496546".split("\t")).build();
		BLATRecord r2 = new BLATRecord.Builder("146	22	0	0	2	2	6	6	-	name	212	-1	110	chr9	12345	79456284	79456454	2	63,111	-1,-1	79456283,79456283".split("\t")).build();
		SplitReadAlignment left = null;
		SplitReadAlignment right = null;
		int lhsBp = 22496527;
		int rhsBp = 22504346;
		String confidenceLevel = "6";
		int length = 212;
		Pair<SplitReadAlignment, SplitReadAlignment> pair = SplitReadContig.setSplitReadAlignments(r1, r2, lhsBp, rhsBp, confidenceLevel, length);
		left = pair.getLeft();
		right = pair.getRight();
		assertEquals(false, left == null);
		assertEquals(false, right == null);
		assertEquals(151, right.getQueryStart().intValue());
		assertEquals(213, right.getQueryEnd().intValue());
		assertEquals(1, left.getQueryStart().intValue());
		assertEquals(159, left.getQueryEnd().intValue());
	}

	@Test
	public void setSplitReadAlignments3() {
		/*
		 *WARNING org.qcmg.qsv.splitread.SplitReadContig - about to call setSplitReadAlignments with r1: 155     172     3       0       0       7       19      7       23      +       name    194     0       194     chr8    12345   136887841       136888039       8       4,2,3,13,6,9,2,155      0,4,6,9,22,28,37,39     136887841,136887845,136887847,136887850,136887863,136887869,136887878,136887880,
		 *r2: 105        144     11      0       0       13      39      15      25      +       name    194     0	194     chr8    12345   136848254       136848434       16      60,1,1,9,3,52,9,4,4,11,7,2,5,4,2,20     0,1,1,62,9,74,126,74,139,143,130,25,163,144,1,174       136848254,136848255,136848255,136848316,136848263,136848328,136848380,136848328,136848393,136848397,136848384,136848279,136848417,136848398,136848255,136848428,
		 *left: null, right: null, lhsBp: 136848311, rhsBp: 136887903, confidenceLevel: 6, length: 194
		 */
		BLATRecord r1 = new BLATRecord.Builder("172	3	0	0	7	19	7	23	+	name	194	0	194	chr8	12345	136887841	136888039	8	4,2,3,13,6,9,2,155	0,4,6,9,22,28,37,39	136887841,136887845,136887847,136887850,136887863,136887869,136887878,136887880".split("\t")).build();
		BLATRecord r2 = new BLATRecord.Builder("144	11	0	0	13	39	15	25	+	name	194	0	194	chr8	12345	136848254	136848434	16	60,1,1,9,3,52,9,4,4,11,7,2,5,4,2,20	0,1,1,62,9,74,126,74,139,143,130,25,163,144,1,174	136848254,136848255,136848255,136848316,136848263,136848328,136848380,136848328,136848393,136848397,136848384,136848279,136848417,136848398,136848255,136848428".split("\t")).build();
		SplitReadAlignment left = null;
		SplitReadAlignment right = null;
		int lhsBp = 136848311;
		int rhsBp = 136887903;
		String confidenceLevel = "6";
		int length = 194;
		Pair<SplitReadAlignment, SplitReadAlignment> pair = SplitReadContig.setSplitReadAlignments(r1, r2, lhsBp, rhsBp, confidenceLevel, length);
		left = pair.getLeft();
		right = pair.getRight();
		assertEquals(false, left == null);
		assertEquals(false, right == null);

		System.out.println("left strand: " + left.getStrand());
		System.out.println("right strand: " + right.getStrand());
		System.out.println("left lower: " + (left.getQueryStart() < right.getQueryStart()));
		System.out.println("left getQueryStart: " + left.getQueryStart());
		System.out.println("left getQueryEnd: " + left.getQueryEnd());
		System.out.println("left getStartPos: " + left.getStartPos());
		System.out.println("right getQueryStart: " + right.getQueryStart());
		System.out.println("right getQueryEnd: " + right.getQueryEnd());

		assertEquals(1, right.getQueryStart().intValue());
		assertEquals(60, right.getQueryEnd().intValue());
		assertEquals(40, left.getQueryStart().intValue());
		assertEquals(194, left.getQueryEnd().intValue());
	}

	@Test
	public void passesNewAlignmentFilters() {
		SplitReadAlignment newAlign = new SplitReadAlignment(new BLATRecord.Builder("215	1	0	0	0	0	0	0	+	splitcon_xxx_chr7_xxx_101126970_xxx_chr7_xxx_156838178_xxx__xxx_true_xxx_1557450089062_xxx_845461	232	15	231	chr7	12345	101127272	101127488	1	216	15	101127287".split("\t")).build());
		assertEquals(false, SplitReadContig.passesNewAlignmentFilters(newAlign, "6", 101126970, 156838178, 232));
	}
	@Test
	public void passesNewAlignmentFilters2() {
		SplitReadAlignment newAlign = new SplitReadAlignment("chr15",'+',	24397930	,24398008	,152	,230);
		assertEquals(true, SplitReadContig.passesNewAlignmentFilters(newAlign, "6", 23831661, 23992703, 230));
	}

	@Test
	public void getBestBlocksFromBlat() {
		/*
		 * 395     0       0       0       1       1       2       210     +       splitcon_chr7_100867120_chr7_100867215__true_1584581279378_768689       398     1       397     chr7    12345   100866756       100867361       3       78,171,147      1,79,250        100866757,100866835,100867006,
		 */
		SplitReadAlignment left = null;
		SplitReadAlignment right = null;
		StructuralVariant knownSV = new StructuralVariant("chr7", "chr7", 100867120, 100867215, null);

		BLATRecord record = new BLATRecord.Builder("395	0	0	0	1	1	2	210	+	splitcon_xxx_chr7_xxx_100867120_xxx_chr7_xxx_100867215_xxx__xxx_true_xxx_1584581279378_xxx_768689	398	1	397	chr7	12345	100866756	100867361	3	78,171,147	1,79,250	100866757,100866835,100867006,").build();
		Pair<SplitReadAlignment, SplitReadAlignment> pair = SplitReadContig.getBestBlocksFromBLAT(record, true, left, right, "6",  knownSV, 398);
		assertEquals(true, pair.getLeft() != null);
		assertEquals(true, pair.getRight() != null);
		System.out.println("left: " + pair.getLeft());
		System.out.println("right: " + pair.getRight());

	}

	@Test
	public void getBestBlocksFromBlat2() {
		SplitReadAlignment left = null;
		SplitReadAlignment right = null;
		StructuralVariant knownSV = new StructuralVariant("chr7", "chr7", 100867120, 100867215, null);

		BLATRecord record = new BLATRecord.Builder("172	0	0	0	0	0	1	94	+	name	172	0	172	chr7	12345	100867054	100867320	2	66,106	0,66	100867054,100867120,").build();
		Pair<SplitReadAlignment, SplitReadAlignment> pair = SplitReadContig.getBestBlocksFromBLAT(record, true, left, right, "6", knownSV, 172);
		assertEquals(true, pair.getLeft() != null);
		assertEquals(true, pair.getRight() != null);

		System.out.println("left: " + pair.getLeft());
		System.out.println("right: " + pair.getRight());

	}

	@Test
	public void getBestBlocksFromBlat3() {
		/*
		 *  getBestBlocksFromBLAT rec: 80    104     20      0       0       1       1       3       13      -       splitcon_chr10_127633807_chr15_34031839__true_1587614939343_235036      188     63      188     chr10   12345   127633738       127633875       4       31,16,7,71      63,94,110,117   127633801,127633833,127633860,127633868,
		 *  left: chr15    +       34031839        34031957        70      188,
		 *  right: null, confidenceLevel: 6,
		 *  knownSV: chr10_127633807_chr15_34031839_, length: 188
		 */
		SplitReadAlignment left = new SplitReadAlignment("chr15", '+', 34031839, 34031957, 70, 188);
		SplitReadAlignment right = null;
		StructuralVariant knownSV = new StructuralVariant("chr10", "chr15", 127633807, 34031839, null);

		BLATRecord record = new BLATRecord.Builder("104	20	0	0	1	1	3	13	-	splitcon_xxx_chr10_xxx_127633807_xxx_chr15_xxx_34031839_xxx__xxx_true_xxx_1587614939343_xxx_235036	188	63	188	chr10	12345	127633738	127633875	4	31,16,7,71	63,94,110,117	127633801,127633833,127633860,127633868,").build();
		Pair<SplitReadAlignment, SplitReadAlignment> pair = SplitReadContig.getBestBlocksFromBLAT(record, true, left, right, "6", knownSV, 188);
		assertEquals(true, pair.getLeft() != null);
		assertEquals(true, pair.getRight() == null);

		System.out.println("left: " + pair.getLeft());
		System.out.println("right: " + pair.getRight());


		/*
		 * this is the record returned from blat
		 * 67      2       0       0       0       0       0       0       -       splitcon_chr10_127633807_chr15_34031839__true_1586132281792_839799      188     0       69      chr10   135534747       127633806       127633875       1       69,     119,    127633806,
		 */

		BLATRecord blatRecord = new BLATRecord.Builder("67	2	0	0	0	0	0	0	-	splitcon_chr10_127633807_chr15_34031839__true_1586132281792_839799	188	0	69	chr10	135534747	127633806	127633875	1	69,	119,	127633806,").build();
		knownSV = new StructuralVariant("chr10", "chr15", 127633807, 34031839, null);
		pair = SplitReadContig.getBestBlocksFromBLAT(blatRecord, true, left, right, "6", knownSV, 188);
		assertEquals(true, pair.getLeft() != null);
		System.out.println("left: " + pair.getLeft());
		System.out.println("right: " + pair.getRight());
	}

	@Test
	public void determineMutationType() {
		StructuralVariant sv = new StructuralVariant("chrX", "chrX", 100, 1000, "1");
		assertEquals("DEL/ITX", SplitReadContig.determineMutationType(sv));
		sv = new StructuralVariant("chrX", "chrX", 100, 1000, "2");
		assertEquals("DUP/INS/ITX", SplitReadContig.determineMutationType(sv));
		sv = new StructuralVariant("chrX", "chrX", 100, 1000, "3");
		assertEquals("INV/ITX", SplitReadContig.determineMutationType(sv));
		sv = new StructuralVariant("chrX", "chrX", 100, 1000, "4");
		assertEquals("INV/ITX", SplitReadContig.determineMutationType(sv));
		sv = new StructuralVariant("chrX", "chrX", 100, 1000, "");
		assertEquals("", SplitReadContig.determineMutationType(sv));
		sv = new StructuralVariant("chrX", "chrX", 100, 1000, null);
		assertEquals("", SplitReadContig.determineMutationType(sv));
	}

	@Test
	public void getAlignmentDifference() {
		SplitReadAlignment sra = new SplitReadAlignment("chr1", '-', 1000, 900, 1, 50);
		assertEquals(null, SplitReadContig.getAlignmentDifference(null, null, 10, sra, QSVConstants.LEVEL_HIGH, 89700299, 89712341, 282));
		assertEquals(sra, SplitReadContig.getAlignmentDifference(null, null, 10, sra, QSVConstants.LEVEL_SINGLE_CLIP, 89700299, 89712341, 282));

		sra = new SplitReadAlignment("chr1", '-', 89700299, 89712341, 1, 50);
		assertEquals(sra, SplitReadContig.getAlignmentDifference(null, null, 10, sra, QSVConstants.LEVEL_HIGH, 89700299, 89712341, 282));

	}

	@Test
	public void testPassesBreakpointFilter() {
		createStandardObject(1);
		splitReadContig.setConfidenceLevel(QSVConstants.LEVEL_HIGH);
		assertTrue(SplitReadContig.passesBreakpointFilter(left, right, QSVConstants.LEVEL_HIGH, 89700299, 89712341));

		splitReadContig.setConfidenceLevel(QSVConstants.LEVEL_SINGLE_CLIP);
		left = new SplitReadAlignment("chr10", QSVUtil.PLUS, 89700210, 89719299, 1, 90);
		right = new SplitReadAlignment("chr10", QSVUtil.MINUS, 89712341, 89712514, 109, 282);
		assertTrue(SplitReadContig.passesBreakpointFilter(left, right, QSVConstants.LEVEL_SINGLE_CLIP, 89700299, 89712341));
		splitReadContig.setConfidenceLevel(QSVConstants.LEVEL_HIGH);
		assertFalse(SplitReadContig.passesBreakpointFilter(left, right, QSVConstants.LEVEL_HIGH, 89700299, 89712341));
	}

	@Test
	public void testQueryStringPositionFilter() {
		createStandardObject(1);
		assertTrue(SplitReadContig.passesQueryPositionFilter(right, left, 282));

		left = new SplitReadAlignment("chr10", QSVUtil.PLUS, 89700210, 89700299, 1, 200);
		right = new SplitReadAlignment("chr10", QSVUtil.PLUS, 89712341, 89712514, 20, 232);
		assertFalse(SplitReadContig.passesQueryPositionFilter(left, right, 282));

		left = new SplitReadAlignment("chr10", QSVUtil.PLUS, 89700210, 89700299, 1, 120);
		right = new SplitReadAlignment("chr10", QSVUtil.PLUS, 89712341, 89712514, 110, 232);
		assertTrue(SplitReadContig.passesQueryPositionFilter(left, right, 282));
	}

	@Test
	public void testQueryLengthFilter() {
		createStandardObject(1);
		assertTrue(SplitReadContig.queryLengthFilter(right, left, 282));

		left = new SplitReadAlignment("chr10", QSVUtil.PLUS, 89700210, 89700299, 1, 10);
		assertFalse(SplitReadContig.queryLengthFilter(left, right, 282));

		left = new SplitReadAlignment("chr10", QSVUtil.PLUS, 89700210, 89700299, 0, 290);
		assertFalse(SplitReadContig.queryLengthFilter(left, right, 282));

		left = new SplitReadAlignment("chr10", QSVUtil.PLUS, 89700210, 89700299, 1, 90);
		right = new SplitReadAlignment("chr10", QSVUtil.PLUS, 89712341, 89712514, 1, 200);
		assertFalse(SplitReadContig.queryLengthFilter(left, right, 282));
		assertFalse(SplitReadContig.queryLengthFilter(right, left, 282));
	}

	@Test
	public void testPassesSizeFilter() {
		createStandardObject(1);
		assertFalse(SplitReadContig.passesSizeFilter(new SplitReadAlignment("chr10", QSVUtil.PLUS, 2970200, 2970220, 1, 19), 282));
		assertFalse(SplitReadContig.passesSizeFilter(new SplitReadAlignment("chr10", QSVUtil.PLUS, 2970200, 2970220, 1, 270), 282));
		assertTrue(SplitReadContig.passesSizeFilter(new SplitReadAlignment("chr10", QSVUtil.PLUS, 2970200, 2970220, 1, 100), 282));
	}

	@Test
	public void testPassesBreakpointFilterSingleAlignment() {
		createStandardObject(1);
		splitReadContig.setConfidenceLevel(QSVConstants.LEVEL_MID);
		assertTrue(SplitReadContig.passesBreakpointFilter(left, QSVConstants.LEVEL_MID, 89700299, 89712341));
		assertTrue(SplitReadContig.passesBreakpointFilter(right, QSVConstants.LEVEL_MID, 89700299, 89712341));

		right = new SplitReadAlignment("chr10", QSVUtil.PLUS, 100, 200, 1, 200);

		assertFalse(SplitReadContig.passesBreakpointFilter(right, QSVConstants.LEVEL_MID, 89700299, 89712341));
	}

	@Test
	public void testCheckBlockSize() throws QSVException {
		createStandardObject(1);
		assertFalse(SplitReadContig.checkBlockSize(new BLATRecord.Builder("187\t5\t0\t0\t2\t61\t1\t17709\t-\tsplitcon_xxx_chr22_xxx_21353096_xxx_chr22_xxx_21566646_xxx_3_xxx_true\t253\t0\t253\tchr22\t51304566\t25002854\t25020755\t3\t17,10,165,\t0,22,88,\t25002854,25002871,25020590,\t").build()));
		assertTrue(SplitReadContig.checkBlockSize(new BLATRecord.Builder("187\t5\t0\t0\t2\t61\t1\t17709\t-\tsplitcon_xxx_chr22_xxx_21353096_xxx_chr22_xxx_21566646_xxx_3_xxx_true\t253\t0\t253\tchr22\t51304566\t25002854\t25020755\t2\t270,165,\t22,88,\t25002854,25020590,\t").build()));
	}

	@Test
	public void testGetMatch() {
		assertTrue(SplitReadContig.getMatch(100, 125));
		assertFalse(SplitReadContig.getMatch(100, 155));
	}

	@Test
	public void testMatchingQueryString() {
		createStandardObject(1);
		assertEquals(true, SplitReadContig.matchingQueryString(286, 85, 232, left, 282));
		assertEquals(false, SplitReadContig.matchingQueryString(286, 20, 232, left, 282));
		assertEquals(true, SplitReadContig.matchingQueryString(286, 1, 95, right, 282));
		assertEquals(false, SplitReadContig.matchingQueryString(286, 20, 232, right, 282));
	}

	private void createStandardObject(int num) {
		p = createMock(QSVParameters.class);
		expect(p.isTumor()).andReturn(false);
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
		splitReadContig = new SplitReadContig(new TIntObjectHashMap<int[]>(), p, "chr10", "chr10", 89700299, 89712341, QSVConstants.ORIENTATION_1);
		splitReadContig.setConsensus("CAGATAGGCAACAGATCGAGACCTTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAGTAATGGTGACAAAGCAATGAAAAGGAAAGGAAGAAGTGATAAGACATGGCAGCAAGCTGAAGTATGATGAGTAAAGAATAGGAATCA");
		left = new SplitReadAlignment("chr10", QSVUtil.PLUS, 89700210, 89700299, 1, 90);
		right = new SplitReadAlignment("chr10", QSVUtil.PLUS, 89712341, 89712514, 109, 282);
		splitReadContig.setSplitReadAlignments(left, right);
	}

	private void createStandardObjectWithChrUn(int num) {
		p = createMock(QSVParameters.class);
		expect(p.isTumor()).andReturn(false);
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

		splitReadContig = new SplitReadContig(new TIntObjectHashMap<int[]>(), p, "chrUn_KI270442v1", "chrUn_KI270442v1", 45730, 44617, QSVConstants.ORIENTATION_2);
		splitReadContig.setConsensus("TCCCCTCCATTCGATGCCATTCAATTCCACCCGCTTCGATTCCACTTGATTCCATTGGATTCAGTTTTTTATGTTCGATTCCATTCGATTTCATTTGATTGCATATGATTCGATTTGATTCAATTTGATTCACTTCCATGTGATTCCATGTGATTCCATGCTATTCCATTACTTTCCATTCCACTCCA");
		left = new SplitReadAlignment("chrUn_KI270442v1", QSVUtil.PLUS, 45730, 45735, 1, 5);
		right = new SplitReadAlignment("chr10", QSVUtil.PLUS, 44617, 44627, 1, 10);
		splitReadContig.setSplitReadAlignments(left, right);
	}

	@Test
	public void testOrientation1() throws Exception {
		records.add(new BLATRecord.Builder("263\t1\t0\t0\t1\t18\t1\t12041\t+\tsplitcon_xxx_chr10_xxx_89700299_xxx_chr10_xxx_89712341_xxx_1_xxx_true\t282\t0\t282\tchr10\t135534747\t89700209\t89712514\t2\t90,174,\t0,108,\t89700209,89712340,\t").build());
		createStandardObject(2);
		testOrientationCategory("chr10", "chr10", 89700299, 89712341, QSVConstants.ORIENTATION_1,
			"CAGATAGGCAACAGATCGAGACCTTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAGTAATGGTGACAAAGCAATGAAAAGGAAAGGAAGAAGTGATAAGACATGGCAGCAAGCTGAAGTATGATGAGTAAAGAATAGGAATCA");
		assertEquals("GAGATTATACTTTGTGTA", splitReadContig.getNonTemplateSequence());
		assertEquals(QSVConstants.NOT_FOUND, splitReadContig.getMicrohomology());
	}

	@Test
	public void testOrientation2() throws Exception {

		createStandardObject(2);
		records.add(new BLATRecord.Builder("103\t0\t0\t0\t0\t0\t0\t0\t+\tsplitcon_xxx_chr7_xxx_104612302_xxx_chr7_xxx_104485067_xxx_2_xxx_true\t255\t0\t103\tchr7\t159138663\t104612199\t104612302\t1\t103,\t0,\t104612199,\t").build());
		records.add(new BLATRecord.Builder("154\t0\t0\t0\t0\t0\t0\t0\t+\tsplitcon_xxx_chr7_xxx_104612302_xxx_chr7_xxx_104485067_xxx_2_xxx_true\t255\t101\t255\tchr7\t159138663\t104485066\t104485220\t1\t154,\t101,\t104485066,\t").build());

		testOrientationCategory("chr7", "chr7", 104485067, 104612302, QSVConstants.ORIENTATION_2,
			"ACTGGTCTGTTCAAATAGTGTGTAATTCAAAGCAATTGCTGCTCCCTAGGCAGGAATTTACAATTGCTACATGTGAAGGGAGGATAATGTGTCATTCCAAGGGTATTGCCTGAGGAAACCAAAGATAAAATAAAAACAACAATGATGTCTAACTCTTAGATAATGCTTGGTATTGACTAGGAATATTCTTTCCTTGTAGAATAATAGAGATATTCTAAGTATTTCTTTTCTTTCTTTTTTGAGACGCAATTTTGC");

		assertEquals(QSVConstants.NOT_FOUND, splitReadContig.getNonTemplateSequence());
		assertEquals("GG", splitReadContig.getMicrohomology());
	}

	@Test
	public void testOrientation3() throws Exception {
		createStandardObject(2);
		records.add(new BLATRecord.Builder("78\t8\t0\t0\t0\t0\t0\t0\t-\tsplitcon_xxx_chr15_xxx_23831661_xxx_chr15_xxx_23992703_xxx_3_xxx_false\t230\t144\t230\tchr15\t102531392\t24563867\t24563953\t1\t86,\t0,\t24563867,\t").build());
		records.add(new BLATRecord.Builder("80\t6\t0\t0\t0\t0\t0\t0\t-\tsplitcon_xxx_chr15_xxx_23831661_xxx_chr15_xxx_23992703_xxx_3_xxx_false\t230\t144\t230\tchr15\t102531392\t24928398\t24928484\t1\t86,\t0,\t24928398,\t").build());
		records.add(new BLATRecord.Builder("80\t6\t0\t0\t0\t0\t0\t0\t-\tsplitcon_xxx_chr15_xxx_23831661_xxx_chr15_xxx_23992703_xxx_3_xxx_false\t230\t144\t230\tchr15\t102531392\t24771250\t24771336\t1\t86,\t0,\t24771250,\t").build());
		records.add(new BLATRecord.Builder("86\t0\t0\t0\t0\t0\t0\t0\t-\tsplitcon_xxx_chr15_xxx_23831661_xxx_chr15_xxx_23992703_xxx_3_xxx_false\t230\t144\t230\tchr15\t102531392\t23831575\t23831661\t1\t86,\t0,\t23831575,\t").build());
		records.add(new BLATRecord.Builder("225\t5\t0\t0\t0\t0\t2\t405222\t+\tsplitcon_xxx_chr15_xxx_23831661_chr15_23992703_xxx_3_false\t230\t0\t230\tchr15\t102531392\t23992556\t24398008\t3\t147,4,79,\t0,147,151,\t23992556,23993156,24397929,\t").build());

		testOrientationCategory("chr15", "chr15",
				23831661,23992703, QSVConstants.ORIENTATION_3,
			"TTTTCCCCACATCCTTGCCAGTATTCATTATCGCCTGTCTATTTGAACACAAAGCCATTTTACCTGGGGTAAGATGATATTTCATTGTGATTTTGCTTTGCATTTCTTTCATGATTAGTGATGATGAACATTTTTAAATAACTGTTGCCACGAGAGTACACAGAGCAAAGGAGACAGGGTCATTTATACCCTGATGCGTCCACCCCACTGCTGTGTCCGGTTTCCATTGG");
		assertEquals(QSVConstants.NOT_FOUND, splitReadContig.getNonTemplateSequence());
		assertEquals("CAA", splitReadContig.getMicrohomology());
	}

	@Test
	public void testOrientation4() throws Exception {
		createStandardObject(2);
		records.add(new BLATRecord.Builder("90\t0\t0\t0\t0\t0\t0\t0\t-\tsplitcon_xxx_chr3_xxx_24565106_xxx_chr3_xxx_24566179_xxx_3_xxx_true\t266\t0\t90\tchr3\t198022430\t24565105\t24565195\t1\t90,\t176,\t24565105,\t").build());
		records.add(new BLATRecord.Builder("178\t1\t0\t0\t0\t0\t0\t0\t+\tsplitcon_xxx_chr3_xxx_24565106_xxx_chr3_xxx_24566179_xxx_3_xxx_true\t266\t87\t266\tchr3\t198022430\t24566178\t24566357\t1\t179,\t87,\t24566178,\t").build());

		testOrientationCategory("chr3", "chr3", 24565106,24566179, QSVConstants.ORIENTATION_4,
			"TTCATAACCAACAATATGTAGGAAGCCATTATCTGAAGTGTAAGCAACTGCATAGTGCTATTTTAATTATGCATTGCAGGGAAACTGTGAGCAGAGCTATATATTTAGGTAGACTGCTCTCAGGCAGAATGAAACACGATGGCACCTGCCACTCACGACCAGGAACCAAACAGGAAAGAATCCAAATTCTGTGTTTACAGGGCTTTCATGCTCAGTAAAATGCATAAGCACTTTTATTAGGGTTCTTAAAATTAGAAATCTATACT");
		assertEquals(QSVConstants.NOT_FOUND, splitReadContig.getNonTemplateSequence());
		assertEquals("TGA", splitReadContig.getMicrohomology());
	}

	@Test
	public void testTranslocationOrientation1() throws Exception {
		records.add(new BLATRecord.Builder("100\t0\t0\t0\t0\t0\t0\t0\t+\tsplitcon_xxx_chr10_xxx_13231026_xxx_chr17_xxx_12656098_xxx_1_xxx_true\t204\t0\t100\tchr10\t135534747\t13230926\t13231026\t1\t100,\t0,\t13230926,\t").build());
		records.add(new BLATRecord.Builder("100\t0\t0\t0\t0\t0\t0\t0\t+\tsplitcon_xxx_chr10_xxx_13231026_xxx_chr17_xxx_12656098_xxx_1_xxx_true\t204\t104\t204\tchr17\t81195210\t12656099\t12656199\t1\t100,\t104,\t12656099,\t").build());
		createStandardObject(2);
		testOrientationCategory("chr10", "chr17", 13231026, 12656098, QSVConstants.ORIENTATION_1,
			"AGCTCAGCGCAAAGCGTGCGGATCTGCAGTCCACCTTCTCTGGAGGACGAATTCCAAAGAAGTTTGCCCGCAGAGGCACCAGCCTCAAAGAACGGCTGTGTTTTAGCAGCCTGAATGGGGGCTCTGTTCCTTCTGAGCTGGATGGGCTGGACTCCGAGAAGGACAAGATGCTGGTGGAGAAGCAGAAGGTGATCAATGAACTCA");
	}

	@Test
	public void testTranslocationOrientation2() throws Exception {
		createStandardObject(2);
		records.add(new BLATRecord.Builder("100\t0\t0\t0\t0\t0\t0\t0\t-\tsplitcon_xxx_chr17_xxx_12656199_xxx_chr10_xxx_13230926_xxx_2_xxx_true\t204\t0\t100\tchr10\t135534747\t13230926\t13231026\t1\t100,\t104,\t13230926,\t").build());
		records.add(new BLATRecord.Builder("100\t0\t0\t0\t0\t0\t0\t0\t-\tsplitcon_xxx_chr17_xxx_12656199_xxx_chr10_xxx_13230926_xxx_2_xxx_true\t204\t104\t204\tchr17\t81195210\t12656099\t12656199\t1\t100,\t0,\t12656099,\t").build());

		testOrientationCategory("chr10", "chr17", 13230926, 12656199, QSVConstants.ORIENTATION_2,
			"CACAGCCGTTCTTTGAGGCTGGTGCCTCTGCGGGCAAACTTCTTTGGAATTCGTCCTCCAGAGAAGGTGGACTGCAGATCCGCACGCTTTGCGCTGAGCTAAAATGAGTTCATTGATCACCTTCTGCTTCTCCACCAGCATCTTGTCCTTCTCGGAGTCCAGCCCATCCAGCTCAGAAGGAACAGAGCCCCCATTCAGGCTGCT");

	}

	@Test
	public void testTranslocationOrientation3() throws Exception {
		createStandardObject(2);
		records.add(new BLATRecord.Builder("100\t0\t0\t0\t0\t0\t0\t0\t+\tsplitcon_xxx_chr10_xxx_13231026_xxx_chr17_xxx_12656200_xxx_3_xxx_true\t204\t0\t100\tchr17\t81195210\t12656099\t12656199\t1\t100,\t0,\t12656099,\t").build());
		records.add(new BLATRecord.Builder("100\t0\t0\t0\t0\t0\t0\t0\t-\tsplitcon_xxx_chr10_xxx_13231026_xxx_chr17_xxx_12656200_xxx_3_xxx_true\t204\t104\t204\tchr10\t135534747\t13230926\t13231026\t1\t100,\t0,\t13230926,\t").build());

		testOrientationCategory("chr10", "chr17", 13231026, 12656200, QSVConstants.ORIENTATION_3,
			"AGCAGCCTGAATGGGGGCTCTGTTCCTTCTGAGCTGGATGGGCTGGACTCCGAGAAGGACAAGATGCTGGTGGAGAAGCAGAAGGTGATCAATGAACTCAAAAACACAGCCGTTCTTTGAGGCTGGTGCCTCTGCGGGCAAACTTCTTTGGAATTCGTCCTCCAGAGAAGGTGGACTGCAGATCCGCACGCTTTGCGCTGAGCT");
	}

	@Test
	public void testTranslocationOrientation4() throws Exception {
		createStandardObject(2);
		records.add(new BLATRecord.Builder("100\t0\t0\t0\t0\t0\t0\t0\t-\tsplitcon_xxx_chr10_xxx_13230927_xxx_chr17_xxx_12656100_xxx_4_xxx_false\t204\t0\t100\tchr17\t81195210\t12656099\t12656199\t1\t100,\t104,\t12656099,\t").build());
		records.add(new BLATRecord.Builder("101\t0\t0\t0\t0\t0\t0\t0\t+\tsplitcon_xxx_chr10_xxx_13230927_xxx_chr17_xxx_12656100_xxx_4_xxx_false\t204\t103\t204\tchr10\t135534747\t13230925\t13231026\t1\t101,\t103,\t13230925,\t").build());
		testOrientationCategory("chr10", "chr17", 13230927, 12656100, QSVConstants.ORIENTATION_4,
			"TGAGTTCATTGATCACCTTCTGCTTCTCCACCAGCATCTTGTCCTTCTCGGAGTCCAGCCCATCCAGCTCAGAAGGAACAGAGCCCCCATTCAGGCTGCTAAAAAGCTCAGCGCAAAGCGTGCGGATCTGCAGTCCACCTTCTCTGGAGGACGAATTCCAAAGAAGTTTGCCCGCAGAGGCACCAGCCTCAAAGAACGGCTGTG");
	}
	private void testOrientationCategory(String leftReference, String rightReference,
			int leftBreakpoint, int rightBreakpoint, String orientation, String consensus) throws Exception {
		splitReadContig = new SplitReadContig(new TIntObjectHashMap<int[]>(), p, leftReference, rightReference, leftBreakpoint,
				rightBreakpoint, orientation);
		splitReadContig.setConsensus(consensus);
		splitReadContig.setConfidenceLevel(QSVConstants.LEVEL_HIGH);
		splitReadContig.parseConsensusAlign(records);
		assertTrue(splitReadContig.getIsPotentialSplitRead());
		assertEquals(splitReadContig.getOrientationCategory(), orientation);
	}


}
