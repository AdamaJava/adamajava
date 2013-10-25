package org.qcmg.common.model;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;


/**
 * Note: this class has a natural ordering that is inconsistent with equals.
 * The compareTo method only takes the total count into account
 * 
 * @author oholmes
 *
 */
public class PileupElementLite implements Comparable<PileupElementLite> {
	
	/**
	 * Number of reverse strand novel starts that satisfies our curiosity in this position
	 * Once this target is reached, no more novel start information on the reverse strand will be collated.
	 */
//	public static final int REVERSE_STRAND_NOVEL_START_TARGET = 32;
	
//	private AtomicIntegerArray forwardQualitiesArray;
//	private AtomicIntegerArray reverseQualitiesArray;
//	
//	private final AtomicInteger lastForwardStartPosition = new AtomicInteger();
//	private final AtomicInteger forwardNovelStartCount = new AtomicInteger();
//	
	private Queue<Integer> reverseStrandStartPositions;
//	
//	private final AtomicInteger endOfReadCountFS = new AtomicInteger();
//	private final AtomicInteger endOfReadCountRS = new AtomicInteger();
	
	private volatile int forwardCount;
	private volatile int reverseCount;
	private QCMGIntArray forwardQualitiesArray;
	private QCMGIntArray reverseQualitiesArray;
	
	private int lastForwardStartPosition;
	private int forwardNovelStartCount;
	
//	private List<Integer> reverseStrandStartPositions;
	
	private int endOfReadCountFS;
	private int endOfReadCountRS;
	
	
	/**
	 * If reverse strand, then the start position needs to be the end of read position, as they are reported on the +ve strand
	 * 
	 * @param startPosition
	 * @param forwardStrand
	 */
//	private void updateNovelStarts(int startPosition, boolean forwardStrand) {
//		if (forwardStrand) {
//			if (startPosition > lastForwardStartPosition.get()) {
//				forwardNovelStartCount.incrementAndGet();
//				lastForwardStartPosition.set(startPosition);
//			}
//		} else {
//			if (null == reverseStrandStartPositions)
//				reverseStrandStartPositions = new ConcurrentLinkedQueue<Integer>();
//			reverseStrandStartPositions.add(startPosition);
//		}
//	}
	private void updateNovelStarts(int startPosition, boolean forwardStrand) {
		if (forwardStrand) {
			if (startPosition > lastForwardStartPosition) {
				forwardNovelStartCount++;
				lastForwardStartPosition = startPosition;
			}
		} else {
			if (null == reverseStrandStartPositions)
				reverseStrandStartPositions = new ArrayDeque<Integer>();
			reverseStrandStartPositions.add(startPosition);
		}
	}
	
	public int getForwardCount() {
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
	
//	public boolean isFoundOnBothStrandsMiddleOfRead() {
//		return getForwardCount() - endOfReadCountFS.get() > 0 && getReverseCount() - endOfReadCountRS.get() > 0;
//	}
	public boolean isFoundOnBothStrandsMiddleOfRead() {
		return getForwardCount() - endOfReadCountFS > 0 && getReverseCount() - endOfReadCountRS > 0;
	}
	
	public void addForwardQuality(byte b, int startPosition) {
		addForwardQuality(b, startPosition, false);
	}
	public void addReverseQuality(byte b, int startPosition) {
		addReverseQuality(b, startPosition, false);
	}
	
	public void addForwardQuality(byte b, int startPosition, boolean endOfRead) {
		if (null == forwardQualitiesArray) forwardQualitiesArray = new QCMGIntArray(100);
		forwardQualitiesArray.increment(b);
		updateNovelStarts(startPosition, true);
		if (endOfRead) endOfReadCountFS++;
	}

	public void addReverseQuality(byte b, int startPosition, boolean endOfRead) {
		if (null == reverseQualitiesArray) reverseQualitiesArray = new QCMGIntArray(100);
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
	
//	public int getNovelStartCount() {
//		if (null != reverseStrandStartPositions) {
//			
//			Set<Integer> set = new HashSet<Integer>(reverseStrandStartPositions);
//			return forwardNovelStartCount.get() + set.size();
////			return forwardNovelStartCount.get() + reverseStrandStartPositions.size();
//		}
//		return forwardNovelStartCount.get();
//	}
	public int getNovelStartCount() {
		if (null != reverseStrandStartPositions) {
			Set<Integer> set = new HashSet<Integer>(reverseStrandStartPositions);
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
