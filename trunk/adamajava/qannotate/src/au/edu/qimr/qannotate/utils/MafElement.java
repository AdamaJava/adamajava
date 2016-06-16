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

import org.qcmg.common.util.IndelUtils;

public enum MafElement {
	Hugo_Symbol( 1,  SnpEffMafRecord.Unknown, "Gene name from EFF sub-field 'Gene_Name'."), 
	Entrez_Gene_Id( 2, SnpEffMafRecord.Zero, "Entrez gene ID (an integer) Default value: 0."), 
	Center(3, SnpEffMafRecord.center, "Specified by user, Default value: " + SnpEffMafRecord.center + "."),  
	NCBI_Build(4, "37", "Default value: 37."),  
	Chromosome(5, SnpEffMafRecord.Null, "Chromosome number."),  
	Start_Position(6, SnpEffMafRecord.Null, "The genomic start position of variant."),  
	End_Position(7, SnpEffMafRecord.Null, "The genomic end position of variant."), 
	Strand(8, "+", "Default value: +."), //Strand
	Variant_Classification(9, SnpEffMafRecord.Unknown, "Translational effect of variant allele."), //from converting EFF sub-field \"EFF\" to our interal classification list.
	Variant_Type(10,  IndelUtils.SVTYPE.UNKOWN.name(), "Type of mutation: SNP, DNP, TNP, ONP, INS, DEL."), //Variant_Type
	Reference_Allele(11,  SnpEffMafRecord.Null, "The plus strand reference allele at this position. Include the sequence deleted for a deletion, or '-' for an insertion."), //Reference_Allele
	Tumor_Seq_Allele1(12, SnpEffMafRecord.Null, "Tumor data genotype, most frequent allele." ), // "-" for a deletion; novel inserted sequence for INS; mutated sequence for SNPs; Reference_Allele for indels if missing genotype information.
	Tumor_Seq_Allele2(13, SnpEffMafRecord.Null, "Tumor data genotype, second most frequent allele." ), // "-" for a deletion; enter Reference_Allele for indels unless genotype information provided.
	DbSNP_RS(14,  "novel", "The dbSNP rs ID (dbSNP_ID) or 'novel' if there is no dbSNP record."),  
	DbSNP_Val_Status(15,  SnpEffMafRecord.Null, "The dbSNP validation status. Default value: null."),  	
	Tumor_Sample_Barcode(16,  SnpEffMafRecord.Null,"Tumor sample AlignedReadGroupSet eg. 4c36e4a3-b130-46c6-ace8-b37a9663e30b."), //from vcf header, eg. '##qTestBamUUID=4c36e4a3-b130-46c6-ace8-b37a9663e30b', or Test BAM file name.
	Matched_Norm_Sample_Barcode(17, SnpEffMafRecord.Null,"Normal sample AlignedReadGroupSet eg. 6946f2e6-24aa-4cb8-a2fb-35fc3486bb9a."),//vcf header, eg. '##qControlBamUUID=6946f2e6-24aa-4cb8-a2fb-35fc3486bb9a', or Control BAM file name.  		
	Match_Norm_Seq_Allele1(18,SnpEffMafRecord.Null,"Matched normal data genotype, most frequent allele." ), //"-" for deletions; novel inserted sequence for INS; mutated sequence for SNPs; Reference_Allele for indels if missing genotype information.
	Match_Norm_Seq_Allele2(19,SnpEffMafRecord.Null,"Matched normal data genotype, second most frequent allele." ), // "-" for deletions; novel inserted sequence for INS; mutated sequence for SNPs; Reference_Allele for indels if missing genotype information.	
	Tumor_Validation_Allele1(20,  SnpEffMafRecord.UnTest, "Secondary data from orthogonal technology. Default value: untested."),  
	Tumor_Validation_Allele2(21,  SnpEffMafRecord.UnTest, "Secondary data from orthogonal technology. Default value: untested."),  
	Match_Norm_Validation_Allele1(22, SnpEffMafRecord.UnTest, "Secondary data from orthogonal technology. Default value: untested."),  
	Match_Norm_Validation_Allele2(23, SnpEffMafRecord.UnTest, "Secondary data from orthogonal technology. Default value: untested."),		
	Verification_Status(24, SnpEffMafRecord.UnTest,"Second pass results from independent attempt using same methods as primary data source. Default value: untested."),  
	Validation_Status(25, SnpEffMafRecord.UnTest, "Second pass results from orthogonal technology. Default value: untested."), 	
	Mutation_Status(26, SnpEffMafRecord.Unknown, "Somatic or Germline according to vcf record info column whether marked as 'SOMATIC' or not."), //Mutation_Status somatic/germline
	Sequencing_Phase(27,  SnpEffMafRecord.Null,"TCGA sequencing phase. Default value: null."),  
	Sequence_Source(28, SnpEffMafRecord.Unknown, "eg. capillary, amplicon, capture, WGS. Default value: Unknown."),  
	Validation_Method(29, SnpEffMafRecord.Unknown,"The assay platforms used for the validation call, Default value: Unknown."), //Validation_Method NO. If Validation_Status = Untested then "SnpEffMafRecord.none" If Validation_Status = Valid or Invalid, then not "SnpEffMafRecord.none" (case insensitive)
	Score(30, SnpEffMafRecord.Null, "Not in use. Default value: null."), //Score
	BAM_File(31,  SnpEffMafRecord.Null, "Tumor sample AlignedReadGroupSet."), //BAM_File
	Sequencer(32, SnpEffMafRecord.Unknown, "Sequencer eg. Illumina HiSeq, SOLID, Default value: Unknown."),  
	Tumor_Sample_UUID(33, SnpEffMafRecord.none, "Tumor sample identifier eg. 6a6cf50e-f803-4245-8ae9-02b39774ff04."),  //retriving from vcf header, eg. ##qTestSample=6a6cf50e-f803-4245-8ae9-02b39774ff04.	
	Matched_Norm_Sample_UUID(34,  SnpEffMafRecord.none,"Normal sample identifier eg. 6946f2e6-24aa-4cb8-a2fb-35fc3486bb9a."), // retriving from vcf header, eg. ##qControlSample=6946f2e6-24aa-4cb8-a2fb-35fc3486bb9a.	
	
