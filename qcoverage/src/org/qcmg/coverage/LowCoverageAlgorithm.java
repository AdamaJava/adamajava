/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import htsjdk.samtools.Cigar;
import htsjdk.samtools.SAMRecord;

public class LowCoverageAlgorithm implements Algorithm {

	private final int lowCoverageTumour;
	private final int lowCoverageControl;

	public LowCoverageAlgorithm(Integer lowCovTumour, Integer lowCovControl) {
		lowCoverageTumour = lowCovTumour;
		lowCoverageControl = lowCovControl;
	}

	@Override
	public String getName() {
		return "low coverage";
	}

	@Override
	public CoverageType getCoverageType() {
		return CoverageType.LOW_COVERAGE;
	}

//	@Override
//	public void applyTo(final SAMRecord read, final int[] perBaseCoverages) {
	@Override
	public void applyTo(final SAMRecord read, Object perBaseCoveragesObj) {
		
		int[] perBaseCoverages = (int[]) perBaseCoveragesObj;
		final int readStart = read.getAlignmentStart();
		if (readStart > 0) {
			// check that read end is not greater than array length.
			// if it is, limit loop iteration to end of array
			final int loopEnd =  Math.min(read.getAlignmentEnd(), perBaseCoverages.length);
			for (int pos = readStart - 1 ; pos < loopEnd ; pos++) {
				if (-1 < perBaseCoverages[pos]) {
					perBaseCoverages[pos] ++;
				}
			}
		}
	}
	
	@Override
	public void applyTo(final SAMRecord read, Object perBaseCoverages, boolean fullyPopulated) {
		applyTo(read, perBaseCoverages);
	}

	public Integer getLowCoverageTumour() {
		return lowCoverageTumour;
	}

	public Integer getLowCoverageControl() {
		return lowCoverageControl;
	}
}
