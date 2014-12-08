package au.edu.qimr.qannotate.modes;

import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

import au.edu.qimr.qannotate.utils.SnpEffMafRecord;

public class Vcf2maf {
	protected VcfHeader header;
	protected String inputUuid;
	
	
	//extra field for new maf
	String Tumor_Sample_UUID;
	String Matched_Norm_Sample_UUID;
	
	
	
	// org.qcmg.common.dcc.DccConsequence.getWorstCaseConsequence(MutationType, String...)
	
	public Vcf2maf(){   
		
			/*		
				try(VCFFileReader reader = new VCFFileReader(f)) {
			    	header = reader.getHeader();
			       	for(final VcfHeaderRecord hr : header)
			    		if(hr.getMetaType().equals(MetaType.META) && hr.getId().equalsIgnoreCase(VcfHeaderUtils.STANDARD_UUID_LINE)){
			    			inputUuid = hr.getDescription();
			    			break;
			    		}
			    	
			    	//no chr in front of position
					for (final VCFRecord qpr : reader) {
					//	positionRecordMap.put(new ChrPosition(qpr.getChromosome(), qpr.getPosition()),qpr);
					}
				} 
			*/	 
	    
 }
	

	//EFF= Effect ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_Change| Amino_Acid_Length | Gene_Name | Transcript_BioType | Gene_Coding | Transcript_ID | Exon_Rank  | Genotype_Number [ | ERRORS | WARNINGS ] )
	
	public SnpEffMafRecord toMafRecord(VcfRecord vcf ){
		final SnpEffMafRecord maf = new SnpEffMafRecord();

		final VcfInfoFieldRecord info =  new VcfInfoFieldRecord(vcf.getInfo());
		info.getfield(VcfHeaderUtils.INFO_EFFECT); 
		
		
		return maf;
		
	}
	
	

}
