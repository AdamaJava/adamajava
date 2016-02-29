package au.edu.qimr.qannotate.modes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;
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


public class Vcf2maf extends AbstractMode{
	
	public static String PROTEINCODE = "protein_coding";
	
	private final QLogger logger;
	protected final  Map<String,String> effRanking = new HashMap<String,String>();	
	private final String center;
	private final String sequencer;
	private final String dornorId ;	 
	private final String testSample ;
	private final String controlSample ;
	private final int test_column;
	private final int control_column;

	// org.qcmg.common.dcc.DccConsequence.getWorstCaseConsequence(MutationType, String...)
	//for unit test
	Vcf2maf(int test_column, int control_column, String test, String control){ 
		center = Vcf2mafOptions.default_center;
		sequencer = SnpEffMafRecord.Unknown; 
		this.dornorId = SnpEffMafRecord.Unknown; 		
		logger = QLoggerFactory.getLogger(Main.class, null,  null);	
		this.test_column = test_column;
		this.control_column = control_column;
		this.testSample = test;
		this.controlSample = control;		
	}
 
	//EFF= Effect ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_Change| Amino_Acid_Length | Gene_Name | Transcript_BioType | Gene_Coding | Transcript_ID | Exon_Rank  | Genotype_Number [ | ERRORS | WARNINGS ] )	
	public Vcf2maf(Vcf2mafOptions option, QLogger logger) throws Exception {
		this.logger = logger;		
		this.center = option.getCenter();
		this.sequencer = option.getSequencer();		
		
		//make output file name		 
		try(VCFFileReader reader = new VCFFileReader(new File( option.getInputFileName()))){
			//get control and test sample column			
			SampleColumn column = new SampleColumn(option.getTestSample(), option.getControlSample() , reader.getHeader());
			test_column = column.getTestSampleColumn();
			control_column = column.getControlSampleColumn();
			testSample = column.getTestSample();
			controlSample = column.getControlSample();	
			
			//get donor id
			String  id = option.getDonorId() ;			
			if(  id == null) 
				for (VcfHeader.Record rec : reader.getHeader().getMetaRecords())
					if (rec.getData().startsWith(VcfHeaderUtils.STANDARD_DONOR_ID)){ 
						id = StringUtils.getValueFromKey(rec.getData(), VcfHeaderUtils.STANDARD_DONOR_ID);
						break;			
					}			
			dornorId = id; 			
			logger.info(String.format("Test Sample %s is located on column %d after FORMAT", testSample, test_column));
			logger.info(String.format("Control Sample %s is located on column %d after FORMAT", controlSample, control_column));
			logger.info("Dornor id is " + dornorId);
			
		}
					
		String outputname;
		if(option.getOutputFileName() != null) 
			outputname =  option.getOutputFileName();
		else if( option.getOutputDir() != null){
			if(dornorId != null && controlSample != null && testSample != null)
				outputname = String.format("%s//%s.%s.%s.maf", option.getOutputDir(), dornorId, controlSample , testSample);
			else
				throw new Exception("can't formate output file name: <dornorId_controlSample_testSample.maf>, missing realted information on input vcf header!");
		}else
			throw new Exception("Please specify output file name or output file directory on command line");
	
		String SHCC  = outputname.replace(".maf", ".Somatic.HighConfidence.Consequence.maf") ;
		String SHC = outputname.replace(".maf", ".Somatic.HighConfidence.maf") ;
		String GHCC  = outputname.replace(".maf", ".Germline.HighConfidence.Consequence.maf") ;
		String GHC = outputname.replace(".maf", ".Germline.HighConfidence.maf") ;
		
		String SLCC  = outputname.replace(".maf", ".Somatic.LowConfidence.Consequence.maf") ;
		String SLC = outputname.replace(".maf", ".Somatic.LowConfidence.maf") ;
		String GLCC  = outputname.replace(".maf", ".Germline.LowConfidence.Consequence.maf") ;
		String GLC = outputname.replace(".maf", ".Germline.LowConfidence.maf") ;		

		long noIn = 0, noOut = 0, no_SHCC = 0, no_SHC = 0, no_GHCC = 0, no_GHC = 0, no_SLCC = 0, no_SLC = 0, no_GLCC = 0, no_GLC = 0; 
		try(VCFFileReader reader = new VCFFileReader(new File( option.getInputFileName()));
				PrintWriter out = new PrintWriter(outputname);
				PrintWriter out_SHCC = new PrintWriter(SHCC);
				PrintWriter out_SHC = new PrintWriter(SHC);
				PrintWriter out_GHCC = new PrintWriter(GHCC);
				PrintWriter out_GHC = new PrintWriter(GHC);

				PrintWriter out_SLCC = new PrintWriter(SLCC);
				PrintWriter out_SLC = new PrintWriter(SLC);
				PrintWriter out_GLCC = new PrintWriter(GLCC);
				PrintWriter out_GLC = new PrintWriter(GLC)){			
			
			reheader( option.getCommandLine(), option.getInputFileName());			
			createMafHeader(out,out_SHCC,out_SHC,out_GHCC,out_GHC,out_SLCC,out_SLC,out_GLCC,out_GLC);
			
			for (final VcfRecord vcf : reader)
        		try{ 
        			noIn ++;
        			SnpEffMafRecord maf = converter(vcf);
        			String Smaf = maf.getMafLine();
        			out.println(Smaf);
        			noOut ++;
        			int rank = Integer.parseInt(maf.getColumnValue(40));
        			if(maf.getColumnValue(38).equalsIgnoreCase(Confidence.HIGH.name()))
        				if(maf.getColumnValue(26).equalsIgnoreCase(VcfHeaderUtils.INFO_SOMATIC)){
        					out_SHC.println(Smaf);
        					no_SHC ++;
        					
        					if(maf.getColumnValue(55).equalsIgnoreCase( PROTEINCODE ) && rank <=5 ){
        						out_SHCC.println(Smaf);
        						no_SHCC ++;
        					}
        				}else{
        					out_GHC.println(Smaf);
        					no_GHC ++; 
        					 
        					if(maf.getColumnValue(55).equalsIgnoreCase( PROTEINCODE ) && rank <=5 ){
        						out_GHCC.println(Smaf);
        						no_GHCC ++;
        					}
        				}   
        			else if(option.doOutputLowMaf() && maf.getColumnValue(38).equalsIgnoreCase(Confidence.LOW.name()))
        				if(maf.getColumnValue(26).equalsIgnoreCase(VcfHeaderUtils.INFO_SOMATIC)){
        					out_SLC.println(Smaf);
        					no_SLC ++;
        					
        					if(maf.getColumnValue(55).equalsIgnoreCase( PROTEINCODE ) && rank <=5 ){
        						out_SLCC.println(Smaf);
        						no_SLCC ++;
        					}
        				}else{
        					out_GLC.println(Smaf);
        					no_GLC ++;
        					
        					if(maf.getColumnValue(55).equalsIgnoreCase( PROTEINCODE ) && rank <=5 ){
        						out_GLCC.println(Smaf);
        						no_GLCC ++;
        					}
        				}          			
          		}catch(final Exception e){  	
        			logger.warn("Error message during vcf2maf: " + e.getMessage() + "\n" + vcf.toString());
        			e.printStackTrace();
        		}       	
		}	
		
		logger.info("total input vcf record number is " + noIn);
		logger.info("total output maf record number is " + noOut);
		logger.info(String.format("There are somatic record: %d (high confidence), %d (high confidence consequence), %d (low confidence), %d (log confidence consequence)", no_SHC, no_SHCC, no_SLC, no_SLCC ));
		logger.info(String.format("There are germatic record: %d (high confidence), %d (high confidence consequence), %d (low confidence), %d (log confidence consequence)", no_GHC, no_GHCC, no_GLC, no_GLCC ));
		
		//delete empty maf files
		deleteEmptyMaf(SHCC, SHC,GHCC,GHC,SLCC,SLC,GLCC,GLC );		
	}
		
