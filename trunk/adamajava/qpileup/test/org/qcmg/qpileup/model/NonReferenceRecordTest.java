package org.qcmg.qpileup.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.qcmg.pileup.model.NonReferenceRecord;
import org.qcmg.pileup.model.PileupDataRecord;

public class NonReferenceRecordTest {
	
	@Test
	public void testDefineNonReferenceMetricsLowRead() {
		PileupDataRecord record = new PileupDataRecord(1);
		record.setBaseA(1);
		record.setNonReferenceNo(0);
		
		NonReferenceRecord r = new NonReferenceRecord("chr1", 1, true, 10, 20);
		r.addNonReferenceMetrics(record, 0);
		r.defineNonReferenceMetrics(0);
		
		assertTrue(r.isLowReadCount());
		assertFalse(r.isHighNonRef());
	}
	
	@Test
	public void testDefineNonReferenceMetricsHighNonRef() {
		PileupDataRecord record = new PileupDataRecord(1);
		record.setBaseA(5);
		record.setBaseC(5);
		record.setNonReferenceNo(5);
		
		NonReferenceRecord r = new NonReferenceRecord("chr1", 1, true, 10, 20);
		r.addNonReferenceMetrics(record, 0);
		r.defineNonReferenceMetrics(0);
		
		assertFalse(r.isLowReadCount());
		assertTrue(r.isHighNonRef());
	}
	
	@Test
	public void testDefineNonReferenceMetricsNonHighNonRef() {
		PileupDataRecord record = new PileupDataRecord(1);
		record.setBaseA(10);
		record.setBaseC(2);
		record.setReferenceNo(10);
		record.setNonReferenceNo(2);
		
		NonReferenceRecord r = new NonReferenceRecord("chr1", 1, true, 10, 20);
		r.addNonReferenceMetrics(record, 0);
		r.defineNonReferenceMetrics(0);
		
		assertFalse(r.isLowReadCount());
		assertFalse(r.isHighNonRef());
	}
	

}
