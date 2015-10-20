/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.splitread;

import htsjdk.samtools.SAMRecord;

import org.qcmg.qsv.util.QSVUtil;

public class UnmappedRead {
	
	private final String reference;
	private final int bpPos;
	private final String readName;
	private final String sequence;
	private final boolean isTumour;

	public UnmappedRead(SAMRecord record, boolean isTumour) {
		this(record, isTumour, false);
	}
	
	public UnmappedRead(String line, boolean isTumour) {
		String values[] = line.split(",");
		this.readName = values[1];
		this.reference = values[2];
		this.bpPos = Integer.parseInt(values[3]);
		this.sequence =values[4];
		this.isTumour = isTumour;
	}
	
	public UnmappedRead(SAMRecord record, boolean isTumour, boolean isReverse) {
		this.readName = record.getReadName() + ":" + (record.getReadGroup() != null ? record.getReadGroup().getId() : "");
		this.reference = record.getReferenceName();		
		this.bpPos = record.getAlignmentStart();
		this.sequence = isReverse ? QSVUtil.reverseComplement(record.getReadString()) : record.getReadString() ;
		this.isTumour = isTumour;
	}

	public String getReference() {
		return reference;
	}

	public Integer getBpPos() {
		return bpPos;
	}

	public String getReadName() {
		return readName;
	}

	public String getSequence() {
		return sequence;
	}

	public String toTmpString() {
		return "unmapped" + "," + readName + "," + reference + "," + bpPos + "," + sequence +  QSVUtil.getNewLine();		
 	}

	@Override
	public String toString() {
		return readName + "," + reference + "," + bpPos + "," + sequence;
 	}

	public boolean isTumour() {
		return this.isTumour;
	}
}
