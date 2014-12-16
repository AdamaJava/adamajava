package au.edu.qimr.qannotate.modes;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.options.Vcf2mafOptions;
import au.edu.qimr.qannotate.utils.SnpEffMafRecord;

public class Vcf2maf extends AbstractMode{
	
	protected final  Map<String,String> effRanking = new HashMap<String,String>();	
	private final String center;
	private final String sequencer;
	private final String tumourid;
	private final String normalid;
	
	public static String  bar = "\\|";
	private int tumour_column = -2; //can't be -1 since will "+1"
	private int normal_column = -2;
	
	// org.qcmg.common.dcc.DccConsequence.getWorstCaseConsequence(MutationType, String...)
	
	//for unit test
	Vcf2maf(){
		this.tumourid = Vcf2mafOptions.normalid_Default;
		this.normalid = Vcf2mafOptions.tumourid_Default;

		center = SnpEffMafRecord.Unknown; 
		sequencer = SnpEffMafRecord.Unknown; 
	}

	

	//EFF= Effect ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_Change| Amino_Acid_Length | Gene_Name | Transcript_BioType | Gene_Coding | Transcript_ID | Exon_Rank  | Genotype_Number [ | ERRORS | WARNINGS ] )
	
	public Vcf2maf(Vcf2mafOptions option, QLogger logger) throws IOException, Exception {
		// TODO Auto-generated constructor stub
		 
		this.center = option.getCenter();
		this.sequencer = option.getSequencer();
		this.tumourid = option.getTumourid();
		this.normalid = option.getNormalid();
		try(VCFFileReader reader = new VCFFileReader(new File( option.getInputFileName()));
				PrintWriter out = new PrintWriter(option.getOutputFileName())){
			
			final String[] samples = reader.getHeader().getSampleId();
			for(int i = 0; i < samples.length; i++)
				if(samples[i].equalsIgnoreCase(tumourid))
					tumour_column = i + 1;
				else if(samples[i].equalsIgnoreCase(normalid))
					normal_column = i + 1;
		 			
			if(tumour_column < 0  && tumourid != null)
				throw new Exception("can't find tumour sample id from vcf header line: " + tumourid);
			if(normal_column < 0  && normalid != null)
				throw new Exception("can't find normal sample id from vcf header line: " + normalid);	 
				
			out.println(SnpEffMafRecord.toFormatHeaderline());
        	for (final VcfRecord vcf : reader){ 
        		try{
        		 out.println(converter(vcf).toFormattedString());
        		}catch(final Exception e){       			
        			logger.warn("Error message during vcf2maf: " + e.getMessage());
        		}
        	}  
		}
	}
	//Effect ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_Change| Amino_Acid_length | Gene_Name | Transcript_BioType | Gene_Coding | Transcript_ID | Exon_Rank  | Genotype_Number [ | ERRORS | WARNINGS ] )
	 SnpEffMafRecord converter(VcfRecord vcf) throws Exception{
		final SnpEffMafRecord maf = new SnpEffMafRecord();
		
		//set common value;				 
		maf.setCenter(center);
		maf.setChromosome(vcf.getChromosome());
		maf.setStartPosition(vcf.getPosition());
		maf.setEndPosition(vcf.getChrPosition().getEndPosition());
		 
		maf.setRef(vcf.getRef());	
		maf.setFlag(vcf.getFilter());
		maf.setDbSnpId(vcf.getId());

		final VcfInfoFieldRecord info =  new VcfInfoFieldRecord(vcf.getInfo());
		if(info.getfield(VcfHeaderUtils.INFO_NOVEL_STARTS) != null) maf.setNovelStartCount(Integer.parseInt(info.getfield(VcfHeaderUtils.INFO_NOVEL_STARTS)));
		if(info.getfield(VcfHeaderUtils.INFO_CONFIDENT) != null)	maf.setStringConfidence(info.getfield(VcfHeaderUtils.INFO_CONFIDENT) );
		if(info.getfield(VcfHeaderUtils.INFO_FS) != null) maf.setCpg(info.getfield(VcfHeaderUtils.INFO_FS));
		if(info.getfield(VcfHeaderUtils.INFO_GMAF) != null) maf.set_Population_frequence(info.getfield(VcfHeaderUtils.INFO_GMAF));

		try{						 
			final String[] eles = info.getfield(VcfHeaderUtils.INFO_EFFECT).split(",")[0].replace("(", " ").replace(")", "").split(" ");
			maf.setVariantClassification(eles[0]);
			maf.setHugoSymbol(eles[1].split(bar)[5] );	//5			
		}catch(final Exception e ){	
			throw new Exception("missing EFF information or EFF info field format wrong: " + info.getfield(VcfHeaderUtils.INFO_EFFECT));
		}
		
		//format & sample field
		final List<String> formats =  vcf.getFormatFields();
		if(   formats.size() <= Math.max(tumour_column, normal_column)  )	
			throw new Exception("Missing sample column for "+ tumourid + " or " + normalid + ", in below vcf:\n"+ vcf.toString());
		
		final VcfFormatFieldRecord tumour = ( tumour_column > 0) ? new VcfFormatFieldRecord(formats.get(0), formats.get(tumour_column )) : null;
		
		if(tumour != null){
			final String allel =  tumour.getfield(VcfHeaderUtils.FORMAT_GENOTYPE_DETAILS);		
			maf.setTd(tumour.toString());
		    if(allel != null && !allel.equals(Constants.MISSING_DATA_STRING)){		    	
		    	maf.setTumourAllele1(allel.substring(0,1));
		    	maf.setTumourAllele2(allel.substring(2,3));
		    	maf.set_t_depth(VcfUtils.getAltFrequency(tumour, null));
		    	maf.set_t_ref_count(VcfUtils.getAltFrequency(tumour, vcf.getRef()));
		    	maf.set_t_alt_count(VcfUtils.getAltFrequency(tumour, vcf.getAlt()));//?? multi allel
		    }
		}
		
		
		final VcfFormatFieldRecord normal= ( normal_column > 0) ? new VcfFormatFieldRecord(formats.get(0), formats.get(normal_column )): null;
		if(normal != null){
			final String allel =  normal.getfield(VcfHeaderUtils.FORMAT_GENOTYPE_DETAILS);		
			maf.setNd(normal.toString());
		    if(allel != null && !allel.equals(Constants.MISSING_DATA_STRING)){
		    	maf.setNormalAllele1(allel.substring(0,1));
		    	maf.setNormalAllele2(allel.substring(2,3));
		    	maf.set_n_depth(VcfUtils.getAltFrequency(normal, null));
		    	maf.set_n_ref_count(VcfUtils.getAltFrequency(normal, vcf.getRef()));
		    	maf.set_n_alt_count(VcfUtils.getAltFrequency(normal, vcf.getAlt()));//?? multi allel
		    }
		}
 			
		return maf;
	}
	 
