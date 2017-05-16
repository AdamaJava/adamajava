/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qvisualise.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

import org.qcmg.common.model.MAPQMiniMatrix;
import org.qcmg.common.model.SummaryByCycle;
import org.qcmg.common.util.Constants;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class QProfilerCollectionsUtils {
	
	/**
	 * Splits the flags collection into 
	 * @param <T>
	 * @param flags
	 * @param distinguisher
	 * @param distDesc
	 * @param nonDistDesc
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> Map<T, AtomicLong> splitFlagTallyByDistinguisher(Map<T, AtomicLong> flags, Map<String, T> distinguisherMap, T nonDistDesc) {
		Map<T, AtomicLong> splitFlags = new LinkedHashMap<>();
		
		// loop through flags, if <T> contains the specified value, add to value 
		for (Entry<T, AtomicLong> entry : flags.entrySet()) {
			final String[] flagSplit = ((String)entry.getKey()).split(", ");
			
			String flagSummaryString = "";
			if (flagSplit.length == 2) {
				flagSummaryString = flagSplit[1];
			}
			
			
			T value = null;
			for (Entry<String, T> distMapEntry : distinguisherMap.entrySet()) {
				if (flagSummaryString.contains(distMapEntry.getKey())) {
					value = distMapEntry.getValue();
				}
			}
			
			if (null == value)
				value =  null != nonDistDesc ? nonDistDesc : (T) "Other";
			
			final AtomicLong currentCount = splitFlags.get(value);
			
			if (null == currentCount) {
				splitFlags.put(value, new AtomicLong(entry.getValue().get()));
			} else {
				currentCount.addAndGet(entry.getValue().get());
			}
				
		}
		
		return splitFlags;
	}
	
	public static <T> SummaryByCycle<T> generateSummaryByCycleFromElement(Element element,
			String name) {

		ConcurrentMap<Integer, ConcurrentMap<T, AtomicLong>> tally = new ConcurrentHashMap<Integer, ConcurrentMap<T, AtomicLong>>();
		final NodeList nl = element.getElementsByTagName(name);
		final Element nameElement = (Element) nl.item(0);

		
		if (null != nameElement) {
			// now get the cycletally underneath..
			Element cycleTallyElement = null;
			if (nameElement.hasChildNodes()) {
				cycleTallyElement = (Element) nameElement.getElementsByTagName("CycleTally").item(0);
	
				// get the cycles
				final NodeList cycles = cycleTallyElement.getElementsByTagName("Cycle");
				for (int i = 0, size = cycles.getLength() ; i < size ; i++) {
					if (cycles.item(i) instanceof Element) {
						final Element cycleElement = (Element) cycles.item(i);
						
						// get tallyitems
						ConcurrentMap<T, AtomicLong> cycleCount = new ConcurrentHashMap<T, AtomicLong>();
						populateTallyItemMap(cycleElement, cycleCount, false);
						
						tally.put(Integer.parseInt(cycleElement.getAttribute("value")),
								cycleCount);
					}
				}
			}
		}

		return new SummaryByCycle<T>(tally);
	}
	
	public static Map<Integer, String> generatePercentagesMapFromElement(Element element,
			String name) {
		
		ConcurrentMap<Integer, String> tally = new ConcurrentHashMap<Integer, String>();
		final NodeList nl = element.getElementsByTagName(name);
		final Element nameElement = (Element) nl.item(0);
		
		
		if (null != nameElement) {
			// now get the cycletally underneath..
			Element cycleTallyElement = null;
			if (nameElement.hasChildNodes()) {
				cycleTallyElement = (Element) nameElement.getElementsByTagName("CycleTally").item(0);
				
				// get the cycles
				final NodeList cycles = cycleTallyElement.getElementsByTagName("Cycle");
				for (int i = 0, size = cycles.getLength() ; i < size ; i++) {
					if (cycles.item(i) instanceof Element) {
						final Element cycleElement = (Element) cycles.item(i);
						
						// get tallyitems
						final NodeList tallyItemsNL = cycleElement.getElementsByTagName("TallyItem");
						
						if (null != tallyItemsNL && tallyItemsNL.item(0) instanceof Element) {
							Element tallyItemElement = (Element) tallyItemsNL.item(0);
							String percent = null;
							if (tallyItemElement.getAttribute("percent").length() > 0) {
								percent = tallyItemElement.getAttribute("percent");
								// can't add null to ConcurrentHashMap
								tally.put(Integer.parseInt(cycleElement.getAttribute("value")), percent);
							}
						}
					}
				}
			}
		}
		return tally;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> void populateTallyItemMap(Element cycleElement,
			Map<T, AtomicLong> map, boolean isInteger) {
		if (null != cycleElement) {
			
			final NodeList tallyItemsNL = cycleElement.getElementsByTagName("TallyItem");
			
			for (int j = 0, size = tallyItemsNL.getLength() ; j < size ; j++) {
				if (tallyItemsNL.item(j) instanceof Element) {
					Element tallyItemElement = (Element) tallyItemsNL.item(j);
					if (tallyItemElement
							.getAttribute("count").length() > 0) {
						
						final long count = Long.parseLong(tallyItemElement
								.getAttribute("count"));
						
						map.put(isInteger ? (T) Integer.valueOf(tallyItemElement.getAttribute("value")) 
								: (T) tallyItemElement.getAttribute("value") , new AtomicLong(count));
												
					}
				}
			}
		}
	}
	
	public static void populateMatrixMap(Element cycleElement,
			Map<MAPQMiniMatrix, AtomicLong> map) {
		if (null != cycleElement) {
			NodeList tallyItemsNL = cycleElement.getElementsByTagName("TallyItem"); 
			for (int j = 0, size = tallyItemsNL.getLength() ; j < size ; j++) {
				Element tallyItemElement = (Element) tallyItemsNL.item(j);
				long count = Long.parseLong(tallyItemElement
						.getAttribute("count"));
				
				//TODO must be a better way of extracting the matrix details from the string
				String matrix = tallyItemElement.getAttribute("value");
				if (matrix.startsWith("MAPQMiniMatrix")) {
					String subString = matrix.substring(matrix.indexOf("["), matrix.indexOf("]"));
					String[] m = subString.split(",");
					int mapQ = Integer.parseInt(m[0].substring(m[0].indexOf("=")+1));
					int value = Integer.parseInt(m[1].substring(m[1].indexOf("=")+1));
					MAPQMiniMatrix mmm = new MAPQMiniMatrix(mapQ, value);
					
					map.put(mmm, new AtomicLong(count));
				}
				
			}
		}
	}
	
	public static AtomicLong tallyArrayValues(AtomicLongArray array) {
		long l = 0;
		for (int i = 0 , len = array.length() ; i < len ; i++) {
			l += array.get(i);
		}
		return new AtomicLong(l);
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
	 */
	public static Map<String, AtomicLong> convertMapIntoBinnedMap(Map<Integer, AtomicLong> map, int binSize, boolean startFromZero) {
		Map<String, AtomicLong> binnedMap = Collections.emptyMap();
		if (null != map && ! map.isEmpty()) {
			// if binSize is less than 1, set to 1 - will put all iSize values into the same bin
			// also avoids an ArithmeticException
			if (binSize < 1)
				binSize = 1;
			
			binnedMap = new LinkedHashMap<String, AtomicLong>(map.size() / binSize);
		
			// get max number from map - map contains absolute values so getting the last key should be safe
			int maxValue = ((SortedMap<Integer, AtomicLong>) map).lastKey();
			int minValue = ((SortedMap<Integer, AtomicLong>) map).firstKey();
			
			long count = 0;
			int fromPosition = startFromZero ? 0 : minValue;
			for (int i = fromPosition ; i <= maxValue ; i++) {
				
				AtomicLong mi = map.get(i);
				if (null != mi)
					count += mi.get();
				
				if ((i+1) % binSize == 0 || i == maxValue) {
					// add count to binnedMap
					binnedMap.put(fromPosition + " - " + i, new AtomicLong(count));
					fromPosition = i + 1;
					// reset count
					count = 0;
				}
			}
		}
		return binnedMap;
	}
	
	
	public static Map<String, List<String>> convertHeaderTextToMap(String headerText) {
		if (null == headerText) return Collections.emptyMap();
		
		String [] params = headerText.split("\n");
		 
		Map<String, List<String>> results = new LinkedHashMap<>(8,0.9f);
		
		//init the map for order
		String[] heads = {"Header","Sequence","Read Group","Program","Comments","Other"};
		for(String key : heads) {
			results.put(key, null);
		}
		
		if (params.length > 1) {
			for (String param : params) {
				
				String key = null;
				if (param.startsWith(Constants.HEADER_PREFIX)) {
					key = "Header";
				} else if (param.startsWith(Constants.SEQUENCE_PREFIX)) {
					key = "Sequence";
				} else if (param.startsWith(Constants.READ_GROUP_PREFIX)) {
					key = "Read Group";
				} else if (param.startsWith(Constants.PROGRAM_PREFIX)) {
					key = "Program";
				} else if (param.startsWith(Constants.COMMENT_PREFIX)) {
					key = "Comments";
				} else {
					key = "Other";
				}
				addDataToList(results, key, param);
			}
		} else {
			//FIXME split on '@' char, and add back in....
			// hack to get around line breaks not being preserved in CDATA sections
			// also check using UTF-16...
			params = headerText.split("@");
			if (params.length > 1) {
				for (String param : params) {
					if (param.trim().length() > 0) {
						String key = null;
						if (param.startsWith("HD")) {
							key = "Header";
						} else if (param.startsWith("SQ")) {
							key = "Sequence";
						} else if (param.startsWith("RG")) {
							key = "Read Group";
						} else if (param.startsWith("PG")) {
							key = "Program";
						} else if (param.startsWith("CO")) {
							key = "Comments";
						} else {
							key = "Other";
						}
						addDataToList(results, key, param);
					}
				}
			}
		}
				
		//remove entries in map that came from the heads array and that have null values
 		for (String key : heads) {
 			if (results.get(key) == null) {
 				results.remove(key);
 			}
 		}
 					
		return results;
	}
	
	private static void addDataToList(Map<String, List<String>> map, String key, String data) {
		map.computeIfAbsent(key, v -> new ArrayList<>()).add(data);
	}

}
