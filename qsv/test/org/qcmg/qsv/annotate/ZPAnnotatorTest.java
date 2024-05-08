package org.qcmg.qsv.annotate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.TestUtil;

import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

public class ZPAnnotatorTest {
    
    
    private final List<SAMRecord> records = new ArrayList<SAMRecord>();
    
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    
    @Before
    public void setUp() throws IOException {
		if (records.isEmpty()) {
    		File file = TestUtil.createSamFile(testFolder.newFile("test.sam").getAbsolutePath(), SortOrder.unsorted, true);
	        try (final SamReader sam = SAMFileReaderFactory.createSAMFileReader(file)){//new SAMFileReader(file);) {
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
        assertEquals("BB", ZPAnnotator.handleOrientation(record, "B"));
        
        record.setFlags(65);
        assertEquals("BA", ZPAnnotator.handleOrientation(record, "B"));
        
        record.setFlags(17);
        assertEquals("AB", ZPAnnotator.handleOrientation(record, "A"));
        
        record.setFlags(33);
        assertEquals("AA", ZPAnnotator.handleOrientation(record, "A"));
    }
    
    @Test
    public void testHandleIntervalSize() {
    	SAMRecord record = records.getFirst();
        record.setFlags(129);
        assertEquals("C", ZPAnnotator.handleIntervalSize(record, 350, 2360));
        assertEquals("A", ZPAnnotator.handleIntervalSize(record, 350, 13000));
        assertEquals("B", ZPAnnotator.handleIntervalSize(record, 13000, 14000));
    }
    
    @Test
    public void testIsInward() {
		SAMRecord record = records.getFirst();
        record.setFlags(33);
        assertTrue(ZPAnnotator.isInward(record));
        assertTrue(ZPAnnotator.isReadForward(record));
        assertTrue(ZPAnnotator.isMateReverse(record));
        
        record = records.get(1);
        record.setFlags(17);
        assertTrue(ZPAnnotator.isInward(record));
        assertTrue(ZPAnnotator.isMateForward(record));
        assertTrue(ZPAnnotator.isReadReverse(record));        
    }
    
    @Test
    public void testIsOutward() {
    	SAMRecord record = records.get(1);
        record.setFlags(33);
        assertTrue(ZPAnnotator.isOutward(record));
        assertTrue(ZPAnnotator.isReadForward(record));
        assertTrue(ZPAnnotator.isMateReverse(record));
        
        record = records.getFirst();
        record.setFlags(17);
        assertTrue(ZPAnnotator.isOutward(record));
        assertTrue(ZPAnnotator.isMateForward(record));
        assertTrue(ZPAnnotator.isReadReverse(record));
    }
    
    @Test
    public void testIsDistanceTooSmall() {
    	SAMRecord record = records.get(1);
    	assertTrue(ZPAnnotator.isDistanceTooSmall(Math.abs(record.getInferredInsertSize()), 13000));
    }
    
    @Test
    public void testIsDistanceNormal() {
		SAMRecord record = records.get(1);
   	 	assertTrue(ZPAnnotator.isDistanceNormal(Math.abs(record.getInferredInsertSize()), 12000, 13000));
    }
    
    @Test
    public void testIsDistanceTooLarge() {
		SAMRecord record = records.get(1);
   	 	assertTrue(ZPAnnotator.isDistanceTooLarge(Math.abs(record.getInferredInsertSize()), 12000));
    }
    
    @Test
    public void testIsF3toF5() {
    	SAMRecord record = records.get(1);    	
    	record.setFlags(129);
    	assertTrue(ZPAnnotator.isF3toF5(record));
    	
   	 	record.setFlags(113);
	 	assertTrue(ZPAnnotator.isF3toF5(record));
	 	
	 	record = records.getFirst();
	 	record.setFlags(65);
   	 	assertTrue(ZPAnnotator.isF3toF5(record));
   	 	
   	 	record.setFlags(177);
	 	assertTrue(ZPAnnotator.isF3toF5(record));
    }
    
    @Test
    public void testIsF5toF3() {
    	SAMRecord record = records.getFirst();
    	record.setFlags(129);
   	 	assertTrue(ZPAnnotator.isF5toF3(record));
   	 	
   	 	record.setFlags(113);
	 	assertTrue(ZPAnnotator.isF5toF3(record));
	 	
	 	record = records.get(1);
	 	record.setFlags(65);
	 	assertTrue(ZPAnnotator.isF5toF3(record));
   	 	
   	 	record.setFlags(177);
	 	assertTrue(ZPAnnotator.isF5toF3(record));
    }
    
    @Test
    public void testIsSameStrand() {
    	SAMRecord record = records.getFirst();
    	record.setFlags(129);
    	assertTrue(ZPAnnotator.isSameStrand(record));
    	
    	record.setFlags(177);
    	assertTrue(ZPAnnotator.isSameStrand(record));
    	
    	record.setFlags(145);
    	assertFalse(ZPAnnotator.isSameStrand(record));
    }
    
    @Test
    public void testIsDifferentStrand() {
    	SAMRecord record = records.getFirst();
    	record.setFlags(129);
    	assertFalse(ZPAnnotator.isDifferentStrand(record));
    	
    	record.setFlags(177);
    	assertFalse(ZPAnnotator.isDifferentStrand(record));
    	
    	record.setFlags(145);
    	assertTrue(ZPAnnotator.isDifferentStrand(record));
    }
    
    @Test
    public void createZPAnnotationAAA() {
		// read is left of mate, read is forward, mate is reverse && normal distance
		SAMRecord rec = records.getFirst();
		rec.setAttribute(QSVConstants.NH, 1);
		rec.setInferredInsertSize(1000);
		assertZPAnnotation(rec, 35, QSVConstants.AAA);
		rec.setAttribute(QSVConstants.NH, 0);
		assertZPAnnotation(rec, 35, QSVConstants.AAA);
		
		// alternatively, NH is null, and we are on dif strands
		rec.setAttribute(QSVConstants.NH, null);
		assertZPAnnotation(rec, 35, QSVConstants.AAA);
    }
    
    @Test
    public void createZPAnnotationB() {
    	// same strand read is left of mate, read is forward, mate is reverse && normal distance
    	SAMRecord rec = records.getFirst();
    	rec.setAttribute(QSVConstants.NH, 1);
    	rec.setInferredInsertSize(119);
    	assertZPAnnotation(rec, 131, "BBB");
    	
    }
    
    @Test
    public void createZPAnnotation() {
		SAMRecord rec = records.getFirst();
		assertZPAnnotation(rec, 1024, QSVConstants.Z_STAR_STAR);
		
		// set NH attribute
		rec.setAttribute(QSVConstants.NH, 1);
		assertZPAnnotation(rec, 1024, QSVConstants.Z_STAR_STAR);
		
		assertZPAnnotation(rec, 11, QSVConstants.D_STAR_STAR);
		assertZPAnnotation(rec, 515, QSVConstants.E_STAR_STAR);
		
		// set NH to zero and set mate to diff strand
		rec.setAttribute(QSVConstants.NH, 0);
		assertZPAnnotation(rec, 3, QSVConstants.Z_STAR_STAR);
		rec.setAttribute(QSVConstants.NH, null);
		assertZPAnnotation(rec, 3, QSVConstants.Z_STAR_STAR);
		assertZPAnnotation(rec, 19, QSVConstants.Z_STAR_STAR);
    }
    
    @Test
    public void testCreateZPAnnotation() {
    	assertZPAnnotation(records.getFirst(), 97, "AAC");
    	assertZPAnnotation(records.get(14), 147, "ABA");
    	assertZPAnnotation(records.getFirst(), 1169, QSVConstants.Z_STAR_STAR);
    	assertZPAnnotation(records.get(16), 147, "C**");
    	assertZPAnnotation(records.getFirst(), 9, "D**");
    	assertZPAnnotation(records.getFirst(), 513, "E**");
    }
    
    public void assertZPAnnotation(SAMRecord record, int flags, String annotation) {
    	record.setFlags(flags);
    	assertEquals(annotation, ZPAnnotator.createZPAnnotation(record, 120, 5000));
    }
}
