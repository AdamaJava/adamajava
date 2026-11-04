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

import org.qcmg.qbamfilter.filter.CigarFilter;
import org.qcmg.qbamfilter.filter.FlagFilter;
import org.qcmg.qbamfilter.filter.IsizeFilter;
import org.qcmg.qbamfilter.filter.MDFilter;
import org.qcmg.qbamfilter.filter.MRNMFilter;
import org.qcmg.qbamfilter.filter.MapQFilter;
import org.qcmg.qbamfilter.filter.PosFilter;
import org.qcmg.qbamfilter.filter.RNameFilter;
import org.qcmg.qbamfilter.filter.TagValueFilter;

public class Expression implements SamRecordFilter{

	queryTree.Operator operator = queryTree.Operator.NULL;
    ArrayList< SamRecordFilter > conditions = new ArrayList<>(); //since boolean is scalar type but Boolean is object type
    ArrayList< Integer > costs = new ArrayList<>(); //evaluation cost of each entry in conditions
    private int cost = 0; //most expensive condition in this expression

    /**
     * @param con: the query condition check results, usually from SAMRecordFilter.filterOut(SAMrecord);
     * add this SAMRecordFilter instance into arrayList named conditions;
     * conditions are kept in ascending order of evaluation cost (ties keep query order), so that
     * cheap checks (eg. flags) can short-circuit before expensive ones (eg. tags) force BAMRecord to decode more data.
     * AND/OR are commutative and filters have no side effects, so the result is unchanged.
     */
    public void addCondition(SamRecordFilter con){
        int c = getCost(con);
        int i = costs.size();
        while (i > 0 && costs.get(i - 1) > c) {
            i--;
        }
        conditions.add(i, con);
        costs.add(i, c);
        cost = Math.max(cost, c);
    }

    /**
     * relative cost of evaluating a filter against a lazily decoded BAMRecord
     * 0: fixed length fields that are always decoded (flag, mapq, pos, isize)
     * 1: reference names (header lookup)
     * 2: cigar (decodes and may validate the cigar)
     * 3: optional fields (decodes all tags)
     * 4: anything else, eg. seq/qual (decodes and scans the read) or unknown filters
     */
    static int getCost(SamRecordFilter filter) {
        if (filter instanceof FlagFilter || filter instanceof MapQFilter
                || filter instanceof PosFilter || filter instanceof IsizeFilter) {
            return 0;
        }
        if (filter instanceof RNameFilter || filter instanceof MRNMFilter) {
            return 1;
        }
        if (filter instanceof CigarFilter) {
            return 2;
        }
        if (filter instanceof MDFilter || filter instanceof TagValueFilter) {
            return 3;
        }
        if (filter instanceof Expression exp) {
            return exp.cost;
        }
        return 4;
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
