package org.qcmg.qprofiler2;


import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.gff.GFFReader;
import org.qcmg.qprofiler2.QProfiler2;

public class QProfiler2Test {
	
	private static final String DODGY_FILE_NAME =  "solid0039_20091125_DODGY.vcf";
	private static final String XML_FILE_NAME =  "solid0039_20091125_1.xml";
	private static final String GFF_FILE_NAME =  "solid0039_20091125_2.gff";
	private static final String BAM_FILE_NAME =  "solid0039_20091125_2.sam";

	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	public File GFF_FILE, BAM_FILE, XML_FILE, DODGY_FILE ;
	
	@Before
	public void setup() throws IOException {
		GFF_FILE = testFolder.newFile(GFF_FILE_NAME);
		BAM_FILE = testFolder.newFile(BAM_FILE_NAME);
		XML_FILE = testFolder.newFile(XML_FILE_NAME);
		DODGY_FILE = testFolder.newFile(DODGY_FILE_NAME);
		
		createTestFile(DODGY_FILE, getDodgyFileContents());	
		createTestFile(BAM_FILE, null);	
	}
	
	@After
	public void cleanup() throws IOException {
		//delete qprofiler.xml which was default output
		
		String USER_DIR = System.getProperty("user.dir");	
		String outputFile = ".xml";
		
		File[] files = (new File(USER_DIR)).listFiles(f->f.getName().contains(outputFile) );
		for(File f : files) f.delete();
		
	}

	@Test
	public final void executeWithValidArguments() throws Exception {
		File logFile = testFolder.newFile("executeWithValidArguments.log");
		File outputFile = testFolder.newFile("executeWithValidArguments.xml");
		//qprofiler2 accept two same input once type are XML, VCF, FASTQ or BAM
		String[] args = {"-log",  logFile.getAbsolutePath(), "-input", BAM_FILE.getAbsolutePath(),
				"-input", BAM_FILE.getAbsolutePath(), "-o", outputFile.getAbsolutePath()};
		int exitStatus = new QProfiler2().setup(args);
		assertEquals(0, exitStatus);
		
		//argument are correct input is doggy , the exception are caught
		args = new String[] {"-log",  logFile.getAbsolutePath(), "-input", DODGY_FILE.getAbsolutePath(),
				 "-o", outputFile.getAbsolutePath()};
		exitStatus = new QProfiler2().setup(args);
		assertEquals(1, exitStatus);
		
		assertTrue(outputFile.exists());
	}
	
	@Test
	public final void executeWithNoArgs() throws Exception {
		String[] args = {};
		try {
			int exitStatus = new QProfiler2().setup(args);
			assertEquals(1, exitStatus);
		} catch (Exception e) {
			fail("no exception should have been thrown from executeWithNoArgs()");
		}
	}
		
	@Test
	public final void executeWithInvalidArgs() throws Exception {
		File logFile = testFolder.newFile("executeWithInvalidArguments.log");
				
		String[] args2 = new String[]{"-input", BAM_FILE.getAbsolutePath(), "-log",logFile.getAbsolutePath(), "-include","html,all,matricies,coverage", };
		try {
			int exitStatus =new QProfiler2().setup(args2);
			fail("no exception should have been thrown from executeWithExcludeArgs()");			 
		} catch (Exception e) {
			assertEquals("'i' is not a recognized option", e.getMessage() );
		}		
	}
	
	@Test
	public final void executeWithInvalidFileType() throws Exception {
		File logFile = testFolder.newFile("executeWithInvalidFileType.log");
		File inputFile = testFolder.newFile("executeWithInvalidFileType.test");
		
		String[] args1 = {"-input",inputFile.getAbsolutePath(), "-log", logFile.getAbsolutePath()};
		String[] args2 = new String[] {"-input",GFF_FILE.getAbsolutePath(), "-log", logFile.getAbsolutePath()};	
		
		for(String[] args : new String[][] {args1, args2})
			try {
				new QProfiler2().setup(args);
				fail("Should have thrown a QProfilerException");
			} catch (Exception qpe) {
				assertTrue(qpe.getMessage().contains( "Unsupported file type" ));
			}
				
	}
	
	@Test
	public final void executeWithNonexistantInputFile() throws Exception {
		File logFile = testFolder.newFile("executeWithNonexistantInputFile.log");
		
		String[] args = {"-input","test123.sam", "-log", logFile.getAbsolutePath()};
		try {
			new QProfiler2().setup(args);
			fail("Should have thrown a QProfilerException");
		} catch (Exception qpe) {
			qpe.printStackTrace();
			assertTrue(qpe.getMessage().startsWith("Cannot read supplied input file"));
		}
	}
	
