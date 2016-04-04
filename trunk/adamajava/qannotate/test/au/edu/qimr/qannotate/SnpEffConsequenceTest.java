package au.edu.qimr.qannotate;

import org.junit.Test;

import au.edu.qimr.qannotate.utils.SnpEffConsequence;

public class SnpEffConsequenceTest {
	
	@Test
	public void getWorstCaseConsequense() {
		String eff = "EFF=upstream_gene_variant(MODIFIER||851|||DDX11L1|processed_transcript|NON_CODING|ENST00000456328||1),upstream_gene_variant(MODIFIER||854|||DDX11L1|transcribed_unprocessed_pseudogene|NON_CODING|ENST00000515242||1),upstream_gene_variant(MODIFIER||856|||DDX11L1|transcribed_unprocessed_pseudogene|NON_CODING|ENST00000518655||1),upstream_gene_variant(MODIFIER||992|||DDX11L1|transcribed_unprocessed_pseudogene|NON_CODING|ENST00000450305||1),downstream_gene_variant(MODIFIER||3345|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000423562||1),downstream_gene_variant(MODIFIER||3345|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000438504||1),downstream_gene_variant(MODIFIER||3345|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000541675||1),downstream_gene_variant(MODIFIER||3386|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000488147||1),downstream_gene_variant(MODIFIER||3393|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000538476||1),intergenic_region(MODIFIER||||||||||1)";
		String cons = SnpEffConsequence.getWorstCaseConsequence(eff.split(","));
		System.out.println("cons: " + cons);
		
		eff = "EFF=upstream_gene_variant(MODIFIER||942|||DDX11L1|processed_transcript|NON_CODING|ENST00000456328||1),upstream_gene_variant(MODIFIER||945|||DDX11L1|transcribed_unprocessed_pseudogene|NON_CODING|ENST00000515242||1),upstream_gene_variant(MODIFIER||947|||DDX11L1|transcribed_unprocessed_pseudogene|NON_CODING|ENST00000518655||1),upstream_gene_variant(MODIFIER||1083|||DDX11L1|transcribed_unprocessed_pseudogene|NON_CODING|ENST00000450305||1),downstream_gene_variant(MODIFIER||3436|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000423562||1),downstream_gene_variant(MODIFIER||3436|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000438504||1),downstream_gene_variant(MODIFIER||3436|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000541675||1),downstream_gene_variant(MODIFIER||3477|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000488147||1),downstream_gene_variant(MODIFIER||3484|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000538476||1),intergenic_region(MODIFIER||||||||||1)";
		cons = SnpEffConsequence.getWorstCaseConsequence(eff.split(","));
		System.out.println("cons: " + cons);
		
		eff = "EFF=upstream_gene_variant(MODIFIER||941|||RP4-669L17.8|unprocessed_pseudogene|NON_CODING|ENST00000514436||1),upstream_gene_variant(MODIFIER||4629|||RP4-669L17.10|lincRNA|NON_CODING|ENST00000431812||1),downstream_gene_variant(MODIFIER||4802|||RP4-669L17.10|lincRNA|NON_CODING|ENST00000608420||1),downstream_gene_variant(MODIFIER||4099|||RP4-669L17.10|lincRNA|NON_CODING|ENST00000432964||1),downstream_gene_variant(MODIFIER||3058|||RP4-669L17.10|lincRNA|NON_CODING|ENST00000601486||1),downstream_gene_variant(MODIFIER||2952|||RP4-669L17.10|lincRNA|NON_CODING|ENST00000599771||1),downstream_gene_variant(MODIFIER||4276|||CICP7|processed_pseudogene|NON_CODING|ENST00000432723||1),downstream_gene_variant(MODIFIER||694|||RP4-669L17.10|lincRNA|NON_CODING|ENST00000423728||1),downstream_gene_variant(MODIFIER||282|||RP4-669L17.10|lincRNA|NON_CODING|ENST00000440038||1),downstream_gene_variant(MODIFIER||200|||RP4-669L17.10|lincRNA|NON_CODING|ENST00000419160||1),TF_binding_site_variant[MA0139.1:CTCF](MODIFIER||||||||||1),intron_variant(MODIFIER|||n.151+2927C>A||RP4-669L17.10|lincRNA|NON_CODING|ENST00000455464|1|1),intron_variant(MODIFIER|||n.258+810C>A||RP4-669L17.10|lincRNA|NON_CODING|ENST00000601814|2|1),non_coding_exon_variant(MODIFIER|||n.400C>A||RP4-669L17.10|lincRNA|NON_CODING|ENST00000425496|1|1)";
		cons = SnpEffConsequence.getWorstCaseConsequence(eff.split(","));
		System.out.println("cons: " + cons);
	}

}
