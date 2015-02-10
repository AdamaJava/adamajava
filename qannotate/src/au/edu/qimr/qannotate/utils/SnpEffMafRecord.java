package au.edu.qimr.qannotate.utils;

import au.edu.qimr.qannotate.options.Vcf2mafOptions;

 

public class SnpEffMafRecord {	
	
	public static final String Unknown = "Unknown";
	public static final String Other = "Other";
//	public static final String Valid = "Valid";
//	public static final String Invalid = "Invalid";
	public static final String novel = "novel";
	public static final char positive = '+';
	public static final String none = "none";
	public static final String No = "No";
	public static final String Null = "null";
	public static final String Yes = "Yes";
	
	
	public enum mutation_status{ 
		None, Germline,Somatic,LOH,PostTranscriptional, modification,Unknown;
		@Override
		public String toString() {
			switch (this) {
				case PostTranscriptional:
					return "Post-transcriptional";			 
				default:
					return name();		 		
			}
		}
	};
	public enum Validation_Status { Untested,Inconclusive, Valid,Invalid };
	
	public enum Variant_Type{
		SNP, DNP,TNP,ONP,INDEL,Unknown;
		
		public static String getType(String base, String alt){
			try{ 
				if(base.length() == alt.length() ){
					if(base.length()  == 1) return SNP.name();	
					else if(base.length()  == 2) return DNP.name();	
					else if(base.length()  == 3) return TNP.name();	
					else if(base.length()  > 3) return ONP.name();	
				}else  
					return INDEL.name();
			}catch(Exception e){
				 //do nothing
			}
			
			return Unknown.name();
		}
	
	};
		
	String[] maf = new String[59];
	
	//all 58 set methods

	public String getMafLine() {
		String line = maf[0];
		for (int i = 1; i < maf.length; i++)
			
			line += "\t" + maf[i];
		
		return line;
	}
	
	/**
	 * 
	 * @param colNum: there are total 57 column on QIMR maf file, start from column number 1.
	 * @param value: column value number will show on maf file
	 * @throws Exception if column number byond [1,57] or non integer string for column 2, 4, 6, 7, 43-48
	 */
	public void setColumnValue(int colNum, String value) throws Exception{
		if(colNum > maf.length || colNum < 1)
			throw new Exception("invalid column number byond maf record column size: " + colNum);
		
		if(colNum == 2 || colNum == 4 || colNum == 6 || colNum == 7 || (colNum > 43 && colNum <= 48 ))
			if(!value.matches("\\d+")) 
				throw new Exception(String.format("Column %d can't accept non Integer number: %s.", colNum, value)) ;
		
		maf[colNum - 1] = value;		
	}
	public String getColumnValue(int colNum ) throws Exception{
		return maf[colNum - 1] ;		
	}	
	
	
	public static String getSnpEffMafHeaderline(){
		final String[] str = new String[59];
		str[0] = "Hugo_Symbol";
		str[1] = "Entrez_Gene_Id";
		str[2] = "Center";
		str[3] = "NCBI_Build";
		str[4] = "Chromosome";
		str[5] = "Start_Position";
		str[6] = "End_Position";
		str[7] = "Strand";
		str[8] = "Variant_Classification"; // maf specification
		str[9] = "Variant_Type";
		str[10] = "Reference_Allele";
		str[11] = "Tumor_Seq_Allele1";
		str[12] = "Tumor_Seq_Allele2";
		str[13] = "dbSNP_RS";
		str[14] = "dbSNP_Val_Status"; //VLD 
		str[15] = "Tumor_Sample_Barcode";
		str[16] = "Matched_Norm_Sample_Barcode";
		str[17] = "Match_Norm_Seq_Allele1";
		str[18] = "Match_Norm_Seq_Allele2";
		str[19] = "Tumor_Validation_Allele1";
		str[20] = "Tumor_Validation_Allele2";
		str[21] = "Match_Norm_Validation_Allele1";
		str[22] = "Match_Norm_Validation_Allele2";
		str[23] = "Verification_Status";
		str[24] = "Validation_Status";
		str[25] = "Mutation_Status";
		str[26] = "Sequencing_Phase";
		str[27] = "Sequence_Source";
		str[28] = "Validation_Method";
		str[29] = "Score";
		str[30] = "BAM_File";
		str[31] = "Sequencer";
		str[32] = "Tumor_Sample_UUID";
		str[33] = "Matched_Norm_Sample_UUID";
		str[34] = "QFlag";
		str[35] = "ND";
		str[36] = "TD";
		str[37] = "confidence";
		str[38] = "Eff_Impact"; //old "consequence";
	
		str[39] = "Consequnce_rank";
		str[40] = "novel_starts"; //old CpG 
		str[41] = "Var_Plus_Flank"; 
		str[42] = "Variant_AF";//old GMAF 
		str[43] = "t_depth"; 
		str[44] = "t_ref_count"; 
		str[45] = "t_alt_count"; 
		str[46] = "n_depth"; 
		str[47] = "n_ref_count"; 
		str[48] = "n_alt_count"; 
		str[49] = "Transcript_ID"; //must be p. or unkown
		str[50] = "Amino_Acid_Change"; 
		str[51] = "CDS_change";  
		str[52] = "Condon_Change";//old Amino_Acid_Length 
		str[53] = "Transcript_BioType";  
		str[54] = "Gene_Coding"; 
		str[55] = "Exon/Intron_Rank";//Exon_Rank 
		str[56] = "Genotype_Number"; 
		str[57] = "effect_ontology"; 
		str[58] = "effect_class"; 		
		
		String line = str[0];
		for (int i = 1; i < 59; i++)
			line += "\t" + str[i];
		
		return line;
	}
 	
