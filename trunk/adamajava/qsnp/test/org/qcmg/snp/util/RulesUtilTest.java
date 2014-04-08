package org.qcmg.snp.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.common.model.Rule;

public class RulesUtilTest {
	@Test
	public void testGetRule() {
		Rule r1 = new Rule(0, 1, 1);
		List<Rule> rules = new ArrayList<Rule>();
		rules.add(r1);
		
		Assert.assertEquals(r1, RulesUtil.getRule(rules, 0));
		Assert.assertEquals(r1, RulesUtil.getRule(rules, 1));
		Assert.assertNull(RulesUtil.getRule(rules, 2));
		
		// add another rule with the same details - should return the original rule, as it will come to it first in the list
		Rule r2 = new Rule(0, 1, 1);
		rules.add(r2);
		Assert.assertEquals(r1, RulesUtil.getRule(rules, 0));
		Assert.assertEquals(r1, RulesUtil.getRule(rules, 1));
		Assert.assertNull(RulesUtil.getRule(rules, 2));
		
		Rule r3 = new Rule(1, 2, 1);
		rules.add(r3);
		Assert.assertEquals(r1, RulesUtil.getRule(rules, 0));
		Assert.assertEquals(r1, RulesUtil.getRule(rules, 1));
		Assert.assertEquals(r3, RulesUtil.getRule(rules, 2));
		Assert.assertNull(RulesUtil.getRule(rules, 3));
		
		Rule r4 = new Rule(2, Integer.MAX_VALUE, 5);
		rules.add(r4);
		Assert.assertEquals(r4, RulesUtil.getRule(rules, 123456789));
	}
	
	@Ignore
	public void testGetRuleCache() {
		int noOfLoops = 100000000;
		
		Rule r1 = new Rule(0,20,3);
		Rule r2 = new Rule(21,50,4);
		Rule r3 = new Rule(50,Integer.MAX_VALUE,5);
		
		List<Rule> rules = new ArrayList<Rule>();
		rules.add(r1);
		rules.add(r2);
		rules.add(r3);
		
		Random random = new Random(1);
		Random randomCache = new Random(1);
		
		Map<Integer, Rule> cache = new HashMap<Integer, Rule>();
		
		
		long start = System.currentTimeMillis();
		long counter = 0;
		for (int i = 0 ; i < noOfLoops ; i++) {
			int coverage = random.nextInt(100);
			Rule r = RulesUtil.getRule(rules, coverage);
			counter += r.getNoOfVariants();
		}
		System.out.println("getRule: " + (System.currentTimeMillis() - start) + ", counter: " + counter);
		
		start = System.currentTimeMillis();
		counter = 0;
		for (int i = 0 ; i < noOfLoops ; i++) {
			int coverage = randomCache.nextInt(100);
			Rule r = cache.get(coverage);
			if (null == r) {
				r = RulesUtil.getRule(rules, coverage);
				cache.put(coverage, r); 
			}
			counter += r.getNoOfVariants();
		}
		System.out.println("cache: " + (System.currentTimeMillis() - start) + ", counter: " + counter);
	}
	
	
	@Test
	public void testExamineRulesBad() throws Exception {
		try {
			RulesUtil.examineRules(null);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		
		List<Rule> rules = new ArrayList<Rule>();
		try {
			RulesUtil.examineRules(rules);
			Assert.fail("Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException iae) {}
		
		// single rule encompassing whole range
		rules.add(new Rule(0,Integer.MAX_VALUE,0));
		String result = RulesUtil.examineRules(rules);
		Assert.assertEquals(true, result == null);
		
		
		// overlapping rule - should throw an exception
		rules.clear();
		rules.add(new Rule(0,10,0));
		rules.add(new Rule(5,15,0));
		try {
			result = RulesUtil.examineRules(rules);
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {}
		
		// overlapping rule - should throw an exception
		rules.clear();
		rules.add(new Rule(0,10,1));
		rules.add(new Rule(5,7,2));
		try {
			result = RulesUtil.examineRules(rules);
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {}
		
		// overlapping rule - should throw an exception
		rules.clear();
		rules.add(new Rule(0,10,1));
		rules.add(new Rule(10,11,3));
		try {
			result = RulesUtil.examineRules(rules);
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {}
		
		// overlapping rule - should throw an exception
		rules.clear();
		rules.add(new Rule(0,10,1));
		rules.add(new Rule(0,1,1));
		try {
			result = RulesUtil.examineRules(rules);
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {}
		
		// overlapping rule - should throw an exception
		rules.clear();
		rules.add(new Rule(0,21,1));
		rules.add(new Rule(21,50,1));
		try {
			result = RulesUtil.examineRules(rules);
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {}
		
	}
	
	@Test
	public void testExamineRulesFair() throws Exception {
		List<Rule> rules = new ArrayList<Rule>();
		
		// rule starts at coverage > 0
		rules.add(new Rule(10,Integer.MAX_VALUE,0));
		Assert.assertEquals("rules don't cover from 0 to 10", RulesUtil.examineRules(rules));
		
		rules.clear();
		rules.add(new Rule(1,Integer.MAX_VALUE,0));
		Assert.assertEquals("rules don't cover from 0 to 1", RulesUtil.examineRules(rules));
		
		rules.clear();
		rules.add(new Rule(1,2,0));
		Assert.assertEquals("rules don't cover from 0 to 1\nrules don't cover from 3 to Integer.MAX_VALUE", RulesUtil.examineRules(rules));
		
		rules.clear();
		rules.add(new Rule(1,Integer.MAX_VALUE-1,0));
		Assert.assertEquals("rules don't cover from 0 to 1\nrules don't cover from " + (Integer.MAX_VALUE) + " to Integer.MAX_VALUE", RulesUtil.examineRules(rules));
		
//		rules.add(new Rule(0,10,0));
//		rules.add(new Rule(5,15,0));
//		try {
//			result = RulesUtil.examineRules(rules);
//			Assert.fail("Should have thrown an exception");
//		} catch (Exception e) {}
//		
//		// overlapping rule - should throw an exception
//		rules.clear();
//		rules.add(new Rule(0,10,1));
//		rules.add(new Rule(5,7,2));
//		try {
//			result = RulesUtil.examineRules(rules);
//			Assert.fail("Should have thrown an exception");
//		} catch (Exception e) {}
//		
//		// overlapping rule - should throw an exception
//		rules.clear();
//		rules.add(new Rule(0,10,1));
//		rules.add(new Rule(10,11,3));
//		try {
//			result = RulesUtil.examineRules(rules);
//			Assert.fail("Should have thrown an exception");
//		} catch (Exception e) {}
//		
//		// overlapping rule - should throw an exception
//		rules.clear();
//		rules.add(new Rule(0,10,1));
//		rules.add(new Rule(0,1,1));
//		try {
//			result = RulesUtil.examineRules(rules);
//			Assert.fail("Should have thrown an exception");
//		} catch (Exception e) {}
		
	}
}
