package org.qcmg.qbasepileup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class QBasePileupUtilTest {
	
	@Test
	public void testGetFullChromosome() {
		
		assertEquals("chr10", QBasePileupUtil.getFullChromosome("10"));
		assertEquals("chr10", QBasePileupUtil.getFullChromosome("chr10"));
	}
	
	@Test
	public void testAddChromosomeReference() {
		
		assertTrue(QBasePileupUtil.addChromosomeReference("10"));		
		assertFalse(QBasePileupUtil.addChromosomeReference("23"));		
		assertFalse(QBasePileupUtil.addChromosomeReference("99"));		
		assertFalse(QBasePileupUtil.addChromosomeReference("100"));		
		assertTrue(QBasePileupUtil.addChromosomeReference("1"));
		assertTrue(QBasePileupUtil.addChromosomeReference("Y"));
		assertTrue(QBasePileupUtil.addChromosomeReference("X"));
		assertTrue(QBasePileupUtil.addChromosomeReference("M"));
		assertTrue(QBasePileupUtil.addChromosomeReference("MT"));
		assertFalse(QBasePileupUtil.addChromosomeReference("MTT"));
		assertFalse(QBasePileupUtil.addChromosomeReference("GL123"));
		assertFalse(QBasePileupUtil.addChromosomeReference("chr10"));
	}
	
	@Test
	public void testParseDCCHeaderVersionOld() throws QBasePileupException {
		List<String> headers = new ArrayList<String>();
		String h = "analysis_id\tcontrol_sample_id\tvariation_id\tvariation_type\tchromosome\tchromosome_start\tchromosome_end\tchromosome_strand\trefsnp_allele\trefsnp_strand\treference_genome_allele\tcontrol_genotype\ttumour_genotype\texpressed_allele\tquality_score\tprobability\tread_count\tis_annotated\tvalidation_status\tvalidation_platform\txref_ensembl_var_id\tnote\tQCMGflag\tND\tTD\tNNS\tFlankSeq\tMutation";
		headers.add(h);
		
		int[] cols = QBasePileupUtil.parseDCCHeader(headers);
		assertEquals(22, cols[0]);
		assertEquals(23, cols[1]);
		assertEquals(24, cols[2]);
		assertEquals(10, cols[3]);
		assertEquals(12, cols[4]);
	}
	
	@Test(expected=QBasePileupException.class)
	public void testParseDCCHeaderVersionOldThrowsException() throws QBasePileupException {
		List<String> headers = new ArrayList<String>();
		String h = "analysis_id\tcontrol_sample_id\tvariation_id\tvariation_type\tchromosome\tchromosome_start\tchromosome_end\tchromosome_strand\trefsnp_allele\trefsnp_strand\treference_genome_allele\tcontrol_genotype\ttumour_genotype\texpressed_allele\tquality_score\tprobability\tread_count\tis_annotated\tvalidation_status\tvalidation_platform\txref_ensembl_var_id\tnote\tQCtypoag\tND\tTD\tNNS\tFlankSeq\tMutation";
		headers.add(h);		
		QBasePileupUtil.parseDCCHeader(headers);
	}
	
	@Test
	public void testParseDCCHeaderVersionNew() throws QBasePileupException {
		List<String> headers = new ArrayList<String>();
		String h = "analysis_id\tanalyzed_sample_id\tmutation_id\tmutation_type\tchromosome\tchromosome_start\tchromosome_end\tchromosome_strand\trefsnp_allele\trefsnp_strand\treference_genome_allele\tcontrol_genotype\ttumour_genotype	mutation\texpressed_allele\tquality_score\tprobability\tread_count\tis_annotated\tverification_status\tverification_platform\txref_ensembl_var_id\tnote\tQCMGflag\tND\tTD\tNNS\tFlankSeq";
		headers.add(h);
		int[] cols = QBasePileupUtil.parseDCCHeader(headers);
		assertEquals(23, cols[0]);
		assertEquals(24, cols[1]);
		assertEquals(25, cols[2]);
		assertEquals(10, cols[3]);
		assertEquals(12, cols[4]);
	}
	
	@Test(expected=QBasePileupException.class)
	public void testParseDCCHeaderVersionNewThrowsException() throws QBasePileupException {
		List<String> headers = new ArrayList<String>();
		String h = "analysis_id\tanalyzed_sample_id\tmutation_id\tmutation_type\tchromosome\tchromosome_start\tchromosome_end\tchromosome_strand\trefsnp_allele\trefsnp_strand\treference_genome_allele\tcontrol_genotype\ttumour_genotype	mutation\texpressed_allele\tquality_score\tprobability\tread_count\tis_annotated\tverification_status\tverification_platform\txref_ensembl_var_id\tnote\tQCMGflag\tnn\tTD\tNNS\tFlankSeq";
		headers.add(h);		
		QBasePileupUtil.parseDCCHeader(headers);
	}
	

}
