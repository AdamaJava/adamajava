/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.model.SummaryByCycle;
import org.qcmg.common.model.SummaryByCycleNew2;

public class SummaryByCycleUtils {
		
	/**
	 * Method that takes in a string, and for each integer (separated by a comma) in that string, calls <code>increment</code>
	 * on the supplied SummaryByCycle object, that is keeping track of the number of occurrences of that particular
	 * integer at that particular position in the string.
	 * <p>
	 * @param summary SummaryByCycle object that is tracking the summary data
	 * @param qualityString String containing the data (comma separated Integers) that we wish to summarise
	 * eg. for Qual data this will be along the lines of 16,15,14,13,12,22,19 etc
	 * @throws Exception - will most likely be a NumberFormatException if the data supplied can't be cast to an Integer
	 */
	public static void parseIntegerSummary(SummaryByCycle<Integer> summary, String qualityString, String seperator) throws Exception {
		if (null != qualityString) {
			String[] quals = qualityString.split(seperator);
			for (int i = 1, size = quals.length ; i <= size ; i++) {
				summary.increment(i, Integer.valueOf( quals[i-1] ));
			}
		}
	}
	
	public static void parseIntegerSummaryST(SummaryByCycle<Integer> summary, String qualityString, String seperator) throws Exception {
		if (null != qualityString) {
			int i = 0;
			for (String s :  TabTokenizer.tokenize(qualityString, seperator.charAt(0))) {
				summary.increment(++i, Integer.valueOf( s ));
			}
		}
	}
	
	/**
	 * Method that takes in a byte array, and for each value in that array, calls <code>increment</code>
	 * on the supplied SummaryByCycle object, that is keeping track of the number of occurrences of that particular
	 * value at that particular position in the string.
	 * 
	 * @param summary SummaryByCycle object that is tracking the summary data
	 * @param qualities byte[] containing the data (comma separated Integers) that we wish to summarise 
	 */
	public static void parseIntegerSummary(SummaryByCycle<Integer> summary, byte[] qualities){
		if (null != qualities) {
			for (int i = 0, size = qualities.length ; i < size ; i++) {
				summary.increment(i+1, qualities[i] & 0xFF);
			}
		}
	}
	public static void parseIntegerSummary(SummaryByCycleNew2<Integer> summary, byte[] qualities){
		if (null != qualities) {
			for (int i = 0, size = qualities.length ; i < size ; i++) {
				summary.increment(i+1, qualities[i] & 0xFF);
			}
		}
	}
	public static void parseIntegerSummary(final SummaryByCycleNew2<Integer> summary, final byte[] qualities, final boolean reverse){
		if (null != qualities) {
			if (reverse) {
				for (int i = qualities.length -1, j = 1 ; i >= 0 ; i--, j++) {
					summary.increment( j, qualities[i] & 0xFF );
				}
			} else {
				for (int i = 0, size = qualities.length ; i < size ; i++) {
					summary.increment( i+1, qualities[i] & 0xFF );
				}
			}
		}
	}
	
	/**
	 * Calls the overloaded method passing in 0 as the offset, and null as the lengthMap
	 * @see org.qcmg.common.util.SummaryByCycleUtils#parseCharacterSummary(SummaryByCycle, String, Map, int)
	 * 
	 * @param summary SummaryByCycle object that is tracking the summary data
	 * @param dataString String containing the data that we wish to summarise
	 * eg. for CS this will be along the lines of T0123012301230123
	 */
	public static void parseCharacterSummary(SummaryByCycle<Character> summary,
			String dataString) {
		parseCharacterSummary(summary, dataString, null, 0);
	}
	
	/**
	 * Calls the overloaded method passing in 0 as the offset
	 * @see org.qcmg.common.util.SummaryByCycleUtils#parseCharacterSummary(SummaryByCycle, String, Map, int)
	 * 
	 * @param summary SummaryByCycle object that is tracking the summary data
	 * @param dataString String containing the data that we wish to summarise
	 * eg. for CS this will be along the lines of T0123012301230123
	 * @param lengthMap Map<Integer, Integer> containing the number of times a string of a 
	 * particular length has occurred in the file- can be null
	 */
	public static void parseCharacterSummary(SummaryByCycle<Character> summary,
			String dataString, ConcurrentMap<Integer, AtomicLong> lengthMap) {
		parseCharacterSummary(summary, dataString, lengthMap, 0);
	}
	
