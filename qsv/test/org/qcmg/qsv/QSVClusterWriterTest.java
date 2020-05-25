package org.qcmg.qsv;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import htsjdk.samtools.SAMFileHeader.SortOrder;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.discordantpair.PairGroup;
import org.qcmg.qsv.util.QSVUtil;
import org.qcmg.qsv.util.TestUtil;



public class QSVClusterWriterTest {

	private QSVCluster record;
	private QSVParameters tumor;
	private QSVParameters normal;
	private QSVClusterWriter writer;
	private List<QSVCluster> list;
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	

    @Before
    public void setUp() throws IOException, Exception {
		File tumorBam = TestUtil.createSamFile(testFolder.newFile("tumor.bam").getAbsolutePath(), PairGroup.AAC, SortOrder.queryname, true);
        tumor = TestUtil.getQSVParameters(testFolder.getRoot(), tumorBam.getAbsolutePath(), tumorBam.getAbsolutePath(), true, "both");
        normal = TestUtil.getQSVParameters(testFolder.getRoot(), tumorBam.getAbsolutePath(), tumorBam.getAbsolutePath(), false, "both");         
        tumor.setReference("file");
        normal.setReference("file");
		list = new ArrayList<>();
    }
    
    @After
    public void tearDown() {
    	record = null;
    	writer = null;
    	list = null;
    	tumor = null;
    	normal = null;
    }
    
    @Test
    public void testWriteTumourSVRecordsNoQCMGSomatic() throws IOException, Exception {
    	File tmp = testFolder.getRoot();       	
    	record = TestUtil.setupQSVCluster(PairGroup.AAC, "somatic", tmp, "chr7", "chr7", false, false);
    	list.add(record);
    	writer = new QSVClusterWriter(tumor, normal, false, "test", false, true, 50, "solid", new ArrayList<String>());
    	writer.writeTumourSVRecords(list);
    	String [] array = testFolder.getRoot().list();
    	if (null != array) {
	    	List<String> filesCreated = Arrays.asList(array);
	    	assertTrue(filesCreated.contains("test.somatic.sv.txt"));
	    	assertTrue(filesCreated.contains("test.germline.sv.txt"));
	    	assertTrue(filesCreated.contains("test.chr7.somatic.records"));
	    	assertFalse(filesCreated.contains("test.germline.dcc"));
	    	assertFalse(filesCreated.contains("test.somatic.dcc"));
	    	assertFalse(filesCreated.contains("test.somatic.qprimer"));
    	}
    	assertLineCount(tmp.getAbsolutePath() + QSVUtil.getFileSeparator() + "test.germline.sv.txt", 1);
       	assertLineCount(tmp.getAbsolutePath() + QSVUtil.getFileSeparator() + "test.somatic.sv.txt", 2);
    }
    
    @Test
    public void testWriteTumourSVRecordsQCMGSomatic() throws IOException, Exception {
    	File tmp = testFolder.getRoot();     
    	record = TestUtil.setupQSVCluster(PairGroup.AAC, "somatic", tmp, "chr7", "chr7", false, false);
    	list.add(record);
    	writer = new QSVClusterWriter(tumor, normal, true, "test", false, true, 50, "solid", new ArrayList<String>());
    	writer.writeTumourSVRecords(list);
    	String [] array = testFolder.getRoot().list();
    	if (null != array) {
	    	List<String> filesCreated = Arrays.asList(array);
	    	assertTrue(filesCreated.contains("test.germline.dcc"));
	    	assertTrue(filesCreated.contains("test.somatic.dcc"));
	    	assertTrue(filesCreated.contains("test.somatic.qprimer"));
    	}
    	assertLineCount(tmp.getAbsolutePath()+ QSVUtil.getFileSeparator() + "test.germline.dcc", 0);
    	assertLineCount(tmp.getAbsolutePath()+ QSVUtil.getFileSeparator() + "test.somatic.dcc", 1);
    	assertLineCount(tmp.getAbsolutePath()+ QSVUtil.getFileSeparator() + "test.somatic.qprimer", 1);
    }
    
    @Test
    public void testWriteTumourSVRecordsNoQCMGGermline() throws IOException, Exception {
    	File tmp = testFolder.getRoot();   
    	record = TestUtil.setupQSVCluster(PairGroup.AAC, "somatic", tmp, "chr7", "chr7", true, false);
    	list.add(record);
    	writer = new QSVClusterWriter(tumor, normal, false, "test", false, true, 50, "solid",new ArrayList<String>());
    	writer.writeTumourSVRecords(list);
    	String [] array = testFolder.getRoot().list();
    	if (null != array) {
	    	List<String> filesCreated = Arrays.asList(array);
	    	assertTrue(filesCreated.contains("test.somatic.sv.txt"));
	    	assertTrue(filesCreated.contains("test.germline.sv.txt"));
	    	assertTrue(filesCreated.contains("test.chr7.germline.records"));
    	}
    	assertLineCount(tmp.getAbsolutePath()+ QSVUtil.getFileSeparator() + "test.germline.sv.txt", 2);
    	assertLineCount(tmp.getAbsolutePath()+ QSVUtil.getFileSeparator() + "test.somatic.sv.txt", 1);
    }
    
