package org.qcmg.qsv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.samtools.SAMFileHeader.SortOrder;

import org.junit.After;
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
    	normalBam = TestUtil.createSamFile(testFolder.newFile("normalBam.bam").getAbsolutePath(), SortOrder.coordinate, false);
    	tumorBam = TestUtil.createSamFile(testFolder.newFile("tumorBam.bam").getAbsolutePath(), SortOrder.coordinate, false);
    }
    
    @After
    public void tearDown() {
    	if (normalBam.exists()) {
    		normalBam.delete();
    	}
    	
    	if (tumorBam.exists()) {
    		tumorBam.delete();
    	}
    }

    @Test
    public void testParametersSetUpForNormal() throws Exception {    	
    	
        QSVParameters p =  TestUtil.getQSVParameters(testFolder, normalBam.getAbsolutePath(),
        		tumorBam.getAbsolutePath(), false, "both", "both");       
        assertEquals(normalBam, p.getInputBamFile());
        assertEquals("normalBam.discordantpair.filtered.bam",
                TestUtil.getFilename(p.getFilteredBamFile()));    
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
    	File normalBam = testFolder.newFile("normalBam.bam");
    	File tumorBam = testFolder.newFile("tumorBam.bam");
        QSVParameters p = TestUtil.getQSVParameters(testFolder, normalBam.getAbsolutePath(),
                tumorBam.getAbsolutePath(), true, "both", "both");
        
        assertEquals(tumorBam, p.getInputBamFile());
        assertEquals("tumorBam.discordantpair.filtered.bam",
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
    public void testMapVsList() {
    		String rg ="20150316155612345"; 	
    		List<String> rgList = new ArrayList<>();
    		rgList.add("20150316155612345");
    		
    		// perform a million contains and time
    		long start = System.currentTimeMillis();
    		int counter = 10000000;
    		int count = 0;
    		for (int i = 0 ; i < counter ; i++) {
    			if (rgList.contains(rg)) {
    				count++;
    			}
    		}
    		System.out.println("list done in " + (System.currentTimeMillis() - start) + "ms with count: " + count);
    		
    		Set<String> rgMap = new HashSet<>(2);
    		rgMap.add("20150316155612345");
    		start = System.currentTimeMillis();
    		count = 0;
    		for (int i = 0 ; i < counter ; i++) {
    			if (rgMap.contains(rg)) {
    				count++;
    			}
    		}
    		System.out.println("set done in " + (System.currentTimeMillis() - start) + "ms with count: " + count);
    		
    		start = System.currentTimeMillis();
    		count = 0;
    		for (int i = 0 ; i < counter ; i++) {
    			if (rgList.contains(rg)) {
    				count++;
    			}
    		}
    		System.out.println("list done in " + (System.currentTimeMillis() - start) + "ms with count: " + count);
    }
    
    @Test
    public void testMapVsList2() {
    	String rg ="20150316155912345"; 	
    	String rg2 ="20150316155912345"; 	
    	List<String> rgList = new ArrayList<>();
    	rgList.add("20150316155612345");
    	rgList.add("20150316155912345");
    	
    	// perform a million contains and time
    	long start = System.currentTimeMillis();
    	int counter = 10000000;
    	int count = 0;
    	for (int i = 0 ; i < counter ; i++) {
    		if (rgList.contains(rg)) {
    			count++;
    		}
    	}
    	System.out.println("2 list done in " + (System.currentTimeMillis() - start) + "ms with count: " + count);
    	
    	Set<String> rgMap = new HashSet<>(4);
    	rgMap.add("20150316155612345");
    	rgMap.add("20150316155912345");
    	start = System.currentTimeMillis();
    	count = 0;
    	for (int i = 0 ; i < counter ; i++) {
    		if (rgMap.contains(rg)) {
    			count++;
    		}
    	}
    	System.out.println("2 set done in " + (System.currentTimeMillis() - start) + "ms with count: " + count);
    	
    	start = System.currentTimeMillis();
    	count = 0;
    	for (int i = 0 ; i < counter ; i++) {
    		if (rgList.contains(rg)) {
    			count++;
    		}
    	}
    	System.out.println("2 list done in " + (System.currentTimeMillis() - start) + "ms with count: " + count);
    	
    }
    @Test
    public void testMapVsList5() {
    	String rg ="20150316155912345"; 	
    	String rg2 ="20150316155912345"; 	
    	List<String> rgList = new ArrayList<>();
    	rgList.add("20150316155612345");
    	rgList.add("20150316155912345");
    	rgList.add("20150316156912345");
    	rgList.add("20150316157912345");
    	rgList.add("20150316158912345");
    	
    	// perform a million contains and time
    	long start = System.currentTimeMillis();
    	int counter = 10000000;
    	int count = 0;
    	for (int i = 0 ; i < counter ; i++) {
    		if (rgList.contains(rg)) {
    			count++;
    		}
    	}
    	System.out.println("5 list done in " + (System.currentTimeMillis() - start) + "ms with count: " + count);
    	
    	Set<String> rgMap = new HashSet<>(8);
    	rgMap.add("20150316155612345");
    	rgMap.add("20150316155912345");
    	rgMap.add("20150316156912345");
    	rgMap.add("20150316157912345");
    	rgMap.add("20150316158912345");
    	start = System.currentTimeMillis();
    	count = 0;
    	for (int i = 0 ; i < counter ; i++) {
    		if (rgMap.contains(rg)) {
    			count++;
    		}
    	}
    	System.out.println("5 set done in " + (System.currentTimeMillis() - start) + "ms with count: " + count);
    	
    	start = System.currentTimeMillis();
    	count = 0;
    	for (int i = 0 ; i < counter ; i++) {
    		if (rgList.contains(rg)) {
    			count++;
    		}
    	}
    	System.out.println("5 list done in " + (System.currentTimeMillis() - start) + "ms with count: " + count);
    	
    }

}
