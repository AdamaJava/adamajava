package org.qcmg.qsv.report;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.discordantpair.PairGroup;


public class SVCountReportTest {
	
	SVCountReport report;
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	

    @Before
    public void setUp() throws IOException, Exception {
    	report = new SVCountReport(testFolder.newFile(), "test");
    	report.addCountsToMap(PairGroup.AAC, 1, 5, 0);
    	report.addCountsToMap(PairGroup.ABA, 2, 0, 5);
    	report.addCountsToMap(PairGroup.Cxx, 1, 1, 1);
    }
    
    @After
    public void tearDown() {
    	report = null;
    }
	
	@Test
	public void testGetHeader() {
		String expected = "Mutation Type\tSample\tAAB\tABB\tAAC\tABA\tABC\tBAA_BBA\tBAB_BBB\tBAC_BBC\tCxx\tTOTAL\n";
		assertEquals(report.getHeader(), expected);
	}
	
	@Test
	public void testGetCountStringSomatic() {
		String expected= "somatic\ttest\t0\t0\t1\t2\t0\t0\t0\t0\t1\t4\n";
		assertEquals(expected, report.getCountString("somatic", report.getSomatic()));
	}
	
	@Test
	public void testGetCountStringGermline() {
		String expected= "germline\ttest\t0\t0\t5\t0\t0\t0\t0\t0\t1\t6\n";
		assertEquals(expected, report.getCountString("germline", report.getGermline()));
	}
	
	@Test
	public void testGetCountStringNormalGermline() {
		String expected= "normal-germline\ttest\t0\t0\t0\t5\t0\t0\t0\t0\t1\t6\n";
		assertEquals(expected, report.getCountString("normal-germline", report.getNormalGermline()));
	}
	
	

}
