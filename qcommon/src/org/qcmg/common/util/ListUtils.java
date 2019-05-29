/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import java.util.Comparator;
import java.util.List;

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
		return removeAdjacentPositionsInList(originalList,ADJACENT_POSITION_BUFFER );
	}
	public static TLongList removeAdjacentPositionsInList(TLongList originalList, int buffer) {
		TLongList list = new TLongArrayList();
		if (null != originalList) {
			int size = originalList.size();
			if (size > 0) {
				originalList.sort();
				long current = originalList.get(0);
				list.add(current);
				long previous = current;
				for (int i = 1 ;  i < size ; i++) {
					current = originalList.get(i);
					if (current - previous > buffer) {
						list.add(current);
						previous = current;
					}
				}
			}
		}
		return list;
	}
//	public static TLongList removeAdjacentPositionsInList(TLongList originalList, int buffer) {
//		TLongSet set = new TLongHashSet(originalList.size() * 2);
//		originalList.sort();
//		for (long l : originalList.toArray()) {
//			boolean noAdjacentPositions = true;
//			for (int i = 1 ; i <= buffer ; i++) {
//				if (set.contains(l + i) || set.contains(l - i)) {
//					noAdjacentPositions = false;
//				}
//			}
//			if (noAdjacentPositions) set.add(l);
//		}
//		
//		return new TLongArrayList(set);
//	}

}
