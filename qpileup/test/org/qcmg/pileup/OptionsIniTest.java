package org.qcmg.pileup;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.pileup.util.TestUtil;

public class OptionsIniTest {
	
	//test ini file by using default low_read_count and nonref_percent value
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	private String reference = getClass().getResource("/resources/test-reference.fa").getFile();
	private String bam = getClass().getResource("/resources/test.bam").getFile();
	private String hdf = getClass().getResource("/resources/test.h5").getFile();
	
	
	/*plans:
	
	1. test add/remove twice in different range
	2. try merge twice in different rang h5s
	2. test [metrics]temporary_dir
	3. test default value for options under [general] threads, bam_override, loglevel, output_dir, range
	4. test merge whether check bootstrap settings
	
	*/
	//test ini file by using default low_read_count and nonref_percent value
	@Test
	public void bootstrapTest() throws Exception {
		
		//without setting low_read_count and nonref_percent
		String generalSec = createGeneralSection( null, "bootstrap.h5", "bootstrap", null, null, null, null);
		String bootstrapSec = createBootstrap(reference, null, null);
		
		Options options = getIniOptions(generalSec, bootstrapSec, null, null);
		options.parseIniFile();
		
		assertEquals("bootstrap", options.getMode());		
		assertEquals("bootstrap.h5", options.getHdfFile());
		assertEquals(reference, options.getReferenceFile());
		
		//default value
		assertEquals("INFO", options.getLogLevel());
		assertEquals(new Integer(10), options.getLowReadCount());
		assertEquals(new Integer(20), options.getPercentNonRef());
		assertEquals(1, options.getThreadNo());
		assertEquals(false, options.isBamOverride());
		assertEquals(null, options.getOutputDir());
		assertEquals(0, options.getReadRanges().size());
				
		//with value setting
		generalSec = createGeneralSection( "level", "bootstrap.h5", "bootstrap", 10, true, "/current", "all");
		bootstrapSec = createBootstrap(reference, 1, 100);
		options = getIniOptions(generalSec, bootstrapSec, null, null);
		options.parseIniFile();
		
		//default value
		assertEquals("level", options.getLogLevel());
		assertEquals(new Integer(1), options.getLowReadCount());
		assertEquals(new Integer(100), options.getPercentNonRef());
		assertEquals(10, options.getThreadNo());
		assertEquals(true, options.isBamOverride());
		assertEquals(null, options.getOutputDir());
		assertEquals(0, options.getReadRanges().size());
	}	
	
	public Options getIniOptions(String general, String bootstrap, String view, String merge) throws Exception {
		
		File iniFolder = testFolder.newFolder();		
		File iniFile = new File(iniFolder, "test.ini");
		
		BufferedWriter out = new BufferedWriter(new FileWriter(iniFile));
		out.write(general);
		if(bootstrap != null ) out.write(bootstrap);
		if(view != null ) out.write(view);
		if(merge != null ) out.write(merge);		
		out.close();		
		
		String[] args = {"--ini", iniFile.getAbsolutePath()};
		Options options = new Options(args);
		
		return options;
	}	
	
	private String createGeneralSection( String level, String hdf, String mode, Integer thread, Boolean override, String outDir, String range) {
		
		String str = "[general]\n";
		str += "log=log\n";
		str += (level == null)? "" :  "loglevel="+level+"\n";
		str += "hdf="+hdf+"\n";
		str += "mode="+mode+"\n";
		str += (thread == null)? "" : "thread_no="+thread +"\n";
		str += (override == null)? "" : "bam_override="+override+"\n";
		str += (outDir == null)? "" :"output_dir="+outDir+"\n";
		str += (range == null)? "" :"range="+range+"\n";		
		
		return str;		
	}
	
	
	private String createBootstrap(String ref, Integer low, Integer nonref) {
		
		String str = "[bootstrap]\n";
		str += "reference = "+ ref +"\n";		 
 		str += (low == null)? "" : "low_read_count = " + low +"\n";
		str += (nonref == null)? "" : "nonref_percent =" + nonref + "\n";
		
		return str;		
	}

}
