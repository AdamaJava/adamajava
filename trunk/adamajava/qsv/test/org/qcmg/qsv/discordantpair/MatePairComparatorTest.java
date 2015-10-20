package org.qcmg.qsv.discordantpair;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMFileHeader.SortOrder;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.discordantpair.MatePair;
import org.qcmg.qsv.discordantpair.PairGroup;
import org.qcmg.qsv.util.TestUtil;

public class MatePairComparatorTest {
	
	private List<SAMRecord> records;
    private List<MatePair> pairs;
    private String fileName;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void before() throws IOException, QSVException {
        records = new ArrayList<SAMRecord>();
        pairs = new ArrayList<MatePair>();
        fileName = testFolder.newFile("test.bam").getCanonicalPath();
        TestUtil.createSamFile(fileName, PairGroup.AAC, SortOrder.coordinate, false);
        SamReader read = SAMFileReaderFactory.createSAMFileReader(new File(fileName));//new SAMFileReader(new File(fileName));
        
        for (SAMRecord r : read) {
            records.add(r);
        }
        read.close();
        
        for (int i=0; i<16; i+=2) {
        	 pairs.add(new MatePair(records.get(i), records.get(i+1)));
        	
        }       
    }

    @After
    public void after() throws IOException {
        records.clear();
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
    }
	
	@Test
    public void testSortByLeftReadStartComparator() {	  
      
        assertEquals(140188379, pairs.get(0).getLeftMate().getStart());
        assertEquals(140189059, pairs.get(7).getLeftMate().getStart());
        Collections.sort(pairs, new MatePair.ReadMateLeftStartComparator());

        assertEquals(140188227, pairs.get(0).getLeftMate().getStart());
        assertEquals(140189059, pairs.get(7).getLeftMate().getStart());
    }
	
    @Test
    public void testSortByLeftReadEndComparator() {
    	assertEquals(140188428, pairs.get(0).getLeftMate().getEnd());
        assertEquals(140189108, pairs.get(7).getLeftMate().getEnd());
        Collections.sort(pairs, new MatePair.ReadMateLeftEndComparator());

        assertEquals(140188276, pairs.get(0).getLeftMate().getEnd());
        assertEquals(140189108, pairs.get(7).getLeftMate().getEnd());

    }
    
	@Test
    public void testSortByRightReadStartComparator() {	  
      
        assertEquals(140191044, pairs.get(0).getRightMate().getStart());
        assertEquals(140191509, pairs.get(7).getRightMate().getStart());
        Collections.sort(pairs, new MatePair.ReadMateRightStartComparator());

        assertEquals(140191044, pairs.get(0).getRightMate().getStart());
        assertEquals(140191611, pairs.get(7).getRightMate().getStart());
    }
	
    @Test
    public void testSortByRightReadEndComparator() {
    	assertEquals(140191093, pairs.get(0).getRightMate().getEnd());
        assertEquals(140191558, pairs.get(7).getRightMate().getEnd());
        Collections.sort(pairs, new MatePair.ReadMateRightEndComparator());

        assertEquals(140191093, pairs.get(0).getRightMate().getEnd());
        assertEquals(140191660, pairs.get(7).getRightMate().getEnd());

    }
	
}
