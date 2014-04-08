package org.qcmg.qmule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CompareReferenceRegionsTest {
	
	File fileA;
	File fileB;
	File fileC; 
	String output;
	private static String FILE_SEPARATOR = System.getProperty("file.separator");
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setUp() throws IOException {
		fileA = createFileA();
		fileB = createFileB();
		fileC = createFileC();
	}
	
	@After
	public void tearDown() {
		fileA.delete();
		fileB.delete();		
		fileC.delete();
	}
	
	@Test
	public void testOneway() throws Exception {
		CompareReferenceRegions c = new CompareReferenceRegions();
		output = testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "output.gff3";
		String outputB = testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "outputB.gff3";
		String[] args = {"--log", testFolder.newFile("test.log").getAbsolutePath(), "--mode", "oneway", "--input", fileA.getAbsolutePath(), "--input", fileB.getAbsolutePath(), "--output", output, "--output", outputB,};
		c.setup(args);
		
		assertOutputFile(output, 100, 110, 2);		
		assertOutputFile(outputB, 90, 90, 1);	
	}
	
	@Test
	public void testAnnotate() throws Exception {
		CompareReferenceRegions c = new CompareReferenceRegions();
		output = testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "output.gff3";
		String outputB = testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "outputB.gff3";
		String[] args = {"--log", testFolder.newFile("test.log").getAbsolutePath(), "--mode", "annotate", "--input", fileA.getAbsolutePath(), "--input", fileB.getAbsolutePath(), "--output", output, "--output", outputB, "--column", "9", "--annotation", "ANNOTATION"};
		c.setup(args);
		BufferedReader reader = new BufferedReader(new FileReader(new File(output)));
		String line;
		int count = 0;
		while ((line = reader.readLine()) != null) {
			count++;
			String[] values = line.split("\t");
			if (count == 1) {				
				assertEquals("90", values[3]);
				assertEquals("90", values[4]);
				assertFalse(values[8].contains("ANNOTATION"));
			}
			if (count == 2) {				
				assertEquals("100", values[3]);
				assertEquals("110", values[4]);
				assertTrue(values[8].contains("ANNOTATION"));
			}
			if (count == 3) {				
				assertEquals("200", values[3]);
				assertEquals("210", values[4]);
				assertTrue(values[8].contains("ANNOTATION"));
			}
		}
		reader.close();	
	}
	
	@Test
	public void testIntersect() throws Exception {
		CompareReferenceRegions c = new CompareReferenceRegions();
		output = testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "output.gff3";
		String[] args = {"--log", testFolder.newFile("test.log").getAbsolutePath(), "--mode", "intersect", "--input", fileA.getAbsolutePath(), "--input", fileB.getAbsolutePath(), "--input", fileC.getAbsolutePath(), "--output", output};
		c.setup(args);
		
		assertOutputFile(output, 190, 220, 1);		
	}
	
	private void assertOutputFile(String file, int start, int end, int count) throws IOException {
		assertTrue(new File(file).exists());
		
		BufferedReader reader = new BufferedReader(new FileReader(new File(file)));
		String line = reader.readLine();
		assertNotNull(line);
		String[] values = line.split("\t");
		assertEquals(Integer.toString(start), values[3]);
		assertEquals(Integer.toString(end), values[4]);
		reader.close();		
	}

	@Test
	public void testUnique() throws Exception {
		CompareReferenceRegions c = new CompareReferenceRegions();
		output = testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "outputA.gff3";
		String outputB = testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "outputB.gff3";
		String outputC = testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "outputC.gff3";
		String[] args = {"--log", testFolder.newFile("test.log").getAbsolutePath(), "--mode", "unique", "--input", fileA.getAbsolutePath(), "--input", fileB.getAbsolutePath(), "--input", fileC.getAbsolutePath(), "--output", output,
				"--output", outputB, "--output", outputC		
		};
		c.setup(args);
		assertOutputFile(output, 90, 90, 1);	
		assertOutputFile(outputB, 80, 80, 1);	
		assertOutputFile(outputC, 50, 55, 1);	
	}
	
	
	private File createFileA() throws IOException {
		File f = new File(testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "fileA.gff3");
		BufferedWriter writer = new BufferedWriter(new FileWriter(f));
		
		writer.write("chr1\ttest\t0\t100\t110\t1.92\t0\t0\tName=Test\n");//overlap with 2
		writer.write("chr1\ttest\t0\t90\t90\t1.92\t0\t0\tName=Test\n");//unique
		writer.write("chr1\ttest\t0\t200\t210\t1.92\t0\t0\tName=Test\n");//overlap with 2 and 3
		writer.close();
		return f;
	}
	
	private File createFileB() throws IOException {
		File f = new File(testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "fileB.gff3");
		BufferedWriter writer = new BufferedWriter(new FileWriter(f));
		
		writer.write("chr1\ttest\t0\t100\t105\t1.92\t0\t0\tName=Test\n");//overlap with 1
		writer.write("chr1\ttest\t0\t80\t80\t1.92\t0\t0\tName=Test\n");//unique
		writer.write("chr1\ttest\t0\t190\t210\t1.92\t0\t0\tName=Test\n");//overlap with 2 and 3
		writer.close();
		return f;
	}
	
	private File createFileC() throws IOException {
		File f = new File(testFolder.getRoot().getAbsolutePath() + FILE_SEPARATOR + "fileC.gff3");
		BufferedWriter writer = new BufferedWriter(new FileWriter(f));
		
		writer.write("chr1\ttest\t0\t50\t55\t1.92\t0\t0\tName=Test\n");//unique
		writer.write("chr1\ttest\t0\t70\t70\t1.92\t0\t0\tName=Test\n");//unique
		writer.write("chr1\ttest\t0\t200\t220\t1.92\t0\t0\tName=Test\n");//overlap with 2 and 3
		writer.close();
		return f;
	}


}
