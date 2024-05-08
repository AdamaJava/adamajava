package org.qcmg.qsv;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.nio.file.FileSystems;
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
    private static final String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();

    @Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
   
    @Test
    public void testCreateOutputDirectory() throws Exception {
    	File tmp = testFolder.newFolder();
    	File fnormal = new File(tmp, "normalBam_filtered.bam");   	 
    	File ftumor = new File(tmp, "tumorBam_filtered.bam");   
    	TestUtil.createBamFile(fnormal.getAbsolutePath(), PairGroup.AAC, SortOrder.queryname);
		TestUtil.createBamFile(ftumor.getAbsolutePath(), PairGroup.AAC, SortOrder.queryname);
        String[] optionsArray = TestUtil.getValidOptions( testFolder.getRoot(), fnormal.getAbsolutePath(), ftumor.getAbsolutePath(), "both", "both");
        Options options = new Options(optionsArray);
        options.parseIniFile();
        String uuid = UUID.randomUUID().toString();
        pipeline = new QSVPipeline(options, tmp.getAbsolutePath()  + FILE_SEPARATOR, new Date(), uuid, new QExec("qsv", "test", null, uuid));
        String name = tmp.getAbsolutePath() + FILE_SEPARATOR + "test";
        pipeline.createOutputDirectory(name);        
        testFolder.delete();
        try {
            pipeline.createOutputDirectory(name);
        } catch (QSVException e) {
            assertFalse(new File(name).exists());
        }
    }
    
    
}
