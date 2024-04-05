package org.qcmg.common.vcf;

import java.util.Arrays;

import org.junit.Test;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.TabTokenizer;

import static org.junit.Assert.*;

public class VcfRecordTest {

	@Test
	public void infoFieldTest(){
		final String[] parms = {"chrY","2675826",".","TG","CA",".","COVN12;MIUN","SOMATIC;NNS=4;END=2675826","ACCS","TG,5,37,CA,0,2","AA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,TG,0,1"};
		final VcfRecord re = new VcfRecord(parms);

        assertEquals("SOMATIC;NNS=4;END=2675826", re.getInfo());
		re.appendInfo("NNS=5");
		assertEquals("5", re.getInfoRecord().getField("NNS"));

        assertTrue(re.getInfo().contains("SOMATIC"));
        assertTrue(re.getInfo().contains("NNS=5"));
        assertTrue(re.getInfo().contains("END=2675826"));
		
		re.setInfo("NNS=6");
		assertEquals("NNS=6", re.getInfo());
	}
	
	@Test
	public void constructorTest(){

		String ref = "TG";
		String alt = "CA";
		String[] parms = {"chrY","2675826",".",ref,alt,"PASS"};
		VcfRecord re = new VcfRecord(parms);
		VcfRecord re1 = new VcfRecord.Builder("chrY",2675826,ref).allele(alt).filter("PASS").build();		
		assertEquals(re1, re);
		
		
		//only compare CHROM POS REF ALT
		String[] parms1 = {"chrY","2675826",".",ref, alt,".","COVN12;MIUN","SOMATIC;NNS=4;END=2675826","ACCS","TG,5,37,CA,0,2","AA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,TG,0,1"};
		re = new VcfRecord(parms1);
        assertEquals(re, re1);
	}
	
	@Test
	public void testToString() {
		String[] parms = {"chr15","1234567",".",".","."};
		VcfRecord re = new VcfRecord(parms);
		assertEquals("chr15	1234567	.	.	.	.	.	.\n", re.toString());
		
		parms = new String[] {"chr15","1234567","rs123456",".","."};
		re = new VcfRecord(parms);
		assertEquals("chr15	1234567	rs123456	.	.	.	.	.\n", re.toString());
		
		parms = new String[] {"chr15","1234567","rs123456","A","."};
		re = new VcfRecord(parms);
		assertEquals("chr15	1234567	rs123456	A	.	.	.	.\n", re.toString());
		
		parms = new String[] {"chr15","1234567","rs123456","A","B"};
		re = new VcfRecord(parms);
		assertEquals("chr15	1234567	rs123456	A	B	.	.	.\n", re.toString());
		
		parms = new String[] {"chr15","1234567","rs123456","A","B"};
		re = new VcfRecord(parms);
		re.setQualString("this_is_my_qual_string");
		assertEquals("chr15	1234567	rs123456	A	B	this_is_my_qual_string	.	.\n", re.toString());
		
		re.setFilter("this_is_my_filter_string");
		assertEquals("chr15	1234567	rs123456	A	B	this_is_my_qual_string	this_is_my_filter_string	.\n", re.toString());
		
		re.setInfo("hello_info");
		assertEquals("chr15	1234567	rs123456	A	B	this_is_my_qual_string	this_is_my_filter_string	hello_info\n", re.toString());
		
		re.setFormatFields(Arrays.asList("FT", "format_1"));
		assertEquals("chr15	1234567	rs123456	A	B	this_is_my_qual_string	this_is_my_filter_string	hello_info	FT	format_1\n", re.toString());
		
		re.setFormatFields(Arrays.asList("FT", "format_1", "format_2"));
		assertEquals("chr15	1234567	rs123456	A	B	this_is_my_qual_string	this_is_my_filter_string	hello_info	FT	format_1	format_2\n", re.toString());
	}
	
	@Test
	public void doesToStringWork(){
		String[] parms = {"chrY","2675826",".","TG","CA"};
		VcfRecord re = new VcfRecord(parms);
	 
		// expecting end to be inserted by toString, and to be equal to start + 1 (compound snp)
		String reToString = re.toString();
        assertTrue(reToString.endsWith(Constants.NEW_LINE));
		
		String[] parm = reToString.split(Constants.TAB_STRING);
		assertEquals(8, parm.length);	
		assertEquals(Constants.MISSING_DATA_STRING + Constants.NL, parm[7]);	
		
		String[] parms2 = {"chrY","2675826",".","TGAA","CATT"};
		re = new VcfRecord(parms2);
		
		// expecting end to be inserted by toString, and to be equal to start + 1 (compound snp)
		assertEquals(2675829, re.getChrPosition().getEndPosition());
	}
	
