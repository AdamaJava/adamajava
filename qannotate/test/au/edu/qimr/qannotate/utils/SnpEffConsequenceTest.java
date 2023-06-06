package au.edu.qimr.qannotate.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SnpEffConsequenceTest {
	
	@Test
	public void getUndefinedConsequence() {
		
		String anno = "A|upstream_gene_variant|MODIFIER|ENSG00000224969|ENSG00000224969|transcript|ENST00000458555.1|pseudogene||n.-662C>T|||||662|,A|intron_variant|MODIFIER|ISG15|ENSG00000187608|transcript|ENST00000624697.4|protein_coding|2/2|c.-21-129G>A||||||,A|intron_variant|MODIFIER|ISG15|ENSG00000187608|transcript|ENST00000624652.1|protein_coding|2/2|c.-21-129G>A||||||WARNING_TRANSCRIPT_INCOMPLETE,A|intron_variant|MODIFIER|ISG15|ENSG00000187608|transcript|ENST00000649529.1|protein_coding|1/1|c.4-129G>A||||||";
		String [] annoArray = anno.split(",");
		assertEquals(4, annoArray.length);
		assertEquals("A|intron_variant|MODIFIER|ISG15|ENSG00000187608|transcript|ENST00000649529.1|protein_coding|1/1|c.4-129G>A||||||", SnpEffConsequence.getUndefinedConsequence(annoArray));
	}

}
