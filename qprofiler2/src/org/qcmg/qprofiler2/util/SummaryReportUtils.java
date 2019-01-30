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
import org.apache.commons.lang3.ArrayUtils;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.util.BaseUtils;


public class SummaryReportUtils {
	 
//	public static final int MAX_I_SIZE = 50000;
//	public static final int INITIAL_I_SIZE_BUCKET_SIZE = 10;
//	public static final int FINAL_I_SIZE_BUCKET_SIZE = 1000000;
//	private static final NumberFormat nf = new DecimalFormat("0.0#%");	
//	private static final QLogger logger = QLoggerFactory.getLogger(SummaryReportUtils.class);
//	
//
//	/**
//	 * Calls <code>lengthMapToXml(Element parent, String elementName,
//	 * 		Map<T, Integer> mapOfLengths, String cycleElementName)</code> passing
//	 * in "lineLength" as the <code>cycleElementName</code> value
//	 * 
//	 * @param <T>Character, String or Integer
//	 * @param parent
//	 *            Element representing the parent that the generated xml content
//	 *            will be added to
//	 * @param elementName
//	 *            String representing the name that the top level element for
//	 *            this map will be
//	 * @param map
//	 *            Map containing the data that we want in xml format
//	 * @see org.qcmg.qprofiler2.util.SummaryReportUtils#lengthMapToXml(Element,
//	 *      String, Map, String)
//	 */
//
//
//	
//	/**
//	 * Same as <code>SummaryReportUtils.tallyQualScores</code> but operates on
//	 * an ASCII string, that does not have a separator
//	 * 
//	 * @param data String containing the data to be examined. ASCII encoded.
//	 * @param map Map containing the tally
//	 * @param offset int pertaining to the ASCII offset
//	 * @see org.qcmg.qprofiler2.util.SummaryReportUtils#tallyQualScores(String, Map, String)
//	 */
//	public static void tallyQualScoresASCII(String data,
//			ConcurrentMap<Integer, AtomicLong> map, int offset) {
//		
//		if (null != data) {
//			int countUnderTen = 0;
//			for (int i = 0, size = data.length() ; i < size ; i++) {
//				if (data.charAt(i) - offset < 10)
//					countUnderTen++;
//			}
//			SummaryByCycleUtils.incrementCount(map, countUnderTen);
//		}
//	}
//	
//	public static void tallyQualScores(byte [] data, ConcurrentMap<Integer, AtomicLong> map) {
//		
//		if (null != data) {
//			int countUnderTen = 0;
//			for (byte b : data) {
//				if ((b & 0xFF) < 10)
//					countUnderTen++;
//			}
//			SummaryByCycleUtils.incrementCount(map, countUnderTen);
//		}
//	}
//	public static void tallyQualScores(byte [] data, AtomicLongArray array) {
//		if (null != data) {
//			int countUnderTen = 0;
//			for (byte b : data) {
//				if ((b & 0xFF) < 10)
//					countUnderTen++;
//			}
//			array.incrementAndGet(countUnderTen);
//		}
//	}
//	/**
//	 * Updates the tally of qual scores on the supplied map. </br> The input
//	 * data string is split by the separator, and a count of base qualities that
//	 * have values below 10 is kept for each read. Then calls
//	 * <code>org.qcmg.qprofiler.util.SummaryByCycleUtils.incrementCount(map, count)</code>
//	 * 
//	 * @param data
//	 *            String containing the data to be examined. Must be seperated
//	 *            be the supplied separator String.
//	 * @param map
//	 *            Map containing the tally
//	 * @param separator
//	 *            String indicating the separator for the data String. Must be provided.
//	 * @see org.qcmg.common.util.SummaryByCycleUtils#incrementCount(Map,
//	 *      Object)
//	 */
//	public static void tallyQualScores(String data, ConcurrentMap<Integer, AtomicLong> map, String separator) {
//		
//		if (null != data && null != separator) {
//			int countUnderTen = 0;
//			String[] quals = data.split(separator);
//			for (int i = 0, size = quals.length ; i < size ; i++) {
//				if (quals[i].length() > 0) {
//					if (Integer.parseInt(quals[i]) < 10)
//						countUnderTen++;
//				}
//			}
//			SummaryByCycleUtils.incrementCount(map, countUnderTen);
//		}
//	}
//
//	
//	/**
//	 * Updates the tally of bad reads on the supplied map.</br> A count of the
//	 * number of times '.' or 'N' occurs in the input String is made for each
//	 * read, and then the map is updated with this info using
//	 * <code>org.qcmg.qprofiler.util.SummaryByCycleUtils.incrementCount(map, count)</code>
//	 * 
//	 * @param data String data that is being checked for bad reads ('.' or 'N')
//	 * @param map Map containing the tally
//	 * @see org.qcmg.common.util.SummaryByCycleUtils#incrementCount(Map, Object)
//	 */
//	public static void tallyBadReadsAsString(String data, ConcurrentMap<Integer, AtomicLong> map) {
//		if (null != data) {
//			int count = 0;
//			for (int i = 0, size = data.length() ; i < size ; i++) {
//				if (isInValid(data.charAt(i)))
//					count++;
//			}
//			SummaryByCycleUtils.incrementCount(map, Integer.valueOf(count));
//		}
//	}
//	public static void tallyBadReadsAsString(String data, AtomicLongArray array) {
//		if (null != data) {
//			int count = 0;
//			for (int i = 0, size = data.length() ; i < size ; i++) {
//				if (isInValid(data.charAt(i)))
//					count++;
//			}
//			array.incrementAndGet(count);
//		}
//	}
//	public static void tallyBadReadsAsString(String data, QCMGAtomicLongArray array) {
//		if (null != data) {
//			int count = 0;
//			for (int i = 0, size = data.length() ; i < size ; i++) {
//				if (isInValid(data.charAt(i)))
//					count++;
//			}
//			array.increment(count);
//		}
//	}
//
//	
//	public static ConcurrentMap<String, ConcurrentMap<Integer, AtomicLong>> binIsize(int binSize, Map<String, AtomicLongArray> iSizeByReadGroupMap, Map<String, QCMGAtomicLongArray> iSizeByReadGroupMapBinned) {
//		ConcurrentMap<String, ConcurrentMap<Integer, AtomicLong>> results = new ConcurrentHashMap<>();
//		for (Entry<String, AtomicLongArray> entry : iSizeByReadGroupMap.entrySet()) {
//			// add entry to results map of  maps
//			String readGroup = entry.getKey();
//			ConcurrentMap<Integer, AtomicLong> iSizeLengths = new ConcurrentHashMap<>();
//			results.putIfAbsent(readGroup, iSizeLengths);
//			
//			AtomicLongArray array = entry.getValue();
//			for (int i = 0 ; i < array.length() ; i++) {
//				long longValue = array.get(i);
//				if (longValue > 0) {
//				
//					AtomicLong al = iSizeLengths.get(i);
//					if (null == al) {
//						al = new AtomicLong();
//						AtomicLong existingLong = iSizeLengths.putIfAbsent(i, al);
//						if (null != existingLong) al = existingLong;
//					}
//					al.addAndGet(longValue);
//				}
//			}
//		}
//		
//		// now for the binned map
//		for (Entry<String, QCMGAtomicLongArray> entry : iSizeByReadGroupMapBinned.entrySet()) {
//			String readGroup = entry.getKey();
//			ConcurrentMap<Integer, AtomicLong> iSizeLengths = results.get(readGroup);
//			if (null == iSizeLengths) {
//				iSizeLengths = new ConcurrentHashMap<Integer, AtomicLong>();
//				ConcurrentMap<Integer, AtomicLong>  existingResults = results.putIfAbsent(readGroup, iSizeLengths);
//				if (null != existingResults) {
//					iSizeLengths = existingResults;
//				}
//			}
//			QCMGAtomicLongArray array = entry.getValue();
//			for (int i = 0 ; i < array.length() ; i++) {
//				long l = array.get(i);
//				if (l > 0) {
//					Integer binNumber = (i == 0 ? MAX_I_SIZE : i * 1000000);
//					AtomicLong al = iSizeLengths.get(binNumber);
//					if (null == al) {
//						al = new AtomicLong();
//						AtomicLong existingAL = iSizeLengths.putIfAbsent(binNumber, al);
//						if (null != existingAL) {
//							al = existingAL;
//						}
//					}
//					al.addAndGet(l);
//				}
//			}
//		}
//		return results;
//	}	
//		
	/**
	 * 
	 * @param dataString: a byte array of readbase
	 * @return reversed byte array and each base are complement. Eg. {'A','G', 'T'} => {'A','C', 'T'}
	 */
	public static byte[] getReversedSeq(byte[] dataString){		
	
		byte[] data = getReversedQual(dataString);		
	 	byte[] complementBases = new byte[data.length];				
		for(int i = 0; i < data.length; i++) 		 
			complementBases[i] = (byte) BaseUtils.getComplement( (char)data[i]);
		
		return complementBases ;		
	}
	
	/**
	 * 
	 * @param dataString: a byte array of Seq or qual
	 * @return reversed byte array
	 */
	public static byte[] getReversedQual(byte[] dataString){
		if(dataString == null) return null;
		byte[] data = dataString.clone();
		ArrayUtils.reverse(data );
		return data; 		
	}

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
