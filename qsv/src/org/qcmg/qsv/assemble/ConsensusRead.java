/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.assemble;


public class ConsensusRead extends Read {
	
	final String clipMateSequence;
	final String referenceSequence;

	public ConsensusRead(String header, String tsequence, String clipString, String referenceSequence) throws Exception {
		super(header, tsequence);	
		this.clipMateSequence = clipString;
		this.referenceSequence = referenceSequence;
	}
	
	public String getClipMateSequence() {
		return clipMateSequence;
	}

	public int getClipMateSequenceLength() {
		return clipMateSequence.length();
	}

	public String getReferenceSequence() {
		return referenceSequence;
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
}
