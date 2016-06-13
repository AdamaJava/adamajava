/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.blat;

import org.qcmg.common.util.Constants;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.util.QSVUtil;

/**
 * Class representing a result returned from BLAT psl file
 * @author felicity
 *
 */
public class BLATRecord implements Comparable<BLATRecord> {

	
	private final String[] rawData;
	private final String name;
	private final String reference;
//	private final char strand;
	
	private final int score;
	
//	private int match;
//	private final int startPos;
//	private final int endPos;
//	private final int queryStart;
//	private final int queryEnd;
//	private int mismatch;
//	private int tGapCount;
//	private int qGapCount;
	private final boolean valid;
//	private final int size;
//	private final int blockCount;
//	private String[] tStarts;
//	private String[] blockSizes;
//	private String[] qStarts;
//	private final String recordString = "";
	private int nonTempBases = 0;
	

	public BLATRecord(String[] values) throws QSVException {
		this.rawData = values;
		this.valid = rawData.length >= 21;
				
		this.score = Integer.parseInt(rawData[0]) - Integer.parseInt(rawData[1]) - Integer.parseInt(rawData[6]) - Integer.parseInt(rawData[4]);
		this.name = rawData[9];
		this.reference = rawData[13];
		
//		this.size = Integer.parseInt(rawData[10]);
//		this.startPos = Integer.parseInt(rawData[15]) + 1;
//		this.endPos= Integer.parseInt (rawData[16]);
//		this.queryStart = Integer.parseInt(rawData[11]) + 1;
//		this.queryEnd = Integer.parseInt(rawData[12]);
//		this.strand = rawData[8].charAt(0);
//		this.blockCount = Integer.parseInt(rawData[17]);
//		if (blockCount > 1) {				
//			this.blockSizes = rawData[18].split(",");
//			this.qStarts = rawData[19].split(",");
//			this.tStarts = rawData[20].split(",");
			
			
//			if (tStarts != null) {
//				for (int i=0; i<tStarts.length; i++) {						
//					int newInt = Integer.parseInt(tStarts[i]) + 1;						
//					tStarts[i] = newInt + "";
//				}
//			}
//		}
	}
	
	public BLATRecord(String line) throws QSVException {
		this(TabTokenizer.tokenize(line));
//		this(line.split("\t"));
	}


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
		
		return score;
//		return match - mismatch - tGapCount - qGapCount;		
	}
	
	public int getSize() {
		return Integer.parseInt(rawData[10]);
	}

	public String getName() {
		return name;
	}

	public String getReference() {
		return reference;
	}

	public char getStrand() {
		return rawData[8].charAt(0);
	}

	public int getStartPos() {
		return Integer.parseInt(rawData[15]) + 1;
	}

	public int getEndPos() {
		return Integer.parseInt (rawData[16]);
	}

	public int getQueryStart() {
		return Integer.parseInt(rawData[11]) + 1;
	}

	public int getQueryEnd() {
		return Integer.parseInt(rawData[12]);
	}
	
//	public int getMatch() {
//		return match;
//	}

	public int getScore() {
		return score;
	}
	
	public boolean isValid() {
		return valid;
	}
	
//	public int getMismatch() {
//		return mismatch;
//	}

//	public int gettGapCount() {
//		return tGapCount;
//	}

