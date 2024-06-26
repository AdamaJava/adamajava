/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.qcmg.common.model.Rule;
import org.qcmg.snp.SnpException;

public class RulesUtil {
	
	/**
	 * Method that selects a Rule from a list of Rules, that should be applied at the specified coverage level
	 * <p>
	 *  
	 * @param rules {@link List} of {@link Rule}s from which a suitable rule will be returned
	 * @param coverage int that specifies the 
	 * @return Rule that applies to the specified coverage, null if such a rule does not exist in the supplied list
	 */
	public static Rule getRule(List<Rule> rules, int coverage) {
		for (Rule r : rules) {
			if (r.maxCoverage() >= coverage && r.minCoverage() <= coverage) {
				return r;
			}
		}
		return null;
	}
	
	public static String examineRules(List<Rule> rules) throws SnpException {
		if (null == rules || rules.isEmpty())
			throw new IllegalArgumentException("null or empty list of rules passed to examineRules");
		
		// sort collection by min value
		Collections.sort(rules, new Comparator<Rule>() {
			@Override
			public int compare(Rule o1, Rule o2) {
				return o1.minCoverage() - o2.minCoverage();
			}
		});
		
		StringBuilder sb = new StringBuilder();
		
		int previousMaxValue = -1;
		for (Rule r : rules) {
			if (r.minCoverage() <= previousMaxValue) {
				throw new SnpException("RULES_OVERLAP");
			} else if (r.minCoverage() > previousMaxValue + 1) {
				sb.append("rules don't cover from " + (previousMaxValue + 1) + " to " + r.minCoverage() + "\n");
			}
//			previousMinValue = r.getMinCoverage();
			previousMaxValue = r.maxCoverage();
		}
		
		// check boundaries
		if (rules.get(0).minCoverage() > 0) {
			String errorMessage = "rules don't cover from 0 to " + rules.get(0).minCoverage();
			if ( ! sb.toString().contains(errorMessage)) {
				sb.append("rules don't cover from 0 to " + rules.get(0).minCoverage() + "\n");
			}
		}
		if (previousMaxValue < Integer.MAX_VALUE) {
			sb.append("rules don't cover from " + (previousMaxValue +1) + " to Integer.MAX_VALUE\n");
		}
		return sb.length() > 0 ? sb.toString().trim() : null;
	}

}
