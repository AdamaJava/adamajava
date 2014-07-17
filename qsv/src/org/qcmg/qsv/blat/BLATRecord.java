/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.blat;

import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.splitread.SplitReadAlignment;

/**
 * Class representing a result returned from BLAT psl file
 * @author felicity
 *
 */
public class BLATRecord implements Comparable<BLATRecord> {

	
	private String name;
	private String reference;
	private String strand;
	private Integer match;
	private Integer startPos;
	private Integer endPos;
	private Integer queryStart;
	private Integer queryEnd;
	private int score;
	private Integer mismatch;
	private Integer repMatch;
	private Integer tGapCount;
	private Integer qGapCount;
	private boolean valid;
	private Integer size;
	private Integer tGapBases;
	private Integer blockCount;
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
			this.match = new Integer(values[0]);
			this.mismatch = new Integer(values[1]);
			this.repMatch = new Integer(values[2]);
			this.tGapCount = new Integer(values[6]);
			this.qGapCount = new Integer(values[4]);
			this.name = values[9];
			this.reference = values[13];
			this.size = new Integer(values[10]);
			this.startPos = new Integer(values[15]);
			this.endPos= new Integer (values[16]);
			this.queryStart = new Integer(values[11]);
			this.queryEnd = new Integer(values[12]);
			this.strand = values[8];
			this.tGapBases = new Integer(values[5]);
			this.blockCount = new Integer(values[17]);
			if (blockCount > 1) {				
				this.blockSizes = values[18].split(",");
				this.qStarts = values[19].split(",");				
				this.tStarts = values[20].split(",");
				if (strand.equals("-")) {
					this.revQStarts = values[19].split(",");			
				}
			}
			this.score = calculateScore();
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
						if (strand.equals("-")) {
							Integer newInt = size - new Integer(qStarts[i]).intValue() - new Integer(blockSizes[i]) + 1;							
							qStarts[i] = newInt.toString();						
						} else {
							Integer newInt = new Integer(qStarts[i]).intValue() + 1;						
							qStarts[i] = newInt.toString();
							if (strand.equals("-")) {
								revQStarts[i] = newInt.toString(); 
							}
						}
					}
				}
				if (tStarts != null) {
					for (int i=0; i<tStarts.length; i++) {						
						Integer newInt = new Integer(tStarts[i]).intValue() + 1;						
						tStarts[i] = newInt.toString();
					}
				}
		}
	}
	
	public BLATRecord(String line) throws QSVException {
		this(line.split("\t"));
	}

	public Integer getSize() {
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

	public int score() {
		return this.score;
	}
	
	public String getName() {
		return name;
	}

	public String getReference() {
		return reference;
	}

	public String getStrand() {
		return strand;
	}

	public Integer getStartPos() {
		return startPos;
	}

	public Integer getEndPos() {
		return endPos;
	}

	public Integer getQueryStart() {
		return queryStart;
	}

	public Integer getQueryEnd() {
		return queryEnd;
	}
	
	public Integer getMatch() {
		return match;
	}

	public int getScore() {
		return this.score;
	}
	
	public boolean isValid() {
		return valid;
	}
	
	public Integer getMismatch() {
		return mismatch;
	}

	public Integer getRepMatch() {
		return repMatch;
	}

	public Integer gettGapCount() {
		return tGapCount;
	}

	public Integer getqGapCount() {
		return qGapCount;
	}

	public Integer gettGapBases() {
		return tGapBases;
	}

	public int getBlockCount() {
		return blockCount.intValue();
	}

	public String[] gettStarts() {
		return tStarts;
	}

	public Integer calculateMateBreakpoint(boolean isLeft, String knownReference, Integer knownBreakpoint, String knownStrand) {		
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
			String knownReference, Integer knownBreakpoint, String knownStrand) {
			if (this.reference.equals(knownReference)) {			
				if (knownStrand.equals(strand)) {
					
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
					
				} else {
					
				}
			}
			return null;
	}


	private Integer calculateSingleMateBreakpoint(boolean isLeft,
			String knownReference, Integer knownBreakpoint, String knownStrand) {
		Integer mateBp = null;
		nonTempBases = 0;
		if (isLeft) {					
			mateBp =  knownStrand.equals (strand) ? endPos : startPos;
			
			if (strand.equals("+")) {
				nonTempBases = knownStrand.equals(strand) ? (size - queryEnd): startPos;
			} else {
				nonTempBases = knownStrand.equals(strand) ? queryStart - 0 : size - queryEnd;
			}
			
		} else {			
			mateBp =  knownStrand.equals(strand) ? startPos: endPos;
			
			if (strand.equals("+")) {
				nonTempBases = knownStrand.equals(strand) ? queryStart: size - queryEnd;
			} else {
				nonTempBases = knownStrand.equals(strand) ? size - queryEnd: queryStart;
			}			
		}		 
		
		return mateBp;	
	}

	private Integer getCurrentBp(int i, boolean isLeft, String knownStrand, Integer knownBreakpoint) {
		Integer startPos = new Integer(tStarts[i]);
		Integer endPos = new Integer(tStarts[i]) + new Integer(blockSizes[i]);
		Integer currentBp = null;
		int buffer = 0;
		if (strand.equals("-")) {
			buffer = 0;
		}
		
		if (knownStrand.equals(strand)) {			
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
	
	private Integer getMateCurrentBp(int i, boolean isLeft, String knownStrand, Integer knownBreakpoint) {
		Integer startPos = new Integer(tStarts[i]);
		Integer endPos = new Integer(tStarts[i]) + new Integer(blockSizes[i]) - 1;
		Integer currentBp = null;
				
		boolean isStart = true;
		if (knownStrand.equals(strand)) {
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

		if (strand.equals("+")) {
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
		if (this.name.equals(o.getName())) {
			return new Integer(this.score).compareTo(new Integer(o.getScore()));
		} else {
			return this.name.compareTo(o.getName());
		}
	}

	public SplitReadAlignment getSplitReadAlignment(int i) {
		Integer startPos = new Integer(tStarts[i]);
		Integer endPos = new Integer(tStarts[i]) + new Integer(blockSizes[i]) -1;
		Integer queryStart = new Integer(qStarts[i]);
		Integer queryEnd = new Integer(qStarts[i]) + new Integer(blockSizes[i]) - 1;
		SplitReadAlignment s = new SplitReadAlignment(reference, strand, startPos, endPos, queryStart, queryEnd);
		
		return s;
	}
	
	@Override 
	public String toString() {
		return this.score + "\t" + this.recordString;
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

	public String toTabString() {
		return "records.add(new BLATRecord(\"" + recordString.replace("\t", "\\t") + "\"));";
	}
}
