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


public class WindowCountTest {
	public static final String INPUT_TUMOR_BAM = "input.tumor.bam";
	public static final String INPUT_NORMAL_BAM = "input.normal.bam";	
	public static final String OUTPUT_FILE_NAME = "output.counts";
	
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
	 * Test the counts in the first reference record, that should be 0
	 * @throws Exception
	 */
	@Test
	public void Count0Test() throws Exception{
		
		/*debug
		 int iAsciiValue = 33; // Currently just the number 9, but we want Tab character
		    // Put the tab character into a string
		    String strAsciiTab = Character.toString((char) iAsciiValue);
		    System.out.println(iAsciiValue + "  is ascii of " + strAsciiTab);
		 */
		
		SAMSequenceRecord ref = genome.get(0);
		int windowSize = 10000;
		WindowCount mycount = new WindowCount(new File( INPUT_NORMAL_BAM), new File(INPUT_TUMOR_BAM), ref, windowSize) ;
		int[] normalCount = mycount.counting(mycount.rref );
		int[] tumorCount = mycount.counting(mycount.rtest );
		
		int N_total = 0;
		int T_total = 0;
		for(int i = 0; i < normalCount.length; i++){
			N_total += normalCount[i];
			T_total += tumorCount[i];
		}
		assertTrue(N_total == 0);
		assertTrue(T_total == 0);
	}

	/**
	 * Test the counts in the second reference record, that should be 14
	 * @throws Exception
	 */
	@Test
	public void CountW319Test() throws Exception{
		SAMSequenceRecord ref = genome.get(1);
		int windowSize = 319;				
		WindowCount mycount = new WindowCount(new File( INPUT_NORMAL_BAM), new File(INPUT_TUMOR_BAM), ref, windowSize) ;
		int[] normalCount = mycount.counting(mycount.rref );
		int[] tumorCount = mycount.counting(mycount.rtest );
	
		int N_total = 0;
		int T_total = 0;
		for(int i = 0; i < normalCount.length; i++){
			N_total += normalCount[i];
			T_total += tumorCount[i];
		}
		//first record at position 319, it counts into second window
		assertTrue(normalCount[0] == 0);
		assertTrue(tumorCount[1] - normalCount[1] == 4);
		assertTrue(T_total - N_total == 4);
		assertTrue(T_total + N_total == 14);
	}

	/**
	 * Test the counts in the second reference record, that should be 14
	 * @throws Exception
	 */
	@Test
	public void CountW320Test() throws Exception{
		SAMSequenceRecord ref = genome.get(1);
		int windowSize = 320;				
		WindowCount mycount = new WindowCount(new File( INPUT_NORMAL_BAM), new File(INPUT_TUMOR_BAM), ref, windowSize) ;
		int[] normalCount = mycount.counting(mycount.rref );
		int[] tumorCount = mycount.counting(mycount.rtest );
	
		int N_total = 0;
		int T_total = 0;
		for(int i = 0; i < normalCount.length; i++){
			N_total += normalCount[i];
			T_total += tumorCount[i];
		}
		//first record at position 319, it counts into first window
		assertTrue(normalCount[0] == 1);
		assertTrue(tumorCount[1] - normalCount[1] == 4);
		assertTrue(T_total - N_total == 4);
		assertTrue(T_total + N_total == 14);
		
//		System.out.println(ref.getSequenceName() + " normal: " + Arrays.toString(normalCount));
//		System.out.println(ref.getSequenceName() + " tumor: " + Arrays.toString(tumorCount));
	}
	
	/*
	 * Test final output counts
	 */
	@Test
	public void MtCountsTest() throws Exception{		
		int windowSize = 320;
		String logFile = "./" + OUTPUT_FILE_NAME + ".log";
		QLogger  logger = QLoggerFactory.getLogger( Main.class,logFile ,null);
		MtCounts cnvThread = new MtCounts(new File( INPUT_NORMAL_BAM), new File(INPUT_TUMOR_BAM), 
				new File(OUTPUT_FILE_NAME), new File( "." ), 1, windowSize, logger);
		cnvThread.callCounts();
		new File(logFile).delete();
						 
		BufferedReader br = new BufferedReader(new FileReader(OUTPUT_FILE_NAME));
		int N_line = 0;
		int N_ref0 = 0;
		int N_ref1 = 0;		
		int N_read = 0;
		String sCurrentLine;
		while ((sCurrentLine = br.readLine()) != null) {
			N_line ++;
			if(sCurrentLine.startsWith(genome.get(0).getSequenceName()))
				N_ref0 ++;
			else if(sCurrentLine.startsWith(genome.get(1).getSequenceName())){
				N_ref1 ++;
				List<String> myList = Arrays.asList(sCurrentLine.split(","));
				N_read += Integer.parseInt(myList.get(3)) + Integer.parseInt(myList.get(4));
				
				if( myList.get(1).equals("320") && myList.get(2).equals("639")){					 
					assertTrue( Integer.parseInt(myList.get(3)) == 3);
					assertTrue( Integer.parseInt(myList.get(4)) == 7); 
				}else if(sCurrentLine.contains("GL000196.1,38720,")){
					assertTrue( Integer.parseInt(myList.get(3)) == 0);
					assertTrue( Integer.parseInt(myList.get(4)) == 0);
					assertTrue( Integer.parseInt(myList.get(2)) == genome.get(1).getSequenceLength());
				}
				
			}else{
				assertTrue(sCurrentLine.startsWith("#ChromosomeArmID"));
			}			 
		}		
		//window number equal to ref length divided by window size
		assertTrue( N_ref0 == genome.get(0).getSequenceLength() / windowSize + 1 );
		assertTrue( N_ref1 == genome.get(1).getSequenceLength() / windowSize + 1 );
		//there should be one headline, plus window number
		assertTrue(N_ref0 + N_ref1 + 1 == N_line);	
		//total 14 reads
		assertTrue(N_read == 14);
	}
}
