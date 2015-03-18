package org.qcmg.qsv.blat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.util.QSVUtil;

public class BLATTest {
	
	BLAT blat;

	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
		

	@Before
	public void setUp() throws Exception {
		blat = new BLAT("localhost", "8000", "/test");
		assertEquals(blat.getCommands().size(), 5);
		assertEquals(blat.getCommands().get(0), "/test" + QSVParameters.FILE_SEPERATOR + "gfClient");
	}
	
	@Test(expected=QSVException.class)
	public void testExecuteThrowsException() throws IOException, InterruptedException, QSVException {
		blat.execute(testFolder.newFile("fasta.fa").getAbsolutePath(), testFolder.newFile("output.psl").getAbsolutePath());
	}
	
	@Test
	public void testParseResults() throws Exception {
		String file = setUpBlatOutputFile();
		Map<String, BLATRecord> results = blat.parseResults(file);
		assertEquals(results.size(), 1);
		assertTrue(results.containsKey("chr10-89712341-true-pos"));
		
		BLATRecord record = results.get("chr10-89712341-true-pos");
		assertTrue(record.isValid());
//		assertEquals(record.getMatch(), 48);
		assertEquals(record.getScore(), 42);
//		assertEquals(record.getMismatch(), 1);
//		assertEquals(record.gettGapCount(), 3);
//		assertEquals(record.getqGapCount(), 2);
		assertEquals(record.getName(), "chr10-89712341-true-pos");
		assertEquals(record.getReference(), "chr10");
		assertEquals(record.getQueryStart(), (1));
		assertEquals(record.getQueryEnd(), (48));
		assertEquals(record.getStartPos(), (89700252));
		assertEquals(record.getEndPos(), (89700299));
		assertEquals(record.getStrand(), QSVUtil.PLUS);
	}
	
	@Test
	public void testParseMultipleResults() throws Exception {
		String file = setUpBlatOutputFileMultipleResutls();
		Map<String, BLATRecord> results = blat.parseResults(file);
		// only the record with the highest score makes it into the map as they all have the same name
		assertEquals(1, results.size());
		BLATRecord rec = results.get("12345_123455_12345");
		assertEquals("chrMT", rec.getReference());
	}
	
	public String setUpBlatOutputFile() throws IOException {
		String file = testFolder.getRoot().toString() + QSVUtil.getFileSeparator() + "out.psl";
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(file)));
		writer.write("48\t1\t0\t0\t2\t0\t3\t0\t+\tchr10-89712341-true-pos\t66\t0\t48\tchr10\t135534747\t89700251\t89700299\t1\t48,\t0,\t89700251,\n");
		writer.close();
		return file;
	}
	
	public String setUpBlatOutputFileMultipleResutls() throws IOException {
		File file = testFolder.newFile();
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file));) {
			writer.write("40\t2\t0\t0\t1\t4\t1\t5\t+\t12345_123455_12345\t70\t14\t60\tchr13\t115169878\t96348720\t96348767\t2\t25,17,\t14,43,\t96348720,96348750,\n");
			writer.write("64\t6\t0\t0\t0\t0\t0\t0\t+\t12345_123455_12345\t70\t0\t70\tchr17\t81195210\t22020726\t22020796\t1\t70,\t0,\t22020726,\n");
			writer.write("41\t1\t0\t0\t1\t8\t1\t9\t+\t12345_123455_12345\t70\t10\t60\tchr7\t159138663\t57266186\t57266237\t2\t29,13,\t10,47,\t57266186,57266224,\n");
			writer.write("47\t3\t0\t0\t0\t0\t1\t1\t+\t12345_123455_12345\t70\t10\t60\tchr9\t141213431\t33655900\t33655951\t2\t32,18,\t10,42,\t33655900,33655933,\n");
			writer.write("70\t0\t0\t0\t0\t0\t0\t0\t+\t12345_123455_12345\t70\t0\t70\tchrMT\t16569\t0\t70\t1\t70,\t0,\t0,\n");
			writer.write("41\t1\t0\t0\t1\t7\t1\t8\t-\t12345_123455_12345\t70\t11\t60\tchr1\t249250621\t238110559\t238110609\t2\t14,28,\t10,31,\t238110559,238110581,\n");
			writer.write("41\t1\t0\t0\t1\t4\t1\t5\t-\t12345_123455_12345\t70\t14\t60\tchr9\t141213431\t81359142\t81359189\t2\t17,25,\t10,31,\t81359142,81359164,\n");
		}
		return file.getAbsolutePath();
	}
	
	

}
