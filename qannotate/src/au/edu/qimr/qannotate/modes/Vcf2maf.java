/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate.modes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.MafConfidence;
import org.qcmg.common.string.StringUtils;

import static org.qcmg.common.util.Constants.*;

import org.qcmg.common.util.Constants;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.vcf.*;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeader.Record;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.Options;
import au.edu.qimr.qannotate.utils.*;
 

public class Vcf2maf extends AbstractMode{
	
	public static String PROTEINCODE = "protein_coding";
	
	private static final QLogger logger = QLoggerFactory.getLogger(Vcf2maf.class);
	protected final  Map<String,String> effRanking = new HashMap<String,String>();	
	private final String center;
	private final String sequencer;
	private final String donorId;	 
	private final String testSample;
	private final String controlSample;
	private String testBamId;
	private String controlBamId;
	private final int test_column;
	private final int control_column;
	
	// org.qcmg.common.dcc.DccConsequence.getWorstCaseConsequence(MutationType, String...)
	//for unit test
	Vcf2maf(int test_column, int control_column, String test, String control){ 
		center = SnpEffMafRecord.center;
		sequencer = SnpEffMafRecord.Unknown; 
		this.donorId = SnpEffMafRecord.Unknown; 		
		this.test_column = test_column;
		this.control_column = control_column;
		this.testSample = test;
		this.controlSample = control;		
		this.testBamId = null; 
		this.controlBamId = null; 
	}
 
