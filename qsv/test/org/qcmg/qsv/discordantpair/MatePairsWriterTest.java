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
import org.qcmg.qsv.util.TestUtil;

public class MatePairsWriterTest {

    private MatePairsWriter writer;
    private File mateDir;
    private List<MatePair> matePairs;
    private static final String FILE_SEPERATOR = System.getProperty("file.separator");

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        mateDir = testFolder.newFolder(PairClassification.AAC + "" );  
        
        writer = new MatePairsWriter(PairClassification.AAC, testFolder.getRoot().toString()+FILE_SEPERATOR, "TD", "lmp");
        matePairs = TestUtil.setupMatePairs( PairGroup.AAC);
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
        File file = new File(mateDir, "chr7-1_TD_AAC");
        assertTrue(file.exists());
        assertTrue(file.length() > 100);
    }

}
