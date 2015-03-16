/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.blat;

import org.qcmg.common.util.Constants;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.qsv.util.QSVUtil;

/**
 * Class representing a result returned from BLAT psl file
 * @author felicity
 *
 */
public class BLATRecord implements Comparable<BLATRecord> {

	
	private final String name;
	private final String reference;
	private final char strand;
	private final int match;
	private final int startPos;
	private final int endPos;
	private final int queryStart;
	private final int queryEnd;
	private final int mismatch;
//	private int repMatch;
	private final int tGapCount;
	private final int qGapCount;
	private final boolean valid;
	private final int size;
//	private int tGapBases;
	private final int blockCount;
	private final int[] tStarts;
	private final int[] blockSizes;
	private final int[] qStarts;
//	private String[] tStarts;
//	private String[] blockSizes;
//	private String[] qStarts;
//	private String[] revQStarts;
	private final String recordString;
	private int nonTempBases = 0;


	public BLATRecord(String[] values) {
		final int length = null != values ? values.length : -1;
		this.valid = length >= 21;
		this.match = length > 0 ? Integer.parseInt(values[0]) : -1;
		this.mismatch = length > 1 ? Integer.parseInt(values[1]) : -1;
		this.tGapCount = length > 6 ? Integer.parseInt(values[6]) : -1;
		this.qGapCount = length > 4 ? Integer.parseInt(values[4]) : -1;
		this.name = length > 9 ? values[9] : null;
		this.reference = length > 13 ? values[13] : null;
		this.size = length > 10 ? Integer.parseInt(values[10]) : -1;
		// +1 the value
		this.startPos = length > 15 ? Integer.parseInt(values[15]) + 1 : -1;
		this.endPos= length > 16 ? Integer.parseInt (values[16]) : -1;
		// +1 the value
		this.queryStart = length > 11 ? Integer.parseInt(values[11]) + 1 : -1;
		this.queryEnd = length > 12 ? Integer.parseInt(values[12]) : -1;
		this.strand = length > 8 ? values[8].charAt(0) : Constants.NULL_CHAR;
		this.blockCount = length > 17 ? Integer.parseInt(values[17]) : -1;
		
		this.blockSizes = blockCount > 1 &&  length > 18 ? getIntArrayFromStringArray(values[18].split(Constants.COMMA_STRING)) : null;
		this.qStarts = blockCount > 1 && length > 19 ? getIntArrayFromStringArray(values[19].split(Constants.COMMA_STRING)) : null;		
		this.tStarts = blockCount > 1 && length > 20 ? getIntArrayFromStringArray(values[20].split(Constants.COMMA_STRING)) : null;
		
		StringBuilder b = new StringBuilder();
		for (String s: values) {
			b.append(s).append(Constants.TAB);
		}
		this.recordString = b.toString();
		
		if (qStarts != null) {
			if (strand == QSVUtil.MINUS) {
				for (int i=0; i<qStarts.length; i++) {
					int newInt = size - qStarts[i] - blockSizes[i] + 1;							
					qStarts[i] =  newInt;						
				}
			} else {
				incrementIntArrayByOne(qStarts);
			}
		}
			
		if (tStarts != null) {
			incrementIntArrayByOne(tStarts);
		}
	}
	
	public static int[] getIntArrayFromStringArray(String [] array) {
		if (null != array && array.length > 0) {
			int [] iArr = new int[array.length];
			int i = 0;
			for (String s : array) {
				iArr[i++] = Integer.parseInt(s);
			}
			return iArr;
		}
		return null;
	}
	
	public static void incrementIntArrayByOne(int [] array) {
		if (null != array) {
			for (int i = 0, len = array.length ; i < len ; i++) {
				array[i]++;
			}
		}
	}
	
	public BLATRecord(String line) {
		this(TabTokenizer.tokenize(line));
//		this(line.split("\t"));
	}

//	public int getSize() {
//		return size;
//	}

	/**
	 * Calculate blat score based on web BLAT
	 * @return
	 */
	private int calculateScore() {
		//web blat score: int pslScore(const struct psl *psl) https://lists.soe.ucsc.edu/pipermail/genome/2004-January/003883.html
		/* Return score for psl. */
		//{
		//return psl->match + (psl->repMatch>>1) - psl->misMatch - psl->qNumInsert
		 // - psl->tNumInsert;
		//}
		
		return match - mismatch - tGapCount - qGapCount;		
	}

	public String getName() {
		return name;
	}

	public String getReference() {
		return reference;
	}

	public char getStrand() {
		return strand;
	}

	public int getStartPos() {
		return startPos;
	}

	public int getEndPos() {
		return endPos;
	}

	public int getQueryStart() {
		return queryStart;
	}

	public int getQueryEnd() {
		return queryEnd;
	}
	
	public int getMatch() {
		return match;
	}

	public int getScore() {
		return calculateScore();
	}
	
	public boolean isValid() {
		return valid;
	}
	
	public int getMismatch() {
		return mismatch;
	}

//	public int getRepMatch() {
//		return repMatch;
//	}

	public int gettGapCount() {
		return tGapCount;
	}

	public int getqGapCount() {
		return qGapCount;
	}

//	public int gettGapBases() {
//		return tGapBases;
//	}

	public int getBlockCount() {
		return blockCount;
	}

