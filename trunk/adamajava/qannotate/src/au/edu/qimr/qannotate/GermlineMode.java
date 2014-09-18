package au.edu.qimr.qannotate;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.VCFRecord;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;
import org.qcmg.vcf.VCFHeader;

public class GermlineMode {
	final Map<ChrPosition,VCFRecord> positionRecordMap = new HashMap<ChrPosition,VCFRecord>();
	VCFHeader header;
	QLogger logger;

	public GermlineMode(GermlineOptions options) throws Exception{
		logger.tool("input: " + options.getInputFileName());
        logger.tool("dbGermline file: " + options.getDatabaseFileName() );
        logger.tool("output for annotated vcf records: " + options.getOutputFileName());
        logger.info("logger level " + options.getLogLevel());
 
		
		inputRecord(new File( options.getInputFileName())   );
		addAnnotation(new File( options.getDatabaseFileName() ));
		writeVCF(new File(options.getOutputFileName()), options.getCommandLine());	
	}
	
 
	private VCFHeader reheader(VCFHeader header, String cmd){
		final String STANDARD_FINAL_HEADER_LINE = "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO";
		final String STANDARD_FILE_DATE = "##fileDate=";

		DateFormat df = new SimpleDateFormat("yyyyMMdd");
		Vector<String> headerLines = new Vector<String>();
		for(String record: header){ 
			if(record.startsWith(STANDARD_FILE_DATE)){
				//replace date
				headerLines.add(STANDARD_FILE_DATE + df.format(Calendar.getInstance().getTime()) + "\n");
				//add uuid
				headerLines.add("##uuid=" + QExec.createUUid());
				continue;
			}else if( record.startsWith(STANDARD_FINAL_HEADER_LINE) ) 
				headerLines.add("##" + cmd + "\n"); //add cmd
	/*
	 * @PG	ID:6a412e43-1ff9-4802-b578-6a9b9b628f8b	PN:qbammerge	zc:7	VN:0.6pre (6775)	CL:qbammerge --output /mnt/seq_results/icgc_pancreatic/APGI_1992/seq_final/IcgcPancreatic_APGI1992_1DNA_7PrimaryTumour_ICGCABMJ2011092355TD_		
	 */
			headerLines.add(record + "\n");
		} 
				
		return new VCFHeader(headerLines);
	}	
	void writeVCF(File outputFile, String cmd ) throws IOException {
		 
		logger.info("Writing VCF output");
				 		
		//get Q_EXEC or #Q_DCCMETA  org.qcmg.common.meta.KeyValue.java or org.qcmg.common.meta.QExec.java	
		List<ChrPosition> orderedList = new ArrayList<ChrPosition>(positionRecordMap.keySet());
		Collections.sort(orderedList);
		
		try (VCFFileWriter writer = new VCFFileWriter( outputFile)) {
			header = reheader(header, cmd);
			
			for(String record: header)  writer.addHeader(record);
			for (ChrPosition position : orderedList) {				
				VCFRecord record = positionRecordMap.get(position); 
				writer.add( record );				 
			}
		}  
		
	}
	
	void inputRecord(File f) throws Exception{
		
        //read record into RAM, meanwhile wipe off the ID field value;
        
        try(VCFFileReader reader = new VCFFileReader(f)) {
        	header = reader.getHeader();
			for (VCFRecord qpr : reader) {
				positionRecordMap.put(new ChrPosition(qpr.getChromosome(), qpr.getPosition()),qpr);
			}
		}       
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
		
	void addAnnotation(File dbGermlineFile) throws Exception{
		 VCFFileReader reader = new VCFFileReader(dbGermlineFile);
		 String filter = null;
		 for (VCFRecord dbGermlineVcf : reader) {
			// vcf dbSNP record chromosome does not contain "chr", whereas the positionRecordMap does - add
			//eg.positionRecordMap (key, value) = (chr1.100, vcfRecord )
			VCFRecord inputVcf = positionRecordMap.get(new ChrPosition("chr"+ dbGermlineVcf.getChromosome(), dbGermlineVcf.getPosition()));
			if (null == inputVcf) continue;
		 		
			// only proceed if we have a SOMATIC variant record
			if ( ! StringUtils.doesStringContainSubString(dbGermlineVcf.getInfo(), "SOMATIC", false)) continue;
			
			//reference base must be same
			if( dbGermlineVcf.getRef() != dbGermlineVcf.getRef() )
				throw new Exception("reference base are different ");
			 
 			String [] alts = null; 
			if(inputVcf.getAlt().length() > 1)	  				
				alts = TabTokenizer.tokenize(inputVcf.getAlt(), ',');
		    else 
				alts = new String[] {inputVcf.getAlt()};			
			
			if (null == alts)  continue;			
			//annotation if at least one alts matches dbSNP alt
			for (String alt : alts)  
				if(dbGermlineVcf.getAlt().toUpperCase().contains(alt.toUpperCase()) ){
					filter = inputVcf.getFilter();
					if(filter.endsWith("PASS") ){
							if (filter.indexOf("PASS") != filter.length() - 4)
							throw new Exception("mutli \"PASS\" marked on the FILTER field for vcf record: " + inputVcf.toString());
							filter = filter.replace("PASS", "GERM");							
					}else
						filter +=  ";GERM";
				 
					inputVcf.setFilter(filter);
					break;
				}
		 }
		 reader.close(); 
	}
	
}
