package au.edu.qimr.indel;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
 

public class IniFileTest {
	public static final String ini = "test.ini";
	public static final String output = "test.output.vcf";
	public static final File fini = new File( ini);
	
	
	@org.junit.Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@After
	public void after() {	
		fini.delete();
	}
	@Test
	public void testQuery() throws IOException, Q3IndelException {
		 		
		//ini file not exist
		String[] args = {"-i", ini};
		try {
			new Options(args);
			Assert.fail("Should have thrown a SnpException");
		} catch (Exception e) {}
		
		// create ini file without query and some dodgy file
		createIniFile(fini,fini,fini,fini,fini, null, new File("blah"));	 
		Options options = new Options(args);	
		assertTrue(options.getFilterQuery() == null);
		
		// create ini file with empty query 
		createIniFile(fini,fini,fini,fini,fini, "", new File("blah"));	 
		options = new Options(args);	
		assertTrue(options.getFilterQuery() == null);
	}	
	@Test
	public final void testIni() {
		
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
			assertEquals("OESO-5007", options.getDonorId());
			
			assertTrue(options.getControlSample().equals("Normalcontrol(othersite):a6b558da-ab2d-4e92-a029-6544fb98653b"));					
			assertTrue(options.getTestSample().equals("Primarytumour:4ca050b3-d15b-436b-b035-d6c1925b59fb"));
		} catch (Exception e) {
			Assert.fail("Should not threw a Exception");
		}
		
	}
	
	
	public static void createIniFile(String ini, String testbam, String controlbam, String testvcf, String controlvcf,  String query){
		createIniFile(new File( ini), new File(testbam), new File(controlbam), new File(testvcf), new File(controlvcf),   query, "gatk", new File(output));
				
	}
	
	public static void createIniFile(File ini, File testbam, File controlbam, File testvcf, File controlvcf,  String query, File outputVcfFile){
		createIniFile( ini,  testbam, controlbam, testvcf,  controlvcf,  query, "gatk", outputVcfFile);
	}
	
	public static void createIniFile(File ini, File testbam, File controlbam, File testvcf, File controlvcf,  String query, String mode, File outputVcfFile){
		
        List<String> data = new ArrayList<>();
        data.add("[IOs]");
        data.add( "ref=");
        
        data.add("testBam=" + (testbam == null? "":testbam.getAbsolutePath()));
        data.add("controlBam="  + (controlbam == null? "":controlbam.getAbsolutePath()));
        data.add("testVcf="  + (testvcf == null? "":testvcf.getAbsolutePath()));
        data.add("controlVcf="  + (controlvcf == null? "":controlvcf.getAbsolutePath()));
                
        data.add("output=" + outputVcfFile.getAbsolutePath() );
        data.add("[ids]");
        data.add("donorId=OESO-5007");
        data.add("analysisId=f6290103-b775-41f8-8880-331e91aeabdc");
        data.add("testSample=Primarytumour:4ca050b3-d15b-436b-b035-d6c1925b59fb");
        data.add("controlSample=Normalcontrol(othersite):a6b558da-ab2d-4e92-a029-6544fb98653b");
        data.add("[parameters]");
        data.add("runMode=" + mode);
        data.add("threadNo=5");
        data.add("filter=" + query);
        data.add("window.nearbyIndel=3");
        data.add("window.homopolymer=100,10");
        data.add("window.softClip =13");
        data.add("strong.event=3");  
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
