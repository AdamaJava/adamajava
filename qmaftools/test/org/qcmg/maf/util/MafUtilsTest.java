package org.qcmg.maf.util;


import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.dcc.DccConsequence;
import org.qcmg.common.dcc.MutationType;
import org.qcmg.common.maf.MAFRecord;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.MafType;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.maf.QMafException;
import org.qcmg.tab.TabbedRecord;

public class MafUtilsTest {
	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void testUpdateFlag() {
		try {
			MafUtils.updateFlag(null, null);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		MAFRecord maf = new MAFRecord();
		MafUtils.updateFlag(maf, null);
		assertEquals(null, maf.getFlag());
		MafUtils.updateFlag(maf, "");
		assertEquals("", maf.getFlag());
		MafUtils.updateFlag(maf, SnpUtils.PASS);
		assertEquals(SnpUtils.PASS, maf.getFlag());
		MafUtils.updateFlag(maf, SnpUtils.PASS);
		assertEquals(SnpUtils.PASS, maf.getFlag());
		MafUtils.updateFlag(maf, SnpUtils.MUTATION_IN_NORMAL);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL, maf.getFlag());
		MafUtils.updateFlag(maf, SnpUtils.MUTANT_READS);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL + ";" + SnpUtils.MUTANT_READS, maf.getFlag());
		MafUtils.updateFlag(maf, SnpUtils.PASS);
		assertEquals(SnpUtils.PASS, maf.getFlag());
		MafUtils.updateFlag(maf, SnpUtils.MUTANT_READS);
		assertEquals(SnpUtils.MUTANT_READS, maf.getFlag());
		MafUtils.updateFlag(maf, SnpUtils.MUTANT_READS);
		assertEquals(SnpUtils.MUTANT_READS, maf.getFlag());
	}

	@Test
	public void testSetupStaticMafFields() {
		MAFRecord maf = new MAFRecord();
		MafUtils.setupStaticMafFields(maf, "patient1", "XXXXX","YYYYY", true);
		assertEquals("QCMG-66-patient1-YYYYY", maf.getTumourSampleBarcode());
		assertEquals("QCMG-66-patient1-XXXXX", maf.getNormalSampleBarcode());
		assertEquals("qcmg.uq.edu.au", maf.getCenter());
		assertEquals("Somatic", maf.getMutationStatus());
		assertEquals("Unknown", maf.getSequencer());
		assertEquals("Unknown", maf.getSequencingSource());
		assertEquals(37, maf.getNcbiBuild());
		
		MafUtils.setupStaticMafFields(maf, "patient1", "XXXXX","YYYYY", false);
		assertEquals("QCMG-66-patient1-YYYYY", maf.getTumourSampleBarcode());
		assertEquals("QCMG-66-patient1-XXXXX", maf.getNormalSampleBarcode());
		assertEquals("qcmg.uq.edu.au", maf.getCenter());
		assertEquals("Germline", maf.getMutationStatus());
		assertEquals("Unknown", maf.getSequencer());
		assertEquals("Unknown", maf.getSequencingSource());
		assertEquals(37, maf.getNcbiBuild());
	}
	
	@Test
	public void testGetHugoSymbolNull() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Null value passed to getHugoSymbol");
		MafUtils.getHugoSymbol(null);
	}
	@Test
	public void testGetHugoSymbol() {
		assertEquals("", MafUtils.getHugoSymbol(""));
		assertEquals("abc", MafUtils.getHugoSymbol("abc"));
		assertEquals("123", MafUtils.getHugoSymbol("123"));
		assertEquals("abc123", MafUtils.getHugoSymbol("abc123"));
		assertEquals("Unknown", MafUtils.getHugoSymbol("-888"));
		assertEquals("888", MafUtils.getHugoSymbol("888"));
		assertEquals("Unknown", MafUtils.getHugoSymbol("Unknown"));
	}
	
	@Test
	public void testGetDBSNPId() {
		assertEquals("novel", MafUtils.getDbSnpId(null));
		assertEquals("novel", MafUtils.getDbSnpId(""));
		assertEquals("abc", MafUtils.getDbSnpId("abc"));
		assertEquals("123", MafUtils.getDbSnpId("123"));
		assertEquals("abc123", MafUtils.getDbSnpId("abc123"));
		assertEquals("novel", MafUtils.getDbSnpId("-888"));
		assertEquals("888", MafUtils.getDbSnpId("888"));
		assertEquals("novel", MafUtils.getDbSnpId("-999"));
		assertEquals("999", MafUtils.getDbSnpId("999"));
		assertEquals("Unknown", MafUtils.getDbSnpId("Unknown"));
	}
	
	@Test
	public void testGetFullChrFromMafChrNull() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Null or empty string passed to getFullChrFromMafChr");
		assertEquals(null, MafUtils.getFullChrFromMafChr(null));
	}
	@Test
	public void testGetFullChrFromMafChrEmptyStr() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Null or empty string passed to getFullChrFromMafChr");
		assertEquals("", MafUtils.getFullChrFromMafChr(""));
	}
	@Test
	public void testGetFullChrFromMafChr() {
		assertEquals("chr1", MafUtils.getFullChrFromMafChr("1"));
		assertEquals("chr100", MafUtils.getFullChrFromMafChr("100"));
		assertEquals("chrX", MafUtils.getFullChrFromMafChr("x"));
		assertEquals("chrA", MafUtils.getFullChrFromMafChr("aBC"));
		assertEquals("chrBBC", MafUtils.getFullChrFromMafChr("BBC"));
		assertEquals("chrB", MafUtils.getFullChrFromMafChr("bBC"));
		assertEquals("chrMT", MafUtils.getFullChrFromMafChr("m"));
		assertEquals("chrMT", MafUtils.getFullChrFromMafChr("mT"));
		assertEquals("chrMT", MafUtils.getFullChrFromMafChr("M"));
		assertEquals("chrMT", MafUtils.getFullChrFromMafChr("MT"));
		assertEquals("chrXXYYZZ", MafUtils.getFullChrFromMafChr("XXYYZZ"));
	}
	
	@Test
	public void testGetFullChromosomeNull() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Null maf object passed to getFullChromosome");
		MafUtils.getFullChromosome(null);
	}
	@Test
	public void testGetFullChromosomeEmptyStr() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Null or empty string passed to getFullChrFromMafChr");
		MafUtils.getFullChromosome(new MAFRecord());
	}
	@Test
	public void testGetFullChromosome() {
		MAFRecord maf = new MAFRecord();
		maf.setChromosome("y");
		assertEquals("chrY", MafUtils.getFullChromosome(maf));
		maf.setChromosome("m");
		assertEquals("chrMT", MafUtils.getFullChromosome(maf));
	}
	
	@Test
	public void testGetVariantNull() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Null maf object passed to getVariant");
		MafUtils.getVariant(null);
	}
	
	@Test
	public void testGetVariantNullAttributes() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Null maf object passed to getVariant");
		MafUtils.getVariant(new MAFRecord());
	}
	
	@Test
	public void testGetVariantNoMafType() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Null maf object passed to getVariant");
		MAFRecord maf = new MAFRecord();
		maf.setRef("A");
		MafUtils.getVariant(maf);
	}
	
	@Test
	public void testGetVariantSomatic() {
		MAFRecord maf = new MAFRecord();
		maf.setMafType(MafType.SNV_SOMATIC);
		maf.setRef("A");
		maf.setTumourAllele1("A");
		maf.setTumourAllele2("B");
		assertEquals("B", MafUtils.getVariant(maf));
		maf.setRef("B");
		maf.setTumourAllele1("A");
		maf.setTumourAllele2("B");
		assertEquals("A", MafUtils.getVariant(maf));
		maf.setRef("BBC");
		maf.setTumourAllele1("ABC");
		maf.setTumourAllele2("BBC");
		assertEquals("ABC", MafUtils.getVariant(maf));
		maf.setRef("BBC-");
		maf.setTumourAllele1("AB-C");
		maf.setTumourAllele2("BBC");
		assertEquals("AB-C", MafUtils.getVariant(maf));
	}
	@Test
	public void testGetVariantGermline() {
		MAFRecord maf = new MAFRecord();
		maf.setMafType(MafType.SNV_GERMLINE);
		maf.setRef("A");
		maf.setTumourAllele1("A");
		maf.setTumourAllele2("B");
		try {
			assertEquals("B", MafUtils.getVariant(maf));
			Assert.fail("SHould have thrown an exception");
		} catch (Exception e) {}
		
		maf.setNormalAllele1("A");
		maf.setNormalAllele2("B");
		assertEquals("B", MafUtils.getVariant(maf));
		
		maf.setRef("B");
		maf.setNormalAllele1("A");
		maf.setNormalAllele2("B");
		assertEquals("A", MafUtils.getVariant(maf));
		maf.setRef("BBC");
		maf.setNormalAllele1("ABC");
		maf.setNormalAllele2("BBC");
		assertEquals("ABC", MafUtils.getVariant(maf));
		maf.setRef("BBC-");
		maf.setNormalAllele1("AB-C");
		maf.setNormalAllele2("BBC");
		assertEquals("AB-C", MafUtils.getVariant(maf));
	}
	
	@Test
	public void testPassesCountCheck() {
		assertEquals(false, MafUtils.passesCountCheck(null, -1, '\u0000'));
//		assertEquals(false, MafUtils.passesCountCheck(null, -1, '\u0000'));
		assertEquals(false, MafUtils.passesCountCheck("", -1, '\u0000'));
		//T:7[40],0[0],G:5[32.68],0[0]
		assertEquals(false, MafUtils.passesCountCheck("A:0[0],0[0]", 1, 'A'));
		assertEquals(true, MafUtils.passesCountCheck("A:1[0],0[0]", 1, 'A'));
		assertEquals(true, MafUtils.passesCountCheck("A:1[0],2[0],G:3[0],4[0],C:5[0],6[0],T:7[0],8[0]", 1, 'A'));
		assertEquals(false, MafUtils.passesCountCheck("A:1[0],2[0],G:3[0],4[0],C:5[0],6[0],T:7[0],8[0]", 4, 'A'));
		assertEquals(true, MafUtils.passesCountCheck("A:1[0],2[0],G:3[0],4[0],C:5[0],6[0],T:7[0],8[0]", 7, 'G'));
		assertEquals(false, MafUtils.passesCountCheck("A:1[0],2[0],G:3[0],4[0],C:5[0],6[0],T:7[0],8[0]", 8, 'G'));
		assertEquals(true, MafUtils.passesCountCheck("A:1[0],2[0],G:3[0],4[0],C:5[0],6[0],T:7[0],8[0]", 11, 'C'));
		assertEquals(false, MafUtils.passesCountCheck("A:1[0],2[0],G:3[0],4[0],C:5[0],6[0],T:7[0],8[0]", 12, 'C'));
		assertEquals(true, MafUtils.passesCountCheck("A:1[0],2[0],G:3[0],4[0],C:5[0],6[0],T:7[0],8[0]", 15, 'T'));
		assertEquals(false, MafUtils.passesCountCheck("A:1[0],2[0],G:3[0],4[0],C:5[0],6[0],T:7[0],8[0]", 16, 'T'));
	}
	
	@Test
	public void testPassesLowConfidenceFilter() {
		assertEquals(false, MafUtils.passesLowerConfidenceFilter(null, null, null, '\u0000'));
		assertEquals(false, MafUtils.passesLowerConfidenceFilter(SnpUtils.PASS, null, null, '\u0000'));		// classA flag
		assertEquals(false, MafUtils.passesLowerConfidenceFilter(SnpUtils.PASS, MutationType.SNP, null, '\u0000'));
		assertEquals(true, MafUtils.passesLowerConfidenceFilter(SnpUtils.PASS, MutationType.INS, null, '\u0000'));
		assertEquals(true, MafUtils.passesLowerConfidenceFilter(SnpUtils.PASS, MutationType.DEL, null, '\u0000'));
		assertEquals(false, MafUtils.passesLowerConfidenceFilter(SnpUtils.PASS, MutationType.SNP, "", '\u0000'));
		assertEquals(false, MafUtils.passesLowerConfidenceFilter(SnpUtils.PASS, MutationType.SNP, "", '\u0000'));
		assertEquals(false, MafUtils.passesLowerConfidenceFilter(SnpUtils.PASS, MutationType.SNP, "A:1[0],2[0],G:3[0],4[0],C:5[0],6[0],T:7[0],8[0]", '\u0000'));
		assertEquals(true, MafUtils.passesLowerConfidenceFilter(SnpUtils.PASS, MutationType.SNP, "A:1[0],2[0],G:3[0],4[0],C:5[0],6[0],T:7[0],8[0]", 'G'));
		assertEquals(true, MafUtils.passesLowerConfidenceFilter(SnpUtils.PASS, MutationType.SNP, "A:1[0],2[0],G:3[0],4[0],C:5[0],6[0],T:7[0],8[0]", 'C'));
		assertEquals(true, MafUtils.passesLowerConfidenceFilter(SnpUtils.PASS, MutationType.SNP, "A:1[0],2[0],G:3[0],4[0],C:5[0],6[0],T:7[0],8[0]", 'T'));
		assertEquals(false, MafUtils.passesLowerConfidenceFilter(SnpUtils.PASS, MutationType.SNP, "A:1[0],2[0],G:3[0],4[0],C:5[0],6[0],T:7[0],8[0]", 'A'));
		assertEquals(true, MafUtils.passesLowerConfidenceFilter(SnpUtils.LESS_THAN_12_READS_NORMAL, MutationType.SNP, "A:1[0],2[0],G:3[0],4[0],C:5[0],6[0],T:7[0],8[0]", 'T'));
		assertEquals(false, MafUtils.passesLowerConfidenceFilter(SnpUtils.MUTATION_IN_NORMAL, MutationType.SNP, "A:1[0],2[0],G:3[0],4[0],C:5[0],6[0],T:7[0],8[0]", 'T'));
	}
	
	@Test
	public void testPassesHighConfidenceFilter() {
		assertEquals(false, MafUtils.passesHighConfidenceFilter(null, null, null, false, '\u0000'));
		assertEquals(false, MafUtils.passesHighConfidenceFilter(SnpUtils.PASS, null, null, false, '\u0000'));		// classA flag
		assertEquals(false, MafUtils.passesHighConfidenceFilter(SnpUtils.PASS, MutationType.SNP, null, false, '\u0000'));
		assertEquals(false, MafUtils.passesHighConfidenceFilter(SnpUtils.PASS, MutationType.INS, null, false, '\u0000'));
		assertEquals(false, MafUtils.passesHighConfidenceFilter(SnpUtils.PASS, MutationType.DEL, null, false, '\u0000'));
		assertEquals(true, MafUtils.passesHighConfidenceFilter(SnpUtils.PASS, MutationType.INS, null, true, '\u0000'));
		assertEquals(true, MafUtils.passesHighConfidenceFilter(SnpUtils.PASS, MutationType.DEL, null, true, '\u0000'));
		assertEquals(false, MafUtils.passesHighConfidenceFilter(SnpUtils.PASS, MutationType.SNP, "", false, '\u0000'));
		assertEquals(false, MafUtils.passesHighConfidenceFilter(SnpUtils.PASS, MutationType.SNP, "", false, '\u0000'));
		assertEquals(false, MafUtils.passesHighConfidenceFilter(SnpUtils.PASS, MutationType.SNP, "A:2[0],2[0],G:3[0],2[0],C:5[0],6[0],T:7[0],8[0]", false, '\u0000'));
		assertEquals(true, MafUtils.passesHighConfidenceFilter(SnpUtils.PASS, MutationType.SNP, "A:2[0],2[0],G:3[0],2[0],C:5[0],6[0],T:7[0],8[0]", false, 'G'));
		assertEquals(true, MafUtils.passesHighConfidenceFilter(SnpUtils.PASS, MutationType.SNP, "A:2[0],2[0],G:3[0],2[0],C:5[0],6[0],T:7[0],8[0]", false, 'C'));
		assertEquals(true, MafUtils.passesHighConfidenceFilter(SnpUtils.PASS, MutationType.SNP, "A:2[0],2[0],G:3[0],2[0],C:5[0],6[0],T:7[0],8[0]", false, 'T'));
		assertEquals(false, MafUtils.passesHighConfidenceFilter(SnpUtils.PASS, MutationType.SNP, "A:2[0],2[0],G:3[0],2[0],C:5[0],6[0],T:7[0],8[0]", false, 'A'));
		assertEquals(false, MafUtils.passesHighConfidenceFilter(SnpUtils.LESS_THAN_12_READS_NORMAL, MutationType.SNP, "A:2[0],2[0],G:3[0],2[0],C:5[0],6[0],T:7[0],8[0]", false, 'T'));
	}
	
	@Test
	public void testLoadPositionsOfInterest() throws Exception {
		File emptyFile = testFolder.newFile("empty");
		List<ChrPosition> collection = new ArrayList<ChrPosition>();
		
		try (FileWriter writer = new FileWriter(emptyFile)) {
			MafUtils.loadPositionsOfInterest(emptyFile.getAbsolutePath(), collection);
			assertEquals(0, collection.size());
			// no header, just records
			writer.write("\t\t\t\t1\t12345\t12345");
			writer.flush();
		}
		
		MafUtils.loadPositionsOfInterest(emptyFile.getAbsolutePath(), collection);
		assertEquals(1, collection.size());
		assertEquals("1", (collection.get(0)).getChromosome());
		assertEquals(12345, (collection.get(0)).getPosition());
		
		// just header
		File headerFile = testFolder.newFile("header");
		try (FileWriter w2 = new FileWriter(headerFile)){
			w2.write("#version 2.2");
			w2.flush();
			
			collection = new ArrayList<ChrPosition>();
			MafUtils.loadPositionsOfInterest(headerFile.getAbsolutePath(), collection);
			assertEquals(0, collection.size());
			
			// add the fields to the header
			w2.write("#Hugo_Symbol\tEntrez_Gene_Id\tCenter\tNCBI_Build\tChromosome\tStart_Position\tEnd_Position\tStrand\tVariant_Classification\t" +
			"Variant_Type\tReference_Allele\tTumor_Seq_Allele1\tTumor_Seq_Allele2\tdbSNP_RS\tdbSNP_Val_Status\tTumor_Sample_Barcode\tMatched_Norm_Sample_Barcode\t" +
			"Match_Norm_Seq_Allele1\tMatch_Norm_Seq_Allele2\tTumor_Validation_Allele1\tTumor_Validation_Allele2\tMatch_Norm_Validation_Allele1\t" +
			"Match_Norm_Validation_Allele2\tVerification_Status\tValidation_Status\tMutation_Status\tSequencing_Phase\tSequence_Source\t" +
			"Validation_Method\tScore\tBAM_File\tSequencer\n");
			w2.flush();
			MafUtils.loadPositionsOfInterest(headerFile.getAbsolutePath(), collection);
			assertEquals(0, collection.size());
			
			// add another header line - this time without the # - should still give an empty collection
			w2.write("Hugo_Symbol\tEntrez_Gene_Id\tCenter\tNCBI_Build\tChromosome\tStart_Position\tEnd_Position\tStrand\tVariant_Classification\t" +
					"Variant_Type\tReference_Allele\tTumor_Seq_Allele1\tTumor_Seq_Allele2\tdbSNP_RS\tdbSNP_Val_Status\tTumor_Sample_Barcode\tMatched_Norm_Sample_Barcode\t" +
					"Match_Norm_Seq_Allele1\tMatch_Norm_Seq_Allele2\tTumor_Validation_Allele1\tTumor_Validation_Allele2\tMatch_Norm_Validation_Allele1\t" +
					"Match_Norm_Validation_Allele2\tVerification_Status\tValidation_Status\tMutation_Status\tSequencing_Phase\tSequence_Source\t" +
			"Validation_Method\tScore\tBAM_File\tSequencer\n");
			w2.flush();
			MafUtils.loadPositionsOfInterest(headerFile.getAbsolutePath(), collection);
			assertEquals(0, collection.size());
			
			// and finally some data
			w2.write("\t\t\t\t1\t12345\t12345");
			w2.flush();
		}
		MafUtils.loadPositionsOfInterest(headerFile.getAbsolutePath(), collection);
		assertEquals(1, collection.size());
		assertEquals("1", (collection.get(0)).getChromosome());
		assertEquals(12345, (collection.get(0)).getPosition());
	}
	
	@Test
	public void testWriteMafOutputEmptyList() throws Exception {
		File mafOutputFile = testFolder.newFile("mafs");
		List<MAFRecord> mafs = new ArrayList<MAFRecord>();
		List<ChrPosition> fromFile = new ArrayList<ChrPosition>();
		
		MafUtils.writeMafOutput(mafOutputFile.getAbsolutePath(), mafs, MafUtils.CORE_HEADER);
		MafUtils.loadPositionsOfInterest(mafOutputFile.getAbsolutePath(), fromFile);
		
		assertEquals(0, fromFile.size());
	}
	@Test
	public void testWriteMafOutput() throws Exception {
		File mafOutputFile = testFolder.newFile("mafs");
		List<MAFRecord> mafs = new ArrayList<MAFRecord>();
		List<ChrPosition> fromFile = new ArrayList<ChrPosition>();
		
		// add some records to maf collection
		mafs.add(new MAFRecord());
		mafs.add(new MAFRecord());
		mafs.add(new MAFRecord());
		
		MafUtils.writeMafOutput(mafOutputFile.getAbsolutePath(), mafs, MafUtils.CORE_HEADER);
		MafUtils.loadPositionsOfInterest(mafOutputFile.getAbsolutePath(), fromFile);
		
		assertEquals(3, fromFile.size());
	}
	@Test
	public void testWriteMafOutputExtraFields() throws Exception {
		File mafOutputFile = testFolder.newFile("mafsExtra");
		List<MAFRecord> mafs = new ArrayList<MAFRecord>();
		List<ChrPosition> fromFile = new ArrayList<ChrPosition>();
		
		// add some records to maf collection
		mafs.add(new MAFRecord());
		mafs.add(new MAFRecord());
		mafs.add(new MAFRecord());
		
		MafUtils.writeMafOutput(mafOutputFile.getAbsolutePath(), mafs, MafUtils.HEADER_WITH_CONFIDENCE_CPG, true);
		MafUtils.loadPositionsOfInterest(mafOutputFile.getAbsolutePath(), fromFile);
		
		assertEquals(3, fromFile.size());
	}
	
	@Test
	public void testLoadEntrezMapping() throws Exception {
		File entrezFile = testFolder.newFile("entrez");
		try (FileWriter writer = new FileWriter(entrezFile)) {
		
			// entez file can contain multiple mappings for each key
			// could potentially have a null mapping for a key???
			writer.write("header\n");
			writer.write("\tOne\t\t\t\t1\n");
			writer.write("\tOne\t\t\t\t11\n");
			writer.write("\tOne\t\t\t\t111\n");
			writer.write("\tOne\t\t\t\t1111\n");
			writer.write("\tTwo\t\t\t\t2\n");
			writer.write("\tThree\t\t\t\t3\n");
			writer.write("\tThree\t\t\t\t33\n");
	//		writer.write("\tFour\t\n");
			writer.flush();
		}
		
		Map<String, Set<Integer>> results = new HashMap<String, Set<Integer>>();
		MafUtils.loadEntrezMapping(entrezFile.getAbsolutePath(), results);
		
		assertEquals(3, results.size());
		assertEquals(4, results.get("One").size());
		assertEquals(1, results.get("Two").size());
		assertEquals(2, results.get("Three").size());
//		assertEquals(0, results.get("Four"));
	}
	
	@Test
	public void testConvertDccToMafWithNS() throws QMafException {
		List<MAFRecord> mafs = new ArrayList<MAFRecord>();
		Map<String, Set<Integer>> ensemblToEntrez = new HashMap<String, Set<Integer>>();
		TabbedRecord data = new TabbedRecord();
		String s = "COLO_829_SNP_4014186	1	19	56662400	56662400	1	-888	-888	A	A/A	A/G	A>G	-999	-999	59	2	2	-888	-999	-999	A:27[28.53],28[29.94]   A:26[27.91],20[29.55],G:4[39],9[39.36]	11	INTRONIC,INTRONIC,INTRONIC,NON_SYNONYMOUS_CODING,INTRONIC	-888,-888,-888,V200A,-888	-888,-888,-888,599A>G,-888	-888,-888,-888,-888,-888	ENSG00000167685|ENSG00000204533 ENST00000337080,ENST00000412291,ENST00000391714|ENST00000376272,ENST00000376271	61	-999	ZNF444|-888	-888,-888,-888|-888,-888	-888,-888,-888|-888,-888	-888,-888,-888|-888,-888	A/G	chr19:56662400-56662400";
		s = s.replaceAll("\\s+", "\t");
		s+= "\tmutation also found in pileup of (unfiltered) normal";
		data.setData(s);
		String [] params = TabTokenizer.tokenize(data.getData());
		assertEquals("mutation also found in pileup of (unfiltered) normal", params[37]);
		
		MafUtils.convertDccToMaf(data, "COLO_829", "control_sample_id", "tumourSampleID", null, mafs, ensemblToEntrez, true, false);
		assertEquals(1, mafs.size());
		assertEquals(11, mafs.get(0).getNovelStartCount());
	}
	
	@Test
	public void testConvertDccToMafWithoutNS() throws QMafException {
		List<MAFRecord> mafs = new ArrayList<MAFRecord>();
		Map<String, Set<Integer>> ensemblToEntrez = new HashMap<String, Set<Integer>>();
		TabbedRecord data = new TabbedRecord();
		String s = "APGI_2057_SNP_3302627   1       20      44115498        44115498        1       -888    -888    A       A/A     A/T     A>T     -999    -999    14      2       2       -888    -999    -999    A:10[40.8],7[40.29],T:7[40.16],3[35.22] A:5[38.21],3[30.25],T:3[40],3[40]       UPSTREAM,DOWNSTREAM     -888,-888       -888,-888       -888,-888       ENSG00000237464|ENSG00000237068 ENST00000417630|ENST00000429598 61      -999    -888|RPL5P2     -888|-888       -888|-888       -888|-888       A/T     chr20:44115498-44115498";
		s = s.replaceAll("\\s+", "\t");
		s+= "\tmutation also found in pileup of normal; mutation is a germline variant in another patient";
		data.setData(s);
		String [] params = TabTokenizer.tokenize(data.getData());
		assertEquals("mutation also found in pileup of normal; mutation is a germline variant in another patient", params[36]);
		
		MafUtils.convertDccToMaf(data, "APGI_2057", "controlSampleID", "tumourSampleID", null, mafs, ensemblToEntrez, false, false);
		assertEquals(1, mafs.size());
//		assertEquals(0, mafs.size());
		
		s = "APGI_2057_SNP_3260617	1	15	102211884	102211884	1	-888	-888	T	T/T	A/T	T>A	-999	-999	7	2	2	-888	-999	-999	T:8[24.83],20[39.34],A:3[21.49],0[0],G:1[16],0[0]	T:5[11.03],1[36],A:1[23],0[0]	NON_SYNONYMOUS_CODING,NON_SYNONYMOUS_CODING	Y619F,Y524F	2073T>A,1626T>A	-888,-888	ENSG00000185418	ENST00000335968,ENST00000333018	61	-999	TARSL2	TIGR00418_PS50862,PS50862_TIGR00418	tigrfam_pfscan,pfscan_tigrfam	Thr-tRNA-synth_IIa__Aminoacyl-tRNA-synth_II,Aminoacyl-tRNA-synth_II__Thr-tRNA-synth_IIa	T/A	chr15:102211884-102211884";
		s = s.replaceAll("\\s+", "\t");
		s+= "\tmutation also found in pileup of normal; mutation is a germline variant in another patient";
		data.setData(s);
		params = TabTokenizer.tokenize(data.getData());
		assertEquals("mutation also found in pileup of normal; mutation is a germline variant in another patient", params[36]);
		
		MafUtils.convertDccToMaf(data, "APGI_2057", "controlSampleID", "tumourSampleID", null, mafs, ensemblToEntrez, false, false);
		assertEquals(2, mafs.size());
//		assertEquals(1, mafs.size());
//		System.out.println("mafs.get(0).getVariantClassification: " + mafs.get(0).getVariantClassification());
//		assertEquals("", mafs.get(0).getVariantClassification());
	}
	
	@Test
	public void testRealLifeData() throws QMafException {
		List<MAFRecord> mafs = new ArrayList<MAFRecord>();
		Map<String, Set<Integer>> ensemblToEntrez = new HashMap<String, Set<Integer>>();
		TabbedRecord data = new TabbedRecord();
		String s = "AOCS_066_SNP_3124       1       1       115256530       115256530       1       G/T     -1      G       G/G     G/T     G>T     -999    -999    1.2420510993064712E-22  110     1       2       -888    rs121913254     -999    G:25[34.12],67[36.06]   G:10[33.2],31[33.35],T:16[39.62],53[38.58]      44      missense_variant,downstream_gene_variant,downstream_gene_variant,downstream_gene_variant,downstream_gene_variant,downstream_gene_variant,downstream_gene_variant,downstream_gene_variant,downstream_gene_variant        Q61K;Q61K;Q61K;Q61K;Q61K;Q61K;Q61K;Q61K;Q61K,-888,-888,-888,-888,-888,-888,-888,-888    435G>T;435G>T;435G>T;435G>T;435G>T;435G>T;435G>T;435G>T;435G>T,-888,-888,-888,-888,-888,-888,-888,-888  PF00071;PF08477;PF00025;PF00009;TIGR00231;PR00449;SM00173;SM00175;SM00174       ENSG00000213281,ENSG00000009307,ENSG00000009307,ENSG00000009307,ENSG00000009307,ENSG00000009307,ENSG00000009307,ENSG00000009307,ENSG00000009307 ENST00000369535,ENST00000339438,ENST00000438362,ENST00000358528,ENST00000261443,ENST00000530886,ENST00000369530,ENST00000483407,ENST00000534699 70      -999    NRAS,CSDE1,CSDE1,CSDE1,CSDE1,CSDE1,CSDE1,CSDE1,CSDE1    PF00071;PF08477;PF00025;PF00009;TIGR00231;PR00449;SM00173;SM00175;SM00174       pfam;pfam;pfam;pfam;tigrfam;prints;smart;smart;smart    Small_GTPase;MIRO-like;Small_GTPase_ARF/SAR;EF_GTP-bd_dom;Small_GTP-bd_dom;Small_GTPase;Small_GTPase_Ras;Small_GTPase_Rab_type;Small_GTPase_Rho chr1:115256530-115256530        PASS    TTCTTTTCCAG";
		s = s.replaceAll("\\s+", "\t");
		data.setData(s);
		
		MafUtils.convertDccToMaf(data, "AOCS_066", "controlSampleID", "tumourSampleID", null, mafs, ensemblToEntrez, true, true);
		
		assertEquals(1, mafs.size());
		MAFRecord maf = mafs.get(0);
		assertEquals("Missense_Mutation", maf.getVariantClassification());
		assertEquals("NRAS", maf.getHugoSymbol());
		maf.setMafType(MafType.SNV_SOMATIC);
		
		// name filter
		assertEquals(true, DccConsequence.passesMafNameFilter(maf.getVariantClassification()));
		
		// high conf filter
//		String variant = maf.getRef().equals(maf.getTumourAllele1()) ? maf.getTumourAllele2() : maf.getTumourAllele1();
		char alt = MafUtils.getVariant(maf).charAt(0);
		assertEquals(true, MafUtils.passesHighConfidenceFilter(maf.getFlag(), maf.getVariantType(), maf.getTd(), true , alt));
		
		// low conf filter
		assertEquals(true, MafUtils.passesLowerConfidenceFilter(maf.getFlag(), maf.getVariantType(), maf.getTd(), alt));
	}
	
	@Test
	public void testRealLifeData2() throws QMafException {
		List<MAFRecord> mafs = new ArrayList<MAFRecord>();
		Map<String, Set<Integer>> ensemblToEntrez = new HashMap<String, Set<Integer>>();
		TabbedRecord data = new TabbedRecord();
		String s = "AOCS_066_SNP_5524       1       1       196762515       196762515       1       -888    -888    A       A/A     A/C     A>C     -999    -999    1.0515508456799864E-8   99      2       2       -888    -999    -999    A:94[38.29],4[38.25]    A:71[35.69],3[40],C:24[38.54],1[37]     19      missense_variant,downstream_gene_variant,3_prime_UTR_variant,NMD_transcript_variant,missense_variant,non_coding_exon_variant,nc_transcript_variant      K289Q,-888,-888,-888,K228Q,-888,-888    957A>C,-888,1139A>C,1139A>C,753A>C,537A>C,537A>C        SSF57535,SSF57535       ENSG00000116785,ENSG00000116785,ENSG00000116785,ENSG00000116785,ENSG00000116785,ENSG00000116785,ENSG00000116785 ENST00000367425,ENST00000471440,ENST00000367427,ENST00000367427,ENST00000391985,ENST00000461558,ENST00000461558 70      -999    CFHR3,CFHR3,CFHR3,CFHR3,CFHR3,CFHR3,CFHR3       SSF57535,SSF57535       superfamily,superfamily Complement_control_module,Complement_control_module     chr1:196762515-196762515        PASS    ACAGACAATAT";
		s = s.replaceAll("\\s+", "\t");
		data.setData(s);
		
		MafUtils.convertDccToMaf(data, "AOCS_066", "controlSampleID", "tumourSampleID", null, mafs, ensemblToEntrez, true, true);
		
		assertEquals(1, mafs.size());
		MAFRecord maf = mafs.get(0);
		
		assertEquals("Missense_Mutation", maf.getVariantClassification());
		assertEquals("CFHR3", maf.getHugoSymbol());
		
		// name filter
		assertEquals(true, DccConsequence.passesMafNameFilter(maf.getVariantClassification()));
		
		maf.setMafType(MafType.SNV_SOMATIC);
		
		// high conf filter
//		String variant = maf.getRef().equals(maf.getTumourAllele1()) ? maf.getTumourAllele2() : maf.getTumourAllele1();
		char alt = MafUtils.getVariant(maf).charAt(0);
		assertEquals(true, MafUtils.passesHighConfidenceFilter(maf.getFlag(), maf.getVariantType(), maf.getTd(), true , alt));
		
		// low conf filter
		assertEquals(true, MafUtils.passesLowerConfidenceFilter(maf.getFlag(), maf.getVariantType(), maf.getTd(), alt));
	}
	
