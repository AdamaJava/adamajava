/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

/**
 * Thrown when a BAM record contains a feature that the fast BAM reader does not support.
 * <p>
 * When this exception is thrown the records have to be processed using htsjdk's BAM
 * reader, as the fast reader is not able to calculate the same coverage.
 */
class UnsupportedBamFeatureException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	UnsupportedBamFeatureException(final String message) {
		super(message);
	}
}
