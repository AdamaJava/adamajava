package org.qcmg.qsv.discordantpair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.discordantpair.MatePair;
import org.qcmg.qsv.discordantpair.MatePairsReader;
import org.qcmg.qsv.discordantpair.PairClassification;
import org.qcmg.qsv.discordantpair.PairGroup;
import org.qcmg.qsv.util.TestUtil;

public class MatePairsReaderTest {

    private MatePairsReader reader;
    private File matePairDir;
    private static final String FILE_SEPERATOR = System.getProperty("file.separator");
    private String fileName = "chr7_test_TD_AAC";

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    
    @Before
    public void setUp() throws IOException, QSVException {
       matePairDir = testFolder.newFolder("matepair");
       TestUtil.createTmpClusterFile(matePairDir.getAbsolutePath() + FILE_SEPERATOR, PairClassification.AAC, fileName);
       reader = new MatePairsReader(PairGroup.AAC, matePairDir.getAbsolutePath() + FILE_SEPERATOR, fileName, "TD");
   	   assertNotNull(reader.getFilesToRead());
   	   assertNotNull(reader.getFilesToRead().get("chr7"));
   	   assertEquals(1, reader.getFilesToRead().get("chr7").size());
   	   assertEquals(fileName, reader.getFilesToRead().get("chr7").get(0).getName()); 
    }
    
    @After
    public void tearDown() throws IOException, QSVException {
       if (matePairDir.exists()) {
    	   matePairDir.delete();
       }
    }    

    @Test
    public void testReadInPairs() throws Exception {
        List<File> files = new ArrayList<File>();
        files.add(new File(matePairDir.getAbsolutePath() + FILE_SEPERATOR + "AAC" + FILE_SEPERATOR + fileName));
        List<MatePair> list = reader.getMatePairsListByFiles(files, true);

       assertEquals(6, list.size());
       assertEquals("1789_1456_806:20110221052813657", list.get(0).getReadName());
       assertEquals("1789_1456_806:20110221052813657", list.get(0).getReadName());
    }

}
