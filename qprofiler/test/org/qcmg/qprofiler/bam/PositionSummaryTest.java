package org.qcmg.qprofiler.bam;

import junit.framework.Assert;

import org.junit.Test;


public class PositionSummaryTest {

	@Test
	public void testAddPositionMinMax() {
		PositionSummary ps = new PositionSummary(123456);
		
		// add some positions to the summary obj
		ps.addPosition(123456);
		ps.addPosition(123457);
		ps.addPosition(123458);
		ps.addPosition(123459);
		
		Assert.assertEquals(123456, ps.getMin());
		Assert.assertEquals(123459, ps.getMax());
		Assert.assertEquals(5, ps.getCoverage().get(0).get());
		
		// add some smaller numbers
		ps.addPosition(123);
		ps.addPosition(456);
		
		Assert.assertEquals(123, ps.getMin());
		Assert.assertEquals(123459, ps.getMax());
		Assert.assertEquals(7, ps.getCoverage().get(0).get());
	}
	
	@Test
	public void testAddPositionMap() {
		PositionSummary ps = new PositionSummary(1);
		
		// add some positions to the summary obj
		ps.addPosition(1000000);
		ps.addPosition(2000000);
		ps.addPosition(3000000);
		ps.addPosition(4000000);
		
		Assert.assertEquals(1, ps.getMin());
		Assert.assertEquals(4000000, ps.getMax());
//		Assert.assertEquals(5, ps.getCoverage().size());
		Assert.assertEquals(1, ps.getCoverage().get(0).get());
		Assert.assertEquals(1, ps.getCoverage().get(1).get());
		Assert.assertEquals(1, ps.getCoverage().get(2).get());
		Assert.assertEquals(1, ps.getCoverage().get(3).get());
		Assert.assertEquals(1, ps.getCoverage().get(4).get());
		
		// add some smaller numbers
		ps.addPosition(0);
		ps.addPosition(456);
		
		Assert.assertEquals(0, ps.getMin());
		Assert.assertEquals(4000000, ps.getMax());
//		Assert.assertEquals(5, ps.getCoverage().size());
		Assert.assertEquals(3, ps.getCoverage().get(0).get());
		
		
		// and some larger numbers
		ps.addPosition(2000002);
		ps.addPosition(5000000);
		
		Assert.assertEquals(0, ps.getMin());
		Assert.assertEquals(5000000, ps.getMax());
//		Assert.assertEquals(6, ps.getCoverage().size());
		Assert.assertEquals(2, ps.getCoverage().get(2).get());
	}
}
