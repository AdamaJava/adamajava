package org.qcmg.qprofiler.fastq;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
//import htsjdk.samtools.PicardException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;

public class FastqSummarizerTest {
	private static final QLogger logger = QLoggerFactory.getLogger(FastqSummarizerTest.class);
	
	private static final String FASTQ_INPUT_FILE = "testInputFile.fastq";
	private static final String FASTQ_DODGY_INPUT_FILE = "testInputFileDodgy.fastq";
	
	
	/*
	 * @D81P8DQ1:196:C42UFACXX:5:1213:8088:93452/1
TTTTTTTAAATTTCCACTGATGATTTTGCTGCATGGCCGGTGTTGAGAATGACTGCGCAAATTTGCCGGATTTCCTTTGCTGTTCCTGCATGTAGTTTAA
+
:?14ADDFHHHHHJJIJJJJJJHIIIJJIJIIIJJJIJJIF@FHGJIHIIJJIEHA<EEFDDEEDEEDDBDDDCDDDCDCDDCCCDDDCCDCDDEEEEED
@D81P8DQ1:196:C42UFACXX:5:2201:12939:66917/1
GACGGAACCAGCACTGTGTGGAGACCAGCTTCAAGGAGCGGAAGGCTGGCTTGAGGCCACACCGCTGGGGCGGGGACCTCTGTCTGCCTGGCCTCCCCGG
+
=8?1+=?@B:DDD@:?2AFEFE=CE1)))CD<?<D>BD>?::@@AC3A<C7='();?5;?A#######################################
@D81P8DQ1:196:C42UFACXX:5:1314:3624:28820/1
CTTCAACAAGCTGGTCATGAGGCGCAAGGGCATCTCTGGGAAAGGACCTGGGGCTGGTGAGGGGCCCGGAGGAGCCTTTGCCCGCGTGTCAGACTCCATC
+
:??:ADDEHGGHFIFBFE@FB>EHEIIIFH6D;BGGHIIIHGGGIIGGI@ECHFEED?BBBCDD>@BBB;BBDBB@CC:CCC:>B@>BD@CCCAACCDDD
@D81P8DQ1:196:C42UFACXX:5:2111:19999:87082/1
CAGTTCTTTATTGATTGGTGTGCCGTTTTCTCTGGAAGCCTCTTAAGAACACTGTGGCGCAGGCTGGGTGGAGCCGTCCCCCCATGGAGCACAGGCAGAC
+
CCCFFFFFHGHHHIJJJIGHHIJJIHIJJJJGJJJJJJJJJJJJJJJJJGIJJIGHIGJJJJHHHGFFDDDDDDDBDDDDDDDDDDDDDDBDDCDDDDDD
@D81P8DQ1:196:C42UFACXX:5:2216:15317:57179/1
GTCGTTTTCTCTGGAAGCCTCTTAAGAACACAGTGGCGCAGGCTGGGTGGAGCCGTCCCCCCATGGAGCACAGGCAGACAGAAGTACCCGCCCCAGCTGT
+
?+1:==+ADDF4<?CCG3<FEFAD?FCH99?FFC<?C?@GFFDF>FC8-'(.755A459@8829?A:99??B?8((2<2?BBB9@(+4@5&)09??1:@#
@D81P8DQ1:196:C42UFACXX:5:1211:16920:58165/1
TCTTAAGAACACAGTGGCGCAGGCTGGGTGGAGCCGTCCCCCCATGGAGCACAGGCAGACAGAAGTCCCCGCCCCAGCTGTGTGGCCTCAAGCCAGCCTT
+
CC@FFFFFHHFFHIIGIJJJJIJIJIIJBGHIIIGIIGIIIJHFFFFFEDDEEDDDDDDDDDDDCCCDD@BDBDDDDBDDCC>ABDBCDCDDDDDDDDDD
@D81P8DQ1:196:C42UFACXX:5:1203:3547:60272/1
CCAGCTTCAAGGAGCGGAAGGCTGGCTTGAGGCCACACAGCTGGGGCGGGGAATTCTGTCTGCCTGTGCTCCATGGGGGGACGGCTCCACCCAGCCTGCG
+
@@CFBDDBCDB<DBHBHBFBAFHGIICGDG<B1DEFHC;F?FEGC@8A####################################################
@D81P8DQ1:196:C42UFACXX:5:2303:17874:93796/1
GCCGTTTTCTCTGGAAGCCTCTTAAGAACACAGTGGCGCAGGCTGGGTGGAGCCGTCCCCCCATGGAGCACAGGCAGACAGAAGTCCCCGCCCCAGCTGT
+
BCCFDFFFHHHHHJJJJIJJJJJIJIJJJJJJJGHJJJJJJJJJJIJFHIJJJHHHFFDDDDDCDCDDDDDDDDDDDDDDDDDD@CDDDDDDDDDBBCCA
	 */

