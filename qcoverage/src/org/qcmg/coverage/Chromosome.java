/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
//package org.qcmg.coverage;
//
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public class Chromosome implements Comparable<Chromosome>{
//	
//	private String name;
//	
//	
//
//	@Override
//	public boolean equals(Object o) {
//		Chromosome other = (Chromosome) o;
//		return name.equals(other.name);
//	}
//	
//	@Override
//	public int hashCode() {
//		return 31 * name.hashCode();
//	}
//
//	@Override
//	public int compareTo(Chromosome o) {
//		if (this.name.equals(o.name)) {
//			return 0;
//		} else {	        
//	        Pattern numPattern = Pattern.compile("\\d*");
//	        Matcher numMatcherThis= numPattern.matcher(leftChr);
//	        Matcher numMatcherOther = numPattern.matcher(rightChr);
//
//	        // check that there is no X/Y chromosome
//	        if (numMatcherleft.matches() && numMatcherRight.matches()) {
//	            if (Integer.parseInt(leftChr) > Integer.parseInt(rightChr)) {
//	                return true;
//	            }
//	        } else {
//	           	if (leftReference.compareTo(rightReference) > 0) {
//	        		return true;
//	        	} 
//	        }
//	        return false;
//			
//		}
//	}
//
//}
