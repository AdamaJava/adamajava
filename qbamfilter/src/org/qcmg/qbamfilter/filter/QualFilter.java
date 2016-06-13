/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfilter.filter;

import htsjdk.samtools.filter.SamRecordFilter;
import htsjdk.samtools.SAMRecord;

public class QualFilter implements SamRecordFilter {
	private boolean averageFilter = false;
    private final int value;
    private final Comparator op;
    
    /**
     * An constructor of QualFilter. Below example shows it check the read with average PHRED quality score greater than 20 
     * @param operatorName eg. "average"
     * @param comp	       eg. ">"
     * @param value        eg. "20" 
     * @throws Exception
     */
    public QualFilter(String operatorName, Comparator comp, String value) throws Exception{
        try{
            this.value = Integer.valueOf(value);
        }catch(Exception e){
            throw new Exception("non integer value used in QUAL field filter: QUAL_" +operatorName + comp.GetString() + value);
        }
        op = comp;
        if(operatorName.equalsIgnoreCase("average")){
            averageFilter = true;            
        }
        else{
            throw new Exception("invaid QUAL String operator: " + operatorName  + "in query condition QUAL_" + operatorName );
        }             
    }
    
    /**
     * @param record: a SAMRecord
     * @return true if the average base quality score is satisfied by the condition
     * Usage example: if you want filter out all reads average base quality score less than 30
     * SAMRecordFilter myfilter = new QualFilter( "average", Comparator.Small, "30" );
     * if(myfilter.filterout(record)){ System.out.println(record.toString);}
     */
    @Override
	public boolean filterOut(final SAMRecord record){
    	//we may test the case of *, that means the quality value is missing
    	byte[] qualities = record.getBaseQualities();
    	
    	int total = 0;
    	for (byte q : qualities) total += q;
    	int ave = total / qualities.length;
    
    	return op.eval(ave, value );
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
 
