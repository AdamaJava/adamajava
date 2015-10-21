package org.qcmg.qsv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import net.sf.samtools.SAMFileHeader.SortOrder;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.discordantpair.PairGroup;
import org.qcmg.qsv.util.TestUtil;

public class QSVTest {

    private static File normalBam;
    private static File tumorBam;
    private static File filteredNormalBam;
    private static File filteredTumorBam;
    private PrintStream defaultOutstream = null;
    private PrintStream testOutPrintStream = null;
    private ByteArrayOutputStream testOut = null;

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
    
    @Before
    public void setUp() throws IOException {
    	normalBam = TestUtil.createBamFile(testFolder.newFile("normalBam.bam").getAbsolutePath(), PairGroup.AAC, SortOrder.coordinate);
    	tumorBam = TestUtil.createBamFile(testFolder.newFile("tumorBam.bam").getAbsolutePath(), PairGroup.AAC, SortOrder.coordinate);
		filteredNormalBam = TestUtil.createBamFile(testFolder.newFile("normalBam_filtered.bam").getAbsolutePath(), PairGroup.AAC, SortOrder.queryname);
		filteredTumorBam = TestUtil.createBamFile(testFolder.newFile("normalBam_filtered.bam").getAbsolutePath(), PairGroup.AAC, SortOrder.queryname);
    }
    
    @After
    public void tearDown() throws IOException {    	
    	normalBam = null;
    	tumorBam = null;
		filteredNormalBam = null;
		filteredTumorBam = null;
    }

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

    @Ignore
    public void testQSVWithModePairBoth() throws Exception {
        String[] args = TestUtil.getValidOptions(testFolder, normalBam.getAbsolutePath(), tumorBam.getAbsolutePath(), "pair","pair");
        QSV qsv = new QSV();
        int exitStatus = qsv.runQSV(args);
        assertEquals(0, exitStatus);
        File results = new File(qsv.getResultsDirectory());
        assertTrue(results.exists());

        List<String> filesCreated = Arrays.asList(testFolder.getRoot().list());
        	
        assertTrue(filesCreated.contains("normalBam.discordantpair.filtered.bam"));
        assertTrue(filesCreated.contains("tumorBam.discordantpair.filtered.bam"));
        
        List<String> filesCreated2 = Arrays.asList(results.list());
        assertTrue(filesCreated2.contains("test.ND.pairing_stats.xml"));
        assertTrue(filesCreated2.contains("test.TD.pairing_stats.xml"));
        
        //make sure exception is thrown if results directory can be created. 
        try {
        testFolder.delete();
        qsv.createResultsDirectory();
        } catch (QSVException e) {
            assertFalse(new File(qsv.getResultsDirectory()).exists());
        }       
    }

}
