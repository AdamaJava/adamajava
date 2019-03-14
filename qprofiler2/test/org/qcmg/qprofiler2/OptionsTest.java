package org.qcmg.qprofiler2;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
		assertEquals(0, options.getFileNames().length);
		assertEquals(0, options.getMaxRecords());
		assertEquals(0, options.getNoOfConsumerThreads());
	}
	
	@Test
	public void testFileNamesOnlyOptions() throws Exception {
		Options2 options = new Options2(new String[] {"-input", FILE_1});
		assertEquals(1, options.getFileNames().length);
		assertEquals(FILE_1, options.getFileNames()[0]);
		assertEquals(0, options.getMaxRecords());
		assertEquals(0, options.getNoOfConsumerThreads());
		
		// and with multiple files
		options = new Options2(new String[] {"-inp", FILE_1, "-inp", FILE_2, "--input", FILE_3, "--input", FILE_4});
		assertEquals(4, options.getFileNames().length);
		assertEquals(FILE_1, options.getFileNames()[0]);
		assertEquals(FILE_2, options.getFileNames()[1]);
		assertEquals(FILE_3, options.getFileNames()[2]);
		assertEquals(FILE_4, options.getFileNames()[3]);
		assertEquals(0, options.getMaxRecords());
		assertEquals(0, options.getNoOfConsumerThreads());
	}
	

	
	@Test
	public void testMaxRecordsOptions() throws Exception {
		Options2 options = new Options2(new String[] {"--maxRecords","2"});
		assertEquals(0, options.getFileNames().length);
		assertEquals(2, options.getMaxRecords());
		assertEquals(0, options.getNoOfConsumerThreads());
		
		// shortcut
		options = new Options2(new String[] {"--m","10"});
		assertEquals(10, options.getMaxRecords());
		
		// invalid option (needs to be integer)
		try {
			new Options2(new String[] {"-max","matrices"});
			fail("Should have thrown an exception");
		} catch (Exception e) {}
		
		// more than 1 value, will throw an exception as they are considered to be nonoption arguments
		try {
			new Options2(new String[] {"--maxRecords","2","3","4","5"});
			fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {}
		
		// more than 1 value, will throw an exception as they are considered to be nonoption arguments
		try {
			new Options2(new String[] {"--maxRecords","2","--maxRecords","3","--maxRecords","4","--maxRecords","5"});
			fail("Should have thrown an exception");
		} catch (Exception e) {}
		
		// empty options
		try {
			new Options2(new String[] {"--maxRecords"});
			fail("Should have thrown an exception");
		} catch (Exception e) {}
	}
	
	@Test
	public void testNoOfThreadsOptions() throws Exception {
		Options2 options = new Options2(new String[] {"--ntC","2"});
		assertEquals(0, options.getFileNames().length);
		assertEquals(0, options.getMaxRecords());
		assertEquals(2, options.getNoOfConsumerThreads());
		
		// shortcut
		options = new Options2(new String[] {"--ntC","10"});
		assertEquals(10, options.getNoOfConsumerThreads());
		// shortcut again
		options = new Options2(new String[] {"-ntP","11"});
		assertEquals(11, options.getNoOfProducerThreads());
		
		// invalid option (needs to be integer)
		try {
			new Options2(new String[] {"-ntP","matrices"});
			fail("Should have thrown an exception");
		} catch (Exception e) {}
		
		// more than 1 value, will throw an exception as they are considered to be nonoption arguments
		try {
			new Options2(new String[] {"--ntC","2","3","4","5"});
			fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {}
		
		// multiple values - will throw an exception
		try {
			new Options2(new String[] {"-ntP","2","-ntC","3","-ntC","4","-ntP","5"});
			fail("Should have thrown an exception");
		} catch (Exception e) {}
		
		// empty options
		try {
			new Options2(new String[] {"--ntC"});
			fail("Should have thrown an exception");
		} catch (Exception e) {}
	}
}
