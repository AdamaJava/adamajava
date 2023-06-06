package au.edu.qimr.qannotate.nanno;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import au.edu.qimr.qannotate.nanno.AnnotationSourceSnpEffVCF;

public class AnnotationSourceSnpEffVCFTest {
	
	@Test
	public void extractFieldsFromInfoField() {
		String info = "AC=2;AF=1.00;AN=2;BaseQRankSum=0.00;DP=46;ExcessHet=0.0000;FS=2.561;MLEAC=2;MLEAF=1.00;MQ=60.00;MQRankSum=0.00;QD=17.47;ReadPosRankSum=0.00;SOR=0.521;ANN=A|3_prime_UTR_variant|MODIFIER|DYNLT5|ENSG00000152760|transcript|ENST00000282670.7|protein_coding|5/5|c.*115G>A|||||115|,A|3_prime_UTR_variant|MODIFIER|DYNLT5|ENSG00000152760|transcript|ENST00000528352.1|nonsense_mediated_decay|7/7|n.*554G>A|||||6971|,A|downstream_gene_variant|MODIFIER|DYNLT5|ENSG00000152760|transcript|ENST00000448074.1|processed_transcript||n.*1991G>A|||||1991|,A|non_coding_transcript_exon_variant|MODIFIER|DYNLT5|ENSG00000152760|transcript|ENST00000528352.1|nonsense_mediated_decay|7/7|n.*554G>A||||||,A|non_coding_transcript_exon_variant|MODIFIER|DYNLT5|ENSG00000152760|transcript|ENST00000489510.1|retained_intron|2/2|n.568G>A||||||";
		assertEquals("effect=3_prime_UTR_variant", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, Arrays.asList("effect"), "", "A"));
		assertEquals("annotation=3_prime_UTR_variant", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, Arrays.asList("annotation"), "", "A"));
		assertEquals("cdna_position=", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, Arrays.asList("cdna_position"), "", "A"));
		assertEquals("cds_position=", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, Arrays.asList("cds_position"), "", "A"));
		assertEquals("distance_to_feature=115", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, Arrays.asList("distance_to_feature"), "", "A"));
		assertEquals("feature_type=transcript", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, Arrays.asList("feature_type"), "", "A"));
		assertEquals("rank=5/5", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, Arrays.asList("rank"), "", "A"));
		assertEquals("hgvs.c=c.*115G>A", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, Arrays.asList("hgvs.c"), "", "A"));
	}
	
	@Test
	public void extractFieldsFromInfoFieldWrongAlt() {
		String info = "AC=2;AF=1.00;AN=2;BaseQRankSum=0.00;DP=46;ExcessHet=0.0000;FS=2.561;MLEAC=2;MLEAF=1.00;MQ=60.00;MQRankSum=0.00;QD=17.47;ReadPosRankSum=0.00;SOR=0.521;ANN=A|3_prime_UTR_variant|MODIFIER|DYNLT5|ENSG00000152760|transcript|ENST00000282670.7|protein_coding|5/5|c.*115G>A|||||115|,A|3_prime_UTR_variant|MODIFIER|DYNLT5|ENSG00000152760|transcript|ENST00000528352.1|nonsense_mediated_decay|7/7|n.*554G>A|||||6971|,A|downstream_gene_variant|MODIFIER|DYNLT5|ENSG00000152760|transcript|ENST00000448074.1|processed_transcript||n.*1991G>A|||||1991|,A|non_coding_transcript_exon_variant|MODIFIER|DYNLT5|ENSG00000152760|transcript|ENST00000528352.1|nonsense_mediated_decay|7/7|n.*554G>A||||||,A|non_coding_transcript_exon_variant|MODIFIER|DYNLT5|ENSG00000152760|transcript|ENST00000489510.1|retained_intron|2/2|n.568G>A||||||";
		assertEquals("", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, Arrays.asList("effect"), "", "T"));
		assertEquals("effect=", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, Arrays.asList("effect"), "effect=", "T"));
		assertEquals("hgvs.c=", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, Arrays.asList("hgvs.c"), "hgvs.c=", "C"));
	}
	
	@Test
	public void getWorstConsequence() {
		String info = "AC=2;AF=1.00;AN=2;DP=43;ExcessHet=0.0000;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;QD=25.36;SOR=0.739;ANN=C|splice_region_variant&intron_variant|LOW|NOC2L|ENSG00000188976|transcript|ENST00000327044.7|protein_coding|8/18|c.888+4C>G||||||,C|splice_region_variant&intron_variant|LOW|NOC2L|ENSG00000188976|transcript|ENST00000477976.5|retained_intron|6/16|n.2335+4C>G||||||,C|downstream_gene_variant|MODIFIER|NOC2L|ENSG00000188976|transcript|ENST00000469563.1|retained_intron||n.*4468C>G|||||4468|,C|downstream_gene_variant|MODIFIER|NOC2L|ENSG00000188976|transcript|ENST00000487214.1|processed_transcript||n.*648C>G|||||648|";
		String alt = "C";
		assertEquals("C|splice_region_variant&intron_variant|LOW|NOC2L|ENSG00000188976|transcript|ENST00000327044.7|protein_coding|8/18|c.888+4C>G||||||", AnnotationSourceSnpEffVCF.getWorstConsequence(info, alt));
	}
	
	@Test
	public void extractFieldsFromInfoField2() {
		String info = "AC=2;AF=1.00;AN=2;DP=43;ExcessHet=0.0000;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;QD=25.36;SOR=0.739;ANN=C|splice_region_variant&intron_variant|LOW|NOC2L|ENSG00000188976|transcript|ENST00000327044.7|protein_coding|8/18|c.888+4C>G||||||,C|splice_region_variant&intron_variant|LOW|NOC2L|ENSG00000188976|transcript|ENST00000477976.5|retained_intron|6/16|n.2335+4C>G||||||,C|downstream_gene_variant|MODIFIER|NOC2L|ENSG00000188976|transcript|ENST00000469563.1|retained_intron||n.*4468C>G|||||4468|,C|downstream_gene_variant|MODIFIER|NOC2L|ENSG00000188976|transcript|ENST00000487214.1|processed_transcript||n.*648C>G|||||648|";
		String alt = "C";
		
		assertEquals("cdna_position=", AnnotationSourceSnpEffVCF.extractFieldsFromInfoField(info, Arrays.asList("cdna_position"), ".", alt));
	}

}
