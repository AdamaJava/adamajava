package org.qcmg.sig;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMProgramRecord;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.qcmg.picard.SAMOrBAMWriterFactory;
import org.qcmg.picard.util.SAMUtils;

public class SignatureGeneratorTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
    static void writeSnpChipFile(File snpChipFile) throws IOException {
	    	try (Writer writer = new FileWriter(snpChipFile);) {
		    	writer.write("[Header]\n");
		    	writer.write("GSGT Version    1.9.4\n");
		    	writer.write("Processing Date 3/1/2013 9:06 PM\n");
		    	writer.write("Content         HumanOmni2.5-8v1_A.bpm\n");
		    	writer.write("Num SNPs        2379855\n");
		    	writer.write("Total SNPs      2379855\n");
		    	writer.write("Num Samples     57\n");
		    	writer.write("Total Samples   57\n");
		    	writer.write("File    31 of 57\n");
		    	writer.write("[Data]\n");
		    	writer.write("SNP Name        Sample ID       Allele1 - Top   Allele2 - Top   GC Score        Sample Name     Sample Group    Sample Index    SNP Index       SNP Aux Allele1 - Forward       Allele2 - Forward       Allele1 - Design        Allele2 - Design        Allele1 - AB    Allele2 - AB    Chr     Position        GT Score        Cluster Sep     SNP     ILMN Strand     Customer Strand Top Genomic Sequence    Theta   R       X       Y       X Raw   Y Raw   B Allele Freq   Log R Ratio\n");
		    	writer.write("rs1000000	WG0227767_DNAG01_LP6005272_DNA_G01	A	G	0.8140	AOCS_094_5_6	G01	20	381565	0	T	C	T	C	A	B	12	126890980	0.7990	1.0000	[T/C]	BOT	BOT		0.444	2.019	1.099	0.920	13935	7437	0.5219	0.0314\n");
		    	writer.write("rs10000023	WG0227769_DNAG01_LP6005274_DNA_G01	C	C	0.8221	AOCS_094_1_1	G01	31	691448	0	G	G	G	G	B	B	4	95733906	0.8042	1.0000	[T/G]	BOT	BOT		0.970	1.928	0.086	1.843	1593	18194	1.0000	0.1902\n");
		    	writer.write("rs1000002	WG0227768_DNAG01_LP6005273_DNA_G01	A	A	0.8183	AOCS_094_6_7	UQueensland_Grimmond2	157	719472	0	A	A	A	A	A	A	3	183635768	0.8018	0.4106	[A/G]	TOP	TOP		0.039	0.745	0.702	0.043	5494	277	0.0000	0.4090\n");
	    	}
    }
    
    static void writeIlluminaArraysDesignFile(File illuminaArraysDesign) throws IOException {
	    	try (Writer writer = new FileWriter(illuminaArraysDesign);) {
	    		writer.write("#dbSNP Id	Reference Genome	dbSNP alleles	Chr	Position(hg19)	dbSNP Strand	IlluminaDesign	ComplementArrayCalls?\n");
			writer.write("rs1000000	G	C/T	chr12	126890980	-	[T/C]	yes\n");
			writer.write("rs10000004	A	A/G	chr4	75406448	+	[T/C]	yes\n");
			writer.write("rs10000006	T	C/T	chr4	108826383	+	[A/G]	yes\n");
			writer.write("rs1000002	C	A/G	chr3	183635768	-	[A/G]	yes\n");
			writer.write("rs10000021	G	G/T	chr4	159441457	+	[A/C]	yes\n");
			writer.write("rs10000023	G	G/T	chr4	95733906	+	[T/G]	no\n");
	    	}
    }
    
    static void writeSnpPositionsFile(File snpPositions) throws IOException {
	    	try (Writer writer = new FileWriter(snpPositions);) {
	    		writer.write("chr3	183635768	random_1016708	C	RANDOM_POSITION\n");
	    		writer.write("chr4	75406448	random_649440	A	RANDOM_POSITION\n");
	    		writer.write("chr4	95733906	random_1053689	G	RANDOM_POSITION\n");
	    		writer.write("chr4	108826383	random_1146989	T	RANDOM_POSITION\n");
	    		writer.write("chr4	159441457	random_1053689	G	RANDOM_POSITION\n");
	    		writer.write("chr12	126890980	random_169627	G	RANDOM_POSITION\n");
	    	}
    }
	
    static void getBamFile(File bamFile, boolean validHeader, boolean useChrs) {
    	getBamFile(bamFile,  validHeader,  useChrs, false);
    }
    
    static void getBamFile(File bamFile, boolean validHeader, boolean useChrs, boolean addReadGroupToHeaderAndRecords) {
	    	final SAMFileHeader header = getHeader(validHeader, useChrs, addReadGroupToHeaderAndRecords);
	    	List<SAMRecord> data = getRecords(useChrs, header, true, addReadGroupToHeaderAndRecords);
	//    	if ( ! validHeader) header.setSequenceDictionary(new SAMSequenceDictionary());
	    	final SAMOrBAMWriterFactory factory = new SAMOrBAMWriterFactory(header, false, bamFile, false);
	    	try {
	    		final SAMFileWriter writer = factory.getWriter();
	    		if (null != data)
	    			for (final SAMRecord s : data) writer.addAlignment(s);
	    	} finally {
	    		factory.closeWriter();
	    	}
    }
    public static SAMFileHeader getHeader(boolean valid, boolean useChrs) {
    	return getHeader(valid, useChrs, false);
    }    
	public static SAMFileHeader getHeader(boolean valid, boolean useChrs, boolean addReadGroups) {
		final SAMFileHeader header = new SAMFileHeader();
		
		final SAMProgramRecord bwaPG = new SAMProgramRecord("bwa");
		bwaPG.setProgramName("bwa");
		bwaPG.setProgramVersion("0.6.1-r104");
		header.addProgramRecord(bwaPG);
		
		if ( ! valid) {
			final SAMProgramRecord invalidPG = new SAMProgramRecord("blah");
			invalidPG.setAttribute("CL", "");
			header.addProgramRecord(invalidPG);
			
			final SAMReadGroupRecord rgRec = new SAMReadGroupRecord("ID");
			rgRec.setAttribute("PG", "tmap");
			header.addReadGroup(rgRec);
		}
		if (addReadGroups) {
			header.addReadGroup(new SAMReadGroupRecord("20130325103517169"));
			header.addReadGroup(new SAMReadGroupRecord("20130325112045146"));
			header.addReadGroup(new SAMReadGroupRecord("20130325084856212"));
		}
		
		// looks like we need this to be specifically defined
		final SAMSequenceDictionary seqDict = new SAMSequenceDictionary();
		seqDict.addSequence(new SAMSequenceRecord(useChrs ? "chr1" : "1", 249250621));
		seqDict.addSequence(new SAMSequenceRecord(useChrs ? "chr2" : "2", 243199373));
		seqDict.addSequence(new SAMSequenceRecord(useChrs ? "chr3" : "3", 198022430));
		seqDict.addSequence(new SAMSequenceRecord(useChrs ? "chr4" : "4", 191154276));
		seqDict.addSequence(new SAMSequenceRecord(useChrs ? "chr5" : "5", 180915260));
		seqDict.addSequence(new SAMSequenceRecord(useChrs ? "chr12" : "12", 80915260));
		header.setSequenceDictionary(seqDict);
		
		return header;
	}
 
	
	private static List<SAMRecord> getRecords(boolean useChr, SAMFileHeader header, boolean isValid, boolean addRG) {
		List<SAMRecord> records = new ArrayList<>();
//		records.add("HS2000-152_756:1:1316:11602:65138	89	chr1	9993	25	100M	=	9993	0	TCTTCCGATCTCCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTA	B@??BBCCB<>BCBB?:BAA?9-A;?2;@ECA=;7BEE?=7D9@@8.C;B8=.HGDBBBCCD::*GGD:?*FDGFCA>EIHEEBEAEFDFFC=+?DD@@@	X0:i:1	X1:i:0	ZC:i:9	MD:Z:0C0T0G6A0A89	PG:Z:MarkDuplicates	RG:Z:20130325103517169	XG:i:0	AM:i:0	NM:i:5	SM:i:25	XM:i:5	XN:i:8	XO:i:0	XT:A:U");
		SAMRecord sam = new SAMRecord(header);
		sam.setAlignmentStart(183635758);
		sam.setReferenceName(useChr ? "chr3" : "3");
		sam.setFlags(67);
		sam.setMappingQuality(60);
		sam.setReadName("HS2000-152_756:1:1316:11602:65138");
		sam.setReadString("TCTTCCGATCTCCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTA");
		sam.setBaseQualityString("B@??BBCCB<>BCBB?:BAA?9-A;?2;@ECA=;7BEE?=7D9@@8.C;B8=.HGDBBBCCD::*GGD:?*FDGFCA>EIHEEBEAEFDFFC=+?DD@@@");
		sam.setCigarString("100M");
		if (addRG) {
			sam.setAttribute("RG", "20130325103517169");
		}
		
		assertEquals(true, SAMUtils.isSAMRecordValidForVariantCalling(sam, true));
		for (int i = 0 ; i < 10; i++) records.add(sam);
		
//		records.add("HS2000-152_756:2:1212:5424:43221	99	chr1	10001	29	45M1I14M4D9M2D21M10S	=	10101	199	TAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCCTAACCCCCACCCCCTACCCCACACTCACCCACCCCCTAACCTCAGCACCCC	CCCFFFFFFHHHHGGIJJIIJJJJJJJJJJGEHIJJ9)?)?D))?(?BFB;CD@C#############################################	ZC:i:7	MD:Z:54T0A3^CTAA5A3^TA2C2A4T0A0A8	PG:Z:MarkDuplicates	RG:Z:20130325112045146	XG:i:7	AM:i:29	NM:i:15	SM:i:29	XM:i:8	XO:i:3	XT:A:M");
		sam = new SAMRecord(header);
		sam.setAlignmentStart(75406428);
		sam.setReferenceName(useChr ? "chr4" : "4");
		sam.setMappingQuality(60);
		sam.setReadName("HS2000-152_756:2:1212:5424:43221");
		sam.setReadString("TAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCCTAACCCCCACCCCCTACCCCACACTCACCCACCCCCTAACCTCAGCACCCC");
		sam.setBaseQualityString("CCCFFFFFFHHHHGGIJJIIJJJJJJJJJJGEHIJJ9)?)?D))?(?BFB;CD@C#############################################");
		sam.setCigarString("45M1I14M4D9M2D21M10S");
		sam.setAttribute("RG", "20130325112045146");
		if (addRG) {
			sam.setAttribute("RG", "20130325112045146");
		}
		for (int i = 0 ; i < 20; i++) records.add(sam);
		
//		records.add("HS2000-152_757:7:1311:15321:98529	163	chr1	10002	0	100M	=	10020	118	AACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACACTAACCCTAACCCTAACCCTAACCCTAACCCTAACC	@@@FFDFFB;DFHIHIIIIJGICGGGGGGF?9CF;@?DD;BDGG2DEFGC9EDHHI@CCEEFE)=?33;6;6;@AA;=A?2<?((59(9<<((28?<?B8	X0:i:362	ZC:i:8	MD:Z:63C36	PG:Z:MarkDuplicates	RG:Z:20130325084856212	XG:i:0	AM:i:0	NM:i:1	SM:i:0	XM:i:1	XO:i:0	XT:A:R");
		sam = new SAMRecord(header);
		sam.setAlignmentStart(95733896);
		sam.setReferenceName(useChr ? "chr4" : "4");
		sam.setReadName("HS2000-152_757:7:1311:15321:98529");
		sam.setMappingQuality(60);
		sam.setReadString("AACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACACTAACCCTAACCCTAACCCTAACCCTAACCCTAACC");
		sam.setBaseQualityString("@@@FFDFFB;DFHIHIIIIJGICGGGGGGF?9CF;@?DD;BDGG2DEFGC9EDHHI@CCEEFE)=?33;6;6;@AA;=A?2<?((59(9<<((28?<?B8");
		sam.setCigarString("100M");
		if (addRG) {
			sam.setAttribute("RG", "20130325084856212");
		}
		for (int i = 0 ; i < 10; i++) records.add(sam);
		
//		records.add("HS2000-152_756:2:2306:7001:4421	99	chr1	10003	29	2S53M1I44M	=	10330	426	TGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAA	CCCFFFFFHHHHHIJJJJEHGIIJIHGIIJIFGIJJJGGHHIIGCDGIHI>GIIEAFGJJI@EGFDFCE@DDDE@CA=A;3;?BDB?CD@DB9ADDBA9?	ZC:i:7	MD:Z:5A5A5A5A5A5A5A5A49	PG:Z:MarkDuplicates	RG:Z:20130325112045146	XG:i:1	AM:i:29	NM:i:9	SM:i:29	XM:i:8	XO:i:1	XT:A:M");
		sam = new SAMRecord(header);
		sam.setAlignmentStart(108826373);
		sam.setReferenceName(useChr ? "chr4" : "4");
		sam.setReadName("HS2000-152_756:2:2306:7001:4421");
		sam.setReadString("TGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAA");
		sam.setMappingQuality(60);
		sam.setBaseQualityString("CCCFFFFFHHHHHIJJJJEHGIIJIHGIIJIFGIJJJGGHHIIGCDGIHI>GIIEAFGJJI@EGFDFCE@DDDE@CA=A;3;?BDB?CD@DB9ADDBA9?");
		sam.setCigarString("2S53M1I44M");
		if (addRG) {
			sam.setAttribute("RG", "20130325112045146");
		}
		for (int i = 0 ; i < 20; i++) records.add(sam);
		
//		records.add("HS2000-152_756:1:1215:14830:88102	99	chr1	10004	29	24M4D76M	=	10441	537	CCCTACCCCTACCCCTACCCCTAAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTTACCCTAACCCTTACCCTAACC	CCCFFFFFHHHHHJJIHFHJIGIHGIIJJJJIJJIIIIJJJJJIIJJJJIJJJJJJJJHHGHHFFEFCEEDD9?BABDCDDDDDDDDDDDDCDDDDDDDB	ZC:i:9	MD:Z:5A5A5A6^CCCT54A11A9	PG:Z:MarkDuplicates	RG:Z:20130325103517169	XG:i:4	AM:i:29	NM:i:9	SM:i:29	XM:i:5	XO:i:1	XT:A:M");
		sam = new SAMRecord(header);
		sam.setAlignmentStart(159441437);
		sam.setReferenceName(useChr ? "chr4" : "4");
		sam.setReadName("HS2000-152_756:1:1215:14830:88102");
		sam.setReadString("CCCTACCCCTACCCCTACCCCTAAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTTACCCTAACCCTTACCCTAACC");
		sam.setBaseQualityString("CCCFFFFFHHHHHJJIHFHJIGIHGIIJJJJIJJIIIIJJJJJIIJJJJIJJJJJJJJHHGHHFFEFCEEDD9?BABDCDDDDDDDDDDDDCDDDDDDDB");
		sam.setMappingQuality(60);
		sam.setCigarString("24M4D76M");
		if (addRG) {
			sam.setAttribute("RG", "20130325103517169");
		}
		for (int i = 0 ; i < 10; i++) records.add(sam);
		
		sam = new SAMRecord(header);
		sam.setAlignmentStart(126890960);
		sam.setReferenceName(useChr ? "chr12" : "12");
		sam.setFlags(67);
		sam.setReadName("HS2000-152_756:1:1215:14830:88102");
		sam.setReadString("CCCTACCCCTACCCCTACCCCTAAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTTACCCTAACCCTTACCCTAACC");
		sam.setBaseQualityString("CCCFFFFFHHHHHJJIHFHJIGIHGIIJJJJIJJIIIIJJJJJIIJJJJIJJJJJJJJHHGHHFFEFCEEDD9?BABDCDDDDDDDDDDDDCDDDDDDDB");
		sam.setMappingQuality(60);
		sam.setCigarString("24M4D76M");
		for (int i = 0 ; i < 20; i++) records.add(sam);
		
		if ( ! isValid) {
			sam = new SAMRecord(header);
			sam.setAlignmentStart(10005);
			sam.setReferenceName(useChr ? "chr1" : "1");
			sam.setReadName("HS2000-152_756:1:1215:14830:88103");
			sam.setReadString("CCCTACCCCTACCCCTACCCCTAAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTTACCCTAACCCTTACCCTAACC");
			sam.setMappingQuality(60);
			sam.setBaseQualityString("CCCFFFFFHHHHHJJIHFHJIGIHGIIJJJJIJJIIIIJJJJJIIJJJJIJJJJJJJJHHGHHFFEFCEEDD9?BABDCDDDDDDDDDDDDCDDDDDDDB");
			sam.setCigarString("24M4D76M");
			sam.setReadUnmappedFlag(true);
			sam.setMappingQuality(1);
			for (int i = 0 ; i < 30; i++) records.add(sam);
		}
		
		return records;
	}
	
}
