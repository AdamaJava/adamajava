package org.qcmg.common.model;

import java.io.Serializable;
import java.util.Comparator;


public class PileupElementComparator implements Comparator<PileupElement> , Serializable {

	private static final long serialVersionUID = -3776703951841780132L;
	private static final char DOT = '.';

	/**
	 * Compares first the total count, whether its the reference, and finally the base quality scores
	 * 
	 */
	public int compare(PileupElement p1, PileupElement p2) {
		int compareSize = p2.getTotalCount() - p1.getTotalCount();
		if (compareSize != 0)
			return compareSize;
		if (DOT == p2.getBase()) 
			return 1;
		else if (DOT == p1.getBase()) 
			return -1;
		int compareBaseQuals = p2.getTotalQualityScore() - p1.getTotalQualityScore();
		return compareBaseQuals;
		// don't know what to do if the qualities are the same
	}
	
}
