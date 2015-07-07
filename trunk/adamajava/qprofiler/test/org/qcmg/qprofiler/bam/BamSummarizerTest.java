package org.qcmg.qprofiler.bam;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class BamSummarizerTest {
	private static final String SAM_INPUT_FILE = "testInputFile.sam";
	private static final String SAM_DODGY_INPUT_FILE = "testInputFileDodgy.sam";

	@Before
	public void setup() {
		createTestSamFile(SAM_INPUT_FILE, createValidSamData());
	}

	@After
	public void tearDown() {
		File outputFile = new File(SAM_INPUT_FILE);
		boolean deleted = outputFile.delete();
		Assert.assertTrue(deleted);
	}

	@Test
	public void testSummarize() throws Exception {
		BamSummarizer bs = new BamSummarizer();
		BamSummaryReport sr = (BamSummaryReport) bs.summarize(new File(
				SAM_INPUT_FILE));

		Assert.assertNotNull(sr);
		
		Assert.assertEquals(5, sr.getRecordsParsed());		// should be 5 records
		testSummaryReport(sr);
	}
	
	@Test
	public void testSummarizeMaxRecords() throws Exception {
		for (int i = 1 ; i < 6 ; i++) {
			BamSummarizer bs = new BamSummarizer(null, i, null, null, null, null);
			BamSummaryReport sr = (BamSummaryReport) bs.summarize(new File(
					SAM_INPUT_FILE));

			Assert.assertNotNull(sr);
			Assert.assertEquals(i, sr.getRecordsParsed());
		}
		
		// test with 0 value - should return everything
		BamSummarizer bs = new BamSummarizer(null, 0, null, null, null, null);
		BamSummaryReport sr = (BamSummaryReport) bs.summarize(new File(
				SAM_INPUT_FILE));
		
		Assert.assertNotNull(sr);
		Assert.assertEquals(5, sr.getRecordsParsed());
	}
	
	@Test
	public void testSummarizeWithExcludesAll() throws Exception {
		String[] args = new String[] {};
		// no excludes defined - should return everything
		BamSummarizer bs = new BamSummarizer(args, 0, null, null, null, null);
		BamSummaryReport sr = (BamSummaryReport) bs.summarize(new File(
				SAM_INPUT_FILE));
		
		Assert.assertNotNull(sr);
		Assert.assertEquals(5, sr.getRecordsParsed());
		testSummaryReport(sr);
		
		// all - collections should be null
		String[] args2 = new String[] {"all"};
		// no excludes defined - should return everything
		bs = new BamSummarizer(args2, 0, null, null, null, null);
		sr = (BamSummaryReport) bs.summarize(new File(
				SAM_INPUT_FILE));
		
		Assert.assertNotNull(sr);
		Assert.assertEquals(5, sr.getRecordsParsed());
	}
	
	@Test
	public void testSummarizeWithExcludeCoverage() throws Exception {
		// first check we are getting coverage info
		BamSummarizer bs = new BamSummarizer();
		BamSummaryReport sr = (BamSummaryReport) bs.summarize(new File(
				SAM_INPUT_FILE));
		
		Assert.assertNotNull(sr);
		Assert.assertTrue(sr.getCoverage().isEmpty());
		
		// great - now lets exclude it!
		String[] args = new String[] {"coverage"};
		bs = new BamSummarizer(args, 0, null, null, null, null);
		sr = (BamSummaryReport) bs.summarize(new File(
				SAM_INPUT_FILE));
		
		Assert.assertNotNull(sr);
		Assert.assertFalse(sr.getCoverage().isEmpty());
		// check matrix info is still there
		Assert.assertTrue(sr.getMapQMatrix().isEmpty());
	}
	
	@Test
	public void testSummarizeWithIncludeMatrices() throws Exception {
		// first check we are getting coverage info
		BamSummarizer bs = new BamSummarizer();
		BamSummaryReport sr = (BamSummaryReport) bs.summarize(new File(
				SAM_INPUT_FILE));
		
		Assert.assertNotNull(sr);
		Assert.assertTrue(sr.getMapQMatrix().isEmpty());
		
		// great - now lets include it!
		bs = new BamSummarizer(new String[] {"matrices"}, 0, null, null, null, null);
		sr = (BamSummaryReport) bs.summarize(new File(SAM_INPUT_FILE));
		
		Assert.assertNotNull(sr);
		Assert.assertFalse(sr.getMapQMatrix().isEmpty());
		// check coverage info is still there
		Assert.assertTrue(sr.getCoverage().isEmpty());
	}

	private void testSummaryReport(BamSummaryReport sr) {
		// ceegars
		Assert.assertEquals(1, sr.getCigarValuesCount().get("13H").get());
		Assert.assertEquals(1, sr.getCigarValuesCount().get("15H").get());
		Assert.assertEquals(1, sr.getCigarValuesCount().get("8H").get());
		Assert.assertEquals(1, sr.getCigarValuesCount().get("22H").get());
		Assert.assertEquals(1, sr.getCigarValuesCount().get("10H").get());
		
		// seq by cycle
		// position 1
		Assert.assertEquals(1, sr.getSeqByCycle().count(1, 'A').get());
		Assert.assertEquals(2, sr.getSeqByCycle().count(1, 'T').get());
		Assert.assertEquals(2, sr.getSeqByCycle().count(1, 'G').get());
		Assert.assertEquals(0, sr.getSeqByCycle().count(1, 'C').get());
		// position 26
		Assert.assertEquals(3, sr.getSeqByCycle().count(26, 'T').get());
		Assert.assertEquals(1, sr.getSeqByCycle().count(26, 'C').get());
		Assert.assertEquals(1, sr.getSeqByCycle().count(26, 'G').get());
		
		// tags....
		// cs
		Assert.assertEquals(3, sr.getTagCSByCycle().count(1, '1').get());
		Assert.assertEquals(3, sr.getTagCSByCycle().count(2, '0').get());
		//cs line lengths
		Assert.assertEquals(5, sr.getTagCSLineLengths().get(Integer.valueOf(50)).get());
		
		// cq
		Assert.assertEquals(1, sr.getTagCQByCycle().count(1, 2).get());
		Assert.assertEquals(1, sr.getTagCQByCycle().count(2, 32).get());
		//cq line lengths
		Assert.assertEquals(5, sr.getTagCQLineLengths().get(Integer.valueOf(50)).get());
		
		// rg
		Assert.assertEquals(5, sr.getTagRGLineLengths().get("1959T").get());
		
		// zm - none of these in sample data as yet...
		
		int noOfPositionsWithPositiveCounts = 0;
		for (int i = 0 ; i < sr.getTagZMLineLengths().length() ; i++) {
			if (sr.getTagZMLineLengths().get(i) > 0) noOfPositionsWithPositiveCounts++;
		}
		Assert.assertEquals(0, noOfPositionsWithPositiveCounts);
		
		// something else..
		
	}

	@Test
	public void testSummarizeMissingData() throws Exception {
		createDodgyDataFile(createSamDataMissingData());

		BamSummarizer qs = new BamSummarizer();
		try {
			qs.summarize(new File(SAM_DODGY_INPUT_FILE));
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().startsWith("Error parsing text SAM file. Not enough fields"));
		}

		deleteDodgyDataFile();
	}

	@Test
	public void testSummarizeEmptyFile() throws Exception {
		createDodgyDataFile(new ArrayList<String>());

		BamSummarizer qs = new BamSummarizer();
		try {
			qs.summarize(new File(SAM_DODGY_INPUT_FILE));
//			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
//			Assert.assertEquals("Input files must be coordinate sorted", e.getMessage());
		}

		deleteDodgyDataFile();
	}

	@Ignore
	public void testSummarizeExtraData() throws Exception {
		createDodgyDataFile(createSamDataExtraData());

		/*
		 * This no longer triggers an exception as we are now setting the validation stringency to be lenient (or silent)
		 * this is due to the Illumina bams having non-zero mapq values for unmapped reads, which picard complains about
		 * unfortunately, setting this flag means we miss other problems with the reads such as those described in this test
		 */
		
		BamSummarizer qs = new BamSummarizer();
		try {
			qs.summarize(new File(SAM_DODGY_INPUT_FILE));
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().startsWith("Error parsing text SAM file. Not enough fields in tag"));
		}

		deleteDodgyDataFile();
	}
	@Test
	public void testSummarizeExtraDataBWA() throws Exception {
		createDodgyDataFile(createSamDataMissingDataBWA());
		
		/*
		 * This no longer triggers an exception as we are now setting the validation stringency to be lenient (or silent)
		 * this is due to the Illumina bams having non-zero mapq values for unmapped reads, which picard complains about
		 * unfortunately, setting this flag means we miss other problems with the reads such as those described in this test
		 */
		
		BamSummarizer qs = new BamSummarizer();
		try {
			qs.summarize(new File(SAM_DODGY_INPUT_FILE));
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().startsWith("Error parsing text SAM file. Not enough fields"));
		}
		
		deleteDodgyDataFile();
	}

	@Ignore
	public void testSummarizeNoHeader() throws Exception {
		createDodgyDataFile(createSamDataBody());
		
		/*
		 * This no longer triggers an exception as we are now setting the validation stringency to be lenient (or silent)
		 * this is due to the Illumina bams having non-zero mapq values for unmapped reads, which picard complains about
		 * unfortunately, setting this flag means we miss other problems with the reads such as those described in this test
		 */

		BamSummarizer qs = new BamSummarizer();
		try {
			qs.summarize(new File(SAM_DODGY_INPUT_FILE));
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
//			Assert.fail("Should NOT have thrown an Exception");
			// if there is no header details, defaults to unsorted hence strange error message
//			Assert.assertEquals("Input files must be coordinate sorted", e.getMessage());
		}

		deleteDodgyDataFile();
	}

	private void deleteDodgyDataFile() {
		File outputFile = new File(SAM_DODGY_INPUT_FILE);
		boolean deleted = outputFile.delete();
		Assert.assertTrue(deleted);
	}

	private void createDodgyDataFile(List<String> dodgyData) {
		createTestSamFile(SAM_DODGY_INPUT_FILE, dodgyData);
	}
	private void createDodgyDataFileBWA(List<String> dodgyData) {
		createTestSamFile(SAM_DODGY_INPUT_FILE, dodgyData);
	}

	private static List<String> createValidSamData() {
		List<String> data = new ArrayList<String>();
		
		for (String dataEntry : createSamDataHeader()) {
			data.add(dataEntry);
		}
		data.add("@CO	called by createValidSamData()");
		
		for (String dataEntry : createSamDataBody()) {
			data.add(dataEntry);
		}
		
		return data;
	}

	private static List<String> createSamDataHeader() {
		List<String> data = new ArrayList<String>();
		data.add("@HD	VN:1.0	SO:coordinate");
		data.add("@RG	ID:1959T	SM:eBeads_20091110_CD	DS:rl=50");
		data.add("@PG	ID:SOLID-GffToSam	VN:1.4.");
		data.add("@SQ	SN:chr1	LN:249250621");
		data.add("@CO	Test SAM file for use by BamSummarizerTest.java");
		return data;
	}
	private static List<String> createSamDataHeaderBWA() {
		List<String> data = new ArrayList<String>();
		data.add("@HD	VN:1.0	SO:coordinate");
		data.add("@RG	ID:1959T	SM:eBeads_20091110_CD	DS:rl=50");
		data.add("@PG	ID:bwa	PN:bwa	VN:1.4.");
		data.add("@SQ	SN:chr1	LN:249250621");
		data.add("@CO	Test SAM file for use by BamSummarizerTest.java");
		return data;
	}
	
