package org.qcmg.common.util;

import java.util.Comparator;
import java.util.List;

public class ListUtils {
	
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

}
