package org.qcmg.pileup.hdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.Datatype;
import ncsa.hdf.object.Group;
import ncsa.hdf.object.h5.H5Datatype;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.pileup.PileupConstants;

public class PileupHDFCreateHDFObjectTest {
	
	String hdfFile;
	PileupHDF hdf;
	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setUp() throws Exception {	
		hdfFile = testFolder.getRoot().getAbsolutePath() + PileupConstants.FILE_SEPARATOR + "test.h5";
		hdf = new PileupHDF(hdfFile, true, true);
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
		Group group = hdf.createGroup("test", "/");
		
		assertNotNull(group);
		assertEquals("/test", group.getFullName());
		group.close(group.getFID());
	}
	
	@Test
	public void testCreateCompoundDS() throws Exception {
		long[] DIMs = { 50, 10 };
		 // create groups
		 Datatype[] mdtypes = new H5Datatype[3];
		 String[] mnames = { "int", "float", "string" };
		 Dataset dset = null;

	     mdtypes[0] = new H5Datatype(Datatype.CLASS_INTEGER, 4, -1, -1);
	     mdtypes[1] = new H5Datatype(Datatype.CLASS_FLOAT, 4, -1, -1);
	     mdtypes[2] = new H5Datatype(Datatype.CLASS_STRING, 2, -1, -1);
	     dset = hdf.createCompoundDS( "/", "test", DIMs, null, null, 0,
	             mnames, mdtypes, null, null);
	     assertNotNull(dset);
	     assertEquals("/test", dset.getFullName());	     
	}
	
	@Test
	public void testCreateScalarDS() throws Exception {
		 Group group = hdf.getGroup("/");
		 
		 assertNotNull(group);
	     Dataset dset = hdf.createScalarDS(group, 1, "test", 1, Datatype.CLASS_INTEGER, 4, null);
	     assertNotNull(dset);
	     assertEquals("/test", dset.getFullName());
	     group.close(group.getFID());
	}
}
