/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import org.qcmg.common.util.Constants;

public class LowCoverageRegion {

	private final String refName;
	private final int start;
	private final int end;
	private final int lowReadDepthLimit;

	public LowCoverageRegion(String name,int start, int end, int minCoverage) {
		this.refName = name;
		this.start = start;
		this.end = end;
        this.lowReadDepthLimit = minCoverage;
    }

	public String getRefName() {
		return refName;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	//For BED file format the start position is 0-based and the end position is 1-based
	//So need to decrement by 1 for the start position
	public String toBedString() {
		return refName + Constants.TAB + (start - 1) + Constants.TAB + end;
	}
}