	private void deleteEmptyMaf(String ...fileNames){
		for(String str : fileNames){
			File f = new File(str);		
			String line = null; //boolean flag = false;
	        try( BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f))); ){        	 
	        	while (null != (line = reader.readLine()) ) 
	        		if(line.startsWith("#") || line.equals(SnpEffMafRecord.getSnpEffMafHeaderline())) 
	        			line = null;         
	        		else
	        			break; //find non header line	        	
	        }catch(Exception e){
	        	logger.warn("Exception during check whether maf if empty or not : " + str);
	        }
		        	
			if(line == null) f.delete();
			 
		}
		
	}
	private void createMafHeader(PrintWriter ... writers) throws Exception{
		 for(PrintWriter write:writers){
			write.println(SnpEffMafRecord.Version);
			
			for(VcfHeader.Record re: header.getMetaRecords())
				if(!re.equals(VcfHeaderUtils.STANDARD_FILE_VERSION ))
					write.println(re.getData());
			
			for(VcfHeader.QPGRecord re: header.getqPGLines())
				write.println(re.getData());
						
			for(Map.Entry<String, VcfHeader.FormattedRecord> re: header.getInfoRecords().entrySet())
				write.println(re.getValue().getData());
	
			write.println(SnpEffMafRecord.getSnpEffMafHeaderline());	 
		 }	 
	}

	//Effect ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_Change| Amino_Acid_length | Gene_Name | Transcript_BioType | Gene_Coding | Transcript_ID | Exon_Rank  | Genotype_Number [ | ERRORS | WARNINGS ] )
	 SnpEffMafRecord converter(VcfRecord vcf) throws Exception{
		final SnpEffMafRecord maf = new SnpEffMafRecord();
		maf.setDefaultValue();
		
		//set common value;				 
		if(center != null) maf.setColumnValue(3, center);
		if(sequencer != null) maf.setColumnValue(32, sequencer); 	//???query DB for sequencer
		maf.setColumnValue(5,  vcf.getChromosome().toUpperCase().replace("CHR", ""));
		
		//Variant Type
		SVTYPE type = IndelUtils.getVariantType(vcf.getRef(), vcf.getAlt());
		maf.setColumnValue(10,type.name());
		
		//start and end position depending on indel type
		if(type.equals(SVTYPE.INS)){
			maf.setColumnValue(6,  Integer.toString(vcf.getPosition()));
			maf.setColumnValue(7, Integer.toString(vcf.getPosition() + 1));			
		}else if(type.equals(SVTYPE.DEL)){	
			maf.setColumnValue(6,  Integer.toString(vcf.getPosition() + 1));
			maf.setColumnValue(7, Integer.toString(vcf.getPosition() + vcf.getRef().length()-1));
		}else{		
			maf.setColumnValue(6,  Integer.toString(vcf.getPosition()));
			maf.setColumnValue(7, Integer.toString(vcf.getPosition() + vcf.getRef().length()-1));
		}
		
		String ref = vcf.getRef();
		if(type.equals(SVTYPE.DEL) )
			ref =  ref.substring(1); //remove heading base
		else if(type.equals(SVTYPE.INS))
			ref = "-"; //replace heading base with "-"
		
		maf.setColumnValue(11,  ref);	
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
			maf.setColumnValue(26,  VcfHeaderUtils.INFO_GERMLINE);
				
//		if(testSample != null) maf.setColumnValue(16,   testSample );
//		if(controlSample != null) maf.setColumnValue(17,  controlSample );	
		
		if(testSample != null) maf.setColumnValue(33,   testSample );
		if(controlSample != null) maf.setColumnValue(34,  controlSample );	
		

		final VcfInfoFieldRecord info =  new VcfInfoFieldRecord(vcf.getInfo());
//		if(info.getField(VcfHeaderUtils.FORMAT_NOVEL_STARTS) != null) maf.setColumnValue(40,  info.getField(VcfHeaderUtils.FORMAT_NOVEL_STARTS));
		if(info.getField(VcfHeaderUtils.INFO_CONFIDENT) != null)	maf.setColumnValue(38,  info.getField(VcfHeaderUtils.INFO_CONFIDENT) );
//		if(info.getField(VcfHeaderUtils.INFO_FS) != null) maf.setColumnValue(41+1,  info.getField(VcfHeaderUtils.INFO_FS));
		if(info.getField(VcfHeaderUtils.INFO_FLANKING_SEQUENCE) != null) maf.setColumnValue(42,  info.getField(VcfHeaderUtils.INFO_FLANKING_SEQUENCE));
		if(info.getField(VcfHeaderUtils.INFO_VAF) != null) maf.setColumnValue(43,  info.getField(VcfHeaderUtils.INFO_VAF));		
		if(info.getField(VcfHeaderUtils.INFO_GERMLINE) != null) maf.setColumnValue(44,  info.getField(VcfHeaderUtils.INFO_GERMLINE));		

		String eff; 
		if( (eff = info.getField(VcfHeaderUtils.INFO_EFFECT)) != null)
			getSnpEffAnnotation( maf, eff);

		
		//format & sample field
		final List<String> formats =  vcf.getFormatFields();
		
		//do nothing if null
		if(formats == null) 	return maf; 
		
		if(   formats.size() <= Math.max(test_column, control_column)  )	// format include "FORMAT" column, must bigger than sample column
			throw new Exception(" Varint missing sample column on :"+ vcf.getChromosome() + "\t" + vcf.getPosition());

		
		VcfFormatFieldRecord sample =  new VcfFormatFieldRecord(formats.get(0), formats.get(test_column));		
		final String[] Tvalues = getAltCounts( sample, vcf.getRef(), vcf.getAlt(), type);		
		if(Tvalues != null){	//allesls counts
			maf.setColumnValue(37,  Tvalues[6]); //TD
	    	maf.setColumnValue(45, Tvalues[1]); //t_depth
	    	maf.setColumnValue(46, Tvalues[2]); //t_ref_count
	    	maf.setColumnValue(47, Tvalues[3]); //t_alt_count
	    	maf.setColumnValue(12, Tvalues[4] );  //TD allele1
	    	maf.setColumnValue(13, Tvalues[5]);		//TD allele2
		}		
		
		sample =  new VcfFormatFieldRecord(formats.get(0), formats.get(control_column));
		final String[] Nvalues = getAltCounts( sample, vcf.getRef(), vcf.getAlt(),type);
		
		if(Nvalues != null){	//allesls counts
			maf.setColumnValue(36, Nvalues[6]);
	    	maf.setColumnValue(48, Nvalues[1]);
	    	maf.setColumnValue(49, Nvalues[2]); 
	    	maf.setColumnValue(50, Nvalues[3]);
	    	maf.setColumnValue(18, Nvalues[4]); //ND allele1
	    	maf.setColumnValue(19, Nvalues[5]);	//ND allele2
		}		

		//NNS eg, ND5:TD7
		String nns = getMafAlt(vcf.getRef(), vcf.getAlt(), type);
		nns += ":ND" + ((Nvalues == null)? 0 : Nvalues[0]);
		nns += ":TD" + ((Tvalues == null)? 0 : Tvalues[0]);
		maf.setColumnValue(41, nns);
		
//		if(Nvalues[0].equals(SnpEffMafRecord.Unknown)) nns = (!Tvalues[0].equals(SnpEffMafRecord.Unknown) )? nns+":TD"+Tvalues[0] : SnpEffMafRecord.Unknown;
//		else if (Tvalues[0].equals(SnpEffMafRecord.Unknown)) nns = (!Nvalues[0].equals(SnpEffMafRecord.Unknown) )? nns+":ND"+Nvalues[0] : SnpEffMafRecord.Unknown;
//		else nns += String.format(":ND%s:TD%s",Nvalues[0], Tvalues[0]);
			

		return maf;

	}
	 
	 public static String getMafAlt(String ref, String alt, SVTYPE type) {
		 String str = alt; 
	 	 if(type.equals(SVTYPE.DEL)){
			str = str.substring(1);
			for(int j = alt.length(); j < ref.length(); j ++)
				str += "-";
		}else  if(type.equals(SVTYPE.INS)) 
			
			str = (ref.equalsIgnoreCase(alt))? "-" : str.substring(1);
			
		 
		 return str; 
	 } 
	 
	 /**
	  * 
	  * @param format
	  * @return array[nns, depth, ref_count, alt_count, allele1, allele2, coverageString(AC,ACCS,ACINDEL)]; 
	  * return null if the input sample hava no value eg. "."
	  */	 
	 private String[] getAltCounts(VcfFormatFieldRecord sample, String ref, String alt, SVTYPE type) {

	 	 if(sample.isMissingSample() ) return null;
	 	 
	 	 String[] values = {"0",SnpEffMafRecord.Zero, SnpEffMafRecord.Zero,SnpEffMafRecord.Zero, SnpEffMafRecord.Null,SnpEffMafRecord.Null,SnpEffMafRecord.Null}; 
	 	 
		 if(type == null)
			 type = IndelUtils.getVariantType(ref, alt);
		 
      	 if(type.equals(SVTYPE.DEL) || type.equals(SVTYPE.INS) ){
      		 //it return null if no indel counts. 
      		 String acindel = sample.getField(IndelUtils.FORMAT_ACINDEL); 
      		      		 
      		 if( !StringUtils.isMissingDtaString(acindel))
      		 //eg. 13,38,37,13[8,5],0,0,1     		
	      		try{  
	      			values[6] = acindel;  //default value is "null" string not null	    			
		      		String[] counts = values[6].split(Constants.COMMA_STRING);
		      		if(counts.length != 8) throw new Exception();
		      		values[0] = counts[0]; //supporting reads nns
		      		values[1] = counts[1]; //coverage
		      		values[3] = counts[3].substring(0,counts[3].indexOf('['));  //supporting reads total
		      	      		
		      		//reference reads counts is the informative reads - support/partial/nearbyindel reads
		//      	int refCounts =  Integer.parseInt(counts[2]) - Integer.parseInt(values[3])- Integer.parseInt(counts[5])-Integer.parseInt(counts[6]);
		      		int refCounts =  Integer.parseInt(counts[2]) - Integer.parseInt(values[3])- Integer.parseInt(counts[5]);
		      		values[2] = refCounts + "";		      		 
	      		}catch(Exception e){	      			 
	      				logger.warn("invalide " + IndelUtils.FORMAT_ACINDEL + " at vcf formate column: " + sample.toString());
	      		}
      	 }else if(  type.equals(SVTYPE.SNP) || type.equals(SVTYPE.DNP) || 
      			 type.equals(SVTYPE.TNP) || type.equals(SVTYPE.ONP) ){
      		if (sample.getField(VcfHeaderUtils.FORMAT_NOVEL_STARTS) != null)   		
        		values[0] = sample.getField(VcfHeaderUtils.FORMAT_NOVEL_STARTS); 
    		//check counts      		
        	values[1] = VcfUtils.getAltFrequency(sample, null) + "";
        	values[2] = VcfUtils.getAltFrequency(sample, ref) + "";
        	values[3] = VcfUtils.getAltFrequency(sample, alt) + "";
        		     
        	values[6] = sample.getField(VcfHeaderUtils.FORMAT_ALLELE_COUNT);
	        values[6] = (values[6] == null) ? sample.getField(VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP): values[6];
      	 }
		 
      	 //get genotype base
		 String gd = sample.getField(VcfHeaderUtils.FORMAT_GENOTYPE_DETAILS);
		 if( StringUtils.isMissingDtaString(gd) )
			 gd = IndelUtils.getGenotypeDetails(sample, ref, alt); //maybe null		 
		 if(  StringUtils.isMissingDtaString(gd)) return values;
		 		 
		  String[] pairs = gd.contains("|") ? gd.split("|") : gd.split("/");		  
		  if(pairs.length > 0) values[4] = getMafAlt(ref, pairs[0], type);
		  if(pairs.length > 1) values[5] = getMafAlt(ref, pairs[1], type);
		 		 	     
		  return values;
	 }
	 	 
	//should unti test to check it
