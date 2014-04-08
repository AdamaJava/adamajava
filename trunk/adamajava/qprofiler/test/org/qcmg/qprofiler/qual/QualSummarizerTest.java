package org.qcmg.qprofiler.qual;

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


public class QualSummarizerTest {
	private static final String QUAL_INPUT_FILE = "testInputFile.qual";
	private static final String QUAL_DODGY_INPUT_FILE = "testInputFileDodgy.qual";
	
	@Before
	public void setup() {
		createTestQualFile(QUAL_INPUT_FILE, createValidQualData());
	}
	
	@After
	public void tearDown() {
		File outputFile = new File(QUAL_INPUT_FILE);
		Assert.assertTrue(outputFile.delete());
	}
	
	@Test
	public void testSummarize() throws Exception {
		QualSummarizer qs = new QualSummarizer();
		QualSummaryReport sr = (QualSummaryReport) qs.summarize(new File(QUAL_INPUT_FILE));
		
		Assert.assertNotNull(sr);
		Assert.assertEquals(5, sr.getQualByCycle().count(1, 1).get());
	}
	
	@Test
	public void testSummarizeWithActualDuffData() throws Exception {
		createDodgyDataFile(createActualDuffQualData());
		QualSummarizer qs = new QualSummarizer();
		try {
			qs.summarize(new File(QUAL_DODGY_INPUT_FILE));
			Assert.fail("Should have thrown an exception");
		} catch (Exception e ) {
			Assert.assertTrue(e.getMessage().startsWith("Bad id"));
		}
		
		// when running with "-exclude all" option, should throw a different exception
		qs = new QualSummarizer(new String[] {"all"});
		try {
			qs.summarize(new File(QUAL_DODGY_INPUT_FILE));
			Assert.fail("Should have thrown an exception");
		} catch (Exception e ) {
//			e.printStackTrace();
			Assert.assertTrue(e.getMessage().startsWith("Bad id format"));
		}
		
//		Assert.assertNotNull(sr);
//		Assert.assertEquals(5, sr.getQualByCycle().count(1, 1).get());
	}
	
