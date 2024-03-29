package org.qcmg.pileup.hdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.pileup.model.Chromosome;


public class PileupHDFReadH5Test {
	
	String hdfFile = getClass().getResource("/resources/test.h5").getFile();
	PileupHDF hdf;
	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setUp() throws Exception {			
		hdf = new PileupHDF(hdfFile, false, false);
		hdf.open();
		assertTrue(hdf.getFile().exists());
		assertTrue(hdf.getFileId() > -1);
	}

	@After
	public void tearDown()  throws Exception{
		hdf.close();
		assertTrue(hdf.getFileId() == -1);	
		hdf = null;
	}
	
	@Test
	public void getCSVHeaderTest() throws Exception {

		String header = hdf.getCSVHeader();
		
		List<String> records = Arrays.asList(header.split("\n"));
		assertEquals(5, records.size());		
				
		assertTrue(records.contains("# REFERENCE=/tmp/reference-long.fa"));
		assertTrue(records.contains("# BAMS_ADDED=1"));
		assertTrue(records.contains("# LOW_READ_COUNT=10"));
		assertTrue(records.contains("# MIN_NONREF_PERCENT=20"));
		
		//test: HDF=/<path>/resources/test.h5
		String hasH5 = records.stream().filter(r ->  r.startsWith("# H5=") && r.endsWith("/resources/test.h5") ).findFirst() .orElse(null);
		assertTrue(hasH5 != null);		 						
	}	

	@Test
	public void testGetChromosomeLengths() throws Exception {

		List<Chromosome> list = hdf.getChromosomeLengths();

		assertEquals(2, list.size());
		assertEquals("chr1", list.get(0).getName());
		assertEquals("chr11", list.get(1).getName());
		assertEquals(69930, list.get(0).getTotalLength().intValue());
		assertEquals(69930, list.get(1).getTotalLength().intValue());
	}
	
	@Test 
	public void testGetGroupMembers() throws Exception {

		List<String> list = hdf.getRootGroupMembers();

		assertEquals(2, list.size());
		assertEquals("chr1", list.get(0));
		assertEquals("chr11", list.get(1));
	}

	@Test
	public void testGetIntegerAttribute() throws Exception {
		
		int lowReadCount = hdf.getIntegerAttribute("/metadata/record", "low_read_count");
		assertEquals(lowReadCount, 10);
		
		int nonrefThreshold = hdf.getIntegerAttribute("/metadata/record", "non_reference_threshold");
		assertEquals(nonrefThreshold, 20);
	}
	
	@Test
	public void testGetGroupLengthAttribute() throws Exception {
		int length = hdf.getGroupIntegerAttribute("/chr1");		
		assertEquals(length, 69930);
	}	
	

	@Test
	public void readStrandDatasetBlockWithIntArray() throws OutOfMemoryError, Exception {
		Object obj = hdf.readDatasetBlock("/chr1/forward/baseA", 0, 1);
		
		assertTrue(obj instanceof int[]);
		int[] array = (int[]) obj;
		assertEquals(1, array.length);
		assertEquals(0, array[0]);
	}
	
	@Test
	public void readStrandDatasetBlockWithLongArray() throws OutOfMemoryError, Exception {
		Object obj = hdf.readDatasetBlock("/chr1/forward/qualA", 0, 1);
		
		assertTrue(obj instanceof long[]);
		long[] array = (long[]) obj;
		assertEquals(1, array.length);
		assertEquals(0, array[0]);
	}
}
