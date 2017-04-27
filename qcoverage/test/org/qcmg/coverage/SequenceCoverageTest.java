package org.qcmg.coverage;

import static org.junit.Assert.assertTrue;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordCoordinateComparator;
import htsjdk.samtools.SAMRecordQueryNameComparator;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMFileHeader.SortOrder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;
import org.qcmg.gff3.GFF3FileWriter;
import org.qcmg.gff3.GFF3Record;
import org.qcmg.picard.SAMFileReaderFactory;

public class SequenceCoverageTest {
	
	String bam;
	String bai;
	String gff54000To54025;
	String gff54030To54070;
	String gff54077To54120;
	String gff54000To54026;
	String gff54076To54120;
	String gff54000To54036;
	String gff54050To54120;
	final String inputSam1 = "coverage.sam";
	final String inputBam1 = "coverage.bam";
	final String inputIndex1 = "coverage.bai";
 	final String output = "output";
	final String gff3 = "test.gff3";
//	final String cmd =  String.format("--log ./logfile --per-feature -t phys --gff3 %s --bam %s --bai %s -o %s",
//			gff3, inputBam1, inputIndex1,output);

	 @Rule
	 public TemporaryFolder testFolder = new TemporaryFolder();
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public final void before() throws Exception {
		if (null == bam) {
			bam = testFolder.newFile("coverage.bam").getAbsolutePath();
			createCoverageBam(bam, getAACSAMRecords(SortOrder.coordinate), createSamHeaderObject(SortOrder.coordinate));
			bai = bam.replace("bam", "bai");
		}
		if (null == gff54000To54025) {
			File f = testFolder.newFile("gff1");
			gff54000To54025 = f.getAbsolutePath();
			createGFF3File(54000, 54025, f);
			
			File f2 = testFolder.newFile("gff2");
			gff54030To54070 = f.getAbsolutePath();
			createGFF3File(54030, 54070, f2);
			
			File f3 = testFolder.newFile("gff3");
			gff54077To54120 = f.getAbsolutePath();
			createGFF3File(54077, 54120, f3);
			
			File f4 = testFolder.newFile("gff4");
			gff54000To54026 = f.getAbsolutePath();
			createGFF3File(54000, 54026, f4);
			
			File f5 = testFolder.newFile("gff5");
			gff54076To54120 = f.getAbsolutePath();
			createGFF3File(54076, 54120, f5);
			
			File f6 = testFolder.newFile("gff6");
			gff54000To54036 = f.getAbsolutePath();
			createGFF3File(54000, 54036, f6);
			
			File f7 = testFolder.newFile("gff7");
			gff54050To54120 = f.getAbsolutePath();
			createGFF3File(54050, 54120, f7);
		}
	}

//	@After
//	public final void after() {
//		try {
//			File file = new File(inputSam1);
//			file.delete();
//			File bamFile = new File(inputBam1);
//			bamFile.delete();
//			File baiFile = new File(inputIndex1);
//			baiFile.delete();
//		} catch (Exception e) {
//			System.err.println("File creation error in test harness: "
//					+ e.getMessage());
//		}
//	}


	private void createGFF3File(final int start, final int end, File file) throws Exception {
		GFF3Record record = new GFF3Record();
		record.setSeqId("chr1");
		record.setType("exon");
		record.setStart(start);
		record.setEnd(end);
		record.setScore(".");
		record.setSource(".");
		record.setStrand("+");

		try (GFF3FileWriter writer = new GFF3FileWriter(file)) {
			writer.add(record);
		}
	}

	private Executor execute(final String command) throws Exception {
		return new Executor(command, "org.qcmg.coverage.Main");
	}
	
	@Test
	public final void leftDisjointReadSeqCov() throws Exception {
		ExpectedException.none();
		Executor exec = execute("--log ./logfile -t seq --gff3 " + gff54000To54025 + " --bam " + bam + " --bai " + bai + " -o " + testFolder.getRoot().getAbsolutePath()+"/leftDisjointReadSeqCov");
		assertTrue(0 == exec.getErrCode());
		assertTrue(new File(testFolder.getRoot().getAbsolutePath()+"/leftDisjointReadSeqCov").exists());
	}

	private void deleteFile(File outputFile) {
		if (outputFile.exists()) {
			outputFile.delete();
		}
	}

