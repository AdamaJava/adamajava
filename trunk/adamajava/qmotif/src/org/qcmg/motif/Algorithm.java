/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.motif;

import java.util.Map;

import htsjdk.samtools.SAMRecord;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.motif.util.RegionCounter;

interface Algorithm {
	public String getName();
	public boolean applyTo(final SAMRecord read, Map<ChrPosition, RegionCounter> motifRefPositions);
}
