/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qvisualise.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CycleDetailUtils {

	private static List<String> seqCycle;
	private static List<String> tagCSNumericCycle;
	private static List<String> qualCycle;
	private static List<String> qualCycleASCII;
	private static List<String> qualFileCycle;

	public static List<String> getTagCSNumericCycle() {
		if (null == tagCSNumericCycle) {
			createNumericCycle();
		}
		return tagCSNumericCycle;
	}
	
	public static List<String> getSeqCycle() {
		if (null == seqCycle) {
			createSPSeqCycle();
		}
		return seqCycle;
	}

	public static List<String> getQualCycle() {
		if (null == qualCycle) {
			createSPQualCycle();
		}
		return qualCycle;
	}
	
	public static List<String> getQualCycleASCII() {
		if (null == qualCycleASCII) {
			createSPQualCycleASCII();
		}
		return qualCycleASCII;
	}
	
	public static List<String> getQualFileCycle() {
		if (null == qualFileCycle) {
			createQualFileCycle();
		}
		return qualFileCycle;
	}

	// ////////////////////////////////////////////////////////////////////////
	// Private methods
	// ////////////////////////////////////////////////////////////////////////

	private static void createSPSeqCycle() {
		seqCycle = Arrays.asList("A","C","G","T","N");
	}
	
	private static void createNumericCycle() {
		tagCSNumericCycle = Arrays.asList("0","1","2","3",".");
	}

	private static void createSPQualCycleASCII() {
		qualCycleASCII = new ArrayList<>(42);
		for (int i = 40 ; i >= 0 ; i--) {
			qualCycleASCII.add(i+"");
		}
	}
	
	private static void createSPQualCycle() {
		qualCycle = new ArrayList<>(46);
		
		qualCycle.add("j");
		qualCycle.add("i");
		qualCycle.add("h");
		qualCycle.add("g");
		qualCycle.add("f");
		qualCycle.add("e");
		qualCycle.add("d");
		qualCycle.add("c");
		qualCycle.add("b");
		qualCycle.add("a");
		qualCycle.add("`");
		qualCycle.add("_");
		qualCycle.add("^");
		qualCycle.add("]");
		qualCycle.add("\\");
		qualCycle.add("[");
		qualCycle.add("Z");
		qualCycle.add("Y");
		qualCycle.add("X");
		qualCycle.add("W");
		qualCycle.add("V");
		qualCycle.add("U");
		qualCycle.add("T");
		qualCycle.add("S");
		qualCycle.add("R");
		qualCycle.add("Q");
		qualCycle.add("P");
		qualCycle.add("O");
		qualCycle.add("N");
		qualCycle.add("M");
		qualCycle.add("L");
		qualCycle.add("K");
		qualCycle.add("J");
		qualCycle.add("I");
		qualCycle.add("H");
		qualCycle.add("G");
		qualCycle.add("F");
		qualCycle.add("E");
		qualCycle.add("D");
		qualCycle.add("C");
		qualCycle.add("B");
		
	}
	
	private static void createQualFileCycle() {
		qualFileCycle = new ArrayList<>(46);
		for (int i = 42 ; i >=-1 ; i--) {
			qualFileCycle.add(i+"");
		}
	}

}
