/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
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
	
	//decide whether DEL or Ins 
	CHROMOSOME_LARGE_DELETION("chromosome",50,"RNA"),   //???
	EXON_DELETED("exon_loss_variant",50,"RNA"),
//	FRAME_SHIFT("frameshift_variant",1,"Frame_Shift_DelORFrame_Shift_Ins(dependsonvarianttype)"),
	FRAME_SHIFT("frameshift_variant",1,"Frame_Shift_"),
	RARE_AMINO_ACID("rare_amino_acid_variant",3,"Missense_Mutation"),
	SPLICE_SITE_ACCEPTOR("splice_acceptor_variant",2,"Splice_Site"),
	SPLICE_SITE_DONOR("splice_donor_variant",2,"Splice_Site"),
	START_LOST("start_lost",3,"Translation_Start_Site"),
	STOP_GAINED("stop_gained",1,"Nonsense_Mutation"),
	STOP_LOST("stop_lost",3,"Nonstop_Mutation"),
//	UTR_3_DELETED("3_prime_UTR_truncation_exon_loss",70, "RNA"),   //???
//	UTR_5_DELETED("5_prime_UTR_truncation_exon_loss_variant",70,"RNA"), //???
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
	UPSTREAM("upstream_gene_variant",100,"5'Flank"),
	
	
		//below calssic Effect are guessing only
	UTR_3_DELETED("3_prime_UTR_truncation+exon_loss",5,"indel"), 
	UTR_5_DELETED("5_prime_UTR_truncation+exon_loss_variant",5,"indel"), 
	CODON_CHANGE_PLUS_CODON_DELETION_PLUS_SYNONYMOUS_CODING("disruptive_inframe_deletion+synonymous_variant",5,"indel"), 
	CODON_CHANGE_PLUS_CODON_INSERTION_PLUS_SYNONYMOUS_CODING("disruptive_inframe_insertion+synonymous_variant",5,"indel"), 
	EXON_DELETED_PLUS_FRAME_SHIFT_PLUS_START_LOST_PLUS_SYNONYMOUS_CODING("exon_loss_variant+frameshift_variant+start_lost+synonymous_variant",1,"indel"), 
	EXON_DELETED_PLUS_FRAME_SHIFT_PLUS_STOP_GAINED("exon_loss_variant+frameshift_variant+stop_gained",1,"indel"), 
	FRAME_SHIFT_PLUS_STOP_GAINED("frameshift_variant+stop_gained",1,"indel"),
	EXON_DELETED_PLUS_START_LOST_PLUS_SYNONYMOUS_CODING("exon_loss_variant+start_lost+synonymous_variant",1,"indel"), 
	EXON_DELETED_PLUS_START_LOST("exon_loss_variant+start_lost",1,"indel"), 
	EXON_DELETED_PLUS_STOP_GAINED_PLUS_SYNONYMOUS_CODING("exon_loss_variant+stop_gained+synonymous_variant",1,"indel"), 
	EXON_DELETED_PLUS_STOP_LOST("exon_loss_variant+stop_lost",1,"indel"), 
	EXON_DELETED_PLUS_STOP_LOST_PLUS_SYNONYMOUS_CODING("exon_loss_variant+stop_lost+synonymous_variant",1,"indel"), 
	EXON_DELETED_PLUS_SYNONYMOUS_CODING("exon_loss_variant+synonymous_variant",1,"indel"), 
	FRAME_SHIFT_PLUS_NON_SYNONYMOUS_CODING("frameshift_variant+missense_variant",1,"indel"), 
	FRAME_SHIFT_PLUS_START_LOST("frameshift_variant+start_lost",1,"indel"), 
	FRAME_SHIFT_PLUS_STOP_GAINED_PLUS_NON_SYNONYMOUS_CODING("frameshift_variant+stop_gained+missense_variant",1,"indel"), 
	FRAME_SHIFT_PLUS_STOP_LOST("frameshift_variant+stop_lost",1,"indel"), 
	FRAME_SHIFT_PLUS_STOP_LOST_PLUS_NON_SYNONYMOUS_STOP("frameshift_variant+stop_lost+stop_retained_variant",1,"indel"), 
	FRAME_SHIFT_PLUS_STOP_LOST_PLUS_SYNONYMOUS_CODING("frameshift_variant+stop_lost+synonymous_variant",1,"indel"), 
	FRAME_SHIFT_PLUS_NON_SYNONYMOUS_STOP("frameshift_variant+stop_retained_variant",1,"indel"), 
	FRAME_SHIFT_PLUS_SYNONYMOUS_CODING("frameshift_variant+synonymous_variant",1,"indel"), 
	STOP_LOST_PLUS_CODON_DELETION_PLUS_SYNONYMOUS_CODING("stop_lost+inframe_deletion+synonymous_variant",1,"indel"), 
	NON_SYNONYMOUS_CODING_PLUS_CODON_CHANGE_PLUS_CODON_DELETION("missense_variant+disruptive_inframe_deletion",5,"indel"), 
	STOP_GAINED_PLUS_NON_SYNONYMOUS_CODING_PLUS_CODON_CHANGE_PLUS_CODON_DELETION("stop_gained+missense_variant+disruptive_inframe_deletion",1,"indel"), 
	NON_SYNONYMOUS_CODING_PLUS_CODON_CHANGE_PLUS_CODON_INSERTION("missense_variant+disruptive_inframe_insertion",5,"indel"), 
	NON_SYNONYMOUS_CODING_PLUS_CODON_DELETION("missense_variant+inframe_deletion",5,"indel"), 
	STOP_LOST_PLUS_NON_SYNONYMOUS_CODING_PLUS_CODON_DELETION("stop_lost+missense_variant+inframe_deletion",1,"indel"), 
	NON_SYNONYMOUS_CODING_PLUS_CODON_INSERTION("missense_variant+inframe_insertion",5,"indel"), 
	STOP_GAINED_PLUS_NON_SYNONYMOUS_CODING_PLUS_CODON_INSERTION("stop_gained+missense_variant+inframe_insertion",1,"indel"), 
	START_LOST_PLUS_CODON_DELETION("start_lost+inframe_deletion",1,"indel"), 
	START_LOST_PLUS_CODON_DELETION_PLUS_SYNONYMOUS_START("start_lost+inframe_deletion+initiator_codon_variant+non_canonical_start_codon",1,"indel"), 
	START_LOST_PLUS_CODON_INSERTION("start_lost+inframe_insertion",1,"indel"), 
	START_LOST_PLUS_CODON_INSERTION_PLUS_NON_SYNONYMOUS_START("start_lost+inframe_insertion+initiator_codon_variant",1,"indel"), 
	START_LOST_PLUS_NON_SYNONYMOUS_START("start_lost+initiator_codon_variant",1,"indel"), 
	STOP_GAINED_PLUS_CODON_CHANGE_PLUS_CODON_INSERTION("stop_gained+disruptive_inframe_insertion",1,"indel"), 
	STOP_GAINED_PLUS_CODON_DELETION("stop_gained+inframe_deletion",1,"indel"), 
	STOP_GAINED_PLUS_CODON_INSERTION("stop_gained+inframe_insertion",1,"indel"), 
	STOP_GAINED_PLUS_START_LOST("stop_gained+start_lost",1,"indel"), 
	STOP_LOST_PLUS_CODON_DELETION("stop_lost+inframe_deletion",1,"indel"), 
 	CODON_DELETION_PLUS_SYNONYMOUS_CODING("inframe_deletion+synonymous_variant",5,"indel"),
 	STOP_GAINED_PLUS_NON_SYNONYMOUS_CODING("stop_gained+missense_variant",1,"indle"),
 	STOP_LOST_PLUS_NON_SYNONYMOUS_STOP("stop_lost+stop_retained_variant",1,"indle"),

 	//below require snpEff classic run
	EXON_NON_CODING("non_coding_exon_variant",100,"unknown"),  //should be "EXON"
	NEXT_PROT("sequence_feature",200,"unknown") ,
	MOTIF("TF_binding_site_variant", 200, "unknown");
	
