/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qvisualise2.util;

import java.util.ArrayList;
import java.util.List;

@Deprecated
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

	private static List<String> createSPSeqCycle() {
		seqCycle = new ArrayList<String>();

		seqCycle.add("A");
		seqCycle.add("C");
		seqCycle.add("G");
		seqCycle.add("T");
		seqCycle.add("N");

		return seqCycle;
	}
	@Deprecated
	private static List<String> createNumericCycle() {
		tagCSNumericCycle = new ArrayList<String>();
		
		tagCSNumericCycle.add("0");
		tagCSNumericCycle.add("1");
		tagCSNumericCycle.add("2");
		tagCSNumericCycle.add("3");
		tagCSNumericCycle.add(".");
		
		return tagCSNumericCycle;
	}

	private static List<String> createSPQualCycleASCII() {
		qualCycleASCII = new ArrayList<String>();
		
		qualCycleASCII.add("40");
		qualCycleASCII.add("39");
		qualCycleASCII.add("38");
		qualCycleASCII.add("37");
		qualCycleASCII.add("36");
		qualCycleASCII.add("35");
		qualCycleASCII.add("34");
		qualCycleASCII.add("33");
		qualCycleASCII.add("32");
		qualCycleASCII.add("31");
		qualCycleASCII.add("30");
		qualCycleASCII.add("29");
		qualCycleASCII.add("28");
		qualCycleASCII.add("27");
		qualCycleASCII.add("26");
		qualCycleASCII.add("25");
		qualCycleASCII.add("24");
		qualCycleASCII.add("23");
		qualCycleASCII.add("22");
		qualCycleASCII.add("21");
		qualCycleASCII.add("20");
		qualCycleASCII.add("19");
		qualCycleASCII.add("18");
		qualCycleASCII.add("17");
		qualCycleASCII.add("16");
		qualCycleASCII.add("15");
		qualCycleASCII.add("14");
		qualCycleASCII.add("13");
		qualCycleASCII.add("12");
		qualCycleASCII.add("11");
		qualCycleASCII.add("10");
		qualCycleASCII.add("9");
		qualCycleASCII.add("8");
		qualCycleASCII.add("7");
		qualCycleASCII.add("6");
		qualCycleASCII.add("5");
		qualCycleASCII.add("4");
		qualCycleASCII.add("3");
		qualCycleASCII.add("2");
		qualCycleASCII.add("1");
		qualCycleASCII.add("0");
		
		return qualCycleASCII;
	}
//	private static List<String> createSPQualCycleASCII() {
//		qualCycle = new ArrayList<String>();
//		
//		qualCycle.add("42");
//		qualCycle.add("41");
//		qualCycle.add("40");
//		qualCycle.add("39");
//		qualCycle.add("38");
//		qualCycle.add("37");
//		qualCycle.add("36");
//		qualCycle.add("35");
//		qualCycle.add("34");
//		qualCycle.add("33");
//		qualCycle.add("32");
//		qualCycle.add("31");
//		qualCycle.add("30");
//		qualCycle.add("29");
//		qualCycle.add("28");
//		qualCycle.add("27");
//		qualCycle.add("26");
//		qualCycle.add("25");
//		qualCycle.add("24");
//		qualCycle.add("23");
//		qualCycle.add("22");
//		qualCycle.add("21");
//		qualCycle.add("20");
//		qualCycle.add("19");
//		qualCycle.add("18");
//		qualCycle.add("17");
//		qualCycle.add("16");
//		qualCycle.add("15");
//		qualCycle.add("14");
//		qualCycle.add("13");
//		qualCycle.add("12");
//		qualCycle.add("11");
//		qualCycle.add("10");
//		qualCycle.add("9");
//		qualCycle.add("8");
//		qualCycle.add("7");
//		qualCycle.add("6");
//		qualCycle.add("5");
//		qualCycle.add("4");
//		qualCycle.add("3");
//		qualCycle.add("2");
//		
//		return qualCycle;
//	}
	private static List<String> createSPQualCycle() {
		qualCycle = new ArrayList<String>();
		
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
		
		return qualCycle;
	}
	
	private static List<String> createQualFileCycle() {
		qualFileCycle = new ArrayList<String>();
		
		qualFileCycle.add("42");
		qualFileCycle.add("41");
		qualFileCycle.add("40");
		qualFileCycle.add("39");
		qualFileCycle.add("38");
		qualFileCycle.add("37");
		qualFileCycle.add("36");
		qualFileCycle.add("35");
		qualFileCycle.add("34");
		qualFileCycle.add("33");
		qualFileCycle.add("32");
		qualFileCycle.add("31");
		qualFileCycle.add("30");
		qualFileCycle.add("29");
		qualFileCycle.add("28");
		qualFileCycle.add("27");
		qualFileCycle.add("26");
		qualFileCycle.add("25");
		qualFileCycle.add("24");
		qualFileCycle.add("23");
		qualFileCycle.add("22");
		qualFileCycle.add("21");
		qualFileCycle.add("20");
		qualFileCycle.add("19");
		qualFileCycle.add("18");
		qualFileCycle.add("17");
		qualFileCycle.add("16");
		qualFileCycle.add("15");
		qualFileCycle.add("14");
		qualFileCycle.add("13");
		qualFileCycle.add("12");
		qualFileCycle.add("11");
		qualFileCycle.add("10");
		qualFileCycle.add("9");
		qualFileCycle.add("8");
		qualFileCycle.add("7");
		qualFileCycle.add("6");
		qualFileCycle.add("5");
		qualFileCycle.add("4");
		qualFileCycle.add("3");
		qualFileCycle.add("2");
		qualFileCycle.add("1");
		qualFileCycle.add("0");
		qualFileCycle.add("-1");
		
		return qualFileCycle;
	}
//	private static List<String> createSPQualCycle() {
//		spQualCycle = new ArrayList<String>();
//		
//		spQualCycle.add("B");
//		spQualCycle.add("C");
//		spQualCycle.add("D");
//		spQualCycle.add("E");
//		spQualCycle.add("F");
//		spQualCycle.add("G");
//		spQualCycle.add("H");
//		spQualCycle.add("I");
//		spQualCycle.add("J");
//		spQualCycle.add("K");
//		spQualCycle.add("L");
//		spQualCycle.add("M");
//		spQualCycle.add("N");
//		spQualCycle.add("O");
//		spQualCycle.add("P");
//		spQualCycle.add("Q");
//		spQualCycle.add("R");
//		spQualCycle.add("S");
//		spQualCycle.add("T");
//		spQualCycle.add("U");
//		spQualCycle.add("V");
//		spQualCycle.add("W");
//		spQualCycle.add("X");
//		spQualCycle.add("Y");
//		spQualCycle.add("Z");
//		spQualCycle.add("[");
//		spQualCycle.add("\\");
//		spQualCycle.add("]");
//		spQualCycle.add("^");
//		spQualCycle.add("_");
//		spQualCycle.add("`");
//		spQualCycle.add("a");
//		spQualCycle.add("b");
//		spQualCycle.add("c");
//		spQualCycle.add("d");
//		spQualCycle.add("e");
//		spQualCycle.add("f");
//		spQualCycle.add("g");
//		spQualCycle.add("h");
//		spQualCycle.add("i");
//		spQualCycle.add("j");
//		
//		return spQualCycle;
//	}

}
