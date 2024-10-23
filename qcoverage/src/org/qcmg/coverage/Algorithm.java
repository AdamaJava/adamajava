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
	void applyTo(final SAMRecord read, Object coverageCounter);
	void applyTo(final SAMRecord read, Object coverageCounter, boolean fullyPopulated);
}
