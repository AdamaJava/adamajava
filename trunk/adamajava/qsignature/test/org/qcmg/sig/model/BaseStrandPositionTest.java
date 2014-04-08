package org.qcmg.sig.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BaseStrandPositionTest {
	
	@Test
	public void testBaseStrandPosition() {
		BaseStrandPosition bsp = new BaseStrandPosition((char) 0, false, 0);
		assertEquals(0, bsp.getBase());
		assertEquals(false, bsp.isForwardStrand());
		assertEquals(0, bsp.getStartPosition());
		
		bsp = new BaseStrandPosition('A', false, 12345);
		assertEquals('A', bsp.getBase());
		assertEquals(false, bsp.isForwardStrand());
		assertEquals(12345, bsp.getStartPosition());
		
		bsp = new BaseStrandPosition('x', true, -666);
		assertEquals('x', bsp.getBase());
		assertEquals(true, bsp.isForwardStrand());
		assertEquals(-666, bsp.getStartPosition());
	}

}
