package org.qcmg.qsv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import htsjdk.samtools.SAMFileHeader.SortOrder;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.util.TestUtil;

public class QSVParametersTest {

    private File normalBam;
    private File tumorBam;
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    
    @Before
    public void setUp() throws IOException {
    	if (null == normalBam) {
    		normalBam = TestUtil.createSamFile(testFolder.newFile("normalBam.sam").getAbsolutePath(), SortOrder.coordinate, false);
    	}
    	if (null == tumorBam) {
    		tumorBam = TestUtil.createSamFile(testFolder.newFile("tumorBam.sam").getAbsolutePath(), SortOrder.coordinate, false);
    	}
    }
    
    @Test
    public void testParametersSetUpForNormal() throws Exception {    	
    	
        QSVParameters p =  TestUtil.getQSVParameters(testFolder.getRoot(), normalBam.getAbsolutePath(),
        		tumorBam.getAbsolutePath(), false, "both", "both");       
        assertEquals(normalBam, p.getInputBamFile());
        assertEquals("normalBam.discordantpair.filtered.sam", TestUtil.getFilename(p.getFilteredBamFile()));    
        assertEquals("ND", p.getFindType());
        assertEquals(3, p.getClusterSize().intValue());
        assertEquals(640, p.getLowerInsertSize().intValue());
        assertEquals(2360, p.getUpperInsertSize().intValue());
        assertEquals("ICGC-DBLG-20110506-01-ND", p.getSampleId());
        assertEquals(p.getAverageInsertSize(), 1500);
        assertFalse(p.isTumor());
    }

    @Test
    public void testParametersSetUpForTumor() throws Exception {
        QSVParameters p = TestUtil.getQSVParameters(testFolder.getRoot(), normalBam.getAbsolutePath(),       		
                tumorBam.getAbsolutePath(), true, "both", "both");
        
        assertEquals(tumorBam, p.getInputBamFile());
        assertEquals("tumorBam.discordantpair.filtered.sam",
                TestUtil.getFilename(p.getFilteredBamFile()));
        assertEquals("TD", p.getFindType());
        assertEquals(3, p.getClusterSize().intValue());
        assertEquals(2360, p.getUpperInsertSize().intValue());
        assertEquals(640, p.getLowerInsertSize().intValue());
        assertEquals(1500, p.getAverageInsertSize());
        assertEquals("ICGC-DBLG-20110506-01-TD", p.getSampleId());
        assertTrue(p.isTumor());
    }

    @Test
    public void testGetUpdatedFileName() {
        // Given
        String fileName1 = "file.sam";
        String fileName2 = "file.bam";
        String fileName3 = "file.cram";
        String fileName4 = "file.file";
        String extension = ".new";

        // When
        String updatedName1 = QSVParameters.getUpdatedFileName(fileName1, extension);
        String updatedName2 = QSVParameters.getUpdatedFileName(fileName2, extension);
        String updatedName3 = QSVParameters.getUpdatedFileName(fileName3, extension);
        String updatedName4 = QSVParameters.getUpdatedFileName(fileName4, extension);

        // Then
        assertEquals("file" + extension + ".sam", updatedName1);
        assertEquals("file" + extension + ".bam", updatedName2);
        assertEquals("file" + extension + ".cram", updatedName3);
        assertEquals("file.file" + extension, updatedName4);
    }
}
