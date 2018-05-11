package org.qcmg.qvisualise2;

import junit.framework.Assert;

import org.junit.Test;
import org.qcmg.qvisualise2.Options;

public class OptionsTest {
	public static final String INPUT_FILE_1 = "file1.xml";
	public static final String INPUT_FILE_2 = "file2.xml";
	public static final String OUTPUT_FILE_1 = "file1.xml.html";

	@Test
	public void testNullOptions() throws Exception {
		Options options = new Options(new String[] {});
		Assert.assertNull( options.getInputFile());
		Assert.assertNull( options.getOutputFile());
	}
	
	@Test
	public void testFileNamesWithNoOptions() throws Exception {
		Options options = new Options(new String[] {INPUT_FILE_1});
		Assert.assertNull(options.getInputFile());
		Assert.assertNull(options.getOutputFile());
		
		options = new Options(new String[] {INPUT_FILE_1, OUTPUT_FILE_1});
		Assert.assertNull(options.getInputFile());
		Assert.assertNull(options.getOutputFile());
		
		options = new Options(new String[] {OUTPUT_FILE_1});
		Assert.assertNull(options.getInputFile());
		Assert.assertNull(options.getOutputFile());
	}
	
	@Test
	public void testOptionsWithNoFileNames() throws Exception {
		try {
			new Options(new String[] {"--input"});
			Assert.fail("Should not have got this far");
		} catch (Exception e) {}
		
		try {
			new Options(new String[] {"--output"});
			Assert.fail("Should not have got this far");
		} catch (Exception e) {}
		
		try {
			new Options(new String[] {"-inp", INPUT_FILE_1, "--output"});
			Assert.fail("Should not have got this far");
		} catch (Exception e) {}
	}
	
	@Test
	public void testOptionsWithMultipleFileNames() throws Exception {
		
		Options options = new Options(new String[] {"--input", INPUT_FILE_1, INPUT_FILE_2});
		Assert.assertEquals(INPUT_FILE_1, options.getInputFile());
		Assert.assertFalse(INPUT_FILE_2.equals(options.getInputFile()));
		
		options = new Options(new String[] {"--input", INPUT_FILE_2, INPUT_FILE_1});
		Assert.assertEquals(INPUT_FILE_2, options.getInputFile());
		Assert.assertFalse(INPUT_FILE_1.equals(options.getInputFile()));
	}
	
	@Test
	public void testFileNamesWithOptions() throws Exception {
		
		Options options = new Options(new String[] {"--input", INPUT_FILE_1});
		Assert.assertEquals(INPUT_FILE_1, options.getInputFile());
		
		options = new Options(new String[] {"--inp", INPUT_FILE_1});
		Assert.assertEquals(INPUT_FILE_1, options.getInputFile());
		
		options = new Options(new String[] {"-i", INPUT_FILE_1});
		Assert.assertEquals(INPUT_FILE_1, options.getInputFile());
		
		options = new Options(new String[] {"-i", INPUT_FILE_1, "--output", OUTPUT_FILE_1});
		Assert.assertEquals(INPUT_FILE_1, options.getInputFile());
		Assert.assertEquals(OUTPUT_FILE_1, options.getOutputFile());
		
		options = new Options(new String[] {"-in", INPUT_FILE_1, "-out", OUTPUT_FILE_1});
		Assert.assertEquals(INPUT_FILE_1, options.getInputFile());
		Assert.assertEquals(OUTPUT_FILE_1, options.getOutputFile());
		
	}
}