//	private String[] getAlleles(VcfFormatFieldRecord format) throws Exception {
//		
//		String[] alleles = null; // = {Constants.NULL_STRING, Constants.NULL_STRING}; 
// 
//		if(format == null) return null;
//		
//		final String allel =  format.getField(VcfHeaderUtils.FORMAT_GENOTYPE_DETAILS);		
//		 
//		if(allel != null && !allel.equals(Constants.MISSING_DATA_STRING)){	    	
//			if(allel.contains(Constants.BAR_STRING)) 
//				alleles = allel.split(Constants.BAR_STRING);
//			else if(allel.contains(Constants.SLASH_STRING)) 
//				alleles = allel.split(Constants.SLASH_STRING);
//			 
//			if(alleles == null || alleles.length <= 0) return null; //new String[] {Constants.NULL_STRING, Constants.NULL_STRING};  
//			else if(alleles.length == 1) return new String[] {alleles[0], Constants.NULL_STRING};
//			else return new String[] {alleles[0], alleles[1]};
//		}else{
//			//compound SNP
//			String accs =  format.getField(VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP);	
//			if(  !StringUtils.isNullOrEmpty( accs) && ! accs.equals(Constants.MISSING_DATA_STRING)){
//				 alleles = accs.split(Constants.COMMA_STRING);
//				 
//			//	if(alleles == null || alleles.length < 3) return null;
//				if(alleles.length %3 != 0) 
//					throw new Exception("Invalid sample format value:" + accs);
//				
//				final int size = alleles.length / 3;
//				String[] base = new String[size];
//				int[] counts = new int[size];
//				for (int i = 0; i < size; i++ ){
//					
//					base[i] = alleles[i*3];
//					counts[i] =  Integer.parseInt(alleles[i*3+1]) + Integer.parseInt(alleles[i*3+2]);
//				}
//				int[] colon = counts.clone();
//				Arrays.sort(counts);
//				
//				String a1 = null, a2 = Constants.NULL_STRING; 
//				for(int i = 0; i <base.length; i ++){
//					if(colon[i] == counts[size-1])
//						a1 = base[i];
//					else if(size > 1 && colon[i] == counts[counts.length-2])
//						a2 = base[i];
//				}
//				
//				if(a1 == null) throw new RuntimeException("Algorithm Error during retrive compound SNP Allels:" + accs);
//				
//				return new String[] {a1, a2};
//			}			
//
//			
//		}
//			
//		//format.getField(VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP) != null && 
//		return null; 
//	}
	 	 
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
	
			maf.setColumnValue(59, ontolog); //effect_ontology
			String str = SnpEffConsequence.getClassicName(ontolog);
			if(str != null) maf.setColumnValue(60, str);			
			str = SnpEffConsequence.getMafClassification(ontolog);
			if(str != null) 
				//check whether frameshift_variant			    
				maf.setColumnValue(9, (str.equals("Frame_Shift_")? str+maf.getColumnValue(10): str)); //eg. RNA
			
			maf.setColumnValue(40,  SnpEffConsequence.getConsequenceRank(ontolog)+""); //get A.M consequence's rank
	
			final String[] effs = annotate.split(Constants.BAR_STRING);
