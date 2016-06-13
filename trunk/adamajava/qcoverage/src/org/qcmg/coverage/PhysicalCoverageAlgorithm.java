/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import htsjdk.samtools.SAMRecord;

public class PhysicalCoverageAlgorithm implements Algorithm {
	@Override
	public String getName() {
		return "physical coverage";
	}

	public Job[] map(final Configuration invariants) {
		return null;
	}

	public void reduce(final Job[] jobs) {
	}
	
	@Override
	public void applyTo(final SAMRecord read, Object perBaseCoverages) {
		applyTo(read, perBaseCoverages, false);
	}
//	@Override
//	public void applyTo(final SAMRecord read, final int[] perBaseCoverages) {
//		applyTo(read, perBaseCoverages, false);
//	}
	
	@Override
	public void applyTo(final SAMRecord read, Object perBaseCoveragesObj, boolean fullyPopulated) {
//		@Override
//		public void applyTo(final SAMRecord read, final int[] perBaseCoverages, boolean fullyPopulated) {	
		
		//cast to int array
		int[] perBaseCoverages = (int[]) perBaseCoveragesObj;
		final int readStart = read.getAlignmentStart();
		if (readStart > 0) {
			// Adjust from 1-based to 0-based indexing
			final int length = perBaseCoverages.length;
			if (read.getReadPairedFlag()) {
				final int insertSize = read.getInferredInsertSize();
				
				if (0 <= insertSize) {
					final int end = readStart + insertSize;
					
					// Avoid bad isizes beyond end-of-reference
					// check once - if OK no need to do further checks
					// if fail, check for each base
					if (end < length) {
						if (fullyPopulated) {
							for (int pos = readStart; pos <= end; pos++) {
								perBaseCoverages[pos - 1] ++;
							}
						} else {
							for (int pos = readStart; pos <= end; pos++) {
								if (-1 < perBaseCoverages[pos - 1])
									perBaseCoverages[pos - 1] ++;
							}
							
						}
					} else {
						// Avoid bad isizes beyond end-of-reference
						if (fullyPopulated) {
							for (int pos = readStart; pos <= end && (pos - 1) < length; pos++) {
								perBaseCoverages[pos - 1] ++;
							}
						} else {
							for (int pos = readStart; pos <= end && (pos - 1) < length; pos++) {
								if (-1 < perBaseCoverages[pos - 1])
									perBaseCoverages[pos - 1] ++;
							}
							
						}
					}
				}
			} else {
				final int readEnd = read.getAlignmentEnd();
				if (readEnd < length) {
					if (fullyPopulated) {
						for (int pos = readStart ; pos <= readEnd ; pos++)
							perBaseCoverages[pos - 1] ++;
					} else {
						for (int pos = readStart ; pos <= readEnd ; pos++)
							if (-1 < perBaseCoverages[pos - 1])
								perBaseCoverages[pos - 1] ++;
					}
				} else {
					if (fullyPopulated) {
						for (int pos = readStart ; pos <= readEnd && (pos -1) < length; pos++) {
							perBaseCoverages[pos - 1] ++;
						}
					} else {
						for (int pos = readStart ; pos <= readEnd && (pos -1) < length; pos++) {
							if (-1 < perBaseCoverages[pos - 1])
								perBaseCoverages[pos - 1] ++;
						}
						
					}
				}
			}
		}
	}
}
