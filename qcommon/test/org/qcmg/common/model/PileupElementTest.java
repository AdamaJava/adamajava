package org.qcmg.common.model;

import org.junit.Assert;
import org.junit.Test;

public class PileupElementTest {
	
	@Test
	public void testConstructor() {
		PileupElement pe = new PileupElement('a');
        Assert.assertEquals('a', pe.getBase());
		Assert.assertEquals(0, pe.getForwardCount());
		Assert.assertEquals(0, pe.getReverseCount());
		Assert.assertEquals(0, pe.getTotalCount());
		
		pe = new PileupElement((byte)'c');
        Assert.assertEquals('c', pe.getBase());
		Assert.assertEquals(0, pe.getForwardCount());
		Assert.assertEquals(0, pe.getReverseCount());
		Assert.assertEquals(0, pe.getTotalCount());
		Assert.assertFalse(pe.isFoundOnBothStrands());
	}
	
	@Test
	public void testIncrementor() {
		PileupElement pe = new PileupElement('a');
		pe.incrementForwardCount();
        Assert.assertEquals('a', pe.getBase());
		Assert.assertEquals(1, pe.getForwardCount());
		Assert.assertEquals(0, pe.getReverseCount());
		Assert.assertEquals(1, pe.getTotalCount());
		Assert.assertFalse(pe.isFoundOnBothStrands());
		
		pe.incrementForwardCount();
		Assert.assertEquals(2, pe.getForwardCount());
		Assert.assertEquals(0, pe.getReverseCount());
		Assert.assertEquals(2, pe.getTotalCount());
		Assert.assertFalse(pe.isFoundOnBothStrands());
		
		pe.incrementReverseCount();
		Assert.assertEquals(2, pe.getForwardCount());
		Assert.assertEquals(1, pe.getReverseCount());
		Assert.assertEquals(3, pe.getTotalCount());
		Assert.assertTrue(pe.isFoundOnBothStrands());
	}

}
