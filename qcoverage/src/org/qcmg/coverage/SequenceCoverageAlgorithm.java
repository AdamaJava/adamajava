/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import htsjdk.samtools.SAMRecord;

public class SequenceCoverageAlgorithm implements Algorithm {
	@Override
	public String getName() {
		return "sequence coverage";
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
}
