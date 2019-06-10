package org.qcmg.picard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.qcmg.picard.BwaPair.Pair;

import htsjdk.samtools.SAMRecord;

public class BwaPairTest {

	@Test
	public void isOverlapF3F5() {
		//read   : first of pair    |---->        or  	   <------|      
		//read   : second of pair        |---->   or  <-----|    
		SAMRecord recorda = new SAMRecord(null);
		// second of mapped pair, both forward 
		recorda.setFlags(131);		
		recorda.setAlignmentStart(120);
		recorda.setMateAlignmentStart(100);
		assertFalse( BwaPair.getPairType(recorda).equals(Pair.F5F3 ));
		assertEquals( BwaPair.getPairType(recorda), Pair.F3F5 );
		
		//only count overlap on reads with positive tlen
		recorda.setInferredInsertSize(-20);		//100-120	
		assertTrue(BwaPair.getOverlapBase(recorda) == 0);
				
		//first pair  fully overlap with tLen == 0 since both forward orientation
		recorda.setCigarString("75M");
		//first of mapped pair, both forward
		recorda.setFlags(65);
		recorda.setAlignmentStart(7480169);
		recorda.setMateAlignmentStart(7480169);		
		assertEquals( BwaPair.getPairType(recorda), Pair.F3F5 );
		//same start first of pair
		recorda.setInferredInsertSize(0);	
		assertTrue(BwaPair.getOverlapBase(recorda) == 75); 
		//same start second of pair
		recorda.setFlags(179);
		assertTrue(BwaPair.getOverlapBase(recorda) == 0); 
		
		//now set to second of mapped pair, both reverse 
		//ST-E00180:52:H5LNMCCXX:1:1116:24274:5247 113 chr1 27977105 60 5S145M = 27977205 0 
		recorda.setAlignmentStart(105);
		recorda.setMateAlignmentStart(205);
		assertEquals( BwaPair.getPairType(recorda), Pair.F3F5 );
		
		//reverse mate end - reverse read end = (120+21-1) - (100 + 21-1) = 20 (just example)
		recorda.setInferredInsertSize(20);	
		recorda.setCigarString("121M"); //set cigar, read end is 100 = 25 -1
		//min_end - max_starts = (105+121-1) - 205, since cigar is nothing
		assertTrue(BwaPair.getOverlapBase(recorda) == 21); 		 
		
		// 105 <------------|225
		// 		205 <-------|225		
		recorda.setInferredInsertSize(0);
		//second of pair overlap ignor if tLen = 0
		assertTrue(BwaPair.getOverlapBase(recorda) == 0); 
		//first of pair overlap >= 0 if tLen = 0
		recorda.setFlags(115);
		assertTrue(BwaPair.getOverlapBase(recorda) == 21); 
	}

	@Test
	public void isOverlapF5F3() {
		//read   : first of pair 	   |---->  or  <------|      
		//read   : second of pair  |---->      or  	        <-----|    
		SAMRecord recorda = new SAMRecord(null);		
		
		//second Of Pair with positive tlen, and both forward
		//ST-E00180:52:H5LNMCCXX:1:2111:15616:38526 129 chr1 56131814 60 150M = 56131947 134 		
		recorda.setFlags(129);
		recorda.setInferredInsertSize(134 );
		recorda.setCigarString("150M");
		recorda.setAlignmentStart(56131814);
		recorda.setMateAlignmentStart(56131947);
		assertTrue(BwaPair.getOverlapBase(recorda) == 56131964 - 56131947); 		
						
		//now set to first of mapped pair, both reverse 
		recorda.setFlags(115);
		recorda.setAlignmentStart(100);
		recorda.setMateAlignmentStart(119);
		//reverse mate end - reverse read end = (119+149) - (100 + 149) + 1 = 20 (just example)
		recorda.setInferredInsertSize(20);
		//overlap = read_ends - max_starts = 249 - 119 + 1 = 131
		assertTrue(BwaPair.getOverlapBase(recorda) == 131);
		 		
		//now set to second of mapped pair, both forward 
		recorda.setFlags(131);
		assertEquals( BwaPair.getPairType(recorda), Pair.F5F3 );
		assertTrue(BwaPair.getOverlapBase(recorda) == 131);
		
		//when there is no overlap
		recorda.setMateAlignmentStart(250);
		recorda.setInferredInsertSize(151);
		assertTrue(BwaPair.getOverlapBase(recorda) == 0);			
		recorda.setMateAlignmentStart(260);
		assertTrue(BwaPair.getOverlapBase(recorda) == 0);	
	}

