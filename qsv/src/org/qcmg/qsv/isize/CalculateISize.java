/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.isize;

import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.qcmg.qsv.QSVException;


public class CalculateISize {
	
	private final TreeMap<Integer, Double> leftMap = new TreeMap<>();
	private final TreeMap<Integer, Double> rightMap = new TreeMap<>();
	private final TreeMap<Integer, Integer> totalMap = new TreeMap<>();
	private double[] leftDyDx;
	private double[] rightDyDx;
	private int isizeMin;
	private int isizeMax;
	
	public CalculateISize(ConcurrentMap<Integer, AtomicInteger> isizeMap) {
		
		for (Entry<Integer, AtomicInteger> entry: isizeMap.entrySet()) {
			totalMap.put(new Integer(entry.getKey().intValue()), new Integer(entry.getValue().intValue()));
		}		
	}
	
	public Map<Integer, Double> getLeftMap() {
		return leftMap;
	}


	public Map<Integer, Double> getRightMap() {
		return rightMap;
	}

	public void calculate() throws QSVException {
		//key = isize, value=count

		if (totalMap.size() < 11) {
			throw new QSVException("INSUFFICIENT_ISIZE_RECORDS");
		}
		
		//remove first point, too anomalous
		totalMap.remove(totalMap.firstKey());
		
		//get map of left side of curve and map of right side of curve
		getLogMaps();
		
		//find derivatives		
		leftDyDx = findFirstDerivative(leftMap);		
		rightDyDx = findFirstDerivative(rightMap);
		
		//left side of curve
		int leftMaxIndex = findMaxIndex(leftDyDx);
		isizeMin = 0;
		isizeMax = 0;
		int count = 0;
		for (Entry<Integer, Double> entry: leftMap.entrySet()) {
			if (count == leftMaxIndex) {
				isizeMin = entry.getKey();
				break;
			}
			count++;
		}
		//take off 5
		isizeMin -= 5;
		
		calculateAreaUnderRightCurve();
	}
	
	private void calculateAreaUnderRightCurve() {
	
		int rightMinIndex = findMinIndex(rightDyDx);

		int index = 0;
		Integer startKey = null;
		int minStart = 0;
		//find minimum of right dydxr 

		for (Entry<Integer, Double> entry: rightMap.entrySet()) {
			if (index == rightMinIndex || (minStart > 0 && minStart <= 3)) { 
				minStart++;
			}
			if (minStart == 4) {
				startKey = entry.getKey();
				break;
			}
			index++;				
		}
		
		//get new right curve points 3 points after min index
		NavigableMap<Integer, Double> map = rightMap.subMap(startKey,true, rightMap.lastKey(), true);
		

		//find derivative again
		double[] newDydxr = findFirstDerivative(map);
		int newRightMinIndex = findMinIndex(newDydxr);
		
		
		int count =0;
		//convert the map to arrays for x/y values
		for (Entry<Integer, Double> entry: map.entrySet()) {
			if (count == newRightMinIndex) {
				isizeMax = entry.getKey().intValue() + 5;
				break;
			}
			count++;				
		}
	}


	int findMaxIndex(double[] array) {
		double max=0; 
		int idx = -1;
		for (int i=0; i<array.length; i++) {
			if (array[i] > max) {
				max = array[i];
				idx = i;
			}
		}
		return idx;
	}
	
	private int findMinIndex(double[] array) {
		double min=array[0]; 
		int idx = 0;
		for (int i=1; i<array.length; i++) {
			if (array[i] < min) {
				min = array[i];
				idx = i;
			}		
		}
		return idx;
	}

	double[] findFirstDerivative(Map<Integer, Double> xyMap) {
		double[] x = new double[xyMap.size()];
		double[] y = new double[xyMap.size()];

		int index =0;
		for (Entry<Integer, Double> entry: xyMap.entrySet()) {
			x[index] = entry.getKey().doubleValue();
			y[index] = entry.getValue().doubleValue();
			index++;
		}
		
		double[] y2 = new double[xyMap.size()];		
		int length = xyMap.size() -1;
		
		y2[0] = (y[1] - y[0])/(x[1] - x[0]);
		y2[length] = (y[length] - y[length-1])/(x[length] - x[length-1]);
		
		for (int i=1; i<length; i++) {
			y2[i] = (y[i+1]-y[i-1])/(x[i+1]-x[i-1]);
		}
		
		return y2;
		
	}

	void getLogMaps() {
		Integer maxCount = null;
		Integer maxKey = null;
		
		//find max 
		for (Entry<Integer, Integer> entry : totalMap.entrySet()) {		

			if (maxCount == null) {
				maxCount = entry.getValue();
				maxKey = entry.getValue();
			}
			if (entry.getValue().intValue() > maxCount) {
				maxCount = entry.getValue();
				maxKey = entry.getKey();
			}
		}

		SortedMap<Integer, Integer> left= totalMap.subMap(0,true, maxKey,true);
		SortedMap<Integer, Integer> right= totalMap.subMap(maxKey, true, totalMap.lastKey(), true);		
		
		//get log of counts for left side
		for (Entry<Integer, Integer> entry: left.entrySet()) {
			leftMap.put(entry.getKey(), new Double(Math.log10(entry.getValue().doubleValue())));			
		}
		
		//get log of counts for right side
		for (Entry<Integer, Integer> entry: right.entrySet()) {
			rightMap.put(entry.getKey(), new Double(Math.log10(entry.getValue().doubleValue())));
		}		
		
	}

	public int getISizeMin() {
		return this.isizeMin;
	}
	
	public int getISizeMax() {
		return this.isizeMax;
	}
}
