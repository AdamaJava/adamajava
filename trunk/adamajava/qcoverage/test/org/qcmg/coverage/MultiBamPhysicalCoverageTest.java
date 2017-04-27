package org.qcmg.coverage;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMFileHeader.SortOrder;

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

public class MultiBamPhysicalCoverageTest {
	final String output = "output";
	String gff3;
	String cmd;
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	 @Rule
	 public TemporaryFolder testFolder = new TemporaryFolder();

	@Before
	public final void before() {
		try {
			if (cmd == null) {
				String inputBam1 = testFolder.newFile("coverage.bam").getAbsolutePath();
				String inputIndex1 = inputBam1.replace("bam", "bai");
				String inputBam2 = testFolder.newFile("coverage2.bam").getAbsolutePath();
				String inputIndex2 = inputBam2.replace("bam", "bai");
				gff3 = testFolder.newFile("test.gff3").getAbsolutePath();

				
				createCoverageBam(inputBam1, getAACSAMRecords(SortOrder.coordinate), createSamHeaderObject(SortOrder.coordinate));
				createCoverageBam(inputBam2, getAACSAMRecords(SortOrder.coordinate), createSamHeaderObject(SortOrder.coordinate));
				
				cmd =  String.format("--log ./logfile -t phys --gff3 %s --bam %s --bai %s --bam %s --bai %s -o %s",
						gff3, inputBam1, inputIndex1, inputBam2, inputIndex2,output);
			}
			
		} catch (IOException e) {
			System.err.println("File creation error in test harness: "
					+ e.getMessage());
		}
	}

	@After
	public final void after() {
		try {
			new File(gff3).delete();
			new File("output").delete();
		} catch (SecurityException e) {
			System.err.println("File deleting error in test harness: "
					+ e.getMessage());
		}
 
		
	}

	private void createGFF3File(final int start, final int end) throws Exception {
		GFF3Record record = new GFF3Record();
		record.setSeqId("chr1");
		record.setType("exon");
		record.setStart(start);
		record.setEnd(end);
		record.setScore(".");
		record.setSource(".");
		record.setStrand("+");

		try (GFF3FileWriter writer = new GFF3FileWriter(new File(gff3))) {
			writer.add(record);
		}
	}

	private Executor execute(final String command) throws Exception {
		return new Executor(command, "org.qcmg.coverage.Main");
	}
	
    @Test
	public final void leftDisjointRead() throws Exception {
		createGFF3File(54000, 54025);
		
		ExpectedException.none();
		Executor exec = execute(cmd );
				
		//debug
		System.out.println(exec.getOutputStreamConsumer());
		System.err.println(exec.getErrorStreamConsumer());
		
		assertTrue(0 == exec.getErrCode());
		assertTrue( new File("output").exists());
	}

    @Test
	public final void rightDisjointRead() throws Exception {
		createGFF3File(54077, 54120);

		ExpectedException.none();
		Executor exec = execute(cmd);
 
		assertTrue(0 == exec.getErrCode());

		assertTrue( new File("output").exists());

	}

   
    @Test
	public final void leftOnEndRead() throws Exception {
		createGFF3File(54000, 54026);

		ExpectedException.none();
		Executor exec = execute(cmd);

		assertTrue(0 == exec.getErrCode());

		assertTrue( new File("output").exists());

	}

    @Test
	public final void rightOnEndRead() throws Exception {

		ExpectedException.none();
		Executor exec = execute(cmd);
		assertTrue(0 == exec.getErrCode());

		assertTrue( new File("output").exists());

	}

    @Test
	public final void leftOverlapRead() throws Exception {
		createGFF3File(54000, 54036);

		ExpectedException.none();
		Executor exec = execute(cmd);
		assertTrue(0 == exec.getErrCode());

		assertTrue( new File("output").exists());

	}

    @Test
	public final void rightOverlapRead() throws Exception {
		createGFF3File(54050, 54120);

		ExpectedException.none();
		Executor exec = execute(cmd);
		assertTrue(0 == exec.getErrCode());

		assertTrue( new File("output").exists());

	}

    @Test
	public final void supersetRead() throws Exception {
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

		assertTrue( new File("output").exists());

	}

    @Test
	public final void leftDisjointReadInferBai() throws Exception {
		createGFF3File(54000, 54025);

		ExpectedException.none();
		Executor exec = execute(cmd);
		assertTrue(0 == exec.getErrCode());

		assertTrue( new File("output").exists());

	}

    @Test
	public final void rightDisjointReadInferBai() throws Exception {
		createGFF3File(54077, 54120);

		ExpectedException.none();
		Executor exec = execute(cmd);
		assertTrue(0 == exec.getErrCode());

		assertTrue( new File("output").exists());

	}

   
    @Test
	public final void leftOnEndReadInferBai() throws Exception {
		createGFF3File(54000, 54026);

		ExpectedException.none();
		Executor exec = execute(cmd);
		assertTrue(0 == exec.getErrCode());

		assertTrue( new File("output").exists());

	}

    @Test
	public final void rightOnEndReadInferBai() throws Exception {
		createGFF3File(54076, 54120);

		ExpectedException.none();
		Executor exec = execute(cmd);
		assertTrue(0 == exec.getErrCode());

		assertTrue( new File("output").exists());

	}

    @Test
	public final void leftOverlapReadInferBai() throws Exception {
		createGFF3File(54000, 54036);

		ExpectedException.none();
		Executor exec = execute(cmd);
		assertTrue(0 == exec.getErrCode());

		assertTrue( new File("output").exists());

	}

    @Test
	public final void rightOverlapReadInferBai() throws Exception {
		createGFF3File(54050, 54120);

		ExpectedException.none();
		Executor exec = execute(cmd);
		assertTrue(0 == exec.getErrCode());

		assertTrue( new File("output").exists());

	}

    @Test
	public final void supersetReadInferBai() throws Exception {
		createGFF3File(54050, 54120);

		ExpectedException.none();
		Executor exec = execute(cmd);
		assertTrue(0 == exec.getErrCode());

		assertTrue( new File("output").exists());

	}
    
    @Test
	public final void subsetReadInferBai() throws Exception {
		createGFF3File(54030, 54070);

		ExpectedException.none();
		Executor exec = execute(cmd);
		assertTrue(0 == exec.getErrCode());

		assertTrue( new File("output").exists());

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
    

	public static final void createCoverageBam(final String inputFileName,
			final String outputFileName, final String index) throws Exception {
		File inputFile = new File(inputFileName);
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(inputFile);
		File outputFile = new File(outputFileName);
		
		SAMFileWriterFactory.setDefaultCreateIndexWhileWriting(true);
//		SAMFileWriterFactory factory = new SAMFileWriterFactory().setCreateIndex(true);		
		SAMFileWriter outputWriter = new SAMFileWriterFactory().makeSAMOrBAMWriter(reader
				.getFileHeader(), true, outputFile);
		for (SAMRecord read : reader) {
			outputWriter.addAlignment(read);
		}
		outputWriter.close();
		reader.close();
		
	}
	
	
	
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
