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
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.qio.vcf.VcfFileReader;

public class AbstractModeTest {
	public static String outputName = "output.vcf";
	public static String inputName = "input.vcf";
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@BeforeClass
	public static void createInput() throws IOException{	
		createVcf();
	}
	
	
	 @AfterClass
	 public static void deleteIO(){

		 new File(inputName).delete();
		 new File(outputName).delete();
		 
	 }	
	
	//test data
	@Test
	public void inputRecordCompoundSnp() throws Exception{
		
		final String[] params =  {"chr1","10180",".","TA","CT","."," MIN;MIUN","SOMATIC;END=10181","ACCS","TA,5,37,CA,0,2", "AA,1,1,CA,4,1,CT,3,1,TA,11,76,TT,2,2,_A,0,3,TG,0,1"};
		final VcfRecord record = new VcfRecord(params);
		assertEquals(10180, record.getPosition());
		assertEquals(10181, record.getChrPosition().getEndPosition());
		 
	}
	

	
	@Test
	public void reHeaderTest() throws Exception{
		
	   try (BufferedReader br = new BufferedReader(new FileReader(inputName))){
	    	   int i = 0;
	    	   while (  br.readLine() != null ) {
	    		   i++;
	    	   }
	    	   assertTrue(i == 3);
	   }
	       
		DbsnpMode db = new DbsnpMode();
		db.loadVcfRecordsFromFile(new File(inputName));		
		db.reheader("testing run",   inputName);
		db.writeVCF(new File(outputName));
		
		
        try (VcfFileReader reader = new VcfFileReader(new File(outputName))) {
        	int i = 0;
        	for (VcfHeaderRecord re :  reader.getVcfHeader()) {
        		if (re.toString().startsWith(VcfHeaderUtils.STANDARD_UUID_LINE)) {
        			// new UUID should have been inserted by now
        			assertEquals(false, "abcd_12345678_xzy_999666333".equals(StringUtils.getValueFromKey(re.getMetaValue(), VcfHeaderUtils.STANDARD_UUID_LINE)));
        		}
        		i ++;
        	}
        	assertEquals(7, i);	//removed blank lines
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
		mode.loadVcfRecordsFromFile(vcfFile);
		mode.writeVCF(outputVcfFile);
		
		/*
		 * now check the ordering
		 */
		try (VcfFileReader reader = new VcfFileReader(outputVcfFile)) {
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
		mode.loadVcfRecordsFromFile(vcfFile);
		mode.writeVCF(outputVcfFile);
		
		/*
		 * now check the ordering
		 */
		try (VcfFileReader reader = new VcfFileReader(outputVcfFile)) {
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
	
	public static void createVcf() throws IOException{
        final List<String> data = new ArrayList<>();
        data.add("##fileformat=VCFv4.0");
        data.add(VcfHeaderUtils.STANDARD_UUID_LINE + "=abcd_12345678_xzy_999666333");
        data.add("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO");
        createVcf(new File(inputName), data);
	}
	
	public static void createVcf(File vcfFile, List<String> data) throws IOException {
		try (BufferedWriter out = new BufferedWriter(new FileWriter(vcfFile));) {          
			for (final String line : data) {
				out.write(line +"\n");                  
			}
		 }
	}
}
