package org.qcmg.maf.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.common.dcc.MutationType;
import org.qcmg.common.model.MafConfidence;
import org.qcmg.common.model.MafType;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.maf.MAFRecord;
import org.qcmg.maf.MafPipelineNew;
import org.qcmg.maf.QMafException;
import org.qcmg.tab.TabbedRecord;

public class MafFilterUtilsTest {
	
	private FilterOptions fo;
	
	@Before
	public void setuop() {
		fo = new FilterOptions();
		fo.setHomopolymerCutoff(MafPipelineNew.HOMOPOLYMER_CUTOFF);
	}
	
	@Test
	public void testCheckMAFForMINSnv() {
		try {
			MafFilterUtils.checkMAFForMIN(null);
			Assert.fail("Should have thrown an IAE");
		} catch(IllegalArgumentException iae) {}
		
		MAFRecord maf = new MAFRecord();
		maf.setMafType(MafType.SNV_SOMATIC);
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(null, maf.getFlag());
		
		maf.setFlag(SnpUtils.MUTATION_IN_NORMAL);
		try {
			MafFilterUtils.checkMAFForMIN(maf);
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		
		maf.setFlag(SnpUtils.PASS);
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.PASS, maf.getFlag());
		
		maf.setFlag(SnpUtils.MUTANT_READS);
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.MUTANT_READS, maf.getFlag());
		
