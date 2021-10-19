package org.qcmg.qmule.bam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;


import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.SequenceUtil;
import htsjdk.samtools.util.StringUtil;

public class QSamToFastqTest {
	
	@ClassRule
	public static TemporaryFolder testFolder = new TemporaryFolder();
	
	
	@Test
	public void testDefault() throws Exception {
		File fsam = testFolder.newFile("testA.sam");
		File fr1 = testFolder.newFile("r1.fastq");
		File fr2 = testFolder.newFile("r2.fastq");
		createTestSamFile(fsam, "queryname");
		
		String[] args = new String[]{ "I="+fsam.getAbsolutePath(), "FASTQ="+fr1.getAbsolutePath(), "SECOND_END_FASTQ="+fr2.getAbsolutePath(), "RC=true" };
		try {
			int exitStatus = new QSamToFastq().instanceMain(args);
			
			assertEquals(0, exitStatus);
			//check output
			assertEquals( countFastq(fr1.getAbsolutePath()), 3 ) ;
			assertEquals( countFastq(fr2.getAbsolutePath()), 3 ) ;

		} catch (Exception e) {
			e.printStackTrace();
			fail("no exception should have been thrown from executeWithExcludeArgs()");
		}		

	}
	
	@Test
	public void SortTest() throws Exception {
		File fsam = testFolder.newFile();
		File fr1 = testFolder.newFile();
		File fr2 = testFolder.newFile();
		createTestSamFile(fsam, "coordinate");
		
		String[] args = new String[]{ "I="+fsam.getAbsolutePath(), "FASTQ="+fr1.getAbsolutePath(), "SECOND_END_FASTQ="+fr2.getAbsolutePath(), "RC=true" };
		try {
			new QSamToFastq().instanceMain(args);
			fail("no exception should reach here!");
		} catch (Exception e) {
			assertEquals(e.getMessage(), "input file is not sorted by " + SortOrder.queryname.name());			
		}		
	}
	
	@Test
	public void missingPairTest() throws IOException {
		
		String[] args = getArgs( "queryname" , true, false, false, false, true, true, false) ;
		 
		String fr1 = args[1].substring(6);
		String fr2 = args[2].substring(17);
		
		try {
			//System.out.println(Arrays.toString(args));
			new QSamToFastq().instanceMain(args);	
						
			//check output
			assertEquals( countFastq(fr1), 2 ) ;
			assertEquals( countFastq(fr2), 2 ) ;				
		} catch (Exception e) {
			fail("no exception should have been thrown from executeWithExcludeArgs()");
		}	
	}
	
	@Test
	public void reverseTest() throws IOException {
				
		//check negative read ST-E00119:628:HFMTKALXX:7:1116:25652:22616 second in pair		 
		String bases = "TCTATCAAAAGAAAGTTTCAAGTCTGTGAGTTGAATTGCACACATC";
		String qualities = "JJ<JFJ<JJJJJJJJFJ-7<FJFJJFJJJJFJJAAJJJJJJJJJJJ";		
	
		try {
			//reverse negative reads, discard bad reads, rescue reads missing mate
			String[] args = getArgs( "queryname" , true, false, false, false, true, true, false) ;			
			new QSamToFastq().instanceMain(args);			
			FastqRecord re = getFastqByOrder(args[2].substring(17), 2);
			assertEquals( re.getReadString(), SequenceUtil.reverseComplement(bases));
			assertEquals( re.getBaseQualityString(), StringUtil.reverseString(qualities));
			
			//not  reverse negative reads,  but discard bad reads, rescue reads missing mate
			args = getArgs( "queryname" , false, false, false, false, true, true, false);			
			new QSamToFastq().instanceMain(args);
		
			re = getFastqByOrder(args[2].substring(17), 2);
			assertEquals( re.getReadString(), bases);
			assertEquals( re.getBaseQualityString(), qualities);	
			 
		} catch (Exception e) {
			fail("no exception should have been thrown from executeWithExcludeArgs()");
		}			
	}
	
	@Test 
	public void baseN_mateFlag_Test() throws IOException {
		
		//data.add("ST-E00119:628:HFMTKALXX:7:1212:8572:14019	77	*	0	0	*	*	0	0	*	*	ZC:i:3	PG:Z:MarkDuplicates	RG:Z:dc8f5b43-b193-408d-946e-b2315ea1485a");		

		try {
		 
			//set N to base if *
			String[] args = getArgs( "queryname" , true, false, false, false, true, true, false) ;			
			new QSamToFastq().instanceMain(args);			
			FastqRecord re = getFastqByOrder(args[1].substring(6), 2);
			assertEquals( re.getReadString(), "N");
			assertEquals( re.getBaseQualityString(), "!");
			assertEquals( re.getReadName(), "ST-E00119:628:HFMTKALXX:7:1212:8572:14019/1");
			
			//keep same if *
			args = getArgs( "queryname" , false, false, false, false, false, false, false);			
			new QSamToFastq().instanceMain(args);
		
			re = getFastqByOrder(args[1].substring(6), 2);			
			
			assertEquals( re.getReadString(), "*");
			assertEquals( re.getBaseQualityString(), "*");
			assertEquals( re.getReadName(), "ST-E00119:628:HFMTKALXX:7:1212:8572:14019");
			 
		} catch (Exception e) {
			fail("no exception should have been thrown from executeWithExcludeArgs()");
		}							
	}
	
