package org.qcmg.common.vcf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.TabTokenizer;

public class VcfRecordTest {

	@Test
	public void infoFieldTest(){
		final String[] parms = {"chrY","2675826",".","TG","CA",".","COVN12;MIUN","SOMATIC;NNS=4;END=2675826","ACCS","TG,5,37,CA,0,2","AA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,TG,0,1"};
		final VcfRecord re = new VcfRecord(parms);
		
		assertTrue(re.getInfo().equals("SOMATIC;NNS=4;END=2675826"));
		re.appendInfo("NNS=5");
		assertEquals("5", re.getInfoRecord().getField("NNS"));
		
		assertEquals(true, re.getInfo().contains("SOMATIC"));
		assertEquals(true, re.getInfo().contains("NNS=5"));
		assertEquals(true, re.getInfo().contains("END=2675826"));
		
		re.setInfo("NNS=6");
		assertTrue(re.getInfo().equals("NNS=6"));
	
	}
	
	
	@Test
	public void constructorTest(){

		String ref = "TG";
		String alt = "CA";
		String[] parms = {"chrY","2675826",".",ref,alt,"PASS"};
		VcfRecord re = new VcfRecord(parms);
		VcfRecord re1 = new VcfRecord.Builder("chrY",2675826,ref).allele(alt).filter("PASS").build();		
		assertTrue(re.equals(re1));
		
		
		//only compare CHROM POS REF ALT
		String[] parms1 = {"chrY","2675826",".",ref, alt,".","COVN12;MIUN","SOMATIC;NNS=4;END=2675826","ACCS","TG,5,37,CA,0,2","AA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,TG,0,1"};
		re = new VcfRecord(parms1);
		assertTrue(re.equals(re1));
		
		
	}
	
	@Test
	public void doesToStringWork(){
		String[] parms = {"chrY","2675826",".","TG","CA"};
		VcfRecord re = new VcfRecord(parms);
	 
		// expecting end to be inserted by toString, and to be equal to start + 1 (compound snp)
		String reToString = re.toString();
		assertEquals(true, reToString.endsWith(Constants.NEW_LINE));
		
		String[] parm = reToString.split(Constants.TAB + "" );
		assertTrue(parm.length == 9);	
		assertTrue(parm[7].equals(Constants.MISSING_DATA_STRING));	
		
		String[] parms2 = {"chrY","2675826",".","TGAA","CATT"};
		re = new VcfRecord(parms2);
		
		// expecting end to be inserted by toString, and to be equal to start + 1 (compound snp)
		reToString = re.toString();
		assertEquals(true, re.getChrPosition().getEndPosition() == 2675829);
	}
	
	@Test
	public void appendId() {
		String[] parms = {"chrY","2675826",".","TG","CA"};
		VcfRecord re = new VcfRecord(parms);
		assertEquals(Constants.MISSING_DATA_STRING, re.getId());
		
		re.appendId(null);
		assertEquals(null, re.getId());
		re.appendId("");
		assertEquals("", re.getId());
		re.appendId(Constants.MISSING_DATA_STRING);
		assertEquals(Constants.MISSING_DATA_STRING, re.getId());
		re.appendId("12345");
		assertEquals("12345", re.getId());
		re.appendId("678");
		assertEquals("12345;678", re.getId());
		re.appendId("678");
		assertEquals("12345;678;678", re.getId());
		re.appendId("12345");
		assertEquals("12345;678;678;12345", re.getId());
	}
	
	@Test
	public void getSampleColumnTest(){
/*
 * 	public VcfFormatFieldRecord getSampleFormatRecord(int index){
		String s = (index >= formatRecords.size())? null: formatRecords.get(index);
		return (s == null)? null : new VcfFormatFieldRecord(formatRecords.get(0), s);
	}		
 */
		//one sample
		String line = "chr2\t92281414\t.\tAC\tTG\t.\tSBIAS;SAT3\tEND=92281415\tACCS\tAC,1,1,TG,0,4\tOK";
		String[] params = TabTokenizer.tokenize(line);
		VcfRecord re = new VcfRecord(params);

		assertTrue(re.getSampleFormatRecord(1).toString().equals("ACCS\tAC,1,1,TG,0,4")); 
 		for(int i = 0; i < 10; i ++)
 			if(i == 1 ) assertTrue(re.getSampleFormatRecord(i).getSampleColumnString().equals("AC,1,1,TG,0,4")  ); 
 			else if( i == 2 ) assertTrue(re.getSampleFormatRecord(i).getSampleColumnString().equals("OK")  );
 			else assertTrue(re.getSampleFormatRecord(i) == null); 
				
	}	
	
	@Test
	public void MissingSampleColumn(){
		//one sample
		String line = "chr2\t92281414\t.\tAC\tTG\t.\tSBIAS;SAT3\tEND=92281415\tACCS\tAC,1,1,TG,0,4";
		String[] params = TabTokenizer.tokenize(line);
		VcfRecord re = new VcfRecord(params);
		assertEquals(re.getFormatFields().size(), 2);
		
		//second sample missing
		line = "chr2\t92281414\t.\tAC\tTG\t.\tSBIAS;SAT3\tEND=92281415\tACCS\tAC,1,1,TG,0,4\t";
		params = TabTokenizer.tokenize(line);	
 		re = new VcfRecord(params);
		assertEquals(re.getFormatFields().size(), 3);
		assertFalse(re.getFormatFields().get(1).equals(Constants.MISSING_DATA_STRING));
		
		//first sample missing
		line = "chr2\t92281414\t.\tAC\tTG\t.\tSBIAS;SAT3\tEND=92281415\tACCS\t\tAC,1,1,TG,0,4";
		params = TabTokenizer.tokenize(line);	
 		re = new VcfRecord(params);
		assertEquals(re.getFormatFields().size(), 3);
		assertTrue(re.getFormatFields().get(1).equals(Constants.MISSING_DATA_STRING));
		
		
		//three sample missing
		line = "chr2\t92281414\t.\tAC\tTG\t.\tSBIAS;SAT3\tEND=92281415\tACCS\t\t\t";
		params = TabTokenizer.tokenize(line);	
 		re = new VcfRecord(params);
		assertEquals(re.getFormatFields().size(), 4);
		assertTrue(re.getFormatFields().get(1).equals(Constants.MISSING_DATA_STRING));
		assertTrue(re.getFormatFields().get(2).equals(Constants.MISSING_DATA_STRING));
		assertTrue(re.getFormatFields().get(3).equals(Constants.MISSING_DATA_STRING));
		
		//first sample missing
		line = "chr2\t92281414\t.\tAC\tTG\t.\tSBIAS;SAT3\tEND=92281415\tACCS\t\t.\t.";
		params = TabTokenizer.tokenize(line);	
 		re = new VcfRecord(params);
		assertEquals(re.getFormatFields().size(), 4);
		assertTrue(re.getFormatFields().get(1).equals(Constants.MISSING_DATA_STRING));
		assertTrue(re.getFormatFields().get(2).equals(Constants.MISSING_DATA_STRING));
		assertTrue(re.getFormatFields().get(3).equals(Constants.MISSING_DATA_STRING));
	}
}
