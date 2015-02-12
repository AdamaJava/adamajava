package au.edu.qimr.qannotate.modes;

import java.io.File;
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
import au.edu.qimr.qannotate.modes.ConfidenceMode.Confidence;
import au.edu.qimr.qannotate.options.Vcf2mafOptions;
import au.edu.qimr.qannotate.utils.SnpEffConsequence;
import au.edu.qimr.qannotate.utils.SnpEffMafRecord;
import au.edu.qimr.qannotate.utils.SnpEffMafRecord.Variant_Type;

public class Vcf2maf extends AbstractMode{
	
	private final QLogger logger;
	protected final  Map<String,String> effRanking = new HashMap<String,String>();	
	private final String center;
	private final String sequencer;
	private String patientId;
	
	// org.qcmg.common.dcc.DccConsequence.getWorstCaseConsequence(MutationType, String...)
	//for unit test
	Vcf2maf(int test_column, int control_column){
 
		center = Vcf2mafOptions.default_center;
		sequencer = SnpEffMafRecord.Unknown; 
		this.control_column = control_column;
		this.test_column = test_column;
		this.patientId = SnpEffMafRecord.Unknown; 
 		
		logger = QLoggerFactory.getLogger(Main.class, null,  null);	
	}

	

	//EFF= Effect ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_Change| Amino_Acid_Length | Gene_Name | Transcript_BioType | Gene_Coding | Transcript_ID | Exon_Rank  | Genotype_Number [ | ERRORS | WARNINGS ] )	
	public Vcf2maf(Vcf2mafOptions option, QLogger logger) throws Exception {
		// TODO Auto-generated constructor stub
		 
		this.logger = logger;		
		this.center = option.getCenter();
		this.sequencer = option.getSequencer();			
		
		String SHCC  = option.getOutputFileName().replace(".maf", ".Somatic.HighConfidence.Consequence.maf") ;
		String SHC = option.getOutputFileName().replace(".maf", ".Somatic.HighConfidence.maf") ;
		String GHCC  = option.getOutputFileName().replace(".maf", ".Germline.HighConfidence.Consequence.maf") ;
		String GHC = option.getOutputFileName().replace(".maf", ".Germline.HighConfidence.maf") ;;
		
			
		try(VCFFileReader reader = new VCFFileReader(new File( option.getInputFileName()));
				PrintWriter out = new PrintWriter(option.getOutputFileName());
				PrintWriter out_SHCC = new PrintWriter(SHCC);
				PrintWriter out_SHC = new PrintWriter(SHC);
				PrintWriter out_GHCC = new PrintWriter(GHCC);
				PrintWriter out_GHC = new PrintWriter(GHC)){
			
			//get control and test sample column
			retriveSampleColumn(option.getTestSample(), option.getControlSample(), reader.getHeader());
			patientId = null;
			for (VcfHeader.Record rec : reader.getHeader().getMetaRecords()) {
				if (rec.getData().startsWith(VcfHeaderUtils.STANDARD_DONOR_ID)) {
					patientId = StringUtils.getValueFromKey(rec.getData(), VcfHeaderUtils.STANDARD_DONOR_ID);
				}
			}
//			patientId = (MetaType.META, VcfHeaderUtils.STANDARD_DONOR_ID).getDescription();
			
			createMafHeader(out, option.getCommandLine(), option.getInputFileName());
			createMafHeader(out_SHCC, option.getCommandLine(), option.getInputFileName());
			createMafHeader(out_SHC, option.getCommandLine(), option.getInputFileName());
			createMafHeader(out_GHCC, option.getCommandLine(), option.getInputFileName());
			createMafHeader(out_GHC, option.getCommandLine(), option.getInputFileName());
						
	       	for (final VcfRecord vcf : reader)
        		try{
        			SnpEffMafRecord maf = converter(vcf);
        			String Smaf = maf.getMafLine();
        			out.println(Smaf);
        			int rank = Integer.parseInt(maf.getColumnValue(40));
        			if(maf.getColumnValue(38).equalsIgnoreCase(Confidence.HIGH.name()))
        				if(maf.getColumnValue(26).equalsIgnoreCase(VcfHeaderUtils.INFO_SOMATIC)){
        					out_SHC.println(Smaf);
        					
        					if(maf.getColumnValue(53+1).equalsIgnoreCase("protein_coding") && rank <=5 )
        						out_SHCC.println(Smaf);
        				}else{
        					out_GHC.println(Smaf);
        					 
        					if(maf.getColumnValue(53+1).equalsIgnoreCase("protein_coding") && rank <=5 )
        						out_GHCC.println(Smaf);
        				}
        		  
          		}catch(final Exception e){  	
        			logger.warn("Error message during vcf2maf: " + e.getMessage() + "\n" + vcf.toString());
        			e.printStackTrace();
        			
        		}
         
		}			
	}
	
