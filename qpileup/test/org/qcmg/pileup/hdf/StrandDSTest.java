package org.qcmg.pileup.hdf;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.pileup.hdf.PileupHDF;
import org.qcmg.pileup.hdf.StrandDS;
import org.qcmg.pileup.model.NonReferenceRecord;
import org.qcmg.pileup.model.StrandElement;
import org.qcmg.pileup.model.StrandEnum;

public class StrandDSTest {
	
	private String hdf = getClass().getResource("/resources/test.h5").getFile();
	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void testConstructor() throws Exception {
		PileupHDF pileupHDF = new PileupHDF(hdf, false, true);
		StrandDS ds = new StrandDS(pileupHDF, 1000, 1000, "chr1", true);
		
		assertEquals(ds.getElementsMap().size(), StrandEnum.values().length);
		assertEquals(ds.getDirection(), "reverse");
	}
	
	@Test
	public void testFinalizeMetricsWithLowReadCount() throws Exception {
		
		PileupHDF pileupHDF = new PileupHDF(hdf, false, false);
		pileupHDF.open();
		StrandDS ds = new StrandDS(pileupHDF, 1000, 1000, "chr1", true);
		ds.readDatasetBlock(0, 1);
		pileupHDF.close();
		//add		
		NonReferenceRecord record = setUpNonReferenceRecord(1, 9);
		ds.finalizeMetrics(1, false, record);		
		Map<String, StrandElement> map = ds.getElementsMap();
		assertEquals(2, map.get("lowRead").getIntDataMembers()[0]);
		assertEquals(0, map.get("highNonreference").getIntDataMembers()[0]);
		
		//remove
		ds.finalizeMetrics(1, true, record);
		map = ds.getElementsMap();		
		assertEquals(1, map.get("lowRead").getIntDataMembers()[0]);
		assertEquals(0, map.get("highNonreference").getIntDataMembers()[0]);
	}
	
	@Test
	public void testFinalizeMetricsWithHighReadCount() throws Exception {
		
		PileupHDF pileupHDF = new PileupHDF(hdf, false, false);
		StrandDS ds = new StrandDS(pileupHDF, 1000, 1000, "chr1", true);
		pileupHDF.open();
		ds.readDatasetBlock(0, 1);
		pileupHDF.close();
		//add		
		NonReferenceRecord record = setUpNonReferenceRecord(1, 10);
		ds.finalizeMetrics(1, false, record);		
		Map<String, StrandElement> map = ds.getElementsMap();
		assertEquals(1, map.get("lowRead").getIntDataMembers()[0]);
		assertEquals(0, map.get("highNonreference").getIntDataMembers()[0]);
		//remove
		ds.finalizeMetrics(1, true, record);
		map = ds.getElementsMap();		
		assertEquals(1, map.get("lowRead").getIntDataMembers()[0]);
		assertEquals(0, map.get("highNonreference").getIntDataMembers()[0]);
	}	
	
	@Test
	public void testFinalizeMetricsWithHighNonReference() throws Exception {
		
		PileupHDF pileupHDF = new PileupHDF(hdf, false, false);
		StrandDS ds = new StrandDS(pileupHDF, 1000, 1000, "chr1", true);
		pileupHDF.open();
		ds.readDatasetBlock(0, 1);
		pileupHDF.close();
		
		//add		
		NonReferenceRecord record = setUpNonReferenceRecord(5, 10);
		ds.finalizeMetrics(1, false, record);		
		Map<String, StrandElement> map = ds.getElementsMap();
		assertEquals(1, map.get("lowRead").getIntDataMembers()[0]);
		assertEquals(1, map.get("highNonreference").getIntDataMembers()[0]);
		
		//remove
		ds.finalizeMetrics(1, true, record);
		map = ds.getElementsMap();		
		assertEquals(1, map.get("lowRead").getIntDataMembers()[0]);
		assertEquals(0, map.get("highNonreference").getIntDataMembers()[0]);
	}

	private NonReferenceRecord setUpNonReferenceRecord(int nonref, int totalBase) {
		NonReferenceRecord record = new NonReferenceRecord("chr1", 1, true, 10, 20);
		int[] nonrefs = new int[1];
		nonrefs[0] = nonref;
		record.setNonReferences(nonrefs);
		int[] totalbases = new int[1];
		totalbases[0] = totalBase;
		record.setTotalBases(totalbases);
		return record;
		
	}

}
