/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfilter.filter;

import htsjdk.samtools.SAMTag;
import htsjdk.samtools.filter.SamRecordFilter;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMTagUtil;

public final class TagValueFilter implements SamRecordFilter{

    private final short tagShort;
    private final String value;
    private final Comparator op;
    private final boolean intCompare; //true if value is an integer, and op can compare it numerically
    private final int intValue;


    /**
     * initialise optional field name, comparator and field value
     * @param tag : the optional field name,it will be converted to upper case automatically.
     * @param comp: see details of valid comparator on org.qcmg.qbamfilter.filter.Comparator.
     * @param value:  a string value.
     * See usage on method filterOut.
     */
    public TagValueFilter(String tag, Comparator comp, String value ){
        tagShort = SAMTag.makeBinaryTag(tag);
        this.value = value;
        op = comp;

        // only use the integer path when it gives the same answer as eval(String, String),
        // ie. value is in canonical form ("05" or "+5" would not equal "5" as strings)
        Integer parsed = null;
        try {
            parsed = Integer.parseInt(value);
            if ( ! Integer.toString(parsed).equals(value)) {
                parsed = null;
            }
        } catch (NumberFormatException e) {
            // not an integer - use string comparison
        }
        intCompare = parsed != null && op.isNumeric();
        intValue = parsed != null ? parsed : 0;
    }

    /**
     * check the optional filed in SAMRecord. return true if that field value is satisfied by the condition
     * @param record: a SAMRecord
     * @return true if this optional field is satisfied with the query
     * Usage example: if you want filter out all reads with field "ZM",and its value is one.
     * CigarFilter myfilter = new TagValueFilter("ZM",Comparator.Equal, "1" );
     * if(myfilter.filterout(record) == true){ System.out.println(record.toString);}
     */
    @Override
    public boolean filterOut(final SAMRecord record){
        Object ob = record.getAttribute(tagShort);
        if (ob != null) {
            // integer tags (eg. NH:i:1) - avoid converting to String and parsing back again
            if (intCompare && (ob instanceof Integer || ob instanceof Short || ob instanceof Byte)) {
                return op.eval(((Number) ob).intValue(), intValue);
            }
            return op.eval(ob.toString(), value);
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
