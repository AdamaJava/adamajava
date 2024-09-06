/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler2.summarise;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.model.QCMGAtomicLongArray;

public class PositionSummary {
	public static final int BUCKET_SIZE = 1000000;
	
	private final Map<String, QCMGAtomicLongArray> rgCoverages; // the coverage for each readgroup on that position

    /**
	 * No default constructor defined as we don't want the initial values of 
	 * min and max to be zero (unless of course the passed in initial position value is zero)
	 * Create the AtomicLongArray with size of 512 as we are binning by the million, and the largest chromosome is around the 250 mill mark
	 * Should give us some breathing space..
	 *
	 */	
	public PositionSummary(List<String> rgs) {		
		rgCoverages = new ConcurrentHashMap<>();
		// create a QCMGAtomicLongArray for each readGroup, and add it to
		// the map
		rgs.forEach(rg -> rgCoverages.put(rg, new QCMGAtomicLongArray(512)));
	}
	
	public long getTotalCount() {
		return rgCoverages.values()
				.parallelStream()
				.mapToLong(QCMGAtomicLongArray::getSum)
				.sum();
	}
	
	public long getTotalCountByRg(String rg) {
		return rgCoverages.get(rg).getSum();
	}
		
	/**
	 * Returns a map which holds the coverage of positions binned by millions
	 * each element of Map is  bin_order, reads number on that bin from specified read group> 
	 */	
	public Map<Integer,  AtomicLong> getCoverageByRg( String rg) {
		Map<Integer, AtomicLong> singleRgCoverage = new TreeMap<>();

		QCMGAtomicLongArray array = rgCoverages.get(rg);
		for (int i = 0, max = array.length() ; i < max ; i ++) {
			if (array.get(i) > 0) {
				singleRgCoverage.put( i, new AtomicLong( array.get(i)));
			}
		}
		return singleRgCoverage;
	}	
		
	/**
	 * This method adds a position to the Summary.
	 * It will adjust the min/max values, and increment the coverage map accordingly
	 * 
	 * @param position int relating to the position being added to the summary
	 */
	public void addPosition(final int position, String  rgid  ) {
		rgCoverages.get(rgid).increment(position / BUCKET_SIZE);
    }
}
