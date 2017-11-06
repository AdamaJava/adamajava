package org.qcmg.common.vcf;

import static org.junit.Assert.*;

import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.junit.Test;

public class VcfFormatFieldRecordTest {
	
	@Test
	public void doesToStringWork() {
		
		try {
			VcfFormatFieldRecord rec = new VcfFormatFieldRecord(null, null);
			assertEquals(null, rec.getSampleColumnString());
		} catch (IllegalArgumentException iae) {}
		
		try {
			VcfFormatFieldRecord rec = new VcfFormatFieldRecord("", null);
			assertEquals("", rec.getSampleColumnString());
		} catch (IllegalArgumentException iae) {}
		
		VcfFormatFieldRecord rec = new VcfFormatFieldRecord("", "");
		assertEquals(null, rec.getSampleColumnString());
		
		
		rec = new VcfFormatFieldRecord("A", ".");
		assertEquals(".", rec.getSampleColumnString());
		
		rec = new VcfFormatFieldRecord("A:B", ".:.");
		assertEquals(".:.", rec.getSampleColumnString());
		
		rec = new VcfFormatFieldRecord("A:B:C:D:E:F:G", ".:.:.:.:.:.:.");
		assertEquals(".:.:.:.:.:.:.", rec.getSampleColumnString());
	}
	
	@Test 
	public void setFieldTest(){
		VcfFormatFieldRecord format = new VcfFormatFieldRecord( "GT:GD:AC:MR:NNS", ".:.:A1[39],0[0],T1[35],0[0]:1:nns:nns2");
		format.setField("AC", "ac");
		format.setField("ACC", "acc");
		format.setField("A", "a");
		assertEquals("GT:GD:AC:MR:NNS:ACC:A", format.getFormatColumnString());
		assertEquals(".:.:ac:1:nns:acc:a", format.getSampleColumnString());
	}
	
	@Test
	public void isMissingSample() {
		VcfFormatFieldRecord format = new VcfFormatFieldRecord( "GT", ".");
		assertEquals(true, format.isMissingSample());
		format = new VcfFormatFieldRecord( "GT", "ABC");
		assertEquals(false, format.isMissingSample());
	}
	
	@Test
	public void whyAmILosingData() {
		VcfRecord vcf = new VcfRecord(new String[]{"chr8","12306635","rs28428895","C","T",".","PASS_1;MIN_2;MR_2;NNS_2","FLANK=ACACATACATA;SOMATIC_2;IN=1,2;DB;GERM=30,185;EFF=intron_variant(MODIFIER|||n.304-488G>A||ENPP7P6|unprocessed_pseudogene|NON_CODING|ENST00000529817|2|1)","GT:GD:AC:MR:NNS:AD:DP:GQ:PL","0/1&.:C/T&C/C:C10[39],3[30],G1[11],0[0],T7[41.29],1[42]&C14[38.79],3[30],G1[11],0[0],T11[39.27],4[25.25]:8&15:8&15:.:.:.:.","0/0&0/1:C/C&C/T:C19[36.11],20[38.45],T1[42],0[0]&C22[36.23],22[36.91],T2[26.5],1[42]:1&3:1&2:4,3:7:86:86,0,133"});
		VcfFormatFieldRecord formatField =  vcf.getSampleFormatRecord(1);
		String bases = formatField.getField(VcfHeaderUtils.FORMAT_ALLELE_COUNT);
		assertEquals("C10[39],3[30],G1[11],0[0],T7[41.29],1[42]&C14[38.79],3[30],G1[11],0[0],T11[39.27],4[25.25]", bases);
	}
	
	
	@Test //(expected=IllegalArgumentException.class)
	public void getFieldTest(){
		
		VcfFormatFieldRecord format = new VcfFormatFieldRecord( "GT:GD:AC:MR:NNS", ".:.:A1[39],0[0],T1[35],0[0]:1");
		
		assertEquals(null, format.getField("ok"));
		assertTrue(format.getField("MR").equals("1"));
		assertTrue(format.getField("NNS").equals(Constants.MISSING_DATA_STRING));
		assertTrue(format.getField("GT").equals(Constants.MISSING_DATA_STRING));	
		format.setField("key", "value");
		assertTrue(format.getField("key").equals("value"));
		format.setField("GT", null);
		assertTrue(format.getField("GT").equals(Constants.MISSING_DATA_STRING));			
		format.setField("GT", "0/0");
		assertTrue(format.getField("GT").equals("0/0"));	
		assertEquals("0/0:.:A1[39],0[0],T1[35],0[0]:1:.:value", format.getSampleColumnString());
		assertEquals("GT:GD:AC:MR:NNS:key", format.getFormatColumnString());
				
		try {
			format.getField(null);
		    fail( "My method didn't throw when I expected it to" );
		} catch (IllegalArgumentException expectedException) {
			assertTrue(true);
		}
	}	

}