	public void setDefaultValue(){
		maf[0] = Unknown; //Hugo_Symbol
		maf[1] = "0"; //??Entrez_Gene_Id Entrez gene ID (an integer). If no gene exists within 3kb enter "0". 
		maf[2] = Vcf2mafOptions.default_center; //Center"
		maf[3] = "37"; //NCBI_Build
		maf[4] = Null; //Chromosome is compulsary with correct value
		maf[5] = Null; //Start_Position
		maf[6] = Null; //End_Position
		maf[7] = "+"; //Strand
		maf[8] = Unknown; //Variant_Classification =snpeff Impact
		maf[9] = Variant_Type.Unknown.toString(); //Variant_Type
		maf[10] = Null; //Reference_Allele
		maf[11] = Null; //Tumor_Seq_Allele1
		maf[12] = Null; //Tumor_Seq_Allele2
		maf[13] = "novel"; //dbSNP_RS
		maf[14] = Null; //dbSNP_Val_Status
		maf[15] = Unknown; //Tumor_Sample_Barcode, eg. ##tumourSample=ICGC-ABMJ-20120706-01
		maf[16] = Unknown; //Matched_Norm_Sample_Barcode eg. ##normalSample=ICGC-ABMP-20091203-10-ND
		maf[17] = Null; //Match_Norm_Seq_Allele1
		maf[18] = Null; //Match_Norm_Seq_Allele2
		maf[19] = Null; //Tumor_Validation_Allele1
		maf[20] = Null; //Tumor_Validation_Allele2
		maf[21] = Null; //Match_Norm_Validation_Allele1
		maf[22] = Null; //Match_Norm_Validation_Allele2
		maf[23] = Null ; //Verification_Status
		maf[24] = Validation_Status.Untested.name(); ; //Validation_Status
		maf[25] = mutation_status.Unknown.name(); //Mutation_Status somatic/germline
		maf[26] = Null; //Sequencing_Phase
		maf[27] = Unknown; //??Sequence_Source
		maf[28] = none; //Validation_Method NO. If Validation_Status = Untested then "none" If Validation_Status = Valid or Invalid, then not "none" (case insensitive)
		maf[29] = Null; //Score
		maf[30] = Null; //BAM_File
		maf[31] = Unknown; //Sequencer eg. Illumina HiSeq, SOLID
		maf[32] = none; //Tumor_Sample_UUID
		maf[33] = none; //Matched_Norm_Sample_UUID
		// maf[34] = ? //QCMG_Flag, vcf filter column
		maf[35] = Null; //ND
		maf[36] = Null; //TD
		
		maf[37]= Unknown; //"confidence";
		maf[38]= Unknown; //"Eff_Impact";"consequence";

		maf[39] = "-1";  //A.M consequce rank
		maf[40] = Unknown; //"novel_starts";  
		maf[41] = Unknown; //"Var_Plus_Flank"; Cpg;  
		maf[42] = Unknown; //"Variant_AF""GMAF"; 
		maf[43] = "-1"; //t_depth  
		maf[44] = "-1"; //t_ref_count 
		maf[45] = "-1"; //t_alt_count 
		maf[46] = "-1"; //n_depth  
		maf[47] = "-1"; //n_ref_count 
		maf[48] = "-1"; //n_alt_count   
		maf[49] = Null; //Transcript_ID 
		maf[50] = Null; //Amino_Acid_Change  
		maf[51] = Null; //CDS_change 
		maf[52] =  Null; //"Condon_Change",Amino_Acid_Length 
		maf[53] = Null; //Transcript_BioType 
		maf[54] = Null; //Gene_Coding 
		maf[55] = Null; //"Exon/Intron_Rank",Exon_Rank 
		maf[56] = Null; //Genotype_Number 
		maf[57] = Null; //effect_ontology 
		maf[58] = Null; //effect_class 
		
		

	}

}
 