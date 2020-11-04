package org.qcmg.qio.gff;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qio.gff.GffReader;
import org.qcmg.qio.gff.GffRecord;

public class GffReaderTest {
	private static File EMPTY_FILE ;
	
	@ClassRule
	public static TemporaryFolder testFolder = new TemporaryFolder();
	
	@BeforeClass
	public static void setup() throws IOException {
		EMPTY_FILE  = testFolder.newFile("empty.gff");
 		createTestFile(EMPTY_FILE.getAbsolutePath(), new ArrayList<String>());	
	}

	@Test
	public void testParseDataInvalid() throws Exception {
		try(GffReader reader = new GffReader(EMPTY_FILE);){
		
			// test empty string
			try {
				reader.getRecord("");
				Assert.fail("Should have thrown an Exception");
			} catch (Exception e) {
				Assert.assertEquals("Not enough fields in the Record", e.getMessage());
			}
			try {
				reader.getRecord("								");
				Assert.fail("Should have thrown an Exception");
			} catch (Exception e) {
				Assert.assertEquals("Not enough fields in the Record", e.getMessage());
			}
			
			// test null
			try {
				reader.getRecord(null);
				Assert.fail("Should have thrown an Exception");
			} catch (AssertionError e) {
				Assert.assertEquals("Record was null", e.getMessage());
			}
			
			// string with fewer than 8 entries
			try {
				reader.getRecord("1	2	3	4	5	6	");
				Assert.fail("Should have thrown an Exception");
			} catch (Exception e) {
				Assert.assertEquals("Not enough fields in the Record", e.getMessage());
			}
		}
	}
	
	@Test
	public void testParseRecordInvalid() throws Exception {
		
		GffReader reader = new GffReader(EMPTY_FILE);		
		// test null
		try {
			reader.getRecord(null);
			Assert.fail("Should have thrown an exception");
		} catch (AssertionError e) {
			Assert.assertEquals("Record was null", e.getMessage());
		}
		// test empty string
		try {
			reader.getRecord("");
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals("Not enough fields in the Record", e.getMessage());
		}
		try {
			reader.getRecord("								");
			Assert.fail("Should have thrown an Exception");
		} catch (Exception e) {
			Assert.assertEquals("Not enough fields in the Record", e.getMessage());
		}
		
		reader.close();
	}
	
	@Test
	public void testParseRecord() throws Exception {
		try (GffReader reader = new GffReader(EMPTY_FILE);){
			// 8 values
			GffRecord record = reader.getRecord("this	is	a	0	1	0.0	works	OK");
			Assert.assertNotNull(record);
			Assert.assertEquals("this", record.getSeqname());
			Assert.assertEquals("OK", record.getFrame());		
		}
	}
	
	@Test
	public void testParseRecordWithAttributes() throws Exception {
		try (GffReader reader = new GffReader(EMPTY_FILE);	){
			
			// real record containing attributes
			GffRecord record = reader.getRecord("1	solid	read	10148	10190	14.4	-	.	" +
					"aID=1212_1636_246;at=F3;b=GGTTAGGGTTAGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG;" +
					"g=G0103200103201032001033001032001032001032001032001;mq=43;o=0;" +
					"q=31,30,32,26,26,26,23,24,29,31,31,23,25,18,14,20,18,11,27,22,18,23,2,18,29,20,25,11,19,18," +
					"18,13,14,18,19,16,14,5,16,23,18,21,16,16,14,20,13,17,20,11;r=23_2;s=a23;u=0,4,1,1");
			Assert.assertNotNull(record);
			Assert.assertEquals("1", record.getSeqname());
			Assert.assertEquals("solid", record.getSource());
			Assert.assertEquals("read", record.getFeature());
			Assert.assertEquals(10148, record.getStart());
			Assert.assertEquals(10190, record.getEnd());
			Assert.assertTrue(14.4 == record.getScore());
			Assert.assertEquals("-", record.getStrand());
			Assert.assertEquals(".", record.getFrame());
		}
	}
	
	@Test
	public void testParseRecordWithInvalidAttributes() throws Exception {
		try (GffReader reader = new GffReader(EMPTY_FILE);	){
			reader.getRecord("sequence	source	feature	0	1	99.99	strand	frame	attributes");
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {
			Assert.assertEquals("Attribute [attributes] is badly formed", e.getMessage());
		}
	}
	
	private static void createTestFile(String fileName, List<String> data) {

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
