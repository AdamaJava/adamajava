/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.splitread;

import org.qcmg.qsv.util.QSVUtil;

import htsjdk.samtools.SAMRecord;

public class UnmappedRead {
	
	private final String reference;
	private final int bpPos;
	private final String readName;
	private final String sequence;
	private final boolean isTumour;

	public UnmappedRead(SAMRecord record, String readGroupId, boolean isTumour) {
		this(record, readGroupId, isTumour, false);
	}
	
	public UnmappedRead(String line, boolean isTumour) {
		String [] values = line.split(",");
		this.readName = values[1];
		this.reference = values[2];
		this.bpPos = Integer.parseInt(values[3]);
		this.sequence =values[4];
		this.isTumour = isTumour;
	}
	
	public UnmappedRead(SAMRecord record, String readGroupId, boolean isTumour, boolean isReverse) {
		this.readName = record.getReadName() + ":" + (readGroupId != null ? readGroupId : "");
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

	@Override
	public String toString() {
		return readName + "," + reference + "," + bpPos + "," + sequence;
 	}

	public boolean isTumour() {
		return this.isTumour;
	}
}