//	private static List<String> createSamDataBody() {
//		List<String> data = new ArrayList<String>();
//		//reverse
//		data.add("243_146_202	115	chr1	10075	6	13H37M	=	10167	142	" +
//				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
//				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
//		//reverse
//		data.add("642_1887_1862	83	chr1	10167	1	15H35M	=	10176	59	" +
//				"CCTAACNCTAACCTAACCCTAACCCTAACCCTAAC	.(01(\"!\"&####07=?$$246/##<>,($3HC3+	RG:Z:1959T	" +
//				"CS:Z:T11032031032301032201032311322310320133320110020210	CQ:Z:#)+90$*(%:##').',$,4*.####$#*##&,%$+$,&&)##$#'#$$)");
//		//forward
//		data.add("970_1290_1068	163	chr1	10176	3	42M8H	=	10167	-59	" +
//				"AACCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTA	I&&HII%%IIII4CII=4?IIF0B((!!7F@+129G))I>.6	RG:Z:1959T	" +
//				"CS:Z:G202023020023010023010023000.2301002302002330000000	CQ:Z:@A&*?=9%;?:A-(<?8&/1@?():(9!,,;&&,'35)69&)./?11)&=");
//		//reverse
//		data.add("681_1482_392	115	chr1	10236	20	10H40M	=	10242	56	" +
//				"AACCCTAACCCTAAACCCTAAACCCTAACCCTAACCCTAA	IIIIIIIIEBIIIIFFIIIIIIIIIIIIIIIIIIIIIIII	RG:Z:1959T	" +
//				"CS:Z:T00320010320010320010032001003200103200100320000320	CQ:Z::<=>:<==8;<;<==9=9?;5>8:<+<;795.89>2;;8<:.78<)1=5;");
//		//reverse
//		data.add("1997_1173_1256	177	chr1	10242	100	22H28M	chr1	10236	0	" +
//				"AACCCTAAACCCTAAACCCTAACCCTAA	IIII27IICHIIIIHIIIHIIIII$$II	RG:Z:1959T	" +
//				"CS:Z:G10300010320010032001003000100320000032000020001220	CQ:Z:5?8$2;>;:458=27597:/5;7:2973:3/9;18;6/:5+4,/85-,'(");
//		return data;
//	}
	
	//updated qprofiler won't parse same strand pairs, so change it
	private static List<String> createSamDataBody() {
		List<String> data = new ArrayList<String>();
		//reverse
		data.add("243_146_202	83	chr1	10075	6	13H37M	=	10167	142	" +
				"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
				"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
		//reverse
		data.add("642_1887_1862	83	chr1	10167	1	15H35M	=	10176	59	" +
				"CCTAACNCTAACCTAACCCTAACCCTAACCCTAAC	.(01(\"!\"&####07=?$$246/##<>,($3HC3+	RG:Z:1959T	" +
				"CS:Z:T11032031032301032201032311322310320133320110020210	CQ:Z:#)+90$*(%:##').',$,4*.####$#*##&,%$+$,&&)##$#'#$$)");
		//forward
		data.add("970_1290_1068	163	chr1	10176	3	42M8H	=	10167	-59	" +
				"AACCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTA	I&&HII%%IIII4CII=4?IIF0B((!!7F@+129G))I>.6	RG:Z:1959T	" +
				"CS:Z:G202023020023010023010023000.2301002302002330000000	CQ:Z:@A&*?=9%;?:A-(<?8&/1@?():(9!,,;&&,'35)69&)./?11)&=");
		//reverse
		data.add("681_1482_392	83	chr1	10236	20	10H40M	=	10242	56	" +
				"AACCCTAACCCTAAACCCTAAACCCTAACCCTAACCCTAA	IIIIIIIIEBIIIIFFIIIIIIIIIIIIIIIIIIIIIIII	RG:Z:1959T	" +
				"CS:Z:T00320010320010320010032001003200103200100320000320	CQ:Z::<=>:<==8;<;<==9=9?;5>8:<+<;795.89>2;;8<:.78<)1=5;");
		//reverse
		data.add("1997_1173_1256	83	chr1	10242	100	22H28M	chr1	10236	0	" +
				"AACCCTAAACCCTAAACCCTAACCCTAA	IIII27IICHIIIIHIIIHIIIII$$II	RG:Z:1959T	" +
				"CS:Z:G10300010320010032001003000100320000032000020001220	CQ:Z:5?8$2;>;:458=27597:/5;7:2973:3/9;18;6/:5+4,/85-,'(");
		return data;
	}

	private static List<String> createSamDataMissingData() {
		List<String> data = new ArrayList<String>();
		for (String dataEntry : createSamDataHeader()) {
			data.add(dataEntry);
		}
		data.add("@CO	called by createSamDataMissingData()");
		
		data.add("429_1353_1176	171	chr1	10245	29	47M3H");
		return data;
	}
	private static List<String> createSamDataMissingDataBWA() {
		List<String> data = new ArrayList<String>();
		for (String dataEntry : createSamDataHeaderBWA()) {
			data.add(dataEntry);
		}
		data.add("@CO	called by createSamDataMissingData()");
		
		data.add("429_1353_1176	171	chr1	10245	29	47M3H");
		return data;
	}

	private static List<String> createSamDataExtraData() {
		List<String> data = new ArrayList<String>();
		for (String dataEntry : createSamDataHeader()) {
			data.add(dataEntry);
		}
		data.add("@CO	called by createSamlDataExtraData()");
		
		for (String dataEntry : createSamDataBody()) {
			data.add(dataEntry);
		}
		
		data.add("429_1353_1176	171	chr1	10245	29	47M3H	*	0	0	" +
				"CCTAAACCCTAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCC	IIIIIIIIIIIIIIIIIIGIIIGI=AIGDI=6HC((@>I((C))I?,	" +
				"RG:Z:1959T	CS:Z:G30230010023001002301002301002301002001000300000300	" +
				"CQ:Z:=A<=<:@=A;:=75<<969/><178&<9/68&18,(;&99(/5);:&'12" +
				"429_1353_1176	171	chr1	10245	29	47M3H	*	0	0	" +
				"CCTAAACCCTAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCC	IIIIIIIIIIIIIIIIIIGIIIGI=AIGDI=6HC((@>I((C))I?,	" +
				"RG:Z:1959T	CS:Z:G30230010023001002301002301002301002001000300000300	" +
				"CQ:Z:=A<=<:@=A;:=75<<969/><178&<9/68&18,(;&99(/5);:&'12" +
				"429_1353_1176	171	chr1	10245	29	47M3H	*	0	0	" +
				"CCTAAACCCTAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCC	IIIIIIIIIIIIIIIIIIGIIIGI=AIGDI=6HC((@>I((C))I?,	" +
				"RG:Z:1959T	CS:Z:G30230010023001002301002301002301002001000300000300	" +
				"CQ:Z:=A<=<:@=A;:=75<<969/><178&<9/68&18,(;&99(/5);:&'12");
		
		return data;
	}
	private static List<String> createSamDataExtraDataBWA() {
		List<String> data = new ArrayList<String>();
		for (String dataEntry : createSamDataHeaderBWA()) {
			data.add(dataEntry);
		}
		data.add("@CO	called by createSamlDataExtraData()");
		
		for (String dataEntry : createSamDataBody()) {
			data.add(dataEntry);
		}
		
		data.add("429_1353_1176	171	chr1	10245	29	47M3H	*	0	0	" +
				"CCTAAACCCTAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCC	IIIIIIIIIIIIIIIIIIGIIIGI=AIGDI=6HC((@>I((C))I?,	" +
				"RG:Z:1959T	CS:Z:G30230010023001002301002301002301002001000300000300	" +
				"CQ:Z:=A<=<:@=A;:=75<<969/><178&<9/68&18,(;&99(/5);:&'12" +
				"429_1353_1176	171	chr1	10245	29	47M3H	*	0	0	" +
				"CCTAAACCCTAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCC	IIIIIIIIIIIIIIIIIIGIIIGI=AIGDI=6HC((@>I((C))I?,	" +
				"RG:Z:1959T	CS:Z:G30230010023001002301002301002301002001000300000300	" +
				"CQ:Z:=A<=<:@=A;:=75<<969/><178&<9/68&18,(;&99(/5);:&'12" +
				"429_1353_1176	171	chr1	10245	29	47M3H	*	0	0	" +
				"CCTAAACCCTAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCC	IIIIIIIIIIIIIIIIIIGIIIGI=AIGDI=6HC((@>I((C))I?,	" +
				"RG:Z:1959T	CS:Z:G30230010023001002301002301002301002001000300000300	" +
		"CQ:Z:=A<=<:@=A;:=75<<969/><178&<9/68&18,(;&99(/5);:&'12");
		
		return data;
	}

	private static void createTestSamFile(String name, List<String> data) {
		String fileName = SAM_INPUT_FILE;
		if (null != name)
			fileName = name;

		PrintWriter out = null;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));

			for (String line : data) {
				out.println(line);
			}
			out.close();
		} catch (IOException e) {
			Logger.getLogger("BamSummarizerTest").log(
					Level.WARNING,
					"IOException caught whilst attempting to write to SAM test file: "
							+ fileName, e);
		} finally {
			out.close();
		}
	}

}
