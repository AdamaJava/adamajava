/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */

package org.qcmg.common.model;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.TabTokenizer;

import java.util.Arrays;

/**
 * Class representing a result returned from BLAT psl file
 * @author felicity
 *
 */
public class BLATRecord implements Comparable<BLATRecord> {
	
	public static final char PLUS = '+';
	public static final char MINUS = '-';
	
	public static class Builder {

		private int match;
		private int misMatch;
		private int repMatch;
		private int nCount;
		private int qNumInsert;
		private int qBaseInsert;
		private int tNumInsert;
		private int tBaseInsert;
		private  char strand;
		private String qName;
		private  int size;
		private  int qStart;
		private  int qEnd;
		private String tName;
		private  int tSize;
		private  int tStart;
		private  int tEnd;
		private  int blockCount;
		private  String blockSizes;
		private  String qStarts;
		private  String tStarts;
		
		public String[] getValues() {
			return new String[] {"" + match, "" + misMatch, "" + repMatch, "" + nCount, "" + qNumInsert, "" + qBaseInsert, 
					"" + tNumInsert, "" + tBaseInsert, "" + strand, qName, "" + size, "" + qStart, "" + qEnd, tName, "" + tSize, 
					"" + tStart, "" + tEnd, "" + blockCount, blockSizes, qStarts, tStarts};
		}
		
		public Builder() {}
		public Builder(String rawDataLine) {
			this(TabTokenizer.tokenize(rawDataLine));
		}
		
		public Builder(String [] rawData) {
			withMatch(Integer.parseInt(rawData[0]))
			.withMisMatch(Integer.parseInt(rawData[1]))
			.withRepMatch(Integer.parseInt(rawData[2]))
			.withNCount(Integer.parseInt(rawData[3]))
			.withQNumInsert(Integer.parseInt(rawData[4]))
			.withQBaseInsert(Integer.parseInt(rawData[5]))
			.withTNumInsert(Integer.parseInt(rawData[6]))
			.withTBaseInsert(Integer.parseInt(rawData[7]))
			.withStrand(rawData[8].charAt(0))
			.withQName(rawData[9])
			.withSize(Integer.parseInt(rawData[10]))
			.withQStart(Integer.parseInt(rawData[11]))
			.withQEnd(Integer.parseInt(rawData[12]))
			.withTName((rawData[13]))
			.withTSize(Integer.parseInt(rawData[14]))
			.withTStart(Integer.parseInt(rawData[15]))
			.withTEnd(Integer.parseInt(rawData[16]))
			.withBlockCount(Integer.parseInt(rawData[17]))
			.withBlockSizes(rawData[18])
			.withQStarts(rawData[19])
			.withTStarts(rawData[20]);
		}
		
		public Builder withMatch(int match) {
			this.match = match;
			return this;
		}
		
		public Builder withMisMatch(int misMatch) {
			this.misMatch = misMatch;
			return this;
		}
		
		public Builder withRepMatch(int repMatch) {
			this.repMatch = repMatch;
			return this;
		}
		
		public Builder withNCount(int nCount) {
			this.nCount = nCount;
			return this;
		}
		
		public Builder withQNumInsert(int qNumInsert) {
			this.qNumInsert = qNumInsert;
			return this;
		}
		
		public Builder withQBaseInsert(int qBaseInsert) {
			this.qBaseInsert = qBaseInsert;
			return this;
		}
		
		public Builder withTNumInsert(int tNumInsert) {
			this.tNumInsert = tNumInsert;
			return this;
		}
		
		public Builder withTBaseInsert(int tBaseInsert) {
			this.tBaseInsert = tBaseInsert;
			return this;
		}
		
		public Builder withStrand(char strand) {
			this.strand = strand;
			return this;
		}
		
		public Builder withQName(String qName) {
			this.qName = qName;
			return this;
		}
		
		public Builder withSize(int size) {
			this.size = size;
			return this;
		}
		
		public Builder withQStart(int qStart) {
			this.qStart = qStart;
			return this;
		}
		
		public Builder withQEnd(int qEnd) {
			this.qEnd = qEnd;
			return this;
		}
		
		public Builder withTName(String tName) {
			this.tName = tName;
			return this;
		}
		
		public Builder withTSize(int tSize) {
			this.tSize = tSize;
			return this;
		}
		