    @Test
    public void testWriteTumourSVRecordsQCMGGermline() throws IOException, Exception {
    	File tmp = testFolder.getRoot();   
    	record = TestUtil.setupQSVCluster(PairGroup.AAC, "somatic", tmp, "chr7", "chr7", true, false);
    	list.add(record);
    	writer = new QSVClusterWriter(tumor, normal, true, "test", false, true, 50, "solid",new ArrayList<String>());
    	writer.writeTumourSVRecords(list);
    	String [] array = tmp.list();
    	
    	if (null != array) {
	    	List<String> filesCreated = Arrays.asList(array);
	    	assertTrue(filesCreated.contains("test.germline.dcc"));
	    	assertTrue(filesCreated.contains("test.somatic.dcc"));
	    	assertTrue(filesCreated.contains("test.somatic.qprimer"));
    	}
    	assertLineCount(tmp.getAbsolutePath()+ QSVUtil.getFileSeparator() + "test.germline.dcc", 1);
    	assertLineCount(tmp.getAbsolutePath()+ QSVUtil.getFileSeparator() + "test.somatic.dcc", 0);
    }
    
    @Test
    public void testWriteNormalSVRecordsNoQCMG() throws IOException, Exception {
    	File tmp = testFolder.getRoot();   
    	record = TestUtil.setupQSVCluster(PairGroup.AAC, "normal-germline", tmp, "chr7", "chr7", false, false);
    	list.add(record);
    	writer = new QSVClusterWriter(tumor, normal, false, "test", false, true, 50, "solid",new ArrayList<String>());
    	writer.writeNormalSVRecords(list);
    	String [] array = testFolder.getRoot().list();
    	if (null != array) {
	    	List<String> filesCreated = Arrays.asList(array);
	    	assertFalse(filesCreated.contains("test.normal-germline.sv.txt"));
	    	assertFalse(filesCreated.contains("test.chr7.normal-germline.records"));
    	}
    }
    
    @Test
    public void testWriteNormalSVRecordsQCMG() throws IOException, Exception {
    	File tmp = testFolder.getRoot();   
    	record = TestUtil.setupQSVCluster(PairGroup.AAC, "normal-germline", tmp, "chr7", "chr7", false, false);
    	list.add(record);
    	writer = new QSVClusterWriter(tumor, normal, true, "test", false, true, 50, "solid",new ArrayList<String>());
    	writer.writeNormalSVRecords(list);
    	String [] array = tmp.list();
    	if (null != array) {
	    	List<String> filesCreated = Arrays.asList(array);
	    	assertTrue(filesCreated.contains("test.normal-germline.sv.txt"));
	    	assertTrue(filesCreated.contains("test.chr7.normal-germline.records"));
    	}
    }
    
    @Test
    public void testWriteReportsSingleSidedIsPrinted() throws Exception {
    	File tmp = testFolder.getRoot();   
    	record = TestUtil.setupQSVCluster(PairGroup.AAC, "somatic", tmp, "chr7", "chr7", false, true);
    	list.add(record);
    	assertTrue(record.singleSidedClip());
    	writer = new QSVClusterWriter(tumor, normal, true, "test", true, true, 50, "solid",new ArrayList<String>());
    	writer.writeTumourSVRecords(list);
    	assertLineCount(tmp.getAbsolutePath()+ QSVUtil.getFileSeparator() + "test.somatic.sv.txt", 2);
     	assertLineCount(tmp.getAbsolutePath()+ QSVUtil.getFileSeparator() + "test.somatic.dcc", 1);
    	
    	try (BufferedReader reader = new BufferedReader(new FileReader(new File(tmp.getAbsolutePath()+ QSVUtil.getFileSeparator() + "test.somatic.sv.txt")));) {
	    	String line = reader.readLine();
	    	int count = 0;
	    	while (line != null) {
	    		count++;    		
	    		
	    		if (count == 2) {
	    			assertTrue(line.contains("not tested\tnot tested"));
	    		}
	    		line = reader.readLine();
	    	}
    	}
    }
    
    @Test
    public void testWriteReportsSingleSidedIsNotPrinted() throws IOException, Exception {
    	File tmp =  testFolder.getRoot();
    	record = TestUtil.setupQSVCluster(PairGroup.AAC, "somatic", tmp, "chr7", "chr7", false, true);
    	list.add(record);
    	assertTrue(record.singleSidedClip());
    	writer = new QSVClusterWriter(tumor, normal, true, "test", false, true, 50, "solid",new ArrayList<String>());
    	writer.writeTumourSVRecords(list);
    	assertLineCount(tmp.getAbsolutePath()+ QSVUtil.getFileSeparator() + "test.somatic.sv.txt", 1);
    	assertLineCount(tmp.getAbsolutePath()+ QSVUtil.getFileSeparator() + "test.somatic.dcc", 0);    	
    }
	    
    public void assertLineCount(String file, int expectedCount) throws IOException {
		assertEquals(expectedCount,  Files.lines(Paths.get(file)).count());
    }
}