	/**
	 * Method that takes in a string, and for each character in that string, calls <code>increment</code>
	 * on the supplied SummaryByCycle object, that is keeping track of the number of occurrences of that particular
	 * character at that particular position in the string.
	 * <p>
	 *  If the offset is supplied and positive, then the process begins at that value rather than 0.
	 *   <p>
	 *  If the lengthMap is specified, then the length of the string (minus any offset) is passed to the
	 *  <code>incrementCount</code> object which keeps tabs on the number of times a string of that
	 *  length has occurred in the file.
	 *  <p>
	 * @param summary SummaryByCycle object that is tracking the summary data
	 * 
	 * @param dataString String containing the data that we wish to summarise
	 * <p>
	 * eg. for CS this will be along the lines of T0123012301230123
	 * 
	 * @param lengthMap Map<Integer, Integer> containing the number of times a string of a 
	 * particular length has occurred in the file- can be null
	 * 
	 * @param offset int representing where in the dataString we should start summarising from
	 */
	public static void parseCharacterSummary(SummaryByCycle<Character> summary,
			String dataString, ConcurrentMap<Integer, AtomicLong> lengthMap, int offset) {
		if (null != dataString) {
			
			int size = dataString.length();
			if (size > 0) {
				
				// set offset to 0 if it is negative, or larger than the supplied string
				if (offset < 0 || offset >= size)
					offset = 0;
				
				for (int i = 1 + offset ; i <= size; i++) {
					summary.increment(i - offset, dataString.charAt(i - 1));
				}
			}
			if (null != lengthMap) {
				incrementCount(lengthMap, size - offset);
			}
		}
	}
	
	public static void parseCharacterSummary(SummaryByCycleNew2<Character> summary,
			String dataString) {
		parseCharacterSummary(summary, dataString, null, 0);
	}
	public static void parseCharacterSummary(SummaryByCycleNew2<Character> summary,
			byte[] dataString) {
		int i = 1;
		for (byte b : dataString) {
			summary.increment(i++ , b);
		}
	}
	
	public static void parseCharacterSummary(final SummaryByCycleNew2<Character> summary,
			final byte[] dataString, final boolean reverse) {
		if (reverse) {
			for (int i = dataString.length -1 , j = 1; i >= 0 ; i--, j++) {
				summary.increment(j , BaseUtils.getComplement((char) dataString[i]));
			}
		} else {
			int i = 1;
			for (byte b : dataString) {
				summary.increment(i++ , b);
			}
		}
	}
	
	public static void parseCharacterSummary(SummaryByCycleNew2<Character> summary,
			String dataString, ConcurrentMap<Integer, AtomicLong> lengthMap, int offset) {
		if (null != dataString) {
			
			int size = dataString.length();
			if (size > 0) {
				
				// set offset to 0 if it is negative, or larger than the supplied string
				if (offset < 0 || offset >= size)
					offset = 0;
				
				for (int i = 1 + offset ; i <= size; i++) {
					summary.increment(i - offset, dataString.charAt(i - 1));
				}
			}
			if (null != lengthMap) {
				incrementCount(lengthMap, size - offset);
			}
		}
	}
	
	/**
	 * Method that updates by one the value in the Map relating to the \
	 * count of the number of occurrences of the supplied data <T>
	 * <p>
	 * If the Map does not contain the data item, then a new entry is inserted 
	 * into the Map for this data item
	 * 
	 * @param <T> Character or Integer in most cases
	 * @param map Map containing the number of times a particular data item has occurred 
	 * @param data T data that we are keeping a count of 
	 */
	public static <T> void incrementCount(ConcurrentMap<T, AtomicLong> map, T data) {

		if (null == map || null == data)
			throw new AssertionError("Null map or data found in SummaryByCycleUtils.incrementMap");
		
		AtomicLong currentCount = map.get(data);
		if (null == currentCount) {
			currentCount = map.putIfAbsent(data, new AtomicLong(1));
			if (null == currentCount)
				return;
		}
		currentCount.incrementAndGet();
	}
	
	
	public static <T> Map<Integer, AtomicLong> getLengthsFromSummaryByCycle(SummaryByCycle<T> summary, long totalSize) {
		Map<Integer, AtomicLong> map = Collections.emptyMap();
		
		if (null != summary && ! summary.cycles().isEmpty()) {
			long previousTally = totalSize;
			long count;
			map = new TreeMap<Integer, AtomicLong>();
			for (Integer integer : summary.cycles()) {
				count = 0;
				for (Entry<T, AtomicLong> entry : summary.getValue(integer).entrySet()) {
					count += entry.getValue().get();
				}
				if (count != previousTally) {
					// add entry to map with difference as the tally
					map.put(integer -1, new AtomicLong(previousTally - count));
					previousTally = count;
				}
			}
			// pop the last entry into the map
			map.put(summary.cycles().last(), new AtomicLong(previousTally));
		}
		return map;
	}
	
	public static <T> Map<Integer, AtomicLong> getLengthsFromSummaryByCycle(SummaryByCycleNew2<T> summary, long totalSize) {
		Map<Integer, AtomicLong> map = Collections.emptyMap();		
		if (null != summary && ! summary.cycles().isEmpty()) {
			long previousTally = totalSize;
			long count;
			map = new TreeMap<Integer, AtomicLong>();
			for (Integer integer : summary.cycles()) {
				count = 0;
				for (Entry<T, AtomicLong> entry : summary.getValue(integer).entrySet()) {
					count += entry.getValue().get();
				}
				if (count != previousTally) {
					// add entry to map with difference as the tally
					map.put(integer -1, new AtomicLong(previousTally - count));
					previousTally = count;
				}
			}
			// pop the last entry into the map
			map.put(summary.cycles().last(), new AtomicLong(previousTally));
		}
		return map;
	}

}
