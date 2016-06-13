/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfilter.grammars;

import htsjdk.samtools.filter.SamRecordFilter;
import org.qcmg.qbamfilter.filter.CigarFilter;
import org.qcmg.qbamfilter.filter.Comparator;
import org.qcmg.qbamfilter.filter.FlagFilter;
import org.qcmg.qbamfilter.filter.IsizeFilter;
import org.qcmg.qbamfilter.filter.MDFilter;
import org.qcmg.qbamfilter.filter.MRNMFilter;
import org.qcmg.qbamfilter.filter.MapQFilter;
import org.qcmg.qbamfilter.filter.PosFilter;
import org.qcmg.qbamfilter.filter.RNameFilter;
import org.qcmg.qbamfilter.filter.TagValueFilter;
import org.qcmg.qbamfilter.filter.SeqFilter;
import org.qcmg.qbamfilter.filter.QualFilter;

/**
 * construct the condition node for query tree. eg. "qual > 30"
 */
public class Condition {
    static final String FLAG = "flag";
    static final String RNAME = "rname";
    static final String MRNM = "mrnm";
    static final String POS = "pos";
    static final String MAPQ = "mapq";
    static final String Cigar = "cigar";
    static final String OPTION = "option";
    static final String MD = "MD";
    static final String ISIZE = "isize";
    static final String SEQ = "seq";
    static final String QUAL = "qual";
    
	private final String key;
	private final String value;
	private final String comp; 
	private final Comparator op;

    /**
     * @param key: This constructor will select related filter based on this string parameter
     * @param comp: This constructor will select related Comparator Type based on this parameter
     * @param value: pass this String value to selected filter;
     */
    Condition(String key, String comp, String value) throws Exception{
		this.key = key;

		this.comp = comp;
		 
		if(Comparator.GetWildCaseComparator(comp, value) != null)
			this.value = Comparator.GetWildCaseValue(value);
		else
			this.value = value;
		
		op = Comparator.GetComparator(comp, value);
		
		if(op == null)
			throw new Exception(String.format("invalide condition in query: %s %s %s ", key, comp, value));

    }
    
	@Override
	/*
	 * make each condition unique and be able to store on hash table if needed.   
	 * 
	 */
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((comp == null) ? 0 : comp.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	/*
	 * compare with other condition
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Condition other = (Condition) obj;
		if (comp == null) {
			if (other.comp != null)
				return false;
		} else if (!comp.equals(other.comp))
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	/**
	 * 
	 * @param key: BAM file fields. eg. POS, ISIZE, option_<TAG>...
	 * @param comp: comparison, eg. >, <, ==, >=, <=, !=
	 * @param value: indicate the filed value, eg. POS > 1000
	 * @return true or false.
	 * @throws Exception
	 */
	public SamRecordFilter getFilter() throws Exception {
        	int underscorePosition = key.indexOf("_");
        	String firstElement = null;
        	String secondElement = null;
        	if( -1 == underscorePosition){
        		firstElement = key;        		
            } else {
        		firstElement = key.substring(0, underscorePosition);
        		secondElement = key.substring(underscorePosition+1);
        	} if(firstElement.equalsIgnoreCase(OPTION)){
               return  new TagValueFilter(secondElement, op, value );
            }
            else if(firstElement.equalsIgnoreCase(MAPQ)){
               return  new MapQFilter( op, value );
            }
            else if(firstElement.equalsIgnoreCase(ISIZE)){
               return  new IsizeFilter( op, value );
            }
           else if(firstElement.equalsIgnoreCase(Cigar)){
               return  new CigarFilter(secondElement, op, value );
            }
            else if(firstElement.equalsIgnoreCase(FLAG)){
               return  new FlagFilter(secondElement, op, value );
            }
            else if(firstElement.equalsIgnoreCase(POS)){
                return  new PosFilter( op, value );
            }
            else if(firstElement.equalsIgnoreCase(RNAME)){
               return  new RNameFilter( op, value );
            }
            else if(firstElement.equalsIgnoreCase(MRNM)){
               return  new MRNMFilter( op, value );
            }
            else if(firstElement.equalsIgnoreCase(MD)){
               return new MDFilter( secondElement,op, value );
            }
            else if(firstElement.equalsIgnoreCase(QUAL)){
                return new QualFilter( secondElement,op, value );
             }
            else if(firstElement.equalsIgnoreCase(SEQ)){
                return new SeqFilter( secondElement,op, value );
             }            
           else{
                throw new Exception("invalid bam field \"" + key + "\" used in query.");
           }
	}
}
