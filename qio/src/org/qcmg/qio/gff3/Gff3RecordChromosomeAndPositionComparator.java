/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qio.gff3;

import java.util.Comparator;

import org.qcmg.common.model.ReferenceNameComparator;

public class Gff3RecordChromosomeAndPositionComparator implements Comparator<Gff3Record> {
	
	private static final Comparator<String> chrComp = new ReferenceNameComparator();
	
	public int compare(Gff3Record recordA, Gff3Record recordB) {
		
		// first compare chromosome
		int chrcompare = chrComp.compare(recordA.getSeqId(), recordB.getSeqId());
		
		if (chrcompare != 0) return chrcompare;
		
		return compareStart(recordA, recordB) + compareEnd(recordA, recordB);
	}

	public int compareStart(Gff3Record recordA, Gff3Record recordB) {
		return recordA.getStart() - recordB.getStart();
	}

	public int compareEnd(Gff3Record recordA, Gff3Record recordB) {
		return recordA.getEnd() - recordB.getEnd();
	}
}
