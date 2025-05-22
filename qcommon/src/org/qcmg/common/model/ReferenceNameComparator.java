/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;

public class ReferenceNameComparator implements Comparator<String> , Serializable {

	@Serial
	private static final long serialVersionUID = 3528840046906334666L;

	private static final Map<String, Integer> STANDARD_CONTIGS = Map.ofEntries(
			// Numeric chromosomes
			Map.entry("1", 1),
			Map.entry("2", 2),
			Map.entry("3", 3),
			Map.entry("4", 4),
			Map.entry("5", 5),
			Map.entry("6", 6),
			Map.entry("7", 7),
			Map.entry("8", 8),
			Map.entry("9", 9),
			Map.entry("10", 10),
			Map.entry("11", 11),
			Map.entry("12", 12),
			Map.entry("13", 13),
			Map.entry("14", 14),
			Map.entry("15", 15),
			Map.entry("16", 16),
			Map.entry("17", 17),
			Map.entry("18", 18),
			Map.entry("19", 19),
			Map.entry("20", 20),
			Map.entry("21", 21),
			Map.entry("22", 22),
			// Special chromosomes
			Map.entry("X", 23),
			Map.entry("Y", 24),
			Map.entry("M", 25),
			Map.entry("MT", 25)
	);

	@Override
	public int compare(String s1, String s2) {

		// Early equality check
		if (s1.equals(s2)) return 0;

		// Strip "chr" prefix if present
		s1 = s1.startsWith("chr") ? s1.substring(3) : s1;
		s2 = s2.startsWith("chr") ? s2.substring(3) : s2;

		// Check equality after stripping prefix
		if (s1.equals(s2)) return 0;

		int s1Index = STANDARD_CONTIGS.getOrDefault(s1, 0);
		int s2Index = STANDARD_CONTIGS.getOrDefault(s2, 0);
		// Handle special chromosomes
		if (s1Index != 0 && s2Index != 0) {
			return Integer.compare(s1Index, s2Index);
		} else if (s1Index != 0) {
			return -1;
		} else if (s2Index != 0) {
			return 1;
		}

		if (s1.length() > 2 && s2.length() > 2) {
			if (Character.isDigit(s1.charAt(0)) && Character.isDigit(s2.charAt(0))) {
				int i1 = Integer.parseInt(getIntegerFromString(s1));
				int i2 = Integer.parseInt(getIntegerFromString(s2));
				if (i1 != i2) {
					return Integer.compare(i1, i2);
				}
			}
		}

		// Default string comparison for all other cases
		return s1.compareToIgnoreCase(s2);
	}
	
	private String getIntegerFromString(String string) {
		// assume number is at beginning of string
		// eg 17_ctg5_hap1
		 
		StringBuilder returnString = new StringBuilder();
		for (int i = 0, len = string.length() ; i < len; i++) {
			char c = string.charAt(i);
			if (Character.isDigit(c)) returnString.append(c);
			else break;
		}
		return returnString.toString();
	}
}
