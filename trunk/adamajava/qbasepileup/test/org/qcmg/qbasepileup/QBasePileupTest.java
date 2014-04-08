package org.qcmg.qbasepileup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


public class QBasePileupTest {
	
	private PrintStream defaultOutstream = null;
    private PrintStream testOutPrintStream = null;
    private ByteArrayOutputStream testOut = null;
    final static String FILE_SEPERATOR = System.getProperty("file.separator");
    
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
    public void testQSVWithHelpMessage() throws Exception {
        setUpStreams();
        String[] args = {"--help" };
        QBasePileup basePileup = new QBasePileup();
        int exit = basePileup.runBasePileup(args);
        assertEquals(0, exit);
        assertTrue(testOut.toString().startsWith("Option"));
        cleanUpStreams();
    }

    @Test
    public void testQSVWithVersionMessage() throws Exception {
        setUpStreams();
        String[] args = {"--version" };
        QBasePileup basePileup = new QBasePileup();
        int exit = basePileup.runBasePileup(args);
        assertEquals(0, exit);
        assertTrue(testOut.toString().startsWith("null"));
        cleanUpStreams();
    }
    
    @Test
    public void testRunBasePileup() throws Exception {
    	String log =  testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "test.log";
    	String input = testFolder.newFile("input.bam").getAbsolutePath();
    	String reference = testFolder.newFile("reference.fa").getAbsolutePath();
		testFolder.newFile("reference.fa.fai").getAbsolutePath();
		String snps = testFolder.newFile("snps.dcc").getAbsolutePath();
		String output = testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "pileup.txt";
        assertFalse(new File(log).exists());
        assertFalse(new File(output).exists());
        String[] args = {"--log", log, "-i", input, "-r", reference, "-o", output, "-s", snps};
        QBasePileup basePileup = new QBasePileup();
        int exit = basePileup.runBasePileup(args);
        assertEquals(0, exit);
        assertTrue(new File(log).exists());
        assertTrue(new File(output).exists());
    }
}
