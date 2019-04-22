package org.qcmg.qsv.report;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DCCReportTest {
	
	@Test
	public void getValidationPlatform() {
		assertEquals("-999", DCCReport.getValidationPlatform(null));
		assertEquals("-999", DCCReport.getValidationPlatform(""));
		assertEquals("4", DCCReport.getValidationPlatform("solid"));
		assertEquals("60", DCCReport.getValidationPlatform("illumina"));
		assertEquals("81", DCCReport.getValidationPlatform("bgi"));
		
		/*
		 * got to be lower case - exact matches only
		 */
		assertEquals("-999", DCCReport.getValidationPlatform("Solid"));
		assertEquals("-999", DCCReport.getValidationPlatform("illuminA"));
		assertEquals("-999", DCCReport.getValidationPlatform("BGI"));
	}

}