		maf.setFlag(SnpUtils.MUTATION_IN_UNFILTERED_NORMAL);
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.MUTATION_IN_UNFILTERED_NORMAL, maf.getFlag());
		
		maf.setFlag(SnpUtils.MUTATION_IN_NORMAL + ";" + SnpUtils.LESS_THAN_8_READS_NORMAL);
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL + ";" + SnpUtils.LESS_THAN_8_READS_NORMAL, maf.getFlag());
	}
	
	@Ignore
	public void testCheckMAFForMINIndel() {
		
		MAFRecord maf = new MAFRecord();
		maf.setMafType(MafType.INDEL_SOMATIC);
		try {
			MafFilterUtils.checkMAFForMIN(maf);
			Assert.fail("Shoud have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		
		maf.setFlag(SnpUtils.PASS + ";" + SnpUtils.MUTATION_IN_NORMAL);
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.PASS + ";" + SnpUtils.MUTATION_IN_NORMAL, maf.getFlag());
		
		maf.setFlag(SnpUtils.PASS);
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.PASS, maf.getFlag());
		
		maf.setFlag(SnpUtils.MUTANT_READS);
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.MUTANT_READS, maf.getFlag());
		
		maf.setFlag(SnpUtils.MUTATION_IN_UNFILTERED_NORMAL);
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.MUTATION_IN_UNFILTERED_NORMAL, maf.getFlag());
		
		maf.setFlag(SnpUtils.MUTATION_IN_NORMAL + ";" + SnpUtils.LESS_THAN_8_READS_NORMAL);
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL + ";" + SnpUtils.LESS_THAN_8_READS_NORMAL, maf.getFlag());
	}
	
	@Ignore
	public void testCheckMAFForMINIndelWithNumbers() {
		
		MAFRecord maf = new MAFRecord();
		maf.setNd("15;150;76;71;1;3;0");
		maf.setFlag(SnpUtils.PASS + ";" + SnpUtils.MUTATION_IN_NORMAL);
		maf.setMafType(MafType.INDEL_SOMATIC);
		
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.PASS + ";" + SnpUtils.MUTATION_IN_NORMAL, maf.getFlag());
		
		maf.setNd("15;150;76;1;1;3;0");		// 1 from 150 - less than 3% - we are now a PASS
		maf.setFlag(SnpUtils.PASS + ";" + SnpUtils.MUTATION_IN_NORMAL);
		
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.PASS, maf.getFlag());
		
		
		maf.setNd("15;30;76;1;1;3;0");		// 1 from 30 - more than 3% -  flag stays the same
		maf.setFlag(SnpUtils.PASS + ";" + SnpUtils.MUTATION_IN_NORMAL);
		
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.PASS + ";" + SnpUtils.MUTATION_IN_NORMAL, maf.getFlag());
		
		maf.setNd("15;33;76;1;1;3;0");		// 1 from 33 - more than 3% -  flag stays the same
		maf.setFlag(SnpUtils.PASS + ";" + SnpUtils.MUTATION_IN_NORMAL);
		
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.PASS + ";" + SnpUtils.MUTATION_IN_NORMAL, maf.getFlag());
		
		maf.setNd("15;34;76;1;1;3;0");		// 1 from 34 - less than 3% -  PASS
		maf.setFlag(SnpUtils.PASS + ";" + SnpUtils.MUTATION_IN_NORMAL);
		
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.PASS, maf.getFlag());
		
		maf.setNd("15;134;76;1;1;3;0");		// 1 from 134 - less than 3% -  PASS
		maf.setFlag(SnpUtils.PASS + ";" + SnpUtils.MUTATION_IN_NORMAL);
		
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.PASS, maf.getFlag());
	}
	
	@Test
	public void testCheckMAFForMINIndelRealLifeData() throws QMafException {
		List<MAFRecord> mafs = new ArrayList<MAFRecord>();
		Map<String, Set<Integer>> ensemblToEntrez = new HashMap<String, Set<Integer>>();
		TabbedRecord data = new TabbedRecord();
		String s = "7PrimaryTumour_ind63058	2	chr19	53651845	53651846	1	-999	-999	T	-999	TTC	T>TTC		-999	-999	-999	-999	-999	-99	-999	-999	15;150;76;71;1;3;0	11;200;56;51;2;0;0;	--	intron_variant,feature_truncation,intron_variant,feature_truncation,intron_variant,feature_truncation,intron_variant,feature_truncation,intron_variant,feature_truncation,intron_variant,feature_truncation,downstream_gene_variant,downstream_gene_variant	-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888	-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888	-88	ENSG00000197937,ENSG00000197937,ENSG00000197937,ENSG00000197937,ENSG00000197937,ENSG00000197937,ENSG00000197937,ENSG00000197937,ENSG00000197937,ENSG00000197937,ENSG00000197937,ENSG00000197937,ENSG00000197937,ENSG00000197937	ENST00000601804,ENST00000601804,ENST00000334197,ENST00000334197,ENST00000452676,ENST00000452676,ENST00000597183,ENST00000597183,ENST00000601469,ENST00000601469,ENST00000595967,ENST00000595967,ENST00000595710,ENST00000599096	70	-999	ZNF347,ZNF347,ZNF347,ZNF347,ZNF347,ZNF347,ZNF347,ZNF347,ZNF347,ZNF347,ZNF347,ZNF347,ZNF347,ZNF347	--	--	--	chr19:53651845-53651846	PASS;MIN	--";
//		s = s.replaceAll("\\s+", "\t");
		data.setData(s);
		
		MafUtils.convertDccToMaf(data, "7PrimaryTumour_ind63058", "controlSampleID", "tumourSampleID", null, mafs, ensemblToEntrez, true, true);
		
		assertEquals(1, mafs.size());
		MAFRecord maf = mafs.get(0);
		maf.setMafType(MafType.INDEL_SOMATIC);
		
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.PASS + ";" + SnpUtils.MUTATION_IN_NORMAL, maf.getFlag());
		
	}
	
	@Test
	public void testCheckMAFForMIN2() {
		
		MAFRecord maf = new MAFRecord();
		maf.setFlag(SnpUtils.MUTATION_IN_NORMAL);
		maf.setRef("G");
		maf.setNd("A:1[30],0[0],G:9[30],0[0]");
		maf.setTumourAllele1("A");
		maf.setTumourAllele2("G");
		maf.setMafType(MafType.SNV_SOMATIC);
		
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL, maf.getFlag());
		
		maf.setNd("A:2[30],0[0],G:8[30],0[0]");
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL, maf.getFlag());
		
		maf.setNd("A:3[30],0[0],G:7[30],0[0]");
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL, maf.getFlag());
		
		maf.setNd("A:1[30],0[0],G:20[30],0[0]");
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL, maf.getFlag());
		
		maf.setNd("A:1[30],0[0],G:30[30],0[0]");
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL, maf.getFlag());
		
		maf.setNd("A:1[30],0[0],G:32[30],0[0]");
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.MUTATION_IN_NORMAL, maf.getFlag());
		
		maf.setNd("A:1[30],0[0],G:33[30],0[0]");
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.PASS, maf.getFlag());
		
		maf.setFlag(SnpUtils.MUTATION_IN_NORMAL);
		maf.setNd("A:1[30],0[0],G:34[30],0[0]");
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.PASS, maf.getFlag());
		
		maf.setFlag(SnpUtils.MUTATION_IN_NORMAL);
		maf.setNd("A:1[30],0[0],G:100[30],0[0]");
		MafFilterUtils.checkMAFForMIN(maf);
		assertEquals(SnpUtils.PASS, maf.getFlag());
	}
	
	@Test
	public void testClassifyMafRecord() {
		try {
			MafFilterUtils.classifyMAFRecord(null);
			Assert.fail("Should not have reached this point");
		} catch (IllegalArgumentException iae) {}
		
		MAFRecord maf = new MAFRecord();
		try {
			MafFilterUtils.classifyMAFRecord(maf);
			Assert.fail("Should not have reached this point");
		} catch (IllegalArgumentException iae) {}
		
		maf.setVariantType(MutationType.SNP);
		MafFilterUtils.classifyMAFRecord(maf);
		assertEquals(MafConfidence.ZERO, maf.getConfidence());
	}
	
	@Test
	public void testClassifyMafRecordSnp() {
		MAFRecord maf = new MAFRecord();
		maf.setVariantType(MutationType.SNP);
		maf.setMafType(MafType.SNV_SOMATIC);
		MafFilterUtils.classifyMAFRecord(maf);
		assertEquals(MafConfidence.ZERO, maf.getConfidence());
		
		// set novel start
		maf.setNovelStartCount(5);
		MafFilterUtils.classifyMAFRecord(maf);
		assertEquals(MafConfidence.ZERO, maf.getConfidence());
		
		// now add flag
		maf.setFlag(SnpUtils.PASS);
		MafFilterUtils.classifyMAFRecord(maf);
		assertEquals(MafConfidence.HIGH, maf.getConfidence());
		
		// bump numbers down so we get a low conf call
		maf.setNovelStartCount(4);
		MafFilterUtils.classifyMAFRecord(maf);
		assertEquals(MafConfidence.LOW, maf.getConfidence());
		
		// and again to get ZERO
		maf.setNovelStartCount(3);
		MafFilterUtils.classifyMAFRecord(maf);
		assertEquals(MafConfidence.ZERO, maf.getConfidence());
	}
	
	@Ignore
	public void testClassifyMafRecordIns() {
		MAFRecord maf = new MAFRecord();
		maf.setVariantType(MutationType.INS);
		MafFilterUtils.classifyMAFRecord(maf);
		assertEquals(MafConfidence.ZERO, maf.getConfidence());
		
		// set novel db snp id
		maf.setDbSnpId("novel");
		maf.setFlag(SnpUtils.PASS);
		MafFilterUtils.classifyMAFRecord(maf, fo);
		assertEquals(MafConfidence.HIGH, maf.getConfidence());
		
		maf.setFlag(SnpUtils.MUTATION_IN_UNFILTERED_NORMAL);
		MafFilterUtils.classifyMAFRecord(maf, fo);
		assertEquals(MafConfidence.LOW, maf.getConfidence());
		
		// bump numbers down so we get a low conf call
		maf.setDbSnpId("rs1234");
		maf.setFlag(SnpUtils.PASS);
		MafFilterUtils.classifyMAFRecord(maf, fo);
		assertEquals(MafConfidence.ZERO, maf.getConfidence());
		
	}
	
	@Test
	public void testContainsNovelStarts() {
		assertEquals(false, MafFilterUtils.containsNovelStart(null));
		MAFRecord maf = new MAFRecord();
		assertEquals(false, MafFilterUtils.containsNovelStart(maf));
		maf.setNovelStartCount(-1);
		assertEquals(false, MafFilterUtils.containsNovelStart(maf));
		maf.setNovelStartCount(0);
		assertEquals(false, MafFilterUtils.containsNovelStart(maf));
		maf.setNovelStartCount(1);
		assertEquals(true, MafFilterUtils.containsNovelStart(maf));
		maf.setNovelStartCount(2);
		assertEquals(true, MafFilterUtils.containsNovelStart(maf));
		maf.setNovelStartCount(200);
		assertEquals(true, MafFilterUtils.containsNovelStart(maf));
	}
	
	@Test
	public void testCheckAltFrequency() {
		assertEquals(false, MafFilterUtils.checkAltFrequency(0, null));
		MAFRecord maf = new MAFRecord();
		maf.setMafType(MafType.SNV_SOMATIC);
		assertEquals(false, MafFilterUtils.checkAltFrequency(1, maf));
		maf.setTd("A:1[40],0[0],C:1[40],0[0]");
		assertEquals(false, MafFilterUtils.checkAltFrequency(1, maf));
		maf.setRef("A");
		assertEquals(false, MafFilterUtils.checkAltFrequency(1, maf));
		maf.setTumourAllele1("A");
		assertEquals(false, MafFilterUtils.checkAltFrequency(1, maf));
		maf.setTumourAllele2("C");
		assertEquals(true, MafFilterUtils.checkAltFrequency(0, maf));
		assertEquals(true, MafFilterUtils.checkAltFrequency(1, maf));
		assertEquals(false, MafFilterUtils.checkAltFrequency(2, maf));
	}
	
	@Test
	public void testCheckAltFrequencyGermline() {
		assertEquals(false, MafFilterUtils.checkAltFrequency(0, null));
		MAFRecord maf = new MAFRecord();
		maf.setMafType(MafType.SNV_GERMLINE);
		assertEquals(false, MafFilterUtils.checkAltFrequency(1, maf));
		maf.setTd("A:1[40],0[0],C:1[40],0[0]");
		assertEquals(false, MafFilterUtils.checkAltFrequency(1, maf));
		maf.setRef("A");
		assertEquals(false, MafFilterUtils.checkAltFrequency(1, maf));
		maf.setTumourAllele1("A");
		assertEquals(false, MafFilterUtils.checkAltFrequency(1, maf));
		maf.setTumourAllele2("C");
		assertEquals(false, MafFilterUtils.checkAltFrequency(0, maf));
		assertEquals(false, MafFilterUtils.checkAltFrequency(1, maf));
		assertEquals(false, MafFilterUtils.checkAltFrequency(2, maf));
		
		maf.setNd("A:1[40],0[0],C:1[40],0[0]");
		assertEquals(false, MafFilterUtils.checkAltFrequency(1, maf));		// need both normal alleles to be set
		maf.setNormalAllele1("A");
		maf.setNormalAllele2("C");
		assertEquals(true, MafFilterUtils.checkAltFrequency(1, maf));		// need both normal alleles to be set
		
	}
	
	@Test
	public void testCheckNonEmptyGene() {
		assertEquals(false, MafFilterUtils.checkNonEmptyGene(null));
		MAFRecord maf = new MAFRecord();
		assertEquals(false, MafFilterUtils.checkNonEmptyGene(maf));
		maf.setHugoSymbol(null);
		assertEquals(false, MafFilterUtils.checkNonEmptyGene(maf));
		maf.setHugoSymbol("");
		assertEquals(false, MafFilterUtils.checkNonEmptyGene(maf));
		maf.setHugoSymbol(" ");
		assertEquals(false, MafFilterUtils.checkNonEmptyGene(maf));
		maf.setHugoSymbol("Unknown");
		assertEquals(false, MafFilterUtils.checkNonEmptyGene(maf));
		maf.setHugoSymbol("--");
		assertEquals(false, MafFilterUtils.checkNonEmptyGene(maf));
		maf.setHugoSymbol("---");
		assertEquals(false, MafFilterUtils.checkNonEmptyGene(maf));
		maf.setHugoSymbol("-");
		assertEquals(false, MafFilterUtils.checkNonEmptyGene(maf));
		maf.setHugoSymbol("-ABC");
		assertEquals(true, MafFilterUtils.checkNonEmptyGene(maf));
		maf.setHugoSymbol("ABC123");
		assertEquals(true, MafFilterUtils.checkNonEmptyGene(maf));
	}
	
	@Test
	public void testCheckConsequence() {
		assertEquals(false, MafFilterUtils.checkConsequence(null));
		MAFRecord maf = new MAFRecord();
		assertEquals(false, MafFilterUtils.checkConsequence(maf));
		maf.setVariantClassification(null);
		assertEquals(false, MafFilterUtils.checkConsequence(maf));
		maf.setVariantClassification("");
		assertEquals(false, MafFilterUtils.checkConsequence(maf));
		maf.setVariantClassification("blah");
		assertEquals(false, MafFilterUtils.checkConsequence(maf));
		maf.setVariantClassification("Splice_Site");
		assertEquals(true, MafFilterUtils.checkConsequence(maf));
		maf.setVariantClassification("blah;blah;blah");
		assertEquals(false, MafFilterUtils.checkConsequence(maf));
		maf.setVariantClassification("blah;blah;Splice_Site;blah");
		assertEquals(true, MafFilterUtils.checkConsequence(maf));
	}
	
	@Test
	public void testCheckNovelStarts() {
		assertEquals(false, MafFilterUtils.checkNovelStarts(0, null));
		MAFRecord maf = new MAFRecord();
		assertEquals(true, MafFilterUtils.checkNovelStarts(0, maf));
		assertEquals(false, MafFilterUtils.checkNovelStarts(1, maf));
		maf.setNovelStartCount(0);
		assertEquals(true, MafFilterUtils.checkNovelStarts(0, maf));
		assertEquals(false, MafFilterUtils.checkNovelStarts(1, maf));
		maf.setNovelStartCount(1);
		assertEquals(true, MafFilterUtils.checkNovelStarts(0, maf));
		assertEquals(true, MafFilterUtils.checkNovelStarts(1, maf));
		assertEquals(false, MafFilterUtils.checkNovelStarts(2, maf));
		maf.setNovelStartCount(2);
		assertEquals(true, MafFilterUtils.checkNovelStarts(0, maf));
		assertEquals(true, MafFilterUtils.checkNovelStarts(1, maf));
		assertEquals(true, MafFilterUtils.checkNovelStarts(2, maf));
		assertEquals(false, MafFilterUtils.checkNovelStarts(3, maf));
		maf.setNovelStartCount(200);
		assertEquals(true, MafFilterUtils.checkNovelStarts(0, maf));
		assertEquals(true, MafFilterUtils.checkNovelStarts(1, maf));
		assertEquals(true, MafFilterUtils.checkNovelStarts(2, maf));
		assertEquals(true, MafFilterUtils.checkNovelStarts(3, maf));
		assertEquals(true, MafFilterUtils.checkNovelStarts(199, maf));
		assertEquals(true, MafFilterUtils.checkNovelStarts(200, maf));
		assertEquals(false, MafFilterUtils.checkNovelStarts(201, maf));
	}
	
	@Test
	public void testCheckDbSnpIsNovel() {
		assertEquals(false, MafFilterUtils.checkDbSnpIsNovel(null));
		MAFRecord maf = new MAFRecord();
		assertEquals(false, MafFilterUtils.checkDbSnpIsNovel(maf));
		maf.setDbSnpId(null);
		assertEquals(false, MafFilterUtils.checkDbSnpIsNovel(maf));
		maf.setDbSnpId("");
		assertEquals(false, MafFilterUtils.checkDbSnpIsNovel(maf));
		maf.setDbSnpId("db123");
		assertEquals(false, MafFilterUtils.checkDbSnpIsNovel(maf));
		maf.setDbSnpId("db123novel");
		assertEquals(false, MafFilterUtils.checkDbSnpIsNovel(maf));
		maf.setDbSnpId("novel123");
		assertEquals(false, MafFilterUtils.checkDbSnpIsNovel(maf));
		maf.setDbSnpId("novel");
		assertEquals(true, MafFilterUtils.checkDbSnpIsNovel(maf));
		maf.setDbSnpId("NOVEL");
		assertEquals(true, MafFilterUtils.checkDbSnpIsNovel(maf));
		maf.setDbSnpId("Novel");
		assertEquals(true, MafFilterUtils.checkDbSnpIsNovel(maf));
	}
	
	@Test
	public void testCheckValidationStatus() {
		assertEquals(false, MafFilterUtils.checkValidationStatus(null));
		MAFRecord maf = new MAFRecord();
		assertEquals(false, MafFilterUtils.checkValidationStatus(maf));
		maf.setValidationStatus(null);
		assertEquals(false, MafFilterUtils.checkValidationStatus(maf));
		maf.setValidationStatus("");
		assertEquals(false, MafFilterUtils.checkValidationStatus(maf));
		maf.setValidationStatus("hello");
		assertEquals(false, MafFilterUtils.checkValidationStatus(maf));
		maf.setValidationStatus("world");
		assertEquals(false, MafFilterUtils.checkValidationStatus(maf));
		maf.setValidationStatus("hello valid");
		assertEquals(false, MafFilterUtils.checkValidationStatus(maf));
		maf.setValidationStatus("valid hello");
		assertEquals(false, MafFilterUtils.checkValidationStatus(maf));
		maf.setValidationStatus("valid");
		assertEquals(true, MafFilterUtils.checkValidationStatus(maf));
		maf.setValidationStatus("Valid");
		assertEquals(true, MafFilterUtils.checkValidationStatus(maf));
		maf.setValidationStatus("VALID");
		assertEquals(true, MafFilterUtils.checkValidationStatus(maf));
		
	}
	
	@Test
	public void testClassifyRealLifeRecord() throws QMafException {
		List<MAFRecord> mafs = new ArrayList<MAFRecord>();
		Map<String, Set<Integer>> ensemblToEntrez = new HashMap<String, Set<Integer>>();
		String s = "AOCS_067_SNP_54973      1       17      30965850        30965850        1       -888    -888    T       T/T     C/T     T>C     -999    -999    0.12844960008474762     19      2       2       -888    -999    -999    T:0[0],12[33.83]        C:0[0],5[7.6],T:1[40],13[23]    5       missense_variant,missense_variant,non_coding_exon_variant,nc_transcript_variant,missense_variant,intron_variant,nc_transcript_variant,non_coding_exon_variant,nc_transcript_variant     N867D,N779D,-888,-888,N867D,-888,-888,-888,-888 2904T>C,2879T>C,546T>C,546T>C,2871T>C,-888,-888,505T>C,505T>C   PF06017,PF06017,PF06017 ENSG00000176658,ENSG00000176658,ENSG00000176658,ENSG00000176658,ENSG00000176658,,,ENSG00000176658,ENSG00000176658       ENST00000318217,ENST00000394649,ENST00000577352,ENST00000577352,ENST00000579584,ENST00000582272,ENST00000582272,ENST00000581059,ENST00000581059 70      -999    MYO1D,MYO1D,MYO1D,MYO1D,MYO1D,,,MYO1D,MYO1D     PF06017,PF06017,PF06017 pfam,pfam,pfam  Myosin_tail_2,Myosin_tail_2,Myosin_tail_2       chr17:30965850-30965850 PASS    TCGATCTACCT";
		TabbedRecord data = new TabbedRecord();
		s = s.replaceAll("\\s+", "\t");
		data.setData(s);
		
		MafUtils.convertDccToMaf(data, "AOCS_067", "controlSampleID", "tumourSampleID", null, mafs, ensemblToEntrez, true, true);
		
		MAFRecord maf = mafs.get(0);
		MafFilterUtils.classifyMAFRecord(maf);
		assertEquals(MafConfidence.HIGH_CONSEQUENCE, maf.getConfidence());
	}
	
	@Test
	public void testClassifyRealLifeRecordIndel() throws QMafException {
		List<MAFRecord> mafs = new ArrayList<MAFRecord>();
		Map<String, Set<Integer>> ensemblToEntrez = new HashMap<String, Set<Integer>>();
		String s = "aba9fc0c_7f03_417f_b087_2e8ab1a45e42_ICGC-ABMJ-20120706-01_ind353976	3	chr11	62638311	62638313	1	-999	-999	TAA	-999	---	TAA/---	1	-999	-999	-999	-999	-999	-999	-99	-999	0;46;37;0;1;0;1	0;34;30;0;0;0;0	--	splice_donor_variant,intron_variant,feature_truncation,splice_donor_variant,intron_variant,feature_truncation,intron_variant,feature_truncation,intron_variant,feature_truncation,intron_variant,feature_truncation,splice_donor_variant,intron_variant,feature_truncation	-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888	-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888	-888	ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003	ENST00000377892,ENST00000377892,ENST00000377892,ENST00000377890,ENST00000377890,ENST00000377890,ENST00000377891,ENST00000377891,ENST00000377889,ENST00000377889,ENST00000535296,ENST00000535296,ENST00000538084,ENST00000538084,ENST00000538084	70	-999	SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2	--	--	--	chr11:62638311-62638313	PASS;NNS;HOMADJ_2	--";
		TabbedRecord data = new TabbedRecord();
		s = s.replaceAll("\\s+", "\t");
		data.setData(s);
		
		MafUtils.convertDccToMaf(data, "", "controlSampleID", "tumourSampleID", null, mafs, ensemblToEntrez, true, true);
		
		MAFRecord maf = mafs.get(0);
		MafFilterUtils.classifyMAFRecord(maf);
		assertEquals(MafConfidence.HIGH_CONSEQUENCE, maf.getConfidence());
	}
	
	@Test
	public void testClassifyRealLifeRecordIndel2() throws QMafException {
		List<MAFRecord> mafs = new ArrayList<MAFRecord>();
		Map<String, Set<Integer>> ensemblToEntrez = new HashMap<String, Set<Integer>>();
		String s = "e3201e6a_2b36_4f04_8eb8_3c71ca2dc59d_ICGC-DBPC-20130205-124_ind14587	2	chr17	39411682	39411683	1	-999	-999	---------------	-99	ACCACCTGCTGCAGG	---------------/ACCACCTGCTGCAGG		-999	-999	-999	-999	-999	-999	-999	-999	0;32;32;0[0|0];0;0;11	0;37;37;0[0|0];0;0;7;\"2_discontiguous_CTGCTGCAGGaccacctgctgcaggACCACCTGCT\"	--	downstream_gene_variant,frameshift_variant,feature_truncation	-888,RT15RT,RT15RT	-888,47->ACCACCTGCTGCAGG,47->ACCACCTGCTGCAGG	-888	ENSG00000241595,ENSG00000198083,ENSG00000198083	ENST00000334109,ENST00000394008,ENST00000394008	70	-999	KRTAP9-4,KRTAP9-9,KRTAP9-9	--	--	--	chr17:39411682-39411683	PASS;NNS;HOMADJ_2	--";
		TabbedRecord data = new TabbedRecord();
		s = s.replaceAll("\\s+", "\t");
		data.setData(s);
		
		MafUtils.convertDccToMaf(data, "", "controlSampleID", "tumourSampleID", null, mafs, ensemblToEntrez, false, true);
		
		MAFRecord maf = mafs.get(0);
		MafFilterUtils.classifyMAFRecord(maf);
		assertEquals(MafConfidence.HIGH_CONSEQUENCE, maf.getConfidence());
	}
	
	@Test
	public void testCheckNovelStartsIndelException() {
		try {
			assertEquals(false, MafFilterUtils.checkNovelStartsIndel(-1, null));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals(false, MafFilterUtils.checkNovelStartsIndel(0, null));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		try {
			MAFRecord maf = new MAFRecord();
			maf.setMafType(MafType.INDEL_SOMATIC);
			assertEquals(false, MafFilterUtils.checkNovelStartsIndel(0, maf));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
	}
	
	@Test
	public void testCheckNovelStartsIndel() {
		MAFRecord maf = new MAFRecord();
		maf.setFlag("PASS;NNS;HOMADJ_2");
		maf.setMafType(MafType.INDEL_SOMATIC);
		assertEquals(false, MafFilterUtils.checkNovelStartsIndel(5, maf));
		maf.setMafType(MafType.INDEL_GERMLINE);
		assertEquals(false, MafFilterUtils.checkNovelStartsIndel(5, maf));
		
		maf.setNd("0;46;37;0;1;0;1");
		assertEquals(false, MafFilterUtils.checkNovelStartsIndel(5, maf));
		maf.setMafType(MafType.INDEL_SOMATIC);
		assertEquals(false, MafFilterUtils.checkNovelStartsIndel(5, maf));
		
		maf.setTd("0;34;30;0;0;0;0;\"2 discontiguous AGATGATAGG___TAATAATAAT\"");
		assertEquals(false, MafFilterUtils.checkNovelStartsIndel(5, maf));
		maf.setMafType(MafType.INDEL_GERMLINE);
		assertEquals(true, MafFilterUtils.checkNovelStartsIndel(5, maf));
	}
	
	@Test
	public void testCheckIndelMotifException() {
		try {
			assertEquals(false, MafFilterUtils.checkIndelMotif(null, null));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals(false, MafFilterUtils.checkIndelMotif(null, MafType.INDEL_SOMATIC));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		try {
			assertEquals(false, MafFilterUtils.checkIndelMotif(new MAFRecord(), MafType.INDEL_SOMATIC));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
	}
	
	@Test
	public void testCheckIndelMotif() {
		MAFRecord maf = new MAFRecord();
		// not small & >=5%
		maf.setRef("GGTAATTAT");
		maf.setNd("0;69;56;0;0;0;0");
		maf.setTd("5;90;67;5;0;0;1;\"2 discontiguous AAAATTCAGA_________GGTAATTATG\"");
		maf.setFlag("PASS;HOMADJ_2");
		assertEquals(true, MafFilterUtils.checkIndelMotif(maf, MafType.INDEL_SOMATIC));
		
		// small motif but >10%
		maf.setRef("-");
		maf.setNd("0;40;39;0;0;0;0");
		maf.setTd("7;70;69;7;0;1;0;\"2 discontiguous TGGGTGGGGTgTTGGCCTCAG\"");
		maf.setFlag("PASS;HOMADJ_2");
		assertEquals(true, MafFilterUtils.checkIndelMotif(maf, MafType.INDEL_SOMATIC));
		
		// low conf
		maf.setRef("T");
		maf.setNd("0;35;35;0;0;1;0");
		maf.setTd("7;74;72;7;0;0;0;\"2 discontiguous ACACACGGCC_GCCACACTCA\"");
		maf.setFlag("PASS;HOMADJ_2");
		assertEquals(false, MafFilterUtils.checkIndelMotif(maf, MafType.INDEL_SOMATIC));
	}
	
	@Test
	public void testCheckIndelEvidenceException() {
		try {
			assertEquals(false, MafFilterUtils.checkIndelEvidence(null));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		MAFRecord maf = new MAFRecord();
		maf.setMafType(MafType.INDEL_SOMATIC);
		try {
			assertEquals(false, MafFilterUtils.checkIndelEvidence(maf));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
		maf.setMafType(MafType.INDEL_GERMLINE);
		try {
			assertEquals(false, MafFilterUtils.checkIndelEvidence(maf));
			Assert.fail("Should have thrown an IAE");
		} catch (IllegalArgumentException iae) {}
	}
	
	@Test
	public void testCheckIndelEvidence() {
		MAFRecord maf = new MAFRecord();
		maf.setNd("0;0;0;0;0;0;0");
		maf.setMafType(MafType.INDEL_GERMLINE);
		assertEquals(false, MafFilterUtils.checkIndelEvidence(maf));
		maf.setNd("1;0;10;0;0;0;0");
		assertEquals(true, MafFilterUtils.checkIndelEvidence(maf));
		maf.setNd("5;0;100;0;0;0;0");
		assertEquals(true, MafFilterUtils.checkIndelEvidence(maf));
		maf.setNd("4;0;100;0;0;0;0");
		assertEquals(false, MafFilterUtils.checkIndelEvidence(maf));
		
		maf.setTd("0;0;0;0;0;0;0");
		maf.setMafType(MafType.INDEL_SOMATIC);
		assertEquals(false, MafFilterUtils.checkIndelEvidence(maf));
		maf.setTd("1;0;10;0;0;0;0");
		assertEquals(true, MafFilterUtils.checkIndelEvidence(maf));
		maf.setTd("5;0;100;0;0;0;0");
		assertEquals(true, MafFilterUtils.checkIndelEvidence(maf));
		maf.setTd("4;0;100;0;0;0;0");
		assertEquals(false, MafFilterUtils.checkIndelEvidence(maf));
	}
			
}
