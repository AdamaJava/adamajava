package org.qcmg.qsv.discordantpair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.discordantpair.MatePair;
import org.qcmg.qsv.discordantpair.MatePairsWriter;
import org.qcmg.qsv.discordantpair.PairClassification;
import org.qcmg.qsv.discordantpair.PairGroup;
import org.qcmg.qsv.util.TestUtil;

public class MatePairsWriterTest {

    private MatePairsWriter writer;
    private List<MatePair> matePairs;
    private static final String FILE_SEPERATOR = System.getProperty("file.separator");

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        File mateDir = new File(testFolder.getRoot().toString() + FILE_SEPERATOR + PairClassification.AAC + FILE_SEPERATOR);
        mateDir.mkdir();
        writer = new MatePairsWriter(PairClassification.AAC, testFolder.getRoot().toString() + FILE_SEPERATOR, "test", "TD", "lmp");
        matePairs = TestUtil.setupMatePairs();
    }

    @After
    public void tearDown() throws IOException {
        writer = null;
    }

    @Test
    public void testAddNewMatePair() {
        assertEquals(0, writer.getMatePairs().size());
        writer.addNewMatePair(matePairs.get(0));
        assertEquals(1, writer.getMatePairs().size());
    }

    @Test
    public void testWriteMatePairsToFile() throws IOException {
        writer.addNewMatePair(matePairs.get(0));
        writer.writeMatePairsToFile();
        File file = new File(testFolder.getRoot().toString() + FILE_SEPERATOR + "AAC" + FILE_SEPERATOR + "chr7-1_TD_AAC");
        assertTrue(file.exists());
        assertTrue(file.length() > 100);
    }

}
