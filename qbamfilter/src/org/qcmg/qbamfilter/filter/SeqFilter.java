/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfilter.filter;

import htsjdk.samtools.filter.SamRecordFilter;
import htsjdk.samtools.SAMRecord;

public class  SeqFilter implements SamRecordFilter {
	 
    private final int value;
    private final Comparator op;
    
    
    /**
     * An constructor of SeqFilter. Below example shows it check the read with average PHRED quality score greater than 20 
     * @param operatorName eg. "numberN"
     * @param comp	       eg. ">"
     * @param value        eg. "3" 
     * @throws Exception
     */    
    public SeqFilter(String operatorName, Comparator comp, String value) throws Exception{
        try{
            this.value = Integer.valueOf(value);
        }catch(Exception e){
            throw new Exception("non integer value used in QUAL field filter: QUAL_" +operatorName + comp.GetString() + value);
        }
        op = comp;
        if( ! operatorName.equalsIgnoreCase("numberN")){
            
            throw new Exception("invaid QUAL String operator: " + operatorName  + "in query condition QUAL_" + operatorName );
        }             
    }
    
    /**
     * @param record: a SAMRecord
     * @return true if the number of 'N' base is satisfied by the condition
     * Usage example: if you want filter out all reads contain more than 3 'N' base
     * SAMRecordFilter myfilter = new SeqFilter( "numberN", Comparator.Great, "3" );
     * if(myfilter.filterout(record)){ System.out.println(record.toString);}
     */
    @Override
	public boolean filterOut(SAMRecord record) {    	
	    	String base = record.getReadString();
	
	    	//traditional counting base is fast
	    	int count = 0;
	    	for(int i = 0; i < base.length(); i ++) {
	    		if( base.charAt(i) == 'N' ) {
	    			count ++;
	    		}
	    	}
	    	
	    	return op.eval(count, value );
	}
    
    /**
     * It is an inherited method and return false only. 
     */
    @Override @Deprecated
	public boolean filterOut(SAMRecord arg0, SAMRecord arg1) {
		// TODO Auto-generated method stub
		return false;
	}


	
}
