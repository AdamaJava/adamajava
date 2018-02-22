package org.qcmg.qprofiler.fasta;

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
import org.junit.Test;

public class FastaSummarizerTest {
	private static final String FASTA_INPUT_FILE = "testInputFile.csfasta";
	private static final String FASTA_DODGY_INPUT_FILE = "testInputFileDodgy.csfasta";

	@Before
	public void setup() {
		createTestFastaFile(FASTA_INPUT_FILE, createValidFastaData());
	}

	@After
	public void tearDown() {
		File outputFile = new File(FASTA_INPUT_FILE);
		Assert.assertTrue(outputFile.delete());
	}

	@Test
	public void testSummarize() throws Exception {
		System.out.println("in testSummarize()");
		FastaSummarizer qs = new FastaSummarizer();
		FastaSummaryReport sr = (FastaSummaryReport) qs.summarize(FASTA_INPUT_FILE, null, null);

		Assert.assertNotNull(sr);
		Assert.assertEquals(2, sr.getFastaByCycle().count(1, '2').get());
		Assert.assertEquals(3, sr.getFastaByCycle().count(1, '3').get());
	}

	@Test
	public void testSummarizeMissingData() throws Exception {
//		System.out.println("in testSummarizeMissingData()");
		createDodgyDataFile(createFastaDataMissingData());

		FastaSummarizer qs = new FastaSummarizer();
		try {
			qs.summarize(FASTA_DODGY_INPUT_FILE, null, null);
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().startsWith("Bad sequence format"));
		}

