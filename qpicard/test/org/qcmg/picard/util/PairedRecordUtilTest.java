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

	@Test
	public void getOverlapBase() {
		
		//forward tlen > 0 
		//here we ignor record.getAlignmentEnd(); assume there is no indel or skipping. 
		//since we only want to know the overlapp cause during sample on library
		SAMRecord record = new SAMRecord(null);
		record.setAlignmentStart(10075);
		record.setMateAlignmentStart(10100);
		record.setFlags(99);
		record.setCigarString("15M50N22M");
		record.setInferredInsertSize(175);
		record.setReadBases("ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC".getBytes());
		assertTrue( 12 == PairedRecordUtils.getOverlapBase(record));
		
		//without overlap
		record.setMateAlignmentStart(10200);
		record.setInferredInsertSize(275);
		assertTrue( 0 > PairedRecordUtils.getOverlapBase(record));		
 		
		//mate reverse tlen < = 0
	    record.setAlignmentStart(10100);
		record.setMateAlignmentStart(10075);
		record.setFlags(147);
		record.setCigarString("25M100D20M5S");
		record.setInferredInsertSize(-175);
		assertTrue( 0 == PairedRecordUtils.getOverlapBase(record));
		record.setReadBases("ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAACACCCTAACCCTAA".getBytes());
		assertTrue( 0 == PairedRecordUtils.getOverlapBase(record));
	}
	 
}
