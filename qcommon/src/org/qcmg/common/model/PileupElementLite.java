/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import org.qcmg.common.util.PileupElementLiteUtil;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;


/**
 * Note: this class has a natural ordering that is inconsistent with equals.
 * The compareTo method only takes the total count into account
 * 
 * @author oholmes
 *
 */
public class PileupElementLite implements Comparable<PileupElementLite> {
	
	private TIntList reverseReadIdStartPositionsQualityList;
	private TIntList forwardReadIdStartPositionsQualityList;
	
	private short endOfReadCountFS;
	private short endOfReadCountRS;

	public TIntList getForwardReadIds() {
		if (null == forwardReadIdStartPositionsQualityList) {
			return new TIntArrayList(0);
		}
		return PileupElementLiteUtil.getDetailsFromCombinedList(forwardReadIdStartPositionsQualityList, 3, 0);
	}

	public TIntList getReverseReadIds() {
		if (null == reverseReadIdStartPositionsQualityList) {
			return new TIntArrayList(0);
		}
		return PileupElementLiteUtil.getDetailsFromCombinedList(reverseReadIdStartPositionsQualityList, 3, 0);
	}
	
	public int getForwardCount() {
		return forwardReadIdStartPositionsQualityList == null || forwardReadIdStartPositionsQualityList.isEmpty() ? 0 : forwardReadIdStartPositionsQualityList.size() / 3;
	}
	
	public int getReverseCount() {
		return reverseReadIdStartPositionsQualityList == null || reverseReadIdStartPositionsQualityList.isEmpty() ? 0 : reverseReadIdStartPositionsQualityList.size() / 3;
	}
	
	public TIntList getReadIdStartPositionsQualityList(boolean fs) {
		return fs ? forwardReadIdStartPositionsQualityList : reverseReadIdStartPositionsQualityList;
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
		if (null == forwardReadIdStartPositionsQualityList) {
			forwardReadIdStartPositionsQualityList = new TIntArrayList();
		}
		
		forwardReadIdStartPositionsQualityList.add(readId);
		forwardReadIdStartPositionsQualityList.add(startPosition);
		forwardReadIdStartPositionsQualityList.add(b);
		if (endOfRead) endOfReadCountFS++;
	}

	public void addReverseQuality(byte b, int startPosition, int readId, boolean endOfRead) {
		if (null == reverseReadIdStartPositionsQualityList) {
			reverseReadIdStartPositionsQualityList = new TIntArrayList();
		}
		
		reverseReadIdStartPositionsQualityList.add(readId);
		reverseReadIdStartPositionsQualityList.add(startPosition);
		reverseReadIdStartPositionsQualityList.add(b);
		if (endOfRead) endOfReadCountRS++;
	}
	
	public int getEndOfReadCount() {
		return endOfReadCountFS + endOfReadCountRS;
	}
	
	public String getEndOfReadString() {
		return endOfReadCountFS + "[]" + endOfReadCountRS + "[]";
	}
	
	public int getMiddleOfReadCount() {
		return getTotalCount() - getEndOfReadCount();
	}
	
	public int getNovelStartCount() {
		int ns = 0;
		if (null != reverseReadIdStartPositionsQualityList) {
			ns = new TIntHashSet(PileupElementLiteUtil.getDetailsFromCombinedList(reverseReadIdStartPositionsQualityList, 3,1)).size();
		}
		if (null != forwardReadIdStartPositionsQualityList) {
			ns += new TIntHashSet(PileupElementLiteUtil.getDetailsFromCombinedList(forwardReadIdStartPositionsQualityList, 3, 1)).size();
		}
		return ns;
	}
	
	public int getTotalForwardQualityScore() {
		return forwardReadIdStartPositionsQualityList == null || forwardReadIdStartPositionsQualityList.isEmpty() ? 0 : 
			PileupElementLiteUtil.getDetailsFromCombinedList(forwardReadIdStartPositionsQualityList, 3,2).sum();
	}
	public int getTotalReverseQualityScore() {
		return reverseReadIdStartPositionsQualityList == null || reverseReadIdStartPositionsQualityList.isEmpty() ? 0 : 
			PileupElementLiteUtil.getDetailsFromCombinedList(reverseReadIdStartPositionsQualityList, 3,2).sum();
	}
	
	public int getTotalQualityScore() {
		return getTotalForwardQualityScore() + getTotalReverseQualityScore();
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