	@Test
	public void testSummarizeMissingData() throws Exception {
		createDodgyDataFile(createQualDataMissingData());
		
		QualSummarizer qs = new QualSummarizer();
		try {
			qs.summarize(new File(QUAL_DODGY_INPUT_FILE));
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {
//			e.printStackTrace();
			Assert.assertTrue(e.getMessage().startsWith("Bad sequence format"));
		}
//		Assert.assertEquals(0, sr.getRecordsParsed());
		
		deleteDodgyDataFile();
	}
	
	@Test
	public void testSummarizeCrapData() throws Exception {
		createDodgyDataFile(createQualDataCrapData());
		
		QualSummarizer qs = new QualSummarizer();
		try {
			qs.summarize(new File(QUAL_DODGY_INPUT_FILE));
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {
		}
		
		deleteDodgyDataFile();
	}
	
	@Test
	public void testSummarizeEmptyFile() throws Exception {
		createDodgyDataFile(new ArrayList<String>());
		
		QualSummarizer qs = new QualSummarizer();
//		try {
		QualSummaryReport sr = (QualSummaryReport) qs.summarize(new File(QUAL_DODGY_INPUT_FILE));
//			Assert.fail("Should have thrown an exception");
//		} catch (Exception e) {
//			Assert.assertTrue(e.getMessage().startsWith("Bad id"));
//		}
		Assert.assertEquals(0, sr.getRecordsParsed());
		
		deleteDodgyDataFile();
	}
	
	@Test
	public void testSummarizeExtraData() throws Exception {
		createDodgyDataFile(createQualDataExtraData());
		
		QualSummarizer qs = new QualSummarizer();
		try {
			qs.summarize(new File(QUAL_DODGY_INPUT_FILE));
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().startsWith("Bad id format:"));
		}
//		Assert.assertEquals(1, sr.getRecordsParsed());
		
		deleteDodgyDataFile();
	}
	
	@Test
	public void testSummarizeNoHeader() throws Exception {
		createDodgyDataFile(createQualDataNoHeader());
		
		QualSummarizer qs = new QualSummarizer();
		QualSummaryReport sr = (QualSummaryReport) qs.summarize(new File(QUAL_DODGY_INPUT_FILE));
		Assert.assertEquals(5, sr.getRecordsParsed());
		
		deleteDodgyDataFile();
	}
	
	private void deleteDodgyDataFile() {
		File outputFile = new File(QUAL_DODGY_INPUT_FILE);
		Assert.assertTrue(outputFile.delete());
	}
	private void createDodgyDataFile(List<String> dodgyData) {
		createTestQualFile(QUAL_DODGY_INPUT_FILE, dodgyData);
	}
	
	private static List<String> createValidQualData() {
		List<String> data = new ArrayList<String>();
		data.add("# test qual file");
		data.add("# auto-generated from QualSummarizerTest.java");
		data.add("> id_123");
		data.add("1 2 3 4 5 6 7 8 9 10");
		data.add("> id_234");
		data.add("1 2 3 4 5 6 7 8 9 10");
		data.add("> id_345");
		data.add("1 2 3 4 5 6 7 8 9 10");
		data.add("> id_456");
		data.add("1 2 3 4 5 6 7 8 9 10");
		data.add("> id_567");
		data.add("1 2 3 4 5 6 7 8 9 10");
		return data;
	}
	
	private static List<String> createActualDuffQualData() {
		List<String> data = new ArrayList<String>();
		data.add("# test qual file");
		data.add("# auto-generated from QualSummarizerTest.java");
		
		data.add(">1767_1825_90_F3");
		data.add("30 33 33 33 32 25 30 30 22 27 29 30 31 25 29 28 30 21 30 31 25 30 26 22 32 13 29 12 30 28 28 27 11 25 27 27 27 30 30 21 28 16 13 14 29 25 16 10 15 17"); 
		data.add(">1767_1825_110_F3");
		data.add("29 8 25 12 5 7 4 5 23 5 8 18 9 4 5 5 22 5 16 14 17 9 6 9 10 4 5 4 11 4 13 6 17 5 8 13 16 6 8 5 11 18 8 5 9 5 5 5 12 6"); 
		data.add(">1767_1825_150_F3");
		data.add("9 21 6 11 19 10 4 7 4 12 26 24 12 4 24 4 19 7 6 13 6 10 13 4 25 12 18 5 8 4 17 7 4 6 8 7 5 5 17 7 4 9 17 4 4 4 8 8 13 9"); 
		data.add(">1767_1825_222_F3");
		data.add("5 4 6 4 16 5 7 7 7 4 7 8 9 9 4 7 4 13 8 8 8 4 7 23 5 6 4 6 5 20 8 10 6 6 4 6 7 7 5 6 7 12 7 4 4 4 4 9 4 4"); 
		data.add(">1767_1825_302_F3");
		data.add("14 24 28 31 28 26 30 29 32 27 26 30 33 33 29 28 17 32 27 25 14 32 31 31 26_1836_991_F3");
		data.add("32 25 29 28 32 25 29 32 29 26 33 30 33 28 31 26 30 27 31 31 33 31 32 32 25 30 33 30 31 33 32 27 32 33 27 33 32 30 33 33 31 31 32 32 29 33 28 26 28 28"); 
		data.add(">1767_1836_1060_F3");
		data.add("33 33 29 33 33 30 33 32 33 33 31 33 30 32 33 31 33 31 30 31 32 33 30 27 32 32 32 33 32 33 33 31 31 17 32 32 33 24 20 33 33 33 31 31 31 31 32 27 32 31"); 
		data.add(">1767_1836_1065_F3");
		data.add("32 33 32 32 33 28 26 30 32 31 32 32 30 32 32 14 27 30 30 33 31 26 32 29 32 22 29 28 21 24 20 27 28 29 13 30 32 30 22 28 30 13 31 28 23 29 28 21 30 30"); 
		data.add(">1767_1836_1129_F3");
		data.add("31 33 33 33 30 28 30 29 21 17 27 30 25 30 26 31 28 22 14 33 24 32 31 27 25 30 32 8 31 6 24 21 30 22 23 20 28 30 8 19 28 30 30 28 5 22 32 5 9 17"); 
		data.add(">1767_1836_1209_F3");
		data.add("32 32 29 30 28 25 32 23 18 29 32 33 26 25 31 31 31 27 30 23 25 31 27 23 23 28 26 29 8 8 18 27 26 9 5 28 30 9 10 21 23 25 22 8 5 19 23 18 4 14"); 
		data.add(">1767_1836_1334_F3");
		data.add("33 33 31 32 31 32 32 30 27 31 31 31 31 32 31 32 31 8 8 29 28 5 21 16 18 26 7 8 13 27 31 13 25 27 8 21 7 4 9 29 19 8 22 8 18 30 6 4 9 9");
		return data;
	}
	
	private static List<String> createQualDataNoHeader() {
		List<String> data = new ArrayList<String>();
		data.add("> id_123");
		data.add("1 2 3 4 5 6 7 8 9 10");
		data.add("> id_234");
		data.add("1 2 3 4 5 6 7 8 9 10");
		data.add("> id_345");
		data.add("1 2 3 4 5 6 7 8 9 10");
		data.add("> id_456");
		data.add("1 2 3 4 5 6 7 8 9 10");
		data.add("> id_567");
		data.add("1 2 3 4 5 6 7 8 9 10");
		return data;
	}
	
	private static List<String> createQualDataMissingData() {
		List<String> data = new ArrayList<String>();
		data.add("# test qual file");
		data.add("# auto-generated from QualSummarizerTest.java");
		data.add("> id_123");
		data.add("> id_234");
		data.add("> id_345");
		data.add("> id_456");
		data.add("> id_567");
		return data;
	}
	
	private static List<String> createQualDataCrapData() {
		List<String> data = new ArrayList<String>();
		data.add("# test qual file");
		data.add("# auto-generated from QualSummarizerTest.java");
		data.add("> id_123");
		data.add("1 2 3 4 5 6 7 8 9 10 abcdefg \t \n <null>");
		data.add("> id_234");
		data.add("> id_345");
		data.add("> id_456");
		data.add("> id_567");
		return data;
	}
	
	private static List<String> createQualDataExtraData() {
		List<String> data = new ArrayList<String>();
		data.add("# test qual file");
		data.add("# auto-generated from QualSummarizerTest.java");
		data.add("> id_123");
		data.add("1 2 3 4 5 6 7 8 9 10");
		data.add("1 2 3 4 5 6 7 8 9 10");
		data.add("1 2 3 4 5 6 7 8 9 10");
		data.add("> id_234");
		data.add("1 2 3 4 5 6 7 8 9 10");
		data.add("> id_345");
		data.add("1 2 3 4 5 6 7 8 9 10");
		data.add("> id_456");
		data.add("1 2 3 4 5 6 7 8 9 10");
		data.add("> id_567");
		data.add("1 2 3 4 5 6 7 8 9 10");
		data.add("1 2 3 4 5 6 7 8 9 10");
		return data;
	}
	
	private static void createTestQualFile(String name, List<String> data) {
		String fileName = QUAL_INPUT_FILE;
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
			Logger.getLogger("QualSummarizerTest").log(Level.WARNING, "IOException caught whilst attempting to write to QUAL test file: " + fileName, e);
		}
	}

}
