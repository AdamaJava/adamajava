package org.qcmg.snp;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;
import org.qcmg.common.model.Accumulator;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMLineParser;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

public class StandardPipelineTest {
	
	static SAMFileHeader header;
	File referenceFile;
	
	@Before
	public  void setupHeader() throws IOException {
		header = new SAMFileHeader();
		SAMSequenceDictionary dict = new SAMSequenceDictionary();
		dict.addSequence(new SAMSequenceRecord("chr1", 800000));
		dict.addSequence(new SAMSequenceRecord("chr2", 800000));
		dict.addSequence(new SAMSequenceRecord("chr3", 180000));
		dict.addSequence(new SAMSequenceRecord("chr4", 800000));
		dict.addSequence(new SAMSequenceRecord("chr5", 1800000));
		dict.addSequence(new SAMSequenceRecord("chr6", 1800000));
		dict.addSequence(new SAMSequenceRecord("chr7", 1200000));
		dict.addSequence(new SAMSequenceRecord("chr8", 4400000));
		dict.addSequence(new SAMSequenceRecord("chr9", 1200000));
		dict.addSequence(new SAMSequenceRecord("chr10", 1200000));
		dict.addSequence(new SAMSequenceRecord("chr11", 4900000));
		dict.addSequence(new SAMSequenceRecord("chr12", 8000000));
		dict.addSequence(new SAMSequenceRecord("chr13", 1200000));
		dict.addSequence(new SAMSequenceRecord("chr14", 120000));
		dict.addSequence(new SAMSequenceRecord("chr15", 1200000));
		dict.addSequence(new SAMSequenceRecord("chr16", 120000));
		dict.addSequence(new SAMSequenceRecord("chr17", 120000));
		dict.addSequence(new SAMSequenceRecord("chr18", 120000));
		dict.addSequence(new SAMSequenceRecord("chr19", 120000));
		dict.addSequence(new SAMSequenceRecord("chr20", 270000));
		dict.addSequence(new SAMSequenceRecord("chrX", 6200000));
		dict.addSequence(new SAMSequenceRecord("GL000208.1", 4340));
		header.setSequenceDictionary(dict);
		header.setSortOrder(SortOrder.coordinate);
		
		referenceFile = createRefFile("StandardPipelineTest.reference.fa");
	}
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void testRunStandardMode() throws Exception{
		final File logFile = testFolder.newFile("qsnp.log");
		final File normalBam = createCoverageBam("normal.bam");
		checkBam(normalBam);
		final File tumourBam = createTumourCoverageBam("tumour.bam");
		checkBam(tumourBam);
		final File ini = testFolder.newFile("ini.ini");
		final File vcf = testFolder.newFile("output.vcf");
		IniFileGenerator.createRulesOnlyIni(ini);
		IniFileGenerator.addInputFiles(ini, false, "ref = " + referenceFile.getAbsolutePath());
		IniFileGenerator.addInputFiles(ini, false, "controlBam = " + normalBam.getAbsolutePath());
		IniFileGenerator.addInputFiles(ini, false, "testBam = " + tumourBam.getAbsolutePath());
		IniFileGenerator.addOutputFiles(ini, false, "vcf = " + vcf.getAbsolutePath());
		IniFileGenerator.addStringToIniFile(ini, "[parameters]\nrunMode = standard\nfilter = and(Flag_DuplicateRead==false, Cigar_M > 34, MD_mismatch <= 3, MAPQ > 10)", true);
		
		final String command = "-log " + logFile.getAbsolutePath() + " -i " + ini.getAbsolutePath();
		final Executor exec = new Executor(command, "org.qcmg.snp.Main");
		assertEquals(0, exec.getErrCode());
		List<String> vcfs = Files.lines(Paths.get(vcf.getPath())).filter(s -> ! s.startsWith("#")).collect(Collectors.toList());
		
		vcfs.stream().forEach(System.out::println);
		
		assertEquals(60, vcfs.size());
		AtomicInteger ai = new AtomicInteger(0);
		IntStream.of(1,2,4).forEach(i -> {
			assertEquals("chr" + i + "	770567	.	A	T	.	.	FLANK=AAAAATAAAAA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	./.:.:.:.:.:.:.:.:.	1/1:0,4:4:.:.:.:SOMATIC:4:T1[40]3[40]", vcfs.get(ai.getAndIncrement()));
			assertEquals("chr" + i + "	770568	.	A	G	.	.	FLANK=AAAAAGAAAAA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	./.:.:.:.:.:.:.:.:.	1/1:0,4:4:.:.:.:SOMATIC:4:G1[37]3[40]", vcfs.get(ai.getAndIncrement()));
			assertEquals("chr" + i + "	770569	.	A	T	.	.	FLANK=AAAAATAAAAA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	./.:.:.:.:.:.:.:.:.	1/1:0,4:4:.:.:.:SOMATIC:4:T1[39]3[40]", vcfs.get(ai.getAndIncrement()));
			assertEquals("chr" + i + "	770570	.	A	G	.	.	FLANK=AAAAAGAAAAA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	./.:.:.:.:.:.:.:.:.	1/1:0,4:4:.:.:.:SOMATIC:4:G1[40]3[40]", vcfs.get(ai.getAndIncrement()));
			assertEquals("chr" + i + "	770571	.	A	G	.	.	FLANK=AAAAAGAAAAA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	./.:.:.:.:.:.:.:.:.	1/1:0,4:4:.:.:.:SOMATIC:4:G1[40]3[38.67]", vcfs.get(ai.getAndIncrement()));
			assertEquals("chr" + i + "	770573	.	A	G	.	.	FLANK=AAAAAGAAAAA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	./.:.:.:.:.:.:.:.:.	1/1:0,3:3:.:G1:.:SOMATIC:3:G0[0]3[40]", vcfs.get(ai.getAndIncrement()));
			assertEquals("chr" + i + "	770575	.	A	G	.	.	FLANK=AAAAAGAAAAA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	./.:.:.:.:.:.:.:.:.	1/1:0,4:4:.:.:.:SOMATIC:4:G1[40]3[40]", vcfs.get(ai.getAndIncrement()));
			assertEquals("chr" + i + "	770579	.	A	G	.	.	FLANK=AAAAAGAAAAA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	./.:.:.:.:.:.:.:.:.	1/1:0,4:4:.:.:.:SOMATIC:4:G1[36]3[40]", vcfs.get(ai.getAndIncrement()));
			assertEquals("chr" + i + "	770580	.	A	T	.	.	FLANK=AAAAATAAAAA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	./.:.:.:.:.:.:.:.:.	1/1:0,4:4:.:.:.:SOMATIC:4:T1[40]3[40]", vcfs.get(ai.getAndIncrement()));
			assertEquals("chr" + i + "	770583	.	A	G	.	.	FLANK=AAAAAGAAAAA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	./.:.:.:.:.:.:.:.:.	1/1:0,4:4:.:.:.:SOMATIC:4:G1[34]3[40]", vcfs.get(ai.getAndIncrement()));
			assertEquals("chr" + i + "	770584	.	A	C	.	.	FLANK=AAAAACAAAAA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	./.:.:.:.:.:.:.:.:.	1/1:0,4:4:.:.:.:SOMATIC:4:C1[35]3[40]", vcfs.get(ai.getAndIncrement()));
			assertEquals("chr" + i + "	770585	.	A	G	.	.	FLANK=AAAAAGAAAAA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	./.:.:.:.:.:.:.:.:.	1/1:0,4:4:.:.:.:SOMATIC:4:G1[40]3[40]", vcfs.get(ai.getAndIncrement()));
			assertEquals("chr" + i + "	770586	.	A	G	.	.	FLANK=AAAAAGAAAAA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	./.:.:.:.:.:.:.:.:.	1/1:0,4:4:.:.:.:SOMATIC:4:G1[40]3[40]", vcfs.get(ai.getAndIncrement()));
			assertEquals("chr" + i + "	770587	.	A	C	.	.	FLANK=AAAAACAAAAA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	./.:.:.:.:.:.:.:.:.	1/1:0,4:4:.:.:.:SOMATIC:4:C1[40]3[40]", vcfs.get(ai.getAndIncrement()));
			assertEquals("chr" + i + "	770591	.	A	T	.	.	FLANK=AAAAATAAAAA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	./.:.:.:.:.:.:.:.:.	1/1:0,4:4:.:.:.:SOMATIC:4:T1[40]3[40]", vcfs.get(ai.getAndIncrement()));
			assertEquals("chr" + i + "	770592	.	A	G	.	.	FLANK=AAAAAGAAAAA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	./.:.:.:.:.:.:.:.:.	1/1:0,4:4:.:.:.:SOMATIC:4:G1[40]3[40]", vcfs.get(ai.getAndIncrement()));
			assertEquals("chr" + i + "	770593	.	A	T	.	.	FLANK=AAAAATAAAAA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	./.:.:.:.:.:.:.:.:.	1/1:0,4:4:.:.:.:SOMATIC:4:T1[29]3[40]", vcfs.get(ai.getAndIncrement()));
			assertEquals("chr" + i + "	770594	.	A	G	.	.	FLANK=AAAAAGAAAAA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	./.:.:.:.:.:.:.:.:.	1/1:0,4:4:G0[]1[]:.:.:SOMATIC:4:G1[23]3[40]", vcfs.get(ai.getAndIncrement()));
			assertEquals("chr" + i + "	770595	.	A	G	.	.	FLANK=AAAAAGAAAAA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	./.:.:.:.:.:.:.:.:.	1/1:0,4:4:G0[]1[]:.:.:SOMATIC:4:G1[40]3[40]", vcfs.get(ai.getAndIncrement()));
			assertEquals("chr" + i + "	770596	.	A	C	.	.	FLANK=AAAAACAAAAA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	./.:.:.:.:.:.:.:.:.	1/1:0,4:4:C0[]1[]:.:.:SOMATIC:4:C1[40]3[40]", vcfs.get(ai.getAndIncrement()));
			
		});
	}
	
