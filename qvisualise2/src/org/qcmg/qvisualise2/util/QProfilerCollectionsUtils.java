/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qvisualise2.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

import org.qcmg.common.math.SimpleStat;
import org.qcmg.common.model.MAPQMiniMatrix;

import org.qcmg.common.model.SummaryByCycle;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.qvisualise2.report.ChartTabBuilder;
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
		Map<T, AtomicLong> splitFlags = new LinkedHashMap<T, AtomicLong>();
		
		// loop through flags, if <T> contains the specified value, add to value 
		for (Entry<T, AtomicLong> entry : flags.entrySet()) {
			final String[] flagSplit = ((String)entry.getKey()).split(", ");
			
			String flagSummaryString = "";
			if (flagSplit.length == 2)
				flagSummaryString = flagSplit[1];
						
			T value = null;
			for (Entry<String, T> distMapEntry : distinguisherMap.entrySet()) {
				if (flagSummaryString.contains(distMapEntry.getKey())) {
					value = distMapEntry.getValue();
				}
			}
			
			if (null == value)
				value =  null != nonDistDesc ? nonDistDesc : (T) "Other";
			
			final AtomicLong currentCount = splitFlags.get(value);
			
			if (null == currentCount)  
				splitFlags.put(value, new AtomicLong(entry.getValue().get()));
			  else  
				currentCount.addAndGet(entry.getValue().get());
			 				
		}
		
		return splitFlags;
	}
	
	public static <T> SummaryByCycle<T> generateSummaryByCycleFromElement(Element element) {
		ConcurrentMap<Integer, ConcurrentMap<T, AtomicLong>> tally = new ConcurrentHashMap<Integer, ConcurrentMap<T, AtomicLong>>();
		
		// get the cycles
		if (null != element  && element.hasChildNodes()) { 				
			final NodeList cycles = element.getElementsByTagName( "Cycle" );
			for (int i = 0, size = cycles.getLength() ; i < size ; i++) 
				if (cycles.item(i) instanceof Element) {
					final Element cycleElement = (Element) cycles.item(i);						
					// get tallyitems
					ConcurrentMap<T, AtomicLong> cycleCount = new ConcurrentHashMap<T, AtomicLong>();
					populateTallyItemMap(cycleElement, cycleCount, false, null);						
					tally.put(Integer.parseInt(cycleElement.getAttribute("value")), cycleCount);
				}			
		}

		return new SummaryByCycle<T>(tally);
	}	

	@SuppressWarnings("unchecked")
	public static <T> void populateTallyItemMap(Element cycleElement, final Map<T, AtomicLong> map, boolean isInteger, ChartTabBuilder.Filter filter) {		
		if(null == cycleElement) return; 
		
		for(Element  tallyItemElement :  QprofilerXmlUtils.getChildElementByTagName(cycleElement, "TallyItem") ){
			if( filter != null && ! filter.result(tallyItemElement) ) continue; //discard unsatisfied nodes eg. RNEXT			
			if (tallyItemElement.getAttribute("count").length() > 0) {					
				final long count = Long.parseLong(tallyItemElement.getAttribute("count"));					
				map.put(isInteger ? (T) Integer.valueOf(tallyItemElement.getAttribute("value")) 
						: (T) tallyItemElement.getAttribute("value") , new AtomicLong(count) );											
			}			 						
		}	 
	}
		
	/**
	 * store counts from children element to input map
	 * @param cycleElement, RangeTally
	 * @param map: it will be filled with <start, count>
	 */
	public static   void populateIntegerMap(Element cycleElement, final Map<Integer, AtomicLongArray> map, boolean isRange, boolean isContinue, ChartTabBuilder.Filter filter) {			
		if(null == cycleElement) return; 
		
		Map<Integer, AtomicLongArray> map1 = new HashMap<Integer, AtomicLongArray>();
		int maxValue = 0, gap = 1, countsNo = 0; // these numbers will be used to fill [0,0,,,]
		
		NodeList nl =cycleElement.getChildNodes();	
		for (int i = 0, size = nl.getLength() ; i < size ; i++) {
			if(! (nl.item(i) instanceof Element)) continue; 
			Element e = (Element) nl.item(i);	
			
			if(filter != null && filter.result(e) == false)	continue;
			String[] sValues = e.getAttribute( QprofilerXmlUtils.counts ).split(",");
			if(countsNo == 0) countsNo = sValues.length;
			long[] nValues = new long[sValues.length];
			for(int j = 0; j < sValues.length; j ++)
				nValues[j] = Long.parseLong(sValues[j].trim());
			
			Integer value = 0;			
			try{
				if(isRange){
					value = Integer.valueOf(e.getAttribute( QprofilerXmlUtils.start ));  
					if(gap == 1) gap = Integer.valueOf(e.getAttribute( QprofilerXmlUtils.end )) - Integer.valueOf(e.getAttribute(QprofilerXmlUtils.start )) + 1;
				}else					
					value = Integer.valueOf(e.getAttribute( QprofilerXmlUtils.value ));
			}catch(NumberFormatException ex){}
			
			if(maxValue < value) maxValue = value;//get maxValue				
			map1.put(value, new AtomicLongArray( nValues));		
		}
		
		//return values; 
		AtomicLongArray zeros = new AtomicLongArray(countsNo);
		for(int i = 0; i <= maxValue; i += gap){
			AtomicLongArray counts =  map1.get((Integer) i );
			if ( counts == null  ){
				if(! isContinue ) continue;				
				counts = zeros; // add zero value for no counts if requier continue, eg. isize, coverage
			} 
			map.put( i, counts );
		}					 		
	}
	
	/**
	 * 
	 * @param cycleElement: eg. <RangeTally count="59942150" ... source="chr1">
	 * @return 2 dimension array of [start, end,  count], here the oustanding peaks are removed if bynond out of [mean-3std, mean+3std]
	 */
	public static Integer[][] populateSmoothStartCountMap(Element cycleElement  ){
		if(null == cycleElement) return null; 

		NodeList nl =cycleElement.getChildNodes();	
		List<Integer[]> counts = new ArrayList<>();		
		for (int i = 0, size = nl.getLength() ; i < size ; i++) {
			if(! (nl.item(i) instanceof Element)) continue; 
			Element e = (Element) nl.item(i);						 		
			try{
				int count =  Integer.valueOf( e.getAttribute( QprofilerXmlUtils.count )  );
				int start = Integer.valueOf(e.getAttribute( QprofilerXmlUtils.start ));  
				int end = Integer.valueOf(e.getAttribute( QprofilerXmlUtils.end ));  
				counts.add( new Integer[]{start, end, count});					 
			}catch(NumberFormatException ex){}			 	
		}
		
		//remove spriks which are byond (mean+/-3std)
		counts = SimpleStat.getWithin3STD(counts, 2);
						
		return  counts.toArray(new Integer[counts.size()][3]);
		
	}
	
	public static void populateSmoothCoverageMapByChromosome(Element element, Map<Integer, AtomicLongArray> map, List<String> possibles) {
		List<Element> nodes = QprofilerXmlUtils.getChildElementByTagName(element, "RangeTally");  
		if(nodes == null || nodes.size() == 0) return ; 
		
		//run again for counts
		for(int t = 0; t < nodes.size(); t ++){
			Element tallyElement = nodes.get(t);					
			String ref = tallyElement.getAttribute(QprofilerXmlUtils.source);
			//update reference name based on xml tag attribute;
			int index = -1; //chr position  eg {chr1...chrY}={0..25}
			for( int ii = 0; ii < possibles.size(); ii ++) 
				if(possibles.get(ii).equals(ref)) { index = ii; break; }
			if(index < 0) continue;  

			Integer[][] counts = populateSmoothStartCountMap(tallyElement);				
			for(int i = 0; i < counts.length; i ++){
				int start = counts[i][0];
				int count = counts[i][2];
				if (map.get(start) == null )
					map.put(start, new AtomicLongArray(possibles.size() ));
				map.get(start).addAndGet(index, count);  //put count to chr position				
			}
		}
	}
	
	/**
	 * 
	 * @param element: <CycleTally source="FirstOfPair">
	 * @return
	 */
	
	//single line cycle
	public static Map<Integer, String> generatePercentagesMapFromElement(Element element ) {
		
		ConcurrentMap<Integer, String> tally = new ConcurrentHashMap<>();
		if (null == element) return tally; 
		
		// get the cycles
		final NodeList cycles = element.getElementsByTagName("Cycle");
		//NodeList cycles = cycleTallyElement.getChildNodes();
		for (int i = 0, size = cycles.getLength() ; i < size ; i++) {
			if (! (cycles.item(i) instanceof Element)) continue;			
			final Element cycleElement = (Element) cycles.item(i);
			// can't add null to ConcurrentHashMap 
			if (cycleElement.getAttribute("percent").length() > 0)  				
				tally.put(Integer.parseInt(cycleElement.getAttribute("value")), cycleElement.getAttribute("percent"));		
		}
		return tally;
	}		
	
	//single line cycle
	public static Map<Integer, String> generateRangesMapFromElement(Element element ) {
		
		ConcurrentMap<Integer, String> tally = new ConcurrentHashMap<Integer, String>();
		if (null == element) return tally; 
		
		// get the cycles
		final NodeList cycles = element.getElementsByTagName("RangeTallyItem");
		//NodeList cycles = cycleTallyElement.getChildNodes();
		for (int i = 0, size = cycles.getLength() ; i < size ; i++) {
			if (! (cycles.item(i) instanceof Element)) continue;			
			final Element cycleElement = (Element) cycles.item(i);
			int start = Integer.parseInt(cycleElement.getAttribute("start"));
			int end = Integer.parseInt(cycleElement.getAttribute("end"));
						
			tally.put( (start+end)/2, String.format("bin size: %,d~%,d", start, end));		
		}
		return tally;
	}
	
	public static <T> Map<T, AtomicLong> sortTallyItemMapByValue( Map<T, AtomicLong> map) {
		List<Map.Entry<T, AtomicLong>> maplist = new LinkedList< Map.Entry<T, AtomicLong> >( map.entrySet() );		
		
	
		Collections.sort(maplist, new Comparator<Map.Entry<T, AtomicLong>>(){
            @Override
            public int compare(Map.Entry<T, AtomicLong> ele1,Map.Entry<T, AtomicLong> ele2) {
            	ele1.getValue().get();
               return  (int)ele1.getValue().get() - (int) ele2.getValue().get()  ;
            }
        });
		
		Map<T, AtomicLong> aMap2 = new LinkedHashMap<T, AtomicLong>();
        for(Entry<T, AtomicLong> entry: maplist) 
            aMap2.put(entry.getKey(), entry.getValue());
        
        return aMap2;		
	}	

	
	public static void populateMatrixMap(Element cycleElement,
			Map<MAPQMiniMatrix, AtomicLong> map) {
		if (null != cycleElement) {
			NodeList tallyItemsNL = cycleElement.getElementsByTagName("TallyItem"); 
			for (int j = 0, size = tallyItemsNL.getLength() ; j < size ; j++) {
				Element tallyItemElement = (Element) tallyItemsNL.item(j);
				long count = Long.parseLong(tallyItemElement.getAttribute("count"));
				
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
	
	public static Map<String, List<String>> convertSummaryTextToMap(String summaryText) {
		if (null == summaryText) return null;
		
		String [] params = summaryText.split("\n");
		Map<String, List<String>> results = new LinkedHashMap<String ,List<String>>();
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
			params = summaryText.split("@");
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
		return results;
	}
	
	public static Map< String, List<String> > convertHeaderTextToMap(String headerText) {
		if (null == headerText) return null;
		
		String [] params = headerText.split("\n");		 
		Map<String, List<String>> results = new LinkedHashMap<String ,List<String>>();
		
		//init the map for order
 		String[] heads = {"Header","Sequence","Read Group","Program","Comments","Other"};		
		for(String key : heads)
			results.put(key, null);
		
		if(params.length == 1){
			//FIXME split on '@' char, and add back in....
			// hack to get around line breaks not being preserved in CDATA sections
			// also check using UTF-16...
			params = headerText.split("@");
			for(int i = 0; i < params.length; i ++)
				params[i] = ("@" + params[i]).trim();			
		}
			
		for (String param : params) {			
			String key = null;
			if (param.startsWith(Constants.HEADER_PREFIX)){ 
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
				
		//remove not exit head field
		for(String key : heads)
			if(results.get(key) == null)
				results.remove(key);
		
		return results;		
	}
	
	private static void addDataToList(Map<String, List<String>> map, String key, String data) {
		List<String> tmpList = map.get(key);
		if(key == null) return; 
		if (null == tmpList) {
			tmpList = new ArrayList<String>();
			map.put(key, tmpList);
		}
		tmpList.add(data);
	}


	/**
	 * 
	 * @param element: <CoverageByReadGroup>
	 * @param map: input an empty map to store the start position and counts from a list of <RangeTally ...>, which source value listed on gcMessages
	 */
	public static void populateCoverageMapByChromosome(Element element, Map<Integer, AtomicLongArray> map, List<String> possibles) {
		// init
		List<Element> nodes = QprofilerXmlUtils.getChildElementByTagName(element, "RangeTally");  
		if(nodes == null || nodes.size() == 0) return ; 
		
		//run again for counts
		for(int t = 0; t < nodes.size(); t ++){
			Element tallyElement = nodes.get(t);					
			String ref = tallyElement.getAttribute(QprofilerXmlUtils.source);

			//update reference name based on xml tag attribute;
			int index = -1; //chr position  eg {chr1...chrY}={0..25}
			for( int ii = 0; ii < possibles.size(); ii ++) 
				if(possibles.get(ii).equals(ref)) { index = ii; break; }
			if(index < 0) continue;  
					 
			List<Element> itemNodes = QprofilerXmlUtils.getChildElementByTagName(tallyElement, QprofilerXmlUtils.rangeTallyItem); 
			for(Element ele: itemNodes){						 
				try{
					int start = Integer.parseInt(ele.getAttribute(QprofilerXmlUtils.start));
					int count = Integer.parseInt(ele.getAttribute(QprofilerXmlUtils.count));					
					if (map.get(start) == null )
						map.put(start, new AtomicLongArray(possibles.size() ));
					map.get(start).addAndGet(index, count);  //put count to chr position
				}catch(NumberFormatException e){}//do nothing for current element if exception						
			}
		}		
	}
	
	
	public static List<String> getPossibleReference( Element element ){
		List<String> possibles = GCCoverageUtils.getGCPercentRefs();	
		List<Element> nodes = QprofilerXmlUtils.getChildElementByTagName(element, "RangeTally");  
		if(nodes == null || nodes.size() == 0) return null; 
		
		//reset refernce lists
		String[] repossible = new String[possibles.size()];
		for(int t = 0; t < nodes.size(); t ++){
			Element tallyElement = nodes.get(t);					
			String ref = tallyElement.getAttribute(QprofilerXmlUtils.source);
			String chr = IndelUtils.getFullChromosome(ref );
			
			//update reference name based on xml tag attribute;
			int index = -1; //chr position  eg {chr1...chrY}={0..25}
			for( int ii = 0; ii < possibles.size(); ii ++) 
				if(possibles.get(ii).equals(chr)) { index = ii; repossible[ii] = ref; break; }
		}	
		possibles.clear();
		for(int t = 0; t< repossible.length; t++)
			if(repossible[t] != null) possibles.add(repossible[t]);		
		
		return possibles; 
	}


}
