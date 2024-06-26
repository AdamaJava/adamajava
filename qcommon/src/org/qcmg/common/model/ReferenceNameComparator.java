/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;

public class ReferenceNameComparator implements Comparator<String> , Serializable {

	@Serial
	private static final long serialVersionUID = 3528840046906334666L;
	
	private static final char CHR = 'c';
	private static final char GL = 'G';

	@Override
	public int compare(String s1, String s2) {
		
		final char s1FirstChar = s1.charAt(0);
		final char s2FirstChar = s2.charAt(0);
		if (CHR == s1FirstChar && CHR == s2FirstChar && isNumeric(s1.charAt(3)) && isNumeric(s2.charAt(3))) {
			// chr comparison - only want to compare numeric chr values here, not X,Y,MT
			if (s1.length() < 6 && s2.length() < 6) {
				return Integer.valueOf(s1.substring(3)).compareTo(Integer.valueOf(s2.substring(3)));
			} else {
				// need to cater for long chr names eg chr17_ctg5_hap1
				String s1Int = s1.length() < 6 ? s1.substring(3) : getIntegerFromString(s1.substring(3));
				String s2Int = s2.length() < 6 ? s2.substring(3) : getIntegerFromString(s2.substring(3));
				
				if (s1Int.equals(s2Int)) {
					return s1.compareToIgnoreCase(s2);
				} else {
					return Integer.valueOf(s1Int).compareTo(Integer.valueOf(s2Int));
				}
			}
				
		} else if (GL == s1FirstChar && GL == s2FirstChar) {
			// GL comparison - use float as we have 'GL000192.1' values
			return Float.compare(Float.parseFloat(s1.substring(2)) , Float.parseFloat(s2.substring(2)));
		} else if (isNumeric(s1FirstChar) && isNumeric(s2FirstChar)) {
			return Integer.compare(Integer.parseInt(s1), Integer.parseInt(s2));
		} else {
			return s1.compareToIgnoreCase(s2);
		}
	}
	
	private boolean isNumeric(char ch) {
		return Character.isDigit(ch);
	 }
	
	private String getIntegerFromString(String string) {
		// assume number is at beginning of string
		// eg 17_ctg5_hap1
		 
		StringBuilder returnString = new StringBuilder();
		for (int i = 0, len = string.length() ; i < len; i++) {
			char c = string.charAt(i);
			if (Character.isDigit(c)) returnString.append(c);
			else break;
		}
		return returnString.toString();
	}
}
