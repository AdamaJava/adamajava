/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 * 
 */

package org.qcmg.qprofiler2.summarise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Class that tallies by cycle using java generics 
 * 
  */
public class CycleSummary<T> { 
	public static final String baseOnCycle = "cycle";
	
	private static final int MAX_ARRAY_CAPACITY = 2048 * 2048;		// over 4 million
		
	/**
	 * 
	 * Sequence data
	 * From SAM spec 1.4:
	 * \*|[A-Za-z=.]+
	 * * has an ascii score of 42
	 * z has an ascii score of 122
	 * 
	 * Quality scores are stored as ASCII representation of Phred-scale base quality + 33
	 * From SAM spec 1.4:
	 * [!-~]+
	 * ! has an ascii score of 33
	 * ~ has an ascii score of 126
	 * 
	 * here both DEFAULT_NO_OF_KEYS_INTEGER and DEFAULT_NO_OF_KEYS_CHARACTER are 128.
	 *  so just use one value 128 for key mask
	 * 
	 */
	private static final int DEFAULT_NO_OF_KEYS = 128;	
		
	// atomic boolean used as a lock when resizing the array
	private final AtomicBoolean resizingInProgress = new AtomicBoolean(false);
	
	// can be set in the constructor, but defaults to the Java default value
	private final  T type;
	private final AtomicInteger cycleMask = new AtomicInteger();
	private final AtomicInteger keyMask = new AtomicInteger();
	private final AtomicInteger maxCycleValue = new AtomicInteger();
	private final AtomicInteger maxKeyValue = new AtomicInteger();
	private final AtomicLong parseCounts = new AtomicLong();
	
	private AtomicLongArray tally;
	
	/**
	 * Constructs a new SummaryByCycleNew2 object with the specified noOfCycles size, 
	 *  and the default value for the noOfKeys, which is dependent on the supplied type T
	 *  
	 *  Calls @see SummaryByCycleNew2
	 * @param type
	 * @param noOfCycles
	 */
	public CycleSummary(T type, final int noOfCycles) { 		
		this.type = type;		
		cycleMask.set(getMask(noOfCycles));
		keyMask.set(getMask( DEFAULT_NO_OF_KEYS));
		int capacity = 1 << (cycleMask.get() + keyMask.get());
		
		maxCycleValue.set((1 << cycleMask.get()) - 1);
		maxKeyValue.set((1 << keyMask.get()) - 1 );		
		tally = new AtomicLongArray(capacity);
	}
	
	public int getMask(int number) { 
		
		// get nearest power of two larger than number
		int maskShift = 1;
		int mask = 1;
		while (mask < number) { 
			++maskShift;
			mask <<= 1;
		}
		
		return maskShift;		
	}
	
	public int getArrayPosition(int cycle, int key) { 
		int result = cycle;
		result += (key << cycleMask.get());
		
		return result;
	}
	
	/**
	 * 
	 * @param position of the array
	 * @return a pair of cycle number and value which is stored in int. eg. array position 100, stores cycle 20 with 'A' (65)
	 */
	public int[] getCycleKeyFromArrayPosition(int position) { 
		int key = ( position >>> cycleMask.get());
		int cycle  = position & (( 1 << cycleMask.get()) - 1);
		return new int[] { cycle,key};
	}

	
	@SuppressWarnings("unchecked")
	private T getTypeFromInt(int l) { 
		if (type instanceof Integer) { 
			return (T)Integer.valueOf(l);
		}
		
		if (type instanceof Character) { 
			return (T) Character.valueOf((char)l);
		} else {
			return null;
		}
	}
	
	/**
	 * Increments the count of a particular value, for a particular cycle.
	 * If the cycle does not exist, it will be added to the summary.
	 * If the value at the specified cycle does not exist, it will be added to the map for that cycle.
	 * 
	 * @param cycle Integer the cycle that is being incremented
	 * @param value for the specified cycle whose count is to b incremented
	 */

	public void increment(final int cycle, final int value) { 
		if (cycle > maxCycleValue.get() || value > maxKeyValue.get()) {
			resize(cycle, value);
		}
		if (resizingInProgress.get()) { 
			// CAS until resizing is complete
			while  ( ! resizingInProgress.compareAndSet(false, false) ) { }
		}
		tally.incrementAndGet(getArrayPosition(cycle, value));
	}
		