	@Before
	public void setup() {
		createTestFastqFile(FASTQ_INPUT_FILE, createValidFastqData());
	}

	@After
	public void tearDown() {
		File outputFile = new File(FASTQ_INPUT_FILE);
		Assert.assertTrue(outputFile.delete());
	}

	@Test
	public void testSummarize() throws Exception {
		logger.info("in testSummarize()");
		FastqSummarizer qs = new FastqSummarizer();
		FastqSummaryReport sr = (FastqSummaryReport) qs.summarize(new File(
				FASTQ_INPUT_FILE));

		Assert.assertNotNull(sr);
		Assert.assertEquals(4, sr.getRecordsParsed());
		Assert.assertEquals(4, sr.getFastqBaseByCycle().count(1, 'G').get());
		Assert.assertEquals(2, sr.getFastqBaseByCycle().count(60, 'T').get());
	}

	@Test
	public void testSummarizeMissingData() throws Exception {
		logger.info("in testSummarizeMissingData()");
		createDodgyDataFile(createFastqDataMissingData());

		FastqSummarizer qs = new FastqSummarizer();
		try {
			qs.summarize(new File(FASTQ_DODGY_INPUT_FILE));
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().startsWith("Quality header must start with +:"));
		}

		deleteDodgyDataFile();
	}
	
	@Test
	public void testSummarizeDataOfDifferingLengths() throws Exception {
		logger.info("in testSummarizeMissingData()");
		createDodgyDataFile(createFastqDataOfDifferingLengths());
		
		FastqSummarizer qs = new FastqSummarizer();
		try {
			qs.summarize(new File(FASTQ_DODGY_INPUT_FILE));
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().startsWith("Sequence and quality line must be the same length"));
		}
		
		deleteDodgyDataFile();
	}

	@Test
	public void testSummarizeEmptyFile() throws Exception {
		logger.info("in testSummarizeEmptyFile()");
		createDodgyDataFile(new ArrayList<String>());

		FastqSummarizer qs = new FastqSummarizer();
		FastqSummaryReport sr = (FastqSummaryReport) qs.summarize(new File(FASTQ_DODGY_INPUT_FILE));
		Assert.assertEquals(0, sr.getRecordsParsed());

		deleteDodgyDataFile();
	}

	@Test
	public void testSummarizeExtraData() throws Exception {
		logger.info("in testSummarizeExtraData()");
		createDodgyDataFile(createFastqDataExtraData());

		FastqSummarizer qs = new FastqSummarizer();
		try {
			qs.summarize(new File(FASTQ_DODGY_INPUT_FILE));
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().startsWith("Sequence header must start with @:"));
		}

		deleteDodgyDataFile();
	}
	
	@Ignore
	public void testSummarizeActualDuffData() throws Exception {
		logger.info("in testSummarizeExtraData()");
		createDodgyDataFile(createActualDuffFastqData());
		
		FastqSummarizer qs = new FastqSummarizer();
		try {
			qs.summarize(new File(FASTQ_DODGY_INPUT_FILE));
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().startsWith("Bad id format"));
		}
		
		deleteDodgyDataFile();
	}
	
	@Test
	public void testSummarizeActualDataShouldWork() throws Exception {
		logger.info("in testSummarizeActualDataShouldWork()");
		createDodgyDataFile(createFastqDataBodyShouldWork());
		
		FastqSummarizer qs = new FastqSummarizer();
		qs.summarize(new File(FASTQ_DODGY_INPUT_FILE));
		
		deleteDodgyDataFile();
	}
	
	@Test
	public void testSummarizeCrapData() throws Exception {
		logger.info("in testSummarizeCrapData()");
		createDodgyDataFile(createFastqDataCrapBody());
		
		FastqSummarizer qs = new FastqSummarizer();
		try {
			qs.summarize(new File(FASTQ_DODGY_INPUT_FILE));
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().startsWith("Invalid fastq character"));
		}