	@Test
	public void badReadTest() throws IOException {
		String[] args = getArgs( "queryname" , true, true,true,true, true, true, true) ;
		 		
		try {
			new QSamToFastq().instanceMain(args);	
					
			//check output
			assertEquals( countFastq(args[1].substring(6)), 5 ) ;
			assertEquals( countFastq(args[2].substring(17)), 5 ) ;			
		} catch (Exception e) {
			e.printStackTrace();
			fail("no exception should have been thrown from executeWithExcludeArgs()");
		}	

	}
		
	
	private static int countFastq(String fastq) {
				
		int count = 0;
		try (FastqReader reader =  new FastqReader( new File(fastq));) {
			for (FastqRecord record : reader) { 				
				count ++;			
			}			 			
		}		
		return count;		
	}
	
	private static FastqRecord getFastqByOrder(String fastq, int order) {
		int count = 0;
		try (FastqReader reader =  new FastqReader( new File(fastq));) {
			for (FastqRecord record : reader) { 					
				count ++;					
				if(count == order ) return record;
			}			 			
		}		
		return null; 		
	}
	
	private static String[] getArgs(String order, boolean rc, boolean non_pf, boolean non_primary, boolean supply, boolean  mate, boolean base2n, boolean rescue) throws IOException {
		
		File fsam = testFolder.newFile();
		File fr1 = testFolder.newFile();
		File fr2 = testFolder.newFile();
		createTestSamFile(fsam, order);
				
		String src = rc? "RC=true" : "RC=false";
		String snon_pf = non_pf? "INCLUDE_NON_PRIMARY_ALIGNMENTS=true" :  "INCLUDE_NON_PRIMARY_ALIGNMENTS=false";		
		String snon_primary =  non_primary? "INCLUDE_NON_PF_READS=true" : "INCLUDE_NON_PF_READS=false";							
		String  ssupply = supply? "INCLUDE_SUPPLEMENTARY_READS=true" : "INCLUDE_SUPPLEMENTARY_READS=false";											
		String   smate = mate? "MARK_MATE=true" : "MARK_MATE=false";								
		String  sbase2n = base2n? "BASE_NULL_TO_N=true" : "BASE_NULL_TO_N=false";
		String  srescue = rescue? "MISS_MATE_RESCUE=true" : "MISS_MATE_RESCUE=false" ;
		/*
		[I=/var/folders/df/jjbl15r1537868_k4cvl3wxjnt65fc/T/junit7340874920304215746/junit5471405651991744034.tmp,
				FASTQ=/var/folders/df/jjbl15r1537868_k4cvl3wxjnt65fc/T/junit7340874920304215746/junit2341121860788428885.tmp, 
				SECOND_END_FASTQ=/var/folders/df/jjbl15r1537868_k4cvl3wxjnt65fc/T/junit7340874920304215746/junit8441651223736471192.tmp, 
				RC=true, 
				INCLUDE_NON_PRIMARY_ALIGNMENTS=true, INCLUDE_NON_PF_READS=true, INCLUDE_SUPPLEMENTARY_READS=, 
				MARK_MATE=true, MISS_BASE_TO_N=true, MISS_MATE_RESCUE=false]
		 */
		 
		return new String[]{ "I="+fsam.getAbsolutePath(), "FASTQ="+fr1.getAbsolutePath() + ".fastq", 
				"SECOND_END_FASTQ="+fr2.getAbsolutePath() + ".fastq", 
				src,  snon_pf, snon_primary, ssupply, smate, sbase2n, srescue };
	}
	
