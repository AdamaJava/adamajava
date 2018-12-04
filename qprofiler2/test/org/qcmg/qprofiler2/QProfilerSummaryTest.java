package org.qcmg.qprofiler2;



import static org.junit.Assert.*;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qprofiler2.QProfiler2;
import org.qcmg.qprofiler2.bam.BamSummarizerTest;


public class QProfilerSummaryTest {
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
		
	@Test
	/**
	 * The only characters not allowed in a filename in *nix are NUL and /. 
	 * In Windows, only NUL, :, and \ are truly not allowed, 
	 * but many apps restrict that further, also preventing ?, *, +, and %.
	 * @throws Exception
	 */
	public void filenameTest() throws Exception{
		
		//test file name contains allowed special letters	
		//for(String name : new String[]{"in#:.sam", "in,.sam","input?.sam", "in*.sam","...sam", "input\n.sam"}){	
		for(String name : new String[]{",in#:.sam","...sam", "input\n.sam"}){	
			int exitStatus = run(name, name+".xml");	
			assertEquals(0, exitStatus);	
		}		
		
		// not allowed special letters		
		for(String name : new String[]{ "in/.sam", null})
			try{
				run(name, name+".xml");				
				fail();
			}catch(Exception e){ assertTrue(true); }			
	}
	
	private int run(String input, String output) throws Exception{
			
		File inputFile = testFolder.newFile(input);			
		BamSummarizerTest.createTestSamFile(inputFile.getAbsolutePath(), BamSummarizerTest.createValidSamData());	
		
		File logFile = testFolder.newFile(output +".log");
		File outputFile = testFolder.newFile(output);
		String[] args = {"--nohtml", "--log",  logFile.getAbsolutePath(), "--input", inputFile.getAbsolutePath(),
				 "-o", outputFile.getAbsolutePath()};
		int exitStatus =  new QProfiler2().setup(args); //not main, so no exit info on log file
		
		assertTrue(outputFile.exists());		
		return exitStatus;
	}
}
