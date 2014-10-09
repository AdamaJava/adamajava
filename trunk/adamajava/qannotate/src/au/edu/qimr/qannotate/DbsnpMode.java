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

public class DbsnpMode {
	
	// /panfs/share/dbSNP/hg19.db130.single.SNP.all
	
	//normally << 10,000,000 records in one vcf file
	final Map<ChrPosition,VCFRecord> positionRecordMap = new HashMap<ChrPosition,VCFRecord>();
	VCFHeader header;
	private final QLogger logger;
		public DbsnpMode(DbsnpOptions options, QLogger logger) throws Exception{
			
		this.logger = logger;		
		logger.tool("input: " + options.getInputFileName());
        logger.tool("dbSNP database: " + options.getDatabaseFileName() );
        logger.tool("output for annotated vcf records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + options.getLogLevel());
		
		inputRecord(new File( options.getInputFileName())   );
		addAnnotation(new File( options.getDatabaseFileName() ));
		writeVCF(new File(options.getOutputFileName()), options.getCommandLine());	
	}
	
//	public void process(){}

	
	void inputRecord(File f) throws Exception{
		
        //read record into RAM, meanwhile wipe off the ID field value;
 
        try(VCFFileReader reader = new VCFFileReader(f)) {
        	header = reader.getHeader();
			for (VCFRecord qpr : reader) {
				qpr.setId(".");
				positionRecordMap.put(new ChrPosition(qpr.getChromosome(), qpr.getPosition()),qpr);
			}
		}  
        
	}
	
	void addAnnotation(File dbSNPFile) throws Exception{
		 VCFFileReader reader = new VCFFileReader(dbSNPFile);
     
		for (VCFRecord dbSNPVcf : reader) {
			// vcf dbSNP record chromosome does not contain "chr", whereas the positionRecordMap does - add
			//eg.positionRecordMap (key, value) = (chr1.100, vcfRecord )
			VCFRecord inputVcf = positionRecordMap.get(new ChrPosition("chr"+ dbSNPVcf.getChromosome(), dbSNPVcf.getPosition()));
			if (null == inputVcf) continue;
		 		
			// only proceed if we have a SNP variant record
			if ( ! StringUtils.doesStringContainSubString(dbSNPVcf.getInfo(), "VC=SNV", false)) continue;
			
			//reference base must be same
			if( dbSNPVcf.getRef() != dbSNPVcf.getRef() )
				throw new Exception("reference base are different ");
			 
 			
			//*eg. dbSNP: "1 100 rs12334 A G,T,C ..." dbSNP may have multiple entries
			//*eg. input.vcf: "1 100 101 A G ..." , "1 100 101 A T,C ..." out snp vcf are single entries			  
			String [] alts = null; 
			if(inputVcf.getAlt().length() > 1)	  				
				alts = TabTokenizer.tokenize(inputVcf.getAlt(), ',');
		    else 
				alts = new String[] {inputVcf.getAlt()};			
			
			if (null == alts)  continue;			
			//annotation if at least one alts matches dbSNP alt
			for (String alt : alts)  
				if(dbSNPVcf.getAlt().toUpperCase().contains(alt.toUpperCase()) ){
					inputVcf.setId(dbSNPVcf.getId());
					break;
				}
		}
		reader.close();

	}
	
	void writeVCF(File outputFile, String cmd ) throws IOException {
 
		logger.info("Writing VCF output");	 		
		//get Q_EXEC or #Q_DCCMETA  org.qcmg.common.meta.KeyValue.java or org.qcmg.common.meta.QExec.java	
		List<ChrPosition> orderedList = new ArrayList<ChrPosition>(positionRecordMap.keySet());
		Collections.sort(orderedList);
		
		try(VCFFileWriter writer = new VCFFileWriter( outputFile)) {
			header = reheader(header, cmd);
			
			for(String record: header)  writer.addHeader(record);
			for (ChrPosition position : orderedList) {				
				VCFRecord record = positionRecordMap.get(position); 
				writer.add( record );				 
			}
		}  
		
	}
	
//	private static final Pattern tabbedPattern = Pattern.compile("[\\t]+");

	
		/**
	 * insert the cmd into header before the final header line
	 * @param header: vcf header
	 * @param cmd: program command line
	 * @return vcf header
	 */
	private VCFHeader reheader(VCFHeader header , String cmd){
		final String STANDARD_FINAL_HEADER_LINE = "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO";
		final String STANDARD_FILE_DATE = "##fileDate=";
		final String STANDARD_SOURCE_LINE = "##source=";
		final String STANDARD_UUID_LINE = "##uuid=";
		
		String version = Main.class.getPackage().getImplementationVersion();

		DateFormat df = new SimpleDateFormat("yyyyMMdd");
		Vector<String> headerLines = new Vector<String>();
		for(String record: header){ 
			if(record.startsWith(STANDARD_FILE_DATE)){
				//replace date
				headerLines.add(STANDARD_FILE_DATE + df.format(Calendar.getInstance().getTime()) + "\n");
				//add uuid
				headerLines.add(STANDARD_UUID_LINE + QExec.createUUid() + "\n");
				headerLines.add(STANDARD_SOURCE_LINE + "qannotate " + version + "\n");
				continue;
			}else if(record.startsWith(STANDARD_SOURCE_LINE) || record.startsWith(STANDARD_UUID_LINE)){
				continue;
			
			}else if( record.startsWith(STANDARD_FINAL_HEADER_LINE) ){
				headerLines.add("##" + cmd + "\n"); //add cmd
				headerLines.add(record + "\n");
				break;
			} 
				
			headerLines.add(record + "\n");
		} 
				
		return new VCFHeader(headerLines);
	}	
}	

	 
	
	
 
