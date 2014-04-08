/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import java.util.Comparator;


public class CoverageComparator implements
		Comparator<CoverageModel> {
	@Override
	public int compare(CoverageModel coverageA, CoverageModel coverageB) {
	    return Integer.parseInt(coverageA.getAt()) - Integer.parseInt(coverageB.getAt());
	}
}
