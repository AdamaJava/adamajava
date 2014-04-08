package org.qcmg.qsv.annotate;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMFileHeader.SortOrder;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.annotate.Annotator;
import org.qcmg.qsv.annotate.RunTypeRecord;
import org.qcmg.qsv.util.TestUtil;

public class AnnotatorTest {   
   
    private List<SAMRecord> records = new ArrayList<SAMRecord>();
    private List<RunTypeRecord> sequencingRuns;
    
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	private File tumourBam;
    
    @Before
    public void setUp() throws IOException, Exception {
    	records = new ArrayList<SAMRecord>();
        tumourBam = testFolder.newFile("test.bam");
        TestUtil.createBamFile(tumourBam.getCanonicalPath(), null, SortOrder.coordinate);
        SAMFileReader read = new SAMFileReader(tumourBam);
        
        for (SAMRecord r : read) {
            records.add(r);
        }
        
        read.close();
    	setupSequencingRuns();
    }
    
    private void setupSequencingRuns() {
    	sequencingRuns = new ArrayList<RunTypeRecord>();
		RunTypeRecord r = new RunTypeRecord("testFile", 300, 2700, "seq_mapped");
		r.setRgId("20110221052813657");
		sequencingRuns.add(r);
	}

	@Test
    public void testLMPAnnotate() throws Exception {
		//QSVParameters p = TestUtil.getQSVParameters(testFolder, testFolder.newFile("normal.bam").getAbsolutePath(), tumourBam.getAbsolutePath(), true, "none");
        Annotator lmp = new Annotator(250, 2790, testFolder.newFile("testFile"), "lmp", sequencingRuns, "lmp", "bioscope");
        //duplicate, passes vendor check
        SAMRecord r1 = records.get(0);
        r1.setFlags(1025);
        
        //singleton, passes vendor check
        SAMRecord r2 = records.get(1);
        
        SAMRecord r3 = records.get(3); 
        //singleton, fails vendor check
        SAMRecord r4 = records.get(4);
        r4.setFlags(512);
        
        //duplicate, fails vendor check
        SAMRecord r5 = records.get(5);
        r5.setFlags(1536);
        
        SAMRecord r6 = records.get(6); 
        r6.setFlags(129);        
    
        lmp.annotate(r1);
        assertEquals("V**", (String)r1.getAttribute("ZP"));
        lmp.annotate(r2);
        assertEquals("AAC", (String)r2.getAttribute("ZP"));
        lmp.annotate(r3);
        assertEquals("ABC", (String)r3.getAttribute("ZP"));
        lmp.annotate(r4);
        assertEquals("X**", (String)r4.getAttribute("ZP"));
        lmp.annotate(r5);
        assertEquals("W**", (String)r5.getAttribute("ZP"));
        lmp.annotate(r6);
        assertEquals("ABC", (String)r6.getAttribute("ZP"));
        
//        System.out.println(r1.getSAMString());
//        System.out.println(r2.getSAMString());
//        System.out.println(r3.getSAMString());
//        System.out.println(r4.getSAMString());
//        System.out.println(r5.getSAMString());
//        System.out.println(r6.getSAMString());
        
        assertEquals(3, lmp.getDuplicates().intValue());
        assertEquals(3, lmp.getSingletons().intValue());
        assertEquals(6, lmp.getTotalCount().intValue());
        assertEquals(2, lmp.getZpCount().get("ABC").intValue());
        assertEquals(1, lmp.getZpCount().get("AAC").intValue());
    }
    
    
    @Test
    public void testPEAnnotate() throws Exception {
    	//QSVParameters p = TestUtil.getQSVParameters(testFolder, testFolder.newFile("normal.bam").getAbsolutePath(), tumourBam.getAbsolutePath(), true, "none");
        Annotator pe = new Annotator(250, 2790, testFolder.newFile("testFile"), "pe", sequencingRuns,  "lmp", "bioscope");
        //duplicate, passes vendor check
        SAMRecord r1 = records.get(0);
        r1.setFlags(1025);
        
        //singleton, passes vendor check
        SAMRecord r2 = records.get(1);
        r2.setFlags(65);
        
        SAMRecord r3 = records.get(3); 
        //singleton, fails vendor check
        SAMRecord r4 = records.get(4);
        r4.setFlags(512);
        
        //duplicate, fails vendor check
        SAMRecord r5 = records.get(5);
        r5.setFlags(1536);
        
        SAMRecord r6 = records.get(6); 
        r6.setFlags(129);
        
        pe.annotate(r1);        
        pe.annotate(r2);
        pe.annotate(r3);
        pe.annotate(r4);
        pe.annotate(r5);
        pe.annotate(r6);
        
        assertEquals(3, pe.getDuplicates().intValue());
        assertEquals(3, pe.getSingletons().intValue());
        assertEquals(6, pe.getTotalCount().intValue());
        assertEquals(3, pe.getZpCount().get("BAC").intValue());
    }

}
