/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import net.sf.samtools.SAMRecord;

interface Algorithm {
	public String getName();
	public void applyTo(final SAMRecord read, Object coverageCounter);
	public void applyTo(final SAMRecord read, Object coverageCounter, boolean fullyPopulated);
//	public void applyTo(final SAMRecord read, final int[] perBaseCoverages);
//	public void applyTo(final SAMRecord read, final int[] perBaseCoverages, boolean fullyPopulated);
}