	private void createMafHeader(PrintWriter write, String cmd, String inputVcfName) throws Exception{
		if(header == null)
			try(VCFFileReader reader = new VCFFileReader(inputVcfName)) {
	        	header = reader.getHeader();
			}
		
		reheader(cmd, inputVcfName);
		

		write.println(SnpEffMafRecord.Version);
		
		for(VcfHeader.Record re: header.getMetaRecords())
			if(!re.equals(VcfHeaderUtils.STANDARD_FILE_VERSION ))
				write.println(re.getData());
		
		for(VcfHeader.QPGRecord re: header.getqPGLines())
			write.println(re.getData());
		
		
		for(Map.Entry<String, VcfHeader.FormattedRecord> re: header.getInfoRecords().entrySet())
			write.println(re.getValue().getData());
 	
//		
//		VcfHeader.Record fileFormat = header.getFileVersion();
//		if (null != fileFormat) {
//			write.println(fileFormat.toString());
//		}
		
//		for(final VcfHeader.Record record: header) 
//			if(record.getMetaType().equals(MetaType.META) && 
//					!record.getId().equals(VcfHeaderUtils.STANDARD_FILE_VERSION ))
//				 write.println(record.toString());
//			else if(record.getMetaType().equals(MetaType.QPG) )
//					 write.println(record.toString());
//>>>>>>> .r514

		write.println(SnpEffMafRecord.getSnpEffMafHeaderline());
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
		
		
		//Variant Type
		maf.setColumnValue(10,Variant_Type.getType(vcf.getRef(), vcf.getAlt()));
		 
		maf.setColumnValue(11,  vcf.getRef());	
		maf.setColumnValue(35,  vcf.getFilter());
		
		//set novel for non dbSNP
		if(vcf.getId().equals(Constants.MISSING_DATA_STRING)) 
			maf.setColumnValue(14,  SnpEffMafRecord.novel);
		else
			maf.setColumnValue(14,  vcf.getId());
		
		if(vcf.getInfoRecord().getField(VcfHeaderUtils.INFO_VLD) != null)
			maf.setColumnValue(15,  VcfHeaderUtils.INFO_VLD);
		
		if(vcf.getInfoRecord().getField(VcfHeaderUtils.INFO_SOMATIC) != null)
			maf.setColumnValue(26,  VcfHeaderUtils.INFO_SOMATIC);
		else
			maf.setColumnValue(26,  VcfHeaderUtils.FILTER_GERMLINE);
		
		if(testSample != null) maf.setColumnValue(16,  patientId + ":" + testSample );
		if(controlSample != null) maf.setColumnValue(17, patientId + ":" + controlSample );
		

		final VcfInfoFieldRecord info =  new VcfInfoFieldRecord(vcf.getInfo());
//		if(info.getField(VcfHeaderUtils.FORMAT_NOVEL_STARTS) != null) maf.setColumnValue(40,  info.getField(VcfHeaderUtils.FORMAT_NOVEL_STARTS));
		if(info.getField(VcfHeaderUtils.INFO_CONFIDENT) != null)	maf.setColumnValue(38,  info.getField(VcfHeaderUtils.INFO_CONFIDENT) );
		if(info.getField(VcfHeaderUtils.INFO_FS) != null) maf.setColumnValue(41+1,  info.getField(VcfHeaderUtils.INFO_FS));
		if(info.getField(VcfHeaderUtils.INFO_VAF) != null) maf.setColumnValue(42+1,  info.getField(VcfHeaderUtils.INFO_VAF));		

		String eff; 
		if( (eff = info.getField(VcfHeaderUtils.INFO_EFFECT)) != null)
			getSnpEffAnnotation( maf, eff);

		
		//format & sample field
		final List<String> formats =  vcf.getFormatFields();
		if(   formats.size() <= Math.max(test_column, control_column)  )	// format include "FORMAT" column, must bigger than sample column
			throw new Exception(" Varint missing sample column on :"+ vcf.getChromosome() + "\t" + vcf.getPosition());

		
		VcfFormatFieldRecord sample =  new VcfFormatFieldRecord(formats.get(0), formats.get(test_column));
		final String[] Tvalues = readFormatField(sample) ;		
		
		if(Tvalues[1] != null){	//allesls counts
			maf.setColumnValue(37,  Tvalues[1]);
	    	maf.setColumnValue(44, Integer.toString( VcfUtils.getAltFrequency(sample, null)));
	    	maf.setColumnValue(45, Integer.toString( VcfUtils.getAltFrequency(sample, vcf.getRef()))); 
	    	maf.setColumnValue(46, Integer.toString( VcfUtils.getAltFrequency(sample, vcf.getAlt())));
	    	maf.setColumnValue(12,  Tvalues[2] );  //TD allele1
	    	maf.setColumnValue(13, Tvalues[3]);		//TD allele2
		}
		
		
		sample =  new VcfFormatFieldRecord(formats.get(0), formats.get(control_column));
		final String[] Nvalues = readFormatField(sample) ;		
		
		if(Nvalues[1] != null){	//allesls counts
			maf.setColumnValue(36,  Nvalues[1]);
	    	maf.setColumnValue(47, Integer.toString( VcfUtils.getAltFrequency(sample, null)));
	    	maf.setColumnValue(48, Integer.toString( VcfUtils.getAltFrequency(sample, vcf.getRef()))); 
	    	maf.setColumnValue(49, Integer.toString( VcfUtils.getAltFrequency(sample, vcf.getAlt())));
	    	maf.setColumnValue(18,  Nvalues[2] );  //ND allele1
	    	maf.setColumnValue(19, Nvalues[3]);	//ND allele2
		}
		

		//NNS eg, ND5:TD7
		String nns = SnpEffMafRecord.Unknown;
		if(Nvalues[0].equals(SnpEffMafRecord.Unknown)) nns = (!Tvalues[0].equals(SnpEffMafRecord.Unknown) )? "TD"+Tvalues[0] : SnpEffMafRecord.Unknown;
		else if (Tvalues[0].equals(SnpEffMafRecord.Unknown)) nns = (!Nvalues[0].equals(SnpEffMafRecord.Unknown) )? "ND"+Nvalues[0] : SnpEffMafRecord.Unknown;
		else nns = String.format("ND%s:TD%s",Nvalues[0], Tvalues[0]);
		maf.setColumnValue(40+1, nns);	

		return maf;

	}
	 /**
	  * 
	  * @param format
	  * @return array[nns, allele_counts, allele1, allele2]
	  * @throws Exception
	  */
	 private String[] readFormatField(VcfFormatFieldRecord format) throws Exception{
		
		 //String nns = null;
		 if(format == null) return null;
		 
		  String[] values = {SnpEffMafRecord.Unknown,null, null, null};
		 
		  //NNS
    	if (format.getField(VcfHeaderUtils.FORMAT_NOVEL_STARTS) != null)   		
    		values[0] = format.getField(VcfHeaderUtils.FORMAT_NOVEL_STARTS); 
	 
		//check counts
    	values[1] = format.getField(VcfHeaderUtils.FORMAT_ALLELE_COUNT);
    	values[1]= (values[1] == null) ? format.getField(VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP): values[1];
    	String[] alleles = getAlleles(format);
		 
    	if(alleles != null)
		 System.arraycopy(alleles, 0, values, 2, 2);
    	
	    return values;
	 }
	 
	 
	 
