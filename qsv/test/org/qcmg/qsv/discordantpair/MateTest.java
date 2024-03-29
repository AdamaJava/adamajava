package org.qcmg.qsv.discordantpair;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qsv.util.TestUtil;

import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

public class MateTest {

    private  String fileName;
    private  Mate mate;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void before() throws IOException {
        fileName = testFolder.newFile("test.sam").getCanonicalPath();
        TestUtil.createSamFile(fileName, SortOrder.coordinate, false);
        mate = new Mate("254_166_1407:20110221052813657", "chr7", 140191044, 140191093, "AAC", 129, true);
    }

    @After
    public void after() throws IOException {
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
        mate = null;
    }

    @Test
    public void testConstructor() {
        Mate currentMate = new Mate("254_166_1407:20110221052813657", "chr7", 140191044, 140191093, "AAC", 129, true);
        assertEquals("254_166_1407:20110221052813657", currentMate.getReadName());
        assertEquals(140191044, currentMate.getStart());
        assertEquals(140191093, currentMate.getEnd());
        assertEquals("chr7", currentMate.getReferenceName());
    }

    @Test
    public void testSAMRecordConstructor() throws IOException {
        SamReader read = SAMFileReaderFactory.createSAMFileReader(new File(fileName));
        SAMRecord record = read.iterator().next();
        Mate samMate = new Mate(record, record.getReadGroup().getId());
        read.close();
        assertEquals("254_166_1407:20110221052813657", samMate.getReadName());
        assertEquals(140191044, samMate.getStart());
        assertEquals(140191093, samMate.getEnd());
        assertEquals("chr7", samMate.getReferenceName());

    }

    @Ignore
    public void testSetReadName() {
        assertEquals("254_166_1407:20110221052813657", mate.getReadName());
        new Mate("test", "chr7", 140191044, 140191093, "AAC", 129, true);
        assertEquals("test", mate.getReadName());
    }

    @Ignore
    public void testSetReferenceName() {
        assertEquals("chr7", mate.getReferenceName());
        new Mate("254_166_1407:20110221052813657", "chr8", 140191044, 140191093, "AAC", 129, true);
        assertEquals("chr8", mate.getReferenceName());
    }

    @Ignore
    public void testSetStart() {
        assertEquals(140191044, mate.getStart());
        new Mate("254_166_1407:20110221052813657", "chr7", 1234, 140191093, "AAC", 129, true);
        assertEquals(1234, mate.getStart());
    }

    @Ignore
    public void testSetEnd() {
        assertEquals(140191093, mate.getEnd());
        new Mate("254_166_1407:20110221052813657", "chr7", 140191044, 1234, "AAC", 129, true);
        assertEquals(1234, mate.getEnd());
    }

}