	@Test
	public final void rightDisjointReadSeqCov() throws Exception {
		ExpectedException.none();
		Executor exec = execute("--log ./logfile -t seq --gff3 " + gff54000To54025 + " --bam " + bam + " --bai " + bai + " -o  " + testFolder.getRoot().getAbsolutePath()+"/rightDisjointReadSeqCov");
		assertTrue(0 == exec.getErrCode());

		assertTrue(new File(testFolder.getRoot().getAbsolutePath()+"/rightDisjointReadSeqCov").exists());
	}

	@Test
	public final void leftOnEndSeqCov() throws Exception {

		ExpectedException.none();
		Executor exec = execute("--log ./logfile -t seq --gff3 " + gff54000To54025 + " --bam " + bam + " --bai " + bai + " -o output");
		assertTrue(0 == exec.getErrCode());

		File outputFile = new File("output");
		assertTrue(outputFile.exists());

		deleteFile(outputFile);
	}
	
	@Test
	public final void rightOnEndSeqCov() throws Exception {
//		File file = createGFF3File(54000, 54025);

		ExpectedException.none();
//		Executor exec = execute("--log ./logfile -t seq --gff3 test.gff3 --bam coverage.bam --bai coverage.bai -o output");
		Executor exec = execute("--log ./logfile -t seq --gff3 " + gff54000To54025 + " --bam " + bam + " --bai " + bai + " -o output");
//		Executor exec = execute("--log ./logfile -t seq --gff3 test.gff3 --bam " + bam + " --bai " + bai + " -o output");
		assertTrue(0 == exec.getErrCode());

		File outputFile = new File("output");
		assertTrue(outputFile.exists());

		deleteFile(outputFile);
//		file.delete();
	}

	@Test
	public final void leftOverlapSeqCov() throws Exception {
//		File file = createGFF3File(54000, 54025);

		ExpectedException.none();
		Executor exec = execute("--log ./logfile -t seq --gff3 " + gff54000To54025 + " --bam " + bam + " --bai " + bai + " -o output");
//		Executor exec = execute("--log ./logfile -t seq --gff3 test.gff3 --bam " + bam + " --bai " + bai + " -o output");
//		Executor exec = execute("--log ./logfile -t seq --gff3 test.gff3 --bam coverage.bam --bai coverage.bai -o output");
		assertTrue(0 == exec.getErrCode());

		File outputFile = new File("output");
		assertTrue(outputFile.exists());

		deleteFile(outputFile);
//		file.delete();
	}
	
	@Test
	public final void rightOverlapSeqCov() throws Exception {
//		File file = createGFF3File(54000, 54025);

		ExpectedException.none();
//		Executor exec = execute("--log ./logfile -t seq --gff3 test.gff3 --bam coverage.bam --bai coverage.bai -o output");
		Executor exec = execute("--log ./logfile -t seq --gff3 " + gff54000To54025 + " --bam " + bam + " --bai " + bai + " -o output");
//		Executor exec = execute("--log ./logfile -t seq --gff3 test.gff3 --bam " + bam + " --bai " + bai + " -o output");
		assertTrue(0 == exec.getErrCode());

		File outputFile = new File("output");
		assertTrue(outputFile.exists());

		deleteFile(outputFile);
//		file.delete();
	}

	@Test
	public final void subsetSeqCov() throws Exception {
//		File file = createGFF3File(54000, 54025);

		ExpectedException.none();
//		Executor exec = execute("--log ./logfile -t seq --gff3 test.gff3 --bam coverage.bam --bai coverage.bai -o output");
		Executor exec = execute("--log ./logfile -t seq --gff3 " + gff54000To54025 + " --bam " + bam + " --bai " + bai + " -o output");
//		Executor exec = execute("--log ./logfile -t seq --gff3 test.gff3 --bam " + bam + " --bai " + bai + " -o output");
		assertTrue(0 == exec.getErrCode());

		File outputFile = new File("output");
		assertTrue(outputFile.exists());

		deleteFile(outputFile);
//		file.delete();
	}
	
