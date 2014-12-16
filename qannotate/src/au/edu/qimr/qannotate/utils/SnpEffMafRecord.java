package au.edu.qimr.qannotate.utils;

import org.qcmg.common.maf.MAFRecord;
 

public class SnpEffMafRecord extends MAFRecord {	
	
	public static final String Unknown = "Unknown";
	public static final String Other = "Other";
//	public static final String Valid = "Valid";
//	public static final String Invalid = "Invalid";
	public static final String novel = "novel";
	public static final char positive = '+';
	public static final String none  = "none";
	public static final String No = "No";
	 
	
	
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
	

	//extra field for new maf
	String Tumor_Sample_UUID;
	String Matched_Norm_Sample_UUID;

	private String population_frequence;
	private int t_depth;  //total coverage for tumor sample
	private int t_ref_count;
	private int t_alt_count;	
	private int n_depth;
	private int n_ref_count;
	private int n_alt_count;
	
	//snpEff information
	private String Transcript_ID;
	private String Amino_Acid_Change;
	private String Amino_Acid_Length; 
	private String Transcript_BioType;
	private String Gene_Coding;
	private int  Exon_Rank; 
	private int Genotype_Number;
		
	public int get_t_depth(){	 return t_depth; }	
	public void set_t_depth(int t){	  t_depth = t; }
	
	public int get_n_depth(){	 return n_depth; }	
	public void set_n_depth(int t){	  n_depth = t; }
	
	public int get_t_ref_count(){	 return t_ref_count; }	
	public void set_t_ref_count(int t){	  t_ref_count = t; }
	
	public int get_n_ref_count(){	 return n_ref_count; }	
	public void set_n_ref_count(int t){	  n_ref_count = t; }
	
	public int get_t_alt_count(){	 return t_alt_count; }	
	public void set_t_alt_count(int t){	  t_alt_count = t; }
	
	public int get_n_alt_count(){	 return n_alt_count; }	
	public void set_n_alt_count(int t){	  n_alt_count = t; }
	
	public void set_Tumor_Sample_UUID(String id){ Tumor_Sample_UUID = id;}
	public String get_Tumor_Sample_UUID(){ return Tumor_Sample_UUID; }	 
	
	public void set_Matched_Norm_Sample_UUID(String id){ Matched_Norm_Sample_UUID = id;}
	public String get_Matched_Norm_Sample_UUID(){ return Matched_Norm_Sample_UUID; }
	
	public void set_Population_frequence(String i){population_frequence = i;}
	public String get_Population_frequence( ){return population_frequence; }
	
	
	String confidence;
	public void setStringConfidence(String string) {
		this.confidence = string;
	}
	
	public String geStringConfidence() {
		return this.confidence;
	}
	
	
	//set default value;
	public SnpEffMafRecord(){
		setHugoSymbol(Unknown);
		setEntrezGeneId("0");
	    setCenter(Unknown);
	    setNcbiBuild(37);
	    
	  //  setChromosome(String chromosome);
	  //  setStartPosition(int startPosition);
	  //  setEndPosition(int endPosition);
	    
	    setStrand(positive);
	    
	    //??Variant_Classification try snpEff
	 //   setVariantClassification(String variantClassification);
	            
	  //  setVariantType(MutationType variantType);        
	  //  setRef(String ref);       
	   // setTumourAllele1(String tumourAllele1);
	   // setTumourAllele2(String tumourAllele2);
	    
	    setDbSnpId(null);        
	    setDbSnpValStatus(null);       
	    setTumourSampleBarcode(Unknown);
	    setNormalSampleBarcode(Unknown);
	    
	   // setNormalAllele1(String normalAllele1);
	   // setNormalAllele2(String normalAllele2);
	    
	    setTumourValidationAllele1(Unknown);
	    setTumourValidationAllele2(Unknown);
	    setNormalValidationAllele1(Unknown);
	    setNormalValidationAllele2(Unknown);
	    
	    setVerificationStatus(Unknown);
	    setValidationStatus(Validation_Status.Untested.toString());	    
	    setMutationStatus(mutation_status.Unknown.toString());	    
	    setSequencingPhase(Unknown);
	    setSequencingSource(Other);	    
	    setValidationMethod( none );	    
	    setScore(null);  
	    setBamFile(null);  	    
	    setSequencer(Unknown);	
	    set_Tumor_Sample_UUID(Unknown);
		set_Matched_Norm_Sample_UUID(Unknown);
		
		
		//extra field
		set_Population_frequence(Unknown);
        setFlag(Unknown) ;     
        setTd(No) ;       
        setNd(No);        
        setCanonicalTranscriptId(Unknown);
        setCanonicalAAChange(Unknown);
        setCanonicalBaseChange(Unknown);
        setAlternateTranscriptId(Unknown);
        setAlternateAAChange(Unknown);
        setAlternateBaseChange(Unknown);
        setCpg(Unknown);
        
        set_t_depth(-1 );
        set_n_depth(-1 );
        set_t_ref_count(-1 );
        set_n_ref_count(-1 );
        set_t_alt_count(-1 );
        set_n_alt_count(-1 );
	}	
	
	 
	@Override
	public String toFormattedString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(hugoSymbol).append(T);
		sb.append(entrezGeneId).append(T);
		sb.append(center).append(T);
		sb.append(ncbiBuild).append(T);
		sb.append(chromosome).append(T);
		sb.append(startPosition).append(T);
		sb.append(endPosition).append(T);
		sb.append(strand).append(T);
		sb.append(variantClassification).append(T);
		sb.append(variantType).append(T);
		sb.append(ref).append(T);
		sb.append(tumourAllele1).append(T);
		sb.append(tumourAllele2).append(T);
		sb.append(dbSnpId).append(T);
		sb.append(dbSnpValStatus).append(T);
		sb.append(tumourSampleBarcode).append(T);
		sb.append(normalSampleBarcode).append(T);
		sb.append(normalAllele1).append(T);
		sb.append(normalAllele2).append(T);
		sb.append(tumourValidationAllele1).append(T);
		sb.append(tumourValidationAllele2).append(T);
		sb.append(normalValidationAllele1).append(T);
		sb.append(normalValidationAllele2).append(T);
		sb.append(verificationStatus).append(T);
		sb.append(validationStatus).append(T);
		sb.append(mutationStatus).append(T);
		sb.append(sequencingPhase).append(T);
		sb.append(sequencingSource).append(T);
		sb.append(validationMethod).append(T);
		sb.append(score).append(T);
		sb.append(bamFile).append(T);
		sb.append(sequencer).append(T);
		sb.append(Tumor_Sample_UUID).append(T);
		sb.append(Matched_Norm_Sample_UUID).append(T);
		sb.append(flag).append(T);
		sb.append(nd).append(T);
		sb.append(td).append(T);
		sb.append(canonicalTranscriptId).append(T);
		sb.append(canonicalAAChange).append(T);
		sb.append(canonicalBaseChange).append(T);
		sb.append(alternateTranscriptId).append(T);
		sb.append(alternateAAChange).append(T);
		sb.append(alternateBaseChange).append(T);
		