//	public int getqGapCount() {
//		return qGapCount;
//	}

	public int getBlockCount() {
		return Integer.parseInt(rawData[17]);
	}

	public String[] gettStarts() {
		if (getBlockCount() > 1) {
			String [] tStarts = rawData[20].split(",");
			if (tStarts != null) {
				for (int i=0; i<tStarts.length; i++) {						
					int newInt = Integer.parseInt(tStarts[i]) + 1;						
					tStarts[i] = newInt + "";
				}
			}
			
			return tStarts;
		} 
		return null;
	}

	public Integer calculateMateBreakpoint(boolean isLeft, String knownReference, Integer knownBreakpoint, char knownStrand) {		
		if (getBlockCount() == 1) {			
			int mateBp = calculateSingleMateBreakpoint(isLeft, knownReference, knownBreakpoint, knownStrand);
			
			return Integer.valueOf(mateBp);
		} else {
			if (getBlockCount() == 2) {
				
				//check current breakpoint matches
				Integer mateBp = calculateDoubleMateBreakpoint(isLeft, knownReference, knownBreakpoint, knownStrand);	
				
				if (mateBp == null) {
					return Integer.valueOf(calculateSingleMateBreakpoint(isLeft, knownReference, knownBreakpoint, knownStrand));
				}
				
				return mateBp;
			}
		}
		return null;
	}	

	private Integer calculateDoubleMateBreakpoint(boolean isLeft,
			String knownReference, Integer knownBreakpoint, char knownStrand) {
			if (this.reference.equals(knownReference)) {			
				if (knownStrand == getStrand()) {
					
					for (int i=0; i<2; i++) {
						int currentBp = getCurrentBp(i, isLeft, knownStrand, knownBreakpoint);							
						
						if (currentBp >= knownBreakpoint.intValue() - 5 & (currentBp <= knownBreakpoint.intValue() + 5)) {
							nonTempBases = 0;
							if (i==0) {
								return getMateCurrentBp(1, isLeft, knownStrand, knownBreakpoint);
							} else {
								return getMateCurrentBp(0, isLeft, knownStrand, knownBreakpoint);
							}
						}
					} 
				}
			}
			return null;
	}


	private int calculateSingleMateBreakpoint(boolean isLeft,
			String knownReference, Integer knownBreakpoint, char knownStrand) {
		int startPos = getStartPos();
		int endPos = getEndPos();
		int queryStart = getQueryStart();
		int queryEnd = getQueryEnd();
		int size = getSize();
		char strand = getStrand();
		int mateBp;
		nonTempBases = 0;
		if (isLeft) {
			mateBp =  knownStrand == strand ? endPos : startPos;
			
			if (strand == QSVUtil.PLUS) {
				nonTempBases = knownStrand == strand ? (size - queryEnd): startPos;
			} else {
				nonTempBases = knownStrand == strand ? queryStart - 0 : size - queryEnd;
			}
			
		} else {
			mateBp =  knownStrand == strand ? startPos: endPos;
			
			if (strand == QSVUtil.PLUS) {
				nonTempBases = knownStrand == strand ? queryStart: size - queryEnd;
			} else {
				nonTempBases = knownStrand == strand ? size - queryEnd: queryStart;
			}
		}
		
		return mateBp;	
	}

	private int getCurrentBp(int i, boolean isLeft, char knownStrand, Integer knownBreakpoint) {
		String [] tStarts = gettStarts();
		int startPos = Integer.parseInt(tStarts[i]);
		int endPos = Integer.parseInt(tStarts[i]) + Integer.parseInt(getBlockSizes()[i]);
		char strand = getStrand();
		
		if (knownStrand == (strand)) {
			return isLeft ?  startPos : endPos;
		} else {
			return isLeft ? endPos : startPos;
		}
	}
	
	private Integer getMateCurrentBp(int i, boolean isLeft, char knownStrand, Integer knownBreakpoint) {
		String [] tStarts = gettStarts();
		int size = getSize();
		int startPos = Integer.parseInt(tStarts[i]);
		int endPos = Integer.parseInt(tStarts[i]) + Integer.parseInt(getBlockSizes()[i]) - 1;
		int currentBp;
		int queryStart = getQueryStart();
		int queryEnd = getQueryEnd();
		char strand = getStrand();
				
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
		return Integer.valueOf(currentBp);
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

	@Override 
	public String toString() {
		StringBuilder b = new StringBuilder();
		for (String s: rawData) {
			b.append(s).append(Constants.TAB);
		}
		return getScore() + Constants.TAB + b.toString();
	}

	public String[] getUnmodifiedStarts() {
		if (getBlockCount() > 1) {
			String[] qStarts = rawData[19].split(",");
			String [] blockSizes = getBlockSizes();
			if (qStarts != null) {
				char strand = getStrand();
				for (int i=0; i<qStarts.length; i++) {
					if (strand == QSVUtil.MINUS) {
						int newInt = getSize() - Integer.parseInt(qStarts[i]) - Integer.parseInt(blockSizes[i]) + 1;							
						qStarts[i] =  newInt + "";						
					} else {
						int newInt = Integer.parseInt(qStarts[i]) + 1;						
						qStarts[i] = newInt + "";
					}
				}
			}
			
			return qStarts;
		} 
		return null;
	}

	public String[] getBlockSizes() {
		if (getBlockCount() > 1) {
			return rawData[18].split(",");
		}
		return null;
	}

	public int getNonTempBases() {
		return this.nonTempBases;
	}

}
