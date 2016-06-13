/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfilter.filter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import htsjdk.samtools.filter.SamRecordFilter;
import htsjdk.samtools.SAMRecord;


public class MDFilter implements SamRecordFilter{
    private boolean MismatchFilter = false;
    private final int value;
    private final Comparator op;
    static final Pattern pa = Pattern.compile("\\d[ATGCN]");
    
    /**
     * Initialize cigar operator name, comparator and operator value
     * @param operatorName : At moment the valid name is "mismatch".
     * @param comp: see details of valid comparator on org.qcmg.qbamfilter.filter.Comparator. 
     * @param value:  a integer string.
     * @throws Exception
     * See usage on method filterout.
     */
    public MDFilter(String operatorName, Comparator comp, String value )throws Exception{
        try{
            this.value = Integer.valueOf(value);
        }catch(Exception e){
            throw new Exception("non integer value used in DM field filter: MD_" +operatorName + comp.GetString() + value);
        }
        op = comp;
        if(operatorName.equalsIgnoreCase("mismatch")){
            MismatchFilter = true;            
        }
        else{
            throw new Exception("invaid MD String operator: " + operatorName  + "in query condition Cigar_" + operatorName );
        }             
    }
    
    /**
     * MD string format are changed, this method counts insert into mismatch mistakenly
     */
    @Deprecated
    public int CountMismatch(String MD){
        
        Matcher m = pa.matcher(MD);
        int mis = 0;
        while(m.find()){mis ++;}

        return mis;
    }
    
    static int tallyMDMismatches(String mdData) {
    	int count = 0;
		if (null != mdData) {
			for (int i = 0, size = mdData.length() ; i < size ; ) {
				char c = mdData.charAt(i);
				if (isValidMismatch(c)) {
					count++;
					i++;
				} else if ('^' == c) {
					while (++i < size && Character.isLetter(mdData.charAt(i))) {}
				} else i++;	// need to increment this or could end up with infinite loop...
			}
		}
		return count;
	}
    
    private static boolean isValidMismatch(char c) {
		return c == 'A' || c == 'C' || c == 'G' || c == 'T' || c == 'N';
	}

    /**
     * check the record base length with required CigarOperator. 
     * @param record: a SAMRecord
     * @return true if the MD indicates mismatch number satisfied by the condition
     * Usage example: if you want filter out all reads with matched base greater equal than 35mers. 
     * SAMRecordFilter myfilter = new MDFilter("mismatch", Comparator.GreatEqual, "3" );
     * if(myfilter.filterout(record) == true){ System.out.println(record.toString);}
     */
    @Override   
    public boolean filterOut(final SAMRecord record){
    	String attribute = (String)record.getAttribute("MD");
    	
        if( attribute == null){
            return false;          
        }

        if(MismatchFilter){
            int mismatch = tallyMDMismatches(attribute);
//            int mismatch = CountMismatch(attribute.toString());
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
