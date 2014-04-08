/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfilter.filter;

import net.sf.picard.filter.SamRecordFilter;
import net.sf.samtools.SAMRecord;
public class IsizeFilter implements SamRecordFilter{

    private final int value;
    private final Comparator op;

     /**
     * initilize Mapping quality  comparator and operator value
     * @param comp: see details of valid comparator on org.qcmg.qbamfilter.filter.Comparator.
     * @param value:  a integer string.
     * @throws Exception if the string value can't convert to integer
     */
    public IsizeFilter(Comparator comp, String value )throws Exception{
        this.value = Integer.valueOf(value);
        op = comp;
    }

   /**
     * check the mapping quality value.
     * @param record: a SAMRecord
     * @return true when the mapping quality value is satisfied
     * Usage example: if you want filter out all reads with mapping quality higher than 16. 
     * SAMRecordFilter myfilter = new IsizeFilter(Comparator.Great, "16" );
     * if(myfilter.filterout(record)){ System.out.println(record.toString);}
     */
    @Override 
    public boolean filterOut(final SAMRecord record){
    	Integer ob = record.getInferredInsertSize();
        
 
        if(ob != null){
              return op.eval(ob, value );
        }

        return false;
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
