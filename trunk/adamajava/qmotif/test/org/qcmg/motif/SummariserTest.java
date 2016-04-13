package org.qcmg.motif;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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

}