	private static void createTestFile(File file, List<String> data) {
		
		if(data == null) {		
			data = new ArrayList<String>();
	        data.add("@HD	VN:1.0	SO:coordinate");
	        data.add("@RG	ID:1959T	SM:eBeads_20091110_CD	DS:rl=50");
	        data.add("@RG	ID:1959N	SM:eBeads_20091110_ND	DS:rl=50");
	        data.add("@PG	ID:SOLID-GffToSam	VN:1.4.3");
	        data.add("@SQ	SN:chr1	LN:249250621");
	        data.add("@SQ	SN:chr11	LN:243199373");
			//unmapped
			data.add("243_146_1	101	chr1	10075	0	*	=	10167	0	" +		 
					"ACCCTAACCCTAACCCTAACCNTAACCCTAACCCAAC	+3?GH##;9@D7HI5,:IIB\"!\"II##>II$$BIIC3	" +
					"RG:Z:1959T	CS:Z:T11010020320310312010320010320013320012232201032202	CQ:Z:**:921$795*#5:;##):<5&'/=,,9(2*#453-'%(.2$6&39$+4'");
		}
                
		try(BufferedWriter out = new BufferedWriter(new FileWriter(file))){	    
			for (String line : data)  out.write(line + "\n");	               
		}catch (IOException e) {
			System.err.println("IOException caught whilst attempting to write to test file: " + file.getAbsolutePath());
			e.printStackTrace();
		}
		
	}
	
