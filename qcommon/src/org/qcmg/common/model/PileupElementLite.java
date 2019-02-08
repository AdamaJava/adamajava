/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import java.util.ArrayList;
import java.util.List;

import org.qcmg.common.util.PileupElementLiteUtil;

import gnu.trove.list.TIntList;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.hash.TIntHashSet;


/**
 * Note: this class has a natural ordering that is inconsistent with equals.
 * The compareTo method only takes the total count into account
 * 
 * @author oholmes
 *
 */
@Deprecated
public class PileupElementLite implements Comparable<PileupElementLite> {
	
	public static final long STRAND_BIT = 0x8000000000000000l;
	public static final int STRAND_BIT_POSITION = 63;
	public static final long END_OF_READ_BIT = 0x4000000000000000l;
	public static final int END_OF_READ_BIT_POSITION = 62;
	public static final int QUALITY_BIT_POSITION = 32;
	
	private TLongList readNameHashStrandPositionQualitys;
//	private TIntList readIdStartPositionsQualityList;
//	private TIntList reverseReadIdStartPositionsQualityList;
//	private TIntList forwardReadIdStartPositionsQualityList;
	
//	List<String> readNamesFS = new ArrayList<>(20);
//	List<String> readNamesRS = new ArrayList<>(20);
	
//	private short endOfReadCountFS;
//	private short endOfReadCountRS;
	
	public void add(long readNameHash, boolean forwardStrand, byte quality, int startPosition, boolean endOfRead) {
		if (null == readNameHashStrandPositionQualitys) {
			readNameHashStrandPositionQualitys = new TLongArrayList(60);
		}
		
		/*
		 * add the read name hash first, and then the long that makes up the strand, quality and position
		 */
		readNameHashStrandPositionQualitys.add(readNameHash);
		readNameHashStrandPositionQualitys.add(convertStrandQualAndPositionToLong(forwardStrand, endOfRead, quality, startPosition));
	}
	
	public TLongList getData() {
		return readNameHashStrandPositionQualitys;
	}
	
	/**
	 * 
	 * Returns a long that is made up of the supplied inputs.
	 * The most significant bit is the strand
	 * The next most significant bit is the endOfRead
	 * The next 8 bits are set to the quality value
	 * The first 32 bits are set to the position
	 * easy!
	 * 
	 * 
	 * @param forwardStrand
	 * @param endOfRead
	 * @param quality
	 * @param position
	 * @return
	 */
	public static long convertStrandQualAndPositionToLong(boolean forwardStrand, boolean endOfRead, byte quality, int position) {
		
		long l = 0;
		if (forwardStrand) {
			l |= STRAND_BIT;
		}
		
		if (endOfRead) {
			l |= END_OF_READ_BIT;
		}
		
		l += (long)quality << QUALITY_BIT_POSITION;
		l += (long)position;
		
		return l;
	}
	
