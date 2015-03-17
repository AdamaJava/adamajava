package org.qcmg.qsv.annotate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMFileHeader.SortOrder;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.TestUtil;

public class PairedEndRecordTest {
    
    
    private final List<SAMRecord> records = new ArrayList<SAMRecord>();
    
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    
    @Before
    public void setUp() throws IOException {
    		File file = TestUtil.createSamFile(testFolder.newFile("test.bam").getAbsolutePath(), SortOrder.unsorted, true);
        final SAMFileReader sam = new SAMFileReader(file);
        for (final SAMRecord samRecord : sam) {
        		records.add(samRecord);
        }
        sam.close();
    }
    
    @After
    public void after() throws IOException {
    		records.clear();
    }   
    
    
    @Test    
    public void testHandleOrientation() {
        SAMRecord record = records.get(0);
        record.setFlags(129);
        PairedEndRecord pe = new PairedEndRecord(record, 350, 2360);
        pe.setZPAnnotation("B");
        
        assertEquals("BB", pe.handleOrientation());
        
        record.setFlags(65);
        pe = new PairedEndRecord(record, 350, 2360);
        pe.setZPAnnotation("B");
        
        assertEquals("BA", pe.handleOrientation());
        
        record.setFlags(17);
        pe = new PairedEndRecord(record, 350, 2360);
        pe.setZPAnnotation("A");
        
        assertEquals("AB", pe.handleOrientation());
        
        record.setFlags(33);
        pe = new PairedEndRecord(record, 350, 2360);
        pe.setZPAnnotation("A");
        
        assertEquals("AA", pe.handleOrientation());
        
    }
    
    @Test
    public void testHandleIntervalSize() {
    	 SAMRecord record = records.get(0);
         record.setFlags(129);
         PairedEndRecord pe = new PairedEndRecord(record, 350, 2360);
         pe.setZPAnnotation(""); 
         pe.handleIntervalSize();
         
         pe = new PairedEndRecord(record, 350, 13000);
         pe.setZPAnnotation(""); 
         pe.handleIntervalSize();
         assertEquals("A", pe.getZPAnnotation());
         
         pe = new PairedEndRecord(record, 13000, 14000);
         pe.setZPAnnotation(""); 
         pe.handleIntervalSize();
         assertEquals("B", pe.getZPAnnotation());
         
    }
    
    @Test
    public void testIsInward() {
    	SAMRecord record = records.get(0);
        record.setFlags(33);
        PairedEndRecord pe = new PairedEndRecord(record, 350, 2360);
        assertTrue(pe.isInward());
        assertTrue(pe.isReadForward());
        assertTrue(pe.isMateReverse());
        
        record = records.get(1);
        record.setFlags(17);
        pe = new PairedEndRecord(record, 350, 2360);
        assertTrue(pe.isInward());
        assertTrue(pe.isMateForward());
        assertTrue(pe.isReadReverse());        
    }
    
    @Test
    public void testIsOutward() {
    	SAMRecord record = records.get(1);
        record.setFlags(33);
        PairedEndRecord pe = new PairedEndRecord(record, 350, 2360);
        assertTrue(pe.isOutward());
        assertTrue(pe.isReadForward());
        assertTrue(pe.isMateReverse());
        
        record = records.get(0);
        record.setFlags(17);
        pe = new PairedEndRecord(record, 350, 2360);
        assertTrue(pe.isOutward());
        assertTrue(pe.isMateForward());
        assertTrue(pe.isReadReverse());        
    }
    
    @Test
    public void testIsDistanceTooSmall() {
    	SAMRecord record = records.get(1);
    	PairedEndRecord pe = new PairedEndRecord(record, 13000, 14000);
    	assertTrue(pe.isDistanceTooSmall());
    }
    
    @Test
    public void testIsDistanceNormal() {
    	SAMRecord record = records.get(1);
   	 	PairedEndRecord pe = new PairedEndRecord(record, 12000, 13000);
   	 	assertTrue(pe.isDistanceNormal());
    }
    
    @Test
    public void testIsDistanceTooLarge() {
    	SAMRecord record = records.get(1);
   	 	PairedEndRecord pe = new PairedEndRecord(record, 11000, 12000);
   	 	assertTrue(pe.isDistanceTooLarge());
    }
    
    @Test
    public void testIsF3toF5() {
    	SAMRecord record = records.get(1);    	
    	record.setFlags(129);
   	 	PairedEndRecord pe = new PairedEndRecord(record, 11000, 12000);
   	 	assertTrue(pe.isF3toF5());
   	 	
   	 	record.setFlags(113);
	 	pe = new PairedEndRecord(record, 11000, 12000);
	 	assertTrue(pe.isF3toF5());
	 	
	 	record = records.get(0);
	 	record.setFlags(65);
   	 	pe = new PairedEndRecord(record, 11000, 12000);
   	 	assertTrue(pe.isF3toF5());
   	 	
   	 	record.setFlags(177);
	 	pe = new PairedEndRecord(record, 11000, 12000);
	 	assertTrue(pe.isF3toF5());
    }
    
