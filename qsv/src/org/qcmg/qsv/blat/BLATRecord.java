/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.blat;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.qcmg.common.util.Constants;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.qsv.util.QSVUtil;

/**
 * Class representing a result returned from BLAT psl file
 * @author felicity
 *
 */
public class BLATRecord implements Comparable<BLATRecord> {

	
	private final String[] rawData;
	private String name;
	private final String reference;
	private final int score;
	private final int size;
	private final char strand;
	private final boolean valid;
	private final int blockCount;
	private final int[] tStarts;
	private final int[] blockSizes;
	private final int[] unmodifiedStarts;
	
	private int nonTempBases = 0;
	

	public BLATRecord(String[] values) {
		this.rawData = values;
		this.valid = rawData.length >= 21;
				
		this.score = Integer.parseInt(rawData[0]) - Integer.parseInt(rawData[1]) - Integer.parseInt(rawData[6]) - Integer.parseInt(rawData[4]);
		this.name = rawData[9];
		this.reference = rawData[13];
		this.blockCount = Integer.parseInt(rawData[17]);
		this.size =  Integer.parseInt(rawData[10]);
		this.strand =   rawData[8].charAt(0);
		
		/*
		 * Setup tStarts array if we have sufficient blockCountage
		 */
		if (blockCount > 1) {
			/*
			 * Setup tStarts
			 */
			String [] tStartsS = rawData[20].split(",");
			if (tStartsS != null) {
				tStarts = new int[tStartsS.length];
				for (int i=0; i<tStartsS.length; i++) {						
					tStarts[i] =Integer.parseInt(tStartsS[i]) + 1;	
				}
			} else {
				tStarts = null;
			}
			/*
			 * Setup block sizes
			 */
			String [] blockSizesS = rawData[18].split(",");
			if (blockSizesS != null) {
				blockSizes = new int[blockSizesS.length];
				for (int i=0; i<blockSizesS.length; i++) {						
					blockSizes[i] =Integer.parseInt(blockSizesS[i]);
				}
			} else {
				blockSizes = null;
			}
			
			/*
			 * setup unmodifedStarts
			 */
			String[] qStarts = rawData[19].split(",");
			if (qStarts != null) {
				unmodifiedStarts = new int[qStarts.length];
				for (int i=0; i<qStarts.length; i++) {
					if (strand == QSVUtil.MINUS) {
						unmodifiedStarts[i] =  getSize() - Integer.parseInt(qStarts[i]) - blockSizes[i] + 1;
					} else {
						unmodifiedStarts[i] =  Integer.parseInt(qStarts[i]) + 1;
					}
				}
			} else {
				unmodifiedStarts = null;
			}
			
		} else {
			tStarts = null;
			blockSizes = null;
			unmodifiedStarts = null;
		}
	}
	
	public BLATRecord(String line) {
		this(TabTokenizer.tokenize(line));
	}

	public int getSize() {
		return size;
	}
	
	public int getMatchCount() {
		return Integer.parseInt(rawData[0]);
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
		this.rawData[9] = name;
	}

	public String getReference() {
		return reference;
	}

	public char getStrand() {
		return strand;
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
	
	public int getScore() {
		return score;
	}
	
	public boolean isValid() {
		return valid;
	}
	
	public int getBlockCount() {
		return blockCount;
	}

	public int[] gettStarts() {
		return tStarts;
	}

	public Integer calculateMateBreakpoint(boolean isLeft, String knownReference, Integer knownBreakpoint, char knownStrand) {		
		if (getBlockCount() == 1) {			
			int mateBp = calculateSingleMateBreakpoint(isLeft, knownStrand);
			
			return Integer.valueOf(mateBp);
		} else {
			if (getBlockCount() == 2) {
				
				//check current breakpoint matches
				Integer mateBp = calculateDoubleMateBreakpoint(isLeft, knownReference, knownBreakpoint, knownStrand);	
				
				if (mateBp == null) {
					return Integer.valueOf(calculateSingleMateBreakpoint(isLeft, knownStrand));
				}
				
				return mateBp;
			}
		}
		return null;
	}	

	private Integer calculateDoubleMateBreakpoint(boolean isLeft, String knownReference, Integer knownBreakpoint, char knownStrand) {
		if (this.reference.equals(knownReference)) {
			if (knownStrand == getStrand()) {
				
				for (int i = 0; i < 2; i++) {
					int currentBp = getCurrentBp(tStarts[i], blockSizes[i], isLeft, knownStrand == getStrand());				
					
					if (currentBp >= knownBreakpoint.intValue() - 5 && (currentBp <= knownBreakpoint.intValue() + 5)) {
						nonTempBases = 0;
						if (i == 0) {
							return getMateCurrentBp(1, isLeft, knownStrand);
						} else {
							return getMateCurrentBp(0, isLeft, knownStrand);
						}
					}
				}
			}
		}
		return null;
	}

	private int calculateSingleMateBreakpoint(boolean isLeft, char knownStrand) {
		int startPos = getStartPos();
		int endPos = getEndPos();
		int queryStart = getQueryStart();
		int queryEnd = getQueryEnd();
		int mateBp;
		nonTempBases = 0;
		if (isLeft) {
			mateBp =  knownStrand == strand ? endPos : startPos;
			
			if (strand == QSVUtil.PLUS) {
				nonTempBases = knownStrand == strand ? (size - queryEnd): startPos;
			} else {
				nonTempBases = knownStrand == strand ? queryStart : size - queryEnd;
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

	public static int getCurrentBp(int start, int blockSize, boolean isLeft, boolean sameStrand) {
		if (sameStrand) {
			return isLeft ?  start : ( start + blockSize);
		} else {
			return isLeft ? ( start + blockSize) : start;
		}
	}
	
	private Integer getMateCurrentBp(int i, boolean isLeft, char knownStrand) {
		int startPos = tStarts[i];
		int endPos = startPos + blockSizes[i] - 1;
		int currentBp;
		int queryStart = getQueryStart();
		int queryEnd = getQueryEnd();
				
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
		int diff = this.name.compareTo(o.getName());
		if (diff != 0) {
			return diff;
		} else {
			diff = Integer.compare(getScore(), o.getScore());
			
			if (diff != 0) {
				return diff;
			}
			
			diff = o.rawData[0].compareTo(rawData[0]);
//			diff = Integer.compare(Integer.parseInt(o.rawData[0]), Integer.parseInt(rawData[0]));
			return diff;
		}
	}

	@Override 
	public String toString() {
//		StringBuilder b = new StringBuilder(Constants.TAB_STRING);
//		for (String s: rawData) {
//			b.append(s).append(Constants.TAB);
//		}
		return getScore() + Constants.TAB_STRING + Arrays.stream(rawData).collect(Collectors.joining(Constants.TAB_STRING));
	}

	public int[] getUnmodifiedStarts() {
		return unmodifiedStarts;
	}

	public int[] getBlockSizes() {
		return blockSizes;
	}

	public int getNonTempBases() {
		return this.nonTempBases;
	}
}
