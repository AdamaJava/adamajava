package org.qcmg.qsv.annotate;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMFileHeader.SortOrder;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qsv.util.TestUtil;

public class IlluminaLongMatePairRecordTest {
    
	private final List<SAMRecord> records = new ArrayList<SAMRecord>();
    
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    
    @Before
    public void setUp() throws IOException {
	    	if (records.isEmpty()) {
	    		File file = TestUtil.createSamFile(testFolder.newFile("test.sam").getAbsolutePath(), SortOrder.unsorted, true);
	        try (final SamReader sam = SAMFileReaderFactory.createSAMFileReader(file)) {//new SAMFileReader(file);
		        for (final SAMRecord samRecord : sam) {
		        		records.add(samRecord);
		        }
	        }
	    	}
    }
    
    @Test    
    public void testHandleOrientation() {
        SAMRecord record = records.getFirst();
        record.setFlags(129);
        IlluminaLongMatePairRecord pe = new IlluminaLongMatePairRecord(record, 350, 2360);
        pe.setZPAnnotation("B");
        
        assertEquals("BA", pe.handleOrientation());
        
        record.setFlags(65);
        pe = new IlluminaLongMatePairRecord(record, 350, 2360);
        pe.setZPAnnotation("B");
        
        assertEquals("BB", pe.handleOrientation());
        
        record.setFlags(17);
        pe = new IlluminaLongMatePairRecord(record, 350, 2360);
        pe.setZPAnnotation("A");
        
        assertEquals("AA", pe.handleOrientation());
        
        record.setFlags(33);
        pe = new IlluminaLongMatePairRecord(record, 350, 2360);
        pe.setZPAnnotation("A");
        
        assertEquals("AB", pe.handleOrientation());
        
    }
    
    @Test
    public void testHandleIntervalSize() {
    	 SAMRecord record = records.getFirst();
         record.setFlags(129);
         IlluminaLongMatePairRecord pe = new IlluminaLongMatePairRecord(record, 350, 2360);
         pe.setZPAnnotation(""); 
         pe.handleIntervalSize();
         
         pe = new IlluminaLongMatePairRecord(record, 350, 13000);
         pe.setZPAnnotation(""); 
         pe.handleIntervalSize();
         assertEquals("A", pe.getZPAnnotation());
         
         pe = new IlluminaLongMatePairRecord(record, 13000, 14000);
         pe.setZPAnnotation(""); 
         pe.handleIntervalSize();
         assertEquals("B", pe.getZPAnnotation());
         
    }
    
    @Test
    public void testIsInward() {
    	SAMRecord record = records.getFirst();
        record.setFlags(33);
        IlluminaLongMatePairRecord pe = new IlluminaLongMatePairRecord(record, 350, 2360);
        assertTrue(pe.isInward());
        assertTrue(pe.isReadForward());
        assertTrue(pe.isMateReverse());
        
        record = records.get(1);
        record.setFlags(17);
        pe = new IlluminaLongMatePairRecord(record, 350, 2360);
        assertTrue(pe.isInward());
        assertTrue(pe.isMateForward());
        assertTrue(pe.isReadReverse());        
    }
    
    @Test
    public void testIsOutward() {
    	SAMRecord record = records.get(1);
        record.setFlags(33);
        IlluminaLongMatePairRecord pe = new IlluminaLongMatePairRecord(record, 350, 2360);
        assertTrue(pe.isOutward());
        assertTrue(pe.isReadForward());
        assertTrue(pe.isMateReverse());
        
        record = records.getFirst();
        record.setFlags(17);
        pe = new IlluminaLongMatePairRecord(record, 350, 2360);
        assertTrue(pe.isOutward());
        assertTrue(pe.isMateForward());
        assertTrue(pe.isReadReverse());        
    }
    
    @Test
    public void testIsDistanceTooSmall() {
    	SAMRecord record = records.get(1);
    	IlluminaLongMatePairRecord pe = new IlluminaLongMatePairRecord(record, 13000, 14000);
    	assertTrue(pe.isDistanceTooSmall());
    }
    
    @Test
    public void testIsDistanceNormal() {
    	SAMRecord record = records.get(1);
   	 	IlluminaLongMatePairRecord pe = new IlluminaLongMatePairRecord(record, 12000, 13000);
   	 	assertTrue(pe.isDistanceNormal());
    }
    
