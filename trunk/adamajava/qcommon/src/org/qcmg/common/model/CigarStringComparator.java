package org.qcmg.common.model;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Comparator class to compare CIGAR string which are in the following format
 * [0-9x][HIDS]
 * 
 * May need to update at a later date to order the string components differently
 * 
 * @author oholmes
 *
 */
public class CigarStringComparator implements Comparator<String> , Serializable{

	private static final long serialVersionUID = -3624872066493448808L;
	
	@Override
	public int compare(String cigar1, String cigar2) {
		
		final int cigar1Length = cigar1.length();
		final int cigar2Length = cigar2.length();
		
		if (cigar1Length > 1 && cigar2Length > 1) {
		
			final char c1 = cigar1.charAt(cigar1Length-1);
			final char c2 = cigar2.charAt(cigar2Length-1);
			
			if (c1 != c2)
				return c1<c2 ? -1 :  1;
			
			// String are the same - compare the number
			return (Integer.valueOf(cigar1.substring(0, cigar1Length-1))
					.compareTo(Integer.valueOf(cigar2.substring(0, cigar2Length-1))));
		} else
			throw new ClassCastException("Invalid CIGAR string lengths in CigarStringComparator");
	}
}
