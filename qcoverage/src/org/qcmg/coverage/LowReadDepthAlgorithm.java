/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

public class LowReadDepthAlgorithm implements Algorithm {

	private final int readdepth_cutoff;

	public LowReadDepthAlgorithm(Integer cutoff) {
		readdepth_cutoff = cutoff;
	}

	@Override
	public String getName() {
		return "low read depth";
	}

	@Override
	public CoverageType getCoverageType() {
		return CoverageType.LOW_READDEPTH;
	}

	@Override
	public void applyTo(final int alignmentStart, final int alignmentEnd, final int insertSize,
			final boolean paired, final int[] differenceArray) {
		if (alignmentStart <= 0) {
			return;
		}
		Algorithm.addRange(differenceArray, alignmentStart, alignmentEnd);
	}

	public int getReaddepthCutoff() {
		return readdepth_cutoff;
	}

}