	@Test
	public void testNumbers() throws IOException, InterruptedException {
		
		final File logFile = testFolder.newFile("qsnp.log");
		final File normalBam = createCoverageGL208("normal.bam", "gl208.sams");
		checkBam(normalBam);
		final File tumourBam = createCoverageGL208("tumour.bam", "gl208.sams");
		checkBam(tumourBam);
		final File ini = testFolder.newFile("ini.ini");
		final File vcf = testFolder.newFile("output.vcf");
		IniFileGenerator.createRulesOnlyIni(ini);
		IniFileGenerator.addInputFiles(ini, false, "ref = " + referenceFile.getAbsolutePath());
		IniFileGenerator.addInputFiles(ini, false, "controlBam = " + normalBam.getAbsolutePath());
		IniFileGenerator.addInputFiles(ini, false, "testBam = " + tumourBam.getAbsolutePath());
		IniFileGenerator.addOutputFiles(ini, false, "vcf = " + vcf.getAbsolutePath());
		IniFileGenerator.addStringToIniFile(ini, "[parameters]\nrunMode = standard\nfilter = and(Flag_DuplicateRead==false, Cigar_M > 34, MD_mismatch <= 3, MAPQ > 10)", true);
		
		final String command = "-log " + logFile.getAbsolutePath() + " -i " + ini.getAbsolutePath();
		final Executor exec = new Executor(command, "org.qcmg.snp.Main");
		assertEquals(0, exec.getErrCode());
		List<String> vcfs = Files.lines(Paths.get(vcf.getPath())).filter(s -> ! s.startsWith("#")).collect(Collectors.toList());
		
		vcfs.stream().forEach(System.out::println);
		
		assertEquals(4, vcfs.size());
		assertEquals("GL000208.1	53	.	T	G	.	.	FLANK=AAGTGGTTCAA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	0/1:115,10:125:T1[]7[]:C1;G51;T89:.:.:9:G2[41]8[40.5];T9[35.22]106[39.88]	0/1:115,10:125:T1[]7[]:C1;G51;T89:.:.:9:G2[41]8[40.5];T9[35.22]106[39.88]", vcfs.get(0));
//		assertEquals("GL000208.1	77	.	T	C	.	.	FLANK=AAAGACGTATT	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	1/1:4,94:98:C0[]3[];T0[]1[]:C171;T11:.:.:64:C9[35.78]85[39.94];T1[41]3[39.67]	1/1:4,94:98:C0[]3[];T0[]1[]:C171;T11:.:.:64:C9[35.78]85[39.94];T1[41]3[39.67]", vcfs.get(1));
//		assertEquals("GL000208.1	84	.	C	A	.	.	FLANK=TATTCAACTCA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	0/1:22,71:93:A2[]6[]:A178;C6:.:.:50:A8[38.62]63[38.92];C2[36.5]20[40.2]	0/1:22,71:93:A2[]6[]:A178;C6:.:.:50:A8[38.62]63[38.92];C2[36.5]20[40.2]", vcfs.get(2));
//		assertEquals("GL000208.1	98	.	C	A	.	.	FLANK=ACTTTAATGCA	GT:AD:DP:EOR:FF:FT:INF:NNS:OABS	1/1:0,83:83:A0[]8[]:A188;G1:.:.:59:A8[37]75[38.63]	1/1:0,83:83:A0[]8[]:A188;G1:.:.:59:A8[37]75[38.63]", vcfs.get(3));
	}
	
