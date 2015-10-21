package org.qcmg.common.vcf;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

public class VcfInfoFieldRecordTest {
	
	@Test
	public void nullAndEmptyCtor() {
		try {
			new VcfInfoFieldRecord(null);
			Assert.fail("blah");
		} catch (IllegalArgumentException iae){}
		try {
			new VcfInfoFieldRecord("");
			Assert.fail("blah");
		} catch (IllegalArgumentException iae){}
	}
	
	@Test
	public void validCtor() {
		VcfInfoFieldRecord rec = new VcfInfoFieldRecord("ABCD");
		assertEquals("ABCD", rec.toString());
		
		rec = new VcfInfoFieldRecord("ABCD;1234");
		assertEquals("ABCD;1234", rec.toString());
		
		rec = new VcfInfoFieldRecord("ABCD;1234;XYZ=???");
		assertEquals("ABCD;1234;XYZ=???", rec.toString());
	}
	
	@Test
	public void setField() {
		VcfInfoFieldRecord rec = new VcfInfoFieldRecord("ABCD;1234;XYZ=???");
		assertEquals("ABCD;1234;XYZ=???", rec.toString());
		
		rec.setField("ABCD", "5678");
		assertEquals(true, rec.toString().contains("ABCD=5678"));
		assertEquals(true, rec.toString().contains("1234"));
		assertEquals(true, rec.toString().contains("XYZ=???"));
		
		rec.setField("1234", "1234");
		assertEquals(true, rec.toString().contains("ABCD=5678"));
		assertEquals(true, rec.toString().contains("1234=1234"));
		assertEquals(true, rec.toString().contains("XYZ=???"));
		System.out.println(rec.toString());
	}

}