	//EFF= Effect ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_Change| Amino_Acid_Length | Gene_Name | Transcript_BioType | Gene_Coding | Transcript_ID | Exon_Rank  | Genotype_Number [ | ERRORS | WARNINGS ] )	
	public Vcf2maf( Options option) throws Exception {
		this.center = option.getCenter();
		this.sequencer = option.getSequencer();		
		
		//make output file name		 
		try(VCFFileReader reader = new VCFFileReader(new File( option.getInputFileName()))){
			//get control and test sample column			
			SampleColumn column = SampleColumn.getSampleColumn(option.getTestSample(), option.getControlSample() , reader.getHeader());
			this.test_column = column.getTestSampleColumn();
			this.control_column = column.getControlSampleColumn();
			this.testSample = column.getTestSample();
			this.controlSample = column.getControlSample();				
			this.testBamId = column.getTestBamId();
			this.controlBamId = column.getControlBamId();
			this.donorId = option.getDonorId() == null? SampleColumn.getDonorId(reader.getHeader()) : option.getDonorId();
		
			logger.info(String.format("Test Sample %s is located on column %d after FORMAT", testSample, test_column));
			logger.info(String.format("Control Sample %s is located on column %d after FORMAT", controlSample, control_column));
			logger.info("Donor id is " + donorId);			
		}
					
		String outputname;
		if(option.getOutputFileName() != null) {
			outputname =  option.getOutputFileName();
		} else if( option.getOutputDir() != null) {
			if (donorId != null && controlSample != null && testSample != null) {
				outputname = String.format("%s//%s.%s.%s.maf", option.getOutputDir(), donorId, controlSample , testSample);
			} else {
				throw new Exception("can't formate output file name: <dornorId_controlSample_testSample.maf>, missing realted information on input vcf header!");
			}
		} else {
			throw new Exception("Please specify output file name or output file directory on command line");
		}
	
		String SHCC  = outputname.replace(".maf", ".Somatic.HighConfidence.Consequence.maf") ;
		String SHC = outputname.replace(".maf", ".Somatic.HighConfidence.maf") ;
		String GHCC  = outputname.replace(".maf", ".Germline.HighConfidence.Consequence.maf") ;
		String GHC = outputname.replace(".maf", ".Germline.HighConfidence.maf") ;
		String SHCCVcf  = outputname.replace(".maf", ".Somatic.HighConfidence.Consequence.vcf") ;
		String SHCVcf = outputname.replace(".maf", ".Somatic.HighConfidence.vcf") ;
		String GHCCVcf  = outputname.replace(".maf", ".Germline.HighConfidence.Consequence.vcf") ;
		String GHCVcf = outputname.replace(".maf", ".Germline.HighConfidence.vcf") ;

		long noIn = 0, noOut = 0, no_SHCC = 0, no_SHC = 0, no_GHCC = 0, no_GHC = 0;// no_SLCC = 0, no_SLC = 0, no_GLCC = 0, no_GLC = 0; 
		try(VCFFileReader reader = new VCFFileReader(new File( option.getInputFileName()));
				PrintWriter out = new PrintWriter(outputname);
				PrintWriter out_SHCC = new PrintWriter(SHCC);
				PrintWriter out_SHC = new PrintWriter(SHC);
				PrintWriter out_GHCC = new PrintWriter(GHCC);
				PrintWriter out_GHC = new PrintWriter(GHC);
				PrintWriter outSHCCVcf = new PrintWriter(SHCCVcf);
				PrintWriter outSHCVcf = new PrintWriter(SHCVcf);
				PrintWriter outGHCCVcf = new PrintWriter(GHCCVcf);
				PrintWriter outGHCVcf = new PrintWriter(GHCVcf);
				){
			
			reheader( option.getCommandLine(), option.getInputFileName());			
			createMafHeader(out,out_SHCC,out_SHC,out_GHCC,out_GHC); //out_SLCC,out_SLC,out_GLCC,out_GLC);			
			createVcfHeaders(reader.getHeader(), outSHCCVcf, outSHCVcf, outGHCCVcf, outGHCVcf);
			
			for (final VcfRecord vcf : reader) {
				
	    			noIn ++;
	    			SnpEffMafRecord maf = converter(vcf);
	    			String Smaf = maf.getMafLine();
	    			out.println(Smaf);
	    			noOut ++;
	    			int rank = Integer.parseInt(maf.getColumnValue( MafElement.Consequence_rank));//40
	    			boolean isConsequence = isConsequence(maf.getColumnValue(MafElement.Transcript_BioType), rank); //55
	    			boolean isSomatic = maf.getColumnValue(MafElement.Mutation_Status).equalsIgnoreCase(VcfHeaderUtils.INFO_SOMATIC); //26
	    			if (isHighConfidence(maf)) {
	    				if (isSomatic){
	    					out_SHC.println(Smaf);
	    					outSHCVcf.print(vcf);
	    					no_SHC ++;
	    					
	    					if(isConsequence){
	    						out_SHCC.println(Smaf);
	    						outSHCCVcf.print(vcf);
	    						no_SHCC ++;
	    					}
	    				} else {
	    					out_GHC.println(Smaf);
	    					outGHCVcf.print(vcf);
	    					no_GHC ++; 
	    					 
	    					if(isConsequence){
	    						out_GHCC.println(Smaf);
	    						outGHCCVcf.print(vcf);
	    						no_GHCC ++;
	    					}
	    				}   
	    			} 
			}
		}
		
		logger.info("total input vcf record number is " + noIn);
		logger.info("total output maf record number is " + noOut);
		logger.info(String.format("There are somatic record: %d (high confidence), %d (high confidence consequence)", no_SHC, no_SHCC ));
		logger.info(String.format("There are germatic record: %d (high confidence), %d (high confidence consequence)", no_GHC, no_GHCC));
		
		//delete empty maf files
		deleteEmptyMaf(SHCC, SHC,GHCC,GHC, SHCCVcf, SHCVcf,GHCCVcf,GHCVcf );//SLCC,SLC,GLCC,GLC );		
	}
	
	public static boolean isHighConfidence(SnpEffMafRecord maf) {
		if (null != maf) {
			String svType = maf.getColumnValue(MafElement.Variant_Type);
			String conf = maf.getColumnValue(MafElement.Confidence);
			if(svType.equalsIgnoreCase(SVTYPE.DEL.name()) || svType.equalsIgnoreCase(SVTYPE.INS.name()))
				return MafConfidence.HIGH.name().equals(conf);
			else 
				return Constants.HIGH_1_HIGH_2.equals(maf.getColumnValue(MafElement.Confidence));
		}
		return false;
	}
	
	public static boolean isConsequence(String consequence, int rank) {
		/*
		 * Current criteria is that consequence is equal to protien_coding, and the rank is lt or eq to 5, we are good
		 */
		return consequence.equalsIgnoreCase( PROTEINCODE ) && rank <=5;
	}
		
