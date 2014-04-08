package org.qcmg.cnv;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMSequenceRecord;
import net.sf.samtools.SAMFileReader.ValidationStringency;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.SAMFileReaderFactory;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class BaseCountTest {
	public static final String INPUT_TUMOR_BAM = "input.tumor.bam";
	public static final String INPUT_NORMAL_BAM = "input.normal.bam";	
	public static final String OUTPUT_FILE_NAME = "output.counts";
	
	//the base counts in test file
	final int refCounts = 212;
	final int testCounts = 354;
	
	List<SAMSequenceRecord> genome;
		
	@After
	public void deleteFiles(){
  		new File(INPUT_TUMOR_BAM).delete(); 
 		new File(INPUT_NORMAL_BAM).delete();	
 		new File(INPUT_TUMOR_BAM + ".bai").delete(); 
 		new File(INPUT_NORMAL_BAM + ".bai").delete();	
		new File(OUTPUT_FILE_NAME).delete(); 
	}
	
	@Before
	public void before(){		 
		TestFiles.CreateBAM(INPUT_NORMAL_BAM, INPUT_TUMOR_BAM);		
		SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(
				new File(INPUT_TUMOR_BAM),
				ValidationStringency.SILENT );
		
		genome = reader.getFileHeader().getSequenceDictionary().getSequences();
		reader.close();
	}
	
	 
	/**
	 * Test the counts in the second reference record. These data is stored in array bf go to the tmp files
	 * @throws Exception
	 */
	@Test
	public void CountTest() throws Exception{
		SAMSequenceRecord ref = genome.get(1);
		BaseCount mycount = new BaseCount(new File( INPUT_NORMAL_BAM), new File(INPUT_TUMOR_BAM), ref, new File(".")) ;
		int[] normalCount = mycount.counting(mycount.rref );
		int[] tumorCount = mycount.counting(mycount.rtest );
	
		int N_total = 0;
		int T_total = 0;
		for(int i = 0; i < normalCount.length; i++){
			N_total += normalCount[i];
			T_total += tumorCount[i];
		}
		//first record at position 319, it counts into base [319,368]
		assertTrue(normalCount[318] == 0);
		for(int i = 319; i < 369; i ++)
			assertTrue(normalCount[i] == 1);		 
		assertTrue(normalCount[369] == 0);
		
		//three reads: [437 , 486], [437 , 486], [480 , 504]
		for(int i = 437; i < 480; i ++)
			assertTrue(normalCount[i] == 2);
		for(int i = 480; i < 487; i ++)
			assertTrue(normalCount[i] == 3);
		for(int i = 487; i < 505; i ++)
			assertTrue(normalCount[i] == 1);
		for(int i = 505; i < 530; i ++)
			assertTrue(normalCount[i] == 0);
		
		//total 5 normal reads and 8 tumor reads		
		assertTrue( N_total == refCounts );		
		assertTrue( T_total== testCounts );
	}
	
	/*
	 * Test final output counts
	 */
	@Test
	public void MtCountsTest() throws Exception{
		String logFile = "./" + OUTPUT_FILE_NAME + ".log";
		QLogger  logger = QLoggerFactory.getLogger( Main.class, logFile,null);
		MtCounts cnvThread = new MtCounts(new File( INPUT_NORMAL_BAM), new File(INPUT_TUMOR_BAM), 
				new File(OUTPUT_FILE_NAME), new File( "." ), 1, 0, logger);
		cnvThread.callCounts();
		new File(logFile).delete();
				 
		BufferedReader br = new BufferedReader(new FileReader(OUTPUT_FILE_NAME));
		int N_line = 0;
		int N_ref = 0;
		int N_test = 0;		

		String sCurrentLine;
		List<String> myLast = Arrays.asList("?","0","0","0");
		
		//current test data, there are three head lines
		while ((sCurrentLine = br.readLine()) != null)  
			if(++ N_line > 3 ){			 
				List<String> myList = Arrays.asList(sCurrentLine.split("\t"));				
				int current = Integer.parseInt(myList.get(1));
				//current postion in second column - last line position 
				int dis = Integer.parseInt(myList.get(1)) - Integer.parseInt(myLast.get(1));
				N_ref += dis * Integer.parseInt(myLast.get(2));   //ref counts
				N_test += dis * Integer.parseInt(myLast.get(3));  //test counts		
				myLast = myList;
			}	
		
		//total counts	should be same to above Count
		assertTrue( N_ref == refCounts );		
		assertTrue( N_test == testCounts );
  		
	}
}
