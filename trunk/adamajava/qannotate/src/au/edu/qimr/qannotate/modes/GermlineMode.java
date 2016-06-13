/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate.modes;

import java.io.File;
import java.util.List;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader.Record;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.Options;

public class GermlineMode extends AbstractMode{
	
	private final QLogger logger = QLoggerFactory.getLogger(GermlineMode.class);
	
	//for unit Test only
	GermlineMode(){}

 	public GermlineMode(Options options) throws Exception{
		
		//this.logger = logger;		
		logger.tool("input: " + options.getInputFileName());
        logger.tool("germline database: " + options.getDatabaseFileName() );
        logger.tool("output annotated records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + (options.getLogLevel() == null ? QLoggerFactory.DEFAULT_LEVEL.getName() :  options.getLogLevel()));
 		
		inputRecord(new File( options.getInputFileName())   );
		addAnnotation(options.getDatabaseFileName() );
		reheader(options.getCommandLine(),options.getInputFileName())	;	
		writeVCF(new File(options.getOutputFileName()) );	
	}
	
  	/*
	 * panfs/share/qsnp/icgc_germline_qsnp.vcf
		dbGermline exampleq
		   1       10002   .       A       C,T     .       .       5;11;17;23;29;30;31;46;55;56;57;60;61;62;71;80;81;83;84;91;125;126;127;128;175;176;179;180;181
		   1       10003   .       A       T       .       .       5;17;23;29;30;31;55;56;57;60;61;62;71;80;81;83;84;91;129;174;175;182	
		 input vcf example, replac end PASS to GERM or append for SOMATCI record only
		   chr1    10321   .       C       T       .       MIN;MR;SBIAS;GERM       SOMATIC;MR=4;NNS=4;FS=AACCCTAACCC       GT:GD:AC        0/0:C/C:C3[36],18[8.44],T0[0],1[7]      0/1:C/T:C1[40],16[15.25],T0[0],4[9.75]
			chr1    10327   rs112750067     T       C       .       SBIAS   MR=8;NNS=6;FS=AACCCCAACCC       GT:GD:AC        0/1:C/T:C0[0],8[8.25],T4[36.25],16[12.56]
       		0/1:C/T:C0[0],9[5],T2[20.5],12[17.92]
	 */
 	@Override	
 	/**
 	 * At moment Ther germline database only store single SNP position. 
 	 */
	void addAnnotation(String dbGermlineFile) throws Exception {
//		header.addFilterLine(VcfHeaderUtils.FILTER_GERMLINE, VcfHeaderUtils.DESCRITPION_FILTER_GERMLINE );
 		
 		//init remove all exsiting GERM annotation
 		for (List<VcfRecord> vcfs : positionRecordMap.values()) {
 			for(VcfRecord vcf : vcfs ) {
 				vcf.getInfoRecord().removeField(VcfHeaderUtils.INFO_GERMLINE);
 			}
 		}
 		
		File germFile = new File(dbGermlineFile);
  		try(VCFFileReader reader = new VCFFileReader(germFile)){
  			
  			//add header line first
  			String date = (reader.getHeader().getFileDate() == null)? null: new VcfHeaderUtils.SplitMetaRecord(reader.getHeader().getFileDate()).getValue();
	 		String germ = String.format("%s=<ID=%s,Number=1,Type=Integer,Description=\"%s\",Source=%s,FileDate=%s>", 
				VcfHeaderUtils.HEADER_LINE_INFO, VcfHeaderUtils.INFO_GERMLINE,VcfHeaderUtils.DESCRITPION_INFO_GERMLINE,
				germFile.getAbsolutePath(),  date);
	 		header.parseHeaderLine(germ, true);
	 		
	 		int total = -1;	 				
			for(Record re : reader.getHeader().getMetaRecords()) {
				if(re.getData().startsWith(VcfHeaderUtils.GERMDB_DONOR_NUMBER)) {
					try{
						total = Integer.parseInt( new VcfHeaderUtils.SplitMetaRecord(re).getValue());
					}catch(Exception e){
						total = 0; 
					}
				}
			}
	 	    
	 	   int updatedRecordCount = 0;
	 	   for (final VcfRecord dbGermlineVcf : reader) {
	 		   ChrPosition germCP = dbGermlineVcf.getChrPosition();
	 		   String chr = IndelUtils.getFullChromosome(dbGermlineVcf.getChromosome());
	 		   if ( ! chr.equals(germCP.getChromosome())) {
	 			  germCP = ChrPositionUtils.cloneWithNewChromosomeName(germCP, chr);
	 		   }
//	 		   int end =  dbGermlineVcf.getPosition() + dbGermlineVcf.getRef().length() - 1;
	 		   
	 		   List<VcfRecord> inputVcfs = positionRecordMap.get(germCP);
	 		   if (null == inputVcfs || inputVcfs.size() == 0) {
	 			  continue; 
	 		   }
	 		  
	 		   for(VcfRecord vcf : inputVcfs ) {
	 			  if(annotateGermlineSnp(vcf,  dbGermlineVcf, total )) {
	 				  updatedRecordCount ++ ;
	 			  }
	 		   }
	 	   }
	 	   
	 	  logger.info("Number of SOMATIC records updated with GERM annotation: " + updatedRecordCount);
	 	}
  		
	 }	 
 	
 	/**
 	 * 
 	 * @param inputVcf: a vcf record
 	 * @param germlineVcf: snp listed on germline file
 	 * @param total: total number of listed germline snp number which is written on database file header
 	 * @return true if the inputVcf is matched the germline vcf
 	 */
  		
 	boolean annotateGermlineSnp(VcfRecord inputVcf, VcfRecord germlineVcf , final int total){
 		boolean flag = false;
 		
 		if ( ! inputVcf.getRef().equals(germlineVcf.getRef()) ){
 			logger.warn(String.format( "germline reference base (%s) are different to vcf Record (%s) for variant at position: %s ", 
 					germlineVcf.getRef(), inputVcf.getRef(), germlineVcf.getPosition() ));			 
 			return flag ; 			 
 		}
 		
 		if ( ! VcfUtils.isRecordSomatic(inputVcf)) {
 			return flag;
 		}
 		
 		String [] alts = inputVcf.getAlt().contains(Constants.COMMA_STRING) ? TabTokenizer.tokenize(inputVcf.getAlt(), Constants.COMMA) : new String[] {inputVcf.getAlt()}; 
 		if (null == alts) return flag ;
 		
 		//only annotate somatic variants
			//annotation if at least one alts matches dbSNP alt		 
		for (final String alt : alts) {
			if (germlineVcf.getAlt().contains(alt)) {
				try {
					int counts = Integer.parseInt(germlineVcf.getInfo()); 
					inputVcf.appendInfo(VcfHeaderUtils.INFO_GERMLINE + "=" +  (total > 0 ?  counts + "," + total: ""+counts));	
					flag = true; 
				} catch (Exception e){						
					logger.error("Germline database vcf formart, can't find patient counts from INFO field!");					 
				}
				break;						
			}
		}
		
		 return flag; 				
 	}
  			
}
	

