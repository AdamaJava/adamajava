package org.qcmg.common.vcf;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.util.Constants;

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
	public void missingData() {
		
		/*
		 * despite the missing data value being returned, it is not actually stored in the info field record
		 */
		VcfInfoFieldRecord rec = new VcfInfoFieldRecord(Constants.MISSING_DATA_STRING);
		assertEquals(Constants.MISSING_DATA_STRING, rec.toString());
		
		rec.addField("IN", "1");
		assertEquals("IN=1", rec.toString());
		
		rec = new VcfInfoFieldRecord("XYZ=.");
		assertEquals("XYZ=.", rec.toString());
		
		rec = new VcfInfoFieldRecord(Constants.MISSING_DATA_STRING + "=123");
		assertEquals(".=123", rec.toString());
		
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
				
		rec = new VcfInfoFieldRecord("END=3;HOMLEN=0;ND=0:28:28:0[0,0]:0:0:27;");		
		rec.setField("ND", "0:28:28:0[0,0]:0:0:27");
		assertEquals(true, rec.toString().equals("END=3;HOMLEN=0;ND=0:28:28:0[0,0]:0:0:27"));				
		rec.setField("ND", "0:37:36:0[0,0]:0:0:0" );
		assertEquals(true, rec.toString().equals("END=3;HOMLEN=0;ND=0:37:36:0[0,0]:0:0:0"));				
		
	}
	@Test
	public void doesToStringBehave() {
		VcfInfoFieldRecord rec = new VcfInfoFieldRecord("ABCD;1234;XYZ=.;CONF=HIGH");
		assertEquals("ABCD;1234;XYZ=.;CONF=HIGH", rec.toString());
		
	}

}
