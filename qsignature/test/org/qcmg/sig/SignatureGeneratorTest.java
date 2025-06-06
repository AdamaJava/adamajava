package org.qcmg.sig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.util.BaseUtils;
import org.qcmg.common.util.IlluminaUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.picard.SAMWriterFactory;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.qio.illumina.IlluminaRecord;
import org.qcmg.sig.util.SignatureUtil;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMProgramRecord;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;

import static org.junit.Assert.*;

public class SignatureGeneratorTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	
	
	@Test
	public void testPatientRegex() {
        assertTrue("APGI_1992".matches(SignatureUtil.PATIENT_REGEX));
        assertFalse("APG_1992".matches(SignatureUtil.PATIENT_REGEX));
        assertFalse("APGI_992".matches(SignatureUtil.PATIENT_REGEX));
        assertFalse("APGI_19922".matches(SignatureUtil.PATIENT_REGEX));
        assertTrue("PPPP_1234".matches(SignatureUtil.PATIENT_REGEX));
        assertTrue("PPPP_1234.exome.ND.bam".substring(0, 9).matches(SignatureUtil.PATIENT_REGEX));
	}
	
	@Test
	public void testUpdateResultsIllumina() {
		String illRecString = "rs9349115	5636391030_R02C01	G	G	0.7370			78	1081258	0	C	C	G	G	B	B	6	39330207	0.7923	1.0000	[C/G]	TOP	BOT		0.922	0.938	0.102	0.835	1780	10559	0.9912	0.2395";
		char ref = 'C';
		String result = updateResultsIllumina(illRecString, ref);
        assertTrue(result.contains("C:20,G:3"));
		
		illRecString = "rs1738240	5636391030_R02C01	A	A	0.8534			78	546817	0	A	A	T	T	A	A	6	38763733	0.8261	1.0000	[T/C]	BOT	TOP		0.021	1.517	1.469	0.048	17587	1072	0.0076	-0.2979";
		ref = 'C';
		result = updateResultsIllumina(illRecString, ref);
        assertTrue(result.contains("T:16"));
		
		illRecString = "rs235501	5636391030_R02C01	C	C	0.8590			78	643550	0	C	C	G	G	A	A	1	171556165	0.8302	1.0000	[G/C]	BOT	TOP		0.006	1.144	1.133	0.011	13745	905	0.0000	-0.0755";
		ref = 'G';
		result = updateResultsIllumina(illRecString, ref);
        assertTrue(result.contains("C:17"));
	}
	
	
	/**
	 * Method has been copied from QSignatureSequential.updateResultsIllumina
	 */
	private String updateResultsIllumina(String illRecString, char ref) {
		final int snpChipCoverageValue = 20;
		final IlluminaRecord illRec = new IlluminaRecord(TabTokenizer.tokenize(illRecString));
		
		final int[] alleleCounts = IlluminaUtils.getAllelicCounts(snpChipCoverageValue, illRec.getLogRRatio(), illRec.getRawX(), illRec.getRawY());
		String illuminaAlleles = null;
		if (illRec.getSnp().length() == 5) {
			illuminaAlleles = illRec.getSnp().substring(1, 4);
		} else {
			System.out.println("unable to process snp: " + illRec.getSnp());
		}
        assertNotNull(illuminaAlleles);
		
		// need to check that the alleles are valid
		if ( ! BaseUtils.isACGTN(illuminaAlleles.charAt(0)) || ! BaseUtils.isACGTN(illuminaAlleles.charAt(2)) ) {
			System.out.println("invalid bases in illumina genotype: " + illuminaAlleles);
		}
		
		final String illuminaCall = illRec.getFirstAlleleCall() + "" + illRec.getSecondAlleleCall();
		System.out.println("illuminaCall: " + illuminaCall);
		
		if ("--".equals(illuminaCall))  {
			System.out.println("dodgy illumina call: --");
		}
		
		char illGenChar1 = illuminaAlleles.charAt(0);
		char illGenChar2 = illuminaAlleles.charAt(1);
		if ('/' == illGenChar2) {
            illGenChar2 = illuminaAlleles.charAt(2);
		}
		
		final boolean complement = ref != illGenChar1 && ref != illGenChar2;
		final boolean newComplement = isComplemented(illuminaCall, illuminaAlleles, illRec.getFirstAlleleForward(), illRec.getSecondAlleleForward());
		final char[] alleleAB = getAlleleAandB(illuminaAlleles, illRec.getStrand());
		System.out.println("old complement: " + complement + ", new complement: " + newComplement);
			if (complement) {
			illGenChar1 = BaseUtils.getComplement(illGenChar1);
			illGenChar2 = BaseUtils.getComplement(illGenChar2);
		}
		final String result = SignatureUtil.getCoverageStringFromCharsAndInts(alleleAB[0], alleleAB[1], alleleCounts[0], alleleCounts[1]);
		System.out.println(result);
		return result;
	}
	
	private boolean isComplemented(String illuminaCall, String snp, char allele1Forward, char allele2Forward) {
		boolean complement = false;
		final char snp1 = snp.charAt(0);
		char snp2 = snp.charAt(1);
		if ('/' == snp2) {
			if (snp.length() < 3)
				throw new IllegalArgumentException("invalid illumina genotype specified: " + snp);
			snp2 = snp.charAt(2);
		}
		
		if ("AA".equals(illuminaCall)) {
			complement = (allele1Forward != snp1);
		} else if ("AB".equals(illuminaCall)) {
			complement = (allele1Forward != snp1);
		} else if ("BB".equals(illuminaCall)) {
			complement = (allele2Forward != snp2);
		}
		
		return complement;
	}
	
	private char[] getAlleleAandB(String snp, String strand) {
		final char[] alleleAB = new char[2];
		final char snp1 = snp.charAt(0);
		char snp2 = snp.charAt(1);
		if ('/' == snp2) {
			if (snp.length() < 3)
				throw new IllegalArgumentException("invalid illumina genotype specified: " + snp);
			snp2 = snp.charAt(2);
		}
		
		if (BaseUtils.isAT(snp1) && BaseUtils.isAT(snp2)) {
			// A/T or T/A - need strand
			if ("TOP".equals(strand)) {
				alleleAB[0] = 'A';
				alleleAB[1] = 'T';
			} else {
				alleleAB[0] = 'T';
				alleleAB[1] = 'A';
			}
		} else if (BaseUtils.isCG(snp1) && BaseUtils.isCG(snp2)) {
			// C/G or G/C  - need strand
			if ("TOP".equals(strand)) {
				alleleAB[0] = 'C';
				alleleAB[1] = 'G';
			} else {
				alleleAB[0] = 'G';
				alleleAB[1] = 'C';
			}
		} else {
			// A/G or A/C or G/A or C/A - no strand required
			alleleAB[0] = snp1;
			alleleAB[1] = snp2;
		}
		
		return alleleAB;
	}
    
    static void writeSnpChipFile(File snpChipFile) throws IOException {
    	try (Writer writer = new FileWriter(snpChipFile)) {
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
    	try (Writer writer = new FileWriter(illuminaArraysDesign)) {
    		writer.write("#dbSNP Id	Reference Genome	dbSNP alleles	Chr	Position(hg19)	dbSNP Strand	IlluminaDesign	ComplementArrayCalls?\n");
			writer.write("rs1000000	G	C/T	chr12	126890980	-	[T/C]	yes\n");
			writer.write("rs10000004	A	A/G	chr4	75406448	+	[T/C]	yes\n");
			writer.write("rs10000006	T	C/T	chr4	108826383	+	[A/G]	yes\n");
			writer.write("rs1000002	C	A/G	chr3	183635768	-	[A/G]	yes\n");
			writer.write("rs10000021	G	G/T	chr4	159441457	+	[A/C]	yes\n");
			writer.write("rs10000023	G	G/T	chr4	95733906	+	[T/G]	no\n");
    	}
    }
    public static void writeSnpPositionsFile(File snpPositions) throws IOException {
    	try (Writer writer = new FileWriter(snpPositions)) {
    		writer.write("chr3	183635768	random_1016708		C		RANDOM_POSITION\n");
    		writer.write("chr3	183635778	random_1016709		G		RANDOM_POSITION\n");
    		writer.write("chr4	75406448	random_649440		A		RANDOM_POSITION\n");
    		writer.write("chr4	95733906	random_1053689		G		RANDOM_POSITION\n");
    		writer.write("chr4	108826383	random_1146989		T		RANDOM_POSITION\n");
    		writer.write("chr4	159441457	random_1053689		G		RANDOM_POSITION\n");
    		writer.write("chr12	126890980	random_169627		G		RANDOM_POSITION\n");
    	}
    }
    
    /*
     * looks a little like a vcf file.... wait it is a vcf!
     */
    public static void writeSnpPositionsVcf(File snpPositions) throws IOException {
    	try (Writer writer = new FileWriter(snpPositions)) {
    		writer.write("##fileformat=VCFv4.2\n");
    		writer.write("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\n");
    		writer.write("chr3	183635768	random_1016708	C	.\t.\t.\t.\n");
    		writer.write("chr3	183635778	random_1016709	G	.\t.\t.\t.\n");
    		writer.write("chr4	75406448	random_649440	A	.\t.\t.\t.\n");
    		writer.write("chr4	95733906	random_1053689	G	.\t.\t.\t.\n");
    		writer.write("chr4	108826383	random_1146989	T	.\t.\t.\t.\n");
    		writer.write("chr4	159441457	random_1053689	G	.\t.\t.\t.\n");
    		writer.write("chr12	126890980	random_169627	G	.\t.\t.\t.\n");
    	}
    }
    static void writeGenePositionsFile(File genePositions, int start, int stop) throws IOException {
    	try (Writer writer = new FileWriter(genePositions)) {
    		writer.write("chr1" + "	ensembl_havana	gene	" + start + "	" + stop + "	.	+	.	ID=gene:ENSG00000188505;Name=NCCRP1;biotype=protein_coding;description=non-specific cytotoxic cell receptor protein 1 homolog (zebrafish) [Source:HGNC Symbol%3BAcc:33739];gene_id=ENSG00000188505;logic_name=ensembl_havana_gene;version=4\n");
    	}
    }
	
    static void getBamFile(File bamFile) {
    	getBamFile(bamFile, false);
    }
    
    static void getBamFile(File bamFile, boolean addReadGroupToHeaderAndRecords) {
    	final SAMFileHeader header = getHeader(true, true, addReadGroupToHeaderAndRecords);
    	List<SAMRecord> data = getRecords(true, header, true, addReadGroupToHeaderAndRecords);
    	final SAMWriterFactory factory = new SAMWriterFactory(header, false, bamFile, false);
    	try (SAMFileWriter writer = factory.getWriter()) {
            for (final SAMRecord s : data) writer.addAlignment(s);
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
	private static List<SAMRecord> getRecords(boolean useChr, SAMFileHeader header, boolean isValid) {
		return  getRecords( useChr,  header,  isValid, false); 
	}	
	private static List<SAMRecord> getRecords(boolean useChr, SAMFileHeader header, boolean isValid, boolean addRG) {
		List<SAMRecord> records = new ArrayList<>();
//		records.add("HS2000-152_756:1:1316:11602:65138	89	chr1	9993	25	100M	=	9993	0	TCTTCCGATCTCCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTA	B@??BBCCB<>BCBB?:BAA?9-A;?2;@ECA=;7BEE?=7D9@@8.C;B8=.HGDBBBCCD::*GGD:?*FDGFCA>EIHEEBEAEFDFFC=+?DD@@@	X0:i:1	X1:i:0	ZC:i:9	MD:Z:0C0T0G6A0A89	PG:Z:MarkDuplicates	RG:Z:20130325103517169	XG:i:0	AM:i:0	NM:i:5	SM:i:25	XM:i:5	XN:i:8	XO:i:0	XT:A:U");
		SAMRecord sam;
		
		sam = new SAMRecord(header);
		sam.setAlignmentStart(150);
		sam.setReferenceName(useChr ? "chr1" : "1");
		sam.setReadName("HS2000-152_756:2:2306:7001:4421");
		sam.setReadString("TGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAA");
		sam.setMappingQuality(60);
		sam.setBaseQualityString("CCCFFFFFHHHHHIJJJJEHGIIJIHGIIJIFGIJJJGGHHIIGCDGIHI>GIIEAFGJJI@EGFDFCE@DDDE@CA=A;3;?BDB?CD@DB9ADDBA9?");
		sam.setCigarString("2S53M1I44M");
		if (addRG) {
			sam.setAttribute("RG", "20130325112045146");
		}
		for (int i = 0 ; i < 30; i++) records.add(sam);
		
		
		sam = new SAMRecord(header);
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

        assertTrue(SAMUtils.isSAMRecordValidForVariantCalling(sam, true));
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
			sam.setFlags(67);
			sam.setAlignmentStart(127890960);
			sam.setReferenceName(useChr ? "chr12" : "12");
			sam.setReadName("HS2000-152_756:1:1215:14830:88103");
			sam.setReadString("CCCTACCCCTACCCCTACCCCTAAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTTACCCTAACCCTTACCCTAACC");
			sam.setMappingQuality(60);
			sam.setBaseQualityString("CCCFFFFFHHHHHJJIHFHJIGIHGIIJJJJIJJIIIIJJJJJIIJJJJIJJJJJJJJHHGHHFFEFCEEDD9?BABDCDDDDDDDDDDDDCDDDDDDDB");
			sam.setCigarString("24M4D76M");
			if (addRG) {
				sam.setAttribute("RG", "20130325112045146");
			}
		}
		return records;
	}
	
}