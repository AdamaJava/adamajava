package org.qcmg.sig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMProgramRecord;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.VCFRecord;
import org.qcmg.common.util.BaseUtils;
import org.qcmg.common.util.IlluminaUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.illumina.IlluminaRecord;
import org.qcmg.picard.SAMOrBAMWriterFactory;
import org.qcmg.sig.util.SignatureUtil;
import org.qcmg.vcf.VCFFileReader;

public class SignatureGeneratorTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	public SignatureGenerator qss;
	
	@Before
	public void setup() {
		qss = new SignatureGenerator();
		qss.logger = QLoggerFactory.getLogger(SignatureGeneratorTest.class);
	}
	
	
	
	@Test
	public void testPatientRegex() {
		Assert.assertEquals(true, "APGI_1992".matches(SignatureUtil.PATIENT_REGEX));
		Assert.assertEquals(false, "APG_1992".matches(SignatureUtil.PATIENT_REGEX));
		Assert.assertEquals(false, "APGI_992".matches(SignatureUtil.PATIENT_REGEX));
		Assert.assertEquals(false, "APGI_19922".matches(SignatureUtil.PATIENT_REGEX));
		Assert.assertEquals(true, "PPPP_1234".matches(SignatureUtil.PATIENT_REGEX));
		Assert.assertEquals(true, "PPPP_1234.exome.ND.bam".substring(0,9).matches(SignatureUtil.PATIENT_REGEX));
	}
	
	@Test
	public void testUpdateResultsIllumina() {
		String illRecString = "rs9349115	5636391030_R02C01	G	G	0.7370			78	1081258	0	C	C	G	G	B	B	6	39330207	0.7923	1.0000	[C/G]	TOP	BOT		0.922	0.938	0.102	0.835	1780	10559	0.9912	0.2395";
		char ref = 'C';
		String result = updateResultsIllumina(illRecString, ref);
		assertEquals(true, result.contains("C:20,G:3"));
//		
		illRecString = "rs1738240	5636391030_R02C01	A	A	0.8534			78	546817	0	A	A	T	T	A	A	6	38763733	0.8261	1.0000	[T/C]	BOT	TOP		0.021	1.517	1.469	0.048	17587	1072	0.0076	-0.2979";
		ref = 'C';
		result = updateResultsIllumina(illRecString, ref);
		assertEquals(true, result.contains("T:16"));
		
		illRecString = "rs235501	5636391030_R02C01	C	C	0.8590			78	643550	0	C	C	G	G	A	A	1	171556165	0.8302	1.0000	[G/C]	BOT	TOP		0.006	1.144	1.133	0.011	13745	905	0.0000	-0.0755";
		ref = 'G';
		result = updateResultsIllumina(illRecString, ref);
		assertEquals(true, result.contains("C:17"));
	}
	
	@Test
	public void testCreateComparatorFromSAMHeader() throws IOException {
//		SignatureGenerator qss = new SignatureGenerator();
//		qss.logger = QLoggerFactory.getLogger(SignatureGeneratorTest.class);
		try {
			qss.createComparatorFromSAMHeader(null);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		
		File bamFile = testFolder.newFile("bamFile");
		getBamFile(bamFile, false, null);
		qss.createComparatorFromSAMHeader(bamFile);

		// no seq in header - should default to ReferenceNameComparator sorting
		int i = qss.chrComparator.compare("chr1", "chr2");
		assertEquals(true, i < 0);
		i = qss.chrComparator.compare("chr10", "chr2");
		assertEquals(true, i > 0);
		
		// header is ordered chr5, 4, 3, 2, 1
		getBamFile(bamFile, true, null);
		qss.createComparatorFromSAMHeader(bamFile);
		
		qss.createComparatorFromSAMHeader(bamFile);
		i = qss.chrComparator.compare("chr1", "chr2");
		assertEquals(true, i > 0);
	}
	
	
	/**
	 * Method has been copied from QSignatureSequential.updateResultsIllumina
	 * 
	 * @see SignatureGenerator#updateResultsIllumina
	 */
	private String updateResultsIllumina(String illRecString, char ref) {
		int snpChipCoverageValue = 20;
		IlluminaRecord illRec = new IlluminaRecord(TabTokenizer.tokenize(illRecString));
		
		int[] alleleCounts = IlluminaUtils.getAllelicCounts(snpChipCoverageValue, illRec.getLogRRatio(), illRec.getRawX(), illRec.getRawY());
		String illuminaAlleles = null;
		if (illRec.getSnp().length() == 5) {
			illuminaAlleles = illRec.getSnp().substring(1, 4);
		} else {
			System.out.println("unable to process snp: " + illRec.getSnp());
		}
		
		// need to check that the alleles are valid
		if ( ! BaseUtils.isACGTN(illuminaAlleles.charAt(0)) || ! BaseUtils.isACGTN(illuminaAlleles.charAt(2)) ) {
			System.out.println("invalid bases in illumina genotype: " + illuminaAlleles);
		}
		
		String illuminaCall = illRec.getFirstAlleleCall() + "" + illRec.getSecondAlleleCall();
		System.out.println("illuminaCall: " + illuminaCall);
		
		if ("--".equals(illuminaCall))  {
			System.out.println("dodgy illumina call: --");
		}
		
		char illGenChar1 = illuminaAlleles.charAt(0);
		char illGenChar2 = illuminaAlleles.charAt(1);
		if ('/' == illGenChar2) {
			if (illuminaAlleles.length() < 3)
				throw new IllegalArgumentException("invalid illumina genotype specified: " + illuminaAlleles);
			illGenChar2 = illuminaAlleles.charAt(2);
		}
		
		boolean complement = ref != illGenChar1 && ref != illGenChar2;
		boolean newComplement = isComplemented(illuminaCall, illuminaAlleles, illRec.getFirstAlleleForward(), illRec.getSecondAlleleForward());
		char[] alleleAB = getAlleleAandB(illuminaAlleles, illRec.getStrand());
		System.out.println("old complement: " + complement + ", new complement: " + newComplement);
			if (complement) {
			illGenChar1 = BaseUtils.getComplement(illGenChar1);
			illGenChar2 = BaseUtils.getComplement(illGenChar2);
		}
		String result = SignatureUtil.getCoverageStringFromCharsAndInts(alleleAB[0], alleleAB[1], alleleCounts[0], alleleCounts[1]);
		System.out.println(result);
		return result;
	}
	
	private boolean isComplemented(String illuminaCall, String snp, char allele1Forward, char allele2Forward) {
		boolean complement = false;
		char snp1 = snp.charAt(0);
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
		char[] alleleAB = new char[2];
		char snp1 = snp.charAt(0);
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
	
	private Executor execute(final String command) throws Exception {
		return new Executor(command, "org.qcmg.sig.SignatureGenerator");
	}
	
	private void deleteFile(File outputFile) {
		if (outputFile.exists()) {
			outputFile.delete();
		}
	}

    @Test
	public void runProcessWithEmptySnpChipFile() throws Exception {
    	File positionsOfInterestFile = testFolder.newFile("runProcessWithEmptySnpChipFile.txt");
    	File snpChipFile = testFolder.newFile("runProcessWithEmptySnpChipFile_snpChip.txt");
    	File illuminaArraysDesignFile = testFolder.newFile("runProcessWithEmptySnpChipFile_snpChipIAD.txt");
    	File logFile = testFolder.newFile("runProcessWithEmptySnpChipFile.log");
    	File outputFile = testFolder.newFile("runProcessWithEmptySnpChipFile.qsig.vcf");
//    	getBamFile(snpChipFile, true, null);

		ExpectedException.none();
		Executor exec = execute("--log " + logFile.getAbsolutePath() + " -i " + positionsOfInterestFile.getAbsolutePath() + " -i " + snpChipFile.getAbsolutePath()+ " -i " + illuminaArraysDesignFile.getAbsolutePath());
		assertTrue(0 == exec.getErrCode());

		assertTrue(outputFile.exists());
	}
    
    @Test
    public void runProcessWithSnpChipFile() throws Exception {
    	File positionsOfInterestFile = testFolder.newFile("runProcessWithSnpChipFile.txt");
    	File illuminaArraysDesignFile = testFolder.newFile("runProcessWithSnpChipFileIAD.txt");
    	File snpChipFile = testFolder.newFile("runProcessWithSnpChipFile_snpChip.txt");
    	File logFile = testFolder.newFile("runProcessWithSnpChipFile.log");
    	String outputFIleName = snpChipFile.getAbsolutePath() + ".qsig.vcf";
    	File outputFile = new File(outputFIleName);
    	
    	writeSnpChipFile(snpChipFile);
    	writeSnpPositionsFile(positionsOfInterestFile);
    	writeIlluminaArraysDesignFile(illuminaArraysDesignFile);
//    	getBamFile(snpChipFile, true, null);
    	
    	int exitStatus = qss.setup(new String[] {"--log" , logFile.getAbsolutePath(), "-i" , positionsOfInterestFile.getAbsolutePath(), "-i" , snpChipFile.getAbsolutePath(),  "-i" , illuminaArraysDesignFile.getAbsolutePath()} );
    	assertEquals(0, exitStatus);
    	
//    	ExpectedException.none();
//    	Executor exec = execute("--log " + logFile.getAbsolutePath() + " -i " + positionsOfInterestFile.getAbsolutePath() + " -i " + snpChipFile.getAbsolutePath() + " -i " + illuminaArraysDesignFile.getAbsolutePath());
//    	assertTrue(0 == exec.getErrCode());
    	
    	assertTrue(outputFile.exists());
    	
    	List<VCFRecord> recs = new ArrayList<>();
    	try (VCFFileReader reader = new VCFFileReader(outputFile);) {
	    	for (VCFRecord rec : reader) {
	    		recs.add(rec);
	    	}
    	}
    	assertEquals(6, recs.size());
    	
    	// now check that any complementation has taken place
    	for (VCFRecord rec : recs) {
//    		if (rec.getChromosome().equalsIgnoreCase("chr12") && rec.getPosition() == 126890980) {
    			
    			String info = rec.getInfo();
    			System.out.println("info: " + info);
    			
//    		}
    		
    		
    		
    	}
    }
    
    private void writeSnpChipFile(File snpChipFile) throws IOException {
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
    private void writeIlluminaArraysDesignFile(File illuminaArraysDesign) throws IOException {
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
    private void writeSnpPositionsFile(File snpPositions) throws IOException {
    	try (Writer writer = new FileWriter(snpPositions);) {
    		writer.write("chr3	183635768	random_1016708	C	RANDOM_POSITION\n");
    		writer.write("chr4	75406448	random_649440	A	RANDOM_POSITION\n");
    		writer.write("chr4	95733906	random_1053689	G	RANDOM_POSITION\n");
    		writer.write("chr4	108826383	random_1146989	T	RANDOM_POSITION\n");
    		writer.write("chr4	159441457	random_1053689	G	RANDOM_POSITION\n");
    		writer.write("chr12	126890980	random_169627	G	RANDOM_POSITION\n");
    	}
    }
	
    private static void getBamFile(File bamFile, boolean validHeader, List<SAMRecord> data) {
    	SAMFileHeader header = getHeader(validHeader);
    	if ( ! validHeader) header.setSequenceDictionary(new SAMSequenceDictionary());
    	SAMOrBAMWriterFactory factory = new SAMOrBAMWriterFactory(header, false, bamFile, false);
    	try {
    		SAMFileWriter writer = factory.getWriter();
    		if (null != data)
    			for (SAMRecord s : data) writer.addAlignment(s);
    	} finally {
    		factory.closeWriter();
    	}
    }
    
	private static SAMFileHeader getHeader(boolean valid) {
		SAMFileHeader header = new SAMFileHeader();
		
		SAMProgramRecord bwaPG = new SAMProgramRecord("bwa");
		bwaPG.setProgramName("bwa");
		bwaPG.setProgramVersion("0.6.1-r104");
		header.addProgramRecord(bwaPG);
		
		if ( ! valid) {
			SAMProgramRecord invalidPG = new SAMProgramRecord("blah");
			invalidPG.setAttribute("CL", "");
			header.addProgramRecord(invalidPG);
			
			SAMReadGroupRecord rgRec = new SAMReadGroupRecord("ID");
			rgRec.setAttribute("PG", "tmap");
			header.addReadGroup(rgRec);
		}
		
		// looks like we need this to be specifically defined
		SAMSequenceDictionary seqDict = new SAMSequenceDictionary();
		SAMSequenceRecord seqRec1 = new SAMSequenceRecord("chr1", 249250621);
		SAMSequenceRecord seqRec2 = new SAMSequenceRecord("chr2", 243199373);
		SAMSequenceRecord seqRec3 = new SAMSequenceRecord("chr3", 198022430);
		SAMSequenceRecord seqRec4 = new SAMSequenceRecord("chr4", 191154276);
		SAMSequenceRecord seqRec5 = new SAMSequenceRecord("chr5", 180915260);
		seqDict.addSequence(seqRec5);
		seqDict.addSequence(seqRec4);
		seqDict.addSequence(seqRec3);
		seqDict.addSequence(seqRec2);
		seqDict.addSequence(seqRec1);
		header.setSequenceDictionary(seqDict);
		
		return header;
	}

}
