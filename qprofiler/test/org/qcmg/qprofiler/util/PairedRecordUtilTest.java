package org.qcmg.qprofiler.util;

import static org.junit.Assert.*;

import org.junit.Test;
import org.qcmg.qprofiler.util.PairedRecordUtils;

import htsjdk.samtools.SAMRecord;

public class PairedRecordUtilTest {
	
	@Test
	public void isOverlapF5F3() {
		//read   : first of pair 	   |---->  or  <------|      
		//read   : second of pair  |---->      or  	        <-----|    
		
		SAMRecord recorda = new SAMRecord(null);
		//first of mapped pair, both forward
		recorda.setFlags(67);
		recorda.setAlignmentStart(120);
		recorda.setMateAlignmentStart(100);
		//read lenght  = seq.length -1
		recorda.setReadBases("AAAAATTTTCCCCGGGGGGGG".getBytes());
		//forward mate start -1 - (forward read start -1) = secondPairStart - firstPairStart
		recorda.setInferredInsertSize(-20);		
		assertTrue(PairedRecordUtils.isF5toF3(recorda));
		//tlen < 0, return overlap == 0
		assertTrue(PairedRecordUtils.getOverlapBase(recorda) == 0); 
				
		//now set to second of mapped pair, both reverse 
		recorda.setFlags(179);
		//reverse mate end - reverse read end = (100+22) - (120 + 22) = -20 (just example)
		recorda.setInferredInsertSize(-20);	
		assertTrue(PairedRecordUtils.isF5toF3(recorda));
		assertTrue(PairedRecordUtils.getOverlapBase(recorda) == 0); 
		
		//now set to first of mapped pair, both reverse 
		recorda.setFlags(115);
		recorda.setAlignmentStart(100);
		recorda.setMateAlignmentStart(120);
		//reverse mate end - reverse read end = (120+22) - (100 + 22) = 20 (just example)
		recorda.setInferredInsertSize(20);
		//overlap == read length - tlen = 21 - 20 = 1
		assertTrue(PairedRecordUtils.getOverlapBase(recorda) == 1);
		 		
		//now set to second of mapped pair, both forward 
		recorda.setFlags(131);
		recorda.setAlignmentStart(100);
		recorda.setMateAlignmentStart(120);
		recorda.setInferredInsertSize(20);	
		assertTrue(PairedRecordUtils.isF5toF3(recorda));
		assertTrue(PairedRecordUtils.getOverlapBase(recorda) == 1);
		
		//when there is no overlap
		recorda.setMateAlignmentStart(130);
		recorda.setInferredInsertSize(30);
		assertTrue(PairedRecordUtils.getOverlapBase(recorda) < 0);
		
	}
	