		sb.append(confidence).append(T); 
		sb.append(cpg).append(T); 
		sb.append(novelStartCount).append(T); 
		
		sb.append( population_frequence).append(T); 
		sb.append(t_depth).append(T);
		sb.append(t_ref_count).append(T);
		sb.append(t_alt_count).append(T);
		sb.append(n_depth).append(T);
		sb.append(n_ref_count).append(T);
		sb.append(n_alt_count);
			
		return sb.toString();
 
	}
	
 
	
 
	
 
 	
	public static String toFormatHeaderline(){
		final StringBuilder sb = new StringBuilder();
		sb.append("Hugo_Symbol").append(T);  //1
		sb.append("Entrez_Gene_Id").append(T);  //2
		sb.append("Center").append(T);  //3
		sb.append("NCBI_Build").append(T);  //4
		sb.append("Chromosome").append(T);  //5
		sb.append("Start_Position").append(T);  //6
		sb.append("End_Position").append(T);  //7
		sb.append("Strand").append(T);  //8
		sb.append("Variant_Classification").append(T);  //9
		sb.append("Variant_Type").append(T);  //10
		sb.append("Reference_Allele").append(T);  //11
		sb.append("Tumor_Seq_Allele1").append(T);  //12
		sb.append("Tumor_Seq_Allele2").append(T);  //13
		sb.append("dbSNP_RS").append(T);  //14
		sb.append("dbSNP_Val_Status").append(T);  //15
		sb.append("Tumor_Sample_Barcode").append(T);  //16
		sb.append("Matched_Norm_Sample_Barcode").append(T);  //17
		sb.append("Match_Norm_Seq_Allele1").append(T);  //18
		sb.append("Match_Norm_Seq_Allele2").append(T);  //19
		sb.append("Tumor_Validation_Allele1").append(T);  //20
		sb.append("Tumor_Validation_Allele2").append(T);  //21
		sb.append("Match_Norm_Validation_Allele1").append(T);  //22
		sb.append("Match_Norm_Validation_Allele2").append(T);  //23
		sb.append("Verification_Status4").append(T);  //24
		sb.append("Validation_Status4").append(T);  //25
		sb.append("Mutation_Status").append(T);  //26
		sb.append("Sequencing_Phase").append(T);  //27
		sb.append("Sequence_Source").append(T);  //28
		sb.append("Validation_Method").append(T);  //29
		sb.append("Score").append(T);  //30
		sb.append("BAM_File").append(T);  //31
		sb.append("Sequencer").append(T);  //32
		sb.append("Tumor_Sample_UUID").append(T);  //33
		sb.append("Matched_Norm_Sample_UUID").append(T);  //34
		sb.append("QCMG_Flag").append(T);
		sb.append("ND").append(T);
		sb.append("TD").append(T);
		sb.append("Canonical_Transcript_Id").append(T);
		sb.append("Canonical_AA_Change").append(T);
		sb.append( "Canonical_Base_Change").append(T); 
		sb.append("Alternate_Transcript_Id").append(T);
		sb.append("Alternate_AA_Change").append(T);
		sb.append("Alternate_Base_Change").append(T);
		sb.append("Confidence").append(T); 
		sb.append("CPG").append(T); 
		sb.append("Novel_Starts").append(T); 
		sb.append( "populatiohn_frequence").append(T); 
		sb.append("t_depth").append(T);
		sb.append("t_ref_count").append(T);
		sb.append("t_alt_count").append(T);
		sb.append("n_depth").append(T);
		sb.append("n_ref_count").append(T);
		sb.append("n_alt_count").append(T);

		return sb.toString();
	}



}
