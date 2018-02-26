package org.qcmg.qprofiler.fastq;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;

public class FastqSummarizerTest {
	private static final QLogger logger = QLoggerFactory.getLogger(FastqSummarizerTest.class);
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	
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

	@Test
	public void testSummarize() throws Exception {
		logger.info("in testSummarize()");
		
		File f = testFolder.newFile("testSummarize.fasta");
		createTestFastqFile(f, createValidFastqData());
		FastqSummarizer qs = new FastqSummarizer();
		FastqSummaryReport sr = (FastqSummaryReport) qs.summarize(f.getAbsolutePath(), null, null);

		Assert.assertNotNull(sr);
		Assert.assertEquals(4, sr.getRecordsParsed());
		Assert.assertEquals(4, sr.getFastqBaseByCycle().count(1, 'G').get());
		Assert.assertEquals(2, sr.getFastqBaseByCycle().count(60, 'T').get());
	}

	@Test
	public void testSummarizeMissingData() throws Exception {
		logger.info("in testSummarizeMissingData()");
		File f = testFolder.newFile("testSummarizeMissingData.fasta");
		createDodgyDataFile(f, createFastqDataMissingData());

		FastqSummarizer qs = new FastqSummarizer();
		try {
			qs.summarize(f);
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().startsWith("Quality header must start with +:"));
		}
	}
	
	@Test
	public void testSummarizeDataOfDifferingLengths() throws Exception {
		logger.info("in testSummarizeMissingData()");
		File f = testFolder.newFile("testSummarizeDataOfDifferingLengths.fasta");
		createDodgyDataFile(f,createFastqDataOfDifferingLengths());
		
		FastqSummarizer qs = new FastqSummarizer();
		try {
			qs.summarize(f.getAbsolutePath(), null, null);
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			assertEquals(true, e.getMessage().startsWith("Sequence and quality line must be the same length"));
		}
	}

	@Test
	public void testSummarizeEmptyFile() throws Exception {
		logger.info("in testSummarizeEmptyFile()");
		File f = testFolder.newFile("testSummarizeEmptyFile.fasta");
		createDodgyDataFile(f, new ArrayList<String>());

		FastqSummarizer qs = new FastqSummarizer();
		FastqSummaryReport sr = (FastqSummaryReport) qs.summarize(f);
		Assert.assertEquals(0, sr.getRecordsParsed());
	}

	@Test
	public void testSummarizeExtraData() throws Exception {
		logger.info("in testSummarizeExtraData()");
		File f = testFolder.newFile("testSummarizeExtraData.fasta");
		createDodgyDataFile(f, createFastqDataExtraData());

		FastqSummarizer qs = new FastqSummarizer();
		try {
			qs.summarize(f);
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals(true, e.getMessage().startsWith("Sequence header must start with @:"));
		}
	}
	
	@Ignore
	public void testSummarizeActualDuffData() throws Exception {
		logger.info("in testSummarizeExtraData()");
		File f = testFolder.newFile("testSummarizeActualDuffData.fasta");
		createDodgyDataFile(f, createActualDuffFastqData());
		
		FastqSummarizer qs = new FastqSummarizer();
		try {
			qs.summarize(f);
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().startsWith("Bad id format"));
		}
	}
	
	@Test
	public void testSummarizeActualDataShouldWork() throws Exception {
		logger.info("in testSummarizeActualDataShouldWork()");
		File f = testFolder.newFile("testSummarizeActualDataShouldWork.fasta");
		createDodgyDataFile(f, createFastqDataBodyShouldWork());
		
		FastqSummarizer qs = new FastqSummarizer();
		qs.summarize(f);
	}
	
	@Test
	public void testSummarizeCrapData() throws Exception {
		logger.info("in testSummarizeCrapData()");
		File f = testFolder.newFile("testSummarizeCrapData.fasta");
		createDodgyDataFile(f, createFastqDataCrapBody());
		
		FastqSummarizer qs = new FastqSummarizer();
		try {
			qs.summarize(f);
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			//debug
			System.out.println("error message: " + e.getMessage());
			
			Assert.assertTrue(e.getMessage().length() > 0);
		}
	}
	
	@Ignore
	public void testSummarizeDifferentData() throws Exception {
		logger.info("in testSummarizeDifferentData()");
		File f = testFolder.newFile("testSummarizeDifferentData.fasta");
		createDodgyDataFile(f, createFastqDataDodgyBody());
		
		FastqSummarizer qs = new FastqSummarizer();
		FastqSummaryReport sr = (FastqSummaryReport) qs.summarize(f);
		Assert.assertEquals(5, sr.getRecordsParsed());
	}

	@Ignore
	public void testSummarizeNoHeader() throws Exception {
		logger.info("in testSummarizeNoHeader()");
		File f = testFolder.newFile("testSummarizeNoHeader.fasta");
		createDodgyDataFile(f, createFastqDataBody());

		FastqSummarizer qs = new FastqSummarizer();
		FastqSummaryReport sr = (FastqSummaryReport) qs.summarize(f);
		Assert.assertEquals(5, sr.getRecordsParsed());
		Assert.assertEquals(5, sr.getRecordsParsed());
	}

	private void createDodgyDataFile(File f, List<String> dodgyData) {
		createTestFastqFile(f, dodgyData);
	}

	private static List<String> createValidFastqData() {
		List<String> data = new ArrayList<>();
		
		for (String dataEntry : createFastqDataBody()) {
			data.add(dataEntry);
		}
		return data;
	}

	
	private static List<String> createFastqDataBody() {
		List<String> data = new ArrayList<>();
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
		List<String> data = new ArrayList<>();
		data.add("@ERR091788.1 HSQ955_155:2:1101:1473:2037/1");
		data.add("GGGCANCCAGCAGCCCTCGGGGCTTCTCTGTTTATGGAGTAGCCATTCTCGTATCCTTCTACTTTCTTAAACTTTCTTTCACTTACAAAAAAATAGTGGA");
		data.add("+");
		data.add("<@@DD#2AFFHHH<FHFF@@FEG@DF?BF4?FFGDIBC?B?=FHIEFHGGG@CGHIIHDHFHFECDEEEECCCCCCAC@CCC>CCCCCCBBBBAC>:@<C");
		return data;
	}
	
	private static List<String> createFastqDataDodgyBody() {
		List<String> data = new ArrayList<>();
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
		List<String> data = new ArrayList<>();
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
		List<String> data = new ArrayList<>();
		
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
		List<String> data = new ArrayList<>();
		
		data.add("@id_123");
		data.add("@id_234");
		data.add("@id_345");
		data.add("@id_456");
		data.add("@id_567");
		return data;
	}
	
	private static List<String> createFastqDataOfDifferingLengths() {
		List<String> data = new ArrayList<>();
		
		data.add("@id_123");
		data.add("ACGTACGT");
		data.add("+id_123");
		data.add("+@+@+@");
		return data;
	}

	private static List<String> createFastqDataExtraData() {
		List<String> data = new ArrayList<>();
		
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

	private static void createTestFastqFile(File file, List<String> data) {
		try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));){
			for (String line : data) {
				out.println(line);
			}
		} catch (IOException e) {
			logger.error("IOException caught whilst attempting to write to FASTQ test file: " +file.getAbsolutePath() , e);
		}
	}

}
