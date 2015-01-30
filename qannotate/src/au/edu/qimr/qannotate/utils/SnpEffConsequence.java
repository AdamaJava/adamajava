/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * 
 */
package au.edu.qimr.qannotate.utils;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.log.*;



/*
 * Consequence are happened at protein_coding with impact HIGH, MODERATE or LOW
 */
public enum SnpEffConsequence {
	

	 
	CHROMOSOME_LARGE_DELETION("chromosome",50,"RNA"),   //???
	EXON_DELETED("exon_loss_variant",50,"RNA"),
	FRAME_SHIFT("frameshift_variant",1,"Frame_Shift_DelORFrame_Shift_Ins(dependsonvarianttype)"),
	RARE_AMINO_ACID("rare_amino_acid_variant",3,"Missense_Mutation"),
	SPLICE_SITE_ACCEPTOR("splice_acceptor_variant",2,"Splice_Site"),
	SPLICE_SITE_DONOR("splice_donor_variant",2,"Splice_Site"),
	START_LOST("start_lost",3,"Translation_Start_Site"),
	STOP_GAINED("stop_gained",1,"Nonsense_Mutation"),
	STOP_LOST("stop_lost",3,"Nonstop_Mutation"),
	UTR_3_DELETED("3_prime_UTR_truncation_exon_loss",70, "RNA"),   //???
	UTR_5_DELETED("5_prime_UTR_truncation_exon_loss_variant",70,"RNA"), //???
	CODON_CHANGE("coding_sequence_variant",4,"Missense_Mutation"),
	CODON_CHANGE_PLUS_CODON_DELETION("disruptive_inframe_deletion",4,"In_Frame_Del"),
	CODON_CHANGE_PLUS_CODON_INSERTION("disruptive_inframe_insertion",4,"In_Frame_Ins"),
	CODON_DELETION("inframe_deletion",4,"In_Frame_Del"),
	CODON_INSERTION("inframe_insertion",4,"In_Frame_Ins"),
	NON_SYNONYMOUS_CODING("missense_variant",4,"Missense_Mutation"),
	SPLICE_SITE_BRANCH_U12("splice_region_variant",4,"RNA"),
	START_GAINED("5_prime_UTR_premature_start_codon_gain_variant",5,"Translation_Start_Site"),  //???
	NON_SYNONYMOUS_START("initiator_codon_variant",5,"Missense_Mutation"),
	SPLICE_SITE_REGION("splice_region_variant",4,"RNA"),
	SPLICE_SITE_BRANCH("splice_region_variant",4,"RNA"),
	SYNONYMOUS_START("start_retained",5,"Translation_Start_Site"),
	NON_SYNONYMOUS_STOP("stop_retained_variant",5,"Missense_Mutation"),
	SYNONYMOUS_STOP("stop_retained_variant",5,"Silent"),
	SYNONYMOUS_CODING("synonymous_variant",5,"Silent"),
	UTR_3_PRIME("3_prime_UTR_variant",100,"3'UTR"),
	UTR_5_PRIME("5_prime_UTR_variant",100,"5'UTR"),
	CDS("coding_sequence_variant",100,"RNA"),
	INTERGENIC_CONSERVED("conserved_intergenic_variant",100,"Intron"),
	INTRON_CONSERVED("conserved_intron_variant",100,"Intron"),
	DOWNSTREAM("downstream_gene_variant",100,"3'Flank"),
	EXON("exon_variant",100,"RNA"),
	GENE("gene_variant",100,"RNA"),
	INTERGENIC("intergenic_region",100,"IGR"),
	INTRAGENIC("intragenic_variant",100,"RNA"),
	INTRON("intron_variant",100,"Intron"),
	MICRO_RNA("miRNA",100,"RNA"),
	REGULATION("regulatory_region_variant",100,"IGR"),
	TRANSCRIPT("transcript_variant",100,"RNA"),
	UPSTREAM("upstream_gene_variant",100,"5'Flank");
	
	
	private static final QLogger logger = QLoggerFactory.getLogger(SnpEffConsequence.class);
	public static String PROTEIN_CODING = "protein_coding";
	
	
	private String ontologName;
	private int snpRank;
	private String maf_calssification;
	
	
	public static String HIGH_IMPACT = "HIGH";
	public static String LOW_IMPACT = "LOW";
	public static String MODERATE_IMPACT = "MODERATE";
	public static String MODIFIER_IMPACT = "MODIFIER";


