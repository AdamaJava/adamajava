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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

import au.edu.qimr.qannotate.Options;

public class GermlineMode extends AbstractMode{
	
	private static final QLogger logger = QLoggerFactory.getLogger(GermlineMode.class);
	
	//for unit Test only
	GermlineMode(){}

 	public GermlineMode(Options options) throws Exception{
		
		//this.logger = logger;		
		logger.tool("input: " + options.getInputFileName());
        logger.tool("germline database: " + options.getDatabaseFileName() );
        logger.tool("output annotated records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + (options.getLogLevel() == null ? QLoggerFactory.DEFAULT_LEVEL.getName() :  options.getLogLevel()));
 		
		loadVcfRecordsFromFile(new File( options.getInputFileName())   );
		addAnnotation(options.getDatabaseFileName() );
		header.addInfo(VcfHeaderUtils.INFO_GERMLINE, ".", "String",VcfHeaderUtils.INFO_GERMLINE_DESC);
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
 	 * At the moment the germline database only contains point mutations. 
 	 */
	void addAnnotation(String dbGermlineFile) throws IOException {
 		
 		/*
 		 *  remove all exsiting GERM annotation
 		 */
 		for (List<VcfRecord> vcfs : positionRecordMap.values()) {
 			for(VcfRecord vcf : vcfs ) {
 				vcf.getInfoRecord().removeField(VcfHeaderUtils.INFO_GERMLINE);
 			}
 		}
 		
 		int updatedRecordCount = 0;
 		try (InputStream fileStream = new FileInputStream(dbGermlineFile);
	 		InputStream gzipStream =  (FileUtils.isFileNameGZip(new File(dbGermlineFile)))  ? new GZIPInputStream(fileStream) : fileStream;
	 		Reader decoder = new InputStreamReader(gzipStream, Charset.forName("US-ASCII"));
	 		BufferedReader buffered = new BufferedReader(decoder);) {
	 		String line;
	 		int counter = 0;
	 		short mCounter = 0;
	 		while ((line = buffered.readLine()) != null) {
	 			counter++;
	 			if (counter == 1000000) {
	 				mCounter++;
	 				counter = 0;
	 				logger.info("Hit " + mCounter + "M germlinedb records");
	 			}
	 			
	 			String[] usParams = TabTokenizer.tokenize(line, '_');
	 			if (null != usParams && usParams.length > 3) {
	 				// contig is first followed by position
					ChrPosition cp = new ChrPointPosition(usParams[0], Integer.parseInt(usParams[1]));
					List<VcfRecord> inputVcfs = positionRecordMap.get(cp);
					if (null == inputVcfs || inputVcfs.size() == 0) {
						continue; 
					}
					String [] cParams = TabTokenizer.tokenize(usParams[3], Constants.COLON);
					if (null != cParams && cParams.length > 0) {
	   
					   for(VcfRecord vcf : inputVcfs ) {
						  if (annotateGermlineSnp(vcf,  usParams[2], cParams[0], Arrays.copyOfRange(cParams, 1, cParams.length - 1))) {
							  updatedRecordCount ++ ;
						  }
					   }
					}
	 			} else {
	 				logger.warn("Malformed germline database line: " + line);
	 			}
	 		}
 		}
 		logger.info("updated " + updatedRecordCount + " vcf records with GERM annotation in INFO field");
 		
//		File germFile = new File(dbGermlineFile);
//  		try(VCFFileReader reader = new VCFFileReader(germFile)){
//  			
//  			//add header line first
//  			String date = (reader.getHeader().getFileDate() == null)? null: reader.getHeader().getFileDate().getMetaValue(); //new VcfHeaderUtils.SplitMetaRecord(reader.getHeader().getFileDate()).getValue();
//	 		String germ = String.format("%s=<ID=%s,Number=.,Type=String,Description=\"%s\",Source=%s,FileDate=%s>", 
//				VcfHeaderUtils.HEADER_LINE_INFO, VcfHeaderUtils.INFO_GERMLINE,VcfHeaderUtils.INFO_GERMLINE_DESC,
//				germFile.getAbsolutePath(),  date);
//	 		if (null != header) {
//	 			header.addOrReplace(germ, true);
//	 		}
//	 		
//	 		int total = -1;	 				
//			for(VcfHeaderRecord re : reader.getHeader()) {
//				if(re.toString().startsWith(VcfHeaderUtils.GERMDB_DONOR_NUMBER)) {
//					try{
//						total = Integer.parseInt( re.getMetaValue());
//					} catch (NumberFormatException e){
//						total = 0; 
//					}
//				}
//			}
//	 	    
//	 	   int updatedRecordCount = 0;
//	 	   for (final VcfRecord dbGermlineVcf : reader) {
//	 		   ChrPosition germCP = dbGermlineVcf.getChrPosition();
//	 		   String chr = IndelUtils.getFullChromosome(dbGermlineVcf.getChromosome());
//	 		   if ( ! chr.equals(germCP.getChromosome())) {
//	 			  germCP = ChrPositionUtils.cloneWithNewChromosomeName(germCP, chr);
//	 		   }
//	 		   
//	 		   List<VcfRecord> inputVcfs = positionRecordMap.get(germCP);
//	 		   if (null == inputVcfs || inputVcfs.size() == 0) {
//	 			  continue; 
//	 		   }
//	 		  
//	 		   for(VcfRecord vcf : inputVcfs ) {
//	 			  if(annotateGermlineSnp(vcf,  dbGermlineVcf, total )) {
//	 				  updatedRecordCount ++ ;
//	 			  }
//	 		   }
//	 	   }
//	 	  logger.info("Number of SOMATIC records updated with GERM annotation: " + updatedRecordCount);
//	 	}
	 }
 	
 	/**
 	 * 
 	 * @param inputVcf: a vcf record - INFO field will be updated if the ref and alts match
 	 * @param germlineVcf: snp listed on germline file
 	 * @param total: total number of listed germline snp number which is written on database file header
 	 * @return true if the inputVcf is matched the germline vcf
 	 */
 	public static boolean annotateGermlineSnp(VcfRecord vcf, String gRef, String gAlt, String[] gNumbers) {
 		boolean flag = false;
 		
 		if ( ! vcf.getRef().equals(gRef) ){
 			logger.warn(String.format( "germline reference base (%s) are different to vcf Record (%s) for variant at position: %s ", 
 					gRef, vcf.getRef(), vcf.getPosition() ));			 
 			return false ; 			 
 		}
 		
 		String [] alts = vcf.getAlt().contains(Constants.COMMA_STRING) ? TabTokenizer.tokenize(vcf.getAlt(), Constants.COMMA) : new String[] {vcf.getAlt()}; 
 		
 		//only annotate somatic variants
		//annotation if at least one alts matches dbSNP alt		 
		for (final String alt : alts) {
			if (gAlt.equals(alt)) {
				/*
				 * add to INFO field as this information is positional.
				 * Add alt to counts so it is possible to distinguish which alt this GERM annotation is being applied to
				 * If a GERM annotation already exists, then we need to append...
				 */
				VcfInfoFieldRecord info = vcf.getInfoRecord();
				info.appendField(VcfHeaderUtils.INFO_GERMLINE, getDataForInfoField(gAlt, gNumbers));
				
				flag = true; 
			}
		}
		 return flag; 				
 	}
 	
 	public static String getDataForInfoField(String alt, String[] numbers) {
 		if (null == alt || null == numbers) {
 			return null;
 		}
 		return alt + ":" + Arrays.stream(numbers).collect(Collectors.joining(Constants.COLON_STRING));
 	}
// 	/**
// 	 * 
// 	 * @param inputVcf: a vcf record
// 	 * @param germlineVcf: snp listed on germline file
// 	 * @param total: total number of listed germline snp number which is written on database file header
// 	 * @return true if the inputVcf is matched the germline vcf
// 	 */
// 	public static boolean annotateGermlineSnp(VcfRecord inputVcf, VcfRecord germlineVcf , final int total){
// 		boolean flag = false;
// 		
// 		if ( ! inputVcf.getRef().equals(germlineVcf.getRef()) ){
// 			logger.warn(String.format( "germline reference base (%s) are different to vcf Record (%s) for variant at position: %s ", 
// 					germlineVcf.getRef(), inputVcf.getRef(), germlineVcf.getPosition() ));			 
// 			return flag ; 			 
// 		}
// 		
// 		if ( ! VcfUtils.isRecordSomatic(inputVcf)) {
// 			return flag;
// 		}
// 		
// 		String [] alts = inputVcf.getAlt().contains(Constants.COMMA_STRING) ? TabTokenizer.tokenize(inputVcf.getAlt(), Constants.COMMA) : new String[] {inputVcf.getAlt()}; 
// 		if (null == alts) return flag ;
// 		
// 		//only annotate somatic variants
// 		//annotation if at least one alts matches dbSNP alt		 
// 		for (final String alt : alts) {
// 			if (germlineVcf.getAlt().contains(alt)) {
// 				try {
// 					int counts = Integer.parseInt(germlineVcf.getInfo());
// 					
// 					/*
// 					 * add to INFO field as this information is positional.
// 					 * Add alt to counts so it is possible to distinguish which alt this GERM annotaiton is being applied to
// 					 * If a GERM annotation already exists, then we need to append...
// 					 */
// 					VcfInfoFieldRecord info = inputVcf.getInfoRecord();
// 					String value = alt + ":" + (total > 0 ?  +  counts + Constants.COMMA_STRING + total: ""+counts);
// 					info.appendField(VcfHeaderUtils.INFO_GERMLINE, value);
// 					
// 					flag = true; 
// 				} catch (NumberFormatException e){						
// 					logger.error("Germline database vcf formart, can't find patient counts from INFO field!");					 
// 				}
// 			}
// 		}
// 		return flag; 				
// 	}
}
