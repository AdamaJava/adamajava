/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard.util;

import java.util.Comparator;

import htsjdk.samtools.SAMSequenceRecord;

public class SAMSequenceRecodComparator implements Comparator<SAMSequenceRecord> {

	@Override
	public int compare(SAMSequenceRecord o1, SAMSequenceRecord o2) {
		// all we care about is sequence length
		return o2.getSequenceLength() - o1.getSequenceLength();
	}

}
