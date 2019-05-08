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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.model.QCMGAtomicLongArray;

public class PositionSummary {
	public static final int BUCKET_SIZE = 1000000;
	
	private final AtomicInteger min;
	private final AtomicInteger max;
	private final QCMGAtomicLongArray[] rgCoverages; // the coverage for each readgroup on that position   
	private final QCMGAtomicLongArray coverage = new QCMGAtomicLongArray(512);  // total coverage on that position
	private final List<String> readGroupIds;
	
	private final ArrayList<Long> maxRgs = new ArrayList<Long>(); //store the max coverage from all read group at each position;
	private Boolean hasAddPosition = true; //set to true after each time addPosition, set to false after getAverage
		
	/**
	 * No default constructor defined as we don't want the initial values of 
	 * min and max to be zero (unless of course the passed in initial position value is zero)
	 * Create the AtomicLongArray with size of 512 as we are binning by the million, and the largest chromosome is around the 250 mill mark
	 * Should give us some breathing space..
	 * @param position sets the first position value of this summary record to position
	 * 
	 */	
	public PositionSummary(List<String> rgs) {
		readGroupIds = rgs; 
		min = new AtomicInteger(512*BUCKET_SIZE);
		max = new AtomicInteger(0); 
		rgCoverages = new QCMGAtomicLongArray[rgs.size()];
		for(int i = 0; i < rgs.size(); i ++)
			rgCoverages[i] = new QCMGAtomicLongArray(512);
	}
	
	public int getMin() { 	return min.intValue(); }
	
	public int getMax() {	return max.intValue(); }
	
	public int getBinNumber() {
		return (max.get()/BUCKET_SIZE) + 1;
	}

	/**
	 * 
	 * @return the max read coverage based on the max coverage from each read group and each position
	 */
	public long getMaxRgCoverage(){
		if( hasAddPosition  || maxRgs.isEmpty()){
			maxRgs.clear();
			for (int i = 0, length = (max.get()/BUCKET_SIZE) + 1; i < length ; i++){
				long[] rgCov = new long[rgCoverages.length];				
				for(int j = 0; j < rgCoverages.length; j ++)
					rgCov[j] = rgCoverages[j].get(i);
				Arrays.sort(rgCov);
				maxRgs.add( rgCov[rgCoverages.length - 1 ]);   //get the max coverage of all readGroup at that position		
			}
			hasAddPosition = false; 
		}
		//stream is slow but we only use it once		
		return maxRgs.stream().mapToLong(val -> val).sum();
	}
	/**
	 * 
	 * @param floorValue
	 * @return return the bin number which max readgroup coverage over the floorValue
	 */
	public int getBigBinNumber(final long floorValue){
		if( hasAddPosition  || maxRgs.isEmpty())  
			getMaxRgCoverage(); //caculate the maxRgs 		
		return  (int) maxRgs.stream().mapToLong(val -> val).filter(val -> val > floorValue).count();
	}
	
	
	
		
	public long getTotalCount() {
		long count = 0;		 
		for (int i = 0, length = (int)coverage.length() ; i < length ; i++)
				count += coverage.get(i);		 
		return count;
	}
	
	
	/**
	 * Returns a map which holds the coverage of positions binned by millions
	 * each element of Map is : <bin_order, reads number on that bin from specified read group> 
	 */
	
	public Map<Integer,  AtomicLong> getCoverageByRg( String rg) {
		Map<Integer, AtomicLong> singleRgCoverage = new TreeMap<Integer, AtomicLong>();		
		for (int i = 0, max =getBinNumber() ; i < max ; i++){ 	
			if(coverage.get(i) == 0) continue; 			
			int order = readGroupIds.indexOf(rg);			
			singleRgCoverage.put( i, new AtomicLong( rgCoverages[order].get(i)));
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
		//count for nominated rg on that position
		int order = readGroupIds.indexOf(rgid);
		if(order < 0 )
			throw new IllegalArgumentException("can't find readGroup Id on Bam header: @RG ID:"+ rgid);
		
		rgCoverages[ readGroupIds.indexOf(rgid)  ].increment(position / BUCKET_SIZE);  
		//last element is the total counts on that position
		coverage.increment(position / BUCKET_SIZE); 
		
		hasAddPosition = true;
	}
}