	@Test
	public void isOverlapF3F5() {
		//read   : first of pair    |---->        or  	   <------|      
		//read   : second of pair        |---->   or  <-----|    
		SAMRecord recorda = new SAMRecord(null);		
		// second of mapped pair, both forward 
		recorda.setFlags(131);		
		recorda.setAlignmentStart(120);
		recorda.setMateAlignmentStart(100);
		//100-120
		recorda.setInferredInsertSize(-20);	
		assertFalse(PairedRecordUtils.isF5toF3(recorda));
		assertTrue(PairedRecordUtils.isF3toF5(recorda));
		//only count overlap on reads with positive tlen
		assertTrue(PairedRecordUtils.getOverlapBase(recorda) == 0);
				
		//now set to second of mapped pair, both reverse 
		recorda.setFlags(179);
		recorda.setAlignmentStart(100);
		recorda.setMateAlignmentStart(120);
		//reverse mate end - reverse read end = (120+22) - (100 + 22) = 20 (just example)
		recorda.setInferredInsertSize(20);	
		assertTrue(PairedRecordUtils.isF3toF5(recorda));
		//readlength - softclip - tlen = 0-0-20 = -20
		assertTrue(PairedRecordUtils.getOverlapBase(recorda) == -20); 
	
		//readlength - softclip - tlen = 25-0-20 = 1
		recorda.setReadBases("AAAAATTTTCCCCGGGGGGGGAAAA".getBytes());
		assertTrue(PairedRecordUtils.getOverlapBase(recorda) == 5);
		
		
	}
	
	
	@Test
	public void isOverlapInward() {
		// |--------->    <---------|		
		SAMRecord recorda = new SAMRecord(null);		
		// second of mapped pair, read reverse, mate forward
		recorda.setFlags(147);
		recorda.setAlignmentStart(120);
		recorda.setMateAlignmentStart(100);
		assertTrue( PairedRecordUtils.isInward(recorda) );
		//100-120 when read length is 0
		recorda.setInferredInsertSize(-20);	
		//set overlap  = 0 sinve tLen  < 0
		assertTrue(PairedRecordUtils.getOverlapBase(recorda) == 0);
		
		// first of mapped pair, read reverse, mate forward
		recorda.setFlags(83);
		//inward disregards first or second
		assertTrue( PairedRecordUtils.isInward(recorda) );
		assertTrue(PairedRecordUtils.getOverlapBase(recorda) ==0);
		 

		// first of mapped pair, read forward, mate reverse
		recorda.setFlags(99);
		recorda.setAlignmentStart(100);
		recorda.setMateAlignmentStart(120);
		assertTrue( PairedRecordUtils.isInward(recorda) );
		
		//120-100 when read length is 0
		recorda.setInferredInsertSize(20);
		//min_ends - max_starts = 100 -120 
		assertTrue(PairedRecordUtils.getOverlapBase(recorda) == -20);

		//120+20-100 when read length is 20
		recorda.setInferredInsertSize(40);
		//min_ends - max_starts = 100 -120 
		assertTrue(PairedRecordUtils.getOverlapBase(recorda) == -20);
		//min_ends - max_starts = min(140, 125) -120 =25
		recorda.setReadBases("AAAAATTTTCCCCGGGGGGGGAAAA".getBytes());
		assertTrue(PairedRecordUtils.getOverlapBase(recorda) == 5);
		assertTrue( PairedRecordUtils.isInward(recorda) );
		
		//        <----|
		// |--------------------->
		// first of mapped pair, read forward, mate reverse
		recorda.setFlags(99);
		//read length = 50
		recorda.setReadBases("AAAAAAAAAACCCCCCCCCCTTTTTTTTTTGGGGGGGGGGAAAAAAAAAA".getBytes());
		assertTrue( PairedRecordUtils.isInward(recorda) );
		
	}
	
	
	@Test
	public void isOverlapOutward() {
		SAMRecord recorda = new SAMRecord(null);			
		// <---------|(mate)    |--------->(read)	
		// first of mapped pair, read forward, mate reverse
		recorda.setFlags(99);
		recorda.setAlignmentStart(120);
		recorda.setMateAlignmentStart(100);
		assertTrue( PairedRecordUtils.isOutward(recorda) );		
		//tlen = mateEnd - (readStart-1) = 100 - 119 = -19. so overlap == 0				
		
		// <---------------| mate
		//          |----> read
		//set mate length = 50; tlen = (100+50) - 119 = 31
		recorda.setInferredInsertSize(31);
		recorda.setReadBases("AAAAA".getBytes());
		assertTrue( PairedRecordUtils.isOutward(recorda) );	
		//min(100+50, 120+5) - max(100, 120) = 5
		assertTrue(PairedRecordUtils.getOverlapBase(recorda) == 5);
	
		
		// <---------|(read)    |--------->(mate)	
		recorda.setFlags(83);
		recorda.setAlignmentStart(100);
		recorda.setMateAlignmentStart(120);
		assertTrue( PairedRecordUtils.isOutward(recorda) );	
		//since readlength = 5, tlen = 120-105
		recorda.setInferredInsertSize(15);
		System.out.println("PairedRecordUtils.getOverlapBase: " + PairedRecordUtils.getOverlapBase(recorda) );
		assertTrue(PairedRecordUtils.getOverlapBase(recorda) == 0);
		
		
		// 100<---------------| read with 30 base
		//          120|----------> mate
		recorda.setReadBases("AAAAAAAAAACCCCCCCCCCTTTTTTTTTT".getBytes());
		recorda.setInferredInsertSize(-10);
		assertTrue( PairedRecordUtils.isOutward(recorda) );	
		assertTrue(PairedRecordUtils.getOverlapBase(recorda) == 0);
		
		
	}
	
	@Test
	public void otherPairs() {
		// |--------->
		// <-----------|
		
		
		
		
	}
	
	
	
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