	/**
	 * 
	 * @param file
	 * @param sort: coordinate, queryname or null
	 * @throws IOException 
	 */
	public static void createTestSamFile(File file, String sort) throws IOException {
		
		
		List<String> data = new ArrayList<String>();
		
		data.add("@HD	VN:1.0	SO:" + sort);
		data.add("@RG	ID:1959T	SM:eBeads_20091110_CD	DS:rl=50");
		data.add("@PG	ID:SOLID-GffToSam	VN:1.4.");
		data.add("@SQ	SN:chr1	LN:249250621");
		data.add("@CO	Test SAM file for use by BamSummarizerTest.java");
			
		 // first in pair supplementary
		data.add("ST-E00119:628:HFMTKALXX:7:1101:32146:48494	2113	chr1	14368326	4	10H35M1I10M95H	chr1	26260486	0	TCTATCAAAAGAAAGTTTCAAGTCTGTGAGTTGAATTGCACACATC	JJ<JFJ<JJJJJJJJFJ-7<FJFJJFJJJJFJJAAJJJJJJJJJJJ	SA:Z:chrX,61733198,+,82S42M1I26M,17,5	XA:Z:chr2,-132983118,105S41M5S,1;chr8,-46871089,105S33M13S,0;chr8,-43807382,105S33M13S,0	ZC:i:3	MD:Z:45 PG:Z:MarkDuplicates	RG:Z:dc8f5b43-b193-408d-946e-b2315ea1485a");

		// second in pair read fails platform/vendor quality checks		
		data.add("ST-E00119:628:HFMTKALXX:7:1101:32146:48494	641	chr1	14368326	4	10H35M1I10M95H	chr1	26260486	0	TCTATCAAAAGAAAGTTTCAAGTCTGTGAGTTGAATTGCACACATC	JJ<JFJ<JJJJJJJJFJ-7<FJFJJFJJJJFJJAAJJJJJJJJJJJ	SA:Z:chrX,61733198,+,82S42M1I26M,17,5	ZC:i:3	MD:Z:45 PG:Z:MarkDuplicates	RG:Z:dc8f5b43-b193-408d-946e-b2315ea1485a");

		 // first in pair read is PCR or optical duplicate		
		data.add("ST-E00119:628:HFMTKALXX:7:1101:32146:48494	1809	chr1	14368326	4	10H35M1I10M95H	chr1	26260486	0	TCTATCAAAAGAAAGTTTCAAGTCTGTGAGTTGAATTGCACACATC	JJ<JFJ<JJJJJJJJFJ-7<FJFJJFJJJJFJJAAJJJJJJJJJJJ	SA:Z:chrX,61733198,+,82S42M1I26M,17,5	ZC:i:3	MD:Z:45 PG:Z:MarkDuplicates	RG:Z:dc8f5b43-b193-408d-946e-b2315ea1485a");
				
		//first in pair
		data.add("ST-E00119:628:HFMTKALXX:7:1212:3599:14336	77	*	0	0	*	*	0	0	TCTATCAAAAGAAAGTTTCAAGTCTGTGAGTTGAATTGCACACATC	JJ<JFJ<JJJJJJJJFJ-7<FJFJJFJJJJFJJAAJJJJJJJJJJJ	ZC:i:3	PG:Z:MarkDuplicates	RG:Z:dc8f5b43-b193-408d-946e-b2315ea1485a");

		//second in pair
		data.add("ST-E00119:628:HFMTKALXX:7:1212:3599:14336	141	*	0	0	*	*	0	0	AGTACAGCAGCCATTCTGCACATAGCTAAAATAGAGATCTGATGAACCATAAACTCTGATTTTTCTACATAGCAATTGTCATGCTGTGTGTCTAACCAGACTGAAGTGCATTTGAGAAATTTACTTTAAAGCCATCTGAAACTGAAAATGT	AAFFFJJJJJJJJFJJJJJFFJFJJJJJJJJJJFJJJ<FJJJJJJJJJJJJJFJJJJJJJ<FFJJJJFJJAFAFJ-FJJJJJJF7JFJJJJ<JAFFFFF7JJF<FFF---JJJJJ<<A-<FFJJJJJF7-7-<-A7AFAA7FFFF---A<7	ZC:i:3	PG:Z:MarkDuplicates	RG:Z:dc8f5b43-b193-408d-946e-b2315ea1485a");
				
		//second in pair,reversed,	missing first read
		data.add("ST-E00119:628:HFMTKALXX:7:1116:25652:22616	147	chr1	20514631	60	151M	=	20514362	-420	TCTATCAAAAGAAAGTTTCAAGTCTGTGAGTTGAATTGCACACATC	JJ<JFJ<JJJJJJJJFJ-7<FJFJJFJJJJFJJAAJJJJJJJJJJJ	ZC:i:3	MD:Z:112C2T35	PG:Z:MarkDuplicates	RG:Z:dc8f5b43-b193-408d-946e-b2315ea1485a");		
		
		//first in pair missing sequence 
		data.add("ST-E00119:628:HFMTKALXX:7:1212:8572:14019	77	*	0	0	*	*	0	0	*	*	ZC:i:3	PG:Z:MarkDuplicates	RG:Z:dc8f5b43-b193-408d-946e-b2315ea1485a");		
				
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));) {
			for (String line : data) out.println(line);	
		}		 
		
	}	
}
