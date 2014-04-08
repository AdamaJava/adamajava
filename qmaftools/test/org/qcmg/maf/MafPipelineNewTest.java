package org.qcmg.maf;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.qcmg.common.dcc.MutationType;
import org.qcmg.common.model.MafConfidence;
import org.qcmg.common.model.MafType;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.maf.util.FilterOptions;
import org.qcmg.maf.util.MafFilterUtils;
import org.qcmg.maf.util.MafUtils;
import org.qcmg.tab.TabbedRecord;

public class MafPipelineNewTest {
	
	
	@Test
	public void testCheckAlleleFractionSnvSomatic() {
		// create a maf record, add to mafs and see if we can change the flag field depending on the maf type value
		MAFRecord maf = new MAFRecord();
		
		String annotation = SnpUtils.ALLELIC_FRACTION + 5;
		
		TestMafPipelineNew mpn = new TestMafPipelineNew();
		mpn.mafs.add(maf);
		mpn.alleleFraction = MafPipelineSNV.SOMATIC_ALLELE_FRACTION;
		mpn.mafType = MafType.SNV_SOMATIC;
		
		maf.setRef("A");
		maf.setTd("A:96[30],0[0],G:4[30],0[0]");
		maf.setTumourAllele1("A");
		maf.setTumourAllele2("G");
		maf.setVariantType(MutationType.SNP);
		mpn.checkAlleleFraction();
		assertEquals(annotation, maf.getFlag());
		
		maf.setFlag(null);
		maf.setTd("A:100[30],0[0],G:5[30],0[0]");
		mpn.checkAlleleFraction();
		assertEquals(annotation, maf.getFlag());
		
		maf.setFlag(null);
		maf.setTd("A:95[30],0[0],G:5[30],0[0]");
		mpn.checkAlleleFraction();
		assertEquals(null, maf.getFlag());
	}
	
	@Test
	public void testCheckAlleleFractionSnvGermline() {
		// create a maf record, add to mafs and see if we can change the flag field depending on the maf type value
		MAFRecord maf = new MAFRecord();
		
		String annotation = SnpUtils.ALLELIC_FRACTION + 20;
		
		TestMafPipelineNew mpn = new TestMafPipelineNew();
		mpn.mafs.add(maf);
		mpn.alleleFraction = MafPipelineSNV.GERMLINE_ALLELE_FRACTION;
		mpn.mafType = MafType.SNV_GERMLINE;
		
		maf.setRef("A");
		maf.setNd("A:96[30],0[0],G:4[30],0[0]");
		maf.setNormalAllele1("A");
		maf.setNormalAllele2("G");
		maf.setVariantType(MutationType.SNP);
		mpn.checkAlleleFraction();
		assertEquals(annotation, maf.getFlag());
		
		maf.setFlag(null);
		maf.setNd("A:95[30],0[0],G:5[30],0[0]");
		mpn.checkAlleleFraction();
		assertEquals(annotation, maf.getFlag());
		
		maf.setFlag(null);
		maf.setNd("A:90[30],0[0],G:10[30],0[0]");
		mpn.checkAlleleFraction();
		assertEquals(annotation, maf.getFlag());
		
		maf.setFlag(null);
		maf.setNd("A:81[30],0[0],G:19[30],0[0]");
		mpn.checkAlleleFraction();
		assertEquals(annotation, maf.getFlag());
		
		maf.setFlag(null);
		maf.setNd("A:80[30],0[0],G:20[30],0[0]");
		mpn.checkAlleleFraction();
		assertEquals(null, maf.getFlag());
		
		maf.setNd("A:20[30],0[0],G:80[30],0[0]");
		mpn.checkAlleleFraction();
		assertEquals(null, maf.getFlag());
		
		maf.setNd("G:80[30],0[0]");
		mpn.checkAlleleFraction();
		assertEquals(null, maf.getFlag());
	}
	
	@Test
	public void testRealLifeIndel() throws QMafException {
		
		List<MAFRecord> mafs = new ArrayList<MAFRecord>();
		Map<String, Set<Integer>> ensemblToEntrez = new HashMap<String, Set<Integer>>();
		String s = "aba9fc0c_7f03_417f_b087_2e8ab1a45e42_ICGC-ABMJ-20120706-01_ind353976	3	chr11	62638311	62638313	1	-999	-999	TAA	-999	---	TAA/---	1	-999	-999	-999	-999	-999	-999	-99	-999	0;46;37;0;1;0;1	0;34;30;0;0;0;0	--	splice_donor_variant,intron_variant,feature_truncation,splice_donor_variant,intron_variant,feature_truncation,intron_variant,feature_truncation,intron_variant,feature_truncation,intron_variant,feature_truncation,splice_donor_variant,intron_variant,feature_truncation	-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888	-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888,-888	-888	ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003,ENSG00000168003	ENST00000377892,ENST00000377892,ENST00000377892,ENST00000377890,ENST00000377890,ENST00000377890,ENST00000377891,ENST00000377891,ENST00000377889,ENST00000377889,ENST00000535296,ENST00000535296,ENST00000538084,ENST00000538084,ENST00000538084	70	-999	SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2,SLC3A2	--	--	--	chr11:62638311-62638313	PASS;NNS;HOMADJ_2	--";
		TabbedRecord data = new TabbedRecord();
		s = s.replaceAll("\\s+", "\t");
		data.setData(s);
		
		MafUtils.convertDccToMaf(data, "", "controlSampleID", "tumourSampleID", null, mafs, ensemblToEntrez, true, true);
		FilterOptions fo = new FilterOptions();
		fo.setHomopolymerCutoff(MafPipelineNew.HOMOPOLYMER_CUTOFF);
		MAFRecord maf = mafs.get(0);
		MafFilterUtils.classifyMAFRecord(maf, fo);
		assertEquals(MafConfidence.HIGH_CONSEQUENCE, maf.getConfidence());
		
		
		TestMafPipelineNew mpn = new TestMafPipelineNew();
		mpn.mafs.add(maf);
		mpn.alleleFraction = MafPipelineSNV.GERMLINE_ALLELE_FRACTION;
		mpn.mafType = MafType.INDEL_SOMATIC;
		mpn.updateMafsWithMafType();
		
		mpn.checkIndel();
		assertEquals(MafConfidence.LOW_CONSEQUENCE, maf.getConfidence());
	}
	
	@Test
	public void testRealLifeIndel2() throws QMafException {
		
		MAFRecord maf = new MAFRecord();
		maf.setConfidence(MafConfidence.HIGH);
		maf.setTd("0;398;380;0[0|0];0;0;0;\"4 contiguous CCTGCACCCC_AGGTGAGCAT\"");
		maf.setFlag("PASS;NNS;HOMCON_4");
		maf.setVariantType(MutationType.DEL);
		
		
		TestMafPipelineNew mpn = new TestMafPipelineNew();
		mpn.mafs.add(maf);
		mpn.alleleFraction = MafPipelineSNV.GERMLINE_ALLELE_FRACTION;
		mpn.mafType = MafType.INDEL_SOMATIC;
		mpn.updateMafsWithMafType();
		
		mpn.checkIndel();
		assertEquals(MafConfidence.LOW, maf.getConfidence());
	}
	
}
