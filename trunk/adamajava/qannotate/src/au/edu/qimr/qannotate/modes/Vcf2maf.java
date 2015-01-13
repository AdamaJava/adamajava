package au.edu.qimr.qannotate.modes;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.options.Vcf2mafOptions;
import au.edu.qimr.qannotate.utils.SnpEffConsequence;
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
	Vcf2maf(int tumour_column, int normal_column){
		this.tumourid = Vcf2mafOptions.normalid_Default;
		this.normalid = Vcf2mafOptions.tumourid_Default;

		center = SnpEffMafRecord.Unknown; 
		sequencer = SnpEffMafRecord.Unknown; 
		this.tumour_column = tumour_column;		 
		this.normal_column = normal_column;
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
		 			
			if(tumour_column <= 0  && tumourid != null)
				throw new Exception("can't find tumour sample id from vcf header line: " + tumourid);
			if(normal_column <= 0  && normalid != null)
				throw new Exception("can't find normal sample id from vcf header line: " + normalid);	 
				
			out.println(SnpEffMafRecord.getSnpEffMafHeaderline());
        	for (final VcfRecord vcf : reader){ 
        		try{
        		 out.println(converter(vcf).getMafLine());
        		}catch(final Exception e){       			
        			logger.warn("Error message during vcf2maf: " + e.getMessage());
        		}
        	}  
		}
	}
	//Effect ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_Change| Amino_Acid_length | Gene_Name | Transcript_BioType | Gene_Coding | Transcript_ID | Exon_Rank  | Genotype_Number [ | ERRORS | WARNINGS ] )
	 SnpEffMafRecord converter(VcfRecord vcf) throws Exception{
		final SnpEffMafRecord maf = new SnpEffMafRecord();
		maf.setDefaultValue();
		
		//set common value;				 
		maf.setColumnValue(3, center);
		maf.setColumnValue(32, sequencer);
		maf.setColumnValue(5,  vcf.getChromosome());
		maf.setColumnValue(6,  Integer.toString(vcf.getPosition()));
		maf.setColumnValue(7, Integer.toString(vcf.getChrPosition().getEndPosition()));
		 
		maf.setColumnValue(11,  vcf.getRef());	
		maf.setColumnValue(35,  vcf.getFilter());
		maf.setColumnValue(14,  vcf.getId());

		final VcfInfoFieldRecord info =  new VcfInfoFieldRecord(vcf.getInfo());
		if(info.getfield(VcfHeaderUtils.INFO_NOVEL_STARTS) != null) maf.setColumnValue(40,  info.getfield(VcfHeaderUtils.INFO_NOVEL_STARTS));
		if(info.getfield(VcfHeaderUtils.INFO_CONFIDENT) != null)	maf.setColumnValue(38,  info.getfield(VcfHeaderUtils.INFO_CONFIDENT) );
		if(info.getfield(VcfHeaderUtils.INFO_FS) != null) maf.setColumnValue(41,  info.getfield(VcfHeaderUtils.INFO_FS));
		if(info.getfield(VcfHeaderUtils.INFO_GMAF) != null) maf.setColumnValue(42,  info.getfield(VcfHeaderUtils.INFO_GMAF));

		String eff; 
		if( (eff = info.getfield(VcfHeaderUtils.INFO_EFFECT)) != null)
			getSnpEffAnnotation( maf, eff);

		
		//format & sample field
		final List<String> formats =  vcf.getFormatFields();
//		if(   formats.size() <= Math.max(tumour_column, normal_column)  )
		if(   formats.size() < Math.max(tumour_column, normal_column)  )	
			throw new Exception("Missing sample column for "+ tumourid + " or " + normalid + ", in below vcf:\n"+ vcf.toString());
		
		final VcfFormatFieldRecord tumour = ( tumour_column > 0) ? new VcfFormatFieldRecord(formats.get(0), formats.get(tumour_column -1)) : null;
		
		if(tumour != null){	
			maf.setColumnValue(37,  tumour.toString());
	    	maf.setColumnValue(43, Integer.toString( VcfUtils.getAltFrequency(tumour, null)));
	    	maf.setColumnValue(44, Integer.toString( VcfUtils.getAltFrequency(tumour, vcf.getRef()))); 
	    	maf.setColumnValue(45, Integer.toString( VcfUtils.getAltFrequency(tumour, vcf.getAlt())));
			
			final String allel =  tumour.getfield(VcfHeaderUtils.FORMAT_GENOTYPE_DETAILS);		
			if(allel != null && !allel.equals(Constants.MISSING_DATA_STRING)){		    	
		    	maf.setColumnValue(12,  allel.substring(0,1));
		    	maf.setColumnValue(13, allel.substring(2,3));
		    	
		    	
		    } 	
		       
		}
		
		
		final VcfFormatFieldRecord normal= ( normal_column > 0) ? new VcfFormatFieldRecord(formats.get(0), formats.get(normal_column - 1)): null;
		if(normal != null){				
			maf.setColumnValue(36,  normal.toString());
	    	maf.setColumnValue(46, Integer.toString( VcfUtils.getAltFrequency(normal, null)));
	    	maf.setColumnValue(47, Integer.toString( VcfUtils.getAltFrequency(normal, vcf.getRef()))); 
	    	maf.setColumnValue(48, Integer.toString( VcfUtils.getAltFrequency(normal, vcf.getAlt())));

			final String allel =  normal.getfield(VcfHeaderUtils.FORMAT_GENOTYPE_DETAILS);	
		    if(allel != null && !allel.equals(Constants.MISSING_DATA_STRING)){
		    	maf.setColumnValue(18,  allel.substring(0,1));
		    	maf.setColumnValue(19, allel.substring(2,3));
		    }
		}
 			
		return maf;
	}
	 
	 
	 private void getSnpEffAnnotation(SnpEffMafRecord maf, String effString) throws Exception{
			
			try{
				//Effect 			   ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_Change| Amino_Acid_length | Gene_Name | Transcript_BioType | Gene_Coding 				| Transcript_ID | Exon_Rank  | Genotype_Number [ | ERRORS | WARNINGS ] )
				//upstream_gene_variant (MODIFIER       |         -         |1760          |     -             |		-			| DDX11L1	|processed_transcript|NON_CODING				|ENST00000456328|		-	 |1)
				
				final String effAnno = SnpEffConsequence.getWorstCaseConsequence(effString.split(","));
				//if(effAnno == null) effAnno = effString.split(",")[0];
								
				final String ontolog = effAnno.substring(0, effAnno.indexOf("("));		
					 
				if(! StringUtils.isNullOrEmpty(ontolog)  ){
					maf.setColumnValue(57, ontolog); //effect_ontology
					final String classic = SnpEffConsequence.getClassicName(ontolog);
					if(classic != null) maf.setColumnValue(58, classic);
					
					if(SnpEffConsequence.isConsequence(ontolog)) maf.setColumnValue(39, SnpEffMafRecord.Yes);
					 
				}
				
				final String annotate = effAnno.substring( effAnno.indexOf("(") + 1, effAnno.indexOf(")"));	
				if(! StringUtils.isNullOrEmpty(annotate)  ){					
					final String[] effs = annotate.split(bar);
					if(! StringUtils.isNullOrEmpty(effs[0]))  maf.setColumnValue(9, effs[0]); //VariantClassification, eg. modifier
					if(! StringUtils.isNullOrEmpty(effs[3]))  maf.setColumnValue(50,effs[2]);
					if(! StringUtils.isNullOrEmpty(effs[3]))  maf.setColumnValue(51,effs[3]);
					if(! StringUtils.isNullOrEmpty(effs[4]))  maf.setColumnValue(52,effs[4]);
					if(! StringUtils.isNullOrEmpty(effs[5]))  maf.setColumnValue(1, effs[5]);
					if(! StringUtils.isNullOrEmpty(effs[6]))  maf.setColumnValue(53,effs[6]);
					if(! StringUtils.isNullOrEmpty(effs[7]))  maf.setColumnValue(54,effs[7]);					
					if(! StringUtils.isNullOrEmpty(effs[8]))  maf.setColumnValue(49,effs[8]);
					if(! StringUtils.isNullOrEmpty(effs[9]))  maf.setColumnValue(55,effs[9]);
					if(! StringUtils.isNullOrEmpty(effs[10])) maf.setColumnValue(56,effs[10]);				
				}
			}catch(final Exception e ){	
				throw new Exception("missing EFF information or EFF info field format wrong: " + effString );
			}
		 
	 }
	 
	 SnpEffMafRecord converter1(VcfRecord vcf){
		 final SnpEffMafRecord maf = new SnpEffMafRecord();
		
		 /* setCenter(Unknown);
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
	 * @throws Exception 
	 */
	private void splitEFF(VcfRecord vcf ) throws Exception{
		
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

	/*
	public SnpEffMafRecord toMafRecord(VcfRecord vcf ){
		final SnpEffMafRecord maf = new SnpEffMafRecord();

		final VcfInfoFieldRecord info =  new VcfInfoFieldRecord(vcf.getInfo());
		info.getfield(VcfHeaderUtils.INFO_EFFECT); 
				
		return maf;
		
	}
*/
	@Override
	void addAnnotation(String dbfile) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	

}
