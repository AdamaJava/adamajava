package au.edu.qimr.qannotate.modes;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.Main;
import au.edu.qimr.qannotate.options.Vcf2mafOptions;
import au.edu.qimr.qannotate.utils.SnpEffConsequence;
import au.edu.qimr.qannotate.utils.SnpEffMafRecord;

public class Vcf2maf extends AbstractMode{
	
	private final QLogger logger;
	protected final  Map<String,String> effRanking = new HashMap<String,String>();	
	private final String center;
	private final String sequencer;
//	private final String tumourid;
//	private final String normalid;
	
	public static String  bar = "\\|";
	
	// org.qcmg.common.dcc.DccConsequence.getWorstCaseConsequence(MutationType, String...)
	
	//for unit test
	Vcf2maf(int test_column, int control_column){
 
		center = SnpEffMafRecord.Unknown; 
		sequencer = SnpEffMafRecord.Unknown; 
		this.control_column = control_column;
		this.test_column = test_column;
 		
		logger = QLoggerFactory.getLogger(Main.class, null,  null);	
	}

	

	//EFF= Effect ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_Change| Amino_Acid_Length | Gene_Name | Transcript_BioType | Gene_Coding | Transcript_ID | Exon_Rank  | Genotype_Number [ | ERRORS | WARNINGS ] )
	
