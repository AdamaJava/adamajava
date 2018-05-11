package org.qcmg.qvisualise2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.ResourceBundle;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.messages.QMessage;
import org.qcmg.qvisualise2.QVisualise;


public class QVisualiseTest {
	
	private static final String TEST_XML = "test.xml";
	private static final String TEST_HTML = "test.html";
	private static final String TEST_TEST = "test.test";
	private static final String LOG_FILE = "QVisualiseTest.log";
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public final void executeWithTooManyArgs() throws Exception {
		String[] args = {"-input","file1.xml", "-output","file2.xml", "-out","file3.xml"};
		try {
			QVisualise qv = new QVisualise();
			qv.setup(args);
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {}
		
		String[] args2 = {"-input","file1.xml", "-i","ifile2.xml","-output","file2.xml", "-out","file3.xml"};
		try {
			QVisualise qv = new QVisualise();
			qv.setup(args2);
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {}
	}
	
	@Ignore
	public final void executeWithNoArgs() throws Exception {
		String[] args = {};
		try {
			QVisualise qv = new QVisualise();
			int exitStatus = qv.setup(args);
			Assert.assertEquals(1, exitStatus);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("no exception should have been thrown from executeWithNoArgs()");
		}
	}
	
	@Ignore
	public final void executeWithValidFileType() throws Exception {
		File inputFile = testFolder.newFile(TEST_XML);
		File logFile = testFolder.newFile(LOG_FILE);
		
		// fail due to missing logFile option 
		String[] args = {"-input",inputFile.getAbsolutePath()};
		try {
			int exitStatus = new QVisualise().setup(args);
			Assert.assertEquals(1, exitStatus);
		} catch (Exception qpe) {
			Assert.fail("Should NOT have thrown a QVisualiseException");
		}
		
		//fail due to missing content in input file
		createSkeletonProfilerFile(inputFile);
		String[] args2 = {"-input",inputFile.getAbsolutePath(), "-log",logFile.getAbsolutePath()};
		try {
			int exitStatus = new QVisualise().setup(args2);
			Assert.assertEquals(0, exitStatus);
		} catch (Exception qpe) {
			Assert.fail("Should NOT have thrown a QVisualiseException");
		}
	}
	
	@Ignore
	public final void executeWithValidFileType2() throws Exception {
		String[] args = {"-input",TEST_XML, "-output",TEST_HTML};
		
		// will fail due to missing log file
		try {
			int exitStatus = new QVisualise().setup(args);
			Assert.assertTrue(1 == exitStatus);
		} catch (Exception qpe) {
			Assert.fail("Should have thrown a QVisualiseException");
		}
		
		File inputFile = testFolder.newFile(TEST_XML);
		File outputFile = testFolder.newFile(TEST_HTML);
		File logFile = testFolder.newFile(LOG_FILE);
		
		String[] args2 = {"-input",inputFile.getAbsolutePath(), "-output",outputFile.getAbsolutePath(), "-log", logFile.getAbsolutePath()};
		
		// will fail due to empty file
		try {
			new QVisualise().setup(args2);
			Assert.fail("Should have thrown a QVisualiseException");
		} catch ( Exception qpe) {
			Assert.assertEquals(true, qpe.getMessage().startsWith("Empty input file"));
		}
		
		createSkeletonProfilerFile(inputFile);
		// will succeed
		try {
			int exitStatus = new QVisualise().setup(args2);
			Assert.assertTrue(0 == exitStatus);
		} catch ( Exception qpe) {
			Assert.fail("Should not have thrown a QVisualiseException");
		}
	}
	
	@Test
	public final void executeWithInvalidInputFileType() throws Exception {
		File logFile = testFolder.newFile("executeWithInvalidFileType.log");
		String[] args = {"-input",TEST_TEST, "-log",logFile.getAbsolutePath()};
		try {
			new QVisualise().setup(args);
			Assert.fail("Should have thrown a QVisualiseException");
		} catch ( Exception qpe) {
			Assert.assertTrue(qpe.getMessage().startsWith("Unsupported file type"));
		}
	}
	
	@Test
	public final void executeWithInvalidOutputFileType() throws Exception {
		File logFile = testFolder.newFile("executeWithInvalidFileType.log");
		String[] args = {"-input",TEST_XML, "-output",TEST_XML, "-log", logFile.getAbsolutePath()};
		try {
			new QVisualise().setup(args);
			Assert.fail("Should have thrown a QVisualiseException");
		} catch ( Exception qpe) {
			Assert.assertTrue(qpe.getMessage().startsWith("Unsupported file type"));
		}
	}
	
	@Test
	public final void executeWithOptions() throws Exception {
		ByteArrayOutputStream errContent = new ByteArrayOutputStream();
		System.setErr(new PrintStream(errContent));
		
		String[] args = {"-h"};
		new QVisualise().setup(args);
		Assert.assertTrue(errContent.toString().contains(   new QMessage(null, ResourceBundle.getBundle("org.qcmg.qvisualise2.messages") ).getUsage()   ));
		
		errContent.reset();
		
		args[0] = "-v";
		new QVisualise().setup(args);
		Assert.assertTrue(errContent.toString().contains("null, version null"));
		
		System.setErr(null);
	}
	
	
	
	private void createSkeletonProfilerFile(File file) throws IOException {
		FileWriter writer = new FileWriter(file);
		try {
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
			writer.write("<!DOCTYPE qProfiler>");
			writer.write("<qProfiler finish_time=\"2010-10-18 14:09:13\" run_by_os=\"Linux\" run_by_user=\"uqoholme\" start_time=\"2010-10-18 13:47:48\" version=\"0.3pre (2243)\">");
			writer.write("<MAReport execution_finished=\"2010-10-18 14:09:13\" execution_started=\"2010-10-18 13:47:48\" file=\"/panfs/imb/seq_mapped/S0417_20100922_1_FragPEBC/S16_04/20101014/F3/s_mapping/S0417_20100922_1_FragPEBC_bcSample1_F3_S16_04.csfasta.ma\" records_parsed=\"69858826\">");
			writer.write("</MAReport>");
			writer.write("</qProfiler>");
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			writer.close();
		}
	}

}
