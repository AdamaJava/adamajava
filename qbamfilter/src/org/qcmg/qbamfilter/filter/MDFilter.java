/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfilter.filter;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMTag;
import htsjdk.samtools.filter.SamRecordFilter;

import java.util.BitSet;

public class MDFilter implements SamRecordFilter{
    private final boolean mismatchFilter;
    private final int value;
    private final Comparator op;
    public static final short MD_TAG = SAMTag.makeBinaryTag("MD");

    /**
     * Constructs an MDFilter object to filter SAM records based on the mismatch condition in the MD field.
     * The filter checks whether the specified mismatch condition satisfies the given comparison criteria.
     *
     * @param operatorName The name of the operator being used. Only "mismatch" is valid for this filter.
     * @param comp The comparator defining the condition to be applied. E.g., GreaterEqual, LessThan, etc.
     * @param value A string representing the numeric threshold for the mismatch condition. Must be a valid integer.
     * @throws Exception If the value is not a valid integer or if an invalid operator is provided.
     */
    public MDFilter (String operatorName, Comparator comp, String value ) throws Exception {
        try {
            this.value = Integer.parseInt(value);
        } catch(Exception e) {
            throw new Exception("non integer value used in MD field filter: MD_" +operatorName + comp.getString() + value);
        }
        op = comp;
        if (operatorName.equalsIgnoreCase("mismatch")){
            mismatchFilter = true;
        } else {
            throw new Exception("invalid MD String operator: " + operatorName  + "in query condition MD_" + operatorName );
        }             
    }

    public  static int tallyMDMismatches(String mdData) {
        if (mdData == null || mdData.isEmpty()) {
            return 0;
        }

        int count = 0;
        int size = mdData.length();
        int i = 0;

        while (i < size) {
            char c = mdData.charAt(i);

            if (Character.isDigit(c)) {
                i++;
                while (i < size && Character.isDigit(mdData.charAt(i))) {
                    i++;
                }
            } else if (c == 'A' || c == 'C' || c == 'G' || c == 'T' || c == 'N') {
                count++;
                i++;
            } else if (c == '^') {
                // Skip the segment after '^' (indicating a deletion)
                i++;
                while (i < size && Character.isLetter(mdData.charAt(i))) {
                    i++;
                }
            }
        }
        return count;
    }

    /**
     * check the record base length with required CigarOperator. 
     * @param record: a SAMRecord
     * @return true if the MD indicates mismatch number satisfied by the condition
     * Usage example: if you want filter out all reads with matched base greater equal than 35mers. 
     * SAMRecordFilter myfilter = new MDFilter("mismatch", Comparator.GreatEqual, "3" );
     * if(myfilter.filterOut(record) == true){ System.out.println(record.toString);}
     */
    @Override   
    public boolean filterOut(final SAMRecord record){
        String attribute = (String)record.getAttribute(MD_TAG);

        if (attribute == null) {
            return false;          
        }

        if (mismatchFilter) {
            int mismatch = tallyMDMismatches(attribute);
            return op.eval(mismatch, value );
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
