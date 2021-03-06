package org.qcmg.qprofiler.util;

import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;

public class PairedRecordUtils {

	   /**
     * Checks if pair orientation is outward
     * @return true if outward
     */
    public static boolean isOutward( SAMRecord  record ) {          
        if (isReadRightOfMate(record) && isReadForward(record) && !isMateForward(record)) 
            return true;
        else if (isReadLeftOfMate(record) && !isReadForward(record) && isMateForward(record))
            return true;
 
        return false;
    }
       
    /**
     * Checks if pair orientation is inward @return true if inward
     */
    public static boolean isInward(SAMRecord  record) {        
        if ( isReadLeftOfMate(record) && isReadForward(record) && !isMateForward(record) )
            return true;
        else if ( isReadRightOfMate(record) && !isReadForward(record) && isMateForward(record) )
            return true;
       
        return false;
    }
     
    public static boolean isF5toF3(SAMRecord  record) {        
        if (isReadF5(record) && isReadLeftOfMate(record) && isReadForward(record)  && isMateForward(record))  
            return true;
        else if (isReadF5(record) && isReadRightOfMate(record) && !isReadForward(record)  && ! isMateForward(record))
            return true;
        else if (isReadF3(record) && isReadRightOfMate(record) && isReadForward(record)  && isMateForward(record))
            return true;
        else if (isReadF3(record) && isReadLeftOfMate(record) && !isReadForward(record)  && ! isMateForward(record))
            return true;
        
        return false;
    }
        
    public static boolean isF3toF5(SAMRecord  record) {        
   	   if (isReadF3(record) && isReadLeftOfMate(record) && isReadForward(record)   && isMateForward(record))
   		   return true; 
       else if (isReadF3(record) && isReadRightOfMate(record) && !isReadForward(record)  && ! isMateForward(record))
    	   return true; 
       else if (isReadF5(record) && isReadRightOfMate(record) && isReadForward(record)  && isMateForward(record))
    	   return true; 
       else if (isReadF5(record) && isReadLeftOfMate(record) && !isReadForward(record)  && ! isMateForward(record))
    	   return true; 
       
        return false;
    }   
    
    /**
     * 
     * @param record
     * @return false if read and mate from different contig/chromosome or read and mate from same strand
     */
    public static boolean isCanonical(SAMRecord  record){
    	    	
	    	if ( ! isSameReference(record) ) return false; 
	    	
	    	return record.getReadNegativeStrandFlag() != record.getMateNegativeStrandFlag();    	
    }
    
    public static boolean isSameReference(SAMRecord  record) {
	    	if( !record.getReadPairedFlag() || record.getReadUnmappedFlag() || record.getMateUnmappedFlag() ) 
	    		return false; 
	    	
	    	return record.getReferenceName().equals( record.getMateReferenceName() );    	
    }  
    
    /**
     * it only look at proper mapped pair and choose the read with Tlen > 0. It can avoid double overlapping 
     * @param record: the read with Tlen > 0. Since Tlen will be set to 0 for single-segment or mapped to different reference. 
     * @return value = 0 for non-paired read, reads with Tlen<= 0; return value < 0 if no overlap exists ; 
     * return value > 0 which is the number of overlapping
     */
    public static int getOverlapBase(SAMRecord  record) {
	 
		//to avoid double the ovelap base, we only delegate all reverse strand reads and Tlen <= 0
    		int iSize = record.getInferredInsertSize();
		if( ! record.getReadPairedFlag() || iSize <= 0 )  return 0 ;	
	 		
		//get softClip
		int lSoft = 0;
		for (CigarElement ce : record.getCigar().getCigarElements()) {
			if (ce.getOperator().equals(CigarOperator.SOFT_CLIP))  {
				 lSoft += ce.getLength();
			}	 
		}
		//canonical read : readLength - softClip - TLEN 
		if(record.getReadNegativeStrandFlag() == record.getMateNegativeStrandFlag()) {
			return record.getReadLength() - lSoft - iSize;
		}
		//the reverse reads of outward pair has positive tlen, but no overlap
		else if(record.getReadNegativeStrandFlag()){
			return 0;
		} else {
			//non-canocial reads: min(both read_end) - max(both read_start) 
			int mate_end = iSize + record.getAlignmentStart();
			int read_end = record.getAlignmentStart() + record.getReadLength() - lSoft;
			return Math.min( read_end, mate_end ) - Math.max(record.getAlignmentStart(), record.getMateAlignmentStart() );
		}
    }
    
    static boolean isReadLeftOfMate(SAMRecord record ) {   return record.getAlignmentStart() < record.getMateAlignmentStart(); }
    
    static boolean isReadRightOfMate(SAMRecord record) {   return record.getAlignmentStart() > record.getMateAlignmentStart(); }
    
    private static boolean isReadF3(SAMRecord  record) {  return record.getFirstOfPairFlag();  }

    private static boolean isReadF5(SAMRecord  record) {   return record.getSecondOfPairFlag();  }	
	   
    private static boolean isReadForward(SAMRecord record ) {   return ! record.getReadNegativeStrandFlag(); }   
	
    private static boolean isMateForward( SAMRecord record ) {   return ! record.getMateNegativeStrandFlag();  }	
}
