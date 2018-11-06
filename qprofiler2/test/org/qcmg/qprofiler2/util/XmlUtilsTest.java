package org.qcmg.qprofiler2.util;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.w3c.dom.Element;

import junit.runner.Version;
public class XmlUtilsTest {

	
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
			QprofilerXmlUtils.backupFileByRenaming(TEST_FILENAME);
		} catch (IOException e) {
			Assert.fail("Should not have thrown Exception");
			e.printStackTrace();
		}
		// current file should be create-able, but no backup should have been made
		Assert.assertTrue(currentFile.createNewFile());
		Assert.assertFalse(backupFile.exists());
		
		// try again
		QprofilerXmlUtils.backupFileByRenaming(TEST_FILENAME);
		// should have created a backup file by renaming the orig file
		Assert.assertTrue(backupFile.exists());
		Assert.assertFalse(currentFile.exists());
		
		// one last time
		currentFile.createNewFile();
		QprofilerXmlUtils.backupFileByRenaming(TEST_FILENAME);
		// should have created a backup file by renaming the orig file
		assertTrue(backupFile.exists());
		assertTrue(backupFile2.exists());
		assertFalse(currentFile.exists());
	}
	
	@Test
	public void testBackupFileByRenamingInvalidPath() throws Exception{
		String madeUpPath = "/this/is/a/made/up/path/" + TEST_FILENAME;
		
		Exception e = assertThrows(IOException.class , ()-> {QprofilerXmlUtils.backupFileByRenaming(madeUpPath);});
	    assertTrue(e.getMessage().contains( "No such file or directory"));
	}
	
	
	
	@Test
	public void testReadGroupElement() throws ParserConfigurationException {				
//		Exception e = assertThrows(Exception.class , ()-> {
//			XmlUtils.createReadGroupNode( QprofilerXmlUtils.createRootElement(null, "qProfiler", null) , "id");
//		});
//		assertTrue(e.getMessage().contains( "invalid parent element name"));
				
		assertAll(  ()-> {
			Element ele = XmlUtils.createReadGroupNode( QprofilerXmlUtils.createRootElement( XmlUtils.readGroupsEle, null) , "id" );
			ele.getAttribute("RGID").equals("id");
			ele.getNodeName().equals("readGroup");
		} );
	}
}
