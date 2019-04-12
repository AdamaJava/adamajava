package org.qcmg.qsv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import junit.framework.Assert;

import org.ini4j.InvalidFileFormatException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.util.TestUtil;

public class OptionsTest {

	private String file1;
	private String file2;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    
    @Before
    public void setUp() throws IOException {
    	file1 = testFolder.newFile("test1.bam").getAbsolutePath();
        file2 = testFolder.newFile("test2.bam").getAbsolutePath();
    }
    
    @After
    public void tearDown() {
    	if (new File(file1).exists()) {
    		new File(file1).delete();
    	}
    	if (new File(file2).exists()) {
    		new File(file2).delete();
    	}
    }


    @Test
    public void testGoodArgumentsFound() throws Exception {
    	
        String[] args = TestUtil.getValidOptions(testFolder, file1, file2, "both", "both");
        Options options = new Options(args);
        assertFalse(options.hasHelpOption());
        assertFalse(options.hasVersionOption());
        assertEquals(options.getIniFile(), testFolder.getRoot().getAbsolutePath() + "/test.ini");
        assertEquals(options.getTempDirName(), testFolder.getRoot().getAbsolutePath());
    }
    @Test
    public void bgiPlatform() throws Exception {
    	
    	String[] args = TestUtil.getValidOptions(testFolder, file1, file2, "both", "both", true, "bwamem", "MGISeq2000");
    	Options options = new Options(args);
    	assertFalse(options.hasHelpOption());
    	assertFalse(options.hasVersionOption());
    	assertEquals(options.getIniFile(), testFolder.getRoot().getAbsolutePath() + "/test.ini");
    	assertEquals(options.getTempDirName(), testFolder.getRoot().getAbsolutePath());
    	options.parseIniFile();
        options.detectBadOptions();
    	assertEquals("bgi", options.getPlatform());
    	assertEquals("MGISeq2000", options.getSequencingPlatform());
    }
    @Test
    public void bgiPlatformAgain() throws Exception {
    	
    	String[] args = TestUtil.getValidOptions(testFolder, file1, file2, "both", "both", true, "bwamem", "BGISeq500");
    	Options options = new Options(args);
    	assertFalse(options.hasHelpOption());
    	assertFalse(options.hasVersionOption());
    	assertEquals(options.getIniFile(), testFolder.getRoot().getAbsolutePath() + "/test.ini");
    	assertEquals(options.getTempDirName(), testFolder.getRoot().getAbsolutePath());
    	options.parseIniFile();
    	options.detectBadOptions();
    	assertEquals("bgi", options.getPlatform());
    	assertEquals("BGISeq500", options.getSequencingPlatform());
    }
    
    @Test
    public void testGoodIniFileOptions() throws Exception {
    	
        String[] args = TestUtil.getValidOptions(testFolder, file1, file2, "both", "both");
        Options options = new Options(args);
        options.parseIniFile();
        options.detectBadOptions();
        assertEquals(options.getLog(), "test.log");
        assertEquals(options.getLogLevel(), "DEBUG");
        assertEquals(options.getSampleName(), "test");
        assertEquals(options.getOutputDirName(), testFolder.getRoot().toString());
        assertTrue(options.getReference().contains("reference_file"));
        assertEquals(options.getPreprocessMode(), "both");
        assertEquals(options.getAnalysisMode(), "both");        
        assertEquals(options.getMaxISizeCount(), "all");
        assertEquals(options.getMinInsertSize(), new Integer(50));
        assertTrue(options.isQCMG());
        assertEquals(options.getPairingType(), "lmp");
        assertEquals(options.getPairQuery(), "and(Cigar_M > 35,option_SM > 14,MD_mismatch < 3,Flag_DuplicateRead == false)");
        assertEquals(options.getClusterSize().intValue(), 3);
        assertEquals(options.getFilterSize().intValue(), 1);
        assertEquals(options.getQPrimerThreshold().intValue(), 3);
        assertEquals(options.getMapper(), "bioscope");
        assertEquals(options.getClipQuery(), "and(Cigar_M > 35,option_SM > 14,MD_mismatch < 3,Flag_DuplicateRead == false)");
        assertEquals(options.getClipSize().intValue(), 3);
        assertEquals(options.getConsensusLength(), new Integer(20));
        assertEquals(options.getBlatPath(), "/home/Software/BLAT");
        assertEquals(options.getBlatServer(), "localhost");
        assertEquals(options.getBlatPort(), "50000");
        
        //tumor
        assertEquals(options.getInputFile(), file2);
        assertEquals(options.getInputFileAbbreviation(), "TD");
        assertEquals(options.getInputSampleId(), "ICGC-DBLG-20110506-01-TD");
        //normal
        assertEquals(options.getComparisonFile(), file1);
        assertEquals(options.getComparisonFileAbbreviation(), "ND");
        assertEquals(options.getComparisonSampleId(), "ICGC-DBLG-20110506-01-ND");
    }