    @Test
    public void testIsDistanceTooLarge() {
    	SAMRecord record = records.get(1);
   	 	IlluminaLongMatePairRecord pe = new IlluminaLongMatePairRecord(record, 11000, 12000);
   	 	assertTrue(pe.isDistanceTooLarge());
    }
    
    @Test
    public void testIsF3toF5() {
    	SAMRecord record = records.get(1);    	
    	record.setFlags(129);
   	 	IlluminaLongMatePairRecord pe = new IlluminaLongMatePairRecord(record, 11000, 12000);
   	 	assertTrue(pe.isF3toF5());
   	 	
   	 	record.setFlags(113);
	 	pe = new IlluminaLongMatePairRecord(record, 11000, 12000);
	 	assertTrue(pe.isF3toF5());
	 	
	 	record = records.getFirst();
	 	record.setFlags(65);
   	 	pe = new IlluminaLongMatePairRecord(record, 11000, 12000);
   	 	assertTrue(pe.isF3toF5());
   	 	
   	 	record.setFlags(177);
	 	pe = new IlluminaLongMatePairRecord(record, 11000, 12000);
	 	assertTrue(pe.isF3toF5());
    }
    
    @Test
    public void testIsF5toF3() {
    	SAMRecord record = records.getFirst();
    	record.setFlags(129);
   	 	IlluminaLongMatePairRecord pe = new IlluminaLongMatePairRecord(record, 11000, 12000);
   	 	assertTrue(pe.isF5toF3());
   	 	
   	 	record.setFlags(113);
	 	pe = new IlluminaLongMatePairRecord(record, 11000, 12000);
	 	assertTrue(pe.isF5toF3());
	 	
	 	record = records.get(1);
	 	record.setFlags(65);
   	 	pe = new IlluminaLongMatePairRecord(record, 11000, 12000);
   	 	assertTrue(pe.isF5toF3());
   	 	
   	 	record.setFlags(177);
	 	pe = new IlluminaLongMatePairRecord(record, 11000, 12000);
	 	assertTrue(pe.isF5toF3());
    }
    
    @Test
    public void testIsSameStrand() {
    	SAMRecord record = records.getFirst();
    	record.setFlags(129);
    	IlluminaLongMatePairRecord pe = new IlluminaLongMatePairRecord(record, 11000, 12000);
    	assertTrue(pe.isSameStrand());
    	
    	record.setFlags(177);
    	pe = new IlluminaLongMatePairRecord(record, 11000, 12000);
    	assertTrue(pe.isSameStrand());
    	
    	record.setFlags(145);
    	pe = new IlluminaLongMatePairRecord(record, 11000, 12000);
    	assertFalse(pe.isSameStrand());
    }
    
    @Test
    public void testIsDifferentStrand() {
    	SAMRecord record = records.getFirst();
    	record.setFlags(129);
    	IlluminaLongMatePairRecord pe = new IlluminaLongMatePairRecord(record, 11000, 12000);
    	assertFalse(pe.isDifferentStrand());
    	
    	record.setFlags(177);
    	pe = new IlluminaLongMatePairRecord(record, 11000, 12000);
    	assertFalse(pe.isDifferentStrand());
    	
    	record.setFlags(145);
    	pe = new IlluminaLongMatePairRecord(record, 11000, 12000);
    	assertTrue(pe.isDifferentStrand());
    }
    
    @Test
    public void testCreateZPAnnotation() {
    	
    	assertZPAnnotation(records.getFirst(), 97, "ABC");
    	assertZPAnnotation(records.get(14), 147, "AAA");
    	assertZPAnnotation(records.getFirst(), 1169, "Z**");
    	assertZPAnnotation(records.get(16), 147, "C**");
    	assertZPAnnotation(records.getFirst(), 9, "D**");
    	assertZPAnnotation(records.getFirst(), 513, "E**");
    }
    
    public void assertZPAnnotation(SAMRecord record, int flags, String annotation) {
    	record.setFlags(flags);
    	IlluminaLongMatePairRecord pe = new IlluminaLongMatePairRecord(record, 120, 5000);
    	pe.createZPAnnotation();
    	assertEquals(annotation, pe.getZPAnnotation());
    }

}
