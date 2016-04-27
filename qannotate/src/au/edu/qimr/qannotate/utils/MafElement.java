/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * 
 */
package au.edu.qimr.qannotate.utils;

import org.qcmg.common.util.IndelUtils;

import au.edu.qimr.qannotate.options.Vcf2mafOptions;

public enum MafElement {
	Hugo_Symbol( 1,  SnpEffMafRecord.Unknown), 
	Entrez_Gene_Id( 2, SnpEffMafRecord.Zero), //??Entrez_Gene_Id Entrez gene ID (an integer). If no gene exists within 3kb enter "0".
	Center(3, Vcf2mafOptions.default_center), //Center"
	NCBI_Build(4, "37"), //NCBI_Build
	Chromosome(5, SnpEffMafRecord.Null), //Chromosome is compulsary with correct value
	Start_Position(6, SnpEffMafRecord.Null), //Start_Position
	End_Position(7, SnpEffMafRecord.Null), //End_Position
	Strand(8, "+"), //Strand
	Variant_Classification(9, SnpEffMafRecord.Unknown), //Variant_Classification =snpeff Impact
	Variant_Type(10,  IndelUtils.SVTYPE.UNKOWN.name()), //Variant_Type
	Reference_Allele(11,  SnpEffMafRecord.Null), //Reference_Allele
	Tumor_Seq_Allele1(12, SnpEffMafRecord.Null), //Tumor_Seq_Allele1
	Tumor_Seq_Allele2(13, SnpEffMafRecord.Null), //Tumor_Seq_Allele2
	dbSNP_RS(14,  "novel"), //dbSNP_RS
	dbSNP_Val_Status(15,  SnpEffMafRecord.Null), //dbSNP_Val_Status
	Tumor_Sample_Barcode(16,  SnpEffMafRecord.Unknown), //Tumor_Sample_Barcode, eg. ##tumourSample=ICGC-ABMJ-20120706-01
	Matched_Norm_Sample_Barcode(17, SnpEffMafRecord.Unknown), //Matched_Norm_Sample_Barcode eg. ##normalSample=ICGC-ABMP-20091203-10-ND
	Match_Norm_Seq_Allele1(18,SnpEffMafRecord.Null), //Match_Norm_Seq_Allele1
	Match_Norm_Seq_Allele2(19,SnpEffMafRecord.Null), //Match_Norm_Seq_Allele2
	Tumor_Validation_Allele1(20,  SnpEffMafRecord.Null), //Tumor_Validation_Allele1
	Tumor_Validation_Allele2(21,  SnpEffMafRecord.Null), //Tumor_Validation_Allele2
	Match_Norm_Validation_Allele1(22, SnpEffMafRecord.Null), //Match_Norm_Validation_Allele1
	Match_Norm_Validation_Allele2(23, SnpEffMafRecord.Null), //Match_Norm_Validation_Allele2
	Verification_Status(24, SnpEffMafRecord.Null ), //Verification_Status
	Validation_Status(25, SnpEffMafRecord.VALIDATION_STATUS.Untested.name()),//Validation_Status
	Mutation_Status(26, SnpEffMafRecord.MUTATION_STATUS.Unknown.name()), //Mutation_Status somatic/germline
	Sequencing_Phase(27,  SnpEffMafRecord.Null), //Sequencing_Phase
	Sequence_Source(28, SnpEffMafRecord.Unknown), //??Sequence_Source
	Validation_Method(29, SnpEffMafRecord.none), //Validation_Method NO. If Validation_Status = Untested then "SnpEffMafRecord.none" If Validation_Status = Valid or Invalid, then not "SnpEffMafRecord.none" (case insensitive)
	Score(30, SnpEffMafRecord.Null), //Score
	BAM_File(31,  SnpEffMafRecord.Null), //BAM_File
	Sequencer(32, SnpEffMafRecord.Unknown), //Sequencer eg. Illumina HiSeq, SOLID
	Tumor_Sample_UUID(33, SnpEffMafRecord.none), //Tumor_Sample_UUID
	Matched_Norm_Sample_UUID(34,  SnpEffMafRecord.none), //Matched_Norm_Sample_UUID
	QFlag(35, SnpEffMafRecord.Null),  //? QCMG_Flag, vcf filter column
	ND(36, SnpEffMafRecord.Null), //ND
	TD(37, SnpEffMafRecord.Null), //TD
	confidence(38, SnpEffMafRecord.Unknown), //"confidence"),
	Eff_Impact(39, SnpEffMafRecord.Unknown), //"Eff_Impact"),"consequence"),
	Consequnce_rank(40, SnpEffMafRecord.Zero), //A.M consequce rank
	novel_starts(41,  SnpEffMafRecord.Unknown), //"novel_starts"),
	Var_Plus_Flank(42,SnpEffMafRecord.Unknown), //"Var_Plus_Flank"), Cpg),
	Variant_AF(43, SnpEffMafRecord.Unknown), //"Variant_AF""GMAF"),
	Germ_Counts(44, SnpEffMafRecord.Null), //germ_counts
	t_depth(45, SnpEffMafRecord.Zero), //t_depth
	t_ref_count(46, SnpEffMafRecord.Zero), //t_ref_count
	t_alt_count(47, SnpEffMafRecord.Zero), //t_alt_count
	n_depth(48, SnpEffMafRecord.Zero), //n_depth
	n_ref_count(49, SnpEffMafRecord.Zero), //n_ref_count
	n_alt_count(50, SnpEffMafRecord.Zero), //n_alt_count
	Transcript_ID(51, SnpEffMafRecord.Null), //Transcript_ID
	Amino_Acid_Change(52, SnpEffMafRecord.Null), //Amino_Acid_Change
	CDS_change(53, SnpEffMafRecord.Null), //CDS_change
	Condon_Change(54, SnpEffMafRecord.Null), //Condon_Change"",Amino_Acid_Length"
	Transcript_BioType(55, SnpEffMafRecord.Null), //Transcript_BioType
	Gene_Coding(56, SnpEffMafRecord.Null), //Gene_Coding
	Exon_Intron_Rank(57, SnpEffMafRecord.Null), // Exon/Intron_Rank"",Exon_Rank"
	Genotype_Number(58, SnpEffMafRecord.Null), //Genotype_Number
	effect_ontology(59, SnpEffMafRecord.Null), //effect_ontology
	effect_class(60,  SnpEffMafRecord.Null), //effect_class
	notes(61, SnpEffMafRecord.Null); //add notes for extra information
		

	private final int columnNo;
	private final String defaultValue;

	private MafElement( int no , String value) {
		this.columnNo = no;
		this.defaultValue = value;	 
	}
	
	public int getColumnNumber(){
		return columnNo;
	}
	
	public String getDefaultValue(){
		return defaultValue;
	}
	
	public static MafElement getByColumnNo(int no){
		for (final MafElement mafEnum : values())  
			if (  mafEnum.columnNo == no )
					return mafEnum; 
		return null; 
	}
}

