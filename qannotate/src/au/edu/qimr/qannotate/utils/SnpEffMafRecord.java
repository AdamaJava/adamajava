package au.edu.qimr.qannotate.utils;

import org.qcmg.common.maf.MAFRecord;
 

public class SnpEffMafRecord extends MAFRecord {	
	//extra field for new maf
	String Tumor_Sample_UUID;
	String Matched_Norm_Sample_UUID;
	
	
	
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
	
	public void snpEff2maf(String eff_string){
		
		
	}
	 
	
	public String toFormattedStringSnpEff() {
		return toFormattedStringBasic() + T +
		getConfidence() + T +
		getCpg() + T +
		getNovelStartCount();
	}
	

}