//		FastqSummaryReport sr = (FastqSummaryReport) qs.summarize(new File(FASTQ_DODGY_INPUT_FILE));
//		Assert.assertEquals(3, sr.getRecordsParsed());
		
		deleteDodgyDataFile();
	}
	
	@Ignore
	public void testSummarizeDifferentData() throws Exception {
		logger.info("in testSummarizeDifferentData()");
		//TODO update method name - it is in fact non-dodgy!!!
		createDodgyDataFile(createFastqDataDodgyBody());
		
		FastqSummarizer qs = new FastqSummarizer();
		FastqSummaryReport sr = (FastqSummaryReport) qs.summarize(new File(FASTQ_DODGY_INPUT_FILE));
		Assert.assertEquals(5, sr.getRecordsParsed());
		
		deleteDodgyDataFile();
	}

	@Ignore
	public void testSummarizeNoHeader() throws Exception {
		logger.info("in testSummarizeNoHeader()");
		createDodgyDataFile(createFastqDataBody());

		FastqSummarizer qs = new FastqSummarizer();
		FastqSummaryReport sr = (FastqSummaryReport) qs.summarize(new File(
				FASTQ_DODGY_INPUT_FILE));
		Assert.assertEquals(5, sr.getRecordsParsed());
		Assert.assertEquals(5, sr.getRecordsParsed());

		deleteDodgyDataFile();
	}

	private void deleteDodgyDataFile() {
		File outputFile = new File(FASTQ_DODGY_INPUT_FILE);
		Assert.assertTrue(outputFile.delete());
	}

	private void createDodgyDataFile(List<String> dodgyData) {
		createTestFastqFile(FASTQ_DODGY_INPUT_FILE, dodgyData);
	}

	private static List<String> createValidFastqData() {
		List<String> data = new ArrayList<String>();
		
//		for (String dataEntry : createFastqDataHeader()) {
//			data.add(dataEntry);
//		}
//		data.add("# called by createValidFastqData()");
		
		for (String dataEntry : createFastqDataBody()) {
			data.add(dataEntry);
		}
		
		return data;
	}

