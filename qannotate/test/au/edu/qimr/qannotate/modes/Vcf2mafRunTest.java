package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

import au.edu.qimr.qannotate.Options;
import au.edu.qimr.qannotate.utils.MafElement;
import au.edu.qimr.qannotate.utils.SnpEffMafRecord;

public class Vcf2mafRunTest {
	
    @org.junit.Rule
    public  TemporaryFolder testFolder = new TemporaryFolder();
	
	
    @Test
    public void fileNameTest() throws IOException {
    	//create vcf
        String[] str = {VcfHeaderUtils.STANDARD_FILE_FORMAT + "=VCFv4.0",            
                VcfHeaderUtils.STANDARD_DONOR_ID + "=MELA_0264",
                VcfHeaderUtils.STANDARD_CONTROL_BAMID + "=CONTROL_UUID",
                VcfHeaderUtils.STANDARD_TEST_BAMID + "=TEST_UUID",                
                VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=CONTROL_sample",
                VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=TEST_sample",                
                VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL_UUID\tTEST_UUID"    
        };                    
         
        File out = runMainTest(true, str); // output to dir
        
        //check output
        try{    
            assertTrue(new File(out.getAbsolutePath() + "/MELA_0264.CONTROL_sample.TEST_sample.maf").exists());
            //below empty files will be deleted at last stage
            assertFalse(new File(out.getAbsolutePath() + "/MELA_0264.CONTROL.TEST.Somatic.HighConfidence.Consequence.maf").exists());
            assertFalse(new File(out.getAbsolutePath() + "/MELA_0264.CONTROL.TEST.Germline.HighConfidence.maf").exists());
            assertFalse(new File(out.getAbsolutePath() + "/MELA_0264.CONTROL.TEST.Somatic.HighConfidence.maf").exists());        
            assertFalse(new File(out.getAbsolutePath() + "/MELA_0264.CONTROL.TEST.Germline.HighConfidence.Consequence.maf").exists());            
        }catch(Exception e){
            fail(e.getMessage()); 
        }
    }


