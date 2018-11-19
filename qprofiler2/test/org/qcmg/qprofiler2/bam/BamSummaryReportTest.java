package org.qcmg.qprofiler2.bam;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.parsers.ParserConfigurationException;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMUtils;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.MAPQMiniMatrix;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.SummaryByCycleUtils;
import org.qcmg.qprofiler2.bam.BamSummaryReport2;
import org.qcmg.qprofiler2.summarise.PositionSummary;
import org.qcmg.qprofiler2.util.SummaryReportUtilsTest;


public class BamSummaryReportTest {
		
	@Test
	public void testParseRNameAndPos() throws Exception {
		BamSummaryReport2 bsr = new BamSummaryReport2(new String [] {"matrices","coverage"}, -1, null, null, null);
		final String rg = "rg1";
		List<String> rgs = Arrays.asList(rg);
		bsr.setReadGroups(rgs );
		
		String rName = "test";
		int position = 999;		
		bsr.parseRNameAndPos( rName, position,rg );
		PositionSummary returnedSummary = bsr.getRNamePosition().get(rName);
		assertEquals( position, returnedSummary.getMax() );
		assertEquals( position, returnedSummary.getMin() );
		assertEquals( 1, returnedSummary.getCoverage().get(0).get() );
		
		// and again - min and max should stay the same, count should increase
		bsr.parseRNameAndPos(rName, position,rg );
		returnedSummary = bsr.getRNamePosition().get(rName);
		assertEquals(position, returnedSummary.getMax());
		assertEquals(position, returnedSummary.getMin());
		assertEquals(2, returnedSummary.getCoverage().get(0).get());
		
		// add another position to this rName
		position = 1000000;
		bsr.parseRNameAndPos(rName, position,rg );
		returnedSummary = bsr.getRNamePosition().get(rName);
		assertEquals(position, returnedSummary.getMax());
		assertEquals(999, returnedSummary.getMin());
		assertEquals(1, returnedSummary.getCoverageByRgs(rgs).get(1).get(0) );
		
		// add another position to this rName
		position = 0;
		bsr.parseRNameAndPos(rName, position,rg );
		returnedSummary = bsr.getRNamePosition().get(rName);
		assertEquals(1000000, returnedSummary.getMax());
		assertEquals(position, returnedSummary.getMin());
		assertEquals(3, returnedSummary.getCoverageByRgs(rgs).get(0).get(0) );
		assertEquals(1, returnedSummary.getCoverageByRgs(rgs).get(1).get(0) );
		
		// add a new rname
		rName = "new rname";
		bsr.parseRNameAndPos(rName, 0,rg );
		returnedSummary = bsr.getRNamePosition().get(rName);
		assertEquals(0, returnedSummary.getMax());
		assertEquals(0, returnedSummary.getMin());
		assertEquals(1, returnedSummary.getCoverageByRgs(rgs).get(0).get(0) );
		assertEquals(1, returnedSummary.getCoverageByRgs(rgs).get(0).length() );
		
	}
	
		
	@Test
	public void testCompareWithSAMUtils() {
		String inputString = "!''*((((***+))%%%++)(%%%%).1***-+*''))**55CCF>>>>>>CCCCCCC65";
		String expectedOutputString = "BHHKIIIIKKKLJJFFFLLJIFFFFJORKKKNLKHHJJKKVVddg______dddddddWV";
		int counter = 100000;
		String outputString = null;
		long start = 0;
		
		start = System.currentTimeMillis();
		for (int i = 0 ; i < counter ; i++) 
			outputString = StringUtils.addASCIIValueToChar(inputString, 33);					
		assertEquals(expectedOutputString, outputString);
				
		byte [] bytes = inputString.getBytes();
		start = System.currentTimeMillis();
		for (int i = 0 ; i < counter ; i++) {
			
			outputString = SAMUtils.phredToFastq(bytes);
		}
		assertEquals(expectedOutputString, outputString);
		
		start = System.currentTimeMillis();
		for (int i = 0 ; i < counter ; i++) {
			outputString = StringUtils.addASCIIValueToChar(inputString, 33);
			
		}
		assertEquals(expectedOutputString, outputString);
	}
	
	
	
				
		
		
	}