	@Test
	public void appendId() {
		String[] parms = {"chrY","2675826",".","TG","CA"};
		VcfRecord re = new VcfRecord(parms);
		assertEquals(Constants.MISSING_DATA_STRING, re.getId());
		
		re.appendId(null);
        assertNull(re.getId());
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
		//one sample
		String line = "chr2\t92281414\t.\tAC\tTG\t.\tSBIAS;SAT3\tEND=92281415\tACCS\tAC,1,1,TG,0,4\tOK";
		String[] params = TabTokenizer.tokenize(line);
		VcfRecord re = new VcfRecord(params);

		assertEquals("ACCS\tAC,1,1,TG,0,4", re.getSampleFormatRecord(1).toString()); 
 		for (int i = 0; i < 10; i ++) {
 			if (i == 1 ) {
 				assertEquals("AC,1,1,TG,0,4", re.getSampleFormatRecord(i).getSampleColumnString()); 
 			} else if ( i == 2 ) {
 				assertEquals("OK", re.getSampleFormatRecord(i).getSampleColumnString());
 			} else {
                assertNull(re.getSampleFormatRecord(i));
 			}
 		}
				
	}	
	
	@Test
	public void missingSampleColumn(){
		//one sample
		String line = "chr2\t92281414\t.\tAC\tTG\t.\tSBIAS;SAT3\tEND=92281415\tACCS\tAC,1,1,TG,0,4";
		String[] params = TabTokenizer.tokenize(line);
		VcfRecord re = new VcfRecord(params);
		assertEquals(re.getFormatFields().size(), 2);
		assertEquals("ACCS", re.getFormatFields().getFirst());
		
		//second sample missing
		line = "chr2\t92281414\t.\tAC\tTG\t.\tSBIAS;SAT3\tEND=92281415\tACCS\tAC,1,1,TG,0,4\t";
		params = TabTokenizer.tokenize(line);	
 		re = new VcfRecord(params);
		assertEquals(re.getFormatFields().size(), 3);
		assertEquals("ACCS", re.getFormatFields().get(0));
        assertNotEquals(Constants.MISSING_DATA_STRING, re.getFormatFields().get(1));
		
		//first sample missing
		line = "chr2\t92281414\t.\tAC\tTG\t.\tSBIAS;SAT3\tEND=92281415\tACCS\t\tAC,1,1,TG,0,4";
		params = TabTokenizer.tokenize(line);	
 		re = new VcfRecord(params);
		assertEquals(re.getFormatFields().size(), 3);
		assertEquals("ACCS", re.getFormatFields().get(0));
        assertEquals(Constants.MISSING_DATA_STRING, re.getFormatFields().get(1));
		
		
		//three sample missing
		line = "chr2\t92281414\t.\tAC\tTG\t.\tSBIAS;SAT3\tEND=92281415\tACCS\t\t\t";
		params = TabTokenizer.tokenize(line);	
 		re = new VcfRecord(params);
		assertEquals(re.getFormatFields().size(), 4);
		assertEquals("ACCS", re.getFormatFields().get(0));
        assertEquals(Constants.MISSING_DATA_STRING, re.getFormatFields().get(1));
        assertEquals(Constants.MISSING_DATA_STRING, re.getFormatFields().get(2));
        assertEquals(Constants.MISSING_DATA_STRING, re.getFormatFields().get(3));
		
		//first sample missing
		line = "chr2\t92281414\t.\tAC\tTG\t.\tSBIAS;SAT3\tEND=92281415\tACCS\t\t.\t.";
		params = TabTokenizer.tokenize(line);	
 		re = new VcfRecord(params);
		assertEquals(re.getFormatFields().size(), 4);
		assertEquals("ACCS", re.getFormatFields().get(0));
        assertEquals(Constants.MISSING_DATA_STRING, re.getFormatFields().get(1));
        assertEquals(Constants.MISSING_DATA_STRING, re.getFormatFields().get(2));
        assertEquals(Constants.MISSING_DATA_STRING, re.getFormatFields().get(3));
	}
}