	private List<String> getDodgyFileContents() {
		List<String> data = new ArrayList<String>();
		data.add("##gff-version 3");
		data.add("##solid-gff-version 3.5");
		data.add("##source-version Gff3Merger 0.1");
		data.add("##date 2010-03-10");
		data.add("##time 15:04:36");
		data.add("##reference-file /path/reference/hg19.fa");
		data.add("##color-code AA=0,AC=1,AG=2,AT=3,CA=1,CC=0,CG=3,CT=2,GA=2,GC=3,GG=0,GT=1,TA=3,TC=2,TG=1,TT=0");
		data.add("##primer-base F3=T");
		data.add("##max-num-mismatches 10");
		data.add("##max-read-length 50");
		data.add("##line-order fragment");
		data.add("##contig 1 chr1");
		data.add("##contig 2 chr2");
		data.add("##contig 3 chr3");
		data.add("##contig 4 chr4");
		data.add("##contig 5 chr5");
		data.add("##contig 6 chr6");
		data.add("##contig 7 chr7");
		data.add("##contig 8 chr8");
		data.add("##contig 9 chr9");
		data.add("##contig 10 chr10");
		data.add("##contig 11 chr11");
		data.add("##contig 12 chr12");
		data.add("##contig 13 chr13");
		data.add("##contig 14 chr14");
		data.add("##contig 15 chr15");
		data.add("##contig 16 chr16");
		data.add("##contig 17 chr17");
		data.add("##contig 18 chr18");
		data.add("##contig 19 chr19");
		data.add("##contig 20 chr20");
		data.add("##contig 21 chr21");
		data.add("##contig 22 chr22");
		data.add("##contig 23 chrX");
		data.add("	##contig 24 chrY");
		data.add("	##contig 25 chrM");
		data.add("##conversion unique");
		data.add("##clear-zone 5");
		data.add("##history AnnotateGff3Changes.java /path/solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD_F3/outputs/../intermediate_maToGff3/output.325614874493032/output.0/solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD_F3.csfasta.ma.0.gff3 /path/reference/hg19.fa --out=/path/solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD_F3/outputs/../intermediate_maToGff3/output.325614874493032/output.0/solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD_F3.csfasta.ma.0.annotated.gff3 --tints=agy --qvThreshold=15 --b=true --cn=true");
		data.add("##history mapping/solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD_F3.csfasta.ma 50 3 0.200000 B=1.000000 P=1 L=25 F=0 m=-2.000000");
		data.add("##history MaToGff3.java /path/solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD_F3/temp_maToGff3/split.323675630031032/temp.0/solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD_F3.csfasta.ma.0 --out=/path/solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD_F3/outputs/../intermediate_maToGff3/output.325614874493032/output.0/solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD_F3.csfasta.ma.0.gff3 --clear=5 --qvs=/data/results/Aditya/gabe/gabe_raw_reads/solid0039_20091125_2_TD04_LMP_F3/solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD_F3_QV.qual --mmp=-2.0 --tempdir=/scratch/solid");
		data.add("##history filter_fasta.pl --output=/data/results/solid0039/solid0039_20091125_2_TD04_LMP/eBeads_20091110_CD/results.01/primary.20091221012849245 --name=solid0039_20091125_2_TD04_LMP_eBeads_20091110_CD --tag=F3 --minlength=50 --mincalls=25 --prefix=T /data/results/solid0039/solid0039_20091125_2_TD04_LMP/eBeads_20091110_CD/jobs/postPrimerSetPrimary.937/rawseq");
		data.add("##hdr seqid	source	type	start	end	score	strand	phase	attributes");
		data.add("1	solid	read	10148	10190	14.4	-	.	aID=1212_1636_246;at=F3;b=GGTTAGGGTTAGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG;g=G0103200103201032001033001032001032001032001032001;mq=43;o=0;q=31,30,32,26,26,26,23,24,29,31,31,23,25,18,14,20,18,11,27,22,18,23,2,18,29,20,25,11,19,18,18,13,14,18,19,16,14,5,16,23,18,21,16,16,14,20,13,17,20,11;r=23_2;s=a23;u=0,4,1,1");
		data.add("1	solid	read	10236	10275	19.6	-	.	aID=681_1482_392;at=F3;b=TTAGGGTTAGGGTTAGGGTTTAGGGTTTAGGGTTAGGGTT;g=T0320010320010320010032001003200103200100320000320;mq=18;o=0;q=25,27,28,29,25,27,28,28,23,26,27,26,27,28,28,24,28,24,30,26,20,29,23,25,27,10,27,26,22,24,20,13,23,24,29,17,26,26,23,27,25,13,22,23,27,8,16,28,20,26;u=8,0,46,1,27,3,1");
		data.add("1	solid	read	10248	10290	10.5	+	.	aID=1578_430_829;at=F3;b=AAACCCTAAACCCTAACCCTAACCCTAACCCTAACCCTAACCC;g=A0010023001002301003301002301002300002301002300003;mq=72;o=0;q=8,23,18,16,18,14,20,21,20,9,11,28,14,18,11,14,7,15,13,5,14,20,5,21,8,11,11,14,8,25,22,7,5,20,7,20,8,11,8,11,7,13,11,14,8,11,20,21,8,5;r=20_2,35_1;s=a20,a35;u=0,0,1");
		data.add("1	solid	read	10456	10480	9.4	+	.	aID=266_752_1391;at=F3;b=TAACCCTAACCCTCGCGGTACCCTC;g=T1303013131001013013023010022333003100220000000202;mq=7;o=15;q=13,11,20,18,15,5,25,16,16,31,8,7,8,14,6,26,11,5,5,11,5,10,5,5,8,18,15,8,24,7,7,19,7,26,10,19,21,5,8,8,8,21,8,18,7,20,19,14,9,14;r=20_0,34_1;s=a20,a34;u=0,0,1");
		data.add("1	solid	read	13290	13336	7.9	-	.	");
		data.add("1	solid	read	13301	13342	4.9	+	.	aID=1986_1440_4;at=F3;b=ACGCTGTTGGCCTGGATCTGAGCCCTGGgtGAGGTCAAAGCC;g=A1333110103021023221223202100112201213023020022210;mq=21;o=0;q=13,2,15,5,3,11,13,4,9,7,19,12,2,7,3,2,11,11,3,4,19,8,6,2,11,9,2,4,2,2,4,4,4,2,9,16,4,2,2,19,5,2,4,2,2,2,13,17,11,6;r=5_2,24_0,29_1,31_0,38_0;rb=29_T,30_G;s=a5,a24,y29,y30,y31,a38;u=0,0,0,0,0,1,0,1");
		data.add("1	solid	read	14933	14973	9.5	+	.	aID=2140_530_759;at=F3;b=GTGCTGGCCCAGGGCGGGCAGCGGCCCTGCCTCCTACCCTT;g=G1132103001200330031233230022302202312020222222202;mq=54;o=0;q=27,20,20,17,21,22,13,15,6,22,20,19,3,9,25,23,14,17,2,25,24,7,2,3,25,26,5,18,8,22,25,21,20,14,5,26,5,5,23,20,23,19,19,13,5,13,20,16,8,5;r=24_0,29_1,38_0;s=a24,a29,a38;u=0,0,0,1");
		return data;
	}
	
}