	@Test
	public final void supersetSeqCov() throws Exception {
//		File file = createGFF3File(54000, 54025);

		ExpectedException.none();
		Executor exec = execute("--log ./logfile -t seq --gff3 " + gff54000To54025 + " --bam " + bam + " --bai " + bai + " -o output");
//		Executor exec = execute("--log ./logfile -t seq --gff3 test.gff3 --bam " + bam + " --bai " + bai + " -o output");
//		Executor exec = execute("--log ./logfile -t seq --gff3 test.gff3 --bam coverage.bam --bai coverage.bai -o output");
		assertTrue(0 == exec.getErrCode());

		File outputFile = new File("output");
		assertTrue(outputFile.exists());

		deleteFile(outputFile);
//		file.delete();
	}

    @Test
	public final void leftDisjointRead() throws Exception {
//		File file = createGFF3File(54000, 54025);

		ExpectedException.none();
//		Executor exec = execute("--log ./logfile -t phys --gff3 test.gff3 --bam coverage.bam --bai coverage.bai -o output");
//		Executor exec = execute("--log ./logfile -t phys --gff3 test.gff3 --bam " + bam + " --bai " + bai + " -o output");
		Executor exec = execute("--log ./logfile -t phys --gff3 " + gff54000To54025 + " --bam " + bam + " --bai " + bai + " -o output");
		assertTrue(0 == exec.getErrCode());

		File outputFile = new File("output");
		assertTrue(outputFile.exists());

		deleteFile(outputFile);
//		file.delete();
	}

    @Test
	public final void rightDisjointRead() throws Exception {
//		File file = createGFF3File(54077, 54120);

		ExpectedException.none();
//		Executor exec = execute("--log ./logfile -t phys --gff3 test.gff3 --bam " + bam + " --bai " + bai + " -o output");
		Executor exec = execute("--log ./logfile -t phys --gff3 " + gff54077To54120 + " --bam " + bam + " --bai " + bai + " -o output");
//		Executor exec = execute("--log ./logfile -t phys --gff3 test.gff3 --bam coverage.bam --bai coverage.bai -o output");
		assertTrue(0 == exec.getErrCode());

		File outputFile = new File("output");
		assertTrue(outputFile.exists());

		deleteFile(outputFile);
//		file.delete();
	}

   
    @Test
	public final void leftOnEndRead() throws Exception {
//		File file = createGFF3File(54000, 54026);

		ExpectedException.none();
//		Executor exec = execute("--log ./logfile -t phys --gff3 test.gff3 --bam coverage.bam --bai coverage.bai -o output");
		Executor exec = execute("--log ./logfile -t phys --gff3 " + gff54000To54026 + " --bam " + bam + " --bai " + bai + " -o output");
//		Executor exec = execute("--log ./logfile -t phys --gff3 test.gff3 --bam " + bam + " --bai " + bai + " -o output");
		assertTrue(0 == exec.getErrCode());

		File outputFile = new File("output");
		assertTrue(outputFile.exists());

		deleteFile(outputFile);
//		file.delete();
	}

    @Test
	public final void rightOnEndRead() throws Exception {
//		File file = createGFF3File(54076, 54120);

		ExpectedException.none();
//		Executor exec = execute("--log ./logfile -t phys --gff3 test.gff3 --bam coverage.bam --bai coverage.bai -o output");
		Executor exec = execute("--log ./logfile -t phys --gff3 " + gff54076To54120 + " --bam " + bam + " --bai " + bai + " -o output");
//		Executor exec = execute("--log ./logfile -t phys --gff3 test.gff3 --bam " + bam + " --bai " + bai + " -o output");
		assertTrue(0 == exec.getErrCode());

		File outputFile = new File("output");
		assertTrue(outputFile.exists());

		deleteFile(outputFile);
//		file.delete();		
	}

    @Test
	public final void leftOverlapRead() throws Exception {
//		File file = createGFF3File(54000, 54036);

		ExpectedException.none();
//		Executor exec = execute("--log ./logfile -t phys --gff3 test.gff3 --bam coverage.bam --bai coverage.bai -o output");
		Executor exec = execute("--log ./logfile -t phys --gff3 " + gff54000To54036 + " --bam " + bam + " --bai " + bai + " -o output");
//		Executor exec = execute("--log ./logfile -t phys --gff3 test.gff3 --bam " + bam + " --bai " + bai + " -o output");
		assertTrue(0 == exec.getErrCode());

		File outputFile = new File("output");
		assertTrue(outputFile.exists());

		deleteFile(outputFile);
//		file.delete();
	}

