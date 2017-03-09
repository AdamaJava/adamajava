package org.qcmg.qprofiler.summarise;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qprofiler.QProfiler;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

public class KmersSummaryTest {
	private static final String SAM_INPUT_FILE = "testInputFile.sam";
	
	@Before
	public void createFile(){ createTestSamFile(); }
	
	@After
	public void deleteFile(){ new File(SAM_INPUT_FILE).delete(); }
	
	@Test
	public void bothReversedTest() throws IOException {		
		KmersSummary summary = new KmersSummary(KmersSummary.maxKmers);	
 		try( SamReader reader = SAMFileReaderFactory.createSAMFileReader( new File(SAM_INPUT_FILE)); ){
 			for (SAMRecord samRecord : reader) 
				summary.parseKmers(samRecord.getReadBases(), true );				
 		}
 		
		//kmers3
		// CAGNG TTAGG <= GTCNCAATCC <= CCTAACNCTG		    
		// AGNG TTAGG <= TCNCAATCC  <= CCTAACNCT				
 		String[] bases = summary.getPossibleKmerString(3, true); 
		for(int cycle = 0; cycle < 10; cycle ++) 
			for(String base : bases)
				if( cycle == 0  && ( base.equals("CAG")  || base.equals("AGN")) )
					assertTrue(summary.getCount(cycle, base ) == 1);
				else if(cycle == 1 && ( base.equals("AGN") || base.equals("GNG")) )
					assertTrue(summary.getCount(cycle, base ) == 1);
				else if(cycle == 2 && (   base.equals("GNG") || base.equals("NGT" )) )
					assertTrue(summary.getCount(cycle, base ) == 1);
				else if(cycle == 3 && ( base.equals("NGT" )  || base.equals( "GTT")) )
					assertTrue(summary.getCount(cycle, base ) == 1);
				else if(cycle == 4 &&  base.equals("GTT") )
					assertTrue(summary.getCount(cycle, base ) == 1);		
				else
					assertTrue(summary.getCount(cycle, base ) == 0);		
	}
		
	@Test
	public void bothForwardTest() throws IOException {

		KmersSummary summary = new KmersSummary(6);	
//		QLogger logger = QLoggerFactory.getLogger(QProfiler.class, "aa.log", null);		
		try( SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(SAM_INPUT_FILE));){
//			logger.logInitialExecutionStats("qprofiler", "", new String[] {""});
			for (SAMRecord samRecord : reader) 
				//for(int i = 0; i < 10000000; i ++)
				summary.parseKmers(samRecord.getReadBases(), false );				
//			logger.logFinalExecutionStats(0);
		}
		
		 //kmers1
		 // CCTA A CNCTG
		 // CCTA A CNCT
		String[] bases = summary.getPossibleKmerString(1, true); 
		for(int cycle = 0; cycle < 10; cycle ++) 
			for( String base : bases )
				if((cycle == 0 || cycle == 1) && base.equals("C")) 
			 		assertTrue(summary.getCount(cycle, base ) == 2);
				else if(cycle == 2 && base.equals("T")) 
			 		assertTrue(summary.getCount(cycle, base ) == 2);
				else if(cycle == 3 && base.equals("A")) 
			 		assertTrue(summary.getCount(cycle, base ) == 2);
				else if(cycle == 4 && base.equals("A")) 
					//short mers from second reads are discarded
			 		assertTrue(summary.getCount(cycle, base ) == 1);
				else{
					assertTrue(summary.getCount(cycle, base ) == 0);
				}
		
		 //kmers2
		 bases = summary.getPossibleKmerString(2, true); 	
		 for(int cycle = 0; cycle < 10; cycle ++) 
			for(String base : bases)
				if(cycle == 0 && base.equals("CC"))
					assertTrue(summary.getCount(cycle, base ) == 2);
				else if(cycle == 1 && base.equals("CT"))
					assertTrue(summary.getCount(cycle, base ) == 2);
				else if(cycle == 2 && base.equals("TA"))
					assertTrue(summary.getCount(cycle, base ) == 2);
				else if(cycle == 3 && base.equals("AA"))
					assertTrue(summary.getCount(cycle, base ) == 2);
				else if(cycle == 4 && base.equals("AC"))
					assertTrue(summary.getCount(cycle, base ) == 1);
				else  
					assertTrue(summary.getCount(cycle, base ) == 0);
	}
	
	private static void createTestSamFile( ) {
		List<String> data = new ArrayList<String>();
		data.add("@HD	VN:1.0	SO:coordinate");
		data.add("@RG	ID:1959T	SM:eBeads_20091110_CD	DS:rl=50");
		data.add("@SQ	SN:chr1	LN:249250621");
		data.add("@CO	Test SAM file for use by KmersSummaryTest.java");
		
		//reverse
		data.add("642_1887_1862	83	chr1	10167	1	5H10M	=	10176	59	CCTAACNCTG	.(01(\"!\"	RG:Z:1959T");	
 		//forward
		data.add("970_1290_1068	163	chr1	10176	3	9M6H	=	10167	-59	CCTAACNCT	I&&HII%%I	RG:Z:1959T");
		
		
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(SAM_INPUT_FILE)) ) ) {
			for (String line : data)   out.println(line);			 
		} catch (IOException e) {
			Logger.getLogger("KmersSummaryTest").log(
					Level.WARNING, "IOException caught whilst attempting to write to SAM test file: " + SAM_INPUT_FILE, e);
		}  
	}
	
}
