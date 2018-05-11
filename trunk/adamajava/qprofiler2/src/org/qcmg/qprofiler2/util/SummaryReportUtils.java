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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.util.BaseUtils;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.common.util.SummaryByCycleUtils;
import org.qcmg.qprofiler2.summarise.PositionSummary;
import org.qcmg.qprofiler2.summarise.ReadGroupSummary;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;


public class SummaryReportUtils {
	 
	public static final int MAX_I_SIZE = 50000;
	public static final int INITIAL_I_SIZE_BUCKET_SIZE = 10;
	public static final int FINAL_I_SIZE_BUCKET_SIZE = 1000000;
	private static final NumberFormat nf = new DecimalFormat("0.0#%");	
	private static final QLogger logger = QLoggerFactory.getLogger(SummaryReportUtils.class);
	

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
	 * @see org.qcmg.qprofiler2.util.SummaryReportUtils#lengthMapToXml(Element,
	 *      String, Map, String)
	 */
	public static <T> void lengthMapToXml(Element parent, String elementName, String sourceName, Map<T, AtomicLong> map, Comparator<T> comparator) {	
				
		Element element = (parent.getElementsByTagName(elementName).getLength() > 0)?
				(Element) parent.getElementsByTagName(elementName).item(0)  :QprofilerXmlUtils.createSubElement(parent, elementName);
		if( map.isEmpty()) return; 
		
		element = QprofilerXmlUtils.createSubElement(element, QprofilerXmlUtils.valueTally );		
		if(sourceName != null) element.setAttribute("source", sourceName);		
		lengthMapToXmlTallyItem(element, map, comparator);
	} 	
	
	public static <T> void lengthMapToXml(Element parent, String elementName, Map<T, AtomicLong> map) {		
		lengthMapToXml(  parent,  elementName, null,   map,  null);
	}
		
	public static <T> void lengthMapToXml(Element parent, String elementName,String sourceName, Map<T, AtomicLong> map) {
		lengthMapToXml(parent, elementName, sourceName, map, null);
	}
	
	public static <T extends QCMGAtomicLongArray, AtomicLongArray >  void lengthMapToXml(Element parent, String elementName, String sourceName, T array) { 				 
		Map<Integer, AtomicLong> map = new TreeMap<Integer, AtomicLong>();
		for (int i = 0, length = (int) array.length() ; i < length ; i++) {
			long count = array.get(i);					 			
			if (count > 0)	map.put( i, new AtomicLong(count) );
		}		
		lengthMapToXml(  parent,  elementName, sourceName, map,  null);
	}
	
	public static <T extends QCMGAtomicLongArray, AtomicLongArray >  void lengthMapToXml(Element parent, String elementName,  T array) { 				  
		lengthMapToXml(  parent,  elementName, null, array);
	}	
	

	public static <T> void lengthMapToXmlTallyItem(Element parent,  Map<T, AtomicLong> mapOfLengths, Comparator<T> comparator) {
		
		if (null != mapOfLengths && ! mapOfLengths.isEmpty()) {
			long mapTotal = getCountOfMapValues(mapOfLengths);
			if(mapTotal <= 0) return; 
			
			// get keys and sort them
			List<T> sortedKeys = new ArrayList<>(mapOfLengths.keySet());
			sortedKeys.sort( comparator ); 
			
			int counter = 0; 
			try {
				parent.setAttribute(QprofilerXmlUtils.totalCount , mapTotal+"");
				for ( T key : sortedKeys ) { 
					if (++counter % 1000000 == 0) 
						logger.info("added " + (counter / 1000000) + "M entries to Document");				 
					Element cycleE = parent.getOwnerDocument().createElement( QprofilerXmlUtils.tallyItem );
					AtomicLong ml = mapOfLengths.get(key);
					double percentage = (((double)ml.get() / mapTotal));
					cycleE.setAttribute(QprofilerXmlUtils.value  , key.toString());
					cycleE.setAttribute(QprofilerXmlUtils.count  , ml.get()+"");
					cycleE.setAttribute( QprofilerXmlUtils.percent, nf.format(percentage));
					parent.appendChild(cycleE);
				}
			} catch ( DOMException e ) { e.printStackTrace(); }
		}
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
		Element element = doc.createElement(QprofilerXmlUtils.rangeTally   );
		parent.appendChild(element);
		
		// sort the map
		Map <Integer, AtomicLong> sortedMap = new TreeMap<Integer, AtomicLong>(map);
		
		try {
			for (Map.Entry<Integer, AtomicLong> entrySet : sortedMap.entrySet()) {
				Element cycleE = doc.createElement( QprofilerXmlUtils.rangeTallyItem   );
				Integer start = entrySet.getKey();
				cycleE.setAttribute(QprofilerXmlUtils.start  , start.toString());
				
				int endValue = start.intValue();
				if (start.intValue() == MAX_I_SIZE) {
					endValue = FINAL_I_SIZE_BUCKET_SIZE - 1;
				} else if  (start.intValue() > MAX_I_SIZE) {
					endValue = start.intValue() + (FINAL_I_SIZE_BUCKET_SIZE - 1);
				}
				
				cycleE.setAttribute(QprofilerXmlUtils.end, "" + endValue);
				cycleE.setAttribute(QprofilerXmlUtils.count, "" + entrySet.getValue().get());
				element.appendChild(cycleE);
			}
		} catch (DOMException e) {
			e.printStackTrace();
		}
	}
	public static void binnedLengthMapToRangeTallyXml(Element parent, QCMGAtomicLongArray array) {
		
		Document doc = parent.getOwnerDocument();
		Element element = doc.createElement("RangeTally");
		parent.appendChild(element);
		
		try {
			long length = array.length();
			for (int i = 0 ; i < length ; i++) {
				if (array.get(i) > 0) {
					Element cycleE = doc.createElement("RangeTallyItem");
					cycleE.setAttribute(QprofilerXmlUtils.start, "" + i);
					int endValue = i < MAX_I_SIZE ? 
							i + (INITIAL_I_SIZE_BUCKET_SIZE - 1) : 
								i + (FINAL_I_SIZE_BUCKET_SIZE - 1); 
							cycleE.setAttribute(QprofilerXmlUtils.end, "" + endValue);
							cycleE.setAttribute(QprofilerXmlUtils.count, "" + array.get(i));
							element.appendChild(cycleE);
				}
			}
		} catch (DOMException e) {
			e.printStackTrace();
		}
	}
	
	public static void binnedLengthMapToRangeTallyXml(Element parent, QCMGAtomicLongArray array, QCMGAtomicLongArray array2) {
		
		Document doc = parent.getOwnerDocument();
		Element element = doc.createElement("RangeTally");
		parent.appendChild(element);
		
		try {
			long length = array.length();
			for (int i = 0 ; i < length ; i++) {
				if (array.get(i) > 0) {
					Element cycleE = doc.createElement("RangeTallyItem");
					cycleE.setAttribute(QprofilerXmlUtils.start, "" + i);
					int endValue = i < MAX_I_SIZE ? 
							i + (INITIAL_I_SIZE_BUCKET_SIZE - 1) : 
								i + (FINAL_I_SIZE_BUCKET_SIZE - 1); 
							cycleE.setAttribute(QprofilerXmlUtils.end, "" + endValue);
							cycleE.setAttribute(QprofilerXmlUtils.count, "" + array.get(i));
							element.appendChild(cycleE);
				}
			}
			length = array2.length();
			for (int i = 0 ; i < length ; i++) {
				if (array2.get(i) > 0) {
					Element cycleE = doc.createElement("RangeTallyItem");
					cycleE.setAttribute(QprofilerXmlUtils.start, "" + i);
					int endValue = i < MAX_I_SIZE ? 
							i + (INITIAL_I_SIZE_BUCKET_SIZE - 1) : 
								i + (FINAL_I_SIZE_BUCKET_SIZE - 1); 
							cycleE.setAttribute(QprofilerXmlUtils.end, "" + endValue);
							cycleE.setAttribute(QprofilerXmlUtils.count, "" + array2.get(i));
							element.appendChild(cycleE);
				}
			}
		} catch (DOMException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Same as <code>SummaryReportUtils.tallyQualScores</code> but operates on
	 * an ASCII string, that does not have a separator
	 * 
	 * @param data String containing the data to be examined. ASCII encoded.
	 * @param map Map containing the tally
	 * @param offset int pertaining to the ASCII offset
	 * @see org.qcmg.qprofiler2.util.SummaryReportUtils#tallyQualScores(String, Map, String)
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
	public static void tallyQualScores(byte [] data, AtomicLongArray array) {
		if (null != data) {
			int countUnderTen = 0;
			for (byte b : data) {
				if ((b & 0xFF) < 10)
					countUnderTen++;
			}
			array.incrementAndGet(countUnderTen);
		}
	}
	
	/**
	 * how many quality base value is below then 10, then record this count to specified array
	 * @param data : base quality array, each base quality is acsii - 33; eg. string in read column 11th "()FF...", equal to "7,8,37,37..."
	 * @param array: an array to storre the bad base count for each bam record
	 */
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
	 * @see org.qcmg.common.util.SummaryByCycleUtils#incrementCount(Map,
	 *      Object)
	 */
	public static void tallyQualScores(String data, ConcurrentMap<Integer, AtomicLong> map, String separator) {
		
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
	 * @param data String data that is being checked for bad reads ('.' or 'N')
	 * @param map Map containing the tally
	 * @see org.qcmg.common.util.SummaryByCycleUtils#incrementCount(Map, Object)
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
	public static void tallyBadReadsAsString(String data, AtomicLongArray array) {
		if (null != data) {
			int count = 0;
			for (int i = 0, size = data.length() ; i < size ; i++) {
				if (isInValid(data.charAt(i)))
					count++;
			}
			array.incrementAndGet(count);
		}
	}
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
	
	
	public static void coverageByReferenceToXml(Element parent, String elementName, Map<String, PositionSummary> mapOfPositions, List<String> readGroupIds) {	
		Element element = QprofilerXmlUtils.createSubElement(parent, elementName);
	
		//get the max number of bin
		List<String> refs =  mapOfPositions.keySet().stream().filter(
				it -> it.toLowerCase().startsWith("chr")).sorted( (s1,s2) ->
				   new ReferenceNameComparator().compare(s1, s2) 						
			).collect(Collectors.toList());
		int maxLength = 0;
		Map<String, Map<Integer, AtomicLong>> chrCounts = new HashMap<String, Map<Integer, AtomicLong>>();
		for(String str : refs){
			if( mapOfPositions.get(str).getMax() > maxLength) 
				maxLength =  mapOfPositions.get(str).getMax();
			chrCounts.put(str, mapOfPositions.get(str).getCoverage());			
		}
		
		Element rNameE = QprofilerXmlUtils.createSubElement(element, "RangeTally" );		 
		rNameE.setAttribute("possibleValues", Arrays.toString(refs.toArray()).replace("[", "").replace("]", "").replace(" ", "") ); 		
		
		for(int i = 0; i < (maxLength /PositionSummary.BUCKET_SIZE) + 1; i ++ ){	
			StringBuilder sb = new StringBuilder();
			long sum = 0;
			for(String str : refs){
				AtomicLong c = chrCounts.get(str).get(i);
				sum += c.get(); 
				if(c == null ) sb.append("0,") ;
				else sb.append(c.get() + ",");				
			}
			if(sum == 0) continue;
			
			if(sb.length() > 1) sb = sb.delete(sb.length()-1, sb.length());			
			Element tallyE =  QprofilerXmlUtils.createSubElement(rNameE, "RangeTallyItem" ); // doc.createElement("RangeTallyItem");
			tallyE.setAttribute(QprofilerXmlUtils.start, "" + i * PositionSummary.BUCKET_SIZE  ); 
			tallyE.setAttribute(QprofilerXmlUtils.end, "" + ((i + 1) * PositionSummary.BUCKET_SIZE  - 1));
			tallyE.setAttribute(QprofilerXmlUtils.counts, sb.toString() );// 				 
		}
		 
	}	
	
	public static  void coverageByReadGroupToXml(Element parent, String elementName, Map<String, PositionSummary> mapOfPositions, List<String> readGroupIds) {	
		
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
				
		if(overFloorBin == 0) overFloorBin ++;
		int cellingValue = (int) (2.5 * coverageTotal / overFloorBin );
		// sort map
		Map<String, PositionSummary> sortedMap = new TreeMap<String, PositionSummary>(new ReferenceNameComparator());
		sortedMap.putAll( mapOfPositions );
				
		try {			
			Element element = QprofilerXmlUtils.createSubElement(parent, elementName);
			for (Map.Entry<String, PositionSummary> entrySet : sortedMap.entrySet()) {
				Element rNameE = QprofilerXmlUtils.createSubElement(element, "RangeTally" );
				rNameE.setAttribute("source", entrySet.getKey());
				PositionSummary ps = entrySet.getValue();
				rNameE.setAttribute("minPosition", "" + ps.getMin() );
				rNameE.setAttribute("maxPosition", "" + ps.getMax() );
				rNameE.setAttribute(QprofilerXmlUtils.count, "" + ps.getTotalCount() );
				rNameE.setAttribute("visuallCellingValue", "" + cellingValue  );
				rNameE.setAttribute("possibleValues", Arrays.toString(readGroupIds.toArray()).replace("[", "").replace("]", "").replace(" ", "") );
				element.appendChild(rNameE);
								
				// insert map of coverage here
				Map<Integer, AtomicLong> coverage = ps.getCoverage();
//				Map<Integer, AtomicLongArray> rgCoverages = ps.getCoverageByRgId(readGroupIds);
				
				for (Entry<Integer, AtomicLongArray> entry : ps.getCoverageByRgId(readGroupIds).entrySet()  ){ //ps.getRgCoverage().entrySet()) {
					Element tallyE =  QprofilerXmlUtils.createSubElement(rNameE,  QprofilerXmlUtils.rangeTallyItem ); //   "RangeTallyItemOld" ); 
					tallyE.setAttribute(QprofilerXmlUtils.start, "" + entry.getKey() * PositionSummary.BUCKET_SIZE  ); 
					tallyE.setAttribute(QprofilerXmlUtils.end, "" + ((entry.getKey() + 1) * PositionSummary.BUCKET_SIZE  - 1));					
					tallyE.setAttribute(QprofilerXmlUtils.counts, entry.getValue().toString().replace("[", "").replace("]", "").replace(" ", "") );//remove space, [ ]
					tallyE.setAttribute(QprofilerXmlUtils.count, "" + coverage.get(entry.getKey()) );
				}
			}
		} catch (DOMException e) {
			e.printStackTrace();
		}
	}
	
	public static void MAPQtoXML(Element parent, String elementName, String[] possible,  QCMGAtomicLongArray[] mapq) {
		Element element = (parent.getElementsByTagName(elementName).getLength() > 0)?
				(Element) parent.getElementsByTagName(elementName).item(0)  :QprofilerXmlUtils.createSubElement(parent, elementName);
				
		//check array get the max mapq value 		
		if(possible.length != mapq.length ) return; 
		long[] totalCounts = new long[mapq.length];
		int maxValue = 0;
		for(int i = 0; i < mapq.length; i ++) 			
			for(int j = 0; j < mapq[i].length(); j ++){
				if(mapq[i].get(j) == 0) continue;
				if(j > maxValue ) maxValue = j; 
				totalCounts[i] += mapq[i].get(j);
			}
		
		//create possibleValues eg "FirstOfPair,SecondOfPair,unPaired"		  
		String possibleValue = "", readsNo = "";
		for(int i = 0; i < totalCounts.length; i ++){
			if(totalCounts[i] == 0) continue; //skip empty array
			possibleValue += (possibleValue.length() > 0 )? QprofilerXmlUtils.COMMA + possible[i] : possible[i]; 
			readsNo += (readsNo.length() > 0)? QprofilerXmlUtils.COMMA + + totalCounts[i] :totalCounts[i] ;		
		}
				
		element = QprofilerXmlUtils.createSubElement(element, QprofilerXmlUtils.valueTally   );	//only one ValueTally for MAPQ
		element.setAttribute( QprofilerXmlUtils.possibles , possibleValue);
		element.setAttribute( "totalCounts", readsNo);
		
		for(int j = 0; j <= maxValue; j ++){
			Element cycleE = QprofilerXmlUtils.createSubElement(element, QprofilerXmlUtils.tallyItem );
			String counts = "";
			for(int i = 0; i < totalCounts.length; i++){
				if(totalCounts[i] == 0) continue; //skip empty array
				counts += (counts.length() > 0)? QprofilerXmlUtils.COMMA + mapq[i].get(j) : mapq[i].get(j) ;		
			}			
			cycleE.setAttribute(QprofilerXmlUtils.counts, counts);
			cycleE.setAttribute(  QprofilerXmlUtils.value, j+"");		
		}


	}
	
	public static <T> void ToXmlWithoutPercentage(Element parent, String elementName, Map<T, AtomicLong> map) {
		//lengthMapToXml(parent, elementName, map, null);
		Document doc = parent.getOwnerDocument();
		Element element = doc.createElement(elementName);
		parent.appendChild(element);
				
		if (null != map && ! map.isEmpty()) {		
		// get keys and sort them
			List<T> sortedKeys = new ArrayList<>(map.keySet());
			Collections.sort(sortedKeys, null);
			try {
				for (T key : sortedKeys) {
					Element cycleE = doc.createElement("TallyItem");
					AtomicLong ml = map.get(key);					
					cycleE.setAttribute("value", key.toString());
					cycleE.setAttribute(QprofilerXmlUtils.count, ml.get()+"");					
					element.appendChild(cycleE);
				}
			} catch (DOMException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static ConcurrentMap<String, ConcurrentMap<Integer, AtomicLong>> binIsize(int binSize, Map<String, AtomicLongArray> iSizeByReadGroupMap, Map<String, QCMGAtomicLongArray> iSizeByReadGroupMapBinned) {
		ConcurrentMap<String, ConcurrentMap<Integer, AtomicLong>> results = new ConcurrentHashMap<>();
		for (Entry<String, AtomicLongArray> entry : iSizeByReadGroupMap.entrySet()) {
			// add entry to results map of  maps
			String readGroup = entry.getKey();
			ConcurrentMap<Integer, AtomicLong> iSizeLengths = new ConcurrentHashMap<>();
			results.putIfAbsent(readGroup, iSizeLengths);
			
			AtomicLongArray array = entry.getValue();
			for (int i = 0 ; i < array.length() ; i++) {
				long longValue = array.get(i);
				if (longValue > 0) {
				
					AtomicLong al = iSizeLengths.get(i);
					if (null == al) {
						al = new AtomicLong();
						AtomicLong existingLong = iSizeLengths.putIfAbsent(i, al);
						if (null != existingLong) al = existingLong;
					}
					al.addAndGet(longValue);
				}
			}
		}
		
		// now for the binned map
		for (Entry<String, QCMGAtomicLongArray> entry : iSizeByReadGroupMapBinned.entrySet()) {
			String readGroup = entry.getKey();
			ConcurrentMap<Integer, AtomicLong> iSizeLengths = results.get(readGroup);
			if (null == iSizeLengths) {
				iSizeLengths = new ConcurrentHashMap<Integer, AtomicLong>();
				ConcurrentMap<Integer, AtomicLong>  existingResults = results.putIfAbsent(readGroup, iSizeLengths);
				if (null != existingResults) {
					iSizeLengths = existingResults;
				}
			}
			QCMGAtomicLongArray array = entry.getValue();
			for (int i = 0 ; i < array.length() ; i++) {
				long l = array.get(i);
				if (l > 0) {
					Integer binNumber = (i == 0 ? MAX_I_SIZE : i * 1000000);
					AtomicLong al = iSizeLengths.get(binNumber);
					if (null == al) {
						al = new AtomicLong();
						AtomicLong existingAL = iSizeLengths.putIfAbsent(binNumber, al);
						if (null != existingAL) {
							al = existingAL;
						}
					}
					al.addAndGet(l);
				}
			}
		}
		return results;
	}	
		
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

	public static void iSize2Xml(Element parent, ConcurrentMap<String, ReadGroupSummary> rgSummaries) {
		//sort all read group
		Set<String> readGroups = new TreeSet<String>( rgSummaries.keySet());
		readGroups.remove(QprofilerXmlUtils.All_READGROUP);
				
		Element valueE = QprofilerXmlUtils.createSubElement(parent, QprofilerXmlUtils.valueTally); 
		String possibles = QprofilerXmlUtils.joinByComma(new ArrayList<String>(readGroups));
		valueE.setAttribute(QprofilerXmlUtils.possibles, possibles  );		
		
		//report isize < 5000
		for(int pos = 0; pos < ReadGroupSummary.middleTlenValue ; pos ++){
			long sum = 0;			 
			StringBuilder sb = new StringBuilder();
			for(String rg : readGroups ){
				QCMGAtomicLongArray counts = rgSummaries.get(rg).getISizeCount();
				sum += counts.get(pos);
				sb.append( counts.get(pos)+ QprofilerXmlUtils.COMMA);				
			}
			//discard isize without reads
			if(sum == 0 ) continue;
			Element element =QprofilerXmlUtils.createSubElement(valueE, QprofilerXmlUtils.tallyItem);
			element.setAttribute(QprofilerXmlUtils.value, pos+"");
			element.setAttribute(QprofilerXmlUtils.counts, sb.substring(0, sb.length()-QprofilerXmlUtils.COMMA.length()));
		}	
		
	}

}