    @Test
	public final void rightOverlapRead() throws Exception {
//		File file = createGFF3File(54050, 54120);

		ExpectedException.none();
//		Executor exec = execute("--log ./logfile -t phys --gff3 test.gff3 --bam coverage.bam --bai coverage.bai -o output");
		Executor exec = execute("--log ./logfile -t phys --gff3 " + gff54050To54120 + " --bam " + bam + " --bai " + bai + " -o output");
//		Executor exec = execute("--log ./logfile -t phys --gff3 test.gff3 --bam " + bam + " --bai " + bai + " -o output");
		assertTrue(0 == exec.getErrCode());

		File outputFile = new File("output");
		assertTrue(outputFile.exists());

		deleteFile(outputFile);
//		file.delete();
	}

    @Test
	public final void supersetRead() throws Exception {
//		File file = createGFF3File(54050, 54120);

		ExpectedException.none();
//		Executor exec = execute("--log ./logfile -t phys --gff3 test.gff3 --bam coverage.bam --bai coverage.bai -o output");
		Executor exec = execute("--log ./logfile -t phys --gff3 " + gff54050To54120 + " --bam " + bam + " --bai " + bai + " -o output");
//		Executor exec = execute("--log ./logfile -t phys --gff3 test.gff3 --bam " + bam + " --bai " + bai + " -o output");
		assertTrue(0 == exec.getErrCode());

		File outputFile = new File("output");
		assertTrue(outputFile.exists());

		deleteFile(outputFile);
//		file.delete();
	}
    
    @Test
	public final void subsetRead() throws Exception {
//		File file = createGFF3File(54030, 54070);

		ExpectedException.none();
		Executor exec = execute("--log ./logfile -t phys --gff3 " + gff54030To54070 + " --bam " + bam + " --bai " + bai + " -o output");
//		Executor exec = execute("--log ./logfile -t phys --gff3 test.gff3 --bam " + bam + " --bai " + bai + " -o output");
//		Executor exec = execute("--log ./logfile -t phys --gff3 test.gff3 --bam coverage.bam --bai coverage.bai -o output");
		assertTrue(0 == exec.getErrCode());

		File outputFile = new File("output");
		assertTrue(outputFile.exists());

		deleteFile(outputFile);
//		file.delete();
	}

    // TODO: No logging fail
    // TODO: Log level
    // TODO: BAI file inferencing single BAM
    // TODO: Multibam file reading test
    // TODO: BAI File inferencing multiple BAM
    // TODO: strict testing
    // TODO: Malformed testing
    // TODO: Query testing (including strict and malformed and BAI inferencing and multibam)
    // TODO: Perfeature
    // TODO: XML out check
    
//	public final void multireadCoverage() throws Exception {
//	}

//	public static final void createCoverageSam(final String fileName)
//			throws Exception {
//		File file = new File(fileName);
//
//		OutputStream os = new FileOutputStream(file);
//		PrintStream ps = new PrintStream(os);
//
//		ps.println("@HD	VN:1.0	SO:coordinate");
//		ps.println("@RG	ID:ZZ	SM:ES	DS:rl=50	");
//		ps.println("@RG	ID:ZZZ	SM:ES	DS:rl=50	");
//		ps.println("@PG	ID:SOLID-GffToSam	VN:1.4.3");
//		ps.println("@SQ	SN:chr1	LN:100000");
//		ps.println("@SQ	SN:chr2	LN:100000");
//		ps.println("@SQ	SN:chr3	LN:100000");
//		ps
//				.println("1290_738_1025	0	chr1	54026	255	45M5H	*	0	0	AACATTCCAAAAGTCAACCATCCAAGTTTATTCTAAATAGATGTG	!DDDDDDDDDDDDDDDD''DDDDDD9DDDDDDDDD:<3B''DDD!	RG:Z:ZZ	CS:Z:T301130201000212101113201021003302230033233111	CQ:Z:BBB=B:@5?>B9A5?>B?'A49<475%@;6<+;9@'4)+8'1?:>");
//		ps
//				.println("2333_755_492	16	chr2	10103	255	10H40M	*	0	0	CACACCACACCCACACACCACACACCACACCCACACCCAC	!=DD?%+DD<)=DDD<@9)9C:DA.:DD>%%,<?('-,4!	RG:Z:ZZ	CS:Z:T0110001110211110111111111111100111001111	CQ:Z:%/&''(*6'&%+441*%=(31)<9(50=9%%8>?+%;<-1");
//		ps
//				.println("1879_282_595	0	chr3	60775	255	40M10H	*	0	0	TCTAAATTTGTTTGATCACATACTCCTTTTCTGGCTAACA	!DD,*@DDD''DD>5:DD>;DDDD=CDD8%%DA9-DDC0!	RG:Z:ZZ	CS:Z:T0223303001200123211133122020003210323011	CQ:Z:=><=,*7685'970/'437(4<:54*:84%%;/3''?;)(");
//		ps.close();
//		os.close();
//	}
	