	 SnpEffMafRecord converter1(VcfRecord vcf){
		 final SnpEffMafRecord maf = new SnpEffMafRecord();
		
		 /*	
		    setCenter(Unknown);

		    setChromosome(String chromosome);
		    setStartPosition(int startPosition);
		    setEndPosition(int endPosition);

		    setVariantType(MutationType variantType);        
		    setRef(String ref);       
		    setTumourAllele1(String tumourAllele1);
		    setTumourAllele2(String tumourAllele2);
		    
		    setDbSnpId(null);        
 		    
		    setNormalAllele1(String normalAllele1);
		    setNormalAllele2(String normalAllele2);
		    
  		    setSequencingSource(Other);  
		    setValidationMethod(none);	    
		    setSequencer(String sequencer);
		*/	

		 return maf;
	 }
	 
	
	
	/**
	 * testing method only to retrive EFF types 
	 * @param vcf
	 */
	private void splitEFF(VcfRecord vcf ){
		
		final VcfInfoFieldRecord info =  new VcfInfoFieldRecord(vcf.getInfo());
		try{
			final String[] effs = info.getfield(VcfHeaderUtils.INFO_EFFECT).split(",");
			for(final String str : effs){
				final String[] eles = str.replace("(", " ").replace("|", " ").split(" ");
				if(effRanking.containsKey(eles[0])  && !effRanking.get(eles[0]).contains(eles[1]))
							effRanking.put(eles[0], effRanking.get(eles[0]).concat( ";").concat(eles[1]));					 
				else
					effRanking.put(eles[0], eles[1]);	
			}
		}catch(final Exception e ){
			return;
		}
	}

	public SnpEffMafRecord toMafRecord(VcfRecord vcf ){
		final SnpEffMafRecord maf = new SnpEffMafRecord();

		final VcfInfoFieldRecord info =  new VcfInfoFieldRecord(vcf.getInfo());
		info.getfield(VcfHeaderUtils.INFO_EFFECT); 
				
		return maf;
		
	}


	@Override
	void addAnnotation(String dbfile) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	

}
