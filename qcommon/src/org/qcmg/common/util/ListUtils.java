/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionName;

import gnu.trove.function.TLongFunction;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;

public class ListUtils {
	
	public static final int ADJACENT_POSITION_BUFFER = 2;
	
	/**
	 * Returns a comparator based on the sequence order of the elements in the list
	 * <p>
	 * If the first object being compared is not in the list, then the second object will be placed ahead of the first<br>
	 * If the second object being compared is not in the list, then the first object will be placed ahead of the second.<br>
	 * If both objects are not in the list, the second will be placed ahead of the first<br>
	 * @param <T>
	 * 
	 * @param sortedList List<T> containing the required order for this comparator 
	 * @return Comparator<T> with its sort order based on the sequence of elements in the supplied list
	 */
	public static <T> Comparator<T> createComparatorFromList(final List<T> sortedList) {
		Comparator<T> c = new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				final int index1 = sortedList.indexOf(o1);
				if (index1 == -1) return 1;
				final int index2 = sortedList.indexOf(o2);
				if (index2 == -1) return -1;
				return index1 - index2;
			}
		};
		return c;
	}
	
	/**
	 * returns the first occurrence of the supplied string in the supplied array, or -1 if not present
	 * 
	 * @param array
	 * @param s
	 * @return
	 */
	public static int positionOfStringInArray(String [] array, String s) {
		int i = 0;
		for (String as : array) {
			if (as.equals(s)) {
				return i;
			}
			i++;
		}
		return -1;
	}
	
	public static TLongList removeAdjacentPositionsInList(TLongList originalList) {
		return removeAdjacentPositionsInList(originalList, ADJACENT_POSITION_BUFFER);
	}
	public static TLongList removeAdjacentPositionsInList(TLongList originalList, int buffer) {
		TLongList list = new TLongArrayList();
		if (null != originalList) {
			int size = originalList.size();
			if (size > 1) {
				
				/*
				 * need to sort based on the losition information in the long, whilst also preserving the seq position and strand information
				 * and so conver to list of Long objects where a custom comparator can be applied
				 */
				List<Long> origValueInNewList = new ArrayList<>(size + 1);
				originalList.transformValues(new TLongFunction() {
					@Override
					public long execute(long l) {
						origValueInNewList.add(l);
						return l;
					}
				});
				
				System.out.println("About to sort list of Longs: " + origValueInNewList.stream().map(l -> Long.toString(l)).collect(Collectors.joining(",")));
				origValueInNewList.sort((l1, l2) -> {
					int diff = Long.compare(NumberUtils.getLongPositionValueFromPackedLong(l1), NumberUtils.getLongPositionValueFromPackedLong(l2));
					if (0 == diff) {
						System.out.println("Got same positions in list!!!: " + l1);
					}
					return diff;
				});
		
				long current = origValueInNewList.get(0);
				list.add(current);
				long previous = current;
				for (int i = 1 ;  i < size ; i++) {
					current = origValueInNewList.get(i);
					if (NumberUtils.getLongPositionValueFromPackedLong(current) - NumberUtils.getLongPositionValueFromPackedLong(previous) > buffer) {
						list.add(current);
						previous = current;
					}
				}
			} else if (size == 1) {
				list.add(originalList.get(0));
			}
		}
		return list;
	}
	
	public static List<ChrPosition> removeAdjacentPositionsInList(List<ChrPosition> originalList, int buffer) {
		List<ChrPosition> list = new ArrayList<>();
		if (null != originalList) {
			int size = originalList.size();
			if (size > 1) {
				originalList.sort(null);
				ChrPosition current = originalList.get(0);
				list.add(current);
				ChrPosition previous = current;
				for (int i = 1 ;  i < size ; i++) {
					current = originalList.get(i);
					if ( ( current instanceof ChrPositionName && previous instanceof ChrPositionName && ! ((ChrPositionName)current).getName().equals(((ChrPositionName)previous).getName())) 
							|| ! current.getChromosome().equals(previous.getChromosome()) 
							|| Math.abs(current.getStartPosition() - previous.getStartPosition()) > buffer) { 
						list.add(current);
						previous = current;
					}
				}
			} else if (size == 1) {
				list.add(originalList.get(0));
			}
		}
		return list;
	}

}