		public Builder withTStart(int tStart) {
			this.tStart = tStart;
			return this;
		}
		
		public Builder withTEnd(int tEnd) {
			this.tEnd = tEnd;
			return this;
		}
		
		public Builder withBlockCount(int blockCount) {
			this.blockCount = blockCount;
			return this;
		}
		
		public Builder withBlockSizes(String blockSizes) {
			this.blockSizes = blockSizes;
			return this;
		}
		
		public Builder withQStarts(String qStarts) {
			this.qStarts = qStarts;
			return this;
		}
		
		public Builder withTStarts(String tStarts) {
			this.tStarts = tStarts;
			return this;
		}
		
		public BLATRecord build() {
			return new BLATRecord(this);
		}
	}

	private final boolean valid;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + blockCount;
		result = prime * result + ((blockSizes == null) ? 0 : blockSizes.hashCode());
		result = prime * result + match;
		result = prime * result + misMatch;
		result = prime * result + nCount;
		result = prime * result + nonTempBases;
		result = prime * result + qBaseInsert;
		result = prime * result + qEnd;
		result = prime * result + ((qName == null) ? 0 : qName.hashCode());
		result = prime * result + qNumInsert;
		result = prime * result + qStart;
		result = prime * result + ((qStarts == null) ? 0 : qStarts.hashCode());
		result = prime * result + repMatch;
		result = prime * result + size;
		result = prime * result + strand;
		result = prime * result + tBaseInsert;
		result = prime * result + tEnd;
		result = prime * result + ((tName == null) ? 0 : tName.hashCode());
		result = prime * result + tNumInsert;
		result = prime * result + tSize;
		result = prime * result + tStart;
		result = prime * result + ((tStarts == null) ? 0 : tStarts.hashCode());
		result = prime * result + (valid ? 1231 : 1237);
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
		BLATRecord other = (BLATRecord) obj;
		if (blockCount != other.blockCount)
			return false;
		if (blockSizes == null) {
			if (other.blockSizes != null)
				return false;
		} else if (!blockSizes.equals(other.blockSizes))
			return false;
		if (match != other.match)
			return false;
		if (misMatch != other.misMatch)
			return false;
		if (nCount != other.nCount)
			return false;
		if (nonTempBases != other.nonTempBases)
			return false;
		if (qBaseInsert != other.qBaseInsert)
			return false;
		if (qEnd != other.qEnd)
			return false;
		if (qName == null) {
			if (other.qName != null)
				return false;
		} else if (!qName.equals(other.qName))
			return false;
		if (qNumInsert != other.qNumInsert)
			return false;
		if (qStart != other.qStart)
			return false;
		if (qStarts == null) {
			if (other.qStarts != null)
				return false;
		} else if (!qStarts.equals(other.qStarts))
			return false;
		if (repMatch != other.repMatch)
			return false;
		if (size != other.size)
			return false;
		if (strand != other.strand)
			return false;
		if (tBaseInsert != other.tBaseInsert)
			return false;
		if (tEnd != other.tEnd)
			return false;
		if (tName == null) {
			if (other.tName != null)
				return false;
		} else if (!tName.equals(other.tName))
			return false;
		if (tNumInsert != other.tNumInsert)
			return false;
		if (tSize != other.tSize)
			return false;
		if (tStart != other.tStart)
			return false;
		if (tStarts == null) {
			if (other.tStarts != null)
				return false;
		} else if (!tStarts.equals(other.tStarts))
			return false;
        return valid == other.valid;
    }

	private final int[] tStartsArray;
	private final int[] blockSizesArray;
	private final int[] unmodifiedStarts;
	
	
	/*
	 * Fields taken from ensemble's psl format http://m.ensembl.org/info/website/upload/psl.html
	 */
	private final int match;
	private final int misMatch;
	private final int repMatch;
	private final int nCount;
	private final int qNumInsert;
	private final int qBaseInsert;
	private final int tNumInsert;
	private final int tBaseInsert;
	private final char strand;
	private String qName;
	private final int size;
	private final int qStart;
	private final int qEnd;
	private final String tName;
	private final int tSize;
	private final int tStart;
	private final int tEnd;
	private final int blockCount;
	private  final String blockSizes;
	private final String qStarts;
	private final String tStarts;
	private int nonTempBases = 0;
	
	public BLATRecord(Builder builder) {
		this.match = builder.match;
		this.misMatch = builder.misMatch;
		this.repMatch = builder.repMatch;
		this.nCount = builder.nCount;
		this.qNumInsert = builder.qNumInsert;
		this.qBaseInsert = builder.qBaseInsert;
		this.tNumInsert = builder.tNumInsert;
		this.tBaseInsert = builder.tBaseInsert;
		this.strand = builder.strand;
		this.qName = builder.qName;
		this.size = builder.size;
		this.qStart = builder.qStart;
		this.qEnd = builder.qEnd;
		this.tName = builder.tName;
		this.tSize = builder.tSize;
		this.tStart = builder.tStart;
		this.tEnd = builder.tEnd;
		this.blockCount = builder.blockCount;
		this.blockSizes = builder.blockSizes;
		this.qStarts = builder.qStarts;
		this.tStarts = builder.tStarts;
		
		/*
		 * not sure of a meaningful measure of validity is so will go with there being non-null entries in the q and t starts fields
		 */
		this.valid =  ! StringUtils.isNullOrEmpty(qStarts) && ! StringUtils.isNullOrEmpty(tStarts);
		
		if (null == qName) {
			System.out.println("null name in BLATRecord: " + Arrays.deepToString(builder.getValues()));
		}
		
		/*
		 * Setup tStarts array if we have sufficient blockCountage
		 */
		if (blockCount > 1) {
			/*
			 * Setup tStarts
			 */
			String [] tStartsS = tStarts.split(",");
			tStartsArray = new int[tStartsS.length];
			for (int i = 0; i < tStartsS.length; i++) {						
				tStartsArray[i] = Integer.parseInt(tStartsS[i]) + 1;	
			}
			/*
			 * Setup block sizes
			 */
			String [] blockSizesS = blockSizes.split(",");
			blockSizesArray = new int[blockSizesS.length];
			for (int i = 0; i < blockSizesS.length; i++) {						
				blockSizesArray[i] = Integer.parseInt(blockSizesS[i]);
			}
			
			/*
			 * setup unmodifiedStarts
			 */
			String[] qStartsS = qStarts.split(",");
			unmodifiedStarts = new int[qStartsS.length];
			for (int i = 0; i < qStartsS.length; i++) {
				if (strand == MINUS) {
					unmodifiedStarts[i] = getSize() - Integer.parseInt(qStartsS[i]) - blockSizesArray[i] + 1;
				} else {
					unmodifiedStarts[i] = Integer.parseInt(qStartsS[i]) + 1;
				}
			}
			
		} else {
			tStartsArray = null;
			blockSizesArray = null;
			unmodifiedStarts = null;
		}
	}
	
	public int getSize() {
		return size;
	}
	
	public int getQueryLength() {
		return getQueryEnd() - (getQueryStart() - 1);
	}
	
	public int getQueryGapCount() {
		return qNumInsert;
	}
	
	public int getMatchCount() {
		return match;
	}

	public String getQName() {
		return qName;
	}
	
	public void setQName(String qName) {
		if (null == qName) {
			System.out.println("setName null");
		}
		this.qName = qName;
	}

	public String getTName() {
		return tName;
	}

	public char getStrand() {
		return strand;
	}

	/**
	 * returns the value of the tStart field plus 1
	 * zero-based
	 */
	public int getStartPos() {
		return tStart + 1;
	}

	public int getEndPos() {
		return tEnd;
	}

	/**
	 * Returns the value of the qStart field plus 1
	 * zero-based
	 */
	public int getQueryStart() {
		return qStart + 1;
	}

	public int getQueryEnd() {
		return qEnd;
	}
	public int getChromsomeLength() {
		return tSize;
	}
	
	public int getScore() {
		return match - misMatch - tNumInsert - qNumInsert;
	}
	
	public boolean isValid() {
		return valid;
	}
	
	public int getBlockCount() {
		return blockCount;
	}

	public int getRepMatch() {
		return repMatch;
	}

	public int getnCount() {
		return nCount;
	}

	public int getqBaseInsert() {
		return qBaseInsert;
	}

	public int gettNumInsert() {
		return tNumInsert;
	}

	public int gettBaseInsert() {
		return tBaseInsert;
	}

	public int getqStart() {
		return qStart;
	}

	public int gettStart() {
		return tStart;
	}

	public int gettEnd() {
		return tEnd;
	}

	public String getqStarts() {
		return qStarts;
	}
	
	public String getBlockSizesString() {
		return blockSizes;
	}
	
	public String gettStarts() {
		return tStarts;
	}


	public Integer calculateMateBreakpoint(boolean isLeft, String knownReference, Integer knownBreakpoint, char knownStrand) {		
		if (getBlockCount() == 1) {
            return calculateSingleMateBreakpoint(isLeft, knownStrand);
		} else {
			if (getBlockCount() == 2) {
				
				//check current breakpoint matches
				Integer mateBp = calculateDoubleMateBreakpoint(isLeft, knownReference, knownBreakpoint, knownStrand);	
				
				if (mateBp == null) {
					return calculateSingleMateBreakpoint(isLeft, knownStrand);
				}
				
				return mateBp;
			}
		}
		return null;
	}	

	private Integer calculateDoubleMateBreakpoint(boolean isLeft, String knownReference, Integer knownBreakpoint, char knownStrand) {
		if (this.tName.equals(knownReference)) {
			if (knownStrand == getStrand()) {
				
				for (int i = 0; i < 2; i++) {
					int currentBp = getCurrentBp(tStartsArray[i], blockSizesArray[i], isLeft, knownStrand == getStrand());				
					
					if (currentBp >= knownBreakpoint - 5 && (currentBp <= knownBreakpoint + 5)) {
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
		int nonTempBases1 = knownStrand == strand ? queryStart : size - queryEnd;
		if (isLeft) {
			mateBp =  knownStrand == strand ? endPos : startPos;
			
			if (strand == PLUS) {
				nonTempBases = knownStrand == strand ? (size - queryEnd): startPos;
			} else {
				nonTempBases = nonTempBases1;
			}
			
		} else {
			mateBp =  knownStrand == strand ? startPos: endPos;
			
			if (strand == PLUS) {
				nonTempBases = nonTempBases1;
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
		int startPos = tStartsArray[i];
		int endPos = startPos + blockSizesArray[i] - 1;
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

		if (strand == PLUS) {
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
		int diff = (null != this.qName && null != o.getQName()) ? this.qName.compareTo(o.getQName()) : null != this.qName ? -1 : null != o.getQName() ? 1 : 0;
		if (diff != 0) {
			return diff;
		} else {
			diff = Integer.compare(getScore(), o.getScore());
			
			if (diff != 0) {
				return diff;
			}
			
			/*
			 * strand next
			 */
			if (strand == PLUS) {
				if (o.strand ==  MINUS) {
					return 1;
				}
			} else if (strand == MINUS) {
				if (o.strand ==  PLUS) {
					return -1;
				}
			}
			
			/*
			 * finally chromosome - lexicographically sorted rather than numerically - thats what BLAT does...
			 */
			diff = o.tName.compareTo(tName);
			if (diff != 0) {
				return diff;
			}
			diff = Integer.compare(o.match, match);
			return diff;
		}
	}
	
	private String[] getValues() {
		return new String[] {"" + match, "" + misMatch, "" + repMatch, "" + nCount, "" + qNumInsert, "" + qBaseInsert, 
				"" + tNumInsert, "" + tBaseInsert, "" + strand, qName, "" + size, "" + qStart, "" + qEnd, tName, "" + tSize, 
				"" + tStart, "" + tEnd, "" + blockCount, blockSizes, qStarts, tStarts};
	}

	@Override 
	public String toString() {
		return getScore() + Constants.TAB_STRING + String.join(Constants.TAB_STRING, getValues());
	}

	public int[] getUnmodifiedStarts() {
		return null != unmodifiedStarts ? Arrays.copyOf(unmodifiedStarts, unmodifiedStarts.length) : null;
	}

	public int[] getBlockSizes() {
		return null != blockSizesArray ? Arrays.copyOf(blockSizesArray, blockSizesArray.length) : null;
	}
	
	public int[] getTStarts() {
		return null != tStartsArray ? Arrays.copyOf(tStartsArray, tStartsArray.length) : null;
	}

	public int getNonTempBases() {
		return this.nonTempBases;
	}
	
	public int getMisMatches() {
		return misMatch;
	}
}
