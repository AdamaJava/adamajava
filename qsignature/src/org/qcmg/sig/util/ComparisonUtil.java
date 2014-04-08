/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig.util;

import java.io.File;
import java.util.List;

import org.qcmg.sig.model.Comparison;

public class ComparisonUtil {
	
	public static final int MINIMUM_NO_OF_CALCULATIONS = 25000;
	
	public static String getComparisonsBody(List<Comparison> comparisons) {
		if (null == comparisons || comparisons.isEmpty()) throw new IllegalArgumentException("null or empty list of comparisons to headerise");
		
		StringBuilder sb = new StringBuilder();
		File mainFile = comparisons.get(0).getMain();
		if (null != mainFile)
			sb.append(mainFile.getName()).append('\t');
		
		for (Comparison comp : comparisons) {
			
			sb.append(SignatureUtil.nf.format(comp.getScore())).append("[").append(comp.getOverlapCoverage())
				.append(",").append(comp.getNumberOfCalculations()).append("],");
		}
		
		sb.deleteCharAt(sb.length() - 1);		// remove trailing comma
		
		return sb.toString();
	}
	
	public static boolean containsDodgyComparisons(List<Comparison> comparisons, double cutoff) {
		if (null == comparisons || comparisons.isEmpty()) throw new IllegalArgumentException("null or empty list of comparisons to examine");
		
		for (Comparison comp : comparisons) {
			// only care about comparisons that had a large number of calculations
			if (comp.getNumberOfCalculations() < MINIMUM_NO_OF_CALCULATIONS)
				continue;
			if (comp.getScore() > cutoff) return true;
		}
		return false;
	}

}