    @Test
    public void fileNameWithNODonorTest() throws IOException{
        String[] str = {"##fileformat=VCFv4.0",            
                "##qControlSample=CONTROL",
                "##qTestSample=TEST",                
                VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL\tTEST" };

        assertEquals(1, executeTest( str)); 
    }
    
    @Test
    public void fileNameWithNoSampleidTest() throws IOException{
        String[] str = {"##fileformat=VCFv4.0",            
                VcfHeaderUtils.STANDARD_DONOR_ID +"=MELA_0264",
                VcfHeaderUtils.STANDARD_TEST_BAMID +"=TEST_uuid",                
                VcfHeaderUtils.STANDARD_CONTROL_BAMID +"=CONTROL_uuid",                
                VcfHeaderUtils.STANDARD_TEST_SAMPLE +"=TEST_sample",                
                VcfHeaderUtils.STANDARD_CONTROL_SAMPLE +"=CONTROL_sample",                
                VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL_uuid\tTEST_uuid"};

        assertEquals(0, executeTest( str));       
    }
    
    @Test
    public void bamIdTest() {
       //create vcf
       String[] str = {                
               VcfHeaderUtils.STANDARD_FILE_FORMAT + "=VCFv4.0",    
               VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=CONTROL_sample",
               VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=TEST_sample",    
               VcfHeaderUtils.STANDARD_TEST_BAMID + "=TEST_bamID",                
               VcfHeaderUtils.STANDARD_CONTROL_BAMID + "=CONTROL_bamID",                
               VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL_bamID\tTEST_bamID",
               "chr1\t7722099\trs6698334\tC\tT\t.\t.\tBaseQRankSum=-0.736;ClippingRankSum=0.736;DP=3;FS=0.000;MQ=60.00;MQ0=0;MQRankSum=0.736;QD=14.92;ReadPosRankSum=0.736;SOR=0.223;IN=2;DB;VLD;VAF=0.06887;EFF=intron_variant(MODIFIER|||c.805+173C>T|1673|CAMTA1|protein_coding|CODING|ENST00000303635|8|1),intron_variant(MODIFIER|||c.805+173C>T|1659|CAMTA1|protein_coding|CODING|ENST00000439411|8|1)\tGT:AD:DP:GQ:FT:MR:NNS:OABS:INF\t.:.:.:.:.:.:.:.:CONF=ZERO\t.:.:.:.:.:.:.:.:.\t0/1:1,2:3:35:COVN8:2:2:C0[0]1[39];T1[35]1[37]:CONF=ZERO\t.:.:.:.:.:.:.:.:."};              
       
       //run vcf2maf 
       File out = runMainTest(false, str); // not dir but output file 
            
       //check output
       try(BufferedReader br = new BufferedReader(new FileReader(out));) {
           String line = null;
           while ((line = br.readLine()) != null) {
                if(line.startsWith("#") || line.startsWith(MafElement.Hugo_Symbol.name())) continue; //skip vcf header                   
                //won't output last two column about ACLAP in any case of vcf2maf mode
                assertEquals(62, line.split("\\t").length);

                SnpEffMafRecord maf =  Vcf2mafIndelTest.toMafRecord(line, false);        
                assertTrue(maf.getColumnValue(16).equals("TEST_bamID"));
                assertEquals("CONTROL_bamID", maf.getColumnValue(MafElement.Matched_Norm_Sample_Barcode));     
                assertTrue(maf.getColumnValue(33).equals("TEST_sample"));
                assertEquals("TEST_bamID:CONTROL_bamID", maf.getColumnValue(MafElement.BAM_File));
           }    
       } catch (Exception e) {
    	   fail("exception during checking the output of vcf2maf!");
       } 
    }    
    
    
    @Test
    public void hasACLAPtest() throws IOException {
        
       String[] str = {"##fileformat=VCFv4.0",            
               "##qControlSample=CONTROL",
               "##qTestSample=TEST",    
               "##FORMAT=<ID=ACLAP,Number=.,Type=Integer,Description=\"test\">",             
               VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL\tTEST",
               "chr10\t100\trs386746181\tTG\tCC\t.\tPASS\tSOMATIC;DB;CONF=HIGH;"
                       + "\tACCS:ACLAP\tTG,29,36,_G,0,1:.\tCC,4,12,TG,15,12:testAC",
               "chr10\t200\trs386746181\tTG\tCC\t.\tPASS\tSOMATIC;DB;CONF=HIGH;"
                       + "\tACCS\tTG,29,36,_G,0,1\tCC,4,12,TG,15,12" };

       File out = runMainTest(false, str); // output to dir
       
       try(BufferedReader br = new BufferedReader(new FileReader(out));) {
           String line = null;
           while ((line = br.readLine()) != null) {
               if(line.startsWith("#") || line.startsWith(MafElement.Hugo_Symbol.name())) continue; //skip vcf header
               //won't output last two column even there is ACLAP in vcf header
               assertEquals(62, line.split("\\t").length);
                  
               //must set to false, since ignore ACLAP
               SnpEffMafRecord maf =  Vcf2mafIndelTest.toMafRecord(line, false);  
         	   assertEquals(maf.getColumnValue(63), SnpEffMafRecord.Null ); 
        	   assertEquals(maf.getColumnValue(63), SnpEffMafRecord.Null );            	   
         
           }
       }
    	
    } 
    
    public void areVcfFilesCreated() throws Exception {
        String[] str = {VcfHeaderUtils.STANDARD_FILE_FORMAT + "=VCFv4.0",            
                VcfHeaderUtils.STANDARD_DONOR_ID + "=ABCD_1234",
                VcfHeaderUtils.STANDARD_CONTROL_BAMID + "=CONTROL_uuid",
                VcfHeaderUtils.STANDARD_TEST_BAMID + "=TEST_uuid",                
                VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=TEST_sample",                
                VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=CONTROL_sample",                
                VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL_uuid\tTEST_uuid"    ,
                "chr10\t87489317\trs386746181\tTG\tCC\t.\tPASS\tSOMATIC;DB;CONF=HIGH;"
                         + "EFF=start_lost(HIGH||atg/GGtg|p.Met1?|580|GRID1|protein_coding|CODING|ENST00000536331||1);"
                         + "LOF=(GRID1|ENSG00000182771|4|0.25);END=87489318\tACCS\tTG,29,36,_G,0,1\tCC,4,12,TG,15,12"
        };
        
            
        File vcf = createInput(str);
        File output = testFolder.newFile();
                
        String [] command = {"--mode", "vcf2maf", "--log" , output.getParent() + "/output.log",  "-i" , vcf.getAbsolutePath() , "-o" , output.getAbsolutePath()};            
        Options options = new Options(command);
        options.parseArgs(command);
        new Vcf2maf(options );
            
        String SHCC  = output.getAbsolutePath().replace(".maf", ".Somatic.HighConfidence.Consequence.maf") ;
        String SHC = output.getAbsolutePath().replace(".maf", ".Somatic.HighConfidence.maf") ;
        String GHCC  = output.getAbsolutePath().replace(".maf", ".Germline.HighConfidence.Consequence.maf") ;
        String GHC = output.getAbsolutePath().replace(".maf", ".Germline.HighConfidence.maf") ;
        String SHCCVcf  = output.getAbsolutePath().replace(".maf", ".Somatic.HighConfidence.Consequence.vcf") ;
        String SHCVcf = output.getAbsolutePath().replace(".maf", ".Somatic.HighConfidence.vcf") ;
        String GHCCVcf  = output.getAbsolutePath().replace(".maf", ".Germline.HighConfidence.Consequence.vcf") ;
        String GHCVcf = output.getAbsolutePath().replace(".maf", ".Germline.HighConfidence.vcf") ;
        
        
        assertEquals(true, output.exists());
        assertEquals(true, new File(output.getAbsolutePath().replaceAll("maf", ".vcf")).exists());
        assertEquals(true, new File(SHCC).exists());
        assertEquals(true, new File(SHC).exists());
        assertEquals(true, new File(GHCC).exists());
        assertEquals(true, new File(GHC).exists());
        assertEquals(true, new File(SHCCVcf).exists());
        assertEquals(true, new File(SHCVcf).exists());
        assertEquals(true, new File(GHCCVcf).exists());
        assertEquals(true, new File(GHCVcf).exists());
    }   
    
    
    /**
     * 
     * @param str is the vcf string
     * @return a vcf file
     */
    private File createInput(String[] str) {
		File input = null;
		try {
			input = testFolder.newFile();		  
		} catch (IOException e) {
			fail("exception during creating file in TemporaryFolder!");
		}
		
		try(PrintWriter out = new PrintWriter(new FileWriter(input));) {
			out.println(Arrays.stream(str).collect(Collectors.joining(Constants.NL_STRING)));          
		} catch (IOException e) {
			fail("exception during writing to file in TemporaryFolder!");
		} 
		return input;
    }
   
    /**
     * only used by fileNameWithNoSampleidTest() and fileNameWithNODonorTest()
     * @param str
     * @return execute code
     */
	private int executeTest(String[] str) {
	      
		File input = createInput(str);	      
	    //run vcf2maf
		try {
		      File log = testFolder.newFile();
		      File output = testFolder.newFolder();
		      final String command = "-mode vcf2maf   --log " + log.getAbsolutePath() + " -i " + input.getAbsolutePath() 
		      	+ " --outdir " + output.getAbsolutePath();    
		      Executor exec = new Executor(command, "au.edu.qimr.qannotate.Main");
		      return exec.getErrCode() ;
		} catch (Exception e) {
			fail("failed during run vcf2maf mode!");
		}  
	      
	    return -1;	 
	}
	
	/**
	 * only used by filenameTest() and bamIdTest()
	 * @param isDir set output a direcotry or file
	 * @param str is vcf txt 
	 * @return output file or direcotory from main
	 */
	private File runMainTest(boolean isDir, String[] str) {
		
		File input = createInput(str);	
		String opt_o = isDir? "--outdir" : "-o";
		
        //run vcf2maf
        File out = null;
        try {
    	   out = isDir? testFolder.newFolder() : testFolder.newFile();
           final String[] command = {"--mode", "vcf2maf",  "-i", input.getAbsolutePath() , opt_o , out.getAbsolutePath()
        		   , "--log",  testFolder.newFile().getAbsolutePath() };
           au.edu.qimr.qannotate.Main.main(command);
        } catch ( Exception e) {
        	fail(e.getMessage()); 
        }   
	       
	    return out;
	}
   
}
