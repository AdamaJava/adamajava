package org.qcmg.pileup;

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
        assertTrue(testOut.toString().startsWith("usage1"));
       cleanUpStreams();
    }
	
	@Test
    public void testQPileupWithViewOptionHeader() throws Exception {        
		String[] args = TestUtil.getViewArgs(testFolder, hdf, "chr1:12000-12300", true);
		String output = runPileup(args);
		String[] linesOfOutput = output.split(System.getProperty("line.separator"));
		assertEquals(12, linesOfOutput.length);
		assertTrue(linesOfOutput[0].startsWith("## DATE"));		
    }
	
	@Test
    public void testQPileupWithViewOption() throws Exception {
		
        String[] args = TestUtil.getViewArgs(testFolder, hdf, "chr1:12000-12300", false);
		String output = runPileup(args);
		String[] linesOfOutput = output.split(System.getProperty("line.separator"));
        assertTrue(linesOfOutput[0].startsWith("## DATE"));
        assertEquals(315, linesOfOutput.length);
    }

	@Test
    public void testQPileupWithHelpMessage() throws Exception {
        setUpStreams();
        String[] args = {"--help" };
        QPileup pileup = new QPileup();
        int exit = pileup.runPileup(args, System.currentTimeMillis());
        assertEquals(0, exit);
        assertTrue(testOut.toString().startsWith("usage"));
        cleanUpStreams();
    }

    @Test
    public void testQPileupWithVersionMessage() throws Exception {
    	String[] args = {"--version" };   	 
        setUpStreams();        
        QPileup pileup = new QPileup();
        int exit = pileup.runPileup(args, System.currentTimeMillis());
        assertEquals(0, exit);
        assertTrue(testOut.toString().contains("version"));
        cleanUpStreams();
    }
    
	@Test
    public void testQPileupWithViewOptionVersion() throws Exception {
		String[] args = new String[] {"--view","--hdf-version", "--hdf", hdf};
		String output = runPileup(args);
		String[] linesOfOutput = output.split(System.getProperty("line.separator"));
        assertEquals(2, linesOfOutput.length);
        assertTrue(linesOfOutput[0].startsWith("## VERSION_BOOTSTRAP"));
    }  
	
	private String runPileup(String[] args) throws IOException {
		PrintStream defaultOutstream = System.out;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        QPileup pileup = new QPileup();
        int exit = pileup.runPileup(args, System.currentTimeMillis());
        baos.flush();
        assertEquals(0, exit);
        String output = baos.toString();
        System.setOut(defaultOutstream);
        return output;        
	}
	

}