//			if(! StringUtils.isNullOrEmpty(effs[0]))  maf.setColumnValue(9, effs[0]); //VariantClassification, AM. list			
			if(! StringUtils.isNullOrEmpty(effs[0]))  maf.setColumnValue(39, effs[0]); //Eff Impact, eg. modifier	
			
			if(effs[3].startsWith("p.")){
				int pos = effs[3].indexOf(Constants.SLASH_STRING);
				if(pos >= 0 ){
					maf.setColumnValue(52,effs[3].substring(0, pos));
					maf.setColumnValue(53,effs[3].substring(pos+1));
				}else
					maf.setColumnValue(52,effs[3]);
				if(! StringUtils.isNullOrEmpty(effs[2]))  maf.setColumnValue(52+1+1,effs[2]);
			}
						
			if(! StringUtils.isNullOrEmpty(effs[5]))  maf.setColumnValue(1, effs[5]);//Gene_Name DDX11L1		
			if(! StringUtils.isNullOrEmpty(effs[6]))  maf.setColumnValue(55,effs[6]);//bioType 	protein_coding		
			if(! StringUtils.isNullOrEmpty(effs[7]))  maf.setColumnValue(56,effs[7]);				
			if(! StringUtils.isNullOrEmpty(effs[8]))  maf.setColumnValue(51,effs[8]);
			if(! StringUtils.isNullOrEmpty(effs[9]))  maf.setColumnValue(57,effs[9]);
			if(! StringUtils.isNullOrEmpty(effs[10])) maf.setColumnValue(58,effs[10]);		
 	 }


	@Override
	void addAnnotation(String dbfile) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	

}
