/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qprofiler.summarise;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Class that tallies by cycle using java generics 
 * 
 * 
 * 
 */

public class SummaryByCycle<T> {

	// can be set in the constructor, but defaults to the Java default value
	private int subMapInitialCapacity = 16;
	
	private final ConcurrentMap<Integer, ConcurrentMap<T, AtomicLong>> tally;
	
	// default constructor
	public SummaryByCycle() {
		tally = new ConcurrentHashMap<Integer, ConcurrentMap<T, AtomicLong>>();
	}
	
	public SummaryByCycle(int initialCapacity) {
		tally = new ConcurrentHashMap<Integer, ConcurrentMap<T, AtomicLong>>(initialCapacity);
	}
	
	public SummaryByCycle(int initialCapacity, int subMapCapacity) {
		tally = new ConcurrentHashMap<Integer, ConcurrentMap<T, AtomicLong>>(initialCapacity);
		this.subMapInitialCapacity = subMapCapacity;
	}
	
	public SummaryByCycle(ConcurrentMap<Integer, ConcurrentMap<T, AtomicLong>> tally) {
		this.tally = tally;
	}

	public ConcurrentMap<T, AtomicLong> getValue(Integer key) {
		return tally.get(key);
	}
	
	/**
	 * Increments the count of a particular value, for a particular cycle.
	 * <p>If the cycle does not exist, it will be added to the summary.
	 * <p>If the value at the specified cycle does not exist, it will be added to the map for that cycle.
	 * 
	 * @param cycle Integer the cycle that is being incremented
	 * @param value <T> the value for the specified cycle whose count is to b incremented
	 */
	public void increment(final Integer cycle, final T value) {
		
		// Make sure we are initialized for this cycle and thingy
		ConcurrentMap<T, AtomicLong> map = tally.get(cycle);
		if (null ==map) {
			final ConcurrentMap<T, AtomicLong> newMap = new ConcurrentHashMap<T, AtomicLong>(subMapInitialCapacity);
			newMap.put(value, new AtomicLong(1));
			map = tally.putIfAbsent(cycle, newMap);
			if (null == map)
				return;
		}
		
		AtomicLong currentCount = map.get(value);
		if (null == currentCount) {
			currentCount = map.putIfAbsent(value, new AtomicLong(1));
			if (null == currentCount)
				return;
		}
		currentCount.incrementAndGet();
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
	public AtomicLong count(Integer cycle, T value) {
		// Make sure we are initialized for this cycle and base
		if (tally.containsKey(cycle) && tally.get(cycle).containsKey(value)) {
			return tally.get(cycle).get(value);
		}
		return new AtomicLong(0);
	}
	
	/**
	 * Returns a SortedSet relating to the cycles currently held by this summary object.
	 * 
	 * @return SortedSet<T> containing all of the cycles for this summary object
	 */
	public SortedSet<Integer> cycles() {
		return (new TreeSet<Integer>(tally.keySet()));
	}

	/**
	 * Returns a SortedSet of elements <T> for a particular cycle of this summary object.
	 * <p>
	 * Returns null if the summary does not contain the cycle
	 * 
	 * @param cycle Integer cycle for which summary details should be returned
	 * @return SortedSet<T>  relating to the values for a particular cycle for this summary object
	 */
	public SortedSet<T> values(Integer cycle) {
		if (tally.containsKey(cycle)) {
			return (new TreeSet<T>(tally.get(cycle).keySet()));
		}
		return null;
	}
	
	public Set<T> getPossibleValues() {
		
		Set<T> allValues = new TreeSet<T>();
		Set<Integer> cycles = cycles();
		for (Integer cycle : cycles) {
			Set<T> values = values(cycle);
			allValues.addAll(values);
		}
		
		return allValues;
	}
	
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
	
	public boolean isEmpty() {
		return tally.isEmpty();
	}

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
				for (T value : values(cycle)) {
					Element tallyE = doc.createElement("TallyItem");
					tallyE.setAttribute("value", value.toString());
					tallyE.setAttribute("count", count(cycle, value).get()+"");
					cycleE.appendChild(tallyE);
				}
			}
		}
		catch (DOMException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns a tab seperated StringBuffer relating to all the elements in this sumary object.
	 * 
	 * @return StringBuffer relating to this objects elements, tab seperated.
	 */
	public StringBuffer tsvReport() {
		StringBuffer buffer = new StringBuffer("");

		// We need sorted lists of all cycles and bases
		SortedSet<Integer> cycles = new TreeSet<Integer>();
		SortedSet<T> values = new TreeSet<T>();
		for (Integer cycle : cycles()) {
			cycles.add(cycle);
			values.addAll(values(cycle));
		}

		// Header line
		buffer.append("Cycle");
		for (T value : values) {
			buffer.append("\t" + value);
		}
		buffer.append("\n");

		for (Integer cycle : cycles) {
			buffer.append(cycle);
			for (T value : values) {
				buffer.append("\t" + count(cycle, value));
			}
			buffer.append("\n");
		}
		return (buffer);
	}

}