    @Test
    public void testHelpOption() throws QSVException, InvalidFileFormatException, IOException {
        Options options = new Options(new String[] {"--help"});
        assertTrue(options.hasHelpOption());

        Options options2 = new Options(new String[] {"-h"});
        assertTrue(options2.hasHelpOption());
    }

    @Test
    public void testVersionOption() throws QSVException, InvalidFileFormatException, IOException {
        Options options = new Options(new String[] {"--version"});
        assertTrue(options.hasVersionOption());

        Options options2 = new Options(new String[] {"-v"});
        assertTrue(options2.hasVersionOption());

        Options options3 = new Options(new String[] {"-V"});
        assertTrue(options3.hasVersionOption());
    }
    
    @Test(expected = QSVException.class)
    public void testBadOptionsNoLogFile() throws QSVException, InvalidFileFormatException, IOException {
        Options options = new Options(new String[] {"--version"});
        options.detectBadOptions();
    }
    
    @Test(expected = QSVException.class)
    public void testBadOptionsBadModes() throws QSVException, InvalidFileFormatException, IOException {
        Options options = new Options(new String[] {"--version"});
        options.setLogFile("log");
        options.setPreprocessMode("pair");
        options.setAnalysisMode("clip");
        options.detectBadOptions();
    }
    
    @Test(expected = QSVException.class)
    public void testBadOptionsBadModes2() throws QSVException, InvalidFileFormatException, IOException {
        Options options = new Options(new String[] {"--version"});
        options.setLogFile("log");
        options.setPreprocessMode("clip");
        options.setAnalysisMode("pair");
        options.detectBadOptions();
    }
    
    @Test(expected = QSVException.class)
    public void testBadOptionsNoPairType() throws QSVException, InvalidFileFormatException, IOException {
    	String[] args = TestUtil.getValidOptions(testFolder, file1, file2, "pair", "pair");
    	Options options = new Options(args);
    	options.parseIniFile();
    	options.setPairingType("te");
        options.detectBadOptions();
    }
    
    @Test(expected = QSVException.class)
    public void testBadOptionsNoMapper() throws QSVException, InvalidFileFormatException, IOException {
    	String[] args = TestUtil.getValidOptions(testFolder, file1, file2, "pair", "pair");
    	Options options = new Options(args);
    	options.parseIniFile();
    	options.setPairingType("pe");
    	options.setMapper("no");
        options.detectBadOptions();
    }
    
    @Test(expected = QSVException.class)
    public void testBadOptionsNoTmpDir() throws QSVException, InvalidFileFormatException, IOException {
    	String[] args = TestUtil.getValidOptions(testFolder, file1, file2, "both", "both");
    	Options options = new Options(args);
    	options.parseIniFile();
        options.setTmpDir("/test");
        options.detectBadOptions();
    }
    
   
    @Test(expected = QSVException.class)
    public void testBadOptionsBadAnalysisMode() throws QSVException, InvalidFileFormatException, IOException {
        Options options = new Options(new String[] {"--version"});
        options.setLogFile("log");
        options.setPreprocessMode("none");
        options.setAnalysisMode("bad");
        options.detectBadOptions();
    }
 
    @Test(expected = QSVException.class)
    public void testBadOptionsNonExistentInputFile() throws Exception {
    	 Options options = new Options(TestUtil.getInvalidOptions(testFolder, file1, "acd", "both", 5, 0, testFolder.getRoot().toString(), "acd"));
         options.parseIniFile();
         Assert.assertEquals("acd", options.getInputFile());
//         options.detectBadOptions();
    }
    
