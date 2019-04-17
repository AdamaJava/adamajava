package org.qcmg.picard;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

public class RenameFileTest {
	
	@Test
	public void renameIndex() throws IOException {
		Path dir = Files.createTempDirectory("java-test");
		File bam = new File(dir.resolve("test.bam").toString());
		File bai = new File(dir.resolve("test.bai").toString());
		// create the bam and a picard-style ${stem/bam/bai} index 
		bam.createNewFile();
		bai.createNewFile();
		File baiRenamed = new File(dir.resolve("test.bam.bai").toString());

		Assert.assertTrue(bai.exists());
		Assert.assertFalse(baiRenamed.exists());
		RenameFile.renameIndex(bam);
		Assert.assertTrue(baiRenamed.exists());
		Assert.assertFalse(bai.exists());
	}
	
	@Test(expected = IOException.class)
	public void renameNonExistentIndex() throws IOException {
		Path dir = Files.createTempDirectory("java-test");
		File bam = new File(dir.resolve("test.bam").toString());
		// create the bam but no index
		bam.createNewFile();
		// expect IOException
		RenameFile.renameIndex(bam);
	}
	
	@Test(expected = IOException.class)
	public void renameAlreadyCanonicalIndex() throws IOException {
		Path dir = Files.createTempDirectory("java-test");
		File bam = new File(dir.resolve("test.bam").toString());
		File bai = new File(dir.resolve("test.bam.bai").toString());
		// create the bam and an already-canonically named index
		bam.createNewFile();
		bai.createNewFile();
		// expect IOException
		RenameFile.renameIndex(bam);
	}

}
