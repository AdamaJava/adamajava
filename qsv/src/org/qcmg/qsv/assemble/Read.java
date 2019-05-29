/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.assemble;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Arrays;

import org.qcmg.common.util.BaseUtils;


public class Read {
	private String header;
	private char[] sequence;
	private final int sequenceLength;
	private final String forwordSeed;
	private final String reverseSeed;
	private TObjectIntHashMap<String> seeds;
	
	public Read (String header, String tsequence) {
		if (header.startsWith(">") || header.startsWith("@")) {
			this.header = header.substring(1); //remove >/@ from header
		} else {
			this.header = header;
		}
		
		sequence = tsequence.toUpperCase().toCharArray();
		sequenceLength = sequence.length;
		
		if (sequenceLength < QSVAssemble.SEED_LENGTH) {			
			throw new IllegalArgumentException("Read sequence length less than SEED_LENGTH: " + sequenceLength + " < " + QSVAssemble.SEED_LENGTH);
		}
		
		forwordSeed = String.copyValueOf(sequence, 0, QSVAssemble.SEED_LENGTH);
		reverseSeed = String.copyValueOf(sequence, sequenceLength - QSVAssemble.SEED_LENGTH, QSVAssemble.SEED_LENGTH);
		
		checkSequence();
	}
	
	public Read (String header, String sequence, boolean isReverse) {
		int i = header.indexOf(' ');
		if (i > 0) {
			this.header = header.substring(1, i);
		} else {
			this.header = header.substring(1);
		}
		if (isReverse) {
			this.header = this.header + ".R";
		} else {
			this.header = this.header + ".F";
		}
		this.sequence = sequence.toUpperCase().toCharArray();
		sequenceLength = this.sequence.length;
		
		forwordSeed = String.copyValueOf(this.sequence, 0, QSVAssemble.SEED_LENGTH);
		reverseSeed = String.copyValueOf(this.sequence, sequenceLength - QSVAssemble.SEED_LENGTH, QSVAssemble.SEED_LENGTH);
		
		checkSequence();
	}	
	
	/*
	 * Checks to determine if there are non-supported characters in the read sequence e.g. chars != A,T,G,C,N
	 */
	private void checkSequence() {
		for (char c : sequence) {
			if ( ! BaseUtils.isACGTN(c)) {
				throw new IllegalArgumentException("Fileformatting error. Are there incorrectly formated read in the file or have you chosen the correct file format?");
			}
		}
	}
	
	/*
	 * Only creates it when needed for space and time reasons
	 */
	public void createHashtable() {
		seeds = new TObjectIntHashMap<String>((sequenceLength - QSVAssemble.SEED_LENGTH) * 2);	// set initial size to avoid resizing
		for (int i = 0; i <= sequenceLength - QSVAssemble.SEED_LENGTH; i++) {
			seeds.put(String.copyValueOf(sequence, i, QSVAssemble.SEED_LENGTH), i);
		}
	}
	
	public int length() {
		return sequenceLength;
	}
	
	public String getSeed() {
		return forwordSeed;
	}
	
	public String getReverseSeed() {
		return reverseSeed;
	}
	
	
	public TObjectIntHashMap<String> getHashtable() {
		if (seeds == null) createHashtable(); // Create it if it hasn't been created yet.
		return seeds;
	}
	
	/*
	 * Checks to determine if the position given (when adjusted for the read position) is 
	 * within this read or not.
	 */
	public boolean positionWithin(int basePos, int readPos) {
		return readPos <= basePos && basePos < (sequenceLength + readPos);
	}
	public boolean positionWithin(int basePos) {
		return basePos >= 0 && basePos < sequenceLength;
	}
	
	/*
	 * Returns the base at a given postion when adjusted for the reads position.
	 */
	public char charAt(int pos, int position) {
		return sequence[pos - position];
	}
	public String getSequence() {
		return String.copyValueOf(sequence);
	}
	
//	public void setSequence(String newSequence) {
//		sequence = newSequence.toCharArray();
//	}
	
	public String getHeader() {
		return header;
	}
	
	public void setHeader(String newHeader) {
		header = newHeader;
	}
		
//	public void reverse () {
//		for (int i = 0; i < sequence.length; i++) {
//			switch (sequence[i]) {
//				case 'A': 
//					sequence[i] = 'T';
//					break;
//				case 'T': 
//					sequence[i] = 'A';
//					break;
//				case 'C': 
//					sequence[i] = 'G';
//					break;
//				case 'G': 
//					sequence[i] = 'C';
//					break;
//			}
//		}
//	}
	
	/*
	 * Used eclipse to generate this. It is much better than mine was :)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((header == null) ? 0 : header.hashCode());
		result = prime * result + Arrays.hashCode(sequence);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Read other = (Read) obj;
		if (header == null) {
			if (other.header != null)
				return false;
		} else if (!header.equals(other.header))
			return false;
		if (!Arrays.equals(sequence, other.sequence))
			return false;
		return true;
	}
	
}
