/**
 *  Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfilter.filter;
import htsjdk.samtools.filter.SamRecordFilter;
import htsjdk.samtools.SAMRecord;

public class RNameFilter implements SamRecordFilter{
    private final String value;
    private final Comparator op;

    /**
     * Initialize Reference filter  comparator and operator value
     * @param comp: only Equal and NotEqual are valid,
     * see details of comparator on org.qcmg.qbamfilter.filter.Comparator.
     * @param value:  reference name string.
     * @throws Exception if using invalid comparator
     * See usage on method filterout.
     */
    public RNameFilter(Comparator comp, String value )throws Exception{
        
        this.value =  value;
        
        if( comp == Comparator.Great  || comp == Comparator.Small 
        		|| comp ==Comparator.SmallEqual || comp == Comparator.GreatEqual )
        	throw new Exception("invalid comparator symbols used for RNAME filter: " + comp.GetString());
        
        this.op = comp;
    }

   /**
     * check the reference name. return true if the value is satisfied by the condition
     * @param record: a SAMRecord
     * @return true if the SAMRecord mapped on specified reference
     * Usage example: if you want filter out all reads mapped on "chr1"
     * SAMRecordFilter myfilter = new RNameFilter(Comparator.Equal, "chr1" );
     * if(myfilter.filterout(record)){ System.out.println(record.toString);}
     */
    @Override
    public boolean filterOut(final SAMRecord record){
        String ref = record.getReferenceName();

        if(value.equalsIgnoreCase("MRNM")){
            String mref = record.getMateReferenceName();
            return op.eval(ref, mref);
        }

        return op.eval(ref, value);
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
