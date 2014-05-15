/**
 * Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfilter.filter;

/**
 * It defined six type comparator for qbamfilter query, the evaluate for each comparator (Comparator.eval(v1,v2)) are:
 * GreatEqual: return true if the integer converted from String v1 is greater than or equal to the integer converted from String v2;
 * SmallEqual: return true if the integer converted from String v1 is smaller than or equal to the integer converted from String v2;
 * Great: return true if the integer converted from String v1 is greater than the integer converted from String v2;
 * Small: return true if the integer converted from String v1 is smaller than the integer converted from String v2;
 * Equal: return true if the integer converted from String v1 is equal to the integer converted from String v2;
 * NotEqual:return true if the integer converted from String v1 is not equal to the integer converted from String v2;
 * @author q.xu
 */
public enum Comparator {
        GreatEqual, SmallEqual, Equal, Great, Small, NotEqual,StartWith, EndWith, Contain, NotStartWith, NotEndWith,NotContain;
        
        /**
         * @param v1
         * @param v2
         * it convert string v1 and v2 into integer when Comparator is GreatEqual, SamallEqual,Great and Small
         * then do integer comparison for v1 and v2. Otherwise it straightly does String comparison ignoring Case
         * for Comparator Equal and NotEqual
         * @return true if v1 is GreatEqual than v2 for Comparator GreatEqual;
         * @return true if v1 is Great than v2 for Comparator Great;
         * @return true if v1 is SmallEqual than v2 for Comparator SmallEqual;
         * @return true if v1 is Small than v2 for Comparator Small;
         * @return true if v1 and v2 are same string for Comparator Equal
         * @return true if v1 and v2 are different string for Comparator NotEqual
         */
        public boolean eval(String v1, String v2) {
            switch(this){
                case GreatEqual: return Integer.valueOf(v1) >= Integer.valueOf(v2);
                case SmallEqual: return Integer.valueOf(v1) <= Integer.valueOf(v2);
                case Great: return Integer.valueOf(v1) > Integer.valueOf(v2);
                case Small: return Integer.valueOf(v1) < Integer.valueOf(v2);
                case Equal: return v1.equalsIgnoreCase(v2);
                case NotEqual: return !(v1.equalsIgnoreCase(v2));
                case StartWith: return v1.toLowerCase().startsWith(v2.toLowerCase());
                case NotStartWith: return !v1.toLowerCase().startsWith(v2.toLowerCase());
                case EndWith: return v1.toLowerCase().endsWith(v2.toLowerCase());
                case NotEndWith: return ! v1.toLowerCase().endsWith(v2.toLowerCase());
                case Contain: return  v1.toLowerCase().contains(v2.toLowerCase());
                case NotContain: return  !v1.toLowerCase().contains(v2.toLowerCase());
            }

            throw new AssertionError("Unknow comparator mark:" + this);
        }

 
        /**
         *
         * @param v1: an integer
         * @param v2: and integer
         * @return true if v1 is GreatEqual than v2 for Comparator GreatEqual;
         * @return true if v1 is Great than v2 for Comparator Great;
         * @return true if v1 is SmallEqual than v2 for Comparator SmallEqual;
         * @return true if v1 is Small than v2 for Comparator Small;
         * @return true if v1 is equal to v2 for Comparator Equal;
         * @return true if v1 is not equal to v2 for Comparator NotEqual;
         */
        public boolean eval(int v1, int v2){
            switch(this){
                case GreatEqual: return  (v1 >= v2);
                case SmallEqual: return (v1 <= v2 );
                case Great: return (v1 > v2);
                case Small: return (v1 < v2);
                case Equal: return (v1 == v2);
                case NotEqual: return (v1 != v2);
            }
            throw new AssertionError("Unknow comparator mark:" + this);
        }

        /**
         * @param v1: a float
         * @param v2: a float
         * @return true if float number v1 and v2 satisfied with current comparison
         * see detail return value on documents of eval(int v1, int v2);
         */
        public boolean eval(float v1, float v2){
            switch(this){
                case GreatEqual: return  (v1 >= v2);
                case SmallEqual: return (v1 <= v2 );
                case Great: return (v1 > v2);
                case Small: return (v1 < v2);
                case Equal: return (v1 == v2);
                case NotEqual: return (v1 != v2);
            }
            throw new AssertionError("Unknow comparator mark:" + this);
        }

        /**
         * @param v1 : a boolean value
         * @param v2 : a boolean value
         * @return true if v1 is equal to v2 for Comparator Equal;
         * @return true if v1 isn't equal to v2 for comparator NotEqual;
         * Throw Exception for another type of Comparator;
         */
        public boolean eval(boolean v1, boolean v2){
            switch(this){
                case Equal: return (v1 == v2);
                case NotEqual: return (v1 != v2 );
            }
            throw new AssertionError("Unknow op:" + this);
        }
        
        /**
         * 
         * @param comp: valid string parameter must belong to [">=", ">", "<=", "<", "==", "!="]
         * @return one of the six Comparators based onthe parameter string comp
         * @throws Exception if the parameter comp is not valid.
         */
        public static final Comparator GetComparator(String comp, String value) {
	
           if(comp.equals(">=")){ return GreatEqual;}
           else if( comp.equals("<=")){return SmallEqual; }
           else if( comp.equals(">")){return Great; }
           else if( comp.equals("<")){return Small; }
           else if( comp.equals("==")){return Equal;  }
           else if( comp.equals("!=")){return NotEqual;}
           else if( comp.equals("=~") || comp.equals("!~")){
         	   return GetWildCaseComparator(comp, value);}
           else return null;
            
        }
        
        /**
         * Here we treat '*' in value String as wildcase
         * @param comp must be '=~' or '!~'
         * @param value: String contain single or non '*' 
         * @return
         * @throws Exception unless the single '*' appear at the begin or end of value string 
         */
        public static Comparator GetWildCaseComparator(String comp, String value) {
        	String subStr = GetWildCaseValue(value);
        	
        	if( !comp.equals("=~") && !comp.equals("!~")  )
        		return null;
        	
//        	System.out.println( String.format("GetWildCaseComparator( String %s, String %s  )", comp, value)  );

        	//only allow single or none '*' 
        	if( value.contains("*")  &&  subStr.length() != (value.length() - 1)  )
        		return null;


        	
        	if(value.startsWith("*"))	
        		return comp.equals("=~")? EndWith: NotEndWith;
        	else if(value.endsWith("*")){
        		return comp.equals("=~")? StartWith: NotStartWith; 
        	}else if( ! value.contains("*")){
        		return comp.equals("=~")? Contain: NotContain; 
        	} 
        	
        	return null;
        }        
     
        
        public static String GetWildCaseValue(String value) {
        	//remove all '*'
        	return value.replace("*", ""); 
        	
    
        }       
        
        /**
         * @return comparator string. eg.
         * return ">=" for Comparator.GreatEqual.GetString().
         * return "==" for Comparator.Equal.GetString().
         */
        public String GetString(){
            switch(this){
                case GreatEqual: return ">=";
                case SmallEqual: return "<=";
                case Great: return ">";
                case Small: return "<";
                case Equal: return "==";
                case NotEqual: return "!=";
            }
            throw new AssertionError("Unknow comparator mark:" + this);
        }
}
