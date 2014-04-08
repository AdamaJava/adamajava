package org.qcmg.qsv.discordantpair;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMFileHeader.SortOrder;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.discordantpair.Mate;
import org.qcmg.qsv.util.TestUtil;

public class MateTest {

    private static String fileName;
    private static Mate mate;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void before() throws IOException {
        fileName = testFolder.newFile("test.bam").getCanonicalPath();
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
    public void testSAMRecordConstructor() {
        SAMFileReader read = new SAMFileReader(new File(fileName));
        SAMRecord record = read.iterator().next();
        Mate samMate = new Mate(record);
        read.close();
        assertEquals("254_166_1407:20110221052813657", samMate.getReadName());
        assertEquals(140191044, samMate.getStart());
        assertEquals(140191093, samMate.getEnd());
        assertEquals("chr7", samMate.getReferenceName());

    }

    @Test
    public void testSetReadName() {
        assertEquals("254_166_1407:20110221052813657", mate.getReadName());
        mate.setReadName("test");
        assertEquals("test", mate.getReadName());
    }

    @Test
    public void testSetReferenceName() {
        assertEquals("chr7", mate.getReferenceName());
        mate.setReferenceName("chr8");
        assertEquals("chr8", mate.getReferenceName());
    }

    @Test
    public void testSetStart() {
        assertEquals(140191044, mate.getStart());
        mate.setStart(1234);
        assertEquals(1234, mate.getStart());
    }

    @Test
    public void testSetEnd() {
        assertEquals(140191093, mate.getEnd());
        mate.setEnd(1234);
        assertEquals(1234, mate.getEnd());
    }

}
