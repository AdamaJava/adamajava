package org.qcmg.qsv;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.ini4j.InvalidFileFormatException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.meta.QExec;
import org.qcmg.qsv.util.TestUtil;

import static org.junit.Assert.*;

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
    	File tmp = testFolder.newFolder();
        String[] args = TestUtil.getValidOptions(tmp, file1, file2, "both", "both");
        Options options = new Options(args);
        assertFalse(options.hasHelpOption());
        assertFalse(options.hasVersionOption());
        assertEquals(options.getIniFile(), tmp.getAbsolutePath() + "/test.ini");
        assertEquals(options.getTempDirName(), tmp.getAbsolutePath());
    }
    
    @Test
    public void bgiPlatform() throws Exception {
    	File tmp = testFolder.newFolder();
    	String[] args = TestUtil.getValidOptions(tmp, file1, file2, "both", "both", true, "bwamem", "MGISeq2000");
    	Options options = new Options(args);
    	assertFalse(options.hasHelpOption());
    	assertFalse(options.hasVersionOption());
    	assertEquals(options.getIniFile(), tmp.getAbsolutePath() + "/test.ini");
    	assertEquals(options.getTempDirName(), tmp.getAbsolutePath());
    	options.parseIniFile();
        options.detectBadOptions();
    	assertEquals("bgi", options.getPlatform());
    	assertEquals("MGISeq2000", options.getSequencingPlatform());
    }
    
    @Test
    public void bgiPlatformAgain() throws Exception {
    	File tmp = testFolder.newFolder();
    	String[] args = TestUtil.getValidOptions(tmp, file1, file2, "both", "both", true, "bwamem", "BGISeq500");
    	Options options = new Options(args);
    	assertFalse(options.hasHelpOption());
    	assertFalse(options.hasVersionOption());
    	assertEquals(options.getIniFile(), tmp.getAbsolutePath() + "/test.ini");
    	assertEquals(options.getTempDirName(), tmp.getAbsolutePath());
    	options.parseIniFile();
    	options.detectBadOptions();
    	assertEquals("bgi", options.getPlatform());
    	assertEquals("BGISeq500", options.getSequencingPlatform());
    }
    @Test
    public void novaSeq() throws Exception {
    	File tmp = testFolder.newFolder();
    	String[] args = TestUtil.getValidOptions(tmp, file1, file2, "both", "both", true, "bwamem", "NovaSeq");
    	Options options = new Options(args);
    	assertFalse(options.hasHelpOption());
    	assertFalse(options.hasVersionOption());
    	assertEquals(options.getIniFile(), tmp.getAbsolutePath() + "/test.ini");
    	assertEquals(options.getTempDirName(), tmp.getAbsolutePath());
    	options.parseIniFile();
    	options.detectBadOptions();
    	assertEquals("illumina", options.getPlatform());
    	assertEquals("NovaSeq", options.getSequencingPlatform());
    }
    
    @Test
    public void nextSeq() throws Exception {
    	File tmp = testFolder.newFolder();
    	String[] args = TestUtil.getValidOptions( tmp, file1, file2, "both", "both", true, "bwamem", "NextSeq");
    	Options options = new Options(args);
    	assertFalse(options.hasHelpOption());
    	assertFalse(options.hasVersionOption());
    	assertEquals(options.getIniFile(), tmp.getAbsolutePath() + "/test.ini");
    	assertEquals(options.getTempDirName(), tmp.getAbsolutePath());
    	options.parseIniFile();
    	options.detectBadOptions();
    	assertEquals("illumina", options.getPlatform());
    	assertEquals("NextSeq", options.getSequencingPlatform());
    }
    
    @Test(expected = QSVException.class)
    public void dummySeq() throws Exception {
    	File tmp = testFolder.newFolder();
    	String[] args = TestUtil.getValidOptions(tmp, file1, file2, "both", "both", true, "bwamem", "anySeqButHere");
    	Options options = new Options(args);
    	assertFalse(options.hasHelpOption());
    	assertFalse(options.hasVersionOption());
    	assertEquals(options.getIniFile(), tmp.getAbsolutePath() + "/test.ini");
    	assertEquals(options.getTempDirName(), tmp.getAbsolutePath());
    	options.parseIniFile();
    }
    
    @Test
    public void testGoodIniFileOptions() throws Exception {
    	File tmp = testFolder.newFolder();
        String[] args = TestUtil.getValidOptions(tmp, file1, file2, "both", "both");
        Options options = new Options(args);
        options.parseIniFile();
        options.detectBadOptions();
        assertEquals(options.getLog(), "test.log");
        assertEquals(options.getLogLevel(), "DEBUG");
        assertEquals(options.getSampleName(), "test");
        assertEquals(options.getOutputDirName(), tmp + Options.FILE_SEPARATOR + options.getUuid() + Options.FILE_SEPARATOR);
        assertTrue(options.getReference().contains("reference_file"));
        assertEquals(options.getPreprocessMode(), "both");
        assertEquals(options.getAnalysisMode(), "both");        
        assertEquals(options.getMaxISizeCount(), "all");
        assertEquals(options.getMinInsertSize(), Integer.valueOf(50));
        assertTrue(options.isQCMG());
        assertEquals(options.getPairingType(), "lmp");
        assertEquals(options.getPairQuery(), "and(Cigar_M > 35,option_SM > 14,MD_mismatch < 3,Flag_DuplicateRead == false)");
        assertEquals(options.getClusterSize().intValue(), 3);
        assertEquals(options.getFilterSize().intValue(), 1);
        assertEquals(options.getQPrimerThreshold().intValue(), 3);
        assertEquals(options.getMapper(), "bioscope");
        assertEquals(options.getClipQuery(), "and(Cigar_M > 35,option_SM > 14,MD_mismatch < 3,Flag_DuplicateRead == false)");
        assertEquals(options.getClipSize().intValue(), 3);
        assertEquals(options.getConsensusLength(), Integer.valueOf(20));
        
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
    public void testVersionOption() throws QSVException, IOException {
    	//long form option
        Options options = new Options(new String[] {"--version"});
        assertTrue(options.hasVersionOption());

        //short form option
        Options options2 = new Options(new String[] {"-v"});
        assertTrue(options2.hasVersionOption());

        try {
        	 Options options3 = new Options(new String[] {"-V"});
            fail("except to throw exception");
         }  catch(Exception e) {}
        
    }
    
    @Test(expected = QSVException.class)
    public void testBadOptionsNoLogFile() throws QSVException, IOException {
        Options options = new Options(new String[] {"--version"});
        options.detectBadOptions();
    }
    
    @Test(expected = QSVException.class)
    public void testBadOptionsBadModes() throws QSVException, IOException {
        Options options = new Options(new String[] {"--version"});
        options.setLogFile("log");
        options.setPreprocessMode("pair");
        options.setAnalysisMode("clip");
        options.detectBadOptions();
    }
    
    @Test(expected = QSVException.class)
    public void testBadOptionsBadModes2() throws QSVException, IOException {
        Options options = new Options(new String[] {"--version"});
        options.setLogFile("log");
        options.setPreprocessMode("clip");
        options.setAnalysisMode("pair");
        options.detectBadOptions();
    }
    
    @Test(expected = QSVException.class)
    public void testBadOptionsNoPairType() throws QSVException, IOException {
    	File tmp = testFolder.newFolder();
    	String[] args = TestUtil.getValidOptions(tmp, file1, file2, "pair", "pair");
    	Options options = new Options(args);
    	options.parseIniFile();
    	options.setPairingType("te");
        options.detectBadOptions();
    }
    
    @Test(expected = QSVException.class)
    public void testBadOptionsNoMapper() throws QSVException, IOException {
    	File tmp = testFolder.newFolder();
    	String[] args = TestUtil.getValidOptions(tmp, file1, file2, "pair", "pair");
    	Options options = new Options(args);
    	options.parseIniFile();
    	options.setPairingType("pe");
    	options.setMapper("no");
        options.detectBadOptions();
    }
    
    @Test(expected = QSVException.class)
    public void testBadOptionsNoTmpDir() throws QSVException, IOException {
    	File tmp = testFolder.newFolder();
    	String[] args = TestUtil.getValidOptions(tmp, file1, file2, "both", "both");
    	Options options = new Options(args);
    	options.parseIniFile();
        options.setTmpDir("/test");
        options.detectBadOptions();
    }
    
   
    @Test(expected = QSVException.class)
    public void testBadOptionsBadAnalysisMode() throws QSVException, IOException {
        Options options = new Options(new String[] {"--version"});
        options.setLogFile("log");
        options.setPreprocessMode("none");
        options.setAnalysisMode("bad");
        options.detectBadOptions();
    }
 
    @Test(expected = QSVException.class)
    public void testBadOptionsNonExistentInputFile() throws Exception {
    	File tmp = testFolder.newFolder();
    	 Options options = new Options(TestUtil.getInvalidOptions(tmp, file1, "acd", "both", 5, 0, tmp.getAbsolutePath(), "acd"));
         options.parseIniFile();
         Assert.assertEquals("acd", options.getInputFile());
    }
    
    @Test(expected = QSVException.class)
    public void testBadOptionsNonExistentInputComparisonFile() throws Exception {
    	File tmp = testFolder.newFolder();
    	 Options options = new Options(TestUtil.getInvalidOptions(tmp, "acd", file2, "both", 5, 0, tmp.getAbsolutePath(), "acd"));
         options.parseIniFile();
         Assert.assertEquals("acd", options.getComparisonFile());
    }

    @Test(expected = QSVException.class)
    public void testBadOptionsNonexistentOutputDir() throws Exception {
    	File tmp = testFolder.newFolder();
    	 Options options = new Options(TestUtil.getInvalidOptions(tmp, file1, file2, "both", 5, 0, tmp.getAbsolutePath(), "acd"));
         options.parseIniFile();
         Assert.assertEquals("acd", options.getOutputDirName());
    }
    
    @Test(expected = QSVException.class)
    public void testBadOptionsNoClusterSize() throws Exception {
    	File tmp = testFolder.newFolder();
    	Options options = new Options(TestUtil.getInvalidOptions(tmp, file1, file2, "both", 5, 0, tmp.getAbsolutePath(), tmp.getAbsolutePath()));
    	options.parseIniFile();
        Assert.assertEquals(null, options.getClusterSize());
    }
    
    @Test(expected = QSVException.class)
    public void testBadOptionsWrongClusterSize() throws Exception {
    	File tmp = testFolder.newFolder();
    	Options options = new Options(TestUtil.getInvalidOptions(tmp, file1, file2, "both", 0, 2,tmp.getAbsolutePath(), tmp.getAbsolutePath()));
    	options.parseIniFile();
        Assert.assertEquals(null, options.getClusterSize());
    }

    @Test(expected = QSVException.class)
    public void testBadOptionsWrongFilterSize() throws Exception {
    	File tmp = testFolder.newFolder();
        Options options = new Options(TestUtil.getInvalidOptions(tmp, file1, file2, "both", 5, 0, tmp.getAbsolutePath(), tmp.getAbsolutePath()));
        options.parseIniFile();
        Assert.assertEquals(Integer.valueOf(0), options.getFilterSize());
    }

    @Test(expected = QSVException.class)
    public void testBadOptionsNoFilterSize() throws Exception {
    	File tmp = testFolder.newFolder();
    		Options options = new Options(TestUtil.getInvalidOptions(tmp, file1, file2, "both", 5, 0, tmp.getAbsolutePath(), tmp.getAbsolutePath()));
    		options.parseIniFile();
        Assert.assertEquals(null, options.getFilterSize());
    }
    
    @Test
    public void getRunIdTest() throws IOException, QSVException {
    	//create output dir
    	File tmp = testFolder.newFolder();
    	
    	//create ini file
    	String[] args = TestUtil.getValidOptions(tmp, file1, file2, "both", "both", true);
    	
    	//without option --uuid, it will create an uuid as runid
	 	Options options = new Options(args);
	 	try {
			UUID.fromString(options.getUuid());
		} catch (IllegalArgumentException iae) {
			Assert.fail("run id is not an uuid string!");
		}
	 	
	   	// add option --uuid,	
		String [] newArgs = Arrays.copyOf(args, args.length + 2);
	 	newArgs[newArgs.length - 2] = "--uuid";
	 	newArgs[newArgs.length - 1] = "test-uuid";	 
	 	options = new Options(newArgs);
	 	options.parseIniFile();
	 	Assert.assertEquals( options.getUuid(), "test-uuid" );
	 	assertTrue(options.getOutputDirName().endsWith("test-uuid"+Options.FILE_SEPARATOR));
    			
    }
    
    @Test
    public void createResultsDirectoryTest() throws IOException {
    	//create output dir
    	File tmp = testFolder.newFolder();
    	//sub folder name
    	File uuid = new File(tmp.getAbsolutePath() + "/uuid"); 
    	assertTrue(!uuid.exists());
    	
    	//first time create dir
    	try {
    		Options.createResultsDirectory(uuid.getAbsolutePath());
    		assertTrue(uuid.exists());
    	}catch(QSVException e) {
    		Assert.fail("Should not thrown an QSVException");
    	}
    	
        //second time create dir
    	try {
    	   	//create an empty file to the subfolder
        	File iniFile = new File(uuid, "test.ini"); 
        	iniFile.createNewFile();  
        	assertTrue(iniFile.exists());
        	
        	//do nothing if the dir already exists, all files under the dir keep same
    		Options.createResultsDirectory(uuid.getAbsolutePath());
    		assertTrue( iniFile.exists());    		
    	}catch(QSVException e) {
    		Assert.fail("Should not thrown an QSVException");
    	}
    }    
    
    @Test
    public void outputOverride() throws IOException, QSVException {
    	File tmp = testFolder.newFolder();
    	//ini file missing output option.
	 	String[] args = TestUtil.getValidOptions(tmp, file1, file2, "both", "both", false);
	 	Options options = new Options(args);
	 	try {
	 		options.parseIniFile();
	 		Assert.fail("Should have thrown an Exception");
	 	} catch (QSVException qsve) {}
	 	
	 	// add in the override option	 	
	 	String [] newArgs = Arrays.copyOf(args, args.length + 2);
	 	newArgs[newArgs.length - 2] = "--uuid";
	 	newArgs[newArgs.length - 1] = QExec.createUUid();
	 	
	 	options = new Options(newArgs);
	 	try {
	 		//it will fail because missing ini output option 
	 		options.parseIniFile();
	 		Assert.fail("Should have thrown an Exception");
	 	} catch (QSVException qsve) {
	 		String exp = new QSVException("NO_OUTPUT").getMessage();
	 		assertEquals(qsve.getMessage(), exp);
	 		
	 	}
	 	
	 		 	
	 	//add output option to ini file
	 	args = TestUtil.getValidOptions(tmp, file1, file2, "both", "both", true);
	 	options = new Options(newArgs);	 	
	 	options.parseIniFile();
    	 	
    }
    
    @Test
    public void outputDirTest() throws QSVException, IOException {
    	 
	 	//output dir not exists without uuid option
    	File tmp1 = testFolder.newFolder();   	
    	String[] args = TestUtil.getValidOptions(tmp1, file1, file2, "both", "both", true);
    	Options options = new Options(args);
    	//Succeed when output directory exists. 
    	options.parseIniFile();
    	    	
    	//delete output directory, mv ini to a different directory. 
    	File tmp2 = testFolder.newFolder();    
    	Path source = Paths.get(args[1]);
    	Path target = new File(tmp2, "test.new.ini").toPath();    	
    	Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);   	
    	FileUtils.deleteDirectory(tmp1);
        assertFalse(tmp1.exists());
    	
    	args[1] = target.getParent() + "/" + target.getFileName();
 		options = new Options(args);     	 
	 	try {
	 		//it will fail because the ini point to a non-exists directory
	 		options.parseIniFile();
	 		Assert.fail("Should have thrown an Exception");
	 	} catch (QSVException qsve) {
	 		String exp = new QSVException("NO_OUTPUT_DIR", tmp1.getAbsolutePath()).getMessage();
	 		assertEquals(qsve.getMessage(), exp);
	 	}
    	
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
    public void testBadOptionsIniFileOption() throws QSVException, IOException {
    	//removed log option from command line, it should inside the ini
    	//Options options = new Options(new String[] {"-log", "log.log", "--loglevel", "INFO"});
    	Options options = new Options(new String[] {"-help"});
        assertNull(options.getIniFile());
        options.detectBadOptions();
    }
    
    @Test
    public void testBadOptionsReference() throws QSVException, IOException {
    	File tmp = testFolder.newFolder();
    	File iniFile = testFolder.newFile("file.ini");
    	File genericInputFile = testFolder.newFile();
    	File refFile = testFolder.newFile("reference.fasta");
    	
    	List<String> data = Arrays.asList("[general]", 
    			"qcmg=true", 
    			"sample=sample", 
    			"output=" + tmp.getAbsolutePath(), 
    			"tiled_aligner=" + genericInputFile.getAbsolutePath(), 
    			"reference=" + refFile.getAbsolutePath(), 
    			"[test]", 
    			"name=test",
    			"input_file=" + genericInputFile.getAbsolutePath());
    	
    	writeIni(iniFile, data);
    	
        String[] args = new String[] {"--output-temporary", tmp.getAbsolutePath(), "--ini", iniFile.getAbsolutePath()};
        Options options = new Options(args);
        /*
         * no reference index file specified
         */
        try {
        	options.parseIniFile();
        	Assert.fail("Should have thrown an exception");
        } catch (QSVException ignored) {}
        
        /*
         * create ref index file but not with expected suffix - should still throw an exception
         */
        try {
        	testFolder.newFile("reference.fasta.fia");
        	options.parseIniFile();
        	Assert.fail("Should have thrown an exception");
        } catch (QSVException ignored) {}
        
        /*
         * create reference index file next to reference file with correct suffix
         */
        File refIndexFile = testFolder.newFile("reference.fasta.fai");
        options.parseIniFile();
        assertEquals(refIndexFile.getAbsolutePath(), options.getReferenceIndex());
        
        
        iniFile.delete();
        /*
         * and finally by explicitly setting the reference index
         */
        data = Arrays.asList("[general]", 
    			"qcmg=true", 
    			"sample=sample", 
    			"output=" + tmp.getAbsolutePath(), 
    			"tiled_aligner=" + genericInputFile.getAbsolutePath(), 
    			"reference=" + refFile.getAbsolutePath(), 
    			"reference_index=" + refIndexFile.getAbsolutePath(), 
    			"[test]", 
    			"name=test", 
    			"input_file=" + genericInputFile.getAbsolutePath());
    	
    	writeIni(iniFile, data);
    	options.parseIniFile();
    	assertEquals(refIndexFile.getAbsolutePath(), options.getReferenceIndex());
        
    }
    
    private void writeIni(File iniFile, List<String> contents) throws IOException {
    	try (BufferedWriter out = new BufferedWriter(new FileWriter(iniFile))) {
    		for (String s : contents) {
    			out.write(s + "\n");
    		}
    	}
    }

}