	QFlag(35, SnpEffMafRecord.Null,"Value from annotated vcf record FILTER column"),  //? QCMG_Flag, vcf filter column
	ND(36, SnpEffMafRecord.Null, "Control sample pileup counts, from vcf format field 'AC' , 'ACCS' or 'ACINDEL'."), //ND
	TD(37, SnpEffMafRecord.Null,"Test sample pileup counts, from vcf format field 'AC' , 'ACCS' or 'ACINDEL'."),  //TD
	Confidence(38, SnpEffMafRecord.Unknown, "Value from annotated vcf record INFO field 'CONF'."), //"confidence"),
	Eff_Impact(39, SnpEffMafRecord.Unknown, "Value from EFF sub-field 'Effect Impact'."), //"Eff_Impact"),"consequence"),
	Consequence_rank(40, SnpEffMafRecord.Zero,"An internal ranking according to EFF sub-field 'EFF'."), //A.M consequce rank	
	Novel_Starts(41,  SnpEffMafRecord.Unknown, "Number of reads containing variant with novel start positions, vcf record format field 'NNS' or 'ACINDEL'."), //"novel_starts"),
	Var_Plus_Flank(42,SnpEffMafRecord.Unknown, "Variant flanking reference sequence."), //"Variant_AF""GMAF", Cpg,  from vcf record INFO field 'FLANK' or 'HOM'.
	dbSNP_AF(43, SnpEffMafRecord.Unknown, "dbSNP allele frequency, Default value: Unknown."), //"GMAF",INFO field 'VAF' which is annotated by dbSNP mode.
	Germ_Counts(44, SnpEffMafRecord.Null, "Number of control samples that contain the variant, Default value: null."), // Counts from vcf record INFO field 'GREM' which is annotated by germline mode.
	T_Depth(45, SnpEffMafRecord.Zero, "Tumor sample coverage at SNP position, or number of informative reads across indel position. Counts from 'TD' column."),
	T_Ref_Count(46, SnpEffMafRecord.Zero, "Tumor sample number of reads with reference base at SNP position, or number of informative reads with reference sequence across indel position. Counts from 'TD' column."), 
	T_Alt_Count(47, SnpEffMafRecord.Zero, "Tumor sample number of reads with alternate base at SNP position, or number of supporting reads containing indel. Counts from 'TD' column."), 
	N_Depth(48, SnpEffMafRecord.Zero, "Normal sample coverage at SNP position, or number of informative reads across indel position. Counts from 'ND' column."),
	N_Ref_Count(49, SnpEffMafRecord.Zero, "Normal sample number of reads with reference base at SNP position, or number of informative reads with reference sequence across indel position. Counts from 'ND' column."), 
	N_Alt_Count(50, SnpEffMafRecord.Zero, "Normal sample number of reads with alternate base at SNP position, or number of supporting reads containing indel. Counts from 'ND' column"), 	
	Transcript_ID(51, SnpEffMafRecord.Null,"Ensembl Transcript_ID, Default value: null."), 
	Amino_Acid_Change(52, SnpEffMafRecord.Null, "First sub-string from EFF sub-field 'Amino_Acid_Change', eg. 'p.Gln236*' from 'p.Gln236*/c.706C>T'. Default value: null."), 
	CDS_Change(53, SnpEffMafRecord.Null,  "Last sub-string from EFF sub-field 'Amino_Acid_Change', eg. 'c.706C>T' from 'p.Gln236*/c.706C>T'. Default value: null."),   
	Codon_Change(54, SnpEffMafRecord.Null, "Value from EFF sub-field 'Codon_Change / Distance'. Default value: null."),
	Transcript_BioType(55, SnpEffMafRecord.Null, "Value from EFF sub-field 'Transcript_BioType'. Default value: null."),  
	Gene_Coding(56, SnpEffMafRecord.Null, "Value from EFF sub-field 'Gene_Coding'. Default value: null."),  
	Exon_Intron_Rank(57, SnpEffMafRecord.Null, "Value from EFF sub-field 'Exon/Intron Rank'. Default value: null."), 
	Genotype_Number(58, SnpEffMafRecord.Null, "Value from EFF sub-field 'Genotype_Number'."),  
	Effect_Ontology(59, SnpEffMafRecord.Null, "Value from EFF sub-field 'Effect'."),  
	Effect_Class(60,  SnpEffMafRecord.Null, "Convert value of Effect_Ontology to Effect_Class."), 
	Input(61, SnpEffMafRecord.Null,"Indicates in which vcf the variant was reported."),
	Notes(62, SnpEffMafRecord.Null, "Additional information from annotated vcf record info column, eg. TRF, HOM, Default value: null.");
		   

	private final int columnNo;
	private final String defaultValue;
	private final String description; 

	private MafElement( int no , String value, String Description ) {
		this.columnNo = no;
		this.defaultValue = value;	 
		this.description = Description;
	}
	
	public int getColumnNumber(){
		return columnNo;
	}
	
	public String getDefaultValue(){
		return defaultValue;
	}
	
	public String getDescriptionLine(){		
		return String.format("#maf column %d, %s: %s", columnNo, name(), description);
	}
	public static MafElement getByColumnNo(int no){
		for (final MafElement mafEnum : values())  
			if (  mafEnum.columnNo == no )
					return mafEnum; 
		return null; 
	}
	
	
}

