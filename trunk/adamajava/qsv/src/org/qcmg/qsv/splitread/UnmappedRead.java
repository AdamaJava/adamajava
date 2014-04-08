/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.splitread;

import org.qcmg.qsv.util.QSVUtil;

import net.sf.samtools.SAMRecord;

public class UnmappedRead {
	
	private String reference;
	private Integer bpPos;
	private int length;
	private String readName;
	private String sequence;
	private Integer start;
	private Integer end;
	private String key;
	private boolean readUnmapped;
	private boolean isTumour;

	public UnmappedRead(SAMRecord record, boolean isTumour) {
		this.readName = record.getReadName() + ":";
		if (record.getReadGroup() != null) {
			this.readName += record.getReadGroup().getId();
		} 
		
		this.reference = record.getReferenceName();		
		this.bpPos = record.getAlignmentStart();
		this.sequence = record.getReadString();
		this.length = sequence.length();
		
		if (record.getReadUnmappedFlag()) {
			this.readUnmapped = true;
		}
	}
	
	public UnmappedRead(String line, boolean isTumour) {
		String values[] = line.split(",");
		this.readName = values[1];
		this.reference = values[2];
		this.bpPos = new Integer(values[3]);
		this.sequence =values[4];
		this.length = sequence.length();
		this.readUnmapped = true;
		this.isTumour = isTumour;
	}
	
	public UnmappedRead(SAMRecord record, boolean isTumour, boolean isReverse) {
		this.readName = record.getReadName() + ":";
		if (record.getReadGroup() != null) {
			this.readName += record.getReadGroup().getId();
		} 
		
		this.reference = record.getReferenceName();		
		this.bpPos = record.getAlignmentStart();
		
		if (isReverse) {
			this.sequence = QSVUtil.reverseComplement(record.getReadString());
		} else {
			this.sequence = record.getReadString();
		}
		this.length = sequence.length();
		
		if (record.getReadUnmappedFlag()) {
			this.readUnmapped = true;
		}
	}

	public boolean isReadUnmapped() {
		return readUnmapped;
	}

	public void setReadUnmapped(boolean readUnmapped) {
		this.readUnmapped = readUnmapped;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public Integer getBpPos() {
		return bpPos;
	}

	public void setBpPos(Integer bpPos) {
		this.bpPos = bpPos;
	}	

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public String getReadName() {
		return readName;
	}

	public void setReadName(String readName) {
		this.readName = readName;
	}

	public String getSequence() {
		return sequence;
	}

	public void setSequence(String sequence) {
		this.sequence = sequence;
	}

	public Integer getStart() {
		return start;
	}

	public void setStart(Integer start) {
		this.start = start;
	}

	public Integer getEnd() {
		return end;
	}

	public void setEnd(Integer end) {
		this.end = end;
	}
	
	public String getAlignedSequence() {
			return (sequence.substring(start, end));
	}
	
	public String toTmpString() {
		return "unmapped" + "," + readName + "," + reference + "," + bpPos + "," + sequence +  QSVUtil.getNewLine();		
 	}

	@Override
	public String toString() {
		return readName + "," + reference + "," + bpPos + "," + sequence;
 	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public boolean isTumour() {
		return this.isTumour;
	}
}
