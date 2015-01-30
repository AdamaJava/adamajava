package org.qcmg.common.vcf;

import static org.junit.Assert.*;
import org.qcmg.common.util.Constants;

import org.junit.Test;

public class VcfFormatFieldRecordTest {
	
	@Test
	public void doesToStringWork() {
		
		try {
			VcfFormatFieldRecord rec = new VcfFormatFieldRecord(null, null);
			assertEquals(null, rec.toString());
		} catch (IllegalArgumentException iae) {}
		
		try {
			VcfFormatFieldRecord rec = new VcfFormatFieldRecord("", null);
			assertEquals("", rec.toString());
		} catch (IllegalArgumentException iae) {}
		
		VcfFormatFieldRecord rec = new VcfFormatFieldRecord("", "");
		assertEquals(null, rec.toString());
		
		
		rec = new VcfFormatFieldRecord("A", ".");
		assertEquals(".", rec.toString());
		
		rec = new VcfFormatFieldRecord("A:B", ".:.");
		assertEquals(".:.", rec.toString());
		
		rec = new VcfFormatFieldRecord("A:B:C:D:E:F:G", ".:.:.:.:.:.:.");
		assertEquals(".:.:.:.:.:.:.", rec.toString());
	}
	
	
	
	@Test //(expected=IllegalArgumentException.class)
	public void getFieldTest(){
		
		VcfFormatFieldRecord format = new VcfFormatFieldRecord( "GT:GD:AC:MR:NNS", ".:.:A1[39],0[0],T1[35],0[0]:1");
		
		assertTrue(format.getField("ok") == null);
		assertTrue(format.getField("MR").equals("1"));
		assertTrue(format.getField("NNS").equals(Constants.MISSING_DATA_STRING));
		assertTrue(format.getField("GT").equals(Constants.MISSING_DATA_STRING));
		
		
		try {
			format.getField(null);
		    fail( "My method didn't throw when I expected it to" );
		} catch (IllegalArgumentException expectedException) {
			assertTrue(true);
		}

	}

}
