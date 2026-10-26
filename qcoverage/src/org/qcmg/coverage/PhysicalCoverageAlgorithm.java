/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

public class PhysicalCoverageAlgorithm implements Algorithm {
	@Override
	public String getName() {
		return "physical coverage";
	}

	@Override
	public CoverageType getCoverageType() {
		return CoverageType.PHYSICAL;
	}

	@Override
	public void applyTo(final int alignmentStart, final int alignmentEnd, final int insertSize,
			final boolean paired, final int[] differenceArray) {
		if (alignmentStart <= 0) {
			return;
		}
		if (paired) {
			// for paired reads the physical coverage is the region spanned by the pair
			if (insertSize < 0) {
				return;
			}
			Algorithm.addRange(differenceArray, alignmentStart, alignmentStart + insertSize);
		} else {
			Algorithm.addRange(differenceArray, alignmentStart, alignmentEnd);
		}
	}
}
