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
package org.qcmg.qprofiler.util;

import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.util.BaseUtils;
import org.qcmg.qprofiler.summarise.PositionSummary;
import org.qcmg.qprofiler.summarise.SummaryByCycle;
import org.qcmg.qprofiler.summarise.SummaryByCycleNew2;
import org.qcmg.qvisualise.util.QProfilerCollectionsUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class SummaryReportUtils {

	public static final int MAX_I_SIZE = 50000;
	public static final int INITIAL_I_SIZE_BUCKET_SIZE = 10;
	public static final int FINAL_I_SIZE_BUCKET_SIZE = 1000000;
	public static final Pattern BAD_MD_PATTERN = Pattern.compile("([ACGTN])");	
	private static final Pattern badReadPattern = Pattern.compile("([.N])");	
	private static final NumberFormat nf = new DecimalFormat("0.0#%");	
	private static final QLogger logger = QLoggerFactory.getLogger(SummaryReportUtils.class);
	
	public static final String EMPTY = "EMPTY";
	public static final String TOTAL = "Total";
	public static final String UNKNOWN_READGROUP = "unkown_readgroup_id";
	public static final String All_READGROUP = "overall";
	
	

	/**
	 * Calls <code>lengthMapToXml(Element parent, String elementName,
	 * 		Map<T, Integer> mapOfLengths, String cycleElementName)</code> passing
	 * in "lineLength" as the <code>cycleElementName</code> value
	 * 
	 * @param <T>Character, String or Integer
	 * @param parent
	 *            Element representing the parent that the generated xml content
	 *            will be added to
	 * @param elementName
	 *            String representing the name that the top level element for
	 *            this map will be
	 * @param map
	 *            Map containing the data that we want in xml format
	 * @see org.qcmg.qprofiler.util.SummaryReportUtils#lengthMapToXml(Element,
	 *      String, Map, String)
	 */
	public static <T> void lengthMapToXml(Element parent, String elementName,
			Map<T, AtomicLong> map, Comparator<T> comparator) {
		
		Document doc = parent.getOwnerDocument();
		Element element = doc.createElement(elementName);
		parent.appendChild(element);		
		lengthMapToXmlTallyItem(element, "ValueTally", map, comparator);
	}
	
	public static <T> void lengthMapToXml(Element parent, String elementName, Map<T, AtomicLong> map) {
		lengthMapToXml(parent, elementName, map, null);
	}
	
	//Xu code
//	public static <T> void ToXmlWithoutPercentage(Element parent, String elementName, Map<T, AtomicLong> map) {
//		//lengthMapToXml(parent, elementName, map, null);
//		Document doc = parent.getOwnerDocument();
//		Element element = doc.createElement(elementName);
//		parent.appendChild(element);
//				
//		if (null != map && ! map.isEmpty()) {		
//		// get keys and sort them
//			List<T> sortedKeys = new ArrayList<>(map.keySet());
//			Collections.sort(sortedKeys, null);
//			try {
//				for (T key : sortedKeys) {
//					Element cycleE = doc.createElement("TallyItem");
//					AtomicLong ml = map.get(key);					
//					cycleE.setAttribute("value", key.toString());
//					cycleE.setAttribute("count", ml.get()+"");					
//					element.appendChild(cycleE);
//				}
//			} catch (DOMException e) {
//				e.printStackTrace();
//			}
//		}
//	}

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
	
//	public static <T> void lengthMapToXml(Element parent, String elementName,
//			AtomicLongArray array) {
//		Map<Integer, AtomicLong> map = new TreeMap<Integer, AtomicLong>();
//		
//		for (int i = 0 , length = array.length() ; i < length ; i++) {
//			if (array.get(i) > 0)
//				map.put(i, new AtomicLong(array.get(i)));
//		}
//		
//		Document doc = parent.getOwnerDocument();
//		Element element = doc.createElement(elementName);
//		parent.appendChild(element);
//		
//		lengthMapToXmlTallyItem(element, "ValueTally", map, null);
//	}
	public static <T> void lengthMapToXml(Element parent, String elementName,
			QCMGAtomicLongArray array) {
		Map<Integer, AtomicLong> map = new TreeMap<Integer, AtomicLong>();
		long length = array.length();
		for (int i = 0 ; i < length ; i++) {
			if (array.get(i) > 0)
				map.put(i, new AtomicLong(array.get(i)));
		}
		
		Document doc = parent.getOwnerDocument();
		Element element = doc.createElement(elementName);
		parent.appendChild(element);
		
		lengthMapToXmlTallyItem(element, "ValueTally", map, null);
	}
	
	public static <T> void lengthMapToXmlTallyItem(Element parent, 
			String elementName, Map<T, AtomicLong> mapOfLengths, Comparator<T> comparator) {
		
		if (null != mapOfLengths && ! mapOfLengths.isEmpty()) {
			Document doc = parent.getOwnerDocument();
			Element element = doc.createElement(elementName);
			parent.appendChild(element);
			
			long mapTotal = getCountOfMapValues(mapOfLengths);
			
			// get keys and sort them
			List<T> sortedKeys = new ArrayList<>(mapOfLengths.keySet());
			sortedKeys.sort(comparator);
			
			int counter = 0;
			try {
				for (T key : sortedKeys) {
					if (++counter % 1000000 == 0) {
						logger.info("added " + (counter / 1000000) + "M entries to Document");
					}
					Element cycleE = doc.createElement("TallyItem");
					AtomicLong ml = mapOfLengths.get(key);
					double percentage = (((double)ml.get() / mapTotal));
					cycleE.setAttribute("value", key.toString());
					cycleE.setAttribute("count", ml.get()+"");
					cycleE.setAttribute("percent", nf.format(percentage));
					element.appendChild(cycleE);
				}
			} catch (DOMException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static <T> void lengthMapToXmlTallyItem(Element parent, 
			String elementName, Map<T, AtomicLong> mapOfLengths) {
		lengthMapToXmlTallyItem(parent, elementName, mapOfLengths, null);
	}
	
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
	 * Adds an xml representation of the current object to the supplied parent element.
	 * 
	 * @param parent Element that the current objects xml representation will be added to
	 * @param elementName String representing the name to be used when creating the element
	 * @return 
	 */
	public static <T> void toXmlWithPercentage(SummaryByCycleNew2<T> sumByCycle,  Element parent, String elementName , QCMGAtomicLongArray percentageArray, long total) {
		final NumberFormat nf = new DecimalFormat("0.0#%");
		Document doc = parent.getOwnerDocument();
		Element element = doc.createElement(elementName);
		parent.appendChild(element);
		
		// adding another level to conform to DTD..
		Element cycleTallyElement = doc.createElement("CycleTally");
		element.appendChild(cycleTallyElement);
		cycleTallyElement.setAttribute("possibleValues", sumByCycle.getPossibleValuesAsString());
		Element possValuesE = doc.createElement("PossibleValues");
		for (T t : sumByCycle.getPossibleValues()) {
			Element valueE = doc.createElement("Value");
			valueE.setAttribute("value", t.toString());
			possValuesE.appendChild(valueE);
		}
		possValuesE.setAttribute("possibleValues", sumByCycle.getPossibleValuesAsString());
		cycleTallyElement.appendChild(possValuesE);
		
		try {
			long count = total;
			for (Integer cycle : sumByCycle.cycles()) {
				long mapTotal = getCountOfMapValues(sumByCycle.getValue(cycle));
				double percentage = (((double) mapTotal / count));
				AtomicLong ml = new AtomicLong(percentageArray.get(cycle));
				if (null != ml) {
					count -= ml.get();
				}
				
				Element cycleE = doc.createElement("Cycle");
				cycleE.setAttribute("value", cycle.toString());
				cycleTallyElement.appendChild(cycleE);
				for (Long value : sumByCycle.values(cycle)) {
					Element tallyE = doc.createElement("TallyItem");
					tallyE.setAttribute("value", ""+(char)value.intValue());
					tallyE.setAttribute("count", sumByCycle.count(cycle, value).get()+"");
					tallyE.setAttribute("percent", nf.format(percentage));
					cycleE.appendChild(tallyE);
				}
			}
		} catch (DOMException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Displays the supplied map of value/count pairs in xml format, attaching
	 * the new element to the supplied parent element
	 * 
	 * @param <T>
	 *            Character, String or Integer
	 * @param parent
	 *            Element representing the parent that the generated xml content
	 *            will be added to
	 * @param elementName
	 *            String representing the name that the top level element for
	 *            this map will be
	 * @param map
	 *            Map containing the data that we want in xml format
	 * @param cycleElementName
	 *            String representing the name given to each of the map.entry
	 *            values
	 */
	public static void binnedLengthMapToRangeTallyXml(Element parent, Map<Integer, AtomicLong> map) {
	
		Document doc = parent.getOwnerDocument();
		Element element = doc.createElement("RangeTally");
		parent.appendChild(element);
		
		// sort the map
		Map <Integer, AtomicLong> sortedMap = new TreeMap<Integer, AtomicLong>(map);
		
		try {
			for (Map.Entry<Integer, AtomicLong> entrySet : sortedMap.entrySet()) {
				Element cycleE = doc.createElement("RangeTallyItem");
				Integer start = entrySet.getKey();
				cycleE.setAttribute("start", start.toString());
				
				int endValue = start.intValue();
				if (start.intValue() == MAX_I_SIZE) {
					endValue = FINAL_I_SIZE_BUCKET_SIZE - 1;
				} else if  (start.intValue() > MAX_I_SIZE) {
					endValue = start.intValue() + (FINAL_I_SIZE_BUCKET_SIZE - 1);
				}
				
				cycleE.setAttribute("end", "" + endValue);
				cycleE.setAttribute("count", "" + entrySet.getValue().get());
				element.appendChild(cycleE);
			}
		} catch (DOMException e) {
			e.printStackTrace();
		}
	}
//	public static void binnedLengthMapToRangeTallyXml(Element parent, QCMGAtomicLongArray array) {
//		
//		Document doc = parent.getOwnerDocument();
//		Element element = doc.createElement("RangeTally");
//		parent.appendChild(element);
//		
//		try {
//			long length = array.length();
//			for (int i = 0 ; i < length ; i++) {
//				if (array.get(i) > 0) {
//					Element cycleE = doc.createElement("RangeTallyItem");
//					cycleE.setAttribute("start", "" + i);
//					int endValue = i < MAX_I_SIZE ? 
//							i + (INITIAL_I_SIZE_BUCKET_SIZE - 1) : 
//								i + (FINAL_I_SIZE_BUCKET_SIZE - 1); 
//							cycleE.setAttribute("end", "" + endValue);
//							cycleE.setAttribute("count", "" + array.get(i));
//							element.appendChild(cycleE);
//				}
//			}
//		} catch (DOMException e) {
//			e.printStackTrace();
//		}
//	}
	
//	public static void binnedLengthMapToRangeTallyXml(Element parent, QCMGAtomicLongArray array, QCMGAtomicLongArray array2) {
//		
//		Document doc = parent.getOwnerDocument();
//		Element element = doc.createElement("RangeTally");
//		parent.appendChild(element);
//		
//		try {
//			long length = array.length();
//			for (int i = 0 ; i < length ; i++) {
//				if (array.get(i) > 0) {
//					Element cycleE = doc.createElement("RangeTallyItem");
//					cycleE.setAttribute("start", "" + i);
//					int endValue = i < MAX_I_SIZE ? 
//							i + (INITIAL_I_SIZE_BUCKET_SIZE - 1) : 
//								i + (FINAL_I_SIZE_BUCKET_SIZE - 1); 
//							cycleE.setAttribute("end", "" + endValue);
//							cycleE.setAttribute("count", "" + array.get(i));
//							element.appendChild(cycleE);
//				}
//			}
//			length = array2.length();
//			for (int i = 0 ; i < length ; i++) {
//				if (array2.get(i) > 0) {
//					Element cycleE = doc.createElement("RangeTallyItem");
//					cycleE.setAttribute("start", "" + i);
//					int endValue = i < MAX_I_SIZE ? 
//							i + (INITIAL_I_SIZE_BUCKET_SIZE - 1) : 
//								i + (FINAL_I_SIZE_BUCKET_SIZE - 1); 
//							cycleE.setAttribute("end", "" + endValue);
//							cycleE.setAttribute("count", "" + array2.get(i));
//							element.appendChild(cycleE);
//				}
//			}
//		} catch (DOMException e) {
//			e.printStackTrace();
//		}
//	}
	
	/**
	 * Same as <code>SummaryReportUtils.tallyQualScores</code> but operates on
	 * an ASCII string, that does not have a separator
	 * 
	 * @param data String containing the data to be examined. ASCII encoded.
	 * @param map Map containing the tally
	 * @param offset int pertaining to the ASCII offset
	 * @see org.qcmg.qprofiler.util.SummaryReportUtils#tallyQualScores(String, Map, String)
	 */
	public static void tallyQualScoresASCII(String data,
			ConcurrentMap<Integer, AtomicLong> map, int offset) {
		
		if (null != data) {
			int countUnderTen = 0;
			for (int i = 0, size = data.length() ; i < size ; i++) {
				if (data.charAt(i) - offset < 10)
					countUnderTen++;
			}
			SummaryByCycleUtils.incrementCount(map, countUnderTen);
		}
	}
	
	public static void tallyQualScores(byte [] data,
			ConcurrentMap<Integer, AtomicLong> map) {
		
		if (null != data) {
			int countUnderTen = 0;
			for (byte b : data) {
				if ((b & 0xFF) < 10)
					countUnderTen++;
			}
			SummaryByCycleUtils.incrementCount(map, countUnderTen);
		}
	}
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
	public static void tallyQualScores(byte [] data, QCMGAtomicLongArray array) {
		if (null != data) {
			int countUnderTen = 0;
			for (byte b : data) {
				if ((b & 0xFF) < 10)
					countUnderTen++;
			}
			array.increment(countUnderTen);
		}
	}

	/**
	 * Updates the tally of qual scores on the supplied map. </br> The input
	 * data string is split by the separator, and a count of base qualities that
	 * have values below 10 is kept for each read. Then calls
	 * <code>org.qcmg.qprofiler.util.SummaryByCycleUtils.incrementCount(map, count)</code>
	 * 
	 * @param data
	 *            String containing the data to be examined. Must be seperated
	 *            be the supplied separator String.
	 * @param map
	 *            Map containing the tally
	 * @param separator
	 *            String indicating the separator for the data String. Must be provided.
	 * @see org.qcmg.qprofiler.util.SummaryByCycleUtils#incrementCount(Map,
	 *      Object)
	 */
	public static void tallyQualScores(String data, ConcurrentMap<Integer, AtomicLong> map,
			String separator) {
		
		if (null != data && null != separator) {
			int countUnderTen = 0;
			String[] quals = data.split(separator);
			for (int i = 0, size = quals.length ; i < size ; i++) {
				if (quals[i].length() > 0) {
					if (Integer.parseInt(quals[i]) < 10)
						countUnderTen++;
				}
			}
			SummaryByCycleUtils.incrementCount(map, countUnderTen);
		}
	}

	/**
	 * Updates the tally of bad reads on the supplied map.</br> A count of the
	 * number of times '.' or 'N' occurs in the input String is made for each
	 * read, and then the map is updated with this info using
	 * <code>org.qcmg.qprofiler.util.SummaryByCycleUtils.incrementCount(map, count)</code>
	 * 
	 * @param data
	 *            String data that is being checked for bad reads ('.' or 'N')
	 * @param map
	 *            Map containing the tally
	 * @see org.qcmg.qprofiler.util.SummaryByCycleUtils#incrementCount(Map,
	 *      Object)
	 * @deprecated
	 */
	@Deprecated
	public static void tallyBadReads(String data, ConcurrentMap<Integer, AtomicLong> map, Pattern pattern) {
		if (null != data) {
			Matcher m = pattern.matcher(data);
			int count = 0;
			while (m.find()) {
				count++;
			}
			SummaryByCycleUtils.incrementCount(map, Integer.valueOf(count));
		}
	}
	
	/**
	 * @deprecated
	 * @param data
	 * @param map
	 */
	@Deprecated
	public static void tallyBadReads(String data, ConcurrentMap<Integer, AtomicLong> map) {
		tallyBadReads(data, map, badReadPattern);
	}
	
	/**
	 * Updates the tally of bad reads on the supplied map.</br> A count of the
	 * number of times '.' or 'N' occurs in the input String is made for each
	 * read, and then the map is updated with this info using
	 * <code>org.qcmg.qprofiler.util.SummaryByCycleUtils.incrementCount(map, count)</code>
	 * 
	 * @param data String data that is being checked for bad reads ('.' or 'N')
	 * @param map Map containing the tally
	 * @see org.qcmg.qprofiler.util.SummaryByCycleUtils#incrementCount(Map, Object)
	 */
	public static void tallyBadReadsAsString(String data, ConcurrentMap<Integer, AtomicLong> map) {
		if (null != data) {
			int count = 0;
			for (int i = 0, size = data.length() ; i < size ; i++) {
				if (isInValid(data.charAt(i)))
					count++;
			}
			SummaryByCycleUtils.incrementCount(map, Integer.valueOf(count));
		}
	}
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
	public static void tallyBadReadsAsString(String data, QCMGAtomicLongArray array) {
		if (null != data) {
			int count = 0;
			for (int i = 0, size = data.length() ; i < size ; i++) {
				if (isInValid(data.charAt(i)))
					count++;
			}
			array.increment(count);
		}
	}
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
	
	public static void tallyBadReadsMD(String data, ConcurrentMap<String, AtomicLong> map) {
		if (null != data) {
			int misMatchCount = 0;
			boolean deletion = false;
			for (int i = 0, size = data.length() ; i < size ; ) {
				
				if ('^' == data.charAt(i)) {
					deletion = true;
					i++;
				} else if (isInValidExtended(data.charAt(i))) {
					
					misMatchCount++;
					while (++i < size && isInValidExtended(data.charAt(i))) {
						misMatchCount++;
					}
					SummaryByCycleUtils.incrementCount(map, misMatchCount + (deletion ? "D" : "M"));
					misMatchCount = 0;
					deletion = false;
				} else i++;
			}
		}
	}
	
	
	public static void tallyMDMismatches(final String mdData, final SummaryByCycleNew2<Character> summary, 
			final byte[] readBases, final boolean reverse, QCMGAtomicLongArray mdRefAltLengthsForward, 
			QCMGAtomicLongArray mdRefAltLengthsReverse) {
		
		if (null != mdData) {
		
			int readLength = readBases.length;
			if (readLength == 0) return;	// secondary alignments can have * as their sequence (which picard doesn't seem to report), which we can't do much with
		
			boolean deletion = false;
			if (reverse) {
				
				int position = 1;
				for (int i = 0, size = mdData.length() ; i < size ; ) {
					
					if (Character.isDigit(mdData.charAt(i))) {
						
						int numberLength = 1;
						while (++i < size && Character.isDigit(mdData.charAt(i))) {
							numberLength++;
						}
						position += Integer.parseInt(mdData.substring(i-numberLength, i));
						
					} else if ('^' == mdData.charAt(i)) {
						deletion = true;
						i++;
					} else if (isInValidExtended(mdData.charAt(i))) {
						// got a letter - update summary with position
						if (! deletion) {
							summary.increment(readLength - position + 1, BaseUtils.getComplement((char)readBases[position-1]));
						
							mdRefAltLengthsReverse.increment(getIntFromChars(BaseUtils.getComplement(mdData.charAt(i)), BaseUtils.getComplement((char)readBases[position-1])));
							i++;
							position++;
						} else {
							while (++i < size && isInValidExtendedInDelete(mdData.charAt(i))) {}
						}
						deletion = false;
					} else i++;	// need to increment this or could end up with infinite loop...
				}
				
			} else {
				
				int position = 1;
				for (int i = 0, size = mdData.length() ; i < size ; ) {
				
					if (Character.isDigit(mdData.charAt(i))) {
						
						int numberLength = 1;
						while (++i < size && Character.isDigit(mdData.charAt(i))) {
							numberLength++;
						}
						position += Integer.parseInt(mdData.substring(i-numberLength, i));
						
					} else if ('^' == mdData.charAt(i)) {
						deletion = true;
						i++;
					} else if (isInValidExtended(mdData.charAt(i))) {
						// got a letter - update summary with position
						if (! deletion) {
							
							summary.increment(position, (char)readBases[position-1]);
							mdRefAltLengthsForward.increment(getIntFromChars(mdData.charAt(i), (char)readBases[position-1]));
							i++;
							position++;
						} else {
							while (++i < size && isInValidExtendedInDelete(mdData.charAt(i))) {}
						}
						deletion = false;
					} else i++;	// need to increment this or could end up with infinite loop...
				}
			}
		}
	}
	
//	public static void tallyMDMismatches(final String mdData, Cigar cigar, final SummaryByCycleNew2<Character> summary, 
	public static String tallyMDMismatches(final String mdData, Cigar cigar, final SummaryByCycleNew2<Character> summary, 
			final byte[] readBases, final boolean reverse, QCMGAtomicLongArray mdRefAltLengthsForward, 
			QCMGAtomicLongArray mdRefAltLengthsReverse) {
		
		String errMessage = null; 
		
		if (null != mdData) {		
			int readLength = readBases.length;
			// secondary alignments can have * as their sequence (which picard doesn't seem to report), which we can't do much with
			if (readLength == 0) 
				return  errMessage;	
		
			boolean deletion = false;
			if (reverse) {
				
				int position = 1;
				for (int i = 0, size = mdData.length() ; i < size ; ) {
					
					if (Character.isDigit(mdData.charAt(i))) {
						
						int numberLength = 1;
						while (++i < size && Character.isDigit(mdData.charAt(i))) {
							numberLength++;
						}
						position += Integer.parseInt(mdData.substring(i-numberLength, i));
						
					} else if ('^' == mdData.charAt(i)) {
						deletion = true;
						i++;
					} else if (isInValidExtended(mdData.charAt(i))) {
						// got a letter - update summary with position
						if (! deletion) {							
							// check cigar to see if we need to adjust our offset due to insertions etc
							int additionalOffset = getInsertionAdjustedReadOffset(cigar, position);
							char readBase = BaseUtils.getComplement((char)readBases[position-1 + additionalOffset]);
							char refBase = BaseUtils.getComplement(mdData.charAt(i));
 						
							if (refBase == readBase) { 
								errMessage =  "Found refBase == altBase, md: " + mdData + " , cigar: " + cigar.toString() + ", seq: " + new String(readBases) + ", reverse strand: " +reverse; 
								System.out.println("Found refBase == altBase at position in md string " + (i+1) + " ,refBase: " + refBase + ", md: " + mdData + " , cigar: " + cigar.toString() + ", seq: " + new String(readBases) + ", reverse strand: " +reverse);
							}
							summary.increment(readLength - position + 1, readBase);
							int intFromChar =getIntFromChars(refBase, readBase);
							mdRefAltLengthsReverse.increment(intFromChar);
							i++;
							position++;
						} else {
							while (++i < size && isInValidExtendedInDelete(mdData.charAt(i))) {}
						}
						deletion = false;
					} else i++;	// need to increment this or could end up with infinite loop...
				}				
			} else {
				
				int position = 1;
				for (int i = 0, size = mdData.length() ; i < size ; ) {				
					if (Character.isDigit(mdData.charAt(i))) {						
						int numberLength = 1;
						while (++i < size && Character.isDigit(mdData.charAt(i))) {
							numberLength++;
						}
						position += Integer.parseInt(mdData.substring(i-numberLength, i));
						
					} else if ('^' == mdData.charAt(i)) {
						deletion = true;
						i++;
					} else if (isInValidExtended(mdData.charAt(i))) {
						// got a letter - update summary with position
						if (! deletion) {
							
							// check cigar to see if we need to adjust our offset due to insertsions etc
							int additionalOffset = getInsertionAdjustedReadOffset(cigar, position);
							char readBase = (char)readBases[position-1 + additionalOffset];
							char refBase = mdData.charAt(i);
							if (refBase == readBase) { 
								errMessage =  "Found refBase == altBase, md: " + mdData + " , cigar: " + cigar.toString() + ", seq: " + new String(readBases) + ", reverse strand: " +reverse; 
								System.out.println("Found refBase == altBase at ref position " + (i+1) + " ,refBase: " + refBase + ", md: " + mdData + " , cigar: " + cigar.toString() + ", seq: " + new String(readBases) + ", reverse strand: " +reverse);
							}
									
							summary.increment(position, readBase);
							
							int intFromChar =getIntFromChars(refBase, readBase);
							mdRefAltLengthsForward.increment(intFromChar);
							i++;
							position++;
						} else {
							while (++i < size && isInValidExtendedInDelete(mdData.charAt(i))) {}
						}
						deletion = false;
					} else i++;	// need to increment this or could end up with infinite loop...
				}
			}
			
		}
		return errMessage;
	}
	
	public static int getInsertionAdjustedReadOffset(Cigar cigar, int i) {
		int offset = 0, rollingLength = 0;
		for (CigarElement ce : cigar.getCigarElements()) {
			CigarOperator co = ce.getOperator();
			
			// Match/mismatch
			if (co.consumesReadBases() && co.consumesReferenceBases()) {
				rollingLength += ce.getLength();
			} else if (co.consumesReadBases()) {
				offset += ce.getLength();
			} else if (co.consumesReferenceBases()) {
//				rollingLength += ce.getLength();
			}
			if (rollingLength >= i) {
				break;
			}
			
		}
		return offset;
	}
	
	public static void tallyMDMismatches(String mdData, SummaryByCycle<Character> summary, String readBases) {
		if (null != mdData) {
			boolean deletion = false;
			int position = 1;
			for (int i = 0, size = mdData.length() ; i < size ; ) {
				
				if (Character.isDigit(mdData.charAt(i))) {
					
					int numberLength = 1;
					while (++i < size && Character.isDigit(mdData.charAt(i))) {
						numberLength++;
					}
					position += Integer.parseInt(mdData.substring(i-numberLength, i));
					
				} else if ('^' == mdData.charAt(i)) {
					deletion = true;
					i++;
				} else if (isInValidExtended(mdData.charAt(i))) {
					// got a letter - update summary with position
					if (! deletion) {
						summary.increment(position, readBases.charAt(position-1));
						i++;
						position++;
					} else {
						while (++i < size && isInValidExtendedInDelete(mdData.charAt(i))) {}
					}
					deletion = false;
				} else i++;	// need to increment this or could end up with infinite loop...
			}
		}
	}
	
	private static boolean isInValid(char c) {
		return c == '.' || c == 'N';
	}
	private static boolean isInValidExtended(char c) {
		return c == 'A' || c == 'C' || c == 'G' || c == 'T' || c == 'N';
	}
	private static boolean isInValidExtendedInDelete(char c) {
		if (! isInValidExtended(c))
			return c == 'M' || c =='R';
		else return true;
	}
	
	/**
	 * 
	 * @param map
	 * @param position
	 * @param length
	 */
	public static void addPositionAndLengthToMap(ConcurrentMap<Integer, AtomicLong> map, int position, int length) {
		for (int i = position ; i < position + length ; i++) {
			SummaryByCycleUtils.incrementCount(map, Integer.valueOf(i));
		}
	}
	
	public static void bamHeaderToXml(Element parent, String header) {
		Document doc = parent.getOwnerDocument();
		Text element = doc.createCDATASection(header);
		parent.appendChild(element);
		
	}
	
	/**
	 * Converts a map containing values and their corresponding counts into a Map that bins the values 
	 * into ranges determined by the supplied noOfBins value and the max value in the original map, 
	 * and the values are the summed values of all entries in the original map that fall within the range
	 * <br>
	 * Note that the supplied Map needs to be of type TreeMap as the method relies on the map being ordered.
	 * <br>
	 * The returned Map contains a string as its key which corresponds to the range (eg. 0-100)
	 * <br>
	 * <b>Note it is assumed that the lowest key value is 0</b><br>
	 *  ie. this will <b>not</b> work when there are negative values in the original map
	 * 
	 * @param map TreeMap map containing Integer keys and values, whose values are to be binned
	 * @param binSize int corresponding to the number of bins that are required. The range each bin will have is dependent on the max number
	 * @return Map of String, Integer pairs relating to the range, and number within that range
	 * @deprecated Use {@link QProfilerCollectionsUtils#convertMapIntoBinnedMap(Map<Integer, AtomicLong>,int,boolean)} instead
	 */
	@Deprecated
	public static Map<String, AtomicLong> convertMapIntoBinnedMap(Map<Integer, AtomicLong> map, int binSize, boolean startFromZero) {
		return QProfilerCollectionsUtils.convertMapIntoBinnedMap(map, binSize,
				startFromZero);
	}
	
	public static <T> void postionSummaryMapToXml(Element parent, String elementName,
			Map<String, PositionSummary> mapOfPositions, List<String> readGroupIds) {	
		
		//get floor and celling value of coverage for qvisualize
		long coverageTotal = 0; 
		int binTotal = 0; 
		for ( PositionSummary summary : mapOfPositions.values()){
			coverageTotal += summary.getMaxRgCoverage();
			binTotal += summary.getBinNumber();
		}
		
		if(binTotal == 0) binTotal ++; //avoid exception for unit test
		int floorValue =  (int) ( coverageTotal / (binTotal*4)); 
		
		int overFloorBin = 0; 
		for ( PositionSummary summary : mapOfPositions.values())
			overFloorBin += summary.getBigBinNumber(floorValue);
		
//			for( AtomicLongArray bin : summary.getRgCoverage().values()){			
//				for(int i = 0, sum = 0; i < bin.length(); i++){
//					sum += bin.get(i);
//					if(sum > floorValue ){ overFloorBin ++; break; }
//				}
//			}
		
		if(overFloorBin == 0) overFloorBin ++;
		int cellingValue = (int) (2.5 * coverageTotal / overFloorBin );
				
		Document doc = parent.getOwnerDocument();
		Element element = doc.createElement(elementName);
		parent.appendChild(element);
		
		// sort map
		Map<String, PositionSummary> sortedMap = new TreeMap<String, PositionSummary>(new ReferenceNameComparator());
		sortedMap.putAll( mapOfPositions );
				
		try {
			for (Map.Entry<String, PositionSummary> entrySet : sortedMap.entrySet()) {
				Element rNameE = doc.createElement("RNAME");
				rNameE.setAttribute("value", entrySet.getKey());
				PositionSummary ps = entrySet.getValue();
				rNameE.setAttribute("minPosition", "" + ps.getMin() );
				rNameE.setAttribute("maxPosition", "" + ps.getMax() );
				rNameE.setAttribute("count", "" + ps.getTotalCount() );
				//rNameE.setAttribute("maxAveOfReadGroup", "" + ps.getAveOfMax() );
				rNameE.setAttribute("visuallCellingValue", "" + cellingValue  );
				rNameE.setAttribute("readGroups", Arrays.toString(readGroupIds.toArray()).replace("[", "").replace("]", "").replace(" ", "") );
				//debug
				rNameE.setAttribute("debugfloorValue", ""+floorValue) ;
				rNameE.setAttribute("debugcoverageTotal", ""+coverageTotal) ;
				rNameE.setAttribute("debugbinTotal", ""+binTotal) ;
				rNameE.setAttribute("debugoverFloorBin", ""+overFloorBin) ;
				
				element.appendChild(rNameE);
				
				Element rangeTallyE = doc.createElement( "RangeTally" );
				rNameE.appendChild(rangeTallyE);
								
				// insert map of coverage here
				Map<Integer, AtomicLong> coverage = ps.getCoverage();
				for (Entry<Integer, AtomicLongArray> entry : ps.getRgCoverage().entrySet()) {
					Element tallyE = doc.createElement("RangeTallyItem");
					tallyE.setAttribute("start", "" + entry.getKey() * PositionSummary.BUCKET_SIZE  ); 
					tallyE.setAttribute("end", "" + ((entry.getKey() + 1) * PositionSummary.BUCKET_SIZE  - 1));					
					tallyE.setAttribute("rgCount", entry.getValue().toString().replace("[", "").replace("]", "").replace(" ", "") );//remove space, [ ]
					tallyE.setAttribute("count", "" + coverage.get(entry.getKey()) );
					rangeTallyE.appendChild( tallyE );
				}
			}
		} catch (DOMException e) {
			e.printStackTrace();
		}
	}
	
	public static int getIntFromChars(final char ref, final char alt) {
//		if (ref == alt) System.out.println("REF == ALT!!! : ref: " + ref + ", alt: " + alt);
		switch (ref) {
		case 'A':
			return 'A' == alt ? 1 : ('C' == alt ? 2 : ('G' == alt ? 3 : ('T' == alt ? 4 : 5)));
		case 'C':
			return 'A' == alt ? 6 : ('C' == alt ? 7 : ('G' == alt ? 8 : ('T' == alt ? 9 : 10)));
		case 'G':
			return 'A' == alt ? 11 : ('C' == alt ? 12 : ('G' == alt ? 13 : ('T' == alt ? 14 : 15)));
		case 'T':
			return 'A' == alt ? 16 : ('C' == alt ? 17 : ('G' == alt ? 18 : ('T' == alt ? 19 : 20)));
		case 'N':
			return 'A' == alt ? 21 : ('C' == alt ? 22 : ('G' == alt ? 23 : ('T' == alt ? 24 : 25)));
		}
		return -1;
	}
	
	public static String getStringFromInt(final int i) {
		switch (i) {
		// A's
		case 1: return "A>A";
		case 2: return "A>C";
		case 3: return "A>G";
		case 4: return "A>T";
		case 5: return "A>N";
		
		//C's
		case 6: return "C>A";
		case 7: return "C>C";
		case 8: return "C>G";
		case 9: return "C>T";
		case 10: return "C>N";
		
		//G's
		case 11: return "G>A";
		case 12: return "G>C";
		case 13: return "G>G";
		case 14: return "G>T";
		case 15: return "G>N";
		
		//T's
		case 16: return "T>A";
		case 17: return "T>C";
		case 18: return "T>G";
		case 19: return "T>T";
		case 20: return "T>N";
		
		//N's
		case 21: return "N>A";
		case 22: return "N>C";
		case 23: return "N>G";
		case 24: return "N>T";
		case 25: return "N>N";
		
		// hmmmm
		case -1: return "???";
		}
		return null;
	}
}
