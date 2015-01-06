/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * 
 */
package au.edu.qimr.qannotate.utils;


public enum SnpEffConsequence {
	
	EXON_DELETED("exon_loss_variant",100),
	UTR_3_PRIME("3_prime_UTR_variant",100),
	UTR_5_PRIME("5_prime_UTR_variant",100),
	CDS("coding_sequence_variant",100),
	INTERGENIC_CONSERVED("conserved_intergenic_variant",100),
	INTRON_CONSERVED("conserved_intron_variant",100),
	DOWNSTREAM("downstream_gene_variant",100),
	EXON("exon_variant",100),
	GENE("gene_variant",100),
	INTERGENIC("intergenic_region",100),
	INTRAGENIC("intragenic_variant",100),
	INTRON("intron_variant",100),
	MICRO_RNA("miRNA",100),
	REGULATION("regulatory_region_variant",100),
	TRANSCRIPT("transcript_variant",100),
	UPSTREAM("upstream_gene_variant",100),
	UTR_3_DELETED("3_prime_UTR_truncation_exon_loss",100),
	UTR_5_DELETED("5_prime_UTR_truncation_exon_loss_variant",100),
	START_GAINED("5_prime_UTR_premature_start_codon_gain_variant",5),
	NON_SYNONYMOUS_START("initiator_codon_variant",5),
	SYNONYMOUS_START("start_retained",5),
	NON_SYNONYMOUS_STOP("stop_retained_variant",5),
	SYNONYMOUS_STOP("stop_retained_variant",5),
	SYNONYMOUS_CODING("synonymous_variant",5),
	CODON_CHANGE("coding_sequence_variant",4),
	CODON_CHANGE_PLUS_CODON_DELETION("disruptive_inframe_deletion",4),
	CODON_CHANGE_PLUS_CODON_INSERTION("disruptive_inframe_insertion",4),
	CODON_DELETION("inframe_deletion",4),
	CODON_INSERTION("inframe_insertion",4),
	NON_SYNONYMOUS_CODING("missense_variant",4),
	SPLICE_SITE_BRANCH_U12("splice_region_variant",4),
	SPLICE_SITE_REGION("splice_region_variant",4),
	SPLICE_SITE_BRANCH("splice_region_variant",4),
	RARE_AMINO_ACID("rare_amino_acid_variant",3),
	START_LOST("start_lost",3),
	STOP_LOST("stop_lost",3),
	SPLICE_SITE_ACCEPTOR("splice_acceptor_variant",2),
	SPLICE_SITE_DONOR("splice_donor_variant",2),
	FRAME_SHIFT("frameshift_variant",1),
	STOP_GAINED("stop_gained",1);
	

	private String ontologName;
	private int snpRank;


	private SnpEffConsequence(String name,   int snpRank ) {
		this.ontologName = name;
		this.snpRank = snpRank;
	 
	}
	
	public static String getClassicName(String name ) {
		
		if (null != name) {
			for (final SnpEffConsequence dcEnum : values())  
				if (dcEnum.ontologName.equals(name) )  
					return dcEnum.name();
		}
		return null;
	}
	
	public static boolean isConsequence(String name) {
		if (null != name) 
			for (final SnpEffConsequence dcEnum : values())  
				if ( dcEnum.snpRank < 100 &&  (dcEnum.ontologName.equals(name)  || dcEnum.name().equals(name)) )
					return true;

		return false;
	}
	
	public static String getWorstCaseConsequence( String ...strings ) {
		
		// we haven't defined all the snp types here - so if its a type snp, set to SNP
		
		SnpEffConsequence worstConsequence = null;
		String worstConsequencestr = null;
		if (null == strings || strings.length == 0) return null;
		
		for (final String consequence : strings) {
			for (final SnpEffConsequence dcEnum : values())  
				if (  dcEnum.ontologName.equals(consequence.subSequence(0, consequence.indexOf("("))) ) 
					if (null == worstConsequence || dcEnum.snpRank < worstConsequence.snpRank)  {
						worstConsequence = dcEnum;
						worstConsequencestr = consequence;
					}			 
		}
		
		return null != worstConsequence ? worstConsequencestr : null;
	}
	

}