    @Test
    public void testIsF5toF3() {
    	SAMRecord record = records.get(0);    	
    	record.setFlags(129);
   	 	PairedEndRecord pe = new PairedEndRecord(record, 11000, 12000);
   	 	assertTrue(pe.isF5toF3());
   	 	
   	 	record.setFlags(113);
	 	pe = new PairedEndRecord(record, 11000, 12000);
	 	assertTrue(pe.isF5toF3());
	 	
	 	record = records.get(1);
	 	record.setFlags(65);
   	 	pe = new PairedEndRecord(record, 11000, 12000);
   	 	assertTrue(pe.isF5toF3());
   	 	
   	 	record.setFlags(177);
	 	pe = new PairedEndRecord(record, 11000, 12000);
	 	assertTrue(pe.isF5toF3());
    }
    
    @Test
    public void testIsSameStrand() {
    	SAMRecord record = records.get(0);  
    	record.setFlags(129);
    	PairedEndRecord pe = new PairedEndRecord(record, 11000, 12000);
    	assertTrue(pe.isSameStrand());
    	
    	record.setFlags(177);
    	pe = new PairedEndRecord(record, 11000, 12000);
    	assertTrue(pe.isSameStrand());
    	
    	record.setFlags(145);
    	pe = new PairedEndRecord(record, 11000, 12000);
    	assertFalse(pe.isSameStrand());
    }
    
    @Test
    public void testIsDifferentStrand() {
    	SAMRecord record = records.get(0);  
    	record.setFlags(129);
    	PairedEndRecord pe = new PairedEndRecord(record, 11000, 12000);
    	assertFalse(pe.isDifferentStrand());
    	
    	record.setFlags(177);
    	pe = new PairedEndRecord(record, 11000, 12000);
    	assertFalse(pe.isDifferentStrand());
    	
    	record.setFlags(145);
    	pe = new PairedEndRecord(record, 11000, 12000);
    	assertTrue(pe.isDifferentStrand());
    }
    
    @Test
    public void createZPAnnotationAAA() {
    		// read is left of mate, read is forward, mate is reverse && normal distance
    		SAMRecord rec = records.get(0);
//    		rec.setReadNegativeStrandFlag(true);
//    		rec.setMateNegativeStrandFlag(false);
    		rec.setAttribute("NH", Integer.valueOf(1));
    		rec.setInferredInsertSize(1000);
    		assertZPAnnotation(rec, 35, QSVConstants.AAA);
    		rec.setAttribute("NH", Integer.valueOf(0));
    		assertZPAnnotation(rec, 35, QSVConstants.AAA);
    		
    		// alternatively, NH is null, and we are on dif strands
    		rec.setAttribute("NH", null);
    		assertZPAnnotation(rec, 35, QSVConstants.AAA);
    }
    
    @Test
    public void createZPAnnotationB() {
    	// same strandread is left of mate, read is forward, mate is reverse && normal distance
    	SAMRecord rec = records.get(0);
//    		rec.setReadNegativeStrandFlag(true);
//    		rec.setMateNegativeStrandFlag(false);
    	rec.setAttribute("NH", Integer.valueOf(1));
    	rec.setInferredInsertSize(119);
    	assertZPAnnotation(rec, 131, "BBB");
    	
    }
    
    @Test
    public void createZPAnnotation() {
    		SAMRecord rec = records.get(0);
//    		rec.setDuplicateReadFlag(true);
    		assertZPAnnotation(rec, 1024, QSVConstants.Z_STAR_STAR);
    		
    		// set NH attribute
    		rec.setAttribute("NH", Integer.valueOf(1));
    		assertZPAnnotation(rec, 1024, QSVConstants.Z_STAR_STAR);
    		
    		assertZPAnnotation(rec, 11, QSVConstants.D_STAR_STAR);
    		assertZPAnnotation(rec, 515, QSVConstants.E_STAR_STAR);
    		
    		// set NH to zero and set mate to diff strand
    		rec.setAttribute("NH", Integer.valueOf(0));
//    		rec.setReadNegativeStrandFlag(false);
//    		rec.setMateNegativeStrandFlag(true);
    		assertZPAnnotation(rec, 3, QSVConstants.Z_STAR_STAR);
    		rec.setAttribute("NH", null);
    		assertZPAnnotation(rec, 3, QSVConstants.Z_STAR_STAR);
    		assertZPAnnotation(rec, 19, QSVConstants.Z_STAR_STAR);
    }
    
    @Test
    public void testCreateZPAnnotation() {
    	
    	assertZPAnnotation(records.get(0), 97, "AAC");
    	assertZPAnnotation(records.get(14), 147, "ABA");
    	assertZPAnnotation(records.get(0), 1169, QSVConstants.Z_STAR_STAR);
    	assertZPAnnotation(records.get(16), 147, "C**");
    	assertZPAnnotation(records.get(0), 9, "D**");
    	assertZPAnnotation(records.get(0), 513, "E**");    	
    }
    
    public void assertZPAnnotation(SAMRecord record, int flags, String annotation) {
    	record.setFlags(flags);
    	PairedEndRecord pe = new PairedEndRecord(record, 120, 5000);
    	pe.createZPAnnotation();
    	assertEquals(annotation, pe.getZPAnnotation());
    }
    
    

}