//	private static List<String> createFastqDataHeader() {
//		List<String> data = new ArrayList<String>();
//		data.add("# test fastq file");
//		data.add("# auto-generated from FastqSummarizerTest.java");
//		return data;
//	}
	
	private static List<String> createFastqDataBody() {
		List<String> data = new ArrayList<String>();
		data.add("@2_1110_310_F3");
		data.add("GATTTGGGGTTCAAAGCAGTATCGATCAAATAGTAAATCCATTTGTTCAACTCACAGTTT");
		data.add("+2_1110_310_F3");
		data.add("+''*((((***+))%%%++)(%%%%).1***-+*''))**55CCF>>>>>>CCCCCCC65");
		data.add("@2_1234_999_F5");
		data.add("GATTTGGGGTTCAAAGCAGTATCGATCAAATAGTAAATCCATTTGTTCAACTCACAGTTT");
		data.add("+2_1234_999_F5");
		data.add("@''*((((***+))%%%++)(%%%%).1***-+*''))**55CCF>>>>>>CCCCCCC65");
		data.add("@ERR091788.1 HSQ955_155:2:1101:1473:2037/1");
		data.add("GGGTGATGGCCGCTGCCGATGGCGTCAAATCCCACC");
		data.add("+SRR001666.1 071112_SLXA-EAS1_s_7:5:1:817:345 length=36");
		data.add("@IIIIIIIIIIIIIIIIIIIIIIIIIIIII9IG9IC");
		data.add("@ERR091788.1 HSQ955_155:2:1101:1473:2037/1");
		data.add("GGGTGATGGCCGCTGCCGATGGCGTCAAATCCCACCACGT");
		data.add("+QCMG001666.1 071112_SLXA-EAS1_s_7:5:1:817:345 length=40");
		data.add("+IIIIIIIIIIIIIIIIIIIIIIIIIIIII9IG9IC++@@");
		return data;
	}
	private static List<String> createFastqDataBodyShouldWork() {
		List<String> data = new ArrayList<String>();
		data.add("@ERR091788.1 HSQ955_155:2:1101:1473:2037/1");
		data.add("GGGCANCCAGCAGCCCTCGGGGCTTCTCTGTTTATGGAGTAGCCATTCTCGTATCCTTCTACTTTCTTAAACTTTCTTTCACTTACAAAAAAATAGTGGA");
		data.add("+");
		data.add("<@@DD#2AFFHHH<FHFF@@FEG@DF?BF4?FFGDIBC?B?=FHIEFHGGG@CGHIIHDHFHFECDEEEECCCCCCAC@CCC>CCCCCCBBBBAC>:@<C");
		return data;
	}
	
	private static List<String> createFastqDataDodgyBody() {
		List<String> data = new ArrayList<String>();
		data.add(">2_1110_310_F3");
		data.add("G22001202300110203202312222020302022200203300000200");
		data.add(">2_28_553_F3");
		data.add("G320.001021110223220200.022232.000.2022.02220000.0.");
		data.add(">2_1110_341_F3");
		data.add("G22221133020220231110222030230232330223332333203333");
		data.add(">2_26_633_F3");
		data.add("G300.1.002100.202020232.0020...222.2000.0202.000...");
		data.add(">2_1110_389_F3");
		data.add("G30110313212323101100021000102000023030002203003200");
		return data;
	}
	
	private static List<String> createFastqDataCrapBody() {
		List<String> data = new ArrayList<String>();
		data.add("@2_1110_310_F3");
		data.add("G220012023001102032<<<<<!@#$%^&*()!@#!@#!@#!@#");
		data.add("+2_28_553_F3");
		data.add("1.002100.202020232.0020...222.2000.0202.000...");
		data.add("@2_1110_389_F3");
		data.add("G30110313212323101100021000102000023030002203003200");
		data.add("+2_1110_389_F3");
		data.add("G30110313212323101100021000102000023030002203003   ");
		return data;
	}
	
	//@TODO - update with actual fastq duff data when available
	private static List<String> createActualDuffFastqData() {
		List<String> data = new ArrayList<String>();
		
		data.add(">1766_862_327_F3");
		data.add("T01121112112121222300000031021231102012320213101000");
		data.add(">1766_862_333_F3");
		data.add("T3301122012033766_906_1097_F3");
		data.add("T00003120000302113130030230121132322121001010320002");
		data.add(">1766_906_1209_F3");
		data.add("T33101210102331013131032020031233020002202022003102");
		data.add(">1766_906_1278_F3");
		data.add("T01220232131030211101013132013110132131002033331200");
		data.add(">1766_906_1526_F3");
		data.add("T20010031232011020123300112100233003121221202002022");

		return data;
	}

	private static List<String> createFastqDataMissingData() {
		List<String> data = new ArrayList<String>();
		
		data.add("@id_123");
		data.add("@id_234");
		data.add("@id_345");
		data.add("@id_456");
		data.add("@id_567");
		return data;
	}
	
	private static List<String> createFastqDataOfDifferingLengths() {
		List<String> data = new ArrayList<String>();
		
		data.add("@id_123");
		data.add("ACGTACGT");
		data.add("+id_123");
		data.add("+@+@+@");
		return data;
	}

	private static List<String> createFastqDataExtraData() {
		List<String> data = new ArrayList<String>();
		
		data.add("@2_1110_310_F3");
		data.add("GATTTGGGGTTCAAAGCAGTATCGATCAAATAGTAAATCCATTTGTTCAACTCACAGTTT");
		data.add("+2_1110_310_F3");
		data.add("+''*((((***+))%%%++)(%%%%).1***-+*''))**55CCF>>>>>>CCCCCCC65");
		data.add("GATTTGGGGTTCAAAGCAGTATCGATCAAATAGTAAATCCATTTGTTCAACTCACAGTTT");
		
		for (String dataEntry : createFastqDataBody()) {
			data.add(dataEntry);
		}
		
		return data;
	}

	private static void createTestFastqFile(String name, List<String> data) {
		String fileName = FASTQ_INPUT_FILE;
		if (null != name)
			fileName = name;

		PrintWriter out;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));

			for (String line : data) {
				out.println(line);
			}
			out.close();
		} catch (IOException e) {
			logger.error("IOException caught whilst attempting to write to FASTQ test file: " +fileName , e);
		}
	}

}
