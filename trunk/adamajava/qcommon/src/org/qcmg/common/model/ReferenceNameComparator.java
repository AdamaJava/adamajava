/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import java.io.Serializable;
import java.util.Comparator;

public class ReferenceNameComparator implements Comparator<String> , Serializable {

	private static final long serialVersionUID = 3528840046906334666L;
	
	private static final char CHR = 'c';
	private static final char GL = 'G';

	@Override
	public int compare(String s1, String s2) {
		
		if (CHR == s1.charAt(0) && CHR == s2.charAt(0) && isNumeric(s1.charAt(3)) && isNumeric(s2.charAt(3))) {
			// chr comparison - only want to compare numeric chr values here, not X,Y,MT
			
			
			if (s1.length() < 6 && s2.length() < 6)
				return Integer.valueOf(s1.substring(3)).compareTo(Integer.valueOf(s2.substring(3)));
			else {
				// need to cater for long chr names eg chr17_ctg5_hap1
				String s1Int = s1.length() < 6 ? s1.substring(3) : getIntegerFromString(s1.substring(3));
				String s2Int = s2.length() < 6 ? s2.substring(3) : getIntegerFromString(s2.substring(3));
				
				if (s1Int.equals(s2Int)) {
					return s1.compareToIgnoreCase(s2);
				} else {
					return Integer.valueOf(s1Int).compareTo(Integer.valueOf(s2Int));
				}
			}
				
		} else if (GL == s1.charAt(0) && GL == s2.charAt(0)) {
			// GL comparison - use float as we have 'GL000192.1' values
			return Float.compare(Float.parseFloat(s1.substring(2)) , Float.parseFloat(s2.substring(2)));
		} else {
			return s1.compareToIgnoreCase(s2);
//			return s1.compareTo(s2);
		}
	}
	
	private boolean isNumeric(char ch) {
	   return ch >= '0' && ch <= '9';
	 }
	
	private String getIntegerFromString(String string) {
		// assume number is at beginning of string
		// eg 17_ctg5_hap1
		String returnString = "";
		for (int i = 0, len = string.length() ; i < len; i++) {
			char c = string.charAt(i);
			if (Character.isDigit(c)) returnString+= c;
			else break;
		}
		return returnString;
	}
}
