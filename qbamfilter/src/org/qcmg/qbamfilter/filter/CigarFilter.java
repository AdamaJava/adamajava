/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfilter.filter;

import htsjdk.samtools.filter.SamRecordFilter;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;

public class CigarFilter  implements SamRecordFilter{
    
    private final CigarOperator operator;
    private final int value;
    private final Comparator op;
    private final boolean shortCutEnabled;
    /**
     * initialize cigar operator name, comparator and operator value
     * 
     * @parm operatorName  At moment the valid name are [M,I,D,N,S,H,P].
     * @param comp refer to valid comparator of org.qcmg.qbamfilter.filter.Comparator.
     * @param value is a integer string.
     * @throws Exception
     * 
     */
    public CigarFilter(String operatorName, Comparator comp, String value )throws Exception{
        this.value = Integer.parseInt(value);
        op = comp;
        if(operatorName.equalsIgnoreCase("M")){operator = CigarOperator.M;}
        else if(operatorName.equalsIgnoreCase("I")){operator = CigarOperator.I;}
        else if(operatorName.equalsIgnoreCase("D")){operator = CigarOperator.D;}
        else if(operatorName.equalsIgnoreCase("N")){operator = CigarOperator.N;}
        else if(operatorName.equalsIgnoreCase("S")){operator = CigarOperator.S;}
        else if(operatorName.equalsIgnoreCase("H")){operator = CigarOperator.H;}
        else if(operatorName.equalsIgnoreCase("P")){operator = CigarOperator.P;}
        else{
            throw new Exception("invalid Cigar String operator: " + operatorName  + "in query condition Cigar_" + operatorName );
        }
        shortCutEnabled = op == Comparator.GreatEqual || op == Comparator.Great;
    }

    /**
     * check the record base length with required CigarOperator. 
     * @param record: a SAMRecord
     * @return true if the length is satisfied by the condition
     * Usage example: if you want filter out all reads with matched base greater equal than 35mers. 
     * SAMRecordFilter myfilter = new CIGARFilter("M", Comparator.GreatEqual, "35" );
     * if(myfilter.filterout(record) == true){ System.out.println(record.toString);}
     */
    @Override
    public boolean filterOut(final SAMRecord record){
        Cigar cigar = record.getCigar();
        int result  = 0;
        
        //eg. cigar = "25M5S15M", here r 2 operator "M" and 1 "S"
        for (CigarElement element : cigar.getCigarElements()) {
            if (operator == element.getOperator()) {
                result += element.getLength();
                // Early termination if result already exceeds the threshold
                if (shortCutEnabled && result > value) {
                    return true; // op.eval(result, value) will always return true
                }
            }
        }
        return op.eval(result, value );
    }
    
    /**
     * It is an inherited method and return false only. 
     */
	@Override
	@Deprecated
	public boolean filterOut(SAMRecord arg0, SAMRecord arg1) {
		// TODO Auto-generated method stub
		return false;
	}

}
