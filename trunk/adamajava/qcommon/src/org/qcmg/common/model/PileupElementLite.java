/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;


/**
 * Note: this class has a natural ordering that is inconsistent with equals.
 * The compareTo method only takes the total count into account
 * 
 * @author oholmes
 *
 */
public class PileupElementLite implements Comparable<PileupElementLite> {
	
	private TIntArrayList reverseStrandStartPositions;
	private TIntArrayList reverseReadIds;
	private TIntArrayList forwardReadIds;
//	private TLongArrayList reverseReadIds;
//	private TLongArrayList forwardReadIds;
//	private Queue<Integer> reverseStrandStartPositions;
//	private Queue<Long> reverseReadIds;
//	private Queue<Long> forwardReadIds;
	
	private volatile int forwardCount;
	private volatile int reverseCount;
	private QCMGIntArray forwardQualitiesArray;
	private QCMGIntArray reverseQualitiesArray;
	
	private int lastForwardStartPosition;
	private int forwardNovelStartCount;
	
	private int endOfReadCountFS;
	private int endOfReadCountRS;

	
	
	public TIntArrayList getForwardReadIds() {
		return forwardReadIds;
	}

	public TIntArrayList getReverseReadIds() {
		return reverseReadIds;
	}
//	public Queue<Long> getForwardReadIds() {
//		return forwardReadIds;
//	}
//	
//	public Queue<Long> getReverseReadIds() {
//		return reverseReadIds;
//	}
	
	/**
	 * 
	 * @param startPosition
	 * @param forwardStrand
	 */
	private void updateNovelStarts(int startPosition, boolean forwardStrand) {
		if (forwardStrand) {
			if (startPosition > lastForwardStartPosition) {
				forwardNovelStartCount++;
				lastForwardStartPosition = startPosition;
			}
		} else {
			if (null == reverseStrandStartPositions) {
				reverseStrandStartPositions = new TIntArrayList();
			}
			reverseStrandStartPositions.add(startPosition);
		}
	}
	
	public int getForwardCount() {
		// set volatile variable so that we don't have to recalculate each time this method is called
		int total = forwardCount;
		if (total == 0) {
			if (null != forwardQualitiesArray) {
				for (int i = 0, len = forwardQualitiesArray.length() ; i < len ; i++) {
					total +=  forwardQualitiesArray.get(i);
				}
				forwardCount = total;
			}
		}
		return total;
	}
	
	public int getReverseCount() {
		// set volatile variable so that we don't have to recalculate each time this method is called
		int total = reverseCount;
		if (total == 0) {
			if (null != reverseQualitiesArray) {
				for (int i = 0, len = reverseQualitiesArray.length() ; i < len ; i++) {
					total +=  reverseQualitiesArray.get(i);
				}
				reverseCount = total;
			}
		}
		return total;
	}
	
	public int getTotalCount() {
		return getForwardCount() + getReverseCount();
	}
	
	public boolean isFoundOnBothStrands() {
		return getForwardCount() > 0 && getReverseCount() > 0;
	}
	
	public boolean isFoundOnBothStrandsMiddleOfRead() {
		return getForwardCount() - endOfReadCountFS > 0 && getReverseCount() - endOfReadCountRS > 0;
	}
	
	public void addForwardQuality(byte b, int startPosition, int readId) {
		addForwardQuality(b, startPosition, readId, false);
	}
	public void addReverseQuality(byte b, int startPosition, int readId) {
		addReverseQuality(b, startPosition, readId, false);
	}
	
	public void addForwardQuality(byte b, int startPosition, int readId, boolean endOfRead) {
		if (null == forwardQualitiesArray) {
			forwardQualitiesArray = new QCMGIntArray(100);
			forwardReadIds =new TIntArrayList();
		}
		
		forwardReadIds.add(readId);
		forwardQualitiesArray.increment(b);
		updateNovelStarts(startPosition, true);
		if (endOfRead) endOfReadCountFS++;
	}

	public void addReverseQuality(byte b, int startPosition, int readId, boolean endOfRead) {
		if (null == reverseQualitiesArray) {
			reverseQualitiesArray = new QCMGIntArray(100);
			reverseReadIds = new TIntArrayList();
		}
		
		reverseReadIds.add(readId);
		reverseQualitiesArray.increment(b);
		updateNovelStarts(startPosition, false);
		if (endOfRead) endOfReadCountRS++;
	}
	
	public int getEndOfReadCount() {
		return endOfReadCountFS + endOfReadCountRS;
	}
	
	public int getMiddleOfReadCount() {
		return getTotalCount() - getEndOfReadCount();
	}
	
	public int getNovelStartCount() {
		if (null != reverseStrandStartPositions) {
			TIntSet set = new TIntHashSet(reverseStrandStartPositions);
			return forwardNovelStartCount + set.size();
		}
		return forwardNovelStartCount;
	}
	
	public int getTotalForwardQualityScore() {
		int total = 0;
		if (null != forwardQualitiesArray) {
			for (int i = 0, len = forwardQualitiesArray.length() ; i < len ; i++) {
				int count = forwardQualitiesArray.get(i);
				if (count > 0) {
					total += 	(count * i);
				}
			}
		}
		return total;
	}
	public int getTotalReverseQualityScore() {
		int total = 0;
		if (null != reverseQualitiesArray) {
			for (int i = 0, len = reverseQualitiesArray.length() ; i < len ; i++) {
				int count = reverseQualitiesArray.get(i);
				if (count > 0) {
					total += 	(count * i);
				}
			}
		}
		return total;
	}
	
	public int getTotalQualityScore() {
		return getTotalForwardQualityScore() + getTotalReverseQualityScore();
	}
	
	public String getForwardQualitiesAsString() {
		if (null != forwardQualitiesArray) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0, len = forwardQualitiesArray.length() ; i < len ; i++) {
				int count = forwardQualitiesArray.get(i);
				char c = (char) (i + 33);
				for (int j = 0 ; j < count ; j++) {
					sb.append(c);
				}
			}
			return sb.toString();
		} else return "";
	}
	public String getReverseQualitiesAsString() {
		if (null != reverseQualitiesArray) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0, len = reverseQualitiesArray.length() ; i < len ; i++) {
				int count = reverseQualitiesArray.get(i);
				char c = (char) (i + 33);
				for (int j = 0 ; j < count ; j++) {
					sb.append(c);
				}
			}
			return sb.toString();
		} else return "";
	}

	/**
	 * Note: this class has a natural ordering that is inconsistent with equals.
	 */
	@Override
	public int compareTo(PileupElementLite o) {
		// only interested in the total count for the purposes or ordering
		return o.getTotalCount() - getTotalCount();
	}

}
