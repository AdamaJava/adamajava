/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

/**
 * Holds the information about a BAM file that is required to process one of the
 * reference sequences that it contains.
 * <p>
 * The file pointers identify the chunks of the BAM file that contain the reads of the
 * reference sequence. They are calculated once, using the shared BAM index, so that each
 * of the jobs can read its reads without having to parse the index itself.
 */
final class BamFileInfo {

	private final long[] filePointers;
	private final int refIndex;

	BamFileInfo(final long[] filePointers, final int refIndex) {
		this.filePointers = filePointers;
		this.refIndex = refIndex;
	}

	/**
	 * @return the file pointers of the reads of the reference sequence, in the format used
	 *         by the BAM index: the start and the end of each chunk of the file. An empty
	 *         array indicates that the BAM file contains no reads for the reference sequence.
	 */
	long[] getFilePointers() {
		return filePointers;
	}

	/**
	 * @return the index of the reference sequence within the BAM header, or -1 if the
	 *         reference sequence is not present in the BAM file
	 */
	int getRefIndex() {
		return refIndex;
	}
}
