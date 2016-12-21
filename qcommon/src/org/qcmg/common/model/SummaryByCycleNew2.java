/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class that tallies by cycle using java generics 
  */

public class SummaryByCycleNew2<T> {
	
	private static final int MAX_ARRAY_CAPACITY = 2048 * 2048;		// over 4 million
	
	
	/**
	 * Quality scores are stored as ASCII representation of Phred-scale base quality + 33
	 * From SAM spec 1.4:
	 * [!-~]+
	 * ! has an ascii score of 33
	 * ~ has an ascii score of 126
	 */
	private static final int DEFAULT_NO_OF_KEYS_INTEGER = 128;
	
	/**
	 * Sequence data
	 * From SAM spec 1.4:
	 * \*|[A-Za-z=.]+
	 * * has an ascii score of 42
	 * z has an ascii score of 122
	 */
	private static final int DEFAULT_NO_OF_KEYS_CHARACTER = 128;
		
	// atomic boolean used as a lock when resizing the array
	private final AtomicBoolean resizingInProgress = new AtomicBoolean(false);
	
	// can be set in the constructor, but defaults to the Java default value
	private final  T type;
	private final AtomicInteger cycleMask = new AtomicInteger();
	private final AtomicInteger keyMask = new AtomicInteger();
	private final AtomicInteger maxCycleValue = new AtomicInteger();
	private final AtomicInteger maxKeyValue = new AtomicInteger();
	
	private AtomicLongArray tally;
	
