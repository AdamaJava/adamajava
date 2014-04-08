/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.qcmg.sig.model.Comparison;

public class Quartile {
	
	
	public static double[] quartile(List<? extends Number> numbers) {
		
		double [] numbersArray = new double[numbers.size()];
		int i = 0;
		for (Number n : numbers) {
			numbersArray[i++] = n.doubleValue();
		}
		
		Percentile p = new Percentile();
		p.setData(numbersArray);
		
		return new double[] {p.evaluate(25), p.evaluate(50), p.evaluate(75)};
	}
	
	public static List<Number> getPassingValues(List<? extends Number> numbers, double iQRMultiplier) {
		List<Number> passingValues = new ArrayList<Number>();
		
		// get quartiles
		double [] quartiles = quartile(numbers);
		double iqr = quartiles[2] - quartiles[0];
		double cutoff = quartiles[0] - (iQRMultiplier * iqr);
		
		for (Number n : numbers) {
			if (n.doubleValue() < cutoff) passingValues.add(n.doubleValue());
		}
		
		return passingValues;
	}
	
	/**
	 * calculates the quartiles for the list of numbers, along with the inter quartile range.
	 * Uses these (along with the supplied IQR multiplier) to retuirn a list of numbers (from the supplied list) that have a value less that the cutoff ( first quartile - (iqr * iqr multiplier))
	 * 
	 * @param comparisons
	 * @param iQRMultiplier
	 * @return
	 */
	public static List<Number> getPassingValuesString(List<Comparison> comparisons, double iQRMultiplier) {
//		List<Number> passingValues = new ArrayList<Number>();
		
		// get doubles from string
		List<Double> doubles = new ArrayList<Double>();
		for (Comparison c : comparisons) {
			doubles.add(c.getScore());
		}
		return getPassingValues(doubles, iQRMultiplier);
		
//		// get quartiles
//		double [] quartiles = quartile(doubles);
//		double iqr = quartiles[2] - quartiles[0];
//		double cutoff = quartiles[0] - (iQRMultiplier * iqr);
//		
//		for (Number n : doubles) {
//			if (n.doubleValue() < cutoff) passingValues.add(n.doubleValue());
//		}
//		
//		return passingValues;
	}
//	public static List<Number> getPassingValuesString(List<String> numbers, double iQRMultiplier) {
////		List<Number> passingValues = new ArrayList<Number>();
//		
//		// get doubles from string
//		List<Double> doubles = new ArrayList<Double>();
//		for (String s : numbers) {
//			double d = Double.valueOf(s.substring(0, s.indexOf(":")));
//			doubles.add(d);
//			
//		}
//		return getPassingValues(doubles, iQRMultiplier);
//		
////		// get quartiles
////		double [] quartiles = quartile(doubles);
////		double iqr = quartiles[2] - quartiles[0];
////		double cutoff = quartiles[0] - (iQRMultiplier * iqr);
////		
////		for (Number n : doubles) {
////			if (n.doubleValue() < cutoff) passingValues.add(n.doubleValue());
////		}
////		
////		return passingValues;
//	}

}
