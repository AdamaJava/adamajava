package org.qcmg.qprofiler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLongArray;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Test;
import org.qcmg.common.util.BaseUtils;

public class StaticMethodsTest {
	
	private static final String TEST_FILENAME = "StaticMethodTest.test";
	
	@After
	public void cleanup() {
		File f = new File(TEST_FILENAME);
		
		if (f.exists())
			f.delete();
		
		for (int i = 0 ; i < 100 ; i++) {
			f = new File(TEST_FILENAME + "." + i);
			if (f.exists()) {
				f.delete();
			}
		}
	}

	@Test
	public void testBackupFileByRenaming() throws Exception{
		String backupFileName = TEST_FILENAME + ".1";
		String backupFileName2 = TEST_FILENAME + ".2";
		File backupFile = new File(backupFileName);
		File backupFile2 = new File(backupFileName2);
		File currentFile = new File(TEST_FILENAME);
		
		try {
			StaticMethods.backupFileByRenaming(TEST_FILENAME);
		} catch (IOException e) {
			Assert.fail("Should not have thrown Exception");
			e.printStackTrace();
		}
		// current file should be create-able, but no backup should have been made
		Assert.assertTrue(currentFile.createNewFile());
		Assert.assertFalse(backupFile.exists());
		
		// try again
		StaticMethods.backupFileByRenaming(TEST_FILENAME);
		// should have created a backup file by renaming the orig file
		Assert.assertTrue(backupFile.exists());
		Assert.assertFalse(currentFile.exists());
		
		// one last time
		currentFile.createNewFile();
		StaticMethods.backupFileByRenaming(TEST_FILENAME);
		// should have created a backup file by renaming the orig file
		Assert.assertTrue(backupFile.exists());
		Assert.assertTrue(backupFile2.exists());
		Assert.assertFalse(currentFile.exists());
	}
	
	@Test
	public void testBackupFileByRenamingInvalidPath() throws Exception{
		String madeUpPath = "/this/is/a/made/up/path/" + TEST_FILENAME;
		
		try {
			StaticMethods.backupFileByRenaming(madeUpPath);
			Assert.fail("Should have thrown Exception");
		} catch (IOException e) {
			Assert.assertEquals("No such file or directory", e.getMessage());
//			e.printStackTrace();
		}
	}
	
}