//[MA0093.1:USF1](LOW||||||||||1)	
//	sequence_feature[compositionally_biased_region:Asp/Glu-rich__acidic_](LOW|||c.2144A>G|749|NOC2L|protein_coding|CODING|ENST00000327044|18|1)

	private static final QLogger logger = QLoggerFactory.getLogger(SnpEffConsequence.class);
	public static final String PROTEIN_CODING = "protein_coding";
	
	
	private String ontologName;
	private int snpRank;
	private String maf_calssification;
	
	
	public static final String HIGH_IMPACT = "HIGH";
	public static final String LOW_IMPACT = "LOW";
	public static final String MODERATE_IMPACT = "MODERATE";
	public static final String MODIFIER_IMPACT = "MODIFIER";


	private SnpEffConsequence(String name,   int snpRank , String maf) {
		this.ontologName = name;
		this.snpRank = snpRank;
		this.maf_calssification = maf;
	 
	}
	
	public static String getClassicName(String name ) {
		
		if (null != name) {
			String ontolog = (name.contains("["))? name.substring(0, name.indexOf("[")) : name;
			for (final SnpEffConsequence dcEnum : values())  
				if (dcEnum.ontologName.equals(ontolog) )  
					return dcEnum.name();
		}
		
		return null;
	}
	
	public static String getMafClassification(String name ) {
		if (null != name) {
			String ontolog = (name.contains("["))? name.substring(0, name.indexOf("[")) : name;
		
			for (final SnpEffConsequence dcEnum : values())  
				if (dcEnum.ontologName.equals(ontolog) )  
					return dcEnum.maf_calssification;
		}
		
		return null;
	}
	
	public static int getConsequenceRank(String name ) {
		if (null != name) {
			String ontolog = (name.contains("["))? name.substring(0, name.indexOf("[")) : name;
			for (final SnpEffConsequence dcEnum : values())  
				if (dcEnum.ontologName.equals(ontolog) )  
					return dcEnum.snpRank;
		}
		return -1;
	}	
	
	/**
	 * 
	 * @param strings of snpEff annotation
	 * @return protein_coding first; then ranking; then length
	 */
	public static String getWorstCaseConsequence( String ...strings ){
		if (null == strings || strings.length == 0) return null;
		
		// we haven't defined all the snp types here - so if its a type snp, set to SNP
		
		SnpEffConsequence worstConsequence = null;
		String worstConsequencestr = null;
		int worstConsequencelen = 0;
		
		for (final String consequence : strings) {
			String ontolog = consequence.substring(0, consequence.indexOf("("));
			ontolog = (ontolog.contains("["))? ontolog.substring(0, ontolog.indexOf("[")) : ontolog;
			String[] conse = consequence.split(Constants.BAR_STRING );
			
			//here Integer.getInteger(conse[4]) return null
			int lconse = (StringUtils.isNumeric(conse[4])) ? Integer.parseInt(conse[4]) : 0; 
			boolean onList = false;
			for (final SnpEffConsequence dcEnum : values()) { 
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

