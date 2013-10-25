package org.qcmg.common.model;


import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.Rule;

public class RuleTest {

	@Test
	public void testConstructorInvalid() {
		// dodgy ones first
		try {
			new Rule(1,0,0);
			Assert.fail("Should have thrown an IllegalArgumentExcpetion");
		} catch (IllegalArgumentException e) {}
		
		try {
			new Rule(1,1,2);
			Assert.fail("Should have thrown an IllegalArgumentExcpetion");
		} catch (IllegalArgumentException e) {}
		
		try {
			new Rule(0,10,11);
			Assert.fail("Should have thrown an IllegalArgumentExcpetion");
		} catch (IllegalArgumentException e) {}
		
		try {
			new Rule(-1,10,1);
			Assert.fail("Should have thrown an IllegalArgumentExcpetion");
		} catch (IllegalArgumentException e) {}
		
		try {
			new Rule(Integer.MIN_VALUE,Integer.MAX_VALUE,0);
			Assert.fail("Should have thrown an IllegalArgumentExcpetion");
		} catch (IllegalArgumentException e) {}
	}
	
	@Test
	public void testConstructorValid() {
		// lets try some reasonable ones now
		Rule r = new Rule(0,0,0);
		Assert.assertNotNull(r);
		
		r = new Rule(-0,-0,-0);
		Assert.assertNotNull(r);
		
		r = new Rule(0,-0,-0);
		Assert.assertNotNull(r);
		
		r = new Rule(0,Integer.MAX_VALUE,1);
		Assert.assertNotNull(r);
		
		r = new Rule(0,Integer.MAX_VALUE,-1);
		Assert.assertNotNull(r);
		
		r = new Rule(0,Integer.MAX_VALUE,Integer.MAX_VALUE);
		Assert.assertNotNull(r);
		
		r = new Rule(Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE);
		Assert.assertNotNull(r);
	}
}