	@Ignore
	public void testContainsAndRemove() {
		final ConcurrentMap<Integer, Accumulator> map = new ConcurrentHashMap<Integer, Accumulator>();
		final int noOfLoops = 100000;
		long time = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			map.remove(i);
		}
		System.out.println("EMPTY: remove time: " + (System.currentTimeMillis() - time));
		
		time = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			if (map.containsKey(i))
				map.remove(i);
		}
		System.out.println("EMPTY: contains and remove time: " + (System.currentTimeMillis() - time));
		
		time = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			map.remove(i);
		}
		System.out.println("EMPTY: remove time: " + (System.currentTimeMillis() - time));
	}
	
	public void checkBam(File f) {
		SamReader reader = SamReaderFactory.makeDefault().open(f);
		assertEquals(SortOrder.coordinate, reader.getFileHeader().getSortOrder());
	}
	
	public final File createCoverageBam(final String fileName) throws IOException {
		final File file = testFolder.newFile(fileName);
		SAMReadGroupRecord rg1 = new SAMReadGroupRecord("20100803052556101");
		rg1.setSample("ES");
		SAMReadGroupRecord rg2 = new SAMReadGroupRecord("20100803052545338");
		rg2.setSample("ES");
		header.setReadGroups(Arrays.asList(rg1, rg2));
		SAMLineParser parser = new SAMLineParser(header);
		SAMFileWriterFactory.setDefaultCreateIndexWhileWriting(true);
		SAMFileWriter writer = new SAMFileWriterFactory().makeBAMWriter(header, true, file);
		writer.addAlignment(parser.parseLine("841_1342_589	65	chr1	10177	18	50M	chr17	10708	0	ATCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACC	IAIII1;CIIIIIIIIIIIIIIIII7D@AIGDDCI**IDIIII@%%BIG3	ZC:i:11	MD:Z:1C48	RG:Z:20100803052556101	NH:i:2	CM:i:4	NM:i:1	SM:i:16	ZP:Z:Z**	CQ:Z:59)B?+'5/=599>6:>;B>85?A;/)<%=>*;*:8*@+:1><9(%):53	CS:Z:T33223010023010023010023010023010023030023010033010"));
		writer.addAlignment(parser.parseLine("710_1533_1074	115	chr1	10201	10	50M	=	11271	1120	CCCTAACCCTAACCCTAACCCTAACCCTAACCCCTAACCCTAACCCTAAA	=IIIIIIIIIIIIIIIIHIIIIICIIIIIIIIIIIIIIIIIIIIIIIIII	ZC:i:10	MD:Z:50	RG:Z:20100803052545338	NH:i:2	CM:i:0	NM:i:0	SM:i:16	ZP:Z:AAA	CQ:Z:>BAAB@BAAB>BB>B>?@A<4;;<B<22<>A<72@2??7?38B=6995A=	CS:Z:T00032001032001032000103200103200103200103200103200"));
		writer.addAlignment(parser.parseLine("1551_1209_1505	161	chr1	10308	30	45M5H	chrX	608650	0	CCAACCCCAACCCCAACCCTAAACCCTAACCCTAACCCTAACCCT	IIIIIIIIIIIIIIIIIIIHIIIIIII==III@;++I=5--8IA1	ZC:i:11	MD:Z:22C22	RG:Z:20100803052556101	NH:i:2	CM:i:3	NM:i:1	SM:i:20	ZP:Z:Z**	CQ:Z:BBBBBBBBBB9BBBB;@BB:/B:<A5<///@<2/-+B//'''2;'+++@/	CS:Z:G30101000101000101002300100230100230000230100200100"));
		writer.addAlignment(parser.parseLine("1989_1770_1230	65	chr1	10308	20	44M6H	=	313943	231384101	CCCACCCCAACCCCAACCCTAACCCCTAACCCTAACCCTAACCC	IIIIIIIGIIIII?AIIIIIIIIII9133FD5979IE5@:-AI<	ZC:i:11	MD:Z:2A41	RG:Z:20100803052556101	NH:i:2	CM:i:2	NM:i:1	SM:i:18	ZP:Z:Z**	CQ:Z:ABBB:A@9/BB;?5+7:@<B;@;5?1))+)>'/+--=)-4'';6')7'4'	CS:Z:T20011000101000101002301000230100230100230100330102"));
		writer.addAlignment(parser.parseLine("841_1342_589	65	chr2	10177	18	50M	chr17	10708	0	ATCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACC	IAIII1;CIIIIIIIIIIIIIIIII7D@AIGDDCI**IDIIII@%%BIG3	ZC:i:11	MD:Z:1C48	RG:Z:20100803052556101	NH:i:2	CM:i:4	NM:i:1	SM:i:16	ZP:Z:Z**	CQ:Z:59)B?+'5/=599>6:>;B>85?A;/)<%=>*;*:8*@+:1><9(%):53	CS:Z:T33223010023010023010023010023010023030023010033010"));
		writer.addAlignment(parser.parseLine("710_1533_1074	115	chr2	10201	10	50M	=	11271	1120	CCCTAACCCTAACCCTAACCCTAACCCTAACCCCTAACCCTAACCCTAAA	=IIIIIIIIIIIIIIIIHIIIIICIIIIIIIIIIIIIIIIIIIIIIIIII	ZC:i:10	MD:Z:50	RG:Z:20100803052545338	NH:i:2	CM:i:0	NM:i:0	SM:i:16	ZP:Z:AAA	CQ:Z:>BAAB@BAAB>BB>B>?@A<4;;<B<22<>A<72@2??7?38B=6995A=	CS:Z:T00032001032001032000103200103200103200103200103200"));
		writer.addAlignment(parser.parseLine("1551_1209_1505	161	chr2	10308	30	45M5H	chrX	608650	0	CCAACCCCAACCCCAACCCTAAACCCTAACCCTAACCCTAACCCT	IIIIIIIIIIIIIIIIIIIHIIIIIII==III@;++I=5--8IA1	ZC:i:11	MD:Z:22C22	RG:Z:20100803052556101	NH:i:2	CM:i:3	NM:i:1	SM:i:20	ZP:Z:Z**	CQ:Z:BBBBBBBBBB9BBBB;@BB:/B:<A5<///@<2/-+B//'''2;'+++@/	CS:Z:G30101000101000101002300100230100230000230100200100"));
		writer.addAlignment(parser.parseLine("1989_1770_1230	65	chr2	10308	20	44M6H	=	313943	231384101	CCCACCCCAACCCCAACCCTAACCCCTAACCCTAACCCTAACCC	IIIIIIIGIIIII?AIIIIIIIIII9133FD5979IE5@:-AI<	ZC:i:11	MD:Z:2A41	RG:Z:20100803052556101	NH:i:2	CM:i:2	NM:i:1	SM:i:18	ZP:Z:Z**	CQ:Z:ABBB:A@9/BB;?5+7:@<B;@;5?1))+)>'/+--=)-4'';6')7'4'	CS:Z:T20011000101000101002301000230100230100230100330102"));
		writer.close();
		return file;
	}
	
	public final File createCoverageGL208(final String fileName, String dataFileName) throws IOException {
		Path p = Paths.get("test/resources/"+dataFileName).toAbsolutePath();
		List<String> sams = Files.readAllLines(p);
		final File file = testFolder.newFile(fileName);
		
		SAMReadGroupRecord rg1 = new SAMReadGroupRecord("8c954e28-d035-4a28-806e-b8837caab052");
		rg1.setSample("ES");
		SAMReadGroupRecord rg2 = new SAMReadGroupRecord("a7408e63-f751-46d8-b0ec-849c8b653631");
		rg2.setSample("ES");
		SAMReadGroupRecord rg3 = new SAMReadGroupRecord("ff341031-8dc5-4e50-9ff2-e9e1ee8f614f");
		rg3.setSample("ES");
		SAMReadGroupRecord rg4 = new SAMReadGroupRecord("3ae5af45-d754-4534-80f3-e08252df172b");
		rg4.setSample("ES");
		SAMReadGroupRecord rg5 = new SAMReadGroupRecord("30a4cb83-388a-48a6-84e4-9e82f3ec8df5");
		rg5.setSample("ES");
		SAMReadGroupRecord rg6 = new SAMReadGroupRecord("cd63589e-70c3-4c57-b11c-4bb05ca22592");
		rg6.setSample("ES");
		SAMReadGroupRecord rg7 = new SAMReadGroupRecord("00587a9e-66fc-4fd2-a972-eee4cbaf9613");
		rg7.setSample("ES");
		SAMReadGroupRecord rg8 = new SAMReadGroupRecord("cbe556ee-ee1b-48ad-a337-2377181d2093");
		rg8.setSample("ES");
		SAMReadGroupRecord rg9 = new SAMReadGroupRecord("042ae93b-833b-44a6-9f82-c948ceecf7c1");
		rg9.setSample("ES");
		header.setReadGroups(Arrays.asList(rg1, rg2, rg3, rg4, rg5, rg6, rg7, rg8, rg9));
		SAMLineParser parser = new SAMLineParser(header);
		SAMFileWriterFactory.setDefaultCreateIndexWhileWriting(true);
		SAMFileWriter writer = new SAMFileWriterFactory().makeBAMWriter(header, true, file);
		for (String s : sams) {
			writer.addAlignment(parser.parseLine(s));
		}
		writer.close();
		return file;
	}
	
	public final File createTumourCoverageBam(final String fileName) throws IOException {
		final File file = testFolder.newFile(fileName);
		SAMReadGroupRecord rg1 = new SAMReadGroupRecord("2010080607305580");
		rg1.setSample("ES");
		SAMReadGroupRecord rg2 = new SAMReadGroupRecord("20100806073055880");
		rg2.setSample("ES");
		SAMReadGroupRecord rg3 = new SAMReadGroupRecord("20100806061644214");
		rg3.setSample("ES");
		header.setReadGroups(Arrays.asList(rg1, rg2, rg3));
		SAMLineParser parser = new SAMLineParser(header);
		SAMFileWriterFactory.setDefaultCreateIndexWhileWriting(true);
		SAMFileWriter writer = new SAMFileWriterFactory().makeBAMWriter(header, true, file);
		writer.addAlignment(parser.parseLine("1652_611_1413	65	chr1	10170	12	48M2H	chr18	98761	0	AACCCTAACCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTA	IIIIIIIIIIIII''H<DB<CIEA;?IB;:7;;;5BD51//9''77A9	ZC:i:7	MD:Z:48	RG:Z:2010080607305580	NH:i:2	CM:i:2	NM:i:0	SM:i:18	ZP:Z:Z**	CQ:Z:@B>9B??59=-B=7'@)412+915-/1;(4'1+1++8-))')1'1'11))	CS:Z:T30100230102301102301002301002301002301002300002310"));
		writer.addAlignment(parser.parseLine("2310_699_589	161	chr1	10176	20	50M	chr7	15912	0	AACCTAACCCTAACCCTAAACCTAACCCTAACCCTAACCCTAACCCTAAC	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIH+	ZC:i:7	MD:Z:19C30	RG:Z:20100806073055880	NH:i:3	CM:i:2	NM:i:1	SM:i:18	ZP:Z:Z**	CQ:Z:BBBBBBBBBBBBABBBABB@@<>?B?BB>@A<B<>=?;B=<7@<BB7A>+	CS:Z:G20102301002301002300102301002301002301002301002301"));
		writer.addAlignment(parser.parseLine("1844_1231_1722	137	chr1	10304	20	2H48M	*	0	0	AACCCCAACCCCAACCCCAACCCTAACCCCTAACCCTAACCCTAACCC	IIIIICD++IIIEIIII))IIIIIIIIIII@I%%IIIG''?9==5?I<	ZC:i:8	MD:Z:48	RG:Z:20100806061644214	NH:i:0	CM:i:4	NM:i:0	SM:i:16	ZP:Z:Z**	CQ:Z::B+A2BB5/6+A8?9-?:</)2;7<9796=8=-4;%?<<17'5+///'9<	CS:Z:G30201000100000101000301002301000230000230000230100"));
		writer.addAlignment(parser.parseLine("1679_1928_748	145	chr1	10304	20	3H47M	chrX	15526	0	AACCCCAACCCCAACCCCAAACCTAACCCCTAACCCTAACCCTAACC	04IIIIB;AGA:;54>%%E<-9=2AIIII%%IHGF:=ADIIBGIIDG	ZC:i:7	MD:Z:20C26	RG:Z:20100806073055880	NH:i:4	CM:i:4	NM:i:1	SM:i:25	ZP:Z:Z**	CQ:Z:441;621<502,/8097%56;8:(+3''60%/0%1+026,03;8?0%,;5	CS:Z:G00103200103200103000010320100110010100010100010000"));
		writer.addAlignment(parser.parseLine("1760_1943_1783	115	chr1	770557	69	8H42M	=	771284	782	AAAAAAAAAATGTGGAGAGAAAGTAAGCGGCAAATGTGGCAA	IIIIIIIIIIIIIIEHIIIIIIIIIIIIIIIIIIIIIIIIII	ZC:i:7	MD:Z:42	RG:Z:20100806073055880	NH:i:1	CM:i:0	NM:i:0	SM:i:90	ZP:Z:AAA	CQ:Z:AA@9AAB@=BBB4;B@>=9BBBAB@B?*<BB>9B?B;AAA??@B>6)4)-	CS:Z:T00130111300130332031200222220111300000000000002222"));
		writer.addAlignment(parser.parseLine("2111_1305_1488	115	chr1	770557	83	3H47M	=	771427	923	AAAAAAAAAATGTGGAGAGAAAGTAAGCGGCAAATGTGGCAAAAACA	BEIIIIIIIIIIIIIIIIIIIIIIHIIIIIIIIIIIIIIIIIIIIII	ZC:i:8	MD:Z:47	RG:Z:20100806061644214	NH:i:1	CM:i:0	NM:i:0	SM:i:100	ZP:Z:AAA	CQ:Z:@@;:?A>:;@<?<8@@?6>A<@/:??A8?@@A64AB?5@@=@=>AA%>@@	CS:Z:T01100001301113001303320312002222201113000000000000"));
		writer.addAlignment(parser.parseLine("293_1672_1413	115	chr1	770557	77	5H45M	=	771674	1172	AAAAAAAAAATGTGGAGAGAAAGTAAGCGGCAAATGTGGCAAAAA	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII	ZC:i:8	MD:Z:45	RG:Z:20100806061644214	NH:i:1	CM:i:0	NM:i:0	SM:i:100	ZP:Z:AAA	CQ:Z:BB?AA?=;;=:?:?>7?@><<@@@A>B@=B:>::?5A@?B?BA;/A@=,8	CS:Z:T00000130111300130332031200222220111300000000000000"));
		writer.addAlignment(parser.parseLine("2225_562_1361	67	chr1	770558	88	50M	=	768815	-1793	AAAAAAAAATGTGGAGAGAAAGTAAGCGGCAAATGTGGCAAAAACACTAC	IIIII@EIIIFHIII%%III?EIIICDIII2BIII>8III7CIIIH5II:	ZC:i:8	MD:Z:50	RG:Z:20100806061644214	NH:i:1	CM:i:1	NM:i:0	SM:i:86	ZP:Z:AAA	CQ:Z:84=<4:'?<==*?7=<%?>=:&@?;<(=>@,'<>@:%4=?-+9:>=,*@:	CS:Z:T30000000031110220220021302330310031110310000111231"));
		writer.addAlignment(parser.parseLine("1652_611_1413	65	chr2	10170	12	48M2H	chr18	98761	0	AACCCTAACCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTA	IIIIIIIIIIIII''H<DB<CIEA;?IB;:7;;;5BD51//9''77A9	ZC:i:7	MD:Z:48	RG:Z:2010080607305580	NH:i:2	CM:i:2	NM:i:0	SM:i:18	ZP:Z:Z**	CQ:Z:@B>9B??59=-B=7'@)412+915-/1;(4'1+1++8-))')1'1'11))	CS:Z:T30100230102301102301002301002301002301002300002310"));
		writer.addAlignment(parser.parseLine("2310_699_589	161	chr2	10176	20	50M	chr7	59127	0	AACCTAACCCTAACCCTAAACCTAACCCTAACCCTAACCCTAACCCTAAC	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIH+	ZC:i:7	MD:Z:19C30	RG:Z:20100806073055880	NH:i:3	CM:i:2	NM:i:1	SM:i:18	ZP:Z:Z**	CQ:Z:BBBBBBBBBBBBABBBABB@@<>?B?BB>@A<B<>=?;B=<7@<BB7A>+	CS:Z:G20102301002301002300102301002301002301002301002301"));
		writer.addAlignment(parser.parseLine("1844_1231_1722	137	chr2	10304	20	2H48M	*	0	0	AACCCCAACCCCAACCCCAACCCTAACCCCTAACCCTAACCCTAACCC	IIIIICD++IIIEIIII))IIIIIIIIIII@I%%IIIG''?9==5?I<	ZC:i:8	MD:Z:48	RG:Z:20100806061644214	NH:i:0	CM:i:4	NM:i:0	SM:i:16	ZP:Z:Z**	CQ:Z::B+A2BB5/6+A8?9-?:</)2;7<9796=8=-4;%?<<17'5+///'9<	CS:Z:G30201000100000101000301002301000230000230000230100"));
		writer.addAlignment(parser.parseLine("1679_1928_748	145	chr2	10304	20	3H47M	chrX	15521	0	AACCCCAACCCCAACCCCAAACCTAACCCCTAACCCTAACCCTAACC	04IIIIB;AGA:;54>%%E<-9=2AIIII%%IHGF:=ADIIBGIIDG	ZC:i:7	MD:Z:20C26	RG:Z:20100806073055880	NH:i:4	CM:i:4	NM:i:1	SM:i:25	ZP:Z:Z**	CQ:Z:441;621<502,/8097%56;8:(+3''60%/0%1+026,03;8?0%,;5	CS:Z:G00103200103200103000010320100110010100010100010000"));
		writer.addAlignment(parser.parseLine("1760_1943_1783	115	chr2	770557	69	8H42M	=	771284	782	AAAAAAAAAATGTGGAGAGAAAGTAAGCGGCAAATGTGGCAA	IIIIIIIIIIIIIIEHIIIIIIIIIIIIIIIIIIIIIIIIII	ZC:i:7	MD:Z:42	RG:Z:20100806073055880	NH:i:1	CM:i:0	NM:i:0	SM:i:90	ZP:Z:AAA	CQ:Z:AA@9AAB@=BBB4;B@>=9BBBAB@B?*<BB>9B?B;AAA??@B>6)4)-	CS:Z:T00130111300130332031200222220111300000000000002222"));
		writer.addAlignment(parser.parseLine("2111_1305_1488	115	chr2	770557	83	3H47M	=	771427	923	AAAAAAAAAATGTGGAGAGAAAGTAAGCGGCAAATGTGGCAAAAACA	BEIIIIIIIIIIIIIIIIIIIIIIHIIIIIIIIIIIIIIIIIIIIII	ZC:i:8	MD:Z:47	RG:Z:20100806061644214	NH:i:1	CM:i:0	NM:i:0	SM:i:100	ZP:Z:AAA	CQ:Z:@@;:?A>:;@<?<8@@?6>A<@/:??A8?@@A64AB?5@@=@=>AA%>@@	CS:Z:T01100001301113001303320312002222201113000000000000"));
		writer.addAlignment(parser.parseLine("293_1672_1413	115	chr2	770557	77	5H45M	=	771674	1172	AAAAAAAAAATGTGGAGAGAAAGTAAGCGGCAAATGTGGCAAAAA	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII	ZC:i:8	MD:Z:45	RG:Z:20100806061644214	NH:i:1	CM:i:0	NM:i:0	SM:i:100	ZP:Z:AAA	CQ:Z:BB?AA?=;;=:?:?>7?@><<@@@A>B@=B:>::?5A@?B?BA;/A@=,8	CS:Z:T00000130111300130332031200222220111300000000000000"));
		writer.addAlignment(parser.parseLine("2225_562_1361	67	chr2	770558	88	50M	=	768815	-1793	AAAAAAAAATGTGGAGAGAAAGTAAGCGGCAAATGTGGCAAAAACACTAC	IIIII@EIIIFHIII%%III?EIIICDIII2BIII>8III7CIIIH5II:	ZC:i:8	MD:Z:50	RG:Z:20100806061644214	NH:i:1	CM:i:1	NM:i:0	SM:i:86	ZP:Z:AAA	CQ:Z:84=<4:'?<==*?7=<%?>=:&@?;<(=>@,'<>@:%4=?-+9:>=,*@:	CS:Z:T30000000031110220220021302330310031110310000111231"));
		writer.addAlignment(parser.parseLine("1760_1943_1783	115	chr4	770557	69	8H42M	=	771284	782	AAAAAAAAAATGTGGAGAGAAAGTAAGCGGCAAATGTGGCAA	IIIIIIIIIIIIIIEHIIIIIIIIIIIIIIIIIIIIIIIIII	ZC:i:7	MD:Z:42	RG:Z:20100806073055880	NH:i:1	CM:i:0	NM:i:0	SM:i:90	ZP:Z:AAA	CQ:Z:AA@9AAB@=BBB4;B@>=9BBBAB@B?*<BB>9B?B;AAA??@B>6)4)-	CS:Z:T00130111300130332031200222220111300000000000002222"));
		writer.addAlignment(parser.parseLine("2111_1305_1488	115	chr4	770557	83	3H47M	=	771427	923	AAAAAAAAAATGTGGAGAGAAAGTAAGCGGCAAATGTGGCAAAAACA	BEIIIIIIIIIIIIIIIIIIIIIIHIIIIIIIIIIIIIIIIIIIIII	ZC:i:8	MD:Z:47	RG:Z:20100806061644214	NH:i:1	CM:i:0	NM:i:0	SM:i:100	ZP:Z:AAA	CQ:Z:@@;:?A>:;@<?<8@@?6>A<@/:??A8?@@A64AB?5@@=@=>AA%>@@	CS:Z:T01100001301113001303320312002222201113000000000000"));
		writer.addAlignment(parser.parseLine("293_1672_1413	115	chr4	770557	77	5H45M	=	771674	1172	AAAAAAAAAATGTGGAGAGAAAGTAAGCGGCAAATGTGGCAAAAA	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII	ZC:i:8	MD:Z:45	RG:Z:20100806061644214	NH:i:1	CM:i:0	NM:i:0	SM:i:100	ZP:Z:AAA	CQ:Z:BB?AA?=;;=:?:?>7?@><<@@@A>B@=B:>::?5A@?B?BA;/A@=,8	CS:Z:T00000130111300130332031200222220111300000000000000"));
		writer.addAlignment(parser.parseLine("2225_562_1361	67	chr4	770558	88	50M	=	768815	-1793	AAAAAAAAATGTGGAGAGAAAGTAAGCGGCAAATGTGGCAAAAACACTAC	IIIII@EIIIFHIII%%III?EIIICDIII2BIII>8III7CIIIH5II:	ZC:i:8	MD:Z:50	RG:Z:20100806061644214	NH:i:1	CM:i:1	NM:i:0	SM:i:86	ZP:Z:AAA	CQ:Z:84=<4:'?<==*?7=<%?>=:&@?;<(=>@,'<>@:%4=?-+9:>=,*@:	CS:Z:T30000000031110220220021302330310031110310000111231"));
		writer.close();
		return file;
	}
	
	public final File createRefFile(final String filename) throws IOException {
		final File file = testFolder.newFile(filename);
		try (final OutputStream os = new FileOutputStream(file);
			final PrintStream ps = new PrintStream(os);) {
			
			for (SAMSequenceRecord ssr : header.getSequenceDictionary().getSequences()) {
				if ( ! "GL000208.1".equals(ssr.getSequenceName())) {
					ps.println(">"+ssr.getSequenceName());
					char[] array = new char[ ssr.getSequenceLength()];
					Arrays.fill(array, 'A');
					ps.println(array);
				}
			}
			
			ps.println(">GL000208.1");
			ps.println("GAATTCTTCAAAGAGTTCCAGATATCCACAGGCAGATTCTACAAAAGAAGTGTTTCAATACTGCTCTATC" +
"AAAAGATGTATTCCACTCAGTTACTTTCATGCACACATCTCAATGAAGTTCCTGAGAAAGCTTCTGTCTA" +
"GTTTTTATGTGAAAATATTTCCTTTTCCATCATGGGCCTCAAAGCGCTCAAAATGAACCCTTGCAGATAC" +
"TAGAGAAAGACTGTTTCAAAACTGCTCTATCCAAAGAACGGTTCCACTCTGTGAGGTGAATGCACACATC" +
"ACAAAGCAGTTTCTGAGAACGCTTCTGTCTAGTTTGTAGGTGAAGATATTTCCTTTTCCTTCATAGGCCT" +
"CTAATCGCTCCAAATATCCACAAGCAGATTCTTCAAAATGTGTGTTTCAACACTGCTCTATCAAAAGAAA" +
"GGTTCAAGTCTGTGAGTTGAATGCACACATCACAAAGCAGTTTCTGAGAATGCCTCTGTCTAGTTTGTAT" +
"GTGAAGATATTTCTTTTTCCGTCTTATGCCTCAAATCGCTCCAAATATCCACTTGCAGATACTTCAAAAA" +
"GACTCTTTCAAAACTGCTCTATCCAAAGAACGGTTCAACTCTGTGAGATGAATGCGCTCATCACAAAGCA" +
"GTGTTTGAGAATGCTTCTGTCTAGTTTTCATGTGAAGATATCCTGTTTACAGAGATTAACTCAAAGAGCT" +
"CCAAGTATCCACTAGTAGAAACTTTAACAGCAGTTTTTCAAAACAGCTCTATCAAAAGAAAGGTTCAACT" +
"CTGAATTGAACACACGGATCACAAAGGTGTTTCTGAAAAAGCTTATGTCTGTTTTTTATATGAAAATATT" +
"TTCTTTTCCACCTTACACCCCAAAGCGCTCCAAATGAACGCTTGCAGATTCTACAAAAAGACTGTTTCCA" +
"AACTGCTCTATGAAAAGAAGGGTTACACTCAGTGAGATAAATGCACACATCACAAAGCAGTTTCTGAGAA" +
"TGCTTCTGTCTATATATTATGTGAAGGTAACTGTTTCCAACATATTCCTCATAGAGTTCCAAATATACAC" +
"AAGCAGATTCTGAAAAAAGATTATTTTAATACTGCTCTTTGAAAAGGTAGATTCATCTCTTTTAGTTGAA" +
"TGCACACATCTCAAAGTAGTTCCTGAGAATGCTTCTGTCTAGTTTTTATGTGAATATATTACCTTTTCCA" +
"CCATAGGCTTCAAAGCACTCCAAATGAACAGTTGCAGATTCTACAAAAGGACTGTTTCAAAACTGCTATA" +
"TGAAAAGAAGTGTTCCACTCTGTGAGGTGAATGCACACATCACAAAGCAGTTTCTGAGAATGATTCTGAC" +
"TAGTGTTTATGTGAAGATATTTCTTTTCCCATCATAAGCCTCAAATCACTCAAAATATACACTTGCAGAT" +
"AGTACAAAAAGTCTGTTTCAAATCTTCTCTCTCAAAAGGAAGGTTCAACTTTGTGAGTTGAGTGCACACA" +
"TCACAAAGCAGTTTCTGAGAATGCTTCTGTCTAGCGTGTATGTGAAGTTACCACGTTTACAAGGAATTCC" +
"TCAAAGAGCTCCAAATATCCACTAGGAGAACCTTAAAAAGCAGTGTTTCAAAAGTGCTCTATCAAAAGAA" +
"AGGTTCAACTCTGTGAATTGAAAACGCATATCACAATGGTGTTTCTGAGAATGCTTCTGTCTAGTTTTCA" +
"TGTGAAGATATTTCCTTTTCCACCATATGCCCCAAAGCGCTAAAAATGAACAATTGCAGATTCTACAAAA" +
"AGTGTGCTTCAGCACTGCTCTATCAAAAGAAAGGCTCAACTCTGTGAGTTGAATGCACACAACACAAAGC" +
"AGTTTCTGAGAATTCCTCTGTCTAGTTTTTATGTGTAGATATTTCCTTTTCCTCATATGACTCAAATCGC" +
"TCCAAATATCCTCTTGCTGATACTACAAAAAGACTGTTTCAAAACTGCTCTCTCAAAAGGAAGGTTCAGC" +
"TCTATGAGTTGAAGGCACACATCAAGAAGCAGTTTCTGAGAATGTTTCTGTCTAGTTTGAATGTGAAGAT" +
"ATCCCATTTACAACGAATTCCTCAAATAACTCAAAATATCCACAAGCAGATTCTACAAAAACAGTGTTTC" +
"AATACTGCTCTACGAAAAGAAAGGTTCAACTCTGTGAATTGAAAACACACATCACAAAGTTGTTTCTGAG" +
"AATGCTTCTGTCTAATTTTTAGGTGAAGATATTTCCTTTTCCACCATGGGCCCCAAAGCGCTAAAAATGA" +
"ACAATTGCAGATTCTACAAAAAGTGTGTTTCAGCACTGCTCTATCAAAAGAAAGGCTCAACCCTGTGAGT" +
"TGAATGCACACATACAAAGCAGTTTCTGAAAATTCTTCTGTCTAGTTTGTATGTGTAGATATTTCCTTTT" +
"CCTCATATGACTTGAATCGCTCCAAATATCCACTTGCACATGCTACGAAAAGACTGTTTCAAAACTGCTC" +
"TCTCAAAAGAAGATTCAGCTCTATGAGTTGCAGGCACACATCAAGAAGTAGTTTCTGAGAATGCTTCTGT" +
"CTAGTTTGAATGTGAAGATACCCCGTTTACAACGAATTCCTCAAAGAACTCCAAATATCCATAAGCAGAT" +
"TCTACAAAAACAGTGTTTCAATACTGCTCTATCAAAAGAAAGGTTCAACTCTGTGAATTGAACACACACA" +
"TCACAAAGGAGTTTCTGAGAATGCTTGTGTCTTGTTTTTATGTGAAGATTTTTTTTTTCCACTATAGGCA" +
"ACAATGCGCTCCAAATGAACACTTGCAGATTCTACAAAAAGTGTGTTTCCACACTGCTCTTTCAAAAGAA" +
"AGGTTCAAGTCTGTGAGTTGAATGCACACATCACAAACAACTTTCTGAGATGGCTTGGGTCTAGTTTTTA" +
"AGTGAAGATATCCGTTTCCAATGAATTCCTCAAGGAGTTGAAAATATCCGCCAGCAGATTCTACAAAAGC" +
"AGTATTTCAATACTGCTCTATCAAAAGAGGGATTCAACTCTTTTAGTTGAATGCACACATCTCAAAGAAG" +
"TTCCTGAGAATGCTTCTATCTTGTTTTTATGTGAAGATATTTTCTTTTCTACCGTAGGCTTCAAAGCGCT" +
"CCAAAAGAACACTTGCAGATTCTTCAAAAAGACTGTTTCAAAACTGCTCTATTAAAAGAAGGGTTCCACT" +
"CTATGAGGTGAATGCACACACCACGAAGCAGATTCTGAGAATGCTTCTTTCTATTTTTTATGTGAAGATA" +
"TTCCCTTTTCCATCACAAGCTTCAAATCTCTCCAAATATCCACTTGCAGATACTACAAAAAGACTGTTTC" +
"ATAACTGCTCTCTCAAAAGGAAGGTTCAACTTTTTGAGTTCAGTGCGCACATCACAAAGCAGTTTCTGAG" +
"AATGCTTCTGTCTAGTTTGTATGTGTAGGTATTTCCTCTTCCATCACATGCCTCAAATCGTTCCAAATAT" +
"CCACTGACAGATACTATAAAAAGACTGTTTCAAAACTGCTCTATCAAAAGGAGTGTTCAACTCTGTGAGT" +
"TGAATGCACACATCACAAATTAGTTTCTGAGAATCCTTCTGTCTACTTTTTATGTGAAGATATTCCCATT" +
"TCCAACCGAGGCTTCAAAGCACTCCAAATATCTACTTGCATATTGTACAAAAAGAGTGCTTCAAAACTGT" +
"TCTATCAAAAGGAAGATTCAACTCTGTAAGTTGAATGCACACATCACAAGGAAGTATCTGAGAATAGTTC" +
"TATCTAGTTCTTATGTGGAGCTATTCCGGTTTCCAAGGAAGGCTTCTGACCATCAGAAATATTCACTTGC" +
"ATATTCTACAAAAAGAGTGTTTCAAAACTGCTCTATTAAAAGGAAGGTTCAACTCTGTGAGTTGAATGCA" +
"CACATCACAAACACTCTTCTGAGAATCCATCTGTCTCTTTTTTATGTGAAGATATACCCGTTTCCAATGA" +
"AGGCTTCAAAGTACTACAGTTATCCACTAGCTGATTCTACACAAAATCTGTTTCAAAACTGCTCCATCAA" +
"AAGGAAGCTTCAACTCTGTGAGTTGAATGCACACATCAAAAGGAAGTTACTGAGAATGCTTCTATCTAGT" +
"TTTTATGTGAATATATTCCCGTTTCCAATGAATTCTTCAAAGCGATCCAAATATACACTTGCAGATACTA" +
"CAAAAAGAGTGTTTTAAAACTGCTCTATCAAAAGGAAGGTTCATCTCTGTGAGTTGAATGCACACATCAC" +
"AAGGAAGTTTCTGACAATGCTTTTGTCTAGTTTTCATGTGAATATATTCTCGTTTCCAACGAAGGCTTCA" +
"AAGCCCTCCAAATATCCACTTGCATATTCTACAAAAAGAGTGTTTCAAAACTGCTCTATCAAAAGGAAGG");
		}
		return file;
	}

}
