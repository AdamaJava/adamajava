package org.qcmg.qprofiler.bam;


import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.qcmg.qprofiler.summarise.PositionSummary;


public class PositionSummaryTest {

	@Test
	public void testAddPositionMinMax() throws Exception {
		List<String> rgs = Arrays.asList(new String[] {"rg1", "rg2"});
		PositionSummary ps = new PositionSummary(rgs);
		
		// add some positions to the summary obj
		ps.addPosition(123456, "rg1");
		ps.addPosition(123456, "rg1");
		ps.addPosition(123457, "rg1");
		ps.addPosition(123458, "rg1");
		ps.addPosition(123459, "rg1");
		
		assertEquals(123456, ps.getMin());
		assertEquals(123459, ps.getMax());		
		assertEquals(5, ps.getCoverage().get(0).get());
		
		// add some smaller numbers
		ps.addPosition(123, "rg2");
		ps.addPosition(456, "rg2");
		
		assertEquals(123, ps.getMin());
		assertEquals(123459, ps.getMax());
		assertEquals(7, ps.getCoverage().get(0).get());
		assertEquals(2, ps.getRgCoverage().get(0).get(1));
		assertEquals(5, ps.getRgCoverage().get(0).get(0));
	}
	
	@Test
	public void testAddPositionMap() throws Exception {
		List<String> rgs = Arrays.asList(new String[] {"rg1", "rg2", "rg3"});
		PositionSummary ps = new PositionSummary(rgs);
		
		// add some positions to the summary obj
		ps.addPosition(1,"rg1");
		ps.addPosition(1000000,"rg2");
		ps.addPosition(2000000,"rg2");
		ps.addPosition(3000000,"rg2");
		ps.addPosition(4000000,"rg2");
		
		assertEquals(1, ps.getMin());
		assertEquals(4000000, ps.getMax());
		assertEquals(1, ps.getCoverage().get(0).get());
		assertEquals(1, ps.getCoverage().get(1).get());
		assertEquals(1, ps.getRgCoverage().get(2).get(1));
		assertEquals(1, ps.getRgCoverage().get(3).get(1));
		assertEquals(1, ps.getCoverage().get(4).get());
		
		// add some smaller numbers
		ps.addPosition(0,"rg3");
		ps.addPosition(456,"rg3");
		
		assertEquals(0, ps.getMin());
		assertEquals(4000000, ps.getMax());
		assertEquals(3, ps.getCoverage().get(0).get());
		assertEquals(2, ps.getRgCoverage().get(0).get(2));
		assertEquals(1, ps.getRgCoverage().get(0).get(0));
		assertEquals(0, ps.getRgCoverage().get(0).get(1));
		
		
		// and some larger numbers
		ps.addPosition(2000002,"rg1");
		ps.addPosition(5000000,"rg1");
		
		assertEquals(0, ps.getMin());
		assertEquals(5000000, ps.getMax());
		assertEquals(2, ps.getCoverage().get(2).get());
		assertEquals(1, ps.getRgCoverage().get(2).get(1));
	}
}
