package org.qcmg.qsv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.util.QSVUtil;
import org.qcmg.qsv.util.TestUtil;

public class QSVTest {

    private  File normalBam;
    private  File tumorBam;
    private PrintStream defaultOutstream = null;
    private PrintStream testOutPrintStream = null;
    private ByteArrayOutputStream testOut = null;

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
    
    public void setUpStreams() {
        defaultOutstream = System.err;
        testOut = new ByteArrayOutputStream();
        testOutPrintStream = new PrintStream(testOut);
        System.setErr(testOutPrintStream);
    }

    public void cleanUpStreams() throws IOException {
        System.setErr(defaultOutstream);
        testOutPrintStream.close();
        testOut.close();
        testOut = null;
        testOutPrintStream = null;
        defaultOutstream = null;
    }

    @Test
    public void testQSVWithInvalidArguments() throws Exception {
        setUpStreams();
        String[] args = {"-log", "qsv.log", "--loglevel", "DEBUG", "iniFile" };
        QSV qsv = new QSV();

        int exitStatus = qsv.runQSV(args);
        assertEquals(1, exitStatus);
        assertTrue(testOut.toString().contains("org.qcmg.qsv.QSVException"));
        cleanUpStreams();
    }
    
    @Test
    public void getResultsDir() {
	    	try {
	    		QSV.getResultsDirectory(null,  null,  null);
	    		Assert.fail("Should have thrownan IllegalArgumentException");
	    	} catch (IllegalArgumentException iae) {}
	    	
	    	assertEquals("ABC/", QSV.getResultsDirectory("ABC",  null,  null));
	    	assertEquals("ABC/", QSV.getResultsDirectory("ABC",  "DEF",  null));
	    	assertEquals("ABC/", QSV.getResultsDirectory("ABC",  "DEF",  "XYZ"));
	    	assertEquals("ABC/", QSV.getResultsDirectory("ABC/",  null,  null));
	    	assertEquals("ABC/", QSV.getResultsDirectory("ABC/",  "DEF",  null));
	    	assertEquals("ABC/", QSV.getResultsDirectory("ABC/",  "DEF",  "XYZ"));
	    	assertEquals("DEF/XYZ/", QSV.getResultsDirectory(null,  "DEF",  "XYZ"));
	    	
	    	try {
	    		QSV.getResultsDirectory(null,  "blah",  null);
	    		Assert.fail("Should have thrownan IllegalArgumentException");
	    	} catch (IllegalArgumentException iae) {}
	    	try {
	    		QSV.getResultsDirectory(null,  null, "blah");
	    		Assert.fail("Should have thrownan IllegalArgumentException");
	    	} catch (IllegalArgumentException iae) {}
    }
    
    @Test
    public void getUUIDFromOverrideOption() {
    		Date now = new Date();
    		String analysisId = QSV.getAnalysisId(false, null, "sample", now);
    		assertEquals(QSVUtil.getAnalysisId(false, "sample", now), analysisId);
    		
    		
    		analysisId = QSV.getAnalysisId(false, "non_null_override_option", "sample", now);
    		assertEquals(QSVUtil.getAnalysisId(false, "sample", now), analysisId);
    		
    		analysisId = QSV.getAnalysisId(true, null, "sample", now);
    		assertEquals(UUID.fromString(analysisId).toString(), analysisId);
    		
    		try {
    			analysisId = QSV.getAnalysisId(true, "non_null_override_option", "sample", now);
    			Assert.fail("Should have thrown an IAE");
    		} catch (IllegalArgumentException aie){}
    		
    		UUID uuid = UUID.randomUUID();
    		
    		analysisId = QSV.getAnalysisId(true, uuid.toString(), "sample", now);
    		assertEquals(uuid.toString(), analysisId);
    		
    }
    
    @Test
    public void testQSVWithNoArguments() throws Exception {
        setUpStreams();
        QSV qsv = new QSV();
        int exit = qsv.runQSV(new String[0]);
        assertEquals(0, exit);
        assertEquals(
                "usage: qsv [OPTIONS] --ini [ini_file] --tmp [temporary_directory]\n",
                testOut.toString());
        cleanUpStreams();
    }

    @Test
    public void testQSVWithHelpMessage() throws Exception {
        setUpStreams();
        String[] args = {"--help" };
        QSV qsv = new QSV();
        int exit = qsv.runQSV(args);
        assertEquals(0, exit);
        assertTrue(testOut.toString().startsWith("Option"));
        cleanUpStreams();
    }

    @Test
    public void testQSVWithVersionMessage() throws Exception {
        setUpStreams();
        String[] args = {"--version" };
        QSV qsv = new QSV();
        int exit = qsv.runQSV(args);
        assertEquals(0, exit);
        assertTrue(testOut.toString().startsWith("null"));
        cleanUpStreams();
    }

    @Test
    public void testQSVNoLogging() throws Exception {
        setUpStreams();
        String[] args = {"-loglevel", "DEBUG"};
        QSV qsv = new QSV();

        int exit = qsv.runQSV(args);
        assertEquals(1, exit);
        assertTrue(testOut.toString().contains("org.qcmg.qsv.QSVException"));
        cleanUpStreams();
    }

}