	private SnpEffConsequence(String name,   int snpRank , String maf) {
		this.ontologName = name;
		this.snpRank = snpRank;
		this.maf_calssification = maf;
	 
	}
	
	public static String getClassicName(String name ) {
		
		if (null != name) {
			for (final SnpEffConsequence dcEnum : values())  
				if (dcEnum.ontologName.equals(name) )  
					return dcEnum.name();
		}
		return null;
	}
	
	public static String getMafClassification(String name ) {
		if (null != name) {
			for (final SnpEffConsequence dcEnum : values())  
				if (dcEnum.ontologName.equals(name) )  
					return dcEnum.maf_calssification;
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
	
	/**
	 * 
	 * @param strings
	 * @return 
	 */
	
	/**
	 * 
	 * @param strings of snpEff annotation
	 * @return protein_coding first; then ranking; then length
	 */
	public static String getWorstCaseConsequence( String ...strings ){
		
		// we haven't defined all the snp types here - so if its a type snp, set to SNP
		
		SnpEffConsequence worstConsequence = null;
		String worstConsequencestr = null;
		int worstConsequencelen = 0;
		if (null == strings || strings.length == 0) return null;
		
		for (final String consequence : strings) {
			String ontolog = consequence.substring(0, consequence.indexOf("("));
			String[] conse = consequence.split(Constants.BAR_STRING );
			int lconse = (StringUtils.isNumeric(conse[6])) ? Integer.getInteger(conse[6]) : 0; 
			boolean onList = false;
			for (final SnpEffConsequence dcEnum : values())  
				if (  dcEnum.ontologName.equals(ontolog) ){ 
					onList = true;
					if (null == worstConsequence || dcEnum.snpRank < worstConsequence.snpRank ||
							(dcEnum.snpRank == worstConsequence.snpRank && worstConsequencelen < lconse) )  {
						worstConsequence = dcEnum;
						worstConsequencestr = consequence;
						worstConsequencelen = lconse;
					} 
					
					break;  //stop seeking in SnpEffConsequence elements
				}
			
			//print waring message if protein_coding annotation is not on the list
			if(!onList && conse[6].equalsIgnoreCase(PROTEIN_CODING))
				logger.error(PROTEIN_CODING + "annoation from snpEff is not on our consequence list: " + consequence);
			else if (!onList)
				logger.warn( "annoation from snpEff is not on our consequence list: " + consequence);
			
		}
				
		return null != worstConsequence ? worstConsequencestr : null;
	}
	
	
	 public static String getUndefinedConsequence( String ...strings ) {
		 final String[] annotations = new String[4];
		for (final String consequence : strings) {
			final String impact = consequence.substring(consequence.indexOf("(") + 1,consequence.indexOf("|" )).trim();
			if(impact.equalsIgnoreCase(SnpEffConsequence.HIGH_IMPACT))  
				annotations[0] = consequence;
			else if(impact.equalsIgnoreCase(SnpEffConsequence.MODERATE_IMPACT))  
				annotations[1] = consequence;
			else if(impact.equalsIgnoreCase(SnpEffConsequence.LOW_IMPACT))  
				annotations[2] = consequence;
			else
				annotations[3] = consequence;
		}
		 
		for(int i = 0; i < 4; i++)
			if(! StringUtils.isNullOrEmpty( annotations[i]  ))
				return annotations[i];
		 
		  return null;
	 }

}

