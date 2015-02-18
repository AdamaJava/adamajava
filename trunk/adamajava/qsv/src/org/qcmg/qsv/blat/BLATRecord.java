/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.blat;

import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.splitread.SplitReadAlignment;
import org.qcmg.qsv.util.QSVUtil;

/**
 * Class representing a result returned from BLAT psl file
 * @author felicity
 *
 */
public class BLATRecord implements Comparable<BLATRecord> {

	
	private String name;
	private String reference;
	private char strand;
	private int match;
	private int startPos;
	private int endPos;
	private int queryStart;
	private int queryEnd;
	private int mismatch;
	private int repMatch;
	private int tGapCount;
	private int qGapCount;
	private boolean valid;
	private int size;
	private int tGapBases;
	private int blockCount;
	private String[] tStarts;
	private String[] blockSizes;
	private String[] qStarts;
	private String[] revQStarts;
	private String recordString = "";
	private int nonTempBases = 0;


	public BLATRecord(String[] values) throws QSVException {
		if (values.length < 21) {
			this.valid = false;
		} else {
			try {

				this.valid = true;
				this.match = Integer.parseInt(values[0]);
				this.mismatch = Integer.parseInt(values[1]);
				this.repMatch = Integer.parseInt(values[2]);
				this.tGapCount = Integer.parseInt(values[6]);
				this.qGapCount = Integer.parseInt(values[4]);
				this.name = values[9];
				this.reference = values[13];
				this.size = Integer.parseInt(values[10]);
				this.startPos = Integer.parseInt(values[15]);
				this.endPos= Integer.parseInt (values[16]);
				this.queryStart = Integer.parseInt(values[11]);
				this.queryEnd = Integer.parseInt(values[12]);
				this.strand = values[8].charAt(0);
				this.tGapBases = Integer.parseInt(values[5]);
				this.blockCount = Integer.parseInt(values[17]);
				if (blockCount > 1) {				
					this.blockSizes = values[18].split(",");
					this.qStarts = values[19].split(",");				
					this.tStarts = values[20].split(",");
					if (strand == QSVUtil.MINUS) {
						this.revQStarts = values[19].split(",");			
					}
				}
			} catch (Exception e) {
				this.valid = false;
			}
			StringBuilder b = new StringBuilder();
			for (String s: values) {
				b.append(s).append("\t");
			}
			this.recordString = b.toString();
				//fix start positions
				startPos++;
				queryStart++;
				if (qStarts != null) {
					for (int i=0; i<qStarts.length; i++) {
						if (strand == QSVUtil.MINUS) {
							int newInt = size - Integer.parseInt(qStarts[i]) - Integer.parseInt(blockSizes[i]) + 1;							
							qStarts[i] =  newInt + "";						
						} else {
							int newInt = Integer.parseInt(qStarts[i]) + 1;						
							qStarts[i] = newInt + "";
							if (strand == QSVUtil.MINUS) {
								revQStarts[i] = newInt + "";
							}
						}
					}
				}
				if (tStarts != null) {
					for (int i=0; i<tStarts.length; i++) {						
						int newInt = Integer.parseInt(tStarts[i]) + 1;						
						tStarts[i] = newInt + "";
					}
				}
		}
	}
	
	public BLATRecord(String line) throws QSVException {
		this(line.split("\t"));
	}

	public int getSize() {
		return size;
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

	public int getRepMatch() {
		return repMatch;
	}

	public int gettGapCount() {
		return tGapCount;
	}

	public int getqGapCount() {
		return qGapCount;
	}

	public int gettGapBases() {
		return tGapBases;
	}

	public int getBlockCount() {
		return blockCount;
	}

	public String[] gettStarts() {
		return tStarts;
	}

	public Integer calculateMateBreakpoint(boolean isLeft, String knownReference, Integer knownBreakpoint, char knownStrand) {		
		if (getBlockCount() == 1) {			
			Integer mateBp = calculateSingleMateBreakpoint(isLeft, knownReference, knownBreakpoint, knownStrand);
			
			return mateBp;
		} else {
			if (getBlockCount() == 2) {
				
				//check current breakpoint matches
				Integer mateBp = calculateDoubleMateBreakpoint(isLeft, knownReference, knownBreakpoint, knownStrand);	
				
				if (mateBp == null) {
					return calculateSingleMateBreakpoint(isLeft, knownReference, knownBreakpoint, knownStrand);
				}
				
				return mateBp;
			}
		}
		return null;
	}	

	private Integer calculateDoubleMateBreakpoint(boolean isLeft,
			String knownReference, Integer knownBreakpoint, char knownStrand) {
			if (this.reference.equals(knownReference)) {			
				if (knownStrand == strand) {
					
					for (int i=0; i<2; i++) {
						Integer currentBp = getCurrentBp(i, isLeft, knownStrand, knownBreakpoint);							
						
						if (currentBp >= knownBreakpoint-5 &(currentBp <=knownBreakpoint+5)) {
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


	private Integer calculateSingleMateBreakpoint(boolean isLeft,
			String knownReference, Integer knownBreakpoint, char knownStrand) {
		Integer mateBp = null;
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

	private Integer getCurrentBp(int i, boolean isLeft, char knownStrand, Integer knownBreakpoint) {
		int startPos = Integer.parseInt(tStarts[i]);
		int endPos = Integer.parseInt(tStarts[i]) + Integer.parseInt(blockSizes[i]);
		Integer currentBp = null;
		int buffer = 0;
		if (strand == QSVUtil.MINUS) {
			buffer = 0;
		}
		
		if (knownStrand == (strand)) {
			if (isLeft) {
				currentBp = startPos + buffer;
			} else {
				currentBp = endPos - buffer;	
			}
		} else {
			if (isLeft) {
				currentBp = endPos - buffer;
			} else {
				currentBp = startPos + buffer;	
			}
		}

		return currentBp;
	}
	
	private Integer getMateCurrentBp(int i, boolean isLeft, char knownStrand, Integer knownBreakpoint) {
		int startPos = Integer.parseInt(tStarts[i]);
		int endPos = Integer.parseInt(tStarts[i]) + Integer.parseInt(blockSizes[i]) - 1;
		Integer currentBp = null;
				
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

	public SplitReadAlignment getSplitReadAlignment(int i) {
		int startPos = Integer.parseInt(tStarts[i]);
		int endPos = Integer.parseInt(tStarts[i]) + Integer.parseInt(blockSizes[i]) -1;
		int queryStart = Integer.parseInt(qStarts[i]);
		int queryEnd = Integer.parseInt(qStarts[i]) + Integer.parseInt(blockSizes[i]) - 1;
		SplitReadAlignment s = new SplitReadAlignment(reference, strand, startPos, endPos, queryStart, queryEnd);
		
		return s;
	}
	
	@Override 
	public String toString() {
		return getScore() + "\t" + this.recordString;
	}

	public String getRecordString() {
		return recordString;
	}

	public String[] getUnmodifiedStarts() {
			return qStarts;
	}

	public String[] getBlockSizes() {
		return this.blockSizes;
	}

	public int getNonTempBases() {
		return this.nonTempBases;
	}

}
