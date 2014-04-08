/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.assemble;


public class ConsensusRead extends Read {
	
	String clipMateSequence;
	String referenceSequence;

	public ConsensusRead(String header, String tsequence, String clipString, String referenceSequence) throws Exception {
		super(header, tsequence);	
		this.clipMateSequence = clipString;
		this.referenceSequence = referenceSequence;
	}
	
	public String getClipMateSequence() {
		return clipMateSequence;
	}

	public void setClipMateSequence(String clipMateSequence) {
		this.clipMateSequence = clipMateSequence;
	}
	
	public int getClipMateSequenceLength() {
		return clipMateSequence.length();
	}

	public String getReferenceSequence() {
		return referenceSequence;
	}

	public void setReferenceSequence(String referenceSequence) {
		this.referenceSequence = referenceSequence;
	}
	
	public int getReferenceSequenceLength() {
		return this.referenceSequence.length();
	}
	
	@Override
	public String toString() {
		String s = "";
		s += (">" + this.getHeader() + "\n");
		s += ("FULL:"+ this.getSequence() + "\n");
		s += ("CLIPS:"+ this.getClipMateSequence() + "\n");
		s += ("READ:"+ this.getReferenceSequence() + "\n");
		return s;
	}

	public int getHeaderCount() {
		
		if (getHeader().split(",") != null) {
			return getHeader().split(",").length;
		}
		return 0;		
	}
}
