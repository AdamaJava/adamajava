/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.qcmg.qbamfilter.filter;

import htsjdk.samtools.filter.SamRecordFilter;
import htsjdk.samtools.SAMRecord;


public class PosFilter implements SamRecordFilter{

    private final int value;
    private final Comparator op;

        /**
     * initialize alignment start position and query comparator
     * @param comp: see details of valid comparator on org.qcmg.qbamfilter.filter.Comparator.
     * @param value:  a integer string.
     */
    public PosFilter(Comparator comp, String value ) {
        this.value = Integer.parseInt(value);
        op = comp;
    }

   /**
     * check the alignment start position.
     * @param record: a SAMRecord
     * @return true if the value is satisfied by the condition
     * Usage example: if you want filter out all reads with mapping quality higher than 16.
     * SAMRecordFilter myfilter = new PosFilter(Comparator.Great, "16" );
     * if(myfilter.filterOut(record)){ System.out.println(record.toString);}
     */
    @Override
    public boolean filterOut(final SAMRecord record){
        return op.eval(record.getAlignmentStart(), value );
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
