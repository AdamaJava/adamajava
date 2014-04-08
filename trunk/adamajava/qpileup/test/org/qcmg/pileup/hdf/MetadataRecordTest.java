package org.qcmg.pileup.hdf;

import static org.junit.Assert.assertEquals;


import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.pileup.QPileupException;

public class MetadataRecordTest {

	private String hdf = getClass().getResource("/resources/test.h5").getFile();
	private MetadataRecordDS metaDS;
	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setUp() throws Exception {
		PileupHDF pileupHDF = new PileupHDF(hdf, false, false);
		metaDS = new MetadataRecordDS(pileupHDF);		
	}
	
	@After 
	public void tearDown() {
		metaDS = null;		
	}
	
	@Test
	public void testAddDatasetMemberBootstrapMode() {
		metaDS.addDatasetMember(0, metaDS.getMemberString("20120202", "bootstrap", "", "1", "12:00:00", "/reference.fa"));
		
		String expected = "## METADATA=MODE:bootstrap,DATE:20120202,RUNTIME:12:00:00,HDF:" + hdf + ",REFERENCE:/reference.fa\n";
		assertEquals(expected, metaDS.getRecords()[0]);
	}
	
	@Test
	public void testAddDatasetMemberMergeMode() {
		metaDS.addDatasetMember(0, metaDS.getMemberString("20120202", "merge", "", "1", "12:00:00", "/reference.fa"));
		
		String expected = "## METADATA=MODE:merge,DATE:20120202,RUNTIME:12:00:00,HDF:" + hdf + ",REFERENCE:/reference.fa\n";
		assertEquals(expected, metaDS.getRecords()[0]);
	}
	
	@Test
	public void testAddDatasetMemberAddMode() {
		metaDS.addDatasetMember(0,  metaDS.getMemberString("20120202", "add", "/bamfile.bam", "1", "12:00:00", "/reference.fa"));
		
		String expected = "## METADATA=MODE:add,DATE:20120202,RUNTIME:12:00:00,HDF:" + hdf + ",FILE:/bamfile.bam,RECORDS:1\n";
		assertEquals(expected, metaDS.getRecords()[0]);
	}
	
	@Test
	public void testCheckHDFMetadataWithBamOverride() throws QPileupException {
		String[] records = {"## METADATA=MODE:merge,DATE:20120202,RUNTIME:12:00:00,HDF:" + hdf + ",REFERENCE:/reference.fa\n"};
		String[] recordsToAdd = {"MODE:bootstrap,DATE:20120522,RUNTIME:00:28:03,HDF:/target.qpileup.h5,REFERENCE:/reference.fa",
			"## METADATA=MODE:add,DATE:20120522,RUNTIME:01:24:21,HDF:/target.qpileup.h5,FILE:testbam.bam,RECORDS:468329"		
		};
		metaDS.setRecords(records);
		assertEquals(null, metaDS.getLowReadCount());
		assertEquals(null, metaDS.getNonreferenceThreshold());
		
		//add first records
		metaDS.checkHDFMetadata(recordsToAdd, true, 10, 20, 0);	
		assertEquals(3, metaDS.getRecords().length);
		
		//add second records - matching
		metaDS.checkHDFMetadata(recordsToAdd, true, 10, 20, 0);	
		assertEquals(5, metaDS.getRecords().length);
	}
	
	@Test(expected=QPileupException.class)
	public void testCheckHDFMetadataWithOutBamOverride() throws QPileupException {
		String[] records = {"## METADATA=MODE:merge,DATE:20120202,RUNTIME:12:00:00,HDF:" + hdf + ",REFERENCE:/reference.fa\n"};
		String[] recordsToAdd = {"MODE:bootstrap,DATE:20120522,RUNTIME:00:28:03,HDF:/target.qpileup.h5,REFERENCE:/reference.fa",
			"## METADATA=MODE:add,DATE:20120522,RUNTIME:01:24:21,HDF:/target.qpileup.h5,FILE:testbam.bam,RECORDS:468329"		
		};
		metaDS.setRecords(records);
		assertEquals(null, metaDS.getLowReadCount());
		assertEquals(null, metaDS.getNonreferenceThreshold());
		
		//add first records
		metaDS.checkHDFMetadata(recordsToAdd, false, 10, 20, 0);	
		assertEquals(3, metaDS.getRecords().length);
		
		//add second records - matching
		metaDS.checkHDFMetadata(recordsToAdd, false, 10, 20, 0);	
		assertEquals(5, metaDS.getRecords().length);
	}
}
