/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfilter.grammars;

import java.util.ArrayList;

import htsjdk.samtools.filter.SamRecordFilter;
import htsjdk.samtools.SAMRecord;

public class Expression implements SamRecordFilter{

	queryTree.Operator operator = queryTree.Operator.NULL;
    ArrayList< SamRecordFilter > conditions = new ArrayList<>(); //since boolean is scalar type but Boolean is object type

    /**
     * @param con: the query condition check results, usually from SAMRecordFilter.filterOut(SAMrecord);
     * add this SAMRecordFilter instance into arrayList named conditions;
     */
    public void addCondition(SamRecordFilter con){
        conditions.add(con);
    }

    /**
     * @param op: Operator [and, or] from query;
     * Add this Operator into current instance arrayList named operator;
     */
    public void addOperator(queryTree.Operator op){
    	//don't worry multi operator exist before left parent, antlr will report error	
        operator = op;   
    }

    /**
     * it execute the query based on the filter condition and operator
     * and operators stored in operators arrayList.
     * @return ture if one of condition return true in case of "OR";
     * @return false if one of condition return false in case of "AND";
     */
    @Override
	public boolean filterOut(final SAMRecord record){

	    	if(operator == queryTree.Operator.AND){
                for (SamRecordFilter condition : conditions) {
                    if (!condition.filterOut(record))
                        return false;
                }
		    	return true;
	    	}else{ // case of OR
                for (SamRecordFilter condition : conditions) {
                    if (condition.filterOut(record))
                        return true;
                }
	    		return false;
	    	}
    }

	@Override
	@Deprecated
	public boolean filterOut(SAMRecord arg0, SAMRecord arg1) {
		// TODO Auto-generated method stub
		return false;
	}


}
