package org.qcmg.common.vcf;

import static org.junit.Assert.*;

import org.qcmg.common.util.Constants;
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
		assertEquals(".:.:ac:1:nns:acc:a", format.getSampleColumnString());
		assertEquals("GT:GD:AC:MR:NNS:ACC:A", format.getFormatColumnString());

	}
	
	@Test
	public void isMissingSample() {
		VcfFormatFieldRecord format = new VcfFormatFieldRecord( "GT", ".");
		assertEquals(true, format.isMissingSample());
		format = new VcfFormatFieldRecord( "GT", "ABC");
		assertEquals(false, format.isMissingSample());
		
	}
	
	
	@Test //(expected=IllegalArgumentException.class)
	public void getFieldTest(){
		
		VcfFormatFieldRecord format = new VcfFormatFieldRecord( "GT:GD:AC:MR:NNS", ".:.:A1[39],0[0],T1[35],0[0]:1");
		
		assertTrue(format.getField("ok") == null);
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
		
//	@Test //new method is faster than old	
//	public void speedTestNew(){
//        double currentMemory = ( (double)((double)(Runtime.getRuntime().totalMemory()/1024)/1024))- ((double)((double)(Runtime.getRuntime().freeMemory()/1024)/1024));
//        System.out.println("start RAM used: " + currentMemory );
//		
//        QLogger logger = QLoggerFactory.getLogger(VcfFormatFieldRecordTest.class, null,  null);	            		               
//        logger.logInitialExecutionStats(null, null, null);
//        int max = Integer.MAX_VALUE / 500; //Integer.MAX_VALUE = 2147483647
//        for(int i = 0; i< max; i ++){
//        	VcfFormatFieldRecord re = new  VcfFormatFieldRecord("A:B:C:D:E:F:G", ".:.:.:.:.:.:.");
//        	re.setField("H", "H");
//        	re.getField("A");
//        	re.getFormatColumnString();
//        	re.toString();
//        }       
//        logger.logFinalExecutionStats(0);
//        
//        currentMemory = ( (double)((double)(Runtime.getRuntime().totalMemory()/1024)/1024))- ((double)((double)(Runtime.getRuntime().freeMemory()/1024)/1024));
//        System.out.println("end RAM used: " + currentMemory );   
//	}	

}
