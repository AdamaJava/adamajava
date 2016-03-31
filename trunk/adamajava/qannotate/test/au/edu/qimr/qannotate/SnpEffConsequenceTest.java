package au.edu.qimr.qannotate;

import org.junit.Test;

import au.edu.qimr.qannotate.utils.SnpEffConsequence;

public class SnpEffConsequenceTest {
	
	@Test
	public void getWorstCaseConsequense() {
		String eff = "EFF=upstream_gene_variant(MODIFIER||851|||DDX11L1|processed_transcript|NON_CODING|ENST00000456328||1),upstream_gene_variant(MODIFIER||854|||DDX11L1|transcribed_unprocessed_pseudogene|NON_CODING|ENST00000515242||1),upstream_gene_variant(MODIFIER||856|||DDX11L1|transcribed_unprocessed_pseudogene|NON_CODING|ENST00000518655||1),upstream_gene_variant(MODIFIER||992|||DDX11L1|transcribed_unprocessed_pseudogene|NON_CODING|ENST00000450305||1),downstream_gene_variant(MODIFIER||3345|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000423562||1),downstream_gene_variant(MODIFIER||3345|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000438504||1),downstream_gene_variant(MODIFIER||3345|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000541675||1),downstream_gene_variant(MODIFIER||3386|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000488147||1),downstream_gene_variant(MODIFIER||3393|||WASH7P|unprocessed_pseudogene|NON_CODING|ENST00000538476||1),intergenic_region(MODIFIER||||||||||1)";
		String cons = SnpEffConsequence.getWorstCaseConsequence(eff.split(","));
		System.out.println("cons: " + cons);
	}

}
