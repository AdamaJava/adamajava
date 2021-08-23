package org.qcmg.pileup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class OptionsIniTest {
	
	//test ini file by using default low_read_count and nonref_percent value
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	private String reference = getClass().getResource("/resources/test-reference.fa").getFile();
	private String bam = getClass().getResource("/resources/test.bam").getFile();
	private String hdf = getClass().getResource("/resources/test.h5").getFile();
	
	
	/*
	
	1. test add/remove twice in different range
	2. try merge twice in different rang h5s
	2. test [metrics]temporary_dir
	3. test default value for options under [general] threads, bam_override, loglevel, output_dir, range
	4. test merge whether check bootstrap settings
	
	*/
	//test ini file by using default low_read_count and nonref_percent value
	@Test
	public void bootstrapTest() throws Exception {				
		String generalSec = createGeneralSection( null, "bootstrap.h5", "bootstrap", null, null, null, new String[] {});
		//without setting low_read_count and nonref_percent
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
				
		//h5 exists
		generalSec = createGeneralSection( "level", hdf, "bootstrap", 10, true, "/current", new String[] {"all"});
		bootstrapSec = createBootstrap(reference, 1, 100);
		options = getIniOptions(generalSec, bootstrapSec, null, null);
		try {
			options.parseIniFile();
			fail("should not reach here!");
		}catch (Exception ex) {
			assertEquals(ex.getMessage(),  Messages.getMessage("EXISTING_HDF", hdf));
		}
						
		//with value setting
		generalSec = createGeneralSection( "level", "bootstrap.h5", "bootstrap", 10, true, "/current", new String[] {"all"});
		bootstrapSec = createBootstrap(reference, 1, 100);
		options = getIniOptions(generalSec, bootstrapSec, null, null);
		options.parseIniFile();
		
		//get value from ini
		assertEquals("level", options.getLogLevel());
		assertEquals(new Integer(1), options.getLowReadCount());
		assertEquals(new Integer(100), options.getPercentNonRef());
		assertEquals(10, options.getThreadNo());
		assertEquals(true, options.isBamOverride());
		assertEquals(null, options.getOutputDir()); //only for view and metrics
		assertEquals(0, options.getReadRanges().size());		
	}	
	
	@Test
	public void viewTest() throws Exception {
		String generalSec = createGeneralSection( null, hdf, "view", null, null, null, new String[] {});	
		//empty view
		String viewSec = createView(new String[] {},new String[] {}) ; 
		Options options = getIniOptions(generalSec, null, viewSec,  null);
		
		try {
			options.parseIniFile();
			fail("should not reach here!");
		}catch (Exception ex) {
			assertEquals(ex.getMessage(),  Messages.getMessage("NO_OPTION", "output_dir"));
		}

		assertEquals("view", options.getMode());		
		assertEquals(hdf, options.getHdfFile());
		//default range is all
		assertEquals("all", options.getReadRanges().get(0));
		assertEquals(0, options.getGroupElements().size());
		assertEquals(0, options.getViewElements().size());
		assertEquals(null, options.getLowReadCount());
		assertEquals(null, options.getPercentNonRef());		
		
		generalSec = createGeneralSection( null, hdf, "view", 3, null, testFolder.getRoot().toString(), new String[] {"chr1", "chr1:1-100"});
		//forward or reverse will add all strandEnum
		viewSec = createView(new String[] {"Aqual", "Nqual"},new String[] {"forward"}) ; 
		options = getIniOptions(generalSec, null, viewSec,  null);
		options.parseIniFile();
		assertEquals("chr1", options.getReadRanges().get(0));		
		//2 elements: "Aqual", "Nqual"
		assertEquals(2, options.getViewElements().size());
		//forward or reverse will add all strandEnum
		assertEquals(29, options.getGroupElements().size());
		
		
		viewSec = createView(new String[] {"Gqual" ,"Tqual"},new String[] {"quals", "bases"}) ; 
		options = getIniOptions(generalSec, null, viewSec,  null);
		options.parseIniFile();
		//two element from 
		assertEquals(2, options.getViewElements().size());
		//9 elements from bases
		assertEquals(9, options.getGroupElements().size());
				
	}	
	
	@Test
	public void mergeTest() throws Exception {
		String generalSec = createGeneralSection( null, hdf, "merge", 3, null, null, new String[] {"chr1", "chr1:1-100"});
		String viewSec = createView(new String[] {"Gqual" ,"Tqual"},new String[] {"quals", "bases"}) ; 
		String mergeSec = createMerge(new String[] {hdf ,hdf}) ; 
		Options options = getIniOptions(generalSec, null, viewSec,  mergeSec);
		
		checkException(options, "missing bootstrap"); //missing bootstrap

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
	
	private String createGeneralSection( String level, String hdf, String mode, Integer thread, Boolean override, String outDir, String[] range) {
		
		String str = "[general]\n";
		str += "log=log\n";
		str += (level == null)? "" :  "loglevel="+level+"\n";
		str += "hdf="+hdf+"\n";
		str += "mode="+mode+"\n";
		str += (thread == null)? "" : "thread_no="+thread +"\n";
		str += (override == null)? "" : "bam_override="+override+"\n";
		str += (outDir == null)? "" :"output_dir="+outDir+"\n";
		for(String s : range) { str += "range="+s+"\n"; }
		 
		return str;		
	}
		
	private String createBootstrap(String ref, Integer low, Integer nonref) {
		
		String str = "[bootstrap]\n";
		str += "reference = "+ ref +"\n";		 
 		str += (low == null)? "" : "low_read_count = " + low +"\n";
		str += (nonref == null)? "" : "nonref_percent =" + nonref + "\n";
		
		return str;		
	}

	private String createView(String[] element, String[] group) {
		String str = "[view]\n";
		for(String s : element) { str += "element = " + s + "\n"; }
		for(String s : group) { str += "group = " + s + "\n"; }
		
		return str;
	}

	private String createMerge(String[] hdfs) {
		String str = "[merge]\n";
		for(String h : hdfs) { str += "input_hdf = " + h + "\n"; }
		 		
		return str;
	}
	
	private void checkException(Options options, String errMsg) {
		
		
		try {
			options.parseIniFile();
			fail("should not reach here!");
		}catch (Exception ex) {
			assertEquals(ex.getMessage(),  errMsg);
		}

		
		
	}
	
}
