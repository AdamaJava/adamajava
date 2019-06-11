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
	 * @return 0 if no overlap, or record with negative tLen value or secondOfPair with zero tLen value
	 * 
	 */
	public static int getOverlapBase(SAMRecord  record) {
		
		int tLen = record.getInferredInsertSize();
		
		//to avoid double counts, we only select one of Pair: tLen > 0 
		//that is we only select the one behind the mate: --selected---    ---mated--
	   	if(tLen < 0) return 0;  
	   	//two reads not overlapped due to far distance:  --selected---    ---mated---
	    if(record.getMateAlignmentStart() - record.getAlignmentEnd() > 0) return 0;
	  	       	
	   	//pick up first of pair if same strand read and tlen=0
	    //warning, mate unmapped/different ref, also tLen == 0; default tLen == 0 if the tLen value is not sure. 
	   	if(tLen == 0 && !record.getFirstOfPairFlag() ) return 0;
	   	       
	   	int result  = 0;  	
		// |--selected--->    <---mated---|  mate end = tLen -1 + read start  
		if( record.getReadNegativeStrandFlag() != record.getMateNegativeStrandFlag()  ) {  		
			//if tLen == 0; mateEnd = read.start, Math.min( readEnd, mateEnd ) = read.start; max(starts) = read.start
			//result = read.start - read.start + 1 = 1
			// outward pair only one base overlap
			if( tLen == 0) {  return 1;   } 
			  			
			//here tLen >= 0 && mate.start <= read.end 
			//so here read must forward  and mate reverse 
			int mateEnd = tLen + record.getAlignmentStart()-1 ;  //reverse mate end
			int readEnd = record.getAlignmentEnd();   //forward read end
			
			result = Math.min( readEnd, mateEnd ) - Math.max(record.getAlignmentStart(), record.getMateAlignmentStart() ) + 1;   
		}else if( !record.getReadNegativeStrandFlag()) { 
	   		//|--->    or     |-----> (read end - mate start>0) or       |----->
	   		//|----->              |----->                      |---> (read end - mate start < 0)  		
			//if tLen >= 0, then mate_start > read_start; so min_end will be read end if we assue pair with same length.    			
			result = record.getAlignmentEnd() - record.getMateAlignmentStart() + 1; 			
		}else {  		
	  		//<---|    or     <-----| (read end - mate start>0) or       <-----|
	   		//<-----|             <-----|                      <---| (read end - mate start < 0)
	   		result = record.getAlignmentEnd() - Math.max( record.getAlignmentStart(), record.getMateAlignmentStart()) + 1;			
		}
		
		return result > 0? result : 0; 	
	}

	public static Pair getPairType( SAMRecord  record ) {
	
		if(record.getReadUnmappedFlag() ||  record.getMateUnmappedFlag() || ! record.getReferenceName().equals(record.getMateReferenceName()) )
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