	private void resize(final int cycle, final int key) { 
		
		// find out if it is the cycle, key or both that are larger than current maximums
		// lock
		while ( ! resizingInProgress.compareAndSet(false, true)) { }
		try { 	
			// only resize if we still need to
			// another thread may have already done this for us
			boolean resize = false;	
			
			// re-set current maximums
			int newCycleMask = -1;
			if (cycle > maxCycleValue.get()) { 
				newCycleMask = getMask(cycle); 
				maxCycleValue.set((1 << newCycleMask) - 1);
				resize = true;
			}
			
			if (key > maxKeyValue.get()) { 
				keyMask.set(getMask(key)); 
				maxKeyValue.set((1 << keyMask.get()) - 1);
				resize = true;
			}
			
			if (resize) { 
				
				// sleep to allow any threads in CAS loop in increment method to complete
				// resizing should not occur often so will not be too great a performance penalty
				// and it SHOULD mean we keep the array in a consistent state 
				Thread.sleep(200);
				
				// get new capacity
				int capacity = 1 << ((newCycleMask == -1 ? cycleMask.get() : newCycleMask) + keyMask.get());
				
				// check on new capacity
				if (capacity > MAX_ARRAY_CAPACITY) {
					throw new IllegalArgumentException("new AtomicLongArray is too large...");
				}
				
				// create new array
				AtomicLongArray newTally = new AtomicLongArray(capacity);
				
				// transfer data from existing tally to new tally
				for (int i = 0, length = tally.length() ; i < length ; i++) { 
					
					// retrieve existing data
					long value = tally.get(i);
					if (value > 0) { 
						int [] cycleKey = getCycleKeyFromArrayPosition(i);
						
						int newArrayIndex = cycleKey[0];
						newArrayIndex += cycleKey[1] << (newCycleMask == -1 ? cycleMask.get() : newCycleMask);
						
						newTally.set(newArrayIndex, value);
					}
				}
				tally = newTally;
				
				// update cycleMask
				if (newCycleMask > -1) {
					cycleMask.set(newCycleMask);
				}
			}	
		} catch (InterruptedException e) { 
			e.printStackTrace();
		} finally { 
			// unlock
			while ( ! resizingInProgress.compareAndSet(true, false)) { }
		}
	}
	
	/**
	 *  * Returns the number of times a particular value has occurred at a particular cycle.
	 * <p>
	 * Returns 0 if the cycle does not exist,  or if the cycle does not contain the specified value.
	 * 
	 * @param cycle Integer representing the cycle number
	 * @param value for which we want a count, at a particular cycle
	 * @return Integer relating to the count of .Defaults to zero if the cycle does not exist, 
	 * or if the cycle does not contain the specified value <T>
	 */	
	public long count(Integer cycle, T value) { 	
 
		int v = (type instanceof Integer) ? (Integer) value : 
			(type instanceof Character) ? (Character)  value  : -1; 
			
		return  tally.get(getArrayPosition(cycle, v));
	}
	

	/**
	 * 
	 * @param key is the cycle number
	 * @return a map of counts for each possible value on this cycle
	 */
	public ConcurrentMap<T, AtomicLong> getValue(Integer key) { 
		ConcurrentMap<T, AtomicLong> cm = new ConcurrentHashMap<T, AtomicLong>();
		
		// loop through keys to get ones with this cycle number		
		for (int i = 0 , length = tally.length() ; i < length ; i++) { 
			long arrayValue = tally.get(i);
			// whole bunch of zeros in here - only want >0 values
			if (arrayValue > 0) { 
				int [] cycleKey = getCycleKeyFromArrayPosition(i);
				if (cycleKey[0] == key.intValue()) { 
					cm.put(getTypeFromInt(cycleKey[1]), new AtomicLong(arrayValue));
				}
			}
		}		
		return cm;
	}	
	/**
	 * 
	 * @return SortedSet relating to the cycles currently held by this summary object.
	 */
	public SortedSet<Integer> cycles() { 
		HashSet<Integer> ts = new HashSet<Integer>();		
		for (int i = 0 , length = tally.length() ; i < length ; i++) { 
			if ( tally.get(i) <= 0 ) {
				continue;		 
			}
			int [] cycleKey = getCycleKeyFromArrayPosition(i);
			ts.add(cycleKey[0]);		
		}		
		return new TreeSet<Integer>(ts);
	}
	
