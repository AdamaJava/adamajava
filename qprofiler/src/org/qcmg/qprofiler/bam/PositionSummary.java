/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler.bam;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.model.QCMGAtomicLongArray;

public class PositionSummary {
	
	private static final int BUCKET_SIZE = 1000000;
	
	private final AtomicInteger min;
	private final AtomicInteger max;
	
	/**
	 * Create the AtomicLongArray with size of 512 as we are binning by the million, and the largest chromosome is around the 250 mill mark
	 * Should give us some breathing space..
	 */
	private final QCMGAtomicLongArray coverage = new QCMGAtomicLongArray(512);
	
	/**
	 * No default constructor defined as we don't want the initial values of 
	 * min and max to be zero (unless of course the passed in initial position value is zero)
	 * 
	 * @param position sets the first position value of this summary record to position
	 * 
	 */
	public PositionSummary(final int position) {
		min = new AtomicInteger(position);
		max = new AtomicInteger(position);
		addPosition(position);
	}
	
	public int getMin() {
		return min.intValue();
	}
	
	public int getMax() {
		return max.intValue();
	}
	
	public long getCount() {
		long count = 0;
		for (int i = 0, length = (int)coverage.length() ; i < length ; i++) {
			count += coverage.get(i);
		}
		return count;
	}
	
	/**
	 * Returns a map which holds the coverage of positions binned by millions
	 */
	public Map<Integer, AtomicLong> getCoverage() {
		ConcurrentMap<Integer, AtomicLong> sortedCoverage = new ConcurrentSkipListMap<Integer, AtomicLong>();
		for (int i = 0, length = (int)coverage.length() ; i < length ; i++) {
			if (coverage.get(i) > 0)
				sortedCoverage.put(i, new AtomicLong(coverage.get(i)));
		}
		return sortedCoverage;
	}
	
	/**
	 * This method adds a position to the Summary.
	 * It will adjust the min/max values, and increment the coverage map accordingly
	 * 
	 * @param position int relating to the position being added to the summary
	 */
	public void addPosition(final int position) {
		
		// my attempt at a non-blocking updating of max
		int tempMax = max.get();
		if (position > tempMax) {
			for (;;) {
				if (position > tempMax) {
					if (max.compareAndSet(tempMax, position)) break;
					else tempMax = max.get();
				} else break;
			}
		}
		
		// and now min....
		if (position < min.get()) {
			int tempMin = min.get();
			for (;;) {
				if (tempMin > position) {
					if (min.compareAndSet(tempMin, position)) break;
					else tempMin = min.get();
				} else break;
			}
		}
		
		coverage.increment(position / BUCKET_SIZE);
	}
}