		deleteDodgyDataFile();
	}

	@Test
	public void testSummarizeEmptyFile() throws Exception {
//		System.out.println("in testSummarizeEmptyFile()");
		createDodgyDataFile(new ArrayList<String>());

		FastaSummarizer qs = new FastaSummarizer();
		FastaSummaryReport sr = (FastaSummaryReport) qs.summarize(FASTA_DODGY_INPUT_FILE, null, null);
		Assert.assertEquals(0, sr.getRecordsParsed());

		deleteDodgyDataFile();
	}

	@Test
	public void testSummarizeExtraData() throws Exception {
//		System.out.println("in testSummarizeExtraData()");
		createDodgyDataFile(createFastalDataExtraData());

		FastaSummarizer qs = new FastaSummarizer();
		try {
			qs.summarize(FASTA_DODGY_INPUT_FILE, null, null);
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().startsWith("Bad id format"));
		}

		deleteDodgyDataFile();
	}
	
	@Test
	public void testSummarizeActualDuffData() throws Exception {
		System.out.println("in testSummarizeExtraData()");
		createDodgyDataFile(createActualDuffFastaData());
		
		FastaSummarizer qs = new FastaSummarizer();
		try {
			qs.summarize(FASTA_DODGY_INPUT_FILE, null, null);
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().startsWith("Bad id format"));
		}
		
		deleteDodgyDataFile();
	}
	
	@Test
	public void testSummarizeCrapData() throws Exception {
		System.out.println("in testSummarizeCrapData()");
		createDodgyDataFile(createFastaDataCrapBody());
		
		FastaSummarizer qs = new FastaSummarizer();
//		try {
//			FastaSummaryReport sr = (FastaSummaryReport) qs.summarize(new File(FASTA_DODGY_INPUT_FILE));
//			Assert.fail("Should have thrown an Exception");
//		} catch (Exception e) {
//			Assert.assertTrue(e.getMessage().startsWith("Bad id format"));
//		}
		FastaSummaryReport sr = (FastaSummaryReport) qs.summarize(FASTA_DODGY_INPUT_FILE, null, null);
		Assert.assertEquals(3, sr.getRecordsParsed());
		
		deleteDodgyDataFile();
	}
	
	@Test
	public void testSummarizeDifferentData() throws Exception {
		System.out.println("in testSummarizeDifferentData()");
		//TODO update method name - it is in fact non-dodgy!!!
		createDodgyDataFile(createFastaDataDodgyBody());
		
		FastaSummarizer qs = new FastaSummarizer();
		FastaSummaryReport sr = (FastaSummaryReport) qs.summarize(FASTA_DODGY_INPUT_FILE, null, null);
		Assert.assertEquals(5, sr.getRecordsParsed());
		
		deleteDodgyDataFile();
	}

	@Test
	public void testSummarizeNoHeader() throws Exception {
//		System.out.println("in testSummarizeNoHeader()");
		createDodgyDataFile(createFastaDataBody());

		FastaSummarizer qs = new FastaSummarizer();
		FastaSummaryReport sr = (FastaSummaryReport) qs.summarize(FASTA_DODGY_INPUT_FILE, null, null);
		Assert.assertEquals(5, sr.getRecordsParsed());
		Assert.assertEquals(5, sr.getRecordsParsed());

		deleteDodgyDataFile();
	}

	private void deleteDodgyDataFile() {
		File outputFile = new File(FASTA_DODGY_INPUT_FILE);
		Assert.assertTrue(outputFile.delete());
	}

	private void createDodgyDataFile(List<String> dodgyData) {
		createTestFastaFile(FASTA_DODGY_INPUT_FILE, dodgyData);
	}

	private static List<String> createValidFastaData() {
		List<String> data = new ArrayList<String>();
		
		for (String dataEntry : createFastaDataHeader()) {
			data.add(dataEntry);
		}
		data.add("# called by createValidFastaData()");
		
		for (String dataEntry : createFastaDataBody()) {
			data.add(dataEntry);
		}
		
		return data;
	}

	private static List<String> createFastaDataHeader() {
		List<String> data = new ArrayList<String>();
		data.add("# test csfasta file");
		data.add("# auto-generated from FastaSummarizerTest.java");
		return data;
	}
	
	private static List<String> createFastaDataBody() {
		List<String> data = new ArrayList<String>();
		data.add(">2_1110_310_F3");
		data.add("T22001202300110203202312222020302022200203300000200");
		data.add(">2_28_553_F3");
		data.add("T320.001021110223220200.022232.000.2022.02220000.0.");
		data.add(">2_1110_341_F3");
		data.add("T22221133020220231110222030230232330223332333203333");
		data.add(">2_26_633_F3");
		data.add("T300.1.002100.202020232.0020...222.2000.0202.000...");
		data.add(">2_1110_389_F3");
		data.add("T30110313212323101100021000102000023030002203003200");
		return data;
	}
	
	private static List<String> createFastaDataDodgyBody() {
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
	
	private static List<String> createFastaDataCrapBody() {
		List<String> data = new ArrayList<String>();
		data.add(">2_1110_310_F3");
		data.add("G220012023001102032<<<<<!@#$%^&*()!@#!@#!@#!@#");
		data.add(">2_28_553_F3");
		data.add("G300.1.002100.202020232.0020...222.2000.0202.000...");
		data.add(">2_1110_389_F3");
		data.add("G30110313212323101100021000102000023030002203003200");
		return data;
	}
	
	private static List<String> createActualDuffFastaData() {
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

	private static List<String> createFastaDataMissingData() {
		List<String> data = new ArrayList<String>();
		for (String dataEntry : createFastaDataHeader()) {
			data.add(dataEntry);
		}
		data.add("# called by createFastaDataMissingData()");
		
		data.add(">id_123");
		data.add(">id_234");
		data.add(">id_345");
		data.add(">id_456");
		data.add(">id_567");
		return data;
	}

	private static List<String> createFastalDataExtraData() {
		List<String> data = new ArrayList<String>();
		for (String dataEntry : createFastaDataHeader()) {
			data.add(dataEntry);
		}
		data.add("# called by createFastalDataExtraData()");
		
		data.add("T332.22112122.032100222.0021.2.020.3000.3203.030...");
		data.add("T322.2.10.202.000020302.0002...002.2002.0022.002...");
		data.add("T21213332221232222222222232232322222233333322333222");
		data.add("T21020020022300231002202003000002020022200202003220");
		
		for (String dataEntry : createFastaDataBody()) {
			data.add(dataEntry);
		}
		
		return data;
	}

	private static void createTestFastaFile(String name, List<String> data) {
		String fileName = FASTA_INPUT_FILE;
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
			Logger.getLogger("FastaSummarizerTest").log(
					Level.WARNING,
					"IOException caught whilst attempting to write to FASTA test file: "
							+ fileName, e);
		}
	}

}
