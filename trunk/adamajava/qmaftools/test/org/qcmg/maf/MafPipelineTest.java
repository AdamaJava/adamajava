package org.qcmg.maf;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.qcmg.maf.util.MafStatsUtils;

public class MafPipelineTest {
	
	@Test
	public void testQualityControlCheck() {
		List<MAFRecord> mafs = new ArrayList<MAFRecord>();
		
		assertEquals(0.0, MafStatsUtils.getRsRatioDouble(mafs), 0.0001);
		
		MAFRecord maf1 = new MAFRecord();
		maf1.setDbSnpId("hello");
		mafs.add(maf1);
		assertEquals(0.0, MafStatsUtils.getRsRatioDouble(mafs), 0.0001);
		
		maf1.setDbSnpId("rs1234");
		assertEquals(100.0, MafStatsUtils.getRsRatioDouble(mafs), 0.0001);
		
		MAFRecord maf2 = new MAFRecord();
		maf2.setDbSnpId("hello");
		mafs.add(maf2);
		assertEquals(50.0, MafStatsUtils.getRsRatioDouble(mafs), 0.0001);
		maf2.setDbSnpId("rshello");
		assertEquals(100.0, MafStatsUtils.getRsRatioDouble(mafs), 0.0001);
	}

}
