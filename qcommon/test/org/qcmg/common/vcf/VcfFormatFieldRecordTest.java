package org.qcmg.common.vcf;

import static org.junit.Assert.assertEquals;

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

}
