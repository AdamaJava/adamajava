package org.qcmg.coverage;

/**
 * Utility methods used by the algorithm tests to convert between the difference array
 * that is used to calculate the coverage and the per base coverage values.
 */
public class CoverageArrayUtils {

	/**
	 * Converts a difference array into the per base coverage values, masking out any
	 * position that is not covered by a feature.
	 *
	 * @param differenceArray the difference array populated by the algorithms
	 * @param mask one element per position of the reference sequence. Positions with a
	 *        negative value are not covered by a feature and are set to -1 in the result.
	 * @return the per base coverage values
	 */
	public static int[] toMaskedPerBaseCoverage(final int[] differenceArray, final int[] mask) {
		final int[] perBaseCoverage = new int[differenceArray.length];
		int runningTotal = 0;
		for (int i = 0; i < differenceArray.length; i++) {
			runningTotal += differenceArray[i];
			perBaseCoverage[i] = (mask[i] < 0) ? -1 : runningTotal;
		}
		return perBaseCoverage;
	}

	/**
	 * Converts a difference array into the per base coverage values.
	 *
	 * @param differenceArray the difference array populated by the algorithms
	 * @return the per base coverage values
	 */
	public static int[] toPerBaseCoverage(final int[] differenceArray) {
		final int[] perBaseCoverage = new int[differenceArray.length];
		int runningTotal = 0;
		for (int i = 0; i < differenceArray.length; i++) {
			runningTotal += differenceArray[i];
			perBaseCoverage[i] = runningTotal;
		}
		return perBaseCoverage;
	}

	/**
	 * Converts an array that indicates which positions are covered by a feature into the
	 * equivalent list of ranges. Overlapping and adjacent positions are merged.
	 *
	 * @param mask one element per position of the reference sequence. Positions with a
	 *        negative value are not covered by a feature.
	 * @return array of 0-based, inclusive ranges, ordered by start position
	 */
	public static int[][] toRanges(final int[] mask) {
		final java.util.List<int[]> ranges = new java.util.ArrayList<>();
		for (int i = 0; i < mask.length; i++) {
			if (mask[i] >= 0) {
				if (!ranges.isEmpty() && ranges.get(ranges.size() - 1)[1] == i - 1) {
					ranges.get(ranges.size() - 1)[1] = i;
				} else {
					ranges.add(new int[] { i, i });
				}
			}
		}
		return ranges.toArray(new int[ranges.size()][]);
	}
}
