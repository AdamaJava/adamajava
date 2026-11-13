/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.assemble;

import gnu.trove.map.hash.TIntIntHashMap;

import java.util.Arrays;

import org.qcmg.common.util.BaseUtils;


public class Read {
	private String header;
	private char[] sequence;
	private final int sequenceLength;
	private final String forwordSeed;
	private final String reverseSeed;
	private final int forwardSeedKey;
	private final int reverseSeedKey;
	/*
	 * hashCode is called for every matching read of every seed during assembly, and is O(read length), so cache it.
	 * Reset whenever the header or sequence changes
	 */
	private int hash;
	private boolean hashSet;
	
	public Read (String header, String tsequence) throws Exception {
		if (header.startsWith(">") || header.startsWith("@")) {
			this.header = header.substring(1); //remove >/@ from header
		} else {
			this.header = header;
		}
		
		sequence = tsequence.toUpperCase().toCharArray();
		sequenceLength = sequence.length;
		
		if (sequenceLength < QSVAssemble.SEED_LENGTH) {			
			throw new Exception("Read sequence length less than SEED_LENGTH: " + sequenceLength + " < " + QSVAssemble.SEED_LENGTH);
		}
		
		forwordSeed = String.copyValueOf(sequence, 0, QSVAssemble.SEED_LENGTH);
		reverseSeed = String.copyValueOf(sequence, sequenceLength - QSVAssemble.SEED_LENGTH, QSVAssemble.SEED_LENGTH);
		forwardSeedKey = seedKey(this.sequence, 0);
		reverseSeedKey = seedKey(this.sequence, sequenceLength - QSVAssemble.SEED_LENGTH);
		
		checkSequence();
	}
	
	public Read (String header, String sequence, boolean isReverse) throws Exception {
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
		forwardSeedKey = seedKey(this.sequence, 0);
		reverseSeedKey = seedKey(this.sequence, sequenceLength - QSVAssemble.SEED_LENGTH);
		
		checkSequence();
	}	
	
	/*
	 * Checks to determine if there are non-supported characters in the read sequence e.g. chars != A,T,G,C,N
	 */
	private void checkSequence() throws Exception {
		for (char c : sequence) {
			if ( ! BaseUtils.isACGTN(c)) {
				throw new Exception("Fileformatting error. Are there incorrectly formated read in the file or have you chosen the correct file format?");
			}
		}
	}
	
	/*
	 * Each base takes 3 bits, so a SEED_LENGTH (10) base seed fits in 30 bits of an int, and distinct seeds always get distinct keys.
	 * Used instead of String seeds to avoid creating a String for every position of every seed read.
	 */
	private static int baseCode(char c) {
		return switch (c) {
			case 'A' -> 0;
			case 'C' -> 1;
			case 'G' -> 2;
			case 'T' -> 3;
			default -> 4;		// only N gets here, as checkSequence only allows ACGTN
		};
	}

	private static int seedKey(char[] seq, int from) {
		int key = 0;
		for (int i = from, end = from + QSVAssemble.SEED_LENGTH; i < end; i++) {
			key = (key << 3) | baseCode(seq[i]);
		}
		return key;
	}

	/*
	 * Map of seed key to the position of that seed in this read. Where a seed occurs more than once, the last position wins.
	 * Missing seeds return -1.
	 */
	public TIntIntHashMap createSeedPositions() {
		int seedCount = sequenceLength - QSVAssemble.SEED_LENGTH + 1;
		TIntIntHashMap seeds = new TIntIntHashMap(seedCount * 2, 0.5f, -1, -1);
		int mask = (1 << (3 * QSVAssemble.SEED_LENGTH)) - 1;
		int key = seedKey(sequence, 0);
		seeds.put(key, 0);
		for (int i = 1; i < seedCount; i++) {
			key = ((key << 3) | baseCode(sequence[i + QSVAssemble.SEED_LENGTH - 1])) & mask;
			seeds.put(key, i);
		}
		return seeds;
	}

	public int getSeedKey() {
		return forwardSeedKey;
	}

	public int getReverseSeedKey() {
		return reverseSeedKey;
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
	
	
	
	/*
	 * Checks to determine if the position given (when adjusted for the read position) is 
	 * within this read or not.
	 */
	public boolean positionWithin(int basePos, int readPos) {
		return readPos <= basePos && basePos < (sequenceLength + readPos);
	}

	/*
	 * Returns the base at a given postion when adjusted for the reads position.
	 */
	public char charAt(int pos, int position) {
		return sequence[pos - position];
	}

	/*
	 * Base at the given (0-based) offset in this read
	 */
	char baseAt(int offset) {
		return sequence[offset];
	}
	public String getSequence() {
		return String.copyValueOf(sequence);
	}
	
	public void setSequence(String newSequence) {
		sequence = newSequence.toCharArray();
		hashSet = false;
	}
	
	public String getHeader() {
		return header;
	}
	
	public void setHeader(String newHeader) {
		header = newHeader;
		hashSet = false;
	}

	/*
	 * Used eclipse to generate this. It is much better than mine was :)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		if ( ! hashSet) {
			hash = computeHashCode();
			hashSet = true;
		}
		return hash;
	}

	private int computeHashCode() {
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
        return Arrays.equals(sequence, other.sequence);
    }
	
}
