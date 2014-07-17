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
		assertEquals(record.getMatch(), 48);
		assertEquals(record.getScore(), 42);
		assertEquals(record.getMismatch(), 1);
		assertEquals(record.gettGapCount(), 3);
		assertEquals(record.getqGapCount(), 2);
		assertEquals(record.getName(), "chr10-89712341-true-pos");
		assertEquals(record.getReference(), "chr10");
		assertEquals(record.getQueryStart(), (1));
		assertEquals(record.getQueryEnd(), (48));
		assertEquals(record.getStartPos(), (89700252));
		assertEquals(record.getEndPos(), (89700299));
		assertEquals(record.getStrand(), "+");
	}
	
	public String setUpBlatOutputFile() throws IOException {
		String file = testFolder.getRoot().toString() + QSVUtil.getFileSeparator() + "out.psl";
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(file)));
		writer.write("48\t1\t0\t0\t2\t0\t3\t0\t+\tchr10-89712341-true-pos\t66\t0\t48\tchr10\t135534747\t89700251\t89700299\t1\t48,\t0,\t89700251,");
		writer.close();
		return file;
	}
	
	

}
