package org.qcmg.pileup.hdf;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MetadataReferenceTest {
	
	private String hdf = getClass().getResource("/resources/test.h5").getFile();
	private String referenceFile = getClass().getResource("/resources/test-reference.fa").getFile();
	private MetadataReferenceDS metaDS;
	private static String expected = "## REFERENCE=SEQUENCE:test,LENGTH:123";
	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setUp() throws Exception {
		PileupHDF pileupHDF = new PileupHDF(hdf, false, false);
		metaDS = new MetadataReferenceDS(pileupHDF, referenceFile);		
	}
	
	@After 
	public void tearDown() {
		metaDS = null;		
	}
	
	@Test
	public void testGetMemberString() {
		String test = metaDS.getMemberString("test", 123);		
		assertEquals(expected, test);
	}
	
	@Test
	public void testAddRecords() {
		List<String> references = new ArrayList<String>();
		references.add(expected);
		metaDS.addRecords(references);
		assertEquals("## REFERENCE=FILE:" + referenceFile, metaDS.getRecords()[0]);
		assertEquals(expected, metaDS.getRecords()[1]);
	}

}
