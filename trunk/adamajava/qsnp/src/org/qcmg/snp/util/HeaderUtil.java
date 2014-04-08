/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp.util;

public class HeaderUtil {
	
	private static final String DCC_SOMATIC_0_6_C = "analysis_id\tanalyzed_sample_id\tmutation_id\tmutation_type\tchromosome\t" +
			"chromosome_start\tchromosome_end\tchromosome_strand\trefsnp_allele\trefsnp_strand\treference_genome_allele\tcontrol_genotype\t" +
			"tumour_genotype\tmutation\texpressed_allele\tquality_score\tprobability\tread_count\tis_annotated\tverification_status\t" +
			"verification_platform\txref_ensembl_var_id\tnote";
	
	
	private static final String DCC_GERMLINE_0_6_C = "analysis_id\tanalyzed_sample_id\tvariant_id\tvariant_type\tchromosome\t" +
			"chromosome_start\tchromosome_end\tchromosome_strand\trefsnp_allele\trefsnp_strand\treference_genome_allele\tcontrol_genotype\t" +
			"tumour_genotype\texpressed_allele\tquality_score\tprobability\tread_count\tis_annotated\tverification_status\t" +
			"verification_platform\txref_ensembl_var_id\tnote";

	
	private static final String FLAG_ND_TD = "\tQCMGflag\tND\tTD";
	
	private static final String SOMATIC_QCMG_FIELDS = FLAG_ND_TD + "\tNNS\tFlankSeq\n";
	private static final String GERMLINE_QCMG_FIELDS = FLAG_ND_TD + "\tNNS\tFlankSeq\tMutation\n";
	
	
	public static final String DCC_GERMLINE_HEADER = DCC_GERMLINE_0_6_C + GERMLINE_QCMG_FIELDS;
	public static final String DCC_SOMATIC_HEADER = DCC_SOMATIC_0_6_C + SOMATIC_QCMG_FIELDS;
	
	public static final String DCC_GERMLINE_HEADER_MINIMAL = DCC_GERMLINE_0_6_C + FLAG_ND_TD + "\tMutation\n";
	public static final String DCC_SOMATIC_HEADER_MINIMAL = DCC_SOMATIC_0_6_C + FLAG_ND_TD + "\n";
	
	public static final String DCC_GERMLINE_HEADER_NNS = DCC_GERMLINE_0_6_C + FLAG_ND_TD + "\tNNS\tMutation\n";
	public static final String DCC_SOMATIC_HEADER_NNS = DCC_SOMATIC_0_6_C + FLAG_ND_TD + "\tNNS\n";
	
}
