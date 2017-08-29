package au.edu.qimr.indel;

import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.common.util.Constants;
 

public class IniFileTest {
	public static final String ini = "test.ini";
	public static final String output = "test.output.vcf";
	public static final File fini = new File( ini);
	
	
	@After
	public void after() throws IOException{	
		fini.delete();
	}
	@Test
	public void testquery() throws Exception {
		 		
		//ini file not exist
		String[] args = {"-i", ini};
		try {
			Options options = new Options(args);
			Assert.fail("Should have thrown a SnpException");
		} catch (Exception e) {}
		
		// create ini file without query and some dodgy file
		createIniFile(fini,fini,fini,fini,fini, null);	 
		try {
			Options options = new Options(args);	
			assertTrue(options.getFilterQuery() == null);
		} catch (Exception e) {
			Assert.fail("Should not threw a Exception");
		}
		
		// create ini file with empty query 
		createIniFile(fini,fini,fini,fini,fini, "");	 
		try {
			Options options = new Options(args);	
			assertTrue(options.getFilterQuery() == null);
		} catch (Exception e) {
			Assert.fail("Should not threw a Exception");
		}
	}	
	@Test
	public final void testIni() throws Exception {
		
		// create ini file with  query 
		String str = "and (flag_ReadUnmapped != true, flag_DuplicatedRead != true)";
	//	File f = new File( ini);
		createIniFile(ini,ini,ini,ini,ini, str);	 
		
		try {
			Options options = new Options(new String[]{"-i", ini});	
			assertTrue(options.getFilterQuery().equalsIgnoreCase(str));
			assertTrue(options.getControlBam().getAbsolutePath().equals(fini.getAbsolutePath() ));
			assertTrue(options.getTestBam().getAbsolutePath().equals(fini.getAbsolutePath()));
			assertTrue(options.getControlInputVcf().getAbsolutePath().equals(fini.getAbsolutePath()));
			assertTrue(options.getTestInputVcf().getAbsolutePath().equals(fini.getAbsolutePath()));
			
			assertTrue(options.getControlSample().equals("Normalcontrol(othersite):a6b558da-ab2d-4e92-a029-6544fb98653b"));					
			assertTrue(options.getTestSample().equals("Primarytumour:4ca050b3-d15b-436b-b035-d6c1925b59fb"));
		} catch (Exception e) {
			Assert.fail("Should not threw a Exception");
		}
		
	}
	
	
	public static void createIniFile(String ini, String testbam, String controlbam, String testvcf, String controlvcf,  String query){
		createIniFile(new File( ini), new File(testbam), new File(controlbam), new File(testvcf), new File(controlvcf),   query, "gatk");
				
	}
	
	public static void createIniFile(File ini, File testbam, File controlbam, File testvcf, File controlvcf,  String query){
		createIniFile( ini,  testbam, controlbam, testvcf,  controlvcf,  query, "gatk");
	}
	
	public static void createIniFile(File ini, File testbam, File controlbam, File testvcf, File controlvcf,  String query, String mode){
		
        List<String> data = new ArrayList<String>();
        data.add("[IOs]");
        data.add( "ref=");
        
        data.add("testBam=" + (testbam == null? "":testbam.getAbsolutePath()));
        data.add("controlBam="  + (controlbam == null? "":controlbam.getAbsolutePath()));
        data.add("testVcf="  + (testvcf == null? "":testvcf.getAbsolutePath()));
        data.add("controlVcf="  + (controlvcf == null? "":controlvcf.getAbsolutePath()));
                
        data.add("output=" + output );
        data.add("");
        data.add("[ids]");
        data.add("donorId=OESO-5007");
        data.add("analysisId=f6290103-b775-41f8-8880-331e91aeabdc");
        data.add("testSample=Primarytumour:4ca050b3-d15b-436b-b035-d6c1925b59fb");
        data.add("controlSample=Normalcontrol(othersite):a6b558da-ab2d-4e92-a029-6544fb98653b");
        data.add("");
        data.add("[parameters]");
        data.add("runMode=" + mode);
        data.add("threadNo=5");
        data.add("filter=" + query);
        data.add("window.nearbyIndel=3");
        data.add("window.homopolymer=100,10");
        data.add("window.softClip =13");
        data.add("strong.event=3");  
        data.add("");
        data.add("[rules]");
        data.add("#discard all duplicate reads");
        data.add("exclude.Duplicates=true");
        data.add("gematic.nns=2");
        data.add("gematic.soi=0.15");  
       	      	     	    
        try( BufferedWriter out = new BufferedWriter(new FileWriter(ini))) {	           
           for (String line : data)  
                   out.write(line + "\n");	           	            
        }catch(IOException e){
        	System.err.println( Q3IndelException.getStrackTrace(e));	 	        	 
        	assertTrue(false);
        }	
	}

}
