package org.qcmg.qbamfix;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.ValidationStringency;
import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.SAMFileReaderFactory;

public class FixBAMTest {
	
	public static final String INPUT_FILE_NAME = "input.sam";
	public static final String OUTPUT_FILE_NAME  = "output.bam"; //only support bam output
	public static final String LOG_FILE_NAME = "./output.log";
	public static final File TMP_FILE = new File("tmp.bam");
	
	public static final QLogger log  = QLoggerFactory.getLogger(FixBAMTest.class, LOG_FILE_NAME ,null);
	
		
	@Test
	public void noBadMateTest() throws Exception{
		//add a mate record with same length with pair
		//createInput();
		appendInput("HWI-ST1243:96:C0VM0ACXX:6:1310:3269:47175	83	chr1	125562470	60	74M27H	=	125562393	-151	CTTGCTCTTGTATGTGGCCAGCCTCCTGGGTAATGGACTCATTGTGGCTGCCATCCAGGCCAGTCCAGCCCTTC	*	X0:i:1	X1:i:0	ZC:i:0	MD:Z:0A0C0A2A0G0C0G1C0C1C1C0T0T0T0G0T0G2G0T1C0T0T0G0C0T0C0T0T0G0T0A0T0G1G1C0C0A0G0C0C0T1C1G0G0G0T0A0A0T0G1A1T0C0A0T0T0G1G0G0	RG:Z:20120806104508577	XG:i:0	AM:i:37	NM:i:59	SM:i:37	XM:i:0	XO:i:0	XT:A:U");
		
		FixBAM myfix = new FixBAM( new File(INPUT_FILE_NAME), new File(OUTPUT_FILE_NAME) ,TMP_FILE, log, 101);
		List<SAMRecord> badReads = myfix.firstFilterRun( TMP_FILE );
		Assert.assertTrue(badReads.size() == 4);
		Assert.assertTrue(TMP_FILE.exists());
		Assert.assertTrue( new File("tmp.bam.bai").exists() );
		Assert.assertEquals(countOutputRecord(TMP_FILE), 4);
		
		HashMap<String, Integer> badMates = myfix.checkMate(badReads, TMP_FILE); 		
		Assert.assertTrue(badMates.size() == 0);
		
		//rename tmp to output, then there should no tmp files and ouptut contains 4 records
		myfix.tmp2outputBAM(TMP_FILE);
		Assert.assertFalse(TMP_FILE.exists());
		Assert.assertFalse( new File("tmp.bam.bai").exists() );		
		Assert.assertEquals(countOutputRecord(new File(OUTPUT_FILE_NAME)), 4);
	
	}
	
	@Test
	public void withBadMateTest() throws Exception{
		//add a mate record with different length with pair
		//createInput();
		appendInput("HWI-ST1243:96:C0VM0ACXX:6:1310:3269:47175	83	chr1	125562470	0	101M	=	125562393	-151	CATGGGTCATCCCCTTCACTCCCAGCTCAGAGCCCAGGCCAGGGGCCCCCAAGAAAGGCTCTGGTGGAGAACCTGTGCATGAAGGCTGTCAACCAGTCCAT	CCBADDDB>BDDDDECA8DDDDDDCCDDDDCDDDDEEEFFFFHHJJJIIJJJJJIJJJJJJJJJJJJJIIGJJJIIJJJJJJJJIIJJHHHHHFFFFFCCC	X0:i:5	X1:i:1	MD:Z:101	RG:Z:20120806104508577	XG:i:0	AM:i:0	NM:i:0	SM:i:0	XM:i:0	XO:i:0	XT:A:R");

		FixBAM myfix = new FixBAM(new File(INPUT_FILE_NAME), new File(OUTPUT_FILE_NAME) ,TMP_FILE, log,101);
		
		List<SAMRecord> badReads = myfix.firstFilterRun( TMP_FILE );
		Assert.assertTrue(badReads.size() == 3);
		
		HashMap<String, Integer> badMates = myfix.checkMate(badReads,  TMP_FILE ); 
		Assert.assertTrue(badMates.size() == 1); //one mate of bad reads contains in  
		
		Assert.assertTrue(TMP_FILE.exists());
		Assert.assertTrue( new File("tmp.bam.bai").exists() );
		Assert.assertEquals(countOutputRecord(TMP_FILE), 5);
		
		//delete tmp, created new output containing four good record
		myfix.secondFilterRun(badMates, TMP_FILE);		
		Assert.assertFalse(TMP_FILE.exists());
		Assert.assertFalse( new File("tmp.bam.bai").exists() );			
		Assert.assertEquals(countOutputRecord(new File(OUTPUT_FILE_NAME)), 4);
	}
	
	private int countOutputRecord(File output) throws IOException{
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(output, ValidationStringency.SILENT);
		
		int num = 0;
		for( SAMRecord record : reader){
			num++;
		}
		
		reader.close();
		return num;
	}
	
