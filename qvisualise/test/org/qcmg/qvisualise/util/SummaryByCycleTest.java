package org.qcmg.qprofiler.util;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Assert;

import org.junit.Test;
import org.qcmg.qvisualise.util.SummaryByCycle;
import org.qcmg.qvisualise.util.SummaryByCycleUtils;

public class SummaryByCycleTest<T> {
	
	private static final String testInput = "ThisIsATestInputString";
	
	
	@Test
	public void testValues() {
		SummaryByCycle<Character> sbc = new SummaryByCycle<Character>();
		SummaryByCycleUtils.parseCharacterSummary(sbc, testInput);
		
		// should only have 1 entry in each set for each cycle
		for (int i = 1, j = testInput.length() ; i <= j ; i++) {
			Assert.assertEquals(1, sbc.values(i).size());
			// check that the correct chars are sitting in the correcy cycle position
			Assert.assertEquals(testInput.charAt(i-1), sbc.values(i).first().charValue());
		}
		
		Assert.assertNull(sbc.values(testInput.length() + 1));
	}
	
	@Test
	public void testGetPossibleValues() {
		SummaryByCycle<Character> sbc = new SummaryByCycle<Character>();
		SummaryByCycleUtils.parseCharacterSummary(sbc, testInput);
		
		Set<Character> possibleValues = sbc.getPossibleValues();
		
		// no of unique characters in testInput
		Set<Character> uniqueTestInputChars = new TreeSet<Character>();
		for (int i = 0, j = testInput.length() ; i < j ; i++) {
			uniqueTestInputChars.add(testInput.charAt(i));
		}
		
		Assert.assertEquals(uniqueTestInputChars.size(), possibleValues.size());
		for (int i = 0, j = testInput.length() ; i < j ; i++) {
			Assert.assertTrue(possibleValues.contains(testInput.charAt(i)));
		}
	}
	
	@Test
	public void testGetPossibleValuesAsString() {
		SummaryByCycle<Character> sbc = new SummaryByCycle<Character>();
		SummaryByCycleUtils.parseCharacterSummary(sbc, testInput);
		
		String possibleValuesString = sbc.getPossibleValuesAsString();
		
		// no of unique characters in testInput
		Set<Character> uniqueTestInputChars = new TreeSet<Character>();
		for (int i = 0, j = testInput.length() ; i < j ; i++) {
			uniqueTestInputChars.add(testInput.charAt(i));
		}
		
		List<String> possibleValuesStringList = Arrays.asList(possibleValuesString.split(","));
		
		Assert.assertEquals(uniqueTestInputChars.size(), possibleValuesStringList.size());
		
		for (int i = 0, j = testInput.length() ; i < j ; i++) {
			Assert.assertTrue(possibleValuesStringList.contains(String.valueOf(testInput.charAt(i))));
		}
	}
}
