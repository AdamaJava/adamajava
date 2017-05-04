package org.qcmg.coverage;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.PrintStream;

import htsjdk.samtools.BAMIndexer;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.qcmg.common.commandline.Executor;
import org.qcmg.gff3.GFF3FileWriter;
import org.qcmg.gff3.GFF3Record;
import org.qcmg.picard.SAMFileReaderFactory;

public class InferencePhysicalCoverageTest {
	
	final String inputSam1 = "coverage.sam"; 
	final String inputBam1 = "coverage.bam";
	final String inputIndex1 = "coverage.bai";
	final String inputSam2 = "coverage2.sam"; 
	final String inputBam2 = "coverage2.bam";
	final String inputIndex2 = "coverage2.bai";
	final String output = "output";
	final String gff3 = "test.gff3";
	final String cmd =  String.format("--log ./logfile -t phys --gff3 %s --bam %s --bai %s -o output", gff3, inputBam1, inputIndex1);
	
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public final void before() {
				
		try {
			createCoverageSam(inputSam1);			 
			MultiBamPhysicalCoverageTest.createCoverageBam(inputSam1, inputBam1,inputIndex1);
			createCoverageSam(inputSam2);
			MultiBamPhysicalCoverageTest.createCoverageBam(inputSam2, inputBam2 , inputIndex2);
		} catch (Exception e) {
			System.err.println("File creation error in test harness: "
					+ e.getMessage());
		}		
		
	}

	@After
	public final void after() {
		try {
			File file = new File(inputSam1);
			file.delete();
			File bamFile = new File(inputBam1);
			bamFile.delete();
			File baiFile = new File(inputIndex1);
			baiFile.delete();
			
			file = new File(inputSam2);
			file.delete();
			bamFile = new File(inputBam2);
			bamFile.delete();
			baiFile = new File(inputIndex2);
			baiFile.delete();			
			
			file = new File(output);
			file.delete();
			
			file = new File(gff3);
			file.delete();
		} catch (Exception e) {
			System.err.println("File deleting error in test harness: "
					+ e.getMessage());
		}
 
		
	}
	
	private File createGFF3File(final int start, final int end) throws Exception {
		GFF3Record record = new GFF3Record();
		record.setSeqId("chr1");
		record.setType("exon");
		record.setStart(start);
		record.setEnd(end);
		record.setScore(".");
		record.setSource(".");
		record.setStrand("+");

		File file = new File("test.gff3");
		GFF3FileWriter writer = new GFF3FileWriter(file);
		writer.add(record);
		writer.close();
		
		return file;
	}

	private Executor execute(final String command) throws Exception {
		return new Executor(command, "org.qcmg.coverage.Main");
	}
	

    @Test
	public final void leftDisjointRead() throws Exception {
		createGFF3File(54000, 54025);

		ExpectedException.none();
		Executor exec = execute(cmd);
		assertTrue(0 == exec.getErrCode());

		File outputFile = new File("output");
		assertTrue(outputFile.exists());
	}

    @Test
	public final void rightDisjointRead() throws Exception {
		createGFF3File(54077, 54120);

		ExpectedException.none();
		Executor exec = execute(cmd);
		assertTrue(0 == exec.getErrCode());

		File outputFile = new File("output");
		assertTrue(outputFile.exists());
	}

   
    @Test
	public final void leftOnEndRead() throws Exception {
		createGFF3File(54000, 54026);

		ExpectedException.none();
		Executor exec = execute(cmd);
		assertTrue(0 == exec.getErrCode());

		File outputFile = new File("output");
		assertTrue(outputFile.exists());
	}

    @Test
	public final void rightOnEndRead() throws Exception {
		createGFF3File(54076, 54120);

		ExpectedException.none();
		Executor exec = execute(cmd);
		assertTrue(0 == exec.getErrCode());

		File outputFile = new File("output");
		assertTrue(outputFile.exists());
	}

    @Test
	public final void leftOverlapRead() throws Exception {
		createGFF3File(54000, 54036);

		ExpectedException.none();
		Executor exec = execute(cmd);
		assertTrue(0 == exec.getErrCode());

		File outputFile = new File("output");
		assertTrue(outputFile.exists());

	}

    @Test
	public final void rightOverlapRead() throws Exception {
		createGFF3File(54050, 54120);

		ExpectedException.none();
		Executor exec = execute(cmd);
		assertTrue(0 == exec.getErrCode());

		File outputFile = new File("output");
		assertTrue(outputFile.exists());
	}

    @Test
	public final void subsetRead() throws Exception {
		createGFF3File(54030, 54070);

		ExpectedException.none();
		Executor exec = execute(cmd);
		assertTrue(0 == exec.getErrCode());

		File outputFile = new File("output");
		assertTrue(outputFile.exists());

	}


	public static final void createCoverageSam(final String fileName) throws Exception {
		File file = new File(fileName);

		try (OutputStream os = new FileOutputStream(file);
			PrintStream ps = new PrintStream(os);) {
	
			ps.println("@HD	VN:1.0	SO:coordinate");
			ps.println("@RG	ID:ZZ	SM:ES	DS:rl=50	");
			ps.println("@RG	ID:ZZZ	SM:ES	DS:rl=50	");
			ps.println("@PG	ID:SOLID-GffToSam	VN:1.4.3");
			ps.println("@SQ	SN:chr1	LN:100000");
			ps.println("@SQ	SN:chr2	LN:100000");
			ps.println("@SQ	SN:chr3	LN:100000");
			ps.println("1290_738_1025	0	chr1	54026	255	45M5H	*	0	0	AACATTCCAAAAGTCAACCATCCAAGTTTATTCTAAATAGATGTG	!DDDDDDDDDDDDDDDD''DDDDDD9DDDDDDDDD:<3B''DDD!	RG:Z:ZZ	CS:Z:T301130201000212101113201021003302230033233111	CQ:Z:BBB=B:@5?>B9A5?>B?'A49<475%@;6<+;9@'4)+8'1?:>");
			ps.println("2333_755_492	16	chr2	10103	255	10H40M	*	0	0	CACACCACACCCACACACCACACACCACACCCACACCCAC	!=DD?%+DD<)=DDD<@9)9C:DA.:DD>%%,<?('-,4!	RG:Z:ZZ	CS:Z:T0110001110211110111111111111100111001111	CQ:Z:%/&''(*6'&%+441*%=(31)<9(50=9%%8>?+%;<-1");
			ps.println("1879_282_595	0	chr3	60775	255	40M10H	*	0	0	TCTAAATTTGTTTGATCACATACTCCTTTTCTGGCTAACA	!DD,*@DDD''DD>5:DD>;DDDD=CDD8%%DA9-DDC0!	RG:Z:ZZ	CS:Z:T0223303001200123211133122020003210323011	CQ:Z:=><=,*7685'970/'437(4<:54*:84%%;/3''?;)(");
		}
	}


}
