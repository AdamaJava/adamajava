package org.qcmg.qsv.annotate;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMFileHeader.SortOrder;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.annotate.SOLiDLongMatePairRecord;
import org.qcmg.qsv.util.TestUtil;

public class SOLiDLongMatePairRecordTest {
    
    private List<SAMRecord> records = new ArrayList<SAMRecord>();
    
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    
    @Before
    public void setUp() throws IOException {
    	File file = TestUtil.createSamFile(testFolder.newFile("test.bam").getAbsolutePath(), SortOrder.unsorted, false);
        final SAMFileReader sam = new SAMFileReader(file);
        for (final SAMRecord samRecord : sam) {
        	//System.out.println(samRecord.getSAMString());
            records.add(samRecord);
        }
        sam.close();
    }
    
    @After
    public void after() throws IOException {
    	records.clear();
    }
    
    @Test    
    public void testAnnotationRecord() {
        assertEquals(12, records.size());
        assertZPAnnotation(records.get(0), "ABC");
        assertZPAnnotation(records.get(1), "ABC");
        assertZPAnnotation(records.get(2), "C**");
        assertZPAnnotation(records.get(3), "C**");
        assertZPAnnotation(records.get(4), "Z**");
        assertZPAnnotation(records.get(5), "Z**");
        assertZPAnnotation(records.get(6), "Z**");
        assertZPAnnotation(records.get(7), "Z**");
        assertZPAnnotation(records.get(8), "AAC");
        assertZPAnnotation(records.get(9), "AAC");
        assertZPAnnotation(records.get(10), "AAB");
        assertZPAnnotation(records.get(11), "AAB");   
    }
    
    public void assertZPAnnotation(SAMRecord r, String expected) {
            SOLiDLongMatePairRecord lmp = new SOLiDLongMatePairRecord(r, 640, 2360);
            lmp.createZPAnnotation();   
            assertEquals(expected, lmp.getZPAnnotation());
    }
    
    @Test
    public void testR3toF3WithR3Read() {
        SAMRecord record = records.get(1);        
        //Read is R3 (second pair), R3 left of F3, Same strand, forward
        record.setFlags(129);
        assertFalse(record.getFirstOfPairFlag());
        assertTrue(record.getAlignmentStart() < record.getMateAlignmentStart());
        SOLiDLongMatePairRecord lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        assertTrue(lmp.isR3ToF3());
        assertFalse(lmp.isF3ToR3());
        lmp.createZPAnnotation();
        
        //Read is R3 (second pair), R3 left of F3, Same strand, reverse
        record = records.get(0);
        record.setFlags(177);
        assertFalse(record.getFirstOfPairFlag());
        assertTrue(record.getAlignmentStart() > record.getMateAlignmentStart());
        lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        assertTrue(lmp.isR3ToF3());
        assertFalse(lmp.isF3ToR3());
    }
    
    @Test
    public void testR3toF3WithF3Read() {
        SAMRecord record = records.get(1);        
        
        //Read is F3 (first of pair), F3 left of R3, Same strand, forward
        record.setFlags(113);
        assertTrue(record.getFirstOfPairFlag());
        assertTrue(record.getAlignmentStart() < record.getMateAlignmentStart());
        SOLiDLongMatePairRecord lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        assertTrue(lmp.isR3ToF3());
        assertFalse(lmp.isF3ToR3());
        
        //Read is F3 (first of pair), F3 right of R3, Same strand, forward
        record = records.get(0);
        record.setFlags(65);
        assertTrue(record.getFirstOfPairFlag());
        assertTrue(record.getAlignmentStart() > record.getMateAlignmentStart());
        lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        assertTrue(lmp.isR3ToF3());
        assertFalse(lmp.isF3ToR3());
    }
    
    @Test
    public void testF3toR3WithR3Read() {
        SAMRecord record = records.get(1);        
        
        //Read is R3 (second pair), R3 left of F3, Same strand, forward
        record.setFlags(177);
        assertFalse(record.getFirstOfPairFlag());
        assertTrue(record.getAlignmentStart() < record.getMateAlignmentStart());
        SOLiDLongMatePairRecord lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        assertFalse(lmp.isR3ToF3());
        assertTrue(lmp.isF3ToR3());

        
        //Read is R3 (second pair), R3 left of F3, Same strand, reverse
        record = records.get(0);
        record.setFlags(129);
        assertFalse(record.getFirstOfPairFlag());
        assertTrue(record.getAlignmentStart() > record.getMateAlignmentStart());
        lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        assertFalse(lmp.isR3ToF3());
        assertTrue(lmp.isF3ToR3());
    }
    
    @Test
    public void testF3toR3WithF3Read() {
        SAMRecord record = records.get(1);        
        
        //Read is F3 (first of pair), F3 left of R3, Same strand, forward
        record.setFlags(65);
        assertTrue(record.getFirstOfPairFlag());
        assertTrue(record.getAlignmentStart() < record.getMateAlignmentStart());
        SOLiDLongMatePairRecord lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        assertFalse(lmp.isR3ToF3());
        assertTrue(lmp.isF3ToR3());
        
        //Read is F3 (first of pair), F3 right of R3, Same strand, forward
        record = records.get(0);
        record.setFlags(113);
        assertTrue(record.getFirstOfPairFlag());
        assertTrue(record.getAlignmentStart() > record.getMateAlignmentStart());
        lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        assertFalse(lmp.isR3ToF3());
        assertTrue(lmp.isF3ToR3());
    }
    
