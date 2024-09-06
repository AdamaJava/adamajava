package org.qcmg.picard;

import htsjdk.samtools.SAMRecord;

public class BwaPair {


	public enum Pair{ 
		F3F5(1), F5F3(2), Inward(3), Outward(4), Others(5); 
		public int id;
		Pair(int id){this.id = id;}
	}

	/**
	 *  
	 * @param record a samRrecord
	 * @return overlapped base counts;
	 * @return 0 if no overlap, or record with negative tLen value or tLen value is not available
	 * 
	 */
	public static int getOverlapBase(SAMRecord  record) {
		
		int tLen = record.getInferredInsertSize();
		
	   	/*
	   	 * In sam specification, tLen == 0 means tLen value is not available, such as mate unmapped/different ref.
	   	 * The different read alignment tools may have a slightly different algorithm to calculate TLEN value. 
	   	 * For example, the bwa-meth will assign "0" to tLen if the reads marked as "not proper mapped pair" even they mapped to the same reference. 
	   	 * While the bwa-mem will still assign a real value to tLen, which is the 
	   	 * " tLen = ( forwardMate.start /reverseMate.End ) - ( forwardRead.start / reverseRead.End ) +/- 1 ". 
	   	 * In the case of the bwa-mem reads with "tLen==0", they may be the pair and overlapped with the same strand and same start/end.
	   	 * 
	   	 * In previous algorithm, we use "if(tLen == 0 && !record.getFirstOfPairFlag() ) return 0;" 
	   	 * but It will throw exception when the input is bwa-meth BAM. 
	   	 * 
	   	 * Hence we have to make this overlap algorithm suits all type of BAMs, now we return 0 for all pair with tLen == 0
	   	 *   
	   	 */
	   	if (tLen <= 0 ) return 0;

	    int alignmentEnd = record.getAlignmentEnd();
	   	//two reads not overlapped due to far distance:  --selected---    ---mated---
	    if (record.getMateAlignmentStart() - alignmentEnd > 0) return 0;

	   	int result = 0;
		// |--selected--->    <---mated---|  mate end = tLen -1 + read start  
		if( record.getReadNegativeStrandFlag() != record.getMateNegativeStrandFlag() ) {
			//here tLen > 0 && mate.start <= read.end 
			//so here read must forward  and mate reverse 
			int mateEnd = tLen + record.getAlignmentStart() - 1 ;  //reverse mate end
//			int readEnd = record.getAlignmentEnd();   //forward read end
			
			result = Math.min( alignmentEnd, mateEnd ) - Math.max(record.getAlignmentStart(), record.getMateAlignmentStart() ) + 1;
		} else if ( !record.getReadNegativeStrandFlag()) {
	   		//|--->    or     |-----> (read end - mate start>0) or       |----->
	   		//|----->              |----->                      |---> (read end - mate start < 0)  		
						
			//if tLen >= 0, then mate_start > read_start; so min_end will be read end if we assue pair with same length.    			
			result = alignmentEnd - record.getMateAlignmentStart() + 1;
		} else {
	  		//<---|    or     <-----| (read end - mate start>0) or       <-----|
	   		//<-----|             <-----|                      <---| (read end - mate start < 0)
	   		result = alignmentEnd - Math.max( record.getAlignmentStart(), record.getMateAlignmentStart()) + 1;
		}
		return Math.max(result, 0);
	}

	public static Pair getPairType( SAMRecord  record ) {
	
		if (record.getReadUnmappedFlag() ||  record.getMateUnmappedFlag() || ! record.getReferenceName().equals(record.getMateReferenceName()) )
			return Pair.Others;
		
		// pair with different orientation 
		if( record.getReadNegativeStrandFlag() != record.getMateNegativeStrandFlag()  ) {	
			// |----->  <-------|(read is reverse)
			if( record.getReadNegativeStrandFlag() && record.getAlignmentStart() >= record.getMateAlignmentStart())
				return Pair.Inward;
				
			// (read is forward)|----->  <-------| 
			if( !record.getReadNegativeStrandFlag() && record.getAlignmentStart() <= record.getMateAlignmentStart())
				return Pair.Inward;
	 					
			return Pair.Outward;
		}
				
		//non-canonical: pair with same orientation  		
		//first of pair start
		int s1 = ( record.getFirstOfPairFlag())? record.getAlignmentStart() : record.getMateAlignmentStart();		
		//second of pair start
		int s2 = ( !record.getFirstOfPairFlag())? record.getAlignmentStart() : record.getMateAlignmentStart();
						    	 		 
		//F3(s1)|---->  F5(s2)|------>
		if( !record.getReadNegativeStrandFlag() &&  s1 <= s2)  return Pair.F3F5;
		//F5(s2)<----|  F3(s1)<------|
		if( record.getReadNegativeStrandFlag() &&  s1 >= s2)  return Pair.F3F5;		
		//F5(s2)|---->  F3(s1)|------>
		if( !record.getReadNegativeStrandFlag() &&  s1 > s2)  return Pair.F5F3;
		//F3(s1)<----|  F5(s2)<----|
		if( record.getReadNegativeStrandFlag() &&  s1 < s2)  return Pair.F5F3;
				
		return Pair.Others;
	}

}