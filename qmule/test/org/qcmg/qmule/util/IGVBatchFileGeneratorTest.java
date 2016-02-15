package org.qcmg.qmule.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;

public class IGVBatchFileGeneratorTest {
	
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();
	
	@Test
	public void testGenerate() throws IOException {
		try {
			IGVBatchFileGenerator.generate(null, null);
			Assert.fail("Should not have reached here");
		} catch (IllegalArgumentException iae) {}
		try {
			IGVBatchFileGenerator.generate(null, "");
			Assert.fail("Should not have reached here");
		} catch (IllegalArgumentException iae) {}
		try {
			IGVBatchFileGenerator.generate(Collections.EMPTY_LIST, "");
			Assert.fail("Should not have reached here");
		} catch (IllegalArgumentException iae) {}
		
		// create a temp File
		File tmpOutput = tempFolder.newFile("testGenerate.igv.batch");
		try {
			IGVBatchFileGenerator.generate(Collections.EMPTY_LIST, tmpOutput.getAbsolutePath());
			Assert.fail("Should not have reached here");
		} catch (IllegalArgumentException iae) {}
		
		List<ChrPosition> positions = new ArrayList<ChrPosition>();
		positions.add(ChrPointPosition.valueOf("chr1", 1));
		positions.add(ChrPointPosition.valueOf("chr2", 1234567890));
		
		IGVBatchFileGenerator.generate(positions, tmpOutput.getAbsolutePath());
		
		//read in contents of file
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(tmpOutput)));
		List<String> fileContents = new ArrayList<String>();
		String line = null;
		while ((line = reader.readLine()) != null) {
			fileContents.add(line);
		}
		reader.close();
		
		Assert.assertEquals("snapshotDirectory " + tmpOutput.getParent() , fileContents.get(0));
		Assert.assertEquals("genome " + IGVBatchFileGenerator.GENOME, fileContents.get(1));
		Assert.assertEquals("goto chr1:1-1", fileContents.get(2));
		Assert.assertEquals("sort base", fileContents.get(3));
		Assert.assertEquals("collapse", fileContents.get(4));
		Assert.assertEquals("snapshot chr1:1.png", fileContents.get(5));
		Assert.assertEquals("goto chr2:1234567890-1234567890", fileContents.get(6));
		Assert.assertEquals("snapshot chr2:1234567890.png", fileContents.get(9));
		
	}

}
