/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import htsjdk.samtools.SAMRecord;

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

	public int getReaddepthCutoff() {
		return readdepth_cutoff;
	}

}
