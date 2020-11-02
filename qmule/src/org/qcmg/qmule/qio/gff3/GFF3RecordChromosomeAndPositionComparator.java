/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.gff3;

import java.util.Comparator;

import org.qcmg.common.model.ReferenceNameComparator;

public class GFF3RecordChromosomeAndPositionComparator implements
		Comparator<GFF3Record> {
	
	private static final Comparator<String> chrComp = new ReferenceNameComparator();
	
	public int compare(GFF3Record recordA, GFF3Record recordB) {
		
		// first compare chromosome
		int chrcompare = chrComp.compare(recordA.getSeqId(), recordB.getSeqId());
		
		if (chrcompare != 0) return chrcompare;
		
		return compareStart(recordA, recordB) + compareEnd(recordA, recordB);
	}

	public int compareStart(GFF3Record recordA, GFF3Record recordB) {
		return recordA.getStart() - recordB.getStart();
	}

	public int compareEnd(GFF3Record recordA, GFF3Record recordB) {
		return recordA.getEnd() - recordB.getEnd();
	}
}
