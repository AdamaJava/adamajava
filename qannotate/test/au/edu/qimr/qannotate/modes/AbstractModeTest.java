package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.utils.SampleColumn;


public class AbstractModeTest {	
	public File input; 
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	{
		try {
			input = testFolder.newFile();
			createVcf();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	@Test
	public void inputRecordCompoundSnp() throws Exception{
		
		final String[] params =  {"chr1","10180",".","TA","CT","."," MIN;MIUN","SOMATIC;END=10181","ACCS","TA,5,37,CA,0,2", "AA,1,1,CA,4,1,CT,3,1,TA,11,76,TT,2,2,_A,0,3,TG,0,1"};
		final VcfRecord record = new VcfRecord(params);
		assertEquals(10180, record.getPosition());
		assertEquals(10181, record.getChrPosition().getEndPosition());
		 
	}
	
	@Test
	public void reHeaderTest() throws Exception{
		
	   try (BufferedReader br = new BufferedReader(new FileReader(input))){
	    	   int i = 0;
	    	   while (  br.readLine() != null ) {
	    		   i++;
	    	   }
	    	   assertTrue(i == 3);
	   }
	       
		DbsnpMode db = new DbsnpMode(true);
		db.loadVcfRecordsFromFile(input,false);		
		db.reheader("testing run",   input.getAbsolutePath());
		db.writeVCF(input);
		
		
        try (VCFFileReader reader = new VCFFileReader(input)) {
	        	int i = 0;
	        	for (VcfHeaderRecord re :  reader.getHeader()) {
	        		if (re.toString().startsWith(VcfHeaderUtils.STANDARD_UUID_LINE)) {
	        			// new UUID should have been inserted by now
	        			assertEquals(false, "abcd_12345678_xzy_999666333".equals(StringUtils.getValueFromKey(re.getMetaValue(), VcfHeaderUtils.STANDARD_UUID_LINE)));
	        		}
	        		i ++;
	        	}
	        	assertEquals(7, i);	// removed blank lines
        }		
	}
	
	 @Test
	 public void sampleColumnTest()throws Exception{
			VcfHeader header = new VcfHeader();		 
			header.addOrReplace("##qControlSample=control");
			header.addOrReplace("##qTestSample=test");
			header.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "qControlSample" + "\tqTestSample");
 
			SampleColumn column = SampleColumn.getSampleColumn(null,null, header);
			assertTrue( column.getControlSampleColumn() == 1);
			assertTrue( column.getTestSampleColumn() == 2);		
			assertEquals( column.getControlSample() , "control");
			assertEquals( column.getTestSample() , "test");	
			
			header.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "qControlSample" + "\ttest");
			column = SampleColumn.getSampleColumn(null,null, header);
			assertTrue( column.getControlSampleColumn() == 1);
			assertTrue( column.getTestSampleColumn() == 2);		
	 }
	 
	
	@Test
	public void  retriveSampleColumnTest(){
		final String control = "Control";
		final String test = "Test";
		
		VcfHeader header = new VcfHeader();		 
		header.addOrReplace("##qControlSample=" + control);
		header.addOrReplace("##qTestSample=" + test);
		header.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + control + "\t" + "test");
		
		SampleColumn column = SampleColumn.getSampleColumn(null,null, header);
		assertTrue( column.getControlSampleColumn() == 1);
		assertTrue( column.getTestSampleColumn() == 2);		
		assertEquals( column.getControlSample() , control);
		assertEquals( column.getTestSample() , test);		
		
		//point to sample column 1: "control"	
		column = SampleColumn.getSampleColumn(control,control, header);
		assertTrue( column.getControlSampleColumn() == 1);
		assertTrue( column.getTestSampleColumn() == 1);		
		assertEquals( column.getControlSample() , control);
		assertEquals( column.getTestSample() , control);		
		
		//point to sample column 1: "test"	 
		column = SampleColumn.getSampleColumn(test,test, header);
		assertTrue( column.getControlSampleColumn() == 2);
		assertTrue( column.getTestSampleColumn() == 2);
		assertEquals( column.getControlSample() , test);
		assertEquals( column.getTestSample() , test);		
				
