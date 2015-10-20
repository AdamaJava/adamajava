package org.qcmg.qsv;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.Date;
import java.util.UUID;

import htsjdk.samtools.SAMFileHeader.SortOrder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.meta.QExec;
import org.qcmg.qsv.discordantpair.PairGroup;
import org.qcmg.qsv.util.TestUtil;

public class QSVPipelineTest {

    private QSVPipeline pipeline;
    private static final String FILE_SEPERATOR = System.getProperty("file.separator");

    @Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
   
    @Test
    public void testCreateOutputDirectory() throws Exception {
    	String filteredNormalBam = testFolder.newFile("normalBam_filtered.bam").getAbsolutePath();
    	String filteredTumorBam = testFolder.newFile("tumorBam_filtered.bam").getAbsolutePath();
    	TestUtil.createBamFile(filteredNormalBam, PairGroup.AAC, SortOrder.queryname);
		TestUtil.createBamFile(testFolder.newFile("normalBam_filtered.bam").getAbsolutePath(), PairGroup.AAC, SortOrder.queryname);
        String[] optionsArray = TestUtil.getValidOptions(testFolder, filteredNormalBam, filteredTumorBam, "both", "both");
        Options options = new Options(optionsArray);
        options.parseIniFile();
        String uuid = UUID.randomUUID().toString();
        pipeline = new QSVPipeline(options, testFolder.getRoot().getAbsolutePath()  + FILE_SEPERATOR, new Date(), uuid, new QExec("qsv", "test", null, uuid));
        String name = testFolder.getRoot().getAbsolutePath() + FILE_SEPERATOR + "test";
        pipeline.createOutputDirectory(name);        
        testFolder.delete();
        try {
            pipeline.createOutputDirectory(name);
        } catch (QSVException e) {
            assertFalse(new File(name).exists());
        }
    }
    
    
}
