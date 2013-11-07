package org.qcmg.gff3;

import java.util.Comparator;

public class GFF3RecordPositionComparator implements
		Comparator<GFF3Record> {
	public int compare(GFF3Record recordA, GFF3Record recordB) {
		return compareStart(recordA, recordB) + compareEnd(recordA, recordB);
	}

	public int compareStart(GFF3Record recordA, GFF3Record recordB) {
		return recordA.getStart() - recordB.getStart();
	}

	public int compareEnd(GFF3Record recordA, GFF3Record recordB) {
		return recordA.getEnd() - recordB.getEnd();
	}
}