	@After
	public void deleteIO(){
		new File(INPUT_FILE_NAME).delete();
		new File(OUTPUT_FILE_NAME).delete();
		new File(OUTPUT_FILE_NAME + ".bai").delete();
		new File(LOG_FILE_NAME).delete();
		
	}
	@Before
	public void createInput(){
		List<String> data = new ArrayList<String>();
		data.add("@HD	VN:1.0	SO:coordinate");	
		data.add("@SQ	SN:chr1	LN:249250621");
		data.add("@SQ	SN:chr11	LN:243199373");
		data.add("@CO	create by org.qcmg.qbamfix.FixBAMTest::createInput");
		data.add("@RG	ID:20120806104508577	SM:S1	FO:ACGGGGGGAG");
		data.add("HWI-ST1243:96:C0VM0ACXX:5:1303:20132:53958	77	*	0	0	*	*	0	0	CACCC	*	ZC:i:3	RG:Z:20120806104508577");				
		data.add("HWI-ST1243:96:C0VM0ACXX:5:1303:20132:53958	141	*	0	0	*	*	0	0	CCCCA	*	ZC:i:3	RG:Z:20120806104508577");
		data.add("HWI-ST1243:96:C0VM0ACXX:7:1211:3532:63992	1187	chr1	12929	0	101M	=	13206	378	CTACAAGCAGCAAACAGTCTGCATGGGTCATCCCCTTCACTCCCAGCTCAGAGCACAGGCCAGGGGCCCCCAAGAAAGGATCTGGTGGAGAGCCTGTGCAG	@@@DBBD;B;3CCFEG:2ACF+AEFE?<+??F*1:;DEG@DH3?G@;?9<*?B=).8.6;56@65AB/49A##############################	X0:i:2	X1:i:1	XA:Z:chr15,-102518141,101M,4;chr9,+13042,101M,5;	MD:Z:54C24C11A8T0	RG:Z:20120806104508577	XG:i:0	AM:i:0	NM:i:4	SM:i:0	XM:i:4	XO:i:0	XT:A:R");
		data.add("HWI-ST1243:96:C0VM0ACXX:7:1314:9890:9091	163	chr1	12929	0	101M	=	13206	378	CTACAAGCAGCAAACAGTCTGCATGGGTCATCCCCTTCACTCCCAGCTCAGAGCCCAGGCCAGGGGCCCCCAAGAAAGGCTCTGGTGGAGAACCTGTGCAT	@CCFFFFFGGGHHJFJJHHIIJIJJJJFGHJJJJJJJJJJJJJJJIJIJJJJJJIGIGJJJJJGFHFFDCDDDDDDDDB?CDDDC:AB8ACDDDDBCCCDD	X0:i:2	X1:i:1	XA:Z:chr15,-102518141,101M,0;chr9,+13042,101M,1;	MD:Z:101	RG:Z:20120806104508577	XG:i:0	AM:i:0	NM:i:0	SM:i:0	XM:i:0	XO:i:0	XT:A:R");
		data.add("HWI-ST1243:96:C0VM0ACXX:7:1210:16664:57152	99	chr1	12933	0	101M	=	13176	344	AAGCAGCAAACAGTCTGCATGGGTCATCCCCTTCACTCCCAGCTCAGAGCCCAGGCCAGGGGCCCCCAAGAAAGGCTCTGGTGGAGAACCTGTGCATGAAG	CCCFFFFFHHHHHJJJJJJIJJJHIJJJJJJJJJJJJJJJJJJJJJJJJIIJFJJJJJJJJIGHFFDDDDDDCDDDDDDDD@CDDBDDDDDDDCDDEDDCC	X0:i:2	X1:i:2	XA:Z:chr15,-102518137,101M,0;chr16,+62614,101M,1;chr9,+13046,101M,1;	MD:Z:101	RG:Z:20120806104508577	XG:i:0	AM:i:0	NM:i:0	SM:i:0	XM:i:0	XO:i:0	XT:A:R");
		data.add("HWI-ST1243:96:C0VM0ACXX:7:1108:8918:3226	1107	chr1	12950	0	101M	=	12798	-253	CATGGGTCATCCCCTTCACTCCCAGCTCAGAGCCCAGGCCAGGGGCCCCCAAGAAAGGCTCTGGTGGAGAACCTGTGCATGAAGGCTGTCAACCAGTCCAT	CCBADDDB>BDDDDECA8DDDDDDCCDDDDCDDDDEEEFFFFHHJJJIIJJJJJIJJJJJJJJJJJJJIIGJJJIIJJJJJJJJIIJJHHHHHFFFFFCCC	X0:i:5	X1:i:1	MD:Z:101	RG:Z:20120806104508577	XG:i:0	AM:i:0	NM:i:0	SM:i:0	XM:i:0	XO:i:0	XT:A:R");
		data.add("HWI-ST1243:96:C0VM0ACXX:6:1310:3269:47175	163	chr1	125562393	60	74M	=	125562470	151	GAGCCCTTCAGGTTCCAGGCGAATAACCAGCCTGCCATGGAGGCTGCCAATGAGTCTTCAGAGGGAATCTCATT	@CCFFFFFGGGHHJFJJHHIIJIJJJJFGHJJJJJJJJJJJJJJJIJIJJJJJJIGIGJJJJJGFHFFDCDDDD	X0:i:1	X1:i:0	ZC:i:0	MD:Z:0C4T0G0C2T1G0A0G0G0C0T2C3G1G0T0C0T0T1A1A0G0G0G0A0A0T0C0T1A0T0T1G0T1T0T0A1T0G0G0G1C0T1A0C2C0A0A0G0T0C0C0	RG:Z:20120806104508577	XG:i:0	AM:i:37	NM:i:51	SM:i:37	XM:i:0	XO:i:0	XT:A:U");
 						
		BufferedWriter out;
		try {
		out = new BufferedWriter(new FileWriter(INPUT_FILE_NAME));
		for (String line : data) {
				out.write(line + "\n");
		}
		out.close();
		} catch (IOException e) {
			System.err.println("IOException caught whilst attempting to write to SAM test file: "
											+ INPUT_FILE_NAME + e);
		}
	}
	
	private void appendInput(String line){
		try
		{
		     
		    FileWriter fw = new FileWriter(INPUT_FILE_NAME,true); //the true will append the new data
		    fw.write( line + "\n");//appends the string to the file
		    fw.close();
		}
		catch(IOException ioe)
		{
		    System.err.println("IOException: " + ioe.getMessage());
		}
		
	}	

}
