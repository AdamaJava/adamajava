package org.qcmg.motif;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.SAXException;

public class SummariserTest {
	
	 @org.junit.Rule
	 public  TemporaryFolder folder = new TemporaryFolder();
	
	@Test
	public void getLinesFromFile() throws IOException {
		assertEquals(0, Summariser.loadInputsFromFile(null).size());
		
		File f = folder.newFile();
		assertEquals(0, Summariser.loadInputsFromFile(f.getAbsolutePath()).size());
		
		try (FileWriter writer = new FileWriter(f)) {
			writer.write("");
		}
		assertEquals(0, Summariser.loadInputsFromFile(f.getAbsolutePath()).size());
		try (FileWriter writer = new FileWriter(f)) {
			writer.write("blah");
		}
		assertEquals(1, Summariser.loadInputsFromFile(f.getAbsolutePath()).size());
		assertEquals("blah", Summariser.loadInputsFromFile(f.getAbsolutePath()).get(0));
	}
	
	@Test
	public void getSummaryEmptyFile() throws ParserConfigurationException, SAXException, IOException {
		assertEquals(null, Summariser.getSummaryData(null));
		assertEquals(null, Summariser.getSummaryData(""));
		File f = folder.newFile();
		try (FileWriter writer = new FileWriter(f)) {
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
			writer.write("<qmotif version=\"1.2 (1084) (0.3)\">\n");
			writer.write("</qmotif>\n");
		}
		assertEquals("", Summariser.getSummaryData(f.getAbsolutePath()));
		
	}
	
	@Test
	public void getFullSummary() throws IOException {
		File sf = folder.newFile();
		try (FileWriter w = new FileWriter(sf)) {
			for (int i = 0 ; i < 100 ; i++) {
				File f = folder.newFile();
				try (FileWriter writer = new FileWriter(f)) {
					writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
					writer.write("<qmotif version=\"1.2 (1084) (0.3)\">\n");
					writer.write("<summary bam=\"/mnt/lustre/working/genomeinfo/sample/" + i + ".bam\">\n");
					writer.write("<counts>\n");
					writer.write("<totalReadCount count=\"982974779\"/>\n");
					writer.write("<noOfMotifs count=\"86195\"/>\n");
					writer.write("<rawUnmapped count=\"11\"/>\n");
					writer.write("<rawIncludes count=\"98187\"/>\n");
					writer.write("<rawGenomic count=\"26\"/>\n");
					writer.write("<scaledUnmapped count=\"11\"/>\n");
					writer.write("<scaledIncludes count=\"99887\"/>\n");
					writer.write("<scaledGenomic count=\"26\"/>\n");
					writer.write("</counts>\n");
					writer.write("</summary>\n");
					writer.write("</qmotif>\n");
				}
				w.write(f.getAbsolutePath() + "\n");
			}
		}
		
		List<String> files = Summariser.loadInputsFromFile(sf.getAbsolutePath());
		assertEquals(100, files.size());
		
		List<String> results = Summariser.getAllSummaryData(files);
		assertEquals(100, results.size());
		
		for (int i = 0 ; i < 100 ; i++) {
			assertEquals(true, results.get(i).contains(i + ".bam"));
		}
		
	}
	
	
	@Test
	public void getSummary() throws ParserConfigurationException, SAXException, IOException {
		File f = folder.newFile();
		try (FileWriter writer = new FileWriter(f)) {
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
			writer.write("<qmotif version=\"1.2 (1084) (0.3)\">\n");
			writer.write("<summary bam=\"/mnt/lustre/working/genomeinfo/sample/5/4/54e9188c-7c47-421f-b14a-1843a4fad3b4/aligned_read_group_set/c78dd4e3-a61e-47d3-882f-cce5e0f6d28c.bam\">\n");
			writer.write("<counts>\n");
			writer.write("<totalReadCount count=\"982974779\"/>\n");
			writer.write("<noOfMotifs count=\"86195\"/>\n");
			writer.write("<rawUnmapped count=\"11\"/>\n");
			writer.write("<rawIncludes count=\"98187\"/>\n");
			writer.write("<rawGenomic count=\"26\"/>\n");
			writer.write("<scaledUnmapped count=\"11\"/>\n");
			writer.write("<scaledIncludes count=\"99887\"/>\n");
			writer.write("<scaledGenomic count=\"26\"/>\n");
			writer.write("</counts>\n");
			writer.write("</summary>\n");
			writer.write("</qmotif>\n");
		}
		String s = Summariser.getSummaryData(f.getAbsolutePath());
		assertEquals(true, s.contains("bam=/mnt/lustre/working/genomeinfo/sample/5/4/54e9188c-7c47-421f-b14a-1843a4fad3b4/aligned_read_group_set/c78dd4e3-a61e-47d3-882f-cce5e0f6d28c.bam"));
		assertEquals(true, s.contains("totalReadCount 982974779"));
		assertEquals(true, s.contains("noOfMotifs 86195"));
		assertEquals(true, s.contains("rawUnmapped 11"));
		assertEquals(true, s.contains("rawIncludes 98187"));
		assertEquals(true, s.contains("rawGenomic 26"));
		assertEquals(true, s.contains("scaledUnmapped 11"));
		assertEquals(true, s.contains("scaledIncludes 99887"));
		assertEquals(true, s.contains("scaledGenomic 26"));
		
	}

}