	/**
	 *  Returns a SortedSet of elements for a particular cycle of this summary object. 
	 *  Returns null if the summary does not contain the cycle.
	 *  
	 */
	public Set<T> getPossibleValues() { 	
		
		HashSet<T> allValues = new HashSet<T>();
		
		for (int i = 0 , length = tally.length() ; i < length ; i++) { 
			if ( tally.get(i) <= 0 ) {
				continue;
			}
			allValues.add( getTypeFromInt(getCycleKeyFromArrayPosition(i)[1] )  );	
		}		
		// order as ACGTN
		if ( type instanceof Character) { 					
			List<T> notATGC = new ArrayList<T>();
			
			for ( T v : allValues) { 
				char v1 =   (Character)  v; 
				if ( v1 != 'A' &&   v1 != 'T' &&  v1 != 'G' &&  v1 != 'C') {
					notATGC.add(v);		
				}				
			}	
			
			allValues.removeAll(notATGC);
			return   Stream.concat(allValues.stream().sorted(), notATGC.stream()).collect(Collectors.toCollection(LinkedHashSet::new));	
		} else if ( type instanceof Integer) { 				
 			// Integer reverse order for QUAL			 
			TreeSet<T> treeSetObj = new TreeSet<T>( (i1,i2) -> ((Integer) i2).compareTo( (Integer) i1) );
		    treeSetObj.addAll(allValues);
		    return treeSetObj; 
		}
		
		return allValues; 
	}

	
	/**
	 * Adds an xml representation of the current object to the supplied parent element.
	 * 
	 * @param metricEle is the root element
	 * @param groupName is the first child element
	 * @param readCount will attached to the readCount attribute
	 */
		
	public void toXml( Element metricEle,  String groupName, long readCount ) { 
		// do nothing if no base detected
		Set<T> possibles = getPossibleValues();
		if ( possibles == null || possibles.size() <= 0 ) {
			return; 
		}
		 	
		Element ele = XmlUtils.createGroupNode( metricEle, groupName);	// <category>   
		ele.setAttribute(ReadGroupSummary.READ_COUNT, readCount + "");
		for (Integer cycle : cycles()) { 
			Map<T, AtomicLong> tallys = new LinkedHashMap<>();
			
			for (T t :  getPossibleValues()) {			 
				tallys.put(  t,new AtomicLong(count(cycle, t)));	
			}
			
			XmlUtils.outputCycleTallyGroup( ele, String.valueOf( cycle ), tallys, false );	
		}		
	}	

	// xu totalSize should be seprate to first and second of pair		
	public Map<Integer, AtomicLong> getLengthMapFromCycle() { 
		Map<Integer, AtomicLong> map = new HashMap<Integer, AtomicLong>();		
		
		long previousTally = -1;	
		Integer last = 0;
		for (Integer cycle : cycles()) { 
		
 			long sum = 0;
			for (T t :  getPossibleValues()) {				
				  sum += count(cycle, t) ;	
			}
			// only for the begin of cycle
			if (previousTally == -1) {
				previousTally = sum;
			}
			if (sum != previousTally) { 
				// record one cycle advance if base counts difference
				// previousTally - count = the number of short reads				
				map.put(cycle - 1, new AtomicLong(previousTally - sum ));
				previousTally = sum;
			}
			last = cycle; 
		}			
		// pop the last entry into the map
		if (previousTally > 0) {
			map.put( last, new AtomicLong(previousTally));		 
		}
		return map;
	}
	
	public long getInputCounts() { 
		return parseCounts.get();
	}
	
	public void parseByteData(final byte[] dataString) { 
		if (dataString == null) {
			return;
		}
		parseCounts.incrementAndGet();
 			
		for (int i = 0, size = dataString.length ; i < size ; i++) { 
			int value = (type instanceof Integer) ? dataString[i] & 0xFF : 
				(type instanceof Character) ? dataString[i] : 0;
			increment( i + 1, value );
		}						
	}	
	
	public void parseStringData( String dataString,  int offset) { 
		if (null == dataString) {
			return; 
		}
		parseCounts.incrementAndGet();
		
		int size = dataString.length();
		if (size > 0) { 				
			// set offset to 0 if it is negative, or larger than the supplied string
			if (offset < 0 || offset >= size) {
				offset = 0;				
			}
			for (int i = 1 + offset ; i <= size; i++) { 
				increment(i - offset, dataString.charAt(i - 1));
			}			 
		}	
	}	

}
