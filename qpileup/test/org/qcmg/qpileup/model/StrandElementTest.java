package org.qcmg.qpileup.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.qcmg.pileup.QPileupException;
import org.qcmg.pileup.model.StrandElement;

public class StrandElementTest {
	
	
	@Test
	public void testAddElement() {
		StrandElement e = createStrandElement(false);		
		e.addElement(0, 1);
		assertEquals(2, e.getIntDataMembers()[0]);
		
		e = createStrandElement(true);		
		e.addElement(0, 1);
		assertEquals(2, e.getLongDataMembers()[0]);	
	}
	
	@Test
	public void testRemoveElement() {
		StrandElement e = createStrandElement(false);		
		e.removeElement(0, 1);
		assertEquals(0, e.getIntDataMembers()[0]);
		
		e = createStrandElement(true);		
		e.removeElement(0, 1);
		assertEquals(0, e.getLongDataMembers()[0]);	
	}
	
	@Test
	public void testGetStrandElementMember() {
		StrandElement e = createStrandElement(false);
		assertEquals(1, e.getStrandElementMember(0));
		
		e = createStrandElement(true);
		assertEquals((long) 1, e.getStrandElementMember(0));
	}
	
	@Test 
	public void testMergeElements() throws QPileupException {
		//int
		StrandElement e1 = createStrandElement(false);
		StrandElement e2 = createStrandElement(false);		
		e1.mergeElement(e2);
		assertEquals(2, e1.getIntDataMembers()[0]);
		
		//long
		e1 = createStrandElement(true);
		e2 = createStrandElement(true);		
		e1.mergeElement(e2);
		assertEquals(2, e1.getLongDataMembers()[0]);	
	}
	
	@Test(expected=QPileupException.class)
	public void testMergeElementsThrowsExceptionNameMismatch() throws QPileupException {
		//int
		StrandElement e1 = createStrandElement(false);
		StrandElement e2 = new StrandElement("wrong", 1, false);		
		e1.mergeElement(e2);
	}
	
	@Test(expected=QPileupException.class)
	public void testMergeElementsThrowsExceptionisLongMismatch() throws QPileupException {
		//int
		StrandElement e1 = createStrandElement(false);
		StrandElement e2 = new StrandElement("test", 1, true);		
		e1.mergeElement(e2);
	}
	
	private StrandElement createStrandElement(boolean isLong) {
		StrandElement e = new StrandElement("test", 1, isLong);
		
		if (isLong) {
			long[] array = {1};
			e.setLongDataMembers(array);
		} else {
			int[] array = {1};
			e.setIntDataMembers(array);
		}
		return e;
	}

}
