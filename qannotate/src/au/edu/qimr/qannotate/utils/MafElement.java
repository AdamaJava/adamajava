/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * 
 */
package au.edu.qimr.qannotate.utils;

import org.qcmg.common.util.IndelUtils;

public enum MafElement {
	Hugo_Symbol( 1,  SnpEffMafRecord.Unknown, "Gene name from EFF sub-field \"Gene_Name\"."), 
	Entrez_Gene_Id( 2, SnpEffMafRecord.Zero, "Entrez gene ID (an integer). qAnnotate is not sure the ID so just enter \"0\"."), 
	Center(3, SnpEffMafRecord.center, "Specified by user, default center is " + SnpEffMafRecord.center + "."),  
	NCBI_Build(4, "37", "Current we use genome 37."),  
	Chromosome(5, SnpEffMafRecord.Null, "Chromosome number without \"chr\" prefix."),  
	Start_Position(6, SnpEffMafRecord.Null, "The start position of SNP/INS vcf record, but one base forward for DEL."),  
	End_Position(7, SnpEffMafRecord.Null," \"vcf reference length + start position - 1\" for SNP/DEL, but one base forward for INS."), 
	Strand(8, "+", "Always assume positive '+'."), //Strand
	Variant_Classification(9, SnpEffMafRecord.Unknown, "Translational effect of variant allele, from converting EFF sub-field \"EFF\" to our interal classification list."), //Variant_Classification =snpeff Impact
	Variant_Type(10,  IndelUtils.SVTYPE.UNKOWN.name(), "Type of mutation: SNP, DNP, TNP, ONP, INS, DEL."), //Variant_Type
	Reference_Allele(11,  SnpEffMafRecord.Null,"The plus strand reference allele at this position. Include the sequence deleted for a deletion, or \"-\" for an insertion."), //Reference_Allele
	Tumor_Seq_Allele1(12, SnpEffMafRecord.Null,"Tumor sequencing (discovery) allele 1 genotype. \" -\" for a deletion; novel inserted sequence for INS; mutated sequence for SNPs; Reference_Allele for indels if missing genotype information." ), 
	Tumor_Seq_Allele2(13, SnpEffMafRecord.Null, "Tumor sequencing (discovery) allele 1 genotype. \" -\" for a deletion; enter Reference_Allele for indels unless genotype information provided." ),  
	DbSNP_RS(14,  "novel", "The dbSNP rs ID (dbSNP_ID) or \"novel\" if there is no dbSNP record."),  
	DbSNP_Val_Status(15,  SnpEffMafRecord.Null, "The dbSNP validation status. Here we enter null."),  
	Tumor_Sample_Barcode(16,  SnpEffMafRecord.Null,"Tumor sample alighmentSet id retriving from vcf header, eg. \"##qTestBamUUID=4c36e4a3-b130-46c6-ace8-b37a9663e30b\", or Test BAM file name."),  
	Matched_Norm_Sample_Barcode(17, SnpEffMafRecord.Null,"Normal sample alighmentSet id retriving from vcf header, eg. \"##qControlBamUUID=6946f2e6-24aa-4cb8-a2fb-35fc3486bb9a\", or Control BAM file name."),  
	Match_Norm_Seq_Allele1(18,SnpEffMafRecord.Null,"Matched normal sequencing allele 1 genotype. \"-\" for deletions; novel inserted sequence for INS; mutated sequence for SNPs; Reference_Allele for indels if missing genotype information." ), 
	Match_Norm_Seq_Allele2(19,SnpEffMafRecord.Null,"Matched normal sequencing allele 2 genotype. \"-\" for deletions; novel inserted sequence for INS; mutated sequence for SNPs; Reference_Allele for indels if missing genotype information." ),  
	Tumor_Validation_Allele1(20,  SnpEffMafRecord.Null, "Secondary data from orthogonal technology. Not valided here, so put null."),  
	Tumor_Validation_Allele2(21,  SnpEffMafRecord.Null, "Secondary data from orthogonal technology. Not valided here, so put null."),  
	Match_Norm_Validation_Allele1(22, SnpEffMafRecord.Null, "Secondary data from orthogonal technology. Not valided here, so put null."),  
	Match_Norm_Validation_Allele2(23, SnpEffMafRecord.Null, "Secondary data from orthogonal technology. Not valided here, so put null."),		
	Verification_Status(24, SnpEffMafRecord.Unknown,"Second pass results from independent attempt using same methods as primary data source. Not verified here, so put null."),  
	Validation_Status(25, SnpEffMafRecord.Unknown, "Second pass results from orthogonal technology.Not valided here, so put unknown."), 	
	Mutation_Status(26, SnpEffMafRecord.Unknown, "Somatic or Germline according to vcf record info column whether marked as \"SOMATIC\" or not."), //Mutation_Status somatic/germline
	Sequencing_Phase(27,  SnpEffMafRecord.Null,"TCGA sequencing phase. Here we put null."),  
	Sequence_Source(28, SnpEffMafRecord.Unknown, "eg.PCR, Capture, WGS. Here we put Unknown."),  
	Validation_Method(29, SnpEffMafRecord.none,"The assay platforms used for the validation call."), //Validation_Method NO. If Validation_Status = Untested then "SnpEffMafRecord.none" If Validation_Status = Valid or Invalid, then not "SnpEffMafRecord.none" (case insensitive)
	Score(30, SnpEffMafRecord.Null, "Not in use."), //Score
	BAM_File(31,  SnpEffMafRecord.Null, "Here we use Tumor_Sample_Barcode."), //BAM_File
	Sequencer(32, SnpEffMafRecord.Unknown, "Sequencer eg. Illumina HiSeq, SOLID."),  
	Tumor_Sample_UUID(33, SnpEffMafRecord.none, "Tumor sample uuid retriving from vcf header, eg. \"##qTestSample=6a6cf50e-f803-4245-8ae9-02b39774ff04\"."),  
	Matched_Norm_Sample_UUID(34,  SnpEffMafRecord.none,"Normal sample uuid retriving from vcf header, eg. \"##qControlSample=6946f2e6-24aa-4cb8-a2fb-35fc3486bb9a\"."),  
	QFlag(35, SnpEffMafRecord.Null,"Value from annotated vcf record FILTER column"),  //? QCMG_Flag, vcf filter column
	ND(36, SnpEffMafRecord.Null, "Control sample pileup counts, from vcf format field 'AC' , 'ACCS' or 'ACINDEL'."), //ND
	TD(37, SnpEffMafRecord.Null,"Test sample pileup counts, from vcf format field 'AC' , 'ACCS' or 'ACINDEL'."),  //TD
	Confidence(38, SnpEffMafRecord.Unknown, "Value from annotated vcf record INFO field 'CONF'."), //"confidence"),
	Eff_Impact(39, SnpEffMafRecord.Unknown, "Value from EFF sub-field \"Effect Impact\"."), //"Eff_Impact"),"consequence"),
	Consequence_rank(40, SnpEffMafRecord.Zero,"An interal ranking accorkding to EFF sub-field \"EFF\"."), //A.M consequce rank	
	Novel_Starts(41,  SnpEffMafRecord.Unknown, "Novel start reads number according to pileup-ed vcf record format field 'NNS' or 'ACINDEL'."), //"novel_starts"),
	Var_Plus_Flank(42,SnpEffMafRecord.Unknown, "Flank sequence from vcf record INFO field 'FLANK' or 'HOM'."), //"Var_Plus_Flank"), Cpg),
	//Variant_AF(43, SnpEffMafRecord.Unknown), //"Variant_AF""GMAF"),
	dbSNP_AF(43, SnpEffMafRecord.Unknown, "Allele frequence from vcf record INFO field 'VAF' which is annotated by dbSNP mode."), //"Variant_AF""GMAF"),
	Germ_Counts(44, SnpEffMafRecord.Null, "Counts from vcf record INFO field 'GREM' which is annotated by germline mode."), //germ_counts
	T_Depth(45, SnpEffMafRecord.Zero, "Tumor sample coverage on SNP position, or informative reads number of indel. Counts from 'TD' column."),
	T_Ref_Count(46, SnpEffMafRecord.Zero, "Tumor sample reference reads number on SNP position, or informative reads number excluds supporting and partial supporting reads. Counts from 'TD' column."), 
	T_Alt_Count(47, SnpEffMafRecord.Zero, "Tumor sample snp reads number or suporting indel reads number. Counts from 'TD' column."), 
	N_Depth(48, SnpEffMafRecord.Zero, "Normal sample coverage on SNP position, or informative reads number of indel. Counts from 'ND' column."),
	N_Ref_Count(49, SnpEffMafRecord.Zero, "Normal sample reference reads number on SNP position, or informative reads number excluds supporting and partial supporting reads. Counts from 'ND' column."), 
	N_Alt_Count(50, SnpEffMafRecord.Zero, "Normal sample snp reads number or suporting indel reads number. Counts from 'ND' column."), 
	Transcript_ID(51, SnpEffMafRecord.Null,"Value from EFF sub-field 'Transcript_ID'."), 
	Amino_Acid_Change(52, SnpEffMafRecord.Null, "First sub-string from EFF sub-field 'Amino_Acid_Change', eg. 'p.Gln236*' from 'p.Gln236*/c.706C>T'."), 
	CDS_Change(53, SnpEffMafRecord.Null,  "Last sub-string from EFF sub-field 'Amino_Acid_Change', eg. 'c.706C>T' from 'p.Gln236*/c.706C>T'."),   
	Codon_Change(54, SnpEffMafRecord.Null, "Value from EFF sub-field 'Codon_Change / Distance'."),
	Transcript_BioType(55, SnpEffMafRecord.Null, "Value from EFF sub-field 'Transcript_BioType'."),  
	Gene_Coding(56, SnpEffMafRecord.Null, "Value from EFF sub-field 'Gene_Coding'."),  
	Exon_Intron_Rank(57, SnpEffMafRecord.Null, "Value from EFF sub-field 'Exon/Intron Rank'."), 
	Genotype_Number(58, SnpEffMafRecord.Null, "Value from EFF sub-field 'Genotype_Number'."),  
	Effect_Ontology(59, SnpEffMafRecord.Null, "Value from EFF sub-field 'Effect'."),  
	Effect_Class(60,  SnpEffMafRecord.Null, "Convert value of Effect_Ontology to Effect_Class."), 
	Input(61, SnpEffMafRecord.Null,"Indicate the belongs of which input VCF file."),
	Notes(62, SnpEffMafRecord.Null, "Additional information from annotated vcf record info column, eg. TRF, HOM.");
		

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

