package org.qcmg.maf;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.dcc.DccConsequence;
import org.qcmg.common.dcc.MutationType;
import org.qcmg.common.model.MafType;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.maf.util.MafUtils;

public class MafPipelineSinglePatientTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public final void executeWithNoArgs() throws Exception {
		try {
			int exitStatus = new MafPipelineSinglePatient().setup(new String[] {});
			Assert.assertEquals(1, exitStatus);
		} catch (Exception e) {
			Assert.fail("no exception should have been thrown from executeWithNoArgs()");
		}
	}
	
	@Test
	public final void executeWithMissingInputFiles() throws Exception {
		exception.expect(QMafException.class);
		exception.expectMessage(Messages.getMessage("NO_ENTREZ_FILE_ERROR"));
		
		String[] args = {"-log",  testFolder.newFile("log").getAbsolutePath(), "--output", testFolder.newFile("output").getAbsolutePath()};
		new MafPipelineSinglePatient().setup(args);
	}
	
	@Test
	public final void executeWithMissingEntrezFile() throws Exception {
		exception.expect(QMafException.class);
		exception.expectMessage(Messages.getMessage("NO_ENTREZ_FILE_ERROR"));
		
		new MafPipelineSinglePatient().setup(new String[] {"-log",  testFolder.newFile("log").getAbsolutePath(), "-entrez", ""});
	}
	
	@Ignore
	public final void executeWithMissingCanonicalTranscriptsFile() throws Exception {
		exception.expect(QMafException.class);
		exception.expectMessage(Messages.getMessage("NO_CANONICAL_FILE_ERROR"));
		
		new MafPipelineSinglePatient().setup(new String[]{"-log",  testFolder.newFile("log").getAbsolutePath(),
			"-canonical", "", "-entrez", testFolder.newFile("entrez").getAbsolutePath()});
	}
	
	@Ignore
	public final void executeWithMissingVerificationFile() throws Exception {
		exception.expect(QMafException.class);
		exception.expectMessage(Messages.getMessage("NO_VERIFIED_FILE_ERROR"));
		
		new MafPipelineSinglePatient().setup(new String[]{"-log",  testFolder.newFile("log").getAbsolutePath(),
				"-entrez", testFolder.newFile("entrez").getAbsolutePath(),
				"-canonical", testFolder.newFile("canonical").getAbsolutePath()});
	}
	
	@Ignore
	public final void executeWithMissingDbSnpFile() throws Exception {
		exception.expect(QMafException.class);
		exception.expectMessage(Messages.getMessage("NO_DBSNP_FILE_ERROR"));
		
		new MafPipelineSinglePatient().setup(new String[]{"-log",  testFolder.newFile("log").getAbsolutePath(),
				"-entrez", testFolder.newFile("entrez").getAbsolutePath(),
				"-canonical", testFolder.newFile("canonical").getAbsolutePath(),
				"-verified", testFolder.newFile("verified").getAbsolutePath()});
	}		
	
	@Ignore
	public final void executeWithMissingKrasFile() throws Exception {
		exception.expect(QMafException.class);
		exception.expectMessage(Messages.getMessage("NO_KRAS_FILE_ERROR"));
		
		new MafPipelineSinglePatient().setup(new String[]{"-log",  testFolder.newFile("log").getAbsolutePath(),
				"-entrez", testFolder.newFile("entrez").getAbsolutePath(),
				"-canonical", testFolder.newFile("canonical").getAbsolutePath(),
				"-verified", testFolder.newFile("verified").getAbsolutePath(),
				"-dbSNP", testFolder.newFile("dbsnp").getAbsolutePath()});
	}
	
	@Ignore
	public final void executeWithMissingFastaFile() throws Exception {
		exception.expect(QMafException.class);
		exception.expectMessage(Messages.getMessage("NO_FASTA_FILE_ERROR"));
		
		new MafPipelineSinglePatient().setup(new String[]{"-log",  testFolder.newFile("log").getAbsolutePath(),
				"-entrez", testFolder.newFile("entrez").getAbsolutePath(),
				"-canonical", testFolder.newFile("canonical").getAbsolutePath(),
				"-verified", testFolder.newFile("verified").getAbsolutePath(),
				"-kras", testFolder.newFile("kras").getAbsolutePath(),
				"-dbSNP", testFolder.newFile("dbsnp").getAbsolutePath()});
	}
	
	@Ignore
	public final void executeWithMissingGffFile() throws Exception {
		exception.expect(QMafException.class);
		exception.expectMessage(Messages.getMessage("NO_GFF_FILE_ERROR"));
		
		new MafPipelineSinglePatient().setup(new String[]{"-log",  testFolder.newFile("log").getAbsolutePath(),
				"-entrez", testFolder.newFile("entrez").getAbsolutePath(),
				"-canonical", testFolder.newFile("canonical").getAbsolutePath(),
				"-verified", testFolder.newFile("verified").getAbsolutePath(),
				"-kras", testFolder.newFile("kras").getAbsolutePath(),
				"-fasta", testFolder.newFile("fasta").getAbsolutePath(),
				"-dbSNP", testFolder.newFile("dbsnp").getAbsolutePath()});
	}
	
	@Ignore
	public final void executeWithAllFile() throws Exception {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Supplied name is not a valid directory or file");
		
		new MafPipelineSinglePatient().setup(new String[]{"-log",  testFolder.newFile("log").getAbsolutePath(),
				"-entrez", testFolder.newFile("entrez").getAbsolutePath(),
				"-canonical", testFolder.newFile("canonical").getAbsolutePath(),
				"-verified", testFolder.newFile("verified").getAbsolutePath(),
				"-kras", testFolder.newFile("kras").getAbsolutePath(),
				"-fasta", testFolder.newFile("fasta").getAbsolutePath(),
				"-gff", testFolder.newFile("gff").getAbsolutePath(),
				"-dbSNP", testFolder.newFile("dbsnp").getAbsolutePath(),
				"-dir", testFolder.newFolder("output").getAbsolutePath()});
	}
	
	@Test
	public void testFilenameFilterRegex() {
		Assert.assertEquals(false, "".matches(MafPipelineSinglePatient.datePattern));
		Assert.assertEquals(false, "1234".matches(MafPipelineSinglePatient.datePattern));
		Assert.assertEquals(false, "1234567".matches(MafPipelineSinglePatient.datePattern));
		Assert.assertEquals(true, "12345678".matches(MafPipelineSinglePatient.datePattern));
		Assert.assertEquals(false, "123456789".matches(MafPipelineSinglePatient.datePattern));
		
		Assert.assertEquals(false, "".matches(MafPipelineSinglePatient.dateAPattern));
		Assert.assertEquals(false, "_A".matches(MafPipelineSinglePatient.dateAPattern));
		Assert.assertEquals(false, "1234_A".matches(MafPipelineSinglePatient.dateAPattern));
		Assert.assertEquals(false, "1234567_A".matches(MafPipelineSinglePatient.dateAPattern));
		Assert.assertEquals(true, "12345678_A".matches(MafPipelineSinglePatient.dateAPattern));
		Assert.assertEquals(false, "_A12345678".matches(MafPipelineSinglePatient.dateAPattern));
		Assert.assertEquals(false, "123456789".matches(MafPipelineSinglePatient.dateAPattern));
		Assert.assertEquals(false, "123456789_A".matches(MafPipelineSinglePatient.dateAPattern));
		Assert.assertEquals(false, "_A123456789".matches(MafPipelineSinglePatient.dateAPattern));
		Assert.assertEquals(false, "1234_A5678".matches(MafPipelineSinglePatient.dateAPattern));
		
		Assert.assertEquals(false, "".matches(MafPipelineSinglePatient.patientPattern));
		Assert.assertEquals(false, "apgi".matches(MafPipelineSinglePatient.patientPattern));
		Assert.assertEquals(false, "APGI".matches(MafPipelineSinglePatient.patientPattern));
		Assert.assertEquals(false, "APGI_123".matches(MafPipelineSinglePatient.patientPattern));
		Assert.assertEquals(false, "APGI_12345".matches(MafPipelineSinglePatient.patientPattern));
		Assert.assertEquals(false, "apgi_1234".matches(MafPipelineSinglePatient.patientPattern));
		Assert.assertEquals(true, "APGI_4321".matches(MafPipelineSinglePatient.patientPattern));
		Assert.assertEquals(true, "APGI_1992".matches(MafPipelineSinglePatient.patientPattern));
	}
	
	@Test
	public void testDateDirectoryFilter() {
		File testDir = testFolder.newFolder("test");
		File [] foundFiles = FileUtils.findFiles(testDir.getAbsolutePath(), MafPipelineSinglePatient.dateDirectoryFilter);
		Assert.assertEquals(0, foundFiles.length);
		
		testFolder.newFolder("test/1234");
		foundFiles = FileUtils.findFiles(testDir.getAbsolutePath(), MafPipelineSinglePatient.dateDirectoryFilter);
		Assert.assertEquals(0, foundFiles.length);
		testFolder.newFolder("test/12345678");
		foundFiles = FileUtils.findFiles(testDir.getAbsolutePath(), MafPipelineSinglePatient.dateDirectoryFilter);
		Assert.assertEquals(1, foundFiles.length);
		
		// _A file filter
		foundFiles = FileUtils.findFiles(testDir.getAbsolutePath(), MafPipelineSinglePatient.dateADirectoryFilter);
		Assert.assertEquals(0, foundFiles.length);
		
		testFolder.newFolder("test/12345678_A");
		foundFiles = FileUtils.findFiles(testDir.getAbsolutePath(), MafPipelineSinglePatient.dateADirectoryFilter);
		Assert.assertEquals(1, foundFiles.length);
		testFolder.newFolder("test/20111231_A");
		foundFiles = FileUtils.findFiles(testDir.getAbsolutePath(), MafPipelineSinglePatient.dateADirectoryFilter);
		Assert.assertEquals(2, foundFiles.length);
		
		// patient file filter
		foundFiles = FileUtils.findFiles(testDir.getAbsolutePath(), MafPipelineSinglePatient.patientDirectoryFilter);
		Assert.assertEquals(0, foundFiles.length);
		
		testFolder.newFolder("test/APGI_1234");
		foundFiles = FileUtils.findFiles(testDir.getAbsolutePath(), MafPipelineSinglePatient.patientDirectoryFilter);
		Assert.assertEquals(1, foundFiles.length);
		testFolder.newFolder("test/APGI_5555");
		foundFiles = FileUtils.findFiles(testDir.getAbsolutePath(), MafPipelineSinglePatient.patientDirectoryFilter);
		Assert.assertEquals(2, foundFiles.length);
		
		testFolder.newFolder("test/AGPI_1234");
		foundFiles = FileUtils.findFiles(testDir.getAbsolutePath(), MafPipelineSinglePatient.patientDirectoryFilter);
		Assert.assertEquals(2, foundFiles.length);
		
		testFolder.newFolder("test/APGI_XXXX");
		foundFiles = FileUtils.findFiles(testDir.getAbsolutePath(), MafPipelineSinglePatient.patientDirectoryFilter);
		Assert.assertEquals(2, foundFiles.length);
	}
	
	@Test
	public void testMAFRecordComparator() {
		MAFRecord m1_1 = new MAFRecord();
		m1_1.setChromosome("1");
		m1_1.setStartPosition(1);
		m1_1.setEndPosition(1);
		MAFRecord m2_2 = new MAFRecord();
		m2_2.setChromosome("2");
		m2_2.setStartPosition(2);
		m2_2.setEndPosition(2);
		
		List<MAFRecord> mafs = new ArrayList<MAFRecord>();
		mafs.add(m2_2);
		mafs.add(m1_1);
		
		Assert.assertEquals(m2_2, mafs.get(0));
		Assert.assertEquals(m1_1, mafs.get(1));
		
		Collections.sort(mafs, MafPipelineSinglePatient.MAF_COMPARATOR);
		
		Assert.assertEquals(m1_1, mafs.get(0));
		Assert.assertEquals(m2_2, mafs.get(1));
		
		MAFRecord m1_2 = new MAFRecord();
		m1_2.setChromosome("1");
		m1_2.setStartPosition(2);
		m1_2.setEndPosition(2);
		MAFRecord m1_4 = new MAFRecord();
		m1_4.setChromosome("1");
		m1_4.setStartPosition(4);
		m1_4.setEndPosition(4);
		
		mafs = new ArrayList<MAFRecord>();
		mafs.add(m2_2);
		mafs.add(m1_2);
		mafs.add(m1_1);
		mafs.add(m1_4);
		
		Assert.assertEquals(m2_2, mafs.get(0));
		Assert.assertEquals(m1_2, mafs.get(1));
		Assert.assertEquals(m1_1, mafs.get(2));
		Assert.assertEquals(m1_4, mafs.get(3));
		
		Collections.sort(mafs, MafPipelineSinglePatient.MAF_COMPARATOR);
		
		Assert.assertEquals(m1_1, mafs.get(0));
		Assert.assertEquals(m1_2, mafs.get(1));
		Assert.assertEquals(m1_4, mafs.get(2));
		Assert.assertEquals(m2_2, mafs.get(3));
	}
	
	@Test
	public void testPassesFinalFilter() {
		MAFRecord maf = new MAFRecord();
		maf.setVariantType(MutationType.SNP);
		Assert.assertEquals(false, MafPipelineSinglePatient.passesFinalFilter(maf));
		maf.setHugoSymbol("KRAS");
		Assert.assertEquals(true, MafPipelineSinglePatient.passesFinalFilter(maf));
		maf.setHugoSymbol("TPG");
		maf.setNovelStartCount(3);
		Assert.assertEquals(false, MafPipelineSinglePatient.passesFinalFilter(maf));
		maf.setNovelStartCount(4);
		Assert.assertEquals(true, MafPipelineSinglePatient.passesFinalFilter(maf));
		maf.setVariantType(MutationType.INS);
		Assert.assertEquals(true, MafPipelineSinglePatient.passesFinalFilter(maf));
		maf.setVariantType(MutationType.DEL);
		Assert.assertEquals(true, MafPipelineSinglePatient.passesFinalFilter(maf));
	}
	
	@Test
	public void testFilter() {
		// real life maf record
		//SPTA1	6708	qcmg.uq.edu.au	37	1	158626363	158626363	+	Silent	SNP	G	A	G	novel	null	QCMG-66-APGI_2150-ICGC-ABMJ-20100813-24-TD	QCMG-66-APGI_2150-ICGC-ABMJ-20100813-23-ND	G	G	null	null	null	null	null	Unknown	Somatic	null	Capture	null	null	null	SOLID	--	G:76[37.26],3[33.27]	G:84[34.38],15[36.36],A:11[35.84],0[0]	ENST00000368148	N963N	3088G>A	null	null	null
		
		
		MAFRecord maf = new MAFRecord();
		maf.setVariantType(MutationType.SNP);
		maf.setMafType(MafType.SNV_SOMATIC);
		maf.setHugoSymbol("SPTA1");
		maf.setVariantClassification("Silent");
		maf.setFlag(SnpUtils.PASS);
		maf.setRef("G");
		maf.setTumourAllele1("G");
		maf.setTumourAllele2("A");
		maf.setTd("G:84[34.38],15[36.36],A:11[35.84],0[0]");
		maf.setNovelStartCount(4);
		
		boolean novelDbSnp = true;
//		String variant = maf.getRef().equals(maf.getTumourAllele1()) ? maf.getTumourAllele2() : maf.getTumourAllele1();
		char alt = MafUtils.getVariant(maf).charAt(0);
		
		// first part of the filter
		Assert.assertEquals(true, DccConsequence.passesMafNameFilter(maf.getVariantClassification()));
		// second part of the filter
		Assert.assertEquals(true, MafUtils.passesHighConfidenceFilter(maf.getFlag(), maf.getVariantType(), maf.getTd(),novelDbSnp , alt));
		// final filter
		Assert.assertEquals(true, MafPipelineSinglePatient.passesFinalFilter(maf));
	}
	
	@Test
	public void testFilter2() {
		// real life maf record
		//ENSG00000228273 0       qcmg.uq.edu.au  37      7       100550291       100550291       +       Missense_Mutation       SNP     C       A       C       novel   null    QCMG-66-APGI_2353-ICGC_ABMJ_20110807_02_TD      QCMG-66-APGI_2353-ICGC_ABMJ_20110807_01_ND      C       C       null    null    null    null    null    Unknown Somatic null    Capture null    null    null    SOLID   C:23[24.75],20[27.46],A:0[0],6[24.86]   C:23[24.75],20[27.46],A:0[0],6[24.86]   C:26[25.1],33[26.9],A:1[30],18[28.17],T:0[0],1[2]       ENST00000379458 -888    -888    null    null    null
		
		
		MAFRecord maf = new MAFRecord();
		maf.setVariantType(MutationType.SNP);
		maf.setMafType(MafType.SNV_SOMATIC);
		maf.setHugoSymbol("ENSG00000228273");
		maf.setVariantClassification("Missense_Mutation");
		maf.setFlag(SnpUtils.PASS);
		maf.setRef("C");
		maf.setTumourAllele1("A");
		maf.setTumourAllele2("C");
		maf.setTd("C:26[25.1],33[26.9],A:1[30],18[28.17],T:0[0],1[2]");
		maf.setNovelStartCount(4);
		
		boolean novelDbSnp = true;
//		String variant = maf.getRef().equals(maf.getTumourAllele1()) ? maf.getTumourAllele2() : maf.getTumourAllele1();
		char alt = MafUtils.getVariant(maf).charAt(0);
		
		// first part of the filter
		Assert.assertEquals(true, DccConsequence.passesMafNameFilter(maf.getVariantClassification()));
		// second part of the filter
		Assert.assertEquals(true, MafUtils.passesHighConfidenceFilter(maf.getFlag(), maf.getVariantType(), maf.getTd(),novelDbSnp , alt));
		// final filter
		Assert.assertEquals(true, MafPipelineSinglePatient.passesFinalFilter(maf));
	}
}
