package org.qcmg.qsv.discordantpair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMFileHeader.SortOrder;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.discordantpair.MatePair;
import org.qcmg.qsv.discordantpair.PairClassification;
import org.qcmg.qsv.util.TestUtil;

public class MatePairTest {
    private List<SAMRecord> records;
    private List<MatePair> pairs;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException, QSVException {
	    	if (null == records || records.isEmpty()) {
	    		records = TestUtil.createSamBodyRecords(SortOrder.unsorted);
	    		pairs = Arrays.asList(
	    				new MatePair(records.get(0), records.get(1)),
	    				new MatePair(records.get(2), records.get(3)),
	    				new MatePair(records.get(4), records.get(5)),
	    				new MatePair(records.get(6), records.get(7)),
	    				new MatePair(records.get(8), records.get(9)),
	    				new MatePair(records.get(10), records.get(11))
	    				);
	    	}
    }

    @Test
    public void testConstructorWithTwoSamRecords() throws QSVException {
        SAMRecord first = records.get(0);
        SAMRecord second = records.get(1);
        MatePair mate = new MatePair(first, second);
        assertEquals("254_166_1407:20110221052813657", mate.getReadName());
        assertEquals("chr7", mate.getLeftMate().getReferenceName());
        assertEquals("254_166_1407:20110221052813657", mate.getLeftMate().getReadName());
        assertEquals(140188379, mate.getLeftMate().getStart());
        assertEquals(140188428, mate.getLeftMate().getEnd());
        assertEquals("chr7", mate.getRightMate().getReferenceName());
        assertEquals("254_166_1407:20110221052813657", mate.getLeftMate().getReadName());
        assertEquals(140191044, mate.getRightMate().getStart());
        assertEquals(140191093, mate.getRightMate().getEnd());
        assertEquals(PairClassification.ABC, mate.getZpType());
    }

    
    @Test
    public void testConstructorWithReadLine() {
    	String line = "254_166_1407:20110221052813657,chr7,140188379,140188428,AAC,129,false,254_166_1407:20110221052813657,chr7,140191044,140191093,AAC,65,false,F2F1";
       	MatePair mate = new MatePair(line);    	
    	assertEquals("254_166_1407:20110221052813657", mate.getReadName());    	
    	assertEquals(PairClassification.AAC, mate.getZpType());
    	assertEquals("+/+", mate.getStrandOrientation());
    	assertEquals("254_166_1407:20110221052813657", mate.getLeftMate().getReadName());
    	assertEquals(140188379, mate.getLeftMate().getStart());
    	assertEquals(140188428, mate.getLeftMate().getEnd());
    	assertEquals(129, mate.getLeftMate().getFlags());
    	assertEquals(false, mate.getLeftMate().getNegOrientation());
    	assertEquals("F2F1", mate.getPairOrder());
    	assertEquals("AAC", mate.getLeftMate().getZp());
    	assertEquals("254_166_1407:20110221052813657", mate.getRightMate().getReadName());
    	assertEquals(140191044, mate.getRightMate().getStart());
    	assertEquals(140191093, mate.getRightMate().getEnd());
    	assertEquals(65, mate.getRightMate().getFlags());
    	assertEquals(false, mate.getRightMate().getNegOrientation());
    	assertEquals("AAC", mate.getRightMate().getZp());
        
    }
      
    @Test
    public void testGetPairClassificationFromSamRecord() throws QSVException, IOException, CloneNotSupportedException {
//        SamReader read = SAMFileReaderFactory.createSAMFileReader(new File(fileName));
//
//        SAMRecordIterator iterator = read.iterator();
        SAMRecord record = (SAMRecord) records.get(0).clone();
        record.setAttribute("ZP", "C**");
        assertEquals("C**", record.getAttribute("ZP"));
        MatePair mate = new MatePair(record, record);
        assertEquals(PairClassification.Cxx, mate.getZpType());
//        read.close();
    }
//    @Test
//    public void testGetPairClassificationFromSamRecord() throws QSVException, IOException {
//    	SamReader read = SAMFileReaderFactory.createSAMFileReader(new File(fileName));
//    	
//    	SAMRecordIterator iterator = read.iterator();
//    	SAMRecord record = iterator.next();
//    	record.setAttribute("ZP", "C**");
//    	assertEquals("C**", record.getAttribute("ZP"));
//    	MatePair mate = new MatePair(record, record);
//    	assertEquals(PairClassification.Cxx, mate.getZpType());
//    	read.close();
//    }
    
    @Test
    public void checkSortOrder() {
        //same chromosome: starts in right order
        MatePair pair = pairs.get(0);
        assertEquals(140188379, pair.getLeftMate().getStart());
        assertEquals(140191044, pair.getRightMate().getStart());
        
        //different chromosomes
        MatePair pair2 = pairs.get(1);
        assertEquals(85925068, pair2.getLeftMate().getStart());
        assertEquals(140188275, pair2.getRightMate().getStart());
        assertEquals("chr4", pair2.getLeftMate().getReferenceName());
        assertEquals("chr7", pair2.getRightMate().getReferenceName());
        
        MatePair pair3 = pairs.get(2);
        assertEquals(140190996, pair3.getLeftMate().getStart());
        assertEquals(6448103, pair3.getRightMate().getStart());
        assertEquals("chr7", pair3.getLeftMate().getReferenceName());
        assertEquals("chrX", pair3.getRightMate().getReferenceName());
        
        MatePair pair4 = pairs.get(3);
        assertEquals(130833637, pair4.getLeftMate().getStart());
        assertEquals(6448103, pair4.getRightMate().getStart());
        assertEquals("chrX", pair4.getLeftMate().getReferenceName());
        assertEquals("chrY", pair4.getRightMate().getReferenceName());     
        
    }
    
    @Test
    public void testToString() {
        
        MatePair p = pairs.get(0);
        String expected = "254_166_1407:20110221052813657,chr7,140188379,140188428,ABC,65,false,254_166_1407:20110221052813657,chr7,140191044,140191093,ABC,129,false,F1F2\n";

        assertEquals(expected, p.toString());
    }
    
    @Test
    public void testToClusterString() {
        
        MatePair p = pairs.get(0);
        //String expected = "254_166_1407:20110221052813657\tchr7\t140188379\t140188428\tAAC\t65\tF1F2\t254_166_1407:20110221052813657\tchr7\t140191044\t140191093\tAAC\t129\tF2F1\n";
        String expected = "254_166_1407:20110221052813657\tchr7\t140188379\t140188428\tABC\t65\t254_166_1407:20110221052813657\tchr7\t140191044\t140191093\tABC\t129\tF1F2\n";

        assertEquals(expected, p.toClusterString());
    }
    
    @Test
    public void testEquals() {
        MatePair p = pairs.get(0);
        
       assertTrue(p.equals(p));
       assertFalse(p.equals(pairs.get(1)));       
    }
    
    @Test
    public void testGetPairOrder() {
        MatePair p = pairs.get(0);
        assertEquals("F1F2", p.getPairOrder());;        
    }
    
    @Test 
    public void hasPairOverlap() {
        MatePair p = pairs.get(0);        
        assertFalse(p.hasPairOverlap());
        
        MatePair p2 = pairs.get(5);        
        assertTrue(p2.hasPairOverlap());
    }    
}