    @Test(expected = QSVException.class)
    public void testBadOptionsNonExistentInputComparisonFile() throws Exception {
    	 Options options = new Options(TestUtil.getInvalidOptions(testFolder, "acd", file2, "both", 5, 0, testFolder.getRoot().toString(), "acd"));
         options.parseIniFile();
         Assert.assertEquals("acd", options.getComparisonFile());
//         options.detectBadOptions();
    }

    @Test(expected = QSVException.class)
    public void testBadOptionsNonexistentOutputDir() throws Exception {
    	 Options options = new Options(TestUtil.getInvalidOptions(testFolder, file1, file2, "both", 5, 0, testFolder.getRoot().toString(), "acd"));
         options.parseIniFile();
         Assert.assertEquals("acd", options.getOutputDirName());
//         options.detectBadOptions();
    }
    
    @Test(expected = QSVException.class)
    public void testBadOptionsNoClusterSize() throws Exception {
    	Options options = new Options(TestUtil.getInvalidOptions(testFolder, file1, file2, "both", 5, 0, testFolder.getRoot().toString(), testFolder.getRoot().toString()));
    	options.parseIniFile();
        Assert.assertEquals(null, options.getClusterSize());
//        options.detectBadOptions();
    }
    
    @Test(expected = QSVException.class)
    public void testBadOptionsWrongClusterSize() throws Exception {
    	Options options = new Options(TestUtil.getInvalidOptions(testFolder, file1, file2, "both", 0, 2,testFolder.getRoot().toString(), testFolder.getRoot().toString()));
    	options.parseIniFile();
        Assert.assertEquals(null, options.getClusterSize());
//        options.detectBadOptions();
    }

    @Test(expected = QSVException.class)
    public void testBadOptionsWrongFilterSize() throws Exception {
        Options options = new Options(TestUtil.getInvalidOptions(testFolder, file1, file2, "both", 5, 0, testFolder.getRoot().toString(), testFolder.getRoot().toString()));
        options.parseIniFile();
        Assert.assertEquals(Integer.valueOf(0), options.getFilterSize());
//        options.detectBadOptions();
    }

    @Test(expected = QSVException.class)
    public void testBadOptionsNoFilterSize() throws Exception {
    		Options options = new Options(TestUtil.getInvalidOptions(testFolder, file1, file2, "both", 5, 0, testFolder.getRoot().toString(), testFolder.getRoot().toString()));
    		options.parseIniFile();
        Assert.assertEquals(null, options.getFilterSize());
//        options.detectBadOptions();
    }
    
    @Test
    public void outputOverride() throws IOException, QSVException {
    	 	String[] args = TestUtil.getValidOptions(testFolder, file1, file2, "both", "both", false);
    	 	Options options = new Options(args);
    	 	try {
    	 		options.parseIniFile();
    	 		Assert.fail("Should have thrown an Exception");
    	 	} catch (QSVException qsve) {}
    	 	
    	 	// add in the override option
    	 	
    	 	String [] newArgs = Arrays.copyOf(args, args.length + 2);
    	 	newArgs[newArgs.length - 2] = "--overrideOutput";
    	 	newArgs[newArgs.length - 1] = testFolder.newFile().getAbsolutePath();
    	 	
    	 	options = new Options(newArgs);
    	 	options.parseIniFile();
    	 	
    }

    @Test
    public void testDirectoryExists() throws QSVException, IOException {
        Options options = new Options(new String[0]);

        // is directory and exists
        boolean exists = options.directoryExists(testFolder.newFolder("testFolder").toString());
        Assert.assertTrue(exists);

        // doesn't exist
        boolean doesntExist = options.directoryExists("abcd");
        Assert.assertFalse(doesntExist);

        // doesn't exist
        File newFile = testFolder.newFile("testFile");
        boolean isFile = options.directoryExists(newFile.toString());
        Assert.assertTrue(newFile.exists());
        Assert.assertFalse(isFile);
    }
    
    @Test(expected = QSVException.class)
    public void testBadOptionsIniFileOption() throws QSVException, InvalidFileFormatException, IOException {
    	Options options = new Options(new String[] {"-log", "log.log", "--loglevel", "INFO"});

        assertEquals(null, options.getIniFile());
        options.detectBadOptions();
    }

}