//	@Ignore
//	public void testRealLifeData2() throws QMafException {
//		List<MAFRecord> mafs = new ArrayList<MAFRecord>();
//		Map<String, Set<Integer>> ensemblToEntrez = new HashMap<String, Set<Integer>>();
//		TabbedRecord data = new TabbedRecord();
//		String s = "APGI_2179_SNP_3228926	1	12	25398284	25398284	1	A/C	-1	C	C/CA/C	C>A	-999	-999	42	1	2	-888	rs121913529	-999	C:21[37.62],21[37.67]	A:14[36.57],18[34.17],C:5[36.2],5[32.2]	28	NON_SYNONYMOUS_CODING,NON_SYNONYMOUS_CODING,NON_SYNONYMOUS_CODING,NON_SYNONYMOUS_CODINGG12V;G12V;G12V;G12V;G12V;G12V;G12V;G12V,G12V;G12V;G12V,G12V;G12V;G12V;G12V;G12V;G12V;G12V;G12V;G12V,G12V;G12V	227C>A;227C>A;227C>A;227C>A;227C>A;227C>A;227C>A;227C>A,232C>A;232C>A;232C>A,99C>A;99C>A;99C>A;99C>A;99C>A;99C>A;99C>A;99C>A;99C>A,212C>A;212C>APF00071;PF08477;PF00025;TIGR00231;PR00449;SM00173;SM00175;SM00174,PF00071;PR00449;SM00173,PF00071;PF08477;PF00025;PR00449;TIGR00231;SM00173;SM00175;SM00174;SM00176,PF00071;PR00449	ENSG00000133703,ENSG00000133703,ENSG00000133703,ENSG00000133703	ENST00000311936,ENST00000557334,ENST00000256078,ENST00000556131	66	-999	KRAS,KRAS,KRAS,KRAS	PF00071;PF08477;PF00025;TIGR00231;PR00449;SM00173;SM00175;SM00174,PF00071;PR00449;SM00173,PF00071;PF08477;PF00025;PR00449;TIGR00231;SM00173;SM00175;SM00174;SM00176,PF00071;PR00449	pfam;pfam;pfam;tigrfam;prints;smart;smart;smart,pfam;prints;smart,pfam;pfam;pfam;prints;tigrfam;smart;smart;smart;smart,pfam;prints	Small_GTPase;MIRO-like;Small_GTPase_ARF/SAR;Small_GTP-bd_dom;Small_GTPase;Small_GTPase_Ras;Small_GTPase_Rab_type;Small_GTPase_Rho,Small_GTPase;Small_GTPase;Small_GTPase_Ras,Small_GTPase;MIRO-like;Small_GTPase_ARF/SAR;Small_GTPase;Small_GTP-bd_dom;Small_GTPase_Ras;Small_GTPase_Rab_type;Small_GTPase_Rho;Ran_GTPase,Small_GTPase;Small_GTPase	chr12:25398284-25398284	PASS;GERM	CGCCAACAGCT";
////		s = s.replaceAll("\\s+", "\t");
//		data.setData(s);
//		
//		MafUtils.convertDccToMaf(data, "APGI_2179", "controlSampleID", "tumourSampleID", null, mafs, ensemblToEntrez, false, false);
//		
//		assertEquals(1, mafs.size());
//		MAFRecord maf = mafs.get(0);
//		
//		// name filter
//		assertEquals(true, DccConsequence.passesMafNameFilter(maf.getVariantClassification()));
//		
//		// high conf filter
////		String variant = maf.getRef().equals(maf.getTumourAllele1()) ? maf.getTumourAllele2() : maf.getTumourAllele1();
//		char alt = MafUtils.getVariant(maf);
//		assertEquals(true, MafUtils.passesHighConfidenceFilter(maf.getFlag(), maf.getVariantType(), maf.getTd(), true , alt));
//		
//		// low conf filter
//		assertEquals(true, MafUtils.passesLowerConfidenceFilter(maf.getFlag(), maf.getVariantType(), maf.getTd(), alt));
//	}
	
	@Test
	public void testRealLifeData3() throws QMafException {
		List<MAFRecord> mafs = new ArrayList<MAFRecord>();
		Map<String, Set<Integer>> ensemblToEntrez = new HashMap<String, Set<Integer>>();
		TabbedRecord data = new TabbedRecord();
		String s = "AOCS_067_SNP_20521      1       5       94784186        94784186        1       -888    -888    A       A/A     A/C     A>C     -999    -999    0.12844960008474762     19      2       2       -888    -999    -999    A:1[18],11[28]  A:0[0],14[27.5],C:0[0],5[26]    4       intron_variant,intron_variant,NMD_transcript_variant,downstream_gene_variant,intron_variant,intron_variant,NMD_transcript_variant       -888,-888,-888,-888,-888,-888,-888      -888,-888,-888,-888,-888,-888,-888      -888    ENSG00000153347,ENSG00000153347,ENSG00000153347,ENSG00000153347,ENSG00000153347,ENSG00000153347,ENSG00000153347 ENST00000283357,ENST00000507832,ENST00000507832,ENST00000503361,ENST00000512365,ENST00000513110,ENST00000513110 70      -999    FAM81B,FAM81B,FAM81B,FAM81B,FAM81B,FAM81B,FAM81B        --      --      --      chr5:94784186-94784186  PASS    CTTTTCTTAAG";
		s = s.replaceAll("\\s+", "\t");
		data.setData(s);
		
		MafUtils.convertDccToMaf(data, "AOCS_067", "controlSampleID", "tumourSampleID", null, mafs, ensemblToEntrez, true, true);
		
		assertEquals(1, mafs.size());
		MAFRecord maf = mafs.get(0);
		assertEquals("Intron", maf.getVariantClassification());
		assertEquals("FAM81B", maf.getHugoSymbol());
		
		// name filter
		assertEquals(false, DccConsequence.passesMafNameFilter(maf.getVariantClassification()));
		
		maf.setMafType(MafType.SNV_SOMATIC);
		
		// high conf filter
//		String variant = maf.getRef().equals(maf.getTumourAllele1()) ? maf.getTumourAllele2() : maf.getTumourAllele1();
		char alt = MafUtils.getVariant(maf).charAt(0);
		assertEquals(true, MafUtils.passesHighConfidenceFilter(maf.getFlag(), maf.getVariantType(), maf.getTd(), true , alt));
		
		// low conf filter
		assertEquals(true, MafUtils.passesLowerConfidenceFilter(maf.getFlag(), maf.getVariantType(), maf.getTd(), alt));
		
		assertEquals("FAM81B", maf.getHugoSymbol());
		
		// and now another one that should also be Unknown
		mafs.clear();
		
		s = "AOCS_067_SNP_38220      1       11      5373331 5373331 1       -888    -888    T       T/T     G/T     T>G     -999    -999    0.001102600781824035    32      2       2       -888    -999    -999    T:29[36.79],58[35.22]   G:0[0],5[28.6],T:8[40],19[28.95]        4       intron_variant,intron_variant,intron_variant,intron_variant,intron_variant,nc_transcript_variant,intron_variant,nc_transcript_variant,intron_variant,nc_transcript_variant,intron_variant,nc_transcript_variant,synonymous_variant,upstream_gene_variant        -888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,V198V;V198V;V198V,-888      -888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,594T>G;594T>G;594T>G,-888   PF00001;PF10320;PS50262 ENSG00000196565,ENSG00000196565,ENSG00000213931,ENSG00000213931,,,,,,,,,ENSG00000176239,        ENST00000380252,ENST00000380259,ENST00000380237,ENST00000396895,ENST00000420465,ENST00000420465,ENST00000415970,ENST00000415970,ENST00000420726,ENST00000420726,ENST00000418729,ENST00000418729,ENST00000380219,ENST00000450768 70      -999    HBG2,HBG2,HBE1,HBE1,,,,,,,,,OR51B6,     PF00001;PF10320;PS50262 pfam;pfam;pfscan        GPCR_Rhodpsn;7TM_GPCR_olfarory/Srsx;GPCR_Rhodpsn_7TM    chr11:5373331-5373331   PASS    CCAGTGGTAGT";
		s = s.replaceAll("\\s+", "\t");
		data.setData(s);
		MafUtils.convertDccToMaf(data, "AOCS_067", "controlSampleID", "tumourSampleID", null, mafs, ensemblToEntrez, true, true);
		maf = mafs.get(0);
		assertEquals("Silent", maf.getVariantClassification());
		assertEquals("OR51B6", maf.getHugoSymbol());
		// name filter
		assertEquals(true, DccConsequence.passesMafNameFilter(maf.getVariantClassification()));
		
		maf.setMafType(MafType.SNV_SOMATIC);
		// high conf filter
//		variant = maf.getRef().equals(maf.getTumourAllele1()) ? maf.getTumourAllele2() : maf.getTumourAllele1();
		alt = MafUtils.getVariant(maf).charAt(0);
		assertEquals(true, MafUtils.passesHighConfidenceFilter(maf.getFlag(), maf.getVariantType(), maf.getTd(), true , alt));
		
		// low conf filter
		assertEquals(true, MafUtils.passesLowerConfidenceFilter(maf.getFlag(), maf.getVariantType(), maf.getTd(), alt));
		
		assertEquals("OR51B6", maf.getHugoSymbol());
	}
	
	@Ignore
	public void testRealLifeData4() throws QMafException {
		List<MAFRecord> mafs = new ArrayList<MAFRecord>();
		Map<String, Set<Integer>> ensemblToEntrez = new HashMap<String, Set<Integer>>();
		TabbedRecord data = new TabbedRecord();
		String s = "APGI_1594_SNP_2943597	1	11	62373211	62373211	1	-888	-888	T	T/T	G/T	T>G	-999	-999	0.060573922747081455	40	2	2	-888	-999	-999	G:1[6],0[0],T:18[22.72],16[37.69]	G:5[7.6],2[21.5],T:11[21.45],22[35.55]	7	upstream_gene_variant,missense,3_prime_UTR_variant,non_coding_exon_variant,missense,non_coding_exon_variant,missense,missense,missense,non_coding_exon_variant,missense,upstream_gene_variant,upstream_gene_variant,upstream_gene_variant,downstream_gene_variant,downstream_gene_variant,upstream_gene_variant,upstream_gene_variant,upstream_gene_variant	-888,H/D,-888,-888,H/D,-888,H/D,H/D,H/D,-888,H/D,-888,-888,-888,-888,-888,-888,-888,-888	-888,Cac/Gac,-888,-888,Cac/Gac,-888,Cac/Gac,Cac/Gac,Cac/Gac,-888,Cac/Gac,-888,-888,-888,-888,-888,-888,-888,-888	-888	ENSG00000149480,ENSG00000149499,ENSG00000149499,ENSG00000149499,ENSG00000149499,ENSG00000149499,ENSG00000149499,ENSG00000149499,ENSG00000149499,ENSG00000149499,ENSG00000149499,ENSG00000149499,ENSG00000149499,ENSG00000149499,ENSG00000149499,ENSG00000149499,ENSG00000254964,ENSG00000149480,ENSG00000149480	ENST00000278823,ENST00000278845,ENST00000494448,ENST00000483199,ENST00000529309,ENST00000526116,ENST00000394776,ENST00000531557,ENST00000494176,ENST00000460939,ENST00000394773,ENST00000439994,ENST00000533165,ENST00000524518,ENST00000462626,ENST00000438258,ENST00000532626,ENST00000527204,ENST00000526844	70	-999	MTA2,EML3,EML3,EML3,EML3,EML3,EML3,EML3,EML3,EML3,EML3,EML3,EML3,EML3,EML3,EML3,RP11-831H9.3,MTA2,MTA2	--	--	--	chr11:62373211-62373211	MIN	AGGGGGGTGTG";
		s = s.replaceAll("\\s+", "\t");
		data.setData(s);
		
		MafUtils.convertDccToMaf(data, "APGI_1594", "controlSampleID", "tumourSampleID", null, mafs, ensemblToEntrez, true, true);
		
		assertEquals(1, mafs.size());
		MAFRecord maf = mafs.get(0);
		assertEquals("Missense_Mutation", maf.getVariantClassification());
		assertEquals("EML3", maf.getHugoSymbol());
	}
	
	@Test
	public void testRealLifeData5() throws QMafException {
		List<MAFRecord> mafs = new ArrayList<MAFRecord>();
		Map<String, Set<Integer>> ensemblToEntrez = new HashMap<String, Set<Integer>>();
		TabbedRecord data = new TabbedRecord();
		String s = "75edf18c_801c_48ae_8acf_44c7c87de319_SNP_5212932.3134   4       1       12907507        12907508        1       /       -888    CT      CT/CT   CC/TT   CT>TC   -999    -999    -999    113     2       2       -888    -999    -999    ALL:1+12-;REF:1+12-;ALT:0+0+0-0-;       ALL:11+12-;REF:11+12-;ALT:0+0+0-0-;     27      missense_variant        E212G   861CT>TC        PIRSF037992     ENSG00000179172 ENST00000317869 70      -999    HNRNPCL1        PIRSF037992     pirsf   hnRNP_C_Raly    chr1:12907507-12907508  PASS    --";
		s = s.replaceAll("\\s+", "\t");
		data.setData(s);
		
		MafUtils.convertDccToMaf(data, "APGI_1594", "controlSampleID", "tumourSampleID", null, mafs, ensemblToEntrez, true, true);
		
		assertEquals(1, mafs.size());
		MAFRecord maf = mafs.get(0);
		assertEquals("Missense_Mutation", maf.getVariantClassification());
		assertEquals("HNRNPCL1", maf.getHugoSymbol());
		
		MafFilterUtils.classifyMAFRecord(maf);
		//assertEquals(MafConfidence.ZERO, maf.getConfidence());
	}
	
	@Ignore
	public void testRealLifeDataGermline() throws QMafException {
		List<MAFRecord> mafs = new ArrayList<MAFRecord>();
		Map<String, Set<Integer>> ensemblToEntrez = new HashMap<String, Set<Integer>>();
		TabbedRecord data = new TabbedRecord();
		String s = "OESO_1020_SNP_3 1       1       10109   10109   1       -888    -888    A       A/T     A/T     -999    -999    -999    170     2       2       -888    -999    -999    A:90[14.17],41[34.61],C:1[6],0[0],G:1[2],0[0],T:19[21.42],18[31.39]     A:72[14.14],46[32.37],C:5[2],0[0],T:11[23.09],19[28.47] 30      UPSTREAM,UPSTREAM,UPSTREAM,UPSTREAM,DOWNSTREAM,DOWNSTREAM,DOWNSTREAM,DOWNSTREAM,DOWNSTREAM      -888,-888,-888,-888,-888,-888,-888,-888,-888    -888,-888,-888,-888,-888,-888,-888,-888,-888    -888    ENSG00000223972,ENSG00000223972,ENSG00000223972,ENSG00000223972,ENSG00000227232,ENSG00000227232,ENSG00000227232,ENSG00000227232,ENSG00000227232 ENST00000456328,ENST00000515242,ENST00000518655,ENST00000450305,ENST00000438504,ENST00000541675,ENST00000423562,ENST00000488147,ENST00000538476 66      -999    DDX11L1,DDX11L1,DDX11L1,DDX11L1,WASH7P,WASH7P,WASH7P,WASH7P,WASH7P      --      --      --      chr1:10109-10109        PASS    AACCCTACCCT     A>T";
		s = s.replaceAll("\\s+", "\t");
		data.setData(s);
	
		MafUtils.convertGermlineDccToMaf(data, "OESO_1020", "controlSampleID", "tumourSampleID", null, mafs, ensemblToEntrez);
		
		assertEquals(1, mafs.size());
		MAFRecord maf = mafs.get(0);
		assertEquals("1", maf.getChromosome());
		assertEquals(10109, maf.getStartPosition());
		assertEquals(10109, maf.getEndPosition());
		assertEquals('+', maf.getStrand());
		assertEquals("A", maf.getNormalAllele1());
		assertEquals("T", maf.getNormalAllele2());
		assertEquals("A", maf.getTumourAllele1());
		assertEquals("T", maf.getTumourAllele2());
		assertEquals("A:90[14.17],41[34.61],C:1[6],0[0],G:1[2],0[0],T:19[21.42],18[31.39]", maf.getNd());
		assertEquals("A:72[14.14],46[32.37],C:5[2],0[0],T:11[23.09],19[28.47]", maf.getTd());
		assertEquals(30, maf.getNovelStartCount());
		assertEquals("AACCCTACCCT", maf.getCpg());
		assertEquals("PASS", maf.getFlag());
		assertEquals("5'Flank", maf.getVariantClassification());
		assertEquals("A", maf.getRef());
		assertEquals("ENST00000456328", maf.getCanonicalTranscriptId());
		assertEquals("DDX11L1", maf.getHugoSymbol());
//		assertEquals("A", maf.getEntrezGeneId());
		
		// name filter
		assertEquals(false, DccConsequence.passesMafNameFilter(maf.getVariantClassification()));
	}
	
	@Test
	public void testRealLifeDataGermline2() throws QMafException {
		List<MAFRecord> mafs = new ArrayList<MAFRecord>();
		Map<String, Set<Integer>> ensemblToEntrez = new HashMap<String, Set<Integer>>();
		TabbedRecord data = new TabbedRecord();
		String s = "0b09a9d1_7d96_4159_881a_a4aaf52ac3e9_SNP_2      1       1       10109   10109   1       -888    -888    A       A/T     A/T     -999    -999    -999    168     2       2       -888    -999    -999    A:88[14.47],29[36.59],C:4[4],0[0],T:16[15.62],31[32.58] A:142[13.49],52[35.42],C:1[2],0[0],T:39[23.08],43[32.47]        30      upstream_gene_variant,upstream_gene_variant,upstream_gene_variant,upstream_gene_variant,downstream_gene_variant,downstream_gene_variant,downstream_gene_variant,downstream_gene_variant,downstream_gene_variant -888    -888    -888    ENSG00000223972,ENSG00000223972,ENSG00000223972,ENSG00000223972,ENSG00000227232,ENSG00000227232,ENSG00000227232,ENSG00000227232,ENSG00000227232 ENST00000456328,ENST00000515242,ENST00000518655,ENST00000450305,ENST00000438504,ENST00000541675,ENST00000423562,ENST00000488147,ENST00000538476 70      -999    DDX11L1,DDX11L1,DDX11L1,DDX11L1,WASH7P,WASH7P,WASH7P,WASH7P,WASH7P      --      --      --      chr1:10109-10109        PASS    AACCCTACCCT     A>T";
		s = s.replaceAll("\\s+", "\t");
		data.setData(s);
		
		MafUtils.convertGermlineDccToMaf(data, "APGI_2027", "controlSampleID", "tumourSampleID", null, mafs, ensemblToEntrez);
		
		assertEquals(1, mafs.size());
		MAFRecord maf = mafs.get(0);
		assertEquals("1", maf.getChromosome());
		assertEquals(10109, maf.getStartPosition());
		assertEquals(10109, maf.getEndPosition());
		assertEquals('+', maf.getStrand());
		assertEquals("A", maf.getNormalAllele1());
		assertEquals("T", maf.getNormalAllele2());
		assertEquals("A", maf.getTumourAllele1());
		assertEquals("T", maf.getTumourAllele2());
		assertEquals("A:88[14.47],29[36.59],C:4[4],0[0],T:16[15.62],31[32.58]", maf.getNd());
		assertEquals("A:142[13.49],52[35.42],C:1[2],0[0],T:39[23.08],43[32.47]", maf.getTd());
		assertEquals(30, maf.getNovelStartCount());
		assertEquals("AACCCTACCCT", maf.getCpg());
		assertEquals("PASS", maf.getFlag());
		assertEquals("5'Flank", maf.getVariantClassification());
		assertEquals("A", maf.getRef());
		assertEquals("ENST00000456328", maf.getCanonicalTranscriptId());
		assertEquals("DDX11L1", maf.getHugoSymbol());
//		assertEquals("A", maf.getEntrezGeneId());
		
		// name filter
		assertEquals(false, DccConsequence.passesMafNameFilter(maf.getVariantClassification()));
	}
	
	@Test
	public void testGetGenePositionsNull() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Null gene array passed to getGenePositions");
		MafUtils.getGenePositions(null);
	}
	
	@Test
	public void testGetGenePositions() {
		assertEquals(0, MafUtils.getGenePositions(new String[] {}).size());
		assertEquals(1, MafUtils.getGenePositions(new String[] {"gene1"}).size());
		assertEquals("gene1", MafUtils.getGenePositions(new String[] {"gene1"}).get(0).getGene());
		assertEquals(0, MafUtils.getGenePositions(new String[] {"gene1"}).get(0).getPositions()[0].intValue());
		assertEquals(1, MafUtils.getGenePositions(new String[] {"gene1","gene1"}).get(0).getPositions()[1].intValue());
		assertEquals(2, MafUtils.getGenePositions(new String[] {"gene1","gene1","gene2","gene2","gene2"}).size());
		assertEquals(2, MafUtils.getGenePositions(new String[] {"gene1","gene1","gene2","gene2","gene2"}).get(1).getPositions()[0].intValue());
		assertEquals(3, MafUtils.getGenePositions(new String[] {"gene1","gene1","gene2","gene2","gene2"}).get(1).getPositions()[1].intValue());
		assertEquals(4, MafUtils.getGenePositions(new String[] {"gene1","gene1","gene2","gene2","gene2"}).get(1).getPositions()[2].intValue());
		
	}
	
	@Ignore
	public void testWorstCaseConsequences() {
		Map<String, Set<Integer>> ensemblToEntrez = new HashMap<String, Set<Integer>>();
		String[] params = setupBogStandardParams();
		params[22] = "NON_SYNONYMOUS_CODING,DOWNSTREAM";	// consequences
		params[26] = "geneid1,geneid2";	// gene ids
		params[27] = "trans1id,trans2id";	// transcript
		params[30] = "gene1,gene2";	// genes
		
		MAFRecord maf = new MAFRecord();
		
		MafUtils.worstCaseConsequence(MutationType.SNP, params, maf, ensemblToEntrez, 0);
		assertEquals("gene1", maf.getHugoSymbol());
		assertEquals("trans1id", maf.getCanonicalTranscriptId());
		
		params[22] = "DOWNSTREAM,NON_SYNONYMOUS_CODING";	// consequences
		maf = new MAFRecord();
		MafUtils.worstCaseConsequence(MutationType.SNP, params, maf, ensemblToEntrez, 0);
		assertEquals("gene2", maf.getHugoSymbol());
		assertEquals("trans2id", maf.getCanonicalTranscriptId());
		
		params[22] = "DOWNSTREAM,UPSTREAM,NON_SYNONYMOUS_CODING";	// consequences
		maf = new MAFRecord();
		MafUtils.worstCaseConsequence(MutationType.SNP, params, maf, ensemblToEntrez, 0);
		assertEquals("Unknown", maf.getHugoSymbol());
		assertEquals(null, maf.getCanonicalTranscriptId());
		
		params[22] = "";	// consequences
		maf = new MAFRecord();
		MafUtils.worstCaseConsequence(MutationType.SNP, params, maf, ensemblToEntrez, 0);
		assertEquals("Unknown", maf.getHugoSymbol());
		assertEquals(null, maf.getCanonicalTranscriptId());
		
		params[22] = "INTRONIC,INTRONIC,INTRONIC,INTRONIC,WITHIN_NON_CODING_GENE";	// consequences
		params[30] = "DVL1,DVL1,DVL1,DVL1,DVL1";
		maf = new MAFRecord();
		MafUtils.worstCaseConsequence(MutationType.SNP, params, maf, ensemblToEntrez, 0);
		assertEquals("DVL1", maf.getHugoSymbol());
		assertEquals("RNA", maf.getVariantClassification());
		assertEquals(null, maf.getCanonicalTranscriptId());
	}
	
	
	private String[] setupBogStandardParams() {
		String [] params = new String[40];
		Arrays.fill(params, "test");
		params[0] = "id_123";
		params[1] = "1";
		params[2] = "2";
		params[3] = "1000";
		params[4] = "1000";
		params[5] = "1";
		params[6] = "-888";
		params[7] = "-888";
		params[8] = "A";
		params[9] = "A/A";
		params[10] = "A/T";
		params[11] = "A>T";
		params[12] = "A>T";
		//...
		// the following all assumes that the offset is zero
		params[22] = "";	// consequences
		params[23] = "";	// aa changes
		params[24] = "";	// base changes
		params[26] = "";	// gene ids
		params[27] = "";	// transcript ids
		params[30] = "";	// genes
		
		return params;
	}
	
}
