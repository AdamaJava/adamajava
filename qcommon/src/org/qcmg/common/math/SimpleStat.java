package org.qcmg.common.math;

import java.util.ArrayList;
import java.util.List;

public class SimpleStat {	
		
	/**
	 * 
	 * @param list of number
	 * @return the sum of the list
	 */
	public static <T extends Number> double getSum( List<T> list ){
		double sum = 0.0;
		for (T i : list)  
            sum+=  i.doubleValue();  		
		return sum; 
	}
	
	/**
	 * 
	 * @param list of number
	 * @return the average value of the list
	 */
	public static <T extends Number> double getMean( List<T> list ){
		if(list == null || list.size() == 0) return 0;		
		return  getSum(list) / list.size();
	}	
	
	/**
	 * 
	 * @param list of number
	 * @return the standard variance 
	 */
	public static <T extends Number> double getStd( List<T> list ){
		if(list == null || list.size() == 0) return 0;	
			
		double variance=0, mean = getMean(list);
		for (T i : list) 
			variance += Math.pow( i.doubleValue() - mean, 2); 	
		
		return   Math.sqrt(variance/(list.size()-1));
	}	
	
	/**
	 * 
	 * @param list of number
	 * @return the number on the middle of the list
	 */
	public static <T extends Number> double getMedian( List<T> list ){
		if(list == null || list.size() == 0) return 0;
		
		int mpos = list.size() / 2;
		if (list.size() % 2 == 0)
		    return ( list.get(mpos).doubleValue() +  list.get(mpos -1).doubleValue() )/2;
		else
		   return list.get(mpos).doubleValue();			
	}	
	
	 
	public static <T extends Number> List<T[]> getWithin3STD( List<T[]> list, int column ){
		if(list == null || list.size() == 0) return list;
		
		List<T> focus = new ArrayList<>();
		for(T[] t : list) focus.add(t[column]);
				
		double mean = getMean(focus);
		double std = getStd(focus);
		
		List<T[]> after = new ArrayList<>();
		for(T[] t : list){
			if( (t[column].doubleValue() > mean - 3 * std) &&   (t[column].doubleValue() < mean + 3 * std) )
				after.add(t);
		}
		
		return after; 
	}	


	
}