	public static int[] convertLongToStrandQualAndPosition(long l) {
		
//		System.out.println("(((l >> 62) & 1): " + (((l >> 62) & 1)));
//		System.out.println("in convert l: " + Long.toBinaryString(l));
//		System.out.println("in convert (l >> 62): " + Long.toBinaryString((l >> 62)));
		
		return new int[] { (int)((l >>> STRAND_BIT_POSITION) & 1),  (int)((l >>> END_OF_READ_BIT_POSITION) & 1), (byte)(l >>> QUALITY_BIT_POSITION), (int) l};
	}
	
	

//	public TIntList getForwardReadIds() {
//		if (null == forwardReadIdStartPositionsQualityList) {
//			return new TIntArrayList(0);
//		}
//		return PileupElementLiteUtil.getDetailsFromCombinedList(forwardReadIdStartPositionsQualityList, 3, 0);
//	}
//
//	public TIntList getReverseReadIds() {
//		if (null == reverseReadIdStartPositionsQualityList) {
//			return new TIntArrayList(0);
//		}
//		return PileupElementLiteUtil.getDetailsFromCombinedList(reverseReadIdStartPositionsQualityList, 3, 0);
//	}
//	
	public int getForwardCount() {
		
		int tally = 0;
		if (null != readNameHashStrandPositionQualitys) {
			for (int i = 1, len = readNameHashStrandPositionQualitys.size() ; i < len ; i += 2) {
				if (((readNameHashStrandPositionQualitys.get(i)  >>> STRAND_BIT_POSITION) & 1) == 1) {
					tally++;
				}
			}
		}
		return tally;
	}
	public int getReverseCount() {
		
		int tally = 0;
		if (null != readNameHashStrandPositionQualitys) {
			for (int i = 1, len = readNameHashStrandPositionQualitys.size() ; i < len ; i += 2) {
				if (((readNameHashStrandPositionQualitys.get(i)  >>> STRAND_BIT_POSITION) & 1) == 0) {
					tally++;
				}
			}
		}
		return tally;
	}
//	
//	public int getReverseCount() {
//		return reverseReadIdStartPositionsQualityList == null || reverseReadIdStartPositionsQualityList.isEmpty() ? 0 : reverseReadIdStartPositionsQualityList.size() / 3;
//	}
//	
//	public TIntList getReadIdStartPositionsQualityList(boolean fs) {
//		return fs ? forwardReadIdStartPositionsQualityList : reverseReadIdStartPositionsQualityList;
//	}
//	
	public int getTotalCount() {
		return null != readNameHashStrandPositionQualitys ? readNameHashStrandPositionQualitys.size() / 2 : 0;
	}
//	
//	public boolean isFoundOnBothStrands() {
//		return getForwardCount() > 0 && getReverseCount() > 0;
//	}
//	
//	public boolean isFoundOnBothStrandsMiddleOfRead() {
//		return getForwardCount() - endOfReadCountFS > 0 && getReverseCount() - endOfReadCountRS > 0;
//	}
//	public void addForwardQuality(byte b, int startPosition, int readId, boolean endOfRead) {
//		addForwardQuality(b, startPosition, readId, endOfRead, "" + readId);
//	}
//	
//	public void addForwardQuality(byte b, int startPosition, int readId, boolean endOfRead, String readName) {
//		if (null == forwardReadIdStartPositionsQualityList) {
//			forwardReadIdStartPositionsQualityList = new TIntArrayList(20);
//		}
//		readNamesRS.add(readName);
//		forwardReadIdStartPositionsQualityList.add(readId);
//		forwardReadIdStartPositionsQualityList.add(startPosition);
//		forwardReadIdStartPositionsQualityList.add(b);
//		if (endOfRead) endOfReadCountFS++;
//	}
//
//	public void addReverseQuality(byte b, int startPosition, int readId, boolean endOfRead) {
//		addReverseQuality(b, startPosition, readId, endOfRead, "" + readId);
//	}
//	public void addReverseQuality(byte b, int startPosition, int readId, boolean endOfRead, String readName) {
//		if (null == reverseReadIdStartPositionsQualityList) {
//			reverseReadIdStartPositionsQualityList = new TIntArrayList(20);
//		}
//		readNamesFS.add(readName);
//		reverseReadIdStartPositionsQualityList.add(readId);
//		reverseReadIdStartPositionsQualityList.add(startPosition);
//		reverseReadIdStartPositionsQualityList.add(b);
//		if (endOfRead) endOfReadCountRS++;
//	}
//	
//	public List<String> getReadNames() {
//		List<String> l = new ArrayList<>(readNamesFS);
//		l.addAll(readNamesRS);
//		return l;
//	}
//	
//	public int getEndOfReadCount() {
//		return endOfReadCountFS + endOfReadCountRS;
//	}
//	
//	public String getEndOfReadString() {
//		return endOfReadCountFS + "[]" + endOfReadCountRS + "[]";
//	}
//	
//	public int getMiddleOfReadCount() {
//		return getTotalCount() - getEndOfReadCount();
//	}
//	
//	public int getNovelStartCount() {
//		int ns = 0;
//		if (null != reverseReadIdStartPositionsQualityList) {
//			ns = new TIntHashSet(PileupElementLiteUtil.getDetailsFromCombinedList(reverseReadIdStartPositionsQualityList, 3,1)).size();
//		}
//		if (null != forwardReadIdStartPositionsQualityList) {
//			ns += new TIntHashSet(PileupElementLiteUtil.getDetailsFromCombinedList(forwardReadIdStartPositionsQualityList, 3, 1)).size();
//		}
//		return ns;
//	}
//	
//	public int getTotalForwardQualityScore() {
//		return forwardReadIdStartPositionsQualityList == null || forwardReadIdStartPositionsQualityList.isEmpty() ? 0 : 
//			PileupElementLiteUtil.getDetailsFromCombinedList(forwardReadIdStartPositionsQualityList, 3,2).sum();
//	}
//	public int getTotalReverseQualityScore() {
//		return reverseReadIdStartPositionsQualityList == null || reverseReadIdStartPositionsQualityList.isEmpty() ? 0 : 
//			PileupElementLiteUtil.getDetailsFromCombinedList(reverseReadIdStartPositionsQualityList, 3,2).sum();
//	}
//	
//	public int getTotalQualityScore() {
//		return getTotalForwardQualityScore() + getTotalReverseQualityScore();
//	}
	
	/**
	 * Note: this class has a natural ordering that is inconsistent with equals.
	 */
	@Override
	public int compareTo(PileupElementLite o) {
		// only interested in the total count for the purposes or ordering
		return o.getTotalCount() - getTotalCount();
	}

}
