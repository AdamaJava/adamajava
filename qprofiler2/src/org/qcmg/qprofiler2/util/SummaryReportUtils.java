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
package org.qcmg.qprofiler2.util;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.qcmg.common.model.QCMGAtomicLongArray;

public class SummaryReportUtils {

	/**
	 * 
	 * @param map of counts for each possible value at specified cycle
	 * @return sum of counts for all possible value at specified cycle
	 */
	public static <T> long getCountOfMapValues(Map<T, AtomicLong> map) {
		long count = 0;
		if (null != map) {
			for (AtomicLong ml : map.values()) {
				count += ml.get();
			}
		}
		return count;
	}
	
	/**
	 * count how many invalid char ('.' or 'N';) on a given read base array, then record the counts to the given array
	 * @param data: read base
	 * @param array: increment(number of invalid char)
	 */
	public static void tallyBadReadsAsString(byte[] data, QCMGAtomicLongArray array) {
		if (null != data) {
			int count = 0;
			for (byte b : data) {
				if (isInValid((char) b))
					count++;
			}
			array.increment(count);
		}
	}
	
	private static boolean isInValid(char c) { 
		return c == '.' || c == 'N';
	}
	
	/**
	 * how many quality base value is below then 10, then record this count to specified array
	 * @param data : base quality array, each base quality is acsii - 33; eg. string in read column 11th "()FF...", equal to "7,8,37,37..."
	 * 				FastqRecord.getBaseQualities() and samRecord.getBaseQualities() return array already -33
	 * @param array: an array to storre the bad base count for each bam record
	 */
	public static void tallyQualScores(byte [] data, QCMGAtomicLongArray array) {
		if (null != data) {
			int countUnderTen = 0;
			for (byte b : data) {
				//When you do a bitwise AND of 0xFF and any value from 0 to 255, the result is the exact same as the value. 
				//And if any value higher than 255 still the result will be within 0-255.
				if ((b & 0xFF) < 10)
					countUnderTen++;
			}
			array.increment(countUnderTen);
		}
	}
}