    @Test 
    public void testHandleIntervalSize() {
        //normal
        SAMRecord record = records.get(1);
        SOLiDLongMatePairRecord lmp = new SOLiDLongMatePairRecord(record, 1000, 3000);
        lmp.setZPAnnotation("");  
        
        assertEquals("A", lmp.handleIntervalSize());
        
        //too small
        record = records.get(3);
        lmp = new SOLiDLongMatePairRecord(record, 1000, 2360);
        lmp.setZPAnnotation("");    
        assertEquals("B", lmp.handleIntervalSize());
        
        //too big
        record = records.get(1);
        lmp = new SOLiDLongMatePairRecord(record, 1000, 1360);
        lmp.setZPAnnotation("");       
        assertEquals("C", lmp.handleIntervalSize());
    }
    
    @Test    
    public void testIsInward() {
        SAMRecord record = records.get(1);
        record.setFlags(97);
        SOLiDLongMatePairRecord lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        assertTrue(lmp.isInward());
        assertTrue(lmp.isReadLeftOfMate());
        assertTrue(lmp.isReadForward());
        assertTrue(lmp.isMateReverse());
        
        record = records.get(2);
        record.setFlags(177);
        lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        assertFalse(lmp.isInward());
        assertTrue(lmp.isReadReverse());
        assertTrue(lmp.isMateReverse());
        
        record = records.get(0);
        record.setFlags(145);
        lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        assertTrue(lmp.isInward());
        assertTrue(lmp.isReadRightOfMate());
        assertTrue(lmp.isReadReverse());
        assertTrue(lmp.isMateForward());
    }
    
    @Test    
    public void testIsOutward() {
        SAMRecord record = records.get(1);
        record.setFlags(145);
        SOLiDLongMatePairRecord lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        assertTrue(lmp.isOutward());
        assertTrue(lmp.isReadLeftOfMate());
        assertTrue(lmp.isMateForward());
        assertTrue(lmp.isReadReverse());

        record = records.get(2);
        record.setFlags(177);
        lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        assertFalse(lmp.isOutward());
        assertTrue(lmp.isReadReverse());
        assertTrue(lmp.isMateReverse());
        
        record = records.get(0);
        record.setFlags(161);
        lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        assertTrue(lmp.isOutward());
        assertTrue(lmp.isReadRightOfMate());
        assertTrue(lmp.isReadForward());
        assertTrue(lmp.isMateReverse());
    }
    
    @Test
    public void testSameStrandIsTrue () {
        SAMRecord record1 = records.get(0);        
        
        //Both on positive strand
        record1.setFlags(65);
        assertFalse(record1.getReadNegativeStrandFlag());
        assertFalse(record1.getMateNegativeStrandFlag());
        SOLiDLongMatePairRecord lmp = new SOLiDLongMatePairRecord(record1, 350, 2360);
        assertTrue(lmp.isSameStrand());
        
        //Both on negative strand
        record1.setFlags(113);
        assertTrue(record1.getReadNegativeStrandFlag());
        assertTrue(record1.getMateNegativeStrandFlag());
        lmp = new SOLiDLongMatePairRecord(record1, 350, 2360);
        assertTrue(lmp.isSameStrand());
    }
    
    @Test
    public void testSameStrandIsFalse () {
        SAMRecord record1 = records.get(0);        
        
        //Read positive, mate negative
        record1.setFlags(97);
        assertFalse(record1.getReadNegativeStrandFlag());
        assertTrue(record1.getMateNegativeStrandFlag());
        SOLiDLongMatePairRecord lmp = new SOLiDLongMatePairRecord(record1, 350, 2360);
        assertFalse(lmp.isSameStrand());
        
        //Read negative, mate positive
        record1.setFlags(81);
        assertTrue(record1.getReadNegativeStrandFlag());
        assertFalse(record1.getMateNegativeStrandFlag());
        lmp = new SOLiDLongMatePairRecord(record1, 350, 2360);
        assertFalse(lmp.isSameStrand());
    }
    
    @Test
    public void testHandleOrientation () {
        SAMRecord record = records.get(0);
        record.setFlags(145);
        SOLiDLongMatePairRecord lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        lmp.setZPAnnotation("B");
        assertEquals("BB", lmp.handleOrientation());
        
        record = records.get(2);
        record.setFlags(177);
        lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        lmp.setZPAnnotation("B");
        assertEquals("BX", lmp.handleOrientation());
        
        record = records.get(0);
        record.setFlags(161);
        lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        lmp.setZPAnnotation("B");
        assertEquals("BA", lmp.handleOrientation());
              
        
        record = records.get(1);        
        record.setFlags(65);      
        lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        lmp.setZPAnnotation("A");
        assertEquals("AB", lmp.handleOrientation());
        
        
        record = records.get(1);
        record.setFlags(129);
        lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        lmp.setZPAnnotation("A");
        assertEquals("AA", lmp.handleOrientation());
        
        record = records.get(0);
        record.setFlags(145);
        lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        lmp.setZPAnnotation("A");
        assertEquals("AX", lmp.handleOrientation());
        
    }
    
    @Test
    public void testCreateZpAnnotation () {
        SAMRecord record = records.get(1);
        record.setFlags(9);
        record.setAttribute("NH", 1);
        SOLiDLongMatePairRecord lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        lmp.createZPAnnotation();
        assertEquals("D**", lmp.getZPAnnotation());
        
        record = records.get(0);
        record.setFlags(641);
        record.setAttribute("NH", 1);        
        lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        lmp.createZPAnnotation();
        assertEquals("E**", lmp.getZPAnnotation());
        
        record = records.get(0);
        record.setFlags(577);
        record.setAttribute("NH", 1);        
        lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        lmp.createZPAnnotation();
        assertEquals("E**", lmp.getZPAnnotation());
    
        
        record = records.get(2);
        record.setAttribute("NH", null);
        lmp = new SOLiDLongMatePairRecord(record, 350, 2360);
        lmp.createZPAnnotation();    
        assertEquals("Z**", lmp.getZPAnnotation());
    }

}
