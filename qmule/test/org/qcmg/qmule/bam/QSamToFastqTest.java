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

import htsjdk.samtools.util.StringUtil;

public class QSamToFastqTest {
	
	@ClassRule
	public static TemporaryFolder testFolder = new TemporaryFolder();
	
	
	@Test
	public void testA() throws Exception {
		File fsam = testFolder.newFile("testA.sam");
		File fr1 = testFolder.newFile("r1.fastq");
		File fr2 = testFolder.newFile("r2.fastq");
		createTestSamFile(fsam, "queryname");
		
		String[] args = new String[]{ "I="+fsam.getAbsolutePath(), "FASTQ="+fr1.getAbsolutePath(), "SECOND_END_FASTQ="+fr2.getAbsolutePath(), "RC=true" };
		try {
			int exitStatus = new QSamToFastq().instanceMain(args);
			assertEquals(0, exitStatus);
		} catch (Exception e) {
			e.printStackTrace();
			fail("no exception should have been thrown from executeWithExcludeArgs()");
		}		

	}
	
	@Test
	public void testSort() throws Exception {
		File fsam = testFolder.newFile("testA.sam");
		File fr1 = testFolder.newFile("r1.fastq");
		File fr2 = testFolder.newFile("r2.fastq");
		createTestSamFile(fsam, "coordinate");
		
		String[] args = new String[]{ "I="+fsam.getAbsolutePath(), "FASTQ="+fr1.getAbsolutePath(), "SECOND_END_FASTQ="+fr2.getAbsolutePath(), "RC=true" };
		try {
			int exitStatus = new QSamToFastq().instanceMain(args);
			assertEquals(0, exitStatus);
		} catch (Exception e) {
			e.printStackTrace();
			fail("no exception should have been thrown from executeWithExcludeArgs()");
		}		
		
		//
		
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
			
		String s="ST-E00119:628:HFMTKALXX:7:1101:32146:48494	2113	chr1	14368326	4	10H35M1I10M95H	chr1	26260486	0	TCTATCAAAAGAAAGTTTCAAGTCTGTGAGTTGAATTGCACACATC	JJ<JFJ<JJJJJJJJFJ-7<FJFJJFJJJJFJJAAJJJJJJJJJJJ	SA:Z:chrX,61733198,+,82S42M1I26M,17,5	ZC:i:3	MD:Z:45 PG:Z:MarkDuplicates	RG:Z:dc8f5b43-b193-408d-946e-b2315ea1485a";		
		 // first in pair supplementary
		data.add("ST-E00119:628:HFMTKALXX:7:1101:32146:48494	2113	chr1	14368326	4	10H35M1I10M95H	chr1	26260486	0	TCTATCAAAAGAAAGTTTCAAGTCTGTGAGTTGAATTGCACACATC	JJ<JFJ<JJJJJJJJFJ-7<FJFJJFJJJJFJJAAJJJJJJJJJJJ	SA:Z:chrX,61733198,+,82S42M1I26M,17,5	XA:Z:chr2,-132983118,105S41M5S,1;chr8,-46871089,105S33M13S,0;chr8,-43807382,105S33M13S,0	ZC:i:3	MD:Z:45 PG:Z:MarkDuplicates	RG:Z:dc8f5b43-b193-408d-946e-b2315ea1485a");

		// first in pair read fails platform/vendor quality checks		
		data.add("ST-E00119:628:HFMTKALXX:7:1101:32146:48494	577	chr1	14368326	4	10H35M1I10M95H	chr1	26260486	0	TCTATCAAAAGAAAGTTTCAAGTCTGTGAGTTGAATTGCACACATC	JJ<JFJ<JJJJJJJJFJ-7<FJFJJFJJJJFJJAAJJJJJJJJJJJ	SA:Z:chrX,61733198,+,82S42M1I26M,17,5	ZC:i:3	MD:Z:45 PG:Z:MarkDuplicates	RG:Z:dc8f5b43-b193-408d-946e-b2315ea1485a");

		 // first in pair read is PCR or optical duplicate		
		data.add("ST-E00119:628:HFMTKALXX:7:1101:32146:48494	1809	chr1	14368326	4	10H35M1I10M95H	chr1	26260486	0	TCTATCAAAAGAAAGTTTCAAGTCTGTGAGTTGAATTGCACACATC	JJ<JFJ<JJJJJJJJFJ-7<FJFJJFJJJJFJJAAJJJJJJJJJJJ	SA:Z:chrX,61733198,+,82S42M1I26M,17,5	ZC:i:3	MD:Z:45 PG:Z:MarkDuplicates	RG:Z:dc8f5b43-b193-408d-946e-b2315ea1485a");
		

				
		//first in pair
		data.add("ST-E00119:628:HFMTKALXX:7:1212:3599:14336	77	*	0	0	*	*	0	0	TGTTTTAGAAAAATAAGTTCAAGAAATTGTGGTGACAAAAGTCTCACAATGACATAAAGTCAAATCTGGGGATTAAATTCAGAACCCTAAGCCTTTGTTTTGCTTCTAAAATGCAGACATCCAAGAGAAACAGGAGAGACAGCAATGCAGC AAAFFJJJJJJJJJJJJJJJJJJJFFJJFJJJJJAJFJJJJJJ<JJJJJJJJJJJAFJJJJJJJFJ-FJJFFJJJJAFJJJJJJJJJJJJAAJAAJFJJJJJJJJJJJJJJJFJJFFJJJAAFFFA7<AAJJJJJJAFJFJJFFFFFJJ<F ZC:i:3	PG:Z:MarkDuplicates	RG:Z:dc8f5b43-b193-408d-946e-b2315ea1485a");
		//second in pair
		data.add("ST-E00119:628:HFMTKALXX:7:1212:3599:14336	141	*	0	0	*	*	0	0	AGTACAGCAGCCATTCTGCACATAGCTAAAATAGAGATCTGATGAACCATAAACTCTGATTTTTCTACATAGCAATTGTCATGCTGTGTGTCTAACCAGACTGAAGTGCATTTGAGAAATTTACTTTAAAGCCATCTGAAACTGAAAATGT AAFFFJJJJJJJJFJJJJJFFJFJJJJJJJJJJFJJJ<FJJJJJJJJJJJJJFJJJJJJJ<FFJJJJFJJAFAFJ-FJJJJJJF7JFJJJJ<JAFFFFF7JJF<FFF---JJJJJ<<A-<FFJJJJJF7-7-<-A7AFAA7FFFF---A<7 ZC:i:3	PG:Z:MarkDuplicates	RG:Z:dc8f5b43-b193-408d-946e-b2315ea1485a");
				
		//second in pair,reversed,	missing first read
		data.add("ST-E00119:628:HFMTKALXX:7:1116:25652:22616	147	chr1	20514631	60	151M	=	20514362	-420	ATTGGCTTCAACACTAATAAAACCGCACACATAAGAAACTGAATAATCAGGTCAAGGAGTAAGAGACATTTGTCAAGGTACTGCCTAAGTGCACTTATTTCTCCTTTCAACATCTATAAGAAGCCAGTCAGATGAATAATTAGTCCGTATA FAAFA--AA-A-<-<FAAJ7<)FJJ<FJJJJAA<JA7FJJJJA7-A7FJJJJJJJJFA7FJJJFJJFJJJJJJJFAFAJFFFJJJJJJJJJJJJJFJJJAJJJJJJJJJJJFJJJJJFAJAAFJAFJJJJJJJJJJJJJJJFJJJJFAA<A ZC:i:3	MD:Z:112C2T35	PG:Z:MarkDuplicates	RG:Z:dc8f5b43-b193-408d-946e-b2315ea1485a");		
		
		//first in pair missing sequence 
		data.add("ST-E00119:628:HFMTKALXX:7:1212:8572:14019	77	*	0	0	*	*	0	0	*	*	ZC:i:3	PG:Z:MarkDuplicates	RG:Z:dc8f5b43-b193-408d-946e-b2315ea1485a");		
		
		
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));){
			for (String line : data) out.println(line);	
		}
		
	}	
}