	//should unti test to check it
	private String[] getAlleles(VcfFormatFieldRecord format) throws Exception {
		
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
			if(  !StringUtils.isNullOrEmpty( accs) && ! accs.equals(Constants.MISSING_DATA_STRING)){
				 alleles = accs.split(Constants.COMMA_STRING);
				 
			//	if(alleles == null || alleles.length < 3) return null;
				if(alleles.length %3 != 0) 
					throw new Exception("Invalid sample format value:" + accs);
				
				final int size = alleles.length / 3;
				String[] base = new String[size];
				int[] counts = new int[size];
				for (int i = 0; i < size; i++ ){
					
					base[i] = alleles[i*3];
					counts[i] =  Integer.parseInt(alleles[i*3+1]) + Integer.parseInt(alleles[i*3+2]);
				}
				int[] colon = counts.clone();
				Arrays.sort(counts);
				
				String a1 = null, a2 = Constants.NULL_STRING; 
				for(int i = 0; i <base.length; i ++){
					if(colon[i] == counts[size-1])
						a1 = base[i];
					else if(size > 1 && colon[i] == counts[counts.length-2])
						a2 = base[i];
				}
				
				if(a1 == null) throw new RuntimeException("Algorithm Error during retrive compound SNP Allels:" + accs);
				
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
	
			maf.setColumnValue(58, ontolog); //effect_ontology
			String str = SnpEffConsequence.getClassicName(ontolog);
			if(str != null) maf.setColumnValue(59, str);
			str = SnpEffConsequence.getMafClassification(ontolog);
			if(str != null) maf.setColumnValue(9, str); //eg. RNA
			
			maf.setColumnValue(40,  SnpEffConsequence.getConsequenceRank(ontolog)+""); //get A.M consequence's rank
	
			final String[] effs = annotate.split(Constants.BAR_STRING);
//			if(! StringUtils.isNullOrEmpty(effs[0]))  maf.setColumnValue(9, effs[0]); //VariantClassification, AM. list			
			if(! StringUtils.isNullOrEmpty(effs[0]))  maf.setColumnValue(39, effs[0]); //Eff Impact, eg. modifier	
			
			if(effs[3].startsWith("p.")){
				int pos = effs[3].indexOf(Constants.SLASH_STRING);
				if(pos >= 0 ){
					maf.setColumnValue(50+1,effs[3].substring(0, pos));
					maf.setColumnValue(51+1,effs[3].substring(pos+1));
				}else
					maf.setColumnValue(50+1,effs[3]);
				if(! StringUtils.isNullOrEmpty(effs[2]))  maf.setColumnValue(52+1,effs[2]);
			}
						
			if(! StringUtils.isNullOrEmpty(effs[5]))  maf.setColumnValue(1, effs[5]);//Gene_Name DDX11L1		
			if(! StringUtils.isNullOrEmpty(effs[6]))  maf.setColumnValue(53+1,effs[6]);//bioType 	protein_coding		
			if(! StringUtils.isNullOrEmpty(effs[7]))  maf.setColumnValue(54+1,effs[7]);				
			if(! StringUtils.isNullOrEmpty(effs[8]))  maf.setColumnValue(49+1,effs[8]);
			if(! StringUtils.isNullOrEmpty(effs[9]))  maf.setColumnValue(55+1,effs[9]);
			if(! StringUtils.isNullOrEmpty(effs[10])) maf.setColumnValue(56+1,effs[10]);		
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


	@Override
	void addAnnotation(String dbfile) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	

}
