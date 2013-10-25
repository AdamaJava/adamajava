package org.qcmg.common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

public class PileupElementComparatorTest {

	@Test
	public void testCompare() {
		// sorts on size, ref, qual score
		List<PileupElement> pileups = new ArrayList<PileupElement>();
		PileupElement pe1 = new PileupElement('A');
		pe1.incrementForwardCount();
		PileupElement pe2 = new PileupElement('B');
		for (int i = 0 ; i < 2 ; i++) pe2.incrementForwardCount();
		PileupElement pe3 = new PileupElement('C');
		for (int i = 0 ; i < 3 ; i++) pe3.incrementForwardCount();
		pileups.add(pe1);
		pileups.add(pe2);
		pileups.add(pe3);
		
		Collections.sort(pileups, new PileupElementComparator());
		
		Assert.assertEquals(pe3, pileups.get(0));
		Assert.assertEquals(pe2, pileups.get(1));
		Assert.assertEquals(pe1, pileups.get(2));
		
		// has same count as pe2 but contains the reference, and so it gets bumped up
		PileupElement pe4 = new PileupElement('.');
		pe4.incrementForwardCount();
		pe4.incrementReverseCount();
		pileups.add(pe4);
		
		Collections.sort(pileups, new PileupElementComparator());
		
		Assert.assertEquals(pe3, pileups.get(0));
		Assert.assertEquals(pe4, pileups.get(1));
		Assert.assertEquals(pe2, pileups.get(2));
		Assert.assertEquals(pe1, pileups.get(3));
	}
	
	@Test
	public void testCompareQuals() {
		// sorts on size, ref, qual score
		List<PileupElement> pileups = new ArrayList<PileupElement>();
		PileupElement pe1 = new PileupElement('A');
		pe1.incrementForwardCount((byte) 10);
		PileupElement pe2 = new PileupElement('B');
		pe2.incrementReverseCount((byte) 11);
		PileupElement pe3 = new PileupElement('C');
		pe3.incrementForwardCount((byte) 9);
		pileups.add(pe1);
		pileups.add(pe2);
		pileups.add(pe3);
		
		Collections.sort(pileups, new PileupElementComparator());
		
		Assert.assertEquals(pe2, pileups.get(0));
		Assert.assertEquals(pe1, pileups.get(1));
		Assert.assertEquals(pe3, pileups.get(2));
	}
}
