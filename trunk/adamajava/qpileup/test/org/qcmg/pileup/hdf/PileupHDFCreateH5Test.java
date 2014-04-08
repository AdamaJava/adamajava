package org.qcmg.pileup.hdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import ncsa.hdf.object.Datatype;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.pileup.PileupConstants;

public class PileupHDFCreateH5Test {
	
	String hdfFile;
	PileupHDF hdf;
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setUp() throws Exception {	
		hdfFile = testFolder.getRoot().getAbsolutePath() + PileupConstants.FILE_SEPARATOR + "test.h5";
		hdf = new PileupHDF(hdfFile, true, false);
		hdf.open();
		assertTrue(hdf.getFile().exists());
		assertTrue(hdf.getFileId() > -1);
	}

	@After
	public void tearDown()  throws Exception{
		hdf.close();
		assertTrue(hdf.getFileId() == -1);
		hdf = null;
		new File(hdfFile).delete();
	}
	
	@Test
	public void testCreateGroup() throws Exception {
		String groupName = hdf.createH5Group("test");
		assertEquals("/test", groupName);
	}
	
	@Test
	public void testCreateScalarDS() throws Exception {
	     hdf.createH5ScalarDS("", 1, "test", 1, Datatype.CLASS_INTEGER, 4, null);
	}
}
