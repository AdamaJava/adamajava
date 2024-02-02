package au.edu.qimr.qannotate.nanno;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.ChrPositionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class AnnotationSourceTest {
	
	@Rule
	public final TemporaryFolder testFolder = new TemporaryFolder();
  	
	
	@Test
	public void compareNameAndPositions() {
		assertEquals(0, AnnotationSource.compareChromosomeNameAndStartPositions("chr1", 1, "chr1", 1));
		assertEquals(0, AnnotationSource.compareChromosomeNameAndStartPositions("chr1", 1, "1", 1));
		assertEquals(0, AnnotationSource.compareChromosomeNameAndStartPositions("1", 1, "chr1", 1));
		assertEquals(0, AnnotationSource.compareChromosomeNameAndStartPositions("1", 1, "1", 1));
		assertEquals(-1, Integer.compare(1, 2));
		assertEquals(-1, AnnotationSource.compareChromosomeNameAndStartPositions("1", 1, "1", 2));
		assertEquals(-1, AnnotationSource.compareChromosomeNameAndStartPositions("1", 1, "2", 2));
		assertEquals(0, AnnotationSource.compareChromosomeNameAndStartPositions("2", 2, "2", 2));
		assertEquals(1, AnnotationSource.compareChromosomeNameAndStartPositions("2", 3, "2", 2));
		assertEquals(1, AnnotationSource.compareChromosomeNameAndStartPositions("3", 32, "2", 2));
	}
	
	@Test
	public void compareCPs() {
		ChrPosition cp1 = null;
		ChrPosition cp2 = null;
		assertEquals(1, AnnotationSource.areCPsEqual(cp1, cp2));
		
		cp1 = ChrPositionUtils.getChrPosition("chr1", 1, 1);
		assertEquals(-1, AnnotationSource.areCPsEqual(cp1, cp2));
		cp2 = ChrPositionUtils.getChrPosition("chr1", 1, 1);
		assertEquals(0, AnnotationSource.areCPsEqual(cp1, cp2));
		
		cp2 = ChrPositionUtils.getChrPosition("chr1", 2, 2);
		assertEquals(-1, AnnotationSource.areCPsEqual(cp1, cp2));
		
		cp1 = ChrPositionUtils.getChrPosition("chr1", 3, 3);
		assertEquals(1, AnnotationSource.areCPsEqual(cp1, cp2));
		
		cp2 = ChrPositionUtils.getChrPosition("chr1", 3, 3);
		assertEquals(0, AnnotationSource.areCPsEqual(cp1, cp2));
		
		cp2 = ChrPositionUtils.getChrPosition("chr3", 3, 3);
		assertEquals(-2, AnnotationSource.areCPsEqual(cp1, cp2));
	}
	
	@Test
	public void isThisOurRecord() {
		ChrPosition cp1 = ChrPositionUtils.getChrPosition("chr10", 1, 1);
		assertEquals(-1, AnnotationSource.isThisOurRecord(cp1, null, 0));
		assertEquals(1, AnnotationSource.isThisOurRecord(null, null, 0));
		assertEquals(1, AnnotationSource.isThisOurRecord(cp1, "chr10", 0));
		assertEquals(-1, AnnotationSource.isThisOurRecord(cp1, "chr10", 2));
		assertEquals(-1, AnnotationSource.isThisOurRecord(cp1, "chr11", 1));
		assertEquals(1, AnnotationSource.isThisOurRecord(cp1, "chr9", 1));
		assertEquals(0, AnnotationSource.isThisOurRecord(cp1, "chr10", 1));
		assertEquals(-1, AnnotationSource.isThisOurRecord(cp1, "chr10", 10));
		assertEquals(1, AnnotationSource.isThisOurRecord(cp1, "chr10", 0));
	}
	
	@Test
	public void getCPFromStringArray() {
		try {
            assertNull(AnnotationSource.getCpFromRecord(null, 0, 0));
			Assert.fail("Should have thrown IAE");
		} catch (IllegalArgumentException iae) {}
		try {
            assertNull(AnnotationSource.getCpFromRecord(new String[]{}, 0, 0));
			Assert.fail("Should have thrown IAE");
		} catch (IllegalArgumentException iae) {}
		ChrPosition cp = ChrPositionUtils.getChrPosition("1", 1, 1);
		assertEquals(cp, AnnotationSource.getCpFromRecord(new String[]{"1"}, 0, 0));
		assertEquals(ChrPositionUtils.getChrPosition("1", 0, 0), AnnotationSource.getCpFromRecord(new String[]{"1", "0"}, 0, 1));
		assertEquals(ChrPositionUtils.getChrPosition("1", 2, 2), AnnotationSource.getCpFromRecord(new String[]{"1", "2"}, 0, 1));
		assertEquals(ChrPositionUtils.getChrPosition("2", 1, 1), AnnotationSource.getCpFromRecord(new String[]{"1", "2"}, 1, 0));
		try {
			assertEquals(ChrPositionUtils.getChrPosition("2", 1, 1), AnnotationSource.getCpFromRecord(new String[]{"1", "2"}, 10, 0));
			Assert.fail("Should have thrown IAE");
		} catch (IllegalArgumentException iae) {}
	}
	
	@Test
	public void vcfFields() {
		String info = "";
		assertEquals("EMPTY", AnnotationSourceVCF.extractFieldsFromInfoField(info, Arrays.asList("",""), "EMPTY"));
		assertEquals("EMPTY", AnnotationSourceVCF.extractFieldsFromInfoField(".", Arrays.asList("",""), "EMPTY"));
		assertEquals("EMPTY", AnnotationSourceVCF.extractFieldsFromInfoField(".", List.of(""), "EMPTY"));
		info = "ALLELEID=75079;CLNDISDB=MONDO:MONDO:0010075,MedGen:C4017377,OMIM:271640|MONDO:MONDO:0014139,MedGen:C3809210,OMIM:615349,Orphanet:ORPHA536467|MONDO:MONDO:0019675,MedGen:C0432243,OMIM:PS271640,Orphanet:ORPHA93359,SNOMED_CT:254100000|MedGen:CN517202;CLNDN=Spondyloepimetaphyseal_dysplasia_with_joint_laxity,_type_1,_with_or_without_fractures|Ehlers-Danlos_syndrome,_spondylodysplastic_type,_2|Spondyloepimetaphyseal_dysplasia_with_joint_laxity|not_provided;CLNHGVS=NC_000001.11:g.1232279A>G;CLNREVSTAT=criteria_provided,_multiple_submitters,_no_conflicts;CLNSIG=Pathogenic;CLNVC=single_nucleotide_variant;CLNVCSO=SO:0001483;CLNVI=OMIM_Allelic_Variant:615291.0001;GENEINFO=B3GALT6:126792;MC=SO:0001582|initiatior_codon_variant,SO:0001583|missense_variant;ORIGIN=1;RS=786200938";
		assertEquals("EMPTY", AnnotationSourceVCF.extractFieldsFromInfoField(info, List.of(""), "EMPTY"));
		assertEquals("ALLELEID=75079", AnnotationSourceVCF.extractFieldsFromInfoField(info, List.of("ALLELEID"), "EMPTY"));
		assertEquals("ALLELEID=75079\tCLNDISDB=MONDO:MONDO:0010075,MedGen:C4017377,OMIM:271640|MONDO:MONDO:0014139,MedGen:C3809210,OMIM:615349,Orphanet:ORPHA536467|MONDO:MONDO:0019675,MedGen:C0432243,OMIM:PS271640,Orphanet:ORPHA93359,SNOMED_CT:254100000|MedGen:CN517202", AnnotationSourceVCF.extractFieldsFromInfoField(info, Arrays.asList("ALLELEID","CLNDISDB"), "EMPTY"));
		assertEquals("CLNDN=Spondyloepimetaphyseal_dysplasia_with_joint_laxity,_type_1,_with_or_without_fractures|Ehlers-Danlos_syndrome,_spondylodysplastic_type,_2|Spondyloepimetaphyseal_dysplasia_with_joint_laxity|not_provided\tMC=SO:0001582|initiatior_codon_variant,SO:0001583|missense_variant", AnnotationSourceVCF.extractFieldsFromInfoField(info, Arrays.asList("CLNDN","MC"), "EMPTY"));
		info = "ALLELEID=1211496;CLNDISDB=MedGen:CN517202;CLNDN=not_provided;CLNHGVS=NC_000001.11:g.19251559C>G;CLNREVSTAT=criteria_provided,_single_submitter;CLNSIG=Benign;CLNVC=single_nucleotide_variant;CLNVCSO=SO:0001483;GENEINFO=EMC1:23065;ORIGIN=1";
		assertEquals("MC=\tCLNDN=not_provided", AnnotationSourceVCF.extractFieldsFromInfoField(info, Arrays.asList("MC","CLNDN"), "EMPTY"));
	}
	
	@Test
	public void tsvGetHeader() {
		String header = "#chr	pos(1-based)	ref	alt	aaref	aaalt	rs_dbSNP151	hg19_chr	hg19_pos(1-based)	hg18_chr	hg18_pos(1-based)	aapos	genename	Ensembl_geneid	Ensembl_transcriptid	Ensembl_proteinid	Uniprot_acc	Uniprot_entry	HGVSc_ANNOVAR	HGVSp_ANNOVAR	HGVSc_snpEff	HGVSp_snpEff	HGVSc_VEP	HGVSp_VEP	APPRIS	GENCODE_basic	TSL	VEP_canonical	cds_strand	refcodon	codonpos	codon_degeneracy	Ancestral_allele	AltaiNeandertal	Denisova	VindijiaNeandertal	SIFT_score	SIFT_converted_rankscore	SIFT_pred	SIFT4G_score	SIFT4G_converted_rankscore	SIFT4G_pred	Polyphen2_HDIV_score	Polyphen2_HDIV_rankscore	Polyphen2_HDIV_pred	Polyphen2_HVAR_score	Polyphen2_HVAR_rankscore	Polyphen2_HVAR_pred	LRT_score	LRT_converted_rankscore	LRT_pred	LRT_Omega	MutationTaster_score	MutationTaster_converted_rankscore	MutationTaster_pred	MutationTaster_model	MutationTaster_AAE	MutationAssessor_score	MutationAssessor_rankscore	MutationAssessor_pred	FATHMM_score	FATHMM_converted_rankscore	FATHMM_pred	PROVEAN_score	PROVEAN_converted_rankscore	PROVEAN_pred	VEST4_score	VEST4_rankscore	MetaSVM_score	MetaSVM_rankscore	MetaSVM_pred	MetaLR_score	MetaLR_rankscore	MetaLR_pred	Reliability_index	M-CAP_score	M-CAP_rankscore	M-CAP_pred	REVEL_score	REVEL_rankscore	MutPred_score	MutPred_rankscore	MutPred_protID	MutPred_AAchange	MutPred_Top5features	MVP_score	MVP_rankscore	MPC_score	MPC_rankscore	PrimateAI_score	PrimateAI_rankscore	PrimateAI_pred	DEOGEN2_score	DEOGEN2_rankscore	DEOGEN2_pred	BayesDel_addAF_score	BayesDel_addAF_rankscore	BayesDel_addAF_pred	BayesDel_noAF_score	BayesDel_noAF_rankscore	BayesDel_noAF_pred	ClinPred_score	ClinPred_rankscore	ClinPred_pred	LIST-S2_score	LIST-S2_rankscore	LIST-S2_pred	Aloft_Fraction_transcripts_affected	Aloft_prob_Tolerant	Aloft_prob_Recessive	Aloft_prob_Dominant	Aloft_pred	Aloft_Confidence	CADD_raw	CADD_raw_rankscore	CADD_phred	CADD_raw_hg19	CADD_raw_rankscore_hg19	CADD_phred_hg19	DANN_score	DANN_rankscore	fathmm-MKL_coding_score	fathmm-MKL_coding_rankscore	fathmm-MKL_coding_pred	fathmm-MKL_coding_group	fathmm-XF_coding_score	fathmm-XF_coding_rankscore	fathmm-XF_coding_pred	Eigen-raw_coding	Eigen-raw_coding_rankscore	Eigen-phred_coding	Eigen-PC-raw_coding	Eigen-PC-raw_coding_rankscore	Eigen-PC-phred_coding	GenoCanyon_score	GenoCanyon_rankscore	integrated_fitCons_score	integrated_fitCons_rankscore	integrated_confidence_value	GM12878_fitCons_score	GM12878_fitCons_rankscore	GM12878_confidence_value	H1-hESC_fitCons_score	H1-hESC_fitCons_rankscore	H1-hESC_confidence_value	HUVEC_fitCons_score	HUVEC_fitCons_rankscore	HUVEC_confidence_value	LINSIGHT	LINSIGHT_rankscore	GERP++_NR	GERP++_RS	GERP++_RS_rankscore	phyloP100way_vertebrate	phyloP100way_vertebrate_rankscore	phyloP30way_mammalian	phyloP30way_mammalian_rankscore	phyloP17way_primate	phyloP17way_primate_rankscore	phastCons100way_vertebrate	phastCons100way_vertebrate_rankscore	phastCons30way_mammalian	phastCons30way_mammalian_rankscore	phastCons17way_primatephastCons17way_primate_rankscore	SiPhy_29way_pi	SiPhy_29way_logOdds	SiPhy_29way_logOdds_rankscore	bStatistic	bStatistic_converted_rankscore	1000Gp3_AC	1000Gp3_AF	1000Gp3_AFR_AC	1000Gp3_AFR_AF	1000Gp3_EUR_AC	1000Gp3_EUR_AF	1000Gp3_AMR_AC	1000Gp3_AMR_AF	1000Gp3_EAS_AC	1000Gp3_EAS_AF	1000Gp3_SAS_AC	1000Gp3_SAS_AF	TWINSUK_AC	TWINSUK_AF	ALSPAC_AC	ALSPAC_AF	UK10K_AC	UK10K_AF	ESP6500_AA_AC	ESP6500_AA_AF	ESP6500_EA_AC	ESP6500_EA_AF	ExAC_AC	ExAC_AF	ExAC_Adj_AC	ExAC_Adj_AF	ExAC_AFR_AC	ExAC_AFR_AF	ExAC_AMR_AC	ExAC_AMR_AF	ExAC_EAS_AC	ExAC_EAS_AF	ExAC_FIN_AC	ExAC_FIN_AF	ExAC_NFE_AC	ExAC_NFE_AF	ExAC_SAS_AC	ExAC_SAS_AF	ExAC_nonTCGA_AC	ExAC_nonTCGA_AF	ExAC_nonTCGA_Adj_AC	ExAC_nonTCGA_Adj_AF	ExAC_nonTCGA_AFR_AC	ExAC_nonTCGA_AFR_AF	ExAC_nonTCGA_AMR_AC	ExAC_nonTCGA_AMR_AF	ExAC_nonTCGA_EAS_AC	ExAC_nonTCGA_EAS_AF	ExAC_nonTCGA_FIN_AC	ExAC_nonTCGA_FIN_AF	ExAC_nonTCGA_NFE_AC	ExAC_nonTCGA_NFE_AF	ExAC_nonTCGA_SAS_AC	ExAC_nonTCGA_SAS_AF	ExAC_nonpsych_AC	ExAC_nonpsych_AF	ExAC_nonpsych_Adj_AC	ExAC_nonpsych_Adj_AF	ExAC_nonpsych_AFR_AC	ExAC_nonpsych_AFR_AF	ExAC_nonpsych_AMR_AC	ExAC_nonpsych_AMR_AF	ExAC_nonpsych_EAS_AC	ExAC_nonpsych_EAS_AF	ExAC_nonpsych_FIN_AC	ExAC_nonpsych_FIN_AF	ExAC_nonpsych_NFE_AC	ExAC_nonpsych_NFE_AF	ExAC_nonpsych_SAS_AC	ExAC_nonpsych_SAS_AF	gnomAD_exomes_flag	gnomAD_exomes_AC	gnomAD_exomes_AN	gnomAD_exomes_AF	gnomAD_exomes_nhomalt	gnomAD_exomes_AFR_AC	gnomAD_exomes_AFR_AN	gnomAD_exomes_AFR_AF	gnomAD_exomes_AFR_nhomalt	gnomAD_exomes_AMR_AC	gnomAD_exomes_AMR_AN	gnomAD_exomes_AMR_AF	gnomAD_exomes_AMR_nhomalt	gnomAD_exomes_ASJ_AC	gnomAD_exomes_ASJ_AN	gnomAD_exomes_ASJ_AF	gnomAD_exomes_ASJ_nhomalt	gnomAD_exomes_EAS_AC	gnomAD_exomes_EAS_AN	gnomAD_exomes_EAS_AF	gnomAD_exomes_EAS_nhomalt	gnomAD_exomes_FIN_AC	gnomAD_exomes_FIN_AN	gnomAD_exomes_FIN_AF	gnomAD_exomes_FIN_nhomalt	gnomAD_exomes_NFE_AC	gnomAD_exomes_NFE_AN	gnomAD_exomes_NFE_AF	gnomAD_exomes_NFE_nhomalt	gnomAD_exomes_SAS_AC	gnomAD_exomes_SAS_AN	gnomAD_exomes_SAS_AF	gnomAD_exomes_SAS_nhomalt	gnomAD_exomes_POPMAX_AC	gnomAD_exomes_POPMAX_AN	gnomAD_exomes_POPMAX_AF	gnomAD_exomes_POPMAX_nhomalt	gnomAD_exomes_controls_AC	gnomAD_exomes_controls_AN	gnomAD_exomes_controls_AF	gnomAD_exomes_controls_nhomalt	gnomAD_exomes_controls_AFR_AC	gnomAD_exomes_controls_AFR_AN	gnomAD_exomes_controls_AFR_AF	gnomAD_exomes_controls_AFR_nhomalt	gnomAD_exomes_controls_AMR_AC	gnomAD_exomes_controls_AMR_AN	gnomAD_exomes_controls_AMR_AF	gnomAD_exomes_controls_AMR_nhomalt	gnomAD_exomes_controls_ASJ_AC	gnomAD_exomes_controls_ASJ_AN	gnomAD_exomes_controls_ASJ_AF	gnomAD_exomes_controls_ASJ_nhomalt	gnomAD_exomes_controls_EAS_AC	gnomAD_exomes_controls_EAS_AN	gnomAD_exomes_controls_EAS_AF	gnomAD_exomes_controls_EAS_nhomalt	gnomAD_exomes_controls_FIN_AC	gnomAD_exomes_controls_FIN_AN	gnomAD_exomes_controls_FIN_AF	gnomAD_exomes_controls_FIN_nhomalt	gnomAD_exomes_controls_NFE_AC	gnomAD_exomes_controls_NFE_AN	gnomAD_exomes_controls_NFE_AF	gnomAD_exomes_controls_NFE_nhomalt	gnomAD_exomes_controls_SAS_AC	gnomAD_exomes_controls_SAS_AN	gnomAD_exomes_controls_SAS_AF	gnomAD_exomes_controls_SAS_nhomalt	gnomAD_exomes_controls_POPMAX_AC	gnomAD_exomes_controls_POPMAX_AN	gnomAD_exomes_controls_POPMAX_AF	gnomAD_exomes_controls_POPMAX_nhomalt	gnomAD_genomes_flag	gnomAD_genomes_AC	gnomAD_genomes_AN	gnomAD_genomes_AF	gnomAD_genomes_nhomalt	gnomAD_genomes_AFR_AC	gnomAD_genomes_AFR_AN	gnomAD_genomes_AFR_AF	gnomAD_genomes_AFR_nhomalt	gnomAD_genomes_AMR_AC	gnomAD_genomes_AMR_AN	gnomAD_genomes_AMR_AF	gnomAD_genomes_AMR_nhomalt	gnomAD_genomes_ASJ_AC	gnomAD_genomes_ASJ_AN	gnomAD_genomes_ASJ_AF	gnomAD_genomes_ASJ_nhomalt	gnomAD_genomes_EAS_AC	gnomAD_genomes_EAS_AN	gnomAD_genomes_EAS_AF	gnomAD_genomes_EAS_nhomalt	gnomAD_genomes_FIN_AC	gnomAD_genomes_FIN_AN	gnomAD_genomes_FIN_AF	gnomAD_genomes_FIN_nhomalt	gnomAD_genomes_NFE_AC	gnomAD_genomes_NFE_AN	gnomAD_genomes_NFE_AF	gnomAD_genomes_NFE_nhomalt	gnomAD_genomes_AMI_AC	gnomAD_genomes_AMI_AN	gnomAD_genomes_AMI_AF	gnomAD_genomes_AMI_nhomalt	gnomAD_genomes_SAS_AC	gnomAD_genomes_SAS_AN	gnomAD_genomes_SAS_AF	gnomAD_genomes_SAS_nhomalt	gnomAD_genomes_POPMAX_AC	gnomAD_genomes_POPMAX_AN	gnomAD_genomes_POPMAX_AF	gnomAD_genomes_POPMAX_nhomalt	clinvar_id	clinvar_clnsig	clinvar_trait	clinvar_review	clinvar_hgvs	clinvar_var_source	clinvar_MedGen_id	clinvar_OMIM_id	clinvar_Orphanet_id	Interpro_domain	GTEx_V8_gene	GTEx_V8_tissueGeuvadis_eQTL_target_gene";
		Map<String, Integer> headerMap = AnnotationSourceTSV.getHeaderNameAndPositions("chr", header);
		assertEquals(1, headerMap.size());
        assertTrue(headerMap.containsKey("chr"));
        assertTrue(headerMap.containsValue(0));
		
		headerMap = AnnotationSourceTSV.getHeaderNameAndPositions("chr,genename", header);
		assertEquals(2, headerMap.size());
        assertTrue(headerMap.containsKey("chr"));
        assertTrue(headerMap.containsValue(0));
        assertTrue(headerMap.containsKey("genename"));
		assertEquals(Integer.valueOf(12), headerMap.get("genename"));
	}
	 
	 
}
