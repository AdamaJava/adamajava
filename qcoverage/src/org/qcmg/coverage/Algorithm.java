/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import htsjdk.samtools.SAMRecord;

interface Algorithm {
	String getName();
	CoverageType getCoverageType();

	/**
	 * Adds the coverage contributed by the given read to the supplied difference array.
	 * <p>
	 * The region of the reference sequence covered by a read is always contiguous, so rather
	 * than incrementing the coverage of every position in that region (which is O(read length)
	 * for each read) the region is recorded in a difference array. The actual per base coverage
	 * values are then obtained with a single pass over that array, which makes the cost of
	 * processing the reads O(number of reads) rather than O(total number of bases).
	 *
	 * @param read read whose coverage is to be added
	 * @param differenceArray array with one element per position of the reference sequence.
	 *        The region [start, end] (0 based, inclusive) is recorded by incrementing
	 *        differenceArray[start] and decrementing differenceArray[end + 1]. The decrement is
	 *        omitted when end is the last position in the array, as it is not required to
	 *        calculate the coverage of any position.
	 */
	default void applyTo(final SAMRecord read, final int[] differenceArray) {
		applyTo(read.getAlignmentStart(), read.getAlignmentEnd(), read.getInferredInsertSize(),
				read.getReadPairedFlag(), differenceArray);
	}

	/**
	 * Adds the coverage contributed by a read with the given properties to the supplied
	 * difference array.
	 * <p>
	 * This is the same as the method that takes a SAMRecord, except that the properties of
	 * the read are supplied directly. This allows the reads to be processed without the
	 * overhead of creating a SAMRecord for each of them.
	 *
	 * @param alignmentStart the first position of the read on the reference sequence (1 based,
	 *        inclusive). Zero if the read is not placed on the reference sequence.
	 * @param alignmentEnd the last position of the read on the reference sequence (1 based,
	 *        inclusive). Zero if the read is unmapped.
	 * @param insertSize the size of the insert that the read is part of, or zero if the read
	 *        is not part of a pair
	 * @param paired true if the read is part of a pair
	 * @param differenceArray array with one element per position of the reference sequence
	 */
	void applyTo(final int alignmentStart, final int alignmentEnd, final int insertSize,
			final boolean paired, final int[] differenceArray);

	/**
	 * Records the coverage of the reference sequence positions between startPos and endPos
	 * (both 1 based and inclusive) in the supplied difference array.
	 * <p>
	 * Positions beyond the end of the reference sequence are ignored.
	 *
	 * @param differenceArray array with one element per position of the reference sequence
	 * @param startPos first position of the region (1 based, inclusive)
	 * @param endPos last position of the region (1 based, inclusive)
	 */
	static void addRange(final int[] differenceArray, final int startPos, final int endPos) {
		final int length = differenceArray.length;
		final int startIndex = startPos - 1;                 // convert to 0 based indexing
		final int endIndex = Math.min(endPos, length) - 1;   // convert to 0 based indexing, clipped to the array
		if (startIndex > endIndex) {
			return;
		}
		differenceArray[startIndex]++;
		if (endIndex + 1 < length) {
			differenceArray[endIndex + 1]--;
		}
	}
}
