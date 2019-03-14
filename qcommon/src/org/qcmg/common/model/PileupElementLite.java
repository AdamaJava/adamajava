/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;

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
		return new int[] { (int)((l >>> STRAND_BIT_POSITION) & 1),  (int)((l >>> END_OF_READ_BIT_POSITION) & 1), (byte)(l >>> QUALITY_BIT_POSITION), (int) l};
	}
	
	

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
	public int getTotalCount() {
		return null != readNameHashStrandPositionQualitys ? readNameHashStrandPositionQualitys.size() / 2 : 0;
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
