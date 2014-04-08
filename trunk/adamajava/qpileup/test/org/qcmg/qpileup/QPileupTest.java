package org.qcmg.qpileup;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;


import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.pileup.PileupConstants;
import org.qcmg.pileup.QPileup;
import org.qcmg.pileup.util.TestUtil;


public class QPileupTest {
	
	private String reference = getClass().getResource("/resources/test-reference.fa").getFile();
	private String bam = getClass().getResource("/resources/test.bam").getFile();
	private String hdf = getClass().getResource("/resources/test.h5").getFile();
    private PrintStream defaultOutstream = null;
    private PrintStream testOutPrintStream = null;
    private ByteArrayOutputStream testOut = null;
    

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
	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void testQPileup() throws Exception  {
		hdf = testFolder.getRoot().getAbsolutePath() + PileupConstants.FILE_SEPARATOR + "test.h5";
		String[] args = TestUtil.getArgs(testFolder, "bootstrap", reference, hdf, bam, testFolder.getRoot().toString(), "all", "");
		
		QPileup pileup = new QPileup();
		int exit = pileup.runPileup(args, System.currentTimeMillis());
		
		assertEquals(0, exit);
		assertTrue(new File(hdf).exists());		
	}

	@Test
    public void testQPileupWithNoArguments() throws Exception {
        setUpStreams();
        QPileup pileup = new QPileup();
        int exit = pileup.runPileup(new String[0], System.currentTimeMillis());
        assertEquals(0, exit);
        String usage = "usage: qpileup [-options] --ini [inifile_options.ini]\n";
        assertEquals(usage, testOut.toString());
        cleanUpStreams();
    }
	
	@Test
    public void testQPileupWithViewOptionHeader() throws Exception {
		PrintStream defaultOutstream = System.out;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        QPileup pileup = new QPileup();
        String[] args = TestUtil.getViewArgs(testFolder, hdf, "chr1:12000-12300", true);
		int exit = pileup.runPileup(args, 1234);
        baos.flush();
        assertEquals(0, exit);
        String output = baos.toString();
        System.setOut(defaultOutstream);
        
        String[] linesOfOutput = output.split(System.getProperty("line.separator"));
//        for (String s: linesOfOutput) {
//        	System.out.println(s);
//        }
        assertEquals(12, linesOfOutput.length);
        assertTrue(linesOfOutput[0].startsWith("## DATE"));
    }
	
	@Test
    public void testQPileupWithViewOption() throws Exception {
		PrintStream defaultOutstream = System.out;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        QPileup pileup = new QPileup();
        String[] args = TestUtil.getViewArgs(testFolder, hdf, "chr1:12000-12300", false);
		int exit = pileup.runPileup(args, 1234);
        baos.flush();
        assertEquals(0, exit);
        String output = baos.toString();
        System.setOut(defaultOutstream);
        String[] linesOfOutput = output.split(System.getProperty("line.separator")); 
        assertEquals(315, linesOfOutput.length);
        assertTrue(linesOfOutput[0].startsWith("## DATE"));
    }

	@Test
    public void testQPileupWithHelpMessage() throws Exception {
        setUpStreams();
        String[] args = {"--help" };
        QPileup pileup = new QPileup();
        int exit = pileup.runPileup(args, System.currentTimeMillis());
        assertEquals(0, exit);
        assertTrue(testOut.toString().startsWith("Option"));
        cleanUpStreams();
    }

    @Test
    public void testQPileupWithVersionMessage() throws Exception {
        setUpStreams();
        String[] args = {"--version" };
        QPileup pileup = new QPileup();
        int exit = pileup.runPileup(args, System.currentTimeMillis());
        assertEquals(0, exit);
        assertTrue(testOut.toString().contains("version"));
        cleanUpStreams();
    }
	

}
