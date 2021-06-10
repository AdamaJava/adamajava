package org.qcmg.qprofiler2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.qcmg.qprofiler2.Options;

public class OptionsTest {
	public static final String FILE_1 = "file1.xml"; 
	public static final String FILE_2 = "file2.xml"; 
	public static final String FILE_3 = "file3.xml"; 
	public static final String FILE_4 = "file4.xml"; 

	@Test
	public void testNullOptions() throws Exception {
		Options options = new Options(new String[] {});
		assertEquals(0, options.getFileNames().length);
		assertEquals(0, options.getMaxRecords());
		assertEquals(0, options.getNoOfConsumerThreads());
	}
	
	@Test
	public void testFileNamesOnlyOptions() throws Exception {
		Options options = new Options(new String[] {"-input", FILE_1});
		assertEquals(1, options.getFileNames().length);
		assertEquals(FILE_1, options.getFileNames()[0]);
		assertEquals(0, options.getMaxRecords());
		assertEquals(0, options.getNoOfConsumerThreads());
		
		// and with multiple files
		options = new Options(new String[] {"-inp", FILE_1, "-inp", FILE_2, "--input", FILE_3, "--input", FILE_4});
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
		Options options = new Options(new String[] {"--bam-records","2"});
		assertEquals(0, options.getFileNames().length);
		assertEquals(2, options.getMaxRecords());
		assertEquals(0, options.getNoOfConsumerThreads());
		
		// shortcut
		options = new Options(new String[] {"--bam-r","10"});
		assertEquals(10, options.getMaxRecords());
		
		// invalid option (needs to be integer)
		try {
			new Options(new String[] {"-bam-rec","matrices"});
			fail("Should have thrown an exception");
		} catch (Exception e) {}
		
		// more than 1 value, will throw an exception as they are considered to be nonoption arguments
		try {
			new Options(new String[] {"--bam-records","2","3","4","5"});
			fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {}
		
		// more than 1 value, will throw an exception as they are considered to be nonoption arguments
		try {
			new Options(new String[] {"--bam-records","2","--bam-records","3","--bam-records","4","--bam-records","5"});
			fail("Should have thrown an exception");
		} catch (Exception e) {}
		
		// empty options
		try {
			new Options(new String[] {"--bam-records"});
			fail("Should have thrown an exception");
		} catch (Exception e) {}
	}
	
	@Test
	public void testNoOfThreadsOptions() throws Exception {
		Options options = new Options(new String[] {"--threads-consumer","2"});
		assertEquals(0, options.getFileNames().length);
		assertEquals(0, options.getMaxRecords());
		assertEquals(2, options.getNoOfConsumerThreads());
		
		// shortcut
		options = new Options(new String[] {"--threads-consumer","10"});
		assertEquals(10, options.getNoOfConsumerThreads());
		// shortcut again
		options = new Options(new String[] {"--bam-threads-producer","11"});
		assertEquals(11, options.getNoOfProducerThreads());
		
		// invalid option (needs to be integer)
		try {
			new Options(new String[] {"--bam-threads-producer","matrices"});
			fail("Should have thrown an exception");
		} catch (Exception e) {
			assertTrue(true);
		}
		
		// more than 1 value, will throw an exception as they are considered to be nonoption arguments
		try {
			new Options(new String[] {"--bam-threads-producer","2","3","4","5"});
			fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {
			assertTrue(true);
		}
		
		// multiple values - will throw an exception
		try {
			new Options(new String[] {"-bam-threads-producer","2","-threads-consumer","3","-threads-consumer","4","-ntP","5"});
			fail("Should have thrown an exception");
		} catch (Exception e) {
			assertTrue(true);
		}
		
		// empty options
		try {
			new Options(new String[] {"--tc"});
			fail("Should have thrown an exception");
		} catch (Exception e) {
			assertTrue(true);
		}
	}
	
	

}