		//point to unexsit sample id 
		try{
			column = SampleColumn.getSampleColumn(test+control,test, header);
			/*
			 * add in assertion that columns are the same - single sample mode...
			 */
//			fail( "My method didn't throw when I expected it to" );
		}catch(Exception e){
		}
	}
	
	
	@Test
	public void ordering() throws IOException {
		List<String> data = new ArrayList<>();
		data.add("##fileformat=VCFv4.0");
	    data.add(VcfHeaderUtils.STANDARD_UUID_LINE + "=abcd_12345678_xzy_999666333");
	    data.add("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO");
	    
	    data.add("chrY\t14923588\t.\tG\tA\t.\tSBIAS\t;FS=GTGATATTCCC\tGT:GD:AC:MR:NNS\t0/1:G/A:A0[0],15[36.2],G11[36.82],9[33]\t0/1:G/A:A0[0],33[35.73],G6[30.5],2[34]:15:13"); 
        data.add("chr1\t2675826\t.\tTG\tCA\t.\tCOVN12;MIUN\tSOMATIC;END=2675826\tACCS\tTG,5,37,CA,0,2\tAA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,TG,0,1");
        data.add("chrY\t2675825\t.\tTTG\tTGG\t.\tCOVN12;MIUN\tSOMATIC;END=2675826\tACCS\tTG,5,37,CA,0,2\tAA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,TG,0,1");
        data.add("chr3\t22012840\t.\tC\tA\t.\tMIUN\tSOMATIC\tGT:GD:AC:MR:NNS\t0/1:C/A:A0[0],15[36.2],C11[36.82],9[33]\t0/1:C/A:A0[0],33[35.73],C6[30.5],2[34]:15:13");  
        data.add("chrY\t77242678\t.\tCA\tTG\t.\tPASS\tEND=77242679\tACCS\tCA,10,14,TG,6,7\tCA,14,9,TG,23,21");
        
        /*
         * create vcf file
         */
        File vcfFile = testFolder.newFile();
        File outputVcfFile = testFolder.newFile();
        createVcf(vcfFile, data);
        
        ConfidenceMode mode = new ConfidenceMode();
		mode.loadVcfRecordsFromFile(vcfFile, true);
		mode.writeVCF(outputVcfFile);
		
		/*
		 * now check the ordering
		 */
		try (VCFFileReader reader = new VCFFileReader(outputVcfFile)) {
			int i = 0;
			for (VcfRecord v : reader) {
				i++;
				if (i == 1) {
					assertEquals("chr1", v.getChromosome());
				} else if (i == 2) {
					assertEquals("chr3", v.getChromosome());
				} else if (i >= 3) {
					assertEquals("chrY", v.getChromosome());
				}
			}
		}		 
	}
	
	@Test
	public void orderingWith38Contigs() throws IOException {
		List<String> data = new ArrayList<>();
		data.add("##fileformat=VCFv4.0");
		data.add(VcfHeaderUtils.STANDARD_UUID_LINE + "=abcd_12345678_xzy_999666333");
		data.add("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO");
		
		data.add("chrUn_GL000216v2\t14923588\t.\tG\tA\t.\tSBIAS\t;FS=GTGATATTCCC\tGT:GD:AC:MR:NNS\t0/1:G/A:A0[0],15[36.2],G11[36.82],9[33]\t0/1:G/A:A0[0],33[35.73],G6[30.5],2[34]:15:13"); 
		data.add("chr17_KI270729v1_random\t2675826\t.\tTG\tCA\t.\tCOVN12;MIUN\tSOMATIC;END=2675826\tACCS\tTG,5,37,CA,0,2\tAA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,TG,0,1");
		data.add("chrY\t2675825\t.\tTTG\tTGG\t.\tCOVN12;MIUN\tSOMATIC;END=2675826\tACCS\tTG,5,37,CA,0,2\tAA,1,1,CA,4,1,CT,3,1,TA,11,76,TG,2,2,TG,0,1");
		data.add("chr3\t22012840\t.\tC\tA\t.\tMIUN\tSOMATIC\tGT:GD:AC:MR:NNS\t0/1:C/A:A0[0],15[36.2],C11[36.82],9[33]\t0/1:C/A:A0[0],33[35.73],C6[30.5],2[34]:15:13");  
		data.add("chrY\t77242678\t.\tCA\tTG\t.\tPASS\tEND=77242679\tACCS\tCA,10,14,TG,6,7\tCA,14,9,TG,23,21");
		data.add("chr1\t77242678\t.\tCA\tTG\t.\tPASS\tEND=77242679\tACCS\tCA,10,14,TG,6,7\tCA,14,9,TG,23,21");
		
		/*
		 * create vcf file
		 */
		File vcfFile = testFolder.newFile();
		File outputVcfFile = testFolder.newFile();
		createVcf(vcfFile, data);
		
		ConfidenceMode mode = new ConfidenceMode();
		mode.loadVcfRecordsFromFile(vcfFile, true);
		mode.writeVCF(outputVcfFile);
		
		/*
		 * now check the ordering
		 */
		try (VCFFileReader reader = new VCFFileReader(outputVcfFile)) {
			int i = 0;
			for (VcfRecord v : reader) {
				i++;
				if (i == 1) {
					assertEquals("chr1", v.getChromosome());
				} else if (i == 2) {
					assertEquals("chr3", v.getChromosome());
				} else if (i == 3 || i == 4) {
					assertEquals("chrY", v.getChromosome());
				} else if (i == 5) {
					assertEquals("chr17_KI270729v1_random", v.getChromosome());
				} else if (i == 6) {
					assertEquals("chrUn_GL000216v2", v.getChromosome());
				}
			}
		}		 
	}
	
	public void createVcf() throws IOException{		
        final List<String> data = new ArrayList<>();
        data.add("##fileformat=VCFv4.0");
        data.add(VcfHeaderUtils.STANDARD_UUID_LINE + "=abcd_12345678_xzy_999666333");
        data.add("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO");
        createVcf(input, data);
	}
	
	public static void createVcf(File vcfFile, List<String> data) throws IOException {
		try (BufferedWriter out = new BufferedWriter(new FileWriter(vcfFile));) {          
			for (final String line : data) {
				out.write(line +"\n");                  
			}
		 }
	}
	
	@Test
	public void getFullChromosome() {
		AbstractMode am = new DbsnpMode(true);			
		assertEquals("1",  am.cloneIfLenient(new ChrRangePosition("1", 100, 200), true ).getChromosome());
		assertEquals("chr1",  am.cloneIfLenient(new ChrRangePosition("1", 100, 200), false ).getChromosome());				
		assertEquals("CHRXY",  am.cloneIfLenient(new ChrRangePosition("CHRXY", 100, 200), true ).getChromosome());
		assertEquals("chrXY",  am.cloneIfLenient(new ChrRangePosition("CHRXY", 100, 200), false ).getChromosome());				
		assertEquals("CHRmT",  am.cloneIfLenient(new ChrPointPosition("CHRmT", 100), true ).getChromosome());
		assertEquals("chrM",  am.cloneIfLenient(new ChrPointPosition("CHRmT", 100), false ).getChromosome());		
	}
	
	
}
