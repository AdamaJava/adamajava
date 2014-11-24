package au.edu.qimr.qannotate.modes;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.VCFRecord;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.qannotate.Main;
import au.edu.qimr.qannotate.options.DbsnpOptions;

public class DbsnpMode extends AbstractMode{
	
	// /panfs/share/dbSNP/hg19.db130.single.SNP.all
	
	//normally << 10,000,000 records in one vcf file
	final Map<ChrPosition,VCFRecord> positionRecordMap = new HashMap<ChrPosition,VCFRecord>();
	VcfHeader header;
	private final QLogger logger;
		public DbsnpMode(DbsnpOptions options, QLogger logger) throws Exception{
			
		this.logger = logger;		
		logger.tool("input: " + options.getInputFileName());
        logger.tool("dbSNP database: " + options.getDatabaseFileName() );
        logger.tool("output for annotated vcf records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + options.getLogLevel());
		
		inputRecord(new File( options.getInputFileName())   );
		
		addAnnotation(options.getDatabaseFileName() );
		reheader(options.getCommandLine(),options.getInputFileName())	;	
		writeVCF(new File(options.getOutputFileName()));	
	}
		
	
	@Override
	void addAnnotation(String dbSNPFile) throws Exception{
		//init remove all exsiting dbsnpid
		Iterator<VCFRecord> it = positionRecordMap.values().iterator(); 
 	    while (it.hasNext()) {
	        VCFRecord vcf = it.next();
	        vcf.setId(".");
 	    }
		 		
		 
		try(VCFFileReader reader= new VCFFileReader( dbSNPFile )) {
			//add dbSNP version into header	
			VcfHeader snpHeader = reader.getHeader();
			for (VcfHeaderRecord hr : snpHeader) 
				if(hr.getId().equalsIgnoreCase(VcfHeaderUtils.STANDARD_DBSNP_LINE)){
					header.replace(hr);  
					break;					
				}	
		 
			//check dbsnp record
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
		}

	}
	@Override
	protected void writeVCF(File outputFile  ) throws IOException {
 
		logger.info("Writing VCF output");	 		
		//get Q_EXEC or #Q_DCCMETA  org.qcmg.common.meta.KeyValue.java or org.qcmg.common.meta.QExec.java	
		List<ChrPosition> orderedList = new ArrayList<ChrPosition>(positionRecordMap.keySet());
		Collections.sort(orderedList);
		
		try(VCFFileWriter writer = new VCFFileWriter( outputFile)) {
			 
			
			for(VcfHeaderRecord record: header)  writer.addHeader(record.toString());
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
		 * @throws Exception 
	 */
	protected VcfHeader reheader(VcfHeader header , String cmd) throws Exception{	
		String version = Main.class.getPackage().getImplementationVersion();

		DateFormat df = new SimpleDateFormat("yyyyMMdd");
		//Vector<String> headerLines = new Vector<String>();
		
		header.updateHeader(new VcfHeaderRecord(VcfHeaderUtils.CURRENT_FILE_VERSION),
				new VcfHeaderRecord(VcfHeaderUtils.STANDARD_FILE_DATE + df.format(Calendar.getInstance().getTime())),
				new VcfHeaderRecord(VcfHeaderUtils.STANDARD_UUID_LINE + QExec.createUUid() ),
				new VcfHeaderRecord(VcfHeaderUtils.STANDARD_SOURCE_LINE + "qannotate " + version) );
 				
		return header;
	}	
}	

	 
	
	
 