	public Vcf2maf(Vcf2mafOptions option, QLogger logger) throws IOException {
		// TODO Auto-generated constructor stub
		 
		this.logger = logger;		
		this.center = option.getCenter();
		this.sequencer = option.getSequencer();				
			
		try(VCFFileReader reader = new VCFFileReader(new File( option.getInputFileName()));
				PrintWriter out = new PrintWriter(option.getOutputFileName())){
			
			//get control and test sample column
			retriveSampleColumn(option.getTestSample(), option.getControlSample(), reader.getHeader());
			
			out.println(SnpEffMafRecord.getSnpEffMafHeaderline());
	       	for (final VcfRecord vcf : reader){ 
        		try{
        		  out.println(converter(vcf).getMafLine());
          		}catch(final Exception e){  	
        			logger.warn("Error message during vcf2maf: " + e.getMessage());
        		}
        	}  
		}			
			
/*				
			final String[] samples = reader.getHeader().getSampleId();
			
			//incase both point into same column
			for(int i = 0; i < samples.length; i++){
				if(samples[i].equalsIgnoreCase(tumourid))
					tumour_column = i + 1;
				if(samples[i].equalsIgnoreCase(normalid))
					test_column = i + 1;
			}
			
			
			if(tumour_column <= 0  && tumourid != null)
				throw new Exception("can't find tumour sample id from vcf header line: " + tumourid);
			if(normal_column <= 0  && normalid != null)
				throw new Exception("can't find normal sample id from vcf header line: " + normalid);	 
				
			*/
 
	}
	//Effect ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_Change| Amino_Acid_length | Gene_Name | Transcript_BioType | Gene_Coding | Transcript_ID | Exon_Rank  | Genotype_Number [ | ERRORS | WARNINGS ] )
	 SnpEffMafRecord converter(VcfRecord vcf) throws Exception{
		final SnpEffMafRecord maf = new SnpEffMafRecord();
		maf.setDefaultValue();
		
		//set common value;				 
		if(center != null) maf.setColumnValue(3, center);
		if(sequencer != null) maf.setColumnValue(32, sequencer); 	//???query DB for sequencer
		maf.setColumnValue(5,  vcf.getChromosome().toUpperCase().replace("CHR", ""));
		maf.setColumnValue(6,  Integer.toString(vcf.getPosition()));
		maf.setColumnValue(7, Integer.toString(vcf.getChrPosition().getEndPosition()));
		 
		maf.setColumnValue(11,  vcf.getRef());	
		maf.setColumnValue(35,  vcf.getFilter());
		maf.setColumnValue(14,  vcf.getId());
		if(vcf.getInfoRecord().getField(VcfHeaderUtils.INFO_VLD) != null)
			maf.setColumnValue(15,  VcfHeaderUtils.INFO_VLD);
		
		if(vcf.getInfoRecord().getField(VcfHeaderUtils.INFO_SOMATIC) != null)
			maf.setColumnValue(26,  VcfHeaderUtils.INFO_SOMATIC);
		
		if(testSample != null) maf.setColumnValue(16,  testSample );
		if(controlSample != null) maf.setColumnValue(17,  controlSample );
		

		final VcfInfoFieldRecord info =  new VcfInfoFieldRecord(vcf.getInfo());
//		if(info.getField(VcfHeaderUtils.FORMAT_NOVEL_STARTS) != null) maf.setColumnValue(40,  info.getField(VcfHeaderUtils.FORMAT_NOVEL_STARTS));
		if(info.getField(VcfHeaderUtils.INFO_CONFIDENT) != null)	maf.setColumnValue(38,  info.getField(VcfHeaderUtils.INFO_CONFIDENT) );
		if(info.getField(VcfHeaderUtils.INFO_FS) != null) maf.setColumnValue(41,  info.getField(VcfHeaderUtils.INFO_FS));
		if(info.getField(VcfHeaderUtils.INFO_VAF) != null) maf.setColumnValue(42,  info.getField(VcfHeaderUtils.INFO_VAF));
		

		String eff; 
		if( (eff = info.getField(VcfHeaderUtils.INFO_EFFECT)) != null)
			getSnpEffAnnotation( maf, eff);

		
		//format & sample field
		final List<String> formats =  vcf.getFormatFields();
//		if(   formats.size() <= Math.max(tumour_column, normal_column)  )
		if(   formats.size() < Math.max(test_column, control_column)  )	
			throw new Exception("Missing sample column in below vcf:\n"+ vcf.toString());
		
		final VcfFormatFieldRecord tumour =  new VcfFormatFieldRecord(formats.get(0), formats.get(test_column));
		
		String nns = null;
		if(tumour != null){	
			//get NNS
			if (tumour.getField(VcfHeaderUtils.FORMAT_NOVEL_STARTS) != null) 
		    		nns = "TD" + tumour.getField(VcfHeaderUtils.FORMAT_NOVEL_STARTS);
	
			//check counts
			String ac = tumour.getField(VcfHeaderUtils.FORMAT_ALLELE_COUNT);
			ac = (ac == null) ? tumour.getField(VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP): ac;

			if(ac != null){
				maf.setColumnValue(37,  ac);
		    	maf.setColumnValue(43, Integer.toString( VcfUtils.getAltFrequency(tumour, null)));
		    	maf.setColumnValue(44, Integer.toString( VcfUtils.getAltFrequency(tumour, vcf.getRef()))); 
		    	maf.setColumnValue(45, Integer.toString( VcfUtils.getAltFrequency(tumour, vcf.getAlt())));
		    	
		    	String[] alleles = getAlleles(tumour);
		    	if(alleles != null && alleles.length == 2){
			    	maf.setColumnValue(12,  alleles[0] );
			    	maf.setColumnValue(13,  alleles[1]);
		    	}
	    	
			}
		}				

		final VcfFormatFieldRecord normal= new VcfFormatFieldRecord(formats.get(0), formats.get(control_column ));
		if(normal != null){		
	    	if (normal.getField(VcfHeaderUtils.FORMAT_NOVEL_STARTS) != null)   		
	    		nns = (nns == null)? "ND" + normal.getField(VcfHeaderUtils.FORMAT_NOVEL_STARTS) : nns +":ND"+ normal.getField(VcfHeaderUtils.FORMAT_NOVEL_STARTS);
			
				//check counts
				String ac = normal.getField(VcfHeaderUtils.FORMAT_ALLELE_COUNT);
				ac = (ac == null) ? tumour.getField(VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP): ac;

				if(ac != null){
					maf.setColumnValue(36,  ac);
			    	maf.setColumnValue(46, Integer.toString( VcfUtils.getAltFrequency(normal, null)));
			    	maf.setColumnValue(47, Integer.toString( VcfUtils.getAltFrequency(normal, vcf.getRef()))); 
			    	maf.setColumnValue(48, Integer.toString( VcfUtils.getAltFrequency(normal, vcf.getAlt())));
			    				    	 
			    	String[] alleles = getAlleles(normal);
			    	if(alleles != null && alleles.length == 2){
				    	maf.setColumnValue(18,  alleles[0] );
				    	maf.setColumnValue(19,  alleles[1]); 	
			    	}
				}
		}
		
		return maf;

	}
	//should unti test to check it
	private String[] getAlleles(VcfFormatFieldRecord format) throws Exception{
		
		String[] alleles = null; // = {Constants.NULL_STRING, Constants.NULL_STRING}; 
 
		if(format == null) return null;
		
		final String allel =  format.getField(VcfHeaderUtils.FORMAT_GENOTYPE_DETAILS);		
		 
		if(allel != null && !allel.equals(Constants.MISSING_DATA_STRING)){	    	
			if(allel.contains(Constants.BAR_STRING)) 
				alleles = allel.split(Constants.BAR_STRING);
			else if(allel.contains(Constants.SLASH_STRING)) 
				alleles = allel.split(Constants.SLASH_STRING);
			 
			if(alleles == null || alleles.length <= 0) return null; //new String[] {Constants.NULL_STRING, Constants.NULL_STRING};  
			else if(alleles.length == 1) return new String[] {alleles[0], Constants.NULL_STRING};
			else return new String[] {alleles[0], alleles[1]};
		}else{
			//compound SNP
			String accs =  format.getField(VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP);	
			if(accs != null && ! accs.equals(Constants.MISSING_DATA_STRING)){
				 alleles = accs.split(Constants.COMMA_STRING);
				 
				if(alleles == null || alleles.length < 3) return null;
				if(alleles.length %3 != 0) 
					throw new Exception("Invalid sample format value:" + accs);;
				

				String[] base = new String[alleles.length / 3];
				int[] counts = new int[alleles.length / 3];
				for (int i = 0; i < alleles.length / 3; i ++ ){
					counts[i] =  Integer.parseInt(alleles[i+1]) + Integer.parseInt(alleles[i+2]);
					base[i] = alleles[i];
				}
				int[] colon = counts.clone();
				Arrays.sort(counts);
				
				String a1 = null, a2 = Constants.NULL_STRING; 
				for(int i = 0; i <base.length; i ++){
					if(colon[i] == counts[counts.length-1])
						a1 = base[i];
					else if(counts.length >= 1 && colon[i] == counts[counts.length-2])
						a2 = base[i];
				}
				
				if(a1 == null) throw new Exception("Algorithm Error during retrive compound SNP Allels:" + accs);
				
				return new String[] {a1, a2};
			}			

			
		}
			
		//format.getField(VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP) != null && 
		return null; 
	}
	 
	 
	 void getSnpEffAnnotation(SnpEffMafRecord maf, String effString) throws Exception  {
		 	String effAnno = SnpEffConsequence.getWorstCaseConsequence(effString.split(","));		 
				//Effect 			   ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_Change| Amino_Acid_length | Gene_Name | Transcript_BioType | Gene_Coding 				| Transcript_ID | Exon_Rank  | Genotype_Number [ | ERRORS | WARNINGS ] )
				//upstream_gene_variant (MODIFIER       |         -         |1760          |     -             |		-		| DDX11L1	|processed_transcript|NON_CODING				|ENST00000456328|		-	 |1)
		 		//	synonymous_variant(LOW			0	|SILENT			   1|aaG/aaA	|p.Lys644Lys/c.1932G>A 3|647  			|VCAM1	    5|protein_coding		6|CODING			7	|ENST00000347652 8|			8|	1)
				//if(effAnno == null) effAnno = effString.split(",")[0];						 
				//if(! StringUtils.isNullOrEmpty(ontolog)  ){
		 	
		 	if( StringUtils.isNullOrEmpty( effAnno )  )
		 		effAnno =  SnpEffConsequence.getUndefinedConsequence(effString.split(","));
		 		
			if(StringUtils.isNullOrEmpty( effAnno )  )
				return;
	
			final String ontolog = effAnno.substring(0, effAnno.indexOf("("));		
			final String annotate = effAnno.substring( effAnno.indexOf("(") + 1, effAnno.indexOf(")"));	
	
			maf.setColumnValue(57, ontolog); //effect_ontology
			String str = SnpEffConsequence.getClassicName(ontolog);
			if(str != null) maf.setColumnValue(58, str);
			str = SnpEffConsequence.getMafClassification(ontolog);
			if(str != null) maf.setColumnValue(9, str); //eg. RNA
	
			final String[] effs = annotate.split(bar);
//			if(! StringUtils.isNullOrEmpty(effs[0]))  maf.setColumnValue(9, effs[0]); //VariantClassification, AM. list			
			if(! StringUtils.isNullOrEmpty(effs[0]))  maf.setColumnValue(39, effs[0]); //Eff Impact, eg. modifier	
			
			if(effs[3].startsWith("p.")){
				int pos = effs[3].indexOf(Constants.SLASH_STRING);
				maf.setColumnValue(50,effs[3].substring(0, pos));
				maf.setColumnValue(51,effs[3].substring(pos+1));
				if(! StringUtils.isNullOrEmpty(effs[2]))  maf.setColumnValue(52,effs[2]);
			}
						
			if(! StringUtils.isNullOrEmpty(effs[5]))  maf.setColumnValue(1, effs[5]);//Gene_Name DDX11L1		
			if(! StringUtils.isNullOrEmpty(effs[6]))  maf.setColumnValue(53,effs[6]);//bioType 	protein_coding		
			if(! StringUtils.isNullOrEmpty(effs[7]))  maf.setColumnValue(54,effs[7]);				
			if(! StringUtils.isNullOrEmpty(effs[8]))  maf.setColumnValue(49,effs[8]);
			if(! StringUtils.isNullOrEmpty(effs[9]))  maf.setColumnValue(55,effs[9]);
			if(! StringUtils.isNullOrEmpty(effs[10])) maf.setColumnValue(56,effs[10]);		
			
			
/*/???
			if(SnpEffConsequence.isConsequence(ontolog)) 
		//		maf.setColumnValue(39, SnpEffMafRecord.Yes);
			else if(! StringUtils.isNullOrEmpty(effs[0].trim())   
					&& ! effs[0].trim().equalsIgnoreCase(SnpEffConsequence.MODIFIER_IMPACT))
				logger.warn( "find undefined consequence of ontolog  from snpEff annotation: " + effString);
*/
 	 }
	 

	
	/**
	 * testing method only to retrive EFF types 
	 * @param vcf
	 * @throws Exception 
	 */
	private void splitEFF(VcfRecord vcf ) throws Exception{
		
		final VcfInfoFieldRecord info =  new VcfInfoFieldRecord(vcf.getInfo());
		try{
			final String[] effs = info.getField(VcfHeaderUtils.INFO_EFFECT).split(",");
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
