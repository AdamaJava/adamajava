package org.qcmg.qprofiler.ma;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

public class MAComparatorTest {
	
	@Test
	public void testCompareAllInts(){
		List<String> list = new ArrayList<String>();
		list.add("20");
		list.add("21");
		list.add("22");
		list.add("6");
		list.add("2");
		list.add("19");
		list.add("3");
		list.add("1");
		list.add("4");
		list.add("16");
		list.add("15");
		list.add("11");
		list.add("17");
		list.add("10");
		list.add("5");
		list.add("7");
		list.add("13");
		list.add("18");
		list.add("9");
		list.add("8");
		list.add("12");
		list.add("14");
		
		Collections.sort(list, new MAComparator());
		
		int i = 1;
		for (String entry : list) {
			Assert.assertEquals(i++, Integer.parseInt(entry));
		}
	}
	
	@Test
	public void testCompareAllStrings(){
		List<String> list = new ArrayList<String>();
		list.add("q20");
		list.add("q21");
		list.add("q22");
		list.add("q6");
		list.add("q2");
		list.add("q19");
		list.add("q3");
		list.add("q1");
		list.add("q4");
		list.add("q16");
		list.add("q15");
		list.add("q11");
		list.add("q17");
		list.add("q10");
		list.add("q5");
		list.add("q7");
		list.add("q13");
		list.add("q18");
		list.add("q9");
		list.add("q8");
		list.add("q12");
		list.add("q14");
		
		Collections.sort(list, new MAComparator(1));
		
		int i = 1;
		for (String entry : list) {
			Assert.assertEquals(i++, Integer.parseInt(entry.substring(1)));
		}
	}
	
}
