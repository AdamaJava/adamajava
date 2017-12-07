package org.qcmg.picard.util;

import static org.junit.Assert.*;

import org.junit.Test;

import htsjdk.samtools.SAMRecord;

public class PairedRecordUtilTest {
	
	@Test
	public void isCanonical() {
		SAMRecord record = new SAMRecord(null);
		assertEquals(false, PairedRecordUtils.isCanonical(record));
		record.setFlags(3);
		record.setMateReferenceName("chr1");
		record.setReferenceName("chr1");
		record.setMateNegativeStrandFlag(true);
		record.setReadNegativeStrandFlag(true);
		assertEquals(false, PairedRecordUtils.isCanonical(record));
		record.setReadNegativeStrandFlag(false);
		assertEquals(true, PairedRecordUtils.isCanonical(record));
	}
	
//	@Test
//	public void isF3ToF5() {
//		SAMRecord record = new SAMRecord(null);
//		assertEquals(true, PairedRecordUtils.isF3toF5(record));
//		
//	}
	
	@Test
	public void sameReference() {
		SAMRecord record = new SAMRecord(null);
		record.setFlags(3);
		record.setMateReferenceName("chr1");
		record.setReferenceName("chr1");
		assertEquals(true, PairedRecordUtils.isSameReference(record));
		record.setReferenceName("chr11");
		assertEquals(false, PairedRecordUtils.isSameReference(record));
	}
	
	@Test
	public void isLefty() {
		SAMRecord record = new SAMRecord(null);
		record.setAlignmentStart(1);
		record.setMateAlignmentStart(1);
		assertEquals(false, PairedRecordUtils.isReadLeftOfMate(record));
		record.setMateAlignmentStart(2);
		assertEquals(true, PairedRecordUtils.isReadLeftOfMate(record));
		record.setAlignmentStart(2);
		assertEquals(false, PairedRecordUtils.isReadLeftOfMate(record));
		record.setMateAlignmentStart(1);
		assertEquals(false, PairedRecordUtils.isReadLeftOfMate(record));
	}
	
	@Test
	public void isRighty() {
		SAMRecord record = new SAMRecord(null);
		record.setAlignmentStart(1);
		record.setMateAlignmentStart(1);
		assertEquals(false, PairedRecordUtils.isReadRightOfMate(record));
		record.setMateAlignmentStart(2);
		assertEquals(false, PairedRecordUtils.isReadRightOfMate(record));
		record.setAlignmentStart(3);
		assertEquals(true, PairedRecordUtils.isReadRightOfMate(record));
		record.setMateAlignmentStart(1);
		assertEquals(true, PairedRecordUtils.isReadRightOfMate(record));
		
	}

}