	public int[] gettStarts() {
		return tStarts;
	}
//	public String[] gettStarts() {
//		return tStarts;
//	}

	public Integer calculateMateBreakpoint(boolean isLeft, String knownReference, Integer knownBreakpoint, char knownStrand) {		
		if (getBlockCount() == 1) {
			
			return calculateSingleMateBreakpoint(isLeft, knownReference,  knownStrand);
		}
		if (getBlockCount() == 2) {
				
			//check current breakpoint matches
			int mateBp = calculateDoubleMateBreakpoint(isLeft, knownReference, knownBreakpoint.intValue(), knownStrand);	
			
			if (mateBp == -1) {
				return calculateSingleMateBreakpoint(isLeft, knownReference,  knownStrand);
			}
			
			return mateBp;
		}
		return null;
	}	

	private int calculateDoubleMateBreakpoint(boolean isLeft, String knownReference, int knownBreakpoint, char knownStrand) {
		
			if (knownStrand == strand && this.reference.equals(knownReference)) {
				for (int i=0; i<2; i++) {
					int currentBp = getCurrentBp(i, isLeft, knownStrand);							
					
					if (currentBp >= knownBreakpoint-5 && (currentBp <=knownBreakpoint+5)) {
						nonTempBases = 0;
						if (i==0) {
							return getMateCurrentBp(1, isLeft, knownStrand);
						} else {
							return getMateCurrentBp(0, isLeft, knownStrand);
						}
					}
				} 
			}
			return -1;
	}


	private int calculateSingleMateBreakpoint(boolean isLeft, String knownReference, char knownStrand) {
		nonTempBases = 0;
		if (isLeft) {
			if (strand == QSVUtil.PLUS) {
				nonTempBases = knownStrand == strand ? (size - queryEnd): startPos;
			} else {
				nonTempBases = knownStrand == strand ? queryStart - 0 : size - queryEnd;
			}
			return  knownStrand == strand ? endPos : startPos;
		} else {
			
			if (strand == QSVUtil.PLUS) {
				nonTempBases = knownStrand == strand ? queryStart: size - queryEnd;
			} else {
				nonTempBases = knownStrand == strand ? size - queryEnd: queryStart;
			}
			return  knownStrand == strand ? startPos: endPos;
		}
	}

	private int getCurrentBp(int i, boolean isLeft, char knownStrand) {
		int startPos = tStarts[i];
		int endPos = tStarts[i] + blockSizes[i];
//		int startPos = Integer.parseInt(tStarts[i]);
//		int endPos = Integer.parseInt(tStarts[i]) + Integer.parseInt(blockSizes[i]);
		
		if (knownStrand == (strand)) {
			return isLeft ? startPos : endPos;
		} else {
			return isLeft ? endPos : startPos;
		}
	}
	
	private int getMateCurrentBp(int i, boolean isLeft, char knownStrand) {
		int startPos = tStarts[i];
		int endPos = tStarts[i] + blockSizes[i];
//		int startPos = Integer.parseInt(tStarts[i]);
//		int endPos = Integer.parseInt(tStarts[i]) + Integer.parseInt(blockSizes[i]) - 1;
		int currentBp;
				
		boolean isStart = true;
		if (knownStrand == strand) {
			if (isLeft) {
				currentBp = endPos;
				isStart = false;
			} else {
				currentBp = startPos;				
			}
		} else {
			if (isLeft) {
				currentBp = startPos;				
			} else {
				currentBp = endPos;		
				isStart = false;
			}
		}		

		if (strand == QSVUtil.PLUS) {
			if (isStart) {
				nonTempBases = queryStart;
			} else {
				nonTempBases = size - queryEnd;
			}
		} else {
			if (isStart) {
				nonTempBases = size - queryEnd;
			} else {
				nonTempBases = queryStart;
			}
		}
		return currentBp;
	}

	@Override
	public int compareTo(BLATRecord o) {
		int nameDiff = this.name.compareTo(o.getName());
		if (nameDiff != 0) {
			return nameDiff;
		} else {
			return Integer.compare(getScore(), o.getScore());
		}
	}

//	public SplitReadAlignment getSplitReadAlignment(int i) {
//		int startPos = Integer.parseInt(tStarts[i]);
//		int endPos = Integer.parseInt(tStarts[i]) + Integer.parseInt(blockSizes[i]) -1;
//		int queryStart = Integer.parseInt(qStarts[i]);
//		int queryEnd = Integer.parseInt(qStarts[i]) + Integer.parseInt(blockSizes[i]) - 1;
//		SplitReadAlignment s = new SplitReadAlignment(reference, strand, startPos, endPos, queryStart, queryEnd);
//		
//		return s;
//	}
	
	@Override 
	public String toString() {
		return getScore() + Constants.TAB + this.recordString;
	}

//	public String getRecordString() {
//		return recordString;
//	}

	public int[] getUnmodifiedStarts() {
			return qStarts;
	}

	public int[] getBlockSizes() {
		return this.blockSizes;
	}
//	public String[] getUnmodifiedStarts() {
//		return qStarts;
//	}
//	
//	public String[] getBlockSizes() {
//		return this.blockSizes;
//	}

	public int getNonTempBases() {
		return this.nonTempBases;
	}

}