	/**
	 * Constructs a new SummaryByCycleNew2 object with the specified noOfCycles size, 
	 *  and the default value for the noOfKeys, which is dependent on the supplied type T
	 *  
	 *  Calls @see SummaryByCycleNew2
	 * @param type
	 * @param noOfCycles
	 */
	public SummaryByCycleNew2(T type, final int noOfCycles) {
		this(type, noOfCycles, type instanceof Character 
				? DEFAULT_NO_OF_KEYS_CHARACTER : DEFAULT_NO_OF_KEYS_INTEGER);
	}
	
	
	public SummaryByCycleNew2(T type, final int noOfCycles, int noOfKeys) {
		this.type = type;
		
		cycleMask.set(getMask(noOfCycles));
		keyMask.set(getMask(noOfKeys));
		int capacity = 1 << (cycleMask.get() + keyMask.get());
		
		maxCycleValue.set((1<<cycleMask.get()) -1);
		maxKeyValue.set((1<<keyMask.get()) -1);
		
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
	
	public int[] getCycleKeyFromArrayPosition(int position) {
		int key = ( position >>> cycleMask.get());
		int cycle  = position & (( 1 << cycleMask.get()) - 1);
		return new int[] {cycle,key};
	}
	
	public ConcurrentMap<T, AtomicLong> getValue(Integer key) {
		ConcurrentMap<T, AtomicLong> cm = new ConcurrentHashMap<T, AtomicLong>();
		// loop through keys to get ones with this cycle number
		
		for (int i = 0 , length = tally.length() ; i < length ; i++) {
			long arrayValue = tally.get(i);
			// whole bunch of zeros in here - only want >0 values
			if (arrayValue > 0) {
				int [] cycleKey = getCycleKeyFromArrayPosition(i);
				if (cycleKey[0] == key.intValue()) {
					cm.put(getTypeFromLong(cycleKey[1]), new AtomicLong(arrayValue));
				}
			}
		}
		
		return cm;
	}
	
	private T getTypeFromLong(int l) {
		if (type instanceof Integer)
			return (T)Integer.valueOf(l);
		if (type instanceof Character)
			return (T)Character.valueOf((char)l);
		else return null;
	}
	
	/**
	 * Increments the count of a particular value, for a particular cycle.
	 * <p>If the cycle does not exist, it will be added to the summary.
	 * <p>If the value at the specified cycle does not exist, it will be added to the map for that cycle.
	 * 
	 * @param cycle Integer the cycle that is being incremented
	 * @param key <T> the value for the specified cycle whose count is to b incremented
	 */
	public void increment(final int cycle, final int key) {
		if (cycle > maxCycleValue.get() || key > maxKeyValue.get())
			resize(cycle, key);
		
		if (resizingInProgress.get()) {
			// CAS until resizing is complete
			while  ( ! resizingInProgress.compareAndSet(false, false) ) {}
		}
		tally.incrementAndGet(getArrayPosition(cycle, key));
	}
	
	
	
	private void resize(final int cycle, final int key) {
		
		// find out if it is the cycle, key or both that are larger than current maximums
		// lock
		while ( ! resizingInProgress.compareAndSet(false, true)) {}
		try {	
			// only resize if we still need to
			// another thread may have already done this for us
			boolean resize = false;	
			
			// re-set current maximums
			int newCycleMask = -1;
			if (cycle > maxCycleValue.get()) {
				newCycleMask = getMask(cycle); 
				maxCycleValue.set((1<<newCycleMask) -1);
				resize = true;
			}
			if (key > maxKeyValue.get()) {
				keyMask.set(getMask(key)); 
				maxKeyValue.set((1<<keyMask.get()) -1);
				resize = true;
			}
			
			if (resize) {
				
				// sleep to allow any threads in CAS loop in increment method to complete
				// resizing should not occur often so will not be too great a performance penalty
				// and it SHOULD mean we keep the array in a consistent state 
				Thread.sleep(200);
				
				
				System.out.println("About to resize tally...");
				// get new capacity
				int capacity = 1 << ((newCycleMask == -1 ? cycleMask.get() : newCycleMask) + keyMask.get());
				
				// check on new capacity
				if (capacity > MAX_ARRAY_CAPACITY)
					throw new IllegalArgumentException("new AtomicLongArray is too large...");
				
				
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
				if (newCycleMask > -1) cycleMask.set(newCycleMask);
			}	
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			//unlock
			while ( ! resizingInProgress.compareAndSet(true, false)) {}
		}
	}
	
	/**
	 * Returns the number of times a particular value has occurred at a particular cycle.
	 * <p>
	 * Returns 0 if the cycle does not exist,  or if the cycle does not contain the specified value.
	 * 
	 * @param cycle Integer representing the cycle number
	 * @param value <T> value for which we want a count, at a particular cycle
	 * @return Integer relating to the count of .Defaults to zero if the cycle does not exist, 
	 * or if the cycle does not contain the specified value <T>
	 */
	public AtomicLong count(Integer cycle, long value) {
		return new AtomicLong(tally.get(getArrayPosition(cycle, (int)value)));
	}
	
	/**
	 * Returns a SortedSet relating to the cycles currently held by this summary object.
	 * 
	 * @return SortedSet<T> containing all of the cycles for this summary object
	 */
	public SortedSet<Integer> cycles() {
		TreeSet<Integer> ts = new TreeSet<Integer>();
		
		for (int i = 0 , length = tally.length() ; i < length ; i++) {
			long tallyValue = tally.get(i);
			if (tallyValue > 0) {
				int [] cycleKey = getCycleKeyFromArrayPosition(i);
				ts.add(cycleKey[0]);
			}
		}
		
		return ts;
	}
	
	/**
	 * Returns a SortedSet of elements <T> for a particular cycle of this summary object.
	 * <p>
	 * Returns null if the summary does not contain the cycle
	 * 
	 * @param cycle Integer cycle for which summary details should be returned
	 * @return SortedSet<T>  relating to the values for a particular cycle for this summary object
	 */
	public SortedSet<Long> values(Integer cycle) {
		TreeSet<Long> ts = new TreeSet<Long>();
		
		for (int i = 0 , length = tally.length() ; i < length ; i++) {
			long tallyValue = tally.get(i);
			if (tallyValue > 0) {
				int [] cycleKey = getCycleKeyFromArrayPosition(i);
				if (cycleKey[0] == cycle.intValue())
					ts.add(Long.valueOf(cycleKey[1]));
					
			}
		}
		
		return ts;
	}
//	public SortedSet<T> values(Integer cycle) {
//		TreeSet<T> ts = new TreeSet<T>();
//		for (Long cv : tally.get(cycle.longValue()).keySet()) {
//			ts.add(getTypeFromLong(cv));
//		}
//		return ts;
//	}
	
	public Set<T> getPossibleValues() {
		
		Set<T> allValues = new TreeSet<T>();
		Set<Integer> cycles = cycles();
		for (Integer cycle : cycles) {
			Set<Long> values = values(cycle);
			for (Long l : values) {
				allValues.add(getTypeFromLong(l.intValue()));
			}
//			allValues.addAll(values);
		}
		
		return allValues;
	}
//	
	public String getPossibleValuesAsString() {
		StringBuilder sb = new StringBuilder();
		for (T t : getPossibleValues()) {
			sb.append(t);
			sb.append(",");
		}
		if (sb.length() > 0)
			return sb.substring(0, sb.length()-1);
		return null;
	}
//	
//	public boolean isEmpty() {
//		return tally.isEmpty();
//	}
	
	/**
	 * Adds an xml representation of the current object to the supplied parent element.
	 * 
	 * @param parent Element that the current objects xml representation will be added to
	 * @param elementName String representing the name to be used when creating the element
	 */
	public void toXml( Element parent, String elementName ) {
		Document doc = parent.getOwnerDocument();
		Element element = doc.createElement(elementName);
		parent.appendChild(element);
		
		// adding another level to conform to DTD..
		Element cycleTallyElement = doc.createElement("CycleTally");
		element.appendChild(cycleTallyElement);
		cycleTallyElement.setAttribute("possibleValues", getPossibleValuesAsString());
		Element possValuesE = doc.createElement("PossibleValues");
		for (T t : getPossibleValues()) {
			Element valueE = doc.createElement("Value");
			valueE.setAttribute("value", t.toString());
			possValuesE.appendChild(valueE);
		}
		possValuesE.setAttribute("possibleValues", getPossibleValuesAsString());
		cycleTallyElement.appendChild(possValuesE);
		
		try {
			for (Integer cycle : cycles()) {
				Element cycleE = doc.createElement("Cycle");
				cycleE.setAttribute("value", cycle.toString());
				cycleTallyElement.appendChild(cycleE);
//				element.appendChild(cycleE);
				for (Long value : values(cycle)) {
//					for (T value : values(cycle)) {
					Element tallyE = doc.createElement("TallyItem");
					tallyE.setAttribute("value", getTypeFromLong(value.intValue()).toString());
					tallyE.setAttribute("count", count(cycle, value).get() + "");
//					tallyE.setAttribute("count", count(cycle, value).toString());
					cycleE.appendChild(tallyE);
				}
			}
		}
		catch (DOMException e) {
			e.printStackTrace();
		}
	}

}
