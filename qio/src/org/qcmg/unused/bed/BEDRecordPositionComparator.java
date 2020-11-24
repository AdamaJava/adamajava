/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.bed;

import java.util.Comparator;

public class BEDRecordPositionComparator implements
		Comparator<BEDRecord> {
	public int compare(BEDRecord recordA, BEDRecord recordB) {
		return compareStart(recordA, recordB) + compareEnd(recordA, recordB);
	}

	public int compareStart(BEDRecord recordA, BEDRecord recordB) {
		return recordA.getChromStart() - recordB.getChromStart();
	}

	public int compareEnd(BEDRecord recordA, BEDRecord recordB) {
		return recordA.getChromEnd() - recordB.getChromEnd();
	}
}
