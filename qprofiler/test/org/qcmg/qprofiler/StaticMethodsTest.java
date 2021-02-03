package org.qcmg.qprofiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Test;


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
			fail("Should not have thrown Exception");
			e.printStackTrace();
		}
		// current file should be create-able, but no backup should have been made
		assertTrue(currentFile.createNewFile());
		assertFalse(backupFile.exists());
		
		// try again
		StaticMethods.backupFileByRenaming(TEST_FILENAME);
		// should have created a backup file by renaming the orig file
		assertTrue(backupFile.exists());
		assertFalse(currentFile.exists());
		
		// one last time
		currentFile.createNewFile();
		StaticMethods.backupFileByRenaming(TEST_FILENAME);
		// should have created a backup file by renaming the orig file
		assertTrue(backupFile.exists());
		assertTrue(backupFile2.exists());
		assertFalse(currentFile.exists());
	}
	
	@Test
	public void testBackupFileByRenamingInvalidPath() throws Exception{
		String madeUpPath = "/this/is/a/made/up/path/" + TEST_FILENAME;
		
		try {
			StaticMethods.backupFileByRenaming(madeUpPath);
			fail("Should have thrown Exception");
		} catch (IOException e) {
			assertEquals("No such file or directory", e.getMessage());
		}
	}
	
}