	public static final void createCoverageBam(String outputFileName,List<SAMRecord> recs, SAMFileHeader h) {
		File outputFile = new File(outputFileName);
		SAMFileWriterFactory.setDefaultCreateIndexWhileWriting(true);
		try (SAMFileWriter outputWriter = new SAMFileWriterFactory().makeSAMOrBAMWriter(h, true, outputFile)) {
			for (SAMRecord read : recs) {
				outputWriter.addAlignment(read);
			}
		}
	}
	
	 public static SAMFileHeader createSamHeaderObject(SortOrder sort) {
		 SAMSequenceDictionary dict = new SAMSequenceDictionary(
				 Arrays.asList(new SAMSequenceRecord("chr1",100000),
						 new SAMSequenceRecord("chr2",100000),
						 new SAMSequenceRecord("chr3",100000)));
		 
		 SAMFileHeader h = new SAMFileHeader();
		 h.setSequenceDictionary(dict);
		 h.setSortOrder(sort);
		 
		 SAMReadGroupRecord rg1 = new SAMReadGroupRecord("ZZ");
		 rg1.setPlatform("SOLiD");
		 rg1.setLibrary("Library_20100702_A	PI:1355	DS:RUNTYPE{50x50MP}");
		 SAMReadGroupRecord rg2 = new SAMReadGroupRecord("ZZZ");
		 rg2.setPlatform("SOLiD");
		 rg2.setLibrary("Library_20100702_A	PI:1355	DS:RUNTYPE{50x50MP}");
		 
		 h.setReadGroups(Arrays.asList(rg1, rg2));
		 return h;
	 }
	
	
	public static List<SAMRecord> getAACSAMRecords(SortOrder so) {
		SAMFileHeader h = createSamHeaderObject(so); 
		
		SAMRecord s1 = getSAM(h, "1290_738_1025",	0	,"chr1",	54026,	255,	"45M5H",	"*",	0,	0,	"AACATTCCAAAAGTCAACCATCCAAGTTTATTCTAAATAGATGTG","!DDDDDDDDDDDDDDDD''DDDDDD9DDDDDDDDD:<3B''DDD!","ZZ");
		SAMRecord s2 = getSAM(h, "2333_755_492",	16	,"chr2",	10103,	255,	"10H40M",	"*",	0,	0,	"CACACCACACCCACACACCACACACCACACCCACACCCAC","!=DD?%+DD<)=DDD<@9)9C:DA.:DD>%%,<?('-,4!","ZZ");
		SAMRecord s3 = getSAM(h, "1879_282_595",	0	,"chr3",	60775,	255,	"40M10H",	"*",	0,	0,	"TCTAAATTTGTTTGATCACATACTCCTTTTCTGGCTAACA","!DD,*@DDD''DD>5:DD>;DDDD=CDD8%%DA9-DDC0!","ZZ");
		
		return Arrays.asList(s1, s2, s3);
	}
	
	private static SAMRecord getSAM(SAMFileHeader h, String readName,int flags, String chr, int pos, int mapQ, String cigar, String mRef, int mPos, int iSize, String bases, String quals, String md) {
		SAMRecord s1 = new SAMRecord(h);
		s1.setAlignmentStart(pos);
		s1.setCigarString(cigar);
		s1.setBaseQualityString(quals);
		s1.setFlags(flags);
		s1.setMappingQuality(mapQ);
		s1.setInferredInsertSize(iSize);
		s1.setReadName(readName);
		s1.setReferenceName(chr);
		s1.setReadString(bases);
		s1.setAttribute("MD", md);
		s1.setMateReferenceName(mRef);
		s1.setMateAlignmentStart(mPos);
		return s1;
	}



}
