package org.qcmg.qprofiler2;

import junit.framework.Assert;

import org.junit.Test;
import org.qcmg.qprofiler2.Options2;

public class OptionsTest {
	public static final String FILE_1 = "file1.xml"; 
	public static final String FILE_2 = "file2.xml"; 
	public static final String FILE_3 = "file3.xml"; 
	public static final String FILE_4 = "file4.xml"; 

	@Test
	public void testNullOptions() throws Exception {
		Options2 options = new Options2(new String[] {});
		Assert.assertEquals(0, options.getFileNames().length);
		Assert.assertEquals(0, options.getBamIncludes().length);
		Assert.assertEquals(0, options.getMaxRecords());
		Assert.assertEquals(0, options.getNoOfConsumerThreads());
	}
	
	@Test
	public void testFileNamesOnlyOptions() throws Exception {
		Options2 options = new Options2(new String[] {"-input", FILE_1});
		Assert.assertEquals(1, options.getFileNames().length);
		Assert.assertEquals(FILE_1, options.getFileNames()[0]);
		Assert.assertEquals(0, options.getBamIncludes().length);
		Assert.assertEquals(0, options.getMaxRecords());
		Assert.assertEquals(0, options.getNoOfConsumerThreads());
		
		// and with multiple files
		options = new Options2(new String[] {"-inp", FILE_1, "-inp", FILE_2, "--input", FILE_3, "--input", FILE_4});
		Assert.assertEquals(4, options.getFileNames().length);
		Assert.assertEquals(FILE_1, options.getFileNames()[0]);
		Assert.assertEquals(FILE_2, options.getFileNames()[1]);
		Assert.assertEquals(FILE_3, options.getFileNames()[2]);
		Assert.assertEquals(FILE_4, options.getFileNames()[3]);
		Assert.assertEquals(0, options.getBamIncludes().length);
		Assert.assertEquals(0, options.getMaxRecords());
		Assert.assertEquals(0, options.getNoOfConsumerThreads());
	}
	
	@Test
	public void testExcludesOnlyOptions() throws Exception {
		Options2 options = new Options2(new String[] {"--include","all"});
		Assert.assertEquals(0, options.getFileNames().length);
		Assert.assertEquals(1, options.getBamIncludes().length);
		Assert.assertEquals("all", options.getBamIncludes()[0]);
		Assert.assertEquals(0, options.getMaxRecords());
		Assert.assertEquals(0, options.getNoOfConsumerThreads());
		
		// shortcut
		options = new Options2(new String[] {"--inc","all"});
		Assert.assertEquals(1, options.getBamIncludes().length);
		Assert.assertEquals("all", options.getBamIncludes()[0]);
		
		// shortcut and =
		options = new Options2(new String[] {"--inc=all"});
		Assert.assertEquals(1, options.getBamIncludes().length);
		Assert.assertEquals("all", options.getBamIncludes()[0]);
		
		// additional excludes
		options = new Options2(new String[] {"-inc","all,coverage,matrices"});
		Assert.assertEquals(3, options.getBamIncludes().length);
		Assert.assertEquals("all", options.getBamIncludes()[0]);
		Assert.assertEquals("coverage", options.getBamIncludes()[1]);
		Assert.assertEquals("matrices", options.getBamIncludes()[2]);
		
		// additional exludes
		options = new Options2(new String[] {"-include","all,coverage,matrices"});
		Assert.assertEquals(3, options.getBamIncludes().length);
		Assert.assertEquals("all", options.getBamIncludes()[0]);
		Assert.assertEquals("coverage", options.getBamIncludes()[1]);
		Assert.assertEquals("matrices", options.getBamIncludes()[2]);
		
		// empty options
		try {
			new Options2(new String[] {"--include"});
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {}
	}
	
	@Test
	public void testMaxRecordsOptions() throws Exception {
		Options2 options = new Options2(new String[] {"--maxRecords","2"});
		Assert.assertEquals(0, options.getFileNames().length);
		Assert.assertEquals(0, options.getBamIncludes().length);
		Assert.assertEquals(2, options.getMaxRecords());
		Assert.assertEquals(0, options.getNoOfConsumerThreads());
		
		// shortcut
		options = new Options2(new String[] {"--m","10"});
		Assert.assertEquals(10, options.getMaxRecords());
		
		// invalid option (needs to be integer)
		try {
			new Options2(new String[] {"-max","matrices"});
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {}
		
		// more than 1 value, will throw an exception as they are considered to be nonoption arguments
		try {
			new Options2(new String[] {"--maxRecords","2","3","4","5"});
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {}
		
		// more than 1 value, will throw an exception as they are considered to be nonoption arguments
		try {
			new Options2(new String[] {"--maxRecords","2","--maxRecords","3","--maxRecords","4","--maxRecords","5"});
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {}
		
		// empty options
		try {
			new Options2(new String[] {"--maxRecords"});
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {}
	}
	
	@Test
	public void testNoOfThreadsOptions() throws Exception {
		Options2 options = new Options2(new String[] {"--ntC","2"});
		Assert.assertEquals(0, options.getFileNames().length);
		Assert.assertEquals(0, options.getBamIncludes().length);
		Assert.assertEquals(0, options.getMaxRecords());
		Assert.assertEquals(2, options.getNoOfConsumerThreads());
		
		// shortcut
		options = new Options2(new String[] {"--ntC","10"});
		Assert.assertEquals(10, options.getNoOfConsumerThreads());
		// shortcut again
		options = new Options2(new String[] {"-ntP","11"});
		Assert.assertEquals(11, options.getNoOfProducerThreads());
		
		// invalid option (needs to be integer)
		try {
			new Options2(new String[] {"-ntP","matrices"});
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {}
		
		// more than 1 value, will throw an exception as they are considered to be nonoption arguments
		try {
			new Options2(new String[] {"--ntC","2","3","4","5"});
			Assert.fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {}
		
		// multiple values - will throw an exception
		try {
			new Options2(new String[] {"-ntP","2","-ntC","3","-ntC","4","-ntP","5"});
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {}
		
		// empty options
		try {
			new Options2(new String[] {"--ntC"});
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {}
	}
}