	@Test
		public void isOverlapInward() {
	//		PairSummary pairS = new PairSummary(Pair.Inward, true );	
			// |--------->    <---------|		
			SAMRecord recorda = new SAMRecord(null);		
			// second of mapped pair, read reverse, mate forward
			recorda.setFlags(147);
			recorda.setAlignmentStart(120);
			recorda.setMateAlignmentStart(100);
			assertEquals( BwaPair.getPairType(recorda), Pair.Inward );
		
			//100-120 when read length is 0
			recorda.setInferredInsertSize(-20);	
			//set overlap  = 0 sinve tLen  < 0
			assertTrue(BwaPair.getOverlapBase(recorda) == 0);
			
			// first of mapped pair, read reverse, mate forward
			recorda.setFlags(83);
			//inward disregards first or second
			assertEquals( BwaPair.getPairType(recorda), Pair.Inward );
			assertTrue(BwaPair.getOverlapBase(recorda) ==0);
			 
	
			// first of mapped pair, read forward, mate reverse
			recorda.setFlags(99);
			recorda.setAlignmentStart(100);
			recorda.setMateAlignmentStart(120);
			assertEquals( BwaPair.getPairType(recorda), Pair.Inward );
			
			//120-100 when read length is 0
			recorda.setInferredInsertSize(20);
			//min_ends - max_starts = 99 -120 (cigar string is nothing)
			assertTrue(BwaPair.getOverlapBase(recorda) == 0);
	
			//120+20-100 when read length is 20
			recorda.setInferredInsertSize(40);
			//min_ends - max_starts = 99 -120 (cigar string is nothing)
			assertTrue(BwaPair.getOverlapBase(recorda) == 0);
			//min_ends - max_starts = min(140, 125) -120 =25
			
			
			// 100|--------->124
			//       120 <----|	140			 
			recorda.setCigarString("25M");
			//min_ends - max_starts = 124 -120+1 (end = start + cigar.M-1)
			assertTrue(BwaPair.getOverlapBase(recorda) == 5);
			assertEquals( BwaPair.getPairType(recorda), Pair.Inward  );
			
			// 100|--------------------->149
			//       120<----|139		
			// first of mapped pair, read forward, mate reverse
			recorda.setCigarString("50M");
			assertEquals( BwaPair.getPairType(recorda), Pair.Inward );
			//min_ends - max_starts = 140 -120 (read end = start + cigar.M-1 = 149. mateEnd2= readStart-1+tLen = 139)
			assertTrue(BwaPair.getOverlapBase(recorda) == 20);
			
		}

	@Test
	public void isOverlapOutward() {
	
		SAMRecord recorda = new SAMRecord(null);			
		// <---------|(mate)    |--------->(read)	
		// first of mapped pair, read forward, mate reverse
		recorda.setFlags(99);
		recorda.setAlignmentStart(120);
		recorda.setMateAlignmentStart(100);
		assertEquals( BwaPair.getPairType(recorda), Pair.Outward);
		assertTrue(BwaPair.getOverlapBase(recorda) == 1); //tLen == 0
		recorda.setInferredInsertSize(-10); //tLen < 0
		assertTrue(BwaPair.getOverlapBase(recorda) == 0);		
		
		//100 <---------------|150 mate
		//          120|---->124 read
		//set mate length = 50; tlen = (100+50) - 119 = 31
		recorda.setInferredInsertSize(31);
		recorda.setCigarString("5M");
		assertEquals( BwaPair.getPairType(recorda), Pair.Outward);
		//min(120-1+31, 120+5-1) - max(100, 120) +1= 5
		assertTrue(BwaPair.getOverlapBase(recorda) == 5);
			
		// 100<----|104(read)    120|--------->(mate)	
		recorda.setFlags(83);
		recorda.setAlignmentStart(100);
		recorda.setMateAlignmentStart(120);
		assertEquals( BwaPair.getPairType(recorda), Pair.Outward);
		//since readlength = 5, tlen = 120-104 + 1
		recorda.setInferredInsertSize(17);
		//overlap = min_ends - max_start + 1= min(104, 120+?) - 120 + 1
		assertTrue(BwaPair.getOverlapBase(recorda) == 0);
		
	}

}