	private void deleteEmptyMaf(String ...fileNames){
		for(String str : fileNames){
			File f = new File(str);		
			String line = null; //boolean flag = false;
	        try( BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f))); ){
		        	while (null != (line = reader.readLine()) ) {
		        		if(line.startsWith("#") || line.equals(SnpEffMafRecord.getSnpEffMafHeaderline())) {
		        			line = null;         
		        		} else {
		        			break; //find non header line
		        		}
		        	}
	        }	catch(IOException e){
	        		logger.warn("IOException during check whether maf if empty or not : " + str);
	        }		        	
			if(line == null) f.delete();			 
		}		
	}
	
	public static void createVcfHeaders(VcfHeader header, PrintWriter ... writers){
		StringBuilder sb = new StringBuilder();
		for (Record rec : header) {
			if (sb.length() > 0) {
				sb.append(Constants.NL);
			}
			sb.append(rec.toString());
		}
		
		
		Arrays.stream(writers).forEach(pw -> pw.println(sb.toString()));
	}
	
	
	private void createMafHeader(PrintWriter ... writers) {
		 for(PrintWriter write:writers){
			write.println(SnpEffMafRecord.Version);
			
			for(VcfHeader.Record re: header.getMetaRecords()) {
				if ( !re.equals(VcfHeaderUtils.STANDARD_FILE_VERSION )) {
					write.println(re.getData());
				}
			}
			
			for(VcfHeader.QPGRecord re: header.getqPGLines()) {
				write.println(re.getData());
			}
						
			for(Map.Entry<String, VcfHeader.FormattedRecord> re: header.getInfoRecords().entrySet()) {
				write.println(re.getValue().getData());
			}
			
			for(MafElement ele: EnumSet.allOf( MafElement.class))
				write.println(ele.getDescriptionLine());
	
			write.println(SnpEffMafRecord.getSnpEffMafHeaderline());	 
		 }	 
	}

	//Effect ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_Change| Amino_Acid_length | Gene_Name | Transcript_BioType | Gene_Coding | Transcript_ID | Exon_Rank  | Genotype_Number [ | ERRORS | WARNINGS ] )
	 SnpEffMafRecord converter(VcfRecord vcf) {
		final SnpEffMafRecord maf = new SnpEffMafRecord();
		
		//set common value;				 
		if(center != null) maf.setColumnValue(MafElement.Center, center);
		if(sequencer != null) maf.setColumnValue(MafElement.Sequencer, sequencer); 	//???query DB for sequencer

		String chr = vcf.getChromosome();
		if (chr.startsWith(CHR)) {
			chr = chr.substring(CHR.length());
		}
		maf.setColumnValue(MafElement.Chromosome, chr);
		
		//Variant Type
		SVTYPE type = IndelUtils.getVariantType(vcf.getRef(), vcf.getAlt());
		maf.setColumnValue(MafElement.Variant_Type ,type.name());
		
		//start and end position depending on indel type
		if (type.equals(SVTYPE.INS)){
			maf.setColumnValue(MafElement.Start_Position ,  Integer.toString(vcf.getPosition()));
			maf.setColumnValue(MafElement.End_Position, Integer.toString(vcf.getPosition() + 1));			
		} else if (type.equals(SVTYPE.DEL)){	
			maf.setColumnValue(MafElement.Start_Position,  Integer.toString(vcf.getPosition() + 1));
			maf.setColumnValue(MafElement.End_Position, Integer.toString(vcf.getPosition() + vcf.getRef().length()-1));
		} else {		
			maf.setColumnValue(MafElement.Start_Position ,  Integer.toString(vcf.getPosition()));
			maf.setColumnValue(MafElement.End_Position, Integer.toString(vcf.getPosition() + vcf.getRef().length()-1));
		}
		
		maf.setColumnValue(MafElement.Reference_Allele,  IndelUtils.getRefForIndels(vcf.getRef(), type));	
		maf.setColumnValue(MafElement.QFlag,  vcf.getFilter());
		
		//set novel for non dbSNP
		if(vcf.getId() == null || vcf.getId().equals(MISSING_DATA_STRING)) { 
			maf.setColumnValue(MafElement.DbSNP_RS,  SnpEffMafRecord.novel);
		} else {
			maf.setColumnValue(MafElement.DbSNP_RS,  vcf.getId());
		}
		
		final VcfInfoFieldRecord info =  vcf.getInfoRecord();
		final String infoString = info.toString();
		
		if(infoString.contains(VcfHeaderUtils.INFO_VLD)) {
			maf.setColumnValue(MafElement.DbSNP_Val_Status,  VcfHeaderUtils.INFO_VLD);
		}
		boolean isSomatic = VcfUtils.isRecordSomatic(vcf);
		maf.setColumnValue( MafElement.Mutation_Status ,  isSomatic ? VcfHeaderUtils.INFO_SOMATIC : VcfHeaderUtils.INFO_GERMLINE);
		
		if(info.getField(Constants.VCF_MERGE_INFO) != null)
			maf.setColumnValue( MafElement.Input ,  info.getField(Constants.VCF_MERGE_INFO));
				
		if(testBamId != null) maf.setColumnValue(MafElement.Tumor_Sample_Barcode , testBamId );
 		if(controlBamId != null) maf.setColumnValue(MafElement.Matched_Norm_Sample_Barcode, controlBamId );	
 		
 		String bam = ((testBamId != null)? testBamId : MafElement.Tumor_Sample_Barcode.getDefaultValue() ) 
 				+ Constants.COLON + ((controlBamId != null) ?   controlBamId : MafElement.Matched_Norm_Sample_Barcode.getDefaultValue());
  		if(!bam.equals(Constants.COLON_STRING)) maf.setColumnValue( MafElement.BAM_File, bam);
 				
		if(testSample != null) maf.setColumnValue(MafElement.Tumor_Sample_UUID,   testSample );
		if(controlSample != null) maf.setColumnValue(MafElement.Matched_Norm_Sample_UUID,  controlSample );
		
		String conf = VcfUtils.getConfidence(vcf);
		if(info.getField(VcfHeaderUtils.INFO_CONFIDENT) != null) {
			maf.setColumnValue(MafElement.Confidence ,  conf );
		}
		if(info.getField(VcfHeaderUtils.INFO_GERMLINE) != null) {
			maf.setColumnValue(MafElement.Germ_Counts,  info.getField(VcfHeaderUtils.INFO_GERMLINE));	
		}
		if(info.getField(VcfHeaderUtils.INFO_VAF) != null) {
			maf.setColumnValue(MafElement.dbSNP_AF,  info.getField(VcfHeaderUtils.INFO_VAF));	
		}
		
		if(info.getField(VcfHeaderUtils.INFO_FLANKING_SEQUENCE) != null) {
			maf.setColumnValue(MafElement.Var_Plus_Flank,  info.getField(VcfHeaderUtils.INFO_FLANKING_SEQUENCE));			
		} else if( info.getField(VcfHeaderUtils.INFO_HOM) != null) {
			maf.setColumnValue(MafElement.Var_Plus_Flank,  info.getField(VcfHeaderUtils.INFO_HOM).split(Constants.COMMA_STRING)[1]);	
		}
		
		//add notes
		String note =  getNotes(info);
		if(!StringUtils.isNullOrEmpty(note))
			maf.setColumnValue(MafElement.Notes, note);

		String eff = info.getField(VcfHeaderUtils.INFO_EFFECT);
		if (eff != null) {
			getSnpEffAnnotation( maf, eff);
		}
		
		//format & sample field
		final List<String> formats =  vcf.getFormatFields();
		
		//do nothing if null
		if(formats == null) 	return maf; 
		
		if(   formats.size() <= Math.max(test_column, control_column)  ) {	// format include "FORMAT" column, must bigger than sample column
			throw new IllegalArgumentException(" Varint missing sample column on :"+ vcf.getChromosome() + "\t" + vcf.getPosition());
		}
		boolean mergedRecord = VcfUtils.isMergedRecord(vcf);
		VcfFormatFieldRecord sample =  new VcfFormatFieldRecord(formats.get(0), formats.get(test_column));		
		final String[] Tvalues = getAltCounts( sample, vcf.getRef(), vcf.getAlt(), type, mergedRecord);		
		if (Tvalues != null){	//allesls counts
			maf.setColumnValue(MafElement.TD,  Tvalues[6]); //TD
		    	maf.setColumnValue(MafElement.T_Depth , Tvalues[1]); //t_depth
		    	maf.setColumnValue(MafElement.T_Ref_Count , Tvalues[2]); //t_ref_count
		    	maf.setColumnValue(MafElement.T_Alt_Count , Tvalues[3]); //t_alt_count
		    	maf.setColumnValue(MafElement.Tumor_Seq_Allele1 , Tvalues[4] );  //TD allele1
		    	maf.setColumnValue(MafElement.Tumor_Seq_Allele2, Tvalues[5]);		//TD allele2
		}		
		
		sample =  new VcfFormatFieldRecord(formats.get(0), formats.get(control_column));
		final String[] Nvalues = getAltCounts( sample, vcf.getRef(), vcf.getAlt(),type, mergedRecord);
		
		if (Nvalues != null){	//allesls counts
			maf.setColumnValue(MafElement.ND, Nvalues[6]);
		    	maf.setColumnValue(MafElement.N_Depth, Nvalues[1]);
		    	maf.setColumnValue(MafElement.N_Ref_Count, Nvalues[2]); 
		    	maf.setColumnValue(MafElement.N_Alt_Count, Nvalues[3]);
		    	maf.setColumnValue(MafElement.Match_Norm_Seq_Allele1, Nvalues[4]); //ND allele1
		    	maf.setColumnValue(MafElement.Match_Norm_Seq_Allele2 , Nvalues[5]);	//ND allele2
		}		

		//NNS eg, ND5:TD7
		String nns = getMafAlt(vcf.getRef(), vcf.getAlt(), type);
		nns += ":ND" + ((Nvalues == null)? 0 : Nvalues[0]);
		nns += ":TD" + ((Tvalues == null)? 0 : Tvalues[0]);
		maf.setColumnValue(MafElement.Novel_Starts, nns);	
		return maf;

	}
	 
	public static String getNotes(VcfInfoFieldRecord info){
		String str = (info.getField(VcfHeaderUtils.INFO_TRF)!= null)? VcfHeaderUtils.INFO_TRF + "=" +info.getField(VcfHeaderUtils.INFO_TRF) + ";" : "";		
		if(info.getField(VcfHeaderUtils.INFO_HOM)!= null){
			String hom = info.getField(VcfHeaderUtils.INFO_HOM ).split(Constants.COMMA_STRING)[0];	
			try{
			int count = Integer.parseInt(hom);
				str += ( count > 1)? VcfHeaderUtils.INFO_HOM + "="+ count: "";	
			}catch (NumberFormatException e){
				//do nothing
			}
		}	
		
		if(str.endsWith(";"))
			str = str.substring(0, str.length()-1);	
			
		return str.equals("")? MafElement.Notes.getDefaultValue(): str; 
		//return str;
	}
	 
	 public static String getMafAlt(String ref, String alt, SVTYPE type) {
		// String str = alt; 
	 	 if(type.equals(SVTYPE.DEL)){
	 		 return (alt.length() == 1)? "-" : alt.substring(1);
	 		//str = str.substring(1) + ((alt.length() == ref.length())? "": "-" );
			 			
		}else  if(type.equals(SVTYPE.INS)) {
			return (ref.equalsIgnoreCase(alt))? "-" : alt.substring(1);
		}
		 
		 return alt; 
	 } 
	 
	 /**
	  * 
	  * If the record is a merged record, return values for the first 
	  * 
	  * 
	  * @param format
	  * @return array[nns, depth, ref_count, alt_count, allele1, allele2, coverageString(AC,ACCS,ACINDEL)]; 
	  * return null if the input sample hava no value eg. "."
	  */	 
	 public static  String[] getAltCounts(VcfFormatFieldRecord sample, String ref, String alt, SVTYPE type, boolean isMerged) {
	 	 if(sample.isMissingSample() ) return null;
	 	 
	 	 String[] values = {SnpEffMafRecord.Zero,SnpEffMafRecord.Zero, SnpEffMafRecord.Zero,SnpEffMafRecord.Zero, SnpEffMafRecord.Null,SnpEffMafRecord.Null,SnpEffMafRecord.Null}; 
	 	 
	 	 String gd = sample.getField(VcfHeaderUtils.FORMAT_GENOTYPE_DETAILS);
		 if( StringUtils.isMissingDtaString(gd) ) {
			 gd = IndelUtils.getGenotypeDetails(sample, ref, alt); //maybe null
		 }
		 
	 	 boolean useFirst = true;	 
		 if ( ! StringUtils.isMissingDtaString(gd)) {
			 if (isMerged) {
				 String [] gds = gd.split(VCF_MERGE_DELIM + "");
				 if(gds.length == 2 && gds[0].equals(MISSING_DATA_STRING))
					 useFirst = false;
				 gd = useFirst ? gds[0] : gds[1];
			 }
	 		 String[] pairs = gd.contains("|") ? gd.split("|") : gd.split("/");
	 		 if(pairs.length > 0) values[4] = getMafAlt(ref, pairs[0], type);
			 if(pairs.length > 1) values[5] = getMafAlt(ref, pairs[1], type);			 
		 }else  if(type.equals(SVTYPE.DEL) || type.equals(SVTYPE.INS) ){
			 //if missing GD put reference 			 
			 values[4] = IndelUtils.getRefForIndels(ref, type);
			 values[5] =  values[4];
		 }
	 	 
	 	 
		 if(type == null)
			 type = IndelUtils.getVariantType(ref, alt);
		 
      	 if(type.equals(SVTYPE.DEL) || type.equals(SVTYPE.INS) ){
      		 //it return null if no indel counts. 
      		 String acindel = sample.getField(IndelUtils.FORMAT_ACINDEL); 
      		      		 
      		 if( !StringUtils.isMissingDtaString(acindel)) {
      		 //eg. 13,38,37,13[8,5],11[11],0,0,1     		
	      		try{  
	      			values[6] = acindel;  //default value is "null" string not null	    			
		      		String[] counts = values[6].split(COMMA_STRING);
		      		if(counts.length != 9) throw new Exception();
		      		values[0] = counts[0]; //strong supporting reads nns
		      		values[1] = counts[2]; //informative

		      		values[3] = counts[5].substring(0,counts[5].indexOf('['));  //supporting reads total not strong support
		      		//reference reads counts is the informative reads - support/partial reads
		      		int refCounts =  Integer.parseInt(counts[2]) - Integer.parseInt(values[3])- Integer.parseInt(counts[6]);
		      		values[2] = refCounts + "";		      		 
	      		}catch(Exception e){	      			 
	      				logger.warn("invalide " + IndelUtils.FORMAT_ACINDEL + " at vcf formate column: " + sample.toString());
	      		}
      		 }
      	 } else if(  type.equals(SVTYPE.SNP) || type.equals(SVTYPE.DNP) || type.equals(SVTYPE.TNP) || type.equals(SVTYPE.ONP) ){
      		 String nns =  sample.getField(VcfHeaderUtils.FORMAT_NOVEL_STARTS);
      		 if (null != nns) {
  				values[0] = isMerged ? (useFirst ? nns.substring(0, nns.indexOf(VCF_MERGE_DELIM)) : nns.substring(nns.indexOf(VCF_MERGE_DELIM) + 1)) : nns;
      		 }
      		 boolean cs = false;
      		 String bases = sample.getField(VcfHeaderUtils.FORMAT_ALLELE_COUNT);
      		 if (null == bases) {
      			bases =  sample.getField(VcfHeaderUtils.FORMAT_ALLELE_COUNT_COMPOUND_SNP);
      			cs = true;
      		 }
      		 if (isMerged) {
      			 bases = useFirst ? bases.substring(0, bases.indexOf(VCF_MERGE_DELIM)) :  bases.substring(bases.indexOf(VCF_MERGE_DELIM) + 1);
      		 }
      		 
      		 values[6] = bases;
	    		//check counts      		
      		 values[1] = SnpUtils.getTotalCountFromNucleotideString(bases, cs) + "";     
	         values[2] = SnpUtils.getCountFromNucleotideString(bases,ref, cs) + "";
	         values[3] = SnpUtils.getCountFromNucleotideString(bases,alt, cs) + "";	        		     
      	}
		 
      	 //get genotype base
		 if(  StringUtils.isMissingDtaString(gd)) return values;
		 		 	     
		  return values;
	 }
	 	 	 	 
	 void getSnpEffAnnotation(SnpEffMafRecord maf, String effString) {
		 	String effAnno = SnpEffConsequence.getWorstCaseConsequence(effString.split(","));		 
			//Effect 			   ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_Change| Amino_Acid_length | Gene_Name | Transcript_BioType | Gene_Coding 				| Transcript_ID | Exon_Rank  | Genotype_Number [ | ERRORS | WARNINGS ] )
			//upstream_gene_variant (MODIFIER       |         -         |1760          |     -             |		-		| DDX11L1	|processed_transcript|NON_CODING				|ENST00000456328|		-	 |1)
	 		//	synonymous_variant(LOW			0	|SILENT			   1|aaG/aaA	|p.Lys644Lys/c.1932G>A 3|647  			|VCAM1	    5|protein_coding		6|CODING			7	|ENST00000347652 8|			8|	1)
			//if(effAnno == null) effAnno = effString.split(",")[0];						 
			//if(! StringUtils.isNullOrEmpty(ontolog)  ){
		 	
		 	if ( StringUtils.isNullOrEmpty( effAnno )  )
		 		effAnno =  SnpEffConsequence.getUndefinedConsequence(effString.split(","));
		 		
			if (StringUtils.isNullOrEmpty( effAnno )  )
				return;
	
			final String ontolog = effAnno.substring(0, effAnno.indexOf("("));		
			final String annotate = effAnno.substring( effAnno.indexOf("(") + 1, effAnno.indexOf(")"));	
	
			maf.setColumnValue(MafElement.Effect_Ontology , ontolog); //effect_ontology
			String str = SnpEffConsequence.getClassicName(ontolog);
			if (str != null) maf.setColumnValue(MafElement.Effect_Class  , str);		
			
			 
			str = SnpEffConsequence.getMafClassification(ontolog);			 
			if (str != null) { 
				//check whether frameshift_variant			    
				maf.setColumnValue(MafElement.Variant_Classification , (str.equals("Frame_Shift_")? str+maf.getColumnValue(MafElement.Variant_Type): str)); //eg. RNA
			}
			maf.setColumnValue(MafElement.Consequence_rank,  SnpEffConsequence.getConsequenceRank(ontolog)+""); //get A.M consequence's rank
	
			final String[] effs = annotate.split(BAR_STRING);
			if ( ! StringUtils.isNullOrEmpty(effs[0]))  maf.setColumnValue(MafElement.Eff_Impact, effs[0]); //Eff Impact, eg. modifier	
			
			if (effs[3].startsWith("p.")){
				int pos = effs[3].indexOf(SLASH_STRING);
				if (pos >= 0 ){
					maf.setColumnValue(MafElement.Amino_Acid_Change,effs[3].substring(0, pos));
					maf.setColumnValue(MafElement.CDS_Change ,effs[3].substring(pos+1));
				} else {
					maf.setColumnValue(MafElement.Amino_Acid_Change,effs[3]);
				}
				if (! StringUtils.isNullOrEmpty(effs[2]))  maf.setColumnValue(MafElement.Codon_Change ,effs[2]);
			}
						
			if(! StringUtils.isNullOrEmpty(effs[5]))  maf.setColumnValue(MafElement.Hugo_Symbol, effs[5]);//Gene_Name DDX11L1		
			if(! StringUtils.isNullOrEmpty(effs[6]))  maf.setColumnValue(MafElement.Transcript_BioType ,effs[6]);//bioType 	protein_coding		
			if(! StringUtils.isNullOrEmpty(effs[7]))  maf.setColumnValue(MafElement.Gene_Coding,effs[7]);				
			if(! StringUtils.isNullOrEmpty(effs[8]))  maf.setColumnValue(MafElement.Transcript_ID ,effs[8]);
			if(! StringUtils.isNullOrEmpty(effs[9]))  maf.setColumnValue(MafElement.Exon_Intron_Rank ,effs[9]);
			if(! StringUtils.isNullOrEmpty(effs[10])) maf.setColumnValue(MafElement.Genotype_Number,effs[10]);		
 	 }

 	public static Optional<String> getBamid(String key, VcfHeader header){
 		for (final VcfHeader.Record hr : header.getMetaRecords()) { 
			if( hr.getData().indexOf(key) != -1) {
				return Optional.ofNullable(StringUtils.getValueFromKey(hr.getData(), key));
			}
 		}
 		return Optional.empty(); 
 	}
	
	@Override
	void addAnnotation(String dbfile) throws Exception {
		// TODO Auto-generated method stub
	}

}
